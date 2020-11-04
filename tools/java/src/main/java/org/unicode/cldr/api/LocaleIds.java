package org.unicode.cldr.api;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.regex.Pattern;

/**
 * Utility methods for working with locale IDs as strings. This could, with a little thought, be
 * made public if necessary.
 */
final class LocaleIds {
    // From: https://unicode.org/reports/tr35/#Identifiers
    // Locale ID is:
    //   (<language>(_<script>)?|<script>)(_<region>)?(_<variant>)*
    //
    // However in CLDR data, there's always a language (even if it's "und"), and never more
    // than one variant, so this can be simplified to:
    //   <language>(_<script>)?(_<region>)?(_<variant>)?
    //
    // * Required language is lowercase 2 or 3 letter language ID (e.g. "en", "gsw").
    //   Note that the specification allows for languages 5-8 characters long, but in reality
    //   this has never occurred yet, so it's ignored in this code.
    //
    // * Script is 4-letter Xxxx script identifier (e.g. "Latn").
    //   The specification permits any casing for script subtags, but since all the data uses
    //   the capitalized "Xxxx" form, that's what this code expects.
    //
    // * Region is the uppercase 2-letter CLDR region code ("GB") or the 3-digit numeric
    //   identifier (e.g. "001").
    //
    // * Variants are a bit complex; either 5-8 length alphanumerics, or length 4 but starting
    //   with a digit (this avoids any ambiguity with script subtags). However because ICU
    //   violates this rule by using "TRADITIONAL" (11-letters) the length restriction is
    //   merely "longer than 5".
    //
    // Finaly, CLDR data only uses an '_' as the separator, whereas the specification allows
    // for either '-' or '_').
    //
    // The regex for unambiguously matching a valid locale ID (other than "root") for CLDR data is:
    private static final Pattern LOCALE_ID =
        Pattern.compile("(?:[a-z]{2,3})"
            + "(?:_(?:[A-Z][a-z]{3}))?"
            + "(?:_(?:[A-Z]{2}|[0-9]{3}))?"
            + "(?:_(?:[A-Z]{5,}|[0-9][A-Z0-9]{3}))?");

    /**
     * Checks whether the given ID is valid for CLDR use (including case). Locale IDs for use in
     * CLDR APIs are a subset of all possible locale IDs and, unlike general locale IDs, they
     * are case sensitive. The rules are:
     * <ul>
     *     <li>A locale ID is up to four subtags {@code
     *         <language>(_<script>)?(_<region>)?(_<variant>)?}
     *     <li>The allowed subtag separator is only ASCII underscore (not hyphen).
     *     <li>The language subtag must exist (though it is permitted to be {@code "und"}).
     *     <li>All other subtags are optional and are separated by a single underscore.
     *     <li>Language subtag is lower-case, and is either 2 or 3 letters (i.e. "[a-z]{2,3}").
     *     <li>Script subtag is mixed-case and must match {@code "[A-Z][a-z]{3}"}.
     *     <li>Region subtag is upper-case and must match {@code "[A-Z]{2}} or {@code "[0-9]{3}"}.
     *     <li>Variant subtag is upper-case and must match {@code "[A-Z]{5,}} or
     *         {@code "[0-9][A-Z0-9]{3}"}.
     *     <li>The special locale ID {@code "root"} is also permitted.
     * <ul>
     *
     * <p>Note that this check does don't enforce validity in terms of checking for deprecated
     * languages, regions or script, so things like {@code "sh_YU"} (deprecated language and/or
     * region) are accepted.
     *
     * <p>Examples of valid locale IDs are {@code "en"}, {@code "zh_Hant"}, {@code "fr_CA"},
     * {@code "sr_Latn_RS"} and {@code "ja_JP_TRADITIONAL"}.
     *
     * <p>Examples of invalid locale IDs are {@code ""}, {@code "en_"}, {@code "Latn"} and
     * {@code "de__TRADITIONAL"}.
     *
     * @param localeId the ID the check.
     * @throws IllegalArgumentException is the ID is invalid.
     */
    public static void checkCldrLocaleId(String localeId) {
        // This check runs on a lot of locales, so make it as minimal as possible. If normalization
        // is ever needed, do it in a separate method.
        checkArgument(LOCALE_ID.matcher(localeId).matches() || localeId.equals("root"),
            "bad locale ID: %s", localeId);
    }

}
