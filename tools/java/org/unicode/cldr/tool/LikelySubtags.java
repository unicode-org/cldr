package org.unicode.cldr.tool;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.SupplementalDataInfo;

public class LikelySubtags {
    static final boolean DEBUG = true;
    static final String TAG_SEPARATOR = "_";

    private Map<String, String> toMaximized;
    private boolean favorRegion = false;

    /**
     * Create the likely subtags. 
     * @param toMaximized
     */
    public LikelySubtags(Map<String, String> toMaximized) {
        this.toMaximized = toMaximized;
    }

    /**
     * Create the likely subtags. 
     * @param toMaximized
     */
    public LikelySubtags(SupplementalDataInfo supplementalDataInfo) {
        this.toMaximized = supplementalDataInfo.getLikelySubtags();
    }

    /**
     * Create the likely subtags. 
     * @param toMaximized
     */
    public LikelySubtags() {
        this.toMaximized = SupplementalDataInfo.getInstance().getLikelySubtags();
    }

    public boolean isFavorRegion() {
        return favorRegion;
    }

    public LikelySubtags setFavorRegion(boolean favorRegion) {
        this.favorRegion = favorRegion;
        return this;
    }

    public Map<String, String> getToMaximized() {
        return toMaximized;
    }

    public LikelySubtags setToMaximized(Map<String, String> toMaximized) {
        this.toMaximized = toMaximized;
        return this;
    }

    public static String maximize(String languageTag, Map<String, String> toMaximized) {
        return new LikelySubtags(toMaximized).maximize(languageTag);
    }

    public static String minimize(String input, Map<String, String> toMaximized, boolean favorRegion) {
        return new LikelySubtags(toMaximized).setFavorRegion(favorRegion).minimize(input);
    }

    public String maximize(String languageTag) {
        if (languageTag == null) {
            return null;
        }
        LanguageTagParser ltp = new LanguageTagParser();
        if (DEBUG && languageTag.equals("es" + TAG_SEPARATOR + "Hans" + TAG_SEPARATOR + "CN")) {
            System.out.print(""); // debug
        }
        // clean up the input by removing Zzzz, ZZ, and changing "" into und.
        ltp.set(languageTag);
        String language = ltp.getLanguage();
        String region = ltp.getRegion();
        String script = ltp.getScript();
        List<String> variants = ltp.getVariants();
        Map<String, String> extensions = ltp.getExtensions();

        if (language.equals("")) {
            ltp.setLanguage(language = "und");
        }
        if (script.equals("Zzzz")) {
            ltp.setScript(script = "");
        }
        if (region.equals("ZZ")) {
            ltp.setRegion(region = "");
        }
        if (variants.size() != 0) {
            ltp.setVariants(Collections.EMPTY_SET);
        }
        if (extensions.size() != 0) {
            ltp.setExtensions(Collections.EMPTY_MAP);
        }

        // check whole
        String result = toMaximized.get(ltp.toString());
        if (result != null) {
            return ltp.set(result).setVariants(variants).setExtensions(extensions).toString();
        }
        // try empty region
        if (region.length() != 0) {
            result = toMaximized.get(ltp.setRegion("").toString());
            if (result != null) {
                return ltp.set(result).setRegion(region).setVariants(variants).setExtensions(extensions).toString();
            }
            ltp.setRegion(region); // restore
        }
        // try empty script
        if (script.length() != 0) {
            result = toMaximized.get(ltp.setScript("").toString());
            if (result != null) {
                return ltp.set(result).setScript(script).setVariants(variants).setExtensions(extensions).toString();
            }
            // try empty script and region
            if (region.length() != 0) {
                result = toMaximized.get(ltp.setRegion("").toString());
                if (result != null) {
                    return ltp.set(result).setScript(script).setRegion(region).setVariants(variants).setExtensions(extensions).toString();
                }
            }
        }
        //    if (!language.equals("und") && script.length() != 0 && region.length() != 0) {
        //      return languageTag; // it was ok, and we couldn't do anything with it
        //    }
        return null; // couldn't maximize
    }

    public String minimize(String input) {
        String maximized = maximize(input, toMaximized);
        if (maximized == null) {
            return null;
        }
        if (DEBUG && maximized.equals("sr" + TAG_SEPARATOR + "Latn" + TAG_SEPARATOR + "RS")) {
            System.out.print(""); // debug
        }
        LanguageTagParser ltp = new LanguageTagParser().set(maximized);
        String language = ltp.getLanguage();
        String region = ltp.getRegion();
        String script = ltp.getScript();
        // try building up from shorter to longer, and find the first  that matches
        // could be more optimized, but for this code we want simplest
        String[] trials = {language, 
                language + TAG_SEPARATOR + (favorRegion ? region : script), 
                language + TAG_SEPARATOR + (!favorRegion ? region : script)};
        for (String trial : trials) {
            String newMaximized = maximize(trial, toMaximized);
            if (maximized.equals(newMaximized)) {
                return trial;
            }
        }
        return maximized;
    }
}
