package org.unicode.cldr.util;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.unicode.cldr.test.CheckCLDR.Phase;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.TestLog;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;

public class CLDRConfig extends Properties {
    /**
     * 
     */
    private static final long serialVersionUID = -2605254975303398336L;
    public static boolean DEBUG = false;
    private static CLDRConfig INSTANCE = null;
    public static final String SUBCLASS = CLDRConfig.class.getName() + "Impl";

    public enum Environment {
        LOCAL, // < == unknown.
        SMOKETEST, // staging area
        PRODUCTION, // production server!
        UNITTEST // unit test setting
    };

    public static CLDRConfig getInstance() {
        synchronized (CLDRConfig.class) {
            if (INSTANCE == null) {
                try {
                    // System.err.println("Attempting to new up a " + SUBCLASS);
                    INSTANCE = (CLDRConfig) (Class.forName(SUBCLASS).newInstance());
                    System.err.println("Using CLDRConfig: " + INSTANCE.toString() + " - "
                        + INSTANCE.getClass().getName());
                } catch (Throwable t) {
                    // t.printStackTrace();
                    // System.err.println("Could not use "+SUBCLASS + " - " + t.toString() +
                    // " - falling back to parent");
                }
            }
            if (INSTANCE == null) {
                INSTANCE = new CLDRConfig();
                CldrUtility.checkValidDirectory(INSTANCE.getProperty("CLDR_DIR"),
                    "You have to set -DCLDR_DIR=<validdirectory>");
            }
        }
        return INSTANCE;
    }

    protected CLDRConfig() {
    }

    private SupplementalDataInfo supplementalDataInfo;
    private StandardCodes sc;
    private Factory cldrFactory;
    private Factory supplementalFactory;
    private CLDRFile english;
    private CLDRFile root;
    private RuleBasedCollator col;
    private Phase phase = null; // default

    private TestLog testLog = null;

    // base level
    public TestLog setTestLog(TestLog log) {
        testLog = log;
        return log;
    }

    // for calling "run"
    public TestFmwk setTestLog(TestFmwk log) {
        testLog = log;
        return log;
    }

    protected void logln(String msg) {
        if (testLog != null) {
            testLog.logln(msg);
        } else {
            System.out.println(msg);
            System.out.flush();
        }
    }

    public SupplementalDataInfo getSupplementalDataInfo() {
        synchronized (this) {
            if (supplementalDataInfo == null) {
                supplementalDataInfo = SupplementalDataInfo.getInstance(CldrUtility.DEFAULT_SUPPLEMENTAL_DIRECTORY);
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
                cldrFactory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
            }
        }
        return cldrFactory;
    }

    public Factory getSupplementalFactory() {
        synchronized (this) {
            if (supplementalFactory == null) {
                supplementalFactory = Factory.make(CldrUtility.DEFAULT_SUPPLEMENTAL_DIRECTORY, ".*");
            }
        }
        return supplementalFactory;
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

    public synchronized Phase getPhase() {
        if (phase == null) {
            if (getEnvironment() == Environment.UNITTEST) {
                phase = Phase.BUILD;
            } else {
                phase = Phase.SUBMISSION;
            }
        }
        return phase;
    }

    @Override
    public String getProperty(String key, String d) {
        String result = getProperty(key);
        if (result == null) return d;
        return result;
    }

    private Set<String> shown = new HashSet<String>();

    private Map<String, String> localSet = null;

    @Override
    public String get(Object key) {
        return getProperty(key.toString());
    }

    @Override
    public String getProperty(String key) {
        String result = null;
        if (localSet != null) {
            result = localSet.get(key);
        }
        if (result == null) {
            result = System.getProperty(key);
        }
        if (result == null) {
            result = System.getProperty(key.toUpperCase(Locale.ENGLISH));
        }
        if (result == null) {
            result = System.getProperty(key.toLowerCase(Locale.ENGLISH));
        }
        if (result == null) {
            result = System.getenv(key);
        }
        if (DEBUG && !shown.contains(key)) {
            logln("-D" + key + "=" + result);
            shown.add(key);
        }
        return result;
    }

    private Environment curEnvironment = null;

    public Environment getEnvironment() {
        if (curEnvironment == null) {
            String envString = getProperty("CLDR_ENVIRONMENT");
            if (envString != null) {
                curEnvironment = Environment.valueOf(envString.trim());
            }
            if (curEnvironment == null) {
                curEnvironment = Environment.LOCAL;
            }
        }
        return curEnvironment;
    }

    public void setEnvironment(Environment environment) {
        curEnvironment = environment;
    }

    /**
     * For test use only. Will throw an exception in non test environments.
     * @param k
     * @param v
     * @return 
     */
    @Override
    public Object setProperty(String k, String v) {
        if (getEnvironment() != Environment.UNITTEST) {
            throw new InternalError("setProperty() only valid in UNITTEST Environment.");
        }
        if (localSet == null) {
            localSet = new ConcurrentHashMap<String, String>();
        }
        shown.remove(k); // show it again with -D
        return localSet.put(k, v);
    }

    @Override
    public Object put(Object k, Object v) {
        return setProperty(k.toString(), v.toString());
    }

    /**
     * Return true if the value indicates 'true'
     * @param k key
     * @param defVal default value
     * @return
     */
    public boolean getProperty(String k, boolean defVal) {
        String val = getProperty(k, defVal ? "true" : null);
        if (val == null) {
            return false;
        } else {
            val = val.trim().toLowerCase();
            return (val.equals("true") || val.equals("t") || val.equals("yes") || val.equals("y"));
        }
    }

    /**
     * Return a numeric property
     * @param k key
     * @param defVal default value
     * @return
     */
    public int getProperty(String k, int defVal) {
        String val = getProperty(k, Integer.toString(defVal));
        if (val == null) {
            return defVal;
        } else {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException nfe) {
                return defVal;
            }
        }
    }
}
