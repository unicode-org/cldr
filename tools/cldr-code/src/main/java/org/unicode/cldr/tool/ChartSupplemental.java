package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.SubtypeToURLProvider;
import org.unicode.cldr.test.TestCache.TestResultBundle;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;

@Deprecated
public class ChartSupplemental extends Chart {
    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    static final CLDRFile ENGLISH = CLDR_CONFIG.getEnglish();
    static final String DIR = CLDRPaths.CHART_DIRECTORY + "verify/supplemental/";

    private final String locale;

    public ChartSupplemental(String locale) {
        super();
        this.locale = locale;
    }

    @Override
    public String getDirectory() {
        return DIR;
    }

    @Override
    public String getTitle() {
        return ENGLISH.getName(locale) + ": Overall Errors";
    }

    @Override
    public String getExplanation() {
        return "<p>This chart shows errors which apply to the entire locale.</p>";
    }

    @Override
    public String getFileName() {
        return locale;
    }

    @Override
    public void writeContents(
            Writer pw, Factory factory, TestResultBundle test, SubtypeToURLProvider urlProvider)
            throws IOException {
        CLDRFile cldrFile = factory.make(locale, true);

        if (test != null) {
            Set<CheckStatus> pp = new TreeSet<CheckStatus>();

            // add any 'early' errors
            pp.addAll(test.getPossibleProblems());

            // add all non-path status
            for (final String x : cldrFile) {
                List<CheckStatus> result = new ArrayList<CheckStatus>();
                test.check(x, result, cldrFile.getStringValue(x));
                for (final CheckStatus s : result) {
                    if (s.getEntireLocale()) pp.add(s);
                }
            }

            // report

            if (pp.isEmpty()) {
                pw.write("<h3>No Errors</h3>\n");
                pw.write("There are no overall locale issues to report");
            } else {
                pw.write("<h3>Overall Errors</h3>\n");
                pw.write("<ol>\n");
                for (final CheckStatus s : pp) {
                    pw.write(
                            String.format(
                                    "<li> <b>%s</b> <i title='%s'>%s</i>\n",
                                    s.getType(), s.getSubtype().name(), s.getSubtype()));
                    pw.write("<p>" + s.getMessage() + "</p>");
                    if (urlProvider != null) {
                        final String moreDetailsUrl = urlProvider.apply(s.getSubtype());
                        pw.write(String.format("<a href=\"%s\">more details</a>", moreDetailsUrl));
                    }
                    pw.write("</li>\n");
                }
                pw.write("</ol>\n\n");
            }
        }

        pw.write("</div> <!-- ReportSupplemental -->\n");
    }
}
