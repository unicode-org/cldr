package org.unicode.cldr.unittest;

import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.ChainedMap.M4;
import org.unicode.cldr.util.Counter2;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.LogicalGrouping;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.RegexLookup.Finder;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.CurrencyDateInfo;
import org.unicode.cldr.util.SupplementalDataInfo.OfficialStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;
import org.unicode.cldr.util.XPathParts;

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.CompactDecimalFormat;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.Transform;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.ULocale;

public class TestCoverageLevel extends TestFmwkPlus {

    private static TestInfo testInfo = TestInfo.getInstance();
    private static final StandardCodes STANDARD_CODES = testInfo.getStandardCodes();
    private static final CLDRFile ENGLISH = testInfo.getEnglish();
    private static final SupplementalDataInfo SDI = testInfo.getSupplementalDataInfo();

    public static void main(String[] args) {
        new TestCoverageLevel().run(args);
    }

    public void oldTestInvariantPaths() {
        org.unicode.cldr.util.Factory factory = testInfo.getCldrFactory();
        PathStarrer pathStarrer = new PathStarrer().setSubstitutionPattern("*");
        SupplementalDataInfo sdi = SupplementalDataInfo
            .getInstance(CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);

        Set<String> allPaths = new HashSet<String>();
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
        Set<String> localesWithUniqueLevels = new TreeSet<String>();
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
                + CollectionUtilities.join(levelsFound, ", ")
                + "\t"
                + (maxLevelCount == 1 ? "all" : localesWithUniqueLevels
                    .size() == 0 ? "none" : CollectionUtilities.join(
                    localesWithUniqueLevels, ", ")));
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
    static Counter2<String> languageLiteratePopulation = new Counter2<String>();
    static Map<String, Date> currencyToLast = new HashMap<String, Date>();
    static Set<String> officialSomewhere = new HashSet<String>();

    static {
        Counter2<String> territoryLiteratePopulation = new Counter2<String>();
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

    public void TestCoverageCompleteness() {
        /**
         * Check that English paths are, except for known cases, at least modern coverage.
         * We filter out the things we know about and have determined are OK to be in comprehensive.
         * If we add a path that doesn't get its coverage set, this test should complain about it.
         */
        final ImmutableSet<String> inactiveMetazones = ImmutableSet.of("Bering", "Dominican", "Shevchenko", "Alaska_Hawaii", "Yerevan",
            "Africa_FarWestern", "British", "Sverdlovsk", "Karachi", "Malaya", "Oral", "Frunze", "Dutch_Guiana", "Irish", "Uralsk", "Tashkent", "Kwajalein",
            "Yukon", "Ashkhabad", "Kizilorda", "Kuybyshev", "Baku", "Dushanbe", "Goose_Bay", "Liberia", "Samarkand", "Tbilisi", "Borneo", "Greenland_Central",
            "Dacca", "Aktyubinsk", "Turkey", "Urumqi", "Acre", "Almaty", "Anadyr", "Aqtau", "Aqtobe", "Kamchatka", "Macau", "Qyzylorda", "Samara",
            "Casey", "Guam", "Lanka", "North_Mariana");

        final Pattern calendar100 = PatternCache.get("(coptic|ethiopic-amete-alem|islamic-(rgsa|tbla|umalqura))");

        final Pattern language100 = PatternCache.get("("
            + "aa|ac[eh]|ad[ay]|aeb?|af[ah]|ain|ak[kz]|al[egnt]|an[gp]?|apa|ar[copqtwyz]|as[et]|ath|aus|avk?|awa|ay|"
            + "ba[dilnstx]|bar|bb[cj]|be[jrw]|bf[dq]|bho?|bkm|bi[kn]?|bjn|bla|bnt|bpy|bqi|br[ah]|bss|btk|bu[amg]|bxr|by[nv]|"
            + "ca[diruy]|cch|ce[bl]|ch[bgkmnopy]?|cmc|cop|cp[efps]|cr[hp]?|csb|cus?|"
            + "da[kry]|de[ln]|dgr|din|doi|dra|dtp|dum|dv|dyu|dzg|"
            + "efi|eg[ly]|eka|elx|enm|esu|ewo|ext|"
            + "fa[nt]|ff|fi[tu]|fon|fr[cmoprs]|fur|"
            + "ga[agny]|gb[az]|gd|ge[mz]|gil|glk|gmh|go[hmnrt]|gr[bc]|gu[cr]|gwi|"
            + "ha[ik]|hi[flmt]|hmn|ho|hsn|hup|hz|"
            + "ia|ib[ab]|ie|ijo|ik|ilo|in[ceh]|io|ir[ao]|izh|"
            + "jam|jbo|jpr|jrb|jut|"
            + "ka[acjrw]|kb[dl]|kc[bg]|ken|kfo|kgp?|kh[aiow]|kiu|kkj|kj|kmb|ko[is]|kpe|kr[cijlou]?|ksh|ku[mt]|kv|"
            + "la[dhm]|lez|lfn|li[jv]?|lmo|lo[lz]|ltg|lu[ains]|lz[hz]|"
            + "ma[dfgiknp]|md[efr]|men|mga|mh|mi[cns]|mkh|mn[cio]|mos|mrj|mu[lns]|mw[lrv]|my[env]|mzn|"
            + "na[hinp]?|new|ng|ni[acu]|njo|nnh|no[gnv]?|nr|nso|nub|nv|nwc|ny[mo]?|nzi|"
            + "o[cj]|osa?|ot[ao]|"
            + "pa[aglmpu]|pcd|pd[ct]|peo|pfl|ph[in]|pi|pms|pnt|pon|pr[ago]|"
            + "qug|"
            + "ra[jpr]|rgn|rif|ro[am]|root|rtm|ru[egp]|"
            + "sa[dhilmstz]|sba|sc[no]?|sdc|se[eilm]|sg[ans]|sh[nu]?|si[dot]|sl[aiy]|sm|snk|so[gn]|sr[nr]|ss[ay]?|stq?|su[ksx]|swb|sy[cr]|szl|"
            + "tai|tcy|te[mrt]|ti[gv]|tk[lr]|tl[hiy]?|tmh|tn|tog|tpi|tr[uv]|ts[di]?|ttt|tu[mpt]|tvl|tw|tyv?|"
            + "udm|uga|umb|"
            + "ve[cp]?|vls|vmf||vot?|vro|"
            + "wa[eklrs]?|wen|wuu|"
            + "xal|xmf|"
            + "ya[opv]|ybb|yi|ypk|yrl|yue|"
            + "zap?|zbl|ze[an]|znd|zun|zza)");

        final Pattern script100 = PatternCache.get("("
            + "Afak|Aghb|Ahom|Armi|Avst|Bali|Bamu|Bass|Batk|Blis|Brah|Bugi|Buhd|Cakm|Cans|Cari|Cham|Cher|Cirt|Copt|Cprt|Cyrs|"
            + "Dsrt|Dupl|Egy[dhp]|Elba|Geok|Glag|Goth|Gran|Hatr|Hano|Hluw|Hmng|Hrkt|Hung|Inds|Ital|Java|Jurc|"
            + "Kali|Khar|Khoj|Kpel|Kthi|Kits|Lana|Lat[fg]|Lepc|Limb|Lin[ab]|Lisu|Loma|Ly[cd]i|Mahj|Man[di]|Maya|Mend|Mer[co]|Modi|Moon|Mroo|Mtei|Mult|"
            + "Narb|Nbat|Nkgb|Nkoo|Nshu|Ogam|Olck|Orkh|Osma|Palm|Pauc|Perm|Phag|Phl[ipv]|Phnx|Plrd|Prti|"
            + "Rjng|Roro|Runr|Samr|Sar[ab]|Saur|Sgnw|Shaw|Shrd|Sidd|Sind|Sora|Sund|Sylo|Syr[cejn]|"
            + "Tagb|Takr|Tal[eu]|Tang|Tavt|Teng|Tfng|Tglg|Tirh|Ugar|Vaii|Visp|Wara|Wole|Xpeo|Xsux|Yiii|Zinh|Zmth)");

        final Pattern keys100 = PatternCache.get("(col(Alternate|Backwards|CaseFirst|CaseLevel|HiraganaQuaternary|"
            + "Normalization|Numeric|Reorder|Strength)|kv|timezone|va|variableTop|x)");

        final Pattern numberingSystem100 = PatternCache.get("("
            + "finance|native|traditional|bali|brah|cakm|cham|cyrl|hanidays|java|kali|lana(tham)?|lepc|limb|"
            + "math(bold|dbl|mono|san[bs])|mong|mtei|mymrshan|nkoo|olck|osma|saur|shrd|sora|sund|takr|talu|vaii)");

        final Pattern collation100 = PatternCache.get("("
            + "big5han|compat|dictionary|emoji|eor|gb2312han|phonebook|phonetic|pinyin|reformed|searchjl|stroke|traditional|unihan|zhuyin)");

        SupplementalDataInfo sdi = testInfo.getSupplementalDataInfo();
        CLDRFile english = testInfo.getEnglish();
        XPathParts xpp = new XPathParts();

        // Calculate date of the upcoming CLDR release, minus 5 years (deprecation policy)
        final int versionNumber = Integer.valueOf(CLDRFile.GEN_VERSION);
        Calendar cal = Calendar.getInstance();
        cal.set(versionNumber / 2 + versionNumber % 2 + 2001, 8 - (versionNumber % 2) * 6, 15);
        Date cldrReleaseMinus5Years = cal.getTime();
        Set<String> modernCurrencies = SDI.getCurrentCurrencies(SDI.getCurrencyTerritories(), cldrReleaseMinus5Years, NOW);

        for (String path : english.fullIterable()) {
            logln("Testing path => " + path);
            xpp.set(path);
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
                // Other paths in numbers without a numbering system are deprecated.
                if (numberingSystem == null) {
                    continue;
                }
            }
            else if (xpp.containsElement("zone")) {
                String zoneType = xpp.findAttributeValue("zone", "type");
                if (zoneType.startsWith("Etc/GMT") && path.endsWith("exemplarCity")) {
                    continue;
                }
                // We don't survey for short timezone names
                if (path.contains("/short/")) {
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
                    }
                    if (element.equals("dayPeriod")) {
                        if ("variant".equals(xpp.findAttributeValue("dayPeriod", "alt"))) {
                            continue;
                        }
                    }
                }
            } else if (path.startsWith("//ldml/units")) {
                // Skip paths for narrow unit fields.
                if ("narrow".equals(xpp.findAttributeValue("unitLength", "type"))) {
                    continue;
                }
            }

            errln("Comprehensive & no exception for path => " + path);
        }
    }
}
