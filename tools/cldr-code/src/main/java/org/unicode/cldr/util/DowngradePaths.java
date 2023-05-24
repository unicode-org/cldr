package org.unicode.cldr.util;

import java.nio.file.Paths;
import org.unicode.cldr.draft.FileUtilities;

public class DowngradePaths {

    static LocalePathValueListMatcher data =
            LocalePathValueListMatcher.load(
                    Paths.get(
                            FileUtilities.getRelativeFileName(
                                    DowngradePaths.class, "downgrade.txt")));

    public static boolean lookingAt(String locale, String path, String value) {
        return data.lookingAt(locale, path, value);
    }

    public static boolean lookingAt(String locale) {
        return data.lookingAt(locale);
    }
}
