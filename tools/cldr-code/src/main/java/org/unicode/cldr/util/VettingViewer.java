package org.unicode.cldr.util;

import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.test.CheckCoverage;
import org.unicode.cldr.test.CheckNew;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.test.OutdatedPaths;
import org.unicode.cldr.test.SubmissionLocales;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.PathHeader.SectionId;
import org.unicode.cldr.util.StandardCodes.LocaleCoverageType;

/**
 * Provides data for the Dashboard, showing the important issues for vetters to review for a given
 * locale.
 *
 * <p>Also provides the Priority Items Summary, which is like a Dashboard that combines multiple
 * locales.
 *
 * @author markdavis
 */
public class VettingViewer<T> {

    private static final boolean DEBUG = false;

    private static final boolean SHOW_SUBTYPES = false;

    private static final String CONNECT_PREFIX = "₍_";
    private static final String CONNECT_SUFFIX = "₎";

    private static final String TH_AND_STYLES = "<th class='tv-th' style='text-align:left'>";

    private static final String SPLIT_CHAR = "\uFFFE";

    private static final boolean DEBUG_THREADS = false;

    private static final Set<CheckCLDR.CheckStatus.Subtype> OK_IF_VOTED =
            EnumSet.of(Subtype.sameAsEnglish);

    public static Organization getNeutralOrgForSummary() {
        return Organization.surveytool;
    }

    private static boolean orgIsNeutralForSummary(Organization org) {
        return org.equals(getNeutralOrgForSummary());
    }

    private LocaleBaselineCount localeBaselineCount = null;

    public void setLocaleBaselineCount(LocaleBaselineCount localeBaselineCount) {
        this.localeBaselineCount = localeBaselineCount;
    }

    public static OutdatedPaths getOutdatedPaths() {
        return outdatedPaths;
    }

    private static PathHeader.Factory pathTransform;
    private static final OutdatedPaths outdatedPaths = new OutdatedPaths();

    /**
     * @author markdavis
     * @param <T>
     */
    public interface UsersChoice<T> {
        /**
         * Return the value that the user's organization (as a whole) voted for, or null if none of
         * the users in the organization voted for the path. <br>
         * NOTE: Would be easier if this were a method on CLDRFile. NOTE: if organization = null,
         * then it must return the absolute winning value.
         */
        String getWinningValueForUsersOrganization(CLDRFile cldrFile, String path, T organization);

        /**
         * Return the vote status NOTE: if organization = null, then it must disregard the
         * organization and never return losing. See VoteStatus.
         */
        VoteResolver.VoteStatus getStatusForUsersOrganization(
                CLDRFile cldrFile, String path, T organization);

        /**
         * Has the given user voted for the given path and locale?
         *
         * @param userId
         * @param loc
         * @param path
         * @return true if that user has voted, else false
         */
        boolean userDidVote(int userId, CLDRLocale loc, String path);

        VoteResolver<String> getVoteResolver(CLDRFile baselineFile, CLDRLocale loc, String path);

        VoteType getUserVoteType(int userId, CLDRLocale loc, String path);
    }

    public interface ErrorChecker {
        enum Status {
            ok,
            error,
            warning
        }

        /**
         * Initialize an error checker with a cldrFile. MUST be called before any getErrorStatus.
         */
        Status initErrorStatus(CLDRFile cldrFile);

        /** Return the detailed CheckStatus information. */
        List<CheckStatus> getErrorCheckStatus(String path, String value);

        /**
         * Return the status, and append the error message to the status message. If there are any
         * errors, then the warnings are not included.
         */
        Status getErrorStatus(String path, String value, StringBuilder statusMessage);

        /**
         * Return the status, and append the error message to the status message, and get the
         * subtypes. If there are any errors, then the warnings are not included.
         */
        Status getErrorStatus(
                String path,
                String value,
                StringBuilder statusMessage,
                EnumSet<Subtype> outputSubtypes);
    }

    private static class DefaultErrorStatus implements ErrorChecker {

        private CheckCLDR checkCldr;
        private CheckCLDR.Options options = null;
        private ArrayList<CheckStatus> result = new ArrayList<>();
        private CLDRFile cldrFile;
        private final Factory factory;

        private DefaultErrorStatus(Factory cldrFactory) {
            this.factory = cldrFactory;
        }

        @Override
        public Status initErrorStatus(CLDRFile cldrFile) {
            this.cldrFile = cldrFile;
            options = new CheckCLDR.Options(CLDRLocale.getInstance(cldrFile.getLocaleID()));
            result = new ArrayList<>();
            // test initialization is handled by TestCache
            return Status.ok;
        }

        @Override
        public List<CheckStatus> getErrorCheckStatus(String path, String value) {
            ArrayList<CheckStatus> result2 = new ArrayList<>();
            factory.getTestCache().getBundle(options).check(path, result2, value);
            return result2;
        }

        @Override
        public Status getErrorStatus(String path, String value, StringBuilder statusMessage) {
            return getErrorStatus(path, value, statusMessage, null);
        }

        @Override
        public Status getErrorStatus(
                String path,
                String value,
                StringBuilder statusMessage,
                EnumSet<Subtype> outputSubtypes) {
            Status result0 = Status.ok;
            StringBuilder errorMessage = new StringBuilder();
            factory.getTestCache().getBundle(options).check(path, result, value);
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
                    appendToMessage(
                            checkStatus.getMessage(), checkStatus.getSubtype(), errorMessage);
                } else if (result0 != Status.error && statusType.equals(CheckStatus.warningType)) {
                    result0 = Status.warning;
                    // accumulate all the warning messages
                    if (outputSubtypes != null) {
                        outputSubtypes.add(checkStatus.getSubtype());
                    }
                    appendToMessage(
                            checkStatus.getMessage(), checkStatus.getSubtype(), errorMessage);
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
    public VettingViewer(
            SupplementalDataInfo supplementalDataInfo,
            Factory cldrFactory,
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
        public final Set<NotificationCategory> problems;
        public final String htmlMessage;
        public final Subtype subtype;

        public WritingInfo(
                PathHeader ph,
                EnumSet<NotificationCategory> problems,
                CharSequence htmlMessage,
                Subtype subtype) {
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

    public class DashboardData {
        public Relation<R2<SectionId, PageId>, WritingInfo> sorted =
                Relation.of(new TreeMap<R2<SectionId, PageId>, Set<WritingInfo>>(), TreeSet.class);

        public VoterProgress voterProgress = new VoterProgress();
    }

    /**
     * Generate the Dashboard
     *
     * @param args the DashboardArgs
     * @return the DashboardData
     */
    public DashboardData generateDashboard(VettingParameters args) {

        DashboardData dd = new DashboardData();

        FileInfo fileInfo =
                new FileInfo(
                        args.locale.getBaseName(),
                        args.coverageLevel,
                        args.choices,
                        (T) args.organization);
        if (args.specificSinglePath != null) {
            fileInfo.setSinglePath(args.specificSinglePath);
        }
        fileInfo.setFiles(args.sourceFile, args.baselineFile);
        fileInfo.setSorted(dd.sorted);
        fileInfo.setVoterProgressAndId(dd.voterProgress, args.userId);
        fileInfo.getFileInfo();

        return dd;
    }

    public LocaleCompletionData generateLocaleCompletion(VettingParameters args) {
        if (!args.sourceFile.isResolved()) {
            throw new IllegalArgumentException("File must be resolved for locale completion");
        }
        FileInfo fileInfo =
                new FileInfo(
                        args.locale.getBaseName(),
                        args.coverageLevel,
                        args.choices,
                        (T) args.organization);
        fileInfo.setFiles(args.sourceFile, args.baselineFile);
        fileInfo.getFileInfo();
        return new LocaleCompletionData(fileInfo.vc.problemCounter);
    }

    private class VettingCounters {
        private final Counter<NotificationCategory> problemCounter = new Counter<>();
        private final Counter<Subtype> errorSubtypeCounter = new Counter<>();
        private final Counter<Subtype> warningSubtypeCounter = new Counter<>();

        /**
         * Combine some statistics into this VettingCounters from another VettingCounters
         *
         * <p>This is used by Priority Items Summary to combine stats from multiple locales.
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
        private final EnumSet<NotificationCategory> choices;
        private final T organization;

        private FileInfo(
                String localeId,
                Level level,
                EnumSet<NotificationCategory> choices,
                T organization) {
            this.localeId = localeId;
            this.cldrLocale = CLDRLocale.getInstance(localeId);
            this.usersLevel = level;
            this.choices = choices;
            this.organization = organization;
        }

        private CLDRFile sourceFile = null;
        private CLDRFile baselineFile = null;
        private CLDRFile baselineFileUnresolved = null;
        private boolean latin = false;

        private void setFiles(CLDRFile sourceFile, CLDRFile baselineFile) {
            this.sourceFile = sourceFile;
            this.baselineFile = baselineFile;
            this.baselineFileUnresolved =
                    (baselineFile == null) ? null : baselineFile.getUnresolved();
            this.latin = VettingViewer.isLatinScriptLocale(sourceFile);
        }

        /** If not null, this object gets filled in with additional information */
        private Relation<R2<SectionId, PageId>, WritingInfo> sorted = null;

        private void setSorted(
                Relation<R2<SectionId, PageId>, VettingViewer<T>.WritingInfo> sorted) {
            this.sorted = sorted;
        }

        /** If voterId > 0, calculate voterProgress for the indicated user. */
        private int voterId = 0;

        private VoterProgress voterProgress = null;

        private void setVoterProgressAndId(VoterProgress voterProgress, int userId) {
            this.voterProgress = voterProgress;
            this.voterId = userId;
        }

        private final VettingCounters vc = new VettingCounters();
        private final EnumSet<NotificationCategory> problems =
                EnumSet.noneOf(NotificationCategory.class);
        private final StringBuilder htmlMessage = new StringBuilder();
        private final StringBuilder statusMessage = new StringBuilder();
        private final EnumSet<Subtype> subtypes = EnumSet.noneOf(Subtype.class);
        private final DefaultErrorStatus errorChecker = new DefaultErrorStatus(cldrFactory);

        /** If not null, getFileInfo will skip all paths except this one */
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
            String value = sourceFile.getWinningValue(path);
            statusMessage.setLength(0);
            subtypes.clear();
            ErrorChecker.Status errorStatus =
                    errorChecker.getErrorStatus(path, value, statusMessage, subtypes);

            // note that the value might be missing!
            Level pathLevel = supplementalDataInfo.getCoverageLevel(path, localeId);

            // skip all but errors above the requested level
            boolean pathLevelIsTooHigh = pathLevel.compareTo(usersLevel) > 0;
            boolean onlyRecordErrors = pathLevelIsTooHigh;

            problems.clear();
            htmlMessage.setLength(0);

            final String oldValue =
                    (baselineFileUnresolved == null)
                            ? null
                            : baselineFileUnresolved.getWinningValue(path);
            if (skipForLimitedSubmission(path, errorStatus, oldValue)) {
                return;
            }
            if (!onlyRecordErrors
                    && choices.contains(NotificationCategory.changedOldValue)
                    && changedFromBaseline(path, value, oldValue, sourceFile)) {
                problems.add(NotificationCategory.changedOldValue);
                vc.problemCounter.increment(NotificationCategory.changedOldValue);
            }
            if (!onlyRecordErrors
                    && choices.contains(NotificationCategory.inheritedChanged)
                    && inheritedChangedFromBaseline(path, value, sourceFile)) {
                problems.add(NotificationCategory.inheritedChanged);
                vc.problemCounter.increment(NotificationCategory.inheritedChanged);
            }
            VoteResolver.VoteStatus voteStatus =
                    userVoteStatus.getStatusForUsersOrganization(sourceFile, path, organization);
            boolean itemsOkIfVoted = (voteStatus == VoteResolver.VoteStatus.ok);
            MissingStatus missingStatus =
                    onlyRecordErrors
                            ? null
                            : recordMissingChangedEtc(path, itemsOkIfVoted, value, oldValue);
            recordChoice(errorStatus, itemsOkIfVoted, onlyRecordErrors);
            if (!onlyRecordErrors) {
                recordLosingDisputedEtc(path, voteStatus, missingStatus);
            }
            if (pathLevelIsTooHigh && problems.isEmpty()) {
                return;
            }
            updateVotedOrAbstained(path);

            if (!problems.isEmpty() && sorted != null) {
                reasonsToPaths.clear();
                R2<SectionId, PageId> group = Row.of(ph.getSectionId(), ph.getPageId());
                sorted.put(group, new WritingInfo(ph, problems, htmlMessage, firstSubtype()));
            }
        }

        private boolean changedFromBaseline(
                String path, String value, String oldValue, CLDRFile sourceFile) {
            if (oldValue != null && !oldValue.equals(value)) {
                if (CldrUtility.INHERITANCE_MARKER.equals(oldValue)) {
                    String baileyValue = sourceFile.getBaileyValue(path, null, null);
                    if (baileyValue != null && baileyValue.equals(value)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        private boolean inheritedChangedFromBaseline(
                String path, String value, CLDRFile sourceFile) {
            Output<String> pathWhereFound = new Output<>();
            Output<String> localeWhereFound = new Output<>();
            String baileyValue = sourceFile.getBaileyValue(path, pathWhereFound, localeWhereFound);
            if (baileyValue == null
                    || GlossonymConstructor.PSEUDO_PATH.equals(pathWhereFound.toString())
                    || XMLSource.ROOT_ID.equals(localeWhereFound.toString())
                    || XMLSource.CODE_FALLBACK_ID.equals(localeWhereFound.toString())) {
                return false;
            }
            if (!baileyValue.equals(value) && !CldrUtility.INHERITANCE_MARKER.equals(value)) {
                return false;
            }
            String baselineInheritedValue;
            if (localeWhereFound.toString().equals(localeId)) { // sideways inheritance
                baselineInheritedValue = baselineFile.getWinningValue(pathWhereFound.toString());
            } else { // inheritance from other locale
                Factory baselineFactory =
                        CLDRConfig.getInstance().getCommonAndSeedAndMainAndAnnotationsFactory();
                CLDRFile parentFile = baselineFactory.make(localeWhereFound.toString(), true);
                baselineInheritedValue = parentFile.getWinningValue(pathWhereFound.toString());
            }
            return !baileyValue.equals(baselineInheritedValue);
        }

        private Subtype firstSubtype() {
            for (Subtype subtype : subtypes) {
                if (subtype != Subtype.none) {
                    return subtype;
                }
            }
            return Subtype.none;
        }

        private void updateVotedOrAbstained(String path) {
            if (voterProgress == null || voterId == 0) {
                return;
            }
            voterProgress.incrementVotablePathCount();
            if (userVoteStatus.userDidVote(voterId, cldrLocale, path)) {
                VoteType voteType = userVoteStatus.getUserVoteType(voterId, cldrLocale, path);
                voterProgress.incrementVotedPathCount(voteType);
            } else if (choices.contains(NotificationCategory.abstained)) {
                problems.add(NotificationCategory.abstained);
                vc.problemCounter.increment(NotificationCategory.abstained);
            }
        }

        private boolean skipForLimitedSubmission(
                String path, ErrorChecker.Status errorStatus, String oldValue) {
            if (CheckCLDR.LIMITED_SUBMISSION) {
                boolean isError = (errorStatus == ErrorChecker.Status.error);
                boolean isMissing = (oldValue == null);
                if (!SubmissionLocales.allowEvenIfLimited(localeId, path, isError, isMissing)) {
                    return true;
                }
            }
            return false;
        }

        private MissingStatus recordMissingChangedEtc(
                String path, boolean itemsOkIfVoted, String value, String oldValue) {
            VoteResolver<String> resolver =
                    userVoteStatus.getVoteResolver(baselineFile, cldrLocale, path);
            MissingStatus missingStatus;
            if (resolver.getWinningStatus() == VoteResolver.Status.missing) {
                missingStatus = getMissingStatus(sourceFile, path, latin);
            } else {
                missingStatus = MissingStatus.PRESENT;
            }
            if (choices.contains(NotificationCategory.missingCoverage)
                    && missingStatus == MissingStatus.ABSENT) {
                problems.add(NotificationCategory.missingCoverage);
                vc.problemCounter.increment(NotificationCategory.missingCoverage);
            }
            if (!CheckCLDR.LIMITED_SUBMISSION
                    && !itemsOkIfVoted
                    && outdatedPaths.isOutdated(localeId, path)) {
                recordEnglishChanged(path, value, oldValue);
            }
            return missingStatus;
        }

        private void recordEnglishChanged(String path, String value, String oldValue) {
            if (Objects.equals(value, oldValue)
                    && choices.contains(NotificationCategory.englishChanged)) {
                String oldEnglishValue = outdatedPaths.getPreviousEnglish(path);
                if (!OutdatedPaths.NO_VALUE.equals(oldEnglishValue)) {
                    // check to see if we voted
                    problems.add(NotificationCategory.englishChanged);
                    vc.problemCounter.increment(NotificationCategory.englishChanged);
                }
            }
        }

        private void recordChoice(
                ErrorChecker.Status errorStatus, boolean itemsOkIfVoted, boolean onlyRecordErrors) {
            NotificationCategory choice =
                    errorStatus == ErrorChecker.Status.error
                            ? NotificationCategory.error
                            : errorStatus == ErrorChecker.Status.warning
                                    ? NotificationCategory.warning
                                    : null;

            if (choice == NotificationCategory.error
                    && choices.contains(NotificationCategory.error)
                    && (!itemsOkIfVoted || !OK_IF_VOTED.containsAll(subtypes))) {
                problems.add(choice);
                appendToMessage(statusMessage, htmlMessage);
                vc.problemCounter.increment(choice);
                for (Subtype subtype : subtypes) {
                    vc.errorSubtypeCounter.increment(subtype);
                }
            } else if (!onlyRecordErrors
                    && choice == NotificationCategory.warning
                    && choices.contains(NotificationCategory.warning)
                    && (!itemsOkIfVoted || !OK_IF_VOTED.containsAll(subtypes))) {
                problems.add(choice);
                appendToMessage(statusMessage, htmlMessage);
                vc.problemCounter.increment(choice);
                for (Subtype subtype : subtypes) {
                    vc.warningSubtypeCounter.increment(subtype);
                }
            }
        }

        private void recordLosingDisputedEtc(
                String path, VoteResolver.VoteStatus voteStatus, MissingStatus missingStatus) {
            switch (voteStatus) {
                case losing:
                    if (choices.contains(NotificationCategory.weLost)) {
                        problems.add(NotificationCategory.weLost);
                        vc.problemCounter.increment(NotificationCategory.weLost);
                    }
                    String usersValue =
                            userVoteStatus.getWinningValueForUsersOrganization(
                                    sourceFile, path, organization);
                    if (usersValue != null) {
                        usersValue =
                                "Losing value: <"
                                        + TransliteratorUtilities.toHTML.transform(usersValue)
                                        + ">";
                        appendToMessage(usersValue, htmlMessage);
                    }
                    break;
                case disputed:
                    if (choices.contains(NotificationCategory.hasDispute)) {
                        problems.add(NotificationCategory.hasDispute);
                        vc.problemCounter.increment(NotificationCategory.hasDispute);
                    }
                    break;
                case provisionalOrWorse:
                    if (missingStatus == MissingStatus.PRESENT
                            && choices.contains(NotificationCategory.notApproved)) {
                        problems.add(NotificationCategory.notApproved);
                        vc.problemCounter.increment(NotificationCategory.notApproved);
                    }
                    break;
                default:
            }
        }
    }

    public final class LocalesWithExplicitLevel implements Predicate<String> {
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
                if (!summarizeAllLocales && !SubmissionLocales.CLDR_LOCALES.contains(localeId)) {
                    return false;
                }
                return desiredLevel == sc.getTargetCoverageLevel(localeId);
            } else {
                Output<LocaleCoverageType> output = new Output<>();
                Level level = sc.getLocaleCoverageLevel(org, localeId, output);
                return desiredLevel == level
                        && output.value == StandardCodes.LocaleCoverageType.explicit;
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

    /**
     * Get the list of locales to be summarized for the given organization
     *
     * @param org the organization
     * @return the list of locale id strings
     */
    public ArrayList<String> getLocaleList(Organization org) {
        final ArrayList<String> list = new ArrayList<>();
        for (Level lv : Level.values()) {
            final Map<String, String> sortedNames = getSortedNames(org, lv);
            for (Map.Entry<String, String> entry : sortedNames.entrySet()) {
                list.add(entry.getValue());
            }
        }
        return list;
    }

    public void generatePriorityItemsSummary(
            Appendable output, EnumSet<NotificationCategory> choices, T organization)
            throws ExecutionException {
        try {
            String header = makeSummaryHeader(choices);
            for (Level level : Level.values()) {
                writeSummaryTable(output, header, level, choices, organization);
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e); // dang'ed checked exceptions
        }
    }

    private void appendDisplay(StringBuilder target, NotificationCategory category)
            throws IOException {
        target.append("<span title='").append(category.description);
        target.append("'>").append(category.buttonLabel).append("*</span>");
    }

    /**
     * This is a context object for Vetting Viewer parallel writes. It keeps track of the input
     * locales, other parameters, as well as the output streams.
     *
     * <p>When done, appendTo() is called to append the output to the original requester.
     *
     * @author srl
     */
    private class WriteContext {

        private final List<String> localeNames = new ArrayList<>();
        private final List<String> localeIds = new ArrayList<>();
        private final StringBuffer[] outputs;
        private final EnumSet<NotificationCategory> choices;
        private final EnumSet<NotificationCategory> ourChoicesThatRequireOldFile;
        private final T organization;
        private final VettingViewer<T>.VettingCounters totals;
        private final Map<String, VettingViewer<T>.FileInfo> localeNameToFileInfo;
        private final String header;
        private final int configChunkSize; // Number of locales to process at once, minimum 1

        private WriteContext(
                Set<Entry<String, String>> entrySet,
                EnumSet<NotificationCategory> choices,
                T organization,
                VettingCounters totals,
                Map<String, FileInfo> localeNameToFileInfo,
                String header) {
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

            EnumSet<NotificationCategory> thingsThatRequireOldFile =
                    EnumSet.of(
                            NotificationCategory.englishChanged,
                            NotificationCategory.missingCoverage,
                            NotificationCategory.changedOldValue,
                            NotificationCategory.inheritedChanged);
            ourChoicesThatRequireOldFile = choices.clone();
            ourChoicesThatRequireOldFile.retainAll(thingsThatRequireOldFile);

            this.organization = organization;
            this.totals = totals;
            this.localeNameToFileInfo = localeNameToFileInfo;
            this.header = header;

            if (DEBUG_THREADS) {
                System.out.println(
                        "writeContext for "
                                + organization.toString()
                                + " booted with "
                                + count
                                + " locales");
            }

            // setup env
            CLDRConfig config = CLDRConfig.getInstance();

            // parallelism. 0 means "let Java decide"
            int configParallel = Math.max(config.getProperty("CLDR_VETTINGVIEWER_PARALLEL", 0), 0);
            if (configParallel < 1) {
                configParallel =
                        java.lang.Runtime.getRuntime()
                                .availableProcessors(); // matches ForkJoinPool() behavior
            }
            this.configChunkSize =
                    Math.max(config.getProperty("CLDR_VETTINGVIEWER_CHUNKSIZE", 1), 1);
            if (DEBUG) {
                System.out.println(
                        "vv: CLDR_VETTINGVIEWER_PARALLEL="
                                + configParallel
                                + ", CLDR_VETTINGVIEWER_CHUNKSIZE="
                                + configChunkSize);
            }
        }

        /**
         * Append all of the results (one stream per locale) to the output parameter. Insert the
         * "header" as needed.
         *
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
                char nextChar = name.charAt(0);
                if (lastChar != nextChar) {
                    output.append(this.header);
                    lastChar = nextChar;
                }
                output.append(outputs[n]);
            }
        }

        /**
         * How many locales are represented in this context?
         *
         * @return
         */
        private int size() {
            return localeNames.size();
        }
    }

    /**
     * Worker action to implement parallel Vetting Viewer writes. This takes a WriteContext as a
     * parameter, as well as a subset of the locales to operate on.
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
                System.out.println(
                        "writeAction(…,"
                                + start
                                + ", "
                                + length
                                + ") of "
                                + context.size()
                                + " with outputCount:"
                                + context.outputs.length);
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
                invokeAll(
                        new WriteAction(context, start, split),
                        new WriteAction(context, start + split, length - split));
            }
        }

        /** Compute this entire task. Can call this to run this step as a single thread. */
        private void computeAll() {
            // do this many at once
            for (int n = start; n < (start + length); n++) {
                computeOne(n);
            }
        }

        /**
         * Calculate the Priority Items Summary output for one locale
         *
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
            EnumSet<NotificationCategory> choices = context.choices;
            Appendable output = context.outputs[n];
            if (output == null) {
                throw new NullPointerException("output " + n + " null");
            }
            // Initialize
            CLDRFile sourceFile = cldrFactory.make(localeID, true);
            CLDRFile baselineFile = null;
            if (!context.ourChoicesThatRequireOldFile.isEmpty()) {
                try {
                    Factory baselineFactory =
                            CLDRConfig.getInstance().getCommonAndSeedAndMainAndAnnotationsFactory();
                    baselineFile = baselineFactory.make(localeID, true);
                } catch (Exception e) {
                }
            }
            Level level = Level.MODERN;
            if (context.organization != null) {
                StandardCodes sc = StandardCodes.make();
                if (orgIsNeutralForSummary((Organization) context.organization)) {
                    level = sc.getTargetCoverageLevel(localeID);
                } else {
                    level = sc.getLocaleCoverageLevel(context.organization.toString(), localeID);
                }
            }
            FileInfo fileInfo = new FileInfo(localeID, level, choices, context.organization);
            fileInfo.setFiles(sourceFile, baselineFile);
            fileInfo.getFileInfo();

            if (context.localeNameToFileInfo != null) {
                context.localeNameToFileInfo.put(name, fileInfo);
            }

            context.totals.addAll(fileInfo.vc);
            if (DEBUG_THREADS) {
                System.out.println(
                        "writeAction.compute(" + n + ") - got fileinfo " + name + ": " + localeID);
            }
            try {
                writeSummaryRow(output, choices, fileInfo.vc.problemCounter, name, localeID, level);
                if (DEBUG_THREADS) {
                    System.out.println(
                            "writeAction.compute(" + n + ") - wrote " + name + ": " + localeID);
                }
            } catch (IOException | ExecutionException e) {
                System.err.println(
                        "writeAction.compute(" + n + ") - writeexc " + name + ": " + localeID);
                this.completeExceptionally(new RuntimeException("While writing " + localeID, e));
            }
            if (DEBUG) {
                System.out.println(
                        "writeAction.compute(" + n + ") - DONE " + name + ": " + localeID);
            }
        }
    }

    /**
     * Write the table for the Priority Items Summary
     *
     * @param output
     * @param header
     * @param desiredLevel
     * @param choices
     * @param organization
     * @throws IOException
     */
    private void writeSummaryTable(
            Appendable output,
            String header,
            Level desiredLevel,
            EnumSet<NotificationCategory> choices,
            T organization)
            throws IOException, ExecutionException {
        Map<String, String> sortedNames = getSortedNames((Organization) organization, desiredLevel);
        if (sortedNames.isEmpty()) {
            return;
        }
        output.append("<h2>Level: ").append(desiredLevel.toString()).append("</h2>");
        output.append("<table class='tvs-table'>\n");

        // Caution: localeNameToFileInfo, if not null, may lead to running out of memory
        Map<String, FileInfo> localeNameToFileInfo = SHOW_SUBTYPES ? new TreeMap<>() : null;

        VettingCounters totals = new VettingCounters();

        Set<Entry<String, String>> entrySet = sortedNames.entrySet();

        WriteContext context =
                this
                .new WriteContext(
                        entrySet, choices, organization, totals, localeNameToFileInfo, header);

        WriteAction writeAction = this.new WriteAction(context);
        if (USE_FORKJOIN) {
            ForkJoinPool.commonPool().invoke(writeAction);
        } else {
            if (DEBUG) {
                System.out.println(
                        "WARNING: calling writeAction.computeAll(), as the ForkJoinPool is disabled.");
            }
            writeAction.computeAll();
        }
        context.appendTo(output); // write all of the results together
        output.append(header); // add one header at the bottom before the Total row
        writeSummaryRow(output, choices, totals.problemCounter, "Total", null, desiredLevel);
        output.append("</table>");
        if (SHOW_SUBTYPES) {
            showSubtypes(output, sortedNames, localeNameToFileInfo, totals, true);
            showSubtypes(output, sortedNames, localeNameToFileInfo, totals, false);
        }
    }

    private Map<String, String> getSortedNames(Organization org, Level desiredLevel) {
        Map<String, String> sortedNames = new TreeMap<>(CLDRConfig.getInstance().getCollator());
        // TODO Fix HACK
        // We are going to ignore the predicate for now, just using the locales that have explicit
        // coverage.
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

    private void showSubtypes(
            Appendable output,
            Map<String, String> sortedNames,
            Map<String, FileInfo> localeNameToFileInfo,
            VettingCounters totals,
            boolean errors)
            throws IOException {

        output.append("<h3>Details: ")
                .append(errors ? "Error Types" : "Warning Types")
                .append("</h3>");
        output.append("<table class='tvs-table'>");
        Counter<Subtype> subtypeCounterTotals =
                errors ? totals.errorSubtypeCounter : totals.warningSubtypeCounter;
        Set<Subtype> sortedBySize = subtypeCounterTotals.getKeysetSortedByCount(false);

        // header
        writeDetailHeader(sortedBySize, output);

        // items
        for (Entry<String, FileInfo> entry : localeNameToFileInfo.entrySet()) {
            Counter<Subtype> counter =
                    errors
                            ? entry.getValue().vc.errorSubtypeCounter
                            : entry.getValue().vc.warningSubtypeCounter;
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
        output.append("<tr>")
                .append(TH_AND_STYLES)
                .append("<i>Total</i>")
                .append("</th>")
                .append(TH_AND_STYLES)
                .append("</th>");
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

    private void writeDetailHeader(Set<Subtype> sortedBySize, Appendable output)
            throws IOException {
        output.append("<tr>")
                .append(TH_AND_STYLES)
                .append("Name")
                .append("</th>")
                .append(TH_AND_STYLES)
                .append("ID")
                .append("</th>");
        for (Subtype subtype : sortedBySize) {
            output.append(TH_AND_STYLES).append(subtype.toString()).append("</th>");
        }
    }

    private String makeSummaryHeader(EnumSet<NotificationCategory> choices) throws IOException {
        StringBuilder headerRow = new StringBuilder();
        headerRow
                .append("<tr class='tvs-tr'>")
                .append(TH_AND_STYLES)
                .append("Level</th>")
                .append(TH_AND_STYLES)
                .append("Locale</th>")
                .append(TH_AND_STYLES)
                .append("Codes</th>")
                .append(TH_AND_STYLES)
                .append("Progress</th>");
        for (NotificationCategory choice : choices) {
            headerRow.append("<th class='tv-th'>");
            appendDisplay(headerRow, choice);
            headerRow.append("</th>");
        }
        headerRow.append(TH_AND_STYLES).append("Status</th>");
        headerRow.append("</tr>\n");
        return headerRow.toString();
    }

    /**
     * Write one row of the Priority Items Summary
     *
     * @param output
     * @param choices
     * @param problemCounter
     * @param name
     * @param localeID if null, this is a "Total" row to be shown at the bottom of the table
     * @param level
     * @throws IOException
     *     <p>CAUTION: this method not only uses "th" for "table header" in the usual sense, it also
     *     uses "th" for cells that contain data, including locale names like "Kashmiri
     *     (Devanagari)" and code values like "<code>ks_Deva₍_IN₎</code>". The same row may have
     *     both "th" and "td" cells.
     */
    private void writeSummaryRow(
            Appendable output,
            EnumSet<NotificationCategory> choices,
            Counter<NotificationCategory> problemCounter,
            String name,
            String localeID,
            Level level)
            throws IOException, ExecutionException {
        output.append("<tr>")
                .append(TH_AND_STYLES)
                .append(level.toString())
                .append("</th>")
                .append(TH_AND_STYLES);
        if (localeID == null) {
            output.append("<i>")
                    .append(name) // here always name = "Total"
                    .append("</i>")
                    .append("</th>")
                    .append(TH_AND_STYLES); // empty cell for Codes
        } else {
            appendNameAndCode(name, localeID, output);
        }
        output.append("</th>\n");
        final String progPerc =
                (localeID == null) ? "" : getLocaleProgressPercent(localeID, problemCounter);
        output.append("<td class='tvs-count'>").append(progPerc).append("</td>\n");
        for (NotificationCategory choice : choices) {
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
        addLocaleStatusColumn(output, localeID);
        output.append("</tr>\n");
    }

    private void addLocaleStatusColumn(Appendable output, String localeID) throws IOException {
        output.append("<td class='tvs-count'>");
        if (localeID != null) {
            output.append(getLocaleStatusColumn(CLDRLocale.getInstance(localeID)));
        }
        output.append("</td>\n");
    }

    private String getLocaleStatusColumn(CLDRLocale locale) {
        if (SpecialLocales.getType(locale) == SpecialLocales.Type.algorithmic) {
            return "AL"; // algorithmic
        } else if (Organization.special.getCoveredLocales().containsLocaleOrParent(locale)) {
            return "HC"; // high coverage
        } else if (Organization.cldr.getCoveredLocales().containsLocaleOrParent(locale)) {
            return "TC"; // Technical Committee
        } else {
            return "";
        }
    }

    private String getLocaleProgressPercent(
            String localeId, Counter<NotificationCategory> problemCounter)
            throws ExecutionException {
        final LocaleCompletionData lcd = new LocaleCompletionData(problemCounter);
        final int problemCount = lcd.problemCount();
        final int total =
                localeBaselineCount.getBaselineProblemCount(CLDRLocale.getInstance(localeId));
        final int done = (problemCount >= total) ? 0 : total - problemCount;
        // return CompletionPercent.calculate(done, total) + "%";

        // Adjust according to https://unicode-org.atlassian.net/browse/CLDR-15785
        // This is NOT a logical long-term solution
        int perc = CompletionPercent.calculate(done, total);
        if (perc == 100 && problemCount > 0) {
            perc = 99;
        }
        return perc + "%";
    }

    private void appendNameAndCode(String name, String localeID, Appendable output)
            throws IOException {
        // See https://unicode-org.atlassian.net/browse/CLDR-15279
        String url = "v#/" + localeID + "//";
        String[] names = name.split(SPLIT_CHAR);
        output.append("<a href='" + url)
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
        return englishFile.getName(localeID, true, CLDRFile.SHORT_ALTS)
                + SPLIT_CHAR
                + gatherCodes(contents);
    }

    /**
     * Collapse the names {en_Cyrl, en_Cyrl_US} => en_Cyrl(_US) {en_GB, en_Latn_GB} => en(_Latn)_GB
     * {en, en_US, en_Latn, en_Latn_US} => en(_Latn)(_US) {az_IR, az_Arab, az_Arab_IR} => az_IR,
     * az_Arab(_IR)
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

    /** Used to determine what the status of a particular path's value is in a given locale. */
    public enum MissingStatus {
        /**
         * There is an explicit value for the path, including ↑↑↑, or there is an inherited value
         * (but not including the ABSENT conditions, e.g. not from root).
         */
        PRESENT,

        /**
         * The value is inherited from a different path. Only applies if the parent is not root.
         * That path might be in the same locale or from a parent (but not root or CODE_FALLBACK).
         */
        ALIASED,

        /** See ABSENT */
        MISSING_OK,

        /** See ABSENT */
        ROOT_OK,

        /**
         * The supplied CLDRFile is null, or the value is null, or the value is inherited from root
         * or CODE_FALLBACK. A special ValuePathStatus.isMissingOk method allows for some
         * exceptions, changing the result to MISSING_OK or ROOT_OK.
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
        String sourceLocale =
                sourceFile.getSourceLocaleIdExtended(
                        path, status, false); // does not skip inheritance marker

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
            result =
                    ValuePathStatus.isMissingOk(sourceFile, path, latin, isAliased)
                            ? MissingStatus.MISSING_OK
                            : MissingStatus.ABSENT;
        } else {
            /*
             * skipInheritanceMarker must be false for getSourceLocaleIdExtended here, since INHERITANCE_MARKER
             * may be found if there are votes for inheritance, in which case we must not skip up to "root" and
             * treat the item as missing. Reference: https://unicode.org/cldr/trac/ticket/11765
             */
            String localeFound =
                    sourceFile.getSourceLocaleIdExtended(
                            path, status, false /* skipInheritanceMarker */);
            final boolean localeFoundIsRootOrCodeFallback =
                    localeFound.equals(XMLSource.ROOT_ID)
                            || localeFound.equals(XMLSource.CODE_FALLBACK_ID);
            final boolean isParentRoot =
                    CLDRLocale.getInstance(sourceFile.getLocaleID()).isParentRoot();
            /*
             * Only count it as missing IF the (localeFound is root or codeFallback)
             * AND the aliasing didn't change the path.
             * Note that localeFound will be where an item with ↑↑↑ was found even though
             * the resolved value is actually inherited from somewhere else.
             */

            if (localeFoundIsRootOrCodeFallback) {
                result =
                        ValuePathStatus.isMissingOk(sourceFile, path, latin, isAliased)
                                ? MissingStatus.ROOT_OK
                                : isParentRoot ? MissingStatus.ABSENT : MissingStatus.ALIASED;
            } else if (!isAliased) {
                result = MissingStatus.PRESENT;
            } else if (isParentRoot) { // We handle ALIASED specially, depending on whether the
                // parent is root or not.
                result =
                        ValuePathStatus.isMissingOk(sourceFile, path, latin, isAliased)
                                ? MissingStatus.MISSING_OK
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

    private static void appendToMessage(
            CharSequence usersValue, Subtype subtype, StringBuilder testMessage) {
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
     */
    public static class ProgressCallback {
        /**
         * Note any progress. This will be called before any output is printed. It will be called
         * approximately once per xpath.
         */
        public void nudge() {}

        /** Called when all operations are complete. */
        public void done() {}

        /**
         * Return true to cause an early stop.
         *
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
     */
    public VettingViewer<T> setProgressCallback(ProgressCallback newCallback) {
        progressCallback = newCallback;
        return this;
    }

    /**
     * Find the status of all the paths in the input file. See the full getStatus for more
     * information.
     *
     * @param file the source. Must be a resolved file, made with minimalDraftStatus = unconfirmed
     * @param pathHeaderFactory PathHeaderFactory.
     * @param foundCounter output counter of the number of paths with values having contributed or
     *     approved status
     * @param unconfirmedCounter output counter of the number of paths with values, but neither
     *     contributed nor approved status
     * @param missingCounter output counter of the number of paths without values
     * @param missingPaths output if not null, the specific paths that are missing.
     * @param unconfirmedPaths TODO
     */
    public static void getStatus(
            CLDRFile file,
            PathHeader.Factory pathHeaderFactory,
            Counter<Level> foundCounter,
            Counter<Level> unconfirmedCounter,
            Counter<Level> missingCounter,
            Relation<MissingStatus, String> missingPaths,
            Set<String> unconfirmedPaths) {
        getStatus(
                file.fullIterable(),
                file,
                pathHeaderFactory,
                foundCounter,
                unconfirmedCounter,
                missingCounter,
                missingPaths,
                unconfirmedPaths);
    }

    /**
     * Find the status of an input set of paths in the input file. It partitions the returned data
     * according to the Coverage levels. NOTE: MissingStatus.ALIASED is handled specially; it is
     * mapped to ABSENT if the parent is root, and otherwise mapped to PRESENT.
     *
     * @param allPaths manual list of paths
     * @param file the source. Must be a resolved file, made with minimalDraftStatus = unconfirmed
     * @param pathHeaderFactory PathHeaderFactory.
     * @param foundCounter output counter of the number of paths with values having contributed or
     *     approved status
     * @param unconfirmedCounter output counter of the number of paths with values, but neither
     *     contributed nor approved status
     * @param missingCounter output counter of the number of paths without values
     * @param missingPaths output if not null, the specific paths that are missing.
     * @param unconfirmedPaths TODO
     */
    public static void getStatus(
            Iterable<String> allPaths,
            CLDRFile file,
            PathHeader.Factory pathHeaderFactory,
            Counter<Level> foundCounter,
            Counter<Level> unconfirmedCounter,
            Counter<Level> missingCounter,
            Relation<MissingStatus, String> missingPaths,
            Set<String> unconfirmedPaths) {

        if (!file.isResolved()) {
            throw new IllegalArgumentException("File must be resolved, no minimal draft status");
        }
        foundCounter.clear();
        unconfirmedCounter.clear();
        missingCounter.clear();

        boolean latin = VettingViewer.isLatinScriptLocale(file);
        CoverageLevel2 coverageLevel2 =
                CoverageLevel2.getInstance(SupplementalDataInfo.getInstance(), file.getLocaleID());

        for (String path : allPaths) {

            PathHeader ph = pathHeaderFactory.fromPath(path);
            if (ph.getSectionId() == SectionId.Special) {
                continue;
            }

            Level level = coverageLevel2.getLevel(path);
            if (level.compareTo(Level.MODERN) > 0) {
                continue;
            }
            MissingStatus missingStatus = VettingViewer.getMissingStatus(file, path, latin);

            switch (missingStatus) {
                case ABSENT:
                    missingCounter.add(level, 1);
                    if (missingPaths != null) {
                        missingPaths.put(missingStatus, path);
                    }
                    break;
                case ALIASED:
                case PRESENT:
                    String fullPath = file.getFullXPath(path);
                    if (fullPath.contains("unconfirmed") || fullPath.contains("provisional")) {
                        unconfirmedCounter.add(level, 1);
                        if (unconfirmedPaths != null) {
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

    private static final EnumSet<NotificationCategory> localeCompletionCategories =
            EnumSet.of(
                    NotificationCategory.error,
                    NotificationCategory.hasDispute,
                    NotificationCategory.notApproved,
                    NotificationCategory.missingCoverage);

    public static EnumSet<NotificationCategory> getDashboardNotificationCategories(
            Organization usersOrg) {
        EnumSet<NotificationCategory> choiceSet = EnumSet.allOf(NotificationCategory.class);
        if (orgIsNeutralForSummary(usersOrg)) {
            choiceSet =
                    EnumSet.of(
                            NotificationCategory.error,
                            NotificationCategory.warning,
                            NotificationCategory.hasDispute,
                            NotificationCategory.notApproved,
                            NotificationCategory.missingCoverage);
            // skip weLost, englishChanged, changedOldValue, abstained
        }
        return choiceSet;
    }

    public static EnumSet<NotificationCategory> getPriorityItemsSummaryCategories(
            Organization org) {
        EnumSet<NotificationCategory> set = getDashboardNotificationCategories(org);
        set.remove(NotificationCategory.abstained);
        return set;
    }

    public static EnumSet<NotificationCategory> getLocaleCompletionCategories() {
        return localeCompletionCategories;
    }

    public interface LocaleBaselineCount {
        int getBaselineProblemCount(CLDRLocale cldrLocale) throws ExecutionException;
    }

    private boolean summarizeAllLocales = false;

    public void setSummarizeAllLocales(boolean b) {
        summarizeAllLocales = b;
    }
}
