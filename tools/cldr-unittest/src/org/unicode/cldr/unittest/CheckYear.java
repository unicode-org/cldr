package org.unicode.cldr.unittest;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.With;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.DateTimePatternGenerator.VariableField;

public class CheckYear {
    static CLDRConfig testInfo = CLDRConfig.getInstance();
    private static final StandardCodes STANDARD_CODES = StandardCodes.make();
    private static final String LOCALES = ".*";
    private static final String[] STOCK = { "short", "medium", "long", "full" };

    enum Category {
        Year2_MonthNumeric, Year2_Other, Year4_MonthNumeric, Year4_Other
    }

    static DateTimePatternGenerator dtp = DateTimePatternGenerator
        .getEmptyInstance();
    static DateTimePatternGenerator.FormatParser formatParser = new DateTimePatternGenerator.FormatParser();

    // mismatches between stocks
    static Map<String, Relation<String, String>> stock2skeleton2locales = new LinkedHashMap<String, Relation<String, String>>();
    static {
        for (String stock : STOCK) {
            stock2skeleton2locales.put("date-" + stock, Relation.of(
                new TreeMap<String, Set<String>>(), TreeSet.class));
        }
        for (String stock : STOCK) {
            stock2skeleton2locales.put("time-" + stock, Relation.of(
                new TreeMap<String, Set<String>>(), TreeSet.class));
        }
    }

    static class LocaleInfo {
        private static final boolean DEBUG = false;
        // information on the type of years
        Relation<Category, String> category2base = Relation.of(
            new EnumMap<Category, Set<String>>(Category.class),
            TreeSet.class);
        // collisions between baseSkeletons
        Map<String, Relation<String, Row.R2<String, String>>> base2BasePatterns2Info = new TreeMap<String, Relation<String, Row.R2<String, String>>>();

        Map<String, String> skeleton2pattern = new HashMap<String, String>();

        public void recordStockTime(String localeId, String stock,
            String dateTimePattern) {
            String skeleton = dtp.getSkeleton(dateTimePattern);
            String base = getBaseSkeleton(skeleton);
            stock2skeleton2locales.get("time-" + stock).put(skeleton, localeId);
            recordBase(base, skeleton, dateTimePattern);
        }

        public void recordStock(String localeId, String stock,
            String dateTimePattern) {
            String skeleton = dtp.getSkeleton(dateTimePattern);
            String base = getBaseSkeleton(skeleton);
            stock2skeleton2locales.get("date-" + stock).put(
                skeleton.replace("yyyy", "y"), localeId);
            String key = skeleton + "*" + stock.charAt(0);
            recordBase(base, skeleton, dateTimePattern);
            recordYearStuff(key, dateTimePattern);
        }

        public void record(String skeleton, String dateTimePattern) {
            String base = getBaseSkeleton(skeleton);
            recordBase(base, skeleton, dateTimePattern);
            recordYearStuff(skeleton, dateTimePattern);
        }

        public void recordBase(String base, String skeleton,
            String dateTimePattern) {
            String coreBase = getCoreSkeleton(base);
            Relation<String, Row.R2<String, String>> basePatterns2Info = base2BasePatterns2Info
                .get(coreBase);
            if (basePatterns2Info == null) {
                base2BasePatterns2Info
                    .put(coreBase,
                        basePatterns2Info = Relation
                            .of(new TreeMap<String, Set<Row.R2<String, String>>>(),
                                TreeSet.class));
            }
            // adjust the pattern to correspond to the base fields
            // String coreSkeleton = getCoreSkeleton(skeleton);
            String minimizedPattern = replaceFieldTypes(dateTimePattern,
                coreBase, !coreBase.equals(base));
            basePatterns2Info.put(minimizedPattern,
                Row.of(skeleton, dateTimePattern));
            // if (skeleton2pattern.put(skeleton, basePattern) != null) {
            // throw new IllegalArgumentException();
            // }
        }

        public String getCoreSkeleton(String skeleton) {
            int slashPos = skeleton.indexOf('/');
            String s = slashPos < 0 ? skeleton : skeleton
                .substring(0, slashPos);
            return s;
        }

        private void recordYearStuff(String skeleton, String dateTimePattern) {
            // do the year stuff
            if (!dateTimePattern.contains("y")) {
                return;
            }
            boolean isDigit4 = true;
            if (dateTimePattern.contains("yyyy")) {
                // nothing
            } else if (dateTimePattern.contains("yy")) {
                isDigit4 = false;
            }
            boolean monthNumeric = false;
            if (dateTimePattern.contains("MMM")
                || dateTimePattern.contains("LLL")) {
                // nothing
            } else if (dateTimePattern.contains("M")
                || dateTimePattern.contains("L")) {
                monthNumeric = true;
            }
            if (isDigit4) {
                if (monthNumeric) {
                    category2base.put(Category.Year4_MonthNumeric, skeleton);
                } else {
                    category2base.put(Category.Year4_Other, skeleton);
                }
            } else {
                if (monthNumeric) {
                    category2base.put(Category.Year2_MonthNumeric, skeleton);
                } else {
                    category2base.put(Category.Year2_Other, skeleton);
                }
            }
        }

        public String replaceFieldTypes(String dateTimePattern,
            String skeleton, boolean isInterval) {
            if (!isInterval) {
                return replaceFieldPartsCompletely(dateTimePattern, skeleton);
            } else {
                String part = getCorePattern(dateTimePattern);
                return replaceFieldPartsCompletely(part, skeleton);
            }
        }

        public String replaceFieldPartsCompletely(String dateTimePattern,
            String skeleton) {
            String minimizedPattern = dtp.replaceFieldTypes(dateTimePattern,
                skeleton);

            // fix numerics
            StringBuilder result = new StringBuilder();
            for (Object item : formatParser.set(minimizedPattern).getItems()) {
                if (item instanceof String) {
                    Object quoteLiteral = formatParser.quoteLiteral(item
                        .toString());
                    result.append(quoteLiteral);
                } else {
                    VariableField item2 = (DateTimePatternGenerator.VariableField) item;
                    if (item2.isNumeric()) {
                        result.append(item.toString().charAt(0));
                    } else {
                        result.append(item);
                    }
                }
            }
            String resultString = result.toString();
            return resultString;
        }

        private String getCorePattern(String intervalPattern) {
            // get up to the first duplicate field. Then compare the result on
            // both sides
            StringBuilder b = new StringBuilder();
            StringBuilder result = new StringBuilder();
            boolean firstPart = true;
            int endFirstPart = -1;
            int startSecondPart = -1;
            int goodSoFar = -1;
            Set<Integer> firstComponents = new HashSet<Integer>();
            Set<Integer> secondComponents = new HashSet<Integer>();
            for (Object item : formatParser.set(intervalPattern).getItems()) {
                if (item instanceof String) {
                    Object quoteLiteral = formatParser.quoteLiteral(item.toString());
                    b.append(quoteLiteral);
                    goodSoFar = result.length();
                    result.append(quoteLiteral);
                } else {
                    VariableField item2 = (DateTimePatternGenerator.VariableField) item;
                    int type = item2.getType();
                    if (firstPart && firstComponents.contains(type)) {
                        firstPart = false;
                        startSecondPart = b.length();
                    }
                    b.append(item);
                    if (firstPart) {
                        endFirstPart = b.length();
                        firstComponents.add(type);
                        result.append(item);
                    } else {
                        secondComponents.add(type);
                        if (firstComponents.contains(type)) {
                            result.setLength(goodSoFar);
                        } else {
                            result.append(item);
                        }
                    }
                }
            }
            String normalized = b.toString();
            if (!normalized.equals(intervalPattern)) {
                System.out.println("Not normalized: " + intervalPattern + "\t"
                    + normalized);
            }
            if (endFirstPart < 0 || startSecondPart < 0) {
                throw new IllegalArgumentException("Illegal interval pattern: "
                    + intervalPattern);
            } else {
                if (DEBUG)
                    System.out.println(normalized.substring(0, endFirstPart)
                        + "$$"
                        + normalized.substring(endFirstPart,
                            startSecondPart)
                        + "$$"
                        + normalized.substring(startSecondPart) + "\t=>\t"
                        + result);
            }
            return result.toString();
        }

        private String getBaseSkeleton(String skeleton) {
            int slashPos = skeleton.indexOf('/');
            String core = skeleton;
            String diff = "";
            if (slashPos >= 0) {
                core = skeleton.substring(0, slashPos);
                diff = skeleton.substring(slashPos);
            }
            core = dtp.getBaseSkeleton(core);
            return core + diff;
        }

    }

    static Map<String, LocaleInfo> data = new TreeMap<String, LocaleInfo>();

    // private static final Relation<String,String> digit4 = Relation.of(new
    // TreeMap<String,Set<String>>(),
    // TreeSet.class);
    // private static final Relation<String,String> digit2 = Relation.of(new
    // TreeMap<String,Set<String>>(),
    // TreeSet.class);

    public static void main(String[] args) throws IOException {
        CLDRFile englishFile = testInfo.getEnglish();

        Factory factory = Factory.make(CLDRPaths.TMP2_DIRECTORY
            + "vxml/common/main/", LOCALES);
        String calendarID = "gregorian";
        System.out.println("Total locales: "
            + factory.getAvailableLanguages().size());
        Map<String, String> sorted = new TreeMap<String, String>();
        SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
        Set<String> defaultContent = sdi.getDefaultContentLocales();
        LanguageTagParser ltp = new LanguageTagParser();

        for (String localeID : factory.getAvailableLanguages()) {
            if (!ltp.set(localeID).getRegion().isEmpty()) {
                continue;
            }
            if (defaultContent.contains(localeID)) {
                System.out.println("Skipping default content: " + localeID);
                continue;
            }
            sorted.put(englishFile.getName(localeID, true), localeID);
            data.put(localeID, new LocaleInfo());
        }

        gatherInfo(factory, calendarID, sorted);

        writeYearWidths(sorted, true, "year-width-diff.txt");
        writeYearWidths(sorted, false, "year-width-diff-other.txt");

        writeConflictingStockItems(true, "conflicting-stock.txt");
        writeConflictingStockItems(false, "conflicting-stock-other.txt");

        writeConflictingPatterns(sorted, true, "conflicting-patterns.txt");
        writeConflictingPatterns(sorted, false,
            "conflicting-patterns-other.txt");
    }

    public static void gatherInfo(Factory factory, String calendarID,
        Map<String, String> sorted) throws IOException {

        for (Entry<String, String> entry : sorted.entrySet()) {
            String localeId = entry.getValue();
            CLDRFile file = factory.make(localeId, true);
            LocaleInfo localeInfo = data.get(localeId);
            for (String stock : STOCK) {
                String path = "//ldml/dates/calendars/calendar[@type=\""
                    + calendarID
                    + "\"]/dateFormats/dateFormatLength[@type=\""
                    + stock
                    + "\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
                String dateTimePattern = file.getStringValue(path);
                localeInfo.recordStock(localeId, stock, dateTimePattern);
                path = "//ldml/dates/calendars/calendar[@type=\""
                    + calendarID
                    + "\"]/timeFormats/timeFormatLength[@type=\""
                    + stock
                    + "\"]/timeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]";
                dateTimePattern = file.getStringValue(path);
                localeInfo.recordStockTime(localeId, stock, dateTimePattern);
            }
            for (String path : With
                .in(file.iterator("//ldml/dates/calendars/calendar[@type=\""
                    + calendarID
                    + "\"]/dateTimeFormats/availableFormats/dateFormatItem"))) {
                XPathParts parts = XPathParts.getTestInstance(path);
                String key = parts.getAttributeValue(-1, "id");
                String value = file.getStringValue(path);
                localeInfo.record(key, value);
            }
            for (String path : With
                .in(file.iterator("//ldml/dates/calendars/calendar[@type=\""
                    + calendarID
                    + "\"]/dateTimeFormats/intervalFormats/intervalFormatItem"))) {
                XPathParts parts = XPathParts.getTestInstance(path);
                String skeleton = parts.getAttributeValue(-2, "id");
                String diff = parts.getAttributeValue(-1, "id");
                String value = file.getStringValue(path);
                localeInfo.record(skeleton + "/" + diff, value);
            }
        }
    }

    public static void writeYearWidths(Map<String, String> sorted,
        boolean modern, String filename) throws IOException {
        PrintWriter out = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY
            + "datecheck/", filename);
        out.println("Name\tid\t"
            + CollectionUtilities.join(Category.values(), "\t"));
        for (Entry<String, String> entry : sorted.entrySet()) {
            String localeId = entry.getValue();
            boolean priority = getPriority(localeId);
            if (modern != priority) {
                continue;
            }
            String name = entry.getKey();
            LocaleInfo localeInfo = data.get(localeId);
            out.print(name + "\t" + localeId);
            for (Category item : Category.values()) {
                Set<String> items = localeInfo.category2base.get(item);
                if (items != null) {
                    out.print("\t" + CollectionUtilities.join(items, " "));
                } else {
                    out.print("\t");
                }
            }
            out.println();
        }
        out.close();
    }

    public static void writeConflictingStockItems(boolean modern,
        String filename) throws IOException {
        PrintWriter out;
        System.out.println("\nMismatched Stock items\n");
        out = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY
            + "datecheck/", filename);
        out.println("Stock\tSkeleton\tLocales");
        for (Entry<String, Relation<String, String>> stockAndSkeleton2locales : stock2skeleton2locales
            .entrySet()) {
            String stock = stockAndSkeleton2locales.getKey();
            for (Entry<String, Set<String>> entry2 : stockAndSkeleton2locales
                .getValue().keyValuesSet()) {
                String filtered = filter(entry2.getValue(), modern);
                if (filtered.isEmpty()) {
                    continue;
                }
                out.println(stock + "\t" + entry2.getKey() + "\t" + filtered);
            }
        }
        out.close();
    }

    private static String filter(Set<String> value, boolean modern) {
        StringBuilder b = new StringBuilder();
        for (String localeId : value) {
            if (modern != getPriority(localeId)) {
                continue;
            }
            if (b.length() != 0) {
                b.append(" ");
            }
            b.append(localeId);
        }
        return b.toString();
    }

    public static void writeConflictingPatterns(Map<String, String> sorted,
        boolean modern, String filename) throws IOException {
        PrintWriter out;
        out = FileUtilities.openUTF8Writer(CLDRPaths.GEN_DIRECTORY
            + "datecheck/", filename);
        out.println("Language\tId\tMin. Skeleton\tMin Pat1\tskeleton → pattern\tMin Pat2\tskeleton → pattern\tMin Pat3\tskeleton → pattern");
        for (Entry<String, String> entry : sorted.entrySet()) {
            String localeId = entry.getValue();
            if (modern != getPriority(localeId)) {
                continue;
            }
            String name = entry.getKey();
            LocaleInfo localeInfo = data.get(localeId);

            for (Entry<String, Relation<String, R2<String, String>>> baseAndBasePatterns2Info : localeInfo.base2BasePatterns2Info
                .entrySet()) {
                String base = baseAndBasePatterns2Info.getKey();
                Relation<String, R2<String, String>> basePatterns2Info = baseAndBasePatterns2Info
                    .getValue();
                if (basePatterns2Info.size() == 1) {
                    continue;
                }
                // Ewe ee MMM LLL → ‹[MMM, LLL]›
                // Ewe ee MMM MMM → ‹[MMM/M, MMM–MMM]›
                // => Ewe ee MMM ‹LLL›: tab MMM → ‹LLL› tab ‹MMM›: tab MMM/M →
                // ‹MMM–MMM›
                StringBuilder s = new StringBuilder(name + "\t" + localeId
                    + "\t" + base);

                for (Entry<String, Set<R2<String, String>>> basePatternsAndInfo : basePatterns2Info
                    .keyValuesSet()) {
                    String basePattern = basePatternsAndInfo.getKey();
                    s.append("\t‹" + basePattern + "›:\t\"");
                    boolean first = true;
                    for (R2<String, String> info : basePatternsAndInfo
                        .getValue()) {
                        if (first) {
                            first = false;
                        } else {
                            s.append(";\n");
                        }
                        s.append(info.get0() + " → ‹" + info.get1() + "›");
                    }
                    s.append("\"");
                }
                out.println(s);
            }
        }
        out.close();
    }

    public static boolean getPriority(String localeId) {
        return STANDARD_CODES.getLocaleCoverageLevel(
            Organization.google.toString(), localeId) == Level.MODERN
            || STANDARD_CODES.getLocaleCoverageLevel(
                Organization.apple.toString(), localeId) == Level.MODERN
            || STANDARD_CODES.getLocaleCoverageLevel(
                Organization.ibm.toString(), localeId) == Level.MODERN;
    }
}
