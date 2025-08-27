# Data for generating descriptions of paths in Survey Tool

Example Entry:

    ### First Day of the Week

    - `^//ldml/localeDisplayNames/types/type\[@key="fw"]\[@type="([^"]*)"]`

    The name of “first day of the week is {1}”. For more information, please see [Locale Option Names].

1. The first line beginning with `###` is a comment and can be used to describe that section. It can be blank, just `###`.
    If it begins with `ROOT` it has special placeholders.
    If it begins with `SKIP` it will be ignored.
2. Next is a bullet entry `-` with a regex in backticks.
3. Finally is markdown, continuing up to the next line beginning with `#`.  Please keep all URLs in [References](#references), which is copied to every markdown fragment. This way we can share URLs and use more natural sounding links.
4. Double hash (`##`) are used to group sections. These may not be blank. The last section is special and is named `References`.

## ROOT descriptions (using special placeholders). Must occur first.

### ROOT territory

- `^//ldml/localeDisplayNames/territories/territory\[@type="(CD|DG|CG|003|021|ZA|018|FK|MK|MM|TW|HK|MO)"]`

Warning - the region {0} requires special attention! Note: before translating, be sure to read [Country Names].

### ROOT script

- `^//ldml/localeDisplayNames/scripts/script\[@type="(Z[^"]*)"]`

The name of the script (writing system) with Unicode script code = {0}. Note: before translating, be sure to read [Script Names].

### ROOT timezone

- `^//ldml/dates/timeZoneNames/zone\[@type="([^"]*)"]/exemplarCity`

The name of {0}. For more information, see [Time Zone City Names].

### ROOT language

- `^//ldml/localeDisplayNames/languages/language\[@type="([^"]*)"]\[@menu="([^"]*)"]`

The name of the language with Unicode language code = {0}, _and_ menu type = {0}. **Be sure to read about this in [Language Names]**.

### ROOT language

- `^//ldml/localeDisplayNames/languages/language\[@type="([^"]*)"]`

The name of the language with Unicode language code = {0}. For more information, see [Language Names].

### ROOT script

- `^//ldml/localeDisplayNames/scripts/script\[@type="([^"]*)"]`

The name of the script (writing system) with Unicode script code = {0}. For more information, see [Script Names].

### ROOT territory

- `^//ldml/localeDisplayNames/territories/territory\[@type="([^"]*)"]`

The name of the country or region with Unicode region code = {0}. For more information, see [Country Names].

### ROOT territory

- `^//ldml/localeDisplayNames/subdivisions/subdivision\[@type="([^"]*)"]`

The name of the country subdivision with Unicode subdivision code = {0}. For more information, see [Country Names].

### ROOT currency

- `^//ldml/numbers/currencies/currency\[@type="([^"]*)"]/symbol$`

The symbol for the currency with the ISO currency code = {0}. For more information, see [Currency Names].

### ROOT currency

- `^//ldml/numbers/currencies/currency\[@type="([^"]*)"]/symbol\[@alt="narrow"]`

The NARROW form of the symbol used for the currency with the ISO currency code = {0}, when the known context is already enough to distinguish the symbol from other currencies that may use the same symbol. Normally, this does not need to be changed from the inherited value. For more information, see [Currency Names].

### ROOT currency

- `^//ldml/numbers/currencies/currency\[@type="([^"]*)"]/symbol\[@alt="([^"]++)"]`

An alternative form of the symbol used for the currency with the ISO currency code = {0}. Usually occurs shortly after a new currency symbol is introduced. For more information, see [Currency Names].

### ROOT currency

- `^//ldml/numbers/currencies/currency\[@type="([^"]*)"]/displayName`

The name of the currency with the ISO currency code = {0}. For more information, see [Currency Names].


<!-- Note: we change the metazones dynamically in code -->

### ROOT metazone

- `^//ldml/dates/timeZoneNames/metazone\[@type="([^"]*)"](.*)/(.*)`

The name of the timezone for “{0}”. Note: before translating, be sure to read [Time Zone City Names].

## OTHER Descriptions

###

- `^//ldml/localeDisplayNames/types/type\[@key="collation"]\[@type="([^"]*)"]`

The name of “{1} collation” (sorting order). For more information, please see [Locale Option Names].

###

- `^//ldml/localeDisplayNames/types/type\[@key="numbers"]\[@type="([^"]*)"]`

The name of “{1} number system”. For more information, please see [Locale Option Names].

###

- `^//ldml/localeDisplayNames/types/type\[@key="calendar"]\[@type="roc"]`

The name of “roc calendar” (common names include “Minguo Calendar”, “Republic of China Calendar”, and “Republican Calendar”). For more information, please see [Locale Option Names].

###

- `^//ldml/localeDisplayNames/types/type\[@key="calendar"]\[@type="([^"]*)"]\[@alt="variant"]`

The alternate name of “{1} calendar”. For more information, please see [Locale Option Names].

###

- `^//ldml/localeDisplayNames/types/type\[@key="calendar"]\[@type="([^"]*)"]`

The name of “{1} calendar”. For more information, please see [Locale Option Names].

###

- `^//ldml/localeDisplayNames/types/type\[@key="em"]\[@type="([^"]*)"]`

The name of “emoji presentation style {1}”. For more information, please see [Locale Option Names].

### First Day of the Week

- `^//ldml/localeDisplayNames/types/type\[@key="fw"]\[@type="([^"]*)"]`

The name of “first day of the week is {1}”. For more information, please see [Locale Option Names].

###

- `^//ldml/localeDisplayNames/types/type\[@key="lb"]\[@type="([^"]*)"]`

The name of “{1} line break style”. For more information, please see [Locale Option Names].

###

- `^//ldml/localeDisplayNames/types/type\[@key="([^"]*)"]\[@type="([^"]*)"]`

The name of the “{2} {1}”. For more information, please see [Locale Option Names].

###

- `^//ldml/localeDisplayNames/keys/key\[@type="([^"]*)"]`

The name of the system for “{1}”. For more information, please see [Locale Option Names].


###

- `^//ldml/localeDisplayNames/types/type\[@key="collation"]\[@type="([^"]*)"]\[@scope="([^"]*)"]`

The _core_ name of “{1} collation” (sorting order) — **without the key name**. For more information, please see [Locale Option Names].

###

- `^//ldml/localeDisplayNames/types/type\[@key="numbers"]\[@type="([^"]*)"]\[@scope="([^"]*)"]`

The _core_ name of “{1} number system” — **without the key name**. For more information, please see [Locale Option Names].

###

- `^//ldml/localeDisplayNames/types/type\[@key="calendar"]\[@type="roc"]\[@scope="([^"]*)"]`

The _core_ name of “roc calendar” (common names include “Minguo Calendar”, “Republic of China Calendar”, and “Republican Calendar”) — **without the key name**. For more information, please see [Locale Option Names].

###

- `^//ldml/localeDisplayNames/types/type\[@key="calendar"]\[@type="([^"]*)"]\[@scope="([^"]*)"]`

The _core_ name of “{1} calendar” — **without the key name**. For more information, please see [Locale Option Names].

###

- `^//ldml/localeDisplayNames/types/type\[@key="em"]\[@type="([^"]*)"]\[@scope="([^"]*)"]`

The _core_ name of “emoji presentation style {1}” — **without the key name**. For more information, please see [Locale Option Names].

###

- `^//ldml/localeDisplayNames/types/type\[@key="fw"]\[@type="([^"]*)"]\[@scope="([^"]*)"]`

The _core_ name of “first day of the week is {1}” — **without the key name**. For more information, please see [Locale Option Names].

###

- `^//ldml/localeDisplayNames/types/type\[@key="lb"]\[@type="([^"]*)"]\[@scope="([^"]*)"]`

The _core_ name of “{1} line break style” — **without the key name**. For more information, please see [Locale Option Names].

###

- `^//ldml/localeDisplayNames/types/type\[@key="([^"]*)"]\[@type="([^"]*)"]\[@scope="([^"]*)"]`

The _core_ name of the “{2} {1}” — **without the key name**. For more information, please see [Locale Option Names].


###

- `^//ldml/localeDisplayNames/variants/variant[@type="([^"]*)"]`

The name of the language variant with code {0}. For more information, please see [Language Names].

###

- `^//ldml/characters/exemplarCharacters$`

Defines the set of characters used in your language. _To change this item, you have to flag it for review\!_ See [Changing Protected Items]. Before filing any tickets to request changes, be sure to also read [Exemplar Characters].

###

- `^//ldml/characters/exemplarCharacters\[@type="([^"]*)"]`

Defines the set of characters used in your language for the “{1}” category. _To change this item, you have to flag it for review\!_ See [Changing Protected Items]. Before filing any tickets to request changes, be sure to also read [Exemplar Characters].

###

- `^//ldml/characters/parseLenients`

Defines sets of characters that are treated as equivalent in parsing. _To change this item, you have to flag it for review\!_ See [Changing Protected Items]. Before filing any tickets to request changes, be sure to also read [Exemplar Characters].

###

- `^//ldml/characters/ellipsis\[@type="([^"]*)"]`

Supply the ellipsis pattern for when the {1} part of a string is omitted. Note: before translating, be sure to read [Characters].

###

- `^//ldml/characters/moreInformation`

The character or short string used to indicate that more information is available. Note: before translating, be sure to read [Characters].

###

- `^//ldml/delimiters/alternateQuotationEnd`

Supply the (alternate) ending quotation mark (the right mark except in BIDI languages). Note: before translating, be sure to read [Characters].

###

- `^//ldml/delimiters/alternateQuotationStart`

Supply the (alternate) starting quotation mark (the left mark except in BIDI languages). Note: before translating, be sure to read [Characters].

###

- `^//ldml/delimiters/quotationEnd`

Supply the ending quotation mark (the right mark except in BIDI languages). Note: before translating, be sure to read [Characters].

###

- `^//ldml/delimiters/quotationStart`

Supply the starting quotation mark (the left mark except in BIDI languages). Note: before translating, be sure to read [Characters].

###

- `^//ldml/localeDisplayNames/localeDisplayPattern/localePattern`

The pattern used to compose locale (language) names. Note: before translating, be sure to read [Locale Patterns].

###

- `^//ldml/localeDisplayNames/localeDisplayPattern/localeSeparator`

The separator used to compose modifiers in locale (language) names. Note: before translating, be sure to read [Locale Patterns].

###

- `^//ldml/localeDisplayNames/localeDisplayPattern/localeKeyTypePattern`

The pattern used to compose key-type values in locale (language) names. Note: before translating, be sure to read [Locale Patterns].

###

- `^//ldml/layout/orientation/characterOrder`

Specifies the horizontal direction of text in the language. Valid values are "left-to-right" or "right-to-left". For more information, see [Units Misc Help].

###

- `^//ldml/layout/orientation/lineOrder`

Specifies the vertical direction of text in the language. Valid values are "top-to-bottom" or "bottom-to-top". For more information, see [Units Misc Help].

###

- `^//ldml/numbers/symbols\[@numberSystem="([a-z]*)"]/(\w++)`

The {2} symbol used in the {1} numbering system. NOTE: especially for the decimal and grouping symbol, before translating, be sure to read [Numbers].

###

- `^//ldml/numbers/defaultNumberingSystem`

The default numbering system used in this locale. For more information, please see [Numbering Systems].

###

- `^//ldml/numbers/minimumGroupingDigits`

The default minimum number of digits before a grouping separator used in this locale. For more information, please see [Numbering Systems].

###

- `^//ldml/numbers/otherNumberingSystems/(\w++)`

The {1} numbering system used in this locale. For more information, please see [Numbering Systems].

###

- `^//ldml/dates/timeZoneNames/regionFormat\[@type="standard"]`

The pattern used to compose standard (winter) fallback time zone names, such as 'Germany Winter Time'. Note: before translating, be sure to read [Time Zone City Names].

###

- `^//ldml/dates/timeZoneNames/regionFormat\[@type="daylight"]`

The pattern used to compose daylight (summer) fallback time zone names, such as 'Germany Summer Time'. Note: before translating, be sure to read [Time Zone City Names].

###

- `^//ldml/dates/timeZoneNames/regionFormat`

The pattern used to compose generic fallback time zone names, such as 'Germany Time'. Note: before translating, be sure to read [Time Zone City Names].

###

- `^//ldml/dates/timeZoneNames/(fallback|fallbackRegion|gmtZero|gmtUnknown|gmt|hour|region)Format`

The {1} pattern used to compose time zone names. Note: before translating, be sure to read [Time Zone City Names].

<!-- Warning: the longer match and more specific match must come first! -->

###

- `^//ldml/units/unitLength\[@type="([^"]*)"]/compoundUnit\[@type="([^"]*)"]/compoundUnitPattern1`

Special pattern used to compose powers of a unit, such as meters squared. Note: before translating, be sure to read [Compound Units].

###

- `^//ldml/units/unitLength\[@type="([^"]*)"]/compoundUnit\[@type="([^"]*)"]/compoundUnitPattern`

Special pattern used to compose forms of two units, such as meters per second. Note: before translating, be sure to read [Units].

###

- `^//ldml/units/unitLength\[@type="([^"]*)"]/compoundUnit\[@type="([^"]*)"]/unitPrefixPattern`

Special pattern used to compose a metric prefix with a unit, such as kilo{0} with meters to produce kilometers. Note: before translating, be sure to read [Units].

###

- `^//ldml/units/unitLength\[@type="([^"]*)"]/coordinateUnit/displayName`

Display name ({1} form) for the type of direction used in latitude and longitude, such as north or east. Note: before translating, be sure to read [Units].

###

- `^//ldml/units/unitLength\[@type="([^"]*)"]/coordinateUnit/coordinateUnitPattern\[@type="([^"]*)"]`

Special pattern used in latitude and longitude, such as 12°N. Note: before translating, be sure to read [Units].

###

- `^//ldml/units/unitLength\[@type="([^"]*)"]/unit\[@type="area-acre"]/displayName`

Display name ({1} form) for “area-acre”, referring specifically to an English acre. Note: before translating, be sure to read [Units].

###

- `^//ldml/units/unitLength\[@type="([^"]*)"]/unit\[@type="duration-day"]/displayName`

Display name ({1} form) for “duration-day”, meaning a time duration of 24 hours (not a calendar day). Note: before translating, be sure to read [Units].

###

- `^//ldml/units/unitLength\[@type="([^"]*)"]/unit\[@type="energy-calorie"]/displayName`

Display name ({1} form) for “energy-calorie”, calories as used in chemistry, not the same as food calorie. Note: before translating, be sure to read [Units].

###

- `^//ldml/units/unitLength\[@type="([^"]*)"]/unit\[@type="energy-foodcalorie"]/displayName`

Display name ({1} form) for “energy-foodcalorie”, kilocalories for food energy; may have same translation as energy-kilocalorie. Note: before translating, be sure to read [Units].

###

- `^//ldml/units/unitLength\[@type="([^"]*)"]/unit\[@type="energy-kilocalorie"]/displayName`

Display name ({1} form) for “energy-kilocalorie”, kilocalories for uses not specific to food energy, such as chemistry. Note: before translating, be sure to read [Units].

###

- `^//ldml/units/unitLength\[@type="([^"]*)"]/unit\[@type="graphics-em"]/displayName`

Display name ({1} form) for “graphics-em”, referring to typographic length equal to a font’s point size. Note: before translating, be sure to read [Units].

###

- `^//ldml/units/unitLength\[@type="([^"]*)"]/unit\[@type="graphics-pixel"]/displayName`

Display name ({1} form) for “graphics-pixel”, used for counting the individual elements in bitmap image. Note: before translating, be sure to read [Units].

###

- `^//ldml/units/unitLength\[@type="([^"]*)"]/unit\[@type="graphics-megapixel"]/displayName`

Display name ({1} form) for “graphics-megapixel”, used for counting the individual elements in bitmap image. Note: before translating, be sure to read [Units].

###

- `^//ldml/units/unitLength\[@type="([^"]*)"]/unit\[@type="graphics-pixel-per-centimeter"]/displayName`

Display name ({1} form) for “graphics-pixel-per-centimeter”, typically used for display resolution. Note: before translating, be sure to read [Units].

###

- `^//ldml/units/unitLength\[@type="([^"]*)"]/unit\[@type="graphics-pixel-per-inch"]/displayName`

Display name ({1} form) for “graphics-pixel-per-inch”, typically used for display resolution. Note: before translating, be sure to read [Units].

###

- `^//ldml/units/unitLength\[@type="([^"]*)"]/unit\[@type="graphics-dot-per-centimeter"]/displayName`

Display name ({1} form) for “graphics-dot-per-centimeter”, typically used for printer resolution. Note: before translating, be sure to read [Units].

###

- `^//ldml/units/unitLength\[@type="([^"]*)"]/unit\[@type="graphics-dot-per-inch"]/displayName`

Display name ({1} form) for “graphics-dot-per-inch”, typically used for printer resolution. Note: before translating, be sure to read [Units].

###

- `^//ldml/units/unitLength\[@type="([^"]*)"]/unit\[@type="length-point"]/displayName`

Display name ({1} form) for “length-point”, referring to a typographic point, 1/72 inch. Note: before translating, be sure to read [Units].

###

- `^//ldml/units/unitLength\[@type="([^"]*)"]/unit\[@type="mass-stone"]/displayName`

Display name ({1} form) for “mass-stone”, used in UK/Ireland for body weight, equal to 14 pounds. Note: before translating, be sure to read [Units].

###

- `^//ldml/units/unitLength\[@type="([^"]*)"]/unit\[@type="mass-ton"]/displayName`

Display name ({1} form) for “mass-ton”, meaning U.S. short ton, not U.K. long ton or metric ton. Note: before translating, be sure to read [Units].

###

- `^//ldml/units/unitLength\[@type="([^"]*)"]/unit\[@type="([^"]*)"]/displayName`

Display name ({1} form) for “{2}”. Note: before translating, be sure to read [Units].

###

- `^//ldml/units/unitLength\[@type="([^"]*)"]/unit\[@type="([^"]*)"]/unitPattern`

[ICU Syntax] Special pattern used to compose plural for {1} forms of “{2}”. Note: before translating, be sure to read [Plurals].

###

- `^//ldml/units/unitLength\[@type="([^"]*)"]/unit\[@type="([^"]*)"]/gender`

Gender ({1} form) for “{2}”. Note: before translating, be sure to read [Grammatical Inflection].

###

- `^//ldml/units/unitLength\[@type="([^"]*)"]/unit\[@type="([^"]*)"]/perUnitPattern`

Special pattern ({1} form) used to compose values per unit, such as “meters per {2}”. Note: before translating, be sure to read [Units].

###

- `^//ldml/units/durationUnit\[@type="(hms|hm|ms)"]`

[ICU Syntax] Special pattern used to compose duration units. Note: before translating, be sure to read [Plurals].

###

- `^//ldml/numbers/decimalFormats\[@numberSystem="([^"]*)"]/decimalFormatLength\[@type="([^"]*)"]/decimalFormat\[@type="([^"]*)"]/pattern\[@type="([^"]*)"]`

Special pattern used for a short version of numbers with the same number of digits as {4}. Note: before translating, be sure to read [Short Numbers].

###

- `^//ldml/numbers/currencyFormats\[@numberSystem="([^"]*)"]/currencyFormatLength\[@type="short"]/currencyFormat\[@type="standard"]/pattern\[@type="(\d+)"]\[@count="([^"]+)"]`

Special currency pattern used to obtain the abbreviated plural forms of numbers with the same number of digits as {2}. See [Short Numbers] for details.

###

- `^//ldml/numbers/decimalFormats\[@numberSystem="([^"]*)"]/decimalFormatLength\[@type="short"]/decimalFormat\[@type="standard"]/pattern\[@type="(\d+)"]\[@count="([^"]+)"]`

Special decimal pattern used to obtain the abbreviated plural forms of numbers with the same number of digits as {2}. See [Short Numbers] for details.

###

- `^//ldml/numbers/decimalFormats\[@numberSystem="([^"]*)"]/decimalFormatLength\[@type="long"]/decimalFormat\[@type="standard"]/pattern\[@type="(\d+)"]\[@count="([^"]+)"]`

Special decimal pattern used to obtain the long plural forms of numbers with the same number of digits as {2}. See [Plural Numbers] for details.

###

- `^//ldml/numbers/currencyFormats\[@numberSystem="([^"]*)"]/currencyPatternAppendISO`

Pattern used to combine a regular currency format with an ISO 4217 code (¤¤). For more information, please see [Number Patterns].

###

- `^//ldml/numbers/currencyFormats\[@numberSystem="([^"]*)"]/unitPattern\[@count="(\w++)"]`

Currency format used for numbers of type {2}. For more information, please see [Number Patterns].

###

- `^//ldml/numbers/miscPatterns\[@numberSystem="([^"]*)"]/pattern\[@type="range"]`

Format used to indicate a range of numbers. The '{'0'}' and '{'1'}' in the pattern represent the lowest and highest numbers in the range, respectively. For more information, please see [Units Misc Help].

<!-- note: see TestPathDescription.java if you change this text-->

###

- `^//ldml/numbers/miscPatterns\[@numberSystem="([^"]*)"]/pattern\[@type="atLeast"]`

Format used to indicate a number is at least a certain value, often combined with other patterns to produce examples such as “≥12kg”. For more information, please see [Units Misc Help].

###

- `^//ldml/numbers/miscPatterns\[@numberSystem="([^"]*)"]/pattern\[@type="atMost"]`

Format used to indicate a number is at most a certain value, often combined with other patterns to produce examples such as “≤12kg”. For more information, please see [Units Misc Help].

###

- `^//ldml/numbers/miscPatterns\[@numberSystem="([^"]*)"]/pattern\[@type="approximately"]`

Format used to indicate a number is approximately a given value, often combined with other patterns to produce examples such as “~12kg”. For more information, please see [Units Misc Help].

###

- `^//ldml/numbers/minimalPairs/ordinalMinimalPairs\[@ordinal="([^"]*)"]`

Minimal pairs for ordinals. For more information, please see [Plural Minimal Pairs].

###

- `^//ldml/numbers/minimalPairs/pluralMinimalPairs\[@count="([^"]*)"]`

Minimal pairs for plurals (cardinals). For more information, please see [Plural Minimal Pairs].

###

- `^//ldml/numbers/minimalPairs/caseMinimalPairs\[@case="([^"]*)"]`

Minimal pairs for cases used in the language. For more information, please see [Grammatical Inflection].

###

- `^//ldml/numbers/minimalPairs/genderMinimalPairs\[@gender="([^"]*)"]`

Minimal pairs for genders. For more information, please see [Grammatical Inflection].

###

- `^//ldml/personNames/nameOrderLocales\[@order="([^"]*)"]`

Person name order for locales. If there are none with a particular direction, insert ❮EMPTY❯. For more information, please see [Person Name Formats].

###

- `^//ldml/personNames/parameterDefault\[@parameter="([^"]*)"]`

Person name default parameters. Make the appropriate formality and length settings for your locale. For more information, please see [Person Name Formats].

###

- `^//ldml/personNames/foreignSpaceReplacement`

For foreign personal names displayed in your locale, any special character that replaces a space (defaults to regular space). If spaces are to be removed, insert ❮EMPTY❯. For more information, please see [Person Name Formats].

###

- `^//ldml/personNames/nativeSpaceReplacement`

For native personal names displayed in your locale, should be ❮EMPTY❯ if your language doesn't use spaces between any name parts (such as Japanese), and otherwise a space. For more information, please see [Person Name Formats].

###

- `^//ldml/personNames/initialPattern\[@type="initial"]`

The pattern used for a single initial in person name formats. For more information, please see [Person Name Formats].

###

- `^//ldml/personNames/initialPattern\[@type="initialSequence"]`

The pattern used to compose sequences of initials in person name formats. For more information, please see [Person Name Formats].

###

- `^//ldml/personNames/personName\[@order="([^"]*)"]\[@length="([^"]*)"]\[@usage="referring"]\[@formality="([^"]*)"]`

Person name formats for referring to a person (with a particular order, length, formality). For more information, please see [Person Name Formats].

###

- `^//ldml/personNames/personName\[@order="([^"]*)"]\[@length="([^"]*)"]\[@usage="addressing"]\[@formality="([^"]*)"]`

Person name format for addressing a person (with a particular order, length, formality). For more information, please see [Person Name Formats].

###

- `^//ldml/personNames/personName\[@order="([^"]*)"]\[@length="([^"]*)"]\[@usage="monogram"]\[@formality="([^"]*)"]`

Person name formats for monograms (with a particular order, length, formality). For more information, please see [Person Name Formats].

###

- `^//ldml/personNames/sampleName`

Sample names for person name format examples (enter ∅∅∅ for optional unused fields). For more information, please see [Person Name Formats].

###

- `^//ldml/numbers/rationalFormats\[@numberSystem="([^"]*)"]/rationalPattern`

A pattern that is used to format a rational fraction (eg, ½), using the numerator and denominator. See [Rational Numbers].

###

- `^//ldml/numbers/rationalFormats\[@numberSystem="([^"]*)"]/integerAndRationalPattern\[@alt="([^"]*)"]`

A pattern that is used to “glue” an integer and a formatted rational fraction (eg, ½) together; only used when the rational fraction does not start with an un-superscripted digit. See [Rational Numbers].

###

- `^//ldml/numbers/rationalFormats\[@numberSystem="([^"]*)"]/integerAndRationalPattern`

A pattern that is used to “glue” an integer and a formatted rational fraction (eg, ½) together. See [Rational Numbers].

###

- `^//ldml/numbers/rationalFormats\[@numberSystem="([^"]*)"]/rationalUsage`

A value that is used to indicate the usage of rational fractions (eg, ½) in your language; **only** pick “never” if it never occurs with this numbering system in your language, including text translated from another language. See [Rational Numbers].

###

- `^//ldml/numbers/currencyFormats\[@numberSystem="([^"]*)"]/currencyFormatLength/currencyFormat\[@type="standard"]/pattern\[@type="standard"]\[@alt="alphaNextToNumber"]`

Special pattern used to compose currency values when the currency symbol has a letter adjacent to the number. Note: before translating, be sure to read [Number Patterns].

###

- `^//ldml/numbers/currencyFormats\[@numberSystem="([^"]*)"]/currencyFormatLength/currencyFormat\[@type="standard"]/pattern\[@type="standard"]\[@alt="noCurrency"]`

Special pattern used to compose currency values for which no currency symbol should be shown. Note: before translating, be sure to read [Number Patterns].

###

- `^//ldml/numbers/currencyFormats\[@numberSystem="([^"]*)"]/currencyFormatLength/currencyFormat\[@type="accounting"]/pattern`

Special pattern used to compose currency values for accounting purposes. Note: before translating, be sure to read [Number Patterns].

###

- `^//ldml/numbers/currencyFormats\[@numberSystem="([^"]*)"]/currencySpacing/([a-zA-Z]*)/([a-zA-Z]*)`

Special pattern used to compose currency signs ($2/$3) with numbers. Note: before translating, be sure to read [Number Patterns].


<!-- the following matches remaining number formats. It must go AFTER specific ones. -->

###

- `^//ldml/numbers/([a-z]*)Formats(\[@numberSystem="([^"]*)"])?/\1FormatLength/\1Format\[@type="standard"]/pattern\[@type="standard"]$`

Special pattern used to compose {1} numbers. Note: before translating, be sure to read [Number Patterns].

###

- `^//ldml/listPatterns/listPattern/listPatternPart\[@type="2"]`

Special pattern used to make an “and” list out of two standard elements. Note: before translating, be sure to read [Lists].

###

- `^//ldml/listPatterns/listPattern/listPatternPart\[@type="([^"]*)"]`

Special pattern used to make a “and” list out of more than two standard elements. This is used for the {1} portion of the list. Note: before translating, be sure to read [Lists].

###

- `^//ldml/listPatterns/listPattern\[@type="standard-short"]/listPatternPart\[@type="2"]`

Special pattern used to make a short-style “and” list out of two standard elements. Note: before translating, be sure to read [Lists].

###

- `^//ldml/listPatterns/listPattern\[@type="standard-short"]/listPatternPart\[@type="([^"]*)"]`

Special pattern used to make a short-style “and” list out of more than two standard elements. This is used for the {1} portion of the list. Note: before translating, be sure to read [Lists].

###

- `^//ldml/listPatterns/listPattern\[@type="standard-narrow"]/listPatternPart\[@type="2"]`

Special pattern used to make a short-style “and” list out of two standard elements. Note: before translating, be sure to read [Lists].

###

- `^//ldml/listPatterns/listPattern\[@type="standard-narrow"]/listPatternPart\[@type="([^"]*)"]`

Special pattern used to make a short-style “and” list out of more than two standard elements. This is used for the {1} portion of the list. Note: before translating, be sure to read [Lists].

###

- `^//ldml/listPatterns/listPattern\[@type="or"]/listPatternPart\[@type="2"]`

Special pattern used to make an “or” list out of two standard elements. Note: before translating, be sure to read [Lists].

###

- `^//ldml/listPatterns/listPattern\[@type="or"]/listPatternPart\[@type="([^"]*)"]`

Special pattern used to make an “or” list out of more than two standard elements. This is used for the {1} portion of the list. Note: before translating, be sure to read [Lists].

###

- `^//ldml/listPatterns/listPattern\[@type="or-short"]/listPatternPart\[@type="2"]`

Special pattern used to make an “or” list out of two standard elements. Note: before translating, be sure to read [Lists].

###

- `^//ldml/listPatterns/listPattern\[@type="or-short"]/listPatternPart\[@type="([^"]*)"]`

Special pattern used to make an “or” list out of more than two standard elements. This is used for the {1} portion of the list. Note: before translating, be sure to read [Lists].

###

- `^//ldml/listPatterns/listPattern\[@type="or-narrow"]/listPatternPart\[@type="2"]`

Special pattern used to make an “or” list out of two standard elements. Note: before translating, be sure to read [Lists].

###

- `^//ldml/listPatterns/listPattern\[@type="or-narrow"]/listPatternPart\[@type="([^"]*)"]`

Special pattern used to make an “or” list out of more than two standard elements. This is used for the {1} portion of the list. Note: before translating, be sure to read [Lists].

###

- `^//ldml/listPatterns/listPattern\[@type="unit"]/listPatternPart\[@type="2"]`

Special pattern used to make a list out of two unit elements. Note: before translating, be sure to read [Lists].

###

- `^//ldml/listPatterns/listPattern\[@type="unit"]/listPatternPart\[@type="([^"]*)"]`

Special pattern used to make a list out of more than two unit elements. This is used for the {1} portion of the list. Note: before translating, be sure to read [Lists].

###

- `^//ldml/listPatterns/listPattern\[@type="unit-short"]/listPatternPart\[@type="2"]`

Special pattern used to make a list out of two abbreviated unit elements. Note: before translating, be sure to read [Lists].

###

- `^//ldml/listPatterns/listPattern\[@type="unit-short"]/listPatternPart\[@type="([^"]*)"]`

Special pattern used to make a list out of more than two abbreviated unit elements. This is used for the {1} portion of the list. Note: before translating, be sure to read [Lists].

###

- `^//ldml/listPatterns/listPattern\[@type="unit-narrow"]/listPatternPart\[@type="2"]`

Special pattern used to make a list out of two narrow unit elements. Note: before translating, be sure to read [Lists].

###

- `^//ldml/listPatterns/listPattern\[@type="unit-narrow"]/listPatternPart\[@type="([^"]*)"]`

Special pattern used to make a list out of more than two narrow unit elements. This is used for the {1} portion of the list. Note: before translating, be sure to read [Lists].

###

- `^//ldml/dates/calendars/calendar\[@type="([^"]*)"]/dayPeriods/dayPeriodContext\[@type="(format)"]/dayPeriodWidth\[@type="([^"]*)"]/dayPeriod\[@type="([^"]*)"]`

Provide the {3}, {2} version of the name for the day period code “{4}”. This version must have the right inflection/prepositions/etc. for adding after a number, such as “in the morning” for use in “10:00 in the morning”. To see the time spans for these codes, please see [Date Time]

###

- `^//ldml/dates/calendars/calendar\[@type="([^"]*)"]/dayPeriods/dayPeriodContext\[@type="([^"]*)"]/dayPeriodWidth\[@type="([^"]*)"]/dayPeriod\[@type="([^"]*)"]`

Provide the {3}, {2} version of the name for the day period code “{4}”. To see the time spans for these codes, please see [Date Time]

###

- `^//ldml/dates/calendars/calendar\[@type="([^"]*)"]/days/dayContext\[@type="([^"]*)"]/dayWidth\[@type="([^"]*)"]/day\[@type="([^"]*)"]`

Provide the {2} and {3} version of the name for day-of-the-week {4}. For more information, please see [Date Time Names].

###

- `^//ldml/dates/calendars/calendar\[@type="([^"]*)"]/eras/eraAbbr/era\[@type="([^"]*)"]`

Provide the format-abbreviated version of the name for era {2}. For more information, please see [Date Time Names].

###

- `^//ldml/dates/calendars/calendar\[@type="([^"]*)"]/eras/eraNames/era\[@type="([^"]*)"]`

Provide the format-wide version of the name for era {1}. For more information, please see [Date Time Names].

###

- `^//ldml/dates/calendars/calendar\[@type="([^"]*)"]/eras/eraNarrow/era\[@type="([^"]*)"]`

Provide the format-narrow version of the name for era {1}. For more information, please see [Date Time Names].

###

- `^//ldml/dates/calendars/calendar\[@type="([^"]*)"]/months/monthContext\[@type="([^"]*)"]/monthWidth\[@type="([^"]*)"]/month\[@type="([^"]*)"]`

Provide the {2} and {3} version of the name for month {4}. For more information, please see [Date Time Names].

###

- `^//ldml/dates/calendars/calendar\[@type="([^"]*)"]/quarters/quarterContext\[@type="([^"]*)"]/quarterWidth\[@type="([^"]*)"]/quarter\[@type="([^"]*)"]`

Provide the {2} and {3} version of the name for quarter {4}. For more information, please see [Date Time Names].

###

- `^//ldml/dates/fields/field\[@type="([^"]*)"]/displayName`

Provide the name (as it would appear in menus) for the field “{1}”. For more information, please see [Date Time Fields].

### Relative Today

- `^//ldml/dates/fields/field\[@type="day"]/relative\[@type="0"]`

Provide the name for today. For more information, please see [Relative Dates].
The lettercasing should be appropriate for the top example. If the lettercasing is then wrong for the bottom example, please file a ticket to fix contextTransforms/relative/stand-alone.

### Relative before-today

- `^//ldml/dates/fields/field\[@type="day"]/relative\[@type="-([^"]*)"]`

Provide a name for the day, {1} before today. For more information, please see [Relative Dates].
The lettercasing should be appropriate for the top example. If the lettercasing is then wrong for the bottom example, please file a ticket to fix contextTransforms/relative/stand-alone.

### Relative after-today

- `^//ldml/dates/fields/field\[@type="day"]/relative\[@type="([^"]*)"]`

Provide a name for the day, {1} after today. For more information, please see [Relative Dates].
The lettercasing should be appropriate for the top example. If the lettercasing is then wrong for the bottom example, please file a ticket to fix contextTransforms/relative/stand-alone.

### This X

- `^//ldml/dates/fields/field\[@type="([^"]*)"]/relative\[@type="0"]`

Provide the name for “this {1}”. For more information, please see [Relative Dates].
The lettercasing should be appropriate for the top example. If the lettercasing is then wrong for the bottom example, please file a ticket to fix contextTransforms/relative/stand-alone.

### Last X

- `^//ldml/dates/fields/field\[@type="([^"]*)"]/relative\[@type="-1"]`

Provide a name for “last {1}”. For more information, please see [Relative Dates].
The lettercasing should be appropriate for the top example. If the lettercasing is then wrong for the bottom example, please file a ticket to fix contextTransforms/relative/stand-alone.

### Next X

- `^//ldml/dates/fields/field\[@type="([^"]*)"]/relative\[@type="1"]`

Provide a name for “next {1}”. For more information, please see [Relative Dates].
The lettercasing should be appropriate for the top example. If the lettercasing is then wrong for the bottom example, please file a ticket to fix contextTransforms/relative/stand-alone.

### Future Pattern

- `^//ldml/dates/fields/field\[@type="([^"]*)"]/relativeTime\[@type="future"]/relativeTimePattern\[@count="([^"]*)"]`

Provide a pattern used to display times in the future. For more information, please see [Date Time Names].
The lettercasing should be appropriate for the top example. If the lettercasing is then wrong for the bottom example, please file a ticket to fix contextTransforms/relative/stand-alone.

### Past Pattern

- `^//ldml/dates/fields/field\[@type="([^"]*)"]/relativeTime\[@type="past"]/relativeTimePattern\[@count="([^"]*)"]`

Provide a pattern used to display times in the past. For more information, please see [Date Time Names].
The lettercasing should be appropriate for the top example. If the lettercasing is then wrong for the bottom example, please file a ticket to fix contextTransforms/relative/stand-alone.

###

- `^//ldml/dates/fields/field\[@type="([^"]*)"]/relativePeriod`

Provide a name for “the {1} of SOME_DATE”. For more information, please see [Date Time Names].

###

- `^//ldml/dates/calendars/calendar\[@type="([^"]*)"]/dateTimeFormats/dateTimeFormatLength\[@type="([^"]*)"]/dateTimeFormat\[@type="standard"]/pattern\[@type="([^"]*)"]`

Provide the {2} version of the date-time pattern suitable for most use cases, including combining a date with a time range. Note: before translating, be sure to read [Date Time Patterns].

###

- `^//ldml/dates/calendars/calendar\[@type="([^"]*)"]/dateTimeFormats/dateTimeFormatLength\[@type="([^"]*)"]/dateTimeFormat\[@type="atTime"]/pattern\[@type="([^"]*)"]`

Provide the {2} version of the date-time pattern suitable for expressing a standard date (e.g. "March 20") at a specific time. Note: before translating, be sure to read [Date Time Patterns].

###

- `^//ldml/dates/calendars/calendar\[@type="([^"]*)"]/dateTimeFormats/dateTimeFormatLength\[@type="([^"]*)"]/dateTimeFormat\[@type="relative"]/pattern\[@type="([^"]*)"]`

Provide the {2} version of the date-time pattern suitable for expressing a relative date (e.g. "tomorrow") at a specific time. Note: before translating, be sure to read [Date Time Patterns].

###

- `^//ldml/dates/calendars/calendar\[@type="([^"]*)"]/dateFormats/dateFormatLength\[@type="([^"]*)"]/dateFormat\[@type="([^"]*)"]/pattern\[@type="([^"]*)"]`

Provide the {2} version of the basic date pattern. Note: before translating, be sure to read [Date Time Patterns].

###

- `^//ldml/dates/calendars/calendar\[@type="([^"]*)"]/timeFormats/timeFormatLength\[@type="([^"]*)"]/timeFormat\[@type="([^"]*)"]/pattern\[@type="([^"]*)"]`

Provide the {2} version of the basic time pattern. Note: before translating, be sure to read [Date Time Patterns].

###

- `^//ldml/dates/calendars/calendar\[@type="([^"]*)"]/dateTimeFormats/availableFormats/dateFormatItem\[@id="([^"]*)"]`

Provide the pattern used in your language for the skeleton “{2}”. Note: before translating, be sure to read [Date Time Patterns].

###

- `^//ldml/dates/calendars/calendar\[@type="([^"]*)"]/dateTimeFormats/appendItems/appendItem\[@request="([^"]*)"]`

Provide the pattern used in your language to append a “{2}” to another format. Note: before translating, be sure to read [Date Time Patterns].

###

- `^//ldml/dates/calendars/calendar\[@type="([^"]*)"]/dateTimeFormats/intervalFormats/intervalFormatFallback`

The pattern used for “fallback” with date/time intervals. Note: before translating, be sure to read [Date Time Patterns].

###

- `^//ldml/dates/calendars/calendar\[@type="([^"]*)"]/dateTimeFormats/intervalFormats/intervalFormatItem\[@id="([^"]*)"]/greatestDifference\[@id="([^"]*)"]`

The pattern used for the date/time interval skeleton “{2}” when the greatest difference is “{3}”. Note: before translating, be sure to read [Date Time Patterns].

###

- `^//ldml/dates/calendars/calendar\[@type="[^"]*"]/cyclicNameSets/cyclicNameSet\[@type="([^"]*)"]/cyclicNameContext\[@type="([^"]*)"]/cyclicNameWidth\[@type="([^"]*)"]/cyclicName\[@type="([^"]*)"]`

Provide the {2} and {3} version of type {4} in the {1} name cycle. For more information, please see [Date/time cyclic names].

###

- `^//ldml/dates/calendars/calendar\[@type="[^"]*"]/monthPatterns/monthPatternContext\[@type="([^"]*)"]/monthPatternWidth\[@type="([^"]*)"]/monthPattern\[@type="([^"]*)"]`

Provide the {1} and {2} version of the name for {3} month types. For more information, please see [Month Names].

###

- `^//ldml/localeDisplayNames/transformNames/transformName\[@type="([^"]*)"]`

The name of the transform “{1}”. For more information, please see [Transforms].

###

- `^//ldml/localeDisplayNames/codePatterns/codePattern[@type="([^"]*)"]`

The pattern to be used when displaying a name for a character {0}. For more information, please see [Locale Patterns].

###

- `^//ldml/localeDisplayNames/measurementSystemNames/measurementSystemName\[@type="([^"]*)"]`

The name of the measurement system “{1}”. For more information, please see [Units Misc Help].

###

- `^//ldml/posix/messages/(no|yes)str`

The word for “{1}”, lowercased, plus any abbreviations separated by a colon. For more information, see [Units Misc Help].

###

- `^//ldml/localeDisplayNames/annotationPatterns/annotationPattern[@type="([^"]*)"]`

The pattern to be used when displaying a {1}. For more information, please see [Locale Patterns].

###

- `^//ldml/characters/stopwords/stopwordList\[@type="([^"]*)"]`

The words that should be ignored in sorting in your language. For more information, see [Units Misc Help].

###

- `^//ldml/dates/timeZoneNames/zone\[@type="([^"]*)"]/([^/]*)/(.*)`

Override for the {3}-{2} timezone name for {1}. For more information, see [Time Zone City Names].

###

- `^//ldml/typographicNames/axisName[@type="([^"]*)"]`

A label for a typographic design axis, such as “Width” or “Weight”. For more information, see [Typography].

###

- `^//ldml/typographicNames/styleName[@type="([^"]*)"][@subtype="([^"]*)"]`

A label for a typographic style, such as “Narrow” or “Semibold”. For more information, see [Typography].

###

- `^//ldml/typographicNames/featureName[@type="([^"]*)"]`

A label for a typographic feature, such as “Small Capitals”. For more information, see [Typography].

###

- `^//ldml/characterLabels/characterLabelPattern\[@type="([^"]*)"]\[@count="([^"]*)"]`

A label for a set of characters that has a numeric placeholder, such as “1 Stroke”, “2 Strokes”. For more information, see [Character Labels].

###

- `^//ldml/characterLabels/characterLabelPattern\[@type="([^"]*)"]`

A modifier composed with a label for a set of characters. For more information, see [Character Labels].

###

- `^//ldml/characterLabels/characterLabel\[@type="([^"]*)"]`

A label for a set of characters. For more information, see [Character Labels].

###

- `^//ldml/annotations/annotation\[@cp="([^"]*)"]\[@type="tts"]`

A name for a character or sequence. For more information, see [Short Character Names].

###

- `^//ldml/annotations/annotation\[@cp="([^"]*)"]`

A set of keywords for a character or sequence. For more information, see [Short Character Names].

## References

<!--
This section is appended to every markdown fragment.
All links should be cldr.unicode.org/translation/
-->

[Changing Protected Items]: https://cldr.unicode.org/translation/getting-started/guide#change-protected-items
[Characters]: https://cldr.unicode.org/translation/characters
[Character Labels]: https://cldr.unicode.org/translation/characters/character-labels
[Compound Units]: https://cldr.unicode.org/translation/units/unit-names-and-patterns#compound-units
[Country Names]: https://cldr.unicode.org/translation/displaynames/countryregion-territory-names
[Currency Names]: https://cldr.unicode.org/translation/currency-names-and-symbols
[Date Time]: https://cldr.unicode.org/translation/date-time/date-time-names#datetime-names
[Date Time Names]: https://cldr.unicode.org/translation/date-time/date-time-names
[Date/time cyclic names]: https://cldr.unicode.org/translation/date-time/date-time-names#non-gregorian-calendar-considerations
[Date Time Fields]: https://cldr.unicode.org/translation/date-time/date-time-names#date-field-names
[Month Names]: https://cldr.unicode.org/translation/date-time/date-time-names#months-of-the-year
[Relative Dates]: https://cldr.unicode.org/translation/date-time/date-time-names#relative-date-and-time
[Date Time Patterns]: https://cldr.unicode.org/translation/date-time/date-time-patterns
[Exemplar Characters]: https://cldr.unicode.org/translation/core-data/exemplars
[Grammatical Inflection]: https://cldr.unicode.org/translation/grammatical-inflection
[Locale Option Names]: https://cldr.unicode.org/translation/displaynames/locale-option-names-key
[Language Names]: https://cldr.unicode.org/translation/displaynames/languagelocale-names
[Lists]: https://cldr.unicode.org/translation/miscellaneous-displaying-lists
[Locale Patterns]: https://cldr.unicode.org/translation/displaynames/languagelocale-name-patterns
[Numbering Systems]: https://cldr.unicode.org/translation/core-data/numbering-systems
[Numbers]: https://cldr.unicode.org/translation/currency-names-and-symbols
[Plural Numbers]: https://cldr.unicode.org/translation/number-currency-formats/number-and-currency-patterns#plural-forms-of-numbers
[Short Numbers]: https://cldr.unicode.org/translation/number-currency-formats/number-and-currency-patterns#compact-decimal-formatting
[Number Patterns]: https://cldr.unicode.org/translation/number-currency-formats/number-and-currency-patterns#types-of-number-patterns
[Rational Numbers]: https://cldr.unicode.org/translation/number-currency-formats/number-and-currency-patterns#rational-formatting
[Parse Lenient]: https://cldr.unicode.org/translation/core-data/characters#parse-parse-lenient
[Person Name Formats]: https://cldr.unicode.org/translation/miscellaneous-person-name-formats
[Plurals]: https://cldr.unicode.org/translation/getting-started/plurals
[Plural Minimal Pairs]: https://cldr.unicode.org/translation/getting-started/plurals#minimal-pairs
[Script Names]: https://cldr.unicode.org/translation/displaynames/script-names
[Short Character Names]: https://cldr.unicode.org/translation/characters/short-names-and-keywords#short-character-names
[Transforms]: https://cldr.unicode.org/translation/transforms
[Typography]: https://cldr.unicode.org/translation/characters/typographic-names
[Time Zone City Names]: https://cldr.unicode.org/translation/time-zones-and-city-names
[Units]: https://cldr.unicode.org/translation/units
[Units Misc Help]: https://cldr.unicode.org/translation/units
