package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.DateTimeFormats;
import org.unicode.cldr.util.FileCopier;
import org.unicode.cldr.util.StackTracker;
import org.unicode.cldr.util.VerifyCompactNumbers;
import org.unicode.cldr.util.VerifyZones;

/**
 * Use -DCHART_VERSION=38.1 (for example) to get a specific version Plus options -DCHART_STATUS=beta
 * (default, uses trunk, calls it β) -DCHART_STATUS=trunk (uses trunk, no β. Used at the end of the
 * release, but before the final data is in cldr-archive) -DCHART_STATUS=release (only uses the
 * cldr-archive, no β)
 *
 * @author markdavis
 */
public class GenerateAllCharts {
    /** print an eyecatcher with the line number of this stage of GenerateAllCharts */
    private static void headline(String str) {
        StackTraceElement caller = StackTracker.currentElement(0);
        System.out.println();
        System.out.println("---------------------------");
        System.out.println(caller.getFileName() + ":" + caller.getLineNumber() + "  " + str);
    }

    public static void main(String[] args) throws Exception {
        headline("Setting Up");
        final File mainDir = new File(CLDRPaths.CHART_DIRECTORY);
        System.out.println("Writing to " + mainDir.getAbsolutePath());
        if (mainDir.mkdirs()) {
            System.err.println("Created: " + mainDir);
        }
        if (!mainDir.isDirectory()) {
            throw new IOException("Main dir doesn't exist: " + mainDir);
        }

        headline("Copying LICENSE, README.md, css, etc.");
        FileCopier.copy(CldrUtility.class, "data/LICENSE", CLDRPaths.CHART_DIRECTORY, "LICENSE");
        FileCopier.copy(
                GenerateAllCharts.class,
                "README-CHARTS.md",
                CLDRPaths.CHART_DIRECTORY,
                "README.md");
        FileCopier.copy(GenerateAllCharts.class, "index.css", CLDRPaths.CHART_DIRECTORY);

        FileCopier.copy(
                GenerateAllCharts.class,
                "main-index.html",
                CLDRPaths.CHART_DIRECTORY,
                "index.html");

        headline("Setting Up include htmls");
        FormattedFileWriter.copyIncludeHtmls(CLDRPaths.CHART_DIRECTORY);

        if (ToolConstants.CHART_VERSION.compareTo("37") >= 0) {
            headline("ChartGrammaticalForms");
            new ChartGrammaticalForms().writeChart(null);
        }

        headline("ShowLanguages");
        ShowLanguages.main(args);

        headline("ChartAnnotations");
        new ChartAnnotations().writeChart(null);
        headline("ChartSubdivisionNames");
        new ChartSubdivisionNames().writeChart(null);
        headline("GenerateBcp47Text");
        GenerateBcp47Text.main(args);
        headline("GenerateSidewaysView");
        GenerateSidewaysView.main(args);
        headline("ShowData");
        ShowData.main(args);

        // headline("GenerateTransformCharts");
        // GenerateTransformCharts.main(args);
        headline("ChartDelta");
        ChartDelta.main(args);
        headline("ChartDelta - high level");
        ChartDelta.main(args, true); // churn
        headline("ChartCollation");
        ChartCollation.main(args);

        headline("VerifyCompactNumbers");
        VerifyCompactNumbers.main(args);
        headline("VerifyZones");
        VerifyZones.main(args);
        headline("DateTimeFormats");
        DateTimeFormats.main(args);
        headline("ChartPersonNames");
        new ChartPersonNames().writeChart(null);

        headline("GenerateKeyboardCharts");
        GenerateKeyboardCharts.main(args);
        headline("DONE");
    }
}
