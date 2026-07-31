## <a name="Language_Plural_Rules" id="Language_Plural_Rules" href="#Language_Plural_Rules">Language Plural Rules</a>

```dtd
<!ELEMENT plurals (pluralRules*, pluralRanges*) >
<!ATTLIST plurals type ( ordinal | cardinal ) #IMPLIED > <!-- default is cardinal -->

<!ELEMENT pluralRules (pluralRule*) >
<!ATTLIST pluralRules locales NMTOKENS #REQUIRED >

<!ELEMENT pluralRule ( #PCDATA ) >
<!ATTLIST pluralRule count (zero | one | two | few | many | other) #REQUIRED >
```
* The plural categories are used to format messages with numeric placeholders, expressed as decimal numbers. The fundamental rule for determining plural categories is the existence of minimal pairs: whenever two different numbers may require different versions of the same message, then the numbers have different plural categories.


* This happens even if nouns are invariant; even if all English nouns were invariant (like “sheep”), English would still require 2 plural categories because of subject-verb agreement, and pronoun agreement. For example:


1. 1 sheep **is** here. Do you want to buy **it**?
2. 2 sheep **are** here. Do you want to buy **them**?

For more information, see [Determining-Plural-Categories](https://cldr.unicode.org/index/cldr-spec/plural-rules#determining-plural-categories).

* English does not have a separate plural category for “zero”, because it does not require a different message for “0”. For example, the same message can be used below, with just the numeric placeholder changing.


1. You have 3 friends online.
2. You have 0 friends online.

* However, across many languages it is commonly more natural to express "0" messages with a negative (“None of your friends are online.”) and "1" messages also with an alternate form “You have a friend online.”. Thus pluralized message APIs should also offer the ability to specify at least the 0 and 1 cases explicitly; developers can use that ability whenever these values might occur in a placeholder.


* The CLDR plural rules are not expected to cover all cases. For example, strictly speaking, there could be more plural and ordinal forms for English. Formally, we have a different plural form where a change in digits forces a change in the rest of the sentence. There is an edge case in English because of the behavior of "a/an".


For example, in changing from 3 to 8:

* "a 3rd of a loaf" should result in "an 8th of a loaf", not "a 8th of a loaf"
* "a 3 foot stick" should result in "an 8 foot stick", not "a 8 foot stick"

So numbers of the following forms could have a special plural category and special ordinal category: 8(X), 11(X), 18(X), 8x(X), where x is 0..9 and the optional X is 00, 000, 00000, and so on.

* On the other hand, the above constructions are relatively rare in messages constructed using numeric placeholders, so the disruption for implementations currently using CLDR plural categories wouldn't be worth the small gain.


* This section defines the types of plural forms that exist in a language—namely, the cardinal and ordinal plural forms. Cardinal plural forms express units such as time, currency or distance, used in conjunction with a number expressed in decimal digits (i.e. "2", not "two", and not an indefinite number such as "some" or "many"). Ordinal plural forms denote the order of items in a set and are always integers. For example, English has two forms for cardinals:


* form "one": 1 day
* form "other": 0 days, 2 days, 10 days, 0.3 days

and four forms for ordinals:

* form "one": 1st floor, 21st floor, 101st floor
* form "two": 2nd floor, 22nd floor, 102nd floor
* form "few": 3rd floor, 23rd floor, 103rd floor
* form "other": 4th floor, 11th floor, 96th floor

* Other languages may have additional forms or only one form for each type of plural. CLDR provides the following tags for designating the various plural forms of a language; for a given language, only the tags necessary for that language are defined, along with the specific numeric ranges covered by each tag (for example, the plural form "few" may be used for the numeric range 2–4 in one language and 3–9 in another):


* zero (see also plural case “0”, described in [Explicit 0 and 1 rules](#Explicit_0_1_rules))
* one (see also plural case “1”, described in [Explicit 0 and 1 rules](#Explicit_0_1_rules))
* two
* few
* many

* In addition, an "other" tag is always implicitly defined to cover the forms not explicitly designated by the tags defined for a language. This "other" tag is also used for languages that only have a single form (in which case no plural-form tags are explicitly defined for the language). For a more complex example, consider the cardinal rules for Russian and certain other languages:


```xml
<pluralRules locales="hr ru sr uk">
    <pluralRules count="one">n mod 10 is 1 and n mod 100 is not 11</pluralRule>
    <pluralRules count="few">n mod 10 in 2..4 and n mod 100 not in 12..14</pluralRule>
</pluralRules>
```
* These rules specify that Russian has a "one" form (for 1, 21, 31, 41, 51, …), a "few" form (for 2–4, 22–24, 32–34, …), and implicitly an "other" form (for everything else: 0, 5–20, 25–30, 35–40, …, decimals). Russian does not need additional separate forms for zero, two, or many, so these are not defined.


* A source number represents the visual appearance of the digits of the result. In text, it can be represented by the EBNF for sampleValue. Note that the same double number can be represented by multiple source numbers. For example, "1.0" and "1.00" are different source numbers, but there is only one double number that they correspond to: 1.0d == 1.00d. As another example, 1e3d == 1000d, but the source numbers "1e3" and "1000" are different, and can have different plural categories. So the input to the plural rules carries more information than a computer double. The plural category for negative numbers is calculated according to the absolute value of the source number, and leading integer digits don't have any effect on the plural category calculation. (This may change in the future, if we find languages that have different behavior.)


Plural categories may also differ according to the visible decimals. For example, here are some of the behaviors exhibited by different languages:

| Behavior | Description                                                                         | Example                                                                |
|----------|-------------------------------------------------------------------------------------|------------------------------------------------------------------------|
| Base     | The fractions are ignored; the category is the same as the category of the integer. | 1.13 has the same plural category as 1.                                |
| Separate | All fractions by value are in one category (typically ‘other’ = ‘plural’).          | 1.01 gets the same class as 9; <br/> 1.00 gets the same category as 1. |
| Visible  | All visible fractions are in one category (typically ‘other’ = ‘plural’).           | 1.00, 1.01, 3.5 all get the same category.                             |
| Digits   | The visible fraction determines the category.                                       | 1.13 gets the same class as 13.                                        |

There are also variants of the above: for example, short fractions may have the Digits behavior, but longer fractions may just look at the final digit of the fraction.

Currently there are no locale keywords that affect plural rule selection; they are selected using the base locale ID, ignoring any -u- extension keywords.

#### <a name="Explicit_0_1_rules" id="Explicit_0_1_rules" href="#Explicit_0_1_rules">Explicit 0 and 1 rules</a>

* Some types of CLDR data (such as [unitPatterns](tr35-general.md#Unit_Elements) and [currency displayNames](#Currencies)) allow specification of plural rules for explicit cases “0” and “1”, in addition to the language-specific plural cases specified above: “zero”, “one”, “two” ... “other”. For the language-specific plural rules:


* The rules depend on language; for a given language, only a subset of the cases may be defined. For example, English only defines “one” and “other”, cases like “two” and “few” cannot be used in plurals for English CLDR items.
* Each plural case may cover multiple numeric values, and may depend on the formatting of those values. For example, in French the “one” case covers 0.0 through 1.99.
* The “one” case, if defined, includes at least some formatted forms of the numeric value 1; the “zero” case, if defined, includes at least some formatted forms of the numeric value 0.

By contrast, for the explicit cases “0” and “1”:

* The explicit “0” and “1” cases are not defined by language-specific rules, and are available in any language for the CLDR data items that accept them.
* The explicit “0” and “1” cases apply to the exact numeric values 0 and 1 respectively. These cases are typically used for plurals of items that do not have fractional value, like books or files.
* The explicit “0” and “1” cases have precedence over the “zero” and “one” cases. For example, if for a particular element CLDR data includes values for both the “1” and “one” cases, then the “1” value is used for numeric values of exactly 1, while the “one” value is used for any other formatted numeric values matching the “one” plural rule for the language.

Usage example: In English (which only defines language-specific rules for “one” and “other”) this can be used to have special behavior for 0:

* count=“0”: no books
* count=“one”: {0} book, e.g. “1 book”
* count=“other”: {0} books, e.g. “3 books”

### <a name="Plural_rules_syntax" id="Plural_rules_syntax" href="#Plural_rules_syntax">Plural rules syntax</a>

The plural categories for each locale are determined by evaluating rules in a plural rule set, which is defined by the contents of the element `pluralRules`.
For example:

```
<plurals type="cardinal">
…
  <pluralRules locales="am as bn doi fa gu hi kn kok kok_Latn pcm zu">
    <pluralRule count="one">i = 0 or n = 1 @integer 0, 1 @decimal 0.0~1.0, 0.00~0.04</pluralRule>
    <pluralRule count="other"> @integer 2~17, 100, 1000, 10000, 100000, 1000000, … @decimal 1.1~2.6, 10.0, 100.0, 1000.0, 10000.0, 100000.0, 1000000.0, …</pluralRule>
  </pluralRules>
…
```
- The `type` attribute is currently either `cardinal` (plural) or `ordinal` (1st, 2nd, …).
- The `locales` attribute lists all the locales that have those `pluralRules`. (No locale can be listed more than once for the same `type`.)
- Each `pluralRule` associates a plural category (the value of the attribute `count`) with a `condition` and `samples`.
There one exception: there is no explicit condition for `other`.
- The plural categories are currently limited to {`zero`, `one`, `two`, `few`, `many`, `other`}.
- No plural category can occur more than once in the same `pluralRules` element.
- The `other` category is mandatory: any particular locale may also have any combination of {`zero`, `one`, `two`, `few`, `many`}.

The pluralRules can be expressed as a single string. For example, ICU uses the following syntax
```
rules         = rule (';' rule)*
rule          = category ':' condition samples
              | 'other' ':' samples
category      = [a-z]+
condition     // as below
samples       // as below
```
In order to determine the plural category for a given number, each `pluralRule` is evaluated in the order: {`zero`, `one`, `two`, `few`, `many`}
- If any rule evaluates to `true`, then the corresponding plural category is returned.
- If no other category is returned, then `other` is.

It is possible for two rules in {`zero`, `one`, `two`, `few`, `many`} to _overlap_, to both evaluate to `true` for the same number.
(That is generally avoided for the CLDR rules, except in cases where a later condition (in evaluation order) would be overly complicated.)
The rules should be constructed so that each listed plural category is non-empty.
(This is true for the CLDR data.)

The `samples` list one or more numbers with that plural category.
Thus they do not include numbers where previous conditions (in the order {`zero`, `one`, `two`, `few`, `many`, `other`} would also evaluate to true.
Each sample number _N_ is a decimal fraction, optionally with compact decimal formatting.
Note that _N_ may have trailing fractional zeros, since those are significant for determining plural categories for many languages.
The compact decimal formatting is denoted by a special notation in the syntax, e.g., “1.2c6” for “1.2M”.

The conditions and samples have the following syntax:

```
condition       = and_condition ('or' and_condition)*
samples         = ('@integer' sampleList)?
                  ('@decimal' sampleList)?
and_condition   = relation ('and' relation)*
relation        = is_relation | in_relation | within_relation
is_relation     = expr 'is' ('not')? value
in_relation     = expr (('not')? 'in' | '=' | '!=') range_list
within_relation = expr ('not')? 'within' range_list
expr            = operand (('mod' | '%') value)?
operand         = 'n' | 'i' | 'f' | 't' | 'v' | 'w' | 'c' | 'e'
range_list      = (range | value) (',' range_list)*
range           = value'..'value
value           = digit+
sampleList      = sampleRange (',' sampleRange)* (',' ('…'|'...'))?
sampleRange     = sampleValue ('~' sampleValue)?
sampleValue     = sign? value ('.' digit+)? ([ce] digitPos digit+)?
sign            = '+' | '-'
digit           = [0-9]
digitPos        = [1-9]
```
* Whitespace (defined as Unicode [Pattern_White_Space](https://util.unicode.org/UnicodeJsps/list-unicodeset.jsp?a=%5Cp%7BPattern_White_Space%7D)) can occur between or around any of the above tokens, with the exception of the tokens in value, digit, and sampleValue.
* In the syntax, **and** binds more tightly than **or**. So **X or Y and Z** is interpreted as **(X or (Y and Z))**.
  * For example, c = 0 and i != 0 and i % 1000000 = 0 and *+v = 0+* or c != 0..5 is parsed as if it were (c = 0 and i != 0 and i % 1000000 = 0 and v = 0) or (c != 0..5)
* Each plural rule must be written to be self-contained, and not depend on the ordering. Thus rules must be mutually exclusive; for a given numeric value, only one rule can apply (i.e., the condition can only be true for one of the pluralRule elements). Each keyword can have at most one condition. The 'other' keyword must have an empty condition: it is only present for samples.
* The samples should be included, since they are used by client software for samples and determining whether the keyword has finite values or not.
* The 'other' keyword must have no condition, and all other keywords must have a condition.

#### <a name="Operands" id="Operands" href="#Operands">Operands</a>

The operands are numeric values corresponding to features of the *source number N*, and have the following meanings given in the table below.
Note that, contrary to source numbers, operands are treated numerically.
Although some of them are used to describe insignificant 0s in the source number, any insignificant 0s in the operands themselves are ignored, e.g., f=03 is equivalent to f=3.

###### <a name="Plural_Operand_Meanings" id="Plural_Operand_Meanings" href="#Plural_Operand_Meanings">Table: Plural Operand Meanings</a>

| Symbol | Value                                                                                           |
|--------|-------------------------------------------------------------------------------------------------|
| n      | the absolute value of N.*                                                                       |
| i      | the integer digits of N.*                                                                       |
| v      | the number of visible fraction digits in N, _with_ trailing zeros.*                             |
| w      | the number of visible fraction digits in N, _without_ trailing zeros.*                          |
| f      | the visible fraction digits in N, _with_ trailing zeros, expressed as an integer.*              |
| t      | the visible fraction digits in N, _without_ trailing zeros, expressed as an integer.*           |
| c      | compact decimal exponent value: exponent of the power of 10 used in compact decimal formatting. |
| e      | a deprecated synonym for ‘c’. Note: it may be redefined in the future.                          |

- If there is a compact decimal exponent value (‘c’), then the n, i, f, t, v, and w values are computed _after_ shifting the decimal point in the original by the ‘c’ value.
So for 1.2c3, the n, i, f, t, v, and w values are the same as those of 1200:  i=1200 and f=0.
Similarly, 1.2005c3 has i=1200 and f=5 (corresponding to 1200.5).
- The old keywords 'mod', 'in', 'is', and 'within' are present only for backwards compatibility. The preferred form is to use '%' for modulo, and '=' or '!=' for the relations, with the operand 'i' instead of within. (The difference between **in** and **within** is that **in** only includes integers in the specified range, while **within** includes all values.)

###### <a name="Plural_Operand_Examples" id="Plural_Operand_Examples" href="#Plural_Operand_Examples">Table: Plural Operand Examples</a>

|    source |         n |         i | v | w |   f |  t | c |
|----------:|----------:|----------:|--:|--:|----:|---:|--:|
|         1 |         1 |         1 | 0 | 0 |   0 |  0 | 0 |
|       1.0 |         1 |         1 | 1 | 0 |   0 |  0 | 0 |
|      1.00 |         1 |         1 | 2 | 0 |   0 |  0 | 0 |
|       1.3 |       1.3 |         1 | 1 | 1 |   3 |  3 | 0 |
|      1.30 |       1.3 |         1 | 2 | 1 |  30 |  3 | 0 |
|      1.03 |      1.03 |         1 | 2 | 2 |   3 |  3 | 0 |
|     1.230 |      1.23 |         1 | 3 | 2 | 230 | 23 | 0 |
|   1200000 |   1200000 |   1200000 | 0 | 0 |   0 |  0 | 0 |
|     1.2c6 |   1200000 |   1200000 | 0 | 0 |   0 |  0 | 6 |
|     123c6 | 123000000 | 123000000 | 0 | 0 |   0 |  0 | 6 |
|     123c5 |  12300000 |  12300000 | 0 | 0 |   0 |  0 | 5 |
|   1200.50 |    1200.5 |      1200 | 2 | 1 |  50 |  5 | 0 |
| 1.20050c3 |    1200.5 |      1200 | 2 | 1 |  50 |  5 | 3 |


#### <a name="Relations" id="Relations" href="#Relations">Relations</a>

* The positive relations are of the format **x = y** and **x = y mod z**. The **y** value can be a comma-separated list, such as **n = 3, 5, 7..15**, and is treated as if each relation were expanded into an OR statement. The range value **a..b** is equivalent to listing all the ***integers*** between **a** and **b**, inclusive. When **!=** is used, it means the entire relation is negated.


###### <a name="Relations_Examples" id="Relations_Examples" href="#Relations_Examples">Table: Relations Examples</a>

| Expression    | Meaning                                 |
|---------------|-----------------------------------------|
| x = 2..4, 15  | x = 2 OR x = 3 OR x = 4 OR x = 15       |
| x != 2..4, 15 | NOT (x = 2 OR x = 3 OR x = 4 OR x = 15) |

| Expression      | Value |
|-----------------|-------|
| 3.5 = 2..4, 15  | false |
| 3.5 != 2..4, 15 | true  |
| 3 = 2..4, 15    | true  |
| 3 != 2..4, 15   | false |

| Expression | Comments |
| --- | --- |
| n = 1 | n can be 1, 1.0, 1.00, … but no greater |
| i = 1 | n can be 1, 1.0, 1.00, … _and_ 1.1, 1.99999, … but no greater |
| i % 10 = 1 | Uses the integer remainder, where 21.33 % 10 ⇒ 1 |
| n % 10 = 1 | Uses the decimal remainder, where 21.33 % 10 ⇒ 1.33. Equivalent to `i % 10 = 1 and f = 0` |
| i % 10 = 3 | The last integer digit of `n` equals 3 |
| i % 1000 = 33 | The last 3 integer digits of `n` equal 33 |

The modulus (% or **mod**) is a remainder operation as defined in Java; for example, where **n** = 4.3 the result of **n mod 3** is 1.3.

* The values of relations are defined according to the operand as follows. Importantly, the results may depend on the visible decimals in the source, including trailing zeros, and the compact decimal exponent.


1. Let the base value BV be computed from absolute value of the original source number according to the operand.
2. Let R be false when the comparison contains ‘not’.
3. Let R be !R if the comparison contains ‘within’ and the source number is not an integer.
4. If there is a module value MV, let BV be BV - floor(BV/MV).
5. Let CR be the list of comparison ranges, normalized that overlapping ranges are merged. Single values in the rule are represented by a range with identical \<starti, endi> values.
6. Iterate through CR:
   * if starti ≤ BV ≤ endi then return R.
7. Otherwise return !R.

###### <a name="Plural_Rules_Examples" id="Plural_Rules_Examples" href="#Plural_Rules_Examples">Table: Plural Rules Examples</a>

| Rules | Comments |
| --- | --- |
| one: n = 1 <br/> few: n = 2..4 | This defines two rules, for 'one' and 'few'. The condition for 'one' is "n = 1" which means that the number must be equal to 1 for this condition to pass. The condition for 'few' is "n = 2..4" which means that the number must be between 2 and 4 inclusive for this condition to pass. All other numbers are assigned the keyword 'other' by the default rule. |
| zero: n = 0 or n != 1 and n mod 100 = 1..19 <br/> one: n = 1 | Each rule should not overlap with other rules. Also note that a modulus is applied to n in the last rule, thus its condition holds for 119, 219, 319, … |
| one: n = 1 <br/> few: n mod 10 = 2..4 and n mod 100 != 12..14 | This illustrates conjunction and negation. The condition for 'few' has two parts, both of which must be met: "n mod 10 = 2..4" and "n mod 100 != 12..14". The first part applies a modulus to n before the test as in the previous example. The second part applies a different modulus and also uses negation, thus it matches all numbers _not_ in 12, 13, 14, 112, 113, 114, 212, 213, 214, … |


#### <a name="Samples" id="Samples" href="#Samples">Samples</a>

Samples are provided if sample indicator (@integer or @decimal) is present on any rule. (CLDR always provides samples.)

* Where samples are provided, the absence of one of the sample indicators indicates that no numeric values can satisfy that rule. For example, the rule "i = 1 and v = 0" can only have integer samples, so @decimal must not occur. The @integer samples have no visible fraction digits, while @decimal samples have visible fraction digits; both can have compact decimal exponent values (if the `c` operand occurs).


* The sampleRanges have a special notation: **start**~**end**. The **start** and **end** values must have the same number of decimal digits, and the same compact decimal exponent values (or neither have compact decimal exponent values). The range encompasses all and only those values **v** where **start ≤ v ≤ end**, and where **v** has the same number of decimal places as **start** and **end**, and the same compact decimal exponent values.


* Samples must indicate whether they are infinite or not. The '…' marker must be present if and only if infinitely many values (integer or decimal) can satisfy the rule. If a set is not infinite, it must list all the possible values.


###### <a name="Plural_Samples_Examples" id="Plural_Samples_Examples" href="#Plural_Samples_Examples">Table: Plural Samples Examples</a>

| Rules | Comments |
| --- | --- |
| @integer 1, 3~5 | 1, 3, 4, 5. |
| @integer 3\~5, 103\~105, … | Infinite set: 3, 4, 5, 103, 104, 105, … |
| @decimal 1.3\~1.5, 1.03\~1.05, … | Infinite set: 1.3, 1.4, 1.5, 1.03, 1.04, 1.05, … |

* In determining whether a set of samples is infinite, leading zero integer digits and trailing zero decimals are not significant. Thus "i = 1000 and f = 0" is satisfied by 01000, 1000, 1000.0, 1000.00, 1000.000, 01c3 etc. but is still considered finite.


#### <a name="Using_cardinals" id="Using_cardinals" href="#Using_cardinals">Using Cardinals</a>

* Elements such as `<currencyFormats>`, `<currency>` and `<unit>` provide selection among subelements designating various localized cardinal plural forms by tagging each of the relevant subelements with a different count value, or with no count value in some cases. Note that the plural forms for a specific currencyFormat, unit type, or currency type may not use all of the different plural-form tags defined for the language. To format a currency or unit type for a particular numeric value, determine the count value according to the plural rules for the language, then select the appropriate display form for the currency format, currency type or unit type using the rules in those sections:


* 2.3 [Number Symbols](#Number_Symbols) (for `currencyFormat`s elements)
* [Currencies](#Currencies) (for `currency` elements)
* The main document [Unit Elements](tr35.md#Unit_Elements)

### <a name="Plural_Ranges" id="Plural_Ranges" href="#Plural_Ranges">Plural Ranges</a>

```dtd
<!ELEMENT pluralRanges (pluralRange*) >
<!ATTLIST pluralRanges locales NMTOKENS #REQUIRED >

<!ELEMENT pluralRange ( #PCDATA ) >
<!ATTLIST pluralRange start (zero|one|two|few|many|other) #IMPLIED >
<!ATTLIST pluralRange end (zero|one|two|few|many|other) #IMPLIED >
<!ATTLIST pluralRange result (zero|one|two|few|many|other) #REQUIRED >
```
* Often ranges of numbers are presented to users, such as in “Length: 3.2–4.5 centimeters”. This means any length from 3.2 cm to 4.5 cm, inclusive. However, different languages have different conventions for the pluralization given to a range: should it be “0–1 centimeter” or “0–1 centimeters”? This becomes much more complicated for languages that have many different plural forms, such as Russian or Arabic.


* The `pluralRanges` element provides information allowing an implementation to derive the plural category of a range from the plural categories of the `start` and `end` values. If there is no value for a _<`start`,`end`>_ pair, the default result is `end`. However, where that result has been verified for a given language, it is included in the CLDR data.


* The data has been gathered presuming that in any usage, the start value is strictly less than the end value, and that no values are negative. Results for any cases that do not meet these criteria are undefined.


For the formatting of number ranges, see <a href="#Number_Range_Formatting">Number Range Formatting</a>.

