---
name: add-rbnf
description: Use when adding or expanding RBNF (Rule Based Number Format) rules for a CLDR language, so numbers can be spelled out as words (e.g. 42 -> "forty-two").
---

# Add RBNF Rules

You are adding or expanding Rule Based Number Format (RBNF) rules for a language in the CLDR repository. RBNF rules turn the binary form of a number into words (e.g. 42 → "forty-two"). The user will specify a BCP 47 language tag (e.g. `sw`, `de_CH`, `sr_Latn`) — either in their request or, if not given, ask which language to work on.

## Prerequisites

This skill assumes a usable CLDR build environment: Maven, Java JDK, and a built `tools/cldr-code/target/cldr-code.jar` (the Phase 3 validation steps run this jar and `mvn test`). If the jar hasn't been built yet, run `mvn -B package -DskipTests --file tools/pom.xml -pl cldr-code` (or an equivalent build) before starting Phase 3.

## Phase 1 — Language Analysis

1. Parse the language code from the user's request. If none was provided, ask the user which language to work on.

2. Check if `common/rbnf/<lang>.xml` already exists.
   - If it exists, read it. You are **expanding** existing rules — do not modify working rulesets, only add missing ones.
   - If it does not exist, you are creating rules for a **new language**.

3. Identify the language's grammatical properties relevant to number formatting using your linguistic knowledge:
   - Grammatical genders (e.g. masculine, feminine, neuter) — do numbers inflect for gender?
   - Grammatical cases (e.g. nominative, genitive, dative, accusative) — do numbers inflect for case?
   - Number word irregularities (e.g. French 70 = soixante-dix, German compound ordering with units before tens)
   - Plural categories that affect large number words (million, billion, etc.)
   - The word used for the decimal separator (e.g. "point", "Komma", "virgule")
   - Whether the language has ordinal concepts that need custom digit-ordinal rules beyond what `root.xml` provides

4. Find the most structurally similar existing RBNF language file to use as a template:
   - Romance languages → `fr.xml`, `es.xml`, `it.xml`
   - Slavic languages → `ru.xml`, `sr.xml`, `uk.xml`
   - Germanic languages → `de.xml`, `nl.xml`
   - Other families → find the closest match among existing files in `common/rbnf/`
   - Read the chosen template file to understand the structural pattern.

5. Present your findings to the user:
   - Which grammatical properties are relevant
   - Which template language you'll use
   - Which rulesets you plan to create (list them)
   - Ask the user to confirm or correct your analysis before proceeding.

**Do not proceed to Phase 2 until the user confirms.**

## Phase 2 — Incremental Rule Authoring

Build rulesets incrementally in complexity order. After writing each ruleset (or small group of closely related rulesets), run the full validation loop in Phase 3 before adding more.

### Ruleset build order

1. `%spellout-numbering` — basic counting form (typically delegates to a base cardinal ruleset).
2. `%spellout-numbering-year` — year formatting.
3. `%spellout-cardinal-*` — cardinal forms:
   - Start with the base cardinal (often `%spellout-cardinal-masculine` or `%spellout-cardinal` depending on whether the language has grammatical gender for numbers).
   - Then add gender variants (feminine, neuter) that delegate to the base for most numbers but differ at key points (typically "1" and sometimes "2").
   - Then add case variants (genitive, dative, accusative, etc.) if the language inflects numbers for case.
4. `%spellout-ordinal-*` — ordinal forms, same progression (base, then gender/case variants).
5. `%digits-ordinal-*` — digit-based ordinals (e.g. "1st", "2e") in an `OrdinalRules` grouping — only if the language needs custom rules beyond `root.xml`.
6. Private helper rulesets (`%%` prefix) as needed — add these alongside the public rulesets that use them.

### Rule authoring standards

Every public ruleset (`%` prefix) must include these special rules:
- `-x:` — negative number handling (e.g. `minus >>` or `moins >>`)
- `x.x:` — fractional number handling with the language-appropriate decimal word (e.g. `<< point >>` or `<< Komma >>`)
- `Inf:` — infinity handling (e.g. `infinity` or `infini`) — only include if the language has a word for infinity; check the template language for guidance

Termination rules:
- Cardinal/numbering rulesets: `1000000000000000000: =#,##0=;`
- Ordinal spell-out rulesets: `1000000000000000000: =#,##0=.;` (or equivalent digit fallback like `=%%digits-ordinal=`)

For large number words (million, billion, trillion, quadrillion), use the plural syntax when the language has plural forms:
```
1000000: << $(cardinal,one{million}other{millions})$[ >>];
```

When a rule needs different text for standalone (zero remainder) vs. compound (non-zero remainder), use the bracket-pipe syntax `[ >>|text]` instead of splitting into two rules. The text before `|` is used when the remainder is non-zero; the text after `|` is used when it is zero:
```
// English ordinal: "two hundredth" (standalone) vs "two hundred first" (compound)
// <%spellout-numbering< formats the quotient through the cardinal ruleset
100: <%spellout-numbering< hundred[ >>|th];

// Telugu ordinal: "వెయ్యవ" (standalone 1000th) vs "వెయ్యి మొదటి" (1001st)
// Common prefix "వెయ్య" gets "ి >>" when compound or "వ" when standalone
1000: వెయ్య[ి >>|వ];
```
This eliminates the need for separate standalone/combining rule pairs (e.g. `1000: వెయ్యవ;` / `1001: వెయ్యి >>;`).

Naming conventions:
- Public rulesets: `%spellout-cardinal-<gender>[-<case>]` (e.g. `%spellout-cardinal-masculine-genitive`)
- Private helpers: `%%` prefix (e.g. `%%et-un`, `%%spellout-leading`)
- Ordinal rulesets: `%spellout-ordinal-<gender>[-<case>]`
- Digit ordinals: `%digits-ordinal-<gender>` (in `OrdinalRules` grouping)

### XML file format

For a **new language**, create `common/rbnf/<lang>.xml` with this structure:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE ldml SYSTEM "../../common/dtd/ldml.dtd">
<!--
Copyright © <this year>-<this year> Unicode, Inc.
CLDR data files are interpreted according to the LDML specification (http://unicode.org/reports/tr35/)
For terms of use, see http://www.unicode.org/copyright.html
-->
<ldml>
    <identity>
        <version number="$Revision$"/>
        <language type="<lang>"/>
    </identity>
    <rbnf>
        <rulesetGrouping type="SpelloutRules">
            <rbnfRules><![CDATA[
... rules go here ...
]]></rbnfRules>
        </rulesetGrouping>
    </rbnf>
</ldml>
```

For locale variants (e.g. `de_CH`, `fr_BE`), add `<territory>` or `<script>` elements in the identity block as appropriate.

Add an `OrdinalRules` grouping only when the language needs custom digit-ordinal rules beyond `root.xml`:

```xml
        <rulesetGrouping type="OrdinalRules">
            <rbnfRules><![CDATA[
... digit ordinal rules ...
]]></rbnfRules>
        </rulesetGrouping>
```

For an **existing language**, read the existing `common/rbnf/<lang>.xml` file and add new rulesets inside the existing `<rbnfRules>` CDATA section without modifying rulesets that already work.

## Phase 3 — Validation

Run this validation loop after each ruleset (or small group of related rulesets) is added or modified. Track how many times you have attempted validation for the current ruleset — if you exceed 5 attempts, stop and ask the user for guidance.

### Validation steps

1. **Delete the existing .ssv file** (so the generator will create a fresh one):
   ```bash
   rm -f common/testData/rbnf/<lang>.ssv
   ```

2. **Generate test data** from the current rules:
   ```bash
   java -DCLDR_DIR=$(pwd) -Xmx6g -jar tools/cldr-code/target/cldr-code.jar generate-rbnf-ssv
   ```

3. **Run the RBNF tests**:
   ```bash
   mvn -DHAS_CLDR_ARCHIVE=false -Dtest="TestRBNF" --file tools/pom.xml -pl cldr-code test
   ```

4. **Read the generated .ssv file** and review it for linguistic correctness:
   - Read `common/testData/rbnf/<lang>.ssv`
   - Spot-check these key numbers: 0, 1, 2, 3, 10, 11, 12, 13, 20, 21, 100, 101, 1000, 1000000
   - Verify gender agreement for "1" across all gendered rulesets
   - Verify correct plural forms for large number words
   - Check ordinals for 1-10 (often irregular)
   - Verify the decimal separator word matches the language convention
   - Verify the negative number prefix matches the language convention

5. **If any issues are found** (test failures OR linguistic errors):
   - Fix the rules in the XML file
   - Go back to step 1 and repeat

6. **If everything passes**, proceed to the next ruleset in the Phase 2 build order, or to Phase 4 if all rulesets are complete.

### Error diagnosis

| Symptom | Likely cause | Fix |
|---|---|---|
| `Failed to create RuleBasedNumberFormat` | Malformed RBNF syntax (missing semicolons, bad references) | Read the error message carefully, fix the XML rule syntax |
| Test format mismatch (`expected "X" but got "Y"`) | Rules produce wrong output | Compare expected vs actual, fix the rule that handles that number |
| Test parse failure (`parse failed for "X"`) | Ambiguous rules — multiple rules could match the same text | Adjust rules to remove ambiguity, ensure number words are unique |
| Linguistic error in .ssv review | Wrong number words, wrong gender, wrong plural | Fix the specific rule, often just the word text for a specific number |

## Phase 4 — Final Review

Once all rulesets have been written and individually validated:

1. **Run the full validation loop one final time** (Phase 3 steps 1-4) to ensure everything still works together.

2. **Read the complete .ssv file** and review all number words across all rulesets. Pay special attention to consistency between related rulesets (e.g. cardinal masculine vs. cardinal feminine should only differ where gender matters).

3. **Present a summary to the user** of what was created or modified:
   - List all rulesets added or changed
   - Note any known limitations or areas where the user should double-check
   - Show a few example outputs (e.g. how 1, 21, 100, and 1000000 render in each ruleset)

4. **Apply code formatting** if any Java files were touched:
   ```bash
   mvn --file=tools/pom.xml spotless:apply
   ```

5. The user can then commit the changes. The commit message should follow the format: `CLDR-NNNN Brief description of change` (the user will provide the JIRA ticket number).
