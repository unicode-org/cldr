import json
from openai import OpenAI

# --- Part 1: LLM Generator ---
# This function calls the LLM to get its understanding of the data.

def generate_data_with_llm(user_prompt: str) -> dict:
    """
    Uses the OpenAI API to generate CLDR-like data.
    Note: The 'DataType' from the LLM is crucial for looking up in the CLDR file.
    """
    try:
        # --- PASTE YOUR OPENAI API KEY HERE ---
        client = OpenAI(api_key="YOUR KEY")

        system_instructions = """
        You are an expert assistant that provides locale-specific data. Based on the user's prompt, generate a single, raw JSON object.
        Your 'DataType' value must be chosen from this specific list to match the CLDR file:
        'Area (Default)', 'Area (Floor)', 'Area (Geographic)', 'Area (Land)', 'Blood Glucose', 'Vehicle Fuel Consumption', 'Duration (Media)',
        'Food Energy', 'Length (Person Height)', 'Length (Road)', 'Speed (Default)', 'Temperature (Weather)', 'Volume (Fluid)'.
        The JSON object must follow this exact structure:
        {
            "Entity": "<Country or Region Name, e.g., United Kingdom>",
            "CountryCode": "<Two-letter ISO code, e.g., GB>",
            "DataType": "<Chosen from the list above, e.g., Speed (Default)>",
            "Data": {
                "Item 1": {
                    "Value": "<The primary unit, formatted like 'mile-per-hour'>",
                    "Context": "<A brief description of its primary use>"
                },
                "Item 2": {
                    "Value": "<A secondary unit, if applicable>",
                    "Context": "<A brief description>"
                }
            }
        }
        Do not include any text or explanation before or after the JSON object. Omit "Item 2" if not relevant.
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
        return {"error": str(e)}


# --- Part 2: CLDR Data Fetcher ---
# These functions load and find the ground truth from your local JSON file.

def load_cldr_data(filepath: str = "unitPreferenceData.json") -> dict:
    """Loads the CLDR data from the local JSON file."""
    try:
        # 'with open(...)' opens the file and assigns it to 'f'
        with open(filepath, 'r', encoding='utf-8') as f:
            # Pass the file object 'f' to json.load()
            return json.load(f)
    except FileNotFoundError:
        print(f"Error: The file '{filepath}' was not found. Please make sure it's in the same directory.")
        return None
    except json.JSONDecodeError:
        print(f"Error: The file '{filepath}' is not a valid JSON file.")
        return None

def fetch_from_cldr(cldr_data: dict, cldr_category: str, cldr_usage: str, region_code: str) -> list:
    """Fetches the preferred units from the parsed CLDR data."""
    if not cldr_data:
        return []
    try:
        # Navigate through the CLDR JSON structure
        preferences = cldr_data["supplemental"]["unitPreferenceData"][cldr_category][cldr_usage]
        
        # Get region-specific units if they exist, otherwise fall back to the world default ("001")
        region_units = preferences.get(region_code, preferences.get("001", []))
        
        # Extract just the unit names into a list
        return [item['unit'] for item in region_units]
    except KeyError:
        # Path not found in the CLDR data
        return []

# --- Part 3: The Validator ---
# This is the main logic that ties everything together.

if __name__ == "__main__":
    # Load the ground truth data once
    cldr_data = load_cldr_data()
    if not cldr_data:
        exit() # Stop if the CLDR file can't be loaded

    # A simple mapping from the LLM's DataType to the keys in the CLDR json file
    # Format: "LLM DataType": ("cldr_category", "cldr_usage")
    cldr_mapping = {
        "Area (Default)": ("area", "default"),
        "Area (Floor)": ("area", "floor"),
        "Area (Geographic)": ("area", "geograph"), # Added for completeness
        "Area (Land)": ("area", "land"),          # <-- THIS IS THE FIX
        "Speed (Default)": ("speed", "default"),
        "Temperature (Weather)": ("temperature", "weather"),
        "Length (Person Height)": ("length", "person-height"),
        "Length (Road)": ("length", "road"),
        "Vehicle Fuel Consumption": ("consumption", "vehicle-fuel"),
        "Blood Glucose": ("concentration", "blood-glucose"), # Added for completeness
    }
    # Get user input
    user_prompt = input("Enter your question about local data: ")
    
    # Step 1: Get data from the LLM
    print("\n Generating data with LLM...")
    llm_output = generate_data_with_llm(user_prompt)

    if "error" in llm_output:
        print(f"LLM Error: {llm_output['error']}")
        exit()
    
    print("--- LLM Generated Data ---")
    print(json.dumps(llm_output, indent=4))
    
    # Step 2: Extract key info from LLM output to perform the lookup
    llm_data_type = llm_output.get("DataType")
    llm_country_code = llm_output.get("CountryCode")
    llm_units = [item["Value"] for item in llm_output.get("Data", {}).values()]

    if not all([llm_data_type, llm_country_code, llm_units]):
        print("\nError: LLM output was missing required keys (DataType, CountryCode, or Data).")
        exit()

    # Step 3: Fetch the corresponding ground truth from CLDR
    print("\n Fetching ground truth from CLDR file...")
    if llm_data_type in cldr_mapping:
        category, usage = cldr_mapping[llm_data_type]
        cldr_units = fetch_from_cldr(cldr_data, category, usage, llm_country_code)
    else:
        cldr_units = []

    # Step 4: Compare the results and create the final output
    print("\n Comparing results...")
    comparison_result = {
        "ValidationInput": {
            "Prompt": user_prompt,
            "LLM_Entity": llm_output.get("Entity"),
            "LLM_CountryCode": llm_country_code,
            "CLDR_Lookup": f"Category: '{category}', Usage: '{usage}', Region: '{llm_country_code}'"
        },
        "LLM_Units_Found": llm_units,
        "CLDR_Units_Found": cldr_units,
        "Comparison": []
    }

    # Compare each unit found by the LLM
    for i, llm_unit in enumerate(llm_units):
        cldr_unit = cldr_units[i] if i < len(cldr_units) else "N/A"
        match_status = "Match" if llm_unit == cldr_unit else "Mismatch"
        comparison_result["Comparison"].append({
            f"Unit_{i+1}": {
                "LLM_Unit": llm_unit,
                "CLDR_Unit": cldr_unit,
                "Status": match_status
            }
        })
    
    # Print the final comparison
    print("\n--- Final Validation Report ---")
    print(json.dumps(comparison_result, indent=4))
