/**
 *
 */
package org.unicode.cldr.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.LruMap;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.PathHeader.SectionId;
import org.unicode.cldr.util.Predicate;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StringId;
import org.unicode.cldr.util.TransliteratorUtilities;
import org.unicode.cldr.util.VettingViewer;
import org.unicode.cldr.util.VettingViewer.Choice;
import org.unicode.cldr.util.VettingViewer.LocalesWithExplicitLevel;
import org.unicode.cldr.util.VettingViewer.UsersChoice;
import org.unicode.cldr.util.VettingViewer.VoteStatus;
import org.unicode.cldr.web.CLDRProgressIndicator.CLDRProgressTask;
import org.unicode.cldr.web.UserRegistry.User;

import com.ibm.icu.dev.util.ElapsedTimer;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.DurationFormat;
import com.ibm.icu.util.ULocale;

/**
 * @author srl
 *
 */
public class VettingViewerQueue {

    /*
     *         {
              "code": "narrow-other-nominative",
              "comment": "&lt;value too wide&gt; Too wide by about 100% (with common fonts).",
              "english": "{0}dsp-Imp",
              "old": "{0} dstspn Imp",
              "winning": "{0} dstspn Imp",
              "xpath": "7bd36b15a66d02cf"
            }
          ],
          "header": "dessert-spoon-imperial",
          "notification": "Error",
          "page": "Volume",

     */
    @Schema(description = "single entry of the dashboard that needs review")
    public static final class ReviewEntry {

        @Schema(description = "Code for this entry", example="narrow-other-nominative")
        public String code;

        @Schema(example = "7bd36b15a66d02cf")
        public String xpath;

        @Schema(description = "English text", example = "{0}dsp-Imp")
        public String english;

        @Schema(description = "Previous English value, for EnglishChanged", example = "{0} dstspn Imp")
        public String previousEnglish;

        @Schema(description = "Prior value from a past release", example = "{0} dstspn Imp")
        public String old;

        @Schema(description = "Winning string in this locale", example = "{0} dstspn Imp")
        public String winning;

        @Schema(description = "html comment on the error", example = "&lt;value too wide&gt; Too wide by about 100% (with common fonts).")
        public String comment;

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

    public static final boolean DEBUG = false || CldrUtility.getProperty("TEST", false);

    public static CLDRLocale SUMMARY_LOCALE = CLDRLocale.getInstance(ULocale.forLanguageTag("und-vetting"));

    private static final class VettingViewerQueueHelper {
        static VettingViewerQueue instance = new VettingViewerQueue();
    }

    /**
     * Get the singleton instance of the queue
     *
     * @return
     */
    public static VettingViewerQueue getInstance() {
        return VettingViewerQueueHelper.instance;
    }

    static int gMax = -1;

    /**
     * Count the # of paths in this CLDRFile
     *
     * @param file
     * @return
     */
    private static int pathCount(CLDRFile f) {
        int jj = 0;
        for (@SuppressWarnings("unused")
        String s : f) {
            jj++;
        }
        return jj;
    }

    /**
     * Get the max expected items in a CLDRFile
     *
     * @param f
     * @return
     */
    private synchronized int getMax(CLDRFile f) {
        if (gMax == -1) {
            gMax = pathCount(f);
        }
        return gMax;
    }

    /**
     * A unique key for hashes
     */
    private static final String KEY = VettingViewerQueue.class.getName();

    /**
     * Status of a Task
     *
     * @author srl
     *
     */
    public enum Status {
        /** Waiting on other users/tasks */
        WAITING,
        /** Processing in progress */
        PROCESSING,
        /** Contents are available */
        READY,
        /** Stopped, due to some err */
        STOPPED,
    }

    /**
     * What policy should be used when querying the queue?
     *
     * @author srl
     *
     */
    public enum LoadingPolicy {
        /** (Default) - start if not started */
        START,
        /** Don't start if not started. Just check. */
        NOSTART,
        /** Force a restart, ignoring old contents */
        FORCERESTART,
        /** Stop. */
        FORCESTOP
    }

    private static class VVOutput {
        public VVOutput(StringBuffer s) {
            output = s;
        }

        private StringBuffer output = new StringBuffer();
        long start = System.currentTimeMillis();

        public String getAge() {
            long now = System.currentTimeMillis();
            return "<span class='age'>Last generated "
                + DurationFormat.getInstance(SurveyMain.TRANS_HINT_LOCALE).formatDurationFromNow(start - now) + "</span>";
        }
    }

    private static class QueueEntry {
        public Task currentTask = null;
        public Map<Pair<CLDRLocale, Organization>, VVOutput> output = new TreeMap<>();
    }

    /**
     * Semaphore to ensure that only one vetter accesses VV at a time.
     */
    private static final Semaphore OnlyOneVetter = new Semaphore(1);

    public class Task implements Runnable {

        /**
         * A VettingViewer.ProgressCallback that updates a CLDRProgressTask
         * @author srl295
         */
        private final class CLDRProgressCallback extends VettingViewer.ProgressCallback {
            private final CLDRProgressTask progress;
            private Thread thread;

            private CLDRProgressCallback(CLDRProgressTask progress, Thread thread) {
                this.progress = progress;
                this.thread = thread;
            }

            public String setRemStr(long now) {
                double per = (double) (now - start) / (double) n;
                rem = (long) ((maxn - n) * per);
                String remStr = ElapsedTimer.elapsedTime(now, now + rem) + " " + "remaining";
                if (rem <= 1500) {
                    remStr = "Finishing...";
                }
                setStatus(remStr);
                return remStr;
            }

            @Override
            public void nudge() {
                if (!myThread.isAlive()) {
                    throw new RuntimeException("Not Running- stop now.");
                }
                long now = System.currentTimeMillis();
                n++;
                // System.err.println("Nudged: " + n);
                if (n > (maxn - 5)) {
                    maxn = n + 10;
                    if (!isSummary && n > gMax) {
                        gMax = n;
                    }
                }

                if ((now - last) > 1200) {
                    last = now;
                    if (n > 500) {
                        progress.update(n, setRemStr(now));
                    } else {
                        progress.update(n);
                    }
                }
            }

            @Override
            public void done() {
                progress.update("Done!");
            }

            @Override
            public boolean isStopped() {
                // if the calling thread is gone, stop processing
                return !(thread.isAlive());
            }
        }

        Thread myThread = null;
        public boolean stop = false;

        public CLDRLocale locale;
        private QueueEntry entry;
        SurveyMain sm;
        VettingViewer<Organization> vv;
        public int maxn;
        public int n = 0;
        final public boolean isSummary;
        public long start = -1;
        public long last;
        public long rem = -1;
        private final String st_org;
        final Level usersLevel;
        final Organization usersOrg;
        String status = "(Waiting my spot in line)";
        public Status statusCode = Status.WAITING; // Need to start out as

        // waiting.

        void setStatus(String status) {
            this.status = status;
        }

        public float progress() {
            if (maxn <= 0)
                return (float) 0.0;
            return ((float) n) / ((float) maxn);
        }

        StringBuffer aBuffer = new StringBuffer();

        public Task(QueueEntry entry, CLDRLocale locale, SurveyMain sm, String baseUrl, Level usersLevel,
            Organization usersOrg, final String st_org) {
            isSummary = isSummary(locale);
            if (DEBUG)
                System.err.println("Creating task " + locale.toString());

            int baseMax = getMax(sm.getTranslationHintsFile());
            if (isSummary) {
                maxn = 0;
                List<Level> levelsToCheck = new ArrayList<>();
                if (usersOrg.equals(Organization.surveytool)) {
                    levelsToCheck.add(Level.COMPREHENSIVE);
                } else {
                    levelsToCheck.addAll(Arrays.asList(Level.values()));
                }
                for (Level lv : levelsToCheck) {
                    LocalesWithExplicitLevel lwe = new LocalesWithExplicitLevel(usersOrg, lv);
                    for (CLDRLocale l : SurveyMain.getLocalesSet()) {
                        if (lwe.is(l.toString())) {
                            maxn += baseMax;
                        }
                    }
                }
            } else {
                maxn = baseMax;
            }
            this.locale = locale;
            this.st_org = st_org;
            this.entry = entry;
            this.sm = sm;
            this.usersLevel = usersLevel;
            this.usersOrg = usersOrg;
        }

        /**
         * @param locale
         * @return
         */
        public boolean isSummary(CLDRLocale locale) {
            return locale == SUMMARY_LOCALE;
        }

        @Override
        public void run()  {
            statusCode = Status.WAITING;
            final CLDRProgressTask progress = CookieSession.sm.openProgress("vv:" + locale, maxn + 100);

            VettingViewer<Organization> vv = null;

            if (DEBUG)
                System.err.println("Starting up vv task:" + locale);

            try {
                status = "Waiting...";
                progress.update("Waiting...");
                OnlyOneVetter.acquire();
                try {
                    if (stop) {
                        status = "Stopped on request.";
                        statusCode = Status.STOPPED;
                        return;
                    }
                    processCriticalWork(progress);
                } finally {
                    OnlyOneVetter.release();
                }
                status = "Finished.";
                statusCode = Status.READY;
            } catch (RuntimeException | InterruptedException re) {
                SurveyLog.logException(re, "While VettingViewer processing " + locale);
                status = "Exception! " + re.toString();
                // We're done.
                statusCode = Status.STOPPED;
            } finally {
                // don't change the status
                if (progress != null)
                    progress.close();
                vv = null; // release vv
            }
        }

        private void processCriticalWork(final CLDRProgressTask progress) {
            VettingViewer<Organization> vv;
            status = "Beginning Process, Calculating";

            vv = new VettingViewer<>(sm.getSupplementalDataInfo(), sm.getSTFactory(),
                getUsersChoice(sm), "Winning " + SurveyMain.getNewVersion());
            progress.update("Got VettingViewer");
            statusCode = Status.PROCESSING;
            start = System.currentTimeMillis();
            last = start;
            n = 0;
            vv.setProgressCallback(new CLDRProgressCallback(progress, Thread.currentThread()));

            EnumSet<VettingViewer.Choice> choiceSet = EnumSet.allOf(VettingViewer.Choice.class);
            if (usersOrg.equals(Organization.surveytool)) {
                choiceSet = EnumSet.of(
                    VettingViewer.Choice.error,
                    VettingViewer.Choice.warning,
                    VettingViewer.Choice.hasDispute,
                    VettingViewer.Choice.notApproved);
            }

            if (!isSummary(locale)) {
                vv.generateHtmlErrorTables(aBuffer, choiceSet, locale.getBaseName(), usersOrg, usersLevel, false);
            } else {
                if (DEBUG) {
                    System.err.println("Starting summary gen..");
                }
                /*
                 * TODO: remove call to getLocalesWithVotes here, unless it has necessary side-effects.
                 * Its return value was formerly an unused argument to generateSummaryHtmlErrorTables
                 */
                getLocalesWithVotes(st_org);
                vv.generateSummaryHtmlErrorTables(aBuffer, choiceSet, usersOrg);
            }
            if (myThread.isAlive()) {
                aBuffer.append("<hr/>" + PRE + "Processing time: " + ElapsedTimer.elapsedTime(start) + POST);
                entry.output.put(new Pair<>(locale, usersOrg), new VVOutput(aBuffer));
            }
        }

        public String status() {
            StringBuffer bar = SurveyProgressManager.appendProgressBar(new StringBuffer(), n, maxn);
            return status + bar;
        }

        Predicate<String> fLocalesWithVotes = null;

        private synchronized Predicate<String> getLocalesWithVotes(String st_org) {
            if (fLocalesWithVotes == null) {
                fLocalesWithVotes = createLocalesWithVotes(st_org);
            }
            return fLocalesWithVotes;
        }

    }

    /**
     *
     * @param st_org
     * @return
     *
     * Called by getLocalesWithVotes and writeVettingViewerOutput
     */
    private Predicate<String> createLocalesWithVotes(String st_org) {
        final Set<CLDRLocale> allLocs = SurveyMain.getLocalesSet();
        /*
         * a. Any locale in Locales.txt for organization (excluding *). Use
         * StandardCodes.make().getLocaleCoverageLocales(String organization).
         * b. All locales listed in the user-languages of any user in the
         * organization c. Any locale with at least one vote by a user in that
         * organization
         */
        final Organization vr_org = UserRegistry.computeVROrganization(st_org); /*
                                                                                * VoteResolver
                                                                                * organization
                                                                                * name
                                                                                */
        final Set<Organization> covOrgs = StandardCodes.make().getLocaleCoverageOrganizations();

        final Set<String> aLocs = new HashSet<>();
        /*
         * TODO: a warning appears for the next line; should be vr_org instead of vr_org.name()?
         * How to exercise this code and test that?
         */
        if (covOrgs.contains(vr_org.name())) {
            aLocs.addAll(StandardCodes.make().getLocaleCoverageLocales(vr_org.name()));
            System.err.println("localesWithVotes st_org=" + st_org + ", vr_org=" + vr_org + ", aLocs=" + aLocs.size());
        } else {
            System.err.println("localesWithVotes st_org=" + st_org + ", vr_org=" + vr_org + ", aLocs= (not a cov org)");
        }

        // b -
        // select distinct cldr_interest.forum from cldr_interest where exists
        // (select * from cldr_users where cldr_users.id=cldr_interest.uid and
        // cldr_users.org='SurveyTool');
        final Set<String> covGroupsForOrg = UserRegistry.getCovGroupsForOrg(st_org);

        // c. any locale with at least 1 vote by a user in that org
        final Set<CLDRLocale> anyVotesFromOrg = UserRegistry.anyVotesForOrg(st_org);

        System.err.println("CovGroupsFor " + st_org + "=" + covGroupsForOrg.size() + ", anyVotes=" + anyVotesFromOrg.size()
            + "  - " + SurveyMain.freeMem());

        Predicate<String> localesWithVotes = new Predicate<String>() {
            final boolean showAllLocales = (vr_org == Organization.surveytool)
                || CldrUtility.getProperty("THRASH_ALL_LOCALES", false);

            @Override
            public boolean is(String item) {
                if (showAllLocales)
                    return true;
                CLDRLocale loc = CLDRLocale.getInstance(item);
                return (aLocs.contains(item) || // a
                    covGroupsForOrg.contains(loc.getBaseName()) || // b
                    anyVotesFromOrg.contains(loc)); // c
            }
        };

        int lcount = 0;
        StringBuilder sb = new StringBuilder();
        for (CLDRLocale l : allLocs) {
            if (localesWithVotes.is(l.getBaseName())) {
                lcount++;
                sb.append(l + " ");
            }
        }
        System.err.println("CovGroupsFor " + st_org + "= union = " + lcount + " - " + sb);

        return localesWithVotes;
    }

    private static final String PRE = "<DIV class='pager'>";
    private static final String POST = "</DIV>";

    public JSONArray getErrorOnPath(CLDRLocale locale, WebContext ctx, CookieSession sess, String path) {
        Level usersLevel;
        Organization usersOrg;
        if (ctx != null) {
            usersLevel = Level.get(ctx.getEffectiveCoverageLevel(ctx.getLocale().toString()));
            sess = ctx.session;
        } else {
            String levelString = sess.settings().get(SurveyMain.PREF_COVLEV, WebContext.PREF_COVLEV_LIST[0]);
            usersLevel = Level.get(levelString);
        }

        usersOrg = Organization.fromString(sess.user.voterOrg());

        SurveyMain sm = CookieSession.sm;
        VettingViewer<Organization> vv = new VettingViewer<>(sm.getSupplementalDataInfo(), sm.getSTFactory(),
            getUsersChoice(sm), "Winning " + SurveyMain.getNewVersion());

        EnumSet<VettingViewer.Choice> choiceSet = EnumSet.allOf(VettingViewer.Choice.class);
        if (usersOrg.equals(Organization.surveytool)) {
            choiceSet = EnumSet.of(
                VettingViewer.Choice.error,
                VettingViewer.Choice.warning,
                VettingViewer.Choice.hasDispute,
                VettingViewer.Choice.notApproved);
        }
        ArrayList<String> out = vv.getErrorOnPath(choiceSet, locale.getBaseName(), usersOrg, usersLevel, path);
        JSONArray result = new JSONArray();
        for (String issues : out) {
            result.put(issues);
        }
        return result;
    }

    /**
     * Get the json for the Dashboard ("review")
     *
     * @param output
     * @param sourceFile
     * @param baselineFile
     * @param sorted
     * @param choices
     * @param localeID
     * @param nonVettingPhase
     * @param quick
     * @param ctx
     *
     * Called only by writeVettingViewerOutput. Deprecated code path and URL.
     */
    @Deprecated
    private void getJSONReview(Appendable output, CLDRFile sourceFile, CLDRFile baselineFile,
        Relation<R2<SectionId, PageId>, VettingViewer<Organization>.WritingInfo> sorted,
        EnumSet<Choice> choices,
        CLDRLocale locale,
        boolean nonVettingPhase,
        boolean quick, WebContext ctx) {
        try {
            JSONObject reviewInfo = new JSONObject();
            JSONArray notificationsCount = new JSONArray();
            List<String> notifications = new ArrayList<>();
            SurveyMain sm = CookieSession.sm;
            CLDRFile englishFile = sm.getSTFactory().make("en", true);

            for (Choice choice : choices) {
                notificationsCount.put(new JSONObject().put("name", choice.buttonLabel.replace(' ', '_')).put("description", choice.description));
                notifications.add(choice.buttonLabel);
            }

            reviewInfo.put("notification", notificationsCount);

            Relation<Row.R4<Choice, SectionId, PageId, String>, VettingViewer<Organization>.WritingInfo> notificationsList
               = getNotificationsList(sorted);

            JSONArray allNotifications = new JSONArray();
            for (Entry<R4<Choice, SectionId, PageId, String>, Set<VettingViewer<Organization>.WritingInfo>> entry : notificationsList.keyValuesSet()) {

                String notificationName = entry.getKey().get0().buttonLabel.replace(' ', '_');
                int notificationNumber = entry.getKey().get0().order;

                String sectionName = entry.getKey().get1().name();
                String pageName = entry.getKey().get2().name();
                String headerName = entry.getKey().get3();

                // TODO: I suspect the following logic is broken, it seems to insert a string with a literal "null"
                // into the output.
                if (allNotifications.optJSONObject(notificationNumber) == null) {
                    allNotifications.put(notificationNumber, new JSONObject().put(notificationName, new JSONObject()));
                }

                JSONObject sections = allNotifications.getJSONObject(notificationNumber).getJSONObject(notificationName);

                if (sections.optJSONObject(sectionName) == null) {
                    sections.accumulate(sectionName, new JSONObject());
                }
                JSONObject pages = sections.getJSONObject(sectionName);

                if (pages.optJSONObject(pageName) == null) {
                    pages.accumulate(pageName, new JSONObject());
                }
                JSONObject header = pages.getJSONObject(pageName);

                JSONArray allContent = new JSONArray();
                //real info
                for (VettingViewer<Organization>.WritingInfo info : entry.getValue()) {
                    JSONObject content = new JSONObject();
                    String code = info.codeOutput.getCode();
                    String path = info.codeOutput.getOriginalPath();
                    Set<Choice> choicesForPath = info.problems;

                    //code
                    content.put("code", code);
                    content.put("path", sm.xpt.getByXpath(path));

                    //english
                    if (choicesForPath.contains(Choice.englishChanged)) {
                        String winning = englishFile.getWinningValue(path);
                        String cellValue = winning == null ? "<i>missing</i>" : TransliteratorUtilities.toHTML
                            .transform(winning);
                        String previous = VettingViewer.getOutdatedPaths().getPreviousEnglish(path);
                        if (previous != null) {
                            cellValue += "<br><span style='color:#900'><b>OLD: </b>"
                                + TransliteratorUtilities.toHTML.transform(previous) + "</span>";
                        } else {
                            cellValue += "<br><b><i>missing</i></b>";
                        }
                        content.put("english", cellValue);
                    } else {
                        content.put("english", englishFile.getWinningValue(path));
                    }

                    //old release
                    final String oldStringValue = baselineFile == null ? null : baselineFile.getWinningValue(path);
                    content.put("old", oldStringValue);

                    //winning value
                    String newWinningValue = sourceFile.getWinningValue(path);
                    if (CharSequences.equals(newWinningValue, oldStringValue)) {
                        newWinningValue = "=";
                    }
                    content.put("winning", newWinningValue);

                    //comment
                    String comment = "";
                    if (!info.htmlMessage.isEmpty()) {
                        comment = info.htmlMessage;
                    }
                    content.put("comment", comment.replace("\"", "&quot;"));

                    content.put("id", StringId.getHexId(info.codeOutput.getOriginalPath()));
                    allContent.put(content);
                }
                header.put(headerName, allContent);

            }
            reviewInfo.put("allNotifications", allNotifications);

            //hidden info
            if (ctx != null) {
                // TODO: remove. New API handles this separately
                ReviewHide review = new ReviewHide();
                reviewInfo.put("hidden", review.getHiddenField(ctx.userId(), ctx.getLocale().toString()));
            }

            // TODO: Why is this included? Should already be visible to the client
            // in the locmap under 'dir'
            reviewInfo.put("direction", sm.getHTMLDirectionFor(locale));

            output.append(reviewInfo.toString());
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Write the Dashboard
     *
     * @param locale
     * @param aBuffer
     * @param ctx the WebContext, never null
     * @param sess the CookieSession
     */
    public void writeVettingViewerOutput(CLDRLocale locale, StringBuffer aBuffer, WebContext ctx, CookieSession sess) {
        String levelString = sess.getLevelString();
        UserRegistry.User user = sess.user;
        Level usersLevel = getLevelForLocale(ctx, locale.getBaseName(), levelString);

        writeVettingViewerOutput(locale, aBuffer, ctx, user, usersLevel);
    }

    public void writeVettingViewerOutput(CLDRLocale locale, StringBuffer aBuffer, WebContext ctx, UserRegistry.User user, Level usersLevel) {
        final SurveyMain sm = CookieSession.sm;
        String loc = locale.getBaseName();
        /*
         * if no coverage level set, use default one
         */
        Organization usersOrg = Organization.fromString(user.voterOrg());
        final boolean quick = false;
        final String st_org = user.org;
        STFactory sourceFactory = sm.getSTFactory();
        VettingViewer<Organization> vv = new VettingViewer<>(sm.getSupplementalDataInfo(), sourceFactory,
            getUsersChoice(sm), "Winning " + SurveyMain.getNewVersion());

        EnumSet<VettingViewer.Choice> choiceSet = getChoiceSetForOrg(usersOrg);

        if (locale != SUMMARY_LOCALE) {
            /*
             * sourceFile provides the current winning values, taking into account recent votes.
             * baselineFile provides the "baseline" (a.k.a. "trunk") values, i.e., the values that
             * are in the current XML in the cldr version control repository. The baseline values
             * are generally the last release values plus any changes that have been made by the
             * technical committee by committing directly to version control rather than voting.
             */
            CLDRFile sourceFile = sourceFactory.make(loc, true);
            Factory baselineFactory = CLDRConfig.getInstance().getCommonAndSeedAndMainAndAnnotationsFactory();
            CLDRFile baselineFile = baselineFactory.make(loc, true);
            Relation<R2<SectionId, PageId>, VettingViewer<Organization>.WritingInfo> file;
            file = vv.generateFileInfoReview(choiceSet, loc, usersOrg, usersLevel, quick, sourceFile, quick ? null : baselineFile);
            this.getJSONReview(aBuffer, sourceFile, baselineFile, file, choiceSet, locale, true, quick, ctx);
        } else {
            if (DEBUG) {
                System.err.println("Starting summary gen..");
            }
            /*
             * TODO: remove call to createLocalesWithVotes here, unless it has necessary side-effects.
             * Its return value was formerly an unused argument to generateSummaryHtmlErrorTables
             */
            createLocalesWithVotes(st_org);
            vv.generateSummaryHtmlErrorTables(aBuffer, choiceSet, usersOrg);
        }
    }

    /**
     * Get Vetting Viewer output as an object
     * @param locale
     * @param user
     * @param usersLevel
     * @return
     */
    public ReviewOutput getDashboardOutput(CLDRLocale locale, UserRegistry.User user, Level usersLevel) {
        final SurveyMain sm = CookieSession.sm;
        String loc = locale.getBaseName();
        /*
         * if no coverage level set, use default one
         */
        Organization usersOrg = Organization.fromString(user.voterOrg());
        final boolean quick = false;
        final String st_org = user.org;
        STFactory sourceFactory = sm.getSTFactory();
        VettingViewer<Organization> vv = new VettingViewer<>(sm.getSupplementalDataInfo(), sourceFactory,
            getUsersChoice(sm), "Winning " + SurveyMain.getNewVersion());

        EnumSet<VettingViewer.Choice> choiceSet = getChoiceSetForOrg(usersOrg);

        // This is to prvent confusion, because parallel functions
        // use the locale to identify a summary locale.
        if (locale == SUMMARY_LOCALE) {
            throw new IllegalArgumentException("SUMMARY_LOCALE not supported via getDashboardOutput()");
        }
        /*
         * sourceFile provides the current winning values, taking into account recent votes.
         * baselineFile provides the "baseline" (a.k.a. "trunk") values, i.e., the values that
         * are in the current XML in the cldr version control repository. The baseline values
         * are generally the last release values plus any changes that have been made by the
         * technical committee by committing directly to version control rather than voting.
         */
        CLDRFile sourceFile = sourceFactory.make(loc, true);
        Factory baselineFactory = CLDRConfig.getInstance().getCommonAndSeedAndMainAndAnnotationsFactory();
        CLDRFile baselineFile = baselineFactory.make(loc, true);
        Relation<R2<SectionId, PageId>, VettingViewer<Organization>.WritingInfo> file;
        file = vv.generateFileInfoReview(choiceSet, loc, usersOrg, usersLevel, quick, sourceFile, quick ? null : baselineFile);
        return getDashboardOutput(sourceFile, baselineFile, file, choiceSet, locale);
    }

    @Schema(description = "Heading for a portion of the notifications")
    public static final class ReviewNotification{

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


    @Schema(description = "Output of Dashboard output")
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
            if(unlessMatches != null && unlessMatches.notification.equals(notificationName)) {
                return unlessMatches;
            } else {
                return add(notificationName);
            }
        }

        public ReviewNotification add(ReviewNotification notification) {
            this.notifications.add(notification);
            return notification;
        }
    }

    /**
     * Get Dashboard output as an object
     * @param sourceFile
     * @param baselineFile
     * @param sorted
     * @param choices
     * @param locale
     * @return
     */
    private ReviewOutput getDashboardOutput(CLDRFile sourceFile, CLDRFile baselineFile,
        Relation<R2<SectionId, PageId>, VettingViewer<Organization>.WritingInfo> sorted,
        EnumSet<Choice> choices,
        CLDRLocale locale) {

        ReviewOutput reviewInfo = new ReviewOutput();

        addNotificationEntries(sourceFile, baselineFile, sorted, reviewInfo);
        return reviewInfo;
    }

    private void addNotificationEntries(CLDRFile sourceFile, CLDRFile baselineFile, Relation<R2<SectionId, PageId>, VettingViewer<Organization>.WritingInfo> sorted,
        ReviewOutput reviewInfo) {
        Relation<Row.R4<Choice, SectionId, PageId, String>, VettingViewer<Organization>.WritingInfo> notificationsList
            = getNotificationsList(sorted);

        ReviewNotification notification = null;

        SurveyMain sm = CookieSession.sm;
        CLDRFile englishFile = sm.getEnglishFile();

        for (Entry<R4<Choice, SectionId, PageId, String>, Set<VettingViewer<Organization>.WritingInfo>> entry : notificationsList.keyValuesSet()) {

            notification = getNextNotification(reviewInfo, notification, entry);

            addNotificiationGroup(sourceFile, baselineFile, notification, englishFile, entry);
        }
    }

    private ReviewNotification getNextNotification(ReviewOutput reviewInfo, ReviewNotification notification,
        Entry<R4<Choice, SectionId, PageId, String>, Set<VettingViewer<Organization>.WritingInfo>> entry) {
        String notificationName = entry.getKey().get0().buttonLabel.replace(' ', '_');

        notification = reviewInfo.add(notificationName, notification);
        return notification;
    }

    private void addNotificiationGroup(CLDRFile sourceFile, CLDRFile baselineFile, ReviewNotification notification, CLDRFile englishFile,
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

            //old release
            final String oldStringValue = baselineFile == null ? null : baselineFile.getWinningValue(path);
            reviewEntry.old = oldStringValue;

            //winning value
            String newWinningValue = sourceFile.getWinningValue(path);
            reviewEntry.winning = newWinningValue;

            //comment
            String comment = "";
            if (!info.htmlMessage.isEmpty()) {
                reviewEntry.comment = info.htmlMessage;
            }
        }
    }

    private Relation<Row.R4<Choice, SectionId, PageId, String>, VettingViewer<Organization>.WritingInfo> getNotificationsList(
        Relation<R2<SectionId, PageId>, VettingViewer<Organization>.WritingInfo> sorted) {
        Relation<Row.R4<Choice, SectionId, PageId, String>, VettingViewer<Organization>.WritingInfo> notificationsList
        = Relation.of(new TreeMap<Row.R4<Choice, SectionId, PageId, String>,
            Set<VettingViewer<Organization>.WritingInfo>>(), TreeSet.class);

        //TODO we can prob do it in only one loop, but with that we can sort
        for (Entry<R2<SectionId, PageId>, Set<VettingViewer<Organization>.WritingInfo>> entry0 : sorted.keyValuesSet()) {
            final Set<VettingViewer<Organization>.WritingInfo> rows = entry0.getValue();
            for (VettingViewer<Organization>.WritingInfo pathInfo : rows) {
                Set<Choice> choicesForPath = pathInfo.problems;
                SectionId section = entry0.getKey().get0();
                PageId subsection = entry0.getKey().get1();
                for (Choice choice : choicesForPath) {
                    //reviewInfo
                    notificationsList.put(Row.of(choice, section, subsection, pathInfo.codeOutput.getHeader()), pathInfo);
                }
            }
        }
        return notificationsList;
    }


    private Level getLevelForLocale(WebContext ctx, String loc, String levelString) {
        Level usersLevel;
        if (levelString.equals("default")) {
            usersLevel = Level.get(ctx.getEffectiveCoverageLevel(loc));
        } else {
            usersLevel = Level.get(levelString);
        }
        return usersLevel;
    }

    private EnumSet<VettingViewer.Choice> getChoiceSetForOrg(Organization usersOrg) {
        EnumSet<VettingViewer.Choice> choiceSet = EnumSet.allOf(VettingViewer.Choice.class);
        if (usersOrg.equals(Organization.surveytool)) {
            choiceSet = EnumSet.of(
                VettingViewer.Choice.error,
                VettingViewer.Choice.warning,
                VettingViewer.Choice.hasDispute,
                VettingViewer.Choice.notApproved);
        }
        return choiceSet;
    }

    /**
     * Return the status of the vetting viewer output request. If a different
     * locale is requested, the previous request will be cancelled.
     *
     * @param ctx
     * @param locale
     * @param output
     *            if there is output, it will be written here. Or not, if it's
     *            null
     * @param jsonStatus TODO
     * @return status message
     * @throws IOException
     * @throws JSONException
     */
    public synchronized String getVettingViewerOutput(WebContext ctx, CookieSession sess, CLDRLocale locale, Status[] status,
        LoadingPolicy forceRestart, Appendable output, JSONObject jStatus) throws IOException, JSONException {
        if (sess == null)
            sess = ctx.session;
        SurveyMain sm = CookieSession.sm; // TODO: ctx.sm if ctx never null
        Pair<CLDRLocale, Organization> key = new Pair<>(locale, sess.user.vrOrg());
        boolean isSummary = (locale == SUMMARY_LOCALE);
        QueueEntry entry = null;
        entry = getEntry(sess);
        if (status == null)
            status = new Status[1];
        jStatus.put("isSummary", isSummary);
        jStatus.put("locale", locale);
        if (forceRestart != LoadingPolicy.FORCERESTART &&
            forceRestart != LoadingPolicy.FORCESTOP) {
            VVOutput res = entry.output.get(key);
            if (res != null) {
                status[0] = Status.READY;
                if (output != null) {
                    output.append(res.getAge()).append("<br>").append(res.output);
                }
                return null;
            }
        } else { /* force restart */
            stop(ctx, locale, entry);
            entry.output.remove(key);
        }

        if (forceRestart == LoadingPolicy.FORCESTOP) {
            status[0] = Status.STOPPED;
            jStatus.put("t_running", false);
            jStatus.put("t_statuscode", Status.STOPPED);
            jStatus.put("t_status", "Stopped on request");
            return "Stopped on request";
        }
        Task t = entry.currentTask;
        CLDRLocale didKill = null;

        if (t != null) {
            String waiting = waitingString(t.sm.startupThread);
            putTaskStatus(jStatus, t);
            if (t.locale.equals(locale)) {
                status[0] = Status.PROCESSING;
                if (t.myThread.isAlive()) {
                    // get progress from current thread
                    status[0] = t.statusCode;
                    if (status[0] != Status.WAITING)
                        waiting = "";
                    return PRE + "In Progress: " + waiting + t.status() + POST;
                } else {
                    return PRE + "Stopped (refresh if stuck) " + t.status() + POST;
                }
            } else if (forceRestart == LoadingPolicy.NOSTART) {
                status[0] = Status.STOPPED;
                jStatus.put("your_other_running", t.myThread.isAlive());
                if (t.myThread.isAlive()) {
                    return PRE + " You have another locale being loaded: " + t.locale + POST;
                } else {
                    return PRE + "Refresh if stuck." + POST;
                }
            } else {
                if (t.myThread.isAlive()) {
                    didKill = t.locale;
                }
                stop(ctx, t.locale, entry);
            }
        }

        if (forceRestart == LoadingPolicy.NOSTART) {
            status[0] = Status.STOPPED;
            return PRE + "Not loading. Click the Refresh button to load." + POST;
        }

        String baseUrl = null;
        Level usersLevel;
        Organization usersOrg;
        if (ctx != null) {
            usersLevel = Level.get(ctx.getEffectiveCoverageLevel(ctx.getLocale().toString()));
        } else {
            String levelString = sess.settings().get(SurveyMain.PREF_COVLEV, WebContext.PREF_COVLEV_LIST[0]);

            usersLevel = Level.get(levelString);
        }
        usersOrg = sess.user.vrOrg();

        // TODO: May be better to use SurveyThreadManager.getExecutorService().invoke() (rather than a raw thread) but would require
        // some restructuring
        t = entry.currentTask = new Task(entry, locale, sm, baseUrl, usersLevel, usersOrg, sess.user.org);
        t.myThread = SurveyThreadManager.getThreadFactory().newThread(t);
        t.myThread.start();
        SurveyThreadManager.getThreadFactory().newThread(t);

        status[0] = Status.PROCESSING;
        String killMsg = "";
        if (didKill != null) {
            killMsg = " (Note: Stopped loading: " + didKill.toULocale().getDisplayName(SurveyMain.TRANS_HINT_LOCALE) + ")";
        }
        putTaskStatus(jStatus, t);
        return PRE + "Started new task: " + waitingString(t.sm.startupThread) + t.status() + "<hr/>" + killMsg + POST;
    }

    /**
     * @param jStatus
     * @param t
     * @throws JSONException
     */
    public void putTaskStatus(JSONObject jStatus, Task t) throws JSONException {
        jStatus.put("t_waiting", totalUsersWaiting(t.sm.startupThread));
        jStatus.put("t_locale", t.locale);
        jStatus.put("t_running", t.myThread.isAlive());
        jStatus.put("t_id", t.myThread.getId());
        jStatus.put("t_statuscode", t.statusCode);
        jStatus.put("t_status", t.status);
        jStatus.put("t_progress", t.n);
        jStatus.put("t_progressmax", t.maxn);
    }

    private String waitingString(SurveyThreadManager startupThread) {
        int aheadOfMe = (totalUsersWaiting(startupThread));
        String waiting = (aheadOfMe > 0) ? ("" + aheadOfMe + " users waiting - ") : "";
        return waiting;
    }

    private void stop(WebContext ctx, CLDRLocale locale, QueueEntry entry) {
        Task t = entry.currentTask;
        if (t != null) {
            if (t.myThread.isAlive() && t.stop  == false) {
                t.stop = true;
                t.myThread.interrupt();
            }
            entry.currentTask = null;
        }
    }

    private QueueEntry getEntry(CookieSession session) {
        QueueEntry entry = (QueueEntry) session.get(KEY);
        if (entry == null) {
            entry = new QueueEntry();
            session.put(KEY, entry);
        }
        return entry;
    }

    LruMap<CLDRLocale, BallotBox<UserRegistry.User>> ballotBoxes = new LruMap<>(8);

    BallotBox<UserRegistry.User> getBox(SurveyMain sm, CLDRLocale loc) {
        BallotBox<User> box = ballotBoxes.get(loc);
        if (box == null) {
            box = sm.getSTFactory().ballotBoxForLocale(loc);
            ballotBoxes.put(loc, box);
        }
        return box;
    }

    private UsersChoice<Organization> getUsersChoice(final SurveyMain sm) {
        return new STUsersChoice(sm);
    }

    private class STUsersChoice implements UsersChoice<Organization> {
        private final SurveyMain sm;

        STUsersChoice(final SurveyMain msm) {
            this.sm = msm;
        }

        @Override
        public String getWinningValueForUsersOrganization(CLDRFile cldrFile, String path, Organization user) {
            CLDRLocale loc = CLDRLocale.getInstance(cldrFile.getLocaleID());
            BallotBox<User> ballotBox = getBox(sm, loc);
            return ballotBox.getResolver(path).getOrgVote(user);
        }

        @Override
        public VoteStatus getStatusForUsersOrganization(CLDRFile cldrFile, String path, Organization orgOfUser) {
            CLDRLocale loc = CLDRLocale.getInstance(cldrFile.getLocaleID());
            BallotBox<User> ballotBox = getBox(sm, loc);
            return ballotBox.getResolver(path).getStatusForOrganization(orgOfUser);
        }

    }

    private static int totalUsersWaiting(SurveyThreadManager st) {
        return (OnlyOneVetter.getQueueLength());
    }
}
