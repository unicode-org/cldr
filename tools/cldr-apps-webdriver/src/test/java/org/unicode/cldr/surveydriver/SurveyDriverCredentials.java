package org.unicode.cldr.surveydriver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SurveyDriverCredentials {
    /**
     * Fictitious domain identifying fictitious email addresses for simulated users like
     * "driver-123@cldr-apps-webdriver.org"
     */
    private static final String EMAIL_AT_DOMAIN = "@cldr-apps-webdriver.org";

    private static final String EMAIL_PREFIX = "driver-";

    /**
     * cldr-apps-webdriver/src/test/resources/org/unicode/cldr/surveydriver/surveydriver.properties
     * -- not in version control; contains a line WEBDRIVER_PASSWORD=...
     */
    private static final String PROPS_FILENAME = "surveydriver.properties";

    private static final String PROPS_PASSWORD_KEY = "WEBDRIVER_PASSWORD";
    private static final String PROPS_URL_KEY = "SURVEYTOOL_URL";

    private static final Object PROPS_WEBDRIVER_KEY = "WEBDRIVER_URL";
    private static final String PROPS_WEBDRIVER_OUTPUT = "WEBDRIVER_OUTPUT";

    private static String webdriverPassword = null;

    private final String email;

    private SurveyDriverCredentials(String email) {
        this.email = email;
    }

    /**
     * Get credentials for logging in as a particular user depending on which Selenium slot we're
     * running on.
     */
    public static SurveyDriverCredentials getForUser(int userIndex) {
        String email = EMAIL_PREFIX + userIndex + EMAIL_AT_DOMAIN;
        return new SurveyDriverCredentials(email);
    }

    public String getEmail() {
        return email;
    }

    private static final class PropsHelper {
        public final Properties props = new java.util.Properties();
        public final File outputDir;

        PropsHelper() {
            if (!tryFromFile()) {
                tryFromResource();
            }
            final String outputPath = props.getOrDefault(PROPS_WEBDRIVER_OUTPUT, "").toString();
            if (!setupOutputDir(outputPath)) {
                System.err.println(
                        "Warning: WEBDRIVER_OUTPUT=" + outputPath + " not set or not usable.");
                // remove this property so it doesn't get used.
                props.remove(PROPS_WEBDRIVER_OUTPUT);
                outputDir = null;
            } else {
                System.out.println("# WEBDRIVER_OUTPUT=" + outputPath);
                outputDir = new File(outputPath);
            }
        }

        boolean tryFromFile() {
            try (final InputStream stream = new FileInputStream(PROPS_FILENAME)) {
                props.load(stream);
                return true;
            } catch (IOException e) {
                System.err.println("While reading " + PROPS_FILENAME + " " + e);
                e.printStackTrace();
                return false;
            }
        }

        void tryFromResource() {
            final InputStream stream =
                    SurveyDriverCredentials.class.getResourceAsStream(PROPS_FILENAME);
            if (stream == null) {
                throw new RuntimeException("File not found: " + PROPS_FILENAME);
            }
            try {
                props.load(stream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        static final PropsHelper INSTANCE = new PropsHelper();
    }

    public static Properties getProperties() {
        return PropsHelper.INSTANCE.props;
    }

    public String getPassword() {
        if (webdriverPassword != null) {
            return webdriverPassword;
        }

        webdriverPassword = (String) getProperties().get(PROPS_PASSWORD_KEY);
        if (webdriverPassword == null || webdriverPassword.isBlank()) {
            throw new RuntimeException("WEBDRIVER_PASSWORD not found in " + PROPS_FILENAME);
        }
        return webdriverPassword;
    }

    public static String getUrl() {
        String host = getProperties().get(PROPS_URL_KEY).toString();
        if (host == null || host.isEmpty()) {
            host = "http://localhost:9080";
        }
        System.out.println(PROPS_URL_KEY + "=" + host);
        return host;
    }

    public static String getWebdriverUrl() {
        String host = getProperties().get(PROPS_WEBDRIVER_KEY).toString();
        if (host == null || host.isEmpty()) {
            host = "http://localhost:4444";
        }
        System.out.println(PROPS_WEBDRIVER_KEY + "=" + host);
        return host;
    }

    public static int getTimeOut() {
        String s = getProperties().get("TIME_OUT_SECONDS").toString();
        if (s == null || s.isEmpty()) {
            s = "60";
        }
        System.out.println("TIME_OUT_SECONDS=" + s);
        return Integer.parseInt(s);
    }

    // output dir

    private static final String LOGS_SUBDIR = "logs/";
    private static final String DATA_SUBDIR = "data/";
    private static final String SCREENSHOTS_SUBDIR = "screenshots/";

    /**
     * @returns true if output dir was setup and cleared
     */
    private static final boolean setupOutputDir(final String dir) {
        if (dir == null || dir.isBlank()) return false;
        final File f = new File(dir);
        if (!f.isDirectory()) return false;

        // make directories ahead of time
        new File(f, LOGS_SUBDIR).mkdirs();
        new File(f, DATA_SUBDIR).mkdirs();
        new File(f, SCREENSHOTS_SUBDIR).mkdirs();

        return true; // OK for now
    }

    /**
     * @returns a FILE with the WEBDRIVER_OUTPUT dir, or null
     */
    public static final File getOutputDir() {
        return PropsHelper.INSTANCE.outputDir;
    }

    /**
     * @returns true if WEBDRIVER_OUTPUT is usable
     */
    public static final boolean haveOutputDir() {
        return getOutputDir() != null;
    }

    /**
     * @returns path to a new subdirectory
     */
    public static final File getOutputDir(String d) {
        if (!haveOutputDir()) return null;

        final File sub = new File(getOutputDir(), d);
        sub.mkdirs(); // always initialize the subdir
        return sub;
    }

    public static final File getDriverLogFile() {
        if (!haveOutputDir()) return null;

        return new File(getOutputDir(LOGS_SUBDIR), "webdriverlog.txt");
    }

    public static final File getDriverSummaryFile() {
        if (!haveOutputDir()) return null;

        return new File(getOutputDir(), "summary.md");
    }
}
