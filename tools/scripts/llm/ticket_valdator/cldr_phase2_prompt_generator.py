#!/usr/bin/env python3
"""Print a concise Phase-2 routing prompt for a CLDR ticket."""
import re, sys, argparse
from typing import Dict, Optional, List
from jira import JIRA
from cldr_ticket_reader import get_ticket_details_string

# --- Paste Jira creds (no env vars needed) ---
JIRA_SERVER = "https://unicode-org.atlassian.net"
JIRA_USER_EMAIL = "YOUR MAIL"  # <-- Replace with your email
JIRA_API_TOKEN = "JIRA API KEY"    # <-- Replace with your API token

# ---------------------------------------------

# Allowed sets used in the prompt (keep short/clear)
ROUTING_GROUPS = [
  "Engineering-SurveyTool-UI","Engineering-Data","QA","Localization",
  "LanguageSpecialist","ICU","Spec-Editors","TC-Review"
]
COMPONENT_SET = [
  "Units","Plural Rules","Locale Data","Date/Time Formats","Collation",
  "Emoji/Annotations","ICU Integration","RBNF","Numbering Systems","Transforms"
]
PRIORITY_SET = ["major","major-stretch","medium","minor"]  # blocks-* only TC

def _pick(pat: str, txt: str, flags=0, default="") -> str:
    m = re.search(pat, txt, flags);  return m.group(1).strip() if m else default

def _clean_desc(text: str) -> str:
    if not text: return ""
    text = re.sub(r"\{noformat\}.*?\{/?noformat\}", " ", text, flags=re.I|re.S)
    text = re.sub(r"\{code.*?\}.*?\{/?code\}", " ", text, flags=re.I|re.S)
    text = re.sub(r"\s+", " ", text).strip()
    return text

def _truncate(text: str, limit: int) -> str:
    if len(text) <= limit: return text
    return text[:limit].rsplit(" ", 1)[0] + "…"

def _bullets(block: str) -> List[str]:
    if not block: return []
    lines = []
    for ln in block.splitlines():
        t = ln.strip()
        if t and not t.lower().startswith("description"):
            t = t.lstrip("-• ").strip()
            if t: lines.append(t)
    return lines

def parse_report(report: str) -> Optional[Dict[str,str]]:
    try:
        return {
            "title": _pick(r"^Title:\s*(.*)$", report, re.M),
            "reporter": _pick(r"^Reporter:\s*(.*)$", report, re.M, "N/A"),
            "priority": _pick(r"^Priority:\s*(.*)$", report, re.M, "N/A"),
            "components": _pick(r"^Components:\s*(.*)$", report, re.M, "None"),
            "labels": _pick(r"^Labels:\s*(.*)$", report, re.M, "None"),
            "links": _bullets(_pick(r"Connected Work Items:\n(.*?)\n\nDescription:", report, re.S, "")),
            "description": _pick(r"Description:\s*\n---\n(.*?)\n---", report, re.S, ""),
        }
    except Exception:
        return None

def build_phase2_prompt(ticket_key: str, jc: JIRA, max_desc: int = 380) -> str:
    report = get_ticket_details_string(ticket_key, jc)
    d = parse_report(report)
    if not d:
        raise RuntimeError("Could not parse ticket details.")
    desc = _truncate(_clean_desc(d["description"]), max_desc)
    links = ", ".join(d["links"]) if d["links"] else "None"

    preface = (
        "You are a CLDR triager. Route this ticket (Phase-2) and return ONLY JSON.\n"
        "Fields:\n"
        f"  routing.groups: array from {', '.join(ROUTING_GROUPS)}; include the best primary first\n"
        "  components.present: array from the ticket\n"
        f"  components.additions: array from {', '.join(COMPONENT_SET)} (dedupe; may be empty)\n"
        f"  priority.level: one of {', '.join(PRIORITY_SET)} (never set blocks-* here)\n"
        '  priority.proposeBlock: "none"|"blocks-progress"|"blocks-release" (if you strongly believe so) and set escalateToTC true\n'
        "  priority.escalateToTC: true|false\n"
        "  notes: <= 200 chars\n\n"
        "Ticket\n"
        f"- id: {ticket_key}\n"
        f"- title: {d['title']}\n"
        f"- description: {desc}\n"
        f"- components: {d['components']} | labels: {d['labels']}\n"
        f"- reporter: {d['reporter']} | priority: {d['priority']}\n"
        f"- links: {links}\n\n"
        "Output JSON schema:\n"
    )

    schema = (
        "{\n"
        '  "ticketId": "",\n'
        '  "routing": { "groups": [] },\n'
        '  "components": { "present": [], "additions": [] },\n'
        '  "priority": { "level": "", "proposeBlock": "none", "escalateToTC": false, "reason": "" },\n'
        '  "notes": ""\n'
        "}\n"
    )

    return preface + schema


def main():
    ap = argparse.ArgumentParser(description="Print a concise Phase-2 routing prompt for a CLDR ticket.")
    ap.add_argument("ticket_key", help="e.g., CLDR-18761")
    ap.add_argument("--max-desc", type=int, default=380)
    args = ap.parse_args()
    jc = JIRA(server=JIRA_SERVER, basic_auth=(JIRA_USER_EMAIL, JIRA_API_TOKEN))
    print(build_phase2_prompt(args.ticket_key, jc, args.max_desc))

if __name__ == "__main__":
    main()
