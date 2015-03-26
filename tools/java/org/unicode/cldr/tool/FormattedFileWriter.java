package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CldrUtility.PairComparator;
import org.unicode.cldr.util.Pair;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.FileUtilities;
import com.ibm.icu.text.Collator;
import com.ibm.icu.util.ULocale;

public class FormattedFileWriter extends java.io.Writer {
    public static final String CHART_TARGET_DIR = CLDRPaths.CHART_DIRECTORY + "/supplemental/";
    public static final Collator COL = Collator.getInstance(ULocale.ROOT).setStrength2(Collator.IDENTICAL);
    public static final PairComparator<String,String> PC = new PairComparator(COL, COL);

    ///Comparator<Pair<>>

    public static class Anchors {
        private Set<Pair<String,String>> anchors = new TreeSet<Pair<String,String>>(PC);
    
        @Override
        public String toString() {
            
            StringBuffer contents = new StringBuffer("<table>");
            for (Pair<String, String> item : anchors) {
                contents.append("<tr><td class='plain'>" + item.getFirst() + "</td>");
                if (item.getSecond() != null) {
                    contents.append("<td class='plain'>" + item.getSecond() + "</td>");
                }
                contents.append("</tr>");
            }
            contents.append("</table>");
            return contents.toString();
        }
    
        public void add(String title, String fileName, String explanation) {
            anchors.add(Pair.of("<a name='" + FileUtilities.anchorize(title) + "' href='" + fileName + "'>"
                + title + "</a>", explanation));
        }
    }

    private Anchors localeAnchors;

    private String dir;

    private String title;
    private String filename;

    private String indexLink = "index.html";
    private String indexTitle = "Index";

    private String explanation;

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
    
    public void close() throws IOException {
        String contents = out.toString();
        if (contents.isEmpty()) {
            return; // skip writing if there are no contents
        }
        if (explanation == null) {
            explanation = ShowLanguages.getHelpHtml(filename);
        }
        if (explanation != null) {
            contents = explanation + contents;
        }
        if (localeAnchors != null) {
            localeAnchors.add(title, filename + ".html", null);
        }
        PrintWriter pw2 = BagFormatter.openUTF8Writer(dir, filename + ".html");
        String[] replacements = { "%header%", "", 
            "%title%", title, 
            "%version%", ToolConstants.CHART_DISPLAY_VERSION,
            "%index%", indexLink, 
            "%index-title%", indexTitle, 
            "%date%", CldrUtility.isoFormat(new Date()), 
            "%body%", contents };
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

    public FormattedFileWriter setIndex(String indexTitle_, String indexLink_) {
        indexLink = indexLink_;
        indexTitle = indexTitle_;
        return this;
    }
}