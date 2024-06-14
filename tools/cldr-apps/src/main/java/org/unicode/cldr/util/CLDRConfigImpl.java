package org.unicode.cldr.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONString;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.SurveyLog;
import org.unicode.cldr.web.SurveyMain;
import org.unicode.cldr.web.SurveyMain.Phase;
import org.unicode.cldr.web.UserRegistry;
import org.unicode.cldr.web.WebContext;

/**
 * This is a concrete implementation of CLDRConfig customized for SurveyTool usage. Its main
 * distinction is that it uses the "cldr.properties" file in the server root rather than environment
 * variables.
 */
public class CLDRConfigImpl extends CLDRConfig implements JSONString {
    private static final String CODE_HASH_KEY = "CLDR_CODE_HASH";
    private static final String DATA_HASH_KEY = "CLDR_DATA_HASH";

    private static final Logger logger = SurveyLog.forClass(CLDRConfigImpl.class);

    /**
     * Get an instance and downcast
     *
     * @return
     */
    public static CLDRConfigImpl getInstance() {
        CLDRConfig config = CLDRConfig.getInstance();
        if (config.getEnvironment() == Environment.UNITTEST) {
            throw new RuntimeException(
                    "CLDR_ENVIRONMENT is set to UNITTEST - please correct this (remove any -DCLDR_ENVIRONMENT)");
        }
        try {
            return (CLDRConfigImpl) config;
        } catch (ClassCastException cce) {
            logger.severe(
                    "Error: CLDRConfig.getInstance() returned "
                            + config.getClass().getName()
                            + " initialized at "
                            + config.getInitStack());
            throw new RuntimeException(
                    "CLDRConfig is not a CLDRConfigImpl - probably CLDRConfig.getInstance() was called before "
                            + CLDRConfigImpl.class.getName()
                            + " was available.",
                    cce);
        }
    }

    /** Name of the main config file for the servlet. */
    public static final String CLDR_PROPERTIES = "cldr.properties";

    private static final long serialVersionUID = 7292884997931046214L;

    static String cldrHome = null;
    static boolean cldrHomeSet = false;
    static File homeFile = null;

    /**
     * Notify CLDRConfig that cldrHome is available. This also has the effect of initializing
     * CLDRConfigImpl. Not called within the test environment such as TestAll Called by {@link
     * SurveyMain#init(javax.servlet.ServletConfig)}
     *
     * @param newHome the path to the CLDR Home, usually a subdirectory of the well known servlet
     *     location.
     * @see {@link #getHomeFile()}
     */
    public static void setCldrHome(String newHome) {
        cldrHome = newHome;
        cldrHomeSet = true;

        // Now, initialize it.
        getInstance().init();
    }

    /**
     * Fetches the File pointing at the Survey Tool's home. This is something like
     * /var/lib/tomcat/cldr and is either a subdirectory of the server's home, or some static
     * location. If {@link #setCldrHome(String)} was not called, this will return null. Note that
     * this is not CLDR_DIR, it is unrelated to {@link CLDRConfig#getCldrBaseDirectory()} which is
     * the root of CLDR data.
     *
     * @return
     */
    public File getHomeFile() {
        return homeFile;
        // TODO: we could have a sort of notification queue for services
        // waiting on setCldrHome() being called. For now, we expect the callers
        // to be called at the appropriate time.
    }

    boolean isInitted = false;
    private Properties survprops;
    /**
     * Defaults to SMOKETEST for server
     *
     * @return
     */
    @Override
    protected Environment getDefaultEnvironment() {
        return Environment.SMOKETEST;
    }

    CLDRConfigImpl() {
        final String env = System.getProperty("CLDR_ENVIRONMENT");
        if (env != null && env.equals(Environment.UNITTEST.name())) {
            throw new InternalError("-DCLDR_ENVIRONMENT=" + env + " - exitting!");
        }

        logger.info(getClass().getName() + ".cldrHome=" + cldrHome);
        if (cldrHomeSet == false) {
            logger.warning("*********************************************************************");
            logger.warning("*** CLDRConfig.getInstance() was called prior to SurveyTool setup ***");
            logger.warning("*** This is probably an error. Look at the stack below, and make  ***");
            logger.warning("*** sure that CLDRConfig.getInstance() is not called at static    ***");
            logger.warning("*** init time.                                                    ***");
            logger.warning(
                    "[cldrHome not set] stack=\n"
                            + StackTracker.currentStack()
                            + "\n CLDRHOMESET = "
                            + cldrHomeSet);
        }
    }

    /** Lazy setup of this object. */
    private synchronized void init() {
        if (isInitted) return;
        if (!cldrHomeSet) {
            RuntimeException t =
                    new RuntimeException(
                            "CLDRConfigImpl used before SurveyMain.init() called! (check static ordering).  Set -DCLDR_ENVIRONMENT=UNITTEST if you are in the test cases.");
            // SurveyLog.logException(t);
            throw t;
        }

        survprops = new java.util.Properties();

        // set defaults here
        survprops.put("CLDR_SURVEY_URL", "survey"); // default to relative URL.

        File propFile;

        tryToInitCldrHome();

        logger.info(CLDRConfigImpl.class.getName() + ".init(), cldrHome=" + cldrHome);
        if (cldrHome == null) {
            String homeParent = null;
            // all of these properties specify the "parent", i.e. the parent of a "cldr" dir.
            // Deprecated, these are fallbacks for old behavior.
            String props[] = {
                "cldr.home",
                "catalina.base",
                "websphere.base",
                "catalina.home",
                "websphere.home",
                "user.dir"
            };
            for (String prop : props) {
                if (homeParent == null) {
                    homeParent = System.getProperty(prop);
                    if (homeParent != null) {
                        logger.info(" cldrHome found, using " + prop + " = " + homeParent);
                    } else {
                        logger.warning(" Unset: " + prop);
                    }
                }
            }
            if (homeParent == null) {
                throw new InternalError(
                        "Could not find cldrHome. please set cldr.home, catalina.base, or user.dir, etc");
                // for(Object qq : System.getProperties().keySet()) {
                // logger.warning("  >> "+qq+"="+System.getProperties().get(qq));
                // }
            }
            homeFile = new File(homeParent, "cldr");
            propFile = new File(homeFile, CLDR_PROPERTIES);
            if (!propFile.exists()) {
                logger.warning("Does not exist: " + propFile.getAbsolutePath());
                createBasicCldr(homeFile); // attempt to create
            }
            if (!homeFile.exists()) {
                throw new InternalError(
                        "{SERVER}/cldr isn't working as a CLDR home. Not a directory: "
                                + homeFile.getAbsolutePath());
            }
            cldrHome = homeFile.getAbsolutePath();
        } else {
            homeFile = new File(cldrHome);
            propFile = new File(homeFile, CLDR_PROPERTIES);
        }

        // Set anything that needs access to the homedir here.
        SurveyLog.setDir(homeFile);

        logger.info(
                "CLDRConfig: reading "
                        + propFile.getAbsolutePath()); // make it explicit where this comes from
        // logger.info("SurveyTool starting up. root=" + new
        // File(cldrHome).getAbsolutePath() + " time="+setupTime);

        loadIntoProperties(survprops, propFile);

        // SCM versions.

        // codeHash = the git version of the cldr-code.jar file embedded in cldr-apps.
        String codeHash = getGitHashForSlug(CldrUtility.CODE_SLUG);
        // dataHash = the git version of the CLDR_DIR currently in use.
        String dir = survprops.getProperty(CldrUtility.DIR_KEY, null);
        String dataHash = CldrUtility.getGitHashForDir(dir);
        survprops.put(CODE_HASH_KEY, codeHash);
        survprops.put(DATA_HASH_KEY, dataHash);
        survprops.put(CldrUtility.HOME_KEY, cldrHome);

        isInitted = true;
    }

    private void tryToInitCldrHome() {
        // Try to use org.unicode.cldr.util.CLDRConfigImpl.cldrHome as a property
        // Note that this specifies the entire path, not just the "parent"
        try {
            cldrHome = System.getProperty(CLDRConfigImpl.class.getName() + ".cldrHome", null);
            if (!new File(cldrHome).isDirectory()) {
                logger.warning(cldrHome + " is not a directory");
                cldrHome = null;
            } else {
                cldrHomeSet = true;
            }
        } catch (Throwable t) {
            logger.warning("Error " + t + " trying to set cldrHome");
            t.printStackTrace();
            cldrHome = null;
        }
    }

    public static final String ALL_GIT_HASHES[] = {CODE_HASH_KEY, DATA_HASH_KEY};

    /**
     * Return the git hash for (slug)-Git-Hash.
     *
     * @param slug
     * @return
     */
    public static final String getGitHashForSlug(String slug) {
        //        return Manifests.read(slug + CldrUtility.GIT_COMMIT_SUFFIX);
        try {
            ClassLoader classLoader = CLDRConfigImpl.class.getClassLoader();
            for (final Enumeration<URL> e = classLoader.getResources(JarFile.MANIFEST_NAME);
                    e.hasMoreElements(); ) {
                final URL u = e.nextElement();
                try (InputStream is = u.openStream()) {
                    Manifest mf = new Manifest(is);
                    String name = slug + CldrUtility.GIT_COMMIT_SUFFIX;
                    String s = mf.getMainAttributes().getValue(name);
                    if (s != null && !s.isEmpty()) {
                        return s;
                    }
                } catch (Throwable t) {
                    //                    t.printStackTrace();
                }
            }
        } catch (Throwable t) {
            //            t.printStackTrace();
        }
        return CLDRURLS.UNKNOWN_REVISION;
    }

    /**
     * Load the properties from the given file.
     *
     * <p>Caution: the assumed encoding is ISO8859-1 (latin1) not UTF-8, unless running Java 9 or
     * later. Reference:
     * https://docs.oracle.com/javase/9/intl/internationalization-enhancements-jdk-9.htm
     *
     * @param props
     * @param propFile
     * @throws InternalError
     */
    private void loadIntoProperties(Properties props, File propFile) throws InternalError {
        try (InputStream is = InputStreamFactory.createInputStream(propFile)) {
            props.load(is);
        } catch (IOException ioe) {
            StringBuilder sb = new StringBuilder("Couldn't load ");
            sb.append(propFile.getName());
            sb.append(" file from '");
            sb.append(propFile.getAbsolutePath());
            sb.append("': ");

            logger.log(java.util.logging.Level.SEVERE, sb.toString(), ioe);

            sb.append(ioe.getMessage());
            // append the stacktrace
            sb.append("\r\n");
            sb.append(ioe.getStackTrace());
            InternalError ie = new InternalError(sb.toString(), ioe);
            ioe.printStackTrace();
            throw ie;
        }
    }

    public void writeHelperFile(String hostportpath, File helperFile) throws IOException {
        if (!helperFile.exists()) {
            try (OutputStream file =
                            new BufferedOutputStream(
                                    new FileOutputStream(helperFile, false)); // Append
                    PrintWriter pw = new PrintWriter(file); ) {

                String vap = (String) survprops.get("CLDR_VAP");
                pw.write("<h3>Survey Tool admin interface link</h3>");
                pw.write("To configure the SurveyTool, use ");
                String url0 = hostportpath + "cldr-setup.jsp" + "?vap=" + vap;
                pw.write("<b>SurveyTool Setup:</b>  <a href='" + url0 + "'>" + url0 + "</a><hr>");
                String url = hostportpath + ("AdminPanel.jsp") + "?vap=" + vap;
                pw.write("<b>Admin Panel:</b>  <a href='" + url + "'>" + url + "</a>");
                pw.write(
                        "<hr>if you change the admin password ( CLDR_VAP in config.properties ), please: "
                                + "1. delete this admin.html file 2. restart the server 3. navigate back to the main SurveyTool page.<p>");
            }
        }
    }

    private void createBasicCldr(File homeFile) {
        logger.info("Attempting to create /cldr  dir at " + homeFile.getAbsolutePath());

        try {
            homeFile.mkdir();
            File propsFile = new File(homeFile, CLDR_PROPERTIES);
            try (OutputStream file = new FileOutputStream(propsFile, false); // Append
                    PrintWriter pw = new PrintWriter(file); ) {
                pw.println("## autogenerated cldr.properties config file");
                pw.println("## generated on " + SurveyMain.localhost() + " at " + new Date());
                pw.println(
                        "## see the readme at \n## "
                                + SurveyMain.URL_CLDR
                                + "data/tools/cldr-code/org/unicode/cldr/web/data/readme.txt ");
                pw.println(
                        "## make sure these settings are OK,\n## and comment out CLDR_MESSAGE for normal operation");
                pw.println("##");
                pw.println(
                        "## SurveyTool must be reloaded, or the web server restarted, \n## for these to take effect.");
                pw.println();
                pw.println(
                        "## Put the SurveyTool in setup mode. This enables cldr-setup.jsp?vap=(CLDR_VAP)");
                pw.println("CLDR_MAINTENANCE=true");
                pw.println();
                pw.println(
                        "## your password. Login as user '"
                                + UserRegistry.ADMIN_EMAIL
                                + "' and this password for admin access.");
                pw.println("CLDR_VAP=" + UserRegistry.makePassword());
                pw.println();
                pw.println("## Special Test Enablement.");
                pw.println("CLDR_TESTPW=" + UserRegistry.makePassword());
                pw.println();
                pw.println("## Special message shown to users as to why survey tool is down.");
                pw.println("## Comment out for normal start-up.");
                pw.println("#CLDR_MESSAGE=");
                pw.println();
                pw.println("## Special message shown to users.");
                pw.println(
                        "CLDR_HEADER=Welcome to SurveyTool@"
                                + SurveyMain.localhost()
                                + ". Please edit "
                                + propsFile.getAbsolutePath().replaceAll("\\\\", "/")
                                + " to change CLDR_HEADER (to change this message), or comment it out entirely. Also see "
                                + homeFile.getAbsolutePath()
                                + "/admin.html to get to the admin panel.");
                pw.println();
                pw.println("## Current SurveyTool phase ");
                pw.println("CLDR_PHASE=" + Phase.BETA.name());
                pw.println();
                pw.println("## 'old' (previous) version");
                pw.println("CLDR_OLDVERSION=CLDR_OLDVERSION");
                pw.println();
                pw.println("## 'new'  version");
                pw.println("CLDR_NEWVERSION=CLDR_NEWVERSION");
                pw.println();
                pw.println("## CLDR trunk. Default value shown");
                pw.println(
                        "CLDR_DIR="
                                + homeFile.getAbsolutePath().replaceAll("\\\\", "/")
                                + "/cldr-trunk");
                pw.println();
                pw.println("## SMTP server. Mail is disabled by default.");
                pw.println("#CLDR_SMTP=127.0.0.1");
                pw.println();
                pw.println("# That's all!");
            }
            //            pw.close();
            //            file.close();
        } catch (IOException exception) {
            logger.warning("While writing " + homeFile.getAbsolutePath() + " props: " + exception);
            exception.printStackTrace();
        }
    }

    @Override
    public SupplementalDataInfo getSupplementalDataInfo() {
        init();
        return CookieSession.sm.getSupplementalDataInfo();
    }

    @Override
    public String getProperty(String key) {
        init();
        return survprops.getProperty(key);
    }

    @Override
    public Object setProperty(String key, String value) {
        if (key.equals("CLDR_HEADER")) {
            logger.info(">> CLDRConfig set " + key + " = " + value);
            if (value == null || value.isEmpty()) {
                survprops.setProperty(key, "");
                survprops.remove(key);
                return null;
            } else {
                return survprops.setProperty(key, value);
            }
        } else {
            return null; // Setting is disallowed (silently?)
        }
    }

    @Override
    public String toJSONString() throws JSONException {
        return toJSONObject().toString();
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject ret = new JSONObject();

        ret.put("CLDR_HEADER", ""); // always show these
        for (Entry<Object, Object> e : survprops.entrySet()) {
            ret.put(e.getKey().toString(), e.getValue().toString());
        }

        return ret;
    }

    @Override
    public CheckCLDR.Phase getPhase() {
        return SurveyMain.getOverallSurveyPhase().toCheckCLDRPhase();
    }

    @Override
    public CheckCLDR.Phase getExtendedPhase() {
        return SurveyMain.getOverallExtendedPhase().toCheckCLDRPhase();
    }

    @Override
    public CLDRURLS internalGetUrls() {
        if (contextUrl == null) contextUrl = CLDRURLS.DEFAULT_PATH;
        return new StaticCLDRURLS(this.getProperty(CLDRURLS.CLDR_SURVEY_PATH, contextUrl));
    }

    @Override
    public CLDRURLS internalGetAbsoluteUrls() {
        if (fullUrl == null) fullUrl = CLDRURLS.DEFAULT_BASE;
        return new StaticCLDRURLS(this.getProperty(CLDRURLS.CLDR_SURVEY_BASE, fullUrl));
    }

    private static String contextUrl = null;
    private static String fullUrl = null;

    /**
     * Must call this before using urls() or absoluteUrls()
     *
     * @param fromRequest
     */
    public static final void setUrls(HttpServletRequest fromRequest) {
        if (fullUrl == null) {
            contextUrl = fromRequest.getContextPath();
            fullUrl = WebContext.contextBase(fromRequest);
        }
    }
}
