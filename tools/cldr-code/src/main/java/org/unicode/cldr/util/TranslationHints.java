package org.unicode.cldr.util;

import org.unicode.cldr.draft.FileUtilities;

import java.util.*;

public class TranslationHints {

    final static private String HINTS_FILE = "data/translation-hints/hints.txt";

    private static Map<String,String> xpathToHint = null;

    public static String get(String xpath) {
        if (xpathToHint == null) {
            initXpathToHint();
        }
        return xpathToHint.get(xpath);
    }

    private static void initXpathToHint() {
        xpathToHint = new HashMap<>();
        String hint = null;
        for (String line : FileUtilities.in(TranslationHints.class, HINTS_FILE)) {
            if (line.startsWith("HINT:")) {
                hint = line.substring(5).trim();
            } else if (line.startsWith("PATH:")) {
                final String xpath = line.substring(5).trim();
                if (hint == null) {
                    throw new RuntimeException("Bad file, must set HINT before PATH: " + HINTS_FILE);
                }
                xpathToHint.put(xpath, hint);
            }
        }
    }
}
