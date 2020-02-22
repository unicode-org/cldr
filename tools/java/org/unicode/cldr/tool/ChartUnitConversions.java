package org.unicode.cldr.tool;

import java.io.IOException;
import java.util.Map.Entry;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.TargetInfo;

import com.google.common.base.Joiner;

public class ChartUnitConversions extends Chart {

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
            + "so that a common internal unit can be converted into what is needed for <a href='unit_preferences.html'>Unit Preferences</a>.<p>";
    }

    @Override
    public void writeContents(FormattedFileWriter pw) throws IOException {
        //  <convertUnit source='ounce' baseUnit='kilogram' factor='lb_to_kg/16' systems="ussystem uksystem"/>
        //  <convertUnit source='fahrenheit' baseUnit='kelvin' factor='5/9' offset='2298.35/9' systems="ussystem uksystem"/>


        TablePrinter tablePrinter = new TablePrinter()
            .addColumn("Quantity", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
            .addColumn("Source Unit", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
            .setBreakSpans(true)
            .addColumn("Factor", "class='source'", null, "class='source'", true)
            .addColumn("Base Unit", "class='source'", null, "class='source'", true)
            .addColumn("Offset", "class='source'", null, "class='source'", true)
            .addColumn("Systems", "class='target'", null, "class='target'", true);

        UnitConverter converter = SDI.getUnitConverter();
        converter.getSourceToSystems();

        for (Entry<String, TargetInfo> entry : converter.getInternalConversionData().entrySet()) {
            String sourceUnit = entry.getKey();
            String quantity = converter.getQuantityFromUnit(sourceUnit, false);
            String baseUnit = converter.getBaseUnitFromQuantity(quantity);
            TargetInfo targetInfo = entry.getValue();
            tablePrinter.addRow()
            .addCell(quantity)
            .addCell(sourceUnit)
            .addCell(targetInfo.unitInfo.factor)
            .addCell(targetInfo.target)
            .addCell(targetInfo.unitInfo.offset)
            .addCell(Joiner.on(", ").join(converter.getSourceToSystems().get(sourceUnit)))
            .finishRow();

        }
        pw.write(tablePrinter.toTable());
    }
}
