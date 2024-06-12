package org.unicode.cldr.test;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.GrammarInfo;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.VoterReportStatus;
import org.unicode.cldr.util.VoterReportStatus.ReportId;

/**
 * This class manages the Limited Submission process.
 *
 * <p>TODO: see https://unicode-org.atlassian.net/browse/CLDR-15230 for TODOs here
 *
 * @see CheckCLDR#LIMITED_SUBMISSION
 */
public final class SubmissionLocales {
    // TODO:  On the use of Locales.txt here, see
    // https://unicode-org.atlassian.net/browse/CLDR-14838
    /** This is the 'raw' list from Locales.txt */
    public static final Set<String> CLDR_LOCALES =
            StandardCodes.make().getLocaleToLevel(Organization.cldr).keySet();

    /** This is the 'special' list from Locales.txt */
    public static final Set<String> SPECIAL_ORG_LOCALES =
            StandardCodes.make().getLocaleToLevel(Organization.special).keySet();

    /**
     * Non-CLDR Locales, but consistently have high level of engagement from volunteers to keep at
     * modern level. Reevaluate for each release based on meeting 95+% of modern, moderate, and
     * basic coverage
     */
    public static Set<String> HIGH_LEVEL_LOCALES =
            ImmutableSet.of(
                    // Note: ALL of these were found in Locales.txt under cldr.
                    "chr", // Cherokee
                    "gd", // Scottish Gaelic, Gaelic
                    "fo", // Faroese
                    "kok", // Konkani
                    "pcm", // Nigerian Pidgin
                    "ha", // Hausa
                    "hsb", // Upper Sorbian
                    "dsb", // Lower Sorbian
                    "yue_Hans", // Cantonese (Simplified)
                    "to" //  Tongan
                    );

    public static final Set<String> CLDR_OR_HIGH_LEVEL_LOCALES =
            ImmutableSet.<String>builder().addAll(CLDR_LOCALES).addAll(HIGH_LEVEL_LOCALES).build();

    /** Subset of reports open for this release */
    private static final Set<ReportId> LIMITED_SUBMISSION_REPORTS =
            Collections.unmodifiableSet(EnumSet.of(VoterReportStatus.ReportId.personnames));

    /** Subset of CLDR_LOCALES, minus special which are only those which are TC orgs */
    public static final Set<String> TC_ORG_LOCALES;

    /** Space-separated list of TC locales to extend submission */
    public static final String DEFAULT_EXTENDED_SUBMISSION = "";

    /** Additional TC locales which have extended submission. Do not add non-tc locales here. */
    public static final Set<String> ADDITIONAL_EXTENDED_SUBMISSION =
            ImmutableSet.copyOf(
                    CLDRConfig.getInstance()
                            .getProperty("CLDR_EXTENDED_SUBMISSION", "")
                            .split(" "));

    /**
     * Set to true iff ONLY grammar locales should be limited submission {@link
     * GrammarInfo#getGrammarLocales()}
     */
    public static final boolean ONLY_GRAMMAR_LOCALES = false;

    /** Update this in each limited release. */
    public static final Set<String> LOCALES_FOR_LIMITED;

    static {
        Set<String> temp = new HashSet<>(CLDR_OR_HIGH_LEVEL_LOCALES);
        if (ONLY_GRAMMAR_LOCALES) {
            temp.retainAll(GrammarInfo.getGrammarLocales());
        }
        LOCALES_FOR_LIMITED = ImmutableSortedSet.copyOf(temp);

        Set<String> temp2 = new HashSet<>(CLDR_LOCALES);
        temp2.removeAll(SPECIAL_ORG_LOCALES);
        TC_ORG_LOCALES = ImmutableSortedSet.copyOf(temp2);
    }

    /**
     * New locales in this release, where we want to allow any paths even if others are restricted
     */
    public static Set<String> ALLOW_ALL_PATHS_BASIC =
            ImmutableSet.of(
                    // locales open for v43:
                    "apc", // Levantine Arabic; NB actual submission was "ajp" South Levantine
                    // Arabic
                    "lmo", // Lombardi
                    "pap", // Papiamento
                    "rif" // Riffian
                    );

    public static Set<String> LOCALES_ALLOWED_IN_LIMITED =
            ImmutableSet.<String>builder()
                    .addAll(LOCALES_FOR_LIMITED)
                    .addAll(ALLOW_ALL_PATHS_BASIC)
                    .build();

    public static final Pattern PATHS_ALLOWED_IN_LIMITED =
            Pattern.compile(
                    "//ldml/"
                            // v43: All person names
                            + "(personNames/.*"
                            // v43: Turkey and its alternate
                            + "|localeDisplayNames/territories/territory\\[@type=\"TR\"\\].*"
                            // v43: Exemplar city for America/Ciudad_Juarez
                            + "|dates/timeZoneNames/zone[@type=\"America/Ciudad_Juarez\"]/exemplarCity"
                            + ")");

    // Pattern.compile("//ldml/units/unitLength\\[@type=\"long\"]");

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

    // ldml/dates/timeZoneNames/metazone[@type="Macau"]/long/daylight, old: Macau Summer Time, new:
    // Macao Summer Time
    // ldml/dates/timeZoneNames/metazone[@type="Macau"]/long/standard, old: Macau Standard Time,
    // new: Macao Standard Time
    // ldml/localeDisplayNames/territories/territory[@type="SZ"][@alt="variant"], old: SZ, new:
    // Swaziland
    // ldml/dates/timeZoneNames/zone[@type="Asia/Macau"]/exemplarCity, old: Macau, new: Macao
    // ldml/dates/timeZoneNames/metazone[@type="Macau"]/long/generic, old: Macau Time, new: Macao
    // Time
    // ldml/localeDisplayNames/territories/territory[@type="SZ"], old: Swaziland, new: Eswatini

    private static final class SubmissionLocalesCache {
        public static SubmissionLocalesCache INSTANCE = new SubmissionLocalesCache();
        private LoadingCache<String, CoverageLevel2> covs;

        SubmissionLocalesCache() {
            covs =
                    CacheBuilder.newBuilder()
                            .build(
                                    new CacheLoader<String, CoverageLevel2>() {
                                        @Override
                                        public CoverageLevel2 load(String key) throws Exception {
                                            return CoverageLevel2.getInstance(
                                                    SupplementalDataInfo.getInstance(), key);
                                        }
                                    });
        }

        public static Enum<Level> getCoverageLevel(String localeString, String path) {
            try {
                return INSTANCE.covs.get(localeString).getLevel(path);
            } catch (ExecutionException e) {
                throw new RuntimeException(
                        String.format("Could not fetch coverage for %s:%s", localeString, path), e);
            }
        }
    }

    /**
     * Only call this if {@link CheckCLDR#LIMITED_SUBMISSION}
     *
     * @param localeString
     * @param path
     * @param isError
     * @param isMissing
     * @return true if submission is allowed, else false
     * @see CheckCLDR#LIMITED_SUBMISSION
     */
    public static boolean allowEvenIfLimited(
            String localeString, String path, boolean isError, boolean isMissing) {

        // Allow errors to be fixed
        if (isError) {
            return true;
        }

        // for new locales, allow basic paths
        if (SubmissionLocales.ALLOW_ALL_PATHS_BASIC.contains(localeString)
                &&
                // Only check coverage level for these locales
                isPathBasicOrLess(localeString, path)) {
            return true;
        }

        // all but specific locales are otherwise locked
        if (!SubmissionLocales.LOCALES_ALLOWED_IN_LIMITED.contains(localeString)) {
            return false;
        }

        // in TC Org locales, lock all paths except missing and special
        if (isMissing && TC_ORG_LOCALES.contains(localeString)) {
            return true;
        }

        if (pathAllowedInLimitedSubmission(path)) {
            return true;
        }

        return false; // skip
    }

    private static boolean isPathBasicOrLess(String localeString, String path) {
        return SubmissionLocalesCache.getCoverageLevel(localeString, path).compareTo(Level.BASIC)
                <= 0;
    }

    private static final boolean DEBUG_REGEX = false;

    /**
     * Only public for testing
     *
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

    public static Set<ReportId> getReportsAvailableInLimited() {
        return LIMITED_SUBMISSION_REPORTS;
    }

    /**
     * @returns true if the locale or its parent is considered a TC Org Locale. Returns true for
     *     ROOT.
     */
    public static boolean isTcLocale(CLDRLocale loc) {
        if (loc == CLDRLocale.ROOT
                || SubmissionLocales.TC_ORG_LOCALES.contains(loc.getBaseName())) {
            // root or explicitly listed locale is a TC locale
            return true;
        } else if (loc.isParentRoot()) {
            // any sublocale of root not listed is not a tc locale
            return false;
        } else {
            return isTcLocale(loc.getParent());
        }
    }

    /**
     * @returns true if the locale or its parent is considered a TC Org Locale. Returns true for
     *     ROOT.
     */
    public static boolean isOpenForExtendedSubmission(CLDRLocale loc) {
        if (loc == CLDRLocale.ROOT) {
            return false; // root is never open
        } else if (SubmissionLocales.ADDITIONAL_EXTENDED_SUBMISSION.contains(loc.getBaseName())) {
            // explicitly listed locale is a open for additional
            return true;
        } else if (SubmissionLocales.TC_ORG_LOCALES.contains(loc.getBaseName())) {
            // TC locale but not listed as extended - NOT open for extended submission.
            return false;
        } else if (loc.isParentRoot()) {
            // Not a TC locale, so it's open.
            return true;
        } else {
            // child locale of an open locale is open
            return isOpenForExtendedSubmission(loc.getParent());
        }
    }
}
