package org.unicode.cldr.tool;

import java.io.IOException;
import java.util.Set;
import org.unicode.cldr.tool.FormattedFileWriter.Anchors;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.FileCopier;

public class ChartPersonNames extends Chart {

    private static final Factory CLDR_FACTORY = CLDRConfig.getInstance().getCldrFactory();
    static final String MAIN_HEADER =
            "<p>These charts shows the sample person names for different locales, formatted according to the locale's pattern.</p>";
    private static final boolean DEBUG = false;
    static final String DIR = CLDRPaths.CHART_DIRECTORY + "verify/personNames/";

    public static void main(String[] args) {
        new ChartPersonNames().writeChart(null);
    }

    @Override
    public String getDirectory() {
        return DIR;
    }

    @Override
    public String getTitle() {
        return "Person Name Charts";
    }

    @Override
    public String getFileName() {
        return "index";
    }

    @Override
    public String getExplanation() {
        return MAIN_HEADER;
    }

    @Override
    public void writeContents(FormattedFileWriter pw) throws IOException {
        FileCopier.ensureDirectoryExists(DIR);
        FileCopier.copy(Chart.class, "index.css", DIR);
        FormattedFileWriter.copyIncludeHtmls(DIR);

        FormattedFileWriter.Anchors anchors = new FormattedFileWriter.Anchors();
        writeSubcharts(anchors);
        pw.setIndex("Main Chart Index", "../index.html");
        pw.write(anchors.toString());
    }

    public void writeSubcharts(Anchors anchors) throws IOException {
        Set<String> locales = CLDR_FACTORY.getAvailable();
        for (String locale : locales) {
            if (locale.equals("root")) {
                continue;
            }
            CLDRFile cldrFile = CLDR_FACTORY.make(locale, false);
            String nameOrderGivenFirst =
                    cldrFile.getStringValue(
                            "//ldml/personNames/nameOrderLocales[@order=\"givenFirst\"]");
            if (nameOrderGivenFirst == null) {
                continue;
            }
            new ChartPersonName(locale).writeChart(anchors);
        }
    }
}
