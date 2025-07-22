# display\_name\_criteria\_validator

Validates CLDR unit display names by ensuring:

1. **All lengths** (`long`, `short`, `narrow`) exist.
2. **All plural categories** (e.g. `zero`, `one`, `two`, `few`, `many`, `other` for English) are covered.
3. **Each pattern value** is non‑empty and contains the `{0}` placeholder.
4. **Optional LLM quality scoring**: patterns can be scored as `GOOD` or `BAD` via a small instruction‑tuned model.

---

## Features

- **Fallback Inheritance**: walks the CLDR locale fallback chain (`e.g. en-US → en → root`) to detect inherited vs defined patterns.
- **Criteria Checks**: reports missing lengths, missing categories, malformed patterns.
- **Value Validation**: ensures every pattern includes the `{0}` placeholder.
- **LLM Quality**: optional `--use-llm` flag to annotate each pattern with a `GOOD`/`BAD` verdict.
- **Human‑Readable Summary**: prints a concise three‑point summary of criteria results.

---

## Installation

1. Clone the CLDR JSON repo (if you want fallback support):

   ```bash
   git clone https://github.com/unicode-org/cldr-json.git
   ```

2. Install Python dependencies:

   ```bash
   pip install transformers torch sentencepiece
   ```

---

## Usage

Save the script as `display_name_criteria_validator.py` alongside or pointed at your CLDR data.

### Basic criteria checks only

```bash
python display_name_criteria_validator.py length-meter --json-path units.json
```

### With full CLDR fallback tree

```bash
python display_name_criteria_validator.py length-meter --tree-root cldr-json/cldr-units-full/main
```

### Enable LLM quality scoring

```bash
python display_name_criteria_validator.py length-meter \
  --tree-root cldr-json/cldr-units-full/main \
  --use-llm --model google/flan-t5-base --device 0
```

---

## Arguments

- `unit_key` (positional): CLDR unit key, e.g. `length-meter`.
- `-l, --locale` (default: `en`): Locale code to validate.
- `-j, --json-path`: Path to a single `units.json` file.
- `-t, --tree-root`: Root of `cldr-units-full/main` for fallback chain.
- `--use-llm`: Enable LLM pattern quality checks.
- `--model`: Hugging Face model name (default: `google/flan-t5-base`).
- `--device`: Device index for model (`-1` for CPU, `0`+ for GPU).

---

## Example Output

```json
{
  "lengths": {"long": {"status": "defined"}, ...},
  "categories": {
    "long": {
      "one": {"status": "defined", "pattern": "{0} meter", "value_ok": true, "llm_quality": "GOOD"},
      "zero": {"status": "missing", "expected_key": "unitPattern-count-zero", "explanation": "'unitPattern-count-zero' not found in fallback chain"},
      ...
    },
    ...
  }
}
```

---


