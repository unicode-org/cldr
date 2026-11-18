# Feature-Based Currency Test Data (`common/testData/currency/features`)

Test data for currency formatting ([UTS #35 Part 3: Numbers](../../../../docs/ldml/tr35-numbers.md)), organized by feature. A feature is one behavior that the spec requires.

## Files

- `features.tsv`: one row per feature. Its `core_set` has the `dimension=value` pairs that exercise the feature, separated by `; `, with `,` between the values of one dimension.
- `coverage.tsv`: one row per spec clause and feature.
    - `clause`: an ID from the TR35 coverage doc (CLDR-19466). S1.1a is section 1, sentence 1, case a.
    - `spec`: the UTS #35 link, relative to `docs/ldml`.
    - `snippet`: the UTS #35 text of the clause, with `<<` and `>>` in place of straight double quotes. `↑↑↑↑↑` means the same snippet as in the row above.
    - `summary`: what the clause requires, in our own words.
    - `feature_id`: empty if no feature tests the clause.
    - `status`: `covered`, `todo`, `waiting` (for a second source of expected values) or `out_of_scope`. `note` gives the ticket or the reason.
- `core_<feature_id>.tsv`: the test cases of one feature, one per combination of its core set. Generated, so don't edit by hand.

All files are UTF-8 and tab-separated. Every line, including the `#` header, has the same number of tabs, and no cell contains `"`.

## Core TSV Columns

Find columns by their header name: a new dimension adds a column before `expected`.

| Column | Meaning |
|---|---|
| `locale` | CLDR locale ID |
| `currency` | ISO 4217 currency code |
| `currency_format_length` | `short` for the compact format; empty otherwise |
| `currency_display` | `symbol`, `symbolNarrow`, `code` or `name`; empty means `symbol` |
| `input` | The amount to format |
| `expected` | The expected result, from ICU4J |
| `main_feature` | The feature the row was made for, as in the file name |
| `other_features` | Other features the row exercises, comma-separated |

## Regenerating and Testing

From the root of the repository:

```sh
mvn compile exec:java -pl tools/cldr-code -DCLDR_DIR=$(pwd) -Dexec.mainClass=org.unicode.cldr.tool.GenerateCurrencyFeatureTestData
mvn test -pl tools/cldr-code -Dtest=TestShim -Dsurefire.failIfNoSpecifiedTests=false "-Dorg.unicode.cldr.unittest.testArgs=-f:TestCurrencyFeature -n"
```
