package com.ibm.icu.text;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.icu.util.ULocale;

/**
 * Provide information about gender in locales based on data in CLDR. Currently just gender of lists.
 *
 * @author markdavis
 */
public class GenderInfo {

    private final ListGenderStyle style; // set based on locale

    /**
     * Gender: OTHER means either the information is unavailable, or the person has declined to state MALE or FEMALE.
     */
    public enum Gender {
        MALE, FEMALE, OTHER
    }

    /**
     * Create GenderInfo from a ULocale.
     *
     * @param uLocale
     */
    public GenderInfo(ULocale uLocale) {
        ULocale language = new ULocale(uLocale.getLanguage()); // in the hard coded data, the language is sufficient.
        // Will change with RB.
        ListGenderStyle tempStyle = localeToListGender.get(language);
        style = tempStyle == null ? ListGenderStyle.NEUTRAL : tempStyle;
    }

    /**
     * Create GenderInfo from a Locale.
     *
     * @param uLocale
     */
    public GenderInfo(Locale locale) {
        this(ULocale.forLocale(locale));
    }

    /**
     * Enum only meant for use in CLDR and in testing. Indicates the category for the locale.
     */
    public enum ListGenderStyle {
        /**
         * Always OTHER (if more than one)
         */
        NEUTRAL,
        /**
         * gender(all male) = male, gender(all female) = female, otherwise gender(list) = other
         */
        MIXED_NEUTRAL,
        /**
         * gender(all female) = female, otherwise gender(list) = male
         */
        MALE_TAINTS
    }

    /**
     * Reset the data used for mapping locales to styles. Only for use in CLDR and in testing.
     *
     * @param uLocale
     */
    public static void setLocaleMapping(Map<ULocale, ListGenderStyle> newULocaleToListGender) {
        localeToListGender.clear();
        for (Entry<ULocale, ListGenderStyle> entry : newULocaleToListGender.entrySet()) {
            localeToListGender.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Get the gender of a list, based on locale usage.
     *
     * @param genders
     *            a list of genders.
     * @return the gender of the list.
     */
    public Gender getListGender(Gender... genders) {
        return getListGender(Arrays.asList(genders));
    }

    /**
     * Get the gender of a list, based on locale usage.
     *
     * @param genders
     *            a list of genders.
     * @return the gender of the list.
     */
    public Gender getListGender(List<Gender> genders) {
        if (genders.size() == 0 || style == ListGenderStyle.NEUTRAL) {
            return Gender.OTHER; // degenerate case
        }
        if (genders.size() == 1) {
            return genders.get(0); // degenerate case
        }
        switch (style) {
        case MIXED_NEUTRAL: // gender(all male) = male, gender(all female) = female, otherwise gender(list) = other
            boolean hasFemale = false;
            boolean hasMale = false;
            for (Gender gender : genders) {
                switch (gender) {
                case FEMALE:
                    if (hasMale) {
                        return Gender.OTHER;
                    }
                    hasFemale = true;
                    break;
                case MALE:
                    if (hasFemale) {
                        return Gender.OTHER;
                    }
                    hasMale = true;
                    break;
                case OTHER:
                    return Gender.OTHER;
                }
            }
            return hasMale ? Gender.MALE : hasFemale ? Gender.FEMALE : Gender.OTHER;
        case MALE_TAINTS: // gender(all female) = female, otherwise gender(list) = male
            for (Gender gender : genders) {
                if (gender != Gender.FEMALE) {
                    return Gender.MALE;
                }
            }
            return Gender.FEMALE;
        default:
            return Gender.OTHER;
        }
    }

    // TODO Get this data from a resource bundle generated from CLDR.
    // For now, hard coded.

    private static Map<ULocale, ListGenderStyle> localeToListGender = new HashMap<>();
    static {
        for (String locale : Arrays.asList("ar", "ca", "cs", "hr", "es", "fr", "he", "hi", "it", "lt", "lv", "mr",
            "nl", "pl", "pt", "ro", "ru", "sk", "sl", "sr", "uk", "ur", "zh")) {
            localeToListGender.put(new ULocale(locale), ListGenderStyle.MALE_TAINTS);
        }
        for (String locale : Arrays.asList("el", "is")) {
            localeToListGender.put(new ULocale(locale), ListGenderStyle.MIXED_NEUTRAL);
        }
    }
}
