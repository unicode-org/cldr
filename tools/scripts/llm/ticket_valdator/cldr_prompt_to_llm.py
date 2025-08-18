#!/usr/bin/env python3
"""
Posts the prepared prompt to OpenAI and prints the model's response.
Usage:
  python cldr_prompt_to_llm.py CLDR-1234
  python cldr_prompt_to_llm.py CLDR-1234 --category "Software Bug"
  python cldr_prompt_to_llm.py CLDR-1234 --auto-category --model gpt-4o-mini
"""
import sys, argparse, json
from openai import OpenAI
from cldr_dynamic_prompter import make_prompt

# ---- Paste your OpenAI creds (no envs required) ----
OPENAI_API_KEY = "API key of openAi"
DEFAULT_MODEL  = "gpt-4o-mini"
# ----------------------------------------------------

def _extract_json(text: str) -> str:
    """If the model adds extra text, try to print the first JSON object cleanly."""
    text = text.strip()
    try:
        json.loads(text); return text
    except Exception:
        pass
    s, e = text.find("{"), text.rfind("}")
    if s != -1 and e != -1 and e > s:
        chunk = text[s:e+1]
        try:
            json.loads(chunk); return chunk
        except Exception:
            return chunk
    return text

def main():
    ap = argparse.ArgumentParser(description="Send CLDR prompt to OpenAI and print the response.")
    ap.add_argument("ticket_key", help="e.g., CLDR-18761")
    ap.add_argument("--category", choices=["Data Accuracy","Documentation Issue","Software Bug","Feature Request","Triage"])
    ap.add_argument("--auto-category", action="store_true")
    ap.add_argument("--model", default=DEFAULT_MODEL)
    args = ap.parse_args()

    # 1) Prepare prompt (re-uses the function in cldr_dynamic_prompter.py)
    prompt = make_prompt(args.ticket_key, category=args.category, auto_category=args.auto_category)

    # 2) Post to OpenAI
    try:
        client = OpenAI(api_key=OPENAI_API_KEY)
        resp = client.chat.completions.create(
            model=args.model,
            messages=[{"role": "user", "content": prompt}],
            temperature=0,
        )
        content = (resp.choices[0].message.content or "").strip()
    except Exception as e:
        print(f"OpenAI call failed: {e}", file=sys.stderr)
        sys.exit(2)

    # 3) Print (JSON if present)
    print(_extract_json(content))

if __name__ == "__main__":
    main()
