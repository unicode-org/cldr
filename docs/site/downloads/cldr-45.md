# CLDR 45 Release Note

| No. |    Date    | Rel. Note |  Data  |  Charts  | Spec |   Delta  | GitHub Tag | Delta DTD | CLDR JSON |
|:---:|:----------:|:---------:|:------:|:--------:|:------------:|:---:|:----------:|:---------:|:---------:|
|  45 | 2024-04-17 |    [v45](http://cldr.unicode.org/index/downloads/cldr-45)    | [CLDR45](http://unicode.org/Public/cldr/45/) | [Charts45](https://unicode.org/cldr/charts/45/) |    [LDML45](https://www.unicode.org/reports/tr35/tr35-72/tr35.html)    | [Δ45](https://unicode-org.atlassian.net/issues/?jql=project+%3D+CLDR+AND+status+%3D+Done+AND+resolution+%3D+Fixed+AND+fixVersion+%3D+%2245%22+ORDER+BY+component+ASC%2C+priority+DESC%2C+created+ASC) | [release-45](https://github.com/unicode-org/cldr/releases/tag/release-45) |   [ΔDtd45](https://www.unicode.org/cldr/charts/45/supplemental/dtd_deltas.html)  |   [45.0.0](https://github.com/unicode-org/cldr-json/releases/tag/45.0.0)  |

## Overview

Unicode CLDR provides key building blocks for software supporting the world's languages. CLDR data is used by all [major software systems](https://www.google.com/url?q=http://cldr.unicode.org/index%23TOC-Who-uses-CLDR-&sa=D&source=editors&ust=1713652058779208&usg=AOvVaw3r8kLAP0U8srdRrX-YUWlu) (including all mobile phones) for their software internationalization and localization, adapting software to the conventions of different languages.

CLDR 45 is a closed release with no submission period, focusing on just a few areas:

### Message Format 2.0 (LDML [Part 9](https://www.unicode.org/reports/tr35/tr35-72/tr35-messageFormat.html#Contents))

Software needs to construct messages that incorporate various pieces of information. The complexities of the world's languages make this challenging. The goal for MessageFormat 2.0  is to allow developers and translators to create natural-sounding, grammatically-correct, user interfaces that can appear in any language and support the needs of diverse cultures.

The new MessageFormat defines the data model, syntax, processing, and conformance requirements for the next generation of dynamic messages. It is intended for adoption by programming languages, software libraries, and software localization tooling. It enables the integration of internationalization APIs (such as date or number formats), and grammatical matching (such as plurals or genders). It is extensible, allowing software developers to create formatting or message selection logic that add on to the core capabilities. Its data model provides a means of representing existing syntaxes, thus enabling gradual adoption by users of older formatting systems.

### Keyboard 3.0 (LDML [Part 7](https://www.unicode.org/reports/tr35/tr35-72/tr35-keyboards.html#Contents))

Keyboard support for digitally disadvantaged languages is often lacking or inconsistent between platforms. The updated LDML Keyboard 3.0 format specifies an interchange format for keyboard data. This will allow keyboard authors to create a single mapping file for their language, which implementations can use to provide that language’s keyboard mapping on their own platform. This format allows both physical and virtual (that is, on-screen or touch) keyboard layouts for a language to be defined in a single file.

### Tooling Changes

Many tooling changes are difficult to accommodate in a data-submission release, including performance work and UI improvements. The changes in v45 improve survey tool performance for linguists during data submission and vetting allowing for higher data quality. They are targeted at the v46 submission period, starting in May, 2024.

## Data Changes

### DTD Changes 

-   **Units**
    -   Addition of the `special` attribute to convertUnit, indicating a conversion that cannot be described using just `factor` and `offset`; it may be table-based, non-linear, or may have other special characteristics.
    -   Addition of `unitPrefix` conversions, to specify the power of 10 or 2 to be used for the prefix and the SI symbol (eg, G for giga-)
-   **Locales**
    -   Additional parentLocale components (plurals, grammaticalFeatures), indicating different inheritance fallback.
    -   A new parentLocale attribute, **localeRules**. The new value for that attribute, **nonlikelyScript**, broadens a list of locales to include all locales of the form `<lang>\_<script>` where the script is different than the likely script for the lang. It thus includes locales like 'ru\_Xxxx', where Xxxx is any script aside from Cyrl, and prevents fallback that would cause mixtures of scripts to appear in generated data.
	- This will require a migration for people using parentLocales (see Migration).
-   **Currencies**
    -   Timezone attributes (tz, to-tz) for currency transitions, so that the more exact time of a transition can be specified.
-   **Keyboards**
    -   There are many changes from the earlier version of keyboards. See [dtd\_deltas](https://unicode.org/cldr/charts/45/supplemental/dtd_deltas.html).

For a full listing, see [Delta DTDs](https://unicode.org/cldr/charts/45/supplemental/dtd_deltas.html).

### Supplemental Data Changes

-   BCP47
    -   Deprecating \-u-co-reformed
    -   Adding cu-xcg (new currency)
-   Timezones
    -   Deprecating timezones that are no longer in zone.tab (America/Nipigon, ...)
    -   New Windows timezone mappings
-   Currencies
    -   New currency XCG (replacing ANG)
    -   Changes in currencies for HR and SL.
-   Parent locales
    -   Removing collation/segmentation fallbacks that are now handled automatically (bs\_Cyrl, ...)
    -   Units
-   New prefixes and SI symbols
    -   Conversions from beaufort to and from meter-per-second, using the convertUnit special attribute
-   Metazone changes
    -   For deprecations and Greenland changes, see the supplemental data delta chart.

For a full listing, see [¤¤BCP47 Delta](https://www.google.com/url?q=https://unicode.org/cldr/charts/45/delta/bcp47.html&sa=D&source=editors&ust=1713652058784981&usg=AOvVaw02X97tSG45_oChn55f3Gjw) and [¤¤Supplemental Delta](https://www.google.com/url?q=https://unicode.org/cldr/charts/45/delta/supplemental-data.html&sa=D&source=editors&ust=1713652058785221&usg=AOvVaw2OKxgUzfQepGGrI7DGbMFt)

### [Locale Changes](https://unicode.org/cldr/charts/45/delta/index.html)

-   Most locale changes were driven by the remove of the "reformed" collation option, and names of deprecated timezones. There are a few spot fixes for certain locales.

For a full listing, see [Delta Data](https://unicode.org/cldr/charts/45/delta/index.html)

### File Changes
The following files were added:

-   /keyboards/3.0/bn.xml
-   /keyboards/test/bn-test.xml

### JSON Data Changes

-   The packages ending in "-modern" are deprecated and will be dropped in v46. Use the "-full" packages instead. ([CLDR-16465](https://unicode-org.atlassian.net/browse/CLDR-16465))
-   The SPDX license for all packages is now Unicode-3.0 ([CLDR-17400](https://unicode-org.atlassian.net/browse/CLDR-17400))
-   cldr-core/supplemental/units.json now has unitPrefixes ([CLDR-16939](https://unicode-org.atlassian.net/browse/CLDR-16939))
-   All package.json files now have  a "cldrVersion" and a "unicodeVersion" property.    
    The "\_cldrVersion" and "\_unicodeVersion" properties will be removed in a future CLDR release. ([CLDR-17285](https://unicode-org.atlassian.net/browse/CLDR-17285))

## [Specification Changes](https://www.unicode.org/reports/tr35/tr35-72/tr35.html)

-   Part 1: Core
    -   In Parent Locales, made substantial changes to the way that parentLocales work, including a new attribute for algorithmic handling of inheritance that avoids needing a long (and fragile) list of language-script codes to skip when falling back to root. That list was retained for migration, but will be withdrawn in the future.
    -   In Special Script Codes, added a description of special script codes, such as Jpan and Aran.
    -   In LocaleId Canonicalization:Preprocessing, restructured the steps for clarity, added more examples.
    -   In Likely Subtags, clarified that language subtags iw, in, and yi are treated specially in the data, to allow for applications that use them as canonical language subtags. Also removed the substitution for macroregions, and noted that some elements could be NO-OPs in customized data, but could be misleading.
    -   In EBNF, added more differences from W3C EBNF, and documented use of wfc: and vc: for wellformedness and validity constraints. Marked clauses with that format where appropriate, and grouped constraints after the relevant EBNF.
-   Part 3: Numbers
    -   For the supplemental  currency element, added attributes tz and to-tz to clarify the from and to dates.
-   Part 4: Dates
    -   In Date Format Patterns, reserved date pattern field lengths of greater than 16 as private use.
-   Part 6: Supplemental
    -   In Mixed Units, clarified many aspects of mixed units (such as foot-and-inch), including how to handle rounding and precision.
    -   In Unit Preferences Overrides, made substantial changes including handling of edge cases, such as where there is no quantity for a unit, or no preference data for a quantity; how to handle invalid subtags; negative unit amounts; the usage of each of the subtags that affect unit preferences, and others.
    -   In Conversion Data, added the `special` attribute for `convertUnit`, used for handling beaufort and possibly other conversions in the future..
    -   In Unit Prefixes, added the SI unit prefixes and the power of 10 (or 2, for binary prefixes) that they represent.
-   Part 7: Keyboards, advancing from Tech Preview to stable
    -   There are many changes to keyboards, see the revisions in Part 7. These include new sections for Definitions, Notation, and Normalization.
-   Part 9: Message Format 2.0, in Tech Preview
    -   This section of the specification is completely new: see Part 9.

## Growth

For this release there are no appreciable changes.

## Migration

1.  Changes to parentLocales require upgrading implementations that use that element. In particular, they need to support the new nonlikelyScript value, and use the appropriate explicit inheritance for each type of inheritance. The v44 list of locales that inherit directly from root is retained for this release, but will disappear in the future. So implementations should move as quickly as possible to support the new value. See Dtd Changes.
2.  The new SI prefixes are provided for metric units so that implementations can verify that they are providing the correct prefix values, including the new prefixes (see [New Prefixes in the SI: ronto, quecto, ronna, quetta](https://blog.ansi.org/anab/new-prefixes-si-ronto-quecto-ronna-quetta/)).
3.  In last release CLDR 44, Unicode updated its outbound license from the "[Unicode, Inc. License - Data Files and Software](https://opensource.org/license/unicode-inc-license-agreement-data-files-and-software)" to the "[Unicode License v3](https://opensource.org/license/unicode-license-v3)". All of the substantive terms of the license remain the same. The only changes made were non-substantive technical edits. The new license is OSI-approved and has been assigned the SPDX Identifier Unicode-3.0.
4.  Keyboard has a new DTD (keyboard3.dtd and the `<keyboard3>` element). There have been some incompatible changes between the tech preview and the stable version now in v44.
5.  Redundant values that inherit “laterally” may be removed in production data: Some data values inherit “sideways” from another element with the same parent, in the same locale. This has been long specified in [LDML: Lateral\_Inheritance](https://www.unicode.org/reports/tr35/#Lateral_Inheritance), but some clients may not implement to the specification, and get the wrong answers. For example, consider the following items in the en locale, some added in CLDR 44 to provide clients a way to explicitly select a particular variant across locales (instead of using the default):
```
<territory type="IO">British Indian Ocean Territory</territory> <!-- The locale default, matches one of the alt forms -->  
<territory type="IO" alt="biot">British Indian Ocean Territory</territory> <!-- explicit "biot" variant" -->  
<territory type="IO" alt="chagos">Chagos Archipelago</territory> <!-- explicit "chagos" variant" --> 
```
    Both `alt` forms inherit sideways from the non-`alt` form. Thus in this case, the "`biot`" variant is redundant and will be removed in production data. Clients that are trying to select the "`biot`" variant but find it missing should fall back to the non-`alt` form. Similar behavior occurs with plural forms for units, where some plural forms may match and thus fall back to the "`other`" form.

## [Known Issues](https://unicode-org.atlassian.net/issues/CLDR-17535?jql=project%20%3D%20cldr%20and%20labels%20%3D%20%22ReleaseKnownIssue%22%20and%20status%20!%3D%20done)

-   [CLDR-17095](https://unicode-org.atlassian.net/browse/CLDR-17095). The region-based firstDay value (see [weekData](https://www.unicode.org/reports/tr35/tr35-72/tr35-dates.html#Week_Data)) is currently used for several different purposes. In the future, some of these functions will be separated out:
    -   The day that should be shown as the first day of the week in a calendar view.
    -   The first day of the week (day 1) for weekday numbering.
    -   The first day of the week for week-of-year calendar calculations.
-   [CLDR-17535](https://unicode-org.atlassian.net/browse/CLDR-17535). Subdivision codes and translations were not updated due to tooling issues.
-   [CLDR-17505](https://unicode-org.atlassian.net/browse/CLDR-17505). Blocking items are obsolete: the spec needs to be corrected to use @ORDERED

## Acknowledgments

Many people have made significant contributions to CLDR and LDML; see the [Acknowledgments](https://cldr.unicode.org/index/acknowledgments) page for a full listing.

The Unicode [Terms of Use](https://unicode.org/copyright.html) apply to CLDR data; in particular, see [Exhibit 1](https://unicode.org/copyright.html#Exhibit1).

For web pages with different views of CLDR data, see [http://cldr.unicode.org/index/charts](https://cldr.unicode.org/index/charts).

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
