package org.unicode.cldr.test;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.cldr.util.StandardCodes;

import com.google.common.collect.ImmutableSet;

public final class SubmissionLocales {
    public static Set<String> NEW_CLDR_LOCALES = ImmutableSet.of(
        "ceb",  // Cebuano (not new in release, but needs major changes)
        "mai",  // Maithili
        "mni",  // Manipuri (Bengali script)-Apple as well
        "sat",  // Santali -(Apple use Olck script)
        "kok",  // Konkani -(Note: this is already covered by a MS vetter at Modern level)
        "sd_Deva",   // Sindhi (Devanagari) 
        "su",   // Sundanese (script TBD)
        "cad",  // Caddo
        "pcm",  // Nigerian Pidgin
        "gn"    // Guarani
        );
    
    public static Set<String> HIGH_LEVEL_LOCALES = ImmutableSet.of(
        "chr",  // Cherokee
        "gd",   // Scottish Gaelic, Gaelic
        "fo"    // Faroese
        );
    
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

    public static final Pattern ALLOWED_IN_LIMITED_PATHS = 
    Pattern.compile("//ldml/annotations/annotation.*[🤵👰⬆➡⬇⬅♾✖➕➖➗]");

    
    /* Example of special paths
     * Pattern.compile(
        "//ldml/"
            + "(listPatterns/listPattern\\[@type=\"standard"
            + "|annotations/annotation\\[@cp=\"([©®‼⁉☑✅✔✖✨✳✴❇❌❎❓-❕❗❣ ➕-➗👫-👭👱🥰🧩🧔😸😺😹😼😻🦊😽😼⭕😺😿😾😻😸😹🐺⭕🦄😽🐼🐸😿🤖🐹🐻🙀🦁]|👱‍♀|👱‍♂)\""
            + "|localeDisplayNames/"
            +   "(scripts/script\\[@type=\"(Elym|Hmnp|Nand|Wcho)\""
            +    "|territories/territory\\[@type=\"(MO|SZ)\"](\\[@alt=\"variant\"])?"
            +    "|types/type\\[@key=\"numbers\"]\\[@type=\"(hmnp|wcho)\"]"
            +   ")"
            + "|dates/timeZoneNames/(metazone\\[@type=\"Macau\"]"
            +   "|zone\\[@type=\"Asia/Macau\"]"
            +   ")"
            + ")"
            );
            */
    
//ldml/dates/timeZoneNames/metazone[@type="Macau"]/long/daylight, old: Macau Summer Time, new: Macao Summer Time
//ldml/dates/timeZoneNames/metazone[@type="Macau"]/long/standard, old: Macau Standard Time, new: Macao Standard Time
//ldml/localeDisplayNames/territories/territory[@type="SZ"][@alt="variant"], old: SZ, new: Swaziland
//ldml/dates/timeZoneNames/zone[@type="Asia/Macau"]/exemplarCity, old: Macau, new: Macao
//ldml/dates/timeZoneNames/metazone[@type="Macau"]/long/generic, old: Macau Time, new: Macao Time
//ldml/localeDisplayNames/territories/territory[@type="SZ"], old: Swaziland, new: Eswatini


    /**
     * Only call this if LIMITED_SUBMISSION
     * @param localeString
     * @param path
     * @param isError
     * @param isMissing
     * @return true if submission is allowed, else false
     */
    public static boolean allowEvenIfLimited(String localeString, String path, boolean isError, boolean isMissing) {

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

        if (isMissing) {
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

    private static final boolean DEBUG_REGEX = false;

    /**
     * Only public for testing
     * @param path
     * @return
     */
    public static boolean pathAllowedInLimitedSubmission(String path) {
        if (ALLOWED_IN_LIMITED_PATHS == null) {
            return false;
        }
        final Matcher matcher = SubmissionLocales.ALLOWED_IN_LIMITED_PATHS.matcher(path);
        boolean result = matcher.lookingAt();
        if (DEBUG_REGEX && !result) {
            System.out.println(RegexUtilities.showMismatch(matcher, path));
        }
        return result;
    }
}