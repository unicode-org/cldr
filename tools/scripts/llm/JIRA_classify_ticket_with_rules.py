import json
import os
import re # Import the regular expressions module
from jira import JIRA

def classify_ticket_with_rules_advanced(title, description):
    """
    Classifies a Jira ticket using an advanced rule-set with
    regex and a scoring system.
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
    scores = {"priority": 0, "sentiment": 0}

    # --- Advanced Rule 1: Use Regex to find duplicate ticket keys ---
    # This pattern looks for "CLDR-" followed by one or more digits.
    duplicate_match = re.search(r"cldr-\d+", text)
    if duplicate_match:
        classification["is_potential_duplicate"] = True
        # .group(0) gets the matched text, .upper() makes it standard
        classification["potential_duplicate_of"] = duplicate_match.group(0).upper()

    # --- Advanced Rule 2: Scoring System for Priority ---
    # Assign points based on keywords.
    priority_keywords = {
        "critical": ["critical", "crash", "urgent", "blocker", "unusable"],
        "high": ["important", "high priority", "major", "severe"],
        "low": ["trivial", "minor", "cosmetic", "suggestion"]
    }
    for word in priority_keywords["critical"]:
        if word in text: scores["priority"] += 20
    for word in priority_keywords["high"]:
        if word in text: scores["priority"] += 10
    for word in priority_keywords["low"]:
        if word in text: scores["priority"] -= 5
        
    # --- Advanced Rule 3: Simple Sentiment Scoring ---
    sentiment_keywords = {
        "positive": ["great", "love", "helpful", "like", "suggestion"],
        "negative": ["frustrating", "confusing", "broken", "hate", "fails"]
    }
    for word in sentiment_keywords["positive"]:
        if word in text: scores["sentiment"] += 1
    for word in sentiment_keywords["negative"]:
        if word in text: scores["sentiment"] -= 1

    # --- Rule 4: Basic Keyword Matching (can still be used) ---
    if scores["sentiment"] < 0 or any(w in text for w in ["error", "bug"]):
        classification["ticket_type"] = "bug"
    elif any(w in text for w in ["add", "create", "implement", "feature"]):
        classification["ticket_type"] = "feature"
        
    # --- Make final decisions based on scores ---
    if scores["priority"] > 15:
        classification["priority"] = "critical"
    elif scores["priority"] > 5:
        classification["priority"] = "high"
    elif scores["priority"] < 0:
        classification["priority"] = "low"
    else:
        classification["priority"] = "medium"
        
    # --- Set routing based on ticket type ---
    if classification["ticket_type"] == "bug":
        classification["routing_group"] = "QA Team"
    else:
        classification["routing_group"] = "CLDR Core Team"

    # --- Final structure ---
    return {
        "classification_status": "valid_ticket",
        "details": classification
    }


# (Configuration and Jira Connection logic)
JIRA_SERVER = os.getenv('JIRA_SERVER', 'https://unicode-org.atlassian.net')
JIRA_USER_EMAIL = os.getenv('JIRA_USER_EMAIL', 'your-email@example.com')
JIRA_API_TOKEN = os.getenv('JIRA_API_TOKEN', 'your_api_token_here')


if not all([JIRA_SERVER, JIRA_USER_EMAIL, JIRA_API_TOKEN]):
    print(" Error: One or more environment variables are not set.")
else:
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

        print("\n" + "="*50)
        print("--- Dynamic Advanced Rule-Based Classification ---")
        print("="*50)
        
        advanced_classification = classify_ticket_with_rules_advanced(ticket_summary, ticket_description)
        
        print(json.dumps(advanced_classification, indent=2))

    except Exception as e:
        print(f" An error occurred: {e}")
