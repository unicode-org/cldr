//##header J2SE15

package org.unicode.cldr.unittest;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.ibm.icu.dev.test.TestFmwk.TestGroup;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;

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
                    "org.unicode.cldr.unittest.TestCanonicalIds",
                    "org.unicode.cldr.unittest.TestLocale",
                    "org.unicode.cldr.unittest.TestBasic",
                    "org.unicode.cldr.unittest.TestSupplementalInfo",
                    "org.unicode.cldr.unittest.TestPaths",
                    "org.unicode.cldr.unittest.TestExternalCodeAPIs",
                    "org.unicode.cldr.unittest.TestMetadata",
                    //"org.unicode.cldr.unittest.TestUtilities",
                    "org.unicode.cldr.unittest.NumeringSystemsTest",
                    "org.unicode.cldr.unittest.TestCheckCLDR",
            },
    "All tests in CLDR");
  }

  public static final String CLASS_TARGET_NAME  = "CLDR";

  public static class TestInfo extends CLDRConfig {
    private static TestInfo INSTANCE = null;

    public static TestInfo getInstance() {
      synchronized (TestInfo.class) {
        if (INSTANCE == null) {
          CldrUtility.checkValidDirectory(CldrUtility.BASE_DIRECTORY, "You have to set -Dcheckdata=<validdirectory>");
          INSTANCE = new TestInfo();
        }
      }
      return INSTANCE;
    }


  }

}
