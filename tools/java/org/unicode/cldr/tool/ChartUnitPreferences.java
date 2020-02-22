package org.unicode.cldr.tool;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitPreferences;
import org.unicode.cldr.util.UnitPreferences.UnitPreference;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Multimap;

public class ChartUnitPreferences extends Chart {
    
    public static void main(String[] args) {
        new ChartUnitPreferences().writeChart(null);
    }

    @Override
    public String getDirectory() {
        return FormattedFileWriter.CHART_TARGET_DIR;
    }

    @Override
    public String getTitle() {
        return "Unit Preferences";
    }

    @Override
    public String getExplanation() {
        return "<p>Unit Preferences provide a way to get the units that are appropriate for different regions and usage. "
            + "The preference also may vary by the size of the unit. "
            + "The <a href='unit_conversions.html'>Unit Conversions</a> are used to convert the resulting units.<p>";
    }

    @Override
    public void writeContents(FormattedFileWriter pw) throws IOException {
        // #   Quantity;   Usage;  Region; Input (r);  Input (d);  Input Unit; Output (r); Output (d); Output Unit

        TablePrinter tablePrinter = new TablePrinter()
            .addColumn("Quantity", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
            .setSortPriority(1)
            .addColumn("Usage", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
            .setSortPriority(2)
            .addColumn("Sample Region", "class='source'", null, "class='source'", true)
            .setSortPriority(3)
            .addColumn("If ≥", "class='source'", null, "class='source'", true)
            .setBreakSpans(true)
            .addColumn("Unit", "class='target'", null, "class='target'", true)
            .addColumn("Skeleton", "class='target'", null, "class='target'", true);

        UnitConverter converter = SDI.getUnitConverter();
        UnitPreferences prefs = SDI.getUnitPreferences();
        for (Entry<String, Map<String, Multimap<Set<String>, UnitPreference>>> entry : prefs.getData().entrySet()) {
            String quantity = entry.getKey();
            String baseUnit = converter.getBaseUnitFromQuantity(quantity);
            for (Entry<String, Multimap<Set<String>, UnitPreference>> entry2 : entry.getValue().entrySet()) {
                String usage = entry2.getKey();
                for (Entry<Set<String>, Collection<UnitPreference>> entry3 : entry2.getValue().asMap().entrySet()) {
                    
                    Set<String> regions = entry3.getKey();
                    String sampleRegion = regions.iterator().next();
                    for (UnitPreference pref : entry3.getValue()) {
                        tablePrinter.addRow()
                        .addCell(quantity)
                        .addCell(usage)
                        .addCell(sampleRegion)
                        .addCell(pref.geq)
                        .addCell(Joiner.on(" & ").join(Splitter.on("-and-").split(pref.unit)))
                        .addCell(pref.skeleton)
                        .finishRow();

                    }
                }
            }
        }
        pw.write(tablePrinter.toTable());
    }
}
