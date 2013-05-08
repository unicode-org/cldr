package org.unicode.cldr.tool;

public class GenerateAllCharts {
    public static void main(String[] args) throws Exception {
        String[] empty = new String[0];
        ShowLanguages.main(empty);
        GenerateBcp47Text.main(empty);
        GenerateSidewaysView.main(empty);
        ShowData.main(empty);
        GenerateTransformCharts.main(empty);
        ShowKeyboards.main(empty);
    }
}
