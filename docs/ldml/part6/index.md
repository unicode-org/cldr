<div class="uts-header">
  <div class="uts-header-top">
    <span class="uts-badge">Unicode® Technical Standard #35</span>
    <span class="uts-version-tag">Version 49 (Draft) • Revision 79</span>
  </div>
  <div class="uts-title">Unicode Locale Data Markup Language (LDML)</div>
  <div class="uts-part">Part 6: Supplemental Metadata</div>
  <div class="uts-meta-grid">
    <div><strong>Editor:</strong> Steven R. Loomis</div>
    <div><strong>Namespace:</strong> <code>https://www.unicode.org/cldr/</code></div>
    <div><strong>Corrigenda:</strong> <a href="https://cldr.unicode.org/index/corrigenda" target="_blank">cldr.unicode.org</a></div>
    <div><strong>Latest Version:</strong> <a href="https://www.unicode.org/reports/tr35/" target="_blank">tr35</a></div>
  </div>
</div>

# Part 6: Supplemental Metadata

For the full header, summary, and status, see [Part 1: Core](tr35.md).

### <a name="_Summary_" id="_Summary_" href="#_Summary_">_Summary_</a>

* This document describes parts of an XML format (_vocabulary_) for the exchange of structured locale data. This format is used in the [Unicode Common Locale Data Repository](https://www.unicode.org/cldr/).


This is a partial document, describing only those parts of the LDML that are relevant for supplemental data. For the other parts of the LDML see the [main LDML document](tr35.md) and the links above.

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

## <a name="Contents" id="Contents" href="#Contents">Contents of Part 6, Supplemental</a>

* [Introduction Supplemental Data](#Supplemental_Data) 
* [Territory Data](#Territory_Data) 
  * [Supplemental Territory Containment](#Supplemental_Territory_Containment) 
  * [Subdivision Containment](#Subdivision_Containment) 
  * [Supplemental Territory Information](#Supplemental_Territory_Information) 
  * [Territory-Based Preferences](#Territory_Based_Preferences) 
    * [Preferred Units for Specific Usages](#Preferred_Units_For_Usage) 
  * [`<rgScope>`: Scope of the “rg” Locale Key](#rgScope) 
* [Supplemental Language Data](#Supplemental_Language_Data) 
* [Supplemental Language Grouping](#Supplemental_Language_Grouping) 
* [Supplemental Code Mapping](#Supplemental_Code_Mapping) 
* [~~Telephone Code Data~~ (Deprecated)](#Telephone_Code_Data) 
* [~~Postal Code Validation (Deprecated)~~](#Postal_Code_Validation) 
* [Supplemental Character Fallback Data](#Supplemental_Character_Fallback_Data) 
* [Coverage Levels](#Coverage_Levels) 
  * [Definitions](#Coverage_Level_Definitions) 
  * [Data Requirements](#Coverage_Level_Data_Requirements) 
  * [Default Values](#Coverage_Level_Default_Values) 
* [Supplemental Metadata](#Appendix_Supplemental_Metadata) 
  * [Supplemental Alias Information](#Supplemental_Alias_Information) 
    * [Table: Alias Attribute Values](#Alias_Attribute_Values) 
  * [~~Supplemental Deprecated Information (Deprecated)~~](#Supplemental_Deprecated_Information) 
  * [Default Content](#Default_Content) 
* [Locale Metadata Elements](#Metadata_Elements) 
* [Version Information](#Version_Information) 
* [Parent Locales](#Parent_Locales) 
* [Unit Conversion](#Unit_Conversion) 
  * [Unit Parsing Data](#Unit_Parsing_Data) 
  * [Unit Prefixes](#Unit_Prefixes) 
  * [Constants](#Constants) 
  * [Conversion Data](#Conversion_Data) 
    * [Derived Unit System](#Derived_Unit_System) 
    * [Conversion Mechanisms](#Conversion_Mechanisms) 
    * [Exceptional Cases](#Exceptional_Cases) 
    * [Identities](#Identities) 
    * [Aliases](#Aliases) 
    * [“Duplicate” Units](#Duplicate_Units) 
    * [Discarding Offsets](#Discarding_Offsets) 
    * [Unresolved Units](#Unresolved_Units) 
* [Quantities and Base Units](#Quantities_and_Base_Units) 
  * [UnitType vs Quantity](#UnitType_vs_Quantity) 
  * [Unit Identifier Normalization](#Unit_Identifier_Normalization) 
* [Mixed Units](#Mixed_Units) 
* [Testing](#Testing) 
* [Unit Preferences](#Unit_Preferences) 
  * [Unit Preferences Overrides](#Unit_Preferences_Overrides) 
    * [Compute override units](#Compute_override_units) 
    * [Compute  regions](#Compute_regions) 
    * [Compute the category](#Compute_the_category) 
  * [Unit Preferences Data](#Unit_Preferences_Data) 
    * [Examples:](#Examples) 
    * [Compute the preferred output unit](#Compute_the_preferred_output_unit) 
    * [Search the ranked units](#Search_the_ranked_units) 
  * [Constraints](#Constraints) 
    * [Examples](#Examples) 
* [Unit APIs](#Unit_APIs) 

