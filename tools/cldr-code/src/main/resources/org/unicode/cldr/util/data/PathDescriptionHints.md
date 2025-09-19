# Data for generating "hint" descriptions of paths in Survey Tool

See PathDescriptions.md for documentation.

# VARIABLES
%anyAttribute = ([^"]*)

## HINT descriptions

### HINT

- `units/unitLength\[@type="%unitLengths"\]/unit\[@type="duration-day"\](/displayName|/unitPattern\[@count="%anyAttribute"\])`

"day" here means a time duration of 24 hours, not a calendar day

###

- `localeDisplayNames/languages/language\[@type="ckb"\]\[@alt="menu"\]`

a form of the name that will sort next to Kurdish, if the standard name does not already do that

###

- `localeDisplayNames/types/type\[@key="calendar"\]\[@type="roc"\]`

also called "Republic of China Calendar", "Republican Calendar"

###

- `localeDisplayNames/languages/language\[@type="ckb"\]\[@alt="variant"\]`

an alternate form using Sorani or Central, whichever is not used by the standard name

###

- `units/unitLength\[@type="%unitLengths"\]/unit\[@type="energy-calorie"\]/(displayName|unitPattern\[@count="%anyAttribute"\])`

calories as used in chemistry, not the same as food calorie

###

- `units/unitLength\[@type="(narrow|short)"\]/unit\[@type="energy-foodcalorie"\]/(displayName|unitPattern\[@count="%anyAttribute"\])`

kilocalories for food energy, may have same translation as energy-kilocalorie; displayed as Cal in the US/UK

###

- `units/unitLength\[@type="long"\]/unit\[@type="energy-foodcalorie"\]/(displayName|unitPattern\[@count="%anyAttribute"\])`

kilocalories for food energy, may have same translation as energy-kilocalorie; displayed as Calories in the US/UK

###

- `units/unitLength\[@type="%unitLengths"\]/unit\[@type="energy-kilocalorie"\]/(displayName|unitPattern\[@count="%anyAttribute"\])`

kilocalories for uses not specific to food energy, such as chemistry

###

- `units/unitLength\[@type="%unitLengths"\]/unit\[@type="area-acre"\]/(displayName|unitPattern\[@count="%anyAttribute"\])`

refers specifically to an English acre

###

- `units/unitLength\[@type="%unitLengths"\]/unit\[@type="mass-ton"\]/(displayName|unitPattern\[@count="%anyAttribute"\])`

refers to U.S. short ton, not U.K. long ton or metric ton

###

- `localeDisplayNames/scripts/script\[@type="Aran"\]`

special code identifies a style variant of Arabic script

###

- `localeDisplayNames/scripts/script\[@type="Qaag"\]`

special code identifies non-standard Myanmar encoding for use with Zawgyi font

###

- `localeDisplayNames/languages/language\[@type="clc"\]`

also referred to as "Tsilhqot’in"

###

- `localeDisplayNames/languages/language\[@type="ikt"\]`

also referred to as "Inuinnaqtun"

###

- `localeDisplayNames/languages/language\[@type="kwk"\]`

previously referred to as "Kwakiutl"

###

- `localeDisplayNames/languages/language\[@type="moe"\]`

also referred to as "Montagnais"

###

- `localeDisplayNames/languages/language\[@type="ojs"\]`

also referred to as "Severn Ojibwa"

###

- `localeDisplayNames/languages/language\[@type="oka"\]`

also referred to as "Nsyilxcən"

###

- `localeDisplayNames/languages/language\[@type="pqm"\]`

also referred to as "Maliseet–Passamaquoddy"

###

- `localeDisplayNames/languages/language\[@type="slh"\]`

also referred to as "Southern Puget Sound Salish"

###

- `localeDisplayNames/languages/language\[@type="zh"\]`

specifically, Mandarin Chinese

###

- `localeDisplayNames/scripts/script\[@type="Han(s|t)"\]`

this version of the script name is used in combination with the language name for Chinese

###

- `localeDisplayNames/scripts/script\[@type="Han(s|t)"\]\[@alt="stand-alone"\]`

this version of the script name is used in isolation, not combined with the language name for Chinese

###

- `dates/timeZoneNames/metazone\[@type="America_Central"\]/long/daylight`

translate as "North American Central Daylight Time"

###

- `dates/timeZoneNames/metazone\[@type="America_Central"\]/long/standard`

translate as "North American Central Standard Time"

###

- `dates/timeZoneNames/metazone\[@type="America_Central"\]/long/generic`

translate as "North American Central Time"

###

- `dates/timeZoneNames/metazone\[@type="America_Eastern"\]/long/daylight`

translate as "North American Eastern Daylight Time"

###

- `dates/timeZoneNames/metazone\[@type="America_Eastern"\]/long/standard`

translate as "North American Eastern Standard Time"

###

- `dates/timeZoneNames/metazone\[@type="America_Eastern"\]/long/generic`

translate as "North American Eastern Time"

###

- `dates/timeZoneNames/metazone\[@type="America_Mountain"\]/long/daylight`

translate as "North American Mountain Daylight Time"

###

- `dates/timeZoneNames/metazone\[@type="America_Mountain"\]/long/standard`

translate as "North American Mountain Standard Time"

###

- `dates/timeZoneNames/metazone\[@type="America_Mountain"\]/long/generic`

translate as "North American Mountain Time"

###

- `dates/timeZoneNames/metazone\[@type="America_Pacific"\]/long/daylight`

translate as "North American Pacific Daylight Time"

###

- `dates/timeZoneNames/metazone\[@type="America_Pacific"\]/long/standard`

translate as "North American Pacific Standard Time"

###

- `dates/timeZoneNames/metazone\[@type="America_Pacific"\]/long/generic`

translate as "North American Pacific Time"

###

- `dates/timeZoneNames/metazone\[@type="Chamorro"\]/long/standard`

translate as just "Chamorro Time"

###

- `dates/timeZoneNames/metazone\[@type="Guam"\]/long/standard`

translate as just "Guam Time"

###

- `dates/timeZoneNames/metazone\[@type="Gulf"\]/long/standard`

translate as just "Gulf Time"

###

- `dates/timeZoneNames/metazone\[@type="India"\]/long/standard`

translate as just "India Time"

###

- `dates/timeZoneNames/metazone\[@type="Singapore"\]/long/standard`

translate as just "Singapore Time"

###

- `dates/timeZoneNames/metazone\[@type="Africa_Southern"\]/long/standard`

translate as just "South Africa Time"

###

- `units/unitLength\[@type="%unitLengths"\]/unit\[@type="graphics-pixel-per-(centimeter|inch)"\]/(displayName|unitPattern\[@count="%anyAttribute"\])`

typically used for display resolution

###

- `units/unitLength\[@type="%unitLengths"\]/unit\[@type="graphics-dot-per-(centimeter|inch)"\]/(displayName|unitPattern\[@count="%anyAttribute"\])`

typically used for printer resolution

###

- `units/unitLength\[@type="%unitLengths"\]/unit\[@type="graphics-em"\]/(displayName|unitPattern\[@count="%anyAttribute"\])`

typographic length equal to a font’s point size

###

- `units/unitLength\[@type="%unitLengths"\]/unit\[@type="graphics-megapixel"\]/(displayName|unitPattern\[@count="%anyAttribute"\])`

used for counting the individual elements in bitmap image

###

- `units/unitLength\[@type="%unitLengths"\]/unit\[@type="graphics-pixel"\]/(displayName|unitPattern\[@count="%anyAttribute"\])`

used for counting the individual elements in bitmap image; in some contexts means 1⁄96 inch

###

- `units/unitLength\[@type="%unitLengths"\]/unit\[@type="mass-stone"\]/(displayName|unitPattern\[@count="%anyAttribute"\])`

used in UK/Ireland for body weight, equal to 14 pounds

###

- `localeDisplayNames/territories/territory\[@type="(003|018|021|057|CD|CG|CI|FK|FM|HK|MM|MO|PS|SZ|TL|ZA)"\](\[@alt="(variant|short)"\])?`

warning, see info panel on right

## References

<!--
This section is appended to every markdown fragment.
All links should be cldr.unicode.org/translation/
Currently, for hints, there are no actual reference links yet, but a "Placeholder" reference link is included for testing. 
-->
[Placeholder]: https://cldr.unicode.org/translation/units
