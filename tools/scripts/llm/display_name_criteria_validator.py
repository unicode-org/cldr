#!/usr/bin/env python3
import json
import argparse
from pathlib import Path

# Optional: for LLM-assisted quality checks
try:
    from transformers import pipeline
except ImportError:
    pipeline = None

# Expected lengths and English plural categories
LENGTHS = ["long", "short", "narrow"]
PLURAL_CATEGORIES = {
    "en": ["zero", "one", "two", "few", "many", "other"],
    # Add other locales here as needed
}

# Default paths
DEFAULT_JSON_FILE = Path(__file__).parent / "units.json"
DEFAULT_CLDR_TREE = Path(__file__).parent / "cldr-json" / "cldr-units-full" / "main"

# Cache for loaded units data
_loaded_cache = {}

# Global LLM object (initialized in main if requested)
llm = None


def init_llm(model_name="google/flan-t5-base", device=-1):
    """Initialize a text2text-generation LLM pipeline."""
    if pipeline is None:
        raise RuntimeError("Please install transformers: pip install transformers torch sentencepiece")
    return pipeline(
        "text2text-generation",
        model=model_name,
        device=device,
        max_new_tokens=32,
        do_sample=False
    )


def ask_llm_quality(pattern):
    """
    Ask the LLM for a quality verdict on the CLDR pattern.
    Returns "GOOD" or "BAD" based on model response.
    """
    prompt = (
        f"Evaluate the following CLDR unit pattern for correctness and style.\n"
        f"Pattern: '{pattern}'\n"
        f"Respond only with the word GOOD if it looks correct, or BAD if it is flawed."
    )
    resp = llm(prompt)[0]["generated_text"].strip().upper()
    return "GOOD" if "GOOD" in resp else "BAD"


def load_units(locale, json_path=None, tree_root=None):
    """Load the 'units' dictionary for a given locale."""
    if json_path:
        path = Path(json_path)
    else:
        tr = Path(tree_root) if tree_root else DEFAULT_CLDR_TREE
        cand = tr / locale / "units.json"
        path = cand if cand.is_file() else DEFAULT_JSON_FILE

    cache_key = (locale, str(path))
    if cache_key in _loaded_cache:
        return _loaded_cache[cache_key]

    if not path.is_file():
        raise FileNotFoundError(f"Could not find units.json at {path}")
    data = json.loads(path.read_text(encoding="utf-8"))
    units = data.get("main", {}).get(locale, {}).get("units", {})
    _loaded_cache[cache_key] = units
    return units


def get_fallback_chain(locale):
    parts = locale.split("-")
    chain = ["-".join(parts[:i]) for i in range(len(parts), 0, -1)]
    chain.append("root")
    return chain


def check_fallback(unit_key, locale, json_path=None, tree_root=None, use_llm=False):
    """Report defined/inherited status, criteria, and LLM quality for each pattern."""
    chain = get_fallback_chain(locale)
    report = {"lengths": {}, "categories": {}}
    needed_cats = PLURAL_CATEGORIES.get(locale, [])

    # Preload units
    units_map = {loc: load_units(loc, json_path=json_path, tree_root=tree_root) for loc in chain}

    for length in LENGTHS:
        # Check length existence
        status = "missing"
        inherited_from = None
        for loc in chain:
            units = units_map.get(loc, {})
            block = units.get(length, {}).get(unit_key)
            if block is not None:
                status = "defined" if loc == locale else "inherited"
                inherited_from = None if loc == locale else loc
                break
        report["lengths"][length] = {"status": status, "inherited_from": inherited_from}

        # Now categories
        report["categories"][length] = {}
        for cat in needed_cats:
            cat_status = "missing"
            cat_inherited_from = None
            pattern = None
            value_ok = None
            value_issue = None

            # Find pattern up the chain
            for loc in chain:
                units = units_map.get(loc, {})
                block = units.get(length, {}).get(unit_key, {})
                if block and f"unitPattern-count-{cat}" in block:
                    pattern = block[f"unitPattern-count-{cat}"]
                    cat_status = "defined" if loc == locale else "inherited"
                    cat_inherited_from = None if loc == locale else loc
                    break

            # Criteria check: presence and placeholder sanity
            if pattern is not None:
                if "{0}" in pattern and pattern.strip():
                    value_ok = True
                else:
                    value_ok = False
                    value_issue = "Pattern missing '{0}' or empty"

            entry = {
                "status": cat_status,
                "inherited_from": cat_inherited_from,
                "pattern": pattern,
                "value_ok": value_ok
            }
            if value_issue:
                entry["value_issue"] = value_issue
            if cat_status == "missing":
                entry["expected_key"] = f"unitPattern-count-{cat}"
                entry["explanation"] = f"'{entry['expected_key']}' not found in any locale or fallback chain"

            # LLM quality check
            if use_llm and pattern:
                entry["llm_quality"] = ask_llm_quality(pattern)

            report["categories"][length][cat] = entry

    return report


def main():
    parser = argparse.ArgumentParser(description="CLDR unit fallback, criteria & LLM checker")
    parser.add_argument("unit_key", help="e.g. 'length-meter'")
    parser.add_argument("-l", "--locale", default="en")
    parser.add_argument("-j", "--json-path", default=None)
    parser.add_argument("-t", "--tree-root", default=None)
    parser.add_argument("--use-llm", action="store_true", help="Enable LLM quality checks")
    parser.add_argument("--model", default="google/flan-t5-base", help="LLM model name")
    parser.add_argument("--device", type=int, default=-1, help="Device id for LLM or -1 for CPU")
    args = parser.parse_args()

    global llm
    if args.use_llm:
        llm = init_llm(model_name=args.model, device=args.device)

    report = check_fallback(
        unit_key=args.unit_key,
        locale=args.locale,
        json_path=args.json_path,
        tree_root=args.tree_root,
        use_llm=args.use_llm
    )
    print(json.dumps(report, indent=2, ensure_ascii=False))

if __name__ == "__main__":
    main()
