---
title: Unicode CLDR Project
---

# Unicode CLDR Project

## News

- **2025-04-24** [Survey Tool is open for submission](https://cldr.unicode.org/translation)
- **2025-03-13 [CLDR 47](downloads/cldr-47) released**


## What is CLDR?

The Unicode Common Locale Data Repository (CLDR) provides key building blocks for software to support the world's languages with the largest and most extensive standard repository of locale data available. This data is supplied by contributors for their languages via the [CLDR SurveyTool](#translations-and-other-language-data).

CLDR is used by a [wide spectrum of companies](#who-uses-cldr) for their software internationalization and localization, adapting software to the conventions of different languages for such common software tasks. It includes:
- **Locale-specific patterns for formatting and parsing:** dates, times, timezones, numbers and currency values, measurement units,…
- **Translations of names:** languages, scripts, countries and regions, currencies, eras, months, weekdays, day periods, time zones, cities, and time units, emoji characters and sequences (and search keywords),…
- **Language & script information:** characters used; plural cases; gender of lists; capitalization; rules for sorting & searching; writing direction; transliteration rules; rules for spelling out numbers; rules for segmenting text into graphemes, words, and sentences; keyboard layouts…
- **Country information:** language usage, currency information, calendar preference, week conventions,…
- **Validity:** Definitions, aliases, and validity information for Unicode locales, languages, scripts, regions, and extensions,…

CLDR uses the XML format provided by [UTS #35: Unicode Locale Data Markup Language (LDML)](https://www.unicode.org/reports/tr35/). LDML is a format used not only for CLDR, but also for general interchange of locale data, such as in Microsoft's .NET.

## Who uses CLDR?

Some of the companies and organizations that use CLDR are:
-   Apple (macOS, iOS, watchOS, tvOS, and several applications; Apple Mobile Device Support and iTunes for Windows; …)
-   Google (Web Search, Chrome, Android, Adwords, Google+, Google Maps, Blogger, Google Analytics, …)
-   IBM (DB2, Lotus, Websphere, Tivoli, Rational, AIX, i/OS, z/OS, …)
-   Meta (Facebook, Messenger, WhatsApp, …)
-   Microsoft (Windows, Office, Visual Studio, …)
-   *and many others, including:* ABAS Software, Adobe, Amazon (Kindle), Amdocs, Apache, Appian, Argonne National Laboratory, Avaya, Babel (Pocoo library), BAE Systems Geospatial eXploitation Products, BEA, BluePhoenix Solutions, BMC Software, Boost, BroadJump, Business Objects, caris, CERN, CLDR Engine, Debian Linux, Dell, Eclipse, eBay, elixir-cldr, EMC Corporation, ESRI, Firebird RDBMS, FreeBSD, Gentoo Linux, GroundWork Open Source, GTK+, Harman/Becker Automotive Systems GmbH, HP, Hyperion, Inktomi, Innodata Isogen, Informatica, Intel, Interlogics, IONA, IXOS, Jikes, jQuery, Library of Congress, Mathworks, Mozilla, Netezza, OpenOffice, Oracle (Solaris, Java), Lawson Software, Leica Geosystems GIS & Mapping LLC, Mandrake Linux, OCLC, Perl, Progress Software, Python, Qt, QNX, Rogue Wave, SAP, Shutterstock, SIL, SPSS, Software AG, SuSE, Symantec, Teradata (NCR), ToolAware, Trend Micro, Twitter, Virage, webMethods, Wikimedia Foundation (Wikipedia), Wine, WMS Gaming, XyEnterprise, Yahoo!, Yelp

There are other projects which consume [cldr-json] directly, see [here][cldr-json-users] for a list.

## How to Use?

Most developers will use CLDR indirectly, via a set of software libraries, such as [ICU](https://icu.unicode.org/), [Closure](https://github.com/google/closure-library), or [TwitterCLDR](https://blog.x.com/engineering/en_us/a/2012/twittercldr-improving-internationalization-support-in-ruby). These libraries typically compile the CLDR data into a format that is compact and easy for the library to load and use.

For those interested in the source CLDR data, it is available for each release in the XML format specified by [LDML](https://www.unicode.org/reports/tr35/). There are also tools that will convert to JSON and POSIX format. For more information, see [CLDR Releases/Downloads](index/downloads).

## How to Contribute?

CLDR is a collaborative project, which benefits by having people join and contribute. There are multiple ways to contribute to CLDR.

#### Translations and other language data

CLDR has an online tool to gather data, the [Survey Tool](index/survey-tool). The Survey Tool is usually open once a year to gather data for new structure, and make corrections in previously-released data.

- For languages that are already available in the Survey Tool, see [picking a locale](translation/getting-started/guide#picking-locales). If your locale is not already available in the Survey Tool, see [Adding new locales](requesting_changes#adding-new-locales).
    -   Contribute as an individual (vetter) for your language by [setting up an account](index/survey-tool/survey-tool-accounts)
    -   Qualifying organizations (companies, governments, institutions, etc) can request for an Organization level contribution status. Please file a [ticket](requesting_changes#how-to-file-a-ticket) if you need organization set up.
- Unicode [voting members](https://home.unicode.org/membership/members/) can join the technical committee for bigger impact. The CLDR Technical committee is responsible for assessing the Survey Tool features, proposals for additions or changes to structure, bug fixes, and final resolution of each release of CLDR.

#### Code and Structure

The CLDR tooling supports the interactive Survey Tool, plus all of the tooling necessary to test and process the release. Programmers interested in contributing to the tooling are welcome; they may also be interested in contributing to [ICU](https://icu.unicode.org/), which uses CLDR data. For more information, see [Development](development).

CLDR covers many different types of data, but not everything. For projects which may cover other types of data, see [Other Projects](covered-by-other-projects).

#### Tickets

People may file [tickets](requesting_changes) with bug fixes or feature requests. Once a ticket is approved, they can also create pull requests on [GitHub](https://github.com/unicode-org/cldr).

## Who has contributed?

Many people have made significant contributions to CLDR and LDML; see the [Acknowledgments](index/acknowledgments) page for a full listing.

## What is the Schedule?

CLDR has a regular schedule, with two cycles per year. There is a consistent release schedule each year so that implementations can plan ahead. The actual dates for each phase are somewhat adjusted for each release: in particular, the dates will usually fall on Wednesdays, and may change for holidays.

The two important periods for translators are:

- Submission: translators are asked to flesh out missing data, and check for consistency.
- Vetting: translators are asked to review all changed or conflicted values, and reach consensus.


The details for the current release are found in [Current CLDR Cycle](https://docs.google.com/spreadsheets/d/1N6inI5R84UoYlRwuCNPBOAP7ri4q2CmJmh8DC5g-S6c/edit#gid=1680747936).

[cldr-json]: /index/json-format-data
[cldr-json-users]: https://github.com/unicode-org/cldr-json/blob/master/USERS.md#projects
