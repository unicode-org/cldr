<div class="uts-header">
  <div class="uts-header-top">
    <span class="uts-badge">Unicode® Technical Standard #35</span>
    <span class="uts-version-tag">Version 49 (Draft) • Revision 79</span>
  </div>
  <div class="uts-title">Unicode Locale Data Markup Language (LDML)</div>
  <div class="uts-part">Part 9: MessageFormat</div>
  <div class="uts-meta-grid">
    <div><strong>Editor:</strong> Eemeli Aro, Addison Phillips</div>
    <div><strong>Namespace:</strong> <code>https://www.unicode.org/cldr/</code></div>
    <div><strong>Corrigenda:</strong> <a href="https://cldr.unicode.org/index/corrigenda" target="_blank">cldr.unicode.org</a></div>
    <div><strong>Latest Version:</strong> <a href="https://www.unicode.org/reports/tr35/" target="_blank">tr35</a></div>
  </div>
</div>

# Part 9: MessageFormat

### <a name="_Summary_" id="_Summary_" href="#_Summary_">_Summary_</a>

This specification defines the data model, syntax, processing, and conformance requirements for the next generation of dynamic messages.

This is a partial document, describing only those parts of the LDML that are relevant for message format. For the other parts of the LDML see the [main LDML document](tr35.md) and the links above.

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

## <a name="Contents" id="Contents" href="#Contents">Contents of Part 9, MessageFormat</a>

* [Introduction](#Introduction) 
  * [Conformance](#Conformance) 
  * [Terminology and Conventions](#Terminology_and_Conventions) 
  * [Stability Policy](#Stability_Policy) 
* [Syntax](#Syntax) 
  * [Design Goals](#Design_Goals) 
  * [Design Restrictions](#Design_Restrictions) 
  * [Messages and their Syntax](#Messages_and_their_Syntax) 
    * [Well-formed vs. Valid Messages](#Wellformed_vs_Valid_Messages) 
  * [The Message](#The_Message) 
    * [Declarations](#Declarations) 
    * [Complex Body](#Complex_Body) 
  * [Pattern](#Pattern) 
    * [Quoted Pattern](#Quoted_Pattern) 
    * [Text](#Text) 
    * [Placeholder](#Placeholder) 
  * [Matcher](#Matcher) 
    * [Selector](#Selector) 
    * [Variant](#Variant) 
    * [Key](#Key) 
  * [Expressions](#Expressions) 
    * [Operand](#Operand) 
    * [Function](#Function) 
      * [Options](#Options) 
  * [Markup](#Markup) 
  * [Attributes](#Attributes) 
  * [Other Syntax Elements](#Other_Syntax_Elements) 
    * [Keywords](#Keywords) 
    * [Literals](#Literals) 
    * [Names and Identifiers](#Names_and_Identifiers) 
  * [Escape Sequences](#Escape_Sequences) 
    * [Whitespace](#Whitespace) 
  * [Complete ABNF](#Complete_ABNF) 
* [message.abnf](#messageabnf) 
* [Formatting](#Formatting) 
  * [Formatting Context](#Formatting_Context) 
  * [Resolved Values](#Resolved_Values) 
  * [Expression and Markup Resolution](#Expression_and_Markup_Resolution) 
    * [Expression Resolution](#Expression_Resolution) 
    * [Literal Resolution](#Literal_Resolution) 
    * [Variable Resolution](#Variable_Resolution) 
    * [Function Resolution](#Function_Resolution) 
      * [Function Handler](#Function_Handler) 
    * [Markup Resolution](#Markup_Resolution) 
    * [Option Resolution](#Option_Resolution) 
    * [Fallback Resolution](#Fallback_Resolution) 
  * [Pattern Selection](#Pattern_Selection) 
    * [Operations on Resolved Values](#Operations_on_Resolved_Values) 
    * [Resolve Selectors](#Resolve_Selectors) 
    * [Compare Variants](#Compare_Variants) 
    * [SelectorsMatch](#SelectorsMatch) 
    * [SelectorsCompare](#SelectorsCompare) 
    * [NormalizeKey](#NormalizeKey) 
  * [Formatting of the Selected Pattern](#Formatting_of_the_Selected_Pattern) 
    * [Formatting Examples](#Formatting_Examples) 
    * [Formatting Fallback Values](#Formatting_Fallback_Values) 
    * [Handling Bidirectional Text](#Handling_Bidirectional_Text) 
* [Errors](#Errors) 
  * [Error Handling](#Error_Handling) 
  * [Syntax Errors](#Syntax_Errors) 
  * [Data Model Errors](#Data_Model_Errors) 
    * [Variant Key Mismatch](#Variant_Key_Mismatch) 
    * [Missing Fallback Variant](#Missing_Fallback_Variant) 
    * [Missing Selector Annotation](#Missing_Selector_Annotation) 
    * [Duplicate Declaration](#Duplicate_Declaration) 
    * [Duplicate Option Name](#Duplicate_Option_Name) 
    * [Duplicate Variant](#Duplicate_Variant) 
  * [Resolution Errors](#Resolution_Errors) 
    * [Unresolved Variable](#Unresolved_Variable) 
    * [Unknown Function](#Unknown_Function) 
    * [Bad Selector](#Bad_Selector) 
  * [Message Function Errors](#Message_Function_Errors) 
    * [Bad Operand](#Bad_Operand) 
    * [Bad Option](#Bad_Option) 
    * [Bad Variant Key](#Bad_Variant_Key) 
    * [Unsupported Operation](#Unsupported_Operation) 
* [Default Functions](#Default_Functions) 
  * [String Value Selection and Formatting](#String_Value_Selection_and_Formatting) 
    * [The `:string` function](#The_string_function) 
    * [`:string` Operands](#string_Operands) 
    * [`:string` Options](#string_Options) 
    * [`:string` Resolved Value](#string_Resolved_Value) 
    * [Selection with `:string`](#Selection_with_string) 
    * [`:string` Formatting](#string_Formatting) 
  * [Numeric Value Selection and Formatting](#Numeric_Value_Selection_and_Formatting) 
    * [The `:number` function](#The_number_function) 
    * [`:number` Operands](#number_Operands) 
    * [`:number` Options](#number_Options) 
    * [`:number` Resolved Value](#number_Resolved_Value) 
    * [Selection with `:number`](#Selection_with_number) 
    * [The `:integer` function](#The_integer_function) 
    * [`:integer` Operands](#integer_Operands) 
    * [`:integer` Options](#integer_Options) 
    * [`:integer` Resolved Value](#integer_Resolved_Value) 
    * [Selection with `:integer`](#Selection_with_integer) 
    * [The `:offset` function](#The_offset_function) 
    * [`:offset` Operands](#offset_Operands) 
    * [`:offset` Options](#offset_Options) 
    * [`:offset` Resolved Value](#offset_Resolved_Value) 
    * [Selection with `:offset`](#Selection_with_offset) 
    * [The `:currency` function](#The_currency_function) 
    * [`:currency` Operands](#currency_Operands) 
    * [`:currency` Options](#currency_Options) 
    * [`:currency` Resolved Value](#currency_Resolved_Value) 
    * [The `:percent` function](#The_percent_function) 
    * [`:percent` Operands](#percent_Operands) 
    * [`:percent` Options](#percent_Options) 
    * [`:percent` Resolved Value](#percent_Resolved_Value) 
    * [Selection with `:percent`](#Selection_with_percent) 
    * [The `:unit` function](#The_unit_function) 
    * [`:unit` Operands](#unit_Operands) 
    * [`:unit` Options](#unit_Options) 
    * [`:unit` Resolved Value](#unit_Resolved_Value) 
    * [Unit Conversion](#Unit_Conversion) 
    * [Numeric Operands](#Numeric_Operands) 
    * [Digit Size Options](#Digit_Size_Options) 
    * [Number Selection](#Number_Selection) 
    * [Default Value of `select` Option](#Default_Value_of_select_Option) 
    * [Rule Selection](#Rule_Selection) 
    * [Exact Literal Match Serialization](#Exact_Literal_Match_Serialization) 
  * [Date and Time Value Formatting](#Date_and_Time_Value_Formatting) 
    * [The `:datetime` function](#The_datetime_function) 
    * [`:datetime` Operands](#datetime_Operands) 
    * [`:datetime` Options](#datetime_Options) 
    * [`:datetime` Resolved Value](#datetime_Resolved_Value) 
    * [The `:date` function](#The_date_function) 
    * [`:date` Operands](#date_Operands) 
    * [`:date` Options](#date_Options) 
    * [`:date` Resolved Value](#date_Resolved_Value) 
    * [The `:time` function](#The_time_function) 
    * [`:time` Operands](#time_Operands) 
    * [`:time` Options](#time_Options) 
    * [`:time` Resolved Value](#time_Resolved_Value) 
    * [Date and Time Operands](#Date_and_Time_Operands) 
    * [Date and Time Override Options](#Date_and_Time_Override_Options) 
* [Unicode Namespace](#Unicode_Namespace) 
  * [Unicode Namespace Options](#Unicode_Namespace_Options) 
    * [`u:id`](#uid) 
    * [`u:dir`](#udir) 
* [Interchange Data Model](#Interchange_Data_Model) 
  * [Message Model](#Message_Model) 
  * [Pattern Model](#Pattern_Model) 
  * [Expression Model](#Expression_Model) 
  * [Markup Model](#Markup_Model) 
  * [Attribute Model](#Attribute_Model) 
  * [Model Extensions](#Model_Extensions) 
  * [`message.json`](#messagejson) 
* [Appendices](#Appendices) 
  * [Security Considerations](#Security_Considerations) 
  * [Non-normative Examples](#Nonnormative_Examples) 
    * [Pattern Selection Examples](#Pattern_Selection_Examples) 
    * [Selection Example 1](#Selection_Example_1) 
    * [Selection Example 2](#Selection_Example_2) 
    * [Selection Example 3](#Selection_Example_3) 
  * [Acknowledgments](#Acknowledgments) 

