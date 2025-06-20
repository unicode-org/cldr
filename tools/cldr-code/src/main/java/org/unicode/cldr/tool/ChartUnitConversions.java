package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R4;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.EnumSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.Rational.FormatStyle;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.TargetInfo;
import org.unicode.cldr.util.UnitConverter.UnitId;
import org.unicode.cldr.util.UnitConverter.UnitSystem;

public class ChartUnitConversions extends Chart {
    private static final boolean DUMP_UNIT_TABLE_TO_STDOUT = false;

    public static void main(String[] args) {
        new ChartUnitConversions().writeChart(null);
    }

    @Override
    public String getDirectory() {
        return FormattedFileWriter.CHART_TARGET_DIR;
    }

    @Override
    public String getTitle() {
        return "Unit Conversions";
    }

    @Override
    public String getExplanation() {
        return "<p>Unit Conversions provide conversions for units, such as meter ⟹ foot, "
                + "so that a source units can be converted into what is needed for localized "
                + "<a href='unit_preferences.html' target='unit_preferences'>Unit Preferences</a>. "
                + "There are many possible units, and additional units and conversions will be added in future releases. "
                + "The unit identifiers are internal, and are to be localized for display to users. "
                + "See <a href='https://www.unicode.org/cldr/charts/latest/by_type/units.area.html#hectare' target='units.area.hectare'>Hectare</a>, for example. "
                + "</p>"
                + "<details><summary><b>Column Key</b></summary><ul>"
                + "<li><b>Quantity.</b> "
                + "The units are grouped and ordered by Quantity (which are based on SI quantities according to "
                + "<a href='https://www.nist.gov/pml/special-publication-811' target='nist811'>NIST 811</a>). "
                + "There are some additions, such as year-duration."
                + "</li>"
                + "<li>"
                + "<b>Base Unit.</b> The Base Unit is based on an SI base unit or derived unit (with some additions). "
                + "</li>"
                + "<li><b>Systems.</b> These indicate which systems the units are used in. </li>"
                + "<li><b>Source Unit.</b> "
                + "Each Source Unit is converted to the Base Unit by multiplying it by the Factor and adding the Offset (if any). "
                + "The inverse conversion is done by subtracting the Offset, and dividing by the Factor. "
                + "Conversion between units is done by converting the first unit to the Base Units, then using the inverse conversion to get the second unit. "
                + "</li>"
                + "<li><b>Approx. Factor.</b> "
                + "This is an approximation to the exact factor, limited to <a href='https://en.wikipedia.org/wiki/Double-precision_floating-point_format'>double precision</a>,"
                + "which has at most 17 decimal digits of accuracy. "
                + "Some values are given as simple rational numbers. "
                + "Some units (such as beaufort) require more complex conversions than factor & offset. "
                + "The values that are not equivalent to the Exact Factor are marked with ~, such as ~9.4607×10ˆ15. "
                + "</li>"
                + "<li>"
                + "<b>Exact* Factor.</b> Each numeric value is an exact rational, "
                + "with a few exceptions, such as radians because the value of π is irrational — a rational approximation is used.) "
                + "The format is a terminating decimal where possible; "
                + "otherwise a repeating decimal if possible (where ˙ marks the start of the <a href='https://en.wikipedia.org/wiki/Repeating_decimal' target='wiki'>reptend</a>); "
                + "otherwise a <a href='https://en.wikipedia.org/wiki/Rational_number' target='wiki'>rational number</a> of the form <i>numerator/denominator</i>. "
                + "The numerator may be a precise decimal (24.01/1331 rather than 2401/133100, and the denominator is skipped if 1."
                + ""
                + "</li>"
                + "<li><b>Offset.</b> This is rarely needed. The format is the same as Exact* Factor. </li>"
                + "</ul>"
                + "<p>"
                + "The "
                + ldmlSpecLink("/tr35-general.html#Contents")
                + " should be consulted for more details, such as how to handle complex units (such as foot-per-minute) by converting the elements, or what the Systems mean. "
                + "</p>"
                + "</details>"
                + dataScrapeMessage(
                        "/tr35-general.html#Contents",
                        "common/testData/units/unitsTest.txt",
                        "common/supplemental/units.xml");
    }

    @Override
    public void writeContents(FormattedFileWriter pw) throws IOException {
        //  <convertUnit source='ounce' baseUnit='kilogram' factor='lb_to_kg/16' systems="ussystem
        // uksystem"/>
        //  <convertUnit source='fahrenheit' baseUnit='kelvin' factor='5/9' offset='2298.35/9'
        // systems="ussystem uksystem"/>

        TablePrinter tablePrinter =
                new TablePrinter()
                        .addColumn("SortKey", "class='source'", null, "class='source'", true)
                        .setHidden(true)
                        .setSortPriority(0)
                        .addColumn(
                                "Quantity",
                                "class='source'",
                                CldrUtility.getDoubleLinkMsg(),
                                "class='source'",
                                true)
                        .setRepeatHeader(true)
                        .setBreakSpans(true)
                        .addColumn("Base Unit", "class='source'", null, "class='source'", true)
                        .addColumn("Systems", "class='source'", null, "class='source'", true)
                        .addColumn(
                                "Source Unit",
                                "class='source'",
                                CldrUtility.getDoubleLinkMsg(),
                                "class='source'",
                                true)
                        .addColumn("Approx. Factor", "class='target'", null, "class='target'", true)
                        .setCellAttributes("class='target' style='text-align:right'")
                        .addColumn("Exact Factor", "class='target'", null, "class='target'", true)
                        .setCellAttributes("class='target' style='text-align:right'")
                        .addColumn("Offset", "class='target'", null, "class='target'", true)
                        .setCellAttributes("class='target' style='text-align:right'");

        UnitConverter converter = SDI.getUnitConverter();
        converter.getSourceToSystems();

        Set<R4<UnitId, UnitSystem, Rational, String>> all = new TreeSet<>();

        for (Entry<String, TargetInfo> entry : converter.getInternalConversionData().entrySet()) {
            String sourceUnit = entry.getKey();
            String quantity = converter.getQuantityFromUnit(sourceUnit, false);
            TargetInfo targetInfo = entry.getValue();

            final EnumSet<UnitSystem> systems =
                    EnumSet.copyOf(converter.getSystemsEnum(sourceUnit));

            // to sort the right items together items together, put together a sort key
            UnitSystem sortingSystem = systems.iterator().next();
            switch (sortingSystem) {
                case si:
                case si_acceptable:
                    sortingSystem = UnitSystem.metric;
                    break;
                case uksystem:
                    sortingSystem = UnitSystem.ussystem;
                    break;
                default:
            }
            UnitId targetUnitId = converter.createUnitId(targetInfo.target);
            R4<UnitId, UnitSystem, Rational, String> sortKey =
                    Row.of(targetUnitId, sortingSystem, targetInfo.unitInfo.factor, sourceUnit);
            all.add(sortKey);

            // get some formatted strings
            // TODO: handle specials here, CLDR-16329 additional PR or follow-on ticket

            final String repeatingFactor =
                    targetInfo.unitInfo.factor.toString(FormatStyle.repeating);
            final String basicFactor = targetInfo.unitInfo.factor.toString(FormatStyle.approx);
            final String repeatingOffset =
                    targetInfo.unitInfo.offset.equals(Rational.ZERO)
                            ? ""
                            : targetInfo.unitInfo.offset.toString(FormatStyle.repeating);

            String targetDisplay = targetInfo.target;
            String normalized = converter.getStandardUnit(targetInfo.target);
            if (!targetDisplay.equals(normalized)) {
                targetDisplay = normalized + " = " + targetDisplay;
            }

            // now make a row

            tablePrinter
                    .addRow()
                    .addCell(sortKey)
                    .addCell(quantity)
                    .addCell(targetDisplay)
                    .addCell(Joiner.on(", ").join(systems))
                    .addCell(sourceUnit)
                    .addCell(basicFactor)
                    .addCell(repeatingFactor)
                    .addCell(repeatingOffset)
                    .finishRow();
        }
        pw.write(tablePrinter.toTable());
        if (DUMP_UNIT_TABLE_TO_STDOUT) {
            PrintWriter pw2 = new PrintWriter(System.out);
            tablePrinter.toTsv(pw2);
            pw2.flush();
            pw2.close();
        }
        //        for (R4<UnitId, UnitSystem, Rational, String> sk : all) {
        //            System.out.println(String.format("%s\t%s\t%s\t%s", sk.get0(), sk.get1(),
        // sk.get2(), sk.get3()));
        //        }
    }
}
