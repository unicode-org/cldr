#!/usr/bin/env python3
"""
validate_all_units.py

Run validations for multiple locales and unit types, and save a combined JSON report.

Usage:
    python validate_all_units.py [output_file.json]

Defaults to writing "all_units_report.json" in the current directory.
"""

import json
import requests
import argparse
from transformers import pipeline

def fetch_units_json(lang):
    """Fetch the master units.json (all types) for a given language code."""
    url = (
        "https://raw.githubusercontent.com/unicode-org/cldr-json"
        "/main/cldr-json/cldr-units-full/main/"
        f"{lang}/units.json"
    )
    resp = requests.get(url)
    resp.raise_for_status()
    return resp.json()

def flatten_patterns(units_dict):
    """Convert a units sub-dictionary into 'key: pattern' lines."""
    lines = []
    for key, info in units_dict.items():
        if "unitPrefixPattern" in info:
            lines.append(f"{key}: {info['unitPrefixPattern']}")
        elif "compoundUnitPattern" in info:
            lines.append(f"{key}: {info['compoundUnitPattern']}")
    return lines

def chunkify(items, size=50):
    """Split a list into fixed-size chunks."""
    return [items[i:i+size] for i in range(0, len(items), size)]

def call_llm(llm, instruction, blocks):
    """Run the instruction across each block, concatenating the outputs."""
    parts = []
    for block in blocks:
        prompt = f"{instruction}\n\n```\n" + "\n".join(block) + "\n```"
        out = llm(prompt)[0]["generated_text"].strip()
        parts.append(out)
    return " ".join(parts)

def parse_list(text):
    """Parse a comma/semicolon-separated LLM reply into a Python list."""
    items = []
    for seg in text.replace(";", ",").split(","):
        seg = seg.strip().strip('"').strip("'")
        if seg and seg.lower() not in ("none", "all correct"):
            items.append(seg)
    return items

def validate_units_for(locale, unit_type, llm):
    """Run all checks for a single locale and type, returning a dict."""
    lang = locale.split('-')[0]
    data = fetch_units_json(lang)
    try:
        units_dict = data["main"][lang]["units"][unit_type]
    except KeyError:
        raise ValueError(f"No units of type '{unit_type}' for locale {locale}")

    patterns = flatten_patterns(units_dict)
    chunks = chunkify(patterns, 50)

    report = {}

    # Correctness check
    instr1 = (
        "For the following CLDR unit patterns, reply exactly "
        "'All correct' or list the incorrect entries."
    )
    res1 = call_llm(llm, instr1, chunks)
    report["Check correctness"] = (
        {"status": "All correct"} if "all correct" in res1.lower()
        else {"errors": parse_list(res1)}
    )

    # Missing-prefix check
    instr2 = (
        "Which standard SI prefixes are missing? "
        "If none, reply exactly 'None'."
    )
    res2 = call_llm(llm, instr2, chunks)
    report["Find missing"] = {"missing_prefixes": parse_list(res2)}

    # Count check (LLM)
    instr3 = (
        "Count how many unit patterns are listed. "
        "Reply only with a JSON object like {\"count\": N}."
    )
    res3 = call_llm(llm, instr3, [patterns])
    try:
        parsed = json.loads(res3)
        count_llm = parsed.get("count", len(patterns))
    except Exception:
        count_llm = len(patterns)
    report["Count patterns (LLM)"] = {"count": count_llm}

    # Count check (Python)
    report["Count patterns (Python)"] = {"count": len(patterns)}

    return report

def main():
    parser = argparse.ArgumentParser(
        description="Validate multiple CLDR unit definitions via LLM"
    )
    parser.add_argument(
        "output", nargs="?", default="all_units_report.json",
        help="Path of the JSON report to write"
    )
    args = parser.parse_args()

    # Define locales and types to validate
    locales = ["en-US", "fr-FR", "es-ES"]
    types = ["long", "short", "narrow"]

    # Initialize the LLM (CPU device)
    llm = pipeline(
        "text2text-generation",
        model="google/flan-t5-base",
        device=-1,
        max_new_tokens=64,
        do_sample=False
    )

    all_results = {}
    for loc in locales:
        all_results[loc] = {}
        for t in types:
            print(f"Validating {loc} ({t})…")
            all_results[loc][t] = validate_units_for(loc, t, llm)

    # Write the combined report
    with open(args.output, "w", encoding="utf-8") as f:
        json.dump(all_results, f, indent=2, ensure_ascii=False)

    print(f"\nSaved combined report to {args.output}")

if __name__ == "__main__":
    main()
