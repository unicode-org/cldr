#!/usr/bin/env python3
"""Send Phase-2 prompt to OpenAI and print the routing JSON."""
import sys, json, argparse
from jira import JIRA
from openai import OpenAI
from cldr_phase2_prompt_generator import build_phase2_prompt

# --- Paste creds ---
JIRA_SERVER = "https://unicode-org.atlassian.net"
JIRA_USER_EMAIL = "YOUR MAIL !!"  # <-- Replace with your email
JIRA_API_TOKEN = "JIRA API KEY !!"    # <-- Replace with your API token

OPENAI_API_KEY = "OpenAI API KRY !!"
DEFAULT_MODEL  = "gpt-4o-mini"
# -------------------

def _json_only(text: str) -> str:
    t = text.strip()
    try: json.loads(t); return t
    except: pass
    s, e = t.find("{"), t.rfind("}")
    if s != -1 and e != -1 and e > s:
        chunk = t[s:e+1]
        try: json.loads(chunk); return chunk
        except: return chunk
    return t

def main():
    ap = argparse.ArgumentParser(description="Run Phase-2 routing via OpenAI.")
    ap.add_argument("ticket_key", help="e.g., CLDR-18761")
    ap.add_argument("--model", default=DEFAULT_MODEL)
    ap.add_argument("--max-desc", type=int, default=380)
    args = ap.parse_args()

    try:
        jc = JIRA(server=JIRA_SERVER, basic_auth=(JIRA_USER_EMAIL, JIRA_API_TOKEN))
        prompt = build_phase2_prompt(args.ticket_key, jc, args.max_desc)
    except Exception as e:
        print(f"Jira/prompt error: {e}", file=sys.stderr); sys.exit(2)

    try:
        client = OpenAI(api_key=OPENAI_API_KEY)
        resp = client.chat.completions.create(
            model=args.model,
            messages=[{"role": "user", "content": prompt}],
            temperature=0,
        )
        raw = (resp.choices[0].message.content or "").strip()
    except Exception as e:
        print(f"OpenAI call failed: {e}", file=sys.stderr); sys.exit(2)

    print(_json_only(raw))

if __name__ == "__main__":
    main()
