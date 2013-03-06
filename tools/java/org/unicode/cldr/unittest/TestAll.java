//##header J2SE15

package org.unicode.cldr.unittest;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CldrUtility;

import com.ibm.icu.dev.test.TestFmwk.TestGroup;
import com.ibm.icu.util.VersionInfo;

/**
 * Top level test used to run all other tests as a batch.
 */
public class TestAll extends TestGroup {

    public static void main(String[] args) {
        CLDRConfig.getInstance().setTestLog(new TestAll()).run(args);
    }

    public TestAll() {
        super(
            new String[] {
                "org.unicode.cldr.unittest.TestCanonicalIds",
                "org.unicode.cldr.unittest.TestDisplayAndInputProcessor",
                "org.unicode.cldr.unittest.TestLocalCurrency",
                "org.unicode.cldr.unittest.TestLocale",
                "org.unicode.cldr.unittest.TestBasic",
                "org.unicode.cldr.unittest.TestSupplementalInfo",
                "org.unicode.cldr.unittest.TestPaths",
                "org.unicode.cldr.unittest.TestPathHeader",
                "org.unicode.cldr.unittest.TestExternalCodeAPIs",
                "org.unicode.cldr.unittest.TestMetadata",
                "org.unicode.cldr.unittest.TestUtilities",
                "org.unicode.cldr.unittest.NumberingSystemsTest",
                "org.unicode.cldr.unittest.StandardCodesTest",
                "org.unicode.cldr.unittest.TestCheckCLDR",
                "org.unicode.cldr.unittest.TestInheritance",
            },
            "All tests in CLDR");
    }

    public static final String CLASS_TARGET_NAME = "CLDR";

    public static class TestInfo extends CLDRConfig {
        private static TestInfo INSTANCE = null;

        public static TestInfo getInstance() {
            synchronized (TestInfo.class) {
                if (INSTANCE == null) {
                    CldrUtility.checkValidDirectory(CldrUtility.BASE_DIRECTORY,
                        "You have to set -Dcheckdata=<validdirectory>");
                    INSTANCE = new TestInfo();
                }
            }
            return INSTANCE;
        }

        public static boolean isCldrVersionBefore(int... version) {
            return TestInfo.getInstance().getEnglish().getDtdVersionInfo().compareTo(getVersion(version)) < 0;
        }

        public static VersionInfo getVersion(int... versionInput) {
            int[] version = new int[4];
            for (int i = 0; i < versionInput.length; ++i) {
                version[i] = versionInput[i];
            }
            return VersionInfo.getInstance(version[0], version[1], version[2], version[3]);
        }
    }
}
