#!/usr/bin/env python3
"""
validate_currencies.py

Run AI‐powered sanity checks on CLDR currency data and emit a JSON report.

Usage:
  python validate_currencies.py [locale] [output.json]

Defaults:
  locale      = en-001
  output.json = currencies_report.json
"""

import os
import json
import requests
import argparse
from transformers import pipeline

def fetch_currencies(locale: str) -> dict:
    """Download the CLDR currencies.json for a given locale."""
    url = (
        "https://raw.githubusercontent.com/unicode-org/cldr-json"
        "/main/cldr-json/cldr-numbers-full/main/"
        f"{locale}/currencies.json"
    )
    resp = requests.get(url)
    resp.raise_for_status()
    return resp.json()

def flatten(cdata: dict, locale: str) -> list[str]:
    """Turn the nested currencies object into lines of text, collecting ALL count forms."""
    cur = cdata["main"][locale]["numbers"]["currencies"]
    out = []
    for code, info in cur.items():
        # collect every displayName-count-* field
        counts = [
            f"{k.split('displayName-count-')[-1]}='{v}'"
            for k, v in info.items()
            if k.startswith("displayName-count-")
        ]
        # join into a single “counts=” piece
        counts_str = ", ".join(counts) if counts else ""
        sym    = info.get("symbol", "")
        narrow = info.get("symbol-alt-narrow", "")
        out.append(
            f"{code}: counts=[{counts_str}], symbol='{sym}', alt_narrow='{narrow}'"
        )
    return out

def chunkify(lines: list[str], size: int = 5) -> list[list[str]]:
    """
    Split a list of lines into fixed‐size chunks.
    Default size=5 keeps each prompt safely under 512 tokens.
    """
    return [lines[i : i + size] for i in range(0, len(lines), size)]

def call_llm(llm, instruction: str, chunks: list[list[str]]) -> str:
    """Run an instruction + each chunk through the LLM, concatenating replies."""
    parts = []
    for chunk in chunks:
        prompt = (
            instruction
            + "\n\n```"
            + "\n".join(chunk)
            + "\n```"
        )
        # add truncation=True just in case
        response = llm(prompt, truncation=True)[0]["generated_text"].strip()
        parts.append(response)
    return " ".join(parts)

def main():
    parser = argparse.ArgumentParser(
        description="Validate CLDR currency data with an LLM and emit a JSON report."
    )
    parser.add_argument("locale", nargs="?", default="en-001",
                        help="CLDR locale code (e.g. en-001, fr-CA)")
    parser.add_argument("outfile", nargs="?", default="currencies_report.json",
                        help="Filename to write the JSON report to")
    args = parser.parse_args()

    # 1) Initialize LLM on GPU if available
    device = 0 if os.environ.get("CUDA_VISIBLE_DEVICES") else -1
    llm = pipeline(
        "text2text-generation",
        model="google/flan-t5-base",
        device=device,
        max_new_tokens=32,
        do_sample=False
    )
    print(f"Device set to use {'GPU' if device >= 0 else 'CPU'}")

    # 2) Fetch & flatten
    data  = fetch_currencies(args.locale)
    lines = flatten(data, args.locale)

    # 3) Break into very small chunks
    chunks = chunkify(lines, size=5)

    # 4) Define checks
    checks = {
        "Check correctness":
          "For each currency line below, reply exactly “All correct” "
          "or list any codes whose names or symbols look wrong.",
        "Find missing symbols":
          "List any currency codes that lack a symbol or alt_narrow entry, "
          "or reply exactly “None.”"
    }

    report = { args.locale: {} }

    # 5) Run the two sanity checks
    for label, instr in checks.items():
        print(f"Running {label}…")
        result = call_llm(llm, instr, chunks)
        key = label.lower().replace(" ", "_")
        report[args.locale][key] = { "result": result }

    # 6) Count locally
    report[args.locale]["count_entries"] = { "python": len(lines) }

    # 7) Write JSON
    with open(args.outfile, "w", encoding="utf-8") as f:
        json.dump(report, f, indent=2, ensure_ascii=False)

    print(f"Report written to {args.outfile}")

if __name__ == "__main__":
    main()
