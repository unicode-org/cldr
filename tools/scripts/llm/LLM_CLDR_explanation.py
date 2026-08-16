import json
import argparse
from openai import OpenAI

# --- API KEY CONFIGURATION (Define Once) ---
# Paste your single, valid OpenAI API key here.
API_KEY = "YOUR_API_KEY!"

# ==============================================================================
# Part 1: Helper Functions
# ==============================================================================

def normalize_unit(unit_string: str) -> str:
    """Converts a unit string to a consistent format for comparison."""
    if not isinstance(unit_string, str):
        return ""
    return unit_string.lower().replace(" ", "").replace("-", "")

def generate_data_with_llm(user_prompt: str, client: OpenAI) -> dict:
    """Uses the OpenAI API to generate the initial CLDR-like data."""
    try:
        # UPDATED: The system prompt now explicitly includes "JSON" to meet API requirements.
        system_instructions = """
        You are an expert assistant providing locale-specific data. Based on the user's prompt, you must generate a single, raw JSON object.

        Your 'DataType' value must be from this list: 'Area (Default)', 'Area (Floor)', 'Area (Geographic)', 'Area (Land)', 'Blood Glucose', 'Vehicle Fuel Consumption', 'Length (Person Height)', 'Speed (Default)', 'Temperature (Weather)', etc.

        The JSON object structure must be:
        {
            "Entity": "<Country Name>", "CountryCode": "<ISO code>", "DataType": "<Chosen from list>",
            "Data": { "Item 1": { "Value": "<unit_name_only>", "Context": "<description>" } }
        }

        IMPORTANT: The "Value" field must contain ONLY the name of the unit of measurement (e.g., 'acre', 'celsius', 'mile-per-hour'). It must NOT contain any numbers, quantities, or conversational answers.
        """
        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[
                {"role": "system", "content": system_instructions},
                {"role": "user", "content": user_prompt}
            ],
            response_format={"type": "json_object"}
        )
        return json.loads(response.choices[0].message.content)
    except Exception as e:
        return {"error": f"An exception occurred in the LLM call: {e}"}

def get_mismatch_explanation(original_prompt: str, llm_unit: str, cldr_unit: str, client: OpenAI) -> str:
    """Makes a second LLM call to get an explanation for a data mismatch."""
    try:
        explanation_prompt = (
            f"Regarding the question '{original_prompt}', you provided the unit '{llm_unit}'. "
            f"However, the official CLDR standard specifies '{cldr_unit}'. "
            f"In one brief sentence, please explain your reasoning for choosing '{llm_unit}'."
        )
        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[{"role": "user", "content": explanation_prompt}]
        )
        return response.choices[0].message.content.strip()
    except Exception as e:
        return f"Could not get explanation: {e}"

def load_cldr_data(filepath: str = "unitPreferenceData.json") -> dict:
    """Loads the CLDR data from the local JSON file."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            return json.load(f)
    except FileNotFoundError:
        return {"error": f"File '{filepath}' not found."}
    except json.JSONDecodeError:
        return {"error": f"File '{filepath}' is not valid JSON."}

def fetch_from_cldr(cldr_data: dict, cldr_category: str, cldr_usage: str, region_code: str) -> list:
    """Fetches the preferred units from the parsed CLDR data."""
    try:
        preferences = cldr_data["supplemental"]["unitPreferenceData"][cldr_category][cldr_usage]
        region_units = preferences.get(region_code, preferences.get("001", []))
        return [item['unit'] for item in region_units]
    except KeyError:
        return []

# ==============================================================================
# Part 2: Main Application Logic
# ==============================================================================

def main():
    """Main function to parse arguments and run the validation."""
    parser = argparse.ArgumentParser(
        description="Validate LLM output against CLDR data using named arguments.",
        formatter_class=argparse.RawTextHelpFormatter
    )
    parser.add_argument("--locale-name", required=True, help="Descriptive name of the locale (e.g., 'United States')")
    parser.add_argument("--locale-code", required=True, help="Two-letter ISO code for the region (e.g., 'US')")
    parser.add_argument("--concept", required=True, help="The concept being measured (e.g., 'human height')")
    parser.add_argument("--cldr-key", required=True, help="The specific usage key from unitPreferenceData.json (e.g., 'person-height')")
    parser.add_argument("--prompt", required=True, help="The prompt template to be sent to the LLM.\nUse {locale_name} and {concept} as placeholders.")
    
    args = parser.parse_args()

    print("--- SCRIPT STARTED ---")
    
    try:
        client = OpenAI(api_key=API_KEY)
    except Exception as e:
        print(f"Error creating OpenAI client: {e}")
        return

    cldr_data = load_cldr_data()
    if "error" in cldr_data:
        print(cldr_data["error"])
        return

    try:
        user_prompt = args.prompt.format(locale_name=args.locale_name, concept=args.concept)
    except KeyError:
        print("Error: Your prompt template must use {locale_name} and {concept} as placeholders.")
        return

    print(f"Generated Prompt: {user_prompt}")
    print("\n Generating data with LLM...")
    llm_output = generate_data_with_llm(user_prompt, client)

    if "error" in llm_output:
        print(f"LLM Error: {llm_output['error']}")
        return
    
    print("--- LLM Generated Data ---")
    print(json.dumps(llm_output, indent=4))
    
    llm_data_type = llm_output.get("DataType")
    llm_units = [item["Value"] for item in llm_output.get("Data", {}).values()]
    cldr_category = llm_data_type.split(" ")[0].lower() if llm_data_type else ""

    print("\n Fetching ground truth from CLDR file...")
    cldr_units = fetch_from_cldr(cldr_data, cldr_category, args.cldr_key, args.locale_code)
    
    print("\n Comparing results...")
    comparison_result = {
        "ValidationInput": vars(args),
        "LLM_Units_Found": llm_units,
        "CLDR_Units_Found": cldr_units,
        "Comparison": []
    }

    for i, llm_unit in enumerate(llm_units):
        cldr_unit = cldr_units[i] if i < len(cldr_units) else "N/A"
        
        # Normalize both strings before comparing
        match_status = "Match" if normalize_unit(llm_unit) == normalize_unit(cldr_unit) else "Mismatch"
        
        comparison_item = {
            "LLM_Unit": llm_unit,
            "CLDR_Unit": cldr_unit,
            "Status": match_status
        }

        if match_status == "Mismatch":
            print(f"Mismatch found for Unit {i+1}. Getting explanation...")
            explanation = get_mismatch_explanation(user_prompt, llm_unit, cldr_unit, client)
            comparison_item["Explanation"] = explanation

        comparison_result["Comparison"].append({f"Unit_{i+1}": comparison_item})
    
    output_filename = f"report_{args.locale_code}_{args.cldr_key}.json"
    try:
        with open(output_filename, 'w', encoding='utf-8') as f:
            json.dump(comparison_result, f, indent=4)
        print(f"\n Report also saved to file: {output_filename}")
    except IOError as e:
        print(f"\n Error saving report to file: {e}")

    print("\n--- Final Validation Report ---")
    print(json.dumps(comparison_result, indent=4))
    print("\n--- SCRIPT FINISHED ---")


if __name__ == "__main__":
    main()
