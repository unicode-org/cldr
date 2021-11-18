package org.unicode.cldr.test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.GrammarInfo;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.cldr.util.StandardCodes;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

public final class SubmissionLocales {

    public static final Set<String> CLDR_LOCALES = StandardCodes.make().getLocaleToLevel(Organization.cldr).keySet();

    /**
     * Non-CLDR Locales, but consistently have high level of engagement from volunteers to keep at modern level.
     * Reevaluate for each release based on meeting 95+% of modern, moderate, and basic coverage
     */
    public static Set<String> HIGH_LEVEL_LOCALES = ImmutableSet.of(
        "chr",  // Cherokee
        "gd",   // Scottish Gaelic, Gaelic
        "fo",   // Faroese
        "kok",  // Konkani
        "pcm",  // Nigerian Pidgin
        "ha",   // Hausa
        "hsb",  // Upper Sorbian
        "dsb",  // Lower Sorbian
        "yue_Hans",   // Cantonese (Simplified)
        "to"    //  Tongan
        );

    public static final Set<String> CLDR_OR_HIGH_LEVEL_LOCALES = ImmutableSet.<String>builder()
        .addAll(CLDR_LOCALES)
        .addAll(HIGH_LEVEL_LOCALES)
        .build();

    /**
     * Update this in each limited release.
     */
    public static final Set<String> LOCALES_FOR_LIMITED;
    static {
        Set<String> temp = new HashSet<>(CLDR_OR_HIGH_LEVEL_LOCALES);
        temp.retainAll(GrammarInfo.getGrammarLocales());
        LOCALES_FOR_LIMITED = ImmutableSortedSet.copyOf(temp);
    }

    /**
     * New locales in this release, where we want to allow any paths even if others are restricted
     */
    public static Set<String> ALLOW_ALL_PATHS = ImmutableSet.of(
        "brx",
        "ks",
        "ks_Deva",
        "rhg"   // Rohingya
        );

    public static Set<String> LOCALES_ALLOWED_IN_LIMITED = ImmutableSet.<String>builder()
        .addAll(LOCALES_FOR_LIMITED)
        .addAll(ALLOW_ALL_PATHS)
        .build();

    public static final Pattern PATHS_ALLOWED_IN_LIMITED =
    Pattern.compile("//ldml/units/unitLength\\[@type=\"long\"]");

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

        if (SubmissionLocales.ALLOW_ALL_PATHS.contains(localeString) || isError) {
            return true;
        } else {
            int debug = 0; // for debugging
        }

        // all but specific locales are otherwise locked

        if (!SubmissionLocales.LOCALES_ALLOWED_IN_LIMITED.contains(localeString)) {
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
        if (PATHS_ALLOWED_IN_LIMITED == null) {
            return false;
        }
        final Matcher matcher = SubmissionLocales.PATHS_ALLOWED_IN_LIMITED.matcher(path);
        boolean result = matcher.lookingAt();
        if (DEBUG_REGEX && !result) {
            System.out.println(RegexUtilities.showMismatch(matcher, path));
        }
        return result;
    }
}