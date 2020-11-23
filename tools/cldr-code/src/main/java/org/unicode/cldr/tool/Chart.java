package org.unicode.cldr.tool;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.unicode.cldr.tool.FormattedFileWriter.Anchors;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRURLS;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.ibm.icu.text.ListFormatter;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.ULocale;

/**
 * To add a new chart, subclass this, and add the subclass to {@link ShowLanguages.printLanguageData()}. There isn't much
 * documentation, so best to look at a simple subclass to see how it works.
 * @author markdavis
 */
public abstract class Chart {
    public static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    public static final SupplementalDataInfo SDI = CONFIG.getSupplementalDataInfo();
    public static final CLDRFile ENGLISH = CONFIG.getEnglish();
    public static final String LS = System.lineSeparator();

    public static final String PREV_CHART_VERSION_DIRECTORY = ToolConstants.getBaseDirectory(ToolConstants.PREV_CHART_VERSION);
    public static final String CHART_VERSION_DIRECTORY = ToolConstants.getBaseDirectory(ToolConstants.CHART_VERSION);

    private static final String GITHUB_ROOT = CLDRURLS.CLDR_REPO_ROOT + "/blob/master/";
    private static final String LDML_SPEC = "https://unicode.org/reports/tr35/";

    public static String dataScrapeMessage(String specPart, String testFile, String... dataFiles) {
        final String dataFileList = dataFiles.length == 0 ? null :
            ListFormatter.getInstance(ULocale.ENGLISH).format(
                Arrays.asList(dataFiles).stream()
                .map(dataFile -> Chart.dataFileLink(dataFile))
                .collect(Collectors.toSet()));

        return "<p>"
        + "<b>Warning:</b> Do not scrape this chart for production data.\n"
        + "Instead, for the meaning of the fields and data consult the " + Chart.ldmlSpecLink(specPart)
        + (dataFileList == null ? "" : ", and for machine-readable source data, access " + dataFileList)
        + (testFile == null ? "" : ", and for test data, access "  + dataFileLink(testFile))
        + ".</p>\n";
    }

    private static String dataFileLink(String dataFile) {
        return "<a href='" + GITHUB_ROOT + dataFile + "' target='" + dataFile  + "'>" + dataFile + "</a>";
    }

    public static String ldmlSpecLink(String specPart) {
        return "<a href='" + LDML_SPEC + (specPart == null ? "" : specPart) + "' target='units.xml'>LDML specification</a>";
    }

    /**
     * null means a string will be constructed from the title. Otherwise a real file name (no html extension).
     * @return
     */
    public String getFileName() {
        return null;
    }

    /**
     * Show Date?
     * @return
     */
    public String getExplanation() {
        return null;
    }

    /**
     * Short explanation that will go just after the title/dates.
     * @return
     */
    public boolean getShowDate() {
        return true;
    }

    /**
     * Directory for the file to go into.
     * @return
     */
    public abstract String getDirectory();

    /**
     * Short title for page. Will appear at the top, and in the window title, and in the index.
     * @return
     */
    public abstract String getTitle();

    /**
     * Work
     * @param pw
     * @throws IOException
     */
    public abstract void writeContents(FormattedFileWriter pw) throws IOException;

    public void writeFooter(FormattedFileWriter pw) throws IOException {
        standardFooter(pw, AnalyticsID.CLDR);
    }

    private enum AnalyticsID {
        CLDR("UA-7672775-1"), ICU("UA-7670213-1"), ICU_GUIDE("UA-7670256-1"), UNICODE("UA-7670213-1"), UNICODE_UTILITY("UA-8314904-1");
        public final String id;

        private AnalyticsID(String id) {
            this.id = id;
        }
    }

    private static void standardFooter(FormattedFileWriter pw, AnalyticsID analytics) throws IOException {
        pw.write("<div style='text-align: center; margin-top:2em; margin-bottom: 60em;'><br>\n"
            + "<a href='http://www.unicode.org/unicode/copyright.html'>\n"
            + "<img src='http://www.unicode.org/img/hb_notice.gif' style='border-style: none; width: 216px; height=50px;' alt='Access to Copyright and terms of use'>"
            + "</a><br>\n<script src='http://www.unicode.org/webscripts/lastModified.js'></script>"
            + "</div><script>\n\n"
            + "(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){"
            + "(i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),"
            + "m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)"
            + "})(window,document,'script','//www.google-analytics.com/analytics.js','ga');"
            + "  ga('create', '"
            + analytics
            + "', 'auto');"
            + "  ga('send', 'pageview');"
            + "</script>\n");
    }

    public final void writeChart(Anchors anchors) {
        try (
            FormattedFileWriter x = new FormattedFileWriter(getFileName(), getTitle(), getExplanation(), anchors);) {
            x.setDirectory(getDirectory());
            x.setShowDate(getShowDate());
            writeContents(x);
            writeFooter(x);
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    public static String getTsvDir(String targetDir, String topicName) {
        String target = targetDir.replaceAll(topicName, "tsv");
        if (target.equals(targetDir)) {
            throw new IllegalArgumentException("Can't make TSV directory from " + targetDir);
        }
        return target;
    }
}
