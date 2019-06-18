package org.unicode.cldr.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.test.ExampleGenerator.ExampleContext;
import org.unicode.cldr.test.ExampleGenerator.ExampleType;
import org.unicode.cldr.util.CLDRFile;

public class ExampleBuilder {
    ExampleContext ec_e, ec_n; /* For ExampleType.ENGLISH and ExampleType.NATIVE */
    ExampleGenerator eg_e, eg_n;

    /**
     * CACHING needs to be false until we have a way to invalidate when voting makes it necessary to
     * rebuild examples; maybe implement Listener interface like TestCache
     */
    final private static boolean CACHING = false;
    final private static Map<String, ExampleBuilder> cache = CACHING ? new ConcurrentHashMap<String, ExampleBuilder>() : null;

    /**
     * Get an instance of ExampleBuilder, creating a new one if needed
     *
     * @param englishFile
     * @param englishExample
     * @param cldrFile -- sometimes null!
     * @return the ExampleBuilder
     *
     * Even if the ExampleBuilder constructor is fast, it's better to re-use an existing one when possible,
     * to avoid throwing away and (eventually) re-creating the examples that are cached by ExampleGenerator.
     */
    public static ExampleBuilder getInstance(CLDRFile englishFile, ExampleGenerator englishExample, CLDRFile cldrFile) {
        ExampleBuilder eb = null;
        String cacheKey = null;
        if (CACHING) {
            cacheKey = cldrFile.getLocaleID().toString();
            eb = cache.get(cacheKey);
            if (eb != null) {
                return eb;
            }
        }
        eb = new ExampleBuilder(englishFile, englishExample, cldrFile);
        if (CACHING) {
            if (eb != null) {
                cache.put(cacheKey, eb);
            }
        }
        return eb;
    }

    /**
     * Construct an ExampleBuilder object
     *
     * @param englishFile the English CLDRFile, always the same sm.getTranslationHintsFile()
     * @param englishExample the English ExampleGenerator, always the same sm.getTranslationHintsExample()
     * @param cldrFile -- sometimes null!
     */
    private ExampleBuilder(CLDRFile englishFile, ExampleGenerator englishExample, CLDRFile cldrFile) {
        /*
         * englishPath is always the same.
         * Could store a copy of it locally, but getSupplementalDirectory().getPath() is fast anyway.
         */
        String englishPath = englishFile.getSupplementalDirectory().getPath();
        eg_e = englishExample;
        eg_n = new ExampleGenerator(cldrFile, englishFile, englishPath);
        ec_e = new ExampleContext();
        ec_n = new ExampleContext();
    }

    /**
     * Get an example string, in html, if there is one for this path, otherwise null.
     *
     * @param xpath
     * @param value
     * @param type ExampleType.ENGLISH or ExampleType.NATIVE
     * @return the example HTML, or null
     * 
     * Called by DataRow.toJSONString, with type = ExampleType.ENGLISH,
     * and by CandidateItem.getExample, with type = ExampleType.NATIVE
     */
    synchronized String getExampleHtml(String xpath, String value, ExampleType type) {
        String s;
        if (type == ExampleType.ENGLISH) {
            s = eg_e.getExampleHtml(xpath, value, ec_e, type);
        } else {
            s = eg_n.getExampleHtml(xpath, value, ec_n, type);
        }
        // if(SurveyMain.isUnofficial) System.err.println(s + "  = geh + " +
        // xpath + ", " + value + ", " + zoomed + ", (ec)," + type);
        return s;
    }

    public String getHelpHtml(String xpath, String value) {
        return eg_e.getHelpHtml(xpath, value, true);
    }
}
