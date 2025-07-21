#!/usr/bin/env python3
"""
verify_cldr_ticket.py

Fetch a CLDR JIRA ticket’s description (rendered HTML),
try to parse out its “Acceptance Criteria” section,
and if that’s missing, ask an LLM to draft explicit criteria.

Usage:
    python verify_cldr_ticket.py CLDR-15694

Requires a .env file with:
    JIRA_BASE_URL       e.g. https://unicode-org.atlassian.net
    JIRA_USER           your Jira username (email)
    JIRA_API_TOKEN      your Jira API token
"""
from dotenv import load_dotenv
load_dotenv()

import os
import re
import argparse
import requests
from bs4 import BeautifulSoup
from transformers import pipeline


def fetch_ticket_description(ticket_id: str) -> str:
    """Retrieve the plain-text description of a JIRA issue, using rendered HTML."""
    base_url = os.getenv('JIRA_BASE_URL')
    auth     = (os.getenv('JIRA_USER'), os.getenv('JIRA_API_TOKEN'))
    url      = f"{base_url}/rest/api/2/issue/{ticket_id}"
    resp     = requests.get(url, auth=auth, params={'expand': 'renderedFields'})
    resp.raise_for_status()
    data     = resp.json()

    # Try rendered HTML first
    html = data.get('renderedFields', {}).get('description', '')
    if html:
        return BeautifulSoup(html, 'html.parser').get_text(separator='\n').strip()

    # Fallback: Atlassian Document Format (ADF)
    adf = data.get('fields', {}).get('description', '')
    if isinstance(adf, dict) and 'content' in adf:
        def recurse(nodes):
            texts = []
            for node in nodes:
                if node.get('type') == 'text':
                    texts.append(node.get('text', ''))
                if 'content' in node:
                    texts.extend(recurse(node['content']))
            return texts
        return '\n'.join(recurse(adf['content'])).strip()

    # Last-ditch: plain string
    return data.get('fields', {}).get('description', '') or ''


def parse_native_criteria(desc: str) -> str:
    """Extract bullets under “Acceptance Criteria:” or “AC:” if present."""
    m = re.search(r'(?i)(?:Acceptance Criteria|AC):?\s*\n([\s\S]+)', desc)
    if not m:
        return ''
    lines = []
    for line in m.group(1).splitlines():
        line = line.strip()
        if line.startswith(('-', '*')):
            txt = line.lstrip('-* ').strip()
            if txt:
                lines.append(f'- {txt}')
        elif not line or re.match(r'^[A-Z][\w ]+:', line):
            break
    return '\n'.join(lines)


def extract_criteria_with_llm(description: str) -> str:
    """Ask the LLM to generate at least five concise acceptance criteria bullets and truncate to five."""
    llm = pipeline('text2text-generation', model='google/flan-t5-base')
    prompt = (
        "You are an expert software engineer drafting acceptance criteria for a Jira ticket.\n"
        "From the description below, generate five concise bullet points. "
        "Each bullet must start on its own line with '- ' and be no more than one sentence. "
        "Do NOT include any extra text—only the bullets.\n\n"
        f"{description}\n\n"
    )
    result = llm(prompt, max_new_tokens=200, num_return_sequences=1)
    raw = result[0]['generated_text'].strip()
    # Extract lines starting with '- '
    bullets = [line.strip() for line in raw.splitlines() if line.strip().startswith('- ')]
    # If no bullets found, split into sentences as fallback
    if not bullets:
        sentences = re.split(r'(?<=[.!?])\s+', raw)
        bullets = [f'- {s.strip()}' for s in sentences if s.strip()][:5]
    # Truncate to exactly five
    return '\n'.join(bullets[:5])


def main():
    parser = argparse.ArgumentParser(
        description="Fetch CLDR ticket and extract or draft acceptance criteria"
    )
    parser.add_argument('ticket', help="JIRA ticket ID, e.g. CLDR-15694")
    args = parser.parse_args()

    print(f"Fetching description for {args.ticket}…")
    desc = fetch_ticket_description(args.ticket)
    if not desc:
        print("No description found; check the ticket ID or your credentials.")
        return

    print("\n── RAW DESCRIPTION ──")
    print(desc)
    print("────────────────────\n")

    native = parse_native_criteria(desc)
    if native:
        print("Found native Acceptance Criteria:\n")
        print(native)
    else:
        print("No native criteria found—drafting criteria via LLM…\n")
        criteria = extract_criteria_with_llm(desc)
        print("Acceptance Criteria (drafted by LLM):\n")
        print(criteria)

if __name__ == '__main__':
    main()
