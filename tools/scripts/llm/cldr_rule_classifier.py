import json
import os
import re
from jira import JIRA

def classify_ticket_with_rules_advanced(title, description):
    """
    Classifies a Jira ticket using an advanced rule-set with
    regex, a scoring system, and specific keyword mapping.
    """
    text = (title + " " + (description or "")).lower()

    # --- Initialize default classification and scoring ---
    classification = {
        "is_cldr_related": True,
        "ticket_type": "task",
        "component": "other",
        "needs_engineering_work": False,
        "needs_language_specialist": False,
        "is_potential_duplicate": False,
        "potential_duplicate_of": None,
        "summary": "Classification based on advanced rules."
    }
    scores = {"priority": 0}

    # --- Rule 1: Use Regex to find duplicate ticket keys ---
    duplicate_match = re.search(r"cldr-\d+", text)
    if duplicate_match:
        classification["is_potential_duplicate"] = True
        classification["potential_duplicate_of"] = duplicate_match.group(0).upper()

    # --- Rule 2: Scoring System for Priority ---
    priority_keywords = {
        "critical": ["critical", "crash", "urgent", "blocker", "unusable"],
        "high": ["important", "major", "severe"],
        "low": ["trivial", "minor", "cosmetic"]
    }
    for word in priority_keywords["critical"]:
        if word in text: scores["priority"] += 20
    for word in priority_keywords["high"]:
        if word in text: scores["priority"] += 10
    for word in priority_keywords["low"]:
        if word in text: scores["priority"] -= 5

    # --- Rule 3: Deduce Component from Keywords ---
    component_keywords = {
        "plural-rules": ["plural", "plurals", "cardinal", "ordinal"],
        "date-time-formats": ["date", "time", "datetime", "timezone", "calendar", "format"],
        "units": ["unit", "measurement", "gallon", "meter", "km", "celsius"],
        "locale-data": ["locale", "country", "language", "territory", "subdivision"],
        "charts": ["chart", "graph", "visualization"]
    }
    for component, keywords in component_keywords.items():
        if any(word in text for word in keywords):
            classification["component"] = component
            break

    # --- Rule 4: Determine Ticket Type ---
    if any(word in text for word in ["error", "bug", "fails", "broken", "crash"]):
        classification["ticket_type"] = "bug"
    elif any(word in text for word in ["add", "create", "implement", "feature", "proposal"]):
        classification["ticket_type"] = "feature"
    elif any(word in text for word in ["improve", "enhance", "update", "refactor"]):
        classification["ticket_type"] = "enhancement"

    # --- Rule 5: Set Routing Group based on content ---
    routing_keywords = {
        "CLDR Design WG": ["structure", "api", "icu", "icu4x", "design", "proposal", "architecture"],
        "PMs": ["data", "accurate", "incorrect", "validate", "country", "language", "locale"]
    }
    classification["routing_group"] = "CLDR Ops"
    if any(word in text for word in routing_keywords["CLDR Design WG"]):
        classification["routing_group"] = "CLDR Design WG"
    elif any(word in text for word in routing_keywords["PMs"]):
        classification["routing_group"] = "PMs"
        
    # --- Make final decisions based on scores ---
    if scores["priority"] > 15:
        classification["priority"] = "critical"
    elif scores["priority"] > 5:
        classification["priority"] = "high"
    elif scores["priority"] < 0:
        classification["priority"] = "low"
    else:
        classification["priority"] = "medium"

    return {
        "classification_status": "valid_ticket",
        "details": classification
    }

# --- Main execution block ---
if __name__ == "__main__":
    # --- Configuration for Jira API Connection ---
    # ⚠ Warning: Hardcoding credentials is a security risk.
    JIRA_SERVER = "https://unicode-org.atlassian.net"
    JIRA_USER_EMAIL = "EMAIL!!"  # <-- Replace with your email
    JIRA_API_TOKEN = "API"    # <-- Replace with your API token

    # --- Get Jira Ticket Key from User Input ---
    TICKET_KEY_TO_FETCH = input("Please enter the Jira Ticket Key (e.g., CLDR-12345): ")
    
    print(f"\n--- Connecting to Jira server at: {JIRA_SERVER} ---")
    try:
        jira_connection = JIRA(server=JIRA_SERVER, basic_auth=(JIRA_USER_EMAIL, JIRA_API_TOKEN))
        print(" Connection successful!")

        print(f"\n--- Fetching ticket: {TICKET_KEY_TO_FETCH} ---")
        issue = jira_connection.issue(TICKET_KEY_TO_FETCH)
        ticket_summary = issue.fields.summary
        ticket_description = issue.fields.description
        print(f"Title: {ticket_summary}")
        print(f"Description: {ticket_description if ticket_description else 'No description found.'}")

        # --- Generate and Print the Dynamic Classification ---
        print("\n" + "="*50)
        print("--- Dynamic Advanced Rule-Based Classification ---")
        print("="*50)
        
        advanced_classification = classify_ticket_with_rules_advanced(ticket_summary, ticket_description)
        
        print(json.dumps(advanced_classification, indent=2))

    except Exception as e:
        print(f" An error occurred: {e}")
