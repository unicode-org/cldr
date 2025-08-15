import sys
import os
import re
import subprocess
from openai import OpenAI

def run_ticket_reader(ticket_key: str) -> str:
    """
    Executes the cldr_ticket_reader.py script as a subprocess
    and captures its standard output.
    """
    print(f"--- Step 1: Running cldr_ticket_reader.py for {ticket_key} ---", file=sys.stderr)
    try:
        command = [sys.executable, "cldr_ticket_reader.py", ticket_key]
        result = subprocess.run(command, capture_output=True, text=True, check=True)
        return result.stdout
    except FileNotFoundError:
        print(" Error: cldr_ticket_reader.py not found in the same directory.", file=sys.stderr)
        return None
    except subprocess.CalledProcessError as e:
        print(f" Error running cldr_ticket_reader.py:\n{e.stderr}", file=sys.stderr)
        return None

def parse_report_text(report_text: str) -> dict:
    """Parses the report text to extract title and description."""
    try:
        title = re.search(r"Title:\s*(.*)", report_text).group(1).strip()
        description = re.search(r"Description:\n---\n(.*?)\n---", report_text, re.DOTALL).group(1).strip()
        return {"title": title, "description": description}
    except AttributeError:
        print(" Error: Could not parse the report from cldr_ticket_reader.py.", file=sys.stderr)
        return None

def pre_classify_ticket_with_llm(ticket_data: dict, client: OpenAI) -> str:
    """Uses a first LLM call to determine the ticket's primary category."""
    print("--- Step 2: Asking OpenAI to pre-classify the ticket ---", file=sys.stderr)
    prompt = f"""
    Analyze the following Jira ticket title and description. Classify the ticket into ONE of the following categories:
    "Data Accuracy", "Documentation Issue", "Software Bug", "Feature Request"

    Title: {ticket_data['title']}
    Description: {ticket_data['description']}

    Return ONLY the category name as a single string.
    """
    try:
        response = client.chat.completions.create(
            model="gpt-3.5-turbo",
            messages=[{"role": "user", "content": prompt}]
        )
        return response.choices[0].message.content.strip().replace('"', '')
    except Exception as e:
        print(f"Error during pre-classification: {e}", file=sys.stderr)
        return "Unknown"

def generate_natural_prompt(category: str, ticket_data: dict) -> str:
    """Selects a template based on the category and generates a natural prompt."""
    print(f"--- Step 3: Generating a '{category}' specific prompt ---", file=sys.stderr)
    title = ticket_data['title']
    description = ticket_data['description']
    
    prompt_templates = {
        "Data Accuracy": f"""
Analyze the following Jira ticket, which appears to be about a data accuracy issue.

**Ticket Title**: {title}
**Description**: {description}

Based on the description, please answer the following:
1. What specific piece of data is reported as incorrect?
2. Does the reporter provide a suggested correction or evidence?
3. Is a language specialist needed to verify this?
""",
        "Documentation Issue": f"""
Analyze the following Jira ticket, which appears to be a documentation or specification issue.

**Ticket Title**: {title}
**Description**: {description}

Based on the description, please answer the following:
1. What is the core inconsistency or problem in the documentation?
2. What part of the specification does this affect?
3. Summarize the proposed change to improve clarity.
""",
        "Software Bug": f"""
Analyze the following Jira ticket, which appears to be a software bug report.

**Ticket Title**: {title}
**Description**: {description}

Based on the description, please extract the following information:
1. What is the expected behavior of the software?
2. What is the actual, incorrect behavior?
3. Are there clear steps to reproduce the bug? If so, list them.
""",
        "Feature Request": f"""
Analyze the following Jira ticket, which appears to be a new feature request.

**Ticket Title**: {title}
**Description**: {description}

Based on the description, please answer the following:
1. What is the new capability being requested?
2. What problem does this new feature solve for the user?
3. Who are the primary users who would benefit from this?
"""
    }
    
    return prompt_templates.get(category, f"Please provide a general summary for the following ticket:\n\nTitle: {title}\nDescription: {description}")

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python cldr_dynamic_prompter.py <TICKET_KEY>", file=sys.stderr)
        sys.exit(1)

    ticket_key_to_fetch = sys.argv[1]
    
    # --- Configuration with Hardcoded API Key ---
    #  Warning: This is a security risk. Do not share this file with your real API key.
    OPENAI_API_KEY = "YOUR API KEY!!!"  # <-- PASTE YOUR OPENAI API KEY HERE

    # Check if the user has replaced the placeholder value
    if not OPENAI_API_KEY or "your_openai_api_key" in OPENAI_API_KEY:
        print(" Error: Please replace the placeholder OpenAI API key in the script before running.", file=sys.stderr)
        sys.exit(1)

    try:
        report_text = run_ticket_reader(ticket_key_to_fetch)
        
        if report_text:
            parsed_data = parse_report_text(report_text)
            
            if parsed_data:
                openai_client = OpenAI(api_key=OPENAI_API_KEY)
                ticket_category = pre_classify_ticket_with_llm(parsed_data, openai_client)
                final_prompt = generate_natural_prompt(ticket_category, parsed_data)
                print(final_prompt)
            
    except Exception as e:
        print(f" An overall error occurred: {e}", file=sys.stderr)
