## <a name="Coverage_Levels" id="Coverage_Levels" href="#Coverage_Levels">Coverage Levels</a>

The following describes the structure used to set coverage levels used for CLDR.
That structure is used in CLDR tooling, and can also be used by consumers of CLDR data, such as described in [Data Size Reduction](tr35.md#Data_Size).

* The following lists the coverage levels. The qualifications for each level may change between releases of CLDR, and more detailed information for each level is on [Coverage Levels](https://cldr.unicode.org/index/cldr-spec/coverage-levels). Each level adds to what is in the lower level, so Basic includes all of Core, Moderate all of Basic, and so on.


| Code  | Level         | Description    |
| ----: | ------------- | -------------- |
| 0     | undetermined  | Does not meet any of the following levels. |
| 10    | core          | Core Locale — Has minimal data about the language and writing system that is required before other information can be added using the CLDR survey tool. |
| 40    | basic         | Selectable Locale — Minimal locale data necessary for a "selectable" locale in a platform UI. Very basic number and datetime formatting, etc. |
| 60    | moderate      | Document Content Locale — Minimal locale data for applications such as spreadsheets and word processors to support general document content internationalization: formatting number, datetime, currencies, sorting, plural handling, and so on. |
| 80    | modern        | UI Locale — Contains all fields in normal modern use, including all CLDR locale names, country names, timezone names, currencies in use, and so on. |
| 100   | comprehensive | Above modern level; typically more data than is needed in most implementations. |

The Basic through Modern levels are based on the definitions and specifications listed below.

```dtd
<!ELEMENT coverageLevels ( approvalRequirements, coverageVariable*, coverageLevel* ) >
<!ELEMENT coverageLevel EMPTY >
<!ATTLIST coverageLevel inLanguage CDATA #IMPLIED >
<!ATTLIST coverageLevel inScript CDATA #IMPLIED >
<!ATTLIST coverageLevel inTerritory CDATA #IMPLIED >
<!ATTLIST coverageLevel value CDATA #REQUIRED >
<!ATTLIST coverageLevel match CDATA #REQUIRED >
```

For example, here is an example coverageLevel line.

```xml
<coverageLevel
    value="30"
    inLanguage="(de|fi)"
    match="localeDisplayNames/types/type[@type='phonebook'][@key='collation']"/>
```

* The `coverageLevel` elements are read in order, and the first match results in a coverage level value. The element matches based on the `inLanguage`, `inScript`, `inTerritory`, and `match` attribute values, which are regular expressions. For example, in the above example, a match occurs if the language is de or fi, and if the path is a locale display name for `collation=phonebook`.


The `match` attribute value logically has `//ldml/` prefixed before it is applied. In addition, the `[@` is automatically quoted. Otherwise standard Perl/Java style regular expression syntax is used.

```dtd
<!ELEMENT coverageVariable EMPTY >
<!ATTLIST coverageVariable key CDATA #REQUIRED >
<!ATTLIST coverageVariable value CDATA #REQUIRED >
```

* The `coverageVariable` element allows us to create variables for certain regular expressions that are used frequently in the coverageLevel definitions above. Each coverage variable must contain a `key` / `value` pair of attributes, which can then be used to be substituted into a coverageLevel definition above.


For example, here is an example coverageLevel line using coverageVariable substitution.

```xml
<coverageVariable key="%dayTypes" value="(sun|mon|tue|wed|thu|fri|sat)">
<coverageVariable key="%wideAbbr" value="(wide|abbreviated)">
<coverageLevel value="20" match="dates/calendars/calendar[@type='gregorian']/days/dayContext[@type='format']/dayWidth[@type='%wideAbbr']/day[@type='%dayTypes']"/>
```

* In this example, the coverge variables %dayTypes and %wideAbbr are used to substitute their respective values into the match expression. This allows us to reuse the same variable for other coverageLevel matches that use the same regular expression fragment.


```dtd
<!ELEMENT approvalRequirements ( approvalRequirement* ) >
<!ELEMENT approvalRequirement EMPTY >
<!ATTLIST approvalRequirement votes CDATA #REQUIRED >
<!ATTLIST approvalRequirement locales CDATA #REQUIRED >
<!ATTLIST approvalRequirement paths CDATA #REQUIRED >
```

* The approvalRequirements allows to specify the number of survey tool votes required for approval, either based on locale, or path, or both. Certain locales require a higher voting threshold (usually 8 votes instead of 4), in order to promote greater stability in the data. Furthermore, certain fields that are very high visibility fields, such as number formats, require a CLDR TC committee member's vote for approval.


`votes=` can be a numeric value, or it can be of the form `=vetter` where `vetter` is one of the `VoteResolver.Level` enumerated values.
It can also be `=LOWER_BAR` (8) or `=HIGH_BAR` (same as `=tc`)  referring to the `VoteResolver` constants of the same names.

Here is an example of the approvalRequirements section.

```xml
<approvalRequirements>
    <!--  "high bar" items -->
    <approvalRequirement votes="=HIGH_BAR" locales="*" paths="//ldml/numbers/symbols[^/]++/(decimal|group)"/>
    <!--  established locales - https://cldr.unicode.org/index/process#h.rm00w9v03ia8 -->
    <approvalRequirement votes="=LOWER_BAR" locales="ar ca cs da de el es fi fr he hi hr hu it ja ko nb nl pl pt pt_PT ro ru sk sl sr sv th tr uk vi zh zh_Hant" paths=""/>
    <!--  all other items -->
    <approvalRequirement votes="=vetter" locales="*" paths=""/>
</approvalRequirements>
```

* This section specifies that a TC vote (20 votes) is required for decimal and grouping separators. Furthermore it specifies that any field in the established locales list (i.e. ar, ca, cs, etc.) requires 8 votes, and that all other locales require 4 votes only.


For more information on the CLDR Voting process, see [https://cldr.unicode.org/index/process](https://cldr.unicode.org/index/process)

### <a name="Coverage_Level_Definitions" id="Coverage_Level_Definitions" href="#Coverage_Level_Definitions">Definitions</a>
This is a snapshot of the contents of certain variables. The actual definitions in the coverageLevels.xml file may vary from these descriptions.

* _Target-Language_ is the language under consideration.
* _Target-Territories_ is the list of territories found by looking up _Target-Language_ in the `<languageData>` elements in [Supplemental Language Data](tr35-info.md#Supplemental_Language_Data).
* _Language-List_ is _Target-Language_, plus
* *** **moderate:** Chinese**: * **moderate:** Chinese, English, French, German, Italian, Japanese, Portuguese, Russian, Spanish, Unknown; Arabic, Hindi, Korean, Indonesian, Dutch, Bengali, Turkish, Thai, Polish (de, en, es, fr, it, ja, pt, ru, zh, und, ar, hi, ko, in, nl, bn, tr, th, pl). If an EU language, add the remaining official EU languages.

  * **modern:** all languages that are official or major commercial languages of modern territories
* _Target-Scripts_ is the list of scripts in which _Target-Language_ can be customarily written (found by looking up _Target-Language_ in the `<languageData>` elements in [Supplemental Language Data](tr35-info.md#Supplemental_Language_Data))_,_ plus Unknown (Zzzz)_._
* _Script-List_ is the _Target-Scripts_ plus the major scripts used for multiple languages
  * Latin, Simplified Chinese, Traditional Chinese, Cyrillic, Arabic (Latn, Hans, Hant, Cyrl, Arab)
* _Territory-List_ is the list of territories formed by taking the _Target-Territories_ and adding:
* *** **moderate:** Brazil**: * **moderate:** Brazil, China, France, Germany, India, Italy, Japan, Russia, United Kingdom, United States, Unknown; Spain, Canada, Korea, Mexico, Australia, Netherlands, Switzerland, Belgium, Sweden, Turkey, Austria, Indonesia, Saudi Arabia, Norway, Denmark, Poland, South Africa, Greece, Finland, Ireland, Portugal, Thailand, Hong Kong SAR China, Taiwan (BR, CN, DE, GB, FR, IN, IT, JP, RU, US, ZZ, ES, BE, SE, TR, AT, ID, SA, NO, DK, PL, ZA, GR, FI, IE, PT, TH, HK, TW). If an EU language, add the remaining member EU countries.

  * **modern:** all current ISO 3166 territories, plus the UN M.49 [[UNM49](tr35.md#UNM49)] regions in [Supplemental Territory Containment](tr35-info.md#Supplemental_Territory_Containment).
* _Currency-List_ is the list of current official currencies used in any of the territories in _Territory-List_, found by looking at the `region` elements in [Supplemental Territory Containment](tr35-info.md#Supplemental_Territory_Containment), plus Unknown (XXX).
* _Calendar-List_ is the set of calendars in customary use in any of _Target-Territories_, plus Gregorian.
* _Number-System-List_ is the set of number systems in customary use in the language.

### <a name="Coverage_Level_Data_Requirements" id="Coverage_Level_Data_Requirements" href="#Coverage_Level_Data_Requirements">Data Requirements</a>

The required data to qualify for each level based on these definitions is then the following.

1. localeDisplayNames
   1. _languages:_ localized names for all languages in _Language-List._
   2. _scripts:_ localized names for all scripts in _Script-List_.
   3. _territories:_ localized names for all territories in _Territory-List_.
   4. _variants, keys, types:_ localized names for any in use in _Target-Territories_; for example, a translation for PHONEBOOK in a German locale.

2. dates: all of the following for each calendar in _Calendar-List_.
   1. calendars: localized names
   2. month names, day names, era names, and quarter names
      * context=format and width=narrow, wide, & abbreviated
      * plus context=standAlone and width=narrow, wide, & abbreviated, _if the grammatical forms of these are different than for context=format._
   3. week: minDays, firstDay, weekendStart, weekendEnd
      * if some of these vary in territories in _Territory-List_, include territory locales for those that do.
   4. am, pm, eraNames, eraAbbr
   5. dateFormat, timeFormat: full, long, medium, short
   6. intervalFormatFallback

3. numbers: symbols, decimalFormats, scientificFormats, percentFormats, currencyFormats for each number system in _Number-System-List_.
4. currencies: displayNames and symbol for all currencies in _Currency-List_, for all plural forms
5. transforms: (moderate and above) transliteration between Latin and each other script in _Target-Scripts._

### <a name="Coverage_Level_Default_Values" id="Coverage_Level_Default_Values" href="#Coverage_Level_Default_Values">Default Values</a>

Items should _only_ be included if they are not the same as the default, which is:

* what is in root, if there is something defined there.
* for timezone IDs: the name computed according to _[Appendix J: Time Zone Display Names](tr35.md#Time_Zone_Fallback)_
* for collation sequence, the UCA DUCET (Default Unicode Collation Element Table), as modified by CLDR.
  * however, in that case the locale must be added to the validSubLocale list in [collation/root.xml](https://github.com/unicode-org/cldr/blob/main/common/collation/root.xml).
* for currency symbol, language, territory, script names, variants, keys, types, the internal code identifiers, for example,
  * currencies: EUR, USD, JPY, ...
  * languages: en, ja, ru, ...
  * territories: GB, JP, FR, ...
  * scripts: Latn, Thai, ...
  * variants: PHONEBOOK, ...

