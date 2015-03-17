package org.unicode.cldr.tool;

import java.io.IOException;
import java.util.List;

import org.unicode.cldr.util.CLDRFile;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Row.R4;

public class ChartLanguageMatching extends Chart {

    public static void main(String[] args) {
        new ChartLanguageMatching().writeChart(null);
    }

    @Override
    public String getDirectory() {
        return FormattedFileWriter.CHART_TARGET_DIR;
    }
    @Override
    public String getTitle() {
        return "Language Matching";
    }
    @Override
    public String getExplanation() {
        return "<p>Language Matching data is used to match the user’s desired language/locales against an application’s supported languages/locales. "
            + "For more information, see "
            + "<a href='http://unicode.org/reports/tr35/#LanguageMatching'>Language Matching</a>. "
            + "The latest release data for this chart is in "
            + "<a href='http://unicode.org/cldr/latest/common/supplemental/languageInfo.xml'>languageInfo.xml</a>.<p>"
            + "<ul>"
            + "<li>The rules are tested in order for matches, with the first one winning.</li>"
            + "<li>The <i>Unknown</i> Language/Script/Region (*) matches any other code (of that type).</li>"
            + "<li>The <i>Distance</i> indicates how close the match is, where identical fields have distance = 0. </li>"
            + "<li>The <i>Sym?</i> column indicates whether the distance is symmetric "
            + "(thus the distance is used also for Supported→Desired as well as for Desired→Supported).</li>"
            + "</ul>"
            ;
    }

    @Override
    public void writeContents(FormattedFileWriter pw) throws IOException {
        TablePrinter tablePrinter = new TablePrinter()
        .addColumn("Desired", "class='source'", null, "class='source'", true)
        .addColumn("Supported", "class='source'", null, "class='source'", true)
        .addColumn("D. Code", "class='source'", null, "class='source'", true)
        .setBreakSpans(true)
        .addColumn("S. Code", "class='source'", null, "class='source'", true)
        .setBreakSpans(true)
        .addColumn("Distance", "class='target'", null, "class='target'", true)
        .addColumn("Sym?", "class='target'", null, "class='target'", true)
        ;

        for (String type : SDI.getLanguageMatcherKeys()) {
            pw.write("<h2>Type=" + type + "</h2>");
            List<R4<String, String, Integer, Boolean>> data = SDI.getLanguageMatcherData(type);
            for (R4<String, String, Integer, Boolean> row : data) {
                // <languageMatch desired="gsw" supported="de" percent="96" oneway="true" /> <!-- All Swiss speakers can read High German -->

                tablePrinter.addRow()
                //.addCell(ENGLISH.getName(locale))
                .addCell(getName(row.get0()))
                .addCell(getName(row.get1()))
                .addCell(row.get0())
                .addCell(row.get1())
                .addCell((100-row.get2()))
                .addCell(row.get3() ? "" : "✓")
                .finishRow();
            }
            pw.write(tablePrinter.toTable());
            tablePrinter.clearRows();
        }
    }

    private String getName(String codeWithStars) {
        if (!codeWithStars.contains("*")) {
            return ENGLISH.getName(codeWithStars);
        }
        String[] parts = codeWithStars.split("_");
        if (parts[0].equals("*")) {
            parts[0] = "und";
        }
        if (parts.length > 1 && parts[1].equals("*")) {
            parts[1] = "Zzzz";
        }
        if (parts.length > 2 && parts[2].equals("*")) {
            parts[2] = "ZZ";
        }
        return ENGLISH.getName(CollectionUtilities.join(parts, "_"), true, CLDRFile.SHORT_ALTS);
    }
}
