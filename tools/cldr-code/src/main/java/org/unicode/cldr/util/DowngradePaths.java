package org.unicode.cldr.util;

public class DowngradePaths {

    private static LocalePathValueListMatcher data =
            LocalePathValueListMatcher.load(CldrUtility.getUTF8Data("downgrade.txt").lines());

    public static boolean lookingAt(String locale, String path, String value) {
        return data.lookingAt(locale, path, value);
    }

    public static boolean lookingAt(String locale) {
        return data.lookingAt(locale);
    }
}
