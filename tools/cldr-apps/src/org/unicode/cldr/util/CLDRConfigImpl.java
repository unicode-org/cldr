package org.unicode.cldr.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Properties;

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
 * This is a concrete implementation of CLDRConfig customized for SurveyTool usage.
 * Its main distinction is that it uses the "cldr.properties" file in the server root
 * rather than environment variables.
 */
public class CLDRConfigImpl extends CLDRConfig implements JSONString {

    /**
     * Get an instance and downcast
     * @return
     */
    public static CLDRConfigImpl getInstance() {
        CLDRConfig config = CLDRConfig.getInstance();
        if (config.getEnvironment() == Environment.UNITTEST) {
            throw new RuntimeException("CLDR_ENVIRONMENT is set to UNITTEST - please correct this (remove any -DCLDR_ENVIRONMENT)");
        }
        try {
            return (CLDRConfigImpl) config;
        } catch (ClassCastException cce) {
            System.err.println("Error: CLDRConfig.getInstance() returned "
                + config.getClass().getName()
                + " initialized at "
                + config.getInitStack());
            throw new RuntimeException("CLDRConfig is not a CLDRConfigImpl - probably CLDRConfig.getInstance() was called before "
                + CLDRConfigImpl.class.getName() + " was available.", cce);
        }
    }

    public static final String CLDR_PROPERTIES = "cldr.properties";
    /**
     *
     */
    private static final long serialVersionUID = 7292884997931046214L;
    static String cldrHome = null;
    static boolean cldrHomeSet = false;

    public static File homeFile = null;

    boolean isInitted = false;
    private Properties survprops;

    /**
     * Defaults to SMOKETEST for server
     * @return
     */
    protected Environment getDefaultEnvironment() {
        return Environment.SMOKETEST;
    }

    CLDRConfigImpl() {
        // TODO remove this after some time- just warn people about the old message
        final String cwt = System.getProperty("CLDR_WEB_TESTS");
        if (cwt != null && cwt.equals("true")) {
            throw new InternalError(
                "Error: CLDR_WEB_TESTS is obsolete - please set the CLDR_ENVIRONMENT to UNITTEST instead -  ( -DCLDR_ENVIRONMENT=UNITTEST ). Anyways, exitting.");
        }

        final String env = System.getProperty("CLDR_ENVIRONMENT");
        if (env != null && env.equals(Environment.UNITTEST.name())) {
            throw new InternalError("-DCLDR_ENVIRONMENT=" + env + " - exitting!");
        }

        System.err.println(getClass().getName() + ".cldrHome=" + cldrHome);
        if (cldrHomeSet == false) {
            System.err.println("[cldrHome not set] stack=\n" + StackTracker.currentStack() + "\n CLDRHOMESET = " + cldrHomeSet);
        }
    }

    public static void setCldrHome(String initParameter) {
        cldrHome = initParameter;
        cldrHomeSet = true;
    }

    private synchronized void init() {
        if (isInitted)
            return;
        if (!cldrHomeSet) {
            RuntimeException t = new RuntimeException(
                "CLDRConfigImpl used before SurveyMain.init() called! (check static ordering).  Set -DCLDR_ENVIRONMENT=UNITTEST if you are in the test cases.");
            // SurveyLog.logException(t);
            throw t;
        }

        survprops = new java.util.Properties();

        // set defaults here
        survprops.put("CLDR_SURVEY_URL", "survey"); // default to relative URL.

        File propFile;
        System.err.println(CLDRConfigImpl.class.getName() + ".init(), cldrHome=" + cldrHome);
        if (cldrHome == null) {
            String homeParent = null;
            String props[] = { "cldr.home", "catalina.base", "websphere.base", "catalina.home", "websphere.home", "user.dir" };
            for (String prop : props) {
                if (homeParent == null) {
                    homeParent = System.getProperty(prop);
                    if (homeParent != null) {
                        System.err.println(" cldrHome found, using " + prop + " = " + homeParent);
                    } else {
                        System.err.println(" Unset: " + prop);
                    }
                }
            }
            if (homeParent == null) {
                throw new InternalError(
                    "Could not find cldrHome. please set cldr.home, catalina.base, or user.dir, etc");
                // for(Object qq : System.getProperties().keySet()) {
                // SurveyLog.logger.warning("  >> "+qq+"="+System.getProperties().get(qq));
                // }
            }
            homeFile = new File(homeParent, "cldr");
            propFile = new File(homeFile, CLDR_PROPERTIES);
            if (!propFile.exists()) {
                System.err.println("Does not exist: " + propFile.getAbsolutePath());
                createBasicCldr(homeFile); // attempt to create
            }
            if (!homeFile.exists()) {
                throw new InternalError("{SERVER}/cldr isn't working as a CLDR home. Not a directory: "
                    + homeFile.getAbsolutePath());
            }
            cldrHome = homeFile.getAbsolutePath();
        } else {
            homeFile = new File(cldrHome);
            propFile = new File(homeFile, CLDR_PROPERTIES);
        }

        SurveyLog.setDir(homeFile);

        System.out.println("CLDRConfig: reading " + propFile.getAbsolutePath()); // make it explicit where this comes from
        // SurveyLog.logger.info("SurveyTool starting up. root=" + new
        // File(cldrHome).getAbsolutePath() + " time="+setupTime);

        loadIntoProperties(survprops, propFile);
        File currev = new File(homeFile, "currev.properties");
        if (currev.canRead()) {
            loadIntoProperties(survprops, currev);
//            try {
//                java.io.FileInputStream is = new java.io.FileInputStream(currev);
//                survprops.load(is);
//                // progress.update("Loading configuration..");
//                is.close();
//            } catch (java.io.IOException ioe) {
//                /* throw new UnavailableException */
//                InternalError ie = new InternalError("Warning: Couldn't load currev.properties file from '"
//                    + currev.getAbsolutePath() + "' :" + ioe.toString());
//                System.err.println(ie.toString() + ioe.toString());
//                // ioe.printStackTrace();
//                // throw ie;
//            }
        }

        survprops.put("CLDRHOME", cldrHome);

        isInitted = true;
    }

    private void loadIntoProperties(Properties props, File propFile) throws InternalError {
        try (InputStream is = InputStreamFactory.createInputStream(propFile)) {
            props.load(is);
            // progress.update("Loading configuration..");
//            is.close();
        } catch (IOException ioe) {
            /* throw new UnavailableException */
            StringBuilder sb = new StringBuilder("Couldn't load ");
            sb.append(propFile.getName());
            sb.append(" file from '");
            sb.append(propFile.getAbsolutePath());
            sb.append("': ");
            sb.append(ioe.getMessage());
            // append the stacktrace
            sb.append("\r\n");
            sb.append(ioe.getStackTrace());
            InternalError ie = new InternalError(sb.toString());
//            InternalError ie = new InternalError("Couldn't load cldr.properties file from '" + propFile.getAbsolutePath() + "' :"
//                + ioe.toString());
            System.err.println(ie.toString() + ioe.toString());
            ioe.printStackTrace();
            throw ie;
        }
    }

    public void writeHelperFile(String hostportpath, File helperFile) throws IOException {
        if (!helperFile.exists()) {
            try (OutputStream file = new BufferedOutputStream(new FileOutputStream(helperFile, false)); // Append
                PrintWriter pw = new PrintWriter(file);) {

                String vap = (String) survprops.get("CLDR_VAP");
                pw.write("<h3>Survey Tool admin interface link</h3>");
                pw.write("To configure the SurveyTool, use ");
                String url0 = hostportpath + "cldr-setup.jsp" + "?vap=" + vap;
                pw.write("<b>SurveyTool Setup:</b>  <a href='" + url0 + "'>" + url0 + "</a><hr>");
                String url = hostportpath + ("AdminPanel.jsp") + "?vap=" + vap;
                pw.write("<b>Admin Panel:</b>  <a href='" + url + "'>" + url + "</a>");
                pw.write("<hr>if you change the admin password ( CLDR_VAP in config.properties ), please: "
                    + "1. delete this admin.html file 2. restart the server 3. navigate back to the main SurveyTool page.<p>");
            }
//            pw.close();
//            file.close();
        }
    }

    private void createBasicCldr(File homeFile) {
        System.err.println("Attempting to create /cldr  dir at " + homeFile.getAbsolutePath());

        try {
            homeFile.mkdir();
            File propsFile = new File(homeFile, CLDR_PROPERTIES);
            try (OutputStream file = new FileOutputStream(propsFile, false); // Append
                PrintWriter pw = new PrintWriter(file);) {
                pw.println("## autogenerated cldr.properties config file");
                pw.println("## generated on " + SurveyMain.localhost() + " at " + new Date());
                pw.println("## see the readme at \n## " + SurveyMain.URL_CLDR
                    + "data/tools/java/org/unicode/cldr/web/data/readme.txt ");
                pw.println("## make sure these settings are OK,\n## and comment out CLDR_MESSAGE for normal operation");
                pw.println("##");
                pw.println("## SurveyTool must be reloaded, or the web server restarted, \n## for these to take effect.");
                pw.println();
                pw.println("## Put the SurveyTool in setup mode. This enables cldr-setup.jsp?vap=(CLDR_VAP)");
                pw.println("CLDR_MAINTENANCE=true");
                pw.println();
                pw.println("## your password. Login as user 'admin@' and this password for admin access.");
                pw.println("CLDR_VAP=" + UserRegistry.makePassword("admin@"));
                pw.println();
                pw.println("## Special Test Enablement.");
                pw.println("CLDR_TESTPW=" + UserRegistry.makePassword("user@example.com"));
                pw.println();
                pw.println("## Special message shown to users as to why survey tool is down.");
                pw.println("## Comment out for normal start-up.");
                pw.println("#CLDR_MESSAGE=");
                pw.println();
                pw.println("## Special message shown to users.");
                pw.println("CLDR_HEADER=Welcome to SurveyTool@" + SurveyMain.localhost() + ". Please edit "
                    + propsFile.getAbsolutePath().replaceAll("\\\\", "/")
                    + " to change CLDR_HEADER (to change this message), or comment it out entirely. Also see "
                    + homeFile.getAbsolutePath() + "/admin.html to get to the admin panel.");
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
                pw.println("## Current SurveyTool phase ");
                pw.println("CLDR_PHASE=" + Phase.BETA.name());
                pw.println();
                pw.println("## CLDR trunk. Default value shown");
                pw.println("CLDR_DIR=" + homeFile.getAbsolutePath().replaceAll("\\\\", "/") + "/cldr-trunk");
                pw.println();
                pw.println("## SMTP server. Mail is disabled by default.");
                pw.println("#CLDR_SMTP=127.0.0.1");
                pw.println();
                pw.println("## FROM address for mail. Don't be a bad administrator, change this.");
                pw.println("#CLDR_FROM=bad_administrator@" + SurveyMain.localhost());
                pw.println();
                pw.println("# That's all!");
            }
//            pw.close();
//            file.close();
        } catch (IOException exception) {
            System.err.println("While writing " + homeFile.getAbsolutePath() + " props: " + exception);
            exception.printStackTrace();
        }
    }

    public SupplementalDataInfo getSupplementalDataInfo() {
        init();
        return CookieSession.sm.getSupplementalDataInfo();
    }

    public String getProperty(String key) {
        init();
        return survprops.getProperty(key);
    }

    @Override
    public Object setProperty(String key, String value) {
        if (key.equals("CLDR_HEADER")) {
            System.err.println(">> CLDRConfig set " + key + " = " + value);
            if (value == null || value.isEmpty()) {
                survprops.setProperty(key, "");
                survprops.remove(key);
                return null;
            } else {
                return survprops.setProperty(key, value);
            }
        } else {
            return null;
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
        return SurveyMain.phase().getCPhase();
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
     * @param fromRequest
     */
    public static final void setUrls(HttpServletRequest fromRequest) {
        if (fullUrl == null) {
            contextUrl = fromRequest.getContextPath();
            fullUrl = WebContext.contextBase(fromRequest);
        }
    }
}
