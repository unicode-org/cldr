package org.unicode.cldr.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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

    public enum LocaleRejection {
        outside_org_coverage("Outside org. coverage"),
        default_content("Default content"),
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

    /** Special String constant for specifying access to all locales. */
    public static final String ALL_LOCALES = StandardCodes.ALL_LOCALES;

    public static boolean isAllLocales(String localeList) {
        return (localeList != null)
                && (localeList.contains(ALL_LOCALES) || localeList.trim().equals("all"));
    }

    /** Special LocaleSet constant for specifying access to all locales. */
    public static final LocaleSet ALL_LOCALES_SET = new LocaleSet(true);

    /**
     * The actual set of locales used by CLDR. For Survey Tool, this may be set by SurveyMain during
     * initialization. It is used for validation, so it should not simply be ALL_LOCALES_SET.
     */
    private static LocaleSet knownLocales = null;

    public static void setKnownLocales(Set<CLDRLocale> localeListSet) {
        knownLocales = new LocaleSet();
        knownLocales.addAll(localeListSet);
    }

    /**
     * Normalize the given locale-list string, removing invalid/duplicate locale names, and saving
     * error/warning messages in this LocaleNormalizer object
     *
     * @param list the String like "zh aa test123"
     * @return the normalized string like "aa zh"
     */
    public String normalize(String list) {
        return norm(this, list, null, this.defaultContentIsDisallowed);
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
        return norm(null, list, null, false /* defaultContentIsDisallowed */);
    }

    public static String normalizeQuietlyDisallowDefaultContent(String list) {
        return norm(null, list, null, true /* defaultContentIsDisallowed */);
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
        return norm(this, list, orgLocaleSet, this.defaultContentIsDisallowed);
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
     * @param defaultContentIsDisallowed true if default-content locales are disallowed
     * @return the normalized string like "aa zh"
     */
    private static String norm(
            LocaleNormalizer locNorm,
            String list,
            LocaleSet orgLocaleSet,
            boolean defaultContentIsDisallowed) {
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
        final LocaleSet locSet =
                setFromString(locNorm, list, orgLocaleSet, defaultContentIsDisallowed);
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
        return setFromString(null, locales, orgLocaleSet, false /* defaultContentIsDisallowed */);
    }

    private static LocaleSet setFromString(
            LocaleNormalizer locNorm,
            String localeList,
            LocaleSet orgLocaleSet,
            boolean defaultContentIsDisallowed) {
        if (isAllLocales(localeList)) {
            if (orgLocaleSet == null || orgLocaleSet.isAllLocales()) {
                return ALL_LOCALES_SET;
            }
            return intersectKnownWithOrgLocales(orgLocaleSet);
        }
        final LocaleSet newSet = new LocaleSet();
        if (localeList == null || (localeList = localeList.trim()).isEmpty()) {
            return newSet;
        }
        final Set<String> defCon =
                (defaultContentIsDisallowed
                                || (locNorm != null && locNorm.defaultContentIsDisallowed))
                        ? SupplementalDataInfo.getInstance().getDefaultContentLocales()
                        : null;
        final String[] array = splitToArray(localeList);
        for (String s : array) {
            if (defCon != null && defCon.contains(s)) {
                if (locNorm != null) {
                    locNorm.addMessage(s, LocaleRejection.default_content);
                }
            } else {
                CLDRLocale locale = CLDRLocale.getInstance(s);
                if (knownLocales == null || knownLocales.contains(locale)) {
                    if (orgLocaleSet == null || orgLocaleSet.containsLocaleOrParent(locale)) {
                        newSet.add(locale);
                    } else if (locNorm != null) {
                        locNorm.addMessage(s, LocaleRejection.outside_org_coverage);
                    }
                } else if (locNorm != null) {
                    locNorm.addMessage(s, LocaleRejection.unknown);
                }
            }
        }
        return newSet;
    }

    public static String[] splitToArray(String localeList) {
        if (localeList == null || localeList.isEmpty()) {
            return new String[0];
        }
        return localeList.trim().split("[, \t\u00a0\\s]+"); // whitespace
    }

    private boolean defaultContentIsDisallowed = false;

    public LocaleNormalizer disallowDefaultContent() {
        this.defaultContentIsDisallowed = true;
        return this;
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

    /////////

    public enum InvalidLocaleAction {
        FIND,
        FIX
    }

    public static class ProblemMap {
        /** Map from invalid locale ID names to Problem descriptions */
        public Map<String, Problem> map = new TreeMap<>();

        public static void merge(ProblemMap allProblems, ProblemMap userProblems) {
            for (String localeId : userProblems.map.keySet()) {
                Problem newProblem = userProblems.map.get(localeId);
                if (!allProblems.map.containsKey(localeId)) {
                    allProblems.map.put(localeId, newProblem);
                } else {
                    Problem oldProblem = allProblems.map.get(localeId);
                    oldProblem.userCount += newProblem.userCount;
                    oldProblem.addSolutions(newProblem.solutions);
                }
            }
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

        public void addSolution(Solution newSolution) {
            if (this.solutions.containsKey(newSolution)) {
                this.solutions.put(newSolution, this.solutions.get(newSolution) + 1);
            } else {
                this.solutions.put(newSolution, 1);
            }
        }

        public void addSolutions(Map<Solution, Integer> newSolutions) {
            for (Solution newSolution : newSolutions.keySet()) {
                Integer newCount = newSolutions.get(newSolution);
                if (this.solutions.containsKey(newSolution)) {
                    this.solutions.put(newSolution, this.solutions.get(newSolution) + newCount);
                } else {
                    this.solutions.put(newSolution, newCount);
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

    public void checkUserLocales(String locales, LocaleSet orgLocales, ProblemMap problems) {
        if (orgLocales == null) {
            normalize(locales);
        } else {
            normalizeForSubset(locales, orgLocales);
        }
        // TODO: what about "en"? It's allowed by normalize(), rejected by normalizeForSubset()
        // unless the organization has "*". But it's always read-only in Survey Tool. Maybe it
        // should be treated similarly to default-content locales, but always deleted, or replaced
        // by "en_XYZ"...?
        // Reference: https://unicode-org.atlassian.net/browse/CLDR-18913
        if (hasMessage()) {
            rejectLocales(orgLocales, problems);
        }
    }

    private void rejectLocales(LocaleSet orgLocales, ProblemMap problems) {
        final Map<String, LocaleRejection> messages = getMessages();
        for (String localeId : messages.keySet()) {
            LocaleRejection rejection = messages.get(localeId);
            Solution solution = solveRejection(rejection, localeId, orgLocales);
            Problem problem = problems.map.get(localeId);
            if (problem == null) {
                problems.map.put(localeId, new Problem(rejection, 1, solution));
            } else {
                problem.addSolution(solution);
                problem.increment();
            }
        }
    }

    private Solution solveRejection(
            LocaleRejection rejection, String localeId, LocaleSet orgLocales) {
        switch (rejection) {
            case unknown:
                return solveUnknownLocale(localeId, orgLocales);
            case default_content:
                return solveDefaultContentLocale(localeId, orgLocales);
            case outside_org_coverage:
                return solveLocaleOutsideOrgCoverage(localeId, orgLocales);
            default:
                throw new RuntimeException("Rejection not handled: " + rejection);
        }
    }

    /**
     * Do not use getLikelySubtags on these; "all" would map to "all_Mlym_IN" and "und" would map to
     * "en_Latn_US"
     */
    private final Set<String> unusableNames =
            new HashSet<>(Arrays.asList("all", LocaleNames.MUL, LocaleNames.UND, LocaleNames.ROOT));

    private Solution solveUnknownLocale(String localeId, LocaleSet orgLocales) {
        if (!unusableNames.contains(localeId)) {
            // TODO: per ticket description, "The normalized code uses the alias table and the
            // likely
            // subtags table."
            // -- what does "alias table" mean? Maybe: GenerateLanguageContainment.ALIAS_MAP?
            // Reference: https://unicode-org.atlassian.net/browse/CLDR-18913
            String replacementLocaleId =
                    SupplementalDataInfo.getInstance().getLikelySubtags().get(localeId);
            if (replacementLocaleId != null && !replacementLocaleId.isEmpty()) {
                CLDRLocale repLoc = CLDRLocale.getInstance(replacementLocaleId);
                if (repLoc != null
                        && (orgLocales == null || orgLocales.contains(repLoc))
                        && !(defaultContentIsDisallowed
                                && SupplementalDataInfo.getInstance()
                                        .getDefaultContentLocales()
                                        .contains(replacementLocaleId))) {
                    return new Solution(localeId, Solution.Type.REPLACE, repLoc);
                }
            }
        }
        // TODO: possibly assign the user the full locale set for their organization
        // Reference: https://unicode-org.atlassian.net/browse/CLDR-18913
        System.out.println("In solveUnknownLocale, returning DELETE; orgLocales = " + orgLocales);
        return new Solution(localeId, Solution.Type.DELETE);
    }

    private Solution solveDefaultContentLocale(String localeId, LocaleSet orgLocales) {
        CLDRLocale loc = CLDRLocale.getInstance(localeId);
        CLDRLocale dcParent = SupplementalDataInfo.getInstance().getBaseFromDefaultContent(loc);
        if (dcParent != null) {
            String replacementLocaleId = dcParent.getBaseName();
            CLDRLocale repLoc = CLDRLocale.getInstance(replacementLocaleId);
            if (repLoc != null && (orgLocales == null || orgLocales.contains(repLoc))) {
                return new Solution(localeId, Solution.Type.REPLACE, repLoc);
            }
        }
        // TODO: possibly assign the user the full locale set for their organization
        // Reference: https://unicode-org.atlassian.net/browse/CLDR-18913
        System.out.println(
                "In solveDefaultContentLocale, returning DELETE; orgLocales = " + orgLocales);
        return new Solution(localeId, Solution.Type.DELETE);
    }

    private Solution solveLocaleOutsideOrgCoverage(String localeId, LocaleSet orgLocales) {
        // TODO: possibly assign the user the full locale set for their organization
        // Reference: https://unicode-org.atlassian.net/browse/CLDR-18913
        System.out.println(
                "In solveDefaultContentLocale, returning DELETE; orgLocales = " + orgLocales);
        return new Solution(localeId, Solution.Type.DELETE);
    }
}
