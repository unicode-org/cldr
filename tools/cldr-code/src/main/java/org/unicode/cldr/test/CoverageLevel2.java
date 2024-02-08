package org.unicode.cldr.test;

import static java.util.Collections.disjoint;

import com.ibm.icu.util.Output;
import com.ibm.icu.util.VersionInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import org.unicode.cldr.tool.ToolConfig;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility.VariableReplacer;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.RegexLookup.Finder;
import org.unicode.cldr.util.RegexLookup.RegexFinder;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.ApprovalRequirementMatcher;
import org.unicode.cldr.util.SupplementalDataInfo.CoverageLevelInfo;
import org.unicode.cldr.util.SupplementalDataInfo.CoverageVariableInfo;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

public class CoverageLevel2 {

    // To modify the results, see /cldr/common/supplemental/coverageLevels.xml

    /** Enable to get more verbose output when debugging */
    private static final boolean DEBUG_LOOKUP = false;

    private RegexLookup<Level> lookup = null;

    enum SetMatchType {
        Target_Language,
        Target_Scripts,
        Target_Territories,
        Target_TimeZones,
        Target_Currencies,
        Target_Plurals,
        Calendar_List
    }

    private static class LocaleSpecificInfo {
        CoverageVariableInfo cvi;
        String targetLanguage;
    }

    final LocaleSpecificInfo myInfo = new LocaleSpecificInfo();

    /**
     * We define a regex finder for use in the lookup. It has extra tests based on the ci value and
     * the cvi value, duplicating what was in SupplementalDataInfo. It uses the sets instead of
     * converting to regex strings.
     *
     * @author markdavis
     */
    public static class MyRegexFinder extends RegexFinder {
        private final SetMatchType additionalMatch;
        private final CoverageLevelInfo ci;

        public MyRegexFinder(String pattern, String additionalMatch, CoverageLevelInfo ci) {
            super(pattern);
            // remove the ${ and the }, and change - to _.
            this.additionalMatch =
                    additionalMatch == null
                            ? null
                            : SetMatchType.valueOf(
                                    additionalMatch
                                            .substring(2, additionalMatch.length() - 1)
                                            .replace('-', '_'));
            this.ci = ci;
        }

        @Override
        public boolean find(String item, Object context, Info info) {
            LocaleSpecificInfo localeSpecificInfo = (LocaleSpecificInfo) context;
            // Modified the logic to handle the case where we want specific languages and specific
            // territories.
            // Any match in language script or territory will succeed when multiple items are
            // present.
            boolean lstOK = false;
            if (ci.inLanguage == null && ci.inScriptSet == null && ci.inTerritorySet == null) {
                lstOK = true;
            } else if (ci.inLanguage != null
                    && ci.inLanguage.matcher(localeSpecificInfo.targetLanguage).matches()) {
                lstOK = true;
            } else if (ci.inScriptSet != null
                    && !disjoint(ci.inScriptSet, localeSpecificInfo.cvi.targetScripts)) {
                lstOK = true;
            } else if (ci.inTerritorySet != null
                    && !disjoint(ci.inTerritorySet, localeSpecificInfo.cvi.targetTerritories)) {
                lstOK = true;
            }

            if (!lstOK) {
                return false;
            }
            boolean result = super.find(item, context, info); // also sets matcher in RegexFinder
            if (!result) {
                return false;
            }
            if (additionalMatch != null) {
                String groupMatch = info.value[1];
                //                    String groupMatch = matcher.group(1);
                // we match on a group, so get the right one
                switch (additionalMatch) {
                    case Target_Language:
                        return localeSpecificInfo.targetLanguage.equals(groupMatch);
                    case Target_Scripts:
                        return localeSpecificInfo.cvi.targetScripts.contains(groupMatch);
                    case Target_Territories:
                        return localeSpecificInfo.cvi.targetTerritories.contains(groupMatch);
                    case Target_TimeZones:
                        return localeSpecificInfo.cvi.targetTimeZones.contains(groupMatch);
                    case Target_Currencies:
                        return localeSpecificInfo.cvi.targetCurrencies.contains(groupMatch);
                        // For Target_Plurals, we have to account for the fact that the @count= part
                        // might not be in the
                        // xpath, so we shouldn't reject the match because of that. ( i.e. The regex
                        // is usually
                        // ([@count='${Target-Plurals}'])?
                    case Target_Plurals:
                        return (groupMatch == null
                                || groupMatch.length() == 0
                                || localeSpecificInfo.cvi.targetPlurals.contains(groupMatch));
                    case Calendar_List:
                        return localeSpecificInfo.cvi.calendars.contains(groupMatch);
                }
            }

            return true;
        }

        @Override
        public boolean equals(Object obj) {
            return false;
        }
    }

    private CoverageLevel2(SupplementalDataInfo sdi, String locale) {
        myInfo.targetLanguage = CLDRLocale.getInstance(locale).getLanguage();
        myInfo.cvi = sdi.getCoverageVariableInfo(myInfo.targetLanguage);
        lookup = sdi.getCoverageLookup();
    }

    private CoverageLevel2(SupplementalDataInfo sdi, String locale, String ruleFile) {
        myInfo.targetLanguage = CLDRLocale.getInstance(locale).getLanguage();
        myInfo.cvi = sdi.getCoverageVariableInfo(myInfo.targetLanguage);
        RawCoverageFile rcf = new RawCoverageFile();
        lookup = rcf.load(ruleFile);
    }

    /**
     * get an instance, using CldrUtility.SUPPLEMENTAL_DIRECTORY
     *
     * @param locale
     * @return
     * @deprecated Don't use this. call the version which takes a SupplementalDataInfo as an
     *     argument.
     * @see #getInstance(SupplementalDataInfo, String)
     * @see CLDRPaths#SUPPLEMENTAL_DIRECTORY
     */
    @Deprecated
    public static CoverageLevel2 getInstance(String locale) {
        return new CoverageLevel2(SupplementalDataInfo.getInstance(), locale);
    }

    public static CoverageLevel2 getInstance(SupplementalDataInfo sdi, String locale) {
        return new CoverageLevel2(sdi, locale);
    }

    public static CoverageLevel2 getInstance(
            SupplementalDataInfo sdi, String locale, String ruleFile) {
        return new CoverageLevel2(sdi, locale, ruleFile);
    }

    public Level getLevel(String path) {
        if (path == null) {
            return Level.UNDETERMINED;
        }
        synchronized (
                lookup) { // synchronize on the class, since the Matchers are changed during the
            // matching process
            Level result;
            if (DEBUG_LOOKUP) { // for testing
                Output<String[]> checkItems = new Output<>();
                Output<Finder> matcherFound = new Output<>();
                List<String> failures = new ArrayList<>();
                result = lookup.get(path, myInfo, checkItems, matcherFound, failures);
                for (String s : failures) {
                    System.out.println(s);
                }
            } else {
                result = lookup.get(path, myInfo, null);
            }
            return result == null ? Level.COMPREHENSIVE : result;
        }
    }

    public int getIntLevel(String path) {
        return getLevel(path).getLevel();
    }

    // Moved code in from SupplementalInfo
    //
    // TODO:
    // 1. drop the corresponding code in SupplementalInfo.
    // 2. change SupplementalInfo to skip reading coverageLevels.xml
    // 3. change the default creation of CoverageLevels2 to instead use this code with that file.
    // Later
    // 4. Generalize the RawCoverageFile code, and use with other supplemental files.
    //    That way supplemental files can be read as needed instead of all at once.

    private final List<String> approvalRequirements = new LinkedList<>(); // xpath array
    private VariableReplacer coverageVariables = new VariableReplacer();
    private SortedSet<CoverageLevelInfo> coverageLevels = new TreeSet<>();

    public class RawCoverageFile {

        private VersionInfo cldrVersion;

        class MyHandler extends XMLFileReader.SimpleHandler {
            @Override
            public void handlePathValue(String path, String pathValue) {
                XPathParts parts = XPathParts.getFrozenInstance(path);
                String level1 = parts.size() < 2 ? null : parts.getElement(1);
                if (level1.equals("version")) {
                    if (cldrVersion == null) {
                        String version = parts.getAttributeValue(1, "cldrVersion");
                        if (version == null) {
                            version = parts.getAttributeValue(0, "version");
                        }
                        cldrVersion = VersionInfo.getInstance(version);
                    }
                } else if (parts.containsElement("approvalRequirement")) {
                    approvalRequirements.add(parts.toString());
                } else if (parts.containsElement("coverageLevel")) {
                    String match =
                            parts.containsAttribute("match")
                                    ? coverageVariables.replace(
                                            parts.getAttributeValue(-1, "match"))
                                    : null;
                    String valueStr = parts.getAttributeValue(-1, "value");
                    // Ticket 7125: map the number to English. So switch from English to number for
                    // construction
                    valueStr = Integer.toString(Level.get(valueStr).getLevel());

                    String inLanguage =
                            parts.containsAttribute("inLanguage")
                                    ? coverageVariables.replace(
                                            parts.getAttributeValue(-1, "inLanguage"))
                                    : null;
                    String inScript =
                            parts.containsAttribute("inScript")
                                    ? coverageVariables.replace(
                                            parts.getAttributeValue(-1, "inScript"))
                                    : null;
                    String inTerritory =
                            parts.containsAttribute("inTerritory")
                                    ? coverageVariables.replace(
                                            parts.getAttributeValue(-1, "inTerritory"))
                                    : null;
                    Integer value =
                            (valueStr != null) ? Integer.valueOf(valueStr) : Integer.valueOf("101");
                    if (cldrVersion.getMajor() < 2) {
                        value = 40;
                    }
                    CoverageLevelInfo ci =
                            new CoverageLevelInfo(match, value, inLanguage, inScript, inTerritory);
                    coverageLevels.add(ci);
                } else if (parts.containsElement("coverageVariable")) {
                    String key = parts.getAttributeValue(-1, "key");
                    String value = parts.getAttributeValue(-1, "value");
                    coverageVariables.add(key, value);
                }
            }

            public void cleanup() {
                CLDRConfig testInfo = ToolConfig.getToolInstance();
                SupplementalDataInfo supplementalDataInfo2 = testInfo.getSupplementalDataInfo();
                CoverageLevelInfo.fixEU(coverageLevels, supplementalDataInfo2);
                coverageLevels = Collections.unmodifiableSortedSet(coverageLevels);
            }
        }

        public RegexLookup<Level> makeCoverageLookup() {
            RegexLookup<Level> lookup =
                    new RegexLookup<>(RegexLookup.LookupType.STAR_PATTERN_LOOKUP);

            Matcher variable = PatternCache.get("\\$\\{[A-Za-z][\\-A-Za-z]*\\}").matcher("");

            for (CoverageLevelInfo ci : coverageLevels) {
                String pattern =
                        ci.match
                                .replace('\'', '"')
                                .replace("[@", "\\[@") // make sure that attributes are quoted
                                .replace("(", "(?:") // make sure that there are no capturing groups
                                // (beyond what we generate
                                .replace("(?:?!", "(?!"); // Allow negative lookahead
                pattern = "^//ldml/" + pattern + "$"; // for now, force a complete match
                String variableType = null;
                variable.reset(pattern);
                if (variable.find()) {
                    pattern =
                            pattern.substring(0, variable.start())
                                    + "([^\"]*)"
                                    + pattern.substring(variable.end());
                    variableType = variable.group();
                    if (variable.find()) {
                        throw new IllegalArgumentException(
                                "We can only handle a single variable on a line");
                    }
                }

                // .replaceAll("\\]","\\\\]");
                lookup.add(new CoverageLevel2.MyRegexFinder(pattern, variableType, ci), ci.value);
            }
            return lookup;
        }

        public RegexLookup<Level> load(String file) {
            MyHandler myHandler = new MyHandler();
            XMLFileReader xfr = new XMLFileReader().setHandler(myHandler);
            xfr.read(file, -1, true);
            myHandler.cleanup();
            return makeCoverageLookup();
        }
    }

    // run these from first to last to get the approval info.
    volatile List<ApprovalRequirementMatcher> approvalMatchers = null;

    /**
     * Get the preliminary number of required votes based on the given locale and PathHeader
     *
     * <p>Important: this number may not agree with VoteResolver.getRequiredVotes since VoteResolver
     * also takes the baseline status into account.
     *
     * <p>Called by VoteResolver, ShowStarredCoverage, TestCoverage, and TestCoverageLevel.
     *
     * @param loc the CLDRLocale
     * @param ph the PathHeader - which path this is applied to, or null if unknown.
     * @return a number such as 4 or 8
     */
    public int getRequiredVotes(CLDRLocale loc, PathHeader ph) {
        if (approvalMatchers == null) {
            approvalMatchers = ApprovalRequirementMatcher.buildAll(approvalRequirements);
        }

        for (ApprovalRequirementMatcher m : approvalMatchers) {
            if (m.matches(loc, ph)) {
                return m.getRequiredVotes();
            }
        }
        throw new RuntimeException(
                "Error: " + loc + " " + ph + " ran off the end of the approvalMatchers.");
    }

    // TODO: move to separate tool

    public static void main(String[] args) {
        // Quick test during development to compare old to new coverageLevels

        checkCoverage("root");
        checkCoverage("de");
    }

    private static void checkCoverage(String locale) {
        final CLDRConfig testInfo = ToolConfig.getToolInstance();
        final SupplementalDataInfo supplementalDataInfo2 = testInfo.getSupplementalDataInfo();

        CoverageLevel2 cvOld = CoverageLevel2.getInstance(supplementalDataInfo2, locale);

        CoverageLevel2 cvNew =
                CoverageLevel2.getInstance(
                        supplementalDataInfo2,
                        locale,
                        CLDRPaths.COMMON_DIRECTORY + "supplemental-temp/coverageLevels2.xml");

        CLDRFile cldrFile = testInfo.getCldrFactory().make(locale, true);
        Set<String> paths = Builder.with(new TreeSet<String>()).addAll(cldrFile).get();
        PathHeader.Factory phf = PathHeader.getFactory();
        Map<PathHeader, String> diff = new TreeMap<>();
        Map<PathHeader, String> same = new TreeMap<>();
        for (String path : paths) {
            Level levelOld = cvOld.getLevel(path);
            Level levelNew = cvNew.getLevel(path);
            if (levelOld != levelNew) {
                diff.put(
                        phf.fromPath(path),
                        locale + "\t" + levelOld + "\t" + levelNew + "\t" + path);
            } else if (levelOld.compareTo(Level.MODERATE) < 0) {
                same.put(phf.fromPath(path), locale + "\t" + path);
            }
        }
        System.out.println("\nLocale\tPath\tPathHeader");
        for (Entry<PathHeader, String> line : same.entrySet()) {
            System.out.println(line.getValue() + "\t" + line.getKey());
        }
        System.out.println("\nLocale\tOld\tNew\tPath\tPathHeader");
        for (Entry<PathHeader, String> line : diff.entrySet()) {
            System.out.println(line.getValue() + "\t" + line.getKey());
        }
    }
}
