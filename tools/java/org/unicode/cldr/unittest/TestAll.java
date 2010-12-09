//##header J2SE15

package org.unicode.cldr.unittest;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.CLDRFile.Factory;

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

  public static class TestInfo {
    private static TestInfo INSTANCE = null;
    private SupplementalDataInfo supplementalDataInfo;
    private StandardCodes sc;
    private Factory cldrFactory;
    private CLDRFile english;
    private CLDRFile root;
    private RuleBasedCollator col;

    public static TestInfo getInstance() {
      synchronized (TestInfo.class) {
        if (INSTANCE == null) {
          CldrUtility.checkValidDirectory(CldrUtility.BASE_DIRECTORY, "You have to set -Dcheckdata=<validdirectory>");
          INSTANCE = new TestInfo();
        }
      }
      return INSTANCE;
    }

    private TestInfo() {}

    public SupplementalDataInfo getSupplementalDataInfo() {
      synchronized(this) {
        if (supplementalDataInfo == null) {
          supplementalDataInfo = SupplementalDataInfo.getInstance(CldrUtility.SUPPLEMENTAL_DIRECTORY);
        }
      }
      return supplementalDataInfo;
    }
    public StandardCodes getStandardCodes() {
      synchronized(this) {
        if (sc == null) {
          sc = StandardCodes.make();
        }
      }
      return sc;
    }
    public Factory getCldrFactory() {
      synchronized(this) {
        if (cldrFactory == null) {
          cldrFactory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
        }
      }
      return cldrFactory;
    }
    public CLDRFile getEnglish() {
      synchronized(this) {
        if (english == null) {
          english = getCldrFactory().make("en", true);
        }
      }
      return english;
    }
    public CLDRFile getRoot() {
      synchronized(this) {
        if (root == null) {
          root = getCldrFactory().make("root", true);
        }
      }
      return root;
    }
    public Collator getCollator() {
      synchronized(this) {
        if (col == null) {
          col = (RuleBasedCollator) Collator.getInstance();
          col.setNumericCollation(true);
        }
      }
      return col;
    }
  }

}
