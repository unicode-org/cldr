package org.unicode.cldr.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.test.CheckCoverage;
import org.unicode.cldr.test.CheckNew;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.test.OutdatedPaths;
import org.unicode.cldr.test.SubmissionLocales;
import org.unicode.cldr.tool.Option;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.tool.ToolConstants;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.PathHeader.SectionId;
import org.unicode.cldr.util.StandardCodes.LocaleCoverageType;

import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

/**
 * Provides a HTML tables showing the important issues for vetters to review for
 * a given locale. See the main for an example. Most elements have CSS styles,
 * allowing for customization of the display.
 *
 * @author markdavis
 */
public class VettingViewer<T> {

    private static boolean SHOW_SUBTYPES = true; // CldrUtility.getProperty("SHOW_SUBTYPES", "false").equals("true");

    private static final String CONNECT_PREFIX = "₍_";
    private static final String CONNECT_SUFFIX = "₎";

    private static final String TH_AND_STYLES = "<th class='tv-th' style='text-align:left'>";

    private static final String SPLIT_CHAR = "\uFFFE";

    private static final String TEST_PATH = "//ldml/localeDisplayNames/territories/territory[@type=\"SX\"]";
    private static final double NANOSECS = 1000000000.0;
    private static final boolean TESTING = CldrUtility.getProperty("TEST", false);
    private static final boolean SHOW_ALL = CldrUtility.getProperty("SHOW", true);

    public static final Pattern ALT_PROPOSED = PatternCache.get("\\[@alt=\"[^\"]*proposed");

    public static Set<CheckCLDR.CheckStatus.Subtype> OK_IF_VOTED = EnumSet.of(Subtype.sameAsEnglishOrCode,
        Subtype.sameAsEnglishOrCode);

    public enum Choice {
        /**
         * There is a console-check error
         */
        error('E', "Error", "The Survey Tool detected an error in the winning value.", 1),
        /**
         * My choice is not the winning item
         */
        weLost(
            'L',
            "Losing",
            "The value that your organization chose (overall) is either not the winning value, or doesn’t have enough votes to be approved. "
                + "This might be due to a dispute between members of your organization.",
            2),
        /**
         * There is a dispute.
         */
        notApproved('P', "Provisional", "There are not enough votes for this item to be approved (and used).", 3),
        /**
         * There is a dispute.
         */
        hasDispute('D', "Disputed", "Different organizations are choosing different values. "
            + "Please review to approve or reach consensus.", 4),
        /**
         * There is a console-check warning
         */
        warning('W', "Warning", "The Survey Tool detected a warning about the winning value.", 5),
        /**
         * The English value for the path changed AFTER the current value for
         * the locale.
         */
        englishChanged('U', "English Changed",
            "The English value has changed in CLDR, but the corresponding value for your language has not. Check if any changes are needed in your language.",
            6),
        /**
         * The value changed from the last version of CLDR
         */
        changedOldValue('N', "New", "The winning value was altered from the last-released CLDR value. (Informational)", 7),
        /**
         * Given the users' coverage, some items are missing.
         */
        missingCoverage(
            'M',
            "Missing",
            "Your current coverage level requires the item to be present. (During the vetting phase, this is informational: you can’t add new values.)", 8),
        // /**
        // * There is a console-check error
        // */
        // other('O', "Other", "Everything else."),
        ;

        public final char abbreviation;
        public final String buttonLabel;
        public final String description;
        public final int order;

        Choice(char abbreviation, String buttonLabel, String description, int order) {
            this.abbreviation = abbreviation;
            this.buttonLabel = TransliteratorUtilities.toHTML.transform(buttonLabel);
            this.description = TransliteratorUtilities.toHTML.transform(description);
            this.order = order;

        }

        public static <T extends Appendable> T appendDisplay(Set<Choice> choices, String htmlMessage, T target) {
            try {
                boolean first = true;
                for (Choice item : choices) {
                    if (first) {
                        first = false;
                    } else {
                        target.append(", ");
                    }
                    item.appendDisplay(htmlMessage, target);
                }
                return target;
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e); // damn'd checked
                // exceptions
            }
        }

        private <T extends Appendable> void appendDisplay(String htmlMessage, T target) throws IOException {
            target.append("<span title='")
                .append(description);
            if (!htmlMessage.isEmpty()) {
                target.append(": ")
                    .append(htmlMessage);
            }
            target.append("'>")
                .append(buttonLabel)
                .append("*</span>");
        }

        public static Choice fromString(String i) {
            try {
                return valueOf(i);
            } catch (NullPointerException e) {
                throw e;
            } catch (RuntimeException e) {
                if (i.isEmpty()) {
                    throw e;
                }
                int cp = i.codePointAt(0);
                for (Choice choice : Choice.values()) {
                    if (cp == choice.abbreviation) {
                        return choice;
                    }
                }
                throw e;
            }
        }

        public static Appendable appendRowStyles(Set<Choice> choices, Appendable target) {
            try {
                target.append("hide");
                for (Choice item : choices) {
                    target.append(' ').append("vv").append(Character.toLowerCase(item.abbreviation));
                }
                return target;
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e); // damn'd checked
                // exceptions
            }
        }
    }

    public static OutdatedPaths getOutdatedPaths() {
        return outdatedPaths;
    }

    static private PathHeader.Factory pathTransform;
    static final Pattern breaks = PatternCache.get("\\|");
    static final OutdatedPaths outdatedPaths = new OutdatedPaths();

    /**
     * See VoteResolver getStatusForOrganization to see how this is computed.
     */
    public enum VoteStatus {
        /**
         * The value for the path is either contributed or approved, and
         * the user's organization didn't vote. (see class def for null user)
         */
        ok_novotes,

        /**
         * The value for the path is either contributed or approved, and
         * the user's organization chose the winning value. (see class def for null user)
         */
        ok,

        /**
         * The user's organization chose the winning value for the path, but
         * that value is neither contributed nor approved. (see class def for null user)
         */
        provisionalOrWorse,

        /**
         * The user's organization's choice is not winning. There may be
         * insufficient votes to overcome a previously approved value, or other
         * organizations may be voting against it. (see class def for null user)
         */
        losing,

        /**
         * There is a dispute, meaning more than one item with votes, or the item with votes didn't win.
         */
        disputed
    }

    /**
     * @author markdavis
     *
     * @param <T>
     */
    public static interface UsersChoice<T> {
        /**
         * Return the value that the user's organization (as a whole) voted for,
         * or null if none of the users in the organization voted for the path. <br>
         * NOTE: Would be easier if this were a method on CLDRFile.
         * NOTE: if user = null, then it must return the absolute winning value.
         *
         * @param locale
         */
        public String getWinningValueForUsersOrganization(CLDRFile cldrFile, String path, T user);

        /**
         *
         * Return the vote status
         * NOTE: if user = null, then it must disregard the user and never return losing. See VoteStatus.
         *
         * @param locale
         */
        public VoteStatus getStatusForUsersOrganization(CLDRFile cldrFile, String path, T user);
    }

    public static interface ErrorChecker {
        enum Status {
            ok, error, warning
        }

        /**
         * Initialize an error checker with a cldrFile. MUST be called before
         * any getErrorStatus.
         */
        public Status initErrorStatus(CLDRFile cldrFile);

        /**
         * Return the detailed CheckStatus information.
         */
        public List<CheckStatus> getErrorCheckStatus(String path, String value);

        /**
         * Return the status, and append the error message to the status
         * message. If there are any errors, then the warnings are not included.
         */
        public Status getErrorStatus(String path, String value, StringBuilder statusMessage);

        /**
         * Return the status, and append the error message to the status
         * message, and get the subtypes. If there are any errors, then the warnings are not included.
         */
        public Status getErrorStatus(String path, String value, StringBuilder statusMessage,
            EnumSet<Subtype> outputSubtypes);
    }

    public static class NoErrorStatus implements ErrorChecker {
        @Override
        public Status initErrorStatus(CLDRFile cldrFile) {
            return Status.ok;
        }

        @Override
        public List<CheckStatus> getErrorCheckStatus(String path, String value) {
            return Collections.emptyList();
        }

        @Override
        public Status getErrorStatus(String path, String value, StringBuilder statusMessage) {
            return Status.ok;
        }

        @Override
        public Status getErrorStatus(String path, String value, StringBuilder statusMessage,
            EnumSet<Subtype> outputSubtypes) {
            return Status.ok;
        }

    }

    public static class DefaultErrorStatus implements ErrorChecker {

        private CheckCLDR checkCldr;
        private HashMap<String, String> options = new HashMap<String, String>();
        private ArrayList<CheckStatus> result = new ArrayList<CheckStatus>();
        private CLDRFile cldrFile;
        private Factory factory;

        public DefaultErrorStatus(Factory cldrFactory) {
            this.factory = cldrFactory;
        }

        @Override
        public Status initErrorStatus(CLDRFile cldrFile) {
            this.cldrFile = cldrFile;
            options = new HashMap<String, String>();
            result = new ArrayList<CheckStatus>();
            checkCldr = CheckCLDR.getCheckAll(factory, ".*");
            checkCldr.setCldrFileToCheck(cldrFile, options, result);
            return Status.ok;
        }

        @Override
        public List<CheckStatus> getErrorCheckStatus(String path, String value) {
            String fullPath = cldrFile.getFullXPath(path);
            ArrayList<CheckStatus> result2 = new ArrayList<CheckStatus>();
            checkCldr.check(path, fullPath, value, options, result2);
            return result2;
        }

        @Override
        public Status getErrorStatus(String path, String value, StringBuilder statusMessage) {
            return getErrorStatus(path, value, statusMessage, null);
        }

        @Override
        public Status getErrorStatus(String path, String value, StringBuilder statusMessage,
            EnumSet<Subtype> outputSubtypes) {
            Status result0 = Status.ok;
            StringBuilder errorMessage = new StringBuilder();
            String fullPath = cldrFile.getFullXPath(path);
            checkCldr.check(path, fullPath, value, options, result);
            for (CheckStatus checkStatus : result) {
                final CheckCLDR cause = checkStatus.getCause();
                if (cause instanceof CheckCoverage || cause instanceof CheckNew) {
                    continue;
                }
                CheckStatus.Type statusType = checkStatus.getType();
                if (statusType.equals(CheckStatus.errorType)) {
                    // throw away any accumulated warning messages
                    if (result0 == Status.warning) {
                        errorMessage.setLength(0);
                        if (outputSubtypes != null) {
                            outputSubtypes.clear();
                        }
                    }
                    result0 = Status.error;
                    if (outputSubtypes != null) {
                        outputSubtypes.add(checkStatus.getSubtype());
                    }
                    appendToMessage(checkStatus.getMessage(), checkStatus.getSubtype(), errorMessage);
                } else if (result0 != Status.error && statusType.equals(CheckStatus.warningType)) {
                    result0 = Status.warning;
                    // accumulate all the warning messages
                    if (outputSubtypes != null) {
                        outputSubtypes.add(checkStatus.getSubtype());
                    }
                    appendToMessage(checkStatus.getMessage(), checkStatus.getSubtype(), errorMessage);
                }
            }
            if (result0 != Status.ok) {
                appendToMessage(errorMessage, statusMessage);
            }
            return result0;
        }
    }

    private final Factory cldrFactory;
    private final Factory cldrFactoryOld;
    private final CLDRFile englishFile;
    private final UsersChoice<T> userVoteStatus;
    private final SupplementalDataInfo supplementalDataInfo;
    private final String lastVersionTitle;
    private final String currentWinningTitle;
    private ErrorChecker errorChecker;

    private final Set<String> defaultContentLocales;

    /**
     * Create the Vetting Viewer.
     *
     * @param supplementalDataInfo
     * @param cldrFactory
     * @param cldrFactoryOld
     * @param lastVersionTitle
     *            The title of the last released version of CLDR.
     * @param currentWinningTitle
     *            The title of the next version of CLDR to be released.
     */
    public VettingViewer(SupplementalDataInfo supplementalDataInfo, Factory cldrFactory, Factory cldrFactoryOld,
        UsersChoice<T> userVoteStatus,
        String lastVersionTitle, String currentWinningTitle) {
        super();
        this.cldrFactory = cldrFactory;
        this.cldrFactoryOld = cldrFactoryOld;
        englishFile = cldrFactory.make("en", true);
        if (pathTransform == null) {
            pathTransform = PathHeader.getFactory(englishFile);
        }
        this.userVoteStatus = userVoteStatus;
        this.supplementalDataInfo = supplementalDataInfo;
        this.defaultContentLocales = supplementalDataInfo.getDefaultContentLocales();

        this.lastVersionTitle = lastVersionTitle;
        this.currentWinningTitle = currentWinningTitle;
        reasonsToPaths = Relation.of(new HashMap<String, Set<String>>(), HashSet.class);
        errorChecker = new DefaultErrorStatus(cldrFactory);
    }

    public class WritingInfo implements Comparable<WritingInfo> {
        public final PathHeader codeOutput;
        public final Set<Choice> problems;
        public final String htmlMessage;

        public WritingInfo(PathHeader pretty, EnumSet<Choice> problems, CharSequence htmlMessage) {
            super();
            this.codeOutput = pretty;
            this.problems = Collections.unmodifiableSet(problems.clone());
            this.htmlMessage = htmlMessage.toString();
        }

        @Override
        public int compareTo(WritingInfo other) {
            return codeOutput.compareTo(other.codeOutput);
        }

        public String getUrl(CLDRLocale locale) {
            return urls.forPathHeader(locale, codeOutput);
        }
    }

    /**
     * Show a table of values, filtering according to the choices here and in
     * the constructor.
     *
     * @param output
     * @param choices
     *            See the class description for more information.
     * @param localeId
     * @param user
     * @param usersLevel
     * @param nonVettingPhase
     */
    public void generateHtmlErrorTables(Appendable output, EnumSet<Choice> choices, String localeID, T user,
        Level usersLevel, boolean nonVettingPhase, boolean quick) {

        // Gather the relevant paths
        // each one will be marked with the choice that it triggered.
        Relation<R2<SectionId, PageId>, WritingInfo> sorted = Relation.of(
            new TreeMap<R2<SectionId, PageId>, Set<WritingInfo>>(), TreeSet.class);

        CLDRFile sourceFile = cldrFactory.make(localeID, true);

        // Initialize
        CLDRFile lastSourceFile = null;
        if (!quick) {
            try {
                lastSourceFile = cldrFactoryOld.make(localeID, true);
            } catch (Exception e) {
            }
        }

        FileInfo fileInfo = new FileInfo().getFileInfo(sourceFile, lastSourceFile, sorted, choices, localeID, nonVettingPhase, user,
            usersLevel, quick);

        // now write the results out
        writeTables(output, sourceFile, lastSourceFile, sorted, choices, localeID, nonVettingPhase, fileInfo, quick);
    }

    /**
     * Give the list of errors
     *
     * @param output
     * @param choices
     *            See the class description for more information.
     * @param localeId
     * @param user
     * @param usersLevel
     * @param nonVettingPhase
     */
    public Relation<R2<SectionId, PageId>, WritingInfo> generateFileInfoReview(Appendable output, EnumSet<Choice> choices, String localeID, T user,
        Level usersLevel, boolean nonVettingPhase, boolean quick) {

        // Gather the relevant paths
        // each one will be marked with the choice that it triggered.
        Relation<R2<SectionId, PageId>, WritingInfo> sorted = Relation.of(
            new TreeMap<R2<SectionId, PageId>, Set<WritingInfo>>(), TreeSet.class);

        CLDRFile sourceFile = cldrFactory.make(localeID, true);

        // Initialize
        CLDRFile lastSourceFile = null;
        if (!quick) {
            try {
                lastSourceFile = cldrFactoryOld.make(localeID, true);
            } catch (Exception e) {
            }
        }

        FileInfo fileInfo = new FileInfo().getFileInfo(sourceFile, lastSourceFile, sorted, choices, localeID, nonVettingPhase, user,
            usersLevel, quick);

        // now write the results out

        return sorted;
    }

    class FileInfo {
        Counter<Choice> problemCounter = new Counter<Choice>();
        Counter<Subtype> errorSubtypeCounter = new Counter<Subtype>();
        Counter<Subtype> warningSubtypeCounter = new Counter<Subtype>();
        EnumSet<Choice> problems = EnumSet.noneOf(Choice.class);

        public void addAll(FileInfo other) {
            problemCounter.addAll(other.problemCounter);
            errorSubtypeCounter.addAll(other.errorSubtypeCounter);
            warningSubtypeCounter.addAll(other.warningSubtypeCounter);
        }

        private FileInfo getFileInfo(CLDRFile sourceFile, CLDRFile lastSourceFile,
            Relation<R2<SectionId, PageId>, WritingInfo> sorted,
            EnumSet<Choice> choices, String localeID, boolean nonVettingPhase,
            T user, Level usersLevel, boolean quick) {
            return this.getFileInfo(sourceFile, lastSourceFile, sorted,
                choices, localeID, nonVettingPhase,
                user, usersLevel, quick, null);
        }

        private FileInfo getFileInfo(CLDRFile sourceFile, CLDRFile lastSourceFile,
            Relation<R2<SectionId, PageId>, WritingInfo> sorted,
            EnumSet<Choice> choices, String localeID, boolean nonVettingPhase,
            T user, Level usersLevel, boolean quick, String xpath) {

            Status status = new Status();
            errorChecker.initErrorStatus(sourceFile);
            Matcher altProposed = ALT_PROPOSED.matcher("");
            problems = EnumSet.noneOf(Choice.class);

            // now look through the paths

            StringBuilder htmlMessage = new StringBuilder();
            StringBuilder statusMessage = new StringBuilder();
            EnumSet<Subtype> subtypes = EnumSet.noneOf(Subtype.class);
            Set<String> seenSoFar = new HashSet<String>();
            boolean latin = VettingViewer.isLatinScriptLocale(sourceFile);
            for (String path : sourceFile.fullIterable()) {
                if (xpath != null && !xpath.equals(path))
                    continue;
                String value = sourceFile.getWinningValue(path);
                statusMessage.setLength(0);
                subtypes.clear();
                ErrorChecker.Status errorStatus = errorChecker.getErrorStatus(path, value, statusMessage, subtypes);

                if (quick && errorStatus != ErrorChecker.Status.error && errorStatus != ErrorChecker.Status.warning) { //skip all values but errors and warnings if in "quick" mode
                    continue;
                }

                if (seenSoFar.contains(path)) {
                    continue;
                }
                seenSoFar.add(path);
                progressCallback.nudge(); // Let the user know we're moving along.

                PathHeader pretty = pathTransform.fromPath(path);
                if (pretty.getSurveyToolStatus() == PathHeader.SurveyToolStatus.HIDE) {
                    continue;
                }

                // note that the value might be missing!

                // make sure we only look at the real values
                if (altProposed.reset(path).find()) {
                    continue;
                }

                if (path.contains("/references")) {
                    continue;
                }

                Level level = supplementalDataInfo.getCoverageLevel(path, sourceFile.getLocaleID());

                // skip anything above the requested level
                if (level.compareTo(usersLevel) > 0) {
                    continue;
                }
                
                problems.clear();
                htmlMessage.setLength(0);
                String oldValue = lastSourceFile == null ? null : lastSourceFile.getWinningValue(path);

                if (CheckCLDR.LIMITED_SUBMISSION) {
                    if (!SubmissionLocales.allowEvenIfLimited(localeID, path, errorStatus == ErrorChecker.Status.error, oldValue == null)) {
                        continue;
                    };
                }

                if (choices.contains(Choice.changedOldValue)) {
                    if (oldValue != null && !oldValue.equals(value)) {
                        problems.add(Choice.changedOldValue);
                        problemCounter.increment(Choice.changedOldValue);
                    }
                }
                VoteStatus voteStatus = userVoteStatus.getStatusForUsersOrganization(sourceFile, path, user);

                MissingStatus missingStatus = getMissingStatus(sourceFile, path, status, latin);
                if (choices.contains(Choice.missingCoverage) && missingStatus == MissingStatus.ABSENT) {
                    problems.add(Choice.missingCoverage);
                    problemCounter.increment(Choice.missingCoverage);
                }
                if (SubmissionLocales.pathAllowedInLimitedSubmission(path)) {
                    problems.add(Choice.englishChanged);
                    problemCounter.increment(Choice.englishChanged);
                }
                boolean itemsOkIfVoted = (voteStatus == VoteStatus.ok);
                if (!CheckCLDR.LIMITED_SUBMISSION && !itemsOkIfVoted && outdatedPaths.isOutdated(localeID, path)) {
                    // the outdated paths compares the base value, before
                    // data submission,
                    // so see if the value changed.
                    // String lastValue = lastSourceFile == null ? null : lastSourceFile.getWinningValue(path);
                    if (Objects.equals(value, oldValue) && choices.contains(Choice.englishChanged)) {
                        // check to see if we voted
                        problems.add(Choice.englishChanged);
                        problemCounter.increment(Choice.englishChanged);
                    }
                }

                Choice choice = errorStatus == ErrorChecker.Status.error ? Choice.error
                    : errorStatus == ErrorChecker.Status.warning ? Choice.warning
                        : null;
                if (choice == Choice.error && choices.contains(Choice.error)
                    && (!itemsOkIfVoted
                        || !OK_IF_VOTED.containsAll(subtypes))) {
                    problems.add(choice);
                    appendToMessage(statusMessage, htmlMessage);
                    problemCounter.increment(choice);
                    for (Subtype subtype : subtypes) {
                        errorSubtypeCounter.increment(subtype);
                    }
                } else if (choice == Choice.warning && choices.contains(Choice.warning)
                    && (!itemsOkIfVoted
                        || !OK_IF_VOTED.containsAll(subtypes))) {
                    problems.add(choice);
                    appendToMessage(statusMessage, htmlMessage);
                    problemCounter.increment(choice);
                    for (Subtype subtype : subtypes) {
                        warningSubtypeCounter.increment(subtype);
                    }
                }

                switch (voteStatus) {
                case losing:
                    if (choices.contains(Choice.weLost)) {
                        problems.add(Choice.weLost);
                        problemCounter.increment(Choice.weLost);
                    }
                    String usersValue = userVoteStatus.getWinningValueForUsersOrganization(sourceFile, path, user);
                    if (usersValue != null) {
                        usersValue = "Losing value: <" + TransliteratorUtilities.toHTML.transform(usersValue) + ">";
                        appendToMessage(usersValue, htmlMessage);
                    }
                    break;
                case disputed:
                    if (choices.contains(Choice.hasDispute)) {
                        problems.add(Choice.hasDispute);
                        problemCounter.increment(Choice.hasDispute);
                    }
                    break;
                case provisionalOrWorse:
                    if (missingStatus == MissingStatus.PRESENT && choices.contains(Choice.notApproved)) {
                        problems.add(Choice.notApproved);
                        problemCounter.increment(Choice.notApproved);
                    }
                    break;
                default:
                }

                if (xpath != null)
                    return this;

                if (!problems.isEmpty()) {
                    if (sorted != null) {
                        reasonsToPaths.clear();
                        R2<SectionId, PageId> group = Row.of(pretty.getSectionId(), pretty.getPageId());

                        sorted.put(group, new WritingInfo(pretty, problems, htmlMessage));
                    }
                }

            }
            return this;
        }
    }

    public static final class LocalesWithExplicitLevel implements Predicate<String> {
        private final Organization org;
        private final Level desiredLevel;

        public LocalesWithExplicitLevel(Organization org, Level level) {
            this.org = org;
            this.desiredLevel = level;
        }

        @Override
        public boolean is(String localeId) {
            Output<LocaleCoverageType> output = new Output<LocaleCoverageType>();
            // For admin - return true if SOME organization has explicit coverage for the locale
            // TODO: Make admin pick up any locale that has a vote
            if (org.equals(Organization.surveytool)) {
                for (Organization checkorg : Organization.values()) {
                    StandardCodes.make().getLocaleCoverageLevel(checkorg, localeId, output);
                    if (output.value == StandardCodes.LocaleCoverageType.explicit) {
                        return true;
                    }
                }
                return false;
            } else {
                Level level = StandardCodes.make().getLocaleCoverageLevel(org, localeId, output);
                return desiredLevel == level && output.value == StandardCodes.LocaleCoverageType.explicit;
            }
        }
    };

    public void generateSummaryHtmlErrorTables(Appendable output, EnumSet<Choice> choices,
        Predicate<String> includeLocale, T organization) {
        try {

            output
                .append("<p>The following summarizes the Priority Items across locales, " +
                    "using the default coverage levels for your organization for each locale. " +
                    "Before using, please read the instructions at " +
                    "<a target='CLDR_ST_DOCS' href='http://cldr.unicode.org/translation/vetting-summary'>Priority " +
                    "Items Summary</a>.</p>\n");

            StringBuilder headerRow = new StringBuilder();
            headerRow
                .append("<tr class='tvs-tr'>")
                .append(TH_AND_STYLES)
                .append("Locale</th>")
                .append(TH_AND_STYLES)
                .append("Codes</th>");
            for (Choice choice : choices) {
                headerRow.append("<th class='tv-th'>");
                choice.appendDisplay("", headerRow);
                headerRow.append("</th>");
            }
            headerRow.append("</tr>\n");
            String header = headerRow.toString();

            if (organization.equals(Organization.surveytool)) {
                writeSummaryTable(output, header, Level.COMPREHENSIVE, choices, organization);
            } else {
                for (Level level : Level.values()) {
                    writeSummaryTable(output, header, level, choices, organization);
                }
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e); // dang'ed checked exceptions
        }

    }

    private void writeSummaryTable(Appendable output, String header, Level desiredLevel,
        EnumSet<Choice> choices, T organization) throws IOException {

        Map<String, String> sortedNames = new TreeMap<String, String>(Collator.getInstance());

        // Gather the relevant paths
        // Each one will be marked with the choice that it triggered.

        // TODO Fix HACK
        // We are going to ignore the predicate for now, just using the locales that have explicit coverage.
        // in that locale, or allow all locales for admin@
        LocalesWithExplicitLevel includeLocale = new LocalesWithExplicitLevel((Organization) organization, desiredLevel);

        for (String localeID : cldrFactory.getAvailable()) {
            if (defaultContentLocales.contains(localeID)
                || localeID.equals("en")
                || !includeLocale.is(localeID)) {
                continue;
            }

            sortedNames.put(getName(localeID), localeID);
        }
        if (sortedNames.isEmpty()) {
            return;
        }

        EnumSet<Choice> thingsThatRequireOldFile = EnumSet.of(Choice.englishChanged, Choice.missingCoverage, Choice.changedOldValue);
        EnumSet<Choice> ourChoicesThatRequireOldFile = choices.clone();
        ourChoicesThatRequireOldFile.retainAll(thingsThatRequireOldFile);
        output.append("<h2>Level: ").append(desiredLevel.toString()).append("</h2>");
        output.append("<table class='tvs-table'>\n");
        char lastChar = ' ';
        Map<String, FileInfo> localeNameToFileInfo = new TreeMap();
        FileInfo totals = new FileInfo();

        for (Entry<String, String> entry : sortedNames.entrySet()) {
            String name = entry.getKey();
            String localeID = entry.getValue();
            // Initialize

            CLDRFile sourceFile = cldrFactory.make(localeID, true);

            CLDRFile lastSourceFile = null;
            if (!ourChoicesThatRequireOldFile.isEmpty()) {
                try {
                    lastSourceFile = cldrFactoryOld.make(localeID, true);
                } catch (Exception e) {
                }
            }
            Level level = Level.MODERN;
            if (organization != null) {
                level = StandardCodes.make().getLocaleCoverageLevel(organization.toString(), localeID);
            }
            FileInfo fileInfo = new FileInfo().getFileInfo(sourceFile, lastSourceFile, null, choices, localeID, true, organization, level, false);
            localeNameToFileInfo.put(name, fileInfo);
            totals.addAll(fileInfo);

            char nextChar = name.charAt(0);
            if (lastChar != nextChar) {
                output.append(header);
                lastChar = nextChar;
            }

            writeSummaryRow(output, choices, fileInfo.problemCounter, name, localeID);

            if (output instanceof Writer) {
                ((Writer) output).flush();
            }
        }
        output.append(header);
        writeSummaryRow(output, choices, totals.problemCounter, "Total", null);
        output.append("</table>");
        if (SHOW_SUBTYPES) {
            showSubtypes(output, sortedNames, localeNameToFileInfo, totals, true);
            showSubtypes(output, sortedNames, localeNameToFileInfo, totals, false);
        }
    }

    private void showSubtypes(Appendable output, Map<String, String> sortedNames,
        Map<String, FileInfo> localeNameToFileInfo,
        FileInfo totals,
        boolean errors) throws IOException {
        output.append("<h3>Details: ").append(errors ? "Error Types" : "Warning Types").append("</h3>");
        output.append("<table class='tvs-table'>");
        Counter<Subtype> subtypeCounterTotals = errors ? totals.errorSubtypeCounter : totals.warningSubtypeCounter;
        Set<Subtype> sortedBySize = subtypeCounterTotals.getKeysetSortedByCount(false);

        // header
        writeDetailHeader(subtypeCounterTotals, sortedBySize, output);

        // items
        for (Entry<String, FileInfo> entry : localeNameToFileInfo.entrySet()) {
            Counter<Subtype> counter = errors ? entry.getValue().errorSubtypeCounter : entry.getValue().warningSubtypeCounter;
            if (counter.getTotal() == 0) {
                continue;
            }
            String name = entry.getKey();
            String localeID = sortedNames.get(name);
            output.append("<tr>").append(TH_AND_STYLES);
            appendNameAndCode(name, localeID, output);
            output.append("</th>");
            for (Subtype subtype : sortedBySize) {
                long count = counter.get(subtype);
                output.append("<td class='tvs-count'>");
                if (count != 0) {
                    output.append(nf.format(count));
                }
                output.append("</td>");
            }
        }

        // subtotals
        writeDetailHeader(subtypeCounterTotals, sortedBySize, output);
        output.append("<tr>").append(TH_AND_STYLES).append("<i>Total</i>").append("</th>").append(TH_AND_STYLES).append("</th>");
        for (Subtype subtype : sortedBySize) {
            long count = subtypeCounterTotals.get(subtype);
            output.append("<td class='tvs-count'>");
            if (count != 0) {
                output.append("<b>").append(nf.format(count)).append("</b>");
            }
            output.append("</td>");
        }
        output.append("</table>");
    }

    private void writeDetailHeader(Counter<Subtype> subtypeCounterTotals, Set<Subtype> sortedBySize, Appendable output) throws IOException {
        output.append("<tr>")
            .append(TH_AND_STYLES).append("Name").append("</th>")
            .append(TH_AND_STYLES).append("ID").append("</th>");
        for (Subtype subtype : sortedBySize) {
            output.append(TH_AND_STYLES).append(subtype.toString()).append("</th>");
        }
    }

    private void writeSummaryRow(Appendable output, EnumSet<Choice> choices, Counter<Choice> problemCounter,
        String name, String localeID) throws IOException {
        output
            .append("<tr>")
            .append(TH_AND_STYLES);
        if (localeID == null) {
            output
                .append("<i>")
                .append(name)
                .append("</i>")
                .append("</th>")
                .append(TH_AND_STYLES);
        } else {
            appendNameAndCode(name, localeID, output);
        }
        output.append("</th>\n");
        for (Choice choice : choices) {
            long count = problemCounter.get(choice);
            output.append("<td class='tvs-count'>");
            if (localeID == null) {
                output.append("<b>");
            }
            output.append(nf.format(count));
            if (localeID == null) {
                output.append("</b>");
            }
            output.append("</td>\n");
        }
        output.append("</tr>\n");
    }

    private void appendNameAndCode(String name, String localeID, Appendable output) throws IOException {
        String[] names = name.split(SPLIT_CHAR);
        output
            .append("<a href='" + urls.forSpecial(CLDRURLS.Special.Vetting, CLDRLocale.getInstance(localeID)))
            .append("'>")
            .append(TransliteratorUtilities.toHTML.transform(names[0]))
            .append("</a>")
            .append("</th>")
            .append(TH_AND_STYLES)
            .append("<code>")
            .append(names[1])
            .append("</code>");
    }

    LanguageTagParser ltp = new LanguageTagParser();

    private String getName(String localeID) {
        Set<String> contents = supplementalDataInfo.getEquivalentsForLocale(localeID);
        // put in special character that can be split on later
        String name = englishFile.getName(localeID, true, CLDRFile.SHORT_ALTS) + SPLIT_CHAR + gatherCodes(contents);
        return name;
    }

    /**
     * Collapse the names
     {en_Cyrl, en_Cyrl_US} => en_Cyrl(_US)
     {en_GB, en_Latn_GB} => en(_Latn)_GB
     {en, en_US, en_Latn, en_Latn_US} => en(_Latn)(_US)
     {az_IR, az_Arab, az_Arab_IR} => az_IR, az_Arab(_IR)
     */
    public static String gatherCodes(Set<String> contents) {
        Set<Set<String>> source = new LinkedHashSet<Set<String>>();
        for (String s : contents) {
            source.add(new LinkedHashSet<String>(Arrays.asList(s.split("_"))));
        }
        Set<Set<String>> oldSource = new LinkedHashSet<Set<String>>();

        do {
            // exchange source/target
            oldSource.clear();
            oldSource.addAll(source);
            source.clear();
            Set<String> last = null;
            for (Set<String> ss : oldSource) {
                if (last == null) {
                    last = ss;
                } else {
                    if (ss.containsAll(last)) {
                        last = combine(last, ss);
                    } else {
                        source.add(last);
                        last = ss;
                    }
                }
            }
            source.add(last);
        } while (oldSource.size() != source.size());

        StringBuilder b = new StringBuilder();
        for (Set<String> stringSet : source) {
            if (b.length() != 0) {
                b.append(", ");
            }
            String sep = "";
            for (String string : stringSet) {
                if (string.startsWith(CONNECT_PREFIX)) {
                    b.append(string + CONNECT_SUFFIX);
                } else {
                    b.append(sep + string);
                }
                sep = "_";
            }
        }
        return b.toString();
    }

    private static Set<String> combine(Set<String> last, Set<String> ss) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (String s : ss) {
            if (last.contains(s)) {
                result.add(s);
            } else {
                result.add(CONNECT_PREFIX + s);
            }
        }
        return result;
    }

    public enum MissingStatus {
        PRESENT, ALIASED, MISSING_OK, ROOT_OK, ABSENT
    }

    /**
     * Get the MissingStatus
     *
     * @param sourceFile the CLDRFile
     * @param path the path
     * @param status used for status.pathWhereFound, also passed to getSourceLocaleIdExtended
     * @param latin boolean from isLatinScriptLocale, passed to isMissingOk
     * @return the MissingStatus
     */
    public static MissingStatus getMissingStatus(CLDRFile sourceFile, String path, Status status, boolean latin) {
        if (sourceFile == null) {
            return MissingStatus.ABSENT;
        }
        if ("root".equals(sourceFile.getLocaleID()) || path.startsWith("//ldml/layout/orientation/")) {
            return MissingStatus.MISSING_OK;
        }
        if (path.equals(TEST_PATH)) {
            int debug = 1;
        }
        MissingStatus result;

        String value = sourceFile.getStringValue(path);
        boolean isAliased = path.equals(status.pathWhereFound);

        if (value == null) {
            result = ValuePathStatus.isMissingOk(sourceFile, path, latin, isAliased) ? MissingStatus.MISSING_OK : MissingStatus.ABSENT;
        } else {
            /*
             * skipInheritanceMarker must be false for getSourceLocaleIdExtended here, since INHERITANCE_MARKER
             * may be found if there are votes for inheritance, in which case we must not skip up to "root" and
             * treat the item as missing. Reference: https://unicode.org/cldr/trac/ticket/11765
             */
            String localeFound = sourceFile.getSourceLocaleIdExtended(path, status, false /* skipInheritanceMarker */);
            /*
             * Only count it as missing IF the (localeFound is root or codeFallback)
             * AND the aliasing didn't change the path
             */
            if (localeFound.equals("root") || localeFound.equals(XMLSource.CODE_FALLBACK_ID)) {
                result = ValuePathStatus.isMissingOk(sourceFile, path, latin, isAliased)
                    || sourceFile.getLocaleID().equals("en") ? MissingStatus.ROOT_OK : MissingStatus.ABSENT;
            } else if (isAliased) {
                result = MissingStatus.ALIASED;
            } else {
                result = MissingStatus.PRESENT;
            }
        }
        return result;
    }

    public static final UnicodeSet LATIN = ValuePathStatus.LATIN;

    public static boolean isLatinScriptLocale(CLDRFile sourceFile) {
        return ValuePathStatus.isLatinScriptLocale(sourceFile);
    }

    private static StringBuilder appendToMessage(CharSequence usersValue, Subtype subtype, StringBuilder testMessage) {
        if (subtype != null) {
            usersValue = "&lt;" + subtype + "&gt; " + usersValue;
        }
        return appendToMessage(usersValue, testMessage);
    }

    private static StringBuilder appendToMessage(CharSequence usersValue, StringBuilder testMessage) {
        if (usersValue.length() == 0) {
            return testMessage;
        }
        if (testMessage.length() != 0) {
            testMessage.append("<br>");
        }
        return testMessage.append(usersValue);
    }

    static final NumberFormat nf = NumberFormat.getIntegerInstance(ULocale.ENGLISH);
    private Relation<String, String> reasonsToPaths;
    private CLDRURLS urls = CLDRConfig.getInstance().urls();

    static {
        nf.setGroupingUsed(true);
    }

    /**
     * Class that allows the relaying of progress information
     *
     * @author srl
     *
     */
    public static class ProgressCallback {
        /**
         * Note any progress. This will be called before any output is printed.
         * It will be called approximately once per xpath.
         */
        public void nudge() {
        }

        /**
         * Called when all operations are complete.
         */
        public void done() {
        }
    }

    /*
     * null instance by default
     */
    private ProgressCallback progressCallback = new ProgressCallback();

    /**
     * Select a new callback. Must be set before running.
     *
     * @return
     *
     */
    public VettingViewer<T> setProgressCallback(ProgressCallback newCallback) {
        progressCallback = newCallback;
        return this;
    }

    public ErrorChecker getErrorChecker() {
        return errorChecker;
    }

    /**
     * Select a new error checker. Must be set before running.
     *
     * @return
     *
     */
    public VettingViewer<T> setErrorChecker(ErrorChecker errorChecker) {
        this.errorChecker = errorChecker;
        return this;
    }

    /**
     * Provide the styles for inclusion into the ST &lt;head&gt; element.
     *
     * @return
     */
    public static String getHeaderStyles() {
        return "<style type='text/css'>\n"
            + ".hide {display:none}\n"
            + ".vve {}\n"
            + ".vvn {}\n"
            + ".vvp {}\n"
            + ".vvl {}\n"
            + ".vvm {}\n"
            + ".vvu {}\n"
            + ".vvw {}\n"
            + ".vvd {}\n"
            + ".vvo {}\n"
            + "</style>";
    }

    private void writeTables(Appendable output, CLDRFile sourceFile, CLDRFile lastSourceFile,
        Relation<R2<SectionId, PageId>, WritingInfo> sorted,
        EnumSet<Choice> choices,
        String localeID,
        boolean nonVettingPhase,
        FileInfo outputFileInfo,
        boolean quick) {
        try {

            boolean latin = VettingViewer.isLatinScriptLocale(sourceFile);

            Status status = new Status();

            output.append("<h2>Summary</h2>\n")
                .append("<p><i>It is important that you read " +
                    "<a target='CLDR-ST-DOCS' href='http://cldr.unicode.org/translation/vetting-view'>" +
                    "Priority Items</a> before starting!</i></p>")
                .append("<form name='checkboxes' action='#'>\n")
                .append("<table class='tvs-table'>\n")
                .append("<tr class='tvs-tr'>" +
                    "<th class='tv-th'>Count</th>" +
                    "<th class='tv-th'>Issue</th>" +
                    "<th class='tv-th'>Description</th>" +
                    "</tr>\n");

            // find the choice to check
            // OLD if !vetting and missing != 0, use missing. Otherwise pick first.
            Choice checkedItem = null;
            // if (nonVettingPhase && problemCounter.get(Choice.missingCoverage) != 0) {
            // checkedItem = Choice.missingCoverage;
            // }

            for (Choice choice : choices) {
                if (quick && choice != Choice.error && choice != Choice.warning) { //if "quick" mode, only show errors and warnings
                    continue;
                }
                long count = outputFileInfo.problemCounter.get(choice);
                output.append("<tr><td class='tvs-count'>")
                    .append(nf.format(count))
                    .append("</td>\n\t<td nowrap class='tvs-abb'>")
                    .append("<input type='checkbox' name='")
                    .append(Character.toLowerCase(choice.abbreviation))
                    .append("' onclick='setStyles()'");
                if (checkedItem == choice || checkedItem == null && count != 0) {
                    output.append(" checked");
                    checkedItem = choice;
                }
                output.append(">");
                choice.appendDisplay("", output);
                output.append("</td>\n\t<td class='tvs-desc'>")
                    .append(choice.description)
                    .append("</td></tr>\n");
            }
            output.append("</table>\n</form>\n"
                + "<script type='text/javascript'>\n" +
                "<!-- \n" +
                "setStyles()\n" +
                "-->\n"
                + "</script>");

            // gather information on choices on each page

            Relation<Row.R3<SectionId, PageId, String>, Choice> choicesForHeader = Relation.of(
                new HashMap<Row.R3<SectionId, PageId, String>, Set<Choice>>(), HashSet.class);

            Relation<Row.R2<SectionId, PageId>, Choice> choicesForSection = Relation.of(
                new HashMap<R2<SectionId, PageId>, Set<Choice>>(), HashSet.class);

            for (Entry<R2<SectionId, PageId>, Set<WritingInfo>> entry0 : sorted.keyValuesSet()) {
                SectionId section = entry0.getKey().get0();
                PageId subsection = entry0.getKey().get1();
                final Set<WritingInfo> rows = entry0.getValue();
                for (WritingInfo pathInfo : rows) {
                    String header = pathInfo.codeOutput.getHeader();
                    Set<Choice> choicesForPath = pathInfo.problems;
                    choicesForSection.putAll(Row.of(section, subsection), choicesForPath);
                    choicesForHeader.putAll(Row.of(section, subsection, header), choicesForPath);
                }
            }

            final String localeId = sourceFile.getLocaleID();
            final CLDRLocale locale = CLDRLocale.getInstance(localeId);
            int count = 0;
            for (Entry<R2<SectionId, PageId>, Set<WritingInfo>> entry0 : sorted.keyValuesSet()) {
                SectionId section = entry0.getKey().get0();
                PageId subsection = entry0.getKey().get1();
                final Set<WritingInfo> rows = entry0.getValue();

                rows.iterator().next(); // getUrl(localeId); (no side effect?)
                // http://kwanyin.unicode.org/cldr-apps/survey?_=ur&x=scripts
                // http://unicode.org/cldr-apps/survey?_=ur&x=scripts

                output.append("\n<h2 class='tv-s'>Section: ")
                    .append(section.toString())
                    .append(" — <i><a " + /*target='CLDR_ST-SECTION' */"href='")
                    .append(urls.forPage(locale, subsection))
                    .append("'>Page: ")
                    .append(subsection.toString())
                    .append("</a></i> (" + rows.size() + ")</h2>\n");
                startTable(choicesForSection.get(Row.of(section, subsection)), output);

                String oldHeader = "";
                for (WritingInfo pathInfo : rows) {
                    String header = pathInfo.codeOutput.getHeader();
                    String code = pathInfo.codeOutput.getCode();
                    String path = pathInfo.codeOutput.getOriginalPath();
                    Set<Choice> choicesForPath = pathInfo.problems;

                    if (!header.equals(oldHeader)) {
                        Set<Choice> headerChoices = choicesForHeader.get(Row.of(section, subsection, header));
                        output.append("<tr class='");
                        Choice.appendRowStyles(headerChoices, output);
                        output.append("'>\n");
                        output.append(" <th class='partsection' colSpan='6'>");
                        output.append(header);
                        output.append("</th>\n</tr>\n");
                        oldHeader = header;
                    }

                    output.append("<tr class='");
                    Choice.appendRowStyles(choicesForPath, output);
                    output.append("'>\n");
                    addCell(output, nf.format(++count), null, "tv-num", HTMLType.plain);
                    // path
                    addCell(output, code, null, "tv-code", HTMLType.plain);
                    // English value
                    if (choicesForPath.contains(Choice.englishChanged)) {
                        String winning = englishFile.getWinningValue(path);
                        String cellValue = winning == null ? "<i>missing</i>" : TransliteratorUtilities.toHTML
                            .transform(winning);
                        String previous = outdatedPaths.getPreviousEnglish(path);
                        if (previous != null) {
                            cellValue += "<br><span style='color:#900'><b>OLD: </b>"
                                + TransliteratorUtilities.toHTML.transform(previous) + "</span>";
                        } else {
                            cellValue += "<br><b><i>missing</i></b>";
                        }
                        addCell(output, cellValue, null, "tv-eng", HTMLType.markup);
                    } else {
                        addCell(output, englishFile.getWinningValue(path), null, "tv-eng", HTMLType.plain);
                    }
                    // value for last version
                    final String oldStringValue = lastSourceFile == null ? null : lastSourceFile.getWinningValue(path);
                    MissingStatus oldValueMissing = getMissingStatus(lastSourceFile, path, status, latin);

                    addCell(output, oldStringValue, null, oldValueMissing != MissingStatus.PRESENT ? "tv-miss"
                        : "tv-last", HTMLType.plain);
                    // value for last version
                    String newWinningValue = sourceFile.getWinningValue(path);
                    if (Objects.equals(newWinningValue, oldStringValue)) {
                        newWinningValue = "=";
                    }
                    addCell(output, newWinningValue, null, choicesForPath.contains(Choice.missingCoverage) ? "tv-miss"
                        : "tv-win", HTMLType.plain);
                    // Fix?
                    // http://unicode.org/cldr/apps/survey?_=az&xpath=%2F%2Fldml%2FlocaleDisplayNames%2Flanguages%2Flanguage%5B%40type%3D%22az%22%5D
                    output.append(" <td class='tv-fix'><a target='_blank' href='")
                        .append(pathInfo.getUrl(locale)) // .append(c)baseUrl + "?_=")
                        // .append(localeID)
                        // .append("&amp;xpath=")
                        // .append(percentEscape.transform(path))
                        .append("'>");
                    Choice.appendDisplay(choicesForPath, "", output);
                    // String otherUrl = pathInfo.getUrl(sourceFile.getLocaleID());
                    output.append("</a></td>");
                    // if (!otherUrl.equals(url)) {
                    // output.append("<td class='tv-test'><a "+/*target='CLDR_ST-SECTION' */"href='")
                    // .append(otherUrl)
                    // .append("'><i>Section*</i></a></td>");
                    // }
                    if (!pathInfo.htmlMessage.isEmpty()) {
                        addCell(output, pathInfo.htmlMessage, null, "tv-test", HTMLType.markup);
                    }
                    output.append("</tr>\n");
                }
                output.append("</table>\n");
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e); // damn'ed checked exceptions
        }
    }

    /**
     *
     * @param output
     * @param choices
     *            See the class description for more information.
     * @param localeId
     * @param user
     * @param usersLevel
     * @param nonVettingPhase
     */
    public ArrayList<String> getErrorOnPath(EnumSet<Choice> choices, String localeID, T user,
        Level usersLevel, boolean nonVettingPhase, String path) {

        // Gather the relevant paths
        // each one will be marked with the choice that it triggered.
        Relation<R2<SectionId, PageId>, WritingInfo> sorted = Relation.of(
            new TreeMap<R2<SectionId, PageId>, Set<WritingInfo>>(), TreeSet.class);

        CLDRFile sourceFile = cldrFactory.make(localeID, true);

        // Initialize
        CLDRFile lastSourceFile = null;
        try {
            lastSourceFile = cldrFactoryOld.make(localeID, true);
        } catch (Exception e) {
        }

        EnumSet<Choice> errors = new FileInfo().getFileInfo(sourceFile, lastSourceFile, sorted, choices, localeID, nonVettingPhase, user, usersLevel,
            false, path).problems;

        ArrayList<String> out = new ArrayList<String>();
        for (Object error : errors.toArray()) {
            out.add(((Choice) error).buttonLabel);
        }

        return out;
    }

    private void startTable(Set<Choice> choices, Appendable output) throws IOException {
        output.append("<table class='tv-table'>\n");
        output.append("<tr class='");
        Choice.appendRowStyles(choices, output);
        output.append("'>" +
            "<th class='tv-th'>No.</th>" +
            "<th class='tv-th'>Code</th>" +
            "<th class='tv-th'>English</th>" +
            "<th class='tv-th'>" + lastVersionTitle + "</th>" +
            "<th class='tv-th'>" + currentWinningTitle + "</th>" +
            "<th class='tv-th'>Fix?</th>" +
            "<th class='tv-th'>Comment</th>" +
            "</tr>\n");
    }

    enum HTMLType {
        plain, markup
    }

    private void addCell(Appendable output, String value, String title, String classValue, HTMLType htmlType)
        throws IOException {
        output.append(" <td class='")
            .append(classValue);
        if (value == null) {
            output.append(" tv-null'><i>missing</i></td>");
        } else {
            if (title != null && !title.equals(value)) {
                output.append("title='").append(TransliteratorUtilities.toHTML.transform(title)).append('\'');
            }
            output
                .append("'>")
                .append(htmlType == HTMLType.markup ? value : TransliteratorUtilities.toHTML.transform(value))
                .append("</td>\n");
        }
    }

    /**
     * Find the status of the items in the file.
     * @param file the source. Must be a resolved file, made with minimalDraftStatus = unconfirmed
     * @param pathHeaderFactory PathHeaderFactory.
     * @param foundCounter output counter of the number of paths with values having contributed or approved status
     * @param unconfirmedCounter output counter of the number of paths with values, but neither contributed nor approved status
     * @param missingCounter output counter of the number of paths without values
     * @param missingPaths output if not null, the specific paths that are missing.
     * @param unconfirmedPaths TODO
     */
    public static void getStatus(CLDRFile file, PathHeader.Factory pathHeaderFactory,
        Counter<Level> foundCounter, Counter<Level> unconfirmedCounter,
        Counter<Level> missingCounter,
        Relation<MissingStatus, String> missingPaths,
        Set<String> unconfirmedPaths) {
        getStatus(file.fullIterable(), file, pathHeaderFactory, foundCounter, unconfirmedCounter, missingCounter, missingPaths, unconfirmedPaths);
    }

    /**
     * Find the status of the items in the file.
     * @param allPaths manual list of paths
     * @param file the source. Must be a resolved file, made with minimalDraftStatus = unconfirmed
     * @param pathHeaderFactory PathHeaderFactory.
     * @param foundCounter output counter of the number of paths with values having contributed or approved status
     * @param unconfirmedCounter output counter of the number of paths with values, but neither contributed nor approved status
     * @param missingCounter output counter of the number of paths without values
     * @param missingPaths output if not null, the specific paths that are missing.
     * @param unconfirmedPaths TODO
     */
    public static void getStatus(Iterable<String> allPaths, CLDRFile file,
        PathHeader.Factory pathHeaderFactory, Counter<Level> foundCounter,
        Counter<Level> unconfirmedCounter,
        Counter<Level> missingCounter,
        Relation<MissingStatus, String> missingPaths, Set<String> unconfirmedPaths) {

        if (!file.isResolved()) {
            throw new IllegalArgumentException("File must be resolved, no minimal draft status");
        }
        foundCounter.clear();
        unconfirmedCounter.clear();
        missingCounter.clear();

        Status status = new Status();
        boolean latin = VettingViewer.isLatinScriptLocale(file);
        CoverageLevel2 coverageLevel2 = CoverageLevel2.getInstance(file.getLocaleID());

        for (String path : allPaths) {

            PathHeader ph = pathHeaderFactory.fromPath(path);
            if (ph.getSectionId() == SectionId.Special) {
                continue;
            }

            Level level = coverageLevel2.getLevel(path);
            // String localeFound = file.getSourceLocaleID(path, status);
            // String value = file.getSourceLocaleID(path, status);
            MissingStatus missingStatus = VettingViewer.getMissingStatus(file, path, status, latin);

            switch (missingStatus) {
            case ABSENT:
                missingCounter.add(level, 1);
                if (missingPaths != null && level.compareTo(Level.MODERN) <= 0) {
                    missingPaths.put(missingStatus, path);
                }
                break;
            case ALIASED:
            case PRESENT:
                String fullPath = file.getFullXPath(path);
                if (fullPath.contains("unconfirmed")
                    || fullPath.contains("provisional")) {
                    unconfirmedCounter.add(level, 1);
                    if (unconfirmedPaths != null && level.compareTo(Level.MODERN) <= 0) {
                        unconfirmedPaths.add(path);
                    }
                } else {
                    foundCounter.add(level, 1);
                }
                break;
            case MISSING_OK:
            case ROOT_OK:
                break;
            default:
                throw new IllegalArgumentException();
            }
        }
    }

    /**
     * Simple example of usage
     *
     * @param args
     * @throws IOException
     */
    final static Options myOptions = new Options();

    enum MyOptions {
        repeat(null, null, "Repeat indefinitely"), filter(".*", ".*", "Filter files"), locale(".*", "af", "Single locale for testing"), source(".*",
            CLDRPaths.MAIN_DIRECTORY, // CldrUtility.TMP2_DIRECTORY + "/vxml/common/main"
            "if summary, creates filtered version (eg -d main): does a find in the name, which is of the form dir/file"), verbose(null, null,
                "verbose debugging messages"), output(".*", CLDRPaths.GEN_DIRECTORY + "vetting/", "filter the raw files (non-summary, mostly for debugging)"),;
        // boilerplate
        final Option option;

        MyOptions(String argumentPattern, String defaultArgument, String helpText) {
            option = myOptions.add(this, argumentPattern, defaultArgument, helpText);
        }
    }

    public static void main(String[] args) throws IOException {
        SHOW_SUBTYPES = true;
        myOptions.parse(MyOptions.source, args, true);
        boolean repeat = MyOptions.repeat.option.doesOccur();
        String fileFilter = MyOptions.filter.option.getValue();
        String myOutputDir = repeat ? null : MyOptions.output.option.getValue();
        String LOCALE = MyOptions.locale.option.getValue();
        String CURRENT_MAIN = MyOptions.source.option.getValue();
        final String version = ToolConstants.PREVIOUS_CHART_VERSION;
        final String lastMain = CLDRPaths.ARCHIVE_DIRECTORY + "/cldr-" + version + "/common/main";
        //final String lastMain = CLDRPaths.ARCHIVE_DIRECTORY + "/common/main";
        do {
            Timer timer = new Timer();
            timer.start();

            Factory cldrFactory = Factory.make(CURRENT_MAIN, fileFilter);
            cldrFactory.setSupplementalDirectory(new File(CLDRPaths.SUPPLEMENTAL_DIRECTORY));
            Factory cldrFactoryOld = Factory.make(lastMain, fileFilter);
            SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo
                .getInstance(CLDRPaths.SUPPLEMENTAL_DIRECTORY);
            CheckCLDR.setDisplayInformation(cldrFactory.make("en", true));

            // FAKE this, because we don't have access to ST data

            UsersChoice<Organization> usersChoice = new UsersChoice<Organization>() {
                // Fake values for now
                public String getWinningValueForUsersOrganization(CLDRFile cldrFile, String path, Organization user) {
                    if (path.contains("USD")) {
                        return "&dummy ‘losing’ value";
                    }
                    return null; // assume we didn't vote on anything else.
                }

                // Fake values for now
                public VoteStatus getStatusForUsersOrganization(CLDRFile cldrFile, String path, Organization user) {
                    String usersValue = getWinningValueForUsersOrganization(cldrFile, path, user);
                    String winningValue = cldrFile.getWinningValue(path);
                    if (usersValue != null && !Objects.equals(usersValue, winningValue)) {
                        return VoteStatus.losing;
                    }
                    String fullPath = cldrFile.getFullXPath(path);
                    if (fullPath.contains("AMD") || fullPath.contains("unconfirmed") || fullPath.contains("provisional")) {
                        return VoteStatus.provisionalOrWorse;
                    } else if (fullPath.contains("AED")) {
                        return VoteStatus.disputed;
                    } else if (fullPath.contains("AED")) {
                        return VoteStatus.ok_novotes;
                    }
                    return VoteStatus.ok;
                }
            };

            // create the tableView and set the options desired.
            // The Options should come from a GUI; from each you can get a long
            // description and a button label.
            // Assuming user can be identified by an int
            VettingViewer<Organization> tableView = new VettingViewer<Organization>(supplementalDataInfo, cldrFactory,
                cldrFactoryOld, usersChoice, "CLDR " + version,
                "Winning Proposed");

            // here are per-view parameters

            final EnumSet<Choice> choiceSet = EnumSet.allOf(Choice.class);
            String localeStringID = LOCALE;
            int userNumericID = 666;
            Level usersLevel = Level.MODERN;
            // http: // unicode.org/cldr-apps/survey?_=ur

            if (!repeat) {
                FileCopier.ensureDirectoryExists(myOutputDir);
                FileCopier.copy(VettingViewer.class, "vettingView.css", myOutputDir);
                FileCopier.copy(VettingViewer.class, "vettingView.js", myOutputDir);
            }
            System.out.println("Creation: " + timer.getDuration() / NANOSECS + " secs");

            timer.start();
            writeFile(myOutputDir, tableView, choiceSet, "", localeStringID, userNumericID, usersLevel, CodeChoice.newCode, null);
            System.out.println("Code: " + timer.getDuration() / NANOSECS + " secs");

            timer.start();
            writeFile(myOutputDir, tableView, choiceSet, "", localeStringID, userNumericID, usersLevel, CodeChoice.summary,
                Organization.google);
            System.out.println("Summary: " + timer.getDuration() / NANOSECS + " secs");
        } while (repeat);
    }

    public enum CodeChoice {
        /** For the normal (locale) view of data **/
        newCode,
        // /** @deprecated **/
        // oldCode,
        /** For a summary view of data **/
        summary
    }

    public static void writeFile(String myOutputDir, VettingViewer<Organization> tableView, final EnumSet<Choice> choiceSet,
        String name, String localeStringID, int userNumericID,
        Level usersLevel,
        CodeChoice newCode, Organization organization)
        throws IOException {
        // open up a file, and output some of the styles to control the table
        // appearance
        PrintWriter out = myOutputDir == null ? new PrintWriter(new StringWriter())
            : FileUtilities.openUTF8Writer(myOutputDir, "vettingView"
                + name
                + (newCode == CodeChoice.newCode ? "" : newCode == CodeChoice.summary ? "-summary" : "")
                + (organization == null ? "" : "-" + organization.toString())
                + ".html");
//        FileUtilities.appendFile(VettingViewer.class, "vettingViewerHead.txt", out);
        FileCopier.copy(VettingViewer.class, "vettingViewerHead.txt", out);
        out.append(getHeaderStyles());
        out.append("</head><body>\n");

        out.println(
            "<p>Note: this is just a sample run. The user, locale, user's coverage level, and choices of tests will change the output. In a real ST page using these, the first three would "
                + "come from context, and the choices of tests would be set with radio buttons. Demo settings are: </p>\n<ol>"
                + "<li>choices: "
                + choiceSet
                + "</li><li>localeStringID: "
                + localeStringID
                + "</li><li>userNumericID: "
                + userNumericID
                + "</li><li>usersLevel: "
                + usersLevel
                + "</ol>"
                + "<p>Notes: This is a static version, using old values and faked values (L) just for testing."
                + (TESTING ? "Also, the white cell after the Fix column is just for testing." : "")
                + "</p><hr>\n");

        // now generate the table with the desired options
        // The options should come from a GUI; from each you can get a long
        // description and a button label.
        // Assuming user can be identified by an int

        switch (newCode) {
        case newCode:
            tableView.generateHtmlErrorTables(out, choiceSet, localeStringID, organization, usersLevel, SHOW_ALL, false);
            break;
        // case oldCode:
        // tableView.generateHtmlErrorTablesOld(out, choiceSet, localeStringID, userNumericID, usersLevel, SHOW_ALL);
        // break;
        case summary:
            //System.out.println(tableView.getName("zh_Hant_HK"));
            tableView.generateSummaryHtmlErrorTables(out, choiceSet, null, organization);
            break;
        }
        out.println("</body>\n</html>\n");
        out.close();
    }
}
