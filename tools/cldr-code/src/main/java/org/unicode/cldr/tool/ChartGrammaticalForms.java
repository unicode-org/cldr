package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.tool.FormattedFileWriter.Anchors;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.FileCopier;
import org.unicode.cldr.util.GrammarInfo;
import org.unicode.cldr.util.GrammarInfo.GrammaticalFeature;
import org.unicode.cldr.util.GrammarInfo.GrammaticalScope;
import org.unicode.cldr.util.GrammarInfo.GrammaticalTarget;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.Rational.FormatStyle;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.ConversionInfo;
import org.unicode.cldr.util.UnitConverter.PlaceholderLocation;
import org.unicode.cldr.util.UnitConverter.UnitId;
import org.unicode.cldr.util.UnitPathType;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.XPathParts;

import com.google.common.base.Joiner;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.locale.XCldrStub.ImmutableMap;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.PluralRules.SampleType;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

public class ChartGrammaticalForms extends Chart {

    private static final String FORMATTED_SAMPLE = "Formatted Sample";

    private static final String INFO_ON_FEATURES = "Current information is only for nominal forms. "
        + "Where a Usage is present other than “general”, that means that a subset of the grammatical features are relevant to that Usage. "
        + "For example, Feature=grammaticalGender and Usage=units might omit an ‘animate’ gender. "
        + "For the meanings of the values, see "
        + "<a target='spec' href='https://unicode.org/reports/tr35/tr35-general.html#Grammatical_Features'>LDML Grammatical Features</a>.";

    private static final String MAIN_HEADER = "<h2>Grammatical Forms</h2>";
    private static final boolean DEBUG = false;
    private static final String DIR = CLDRPaths.CHART_DIRECTORY + "grammar/";
    public static final PluralRules ENGLISH_PLURAL_RULES = SDI.getPlurals("en").getPluralRules();

    public static void main(String[] args) {
        new ChartGrammaticalForms().writeChart(null);
    }

    @Override
    public String getDirectory() {
        return DIR;
    }

    @Override
    public String getTitle() {
        return "Grammatical Forms Charts";
    }

    @Override
    public String getFileName() {
        return "index";
    }

    @Override
    public String getExplanation() {
        return MAIN_HEADER + "<p>In this version a preliminary set of languages have additional grammatical information, as listed below.<p>";
    }

    @Override
    public void writeContents(FormattedFileWriter pw) throws IOException {
        FileCopier.ensureDirectoryExists(DIR);
        FileCopier.copy(Chart.class, "index.css", DIR);
        FormattedFileWriter.copyIncludeHtmls(DIR);

        FormattedFileWriter.Anchors anchors = new FormattedFileWriter.Anchors();
        writeSubcharts(anchors);
        pw.setIndex("Main Chart Index", "../index.html");
        pw.write(anchors.toString());
        showInfo(pw);
    }

    private void showInfo(FormattedFileWriter pw) throws IOException {
        pw.append("<h2>Grammatical Features Info</h2>");
        pw.append("<p>The following lists the available information about grammatical features for locales. "
            + "Note that only the above locales have localized data, at this time. "
            + INFO_ON_FEATURES
            + "</p>");
        if (GrammaticalTarget.values().length > 1) {
            throw new IllegalArgumentException("Needs adjustment for additional GrammaticalTarget.values()");
        }

        System.out.println(SDI.hasGrammarInfo());

        TablePrinter tablePrinter = getFormattedGrammarInfo(SDI.hasGrammarInfo());
        pw.append(tablePrinter.toString());
    }

    private TablePrinter getFormattedGrammarInfo(Set<String> localeIds) {
        TablePrinter tablePrinter = new TablePrinter()
            .addColumn("Locale", "class='source' width='1%'", null, "class='source'", true)
            .setSortPriority(0)
            .setBreakSpans(true)
            .addColumn("ID", "class='source' width='1%'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
            .setBreakSpans(true)
            .addColumn("Feature", "class='source' width='1%'", null, "class='source'", true)
            .setSortPriority(1)
            .setBreakSpans(true)
            .addColumn("Usage", "class='source'", null, "class='source'", true)
            .addColumn("Values", "class='source'", null, "class='source'", true)
            ;
        for (String localeId : localeIds) {
            if (localeId.equals("fi")) {
                int debug = 0;
            }
            Set<String> failures = new LinkedHashSet<>();
            GrammarInfo grammarInfo = SDI.getGrammarInfo(localeId, false);
            String localeName = CONFIG.getEnglish().getName(localeId);
            for (GrammaticalFeature feature : GrammaticalFeature.values()) {
                Map<GrammaticalScope, Set<String>> scopeToValues = grammarInfo.get(GrammaticalTarget.nominal, feature);
                if (scopeToValues.isEmpty()) {
                    continue;
                }

                Set<String> values = null;
                boolean multiline = false;
                for (Entry<GrammaticalScope, Set<String>> entry : scopeToValues.entrySet()) {
                    if (values == null) {
                        values = entry.getValue();
                    } else if (!values.equals(entry.getValue())) {
                        multiline = true;
                        break;
                    }
                }
                Set<String> sortedValues = new TreeSet(feature.getValueComparator());
                if (multiline) {
                    for (GrammaticalScope usage : GrammaticalScope.values()) {
                        values = scopeToValues.get(usage);
                        if (values.isEmpty())  {
                            continue;
                        }
                        sortedValues.clear();
                        sortedValues.addAll(values);
                        addRow(tablePrinter,
                            localeName,
                            localeId,
                            feature,
                            usage.toString(),
                            Joiner.on(", ").join(sortedValues));
                    }
                } else {
                    try {
                        sortedValues.addAll(values);
                        addRow(tablePrinter,
                            localeName,
                            localeId,
                            feature,
                            Joiner.on(", ").join(scopeToValues.keySet()),
                            Joiner.on(", ").join(sortedValues));
                    } catch (Exception e) {
                        failures.add(e.getMessage());
                    }
                }
            }
            if (!failures.isEmpty()) {
                System.out.println("# Failures, " + localeId + "\t" + failures);
            }
        }
        return tablePrinter;
    }

    public void addRow(TablePrinter tablePrinter, String locale, String id, GrammaticalFeature feature, String usage, final String valueString) {
        tablePrinter.addRow()
        .addCell(locale)
        .addCell(id)
        .addCell(feature)
        .addCell(usage)
        .addCell(valueString)
        .finishRow();
    }

    static final UnitConverter uc = SDI.getUnitConverter();
    static final Map<String, Map<Rational, String>> BASE_TO_FACTOR_TO_UNIT;
    static {
        Map<String, Map<Rational, String>> _BASE_TO_BEST = new TreeMap<>();
        ImmutableSet<String> skip = ImmutableSet.of("mile-scandinavian", "100-kilometer", "dunam");
        Output<String> baseOut = new Output<>();
        for (String longUnit : Validity.getInstance().getStatusToCodes(LstrType.unit).get(Validity.Status.regular)) {
            String shortUnit = uc.getShortId(longUnit);
            System.out.println(shortUnit);
            if (skip.contains(shortUnit)) {
                continue;
            }
            if ("mile-per-gallon".equals(shortUnit)) {
                int debug = 0;
            }
            //Set<String> systems = uc.getSystems(unit);
            ConversionInfo info = uc.parseUnitId(shortUnit, baseOut, false);
            if (info == null) {
                continue;
            }
            Map<Rational, String> factorToUnit = _BASE_TO_BEST.get(baseOut.value);
            if (factorToUnit == null) {
                _BASE_TO_BEST.put(baseOut.value, factorToUnit = new TreeMap<>());
                factorToUnit.put(Rational.ONE, baseOut.value);
            }

            if (!info.factor.isPowerOfTen()) {
                continue;
            }

            String old = factorToUnit.get(info.factor);
            if (old == null || old.length() > shortUnit.length()) {
                factorToUnit.put(info.factor, shortUnit);
            }
        }
        BASE_TO_FACTOR_TO_UNIT = CldrUtility.protectCollection(_BASE_TO_BEST);
        for (Entry<String, Map<Rational, String>> entry : BASE_TO_FACTOR_TO_UNIT.entrySet()) {
            System.out.println(entry);
        }
    }

    class BestUnitForGender implements Comparable<BestUnitForGender> {
        final boolean durationOrLength; // true is better
        final boolean metric; // true is better
        final double distanceFromOne; // zero is better
        final String quantity;
        final String shortUnit;
        public BestUnitForGender(String shortUnit, String quantity, Collection<String> systems, double baseSize) {
            super();
            this.shortUnit = shortUnit;
            this.quantity = quantity;
            this.durationOrLength = quantity.equals("duration") || quantity.equals("length");
            this.metric = systems.contains("metric");
            this.distanceFromOne = Math.abs(Math.log(baseSize));
        }
        @Override
        public int compareTo(BestUnitForGender o) {
            // negation, because we want the best one first
            return ComparisonChain.start()
                .compare(o.durationOrLength, durationOrLength)
                .compare(o.metric, metric)
                .compare(quantity, o.quantity)
                .compare(distanceFromOne, o.distanceFromOne)
                .compare(shortUnit, o.shortUnit)
                .result();
        }
        @Override
        public int hashCode() {
            return shortUnit.hashCode();
        }
        @Override
        public boolean equals(Object obj) {
            return compareTo((BestUnitForGender)obj) == 0;
        }
        @Override
        public String toString() {
            return shortUnit + "(" + (durationOrLength ? "D" : "") + (metric ? "M" : "") + ":" + quantity + ":" + Math.round(distanceFromOne*10) + ")";
        }
    }

    public class TablePrinterWithHeader {
        final String header;
        final TablePrinter tablePrinter;
        public TablePrinterWithHeader(String header, TablePrinter tablePrinter) {
            this.header = header;
            this.tablePrinter = tablePrinter;
        }
    }

    public void writeSubcharts(Anchors anchors) throws IOException {
        Set<String> locales = GrammarInfo.getGrammarLocales();

        LanguageTagParser ltp = new LanguageTagParser();
        //ImmutableSet<String> casesNominativeOnly = ImmutableSet.of(GrammaticalFeature.grammaticalCase.getDefault(null));
        Factory factory = CLDRConfig.getInstance().getCldrFactory();

        Comparator<String> caseOrder = GrammarInfo.CaseValues.COMPARATOR;
        Set<String> sortedCases = new TreeSet<>(caseOrder);

        Comparator<String> genderOrder = GrammarInfo.GenderValues.COMPARATOR;
        Set<String> sortedGenders = new TreeSet<>(genderOrder);

        Output<Double> sizeInBaseUnits = new Output<>();

        // collect the "best unit ordering"
        Map<String, BestUnitForGender> unitToBestUnit = new TreeMap<>();
        Set<String> rawUnitsToAddGrammar = GrammarInfo.getUnitsToAddGrammar();
        for (String longUnit : rawUnitsToAddGrammar) {
            final String shortUnit = uc.getShortId(longUnit);
            if (shortUnit.equals("generic")) {
                continue;
            }
            String unitCell = getBestBaseUnit(uc, shortUnit, sizeInBaseUnits);
            String quantity = shortUnit.contentEquals("generic") ? "temperature" : uc.getQuantityFromUnit(shortUnit, false);

            Set<String> systems = uc.getSystems(shortUnit);
            unitToBestUnit.put(shortUnit, new BestUnitForGender(shortUnit, quantity, systems, sizeInBaseUnits.value));
        }
        unitToBestUnit = ImmutableMap.copyOf(unitToBestUnit);
        // quick check
//        final BestUnitForGender u1 = unitToBestUnit.get("meter");
//        final BestUnitForGender u2 = unitToBestUnit.get("square-centimeter");
//        int comp = u1.compareTo(u2); // should be less

        Set<BestUnitForGender> sorted2 = new TreeSet<>(unitToBestUnit.values());
        System.out.println(sorted2);

        PlaceholderLocation placeholderPosition = PlaceholderLocation.missing;
        Matcher placeholderMatcher = UnitConverter.PLACEHOLDER.matcher("");
        Output<String> unitPatternOut = new Output<>();

        for (String locale : locales) {
            if (locale.equals("root")) {
                continue;
            }
            ltp.set(locale);
            String region = ltp.getRegion();
            if (!region.isEmpty()) {
                continue;
            }
            GrammarInfo grammarInfo = SDI.getGrammarInfo(locale, true);
            if (grammarInfo == null || !grammarInfo.hasInfo(GrammaticalTarget.nominal)) {
                continue;
            }
            CLDRFile cldrFile = factory.make(locale, true);

            {
                Collection<String> genders = grammarInfo.get(GrammaticalTarget.nominal, GrammaticalFeature.grammaticalGender, GrammaticalScope.units);
                sortedGenders.clear();
                sortedGenders.addAll(genders);
            }
            {
                Collection<String> rawCases = grammarInfo.get(GrammaticalTarget.nominal, GrammaticalFeature.grammaticalCase, GrammaticalScope.units);
                if (rawCases.isEmpty()) {
                    rawCases = ImmutableSet.of(GrammaticalFeature.grammaticalCase.getDefault(null));
                }
                sortedCases.clear();
                sortedCases.addAll(rawCases);
            }
            if (sortedCases.size() <= 1 && sortedGenders.size() <= 1) {
                continue;
            }

            //Collection<String> nomCases = rawCases.isEmpty() ? casesNominativeOnly : rawCases;

            PluralInfo plurals = SDI.getPlurals(PluralType.cardinal, locale);
            if (plurals == null) {
                System.err.println("No " + PluralType.cardinal + "  plurals for " + locale);
            }
            Collection<Count> adjustedPlurals = plurals.getAdjustedCounts();
            ICUServiceBuilder isb = ICUServiceBuilder.forLocale(CLDRLocale.getInstance(locale));
            DecimalFormat decFormat = isb.getNumberFormat(1);

            Map<String, TablePrinterWithHeader> info = new LinkedHashMap<>();

            TablePrinter tablePrinter = getFormattedGrammarInfo(Collections.singleton(locale));
            info.put("Grammatical Features", new TablePrinterWithHeader(
                "<p>The following lists the available information about grammatical features for this locale. "
                    + INFO_ON_FEATURES
                    + "</p>"
                    , tablePrinter));

            // because some locales have more units with grammar, get the additional ones. Also grab the minimal pairs

            Set<String> unitsToAddGrammar = new TreeSet<>(rawUnitsToAddGrammar);
            Map<PathHeader,String> minimalInfo = new TreeMap<>();
            PathHeader.Factory phf = PathHeader.getFactory();
            for (String path : cldrFile) {
                if (!path.startsWith("//ldml/units/unitLength[@type=\"long\"]/unit")) {
                    if (path.startsWith("//ldml/numbers/minimalPairs/")) {
                        if (!path.contains("ordinal")) {
                            minimalInfo.put(phf.fromPath(path), cldrFile.getStringValueWithBailey(path));
                        }
                    }
                    continue;
                }
                XPathParts parts = XPathParts.getFrozenInstance(path);
                String foundUnit = parts.getAttributeValue(3, "type");
                if (unitsToAddGrammar.contains(foundUnit)) {
                    continue;
                }
                //ldml/units/unitLength[@type="long"]/unit[@type="duration-decade"]/gender
                //ldml/units/unitLength[@type="long"]/unit[@type="duration-decade"]/unitPattern[@count="one"][@case="accusative"]
                switch (parts.getElement(-1)) {
                case "gender":
                    unitsToAddGrammar.add(foundUnit);
                    break;
                case "unitPattern":
                    if (parts.getAttributeValue(4, "case") != null) {
                        unitsToAddGrammar.add(foundUnit);
                    }
                    break;
                }
            }

            TablePrinter minimalPrinter = new TablePrinter()
                .addColumn("Type", "class='source' width='1%'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
                .setRepeatHeader(true)
                .addColumn("Size", "class='source' width='1%'", null, "class='source'", true)
                .setSortPriority(0)
                .setHidden(true)
                .setBreakSpans(true)
                .addColumn("Code", "class='source' width='1%'", null, "class='source'", true)
                .addColumn("Pattern", "class='source'", null, "class='target'", true)
                .addColumn("Formatted Sample", "class='source'", null, "class='target'", true)
                ;

            int counter = 0;
            ExampleGenerator exampleGenerator = new ExampleGenerator(cldrFile, CONFIG.getEnglish(), null);
            for (Entry<PathHeader, String> entry : minimalInfo.entrySet()) {
                PathHeader pathHeader = entry.getKey();
                String value = entry.getValue();
                minimalPrinter
                .addRow()
                .addCell(pathHeader.getHeader())
                .addCell(counter++)
                .addCell(pathHeader.getCode())
                .addCell(value)
                .addCell(exampleGenerator.getExampleHtml(pathHeader.getOriginalPath(), value));

                // finish the row
                minimalPrinter.finishRow();
            }
            info.put("Minimal Pairs", new TablePrinterWithHeader(
                "<p>This table has the minimal pairs used to test the appropriateness of different values.</p>\n"
                    , minimalPrinter));

            final PluralRules pluralRules = plurals.getPluralRules();
            // set up the table and add the headers

            TablePrinter caseTablePrinter = new TablePrinter()
                .addColumn("Quantity", "class='source' width='1%'", null, "class='source'", true)
                .setSortPriority(0)
                .setRepeatHeader(true)
                .addColumn("Size", "class='source' width='1%'", null, "class='source'", true)
                .setSortPriority(1)
                .setHidden(true)
                .addColumn("Unit", "class='source' width='1%'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
                .setSortPriority(2)
                .setBreakSpans(true);
            if (sortedGenders.size() > 1) {
                caseTablePrinter
                .addColumn("Gender", "class='source' width='1%'", null, "class='source'", true)
                .addColumn("Gender MP + unit", "class='target'", null, "class='source'", true);
            }
            if (sortedCases.size() > 1) {
                caseTablePrinter
                .addColumn("Case", "class='source' width='1%'", null, "class='source'", true)
                ;
                // double width = ((int) ((99.0 / (adjustedPlurals.size()*2 + 1)) * 1000)) / 1000.0;
                // String widthStringTarget = "class='target' width='" + width + "%'";
                String widthStringTarget = "class='target'";

                addTwoColumns(caseTablePrinter, widthStringTarget, adjustedPlurals, pluralRules, true);
            }

            // now get the case and/or gender items

            // also gather info on the "best power units"

            for (String longUnit : unitsToAddGrammar) {
                final String shortUnit = uc.getShortId(longUnit);
                String unitCell = getBestBaseUnit(uc, shortUnit, sizeInBaseUnits);
                String quantity = shortUnit.contentEquals("generic") ? "temperature" : uc.getQuantityFromUnit(shortUnit, false);
                String genderFormatted = "n/a";
                String gender = "n/a";

                if (sortedGenders.size() > 1) {
                    gender = UnitPathType.gender.getTrans(cldrFile, "long", shortUnit, null, null, null, null);
                    if (gender == null) {
                        gender = "n/a";
                    } else {
                        String genderMinimalPair = cldrFile.getStringValue("//ldml/numbers/minimalPairs/genderMinimalPairs[@gender=\"" + gender + "\"]");
                        if (genderMinimalPair != null) {
                            Count bestCount = adjustedPlurals.contains(Count.one) ? Count.one : Count.other;

                            String unitPattern = cldrFile.getStringValueWithBailey("//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"" + longUnit + "\"]/unitPattern"
                                + GrammarInfo.getGrammaticalInfoAttributes(grammarInfo, UnitPathType.unit, bestCount.toString(), null, "nominative"));
                            String unit = unitPattern.replace("\u00A0", "").replace("{0}", "").trim();
                            genderFormatted = MessageFormat.format(genderMinimalPair, unit);
                        }
                    }
                }
                if (sortedCases.size() <= 1) {
                    caseTablePrinter
                    .addRow()
                    .addCell(quantity)
                    .addCell(sizeInBaseUnits.value)
                    .addCell(unitCell)
                    .addCell(gender)
                    .addCell(genderFormatted)
                    ;
                    // finish the row
                    caseTablePrinter.finishRow();
                } else {
                    //Set<String> systems = uc.getSystems(shortUnit);

                    if (unitCell == null || quantity == null || gender == null || sizeInBaseUnits.value == null) {
                        throw new IllegalArgumentException("No best base unit for: " + shortUnit);
                    }

                    for (String case1 : sortedCases) { //
                        // start a row, then add the cells in the row.
                        caseTablePrinter
                        .addRow()
                        .addCell(quantity)
                        .addCell(sizeInBaseUnits.value)
                        .addCell(unitCell);
                        if (sortedGenders.size() > 1) {
                            caseTablePrinter
                            .addCell(gender)
                            .addCell(genderFormatted);
                        }
                        caseTablePrinter
                        .addCell(case1);

                        for (Count plural : adjustedPlurals) {
                            Double sample = getBestSample(pluralRules, plural);

                            // <caseMinimalPairs case="nominative">{0} kostet €3,50.</caseMinimalPairs>

                            String unitPattern = cldrFile.getStringValueWithBailey("//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"" + longUnit + "\"]/unitPattern"
                                + GrammarInfo.getGrammaticalInfoAttributes(grammarInfo, UnitPathType.unit, plural.toString(), null, case1));
                            unitPattern = unitPattern.replace("\u00A0", " ");

                            caseTablePrinter.addCell(unitPattern);

                            String numberPlusUnit = MessageFormat.format(unitPattern, decFormat.format(sample));

                            String caseMinimalPair = cldrFile.getStringValue("//ldml/numbers/minimalPairs/caseMinimalPairs[@case=\"" + case1 + "\"]");
                            String withContext = caseMinimalPair == null ? numberPlusUnit : MessageFormat.format(caseMinimalPair, numberPlusUnit);

                            caseTablePrinter.addCell(withContext);
                        }
                        // finish the row
                        caseTablePrinter.finishRow();
                    }
                }
            }
            info.put("Unit Case and Gender Info", new TablePrinterWithHeader(
                "<p>This table has rows contains unit forms appropriate for different grammatical cases and plural forms. "
                    + "Each plural form has a sample value such as <i>(1.2)</i> or <i>(2)</i>. "
                    + "That value is used with the localized unit pattern to form a formatted measure, such as “2,0 Stunden”. "
                    + "That formatted measure is in turn substituted into a "
                    + "<b><a target='doc-minimal-pairs' href='http://cldr.unicode.org/translation/grammatical-inflection#TOC-Miscellaneous-Minimal-Pairs'>case minimal pair pattern</a> to get the "
                    + FORMATTED_SAMPLE + "</b>. "
                    + "The <b>Gender</b> column is informative; it just supplies the supplied gender for the unit.</p>\n"
                    + "<ul><li>For clarity, conversion values are supplied for non-metric units. "
                    + "For more information, see <a target='unit_conversions' href='../supplemental/unit_conversions.html'>Unit Conversions</a>.</li>"
                    + "</ul>\n"
                    , caseTablePrinter));

            // get best units for gender.
            Multimap<String, BestUnitForGender> bestUnitForGender = TreeMultimap.create();

            for (String longUnit : unitsToAddGrammar) {
                final String shortUnit = uc.getShortId(longUnit);
                String gender = UnitPathType.gender.getTrans(cldrFile, "long", shortUnit, null, null, null, null);
                final BestUnitForGender bestUnit = unitToBestUnit.get(shortUnit);
                if (gender != null && bestUnit != null) {
                    bestUnitForGender.put(gender, bestUnit);
                }
            }

            for (Entry<String, Collection<BestUnitForGender>> entry : bestUnitForGender.asMap().entrySet()) {
                List<String> items = entry.getValue()
                    .stream()
                    .map(x -> x.shortUnit)
                    .collect(Collectors.toList());
                System.out.println(locale + "\t" + entry.getKey() + "\t" + items);
            }

            TablePrinter powerTable = new TablePrinter()
                .addColumn("Unit", "class='source' width='1%'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
                .setSortPriority(2)
                .setRepeatHeader(true)
                .addColumn("Case", "class='source' width='1%'", null, "class='source'", true)
                .addColumn("Gender", "class='source' width='1%'", null, "class='source'", true)
                ;
            double width = ((int) ((99.0 / (adjustedPlurals.size()*2 + 1)) * 1000)) / 1000.0;
            String widthStringTarget = "class='target' width='" + width + "%'";

            addTwoColumns(powerTable, widthStringTarget, adjustedPlurals, pluralRules, false);

            // now get the items
            for (String power : Arrays.asList("power2", "power3")) {
                String unitCell = power;

                for (String gender : sortedGenders) {
                    Collection<BestUnitForGender> bestUnits = bestUnitForGender.get(gender);
                    String bestUnit = null;
                    if (!bestUnits.isEmpty()) {
                        bestUnit = bestUnits.iterator().next().shortUnit;
                    }

                    for (String case1 : sortedCases) { //
                        // start a row, then add the cells in the row.
                        powerTable
                        .addRow()
                        .addCell(unitCell)
                        .addCell(case1)
                        .addCell(gender + (bestUnit == null ? "" : "\n(" + bestUnit + ")"))
                        ;

                        for (Count plural : adjustedPlurals) {
                            String localizedPowerPattern = UnitPathType.power.getTrans(cldrFile, "long", power, plural.toString(), case1, gender, null);
                            localizedPowerPattern = localizedPowerPattern.replace("\u00A0", " ");
                            powerTable.addCell(localizedPowerPattern);

                            if (bestUnit == null) {
                                powerTable.addCell("n/a");
                            } else {
                                Double samplePlural = getBestSample(pluralRules, plural);
                                String localizedUnitPattern = UnitPathType.unit.getTrans(cldrFile, "long", bestUnit, plural.toString(), case1, gender, null);
                                placeholderPosition = UnitConverter.extractUnit(placeholderMatcher, localizedUnitPattern, unitPatternOut);
                                if (placeholderPosition != PlaceholderLocation.middle) {
                                    localizedUnitPattern = unitPatternOut.value;
                                    localizedUnitPattern = localizedUnitPattern.replace("\u00A0", " ");
                                    String placeholderPattern = placeholderPosition == PlaceholderLocation.missing
                                        ? localizedUnitPattern
                                            : placeholderMatcher.group();

                                    String combined;
                                    try {
                                        combined = UnitConverter.combineLowercasing(new ULocale(locale), "long", localizedPowerPattern, localizedUnitPattern);
                                    } catch (Exception e) {
                                        throw new IllegalArgumentException(locale + ") Can't combine "
                                            + "localizedPowerPattern=«" + localizedPowerPattern
                                            + "» with localizedUnitPattern=«"+ localizedUnitPattern + "»"
                                            );
                                    }
                                    String combinedWithPlaceholder = UnitConverter.addPlaceholder(combined, placeholderPattern, placeholderPosition);

                                    String sample = MessageFormat.format(combinedWithPlaceholder, decFormat.format(samplePlural));

                                    String caseMinimalPair = cldrFile.getStringValue("//ldml/numbers/minimalPairs/caseMinimalPairs[@case=\"" + case1 + "\"]");
                                    String withContext = caseMinimalPair == null ? sample : MessageFormat.format(caseMinimalPair, sample);

                                    powerTable.addCell(withContext);
                                } else {
                                    powerTable.addCell("n/a");
                                }
                            }

                        }
                        // finish the row
                        powerTable.finishRow();
                    }
                }
            }
            info.put("Unit Power Components", new TablePrinterWithHeader(
                "<p>This table shows the square (power2) and cubic (power3) patterns, which may vary by case, gender, and plural forms. "
                    + "Each gender is illustrated with a unit where possible, such as <i>(second)</i> or <i>(meter)</i>. "
                    + "Each plural category is illustrated with a unit where possible, such as <i>(1)</i> or <i>(1.2)</i>. "
                    + "The patterns are first supplied, and then combined with the samples and "
                    + "<b><a target='doc-minimal-pairs' href='http://cldr.unicode.org/translation/grammatical-inflection#TOC-Miscellaneous-Minimal-Pairs'>case minimal pair patterns</a></b> "
                    + "in the next <b>" + FORMATTED_SAMPLE + "</b> column."
                    + "</p>", powerTable));


            if (!info.isEmpty()) {
                String name = ENGLISH.getName(locale);
                new Subchart(name + ": Unit Grammar Info", locale, info).writeChart(anchors);
            }
        }
    }

    public void addTwoColumns(TablePrinter caseTablePrinter, String widthStringTarget, Collection<Count> adjustedPlurals, final PluralRules pluralRules, boolean spanRows) {
        for (Count plural : adjustedPlurals) {
            Double sample = getBestSample(pluralRules, plural);
            //final String pluralHeader = plural.toString() + " (" + sample + ")";
            caseTablePrinter.addColumn("Pattern for " + plural.toString(), widthStringTarget, null, "class='target'", true)
            .setSpanRows(spanRows);
            caseTablePrinter.addColumn("Case MP + pattern with " + sample, widthStringTarget, null, "class='target'", true);
        }
    }

    static final Map<String, Pair<String, Double>> BEST_UNIT_CACHE = new HashMap<>();

    public static String getBestBaseUnit(UnitConverter uc, final String shortUnit, Output<Double> sizeInBaseUnits) {
        Pair<String, Double> cached = BEST_UNIT_CACHE.get(shortUnit);
        if (cached != null) {
            sizeInBaseUnits.value = cached.getSecond();
            return cached.getFirst();
        }
        if (shortUnit.equals("square-mile")) {
            int debug = 0;
        }
        String unitCell = ENGLISH.getStringValue("//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"" + uc.getLongId(shortUnit)
        + "\"]/displayName");
        Output<String> baseUnit = new Output<>();
        ConversionInfo info = uc.parseUnitId(shortUnit, baseUnit, false);

        if (info != null) {
            sizeInBaseUnits.value = info.factor.doubleValue();
            Map<Rational, String> factorToUnit = BASE_TO_FACTOR_TO_UNIT.get(baseUnit.value);
            if (factorToUnit == null) {
                int debug = 0;
            }
            String bestUnit = null;
            Rational bestFactor = null;
            Rational inputBoundary = Rational.of(2).multiply(info.factor);
            for (Entry<Rational, String> entry : factorToUnit.entrySet()) {
                final Rational currentFactor = entry.getKey();
                if (bestUnit != null && currentFactor.compareTo(inputBoundary) >= 0) {
                    break;
                }
                bestFactor = currentFactor;
                bestUnit = entry.getValue();
            }
            bestFactor = info.factor.divide(bestFactor); // scale for bestUnit
            if (!bestFactor.equals(Rational.ONE) || !shortUnit.equals(bestUnit)) {
                final String string = bestFactor.toString(FormatStyle.repeating);
                final double bestDoubleFactor = bestFactor.doubleValue();
                String pluralCategory = ENGLISH_PLURAL_RULES.select(bestDoubleFactor);
                final String unitPath = "//ldml/units/unitLength[@type=\"short\"]/unit[@type=\"" + uc.getLongId(bestUnit)
                + "\"]/unitPattern[@count=\"" + pluralCategory
                + "\"]";
                String unitPattern = ENGLISH.getStringValue(unitPath);
                if (unitPattern == null) {
                    final UnitId unitId = uc.createUnitId(bestUnit);
                    unitPattern = unitId.toString(ENGLISH, "long", pluralCategory, null, null, false);
                    if (unitPattern == null) {
                        return null;
                    }
                }
                String unitMeasure = MessageFormat.format(unitPattern, string.contains("/") ? "~" + bestDoubleFactor : string);
                unitCell = shortUnit + "\n( = " + unitMeasure + ")";
            }
        } else {
            sizeInBaseUnits.value = Double.valueOf(-1);
        }
        BEST_UNIT_CACHE.put(shortUnit, Pair.of(unitCell, sizeInBaseUnits.value));
        return unitCell;
    }

    private Double getBestSample(PluralRules pluralRules, Count plural) {
        Collection<Double> samples = pluralRules.getSamples(plural.toString());
        if (samples.isEmpty()) {
            samples = pluralRules.getSamples(plural.toString(), SampleType.DECIMAL);
        }
        int size = samples.size();
        switch (size) {
        case 0:
            throw new IllegalArgumentException("shouldn't happen");
        case 1:
            return samples.iterator().next();
        }
        return Iterables.skip(samples, 1).iterator().next();
    }

    private class Subchart extends Chart {
        final private String title;
        final private String file;
        final private Map<String,TablePrinterWithHeader> tablePrinter;

        @Override
        public boolean getShowDate() {
            return false;
        }

        public Subchart(String title, String file, Map<String, TablePrinterWithHeader> info) {
            super();
            this.title = title;
            this.file = file;
            this.tablePrinter = info;
        }

        @Override
        public String getDirectory() {
            return DIR;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getFileName() {
            return file;
        }

        @Override
        public String getExplanation() {
            return MAIN_HEADER
                + "<p><i>Unit Inflections, Phase 1:</i> The end goal is to add full case and gender support for formatted units. "
                + "During Phase 1, a limited number of locales and units of measurement are being handled in CLDR v38, "
                + "so that we can work kinks out of the process before expanding to all units for all locales.</p>\n"
                + "<p>This chart shows grammatical information available for certain unit and/or power patterns. These patterns are also illustrated with a <b>" + FORMATTED_SAMPLE
                + "</b> that combine the patterns with sample numbers and "
                + "<b><a target='doc-minimal-pairs' href='http://cldr.unicode.org/translation/grammatical-inflection#TOC-Miscellaneous-Minimal-Pairs'>case minimal pair patterns</a></b>. "
                + "For example, “… für {0} …” is a <i>case minimal pair pattern</i> that requires the placeholder {0} to be in the accusative case in German. By inserting into a minimal pair pattern, "
                + "it is easier to ensure that the original unit and/or power patterns are correctly inflected. </p>\n"
                + "<p><b>Notes</b>"
                + "<ul><li>We don't have the cross-product of minimal pairs for both case and plural forms, "
                + "so the <i>case minimal pair pattern</i> might not be correct for the row’s plural category, especially in the nominative.</li>"
                + "<li>Translators often have difficulties with the the minimal pair patterns, "
                + "since they are <i>transcreations</i> not translations. The Hindi minimal pair patterns for case and gender have been discarded because they were incorrectly translated.</li>"
                + "<li>We don't expect translators to supply minimal pair patterns that are natural for any kind of placeholder: "
                + "for example, it is probably not typical to use the vocative with 3.2 meters! So look at the <b>" + FORMATTED_SAMPLE
                + "</b> as an aid for helping to see the context for grammatical inflections, but one that has limitations.</li></ul>"
                ;
        }

        @Override
        public void writeContents(FormattedFileWriter pw) throws IOException {
            try (PrintWriter tsv = FileUtilities.openUTF8Writer(getDirectory() + "tsv/", file + ".tsv");) {
                if (tablePrinter.size() > 1) {
                    pw.write("<h2>Table of Contents</h2>\n");
                    pw.append("<ol>\n");
                    for (String header : tablePrinter.keySet()) {
                        pw.write(writeTOC(header));
                    }
                    pw.append("</ol>\n");
                }
                String sep = "";
                for (Entry<String, TablePrinterWithHeader> entry : tablePrinter.entrySet()) {
                    final String header = entry.getKey();
                    writeHeader(pw, header);
                    final TablePrinterWithHeader explanation = entry.getValue();
                    pw.write(explanation.header);
                    pw.write(explanation.tablePrinter.toTable());
                    tsv.write(sep + "# " + entry.getKey() + "\n");
                    explanation.tablePrinter.toTsv(tsv);
                    sep = "\n";
                }
            }
        }

        private void writeHeader(FormattedFileWriter pw, final String header) throws IOException {
            pw.write("<h2><a name='" + FileUtilities.anchorize(header)+ "'>" + header + "</a></h2>\n");
        }

        private String writeTOC(String header) {
            return "<li><b>"
                + "<a href='#" + FileUtilities.anchorize(header)+ "'>" + header + "</a>"
                + "</b></li>\n";
        }
    }

    public static RuleBasedCollator RBC;
    static {
        Factory cldrFactory = Factory.make(CLDRPaths.COMMON_DIRECTORY + "collation/", ".*");
        CLDRFile root = cldrFactory.make("root", false);
        String rules = root.getStringValue("//ldml/collations/collation[@type=\"emoji\"][@visibility=\"external\"]/cr");

//        if (!rules.contains("'#⃣'")) {
//            rules = rules.replace("#⃣", "'#⃣'").replace("*⃣", "'*⃣'"); //hack for 8288
//        }

        try {
            RBC = new RuleBasedCollator(rules);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failure in rules for " + CLDRPaths.COMMON_DIRECTORY + "collation/" + "root", e);
        }
    }
}
