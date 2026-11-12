package org.unicode.cldr.unittest;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Multimap;
import com.ibm.icu.util.Output;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.unicode.cldr.tool.unitsinfo.Conversion;
import org.unicode.cldr.tool.unitsinfo.GenerateUnitsInfo;
import org.unicode.cldr.tool.unitsinfo.StableIds;
import org.unicode.cldr.tool.unitsinfo.UnitConversions;
import org.unicode.cldr.tool.unitsinfo.UnitHistory;
import org.unicode.cldr.tool.unitsinfo.UnitsInfo;
import org.unicode.cldr.tool.unitsinfo.UnitsInfo.CategoryRow;
import org.unicode.cldr.tool.unitsinfo.UnitsInfo.CombinedRow;
import org.unicode.cldr.tool.unitsinfo.UnitsInfo.Complexity;
import org.unicode.cldr.tool.unitsinfo.UnitsInfo.DisplayName;
import org.unicode.cldr.tool.unitsinfo.UnitsInfo.IntegerType;
import org.unicode.cldr.tool.unitsinfo.UnitsInfo.QuantityRow;
import org.unicode.cldr.tool.unitsinfo.UnitsInfo.Status;
import org.unicode.cldr.tool.unitsinfo.UnitsInfo.UnitRow;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.TestCLDRPaths;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.ConversionInfo;
import org.unicode.cldr.util.UnitPreferences.UnitPreference;

/**
 * Tests units_info.tsv, unit_categories.tsv, unit_quantities.tsv, and unit_combinations.tsv, and
 * GenerateUnitsInfo, which writes them.
 */
public class TestUnitsInfo extends TestFmwkPlus {

    private static final SupplementalDataInfo SDI =
            CLDRConfig.getInstance().getSupplementalDataInfo();
    private static final UnitConverter CONVERTER = SDI.getUnitConverter();
    private static final Path DIR = Path.of(UnitsInfo.PROPERTIES_DIR);
    private static final Splitter MIXED_PARTS = Splitter.on("-and-");

    public static void main(String[] args) {
        new TestUnitsInfo().run(args);
    }

    /** The files are well formed, exact, and match the CLDR data. */
    public void TestFiles() throws IOException {
        checkHeaderAndTabs(
                DIR.resolve(UnitsInfo.UNIT_CATEGORIES_FILE), UnitsInfo.CATEGORIES_HEADER);
        checkHeaderAndTabs(
                DIR.resolve(UnitsInfo.UNIT_QUANTITIES_FILE), UnitsInfo.QUANTITIES_HEADER);
        checkHeaderAndTabs(DIR.resolve(UnitsInfo.UNITS_INFO_FILE), UnitsInfo.UNITS_HEADER);
        checkHeaderAndTabs(
                DIR.resolve(UnitsInfo.UNIT_COMBINATIONS_FILE), UnitsInfo.COMBINATIONS_HEADER);
        UnitsInfo info = UnitsInfo.read(DIR);

        // IDs are 0 to n-1, because they start at 0 and rows are never dropped. unitIds() maps
        // each unit to its ID, so this also fails if two rows share a unit or a unit_id: a unit_id
        // is unique whatever the unit's category or quantity.
        assertEquals("category IDs", idRange(info.categories().size()), sorted(info.categoryIds()));
        assertEquals("quantity IDs", idRange(info.quantities().size()), sorted(info.quantityIds()));
        assertEquals("unit IDs", idRange(info.units().size()), sorted(info.unitIds()));

        // Rows are in ID order, so a new row goes at the end, and two pull requests that add rows
        // at the same time conflict in git instead of taking the same ID.
        assertEquals(
                "categories in ID order",
                idRange(info.categories().size()),
                info.categories().stream().map(CategoryRow::id).toList());
        assertEquals(
                "quantities in ID order",
                idRange(info.quantities().size()),
                info.quantities().stream().map(QuantityRow::id).toList());
        assertEquals(
                "units in ID order",
                idRange(info.units().size()),
                info.units().stream().map(UnitRow::id).toList());

        Map<String, Integer> categoryIds = info.categoryIds();
        Map<String, QuantityRow> quantities = new HashMap<>();
        info.quantities().forEach(q -> quantities.put(Integer.toString(q.id()), q));
        for (UnitRow row : info.units()) {
            String convertible =
                    row.status() == Status.DEPRECATED && !row.replacement().isEmpty()
                            ? row.replacement()
                            : row.unit();
            checkConversion(row.unit(), convertible, row.complexity(), row.conversion());
            checkQuantity(row.unit(), row.conversion(), quantities);
            assertEquals(
                    row.unit() + ": category ID",
                    categoryIds.get(row.category()),
                    (Integer) row.categoryId());
        }

        // A combined unit has no unit ID and isn't in units_info.tsv. Only a combined unit can
        // lack a category.
        Map<String, Integer> unitIds = info.unitIds();
        Set<String> combinedUnits = new TreeSet<>();
        for (CombinedRow row : info.combined()) {
            assertTrue(row.unit() + ": one row", combinedUnits.add(row.unit()));
            assertNull(
                    row.unit() + ": not in " + UnitsInfo.UNITS_INFO_FILE, unitIds.get(row.unit()));
            checkConversion(row.unit(), row.unit(), row.complexity(), row.conversion());
            checkQuantity(row.unit(), row.conversion(), quantities);
            if (row.category().isEmpty()) {
                assertEquals(
                        row.unit() + ": no category ID", CombinedRow.NO_CATEGORY, row.categoryId());
            } else {
                assertEquals(
                        row.unit() + ": category ID",
                        categoryIds.get(row.category()),
                        (Integer) row.categoryId());
            }
        }

        // Each quantity lists the categories of the units in units_info.tsv that measure it, in ID
        // order.
        Map<String, Set<Integer>> categoriesByQuantity = new HashMap<>();
        for (UnitRow row : info.units()) {
            String quantity = row.conversion().quantity();
            if (!quantity.isEmpty()) {
                categoriesByQuantity
                        .computeIfAbsent(quantity, q -> new TreeSet<>())
                        .add(row.categoryId());
            }
        }
        Map<Integer, String> categoryNames = new HashMap<>();
        info.categories().forEach(row -> categoryNames.put(row.id(), row.category()));
        for (QuantityRow row : info.quantities()) {
            Set<Integer> ids = categoriesByQuantity.getOrDefault(row.quantity(), Set.of());
            assertEquals(
                    row.quantity() + ": category_ids", Joiner.on(' ').join(ids), row.categoryIds());
            assertEquals(
                    row.quantity() + ": categories",
                    ids.stream().map(categoryNames::get).collect(Collectors.joining(" ")),
                    row.categories());
        }

        // Regenerating must not change the files. So a change to the CLDR unit data fails here
        // until the files are regenerated, and their diff shows ICU and ICU4X what changed.
        UnitsInfo regenerated = GenerateUnitsInfo.update(info);
        assertSameLines(
                UnitsInfo.UNIT_CATEGORIES_FILE, info.categoryLines(), regenerated.categoryLines());
        assertSameLines(
                UnitsInfo.UNIT_QUANTITIES_FILE, info.quantityLines(), regenerated.quantityLines());
        assertSameLines(UnitsInfo.UNITS_INFO_FILE, info.unitLines(), regenerated.unitLines());
        assertSameLines(
                UnitsInfo.UNIT_COMBINATIONS_FILE,
                info.combinedLines(),
                regenerated.combinedLines());
    }

    /**
     * The units that CLDR data uses without display names of their own are in
     * unit_combinations.tsv: every unit in the unit preferences that isn't in units_info.tsv, and
     * every quantity's base unit that isn't. Their display_name says whether CLDR's patterns can
     * build names for them.
     */
    public void TestCombinedUnits() {
        UnitsInfo info = UnitsInfo.read(DIR);
        Map<String, UnitRow> named = new HashMap<>();
        info.units().forEach(row -> named.put(row.unit(), row));
        Map<String, CombinedRow> combined = new HashMap<>();
        info.combined().forEach(row -> combined.put(row.unit(), row));

        Set<String> preferenceUnits = new TreeSet<>();
        for (Map<String, Multimap<Set<String>, UnitPreference>> byUsage :
                SDI.getUnitPreferences().getData().values()) {
            for (Multimap<Set<String>, UnitPreference> byRegions : byUsage.values()) {
                byRegions.values().forEach(preference -> preferenceUnits.add(preference.unit));
            }
        }
        assertTrue("there are unit preferences", !preferenceUnits.isEmpty());
        for (String unit : preferenceUnits) {
            Status status =
                    named.containsKey(unit)
                            ? named.get(unit).status()
                            : combined.containsKey(unit) ? combined.get(unit).status() : null;
            assertNotNull(unit + ": a unit in the unit preferences is a row", status);
            assertNotEquals(unit + ": active", Status.DEPRECATED, status);
        }
        for (QuantityRow quantity : info.quantities()) {
            String base = quantity.baseUnit();
            assertTrue(
                    quantity.quantity() + ": its base unit " + base + " is a row",
                    named.containsKey(base) || combined.containsKey(base));
        }

        // A mixed unit converts through its parts, so it has no factor of its own.
        CombinedRow footAndInch = combined.get("foot-and-inch");
        if (assertNotNull("foot-and-inch is a combined unit", footAndInch)) {
            assertEquals("foot-and-inch complexity", Complexity.MIXED, footAndInch.complexity());
            assertEquals(
                    "foot-and-inch display_name", DisplayName.COMPOSED, footAndInch.displayName());
            assertEquals("foot-and-inch category", "length", footAndInch.category());
            assertEquals("foot-and-inch base unit", "meter", footAndInch.conversion().baseUnit());
            assertEquals("foot-and-inch factor", "", footAndInch.conversion().factorNum());
            assertEquals(
                    "foot-and-inch systems",
                    "ussystem uksystem",
                    footAndInch.conversion().systems());
        }
        CombinedRow millimeterPerHour = combined.get("millimeter-per-hour");
        if (assertNotNull("millimeter-per-hour is a combined unit", millimeterPerHour)) {
            assertEquals(
                    "millimeter-per-hour display_name",
                    DisplayName.COMPOSED,
                    millimeterPerHour.displayName());
            assertEquals("millimeter-per-hour category", "speed", millimeterPerHour.category());
        }
        // CLDR has display name patterns for square and cubic, not for pow4.
        CombinedRow capacitanceBase =
                combined.get("pow4-second-square-ampere-per-kilogram-square-meter");
        if (assertNotNull(
                "the base unit of electric-capacitance is a combined unit", capacitanceBase)) {
            assertEquals(
                    "electric-capacitance base unit display_name",
                    DisplayName.NONE,
                    capacitanceBase.displayName());
            assertEquals("its category", "electric", capacitanceBase.category());
        }
        // No unit with names measures mass-density, so its base unit has no category.
        CombinedRow massDensityBase = combined.get("kilogram-per-cubic-meter");
        if (assertNotNull("kilogram-per-cubic-meter is a combined unit", massDensityBase)) {
            assertEquals("kilogram-per-cubic-meter category", "", massDensityBase.category());
            assertEquals(
                    "kilogram-per-cubic-meter quantity",
                    "mass-density",
                    massDensityBase.conversion().quantity());
        }
        assertNull("meter has names, so it isn't a combined unit", combined.get("meter"));
    }

    /**
     * A quantity's categories are the ones of the units that measure it: one for most quantities,
     * two for energy (torque units have joule's dimension), and none for mass-density.
     */
    public void TestQuantityCategories() {
        Map<String, QuantityRow> quantities = new HashMap<>();
        UnitsInfo.read(DIR).quantities().forEach(row -> quantities.put(row.quantity(), row));
        assertEquals("length", "length", quantities.get("length").categories());
        assertEquals("energy", "energy torque", quantities.get("energy").categories());
        assertEquals("power", "light power", quantities.get("power").categories());
        assertEquals("mass-density", "", quantities.get("mass-density").categories());
        assertEquals("mass-density IDs", "", quantities.get("mass-density").categoryIds());
    }

    /**
     * Rebuilding the history from the release tags keeps the rows that no release has, such as a
     * unit that was checked in and removed again before a release: its ID is permanent.
     */
    public void TestRebuildKeepsCheckedInRows() {
        UnitsInfo info = UnitsInfo.read(DIR);
        Map<String, UnitRow> rows = new HashMap<>();
        Map<String, String> active = new TreeMap<>();
        for (UnitRow row : info.units()) {
            rows.put(row.unit(), row);
            if (row.status() != Status.DEPRECATED) {
                active.put(row.unit(), row.category());
            }
        }
        // A rebuilt history without one unit, as if no release tag had it.
        String missing = active.keySet().iterator().next();
        active.remove(missing);
        UnitHistory rebuilt = new UnitHistory();
        rebuilt.add("24", active);
        rebuilt.addMissing(UnitHistory.of(info));

        UnitHistory.UnitEntry kept = rebuilt.units().get(missing);
        if (assertNotNull(missing + " is kept", kept)) {
            assertEquals(missing + " since", rows.get(missing).since(), kept.since());
        }
        Map<String, Integer> unitIds = StableIds.assign(info.unitIds(), rebuilt.unitOrder());
        StableIds.verify("unit", info.unitIds(), unitIds);
        assertEquals("no new IDs", info.unitIds().size(), unitIds.size());
    }

    /** Over two simulated releases, deprecated entries keep their IDs and new ones get the next. */
    public void TestFutureReleases() {
        UnitsInfo info = UnitsInfo.read(DIR);
        Map<String, Integer> categoryIds = info.categoryIds();
        Map<String, Integer> unitIds = info.unitIds();
        int maxCategoryId = Collections.max(categoryIds.values());
        int maxUnitId = Collections.max(unitIds.values());
        UnitHistory history = UnitHistory.of(info);

        // Release 50 deprecates a unit and a whole category, and adds a category with two units.
        Map<String, String> release50 = new TreeMap<>();
        history.units()
                .forEach(
                        (unit, entry) -> {
                            if (entry.deprecatedIn().isEmpty()) {
                                release50.put(unit, entry.category());
                            }
                        });
        String droppedUnit = release50.keySet().iterator().next();
        String droppedCategory =
                release50.values().stream()
                        .filter(category -> !category.equals(release50.get(droppedUnit)))
                        .findFirst()
                        .orElseThrow();
        release50.remove(droppedUnit);
        release50.values().removeIf(droppedCategory::equals);
        release50.put("test-unit-a", "test-category");
        release50.put("test-unit-b", "test-category");
        history.add("50", release50);

        // It also drops one combined unit and adds one.
        Set<String> combined50 = new TreeSet<>();
        history.combined()
                .forEach(
                        (unit, entry) -> {
                            if (entry.deprecatedIn().isEmpty()) {
                                combined50.add(unit);
                            }
                        });
        String droppedCombined = combined50.iterator().next();
        combined50.remove(droppedCombined);
        combined50.add("test-unit-a-per-test-unit-b");
        history.addCombined("50", combined50);

        Map<String, Integer> categoryIds50 = StableIds.assign(categoryIds, history.categoryOrder());
        Map<String, Integer> unitIds50 = StableIds.assign(unitIds, history.unitOrder());
        StableIds.verify("category", categoryIds, categoryIds50);
        StableIds.verify("unit", unitIds, unitIds50);
        assertEquals(
                "new category ID", maxCategoryId + 1, (int) categoryIds50.get("test-category"));
        assertEquals("first new unit ID", maxUnitId + 1, (int) unitIds50.get("test-unit-a"));
        assertEquals("second new unit ID", maxUnitId + 2, (int) unitIds50.get("test-unit-b"));
        assertEquals(
                droppedUnit + " deprecated in",
                "50",
                history.units().get(droppedUnit).deprecatedIn());
        assertEquals(
                droppedCategory + " deprecated in",
                "50",
                history.categories().get(droppedCategory).deprecatedIn());
        assertEquals(
                "unit added in 50, status in 50",
                Status.NEW,
                history.units().get("test-unit-a").status("50"));
        assertEquals(
                droppedCombined + " deprecated in",
                "50",
                history.combined().get(droppedCombined).deprecatedIn());
        assertEquals(
                "combined unit added in 50, status in 50",
                Status.NEW,
                history.combined().get("test-unit-a-per-test-unit-b").status("50"));

        // Release 51 adds one more unit. Release 50's IDs and history stay.
        Map<String, String> release51 = new TreeMap<>(release50);
        release51.put("test-unit-c", "test-category");
        history.add("51", release51);
        Map<String, Integer> unitIds51 = StableIds.assign(unitIds50, history.unitOrder());
        StableIds.verify("unit", unitIds50, unitIds51);
        assertEquals("unit added in 51", maxUnitId + 3, (int) unitIds51.get("test-unit-c"));
        assertEquals(
                droppedUnit + " deprecated in, after 51",
                "50",
                history.units().get(droppedUnit).deprecatedIn());
        assertEquals(
                "unit added in 50, status in 51",
                Status.REGULAR,
                history.units().get("test-unit-a").status("51"));
    }

    /** No ID in the last release's files has changed. */
    public void TestIdsMatchLastRelease() {
        if (!TestCLDRPaths.canUseArchiveDirectory()) {
            return;
        }
        Path released = Path.of(CLDRPaths.LAST_RELEASE_DIRECTORY, "common", "properties");
        if (!Files.exists(released.resolve(UnitsInfo.UNITS_INFO_FILE))) {
            logln("Skipping: no " + UnitsInfo.UNITS_INFO_FILE + " in " + released);
            return;
        }
        try {
            StableIds.verify(UnitsInfo.read(released), UnitsInfo.read(DIR));
        } catch (IllegalStateException e) {
            errln(e.getMessage());
        }
    }

    /**
     * Every number fits its column's integer size in UnitsInfo.INTEGER_BITS, in the files and after
     * regenerating them. If not, this fails with a big warning, because ICU and ICU4X would have to
     * change before the number can ship. It also warns which factors need 64-bit integers.
     */
    public void TestIntegerSizes() {
        UnitsInfo info = UnitsInfo.read(DIR);
        Set<String> problems = new LinkedHashSet<>(info.integerProblems());
        // Regenerating catches new CLDR data even before the files are regenerated.
        problems.addAll(GenerateUnitsInfo.update(info).integerProblems());
        if (!problems.isEmpty()) {
            errln(
                    "*** TOO BIG FOR ICU AND ICU4X *** These numbers don't fit the integer sizes"
                            + " in UnitsInfo.INTEGER_BITS. Change ICU and ICU4X first, then raise"
                            + " the size: "
                            + String.join("; ", problems));
        }

        // Not an error, but keep it visible: these factors don't fit in 32 bits.
        Map<String, Integer> wide = new LinkedHashMap<>();
        for (UnitRow row : info.units()) {
            addIfWide(wide, row.unit(), row.conversion());
        }
        for (CombinedRow row : info.combined()) {
            addIfWide(wide, row.unit(), row.conversion());
        }
        if (!wide.isEmpty()) {
            warnln(
                    "*** 64-BIT FACTORS *** "
                            + wide.size()
                            + " units have a factor_num or factor_den that doesn't fit in a"
                            + " signed 32-bit integer, so ICU and ICU4X must store these columns as"
                            + " 64-bit integers: "
                            + wide.entrySet().stream()
                                    .sorted(
                                            Map.Entry.<String, Integer>comparingByValue()
                                                    .reversed())
                                    .map(e -> e.getKey() + " (" + e.getValue() + " bits)")
                                    .collect(Collectors.joining(", ")));
        }
    }

    private static void addIfWide(Map<String, Integer> wide, String unit, Conversion c) {
        if (!c.factorNum().isEmpty()) {
            int bits =
                    Math.max(
                            new BigInteger(c.factorNum()).bitLength(),
                            new BigInteger(c.factorDen()).bitLength());
            if (bits >= Integer.SIZE) {
                wide.put(unit, bits);
            }
        }
    }

    /** The size check catches numbers that are one bit too big for their column. */
    public void TestIntegerSizeCheck() {
        String twoTo63 = BigInteger.TWO.pow(63).toString();
        Conversion conversion =
                new Conversion(
                        false,
                        "",
                        "length",
                        "meter",
                        twoTo63,
                        "1",
                        "0",
                        "",
                        "",
                        "exact",
                        "",
                        "si metric prefixable");
        UnitRow row =
                new UnitRow(
                        32768,
                        0,
                        "length",
                        "test-unit",
                        Complexity.SIMPLE,
                        "",
                        conversion,
                        Status.REGULAR,
                        "24",
                        "",
                        "",
                        "",
                        "");
        assertEquals(
                "problems",
                List.of(
                        "test-unit: unit_id = 32768 doesn't fit in a signed 16-bit integer (it has"
                                + " 16 bits)",
                        "test-unit: factor_num = "
                                + twoTo63
                                + " doesn't fit in a signed 64-bit integer (it has 64 bits)"),
                new UnitsInfo(List.of(), List.of(), List.of(row), List.of()).integerProblems());
    }

    /**
     * factor_num_type, factor_den_type, and constant_denominator_type name the smallest signed
     * integer type that holds each number, in both files that have them.
     */
    public void TestIntegerTypes() throws IOException {
        assertEquals("127", IntegerType.INT8_T, IntegerType.of(BigInteger.valueOf(127)));
        assertEquals("-128", IntegerType.INT8_T, IntegerType.of(BigInteger.valueOf(-128)));
        assertEquals("128", IntegerType.INT16_T, IntegerType.of(BigInteger.valueOf(128)));
        assertEquals("32767", IntegerType.INT16_T, IntegerType.of(BigInteger.valueOf(32767)));
        assertEquals("32768", IntegerType.INT32_T, IntegerType.of(BigInteger.valueOf(32768)));
        assertEquals(
                "2^31 - 1",
                IntegerType.INT32_T,
                IntegerType.of(BigInteger.valueOf(Integer.MAX_VALUE)));
        assertEquals("2^31", IntegerType.INT64_T, IntegerType.of(BigInteger.valueOf(1L << 31)));
        assertEquals(
                "2^63 - 1",
                IntegerType.INT64_T,
                IntegerType.of(BigInteger.valueOf(Long.MAX_VALUE)));
        assertEquals("2^63", IntegerType.NONE_FIT_IN_INT, IntegerType.of(BigInteger.TWO.pow(63)));
        assertEquals("cell", "none_fit_in_int", IntegerType.NONE_FIT_IN_INT.toString());

        for (String file : List.of(UnitsInfo.UNITS_INFO_FILE, UnitsInfo.UNIT_COMBINATIONS_FILE)) {
            for (Map<String, String> row : rowsByColumn(DIR.resolve(file))) {
                for (String column : List.of("factor_num", "factor_den", "constant_denominator")) {
                    String number = row.get(column);
                    assertEquals(
                            file + ", " + row.get("unit") + ": " + column + "_type",
                            number.isEmpty()
                                    ? ""
                                    : IntegerType.of(new BigInteger(number)).toString(),
                            row.get(column + "_type"));
                }
            }
        }
    }

    /** The data rows of a TSV file as column name → cell, using the file's own header. */
    private static List<Map<String, String>> rowsByColumn(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);
        Splitter tab = Splitter.on('\t');
        List<String> header = tab.splitToList(lines.get(0).substring("# ".length()));
        List<Map<String, String>> rows = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            List<String> cells = tab.splitToList(line);
            Map<String, String> row = new HashMap<>();
            for (int i = 0; i < header.size(); i++) {
                row.put(header.get(i), cells.get(i));
            }
            rows.add(row);
        }
        return rows;
    }

    /**
     * constant_denominator is the constant in a unit ID as an integer (100 in
     * liter-per-100-kilometer), only core units have one, and the factor already includes it.
     */
    public void TestConstantDenominators() {
        Map<String, UnitRow> units = new HashMap<>();
        UnitsInfo info = UnitsInfo.read(DIR);
        info.units().forEach(row -> units.put(row.unit(), row));
        assertEquals("liter-per-100-kilometer", "100", constant(units, "liter-per-100-kilometer"));
        assertEquals(
                "kilowatt-hour-per-100-kilometer",
                "100",
                constant(units, "kilowatt-hour-per-100-kilometer"));
        assertEquals("part-per-1e6", "1000000", constant(units, "part-per-1e6"));
        assertEquals("part-per-1e9", "1000000000", constant(units, "part-per-1e9"));
        assertEquals("meter", "", constant(units, "meter"));
        assertEquals("liter-per-kilometer", "", constant(units, "liter-per-kilometer"));

        for (UnitRow row : info.units()) {
            assertTrue(
                    row.unit() + ": only a core unit has a constant",
                    row.constantDenominator().isEmpty() || row.complexity() == Complexity.CORE);
        }
        for (CombinedRow row : info.combined()) {
            assertTrue(
                    row.unit() + ": only a core unit has a constant",
                    row.constantDenominator().isEmpty() || row.complexity() == Complexity.CORE);
        }

        // liter-per-100-kilometer converts like liter-per-kilometer divided by 100.
        assertEquals(
                "liter-per-100-kilometer factor",
                factor(units.get("liter-per-kilometer").conversion()).divide(Rational.of(100)),
                factor(units.get("liter-per-100-kilometer").conversion()));
    }

    private static String constant(Map<String, UnitRow> units, String unit) {
        UnitRow row = units.get(unit);
        return row == null ? null : row.constantDenominator();
    }

    private static Rational factor(Conversion c) {
        return Rational.of(new BigInteger(c.factorNum()), new BigInteger(c.factorDen()))
                .multiply(Rational.pow10(Integer.parseInt(c.factorExp())));
    }

    /**
     * Each unit has the right UTS #35 complexity level: simple (meter, pound, pound-force,
     * generic), prefixed (kilogram, centimeter), single (square-meter, square-centimeter), core
     * (meter-per-second, part-per-1e6), or mixed (foot-and-inch).
     */
    public void TestComplexity() {
        Map<String, Complexity> byUnit = new HashMap<>();
        UnitsInfo info = UnitsInfo.read(DIR);
        info.units().forEach(row -> byUnit.put(row.unit(), row.complexity()));
        info.combined().forEach(row -> byUnit.put(row.unit(), row.complexity()));
        assertEquals("meter", Complexity.SIMPLE, byUnit.get("meter"));
        assertEquals("pound", Complexity.SIMPLE, byUnit.get("pound"));
        assertEquals("gram", Complexity.SIMPLE, byUnit.get("gram"));
        assertEquals("pound-force", Complexity.SIMPLE, byUnit.get("pound-force"));
        assertEquals("kilogram-force", Complexity.SIMPLE, byUnit.get("kilogram-force"));
        assertEquals("generic", Complexity.SIMPLE, byUnit.get("generic"));
        assertEquals("kilogram", Complexity.PREFIXED, byUnit.get("kilogram"));
        assertEquals("centimeter", Complexity.PREFIXED, byUnit.get("centimeter"));
        assertEquals("square-meter", Complexity.SINGLE, byUnit.get("square-meter"));
        assertEquals("square-centimeter", Complexity.SINGLE, byUnit.get("square-centimeter"));
        assertEquals("meter-per-second", Complexity.CORE, byUnit.get("meter-per-second"));
        assertEquals("kilowatt-hour", Complexity.CORE, byUnit.get("kilowatt-hour"));
        assertEquals("millimeter-ofhg", Complexity.CORE, byUnit.get("millimeter-ofhg"));
        assertEquals("part-per-1e6", Complexity.CORE, byUnit.get("part-per-1e6"));
        assertEquals("foot-and-inch", Complexity.MIXED, byUnit.get("foot-and-inch"));
        assertEquals(
                "foot-and-inch",
                Complexity.MIXED,
                UnitConversions.complexityOf("foot-and-inch", "foot-and-inch"));
    }

    /**
     * A unit's reciprocal quantity is in the same category, its base unit is 1 / the unit's base
     * unit, and the units of that quantity point back.
     */
    public void TestReciprocals() {
        UnitsInfo info = UnitsInfo.read(DIR);
        Map<String, List<String>> reciprocalsByUnit = new TreeMap<>();
        Map<List<String>, String> baseUnits = new HashMap<>();
        Map<List<String>, String> reciprocals = new HashMap<>();
        for (UnitRow row : info.units()) {
            addReciprocalRow(
                    row.unit(),
                    row.category(),
                    row.conversion(),
                    row.reciprocalQuantity(),
                    row.reciprocalBaseUnit(),
                    reciprocalsByUnit,
                    baseUnits,
                    reciprocals);
        }
        for (CombinedRow row : info.combined()) {
            addReciprocalRow(
                    row.unit(),
                    row.category(),
                    row.conversion(),
                    row.reciprocalQuantity(),
                    row.reciprocalBaseUnit(),
                    reciprocalsByUnit,
                    baseUnits,
                    reciprocals);
        }
        reciprocalsByUnit.forEach(
                (unit, cells) -> {
                    String category = cells.get(0);
                    String quantity = cells.get(1);
                    String baseUnit = cells.get(2);
                    String reciprocalQuantity = cells.get(3);
                    String reciprocalBaseUnit = cells.get(4);
                    if (reciprocalQuantity.isEmpty()) {
                        assertEquals(unit + ": no reciprocal base unit", "", reciprocalBaseUnit);
                        return;
                    }
                    List<String> other = List.of(category, reciprocalQuantity);
                    assertEquals(
                            unit
                                    + ": the reciprocal quantity is in the same category, with that"
                                    + " base unit",
                            reciprocalBaseUnit,
                            baseUnits.get(other));
                    assertEquals(
                            unit + ": the reciprocal quantity points back",
                            quantity,
                            reciprocals.get(other));
                    assertEquals(
                            unit
                                    + ": CLDR converts 2 of the base unit to 1/2 of the reciprocal"
                                    + " base unit",
                            Rational.of(1, 2),
                            CONVERTER.convert(Rational.of(2), baseUnit, reciprocalBaseUnit, false));
                });
    }

    private static void addReciprocalRow(
            String unit,
            String category,
            Conversion c,
            String reciprocalQuantity,
            String reciprocalBaseUnit,
            Map<String, List<String>> reciprocalsByUnit,
            Map<List<String>, String> baseUnits,
            Map<List<String>, String> reciprocals) {
        List<String> key = List.of(category, c.quantity());
        baseUnits.put(key, c.baseUnit());
        reciprocals.put(key, reciprocalQuantity);
        reciprocalsByUnit.put(
                unit,
                List.of(
                        category,
                        c.quantity(),
                        c.baseUnit(),
                        reciprocalQuantity,
                        reciprocalBaseUnit));
    }

    /** A unit's quantity_id points to the row of its quantity, with the same base unit. */
    private void checkQuantity(String unit, Conversion c, Map<String, QuantityRow> quantities) {
        QuantityRow quantity = quantities.get(c.quantityId());
        assertEquals(
                unit + ": quantity of quantity_id " + c.quantityId(),
                c.quantity(),
                quantity == null ? "" : quantity.quantity());
        assertEquals(
                unit + ": base unit of quantity_id " + c.quantityId(),
                c.baseUnit(),
                quantity == null ? "" : quantity.baseUnit());
    }

    /**
     * The factor and offset are reduced integers that fit their types and give CLDR's values. A
     * deprecated ID converts like its {@code convertible} replacement.
     */
    private void checkConversion(
            String unit, String convertible, Complexity complexity, Conversion c) {
        if (c.formatOnly()) {
            assertEquals(unit + ": a format-only unit has no conversion cells", Conversion.NONE, c);
            assertNull(
                    unit + ": CLDR has no conversion for a format-only unit",
                    CONVERTER.parseUnitId(unit, new Output<>(), false));
            return;
        }
        assertTrue(
                unit + ": a convertible unit has a quantity and a base unit",
                !c.quantity().isEmpty() && !c.baseUnit().isEmpty());
        if (complexity == Complexity.MIXED) {
            // A mixed unit converts through its parts, which share its quantity.
            List<String> parts = MIXED_PARTS.splitToList(convertible);
            assertEquals(
                    unit + ": systems",
                    Joiner.on(' ').join(CONVERTER.getSystemsEnum(String.join("-", parts))),
                    c.systems());
            assertTrue(
                    unit + ": a mixed unit has no factor, offset, or special conversion",
                    c.factorNum().isEmpty()
                            && c.offsetNum().isEmpty()
                            && c.exactness().isEmpty()
                            && c.special().isEmpty());
            for (String part : parts) {
                assertEquals(
                        unit + ": quantity of " + part,
                        c.quantity(),
                        CONVERTER.getQuantityFromUnit(part, false));
            }
            return;
        }
        assertEquals(
                unit + ": systems",
                Joiner.on(' ').join(CONVERTER.getSystemsEnum(convertible)),
                c.systems());
        if (c.factorNum().isEmpty()) {
            assertTrue(unit + ": a unit without a factor is special", !c.special().isEmpty());
            assertTrue(unit + ": no offset without a factor", c.offsetNum().isEmpty());
            return;
        }
        BigInteger num = new BigInteger(c.factorNum());
        BigInteger den = new BigInteger(c.factorDen());
        assertTrue(
                unit + ": factor_num and factor_den are positive",
                num.signum() > 0 && den.signum() > 0);
        assertEquals(unit + ": factor is reduced", BigInteger.ONE, num.gcd(den));
        assertTrue(
                unit + ": factor_num and factor_den have no trailing zeros",
                num.mod(BigInteger.TEN).signum() != 0 && den.mod(BigInteger.TEN).signum() != 0);
        Rational factor =
                Rational.of(num, den).multiply(Rational.pow10(Integer.parseInt(c.factorExp())));

        Rational offset = Rational.ZERO;
        if (!c.offsetNum().isEmpty()) {
            offset = Rational.of(new BigInteger(c.offsetNum()), new BigInteger(c.offsetDen()));
        }

        ConversionInfo info = CONVERTER.parseUnitId(convertible, new Output<>(), false);
        assertEquals(unit + ": factor", info.factor, factor);
        assertEquals(unit + ": offset", info.offset, offset);
    }

    /** The file starts with {@code header}, and each line has as many tabs, for GitHub's table. */
    private void checkHeaderAndTabs(Path file, String header) throws IOException {
        List<String> lines = Files.readAllLines(file);
        assertEquals(file.getFileName() + ": header", header, lines.get(0));
        for (int i = 1; i < lines.size(); i++) {
            assertEquals(
                    file.getFileName() + ": tabs in line " + (i + 1),
                    tabs(header),
                    tabs(lines.get(i)));
        }
    }

    private void assertSameLines(String file, List<String> expected, List<String> actual) {
        assertEquals(file + ": rows", expected.size(), actual.size());
        for (int i = 0; i < Math.min(expected.size(), actual.size()); i++) {
            assertEquals(
                    file + ": row " + (i + 1) + " (regenerate with GenerateUnitsInfo)",
                    expected.get(i),
                    actual.get(i));
        }
    }

    private static long tabs(String line) {
        return line.chars().filter(ch -> ch == '\t').count();
    }

    private static List<Integer> idRange(int size) {
        return IntStream.range(0, size).boxed().toList();
    }

    private static List<Integer> sorted(Map<String, Integer> ids) {
        return ids.values().stream().sorted().toList();
    }
}
