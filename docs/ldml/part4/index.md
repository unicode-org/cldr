<div class="uts-header">
  <div class="uts-header-top">
    <span class="uts-badge">Unicode® Technical Standard #35</span>
    <span class="uts-version-tag">Version 49 (Draft) • Revision 79</span>
  </div>
  <div class="uts-title">Unicode Locale Data Markup Language (LDML)</div>
  <div class="uts-part">Part 4: Dates & Times</div>
  <div class="uts-meta-grid">
    <div><strong>Editor:</strong> Mark Davis, Peter Edberg (Apple)</div>
    <div><strong>Namespace:</strong> <code>https://www.unicode.org/cldr/</code></div>
    <div><strong>Corrigenda:</strong> <a href="https://cldr.unicode.org/index/corrigenda" target="_blank">cldr.unicode.org</a></div>
    <div><strong>Latest Version:</strong> <a href="https://www.unicode.org/reports/tr35/" target="_blank">tr35</a></div>
  </div>
</div>

# Part 4: Dates & Times

### <a name="_Summary_" id="_Summary_" href="#_Summary_">_Summary_</a>

* This document describes parts of an XML format (_vocabulary_) for the exchange of structured locale data. This format is used in the [Unicode Common Locale Data Repository](https://www.unicode.org/cldr/).


* This is a partial document, describing only those parts of the LDML that are relevant for date, time, and time zone formatting. For the other parts of the LDML see the [main LDML document](tr35.md) and the links above.


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

## <a name="Contents" id="Contents" href="#Contents">Contents of Part 4, Dates</a>

* [Overview: Dates Element, Supplemental Date and Calendar Information](#Overview_Dates_Element_Supplemental) 
* [Calendar Elements](#Calendar_Elements) 
  * [Elements months, days, quarters, eras](#months_days_quarters_eras) 
  * [Elements monthPatterns, cyclicNameSets](#monthPatterns_cyclicNameSets) 
  * [Element dayPeriods](#dayPeriods) 
  * [Element dateFormats](#dateFormats) 
  * [Element timeFormats](#timeFormats) 
  * [Element dateTimeFormats](#dateTimeFormats) 
    * [Element dateTimeFormat](#dateTimeFormat) 
      * [Table: Date-Time Combination Examples](#Date_Time_Combination_Examples) 
    * [Elements availableFormats, appendItems](#availableFormats_appendItems) 
      * [Table: Mapping Requested Time Skeletons To Patterns](#Mapping_Requested_Time_Skeletons_To_Patterns) 
    * [Matching Skeletons](#Matching_Skeletons) 
    * [Missing Skeleton Fields](#Missing_Skeleton_Fields) 
    * [Element intervalFormats](#intervalFormats) 
* [Calendar Fields](#Calendar_Fields) 
* [Supplemental Calendar Data](#Supplemental_Calendar_Data) 
  * [Calendar Data](#Calendar_Data) 
  * [Calendar Preference Data](#Calendar_Preference_Data) 
  * [Week Data](#Week_Data) 
    * [Table: Week Designation Types](#Week_Designation_Types) 
    * [First Day Overrides](#First_Day_Overrides) 
  * [Time Data](#Time_Data) 
  * [Day Period Rule Sets](#Day_Period_Rule_Sets) 
    * [Day Period Rules](#Day_Period_Rules) 
    * [Fixed periods](#Fixed_periods) 
    * [Variable periods](#Variable_periods) 
    * [Parsing Day Periods](#Parsing_Day_Periods) 
* [Time Zone Names](#Time_Zone_Names) 
  * [Table: timeZoneNames Elements Used for Fallback](#timeZoneNames_Elements_Used_for_Fallback) 
  * [Metazone Names](#Metazone_Names) 
* [Supplemental Time Zone Data](#Supplemental_Time_Zone_Data) 
  * [Metazones](#Metazones) 
  * [Windows Zones](#Windows_Zones) 
  * [Primary Zones](#Primary_Zones) 
* [Using Time Zone Names](#Using_Time_Zone_Names) 
  * [Time Zone Format Terminology](#Time_Zone_Format_Terminology) 
  * [Goals](#Time_Zone_Goals) 
  * [Parsing](#Time_Zone_Parsing) 
* [Date Format Patterns](#Date_Format_Patterns) 
  * [Table: Date Format Pattern Examples](#Date_Format_Pattern_Examples) 
  * [Table: Date Field Symbol Table](#Date_Field_Symbol_Table) 
  * [Localized Pattern Characters (deprecated)](#Localized_Pattern_Characters) 
  * [AM / PM](#Date_Patterns_AM_PM) 
  * [Eras](#Date_Patterns_Eras) 
  * [Week of Year](#Date_Patterns_Week_Of_Year) 
  * [Week Elements](#Date_Patterns_Week_Elements) 
* [Parsing Dates and Times](#Parsing_Dates_Times) 
* [Semantic Skeletons](#Semantic_Skeletons) 
  * [Parts of a Semantic Skeleton](#Parts_of_a_Semantic_Skeleton) 
    * [Semantic Field Sets](#Semantic_Field_Sets) 
    * [Date Field Sets](#Semantic_Date_Field_Sets) 
    * [Calendar Period Field Sets](#Semantic_Calendar_Period_Field_Sets) 
    * [Time Field Sets](#Semantic_Time_Field_Sets) 
    * [Time Zone Field Sets](#Semantic_Time_Zone_Field_Sets) 
    * [Composite Field Sets](#Semantic_Composite_Field_Sets) 
    * [Semantic Skeleton Options](#Semantic_Skeleton_Options) 
    * [Length](#Semantic_Skeleton_Length) 
    * [Alignment](#Semantic_Skeleton_Alignment) 
    * [Year Style](#Semantic_Skeleton_Year_Style) 
    * [Hour Cycle](#Semantic_Skeleton_Hour_Cycle) 
    * [Time Precision](#Semantic_Skeleton_Time_Precision) 
    * [Time Zone Style](#Semantic_Skeleton_Time_Zone_Style) 
  * [Generating Patterns for Semantic Skeletons](#Generating_Patterns_for_Semantic_Skeletons) 
    * [Mapping to Standard Skeletons](#Mapping_to_Standard_Skeletons) 
    * [Time Precision Skeleton Variations](#Semantic_Time_Precision_Skeleton_Variations) 
    * [Year Style Skeleton Variations](#Semantic_Year_Style_Skeleton_Variations) 
  * [Semantic Skeleton Conformance](#Semantic_Skeleton_Conformance) 

