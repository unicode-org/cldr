package org.unicode.cldr.test;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.unittest.TestAll;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.Timer;
import org.unicode.cldr.util.RegexLookup.RegexFinder;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.CoverageLevelInfo;
import org.unicode.cldr.util.SupplementalDataInfo.CoverageVariableInfo;

import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.util.ULocale;

public class CoverageLevel2 {
    @SuppressWarnings("deprecation")
    private static final SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo.getInstance(CldrUtility.SUPPLEMENTAL_DIRECTORY);
    private static RegexLookup<Level> lookup = new RegexLookup<Level>();
    
    enum SetMatchType {Target_Language, Target_Scripts, Target_Territories, Target_TimeZones, Target_Currencies, Calendar_List}

    private static class LocaleSpecificInfo {
        CoverageVariableInfo cvi;
        String targetLanguage;
    }
    
    final LocaleSpecificInfo myInfo = new LocaleSpecificInfo();

    /**
     * We define a regex finder for use in the lookup. It has extra tests based on the ci value and the cvi value, duplicating
     * what was in SupplementalDataInfo. It uses the sets instead of converting to regex strings.
     * @author markdavis
     *
     */
    private static class MyRegexFinder extends RegexFinder {
        final private SetMatchType additionalMatch;
        final private CoverageLevelInfo ci;
        
        public MyRegexFinder(String pattern, String additionalMatch, CoverageLevelInfo ci) {
            super(pattern);
            // remove the ${ and the }, and change - to _.
            this.additionalMatch = additionalMatch == null ? null : SetMatchType.valueOf(additionalMatch.substring(2, additionalMatch.length()-1).replace('-', '_'));
            this.ci = ci;
        }
        @Override
        public boolean find(String item, Object context) {
            LocaleSpecificInfo localeSpecificInfo = (LocaleSpecificInfo) context;
            if (ci.inLanguageSet != null 
                    && !ci.inLanguageSet.contains(localeSpecificInfo.targetLanguage)) {
               return false;
            }
            if (ci.inScriptSet != null 
                    && CollectionUtilities.containsNone(ci.inScriptSet, localeSpecificInfo.cvi.targetScripts)) {
                return false;
             }
            if (ci.inTerritorySet != null 
                    && CollectionUtilities.containsNone(ci.inTerritorySet, localeSpecificInfo.cvi.targetTerritories)) {
                return false;
             }
            boolean result = super.find(item, context); // also sets matcher in RegexFinder
            if (!result) {
                return false;
            }
            if (additionalMatch != null) {
                String groupMatch = matcher.group(1);
                // we match on a group, so get the right one
                switch (additionalMatch) {
                case Target_Language: return localeSpecificInfo.targetLanguage.equals(groupMatch);
                case Target_Scripts: return localeSpecificInfo.cvi.targetScripts.contains(groupMatch);
                case Target_Territories: return localeSpecificInfo.cvi.targetTerritories.contains(groupMatch);
                case Target_TimeZones: return localeSpecificInfo.cvi.targetTimeZones.contains(groupMatch);
                case Target_Currencies: return localeSpecificInfo.cvi.targetCurrencies.contains(groupMatch);
                case Calendar_List: return localeSpecificInfo.cvi.calendars.contains(groupMatch);
                }
            }
            return true;
        }
        @Override
        public boolean equals(Object obj) {
            return false;
        }
    }

    /*
     * Set up the RegexLookup by building the right patterns.
     */
    static {
        Matcher variable = Pattern.compile("\\$\\{[\\-A-Za-z]*\\}").matcher("");
        
        for (CoverageLevelInfo ci : supplementalDataInfo.getCoverageLevelInfo()) {
            String pattern = ci.match.replace('\'','"')
                    .replace("[@","\\[@") // make sure that attributes are quoted
                    .replace("(","(?:") // make sure that there are no capturing groups (beyond what we generate below).
                    ;
            pattern = "^//ldml/" + pattern + "$"; // for now, force a complete match
            String variableType = null;
            variable.reset(pattern);
            if (variable.find()) {
                pattern = pattern.substring(0,variable.start()) + "([^\"]*)" + pattern.substring(variable.end());
                variableType = variable.group();
                if (variable.find()) {
                    throw new IllegalArgumentException("We can only handle a single variable on a line");
                }
            }
            
            //.replaceAll("\\]","\\\\]");
            lookup.add(new MyRegexFinder(pattern, variableType, ci), ci.value);
        }
    }

    private CoverageLevel2(String locale) {
        myInfo.targetLanguage = new LanguageTagParser().set(locale).getLanguage();
        myInfo.cvi = supplementalDataInfo.getCoverageVariableInfo(myInfo.targetLanguage);
    }

    public static CoverageLevel2 getInstance(String locale) {
        return new CoverageLevel2(locale);
    }

    public Level getLevel(String path) {
        synchronized (lookup) { // synchronize on the class, since the Matchers are changed during the matching process
            Level result = lookup.get(path, myInfo, null);
            return result == null ? Level.OPTIONAL : result;
        }
    }

    public int getIntLevel(String path) {
        return getLevel(path).getLevel();
    }
    
    public static void main(String[] args) {
        // quick test
        // TODO convert to unit test
        CoverageLevel2 cv2 = CoverageLevel2.getInstance("en");
        ULocale uloc = new ULocale("en");
        TestInfo testInfo = TestAll.TestInfo.getInstance();
        SupplementalDataInfo supplementalDataInfo2 = testInfo.getSupplementalDataInfo();
        CLDRFile englishPaths1 = testInfo.getEnglish();
        Set<String> englishPaths = Builder.with(new TreeSet<String>()).addAll(englishPaths1).get();

        Timer timer = new Timer();
        timer.start();
        for (String path : englishPaths) {
            int oldLevel = supplementalDataInfo2.getCoverageValueOld(path, uloc);
        }
        long oldTime = timer.getDuration();
        System.out.println(timer.toString(1));
        
        timer.start();
        for (String path : englishPaths) {
            int newLevel = cv2.getIntLevel(path);
        }
        System.out.println(timer.toString(1, oldTime));

        for (String path : englishPaths) {
            int newLevel = cv2.getIntLevel(path);
            int oldLevel = supplementalDataInfo2.getCoverageValueOld(path, uloc);
            if (newLevel != oldLevel) {
                newLevel = cv2.getIntLevel(path);
                System.out.println(oldLevel + "\t" + newLevel + "\t" + path);
            }
        }
    }
}
