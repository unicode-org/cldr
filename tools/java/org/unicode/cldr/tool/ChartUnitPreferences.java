package org.unicode.cldr.tool;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.Rational.FormatStyle;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitPreferences;
import org.unicode.cldr.util.UnitPreferences.UnitPreference;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

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
        return "<p>Unit Preferences provide a way to get the units that are appropriate for a region, and usage, and threshold amounts. "
            + "The this release adds additional structure for usage and threshold amount, allowing for more additions of regions, usages, thresholds, and units in future releases.</p>"
            + "<ul>"
            + ChartUnitConversions.NIST_SOURCES
            + "<li>The unit identifiers are internal, and would be localized for display to users. See <a href='https://www.unicode.org/cldr/charts/latest/by_type/units.area.html#hectare' target='units.area.hectare'>Hectare</a>, for example. "
            + "<li>The sample region  represents a set of regions if it has a superscript. See the table at the bottom. 001 (World) means the default if no specific region is found.</li>"
            + "<li>The 'If ≥' column shows the thresholds: the first line for a given region where the input amount is greater or equal applies. "
            + "For example, for 0.5km as input for [area, default, 001] would result in <i>hectare</i>.</li>"
            + "<li>The <a href='unit_conversions.html' target='unit_conversions'>Unit Conversions</a> are used to handle conversion of units needed to use the preferences.</li>"
            + "<li>" + ChartUnitConversions.RATIONAL_MSG + "</li>"
            + "<li>The LDML spec should be consulted for more details, such as how to handle complex units (such as foot-per-minute) by converting the elements, and how to fall back if a given usage or region is not found.</li>"
            + "</ul>"
            + dataScrapeMessage("common/supplemental/units.xml", "common/testData/units/unitPreferencesTest.txt", "/tr35-general.html#Contents");
    }

    @Override
    public void writeContents(FormattedFileWriter pw) throws IOException {
        // #   Quantity;   Usage;  Region; Input (r);  Input (d);  Input Unit; Output (r); Output (d); Output Unit

        TablePrinter tablePrinter = new TablePrinter()
            .addColumn("Quantity", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
            .setSortPriority(1)
            .setBreakSpans(true)
            .setRepeatHeader(true)
            .addColumn("Usage", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
            .setSortPriority(2)
            .setBreakSpans(true)
            .addColumn("Sample Region", "class='source'", null, "class='source'", true)
            .setSortPriority(3) 
            .addColumn("If ≥", "class='target'", null, "class='source'", true)
            .setCellAttributes("class='target' style='text-align:right'")
            .setSpanRows(false)
            .addColumn("Unit", "class='target'", null, "class='target'", true)
            .setSpanRows(false)
            .addColumn("Skeleton", "class='target'", null, "class='target'", true);

        UnitConverter converter = SDI.getUnitConverter();
        UnitPreferences prefs = SDI.getUnitPreferences();
        Samples samples = new Samples();

        for (Entry<String, Map<String, Multimap<Set<String>, UnitPreference>>> entry : prefs.getData().entrySet()) {
            String quantity = entry.getKey();
            String baseUnit = converter.getBaseUnitFromQuantity(quantity);
            for (Entry<String, Multimap<Set<String>, UnitPreference>> entry2 : entry.getValue().entrySet()) {
                String usage = entry2.getKey();
                for (Entry<Set<String>, Collection<UnitPreference>> entry3 : entry2.getValue().asMap().entrySet()) {

                    Set<String> regions = entry3.getKey();
                    Pair<String, Integer> sampleRegion = samples.getSample(regions);
                    final String sampleRegionStr = sampleDisplay(sampleRegion);

                    for (UnitPreference pref : entry3.getValue()) {
                        tablePrinter.addRow()
                        .addCell(quantity)
                        .addCell(usage)
                        .addCell(sampleRegionStr)
                        .addCell(pref.geq.toString(FormatStyle.html))
                        .addCell(Joiner.on(" & ").join(Splitter.on("-and-").split(pref.unit)))
                        .addCell(pref.skeleton)
                        .finishRow();

                    }
                }
            }
        }
        pw.write(tablePrinter.toTable());
        pw.write("<br><h1>Region Sets</h1>\n");
        TablePrinter tablePrinter2 = new TablePrinter()
            .addColumn("Sample Region", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
            .addColumn("Region Set", "class='target'", CldrUtility.getDoubleLinkMsg(), "class='source'", true);
        
        TreeSet<Pair<String, Integer>> sorted = new TreeSet<>(samples.setToSample.values());
        for (Pair<String, Integer> pair : sorted) {
            tablePrinter2.addRow()
            .addCell(sampleDisplay(pair))
            .addCell(Joiner.on(", ").join(samples.setToSample.inverse().get(pair)))
            .finishRow();
        }
        pw.write(tablePrinter2.toTable());
    }

    private String sampleDisplay(Pair<String, Integer> sampleRegion) {
        return sampleRegion.getFirst() + (sampleRegion.getSecond() < 0 ?  "" : "<sup>" + sampleRegion.getSecond() + "</sup>");
    }

    private static final class Samples {
        BiMap<Set<String>, Pair<String, Integer>> setToSample = HashBiMap.create();
        Multiset<String> counters = HashMultiset.create();

        private Pair<String, Integer> getSample(Set<String> regions) {
            if (regions.size() == 1) {
                return new Pair<>(regions.iterator().next(), -1);
            }
            Pair<String, Integer> sample = setToSample.get(regions);
            if (sample == null) {
                String sampleBase = regions.iterator().next() + ", …";
                counters.add(sampleBase);
                setToSample.put(regions, sample = new Pair<>(sampleBase, counters.count(sampleBase)));
            }
            return sample;
        }
    }
}
