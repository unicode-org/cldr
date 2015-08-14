package org.unicode.cldr.tool;

import org.unicode.cldr.util.DateTimeFormats;
import org.unicode.cldr.util.VerifyCompactNumbers;
import org.unicode.cldr.util.VerifyZones;

public class GenerateAllCharts {
    public static void main(String[] args) throws Exception {
        GenerateBcp47Text.main(args);
        GenerateSidewaysView.main(args);
        ShowData.main(args);
        GenerateTransformCharts.main(args);
        ShowKeyboards.main(args);
        ShowLanguages.main(args);
        ChartDelta.main(args);
        DateTimeFormats.main(args);
        VerifyCompactNumbers.main(args);
        VerifyZones.main(args);
    }
}
