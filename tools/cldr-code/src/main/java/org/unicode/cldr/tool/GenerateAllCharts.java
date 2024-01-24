package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.DateTimeFormats;
import org.unicode.cldr.util.FileCopier;
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
    public static void main(String[] args) throws Exception {
        final File mainDir = new File(CLDRPaths.CHART_DIRECTORY);
        if (mainDir.mkdirs()) {
            System.err.println("Created: " + mainDir);
        }
        if (!mainDir.isDirectory()) {
            throw new IOException("Main dir doesn't exist: " + mainDir);
        }
        FileCopier.copy(GenerateAllCharts.class, "index.css", CLDRPaths.CHART_DIRECTORY);

        FileCopier.copy(
                GenerateAllCharts.class,
                "main-index.html",
                CLDRPaths.CHART_DIRECTORY,
                "index.html");
        FormattedFileWriter.copyIncludeHtmls(CLDRPaths.CHART_DIRECTORY);

        if (ToolConstants.CHART_VERSION.compareTo("37") >= 0) {
            new ChartGrammaticalForms().writeChart(null);
        }

        ShowLanguages.main(args);

        new ChartAnnotations().writeChart(null);
        new ChartSubdivisionNames().writeChart(null);
        GenerateBcp47Text.main(args);
        GenerateSidewaysView.main(args);
        ShowData.main(args);

        // GenerateTransformCharts.main(args);
        ChartDelta.main(args);
        ChartDelta.main(args, true); // churn
        ChartCollation.main(args);

        VerifyCompactNumbers.main(args);
        VerifyZones.main(args);
        DateTimeFormats.main(args);
        new ChartPersonNames().writeChart(null);

        GenerateKeyboardCharts.main(args);
    }
}
