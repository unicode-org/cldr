package org.unicode.cldr.tool;

import java.io.IOException;

public class GenerateTestData {
    // TODO Flesh this out. See https://unicode-org.atlassian.net/browse/CLDR-14186
    public static void main(String[] args) throws IOException {
        GenerateLocaleIDTestData.main(args);
        GenerateLikelyTestData.main(args);
        GeneratePersonNameTestData.main(args);
        System.out.println(
                "SEE GenerateTestData.md\n"
                        + "There is still a manual step: (CLDR-14186)\n"
                        + "Manually run TestUnits with -DTestUnits:GENERATE_TESTS. It will create files in place.");
    }
}
