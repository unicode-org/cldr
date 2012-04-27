package org.unicode.cldr.util;


import java.util.Locale;

import org.unicode.cldr.util.CLDRConfig.Environment;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;

public class CLDRConfig {
    private static CLDRConfig INSTANCE = null;
    public static final String SUBCLASS = CLDRConfig.class.getName()+"Impl";
    public enum Environment { 
            LOCAL,  //< == unknown.
            SMOKETEST,  // staging area
             PRODUCTION // production server!
    };

    public static CLDRConfig getInstance() {
      synchronized (CLDRConfig.class) {
        if (INSTANCE == null) {
            try {
                //System.err.println("Attempting to new up a " + SUBCLASS);
                INSTANCE = (CLDRConfig)(Class.forName(SUBCLASS).newInstance());
                System.err.println("Using CLDRConfig: " + INSTANCE.toString() + " - " + INSTANCE.getClass().getName());
            } catch(Throwable t) {
                //t.printStackTrace();
                //System.err.println("Could not use "+SUBCLASS + " - " + t.toString() + " - falling back to parent");
            }
        }
        if(INSTANCE == null) {
          INSTANCE = new CLDRConfig();
          CldrUtility.checkValidDirectory(INSTANCE.getProperty("CLDR_DIR"), "You have to set -DCLDR_DIR=<validdirectory>");
        }
      }
      return INSTANCE;
    }

    protected CLDRConfig() {}

    private SupplementalDataInfo supplementalDataInfo;
    private StandardCodes sc;
    private Factory cldrFactory;
    private CLDRFile english;
    private CLDRFile root;
    private RuleBasedCollator col;

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

    public String getProperty(String key, String d) {
        String result = getProperty(key);
        if(result==null) return d;
        return result;
    }
    
    public String getProperty(String key) {
        String result = System.getProperty(key);
        if (result == null) {
          result = System.getProperty(key.toUpperCase(Locale.ENGLISH));
        }
        if (result == null) {
          result = System.getProperty(key.toLowerCase(Locale.ENGLISH));
        }
        if (result == null) {
          result = System.getenv(key);
        }
        System.out.println("-D" + key + "=" + result);
        System.out.flush();
        return result;
    }

    private Environment curEnvironment = null;
    
    public Environment getEnvironment() {
        if(curEnvironment==null) {
            String envString = getProperty("CLDR_ENVIRONMENT");
            if(envString!=null) {
                curEnvironment = Environment.valueOf(envString.trim());
            }
            if(curEnvironment==null) {
                curEnvironment = Environment.LOCAL;
            }
        }
        return curEnvironment;
    }

    public void setEnvironment(Environment environment) {
        curEnvironment = environment;
    }
}
