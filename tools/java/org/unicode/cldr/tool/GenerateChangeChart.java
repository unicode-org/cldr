package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.CoverageInfo;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.FileCopier;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.PathHeader.SectionId;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XMLSource;

import com.google.common.base.Objects;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R5;
import com.ibm.icu.util.ICUUncheckedIOException;

public class GenerateChangeChart {
    private static final boolean QUICK_TEST = false;

    private static final String SEP = " • ";
    private static final String BY_PATH = "by path";
    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    private static final SupplementalDataInfo SUP = CONFIG.getSupplementalDataInfo();
    private static CLDRFile ENGLISH = CONFIG.getEnglish();

    public static void main(String[] args) throws IOException {
        Factory current = CONFIG.getCldrFactory();
        Factory old = Factory.make(CLDRPaths.ARCHIVE_DIRECTORY + "/cldr-25.0/common/main", ".*");
        PathHeader.Factory phf = PathHeader.getFactory(ENGLISH);

        Set<R5<PathHeader, CLDRLocale, String, String, String>> data = new TreeSet<>();

        CLDRFile currentRoot = current.make("root", true);
        CLDRFile oldRoot = old.make("root", true);
        Set<String> locales = CONFIG.getStandardCodes().getLocaleCoverageLocales(Organization.cldr, EnumSet.of(Level.MODERN));
        String dir = CLDRPaths.CHART_DIRECTORY + "changes/";
        CoverageInfo coverage = CONFIG.getCoverageInfo();
        EnumSet<SectionId> sections = EnumSet.noneOf(SectionId.class);
        FileCopier.ensureDirectoryExists(dir);
        FileCopier.copy(ShowLanguages.class, "index.css", dir);

        try (PrintWriter out = org.unicode.cldr.draft.FileUtilities.openUTF8Writer(dir, "summary.txt");) {
            Counter<SectionId> counter = new Counter<>();
            for (String locale : locales) {
                if (QUICK_TEST && locale.compareTo("b") >= 0) {
                    continue;
                }
                CLDRLocale cloc = CLDRLocale.getInstance(locale);
                CLDRFile currentFile = current.make(locale, true);
                if (currentFile == null) continue;
                CLDRFile oldFile = old.make(locale, true);
                Status status = new Status();
                counter.clear();
                for (SectionId s : SectionId.values()) {
                    counter.add(s, 0); // just to have values
                }

                for (String path : oldFile) {
                    Level level = coverage.getCoverageLevel(path, locale);
                    if (level.compareTo(Level.MODERN) > 0) {
                        continue;
                    }
                    String oldValue = oldFile.getStringValue(path);
                    String source = oldFile.getSourceLocaleID(path, status);
                    if (source.equals(XMLSource.CODE_FALLBACK_ID)
                        || source.equals(XMLSource.ROOT_ID)
                        || oldValue.equals(oldRoot.getStringValue(path))) {
                        continue;
                    }
                    String newValue = currentFile.getStringValue(path);
                    if (Objects.equal(oldValue, newValue)) {
                        continue;
                    }
                    String engValue = ENGLISH.getStringValue(path);
                    PathHeader ph = phf.fromPath(path);
                    sections.add(ph.getSectionId());
//                    final R4<SectionId, PageId, String, String> key = Row.of(ph.getSectionId(), ph.getPageId(), ph.getHeader(), locale);
//                    final R3<String, String, String> value = Row.of(ph.getCode(), oldValue, newValue);
                    final R5<PathHeader, CLDRLocale, String, String, String> key = Row.of(ph, cloc, oldValue, newValue, engValue);
                    data.add(key);
                    counter.add(ph.getSectionId(), 1);
                }
                final String summaryLine = locale + "\t" + ENGLISH.getName(locale) + "\t" + counter;
                System.out.println(summaryLine);
                out.println(summaryLine);
            }
            String topLinks = buildLinks(sections);

            LocaleFirstChartWriter localeFirstOut = null;
            PathFirstChartWriter pathFirstOut = null;

            SectionId lastSectionId = null;
            PrintWriter summary = null;

            int count = 0;
            for (R5<PathHeader, CLDRLocale, String, String, String> entry : data) {
                final PathHeader pathHeader = entry.get0();
                SectionId sectionId = pathHeader.getSectionId();
                if (sectionId != lastSectionId) {
                    if (lastSectionId != null) {
                        localeFirstOut.write();
                        pathFirstOut.write();
                        System.out.println(lastSectionId + "\t" + count);
                        out.println(lastSectionId + "\t" + count);
                        count = 0;
                    }
                    localeFirstOut = new LocaleFirstChartWriter(summary, dir, sectionId.toString(), topLinks);
                    pathFirstOut = new PathFirstChartWriter(summary, dir, sectionId.toString() + " " + BY_PATH, topLinks);
                    lastSectionId = sectionId;
                }
                ++count;
                PageId pageId = pathHeader.getPageId();
                String header = pathHeader.getHeader();
                String code = pathHeader.getCode();
                CLDRLocale locale = entry.get1();
                String oldValue = entry.get2();
                String newValue = entry.get3();
                String engValue = entry.get4();
                int votes = SUP.getRequiredVotes(locale, pathHeader);
                localeFirstOut.add(pathHeader, locale, oldValue, newValue, engValue, votes);
                pathFirstOut.add(pathHeader, locale, oldValue, newValue, engValue, votes);
            }
            localeFirstOut.write();
            pathFirstOut.write();
            System.out.println(lastSectionId + "\t" + count);
            out.println(lastSectionId + "\t" + count);
        }
    }

    public static <T> String buildLinks(Set<T> sections) {
        StringBuilder topLinksBuilder = new StringBuilder("<p align='center'>");
        boolean first = true;
        for (T x : sections) {
            if (!first) {
                topLinksBuilder.append(SEP);
            }
            String xString = x.toString();
            topLinksBuilder.append("<a href='" + FileUtilities.anchorize(xString) + ".html'>" + xString + "</a>");
            topLinksBuilder.append(" (<a href='" + FileUtilities.anchorize(xString + " " + BY_PATH) + ".html'>" + BY_PATH + "</a>)");
            first = false;
        }
        String topLinks = topLinksBuilder.append("</p>").toString();
        return topLinks;
    }

    static class LocaleFirstChartWriter extends ChartWriter {
        public LocaleFirstChartWriter(PrintWriter summary, String dir, String title, String explanation) {
            super(summary, dir, title, explanation);
        }

        {
            this
                .addColumn("Min Votes", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
                .setSpanRows(true)
                .setSortPriority(0)
                .setSortAscending(false)
                .setBreakSpans(true)
                .addColumn("Locale Name", "class='source'", null, "class='source'", true)
                //            .setSpanRows(true)
                .setSortPriority(1)
                .setBreakSpans(true)
                .addColumn("Locale ID", "class='source'", null, "class='source'", true)
                //            .setSpanRows(true)
                //.setBreakSpans(true)
                .addColumn("PageHeader", "class='source'", null, "class='source'", true)
                .setSortPriority(3)
                .setHidden(true)
                .addColumn("Page", "class='source'", null, "class='source'", true)
                .setSpanRows(true)
                .setBreakSpans(true)
                .addColumn("Header", "class='source'", null, "class='source'", true)
                .setSpanRows(true)
                .setBreakSpans(true)
                .addColumn("Code", "class='source'", null, "class='source'", true)
                .addColumn("Eng Value", "class='source'", null, "class='source'", true)
                .setSpanRows(true)
                .addColumn("Old Value", "class='target'", null, "class='target'", true)
                .addColumn("", "class='target'", null, "class='target'", true)
                .addColumn("New Value", "class='target'", null, "class='target'", true);
        }

        void add(final PathHeader pathHeader, CLDRLocale locale,
            String oldValue, String newValue, String engValue, int votes) {
            final String name = ENGLISH.getName(locale.toString());
            PageId pageId = pathHeader.getPageId();
            String header = pathHeader.getHeader();
            String code = pathHeader.getCode();
            addRow()
                .addCell(votes)
                .addCell(name)
                .addCell(locale)
                .addCell(pathHeader) // hidden field
                .addCell(pageId)
                .addCell(header == null ? "" : header)
                .addCell(code)
                .addCell(engValue == null ? "∅" : engValue)
                .addCell(oldValue)
                .addCell("→")
                .addCell(newValue == null ? "∅" : newValue)
                .finishRow();
        }
    }

    static class PathFirstChartWriter extends ChartWriter {
        public PathFirstChartWriter(PrintWriter summary, String dir, String title, String explanation) {
            super(summary, dir, title, explanation);
        }

        {
            this
                //.setBreakSpans(true)
                .addColumn("PageHeader", "class='source'", null, "class='source'", true)
                .setSortPriority(0)
                .setHidden(true)
                .addColumn("Page", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
                .setSpanRows(true)
                .setBreakSpans(true)
                .addColumn("Header", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
                .setSpanRows(true)
                .setBreakSpans(true)
                .addColumn("Code", "class='source'", null, "class='source'", true)
                .addColumn("Eng Value", "class='source'", null, "class='source'", true)
                .setSpanRows(true)
                .addColumn("Min Votes", "class='source'", null, "class='source'", true)
                .setSpanRows(true)
                .setSortPriority(1)
                .setSortAscending(false)
                .setBreakSpans(true)
                .addColumn("Locale Name", "class='source'", null, "class='source'", true)
                .setSortPriority(2)
                .setBreakSpans(true)
                .addColumn("Locale ID", "class='source'", null, "class='source'", true)
                .addColumn("Old Value", "class='target'", null, "class='target'", true)
                .addColumn("", "class='target'", null, "class='target'", true)
                .addColumn("New Value", "class='target'", null, "class='target'", true);
        }

        void add(final PathHeader pathHeader, CLDRLocale locale,
            String oldValue, String newValue, String engValue, int votes) {
            final String name = ENGLISH.getName(locale.toString());
            PageId pageId = pathHeader.getPageId();
            String header = pathHeader.getHeader();
            String code = pathHeader.getCode();
            addRow()
                .addCell(pathHeader) // hidden field
                .addCell(pageId)
                .addCell(header == null ? "" : header)
                .addCell(code)
                .addCell(engValue == null ? "∅" : engValue)
                .addCell(votes)
                .addCell(name)
                .addCell(locale)
                .addCell(oldValue)
                .addCell("→")
                .addCell(newValue == null ? "∅" : newValue)
                .finishRow();
        }
    }

    static class ChartWriter extends TablePrinter {
        private FormattedFileWriter out;

        public ChartWriter(PrintWriter summary, String dir, String title, String explanation) {
            try {
                out = new FormattedFileWriter(summary, dir, title.toString(), explanation, null);
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }

        public void write() {
            try {
                out.write(this.toTable());
                out.close();
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e);
            }
        }
    }

    private static String getFileName(String string) {
        return string.replaceAll("[^.a-zA-Z0-9]+", "_");
    }

    public static class FormattedFileWriter extends java.io.Writer {

        private StringWriter out = new StringWriter();

        private String title;

        private String filename;

        private String dir;

        public FormattedFileWriter(PrintWriter indexFile, String dir, String title, String explanation, List<String> anchors)
            throws IOException {
            this.dir = dir;
            String anchor = FileUtilities.anchorize(title);
            filename = anchor + ".html";
            this.title = title;
            if (anchors != null) {
                anchors.add("<a name='" + anchor + "' href='" + getFilename() + "'>"
                    + getTitle() + "</a></caption>");
            }
            //String helpText = getHelpHtml(anchor);
//            if (explanation == null) {
//                explanation = helpText;
//            }
            if (explanation != null) {
                out.write(explanation);
            }
            out.write("<div align='center'>");
        }

        public String getFilename() {
            return filename;
        }

        public String getTitle() {
            return title;
        }

        public void close() throws IOException {
            out.write("</div>");
            PrintWriter pw2 = org.unicode.cldr.draft.FileUtilities.openUTF8Writer(dir, filename);
            String[] replacements = { "%header%", "", "%title%", title, "%version%", ToolConstants.CHART_DISPLAY_VERSION,
                "%date%", CldrUtility.isoFormatDateOnly(new Date()), "%body%", out.toString() };
            final String templateFileName = "chart-template.html";
            FileUtilities.appendBufferedReader(ToolUtilities.getUTF8Data(templateFileName), pw2, replacements);
            pw2.close();
        }

        public void write(char[] cbuf, int off, int len) throws IOException {
            out.write(cbuf, off, len);
        }

        public void flush() throws IOException {
            out.flush();
        }
    }

}
