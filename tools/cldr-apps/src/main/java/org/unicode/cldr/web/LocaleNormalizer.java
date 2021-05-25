package org.unicode.cldr.web;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRLocale;

/**
 * Normalize and validate sets of locales. This class was split off from UserRegistry.java with
 * the goal of encapsulation to support refactoring and implementation of new features such as
 * warning a Manager who tries to assign to a Vetter unknown locales or locales that are not
 * covered by their organization.
 *
 * CLDR employs a variety of representations and data types for locales and sets of locales.
 *
 * A single locale may be represented by a string like "fr_CA" for Canadian French, or by
 * a CLDRLocale object.
 *
 * A set of locales related to a particular Survey Tool user is compactly represented by a single string
 * like "am fr_CA zh" (meaning "Amharic, Canadian French, and Chinese"). Survey Tool uses this compact
 * representation for storage in the user database, and for browser inputting/editing forms, etc.
 *
 * A set of locales related to a particular organization is stored primarily in Locales.txt, which
 * is read by Survey Tool at start-up and loaded into a set of strings:
 *
 *   final Set<String> orgLocaleSet = StandardCodes.make().getLocaleCoverageLocales(org);
 *
 * In addition, arrays and sets (HashSet/TreeSet) of locale strings or CLDRLocale objects are used:
 *   String[]
 *   CLDRLocale[]
 *   Set<String> localesSet = new HashSet<>()
 *   Set<CLDRLocale> res = new HashSet<>()
 *   Set<CLDRLocale> s = new TreeSet<>()
 *
 * The code should be made simpler and faster by consistently using a smaller variety of representations
 * of locale sets, and reducing the amount of conversion between them.
 */
public class LocaleNormalizer {

    /**
     * Special constant for specifying access to no locales. Used with intlocs (not with locale access)
     */
    public static final String NO_LOCALES = "none";

    /**
     * Special constant for specifying access to all locales.
     */
    public static final String ALL_LOCALES = "*";

    public static boolean isAllLocales(String localeList) {
        return (localeList != null) && (localeList.contains(ALL_LOCALES) || localeList.trim().equals("all"));
    }

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
        if (list.isEmpty() || NO_LOCALES.equals(list)) {
            return "";
        }
        if (isAllLocales(list)) {
            return ALL_LOCALES;
        }
        final Set<CLDRLocale> allLocs = SurveyMain.getLocalesSet();
        final CLDRLocale locs[] = tokenizeCLDRLocale(list);
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

    /**
     * Tokenize a list, and validate it against actual locales
     *
     * @param localeList the input string like "aa zh"
     * @return the Set of CLDRLocale objects
     */
    public static Set<CLDRLocale> tokenizeValidCLDRLocale(String localeList) {
        if (isAllLocales(localeList)) {
            throw new IllegalArgumentException("Don't call this function with '" + ALL_LOCALES + "' - " + localeList);
        }
        Set<CLDRLocale> s = new TreeSet<>();
        if (localeList == null || isAllLocales(localeList)) {
            return s; // empty
        }

        Set<CLDRLocale> allLocs = SurveyMain.getLocalesSet();
        CLDRLocale locs[] = tokenizeCLDRLocale(localeList);
        for (CLDRLocale l : locs) {
            if (!allLocs.contains(l)) {
                continue;
            }
            s.add(l);
        }
        return s;
    }

    /**
     * Tokenize a string, and return an array of CLDRLocales
     *
     * @param localeList the input string like "aa zh"
     * @return the Array of CLDRLocales
     */
    private static CLDRLocale[] tokenizeCLDRLocale(String localeList) {
        if (isAllLocales(localeList)) {
            throw new IllegalArgumentException("Don't call this function with '" + ALL_LOCALES + "' - " + localeList);
        }
        if ((localeList == null) || ((localeList = localeList.trim()).length() == 0)) {
            return new CLDRLocale[0];
        }

        String s[] = tokenizeLocale(localeList);
        CLDRLocale l[] = new CLDRLocale[s.length];
        for (int j = 0; j < s.length; j++) {
            l[j] = CLDRLocale.getInstance(s[j]);
        }
        return l;
    }

    /*
     * Split into an array. Will return {} if no locales match
     */
    public static String[] tokenizeLocale(String localeList) {
        if (isAllLocales(localeList)) {
            throw new IllegalArgumentException("Don't call this function with '" + ALL_LOCALES + "' - " + localeList);
        }
        if ((localeList == null) || ((localeList = localeList.trim()).length() == 0)) {
            return new String[0];
        }
        final String LOCALE_PATTERN = "[, \t\u00a0\\s]+"; // whitespace
        return localeList.trim().split(LOCALE_PATTERN);
    }

    public static boolean localeMatchesLocaleList(String localeArray[], CLDRLocale locale) {
        return localeMatchesLocaleList(UserRegistry.stringArrayToLocaleArray(localeArray), locale);
    }

    private static boolean localeMatchesLocaleList(CLDRLocale localeArray[], CLDRLocale locale) {
        for (CLDRLocale entry : localeArray) {
            if (entry.equals(locale)) {
                return true;
            }
        }
        return false;
    }

    public static boolean localeMatchesLocaleList(String localeList, CLDRLocale locale) {
        if (isAllLocales(localeList)) {
            return true;
        }
        String localeArray[] = tokenizeLocale(localeList);
        return localeMatchesLocaleList(localeArray, locale);
    }
}
