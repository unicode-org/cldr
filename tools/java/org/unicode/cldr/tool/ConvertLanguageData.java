package org.unicode.cldr.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.draft.ScriptMetadata;
import org.unicode.cldr.draft.ScriptMetadata.IdUsage;
import org.unicode.cldr.draft.ScriptMetadata.Info;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Iso639Data;
import org.unicode.cldr.util.Iso639Data.Scope;
import org.unicode.cldr.util.Iso639Data.Source;
import org.unicode.cldr.util.Iso639Data.Type;
import org.unicode.cldr.util.LanguageTagCanonicalizer;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.LocaleIDParser.Level;
import org.unicode.cldr.util.Log;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.SpreadSheet;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData;
import org.unicode.cldr.util.SupplementalDataInfo.OfficialStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.XPathParts.Comments;

import com.google.common.collect.ImmutableSet;
import com.google.common.math.DoubleMath;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.util.ULocale;

/**
 * @author markdavis
 *
 */
public class ConvertLanguageData {

    private static final boolean DEBUG = false;
    // change this if you need to override what is generated for the default contents.
    private static final List<String> defaultOverrides = Arrays.asList("es_ES".split("\\s+")); // und_ZZ

    public static final boolean SHOW_DIFF = false;

    private static final boolean ALLOW_SMALL_NUMBERS = true;

    static final Comparator<String> GENERAL_COLLATOR = new GeneralCollator();
    static final Comparator<String> INVERSE_GENERAL = new InverseComparator<String>(GENERAL_COLLATOR);

    private static StandardCodes sc = StandardCodes.make();

    static final double populationFactor = 1;
    static final double gdpFactor = 1;
    static final int BAD_COUNTRY_NAME = 0, COUNTRY_CODE = 1, COUNTRY_POPULATION = 2, COUNTRY_LITERACY = 3,
        COUNTRY_GDP = 4, OFFICIAL_STATUS = 5, BAD_LANGUAGE_NAME = 6, LANGUAGE_CODE = 7, LANGUAGE_POPULATION = 8,
        LANGUAGE_LITERACY = 9, COMMENT = 10, NOTES = 11;
    static final Map<String, CodeAndPopulation> languageToMaxCountry = new TreeMap<String, CodeAndPopulation>();
    static final Map<String, CodeAndPopulation> languageToMaxScript = new TreeMap<String, CodeAndPopulation>();

    private static final double NON_OFFICIAL_WEIGHT = 0.40;

    private static final boolean SHOW_OLD_DEFAULT_CONTENTS = false;

    private static final ImmutableSet<String> scriptAssumedLocales = ImmutableSet.of(
        "bm_ML", "ha_GH", "ha_NE", "ha_NG", "kk_KZ", "ks_IN", "ky_KG", "mn_MN", "ms_BN", "ms_MY", "ms_SG", "tk_TM", "tzm_MA", "ug_CN");

    static Set<String> skipLocales = new HashSet<String>(
        Arrays
            .asList(
                "sh sh_BA sh_CS sh_YU characters supplementalData supplementalData-old supplementalData-old2 supplementalData-old3 supplementalMetadata root"
                    .split("\\s")));

    static Map<String, String> defaultContent = new TreeMap<String, String>();

    static Factory cldrFactory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
    static CLDRFile english = cldrFactory.make("en", true);

    static SupplementalDataInfo supplementalData = SupplementalDataInfo
        .getInstance(CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);

    public static void main(String[] args) throws IOException, ParseException {
        BufferedReader oldFile = null;
        try {
            // load elements we care about
            Log.setLogNoBOM(CLDRPaths.GEN_DIRECTORY + "/supplemental", "supplementalData.xml");
            // Log.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
            // Log.println("<!DOCTYPE supplementalData SYSTEM \"http://www.unicode.org/cldr/data/dtd/ldmlSupplemental.dtd\">");
            // Log.println("<supplementalData version=\"1.5\">");

            oldFile = FileUtilities.openUTF8Reader(CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY, "supplementalData.xml");
            CldrUtility.copyUpTo(oldFile, PatternCache.get("\\s*<languageData>\\s*"), Log.getLog(), false);

            Set<String> available = cldrFactory.getAvailable();

            Set<String> cldrParents = getCldrParents(available);

            List<String> failures = new ArrayList<String>();
            Map<String, RowData> localeToRowData = new TreeMap<String, RowData>();

            Set<RowData> sortedInput = getExcelData(failures, localeToRowData);

            // get the locales (including parents)
            Set<String> localesWithData = new TreeSet<String>(localeToRowData.keySet());
            for (String locale : localeToRowData.keySet()) {
                while (true) {
                    String parent = LocaleIDParser.getParent(locale);
                    if (parent == null) break;
                    localesWithData.add(parent);
                    locale = parent;
                }
            }

            final LanguageTagParser languageTagParser = new LanguageTagParser();

            for (String localeRaw : available) {
                String locale = languageTagCanonicalizer.transform(localeRaw);
                if (!localesWithData.contains(locale)) {
                    CLDRFile locFile = cldrFactory.make(localeRaw, false);
                    if (locFile.isAliasedAtTopLevel()) {
                        continue;
                    }
                    if (scriptAssumedLocales.contains(locale)) {
                        continue;
                    }
                    languageTagParser.set(locale);
                    if (languageTagParser.getVariants().size() != 0) {
                        continue;
                    }
                    String withoutScript = languageTagParser.setScript("").toString();
                    if (!localesWithData.contains(withoutScript)) {
                        String region = new LanguageTagParser().set(locale).getRegion();
                        if (StandardCodes.isCountry(region)) {
                            BadItem.ERROR.show("missing language/population data for CLDR locale", locale + " = " + getLanguageCodeAndName(locale));
                        }
                    } else {
                        // These exceptions are OK, because these locales by default use the non-default script
                        Set<String> OKExceptions = ImmutableSet.of("sr_Cyrl_ME", "zh_Hans_HK", "zh_Hans_MO");
                        if (OKExceptions.contains(locale)) {
                            continue;
                        }
                        BadItem.ERROR.show("missing language/population data for CLDR locale", locale + " = " + getLanguageCodeAndName(locale)
                            + " but have data for " + getLanguageCodeAndName(withoutScript));
                    }
                }
            }

            // TODO sort by country code, then functionalPopulation, then language code
            // and keep the top country for each language code (even if < 1%)

            addLanguageScriptData();

            // showAllBasicLanguageData(allLanguageData, "old");
            getLanguage2Scripts(sortedInput);

            writeNewBasicData2(sortedInput);
            // writeNewBasicData(sortedInput);

            writeTerritoryLanguageData(failures, sortedInput);

            checkBasicData(localeToRowData);

            Set<String> defaultLocaleContent = new TreeSet<String>();

            showDefaults(cldrParents, nf, defaultContent, localeToRowData, defaultLocaleContent);

            // showContent(available);

            // certain items are overridden

            List<String> toRemove = new ArrayList<String>();
            for (String override : defaultOverrides) {
                String replacement = getReplacement(override, defaultLocaleContent);
                if (replacement != null) {
                    toRemove.add(replacement);
                }
            }
            defaultLocaleContent.removeAll(toRemove);
            defaultLocaleContent.addAll(defaultOverrides);

            showFailures(failures);

            CldrUtility.copyUpTo(oldFile, PatternCache.get("\\s*</territoryInfo>\\s*"), null, false);
            CldrUtility.copyUpTo(oldFile, PatternCache.get("\\s*<references>\\s*"), Log.getLog(), false);
            // generateIso639_2Data();
            references.printReferences();
            CldrUtility.copyUpTo(oldFile, PatternCache.get("\\s*</references>\\s*"), null, false);
            CldrUtility.copyUpTo(oldFile, null, Log.getLog(), false);
            // Log.println("</supplementalData>");
            Log.close();
            oldFile.close();

            Log.setLog(CLDRPaths.GEN_DIRECTORY + "/supplemental", "language_script_raw.txt");
            getLanguageScriptSpreadsheet(Log.getLog());
            Log.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (oldFile != null) {
                oldFile.close();
            }
            System.out.println("DONE");
        }
    }

    public static String getLanguageCodeAndName(String code) {
        if (code == null) return null;
        return english.getName(code) + " [" + code + "]";
    }

    private static String getReplacement(String oldDefault, Set<String> defaultLocaleContent) {
        String parent = LocaleIDParser.getParent(oldDefault);
        for (String replacement : defaultLocaleContent) {
            if (replacement.startsWith(parent)) {
                if (parent.equals(LocaleIDParser.getParent(replacement))) {
                    return replacement;
                }
            }
        }
        return null;
    }

    private static void getLanguageScriptSpreadsheet(PrintWriter out) {
        out.println("#Lcode LanguageName  Status  Scode ScriptName  References");
        Pair<String, String> languageScript = new Pair<String, String>("", "");
        for (String language : language_status_scripts.keySet()) {
            Relation<BasicLanguageData.Type, String> status_scripts = language_status_scripts.get(language);
            for (BasicLanguageData.Type status : status_scripts.keySet()) {
                for (String script : status_scripts.getAll(status)) {
                    String reference = language_script_references.get(languageScript.setFirst(language).setSecond(
                        script));
                    out.println(language + "\t" + getLanguageName(language) + "\t" + status + "\t" + script + "\t"
                        + getDisplayScript(script)
                        + (reference == null ? "" : "\t" + reference));
                }
            }
        }
    }

    /**
     * Write data in format:
     * <languageData>
     * <language type="aa" scripts="Latn" territories="DJ ER ET"/>
     *
     * @param sortedInput
     */
    private static void writeNewBasicData2(Set<RowData> sortedInput) {
        double cutoff = 0.2; // 20%

        // Relation<String, BasicLanguageData> newLanguageData = new Relation(new TreeMap(), TreeSet.class);
        LanguageTagParser ltp = new LanguageTagParser();
        Map<String, Relation<BasicLanguageData.Type, String>> language_status_territories = new TreeMap<String, Relation<BasicLanguageData.Type, String>>();
        //Map<String, Pair<String, String>> languageToBestCountry;
        for (RowData rowData : sortedInput) {
            if (rowData.countryCode.equals("ZZ")) continue;
            ltp.set(rowData.languageCode);
            String languageCode = ltp.getLanguage();
            Relation<BasicLanguageData.Type, String> status_territories = language_status_territories.get(languageCode);
            if (status_territories == null) {
                language_status_territories.put(languageCode, status_territories = Relation.of(
                    new TreeMap<BasicLanguageData.Type, Set<String>>(),
                    TreeSet.class));
            }
            if (rowData.officialStatus.isMajor()) {
                status_territories.put(BasicLanguageData.Type.primary, rowData.countryCode);
            } else if (rowData.officialStatus.isOfficial()
                || rowData.getLanguagePopulation() >= cutoff * rowData.countryPopulation
                || rowData.getLanguagePopulation() >= 1000000) {
                status_territories.put(BasicLanguageData.Type.secondary, rowData.countryCode);
            }
        }

        Set<String> allLanguages = new TreeSet<String>(language_status_territories.keySet());
        allLanguages.addAll(language_status_scripts.keySet());
        // now add all the remaining language-script info
        // <language type="sv" scripts="Latn" territories="AX FI SE"/>
        Set<String> warnings = new LinkedHashSet<String>();
        Log.println("\t<languageData>");
        for (String languageSubtag : allLanguages) {
            Relation<BasicLanguageData.Type, String> status_scripts = language_status_scripts.get(languageSubtag);
            Relation<BasicLanguageData.Type, String> status_territories = language_status_territories
                .get(languageSubtag);

            // check against old:
            Map<BasicLanguageData.Type, BasicLanguageData> oldData = supplementalData
                .getBasicLanguageDataMap(languageSubtag);
            if (oldData == null) {
                oldData = Collections.emptyMap();
            }

            EnumMap<BasicLanguageData.Type, BasicLanguageData> newData = new EnumMap<BasicLanguageData.Type, BasicLanguageData>(
                BasicLanguageData.Type.class);
            for (BasicLanguageData.Type status : BasicLanguageData.Type.values()) {
                Set<String> scripts = status_scripts == null ? null : status_scripts.getAll(status);
                Set<String> territories = status_territories == null ? null : status_territories.getAll(status);
                if (scripts == null && territories == null) continue;
                BasicLanguageData bld = new BasicLanguageData();
                bld.setTerritories(territories);
                bld.setScripts(scripts);
                bld.setType(status);
                bld.freeze();
                newData.put(status, bld);
            }

            // compare
            if (!CldrUtility.equals(oldData.entrySet(), newData.entrySet())) {
                for (String problem : compare(oldData, newData)) {
                    warnings.add(BadItem.DETAIL.toString("changing <languageData>", languageSubtag
                        + "\t" + english.getName(languageSubtag), problem));
                }
            }

            for (BasicLanguageData bld : newData.values()) {
                Set<String> scripts = bld.getScripts();
                Set<String> territories = bld.getTerritories();
                BasicLanguageData.Type status = bld.getType();
                Log.println("\t\t<language type=\"" + languageSubtag + "\""
                    + (scripts.isEmpty() ? "" : " scripts=\"" + CldrUtility.join(scripts, " ") + "\"")
                    + (territories.isEmpty() ? "" : " territories=\"" + CldrUtility.join(territories, " ") + "\"")
                    + (status == BasicLanguageData.Type.primary ? "" : " alt=\"secondary\"")
                    + "/>");
            }
        }
        Log.println("\t</languageData>");
        for (String s : warnings) {
            if (s.contains("!")) {
                System.out.println(s);
            }
        }
        for (String s : warnings) {
            if (!s.contains("!")) {
                System.out.println(s);
            }
        }
    }

    private static List<String> compare(Map<BasicLanguageData.Type, BasicLanguageData> oldData,
        Map<BasicLanguageData.Type, BasicLanguageData> newData) {
        Map<String, BasicLanguageData.Type> oldDataToType = getDataToType(oldData.values(), true);
        Map<String, BasicLanguageData.Type> newDataToType = getDataToType(newData.values(), true);
        List<String> result = new ArrayList<>();
        StringBuilder temp = new StringBuilder();
        for (String s : Builder.with(new LinkedHashSet<String>()).addAll(oldDataToType.keySet())
            .addAll(newDataToType.keySet()).get()) {
            BasicLanguageData.Type oldValue = oldDataToType.get(s);
            BasicLanguageData.Type newValue = newDataToType.get(s);
            if (!CldrUtility.equals(oldValue, newValue)) {
                temp.setLength(0);
                temp.append("[").append(s).append(":")
                    .append(english.getName(s.length() == 4 ? "script" : "region", s)).append("] ");
                if (oldValue == null) {
                    temp.append(" added as ").append(newValue);
                } else if (newValue == null) {
                    temp.append(" REMOVED!");
                } else if (oldValue == BasicLanguageData.Type.primary) {
                    temp.append(" DOWNGRADED TO! ").append(newValue);
                } else {
                    temp.append(" upgraded to ").append(newValue);
                }
                result.add(temp.toString());
            }
        }
        result.add(newData.toString());
        return result;
    }

    private static Map<String, BasicLanguageData.Type> getDataToType(
        Collection<BasicLanguageData> collection, boolean script) {
        Map<String, BasicLanguageData.Type> result = new TreeMap<String, BasicLanguageData.Type>();
        for (BasicLanguageData i : collection) {
            for (String s : i.getScripts()) {
                result.put(s, i.getType());
            }
            for (String s : i.getTerritories()) {
                result.put(s, i.getType());
            }
        }
        return result;
    }

    private static void checkBasicData(Map<String, RowData> localeToRowData) {
        // find languages with multiple scripts
        Relation<String, String> languageToScripts = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);
        for (String languageSubtag : language2BasicLanguageData.keySet()) {
            for (BasicLanguageData item : language2BasicLanguageData.getAll(languageSubtag)) {
                languageToScripts.putAll(StandardCodes.fixLanguageTag(languageSubtag), item.getScripts());
            }
        }
        // get primary combinations
        Set<String> primaryCombos = new TreeSet<String>();
        Set<String> basicCombos = new TreeSet<String>();
        for (String languageSubtag : language2BasicLanguageData.keySet()) {
            for (BasicLanguageData item : language2BasicLanguageData.getAll(languageSubtag)) {
                Set<String> scripts = new TreeSet<String>();
                scripts.addAll(item.getScripts());
                languageToScripts.putAll(StandardCodes.fixLanguageTag(languageSubtag), scripts);
                if (scripts.size() == 0) {
                    scripts.add("Zzzz");
                }
                Set<String> territories = new TreeSet<String>();
                territories.addAll(item.getTerritories());
                if (territories.size() == 0) {
                    territories.add("ZZ");
                    continue;
                }

                for (String script : scripts) {
                    for (String territory : territories) {
                        String locale = StandardCodes.fixLanguageTag(languageSubtag)
                            // + (script.equals("Zzzz") ? "" : languageToScripts.getAll(languageSubtag).size() <= 1 ? ""
                            // : "_" + script)
                            + (territories.equals("ZZ") ? "" : "_" + territory);
                        if (item.getType() != BasicLanguageData.Type.secondary) {
                            primaryCombos.add(locale);
                        }
                        basicCombos.add(locale);
                    }
                }
            }
        }
        Set<String> populationOver20 = new TreeSet<String>();
        Set<String> population = new TreeSet<String>();
        LanguageTagParser ltp = new LanguageTagParser();
        for (String rawLocale : localeToRowData.keySet()) {
            ltp.set(rawLocale);
            String locale = ltp.getLanguage() + (ltp.getRegion().length() == 0 ? "" : "_" + ltp.getRegion());
            population.add(locale);
            RowData rowData = localeToRowData.get(rawLocale);
            if (rowData.getLanguagePopulation() / rowData.countryPopulation >= 0.2
            //|| rowData.getLanguagePopulation() > 900000
            ) {
                populationOver20.add(locale);
            } else {
                PopulationData popData = supplementalData.getLanguageAndTerritoryPopulationData(
                    ltp.getLanguageScript(), ltp.getRegion());
                if (popData != null && popData.getOfficialStatus().isOfficial()) {
                    populationOver20.add(locale);
                }
            }
        }
        Set<String> inBasicButNotPopulation = new TreeSet<String>(primaryCombos);

        inBasicButNotPopulation.removeAll(population);
        for (String locale : inBasicButNotPopulation) {
            ltp.set(locale);
            String region = ltp.getRegion();
            String language = ltp.getLanguage();
            if (!sc.isModernLanguage(language)) continue;
            PopulationData popData = supplementalData.getPopulationDataForTerritory(region);
            // Afghanistan AF "29,928,987" 28.10% "21,500,000,000" Hazaragi haz "1,770,000" 28.10%
            BadItem.WARNING.show("In Basic Data but not Population > 20%",
                getDisplayCountry(region)
                    + "\t" + region
                    + "\t\"" + formatNumber(popData.getPopulation(), 0, false) + "\""
                    + "\t\"" + formatPercent(popData.getLiteratePopulation() / popData.getPopulation(), 0, false)
                    + "\""
                    + "\t\"" + formatPercent(popData.getGdp(), 0, false) + "\""
                    + "\t" + ""
                    + "\t" + getLanguageName(language)
                    + "\t" + language
                    + "\t" + -1
                    + "\t\"" + formatPercent(popData.getLiteratePopulation() / popData.getPopulation(), 0, false)
                    + "\"");
        }

        Set<String> inPopulationButNotBasic = new TreeSet<String>(populationOver20);
        inPopulationButNotBasic.removeAll(basicCombos);
        for (Iterator<String> it = inPopulationButNotBasic.iterator(); it.hasNext();) {
            String locale = it.next();
            if (locale.endsWith("_ZZ")) {
                it.remove();
            }
        }
        for (String locale : inPopulationButNotBasic) {
            BadItem.WARNING.show("In Population>20% but not Basic Data", locale + " " + getLanguageName(locale), localeToRowData.get(locale).toString());
        }
    }

    static class LanguageInfo {
        static LanguageInfo INSTANCE = new LanguageInfo();

        Map<String, Set<String>> languageToScripts = new TreeMap<String, Set<String>>();
        Map<String, Set<String>> languageToRegions = new TreeMap<String, Set<String>>();
        Map<String, Comments> languageToComments = new TreeMap<String, Comments>();

        Map<String, Set<String>> languageToScriptsAlt = new TreeMap<String, Set<String>>();
        Map<String, Set<String>> languageToRegionsAlt = new TreeMap<String, Set<String>>();
        Map<String, Comments> languageToCommentsAlt = new TreeMap<String, Comments>();

        private LanguageInfo() {
            cldrFactory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
            //Set<String> available = cldrFactory.getAvailable();
            CLDRFile supplemental = cldrFactory.make("supplementalData", true);
            for (Iterator<String> it = supplemental.iterator("//supplementalData/languageData/language"); it.hasNext();) {
                String xpath = it.next();
                XPathParts parts = XPathParts.getTestInstance(xpath);
                Map<String, String> x = parts.getAttributes(-1);
                boolean alt = x.containsKey("alt");
                String lang = x.get("type");
                List<String> scripts = getAttributeList(x, "scripts");
                if (scripts != null) {
                    if (alt) {
                        putAll(languageToScriptsAlt, lang, new LinkedHashSet<String>(scripts));
                    } else {
                        putAll(languageToScripts, lang, new LinkedHashSet<String>(scripts));
                    }
                }
                List<String> regions = getAttributeList(x, "territories");
                if (regions != null) {
                    if (alt) {
                        putAll(languageToRegionsAlt, lang, new LinkedHashSet<String>(regions));
                    } else {
                        putAll(languageToRegions, lang, new LinkedHashSet<String>(regions));
                    }
                }
            }
        }

        private List<String> getAttributeList(Map<String, String> x, String attribute) {
            List<String> scripts = null;
            String scriptString = x.get(attribute);
            if (scriptString != null) {
                scripts = Arrays.asList(scriptString.split("\\s+"));
            }
            return scripts;
        }
    }

    private static <K, V> void putUnique(Map<K, V> map, K key, V value) {
        V oldValue = map.get(key);
        if (oldValue != null && !oldValue.equals(value)) {
            throw new IllegalArgumentException("Duplicate value for <" + key + ">: <" + oldValue + ">, <" + value + ">");
        }
        map.put(key, value);
    }

    private static <K, W> void putAll(Map<K, Set<W>> map, K key, Set<W> values) {
        Set<W> oldValue = map.get(key);
        if (oldValue == null) {
            map.put(key, values);
        } else {
            oldValue.addAll(values);
        }
    }

    // public enum OfficialStatus {unknown, de_facto_official, official, official_regional, official_minority};

    static class RowData implements Comparable<Object> {
        private final String countryCode;
        private final double countryGdp;
        private final double countryLiteracy;
        private final double countryPopulation;
        private final String languageCode;
        private final OfficialStatus officialStatus;
        private final double languagePopulation;
        private final double languageLiteracy;
        private final String comment;
        private final String notes;
        private final String badLanguageName;
        private final boolean relativeLanguagePopulation;
        // String badLanguageCode = "";
        private final static Set<String> doneCountries = new HashSet<String>();

        private final static Set<String> countryCodes = sc.getGoodAvailableCodes("territory");

        public RowData(String country, String language) {
            this.countryCode = country;
            this.languageCode = language;
            badLanguageName = country = language = notes = comment = "";
            officialStatus = OfficialStatus.unknown;
            countryGdp = roundToPartsPer(AddPopulationData.getGdp(countryCode).doubleValue(), 1000);
            countryLiteracy = AddPopulationData.getLiteracy(countryCode).doubleValue() / 100.0d;
            countryPopulation = AddPopulationData.getPopulation(countryCode).doubleValue();
            languagePopulation = languageLiteracy = Double.NaN;
            relativeLanguagePopulation = false;
        }

        RowData(List<String> row) throws ParseException {
            countryCode = fixCountryCode(row.get(COUNTRY_CODE), row);

            if (!countryCodes.contains(countryCode)) {
                System.err.println("WRONG COUNTRY CODE: " + row);
            }

            double countryPopulation1 = parseDecimal(row.get(COUNTRY_POPULATION));
            double countryLiteracy1 = parsePercent(row.get(COUNTRY_LITERACY), countryPopulation1);

            countryGdp = roundToPartsPer(AddPopulationData.getGdp(countryCode).doubleValue(), 1000);
            countryLiteracy = AddPopulationData.getLiteracy(countryCode).doubleValue() / 100.0d;
            countryPopulation = AddPopulationData.getPopulation(countryCode).doubleValue();

            String officialStatusString = row.get(OFFICIAL_STATUS).trim().replace(' ', '_');
            if (officialStatusString.equals("national")) {
                officialStatusString = "official";
            } else if (officialStatusString.equals("regional_official")) {
                officialStatusString = "official_regional";
            } else if (officialStatusString.length() == 0 || officialStatusString.equals("uninhabited")) {
                officialStatusString = "unknown";
            }
            try {
                officialStatus = OfficialStatus.valueOf(officialStatusString);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Can't interpret offical-status: " + officialStatusString);
            }

            String languageCode1 = row.get(LANGUAGE_CODE);
            if (languageCode1.startsWith("*") || languageCode1.startsWith("\u00A7")) {
                languageCode1 = languageCode1.substring(1);
            }
            languageCode = fixLanguageCode(languageCode1, row);

            if (doneCountries.contains(countryCode) == false) {
                // showDiff(countryGdp1, countryGdp);
                // showDiff(countryLiteracy1, countryLiteracy);
                if (SHOW_DIFF) showDiff(countryPopulation1, countryPopulation, 0.1, false);
                doneCountries.add(countryCode);
            }

            double languagePopulation1 = parsePercent(row.get(LANGUAGE_POPULATION), countryPopulation1)
                * countryPopulation1;
            if ((officialStatus.isMajor())
                && languagePopulation1 * 100 < countryPopulation && languagePopulation1 < 1000000) {
                BadItem.WARNING.show("official language has population < 1% of country & < 1,000,000", languageCode + ", " + Math.round(languagePopulation1),
                    row);
            }
            if (languagePopulation1 < 0.999) {
                BadItem.WARNING.show("suspect language population, < 1", languageCode + ", " + Math.round(languagePopulation1), row);
            }
            if (languagePopulation1 > 10000) {
                relativeLanguagePopulation = true;
                languagePopulation1 = languagePopulation1 * countryPopulation / countryPopulation1; // correct the
                // values
            } else {
                relativeLanguagePopulation = false;
            }
            if (isApproximatelyGreater(languagePopulation1, countryPopulation, 0.0001)) {
                BadItem.ERROR.show("language population > country population", Math.round(languagePopulation1) + " > " + countryPopulation, row);
            }
            languagePopulation = languagePopulation1 < countryPopulation ? languagePopulation1 : countryPopulation;

            if (SHOW_DIFF)
                showDiff(languagePopulation1 / countryPopulation1, languagePopulation / countryPopulation, 0.01, true);

            String stringLanguageLiteracy = row.size() <= LANGUAGE_LITERACY ? "" : row.get(LANGUAGE_LITERACY);
            double languageLiteracy1 = stringLanguageLiteracy.length() == 0 ? countryLiteracy
                : parsePercent(stringLanguageLiteracy, languagePopulation);
            if (isApproximatelyEqual(languageLiteracy1, countryLiteracy1, 0.001)) {
                languageLiteracy1 = countryLiteracy; // correct the values
            }
            languageLiteracy = languageLiteracy1;

            if (row.size() > COMMENT) {
                comment = row.get(COMMENT);
            } else {
                comment = "";
            }
            if (row.size() > NOTES) {
                notes = row.get(NOTES);
            } else {
                notes = "";
            }
            badLanguageName = row.get(BAD_LANGUAGE_NAME);
        }

        private void showDiff(double a, double new_a, double maxRelativeDiff, boolean showLang) {
            final double diff = new_a / a - 1;
            if (Math.abs(diff) > maxRelativeDiff) {
                System.out.println(formatPercent(diff, 0, false)
                    + "\t" + countryCode + "\t" + getDisplayCountry(countryCode)
                    + (showLang ? "\t" + languageCode + "\t" + getLanguageName(languageCode) : "")
                    + "\t" + formatNumber(a, 0, false) + "\t=>\t" + formatNumber(new_a, 0, false));
            }
        }

        private double roundToPartsPer(double a, double whole) {
            // break this out just to make it easier to follow.
            double log10 = Math.log10(a / whole);
            long digitsFound = (long) (log10);
            long factor = (long) (Math.pow(10, digitsFound));
            double rounded = Math.round(a / factor);
            double result = rounded * factor;
            // if (Math.abs(result - a) >= 1) {
            // System.out.println("Rounding " + a + " => " + result);
            // }
            return result;
        }

        private static boolean isApproximatelyEqual(double a, double b, double epsilon) {
            return a == b || Math.abs(a - b) < epsilon;
        }

        private static boolean isApproximatelyGreater(double a, double b, double epsilon) {
            return a > b + epsilon;
        }

        double parseDecimal(String numericRepresentation) throws ParseException {
            try {
                // if (numericRepresentation == null || numericRepresentation.length() == 0) return Double.NaN;
                Number result = nf.parse(numericRepresentation);
                // if (result == null) return Double.NaN;
                return result.doubleValue();
            } catch (ParseException e) {
                throw e;
                // (RuntimeException) new IllegalArgumentException("can't parse <" + numericRepresentation +
                // ">").initCause(e);
            }
        }

        double parsePercent(String numericRepresentation, double baseValue) throws ParseException {
            try {
                double result;
                if (numericRepresentation.contains("%")) {
                    Number result0 = pf.parse(numericRepresentation);
                    result = result0.doubleValue();
                } else {
                    Number result0 = nf.parse(numericRepresentation);
                    result = result0.doubleValue() / baseValue;
                }
                // if (numericRepresentation == null || numericRepresentation.length() == 0) return Double.NaN;
                // if (result == null) return Double.NaN;
                return result;
            } catch (ParseException e) {
                throw e;
                // (RuntimeException) new IllegalArgumentException("can't parse <" + numericRepresentation +
                // ">").initCause(e);
            }
        }

        public double getLanguageLiteratePopulation() {
            return languageLiteracy * languagePopulation;
        }

        /**
         * Get the weighted population
         *
         * @param weightIfNotOfficial
         * @return
         */
        public double getLanguageLiteratePopulation(double weightIfNotOfficial) {
            double result = languageLiteracy * languagePopulation;
            if (!officialStatus.isMajor()) {
                result *= weightIfNotOfficial;
            }
            return result;
        }

        public int compareTo(Object o) {
            RowData that = (RowData) o;
            int result;
            if (0 != (result = GENERAL_COLLATOR.compare(countryCode, that.countryCode))) return result;
            if (languagePopulation > that.languagePopulation) return -1; // descending
            if (languagePopulation < that.languagePopulation) return 1;
            if (0 != (result = GENERAL_COLLATOR.compare(languageCode, that.languageCode))) return result;
            return 0;
        }

        public static String toStringHeader() {
            return "countryCode" + "\t" + "countryPopulation" + "\t" + "countryGdp"
                + "\t" + "countryLiteracy"
                + "\t" + "languagePopulation" + "\t" + "languageCode"
                + "\t" + "writingPopulation";
        }

        public String toString() {
            return countryCode + "\t" + countryPopulation + "\t" + countryGdp
                + "\t" + countryLiteracy
                + "\t" + languagePopulation + "\t" + languageCode
                + "\t" + languageLiteracy;
        }

        public String toString(boolean b) {
            return "region:\t" + getCountryCodeAndName(countryCode)
                + "\tpop:\t" + countryPopulation
                + "\tgdp:\t" + countryGdp
                + "\tlit:\t" + countryLiteracy
                + "\tlang:\t" + getLanguageCodeAndName(languageCode)
                + "\tpop:\t" + languagePopulation
                + "\tlit:\t" + languageLiteracy;
        }

        static boolean MARK_OUTPUT = false;

        public String getRickLanguageCode() {
            if (languageCode.contains("_")) return languageCode;
            Source source = Iso639Data.getSource(languageCode);
            if (source == null) {
                return "§" + languageCode;
            }
            if (MARK_OUTPUT) {
                if (source == Source.ISO_639_3) {
                    return "*" + languageCode;
                }
            }
            return languageCode;
        }

        static Map<String, String> oldToFixed = new HashMap<>();

        public String getRickLanguageName() {
            String cldrResult = getExcelQuote(english.getName(languageCode, true));
//            String result = getRickLanguageName2();
//            if (!result.equalsIgnoreCase(cldrResult)) {
//                if (null == oldToFixed.put(result, cldrResult)) {
//                    System.out.println("## " + result + "!=" + cldrResult);
//                }
//            }
            return cldrResult;
        }

        public String getRickLanguageName2() {
            String result = new ULocale(languageCode).getDisplayName();
            if (!result.equals(languageCode)) return getExcelQuote(result);
            Set<String> names = Iso639Data.getNames(languageCode);
            if (names != null && names.size() != 0) {
                if (MARK_OUTPUT) {
                    return getExcelQuote("*" + names.iterator().next());
                } else {
                    return getExcelQuote(names.iterator().next());
                }
            }
            return getExcelQuote("§" + badLanguageName);
        }

        public String getCountryName() {
            return getExcelQuote(getDisplayCountry(countryCode));
        }

        public String getCountryGdpString() {
            return getExcelQuote(formatNumber(countryGdp, 0, false));
        }

        public String getCountryLiteracyString() {
            return formatPercent(countryLiteracy, 2, false);
        }

        public String getCountryPopulationString() {
            return getExcelQuote(formatNumber(countryPopulation, 0, false));
        }

        public String getLanguageLiteracyString() {
            return formatPercent(languageLiteracy, 2, false);
        }

        public String getLanguagePopulationString() {

            try {
                final double percent = languagePopulation / countryPopulation;
                return getExcelQuote(relativeLanguagePopulation
                    && percent > 0.03
                    && languagePopulation > 10000
                        ? formatPercent(percent, 2, false)
                        : formatNumber(languagePopulation, 3, false));
            } catch (IllegalArgumentException e) {
                return "NaN";
            }
        }

        private double getLanguagePopulation() {
            return languagePopulation;
        }

    }

    public static String getExcelQuote(String comment) {
        return comment == null || comment.length() == 0 ? ""
            : comment.contains(",") ? '"' + comment + '"'
                : comment.contains("\"") ? '"' + comment.replace("\"", "\"\"") + '"'
                    : comment;
    }

    public static String getCountryCodeAndName(String code) {
        if (code == null) return null;
        return english.getName(CLDRFile.TERRITORY_NAME, code) + " [" + code + "]";
    }

    static class RickComparator implements Comparator<RowData> {
        public int compare(RowData me, RowData that) {
            int result;
            if (0 != (result = GENERAL_COLLATOR.compare(me.getCountryName(), that.getCountryName()))) return result;
            if (0 != (result = GENERAL_COLLATOR.compare(me.getRickLanguageName(), that.getRickLanguageName())))
                return result;
            return me.compareTo(that);
        }
    }

    private static void writeTerritoryLanguageData(List<String> failures, Set<RowData> sortedInput) {

        String lastCountryCode = "";
        boolean first = true;
        LanguageTagParser ltp = new LanguageTagParser();

        Log.println(" <!-- See http://unicode.org/cldr/data/diff/supplemental/territory_language_information.html for more information on territoryInfo. -->");
        Log.println("\t<territoryInfo>");

        for (RowData row : sortedInput) {
            String countryCode = row.countryCode;

            double countryPopulationRaw = row.countryPopulation;
            double countryPopulation = countryPopulationRaw; // (long) Utility.roundToDecimals(countryPopulationRaw, 2);
            double languageLiteracy = row.languageLiteracy;
            double countryLiteracy = row.countryLiteracy;

            double countryGDPRaw = row.countryGdp;
            long countryGDP = Math.round(countryGDPRaw / gdpFactor);

            String languageCode = row.languageCode;

            double languagePopulationRaw = row.getLanguagePopulation();
            double languagePopulation = languagePopulationRaw; // (long) Utility.roundToDecimals(languagePopulationRaw,
            // 2);

            double languagePopulationPercent = languagePopulation / countryPopulation;
            // Utility.roundToDecimals(Math.min(100, Math.max(0,
            // languagePopulation*100 / (double)countryPopulation)),3);

            if (!countryCode.equals(lastCountryCode)) {
                if (first) {
                    first = false;
                } else {
                    Log.println("\t\t</territory>");
                }
                Log.print("\t\t<territory type=\"" + countryCode + "\""
                    + " gdp=\"" + formatNumber(countryGDP, 4, true) + "\""
                    + " literacyPercent=\"" + formatPercent(countryLiteracy, 3, true) + "\""
                    + " population=\"" + formatNumber(countryPopulation, 6, true) + "\">");
                lastCountryCode = countryCode;
                Log.println("\t<!--" + getDisplayCountry(countryCode) + "-->");
            }

            if (languageCode.length() != 0
                && languagePopulationPercent > 0.0000
                && (ALLOW_SMALL_NUMBERS || languagePopulationPercent >= 1 || languagePopulationRaw > 100000
                    || languageCode.equals("haw") || row.officialStatus.isOfficial())) {
                // add best case
                addBestRegion(languageCode, countryCode, languagePopulationRaw);
                String baseScriptLanguage = ltp.set(languageCode).getLanguageScript();
                if (!baseScriptLanguage.equals(languageCode)) {
                    addBestRegion(baseScriptLanguage, countryCode, languagePopulationRaw);
                }
                String baseLanguage = ltp.set(baseScriptLanguage).getLanguage();
                if (!baseLanguage.equals(baseScriptLanguage)) {
                    addBestRegion(baseLanguage, countryCode, languagePopulationRaw);
                    addBestScript(baseLanguage, ltp.set(languageCode).getScript(), languagePopulationRaw);
                }

                if (languageLiteracy != countryLiteracy) {
                    int debug = 0;
                }
                Log.print("\t\t\t<languagePopulation type=\""
                    + languageCode
                    + "\""
                    + (DoubleMath.fuzzyCompare(languageLiteracy, countryLiteracy, 0.0001) == 0 ? ""
                        : (DoubleMath.fuzzyCompare(languageLiteracy, 0.05, 0.0001) == 0 ? " writingPercent=\"" : " literacyPercent=\"")
                            + formatPercent(languageLiteracy, 2, true) + "\"")
                    + " populationPercent=\"" + formatPercent(languagePopulationPercent, 2, true) + "\""
                    + (row.officialStatus.isOfficial() ? " officialStatus=\"" + row.officialStatus + "\"" : "")
                    + references.addReference(row.notes)
                    + "/>");
                Log.println("\t<!--" + getLanguageName(languageCode) + "-->");
            } else if (!row.countryCode.equals("ZZ")) {
                failures.add(BadItem.ERROR.toString("too few speakers: suspect line", languageCode, row.toString(true)));
            }
            // if (first) {
            if (false) System.out.print(
                "countryCode: " + countryCode + "\t"
                    + "countryPopulation: " + countryPopulation + "\t"
                    + "countryGDP: " + countryGDP + "\t"
                    + "languageCode: " + languageCode + "\t"
                    + "languagePopulation: " + languagePopulation + CldrUtility.LINE_SEPARATOR);
            // }
        }

        Log.println("\t\t</territory>");
        Log.println("\t</territoryInfo>");
    }

    private static String getDisplayCountry(String countryCode) {
        String result = getULocaleCountryName(countryCode);
        if (!result.equals(countryCode)) {
            return result;
        }
        result = sc.getData("territory", countryCode);
        if (result != null) {
            return result;
        }
        return countryCode;
        // new ULocale("und-" + countryCode).getDisplayCountry()
    }

    private static String getDisplayScript(String scriptCode) {
        String result = getULocaleScriptName(scriptCode);
        if (!result.equals(scriptCode)) {
            return result;
        }
        result = sc.getData("territory", scriptCode);
        if (result != null) {
            return result;
        }
        return scriptCode;
        // new ULocale("und-" + countryCode).getDisplayCountry()
    }

    private static String getLanguageName(String languageCode) {
        String result = getULocaleLocaleName(languageCode);
        if (!result.equals(languageCode)) return result;
        Set<String> names = Iso639Data.getNames(languageCode);
        if (names != null && names.size() != 0) {
            return names.iterator().next();
        }
        return languageCode;
    }

    static class References {
        Map<String, Pair<String, String>> Rxxx_to_reference = new TreeMap<String, Pair<String, String>>();
        Map<Pair<String, String>, String> reference_to_Rxxx = new TreeMap<Pair<String, String>, String>();
        Map<String, Pair<String, String>> Rxxx_to_oldReferences = supplementalData.getReferences();
        Map<Pair<String, String>, String> oldReferences_to_Rxxx = new TreeMap<Pair<String, String>, String>();
        {
            for (String Rxxx : Rxxx_to_oldReferences.keySet()) {
                oldReferences_to_Rxxx.put(Rxxx_to_oldReferences.get(Rxxx), Rxxx);
            }
        }
        Matcher URI = PatternCache.get("([a-z]+\\://[\\S]+)\\s?(.*)").matcher("");

        static int referenceStart = 1000;

        /**
         * Returns " references=\"" + Rxxx + "\"" or "" if there is no reference.
         *
         * @param rawReferenceText
         * @return
         */
        private String addReference(String rawReferenceText) {
            if (rawReferenceText == null || rawReferenceText.length() == 0) return "";
            Pair<String, String> p;
            if (URI.reset(rawReferenceText).matches()) {
                p = new Pair<String, String>(URI.group(1), URI.group(2) == null || URI.group(2).length() == 0 ? "[missing]"
                    : URI.group(2)).freeze();
            } else {
                p = new Pair<String, String>(null, rawReferenceText).freeze();
            }

            String Rxxx = reference_to_Rxxx.get(p);
            if (Rxxx == null) { // add new
                Rxxx = oldReferences_to_Rxxx.get(p);
                if (Rxxx != null) { // if old, just keep number
                    p = Rxxx_to_oldReferences.get(Rxxx);
                } else { // find an empty number
                    while (true) {
                        Rxxx = "R" + (referenceStart++);
                        if (Rxxx_to_reference.get(Rxxx) == null && Rxxx_to_oldReferences.get(Rxxx) == null) {
                            break;
                        }
                    }
                }
                // add to new references
                reference_to_Rxxx.put(p, Rxxx);
                Rxxx_to_reference.put(Rxxx, p);
            }
            // references="R034"
            return " references=\"" + Rxxx + "\"";
        }

        String getReferenceHTML(String Rxxx) {
            Pair<String, String> p = Rxxx_to_reference.get(Rxxx); // exception if fails.
            String uri = p.getFirst();
            String value = p.getSecond();
            uri = uri == null ? "" : " uri=\"" + TransliteratorUtilities.toHTML.transliterate(uri) + "\"";
            value = value == null ? "[missing]" : TransliteratorUtilities.toHTML.transliterate(value);
            return "\t\t<reference type=\"" + Rxxx + "\"" + uri + ">" + value + "</reference>";
        }

        void printReferences() {
            // <reference type="R034" uri="isbn:0-321-18578-1">The Unicode Standard 4.0</reference>
            Log.println("\t<references>");
            for (String Rxxx : Rxxx_to_reference.keySet()) {
                Log.println(getReferenceHTML(Rxxx));
            }
            Log.println("\t</references>");
        }
    }

    static References references = new References();

    private static Set<RowData> getExcelData(List<String> failures, Map<String, RowData> localeToRowData)
        throws IOException {

        LanguageTagParser ltp = new LanguageTagParser();

        String dir = CLDRPaths.GEN_DIRECTORY + "supplemental/";
        final String ricksFile = "country_language_population_raw.txt";
        System.out.println("\n# Problems in " + ricksFile + "\n");
        List<List<String>> input = SpreadSheet.convert(CldrUtility.getUTF8Data(ricksFile));

        Set<String> languages = languagesNeeded; // sc.getGoodAvailableCodes("language");

        Set<String> territories = new TreeSet<String>(sc.getGoodAvailableCodes("territory"));
        territories.removeAll(supplementalData.getContainers());
        territories.remove("EU");
        territories.remove("QO");

        Set<String> countriesNotFound = new TreeSet<String>(territories);
        Set<OfficialStatus> statusFound = new TreeSet<OfficialStatus>();
        Set<String> countriesWithoutOfficial = new TreeSet<String>(territories);
        countriesWithoutOfficial.remove("ZZ");

        Map<String, Row.R2<String, Double>> countryToLargestOfficialLanguage = new HashMap<String, Row.R2<String, Double>>();

        Set<String> languagesNotFound = new TreeSet<String>(languages);
        Set<RowData> sortedInput = new TreeSet<RowData>();
        int count = 0;
        for (List<String> row : input) {
            ++count;
            if (count == 1 || row.size() <= COUNTRY_GDP) {
                failures.add(join(row, "\t") + "\tShort row");
                continue;
            }
            try {
                RowData x = new RowData(row);
                if (x.officialStatus.isOfficial()) {
                    Row.R2<String, Double> largestOffical = countryToLargestOfficialLanguage.get(x.countryCode);
                    if (largestOffical == null) {
                        countryToLargestOfficialLanguage.put(x.countryCode,
                            Row.of(x.languageCode, x.languagePopulation));
                    } else if (largestOffical.get1() < x.languagePopulation) {
                        largestOffical.set0(x.languageCode);
                        largestOffical.set1(x.languagePopulation);
                    }
                }
                if (x.officialStatus.isMajor() || x.countryPopulation < 1000) {
                    countriesWithoutOfficial.remove(x.countryCode);
                }
                if (!checkCode(LstrType.region, x.countryCode, row)) continue;
                statusFound.add(x.officialStatus);
                countriesNotFound.remove(x.countryCode);
                languagesNotFound.remove(x.languageCode);
                if (x.languageCode.contains("_")) {
                    ltp.set(x.languageCode);
                    languagesNotFound.remove(ltp.getLanguage());
                    if (!checkCode(LstrType.language, ltp.getLanguage(), row)) continue;
                    if (!checkCode(LstrType.script, ltp.getScript(), row)) continue;
                }
                String locale = x.languageCode + "_" + x.countryCode;
                if (localeToRowData.get(locale) != null) {
                    BadItem.ERROR.show("duplicate data", x.languageCode + " with " + x.countryCode, row);
                }
                localeToRowData.put(locale, x);
                sortedInput.add(x);
            } catch (ParseException e) {
                failures.add(join(row, "\t") + "\t" + e.getMessage() + "\t"
                    + join(Arrays.asList(e.getStackTrace()), ";\t"));
            } catch (RuntimeException e) {
                throw (RuntimeException) new IllegalArgumentException("Failure on line " + count + ")\t" + row)
                    .initCause(e);
            }
        }
        // System.out.println("Note: the following Status values were found in the data: " +
        // CldrUtility.join(statusFound, " | "));

        // make sure we have something
        for (String country : countriesNotFound) {
            RowData x = new RowData(country, "und");
            sortedInput.add(x);
        }
        for (String language : languagesNotFound) {
            RowData x = new RowData("ZZ", language);
            sortedInput.add(x);
        }

        for (RowData row : sortedInput) {
            // see which countries have languages that are larger than any offical language

            if (!row.officialStatus.isOfficial()) {
                //String country = row.countryCode;
                Row.R2<String, Double> largestOffical = countryToLargestOfficialLanguage.get(row.countryCode);
                if (largestOffical != null && largestOffical.get1() < row.languagePopulation) {
                    BadItem.WARNING.show("language population > all official languages", getLanguageCodeAndName(largestOffical.get0()), row.toString(true));
                }
            }

            // see which countries are missing an official language
            if (!countriesWithoutOfficial.contains(row.countryCode)) continue;
            BadItem.ERROR.show("missing official language", row.getCountryName() + "\t" + row.countryCode, row.toString(true));
            countriesWithoutOfficial.remove(row.countryCode);
        }

        // write out file for rick
        PrintWriter log = FileUtilities.openUTF8Writer(dir, ricksFile);
        log.println(
            "*\tCName" +
                "\tCCode" +
                "\tCPopulation" +
                "\tCLiteracy" +
                "\tCGdp" +
                "\tOfficialStatus" +
                "\tLanguage" +
                "\tLCode" +
                "\tLPopulation" +
                "\tWritingPop" +
                "\tReferences" +
                "\tNotes");
        RickComparator rickSorting = new RickComparator();
        Set<RowData> rickSorted = new TreeSet<RowData>(rickSorting);
        rickSorted.addAll(sortedInput);

        for (RowData row : rickSorted) {
            final String langLit = row.getLanguageLiteracyString();
            final String countryLit = row.getCountryLiteracyString();
            log.println(
                row.getCountryName()
                    + "\t" + row.countryCode
                    + "\t" + row.getCountryPopulationString()
                    + "\t" + countryLit
                    + "\t" + row.getCountryGdpString()
                    + "\t" + (row.officialStatus == OfficialStatus.unknown ? "" : row.officialStatus)
                    + "\t" + row.getRickLanguageName()
                    + "\t" + row.getRickLanguageCode()
                    + "\t" + row.getLanguagePopulationString()
                    + "\t" + (langLit.equals(countryLit) ? "" : langLit)
                    + "\t" + getExcelQuote(row.comment)
                    + "\t" + getExcelQuote(row.notes));
        }
        log.close();
        return sortedInput;
    }

    private static Set<String> getCldrParents(Set<String> available) {
        LanguageTagParser ltp2 = new LanguageTagParser();
        Set<String> cldrParents = new TreeSet<String>();
        for (String locale : available) {
            if (skipLocales.contains(locale)) continue;
            try {
                ltp2.set(locale);
            } catch (RuntimeException e) {
                System.out.println("Skipping CLDR file: " + locale);
                continue;
            }
            String locale2 = ltp2.getLanguageScript();
            if (locale2.equals("sh")) continue;
            // int lastPos = locale.lastIndexOf('_');
            // if (lastPos < 0) continue;
            // String locale2 = locale.substring(0,lastPos);
            cldrParents.add(locale2);
            languageToMaxCountry.put(locale2, null);
        }
        //System.out.println("CLDR Parents: " + cldrParents);
        return cldrParents;
    }

    private static void showFailures(List<String> failures) {
        if (failures.size() <= 1) {
            return;
        }
        System.out.println();
        System.out.println("Failures in Output");
        System.out.println();

        System.out.println(RowData.toStringHeader());
        for (String failure : failures) {
            System.out.println(failure);
        }
    }

    public static String getProcessedParent(String localeCode) {
        if (localeCode == null || localeCode.equals("root")) return null;
        int pos = localeCode.lastIndexOf('_');
        if (pos < 0) return "root";
        LanguageTagParser ltp = new LanguageTagParser();
        String script = ltp.set(localeCode).getScript();
        if (script.length() == 0) {
            return getFullyResolved(localeCode);
        }
        return localeCode.substring(0, pos);
    }

    private static String getFullyResolved(String languageCode) {
        String result = defaultContent.get(languageCode);
        if (result != null) return result;
        // we missed. Try taking parent and trying again
        int pos = languageCode.length() + 1;
        while (true) {
            pos = languageCode.lastIndexOf('_', pos - 1);
            if (pos < 0) {
                return "***" + languageCode;
            }
            result = defaultContent.get(languageCode.substring(0, pos));
            if (result != null) {
                LanguageTagParser ltp = new LanguageTagParser().set(languageCode);
                LanguageTagParser ltp2 = new LanguageTagParser().set(result);
                String region = ltp.getRegion();
                if (region.length() == 0) {
                    ltp.setRegion(ltp2.getRegion());
                }
                String script = ltp.getScript();
                if (script.length() == 0) {
                    ltp.setScript(ltp2.getScript());
                }
                return ltp.toString();
            }
        }
    }

    static Comparator<Iterable> firstElementComparator = new Comparator<Iterable>() {
        public int compare(Iterable o1, Iterable o2) {
            int result = ((Comparable) o1.iterator().next()).compareTo((o2.iterator().next()));
            assert result != 0;
            return result;
        }
    };

    private static void showDefaults(Set<String> cldrParents, NumberFormat nf, Map<String, String> defaultContent,
        Map<String, RowData> localeToRowData,
        Set<String> defaultLocaleContent) {

        if (SHOW_OLD_DEFAULT_CONTENTS) {
            System.out.println();
            System.out.println("Computing Defaults Contents");
            System.out.println();
        }

        Factory cldrFactory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
        Set<String> locales = new TreeSet<String>(cldrFactory.getAvailable());
        LocaleIDParser lidp = new LocaleIDParser();

        // add all the combinations of language, script, and territory.
        for (String locale : localeToRowData.keySet()) {
            String baseLanguage = lidp.set(locale).getLanguage();
            if (locales.contains(baseLanguage) && !locales.contains(locale)) {
                locales.add(locale);
                if (SHOW_OLD_DEFAULT_CONTENTS) System.out.println("\tadding: " + locale);
            }
        }

        // adding parents
        Set<String> toAdd = new TreeSet<String>();
        while (true) {
            for (String locale : locales) {
                String newguy = LocaleIDParser.getParent(locale);
                if (newguy != null && !locales.contains(newguy) && !toAdd.contains(newguy)) {
                    toAdd.add(newguy);
                    if (SHOW_OLD_DEFAULT_CONTENTS) System.out.println("\tadding parent: " + newguy);
                }
            }
            if (toAdd.size() == 0) {
                break;
            }
            locales.addAll(toAdd);
            toAdd.clear();
        }

        // get sets of siblings
        Set<Set<String>> siblingSets = new TreeSet<Set<String>>(firstElementComparator);
        Set<String> needsADoin = new TreeSet<String>(locales);

        Set<String> deprecatedLanguages = new TreeSet<String>();
        deprecatedLanguages.add("sh");
        Set<String> deprecatedRegions = new TreeSet<String>();
        deprecatedRegions.add("YU");
        deprecatedRegions.add("CS");
        deprecatedRegions.add("ZZ");

        // first find all the language subtags that have scripts, and those we need to skip. Those are aliased-only
        Set<String> skippingItems = new TreeSet<String>();
        Set<String> hasAScript = new TreeSet<String>();
        //Set<LocaleIDParser.Level> languageOnly = EnumSet.of(LocaleIDParser.Level.Language);
        for (String locale : locales) {
            lidp.set(locale);
            if (lidp.getScript().length() != 0) {
                hasAScript.add(lidp.getLanguage());
            }
            Set<LocaleIDParser.Level> levels = lidp.getLevels();
            // must have no variants, must have either script or region, no deprecated elements
            if (levels.contains(LocaleIDParser.Level.Variants) // no variants
                || !(levels.contains(LocaleIDParser.Level.Script)
                    || levels.contains(LocaleIDParser.Level.Region))
                || deprecatedLanguages.contains(lidp.getLanguage())
                || deprecatedRegions.contains(lidp.getRegion())) {
                // skip language-only locales, and ones with variants
                needsADoin.remove(locale);
                skippingItems.add(locale);
                if (SHOW_OLD_DEFAULT_CONTENTS) System.out.println("\tremoving: " + locale);
                continue;
            }
        }
        // walk through the locales, getting the ones we care about.
        Map<String, Double> scriptLocaleToLanguageLiteratePopulation = new TreeMap<String, Double>();

        for (String locale : new TreeSet<String>(needsADoin)) {
            if (!needsADoin.contains(locale)) continue;
            lidp.set(locale);
            Set<Level> level = lidp.getLevels();
            // skip locales that need scripts and don't have them
            if (!level.contains(LocaleIDParser.Level.Script) // no script
                && hasAScript.contains(lidp.getLanguage())) {
                needsADoin.remove(locale);
                skippingItems.add(locale);
                continue;
            }
            // get siblings
            Set<String> siblingSet = lidp.getSiblings(needsADoin);
            // if it has a script and region
            if (level.contains(LocaleIDParser.Level.Script) && level.contains(LocaleIDParser.Level.Region)) {
                double languageLiteratePopulation = 0;
                for (String localeID2 : siblingSet) {
                    RowData rowData = localeToRowData.get(localeID2);
                    if (rowData != null) {
                        languageLiteratePopulation += rowData.getLanguageLiteratePopulation(NON_OFFICIAL_WEIGHT);
                    }
                }
                String parentID = LocaleIDParser.getParent(locale);
                scriptLocaleToLanguageLiteratePopulation.put(parentID, languageLiteratePopulation);
            }

            try {
                siblingSets.add(siblingSet);
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
            needsADoin.removeAll(siblingSet);
        }
        if (SHOW_OLD_DEFAULT_CONTENTS) System.out.println("ConvertLanguageData Skipping: " + skippingItems);
        if (needsADoin.size() != 0) {
            if (SHOW_OLD_DEFAULT_CONTENTS) System.out.println("Missing: " + needsADoin);
        }

        // walk through the data
        Set<String> skippingSingletons = new TreeSet<String>();

        Set<String> missingData = new TreeSet<String>();
        for (Set<String> siblingSet : siblingSets) {
            if (SHOW_OLD_DEFAULT_CONTENTS) System.out.println("** From siblings: " + siblingSet);

            if (false & siblingSet.size() == 1) {
                skippingSingletons.add(siblingSet.iterator().next());
                continue;
            }
            // get best
            double best = Double.NEGATIVE_INFINITY;
            String bestLocale = "???";
            Set<Pair<Double, String>> data = new TreeSet<>();
            LanguageTagParser ltp = new LanguageTagParser();
            for (String locale : siblingSet) {
                RowData rowData = localeToRowData.get(locale);
                double languageLiteratePopulation = -1;
                if (rowData != null) {
                    languageLiteratePopulation = rowData.getLanguageLiteratePopulation(NON_OFFICIAL_WEIGHT);
                } else {
                    Double d = scriptLocaleToLanguageLiteratePopulation.get(locale);
                    if (d != null) {
                        languageLiteratePopulation = d;
                    } else {
                        final String region = ltp.set(locale).getRegion();
                        if (region.isEmpty() || StandardCodes.isCountry(region)) {
                            missingData.add(locale);
                        }
                    }
                }
                data.add(new Pair<Double, String>(languageLiteratePopulation, locale));
                if (best < languageLiteratePopulation) {
                    best = languageLiteratePopulation;
                    bestLocale = locale;
                }
            }
            // show it
            for (Pair<Double, String> datum : data) {
                if (SHOW_OLD_DEFAULT_CONTENTS)
                    System.out.format(
                        "\tContenders: %s %f (based on literate population)" + CldrUtility.LINE_SEPARATOR,
                        datum.getSecond(), datum.getFirst());
            }
            // System.out.format("\tPicking default content: %s %f (based on literate population)" +
            // Utility.LINE_SEPARATOR, bestLocale, best);
            // Hack to fix English
            // TODO Generalize in the future for other locales with non-primary scripts
            if (bestLocale.startsWith("en_")) {
                defaultLocaleContent.add("en_US");
            } else {
                defaultLocaleContent.add(bestLocale);
            }
        }

        for (String singleton : skippingSingletons) {
            BadItem.WARNING.show("skipping Singletons", singleton);
        }
        for (String missing : missingData) {
            BadItem.WARNING.show("Missing Data", missing);
        }

        // LanguageTagParser ltp = new LanguageTagParser();
        // Set<String> warnings = new LinkedHashSet();
        // for (String languageCode : languageToMaxCountry.keySet()) {
        // CodeAndPopulation best = languageToMaxCountry.get(languageCode);
        // String languageSubtag = ltp.set(languageCode).getLanguage();
        // String countryCode = "ZZ";
        // double rawLanguagePopulation = -1;
        // if (best != null) {
        // countryCode = best.code;
        // rawLanguagePopulation = best.population;
        // Set<String> regions = LanguageInfo.INSTANCE.languageToRegions.get(languageSubtag);
        // if (regions == null || !regions.contains(countryCode)) {
        // Set<String> regions2 = LanguageInfo.INSTANCE.languageToRegionsAlt.get(languageSubtag);
        // if (regions2 == null || !regions2.contains(countryCode)) {
        // warnings.add("WARNING: " + languageCode + " => " + countryCode + ", not in " + regions + "/" + regions2);
        // }
        // }
        // }
        // String resolvedLanguageCode = languageCode + "_" + countryCode;
        // ltp.set(languageCode);
        // Set<String> scripts = LanguageInfo.INSTANCE.languageToScripts.get(languageCode);
        // String script = ltp.getScript();
        // if (script.length() == 0) {
        // CodeAndPopulation bestScript = languageToMaxScript.get(languageCode);
        // if (bestScript != null) {
        // script = bestScript.code;
        // if (scripts == null || !scripts.contains(script)) {
        // warnings.add("WARNING: " + languageCode + " => " + script + ", not in " + scripts);
        // }
        // } else {
        // script = "Zzzz";
        // if (scripts == null) {
        // scripts = LanguageInfo.INSTANCE.languageToScriptsAlt.get(languageCode);
        // }
        // if (scripts != null) {
        // script = scripts.iterator().next();
        // if (scripts.size() != 1) {
        // warnings.add("WARNING: " + languageCode + " => " + scripts);
        // }
        // }
        // }
        // if (scripts == null) {
        // warnings.add("Missing scripts for: " + languageCode);
        // } else if (scripts.size() == 1){
        // script = "";
        // }
        // resolvedLanguageCode = languageCode
        // + (script.length() == 0 ? "" : "_" + script)
        // + "_" + countryCode;
        // }
        //
        //
        // System.out.println(
        // resolvedLanguageCode
        // + "\t" + languageCode
        // + "\t" + ULocale.getDisplayName(languageCode, ULocale.ENGLISH)
        // + "\t" + countryCode
        // + "\t" + ULocale.getDisplayCountry("und_" + countryCode, ULocale.ENGLISH)
        // + "\t" + formatNumber(rawLanguagePopulation)
        // + (cldrParents.contains(languageCode) ? "\tCLDR" : "")
        // );
        // if (languageCode.length() == 0) continue;
        // defaultContent.put(languageCode, resolvedLanguageCode);
        // }
        // for (String warning : warnings) {
        // System.out.println(warning);
        // }
    }

    // private static void printDefaultContent(Set<String> defaultLocaleContent) {
    // String sep = Utility.LINE_SEPARATOR + "\t\t\t";
    // String broken = Utility.breakLines(join(defaultLocaleContent," "), sep, PatternCache.get("(\\S)\\S*").matcher(""),
    // 80);
    //
    // Log.println("\t\t<defaultContent locales=\"" + broken + "\"");
    // Log.println("\t\t/>");
    // }

    private static Object getSuppressScript(String languageCode) {
        // TODO Auto-generated method stub
        return null;
    }

    public static String join(Collection c, String separator) {
        StringBuffer result = new StringBuffer();
        boolean first = true;
        for (Object x : c) {
            if (first)
                first = false;
            else
                result.append(separator);
            result.append(x);
        }
        return result.toString();
    }

    private static void addBestRegion(String languageCode, String countryCode, double languagePopulationRaw) {
        addBest(languageCode, languagePopulationRaw, countryCode, languageToMaxCountry);
    }

    private static void addBestScript(String languageCode, String scriptCode, double languagePopulationRaw) {
        addBest(languageCode, languagePopulationRaw, scriptCode, languageToMaxScript);
    }

    private static void addBest(String languageCode, double languagePopulationRaw, String code,
        Map<String, CodeAndPopulation> languageToMaxCode) {
        if (languageCode.length() == 0) {
            throw new IllegalArgumentException();
        }
        CodeAndPopulation best = languageToMaxCode.get(languageCode);
        if (best == null) {
            languageToMaxCode.put(languageCode, best = new CodeAndPopulation());
        } else if (best.population >= languagePopulationRaw) {
            return;
        }
        best.population = languagePopulationRaw;
        best.code = code;
    }

    static class CodeAndPopulation {
        String code = null;
        double population = Double.NaN;

        public String toString() {
            return "{" + code + "," + population + "}";
        }
    }

    static public class GeneralCollator implements Comparator<String> {
        static UTF16.StringComparator cpCompare = new UTF16.StringComparator(true, false, 0);
        static RuleBasedCollator UCA = (RuleBasedCollator) Collator
            .getInstance(ULocale.ROOT);
        static {
            UCA.setNumericCollation(true);
        }

        public int compare(String s1, String s2) {
            if (s1 == null) {
                return s2 == null ? 0 : -1;
            } else if (s2 == null) {
                return 1;
            }
            int result = UCA.compare(s1, s2);
            if (result != 0) return result;
            return cpCompare.compare(s1, s2);
        }
    };

    public static class InverseComparator<T> implements Comparator<T> {
        private Comparator<T> other;

        public InverseComparator() {
            this.other = null;
        }

        public InverseComparator(Comparator<T> other) {
            this.other = other;
        }

        public int compare(T a, T b) {
            return other == null
                ? ((Comparable) b).compareTo(a)
                : other.compare(b, a);
        }
    }

    static Set<String> languagesNeeded = new TreeSet<String>(
        Arrays
            .asList("ab ba bh bi bo fj fy gd ha ht ik iu ks ku ky lg mi na nb rm sa sd sg si sm sn su tg tk to tw vo yi za lb dv chr syr kha sco gv"
                .split("\\s")));

    static void generateIso639_2Data() {
        for (String languageSubtag : sc.getAvailableCodes("language")) {
            String alpha3 = Iso639Data.toAlpha3(languageSubtag);
            Type type = Iso639Data.getType(languageSubtag);
            Scope scope = Iso639Data.getScope(languageSubtag);
            if (type != null || alpha3 != null || scope != null) {
                Log.println("\t\t<languageCode type=\"" + languageSubtag + "\"" +
                    (alpha3 == null ? "" : " iso639Alpha3=\"" + alpha3 + "\"") +
                    (type == null ? "" : " iso639Type=\"" + type + "\"") +
                    (scope == null ? "" : " iso639Scope=\"" + scope + "\"") +
                    "/>");
            }

        }
    }

    static Relation<String, BasicLanguageData> language2BasicLanguageData = Relation.of(new TreeMap<String, Set<BasicLanguageData>>(), TreeSet.class);

    static Map<String, Relation<BasicLanguageData.Type, String>> language_status_scripts;
    static Map<Pair<String, String>, String> language_script_references = new TreeMap<Pair<String, String>, String>();

    static final Map<String, Map<String, R2<List<String>, String>>> LOCALE_ALIAS_INFO = SupplementalDataInfo
        .getInstance().getLocaleAliasInfo();

    static void getLanguage2Scripts(Set<RowData> sortedInput) throws IOException {
        language_status_scripts = new TreeMap<String, Relation<BasicLanguageData.Type, String>>();

        // // get current scripts
        // Relation<String,String> languageToDefaultScript = new Relation(new TreeMap(), TreeSet.class);
        // Relation<String,String> secondaryLanguageToDefaultScript = new Relation(new TreeMap(), TreeSet.class);
        // for (String languageSubtag : language2BasicLanguageData.keySet()) {
        // for (BasicLanguageData item : language2BasicLanguageData.getAll(languageSubtag)) {
        // for (String script : item.getScripts()) {
        // addLanguage2Script(languageSubtag, item.getType(), script);
        // }
        // }
        // }
        // System.out.println("Language 2 scripts: " + language_status_scripts);

        // #Lcode LanguageName Status Scode ScriptName References
        List<List<String>> input = SpreadSheet.convert(CldrUtility.getUTF8Data("language_script_raw.txt"));
        System.out.println(CldrUtility.LINE_SEPARATOR + "# Problems in language_script_raw.txt"
            + CldrUtility.LINE_SEPARATOR);
        //int count = -1;
        for (List<String> row : input) {
            try {
                if (row.size() == 0) continue;
                //++count;
                String language = row.get(0).trim();
                if (language.length() == 0 || language.startsWith("#")) continue;
                BasicLanguageData.Type status = BasicLanguageData.Type.valueOf(row.get(2));
                String scripts = row.get(3);
                if (!checkCode(LstrType.language, language, row)) continue;
                for (String script : scripts.split("\\s+")) {
                    if (!checkCode(LstrType.script, script, row)) continue;
                    // if the script is not modern, demote
                    Info scriptInfo = ScriptMetadata.getInfo(script);
                    if (scriptInfo == null) {
                        BadItem.ERROR.toString("illegal script; must be represented in Unicode, remove line or fix", script, row);
                        continue;
                    }
                    IdUsage idUsage = scriptInfo.idUsage;
                    if (status == BasicLanguageData.Type.primary && idUsage != IdUsage.RECOMMENDED) {
                        if (idUsage == IdUsage.ASPIRATIONAL || idUsage == IdUsage.LIMITED_USE) {
                            BadItem.WARNING.toString("Script has unexpected usage; make secondary if a Recommended script is used widely for the langauge",
                                idUsage + ", " + script + "=" + getULocaleScriptName(script), row);
                        } else {
                            BadItem.ERROR.toString("Script is not modern; make secondary", idUsage + ", " + script + "=" + getULocaleScriptName(script), row);
                            status = BasicLanguageData.Type.secondary;
                        }
                    }

                    // if the language is not modern, demote
                    if (LOCALE_ALIAS_INFO.get("language").containsKey(language)) {
                        BadItem.ERROR.toString("Remove/Change deprecated language", language + " "
                            + getLanguageName(language) + "; " + LOCALE_ALIAS_INFO.get("language").get(language), row);
                        continue;
                    }
                    if (status == BasicLanguageData.Type.primary && !sc.isModernLanguage(language)) {
                        BadItem.ERROR.toString("Should be secondary, language is not modern", language + " " + getLanguageName(language), row);
                        status = BasicLanguageData.Type.secondary;
                    }

                    addLanguage2Script(language, status, script);
                    if (row.size() > 5) {
                        String reference = row.get(5);
                        if (reference != null && reference.length() == 0) {
                            language_script_references.put(new Pair<String, String>(language, script), reference);
                        }
                    }
                }
            } catch (RuntimeException e) {
                System.err.println(row);
                throw e;
            }
        }

        // System.out.println("Language 2 scripts: " + language_status_scripts);

        for (String language : sc.getGoodAvailableCodes("language")) {
            if (supplementalData.getDeprecatedInfo("language", language) != null) {
                continue;
            }
            Map<String, String> registryData = sc.getLangData("language", language);
            if (registryData != null) {
                String suppressScript = registryData.get("Suppress-Script");
                if (suppressScript == null) continue;
                if (ScriptMetadata.getInfo(suppressScript) == null) {
                    // skip, not represented in Unicode
                    continue;
                }
                // if there is something already there, we have a problem.
                Relation<BasicLanguageData.Type, String> status_scripts = language_status_scripts.get(language);
                if (status_scripts == null) {
                    System.out
                        .println("Missing Suppress-Script: " + language + "\tSuppress-Script:\t" + suppressScript);
                } else if (!status_scripts.values().contains(suppressScript)) {
                    System.out.println("Missing Suppress-Script: " + language + "\tSuppress-Script:\t" + suppressScript
                        + "\tall:\t" + status_scripts.values());
                } else {
                    // at this point, the suppressScript is in the union of the primary and secondary.
                    Set<String> primaryScripts = status_scripts.getAll(BasicLanguageData.Type.primary);
                    if (primaryScripts != null && !primaryScripts.contains(suppressScript)) {
                        System.out.println("Suppress-Script is not in primary: " + language + "\tSuppress-Script:\t"
                            + suppressScript + "\tprimary:\t"
                            + primaryScripts);
                    }
                }
                addLanguage2Script(language, BasicLanguageData.Type.primary, suppressScript);
            }
        }

        // remove primaries from secondaries
        // check for primaries for scripts
        for (String language : language_status_scripts.keySet()) {
            Relation<BasicLanguageData.Type, String> status_scripts = language_status_scripts.get(language);
            Set<String> secondaryScripts = status_scripts.getAll(BasicLanguageData.Type.secondary);
            if (secondaryScripts == null) continue;
            Set<String> primaryScripts = status_scripts.getAll(BasicLanguageData.Type.primary);
            if (primaryScripts == null) {
                // status_scripts.putAll(BasicLanguageData.Type.primary, secondaryScripts);
                // status_scripts.removeAll(BasicLanguageData.Type.secondary);
                if (sc.isModernLanguage(language)) {
                    BadItem.ERROR.show("modern language without primary script, might need to edit moribund_languages.txt", language + " "
                        + getLanguageName(language));
                }
            } else {
                status_scripts.removeAll(BasicLanguageData.Type.secondary, primaryScripts);
            }
        }

        // check that every living language in the row data has a script
        Set<String> livingLanguagesWithTerritories = new TreeSet<String>();
        for (RowData rowData : sortedInput) {
            String language = rowData.languageCode;
            if (sc.isModernLanguage(language) && Iso639Data.getSource(language) != Iso639Data.Source.ISO_639_3) {
                livingLanguagesWithTerritories.add(language);
            }
        }
        for (String language : livingLanguagesWithTerritories) {
            Relation<BasicLanguageData.Type, String> status_scripts = language_status_scripts.get(language);
            if (status_scripts != null) {
                Set<String> primaryScripts = status_scripts.getAll(BasicLanguageData.Type.primary);
                if (primaryScripts != null && primaryScripts.size() > 0) {
                    continue;
                }
            }
            if (language.equals("tw")) continue; // TODO load aliases and check...
            BadItem.WARNING.show("ISO 639-1/2 language in language-territory list without primary script", language + "\t" + getLanguageName(language));
        }

        // System.out.println("Language 2 scripts: " + language_status_scripts);
    }

    private static boolean checkScript(String script) {
        // TODO Auto-generated method stub
        return false;
    }

    static Validity VALIDITY = Validity.getInstance();

    private static boolean checkCode(LstrType type, String code, List<String> sourceLine) {
        Status validity = VALIDITY.getCodeToStatus(type).get(code);
        if (validity == Status.regular) {
            if (type == LstrType.language && code.equals("no")) {
                validity = Status.invalid;
            } else {
                return true;
            }
        } else if (validity == Status.unknown && type == LstrType.region) {
            return true;
        }
        BadItem.ERROR.show("Illegitimate Code", type + ": " + code + " = " + validity, sourceLine);
        return false;
    }

    private static void addLanguage2Script(String language, BasicLanguageData.Type type, String script) {
        Relation<BasicLanguageData.Type, String> status_scripts = language_status_scripts.get(language);
        if (status_scripts == null)
            language_status_scripts.put(language, status_scripts = Relation.of(new TreeMap<BasicLanguageData.Type, Set<String>>(), TreeSet.class));
        status_scripts.put(type, script);
    }

    static void addLanguageScriptData() throws IOException {
        // check to make sure that every language subtag is in 639-3
        Set<String> langRegistryCodes = sc.getGoodAvailableCodes("language");
        // Set<String> iso639_2_missing = new TreeSet(langRegistryCodes);
        // iso639_2_missing.removeAll(Iso639Data.getAvailable());
        // iso639_2_missing.remove("root");
        // if (iso639_2_missing.size() != 0) {
        // for (String missing : iso639_2_missing){
        // System.out.println("*ERROR in StandardCodes* Missing Lang/Script data:\t" + missing + ", " +
        // sc.getData("language", missing));
        // }
        // }

        // Map<String, String> nameToTerritoryCode = new TreeMap();
        // for (String territoryCode : sc.getGoodAvailableCodes("territory")) {
        // nameToTerritoryCode.put(sc.getData("territory", territoryCode).toLowerCase(), territoryCode);
        // }
        // nameToTerritoryCode.put("iran", nameToTerritoryCode.get("iran, islamic republic of")); //

        //BasicLanguageData languageData = new BasicLanguageData();

        BufferedReader in = CldrUtility.getUTF8Data("extraLanguagesAndScripts.txt");
        while (true) {
            String line = in.readLine();
            if (line == null) break;
            String[] parts = line.split("\\t");
            String alpha3 = parts[0];
            alpha3 = stripBrackets(alpha3);
            String languageSubtag = Iso639Data.fromAlpha3(alpha3);
            if (languageSubtag == null) {
                if (langRegistryCodes.contains(alpha3)) {
                    languageSubtag = alpha3;
                } else {
                    BadItem.WARNING.show("Language subtag not found on line", alpha3, line);
                    continue;
                }
            }
            //String name = parts[1];
            Set<String> names = Iso639Data.getNames(languageSubtag);
            if (names == null) {
                Map<String, String> name2 = sc.getLangData("language", languageSubtag);
                if (name2 != null) {
                    String name3 = name2.get("Description");
                    if (name3 != null) {
                        names = new TreeSet<String>();
                        names.add(name3);
                    }
                }
            }
            // if (names == null || !names.contains(name)) {
            // System.out.println("Name <" + name + "> for <" + languageSubtag + "> not found in " + names);
            // }

            // names all straight, now get scripts and territories
            // [Cyrl]; [Latn]
            Set<String> fullScriptList = sc.getGoodAvailableCodes("script");

            String[] scriptList = parts[2].split("[;,]\\s*");
            Set<String> scripts = new TreeSet<String>();
            Set<String> scriptsAlt = new TreeSet<String>();
            for (String script : scriptList) {
                if (script.length() == 0) continue;
                boolean alt = false;
                if (script.endsWith("*")) {
                    alt = true;
                    script = script.substring(0, script.length() - 1);
                }
                script = stripBrackets(script);
                if (!fullScriptList.contains(script)) {
                    System.out.println("Script <" + script + "> for <" + languageSubtag + "> not found in "
                        + fullScriptList);
                } else if (alt) {
                    scriptsAlt.add(script);
                } else {
                    scripts.add(script);
                }
            }
            // now territories
            Set<String> territories = new TreeSet<String>();
            if (parts.length > 4) {
                String[] territoryList = parts[4].split("\\s*[;,-]\\s*");
                for (String territoryName : territoryList) {
                    if (territoryName.equals("ISO/DIS 639") || territoryName.equals("3")) continue;
                    String territoryCode = CountryCodeConverter.getCodeFromName(territoryName);
                    if (territoryCode == null) {
                        BadItem.ERROR.show("no name found for territory", "<" + territoryName + ">", languageSubtag);
                    } else {
                        territories.add(territoryCode);
                    }
                }
            }
            // <language type="de" scripts="Latn" territories="IT" alt="secondary"/>
            // we're going to go ahead and set these all to secondary.
            if (scripts.size() != 0) {
                language2BasicLanguageData.put(languageSubtag,
                    new BasicLanguageData().setType(BasicLanguageData.Type.secondary).setScripts(scripts)
                        .setTerritories(territories));
            }
            if (scriptsAlt.size() != 0) {
                language2BasicLanguageData.put(languageSubtag,
                    new BasicLanguageData().setType(BasicLanguageData.Type.secondary).setScripts(scriptsAlt)
                        .setTerritories(territories));
            }
        }
        in.close();

        // add other data
        for (String languageSubtag : supplementalData.getBasicLanguageDataLanguages()) {
            Set<BasicLanguageData> otherData = supplementalData.getBasicLanguageData(languageSubtag);
            language2BasicLanguageData.putAll(languageSubtag, otherData);
        }
    }

    // private static void showAllBasicLanguageData(Relation<String, BasicLanguageData> language2basicData, String
    // comment) {
    // // now print
    // Relation<String, String> primaryCombos = new Relation(new TreeMap(), TreeSet.class);
    // Relation<String, String> secondaryCombos = new Relation(new TreeMap(), TreeSet.class);
    //
    // Log.println("\t<languageData>" + (comment == null ? "" : " <!-- " + comment + " -->"));
    //
    // for (String languageSubtag : language2basicData.keySet()) {
    // String duplicate = "";
    // // script,territory
    // primaryCombos.clear();
    // secondaryCombos.clear();
    //
    // for (BasicLanguageData item : language2basicData.getAll(languageSubtag)) {
    // Set<String> scripts = item.getScripts();
    // if (scripts.size() == 0) scripts = new TreeSet(Arrays.asList(new String[] { "Zzzz" }));
    // for (String script : scripts) {
    // Set<String> territories = item.getTerritories();
    // if (territories.size() == 0) territories = new TreeSet(Arrays.asList(new String[] { "ZZ" }));
    // for (String territory : territories) {
    // if (item.getType().equals(BasicLanguageData.Type.primary)) {
    // primaryCombos.put(script, territory);
    // } else {
    // secondaryCombos.put(script, territory);
    // }
    // }
    // }
    // }
    // secondaryCombos.removeAll(primaryCombos);
    // showBasicLanguageData(languageSubtag, primaryCombos, null, BasicLanguageData.Type.primary);
    // showBasicLanguageData(languageSubtag, secondaryCombos, primaryCombos.keySet(),
    // BasicLanguageData.Type.secondary);
    // // System.out.println(item.toString(languageSubtag) + duplicate);
    // // duplicate = " <!-- " + "**" + " -->";
    // }
    // Log.println("\t</languageData>");
    // }

    private static void showBasicLanguageData(String languageSubtag, Relation<String, String> primaryCombos,
        Set<String> suppressEmptyScripts, BasicLanguageData.Type type) {
        Set<String> scriptsWithSameTerritories = new TreeSet<String>();
        Set<String> lastTerritories = Collections.emptySet();
        for (String script : primaryCombos.keySet()) {
            Set<String> territories = primaryCombos.getAll(script);
            if (lastTerritories == Collections.EMPTY_SET) {
                // skip first
            } else if (lastTerritories.equals(territories)) {
                scriptsWithSameTerritories.add(script);
            } else {
                showBasicLanguageData2(languageSubtag, scriptsWithSameTerritories, suppressEmptyScripts,
                    lastTerritories, type);
                scriptsWithSameTerritories.clear();
            }
            lastTerritories = territories;
            scriptsWithSameTerritories.add(script);
        }
        showBasicLanguageData2(languageSubtag, scriptsWithSameTerritories, suppressEmptyScripts, lastTerritories, type);
    }

    private static void showBasicLanguageData2(String languageSubtag, Set<String> scripts,
        Set<String> suppressEmptyScripts, Set<String> territories, BasicLanguageData.Type type) {
        scripts.remove("Zzzz");
        territories.remove("ZZ");
        if (territories.size() == 0 && suppressEmptyScripts != null) {
            scripts.removeAll(suppressEmptyScripts);
        }
        if (scripts.size() == 0 && territories.size() == 0) return;
        Log.println("\t\t<language type=\"" + languageSubtag + "\"" +
            (scripts.size() == 0 ? "" : " scripts=\"" + CldrUtility.join(scripts, " ") + "\"") +
            (territories.size() == 0 ? "" : " territories=\"" + CldrUtility.join(territories, " ") + "\"") +
            (type == BasicLanguageData.Type.primary ? "" : " alt=\"" + type + "\"") +
            "/>");
    }

    /*
     * System.out.println(
     * "\t\t<language type=\"" + languageSubtag + "\"" +
     * " scripts=\"" + Utility.join(scripts," ") + "\"" +
     * (territories.size() == 0 ? "" : " territories=\"" + Utility.join(territories," ") + "\"") +
     * "/>"
     * );
     */

    private static String stripBrackets(String alpha3) {
        if (alpha3.startsWith("[") && alpha3.endsWith("]")) {
            alpha3 = alpha3.substring(1, alpha3.length() - 1);
        }
        return alpha3;
    }

    static NumberFormat nf = NumberFormat.getInstance(ULocale.ENGLISH);
    static NumberFormat nf_no_comma = NumberFormat.getInstance(ULocale.ENGLISH);
    static {
        nf_no_comma.setGroupingUsed(false);
    }
    static NumberFormat pf = NumberFormat.getPercentInstance(ULocale.ENGLISH);

    public static String formatNumber(double original, int roundDigits, boolean xml) {
        double d = original;
        if (roundDigits != 0) {
            d = CldrUtility.roundToDecimals(original, roundDigits);
        }
        if (Double.isNaN(d)) {
            d = CldrUtility.roundToDecimals(original, roundDigits);
            throw new IllegalArgumentException("Double is NaN");
        }
        if (xml) {
            return nf_no_comma.format(d);
        }
        return nf.format(d);
    }

    public static String formatPercent(double d, int roundDigits, boolean xml) {
        if (roundDigits != 0) {
            d = CldrUtility.roundToDecimals(d, roundDigits);
        }
        if (xml) {
            nf_no_comma.setMaximumFractionDigits(roundDigits + 2);
            return nf_no_comma.format(d * 100.0);
        }
        pf.setMaximumFractionDigits(roundDigits + 2);
        return pf.format(d);
    }

    static final LanguageTagCanonicalizer languageTagCanonicalizer = new LanguageTagCanonicalizer();

    private static String fixLanguageCode(String languageCodeRaw, List<String> row) {
        String languageCode = languageTagCanonicalizer.transform(languageCodeRaw);
        if (DEBUG && !languageCode.equals(languageCodeRaw)) {
            System.out.println("## " + languageCodeRaw + " => " + languageCode);
        }
        int bar = languageCode.indexOf('_');
        String script = "";
        if (bar >= 0) {
            script = languageCode.substring(bar);
            languageCode = languageCode.substring(0, bar);
        }
        R2<List<String>, String> replacement = supplementalData.getLocaleAliasInfo().get("language").get(languageCode);
        if (replacement != null) {
            String replacementCode = replacement.get0().get(0);
            BadItem.ERROR.show("deprecated language code", languageCode + " => " + replacementCode, row);
            languageCode = replacementCode;
        }
        if (!sc.getAvailableCodes("language").contains(languageCode)) {
            BadItem.ERROR.show("bad language code", languageCode, row);
        }
        return languageCode + script;
    }

    enum BadItem {
        ERROR, WARNING, DETAIL;

        void show(String problem, String details, String... items) {
            System.out.println(toString(problem, details, items));
        }

        void show(String problem, String details, List<String> row) {
            System.out.println(toString(problem, details, row));
        }

        private String toString(String problem, String details, String... items) {
            return toString(problem, details, Arrays.asList(items));
        }

        private String toString(String problem, String details, List<String> row) {
            return "* " + this
                + " *\t" + problem + ":"
                + "\t" + details
                + (row != null && row.size() > 0 ? "\t" + CollectionUtilities.join(row, "\t") : "");
        }
    }

    private static String fixCountryCode(String countryCode, List<String> row) {
        R2<List<String>, String> replacement = supplementalData.getLocaleAliasInfo().get("territory").get(countryCode);
        if (replacement != null) {
            String replacementCode = replacement.get0().get(0);
            BadItem.ERROR.show("deprecated territory code", countryCode + " => " + replacementCode, row);
            countryCode = replacementCode;
        }
        if (!sc.getAvailableCodes("territory").contains(countryCode)) {
            BadItem.ERROR.show("bad territory code", countryCode, row);
        }
        return countryCode;
    }

    private static String getULocaleLocaleName(String languageCode) {
        return english.getName(languageCode, true);
        //return new ULocale(languageCode).getDisplayName();
    }

    private static String getULocaleScriptName(String scriptCode) {
        return english.getName(CLDRFile.SCRIPT_NAME, scriptCode);
        // return ULocale.getDisplayScript("und_" + scriptCode, ULocale.ENGLISH);
    }

    private static String getULocaleCountryName(String countryCode) {
        return english.getName(CLDRFile.TERRITORY_NAME, countryCode);
        //return ULocale.getDisplayCountry("und_" + countryCode, ULocale.ENGLISH);
    }
}
