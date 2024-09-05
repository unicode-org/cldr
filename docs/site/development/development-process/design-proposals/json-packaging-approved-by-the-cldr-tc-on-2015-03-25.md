---
title: JSON Packaging (Approved by the CLDR TC on 2015-03-25)
---

# JSON Packaging (Approved by the CLDR TC on 2015-03-25)

***This page is out of date, see*** [***http://cldr.unicode.org/index/downloads#TOC-JSON-Data***](http://cldr.unicode.org/index/downloads#TOC-JSON-Data) ***for details of the JSON data.***

## Overview

CLDR's official locale data is, and probably always will be, published in XML format. However, JSON format data is quickly becoming a popular alternative, especially for JavaScript applications. Currently, when a CLDR release is published, we generate the corresponding JSON data ( according to http://cldr.unicode.org/index/cldr-spec/json ) and include additional zip files as part of the CLDR distribution. While the distribution of the data in this way is helpful to some, it would be even more useful for us to create a series of packages that are installable using bower ( see http://bower.io/ for details ). In this way, applications that desire to use the CLDR data in JSON format can simply specify the appropriate packages as a prerequisite, thus eliminating the need to copy and redistribute the data.

### Design Goals

- Data grouped by functionality - The design should allow people to install packages that contain the data they intend to use, while hopefully keeping the number of packages to a minimum.
- Tiered locale coverage - For the packages created, we can use the CLDR tiers as specified in \<location> to define an incrementally larger set of locales for each functional package. For each such package, a package containing additional locales can be defined as a proper superset of the corresponding smaller package. Because bower's install mechanism does not allow data from multiple packages to be put in the same directory, each "full" package will contain the complete set of locales that were defined in the corresponding "modern" package, so that applications don't have to have similar data for different locales residing in two different places. The set of locales contained in each tier would be consistent across all the functional packages.
- Default content locales - In order to keep the data size to a minimum, JSON data for default content locales will not be included in the installable packages. Since all default content locales can retrieve their data from the parent via simple inheritance ( i.e. removal of the rightmost portion of the language tag ), this should be relatively easy for most JavaScript applications to determine the appropriate location from which to retrieve the data. A new JSON file "defaultContent.json" shall be included in CLDR's as part of the cldr-core package, in the top level directory, that will contain the list of default content locales for the release.

### Functional Groups and Packages

The existing JSON data for CLDR shall be split into functional groupings as follows:

- **core** - This package will contain a core JSON file "availableLocales.json", in the top level directory, which will outline, by tier, the CLDR locales that are included in each tiered locale coverage level. In addition, since most applications will make use of the supplemental data, and since the supplemental data is relatively small, CLDR's supplemental data directory shall be included in the "core" package.
- **dates** - Package for basic date/time processing. For each available locale, contains "ca-generic.json", "ca-gregorian.json", "dateFields.json", and "timeZoneNames.json", the required set for Gregorian calendar support. Any packages for non-gregorian calendars should list the corresponding dates package as a prerequisite.
- **cal-{type}** - Package for non-gregorian locale support. For each available locale, contains "ca-{type}.json", where {type} is one of the supported CLDR calendar types: buddhist, chinese, coptic, dangi, ethiopic, hebrew, indian, islamic, japanese, persian, or roc. Note: all required json for calendar variants shall be contained in a single package. For example, the "cal-islamic" package would contain, for each supported locale, "ca-islamic.json", "ca-islamic-civil.json", "ca-islamic-rgsa.json", "ca-islamic-tbla.json", and "ca-islamic-umalqura.json".
- **localenames** - Package for display of locale names, language/territory names, etc. For each available locale, contains "localeDisplayNames.json", "languages.json", "scripts.json", "territories.json", "transformNames.json", and "variants.json".
- **misc** - Package for miscellaneous JSON that doesn't fall into any other functional category. For each included locale, contains characters.json, contextTransforms.json, delimiters.json, layout.json, listPatterns.json, and "posix.json".
- **numbers** - Package containing data necessary for proper formatting of numbers and currencies. For each available, contains currencies.json and numbers.json. The "dates" package should list the "numbers" package as a requisite, since numbers information is often required in order to format dates properly.
- **units** - Package containing data pertaining to units. For each included locale, contains units.json and measurementSystemNames.json.
- **segments** - Contains the segments data (from the unicode ULI project). Contains "segments/{locale}/suppressions.json", where {locale} is one of the locale identifiers that has segmentation information.

### Locale Coverage Tiers

For each functional group listed above (with the exception of the "core" package), data packages shall be created that define successively larger numbers of locales. The tiers shall be based on the coverage information used in the CLDR survey tool, as defined in http://unicode.org/repos/cldr/trunk/tools/java/org/unicode/cldr/util/data/Locales.txt . For any given language, all the valid CLDR sublocales for that language shall be in package. The following tiers shall be created.

- **modern** - (Depends on the "core" package only) - Contains those locales specified for "modern" coverage in Locales.txt in the "Cldr" organization. Includes those locales listed in the "tier1", "tier2", "tier 3", "tier 4", "generated", "ext", and "other" sections.
- **full** - (Depends on the corresponding "modern" package) - Contains all remaining locales published in CLDR's main directory, that aren't contained in one of the tiers above.

### Summary Table of Packages and Dependencies

| Package Name |  Depends On |
|---|---|
|  cldr-core |  &lt;nothing&gt; |
|  cldr-dates-modern |  cldr-numbers-modern |
|  cldr-dates-full |  cldr-dates-modern, cldr-numbers-full |
|  cldr-cal-buddhist-modern |  cldr-dates-modern |
|  cldr-cal-buddhist-full |  cldr-dates-full |
|  ...&lt;similar pattern for other cldr-cal-* packages&gt; |   |
|  cldr-localenames-modern |  cldr-core |
|  cldr-localenames-full |  cldr-localenames-modern |
|  cldr-misc-modern |  cldr-core |
|  cldr-misc-full |  cldr-misc-modern |
|  cldr-numbers-modern |  cldr-core |
|  cldr-numbers-full |  cldr-numbers-modern |
|  cldr-units-modern |  cldr-core |
|  cldr-units-full |  cldr-units-modern |
|  cldr-segments-modern |  cldr-core |

Locales by Tier as of CLDR 26 (for reference purposes only)

| **Tier** | **Locales** |
|---|---|
| modern | af, am, ar, az, bg, bn, bs. ca, cs, da, de, en, el, es, et, eu, fa, fil, fi, fr, gl, gu, he, hi, hu, hy, id, is, it, ja, ka, kk, km, kn, ko, ky, lo, lt, lv, mk, ml, mn, ms, mr, my, nb, ne, nl, pa, pl, pt, ro, ru, si, sr, sk, sl, sq, sv, sw, ta, te, th, tr, uk, ur, uz, vi, zh, zu  |
| full | All other locales. |


![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)