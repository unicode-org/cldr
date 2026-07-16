
import json
import os
from jira import JIRA

# --- Step 1: Determine Classification Criteria (Our 'Rulebook') ---
# This "rulebook" defines the complete structure for the final JSON output.
# An LLM would use this as a schema to generate its classification.
classification_criteria = {
    "classification_status": {
        "type": "string",
        "allowed_values": ["valid_ticket", "spam", "out_of_scope"],
        "description": "The overall status of the ticket after initial review."
    },
    "details": {
        "description": "Contains detailed classification if the ticket is valid. Is null otherwise.",
        "is_cldr_related": {
            "type": "boolean",
            "description": "Is the ticket related to CLDR?"
        },
        "ticket_type": {
            "type": "string",
            "allowed_values": ["bug", "feature", "enhancement", "task", "other"],
            "description": "The type of work required for the ticket."
        },
        "component": {
            "type": "string",
            "allowed_values": ["units", "plural-rules", "locale-data", "date-time-formats", "charts", "other"],
            "description": "The specific CLDR component the ticket relates to."
        },
        "priority": {
            "type": "string",
            "allowed_values": ["critical", "high", "medium", "low"],
            "description": "The urgency of the ticket."
        },
        "needs_engineering_work": {
            "type": "boolean",
            "description": "Does this ticket require work from the technical committee?"
        },
        "needs_language_specialist": {
            "type": "boolean",
            "description": "Does this ticket require validation from a language specialist?"
        },
        "routing_group": {
            "type": "string",
            "allowed_values": ["CLDR Core Team", "Localization Experts", "QA Team", "Technical Committee", "Specific Developer"],
            "description": "The team or group who should handle the ticket next."
        },
        "is_potential_duplicate": {
            "type": "boolean",
            "description": "Is this ticket a potential duplicate of another existing ticket?"
        },
        "potential_duplicate_of": {
            "type": "string",
            "description": "The ticket key (e.g., 'CLDR-1234') this might be a duplicate of. Null if not a duplicate."
        },
        "summary": {
            "type": "string",
            "description": "A brief, one-sentence summary of the ticket's request and the reasoning for the classification."
        }
    }
}


# --- Configuration for Jira API Connection ---
# Securely loads your credentials from environment variables.
JIRA_SERVER = os.getenv('JIRA_SERVER', 'https://unicode-org.atlassian.net')
JIRA_USER_EMAIL = os.getenv('JIRA_USER_EMAIL', 'YOUR JIRA EMAIL')
JIRA_API_TOKEN = os.getenv('JIRA_API_TOKEN', 'YOUR API KEY!')



# --- Get Jira Ticket Key from User Input ---
# Asks the user to enter the ticket key to analyze.
TICKET_KEY_TO_FETCH = input("Please enter the Jira Ticket Key (e.g., CLDR-12345): ")


# --- Script Execution ---
# Main block to connect, fetch, and display information.
if not all([JIRA_SERVER, JIRA_USER_EMAIL, JIRA_API_TOKEN]):
    print(" Error: One or more environment variables (JIRA_SERVER, JIRA_USER_EMAIL, JIRA_API_TOKEN) are not set.")
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


        # --- Display the Example of a Full JSON Output Format ---
        # This shows what a final, AI-generated classification would look like.
        print("\n" + "="*50)
        print("--- Example of Full JSON Output Format ---")
        print("="*50)
        
        full_json_output = {
          "classification_status": "valid_ticket",
          "details": {
            "is_cldr_related": True,
            "ticket_type": "task",
            "component": "other",
            "priority": "medium",
            "needs_engineering_work": True,
            "needs_language_specialist": False,
            "routing_group": "CLDR Core Team",
            "is_potential_duplicate": False,
            "potential_duplicate_of": None,
            "summary": "The ticket contains meeting notes for the CLDR Technical Committee and should be archived as a task."
          }
        }
        
        # Print the complex JSON object in a readable format
        print(json.dumps(full_json_output, indent=2))

    except Exception as e:
        print(f" An error occurred: {e}")
