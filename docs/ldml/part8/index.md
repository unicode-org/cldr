<div class="uts-header">
  <div class="uts-header-top">
    <span class="uts-badge">Unicode® Technical Standard #35</span>
    <span class="uts-version-tag">Version 49 (Draft) • Revision 79</span>
  </div>
  <div class="uts-title">Unicode Locale Data Markup Language (LDML)</div>
  <div class="uts-part">Part 8: Person Names</div>
  <div class="uts-meta-grid">
    <div><strong>Editor:</strong> Mark Davis, Peter Edberg, Rich Gillam, Alex Kolisnychenko, Mike McKenna</div>
    <div><strong>Namespace:</strong> <code>https://www.unicode.org/cldr/</code></div>
    <div><strong>Corrigenda:</strong> <a href="https://cldr.unicode.org/index/corrigenda" target="_blank">cldr.unicode.org</a></div>
    <div><strong>Latest Version:</strong> <a href="https://www.unicode.org/reports/tr35/" target="_blank">tr35</a></div>
  </div>
</div>

# Part 8: Person Names

### <a name="_Summary_" id="_Summary_" href="#_Summary_">_Summary_</a>

* This document describes parts of an XML format (_vocabulary_) for the exchange of structured locale data. This format is used in the [Unicode Common Locale Data Repository](https://www.unicode.org/cldr/).


* This is a partial document, describing only those parts of the LDML that are relevant for person names (name structure, formats, sorting). For the other parts of the LDML see the [main LDML document](tr35.md) and the links above.


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

## <a name="Contents" id="Contents" href="#Contents">Contents of Part 8, Person Names</a>

* [CLDR Person Names](#CLDR_Person_Names) 
  * [Introduction](#Introduction) 
    * [Not in scope](#Not_in_scope) 
  * [API Implementation](#API_Implementation) 
  * [Person Name Formatting Overview](#Person_Name_Formatting_Overview) 
  * [Example Usage](#Example_Usage) 
* [XML Structure](#XML_Structure) 
  * [personNames Element](#personNames_Element) 
  * [personName Element](#personName_Element) 
  * [nameOrderLocales Element](#nameOrderLocales_Element) 
  * [parameterDefault Element](#parameterDefault_Element) 
  * [foreignSpaceReplacement Element](#foreignSpaceReplacement_Element) 
  * [nativeSpaceReplacement Element](#nativeSpaceReplacement_Element) 
  * [initialPattern Element](#initialPattern_Element) 
    * [Syntax](#Syntax) 
* [Person Name Object](#Person_Name_Object) 
* [Person Name Attributes](#Person_Name_Attributes) 
  * [order](#order) 
  * [length](#length) 
  * [usage](#usage) 
  * [formality](#formality) 
* [namePattern Syntax](#namePattern_Syntax) 
  * [Fields](#Fields) 
  * [Modifiers](#Modifiers) 
    * [Grammatical Modifiers for Names](#Grammatical_Modifiers_for_Names) 
    * [Future Modifiers](#Future_Modifiers) 
* [Formatting Process](#Formatting_Process) 
  * [Derive the name locale](#Derive_the_name_locale) 
  * [Derive the formatting locale](#Derive_the_formatting_locale) 
    * [Switch the formatting locale if necessary](#Switch_the_formatting_locale_if_necessary) 
  * [Derive the name order](#Derive_the_name_order) 
  * [Choose a personName element](#Choose_a_personName_element) 
  * [Choose a namePattern](#Choose_a_namePattern) 
  * [Access PersonName object](#Access_PersonName_object) 
    * [Handle missing surname](#Handle_missing_surname) 
    * [Handle core and prefix](#Handle_core_and_prefix) 
    * [Derive initials](#Derive_initials) 
  * [Process a namePattern](#Process_a_namePattern) 
    * [Handling foreign names](#Handling_foreign_names) 
    * [Setting the spaceReplacement](#Setting_the_spaceReplacement) 
    * [Examples of space replacement](#Examples_of_space_replacement) 
  * [Formatting examples](#Formatting_examples) 
* [Sample Name](#Sample_Name) 
  * [Syntax](#Syntax) 
  * [Expected values](#Expected_values) 
* [Person Name Validation](#Person_Name_Validation) 
  * [Letters](#Letters) 
  * [Non-Letters](#NonLetters) 
  * [Normalization](#Normalization) 
  * [Additional possible constraints](#Additional_possible_constraints) 
* [PersonName Data Interface Examples](#PersonName_Data_Interface_Examples) 
  * [Example 1](#Example_1) 
  * [Example 2](#Example_2) 

