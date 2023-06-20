package org.unicode.cldr.web;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.unicode.cldr.util.CLDRLocale;

/**
 * This class tracks the expected maximum size of strings in the locale.
 *
 * @author srl
 */
public class LocaleMaxSizer {
    public static final int EXEMPLAR_CHARACTERS_MAX = 8192;

    public static final String EXEMPLAR_CHARACTERS = "//ldml/characters/exemplarCharacters";

    Map<CLDRLocale, Map<String, Integer>> sizeExceptions;

    TreeMap<String, Integer> exemplars_prefix = new TreeMap<>();
    Set<CLDRLocale> exemplars_set = new TreeSet<>();

    /** Construct a new sizer. */
    public LocaleMaxSizer() {
        // set up the map
        sizeExceptions = new TreeMap<>();
        exemplars_prefix.put(EXEMPLAR_CHARACTERS, EXEMPLAR_CHARACTERS_MAX);
        String[] locs = {"ja", "ko", "zh", "zh_Hant" /*because of cross-script inheritance*/};
        for (String loc : locs) {
            exemplars_set.add(CLDRLocale.getInstance(loc));
        }
    }

    /**
     * It's expected that this is called with EVERY locale, so we do not recurse into parents.
     *
     * @param l
     */
    public void add(CLDRLocale l) {
        if (l == null) return; // attempt to add null
        CLDRLocale hnr = l.getHighestNonrootParent();
        if (hnr == null) return; // Exit if l is root
        if (exemplars_set.contains(hnr)) { // are we a child of ja, ko, zh?
            sizeExceptions.put(l, exemplars_prefix);
        }
    }

    /**
     * For the specified locale, what is the expected string size?
     *
     * @param locale
     * @param xpath
     * @return
     */
    public int getSize(CLDRLocale locale, String xpath) {
        Map<String, Integer> prefixes = sizeExceptions.get(locale);
        if (prefixes != null) {
            for (Map.Entry<String, Integer> e : prefixes.entrySet()) {
                if (xpath.startsWith(e.getKey())) {
                    return e.getValue();
                }
            }
        }
        return MAX_VAL_LEN;
    }

    /** The max string length accepted of any value. */
    public static final int MAX_VAL_LEN = 4096;
}
