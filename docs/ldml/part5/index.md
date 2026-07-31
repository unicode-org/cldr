<div class="uts-header">
  <div class="uts-header-top">
    <span class="uts-badge">Unicode® Technical Standard #35</span>
    <span class="uts-version-tag">Version 49 (Draft) • Revision 79</span>
  </div>
  <div class="uts-title">Unicode Locale Data Markup Language (LDML)</div>
  <div class="uts-part">Part 5: Collation</div>
  <div class="uts-meta-grid">
    <div><strong>Editor:</strong> Markus Scherer (Google)</div>
    <div><strong>Namespace:</strong> <code>https://www.unicode.org/cldr/</code></div>
    <div><strong>Corrigenda:</strong> <a href="https://cldr.unicode.org/index/corrigenda" target="_blank">cldr.unicode.org</a></div>
    <div><strong>Latest Version:</strong> <a href="https://www.unicode.org/reports/tr35/" target="_blank">tr35</a></div>
  </div>
</div>

# Part 5: Collation

### <a name="_Summary_" id="_Summary_" href="#_Summary_">_Summary_</a>

* This document describes parts of an XML format (_vocabulary_) for the exchange of structured locale data. This format is used in the [Unicode Common Locale Data Repository](https://www.unicode.org/cldr/).


* This is a partial document, describing only those parts of the LDML that are relevant for collation (sorting, searching & grouping). For the other parts of the LDML see the [main LDML document](tr35.md) and the links above.


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

## <a name="Contents" id="Contents" href="#Contents">Contents of Part 5, Collation</a>

* [CLDR Collation](#CLDR_Collation) 
  * [CLDR Collation Algorithm](#CLDR_Collation_Algorithm) 
    * [U+FFFE](#Algorithm_FFFE) 
    * [Context-Sensitive Mappings](#Context_Sensitive_Mappings) 
    * [Case Handling](#Algorithm_Case) 
    * [Reordering Groups](#Algorithm_Reordering_Groups) 
    * [Combining Rules](#Combining_Rules) 
* [Root Collation](#Root_Collation) 
  * [Grouping classes of characters](#grouping_classes_of_characters) 
  * [Non-variable symbols](#non_variable_symbols) 
  * [Additional contractions for Tibetan](#tibetan_contractions) 
  * [Tailored noncharacter weights](#tailored_noncharacter_weights) 
  * [Root Collation Data Files](#Root_Data_Files) 
  * [Root Collation Data File Formats](#Root_Data_File_Formats) 
    * [allkeys_CLDR.txt](#File_Format_allkeys_CLDR_txt) 
    * [FractionalUCA.txt](#File_Format_FractionalUCA_txt) 
    * [UCA_Rules.txt](#File_Format_UCA_Rules_txt) 
* [Collation Tailorings](#Collation_Tailorings) 
  * [Collation Types](#Collation_Types) 
    * [Collation Type Fallback](#Collation_Type_Fallback) 
  * [Version](#Collation_Version) 
  * [Collation Element](#Collation_Element) 
  * [Setting Options](#Setting_Options) 
    * [Table: Collation Settings](#Collation_Settings) 
    * [Common settings combinations](#Common_Settings) 
    * [Notes on the normalization setting](#Normalization_Setting) 
    * [Notes on variable top settings](#Variable_Top_Settings) 
  * [Collation Rule Syntax](#Rules) 
  * [Orderings](#Orderings) 
    * [Table: Specifying Collation Ordering](#Specifying_Collation_Ordering) 
    * [Table: Abbreviating Ordering Specifications](#Abbreviating_Ordering_Specifications) 
  * [Contractions](#Contractions) 
    * [Table: Specifying Contractions](#Specifying_Contractions) 
  * [Expansions](#Expansions) 
  * [Context Before](#Context_Before) 
    * [Table: Specifying Previous Context](#Specifying_Previous_Context) 
  * [Placing Characters Before Others](#Placing_Characters_Before_Others) 
  * [Logical Reset Positions](#Logical_Reset_Positions) 
    * [Table: Specifying Logical Positions](#Specifying_Logical_Positions) 
  * [Special-Purpose Commands](#Special_Purpose_Commands) 
    * [Table: Special-Purpose Elements](#Special_Purpose_Elements) 
  * [Collation Reordering](#Script_Reordering) 
    * [Interpretation of a reordering list](#Interpretation_reordering) 
    * [Reordering Groups for allkeys.txt](#Reordering_Groups_allkeys) 
  * [Case Parameters](#Case_Parameters) 
    * [Untailored Characters](#Case_Untailored) 
    * [Compute Modified Collation Elements](#Case_Weights) 
    * [Tailored Strings](#Case_Tailored) 
  * [Visibility](#Visibility) 
  * [Collation Indexes](#Collation_Indexes) 
    * [Index Characters](#Index_Characters) 
    * [CJK Index Markers](#CJK_Index_Markers) 

