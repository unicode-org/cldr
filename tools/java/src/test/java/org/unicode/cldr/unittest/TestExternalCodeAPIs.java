package org.unicode.cldr.unittest;

import org.unicode.cldr.util.CLDRConfig;

import com.ibm.icu.dev.test.TestFmwk;

public class TestExternalCodeAPIs extends TestFmwk {
    static CLDRConfig testInfo = CLDRConfig.getInstance();

    public static void main(String[] args) {
        new TestExternalCodeAPIs().run(args);
    }
}
