---
title: Attribute Value Constraints
---

# Attribute Value Constraints

The following are [DTD Annotations](https://unicode.org/reports/tr35/tr35.html#DTD_Annotations) that provide constraints on attribute values.
They are used internally in managing and testing the data in XML files.
Because they are for internal use for CLDR tooling, 
they are described here instead of in LDML section: [DTD Annotations](https://unicode.org/reports/tr35/tr35.html#DTD_Annotations).

They are used in DTD Annotation lines such as the following;

`<!--@MATCH:{attribute value constraint}-->`

The following describes the options available.

| Constraint                | Matches | Example | Example Match |
| ------------------------- | -------- | -------- |:--------:|
| `any`                       | any string value; often used before applying tighter constraints | -| 1438y9fbquio |
| `any/TODO`                  | placeholder for future constraints | -| uinpq43re |
| `bcp47/{subtype}`              | see bcp47 subtypes below | bcp47/cu | usd |
| `literal/{literal values}` | comma separated literals| literal/-12, 0, 87| 87 |
| `regex/{pattern}` | valid regex expression | regex/\[A-Z]{2} | BE |
| `metazone`                  | valid metazone | |Africa/Abidjan |
| `range/{start~end}` | number between (inclusive) start and end |range/0~100 | 3.2 |
| `time/{pattern}` | time or date or date-time pattern | time | y-MM-dd |
| `unicodeset/{pattern}` | valid unicodeset | unicodeset/\p{Letter} | A |
| `validity/{subtype}`       | see validity subtypes below | validity/locale | en_US |
| `version`                   | 1 to 4 digit field version | - | 35.3.9 |
| `set/{match}`              | any element of a set of elements that match \{match} | set/bcp47/tz| adalv aedxb |
| `or/{match1}\|\|{match2}`  | matches at least one of \{match1}, etc | or/bcp47/anykey\|\|literal/t | t |

## BCP 47 subtypes
These subtypes test identifiers according to the [bcp47 files](https://github.com/unicode-org/cldr/tree/main/common/validity), where key is a ukey or tkey, and type is a uvalue or tvalue.
| Constraint                | Matches | Example | Example Match |
| ------------------------- | -------- | -------- |:--------:|
| `bcp47/anykey`              | any bcp47 key | -| nu (number system)|
| `bcp47/anyvalue`            | any bcp47 value | - | roman |
| `bcp47/{key}`      | any value for that key | bcp47/nu | roman |

***\[The following is draft]***

## Validity subtypes
These subtypes test identifiers according to the [validity files](https://github.com/unicode-org/cldr/tree/main/common/validity), plus some related identifiers.
| Constraint                | Matches | Example | Example Match |
| ------------------------- | -------- | -------- |:--------:|
| `validity/currency`       | currency codes | validity/currency | USD |
| `validity/currency/{idStatusList}`       | currency codes of idStatusList | validity/currency/deprecated | ADP |
The subtypes language, region, script, subdivision, unit, variant behave like currency, eg validity/region/macroregion matches 001. 
The optional idStatusList is a list of one or more idStatus values, such as `validity/unit/regular deprecated`.
The default is all of the idStatus values that are valid for that validity file, except for deprecated. So validity/region doesn't match SU (Soviet Union).
There is a special idStatus value 'all' that includes deprecated, so `validity/region/all` matches SU (Soviet Union).
Note that validity/unit tests for long unit values.

The related matchers are:
- bcp47-wellformed: tests for wellformed bcp47, but not for validity
- locale: tests locale validity using the locale, script, region, and variant values.
    - The idStatus is applied to each of those fields (after removing ones invalid for that field).
- locale-for-names: also allows certain deprecated locales, and is used in matching locales names.
- locale-for-likely: also allows certain deprecated locales, and is used in matching likely subtags values.
