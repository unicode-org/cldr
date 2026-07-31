<div class="uts-header">
  <div class="uts-header-top">
    <span class="uts-badge">Unicode® Technical Standard #35</span>
    <span class="uts-version-tag">Version 49 (Draft) • Revision 79</span>
  </div>
  <div class="uts-title">Unicode Locale Data Markup Language (LDML)</div>
  <div class="uts-part">Part 3: Numbers</div>
  <div class="uts-meta-grid">
    <div><strong>Editor:</strong> Shane F. Carr (Google)</div>
    <div><strong>Namespace:</strong> <code>https://www.unicode.org/cldr/</code></div>
    <div><strong>Corrigenda:</strong> <a href="https://cldr.unicode.org/index/corrigenda" target="_blank">cldr.unicode.org</a></div>
    <div><strong>Latest Version:</strong> <a href="https://www.unicode.org/reports/tr35/" target="_blank">tr35</a></div>
  </div>
</div>

# Part 3: Numbers

### <a name="_Summary_" id="_Summary_" href="#_Summary_">_Summary_</a>

* This document describes parts of an XML format (_vocabulary_) for the exchange of structured locale data. This format is used in the [Unicode Common Locale Data Repository](https://www.unicode.org/cldr/).


* This is a partial document, describing only those parts of the LDML that are relevant for number and currency formatting. For the other parts of the LDML see the [main LDML document](tr35.md) and the links above.


### <a name="_Status_" id="_Status_" href="#_Status_">_Status_</a>

<div id='currentStatus'></div>


> _**A Unicode Technical Standard (UTS)** is an independent specification. Conformance to the Unicode Standard does not imply conformance to any UTS._

_Please submit corrigenda and other comments with the CLDR bug reporting form [[Bugs](https://cldr.unicode.org/index/bug-reports)].
Related information that is useful in understanding this document is found in the [References](tr35.md#References).
For the latest version of the Unicode Standard see [[Unicode](https://www.unicode.org/versions/latest/)].
For more information see [About Unicode Technical Reports](https://www.unicode.org/reports/about-reports.html) and the [Specifications FAQ](https://www.unicode.org/faq/specifications.html).
Unicode Technical Reports are governed by the Unicode [Terms of Use](https://www.unicode.org/copyright.html)._

## <a name="Parts" id="Parts" href="#Parts">Parts</a>

The LDML specification is divided into the following parts:

*   Part 1: [Core](tr35.md#Contents) (languages, locales, basic structure)
*   Part 2: [General](tr35-general.md#Contents) (display names & transforms, etc.)
*   Part 3: [Numbers](tr35-numbers.md#Contents) (number & currency formatting)
*   Part 4: [Dates](tr35-dates.md#Contents) (date, time, time zone formatting)
*   Part 5: [Collation](tr35-collation.md#Contents) (sorting, searching, grouping)
*   Part 6: [Supplemental](tr35-info.md#Contents) (supplemental data)
*   Part 7: [Keyboards](tr35-keyboards.md#Contents) (keyboard mappings)
*   Part 8: [Person Names](tr35-personNames.md#Contents) (person names)
*   Part 9: [MessageFormat](tr35-messageFormat.md#Contents) (message format)
*   Appendix A: [Modifications](tr35-modifications.md#modifications)
*   Appendix B: [Acknowledgments](tr35-acknowledgments.md#acknowledgments)

## <a name="Contents" id="Contents" href="#Contents">Contents of Part 3, Numbers</a>

* [Numbering Systems](#Numbering_Systems) 
* [Number Elements](#Number_Elements) 
  * [Default Numbering System](#defaultNumberingSystem) 
  * [Other Numbering Systems](#otherNumberingSystems) 
  * [Number Symbols](#Number_Symbols) 
  * [Number Formats](#Number_Formats) 
    * [Compact Number Formats](#Compact_Number_Formats) 
    * [Currency Formats](#Currency_Formats) 
    * [Element Placement](#Element_Placement) 
    * [Format Types & Display Forms](#Format_Types_and_Display_Forms) 
    * [Pattern Variant Usage Rules](#Pattern_Variant_Usage_Rules) 
  * [Miscellaneous Patterns](#Miscellaneous_Patterns) 
  * [Minimal Pairs](#Minimal_Pairs) 
* [Number Format Patterns](#Number_Format_Patterns) 
  * [Number Patterns](#Number_Patterns) 
    * [Table: Number Pattern Examples](#Number_Pattern_Examples) 
  * [Special Pattern Characters](#Special_Pattern_Characters) 
    * [Table: Number Pattern Character Definitions](#Number_Pattern_Character_Definitions) 
    * [Table: Sample Patterns and Results](#Sample_Patterns_and_Results) 
    * [Explicit Plus Signs](#Explicit_Plus) 
  * [Formatting](#Formatting) 
  * [Scientific Notation](#sci) 
  * [Significant Digits](#sigdig) 
    * [Table: Significant Digits Examples](#Significant_Digits_Examples) 
  * [Padding](#Padding) 
  * [Rounding](#Rounding) 
  * [Quoting Rules](#Quoting_Rules) 
* [Rational Numbers](#Rational_Numbers) 
* [Currencies](#Currencies) 
  * [Supplemental Currency Data](#Supplemental_Currency_Data) 
* [Language Plural Rules](#Language_Plural_Rules) 
  * [Explicit 0 and 1 rules](#Explicit_0_1_rules) 
  * [Plural rules syntax](#Plural_rules_syntax) 
    * [Operands](#Operands) 
      * [Table: Plural Operand Meanings](#Plural_Operand_Meanings) 
      * [Table: Plural Operand Examples](#Plural_Operand_Examples) 
    * [Relations](#Relations) 
      * [Table: Relations Examples](#Relations_Examples) 
      * [Table: Plural Rules Examples](#Plural_Rules_Examples) 
    * [Samples](#Samples) 
      * [Table: Plural Samples Examples](#Plural_Samples_Examples) 
    * [Using Cardinals](#Using_cardinals) 
  * [Plural Ranges](#Plural_Ranges) 
* [Rule-Based Number Formatting](#Rule-Based_Number_Formatting) 
  * [Rule-Based Number Formatting Scope](#Rule-Based_Number_Formatting_Scope) 
    * [SpelloutRules](#SpelloutRules) 
    * [numbering](#numbering) 
    * [numbering-year](#numberingyear) 
    * [cardinal](#cardinal) 
    * [ordinal](#ordinal) 
    * [NumberingSystemRules](#NumberingSystemRules) 
    * [OrdinalRules](#OrdinalRules) 
  * [Limitations](#RBNF_Limitations) 
  * [Syntax of `rbnfRules`](#RBNF_Syntax) 
  * [Rule Sets](#RBNF_Syntax_Rule_Set) 
  * [Planned removal of ruleset and rule tags](#RBNF_Remove_Ruleset_Rule) 
* [Parsing Numbers](#Parsing_Numbers) 
* [Number Range Formatting](#Number_Range_Formatting) 
  * [Approximate Number Formatting](#Approximate_Number_Formatting) 
  * [Collapsing Number Ranges](#Collapsing_Number_Ranges) 
  * [Range Pattern Processing](#Range_Pattern_Processing) 

