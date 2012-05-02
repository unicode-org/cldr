package org.unicode.cldr.test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.util.CollectionUtilities;
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
        casingInfo = new CasingInfo(factory.getSupplementalDirectory().getAbsolutePath()+"/../casing"); // TODO: fix.
    }

    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Map<String, String> options, List<CheckStatus> possibleErrors) {
        if (cldrFileToCheck == null) return this;
        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        locale = cldrFileToCheck.getLocaleID();

        Map<String, CasingType> casing = casingInfo.getLocaleCasing(locale);
        if(casing == null) {
            possibleErrors.add(new CheckStatus().setCause(this)
                .setMainType(CheckStatus.warningType)
                .setSubtype(Subtype.incorrectCasing)
                .setMessage("Could not load casing info for {0}", locale));
            hasCasingInfo = false;
        } else {
            for (int i = 0; i < typeNames.length; i++) {
                types[i] = casing.get(typeNames[i]);
                if (types[i] == null) types[i] = CasingType.other;
            }
            hasCasingInfo = casing.size() > 0;
        }
        return this;
    }

    // If you don't need any file initialization or postprocessing, you only need this one routine
    public CheckCLDR handleCheck(String path, String fullPath, String value, Map<String, String> options, List<CheckStatus> result) {
        // it helps performance to have a quick reject of most paths
        if (fullPath == null) return this; // skip paths that we don't have
        if (!hasCasingInfo) return this;

        String locale2 = getCldrFileToCheck().getSourceLocaleID(path, null);
        if (locale2.equals(locale) && value != null && value.length() > 0) {
            int index = getIndex(path);
            if (index >= 0) {
                checkConsistentCasing(index, path, fullPath, value, options, result);
            }
        }
        return this;
    }

    static final Matcher placeholder = Pattern.compile("\\{\\d+\\}").matcher("");

    /**
     * The casing type of a given string.
     */
    enum CasingType {
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
//                if (cp == '{') {
//                    if (placeholder.reset(s).region(i,s.length()).lookingAt()) {
//                        i = placeholder.end() - 1; // skip
//                        continue;
//                    }
//                }
                int type = UCharacter.getType(cp);
                switch(type) {
                
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
                case UCharacter.MATH_SYMBOL :
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
         * @param ft
         * @param firstLetterType
         * @return
         */
        public static boolean works(CasingType a, CasingType b) {
            return a == b || a == CasingType.other || b == CasingType.other;
        }
    }

    static final RegexLookup<Integer> pathToBucket = new RegexLookup<Integer>()
    .add("//ldml/localeDisplayNames/languages/language", 0)
    .add("//ldml/localeDisplayNames/scripts/script", 1)
    .add("//ldml/localeDisplayNames/territories/territory", 2)
    .add("//ldml/localeDisplayNames/variants/variant", 3)
    .add("//ldml/localeDisplayNames/keys/key", 30)
    .add("//ldml/localeDisplayNames/types/type", 4)
    .add("//ldml/dates/calendars/calendar.*/months.*narrow", 5)
    .add("//ldml/dates/calendars/calendar.*/months.*format", 6)
    .add("//ldml/dates/calendars/calendar.*/months", 7)
    .add("//ldml/dates/calendars/calendar.*/days.*narrow", 8)
    .add("//ldml/dates/calendars/calendar.*/days.*format", 9)
    .add("//ldml/dates/calendars/calendar.*/days", 10)
    .add("//ldml/dates/calendars/calendar.*/eras/eraNarrow", 11)
    .add("//ldml/dates/calendars/calendar.*/eras/eraAbbr", 12)
    .add("//ldml/dates/calendars/calendar.*/eras/", 13)
    .add("//ldml/dates/calendars/calendar.*/quarters.*narrow", 14)
    .add("//ldml/dates/calendars/calendar.*/quarters.*abbreviated", 15)
    .add("//ldml/dates/calendars/calendar.*/quarters.*format", 16)
    .add("//ldml/dates/calendars/calendar.*/quarters", 17)
    .add("//ldml/.*/relative", 28)
    .add("//ldml/dates/calendars/calendar.*/fields", 18)
    .add("//ldml/dates/timeZoneNames/zone.*/exemplarCity", 19)
    .add("//ldml/dates/timeZoneNames/zone.*/short", 20)
    .add("//ldml/dates/timeZoneNames/zone", 21)
    .add("//ldml/dates/timeZoneNames/metazone.*/commonlyUsed", 22) // just to remove them from the other cases
    .add("//ldml/dates/timeZoneNames/metazone.*/short", 23)
    .add("//ldml/dates/timeZoneNames/metazone", 24)
    .add("//ldml/numbers/currencies/currency.*/symbol", 25)
    .add("//ldml/numbers/currencies/currency.*/displayName.*@count", 26)
    .add("//ldml/numbers/currencies/currency.*/displayName", 27)
    .add("//ldml/units/unit.*/unitPattern.*(past|future)", 28)
    .add("//ldml/units/unit.*/unitPattern", 29)
    //ldml/localeDisplayNames/keys/key[@type=".*"]
    //ldml/localeDisplayNames/measurementSystemNames/measurementSystemName[@type=".*"]
    //ldml/localeDisplayNames/transformNames/transformName[@type=".*"]
    ;

    public static final int LIMIT_COUNT = 31;

    CasingType[] types = new CasingType[LIMIT_COUNT];

    public static String NOT_USED = "NOT_USED";
 
    public static String[] typeNames = new String[] {
        "language","script", "territory", "variant", "type",
        "month-narrow", "month-format-except-narrow", "month-standalone-except-narrow",
        "day-narrow", "day-format-except-narrow", "day-standalone-except-narrow",
        "era-narrow", "era-abbr", "era-name",
        "quarter-narrow", "quarter-abbreviated", "quarter-format-wide", "quarter-standalone-wide",
        "calendar-field",
        "zone-exemplarCity", "zone-short", "zone-long",
        NOT_USED,
        "metazone-short", "metazone-long",
        "symbol",
        "displayName-count", "displayName",
        "tense", "unit-pattern",
        "key"
    };

    // //ldml/numbers/currencies/currency[@type="ADP"]/displayName
    // //ldml/numbers/currencies/currency[@type="RON"]/displayName[@count="other"]
    // //ldml/numbers/currencies/currency[@type="BYB"]/symbol

    static int getIndex(String path) {
        Integer bucket = pathToBucket.get(path);
        return bucket == null ? -1 : bucket;
    }

    /**
     * Calculates casing information using data from the specified CLDRFile. 
     * @param resolved the resolved CLDRFile to calculate casing information from
     * @return
     */
    public static Map<String, CasingType> getSamples(CLDRFile resolved) {
        Counter<CasingType>[] counters = new Counter[LIMIT_COUNT];

        for (int i = 0; i < LIMIT_COUNT; ++i) {
            counters[i] = new Counter<CasingType>();
        }
        PathStarrer starrer = new PathStarrer();
        Set<String> items = new TreeSet<String>(CLDRFile.ldmlComparator);
        Iterator<String> it = resolved.iterator();
        CollectionUtilities.addAll(it, items);
        resolved.getExtraPaths(items);
        boolean isRoot = "root".equals(resolved.getLocaleID());
        Set<String> missing = !DEBUG ? null : new TreeSet<String>();

        for (String path : items) {
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
            int i = getIndex(path);
            if (i >= 0) {
                String value = resolved.getStringValue(path);
                if (value == null || value.length() == 0) continue;
                CasingType ft = CasingType.from(value);
                counters[i].add(ft, 1);
            } else if (DEBUG) {
                String starred = starrer.set(path);
                missing.add(starred);
            }
        }
        
        CasingType[] types = new CasingType[LIMIT_COUNT];
        Map<String, CasingType> info = new HashMap();
        for (int i = 0; i < LIMIT_COUNT; ++i) {
            long countLower = counters[i].getCount(CasingType.lowercase);
            long countUpper = counters[i].getCount(CasingType.titlecase);
            long countOther = counters[i].getCount(CasingType.other);
            if (countLower + countUpper == 0) {
                types[i] = CasingType.other;
            } else if (countLower >= countUpper * MIN_FACTOR && countLower >= countOther) {
                types[i] = CasingType.lowercase;
            } else if (countUpper >= countLower * MIN_FACTOR && countUpper >= countOther) {
                types[i] = CasingType.titlecase;
            } else {
                types[i] = CasingType.other;
            }
            if (!typeNames[i].equals(CheckConsistentCasing.NOT_USED)) {
                info.put(typeNames[i], types[i]);
            }
        }
        if (DEBUG && missing.size() != 0) {
            System.out.println("Paths skipped:\n" + CollectionUtilities.join(missing, "\n"));
        }
        return info;
    }
    
    private void checkConsistentCasing(int i, String path, String fullPath, String value,
            Map<String, String> options, List<CheckStatus> result) {
        CasingType ft = CasingType.from(value);
        if (!CasingType.works(ft, types[i])) {
            result.add(new CheckStatus().setCause(this)
                    .setMainType(CheckStatus.warningType)
                    .setSubtype(Subtype.incorrectCasing) // typically warningType or errorType
                    .setMessage("The first letter of 〈{0}〉 is {1}, which differs from the majority of this type: {2}", 
                            value, ft, types[i])); // the message; can be MessageFormat with arguments
        }
    }
}