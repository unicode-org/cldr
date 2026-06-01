package org.unicode.cldr.tool;

import com.ibm.icu.text.ListFormatter;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.ULocale;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.TestCache.TestResultBundle;
import org.unicode.cldr.tool.FormattedFileWriter.Anchors;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRURLS;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.VoterReportStatus.ReportId;

/**
 * To add a new chart, subclass this, and add the subclass to {@link
 * ShowLanguages.printLanguageData()}. There isn't much documentation, so best to look at a simple
 * subclass to see how it works.
 *
 * @author markdavis
 */
public abstract class Chart {
    public static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    public static final SupplementalDataInfo SDI = CONFIG.getSupplementalDataInfo();
    public static final CLDRFile ENGLISH = CONFIG.getEnglish();
    public static final String LS = System.lineSeparator();

    public static final String PREV_CHART_VERSION_DIRECTORY =
            ToolConstants.getBaseDirectory(ToolConstants.PREV_CHART_VERSION);
    public static final String CHART_VERSION_DIRECTORY =
            ToolConstants.getBaseDirectory(ToolConstants.CHART_VERSION);

    public static final String GITHUB_ROOT = CLDRURLS.CLDR_REPO_MAIN;
    public static final String LDML_SPEC = "https://unicode.org/reports/tr35/";

    public static String dataScrapeMessage(String specPart, String testFile, String... dataFiles) {
        final String dataFileList =
                dataFiles.length == 0
                        ? null
                        : ListFormatter.getInstance(ULocale.ENGLISH)
                                .format(
                                        Arrays.asList(dataFiles).stream()
                                                .map(dataFile -> Chart.dataFileLink(dataFile))
                                                .collect(Collectors.toSet()));

        return "<p>"
                + "<b>Warning:</b> Do not scrape this chart for production data.\n"
                + "Instead, for the meaning of the fields and data consult the "
                + Chart.ldmlSpecLink(specPart)
                + (dataFileList == null
                        ? ""
                        : ", and for machine-readable source data, access " + dataFileList)
                + (testFile == null ? "" : ", and for test data, access " + dataFileLink(testFile))
                + ".</p>\n";
    }

    private static String dataFileLink(String dataFile) {
        return "<a href='"
                + GITHUB_ROOT
                + dataFile
                + "' target='"
                + dataFile
                + "'>"
                + dataFile
                + "</a>";
    }

    public static String ldmlSpecLink(String specPart) {
        return "<a href='"
                + LDML_SPEC
                + (specPart == null ? "" : specPart)
                + "' target='units.xml'>LDML specification</a>";
    }

    /**
     * null means a string will be constructed from the title. Otherwise a real file name (no html
     * extension).
     *
     * @return
     */
    public String getFileName() {
        return null;
    }

    /**
     * Show Date?
     *
     * @return
     */
    public String getExplanation() {
        return null;
    }

    /**
     * Short explanation that will go just after the title/dates.
     *
     * @return
     */
    public boolean getShowDate() {
        return true;
    }

    /**
     * Directory for the file to go into.
     *
     * @return
     */
    public abstract String getDirectory();

    /**
     * Short title for page. Will appear at the top, and in the window title, and in the index.
     *
     * @return
     */
    public abstract String getTitle();

    /**
     * Work
     *
     * @param pw
     * @throws IOException
     */
    public void writeContents(FormattedFileWriter pw) throws IOException {
        writeContents(pw.getStringWriter());
    }

    /**
     * Helper function to use the default factory. Not for use within SurveyTool.
     *
     * @param pw
     * @throws IOException
     */
    public void writeContents(Writer pw) throws IOException {
        writeContents(pw, CLDRConfig.getInstance().getCldrFactory());
    }

    public void writeContents(OutputStream output, Factory factory) throws IOException {
        try (final Writer w = new OutputStreamWriter(output); ) {
            writeContents(w, factory);
        }
    }

    /**
     * Do the work of generating the chart.
     *
     * @param pw
     * @param factory
     * @throws IOException
     */
    public void writeContents(Writer pw, Factory factory) throws IOException {
        // TODO: this should be an abstract function.
        throw new IllegalArgumentException("Not implemented yet");
    }

    /**
     * extended function with some additional parameters subclasses may optionally implement this.
     */
    public void writeContents(
            Writer pw,
            Factory factory,
            TestResultBundle bundle,
            CheckCLDR.SubtypeToURLProvider urlProvider)
            throws IOException {
        this.writeContents(pw, factory);
    }

    private static final class AnalyticsHelper {
        private static final AnalyticsHelper INSTANCE = new AnalyticsHelper();

        public final String str;

        AnalyticsHelper() {
            str =
                    ToolUtilities.getUTF8Data("analytics.html")
                            .lines()
                            .collect(Collectors.joining("\n"));
        }
    }

    public enum AnalyticsID {
        CLDR("G-BPN1D3SEJM"),
        ICU("G-06PL1DM20S"),
        ICU_GUIDE("UA-7670256-1"),
        UNICODE("G-GC4HXC4GVQ"),
        UNICODE_UTILITY("G-0M7Q5QLZPV");
        public final String id;

        private AnalyticsID(String id) {
            this.id = id;
        }

        public String getScript() {
            return AnalyticsHelper.INSTANCE.str.replaceAll("TAG_ID", id);
        }
    }

    public final void writeChart(Anchors anchors) {
        try (FormattedFileWriter x =
                new FormattedFileWriter(getFileName(), getTitle(), getExplanation(), anchors); ) {
            x.setDirectory(getDirectory());
            x.setShowDate(getShowDate());
            writeContents(x);
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

    public String getFixLinkFromPath(CLDRFile cldrFile, String path) {
        String result = PathHeader.getLinkedView(CLDRConfig.getInstance().urls(), cldrFile, path);
        return result == null ? "" : result;
    }

    /**
     * Attempt to allocate the Chart that goes along with this report Also see {@link
     * org.unicode.cldr.util.VoterReportStatus.ReportId} and keep up to date
     */
    public static Chart forReport(final ReportId report, final String locale) {
        switch (report) {
            case personnames:
                return new ChartPersonName(locale);
            default:
                return null;
        }
    }
}
