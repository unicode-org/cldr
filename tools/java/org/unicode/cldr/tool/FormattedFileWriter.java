package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.test.HelpMessages;
import org.unicode.cldr.util.ArrayComparator;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;

import com.ibm.icu.text.Collator;
import com.ibm.icu.util.ULocale;

public class FormattedFileWriter extends java.io.Writer {
    public static final String CHART_TARGET_DIR = CLDRPaths.CHART_DIRECTORY + "/supplemental/";
    public static final Collator COL = Collator.getInstance(ULocale.ROOT).setStrength2(Collator.IDENTICAL);
    //public static final PairComparator<String,String> PC = new PairComparator(COL, null);
    public static final ArrayComparator PC = new ArrayComparator(COL);

    ///Comparator<Pair<>>

    public static class Anchors {
        boolean hasExplanations = false;
        private Set<String[]> anchors = new TreeSet<String[]>(PC);

        @Override
        public String toString() {
            /*
            <div id="chits">
            <div class='chit chGold'>
            <a name='g002C' title='U+002C &#x002C; COMMA' href='#g002C'>
            <img class='chitImg' src='/consortium/aacimg/002C.png' alt='&#x002C;'></a>Mark Davis and Anne Gundelfinger
            </div>
             */
            //StringBuffer contents = new StringBuffer("<div align='center'>" + Chart.LS + "<table>" + Chart.LS); //
            StringBuffer contents = new StringBuffer("<div id='chits'>" + Chart.LS); //
            ArrayList<String[]> anchorList = new ArrayList<>(anchors); // flatten
            for (String[] item : anchorList) {
                String title = item[0];
                String fileName = item[1];
                String explanation = item[2];
                contents
                    .append("\t<div class='chit'><a name='" + FileUtilities.anchorize(title) + "' href='" + fileName + "'>" + title + "</a></div>" + Chart.LS);
                if (hasExplanations) {
                    contents.append("\t<div class='chit'>" + explanation + "</div>" + Chart.LS);
                }
            }
//            int columns = hasExplanations ? 2 : 4;
//            int rows = 1 + (anchorList.size() - 1) / columns;
//            String td = "<td class='plain' style='width:" + (100 / columns) + "%'>";
//            for (int row = 0; row < rows; ++row) {
//                contents.append("<tr>" + Chart.LS);
//                for (int column = 0; column < columns; ++column) {
//                    int index = column * rows + row;
//                    String linkedTitle = "";
//                    String explanation = "";
//                    if (index < anchorList.size()) {
//                        String[] item = anchorList.get(index);
//                        String title = item[0];
//                        String fileName = item[1];
//                        explanation = item[2];
//                        linkedTitle = "<a name='" + FileUtilities.anchorize(title) + "' href='" + fileName + "'>" + title + "</a>";
//                    }
//                    contents.append(td + linkedTitle + "</td>" + Chart.LS);
//                    if (hasExplanations) {
//                        contents.append(td + explanation + "</td>" + Chart.LS);
//                    }
//                }
//                contents.append("</tr>" + Chart.LS);
//                td = "<td class='plain'>"; // only need width on first row
//            }
            contents.append("</div>" + Chart.LS);
            // contents.append("</table>" + Chart.LS + "</div>" + Chart.LS);
            return contents.toString();
        }

        public void add(String title, String fileName, String explanation) {
            anchors.add(new String[] { title, fileName, explanation });
            if (explanation != null) {
                hasExplanations = true;
            }
        }
    }

    private Anchors localeAnchors;

    private String dir;

    private String title;
    private String filename;

    private String indexLink = "index.html";
    private String indexTitle = "Index";

    private String explanation;
    private boolean showDate = true;

    private StringWriter out = new StringWriter();

    public FormattedFileWriter(String baseFileName, String title, String explanation, Anchors anchors)
        throws IOException {
        // we set up a bunch of variables, but we won't actually use them unless there is generate content. See close()
        if (baseFileName == null) {
            baseFileName = FileUtilities.anchorize(title);
        }
        this.dir = FormattedFileWriter.CHART_TARGET_DIR;
        this.filename = baseFileName;
        this.title = title;
        this.explanation = explanation;
        this.localeAnchors = anchors;
    }

    public FormattedFileWriter setDirectory(String dir) {
        this.dir = dir;
        return this;
    }

    public FormattedFileWriter setShowDate(boolean showDate) {
        this.showDate = showDate;
        return this;
    }

    public void close() throws IOException {
        String contents = out.toString();
        if (contents.isEmpty()) {
            return; // skip writing if there are no contents
        }
        if (explanation == null) {
            explanation = HelpMessages.getChartMessages(filename);
        }
        if (explanation != null) {
            contents = explanation + contents;
        }
        if (localeAnchors != null) {
            localeAnchors.add(title, filename + ".html", null);
        }
        PrintWriter pw2 = org.unicode.cldr.draft.FileUtilities.openUTF8Writer(dir, filename + ".html");
        String[] replacements = { "%header%", "",
            "%title%", title,
            "%version%", ToolConstants.CHART_DISPLAY_VERSION,
            "%index%", indexLink,
            "%index-title%", indexTitle,
            "%date%", getDateValue(),
            "%body%", contents };
        final String templateFileName = "chart-template.html";
        FileUtilities.appendBufferedReader(ToolUtilities.getUTF8Data(templateFileName), pw2, replacements);
        pw2.close();
    }

    private String getDateValue() {
        return showDate ? CldrUtility.isoFormatDateOnly(new Date()) : "";
    }

    public void write(char[] cbuf, int off, int len) throws IOException {
        out.write(cbuf, off, len);
    }

    public void flush() throws IOException {
        out.flush();
    }

    public FormattedFileWriter setIndex(String indexTitle_, String indexLink_) {
        indexLink = indexLink_;
        indexTitle = indexTitle_;
        return this;
    }
}