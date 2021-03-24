package org.unicode.cldr.tool;

import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.DateTimeFormats;
import org.unicode.cldr.util.FileCopier;
import org.unicode.cldr.util.VerifyCompactNumbers;
import org.unicode.cldr.util.VerifyZones;

/**
 * Use -DCHART_VERSION=38.1 (for example) to get a specific version
 * @author markdavis
 *
 */
public class GenerateAllCharts {
    public static void main(String[] args) throws Exception {
        FileCopier.copy(GenerateAllCharts.class, "index.css", CLDRPaths.CHART_DIRECTORY);
        FileCopier.copy(GenerateAllCharts.class, "main-index.html", CLDRPaths.CHART_DIRECTORY, "index.html");
        FormattedFileWriter.copyIncludeHtmls(CLDRPaths.CHART_DIRECTORY);

        ShowLanguages.main(args);

        if (ToolConstants.CHART_VERSION.compareTo("37") >= 0) {
            new ChartGrammaticalForms().writeChart(null);
        }

        new ChartAnnotations().writeChart(null);
        new ChartSubdivisionNames().writeChart(null);
        GenerateBcp47Text.main(args);
        GenerateSidewaysView.main(args);
        ShowData.main(args);
        //GenerateTransformCharts.main(args);
        ShowKeyboards.main(args);
        ChartDelta.main(args);
        ChartDelta.main(args, true); // churn
        ChartCollation.main(args);

        VerifyCompactNumbers.main(args);
        VerifyZones.main(args);
        DateTimeFormats.main(args);
    }
}
