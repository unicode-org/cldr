package org.unicode.cldr.test;

import java.util.HashSet;
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
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.PrettyPath;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;

public class CheckConsistentCasing extends CheckCLDR {

    private static final boolean DEBUG = CldrUtility.getProperty("DEBUG", false);
    
    private static final double MIN_FACTOR = 2.5;
    // remember to add this class to the list in CheckCLDR.getCheckAll
    // to run just this test, on just locales starting with 'nl', use CheckCLDR with -fnl.* -t.*Currencies.*

    XPathParts parts = new XPathParts(); // used to parse out a path
    ULocale uLocale = null;
    BreakIterator breaker = null;
    private String locale;
    private CLDRFile resolvedCldrFileToCheck2;
    PrettyPath pretty = new PrettyPath();
    Set<String> wasMissing = new HashSet();

    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Map<String, String> options, List<CheckStatus> possibleErrors) {
        if (cldrFileToCheck == null) return this;
        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        locale = cldrFileToCheck.getLocaleID();

        resolvedCldrFileToCheck2 = getResolvedCldrFileToCheck();
        getSamples(resolvedCldrFileToCheck2);
        return this;
    }

    // If you don't need any file initialization or postprocessing, you only need this one routine
    public CheckCLDR handleCheck(String path, String fullPath, String value, Map<String, String> options, List<CheckStatus> result) {
        // it helps performance to have a quick reject of most paths
        if (fullPath == null) return this; // skip paths that we don't have
        //    if (resolvedCldrFileToCheck2.getStringValue(path) == null) {
        //      System.out.println("???");
        //    }

        String locale2 = getCldrFileToCheck().getSourceLocaleID(path, null);
        if (!locale2.equals(locale)) {
            //System.out.println(locale + "\t" + uLocale);
        } else if (value != null && value.length() > 0) {
            int index = getIndex(path);
            if (index >= 0) {
                checkConsistentCasing(index, path, fullPath, value, options, result);
            }
        }
        return this;
    }

    static final Matcher placeholder = Pattern.compile("\\{\\d+\\}").matcher("");

    /**
     * The type of the first letter
     */
    enum FirstLetterType {
        uppercase, lowercase, other;
        public static FirstLetterType from(String s) {
            if (s == null || s.length() == 0) {
                return other;
            }
            int cp;
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
                    return uppercase;
                
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
        public static boolean works(FirstLetterType a, FirstLetterType b) {
            // TODO Auto-generated method stub
            return a == b || a == FirstLetterType.other || b == FirstLetterType.other;
        }
    }

    static final RegexLookup<Integer> pathToBucket = new RegexLookup<Integer>()
    .add("//ldml/localeDisplayNames/languages/language", 0)
    .add("//ldml/localeDisplayNames/scripts/script", 1)
    .add("//ldml/localeDisplayNames/territories/territory", 2)
    .add("//ldml/localeDisplayNames/variants/variant", 3)
    .add("//ldml/localeDisplayNames/variants/type", 4)
    .add("//ldml/dates/calendars/calendar.*/months.*narrow", 5)
    .add("//ldml/dates/calendars/calendar.*/months.*format", 6)
    .add("//ldml/dates/calendars/calendar.*/months", 7)
    .add("//ldml/dates/calendars/calendar.*/days.*narrow", 8)
    .add("//ldml/dates/calendars/calendar.*/days.*format", 9)
    .add("//ldml/dates/calendars/calendar.*/days", 10)
    .add("//ldml/dates/calendars/calendar.*/eras.*narrow", 11)
    .add("//ldml/dates/calendars/calendar.*/eras.*format", 12)
    .add("//ldml/dates/calendars/calendar.*/eras", 13)
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
    static final int LIMIT_COUNT = 30;

    FirstLetterType[] types = new FirstLetterType[LIMIT_COUNT];
    String[] percent = new String[LIMIT_COUNT];
    String[] samples = new String[LIMIT_COUNT];

    // //ldml/numbers/currencies/currency[@type="ADP"]/displayName
    // //ldml/numbers/currencies/currency[@type="RON"]/displayName[@count="other"]
    // //ldml/numbers/currencies/currency[@type="BYB"]/symbol

    int getIndex(String path) {
        Integer bucket = pathToBucket.get(path);
        return bucket == null ? -1 : bucket;
    }

    private void getSamples(CLDRFile unresolved) {
        Counter<FirstLetterType>[] counters = new Counter[LIMIT_COUNT];
        Set<String>[] sampleLower = new Set[LIMIT_COUNT];
        Set<String>[] sampleUpper = new Set[LIMIT_COUNT];

        for (int i = 0; i < LIMIT_COUNT; ++i) {
            counters[i] = new Counter<FirstLetterType>();
            sampleLower[i] = new HashSet<String>();
            sampleUpper[i] = new HashSet<String>();
        }
        
        Set<String> items = new TreeSet<String>(CLDRFile.ldmlComparator);
        Iterator<String> it = unresolved.iterator();
        CollectionUtilities.addAll(it, items);
        unresolved.getExtraPaths(items);
        boolean isRoot = "root".equals(unresolved.getLocaleID());
        PathStarrer starrer = !DEBUG ? null : new PathStarrer();
        Set<String> missing = !DEBUG ? null : new TreeSet<String>();

        for (String path : items) {
            //      if (path.contains("displayName") && path.contains("count")) {
            //        System.out.println("count");
            //      }
            if (!isRoot) {
                String locale2 = getCldrFileToCheck().getSourceLocaleID(path, null);
                if (locale2.equals("root") || locale2.equals("code-fallback")) {
                    continue;
                }
            }
            String winningPath = unresolved.getWinningPath(path);
            if (!winningPath.equals(path)) {
                continue;
            }
            // System.out.println(locale2 + "\t\t" + path);
            int i = getIndex(path);
            if (i >= 0) {
                String value = unresolved.getStringValue(path);
                if (value == null || value.length() == 0) continue;
                FirstLetterType ft = FirstLetterType.from(value);
                counters[i].add(ft, 1);
                switch (ft) {
                case lowercase: sampleLower[i].add(value); break;
                case uppercase: sampleUpper[i].add(value); break;
                }
            } else if (DEBUG) {
                String starred = starrer.set(path);
                missing.add(starred);
            }
        }
        
        for (int i = 0; i < LIMIT_COUNT; ++i) {
            long countLower = counters[i].getCount(FirstLetterType.lowercase);
            long countUpper = counters[i].getCount(FirstLetterType.uppercase);
            long countOther = counters[i].getCount(FirstLetterType.other);
            if (countLower >= countUpper * MIN_FACTOR && countLower >= countOther) {
                types[i] = FirstLetterType.lowercase;
                samples[i] = extractSamples(sampleLower[i]);
                setPercent(i, countLower, countLower + countUpper);
            } else if (countUpper >= countLower * MIN_FACTOR && countUpper >= countOther) {
                types[i] = FirstLetterType.uppercase;
                samples[i] = extractSamples(sampleUpper[i]);
                setPercent(i, countUpper, countLower + countUpper);
            } else {
                types[i] = FirstLetterType.other;
            }
        }
        if (DEBUG && missing.size() != 0) {
            missing.removeAll(wasMissing);
            System.out.println("Paths skipped:\n" + CollectionUtilities.join(missing, "\n"));
            wasMissing.addAll(missing);
        }
    }

    private String extractSamples(Set<String> values) {
        StringBuffer stringBuffer = new StringBuffer();
        // we count on the hash to randomize the results.
        for (String value : values) {
            if (stringBuffer.length() != 0) {
                stringBuffer.append("; ");
            }
            stringBuffer.append("〈").append(value).append("〉");
            if (stringBuffer.length() > 50) break;
        }
        return stringBuffer.toString();
    }

    private void setPercent(int i, long countLower, long total) {
        percent[i] = percentFormat.format(countLower/(double)total);
        if ("100%".equals(percent[i])) {
            percent[i] = "99+%";
        }
    }

    static final NumberFormat percentFormat = NumberFormat.getPercentInstance(ULocale.ENGLISH);
    
    private void checkConsistentCasing(int i, String path, String fullPath, String value,
            Map<String, String> options, List<CheckStatus> result) {
        //    if (path.contains("displayName") && path.contains("count")) {
        //      System.out.println("count");
        //    }
        FirstLetterType ft = FirstLetterType.from(value);
        if (!FirstLetterType.works(ft, types[i])) {
            result.add(new CheckStatus().setCause(this)
                    .setMainType(CheckStatus.warningType)
                    .setSubtype(Subtype.incorrectCasing) // typically warningType or errorType
                    .setMessage("The first letter of 〈{0}〉 is {1}, which differs from the majority of this type: {2}={3}, such as {4}…", 
                            new Object[]{value, ft, percent[i], types[i], samples[i]})); // the message; can be MessageFormat with arguments
        }
    }
}