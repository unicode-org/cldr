package org.unicode.cldr.tool;

import java.io.IOException;

public class GenerateTestData {
    public static void main(String[] args) throws IOException {
        GenerateLocaleIDTestData.main(args);
        GenerateLikelyTestData.main(args);
        GeneratePersonNameTestData.main(args);
        GenerateUnitTestData.main(args);
    }
}
