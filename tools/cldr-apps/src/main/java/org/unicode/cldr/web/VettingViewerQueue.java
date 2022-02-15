package org.unicode.cldr.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.VettingViewer;
import org.unicode.cldr.util.VettingViewer.LocalesWithExplicitLevel;
import org.unicode.cldr.web.CLDRProgressIndicator.CLDRProgressTask;

import com.ibm.icu.dev.util.ElapsedTimer;

/**
 * @author srl
 */
public class VettingViewerQueue {
    private static final Logger logger = SurveyLog.forClass(VettingViewerQueue.class);

    private static final boolean DEBUG = false || CldrUtility.getProperty("TEST", false);

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

    private static int gMax = -1;

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
     */
    public enum Status {
        /** Waiting on other users/tasks */
        WAITING,
        /** Processing in progress */
        PROCESSING,
        /** Contents are available */
        READY,
        /** Stopped, due to error or successful completion */
        STOPPED,
    }

    /**
     * What policy should be used when querying the queue?
     *
     * Public for access by SummaryRequest
     *
     * @author srl
     */
    public enum LoadingPolicy {
        /** (Default) - start if not started */
        START,
        /** Don't start if not started. Just check. */
        NOSTART,
        /** Stop. */
        FORCESTOP
    }

    private static class VVOutput {
        public VVOutput(StringBuffer s) {
            output = s;
        }

        private final StringBuffer output;
    }

    private static class QueueEntry {
        private Task currentTask = null;
        private final Map<Organization, VVOutput> output = new TreeMap<>();
    }

    /**
     * Semaphore to ensure that only one vetter accesses VV at a time.
     */
    private static final Semaphore OnlyOneVetter = new Semaphore(1);

    private class Task implements Runnable {

        /**
         * A VettingViewer.ProgressCallback that updates a CLDRProgressTask
         * @author srl295
         */
        private final class CLDRProgressCallback extends VettingViewer.ProgressCallback {
            private final CLDRProgressTask progress;
            private final Thread thread;

            private CLDRProgressCallback(CLDRProgressTask progress, Thread thread) {
                this.progress = progress;
                this.thread = thread;
            }

            private String setRemStr(long now) {
                double per = (double) (now - start) / (double) n;
                long rem = (long) ((maxn - n) * per);
                String remStr = ElapsedTimer.elapsedTime(now, now + rem) + " " + "remaining";
                /*
                 * TODO: explain/encapsulate this magic number! 1500? = 1.5 seconds?
                 * Reference: https://unicode-org.atlassian.net/browse/CLDR-14925
                 */
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
                /*
                 * TODO: explain/encapsulate these magic numbers! 5? 10? 1200? 500?
                 * Reference: https://unicode-org.atlassian.net/browse/CLDR-14925
                 */
                if (n > (maxn - 5)) {
                    maxn = n + 10;
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

        private Thread myThread = null;
        private boolean stop = false;

        private final QueueEntry entry;
        private final SurveyMain sm;
        private int maxn;
        private int n = 0;
        private long start = -1;
        private long last;
        private final Organization usersOrg;
        private String status = "(Waiting my spot in line)";
        private Status statusCode = Status.WAITING; // Need to start out as waiting.

        private void setStatus(String status) {
            this.status = status;
        }

        private final StringBuffer aBuffer = new StringBuffer();

        /**
         * Construct a Runnable object specifically for Priority Items Summary
         *
         * @param entry the QueueEntry
         * @param usersOrg
         */
        private Task(QueueEntry entry, Organization usersOrg) {
            if (DEBUG) {
                System.out.println("Creating task for Priority Items Summary");
            }
            this.sm = CookieSession.sm;
            this.entry = entry;
            this.usersOrg = usersOrg;
            this.maxn = approximatelyHowManyLocalesToCheck() * getMax(sm.getTranslationHintsFile());
        }

        /**
         * Get the approximate number of locales to be summarized
         *
         * @return the number of locales
         *
         * TODO: fix the discrepancies between this code and related code in VettingViewer.
         * The set of locales counted here does not exactly agree with the set of locales determined
         * in VettingViewer. Avoid determining the set of files twice with different code.
         * VettingViewer skips "en" while we do not skip it here.
         * VettingViewer skips defaultContentLocales which is not mentioned here.
         * VettingViewer uses cldrFactory.getAvailable() while here we use SurveyMain.getLocalesSet().
         * The locales should be counted only when the actual set is determined -- in VettingViewer, not here.
         * Reference: https://unicode-org.atlassian.net/browse/CLDR-14925
         */
        private int approximatelyHowManyLocalesToCheck() {
            int localeCount = 0;
            List<Level> levelsToCheck = new ArrayList<>();
            if (VettingViewer.orgIsNeutralForSummary(usersOrg)) {
                levelsToCheck.add(Level.COMPREHENSIVE);
            } else {
                levelsToCheck.addAll(Arrays.asList(Level.values()));
            }
            for (Level lv : levelsToCheck) {
                LocalesWithExplicitLevel lwe = new LocalesWithExplicitLevel(usersOrg, lv);
                for (CLDRLocale l : SurveyMain.getLocalesSet()) {
                    if (lwe.is(l.toString())) {
                        ++localeCount;
                    }
                }
            }
            return localeCount;
        }

        @Override
        public void run() {
            statusCode = Status.WAITING;
            /*
             * TODO: explain this magic number! Why add 100?
             * Reference: https://unicode-org.atlassian.net/browse/CLDR-14925
             */
            final CLDRProgressTask progress = CookieSession.sm.openProgress("vv: Priority Items Summary", maxn + 100);

            if (DEBUG) {
                System.out.println("Starting up vv task: Priority Items Summary");
            }

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
                SurveyLog.logException(logger, re, "While VettingViewer processing Priority Items Summary");
                status = "Exception! " + re;
                // We're done.
                statusCode = Status.STOPPED;
            } finally {
                // don't change the status
                if (progress != null) {
                    progress.close();
                }
            }
        }

        private void processCriticalWork(final CLDRProgressTask progress) {
            status = "Beginning Process, Calculating";
            VettingViewer<Organization> vv;
            vv = new VettingViewer<>(sm.getSupplementalDataInfo(), sm.getSTFactory(),
                new STUsersChoice(sm));
            progress.update("Got VettingViewer");
            statusCode = Status.PROCESSING;
            start = System.currentTimeMillis();
            last = start;
            n = 0;
            vv.setProgressCallback(new CLDRProgressCallback(progress, Thread.currentThread()));

            EnumSet<VettingViewer.Choice> choiceSet = VettingViewer.getChoiceSetForOrg(usersOrg);
            choiceSet.remove(VettingViewer.Choice.abstained);

            if (DEBUG) {
                System.out.println("Starting generation of Priority Items Summary");
            }
            vv.generatePriorityItemsSummary(aBuffer, choiceSet, usersOrg);
            if (myThread.isAlive()) {
                aBuffer.append("<hr/>" + PRE + "Processing time: " + ElapsedTimer.elapsedTime(start) + POST);
                entry.output.put(usersOrg, new VVOutput(aBuffer));
            }
        }

        private String status() {
            StringBuffer bar = SurveyProgressManager.appendProgressBar(new StringBuffer(), n, maxn);
            return status + bar;
        }
    }

    private static final String PRE = "<DIV class='pager'>";
    private static final String POST = "</DIV>";

    /**
     * Return the status of the vetting viewer output request
     *
     * @param sess the CookieSession
     * @param status the array of one new VettingViewerQueue.Status to be filled in
     * @param loadingPolicy the LoadingPolicy
     * @param output the empty Appendable (StringBuilder) to be filled in
     * @return the status message, or null
     * @throws IOException
     * @throws JSONException
     */
    public synchronized String getPriorityItemsSummaryOutput(CookieSession sess, Status[] status,
        LoadingPolicy loadingPolicy, Appendable output) throws IOException, JSONException {
        JSONObject debugStatus = DEBUG ? new JSONObject() : null;
        QueueEntry entry = getEntry(sess);
        if (status == null) {
            status = new Status[1];
        }
        Organization usersOrg = sess.user.vrOrg();
        if (loadingPolicy != LoadingPolicy.FORCESTOP) {
            VVOutput res = entry.output.get(usersOrg);
            if (res != null) {
                status[0] = Status.READY;
                if (output != null) {
                    output.append(res.output);
                }
                stop(entry);
                entry.output.remove(usersOrg);
                return "Stopped on completion";
            }
        } else { /* force stop */
            stop(entry);
            entry.output.remove(usersOrg);
        }
        if (loadingPolicy == LoadingPolicy.FORCESTOP) {
            status[0] = Status.STOPPED;
            if (debugStatus != null) {
                debugStatus.put("t_running", false);
                debugStatus.put("t_statuscode", Status.STOPPED);
                debugStatus.put("t_status", "Stopped on request");
            }
            return "Stopped on request";
        }
        Task t = entry.currentTask;

        if (t != null) {
            String waiting = waitingString();
            if (debugStatus != null) {
                putTaskStatus(debugStatus, t);
            }
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
        }

        if (loadingPolicy == LoadingPolicy.NOSTART) {
            status[0] = Status.STOPPED;
            return PRE + "Not loading. Click the Refresh button to load." + POST;
        }

        // TODO: May be better to use SurveyThreadManager.getExecutorService().invoke() (rather than a raw thread) but would require
        // some restructuring
        t = entry.currentTask = new Task(entry, usersOrg);
        t.myThread = SurveyThreadManager.getThreadFactory().newThread(t);
        t.myThread.start();
        SurveyThreadManager.getThreadFactory().newThread(t);

        status[0] = Status.PROCESSING;
        if (DEBUG) {
            putTaskStatus(debugStatus, t);
        }
        return PRE + "Started new task: " + waitingString() + t.status() + "<hr/>" + POST;
    }

    /**
     * Assemble debugging info
     *
     * @param debugStatus the JSONObject to be filled in with debugging info
     * @param t the Task
     * @throws JSONException
     */
    public void putTaskStatus(JSONObject debugStatus, Task t) throws JSONException {
        debugStatus.put("t_waiting", totalUsersWaiting());
        debugStatus.put("t_running", t.myThread.isAlive());
        debugStatus.put("t_id", t.myThread.getId());
        debugStatus.put("t_statuscode", t.statusCode);
        debugStatus.put("t_status", t.status);
        debugStatus.put("t_progress", t.n);
        debugStatus.put("t_progressmax", t.maxn);
    }

    private String waitingString() {
        int aheadOfMe = totalUsersWaiting();
        return (aheadOfMe > 0) ? ("" + aheadOfMe + " users waiting - ") : "";
    }

    private void stop(QueueEntry entry) {
        Task t = entry.currentTask;
        if (t != null) {
            if (t.myThread.isAlive() && !t.stop) {
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

    private static int totalUsersWaiting() {
        return (OnlyOneVetter.getQueueLength());
    }
}
