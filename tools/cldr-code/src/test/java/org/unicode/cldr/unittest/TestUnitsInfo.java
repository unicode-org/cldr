package org.unicode.cldr.unittest;

import com.ibm.icu.util.Output;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;
import org.unicode.cldr.tool.unitsinfo.Conversion;
import org.unicode.cldr.tool.unitsinfo.GenerateUnitsInfo;
import org.unicode.cldr.tool.unitsinfo.StableIds;
import org.unicode.cldr.tool.unitsinfo.UnitHistory;
import org.unicode.cldr.tool.unitsinfo.UnitsInfo;
import org.unicode.cldr.tool.unitsinfo.UnitsInfo.Status;
import org.unicode.cldr.tool.unitsinfo.UnitsInfo.UnitRow;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.TestCLDRPaths;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.ConversionInfo;

/** Tests units_info.tsv and unit_categories.tsv, and GenerateUnitsInfo, which writes them. */
public class TestUnitsInfo extends TestFmwkPlus {

    private static final UnitConverter CONVERTER =
            CLDRConfig.getInstance().getSupplementalDataInfo().getUnitConverter();
    private static final Path DIR = Path.of(UnitsInfo.PROPERTIES_DIR);

    public static void main(String[] args) {
        new TestUnitsInfo().run(args);
    }

    /** The files are well formed, exact, and match the CLDR data. */
    public void TestFiles() throws IOException {
        checkHeaderAndTabs(
                DIR.resolve(UnitsInfo.UNIT_CATEGORIES_FILE), UnitsInfo.CATEGORIES_HEADER);
        checkHeaderAndTabs(DIR.resolve(UnitsInfo.UNITS_INFO_FILE), UnitsInfo.UNITS_HEADER);
        UnitsInfo info = UnitsInfo.read(DIR);

        // IDs are 0 to n-1, because they start at 0 and rows are never dropped.
        assertEquals("category IDs", idRange(info.categories().size()), sorted(info.categoryIds()));
        assertEquals("unit IDs", idRange(info.units().size()), sorted(info.unitIds()));

        for (UnitRow row : info.units()) {
            checkConversion(row);
        }

        // Regenerating must not change the files. So a change to the CLDR unit data fails here
        // until the files are regenerated, and their diff shows ICU and ICU4X what changed.
        UnitsInfo regenerated = GenerateUnitsInfo.update(info);
        assertSameLines(
                UnitsInfo.UNIT_CATEGORIES_FILE, info.categoryLines(), regenerated.categoryLines());
        assertSameLines(UnitsInfo.UNITS_INFO_FILE, info.unitLines(), regenerated.unitLines());
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

    /** The factor and offset are reduced integers that fit their types and give CLDR's values. */
    private void checkConversion(UnitRow row) {
        String unit = row.unit();
        Conversion c = row.conversion();
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
        if (c.factorNum().isEmpty()) {
            assertTrue(unit + ": a unit without a factor is special", !c.special().isEmpty());
            assertTrue(unit + ": no offset without a factor", c.offsetNum().isEmpty());
            return;
        }
        assertTrue(
                unit + ": factor_num and factor_den fit in signed 64-bit integers",
                fits(c.factorNum(), Long.SIZE) && fits(c.factorDen(), Long.SIZE));
        assertTrue(
                unit + ": factor_exp fits in a signed 32-bit integer",
                fits(c.factorExp(), Integer.SIZE));
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
            assertTrue(
                    unit + ": offset_num and offset_den fit in signed 32-bit integers",
                    fits(c.offsetNum(), Integer.SIZE) && fits(c.offsetDen(), Integer.SIZE));
            offset = Rational.of(new BigInteger(c.offsetNum()), new BigInteger(c.offsetDen()));
        }

        // A deprecated ID converts like its replacement.
        String convertible =
                row.status() == Status.DEPRECATED && !row.replacement().isEmpty()
                        ? row.replacement()
                        : unit;
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

    private static boolean fits(String number, int bits) {
        return new BigInteger(number).bitLength() < bits;
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
