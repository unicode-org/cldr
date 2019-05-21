package org.unicode.cldr.unittest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.cldr.tool.GenerateMaximalLocales;
import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.ChainedMap.M3;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData.Type;
import org.unicode.cldr.util.SupplementalDataInfo.OfficialStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row.R2;

public class TestInheritance extends TestFmwk {

    static CLDRConfig testInfo = CLDRConfig.getInstance();

    private static boolean DEBUG = CldrUtility.getProperty("DEBUG", false);

    private static Matcher pathMatcher = PatternCache.get(
        CldrUtility.getProperty("XPATH", ".*")).matcher("");

    public static void main(String[] args) throws IOException {
        new TestInheritance().run(args);
    }

    private static final SupplementalDataInfo dataInfo = SupplementalDataInfo
        .getInstance();
    private static final Set<String> defaultContents = dataInfo
        .getDefaultContentLocales();

    private static final boolean EXPECT_EQUALITY = false;

    private static Set<String> availableLocales = testInfo.getFullCldrFactory().getAvailable();

    public void TestLocalesHaveOfficial() {
        // If we have a language, we have all the region locales where the
        // language is official
        Set<String> SKIP_TERRITORIES = new HashSet<String>(Arrays.asList("001",
            "150"));
        for (Entry<String, R2<List<String>, String>> s : dataInfo
            .getLocaleAliasInfo().get("territory").entrySet()) {
            SKIP_TERRITORIES.add(s.getKey());
        }

        LanguageTagParser ltp = new LanguageTagParser();

        Relation<String, String> languageLocalesSeen = Relation.of(
            new TreeMap<String, Set<String>>(), TreeSet.class);

        Set<String> testOrg = testInfo.getStandardCodes()
            .getLocaleCoverageLocales("google");
        ChainedMap.M4<String, OfficialStatus, String, Boolean> languageToOfficialChildren = ChainedMap
            .of(new TreeMap<String, Object>(),
                new TreeMap<OfficialStatus, Object>(),
                new TreeMap<String, Object>(), Boolean.class);

        // gather the data

        for (String language : dataInfo
            .getLanguagesForTerritoriesPopulationData()) {
            for (String territory : dataInfo
                .getTerritoriesForPopulationData(language)) {
                if (SKIP_TERRITORIES.contains(territory)) {
                    continue;
                }
                PopulationData data = dataInfo
                    .getLanguageAndTerritoryPopulationData(language,
                        territory);
                OfficialStatus status = data.getOfficialStatus();
                if (data.getOfficialStatus() != OfficialStatus.unknown) {
                    String locale = removeScript(language + "_" + territory);
                    String lang = removeScript(ltp.set(locale).getLanguage());
                    languageToOfficialChildren.put(lang, status, locale,
                        Boolean.TRUE);
                    languageLocalesSeen.put(lang, locale);
                }
            }
        }

        // flesh it out by adding 'clean' codes.
        // also get the child locales in cldr.

        Relation<String, String> languageToChildren = Relation.of(
            new TreeMap<String, Set<String>>(), TreeSet.class);
        for (String locale : testInfo.getCldrFactory().getAvailable()) {
            String lang = ltp.set(locale).getLanguage();
            if (SKIP_TERRITORIES.contains(ltp.getRegion())) {
                continue;
            }
            lang = removeScript(lang);
            locale = removeScript(locale);

            if (!lang.equals(locale)) {
                languageToChildren.put(lang, locale);
                Set<String> localesSeen = languageLocalesSeen.get(lang);
                if (localesSeen == null || !localesSeen.contains(locale)) {
                    languageToOfficialChildren.put(lang,
                        OfficialStatus.unknown, locale, Boolean.TRUE);
                }
            }
        }

        for (Entry<String, Set<String>> languageAndChildren : languageToChildren
            .keyValuesSet()) {
            String language = languageAndChildren.getKey();
            Set<String> children = languageAndChildren.getValue();
            M3<OfficialStatus, String, Boolean> officalStatusToChildren = languageToOfficialChildren
                .get(language);
            for (Entry<OfficialStatus, Map<String, Boolean>> entry : officalStatusToChildren) {
                OfficialStatus status = entry.getKey();
                if (status != OfficialStatus.official
                    && status != OfficialStatus.de_facto_official) {
                    continue;
                }
                Set<String> officalChildren = entry.getValue().keySet();
                if (!children.containsAll(officalChildren)) {
                    Set<String> missing = new TreeSet<String>(officalChildren);
                    missing.removeAll(children);
                    String message = "Missing CLDR locales for " + status
                        + " languages: " + missing;
                    errln(message);
                } else {
                    logln("CLDR locales " + children + " cover " + status
                        + " locales " + officalChildren);
                }

            }
        }

        if (DEBUG) {
            Set<String> languages = new TreeSet<String>(
                languageToChildren.keySet());
            languages.addAll(languageToOfficialChildren.keySet());
            System.out.print("\ncode\tlanguage");
            for (OfficialStatus status : OfficialStatus.values()) {
                System.out.print("\tNo\t" + status);
            }
            System.out.println();
            for (String language : languages) {
                if (!testOrg.contains(language)) {
                    continue;
                }
                System.out.print(language + "\t"
                    + testInfo.getEnglish().getName(language));

                M3<OfficialStatus, String, Boolean> officialChildren = languageToOfficialChildren
                    .get(language);
                for (OfficialStatus status : OfficialStatus.values()) {
                    Map<String, Boolean> children = officialChildren
                        .get(status);
                    if (children == null) {
                        System.out.print("\t" + 0 + "\t");
                    } else {
                        System.out.print("\t" + children.size() + "\t"
                            + show(children.keySet(), false));
                    }
                }
                System.out.println();
            }
        }
    }

    private String show(Set<String> joint, boolean showStatus) {
        StringBuffer b = new StringBuffer();
        for (String s : joint) {
            if (b.length() != 0) {
                b.append(", ");
            }
            LanguageTagParser ltp = new LanguageTagParser().set(s);
            String script = ltp.getScript();
            if (script.length() != 0) {
                b.append(testInfo.getEnglish().getName(CLDRFile.SCRIPT_NAME,
                    script));
            }
            String region = ltp.getRegion();
            if (region.length() != 0) {
                if (script.length() != 0) {
                    b.append("-");
                }
                b.append(testInfo.getEnglish().getName(CLDRFile.TERRITORY_NAME,
                    region));
            }
            b.append(" [").append(s);
            if (showStatus) {
                PopulationData data = dataInfo
                    .getLanguageAndTerritoryPopulationData(
                        ltp.getLanguage(), region);
                if (data == null) {
                    data = dataInfo.getLanguageAndTerritoryPopulationData(
                        ltp.getLanguageScript(), region);
                }
                b.append("; ");
                b.append(data == null ? "?" : data.getOfficialStatus());
            }
            b.append("]");

        }
        return b.toString();
    }

    private String removeScript(String lang) {
        if (!lang.contains("_")) {
            return lang;
        }
        LanguageTagParser ltp = new LanguageTagParser().set(lang);
        // String ls = ltp.getLanguageScript();
        // if (defaultContents.contains(ls)) {
        ltp.setScript("");
        // }
        return ltp.toString();
    }

    public void TestLikelyAndDefaultConsistency() {
        LikelySubtags likelySubtags = new LikelySubtags();
        LanguageTagParser ltp = new LanguageTagParser();
        // find multiscript locales
        Relation<String, String> base2scripts = Relation.of(
            new TreeMap<String, Set<String>>(), TreeSet.class);
        Map<String, String> parent2default = new TreeMap<String, String>();
        Map<String, String> default2parent = new TreeMap<String, String>();
        Relation<String, String> base2locales = Relation.of(
            new TreeMap<String, Set<String>>(), TreeSet.class);

        Set<String> knownMultiScriptLanguages = new HashSet<String>(Arrays.asList("bm", "ha"));
        // get multiscript locales
        for (String localeID : availableLocales) {
            String script = ltp.set(localeID).getScript();
            final String base = ltp.getLanguage();
            if (!availableLocales.contains(base)) {
                errln("Missing base locale for: " + localeID);
            }
            base2locales.put(base, localeID);
            if (!script.isEmpty() && !base.equals("en")) { // HACK for en
                base2scripts.put(base, script);
            }
            if (script.isEmpty() && knownMultiScriptLanguages.contains(base)) {
                base2scripts.put(base, dataInfo.getDefaultScript(base));
            }
        }

        // get default contents
        for (String localeID : defaultContents) {
            checkLocale(localeID, false);
            String simpleParent = LocaleIDParser.getSimpleParent(localeID);
            parent2default.put(simpleParent, localeID);
            default2parent.put(localeID, simpleParent);
            // if (!available.contains(simpleParent)) {
            // // verify that base language has locale in CLDR (we don't want
            // others)
            // errln("Default contents contains locale not in CLDR:\t" +
            // simpleParent);
            // }
        }

        // get likely
        Map<String, String> likely2Maximized = likelySubtags.getToMaximized();
        for (Entry<String, String> likelyAndMaximized : likely2Maximized
            .entrySet()) {
            checkLocale(likelyAndMaximized.getKey(), true);
            checkLocale(likelyAndMaximized.getValue(), true);
        }
        Map<String, String> exceptionDcLikely = new HashMap<String, String>();
        Map<String, String> exceptionLikelyDc = new HashMap<String, String>();
        for (String[] s : new String[][] { { "ar_001", "ar_Arab_EG" }, }) {
            exceptionDcLikely.put(s[0], s[1]);
            exceptionLikelyDc.put(s[1], s[0]);
        }

        verifyDefaultContentsImplicationsForLikelySubtags(ltp, parent2default,
            likely2Maximized, exceptionDcLikely);

        verifyLikelySubtagsImplicationsForDefaultContents(ltp, base2scripts,
            parent2default, likely2Maximized, exceptionLikelyDc);

        verifyScriptsWithDefaultContents(ltp, base2scripts, parent2default,
            base2locales);
    }

    public void TestParentLocaleRelationships() {
        // Testing invariant relationships between locales - See
        // http://unicode.org/cldr/trac/ticket/5758
        Matcher langScript = PatternCache.get("^[a-z]{2,3}_[A-Z][a-z]{3}$")
            .matcher("");
        for (String loc : availableLocales) {
            if (langScript.reset(loc).matches()) {
                String expectedParent = loc.split("_")[0];
                if (!defaultContents.contains(loc)) {
                    expectedParent = "root";
                }
                String actualParent = dataInfo.getExplicitParentLocale(loc);
                if (actualParent == null) {
                    actualParent = loc.split("_")[0];
                }
                if (!actualParent.equals(expectedParent)) {
                    errln("Unexpected parent locale for locale " + loc
                        + ". Expected: " + expectedParent + " Got: "
                        + actualParent);
                }

                if (dataInfo.getExplicitParentLocale(loc) != null
                    && defaultContents.contains(loc)) {
                    errln("Locale "
                        + loc
                        + " can't have an explicit parent AND be a default content locale");
                }
            }
        }
    }

    public void TestParentLocaleInvariants() {
        // Testing invariant relationships in parent locales - See
        // http://unicode.org/cldr/trac/ticket/7887
        LocaleIDParser lp = new LocaleIDParser();
        for (String loc : availableLocales) {
            String parentLocale = dataInfo.getExplicitParentLocale(loc);
            if (parentLocale != null) {
                if (!"root".equals(parentLocale)
                    && !lp.set(loc).getLanguage()
                    .equals(lp.set(parentLocale).getLanguage())) {
                    errln("Parent locale [" + parentLocale + "] for locale ["
                        + loc + "] cannot be a different language code.");
                }
                if (!"root".equals(parentLocale)
                    && !lp.set(loc).getScript()
                    .equals(lp.set(parentLocale).getScript())) {
                    errln("Parent locale [" + parentLocale + "] for locale ["
                        + loc + "] cannot be a different script code.");
                }
                lp.set(loc);
                if (lp.getScript().length() == 0 && lp.getRegion().length() == 0) {
                    errln("Base language locale [" + loc + "] cannot have an explicit parent.");
                }

            }
        }
    }

    public void TestParentLocalesForCycles() {
        // Testing for cyclic relationships in parent locales - See
        // http://unicode.org/cldr/trac/ticket/7887
        for (String loc : availableLocales) {
            String currentLoc = loc;
            boolean foundError = false;
            List<String> inheritanceChain = new ArrayList<String>(Arrays.asList(loc));
            while (currentLoc != null && !foundError) {
                currentLoc = LocaleIDParser.getParent(currentLoc);
                if (inheritanceChain.contains(currentLoc)) {
                    foundError = true;
                    inheritanceChain.add(currentLoc);
                    errln("Inheritance chain for locale [" + loc + "] contains a cyclic relationship. " + inheritanceChain.toString());
                }
                inheritanceChain.add(currentLoc);
            }
        }
    }

    private void verifyScriptsWithDefaultContents(LanguageTagParser ltp,
        Relation<String, String> base2scripts,
        Map<String, String> parent2default,
        Relation<String, String> base2locales) {
        Set<String> skip = Builder.with(new HashSet<String>())
            .addAll("root", "und")
            .freeze();
        Set<String> languagesWithOneOrLessLocaleScriptInCommon = new HashSet<String>(Arrays.asList("bm", "ha", "ms", "iu", "mn"));
        // for each base we have to have,
        // if multiscript, we have default contents for base+script,
        // base+script+region;
        // otherwise base+region.
        for (String base : base2locales.keySet()) {
            if (skip.contains(base)) {
                continue;
            }
            String defaultContent = parent2default.get(base);
            // Set<String> likely = base2likely.get(base);
            // if (likely == null) {
            // errln("Missing likely subtags for: " + base + "  " +
            // suggestLikelySubtagFor(base));
            // }
            if (defaultContent == null) {
                errln("Missing default content for: " + base + "  "
                    + suggestLikelySubtagFor(base));
                continue;
            }
            Set<String> scripts = base2scripts.get(base);
            ltp.set(defaultContent);
            String script = ltp.getScript();
            String region = ltp.getRegion();
            if (scripts == null || languagesWithOneOrLessLocaleScriptInCommon.contains(base)) {
                if (!script.isEmpty()) {
                    errln("Script should be empty in default content for: "
                        + base + "," + defaultContent);
                }
                if (region.isEmpty()) {
                    errln("Region must not be empty in default content for: "
                        + base + "," + defaultContent);
                }
            } else {
                if (script.isEmpty()) {
                    errln("Script should not be empty in default content for: "
                        + base + "," + defaultContent);
                }
                if (!region.isEmpty()) {
                    errln("Region should be empty in default content for: "
                        + base + "," + defaultContent);
                }
                String defaultContent2 = parent2default.get(defaultContent);
                if (defaultContent2 == null) {
                    errln("Missing default content for: " + defaultContent);
                    continue;
                }
                ltp.set(defaultContent2);
                region = ltp.getRegion();
                if (region.isEmpty()) {
                    errln("Region must not be empty in default content for: "
                        + base + "," + defaultContent);
                }
            }
        }
    }

    private void verifyLikelySubtagsImplicationsForDefaultContents(
        LanguageTagParser ltp, Relation<String, String> base2scripts,
        Map<String, String> parent2default,
        Map<String, String> likely2Maximized,
        Map<String, String> exceptionLikelyDc) {
        // Now check invariants for all LikelySubtags implications for Default
        // Contents
        // a) suppose likely max for la_Scrp => la_Scrp_RG
        // Then default contents la_Scrp => la_Scrp_RG
        // b) suppose likely max for la_RG => la_Scrp_RG
        // Then we can draw no conclusions // was default contents la_Scrp =>
        // la_Scrp_RG
        // c) suppose likely max for la => la_Scrp_RG
        // Then default contents la => la_Scrp && la_Scrp => la_Scrp_RG
        // or default contents la => la_RG && ! la_Scrp => la_Scrp_RG

        TreeSet<String> additionalDefaultContents = new TreeSet<String>();

        for (Entry<String, String> entry : likely2Maximized.entrySet()) {
            String source = entry.getKey();
            String likelyMax = entry.getValue();
            String sourceLang = ltp.set(source).getLanguage();
            if (sourceLang.equals("und") || source.equals("zh_Hani")
                || source.equals("tl")) {
                continue;
            }
            String sourceScript = ltp.getScript();
            String sourceRegion = ltp.getRegion();

            String likelyMaxLang = ltp.set(likelyMax).getLanguage();
            String likelyMaxScript = ltp.getScript();
            String likelyMaxRegion = ltp.getRegion();

            String dc = parent2default.get(source);
            String possibleException = exceptionLikelyDc.get(likelyMax);
            if (possibleException != null && possibleException.equals(dc)) {
                continue;
            }
            String likelyLangScript = likelyMaxLang + "_" + likelyMaxScript;
            String dcFromLangScript = parent2default.get(likelyLangScript);

            boolean consistent = true;
            String caseNumber = null;
            if (consistent) {
                if (!sourceScript.isEmpty()) {
                    caseNumber = "a";
                    if (dc == null) {
                        if (EXPECT_EQUALITY) {
                            String expected = likelyMax;
                            errln("Default contents null for " + source
                                + ", expected:\t" + expected);
                            additionalDefaultContents.add(expected);
                        }
                        continue;
                    }
                    consistent = likelyMax.equals(dc);
                } else if (!sourceRegion.isEmpty()) { // a
                    caseNumber = "b";
                    // consistent = likelyMax.equals(dcFromLangScript);
                } else { // c
                    caseNumber = "c";
                    if (dc == null) {
                        if (EXPECT_EQUALITY) {
                            String expected = base2scripts.get(source) == null ? likelyMaxLang
                                + "_" + likelyMaxRegion
                                : likelyMaxLang + "_" + likelyMaxScript;
                            errln("Default contents null for " + source
                                + ", expected:\t" + expected);
                            additionalDefaultContents.add(expected);
                        }
                        continue;
                    }
                    String dcScript = ltp.set(dc).getScript();
                    consistent = likelyLangScript.equals(dc)
                        && likelyMax.equals(dcFromLangScript)
                        || dcScript.isEmpty()
                        && !likelyMax.equals(dcFromLangScript);
                    // || dcScript.isEmpty() && dcRegion.equals(likelyMaxRegion)
                    // && dcFromLangScript == null;
                }
            }
            if (!consistent) {
                errln("default contents inconsistent with likely subtag: ("
                    + caseNumber + ")" + "\n\t" + source + " => (ls) "
                    + likelyMax + "\n\t" + source + " => (dc) " + dc
                    + "\n\t" + likelyLangScript + " => (dc) "
                    + dcFromLangScript);
            }
        }
        if (additionalDefaultContents.size() != 0) {
            errln("Suggested additions to supplementalMetadata/../defaultContent:\n"
                + CollectionUtilities.join(additionalDefaultContents, " "));
        }
    }

    private void verifyDefaultContentsImplicationsForLikelySubtags(
        LanguageTagParser ltp, Map<String, String> parent2default,
        Map<String, String> likely2Maximized,
        Map<String, String> exceptionDcLikely) {
        // Now check invariants for all Default Contents implications for
        // LikelySubtags
        // a) suppose default contents la => la_Scrp.
        // Then the likely contents for la => la_Scrp_*
        // b) suppose default contents la => la_RG.
        // Then the likely contents for la => la_*_RG
        // c) suppose default contents la_Scrp => la_Scrp_RG.
        // Then the likely contents of la_Scrp => la_Scrp_RG OR likely contents
        // for la => la_*_*
        for (Entry<String, String> parentAndDefault : parent2default.entrySet()) {
            String source = parentAndDefault.getKey();
            String dc = parentAndDefault.getValue();
            String likelyMax = likely2Maximized.get(source);

            // skip special exceptions
            String possibleException = exceptionDcLikely.get(dc);
            if (possibleException != null
                && possibleException.equals(likelyMax)) {
                continue;
            }

            String sourceLang = ltp.set(source).getLanguage();
            String sourceScript = ltp.getScript();
            // there cannot be a sourceRegion

            String dcScript = ltp.set(dc).getScript();
            String dcRegion = ltp.getRegion();

            String likelyMaxLang = "", likelyMaxScript = "", likelyMaxRegion = "";
            if (likelyMax != null) {
                likelyMaxLang = ltp.set(likelyMax).getLanguage();
                likelyMaxScript = ltp.getScript();
                likelyMaxRegion = ltp.getRegion();
            }

            String likelyMax2 = likely2Maximized.get(sourceLang);

            boolean consistent = true;

            if (sourceScript.isEmpty()) { // a or b
                if (!dcScript.isEmpty()) { // a
                    consistent = likelyMaxLang.equals(source)
                        && likelyMaxScript.equals(dcScript);
                } else { // b
                    consistent = likelyMaxLang.equals(source)
                        && likelyMaxRegion.equals(dcRegion);
                }
            } else { // c
                consistent = dc.equals(likelyMax) || likelyMax2 != null;
            }
            if (!consistent) {
                errln("likely subtag inconsistent with default contents: "
                    + "\n\t"
                    + source
                    + " =>( dc) "
                    + dc
                    + "\n\t"
                    + source
                    + " => (ls) "
                    + likelyMax
                    + (source.equals(sourceLang) ? "" : "\n\t" + sourceLang
                        + " => (ls) " + likelyMax2));
            }
        }
    }

    /**
     * Suggest a likely subtag
     *
     * @param base
     * @return
     */
    static String suggestLikelySubtagFor(String base) {
        SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();

        CLDRLocale loc = CLDRLocale.getInstance(base);

        if (!loc.getLanguage().equals(base)) {
            return " (no suggestion- not a simple language locale)"; // no
            // suggestion
            // unless
            // just
            // a
            // language
            // locale.
        }
        Set<BasicLanguageData> basicData = sdi.getBasicLanguageData(base);

        for (BasicLanguageData bld : basicData) {
            if (bld.getType() == org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData.Type.primary) {
                Set<String> scripts = bld.getScripts();
                Set<String> territories = bld.getTerritories();

                if (scripts.size() == 1) {
                    if (territories.size() == 1) {
                        return createSuggestion(
                            loc,
                            CLDRLocale.getInstance(base + "_"
                                + scripts.iterator().next() + "_"
                                + territories.iterator().next()));
                    }
                }
                return "(no suggestion - multiple scripts or territories)";
            }
        }
        return ("(no suggestion- no data)");
    }

    /**
     * Format and return a suggested likelysubtag
     */
    private static String createSuggestion(CLDRLocale loc, CLDRLocale toLoc) {
        return " Suggest this to likelySubtags.xml:        <likelySubtag from=\""
            + loc
            + "\" to=\""
            + toLoc
            + "\"/>\n"
            + "        <!--{ "
            + loc.getDisplayName()
            + "; ?; ? } => { "
            + loc.getDisplayName()
            + "; "
            + toLoc.toULocale().getDisplayScript()
            + "; "
            + toLoc.toULocale().getDisplayCountry() + " }-->";

    }

    public void TestDeprecatedTerritoryDataLocaleIds() {
        HashSet<String> checked = new HashSet<String>();
        for (String language : dataInfo
            .getLanguagesForTerritoriesPopulationData()) {
            checkLocale(language, false); // checks la_Scrp and la
            for (String region : dataInfo
                .getTerritoriesForPopulationData(language)) {
                if (!checked.contains(region)) {
                    checkValidCode(language + "_" + region, "territory",
                        region, false);
                    checked.add(region);
                }
            }
        }
        for (String language : dataInfo.getBasicLanguageDataLanguages()) {
            checkLocale(language, false); // checks la_Scrp and la
            Set<BasicLanguageData> data = dataInfo
                .getBasicLanguageData(language);
            for (BasicLanguageData datum : data) {
                for (String script : datum.getScripts()) {
                    checkValidCode(language + "_" + script, "script", script,
                        false);
                    checked.add(script);
                }
                for (String region : datum.getTerritories()) {
                    checkValidCode(language + "_" + region, "territory",
                        region, false);
                    checked.add(region);
                }
            }
        }

    }

    public void TestBasicLanguageDataAgainstScriptMetadata() {
        // the invariants are:
        // if there is primary data, the script must be there
        // otherwise it must be in the secondary
        main: for (String script : ScriptMetadata.getScripts()) {
            Info info = ScriptMetadata.getInfo(script);
            String language = info.likelyLanguage;
            if (language.equals("und")) {
                continue;
            }
            Map<Type, BasicLanguageData> data = dataInfo
                .getBasicLanguageDataMap(language);
            if (data == null) {
                logln("Warning: ScriptMetadata has " + language + " for "
                    + script + "," + " but " + language
                    + " is missing in language_script.txt");
                continue;
            }
            for (BasicLanguageData entry : data.values()) {
                if (entry.getScripts().contains(script)) {
                    continue main;
                }
                continue;
            }
            logln("Warning: ScriptMetadata has " + language + " for " + script
                + "," + " but " + language + " doesn't have " + script
                + " in language_script.txt");
        }
    }

    public void TestCldrFileConsistency() {
        boolean haveErrors = false;
        for (String locale : testInfo.getCldrFactory().getAvailable()) {
            CLDRFile cldrFileToCheck = testInfo.getCLDRFile(locale,
                false);
            int errors = 0;
            for (String path : cldrFileToCheck) {
                if (!pathMatcher.reset(path).find()) {
                    continue;
                }
                String fullPath = cldrFileToCheck.getFullXPath(path);
                if (fullPath == null) {
                    // try again, for debugging
                    fullPath = cldrFileToCheck.getFullXPath(path);
                    String value = cldrFileToCheck.getStringValue(path);
                    if (DEBUG) {
                        errln("Invalid full path\t" + locale + ", " + path
                            + ", " + fullPath + ", " + value);
                    }
                    errors++;
                    haveErrors = true;
                }
            }
            if (errors != 0) {
                errln(locale
                    + (errors != 0 ? "\tinvalid getFullXPath() values:"
                        + errors : ""));
            } else {
                logln(locale);
            }
        }
        if (haveErrors && !DEBUG) {
            errln("Use -DDEBUG to see details");
        }
    }

    static SupplementalDataInfo info = SupplementalDataInfo.getInstance();
    LanguageTagParser ltp = new LanguageTagParser();

    Matcher aliasMatcher = PatternCache.get("//ldml.*/alias.*").matcher("");

    private String minimize(Map<String, String> likelySubtags, String locale) {
        String result = GenerateMaximalLocales.minimize(locale, likelySubtags,
            false);
        if (result == null) {
            LanguageTagParser ltp3 = new LanguageTagParser().set(locale);
            List<String> variants = ltp3.getVariants();
            Map<String, String> extensions = ltp3.getExtensions();
            Set<String> emptySet = Collections.emptySet();
            ltp3.setVariants(emptySet);
            Map<String, String> emptyMap = Collections.emptyMap();
            ltp3.setExtensions(emptyMap);
            String newLocale = ltp3.toString();
            result = GenerateMaximalLocales.minimize(newLocale, likelySubtags,
                false);
            if (result != null) {
                ltp3.set(result);
                ltp3.setVariants(variants);
                ltp3.setExtensions(extensions);
                result = ltp3.toString();
            }
        }
        return result;
    }

    private String maximize(Map<String, String> likelySubtags, String locale) {
        String result = GenerateMaximalLocales.maximize(locale, likelySubtags);
        if (result == null) {
            LanguageTagParser ltp3 = new LanguageTagParser().set(locale);
            List<String> variants = ltp3.getVariants();
            Map<String, String> extensions = ltp3.getExtensions();
            Set<String> emptySet = Collections.emptySet();
            ltp3.setVariants(emptySet);
            Map<String, String> emptyMap = Collections.emptyMap();
            ltp3.setExtensions(emptyMap);
            String newLocale = ltp3.toString();
            result = GenerateMaximalLocales.maximize(newLocale, likelySubtags);
            if (result != null) {
                ltp3.set(result);
                ltp3.setVariants(variants);
                ltp3.setExtensions(extensions);
                result = ltp3.toString();
            }
        }
        return result;
    }

    // TODO move this into central utilities
    public static boolean equals(CharSequence string, int codePoint) {
        if (string == null) {
            return false;
        }
        switch (string.length()) {
        case 1:
            return codePoint == string.charAt(0);
        case 2:
            return codePoint >= 0x10000
            && codePoint == Character.codePointAt(string, 0);
        default:
            return false;
        }
    }

    // TODO move this into central utilities

    private static final StandardCodes STANDARD_CODES = testInfo.getStandardCodes();
    private static final Map<String, Map<String, R2<List<String>, String>>> DEPRECATED_INFO = dataInfo
        .getLocaleAliasInfo();

    private void checkLocale(String localeID, boolean allowDeprecated) {
        // verify that the localeID is valid
        LanguageTagParser ltp = new LanguageTagParser().set(localeID);
        String language = ltp.getLanguage();
        String script = ltp.getScript();
        String region = ltp.getRegion();
        // TODO check variants, extensions also.
        checkValidCode(localeID, "language", language, allowDeprecated);
        checkValidCode(localeID, "script", script, allowDeprecated);
        checkValidCode(localeID, "territory", region, allowDeprecated);
    }

    private void checkValidCode(String localeID, String subtagType,
        String subtag, boolean allowDeprecated) {
        if (subtagType.equals("language")) {
            if (subtag.equals("und")) {
                return;
            }
        } else {
            if (subtag.isEmpty()) {
                return;
            }
        }
        if (!STANDARD_CODES.getAvailableCodes(subtagType).contains(subtag)) {
            errln("Locale " + localeID + " contains illegal " + showCode(subtagType, subtag));
        } else if (!allowDeprecated) {
            // "language" -> "sh" -> <{"sr_Latn"}, reason>
            R2<List<String>, String> deprecatedInfo = DEPRECATED_INFO.get(
                subtagType).get(subtag);
            if (deprecatedInfo != null) {
                errln("Locale " + localeID + " contains deprecated "
                    + showCode(subtagType, subtag) + " "
                    + deprecatedInfo.get1() + "; suggest "
                    + showName(deprecatedInfo.get0(), subtagType));
            }
        }
    }

    private String showName(List<String> deprecatedInfo, String subtagType) {
        StringBuilder result = new StringBuilder();
        for (String s : deprecatedInfo) {
            result.append(showName(subtagType, s)).append(" ");
        }
        return result.toString();
    }

    private String showCode(String subtagType, String subtag) {
        return subtagType + " code: " + showName(subtagType, subtag);
    }

    private String showName(String subtagType, String subtag) {
        return subtag + " (" + getName(subtagType, subtag) + ")";
    }

    private String getName(String subtagType, String subtag) {
        Map<String, String> data = STANDARD_CODES.getLangData(subtagType,
            subtag);
        if (data == null) {
            return "<no name>";
        }
        return data.get("Description");
    }

    // TODO move this into central utilities
    public static boolean equals(int codePoint, CharSequence string) {
        return equals(string, codePoint);
    }

    // TODO move this into central utilities
    public static boolean equals(Object a, Object b) {
        return a == b ? true : a == null || b == null ? false : a.equals(b);
    }

    // TODO move this into central utilities
    private <K, V> String showDifferences(Map<K, V> a, Map<K, V> b) {
        StringBuilder result = new StringBuilder();
        Set<K> keys = new LinkedHashSet<K>();
        keys.addAll(a.keySet());
        keys.addAll(b.keySet());
        for (K key : keys) {
            if (!a.containsKey(key)) {
                result.append(key).append("→‹").append(a.get(key))
                .append("›,∅; ");
            } else if (!b.containsKey(key)) {
                result.append(key).append("→∅,‹").append(b.get(key))
                .append("›; ");
            } else {
                V aKey = a.get(key);
                V bKey = b.get(key);
                if (!equals(aKey, bKey)) {
                    result.append(key).append("→‹").append(a.get(key))
                    .append("›,‹").append(b.get(key)).append("›; ");
                }
            }
        }
        return result.toString();
    }

    public void TestLanguageTagParser() {
        LanguageTagParser ltp = new LanguageTagParser();
        ltp.set("en-Cyrl-US");
        assertEquals(null, "en", ltp.getLanguage());
        assertEquals(null, "en_Cyrl", ltp.getLanguageScript());
        assertEquals(null, "Cyrl", ltp.getScript());
        assertEquals(null, "US", ltp.getRegion());
        try {
            ltp.set("$");
            assertFalse("expected exception", true);
        } catch (Exception e) {
            logln(e.getMessage());
        }
    }
}
