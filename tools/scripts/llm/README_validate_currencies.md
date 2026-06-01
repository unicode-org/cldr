# README for `validate_currencies.py`

## Overview

`validate_currencies.py` is a command-line tool that performs AI‑powered sanity checks on CLDR currency data. It fetches the currency definitions for a specified locale, analyzes them with a large language model (LLM), and writes a JSON report summarizing:

* **Check correctness:** Verifies display names and symbols.
* **Find missing symbols:** Identifies currency codes missing `symbol` or `symbol-alt-narrow` entries.
* **Count entries:** Provides a local count of currency lines.

## Prerequisites

* **Python 3.7+**
* **pip** for installing dependencies

## Installation

1. **Clone or download** the CLDR repository (or ensure you have access to the `cldr-json` data).
2. **Install Python dependencies**:

   ```bash
   pip install transformers requests
   ```

## Usage

```bash
python validate_currencies.py [locale] [output.json]
```

* **`locale`**: CLDR locale code (e.g., `en-001`, `fr-CA`). Defaults to `en-001`.
* **`output.json`**: Path to write the JSON report. Defaults to `currencies_report.json`.

Example:

```bash
python validate_currencies.py en-001 en_report.json
```

## Data Source

The script downloads currency data from the CLDR JSON repository using:

```
https://raw.githubusercontent.com/unicode-org/cldr-json/main/
  cldr-json/cldr-numbers-full/main/<locale>/currencies.json
```

For example, for English (World) it is:

```
https://raw.githubusercontent.com/unicode-org/cldr-json/main/
  cldr-json/cldr-numbers-full/main/en-001/currencies.json
```

## Output Format

Results are serialized to JSON with this structure:

```json
{
  "<locale>": {
    "check_correctness": {
      "result": "All correct"
    },
    "find_missing_symbols": {
      "result": "None."
    },
    "count_entries": {
      "python": 307
    }
  }
}
```

* **`check_correctness.result`**: LLM response verifying correctness.
* **`find_missing_symbols.result`**: LLM response listing missing symbols or `None.`
* **`count_entries.python`**: Ground-truth line count.

## How It Works

1. **Fetch & parse** the CLDR `currencies.json` for the given locale.
2. **Flatten** the nested data into human-readable lines: code, names, symbols.
3. **Chunk** lines to avoid exceeding LLM context limits.
4. **Run** two LLM prompts over each chunk to check correctness and find missing symbols.
5. **Count** the lines locally using Python.
6. **Write** a combined JSON report.

## Customization

* **Locale**: Pass any CLDR-supported locale code.
* **Chunk size**: Adjust `chunkify(..., size=N)` to suit different LLM token limits.
* **LLM model**: Modify the `pipeline(..., model=...)` line to use a different transformer.

## License

This tool is licensed under the Unicode License. Please refer to the main CLDR repository for details.
