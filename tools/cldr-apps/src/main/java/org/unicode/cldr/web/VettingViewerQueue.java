package org.unicode.cldr.web;

import com.ibm.icu.dev.util.ElapsedTimer;
import java.io.IOException;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.unicode.cldr.util.*;
import org.unicode.cldr.util.TimeDiff;
import org.unicode.cldr.web.CLDRProgressIndicator.CLDRProgressTask;
import org.unicode.cldr.web.api.LocaleCompletion;

/**
 * @author srl
 */
public class VettingViewerQueue {
    private static final Logger logger = SurveyLog.forClass(VettingViewerQueue.class);

    private static final boolean DEBUG = false || CldrUtility.getProperty("TEST", false);
    private static final String WAITING_IN_LINE_MESSAGE = "Waiting in line";

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
     * @param f
     * @return
     */
    private static int pathCount(CLDRFile f) {
        int jj = 0;
        for (@SuppressWarnings("unused") String s : f) {
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

    /** A unique key for storing QueueEntry objects in QueueMemberId objects */
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
     * <p>Public for access by SummaryRequest
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

    /** Semaphore to ensure that only one vetter accesses VV at a time. */
    private static final Semaphore OnlyOneVetter = new Semaphore(1);

    private class Task implements Runnable {

        /**
         * A VettingViewer.ProgressCallback that updates a CLDRProgressTask
         *
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
                String remStr = "Estimated completion: " + TimeDiff.timeDiff(now, now - rem);
                if (rem <= 1500) { // Less than 1.5 seconds remaining
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
                 * Reference: https://unicode-org.atlassian.net/browse/CLDR-15369
                 */
                if (n > (maxn - 5)) {
                    maxn = n + 10;
                }
                if ((now - last) > 1200) {
                    if (DEBUG) {
                        System.out.println(
                                "Task.nudge() for Priority Items Summary, " + taskDescription());
                    }
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
                // note: this method is possibly never called
                progress.update("Done!");
            }

            @Override
            public boolean isStopped() {
                // if the calling thread is gone, stop processing
                return stop || !(thread.isAlive());
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
        private String status = WAITING_IN_LINE_MESSAGE;
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
        }

        @Override
        public void run() {
            statusCode = Status.WAITING;
            /*
             * TODO: explain this magic number! Why add 100?
             * Reference: https://unicode-org.atlassian.net/browse/CLDR-15369
             */
            final CLDRProgressTask progress =
                    CookieSession.sm.openProgress("vv: Priority Items Summary", maxn + 100);

            if (DEBUG) {
                System.out.println(
                        "Starting up vv task: Priority Items Summary, " + taskDescription());
            }

            try {
                status = "Waiting...";
                progress.update("Waiting...");
                if (DEBUG) {
                    System.out.println("Calling OnlyOneVetter.acquire(), " + taskDescription());
                }
                OnlyOneVetter.acquire();
                if (DEBUG) {
                    System.out.println("Did call OnlyOneVetter.acquire(), " + taskDescription());
                }
                try {
                    if (stop) {
                        // this NEVER happens?
                        if (DEBUG) {
                            System.out.println(
                                    "VettingViewerQueue.Task.run -- stopping, "
                                            + taskDescription());
                        }
                        status = "Stopped on request.";
                        statusCode = Status.STOPPED;
                        return;
                    }
                    processCriticalWork(progress);
                } finally {
                    // this happens sometimes six minutes after pressing Stop button
                    if (DEBUG) {
                        System.out.println("Calling OnlyOneVetter.release(), " + taskDescription());
                    }
                    OnlyOneVetter.release();
                }
                status = "Finished.";
                statusCode = Status.READY;
            } catch (RuntimeException | InterruptedException | ExecutionException re) {
                SurveyLog.logException(
                        logger,
                        re,
                        "While VettingViewer processing Priority Items Summary, "
                                + taskDescription());
                status = "Exception! " + re + ", " + taskDescription();
                // We're done.
                statusCode = Status.STOPPED;
            } finally {
                // don't change the status
                if (progress != null) {
                    progress.close();
                }
            }
        }

        private String taskDescription() {
            return "thread " + myThread.getId() + ", " + LocalTime.now();
        }

        private void processCriticalWork(final CLDRProgressTask progress)
                throws ExecutionException {
            status = "Beginning Process, Calculating";
            VettingViewer<Organization> vv;
            vv =
                    new VettingViewer<>(
                            sm.getSupplementalDataInfo(), sm.getSTFactory(), new STUsersChoice(sm));
            vv.setSummarizeAllLocales(summarizeAllLocales);
            int localeCount = vv.getLocaleCount(usersOrg);
            int pathCount = getMax(sm.getEnglishFile());
            maxn = localeCount * pathCount;
            progress.update("Got VettingViewer");
            statusCode = Status.PROCESSING;
            start = System.currentTimeMillis();
            last = start;
            n = 0;
            vv.setProgressCallback(new CLDRProgressCallback(progress, Thread.currentThread()));

            EnumSet<NotificationCategory> choiceSet =
                    VettingViewer.getPriorityItemsSummaryCategories(usersOrg);
            if (DEBUG) {
                System.out.println(
                        "Starting generation of Priority Items Summary, " + taskDescription());
            }
            vv.setLocaleBaselineCount(new VVQueueLocaleBaselineCount());
            vv.generatePriorityItemsSummary(aBuffer, choiceSet, usersOrg);
            if (myThread.isAlive()) {
                if (DEBUG) {
                    System.out.println(
                            "Finished generation of Priority Items Summary, " + taskDescription());
                }
                aBuffer.append("<hr/>Processing time: " + ElapsedTimer.elapsedTime(start));
                entry.output.put(usersOrg, new VVOutput(aBuffer));
            } else {
                if (DEBUG) {
                    System.out.println(
                            "Stopped generation of Priority Items Summary (thread is dead), "
                                    + taskDescription());
                }
            }
        }

        private int getPercent() {
            if (n <= 0 || maxn <= 0) {
                return 0;
            } else if (n >= maxn) {
                return 100;
            } else {
                int p = (n * 100) / maxn;
                return (p > 0) ? p : 1;
            }
        }
    }

    /** Arguments for getPriorityItemsSummaryOutput */
    public class Args {
        private final QueueMemberId qmi;
        private final Organization usersOrg;
        private final LoadingPolicy loadingPolicy;

        public Args(QueueMemberId qmi, Organization usersOrg, LoadingPolicy loadingPolicy) {
            this.qmi = qmi;
            this.usersOrg = usersOrg;
            this.loadingPolicy = loadingPolicy;
        }
    }

    /**
     * Results for getPriorityItemsSummaryOutput
     *
     * <p>These fields get filled in by getPriorityItemsSummaryOutput, and referenced by the caller
     * after getPriorityItemsSummaryOutput returns
     */
    public class Results {
        public Status status = Status.STOPPED;
        public Appendable output = new StringBuilder();
    }

    /*
     * Messages returned by getPriorityItemsSummaryOutput
     */
    private static final String SUM_MESSAGE_COMPLETE = "Completed successfully";
    private static final String SUM_MESSAGE_STOPPED_ON_REQUEST = "Stopped on request";
    private static final String SUM_MESSAGE_PROGRESS = "In Progress";
    private static final String SUM_MESSAGE_STOPPED_STUCK = "Stopped (refresh if stuck)";
    private static final String SUM_MESSAGE_NOT_LOADING = "Not loading. Click the button to load.";
    private static final String SUM_MESSAGE_STARTED = "Started new task";

    /**
     * Return the status of the vetting viewer output request
     *
     * @param args the VettingViewerQueue.Args
     * @param results the VettingViewerQueue.Results
     * @return the status message, or null
     * @throws IOException
     * @throws JSONException
     */
    public synchronized String getPriorityItemsSummaryOutput(
            VettingViewerQueue.Args args, VettingViewerQueue.Results results)
            throws IOException, JSONException {
        JSONObject debugStatus = DEBUG ? new JSONObject() : null;
        QueueEntry entry = getEntry(args.qmi);
        Task t = entry.currentTask;
        if (args.loadingPolicy != LoadingPolicy.FORCESTOP) {
            VVOutput res = entry.output.get(args.usersOrg);
            if (res != null) {
                setPercent(100);
                results.status = Status.READY;
                results.output.append(res.output);
                if (DEBUG) {
                    final String desc = (t == null) ? "[null task]" : t.taskDescription();
                    System.out.println(
                            "Got result, calling stop for Priority Items Summary, " + desc);
                }
                stop(entry);
                entry.output.remove(args.usersOrg);
                return SUM_MESSAGE_COMPLETE;
            }
        } else {
            /* force stop */
            if (DEBUG) {
                final String desc = (t == null) ? "[null task]" : t.taskDescription();
                System.out.println("Forced stop of Priority Items Summary, " + desc);
            }
            stop(entry);
            entry.output.remove(args.usersOrg);
            results.status = Status.STOPPED;
            if (debugStatus != null) {
                debugStatus.put("t_running", false);
                debugStatus.put("t_statuscode", Status.STOPPED);
                debugStatus.put("t_status", SUM_MESSAGE_STOPPED_ON_REQUEST);
            }
            return SUM_MESSAGE_STOPPED_ON_REQUEST;
        }
        if (t != null) {
            String waiting = waitingString();
            if (debugStatus != null) {
                putTaskStatus(debugStatus, t);
            }
            results.status = Status.PROCESSING;
            if (t.myThread.isAlive()) {
                // get progress from current thread
                results.status = t.statusCode;
                if (results.status != Status.WAITING) {
                    waiting = "";
                }
                setPercent(t.getPercent());
                return SUM_MESSAGE_PROGRESS + ": " + waiting + t.status;
            } else {
                setPercent(0);
                return SUM_MESSAGE_STOPPED_STUCK + " " + t.status;
            }
        }
        if (args.loadingPolicy == LoadingPolicy.NOSTART) {
            results.status = Status.STOPPED;
            setPercent(0);
            return SUM_MESSAGE_NOT_LOADING;
        }

        // TODO: May be better to use SurveyThreadManager.getExecutorService().invoke() (rather than
        // a raw thread) but would require
        // some restructuring
        t = entry.currentTask = new Task(entry, args.usersOrg);
        t.myThread = SurveyThreadManager.getThreadFactory().newThread(t);
        if (DEBUG) {
            System.out.println(
                    "Starting new thread for Priority Items Summary, " + t.taskDescription());
        }
        t.myThread.start();

        results.status = Status.PROCESSING;
        if (DEBUG) {
            putTaskStatus(debugStatus, t);
        }
        setPercent(0);
        final String waitStr = waitingString();
        if (WAITING_IN_LINE_MESSAGE.equals(t.status) && waitStr.isEmpty()) {
            // Simplify “Started new task: Waiting in line” to "Waiting in line"
            return WAITING_IN_LINE_MESSAGE;
        }
        return SUM_MESSAGE_STARTED + ": " + waitStr + t.status;
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
                if (DEBUG) {
                    System.out.println(
                            "Alive; stop() setting stop = true for Priority Items Summary, "
                                    + t.taskDescription());
                }
                t.stop = true;
                t.myThread.interrupt();
                if (DEBUG) {
                    System.out.println(
                            "Alive; called interrupt() for Priority Items Summary, "
                                    + t.taskDescription());
                }
            } else if (DEBUG) {
                System.out.println(
                        "Not alive or already stopped for Priority Items Summary, "
                                + t.taskDescription());
            }
            entry.currentTask = null;
        } else if (DEBUG) {
            System.out.println("Task was null in stop() for Priority Items Summary");
        }
    }

    private QueueEntry getEntry(QueueMemberId qmi) {
        QueueEntry entry = (QueueEntry) qmi.get(KEY);
        if (entry == null) {
            entry = new QueueEntry();
            qmi.put(KEY, entry);
        }
        return entry;
    }

    private static int totalUsersWaiting() {
        return (OnlyOneVetter.getQueueLength());
    }

    private int percent = 0;

    private void setPercent(int p) {
        percent = p;
    }

    public int getPercent() {
        return percent;
    }

    private static class VVQueueLocaleBaselineCount implements VettingViewer.LocaleBaselineCount {

        public int getBaselineProblemCount(CLDRLocale cldrLocale) throws ExecutionException {
            return LocaleCompletion.getBaselineCount(cldrLocale);
        }
    }

    private boolean summarizeAllLocales = false;

    public void setSummarizeAllLocales(boolean b) {
        summarizeAllLocales = b;
    }
}
