package org.unicode.cldr.util;


import java.util.Locale;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;

public class CLDRConfig {
    private static CLDRConfig INSTANCE = null;
    public static final String SUBCLASS = CLDRConfig.class.getName()+"Impl";

    public static CLDRConfig getInstance() {
      synchronized (CLDRConfig.class) {
        if (INSTANCE == null) {
            try {
                System.err.println("Attempting to new up a " + SUBCLASS);
                INSTANCE = (CLDRConfig)(Class.forName(SUBCLASS).newInstance());
                System.err.println("Using CLDRConfig: " + INSTANCE.toString() + " - " + INSTANCE.getClass().getName());
            } catch(Throwable t) {
                t.printStackTrace();
                System.err.println("Could not use "+SUBCLASS + " - " + t.toString());
            }
        }
        if(INSTANCE == null) {
          CldrUtility.checkValidDirectory(CldrUtility.BASE_DIRECTORY, "You have to set -Dcheckdata=<validdirectory>");
          INSTANCE = new CLDRConfig();
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
        return result;
    }
}
