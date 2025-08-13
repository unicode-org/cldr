# LLM CLDR Data Validator

![Language](https://img.shields.io/badge/Language-Python-blue.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

This Python script is an automated tool to evaluate the accuracy of Large Language Models (LLMs) concerning locale-specific unit preferences. It compares the LLM's knowledge against the official Unicode Common Locale Data Repository (CLDR) to verify data conformance.

---

## ✨ Features

* **Interactive Validation**: Accepts natural language questions from the command line.
* **Live LLM Queries**: Uses the OpenAI API (`gpt-4o-mini`) to generate structured JSON data based on the prompt.
* **Ground Truth Comparison**: Validates the LLM's output against a local `unitPreferenceData.json` file from the CLDR.
* **Detailed Reporting**: Produces a final JSON report showing the LLM's response, the official CLDR data, and a unit-by-unit comparison with a "Match" or "Mismatch" status.
* **Fallback Logic**: Correctly handles lookups for regions not explicitly listed in the CLDR data by using the world default (`001`).

---

## 🛠️ Prerequisites

* Python 3.8+
* OpenAI Python library:

  ```bash
  pip install openai
  ```

---

## ⚙️ Setup & Configuration

1. **Place Files**: Ensure the following two files are in the same project directory:

   * `llm_cldr_validator.py` 
   * `unitPreferenceData.json` (The CLDR data file)

2. **Add API Key**: Open `llm_cldr_validator.py` in a text editor. Find the following line and replace the placeholder with your actual OpenAI API key.

   ```python
   # In the generate_data_with_llm function:
   client = OpenAI(api_key="YOUR API KEY")
   ```

> \[!WARNING]
> **Security Alert**: Never commit files with hardcoded API keys to public repositories like GitHub. For production applications, always use environment variables or a dedicated secret manager.

---

## 🚀 How to Run

1. Open your terminal or command prompt.
2. Navigate to the directory containing your files.

   ```bash
   cd path/to/your/project_folder
   ```
3. Execute the script:

   ```bash
   python llm_cldr_validator.py
   ```
4. The script will prompt you to enter a question. Type your question and press Enter.

---

## 📝 Examples

### Example 1: Successful Match

This example shows a straightforward case where the LLM's output perfectly matches the CLDR standard.

**Prompt:**

```bash
 Enter your question about local data: What is the unit for weather temperature in the United States?
```

**Final Validation Report:**

```json
{
    "ValidationInput": {
        "Prompt": "What is the unit for weather temperature in the United States?",
        "LLM_Entity": "United States",
        "LLM_CountryCode": "US",
        "CLDR_Lookup": "Category: 'temperature', Usage: 'weather', Region: 'US'"
    },
    "LLM_Units_Found": [
        "fahrenheit"
    ],
    "CLDR_Units_Found": [
        "fahrenheit"
    ],
    "Comparison": [
        {
            "Unit_1": {
                "LLM_Unit": "fahrenheit",
                "CLDR_Unit": "fahrenheit",
                "Status": "Match"
            }
        }
    ]
}
```

### Example 2: In-Depth Mismatch Analysis

This example showcases the validator's ability to handle complex prompts and identify nuanced differences between an LLM's conversational output and the strict CLDR standard.

**Prompt:**

```bash
 Enter your question about local data: In a United Kingdom, english speaker context, what units are used for measuring human height?
```

**Final Validation Report:**

```json
{
    "ValidationInput": {
        "Prompt": "In a United Kingdom, english speaker context, what units are used for measuring human height?",
        "LLM_Entity": "United Kingdom",
        "LLM_CountryCode": "GB",
        "CLDR_Lookup": "Category: 'length', Usage: 'person-height', Region: 'GB'"
    },
    "LLM_Units_Found": [
        "feet-and-inches",
        "centimeters"
    ],
    "CLDR_Units_Found": [
        "foot-and-inch",
        "inch"
    ],
    "Comparison": [
        {
            "Unit_1": {
                "LLM_Unit": "feet-and-inches",
                "CLDR_Unit": "foot-and-inch",
                "Status": "Mismatch"
            }
        },
        {
            "Unit_2": {
                "LLM_Unit": "centimeters",
                "CLDR_Unit": "inch",
                "Status": "Mismatch"
            }
        }
    ]
}
```

#### Analysis of the Mismatch

This result is a success for the validator, as it highlights key differences:

1. **Subtle Wording**: The LLM used the grammatically natural "feet-and-inches" (plural), while the CLDR standard specifies the canonical unit name "foot-and-inch" (singular).
2. **Preference Order**: The LLM suggested "centimeters" as a logical secondary unit (common in medical settings). However, the official CLDR preference for the UK lists "inch" as the next preferred unit after "foot-and-inch".

This demonstrates the tool's value in catching discrepancies between an LLM's generalized knowledge and a formal data standard.
