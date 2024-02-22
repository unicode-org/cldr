/** Copyright (C) 2011-2013 IBM Corporation and Others. All Rights Reserved. */
// ##header J2SE15

package org.unicode.cldr.unittest.web;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.ibm.icu.dev.test.TestFmwk.TestGroup;
import com.ibm.icu.dev.test.TestLog;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import java.io.File;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.logging.Logger;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRConfig.Environment;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.web.CLDRProgressIndicator;
import org.unicode.cldr.web.DBUtils;
import org.unicode.cldr.web.SurveyLog;

/** Top level test used to run all other tests as a batch. */
public class TestAll extends TestGroup {
    private static final Logger logger = SurveyLog.forClass(TestAll.class);

    private static final String CLDR_TEST_JDBC = TestAll.class.getPackage().getName() + ".jdbcurl";
    private static boolean sane = false;
    private static final boolean DEBUG = CldrUtility.getProperty("DEBUG", false);

    /** True if the Test DB is setup properly. */
    public static final boolean HAVE_TEST_DB =
            !(CldrUtility.getProperty(CLDR_TEST_JDBC, "").isEmpty());

    /** Verify some setup things */
    public static final synchronized void sanity() {
        if (!sane) {
            verifyIsDir(
                    CLDRPaths.BASE_DIRECTORY, CldrUtility.DIR_KEY, "=${workspace_loc:common/..}");
            verifyIsDir(
                    CLDRPaths.MAIN_DIRECTORY,
                    CldrUtility.MAIN_KEY,
                    "=${workspace_loc:common/main}");
            verifyIsFile(new File(CLDRPaths.MAIN_DIRECTORY, "root.xml"));
            sane = true;
        }
    }

    private static void verifyIsFile(File file) {
        if (!file.isFile() || !file.canRead()) {
            throw new IllegalArgumentException("Not a file: " + file.getAbsolutePath());
        }
    }

    private static void verifyIsDir(String f, String string, String sugg) {
        if (f == null) {
            pleaseSet(string, "is null", sugg);
        }
        verifyIsDir(new File(f), string, sugg);
    }

    private static void verifyIsDir(File f, String string, String sugg) {
        if (f == null) {
            pleaseSet(string, "is null", sugg);
        }
        if (!f.isDirectory()) {
            pleaseSet(string, "is not a directory", sugg);
        }
    }

    private static void pleaseSet(String var, String err, String sugg) {
        throw new IllegalArgumentException(
                "Error: variable " + var + " " + err + ", please set -D" + var + sugg);
    }

    public static void main(String[] args) {
        main(args, null);
        /* NOTREACHED */
    }

    public static int main(String[] args, PrintWriter logs) {
        CheckCLDR.setDisplayInformation(CLDRConfig.getInstance().getEnglish());
        args = TestAll.doResetDb(args);
        TestAll test = new TestAll();
        if (logs != null) {
            return test.run(args, logs);
        } else {
            test.run(args);
            /* NOTREACHED */
            return -1; // does not actually return
        }
    }

    public static String[] doResetDb(String[] args) {
        if (CLDRConfig.getInstance().getEnvironment() != Environment.UNITTEST) {
            throw new InternalError(
                    "Error: the CLDRConfig Environment is not UNITTEST. Please set -DCLDR_ENVIRONMENT=UNITTEST");
        }
        return args;
    }

    public TestAll() {
        super(
                new String[] {
                    // use class.getName so we are in sync with name changes and
                    // removals (if not additions)
                    TestIntHash.class.getName(),
                    TestXPathTable.class.getName(),
                    TestMisc.class.getName(),
                    TestUserSettingsData.class.getName(),
                    TestAnnotationVotes.class.getName(),
                    TestUserRegistry.class.getName(),
                },
                "All tests in CLDR Web");
    }

    public static final String CLASS_TARGET_NAME = "CLDR.Web";

    /**
     * @author srl
     */
    public static class WebTestInfo {
        private static WebTestInfo INSTANCE = null;

        private SupplementalDataInfo supplementalDataInfo;
        private StandardCodes sc;
        private Factory cldrFactory;
        private CLDRFile english;
        private CLDRFile root;
        private RuleBasedCollator col;

        public static WebTestInfo getInstance() {
            synchronized (WebTestInfo.class) {
                if (INSTANCE == null) {
                    INSTANCE = new WebTestInfo();
                }
            }
            return INSTANCE;
        }

        private WebTestInfo() {}

        public SupplementalDataInfo getSupplementalDataInfo() {
            synchronized (this) {
                if (supplementalDataInfo == null) {
                    supplementalDataInfo =
                            SupplementalDataInfo.getInstance(CLDRPaths.SUPPLEMENTAL_DIRECTORY);
                }
            }
            return supplementalDataInfo;
        }

        public StandardCodes getStandardCodes() {
            synchronized (this) {
                if (sc == null) {
                    sc = StandardCodes.make();
                }
            }
            return sc;
        }

        public Factory getCldrFactory() {
            synchronized (this) {
                if (cldrFactory == null) {
                    cldrFactory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
                }
            }
            return cldrFactory;
        }

        public CLDRFile getEnglish() {
            synchronized (this) {
                if (english == null) {
                    english = getCldrFactory().make("en", true);
                }
            }
            return english;
        }

        public CLDRFile getRoot() {
            synchronized (this) {
                if (root == null) {
                    root = getCldrFactory().make("root", true);
                }
            }
            return root;
        }

        public Collator getCollator() {
            synchronized (this) {
                if (col == null) {
                    col = (RuleBasedCollator) Collator.getInstance();
                    col.setNumericCollation(true);
                }
            }
            return col;
        }
    }

    static boolean dbSetup = false;

    /** Set up the CLDR db */
    public static synchronized void setupTestDb() {
        if (dbSetup == false) {
            sanity();
            makeDataSource();
            dbSetup = true;
        }
    }

    public static void shutdownDb() throws SQLException {
        setupTestDb();
        DBUtils.getInstance().doShutdown();
    }

    public static final String CORE_TEST_PATH = "cldr_db_test";
    public static final String CLDR_WEBTEST_DIR = TestAll.class.getPackage().getName() + ".dir";
    public static final String CLDR_WEBTEST_DIR_STRING =
            CldrUtility.getProperty(
                    CLDR_WEBTEST_DIR,
                    System.getProperty("user.home") + File.separator + CORE_TEST_PATH);
    public static final File CLDR_WEBTEST_FILE = new File(CLDR_WEBTEST_DIR_STRING);
    static File baseDir = null;

    public static synchronized File getBaseDir() {
        if (baseDir == null) {
            File newBaseDir = CLDR_WEBTEST_FILE;
            if (!newBaseDir.exists()) {
                newBaseDir.mkdir();
            }
            if (!newBaseDir.isDirectory()) {
                throw new IllegalArgumentException(
                        "Bad dir [" + CLDR_WEBTEST_DIR + "]: " + newBaseDir.getAbsolutePath());
            }
            SurveyLog.debug(
                    "Note: using test dir ["
                            + CLDR_WEBTEST_DIR
                            + "]: "
                            + newBaseDir.getAbsolutePath());
            baseDir = newBaseDir;
        }
        return baseDir;
    }

    public static File getDir(String forWhat) {
        return new File(getBaseDir(), forWhat);
    }

    public static File getEmptyDir(String forWhat) {
        return emptyDir(getDir(forWhat));
    }

    public static File emptyDir(File dir) {
        if (DEBUG) System.err.println("Erasing: " + dir.getAbsolutePath());
        if (dir.isDirectory()) {
            File cachedBFiles[] = dir.listFiles();
            if (cachedBFiles != null) {
                for (File f : cachedBFiles) {
                    if (f.isFile()) {
                        f.delete();
                    } else if (f.isDirectory()) {
                        if (f.getAbsolutePath().contains(CORE_TEST_PATH)) {
                            emptyDir(f);
                            f.delete();
                        } else {
                            logger.warning("Please manually remove: " + f.getAbsolutePath());
                        }
                    }
                }
            }
        } else {
            dir.mkdir();
        }
        return dir;
    }

    static void makeDataSource() {
        final String jdbcUrl = CldrUtility.getProperty(CLDR_TEST_JDBC, "");
        System.err.println(CLDR_TEST_JDBC + "=" + jdbcUrl);
        if (!jdbcUrl.isEmpty()) {
            DBUtils.makeInstanceFrom(null, jdbcUrl);
        } else {
            throw new RuntimeException(
                    "Error: set the -DCLDR_TEST_JDBC property to a valid MySQL URL.");
        }
    }

    public static CLDRProgressIndicator getProgressIndicator(TestLog t) {
        final TestLog test = t;
        return new CLDRProgressIndicator() {

            @Override
            public CLDRProgressTask openProgress(String what) {
                return openProgress(what, 0);
            }

            @Override
            public CLDRProgressTask openProgress(String what, int max) {
                final String whatP = what;
                return new CLDRProgressTask() {

                    @Override
                    public void close() {}

                    @Override
                    public void update(int count) {
                        update(count, "");
                    }

                    @Override
                    public void update(int count, String what) {
                        test.logln(whatP + " Update: " + what + ", " + count);
                    }

                    @Override
                    public void update(String what) {
                        update(0, what);
                    }

                    @Override
                    public long startTime() {
                        return 0;
                    }
                };
            }
        };
    }

    public static final boolean skipIfNoDb() {
        if (!HAVE_TEST_DB) {
            System.err.println(
                    "DB tests skipped because -D"
                            + CLDR_TEST_JDBC
                            + " was not set to a MySQL URL.");
            return true;
        }
        return false;
    }

    /**
     * @see #skipIfNoDb() - same but for JUnit
     */
    public static void assumeHaveDb() {
        assumeTrue(
                HAVE_TEST_DB,
                String.format(
                        "DB tests skipped because -D%s was not set to a MySQL URL.",
                        CLDR_TEST_JDBC));
    }
}
