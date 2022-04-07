package org.unicode.cldr.web;

import java.util.*;
import java.util.Map.Entry;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.json.JSONArray;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.util.*;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.PathHeader.SectionId;
import org.unicode.cldr.util.VettingViewer.Choice;

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
         * @param notificationName name of the next notification (e.g. Error)
         * @param unlessMatches if this notification is already the same as notificationName, just return it.
         * @return
         */
        public ReviewNotification add(String notificationName, ReviewNotification unlessMatches) {
            if (unlessMatches != null && unlessMatches.notification.equals(notificationName)) {
                return unlessMatches;
            } else {
                return add(notificationName);
            }
        }

        public ReviewNotification add(ReviewNotification notification) {
            this.notifications.add(notification);
            return notification;
        }

        @Schema(description = "Notifications that the user has chosen to hide")
        private HiddenNotifications hidden;

        /**
         * Get a map from each subtype (String from CheckCLDR.CheckStatus.Subtype)
         * to an array of path-value pairs
         *
         * @return the map
         */
        public Map<String, PathValuePair[]> getHidden() {
            return hidden.getHidden();
        }

        public VoterProgress voterProgress = null;
    }

    @Schema(description = "Heading for a portion of the notifications")
    public static final class ReviewNotification {

        @Schema(description = "Notification type", example = "Error")
        public String notification;

        public ReviewNotification(String notificationName) {
            this.notification = notificationName;
        }

        // for serialization
        public ReviewNotificationGroup[] getEntries() {
            return entries.toArray(new ReviewNotificationGroup[entries.size()]);
        }

        private List<ReviewNotificationGroup> entries = new ArrayList<>();

        ReviewNotificationGroup add(ReviewNotificationGroup group) {
            this.entries.add(group);
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
        public String xpath;

        @Schema(description = "English text", example = "{0}dsp-Imp")
        public String english;

        @Schema(description = "Previous English value, for EnglishChanged", example = "{0} dstspn Imp")
        public String previousEnglish;

        @Schema(description = "Baseline value", example = "{0} dstspn Imp")
        public String old; /* Not currently (2022-04-05) used by front end; should be renamed "baseline" */

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
            this.xpath = XPathTable.getStringIDString(xpath);
        }
    }

    /**
     * Get a miniature version of the Dashboard data, for only a single path
     *
     * Called by SurveyAjax.getRow
     */
    public VettingViewer.SinglePathDashResults getOneRow(CLDRLocale locale, WebContext ctx, CookieSession sess, String path) {
        VettingViewer.SinglePathDashArgs args = new VettingViewer.SinglePathDashArgs();
        if (ctx != null) {
            args.usersLevel = Level.get(ctx.getEffectiveCoverageLevel(ctx.getLocale().toString()));
            sess = ctx.session;
        } else {
            String levelString = sess.settings().get(SurveyMain.PREF_COVLEV, WebContext.PREF_COVLEV_LIST[0]);
            args.usersLevel = Level.get(levelString);
        }
        args.organization = Organization.fromString(sess.user.voterOrg());
        args.choiceSet = VettingViewer.getChoiceSetForOrg(args.organization);
        args.localeID = locale.getBaseName();
        args.path = path;
        SurveyMain sm = CookieSession.sm;
        VettingViewer<Organization> vv = new VettingViewer<>(sm.getSupplementalDataInfo(), sm.getSTFactory(),
            new STUsersChoice(sm));
        VettingViewer.SinglePathDashResults results = new VettingViewer.SinglePathDashResults();
        vv.getSinglePathDash(args, results);
        return results;
    }

    /**
     * Get Dashboard output as an object
     *
     * @param locale
     * @param user
     * @param usersLevel
     * @return the ReviewOutput
     */
    public ReviewOutput get(CLDRLocale locale, UserRegistry.User user, Level usersLevel) {
        final SurveyMain sm = CookieSession.sm;
        String loc = locale.getBaseName();
        /*
         * if no coverage level set, use default one
         */
        Organization usersOrg = Organization.fromString(user.voterOrg());
        STFactory sourceFactory = sm.getSTFactory();
        final Factory baselineFactory = CookieSession.sm.getDiskFactory();
        VettingViewer<Organization> vv = new VettingViewer<>(sm.getSupplementalDataInfo(), sourceFactory,
            new STUsersChoice(sm));

        EnumSet<VettingViewer.Choice> choiceSet = VettingViewer.getChoiceSetForOrg(usersOrg);

        /*
         * sourceFile provides the current winning values, taking into account recent votes.
         * baselineFile provides the "baseline" (a.k.a. "trunk") values, i.e., the values that
         * are in the current XML in the cldr version control repository. The baseline values
         * are generally the last release values plus any changes that have been made by the
         * technical committee by committing directly to version control rather than voting.
         */
        CLDRFile sourceFile = sourceFactory.make(loc);
        CLDRFile baselineFile = baselineFactory.make(loc, true);

        /*
         * TODO: refactor -- too many parameters! Some could be fields of Dashboard or other classes...
         * Reference: https://unicode-org.atlassian.net/browse/CLDR-15056
         */
        VettingViewer<Organization>.DashboardData dd;
        dd = vv.generateDashboard(choiceSet, loc, user.id, usersOrg, usersLevel, sourceFile, baselineFile);
        return reallyGet(sourceFile, baselineFile, dd, locale, user.id);
    }

    private ReviewOutput reallyGet(CLDRFile sourceFile, CLDRFile baselineFile,
        VettingViewer<Organization>.DashboardData dd,
        CLDRLocale locale, int userId) {

        ReviewOutput reviewInfo = new ReviewOutput();

        addNotificationEntries(sourceFile, baselineFile, dd.sorted, reviewInfo);

        reviewInfo.hidden = new ReviewHide(userId, locale.toString()).get();

        reviewInfo.voterProgress = dd.voterProgress;

        return reviewInfo;
    }

    private void addNotificationEntries(CLDRFile sourceFile, CLDRFile baselineFile,
        Relation<R2<SectionId, PageId>, VettingViewer<Organization>.WritingInfo> sorted,
        ReviewOutput reviewInfo) {
        Relation<Row.R4<Choice, SectionId, PageId, String>, VettingViewer<Organization>.WritingInfo> notificationsList = getNotificationsList(sorted);

        ReviewNotification notification = null;

        SurveyMain sm = CookieSession.sm;
        CLDRFile englishFile = sm.getEnglishFile();

        for (Entry<R4<Choice, SectionId, PageId, String>, Set<VettingViewer<Organization>.WritingInfo>> entry : notificationsList.keyValuesSet()) {

            notification = getNextNotification(reviewInfo, notification, entry);

            addNotificationGroup(sourceFile, baselineFile, notification, englishFile, entry);
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

    private ReviewNotification getNextNotification(ReviewOutput reviewInfo, ReviewNotification notification,
        Entry<R4<Choice, SectionId, PageId, String>, Set<VettingViewer<Organization>.WritingInfo>> entry) {
        String notificationName = entry.getKey().get0().jsonLabel;

        notification = reviewInfo.add(notificationName, notification);
        return notification;
    }

    private void addNotificationGroup(CLDRFile sourceFile, CLDRFile baselineFile, ReviewNotification notification, CLDRFile englishFile,
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
            final String oldStringValue = baselineFile == null ? null : baselineFile.getWinningValue(path);
            reviewEntry.old = oldStringValue;

            // winning value
            String newWinningValue = sourceFile.getWinningValue(path);
            reviewEntry.winning = newWinningValue;

            // CheckCLDR.CheckStatus.Subtype
            reviewEntry.subtype = info.subtype;

            // comment
            if (!info.htmlMessage.isEmpty()) {
                reviewEntry.comment = info.htmlMessage;
            }
        }
    }
}
