package org.unicode.cldr.web;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRLocale;

public class LocaleNormalizer {
    /**
     * Normalize the given locale-list string, removing invalid/duplicate locale names,
     * and saving error/warning messages in this LocaleNormalizer object
     *
     * @param list the String like "zh  aa test123"
     * @return the normalized string like "aa zh"
     */
    public String normalize(String list) {
        return normalizeLocaleList(list, this);
    }

    /**
     * Normalize the given locale-list string, removing invalid/duplicate locale names
     *
     * Do not report any errors or warnings
     *
     * @param list the String like "zh  aa test123"
     * @return the normalized string like "aa zh"
     */
    public static String normalizeQuietly(String list) {
        return normalizeLocaleList(list, null);
    }

    /**
     * Normalize the given locale-list string, removing invalid/duplicate locale names
     *
     * @param list the String like "zh  aa test123"
     * @param locNorm the object to be filled in with warning/error messages, if not null
     * @return the normalized string like "aa zh"
     */
    private static String normalizeLocaleList(String list, LocaleNormalizer locNorm) {
        if (list == null) {
            return "";
        }
        list = list.trim();
        if (list.isEmpty() || UserRegistry.NO_LOCALES.equals(list)) {
            return "";
        }
        if (UserRegistry.isAllLocales(list)) {
            return UserRegistry.ALL_LOCALES;
        }
        final Set<CLDRLocale> allLocs = SurveyMain.getLocalesSet();
        final CLDRLocale locs[] = UserRegistry.tokenizeCLDRLocale(list);
        final Set<String> set = new TreeSet<>();
        for (CLDRLocale l : locs) {
            if (allLocs.contains(l)) {
                set.add(l.getBaseName());
            } else if (locNorm != null) {
                locNorm.addMessage("Unknown: " + l.getBaseName());
            }
        }
        return String.join(" ", set);
    }

    private ArrayList<String> messages = null;

    private void addMessage(String s) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(s);
    }

    public boolean hasMessage() {
        return messages != null && !messages.isEmpty();
    }

    public String getMessagePlain() {
        return String.join("\n", messages);
    }

    public String getMessageHtml() {
        return String.join("<br />\n", messages);
    }
}
