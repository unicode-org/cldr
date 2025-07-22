#!/usr/bin/env python3
# Install needed libraries
!pip install --quiet transformers torch sentencepiece

import json
from transformers import pipeline

# Initialize the LLM once on GPU 0
llm = pipeline(
    "text2text-generation",
    model="google/flan-t5-base",
    device=0,
    max_new_tokens=16,
    do_sample=False
)

# Load your units.json
with open("units.json", "r", encoding="utf-8") as f:
    data = json.load(f)
units = data["main"]["en"]["units"]["long"]

# Flatten into "key: pattern" lines
patterns = []
for name, info in units.items():
    if "unitPrefixPattern" in info:
        patterns.append(f"{name}: {info['unitPrefixPattern']}")
    elif "compoundUnitPattern" in info:
        patterns.append(f"{name}: {info['compoundUnitPattern']}")

# Break into 50-line chunks so we stay under the model’s context limit
chunks = [patterns[i:i+50] for i in range(0, len(patterns), 50)]

# Prompt the user (or use defaults)
default1 = "For the following CLDR English unit patterns, reply exactly “All correct” or list any incorrect entries."
default2 = "Which standard SI prefixes are missing? If none, reply exactly “None.”"
default3 = "How many patterns are in this list? Reply with only the integer."

instr1 = input(f"Check correctness instruction (enter to use default):\n  {default1}\n> ").strip() or default1
instr2 = input(f"Find missing instruction (enter to use default):\n  {default2}\n> ").strip() or default2
instr3 = input(f"Count patterns instruction (enter to use default):\n  {default3}\n> ").strip() or default3

prompts = {
    "Check correctness": instr1,
    "Find missing": instr2,
    "Count patterns": instr3
}

# Run each of the first two prompts across all chunks
for label in ["Check correctness", "Find missing"]:
    print(f"\n=== {label} ===")
    answers = []
    for chunk in chunks:
        block = "\n".join(chunk)
        prompt = f"{prompts[label]}\n\n```\n{block}\n```"
        answers.append(llm(prompt)[0]["generated_text"].strip())
    print(" ".join(answers))

# For the count prompt, send the entire list at once
print("\n=== Count patterns (LLM) ===")
full_block = "\n".join(patterns)
count_prompt = f"{prompts['Count patterns']}\n\n```\n{full_block}\n```"
llm_count = llm(count_prompt)[0]["generated_text"].strip()
print(llm_count)

# And our ground-truth Python count
print("\n=== Count patterns (Python) ===")
print(len(patterns))
