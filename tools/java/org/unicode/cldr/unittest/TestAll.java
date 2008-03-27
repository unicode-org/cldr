//##header J2SE15

package org.unicode.cldr.unittest;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.dev.test.TestFmwk.TestGroup;

/**
 * Top level test used to run all other tests as a batch.
 */
public class TestAll extends TestGroup {
  static class TestInfo {
    SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo.getInstance(Utility.SUPPLEMENTAL_DIRECTORY);
    StandardCodes sc = StandardCodes.make();
    Factory cldrFactory = Factory.make(Utility.MAIN_DIRECTORY, ".*");
    CLDRFile english = cldrFactory.make("en", true);
    CLDRFile root = cldrFactory.make("root", true);
  }

  public static void main(String[] args) {
    new TestAll().run(args);
  }

  public TestAll() {
    super(
            new String[] {
                    "org.unicode.cldr.unittest.TestLocale",
                    "org.unicode.cldr.unittest.TestSupplementalInfo",
                    "org.unicode.cldr.unittest.TestPaths",
            },
    "All tests in CLDR");
  }

  public static final String CLASS_TARGET_NAME  = "CLDR";
}
