package org.unicode.cldr.tool;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;

class LdmlConvertRules {

    /** All sub-directories that will be processed in JSON transformation. */
    public static final String CLDR_SUBDIRS[] = {
        "main", "supplemental"
        // We could do everything, but not really sure how useful it would be.
        // For now, just do main and supplemental per CLDR TC agreement.
        // "collation", "bcp47", "supplemental", "rbnf", "segments", "main", "transforms"
    };

    /** File set that will not be processed in JSON transformation. */
    public static final Set<String> IGNORE_FILE_SET = Builder.with(new HashSet<String>())
        .add("supplementalMetadata").add("coverageLevels").freeze();

    /**
     * The attribute list that should become part of the name in form of
     * name-(attribute)-(value).
     * [parent_element]:[element]:[attribute]
     */
    private static final String[] NAME_PART_DISTINGUISHING_ATTR_LIST = {
        // common/main
        "monthWidth:month:yeartype",
        "currencyFormats:unitPattern:count",
        "currency:displayName:count",
        "numbers:symbols:numberSystem",
        "numbers:decimalFormats:numberSystem",
        "numbers:currencyFormats:numberSystem",
        "numbers:percentFormats:numberSystem",
        "numbers:scientificFormats:numberSystem",
        "territoryContainment:group:status",
        "decimalFormat:pattern:count",
        "unit:unitPattern:count",
        "pluralRules:pluralRule:count"
    };

    /**
     * The set of attributes that should become part of the name in form of
     * name-(attribute)-(value).
     */
    public static final Set<String> NAME_PART_DISTINGUISHING_ATTR_SET =
        new HashSet<String>(Arrays.asList(NAME_PART_DISTINGUISHING_ATTR_LIST));

    /**
     * Following is a list of element:attribute pair. These attributes should be
     * treated as values. For example,
     * <type type="arab" key="numbers">Arabic-Indic Digits</type>
     * should be really converted as,
     * "arab": {
     * "_value": "Arabic-Indic Digits",
     * "@key": "numbers"
     * }
     */
    private static final String[] ATTR_AS_VALUE_LIST = {
        // common/main
        "types:type:key",

        // in common/supplemental/dayPeriods.xml
        "dayPeriodRules:dayPeriodRule:from",

        // in common/supplemental/likelySubtags.xml
        "likelySubtags:likelySubtag:to",

        // in common/supplemental/metaZones.xml
        "timezone:usesMetazone:mzone",
        // Only the current usesMetazone will be kept, it is not necessary to keep
        // "to" and "from" attributes to make key unique. This is needed as their
        // value is not good if used as key.
        "timezone:usesMetazone:to",
        "timezone:usesMetazone:from",

        "mapTimezones:mapZone:other",
        "mapTimezones:mapZone:type",
        "mapTimezones:mapZone:territory",

        // in common/supplemental/numberingSystems.xml
        "numberingSystems:numberingSystem:type",

        // in common/supplemental/supplementalData.xml
        "region:currency:from",
        "region:currency:to",
        "region:currency:tender",
        "calendar:calendarSystem:type",
        "codeMappings:territoryCodes:numeric",
        "codeMappings:territoryCodes:alpha3",

        // in common/supplemental/windowsZones.xml
        "mapTimezones:mapZone:other",

        // in common/bcp47/*.xml
        "keyword:key:alias",
        "key:type:alias",
        "key:type:name",

        // identity elements
        "identity:language:type",
        "identity:script:type",
        "identity:territory:type",
        "identity:variant:type",

    };

    /**
     * The set of element:attribute pair in which the attribute should be
     * treated as value. All the attribute here are non-distinguishing attributes.
     */
    public static final Set<String> ATTR_AS_VALUE_SET =
        new HashSet<String>(Arrays.asList(ATTR_AS_VALUE_LIST));

    /**
     * For those attributes that are treated as values, they taken the form of
     * element_name: { ..., attribute: value, ...}
     * This is desirable as an element may have several attributes that are
     * treated as values. But in some cases, there is one such attribute only,
     * and it is more desirable to convert
     * element_name: { attribute: value}
     * to
     * element_name: value
     * With a solid example,
     * <likelySubtag from="zh" to="zh_Hans_CN" />
     * distinguishing attr "from" will become the key, its better to
     * omit "to" and have this simple mapping:
     * "zh" : "zh_Hans_CN",
     */
    private static final String[] COMPACTABLE_ATTR_AS_VALUE_LIST = {
        // common/main
        "calendars:default:choice",
        "dateFormats:default:choice",
        "months:default:choice",
        "monthContext:default:choice",
        "days:default:choice",
        "dayContext:default:choice",
        "timeFormats:default:choice",
        "dateTimeFormats:default:choice",
        "timeZoneNames:singleCountries:list",

        // common/supplemental
        "likelySubtags:likelySubtag:to",
        "territoryContainment:group:contains",
        "calendar:calendarSystem:type",
        "calendarPreferenceData:calendarPreference:ordering",
        "firstDay:firstDay:day",
        "minDays:minDays:territories",
        "weekendStart:weekendStart:day",
        "weekendEnd:weekendEnd:day",
        "measurementData:measurementSystem:type",
        "codesByterritory:telephoneCountryCode:code",

        // common/collation
        "collations:default:choice",

        // identity elements
        "identity:language:type",
        "identity:script:type",
        "identity:territory:type",
        "identity:variant:type",
    };

    /**
     * The set of attributes that should be treated as value, and reduce to
     * simple value only form.
     */
    public static final Set<String> COMPACTABLE_ATTR_AS_VALUE_SET =
        new HashSet<String>(Arrays.asList(COMPACTABLE_ATTR_AS_VALUE_LIST));

    /**
     * Anonymous key name.
     */
    public static final String ANONYMOUS_KEY = "_";

    /**
     * Check if the attribute should be suppressed.
     * 
     * Right now only "_q" is suppressed. In most cases array is used and there
     * is no need for this information. In other cases, order is irrelevant.
     * 
     * @return True if the attribute should be suppressed.
     */
    public static boolean IsSuppresedAttr(String attr) {
        return attr.endsWith("_q");
    }

    /**
     * The set of attributes that should be ignored in the conversion process.
     */
    public static final Set<String> IGNORABLE_NONDISTINGUISHING_ATTR_SET =
        Builder.with(new HashSet<String>())
            .add("draft")
            .add("references").freeze();

    /**
     * List of attributes that should be suppressed.
     * This list comes form cldr/common/supplemental/supplementalMetadata. Each
     * three of them is a group, they are for element, value and attribute.
     * If the specified attribute appears in specified element with specified =
     * value, it should be suppressed.
     */
    public static final String[] ATTR_SUPPRESS_LIST = {
        // common/main
        "dateFormat", "standard", "type",
        "dateTimeFormat", "standard", "type",
        "timeFormat", "standard", "type",
        "currencyFormat", "standard", "type",
        "decimalFormat", "standard", "type",
        "percentFormat", "standard", "type",
        "scientificFormat", "standard", "type",
        "pattern", "standard", "type",
    };

    /**
     * This is a simple class to hold the splittable attribute specification.
     */
    public static class SplittableAttributeSpec {
        public String element;
        public Pattern pattern;

        SplittableAttributeSpec(String el, String attr) {
            element = el;
            pattern = Pattern.compile("(.*\\[@" + attr + "=\")([^\"]*)(\"\\].*)");
        }
    }

    /**
     * List of attributes that has value that can be split. Each two of them is a
     * group, and represent element and value. Occurrences of such match should
     * lead to creation of multiple node.
     * Example:
     * <weekendStart day="thu" territories="DZ KW OM SA SD YE AF IR"/>
     * should be treated as if following node is encountered.
     * <weekendStart day="thu" territories="DZ"/>
     * <weekendStart day="thu" territories="KW"/>
     * <weekendStart day="thu" territories="OM"/>
     * <weekendStart day="thu" territories="SA"/>
     * <weekendStart day="thu" territories="SD"/>
     * <weekendStart day="thu" territories="YE"/>
     * <weekendStart day="thu" territories="AF"/>
     * <weekendStart day="thu" territories="IR"/>
     */
    public static final SplittableAttributeSpec[] SPLITTABLE_ATTRS = {
        new SplittableAttributeSpec("/measurementSystem", "territories"),
        new SplittableAttributeSpec("/calendarPreference", "territories"),
        new SplittableAttributeSpec("/pluralRules", "locales"),
        new SplittableAttributeSpec("/weekendStart", "territories"),
        new SplittableAttributeSpec("/weekendEnd", "territories"),
        new SplittableAttributeSpec("/firstDay", "territories"),
        new SplittableAttributeSpec("/dayPeriodRules", "locales")
    };

    /**
     * The set that contains all timezone type of elements.
     */
    public static final Set<String> TIMEZONE_ELEMENT_NAME_SET =
        Builder.with(new HashSet<String>())
            .add("zone").add("timezone")
            .add("zoneItem").add("typeMap").freeze();

    /**
     * Following is the list of elements that need to be sorted before output.
     * 
     * Time zone item is split to multiple level, and each level should be
     * grouped together. The locale list in "dayPeriodRule" could be split to
     * multiple items, and items for each locale should be grouped together.
     */
    public static final String[] ELEMENT_NEED_SORT = {
        "zone", "timezone", "zoneItem", "typeMap", "dayPeriodRule",
        "pluralRules"
    };

    /**
     * Some elements in CLDR has multiple children of the same type of element.
     * We would like to treat them as array.
     */
    public static final Pattern ARRAY_ITEM_PATTERN = Pattern.compile(
        "(.*/collation[^/]*/rules[^/]*/" +
            "|.*/character-fallback[^/]*/character[^/]*/" +
            "|.*/dayPeriodRuleSet[^/]*/dayPeriodRules[^/]*/" +
            "|.*/languageMatching[^/]*/languageMatches[^/]*/" +
            "|.*/windowsZones[^/]*/mapTimezones[^/]*/" +
            "|.*/metaZones[^/]*/mapTimezones[^/]*/" +
            "|.*/segmentation[^/]*/variables[^/]*/" +
            "|.*/transform[^/]*/tRules[^/]*/" +
            "|.*/region/region[^/]*/" +
            "|.*/keyword[^/]*/key[^/]*/" +
            "|.*/telephoneCodeData[^/]*/codesByTerritory[^/]*/" +
            "|.*/metazoneInfo[^/]*/timezone\\[[^\\]]*\\]/" +
            ")(.*)");

    /**
     * Number elements without a numbering system are there only for compatibility purposes.
     * We automatically suppress generation of JSON objects for them.
     */
    public static final Pattern NO_NUMBERING_SYSTEM_PATTERN = Pattern
        .compile("//ldml/numbers/(symbols|(decimal|percent|scientific|currency)Formats)/.*");
    public static final Pattern NUMBERING_SYSTEM_PATTERN = Pattern
        .compile("//ldml/numbers/(symbols|(decimal|percent|scientific|currency)Formats)\\[@numberSystem=\"([^\"]++)\"\\]/.*");

    public static final String[] ACTIVE_NUMBERING_SYSTEM_XPATHS = {
        "//ldml/numbers/defaultNumberingSystem",
        "//ldml/numbers/otherNumberingSystems/native",
        "//ldml/numbers/otherNumberingSystems/traditional",
        "//ldml/numbers/otherNumberingSystems/finance"
    };

    /**
     * A simple class to hold the specification of a path transformation.
     */
    public static class PathTransformSpec {
        public Pattern pattern;
        public String replacement;

        PathTransformSpec(String patternStr, String replacement) {
            pattern = Pattern.compile(patternStr);
            this.replacement = replacement;
        }
    }

    /**
     * Some special transformation, like add an additional layer, can be easily
     * done by transforming the path. Following rules covers these kind of
     * transformation.
     * Note: It is important to keep the order for these rules. Whenever a
     * rule matches, further rule won't be applied.
     */
    public static final PathTransformSpec PATH_TRANSFORMATIONS[] = {
        // Add "standard" as type attribute to exemplarCharacter element if there
        // is none, and separate them to two layers.
        new PathTransformSpec(
            "(.*ldml/exemplarCharacters)\\[@type=\"([^\"]*)\"\\](.*)", "$1/$2$3"),
        new PathTransformSpec("(.*ldml/exemplarCharacters)(.*)$", "$1/standard$2"),

        // Add cldrVersion attribute
        new PathTransformSpec("(.*/identity/version\\[@number=\"([^\"]*)\")(\\])", "$1" + "\\]\\[@cldrVersion=\""
            + CLDRFile.GEN_VERSION + "\"\\]"),

        // Separate "ellipsis" from its type as another layer.
        new PathTransformSpec("(.*/ellipsis)\\[@type=\"([^\"]*)\"\\](.*)$",
            "$1/$2$3"),

        // Separate "metazone" from its type as another layer.
        new PathTransformSpec("(.*/metazone)\\[@type=\"([^\"]*)\"\\]/(.*)$",
            "$1/$2/$3"),

        // Add "type" attribute with value "standard" if there is no "type" in
        // "decimalFormatLength".
        new PathTransformSpec(
            "(.*/numbers/(decimal|currency|scientific|percent)Formats\\[@numberSystem=\"([^\"]*)\"\\]/(decimal|currency|scientific|percent)FormatLength)/(.*)$",
            "$1[@type=\"standard\"]/$5"),

        // Separate type of an language as another layer.
        // new PathTransformSpec("(.*identity/language)\\[@type=\"([^\"]*)\"\\](.*)$",
        // "$1/$2$3"),

        new PathTransformSpec("(.*/languagePopulation)\\[@type=\"([^\"]*)\"\\](.*)",
            "$1/$2$3"),

        new PathTransformSpec("(.*/paperSize)\\[@type=\"([^\"]*)\"\\](.*)",
            "$1/$2$3"),

        new PathTransformSpec("(.*/alias)(.*)", "$1/alias$2"),

        // The purpose for following transformation is to keep the element name
        // and still use distinguishing attribute inside. Element name is repeated
        // for attribute identification to work.
        new PathTransformSpec("(.*/firstDay)(.*)", "$1/firstDay$2"),

        new PathTransformSpec("(.*/minDays)(.*)", "$1/minDays$2"),

        new PathTransformSpec("(.*/weekendStart)(.*)", "$1/weekendStart$2"),

        new PathTransformSpec("(.*/weekendEnd)(.*)", "$1/weekendEnd$2"),

        new PathTransformSpec("(.*currencyData/region)(.*)", "$1/region$2"),

        new PathTransformSpec("(.*/transforms/transform[^/]*)/(.*)", "$1/tRules/$2"),
    };
}
