package org.unicode.cldr.util;

import java.io.IOException;
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
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.test.CheckCLDR.Options;
import org.unicode.cldr.test.CheckCoverage;
import org.unicode.cldr.test.CheckNew;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.test.OutdatedPaths;
import org.unicode.cldr.test.SubmissionLocales;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.PathHeader.SectionId;
import org.unicode.cldr.util.StandardCodes.LocaleCoverageType;

import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

/**
 * Provides data for the Dashboard, showing the important issues for vetters to review for
 * a given locale.
 *
 * Also provides the Priority Items Summary, which is like a Dashboard that combines multiple locales.
 *
 * @author markdavis
 */
public class VettingViewer<T> {

    private static final boolean DEBUG = false;

    private static final boolean SHOW_SUBTYPES = true; // CldrUtility.getProperty("SHOW_SUBTYPES", "false").equals("true");

    private static final String CONNECT_PREFIX = "₍_";
    private static final String CONNECT_SUFFIX = "₎";

    private static final String TH_AND_STYLES = "<th class='tv-th' style='text-align:left'>";

    private static final String SPLIT_CHAR = "\uFFFE";

    private static final boolean DEBUG_THREADS = false;

    private static final Set<CheckCLDR.CheckStatus.Subtype> OK_IF_VOTED = EnumSet.of(Subtype.sameAsEnglish);

    public static Organization getNeutralOrgForSummary() {
        return Organization.surveytool;
    }

    private static boolean orgIsNeutralForSummary(Organization org) {
        return org.equals(getNeutralOrgForSummary());
    }

    /**
     * Notification categories
     *
     * TODO: rename VettingViewer.Choice to a better name, such as VettingViewer.Category,
     * and use it consistently for instances of this type. Currently, instances of this type,
     * and strings derived from it, are confusingly named "notification", "category", "type",
     * "issue", "problem", "label", and "choice". Since it normally doesn't represent any kind of a
     * choice, the name "choice" isn't helpful. The name "notification" is potentially confusable
     * with objects of type ReviewNotification; such an object "is" a notification and it "has"
     * (belongs to) a category. Reference: https://unicode-org.atlassian.net/browse/CLDR-14906
     */
    public enum Choice {
        /**
         * There is a console-check error
         */
        error('E',
            "Error",
            "The Survey Tool detected an error in the winning value."),

        /**
         * Given the users' coverage, some items are missing
         */
        missingCoverage('M',
            "Missing",
            "Your current coverage level requires the item to be present. "
                + "(During the vetting phase, this is informational: you can’t add new values.)"),

        /**
         * Provisional: there are not enough votes to be approved
         */
        notApproved('P',
            "Provisional",
            "There are not enough votes for this item to be approved (and used)."),

        /**
         * There is a dispute.
         */
        hasDispute('D',
            "Disputed",
            "Different organizations are choosing different values. Please review to approve or reach consensus."),

        /**
         * My choice is not the winning item
         */
        weLost('L',
            "Losing",
            "The value that your organization chose (overall) is either not the winning value, or doesn’t have enough votes to be approved. "
                + "This might be due to a dispute between members of your organization."),

        /**
         * There is a console-check warning
         */
        warning('W',
            "Warning",
            "The Survey Tool detected a warning about the winning value."),

        /**
         * The English value for the path changed AFTER the current value for
         * the locale.
         */
        englishChanged('U',
            "English Changed",
            "The English value has changed in CLDR, but the corresponding value for your language has not. "
                + "Check if any changes are needed in your language."),

        /**
         * The value changed from the baseline
         */
        changedOldValue('C',
            "Changed",
            "The winning value was altered from the baseline value. (Informational)"),

        /**
         * You have abstained, or not yet voted for any value
         */
        abstained('A',
            "Abstained",
            "You have abstained, or not yet voted for any value."),
            ;

        public final char abbreviation;
        public final String buttonLabel;
        public final String jsonLabel;

        /**
         * This human-readable description is used for Priority Items Summary, which still
         * creates html on the back end. For Dashboard, identical descriptions are on the
         * front end. When Priority Items Summary is modernized to be more like Dashboard,
         * these descriptions on the back end should become unnecessary.
         */
        public final String description;

        Choice(char abbreviation, String label, String description) {
            this.abbreviation = abbreviation;
            this.jsonLabel = label.replace(' ', '_');
            this.buttonLabel = TransliteratorUtilities.toHTML.transform(label);
            this.description = TransliteratorUtilities.toHTML.transform(description);
        }

        private <T extends Appendable> void appendDisplay(T target) throws IOException {
            target.append("<span title='")
                .append(description);
            target.append("'>")
                .append(buttonLabel)
                .append("*</span>");
        }
    }

    public static OutdatedPaths getOutdatedPaths() {
        return outdatedPaths;
    }

    private static PathHeader.Factory pathTransform;
    private static final OutdatedPaths outdatedPaths = new OutdatedPaths();

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
    public interface UsersChoice<T> {
        /**
         * Return the value that the user's organization (as a whole) voted for,
         * or null if none of the users in the organization voted for the path. <br>
         * NOTE: Would be easier if this were a method on CLDRFile.
         * NOTE: if organization = null, then it must return the absolute winning value.
         */
        String getWinningValueForUsersOrganization(CLDRFile cldrFile, String path, T organization);

        /**
         * Return the vote status
         * NOTE: if organization = null, then it must disregard the organization and never return losing. See VoteStatus.
         */
        VoteStatus getStatusForUsersOrganization(CLDRFile cldrFile, String path, T organization);

        /**
         * Has the given user voted for the given path and locale?
         * @param userId
         * @param loc
         * @param path
         * @return true if that user has voted, else false
         */
        boolean userDidVote(int userId, CLDRLocale loc, String path);

        VoteResolver<String> getVoteResolver(CLDRLocale loc, String path);
    }

    public interface ErrorChecker {
        enum Status {
            ok, error, warning
        }

        /**
         * Initialize an error checker with a cldrFile. MUST be called before
         * any getErrorStatus.
         */
        Status initErrorStatus(CLDRFile cldrFile);

        /**
         * Return the detailed CheckStatus information.
         */
        List<CheckStatus> getErrorCheckStatus(String path, String value);

        /**
         * Return the status, and append the error message to the status
         * message. If there are any errors, then the warnings are not included.
         */
        Status getErrorStatus(String path, String value, StringBuilder statusMessage);

        /**
         * Return the status, and append the error message to the status
         * message, and get the subtypes. If there are any errors, then the warnings are not included.
         */
        Status getErrorStatus(String path, String value, StringBuilder statusMessage,
            EnumSet<Subtype> outputSubtypes);
    }

    private static class DefaultErrorStatus implements ErrorChecker {

        private CheckCLDR checkCldr;
        private HashMap<String, String> options = new HashMap<>();
        private ArrayList<CheckStatus> result = new ArrayList<>();
        private CLDRFile cldrFile;
        private final Factory factory;

        private DefaultErrorStatus(Factory cldrFactory) {
            this.factory = cldrFactory;
        }

        @Override
        public Status initErrorStatus(CLDRFile cldrFile) {
            this.cldrFile = cldrFile;
            options = new HashMap<>();
            result = new ArrayList<>();
            checkCldr = CheckCLDR.getCheckAll(factory, ".*");
            checkCldr.setCldrFileToCheck(cldrFile, new Options(options), result);
            return Status.ok;
        }

        @Override
        public List<CheckStatus> getErrorCheckStatus(String path, String value) {
            String fullPath = cldrFile.getFullXPath(path);
            ArrayList<CheckStatus> result2 = new ArrayList<>();
            checkCldr.check(path, fullPath, value, new CheckCLDR.Options(options), result2);
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
            checkCldr.check(path, fullPath, value, new CheckCLDR.Options(options), result);
            for (CheckStatus checkStatus : result) {
                final CheckCLDR cause = checkStatus.getCause();
                /*
                 * CheckCoverage will be shown under Missing, not under Warnings; and
                 * CheckNew will be shown under New, not under Warnings; so skip them here.
                 */
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
    private final CLDRFile englishFile;
    private final UsersChoice<T> userVoteStatus;
    private final SupplementalDataInfo supplementalDataInfo;
    private final Set<String> defaultContentLocales;

    /**
     * Create the Vetting Viewer.
     *
     * @param supplementalDataInfo
     * @param cldrFactory
     * @param userVoteStatus
     */
    public VettingViewer(SupplementalDataInfo supplementalDataInfo, Factory cldrFactory,
        UsersChoice<T> userVoteStatus) {

        super();
        this.cldrFactory = cldrFactory;
        englishFile = cldrFactory.make("en", true);
        if (pathTransform == null) {
            pathTransform = PathHeader.getFactory(englishFile);
        }
        this.userVoteStatus = userVoteStatus;
        this.supplementalDataInfo = supplementalDataInfo;
        this.defaultContentLocales = supplementalDataInfo.getDefaultContentLocales();

        reasonsToPaths = Relation.of(new HashMap<>(), HashSet.class);
    }

    public class WritingInfo implements Comparable<WritingInfo> {
        public final PathHeader codeOutput;
        public final Set<Choice> problems;
        public final String htmlMessage;
        public final Subtype subtype;

        public WritingInfo(PathHeader ph, EnumSet<Choice> problems, CharSequence htmlMessage, Subtype subtype) {
            super();
            this.codeOutput = ph;
            this.problems = Collections.unmodifiableSet(problems.clone());
            this.htmlMessage = htmlMessage.toString();
            this.subtype = subtype;
        }

        @Override
        public int compareTo(WritingInfo other) {
            return codeOutput.compareTo(other.codeOutput);
        }
    }

    public static class DashboardArgs {
        final EnumSet<Choice> choices;
        final CLDRLocale locale;
        final Level coverageLevel;
        int userId = 0;
        Organization organization = null;
        CLDRFile sourceFile = null;
        CLDRFile baselineFile = null;

        /**
         * A path like "//ldml/dates/timeZoneNames/zone[@type="America/Guadeloupe"]/short/daylight",
         * or null. If it is null, check all paths, otherwise only check this path.
         */
        String specificSinglePath = null;

        public DashboardArgs(EnumSet<Choice> choices, CLDRLocale locale, Level coverageLevel) {
            this.choices = choices;
            this.locale = locale;
            this.coverageLevel = coverageLevel;
        }

        public void setFiles(CLDRFile sourceFile, CLDRFile baselineFile) {
            this.sourceFile = sourceFile;
            this.baselineFile = baselineFile;
        }

        public void setXpath(String xpath) {
            this.specificSinglePath = xpath;
        }

        public void setUserAndOrganization(int id, Organization usersOrg) {
            this.userId = userId;
            this.organization = organization;
        }
    }

    public class DashboardData {
        public Relation<R2<SectionId, PageId>, WritingInfo> sorted = Relation.of(
            new TreeMap<R2<SectionId, PageId>, Set<WritingInfo>>(), TreeSet.class);

        public VoterProgress voterProgress = new VoterProgress();
    }

    /**
     * Generate the Dashboard
     *
     * @param args the DashboardArgs
     * @return the DashboardData
     */
    public DashboardData generateDashboard(DashboardArgs args) {

        DashboardData dd = new DashboardData();

        FileInfo fileInfo = new FileInfo(args.locale.getBaseName(), args.coverageLevel, args.choices, (T) args.organization);
        if (args.specificSinglePath != null) {
            fileInfo.setSinglePath(args.specificSinglePath);
        }
        fileInfo.setFiles(args.sourceFile, args.baselineFile);
        fileInfo.setSorted(dd.sorted);
        fileInfo.setVoterProgressAndId(dd.voterProgress, args.userId);
        fileInfo.getFileInfo();

        return dd;
    }

    private class VettingCounters {
        private final Counter<Choice> problemCounter = new Counter<>();
        private final Counter<Subtype> errorSubtypeCounter = new Counter<>();
        private final Counter<Subtype> warningSubtypeCounter = new Counter<>();

        /**
         * Combine some statistics into this VettingCounters from another VettingCounters
         *
         * This is used by Priority Items Summary to combine stats from multiple locales.
         *
         * @param other the other VettingCounters object (for a single locale)
         */
        private void addAll(VettingCounters other) {
            problemCounter.addAll(other.problemCounter);
            errorSubtypeCounter.addAll(other.errorSubtypeCounter);
            warningSubtypeCounter.addAll(other.warningSubtypeCounter);
        }
    }

    /**
     * A FileInfo contains parameters, results, and methods for gathering information about a locale
     */
    private class FileInfo {
        private final String localeId;
        private final CLDRLocale cldrLocale;
        private final Level usersLevel;
        private final EnumSet<Choice> choices;
        private final T organization;

        private FileInfo(String localeId, Level level, EnumSet<Choice> choices, T organization) {
            this.localeId = localeId;
            this.cldrLocale = CLDRLocale.getInstance(localeId);
            this.usersLevel = level;
            this.choices = choices;
            this.organization = organization;
        }

        private CLDRFile sourceFile = null;
        private CLDRFile baselineFileUnresolved = null;
        private boolean latin = false;

        private void setFiles(CLDRFile sourceFile, CLDRFile baselineFile) {
            this.sourceFile = sourceFile;
            this.baselineFileUnresolved = (baselineFile == null) ? null : baselineFile.getUnresolved();
            this.latin = VettingViewer.isLatinScriptLocale(sourceFile);
        }

        /**
         * If not null, this object gets filled in with additional information
         */
        private Relation<R2<SectionId, PageId>, WritingInfo> sorted = null;

        private void setSorted(Relation<R2<SectionId, PageId>, VettingViewer<T>.WritingInfo> sorted) {
            this.sorted = sorted;
        }

        /**
         * If voterId > 0, calculate voterProgress for the indicated user.
         */
        private int voterId = 0;
        private VoterProgress voterProgress = null;

        private void setVoterProgressAndId(VoterProgress voterProgress, int userId) {
            this.voterProgress = voterProgress;
            this.voterId = userId;
        }

        private final VettingCounters vc = new VettingCounters();
        private final EnumSet<Choice> problems = EnumSet.noneOf(Choice.class);
        private final StringBuilder htmlMessage = new StringBuilder();
        private final StringBuilder statusMessage = new StringBuilder();
        private final EnumSet<Subtype> subtypes = EnumSet.noneOf(Subtype.class);
        private final DefaultErrorStatus errorChecker = new DefaultErrorStatus(cldrFactory);

        /**
         * If not null, getFileInfo will skip all paths except this one
         */
        private String specificSinglePath = null;

        private void setSinglePath(String path) {
            this.specificSinglePath = path;
        }

        /**
         * Loop through paths for the Dashboard or the Priority Items Summary
         *
         * @return the FileInfo
         */
        private void getFileInfo() {
            if (progressCallback.isStopped()) {
                throw new RuntimeException("Requested to stop");
            }
            errorChecker.initErrorStatus(sourceFile);
            if (specificSinglePath != null) {
                handleOnePath(specificSinglePath);
                return;
            }
            Set<String> seenSoFar = new HashSet<>();
            for (String path : sourceFile.fullIterable()) {
                if (seenSoFar.contains(path)) {
                    continue;
                }
                seenSoFar.add(path);
                progressCallback.nudge(); // Let the user know we're moving along
                handleOnePath(path);
            }
        }

        private void handleOnePath(String path) {
            PathHeader ph = pathTransform.fromPath(path);
            if (ph == null || ph.shouldHide()) {
                return;
            }
            String value = sourceFile.getWinningValueForVettingViewer(path);
            statusMessage.setLength(0);
            subtypes.clear();
            ErrorChecker.Status errorStatus = errorChecker.getErrorStatus(path, value, statusMessage, subtypes);

            // note that the value might be missing!
            Level pathLevel = supplementalDataInfo.getCoverageLevel(path, localeId);

            // skip all but errors above the requested level
            boolean pathLevelIsTooHigh = pathLevel.compareTo(usersLevel) > 0;
            boolean onlyRecordErrors = pathLevelIsTooHigh;

            problems.clear();
            htmlMessage.setLength(0);

            final String oldValue = (baselineFileUnresolved == null) ? null : baselineFileUnresolved.getWinningValue(path);
            if (skipForLimitedSubmission(path, errorStatus, oldValue)) {
                return;
            }
            if (!onlyRecordErrors && choices.contains(Choice.changedOldValue)) {
                if (oldValue != null && !oldValue.equals(value)) {
                    problems.add(Choice.changedOldValue);
                    vc.problemCounter.increment(Choice.changedOldValue);
                }
            }
            VoteStatus voteStatus = userVoteStatus.getStatusForUsersOrganization(sourceFile, path, organization);
            boolean itemsOkIfVoted = (voteStatus == VoteStatus.ok);
            MissingStatus missingStatus = onlyRecordErrors ? null : recordMissingChangedEtc(path, itemsOkIfVoted, value, oldValue);
            recordChoice(errorStatus, itemsOkIfVoted, onlyRecordErrors);
            if (!onlyRecordErrors) {
                recordLosingDisputedEtc(path, voteStatus, missingStatus);
            }
            updateVotedOrAbstained(path, pathLevelIsTooHigh);
            if (!problems.isEmpty() && sorted != null) {
                reasonsToPaths.clear();
                R2<SectionId, PageId> group = Row.of(ph.getSectionId(), ph.getPageId());
                sorted.put(group, new WritingInfo(ph, problems, htmlMessage, firstSubtype()));
            }
        }

        private Subtype firstSubtype() {
            for (Subtype subtype : subtypes) {
                if (subtype != Subtype.none) {
                    return subtype;
                }
            }
            return Subtype.none;
        }

        private void updateVotedOrAbstained(String path, boolean pathLevelIsTooHigh) {
            if (voterProgress == null || voterId == 0) {
                return;
            }
            if (pathLevelIsTooHigh && problems.isEmpty()) {
                return;
            }
            voterProgress.incrementVotablePathCount();
            if (userVoteStatus.userDidVote(voterId, cldrLocale, path)) {
                voterProgress.incrementVotedPathCount();
            } else if (choices.contains(Choice.abstained)) {
                problems.add(Choice.abstained);
                vc.problemCounter.increment(Choice.abstained);
            }
        }

        private boolean skipForLimitedSubmission(String path, ErrorChecker.Status errorStatus, String oldValue) {
            if (CheckCLDR.LIMITED_SUBMISSION) {
                boolean isError = (errorStatus == ErrorChecker.Status.error);
                boolean isMissing = (oldValue == null);
                if (!SubmissionLocales.allowEvenIfLimited(localeId, path, isError, isMissing)) {
                    return true;
                }
            }
            return false;
        }

        private MissingStatus recordMissingChangedEtc(String path,
            boolean itemsOkIfVoted, String value, String oldValue) {
            VoteResolver<String> resolver = userVoteStatus.getVoteResolver(cldrLocale, path);
            MissingStatus missingStatus;
            if (resolver.getWinningStatus() == VoteResolver.Status.missing) {
                missingStatus = getMissingStatus(sourceFile, path, latin);
            } else {
                missingStatus = MissingStatus.PRESENT;
            }
            if (choices.contains(Choice.missingCoverage) && missingStatus == MissingStatus.ABSENT) {
                problems.add(Choice.missingCoverage);
                vc.problemCounter.increment(Choice.missingCoverage);
            }
            if (SubmissionLocales.pathAllowedInLimitedSubmission(path)) {
                problems.add(Choice.englishChanged);
                vc.problemCounter.increment(Choice.englishChanged);
            }
            if (!CheckCLDR.LIMITED_SUBMISSION
                && !itemsOkIfVoted && outdatedPaths.isOutdated(localeId, path)) {
                recordEnglishChanged(path, value, oldValue);
            }
            return missingStatus;
        }

        private void recordEnglishChanged(String path, String value, String oldValue) {
            if (Objects.equals(value, oldValue) && choices.contains(Choice.englishChanged)) {
                String oldEnglishValue = outdatedPaths.getPreviousEnglish(path);
                if (!OutdatedPaths.NO_VALUE.equals(oldEnglishValue)) {
                    // check to see if we voted
                    problems.add(Choice.englishChanged);
                    vc.problemCounter.increment(Choice.englishChanged);
                }
            }
        }

        private void recordChoice(ErrorChecker.Status errorStatus, boolean itemsOkIfVoted, boolean onlyRecordErrors) {
            Choice choice = errorStatus == ErrorChecker.Status.error ? Choice.error
                : errorStatus == ErrorChecker.Status.warning ? Choice.warning
                    : null;

            if (choice == Choice.error && choices.contains(Choice.error)
                && (!itemsOkIfVoted
                    || !OK_IF_VOTED.containsAll(subtypes))) {
                problems.add(choice);
                appendToMessage(statusMessage, htmlMessage);
                vc.problemCounter.increment(choice);
                for (Subtype subtype : subtypes) {
                    vc.errorSubtypeCounter.increment(subtype);
                }
            } else if (!onlyRecordErrors && choice == Choice.warning && choices.contains(Choice.warning)
                && (!itemsOkIfVoted
                    || !OK_IF_VOTED.containsAll(subtypes))) {
                problems.add(choice);
                appendToMessage(statusMessage, htmlMessage);
                vc.problemCounter.increment(choice);
                for (Subtype subtype : subtypes) {
                    vc.warningSubtypeCounter.increment(subtype);
                }
            }
        }

        private void recordLosingDisputedEtc(String path, VoteStatus voteStatus, MissingStatus missingStatus) {
            switch (voteStatus) {
            case losing:
                if (choices.contains(Choice.weLost)) {
                    problems.add(Choice.weLost);
                    vc.problemCounter.increment(Choice.weLost);
                }
                String usersValue = userVoteStatus.getWinningValueForUsersOrganization(sourceFile, path, organization);
                if (usersValue != null) {
                    usersValue = "Losing value: <" + TransliteratorUtilities.toHTML.transform(usersValue) + ">";
                    appendToMessage(usersValue, htmlMessage);
                }
                break;
            case disputed:
                if (choices.contains(Choice.hasDispute)) {
                    problems.add(Choice.hasDispute);
                    vc.problemCounter.increment(Choice.hasDispute);
                }
                break;
            case provisionalOrWorse:
                if (missingStatus == MissingStatus.PRESENT && choices.contains(Choice.notApproved)) {
                    problems.add(Choice.notApproved);
                    vc.problemCounter.increment(Choice.notApproved);
                }
                break;
            default:
            }
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
            StandardCodes sc = StandardCodes.make();
            if (orgIsNeutralForSummary(org)) {
                if (!SubmissionLocales.CLDR_LOCALES.contains(localeId)) {
                    return false;
                }
                return desiredLevel == sc.getTargetCoverageLevel(localeId);
            } else {
                Output<LocaleCoverageType> output = new Output<>();
                Level level = sc.getLocaleCoverageLevel(org, localeId, output);
                return desiredLevel == level && output.value == StandardCodes.LocaleCoverageType.explicit;
            }
        }
    }

    /**
     * Get the number of locales to be summarized for the given organization
     *
     * @param org the organization
     * @return the number of locales
     */
    public int getLocaleCount(Organization org) {
        int localeCount = 0;
        for (Level lv : Level.values()) {
            Map<String, String> sortedNames = getSortedNames(org, lv);
            localeCount += sortedNames.size();
        }
        return localeCount;
    }

    public void generatePriorityItemsSummary(Appendable output, EnumSet<Choice> choices, T organization) {
        try {
            StringBuilder headerRow = new StringBuilder();
            headerRow
                .append("<tr class='tvs-tr'>")
                .append(TH_AND_STYLES)
                .append("Locale</th>")
                .append(TH_AND_STYLES)
                .append("Codes</th>");
            for (Choice choice : choices) {
                headerRow.append("<th class='tv-th'>");
                choice.appendDisplay(headerRow);
                headerRow.append("</th>");
            }
            headerRow.append("</tr>\n");
            String header = headerRow.toString();

            for (Level level : Level.values()) {
                writeSummaryTable(output, header, level, choices, organization);
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e); // dang'ed checked exceptions
        }
    }

    /**
     * This is a context object for Vetting Viewer parallel writes.
     * It keeps track of the input locales, other parameters, as well as the output
     * streams.
     *
     * When done, appendTo() is called to append the output to the original requester.
     * @author srl
     */
    private class WriteContext {

        private final List<String> localeNames = new ArrayList<>();
        private final List<String> localeIds = new ArrayList<>();
        private final StringBuffer[] outputs;
        private final EnumSet<Choice> choices;
        private final EnumSet<Choice> ourChoicesThatRequireOldFile;
        private final T organization;
        private final VettingViewer<T>.VettingCounters totals;
        private final Map<String, VettingViewer<T>.FileInfo> localeNameToFileInfo;
        private final String header;
        private final int configChunkSize; // Number of locales to process at once, minimum 1

        private WriteContext(Set<Entry<String, String>> entrySet, EnumSet<Choice> choices, T organization, VettingCounters totals,
            Map<String, FileInfo> localeNameToFileInfo, String header) {
            for (Entry<String, String> e : entrySet) {
                localeNames.add(e.getKey());
                localeIds.add(e.getValue());
            }
            int count = localeNames.size();
            this.outputs = new StringBuffer[count];
            for (int i = 0; i < count; i++) {
                outputs[i] = new StringBuffer();
            }
            if (DEBUG_THREADS) {
                System.out.println("Initted " + this.outputs.length + " outputs");
            }

            // other data
            this.choices = choices;

            EnumSet<Choice> thingsThatRequireOldFile = EnumSet.of(Choice.englishChanged, Choice.missingCoverage, Choice.changedOldValue);
            ourChoicesThatRequireOldFile = choices.clone();
            ourChoicesThatRequireOldFile.retainAll(thingsThatRequireOldFile);

            this.organization = organization;
            this.totals = totals;
            this.localeNameToFileInfo = localeNameToFileInfo;
            this.header = header;

            if (DEBUG_THREADS) {
                System.out.println("writeContext for " + organization.toString() + " booted with " + count + " locales");
            }

            // setup env
            CLDRConfig config = CLDRConfig.getInstance();

            // parallelism. 0 means "let Java decide"
            int configParallel = Math.max(config.getProperty("CLDR_VETTINGVIEWER_PARALLEL", 0), 0);
            if (configParallel < 1) {
                configParallel = java.lang.Runtime.getRuntime().availableProcessors(); // matches ForkJoinPool() behavior
            }
            this.configChunkSize = Math.max(config.getProperty("CLDR_VETTINGVIEWER_CHUNKSIZE", 1), 1);
            if (DEBUG) {
                System.out.println("vv: CLDR_VETTINGVIEWER_PARALLEL=" + configParallel +
                    ", CLDR_VETTINGVIEWER_CHUNKSIZE=" + configChunkSize);
            }
        }

        /**
         * Append all of the results (one stream per locale) to the output parameter.
         * Insert the "header" as needed.
         * @param output
         * @throws IOException
         */
        private void appendTo(Appendable output) throws IOException {
            // all done, append all
            char lastChar = ' ';

            for (int n = 0; n < outputs.length; n++) {
                final String name = localeNames.get(n);
                if (DEBUG_THREADS) {
                    System.out.println("Appending " + name + " - " + outputs[n].length());
                }
                output.append(outputs[n]);

                char nextChar = name.charAt(0);
                if (lastChar != nextChar) {
                    output.append(this.header);
                    lastChar = nextChar;
                }
            }
        }

        /**
         * How many locales are represented in this context?
         * @return
         */
        private int size() {
            return localeNames.size();
        }
    }

    /**
     * Worker action to implement parallel Vetting Viewer writes.
     * This takes a WriteContext as a parameter, as well as a subset of the locales
     * to operate on.
     *
     * @author srl
     */
    private class WriteAction extends RecursiveAction {
        private final int length;
        private final int start;
        private final WriteContext context;

        public WriteAction(WriteContext context) {
            this(context, 0, context.size());
        }

        public WriteAction(WriteContext context, int start, int length) {
            this.context = context;
            this.start = start;
            this.length = length;
            if (DEBUG_THREADS) {
                System.out.println("writeAction(…," + start + ", " + length + ") of " + context.size() +
                    " with outputCount:" + context.outputs.length);
            }
        }

        private static final long serialVersionUID = 1L;

        @Override
        protected void compute() {
            if (length == 0) {
                return;
            } else if (length <= context.configChunkSize) {
                computeAll();
            } else {
                int split = length / 2;
                // subdivide
                invokeAll(new WriteAction(context, start, split),
                    new WriteAction(context, start + split, length - split));
            }
        }

        /**
         * Compute this entire task.
         * Can call this to run this step as a single thread.
         */
        private void computeAll() {
            // do this many at once
            for (int n = start; n < (start + length); n++) {
                computeOne(n);
            }
        }

        /**
         * Calculate the Priority Items Summary output for one locale
         * @param n
         */
        private void computeOne(int n) {
            if (progressCallback.isStopped()) {
                throw new RuntimeException("Requested to stop");
            }
            if (DEBUG) {
                MemoryHelper.availableMemory("VettingViewer.WriteAction.computeOne", true);
            }
            final String name = context.localeNames.get(n);
            final String localeID = context.localeIds.get(n);
            if (DEBUG_THREADS) {
                System.out.println("writeAction.compute(" + n + ") - " + name + ": " + localeID);
            }
            EnumSet<Choice> choices = context.choices;
            Appendable output = context.outputs[n];
            if (output == null) {
                throw new NullPointerException("output " + n + " null");
            }
            // Initialize
            CLDRFile sourceFile = cldrFactory.make(localeID, true);
            CLDRFile baselineFile = null;
            if (!context.ourChoicesThatRequireOldFile.isEmpty()) {
                try {
                    Factory baselineFactory = CLDRConfig.getInstance().getCommonAndSeedAndMainAndAnnotationsFactory();
                    baselineFile = baselineFactory.make(localeID, true);
                } catch (Exception e) {
                }
            }
            Level level = Level.MODERN;
            if (context.organization != null) {
                level = StandardCodes.make().getLocaleCoverageLevel(context.organization.toString(), localeID);
            }
            FileInfo fileInfo = new FileInfo(localeID, level, choices, context.organization);
            fileInfo.setFiles(sourceFile, baselineFile);
            fileInfo.getFileInfo();
            context.localeNameToFileInfo.put(name, fileInfo);
            context.totals.addAll(fileInfo.vc);
            if (DEBUG_THREADS) {
                System.out.println("writeAction.compute(" + n + ") - got fileinfo " + name + ": " + localeID);
            }
            try {
                writeSummaryRow(output, choices, fileInfo.vc.problemCounter, name, localeID);
                if (DEBUG_THREADS) {
                    System.out.println("writeAction.compute(" + n + ") - wrote " + name + ": " + localeID);
                }

            } catch (IOException e) {
                System.err.println("writeAction.compute(" + n + ") - writeexc " + name + ": " + localeID);
                this.completeExceptionally(new RuntimeException("While writing " + localeID, e));
            }
            if (DEBUG) {
                System.out.println("writeAction.compute(" + n + ") - DONE " + name + ": " + localeID);
            }
        }
    }

    /**
     * Write the table for the Priority Items Summary
     * @param output
     * @param header
     * @param desiredLevel
     * @param choices
     * @param organization
     * @throws IOException
     */
    private void writeSummaryTable(Appendable output, String header, Level desiredLevel,
        EnumSet<Choice> choices, T organization) throws IOException {
        Map<String, String> sortedNames = getSortedNames((Organization) organization, desiredLevel);
        if (sortedNames.isEmpty()) {
            return;
        }
        output.append("<h2>Level: ").append(desiredLevel.toString()).append("</h2>");
        output.append("<table class='tvs-table'>\n");
        Map<String, FileInfo> localeNameToFileInfo = new TreeMap<>();

        VettingCounters totals = new VettingCounters();

        Set<Entry<String, String>> entrySet = sortedNames.entrySet();

        WriteContext context = this.new WriteContext(entrySet, choices, organization, totals, localeNameToFileInfo, header);

        WriteAction writeAction = this.new WriteAction(context);
        if (USE_FORKJOIN) {
            ForkJoinPool.commonPool().invoke(writeAction);
        } else {
            if (DEBUG) {
                System.out.println("WARNING: calling writeAction.computeAll(), as the ForkJoinPool is disabled.");
            }
            writeAction.computeAll();
        }
        context.appendTo(output); // write all of the results together
        output.append(header); // add one header at the bottom
        writeSummaryRow(output, choices, totals.problemCounter, "Total", null);
        output.append("</table>");
        if (SHOW_SUBTYPES) {
            showSubtypes(output, sortedNames, localeNameToFileInfo, totals, true);
            showSubtypes(output, sortedNames, localeNameToFileInfo, totals, false);
        }
    }

    private Map<String, String> getSortedNames(Organization org, Level desiredLevel) {
        Map<String, String> sortedNames = new TreeMap<>(CLDRConfig.getInstance().getCollator());
        // TODO Fix HACK
        // We are going to ignore the predicate for now, just using the locales that have explicit coverage.
        // in that locale, or allow all locales for admin@
        LocalesWithExplicitLevel includeLocale = new LocalesWithExplicitLevel(org, desiredLevel);

        for (String localeID : cldrFactory.getAvailable()) {
            if (defaultContentLocales.contains(localeID)
                || localeID.equals("en")
                || !includeLocale.is(localeID)) {
                continue;
            }
            sortedNames.put(getName(localeID), localeID);
        }
        return sortedNames;
    }

    private final boolean USE_FORKJOIN = false;

    private void showSubtypes(Appendable output, Map<String, String> sortedNames,
        Map<String, FileInfo> localeNameToFileInfo,
        VettingCounters totals, boolean errors) throws IOException {

        output.append("<h3>Details: ").append(errors ? "Error Types" : "Warning Types").append("</h3>");
        output.append("<table class='tvs-table'>");
        Counter<Subtype> subtypeCounterTotals = errors ? totals.errorSubtypeCounter : totals.warningSubtypeCounter;
        Set<Subtype> sortedBySize = subtypeCounterTotals.getKeysetSortedByCount(false);

        // header
        writeDetailHeader(sortedBySize, output);

        // items
        for (Entry<String, FileInfo> entry : localeNameToFileInfo.entrySet()) {
            Counter<Subtype> counter = errors ? entry.getValue().vc.errorSubtypeCounter : entry.getValue().vc.warningSubtypeCounter;
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
        writeDetailHeader(sortedBySize, output);
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

    private void writeDetailHeader(Set<Subtype> sortedBySize, Appendable output) throws IOException {
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
        // See https://unicode-org.atlassian.net/browse/CLDR-15279
        String url = "v#/" + localeID + "//";
        String[] names = name.split(SPLIT_CHAR);
        output
            .append("<a href='" + url)
            .append("'>")
            .append(TransliteratorUtilities.toHTML.transform(names[0]))
            .append("</a>")
            .append("</th>")
            .append(TH_AND_STYLES)
            .append("<code>")
            .append(names[1])
            .append("</code>");
    }

    private String getName(String localeID) {
        Set<String> contents = supplementalDataInfo.getEquivalentsForLocale(localeID);
        // put in special character that can be split on later
        return englishFile.getName(localeID, true, CLDRFile.SHORT_ALTS) + SPLIT_CHAR + gatherCodes(contents);
    }

    /**
     * Collapse the names
     {en_Cyrl, en_Cyrl_US} => en_Cyrl(_US)
     {en_GB, en_Latn_GB} => en(_Latn)_GB
     {en, en_US, en_Latn, en_Latn_US} => en(_Latn)(_US)
     {az_IR, az_Arab, az_Arab_IR} => az_IR, az_Arab(_IR)
     */
    private static String gatherCodes(Set<String> contents) {
        Set<Set<String>> source = new LinkedHashSet<>();
        for (String s : contents) {
            source.add(new LinkedHashSet<>(Arrays.asList(s.split("_"))));
        }
        Set<Set<String>> oldSource = new LinkedHashSet<>();

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
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String s : ss) {
            if (last.contains(s)) {
                result.add(s);
            } else {
                result.add(CONNECT_PREFIX + s);
            }
        }
        return result;
    }

    /**
     * Used to determine what the status of a particular path's value is in a given locale.
     */
    public enum MissingStatus {
        /**
         * There is an explicit value for the path, including ↑↑↑,
         * or there is an inherited value (but not including the ABSENT conditions, e.g. not from root).
         */
        PRESENT,

        /**
         * The value is inherited from a different path. Only applies if the parent is not root.
         * That path might be in the same locale or from a parent (but not root or CODE_FALLBACK).
         */
        ALIASED,

        /**
         * See ABSENT
         */
        MISSING_OK,

        /**
         * See ABSENT
         */
        ROOT_OK,

        /**
         * The supplied CLDRFile is null, or the value is null, or the value is inherited from root or CODE_FALLBACK.
         * A special ValuePathStatus.isMissingOk method allows for some exceptions, changing the result to  MISSING_OK or ROOT_OK.
         */
        ABSENT
    }

    /**
     * Get the MissingStatus: for details see the javadoc for MissingStatus.
     *
     * @param sourceFile the CLDRFile
     * @param path the path
     * @param latin boolean from isLatinScriptLocale, passed to isMissingOk
     * @return the MissingStatus
     */
    public static MissingStatus getMissingStatus(CLDRFile sourceFile, String path, boolean latin) {
        if (sourceFile == null) {
            return MissingStatus.ABSENT;
        }
        final String sourceLocaleID = sourceFile.getLocaleID();
        if ("root".equals(sourceLocaleID)) {
            return MissingStatus.MISSING_OK;
        }
        MissingStatus result;

        String value = sourceFile.getStringValue(path);
        Status status = new Status();
        String sourceLocale = sourceFile.getSourceLocaleIdExtended(path, status, false); // does not skip inheritance marker

        boolean isAliased = !path.equals(status.pathWhereFound);
        if (DEBUG) {
            if (path.equals("//ldml/characterLabels/characterLabelPattern[@type=\"subscript\"]")) {
                int debug = 0;
            }
            if (!isAliased && !sourceLocale.equals(sourceLocaleID)) {
                int debug = 0;
            }
        }

        if (value == null) {
            result = ValuePathStatus.isMissingOk(sourceFile, path, latin, isAliased) ? MissingStatus.MISSING_OK
                : MissingStatus.ABSENT;
        } else {
            /*
             * skipInheritanceMarker must be false for getSourceLocaleIdExtended here, since INHERITANCE_MARKER
             * may be found if there are votes for inheritance, in which case we must not skip up to "root" and
             * treat the item as missing. Reference: https://unicode.org/cldr/trac/ticket/11765
             */
            String localeFound = sourceFile.getSourceLocaleIdExtended(path, status, false /* skipInheritanceMarker */);
            final boolean localeFoundIsRootOrCodeFallback = localeFound.equals("root")
                || localeFound.equals(XMLSource.CODE_FALLBACK_ID);
            final boolean isParentRoot = CLDRLocale.getInstance(sourceFile.getLocaleID()).isParentRoot();
            /*
             * Only count it as missing IF the (localeFound is root or codeFallback)
             * AND the aliasing didn't change the path.
             * Note that localeFound will be where an item with ↑↑↑ was found even though
             * the resolved value is actually inherited from somewhere else.
             */

            if (localeFoundIsRootOrCodeFallback) {
                result = ValuePathStatus.isMissingOk(sourceFile, path, latin, isAliased) ? MissingStatus.ROOT_OK
                    : isParentRoot ? MissingStatus.ABSENT
                        : MissingStatus.ALIASED;
            } else if (!isAliased) {
                result = MissingStatus.PRESENT;
            } else if (isParentRoot) { // We handle ALIASED specially, depending on whether the parent is root or not.
                result = ValuePathStatus.isMissingOk(sourceFile, path, latin, isAliased) ? MissingStatus.MISSING_OK
                    : MissingStatus.ABSENT;
            } else {
                result = MissingStatus.ALIASED;
            }
        }
        return result;
    }

    public static final UnicodeSet LATIN = ValuePathStatus.LATIN;

    public static boolean isLatinScriptLocale(CLDRFile sourceFile) {
        return ValuePathStatus.isLatinScriptLocale(sourceFile);
    }

    private static void appendToMessage(CharSequence usersValue, Subtype subtype, StringBuilder testMessage) {
        if (subtype != null) {
            usersValue = "&lt;" + subtype + "&gt; " + usersValue;
        }
        appendToMessage(usersValue, testMessage);
    }

    private static void appendToMessage(CharSequence usersValue, StringBuilder testMessage) {
        if (usersValue.length() == 0) {
            return;
        }
        if (testMessage.length() != 0) {
            testMessage.append("<br>");
        }
        testMessage.append(usersValue);
    }

    static final NumberFormat nf = NumberFormat.getIntegerInstance(ULocale.ENGLISH);
    private final Relation<String, String> reasonsToPaths;

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

        /**
         * Return true to cause an early stop.
         * @return
         */
        public boolean isStopped() {
            return false;
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

    /**
     * Provide the styles for inclusion into the ST &lt;head&gt; element.
     *
     * @return
     */
    public static String getHeaderStyles() {
        return "<style>\n"
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

    /**
     * Find the status of all the paths in the input file. See the full getStatus for more information.
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
     * Find the status of an input set of paths in the input file.
     * It partitions the returned data according to the Coverage levels.
     * NOTE: MissingStatus.ALIASED is handled specially; it is mapped to ABSENT if the parent is root, and otherwise mapped to PRESENT.
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

        boolean latin = VettingViewer.isLatinScriptLocale(file);
        CoverageLevel2 coverageLevel2 = CoverageLevel2.getInstance(SupplementalDataInfo.getInstance(), file.getLocaleID());

        for (String path : allPaths) {

            PathHeader ph = pathHeaderFactory.fromPath(path);
            if (ph.getSectionId() == SectionId.Special) {
                continue;
            }

            Level level = coverageLevel2.getLevel(path);
            MissingStatus missingStatus = VettingViewer.getMissingStatus(file, path, latin);

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

    public static EnumSet<VettingViewer.Choice> getChoiceSetForOrg(Organization usersOrg) {
        EnumSet<VettingViewer.Choice> choiceSet = EnumSet.allOf(VettingViewer.Choice.class);
        if (orgIsNeutralForSummary(usersOrg)) {
            choiceSet = EnumSet.of(
                VettingViewer.Choice.error,
                VettingViewer.Choice.warning,
                VettingViewer.Choice.hasDispute,
                VettingViewer.Choice.notApproved);
                // skip missingCoverage, weLost, englishChanged, changedOldValue, abstained
        }
        return choiceSet;
    }
}
