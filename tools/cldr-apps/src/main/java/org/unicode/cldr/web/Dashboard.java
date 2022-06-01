package org.unicode.cldr.web;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.PathHeader.SectionId;
import org.unicode.cldr.util.VettingViewer;
import org.unicode.cldr.util.VettingViewer.Choice;
import org.unicode.cldr.util.VettingViewer.DashboardArgs;
import org.unicode.cldr.util.VoterReportStatus.ReportId;
import org.unicode.cldr.util.VoterReportStatus.ReportStatus;
import org.unicode.cldr.util.VoterProgress;

import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R4;

public class Dashboard {

    /**
     * ReviewOutput defines the complete json sent to the front end
     */
    @Schema(description = "Output of Dashboard")
    public static final class ReviewOutput {
        @Schema(description = "list of notifications")
        private List<ReviewNotification> notifications = new ArrayList<>();

        public ReviewNotification[] getNotifications() {
            return notifications.toArray(new ReviewNotification[notifications.size()]);
        }

        public ReviewNotification add(String notification) {
            return add(new ReviewNotification(notification));
        }

        /**
         * Add this notification, unless the name is an exact match
         * @param category category of the next notification (e.g. English_Changed)
         * @param unlessMatches if this notification is already the same as notificationName, just return it.
         * @return
         */
        public ReviewNotification add(String category, ReviewNotification unlessMatches) {
            if (unlessMatches != null && unlessMatches.category.equals(category)) {
                return unlessMatches;
            } else {
                return add(category);
            }
        }

        public ReviewNotification add(ReviewNotification notification) {
            this.notifications.add(notification);
            return notification;
        }

        @Schema(description = "Notifications that the user has chosen to hide")
        private HiddenNotifications hidden = null;

        /**
         * Get a map from each subtype (String from CheckCLDR.CheckStatus.Subtype)
         * to an array of path-value pairs
         *
         * @return the map
         */
        public Map<String, PathValuePair[]> getHidden() {
            if (hidden == null) {
                return null;
            }
            return hidden.getHidden();
        }

        public VoterProgress voterProgress = null;

        /**
         * Add Report status
         * @param userId
         * @param localeId
         */
        public void addReports(int userId, String localeId) {
            final CLDRLocale locale = CLDRLocale.getInstance(localeId);
            ReportStatus reportStatus = ReportsDB.getInstance().getReportStatus(userId, locale);
            EnumSet<ReportId> incomplete = EnumSet.complementOf(reportStatus.completed);
            if (!incomplete.isEmpty()) {
                ReviewNotificationGroup rng = new ReviewNotificationGroup("Reports", "", "");
                incomplete.forEach(r -> rng.add(r.name()));
                add("Reports").add(rng);
            }
            // update counts. Completed are both voted and votable.
            reportStatus.completed.forEach(r -> {
                voterProgress.incrementVotedPathCount();
                voterProgress.incrementVotablePathCount();
            });
            // Incomplete are votable but not voted.
            incomplete.forEach(r -> {
                voterProgress.incrementVotablePathCount();
            });
        }
    }

    @Schema(description = "Heading for a portion of the notifications")
    public static final class ReviewNotification {

        // See org.unicode.cldr.util.VettingViewer.Choice
        @Schema(description = "Notification category", example = "English_Changed")
        public String category;

        public ReviewNotification(String category) {
            this.category = category;
        }

        // for serialization
        public ReviewNotificationGroup[] getGroups() {
            return groups.toArray(new ReviewNotificationGroup[groups.size()]);
        }

        private List<ReviewNotificationGroup> groups = new ArrayList<>();

        ReviewNotificationGroup add(ReviewNotificationGroup group) {
            this.groups.add(group);
            return group;
        }
    }

    @Schema(description = "Group of notifications which share the same section/page/header")
    public static final class ReviewNotificationGroup {
        private List<ReviewEntry> entries = new ArrayList<>();

        @Schema(description = "SurveyTool section", example = "Units")
        public String section;
        @Schema(description = "SurveyTool page", example = "Volume")
        public String page;
        @Schema(description = "SurveyTool header", example = "dessert-spoon-imperial")
        public String header;

        public ReviewNotificationGroup(String sectionName, String pageName, String headerName) {
            this.section = sectionName;
            this.page = pageName;
            this.header = headerName;
        }

        public ReviewEntry add(String code, String xpath) {
            ReviewEntry e = new ReviewEntry(code, xpath);
            this.entries.add(e);
            return e;
        }

        public ReviewEntry add(String code) {
            ReviewEntry e = new ReviewEntry(code);
            this.entries.add(e);
            return e;
        }

        // for serialization
        public ReviewEntry[] getEntries() {
            return entries.toArray(new ReviewEntry[entries.size()]);
        }
    }

    @Schema(description = "Single entry of the dashboard that needs review")
    public static final class ReviewEntry {

        @Schema(description = "Code for this entry", example = "narrow-other-nominative")
        public String code;

        @Schema(example = "7bd36b15a66d02cf")
        public String xpstrid;

        @Schema(description = "English text", example = "{0}dsp-Imp")
        public String english;

        @Schema(description = "Previous English value, for EnglishChanged", example = "{0} dstspn Imp")
        public String previousEnglish;

        @Schema(description = "Baseline value", example = "{0} dstspn Imp")
        public String old; /* Not currently (2022-04-08) used by front end; should be renamed "baseline" */

        @Schema(description = "Winning value in this locale", example = "{0} dstspn Imp")
        public String winning;

        @Schema(description = "html comment on the error", example = "&lt;value too wide&gt; Too wide by about 100% (with common fonts).")
        public String comment;

        @Schema(description = "Subtype of the error", example = "largerDifferences")
        public CheckCLDR.CheckStatus.Subtype subtype;

        /**
         * Create a new ReviewEntry
         * @param code item code
         * @param xpath xpath string
         */
        public ReviewEntry(String code, String xpath) {
            this.code = code;
            this.xpstrid = XPathTable.getStringIDString(xpath);
        }

        public ReviewEntry(String code) {
            this.code = code;
            this.xpstrid = code;
        }
    }

    /**
     * Get Dashboard output as an object
     *
     * @param locale
     * @param user
     * @param coverageLevel
     * @param xpath like "//ldml/..."; only check the given xpath; if xpath is null, check all paths
     * @return the ReviewOutput
     */
    public ReviewOutput get(CLDRLocale locale, UserRegistry.User user, Level coverageLevel, String xpath) {
        final SurveyMain sm = CookieSession.sm;
        Organization usersOrg = Organization.fromString(user.voterOrg());
        STFactory sourceFactory = sm.getSTFactory();
        VettingViewer<Organization> vv = new VettingViewer<>(sm.getSupplementalDataInfo(), sourceFactory,
            new STUsersChoice(sm));
        EnumSet<VettingViewer.Choice> choiceSet = VettingViewer.getChoiceSetForOrg(usersOrg);
        DashboardArgs args = new DashboardArgs(choiceSet, locale, coverageLevel);
        args.setUserAndOrganization(user.id, usersOrg);
        setFiles(args, locale, sourceFactory);
        if (xpath != null) {
            args.setXpath(xpath);
        }
        return reallyGet(vv, args);
    }

    public static void setFiles(DashboardArgs args, CLDRLocale locale, STFactory sourceFactory) {
        /*
         * sourceFile provides the current winning values, taking into account recent votes.
         * baselineFile provides the "baseline" (a.k.a. "trunk") values, i.e., the values that
         * are in the current XML in the cldr version control repository. The baseline values
         * are generally the last release values plus any changes that have been made by the
         * technical committee by committing directly to version control rather than voting.
         */
        final String localeId = locale.getBaseName();
        final CLDRFile sourceFile = sourceFactory.make(localeId);
        final Factory baselineFactory = CookieSession.sm.getDiskFactory();
        final CLDRFile baselineFile = baselineFactory.make(localeId, true);
        args.setFiles(sourceFile, baselineFile);
    }

    private ReviewOutput reallyGet(VettingViewer<Organization> vv, DashboardArgs args) {
        VettingViewer<Organization>.DashboardData dd;
        dd = vv.generateDashboard(args);

        ReviewOutput reviewOutput = new ReviewOutput();

        addNotificationEntries(args, dd.sorted, reviewOutput);

        if (!args.isOnlyForSinglePath()) {
            final String localeId = args.getLocale().toString();
            reviewOutput.hidden = new ReviewHide(args.getUserId(), localeId).get();
            reviewOutput.voterProgress = dd.voterProgress;
            reviewOutput.addReports(args.getUserId(), localeId);
        }
        return reviewOutput;
    }

    private void addNotificationEntries(DashboardArgs args,
        Relation<R2<SectionId, PageId>, VettingViewer<Organization>.WritingInfo> sorted,
        ReviewOutput reviewOutput) {
        Relation<Row.R4<Choice, SectionId, PageId, String>, VettingViewer<Organization>.WritingInfo> notificationsList = getNotificationsList(sorted);

        ReviewNotification notification = null;

        SurveyMain sm = CookieSession.sm;
        CLDRFile englishFile = sm.getEnglishFile();

        for (Entry<R4<Choice, SectionId, PageId, String>, Set<VettingViewer<Organization>.WritingInfo>> entry : notificationsList.keyValuesSet()) {

            notification = getNextNotification(reviewOutput, notification, entry);

            addNotificationGroup(args, notification, englishFile, entry);
        }
    }

    private Relation<Row.R4<Choice, SectionId, PageId, String>, VettingViewer<Organization>.WritingInfo> getNotificationsList(
        Relation<R2<SectionId, PageId>, VettingViewer<Organization>.WritingInfo> sorted) {
        Relation<Row.R4<Choice, SectionId, PageId, String>, VettingViewer<Organization>.WritingInfo> notificationsList = Relation
            .of(new TreeMap<Row.R4<Choice, SectionId, PageId, String>, Set<VettingViewer<Organization>.WritingInfo>>(), TreeSet.class);

        for (Entry<R2<SectionId, PageId>, Set<VettingViewer<Organization>.WritingInfo>> entry0 : sorted.keyValuesSet()) {
            final Set<VettingViewer<Organization>.WritingInfo> rows = entry0.getValue();
            for (VettingViewer<Organization>.WritingInfo pathInfo : rows) {
                Set<Choice> choicesForPath = pathInfo.problems;
                SectionId section = entry0.getKey().get0();
                PageId subsection = entry0.getKey().get1();
                for (Choice choice : choicesForPath) {
                    notificationsList.put(Row.of(choice, section, subsection, pathInfo.codeOutput.getHeader()), pathInfo);
                }
            }
        }
        return notificationsList;
    }

    private ReviewNotification getNextNotification(ReviewOutput reviewOutput, ReviewNotification notification,
        Entry<R4<Choice, SectionId, PageId, String>, Set<VettingViewer<Organization>.WritingInfo>> entry) {
        String notificationName = entry.getKey().get0().jsonLabel;

        notification = reviewOutput.add(notificationName, notification);
        return notification;
    }

    private void addNotificationGroup(DashboardArgs args, ReviewNotification notification, CLDRFile englishFile,
        Entry<R4<Choice, SectionId, PageId, String>, Set<VettingViewer<Organization>.WritingInfo>> entry) {
        String sectionName = entry.getKey().get1().name();
        String pageName = entry.getKey().get2().name();
        String headerName = entry.getKey().get3();

        ReviewNotificationGroup header = notification.add(new ReviewNotificationGroup(sectionName, pageName, headerName));

        for (VettingViewer<Organization>.WritingInfo info : entry.getValue()) {
            String code = info.codeOutput.getCode();
            String path = info.codeOutput.getOriginalPath();
            Set<Choice> choicesForPath = info.problems;

            ReviewEntry reviewEntry = header.add(code, path);

            reviewEntry.english = englishFile.getWinningValue(path);

            if (choicesForPath.contains(Choice.englishChanged)) {
                String previous = VettingViewer.getOutdatedPaths().getPreviousEnglish(path);
                reviewEntry.previousEnglish = previous;
            }

            // old = baseline; not currently used by client
            final CLDRFile baselineFile = args.getBaselineFile();
            reviewEntry.old = baselineFile == null ? null : baselineFile.getWinningValue(path);

            // winning value
            reviewEntry.winning = args.getSourceFile().getWinningValue(path);

            // CheckCLDR.CheckStatus.Subtype
            reviewEntry.subtype = info.subtype;

            // comment
            if (!info.htmlMessage.isEmpty()) {
                reviewEntry.comment = info.htmlMessage;
            }
        }
    }
}
