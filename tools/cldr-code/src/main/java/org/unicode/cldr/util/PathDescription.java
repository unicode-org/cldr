package org.unicode.cldr.util;

import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.Output;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.util.PatternPlaceholders.PlaceholderInfo;

public class PathDescription {
    /** Remember to quote any [ character! */
    private static final String pathDescriptionString =
            ""
                    /*
                     * ROOT descriptions (using special placeholders). Must occur first.
                     */
                    + "^//ldml/localeDisplayNames/territories/territory\\[@type=\"(CD|DG|CG|003|021|ZA|018|FK|MK|MM|TW|HK|MO)\"]"
                    + RegexLookup.SEPARATOR
                    + "ROOT territory; Warning - the region {0} requires special attention! Note: before translating, be sure to read "
                    + CLDRURLS.COUNTRY_NAMES
                    + ".\n"
                    + "^//ldml/localeDisplayNames/scripts/script\\[@type=\"(Z[^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "ROOT script; The name of the script (writing system) with Unicode script code = {0}. Note: before translating, be sure to read "
                    + CLDRURLS.SCRIPT_NAMES
                    + ".\n"
                    + "^//ldml/dates/timeZoneNames/zone\\[@type=\"([^\"]*)\"]/exemplarCity"
                    + RegexLookup.SEPARATOR
                    + "ROOT timezone"
                    + RegexLookup.SEPARATOR
                    + "The name of {0}. For more information, see "
                    + CLDRURLS.TZ_CITY_NAMES
                    + ".\n"
                    + "^//ldml/localeDisplayNames/languages/language\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "ROOT language; The name of the language with Unicode language code = {0}. For more information, see "
                    + CLDRURLS.LANGUAGE_NAMES
                    + ".\n"
                    + "^//ldml/localeDisplayNames/scripts/script\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "ROOT script; The name of the script (writing system) with Unicode script code = {0}. For more information, see "
                    + CLDRURLS.SCRIPT_NAMES
                    + ".\n"
                    + "^//ldml/localeDisplayNames/territories/territory\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "ROOT territory; The name of the country or region with Unicode region code = {0}. For more information, see "
                    + CLDRURLS.COUNTRY_NAMES
                    + ".\n"
                    + "^//ldml/localeDisplayNames/subdivisions/subdivision\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "ROOT territory; The name of the country subdivision with Unicode subdivision code = {0}. For more information, see "
                    + CLDRURLS.COUNTRY_NAMES
                    + ".\n"
                    + "^//ldml/numbers/currencies/currency\\[@type=\"([^\"]*)\"]/symbol$"
                    + RegexLookup.SEPARATOR
                    + "ROOT currency; The symbol for the currency with the ISO currency code = {0}. For more information, see "
                    + CLDRURLS.CURRENCY_NAMES
                    + ".\n"
                    + "^//ldml/numbers/currencies/currency\\[@type=\"([^\"]*)\"]/symbol\\[@alt=\"narrow\"]"
                    + RegexLookup.SEPARATOR
                    + "ROOT currency; The NARROW form of the symbol used for the currency with the ISO currency code = {0}, when the known context is already enough to distinguish the symbol from other currencies that may use the same symbol. Normally, this does not need to be changed from the inherited value. For more information, see "
                    + CLDRURLS.CURRENCY_NAMES
                    + ".\n"
                    + "^//ldml/numbers/currencies/currency\\[@type=\"([^\"]*)\"]/symbol\\[@alt=\"([^\"]++)\"]"
                    + RegexLookup.SEPARATOR
                    + "ROOT currency; An alternative form of the symbol used for the currency with the ISO currency code = {0}.  Usually occurs shortly after a new currency symbol is introduced. For more information, see "
                    + CLDRURLS.CURRENCY_NAMES
                    + ".\n"
                    + "^//ldml/numbers/currencies/currency\\[@type=\"([^\"]*)\"]/displayName"
                    + RegexLookup.SEPARATOR
                    + "ROOT currency; The name of the currency with the ISO currency code = {0}. For more information, see "
                    + CLDRURLS.CURRENCY_NAMES
                    + ".\n"

                    /*
                     * Note: we change the metazones dynamically in code
                     */
                    + "^//ldml/dates/timeZoneNames/metazone\\[@type=\"([^\"]*)\"](.*)/(.*)"
                    + RegexLookup.SEPARATOR
                    + "ROOT metazone; The name of the timezone for “{0}”. Note: before translating, be sure to read "
                    + CLDRURLS.TZ_CITY_NAMES
                    + ".\n"

                    /*
                     * OTHER Descriptions
                     */
                    + "^//ldml/localeDisplayNames/types/type\\[@key=\"collation\"]\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "The name of “{1} collation” (sorting order). For more information, please see "
                    + CLDRURLS.KEY_NAMES
                    + ".\n"
                    + "^//ldml/localeDisplayNames/types/type\\[@key=\"numbers\"]\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "The name of “{1} number system”. For more information, please see "
                    + CLDRURLS.KEY_NAMES
                    + ".\n"
                    + "^//ldml/localeDisplayNames/types/type\\[@key=\"calendar\"]\\[@type=\"roc\"]"
                    + RegexLookup.SEPARATOR
                    + "The name of “roc calendar” (common names include “Minguo Calendar”, “Republic of China Calendar”, and “Republican Calendar”). For more information, please see "
                    + CLDRURLS.KEY_NAMES
                    + ".\n"
                    + "^//ldml/localeDisplayNames/types/type\\[@key=\"calendar\"]\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "The name of “{1} calendar”. For more information, please see "
                    + CLDRURLS.KEY_NAMES
                    + ".\n"
                    + "^//ldml/localeDisplayNames/types/type\\[@key=\"em\"]\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "The name of “emoji presentation style {1}”. For more information, please see "
                    + CLDRURLS.KEY_NAMES
                    + ".\n"
                    + "^//ldml/localeDisplayNames/types/type\\[@key=\"fw\"]\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "The name of “first day of the week is {1}”. For more information, please see "
                    + CLDRURLS.KEY_NAMES
                    + ".\n"
                    + "^//ldml/localeDisplayNames/types/type\\[@key=\"lb\"]\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "The name of “{1} line break style”. For more information, please see "
                    + CLDRURLS.KEY_NAMES
                    + ".\n"
                    + "^//ldml/localeDisplayNames/types/type\\[@key=\"([^\"]*)\"]\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "The name of the “{2} {1}”. For more information, please see "
                    + CLDRURLS.KEY_NAMES
                    + ".\n"
                    + "^//ldml/localeDisplayNames/keys/key\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "The name of the system for “{1}”. For more information, please see "
                    + CLDRURLS.KEY_NAMES
                    + ".\n"
                    + "^//ldml/localeDisplayNames/variants/variant[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "The name of the language variant with code {1}”. For more information, please see "
                    + CLDRURLS.LANGUAGE_NAMES
                    + ".\n"
                    + "^//ldml/characters/exemplarCharacters$"
                    + RegexLookup.SEPARATOR
                    + "Defines the set of characters used in your language. You may not edit or vote on this item at this time. Before filing any tickets to request changes, be sure to read "
                    + CLDRURLS.EXEMPLAR_CHARACTERS
                    + ".\n"
                    + "^//ldml/characters/exemplarCharacters\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Defines the set of characters used in your language for the “{1}” category.  You may not edit or vote on this item at this time. Before filing any tickets to request changes, be sure to read "
                    + CLDRURLS.EXEMPLAR_CHARACTERS
                    + ".\n"
                    + "^//ldml/characters/parseLenients"
                    + RegexLookup.SEPARATOR
                    + "Defines sets of characters that are treated as equivalent in parsing.  You may not edit or vote on this item at this time. Before filing any tickets to request changes, be sure to read "
                    + CLDRURLS.PARSE_LENIENT
                    + ".\n"
                    + "^//ldml/characters/ellipsis\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Supply the ellipsis pattern for when the {1} part of a string is omitted. Note: before translating, be sure to read "
                    + CLDRURLS.CHARACTERS_HELP
                    + ".\n"
                    + "^//ldml/characters/moreInformation"
                    + RegexLookup.SEPARATOR
                    + "The character or short string used to indicate that more information is available. Note: before translating, be sure to read "
                    + CLDRURLS.CHARACTERS_HELP
                    + ".\n"
                    + "^//ldml/delimiters/alternateQuotationEnd"
                    + RegexLookup.SEPARATOR
                    + "Supply the (alternate) ending quotation mark (the right mark except in BIDI languages). Note: before translating, be sure to read "
                    + CLDRURLS.CHARACTERS_HELP
                    + ".\n"
                    + "^//ldml/delimiters/alternateQuotationStart"
                    + RegexLookup.SEPARATOR
                    + "Supply the (alternate) starting quotation mark (the left mark except in BIDI languages). Note: before translating, be sure to read "
                    + CLDRURLS.CHARACTERS_HELP
                    + ".\n"
                    + "^//ldml/delimiters/quotationEnd"
                    + RegexLookup.SEPARATOR
                    + "Supply the ending quotation mark (the right mark except in BIDI languages). Note: before translating, be sure to read "
                    + CLDRURLS.CHARACTERS_HELP
                    + ".\n"
                    + "^//ldml/delimiters/quotationStart"
                    + RegexLookup.SEPARATOR
                    + "Supply the starting quotation mark (the left mark except in BIDI languages). Note: before translating, be sure to read "
                    + CLDRURLS.CHARACTERS_HELP
                    + ".\n"
                    + "^//ldml/localeDisplayNames/localeDisplayPattern/localePattern"
                    + RegexLookup.SEPARATOR
                    + "The pattern used to compose locale (language) names. Note: before translating, be sure to read "
                    + CLDRURLS.LOCALE_PATTERN
                    + ".\n"
                    + "^//ldml/localeDisplayNames/localeDisplayPattern/localeSeparator"
                    + RegexLookup.SEPARATOR
                    + "The separator used to compose modifiers in locale (language) names. Note: before translating, be sure to read "
                    + CLDRURLS.LOCALE_PATTERN
                    + ".\n"
                    + "^//ldml/localeDisplayNames/localeDisplayPattern/localeKeyTypePattern"
                    + RegexLookup.SEPARATOR
                    + "The pattern used to compose key-type values in locale (language) names. Note: before translating, be sure to read "
                    + CLDRURLS.LOCALE_PATTERN
                    + ".\n"
                    + "^//ldml/layout/orientation/characterOrder"
                    + RegexLookup.SEPARATOR
                    + "Specifies the horizontal direction of text in the language. Valid values are \"left-to-right\" or \"right-to-left\". For more information, see "
                    + CLDRURLS.UNITS_MISC_HELP
                    + ".\n"
                    + "^//ldml/layout/orientation/lineOrder"
                    + RegexLookup.SEPARATOR
                    + "Specifies the vertical direction of text in the language. Valid values are \"top-to-bottom\" or \"bottom-to-top\". For more information, see "
                    + CLDRURLS.UNITS_MISC_HELP
                    + ".\n"
                    + "^//ldml/numbers/symbols/(\\w++)"
                    + RegexLookup.SEPARATOR
                    + "The {1} symbol used in the localized form of numbers. Note: before translating, be sure to read "
                    + CLDRURLS.NUMBERS_HELP
                    + ".\n"
                    + "^//ldml/numbers/symbols\\[@numberSystem=\"([a-z]*)\"]/(\\w++)"
                    + RegexLookup.SEPARATOR
                    + "The {2} symbol used in the {1} numbering system. NOTE: especially for the decimal and grouping symbol, before translating, be sure to read "
                    + CLDRURLS.NUMBERS_HELP
                    + ".\n"
                    + "^//ldml/numbers/defaultNumberingSystem"
                    + RegexLookup.SEPARATOR
                    + "The default numbering system used in this locale. For more information, please see "
                    + CLDRURLS.NUMBERING_SYSTEMS
                    + ".\n"
                    + "^//ldml/numbers/minimumGroupingDigits"
                    + RegexLookup.SEPARATOR
                    + "The default minimum number of digits before a grouping separator used in this locale. For more information, please see "
                    + CLDRURLS.NUMBERING_SYSTEMS
                    + ".\n"
                    + "^//ldml/numbers/otherNumberingSystems/(\\w++)"
                    + RegexLookup.SEPARATOR
                    + "The {1} numbering system used in this locale. For more information, please see "
                    + CLDRURLS.NUMBERING_SYSTEMS
                    + ".\n"
                    + "^//ldml/dates/timeZoneNames/regionFormat\\[@type=\"standard\"]"
                    + RegexLookup.SEPARATOR
                    + "The pattern used to compose standard (winter) fallback time zone names, such as 'Germany Winter Time'. Note: before translating, be sure to read "
                    + CLDRURLS.TZ_CITY_NAMES
                    + ".\n"
                    + "^//ldml/dates/timeZoneNames/regionFormat\\[@type=\"daylight\"]"
                    + RegexLookup.SEPARATOR
                    + "The pattern used to compose daylight (summer) fallback time zone names, such as 'Germany Summer Time'. Note: before translating, be sure to read "
                    + CLDRURLS.TZ_CITY_NAMES
                    + ".\n"
                    + "^//ldml/dates/timeZoneNames/regionFormat"
                    + RegexLookup.SEPARATOR
                    + "The pattern used to compose generic fallback time zone names, such as 'Germany Time'. Note: before translating, be sure to read "
                    + CLDRURLS.TZ_CITY_NAMES
                    + ".\n"
                    + "^//ldml/dates/timeZoneNames/(fallback|fallbackRegion|gmtZero|gmt|hour|region)Format"
                    + RegexLookup.SEPARATOR
                    + "The {1} pattern used to compose time zone names. Note: before translating, be sure to read "
                    + CLDRURLS.TZ_CITY_NAMES
                    + ".\n"

                    /*
                     * Warning: the longer match must come first
                     */
                    + "^//ldml/units/unitLength\\[@type=\"([^\"]*)\"]/compoundUnit\\[@type=\"([^\"]*)\"]/compoundUnitPattern1"
                    + RegexLookup.SEPARATOR
                    + "Special pattern used to compose powers of a unit, such as meters squared. Note: before translating, be sure to read "
                    + CLDRURLS.UNITS_HELP
                    + ".\n"
                    + "^//ldml/units/unitLength\\[@type=\"([^\"]*)\"]/compoundUnit\\[@type=\"([^\"]*)\"]/compoundUnitPattern"
                    + RegexLookup.SEPARATOR
                    + "Special pattern used to compose forms of two units, such as meters per second. Note: before translating, be sure to read "
                    + CLDRURLS.UNITS_HELP
                    + ".\n"
                    + "^//ldml/units/unitLength\\[@type=\"([^\"]*)\"]/compoundUnit\\[@type=\"([^\"]*)\"]/unitPrefixPattern"
                    + RegexLookup.SEPARATOR
                    + "Special pattern used to compose a metric prefix with a unit, such as kilo{0} with meters to produce kilometers. Note: before translating, be sure to read "
                    + CLDRURLS.UNITS_HELP
                    + ".\n"
                    + "^//ldml/units/unitLength\\[@type=\"([^\"]*)\"]/coordinateUnit/displayName"
                    + RegexLookup.SEPARATOR
                    + "Display name ({1} form) for the type of direction used in latitude and longitude, such as north or east. Note: before translating, be sure to read "
                    + CLDRURLS.UNITS_HELP
                    + ".\n"
                    + "^//ldml/units/unitLength\\[@type=\"([^\"]*)\"]/coordinateUnit/coordinateUnitPattern\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Special pattern used in latitude and longitude, such as 12°N. Note: before translating, be sure to read "
                    + CLDRURLS.UNITS_HELP
                    + ".\n"
                    + "^//ldml/units/unitLength\\[@type=\"([^\"]*)\"]/unit\\[@type=\"area-acre\"]/displayName"
                    + RegexLookup.SEPARATOR
                    + "Display name ({1} form) for “area-acre”, referring specifically to an English acre. Note: before translating, be sure to read "
                    + CLDRURLS.UNITS_HELP
                    + ".\n"
                    + "^//ldml/units/unitLength\\[@type=\"([^\"]*)\"]/unit\\[@type=\"duration-day\"]/displayName"
                    + RegexLookup.SEPARATOR
                    + "Display name ({1} form) for “duration-day”, meaning a time duration of 24 hours (not a calendar day). Note: before translating, be sure to read "
                    + CLDRURLS.UNITS_HELP
                    + ".\n"
                    + "^//ldml/units/unitLength\\[@type=\"([^\"]*)\"]/unit\\[@type=\"energy-calorie\"]/displayName"
                    + RegexLookup.SEPARATOR
                    + "Display name ({1} form) for “energy-calorie”, calories as used in chemistry, not the same as food calorie. Note: before translating, be sure to read "
                    + CLDRURLS.UNITS_HELP
                    + ".\n"
                    + "^//ldml/units/unitLength\\[@type=\"([^\"]*)\"]/unit\\[@type=\"energy-foodcalorie\"]/displayName"
                    + RegexLookup.SEPARATOR
                    + "Display name ({1} form) for “energy-foodcalorie”, kilocalories for food energy; may have same translation as energy-kilocalorie. Note: before translating, be sure to read "
                    + CLDRURLS.UNITS_HELP
                    + ".\n"
                    + "^//ldml/units/unitLength\\[@type=\"([^\"]*)\"]/unit\\[@type=\"energy-kilocalorie\"]/displayName"
                    + RegexLookup.SEPARATOR
                    + "Display name ({1} form) for “energy-kilocalorie”, kilocalories for uses not specific to food energy, such as chemistry. Note: before translating, be sure to read "
                    + CLDRURLS.UNITS_HELP
                    + ".\n"
                    + "^//ldml/units/unitLength\\[@type=\"([^\"]*)\"]/unit\\[@type=\"graphics-em\"]/displayName"
                    + RegexLookup.SEPARATOR
                    + "Display name ({1} form) for “graphics-em”, referring to typographic length equal to a font’s point size. Note: before translating, be sure to read "
                    + CLDRURLS.UNITS_HELP
                    + ".\n"
                    + "^//ldml/units/unitLength\\[@type=\"([^\"]*)\"]/unit\\[@type=\"graphics-pixel\"]/displayName"
                    + RegexLookup.SEPARATOR
                    + "Display name ({1} form) for “graphics-pixel”, used for counting the individual elements in bitmap image. Note: before translating, be sure to read "
                    + CLDRURLS.UNITS_HELP
                    + ".\n"
                    + "^//ldml/units/unitLength\\[@type=\"([^\"]*)\"]/unit\\[@type=\"graphics-megapixel\"]/displayName"
                    + RegexLookup.SEPARATOR
                    + "Display name ({1} form) for “graphics-megapixel”, used for counting the individual elements in bitmap image. Note: before translating, be sure to read "
                    + CLDRURLS.UNITS_HELP
                    + ".\n"
                    + "^//ldml/units/unitLength\\[@type=\"([^\"]*)\"]/unit\\[@type=\"graphics-pixel-per-centimeter\"]/displayName"
                    + RegexLookup.SEPARATOR
                    + "Display name ({1} form) for “graphics-pixel-per-centimeter”, typically used for display resolution. Note: before translating, be sure to read "
                    + CLDRURLS.UNITS_HELP
                    + ".\n"
                    + "^//ldml/units/unitLength\\[@type=\"([^\"]*)\"]/unit\\[@type=\"graphics-pixel-per-inch\"]/displayName"
                    + RegexLookup.SEPARATOR
                    + "Display name ({1} form) for “graphics-pixel-per-inch”, typically used for display resolution. Note: before translating, be sure to read "
                    + CLDRURLS.UNITS_HELP
                    + ".\n"
                    + "^//ldml/units/unitLength\\[@type=\"([^\"]*)\"]/unit\\[@type=\"graphics-dot-per-centimeter\"]/displayName"
                    + RegexLookup.SEPARATOR
                    + "Display name ({1} form) for “graphics-dot-per-centimeter”, typically used for printer resolution. Note: before translating, be sure to read "
                    + CLDRURLS.UNITS_HELP
                    + ".\n"
                    + "^//ldml/units/unitLength\\[@type=\"([^\"]*)\"]/unit\\[@type=\"graphics-dot-per-inch\"]/displayName"
                    + RegexLookup.SEPARATOR
                    + "Display name ({1} form) for “graphics-dot-per-inch”, typically used for printer resolution. Note: before translating, be sure to read "
                    + CLDRURLS.UNITS_HELP
                    + ".\n"
                    + "^//ldml/units/unitLength\\[@type=\"([^\"]*)\"]/unit\\[@type=\"length-point\"]/displayName"
                    + RegexLookup.SEPARATOR
                    + "Display name ({1} form) for “length-point”, referring to a typographic point, 1/72 inch. Note: before translating, be sure to read "
                    + CLDRURLS.UNITS_HELP
                    + ".\n"
                    + "^//ldml/units/unitLength\\[@type=\"([^\"]*)\"]/unit\\[@type=\"mass-stone\"]/displayName"
                    + RegexLookup.SEPARATOR
                    + "Display name ({1} form) for “mass-stone”, used in UK/Ireland for body weight, equal to 14 pounds. Note: before translating, be sure to read "
                    + CLDRURLS.UNITS_HELP
                    + ".\n"
                    + "^//ldml/units/unitLength\\[@type=\"([^\"]*)\"]/unit\\[@type=\"mass-ton\"]/displayName"
                    + RegexLookup.SEPARATOR
                    + "Display name ({1} form) for “mass-ton”, meaning U.S. short ton, not U.K. long ton or metric ton. Note: before translating, be sure to read "
                    + CLDRURLS.UNITS_HELP
                    + ".\n"
                    + "^//ldml/units/unitLength\\[@type=\"([^\"]*)\"]/unit\\[@type=\"([^\"]*)\"]/displayName"
                    + RegexLookup.SEPARATOR
                    + "Display name ({1} form) for “{2}”. Note: before translating, be sure to read "
                    + CLDRURLS.UNITS_HELP
                    + ".\n"
                    + "^//ldml/units/unitLength\\[@type=\"([^\"]*)\"]/unit\\[@type=\"([^\"]*)\"]/unitPattern"
                    + RegexLookup.SEPARATOR
                    + "[ICU Syntax] Special pattern used to compose plural for {1} forms of “{2}”. Note: before translating, be sure to read "
                    + CLDRURLS.PLURALS_HELP
                    + ".\n"
                    + "^//ldml/units/unitLength\\[@type=\"([^\"]*)\"]/unit\\[@type=\"([^\"]*)\"]/perUnitPattern"
                    + RegexLookup.SEPARATOR
                    + "Special pattern ({1} form) used to compose values per unit, such as “meters per {2}”. Note: before translating, be sure to read "
                    + CLDRURLS.UNITS_HELP
                    + ".\n"
                    + "^//ldml/units/durationUnit\\[@type=\"(hms|hm|ms)\"]"
                    + RegexLookup.SEPARATOR
                    + "[ICU Syntax] Special pattern used to compose duration units. Note: before translating, be sure to read "
                    + CLDRURLS.PLURALS_HELP
                    + ".\n"
                    + "^//ldml/numbers/decimalFormats/decimalFormatLength\\[@type=\"([^\"]*)\"]/decimalFormat\\[@type=\"([^\"]*)\"]/pattern\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Special pattern used for a short version of numbers with the same number of digits as {3}. Note: before translating, be sure to read "
                    + CLDRURLS.NUMBERS_SHORT
                    + ".\n"
                    + "^//ldml/numbers/currencyFormats\\[@numberSystem=\"([^\"]*)\"]/currencyFormatLength\\[@type=\"short\"]/currencyFormat\\[@type=\"standard\"]/pattern\\[@type=\"(\\d+)\"]\\[@count=\"([^\"]+)\"]"
                    + RegexLookup.SEPARATOR
                    + "Special currency pattern used to obtain the abbreviated plural forms of numbers with the same number of digits as {2}. See "
                    + CLDRURLS.NUMBERS_SHORT
                    + " for details.\n"
                    + "^//ldml/numbers/decimalFormats\\[@numberSystem=\"([^\"]*)\"]/decimalFormatLength\\[@type=\"short\"]/decimalFormat\\[@type=\"standard\"]/pattern\\[@type=\"(\\d+)\"]\\[@count=\"([^\"]+)\"]"
                    + RegexLookup.SEPARATOR
                    + "Special decimal pattern used to obtain the abbreviated plural forms of numbers with the same number of digits as {2}. See "
                    + CLDRURLS.NUMBERS_SHORT
                    + " for details.\n"
                    + "^//ldml/numbers/decimalFormats\\[@numberSystem=\"([^\"]*)\"]/decimalFormatLength\\[@type=\"long\"]/decimalFormat\\[@type=\"standard\"]/pattern\\[@type=\"(\\d+)\"]\\[@count=\"([^\"]+)\"]"
                    + RegexLookup.SEPARATOR
                    + "Special decimal pattern used to obtain the long plural forms of numbers with the same number of digits as {2}. See "
                    + CLDRURLS.NUMBERS_PLURAL
                    + " for details.\n"
                    + "^//ldml/numbers/currencyFormats/currencyPatternAppendISO"
                    + RegexLookup.SEPARATOR
                    + "Pattern used to combine a regular currency format with an ISO 4217 code (¤¤). For more information, please see "
                    + CLDRURLS.NUMBER_PATTERNS
                    + ".\n"
                    + "^//ldml/numbers/currencyFormats\\[@numberSystem=\"([^\"]*)\"]/currencyPatternAppendISO"
                    + RegexLookup.SEPARATOR
                    + "Pattern used to combine a regular currency format with an ISO 4217 code (¤¤). For more information, please see "
                    + CLDRURLS.NUMBER_PATTERNS
                    + ".\n"
                    + "^//ldml/numbers/currencyFormats\\[@numberSystem=\"([^\"]*)\"]/unitPattern\\[@count=\"(\\w++)\"]"
                    + RegexLookup.SEPARATOR
                    + "Currency format used for numbers of type {2}. For more information, please see "
                    + CLDRURLS.NUMBER_PATTERNS
                    + ".\n"
                    + "^//ldml/numbers/miscPatterns\\[@numberSystem=\"([^\"]*)\"]/pattern\\[@type=\"range\"]"
                    + RegexLookup.SEPARATOR
                    + "Format used to indicate a range of numbers. The '{'0'}' and '{'1'}' in the pattern represent the lowest and highest numbers in the range, respectively. For more information, please see "
                    + CLDRURLS.UNITS_MISC_HELP
                    + ".\n"
                    + "^//ldml/numbers/miscPatterns\\[@numberSystem=\"([^\"]*)\"]/pattern\\[@type=\"atLeast\"]"
                    + RegexLookup.SEPARATOR
                    + "Format used to indicate a number is at least a certain value, often combined with other patterns to produce examples such as “≥12kg”. For more information, please see "
                    + CLDRURLS.UNITS_MISC_HELP
                    + ".\n"
                    + "^//ldml/numbers/miscPatterns\\[@numberSystem=\"([^\"]*)\"]/pattern\\[@type=\"atMost\"]"
                    + RegexLookup.SEPARATOR
                    + "Format used to indicate a number is at most a certain value, often combined with other patterns to produce examples such as “≤12kg”. For more information, please see "
                    + CLDRURLS.UNITS_MISC_HELP
                    + ".\n"
                    + "^//ldml/numbers/miscPatterns\\[@numberSystem=\"([^\"]*)\"]/pattern\\[@type=\"approximately\"]"
                    + RegexLookup.SEPARATOR
                    + "Format used to indicate a number is approximately a given value, often combined with other patterns to produce examples such as “~12kg”. For more information, please see "
                    + CLDRURLS.UNITS_MISC_HELP
                    + ".\n"
                    + "^//ldml/numbers/minimalPairs/ordinalMinimalPairs\\[@ordinal=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Minimal pairs for ordinals. For more information, please see "
                    + CLDRURLS.PLURALS_HELP_MINIMAL
                    + ".\n"
                    + "^//ldml/numbers/minimalPairs/pluralMinimalPairs\\[@count=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Minimal pairs for plurals (cardinals). For more information, please see "
                    + CLDRURLS.PLURALS_HELP_MINIMAL
                    + ".\n"
                    + "^//ldml/numbers/minimalPairs/caseMinimalPairs\\[@case=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Minimal pairs for cases used in the language. For more information, please see "
                    + CLDRURLS.GRAMMATICAL_INFLECTION
                    + ".\n"
                    + "^//ldml/numbers/minimalPairs/genderMinimalPairs\\[@gender=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Minimal pairs for genders. For more information, please see "
                    + CLDRURLS.GRAMMATICAL_INFLECTION
                    + ".\n"
                    + "^//ldml/personNames/nameOrderLocales\\[@order=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Person name order for locales. If there are none with a particular direction, insert ❮EMPTY❯. For more information, please see "
                    + CLDRURLS.PERSON_NAME_FORMATS
                    + ".\n"
                    + "^//ldml/personNames/parameterDefault\\[@parameter=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Person name default parameters. Make the appropriate formality and length settings for your locale. For more information, please see "
                    + CLDRURLS.PERSON_NAME_FORMATS
                    + ".\n"
                    + "^//ldml/personNames/foreignSpaceReplacement"
                    + RegexLookup.SEPARATOR
                    + "For foreign personal names displayed in your locale, any special character that replaces a space (defaults to regular space). If spaces are to be removed, insert ❮EMPTY❯. For more information, please see "
                    + CLDRURLS.PERSON_NAME_FORMATS
                    + ".\n"
                    + "^//ldml/personNames/nativeSpaceReplacement"
                    + RegexLookup.SEPARATOR
                    + "For native personal names displayed in your locale, should be ❮EMPTY❯ if your language doesn't use spaces between any name parts (such as Japanese), and otherwise a space. For more information, please see "
                    + CLDRURLS.PERSON_NAME_FORMATS
                    + ".\n"
                    + "^//ldml/personNames/initialPattern\\[@type=\"initial\"]"
                    + RegexLookup.SEPARATOR
                    + "The pattern used for a single initial in person name formats. For more information, please see "
                    + CLDRURLS.PERSON_NAME_FORMATS
                    + ".\n"
                    + "^//ldml/personNames/initialPattern\\[@type=\"initialSequence\"]"
                    + RegexLookup.SEPARATOR
                    + "The pattern used to compose sequences of initials in person name formats. For more information, please see "
                    + CLDRURLS.PERSON_NAME_FORMATS
                    + ".\n"
                    + "^//ldml/personNames/personName\\[@order=\"([^\"]*)\"]\\[@length=\"([^\"]*)\"]\\[@usage=\"referring\"]\\[@formality=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Person name formats for referring to a person (with a particular order, length, formality). For more information, please see "
                    + CLDRURLS.PERSON_NAME_FORMATS
                    + ".\n"
                    + "^//ldml/personNames/personName\\[@order=\"([^\"]*)\"]\\[@length=\"([^\"]*)\"]\\[@usage=\"addressing\"]\\[@formality=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Person name format for addressing a person (with a particular order, length, formality). For more information, please see "
                    + CLDRURLS.PERSON_NAME_FORMATS
                    + ".\n"
                    + "^//ldml/personNames/personName\\[@order=\"([^\"]*)\"]\\[@length=\"([^\"]*)\"]\\[@usage=\"monogram\"]\\[@formality=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Person name formats for monograms (with a particular order, length, formality). For more information, please see "
                    + CLDRURLS.PERSON_NAME_FORMATS
                    + ".\n"
                    + "^//ldml/personNames/sampleName"
                    + RegexLookup.SEPARATOR
                    + "Sample names for person name format examples (enter ∅∅∅ for optional unused fields). For more information, please see "
                    + CLDRURLS.PERSON_NAME_FORMATS
                    + ".\n"
                    + "^//ldml/numbers/([a-z]*)Formats(\\[@numberSystem=\"([^\"]*)\"])?/\\1FormatLength/\\1Format\\[@type=\"standard\"]/pattern\\[@type=\"standard\"]$"
                    + RegexLookup.SEPARATOR
                    + "Special pattern used to compose {1} numbers. Note: before translating, be sure to read "
                    + CLDRURLS.NUMBER_PATTERNS
                    + ".\n"
                    + "^//ldml/numbers/currencyFormats\\[@numberSystem=\"([^\"]*)\"]/currencyFormatLength/currencyFormat\\[@type=\"standard\"]/pattern\\[@type=\"standard\"]\\[@alt=\"alphaNextToNumber\"]"
                    + RegexLookup.SEPARATOR
                    + "Special pattern used to compose currency values when the currency symbol has a letter adjacent to the number. Note: before translating, be sure to read "
                    + CLDRURLS.NUMBER_PATTERNS
                    + ".\n"
                    + "^//ldml/numbers/currencyFormats\\[@numberSystem=\"([^\"]*)\"]/currencyFormatLength/currencyFormat\\[@type=\"standard\"]/pattern\\[@type=\"standard\"]\\[@alt=\"noCurrency\"]"
                    + RegexLookup.SEPARATOR
                    + "Special pattern used to compose currency values for which no currency symbol should be shown. Note: before translating, be sure to read "
                    + CLDRURLS.NUMBER_PATTERNS
                    + ".\n"
                    + "^//ldml/numbers/currencyFormats\\[@numberSystem=\"([^\"]*)\"]/currencyFormatLength/currencyFormat\\[@type=\"accounting\"]/pattern"
                    + RegexLookup.SEPARATOR
                    + "Special pattern used to compose currency values for accounting purposes. Note: before translating, be sure to read "
                    + CLDRURLS.NUMBER_PATTERNS
                    + ".\n"
                    + "^//ldml/numbers/currencyFormats/currencySpacing/([a-zA-Z]*)/([a-zA-Z]*)"
                    + RegexLookup.SEPARATOR
                    + "Special pattern used to compose currency signs ($1/$2) with numbers. Note: before translating, be sure to read "
                    + CLDRURLS.NUMBER_PATTERNS
                    + ".\n"
                    + "^//ldml/listPatterns/listPattern/listPatternPart\\[@type=\"2\"]"
                    + RegexLookup.SEPARATOR
                    + "Special pattern used to make an “and” list out of two standard elements. Note: before translating, be sure to read "
                    + CLDRURLS.LISTS_HELP
                    + ".\n"
                    + "^//ldml/listPatterns/listPattern/listPatternPart\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Special pattern used to make a “and” list out of more than two standard elements. This is used for the {1} portion of the list. Note: before translating, be sure to read "
                    + CLDRURLS.LISTS_HELP
                    + ".\n"
                    + "^//ldml/listPatterns/listPattern\\[@type=\"standard-short\"]/listPatternPart\\[@type=\"2\"]"
                    + RegexLookup.SEPARATOR
                    + "Special pattern used to make a short-style “and” list out of two standard elements. Note: before translating, be sure to read "
                    + CLDRURLS.LISTS_HELP
                    + ".\n"
                    + "^//ldml/listPatterns/listPattern\\[@type=\"standard-short\"]/listPatternPart\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Special pattern used to make a short-style “and” list out of more than two standard elements. This is used for the {1} portion of the list. Note: before translating, be sure to read "
                    + CLDRURLS.LISTS_HELP
                    + ".\n"
                    + "^//ldml/listPatterns/listPattern\\[@type=\"standard-narrow\"]/listPatternPart\\[@type=\"2\"]"
                    + RegexLookup.SEPARATOR
                    + "Special pattern used to make a short-style “and” list out of two standard elements. Note: before translating, be sure to read "
                    + CLDRURLS.LISTS_HELP
                    + ".\n"
                    + "^//ldml/listPatterns/listPattern\\[@type=\"standard-narrow\"]/listPatternPart\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Special pattern used to make a short-style “and” list out of more than two standard elements. This is used for the {1} portion of the list. Note: before translating, be sure to read "
                    + CLDRURLS.LISTS_HELP
                    + ".\n"
                    + "^//ldml/listPatterns/listPattern\\[@type=\"or\"]/listPatternPart\\[@type=\"2\"]"
                    + RegexLookup.SEPARATOR
                    + "Special pattern used to make an “or” list out of two standard elements. Note: before translating, be sure to read "
                    + CLDRURLS.LISTS_HELP
                    + ".\n"
                    + "^//ldml/listPatterns/listPattern\\[@type=\"or\"]/listPatternPart\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Special pattern used to make an “or” list out of more than two standard elements. This is used for the {1} portion of the list. Note: before translating, be sure to read "
                    + CLDRURLS.LISTS_HELP
                    + ".\n"
                    + "^//ldml/listPatterns/listPattern\\[@type=\"or-short\"]/listPatternPart\\[@type=\"2\"]"
                    + RegexLookup.SEPARATOR
                    + "Special pattern used to make an “or” list out of two standard elements. Note: before translating, be sure to read "
                    + CLDRURLS.LISTS_HELP
                    + ".\n"
                    + "^//ldml/listPatterns/listPattern\\[@type=\"or-short\"]/listPatternPart\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Special pattern used to make an “or” list out of more than two standard elements. This is used for the {1} portion of the list. Note: before translating, be sure to read "
                    + CLDRURLS.LISTS_HELP
                    + ".\n"
                    + "^//ldml/listPatterns/listPattern\\[@type=\"or-narrow\"]/listPatternPart\\[@type=\"2\"]"
                    + RegexLookup.SEPARATOR
                    + "Special pattern used to make an “or” list out of two standard elements. Note: before translating, be sure to read "
                    + CLDRURLS.LISTS_HELP
                    + ".\n"
                    + "^//ldml/listPatterns/listPattern\\[@type=\"or-narrow\"]/listPatternPart\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Special pattern used to make an “or” list out of more than two standard elements. This is used for the {1} portion of the list. Note: before translating, be sure to read "
                    + CLDRURLS.LISTS_HELP
                    + ".\n"
                    + "^//ldml/listPatterns/listPattern\\[@type=\"unit\"]/listPatternPart\\[@type=\"2\"]"
                    + RegexLookup.SEPARATOR
                    + "Special pattern used to make a list out of two unit elements. Note: before translating, be sure to read "
                    + CLDRURLS.LISTS_HELP
                    + ".\n"
                    + "^//ldml/listPatterns/listPattern\\[@type=\"unit\"]/listPatternPart\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Special pattern used to make a list out of more than two unit elements. This is used for the {1} portion of the list. Note: before translating, be sure to read "
                    + CLDRURLS.LISTS_HELP
                    + ".\n"
                    + "^//ldml/listPatterns/listPattern\\[@type=\"unit-short\"]/listPatternPart\\[@type=\"2\"]"
                    + RegexLookup.SEPARATOR
                    + "Special pattern used to make a list out of two abbreviated unit elements. Note: before translating, be sure to read "
                    + CLDRURLS.LISTS_HELP
                    + ".\n"
                    + "^//ldml/listPatterns/listPattern\\[@type=\"unit-short\"]/listPatternPart\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Special pattern used to make a list out of more than two abbreviated unit elements. This is used for the {1} portion of the list. Note: before translating, be sure to read "
                    + CLDRURLS.LISTS_HELP
                    + ".\n"
                    + "^//ldml/listPatterns/listPattern\\[@type=\"unit-narrow\"]/listPatternPart\\[@type=\"2\"]"
                    + RegexLookup.SEPARATOR
                    + "Special pattern used to make a list out of two narrow unit elements. Note: before translating, be sure to read "
                    + CLDRURLS.LISTS_HELP
                    + ".\n"
                    + "^//ldml/listPatterns/listPattern\\[@type=\"unit-narrow\"]/listPatternPart\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Special pattern used to make a list out of more than two narrow unit elements. This is used for the {1} portion of the list. Note: before translating, be sure to read "
                    + CLDRURLS.LISTS_HELP
                    + ".\n"
                    + "^//ldml/dates/calendars/calendar\\[@type=\"([^\"]*)\"]/dayPeriods/dayPeriodContext\\[@type=\"(format)\"]/dayPeriodWidth\\[@type=\"([^\"]*)\"]/dayPeriod\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Provide the {3}, {2} version of the name for the day period code “{4}”. This version must have the right inflection/prepositions/etc. for adding after a number, such as “in the morning” for use in “10:00 in the morning”. To see the time spans for these codes, please see "
                    + CLDRURLS.DATE_TIME_HELP
                    + "\n"
                    + "^//ldml/dates/calendars/calendar\\[@type=\"([^\"]*)\"]/dayPeriods/dayPeriodContext\\[@type=\"([^\"]*)\"]/dayPeriodWidth\\[@type=\"([^\"]*)\"]/dayPeriod\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Provide the {3}, {2} version of the name for the day period code “{4}”. To see the time spans for these codes, please see "
                    + CLDRURLS.DATE_TIME_HELP
                    + "\n"
                    + "^//ldml/dates/calendars/calendar\\[@type=\"([^\"]*)\"]/days/dayContext\\[@type=\"([^\"]*)\"]/dayWidth\\[@type=\"([^\"]*)\"]/day\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Provide the {2} and {3} version of the name for day-of-the-week {4}. For more information, please see "
                    + CLDRURLS.DATE_TIME_NAMES
                    + ".\n"
                    + "^//ldml/dates/calendars/calendar\\[@type=\"([^\"]*)\"]/eras/eraAbbr/era\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Provide the format-abbreviated version of the name for era {4}. For more information, please see "
                    + CLDRURLS.DATE_TIME_NAMES
                    + ".\n"
                    + "^//ldml/dates/calendars/calendar\\[@type=\"([^\"]*)\"]/eras/eraNames/era\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Provide the format-wide version of the name for era {4}. For more information, please see "
                    + CLDRURLS.DATE_TIME_NAMES
                    + ".\n"
                    + "^//ldml/dates/calendars/calendar\\[@type=\"([^\"]*)\"]/eras/eraNarrow/era\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Provide the format-narrow version of the name for era {4}. For more information, please see "
                    + CLDRURLS.DATE_TIME_NAMES
                    + ".\n"
                    + "^//ldml/dates/calendars/calendar\\[@type=\"([^\"]*)\"]/months/monthContext\\[@type=\"([^\"]*)\"]/monthWidth\\[@type=\"([^\"]*)\"]/month\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Provide the {2} and {3} version of the name for month {4}. For more information, please see "
                    + CLDRURLS.DATE_TIME_NAMES
                    + ".\n"
                    + "^//ldml/dates/calendars/calendar\\[@type=\"([^\"]*)\"]/quarters/quarterContext\\[@type=\"([^\"]*)\"]/quarterWidth\\[@type=\"([^\"]*)\"]/quarter\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Provide the {2} and {3} version of the name for quarter {4}. For more information, please see "
                    + CLDRURLS.DATE_TIME_NAMES
                    + ".\n"
                    + "^//ldml/dates/fields/field\\[@type=\"([^\"]*)\"]/displayName"
                    + RegexLookup.SEPARATOR
                    + "Provide the name (as it would appear in menus) for the field “{1}”. For more information, please see "
                    + CLDRURLS.DATE_TIME_NAMES_FIELD
                    + ".\n"
                    + "^//ldml/dates/fields/field\\[@type=\"day\"]/relative\\[@type=\"0\"]"
                    + RegexLookup.SEPARATOR
                    + "Provide the name for today. For more information, please see "
                    + CLDRURLS.DATE_TIME_NAMES_RELATIVE
                    + ".\n"
                    + "^//ldml/dates/fields/field\\[@type=\"day\"]/relative\\[@type=\"-([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Provide a name for the day, {1} before today. For more information, please see "
                    + CLDRURLS.DATE_TIME_NAMES_RELATIVE
                    + ".\n"
                    + "^//ldml/dates/fields/field\\[@type=\"day\"]/relative\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Provide a name for the day, {1} after today. For more information, please see "
                    + CLDRURLS.DATE_TIME_NAMES_RELATIVE
                    + ".\n"
                    + "^//ldml/dates/fields/field\\[@type=\"([^\"]*)\"]/relative\\[@type=\"0\"]"
                    + RegexLookup.SEPARATOR
                    + "Provide the name for “this {2}”. For more information, please see "
                    + CLDRURLS.DATE_TIME_NAMES_RELATIVE
                    + ".\n"
                    + "^//ldml/dates/fields/field\\[@type=\"([^\"]*)\"]/relative\\[@type=\"-1\"]"
                    + RegexLookup.SEPARATOR
                    + "Provide a name for “last {1}”. For more information, please see "
                    + CLDRURLS.DATE_TIME_NAMES_RELATIVE
                    + ".\n"
                    + "^//ldml/dates/fields/field\\[@type=\"([^\"]*)\"]/relative\\[@type=\"1\"]"
                    + RegexLookup.SEPARATOR
                    + "Provide a name for “next {1}”. For more information, please see "
                    + CLDRURLS.DATE_TIME_NAMES_RELATIVE
                    + ".\n"
                    + "^//ldml/dates/fields/field\\[@type=\"([^\"]*)\"]/relativeTime\\[@type=\"future\"]/relativeTimePattern\\[@count=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Provide a pattern used to display times in the future. For more information, please see "
                    + CLDRURLS.DATE_TIME_NAMES
                    + ".\n"
                    + "^//ldml/dates/fields/field\\[@type=\"([^\"]*)\"]/relativeTime\\[@type=\"past\"]/relativeTimePattern\\[@count=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Provide a pattern used to display times in the past. For more information, please see "
                    + CLDRURLS.DATE_TIME_NAMES
                    + ".\n"
                    + "^//ldml/dates/fields/field\\[@type=\"([^\"]*)\"]/relativePeriod"
                    + RegexLookup.SEPARATOR
                    + "Provide a name for “the {1} of SOME_DATE”. For more information, please see "
                    + CLDRURLS.DATE_TIME_NAMES
                    + ".\n"
                    + "^//ldml/dates/calendars/calendar\\[@type=\"([^\"]*)\"]/dateTimeFormats/dateTimeFormatLength\\[@type=\"([^\"]*)\"]/dateTimeFormat\\[@type=\"standard\"]/pattern\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Provide the {2} version of the date-time pattern suitable for most use cases, including combining a date with a time range. Note: before translating, be sure to read "
                    + CLDRURLS.DATE_TIME_PATTERNS
                    + ".\n"
                    + "^//ldml/dates/calendars/calendar\\[@type=\"([^\"]*)\"]/dateTimeFormats/dateTimeFormatLength\\[@type=\"([^\"]*)\"]/dateTimeFormat\\[@type=\"atTime\"]/pattern\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Provide the {2} version of the date-time pattern suitable for expressing a date at a specific time. Note: before translating, be sure to read "
                    + CLDRURLS.DATE_TIME_PATTERNS
                    + ".\n"
                    + "^//ldml/dates/calendars/calendar\\[@type=\"([^\"]*)\"]/dateFormats/dateFormatLength\\[@type=\"([^\"]*)\"]/dateFormat\\[@type=\"([^\"]*)\"]/pattern\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Provide the {2} version of the basic date pattern. Note: before translating, be sure to read "
                    + CLDRURLS.DATE_TIME_PATTERNS
                    + ".\n"
                    + "^//ldml/dates/calendars/calendar\\[@type=\"([^\"]*)\"]/timeFormats/timeFormatLength\\[@type=\"([^\"]*)\"]/timeFormat\\[@type=\"([^\"]*)\"]/pattern\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Provide the {2} version of the basic time pattern. Note: before translating, be sure to read "
                    + CLDRURLS.DATE_TIME_PATTERNS
                    + ".\n"
                    + "^//ldml/dates/calendars/calendar\\[@type=\"([^\"]*)\"]/dateTimeFormats/availableFormats/dateFormatItem\\[@id=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Provide the pattern used in your language for the skeleton “{2}”. Note: before translating, be sure to read "
                    + CLDRURLS.DATE_TIME_PATTERNS
                    + ".\n"
                    + "^//ldml/dates/calendars/calendar\\[@type=\"([^\"]*)\"]/dateTimeFormats/appendItems/appendItem\\[@request=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Provide the pattern used in your language to append a “{2}” to another format. Note: before translating, be sure to read "
                    + CLDRURLS.DATE_TIME_PATTERNS
                    + ".\n"
                    + "^//ldml/dates/calendars/calendar\\[@type=\"([^\"]*)\"]/dateTimeFormats/intervalFormats/intervalFormatFallback"
                    + RegexLookup.SEPARATOR
                    + "The pattern used for “fallback” with date/time intervals. Note: before translating, be sure to read "
                    + CLDRURLS.DATE_TIME_PATTERNS
                    + ".\n"
                    + "^//ldml/dates/calendars/calendar\\[@type=\"([^\"]*)\"]/dateTimeFormats/intervalFormats/intervalFormatItem\\[@id=\"([^\"]*)\"]/greatestDifference\\[@id=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "The pattern used for the date/time interval skeleton “{2}” when the greatest difference is “{3}”. Note: before translating, be sure to read "
                    + CLDRURLS.DATE_TIME_PATTERNS
                    + ".\n"
                    + "^//ldml/dates/calendars/calendar\\[@type=\"[^\"]*\"]/cyclicNameSets/cyclicNameSet\\[@type=\"([^\"]*)\"]/cyclicNameContext\\[@type=\"([^\"]*)\"]/cyclicNameWidth\\[@type=\"([^\"]*)\"]/cyclicName\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Provide the {2} and {3} version of type {4} in the {1} name cycle. For more information, please see "
                    + CLDRURLS.DATE_TIME_NAMES_CYCLIC
                    + ".\n"
                    + "^//ldml/dates/calendars/calendar\\[@type=\"[^\"]*\"]/monthPatterns/monthPatternContext\\[@type=\"([^\"]*)\"]/monthPatternWidth\\[@type=\"([^\"]*)\"]/monthPattern\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "Provide the {1} and {2} version of the name for {3} month types. For more information, please see "
                    + CLDRURLS.DATE_TIME_NAMES_MONTH
                    + ".\n"
                    + "^//ldml/localeDisplayNames/transformNames/transformName\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "The name of the transform “{1}”. For more information, please see "
                    + CLDRURLS.TRANSFORMS_HELP
                    + ".\n"
                    + "^//ldml/localeDisplayNames/codePatterns/codePattern[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "The pattern to be used when displaying a name for a character {1}. For more information, please see "
                    + CLDRURLS.LOCALE_PATTERN
                    + ".\n"
                    + "^//ldml/localeDisplayNames/measurementSystemNames/measurementSystemName\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "The name of the measurement system “{1}”.  For more information, please see "
                    + CLDRURLS.UNITS_MISC_HELP
                    + ".\n"
                    + "^//ldml/posix/messages/(no|yes)str"
                    + RegexLookup.SEPARATOR
                    + "The word for “{1}”, lowercased, plus any abbreviations separated by a colon. For more information, see "
                    + CLDRURLS.UNITS_MISC_HELP
                    + ".\n"
                    + "^//ldml/localeDisplayNames/annotationPatterns/annotationPattern[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "The pattern to be used when displaying a {1}. For more information, please see "
                    + CLDRURLS.LOCALE_PATTERN
                    + ".\n"
                    + "^//ldml/characters/stopwords/stopwordList\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "The words that should be ignored in sorting in your language.  For more information, see "
                    + CLDRURLS.UNITS_MISC_HELP
                    + ".\n"
                    + "^//ldml/dates/timeZoneNames/zone\\[@type=\"([^\"]*)\"]/([^/]*)/(.*)"
                    + RegexLookup.SEPARATOR
                    + "Override for the $3-$2 timezone name for $1.  For more information, see "
                    + CLDRURLS.TZ_CITY_NAMES
                    + ".\n"
                    + "^//ldml/typographicNames/axisName[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "A label for a typographic design axis, such as “Width” or “Weight”.  For more information, see "
                    + CLDRURLS.TYPOGRAPHIC_NAMES
                    + ".\n"
                    + "^//ldml/typographicNames/styleName[@type=\"([^\"]*)\"][@subtype=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "A label for a typographic style, such as “Narrow” or “Semibold”.  For more information, see "
                    + CLDRURLS.TYPOGRAPHIC_NAMES
                    + ".\n"
                    + "^//ldml/typographicNames/featureName[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "A label for a typographic feature, such as “Small Capitals”.  For more information, see "
                    + CLDRURLS.TYPOGRAPHIC_NAMES
                    + ".\n"
                    + "^//ldml/characterLabels/characterLabelPattern\\[@type=\"([^\"]*)\"]\\[@count=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "A label for a set of characters that has a numeric placeholder, such as “1 Stroke”, “2 Strokes”.  For more information, see "
                    + CLDRURLS.CHARACTER_LABELS
                    + ".\n"
                    + "^//ldml/characterLabels/characterLabelPattern\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "A modifier composed with a label for a set of characters.  For more information, see "
                    + CLDRURLS.CHARACTER_LABELS
                    + ".\n"
                    + "^//ldml/characterLabels/characterLabel\\[@type=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "A label for a set of characters.  For more information, see "
                    + CLDRURLS.CHARACTER_LABELS
                    + ".\n"
                    + "^//ldml/annotations/annotation\\[@cp=\"([^\"]*)\"]\\[@type=\"tts\"]"
                    + RegexLookup.SEPARATOR
                    + "A name for a character or sequence. For more information, see "
                    + CLDRURLS.SHORT_CHARACTER_NAMES
                    + ".\n"
                    + "^//ldml/annotations/annotation\\[@cp=\"([^\"]*)\"]"
                    + RegexLookup.SEPARATOR
                    + "A set of keywords for a character or sequence.  For more information, see "
                    + CLDRURLS.SHORT_CHARACTER_NAMES
                    + ".\n";

    private static final Logger logger = Logger.getLogger(PathDescription.class.getName());

    public enum ErrorHandling {
        SKIP,
        CONTINUE
    }

    public static final Set<String> EXTRA_LANGUAGES =
            new TreeSet<>(
                    Arrays.asList(
                            "ach|af|ak|ak|am|ar|az|be|bem|bg|bh|bn|br|bs|ca|chr|ckb|co|crs|cs|cy|da|de|de_AT|de_CH|ee|el|en|en_AU|en_CA|en_GB|en_US|eo|es|es_419|es_ES|et|eu|fa|fi|fil|fo|fr|fr_CA|fr_CH|fy|ga|gaa|gd|gl|gn|gsw|gu|ha|haw|he|hi|hr|ht|hu|hy|ia|id|ig|io|is|it|ja|jv|ka|kg|kk|km|kn|ko|kri|ku|ky|la|lg|ln|lo|loz|lt|lua|lv|mfe|mg|mi|mk|ml|mn|mr|ms|mt|my|nb|ne|nl|nl_BE|nn|no|nso|ny|nyn|oc|om|or|pa|pcm|pl|ps|pt|pt_BR|pt_PT|qu|rm|rn|ro|ro|ro_MD|ru|rw|sd|si|sk|sl|sn|so|sq|sr|sr_Latn|sr_ME|st|su|sv|sw|ta|te|tg|th|ti|tk|tlh|tn|to|tr|tt|tum|ug|uk|und|ur|uz|vi|wo|xh|yi|yo|zh|zh_Hans|zh_Hant|zh_HK|zu|zxx"
                                    .split("\\|")));

    private static final Pattern METAZONE_PATTERN =
            Pattern.compile("//ldml/dates/timeZoneNames/metazone\\[@type=\"([^\"]*)\"]/(.*)/(.*)");
    private static final Pattern STAR_ATTRIBUTE_PATTERN = PatternCache.get("=\"([^\"]*)\"");

    private static final StandardCodes STANDARD_CODES = StandardCodes.make();
    private static final Map<String, String> ZONE2COUNTRY =
            STANDARD_CODES.zoneParser.getZoneToCounty();

    private static final RegexLookup<String> pathHandling =
            new RegexLookup<String>().loadFromString(pathDescriptionString);

    // set in construction

    private final CLDRFile english;
    private final Map<String, String> extras;
    private final ErrorHandling errorHandling;
    private final Map<String, List<Set<String>>> starredPaths;
    private final Set<String> allMetazones;

    // used on instance

    private final Matcher metazoneMatcher = METAZONE_PATTERN.matcher("");
    private String starredPathOutput;
    private final Output<String[]> pathArguments = new Output<>();
    private final EnumSet<Status> status = EnumSet.noneOf(Status.class);

    public static final String MISSING_DESCRIPTION =
            "Before translating, please see " + CLDRURLS.GENERAL_HELP_URL + ".";

    public PathDescription(
            SupplementalDataInfo supplementalDataInfo,
            CLDRFile english,
            Map<String, String> extras,
            Map<String, List<Set<String>>> starredPaths,
            ErrorHandling errorHandling) {
        this.english = english;
        this.extras = extras == null ? new HashMap<>() : extras;
        this.starredPaths = starredPaths == null ? new HashMap<>() : starredPaths;
        allMetazones = supplementalDataInfo.getAllMetazones();
        this.errorHandling = errorHandling;
    }

    public String getStarredPathOutput() {
        return starredPathOutput;
    }

    public EnumSet<Status> getStatus() {
        return status;
    }

    public enum Status {
        SKIP,
        NULL_VALUE,
        EMPTY_CONTENT,
        NOT_REQUIRED
    }

    public String getRawDescription(String path, Object context) {
        status.clear();
        return pathHandling.get(path, context, pathArguments);
    }

    public String getDescription(String path, String value, Object context) {
        status.clear();

        String description = pathHandling.get(path, context, pathArguments);
        if (description == null) {
            description = MISSING_DESCRIPTION;
        } else if ("SKIP".equals(description)) {
            status.add(Status.SKIP);
            if (errorHandling == ErrorHandling.SKIP) {
                return null;
            }
        }
        if (value == null) { // a count item?
            String xpath = extras.get(path);
            if (xpath != null) {
                value = english.getStringValue(xpath);
            } else if (path.contains("/metazone")) {
                if (metazoneMatcher.reset(path).matches()) {
                    String name = metazoneMatcher.group(1);
                    String type = metazoneMatcher.group(3);
                    value =
                            name.replace('_', ' ')
                                    + (type.equals("generic")
                                            ? ""
                                            : type.equals("daylight") ? " Summer" : " Winter")
                                    + " Time";
                }
            }
            if (value == null) {
                status.add(Status.NULL_VALUE);
                if (errorHandling == ErrorHandling.SKIP) {
                    return null;
                }
            }
        }
        if (value != null && value.length() == 0) {
            status.add(Status.EMPTY_CONTENT);
            if (errorHandling == ErrorHandling.SKIP) {
                return null;
            }
        }

        List<String> attributes = addStarredInfo(starredPaths, path);

        // In special cases, only use if there is a root value (languageNames, ...
        if (description.startsWith("ROOT")) {
            int typeEnd = description.indexOf(';');
            String type = description.substring(4, typeEnd).trim();
            description = description.substring(typeEnd + 1).trim();

            boolean isMetazone = type.equals("metazone");
            String code = attributes.get(0);
            boolean isRootCode = isRootCode(code, allMetazones, type, isMetazone);
            if (!isRootCode) {
                status.add(Status.NOT_REQUIRED);
                if (errorHandling == ErrorHandling.SKIP) {
                    return null;
                }
            }
            if (isMetazone) {
                XPathParts parts = XPathParts.getFrozenInstance(path);
                String daylightType = parts.getElement(-1);
                daylightType =
                        daylightType.equals("daylight")
                                ? "summer"
                                : daylightType.equals("standard") ? "winter" : daylightType;
                String length = parts.getElement(-2);
                length = length.equals("long") ? "" : "abbreviated ";
                code = code + ", " + length + daylightType + " form";
            } else if (type.equals("timezone")) {
                String country = ZONE2COUNTRY.get(code);
                int lastSlash = code.lastIndexOf('/');
                String codeName =
                        lastSlash < 0 ? code : code.substring(lastSlash + 1).replace('_', ' ');

                boolean found = false;
                if ("001".equals(country)) {
                    code = "the timezone “" + codeName + "”";
                    found = true;
                } else if (country != null) {
                    String countryName = english.getName("territory", country);
                    if (countryName != null) {
                        if (!codeName.equals(countryName)) {
                            code = "the city “" + codeName + "” (in " + countryName + ")";
                        } else {
                            code = "the country “" + codeName + "”";
                        }
                        found = true;
                    }
                }
                if (!found) {
                    logger.warning("Missing country for timezone " + code);
                }
            }
            description =
                    MessageFormat.format(MessageFormat.autoQuoteApostrophe(description), code);
        } else if (path.contains("exemplarCity")) {
            String regionCode = ZONE2COUNTRY.get(attributes.get(0));
            String englishRegionName = english.getName(CLDRFile.TERRITORY_NAME, regionCode);
            description =
                    MessageFormat.format(
                            MessageFormat.autoQuoteApostrophe(description), englishRegionName);
        } else if (!MISSING_DESCRIPTION.equals(description)) {
            description =
                    MessageFormat.format(
                            MessageFormat.autoQuoteApostrophe(description),
                            (Object[]) pathArguments.value);
        }

        return description;
    }

    /**
     * Creates an escaped HTML string of placeholder information.
     *
     * @param path the xpath to specify placeholder information for
     * @return a HTML string, or an empty string if there was no placeholder information
     */
    public String getPlaceholderDescription(String path) {
        Map<String, PlaceholderInfo> placeholders = PatternPlaceholders.getInstance().get(path);
        if (placeholders != null && placeholders.size() > 0) {
            StringBuilder buffer = new StringBuilder();
            buffer.append("<table>");
            buffer.append("<tr><th>Placeholder</th><th>Meaning</th><th>Example</th></tr>");
            for (Entry<String, PlaceholderInfo> entry : placeholders.entrySet()) {
                PlaceholderInfo info = entry.getValue();
                buffer.append("<tr>");
                buffer.append("<td>").append(entry.getKey()).append("</td>");
                buffer.append("<td>").append(info.name).append("</td>");
                buffer.append("<td>").append(info.example).append("</td>");
                buffer.append("</tr>");
            }
            buffer.append("</table>");
            return buffer.toString();
        }
        return "";
    }

    private static boolean isRootCode(
            String code, Set<String> allMetazones, String type, boolean isMetazone) {
        Set<String> codes =
                isMetazone
                        ? allMetazones
                        : type.equals("timezone")
                                ? STANDARD_CODES.zoneParser.getZoneData().keySet()
                                : STANDARD_CODES.getSurveyToolDisplayCodes(type);
        // end
        boolean isRootCode = codes.contains(code) || code.contains("_");
        if (!isRootCode && type.equals("language") && EXTRA_LANGUAGES.contains(code)) {
            isRootCode = true;
        }
        return isRootCode;
    }

    private List<String> addStarredInfo(Map<String, List<Set<String>>> starredPaths, String path) {
        Matcher starAttributeMatcher = STAR_ATTRIBUTE_PATTERN.matcher(path);
        StringBuilder starredPath = new StringBuilder();
        List<String> attributes = new ArrayList<>();
        int lastEnd = 0;
        while (starAttributeMatcher.find()) {
            int start = starAttributeMatcher.start(1);
            int end = starAttributeMatcher.end(1);
            starredPath.append(path, lastEnd, start);
            starredPath.append(".*");

            attributes.add(path.substring(start, end));
            lastEnd = end;
        }
        starredPath.append(path.substring(lastEnd));
        String starredPathString = starredPath.toString().intern();
        starredPathOutput = starredPathString;

        List<Set<String>> attributeList =
                starredPaths.computeIfAbsent(starredPathString, k -> new ArrayList<>());
        int i = 0;
        for (String attribute : attributes) {
            if (attributeList.size() <= i) {
                TreeSet<String> subset = new TreeSet<>();
                subset.add(attribute);
                attributeList.add(subset);
            } else {
                Set<String> subset = attributeList.get(i);
                subset.add(attribute);
            }
            ++i;
        }
        return attributes;
    }
}
