package org.unicode.cldr.json;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.FileProcessor;
import org.unicode.cldr.util.PatternCache;

import com.google.common.collect.ImmutableSet;

class LdmlConvertRules {

    /** File sets that will not be processed in JSON transformation. */
    public static final ImmutableSet<String> IGNORE_FILE_SET = ImmutableSet.of("attributeValueValidity", "coverageLevels", "postalCodeData",
        "subdivisions");

    /**
     * The attribute list that should become part of the name in form of
     * name-(attribute)-(value).
     * [parent_element]:[element]:[attribute]
     */
    // common/main
    static final ImmutableSet<String> NAME_PART_DISTINGUISHING_ATTR_SET = ImmutableSet.of(
        "monthWidth:month:yeartype",
        "characters:parseLenients:scope",
        "dateFormat:pattern:numbers",
        "characterLabelPatterns:characterLabelPattern:count", // originally under characterLabels
        "currencyFormats:unitPattern:count",
        "currency:displayName:count",
        "numbers:symbols:numberSystem",
        "numbers:decimalFormats:numberSystem",
        "numbers:currencyFormats:numberSystem",
        "numbers:percentFormats:numberSystem",
        "numbers:scientificFormats:numberSystem",
        "numbers:miscPatterns:numberSystem",
        "minimalPairs:pluralMinimalPairs:count",
        "territoryContainment:group:status",
        "decimalFormat:pattern:count",
        "currencyFormat:pattern:count",
        "unit:unitPattern:count",
        // compound units
        "compoundUnit:compoundUnitPattern1:count",
        "compoundUnit:compoundUnitPattern1:gender",
        "compoundUnit:compoundUnitPattern1:case",
        "field:relative:type",
        "field:relativeTime:type",
        "relativeTime:relativeTimePattern:count",
        "availableFormats:dateFormatItem:count",
        "listPatterns:listPattern:type",
        "timeZoneNames:regionFormat:type",
        "units:durationUnit:type",
        "weekData:minDays:territories",
        "weekData:firstDay:territories",
        "weekData:weekendStart:territories",
        "weekData:weekendEnd:territories",
        "supplemental:dayPeriodRuleSet:type",
        // units
        "unitPreferenceDataData:unitPreferences:category",
       // grammatical features
       // in common/supplemental/grammaticalFeatures.xml
        "grammaticalData:grammaticalFeatures:targets",
        "grammaticalGenderData:grammaticalFeatures:targets",
        "grammaticalFeatures:grammaticalCase:scope",
        "grammaticalFeatures:grammaticalGender:scope",
        "grammaticalDerivations:deriveCompound:structure",
        "grammaticalDerivations:deriveCompound:feature",
        "grammaticalDerivations:deriveComponent:feature",
        "grammaticalDerivations:deriveComponent:structure",
        // measurement
        "measurementData:measurementSystem:category",
        "supplemental:plurals:type",
        "pluralRanges:pluralRange:start",
        "pluralRanges:pluralRange:end",
        "pluralRules:pluralRule:count",
        "languageMatches:languageMatch:desired",
        "styleNames:styleName:subtype",
        "styleNames:styleName:alt");

    /**
     * The set of attributes that should become part of the name in form of
     * name-(attribute)-(value).
     */

    /**
     * Following is a list of element:attribute pair. These attributes should be
     * treated as values. For example,
     * <type type="arab" key="numbers">Arabic-Indic Digits</type>
     * should be really converted as,
     * "arab": {
     * "_value": "Arabic-Indic Digits",
     * "_key": "numbers"
     * }
     */
    static final ImmutableSet<String> ATTR_AS_VALUE_SET = ImmutableSet.of(

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
        "codeMappings:currencyCodes:numeric",
        "timeData:hours:allowed",
        "timeData:hours:preferred",
        // common/supplemental/supplementalMetaData.xml
        "validity:variable:type",
        "deprecated:deprecatedItems:elements",
        "deprecated:deprecatedItems:attributes",
        "deprecated:deprecatedItems:type",

        // in common/supplemental/telephoneCodeData.xml
        "codesByTerritory:telephoneCountryCode:code",

        // in common/supplemental/windowsZones.xml
        "mapTimezones:mapZone:other",

        // in common/supplemental/units.xml
        "*:unitPreference:geq",
        "*:unitPreference:skeleton",

        // in common/supplemental/grammaticalFeatures.xml
        "grammaticalDerivations:deriveComponent:value0",
        "grammaticalDerivations:deriveComponent:value1",

        // identity elements
        "identity:language:type",
        "identity:script:type",
        "identity:territory:type",
        "identity:variant:type",

        // in common/bcp47/*.xml
        "keyword:key:name"
    );

    /**
     * The set of element:attribute pair in which the attribute should be
     * treated as value. All the attribute here are non-distinguishing attributes.
     */

    /**
     * For those attributes that are treated as values, they taken the form of
     * element_name: { ..., attribute: value, ...}
     * This is desirable as an element may have several attributes that are
     * treated as values. But in some cases, there is one such attribute only,
     * and it is more desirable to convert
     * element_name: { attribute: value}
     * to
     * element_name: value
     * With a solid example, (likelySubtags:likelySubtag:to)
     * <likelySubtag from="zh" to="zh_Hans_CN" />
     * distinguishing attr "from" will become the key, its better to
     * omit "to" and have this simple mapping:
     * "zh" : "zh_Hans_CN",
     */
    static final ImmutableSet<String> COMPACTABLE_ATTR_AS_VALUE_SET = ImmutableSet.of(
        // parent:element:attribute
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

        //rbnf
        "ruleset:rbnfrule:value",
        // common/supplemental
        "likelySubtags:likelySubtag:to",
        //"territoryContainment:group:type",
        "calendar:calendarSystem:type",
        "calendarPreferenceData:calendarPreference:ordering",
        "codesByTerritory:telephoneCountryCode:code",

        // common/collation
        "collations:default:choice",

        // common/supplemental/pluralRanges.xml
        "pluralRanges:pluralRange:result",

        // identity elements
        "identity:language:type",
        "identity:script:type",
        "identity:territory:type",
        "identity:variant:type",

        "grammaticalFeatures:grammaticalGender:values",
        "grammaticalFeatures:grammaticalDefiniteness:values",
        "grammaticalFeatures:grammaticalCase:values",
        "grammaticalDerivations:deriveCompound:value"

    );

    /**
     * The set of attributes that should be treated as value, and reduce to
     * simple value only form.
     */

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
        return attr.endsWith("_q") || attr.endsWith("-q");
    }

    /**
     * The set of attributes that should be ignored in the conversion process.
     */
    public static final ImmutableSet<String> IGNORABLE_NONDISTINGUISHING_ATTR_SET = ImmutableSet.of("draft", "references");

    /**
     * List of attributes that should be suppressed.
     * This list comes from cldr/common/supplemental/supplementalMetadata. Each
     * three of them is a group, they are for element, value and attribute.
     * If the specified attribute appears in specified element with specified =
     * value, it should be suppressed.
     */
    public static final String[] ATTR_SUPPRESS_LIST = {
        // common/main
        "dateFormat", "standard", "type",
        "dateTimeFormat", "standard", "type",
        "timeFormat", "standard", "type",
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
        public String attribute;
        public String attrAsValueAfterSplit;

        SplittableAttributeSpec(String el, String attr, String av) {
            element = el;
            attribute = attr;
            attrAsValueAfterSplit = av;
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
    private static final SplittableAttributeSpec[] SPLITTABLE_ATTRS = {
        new SplittableAttributeSpec("calendarPreference", "territories", null),
        new SplittableAttributeSpec("pluralRanges", "locales", null),
        new SplittableAttributeSpec("pluralRules", "locales", null),
        new SplittableAttributeSpec("minDays", "territories", "count"),
        new SplittableAttributeSpec("firstDay", "territories", "day"),
        new SplittableAttributeSpec("weekendStart", "territories", "day"),
        new SplittableAttributeSpec("weekendEnd", "territories", "day"),
        new SplittableAttributeSpec("weekOfPreference", "locales", "ordering"),
        new SplittableAttributeSpec("measurementSystem", "territories", "type"),
        // this is deprecated, so no need to generalize this exception.
        new SplittableAttributeSpec("measurementSystem-category-temperature", "territories", "type"),
        new SplittableAttributeSpec("paperSize", "territories", "type"),
        new SplittableAttributeSpec("parentLocale", "locales", "parent"),
        new SplittableAttributeSpec("hours", "regions", null),
        new SplittableAttributeSpec("dayPeriodRules", "locales", null),
        // new SplittableAttributeSpec("group", "contains", "group"),
        new SplittableAttributeSpec("personList", "locales", "type"),
        new SplittableAttributeSpec("unitPreference", "regions", null),
        new SplittableAttributeSpec("grammaticalFeatures", "locales", null),
        new SplittableAttributeSpec("grammaticalDerivations", "locales", null),
    };

    /**
     * The set that contains all timezone type of elements.
     */
    public static final Set<String> TIMEZONE_ELEMENT_NAME_SET = Builder.with(new HashSet<String>())
        .add("zone").add("timezone")
        .add("zoneItem").add("typeMap").freeze();

    /**
     * There are a handful of attribute values that are more properly represented as an array of strings rather than
     * as a single string.
     */
    public static final Set<String> ATTRVALUE_AS_ARRAY_SET = Builder.with(new HashSet<String>())
        .add("territories").add("scripts").add("contains").add("systems").freeze();

    /**
     * Following is the list of elements that need to be sorted before output.
     *
     * Time zone item is split to multiple level, and each level should be
     * grouped together. The locale list in "dayPeriodRule" could be split to
     * multiple items, and items for each locale should be grouped together.
     */
    public static final String[] ELEMENT_NEED_SORT = {
        "zone", "timezone", "zoneItem", "typeMap", "dayPeriodRule", "pluralRanges",
        "pluralRules", "personList", "calendarPreferenceData", "character-fallback", "types", "timeData", "minDays",
        "firstDay", "weekendStart", "weekendEnd", "measurementData", "measurementSystem"
    };

    /**
     * Some elements in CLDR has multiple children of the same type of element.
     * We would like to treat them as array.
     */
    public static final Pattern ARRAY_ITEM_PATTERN = PatternCache.get(
        "(.*/collation[^/]*/rules[^/]*/" +
            "|.*/character-fallback[^/]*/character[^/]*/" +
            "|.*/rbnfrule[^/]*/" +
            "|.*/ruleset[^/]*/" +
            "|.*/languageMatching[^/]*/languageMatches[^/]*/" +
            "|.*/unitPreferences/[^/]*/[^/]*/" +
            "|.*/windowsZones[^/]*/mapTimezones[^/]*/" +
            "|.*/metaZones[^/]*/mapTimezones[^/]*/" +
            "|.*/segmentation[^/]*/variables[^/]*/" +
            "|.*/segmentation[^/]*/suppressions[^/]*/" +
            "|.*/transform[^/]*/tRules[^/]*/" +
            "|.*/region/region[^/]*/" +
            "|.*/keyword[^/]*/key[^/]*/" +
            "|.*/telephoneCodeData[^/]*/codesByTerritory[^/]*/" +
            "|.*/metazoneInfo[^/]*/timezone\\[[^\\]]*\\]/" +
            "|.*/metadata[^/]*/validity[^/]*/" +
            "|.*/metadata[^/]*/suppress[^/]*/" +
            "|.*/metadata[^/]*/deprecated[^/]*/" +
            ")(.*)");

    /**
     * These objects values should be output as arrays.
     */
    public static final Pattern VALUE_IS_SPACESEP_ARRAY = PatternCache.get(
        "(grammaticalCase|grammaticalGender|grammaticalDefiniteness)"
    );
    public static final Set<String> CHILD_VALUE_IS_SPACESEP_ARRAY = ImmutableSet.of(
        "weekOfPreference",
        "calendarPreferenceData"
    );

    /**
     * Number elements without a numbering system are there only for compatibility purposes.
     * We automatically suppress generation of JSON objects for them.
     */
    public static final Pattern NO_NUMBERING_SYSTEM_PATTERN = Pattern
        .compile("//ldml/numbers/(symbols|(decimal|percent|scientific|currency)Formats)/.*");
    public static final Pattern NUMBERING_SYSTEM_PATTERN = Pattern
        .compile("//ldml/numbers/(symbols|miscPatterns|(decimal|percent|scientific|currency)Formats)\\[@numberSystem=\"([^\"]++)\"\\]/.*");
    public static final String[] ACTIVE_NUMBERING_SYSTEM_XPATHS = {
        "//ldml/numbers/defaultNumberingSystem",
        "//ldml/numbers/otherNumberingSystems/native",
        "//ldml/numbers/otherNumberingSystems/traditional",
        "//ldml/numbers/otherNumberingSystems/finance"
    };

    /**
     * Root language id pattern should be discarded in all locales except root,
     * even though the path will exist in a resolved CLDRFile.
     */
    public static final Pattern ROOT_IDENTITY_PATTERN = Pattern
        .compile("//ldml/identity/language\\[@type=\"root\"\\]");

    /**
     * A simple class to hold the specification of a path transformation.
     */
    public static class PathTransformSpec {

        final private boolean DEBUG_TRANSFORMS = false;
        public Pattern pattern;
        public String replacement;
        public String patternStr;
        public String comment = "";
        private AtomicInteger use = new AtomicInteger();

        PathTransformSpec(String patternStr, String replacement, String comment) {
            this.patternStr = patternStr;
            pattern = PatternCache.get(patternStr);
            this.replacement = replacement;
            this.comment = comment;
            if(this.comment == null) this.comment = "";
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('\n')
            .append("# ").append(comment.replace('\n', ' ')).append('\n')
            .append("< ").append(patternStr).append('\n')
            .append("> ").append(replacement).append('\n');
            return sb.toString();
        }

        /**
         * Apply this rule to a string
         * @param result input string
         * @return result, or null if unchanged
         */
        public String apply(String result) {
            Matcher m = pattern.matcher(result);
            if (m.matches()) {
                final String newResult = m.replaceFirst(replacement);
                final int count = this.use.incrementAndGet();
                if(DEBUG_TRANSFORMS) {
                    System.err.println(result + " => " + newResult + " count " + count + " << " + this.toString());
                }
                return newResult;
            }
            return null;
        }
        public static void dumpAll() {
            System.out.println("# Path Transformations");
            for (final PathTransformSpec ts : getPathTransformations()) {
                System.out.append(ts.toString());
            }
            System.out.println();
        }

        public static final String applyAll(String result) {
            for (final PathTransformSpec ts : getPathTransformations()) {
                final String changed = ts.apply(result);
                if(changed != null) {
                    result = changed;
                    break;
                }
            }
            return result;
        }
    }

    public static final Iterable<PathTransformSpec> getPathTransformations() {
        return PathTransformSpecHelper.INSTANCE;
    }

    /**
     * Add a path transform for the //ldml/identity/version element to the specific number
     * @param version
     */
    public static final void addVersionHandler(String version) {
        if(!CLDRFile.GEN_VERSION.equals(version)) {
            PathTransformSpecHelper.INSTANCE.prependVersionTransforms(version);
        }
    }

    public static final class PathTransformSpecHelper extends FileProcessor implements Iterable<PathTransformSpec> {
        static final PathTransformSpecHelper INSTANCE = make();

        static final PathTransformSpecHelper make() {
            final PathTransformSpecHelper helper = new PathTransformSpecHelper();
            helper.process(PathTransformSpecHelper.class, "pathTransforms.txt");
            return helper;
        }

        private PathTransformSpecHelper() {}
        private List<PathTransformSpec> data = new ArrayList<>();
        private String lastComment = "";
        private String lastPattern = null;
        private String lastReplacement = null;

        @Override
        protected
        void handleStart() {
            // Add these to the beginning because of the dynamic version
            String version = CLDRFile.GEN_VERSION;
            prependVersionTransforms(version);
        }

        /**
         * Prepend version transform.
         * If called twice, the LAST caller will be used.
         * @param version
         */
        public void prependVersionTransforms(String version) {
            data.add(0, new PathTransformSpec("(.+)/identity/version\\[@number=\"([^\"]*)\"\\]", "$1" + "/identity/version\\[@cldrVersion=\""
                + version + "\"\\]", "added by code"));
            // Add cldrVersion attribute to supplemental data
            data.add(0, new PathTransformSpec("(.+)/version\\[@number=\"([^\"]*)\"\\]\\[@unicodeVersion=\"([^\"]*\")(\\])", "$1" + "/version\\[@cldrVersion=\""
                + version + "\"\\]" + "\\[@unicodeVersion=\"" + "$3" + "\\]", "added by code"));
        }

        @Override
        protected boolean handleLine(int lineCount, String line) {
            if(line.isEmpty()) return true;
            if(line.startsWith("<")) {
                lastReplacement = null;
                if(lastPattern != null) {
                    throw new IllegalArgumentException("line " + lineCount+": two <'s in a row");
                }
                lastPattern = line.substring(1).trim();
                if(lastPattern.isEmpty()) {
                    throw new IllegalArgumentException("line " + lineCount+": empty < pattern");
                }
            } else if(line.startsWith(">")) {
                if(lastPattern == null) {
                    throw new IllegalArgumentException("line " + lineCount+": need < line before > line");
                }
                lastReplacement = line.substring(1).trim();
                data.add(new PathTransformSpec(lastPattern, lastReplacement, lastComment));
                reset();
            }
            return true;
        }

        @Override
        protected
        void handleEnd() {
            if(lastPattern != null) {
                throw new IllegalArgumentException("ended with a < but no >");
            }
        }

        private void reset() {
            this.lastComment = "";
            this.lastPattern = null;
            this.lastReplacement = null;
        }

        @Override
        public void handleComment(String line, int commentCharPosition) {
            lastComment = line.substring(commentCharPosition+1).trim();
        }

        @Override
        public Iterator<PathTransformSpec> iterator() {
            return data.iterator();
        }
    }


    public static void main(String args[]) {
        // for debugging / verification
        PathTransformSpec.dumpAll();
    }

    public final static String getKeyStr(String name, String key) {
        String keyStr2 = "*:" + name + ":" + key;
        return keyStr2;
    }

    public final static String getKeyStr(String parent, String name, String key) {
        String keyStr = parent + ":" + name + ":" + key;
        return keyStr;
    }

    public static SplittableAttributeSpec[] getSplittableAttrs() {
        return SPLITTABLE_ATTRS;
    }

    public static final boolean valueIsSpacesepArray(final String nodeName, String parent) {
        return VALUE_IS_SPACESEP_ARRAY.matcher(nodeName).matches()
            || (parent!=null && CHILD_VALUE_IS_SPACESEP_ARRAY.contains(parent));
    }

    static final Set<String> BOOLEAN_OMIT_FALSE = ImmutableSet.of(
        // attribute names within bcp47 that are booleans, but omitted if false.
        "deprecated"
    );

    // These attributes are booleans, and should be omitted if false
    public static final boolean attrIsBooleanOmitFalse(final String fullPath, final String nodeName, final String parent, final String key) {
        return (fullPath != null &&
            (fullPath.startsWith("//supplementalData/metaZones/metazoneIds") &&
            BOOLEAN_OMIT_FALSE.contains(key)));
    }
}
