package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Set;

/**
 * Normalize and validate sets of locales. This class was split off from UserRegistry.java with
 * the goal of encapsulation to support refactoring and implementation of new features such as
 * warning a Manager who tries to assign to a Vetter unknown locales or locales that are not
 * covered by their organization.
 *
 * A single locale may be represented by a string like "fr_CA" for Canadian French, or by
 * a CLDRLocale object.
 *
 * A set of locales related to a particular Survey Tool user is compactly represented by a single string
 * like "am fr_CA zh" (meaning "Amharic, Canadian French, and Chinese"). Survey Tool uses this compact
 * representation for storage in the user database, and for browser inputting/editing forms, etc.
 *
 * Otherwise the preferred representation is a LocaleSet, which encapsulates a Set<CLDRLocale> along
 * with special handling for isAllLocales.
 */
public class LocaleNormalizer {

    /**
     * Special constant for specifying access to no locales. Used with intlocs (not with locale access)
     */
    public static final String NO_LOCALES = "none";

    /**
     * Special String constant for specifying access to all locales.
     */
    public static final String ALL_LOCALES = "*";

    public static boolean isAllLocales(String localeList) {
        return (localeList != null) && (localeList.contains(ALL_LOCALES) || localeList.trim().equals("all"));
    }

    /**
     * Special LocaleSet constant for specifying access to all locales.
     */
    public static final LocaleSet ALL_LOCALES_SET = new LocaleSet(true);

    /**
     * The actual set of locales used by CLDR. For Survey Tool, this may be set by SurveyMain during initialization.
     * It is used for validation so it should not simply be ALL_LOCALES_SET.
     */
    private static LocaleSet knownLocales = null;

    public static void setKnownLocales(Set<CLDRLocale> localeListSet) {
        knownLocales = new LocaleSet();
        knownLocales.addAll(localeListSet);
    }

    /**
     * Normalize the given locale-list string, removing invalid/duplicate locale names,
     * and saving error/warning messages in this LocaleNormalizer object
     *
     * @param list the String like "zh  aa test123"
     * @return the normalized string like "aa zh"
     */
    public String normalize(String list) {
        return norm(this, list, null);
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
        return norm(null, list, null);
    }

    /**
     * Normalize the given locale-list string, removing invalid/duplicate locale names,
     * and saving error/warning messages in this LocaleNormalizer object
     *
     * @param list the String like "zh  aa test123"
     * @param orgLocaleSet the locales covered by a particular organization,
     *        used as a filter unless null or ALL_LOCALES_SET
     * @return the normalized string like "aa zh"
     */
    public String normalizeForSubset(String list, LocaleSet orgLocaleSet) {
        return norm(this, list, orgLocaleSet);
    }

    /**
     * Normalize the given locale-list string, removing invalid/duplicate locale names
     *
     * Always filter out unknown locales.
     * If orgLocaleSet isn't null, filter out locales missing from it.
     *
     * This is static and has an optional LocaleNormalizer parameter that enables saving
     * warning/error messages that can be shown to the user.
     *
     * @param locNorm the object to be filled in with warning/error messages, if not null
     * @param list the String like "zh  aa test123"
     * @param orgLocaleSet the locales covered by a particular organization,
     *        used as a filter unless null or ALL_LOCALES_SET
     * @return the normalized string like "aa zh"
     */
    private static String norm(LocaleNormalizer locNorm, String list, LocaleSet orgLocaleSet) {
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
        final LocaleSet locSet = setFromString(locNorm, list, orgLocaleSet);
        return locSet.toString();
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

    public static LocaleSet setFromStringQuietly(String locales, LocaleSet orgLocaleSet) {
        return setFromString(null, locales, orgLocaleSet);
    }

    private static LocaleSet setFromString(LocaleNormalizer locNorm, String localeList, LocaleSet orgLocaleSet) {
        if (isAllLocales(localeList)) {
            if (orgLocaleSet == null || orgLocaleSet.isAllLocales()) {
                return ALL_LOCALES_SET;
            }
            return intersectKnownWithOrgLocales(orgLocaleSet);
        }
        final LocaleSet newSet = new LocaleSet();
        if (localeList == null || (localeList = localeList.trim()).length() == 0) {
            return newSet;
        }
        final String[] array = localeList.split("[, \t\u00a0\\s]+"); // whitespace
        for (String s : array) {
            CLDRLocale locale = CLDRLocale.getInstance(s);
            if (knownLocales == null || knownLocales.contains(locale)) {
                if (orgLocaleSet == null || orgLocaleSet.containsLocaleOrParent(locale)) {
                    newSet.add(locale);
                } else if (locNorm != null) {
                    locNorm.addMessage("Outside org. coverage: " + locale.getBaseName());
                }
            } else if (locNorm != null) {
                locNorm.addMessage("Unknown: " + locale.getBaseName());
            }
        }
        return newSet;
    }

    private static LocaleSet intersectKnownWithOrgLocales(LocaleSet orgLocaleSet) {
        if (knownLocales == null) {
            final LocaleSet orgSetCopy = new LocaleSet();
            orgSetCopy.addAll(orgLocaleSet.getSet());
            return orgSetCopy;
        }
        final LocaleSet intersection = new LocaleSet();
        for (CLDRLocale locale : knownLocales.getSet()) {
            if (orgLocaleSet.containsLocaleOrParent(locale)) {
                intersection.add(locale);
            }
        }
        return intersection;
    }
}
