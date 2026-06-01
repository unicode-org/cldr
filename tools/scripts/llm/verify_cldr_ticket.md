File: `verify_cldr_ticket.md`

# CLDR Ticket Verification Script

## Overview

`verify_cldr_ticket.py` is a utility that automates gathering and formalizing acceptance criteria for CLDR JIRA tickets. It leverages JIRA’s REST API to fetch rendered HTML descriptions, parses any native “Acceptance Criteria” sections, and uses a lightweight LLM to draft exactly five concise bullet points when none are present.

## Features

- Retrieves rendered ticket descriptions via JIRA’s API
- Parses existing **Acceptance Criteria** or **AC** sections
- Invokes an LLM (Google FLAN-T5) to draft criteria if missing
- Ensures output of exactly five one-sentence bullets
- Loads credentials from a local `.env` file to keep secrets out of source control

## Prerequisites

- Python 3.8 or higher
- Access to the CLDR JIRA instance with API permissions
- Virtual environment support (venv)

## Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/yourusername/cldr-ticket-verifier.git
   cd cldr-ticket-verifier
   ```

2. **Create and activate a virtual environment**:
   ```bash
   python3 -m venv venv
   source venv/bin/activate      # macOS/Linux
   # or
   venv\Scripts\Activate.ps1   # Windows PowerShell
   ```

3. **Install dependencies**:
   ```bash
   pip install python-dotenv beautifulsoup4 transformers torch
   ```

## Configuration

1. **Create a `.env` file** in the project root:
   ```ini
   JIRA_BASE_URL=https://unicode-org.atlassian.net
   JIRA_USER=your-email@domain.com
   JIRA_API_TOKEN=your-jira-api-token
   ```
2. **Ensure** `.env` is listed in `.gitignore` to avoid leaking credentials.

## Usage

Run the verifier script with a ticket ID:

```bash
python verify_cldr_ticket.py CLDR-15694
```

Expected output:

```
Fetching description for CLDR-15694…

── RAW DESCRIPTION ──
<ticket description text>
────────────────────

No native criteria found—drafting criteria via LLM…

Acceptance Criteria (drafted by LLM):
- First concise bullet
- Second concise bullet
- Third concise bullet
- Fourth concise bullet
- Fifth concise bullet
```

## Customization

- **Model**: Change `google/flan-t5-base` to another HF or OpenAI model in `extract_criteria_with_llm`.
- **Bullet Count**: Adjust truncation logic in `extract_criteria_with_llm` for different counts.
- **Parsing**: Tweak regex in `parse_native_criteria` for alternate headings.


