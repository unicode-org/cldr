---
title: Unicode CLDR Project
---

# Unicode CLDR Project

## News
- **2026-04-29** [CLDR Survey Tool opens for General Submission for CLDR 49](/translation)
- **2026-03-17** [CLDR 48.2](/downloads/cldr-48#482-changes) released
- **2026-01-08** [CLDR 48.1](/downloads/cldr-48#481-changes) released
- **2025-10-29** [CLDR 48](downloads/cldr-48) released

## CLDR Mission

_To build and maintain the most trusted and comprehensive repository of [locale data], reflecting common usage across the world, through active participation from organizations and community members._

## What is CLDR?

CLDR (a.k.a. _Common Locale Data Repository_) supplies key information and structures critical for programs and operating systems around the world to ensure that they feel natural,
no matter which language users speak or where they live.

For example, imagine looking at a list of files on your mobile phone.
You’ll see the format of the dates (like the creation date), numbers, units (like the size of the file), and the alphabetical order of the files.
All of these will vary depending on your language — and all of these are supplied by CLDR. 

Just as there are Unicode standards for handling characters, writing systems, and their properties, CLDR is focused on languages and their regional variations (collectively referred to as locales).
Over 100 languages are supported, with more added each release.

CLDR consists of three main components:

1. A curated collection of structured data used by implementations
1. A specification, [UTS #35: Unicode Locale Data Markup Language (LDML)][], documenting the structure and usage of that data (via defined algorithms), and including conformance requirements and guidelines. 
1. Code used to collect that data from language specialists, guide those specialists in supplying the data, verify the validity and consistency, and process it into different formats for use by software developers.

Formatting dates, numbers, currencies, and units of measurement is far more complicated across different languages and regions than most people recognize.
Part of the goal of CLDR is to provide the foundation for APIs that handle that complexity without developers needing to know about 100+ languages.
It is the source for enabling software that needs to support languages ranging from Arabic to Zulu.

CLDR continues to add additional features each year, such as support for more complex grammatical and cultural variations needed in many countries.
So now (among many other things) it also describes how plurals work in various languages, and variations in how lists are alphabetized.
CLDR data and standards are vetted by in-country, native speaker linguistic experts and validated by Unicode’s diverse membership.

The standards, data, and algorithms that make up CLDR provide the basis for international language support and cultural adaptation of software for all manner of devices and software globally, with support for over 100 distinct languages.

## Who uses CLDR?

CLDR is incorporated into all modern operating systems and browsers; into many programming languages such as Java, C#, .NET, Swift, Javascript; and into most application programs.
Often the usage is indirect; an application uses an operating system service (eg, to format a date), which calls an ICU library (Unicode’s production code for C, C++, Java, and Rust), which then uses CLDR.
There are other libraries for other programming languages, such as Babel (Python), TwitterCLDR (Ruby), and Unicode::CLDR (Perl).
Some CLDR data is used more directly; the emoji short names and search keywords for many languages form the basis for many character pickers in applications and virtual keyboards.

Some of the companies and organizations that use CLDR are:
*   Apple (macOS, iOS, watchOS, tvOS, and several applications; Apple Mobile Device Support and iTunes for Windows; …)
*   Google (Web Search, Chrome, Android, Adwords, Google+, Google Maps, Blogger, Google Analytics, …)
*   IBM (DB2, Lotus, Websphere, Tivoli, Rational, AIX, i/OS, z/OS, …)
*   Meta (Facebook, Messenger, WhatsApp, …)
*   Microsoft (Windows, Office, Visual Studio, …)
*   *and many others, including:* ABAS Software, Adobe, Amazon (Kindle), Amdocs, Apache, Appian, Argonne National Laboratory, Avaya, Babel (Pocoo library), BAE Systems Geospatial eXploitation Products, BEA, BluePhoenix Solutions, BMC Software, Boost, BroadJump, Business Objects, caris, CERN, CLDR Engine, Debian Linux, Dell, Eclipse, eBay, elixir-cldr, EMC Corporation, ESRI, Firebird RDBMS, FreeBSD, Gentoo Linux, GroundWork Open Source, GTK+, Harman/Becker Automotive Systems GmbH, HP, Hyperion, Inktomi, Innodata Isogen, Informatica, Intel, Interlogics, IONA, IXOS, Jikes, jQuery, Library of Congress, Mathworks, Mozilla, Netezza, OpenOffice, Oracle (Solaris, Java), Lawson Software, Leica Geosystems GIS & Mapping LLC, Mandrake Linux, OCLC, Perl, Progress Software, Python, Qt, QNX, Rogue Wave, SAP, Shutterstock, SIL, SPSS, Software AG, SuSE, Symantec, Teradata (NCR), ToolAware, Trend Micro, Twitter, Virage, webMethods, Wikimedia Foundation (Wikipedia), Wine, WMS Gaming, XyEnterprise, Yahoo!, Yelp

There are other projects which consume [cldr-json] directly, see [here][cldr-json-users] for a list.

## How to Use?

Most developers will use CLDR indirectly, via a set of software libraries, such as [ICU](https://icu.unicode.org/), [Closure](https://github.com/google/closure-library), or [TwitterCLDR](https://blog.x.com/engineering/en_us/a/2012/twittercldr-improving-internationalization-support-in-ruby). These libraries typically compile the CLDR data into a format that is compact and easy for the library to load and use.

For those interested in the source CLDR data, it is available for each release in the XML format specified by [UTS #35: Unicode Locale Data Markup Language (LDML)][]. There are also tools that will convert to JSON and POSIX format. For more information, see [CLDR Releases/Downloads](index/downloads).

## How to Contribute?

CLDR is a collaborative project, which benefits by having people join and contribute. There are multiple ways to contribute to CLDR.

#### Translations and other language data

CLDR has an online tool to gather data, the [Survey Tool](index/survey-tool). The Survey Tool is usually open once a year to gather data for new structure, and make corrections in previously-released data.

- For languages that are already available in the Survey Tool, see [picking a locale](translation/getting-started/guide#picking-locales). If your locale is not already available in the Survey Tool, see [Adding new locales](requesting_changes#adding-new-locales).
    -   Contribute as an individual (vetter) for your language by [setting up an account](index/survey-tool/survey-tool-accounts)
    -   Qualifying organizations (companies, governments, institutions, etc) can request Organization level contribution status. See [CLDR Organization](index/survey-tool/cldr-organization) for details.
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
[locale data]: /index/cldr-spec/definitions#locale-data
[UTS #35: Unicode Locale Data Markup Language (LDML)]: https://www.unicode.org/reports/tr35/
