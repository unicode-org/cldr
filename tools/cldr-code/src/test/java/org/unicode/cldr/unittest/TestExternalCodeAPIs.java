package org.unicode.cldr.unittest;

import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.util.CLDRConfig;

public class TestExternalCodeAPIs extends TestFmwk {
    static CLDRConfig testInfo = CLDRConfig.getInstance();

    public static void main(String[] args) {
        new TestExternalCodeAPIs().run(args);
    }
}
