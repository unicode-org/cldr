package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Pair;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.FileUtilities;

public class FormattedFileWriter extends java.io.Writer {
    
    public static class Anchors {
        private static List<Pair<String,String>> anchors = new ArrayList<Pair<String,String>>();
    
        @Override
        public String toString() {
            StringBuffer contents = new StringBuffer("<table>");
            for (Pair<String, String> item : anchors) {
                contents.append("<tr><td class='plain'>" + item.getFirst() + "</td>");
                if (item.getSecond() != null) {
                    contents.append("<td class='plain'>" + item.getSecond() + "</td>");
                }
                contents.append("<tr>");
            }
            contents.append("</table>");
            return contents.toString();
        }
    
        public void add(String title, String fileName, String explanation) {
            anchors.add(Pair.of("<a name='" + FileUtilities.anchorize(title) + "' href='" + fileName + "'>"
                + title + "</a>", explanation));
        }
    }

    Anchors localeAnchors;

    private StringWriter out = new StringWriter();

    private String title;

    private String filename;

    private String dir;
    
    private String index = "../index.html";

    public FormattedFileWriter(PrintWriter indexFile, String fileName, String title, String explanation, boolean skipIndex)
        throws IOException {
        this(indexFile, fileName, title, explanation, skipIndex, ShowLanguages.anchors);
    }
    
    public FormattedFileWriter(PrintWriter indexFile, String baseFileName, String title, String explanation, boolean skipIndex, Anchors anchors)
        throws IOException {
        if (baseFileName == null) {
            baseFileName = FileUtilities.anchorize(title);
        }
        localeAnchors = anchors;
        this.dir = ShowLanguages.CHART_TARGET_DIR;
        filename = baseFileName + ".html";
        this.title = title;
        if (!skipIndex) {
            localeAnchors.add(getTitle(), getFilename(), null);
        }
        if (explanation == null) {
            explanation = ShowLanguages.getHelpHtml(filename);
        }
        if (explanation != null) {
            out.write(explanation);
        }
    }

    public FormattedFileWriter setDirectory(String dir) {
        this.dir = dir;
        return this;
    }
    
    public String getFilename() {
        return filename;
    }

    public String getTitle() {
        return title;
    }

    public FormattedFileWriter setTitle(String title) {
        this.title = title;
        return this;
    }

    public void close() throws IOException {
        PrintWriter pw2 = BagFormatter.openUTF8Writer(dir, filename);
        String[] replacements = { "%header%", "", 
            "%title%", title, 
            "%version%", ToolConstants.CHART_DISPLAY_VERSION,
            "%index%", index, 
            "%date%", CldrUtility.isoFormat(new Date()), 
            "%body%", out.toString() };
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

    public FormattedFileWriter setAnchors(FormattedFileWriter.Anchors anchors) {
        localeAnchors = anchors;
        return this;
    }

    public FormattedFileWriter setIndex(String indexString) {
        index = indexString;
        return this;
    }
}