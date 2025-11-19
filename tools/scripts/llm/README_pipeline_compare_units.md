# pipeline\_compare\_units

This README is for `pipeline_compare_units.py` and provides setup, usage, and extension details.

A complete end-to-end pipeline for extracting locale-specific unit preferences from CLDR data, predicting the same with GPT-4, and comparing the two outputs automatically.

## Features

* **CLDR Extraction**: Parses `units.xml` to extract preferred units for **person height** and **road distance** by ISO country code.
* **LLM Prompting**: Builds neutral prompts (no CLDR jargon) and queries GPT-4 for each measurement type.
* **Parsing & Normalization**: Converts GPT-4’s bullet-list responses into JSON, mapping full country names to ISO-2 codes.
* **Automated Comparison**: Constructs a “compare these” prompt with both CLDR and LLM JSON and receives a human-readable match/mismatch report from GPT-4.

---

## Prerequisites

* Python 3.7 or later
* `openai` Python client (v1.x)
* CLDR `units.xml` (placed alongside the script)
* CLDR units.xml file link: [https://github.com/unicode-org/cldr/blob/main/common/supplemental/units.xml](https://github.com/unicode-org/cldr/blob/main/common/supplemental/units.xml)
* An OpenAI API key with access to GPT-4

---

## Installation

1. Clone this repository and navigate to the project root:

   ```bash
   git clone <repo_url>
   cd <repo_folder>
   ```
2. (Optional) Create and activate a virtual environment:

   ```bash
   python3 -m venv .venv
   source .venv/bin/activate    # macOS/Linux
   .\.venv\Scripts\activate   # Windows PowerShell
   ```
3. Install dependencies:

   ```bash
   pip install openai
   ```
4. Place your CLDR `units.xml` in the root directory next to the script.

---

## Configuration

* The script **hard-codes** the OpenAI API key for quick testing:

  ```python
  client = OpenAI(api_key="sk-XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX")
  ```

  **⚠️** Before committing, replace with:

  ```python
  client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))
  ```

  and set the environment variable:

  ```bash
  export OPENAI_API_KEY="sk-..."
  ```

---

## Usage

Run the pipeline with a single command:

```bash
python pipeline_compare_units.py
```

This generates six output files:

* `person_height_units_cldr.json`
* `person_height_units_llm.json`
* `road_distance_units_cldr.json`
* `road_distance_units_llm.json`
* `person_height_comparison.txt`
* `road_distance_comparison.txt`

Open the `.json` files to inspect extracted and predicted unit maps. Read the `.txt` reports for detailed match/mismatch explanations.

---

## File Overview

* **pipeline\_compare\_units.py**: Main script implementing extraction, prompting, parsing, and comparison.
* **units.xml**: CLDR supplemental data file containing unit preference definitions.
* **\*.json**, **\*.txt**: Generated outputs from the pipeline.

---

## Customization & Extension

* **Add more measurements**: Modify `COUNTRY_NAME_TO_CODE` and `build_prompt(...)` to include additional categories (e.g., temperature, speed).
* **Change models**: Switch `model="gpt-4"` to another OpenAI engine (e.g., `gpt-3.5-turbo`).
* **Error handling**: Add try/except blocks around API calls and XML parsing.
* **CI Integration**: Add automated jobs to flag any deviations when CLDR updates its unit preferences.

---
