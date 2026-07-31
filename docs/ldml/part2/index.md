<div class="uts-header">
  <div class="uts-header-top">
    <span class="uts-badge">Unicode® Technical Standard #35</span>
    <span class="uts-version-tag">Version 49 (Draft) • Revision 79</span>
  </div>
  <div class="uts-title">Unicode Locale Data Markup Language (LDML)</div>
  <div class="uts-part">Part 2: General</div>
  <div class="uts-meta-grid">
    <div><strong>Editor:</strong> Yoshito Umaoka (IBM)</div>
    <div><strong>Namespace:</strong> <code>https://www.unicode.org/cldr/</code></div>
    <div><strong>Corrigenda:</strong> <a href="https://cldr.unicode.org/index/corrigenda" target="_blank">cldr.unicode.org</a></div>
    <div><strong>Latest Version:</strong> <a href="https://www.unicode.org/reports/tr35/" target="_blank">tr35</a></div>
  </div>
</div>

# Part 2: General

### <a name="_Summary_" id="_Summary_" href="#_Summary_">_Summary_</a>

* This document describes parts of an XML format (_vocabulary_) for the exchange of structured locale data. This format is used in the [Unicode Common Locale Data Repository](https://www.unicode.org/cldr/).


This is a partial document, describing general parts of the LDML: display names & transforms, etc. For the other parts of the LDML see the [main LDML document](tr35.md) and the links above.

### <a name="_Status_" id="_Status_" href="#_Status_">_Status_</a>

<div id='currentStatus'></div>

 _**A Unicode Technical Standard (UTS)** is an independent specification. Conformance to the Unicode Standard does not imply conformance to any UTS._

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

## <a name="Contents" id="Contents" href="#Contents">Contents of Part 2, General</a>

* [Display Name Elements](#Display_Name_Elements) 
  * [Locale Display Name Algorithm](#locale_display_name_algorithm) 
  * [Locale Display Name Fields](#locale_display_name_fields) 
  * [Type Values](#Type_Values) 
* [Layout Elements](#Layout_Elements) 
* [Character Elements](#Character_Elements) 
  * [Exemplars](#Exemplars) 
    * [Exemplar Syntax](#ExemplarSyntax) 
    * [Restrictions](#Restrictions) 
  * [~~Mapping~~](#Character_Mapping) 
  * [~~Index Labels~~](#IndexLabels) 
  * [Ellipsis](#Ellipsis) 
  * [Nested Bracket Replacement](#Character_Nested_Bracket_Replacement) 
  * [More Information](#Character_More_Info) 
  * [Parse Lenient](#Character_Parse_Lenient) 
* [Delimiter Elements](#Delimiter_Elements) 
  * [Tailoring Linebreak Using Delimiters](#Tailor_Linebreak_With_Delimiters) 
* [Measurement System Data](#Measurement_System_Data) 
  * [Measurement Elements (deprecated)](#Measurement_Elements) 
* [Unit Elements](#Unit_Elements) 
  * [Unit Preference and Conversion Data](#Unit_Preference_and_Conversion) 
  * [Unit Identifiers](#Unit_Identifiers) 
    * [Nomenclature](#Nomenclature) 
    * [Unit Syntax](#Unit_Syntax) 
  * [Unit Identifier Uniqueness](#Unit_Identifier_Uniqueness) 
  * [Example Units](#Example_Units) 
  * [Compound Units](#Compound_Units) 
    * [Precomposed Compound Units](#Precomposed_Compound_Units) 
  * [Unit Sequences (Mixed Units)](#Unit_Sequences) 
  * [durationUnit](#durationUnit) 
  * [coordinateUnit](#coordinateUnit) 
  * [Territory-Based Unit Preferences](#Territory_Based_Unit_Preferences) 
  * [Private-Use Units](#Private_Use_Units) 
* [POSIX Elements](#POSIX_Elements) 
* [Reference Element](#Reference_Elements) 
* [Segmentations](#Segmentations) 
  * [Segmentation Inheritance](#Segmentation_Inheritance) 
  * [Segmentation Suppressions](#Segmentation_Exceptions) 
* [Transforms](#Transforms) 
  * [Inheritance](#Inheritance) 
    * [Pivots](#Pivots) 
  * [Variants](#Variants) 
  * [Transform Rules Syntax](#Transform_Rules_Syntax) 
    * [Dual Rules](#Dual_Rules) 
    * [Context](#Context) 
    * [Revisiting](#Revisiting) 
    * [Example](#Example) 
    * [Rule Syntax](#Rule_Syntax) 
    * [Transform Rules](#Transform_Rules) 
    * [Variable Definition Rules](#Variable_Definition_Rules) 
    * [Filter Rules](#Filter_Rules) 
    * [Conversion Rules](#Conversion_Rules) 
    * [Intermixing Transform Rules and Conversion Rules](#Intermixing_Transform_Rules_and_Conversion_Rules) 
    * [Inverse Summary](#Inverse_Summary) 
  * [Transform Syntax Characters](#Transform_Syntax_Characters) 
* [List Patterns](#ListPatterns) 
  * [Gender of Lists](#List_Gender) 
* [ContextTransform Elements](#Context_Transform_Elements) 
  * [Table: Element contextTransformUsage type attribute values](#contextTransformUsage_type_attribute_values) 
* [Choice Patterns](#Choice_Patterns) 
* [Annotations and Labels](#Annotations) 
  * [Usage Model](#Usage_Model) 
  * [cp attribute](#cp_attribute) 
  * [Synthesizing Sequence Names](#SynthesizingNames) 
    * [Table: Synthesized Emoji Sequence Names](#Table_Synthesized_Emoji_Sequence_Names) 
  * [Annotations Character Labels](#Character_Labels) 
    * [Table: characterLabelPattern](#Table_characterLabelPattern) 
    * [Table: characterLabel](#Table_characterLabel) 
  * [Typographic Names](#Typographic_Names) 
* [Grammatical Features](#Grammatical_Features) 
* [Features](#Features) 
  * [Gender](#Gender) 
    * [Example](#Example) 
    * [Table: Values](#Table_Values) 
  * [Case](#Case) 
    * [Table: Case](#Table_Case) 
    * [Example](#Example) 
      * [Table: Values](#Table_Values) 
  * [Definiteness](#Definiteness) 
    * [Table: Values](#Table_Values) 
* [Grammatical Derivations](#Grammatical_Derivations) 
  * [Deriving the Gender of Compound Units](#gender_compound_units) 
  * [Deriving the Plural Category of Unit Components](#plural_compound_units) 
  * [Deriving the Case of Unit Components](#case_compound_units) 

