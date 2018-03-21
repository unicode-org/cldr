package org.unicode.cldr.tool;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row.R2;

public class ChartSubdivisions extends Chart {

    static SubdivisionNames EN = new SubdivisionNames("en");

    public static void main(String[] args) {
        new ChartSubdivisions().writeChart(null);
    }

    @Override
    public String getDirectory() {
        return FormattedFileWriter.CHART_TARGET_DIR;
    }

    @Override
    public String getTitle() {
        return "Territory Subdivisions";
    }

    @Override
    public String getExplanation() {
        return "<p>Shows the subdivisions of territories, using the Unicode Subdivision Codes with the English names (and sort order). "
            + "For more information see the LDML spec.<p>";
    }

    @Override
    public void writeContents(FormattedFileWriter pw) throws IOException {

        TablePrinter tablePrinter = new TablePrinter()
            .addColumn("Region", "class='source'", null, "class='source'", true)
            .setSortPriority(1)
            .addColumn("Code", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
            .setBreakSpans(true)

            .addColumn("Subdivision1", "class='target'", null, "class='target'", true)
            .setSortPriority(2)
            .addColumn("Code", "class='target'", CldrUtility.getDoubleLinkMsg(), "class='target'", true)
            .setBreakSpans(true)

            .addColumn("Subdivision2", "class='target'", null, "class='target'", true)
            .setSortPriority(3)
            .addColumn("Code", "class='target'", CldrUtility.getDoubleLinkMsg(), "class='target'", true);

        Map<String, R2<List<String>, String>> aliases = SDI.getLocaleAliasInfo().get("subdivision");

        Set<String> remainder = new HashSet<>(Validity.getInstance().getStatusToCodes(LstrType.region).get(Status.regular));
        Relation<String, String> inverseAliases = Relation.of(new HashMap(), TreeSet.class);
        for (Entry<String, R2<List<String>, String>> entry : aliases.entrySet()) {
            List<String> value = entry.getValue().get0();
            inverseAliases.putAll(value, entry.getKey());
        }

        for (String container : SDI.getContainersForSubdivisions()) {
            boolean containerIsRegion = SubdivisionNames.isRegionCode(container);
            String region = containerIsRegion ? container : SubdivisionNames.getRegionFromSubdivision(container);
//            int pos = container.indexOf('-');
//            String region = pos < 0 ? container : container.substring(0, pos);
            for (String contained : SDI.getContainedSubdivisions(container)) {
                if (contained.equals("usas")) {
                    int debug = 0;
                }
                if (SDI.getContainedSubdivisions(contained) != null) {
                    continue;
                }
                String s1 = containerIsRegion ? contained : container;
                String s2 = containerIsRegion ? "" : contained;

                String name1 = getName(s1);
                String name2 = getName(s2);

                // mark aliases
                R2<List<String>, String> a1 = aliases.get(s1);
                if (a1 != null) {
                    name1 = "= " + a1.get0().get(0) + " (" + name1 + ")";
                }
                R2<List<String>, String> a2 = aliases.get(s2);
                if (a2 != null) {
                    name2 = "= " + a2.get0().get(0) + " (" + name2 + ")";
                }
                tablePrinter.addRow()
                    .addCell(ENGLISH.getName(CLDRFile.TERRITORY_NAME, region))
                    .addCell(region)
                    .addCell(name1)
                    //.addCell(type)
                    .addCell(s1)
                    .addCell(name2)
                    .addCell(s2)
                    .finishRow();
                remainder.remove(region);
            }
        }
        for (String region : remainder) {
            Set<String> regionAliases = inverseAliases.get(region);
            tablePrinter.addRow()
                .addCell(ENGLISH.getName(CLDRFile.TERRITORY_NAME, region))
                .addCell(region)
                .addCell(regionAliases == null ? "«none»" : "=" + CollectionUtilities.join(regionAliases, ", "))
                //.addCell(type)
                .addCell("")
                .addCell("")
                .addCell("")
                .finishRow();
        }
        pw.write(tablePrinter.toTable());
    }

    private static String getName(String s1) {
        return s1.isEmpty() ? "" : TransliteratorUtilities.toHTML.transform(CldrUtility.ifNull(EN.get(s1), ""));
    }

}
