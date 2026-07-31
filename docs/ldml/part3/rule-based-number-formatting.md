## <a name="Rule-Based_Number_Formatting" id="Rule-Based_Number_Formatting" href="#Rule-Based_Number_Formatting">Rule-Based Number Formatting</a>

```dtd
<!ELEMENT rbnf ( alias | rulesetGrouping*) >

<!ELEMENT rulesetGrouping ( alias | rbnfRules? ruleset*) >
<!ATTLIST rulesetGrouping type NMTOKEN #REQUIRED>

<!ELEMENT rbnfRules ( #PCDATA )>

<!ELEMENT ruleset ( alias | rbnfrule*) >
<!ATTLIST ruleset type NMTOKEN #REQUIRED>
<!ATTLIST ruleset access ( public | private ) #IMPLIED >

<!ELEMENT rbnfrule ( #PCDATA ) >
<!ATTLIST rbnfrule value CDATA #REQUIRED >
<!ATTLIST rbnfrule radix CDATA #IMPLIED >
<!ATTLIST rbnfrule decexp CDATA #IMPLIED >
```
* The rule-based number format (RBNF) encapsulates a set of rules for transforming numeric values to and from a representation words that represent a number. For example, format 25,376 as "twenty-five thousand three hundred seventy-six" or "vingt-cinq mille trois cent soixante-seize" or "fünf­und­zwanzig­tausend­drei­hundert­sechs­und­siebzig" depending on the language being used. These rules are typically used for spelling out numeric values, but can also be used for other number systems like roman numerals, Chinese numerals, or for ordinal numbers with digits (e.g. 1st, 2nd, 3rd, …).


* Where, however, the [[CLDR plurals or ordinals]](#language-plural-rules) can be used, their usage is recommended in preference to the RBNF data. First, the RBNF data may be missing some relevant forms for grammatical case or types over some languages that otherwise have modern coverage. Secondly, the choice of rules requires additional language specific context. CLDR does not supply any data for when to use one vs another (e.g. when to use `spellout-cardinal-masculine` vs `spellout-cardinal-feminine`). So these data are insufficient without additional software that provides grammatical context to choose the correct rule for grammatical agreement in a sentence.


### <a name="Rule-Based_Number_Formatting_Scope" id="Rule-Based_Number_Formatting_Scope" href="#Rule-Based_Number_Formatting_Scope">Rule-Based Number Formatting Scope</a>

```xml
<ruleSetGrouping>
```
Used to group rules into functional sets. There are 3 known rule types. They are `SpelloutRules`, `NumberingSystemRules`, and `OrdinalRules`.

#### <a name="SpelloutRules" id="SpelloutRules" href="#SpelloutRules">SpelloutRules</a>

* The `SpelloutRules` type is used for representing a numerical datatype with words that are typically found in speech. There are 4 common rule categories for spellout rules. A language with complete coverage will cover all of these categories. Some languages may provide more than these 4 types depending on what is relevant for a given language:


#### <a name="numbering" id="numbering" href="#numbering">numbering</a>
* This is the default used when there is no context for the number. Usually this is used for counting without reference to a noun. For many languages, this may also be used for enumeration of objects, like used when pronouncing "table number one" and "table number two". It can also be used for pronouncing a math equation, like "2 - 3 = -1".

#### <a name="numberingyear" id="numberingyear" href="#numberingyear">numbering-year</a>
* This is used for cases where years are pronounced or written a certain way. An example in English is the year 1999, which comes out as "nineteen ninety-nine" instead of the numbering value "one thousand nine hundred ninety-nine". The rules for this type have undefined behavior for non-integer numbers, and values less than 1.

#### <a name="cardinal" id="cardinal" href="#cardinal">cardinal</a>
* This is used when providing the quantity of the number of objects. For many languages, there may not be a default cardinal type. Many languages require the notion of the gender and other grammatical properties so that the number and the objects being referenced are in grammatical agreement. An example of its usage is "one e-mail", "two people" or "three kilometers". Some languages may not have dedicated words for 0 or negative numbers for cardinals. In those cases, the words from the numbering type can be reused.

#### <a name="ordinal" id="ordinal" href="#ordinal">ordinal</a>
* This is used when providing the order of the number of objects. For many languages, there may not be a default ordinal type. Many languages also require the notion of the gender for ordinal so that the ordinal number and the objects being referenced are in grammatical agreement. An example of its usage is "first place", "second e-mail" or "third house on the right". The rules for this type have undefined behavior for non-integer numbers, and values less than 1.


#### <a name="NumberingSystemRules" id="NumberingSystemRules" href="#NumberingSystemRules">NumberingSystemRules</a>

* The `NumberingSystemRules` type is used for numbering systems. Even though they may be derived from a specific culture, they are typically not translated and the rules are in **root**. An example of these rules are the Roman numerals where the value 8 comes out as VIII. These are typically supported in scenarios where the numbering system does not use decimal digits, but symbols are used to represent a number.


#### <a name="OrdinalRules" id="OrdinalRules" href="#OrdinalRules">OrdinalRules</a>

The `OrdinalRules` type is used for ordinal numbers with digits (e.g. 1st, 2nd, 3rd, …). If a language does not have such a concept, then it should be the format of numbers in an ordered list.

### <a name="RBNF_Limitations" id="RBNF_Limitations" href="#RBNF_Limitations">Limitations</a>

* With regards to the number range supported for all these number types, the largest possible number range tries to be supported, but some languages may not have words for large numbers. For example, the old Roman numbering system can't support the value 5000 and beyond. For those unsupported cases, the default number format from CLDR is used.


* For most languages, the largest number represented by the number rules in CLDR is typically 1 quintillion - 1 or 10<sup>18</sup> - 1. Some languages may not have commonly recognized words for these large numbers. These larger values are also hard to represent accurately in an IEEE 754 double floating point number, which only has 53 bits of precision. A signed 64-bit number can only represent 9.2 quintillion. Even if larger values were supported, the value of such translations would be limited.


* Number types that have a small limited range are generally not supported by these number rules. For example, multiplicatives (e.g. single, double, triple) could be supported, but the range is so small that the entirety of well known words could be represented with a small lookup table. There is no recursion needed to support such rules. Due to their limited nature with a narrow range, such rules are generally not supported.


* Numbers with units are inappropriate to use within these number rules. It's more appropriate to format or parse the number component, and to use a message format framework to format the unit. This number rules based system does not scale well with the number of combinations of possible values with units and subunits. Such implementations will also struggle with adjusting the precision for formatting or the relevant unit recognized when parsing. It is more scalable to use this number rules with a [[Language Plural Rules]](#language-plural-rules) with a message formatting framework.


### <a name="RBNF_Syntax" id="RBNF_Syntax" href="#RBNF_Syntax">Syntax of `rbnfRules`</a>
```xml
<rbnfRules>
```
The syntax is carried over from the ICU based RBNF rules. The rules are fairly sophisticated. For more details see [_Rule-Based Number Formatter_](tr35.md#RBNF).

In its simplest form, the description consists of a semicolon-delimited list of *rules*.
Each rule has a string of output text and a value or range of values it is applicable to.
In a typical spellout rule set, the first twenty rules are the words for the numbers from
0 to 19:

```
zero; one; two; three; four; five; six; seven; eight; nine;
ten; eleven; twelve; thirteen; fourteen; fifteen; sixteen; seventeen; eighteen; nineteen;
```
For larger numbers, we can use the preceding set of rules to format the ones place, and
we only have to supply the words for the multiples of 10:

```
20: twenty[->>];
30: thirty[->>];
40: forty[->>];
50: fifty[->>];
60: sixty[->>];
70: seventy[->>];
80: eighty[->>];
90: ninety[->>];
```
In these rules, the *base value* is spelled out explicitly and set off from the
rule's output text with a colon. The rules are in a sorted list, and a rule is applicable
to all numbers from its own base value to one less than the next rule's base value. The
">>" token is called a *substitution* and tells the formatter to
isolate the number's ones digit, format it using this same set of rules, and place the
result at the position of the ">>" token. Text in brackets is omitted if
the number being formatted is an even multiple of 10 (the hyphen is a literal hyphen; 24
is "twenty-four," not "twenty four").

For even larger numbers, we can actually look up several parts of the number in the
list:

```
100: << hundred[ >>];
```
The "<<" represents a new kind of substitution. The << isolates
the hundreds digit (and any digits to its left), formats it using this same rule set, and
places the result where the "<<" was. Notice also that the meaning of >>
has changed: it now refers to both the tens and the ones digits. The meaning of
both substitutions depends on the rule's base value. The base value determines the rule's *divisor*,
which is the highest power of 10 that is less than or equal to the base value (the user
can change this). To fill in the substitutions, the formatter divides the number being
formatted by the divisor. The integral quotient is used to fill in the <<
substitution, and the remainder is used to fill in the >> substitution. The meaning
of the brackets changes similarly: text in brackets is omitted if the value being
formatted is an even multiple of the rule's divisor. The rules are applied recursively, so
if a substitution is filled in with text that includes another substitution, that
substitution is also filled in.

This rule covers values up to 999, at which point we add another rule:

```
1000: << thousand[ >>];
```
Just like the 100 rule, the meanings of the brackets and substitution tokens shift because the rule's
base value is a higher power of 10, changing the rule's divisor. This rule can actually be
used all the way up to 999,999. This allows us to finish out the rules as follows:

```
1,000,000: << million[ >>];
1,000,000,000: << billion[ >>];
1,000,000,000,000: << trillion[ >>];
1,000,000,000,000,000: =#,##0=;
```
Commas, periods, and spaces can be used in the base values to improve legibility and
are ignored by the rule parser. The last rule in the list is customarily treated as an
"overflow rule", which applies to everything from its base value on up.
It is often used to print out a default representation, which in this case is the decimal format syntax.

To see how these rules actually work in practice, consider the following example.
Formatting 25,340 with this rule set would work like this:

| Rule                                         | Description                                                                                                                         |
|----------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| **<< thousand >>**                           | The rule whose base value is 1,000 is applicable to 25,340                                                                          |
| **twenty->>** thousand >>                    | 25,340 over 1,000 is 25. The rule for 20 applies.                                                                                   |
| twenty-**five** thousand >>                  | 25 mod 10 is 5. The rule for 5 is "five."                                                                                           |
| twenty-five thousand **<< hundred >>**       | 25,340 mod 1,000 is 340. The rule for 100 applies.                                                                                  |
| twenty-five thousand **three** hundred >>    | 340 over 100 is 3. The rule for 3 is "three."                                                                                       |
| twenty-five thousand three hundred **forty** | 340 mod 100 is 40. The rule for 40 applies. Since 40 divides evenly by 10, the hyphen and substitution in the brackets are omitted. |

The above syntax suffices only to format positive integers. To format negative numbers, we add a special rule:

```
-x: minus >>;
```
This is called a *negative-number rule*, and is identified by "-x"
where the base value would be. This rule is used to format all negative numbers. the >>
token here means "find the number's absolute value, format it with these
rules, and put the result here."

We also add a special rule called a *fraction rule* for numbers with fractional parts:

```
x.x: << point >>;
```
This rule is used for all positive non-integers (negative non-integers pass through the
negative-number rule first and then through this rule). Here, the << token refers to
the number's integral part, and the >> to the number's fractional part. The
fractional part is formatted as a series of single-digit numbers (e.g., 123.456 would be
formatted as "one hundred twenty-three point four five six").

### <a name="RBNF_Syntax_Rule_Set" id="RBNF_Syntax_Rule_Set" href="#RBNF_Syntax_Rule_Set">Rule Sets</a>

Multiple sets of rules can be defined with one or more *rule
sets*. Each rule set consists of a name, a colon, and a list of *rules*. A rule
set name must begin with a % sign. Rule sets with a name that begins with a single % sign
are *public*, and that name can be referenced to format and parse numbers.
Rule sets with names that begin with %% are *private*. They exist only for the use
by other rule sets. If a formatter only has one rule set, the name may be omitted.

To improve parsing of numbers, a special "rule set" named `%%lenient-parse` can be used.
The body of `%%lenient-parse` isn't a set of number-formatting rules. It is a set of [collation rules](tr35-collation.md).
These rules define equivalences for lenient parsing. Symbols that have syntactic meaning
in collation rules, such as '&', have no particular meaning when appearing outside
of the `lenient-parse` rule set.

The body of a rule set consists of an ordered, semicolon-delimited list of *rules*.
Internally, every rule has a base value, a divisor, rule text, and zero, one, or two *substitutions*.
These parameters are controlled by the description syntax, which consists of a *rule
descriptor*, a colon, and a *rule body*.

A rule descriptor can take one of the following forms. The text in *italics* is the
name of a token.

| Descriptor   | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
|--------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| *bv*:        | *bv* specifies the rule's base value. *bv* is a decimal number expressed using ASCII digits. *bv* may contain spaces, period, and commas, which are ignored. The rule's divisor is the highest power of 10 less than or equal to the base value.                                                                                                                                                                                                                                                                                                                           |
| *bv*/*rad*:  | *bv* specifies the rule's base value. The rule's divisor is the highest power of *rad* less than or equal to the base value.                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| *bv*>:       | *bv* specifies the rule's base value. To calculate the divisor, let the radix be 10, and the exponent be the highest exponent of the radix that yields a result less than or equal to the base value. Every > character after the base value decreases the exponent by 1. If the exponent is positive or 0, the divisor is the radix raised to the power of the exponent; otherwise, the divisor is 1.                                                                                                                                                                     |
| *bv*/*rad*>: | *bv* specifies the rule's base value. To calculate the divisor, let the radix be *rad*, and the exponent be the highest exponent of the radix that yields a result less than or equal to the base value. Every > character after the radix decreases the exponent by 1. If the exponent is positive or 0, the divisor is the radix raised to the power of the exponent; otherwise, the divisor is 1.                                                                                                                                                                       |
| -x:          | The rule is a negative-number rule.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| x.x          | The rule is an *improper fraction rule*. If the full stop in the middle of the rule name is replaced with the decimal point that is used in the language or a [Decimal Format Pattern](#Number_Format_Patterns), then that rule will have precedence when formatting and parsing this rule. For example, some languages use the comma, and can thus be written as x,x instead. For example, you can use "x.x: << point >>;x,x: << comma >>;" to handle the decimal point that matches the language's natural spelling of the punctuation of either the full stop or comma. |
| 0.x:         | The rule is a *proper fraction rule*. If the full stop in the middle of the rule name is replaced with the decimal point that is used in the language or [Decimal Format Pattern](#Number_Format_Patterns), then that rule will have precedence when formatting and parsing this rule. For example, some languages use the comma, and can thus be written as 0,x instead. For example, you can use "0.x: point >>;0,x: comma >>;" to handle the decimal point that matches the language's natural spelling of the punctuation of either the full stop or comma.            |
| x.0:         | The rule is a *default rule*. If the full stop in the middle of the rule name is replaced with the decimal point that is used in the language or [Decimal Format Pattern](#Number_Format_Patterns), then that rule will have precedence when formatting and parsing this rule. For example, some languages use the comma, and can thus be written as x,0 instead. For example, you can use "x.0: << point;x,0: << comma;" to handle the decimal point that matches the language's natural spelling of the punctuation of either the full stop or comma.                    |
| Inf:         | The rule for infinity.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| NaN:         | The rule for an IEEE 754 NaN (not a number).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| *nothing*    | If the rule's rule descriptor is left out, the base value is one plus the preceding rule's base value (or zero if this is the first rule in the list) in a normal rule set. In a fraction rule set, the base value is the same as the preceding rule's base value.                                                                                                                                                                                                                                                                                                         |

A rule set may be either a regular rule set or a *fraction rule set*, depending
on whether it is used to format a number's integral part (or the whole number) or a
number's fractional part. Using a rule set to format a rule's fractional part makes it a
fraction rule set.

Which rule is used to format a number is defined according to one of the following
algorithms. If the rule set is a regular rule set, do the following:

* If the rule set includes a default rule (and the number was passed in as a `double`),
    use the default rule. If the number being formatted was passed in as a `long`,
    the default rule is ignored.
* If the number is negative, use the negative-number rule.
* If the number has a fractional part and is greater than 1, use the improper fraction rule.
* If the number has a fractional part and is between 0 and 1, use the proper fraction rule.
* Binary-search the rule list for the rule with the highest base value less than or equal
    to the number. If that rule has two substitutions, its base value is not an even multiple
    of its divisor, and the number *is* an even multiple of the rule's divisor, use the
    rule that precedes it in the rule list. Otherwise, use the rule itself.

If the rule set is a fraction rule set, do the following:

* Ignore negative-number and fraction rules.
* For each rule in the list, multiply the number being formatted (which will always be
    between 0 and 1) by the rule's base value. Keep track of the distance between the result
    the nearest integer.
* Use the rule that produced the result closest to zero in the above calculation. In the
    event of a tie or a direct hit, use the first matching rule encountered. The idea here is
    to try each rule's base value as a possible denominator of a fraction. Whichever
    denominator produces the fraction closest in value to the number being formatted wins. If
    the rule following the matching rule has the same base value, use it if the numerator of
    the fraction is anything other than 1; if the numerator is 1, use the original matching
    rule. This is to allow singular and plural forms of the rule text without a lot of extra
    hassle.

A rule's body consists of a string of characters terminated by a semicolon. The rule
may include zero, one, or two *substitution tokens*, and a range of text in
brackets. The brackets denote optional text (and may also include one or both
substitutions). The exact meanings of the substitution tokens, and under what conditions
optional text is omitted, depend on the syntax of the substitution token and the context.
The rest of the text in a rule body is literal text that is output when the rule matches
the number being formatted.

A substitution token begins and ends with a *token character*. The token
character and the context together specify a mathematical operation to be performed on the
number being formatted. An optional *substitution descriptor* specifies how the
value resulting from that operation is used to fill in the substitution. The position of
the substitution token in the rule body specifies the location of the resultant text in
the original rule text.

The meanings of the substitution token characters are as follows:

| Syntax                         | Context                      | Usage                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
|--------------------------------|------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| \>>                            | in normal rule               | Divide the number by the rule's divisor and format the remainder.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| \>>                            | in negative-number rule      | Find the absolute value of the number and format the result.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| \>>                            | in fraction or default rule  | Isolate the number's fractional part and format it.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| \>>                            | in rule in fraction rule set | Not allowed.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| \>>>                           | in normal rule               | Divide the number by the rule's divisor and format the remainder, but bypass the normal rule-selection process and just use the rule that precedes this one in this rule list.                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| \>>>                           | in all other rules           | Not allowed.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| <<                             | in normal rule               | Divide the number by the rule's divisor, perform floor() on the quotient, and format the resulting value.<br> If there is a [Decimal Format Pattern](#Number_Format_Patterns) between the < characters and the rule does NOT also contain a >> substitution, we don't perform floor() on the quotient. The quotient is passed through to the [Decimal Format Pattern](#Number_Format_Patterns) intact.  That is, for the value 1,900:<ul><li>"1/1000: << thousand;" will produce "one thousand"</li><li>"1/1000: <0< thousand;" will produce "2 thousand" (*not* "1 thousand")</li><li>"1/1000: <0< thousand >0>;" will produce "1 thousand 900"</ul> |
| <<                             | in negative-number rule      | Not allowed.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| <<                             | in fraction or default rule  | Isolate the number's integral part and format it.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| <<                             | in rule in fraction rule set | Multiply the number by the rule's base value and format the result.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| ==                             | in all rule sets             | Format the number unchanged                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| []<br/>[\|]                    | in normal rule               | <ul><li>When the number is not an even multiple of the rule's divisor, use the text and rules between the beginning square bracket, and the end square bracket or the \| symbol.</li> <li>When the number is an even multiple of the rule's divisor, and no \| symbol is used, omit the text.</li> <li>When the number is an even multiple of the rule's divisor, and \| symbol is used, use the text and rules between the \| symbol, and the end square bracket.</li></ul>                                                                                                                                                                          |
| []<br/>[\|]                    | in improper-fraction rule    | This syntax is the same as specifying both an x.x rule and a 0.x rule. <ul><li>When the number is not between 0 and 1, use the text and rules between the beginning square bracket, and the end square bracket or the \| symbol.</li><li>When the number is between 0 and 1, and no \| symbol is used, omit the text.</li><li>When the number is between 0 and 1, and \| symbol is used, use the text and rules between the \| symbol, and the end square bracket.</li></ul>                                                                                                                                                                          |
| []<br/>[\|]                    | in default rule              | This syntax is the same as specifying both an x.x rule and an x.0 rule. <ul><li>When the number is not an integer, use the text and rules between the beginning square bracket, and the end square bracket or the \| symbol.</li> <li>When the number is an integer, and no \| symbol is used, omit the text.</li> <li>When the number is an integer, and \| symbol is used, use the text and rules between the \| symbol, and the end square bracket.</li></ul>                                                                                                                                                                                      |
| []<br/>[\|]                    | in rule in fraction rule set | <ul><li>When multiplying the number by the rule's base value does not yield 1, use the text and rules between the beginning square bracket, and the end square bracket or the \| symbol.</li> <li>When multiplying the number by the rule's base value yields 1, and no \| symbol is used, omit the text.</li> <li>When multiplying the number by the rule's base value yields 1, and \| symbol is used, use the text and rules between the \| symbol, and the end square bracket.</li></ul>                                                                                                                                                          |
| []<br/>[\|]                    | in proper-fraction rule      | Not allowed.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| []<br/>[\|]                    | in negative-number rule      | Not allowed.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| \$(cardinal,*plural syntax*)\$ | in all rule sets             | This provides the ability to choose a word based on the number divided by the radix to the power of the exponent of the base value for the specified locale, which is normally equivalent to the << value. This uses the cardinal plural rules from [[Language Plural Rules]](#language-plural-rules). All strings used in the plural format are treated as the same base value for parsing.                                                                                                                                                                                                                                                          |
| \$(ordinal,*plural syntax*)\$  | in all rule sets             | This provides the ability to choose a word based on the number divided by the radix to the power of the exponent of the base value for the specified locale, which is normally equivalent to the << value. This uses the ordinal plural rules from [[Language Plural Rules]](#language-plural-rules). All strings used in the plural format are treated as the same base value for parsing.                                                                                                                                                                                                                                                           |

The substitution descriptor (i.e., the text between the token characters) may take one of three forms:

| Descriptor                                          | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
|-----------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| a rule set name                                     | Perform the mathematical operation on the number, and format the result using the named rule set.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| a [Decimal Format Pattern](#Number_Format_Patterns) | Perform the mathematical operation on the number, and format the result using a [Decimal Format Pattern](#Number_Format_Patterns) with the specified pattern. The pattern must begin with 0 or #.                                                                                                                                                                                                                                                                                                                                                                                  |
| nothing                                             | Perform the mathematical operation on the number, and format the result using the rule set containing the current rule, except: <ul><li>You can't have an empty substitution descriptor with a == substitution.</li> <li>If you omit the substitution descriptor in a >> substitution in a fraction rule, format the result one digit at a time using the rule set containing the current rule.</li> <li>If you omit the substitution descriptor in a << substitution in a rule in a fraction rule set, format the result using the default rule set for this formatter.</li></ul> |

Whitespace is ignored between a rule set name and a rule set body, between a rule
descriptor and a rule body, or between rules. If a rule body begins with an apostrophe,
the apostrophe is ignored, but all text after it becomes significant (this is how you can
have a rule's rule text begin with whitespace). There is no escape function: the semicolon
is not allowed in rule set names or in rule text, and the colon is not allowed in rule set
names. The characters beginning a substitution token are always treated as the beginning
of a substitution token.

### <a name="RBNF_Remove_Ruleset_Rule" id="RBNF_Remove_Ruleset_Rule" href="#RBNF_Remove_Ruleset_Rule">Planned removal of ruleset and rule tags</a>

The following `<ruleset>` and `<rule>` tags will be removed in the next release. They contain redundant information contained in `<rbnfRules>` to provide time to transition to `<rbnfRules>`.

```xml
<ruleset>
```
This element denotes a specific rule set to the number formatter. The ruleset is assumed to be a public ruleset unless the attribute type="private" is specified.

```xml
<rule>
```

* Contains the actual formatting rule for a particular number or sequence of numbers. The `value` attribute is used to indicate the starting number to which the rule applies. The actual text of the rule is identical to the ICU syntax, with the exception that Unicode left and right arrow characters are used to replace < and > in the rule text, since < and > are reserved characters in XML. The `radix` attribute is used to indicate an alternate radix to be used in calculating the prefix and postfix values for number formatting. Alternate radix values are typically used for formatting year numbers in formal documents, such as "nineteen hundred seventy-six" instead of "one thousand nine hundred seventy-six".


