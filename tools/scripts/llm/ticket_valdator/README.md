# Phase 1 — CLDR Ticket Validator (Reader · Prompt Preparer · Prompt → LLM)

This folder contains the Phase-1 tools used to **read a CLDR JIRA ticket**, **prepare a short LLM prompt**, and **post that prompt to an LLM** to get a JSON triage result.

Path: `tools/scripts/llm/ticket_valdator/`

---

## Files

* `cldr_ticket_reader.py` — reads a JIRA ticket and prints a human-readable report

  * Also exposes `get_ticket_details_string(ticket_key, jira_client)` for other tools.
* `cldr_dynamic_prompter.py` — **prepares** a concise, ticket-specific prompt

  * CLI prints the prompt; also exposes `make_prompt(ticket_key, category=None, auto_category=False)`.
* `cldr_prompt_to_llm.py` — **posts** the prepared prompt to OpenAI and prints the model’s reply (JSON).

> Keep it simple: three files, three responsibilities — **reader → prompt → LLM**.

---

## Requirements

* Python 3.9+
* Packages:

  ```bash
  pip install jira "openai>=1.0.0"
  ```
* Access to the CLDR JIRA (`https://unicode-org.atlassian.net`)
* (For posting to an LLM) An OpenAI API key

---

## Quick Setup

Open each script and paste credentials as indicated at the top (you can use env vars if you prefer; avoid committing real keys):

* **JIRA**
  `JIRA_SERVER`, `JIRA_USER_EMAIL`, `JIRA_API_TOKEN`
* **OpenAI** (only needed for auto-category in the prompter, and for the poster)
  `OPENAI_API_KEY` (and optionally `OPENAI_MODEL`, default `gpt-4o-mini`)

---

## Usage

### 1) Read a ticket (human-readable report)

```bash
python cldr_ticket_reader.py CLDR-18761
```

### 2) Prepare a prompt (print to stdout)

Default (Phase-1 triage, no category forced):

```bash
python cldr_dynamic_prompter.py CLDR-18761
```

Force a category (adds targeted questions):

```bash
python cldr_dynamic_prompter.py CLDR-18761 --category "Documentation Issue"
```

Auto-pick a category (one lightweight LLM call):

```bash
python cldr_dynamic_prompter.py CLDR-18761 --auto-category
```

> Programmatic use in other tools:
>
> ```python
> from cldr_dynamic_prompter import make_prompt
> prompt = make_prompt("CLDR-18761", category="Software Bug")  # or auto_category=True
> ```

### 3) Post the prompt to OpenAI (get JSON)

```bash
python cldr_prompt_to_llm.py CLDR-18761
# or
python cldr_prompt_to_llm.py CLDR-18761 --category "Feature Request"
# or
python cldr_prompt_to_llm.py CLDR-18761 --auto-category --model gpt-4o-mini
```

The script prints only the model reply; it also tries to trim to the first valid `{ ... }` block if extra text appears.

---

## Editing the Prompt (user-friendly)

Most users will only edit these two functions in **`cldr_dynamic_prompter.py`**:

* `build_triage_prompt(data)` — the default Phase-1 JSON classification prompt
* `build_topic_prompt(category, topic, data)` — short follow-ups when a category is chosen

**Tip:** Keep descriptions short; Title/Description appear once. If you need a different tone, edit just these functions. (In a later phase we can move the template into a separate `templates/` file and add a `--template` flag.)

---

## Expected JSON (Phase-1)

The LLM output should match keys like:

```json
{
  "spam": false,
  "outOfScopeForCLDR": false,
  "needsEngineeringTC": true,
  "needsLanguageSpecialist": false,
  "stretchValidation": { "likelyTrue": true, "evidenceLinks": [] },
  "changeTaskTypeToFixInSurveyTool": true,
  "notes": "1–3 brief sentences"
}
```

---

## Troubleshooting

* **401 / auth error:** Recheck JIRA email/token; ensure your account can read the ticket.
* **OpenAI call failed:** Paste a valid `OPENAI_API_KEY` or remove `--auto-category` and skip the poster tool.
* **Messy model output:** `cldr_prompt_to_llm.py` already extracts the first JSON block; tighten your prompt if needed.
* **Classification feels generic:** Use `--category` to add targeted questions or refine `build_topic_prompt`.

---

## Security

* **Do not commit real tokens/API keys** to the repo.
* Prefer environment variables or a local config file ignored by git.
* Rotate any tokens that may have been shared accidentally.

---

## Summary

Phase-1 tools are ready:

1. **Reader**: get ticket details
2. **Prompter**: print a concise, LLM-ready prompt
3. **Poster**: send to OpenAI and print JSON

Small, composable, and easy to tweak.
