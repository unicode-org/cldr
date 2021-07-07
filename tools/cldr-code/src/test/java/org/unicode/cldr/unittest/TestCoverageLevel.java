package org.unicode.cldr.unittest;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.ChainedMap.M4;
import org.unicode.cldr.util.Counter2;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdData.Element;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.GrammarInfo;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.LogicalGrouping;
import org.unicode.cldr.util.LogicalGrouping.PathType;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.Factory;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.RegexLookup.Finder;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.CurrencyDateInfo;
import org.unicode.cldr.util.SupplementalDataInfo.OfficialStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;
import org.unicode.cldr.util.XPathParts;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.CompactDecimalFormat;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.Transform;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.ULocale;

public class TestCoverageLevel extends TestFmwkPlus {

    private static CLDRConfig testInfo = CLDRConfig.getInstance();
    private static final StandardCodes STANDARD_CODES = StandardCodes.make();
    private static final CLDRFile ENGLISH = testInfo.getEnglish();
    private static final SupplementalDataInfo SDI = testInfo.getSupplementalDataInfo();

    public static void main(String[] args) {
        new TestCoverageLevel().run(args);
    }

    public void testSpecificPaths() {
        String[][] rows = {
            { "//ldml/characters/parseLenients[@scope=\"number\"][@level=\"lenient\"]/parseLenient[@sample=\",\"]", "moderate", "20" }
        };
        Factory phf = PathHeader.getFactory(ENGLISH);
        CoverageLevel2 coverageLevel = CoverageLevel2.getInstance(SDI, "fr");
        CLDRLocale loc = CLDRLocale.getInstance("fr");
        for (String[] row : rows) {
            String path = row[0];
            Level expectedLevel = Level.fromString(row[1]);
            Level level = coverageLevel.getLevel(path);
            assertEquals("Level for " + path, expectedLevel, level);

            int expectedRequiredVotes = Integer.parseInt(row[2]);
            int votes = SDI.getRequiredVotes(loc, phf.fromPath(path));
            assertEquals("Votes for " + path, expectedRequiredVotes, votes);
        }
    }

    public void testSpecificPathsPersCal() {
        String[][] rows = {
            { "//ldml/dates/calendars/calendar[@type=\"persian\"]/eras/eraAbbr/era[@type=\"0\"]", "basic", "4" },
            { "//ldml/dates/calendars/calendar[@type=\"persian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"1\"]", "basic", "4" }
        };
        Factory phf = PathHeader.getFactory(ENGLISH);
        CoverageLevel2 coverageLevel = CoverageLevel2.getInstance(SDI, "ckb_IR");
        CLDRLocale loc = CLDRLocale.getInstance("ckb_IR");
        for (String[] row : rows) {
            String path = row[0];
            Level expectedLevel = Level.fromString(row[1]);
            Level level = coverageLevel.getLevel(path);
            assertEquals("Level for " + path, expectedLevel, level);

            int expectedRequiredVotes = Integer.parseInt(row[2]);
            int votes = SDI.getRequiredVotes(loc, phf.fromPath(path));
            assertEquals("Votes for " + path, expectedRequiredVotes, votes);
        }
    }

    public void oldTestInvariantPaths() {
        org.unicode.cldr.util.Factory factory = testInfo.getCldrFactory();
        PathStarrer pathStarrer = new PathStarrer().setSubstitutionPattern("*");
        SupplementalDataInfo sdi = SupplementalDataInfo
            .getInstance(CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);

        Set<String> allPaths = new HashSet<>();
        M4<String, String, Level, Boolean> starredToLocalesToLevels = ChainedMap
            .of(new TreeMap<String, Object>(),
                new TreeMap<String, Object>(),
                new TreeMap<Level, Object>(), Boolean.class);

        for (String locale : factory.getAvailableLanguages()) {
            logln(locale);
            CLDRFile cldrFileToCheck = factory.make(locale, true);
            for (String path : cldrFileToCheck.fullIterable()) {
                allPaths.add(path);
                String starred = pathStarrer.set(path);
                Level level = sdi.getCoverageLevel(path, locale);
                starredToLocalesToLevels.put(starred, locale, level, true);
            }
        }

        Set<Level> levelsFound = EnumSet.noneOf(Level.class);
        Set<String> localesWithUniqueLevels = new TreeSet<>();
        for (Entry<String, Map<String, Map<Level, Boolean>>> entry : starredToLocalesToLevels) {
            String starred = entry.getKey();
            Map<String, Map<Level, Boolean>> localesToLevels = entry.getValue();
            int maxLevelCount = 0;
            double localeCount = 0;
            levelsFound.clear();
            localesWithUniqueLevels.clear();

            for (Entry<String, Map<Level, Boolean>> entry2 : localesToLevels
                .entrySet()) {
                String locale = entry2.getKey();
                Map<Level, Boolean> levels = entry2.getValue();
                levelsFound.addAll(levels.keySet());
                if (levels.size() > maxLevelCount) {
                    maxLevelCount = levels.size();
                }
                if (levels.size() == 1) {
                    localesWithUniqueLevels.add(locale);
                }
                localeCount++;
            }
            System.out.println(maxLevelCount
                + "\t"
                + localesWithUniqueLevels.size()
                / localeCount
                + "\t"
                + starred
                + "\t"
                + Joiner.on(", ").join(levelsFound)
                + "\t"
                + (maxLevelCount == 1 ? "all" : localesWithUniqueLevels
                    .size() == 0 ? "none" : Joiner.on(", ").join(localesWithUniqueLevels)));
        }
    }

    enum LanguageStatus {
        Lit100M("P1"), Lit10MandOfficial("P2"), Lit1MandOneThird("P3");
        final String name;

        LanguageStatus(String name) {
            this.name = name;
        }
    }

    static Relation<String, LanguageStatus> languageStatus = Relation.of(
        new HashMap<String, Set<LanguageStatus>>(), TreeSet.class);
    static Counter2<String> languageLiteratePopulation = new Counter2<>();
    static Map<String, Date> currencyToLast = new HashMap<>();
    static Set<String> officialSomewhere = new HashSet<>();

    static {
        Counter2<String> territoryLiteratePopulation = new Counter2<>();
        LanguageTagParser parser = new LanguageTagParser();
        // cf
        // http://cldr.unicode.org/development/development-process/design-proposals/languages-to-show-for-translation
        for (String language : SDI
            .getLanguagesForTerritoriesPopulationData()) {
            String base = parser.set(language).getLanguage();
            boolean isOfficial = false;
            double languageLiterate = 0;
            for (String territory : SDI
                .getTerritoriesForPopulationData(language)) {
                PopulationData pop = SDI
                    .getLanguageAndTerritoryPopulationData(language,
                        territory);
                OfficialStatus officialStatus = pop.getOfficialStatus();
                if (officialStatus.compareTo(OfficialStatus.de_facto_official) >= 0) {
                    isOfficial = true;
                    languageStatus.put(base + "_" + territory,
                        LanguageStatus.Lit10MandOfficial);
                    officialSomewhere.add(base);
                }
                double litPop = pop.getLiteratePopulation();
                languageLiterate += litPop;
                territoryLiteratePopulation.add(territory, litPop);
                languageLiteratePopulation.add(base + "_" + territory, litPop);
            }
            languageLiteratePopulation.add(base, languageLiterate);
            if (languageLiterate > 100000000) {
                languageStatus.put(base, LanguageStatus.Lit100M);
            }
            if (languageLiterate > 10000000 && isOfficial) {
                languageStatus.put(base, LanguageStatus.Lit10MandOfficial);
            }
        }
        for (String language : SDI
            .getLanguagesForTerritoriesPopulationData()) {
            if (languageLiteratePopulation.getCount(language) < 1000000) {
                continue;
            }
            String base = parser.set(language).getLanguage();
            for (String territory : SDI
                .getTerritoriesForPopulationData(language)) {
                PopulationData pop = SDI
                    .getLanguageAndTerritoryPopulationData(language,
                        territory);
                double litPop = pop.getLiteratePopulation();
                double total = territoryLiteratePopulation.getCount(territory);
                if (litPop > total / 3) {
                    languageStatus.put(base, LanguageStatus.Lit1MandOneThird);
                }
            }
        }
        for (String territory : STANDARD_CODES.getAvailableCodes(
            "territory")) {
            Set<CurrencyDateInfo> cdateInfo = SDI.getCurrencyDateInfo(territory);
            if (cdateInfo == null) {
                continue;
            }
            for (CurrencyDateInfo dateInfo : cdateInfo) {
                String currency = dateInfo.getCurrency();
                Date last = dateInfo.getEnd();
                Date old = currencyToLast.get(currency);
                if (old == null || old.compareTo(last) < 0) {
                    currencyToLast.put(currency, last);
                }
            }
        }
    }

    static CompactDecimalFormat cdf = CompactDecimalFormat.getInstance(
        ULocale.ENGLISH, CompactStyle.SHORT);

    static String isBigLanguage(String lang) {
        Set<LanguageStatus> status = languageStatus.get(lang);
        Double size = languageLiteratePopulation.getCount(lang);
        String sizeString = size == null ? "?" : cdf.format(size);
        String off = officialSomewhere.contains(lang) ? "o" : "";
        if (status == null || status.isEmpty()) {
            return "P4-" + sizeString + off;
        }
        return status.iterator().next().name + "-" + sizeString + off;
    }

    static final Date NOW = new Date();

    static class TypeName implements Transform<String, String> {
        private final int field;
        private final Map<String, R2<List<String>, String>> dep;

        public TypeName(int field) {
            this.field = field;
            switch (field) {
            case CLDRFile.LANGUAGE_NAME:
                dep = SDI.getLocaleAliasInfo()
                .get("language");
                break;
            case CLDRFile.TERRITORY_NAME:
                dep = SDI.getLocaleAliasInfo()
                .get("territory");
                break;
            case CLDRFile.SCRIPT_NAME:
                dep = SDI.getLocaleAliasInfo()
                .get("script");
                break;
            default:
                dep = null;
                break;
            }
        }

        @Override
        public String transform(String source) {
            String result = ENGLISH.getName(field, source);
            String extra = "";
            if (field == CLDRFile.LANGUAGE_NAME) {
                String lang = isBigLanguage(source);
                extra = lang == null ? "X" : lang;
            } else if (field == CLDRFile.CURRENCY_NAME) {
                Date last = currencyToLast.get(source);
                extra = last == null ? "?" : last.compareTo(NOW) < 0 ? "old"
                    : "";
            }
            R2<List<String>, String> depValue = dep == null ? null : dep
                .get(source);
            if (depValue != null) {
                extra += extra.isEmpty() ? "" : "-";
                extra += depValue.get1();
            }
            return result + (extra.isEmpty() ? "" : "\t" + extra);
        }
    }

    RegexLookup<Level> exceptions = RegexLookup.of(null,
        new Transform<String, Level>() {
        @Override
        public Level transform(String source) {
            return Level.fromLevel(Integer.parseInt(source));
        }
    }, null).loadFromFile(TestCoverageLevel.class,
        "TestCoverageLevel.txt");

    public void TestExceptions() {
        for (Map.Entry<Finder, Level> x : exceptions) {
            logln(x.getKey().toString() + " => " + x.getValue());
        }
    }

    public void TestNarrowCurrencies() {
        String path = "//ldml/numbers/currencies/currency[@type=\"USD\"]/symbol[@alt=\"narrow\"]";
        String value = ENGLISH.getStringValue(path);
        assertEquals("Narrow $", "$", value);
        SupplementalDataInfo sdi = SupplementalDataInfo
            .getInstance(CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);
        Level level = sdi.getCoverageLevel(path, "en");
        assertEquals("Narrow $", Level.BASIC, level);
    }

    public void TestA() {
        String path = "//ldml/characterLabels/characterLabel[@type=\"other\"]";
        SupplementalDataInfo sdi = SupplementalDataInfo
            .getInstance(CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);
        Level level = sdi.getCoverageLevel(path, "en");
        assertEquals("Quick Check for any attribute", Level.MODERN, level);
    }

    public void TestCoverageCompleteness() {
        /**
         * Check that English paths are, except for known cases, at least modern coverage.
         * We filter out the things we know about and have determined are OK to be in comprehensive.
         * If we add a path that doesn't get its coverage set, this test should complain about it.
         */
        final ImmutableSet<String> inactiveMetazones = ImmutableSet.of("Bering", "Dominican", "Shevchenko", "Alaska_Hawaii", "Yerevan",
            "Africa_FarWestern", "British", "Sverdlovsk", "Karachi", "Malaya", "Oral", "Frunze", "Dutch_Guiana", "Irish", "Uralsk", "Tashkent", "Kwajalein",
            "Ashkhabad", "Kizilorda", "Kuybyshev", "Baku", "Dushanbe", "Goose_Bay", "Liberia", "Samarkand", "Tbilisi", "Borneo", "Greenland_Central",
            "Dacca", "Aktyubinsk", "Turkey", "Urumqi", "Acre", "Almaty", "Anadyr", "Aqtau", "Aqtobe", "Kamchatka", "Macau", "Qyzylorda", "Samara",
            "Casey", "Guam", "Lanka", "North_Mariana");

        final Pattern calendar100 = PatternCache.get("(coptic|ethiopic-amete-alem|islamic-(rgsa|tbla|umalqura))");

        final Pattern language100 = PatternCache.get("("
            + "ach|aeb?|afh|ak[kz]|aln|ang|ar[coqswyz]|ase|avk|"
            + "ba[lrx]|bb[cj]|be[jw]|bf[dq]|bgn|bik|bjn|bkm|bpy|bqi|br[ah]|bss|bu[am]|byv|"
            + "ca[dry]|cch|ch[bgnp]|cic|cop|cps|crh?|csb|"
            + "de[ln]|din|doi|dtp|dum|dyu|"
            + "eg[ly]|elx|enm|esu|ext|"
            + "fa[nt]|fit|fr[cmoprs]|"
            + "ga[gny]|gb[az]|glk|gmh|go[hmnt]|gr[bc]|gu[cr]|"
            + "ha[ik]|hi[ft]|ho|hsn|"
            + "i[ek]|izh|"
            + "jam|jpr|jrb|jut|"
            + "ka[aw]|kbl|ken|kgp?|kh[ow]|kiu|ko[is]|kr[ij]|kut|"
            + "la[hm]|lfn|li[jv]|lmo|lo[lu]|ltg|lui|lz[hz]|"
            + "ma[fn]|md[er]|mga|mnc|mrj|mus|mw[rv]|mye|"
            + "nan|nds(_NL)?|njo|no[nv]?|nwc|ny[mo]|nzi|"
            + "oj|osa|ota|"
            + "pal|pcd|pd[ct]|peo|pfl|phn|pi|pms|pnt|pon|pro|"
            + "qug|"
            + "raj|rgn|rif|rom|rtm|ru[eg]|"
            + "sa[msz]|sbp|sd[ch]|se[eil]|sg[as]|shu?|sid|sl[iy]|sog|srr|stq|su[sx]|syc|szl|"
            + "tcy|ter|tiv|tk[lr]|tl[iy]?|tmh|tog|tpi|tru|ts[di]|ttt|tw|"
            + "uga|"
            + "ve[cp]|vls|vmf||vot|vro|"
            + "was|wbp|wuu|"
            + "xmf|"
            + "ya[op]|yrl|"
            + "zap?|zbl|ze[an]|"
            + "gil|tlh|gil|tlh|tet|ro_MD|ss|new|ba|iu|suk|kmb|rup|sms|udm|lus|gn|ada|kbd|kcg|eka|"
            + "dak|nap|bin|arn|kfo|ch|ab|fa_AF|kac|ty|tvl|arp|aa|ng|hup|wa|min|ilo|kru|hil|sat|bho|"
            + "jbo|pag|tig|bi|tyv|pcm|ace|tum|mh|fon|chk|awa|root|hz|chm|mdf|kaj|nr|dar|shn|zun|"
            + "cho|li|moh|nso|sw_CD|srn|lad|ve|gaa|pam|ale|sma|sba|lua|kha|sc|nv|men|cv|quc|pap|bla|"
            + "kj|anp|an|niu|mni|dv|swb|pau|gor|nqo|krc|crs|gwi|zza|mad|nog|lez|byn|sad|ssy|mag|iba|"
            + "tpi|kum|wal|mos|dzg|gez|io|tn|snk|mai|ady|chy|mwl|sco|av|efi|war|mic|loz|scn|smj|tem|"
            + "dgr|mak|inh|lun|ts|fj|na|kpe|sr_ME|trv|rap|bug|ban|xal|oc|alt|nia|myv|ain|rar|krl|ay|"
            + "syr|kv|umb|cu|prg|vo)");

        /**
         * Recommended scripts that are allowed for comprehensive coverage.
         * Not-recommended scripts (according to ScriptMetadata) are filtered out automatically.
         */
        final Pattern script100 = PatternCache.get("(Zinh)");

        final Pattern keys100 = PatternCache.get("(col(Alternate|Backwards|CaseFirst|CaseLevel|HiraganaQuaternary|"
            + "Normalization|Numeric|Reorder|Strength)|kv|sd|timezone|va|variableTop|x|d0|h0|i0|k0|m0|s0)");

        final Pattern numberingSystem100 = PatternCache.get("("
            + "finance|native|traditional|adlm|ahom|bali|bhks|brah|cakm|cham|cyrl|diak|"
            + "gong|gonm|hanidays|hmng|hmnp|java|jpanyear|kali|lana(tham)?|lepc|limb|"
            + "math(bold|dbl|mono|san[bs])|modi|mong|mroo|mtei|mymr(shan|tlng)|"
            + "newa|nkoo|olck|osma|rohg|saur|segment|shrd|sin[dh]|sora|sund|takr|talu|tirh|tnsa|vaii|wara|wcho)");

        final Pattern collation100 = PatternCache.get("("
            + "big5han|compat|dictionary|emoji|eor|gb2312han|phonebook|phonetic|pinyin|reformed|searchjl|stroke|traditional|unihan|zhuyin)");

        SupplementalDataInfo sdi = testInfo.getSupplementalDataInfo();
        CLDRFile english = testInfo.getEnglish();

        // Calculate date of the upcoming CLDR release, minus 5 years (deprecation policy)
        final int versionNumber = Integer.valueOf((CLDRFile.GEN_VERSION).split("\\.")[0]);
        Calendar cal = Calendar.getInstance();
        cal.set(versionNumber / 2 + versionNumber % 2 + 2001, 8 - (versionNumber % 2) * 6, 15);
        Date cldrReleaseMinus5Years = cal.getTime();
        Set<String> modernCurrencies = SDI.getCurrentCurrencies(SDI.getCurrencyTerritories(), cldrReleaseMinus5Years, NOW);

        Set<String> needsNumberSystem = new HashSet<>();
        DtdData dtdData = DtdData.getInstance(DtdType.ldml);
        Element numbersElement = dtdData.getElementFromName().get("numbers");
        for (Element childOfNumbers : numbersElement.getChildren().keySet()) {
            if (childOfNumbers.containsAttribute("numberSystem")) {
                needsNumberSystem.add(childOfNumbers.name);
            }
        }

        for (String path : english.fullIterable()) {
            logln("Testing path => " + path);
            XPathParts xpp = XPathParts.getFrozenInstance(path);
            if (path.endsWith("/alias") || path.matches("//ldml/(identity|contextTransforms|layout|localeDisplayNames/transformNames)/.*")) {
                continue;
            }
            if (sdi.isDeprecated(DtdType.ldml, path)) {
                continue;
            }
            Level lvl = sdi.getCoverageLevel(path, "en");
            if (lvl == Level.UNDETERMINED) {
                errln("Undetermined coverage value for path => " + path);
                continue;
            }
            if (lvl.compareTo(Level.MODERN) <= 0) {
                logln("Level OK [" + lvl.toString() + "] for path => " + path);
                continue;
            }

            if (path.startsWith("//ldml/numbers")) {
                // Paths in numbering systems outside "latn" are specifically excluded.
                String numberingSystem = xpp.findFirstAttributeValue("numberSystem");
                if (numberingSystem != null && !numberingSystem.equals("latn")) {
                    continue;
                }
                if (xpp.containsElement("currencySpacing") ||
                    xpp.containsElement("list")) {
                    continue;
                }
                if (xpp.containsElement("currency")) {
                    String currencyType = xpp.findAttributeValue("currency", "type");
                    if (!modernCurrencies.contains(currencyType)) {
                        continue; // old currency or not tender, so we don't care
                    }
                }
                // Currently not collecting timeSeparator data in SurveyTool
                if (xpp.containsElement("timeSeparator")) {
                    continue;
                }
                // Other paths in numbers without a numbering system are deprecated.
//                if (numberingSystem == null) {
//                    continue;
//                }
                if (needsNumberSystem.contains(xpp.getElement(2))) {
                    continue;
                }
            } else if (xpp.containsElement("zone")) {
                String zoneType = xpp.findAttributeValue("zone", "type");
                if ((zoneType.startsWith("Etc/GMT") || zoneType.equals("Etc/UTC"))
                    && path.endsWith("exemplarCity")) {
                    continue;
                }
                // We don't survey for short timezone names or at least some alts
                if (path.contains("/short/") || path.contains("[@alt=\"formal\"]")) {
                    continue;
                }
            } else if (xpp.containsElement("metazone")) {
                // We don't survey for short metazone names
                if (path.contains("/short/")) {
                    continue;
                }
                String mzName = xpp.findAttributeValue("metazone", "type");
                // Skip inactive metazones.
                if (inactiveMetazones.contains(mzName)) {
                    continue;
                }
                // Skip paths for daylight or generic mz strings where
                // the mz doesn't use DST.
                if ((path.endsWith("daylight") || path.endsWith("generic")) &&
                    !LogicalGrouping.metazonesDSTSet.contains(mzName)) {
                    continue;
                }
            } else if (path.startsWith("//ldml/dates/fields")) {
                if ("variant".equals(xpp.findAttributeValue("displayName", "alt"))) {
                    continue;
                }
                // relative day/week/month, etc. short or narrow
                if (xpp.getElement(-1).equals("relative")) {
                    String fieldType = xpp.findAttributeValue("field", "type");
                    if (fieldType.matches(".*-(short|narrow)|quarter")) {
                        continue;
                    }
                    // "now" - [JCE] not sure on this so I opened ticket #8833
                    if (fieldType.equals("second") && xpp.findAttributeValue("relative", "type").equals("0")) {
                        continue;
                    }
                }
            } else if (xpp.containsElement("language")) {
                // Comprehensive coverage is OK for some languages.
                String languageType = xpp.findAttributeValue("language", "type");
                if (language100.matcher(languageType).matches()) {
                    continue;
                }
            } else if (xpp.containsElement("script")) {
                // Skip user defined script codes and alt=short
                String scriptType = xpp.findAttributeValue("script", "type");
                if (scriptType.startsWith("Q") || "short".equals(xpp.findAttributeValue("script", "alt"))) {
                    continue;
                }
                ScriptMetadata.Info scriptInfo = ScriptMetadata.getInfo(scriptType);
                if (scriptInfo == null || scriptInfo.idUsage != ScriptMetadata.IdUsage.RECOMMENDED) {
                    continue;
                }
                if (script100.matcher(scriptType).matches()) {
                    continue;
                }
            } else if (xpp.containsElement("territory")) {
                // All territories are usually modern, unless the territory code is deprecated.  The only
                // such one right now is "AN" (Netherlands Antilles), which should go outside the 5-year
                // deprecation window in 2016.
                String territoryType = xpp.findAttributeValue("territory", "type");
                if (territoryType.equals("AN")) {
                    continue;
                }
            } else if (xpp.containsElement("key")) {
                // Comprehensive coverage is OK for some key/types.
                String keyType = xpp.findAttributeValue("key", "type");
                if (keys100.matcher(keyType).matches()) {
                    continue;
                }
            } else if (xpp.containsElement("type")) {
                if ("short".equals(xpp.findAttributeValue("type", "alt"))) {
                    continue;
                }
                // Comprehensive coverage is OK for some key/types.
                String keyType = xpp.findAttributeValue("type", "key");
                if (keys100.matcher(keyType).matches()) {
                    continue;
                }
                if (keyType.equals("numbers")) {
                    String ns = xpp.findAttributeValue("type", "type");
                    if (numberingSystem100.matcher(ns).matches()) {
                        continue;
                    }
                }
                if (keyType.equals("collation")) {
                    String ct = xpp.findAttributeValue("type", "type");
                    if (collation100.matcher(ct).matches()) {
                        continue;
                    }
                }
                if (keyType.equals("calendar")) {
                    String ct = xpp.findAttributeValue("type", "type");
                    if (calendar100.matcher(ct).matches()) {
                        continue;
                    }
                }
            } else if (xpp.containsElement("variant")) {
                // All variant names are comprehensive coverage
                continue;
            } else if (path.startsWith("//ldml/dates/calendars")) {
                String calType = xpp.findAttributeValue("calendar", "type");
                if (!calType.matches("(gregorian|generic)")) {
                    continue;
                }
                // So far we are generating datetimeSkeleton mechanically, no coverage
                if (xpp.containsElement("datetimeSkeleton")) {
                    continue;
                }
                String element = xpp.getElement(-1);
                // Skip things that shouldn't normally exist in the generic calendar
                // days, dayPeriods, quarters, and months
                if (calType.equals("generic")) {
                    if (element.matches("(day(Period)?|month|quarter|era|appendItem)")) {
                        continue;
                    }
                    if (xpp.containsElement("intervalFormatItem")) {
                        String intervalFormatID = xpp.findAttributeValue("intervalFormatItem", "id");
                        // "Time" related, so shouldn't be in generic calendar.
                        if (intervalFormatID.matches("(h|H).*")) {
                            continue;
                        }
                    }
                    if (xpp.containsElement("dateFormatItem")) {
                        String dateFormatID = xpp.findAttributeValue("dateFormatItem", "id");
                        // "Time" related, so shouldn't be in generic calendar.
                        if (dateFormatID.matches("E?(h|H|m).*")) {
                            continue;
                        }
                    }
                    if (xpp.containsElement("timeFormat")) {
                        continue;
                    }
                } else { // Gregorian calendar
                    if (xpp.containsElement("eraNarrow")) {
                        continue;
                    }
                    if (element.equals("appendItem")) {
                        String request = xpp.findAttributeValue("appendItem", "request");
                        if (!request.equals("Timezone")) {
                            continue;
                        }
                    } else if (element.equals("dayPeriod")) {
                        if ("variant".equals(xpp.findAttributeValue("dayPeriod", "alt"))) {
                            continue;
                        }
                    } else if (element.equals("dateFormatItem")) {
                        //ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='%dateFormatItems']
                        assertEquals(path, Level.BASIC, lvl);
                        continue;
                    }
                }
            } else if (path.startsWith("//ldml/units")) {
                // Skip paths for narrow unit fields.
                if ("narrow".equals(xpp.findAttributeValue("unitLength", "type"))
                    || path.endsWith("/compoundUnitPattern1")
                    ) {
                    continue;
                }
            }

            errln("Comprehensive & no exception for path =>\t" + path);
        }
    }

    public void testBreakingLogicalGrouping() {
        checkBreakingLogicalGrouping("en");
        checkBreakingLogicalGrouping("ar");
        checkBreakingLogicalGrouping("de");
        checkBreakingLogicalGrouping("pl");
    }

    private void checkBreakingLogicalGrouping(String localeId) {
        SupplementalDataInfo sdi = testInfo.getSupplementalDataInfo();
        CLDRFile cldrFile = testInfo.getCldrFactory().make(localeId, true);
        HashSet<String> seen = new HashSet<>();
        Multimap<Level, String> levelToPaths = TreeMultimap.create();
        int count = 0;
        for (String path : cldrFile.fullIterable()) {
            if (seen.contains(path)) {
                continue;
            }
            Set<String> grouping = LogicalGrouping.getPaths(cldrFile, path);
            seen.add(path);
            if (grouping == null) {
                continue;
            }
            seen.addAll(grouping);
            levelToPaths.clear();
            for (String groupingPath : grouping) {
                if (LogicalGrouping.isOptional(cldrFile, groupingPath)) {
                    continue;
                }
                Level level = sdi.getCoverageLevel(groupingPath, localeId);
                levelToPaths.put(level, groupingPath);
            }
            if (levelToPaths.keySet().size() <= 1) {
                continue;
            }
            // we have a failure
            for (Entry<Level, Collection<String>> entry : levelToPaths.asMap().entrySet()) {
                errln(localeId + " (" + count + ") Broken Logical Grouping: " + entry.getKey() + " => " + entry.getValue());
            }
            ++count;
        }
    }

    public void testLogicalGroupingSamples() {
        getLogger().fine(GrammarInfo.getGrammarLocales().toString());
        String[][] test = {
            {"de",
                "SINGLETON",
                "//ldml/localeDisplayNames/localeDisplayPattern/localePattern",
            },
            {"de",
                "METAZONE",
                "//ldml/dates/timeZoneNames/metazone[@type=\"Alaska\"]/long/generic",
                "//ldml/dates/timeZoneNames/metazone[@type=\"Alaska\"]/long/standard",
                "//ldml/dates/timeZoneNames/metazone[@type=\"Alaska\"]/long/daylight",
            },
            {"de",
                "DAYS",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"sun\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"mon\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"tue\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"wed\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"thu\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"fri\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"sat\"]",
            },
            {"nl",
                "DAY_PERIODS",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"wide\"]/dayPeriod[@type=\"morning1\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"wide\"]/dayPeriod[@type=\"afternoon1\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"wide\"]/dayPeriod[@type=\"evening1\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"wide\"]/dayPeriod[@type=\"night1\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"wide\"]/dayPeriod[@type=\"midnight\"]",
            },
            {"de",
                "QUARTERS",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/quarters/quarterContext[@type=\"format\"]/quarterWidth[@type=\"wide\"]/quarter[@type=\"1\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/quarters/quarterContext[@type=\"format\"]/quarterWidth[@type=\"wide\"]/quarter[@type=\"2\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/quarters/quarterContext[@type=\"format\"]/quarterWidth[@type=\"wide\"]/quarter[@type=\"3\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/quarters/quarterContext[@type=\"format\"]/quarterWidth[@type=\"wide\"]/quarter[@type=\"4\"]",
            },
            {"de",
                "MONTHS",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"1\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"2\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"3\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"4\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"5\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"6\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"7\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"8\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"9\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"10\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"11\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"12\"]",
            },
            {"de",
                "RELATIVE",
                "//ldml/dates/fields/field[@type=\"week-short\"]/relative[@type=\"-1\"]",
                "//ldml/dates/fields/field[@type=\"week-short\"]/relative[@type=\"0\"]",
                "//ldml/dates/fields/field[@type=\"week-short\"]/relative[@type=\"1\"]",
            },
            {"de",
                "DECIMAL_FORMAT_LENGTH",
                "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"long\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"1000\"][@count=\"one\"]",
                "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"long\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"1000\"][@count=\"other\"]",
                "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"long\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"10000\"][@count=\"one\"]",
                "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"long\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"10000\"][@count=\"other\"]",
                "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"long\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"100000\"][@count=\"one\"]",
                "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"long\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"100000\"][@count=\"other\"]",
            },
            {"cs",
                "COUNT",
                "//ldml/numbers/currencies/currency[@type=\"BMD\"]/displayName[@count=\"one\"]",
                "//ldml/numbers/currencies/currency[@type=\"BMD\"]/displayName[@count=\"few\"]",
                "//ldml/numbers/currencies/currency[@type=\"BMD\"]/displayName[@count=\"many\"]",
                "//ldml/numbers/currencies/currency[@type=\"BMD\"]/displayName[@count=\"other\"]",
            },
            {"de",
                "COUNT",
                "//ldml/numbers/minimalPairs/pluralMinimalPairs[@count=\"one\"]",
                "//ldml/numbers/minimalPairs/pluralMinimalPairs[@count=\"other\"]",
            },
            {"de",
                "COUNT_CASE",
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"area-square-kilometer\"]/unitPattern[@count=\"one\"][@case=\"accusative\"]",
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"area-square-kilometer\"]/unitPattern[@count=\"one\"][@case=\"dative\"]",
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"area-square-kilometer\"]/unitPattern[@count=\"one\"][@case=\"genitive\"]",
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"area-square-kilometer\"]/unitPattern[@count=\"one\"]",
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"area-square-kilometer\"]/unitPattern[@count=\"other\"][@case=\"accusative\"]",
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"area-square-kilometer\"]/unitPattern[@count=\"other\"][@case=\"dative\"]",
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"area-square-kilometer\"]/unitPattern[@count=\"other\"][@case=\"genitive\"]",
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"area-square-kilometer\"]/unitPattern[@count=\"other\"]",
            },
            {"hi",
                "COUNT_CASE_GENDER",
                "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"one\"]",
                "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"one\"][@gender=\"feminine\"]",
                "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"other\"]",
                "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"other\"][@gender=\"feminine\"]",
                "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"one\"][@case=\"oblique\"]",
                "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"one\"][@gender=\"feminine\"][@case=\"oblique\"]",
                "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"other\"][@case=\"oblique\"]",
                "//ldml/units/unitLength[@type=\"long\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"other\"][@gender=\"feminine\"][@case=\"oblique\"]"
            }
        };
        Set<PathType> seenPt = new TreeSet<>(Arrays.asList(PathType.values()));
        for (String[] row : test) {
            String locale = row[0];
            PathType expectedPathType = PathType.valueOf(row[1]);
            CLDRFile cldrFile = testInfo.getCldrFactory().make(locale, true);
            List<String> paths = Arrays.asList(row);
            paths = paths.subList(2, paths.size());
            Set<String> expected = new TreeSet<>(paths);
            Set<Multimap<String, String>> seen = new LinkedHashSet<>();
            for (String path : expected) {
                Set<String> grouping = new TreeSet<>(LogicalGrouping.getPaths(cldrFile, path));
                final Multimap<String, String> deltaValue = delta(expected, grouping);
                if (seen.add(deltaValue)) {
                    assertEquals("Logical group for " + locale + ", " + path, ImmutableListMultimap.of(), deltaValue);
                }
                PathType actualPathType = PathType.getPathTypeFromPath(path);
                assertEquals("PathType", expectedPathType, actualPathType);
            }
            seenPt.remove(expectedPathType);
        }
        assertEquals("PathTypes tested", Collections.emptySet(), seenPt);
        logKnownIssue("CLDR-13951", "Add more LogicalGrouping tests, fix DECIMAL_FORMAT_LENGTH, etc.");
    }

    private Multimap<String,String> delta(Set<String> expected, Set<String> grouping) {
        if (expected.equals(grouping)) {
            return ImmutableListMultimap.of();
        }
        Multimap<String,String> result = LinkedHashMultimap.create();
        TreeSet<String> aMinusB = new TreeSet<>(expected);
        aMinusB.removeAll(grouping);
        result.putAll("expected-actual", aMinusB);
        TreeSet<String> bMinusA = new TreeSet<>(grouping);
        bMinusA.removeAll(expected);
        result.putAll("actual-expected", bMinusA);
        return result;
    }
}
