package org.unicode.cldr.util;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.Builder.CBuilder;
import org.unicode.cldr.util.DayPeriodInfo.DayPeriod;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;

import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.impl.IterableComparator;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Freezable;
import com.ibm.icu.util.ULocale;

/**
 * Singleton class to provide API access to supplemental data -- in all the supplemental data files.
 * <p>To create, use SupplementalDataInfo.getInstance
 * <p>To add API for new structure, you will generally:
 * <ul><li>add a Map or Relation as a data member,
 * <li>put a check and handler in MyHandler for the paths that you consume, 
 * <li>make the data member immutable in makeStuffSave, and
 * <li>add a getter for the data member
 * </ul>
 * @author markdavis
 */

public class SupplementalDataInfo {
    private static final boolean DEBUG = false;

    //TODO add structure for items shown by TestSupplementalData to be missing
    /*[calendarData/calendar, 
     * characters/character-fallback, 
     * measurementData/measurementSystem, measurementData/paperSize, 
     * metadata/attributeOrder, metadata/blocking, metadata/coverageAdditions, metadata/deprecated, metadata/distinguishing, metadata/elementOrder, metadata/serialElements, metadata/skipDefaultLocale, metadata/suppress, metadata/validity, metazoneInfo/timezone, 
     * timezoneData/mapTimezones, 
     * weekData/firstDay, weekData/minDays, weekData/weekendEnd, weekData/weekendStart]
     */
    // TODO: verify that we get everything by writing the files solely from the API, and verifying identity.

    /**
     * Official status of languages
     */
    public enum OfficialStatus {
        unknown("U", 1),
        recognized("R", 1),
        official_minority("OM", 2),
        official_regional("OR", 3), 
        de_facto_official("OD", 10),
        official("O", 10);

        private final String shortName;
        private final int    weight;

        private OfficialStatus(String shortName, int weight) {
            this.shortName = shortName;
            this.weight = weight;
        }

        public String toShortString() {
            return shortName;
        }

        public int getWeight() {
            return weight;
        }

        public boolean isMajor() {
            return compareTo(OfficialStatus.de_facto_official) >= 0;
        }

        public boolean isOfficial() {
            return compareTo(OfficialStatus.official_regional) >= 0;
        }
    };

    /**
     * Population data for different languages.
     */
    public static final class PopulationData implements Freezable {
        private double population = Double.NaN;

        private double literatePopulation = Double.NaN;

        private double gdp = Double.NaN;

        private OfficialStatus officialStatus = OfficialStatus.unknown;

        public double getGdp() {
            return gdp;
        }

        public double getLiteratePopulation() {
            return literatePopulation;
        }

        public double getPopulation() {
            return population;
        }

        public PopulationData setGdp(double gdp) {
            if (frozen) {
                throw new UnsupportedOperationException(
                "Attempt to modify frozen object");
            }
            this.gdp = gdp;
            return this;
        }

        public PopulationData setLiteratePopulation(double literatePopulation) {
            if (frozen) {
                throw new UnsupportedOperationException(
                "Attempt to modify frozen object");
            }
            this.literatePopulation = literatePopulation;
            return this;
        }

        public PopulationData setPopulation(double population) {
            if (frozen) {
                throw new UnsupportedOperationException(
                "Attempt to modify frozen object");
            }
            this.population = population;
            return this;
        }

        public PopulationData set(PopulationData other) {
            if (frozen) {
                throw new UnsupportedOperationException(
                "Attempt to modify frozen object");
            }
            if (other == null) {
                population = literatePopulation = gdp = Double.NaN;
            } else {
                population = other.population;
                literatePopulation = other.literatePopulation;
                gdp = other.gdp;
            }
            return this;
        }

        public void add(PopulationData other) {
            if (frozen) {
                throw new UnsupportedOperationException(
                "Attempt to modify frozen object");
            }
            population += other.population;
            literatePopulation += other.literatePopulation;
            gdp += other.gdp;
        }

        public String toString() {
            return MessageFormat
            .format(
                    "[pop: {0,number,#,##0},\t lit: {1,number,#,##0.00},\t gdp: {2,number,#,##0},\t status: {3}]",
                    new Object[] { population, literatePopulation, gdp, officialStatus});
        }

        private boolean frozen;

        public boolean isFrozen() {
            return frozen;
        }

        public Object freeze() {
            frozen = true;
            return this;
        }

        public Object cloneAsThawed() {
            throw new UnsupportedOperationException("not yet implemented");
        }

        public OfficialStatus getOfficialStatus() {
            return officialStatus;
        }

        public PopulationData setOfficialStatus(OfficialStatus officialStatus) {
            this.officialStatus = officialStatus;
            return this;
        }
    }

    static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");


    /**
     * Simple language/script/region information
     */
    public static class BasicLanguageData implements
    Comparable<BasicLanguageData>, Freezable {
        public enum Type {
            primary, secondary
        };

        private Type type = Type.primary;

        private Set<String> scripts = Collections.EMPTY_SET;

        private Set<String> territories = Collections.EMPTY_SET;

        public Type getType() {
            return type;
        }

        public BasicLanguageData setType(Type type) {
            this.type = type;
            return this;
        }

        public BasicLanguageData setScripts(String scriptTokens) {
            return setScripts(scriptTokens == null ? null : Arrays
                    .asList(WHITESPACE_PATTERN.split(scriptTokens)));
        }

        public BasicLanguageData setTerritories(String territoryTokens) {
            return setTerritories(territoryTokens == null ? null : Arrays
                    .asList(WHITESPACE_PATTERN.split(territoryTokens)));
        }

        public BasicLanguageData setScripts(Collection<String> scriptTokens) {
            if (frozen) {
                throw new UnsupportedOperationException();
            }
            // TODO add error checking
            scripts = Collections.EMPTY_SET;
            if (scriptTokens != null) {
                for (String script : scriptTokens) {
                    addScript(script);
                }
            }
            return this;
        }

        public BasicLanguageData setTerritories(Collection<String> territoryTokens) {
            if (frozen) {
                throw new UnsupportedOperationException();
            }
            territories = Collections.EMPTY_SET;
            if (territoryTokens != null) {
                for (String territory : territoryTokens) {
                    addTerritory(territory);
                }
            }
            return this;
        }

        public BasicLanguageData set(BasicLanguageData other) {
            scripts = other.scripts;
            territories = other.territories;
            return this;
        }

        public Set<String> getScripts() {
            return scripts;
        }

        public Set<String> getTerritories() {
            return territories;
        }

        public String toString(String languageSubtag) {
            if (scripts.size() == 0 && territories.size() == 0)
                return "";
            return "\t\t<language type=\""
            + languageSubtag
            + "\""
            + (scripts.size() == 0 ? "" : " scripts=\""
                + CldrUtility.join(scripts, " ") + "\"")
                + (territories.size() == 0 ? "" : " territories=\""
                    + CldrUtility.join(territories, " ") + "\"")
                    + (type == Type.primary ? "" : " alt=\"" + type + "\"") + "/>";
        }

        public int compareTo(BasicLanguageData o) {
            int result;
            if (0 != (result = type.compareTo(o.type)))
                return result;
            if (0 != (result = IterableComparator.compareIterables(scripts, o.scripts)))
                return result;
            if (0 != (result = IterableComparator.compareIterables(territories, o.territories)))
                return result;
            return 0;
        }

        public BasicLanguageData addScript(String script) {
            // simple error checking
            if (script.length() != 4) {
                throw new IllegalArgumentException("Illegal Script: " + script);
            }
            if (scripts == Collections.EMPTY_SET) {
                scripts = new TreeSet();
            }
            scripts.add(script);
            return this;
        }

        public BasicLanguageData addTerritory(String territory) {
            // simple error checking
            if (territory.length() != 2) {
                throw new IllegalArgumentException("Illegal Territory: " + territory);
            }
            if (territories == Collections.EMPTY_SET) {
                territories = new TreeSet();
            }
            territories.add(territory);
            return this;
        }

        boolean frozen = false;

        public boolean isFrozen() {
            // TODO Auto-generated method stub
            return frozen;
        }

        public Object freeze() {
            frozen = true;
            if (scripts != Collections.EMPTY_SET) {
                scripts = Collections.unmodifiableSet(scripts);
            }
            if (territories != Collections.EMPTY_SET) {
                territories = Collections.unmodifiableSet(territories);
            }
            return this;
        }

        public Object cloneAsThawed() {
            throw new UnsupportedOperationException();
        }

        public void addScripts(Set<String> scripts2) {
            for (String script : scripts2) {
                addScript(script);
            }
        }
    }

    /**
     * Information about currency digits and rounding.
     */
    public static class CurrencyNumberInfo {
        int digits;
        int rounding;
        double roundingIncrement;

        public int getDigits() {
            return digits;
        }
        public int getRounding() {
            return rounding;
        }
        public double getRoundingIncrement() {
            return roundingIncrement;
        }
        public CurrencyNumberInfo(int digits, int rounding) {
            this.digits = digits;
            this.rounding = rounding;
            roundingIncrement = rounding * Math.pow(10.0, -digits);
        }
    }

    public static class NumberingSystemsInfo {
        public Set<String> getAvailableNumberingSystems() {
            return null;
        }
    }
    /**
     * Information about when currencies are in use in territories
     */
    public static class CurrencyDateInfo implements Comparable<CurrencyDateInfo> {
        public static final Date END_OF_TIME = new Date(Long.MAX_VALUE);
        public static final Date START_OF_TIME = new Date(Long.MIN_VALUE);
        private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        private String currency;
        private Date start;
        private Date end;
        private boolean isLegalTender;
        private String errors = "";

        public CurrencyDateInfo(String currency, String startDate, String endDate, boolean isLegalTender) {
            this.currency = currency;
            start = parseDate(startDate, START_OF_TIME);
            end = parseDate(endDate, END_OF_TIME);
            this.isLegalTender = isLegalTender;
        }

        static DateFormat[] simpleFormats = { 
            new SimpleDateFormat("yyyy-MM-dd"),
            new SimpleDateFormat("yyyy-MM"), 
            new SimpleDateFormat("yyyy"), };

        Date parseDate(String dateString, Date defaultDate) {
            if (dateString == null) {
                return defaultDate;
            }
            ParseException e2 = null;
            for (int i = 0; i < simpleFormats.length; ++i) {
                try {
                    Date result = simpleFormats[i].parse(dateString);
                    return result;
                } catch (ParseException e) {
                    if (i == 0) {
                        errors += dateString + " ";
                    }
                    if (e2 == null) {
                        e2 = e;
                    }
                }
            }
            throw (IllegalArgumentException)new IllegalArgumentException().initCause(e2);  
        }

        public String getCurrency() {
            return currency;
        }

        public Date getStart() {
            return start;
        }

        public Date getEnd() {
            return end;
        }

        public String getErrors() {
            return errors;
        }

        public boolean isLegalTender() {
            return isLegalTender;
        }

        public int compareTo(CurrencyDateInfo o) {
            int result = start.getDate() - o.start.getDate();
            if (result != 0) return result;
            result = end.getDate() - o.end.getDate();
            if (result != 0) return result;
            return currency.compareTo(o.currency);
        }

        public String toString() {
            return "{" + formatDate(start) + ", " + formatDate(end) + ", " + currency + "}";
        }

        public static String formatDate(Date date) {
            if (date.equals(START_OF_TIME)) return "-∞";
            if (date.equals(END_OF_TIME)) return "∞";
            return dateFormat.format(date);
        }
    }

    /**
     * Information about telephone code(s) for a given territory
     */
    public static class TelephoneCodeInfo implements Comparable<TelephoneCodeInfo> {
        public static final Date END_OF_TIME = new Date(Long.MAX_VALUE);
        public static final Date START_OF_TIME = new Date(Long.MIN_VALUE);
        private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        private String code;
        private Date start;
        private Date end;
        private String alt;
        private String errors = "";

        // code must not be null, the others can be
        public TelephoneCodeInfo(String code, String startDate, String endDate, String alt) {
            if (code == null)
                throw new NullPointerException();
            this.code = code;                                 // code will not be null
            this.start = parseDate(startDate, START_OF_TIME); // start will not be null
            this.end = parseDate(endDate, END_OF_TIME);       // end willl not be null
            this.alt = (alt == null)? "": alt;                // alt will not be null
        }

        static DateFormat[] simpleFormats = { 
            new SimpleDateFormat("yyyy-MM-dd"),
            new SimpleDateFormat("yyyy-MM"), 
            new SimpleDateFormat("yyyy"), };

        Date parseDate(String dateString, Date defaultDate) {
            if (dateString == null) {
                return defaultDate;
            }
            ParseException e2 = null;
            for (int i = 0; i < simpleFormats.length; ++i) {
                try {
                    Date result = simpleFormats[i].parse(dateString);
                    return result;
                } catch (ParseException e) {
                    if (i == 0) {
                        errors += dateString + " ";
                    }
                    if (e2 == null) {
                        e2 = e;
                    }
                }
            }
            throw (IllegalArgumentException)new IllegalArgumentException().initCause(e2);  
        }

        public String getCode() {
            return code;
        }

        public Date getStart() {
            return start;
        }

        public Date getEnd() {
            return end;
        }

        public String getAlt() {
            return alt; // may return null
        }

        public String getErrors() {
            return errors;
        }

        public boolean equals(Object o) {
            if (!(o instanceof TelephoneCodeInfo))
                return false;
            TelephoneCodeInfo tc = (TelephoneCodeInfo)o;
            return tc.code.equals(code) && tc.start.equals(start) && tc.end.equals(end) && tc.alt.equals(alt);
        }

        public int hashCode() {
            return 31*code.hashCode() + start.hashCode() + end.hashCode() + alt.hashCode();
        }

        public int compareTo(TelephoneCodeInfo o) {
            int result = code.compareTo(o.code);
            if (result != 0) return result;
            result = start.compareTo(o.start);
            if (result != 0) return result;
            result = end.compareTo(o.end);
            if (result != 0) return result;
            return alt.compareTo(o.alt);
        }

        public String toString() {
            return "{" + code + ", " + formatDate(start) + ", " + formatDate(end) + ", " + alt + "}";
        }

        public static String formatDate(Date date) {
            if (date.equals(START_OF_TIME)) return "-∞";
            if (date.equals(END_OF_TIME)) return "∞";
            return dateFormat.format(date);
        }
    }
    public static class CoverageLevelInfo implements Comparable<CoverageLevelInfo> {
        private String match;
        private Integer value;
        private String inLanguage;
        private String inScript;
        private String inTerritory;
        public CoverageLevelInfo(String match, Integer value, String language, String script, String territory) {
            this.inLanguage = language;
            this.inScript = script;
            this.inTerritory = territory;
            this.match = match;
            this.value = value;
        }

        public int compareTo(CoverageLevelInfo o) {
            if (value.equals(o.value)) {
                return match.compareTo(o.match);
            } else {
                return value.compareTo(o.value);
            }
        }
    }

    private Map<String, PopulationData> territoryToPopulationData = new TreeMap();

    private Map<String, Map<String, PopulationData>> territoryToLanguageToPopulationData = new TreeMap();

    private Map<String, PopulationData> languageToPopulation = new TreeMap();

    private Map<String, PopulationData> baseLanguageToPopulation = new TreeMap();

    private Relation<String, String> languageToScriptVariants = new Relation(new TreeMap(), TreeSet.class);

    private Relation<String, String> languageToTerritories = new Relation(new TreeMap(), LinkedHashSet.class);

    transient private Relation<String, Pair<Boolean, Pair<Integer, String>>> languageToTerritories2 = new Relation(new TreeMap(), TreeSet.class);

    private Relation<String, BasicLanguageData> languageToBasicLanguageData = new Relation(new TreeMap(), TreeSet.class);

    // private Map<String, BasicLanguageData> languageToBasicLanguageData2 = new
    // TreeMap();

    // Relation(new TreeMap(), TreeSet.class, null);

    private Set<String> allLanguages = new TreeSet();

    private Relation<String, String> containment = new Relation(new TreeMap(),
            TreeSet.class);

    private Map<String, CurrencyNumberInfo> currencyToCurrencyNumberInfo = new TreeMap();

    private Relation<String, CurrencyDateInfo> territoryToCurrencyDateInfo = new Relation(new TreeMap(), LinkedHashSet.class);

    //private Relation<String, TelephoneCodeInfo> territoryToTelephoneCodeInfo = new Relation(new TreeMap(), LinkedHashSet.class);
    private Map<String, Set<TelephoneCodeInfo>> territoryToTelephoneCodeInfo = new TreeMap<String, Set<TelephoneCodeInfo>>();

    private Set<String> multizone = new TreeSet();

    private Map<String, String> zone_territory = new TreeMap();

    private Relation<String, String> zone_aliases = new Relation(new TreeMap(),
            LinkedHashSet.class);

    private  Map<String, Map<String, Map<String,String>>> typeToZoneToRegionToZone = new TreeMap<String, Map<String, Map<String,String>>>();

    private Map<String, String> metazoneContinentMap = new HashMap<String,String>();
    private Set<String> allMetazones = new TreeSet<String>();

    private Map<String, String> alias_zone = new TreeMap();

    public Relation<String, Integer> numericTerritoryMapping = new Relation(new HashMap(), HashSet.class);

    public Relation<String, String> alpha3TerritoryMapping = new Relation(new HashMap(), HashSet.class);

    static Map<String, SupplementalDataInfo> directory_instance = new HashMap();

    public Map<String, Map<String,Row.R2<List<String>,String>>> typeToTagToReplacement = new TreeMap<String, Map<String,Row.R2<List<String>,String>>>();

    Map<String,List<Row.R4<String,String,Integer,Boolean>>> languageMatch = new HashMap();

    public Relation<String, String> key_subtypes = new Relation(new TreeMap(), TreeSet.class);
    public Relation<Row.R2<String,String>, String> bcp47Aliases = new Relation(new TreeMap(), TreeSet.class);

    public Relation<String, String> getAlpha3TerritoryMapping() {
        return alpha3TerritoryMapping;
    }

    public Relation<String, Integer> getNumericTerritoryMapping() {
        return numericTerritoryMapping;
    }

    /**
     * Returns type -> tag -> replacement, like "language" -> "sh" -> "sr_Latn"
     * @return
     */
    public Map<String, Map<String, R2<List<String>, String>>> getLocaleAliasInfo() {
        return typeToTagToReplacement;
    }

    public static SupplementalDataInfo getInstance(File supplementalDirectory)  {
        try {
            return getInstance(supplementalDirectory.getCanonicalPath());
        } catch (IOException e) {
            throw (IllegalArgumentException) new IllegalArgumentException()
            .initCause(e);
        }
    }
    
    public static SupplementalDataInfo getInstance() {
        return getInstance(CldrUtility.SUPPLEMENTAL_DIRECTORY);
    }
    
    public static SupplementalDataInfo getInstance(String supplementalDirectory) {
        synchronized (SupplementalDataInfo.class) {
            SupplementalDataInfo instance = directory_instance
            .get(supplementalDirectory);
            if (instance != null) {
                return instance;
            }
            // canonicalize name & try again
            String canonicalpath;
            try {
                canonicalpath = new File(supplementalDirectory).getCanonicalPath();
            } catch (IOException e) {
                throw (IllegalArgumentException) new IllegalArgumentException()
                .initCause(e);
            }
            if (!canonicalpath.equals(supplementalDirectory)) {
                instance = directory_instance.get(canonicalpath);
                if (instance != null) {
                    directory_instance.put(supplementalDirectory, instance);
                    return instance;
                }
            }
            instance = new SupplementalDataInfo();
            MyHandler myHandler = instance.new MyHandler();
            XMLFileReader xfr = new XMLFileReader().setHandler(myHandler);
            File files1[] = new File(canonicalpath).listFiles();
            if(files1==null) {
                throw new InternalError("Could not list XML Files from " + canonicalpath);
            }
            // get bcp47 files also
            File files2[] = new File(canonicalpath + "/../bcp47").listFiles();

            CBuilder<File, ArrayList<File>> builder = Builder.with(new ArrayList<File>()).addAll(files1);
            if (files2 != null) {
                builder.addAll(files2);
            }
            for (File file : builder.get()) {
                if (DEBUG) {
                    try {
                        System.out.println(file.getCanonicalPath());
                    } catch (IOException e) {}
                }
                String name = file.toString();
                if (!name.endsWith(".xml")) continue;
                xfr.read(name, -1, true);
            }

            //xfr = new XMLFileReader().setHandler(instance.new MyHandler());
            //.xfr.read(canonicalpath + "/supplementalMetadata.xml", -1, true);


            instance.makeStuffSafe();
            // cache
            directory_instance.put(supplementalDirectory, instance);
            if (!canonicalpath.equals(supplementalDirectory)) {
                directory_instance.put(canonicalpath, instance);
            }
            return instance;
        }
    }

    private SupplementalDataInfo() {
    }; // hide

    private void makeStuffSafe() {
        // now make stuff safe
        allLanguages.addAll(languageToPopulation.keySet());
        allLanguages.addAll(baseLanguageToPopulation.keySet());
        allLanguages = Collections.unmodifiableSet(allLanguages);
        skippedElements = Collections.unmodifiableSet(skippedElements);
        multizone = Collections.unmodifiableSet(multizone);
        zone_territory = Collections.unmodifiableMap(zone_territory);
        alias_zone = Collections.unmodifiableMap(alias_zone);
        references = Collections.unmodifiableMap(references);
        likelySubtags = Collections.unmodifiableMap(likelySubtags);
        currencyToCurrencyNumberInfo = Collections.unmodifiableMap(currencyToCurrencyNumberInfo);
        territoryToCurrencyDateInfo.freeze();
        //territoryToTelephoneCodeInfo.freeze();
        territoryToTelephoneCodeInfo = Collections.unmodifiableMap(territoryToTelephoneCodeInfo);

        typeToZoneToRegionToZone = CldrUtility.protectCollection(typeToZoneToRegionToZone);
        typeToTagToReplacement = CldrUtility.protectCollection(typeToTagToReplacement);

        containment.freeze();
        languageToBasicLanguageData.freeze();
        for (String language : languageToTerritories2.keySet()) {
            for (Pair<Boolean, Pair<Integer, String>> pair : languageToTerritories2.getAll(language)) {
                languageToTerritories.put(language, pair.getSecond().getSecond());
            }
        }
        languageToTerritories2 = null; // free up the memory.
        languageToTerritories.freeze();
        zone_aliases.freeze();
        languageToScriptVariants.freeze();

        numericTerritoryMapping.freeze();
        alpha3TerritoryMapping.freeze();

        // freeze contents
        for (String language : languageToPopulation.keySet()) {
            languageToPopulation.get(language).freeze();
        }
        for (String language : baseLanguageToPopulation.keySet()) {
            baseLanguageToPopulation.get(language).freeze();
        }
        for (String territory : territoryToPopulationData.keySet()) {
            territoryToPopulationData.get(territory).freeze();
        }
        for (String territory : territoryToLanguageToPopulationData.keySet()) {
            Map<String, PopulationData> languageToPopulationDataTemp = territoryToLanguageToPopulationData
            .get(territory);
            for (String language : languageToPopulationDataTemp.keySet()) {
                languageToPopulationDataTemp.get(language).freeze();
            }
        }
        addPluralInfo();
        localeToPluralInfo = Collections.unmodifiableMap(localeToPluralInfo);

        if (lastDayPeriodLocales != null) {
            addDayPeriodInfo();
        }
        localeToDayPeriodInfo = Collections.unmodifiableMap(localeToDayPeriodInfo);
        languageMatch = CldrUtility.protectCollection(languageMatch);
        key_subtypes.freeze();
        bcp47Aliases.freeze();
    }

    //private Map<String, Map<String, String>> makeUnmodifiable(Map<String, Map<String, String>> metazoneToRegionToZone) {
    //Map<String, Map<String, String>> temp = metazoneToRegionToZone;
    //for (String mzone : metazoneToRegionToZone.keySet()) {
    //temp.put(mzone, Collections.unmodifiableMap(metazoneToRegionToZone.get(mzone)));
    //}
    //return Collections.unmodifiableMap(temp);
    //}

    /**
     * Core function used to process each of the paths, and add the data to the appropriate data member.
     */
    class MyHandler extends XMLFileReader.SimpleHandler {
        private static final double MAX_POPULATION = 3000000000.0;

        XPathParts parts = new XPathParts();

        LanguageTagParser languageTagParser = new LanguageTagParser();

        public void handlePathValue(String path, String value) {
            try {
                parts.set(path);
                String level0 = parts.getElement(0);
                String level1 = parts.size() < 2 ? null : parts.getElement(1);
                String level2 = parts.size() < 3 ? null : parts.getElement(2);
                String level3 = parts.size() < 4 ? null : parts.getElement(3);
                //String level4 = parts.size() < 5 ? null : parts.getElement(4);
                if (level1.equals("generation") || level1.equals("version")) {
                    // skip
                    return;
                }

                // copy the rest from ShowLanguages later
                if (level0.equals("ldmlBCP47")) {
                    if (handleBcp47(level2)) {
                        return;
                    }
                } else if (level1.equals("territoryInfo")) {
                    if (handleTerritoryInfo()) {
                        return;
                    }
                } else if (level1.equals("calendarPreferenceData")) {
                    handleCalendarPreferenceData();
                    return;
                } else if (level1.equals("languageData")) {
                    handleLanguageData();
                    return;
                } else if (level1.equals("territoryContainment")) {
                    handleTerritoryContainment();
                    return;
                } else if (level1.equals("currencyData")) {
                    if (handleCurrencyData(level2)) {
                        return;
                    }
                    //        } else if (level1.equals("timezoneData")) {
                    //          if (handleTimezoneData(level2)) {
                    //            return;
                    //          }
                } else if ("mapTimezones".equals(level2)) {
                    if (handleMetazoneData(level2,level3)) {
                        return;
                    }
                } else if (level1.equals("plurals")) {
                    addPluralPath(parts, value);
                    return;
                } else if (level1.equals("dayPeriodRuleSet")) {
                    addDayPeriodPath(parts, value);
                    return;
                } else if (level1.equals("telephoneCodeData")) {
                    handleTelephoneCodeData(parts);
                    return;
                } else if (level1.equals("references")) {
                    String type = parts.getAttributeValue(-1, "type");
                    String uri = parts.getAttributeValue(-1, "uri");
                    references.put(type, (Pair)new Pair(uri, value).freeze());
                    return;
                } else if (level1.equals("likelySubtags")) {
                    handleLikelySubtags();
                    return;
                } else if (level1.equals("numberingSystems")) {
                    handleNumberingSystems();
                    return;
                } else if (level1.equals("coverageLevels")) {
                    handleCoverageLevels();
                    return;
                } else if (level1.equals("metadata")) {
                    if (handleMetadata(level2, value)) {
                        return;
                    }
                } else if (level1.equals("codeMappings")) {
                    if (handleCodeMappings(level2)) {
                        return;
                    }
                } else if (level1.equals("languageMatching")) {
                    if (handleLanguageMatcher(level2)) {
                        return;
                    }
                }

                // capture elements we didn't look at, since we should cover everything.
                // this helps for updates

                final String skipKey = level1 + (level2 == null ? "" : "/" + level2);
                if (!skippedElements.contains(skipKey)) {
                    skippedElements.add(skipKey);
                }
                // System.out.println("Skipped Element: " + path);
            } catch (Exception e) {
                throw (IllegalArgumentException) new IllegalArgumentException("path: "
                        + path + ",\tvalue: " + value).initCause(e);
            }
        }

        private boolean handleBcp47(String level2) {
            String key = parts.getAttributeValue(2, "name");
            String keyAlias = parts.getAttributeValue(2, "alias");
            String subtype = parts.getAttributeValue(3, "name");
            String subtypeAlias = parts.getAttributeValue(3, "alias");
            key_subtypes.put(key, subtype);
            if (keyAlias != null) {
                bcp47Aliases.putAll((R2<String, String>) Row.of(key,"").freeze(), Arrays.asList(keyAlias.trim().split("\\s+")));
            }
            if (subtypeAlias != null) {
                bcp47Aliases.putAll((R2<String, String>) Row.of(key,subtype).freeze(), Arrays.asList(subtypeAlias.trim().split("\\s+")));
            }
            return true;
        }

        private boolean handleLanguageMatcher(String level2) {
            String type = parts.getAttributeValue(2, "type");
            List<R4<String, String, Integer, Boolean>> matches = languageMatch.get(type);
            if (matches == null) {
                languageMatch.put(type, matches = new ArrayList());
            }
            matches.add(Row.of(parts.getAttributeValue(3,"desired"), parts.getAttributeValue(3,"supported"),
                    Integer.parseInt(parts.getAttributeValue(3,"percent")), 
                    "true".equals(parts.getAttributeValue(3, "oneway"))));
            return true;
        }


        private boolean handleCodeMappings(String level2) {
            if (level2.equals("territoryCodes")) {
                // <territoryCodes type="VU" numeric="548" alpha3="VUT"/>
                String type = parts.getAttributeValue(-1, "type");
                numericTerritoryMapping.put(type, Integer.parseInt(parts.getAttributeValue(-1, "numeric")));
                alpha3TerritoryMapping.put(type, parts.getAttributeValue(-1, "alpha3"));
                return true;
            }
            return false;
        }

        private void handleNumberingSystems() {
            String name = parts.getAttributeValue(-1,"id");
            numberingSystems.add(name);
        }

        private void handleCoverageLevels() {
            String match = parts.getAttributeValue(-1,"match");
            String valueStr = parts.getAttributeValue(-1,"value");
            String inLanguage = parts.getAttributeValue(-1,"inLanguage");
            String inScript = parts.getAttributeValue(-1,"inScript");
            String inTerritory = parts.getAttributeValue(-1,"inTerritory");
            Integer value =  ( valueStr != null ) ? Integer.valueOf(valueStr) : Integer.valueOf("101");
            CoverageLevelInfo ci = new CoverageLevelInfo(match,value,inLanguage,inScript,inTerritory);
            coverageLevels.add(ci);
        }
        private void handleCalendarPreferenceData() {
            String territoryString = parts.getAttributeValue(-1, "territories");
            String orderingString = parts.getAttributeValue(-1,"ordering");
            String[] calendars = orderingString.split(" ");
            String[] territories = territoryString.split(" ");
            List<String> calendarList = Arrays.asList(calendars);
            for ( int i = 0 ; i < territories.length ; i++ ) {
                calendarPreferences.put(territories[i], calendarList);
            }
        }
        private void handleLikelySubtags() {
            String from = parts.getAttributeValue(-1, "from");
            String to = parts.getAttributeValue(-1, "to");
            likelySubtags.put(from, to);
        }

        //    private boolean handleTimezoneData(String level2) {
        //      if (level2.equals("zoneFormatting")) {
        //        handleZoneFormatting();
        //        return true;
        //      }
        //
        //      /* Note, the following is for obsolete (pre 1.8) format data. */
        //
        //      /*
        //       * <mapTimezones type="metazones">
        //       *  <mapZone other="Acre"  territory="001" type="America/Rio_Branco"/>
        //       */
        //      if (level2.equals("mapTimezones")) {
        //        String mzone = parts.getAttributeValue(3,"other");
        //        String region = parts.getAttributeValue(3,"territory");
        //        String zone = parts.getAttributeValue(3,"type");
        //        if ("metazones".equals(parts.getAttributeValue(2,"type"))) {
        //          if (region == null) {
        //            throw new IllegalArgumentException("metazone mapping needs region: " + parts);
        //          }
        //          Map<String, String> regionToZone = metazoneToRegionToZone.get(mzone);
        //          if (regionToZone == null) metazoneToRegionToZone.put(mzone, regionToZone = new HashMap<String,String>());
        //          regionToZone.put(region, zone);
        //        }
        //        
        //        return true;
        //      }
        //
        //      // <mapTimezones type="windows"> <mapZone other="Dateline"
        //      // type="Etc/GMT+12"/> <!-- S (GMT-12:00) International Date Line
        //      // West-->
        //      return false;
        //    }

        /*
<supplementalData>
  <metaZones>
    <metazoneInfo...>
    <mapTimezones type="metazones">
      <mapZone other="Acre" territory="001" type="America/Rio_Branco"/>

<supplementalData>
  <windowsZones>
    <mapTimezones>
      <mapZone other="AUS Central Standard Time" type="Australia/Darwin"/> <!-- S (GMT+09:30) Darwin -->
         */
        /**
         * Only called if level2 = mapTimezones. Level 1 might be metaZones or might be windowsZones
         */
        private boolean handleMetazoneData(String level2, String level3) {
            if (level3.equals("mapZone")) {
                String maintype = parts.getAttributeValue(2,"type");
                if (maintype == null) {
                    maintype = "windows";
                }
                String mzone = parts.getAttributeValue(3,"other");
                String region = parts.getAttributeValue(3,"territory");
                String zone = parts.getAttributeValue(3,"type");

                Map<String, Map<String, String>> zoneToRegionToZone = typeToZoneToRegionToZone.get(maintype);
                if (zoneToRegionToZone == null) {
                    typeToZoneToRegionToZone.put(maintype, zoneToRegionToZone = new TreeMap<String, Map<String, String>>());
                }
                Map<String, String> regionToZone = zoneToRegionToZone.get(mzone);
                if (regionToZone == null) {
                    zoneToRegionToZone.put(mzone, regionToZone = new TreeMap<String,String>());
                }
                regionToZone.put(region, zone);
                if (maintype.equals("metazones")) {
                    if(mzone != null && region.equals("001")) {
                        metazoneContinentMap.put(mzone,zone.substring(0,zone.indexOf("/")));
                    }
                    allMetazones.add(mzone);
                }
                return true;
            }
            return false;
        }


        private boolean handleMetadata(String level2, String value) {
            if (parts.contains("defaultContent")) {
                String defContent = parts.getAttributeValue(-1, "locales").trim();
                String [] defLocales = defContent.split("\\s+");
                defaultContentLocales = Collections.unmodifiableSet(new TreeSet<String>(Arrays.asList(defLocales)));
                return true;
            }
            if (level2.equals("alias")) {
                //      <alias>
                //      <!-- grandfathered 3066 codes -->
                //      <languageAlias type="art-lojban" replacement="jbo"/> <!-- Lojban -->
                String level3 = parts.getElement(3);
                if (!level3.endsWith("Alias")) {
                    throw new IllegalArgumentException();
                }
                level3 = level3.substring(0,level3.length() - "Alias".length());
                Map<String, R2<List<String>, String>> tagToReplacement = typeToTagToReplacement.get(level3);
                if (tagToReplacement == null) {
                    typeToTagToReplacement.put(level3, tagToReplacement = new TreeMap<String, R2<List<String>, String>>());
                }
                final String replacement = parts.getAttributeValue(3,"replacement");
                final String reason = parts.getAttributeValue(3,"reason");
                List<String> replacementList = replacement == null ? null : Arrays.asList(replacement.replace("-","_").split("\\s+"));
                String cleanTag = parts.getAttributeValue(3,"type").replace("-","_");
                tagToReplacement.put(cleanTag, (R2<List<String>, String>) Row.of(replacementList, reason).freeze());
                return true;
            }
            if (level2.equals("attributeOrder")) {
                attributeOrder = Arrays.asList(value.trim().split("\\s+"));
                return true;
            }
            if (level2.equals("elementOrder")) {
                elementOrder = Arrays.asList(value.trim().split("\\s+"));
                return true;
            }
            if (level2.equals("serialElements")) {
                serialElements = Arrays.asList(value.trim().split("\\s+"));
                return true;
            }
            return false;
        }

        private boolean handleTerritoryInfo() {

            // <territoryInfo>
            // <territory type="AD" gdp="1840000000" literacyPercent="100"
            // population="66000"> <!--Andorra-->
            // <languagePopulation type="ca" populationPercent="50"/>
            // <!--Catalan-->

            Map<String, String> territoryAttributes = parts.getAttributes(2);
            String territory = territoryAttributes.get("type");
            double territoryPopulation = parseDouble(territoryAttributes.get("population"));
            if (failsRangeCheck("population", territoryPopulation, 0, MAX_POPULATION)) {
                return true;
            }

            double territoryLiteracyPercent = parseDouble(territoryAttributes.get("literacyPercent"));
            double territoryGdp = parseDouble(territoryAttributes.get("gdp"));
            if (territoryToPopulationData.get(territory) == null) {
                territoryToPopulationData.put(territory, new PopulationData()
                .setPopulation(territoryPopulation)
                .setLiteratePopulation(territoryLiteracyPercent * territoryPopulation / 100)
                .setGdp(territoryGdp));
            }
            if (parts.size() > 3) {

                Map<String, String> languageInTerritoryAttributes = parts
                .getAttributes(3);
                String language = languageInTerritoryAttributes.get("type");
                double languageLiteracyPercent = parseDouble(languageInTerritoryAttributes.get("writingPercent"));
                if (Double.isNaN(languageLiteracyPercent)) {
                    languageLiteracyPercent = territoryLiteracyPercent;
                } else {
                    if (false) System.out.println("writingPercent\t" + languageLiteracyPercent
                            + "\tterritory\t" + territory
                            + "\tlanguage\t" + language);
                }
                double languagePopulationPercent = parseDouble(languageInTerritoryAttributes.get("populationPercent"));
                double languagePopulation = languagePopulationPercent * territoryPopulation / 100;
                //double languageGdp = languagePopulationPercent * territoryGdp;

                // store
                Map<String, PopulationData> territoryLanguageToPopulation = territoryToLanguageToPopulationData
                .get(territory);
                if (territoryLanguageToPopulation == null) {
                    territoryToLanguageToPopulationData.put(territory,
                            territoryLanguageToPopulation = new TreeMap());
                }
                OfficialStatus officialStatus = OfficialStatus.unknown;
                String officialStatusString = languageInTerritoryAttributes.get("officialStatus");
                if (officialStatusString != null) officialStatus = OfficialStatus.valueOf(officialStatusString);

                PopulationData newData = new PopulationData()
                .setPopulation(languagePopulation)
                .setLiteratePopulation(languageLiteracyPercent * languagePopulation / 100)
                .setOfficialStatus(officialStatus)
                //.setGdp(languageGdp)
                ;
                newData.freeze();
                if (territoryLanguageToPopulation.get(language) != null) {
                    System.out
                    .println("Internal Problem in supplementalData: multiple data items for "
                            + language + ", " + territory + "\tSkipping " + newData);
                    return true;
                }

                territoryLanguageToPopulation.put(language, newData);
                // add the language, using the Pair fields to get the ordering right
                languageToTerritories2.put(language, new Pair(newData.getOfficialStatus().isMajor() ? 0 : 1, new Pair(-newData.getLiteratePopulation(), territory)));

                // now collect data for languages globally
                PopulationData data = languageToPopulation.get(language);
                if (data == null) {
                    languageToPopulation.put(language, data = new PopulationData().set(newData));
                } else {
                    data.add(newData);
                }
                if (false && language.equals("en")) {
                    System.out.println(territory + "\tnewData:\t" + newData + "\tdata:\t" + data);   
                }
                String baseLanguage = languageTagParser.set(language).getLanguage();
                if (!baseLanguage.equals(language)) {
                    languageToScriptVariants.put(baseLanguage,language);

                    data = baseLanguageToPopulation.get(baseLanguage);
                    if (data == null)
                        baseLanguageToPopulation.put(baseLanguage,
                                data = new PopulationData());
                    data.add(newData);
                }
            }
            return true;
        }

        private boolean handleCurrencyData(String level2) {
            if (level2.equals("fractions")) {
                // <info iso4217="ADP" digits="0" rounding="0"/>
                currencyToCurrencyNumberInfo.put(parts.getAttributeValue(3, "iso4217"), 
                        new CurrencyNumberInfo(Integer.parseInt(parts.getAttributeValue(3, "digits")), 
                                Integer.parseInt(parts.getAttributeValue(3, "rounding"))));
                return true;
            }
            /*
             * <region iso3166="AD">
        <currency iso4217="EUR" from="1999-01-01"/>
        <currency iso4217="ESP" from="1873" to="2002-02-28"/>
             */
            if (level2.equals("region")) {
                territoryToCurrencyDateInfo.put(parts.getAttributeValue(2, "iso3166"), 
                        new CurrencyDateInfo(parts.getAttributeValue(3, "iso4217"),
                                parts.getAttributeValue(3, "from"),
                                parts.getAttributeValue(3, "to"),
                                parts.getAttributeValue(3, "tender") != "false"
                        ));
                return true;
            }

            return false;
        }

        private void handleTelephoneCodeData(XPathParts parts) {
            // element 2: codesByTerritory territory [draft] [references]
            String terr = parts.getAttributeValue(2, "territory");
            // element 3: telephoneCountryCode code [from] [to] [draft] [references] [alt]
            TelephoneCodeInfo tcInfo = new TelephoneCodeInfo( parts.getAttributeValue(3, "code"),
                    parts.getAttributeValue(3, "from"),
                    parts.getAttributeValue(3, "to"),
                    parts.getAttributeValue(3, "alt") );

            Set<TelephoneCodeInfo> tcSet = territoryToTelephoneCodeInfo.get(terr);
            if (tcSet == null) {
                tcSet = new LinkedHashSet<TelephoneCodeInfo>();
                territoryToTelephoneCodeInfo.put( terr, tcSet );
            }
            tcSet.add(tcInfo);
        }

        private void handleZoneFormatting() {
            // <zoneFormatting multizone="001 AQ AR AU BR CA CD CL CN EC ES FM GL
            // ID KI KZ MH MN MX MY NZ PF PT RU SJ UA UM US UZ"
            // tzidVersion="2007c">
            // <zoneItem type="Africa/Abidjan" territory="CI"/>
            // <zoneItem type="Africa/Asmera" territory="ER"
            // aliases="Africa/Asmara"/>
            if (multizone.size() == 0) {
                multizone.addAll(Arrays.asList(parts.getAttributeValue(2,
                "multizone").trim().split("\\s+")));
            }
            String zone = parts.getAttributeValue(3, "type");
            String territory = parts.getAttributeValue(3, "territory");
            String aliases = parts.getAttributeValue(3, "aliases");
            if (territory != null) {
                zone_territory.put(zone, territory);
            } else {
                throw new IllegalArgumentException("Problem in data");
            }
            // include the item itself
            Collection<String> aliasArray = new LinkedHashSet<String>();
            //aliasArray.add(zone);
            if (aliases != null) {
                aliasArray.addAll(Arrays.asList(aliases.split("\\s+")));
            }
            for (String alias : aliasArray) {
                alias_zone.put(alias, zone);
                zone_aliases.put(zone, alias);
            }
        }

        private void handleTerritoryContainment() {
            // <group type="001" contains="002 009 019 142 150"/>
            containment.putAll(parts.getAttributeValue(-1, "type"), Arrays
                    .asList(parts.getAttributeValue(-1, "contains").split("\\s+")));
        }

        private void handleLanguageData() {
            // <languageData>
            // <language type="aa" scripts="Latn" territories="DJ ER ET"/> <!--
            // Reflecting submitted data, cldrbug #1013 -->
            // <language type="ab" scripts="Cyrl" territories="GE"
            // alt="secondary"/>
            String language = (String) parts.getAttributeValue(2, "type");
            BasicLanguageData languageData = new BasicLanguageData();
            languageData
            .setType(parts.getAttributeValue(2, "alt") == null ? BasicLanguageData.Type.primary
                    : BasicLanguageData.Type.secondary);
            languageData.setScripts(parts.getAttributeValue(2, "scripts"))
            .setTerritories(parts.getAttributeValue(2, "territories"));
            languageToBasicLanguageData.put(language, languageData);
        }

        private boolean failsRangeCheck(String path, double input, double min, double max) {
            if (input >= min && input <= max) {
                return false;
            }
            System.out
            .println("Internal Problem in supplementalData: range check fails for "
                    + input + ", min: " + min + ", max:" + max + "\t" + path);

            return false;
        }

        private double parseDouble(String literacyString) {
            return literacyString == null ? Double.NaN : Double
                    .parseDouble(literacyString);
        }
    }

    private class CoverageVariableInfo {
        public Set<String> targetScripts;
        public Set<String> targetTerritories;
        public Set<String> calendars;
    }

    public static String toRegexString(Set<String> s) {
        Iterator<String> it = s.iterator();
        StringBuilder sb = new StringBuilder("(");
        int count = 0;
        while (it.hasNext()) {
            if ( count > 0 ) {
                sb.append("|");
            }
            sb.append(it.next());
            count++;
        }
        sb.append(")");
        return sb.toString();

    }

    Set<String> skippedElements = new TreeSet();

    private Map<String, Pair<String, String>> references = new TreeMap();
    private Map<String, String> likelySubtags = new TreeMap();
    private SortedSet<CoverageLevelInfo> coverageLevels = new TreeSet<CoverageLevelInfo>();
    private Map<String, List<String>> calendarPreferences= new HashMap();
    private Map<String, CoverageVariableInfo> coverageVariables = new TreeMap();    
    private Set<String> numberingSystems = new TreeSet();
    private Set<String> defaultContentLocales;
    /**
     * Get the population data for a language. Warning: if the language has script variants, cycle on those variants.
     * 
     * @param language
     * @param output
     * @return
     */
    public PopulationData getLanguagePopulationData(String language) {
        return languageToPopulation.get(language);
    }

    public Set<String> getLanguages() {
        return allLanguages;
    }

    public Set<String> getTerritoryToLanguages(String territory) {
        Map<String, PopulationData> result = territoryToLanguageToPopulationData
        .get(territory);
        if (result == null) {
            return Collections.EMPTY_SET;
        }
        return result.keySet();
    }

    public PopulationData getLanguageAndTerritoryPopulationData(String language,
            String territory) {
        Map<String, PopulationData> result = territoryToLanguageToPopulationData
        .get(territory);
        if (result == null) {
            return null;
        }
        return result.get(language);
    }

    public Set<String> getTerritoriesWithPopulationData() {
        return territoryToLanguageToPopulationData.keySet();
    }

    public Set<String> getLanguagesForTerritoryWithPopulationData(String territory) {
        return territoryToLanguageToPopulationData.get(territory).keySet();
    }

    public Set<BasicLanguageData> getBasicLanguageData(String language) {
        return languageToBasicLanguageData.getAll(language);
    }

    public Set<String> getBasicLanguageDataLanguages() {
        return languageToBasicLanguageData.keySet();
    }

    public Set<String> getContained(String territoryCode) {
        return containment.getAll(territoryCode);
    }

    public Set<String> getContainers() {
        return containment.keySet();
    }

    public Relation<String, String> getTerritoryToContained() {
        return containment;
    }

    public Set<String> getSkippedElements() {
        return skippedElements;
    }

    public Set<String> getZone_aliases(String zone) {
        Set<String> result = zone_aliases.getAll(zone);
        if (result == null) {
            return Collections.EMPTY_SET;
        }
        return result;
    }

    public String getZone_territory(String zone) {
        return zone_territory.get(zone);
    }

    public Set<String> getCanonicalZones() {
        return zone_territory.keySet();
    }

    /**
     * Return the multizone countries (should change name).
     * @return
     */
    public Set<String> getMultizones() {
        // TODO Auto-generated method stub
        return multizone;
    }

    private Set<String> singleRegionZones;

    public Set<String> getSingleRegionZones() {
        synchronized (this) {
            if (singleRegionZones == null) {
                singleRegionZones = new HashSet<String>();
                SupplementalDataInfo supplementalData = this; // TODO: this?
                Set<String> multizoneCountries = supplementalData.getMultizones();
                for (String zone : supplementalData.getCanonicalZones()) {
                    String region = supplementalData.getZone_territory(zone);
                    if (!multizoneCountries.contains(region) || zone.startsWith("Etc/")) {
                        singleRegionZones.add(zone);
                    }
                }
                singleRegionZones.remove("Etc/Unknown"); // remove special case
                singleRegionZones = Collections.unmodifiableSet(singleRegionZones);
            }
        }
        return singleRegionZones;
    }

    public Set<String> getTerritoriesForPopulationData(String language) {
        return languageToTerritories.getAll(language);
    }

    public Set<String> getLanguagesForTerritoriesPopulationData() {
        return languageToTerritories.keySet();
    }

    /**
     * Return the canonicalized zone, or null if there is none.
     * 
     * @param alias
     * @return
     */
    public Set<String> getDefaultContentLocales() {
        return defaultContentLocales;
    }
    public Set<String> getNumberingSystems() {
        return numberingSystems;
    }
    public int getCoverageValue(String xpath) {
        ULocale loc = new ULocale("und");
        return getCoverageValue(xpath,loc);
    }
    public int getCoverageValue(String xpath, ULocale loc) {
        CoverageVariableInfo cvi;
        String targetLanguage = loc.getLanguage();
       
        if ( coverageVariables.containsKey(targetLanguage)) {
            cvi = coverageVariables.get(targetLanguage);
        } else {
            cvi = new CoverageVariableInfo();
            cvi.targetScripts = getTargetScripts(targetLanguage);
            cvi.targetTerritories = getTargetTerritories(targetLanguage);
            cvi.calendars = getCalendars(cvi.targetTerritories);
            coverageVariables.put(targetLanguage, cvi);
        }
        String targetScriptString = toRegexString(cvi.targetScripts);
        String targetTerritoryString = toRegexString(cvi.targetTerritories);
        String calendarListString = toRegexString(cvi.calendars);
        Iterator<CoverageLevelInfo> i = coverageLevels.iterator();
        while (i.hasNext()) {
            CoverageLevelInfo ci = i.next();
            StringBuilder sb = new StringBuilder(ci.match.replace('\'','"'));
            String regex = "//ldml/"+ci.match.replace('\'','"')
                                             .replaceAll("\\[","\\\\[")
                                             .replaceAll("\\]","\\\\]")
                                             .replaceAll("\\$\\{Target\\-Language\\}", targetLanguage)
                                             .replaceAll("\\$\\{Target\\-Scripts\\}", targetScriptString)
                                             .replaceAll("\\$\\{Target\\-Territories\\}", targetTerritoryString)
                                             .replaceAll("\\$\\{Calendar\\-List\\}", calendarListString);

            // Special logic added for coverage fields that are only to be applicable
            // to certain territories
            if (ci.inTerritory != null) {
              if (ci.inTerritory.equals("EU")) {
                  Set<String> containedTerritories = new HashSet<String>();
                  containedTerritories.addAll(getContained(ci.inTerritory));
                  containedTerritories.retainAll(cvi.targetTerritories);                  
                  if ( containedTerritories.isEmpty())  {
                   continue;
                  }
              }
              else {
                  if (!cvi.targetTerritories.contains(ci.inTerritory)) {
                      continue;
                  }
              }
            }
            // Special logic added for coverage fields that are only to be applicable
            // to certain languages         
            if (ci.inLanguage != null && !targetLanguage.matches(ci.inLanguage)) {
                continue;
            }
            
            // Special logic added for coverage fields that are only to be applicable
            // to certain scripts
            if (ci.inScript != null && !cvi.targetScripts.contains(ci.inScript)) {
                continue;
            }
            
            if (xpath.matches(regex)) {
                return ci.value.intValue();
            }
            
            if (xpath.matches(regex)) {
                return ci.value.intValue();
            }
        }
        return 101; // If no match then return highest possible value
    }
    private Set<String> getTargetScripts(String language) {
        Set<BasicLanguageData> langData = getBasicLanguageData(language);
        Set<String> targetScripts = new HashSet<String>();
        Iterator<BasicLanguageData> ldi = langData.iterator();
        while ( ldi.hasNext()) {
            Set<String> addScripts = ldi.next().scripts;
            if ( addScripts != null ) {
                targetScripts.addAll(addScripts);              
            }
        }
        return targetScripts;
    }
    private Set<String> getTargetTerritories(String language) {
        Set<BasicLanguageData> langData = getBasicLanguageData(language);
        Set<String> targetTerritories = new HashSet<String>();
        Iterator<BasicLanguageData> ldi = langData.iterator();
        while ( ldi.hasNext()) {
            Set<String> addTerritories = ldi.next().territories;
            if ( addTerritories != null ) {
                targetTerritories.addAll(addTerritories);              
            }
        }
        return targetTerritories;
    }
    private Set<String> getCalendars(Set<String> territories) {
        Set<String> targetCalendars = new HashSet<String>();
        Iterator<String> it = territories.iterator();
        while ( it.hasNext()) {
            List<String> addCalendars = calendarPreferences.get(it.next());
            if ( addCalendars == null ) {
                continue;
            }
            Iterator<String> it2 = addCalendars.iterator();
            while ( it2.hasNext() ) {
                targetCalendars.add(it2.next());
            }
        }
        return targetCalendars;
    }
    
    /**
     * Return the canonicalized zone, or null if there is none.
     * 
     * @param alias
     * @return
     */
    public String getZoneFromAlias(String alias) {
        String zone = alias_zone.get(alias);
        if (zone != null)
            return zone;
        if (zone_territory.get(alias) != null)
            return alias;
        return null;
    }

    public boolean isCanonicalZone(String alias) {
        return zone_territory.get(alias) != null;
    }

    /**
     * Return the approximate economic weight of this language, computed by taking
     * all of the languages in each territory, looking at the literate population
     * and dividing up the GDP of the territory (in PPP) according to the
     * proportion that language has of the total. This is only an approximation,
     * since the language information is not complete, languages may overlap
     * (bilingual speakers), the literacy figures may be estimated, and literacy
     * is only a rough proxy for weight of each language in the economy of the
     * territory.
     * 
     * @param language
     * @return
     */
    public double getApproximateEconomicWeight(String targetLanguage) {
        double weight = 0;
        Set<String> territories = getTerritoriesForPopulationData(targetLanguage);
        if (territories == null) return weight;
        for (String territory : territories) {
            Set<String> languagesInTerritory = getTerritoryToLanguages(territory);
            double totalLiteratePopulation = 0;
            double targetLiteratePopulation = 0;
            for (String language : languagesInTerritory) {
                PopulationData populationData = getLanguageAndTerritoryPopulationData(
                        language, territory);
                totalLiteratePopulation += populationData.getLiteratePopulation();
                if (language.equals(targetLanguage)) {
                    targetLiteratePopulation = populationData.getLiteratePopulation();
                }
            }
            PopulationData territoryPopulationData = getPopulationDataForTerritory(territory);
            final double gdp = territoryPopulationData.getGdp();
            final double scaledGdp = gdp * targetLiteratePopulation / totalLiteratePopulation;
            if (scaledGdp > 0) {
                weight += scaledGdp;
            } else {
                //System.out.println("?\t" + territory + "\t" + targetLanguage);
            }
        }
        return weight;
    }

    public PopulationData getPopulationDataForTerritory(String territory) {
        return territoryToPopulationData.get(territory);
    }

    public Set<String> getScriptVariantsForPopulationData(String language) {
        return languageToScriptVariants.getAll(language);
    }

    public Map<String, Pair<String, String>> getReferences() {
        return references;
    }

    public Map<String,Map<String,String>> getMetazoneToRegionToZone() {
        return typeToZoneToRegionToZone.get("metazones");
    }

    public Map<String,Map<String,Map<String,String>>> getTypeToZoneToRegionToZone() {
        return typeToZoneToRegionToZone;
    }

    public Map<String,String> getMetazoneToContinentMap() {
        return metazoneContinentMap;
    }

    public Set<String> getAllMetazones() {
        return allMetazones;
    }

    public Map<String, String> getLikelySubtags() {
        return likelySubtags;
    }

    private Map<String,PluralInfo> localeToPluralInfo = new LinkedHashMap<String,PluralInfo>();
    private Map<String,DayPeriodInfo> localeToDayPeriodInfo = new LinkedHashMap<String,DayPeriodInfo>();
    private transient String lastPluralLocales = "root";
    private transient Map<Count,String> lastPluralMap = new LinkedHashMap<Count,String>();
    private transient String lastDayPeriodLocales = null;
    private transient DayPeriodInfo.Builder dayPeriodBuilder = new DayPeriodInfo.Builder();

    private void addDayPeriodPath(XPathParts path, String value) {
        //ldml/dates/calendars/calendar[@type="gregorian"]/dayPeriods/dayPeriodContext[@type="format"]/dayPeriodWidth[@type="wide"]/dayPeriod[@type="am"]
        /*
     <supplementalData>
     <version number="$Revision$"/>
     <generation date="$Date$"/>
     <dayPeriodRuleSet>
         <dayPeriodRules locales = "en">  <!--  default for any locales not listed under other dayPeriods -->
            <dayPeriodRule type = "am" from = "0:00" before="12:00"/>
            <dayPeriodRule type = "pm" from = "12:00" to="24:00"/>
         */
        String locales = path.getAttributeValue(2, "locales").trim();
        if (!locales.equals(lastDayPeriodLocales)) {
            if (lastDayPeriodLocales != null) {
                addDayPeriodInfo();
            }
            lastDayPeriodLocales = locales;
        }
        DayPeriod dayPeriod = DayPeriod.valueOf(path.getAttributeValue(-1, "type"));
        String at = path.getAttributeValue(-1, "at");
        String from = path.getAttributeValue(-1, "from");
        String after = path.getAttributeValue(-1, "after");
        String to = path.getAttributeValue(-1, "to");
        String before = path.getAttributeValue(-1, "before");
        if (at != null) {
            if (from != null || after != null || to != null || before != null) {
                throw new IllegalArgumentException();
            }
            from = at;
            to = at;
        } else if ((from == null) == (after == null) || (to == null) == (before == null)) {
            throw new IllegalArgumentException();
        }
        boolean includesStart = from != null;
        boolean includesEnd = to != null;
        int start = parseTime(includesStart ? from : after);
        int end = parseTime(includesEnd ? to : before);
        dayPeriodBuilder.add(dayPeriod, start, includesStart, end, includesEnd);
    }

    static Pattern PARSE_TIME = Pattern.compile("(\\d\\d?):(\\d\\d)");
    private int parseTime(String string) {
        // TODO Auto-generated method stub
        Matcher matcher = PARSE_TIME.matcher(string);
        if (!matcher.matches()) {
            throw new IllegalArgumentException();
        }
        return (Integer.parseInt(matcher.group(1)) * 60 + Integer.parseInt(matcher.group(2))) * 60 * 1000;
    }

    private void addDayPeriodInfo() {
        String[] locales = lastDayPeriodLocales.split("\\s+");
        DayPeriodInfo temp = dayPeriodBuilder.finish(locales);
        for (String locale : locales) {
            localeToDayPeriodInfo.put(locale, temp);
        }
    }

    private void addPluralPath(XPathParts path, String value) {
        String locales = path.getAttributeValue(2, "locales");
        if (!lastPluralLocales.equals(locales)) {
            addPluralInfo();
            lastPluralLocales = locales;
        }
        final String countString = path.getAttributeValue(-1, "count");
        if (countString == null) return;
        Count count = Count.valueOf(countString);
        if (lastPluralMap.containsKey(count)) {
            throw new IllegalArgumentException("Duplicate plural count: " + count + " in " + locales);
        }
        lastPluralMap.put(count, value);
    }

    private void addPluralInfo() {
        final String[] locales = lastPluralLocales.split("\\s+");
        PluralInfo info = new PluralInfo(lastPluralMap);
        for (String locale : locales) {
            if (localeToPluralInfo.containsKey(locale)) {
                throw new IllegalArgumentException("Duplicate plural locale: " + locale);
            }
            localeToPluralInfo.put(locale, info);
        }
        lastPluralMap.clear();
    }

    /**
     * Immutable class with plural info for different locales
     * @author markdavis
     */
    public static class PluralInfo {
        public enum Count {
            zero, one, two, few, many, other;
        }
        static final Pattern pluralPaths = Pattern.compile(".*pluralRule.*");

        private final Map<Count,List<Double>> countToExampleList;
        private final Map<Count,String> countToStringExample;
        private final Map<Integer,Count> exampleToCount;
        private final PluralRules pluralRules;
        private final String pluralRulesString;

        private PluralInfo(Map<Count,String> countToRule) {
            // now build rules
            StringBuilder pluralRuleBuilder = new StringBuilder();
            XPathParts parts = new XPathParts();
            for (Count count : countToRule.keySet()) {
                if (pluralRuleBuilder.length() != 0) {
                    pluralRuleBuilder.append(';');
                }
                pluralRuleBuilder.append(count).append(':').append(countToRule.get(count));
            }
            pluralRulesString = pluralRuleBuilder.toString();
            pluralRules = PluralRules.createRules(pluralRulesString);
            Set targetKeywords = pluralRules.getKeywords();

            Map<Count,List<Double>> countToExampleListRaw = new TreeMap<Count,List<Double>>();
            Map<Integer,Count> exampleToCountRaw = new TreeMap<Integer,Count>();
            Map<Count,UnicodeSet> typeToExamples2 = new TreeMap<Count,UnicodeSet>();

            for (int i = 0; i < 1000; ++i) {
                Count type = Count.valueOf(pluralRules.select(i));
                UnicodeSet uset = typeToExamples2.get(type);
                if (uset == null) typeToExamples2.put(type, uset = new UnicodeSet());
                uset.add(i);       
            }
            // double check
            //    if (!targetKeywords.equals(typeToExamples2.keySet())) {
            //    throw new IllegalArgumentException ("Problem in plurals " + targetKeywords + ", " + this);
            //    }
            // now fix the longer examples
            String otherFractionalExamples = "";
            List<Double> otherFractions = new ArrayList(0);

            // add fractional samples
            Map<Count,String> countToStringExampleRaw = new TreeMap<Count,String>();
            for (Count type : typeToExamples2.keySet()) {
                UnicodeSet uset = typeToExamples2.get(type);
                int sample = uset.getRangeStart(0);
                if (sample == 0 && uset.size() > 1) { // pick non-zero if possible
                    UnicodeSet temp = new UnicodeSet(uset);
                    temp.remove(0);
                    sample = temp.getRangeStart(0);
                }
                Integer sampleInteger = sample;
                exampleToCountRaw.put(sampleInteger, type);

                final ArrayList<Double> arrayList = new ArrayList<Double>();
                arrayList.add((double)sample);

                // add fractional examples
                final double fraction = (sample + 0.31d);
                Count fracType = Count.valueOf(pluralRules.select(fraction));
                boolean addCurrentFractionalExample = false;

                if (fracType == Count.other) {
                    otherFractions.add(fraction);
                    if (otherFractionalExamples.length() != 0) {
                        otherFractionalExamples += ", ";
                    }
                    otherFractionalExamples += fraction;
                } else if (fracType == type) {
                    arrayList.add(fraction);
                    addCurrentFractionalExample = true;
                } // else we ignore it

                StringBuilder b = new StringBuilder();
                int limit = uset.getRangeCount();
                int count = 0;
                boolean addEllipsis = false;
                for (int i = 0; i < limit; ++i) {
                    if (count > 5) {
                        addEllipsis = true;
                        break;
                    }
                    int start = uset.getRangeStart(i);
                    int end = uset.getRangeEnd(i);
                    if (b.length() != 0) {
                        b.append(", ");
                    }
                    if (start == end) {
                        b.append(start);
                        ++count;
                    } else if (start+1 == end) {
                        b.append(start).append(", ").append(end);
                        count+=2;
                    } else {
                        b.append(start).append('-').append(end);
                        count+=2;
                    }
                }
                if (addCurrentFractionalExample) {
                    if (b.length() != 0) {
                        b.append(", ");
                    }
                    b.append(fraction).append("...");
                } else if (addEllipsis) {
                    b.append("...");
                }

                countToExampleListRaw.put(type, arrayList);
                countToStringExampleRaw.put(type, b.toString());
            }
            final String baseOtherExamples = countToStringExampleRaw.get(Count.other);
            String otherExamples = (baseOtherExamples == null ? "" :  baseOtherExamples + "; ") + otherFractionalExamples + "...";
            countToStringExampleRaw.put(Count.other, otherExamples);
            // add otherFractions
            List<Double> list_temp = countToExampleListRaw.get(Count.other);
            if (list_temp == null) {
                countToExampleListRaw.put(Count.other, list_temp = new ArrayList<Double>(0));
            }
            list_temp.addAll(otherFractions);

            for (Count type : countToExampleListRaw.keySet()) {
                List<Double> list = countToExampleListRaw.get(type);
                //        if (type.equals(Count.other)) {
                //          list.addAll(otherFractions);
                //        }
                list = Collections.unmodifiableList(list);
            }

            countToExampleList = Collections.unmodifiableMap(countToExampleListRaw);
            countToStringExample = Collections.unmodifiableMap(countToStringExampleRaw);
            exampleToCount = Collections.unmodifiableMap(exampleToCountRaw);
        }

        public String toString() {
            return countToExampleList + "; " + exampleToCount + "; " + pluralRules;
        }

        public Map<Count, List<Double>> getCountToExamplesMap() {
            return countToExampleList;
        }
        public Map<Count, String> getCountToStringExamplesMap() {
            return countToStringExample;
        }

        public Count getCount(double exampleCount) {
            return Count.valueOf(pluralRules.select(exampleCount));
        }

        public String getRules() {
            // TODO Auto-generated method stub
            return pluralRulesString;
        }

        public Count getDefault() {
            return null;
        }
    }

    public Set<String> getPluralLocales() {
        return localeToPluralInfo.keySet();
    }

    /**
     * Returns the plural info for a given locale.
     * @param locale
     * @return
     */
    public PluralInfo getPlurals(String locale) {
        while (locale != null) {
            PluralInfo result = localeToPluralInfo.get(locale);
            if (result != null) return result;
            locale = LanguageTagParser.getParent(locale);
        }
        return null;
    }

    public DayPeriodInfo getDayPeriods(String locale) {
        while (locale != null) {
            DayPeriodInfo result = localeToDayPeriodInfo.get(locale);
            if (result != null) return result;
            locale = LanguageTagParser.getParent(locale);
        }
        return null;
    }

    public Set<String> getDayPeriodLocales() {
        return localeToDayPeriodInfo.keySet();
    }

    private static CurrencyNumberInfo DEFAULT_NUMBER_INFO = new CurrencyNumberInfo(2,0);

    public CurrencyNumberInfo getCurrencyNumberInfo(String currency) {
        CurrencyNumberInfo result = currencyToCurrencyNumberInfo.get(currency);
        if (result == null) {
            result = DEFAULT_NUMBER_INFO;
        }
        return result;
    }

    /**
     * Returns ordered set of currency data information
     * @param territory
     * @return
     */
    public Set<CurrencyDateInfo> getCurrencyDateInfo(String territory) {
        return territoryToCurrencyDateInfo.getAll(territory);
    }

    public Map<String, Set<TelephoneCodeInfo>> getTerritoryToTelephoneCodeInfo() {
        return territoryToTelephoneCodeInfo;
    }

    public Set<TelephoneCodeInfo> getTelephoneCodeInfoForTerritory(String territory) {
        return territoryToTelephoneCodeInfo.get(territory);
    }

    public Set<String> getTerritoriesForTelephoneCodeInfo() {
        return territoryToTelephoneCodeInfo.keySet();
    }

    private List<String> attributeOrder;
    private List<String> elementOrder;
    private List<String> serialElements;

    public List<String> getAttributeOrder() {
        return attributeOrder;
    }

    public List<String> getElementOrder() {
        return elementOrder;
    }

    public List<String> getSerialElements() {
        return serialElements;
    }

    public List<R4<String, String, Integer, Boolean>> getLanguageMatcherData(String string) {
        return languageMatch.get(string);
    }

    /**
     * Return mapping from keys to subtypes
     */
    public Relation<String,String> getBcp47Keys() {
        return key_subtypes;
    }
    /**
     * Return mapping from <key,subtype> to aliases
     */
    public Relation<R2<String, String>, String> getBcp47Aliases() {
        return bcp47Aliases;
    }
}

