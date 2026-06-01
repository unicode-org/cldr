#!/usr/bin/env python3
"""
Posts the prepared prompt to OpenAI and prints the model's response.
Optionally: posts the JSON back to the JIRA ticket as a comment and updates fields.

Usage:
  python cldr_prompt_to_llm.py CLDR-1234
  python cldr_prompt_to_llm.py CLDR-1234 --category "Software Bug"
  python cldr_prompt_to_llm.py CLDR-1234 --auto-category --model gpt-4o-mini
  # post back to JIRA:
  python cldr_prompt_to_llm.py CLDR-1234 --post-comment
  python cldr_prompt_to_llm.py CLDR-1234 --post-comment --update-fields
"""

import sys
import json
import argparse
from openai import OpenAI
from jira import JIRA

from cldr_dynamic_prompter import make_prompt

# ---------------- OpenAI (fill before running) ----------------
OPENAI_API_KEY = "YOUR KEY!!!"   # <- your real OpenAI key (don't commit!)
DEFAULT_MODEL  = "gpt-4o-mini"
# --------------------------------------------------------------

# ---------------- JIRA (only needed for --post-comment / --update-fields) ---------------
JIRA_SERVER = "https://unicode-org.atlassian.net"
JIRA_USER_EMAIL = "YOUR MAIL ID"   # <- your JIRA email
JIRA_API_TOKEN  = "YOUR KEY !!!"        # <- your JIRA API token (don't commit!)
# ---------------------------------------------------------------------------------------

def _extract_json(text: str) -> str:
    """
    If the model returns extra prose, try to extract the first JSON object.
    Returns the original text if no JSON can be isolated.
    """
    text = text.strip()
    try:
        json.loads(text)
        return text
    except Exception:
        pass

    s, e = text.find("{"), text.rfind("}")
    if s != -1 and e != -1 and e > s:
        chunk = text[s:e+1]
        try:
            json.loads(chunk)
            return chunk
        except Exception:
            return chunk
    return text

# -------------------- JIRA helpers --------------------

def _jira_client() -> JIRA:
    return JIRA(server=JIRA_SERVER, basic_auth=(JIRA_USER_EMAIL, JIRA_API_TOKEN))

def post_result_comment(ticket_key: str, json_text: str) -> None:
    """
    Add a comment with the Phase-1/Phase-2 JSON. Uses Jira's {code:json} block.
    Adjust visibility if you need it to be restricted.
    """
    jira = _jira_client()
    body = (
        "Classification result (LLM)\n"
        "{code:json}\n" + json_text + "\n{code}\n"
        "_Posted by CLDR validator tool_"
    )
    # Example for restricted comment (role):
    # jira.add_comment(ticket_key, body, visibility={"type": "role", "value": "Administrators"})
    jira.add_comment(ticket_key, body)

def _is_false_like(v) -> bool:
    # Helper: handle "false" (str), False (bool), 0, etc.
    if isinstance(v, str):
        return v.strip().lower() in {"false", "no", "0"}
    return not bool(v)

def _is_true_like(v) -> bool:
    if isinstance(v, str):
        return v.strip().lower() in {"true", "yes", "1"}
    return bool(v)

def update_fields_from_result(ticket_key: str, result: dict) -> None:
    """
    Example field updates:
      - Merge useful labels (spam, out-of-scope, phase1-classified, needs-lang, needs-tc)
      - Optionally merge components if JSON includes `components.additions` (Phase 2 style)
    """
    jira = _jira_client()
    issue = jira.issue(ticket_key)

    # ----- labels -----
    existing_labels = set(issue.fields.labels or [])
    to_add = {"phase1-classified"}

    # Phase-1 schemas vary across prompts; check common shapes
    spam = result.get("spamCheck", {}).get("isSpam")
    scope = result.get("scopeCheck", {}).get("inScope")
    needs_lang = (
        result.get("needsLanguageSpecialist", {}).get("status") or
        result.get("needsLanguageSpecialist")
    )
    needs_tc = (
        result.get("needsEngineeringTC") or
        result.get("needsEngineeringWork", {}).get("status")
    )

    if _is_true_like(spam):
        to_add.add("spam")

    # out-of-scope can be boolean or "false"/"true"/"unknown"
    if isinstance(scope, str):
        if scope.strip().lower() == "false":
            to_add.add("out-of-scope")
    elif scope is not None:
        if _is_false_like(scope):
            to_add.add("out-of-scope")

    if _is_true_like(needs_lang):
        to_add.add("needs-language-specialist")

    if _is_true_like(needs_tc):
        to_add.add("needs-engineering-tc")

    new_labels = sorted(existing_labels | to_add)
    if new_labels != list(existing_labels):
        issue.update(fields={"labels": new_labels})

    # ----- components (Phase-2 style JSON) -----
    # If your JSON includes something like:
    #   "components": { "present": [...], "additions": ["Units","Plural Rules"] }
    comp_suggestions = (result.get("components") or {}).get("additions", [])
    if comp_suggestions:
        current_names = {c.name for c in (issue.fields.components or [])}
        merged = sorted(current_names | set(comp_suggestions))
        if merged != list(current_names):
            issue.update(fields={"components": [{"name": n} for n in merged]})

# -------------------- main --------------------

def main():
    ap = argparse.ArgumentParser(
        description="Send CLDR prompt to OpenAI and print the response (optional: post to JIRA)."
    )
    ap.add_argument("ticket_key", help="e.g., CLDR-18761")
    ap.add_argument(
        "--category",
        choices=["Data Accuracy", "Documentation Issue", "Software Bug", "Feature Request", "Triage"],
        help="Force category; otherwise use default behavior of the prompt generator."
    )
    ap.add_argument("--auto-category", action="store_true", help="Let the prompter LLM choose a category.")
    ap.add_argument("--model", default=DEFAULT_MODEL, help="OpenAI model (default: gpt-4o-mini).")
    ap.add_argument("--post-comment", action="store_true", help="Post the JSON result into the JIRA ticket.")
    ap.add_argument("--update-fields", action="store_true", help="Update labels/components based on the JSON.")

    args = ap.parse_args()

    # 1) Build the prompt (uses your generator)
    prompt = make_prompt(args.ticket_key, category=args.category, auto_category=args.auto_category)

    # 2) Call OpenAI
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

    # 3) Print the (extracted) JSON to stdout
    json_text = _extract_json(content)
    print(json_text)

    # 4) Optionally push back to JIRA
    if args.post_comment:
        try:
            post_result_comment(args.ticket_key, json_text)
        except Exception as e:
            print(f"Failed to post JIRA comment: {e}", file=sys.stderr)

    if args.update_fields:
        try:
            parsed = json.loads(json_text)
            update_fields_from_result(args.ticket_key, parsed)
        except Exception as e:
            print(f"Skipping field updates (invalid JSON or JIRA error): {e}", file=sys.stderr)

if __name__ == "__main__":
    main()
