package org.unicode.cldr.tool;

import java.io.IOException;
import java.util.Map.Entry;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.Rational.FormatStyle;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.TargetInfo;

import com.google.common.base.Joiner;

public class ChartUnitConversions extends Chart {

    public static final String QUANTITY_MSG = "The units are grouped and ordered by Quantity (which are based on the NIST quantities, see "
        + "<a href='https://www.nist.gov/pml/special-publication-811' target='nist811'>NIST 811</a>). Note that the quantities are informative.";
    public static final String RATIONAL_MSG = "Each numeric value is an exact rational. The format is a terminating decimal where possible; "
        + "otherwise a repeating decimal if possible (where ˙ marks the start of the <a href='https://en.wikipedia.org/wiki/Repeating_decimal' target='wiki'>reptend</a>); "
        + "otherwise a <a href='https://en.wikipedia.org/wiki/Rational_number' target='wiki'>rational number</a> (of the form <i>numerator/denominator</i>).";
    public static final String SPEC_GENERAL_MSG = "The " + ldmlSpecLink("/tr35-general.html#Contents")
    + " should be consulted for more details, such as how to handle complex units (such as foot-per-minute) by converting the elements";

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
            + "There are many possible units, and additional units and conversions will be added in future releases.</p>"
            + "<ul>"
            + "<li>Each Source Unit is converted to the Target Unit by multiplying it by the Factor and adding the Offset (if any).</li>"
            + "<li>The unit identifiers are internal, and are to be localized for display to users. See <a href='https://www.unicode.org/cldr/charts/latest/by_type/units.area.html#hectare' target='units.area.hectare'>Hectare</a>, for example. "
            + "<li>" + RATIONAL_MSG + "</li>"
            + "<li>The Systems column indicates which systems the units are used in. For now, they just show the two ‘inch-pound’ systems.</li>"
            + "<li>" + QUANTITY_MSG + "</li>"
            + "<li>" + SPEC_GENERAL_MSG + ".</li>"
            + "</ul>"
            + dataScrapeMessage("/tr35-general.html#Contents", "common/testData/units/unitsTest.txt", "common/supplemental/units.xml");
    }

    @Override
    public void writeContents(FormattedFileWriter pw) throws IOException {
        //  <convertUnit source='ounce' baseUnit='kilogram' factor='lb_to_kg/16' systems="ussystem uksystem"/>
        //  <convertUnit source='fahrenheit' baseUnit='kelvin' factor='5/9' offset='2298.35/9' systems="ussystem uksystem"/>


        TablePrinter tablePrinter = new TablePrinter()
            .addColumn("Quantity", "class='target'", null, "class='target'", true)
            .setHidden(true)
            .setRepeatHeader(true)
            .setBreakSpans(true)
            .addColumn("Source Unit", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
            .addColumn("Factor", "class='target'", null, "class='target'", true)
            .setCellAttributes("class='target' style='text-align:right'")
            .addColumn("Offset", "class='target'", null, "class='target'", true)
            .setCellAttributes("class='target' style='text-align:right'")
            .addColumn("Target", "class='target'", null, "class='target'", true)
            .addColumn("Systems", "class='target'", null, "class='target'", true)
            .addColumn("Quantity", "class='target'", CldrUtility.getDoubleLinkMsg(), "class='target'", true)
            ;

        UnitConverter converter = SDI.getUnitConverter();
        converter.getSourceToSystems();

        for (Entry<String, TargetInfo> entry : converter.getInternalConversionData().entrySet()) {
            String sourceUnit = entry.getKey();
            String quantity = converter.getQuantityFromUnit(sourceUnit, false);
            TargetInfo targetInfo = entry.getValue();
            tablePrinter.addRow()
            .addCell(quantity)
            .addCell(sourceUnit)
            .addCell(targetInfo.unitInfo.factor.toString(FormatStyle.repeating))
            .addCell(targetInfo.unitInfo.offset.equals(Rational.ZERO) ? "" : targetInfo.unitInfo.offset.toString(FormatStyle.repeating))
            .addCell(targetInfo.target)
            .addCell(Joiner.on(", ").join(converter.getSourceToSystems().get(sourceUnit)))
            .addCell(quantity)
            .finishRow();
        }
        pw.write(tablePrinter.toTable());
    }
}
