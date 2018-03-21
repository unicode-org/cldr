package org.unicode.cldr.test;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.cldr.draft.ScriptMetadata.Trinary;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.util.ULocale;

public class CheckConsistentCasing extends FactoryCheckCLDR {

    private static final boolean DEBUG = CldrUtility.getProperty("DEBUG", false);

    private static final double MIN_FACTOR = 2.5;
    // remember to add this class to the list in CheckCLDR.getCheckAll
    // to run just this test, on just locales starting with 'nl', use CheckCLDR with -fnl.* -t.*Currencies.*

    XPathParts parts = new XPathParts(); // used to parse out a path
    ULocale uLocale = null;
    BreakIterator breaker = null;
    private String locale;
    CasingInfo casingInfo;
    private boolean hasCasingInfo;

    public CheckConsistentCasing(Factory factory) {
        super(factory);
        casingInfo = new CasingInfo(factory);
    }

    @Override
    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Options options,
        List<CheckStatus> possibleErrors) {
        if (cldrFileToCheck == null) return this;
        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        locale = cldrFileToCheck.getLocaleID();
        // get info about casing; note that this is done in two steps since
        // ScriptMetadata.getInfo() returns null, in some instances.
        // OLD: Info localeInfo = ScriptMetadata.getInfo(locale);
        String script = new LikelySubtags().getLikelyScript(locale);
        Info localeInfo = ScriptMetadata.getInfo(script);

        if (localeInfo != null && localeInfo.hasCase == Trinary.YES) {
            // this script has casing info, so we can request it here
            try {
                types = casingInfo.getLocaleCasing(locale);
            } catch (Exception e) {
                types = Collections.emptyMap();
            }
        } else {
            // no casing info - since the types Map is global, and null checks aren't done,
            // we are better off  with an empty map here
            types = Collections.emptyMap();
        }
        if (types == null || types.isEmpty()) {
            possibleErrors.add(new CheckStatus().setCause(this)
                .setMainType(CheckStatus.warningType)
                .setSubtype(Subtype.incorrectCasing)
                .setMessage("Could not load casing info for {0}", locale));
        }
        // types may be null, avoid NPE
        hasCasingInfo = (types == null) ? false : types.size() > 0;
        return this;
    }

    // If you don't need any file initialization or postprocessing, you only need this one routine
    public CheckCLDR handleCheck(String path, String fullPath, String value, Options options,
        List<CheckStatus> result) {
        // it helps performance to have a quick reject of most paths
        if (fullPath == null) return this; // skip paths that we don't have
        if (!hasCasingInfo) return this;

        String locale2 = getCldrFileToCheck().getSourceLocaleID(path, null);
        if (locale2.equals(locale) && value != null && value.length() > 0) {
            Category category = getCategory(path);
            if (category != null) {
                checkConsistentCasing(category, path, fullPath, value, options, result);
            }
        }
        return this;
    }

    static final Matcher placeholder = PatternCache.get("\\{\\d+\\}").matcher("");

    /**
     * The casing type of a given string.
     */
    public enum CasingType {
        titlecase, lowercase, other;
        public static CasingType from(String s) {
            if (s == null || s.length() == 0) {
                return other;
            }
            int cp;
            // Look for the first meaningful character in the string to determine case.
            for (int i = 0; i < s.length(); i += Character.charCount(cp)) {
                cp = s.codePointAt(i);
                // used to skip the placeholders, but works better to have them be 'other'
                // if (cp == '{') {
                // if (placeholder.reset(s).region(i,s.length()).lookingAt()) {
                // i = placeholder.end() - 1; // skip
                // continue;
                // }
                // }
                int type = UCharacter.getType(cp);
                switch (type) {

                case UCharacter.LOWERCASE_LETTER:
                    return lowercase;

                case UCharacter.UPPERCASE_LETTER:
                case UCharacter.TITLECASE_LETTER:
                    return titlecase;

                // for other letters / numbers / symbols, return other
                case UCharacter.OTHER_LETTER:
                case UCharacter.DECIMAL_DIGIT_NUMBER:
                case UCharacter.LETTER_NUMBER:
                case UCharacter.OTHER_NUMBER:
                case UCharacter.MATH_SYMBOL:
                case UCharacter.CURRENCY_SYMBOL:
                case UCharacter.MODIFIER_SYMBOL:
                case UCharacter.OTHER_SYMBOL:
                    return other;
                // ignore everything else (whitespace, punctuation, etc) and keep going
                }
            }
            return other;
        }

        /**
         * Return true if either is other, or they are identical.
         */
        public boolean worksWith(CasingType otherType) {
            return otherType == null || this == otherType || this == CasingType.other || otherType == CasingType.other;
        }
    }

    public enum CasingTypeAndErrFlag {
        titlecase_mismatchWarn(CasingType.titlecase, false), titlecase_mismatchErr(CasingType.titlecase, true), lowercase_mismatchWarn(CasingType.lowercase,
            false), lowercase_mismatchErr(CasingType.lowercase, true), other_mismatchWarn(CasingType.other, false), other_mismatchErr(CasingType.other, true);

        private final CasingType type;
        private final boolean flag; // force error instead of warning for mismatch

        private CasingTypeAndErrFlag(CasingType type, boolean flag) {
            this.type = type;
            this.flag = flag;
        }

        public CasingType type() {
            return type;
        }

        public boolean flag() {
            return flag;
        }
    }

    static final RegexLookup<Category> pathToBucket = new RegexLookup<Category>()
        .add("//ldml/localeDisplayNames/languages/language", Category.language)
        .add("//ldml/localeDisplayNames/scripts/script", Category.script)
        .add("//ldml/localeDisplayNames/territories/territory", Category.territory)
        .add("//ldml/localeDisplayNames/variants/variant", Category.variant)
        .add("//ldml/localeDisplayNames/keys/key", Category.key)
        .add("//ldml/localeDisplayNames/types/type", Category.keyValue)
        .add("//ldml/dates/calendars/calendar.*/months.*narrow", Category.month_narrow)
        .add("//ldml/dates/calendars/calendar.*/months.*format", Category.month_format_except_narrow)
        .add("//ldml/dates/calendars/calendar.*/months", Category.month_standalone_except_narrow)
        .add("//ldml/dates/calendars/calendar.*/days.*narrow", Category.day_narrow)
        .add("//ldml/dates/calendars/calendar.*/days.*format", Category.day_format_except_narrow)
        .add("//ldml/dates/calendars/calendar.*/days", Category.day_standalone_except_narrow)
        .add("//ldml/dates/calendars/calendar.*/eras/eraNarrow", Category.era_narrow)
        .add("//ldml/dates/calendars/calendar.*/eras/eraAbbr", Category.era_abbr)
        .add("//ldml/dates/calendars/calendar.*/eras/", Category.era_name)
        .add("//ldml/dates/calendars/calendar.*/quarters.*narrow", Category.quarter_narrow)
        .add("//ldml/dates/calendars/calendar.*/quarters.*abbreviated", Category.quarter_abbreviated)
        .add("//ldml/dates/calendars/calendar.*/quarters.*format", Category.quarter_format_wide)
        .add("//ldml/dates/calendars/calendar.*/quarters", Category.quarter_standalone_wide)
        .add("//ldml/.*/relative", Category.relative)
        .add("//ldml/dates/fields", Category.calendar_field)
        .add("//ldml/dates/timeZoneNames/zone.*/exemplarCity", Category.zone_exemplarCity)
        .add("//ldml/dates/timeZoneNames/zone.*/short", Category.zone_short)
        .add("//ldml/dates/timeZoneNames/zone", Category.zone_long)
        .add("//ldml/dates/timeZoneNames/metazone.*/commonlyUsed", Category.NOT_USED) // just to remove them from the other cases
        .add("//ldml/dates/timeZoneNames/metazone.*/short", Category.metazone_long)
        .add("//ldml/dates/timeZoneNames/metazone", Category.metazone_long)
        .add("//ldml/numbers/currencies/currency.*/symbol", Category.symbol)
        .add("//ldml/numbers/currencies/currency.*/displayName.*@count", Category.currencyName_count)
        .add("//ldml/numbers/currencies/currency.*/displayName", Category.currencyName)
        .add("//ldml/units/unit.*/unitPattern.*(past|future)", Category.relative)
        .add("//ldml/units/unit.*/unitPattern", Category.unit_pattern)
    // ldml/localeDisplayNames/keys/key[@type=".*"]
    // ldml/localeDisplayNames/measurementSystemNames/measurementSystemName[@type=".*"]
    // ldml/localeDisplayNames/transformNames/transformName[@type=".*"]
    ;

    Map<Category, CasingTypeAndErrFlag> types = new EnumMap<Category, CasingTypeAndErrFlag>(Category.class);

    public enum Category {
        language, script, territory, variant, keyValue, month_narrow, month_format_except_narrow, month_standalone_except_narrow, day_narrow, day_format_except_narrow, day_standalone_except_narrow, era_narrow, era_abbr, era_name, quarter_narrow, quarter_abbreviated, quarter_format_wide, quarter_standalone_wide, calendar_field, zone_exemplarCity, zone_short, zone_long, NOT_USED, metazone_short, metazone_long, symbol, currencyName_count, currencyName, relative, unit_pattern, key;
    }

    // //ldml/numbers/currencies/currency[@type="ADP"]/displayName
    // //ldml/numbers/currencies/currency[@type="RON"]/displayName[@count="other"]
    // //ldml/numbers/currencies/currency[@type="BYB"]/symbol

    static Category getCategory(String path) {
        return pathToBucket.get(path);
    }

    /**
     * Calculates casing information using data from the specified CLDRFile.
     *
     * @param resolved
     *            the resolved CLDRFile to calculate casing information from
     * @return
     */
    public static Map<Category, CasingType> getSamples(CLDRFile resolved) {
        // Use EnumMap instead of an array for type safety.
        Map<Category, Counter<CasingType>> counters = new EnumMap<Category, Counter<CasingType>>(Category.class);

        for (Category category : Category.values()) {
            counters.put(category, new Counter<CasingType>());
        }
        PathStarrer starrer = new PathStarrer();
        boolean isRoot = "root".equals(resolved.getLocaleID());
        Set<String> missing = !DEBUG ? null : new TreeSet<String>();

        for (String path : resolved) {
            if (!isRoot) {
                String locale2 = resolved.getSourceLocaleID(path, null);
                if (locale2.equals("root") || locale2.equals("code-fallback")) {
                    continue;
                }
            }
            String winningPath = resolved.getWinningPath(path);
            if (!winningPath.equals(path)) {
                continue;
            }
            Category category = getCategory(path);
            if (category != null) {
                String value = resolved.getStringValue(path);
                if (value == null || value.length() == 0) continue;
                CasingType ft = CasingType.from(value);
                counters.get(category).add(ft, 1);
            } else if (DEBUG) {
                String starred = starrer.set(path);
                missing.add(starred);
            }
        }

        Map<Category, CasingType> info = new EnumMap<Category, CasingType>(Category.class);
        for (Category category : Category.values()) {
            if (category == Category.NOT_USED) continue;
            Counter<CasingType> counter = counters.get(category);
            long countLower = counter.getCount(CasingType.lowercase);
            long countUpper = counter.getCount(CasingType.titlecase);
            long countOther = counter.getCount(CasingType.other);
            CasingType type;
            if (countLower + countUpper == 0) {
                type = CasingType.other;
            } else if (countLower >= countUpper * MIN_FACTOR && countLower >= countOther) {
                type = CasingType.lowercase;
            } else if (countUpper >= countLower * MIN_FACTOR && countUpper >= countOther) {
                type = CasingType.titlecase;
            } else {
                type = CasingType.other;
            }
            info.put(category, type);
        }
        if (DEBUG && missing.size() != 0) {
            System.out.println("Paths skipped:\n" + CollectionUtilities.join(missing, "\n"));
        }
        return info;
    }

    private static final String CASE_WARNING = "The first letter of 〈{0}〉 is {1}, which differs from what is expected " +
        "for the {2} category: that almost all values be {3}.\n\n" +
        "For guidance, see ​http://cldr.org/translation/capitalization. " +
        "If this warning is wrong, please file a ticket at http://unicode.org/cldr/trac/.";

    private void checkConsistentCasing(Category category, String path, String fullPath, String value,
        Options options, List<CheckStatus> result) {
        // Avoid NPE
        if (types != null) {
            CasingType ft = CasingType.from(value);
            CasingTypeAndErrFlag typeAndFlagFromCat = types.get(category);
            if (typeAndFlagFromCat == null) {
                typeAndFlagFromCat = CasingTypeAndErrFlag.other_mismatchWarn;
            }
            if (!ft.worksWith(typeAndFlagFromCat.type())) {
                result.add(new CheckStatus().setCause(this)
                    .setMainType(typeAndFlagFromCat.flag() ? CheckStatus.errorType : CheckStatus.warningType)
                    .setSubtype(Subtype.incorrectCasing) // typically warningType or errorType
                    .setMessage(CASE_WARNING, value, ft, category, typeAndFlagFromCat.type())); // the message; can be MessageFormat with arguments
            }
        }
    }
}