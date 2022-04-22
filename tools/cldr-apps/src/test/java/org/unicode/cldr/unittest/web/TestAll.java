/**
 * Copyright (C) 2011-2013 IBM Corporation and Others. All Rights Reserved.
 */
//##header J2SE15

package org.unicode.cldr.unittest.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRConfig.Environment;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.FileReaders;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.web.CLDRProgressIndicator;
import org.unicode.cldr.web.DBUtils;
import org.unicode.cldr.web.SurveyLog;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.TestFmwk.TestGroup;
import com.ibm.icu.dev.test.TestLog;
import com.ibm.icu.dev.util.ElapsedTimer;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;

/**
 * Top level test used to run all other tests as a batch.
 */
public class TestAll extends TestGroup {
    private static final Logger logger = SurveyLog.forClass(TestAll.class);

    private static final String DB_SUBDIR = "db";
    private static final String CLDR_TEST_JDBC = TestAll.class.getPackage().getName() + ".jdbcurl";
    private static final String CLDR_TEST_KEEP_DB = TestAll.class.getPackage().getName() + ".KeepDb";
    private static final String CLDR_TEST_DISK_DB = TestAll.class.getPackage().getName() + ".DiskDb";
    private static boolean sane = false;
    private static final boolean DEBUG = CldrUtility.getProperty("DEBUG", false);

    /**
     * Verify some setup things
     */
    public static synchronized final void sanity() {
        if (!sane) {
            verifyIsDir(CLDRPaths.BASE_DIRECTORY, CldrUtility.DIR_KEY, "=${workspace_loc:common/..}");
            verifyIsDir(CLDRPaths.MAIN_DIRECTORY, CldrUtility.MAIN_KEY, "=${workspace_loc:common/main}");
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
        throw new IllegalArgumentException("Error: variable " + var + " " + err + ", please set -D" + var + sugg);
    }

    private static final String DERBY_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
    public static final String DERBY_PREFIX = "jdbc:derby:";

    public static void main(String[] args) {
        main(args, null);
        /* NOTREACHED */
    }

    public static int main(String[] args, PrintWriter logs) {
        CheckCLDR.setDisplayInformation(CLDRConfig.getInstance().getEnglish());
        args = TestAll.doResetDb(args);
        TestAll test = new TestAll();
        if(logs != null) {
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
                "Error: the CLDRConfig Environment is not UNITTEST. Please set -DCLDR_ENVIRONMENT=UNITTESTS");
        }
        if (CldrUtility.getProperty(CLDR_TEST_KEEP_DB, false)) {
            if (DEBUG)
                logger.warning("Keeping database..");
        } else {
            if (DEBUG)
                logger.warning("Removing old test database..  set -D" + CLDR_TEST_KEEP_DB
                    + "=true if you want to keep it..");
            File f = getEmptyDir(DB_SUBDIR);
            f.delete();
            if (DEBUG)
                logger.warning("Erased: " + f.getAbsolutePath() + " - now exists=" + f.isDirectory());
        }
        return args;
    }

    public TestAll() {
        super(new String[] {
            // use class.getName so we are in sync with name changes and
            // removals (if not additions)
            TestIntHash.class.getName(),
            TestXPathTable.class.getName(),
            TestMisc.class.getName(),
            TestSTFactory.class.getName(),
            TestUserSettingsData.class.getName(),
            TestAnnotationVotes.class.getName(),
            TestUserRegistry.class.getName(),
        },
            "All tests in CLDR Web");
    }

    public static final String CLASS_TARGET_NAME = "CLDR.Web";

    /**
     *
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

        private WebTestInfo() {
        }

        public SupplementalDataInfo getSupplementalDataInfo() {
            synchronized (this) {
                if (supplementalDataInfo == null) {
                    supplementalDataInfo = SupplementalDataInfo.getInstance(CLDRPaths.SUPPLEMENTAL_DIRECTORY);
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

    /**
     * Set up the CLDR db
     */
    public synchronized static void setupTestDb() {
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
    public static final String CLDR_WEBTEST_DIR_STRING = CldrUtility.getProperty(CLDR_WEBTEST_DIR,
        System.getProperty("user.home") + File.separator + CORE_TEST_PATH);
    public static final File CLDR_WEBTEST_FILE = new File(CLDR_WEBTEST_DIR_STRING);
    static File baseDir = null;

    public synchronized static File getBaseDir() {
        if (baseDir == null) {
            File newBaseDir = CLDR_WEBTEST_FILE;
            if (!newBaseDir.exists()) {
                newBaseDir.mkdir();
            }
            if (!newBaseDir.isDirectory()) {
                throw new IllegalArgumentException("Bad dir [" + CLDR_WEBTEST_DIR + "]: " + newBaseDir.getAbsolutePath());
            }
            SurveyLog.debug("Note: using test dir [" + CLDR_WEBTEST_DIR + "]: " + newBaseDir.getAbsolutePath());
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
        if (DEBUG)
            System.err.println("Erasing: " + dir.getAbsolutePath());
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

    static void initDerby() {
        long start = System.currentTimeMillis();
        try {
            Class.forName(DERBY_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            if (DEBUG)
                System.err.println("Load " + DERBY_DRIVER + " - " + ElapsedTimer.elapsedTime(start));
        }
    }

    static void makeDataSource() {
        final String jdbcUrl = CldrUtility.getProperty(CLDR_TEST_JDBC, "");
        System.err.println(CLDR_TEST_JDBC +"="+jdbcUrl);
        if (!jdbcUrl.isEmpty()) {
            DBUtils.makeInstanceFrom(null, jdbcUrl);
        } else if (CldrUtility.getProperty(CLDR_TEST_DISK_DB, false)) {
            initDerby();
            DBUtils.makeInstanceFrom(setupDerbyDataSource(getDir(DB_SUBDIR)), null);
        } else {
            initDerby();
            DBUtils.makeInstanceFrom(setupDerbyDataSource(null), null);
        }
    }

    // from
    // http://svn.apache.org/viewvc/commons/proper/dbcp/trunk/doc/ManualPoolingDataSourceExample.java?view=co

    private static boolean isSetup = false;

    /**
     * null = inmemory.
     *
     * @param theDir
     * @return
     */
    public static DataSource setupDerbyDataSource(File theDir) {
        org.apache.derby.jdbc.EmbeddedDataSource ds = new EmbeddedDataSource();
        if (theDir != null) {
            ds.setDatabaseName(theDir.getAbsolutePath());
        } else {
            ds.setDatabaseName("memory:sttest");
        }
        if (isSetup == false || (theDir != null && !theDir.exists())) {
            isSetup = true;
            if (theDir != null) {
                if (DEBUG)
                    logger.warning("Using new: " + theDir.getAbsolutePath() + " baseDir = "
                        + getBaseDir().getAbsolutePath());
            }

            ds.setCreateDatabase("create");
        }
        return ds;
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
                    public void close() {

                    }

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

    /**
     * Fetch data from jar
     *
     * @param name
     *            name of thing to load
     *            (org.unicode.cldr.unittest.web.data.name)
     */
    static public BufferedReader getUTF8Data(String name) throws java.io.IOException {
        return FileReaders.openFile(TestAll.class, "data/" + name);
    }

    /**
     * Print a warning and skip if the test is derby-sensitive.
     * Usage:   if(skipIfDerby(this)) return;
     * @return true if skip
     */
    public static boolean skipIfDerby(TestFmwk t) {
        if (DBUtils.getDBKind().equals("Derby")
            && t.logKnownIssue("CLDR-14213", "May not work with Derby")) {
            return true;
        } else {
            return false;
        }
    }
}
