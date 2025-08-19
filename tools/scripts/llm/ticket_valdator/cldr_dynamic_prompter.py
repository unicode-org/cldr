#!/usr/bin/env python3
"""
cldr_dynamic_prompter.py

What it does: prints a short, ticket-specific LLM prompt for a CLDR JIRA ticket.
Usage:
  python cldr_dynamic_prompter.py CLDR-1234
  python cldr_dynamic_prompter.py CLDR-1234 --category "Software Bug"
  python cldr_dynamic_prompter.py CLDR-1234 --auto-category
"""

import re
import sys
import argparse
from typing import Optional, Dict, List

from jira import JIRA
from cldr_ticket_reader import get_ticket_details_string

# =========================
# CONFIG: paste credentials
# =========================
JIRA_SERVER = "https://unicode-org.atlassian.net"
JIRA_USER_EMAIL = "MAIL"  # <-- Replace with your email
JIRA_API_TOKEN = "API OF JIRA"    # <-- Replace with your API token


# optional; only needed if you use --auto-category
OPENAI_API_KEY = "OpenAI API KEY!!"
OPENAI_MODEL = "gpt-4o-mini"

CATEGORIES = ("Data Accuracy", "Documentation Issue", "Software Bug", "Feature Request")

# -----------------------------
# Jira + parsing helpers
# -----------------------------

def get_jira_client_from_config() -> JIRA:
    if not JIRA_USER_EMAIL or not JIRA_API_TOKEN:
        print("Error: fill JIRA_USER_EMAIL and JIRA_API_TOKEN in cldr_dynamic_prompter.py", file=sys.stderr)
        sys.exit(2)
    return JIRA(server=JIRA_SERVER, basic_auth=(JIRA_USER_EMAIL, JIRA_API_TOKEN))

def _get_match(pattern: str, text: str, flags=0, default: str = "") -> str:
    m = re.search(pattern, text, flags)
    return m.group(1).strip() if m else default

def parse_report_text(report_text: str) -> Optional[Dict[str, str]]:
    """Extract fields from cldr_ticket_reader.py's report string."""
    try:
        title = _get_match(r"^Title:\s*(.*)$", report_text, re.MULTILINE)
        reporter = _get_match(r"^Reporter:\s*(.*)$", report_text, re.MULTILINE, "N/A")
        priority = _get_match(r"^Priority:\s*(.*)$", report_text, re.MULTILINE, "N/A")
        components = _get_match(r"^Components:\s*(.*)$", report_text, re.MULTILINE, "None")
        labels = _get_match(r"^Labels:\s*(.*)$", report_text, re.MULTILINE, "None")
        desc = _get_match(r"Description:\s*\n---\n(.*?)\n---", report_text, re.DOTALL, "")
        code_blocks = re.findall(r"\{code.*?\}(.*?)\{code\}", desc, re.DOTALL)
        has_code = bool(code_blocks)
        return {
            "title": title,
            "description": desc,
            "reporter": reporter,
            "priority": priority,
            "components": components,
            "labels": labels,
            "has_code": has_code,
        }
    except Exception:
        return None

def fetch_ticket_data(ticket_key: str, jira_client: JIRA) -> Dict[str, str]:
    """Use cldr_ticket_reader.get_ticket_details_string, then parse."""
    report = get_ticket_details_string(ticket_key, jira_client)
    data = parse_report_text(report)
    if data:
        return data

    # fallback
    issue = jira_client.issue(ticket_key)
    title = getattr(issue.fields, "summary", "") or ""
    description = getattr(issue.fields, "description", "") or ""
    components = ", ".join([c.name for c in (issue.fields.components or [])]) or "None"
    labels = ", ".join(issue.fields.labels or []) or "None"
    reporter = getattr(getattr(issue.fields, "reporter", None), "displayName", "N/A")
    priority = getattr(getattr(issue.fields, "priority", None), "name", "N/A")
    has_code = bool(re.search(r"\{code.*?\}", description or ""))
    return {
        "title": title.strip(),
        "description": description.strip(),
        "reporter": reporter,
        "priority": priority,
        "components": components,
        "labels": labels,
        "has_code": has_code,
    }

# -----------------------------
# Optional: auto category via LLM
# -----------------------------

def auto_pick_category(title: str, description: str) -> str:
    if not OPENAI_API_KEY:
        return "Triage"
    try:
        from openai import OpenAI
        client = OpenAI(api_key=OPENAI_API_KEY)
        prompt = (
            "Pick ONE category only from: "
            + ", ".join(f'"{c}"' for c in CATEGORIES)
            + ". Return just the category string.\n\n"
            f"Title: {title}\nDescription: {description}\n"
        )
        resp = client.chat.completions.create(
            model=OPENAI_MODEL,
            messages=[{"role": "user", "content": prompt}],
            temperature=0,
        )
        cat = (resp.choices[0].message.content or "").strip().strip('"')
        return cat if cat in CATEGORIES else "Triage"
    except Exception as e:
        print(f"Auto-category failed, falling back to 'Triage': {e}", file=sys.stderr)
        return "Triage"

# -----------------------------
# Topic detection (adds variety)
# -----------------------------

def _norm_list_field(s: str) -> List[str]:
    if not s or s == "None":
        return []
    return [x.strip().lower() for x in s.split(",") if x.strip()]

def detect_topic(data: Dict[str, str]) -> str:
    title = (data.get("title") or "").lower()
    desc = (data.get("description") or "").lower()
    components = _norm_list_field(data.get("components"))
    labels = _norm_list_field(data.get("labels"))
    text = " ".join([title, desc] + components + labels)

    if any(k in text for k in ["intervalformatfallback", "datetime", "date time", "date-time", "skeleton", "pattern", "quotes", "apostrophe"]):
        return "Date/Time Patterns"
    if "icu" in text or any(k in labels for k in ["icu"]):
        return "ICU/Integration"
    if any(k in text for k in ["unit", "measurement", "unitpreference", "measurement system"]):
        return "Units"
    if any(k in text for k in ["plural", "pluralrules", "plural rules"]):
        return "Plural Rules"
    if any(k in text for k in ["collation", "sort order", "uca"]):
        return "Collation"
    if any(k in text for k in ["annotation", "emoji"]):
        return "Emoji/Annotations"
    if any(k in text for k in ["rbnf", "rule-based number", "spellout"]):
        return "RBNF"
    return "General/Locale Data"

# -----------------------------
# Prompt builders
# -----------------------------

def build_triage_prompt(data: Dict[str, str]) -> str:
    return f"""You are assisting CLDR ticket triage.

Ticket
Title: {data['title']}
Description: {data['description']}

Context
Reporter: {data['reporter']} | Priority: {data['priority']}
Components: {data['components']} | Labels: {data['labels']}
HasCodeBlock: {str(data['has_code']).lower()}

Task
Classify and return STRICT JSON with these keys:
{{
  "spam": true|false,
  "outOfScopeForCLDR": true|false,
  "needsEngineeringTC": true|false,
  "needsLanguageSpecialist": true|false,
  "stretchValidation": {{
    "likelyTrue": true|false,
    "evidenceLinks": ["..."]
  }},
  "changeTaskTypeToFixInSurveyTool": true|false,
  "notes": "1-3 sentences"
}}
Rules
- spam: no meaningful info or advertisement.
- outOfScopeForCLDR: e.g., emoji image design (not CLDR data).
- Be concise; output ONLY valid JSON.
"""

def build_topic_prompt(category: str, topic: str, data: Dict[str, str]) -> str:
    base = f"""Analyze the ticket and answer succinctly.

Ticket
Title: {data['title']}
Description: {data['description']}

Context
Reporter: {data['reporter']} | Priority: {data['priority']}
Components: {data['components']} | Labels: {data['labels']}
HasCodeBlock: {str(data['has_code']).lower()}

Detected
Category: {category}
Topic: {topic}
"""
    topic_asks = {
        "Date/Time Patterns": (
            "Questions\n"
            "1) Which LDML/CLDR rule applies (cite section name)?\n"
            "2) Provide a corrected minimal example.\n"
            "3) Remove or escape curly quotes/apostrophes?\n"
            "4) Any ICU impact/doc reference?\n"
            "Return short bullets."
        ),
        "ICU/Integration": (
            "Questions\n"
            "1) Which CLDR data groups ICU consumes here?\n"
            "2) What ICU does NOT focus on (if noted)?\n"
            "3) 2–4 bullets for doc outline + owners.\n"
            "Return short bullets."
        ),
        "Units": (
            "Questions\n"
            "1) Which unit(s)/system?\n"
            "2) Preference, formatting, or conversion?\n"
            "3) One verification (locale + example).\n"
            "Return short bullets."
        ),
        "Plural Rules": (
            "Questions\n"
            "1) Locales/categories?\n"
            "2) Logic vs examples?\n"
            "3) 1–2 minimal tests.\n"
            "Return short bullets."
        ),
        "Collation": (
            "Questions\n"
            "1) Tailorings implicated?\n"
            "2) 3–5 sample order.\n"
            "3) Root vs locale tailoring?\n"
            "Return short bullets."
        ),
        "Emoji/Annotations": (
            "Questions\n"
            "1) Annotation text vs image/design (image is out of scope)?\n"
            "2) Locales impacted?\n"
            "3) One-line next step.\n"
            "Return short bullets."
        ),
        "RBNF": (
            "Questions\n"
            "1) Which ruleset(s)?\n"
            "2) Minimal before/after snippet.\n"
            "3) Example input + expected output.\n"
            "Return short bullets."
        ),
        "General/Locale Data": (
            "Questions\n"
            "1) Which locales and data types?\n"
            "2) Is this accuracy, documentation, or feature scope?\n"
            "3) Concise next step (owner + artifact).\n"
            "Return short bullets."
        ),
    }
    return base + topic_asks.get(topic, topic_asks["General/Locale Data"])

# -----------------------------
# PUBLIC API for other tools
# -----------------------------

def make_prompt(ticket_key: str, category: Optional[str] = None, auto_category: bool = False) -> str:
    """
    Return a ready-to-send prompt string for the given ticket.
    Other tools import and call this.
    """
    jira = get_jira_client_from_config()
    data = fetch_ticket_data(ticket_key, jira)

    if category:
        chosen = category
    elif auto_category:
        chosen = auto_pick_category(data["title"], data["description"])
    else:
        chosen = "Triage"

    if chosen == "Triage":
        return build_triage_prompt(data)

    topic = detect_topic(data)
    return build_topic_prompt(chosen, topic, data)

# -----------------------------
# CLI (prints the prompt)
# -----------------------------

def main():
    parser = argparse.ArgumentParser(
        prog="cldr_dynamic_prompter",
        description="Print a ticket-specific LLM prompt for a CLDR JIRA ticket."
    )
    parser.add_argument("ticket_key", help="e.g., CLDR-12345")
    parser.add_argument("--category", choices=list(CATEGORIES) + ["Triage"])
    parser.add_argument("--auto-category", action="store_true")
    args = parser.parse_args()

    prompt = make_prompt(args.ticket_key, category=args.category, auto_category=args.auto_category)
    print(prompt)

if __name__ == "__main__":
    main()
