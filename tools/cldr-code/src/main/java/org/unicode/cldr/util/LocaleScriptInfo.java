package org.unicode.cldr.util;

import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import java.util.Map;
import java.util.Set;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.ExemplarSets.ExemplarType;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData.Type;

public class LocaleScriptInfo {
    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();

    public static String UNKNOWN_SCRIPT = UScript.getShortName(UScript.UNKNOWN);
    public static String UNKNOWN_REGION = "ZZ";

    private static SupplementalDataInfo supplementalData = CLDR_CONFIG.getSupplementalDataInfo();
    private static Factory factory = CLDR_CONFIG.getCldrFactory();

    /**
     * Get the script code (aka short property name, like Latn) from the locale, or if that fails,
     * from the supplemental languageData.
     *
     * @param locale
     * @return null if fails
     */
    public static String getScriptFromLocaleOrSupplemental(String locale) {
        String script = getScriptFromLocale(locale);
        if (script == null) {
            script = getScriptFromSupplementalData(locale);
        }
        return script;
    }

    /**
     * Get the script code (aka short property name, like Latn) from the locale id, or if that
     * fails, from the main exemplars.
     *
     * @param locale
     * @return null if fails
     */
    public static String getScriptFromLocale(String locale) {
        // if the script is in the locale ID, return it

        CLDRLocale cLocale = CLDRLocale.getInstance(locale);
        String script = cLocale.getScript();
        if (!script.isEmpty()) {
            return script;
        }

        // Otherwise, check the main exemplars

        try {
            CLDRFile cldrFile = factory.make(locale, true);
            UnicodeSet exemplars =
                    cldrFile.getExemplarSet(ExemplarType.main, WinningChoice.WINNING);
            script = getExemplarScriptCode(exemplars);
            if (!script.equals(UNKNOWN_SCRIPT)) {
                return script;
            }
        } catch (RuntimeException e) {
            // we failed for some reason
        }

        return null;
    }

    /**
     * Get the script code (aka short property name, like Latn) from the supplemental languageData.
     * Take the first one if there are 2.
     *
     * @param locale
     * @return null if fails
     */
    public static String getScriptFromSupplementalData(String locale) {
        final Map<Type, BasicLanguageData> basicLanguageData =
                supplementalData.getBasicLanguageDataMap(locale);
        if (basicLanguageData != null) {
            String result = null;
            for (BasicLanguageData datum : basicLanguageData.values()) {
                final Set<String> scripts = datum.getScripts();
                boolean isPrimary = datum.getType() == BasicLanguageData.Type.primary;
                if (scripts.isEmpty()) {
                    continue;
                }
                String script = scripts.iterator().next();
                if (isPrimary) {
                    return result = script;
                } else if (result == null) {
                    result = script;
                }
            }
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /**
     * Get the first explicit script code from a UnicodeSet, which should be a locale's main
     * exemplars
     *
     * @param unicodeSet
     * @return Zzzz = Unknown Script if fails.
     */
    public static String getExemplarScriptCode(UnicodeSet unicodeSet) {
        return UScript.getShortName(getExemplarUScriptId(unicodeSet));
    }

    /**
     * Get the first explicit script code from a UnicodeSet, which should be a locale's main
     * exemplars
     *
     * @param unicodeSet
     * @return UScript.UNKNOWN if fails
     */
    public static int getExemplarUScriptId(UnicodeSet unicodeSet) {
        for (UnicodeSetIterator it = new UnicodeSetIterator(unicodeSet); it.next(); ) {
            if (it.codepoint != UnicodeSetIterator.IS_STRING) {
                int script = UScript.getScript(it.codepoint);
                switch (script) {
                    case UScript.COMMON:
                    case UScript.INHERITED:
                    case UScript.UNKNOWN:
                        continue;
                    default:
                        return script;
                }
            } else {
                int cp;
                for (int i = 0; i < it.string.length(); i += UTF16.getCharCount(cp)) {
                    int script = UScript.getScript(cp = UTF16.charAt(it.string, i));
                    switch (script) {
                        case UScript.COMMON:
                        case UScript.INHERITED:
                        case UScript.UNKNOWN:
                            continue;
                        default:
                            return script;
                    }
                }
            }
        }
        return UScript.UNKNOWN;
    }
}
