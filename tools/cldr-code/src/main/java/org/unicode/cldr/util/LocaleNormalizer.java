package org.unicode.cldr.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Normalize and validate sets of locales. This class was split off from UserRegistry.java with the
 * goal of encapsulation to support refactoring and implementation of new features such as warning a
 * Manager who tries to assign to a Vetter unknown locales or locales that are not covered by their
 * organization.
 *
 * <p>A single locale may be represented by a string like "fr_CA" for Canadian French, or by a
 * CLDRLocale object.
 *
 * <p>A set of locales related to a particular Survey Tool user is compactly represented by a single
 * string like "am fr_CA zh" (meaning "Amharic, Canadian French, and Chinese"). Survey Tool uses
 * this compact representation for storage in the user database, and for browser inputting/editing
 * forms, etc.
 *
 * <p>Otherwise the preferred representation is a LocaleSet, which encapsulates a Set<CLDRLocale>
 * along with special handling for isAllLocales.
 */
public class LocaleNormalizer {

    /**
     * Reasons why a particular locale ID can't be assigned to a particular Survey Tool user for
     * voting
     */
    public enum LocaleRejection {
        /** Most users (except managers/TC) are not allowed to have "*" meaning "all locales" */
        all_locales("All locales"),

        /** A default-content locale (like "ja_JP", unlike "ja"), doesn't allow voting */
        default_content("Default content"),

        /** CLDRLocale.getExistingInstance failed; the ID is bogus or unsupported */
        not_cldr_locale("Not a CLDR locale"),

        /** The user's organization does not cover this locale */
        outside_org_coverage("Outside org. coverage"),

        /** The locale is read-only (but not default-content), like "en" */
        read_only("Read only"),

        /** The locale is "scratch", like "mul" */
        scratch("Scratch locale"),

        /** The locale is not included in knownLocales; often equivalent to not_cldr_locale */
        unknown("Unknown");

        LocaleRejection(String message) {
            this.message = message;
        }

        final String message;

        @Override
        public String toString() {
            return message;
        }
    }

    /**
     * Special constant for specifying access to no locales. Used with intlocs (not with locale
     * access)
     */
    public static final String NO_LOCALES = "none";

    /** Special String constant "*" for specifying access to all locales. */
    public static final String ALL_LOCALES = StandardCodes.ALL_LOCALES;

    /** Sometimes used (maybe mistakenly) for the same meaning as ALL_LOCALES ("*") */
    private static final String ALL_LOCALES_STRING = "all";

    public static boolean isAllLocales(String localeList) {
        return (localeList != null)
                && (localeList.contains(ALL_LOCALES)
                        || localeList.trim().equals(ALL_LOCALES_STRING));
    }

    /** Special LocaleSet constant for specifying access to all locales. */
    public static final LocaleSet ALL_LOCALES_SET = new LocaleSet(true);

    /**
     * The actual set of locales used by CLDR. It is used for validation, so it should not simply be
     * ALL_LOCALES_SET. For Survey Tool, this may be set by SurveyMain during initialization, using
     * the set of locale names used for files in common/main/*.xml and common/annotations/*.xml. For
     * example, "aa" is in the set if common/main/aa.xml exists.
     */
    private static LocaleSet knownLocales = null;

    public static void setKnownLocales(Set<CLDRLocale> localeListSet) {
        knownLocales = new LocaleSet().addAll(localeListSet);
    }

    /**
     * Normalize the given locale-list string, removing invalid/duplicate locale names, and saving
     * error/warning messages in this LocaleNormalizer object
     *
     * @param list the String like "zh aa test123"
     * @return the normalized string like "aa zh"
     */
    public String normalize(String list) {
        return norm(this, list, null, this.isStrict);
    }

    /**
     * Normalize the given locale-list string, removing invalid/duplicate locale names
     *
     * <p>Do not report any errors or warnings
     *
     * @param list the String like "zh aa test123"
     * @return the normalized string like "aa zh"
     */
    public static String normalizeQuietly(String list) {
        return norm(null, list, null, false /* isStrict */);
    }

    public static String normalizeQuietlyDisallowDefaultContent(String list) {
        return norm(null, list, null, true /* isStrict */);
    }

    /**
     * Normalize the given locale-list string, removing invalid/duplicate locale names, and saving
     * error/warning messages in this LocaleNormalizer object
     *
     * @param list the String like "zh aa test123"
     * @param orgLocaleSet the locales covered by a particular organization, used as a filter unless
     *     null or ALL_LOCALES_SET
     * @return the normalized string like "aa zh"
     */
    public String normalizeForSubset(String list, LocaleSet orgLocaleSet) {
        return norm(this, list, orgLocaleSet, this.isStrict);
    }

    /**
     * Normalize the given locale-list string, removing invalid/duplicate locale names
     *
     * <p>Always filter out unknown locales. If orgLocaleSet isn't null, filter out locales missing
     * from it.
     *
     * <p>This is static and has an optional LocaleNormalizer parameter that enables saving
     * warning/error messages that can be shown to the user.
     *
     * @param locNorm the object to be filled in with warning/error messages, if not null
     * @param list the String like "zh aa test123"
     * @param orgLocaleSet the locales covered by a particular organization, used as a filter unless
     *     null or ALL_LOCALES_SET
     * @param isStrict true if strict limitations are enforced on allowed locales
     * @return the normalized string like "aa zh"
     */
    private static String norm(
            LocaleNormalizer locNorm, String list, LocaleSet orgLocaleSet, boolean isStrict) {
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
        final LocaleSet locSet = setFromString(locNorm, list, orgLocaleSet, isStrict);
        return locSet.toString();
    }

    private Map<String, LocaleRejection> messages = null;

    private void addMessage(String locale, LocaleRejection rejection) {
        if (messages == null) {
            messages = new TreeMap<>();
        }
        messages.put(locale, rejection);
    }

    public boolean hasMessage() {
        return messages != null && !messages.isEmpty();
    }

    public String getMessagePlain() {
        return String.join("\n", getMessageArrayPlain());
    }

    public String getMessageHtml() {
        return String.join("<br />\n", getMessageArrayPlain());
    }

    public String[] getMessageArrayPlain() {
        return getMessagesPlain().toArray(new String[0]);
    }

    public Collection<String> getMessagesPlain() {
        return getMessages().entrySet().stream()
                .map(e -> (e.getValue() + ": " + e.getKey()))
                .collect(Collectors.toList());
    }

    public Map<String, LocaleRejection> getMessages() {
        if (messages == null) return Collections.emptyMap();
        return Collections.unmodifiableMap(messages);
    }

    public static LocaleSet setFromStringQuietly(String locales, LocaleSet orgLocaleSet) {
        return setFromString(null, locales, orgLocaleSet, false /* isStrict */);
    }

    /**
     * Construct a normalized LocaleSet from the given string
     *
     * @param locNorm the LocaleNormalizer, or null
     * @param localeList the space-separated list of locale IDs to be normalized and converted into
     *     a LocaleSet
     * @param orgLocaleSet the organization's set of authorized locales, or null
     * @param isStrict whether to enforce strict checking of locales
     * @return the LocaleSet
     */
    private static LocaleSet setFromString(
            LocaleNormalizer locNorm, String localeList, LocaleSet orgLocaleSet, boolean isStrict) {
        if (locNorm != null && locNorm.isStrict) {
            isStrict = true;
        }
        if (isStrict && knownLocales == null) {
            throw new InternalCldrException(
                    "knownLocales must be initialized for strict normalization");
        }
        if (isAllLocales(localeList)) {
            if (orgLocaleSet == null || orgLocaleSet.isAllLocales()) {
                return ALL_LOCALES_SET;
            }
            return intersectKnownWithOrgLocales(orgLocaleSet);
        }
        final LocaleSet newSet = new LocaleSet();
        if (localeList == null || (localeList = localeList.trim()).isEmpty()) {
            return newSet; // empty
        }
        for (String s : splitToArray(localeList)) {
            CLDRLocale locale = CLDRLocale.getExistingInstance(s);
            LocaleRejection rej = getRejection(locale, orgLocaleSet, isStrict);
            if (rej != null) {
                if (locNorm != null) {
                    locNorm.addMessage(s, rej);
                }
            } else {
                newSet.add(locale);
            }
        }
        return newSet;
    }

    private static LocaleRejection getRejection(
            CLDRLocale locale, LocaleSet orgLocaleSet, boolean isStrict) {
        LocaleRejection rej;
        // The order of conditionals matters if some locales might be rejectable for more than one
        // reason
        if (locale == null) {
            return LocaleRejection.not_cldr_locale;
        } else if (knownLocales != null && !knownLocales.contains(locale)) {
            return LocaleRejection.unknown; // rare since generally not_cldr_locale
        } else if (isStrict && (rej = checkStrictRejection(locale)) != null) {
            return rej; // default_content, scratch, read_only
        } else if (orgLocaleSet != null && !orgLocaleSet.containsLocaleOrParent(locale)) {
            return LocaleRejection.outside_org_coverage;
        } else {
            return null;
        }
    }

    public static String[] splitToArray(String localeList) {
        if (localeList == null || localeList.isEmpty()) {
            return new String[0];
        }
        return localeList.trim().split("[, \t\u00a0\\s]+"); // whitespace
    }

    /**
     * If this is set to true, disallow per checkStrictRejection. The default is non-strict, pending
     * verification that strict normalization is appropriate for all existing usages of
     * LocaleNormalizer.
     */
    private boolean isStrict = false;

    public LocaleNormalizer makeStrict() {
        this.isStrict = true;
        return this;
    }

    /**
     * Reject default-content, scratch, and read-only locales
     *
     * @param cldrLocale the locale to check
     * @return null to accept, or a non-null LocaleRejection to reject
     */
    private static LocaleRejection checkStrictRejection(CLDRLocale cldrLocale) {
        if (SupplementalDataInfo.getInstance()
                .getDefaultContentLocales()
                .contains(cldrLocale.getBaseName())) {
            return LocaleRejection.default_content;
        }
        if (SpecialLocales.isScratchLocale(cldrLocale)) {
            return LocaleRejection.scratch;
        }
        if (SpecialLocales.Type.isReadOnly(SpecialLocales.getType(cldrLocale))) {
            return LocaleRejection.read_only; // includes algorithmic
        }
        return null; // not rejected
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

    public enum InvalidLocaleAction {
        FIND,
        FIX
    }

    public static class ProblemMap {
        /** Map from invalid locale ID names to Problem descriptions */
        public final Map<String, Problem> locMap = new TreeMap<>();

        public void add(String localeId, LocaleRejection rejection, Solution solution) {
            Problem problem = locMap.get(localeId);
            if (problem == null) {
                locMap.put(localeId, new Problem(rejection, 1, solution));
            } else {
                problem.addSolution(solution);
                problem.increment();
            }
        }

        public Set<Organization> leaderlessOrgs = new TreeSet<>();

        public void addLeaderlessOrg(Organization org) {
            leaderlessOrgs.add(org);
        }

        public void merge(ProblemMap moreProblems) {
            for (String localeId : moreProblems.locMap.keySet()) {
                Problem newProblem = moreProblems.locMap.get(localeId);
                if (!locMap.containsKey(localeId)) {
                    locMap.put(localeId, newProblem);
                } else {
                    Problem oldProblem = locMap.get(localeId);
                    oldProblem.userCount += newProblem.userCount;
                    oldProblem.addSolutions(newProblem.solutions);
                }
            }
            leaderlessOrgs.addAll(moreProblems.leaderlessOrgs);
        }
    }

    public static class Problem {
        public final LocaleRejection rejection;
        public Integer userCount;
        public Map<Solution, Integer> solutions = new TreeMap<>(); // map to count (repetitions)

        public Problem(LocaleRejection rejection, Integer count, Solution solution) {
            this.rejection = rejection;
            this.userCount = count;
            this.solutions.put(solution, 1);
        }

        public void addSolution(Solution solution) {
            if (this.solutions.containsKey(solution)) {
                this.solutions.put(solution, this.solutions.get(solution) + 1);
            } else {
                this.solutions.put(solution, 1);
            }
        }

        public void addSolutions(Map<Solution, Integer> newSolutions) {
            for (Solution solution : newSolutions.keySet()) {
                Integer newCount = newSolutions.get(solution);
                if (this.solutions.containsKey(solution)) {
                    this.solutions.put(solution, this.solutions.get(solution) + newCount);
                } else {
                    this.solutions.put(solution, newCount);
                }
            }
        }

        public void increment() {
            ++userCount;
        }
    }

    public static class Solution implements Comparable<Solution> {
        public enum Type {
            REPLACE,
            DELETE
        }

        public final String localeId;
        public Type type;
        public CLDRLocale replacementLocale = null;

        public Solution(String localeId, Type type, CLDRLocale repLoc) {
            if (type != Type.REPLACE) {
                throw new IllegalArgumentException("3-arg constructor is for REPLACE, not " + type);
            }
            this.localeId = localeId;
            this.type = type;
            this.replacementLocale = repLoc;
        }

        public Solution(String localeId, Type type) {
            if (type != Type.DELETE) {
                throw new IllegalArgumentException("2-arg constructor is for DELETE, not " + type);
            }
            this.localeId = localeId;
            this.type = type;
        }

        @Override
        public int compareTo(Solution o) {
            return this.toString().compareTo(o.toString());
        }

        public String toString() {
            if (type == null) {
                type = Type.DELETE;
            }
            switch (type) {
                case REPLACE:
                    return type + " with " + replacementLocale.getBaseName();
                case DELETE:
                    return type.toString();
                default:
                    throw new RuntimeException("Solution type not handled: " + type);
            }
        }
    }

    /**
     * Check the authorized locales for one user
     *
     * @param localesFromDB the string with locale IDs retrieved from the db for this user
     * @param orgLocales the user's organization's set of covered locales, used for some solutions
     *     even if canVoteNonOrg
     * @param canVoteNonOrg true if this user is allowed to vote in locales outside org coverage
     * @param problems the ProblemMap for adding problems
     */
    public void checkUserLocales(
            String localesFromDB,
            LocaleSet orgLocales,
            boolean canVoteNonOrg,
            ProblemMap problems) {
        if (isStrict && isAllLocales(localesFromDB)) {
            // Only manager or stronger are allowed to have "*" for all locales, and this method
            // is not called for such users (caller returns early). At this point in the code,
            // reject "*".
            addMessage(localesFromDB, LocaleRejection.all_locales);
        }
        if (canVoteNonOrg || orgLocales == null) {
            normalize(localesFromDB);
        } else {
            normalizeForSubset(localesFromDB, orgLocales);
        }
        if (hasMessage()) {
            final Map<String, LocaleRejection> messages = getMessages();
            for (String localeId : messages.keySet()) {
                LocaleRejection rejection = messages.get(localeId);
                Solution solution = solveRejection(rejection, localeId, orgLocales);
                problems.add(localeId, rejection, solution);
            }
        }
    }

    private Solution solveRejection(
            LocaleRejection rejection, String localeId, LocaleSet orgLocales) {
        Solution solution;
        switch (rejection) {
            case all_locales:
            case read_only:
            case scratch:
            case outside_org_coverage:
                solution = new Solution(localeId, Solution.Type.DELETE);
                break;
            case not_cldr_locale:
            case unknown:
                solution = solveUnknownLocale(localeId, orgLocales);
                break;
            case default_content:
                solution = solveDefaultContentLocale(localeId, orgLocales);
                break;
            default:
                throw new RuntimeException("Rejection not handled: " + rejection);
        }
        return solution;
    }

    private Solution solveUnknownLocale(String localeId, LocaleSet orgLocales) {
        /*
         * Do not use getLikelySubtags on these "unusable" names; ALL_LOCALES_STRING ("all") would map to "all_Mlym_IN"
         * and LocaleNames.UND ("und") would map to "en_Latn_US"
         *
         * <p>Note: we shouldn't get LocaleNames.MUL in solveUnknownLocale since it would be rejected
         * earlier as scratch, and maybe likewise LocaleNames.UND, LocaleNames.ROOT, and
         * ALL_LOCALES_STRING, but (double-)check here anyway (better safe than sorry in case the code
         * changes).
         */
        final Set<String> unusableNames =
                new HashSet<>(
                        Arrays.asList(
                                ALL_LOCALES_STRING,
                                LocaleNames.MUL,
                                LocaleNames.UND,
                                LocaleNames.ROOT));
        if (!unusableNames.contains(localeId)) {
            String replacementLocaleId =
                    SupplementalDataInfo.getInstance().getLikelySubtags().get(localeId);
            if (replacementLocaleId != null && !replacementLocaleId.isEmpty()) {
                CLDRLocale repLoc = CLDRLocale.getExistingInstance(replacementLocaleId);
                if (repLoc != null
                        && (orgLocales == null || orgLocales.contains(repLoc))
                        && !(isStrict && checkStrictRejection(repLoc) != null)) {
                    return new Solution(localeId, Solution.Type.REPLACE, repLoc);
                }
            }
        }
        return new Solution(localeId, Solution.Type.DELETE);
    }

    private Solution solveDefaultContentLocale(String localeId, LocaleSet orgLocales) {
        CLDRLocale loc = CLDRLocale.getExistingInstance(localeId);
        CLDRLocale dcParent = SupplementalDataInfo.getInstance().getBaseFromDefaultContent(loc);
        if (dcParent != null) {
            String replacementLocaleId = dcParent.getBaseName();
            CLDRLocale repLoc = CLDRLocale.getExistingInstance(replacementLocaleId);
            if (repLoc != null && (orgLocales == null || orgLocales.contains(repLoc))) {
                return new Solution(localeId, Solution.Type.REPLACE, repLoc);
            }
        }
        return new Solution(localeId, Solution.Type.DELETE);
    }
}
