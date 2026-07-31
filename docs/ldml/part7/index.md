<div class="uts-header">
  <div class="uts-header-top">
    <span class="uts-badge">Unicode® Technical Standard #35</span>
    <span class="uts-version-tag">Version 49 (Draft) • Revision 79</span>
  </div>
  <div class="uts-title">Unicode Locale Data Markup Language (LDML)</div>
  <div class="uts-part">Part 7: Keyboards</div>
  <div class="uts-meta-grid">
    <div><strong>Editor:</strong> Steven R. Loomis</div>
    <div><strong>Namespace:</strong> <code>https://www.unicode.org/cldr/</code></div>
    <div><strong>Corrigenda:</strong> <a href="https://cldr.unicode.org/index/corrigenda" target="_blank">cldr.unicode.org</a></div>
    <div><strong>Latest Version:</strong> <a href="https://www.unicode.org/reports/tr35/" target="_blank">tr35</a></div>
  </div>
</div>

# Part 7: Keyboards

### <a name="_Summary_" id="_Summary_" href="#_Summary_">_Summary_</a>

* This document describes parts of an XML format (_vocabulary_) for the exchange of structured locale data. This format is used in the [Unicode Common Locale Data Repository](https://www.unicode.org/cldr/).


This is a partial document, describing keyboards. For the other parts of the LDML see the [main LDML document](tr35.md) and the links above.

### <a name="_Status_" id="_Status_" href="#_Status_">_Status_</a>

<div id='currentStatus'></div>

> _**A Unicode Technical Standard (UTS)** is an independent specification. Conformance to the Unicode Standard does not imply conformance to any UTS._

_Please submit corrigenda and other comments with the CLDR bug reporting form [[Bugs](https://cldr.unicode.org/index/bug-reports)].
Related information that is useful in understanding this document is found in the [References](tr35.md#References).
For the latest version of the Unicode Standard see [[Unicode](https://www.unicode.org/versions/latest/)].
For more information see [About Unicode Technical Reports](https://www.unicode.org/reports/about-reports.html) and the [Specifications FAQ](https://www.unicode.org/faq/specifications.html).
Unicode Technical Reports are governed by the Unicode [Terms of Use](https://www.unicode.org/copyright.html)._

See also [Compatibility Notice](#compatibility-notice).

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

## <a name="Contents" id="Contents" href="#Contents">Contents of Part 7, Keyboards</a>

* [Keyboards](#Keyboards) 
* [Goals and Non-goals](#Goals_and_Nongoals) 
  * [Compatibility Notice](#Compatibility_Notice) 
  * [Accessibility](#Accessibility) 
* [Definitions](#Definitions) 
* [Notation](#Notation) 
  * [Escaping](#Escaping) 
  * [UnicodeSet Escaping](#UnicodeSet_Escaping) 
  * [UTS18 Escaping](#UTS18_Escaping) 
* [File and Directory Structure](#File_and_Directory_Structure) 
  * [Extensibility](#Extensibility) 
* [Normalization](#Normalization) 
  * [Where Normalization Occurs](#Where_Normalization_Occurs) 
  * [Normalization and Transform Matching](#Normalization_and_Transform_Matching) 
  * [Normalization and Markers](#Normalization_and_Markers) 
    * [Rationale for 'gluing' markers](#Rationale_for_gluing_markers) 
    * [Data Model: `Marker`](#Data_Model_Marker) 
    * [Data Model: string](#Data_Model_string) 
    * [Data Model: `MarkerEntry`](#Data_Model_MarkerEntry) 
    * [Marker Algorithm Overview](#Marker_Algorithm_Overview) 
    * [Phase 1: Parsing/Removing Markers](#Phase_1_ParsingRemoving_Markers) 
    * [Phase 2: Plain Text Processing](#Phase_2_Plain_Text_Processing) 
    * [Phase 3: Adding Markers](#Phase_3_Adding_Markers) 
    * [Example Normalization with Markers](#Example_Normalization_with_Markers) 
  * [Normalization and Character Classes](#Normalization_and_Character_Classes) 
  * [Normalization and Reorder elements](#Normalization_and_Reorder_elements) 
  * [Normalization-safe Segments](#Normalizationsafe_Segments) 
  * [Normalization and Output](#Normalization_and_Output) 
  * [Disabling Normalization](#Disabling_Normalization) 
* [Element Hierarchy](#Element_Hierarchy) 
  * [Element: keyboard3](#Element_keyboard3) 
  * [Element: import](#Element_import) 
  * [Element: locales](#Element_locales) 
  * [Element: locale](#Element_locale) 
  * [Element: version](#Element_version) 
  * [Element: info](#Element_info) 
  * [Element: settings](#Element_settings) 
  * [Element: displays](#Element_displays) 
  * [Element: display](#Element_display) 
    * [Non-spacing marks on keytops](#Nonspacing_marks_on_keytops) 
  * [Element: displayOptions](#Element_displayOptions) 
  * [Element: keys](#Element_keys) 
  * [Element: key](#Element_key) 
    * [Implied Keys](#Implied_Keys) 
  * [Element: flicks](#Element_flicks) 
    * [Element: flick](#Element_flick) 
    * [Element: flickSegment](#Element_flickSegment) 
  * [Element: forms](#Element_forms) 
  * [Element: form](#Element_form) 
    * [Implied Form Values](#Implied_Form_Values) 
  * [Element: scanCodes](#Element_scanCodes) 
  * [Element: layers](#Element_layers) 
  * [Element: layer](#Element_layer) 
    * [Layer Modifier Sets](#Layer_Modifier_Sets) 
    * [Layer Modifier Components](#Layer_Modifier_Components) 
    * [Modifier Left- and Right- keys](#Modifier_Left_and_Right_keys) 
    * [Layer Modifier Matching](#Layer_Modifier_Matching) 
  * [Element: row](#Element_row) 
  * [Element: variables](#Element_variables) 
  * [Element: string](#Element_string) 
  * [Element: set](#Element_set) 
  * [Element: uset](#Element_uset) 
  * [Element: transforms](#Element_transforms) 
    * [Markers](#Markers) 
  * [Element: transformGroup](#Element_transformGroup) 
    * [Example: `transformGroup` with `transform` elements](#Example_transformGroup_with_transform_elements) 
    * [Example: `transformGroup` with `reorder` elements](#Example_transformGroup_with_reorder_elements) 
  * [Element: transform](#Element_transform) 
    * [Regex-like Syntax](#Regexlike_Syntax) 
    * [Additional Features](#Additional_Features) 
    * [Disallowed Regex Features](#Disallowed_Regex_Features) 
    * [Replacement syntax](#Replacement_syntax) 
    * [Transform Grammar](#Transform_Grammar) 
    * [Transform From Grammar](#Transform_From_Grammar) 
    * [Transform To Grammar](#Transform_To_Grammar) 
    * [ABNF](#ABNF) 
  * [Element: reorder](#Element_reorder) 
    * [Using `<import>` with `<reorder>` elements](#Using_with_elements) 
    * [Example Post-reorder transforms](#Example_Postreorder_transforms) 
    * [Reorder and Markers](#Reorder_and_Markers) 
  * [Backspace Transforms](#Backspace_Transforms) 
    * [Default Backspace Transform](#Default_Backspace_Transform) 
* [Invariants](#Invariants) 
* [Keyboard IDs](#Keyboard_IDs) 
  * [Principles for Keyboard IDs](#Principles_for_Keyboard_IDs) 
* [Platform Behaviors in Edge Cases](#Platform_Behaviors_in_Edge_Cases) 

