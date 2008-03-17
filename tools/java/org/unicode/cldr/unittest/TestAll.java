//##header J2SE15

package org.unicode.cldr.unittest;

import com.ibm.icu.dev.test.TestFmwk.TestGroup;

/**
 * Top level test used to run all other tests as a batch.
 */
public class TestAll extends TestGroup {

    public static void main(String[] args) {
        new TestAll().run(args);
    }

    public TestAll() {
        super(
              new String[] {
                  "org.unicode.cldr.unittest.TestLocale",
              },
              "All tests in CLDR");
    }

    public static final String CLASS_TARGET_NAME  = "CLDR";
}
