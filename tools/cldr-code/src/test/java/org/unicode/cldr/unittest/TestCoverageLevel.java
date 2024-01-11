package org.unicode.cldr.unittest;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.text.CompactDecimalFormat;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.Transform;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;
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
import org.unicode.cldr.tool.LikelySubtags;
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
import org.unicode.cldr.util.LocaleNames;
import org.unicode.cldr.util.LogicalGrouping;
import org.unicode.cldr.util.LogicalGrouping.PathType;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.Factory;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.RegexLookup.Finder;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.CoverageVariableInfo;
import org.unicode.cldr.util.SupplementalDataInfo.CurrencyDateInfo;
import org.unicode.cldr.util.SupplementalDataInfo.OfficialStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.util.XPathParts;

public class TestCoverageLevel extends TestFmwkPlus {

    private static final boolean SHOW_LSR_DATA = false;

    private static CLDRConfig testInfo = CLDRConfig.getInstance();
    private static final StandardCodes STANDARD_CODES = StandardCodes.make();
    private static final CLDRFile ENGLISH = testInfo.getEnglish();
    private static final SupplementalDataInfo SDI = testInfo.getSupplementalDataInfo();
    private static final String TC_VOTES =
            Integer.toString(VoteResolver.Level.tc.getVotes(Organization.apple));

    public static void main(String[] args) {
        new TestCoverageLevel().run(args);
    }

    public void testSpecificPaths() {
        String[][] rows = {
            {
                "//ldml/characters/parseLenients[@scope=\"number\"][@level=\"lenient\"]/parseLenient[@sample=\",\"]",
                "moderate",
                TC_VOTES
            }
        };
        doSpecificPathTest("fr", rows);
    }

    public void testSpecificPathsPersCal() {
        String[][] rows = {
            {
                "//ldml/dates/calendars/calendar[@type=\"persian\"]/eras/eraAbbr/era[@type=\"0\"]",
                "moderate",
                "4"
            },
            {
                "//ldml/dates/calendars/calendar[@type=\"persian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"1\"]",
                "moderate",
                "4"
            }
        };
        doSpecificPathTest("ckb_IR", rows);
    }

    public void testSpecificPathsDeFormatLength() {
        String[][] rows = {
            /* For German (de) these should be high-bar (20) per https://unicode-org.atlassian.net/browse/CLDR-14988 */
            {
                "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"short\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"1000\"][@count=\"one\"]",
                "modern",
                TC_VOTES
            },
            {
                "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"short\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"1000\"][@count=\"other\"]",
                "modern",
                TC_VOTES
            },
            {
                "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"short\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"10000\"][@count=\"one\"]",
                "modern",
                TC_VOTES
            },
            {
                "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"short\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"10000\"][@count=\"other\"]",
                "modern",
                TC_VOTES
            },
            {
                "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"short\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"100000\"][@count=\"one\"]",
                "modern",
                TC_VOTES
            },
            {
                "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"short\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"100000\"][@count=\"other\"]",
                "modern",
                TC_VOTES
            },
            {
                "//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]/currencyFormatLength[@type=\"short\"]/currencyFormat[@type=\"standard\"]/pattern[@type=\"1000\"][@count=\"one\"]",
                "modern",
                TC_VOTES
            },
            {
                "//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]/currencyFormatLength[@type=\"short\"]/currencyFormat[@type=\"standard\"]/pattern[@type=\"1000\"][@count=\"other\"]",
                "modern",
                TC_VOTES
            },
            {
                "//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]/currencyFormatLength[@type=\"short\"]/currencyFormat[@type=\"standard\"]/pattern[@type=\"10000\"][@count=\"one\"]",
                "modern",
                TC_VOTES
            },
            {
                "//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]/currencyFormatLength[@type=\"short\"]/currencyFormat[@type=\"standard\"]/pattern[@type=\"10000\"][@count=\"other\"]",
                "modern",
                TC_VOTES
            },
            {
                "//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]/currencyFormatLength[@type=\"short\"]/currencyFormat[@type=\"standard\"]/pattern[@type=\"100000\"][@count=\"one\"]",
                "modern",
                TC_VOTES
            },
            {
                "//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]/currencyFormatLength[@type=\"short\"]/currencyFormat[@type=\"standard\"]/pattern[@type=\"100000\"][@count=\"other\"]",
                "modern",
                TC_VOTES
            },
            /* not high-bar (20): wrong number of zeroes, or count many*/
            {
                "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"short\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"100\"][@count=\"other\"]",
                "comprehensive",
                "8"
            },
            {
                "//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]/currencyFormatLength[@type=\"short\"]/currencyFormat[@type=\"standard\"]/pattern[@type=\"1000000\"][@count=\"other\"]",
                "modern",
                "8"
            },
            {
                "//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]/currencyFormatLength[@type=\"short\"]/currencyFormat[@type=\"standard\"]/pattern[@type=\"1000\"][@count=\"many\"]",
                "modern",
                "8"
            },
        };
        doSpecificPathTest("de", rows);
    }

    private void doSpecificPathTest(String localeStr, String[][] rows) {
        Factory phf = PathHeader.getFactory(ENGLISH);
        CoverageLevel2 coverageLevel = CoverageLevel2.getInstance(SDI, localeStr);
        CLDRLocale loc = CLDRLocale.getInstance(localeStr);
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
        SupplementalDataInfo sdi =
                SupplementalDataInfo.getInstance(CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);

        Set<String> allPaths = new HashSet<>();
        M4<String, String, Level, Boolean> starredToLocalesToLevels =
                ChainedMap.of(
                        new TreeMap<String, Object>(),
                        new TreeMap<String, Object>(),
                        new TreeMap<Level, Object>(),
                        Boolean.class);

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

            for (Entry<String, Map<Level, Boolean>> entry2 : localesToLevels.entrySet()) {
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
            System.out.println(
                    maxLevelCount
                            + "\t"
                            + localesWithUniqueLevels.size() / localeCount
                            + "\t"
                            + starred
                            + "\t"
                            + Joiner.on(", ").join(levelsFound)
                            + "\t"
                            + (maxLevelCount == 1
                                    ? "all"
                                    : localesWithUniqueLevels.size() == 0
                                            ? "none"
                                            : Joiner.on(", ").join(localesWithUniqueLevels)));
        }
    }

    enum LanguageStatus {
        Lit100M("P1"),
        Lit10MandOfficial("P2"),
        Lit1MandOneThird("P3");
        final String name;

        LanguageStatus(String name) {
            this.name = name;
        }
    }

    static Relation<String, LanguageStatus> languageStatus =
            Relation.of(new HashMap<String, Set<LanguageStatus>>(), TreeSet.class);
    static Counter2<String> languageLiteratePopulation = new Counter2<>();
    static Map<String, Date> currencyToLast = new HashMap<>();
    static Set<String> officialSomewhere = new HashSet<>();

    static {
        Counter2<String> territoryLiteratePopulation = new Counter2<>();
        LanguageTagParser parser = new LanguageTagParser();
        // cf
        // http://cldr.unicode.org/development/development-process/design-proposals/languages-to-show-for-translation
        for (String language : SDI.getLanguagesForTerritoriesPopulationData()) {
            String base = parser.set(language).getLanguage();
            boolean isOfficial = false;
            double languageLiterate = 0;
            for (String territory : SDI.getTerritoriesForPopulationData(language)) {
                PopulationData pop = SDI.getLanguageAndTerritoryPopulationData(language, territory);
                OfficialStatus officialStatus = pop.getOfficialStatus();
                if (officialStatus.compareTo(OfficialStatus.de_facto_official) >= 0) {
                    isOfficial = true;
                    languageStatus.put(base + "_" + territory, LanguageStatus.Lit10MandOfficial);
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
        for (String language : SDI.getLanguagesForTerritoriesPopulationData()) {
            if (languageLiteratePopulation.getCount(language) < 1000000) {
                continue;
            }
            String base = parser.set(language).getLanguage();
            for (String territory : SDI.getTerritoriesForPopulationData(language)) {
                PopulationData pop = SDI.getLanguageAndTerritoryPopulationData(language, territory);
                double litPop = pop.getLiteratePopulation();
                double total = territoryLiteratePopulation.getCount(territory);
                if (litPop > total / 3) {
                    languageStatus.put(base, LanguageStatus.Lit1MandOneThird);
                }
            }
        }
        for (String territory : STANDARD_CODES.getAvailableCodes("territory")) {
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

    static CompactDecimalFormat cdf =
            CompactDecimalFormat.getInstance(ULocale.ENGLISH, CompactStyle.SHORT);

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

    private static final boolean DEBUG = false;

    static class TypeName implements Transform<String, String> {
        private final int field;
        private final Map<String, R2<List<String>, String>> dep;

        public TypeName(int field) {
            this.field = field;
            switch (field) {
                case CLDRFile.LANGUAGE_NAME:
                    dep = SDI.getLocaleAliasInfo().get("language");
                    break;
                case CLDRFile.TERRITORY_NAME:
                    dep = SDI.getLocaleAliasInfo().get("territory");
                    break;
                case CLDRFile.SCRIPT_NAME:
                    dep = SDI.getLocaleAliasInfo().get("script");
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
                extra = last == null ? "?" : last.compareTo(NOW) < 0 ? "old" : "";
            }
            R2<List<String>, String> depValue = dep == null ? null : dep.get(source);
            if (depValue != null) {
                extra += extra.isEmpty() ? "" : "-";
                extra += depValue.get1();
            }
            return result + (extra.isEmpty() ? "" : "\t" + extra);
        }
    }

    RegexLookup<Level> exceptions =
            RegexLookup.of(
                            null,
                            new Transform<String, Level>() {
                                @Override
                                public Level transform(String source) {
                                    return Level.fromLevel(Integer.parseInt(source));
                                }
                            },
                            null)
                    .loadFromFile(TestCoverageLevel.class, "TestCoverageLevel.txt");

    public void TestExceptions() {
        for (Map.Entry<Finder, Level> x : exceptions) {
            logln(x.getKey().toString() + " => " + x.getValue());
        }
    }

    public void TestNarrowCurrencies() {
        String path = "//ldml/numbers/currencies/currency[@type=\"USD\"]/symbol[@alt=\"narrow\"]";
        String value = ENGLISH.getStringValue(path);
        assertEquals("Narrow $", "$", value);
        SupplementalDataInfo sdi =
                SupplementalDataInfo.getInstance(CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);
        Level level = sdi.getCoverageLevel(path, "en");
        assertEquals("Narrow $", Level.MODERATE, level);
    }

    public void TestA() {
        String path = "//ldml/characterLabels/characterLabel[@type=\"other\"]";
        SupplementalDataInfo sdi =
                SupplementalDataInfo.getInstance(CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);
        Level level = sdi.getCoverageLevel(path, "en");
        assertEquals("Quick Check for any attribute", Level.MODERN, level);
    }

    public void TestCoverageCompleteness() {
        /**
         * Check that English paths are, except for known cases, at least modern coverage. We filter
         * out the things we know about and have determined are OK to be in comprehensive. If we add
         * a path that doesn't get its coverage set, this test should complain about it.
         */
        final ImmutableSet<String> inactiveMetazones =
                ImmutableSet.of(
                        "Greenland", // TODO: New metazone added for tz2023d update,
                        // In CLDR 45, we don't want to include this one in modern coverage because
                        // we don't open ST for translating display names for this metazone.
                        // After 45, we will include "Greenland" in modern coverage.
                        "Bering",
                        "Dominican",
                        "Shevchenko",
                        "Alaska_Hawaii",
                        "Yerevan",
                        "Africa_FarWestern",
                        "British",
                        "Sverdlovsk",
                        "Karachi",
                        "Malaya",
                        "Oral",
                        "Frunze",
                        "Dutch_Guiana",
                        "Irish",
                        "Uralsk",
                        "Tashkent",
                        "Kwajalein",
                        "Ashkhabad",
                        "Kizilorda",
                        "Kuybyshev",
                        "Baku",
                        "Dushanbe",
                        "Goose_Bay",
                        "Liberia",
                        "Samarkand",
                        "Tbilisi",
                        "Borneo",
                        "Greenland_Central",
                        "Dacca",
                        "Aktyubinsk",
                        "Turkey",
                        "Urumqi",
                        "Acre",
                        "Almaty",
                        "Anadyr",
                        "Aqtau",
                        "Aqtobe",
                        "Kamchatka",
                        "Macau",
                        "Qyzylorda",
                        "Samara",
                        "Casey",
                        "Guam",
                        "Lanka",
                        "North_Mariana");

        final Pattern calendar100 =
                PatternCache.get("(coptic|ethiopic-amete-alem|islamic-(rgsa|tbla|umalqura))");

        // Warning: shorter strings must come AFTER longer ones. Can process with MinimizeRegex to
        // reorder
        final Pattern language100 =
                PatternCache.get(
                        "(" // start
                                + "nds_NL|fa_AF|ro_MD|sr_ME|sw_CD"
                                // Length 4
                                + "|root"
                                // Length 3
                                + "|ace|ach|ada|ady|aeb|afh|agq|ain|akk|akz|ale|aln|alt|ang|ann|anp|apc|arc|arn|aro|arp|arq|ars|arw|ary|arz|asa|ase|atj|avk|awa"
                                + "|bal|ban|bar|bax|bbc|bbj|bej|bem|bew|bez|bfd|bfq|bgc|bgn|bho|bik|bin|bjn|bkm|bla|blo|blt|bpy|bqi|bra|brh|bss|bua|bug|bum|byn|byv"
                                + "|cad|car|cay|cch|ccp|cgg|chb|chg|chk|chm|chn|cho|chp|chy|cic|ckb|clc|cop|cps|crg|crh|crj|crk|crl|crm|crr|crs|csb|csw|cwd"
                                + "|dak|dar|dav|del|den|dgr|din|dje|doi|dtp|dua|dum|dyo|dyu|dzg"
                                + "|ebu|efi|egl|egy|eka|elx|enm|esu|ext|fan|fat|fit|fon|frc|frm|fro|frp|frr|frs|fur"
                                + "|gaa|gag|gan|gay|gba|gbz|gez|gil|glk|gmh|goh|gom|gon|gor|got|grb|grc|gsw|guc|gur|guz|gwi"
                                + "|hai|hak|haw|hax|hdn|hif|hil|hit|hnj|hsn|hup|hur|iba|ilo|inh|izh|jam|jbo|jgo|jmc|jpr|jrb|jut"
                                + "|kaa|kab|kac|kaj|kam|kaw|kbd|kbl|kcg|kde|ken|kfo|kgp|kha|kho|khq|khw|kiu|kln|kmb|koi|kos|kpe|krc|kri|krj|krl|kru|ksb|ksf|ksh|kum|kut|kwk|kxv"
                                + "|lad|lag|lah|lam|lez|lfn|lij|lil|liv|lkt|lmo|lol|lou|loz|lrc|ltg|lua|lui|lun|luo|lus|luy|lzh|lzz"
                                + "|mad|maf|mag|mai|mak|man|mas|mde|mdf|mdr|men|mer|mfe|mga|mgh|mgo|mic|min|mnc|mni|moe|moh|mos|mrj|mua|mus|mwl|mwr|mwv|mye|myv|mzn"
                                + "|nan|nap|naq|nds|new|nia|niu|njo|nmg|nog|non|nov|nqo|nso|nus|nwc|nym|nyn|nyo|nzi|oka|osa|ota"
                                + "|pag|pal|pam|pap|pau|pcd|pcm|pdc|pdt|peo|pfl|phn|pms|pnt|pon|pqm|prg|pro|quc|qug|raj|rap|rar|rgn|rif|rof|rom|rtm|rue|rug|rup|rwk"
                                + "|sad|sam|saq|sas|sat|saz|sba|sbp|sdc|sdh|see|seh|sei|sel|ses|sga|sgs|shi|shn|shu|sid|skr|slh|sli|sly|sma|smj|smn|sms|snk|sog|srn|srr|stq|str|suk|sus|sux|swb|syc|syr|szl"
                                + "|tce|tcy|tem|teo|ter|tet|tgx|tht|tig|tiv|tkl|tkr|tlh|tli|tly|tmh|tog|tok|tpi|tru|trv|trw|tsd|tsi|ttm|ttt|tum|tvl|tzm"
                                + "|udm|uga|umb|vai|vec|vep|vls|vmf|vmw|vot|vro|vun|wae|wal|war|was|wbp|wuu|xal|xmf|xnr|xog|yao|yap|yrl|zap|zbl|zea|zen|zgh|zun|zza"
                                + "|ike|ojg|ssy|pis|twq"
                                // Length 2
                                + "|aa|ab|ae|ak|an|av|ay|ba|bi|bm|bo|ce|ch|cr|cu|cv|dv|dz|ee|eo|fj|gn|gv|ho|hz|ie|ii|ik|io|iu|kg|ki|kj|kl|kv|kw|lg|li|ln|lu"
                                + "|mg|mh|na|nb|nd|ng|no|nr|nv|oc|oj|om|os|pi|rn|rw|sc|se|sg|sh|sn|ss|tl|tn|ts|tw|ty|ve|vo|wa|yi|za"
                                // end
                                + ")");

        /**
         * Recommended scripts that are allowed for comprehensive coverage. Not-recommended scripts
         * (according to ScriptMetadata) are filtered out automatically.
         */
        final Pattern script100 = PatternCache.get("(Zinh)");

        final Pattern keys100 =
                PatternCache.get(
                        "(col(Alternate|Backwards|CaseFirst|CaseLevel|HiraganaQuaternary|"
                                + "Normalization|Numeric|Reorder|Strength)|kv|sd|mu|timezone|va|variableTop|x|d0|h0|i0|k0|m0|s0)");

        final Pattern numberingSystem100 =
                PatternCache.get(
                        "("
                                + "finance|native|traditional|adlm|ahom|bali|bhks|brah|cakm|cham|cyrl|diak|"
                                + "gong|gonm|hanidays|hmng|hmnp|java|jpanyear|kali|kawi|lana(tham)?|lepc|limb|"
                                + "math(bold|dbl|mono|san[bs])|modi|mong|mroo|mtei|mymr(shan|tlng)|"
                                + "nagm|newa|nkoo|olck|osma|rohg|saur|segment|shrd|sin[dh]|sora|sund|"
                                + "takr|talu|tirh|tnsa|vaii|wara|wcho)");

        final Pattern collation100 =
                PatternCache.get(
                        "("
                                + "big5han|compat|dictionary|emoji|eor|gb2312han|phonebook|phonetic|pinyin|reformed|searchjl|stroke|traditional|unihan|zhuyin)");

        SupplementalDataInfo sdi = testInfo.getSupplementalDataInfo();
        CLDRFile english = testInfo.getEnglish();

        // Calculate date of the upcoming CLDR release, minus 5 years (deprecation policy)
        final int versionNumber = Integer.valueOf((CLDRFile.GEN_VERSION).split("\\.")[0]);
        Calendar cal = Calendar.getInstance();
        cal.set(versionNumber / 2 + versionNumber % 2 + 2001, 8 - (versionNumber % 2) * 6, 15);
        Date cldrReleaseMinus5Years = cal.getTime();
        Set<String> modernCurrencies =
                SDI.getCurrentCurrencies(SDI.getCurrencyTerritories(), cldrReleaseMinus5Years, NOW);

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
            if (path.endsWith("/alias")
                    || path.matches(
                            "//ldml/(identity|contextTransforms|layout|localeDisplayNames/transformNames)/.*")) {
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
                if (xpp.containsElement("currencySpacing") || xpp.containsElement("list")) {
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
                if ((path.endsWith("daylight") || path.endsWith("generic"))
                        && !LogicalGrouping.metazonesDSTSet.contains(mzName)) {
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
                if (scriptType.startsWith("Q")
                        || "short".equals(xpp.findAttributeValue("script", "alt"))) {
                    continue;
                }
                ScriptMetadata.Info scriptInfo = ScriptMetadata.getInfo(scriptType);
                if (scriptInfo == null
                        || scriptInfo.idUsage != ScriptMetadata.IdUsage.RECOMMENDED) {
                    continue;
                }
                if (script100.matcher(scriptType).matches()) {
                    continue;
                }
            } else if (xpp.containsElement("territory")) {
                String territoryType = xpp.findAttributeValue("territory", "type");
                if (territoryType.equals("CQ")) { // Exceptionally reserved by ISO-3166
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
                // The alt="ascii" time patterns are hopefully short-lived. We do not survey
                // for them, they can be generated mechanically from the non-alt patterns.
                // CLDR-16606
                if (path.contains("[@alt=\"ascii\"]")) {
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
                        String intervalFormatID =
                                xpp.findAttributeValue("intervalFormatItem", "id");
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
                        // ldml/dates/calendars/calendar[@type='gregorian']/dateTimeFormats/availableFormats/dateFormatItem[@id='%dateFormatItems']
                        assertEquals(path, Level.BASIC, lvl);
                        continue;
                    }
                }
            } else if (path.startsWith("//ldml/units")) {
                // Skip paths for narrow unit fields.
                if ("narrow".equals(xpp.findAttributeValue("unitLength", "type"))
                        || path.endsWith("/compoundUnitPattern1")) {
                    continue;
                }
            } else if (xpp.contains("posix")) {
                continue;
            }

            errln("Comprehensive & no exception for path =>\t" + path);
        }
    }

    public static class TargetsAndSublocales {
        public final CoverageVariableInfo cvi;
        public Set<String> scripts;
        public Set<String> regions;

        public TargetsAndSublocales(String localeLanguage) {
            cvi = SDI.getCoverageVariableInfo(localeLanguage);
            scripts = new TreeSet<>();
            regions = new TreeSet<>();
        }

        public boolean addScript(String localeScript) {
            return scripts.add(localeScript);
        }

        public boolean addRegion(String localeRegion) {
            return regions.add(localeRegion);
        }
    }

    public void TestCoverageVariableInfo() {
        /**
         * Compare the targetScripts and targetTerritories for a language to what we actually have
         * in locales
         */
        Map<String, TargetsAndSublocales> langToTargetsAndSublocales = new TreeMap<>();
        org.unicode.cldr.util.Factory factory = testInfo.getCldrFactory();
        for (CLDRLocale locale : factory.getAvailableCLDRLocales()) {
            String language = locale.getLanguage();
            if (language.length() == 0 || language.equals("root")) {
                continue;
            }
            TargetsAndSublocales targetsAndSublocales = langToTargetsAndSublocales.get(language);
            if (targetsAndSublocales == null) {
                targetsAndSublocales = new TargetsAndSublocales(language);
                langToTargetsAndSublocales.put(language, targetsAndSublocales);
            }
            String script = locale.getScript();
            if (script.length() > 0) {
                targetsAndSublocales.addScript(script);
            }
            String region = locale.getCountry();
            if (region.length() > 0
                    && region.length() < 3) { // do not want numeric codes like 001, 419
                targetsAndSublocales.addRegion(region);
            }
        }

        for (String language : langToTargetsAndSublocales.keySet()) {
            TargetsAndSublocales targetsAndSublocales = langToTargetsAndSublocales.get(language);
            if (targetsAndSublocales == null) {
                continue;
            }
            Set<String> targetScripts = new TreeSet<>(targetsAndSublocales.cvi.targetScripts);
            Set<String> localeScripts = targetsAndSublocales.scripts;
            localeScripts.removeAll(targetScripts);
            if (localeScripts.size() > 0) {
                errln(
                        "Missing scripts for language: "
                                + language
                                + ", target scripts: "
                                + targetScripts
                                + ", but locales also have: "
                                + localeScripts);
            }
            Set<String> targetRegions = new TreeSet<>(targetsAndSublocales.cvi.targetTerritories);
            Set<String> localeRegions = targetsAndSublocales.regions;
            localeRegions.removeAll(targetRegions);
            if (localeRegions.size() > 0) {
                errln(
                        "Missing regions for language: "
                                + language
                                + ", target regions: "
                                + targetRegions
                                + ", but locales also have: "
                                + localeRegions);
            }
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
                errln(
                        localeId
                                + " ("
                                + count
                                + ") Broken Logical Grouping: "
                                + entry.getKey()
                                + " => "
                                + entry.getValue());
            }
            ++count;
        }
    }

    public void testLogicalGroupingSamples() {
        getLogger().fine(GrammarInfo.getGrammarLocales().toString());
        String[][] test = {
            {
                "de", "SINGLETON", "//ldml/localeDisplayNames/localeDisplayPattern/localePattern",
            },
            {
                "de",
                "METAZONE",
                "//ldml/dates/timeZoneNames/metazone[@type=\"Alaska\"]/long/generic",
                "//ldml/dates/timeZoneNames/metazone[@type=\"Alaska\"]/long/standard",
                "//ldml/dates/timeZoneNames/metazone[@type=\"Alaska\"]/long/daylight",
            },
            {
                "de",
                "DAYS",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"sun\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"mon\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"tue\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"wed\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"thu\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"fri\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/days/dayContext[@type=\"format\"]/dayWidth[@type=\"wide\"]/day[@type=\"sat\"]",
            },
            {
                "nl",
                "DAY_PERIODS",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"wide\"]/dayPeriod[@type=\"morning1\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"wide\"]/dayPeriod[@type=\"afternoon1\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"wide\"]/dayPeriod[@type=\"evening1\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"wide\"]/dayPeriod[@type=\"night1\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"wide\"]/dayPeriod[@type=\"midnight\"]",
            },
            {
                "de",
                "QUARTERS",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/quarters/quarterContext[@type=\"format\"]/quarterWidth[@type=\"wide\"]/quarter[@type=\"1\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/quarters/quarterContext[@type=\"format\"]/quarterWidth[@type=\"wide\"]/quarter[@type=\"2\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/quarters/quarterContext[@type=\"format\"]/quarterWidth[@type=\"wide\"]/quarter[@type=\"3\"]",
                "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/quarters/quarterContext[@type=\"format\"]/quarterWidth[@type=\"wide\"]/quarter[@type=\"4\"]",
            },
            {
                "de",
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
            {
                "de",
                "RELATIVE",
                "//ldml/dates/fields/field[@type=\"week-short\"]/relative[@type=\"-1\"]",
                "//ldml/dates/fields/field[@type=\"week-short\"]/relative[@type=\"0\"]",
                "//ldml/dates/fields/field[@type=\"week-short\"]/relative[@type=\"1\"]",
            },
            {
                "de",
                "DECIMAL_FORMAT_LENGTH",
                "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"long\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"1000\"][@count=\"one\"]",
                "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"long\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"1000\"][@count=\"other\"]",
                "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"long\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"10000\"][@count=\"one\"]",
                "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"long\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"10000\"][@count=\"other\"]",
                "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"long\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"100000\"][@count=\"one\"]",
                "//ldml/numbers/decimalFormats[@numberSystem=\"latn\"]/decimalFormatLength[@type=\"long\"]/decimalFormat[@type=\"standard\"]/pattern[@type=\"100000\"][@count=\"other\"]",
            },
            {
                "cs",
                "COUNT",
                "//ldml/numbers/currencies/currency[@type=\"BMD\"]/displayName[@count=\"one\"]",
                "//ldml/numbers/currencies/currency[@type=\"BMD\"]/displayName[@count=\"few\"]",
                "//ldml/numbers/currencies/currency[@type=\"BMD\"]/displayName[@count=\"many\"]",
                "//ldml/numbers/currencies/currency[@type=\"BMD\"]/displayName[@count=\"other\"]",
            },
            {
                "de",
                "COUNT",
                "//ldml/numbers/minimalPairs/pluralMinimalPairs[@count=\"one\"]",
                "//ldml/numbers/minimalPairs/pluralMinimalPairs[@count=\"other\"]",
            },
            {
                "de",
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
            {
                "hi",
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
                    assertEquals(
                            "Logical group for " + locale + ", " + path,
                            ImmutableListMultimap.of(),
                            deltaValue);
                }
                PathType actualPathType = PathType.getPathTypeFromPath(path);
                assertEquals("PathType", expectedPathType, actualPathType);
            }
            seenPt.remove(expectedPathType);
        }
        assertEquals("PathTypes tested", Collections.emptySet(), seenPt);
    }

    private Multimap<String, String> delta(Set<String> expected, Set<String> grouping) {
        if (expected.equals(grouping)) {
            return ImmutableListMultimap.of();
        }
        Multimap<String, String> result = LinkedHashMultimap.create();
        TreeSet<String> aMinusB = new TreeSet<>(expected);
        aMinusB.removeAll(grouping);
        result.putAll("expected-actual", aMinusB);
        TreeSet<String> bMinusA = new TreeSet<>(grouping);
        bMinusA.removeAll(expected);
        result.putAll("actual-expected", bMinusA);
        return result;
    }

    static class CoverageStatus {

        private Level level;
        private boolean inRoot;
        private boolean inId;
        private Level languageLevel;
        private String displayName;

        public CoverageStatus(
                Level level,
                boolean inRoot,
                boolean inId,
                Level languageLevel,
                String displayName) {
            this.level = level;
            this.inRoot = inRoot;
            this.inId = inId;
            this.languageLevel = languageLevel == null ? Level.UNDETERMINED : languageLevel;
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return (inRoot ? "root" : "x")
                    + "\t"
                    + (inId ? "ids" : "x")
                    + "\t"
                    + stringForm(languageLevel)
                    + "\t"
                    + stringForm(level)
                    + "\t"
                    + displayName;
        }

        private String stringForm(Level level2) {
            if (level == null) {
                return "υnd";
            }
            switch (level2) {
                case UNDETERMINED:
                    return "υnd";
                case COMPREHENSIVE:
                    return "ϲomp";
                default:
                    return level2.toString();
            }
        }
    }

    public void testLSR() {
        SupplementalDataInfo supplementalData = testInfo.getSupplementalDataInfo();
        org.unicode.cldr.util.Factory factory = testInfo.getCldrFactory();
        CLDRFile root = factory.make(LocaleNames.ROOT, true);
        CoverageLevel2 coverageLevel =
                CoverageLevel2.getInstance(supplementalData, "qtz"); // non-existent locale

        Set<String> langsRoot = new TreeSet<>();
        Set<String> scriptsRoot = new TreeSet<>();
        Set<String> regionsRoot = new TreeSet<>();

        // Get root LSR codes

        for (String path : root) {
            if (!path.startsWith("//ldml/localeDisplayNames/")) {
                continue;
            }
            XPathParts parts = XPathParts.getFrozenInstance(path);
            String code = parts.getAttributeValue(3, "type");
            if (code == null || code.contains("_")) {
                continue;
            }
            switch (parts.getElement(3)) {
                case "language":
                    langsRoot.add(code);
                    break;
                case "script":
                    scriptsRoot.add(code);
                    break;
                case "territory":
                    regionsRoot.add(code);
                    break;
            }
        }
        langsRoot = ImmutableSet.copyOf(langsRoot);
        scriptsRoot = ImmutableSet.copyOf(scriptsRoot);
        regionsRoot = ImmutableSet.copyOf(regionsRoot);

        // get CLDR locale IDs' codes

        Map<String, Level> langs = new TreeMap<>();
        Map<String, Level> scripts = new TreeMap<>();
        Map<String, Level> regions = new TreeMap<>();
        LikelySubtags likely = new LikelySubtags();

        LanguageTagParser ltp = new LanguageTagParser();
        for (String locale : factory.getAvailable()) {
            Level languageLevel = STANDARD_CODES.getLocaleCoverageLevel(Organization.cldr, locale);
            if (languageLevel == null || languageLevel == Level.UNDETERMINED) {
                languageLevel = Level.CORE;
            }
            ltp.set(locale);
            likely.maximize(ltp);
            addBestLevel(langs, ltp.getLanguage(), languageLevel);
            addBestLevel(scripts, ltp.getScript(), languageLevel);
            addBestLevel(regions, ltp.getRegion(), languageLevel);
        }
        regions.remove("");
        scripts.remove("");

        // get the data

        Map<String, CoverageStatus> data = new TreeMap<>();

        ImmutableMap<Integer, R4<String, Map<String, Level>, Set<String>, Level>> typeToInfo =
                ImmutableMap.of(
                        CLDRFile.LANGUAGE_NAME,
                        Row.of("language", langs, langsRoot, Level.MODERN),
                        CLDRFile.SCRIPT_NAME,
                        Row.of("script", scripts, scriptsRoot, Level.MODERATE),
                        CLDRFile.TERRITORY_NAME,
                        Row.of("region", regions, regionsRoot, Level.MODERATE));

        for (Entry<Integer, R4<String, Map<String, Level>, Set<String>, Level>> typeAndInfo :
                typeToInfo.entrySet()) {
            int type = typeAndInfo.getKey();
            String name = typeAndInfo.getValue().get0();
            Map<String, Level> idPartMap = typeAndInfo.getValue().get1();
            Set<String> setRoot = typeAndInfo.getValue().get2();
            Level targetLevel = typeAndInfo.getValue().get3();
            for (String code : Sets.union(idPartMap.keySet(), setRoot)) {
                String displayName = testInfo.getEnglish().getName(type, code);
                String path = CLDRFile.getKey(type, code);
                Level level = coverageLevel.getLevel(path);
                data.put(
                        name + "\t" + code,
                        new CoverageStatus(
                                level,
                                setRoot.contains(code),
                                idPartMap.containsKey(code),
                                idPartMap.get(code),
                                displayName));
            }
        }
        if (SHOW_LSR_DATA) {

            System.out.println(
                    "\nType\tCode\tIn Root\tIn CLDR Locales\tCLDR TargeLevel\tRoot Path Level\tCombinations");
            for (Entry<String, CoverageStatus> entry : data.entrySet()) {
                System.out.println(entry.getKey() + "\t" + entry.getValue());
            }
            System.out.println();
            for (Entry<String, CoverageStatus> entry : data.entrySet()) {
                final String key = entry.getKey();
                if (!key.startsWith("language")) {
                    continue;
                }
                final CoverageStatus value = entry.getValue();
                if (value.inId) {
                    continue;
                }
                String[] parts = key.split("\t");
                PopulationData population = SDI.getBaseLanguagePopulationData(parts[1]);
                if (population == null) {
                    System.out.println(key + "\t" + value.displayName + "\t" + value + "\t-1\t-1");
                } else {
                    System.out.println(
                            key
                                    + "\t"
                                    + value.displayName
                                    + "\t"
                                    + value
                                    + "\t"
                                    + population.getPopulation()
                                    + "\t"
                                    + population.getLiteratePopulation());
                }
            }
        }

        Set<String> ids = new TreeSet<>();
        Set<String> missing = new TreeSet<>();
        for (Entry<String, CoverageStatus> entry : data.entrySet()) {
            final String key = entry.getKey();
            if (!key.startsWith("language")) {
                continue;
            }
            final CoverageStatus value = entry.getValue();
            if (value.inId) {
                String[] parts = key.split("\t");
                ids.add(parts[1]);
                if (!value.inRoot) {
                    missing.add(parts[1]);
                }
            }
        }
        if (!assertEquals(
                "Language subtags that are in a CLDR locale's ID are in root ("
                        + missing.size()
                        + ")",
                "",
                Joiner.on(' ').join(missing))) {
            warnln(
                    "Full set for resetting $language in attributeValueValidity.xml ("
                            + ids.size()
                            + "):"
                            + breakLines(ids, "\n                "));
        }
    }

    private String breakLines(Set<String> ids, String indent) {
        StringBuilder result = new StringBuilder();
        int lastFirstChar = 0;
        for (String id : ids) {
            int firstChar = id.codePointAt(0);
            result.append(firstChar == lastFirstChar ? " " : indent);
            result.append(id);
            lastFirstChar = firstChar;
        }
        return result.toString();
    }

    private void addBestLevel(Map<String, Level> codeToBestLevel, String code, Level level) {
        if (level != Level.UNDETERMINED) {
            int debug = 0;
        }
        Level old = codeToBestLevel.get(code);
        if (old == null) {
            codeToBestLevel.put(code, level);
        } else if (level.compareTo(old) > 0) {
            codeToBestLevel.put(code, level);
        } else if (level != old) {
            int debug = 0;
        }
    }

    public void TestEnglishCoverage() {
        Output<String> pathWhereFound = new Output<>();
        Output<String> localeWhereFound = new Output<>();
        Set<Row.R5<String, String, Boolean, Boolean, Level>> inherited = new TreeSet<>();
        for (String path : ENGLISH) {
            String value = ENGLISH.getStringValueWithBailey(path, pathWhereFound, localeWhereFound);
            final boolean samePath = path.equals(pathWhereFound.value);
            final boolean sameLocale = "en".equals(localeWhereFound.value);
            if (!samePath) {
                Level level = SDI.getCoverageLevel(path, "en");
                if (level.compareTo(Level.MODERN) <= 0) {
                    inherited.add(Row.of(path, value, samePath, sameLocale, level));
                }
            }
        }
        if (!assertEquals("English has sideways inheritance:", 0, inherited.size())) {
            System.out.println("Check the following, then use in modify_config.txt\n");
            String pattern = "locale=en ; action=add ; new_path=%s ; new_value=%s";
            for (Row.R5<String, String, Boolean, Boolean, Level> row : inherited) {
                System.out.println(String.format(pattern, row.get0(), row.get1()));
                if (DEBUG) {
                    System.out.println(
                            String.format(
                                    "%s\t%s\t%s\t%s\t%s",
                                    row.get0(), row.get1(), row.get2(), row.get3(), row.get4()));
                }
            }
        }
    }
}
