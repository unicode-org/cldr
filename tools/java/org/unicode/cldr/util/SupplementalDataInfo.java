package org.unicode.cldr.util;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.tool.SubdivisionNames;
import org.unicode.cldr.util.Builder.CBuilder;
import org.unicode.cldr.util.CldrUtility.VariableReplacer;
import org.unicode.cldr.util.DayPeriodInfo.DayPeriod;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData.Type;
import org.unicode.cldr.util.SupplementalDataInfo.NumberingSystemInfo.NumberingSystemType;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.Validity.Status;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.IterableComparator;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.FixedDecimal;
import com.ibm.icu.text.PluralRules.FixedDecimalRange;
import com.ibm.icu.text.PluralRules.FixedDecimalSamples;
import com.ibm.icu.text.PluralRules.SampleType;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Freezable;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

/**
 * Singleton class to provide API access to supplemental data -- in all the supplemental data files.
 * <p>
 * To create, use SupplementalDataInfo.getInstance
 * <p>
 * To add API for new structure, you will generally:
 * <ul>
 * <li>add a Map or Relation as a data member,
 * <li>put a check and handler in MyHandler for the paths that you consume,
 * <li>make the data member immutable in makeStuffSave, and
 * <li>add a getter for the data member
 * </ul>
 *
 * @author markdavis
 */

public class SupplementalDataInfo {
    private static final boolean DEBUG = false;
    private static final StandardCodes sc = StandardCodes.make();
    private static final String UNKNOWN_SCRIPT = "Zzzz";

    // TODO add structure for items shown by TestSupplementalData to be missing
    /*
     * [calendarData/calendar,
     * characters/character-fallback,
     * measurementData/measurementSystem, measurementData/paperSize,
     * metadata/attributeOrder, metadata/blocking, metadata/deprecated,
     * metadata/distinguishing, metadata/elementOrder, metadata/serialElements, metadata/skipDefaultLocale,
     * metadata/suppress, metadata/validity, metazoneInfo/timezone,
     * timezoneData/mapTimezones,
     * weekData/firstDay, weekData/minDays, weekData/weekendEnd, weekData/weekendStart]
     */
    // TODO: verify that we get everything by writing the files solely from the API, and verifying identity.

    /**
     * Official status of languages
     */
    public enum OfficialStatus {
        unknown("U", 1), recognized("R", 1), official_minority("OM", 2), official_regional("OR", 3), de_facto_official("OD", 10), official("O", 10);

        private final String shortName;
        private final int weight;

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
    public static final class PopulationData implements Freezable<PopulationData> {
        private double population = Double.NaN;

        private double literatePopulation = Double.NaN;

        private double writingPopulation = Double.NaN;

        private double gdp = Double.NaN;

        private OfficialStatus officialStatus = OfficialStatus.unknown;

        public double getGdp() {
            return gdp;
        }

        public double getLiteratePopulation() {
            return literatePopulation;
        }

        public double getLiteratePopulationPercent() {
            return 100 * literatePopulation / population;
        }

        public double getWritingPopulation() {
            return writingPopulation;
        }

        public double getWritingPercent() {
            return 100 * writingPopulation / population;
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
                writingPopulation = other.writingPopulation;
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
            writingPopulation += other.writingPopulation;
            gdp += other.gdp;
        }

        public String toString() {
            return MessageFormat
                .format(
                    "[pop: {0,number,#,##0},\t lit: {1,number,#,##0.00},\t gdp: {2,number,#,##0},\t status: {3}]",
                    new Object[] { population, literatePopulation, gdp, officialStatus });
        }

        private boolean frozen;

        public boolean isFrozen() {
            return frozen;
        }

        public PopulationData freeze() {
            frozen = true;
            return this;
        }

        public PopulationData cloneAsThawed() {
            throw new UnsupportedOperationException("not yet implemented");
        }

        public OfficialStatus getOfficialStatus() {
            return officialStatus;
        }

        public PopulationData setOfficialStatus(OfficialStatus officialStatus) {
            if (frozen) {
                throw new UnsupportedOperationException(
                    "Attempt to modify frozen object");
            }
            this.officialStatus = officialStatus;
            return this;
        }

        public PopulationData setWritingPopulation(double writingPopulation) {
            if (frozen) {
                throw new UnsupportedOperationException(
                    "Attempt to modify frozen object");
            }
            this.writingPopulation = writingPopulation;
            return this;
        }
    }

    static final Pattern WHITESPACE_PATTERN = PatternCache.get("\\s+");

    /**
     * Simple language/script/region information
     */
    public static class BasicLanguageData implements Comparable<BasicLanguageData>,
    com.ibm.icu.util.Freezable<BasicLanguageData> {
        public enum Type {
            primary, secondary
        };

        private Type type = Type.primary;

        private Set<String> scripts = Collections.emptySet();

        private Set<String> territories = Collections.emptySet();

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
            scripts = Collections.emptySet();
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
            territories = Collections.emptySet();
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

        public String toString() {
            return "[" + type
                + (scripts.isEmpty() ? "" : "; scripts=" + CollectionUtilities.join(scripts, " "))
                + (scripts.isEmpty() ? "" : "; territories=" + CollectionUtilities.join(territories, " "))
                + "]";
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

        public boolean equals(Object input) {
            return compareTo((BasicLanguageData) input) == 0;
        }

        @Override
        public int hashCode() {
            // TODO Auto-generated method stub
            return ((type.ordinal() * 37 + scripts.hashCode()) * 37) + territories.hashCode();
        }

        public BasicLanguageData addScript(String script) {
            // simple error checking
            if (script.length() != 4) {
                throw new IllegalArgumentException("Illegal Script: " + script);
            }
            if (scripts == Collections.EMPTY_SET) {
                scripts = new TreeSet<String>();
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
                territories = new TreeSet<String>();
            }
            territories.add(territory);
            return this;
        }

        boolean frozen = false;

        public boolean isFrozen() {
            return frozen;
        }

        public BasicLanguageData freeze() {
            frozen = true;
            if (scripts != Collections.EMPTY_SET) {
                scripts = Collections.unmodifiableSet(scripts);
            }
            if (territories != Collections.EMPTY_SET) {
                territories = Collections.unmodifiableSet(territories);
            }
            return this;
        }

        public BasicLanguageData cloneAsThawed() {
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
        public final int digits;
        public final int rounding;
        public final double roundingIncrement;
        public final int cashDigits;
        public final int cashRounding;
        public final double cashRoundingIncrement;

        public int getDigits() {
            return digits;
        }

        public int getRounding() {
            return rounding;
        }

        public double getRoundingIncrement() {
            return roundingIncrement;
        }

        public CurrencyNumberInfo(int _digits, int _rounding, int _cashDigits, int _cashRounding) {
            digits = _digits;
            rounding = _rounding < 0 ? 0 : _rounding;
            roundingIncrement = rounding * Math.pow(10.0, -digits);
            // if the values are not set, use the above values
            cashDigits = _cashDigits < 0 ? digits : _cashDigits;
            cashRounding = _cashRounding < 0 ? rounding : _cashRounding;
            cashRoundingIncrement = this.cashRounding * Math.pow(10.0, -digits);
        }
    }

    public static class NumberingSystemInfo {
        public enum NumberingSystemType {
            algorithmic, numeric, unknown
        };

        public final String name;
        public final NumberingSystemType type;
        public final String digits;
        public final String rules;

        public NumberingSystemInfo(XPathParts parts) {
            name = parts.getAttributeValue(-1, "id");
            digits = parts.getAttributeValue(-1, "digits");
            rules = parts.getAttributeValue(-1, "rules");
            type = NumberingSystemType.valueOf(parts.getAttributeValue(-1, "type"));
        }

    }

    /**
     * Class for a range of two dates, refactored to share code.
     *
     * @author markdavis
     */
    public static final class DateRange implements Comparable<DateRange> {
        public static final long START_OF_TIME = Long.MIN_VALUE;
        public static final long END_OF_TIME = Long.MAX_VALUE;
        public final long from;
        public final long to;

        public DateRange(String fromString, String toString) {
            from = parseDate(fromString, START_OF_TIME);
            to = parseDate(toString, END_OF_TIME);
        }

        public long getFrom() {
            return from;
        }

        public long getTo() {
            return to;
        }

        static final DateFormat[] simpleFormats = {
            new SimpleDateFormat("yyyy-MM-dd HH:mm"),
            new SimpleDateFormat("yyyy-MM-dd"),
            new SimpleDateFormat("yyyy-MM"),
            new SimpleDateFormat("yyyy"),
        };
        static {
            TimeZone gmt = TimeZone.getTimeZone("GMT");
            for (DateFormat format : simpleFormats) {
                format.setTimeZone(gmt);
            }
        }

        long parseDate(String dateString, long defaultDate) {
            if (dateString == null) {
                return defaultDate;
            }
            ParseException e2 = null;
            for (int i = 0; i < simpleFormats.length; ++i) {
                try {
                    synchronized (simpleFormats[i]) {
                        Date result = simpleFormats[i].parse(dateString);
                        return result.getTime();
                    }
                } catch (ParseException e) {
                    if (e2 == null) {
                        e2 = e;
                    }
                }
            }
            throw new IllegalArgumentException(e2);
        }

        public String toString() {
            return "{" + formatDate(from)
            + ", "
            + formatDate(to) + "}";
        }

        public static String formatDate(long date) {
            if (date == START_OF_TIME) {
                return "-∞";
            }
            if (date == END_OF_TIME) {
                return "∞";
            }
            synchronized (simpleFormats[0]) {
                return simpleFormats[0].format(date);
            }
        }

        @Override
        public int compareTo(DateRange arg0) {
            return to > arg0.to ? 1 : to < arg0.to ? -1 : from > arg0.from ? 1 : from < arg0.from ? -1 : 0;
        }
    }

    /**
     * Information about when currencies are in use in territories
     */
    public static class CurrencyDateInfo implements Comparable<CurrencyDateInfo> {

        public static final Date END_OF_TIME = new Date(DateRange.END_OF_TIME);
        public static final Date START_OF_TIME = new Date(DateRange.START_OF_TIME);

        private String currency;
        private DateRange dateRange;
        private boolean isLegalTender;
        private String errors = "";

        public CurrencyDateInfo(String currency, String startDate, String endDate, String tender) {
            this.currency = currency;
            this.dateRange = new DateRange(startDate, endDate);
            this.isLegalTender = (tender == null || !tender.equals("false"));
        }

        public String getCurrency() {
            return currency;
        }

        public Date getStart() {
            return new Date(dateRange.getFrom());
        }

        public Date getEnd() {
            return new Date(dateRange.getTo());
        }

        public String getErrors() {
            return errors;
        }

        public boolean isLegalTender() {
            return isLegalTender;
        }

        public int compareTo(CurrencyDateInfo o) {
            int result = dateRange.compareTo(o.dateRange);
            if (result != 0) return result;
            return currency.compareTo(o.currency);
        }

        public String toString() {
            return "{" + dateRange + ", " + currency + "}";
        }

        public static String formatDate(Date date) {
            return DateRange.formatDate(date.getTime());
        }

    }

    public static final class MetaZoneRange implements Comparable<MetaZoneRange> {
        public final DateRange dateRange;
        public final String metazone;

        /**
         * @param metazone
         * @param from
         * @param to
         */
        public MetaZoneRange(String metazone, String fromString, String toString) {
            super();
            this.metazone = metazone;
            dateRange = new DateRange(fromString, toString);
        }

        @Override
        public int compareTo(MetaZoneRange arg0) {
            int result;
            if (0 != (result = dateRange.compareTo(arg0.dateRange))) {
                return result;
            }
            return metazone.compareTo(arg0.metazone);
        }

        public String toString() {
            return "{" + dateRange + ", " + metazone + "}";
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
            this.code = code; // code will not be null
            this.start = parseDate(startDate, START_OF_TIME); // start will not be null
            this.end = parseDate(endDate, END_OF_TIME); // end willl not be null
            this.alt = (alt == null) ? "" : alt; // alt will not be null
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
            throw (IllegalArgumentException) new IllegalArgumentException().initCause(e2);
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
            TelephoneCodeInfo tc = (TelephoneCodeInfo) o;
            return tc.code.equals(code) && tc.start.equals(start) && tc.end.equals(end) && tc.alt.equals(alt);
        }

        public int hashCode() {
            return 31 * code.hashCode() + start.hashCode() + end.hashCode() + alt.hashCode();
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
        public final String match;
        public final Level value;
        public final Pattern inLanguage;
        public final String inScript;
        public final Set<String> inScriptSet;
        public final String inTerritory;
        public final Set<String> inTerritorySet;
        private Set<String> inTerritorySetInternal;

        public CoverageLevelInfo(String match, int value, String language, String script, String territory) {
            this.inLanguage = language != null ? PatternCache.get(language) : null;
            this.inScript = script;
            this.inTerritory = territory;
            this.inScriptSet = toSet(script);
            this.inTerritorySet = toSet(territory); // MUST BE LAST, sets inTerritorySetInternal
            this.match = match;
            this.value = Level.fromLevel(value);
        }

        public static final Pattern NON_ASCII_LETTER = PatternCache.get("[^A-Za-z]+");

        private Set<String> toSet(String source) {
            if (source == null) {
                return null;
            }
            Set<String> result = new HashSet<String>(Arrays.asList(NON_ASCII_LETTER.split(source)));
            result.remove("");
            inTerritorySetInternal = result;
            return Collections.unmodifiableSet(result);
        }

        public int compareTo(CoverageLevelInfo o) {
            if (value == o.value) {
                return match.compareTo(o.match);
            } else {
                return value.compareTo(o.value);
            }
        }

        static void fixEU(Collection<CoverageLevelInfo> targets, SupplementalDataInfo info) {
            Set<String> euCountries = info.getContained("EU");
            for (CoverageLevelInfo item : targets) {
                if (item.inTerritorySet != null
                    && item.inTerritorySet.contains("EU")) {
                    item.inTerritorySetInternal.addAll(euCountries);
                }
            }
        }
    }
    
    public static final String STAR = "*";
    public static final Set<String> STAR_SET = Builder.with(new HashSet<String>()).add("*").freeze();

    private VersionInfo cldrVersion;

    private Map<String, PopulationData> territoryToPopulationData = new TreeMap<String, PopulationData>();

    private Map<String, Map<String, PopulationData>> territoryToLanguageToPopulationData = new TreeMap<String, Map<String, PopulationData>>();

    private Map<String, PopulationData> languageToPopulation = new TreeMap<String, PopulationData>();

    private Map<String, PopulationData> baseLanguageToPopulation = new TreeMap<String, PopulationData>();

    private Relation<String, String> languageToScriptVariants = Relation.of(new TreeMap<String, Set<String>>(),
        TreeSet.class);

    private Relation<String, String> languageToTerritories = Relation.of(new TreeMap<String, Set<String>>(),
        LinkedHashSet.class);

    transient private Relation<String, Pair<Boolean, Pair<Double, String>>> languageToTerritories2 = Relation
        .of(new TreeMap<String, Set<Pair<Boolean, Pair<Double, String>>>>(), TreeSet.class);

    private Map<String, Map<BasicLanguageData.Type, BasicLanguageData>> languageToBasicLanguageData = new TreeMap<String, Map<BasicLanguageData.Type, BasicLanguageData>>();

    // private Map<String, BasicLanguageData> languageToBasicLanguageData2 = new
    // TreeMap();

    // Relation(new TreeMap(), TreeSet.class, null);

    private Set<String> allLanguages = new TreeSet<String>();
    final private List<String> approvalRequirements = new LinkedList<String>(); // xpath array

    private Relation<String, String> containment = Relation.of(new LinkedHashMap<String, Set<String>>(),
        LinkedHashSet.class);
    private Relation<String, String> containmentCore = Relation.of(new LinkedHashMap<String, Set<String>>(),
        LinkedHashSet.class);
    //    private Relation<String, String> containmentNonDeprecated = Relation.of(new LinkedHashMap<String, Set<String>>(),
    //        LinkedHashSet.class);
    private Relation<String, String> containmentGrouping = Relation.of(new LinkedHashMap<String, Set<String>>(),
        LinkedHashSet.class);
    private Relation<String, String> containmentDeprecated = Relation.of(new LinkedHashMap<String, Set<String>>(),
        LinkedHashSet.class);
    private Relation<String, String> containerToSubdivision = Relation.of(new LinkedHashMap<String, Set<String>>(),
        LinkedHashSet.class);

    private Map<String, CurrencyNumberInfo> currencyToCurrencyNumberInfo = new TreeMap<String, CurrencyNumberInfo>();

    private Relation<String, CurrencyDateInfo> territoryToCurrencyDateInfo = Relation.of(
        new TreeMap<String, Set<CurrencyDateInfo>>(), LinkedHashSet.class);

    // private Relation<String, TelephoneCodeInfo> territoryToTelephoneCodeInfo = new Relation(new TreeMap(),
    // LinkedHashSet.class);
    private Map<String, Set<TelephoneCodeInfo>> territoryToTelephoneCodeInfo = new TreeMap<String, Set<TelephoneCodeInfo>>();

    private Set<String> multizone = new TreeSet<String>();

    private Map<String, String> zone_territory = new TreeMap<String, String>();

    private Relation<String, String> zone_aliases = Relation
        .of(new TreeMap<String, Set<String>>(), LinkedHashSet.class);

    private Map<String, Map<String, Map<String, String>>> typeToZoneToRegionToZone = new TreeMap<String, Map<String, Map<String, String>>>();
    private Relation<String, MetaZoneRange> zoneToMetaZoneRanges = Relation.of(
        new TreeMap<String, Set<MetaZoneRange>>(), TreeSet.class);
//    private Map<String, Map<String, Relation<String, String>>> deprecated = new HashMap<String, Map<String, Relation<String, String>>>();

    private Map<String, String> metazoneContinentMap = new HashMap<String, String>();
    private Set<String> allMetazones = new TreeSet<String>();

    private Map<String, String> alias_zone = new TreeMap<String, String>();

    public Relation<String, Integer> numericTerritoryMapping = Relation.of(new HashMap<String, Set<Integer>>(),
        HashSet.class);

    public Relation<String, String> alpha3TerritoryMapping = Relation.of(new HashMap<String, Set<String>>(),
        HashSet.class);

    public Relation<String, Integer> numericCurrencyCodeMapping = Relation.of(new HashMap<String, Set<Integer>>(),
        HashSet.class);

    static Map<String, SupplementalDataInfo> directory_instance = new HashMap<String, SupplementalDataInfo>();

    public Map<String, Map<String, Row.R2<List<String>, String>>> typeToTagToReplacement = new TreeMap<String, Map<String, Row.R2<List<String>, String>>>();

    Map<String, List<Row.R4<String, String, Integer, Boolean>>> languageMatch = new HashMap<String, List<Row.R4<String, String, Integer, Boolean>>>();

    public Relation<String, String> bcp47Key2Subtypes = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);
    public Relation<String, String> bcp47Extension2Keys = Relation
        .of(new TreeMap<String, Set<String>>(), TreeSet.class);
    public Relation<Row.R2<String, String>, String> bcp47Aliases = Relation.of(
        new TreeMap<Row.R2<String, String>, Set<String>>(), LinkedHashSet.class);
    public Map<Row.R2<String, String>, String> bcp47Descriptions = new TreeMap<Row.R2<String, String>, String>();
    public Map<Row.R2<String, String>, String> bcp47Since = new TreeMap<Row.R2<String, String>, String>();
    public Map<Row.R2<String, String>, String> bcp47Preferred = new TreeMap<Row.R2<String, String>, String>();
    public Map<Row.R2<String, String>, String> bcp47Deprecated = new TreeMap<Row.R2<String, String>, String>();
    public Map<String, String> bcp47ValueType = new TreeMap<String, String>();
    

    public Map<String, Row.R2<String, String>> validityInfo = new LinkedHashMap<String, Row.R2<String, String>>();
    public Map<AttributeValidityInfo, String> attributeValidityInfo = new LinkedHashMap<>();

    public Multimap<String, String> languageGroups = TreeMultimap.create();

    public enum MeasurementType {
        measurementSystem, paperSize
    }

    Map<MeasurementType, Map<String, String>> measurementData = new HashMap<MeasurementType, Map<String, String>>();
    Map<String, PreferredAndAllowedHour> timeData = new HashMap<String, PreferredAndAllowedHour>();

    public Relation<String, String> getAlpha3TerritoryMapping() {
        return alpha3TerritoryMapping;
    }

    public Relation<String, Integer> getNumericTerritoryMapping() {
        return numericTerritoryMapping;
    }

    public Relation<String, Integer> getNumericCurrencyCodeMapping() {
        return numericCurrencyCodeMapping;
    }

    /**
     * Returns type -> tag -> <replacementList, reason>, like "language" -> "sh" -> <{"sr_Latn"}, reason>
     *
     * @return
     */
    public Map<String, Map<String, R2<List<String>, String>>> getLocaleAliasInfo() {
        return typeToTagToReplacement;
    }

    public R2<List<String>, String> getDeprecatedInfo(String type, String code) {
        return typeToTagToReplacement.get(type).get(code);
    }

    public static SupplementalDataInfo getInstance(File supplementalDirectory) {
        try {
            return getInstance(supplementalDirectory.getCanonicalPath());
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    static private SupplementalDataInfo defaultInstance = null;
    /**
     * Which directory did we come from?
     */
    final private File directory;

    /**
     * Get an instance chosen using setAsDefaultInstance(), otherwise return an instance using the default directory
     * CldrUtility.SUPPLEMENTAL_DIRECTORY
     *
     * @return
     */
    public static SupplementalDataInfo getInstance() {
        if (defaultInstance != null) {
            return defaultInstance;
        }
        /*
         * TODO: fix re-entrance problems involving LanguageTagParser:
         * SupplementalDataInfo.getInstance(String) line: 1050  
         * CLDRConfig.getSupplementalDataInfo() line: 215  
         * SupplementalDataInfo.getInstance() line: 977    
         * LanguageTagParser.<clinit>() line: 194  
         * SupplementalDataInfo$MyHandler.<init>(SupplementalDataInfo) line: 1195  
         * SupplementalDataInfo.getInstance(String) line: 1019 
         * CLDRConfig.getSupplementalDataInfo() line: 215  
         * SupplementalDataInfo.getInstance()
         */
        return CLDRConfig.getInstance().getSupplementalDataInfo();
        // return getInstance(CldrUtility.SUPPLEMENTAL_DIRECTORY);
    }

    /**
     * Mark this as the default instance to be returned by getInstance()
     */
    public void setAsDefaultInstance() {
        defaultInstance = this;
    }

    public static SupplementalDataInfo getInstance(String supplementalDirectory) {
        synchronized (SupplementalDataInfo.class) {
            // Sanity checks - not null, not empty
            if (supplementalDirectory == null) {
                throw new IllegalArgumentException("Error: null supplemental directory.");
            }
            if (supplementalDirectory.isEmpty()) {
                throw new IllegalArgumentException("Error: The string passed as a parameter resolves to the empty string.");
            }
            // canonicalize path
            String canonicalpath = null;
            try {
                canonicalpath = new File(supplementalDirectory).getCanonicalPath();
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
            SupplementalDataInfo instance = directory_instance.get(canonicalpath);
            if (instance != null) {
                return instance;
            }
            // reaching here means we have not cached the entry
            File directory = new File(canonicalpath);
            instance = new SupplementalDataInfo(directory);
            MyHandler myHandler = instance.new MyHandler();
            XMLFileReader xfr = new XMLFileReader().setHandler(myHandler);
            File files1[] = directory.listFiles();
            if (files1 == null || files1.length == 0) {
                throw new ICUUncheckedIOException("Error: Supplemental files missing from " + directory.getAbsolutePath());
            }
            // get bcp47 files also
            File bcp47dir = instance.getBcp47Directory();
            if (!bcp47dir.isDirectory()) {
                throw new ICUUncheckedIOException("Error: BCP47 dir is not a directory: " + bcp47dir.getAbsolutePath());
            }
            File files2[] = bcp47dir.listFiles();
            if (files2 == null || files2.length == 0) {
                throw new ICUUncheckedIOException("Error: BCP47 files missing from " + bcp47dir.getAbsolutePath());
            }

            CBuilder<File, ArrayList<File>> builder = Builder.with(new ArrayList<File>());
            builder.addAll(files1);
            builder.addAll(files2);
            for (File file : builder.get()) {
                if (DEBUG) {
                    try {
                        System.out.println(file.getCanonicalPath());
                    } catch (IOException e) {
                    }
                }
                String name = file.toString();
                String shortName = file.getName();
                if (!shortName.endsWith(".xml") || // skip non-XML
                    shortName.startsWith("#") || // skip other junk files
                    shortName.startsWith(".")) continue; // skip dot files (backups, etc)
                xfr.read(name, -1, true);
                myHandler.cleanup();
            }

            // xfr = new XMLFileReader().setHandler(instance.new MyHandler());
            // .xfr.read(canonicalpath + "/supplementalMetadata.xml", -1, true);

            instance.makeStuffSafe();
            // cache
            //            directory_instance.put(supplementalDirectory, instance);
            directory_instance.put(canonicalpath, instance);
            //            if (!canonicalpath.equals(supplementalDirectory)) {
            //                directory_instance.put(canonicalpath, instance);
            //            }
            return instance;
        }
    }

    private File getBcp47Directory() {
        return new File(getDirectory().getParent(), "bcp47");
    }

    private SupplementalDataInfo(File directory) {
        this.directory = directory;
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
        // territoryToTelephoneCodeInfo.freeze();
        territoryToTelephoneCodeInfo = Collections.unmodifiableMap(territoryToTelephoneCodeInfo);

        typeToZoneToRegionToZone = CldrUtility.protectCollection(typeToZoneToRegionToZone);
        typeToTagToReplacement = CldrUtility.protectCollection(typeToTagToReplacement);

        zoneToMetaZoneRanges.freeze();

        containment.freeze();
        containmentCore.freeze();
        //        containmentNonDeprecated.freeze();
        containmentGrouping.freeze();
        containmentDeprecated.freeze();

        containerToSubdivision.freeze();

        CldrUtility.protectCollection(languageToBasicLanguageData);
        for (String language : languageToTerritories2.keySet()) {
            for (Pair<Boolean, Pair<Double, String>> pair : languageToTerritories2.getAll(language)) {
                languageToTerritories.put(language, pair.getSecond().getSecond());
            }
        }
        languageToTerritories2 = null; // free up the memory.
        languageToTerritories.freeze();
        zone_aliases.freeze();
        languageToScriptVariants.freeze();

        numericTerritoryMapping.freeze();
        alpha3TerritoryMapping.freeze();
        numericCurrencyCodeMapping.freeze();

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
        localeToPluralInfo2.put(PluralType.cardinal, Collections.unmodifiableMap(localeToPluralInfo2.get(PluralType.cardinal)));
        localeToPluralInfo2.put(PluralType.ordinal, Collections.unmodifiableMap(localeToPluralInfo2.get(PluralType.ordinal)));

        localeToPluralRanges = Collections.unmodifiableMap(localeToPluralRanges);
        for (PluralRanges pluralRanges : localeToPluralRanges.values()) {
            pluralRanges.freeze();
        }

        if (lastDayPeriodLocales != null) {
            addDayPeriodInfo();
        }
        typeToLocaleToDayPeriodInfo = CldrUtility.protectCollection(typeToLocaleToDayPeriodInfo);
        languageMatch = CldrUtility.protectCollection(languageMatch);
        bcp47Key2Subtypes.freeze();
        bcp47Extension2Keys.freeze();
        bcp47Aliases.freeze();
        if (bcp47Key2Subtypes.isEmpty()) {
            throw new InternalError("No BCP47 key 2 subtype data was loaded from bcp47 dir " + getBcp47Directory().getAbsolutePath());
        }
        CldrUtility.protectCollection(bcp47Descriptions);
        CldrUtility.protectCollection(bcp47Since);
        CldrUtility.protectCollection(bcp47Preferred);
        CldrUtility.protectCollection(bcp47Deprecated);
        CldrUtility.protectCollection(bcp47ValueType);

        CoverageLevelInfo.fixEU(coverageLevels, this);
        coverageLevels = Collections.unmodifiableSortedSet(coverageLevels);

        measurementData = CldrUtility.protectCollection(measurementData);
        timeData = CldrUtility.protectCollection(timeData);

        validityInfo = CldrUtility.protectCollection(validityInfo);
        attributeValidityInfo = CldrUtility.protectCollection(attributeValidityInfo);
        parentLocales = Collections.unmodifiableMap(parentLocales);
        languageGroups = ImmutableSetMultimap.copyOf(languageGroups);

        ImmutableSet.Builder<String> newScripts = ImmutableSet.<String> builder();
        Map<Validity.Status, Set<String>> scripts = Validity.getInstance().getStatusToCodes(LstrType.script);
        for (Entry<Status, Set<String>> e : scripts.entrySet()) {
            switch (e.getKey()) {
            case regular:
            case special:
            case unknown:
                newScripts.addAll(e.getValue());
                break;
            default:
                break; // do nothing
            }
        }
        CLDRScriptCodes = newScripts.build();
    }

    /**
     * Core function used to process each of the paths, and add the data to the appropriate data member.
     */
    class MyHandler extends XMLFileReader.SimpleHandler {
        private static final double MAX_POPULATION = 3000000000.0;

        LanguageTagParser languageTagParser = new LanguageTagParser();

        /**
         * Finish processing anything left hanging in the file.
         */
        public void cleanup() {
            if (lastPluralMap.size() > 0) {
                addPluralInfo(lastPluralWasOrdinal);
            }
            lastPluralLocales = "";
        }

        public void handlePathValue(String path, String value) {
            try {
                XPathParts parts = XPathParts.getTestInstance(path);
                String level0 = parts.getElement(0);
                String level1 = parts.size() < 2 ? null : parts.getElement(1);
                String level2 = parts.size() < 3 ? null : parts.getElement(2);
                String level3 = parts.size() < 4 ? null : parts.getElement(3);
                // String level4 = parts.size() < 5 ? null : parts.getElement(4);
                if (level1.equals("generation")) {
                    // skip
                    return;
                }
                if (level1.equals("version")) {
                    if (cldrVersion == null) {
                        String version = parts.getAttributeValue(1, "cldrVersion");
                        if (version == null) {
                            // old format
                            version = parts.getAttributeValue(0, "version");
                        }
                        cldrVersion = VersionInfo.getInstance(version);
                    }
                    return;
                }

                // copy the rest from ShowLanguages later
                if (level0.equals("ldmlBCP47")) {
                    if (handleBcp47(level1, level2, parts)) {
                        return;
                    }
                } else if (level1.equals("territoryInfo")) {
                    if (handleTerritoryInfo(parts)) {
                        return;
                    }
                } else if (level1.equals("calendarPreferenceData")) {
                    handleCalendarPreferenceData(parts);
                    return;
                } else if (level1.equals("languageData")) {
                    handleLanguageData(parts);
                    return;
                } else if (level1.equals("territoryContainment")) {
                    handleTerritoryContainment(parts);
                    return;
                } else if (level1.equals("subdivisionContainment")) {
                    handleSubdivisionContainment(parts);
                    return;
                } else if (level1.equals("currencyData")) {
                    if (handleCurrencyData(level2, parts)) {
                        return;
                    }
                 } else if ("metazoneInfo".equals(level2)) {
                    if (handleMetazoneInfo(level2, level3, parts)) {
                        return;
                    }
                } else if ("mapTimezones".equals(level2)) {
                    if (handleMetazoneData(level2, level3, parts)) {
                        return;
                    }
                } else if (level1.equals("plurals")) {
                    if (addPluralPath(parts, value)) {
                        return;
                    }
                } else if (level1.equals("dayPeriodRuleSet")) {
                    addDayPeriodPath(parts, value);
                    return;
                } else if (level1.equals("telephoneCodeData")) {
                    handleTelephoneCodeData(parts);
                    return;
                } else if (level1.equals("references")) {
                    String type = parts.getAttributeValue(-1, "type");
                    String uri = parts.getAttributeValue(-1, "uri");
                    references.put(type, new Pair<String, String>(uri, value).freeze());
                    return;
                } else if (level1.equals("likelySubtags")) {
                    handleLikelySubtags(parts);
                    return;
                } else if (level1.equals("numberingSystems")) {
                    handleNumberingSystems(parts);
                    return;
                } else if (level1.equals("coverageLevels")) {
                    handleCoverageLevels(parts);
                    return;
                } else if (level1.equals("parentLocales")) {
                    handleParentLocales(parts);
                    return;
                } else if (level1.equals("metadata")) {
                    if (handleMetadata(level2, value, parts)) {
                        return;
                    }
                } else if (level1.equals("codeMappings")) {
                    if (handleCodeMappings(level2, parts)) {
                        return;
                    }
                } else if (level1.equals("languageMatching")) {
                    if (handleLanguageMatcher(level2, parts)) {
                        return;
                    }
                } else if (level1.equals("measurementData")) {
                    if (handleMeasurementData(level2, parts)) {
                        return;
                    }
                } else if (level1.equals("timeData")) {
                    if (handleTimeData(level2, parts)) {
                        return;
                    }
                } else if (level1.equals("languageGroups")) {
                    if (handleLanguageGroups(level2, value, parts)) {
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
                throw (IllegalArgumentException) new IllegalArgumentException("Exception while processing path: "
                    + path + ",\tvalue: " + value).initCause(e);
            }
        }

        private boolean handleLanguageGroups(String level2, String value, XPathParts parts) {
            String parent = parts.getAttributeValue(-1, "parent");
            List<String> children = WHITESPACE_SPLTTER.splitToList(value);
            languageGroups.putAll(parent, children);
            return true;
        }

        private boolean handleMeasurementData(String level2, XPathParts parts) {
            /**
             * <measurementSystem type="US" territories="LR MM US"/>
             * <paperSize type="A4" territories="001"/>
             */
            MeasurementType measurementType = MeasurementType.valueOf(level2);
            String type = parts.getAttributeValue(-1, "type");
            String territories = parts.getAttributeValue(-1, "territories");
            Map<String, String> data = measurementData.get(measurementType);
            if (data == null) {
                measurementData.put(measurementType, data = new HashMap<String, String>());
            }
            for (String territory : territories.trim().split("\\s+")) {
                data.put(territory, type);
            }
            return true;
        }

        private boolean handleTimeData(String level2, XPathParts parts) {
            /**
             * <hours preferred="H" allowed="H" regions="IL RU"/>
             */
            String preferred = parts.getAttributeValue(-1, "preferred");
            // String[] allowed = parts.getAttributeValue(-1, "allowed").trim().split("\\s+");
            PreferredAndAllowedHour preferredAndAllowedHour = new PreferredAndAllowedHour(preferred,
                parts.getAttributeValue(-1, "allowed"));
            for (String region : parts.getAttributeValue(-1, "regions").trim().split("\\s+")) {
                PreferredAndAllowedHour oldValue = timeData.put(region, preferredAndAllowedHour);
                if (oldValue != null) {
                    throw new IllegalArgumentException("timeData/hours must not have duplicate regions: " + region);
                }
            }
            return true;
        }

        private boolean handleBcp47(String level1, String level2, XPathParts parts) {
            if (level1.equals("version") || level1.equals("generation") || level1.equals("cldrVersion")) {
                return true; // skip
            }
            if (!level1.equals("keyword")) {
                throw new IllegalArgumentException("Unexpected level1 element: " + level1);
            }

            String finalElement = parts.getElement(-1);
            String key = parts.getAttributeValue(2, "name");
            String extension = parts.getAttributeValue(2, "extension");
            if (extension == null) {
                extension = "u";
            }
            bcp47Extension2Keys.put(extension, key);

            String keyAlias = parts.getAttributeValue(2, "alias");
            String keyDescription = parts.getAttributeValue(2, "description");
            String deprecated = parts.getAttributeValue(2, "deprecated");
            // TODO add preferred, valueType, since

            final R2<String, String> key_empty = (R2<String, String>) Row.of(key, "").freeze();

            if (keyAlias != null) {
                bcp47Aliases.putAll(key_empty, Arrays.asList(keyAlias.trim().split("\\s+")));
            }

            if (keyDescription != null) {
                bcp47Descriptions.put(key_empty, keyDescription);
            }
            if (deprecated != null && deprecated.equals("true")) {
                bcp47Deprecated.put(key_empty, deprecated);
            }

            switch (finalElement) {
            case "key":
                break; // all actions taken above

            case "type":
                String subtype = parts.getAttributeValue(3, "name");
                String subtypeAlias = parts.getAttributeValue(3, "alias");
                String desc = parts.getAttributeValue(3, "description");
                String subtypeDescription = desc == null ? null : desc.replaceAll("\\s+", " ");
                String subtypeSince = parts.getAttributeValue(3, "since");
                String subtypePreferred = parts.getAttributeValue(3, "preferred");
                String subtypeDeprecated = parts.getAttributeValue(3, "deprecated");
                String valueType = parts.getAttributeValue(3, "deprecated");

                Set<String> set = bcp47Key2Subtypes.get(key);
                if (set != null && set.contains(key)) {
                    throw new IllegalArgumentException("Collision with bcp47 key-value: " + key + "," + subtype);
                }
                bcp47Key2Subtypes.put(key, subtype);

                final R2<String, String> key_subtype = (R2<String, String>) Row.of(key, subtype).freeze();

                if (subtypeAlias != null) {
                    bcp47Aliases.putAll(key_subtype, Arrays.asList(subtypeAlias.trim().split("\\s+")));
                }
                if (subtypeDescription != null) {
                    bcp47Descriptions.put(key_subtype, subtypeDescription.replaceAll("\\s+", " "));
                }
                if (subtypeDescription != null) {
                    bcp47Since.put(key_subtype, subtypeSince);
                }
                if (subtypePreferred != null) {
                    bcp47Preferred.put(key_subtype, subtypePreferred);
                }
                if (subtypeDeprecated != null) {
                    bcp47Deprecated.put(key_subtype, subtypeDeprecated);
                }
                if (valueType != null) {
                    bcp47ValueType.put(subtype, valueType);
                }
                break;
            default:
                throw new IllegalArgumentException("Unexpected element: " + finalElement);
            }

            return true;
        }

        private boolean handleLanguageMatcher(String level2, XPathParts parts) {
            String type = parts.getAttributeValue(2, "type");
            String alt = parts.getAttributeValue(2, "alt");
            if (alt != null) {
                type += "_" + alt;
            }
            switch (parts.getElement(3)) {
            case "paradigmLocales":
                List<String> locales = WHITESPACE_SPLTTER.splitToList(parts.getAttributeValue(3, "locales"));
                // TODO
//                LanguageMatchData languageMatchData = languageMatchData.get(type);
//                if (languageMatchData == null) {
//                    languageMatch.put(type, languageMatchData = new LanguageMatchData());
//                }
                break;
            case "matchVariable":
                String id = parts.getAttributeValue(3, "id");
                String value = parts.getAttributeValue(3, "value");
                // TODO
                break;
            case "languageMatch":
                List<R4<String, String, Integer, Boolean>> matches = languageMatch.get(type);
                if (matches == null) {
                    languageMatch.put(type, matches = new ArrayList<R4<String, String, Integer, Boolean>>());
                }
                String percent = parts.getAttributeValue(3, "percent");
                String distance = parts.getAttributeValue(3, "distance");
                matches.add(Row.of(
                    parts.getAttributeValue(3, "desired"),
                    parts.getAttributeValue(3, "supported"),
                    percent != null ? Integer.parseInt(percent)
                        : 100 - Integer.parseInt(distance),
                        "true".equals(parts.getAttributeValue(3, "oneway"))));
                break;
            default:
                throw new IllegalArgumentException("Unknown element");
            }
            return true;
        }

        private boolean handleCodeMappings(String level2, XPathParts parts) {
            if (level2.equals("territoryCodes")) {
                // <territoryCodes type="VU" numeric="548" alpha3="VUT"/>
                String type = parts.getAttributeValue(-1, "type");
                final String numeric = parts.getAttributeValue(-1, "numeric");
                if (numeric != null) {
                    numericTerritoryMapping.put(type, Integer.parseInt(numeric));
                }
                final String alpha3 = parts.getAttributeValue(-1, "alpha3");
                if (alpha3 != null) {
                    alpha3TerritoryMapping.put(type, alpha3);
                }
                return true;
            } else if (level2.equals("currencyCodes")) {
                // <currencyCodes type="BBD" numeric="52"/>
                String type = parts.getAttributeValue(-1, "type");
                final String numeric = parts.getAttributeValue(-1, "numeric");
                if (numeric != null) {
                    numericCurrencyCodeMapping.put(type, Integer.parseInt(numeric));
                }
                return true;
            }
            return false;
        }

        private void handleNumberingSystems(XPathParts parts) {
            NumberingSystemInfo ns = new NumberingSystemInfo(parts);
            numberingSystems.put(ns.name, ns);
            if (ns.type == NumberingSystemType.numeric) {
                numericSystems.add(ns.name);
            }
        }

        private void handleCoverageLevels(XPathParts parts) {
            if (parts.containsElement("approvalRequirement")) {
                approvalRequirements.add(parts.toString());
            } else if (parts.containsElement("coverageLevel")) {
                String match = parts.containsAttribute("match") ? coverageVariables.replace(parts.getAttributeValue(-1,
                    "match")) : null;
                String valueStr = parts.getAttributeValue(-1, "value");
                // Ticket 7125: map the number to English. So switch from English to number for construction
                valueStr = Integer.toString(Level.get(valueStr).getLevel());

                String inLanguage = parts.containsAttribute("inLanguage") ? coverageVariables.replace(parts
                    .getAttributeValue(-1, "inLanguage")) : null;
                String inScript = parts.containsAttribute("inScript") ? coverageVariables.replace(parts
                    .getAttributeValue(-1, "inScript")) : null;
                String inTerritory = parts.containsAttribute("inTerritory") ? coverageVariables.replace(parts
                    .getAttributeValue(-1, "inTerritory")) : null;
                Integer value = (valueStr != null) ? Integer.valueOf(valueStr) : Integer.valueOf("101");
                if (cldrVersion.getMajor() < 2) {
                    value = 40;
                }
                CoverageLevelInfo ci = new CoverageLevelInfo(match, value, inLanguage, inScript, inTerritory);
                coverageLevels.add(ci);
            } else if (parts.containsElement("coverageVariable")) {
                String key = parts.getAttributeValue(-1, "key");
                String value = parts.getAttributeValue(-1, "value");
                coverageVariables.add(key, value);
            }
        }

        private void handleParentLocales(XPathParts parts) {
            String parent = parts.getAttributeValue(-1, "parent");
            String locales = parts.getAttributeValue(-1, "locales");
            String[] pl = locales.split(" ");
            for (int i = 0; i < pl.length; i++) {
                parentLocales.put(pl[i], parent);
            }
        }

        private void handleCalendarPreferenceData(XPathParts parts) {
            String territoryString = parts.getAttributeValue(-1, "territories");
            String orderingString = parts.getAttributeValue(-1, "ordering");
            String[] calendars = orderingString.split(" ");
            String[] territories = territoryString.split(" ");
            List<String> calendarList = Arrays.asList(calendars);
            for (int i = 0; i < territories.length; i++) {
                calendarPreferences.put(territories[i], calendarList);
            }
        }

        private void handleLikelySubtags(XPathParts parts) {
            String from = parts.getAttributeValue(-1, "from");
            String to = parts.getAttributeValue(-1, "to");
            likelySubtags.put(from, to);
        }

        /**
         * Only called if level2 = mapTimezones. Level 1 might be metaZones or might be windowsZones
         */
        private boolean handleMetazoneData(String level2, String level3, XPathParts parts) {
            if (level3.equals("mapZone")) {
                String maintype = parts.getAttributeValue(2, "type");
                if (maintype == null) {
                    maintype = "windows";
                }
                String mzone = parts.getAttributeValue(3, "other");
                String region = parts.getAttributeValue(3, "territory");
                String zone = parts.getAttributeValue(3, "type");

                Map<String, Map<String, String>> zoneToRegionToZone = typeToZoneToRegionToZone.get(maintype);
                if (zoneToRegionToZone == null) {
                    typeToZoneToRegionToZone.put(maintype,
                        zoneToRegionToZone = new TreeMap<String, Map<String, String>>());
                }
                Map<String, String> regionToZone = zoneToRegionToZone.get(mzone);
                if (regionToZone == null) {
                    zoneToRegionToZone.put(mzone, regionToZone = new TreeMap<String, String>());
                }
                if (region != null) {
                    regionToZone.put(region, zone);
                }
                if (maintype.equals("metazones")) {
                    if (mzone != null && region.equals("001")) {
                        metazoneContinentMap.put(mzone, zone.substring(0, zone.indexOf("/")));
                    }
                    allMetazones.add(mzone);
                }
                return true;
            }
            return false;
        }

        private Collection<String> getSpaceDelimited(int index, String attribute, Collection<String> defaultValue, XPathParts parts) {
            String temp = parts.getAttributeValue(index, attribute);
            Collection<String> elements = temp == null ? defaultValue : Arrays.asList(temp.split("\\s+"));
            return elements;
        }

        /*
         *
         * <supplementalData>
         * <metaZones>
         * <metazoneInfo>
         * <timezone type="Asia/Yerevan">
         * <usesMetazone to="1991-09-22 20:00" mzone="Yerevan"/>
         * <usesMetazone from="1991-09-22 20:00" mzone="Armenia"/>
         */

        private boolean handleMetazoneInfo(String level2, String level3, XPathParts parts) {
            if (level3.equals("timezone")) {
                String zone = parts.getAttributeValue(3, "type");
                String mzone = parts.getAttributeValue(4, "mzone");
                String from = parts.getAttributeValue(4, "from");
                String to = parts.getAttributeValue(4, "to");
                MetaZoneRange mzoneRange = new MetaZoneRange(mzone, from, to);
                zoneToMetaZoneRanges.put(zone, mzoneRange);
                return true;
            }
            return false;
        }

        private boolean handleMetadata(String level2, String value, XPathParts parts) {
            if (parts.contains("defaultContent")) {
                String defContent = parts.getAttributeValue(-1, "locales").trim();
                String[] defLocales = defContent.split("\\s+");
                defaultContentLocales = Collections.unmodifiableSet(new TreeSet<String>(Arrays.asList(defLocales)));
                return true;
            }
            if (level2.equals("alias")) {
                // <alias>
                // <!-- grandfathered 3066 codes -->
                // <languageAlias type="art-lojban" replacement="jbo"/> <!-- Lojban -->
                String level3 = parts.getElement(3);
                if (!level3.endsWith("Alias")) {
                    throw new IllegalArgumentException();
                }
                level3 = level3.substring(0, level3.length() - "Alias".length());
                boolean isSubdivision = level3.equals("subdivision");
                Map<String, R2<List<String>, String>> tagToReplacement = typeToTagToReplacement.get(level3);
                if (tagToReplacement == null) {
                    typeToTagToReplacement.put(level3,
                        tagToReplacement = new TreeMap<String, R2<List<String>, String>>());
                }
                final String replacement = parts.getAttributeValue(3, "replacement");
                List<String> replacementList = null;
                if (replacement != null) {
                    Set<String> builder = new LinkedHashSet<>();
                    for (String item : replacement.split("\\s+")) {
                        String cleaned = SubdivisionNames.isOldSubdivisionCode(item) 
                            ? replacement.replace("-", "").toLowerCase(Locale.ROOT)
                                : item;
                        builder.add(cleaned);
                    }
                    replacementList = ImmutableList.copyOf(builder);
                }
                final String reason = parts.getAttributeValue(3, "reason");
                String cleanTag = parts.getAttributeValue(3, "type");
                tagToReplacement.put(cleanTag, (R2<List<String>, String>) Row.of(replacementList, reason).freeze());
                return true;
            } else if (level2.equals("validity")) {
                // <variable id="$grandfathered" type="choice">
                String level3 = parts.getElement(3);
                if (level3.equals("variable")) {
                    Map<String, String> attributes = parts.getAttributes(-1);
                    validityInfo.put(attributes.get("id"), Row.of(attributes.get("type"), value));
                    String idString = attributes.get("id");
                    if (("$language".equals(idString) || "$languageExceptions".equals(attributes.get("id")))
                        && "choice".equals(attributes.get("type"))) {
                        String[] validCodeArray = value.trim().split("\\s+");
                        CLDRLanguageCodes.addAll(Arrays.asList(validCodeArray));
                    }
                    return true;
                } else if (level3.equals("attributeValues")) {
                    AttributeValidityInfo.add(parts.getAttributes(-1), value, attributeValidityInfo);
                    return true;
                }
            } else if (level2.equals("serialElements")) {
                serialElements = Arrays.asList(value.trim().split("\\s+"));
                return true;
            } else if (level2.equals("distinguishing")) {
                String level3 = parts.getElement(3);
                if (level3.equals("distinguishingItems")) {
                    Map<String, String> attributes = parts.getAttributes(-1);
                    // <distinguishingItems
                    // attributes="key request id _q registry alt iso4217 iso3166 mzone from to type numberSystem"/>
                    // <distinguishingItems exclude="true"
                    // elements="default measurementSystem mapping abbreviationFallback preferenceOrdering"
                    // attributes="type"/>

                    if (attributes.containsKey("exclude") && "true".equals(attributes.get("exclude"))) {
                        return false; // don't handle the excludes -yet.
                    } else {
                        distinguishingAttributes = Collections.unmodifiableCollection(getSpaceDelimited(-1,
                            "attributes", STAR_SET, parts));
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean handleTerritoryInfo(XPathParts parts) {

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
                double languageLiteracyPercent = parseDouble(languageInTerritoryAttributes.get("literacyPercent"));
                if (Double.isNaN(languageLiteracyPercent)) {
                    languageLiteracyPercent = territoryLiteracyPercent;
                }
                double writingPercent = parseDouble(languageInTerritoryAttributes.get("writingPercent"));
                if (Double.isNaN(writingPercent)) {
                    writingPercent = languageLiteracyPercent;
                }
                // else {
                // System.out.println("writingPercent\t" + languageLiteracyPercent
                // + "\tterritory\t" + territory
                // + "\tlanguage\t" + language);
                // }
                double languagePopulationPercent = parseDouble(languageInTerritoryAttributes.get("populationPercent"));
                double languagePopulation = languagePopulationPercent * territoryPopulation / 100;
                // double languageGdp = languagePopulationPercent * territoryGdp;

                // store
                Map<String, PopulationData> territoryLanguageToPopulation = territoryToLanguageToPopulationData
                    .get(territory);
                if (territoryLanguageToPopulation == null) {
                    territoryToLanguageToPopulationData.put(territory,
                        territoryLanguageToPopulation = new TreeMap<String, PopulationData>());
                }
                OfficialStatus officialStatus = OfficialStatus.unknown;
                String officialStatusString = languageInTerritoryAttributes.get("officialStatus");
                if (officialStatusString != null) officialStatus = OfficialStatus.valueOf(officialStatusString);

                PopulationData newData = new PopulationData()
                    .setPopulation(languagePopulation)
                    .setLiteratePopulation(languageLiteracyPercent * languagePopulation / 100)
                    .setWritingPopulation(writingPercent * languagePopulation / 100)
                    .setOfficialStatus(officialStatus)
                    // .setGdp(languageGdp)
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
                languageToTerritories2.put(language,
                    Pair.of(newData.getOfficialStatus().isMajor() ? false : true,
                        Pair.of(-newData.getLiteratePopulation(), territory)));

                // now collect data for languages globally
                PopulationData data = languageToPopulation.get(language);
                if (data == null) {
                    languageToPopulation.put(language, data = new PopulationData().set(newData));
                } else {
                    data.add(newData);
                }
                // if (language.equals("en")) {
                // System.out.println(territory + "\tnewData:\t" + newData + "\tdata:\t" + data);
                // }
                String baseLanguage = languageTagParser.set(language).getLanguage();
                data = baseLanguageToPopulation.get(baseLanguage);
                if (data == null) {
                    baseLanguageToPopulation.put(baseLanguage, data = new PopulationData().set(newData));
                } else {
                    data.add(newData);
                }
                if (!baseLanguage.equals(language)) {
                    languageToScriptVariants.put(baseLanguage, language);
                }
            }
            return true;
        }

        private boolean handleCurrencyData(String level2, XPathParts parts) {
            if (level2.equals("fractions")) {
                // <info iso4217="ADP" digits="0" rounding="0" cashRounding="5"/>
                currencyToCurrencyNumberInfo.put(parts.getAttributeValue(3, "iso4217"),
                    new CurrencyNumberInfo(
                        parseIntegerOrNull(parts.getAttributeValue(3, "digits")),
                        parseIntegerOrNull(parts.getAttributeValue(3, "rounding")),
                        parseIntegerOrNull(parts.getAttributeValue(3, "cashDigits")),
                        parseIntegerOrNull(parts.getAttributeValue(3, "cashRounding"))));
                return true;
            }
            /*
             * <region iso3166="AD">
             * <currency iso4217="EUR" from="1999-01-01"/>
             * <currency iso4217="ESP" from="1873" to="2002-02-28"/>
             */
            if (level2.equals("region")) {
                territoryToCurrencyDateInfo.put(parts.getAttributeValue(2, "iso3166"),
                    new CurrencyDateInfo(parts.getAttributeValue(3, "iso4217"),
                        parts.getAttributeValue(3, "from"),
                        parts.getAttributeValue(3, "to"),
                        parts.getAttributeValue(3, "tender")));
                return true;
            }

            return false;
        }

        private void handleTelephoneCodeData(XPathParts parts) {
            // element 2: codesByTerritory territory [draft] [references]
            String terr = parts.getAttributeValue(2, "territory");
            // element 3: telephoneCountryCode code [from] [to] [draft] [references] [alt]
            TelephoneCodeInfo tcInfo = new TelephoneCodeInfo(parts.getAttributeValue(3, "code"),
                parts.getAttributeValue(3, "from"),
                parts.getAttributeValue(3, "to"),
                parts.getAttributeValue(3, "alt"));

            Set<TelephoneCodeInfo> tcSet = territoryToTelephoneCodeInfo.get(terr);
            if (tcSet == null) {
                tcSet = new LinkedHashSet<TelephoneCodeInfo>();
                territoryToTelephoneCodeInfo.put(terr, tcSet);
            }
            tcSet.add(tcInfo);
        }

        private void handleTerritoryContainment(XPathParts parts) {
            // <group type="001" contains="002 009 019 142 150"/>
            final String container = parts.getAttributeValue(-1, "type");
            final List<String> contained = Arrays
                .asList(parts.getAttributeValue(-1, "contains").split("\\s+"));
            // everything!
            containment.putAll(container, contained);

            String status = parts.getAttributeValue(-1, "status");
            String grouping = parts.getAttributeValue(-1, "grouping");
            if (status == null && grouping == null) {
                containmentCore.putAll(container, contained);
            }
            if (status != null && status.equals("deprecated")) {
                containmentDeprecated.putAll(container, contained);
            }
            if (grouping != null) {
                containmentGrouping.putAll(container, contained);
            }
        }

        private void handleSubdivisionContainment(XPathParts parts) {
            //      <subgroup type="AL" subtype="04" contains="FR MK LU"/>
            final String country = parts.getAttributeValue(-1, "type");
            final String subtype = parts.getAttributeValue(-1, "subtype");
            final String container = subtype == null ? country : (country + subtype).toLowerCase(Locale.ROOT);
            for (String contained : parts.getAttributeValue(-1, "contains").split("\\s+")) {
                String newContained = contained.charAt(0) >= 'a' ? contained : (country + contained).toLowerCase(Locale.ROOT);
                containerToSubdivision.put(container, newContained);
            }
        }

        private void handleLanguageData(XPathParts parts) {
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
            Map<Type, BasicLanguageData> map = languageToBasicLanguageData.get(language);
            if (map == null) {
                languageToBasicLanguageData.put(language, map = new EnumMap<Type, BasicLanguageData>(
                    BasicLanguageData.Type.class));
            }
            if (map.containsKey(languageData.type)) {
                throw new IllegalArgumentException("Duplicate value:\t" + parts);
            }
            map.put(languageData.type, languageData);
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

    public class CoverageVariableInfo {
        public Set<String> targetScripts;
        public Set<String> targetTerritories;
        public Set<String> calendars;
        public Set<String> targetCurrencies;
        public Set<String> targetTimeZones;
        public Set<String> targetPlurals;
    }

    public static String toRegexString(Set<String> s) {
        Iterator<String> it = s.iterator();
        StringBuilder sb = new StringBuilder("(");
        int count = 0;
        while (it.hasNext()) {
            if (count > 0) {
                sb.append("|");
            }
            sb.append(it.next());
            count++;
        }
        sb.append(")");
        return sb.toString();

    }

    public int parseIntegerOrNull(String attributeValue) {
        return attributeValue == null ? -1 : Integer.parseInt(attributeValue);
    }

    Set<String> skippedElements = new TreeSet<String>();

    private Map<String, Pair<String, String>> references = new TreeMap<String, Pair<String, String>>();
    private Map<String, String> likelySubtags = new TreeMap<String, String>();
    // make public temporarily until we resolve.
    private SortedSet<CoverageLevelInfo> coverageLevels = new TreeSet<CoverageLevelInfo>();
    private Map<String, String> parentLocales = new HashMap<String, String>();
    private Map<String, List<String>> calendarPreferences = new HashMap<String, List<String>>();
    private Map<String, CoverageVariableInfo> localeSpecificVariables = new TreeMap<String, CoverageVariableInfo>();
    private VariableReplacer coverageVariables = new VariableReplacer();
    private Map<String, NumberingSystemInfo> numberingSystems = new HashMap<String, NumberingSystemInfo>();
    private Set<String> numericSystems = new TreeSet<String>();
    private Set<String> defaultContentLocales;
    public Map<CLDRLocale, CLDRLocale> baseToDefaultContent; // wo -> wo_Arab_SN
    public Map<CLDRLocale, CLDRLocale> defaultContentToBase; // wo_Arab_SN -> wo
    private Set<String> CLDRLanguageCodes = new TreeSet<String>();;
    private Set<String> CLDRScriptCodes;

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

    public PopulationData getBaseLanguagePopulationData(String language) {
        return baseLanguageToPopulation.get(language);
    }

    public Set<String> getLanguages() {
        return allLanguages;
    }

    public Set<String> getTerritoryToLanguages(String territory) {
        Map<String, PopulationData> result = territoryToLanguageToPopulationData
            .get(territory);
        if (result == null) {
            return Collections.emptySet();
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
        Map<Type, BasicLanguageData> map = languageToBasicLanguageData.get(language);
        if (map == null) {
            throw new IllegalArgumentException("Bad language code: " + language);
        }
        return new LinkedHashSet<BasicLanguageData>(map.values());
    }

    public Map<Type, BasicLanguageData> getBasicLanguageDataMap(String language) {
        return languageToBasicLanguageData.get(language);
    }

    public Set<String> getBasicLanguageDataLanguages() {
        return languageToBasicLanguageData.keySet();
    }

    public Relation<String, String> getContainmentCore() {
        return containmentCore;
    }

    public Set<String> getContained(String territoryCode) {
        return containment.getAll(territoryCode);
    }

    public Set<String> getContainers() {
        return containment.keySet();
    }

    public Set<String> getContainedSubdivisions(String territoryOrSubdivisionCode) {
        return containerToSubdivision.getAll(territoryOrSubdivisionCode);
    }

    public Set<String> getContainersForSubdivisions() {
        return containerToSubdivision.keySet();
    }

    public Relation<String, String> getTerritoryToContained() {
        return getTerritoryToContained(ContainmentStyle.all); // true
    }

    //    public Relation<String, String> getTerritoryToContained(boolean allowDeprecated) {
    //        return allowDeprecated ? containment : containmentNonDeprecated;
    //    }
    //
    public enum ContainmentStyle {
        all, core, grouping, deprecated
    }

    public Relation<String, String> getTerritoryToContained(ContainmentStyle containmentStyle) {
        switch (containmentStyle) {
        case all:
            return containment;
        case core:
            return containmentCore;
        case grouping:
            return containmentGrouping;
        case deprecated:
            return containmentDeprecated;
        }
        throw new IllegalArgumentException("internal error");
    }

    public Set<String> getSkippedElements() {
        return skippedElements;
    }

    public Set<String> getZone_aliases(String zone) {
        Set<String> result = zone_aliases.getAll(zone);
        if (result == null) {
            return Collections.emptySet();
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
     *
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
     * Return the list of default content locales.
     *
     * @return
     */
    public Set<String> getDefaultContentLocales() {
        return defaultContentLocales;
    }

    public static Map<String, String> makeLocaleToDefaultContents(Set<String> defaultContents,
        Map<String, String> result, Set<String> errors) {
        for (String s : defaultContents) {
            String simpleParent = LanguageTagParser.getSimpleParent(s);
            String oldValue = result.get(simpleParent);
            if (oldValue != null) {
                errors.add("*** Error: Default contents cannot contain two children for the same parent:\t"
                    + oldValue + ", " + s + "; keeping " + oldValue);
                continue;
            }
            result.put(simpleParent, s);
        }
        return result;
    }

    /**
     * Return the list of default content locales.
     *
     * @return
     */
    public Set<CLDRLocale> getDefaultContentCLDRLocales() {
        initCLDRLocaleBasedData();
        return defaultContentToBase.keySet();
    }

    /**
     * Get the default content locale for a specified language
     *
     * @param language
     *            language to search
     * @return default content, or null if none
     */
    public String getDefaultContentLocale(String language) {
        for (String dc : defaultContentLocales) {
            if (dc.startsWith(language + "_")) {
                return dc;
            }
        }
        return null;
    }

    /**
     * Get the default content locale for a specified language and script.
     * If script is null, delegates to {@link #getDefaultContentLocale(String)}
     *
     * @param language
     * @param script
     *            if null, delegates to {@link #getDefaultContentLocale(String)}
     * @return default content, or null if none
     */
    public String getDefaultContentLocale(String language, String script) {
        if (script == null) return getDefaultContentLocale(language);
        for (String dc : defaultContentLocales) {
            if (dc.startsWith(language + "_" + script + "_")) {
                return dc;
            }
        }
        return null;
    }

    /**
     * Given a default locale (such as 'wo_Arab_SN') return the base locale (such as 'wo'), or null if the input wasn't
     * a default conetnt locale.
     *
     * @param baseLocale
     * @return
     */
    public CLDRLocale getBaseFromDefaultContent(CLDRLocale dcLocale) {
        initCLDRLocaleBasedData();
        return defaultContentToBase.get(dcLocale);
    }

    /**
     * Given a base locale (such as 'wo') return the default content locale (such as 'wo_Arab_SN'), or null.
     *
     * @param baseLocale
     * @return
     */
    public CLDRLocale getDefaultContentFromBase(CLDRLocale baseLocale) {
        initCLDRLocaleBasedData();
        return baseToDefaultContent.get(baseLocale);
    }

    /**
     * Is this a default content locale?
     *
     * @param dcLocale
     * @return
     */
    public boolean isDefaultContent(CLDRLocale dcLocale) {
        initCLDRLocaleBasedData();
        if (dcLocale == null) throw new NullPointerException("null locale");
        return (defaultContentToBase.get(dcLocale) != null);
    }

    public Set<String> getNumberingSystems() {
        return numberingSystems.keySet();
    }

    public Set<String> getNumericNumberingSystems() {
        return Collections.unmodifiableSet(numericSystems);
    }

    public String getDigits(String numberingSystem) {
        try {
            return numberingSystems.get(numberingSystem).digits;
        } catch (Exception e) {
            throw new IllegalArgumentException("Can't get digits for:" + numberingSystem);
        }
    }

    public NumberingSystemType getNumberingSystemType(String numberingSystem) {
        return numberingSystems.get(numberingSystem).type;
    }

    public SortedSet<CoverageLevelInfo> getCoverageLevelInfo() {
        return coverageLevels;
    }

    /**
     * Used to get the coverage value for a path. This is generally the most
     * efficient way for tools to get coverage.
     *
     * @param xpath
     * @param loc
     * @return
     */
    public Level getCoverageLevel(String xpath, String loc) {
        Level result = null;
        result = coverageCache.get(xpath, loc);
        if (result == null) {
            CoverageLevel2 cov = localeToCoverageLevelInfo.get(loc);
            if (cov == null) {
                cov = CoverageLevel2.getInstance(this, loc);
                localeToCoverageLevelInfo.put(loc, cov);
            }

            result = cov.getLevel(xpath);
            coverageCache.put(xpath, loc, result);
        }
        return result;
    }

    /**
     * Cache Data structure with object expiry,
     * List that can hold up to MAX_LOCALES caches of locales, when one locale hasn't been used for a while it will removed and GC'd
     */
    private class CoverageCache {
        private final Deque<Node> localeList = new LinkedList<>();
        private final int MAX_LOCALES = 10;

        /**
         * Object to sync on for modifying the locale list
         */
        private final Object LOCALE_LIST_ITER_SYNC = new Object();

        /*
         * constructor
         */
        public CoverageCache() {
//            localeList = new LinkedList<Node>();
        }

        /*
         * retrieves coverage level associated with two keys if it exists in the cache, otherwise returns null
         * @param xpath
         * @param loc
         * @return the coverage level of the above two keys
         */
        public Level get(String xpath, String loc) {
            synchronized (LOCALE_LIST_ITER_SYNC) {
                Iterator<Node> it = localeList.iterator();
                Node reAddNode = null;
                while (it.hasNext()) {
//            for (Iterator<Node> it = localeList.iterator(); it.hasNext();) {
                    Node node = it.next();
                    if (node.loc.equals(loc)) {
                        reAddNode = node;
                        it.remove();
                        break;

                    }
                }
                if (reAddNode != null) {
                    localeList.addFirst(reAddNode);
                    return reAddNode.map.get(xpath);
                }
                return null;
            }
        }

        /*
         * places a coverage level into the cache, with two keys
         * @param xpath
         * @param loc
         * @param covLevel    the coverage level of the above two keys
         */
        public void put(String xpath, String loc, Level covLevel) {
            synchronized (LOCALE_LIST_ITER_SYNC) {
                //if locale's map is already in the cache add to it
//            for (Iterator<Node> it = localeList.iterator(); it.hasNext();) {
                for (Node node : localeList) {
//                Node node = it.next();
                    if (node.loc.equals(loc)) {
                        node.map.put(xpath, covLevel);
                        return;
                    }
                }

                //if it is not, add a new map with the coverage level, and remove the last map in the list (used most seldom) if the list is too large
                Map<String, Level> newMap = new ConcurrentHashMap<String, Level>();
                newMap.put(xpath, covLevel);
                localeList.addFirst(new Node(loc, newMap));

                if (localeList.size() > MAX_LOCALES) {
                    localeList.removeLast();
                }
            }
        }

        /*
         * node to hold a location and a Map
         */
        private class Node {
            //public fields to emulate a C/C++ struct
            public String loc;
            public Map<String, Level> map;

            public Node(String _loc, Map<String, Level> _map) {
                loc = _loc;
                map = _map;
            }
        }
    }

    /**
     * Used to get the coverage value for a path. Note, it is more efficient to create
     * a CoverageLevel2 for a language, and keep it around.
     *
     * @param xpath
     * @param loc
     * @return
     */
    public int getCoverageValue(String xpath, String loc) {
        return getCoverageLevel(xpath, loc).getLevel();
    }

    private RegexLookup<Level> coverageLookup = null;

    public synchronized RegexLookup<Level> getCoverageLookup() {
        if (coverageLookup == null) {
            RegexLookup<Level> lookup = new RegexLookup<Level>(RegexLookup.LookupType.STAR_PATTERN_LOOKUP);

            Matcher variable = PatternCache.get("\\$\\{[A-Za-z][\\-A-Za-z]*\\}").matcher("");

            for (CoverageLevelInfo ci : getCoverageLevelInfo()) {
                String pattern = ci.match.replace('\'', '"')
                    .replace("[@", "\\[@") // make sure that attributes are quoted
                    .replace("(", "(?:") // make sure that there are no capturing groups (beyond what we generate
                    .replace("(?:?!", "(?!"); // Allow negative lookahead
                pattern = "^//ldml/" + pattern + "$"; // for now, force a complete match
                String variableType = null;
                variable.reset(pattern);
                if (variable.find()) {
                    pattern = pattern.substring(0, variable.start()) + "([^\"]*)" + pattern.substring(variable.end());
                    variableType = variable.group();
                    if (variable.find()) {
                        throw new IllegalArgumentException("We can only handle a single variable on a line");
                    }
                }

                // .replaceAll("\\]","\\\\]");
                lookup.add(new CoverageLevel2.MyRegexFinder(pattern, variableType, ci), ci.value);
            }
            coverageLookup = lookup;
        }
        return coverageLookup;
    }

    /**
     * This appears to be unused, so didn't provide new version.
     *
     * @param xpath
     * @return
     */
    public int getCoverageValueOld(String xpath) {
        ULocale loc = new ULocale("und");
        return getCoverageValueOld(xpath, loc);
    }

    /**
     * Older version of code.
     *
     * @param xpath
     * @param loc
     * @return
     */
    public int getCoverageValueOld(String xpath, ULocale loc) {
        String targetLanguage = loc.getLanguage();

        CoverageVariableInfo cvi = getCoverageVariableInfo(targetLanguage);
        String targetScriptString = toRegexString(cvi.targetScripts);
        String targetTerritoryString = toRegexString(cvi.targetTerritories);
        String calendarListString = toRegexString(cvi.calendars);
        String targetCurrencyString = toRegexString(cvi.targetCurrencies);
        String targetTimeZoneString = toRegexString(cvi.targetTimeZones);
        String targetPluralsString = toRegexString(cvi.targetPlurals);
        Iterator<CoverageLevelInfo> i = coverageLevels.iterator();
        while (i.hasNext()) {
            CoverageLevelInfo ci = i.next();
            String regex = "//ldml/" + ci.match.replace('\'', '"')
            .replaceAll("\\[", "\\\\[")
            .replaceAll("\\]", "\\\\]")
            .replace("${Target-Language}", targetLanguage)
            .replace("${Target-Scripts}", targetScriptString)
            .replace("${Target-Territories}", targetTerritoryString)
            .replace("${Target-TimeZones}", targetTimeZoneString)
            .replace("${Target-Currencies}", targetCurrencyString)
            .replace("${Target-Plurals}", targetPluralsString)
            .replace("${Calendar-List}", calendarListString);

            // Special logic added for coverage fields that are only to be applicable
            // to certain territories
            if (ci.inTerritory != null) {
                if (ci.inTerritory.equals("EU")) {
                    Set<String> containedTerritories = new HashSet<String>();
                    containedTerritories.addAll(getContained(ci.inTerritory));
                    containedTerritories.retainAll(cvi.targetTerritories);
                    if (containedTerritories.isEmpty()) {
                        continue;
                    }
                } else {
                    if (!cvi.targetTerritories.contains(ci.inTerritory)) {
                        continue;
                    }
                }
            }
            // Special logic added for coverage fields that are only to be applicable
            // to certain languages
            if (ci.inLanguage != null && !ci.inLanguage.matcher(targetLanguage).matches()) {
                continue;
            }

            // Special logic added for coverage fields that are only to be applicable
            // to certain scripts
            if (ci.inScript != null && !cvi.targetScripts.contains(ci.inScript)) {
                continue;
            }

            if (xpath.matches(regex)) {
                return ci.value.getLevel();
            }

            if (xpath.matches(regex)) {
                return ci.value.getLevel();
            }
        }
        return Level.OPTIONAL.getLevel(); // If no match then return highest possible value
    }

    public CoverageVariableInfo getCoverageVariableInfo(String targetLanguage) {
        CoverageVariableInfo cvi;
        if (localeSpecificVariables.containsKey(targetLanguage)) {
            cvi = localeSpecificVariables.get(targetLanguage);
        } else {
            cvi = new CoverageVariableInfo();
            cvi.targetScripts = getTargetScripts(targetLanguage);
            cvi.targetTerritories = getTargetTerritories(targetLanguage);
            cvi.calendars = getCalendars(cvi.targetTerritories);
            cvi.targetCurrencies = getCurrentCurrencies(cvi.targetTerritories);
            cvi.targetTimeZones = getCurrentTimeZones(cvi.targetTerritories);
            cvi.targetPlurals = getTargetPlurals(targetLanguage);
            localeSpecificVariables.put(targetLanguage, cvi);
        }
        return cvi;
    }

    private Set<String> getTargetScripts(String language) {
        Set<String> targetScripts = new HashSet<String>();
        try {
            Set<BasicLanguageData> langData = getBasicLanguageData(language);
            Iterator<BasicLanguageData> ldi = langData.iterator();
            while (ldi.hasNext()) {
                BasicLanguageData bl = ldi.next();
                Set<String> addScripts = bl.scripts;
                if (addScripts != null && bl.getType() != BasicLanguageData.Type.secondary) {
                    targetScripts.addAll(addScripts);
                }
            }
        } catch (Exception e) {
            // fall through
        }

        if (targetScripts.size() == 0) {
            targetScripts.add("Zzzz"); // Unknown Script
        }
        return targetScripts;
    }

    private Set<String> getTargetTerritories(String language) {
        Set<String> targetTerritories = new HashSet<String>();
        try {
            Set<BasicLanguageData> langData = getBasicLanguageData(language);
            Iterator<BasicLanguageData> ldi = langData.iterator();
            while (ldi.hasNext()) {
                BasicLanguageData bl = ldi.next();
                Set<String> addTerritories = bl.territories;
                if (addTerritories != null && bl.getType() != BasicLanguageData.Type.secondary) {
                    targetTerritories.addAll(addTerritories);
                }
            }
        } catch (Exception e) {
            // fall through
        }
        if (targetTerritories.size() == 0) {
            targetTerritories.add("ZZ");
        }
        return targetTerritories;
    }

    private Set<String> getCalendars(Set<String> territories) {
        Set<String> targetCalendars = new HashSet<String>();
        Iterator<String> it = territories.iterator();
        while (it.hasNext()) {
            List<String> addCalendars = getCalendars(it.next());
            if (addCalendars == null) {
                continue;
            }
            targetCalendars.addAll(addCalendars);
        }
        return targetCalendars;
    }

    /**
     * @param territory
     * @return a list the calendars used in the specified territorys
     */
    public List<String> getCalendars(String territory) {
        return calendarPreferences.get(territory);
    }

    private Set<String> getCurrentCurrencies(Set<String> territories) {
        Date now = new Date();
        return getCurrentCurrencies(territories, now, now);
    }

    public Set<String> getCurrentCurrencies(Set<String> territories, Date startsBefore, Date endsAfter) {
        Set<String> targetCurrencies = new HashSet<String>();
        Iterator<String> it = territories.iterator();
        while (it.hasNext()) {
            Set<CurrencyDateInfo> targetCurrencyInfo = getCurrencyDateInfo(it.next());
            if (targetCurrencyInfo == null) {
                continue;
            }
            Iterator<CurrencyDateInfo> it2 = targetCurrencyInfo.iterator();
            while (it2.hasNext()) {
                CurrencyDateInfo cdi = it2.next();
                if (cdi.getStart().before(startsBefore) && cdi.getEnd().after(endsAfter) && cdi.isLegalTender()) {
                    targetCurrencies.add(cdi.getCurrency());
                }
            }
        }
        return targetCurrencies;
    }

    private Set<String> getCurrentTimeZones(Set<String> territories) {
        Set<String> targetTimeZones = new HashSet<String>();
        Iterator<String> it = territories.iterator();
        while (it.hasNext()) {
            String[] countryIDs = TimeZone.getAvailableIDs(it.next());
            for (int i = 0; i < countryIDs.length; i++) {
                targetTimeZones.add(countryIDs[i]);
            }
        }
        return targetTimeZones;
    }

    private Set<String> getTargetPlurals(String language) {
        Set<String> targetPlurals = new HashSet<String>();
        targetPlurals.addAll(getPlurals(PluralType.cardinal, language).getCanonicalKeywords());
        // TODO: Kept 0 and 1 specifically until Mark figures out what to do with them.
        // They should be removed once this is done.
        targetPlurals.add("0");
        targetPlurals.add("1");
        return targetPlurals;
    }

    public String getExplicitParentLocale(String loc) {
        return parentLocales.get(loc);
    }

    public Set<String> getExplicitChildren() {
        return parentLocales.keySet();
    }

    public Collection<String> getExplicitParents() {
        return parentLocales.values();
    }

    private final static class ApprovalRequirementMatcher {
        @Override
        public String toString() {
            return locales + " / " + xpathMatcher + " = " + requiredVotes;
        }

        ApprovalRequirementMatcher(String xpath) {
            XPathParts parts = XPathParts.getTestInstance(xpath);
            if (parts.containsElement("approvalRequirement")) {
                requiredVotes = Integer.parseInt(parts.getAttributeValue(-1, "votes"));
                String localeAttrib = parts.getAttributeValue(-1, "locales");
                if (localeAttrib == null || localeAttrib.equals(STAR) || localeAttrib.isEmpty()) {
                    locales = null; // no locale listed == '*'
                } else {
                    Set<CLDRLocale> localeList = new HashSet<CLDRLocale>();
                    String[] el = localeAttrib.split(" ");
                    for (int i = 0; i < el.length; i++) {
                        if (el[i].indexOf(":") == -1) { // Just a simple locale designation
                            localeList.add(CLDRLocale.getInstance(el[i]));
                        } else { // Org:CoverageLevel
                            String[] coverageLocaleParts = el[i].split(":", 2);
                            String org = coverageLocaleParts[0];
                            String level = coverageLocaleParts[1].toUpperCase();
                            Set<String> coverageLocales = sc.getLocaleCoverageLocales(Organization.fromString(org), EnumSet.of(Level.fromString(level)));
                            for (String cl : coverageLocales) {
                                localeList.add(CLDRLocale.getInstance(cl));
                            }
                        }
                    }
                    locales = Collections.unmodifiableSet(localeList);
                }
                String xpathMatch = parts.getAttributeValue(-1, "paths");
                if (xpathMatch == null || xpathMatch.isEmpty() || xpathMatch.equals(STAR)) {
                    xpathMatcher = null;
                } else {
                    xpathMatcher = PatternCache.get(xpathMatch);
                }
            } else {
                throw new RuntimeException("Unknown approval requirement: " + xpath);
            }
        }

        final private Set<CLDRLocale> locales;
        final private Pattern xpathMatcher;
        final int requiredVotes;

        public static List<ApprovalRequirementMatcher> buildAll(List<String> approvalRequirements) {
            List<ApprovalRequirementMatcher> newList = new LinkedList<ApprovalRequirementMatcher>();

            for (String xpath : approvalRequirements) {
                newList.add(new ApprovalRequirementMatcher(xpath));
            }

            return Collections.unmodifiableList(newList);
        }

        public boolean matches(CLDRLocale loc, PathHeader ph) {
            if (DEBUG) System.err.println(">> testing " + loc + " / " + ph + " vs " + toString());
            if (locales != null) {
                if (!locales.contains(loc)) {
                    return false;
                }
            }
            if (xpathMatcher != null) {
                if (ph != null) {
                    if (!xpathMatcher.matcher(ph.getOriginalPath()).matches()) {
                        return false;
                    } else {
                        return true;
                    }
                } else {
                    return false;
                }
            }
            return true;
        }

        public int getRequiredVotes() {
            return requiredVotes;
        }
    }

    // run these from first to last to get the approval info.
    volatile List<ApprovalRequirementMatcher> approvalMatchers = null;

    /**
     * Only called by VoteResolver.
     * @param loc
     * @param PathHeader - which path this is applied to, or null if unknown.
     * @return
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
        throw new RuntimeException("Error: " + loc + " " + ph + " ran off the end of the approvalMatchers.");
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
     * @param languageId
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
                // System.out.println("?\t" + territory + "\t" + targetLanguage);
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

    public Map<String, Map<String, String>> getMetazoneToRegionToZone() {
        return typeToZoneToRegionToZone.get("metazones");
    }

    public String getZoneForMetazoneByRegion(String metazone, String region) {
        String result = null;
        if (getMetazoneToRegionToZone().containsKey(metazone)) {
            Map<String, String> myMap = getMetazoneToRegionToZone().get(metazone);
            if (myMap.containsKey(region)) {
                result = myMap.get(region);
            } else {
                result = myMap.get("001");
            }
        }

        if (result == null) {
            result = "Etc/GMT";
        }

        return result;
    }

    public Map<String, Map<String, Map<String, String>>> getTypeToZoneToRegionToZone() {
        return typeToZoneToRegionToZone;
    }

    /**
     * @deprecated, use PathHeader.getMetazonePageTerritory
     */
    public Map<String, String> getMetazoneToContinentMap() {
        return metazoneContinentMap;
    }

    public Set<String> getAllMetazones() {
        return allMetazones;
    }

    public Map<String, String> getLikelySubtags() {
        return likelySubtags;
    }

    public enum PluralType {
        cardinal(PluralRules.PluralType.CARDINAL), ordinal(PluralRules.PluralType.ORDINAL);

        // add some gorp to interwork until we clean things up

        public final PluralRules.PluralType standardType;

        PluralType(PluralRules.PluralType standardType) {
            this.standardType = standardType;
        }

        public static PluralType fromStandardType(PluralRules.PluralType standardType) {
            return standardType == null ? null
                : standardType == PluralRules.PluralType.CARDINAL ? cardinal
                    : ordinal;
        }
    };

    private EnumMap<PluralType, Map<String, PluralInfo>> localeToPluralInfo2 = new EnumMap<>(PluralType.class);
    {
        localeToPluralInfo2.put(PluralType.cardinal, new LinkedHashMap<String, PluralInfo>());
        localeToPluralInfo2.put(PluralType.ordinal, new LinkedHashMap<String, PluralInfo>());
    }
    private Map<String, PluralRanges> localeToPluralRanges = new LinkedHashMap<String, PluralRanges>();

    private Map<DayPeriodInfo.Type, Map<String, DayPeriodInfo>> typeToLocaleToDayPeriodInfo = new EnumMap<DayPeriodInfo.Type, Map<String, DayPeriodInfo>>(
        DayPeriodInfo.Type.class);
    private Map<String, CoverageLevel2> localeToCoverageLevelInfo = new ConcurrentHashMap<String, CoverageLevel2>();
    private CoverageCache coverageCache = new CoverageCache();
    private transient String lastPluralLocales = "";
    private transient PluralType lastPluralWasOrdinal = null;
    private transient Map<Count, String> lastPluralMap = new EnumMap<Count, String>(Count.class);
    private transient String lastDayPeriodLocales = null;
    private transient DayPeriodInfo.Type lastDayPeriodType = null;
    private transient DayPeriodInfo.Builder dayPeriodBuilder = new DayPeriodInfo.Builder();

    private void addDayPeriodPath(XPathParts path, String value) {
        // ldml/dates/calendars/calendar[@type="gregorian"]/dayPeriods/dayPeriodContext[@type="format"]/dayPeriodWidth[@type="wide"]/dayPeriod[@type="am"]
        /*
         * <supplementalData>
         * <version number="$Revision$"/>
         * <generation date="$D..e... $"/>
         * <dayPeriodRuleSet>
         * <dayPeriodRules locales = "en"> <!-- default for any locales not listed under other dayPeriods -->
         * <dayPeriodRule type = "am" from = "0:00" before="12:00"/>
         * <dayPeriodRule type = "pm" from = "12:00" to="24:00"/>
         */
        String typeString = path.getAttributeValue(1, "type");
        String locales = path.getAttributeValue(2, "locales").trim();
        DayPeriodInfo.Type type = typeString == null
            ? DayPeriodInfo.Type.format
                : DayPeriodInfo.Type.valueOf(typeString.trim());
        if (!locales.equals(lastDayPeriodLocales) || type != lastDayPeriodType) {
            if (lastDayPeriodLocales != null) {
                addDayPeriodInfo();
            }
            lastDayPeriodLocales = locales;
            lastDayPeriodType = type;
            // System.out.println(type + ", " + locales + ", " + path);
        }
        if (path.size() != 4) {
            if (locales.equals("root")) return; // we allow root to be empty
            throw new IllegalArgumentException(locales + " must have dayPeriodRule elements");
        }
        DayPeriod dayPeriod;
        try {
            dayPeriod = DayPeriod.fromString(path.getAttributeValue(-1, "type"));
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return;
        }
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
//        if (dayPeriodBuilder.contains(dayPeriod)) { // disallow multiple rules with same dayperiod
//            throw new IllegalArgumentException("Multiple rules with same dayperiod are disallowed: "
//                + lastDayPeriodLocales + ", " + lastDayPeriodType + ", " + dayPeriod);
//        }
        boolean includesStart = from != null;
        boolean includesEnd = to != null;
        int start = parseTime(includesStart ? from : after);
        int end = parseTime(includesEnd ? to : before);
        // Check if any periods contain 0, e.g. 1700 - 300
        if (start > end) {
            // System.out.println("start " + start + " end " + end);
            dayPeriodBuilder.add(dayPeriod, start, includesStart, parseTime("24:00"), includesEnd);
            dayPeriodBuilder.add(dayPeriod, parseTime("0:00"), includesStart, end, includesEnd);
        } else {
            dayPeriodBuilder.add(dayPeriod, start, includesStart, end, includesEnd);
        }
    }

    static Pattern PARSE_TIME = PatternCache.get("(\\d\\d?):(\\d\\d)");

    private int parseTime(String string) {
        Matcher matcher = PARSE_TIME.matcher(string);
        if (!matcher.matches()) {
            throw new IllegalArgumentException();
        }
        return (Integer.parseInt(matcher.group(1)) * 60 + Integer.parseInt(matcher.group(2))) * 60 * 1000;
    }

    private void addDayPeriodInfo() {
        String[] locales = lastDayPeriodLocales.split("\\s+");
        DayPeriodInfo temp = dayPeriodBuilder.finish(locales);
        Map<String, DayPeriodInfo> locale2DPI = typeToLocaleToDayPeriodInfo.get(lastDayPeriodType);
        if (locale2DPI == null) {
            typeToLocaleToDayPeriodInfo.put(lastDayPeriodType, locale2DPI = new LinkedHashMap<String, DayPeriodInfo>());
            //System.out.println(lastDayPeriodType + ", " + locale2DPI);
        }
        for (String locale : locales) {
            locale2DPI.put(locale, temp);
        }
    }

    static String lastPluralRangesLocales = null;
    static PluralRanges lastPluralRanges = null;

    private boolean addPluralPath(XPathParts path, String value) {
        /*
         * Adding
         <pluralRanges locales="am">
          <pluralRange start="one" end="one" result="one" />
         </pluralRanges>
         */
        String locales = path.getAttributeValue(2, "locales").trim();
        String element = path.getElement(2);
        if ("pluralRanges".equals(element)) {
            if (!locales.equals(lastPluralRangesLocales)) {
                addPluralRanges(locales);
            }
            if (path.size() == 3) {
                // ok for ranges to be empty
                return true;
            }
            String rangeStart = path.getAttributeValue(-1, "start");
            String rangeEnd = path.getAttributeValue(-1, "end");
            String result = path.getAttributeValue(-1, "result");
            lastPluralRanges.add(rangeStart == null ? null : Count.valueOf(rangeStart),
                rangeEnd == null ? null : Count.valueOf(rangeEnd),
                    Count.valueOf(result));
            return true;
        } else if ("pluralRules".equals(element)) {

            String type = path.getAttributeValue(1, "type");
            PluralType pluralType = type == null ? PluralType.cardinal : PluralType.valueOf(type);
            if (!lastPluralLocales.equals(locales)) {
                addPluralInfo(pluralType);
                lastPluralLocales = locales;
            }
            final String countString = path.getAttributeValue(-1, "count");
            if (countString == null) {
                return false;
            }
            Count count = Count.valueOf(countString);
            if (lastPluralMap.containsKey(count)) {
                throw new IllegalArgumentException("Duplicate plural count: " + count + " in " + locales);
            }
            lastPluralMap.put(count, value);
            lastPluralWasOrdinal = pluralType;
            return true;
        } else {
            return false;
        }
    }

    private void addPluralRanges(String localesString) {
        final String[] locales = localesString.split("\\s+");
        lastPluralRanges = new PluralRanges();
        for (String locale : locales) {
            if (localeToPluralRanges.containsKey(locale)) {
                throw new IllegalArgumentException("Duplicate plural locale: " + locale);
            }
            localeToPluralRanges.put(locale, lastPluralRanges);
        }
        lastPluralRangesLocales = localesString;
    }

    private void addPluralInfo(PluralType pluralType) {
        final String[] locales = lastPluralLocales.split("\\s+");
        PluralInfo info = new PluralInfo(lastPluralMap, pluralType);
        Map<String, PluralInfo> localeToInfo = localeToPluralInfo2.get(pluralType);
        for (String locale : locales) {
            if (localeToInfo.containsKey(locale)) {
                throw new IllegalArgumentException("Duplicate plural locale: " + locale);
            } else if (!locale.isEmpty()) {
                localeToInfo.put(locale, info);
            }
        }
        lastPluralMap.clear();
    }

    public static class SampleList {
        public static final SampleList EMPTY = new SampleList().freeze();

        private UnicodeSet uset = new UnicodeSet();
        private List<FixedDecimal> fractions = new ArrayList<FixedDecimal>(0);

        public String toString() {
            return toString(6, 3);
        }

        public String toString(int intLimit, int fractionLimit) {
            StringBuilder b = new StringBuilder();
            int intCount = 0;
            int fractionCount = 0;
            int limit = uset.getRangeCount();
            for (int i = 0; i < limit; ++i) {
                if (intCount >= intLimit) {
                    b.append(", …");
                    break;
                }
                if (b.length() != 0) {
                    b.append(", ");
                }
                int start = uset.getRangeStart(i);
                int end = uset.getRangeEnd(i);
                if (start == end) {
                    b.append(start);
                    ++intCount;
                } else if (start + 1 == end) {
                    b.append(start).append(", ").append(end);
                    intCount += 2;
                } else {
                    b.append(start).append('-').append(end);
                    intCount += 2;
                }
            }
            if (fractions.size() > 0) {
                for (int i = 0; i < fractions.size(); ++i) {
                    if (fractionCount >= fractionLimit) {
                        break;
                    }
                    if (b.length() != 0) {
                        b.append(", ");
                    }
                    FixedDecimal fraction = fractions.get(i);
                    String formatted = String.format(
                        Locale.ROOT,
                        "%." + fraction.getVisibleDecimalDigitCount() + "f",
                        fraction.getSource());
                    b.append(formatted);
                    ++fractionCount;
                }
                b.append(", …");
            }
            return b.toString();
        }

        public int getRangeCount() {
            return uset.getRangeCount();
        }

        public int getRangeStart(int index) {
            return uset.getRangeStart(index);
        }

        public int getRangeEnd(int index) {
            return uset.getRangeEnd(index);
        }

        public List<FixedDecimal> getFractions() {
            return fractions;
        }

        public int intSize() {
            return uset.size();
        }

        public SampleList remove(int i) {
            uset.remove(i);
            return this;
        }

        public SampleList add(int i) {
            uset.add(i);
            return this;
        }

        public SampleList freeze() {
            uset.freeze();
            if (fractions instanceof ArrayList) {
                fractions = Collections.unmodifiableList(fractions);
            }
            return this;
        }

        public void add(FixedDecimal i) {
            fractions.add(i);
        }

        public int fractionSize() {
            return fractions.size();
        }
    }

    public static class CountSampleList {
        private final Map<Count, SampleList> countToIntegerSamples9999;
        private final Map<Count, SampleList[]> countToDigitToIntegerSamples9999;

        CountSampleList(PluralRules pluralRules, Set<Count> keywords, PluralType pluralType) {
            // Create the integer counts
            countToIntegerSamples9999 = new EnumMap<Count, SampleList>(Count.class);
            countToDigitToIntegerSamples9999 = new EnumMap<Count, SampleList[]>(Count.class);
            for (Count c : keywords) {
                countToIntegerSamples9999.put(c, new SampleList());
                SampleList[] row = new SampleList[5];
                countToDigitToIntegerSamples9999.put(c, row);
                for (int i = 1; i < 5; ++i) {
                    row[i] = new SampleList();
                }
            }
            for (int ii = 0; ii < 10000; ++ii) {
                int i = ii;
                int digit;
                if (i > 999) {
                    digit = 4;
                } else if (i > 99) {
                    digit = 3;
                } else if (i > 9) {
                    digit = 2;
                } else {
                    digit = 1;
                }
                Count count = Count.valueOf(pluralRules.select(i));
                addSimple(countToIntegerSamples9999, i, count);
                addDigit(countToDigitToIntegerSamples9999, i, count, digit);
                if (haveFractions(keywords, digit)) {
                    continue;
                }
                if (pluralType == PluralType.cardinal) {
                    for (int f = 0; f < 30; ++f) {
                        FixedDecimal ni = new FixedDecimal(i + f / 10.0d, f < 10 ? 1 : 2, f);
                        count = Count.valueOf(pluralRules.select(ni));
                        addSimple(countToIntegerSamples9999, ni, count);
                        addDigit(countToDigitToIntegerSamples9999, ni, count, digit);
                    }
                }
            }
            // HACK for Breton
            addSimple(countToIntegerSamples9999, 1000000, Count.valueOf(pluralRules.select(1000000)));

            for (Count count : keywords) {
                SampleList uset = countToIntegerSamples9999.get(count);
                uset.freeze();
                SampleList[] map = countToDigitToIntegerSamples9999.get(count);
                for (int i = 1; i < map.length; ++i) {
                    map[i].freeze();
                }
            }
        }

        private boolean haveFractions(Set<Count> keywords, int digit) {
            for (Count c : keywords) {
                int size = countToDigitToIntegerSamples9999.get(c)[digit].fractionSize();
                if (size < MAX_COLLECTED_FRACTION) {
                    return false;
                }
            }
            return true;
        }

        static final int MAX_COLLECTED_FRACTION = 5;

        private boolean addDigit(Map<Count, SampleList[]> countToDigitToIntegerSamples9999, FixedDecimal i, Count count, int digit) {
            return addFraction(i, countToDigitToIntegerSamples9999.get(count)[digit]);
        }

        private boolean addFraction(FixedDecimal i, SampleList sampleList) {
            if (sampleList.fractionSize() < MAX_COLLECTED_FRACTION) {
                sampleList.add(i);
                return true;
            } else {
                return false;
            }
        }

        private boolean addSimple(Map<Count, SampleList> countToIntegerSamples9999, FixedDecimal i, Count count) {
            return addFraction(i, countToIntegerSamples9999.get(count));
        }

        private void addDigit(Map<Count, SampleList[]> countToDigitToIntegerSamples9999, int i, Count count, int digit) {
            countToDigitToIntegerSamples9999.get(count)[digit].add(i);
        }

        private void addSimple(Map<Count, SampleList> countToIntegerSamples9999, int i, Count count) {
            countToIntegerSamples9999.get(count).add(i);
        }

        public SampleList get(Count type) {
            return countToIntegerSamples9999.get(type);
        }

        public SampleList get(Count c, int digit) {
            SampleList[] sampleLists = countToDigitToIntegerSamples9999.get(c);
            return sampleLists == null ? null : sampleLists[digit];
        }
    }

    /**
     * Immutable class with plural info for different locales
     *
     * @author markdavis
     */
    public static class PluralInfo implements Comparable<PluralInfo> {
        static final Set<Double> explicits = new HashSet<Double>();
        static {
            explicits.add(0.0d);
            explicits.add(1.0d);
        }

        public enum Count {
            zero, one, two, few, many, other;
            public static final int LENGTH = Count.values().length;
            public static final List<Count> VALUES = Collections.unmodifiableList(Arrays.asList(values()));
        }

        static final Pattern pluralPaths = PatternCache.get(".*pluralRule.*");
        static final int fractDecrement = 13;
        static final int fractStart = 20;

        private final Map<Count, Set<Double>> countToExampleSet;
        private final Map<Count, String> countToStringExample;
        private final Map<Integer, Count> exampleToCount;
        private final PluralRules pluralRules;
        private final String pluralRulesString;
        private final Set<String> canonicalKeywords;
        private final Set<Count> keywords;
        private final Set<Count> integerKeywords;
        private final Set<Count> decimalKeywords;
        private final CountSampleList countSampleList;
        private final Map<Count, String> countToRule;

        private PluralInfo(Map<Count, String> countToRule, PluralType pluralType) {
            EnumMap<Count, String> tempCountToRule = new EnumMap<Count, String>(Count.class);
            tempCountToRule.putAll(countToRule);
            this.countToRule = Collections.unmodifiableMap(tempCountToRule);

            // now build rules
            NumberFormat nf = NumberFormat.getNumberInstance(ULocale.ENGLISH);
            nf.setMaximumFractionDigits(2);
            StringBuilder pluralRuleBuilder = new StringBuilder();
            for (Count count : countToRule.keySet()) {
                if (pluralRuleBuilder.length() != 0) {
                    pluralRuleBuilder.append(';');
                }
                pluralRuleBuilder.append(count).append(':').append(countToRule.get(count));
            }
            pluralRulesString = pluralRuleBuilder.toString();
            try {
                pluralRules = PluralRules.parseDescription(pluralRulesString);
            } catch (ParseException e) {
                throw new IllegalArgumentException("Can't create plurals from <" + pluralRulesString + ">", e);
            }
            EnumSet<Count> _keywords = EnumSet.noneOf(Count.class);
            EnumSet<Count> _integerKeywords = EnumSet.noneOf(Count.class);
            EnumSet<Count> _decimalKeywords = EnumSet.noneOf(Count.class);
            for (String s : pluralRules.getKeywords()) {
                Count c = Count.valueOf(s);
                _keywords.add(c);
                if (pluralRules.getDecimalSamples(s, SampleType.DECIMAL) != null) {
                    _decimalKeywords.add(c);
                } else {
                    int debug = 1;
                }
                if (pluralRules.getDecimalSamples(s, SampleType.INTEGER) != null) {
                    _integerKeywords.add(c);
                } else {
                    int debug = 1;
                }
            }
            keywords = Collections.unmodifiableSet(_keywords);
            decimalKeywords = Collections.unmodifiableSet(_decimalKeywords);
            integerKeywords = Collections.unmodifiableSet(_integerKeywords);

            countSampleList = new CountSampleList(pluralRules, keywords, pluralType);

            Map<Count, Set<Double>> countToExampleSetRaw = new TreeMap<Count, Set<Double>>();
            Map<Integer, Count> exampleToCountRaw = new TreeMap<Integer, Count>();

            Output<Map<Count, SampleList[]>> output = new Output();

            // double check
            // if (!targetKeywords.equals(typeToExamples2.keySet())) {
            // throw new IllegalArgumentException ("Problem in plurals " + targetKeywords + ", " + this);
            // }
            // now fix the longer examples
            String otherFractionalExamples = "";
            List<Double> otherFractions = new ArrayList<Double>(0);

            // add fractional samples
            Map<Count, String> countToStringExampleRaw = new TreeMap<Count, String>();
            for (Count type : keywords) {
                SampleList uset = countSampleList.get(type);
                countToStringExampleRaw.put(type, uset.toString(5, 5));
            }
            final String baseOtherExamples = countToStringExampleRaw.get(Count.other);
            String otherExamples = (baseOtherExamples == null ? "" : baseOtherExamples + "; ")
                + otherFractionalExamples + "...";
            countToStringExampleRaw.put(Count.other, otherExamples);

            // Now do double examples (previously unused & not working).
            // Currently a bit of a hack, we should enhance SampleList to make this easier
            // and then use SampleList directly, see http://unicode.org/cldr/trac/ticket/9813
            for (Count type : countToStringExampleRaw.keySet()) {
                Set<Double> doublesSet = new LinkedHashSet<Double>(0);
                String examples = countToStringExampleRaw.get(type);
                if (examples == null) {
                    examples = "";
                }
                String strippedExamples = examples.replaceAll("(, …)|(; ...)", "");
                String[] exampleArray = strippedExamples.split("(, )|(-)");
                for (String example : exampleArray) {
                    if (example == null || example.length() == 0) {
                        continue;
                    }
                    Double doubleValue = Double.valueOf(example);
                    doublesSet.add(doubleValue);
                }
                doublesSet = Collections.unmodifiableSet(doublesSet);
                countToExampleSetRaw.put(type, doublesSet);
            }

            countToExampleSet = Collections.unmodifiableMap(countToExampleSetRaw);
            countToStringExample = Collections.unmodifiableMap(countToStringExampleRaw);
            exampleToCount = Collections.unmodifiableMap(exampleToCountRaw);
            Set<String> temp = new LinkedHashSet<String>();
            // String keyword = pluralRules.select(0.0d);
            // double value = pluralRules.getUniqueKeywordValue(keyword);
            // if (value == pluralRules.NO_UNIQUE_VALUE) {
            // temp.add("0");
            // }
            // keyword = pluralRules.select(1.0d);
            // value = pluralRules.getUniqueKeywordValue(keyword);
            // if (value == pluralRules.NO_UNIQUE_VALUE) {
            // temp.add("1");
            // }
            Set<String> keywords = pluralRules.getKeywords();
            for (Count count : Count.values()) {
                String keyword = count.toString();
                if (keywords.contains(keyword)) {
                    temp.add(keyword);
                }
            }
            // if (false) {
            // change to this after rationalizing 0/1
            // temp.add("0");
            // temp.add("1");
            // for (Count count : Count.values()) {
            // temp.add(count.toString());
            // KeywordStatus status = org.unicode.cldr.util.PluralRulesUtil.getKeywordStatus(pluralRules,
            // count.toString(), 0, explicits, true);
            // if (status != KeywordStatus.SUPPRESSED && status != KeywordStatus.INVALID) {
            // temp.add(count.toString());
            // }
            // }
            // }
            canonicalKeywords = Collections.unmodifiableSet(temp);
        }

        public String toString() {
            return countToExampleSet + "; " + exampleToCount + "; " + pluralRules;
        }

        public Map<Count, Set<Double>> getCountToExamplesMap() {
            return countToExampleSet;
        }

        public Map<Count, String> getCountToStringExamplesMap() {
            return countToStringExample;
        }

        public Count getCount(double exampleCount) {
            return Count.valueOf(pluralRules.select(exampleCount));
        }

        public Count getCount(PluralRules.FixedDecimal exampleCount) {
            return Count.valueOf(pluralRules.select(exampleCount));
        }

        public PluralRules getPluralRules() {
            return pluralRules;
        }

        public String getRules() {
            return pluralRulesString;
        }

        public Count getDefault() {
            return null;
        }

        public Set<String> getCanonicalKeywords() {
            return canonicalKeywords;
        }

        public Set<Count> getCounts() {
            return keywords;
        }

        public Set<Count> getCounts(SampleType sampleType) {
            return sampleType == SampleType.DECIMAL ? decimalKeywords : integerKeywords;
        }

        /**
         * Return the integer samples from 0 to 9999. For simplicity and compactness, this is a UnicodeSet, but
         * the interpretation is simply as a list of integers. UnicodeSet.EMPTY is returned if there are none.
         * @param c
         * @return
         */
        public SampleList getSamples9999(Count c) {
            return countSampleList.get(c);
        }

        /**
         * Return the integer samples for the specified digit, eg 1 => 0..9. For simplicity and compactness, this is a UnicodeSet, but
         * the interpretation is simply as a list of integers.
         * @param c
         * @return
         */
        public SampleList getSamples9999(Count c, int digit) {
            return countSampleList.get(c, digit);
        }

        public boolean hasSamples(Count c, int digits) {
            SampleList samples = countSampleList.get(c, digits);
            return samples != null && (samples.fractionSize() > 0 || samples.intSize() > 0);
        }

        public String getRule(Count keyword) {
            return countToRule.get(keyword);
        }

        @Override
        public int compareTo(PluralInfo other) {
            int size1 = this.countToRule.size();
            int size2 = other.countToRule.size();
            int diff = size1 - size2;
            if (diff != 0) {
                return diff;
            }
            Iterator<Count> it1 = countToRule.keySet().iterator();
            Iterator<Count> it2 = other.countToRule.keySet().iterator();
            while (it1.hasNext()) {
                Count a1 = it1.next();
                Count a2 = it2.next();
                diff = a1.ordinal() - a2.ordinal();
                if (diff != 0) {
                    return diff;
                }
            }
            return pluralRules.compareTo(other.pluralRules);
        }

        enum MinMax {
            MIN, MAX
        }

        public static final FixedDecimal NEGATIVE_INFINITY = new FixedDecimal(Double.NEGATIVE_INFINITY, 0, 0);
        public static final FixedDecimal POSITIVE_INFINITY = new FixedDecimal(Double.POSITIVE_INFINITY, 0, 0);

        static double doubleValue(FixedDecimal a) {
            return a.doubleValue();
        }

        public boolean rangeExists(Count s, Count e, Output<FixedDecimal> minSample, Output<FixedDecimal> maxSample) {
            if (!getCounts().contains(s) || !getCounts().contains(e)) {
                return false;
            }
            FixedDecimal temp;
            minSample.value = getLeastIn(s, SampleType.INTEGER, NEGATIVE_INFINITY, POSITIVE_INFINITY);
            temp = getLeastIn(s, SampleType.DECIMAL, NEGATIVE_INFINITY, POSITIVE_INFINITY);
            if (lessOrFewerDecimals(temp, minSample.value)) {
                minSample.value = temp;
            }
            maxSample.value = getGreatestIn(e, SampleType.INTEGER, NEGATIVE_INFINITY, POSITIVE_INFINITY);
            temp = getGreatestIn(e, SampleType.DECIMAL, NEGATIVE_INFINITY, POSITIVE_INFINITY);
            if (greaterOrFewerDecimals(temp, maxSample.value)) {
                maxSample.value = temp;
            }
            // if there is no range, just return
            if (doubleValue(minSample.value) >= doubleValue(maxSample.value)) {
                return false;
            }
            // see if we can get a better range, with not such a large end range

            FixedDecimal lowestMax = new FixedDecimal(doubleValue(minSample.value) + 0.00001, 5);
            SampleType bestType = getCounts(SampleType.INTEGER).contains(e) ? SampleType.INTEGER : SampleType.DECIMAL;
            temp = getLeastIn(e, bestType, lowestMax, POSITIVE_INFINITY);
            if (lessOrFewerDecimals(temp, maxSample.value)) {
                maxSample.value = temp;
            }
            if (maxSample.value.getSource() > 100000) {
                temp = getLeastIn(e, bestType, lowestMax, POSITIVE_INFINITY);
                if (lessOrFewerDecimals(temp, maxSample.value)) {
                    maxSample.value = temp;
                }
            }

            return true;
        }

        public boolean greaterOrFewerDecimals(FixedDecimal a, FixedDecimal b) {
            return doubleValue(a) > doubleValue(b)
                || doubleValue(b) == doubleValue(a) && b.getDecimalDigits() > a.getDecimalDigits();
        }

        public boolean lessOrFewerDecimals(FixedDecimal a, FixedDecimal b) {
            return doubleValue(a) < doubleValue(b)
                || doubleValue(b) == doubleValue(a) && b.getDecimalDigits() > a.getDecimalDigits();
        }

        private FixedDecimal getLeastIn(Count s, SampleType sampleType, FixedDecimal min, FixedDecimal max) {
            FixedDecimal result = POSITIVE_INFINITY;
            FixedDecimalSamples sSamples1 = pluralRules.getDecimalSamples(s.toString(), sampleType);
            if (sSamples1 != null) {
                for (FixedDecimalRange x : sSamples1.samples) {
                    // overlap in ranges??
                    if (doubleValue(x.start) > doubleValue(max)
                        || doubleValue(x.end) < doubleValue(min)) {
                        continue; // no, continue
                    }
                    // get restricted range
                    FixedDecimal minOverlap = greaterOrFewerDecimals(min, x.start) ? max : x.start;
                    //FixedDecimal maxOverlap = lessOrFewerDecimals(max, x.end) ? max : x.end;

                    // replace if better
                    if (lessOrFewerDecimals(minOverlap, result)) {
                        result = minOverlap;
                    }
                }
            }
            return result;
        }

        private FixedDecimal getGreatestIn(Count s, SampleType sampleType, FixedDecimal min, FixedDecimal max) {
            FixedDecimal result = NEGATIVE_INFINITY;
            FixedDecimalSamples sSamples1 = pluralRules.getDecimalSamples(s.toString(), sampleType);
            if (sSamples1 != null) {
                for (FixedDecimalRange x : sSamples1.samples) {
                    // overlap in ranges??
                    if (doubleValue(x.start) > doubleValue(max)
                        || doubleValue(x.end) < doubleValue(min)) {
                        continue; // no, continue
                    }
                    // get restricted range
                    //FixedDecimal minOverlap = greaterOrFewerDecimals(min, x.start) ? max : x.start;
                    FixedDecimal maxOverlap = lessOrFewerDecimals(max, x.end) ? max : x.end;

                    // replace if better
                    if (greaterOrFewerDecimals(maxOverlap, result)) {
                        result = maxOverlap;
                    }
                }
            }
            return result;
        }

        public static FixedDecimal getNonZeroSampleIfPossible(FixedDecimalSamples exampleList) {
            Set<FixedDecimalRange> sampleSet = exampleList.getSamples();
            FixedDecimal sampleDecimal = null;
            // skip 0 if possible
            for (FixedDecimalRange range : sampleSet) {
                sampleDecimal = range.start;
                if (sampleDecimal.getSource() != 0.0) {
                    break;
                }
                sampleDecimal = range.end;
                if (sampleDecimal.getSource() != 0.0) {
                    break;
                }
            }
            return sampleDecimal;
        }
    }

    /**
     * @deprecated use {@link #getPlurals(PluralType)} instead
     */
    public Set<String> getPluralLocales() {
        return getPluralLocales(PluralType.cardinal);
    }

    /**
     * @param type
     * @return the set of locales that have rules for the specified plural type
     */
    public Set<String> getPluralLocales(PluralType type) {
        return localeToPluralInfo2.get(type).keySet();
    }

    public Set<String> getPluralRangesLocales() {
        return localeToPluralRanges.keySet();
    }

    public PluralRanges getPluralRanges(String locale) {
        return localeToPluralRanges.get(locale);
    }

    /**
     * @deprecated use {@link #getPlurals(PluralType, String)} instead
     */
    public PluralInfo getPlurals(String locale) {
        return getPlurals(locale, true);
    }

    /**
     * Returns the plural info for a given locale.
     *
     * @param locale
     * @return
     */
    public PluralInfo getPlurals(PluralType type, String locale) {
        return getPlurals(type, locale, true);
    }

    /**
     * @deprecated use {@link #getPlurals(PluralType, String, boolean)} instead.
     */
    public PluralInfo getPlurals(String locale, boolean allowRoot) {
        return getPlurals(PluralType.cardinal, locale, allowRoot);
    }

    /**
     * Returns the plural info for a given locale.
     *
     * @param locale
     * @param allowRoot
     * @param type
     * @return
     */
    public PluralInfo getPlurals(PluralType type, String locale, boolean allowRoot) {
        Map<String, PluralInfo> infoMap = localeToPluralInfo2.get(type);
        while (locale != null) {
            if (!allowRoot && locale.equals("root")) {
                break;
            }
            PluralInfo result = infoMap.get(locale);
            if (result != null) {
                return result;
            }
            locale = LocaleIDParser.getSimpleParent(locale);
        }
        return null;
    }

    public DayPeriodInfo getDayPeriods(DayPeriodInfo.Type type, String locale) {
        Map<String, DayPeriodInfo> map1 = typeToLocaleToDayPeriodInfo.get(type);
        while (locale != null) {
            DayPeriodInfo result = map1.get(locale);
            if (result != null) {
                return result;
            }
            locale = LocaleIDParser.getSimpleParent(locale);
        }
        return null;
    }

    public Set<String> getDayPeriodLocales(DayPeriodInfo.Type type) {
        return typeToLocaleToDayPeriodInfo.get(type).keySet();
    }

    private static CurrencyNumberInfo DEFAULT_NUMBER_INFO = new CurrencyNumberInfo(2, -1, -1, -1);

    public CurrencyNumberInfo getCurrencyNumberInfo(String currency) {
        CurrencyNumberInfo result = currencyToCurrencyNumberInfo.get(currency);
        if (result == null) {
            result = DEFAULT_NUMBER_INFO;
        }
        return result;
    }

    /**
     * Returns ordered set of currency data information
     *
     * @param territory
     * @return
     */
    public Set<CurrencyDateInfo> getCurrencyDateInfo(String territory) {
        return territoryToCurrencyDateInfo.getAll(territory);
    }

    /**
     * Returns ordered set of currency data information
     *
     * @param territory
     * @return
     */
    public Set<String> getCurrencyTerritories() {
        return territoryToCurrencyDateInfo.keySet();
    }

    /**
     * Returns the ISO4217 currency code of the default currency for a given
     * territory. The default currency is the first one listed which is legal
     * tender at the present moment.
     *
     * @param territory
     * @return
     */
    public String getDefaultCurrency(String territory) {

        Set<CurrencyDateInfo> targetCurrencyInfo = getCurrencyDateInfo(territory);
        String result = "XXX";
        Date now = new Date();
        for (CurrencyDateInfo cdi : targetCurrencyInfo) {
            if (cdi.getStart().before(now) && cdi.getEnd().after(now) && cdi.isLegalTender()) {
                result = cdi.getCurrency();
                break;
            }
        }
        return result;
    }

    /**
     * Returns the ISO4217 currency code of the default currency for a given
     * CLDRLocale. The default currency is the first one listed which is legal
     * tender at the present moment.
     *
     * @param territory
     * @return
     */
    public String getDefaultCurrency(CLDRLocale loc) {
        return getDefaultCurrency(loc.getCountry());
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

    private List<String> serialElements;
    private Collection<String> distinguishingAttributes;

//    @Deprecated
//    public List<String> getSerialElements() {
//        return serialElements;
//    }

//    @Deprecated
//    public Collection<String> getDistinguishingAttributes() {
//        return distinguishingAttributes;
//    }

    public List<R4<String, String, Integer, Boolean>> getLanguageMatcherData(String string) {
        return languageMatch.get(string);
    }

    public Set<String> getLanguageMatcherKeys() {
        return languageMatch.keySet();
    }

    /**
     * Return mapping from type to territory to data. 001 is the default.
     */
    public Map<MeasurementType, Map<String, String>> getTerritoryMeasurementData() {
        return measurementData;
    }

    /**
     * Return mapping from keys to subtypes
     */
    public Relation<String, String> getBcp47Keys() {
        return bcp47Key2Subtypes;
    }

    /**
     * Return mapping from extensions to keys
     */
    public Relation<String, String> getBcp47Extension2Keys() {
        return bcp47Extension2Keys;
    }

    /**
     * Return mapping from &lt;key,subtype> to aliases
     */
    public Relation<R2<String, String>, String> getBcp47Aliases() {
        return bcp47Aliases;
    }

    /**
     * Return mapping from &lt;key,subtype> to description
     */
    public Map<R2<String, String>, String> getBcp47Descriptions() {
        return bcp47Descriptions;
    }

    /**
     * Return mapping from &lt;key,subtype> to since
     */
    public Map<R2<String, String>, String> getBcp47Since() {
        return bcp47Since;
    }

    /**
     * Return mapping from &lt;key,subtype> to preferred
     */
    public Map<R2<String, String>, String> getBcp47Preferred() {
        return bcp47Preferred;
    }

    /**
     * Return mapping from &lt;key,subtype> to deprecated
     */
    public Map<R2<String, String>, String> getBcp47Deprecated() {
        return bcp47Deprecated;
    }

    /**
     * Return mapping from subtype to deprecated
     */
    public Map<String, String> getBcp47ValueType() {
        return bcp47ValueType;
    }


    static Set<String> MainTimeZones;;

    /**
     * Return canonical timezones
     *
     * @return
     */
    public Set<String> getCanonicalTimeZones() {
        synchronized (SupplementalDataInfo.class) {
            if (MainTimeZones == null) {
                MainTimeZones = new TreeSet<String>();
                SupplementalDataInfo info = SupplementalDataInfo.getInstance();
                for (Entry<R2<String, String>, Set<String>> entry : info.getBcp47Aliases().keyValuesSet()) {
                    R2<String, String> subtype_aliases = entry.getKey();
                    if (!subtype_aliases.get0().equals("timezone")) {
                        continue;
                    }
                    MainTimeZones.add(entry.getValue().iterator().next());
                }
                MainTimeZones = Collections.unmodifiableSet(MainTimeZones);
            }
            return MainTimeZones;
        }
    }

    public Set<MetaZoneRange> getMetaZoneRanges(String zone) {
        return zoneToMetaZoneRanges.get(zone);
    }

    /**
     * Return the metazone containing this zone at this date
     *
     * @param zone
     * @param date
     * @return
     */
    public MetaZoneRange getMetaZoneRange(String zone, long date) {
        Set<MetaZoneRange> metazoneRanges = zoneToMetaZoneRanges.get(zone);
        if (metazoneRanges != null) {
            for (MetaZoneRange metazoneRange : metazoneRanges) {
                if (metazoneRange.dateRange.getFrom() <= date && date < metazoneRange.dateRange.getTo()) {
                    return metazoneRange;
                }
            }
        }
        return null;
    }

    public boolean isDeprecated(DtdType type, String element, String attribute, String value) {
        return DtdData.getInstance(type).isDeprecated(element, attribute, value);
    }

    public boolean isDeprecated(DtdType type, String path) {

        XPathParts parts = XPathParts.getInstance(path);
        for (int i = 0; i < parts.size(); ++i) {
            String element = parts.getElement(i);
            if (isDeprecated(type, element, "*", "*")) {
                return true;
            }
            for (Entry<String, String> entry : parts.getAttributes(i).entrySet()) {
                String attribute = entry.getKey();
                String value = entry.getValue();
                if (isDeprecated(type, element, attribute, value)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns map of ID/Type/Value, such as id="$integer" type="regex" value=[0-9]+
     * @return
     */
    public Map<String, R2<String, String>> getValidityInfo() {
        return validityInfo;
    }

    public Set<String> getCLDRLanguageCodes() {
        return CLDRLanguageCodes;
    }

    public boolean isCLDRLanguageCode(String code) {
        return CLDRLanguageCodes.contains(code);
    }

    public Set<String> getCLDRScriptCodes() {
        return CLDRScriptCodes;
    }

    public boolean isCLDRScriptCode(String code) {
        return CLDRScriptCodes.contains(code);
    }

    private synchronized void initCLDRLocaleBasedData() throws InternalError {
        // This initialization depends on SDI being initialized.
        if (defaultContentToBase == null) {
            Map<CLDRLocale, CLDRLocale> p2c = new TreeMap<CLDRLocale, CLDRLocale>();
            Map<CLDRLocale, CLDRLocale> c2p = new TreeMap<CLDRLocale, CLDRLocale>();
            TreeSet<CLDRLocale> tmpAllLocales = new TreeSet<CLDRLocale>();
            // copied from SupplementalData.java - CLDRLocale based
            for (String l : defaultContentLocales) {
                CLDRLocale child = CLDRLocale.getInstance(l);
                tmpAllLocales.add(child);
            }

            for (CLDRLocale child : tmpAllLocales) {
                // Find a parent of this locale which is NOT itself also a defaultContent
                CLDRLocale nextParent = child.getParent();
                // /System.err.println(">> considering " + child + " with parent " + nextParent);
                while (nextParent != null) {
                    if (!tmpAllLocales.contains(nextParent)) { // Did we find a parent that's also not itself a
                        // defaultContent?
                        // /System.err.println(">>>> Got 1? considering " + child + " with parent " + nextParent);
                        break;
                    }
                    // /System.err.println(">>>>> considering " + child + " with parent " + nextParent);
                    nextParent = nextParent.getParent();
                }
                // parent
                if (nextParent == null) {
                    throw new InternalError("SupplementalDataInfo.defaultContentToChild(): No valid parent for "
                        + child);
                } else if (nextParent == CLDRLocale.ROOT || nextParent == CLDRLocale.getInstance("root")) {
                    throw new InternalError(
                        "SupplementalDataInfo.defaultContentToChild(): Parent is root for default content locale "
                            + child);
                } else {
                    c2p.put(child, nextParent); // wo_Arab_SN -> wo
                    CLDRLocale oldChild = p2c.get(nextParent);
                    if (oldChild != null) {
                        CLDRLocale childParent = child.getParent();
                        if (!childParent.equals(oldChild)) {
                            throw new InternalError(
                                "SupplementalData.defaultContentToChild(): defaultContent list in wrong order? Tried to map "
                                    + nextParent + " -> " + child + ", replacing " + oldChild + " (should have been "
                                    + childParent + ")");
                        }
                    }
                    p2c.put(nextParent, child); // wo -> wo_Arab_SN
                }
            }

            // done, save the hashtables..
            baseToDefaultContent = Collections.unmodifiableMap(p2c); // wo -> wo_Arab_SN
            defaultContentToBase = Collections.unmodifiableMap(c2p); // wo_Arab_SN -> wo
        }
    }

    public Map<String, PreferredAndAllowedHour> getTimeData() {
        return timeData;
    }

    public String getDefaultScript(String baseLanguage) {
        String ls = likelySubtags.get(baseLanguage);
        if (ls == null) {
            return UNKNOWN_SCRIPT;
        }
        LocaleIDParser lp = new LocaleIDParser().set(ls);
        String defaultScript = lp.getScript();
        if (defaultScript.length() > 0) {
            return defaultScript;
        } else {
            return UNKNOWN_SCRIPT;
        }
    }

    private XEquivalenceClass<String, String> equivalentLocales = null;

    public Set<String> getEquivalentsForLocale(String localeId) {
        if (equivalentLocales == null) {
            equivalentLocales = getEquivalentsForLocale();
        }
        Set<String> result = new TreeSet(LENGTH_FIRST);
        result.add(localeId);
        Set<String> equiv = equivalentLocales.getEquivalences(localeId);
        //        if (equiv == null) {
        //            result.add(localeId);
        //            return result;
        //        }
        if (equiv != null) {
            result.addAll(equivalentLocales.getEquivalences(localeId));
        }
        Map<String, String> likely = getLikelySubtags();
        String newMax = LikelySubtags.maximize(localeId, likely);
        if (newMax != null) {
            result.add(newMax);
            newMax = LikelySubtags.minimize(localeId, likely, true);
            if (newMax != null) {
                result.add(newMax);
            }
            newMax = LikelySubtags.minimize(localeId, likely, false);
            if (newMax != null) {
                result.add(newMax);
            }
        }

        //        if (result.size() == 1) {
        //            LanguageTagParser ltp = new LanguageTagParser().set(localeId);
        //            if (ltp.getScript().isEmpty()) {
        //                String ds = getDefaultScript(ltp.getLanguage());
        //                if (ds != null) {
        //                    ltp.setScript(ds);
        //                    result.add(ltp.toString());
        //                }
        //            }
        //        }
        return result;
    }

    public final static class LengthFirstComparator<T> implements Comparator<T> {
        public int compare(T a, T b) {
            String as = a.toString();
            String bs = b.toString();
            if (as.length() < bs.length())
                return -1;
            if (as.length() > bs.length())
                return 1;
            return as.compareTo(bs);
        }
    }

    public static final LengthFirstComparator LENGTH_FIRST = new LengthFirstComparator();

    private synchronized XEquivalenceClass<String, String> getEquivalentsForLocale() {
        SupplementalDataInfo sdi = this;
        Relation<String, String> localeToDefaultContents = Relation.of(new HashMap<String, Set<String>>(),
            LinkedHashSet.class);

        Set<String> dcl = sdi.getDefaultContentLocales();
        Map<String, String> likely = sdi.getLikelySubtags();
        XEquivalenceClass<String, String> locales = new XEquivalenceClass<String, String>();
        LanguageTagParser ltp = new LanguageTagParser();
        Set<String> temp = new HashSet<String>();
        for (Entry<String, String> entry : likely.entrySet()) {
            String source = entry.getKey();
            if (source.startsWith("und")) {
                continue;
            }
            for (String s : getCombinations(source, ltp, likely, temp)) {
                locales.add(source, s);
            }
            for (String s : getCombinations(entry.getValue(), ltp, likely, temp)) {
                locales.add(source, s);
            }
        }
        //        Set<String> sorted = new TreeSet(locales.getExplicitItems());
        //        for (String s : sorted) {
        //            System.out.println(locales.getEquivalences(s));
        //        }
        for (String defaultContentLocale : dcl) {
            if (defaultContentLocale.startsWith("zh")) {
                int x = 0;
            }
            Set<String> set = locales.getEquivalences(defaultContentLocale);

            String parent = LocaleIDParser.getSimpleParent(defaultContentLocale);
            if (!set.contains(parent)) {
                localeToDefaultContents.put(parent, defaultContentLocale);
                //System.out.println("Mismatch " + parent + ", " + set);
            }
            if (parent.contains("_")) {
                continue;
            }
            // only base locales after this point
            String ds = sdi.getDefaultScript(parent);
            if (ds != null) {
                ltp.set(parent);
                ltp.setScript(ds);
                String trial = ltp.toString();
                if (!set.contains(trial)) {
                    //System.out.println("Mismatch " + trial + ", " + set);
                    localeToDefaultContents.put(parent, trial);
                }
            }
        }
        return locales;
    }

    private Set<String> getCombinations(String source, LanguageTagParser ltp, Map<String, String> likely,
        Set<String> locales) {
        locales.clear();

        String max = LikelySubtags.maximize(source, likely);
        locales.add(max);

        ltp.set(source);
        ltp.setScript("");
        String trial = ltp.toString();
        String newMax = LikelySubtags.maximize(trial, likely);
        if (Objects.equals(newMax, max)) {
            locales.add(trial);
        }

        ltp.set(source);
        ltp.setRegion("");
        trial = ltp.toString();
        newMax = LikelySubtags.maximize(trial, likely);
        if (Objects.equals(newMax, max)) {
            locales.add(trial);
        }

        return locales;
    }

    public VersionInfo getCldrVersion() {
        return cldrVersion;
    }

    public File getDirectory() {
        return directory;
    }

    public final static Splitter WHITESPACE_SPLTTER = Splitter.on(PatternCache.get("\\s+")).omitEmptyStrings();

    public static final class AttributeValidityInfo {
        //<attributeValues elements="alias" attributes="path" type="path">notDoneYet</attributeValues>

        final String type;
        final Set<DtdType> dtds;
        final Set<String> elements;
        final Set<String> attributes;
        final String order;

        @Override
        public String toString() {
            return "type:" + type
                + ", elements:" + elements
                + ", attributes:" + attributes
                + ", order:" + order;
        }

        static void add(Map<String, String> inputAttibutes, String inputValue, Map<AttributeValidityInfo, String> data) {
            final AttributeValidityInfo key = new AttributeValidityInfo(
                inputAttibutes.get("dtds"),
                inputAttibutes.get("type"),
                inputAttibutes.get("attributes"),
                inputAttibutes.get("elements"),
                inputAttibutes.get("order"));
            if (data.containsKey(key)) {
                throw new IllegalArgumentException(key + " declared twice");
            }
            data.put(key, inputValue);
        }

        public AttributeValidityInfo(String dtds, String type, String attributes, String elements, String order) {
            if (dtds == null) {
                this.dtds = Collections.singleton(DtdType.ldml);
            } else {
                Set<DtdType> temp = EnumSet.noneOf(DtdType.class);
                for (String s : WHITESPACE_SPLTTER.split(dtds)) {
                    temp.add(DtdType.valueOf(s));
                }
                this.dtds = Collections.unmodifiableSet(temp);
            }
            this.type = type != null ? type : order != null ? "choice" : null;
            this.elements = elements == null ? Collections.EMPTY_SET
                : With.in(WHITESPACE_SPLTTER.split(elements)).toUnmodifiableCollection(new HashSet<String>());
            this.attributes = With.in(WHITESPACE_SPLTTER.split(attributes)).toUnmodifiableCollection(new HashSet<String>());
            this.order = order;
        }

        public String getType() {
            return type;
        }

        public Set<DtdType> getDtds() {
            return dtds;
        }

        public Set<String> getElements() {
            return elements;
        }

        public Set<String> getAttributes() {
            return attributes;
        }

        public String getOrder() {
            return order;
        }

        @Override
        public boolean equals(Object obj) {
            AttributeValidityInfo other = (AttributeValidityInfo) obj;
            return CldrUtility.deepEquals(
                type, other.type,
                dtds, other.dtds,
                elements, other.elements,
                attributes, other.attributes,
                order, other.order);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, dtds, elements, attributes, order);
        }
    }

    public Map<AttributeValidityInfo, String> getAttributeValidity() {
        return attributeValidityInfo;
    }

    public Multimap<String, String> getLanguageGroups() {
        return languageGroups;
    }
}
