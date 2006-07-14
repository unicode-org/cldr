/************************************************************************
* Copyright (c) 2005, Sun Microsystems and others.  All Rights Reserved.
*************************************************************************/

package org.unicode.cldr.ooo;

/**
 * class defines constants for OpenOffice's locale.dtd elements and attributes
 */
public class OOConstants
{
    //for geenrating templates
    public static final String NO_DATA             = "<NA>";  //use for writing template OO.o locales from CLDR
    public static final String NO_DATA_Q           = "<NA?>";
    public static final String NO_DATA_P           = "<NA+>";
    public static final String NO_DATA_A            = "<NA*>";
    public static final String TRUE_FALSE           = "true|false";
    public static final String LONG_MEDIUM_SHORT    = "long|medium|short";
    public static final String YES_NO               = "yes|no";
    
    
    public static final String UNOID                    = "unoid";
    public static final String MSGID                    = "msgid";
    public static final String REF                      = "ref";
    public static final String LOCALE                   = "Locale";
    public static final String LC_INFO                  = "LC_INFO";
    public static final String LC_CTYPE                 = "LC_CTYPE";
    public static final String LC_FORMAT                = "LC_FORMAT";
    public static final String LC_COLLATION             = "LC_COLLATION";
    public static final String LC_SEARCH                = "LC_SEARCH";
    public static final String LC_INDEX                 = "LC_INDEX";
    public static final String LC_CALENDAR              = "LC_CALENDAR";
    public static final String LC_CURRENCY              = "LC_CURRENCY";
    public static final String LC_TRANSLITERATION       = "LC_TRANSLITERATION";
    public static final String LC_MISC                  = "LC_MISC";
    public static final String LC_NUMBERING_LEVEL       =  "LC_NumberingLevel";
    public static final String LC_OUTLINE_NUMBERING_LEVEL = "LC_OutLineNumberingLevel";
    public static final String VERSION                  = "version";
    public static final String LANGUAGE                 = "Language";
    public static final String COUNTRY                  = "Country";
    public static final String PLATFORM                 = "Platform";
    public static final String VARIENT                  = "Varient";
    public static final String VARIANT                  = "Variant";
    public static final String LANG_ID                  = "LangID";
    public static final String DEFAULT_NAME             = "DefaultName";
    public static final String COUNTRY_ID               = "CountryID";
    public static final String PLATFORM_ID              = "PlatformID";
    public static final String FORMAT_ELEMENT           = "FormatElement";
//    public static final String REPLACE_FROM             = "ReplaceFrom";
//    public static final String REPLACE_TO               = "ReplaceTo";
    public static final String REPLACE_FROM_SMALL       = "replaceFrom";
    public static final String REPLACE_TO_SMALL         = "replaceTo";
    public static final String FORMAT_CODE              = "FormatCode";
    public static final String FORMAT_INDEX             = "formatindex";
    public static final String DEFAULT                  = "default";
    public static final String TYPE                     = "type";
    public static final String USAGE                    = "usage";
    public static final String CALENDAR                 = "Calendar";
    public static final String DAYS_OF_WEEK             = "DaysOfWeek";
    public static final String MONTHS_OF_YEAR           = "MonthsOfYear";
    public static final String ERAS                     = "Eras";
    public static final String START_DAY_OF_WEEK        = "StartDayOfWeek";
    public static final String MINIMAL_DAYS_IN_FIRST_WEEK = "MinimalDaysInFirstWeek";
    public static final String DAY                      = "Day";
    public static final String DAY_ID                   = "DayID";
    public static final String DEFAULT_ABBRV_NAME       = "DefaultAbbrvName";
    public static final String DEFAULT_FULL_NAME        = "DefaultFullName";
    public static final String MONTH                    = "Month";
    public static final String MONTH_ID                 = "MonthID";
    public static final String ERA                      = "Era";
    public static final String ERA_ID                   = "EraID";
    public static final String CURRENCY                 = "Currency";
    public static final String CURRENCY_ID              = "CurrencyID";
    public static final String CURRENCY_SYMBOL          = "CurrencySymbol";
    public static final String BANK_SYMBOL              = "BankSymbol";
    public static final String CURRENCY_NAME            = "CurrencyName";
    public static final String DECIMAL_PLACES           = "DecimalPlaces";
    public static final String USED_IN_COMPARTIBLE_FORMATCODES = "UsedInCompatibleFormatCodes";
    public static final String USED_IN_COMPARTIBLE_FORMATCODES_SMALL = "usedInCompatibleFormatCodes";
    public static final String SEPARATORS               = "Separators";
    public static final String MARKERS                  = "Markers";
    public static final String TIME_AM                  = "TimeAM";
    public static final String TIME_PM                  = "TimePM";
    public static final String MEASUREMENT_SYSTEM       = "MeasurementSystem";
    public static final String DATE_SEPARATOR           = "DateSeparator";
    public static final String THOUSAND_SEPARATOR       = "ThousandSeparator";
    public static final String DECIMAL_SEPARATOR        = "DecimalSeparator";
    public static final String TIME_SEPARATOR           = "TimeSeparator";
    public static final String TIME_100SEC_SEPARATOR    = "Time100SecSeparator";
    public static final String LIST_SEPARATOR           = "ListSeparator";
    public static final String LONG_DATE_DAY_OF_WEEK_SEPARATOR = "LongDateDayOfWeekSeparator";
    public static final String LONG_DATE_DAY_SEPARATOR  = "LongDateDaySeparator";
    public static final String LONG_DATE_MONTH_SEPARATOR = "LongDateMonthSeparator";
    public static final String LONG_DATE_YEAR_SEPARATOR = "LongDateYearSeparator";
    public static final String QUOTATION_START          = "QuotationStart";
    public static final String QUOTATION_END            = "QuotationEnd";
    public static final String DOUBLE_QUOTATION_START   = "DoubleQuotationStart";
    public static final String DOUBLE_QUOTATION_END     = "DoubleQuotationEnd";
    public static final String COLLATOR                 = "Collator";
    public static final String COLLATION_OPTIONS        = "CollationOptions";
    public static final String TRANSLITERATION_MODULES  = "TransliterationModules";
    public static final String SEARCH_OPTIONS           = "SearchOptions";
    public static final String INDEX_KEY                = "IndexKey";
    public static final String UNICODE_SCRIPT           = "UnicodeScript";
    public static final String FOLLOW_PAGE_WORD         = "FollowPageWord";
    public static final String PHONETIC                 = "phonetic";
    public static final String MODULE                   = "module";
    public static final String TRANSLITERATION          = "Transliteration";
    public static final String FORBIDDEN_CHARACTERS     = "ForbiddenCharacters";
    public static final String RESERVED_WORDS            = "ReservedWords";
    public static final String FORBIDDEN_LINE_BEGIN_CHARACTERS     = "ForbiddenLineBeginCharacters";
    public static final String FORBIDDEN_LINE_END_CHARACTERS       = "ForbiddenLineEndCharacters";
    public static final String TRUE_WORD                = "trueWord";
    public static final String FALSE_WORD               = "falseWord";
    public static final String QUARTER_1_WORD           = "quarter1Word";
    public static final String QUARTER_2_WORD           = "quarter2Word";
    public static final String QUARTER_3_WORD           = "quarter3Word";
    public static final String QUARTER_4_WORD           = "quarter4Word";
    public static final String ABOVE_WORD               = "aboveWord";
    public static final String BELOW_WORD               = "belowWord";
    public static final String QUARTER_1_ABBREVIATION   = "quarter1Abbreviation";
    public static final String QUARTER_2_ABBREVIATION   = "quarter2Abbreviation";
    public static final String QUARTER_3_ABBREVIATION   = "quarter3Abbreviation";
    public static final String QUARTER_4_ABBREVIATION   = "quarter4Abbreviation";
    public static final String NUMBERING_LEVEL          = "NumberingLevel";
    public static final String PREFIX                   = "Prefix";
    public static final String NUM_TYPE                 = "NumType";
    public static final String SUFFIX                   = "Suffix";
    public static final String NAT_NUM                  = "NatNum";
    public static final String OUTLINE_STYLE            = "OutlineStyle";
    public static final String OUTLUNE_NUMBERING_LEVEL  = "OutLineNumberingLevel";
    public static final String BULLET_CHAR              = "BulletChar";
    public static final String BULLET_FONT_NAME         = "BulletFontName";
    public static final String PARENT_NUMBERING         = "ParentNumbering";
    public static final String LEFT_MARGIN              = "LeftMargin";
    public static final String SYMBOL_TEXT_DISTANCE     = "SymbolTextDistance";
    public static final String FIRST_LINE_OFFSET        = "FirstLineOffset";
    
    public static final String TRUE                     = "true";
    public static final String FALSE                    = "false";
    
    public static final String SUN                      = "sun";
    public static final String MON                      = "mon";
    public static final String TUE                      = "tue";
    public static final String WED                      = "wed";
    public static final String THU                      = "thu";
    public static final String FRI                      = "fri";
    public static final String SAT                      = "sat";
    
    public static final String MONTH_1                      = "jan";
    public static final String MONTH_2                      = "feb";
    public static final String MONTH_3                      = "mar";
    public static final String MONTH_4                      = "apr";
    public static final String MONTH_5                      = "may";
    public static final String MONTH_6                      = "jun";
    public static final String MONTH_7                      = "jul";
    public static final String MONTH_8                      = "aug";
    public static final String MONTH_9                      = "sep";
    public static final String MONTH_10                      = "oct";
    public static final String MONTH_11                      = "nov";
    public static final String MONTH_12                      = "dec";
    
    public static final String MONTH_1_ALT                  = "Nissan";
    public static final String MONTH_2_ALT                  = "Iyar";
    public static final String MONTH_3_ALT                  = "Sivan";
    public static final String MONTH_4_ALT                  = "Tammuz";
    public static final String MONTH_5_ALT                  = "Av";
    public static final String MONTH_6_ALT                  = "Elul";
    public static final String MONTH_7_ALT                  = "Tishri";
    public static final String MONTH_8_ALT                  = "Heshvan";
    public static final String MONTH_9_ALT                  = "Kislev";
    public static final String MONTH_10_ALT                 = "Tevet";
    public static final String MONTH_11_ALT                 = "Shevat";
    public static final String MONTH_12_ALT                 = "Adar";
    public static final String MONTH_13_ALT                 = "ve-Adar";
    
    //calendars
    public static final String GREGORIAN                = "gregorian";
    public static final String HIJRI                    = "hijri";   //arabic
    public static final String JEWISH                   = "jewish";
    public static final String GENGOU                   = "gengou";  //japanese
    public static final String HANJA                    = "hanja";
    public static final String BUDDHIST                 = "buddhist";
    public static final String ROC                      = "ROC";   //Taiwan
    //eras
    //follwoing era names used in GREGORIAN and HANJA (ko):
    public static final String BC                       = "bc";
    public static final String AD                       = "ad";
    //follwoing era names used in HIJRI (arabic):
    public static final String BEFORE_HIJRA             = "BeforeHijra";
    public static final String AFTER_HIJRA              = "AfterHijra";
    //follwoing era names used in BUDDHIST and JEWISH:
    public static final String BEFORE                   = "before";
    public static final String AFTER                    = "after";
    //follwoing era names used in GENGOU  (japanese)
    public static final String DUMMY                    = "Dummy";
    public static final String MEIJI                    = "Meiji";
    public static final String TAISHO                   = "Taisho";
    public static final String SHOWA                    = "Showa";
    public static final String HEISEI                   = "Heisei";
    //follwoing era names used in ROC (used in Taiwan) :
    public static final String BEFORE_ROC               = BEFORE;
    public static final String MINGUO                   = "MINGUO";
    
    public static final String METRIC_1                 = "Metric";
    public static final String METRIC_2                 = "metric";
    public static final String US                       = "US";
    
    //FormatElement usage : possible attrib values :
    public static final String FEU_FIXED_NUMBER          = "FIXED_NUMBER";
    public static final String FEU_FRACTION_NUMBER       = "FRACTION_NUMBER";
    public static final String FEU_PERCENT_NUMBER        = "PERCENT_NUMBER";
    public static final String FEU_SCIENTIFIC_NUMBER     = "SCIENTIFIC_NUMBER";
    public static final String FEU_CURRENCY              = "CURRENCY";
    public static final String FEU_DATE                  = "DATE";
    public static final String FEU_TIME                  = "TIME";
    public static final String FEU_DATE_TIME             = "DATE_TIME";
    
    //Transliteration eleemnts and attrs , possible values
    public static final String TRANS_UL                 = "UPPERCASE_LOWERCASE";
    public static final String TRANS_LU                 = "LOWERCASE_UPPERCASE";
    public static final String TRANS_IC                 = "IGNORE_CASE";
    
    //new constants
    public static final String VERSION_DTD              = "versionDTD";
    public static final String ALLOW_UPDATE_FROM_CLDR   = "allowUpdateFromCLDR";
   
   public static final String LEGACY_ONLY                = "legacyOnly";
}
