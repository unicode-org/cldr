import os
import sys
import json
from jira import JIRA

def get_ticket_details_string(ticket_key: str, jira_client: JIRA) -> str:
    """
    Fetches comprehensive details for a Jira ticket and formats them
    into a single, human-readable report string.
    """
    try:
        # Fetch the full issue object from Jira
        issue = jira_client.issue(ticket_key, expand="changelog")

        # --- Extract Core Jira Data ---
        title = issue.fields.summary
        reporter = issue.fields.reporter.displayName if issue.fields.reporter else "N/A"
        priority = issue.fields.priority.name if issue.fields.priority else "N/A"
        labels = ", ".join(issue.fields.labels) if issue.fields.labels else "None"
        components = ", ".join([c.name for c in issue.fields.components]) if issue.fields.components else "None"
        description = issue.fields.description if issue.fields.description else "No description provided."
        
        # Extract connected work items (issue links)
        connected_items = []
        if issue.fields.issuelinks:
            for link in issue.fields.issuelinks:
                if hasattr(link, 'outwardIssue'):
                    connected_items.append(f"  - {link.type.outward}: {link.outwardIssue.key}")
                elif hasattr(link, 'inwardIssue'):
                    connected_items.append(f"  - {link.type.inward}: {link.inwardIssue.key}")
        
        connected_items_str = "\n".join(connected_items) if connected_items else "  - None"

        # --- Combine all extracted data into a single report ---
        details_report = (
            f"=======================================================\n"
            f"            Jira Ticket Report: {ticket_key}\n"
            f"=======================================================\n\n"
            f"Title:       {title}\n"
            f"Reporter:    {reporter}\n"
            f"Priority:    {priority}\n"
            f"Components:  {components}\n"
            f"Labels:      {labels}\n\n"
            f"Connected Work Items:\n{connected_items_str}\n\n"
            f"Description:\n---\n{description}\n---\n"
            f"=======================================================\n"
        )
        return details_report

    except Exception as e:
        return f"Error fetching or processing ticket {ticket_key}: {e}"

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python cldr_ticket_reader.py <TICKET_KEY>")
        sys.exit(1)

    ticket_key_to_fetch = sys.argv[1]

    # --- Configuration with Hardcoded Values ---
    #  Warning: This is a security risk. Best practice is to use environment variables.
    JIRA_SERVER = "https://unicode-org.atlassian.net"
    JIRA_USER_EMAIL = "YOU MAIL!!!"  # <-- Replace with your email
    JIRA_API_TOKEN = "YOUR API KEY"    # <-- Replace with your API token

    try:
        jira_client = JIRA(server=JIRA_SERVER, basic_auth=(JIRA_USER_EMAIL, JIRA_API_TOKEN))
        ticket_report = get_ticket_details_string(ticket_key_to_fetch, jira_client)
        print(ticket_report)

    except Exception as e:
        print(f"An error occurred during connection: {e}")
