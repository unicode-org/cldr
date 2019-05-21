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
            + "<a href='http://unicode.org/cldr/latest/common/supplemental/languageInfo.xml'>languageInfo.xml</a>. "
            + "The matching process is approximately:<p>"
            + "<ul>"
            + "<li>The rules are tested—in order—for matches, with the first one winning.</li>"
            + "<li>Any exact match between fields has zero distance.</li>"
            + "<li>The placeholder (*) matches any code (of that type). "
            + "For the last field in Supported, it must be different than Desired.</li>"
            + "<li>The <i>Distance</i> indicates how close the match is, where identical fields have distance = 0. </li>"
            + "<li>A ⬌︎ in the <i>Sym?</i> column indicates that the distance is symmetric, "
            + "and is thus used for both directions: Supported→Desired and Desired→Supported. "
            + "A → indicates that the distance is <i>not</i> symmetric: this is usually a <i>fallback</i> match.</li>"
            + "</ul>";
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
            .addColumn("Sym?", "class='target'", null, "class='target'", true);

        for (String type : SDI.getLanguageMatcherKeys()) {
            pw.write("<h2>Type=" + type + "</h2>");
            List<R4<String, String, Integer, Boolean>> data = SDI.getLanguageMatcherData(type);
            for (R4<String, String, Integer, Boolean> row : data) {
                // <languageMatch desired="gsw" supported="de" percent="96" oneway="true" /> <!-- All Swiss speakers can read High German -->

                tablePrinter.addRow()
                    //.addCell(ENGLISH.getName(locale))
                    .addCell(getName(row.get0(), true))
                    .addCell(getName(row.get1(), false))
                    .addCell(row.get0())
                    .addCell(row.get1())
                    .addCell((100 - row.get2()))
                    .addCell(row.get3() ? "→" : "⬌")
                    .finishRow();
            }
            pw.write(tablePrinter.toTable());
            tablePrinter.clearRows();
        }
    }

    private String getName(String codeWithStars, boolean user) {
        if (!codeWithStars.contains("*") && !codeWithStars.contains("$")) {
            return ENGLISH.getName(codeWithStars, true, CLDRFile.SHORT_ALTS);
        }
        String[] parts = codeWithStars.split("_");
        if (parts[0].equals("*")) {
            parts[0] = "xxx";
        }
        if (parts.length > 1 && parts[1].equals("*")) {
            parts[1] = "Xxxx";
        }
        String parts2orig = "XY";
        if (parts.length > 2) {
            parts2orig = parts[2];
            if (parts[2].equals("*")) {
                parts[2] = "XX";
            } else if (parts[2].startsWith("$")) {
                parts[2] = "XY";
            }
        }
        String result = ENGLISH.getName(CollectionUtilities.join(parts, "_"), true, CLDRFile.SHORT_ALTS);
        if (user) {
            result = result
                .replace("Xxxx", "any-script")
                .replace("xxx", "any-language")
                .replace("XX", "any-region")
                .replace("XY", parts2orig);
        } else {
            result = replaceStar(result);
        }
        return result;
    }

    private String replaceStar(String result) {
        String temp = result.replace("XX", "any-other-region");
        temp = temp.equals(result) ? temp.replace("Xxxx", "any-other-script") : temp.replace("Xxxx", "any-script");
        temp = temp.equals(result) ? temp.replace("xxx", "any-other-language") : temp.replace("xxx", "any-language");
        return temp;
    }
}
