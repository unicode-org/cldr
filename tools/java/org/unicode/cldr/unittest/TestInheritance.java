package org.unicode.cldr.unittest;

import java.io.IOException;
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
import java.util.regex.Pattern;

import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.cldr.tool.GenerateMaximalLocales;
import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagCanonicalizer;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData.Type;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.impl.Row.R2;

public class TestInheritance extends TestFmwk {

    private static boolean DEBUG = CldrUtility.getProperty("DEBUG", true);

    private static String fileMatcher = CldrUtility.getProperty("FILE", ".*");

    private static Matcher pathMatcher = Pattern.compile(CldrUtility.getProperty("XPATH", ".*")).matcher("");

    public static void main(String[] args) throws IOException {
        new TestInheritance().run(args);
    }

    private static final SupplementalDataInfo dataInfo = SupplementalDataInfo.getInstance();

    private static final boolean EXPECT_EQUALITY = false;

    public void TestLikelyAndDefaultConsistency() {
        Set<String> defaultContents = dataInfo.getDefaultContentLocales();
        LikelySubtags likelySubtags = new LikelySubtags();
        Factory factory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
        Factory factory2 = Factory.make(CldrUtility.BASE_DIRECTORY + "seed/", ".*");
        Set<String> available = Builder.with(new TreeSet<String>()).addAll(factory.getAvailable())
            .addAll(factory2.getAvailable()).freeze();
        LanguageTagParser ltp = new LanguageTagParser();
        // find multiscript locales
        Relation<String, String> base2scripts = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);
        Relation<String, String> base2likely = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);
        Map<String, String> parent2default = new TreeMap<String, String>();
        Map<String, String> default2parent = new TreeMap<String, String>();
        Relation<String, String> base2locales = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);

        // get multiscript locales
        for (String localeID : available) {
            String script = ltp.set(localeID).getScript();
            final String base = ltp.getLanguage();
            if (!available.contains(base)) {
                errln("Missing base locale for: " + localeID);
            }
            base2locales.put(base, localeID);
            if (!script.isEmpty() && !base.equals("en")) { // HACK for en
                base2scripts.put(base, script);
            }
        }

        // get default contents
        for (String localeID : defaultContents) {
            checkLocale(localeID, false);
            String simpleParent = LocaleIDParser.getSimpleParent(localeID);
            parent2default.put(simpleParent, localeID);
            default2parent.put(localeID, simpleParent);
            // if (!available.contains(simpleParent)) {
            // // verify that base language has locale in CLDR (we don't want others)
            // errln("Default contents contains locale not in CLDR:\t" + simpleParent);
            // }
        }

        // get likely
        Map<String, String> likely2Maximized = likelySubtags.getToMaximized();
        for (Entry<String, String> likelyAndMaximized : likely2Maximized.entrySet()) {
            checkLocale(likelyAndMaximized.getKey(), true);
            checkLocale(likelyAndMaximized.getValue(), true);
        }
        Map<String, String> exceptionDcLikely = new HashMap<String, String>();
        Map<String, String> exceptionLikelyDc = new HashMap<String, String>();
        for (String[] s : new String[][] {
            { "ar_001", "ar_Arab_EG" },
        }) {
            exceptionDcLikely.put(s[0], s[1]);
            exceptionLikelyDc.put(s[1], s[0]);
        }

        verifyDefaultContentsImplicationsForLikelySubtags(ltp, parent2default, likely2Maximized, exceptionDcLikely);

        verifyLikelySubtagsImplicationsForDefaultContents(ltp, base2scripts, parent2default, likely2Maximized,
            exceptionLikelyDc);

        verifyScriptsWithDefaultContents(ltp, base2scripts, parent2default, base2locales);
    }

    private void verifyScriptsWithDefaultContents(LanguageTagParser ltp, Relation<String, String> base2scripts,
        Map<String, String> parent2default, Relation<String, String> base2locales) {
        Set<String> skip = Builder.with(new HashSet<String>()).addAll("in", "iw", "mo", "no", "root", "sh", "tl")
            .freeze();

        // for each base we have to have,
        // if multiscript, we have default contents for base+script, base+script+region;
        // otherwise base+region.
        for (String base : base2locales.keySet()) {
            if (skip.contains(base)) {
                continue;
            }
            String defaultContent = parent2default.get(base);
            // Set<String> likely = base2likely.get(base);
            // if (likely == null) {
            // errln("Missing likely subtags for: " + base + "  " + suggestLikelySubtagFor(base));
            // }
            if (defaultContent == null) {
                errln("Missing default content for: " + base + "  " + suggestLikelySubtagFor(base));
                continue;
            }
            Set<String> scripts = base2scripts.get(base);
            ltp.set(defaultContent);
            String script = ltp.getScript();
            String region = ltp.getRegion();
            if (scripts == null) {
                if (!script.isEmpty()) {
                    errln("Script should be empty in default content for: " + base + "," + defaultContent);
                }
                if (region.isEmpty()) {
                    errln("Region must be empty in default content for: " + base + "," + defaultContent);
                }
            } else {
                if (script.isEmpty()) {
                    errln("Script should be empty in default content for: " + base + "," + defaultContent);
                }
                if (!region.isEmpty()) {
                    errln("Region should not be empty in default content for: " + base + "," + defaultContent);
                }
                String defaultContent2 = parent2default.get(defaultContent);
                if (defaultContent2 == null) {
                    errln("Missing default content for: " + defaultContent);
                    continue;
                }
                ltp.set(defaultContent2);
                region = ltp.getRegion();
                if (region.isEmpty()) {
                    errln("Region must be empty in default content for: " + base + "," + defaultContent);
                }
            }
        }
    }

    private void verifyLikelySubtagsImplicationsForDefaultContents(LanguageTagParser ltp,
        Relation<String, String> base2scripts, Map<String, String> parent2default,
        Map<String, String> likely2Maximized, Map<String, String> exceptionLikelyDc) {
        // Now check invariants for all LikelySubtags implications for Default Contents
        // a) suppose likely max for la_Scrp => la_Scrp_RG
        // Then default contents la_Scrp => la_Scrp_RG
        // b) suppose likely max for la_RG => la_Scrp_RG
        // Then we can draw no conclusions // was default contents la_Scrp => la_Scrp_RG
        // c) suppose likely max for la => la_Scrp_RG
        // Then default contents la => la_Scrp && la_Scrp => la_Scrp_RG
        // or default contents la => la_RG && ! la_Scrp => la_Scrp_RG

        TreeSet<String> additionalDefaultContents = new TreeSet<String>();

        for (Entry<String, String> entry : likely2Maximized.entrySet()) {
            String source = entry.getKey();
            String likelyMax = entry.getValue();
            String sourceLang = ltp.set(source).getLanguage();
            if (sourceLang.equals("und") || source.equals("zh_Hani") || source.equals("tl")) {
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
                            errln("Default contents null for " + source + ", expected:\t" + expected);
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
                            String expected = base2scripts.get(source) == null
                                ? likelyMaxLang + "_" + likelyMaxRegion
                                : likelyMaxLang + "_" + likelyMaxScript;
                            errln("Default contents null for " + source + ", expected:\t" + expected);
                            additionalDefaultContents.add(expected);
                        }
                        continue;
                    }
                    String dcLang = ltp.set(dc).getLanguage();
                    String dcScript = ltp.getScript();
                    String dcRegion = ltp.getRegion();
                    consistent = likelyLangScript.equals(dc) && likelyMax.equals(dcFromLangScript)
                        || dcScript.isEmpty() && !likelyMax.equals(dcFromLangScript);
                    // || dcScript.isEmpty() && dcRegion.equals(likelyMaxRegion) && dcFromLangScript == null;
                }
            }
            if (!consistent) {
                errln("default contents inconsistent with likely subtag: (" + caseNumber + ")"
                    + "\n\t" + source + " => (ls) " + likelyMax
                    + "\n\t" + source + " => (dc) " + dc
                    + "\n\t" + likelyLangScript + " => (dc) " + dcFromLangScript);
            }
        }
        if (additionalDefaultContents.size() != 0) {
            errln("Suggested additions to supplementalMetadata/../defaultContent:\n" +
                CollectionUtilities.join(additionalDefaultContents, " "));
        }
    }

    private void verifyDefaultContentsImplicationsForLikelySubtags(LanguageTagParser ltp,
        Map<String, String> parent2default, Map<String, String> likely2Maximized, Map<String, String> exceptionDcLikely) {
        // Now check invariants for all Default Contents implications for LikelySubtags
        // a) suppose default contents la => la_Scrp.
        // Then the likely contents for la => la_Scrp_*
        // b) suppose default contents la => la_RG.
        // Then the likely contents for la => la_*_RG
        // c) suppose default contents la_Scrp => la_Scrp_RG.
        // Then the likely contents of la_Scrp => la_Scrp_RG OR likely contents for la => la_*_*
        for (Entry<String, String> parentAndDefault : parent2default.entrySet()) {
            String source = parentAndDefault.getKey();
            String dc = parentAndDefault.getValue();
            String likelyMax = likely2Maximized.get(source);

            // skip special exceptions
            String possibleException = exceptionDcLikely.get(dc);
            if (possibleException != null && possibleException.equals(likelyMax)) {
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
                    consistent = likelyMaxLang.equals(source) && likelyMaxScript.equals(dcScript);
                } else { // b
                    consistent = likelyMaxLang.equals(source) && likelyMaxRegion.equals(dcRegion);
                }
            } else { // c
                consistent = dc.equals(likelyMax) || likelyMax2 != null;
            }
            if (!consistent) {
                errln("likely subtag inconsistent with default contents: "
                    + "\n\t" + source + " =>( dc) " + dc
                    + "\n\t" + source + " => (ls) " + likelyMax
                    + (source.equals(sourceLang) ? "" : "\n\t" + sourceLang + " => (ls) " + likelyMax2));
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
            return " (no suggestion- not a simple language locale)"; // no suggestion unless just a language locale.
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
                            CLDRLocale.getInstance(base + "_" + scripts.iterator().next() + "_"
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
        return " Suggest this to likelySubtags.xml:        <likelySubtag from=\"" + loc + "\" to=\"" + toLoc + "\"/>\n"
            +
            "        <!--{ " + loc.getDisplayName() + "; ?; ? } => { " + loc.getDisplayName() + "; "
            + toLoc.toULocale().getDisplayScript() + "; " + toLoc.toULocale().getDisplayCountry() + " }-->";

    }

    public void TestLanguageTagCanonicalizer() {
        String[][] tests = {
            { "eng-840", "en_US" },
            { "sh_ba", "sr_Latn_BA" },
            { "iw-arab-010", "he_Arab_AQ" },
            { "en-POLYTONI-WHATEVER-ANYTHING-AALAND", "en_AX_ANYTHING_POLYTON_WHATEVER" },
        };
        LanguageTagCanonicalizer canon = new LanguageTagCanonicalizer();
        for (String[] inputExpected : tests) {
            assertEquals("Canonicalize", inputExpected[1], canon.transform(inputExpected[0]));
        }
    }

    public void TestDeprecatedTerritoryDataLocaleIds() {
        HashSet<String> checked = new HashSet<String>();
        for (String language : dataInfo.getLanguagesForTerritoriesPopulationData()) {
            checkLocale(language, false); // checks la_Scrp and la
            for (String region : dataInfo.getTerritoriesForPopulationData(language)) {
                if (!checked.contains(region)) {
                    checkValidCode(language + "_" + region, "territory", region, false);
                    checked.add(region);
                }
            }
        }
        for (String language : dataInfo.getBasicLanguageDataLanguages()) {
            checkLocale(language, false); // checks la_Scrp and la
            Set<BasicLanguageData> data = dataInfo.getBasicLanguageData(language);
            for (BasicLanguageData datum : data) {
                for (String script : datum.getScripts()) {
                    checkValidCode(language + "_" + script, "script", script, false);
                    checked.add(script);
                }
                for (String region : datum.getTerritories()) {
                    checkValidCode(language + "_" + region, "territory", region, false);
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
            Map<Type, BasicLanguageData> data = dataInfo.getBasicLanguageDataMap(language);
            if (data == null) {
                logln("Warning: ScriptMetadata has " + language + " for " + script + "," +
                    " but " + language + " is missing in language_script.txt");
                continue;
            }
            for (BasicLanguageData entry : data.values()) {
                if (entry.getScripts().contains(script)) {
                    continue main;
                }
                continue;
            }
            logln("Warning: ScriptMetadata has " + language + " for " + script + "," +
                " but " + language + " doesn't have " + script + " in language_script.txt");
        }
    }

    public void TestCldrFileConsistency() {
        Factory factory = Factory.make(CldrUtility.MAIN_DIRECTORY, fileMatcher);
        boolean haveErrors = false;
        for (String locale : factory.getAvailable()) {
            CLDRFile cldrFileToCheck = factory.make(locale, true);
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
                        errln("Invalid full path\t" + locale + ", " + path + ", " + fullPath + ", " + value);
                    }
                    errors++;
                    haveErrors = true;
                }
            }
            if (errors != 0) {
                errln(locale + (errors != 0 ? "\tinvalid getFullXPath() values:" + errors : ""));
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

    // public void TestAliases() {
    // Factory factory = Factory.make(CldrUtility.MAIN_DIRECTORY, fileMatcher);
    // Set<String> allLocales = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*").getAvailable();
    //
    // LanguageTagCanonicalizer languageTagCanonicalizer = new LanguageTagCanonicalizer();
    //
    // Set<String> defaultContents = info.getDefaultContentLocales();
    //
    // Map<String, String> likelySubtags = info.getLikelySubtags();
    //
    // XPathParts xpp = new XPathParts();
    //
    // // get the top level aliases, and verify that they are consistent with
    // // maximization
    // Map<String, String> topLevelAliases = new TreeMap<String, String>();
    // Set<String> crossScriptSet = new TreeSet<String>();
    // Set<String> aliasPaths = new TreeSet<String>();
    // Set<String> locales = factory.getAvailable();
    //
    // // get the languages that need scripts
    // // TODO broaden to beyond CLDR
    // Set<String> needScripts = new TreeSet<String>();
    // for (String locale : locales) {
    // String script = ltp.set(locale).getScript();
    // if (script.length() != 0) {
    // needScripts.add(ltp.getLanguage());
    // }
    // }
    //
    // logln("Languages that have scripts:\t" + needScripts);
    //
    // for (String locale : locales) {
    //
    // // get alias locale
    // String aliasLocale = locale;
    // String explicitAlias = null;
    // String aliasPathNew = null;
    // CLDRFile cldrFileToCheck = factory.make(locale, false);
    // aliasPaths.clear();
    // // examples:
    // // in: <alias source="id" path="//ldml"/>
    // // ar_IR: <alias source="az_Arab_IR" path="//ldml"/>
    //
    // cldrFileToCheck.getPaths("//ldml/alias", null, aliasPaths);
    // if (aliasPaths.size() != 0) {
    // String aliasPath = aliasPaths.iterator().next();
    // String fullPath = cldrFileToCheck.getFullXPath(aliasPath);
    // explicitAlias = aliasLocale = xpp.set(fullPath).getAttributeValue(1, "source");
    // String aliasParent = LocaleIDParser.getParent(aliasLocale);
    // if (!aliasParent.equals("root")) {
    // topLevelAliases.put(locale, aliasParent);
    // }
    // aliasPathNew = xpp.set(fullPath).getAttributeValue(1, "path");
    // if ("//ldml/".equals(aliasPathNew)) {
    // errln("Bad alias path:\t" + fullPath);
    // }
    // }
    //
    // checkAliasValues(cldrFileToCheck, allLocales);
    //
    // // get canonicalized
    // String canonicalizedLocale = languageTagCanonicalizer.transform(locale);
    // if (!locale.equals(canonicalizedLocale)) {
    // logln("Locale\t" + locale + " => " + canonicalizedLocale);
    // }
    //
    // String base = ltp.set(canonicalizedLocale).getLanguage();
    // String script = ltp.getScript();
    // if (canonicalizedLocale.equals(base)) { // eg, id, az
    // continue;
    // }
    //
    // // see if the locale's default script is the same as the base locale's
    //
    // String maximized = maximize(likelySubtags, canonicalizedLocale);
    // if (maximized == null) {
    // errln("Missing likely subtags for:\t" + locale + "  " + suggestLikelySubtagFor(locale));
    // continue;
    // }
    // String maximizedScript = ltp.set(maximized).getScript();
    //
    // String minimized = minimize(likelySubtags, canonicalizedLocale);
    //
    // String baseMaximized = maximize(likelySubtags, base);
    // String baseScript = ltp.set(baseMaximized).getScript();
    //
    // if (script.length() != 0 && !script.equals(baseScript)) {
    // crossScriptSet.add(ltp.set(locale).getLanguageScript());
    // }
    //
    // // Finally, put together the expected alias for comparison.
    // // It is the "best" alias, in that the default-content locales are skipped in favor of their parents
    //
    // String expectedAlias =
    // !baseScript.equals(maximizedScript) ? minimized :
    // !locale.equals(canonicalizedLocale) ? canonicalizedLocale :
    // // needScripts.contains(base) ? ltp.getLanguageScript() :
    // locale;
    //
    // if (!equals(aliasLocale, expectedAlias)) {
    // String aliasMaximized = maximize(likelySubtags, aliasLocale);
    // String expectedMaximized = maximize(likelySubtags, expectedAlias);
    // if (!equals(aliasMaximized, expectedMaximized)) {
    // errln("For locale:\t" + locale
    // + ",\tbase-script:\t" + baseScript
    // + ",\texpected alias Locale != actual alias Locale:\t"
    // + expectedAlias + ", " + aliasLocale);
    // } else if (explicitAlias == null) {
    // // skip, we don't care in this case
    // // but we emit warnings if the other conditions are true. The aliasing could be simpler.
    // } else if (equals(expectedAlias, locale)) {
    // logln("Warning; alias could be omitted. For locale:\t" + locale
    // + ",\tbase-script:\t" + baseScript
    // + ",\texpected alias Locale != actual alias Locale:\t"
    // + expectedAlias + ", " + aliasLocale);
    // } else {
    // logln("Warning; alias could be minimized. For locale:\t" + locale
    // + ",\tbase-script:\t" + baseScript
    // + ",\texpected alias Locale != actual alias Locale:\t"
    // + expectedAlias + ", " + aliasLocale);
    // }
    // }
    // }
    //
    // // check the LocaleIDParser.TOP_LEVEL_ALIAS_LOCALES value and make sure it matches what is in the files in main/
    //
    // if (!topLevelAliases.equals(LocaleIDParser.TOP_LEVEL_ALIAS_LOCALES)
    // && locales.equals(allLocales)) {
    // String diff = showDifferences(LocaleIDParser.TOP_LEVEL_ALIAS_LOCALES, topLevelAliases);
    // if (!diff.isEmpty()) {
    // errln("LocaleIDParser.TOP_LEVEL_ALIAS_LOCALES ≠ topLevelAliases: " + diff);
    // }
    // StringBuilder result = new StringBuilder(
    // "Suggest changing LocaleIDParser.TOP_LEVEL_ALIAS_LOCALES to:\n");
    // for (Entry<String, String> entry : topLevelAliases.entrySet()) {
    // result.append("\t.put(\"")
    // .append(entry.getKey())
    // .append("\", \"")
    // .append(entry.getValue())
    // .append("\")\n");
    // }
    // errln(result.toString());
    // } else {
    // logln("Top Level Aliases:\t" + topLevelAliases);
    // }
    //
    // // verify that they are the same as what we would get if we were to maximize
    // // all the locales and check against default_contents
    //
    // for (String locale : defaultContents) {
    // CLDRFile cldrFileToCheck = null;
    // try {
    // cldrFileToCheck = factory.make(locale, false);
    // } catch (Exception e) {}
    // if (cldrFileToCheck == null) {
    // logln("Present in default contents but has no XML file:\t" + locale);
    // continue;
    // }
    // logln("Locale:\t" + locale);
    // // verify empty, except for identity elements and alias
    // for (String path : cldrFileToCheck) {
    // if (path.contains("/identity/")) {
    // continue;
    // }
    // errln("Default content locale not empty:\t" + locale + ", " + path);
    // break;
    // }
    // }
    // }

    Matcher aliasMatcher = Pattern.compile("//ldml.*/alias.*").matcher("");

    private void checkAliasValues(CLDRFile cldrFileToCheck, Set<String> locales) {
        Set<String> aliasPaths = new TreeSet<String>();
        Set<String> allAliasPaths = cldrFileToCheck.getPaths("//ldml/", aliasMatcher, aliasPaths);
        XPathParts xpp = new XPathParts();
        for (String aliasPath : allAliasPaths) {
            if (aliasPath.startsWith("//ldml/alias")) {
                continue; // we have different tests elsewhere
            }
            String fullPath = cldrFileToCheck.getFullXPath(aliasPath);
            String aliasLocale = xpp.set(fullPath).getAttributeValue(-1, "source");
            // just check to make sure that the alias is in the locales
            if (aliasLocale != null && !aliasLocale.equals("locale")) {
                if (!locales.contains(aliasLocale)) {
                    errln("Unknown Alias:\t" + aliasLocale + "\t in\t" + fullPath);
                }
            }
            String aliasPathNew = xpp.set(fullPath).getAttributeValue(-1, "path");
            // just one check
            if (".".equals(aliasPathNew)) {
                errln("Illegal path, must not be .:\t" + aliasLocale + "\t in\t" + fullPath);
            }

        }
    }

    private String minimize(Map<String, String> likelySubtags, String locale) {
        String result = GenerateMaximalLocales.minimize(locale, likelySubtags, false);
        if (result == null) {
            LanguageTagParser ltp3 = new LanguageTagParser().set(locale);
            List<String> variants = ltp3.getVariants();
            Map<String, String> extensions = ltp3.getExtensions();
            Set<String> emptySet = Collections.emptySet();
            ltp3.setVariants(emptySet);
            Map<String, String> emptyMap = Collections.emptyMap();
            ltp3.setExtensions(emptyMap);
            String newLocale = ltp3.toString();
            result = GenerateMaximalLocales.minimize(newLocale, likelySubtags, false);
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
            return codePoint >= 0x10000 && codePoint == Character.codePointAt(string, 0);
        default:
            return false;
        }
    }

    // TODO move this into central utilities

    private static final StandardCodes STANDARD_CODES = StandardCodes.make();
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

    private void checkValidCode(String localeID, String subtagType, String subtag, boolean allowDeprecated) {
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
            R2<List<String>, String> deprecatedInfo = DEPRECATED_INFO.get(subtagType).get(subtag);
            if (deprecatedInfo != null) {
                errln("Locale " + localeID + " contains deprecated " + showCode(subtagType, subtag) + " "
                    + deprecatedInfo.get1()
                    + "; suggest " + showName(deprecatedInfo.get0(), subtagType));
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
        Map<String, String> data = STANDARD_CODES.getLangData(subtagType, subtag);
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
        return a == b ? true
            : a == null || b == null ? false
                : a.equals(b);
    }

    // TODO move this into central utilities
    private <K, V> String showDifferences(Map<K, V> a, Map<K, V> b) {
        StringBuilder result = new StringBuilder();
        Set<K> keys = new LinkedHashSet<K>();
        keys.addAll(a.keySet());
        keys.addAll(b.keySet());
        for (K key : keys) {
            if (!a.containsKey(key)) {
                result.append(key).append("→‹").append(a.get(key)).append("›,∅; ");
            } else if (!b.containsKey(key)) {
                result.append(key).append("→∅,‹").append(b.get(key)).append("›; ");
            } else {
                V aKey = a.get(key);
                V bKey = b.get(key);
                if (!equals(aKey, bKey)) {
                    result.append(key).append("→‹").append(a.get(key)).append("›,‹").append(b.get(key)).append("›; ");
                }
            }
        }
        return result.toString();
    }
}
