package org.unicode.cldr.test;

import java.util.Set;
import java.util.regex.Pattern;

import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.StandardCodes;

import com.google.common.collect.ImmutableSet;

public final class SubmissionLocales {
    static Set<String> NEW_CLDR_LOCALES = ImmutableSet.of("jv", "so", "ceb", "ha", "ig", "yo");
    static Set<String> HIGH_LEVEL_LOCALES = ImmutableSet.of("chr", "gd", "fo");
    // have to have a lazy eval because otherwise CLDRConfig is called too early in the boot process
    static Set<String> CLDR_LOCALES = ImmutableSet.<String>builder()
        .addAll(HIGH_LEVEL_LOCALES)
        .addAll(NEW_CLDR_LOCALES)
        .addAll(StandardCodes.make().getLocaleToLevel(Organization.cldr).keySet()).build();

//            synchronized (SUBMISSION) {
//                if (CLDR_LOCALES == null) {
//                    CLDR_LOCALES = ImmutableSet.<String>builder()
//                        .addAll(HIGH_LEVEL_LOCALES)
//                        .addAll(StandardCodes.make().getLocaleToLevel(Organization.cldr).keySet()).build();
//                }
//            }

    public static final Pattern ALLOWED_IN_LIMITED_PATHS = Pattern.compile(
        "//ldml/"
            + "(listPatterns/listPattern\\[@type=\"standard"
            + "|annotations/annotation\\[@cp=\"([©®‼⁉☑✅✔✖✨✳✴❇❌❎❓-❕❗❣ ➕-➗👫-👭👱🥰🧩🧔]|👱‍♀|👱‍♂)\""
            + "|localeDisplayNames/"
            + "(scripts/script\\[@type=\"(Elym|Hmnp|Nand|Wcho)\""
            + "|territories/territory\\[@type=\"MO\"]"
            + ")"
            + ")"
            );

    /**
     * Only call this if LIMITED_SUBMISSION
     * @param localeString
     * @param path
     * @param valueStatus
     * @param lastReleaseStatus
     * @return
     */
    public static boolean allowEvenIfLimited(String localeString, String path, boolean isError, boolean missingInLastRelease) {

        // don't limit new locales or errors

        if (SubmissionLocales.NEW_CLDR_LOCALES.contains(localeString) || isError) {
            return true; 
        } else {
            int debug = 0; // for debugging
        }

        // all but CLDR locales are otherwise locked

        if (!SubmissionLocales.CLDR_LOCALES.contains(localeString)) {
            return false;
        } else {
            int debug = 0; // for debugging
        }

        // in those locales, lock all paths except missing and special

        if (missingInLastRelease) {
            return true;
        } else {
            int debug = 0; // for debugging
        }

        if (pathAllowedInLimitedSubmission(path)) {
            return true;
        } else {
            int debug = 0; // for debugging
        }

        return false; // skip
    }

    public static boolean pathAllowedInLimitedSubmission(String path) {
        return SubmissionLocales.ALLOWED_IN_LIMITED_PATHS.matcher(path).lookingAt();
    }
}