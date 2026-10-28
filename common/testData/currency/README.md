# CLDR Currency Format Test Data (`common/testData/currency`)

Feature-based test data for currency formatting ([UTS #35 Part 3: Numbers](../../../docs/ldml/tr35-numbers.md)). A feature is one behavior that a part of the spec requires.

## Files

- `features.tsv`: one row per feature, with its ID, its UTS #35 link, its clauses, its core set, and a summary. The core set lists the `dimension=value` pairs that exercise the feature (several values of one dimension are separated by `,`).
- `core_<feature_id>.tsv`: the test cases of one feature, one for each combination of its core set.

## Core TSV Format

UTF-8, tab-separated. Every line, including the `#` header, has the same number of tabs.

| Column | Meaning |
|---|---|
| `locale` | CLDR locale ID |
| `currency` | ISO 4217 currency code |
| `currency_format_length` | `short` for the compact format; empty for the non-compact format |
| `currency_display` | `symbol`, `symbolNarrow`, `code` or `name`; empty means `symbol` |
| `input` | The amount to format |
| `expected` | The expected result |
| `main_feature` | The feature that the row was made for (the same as in the file name) |
| `other_features` | Other features that the row exercises, comma-separated |

## Regenerating and Testing

From the root of the repository:

```sh
mvn compile exec:java -pl tools/cldr-code -DCLDR_DIR=$(pwd) -Dexec.mainClass=org.unicode.cldr.tool.GenerateCurrencyFeatureTestData
mvn test -pl tools/cldr-code -Dtest=TestShim -Dsurefire.failIfNoSpecifiedTests=false "-Dorg.unicode.cldr.unittest.testArgs=-f:TestCurrencyFeature -n"
```
