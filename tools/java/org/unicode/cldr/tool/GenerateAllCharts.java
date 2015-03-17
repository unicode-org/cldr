package org.unicode.cldr.tool;

public class GenerateAllCharts {
    public static void main(String[] args) throws Exception {
        GenerateBcp47Text.main(args);
        GenerateSidewaysView.main(args);
        ShowData.main(args);
        GenerateTransformCharts.main(args);
        ShowKeyboards.main(args);
        ShowLanguages.main(args);
        ChartDelta.main(args);
    }
}
