package org.unicode.cldr.util;

import java.io.IOException;
import java.io.Writer;

/**
 * Generate HTML for the "Upload XML" (gear menu) interface
 *
 * Some code has been, or should be, moved here from jsp and jspf files.
 * Reference: https://unicode-org.atlassian.net/browse/CLDR-11877
 *
 * TODO: in the long term, avoid generating html on the server. Use ajax. Let the client handle the UI presentation.
 */
public class XMLUploader {

    /**
     * Write a portion of the HTML for the Bulk Upload interface
     *
     * @param request the HttpServletRequest
     * @param bulkStage the string such as "upload", "check", "test", "submit"
     * @throws IOException
     *
     * Some code was moved here from bulkinfo.jspf
     * Reference: https://unicode-org.atlassian.net/browse/CLDR-11877
     */
    static public void writeBulkInfoHtml(String bulkStage, Writer out) throws IOException {
        out.write("<div class='bulkNextInfo'>\n");
        out.write("<ul>\n");
        out.write("<li class='header'>Bulk Upload:</li>\n");
        String stages[] = { "upload", "Upload XML file",
            "check",  "Verify valid XML",
            "test",   "Test for CLDR errors",
            "submit",   "Data submitted into SurveyTool"
        };

        for (int i = 0; i < stages.length; i += 2) {
            final int stageNumber = (i/2) + 1;
            final String stageName = stages[i + 0];
            final String stageDescription = stages[i + 1];
            final boolean active = bulkStage.equals(stageName);
            final String activeClass = active ? "active" : "inactive";

            out.write("<li class='" + activeClass + "'>\n");
            out.write("<h1>" + stageNumber + ". " + stageName + "</h1>\n");
            out.write("<h2>" + stageDescription + "</h2>\n");
            out.write("</li>\n");
        }
        out.write("</ul>\n");
        if (bulkStage.equals("upload")) {
            out.write("<p class='helpContent'>");
            out.write("Click the button in the bottom-right corner to proceed to the next step.");
            out.write("</p>\n");
        }
        out.write("</div>\n");
    }
}
