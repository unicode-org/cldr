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
    private static TestInfo INSTANCE = null;
    private SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo.getInstance(Utility.SUPPLEMENTAL_DIRECTORY);
    private StandardCodes sc = StandardCodes.make();
    private Factory cldrFactory = Factory.make(Utility.MAIN_DIRECTORY, ".*");
    private CLDRFile english = getCldrFactory().make("en", true);
    private CLDRFile root = getCldrFactory().make("root", true);
    
    public static TestInfo getInstance() {
      synchronized (TestInfo.class) {
        if (INSTANCE == null) {
          INSTANCE = new TestInfo();
        }
      }
      return INSTANCE;
    }
    
    private TestInfo() {}
    
    public SupplementalDataInfo getSupplementalDataInfo() {
      return supplementalDataInfo;
    }
    public Factory getCldrFactory() {
      return cldrFactory;
    }
    public StandardCodes getStandardCodes() {
      return sc;
    }
    public void setEnglish(CLDRFile english) {
      this.english = english;
    }
    public CLDRFile getEnglish() {
      return english;
    }
    public void setRoot(CLDRFile root) {
      this.root = root;
    }
    public CLDRFile getRoot() {
      return root;
    }
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
                    "org.unicode.cldr.unittest.TestExternalCodeAPIs",
                    
            },
    "All tests in CLDR");
  }

  public static final String CLASS_TARGET_NAME  = "CLDR";
}
