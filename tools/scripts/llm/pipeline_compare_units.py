#!/usr/bin/env python3
"""
pipeline_compare_units.py

1. Extracts 'person-height' and 'road' unit preferences from CLDR XML.
2. Prompts GPT-4 (without mentioning CLDR) for each country’s commonly used
   person‐height and road‐distance units.
3. Parses the LLM output into structured JSON.
4. Feeds both CLDR and LLM results into GPT-4 to produce a human‐readable
   comparison of matches and mismatches.

 NOTE: The OpenAI API key is hard-coded below for quick local testing.
   Be sure to remove or secure it before committing this file to any repository.
"""

import json
import os
import xml.etree.ElementTree as ET
from collections import defaultdict
from openai import OpenAI

# ── Hard-coded API key (for local testing only!) ──
client = OpenAI(api_key="put your-api-key-here")

# Static mapping from country names to ISO2 codes
COUNTRY_NAME_TO_CODE = {
    "United States": "US",
    "Canada": "CA",
    "United Kingdom": "GB",
    "Germany": "DE",
    "Switzerland": "CH",
    "France": "FR",
    "Japan": "JP",
    "India": "IN",
    "Australia": "AU",
    "South Korea": "KR",
}

def extract_cldr_units(xml_path):
    """Parse CLDR units.xml and return two dicts: person_height_cldr, road_cldr."""
    tree = ET.parse(xml_path)
    root = tree.getroot()

    target = {
        "person-height": defaultdict(list),
        "road": defaultdict(list),
    }

    for up in root.findall(".//unitPreferences"):
        cat = up.get("category")
        usage = up.get("usage")
        if cat == "length" and usage in target:
            for pref in up.findall("unitPreference"):
                unit = pref.text.strip()
                for region in pref.get("regions", "").split():
                    target[usage][region].append(unit)

    return dict(target["person-height"]), dict(target["road"])

def build_prompt(countries, measure):
    """Construct a general prompt for GPT-4 without mentioning CLDR."""
    headers = {
        "person-height": (
            "For each of the following countries, what unit is commonly used to measure "
            "a person's height in daily life? Consider everyday conversation, forms, or health records. "
            "Only list the most common unit(s)."
        ),
        "road": (
            "For each of the following countries, what unit is commonly used to report road "
            "distances (e.g., highway signs, maps)? Only list the most common unit(s)."
        )
    }
    follow_ups = {
        "person-height": (
            "If any country uses more than one unit depending on the height range, please note that."
        ),
        "road": (
            "If shorter vs. longer distances use different units (e.g., city vs. highway), please note that."
        )
    }
    header = headers[measure]
    follow_up = follow_ups[measure]
    lines = "\n".join(f"- {c}" for c in countries)
    return f"{header}\n\nCountries:\n{lines}\n\n{follow_up}"

def ask_llm(prompt_text):
    """Call GPT-4 and return its raw reply text."""
    resp = client.chat.completions.create(
        model="gpt-4",
        messages=[{"role": "user", "content": prompt_text}],
        temperature=0
    )
    return resp.choices[0].message.content

def parse_llm_response(text):
    """
    Parse lines like '- United States: Feet and inches' into
    { 'United States': ['Feet and inches'], ... }
    """
    result = {}
    for line in text.strip().splitlines():
        if ":" not in line:
            continue
        # remove any leading bullets or whitespace
        line = line.lstrip(" -\t")
        country, units = line.split(":", 1)
        parts = [u.strip() for segment in units.replace(" and ", ",").split(",") for u in [segment] if u.strip()]
        result[country.strip()] = parts
    return result

def normalize_by_code(llm_dict):
    """Convert country names in LLM output to ISO2 codes."""
    out = {}
    for name, units in llm_dict.items():
        code = COUNTRY_NAME_TO_CODE.get(name)
        if code:
            out[code] = units
    return out

def compare_and_report(cldr, llm):
    """
    Build a prompt containing both CLDR and LLM JSON and ask GPT-4 to compare them.
    """
    prompt = (
        "I have two JSON objects mapping ISO country codes to lists of units:\n\n"
        "CLDR data:\n" +
        json.dumps(cldr, indent=2) +
        "\n\nLLM predictions:\n" +
        json.dumps(llm, indent=2) +
        "\n\nPlease compare them, listing for each country whether they match or not, "
        "and if not, describe the discrepancy."
    )
    return ask_llm(prompt)

def main():
    # 1) Extract CLDR data
    person_cldr, road_cldr = extract_cldr_units("units.xml")

    # 2) Get LLM predictions for person-height
    countries = list(COUNTRY_NAME_TO_CODE.keys())
    ph_prompt = build_prompt(countries, "person-height")
    ph_raw = ask_llm(ph_prompt)
    ph_parsed = parse_llm_response(ph_raw)
    ph_llm = normalize_by_code(ph_parsed)
    with open("person_height_units_llm.json", "w") as f:
        json.dump(ph_llm, f, indent=2)

    # 3) Get LLM predictions for road distances
    road_prompt = build_prompt(countries, "road")
    road_raw = ask_llm(road_prompt)
    road_parsed = parse_llm_response(road_raw)
    road_llm = normalize_by_code(road_parsed)
    with open("road_distance_units_llm.json", "w") as f:
        json.dump(road_llm, f, indent=2)

    # 4) Compare via LLM and write reports
    report_ph = compare_and_report(person_cldr, ph_llm)
    report_road = compare_and_report(road_cldr, road_llm)
    with open("person_height_comparison.txt", "w") as f:
        f.write(report_ph)
    with open("road_distance_comparison.txt", "w") as f:
        f.write(report_road)

    print("Done!")
    print("Generated files:")
    print(" - person_height_units_cldr.json")
    print(" - person_height_units_llm.json")
    print(" - road_distance_units_cldr.json")
    print(" - road_distance_units_llm.json")
    print(" - person_height_comparison.txt")
    print(" - road_distance_comparison.txt")

if __name__ == "__main__":
    main()
