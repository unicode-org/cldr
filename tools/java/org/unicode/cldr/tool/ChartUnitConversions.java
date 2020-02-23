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

    static final String RATIONAL_MSG = "Each numeric value is exact, presented in a simplified rational form: "
        + "any divisors that would cause repeating fractions are separated out. Example: <sup>0.25</sup>/<sub>3<sub></sub></sub> = <sup>25</sup>/<sub>300<sub></sub></sub>";
    
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
        return "<p>Unit Conversions provide conversions for units, "
            + "so that a source units can be converted into what is needed for localized "
            + "<a href='unit_preferences.html' target='unit_preferences'>Unit Preferences</a>.</p>"
            + "<ul><li>The units are organized by Quantity (which are based on the NIST quantities).</li>"
            + "<li>Each Source Unit is converted to the Target Unit by multiplying it by the Factor and adding the Offset (if any).</li>"
            + "<li>" + RATIONAL_MSG + "</li>"
            + "<li>The systems indicate which systems the units are used in.</li>"
            + "<li>The LDML spec should be consulted for more details, such as how to handle complex units (such as foot-per-minute) by converting the elements.</li>"
            + "</ul>"
            + dataScrapeMessage("common/supplemental/units.txt");
    }

    @Override
    public void writeContents(FormattedFileWriter pw) throws IOException {
        //  <convertUnit source='ounce' baseUnit='kilogram' factor='lb_to_kg/16' systems="ussystem uksystem"/>
        //  <convertUnit source='fahrenheit' baseUnit='kelvin' factor='5/9' offset='2298.35/9' systems="ussystem uksystem"/>


        TablePrinter tablePrinter = new TablePrinter()
            .addColumn("Quantity", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
            .setRepeatHeader(true)
            .addColumn("Source Unit", "class='target'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
            .setBreakSpans(true)
            .addColumn("Factor", "class='target'", null, "class='source'", true)
            .setCellAttributes("class='target' style='text-align:right'")
            .addColumn("Offset", "class='target'", null, "class='source'", true)
            .setCellAttributes("class='target' style='text-align:right'")
            .addColumn("Target", "class='source'", null, "class='source'", true)
           .addColumn("Systems", "class='target'", null, "class='target'", true);

        UnitConverter converter = SDI.getUnitConverter();
        converter.getSourceToSystems();

        for (Entry<String, TargetInfo> entry : converter.getInternalConversionData().entrySet()) {
            String sourceUnit = entry.getKey();
            String quantity = converter.getQuantityFromUnit(sourceUnit, false);
            TargetInfo targetInfo = entry.getValue();
            tablePrinter.addRow()
            .addCell(quantity)
            .addCell(sourceUnit)
            .addCell(targetInfo.unitInfo.factor.toString(FormatStyle.html))
            .addCell(targetInfo.unitInfo.offset.equals(Rational.ZERO) ? "" : targetInfo.unitInfo.offset.toString(FormatStyle.html))
            .addCell(targetInfo.target)
            .addCell(Joiner.on(", ").join(converter.getSourceToSystems().get(sourceUnit)))
            .finishRow();

        }
        pw.write(tablePrinter.toTable());
    }
}
