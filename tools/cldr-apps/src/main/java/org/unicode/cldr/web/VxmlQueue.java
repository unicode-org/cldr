package org.unicode.cldr.web;

import java.io.File;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;
import org.unicode.cldr.util.*;

public class VxmlQueue {
    private static final Logger logger = SurveyLog.forClass(VxmlQueue.class);

    private static final String WAITING_IN_LINE_MESSAGE = "Waiting in line";
    private static final String VXML_MESSAGE_STOPPED_ON_REQUEST = "Stopped on request";
    private static final String VXML_MESSAGE_STARTED = "Started new task";
    private static final String VXML_MESSAGE_PROGRESS = "In Progress";
    private static final String VXML_MESSAGE_STOPPED_STUCK = "Stopped (refresh if stuck)";
    private static final String VXML_MESSAGE_NOT_LOADING = "Not loading";
    private static final String VXML_MESSAGE_SUCCESS = "Successful completion";
    private static final String VXML_MESSAGE_PROBLEM = "Problem(s) occurred";

    private static final class VxmlQueueHelper {
        static VxmlQueue instance = new VxmlQueue();
    }

    /**
     * Get the singleton instance of the queue
     *
     * @return the singleton instance
     */
    public static VxmlQueue getInstance() {
        return VxmlQueueHelper.instance;
    }

    /** A unique key for storing QueueEntry objects in QueueMemberId objects */
    private static final String KEY = VxmlQueue.class.getName();

    public enum Status {
        /** Waiting on other users/tasks */
        WAITING,
        /** Processing in progress */
        PROCESSING,
        /** Contents are available and successfully validated */
        SUCCEEDED,
        /** Stopped, due to error, validation failure, or cancellation */
        STOPPED,
    }

    public enum RequestType {
        /** Start a new VXML generation task */
        START,
        /** Continue the task already in progress and get its status */
        CONTINUE,
        /** Cancel (stop) the task */
        CANCEL
    }

    private static class QueueEntry {
        public Results results = new Results();
        private Task currentTask = null;
        private Boolean done = false;

        void reset() {
            results = new Results();
            currentTask = null;
            done = false;
        }
    }

    /** Semaphore to ensure that only one vetter generates VXML at a time. */
    private static final Semaphore OnlyOneVetter = new Semaphore(1);

    public static class Task implements Runnable {

        /**
         * Construct a Runnable object specifically for VXML
         *
         * @param entry the QueueEntry
         */
        private Task(QueueEntry entry) {
            this.entry = entry;
        }

        public final class VxmlProgressCallback extends VxmlGenerator.ProgressCallback {
            private final Thread thread;

            private VxmlProgressCallback(Thread thread) {
                this.thread = thread;
            }

            public void nudge(CLDRLocale loc) {
                if (!myThread.isAlive()) {
                    throw new RuntimeException("The thread is not running. Stop now.");
                }
                setLocale(loc);
                setStatusMessage("Writing VXML files");
                n++;
            }

            public boolean isStopped() {
                // if the calling thread is gone, stop processing
                return stop || !(thread.isAlive());
            }
        }

        private Thread myThread = null;
        private boolean stop = false;
        private final QueueEntry entry;
        private int maxn;
        private int n = 0;
        private Status statusCode = Status.WAITING;
        private String statusMessage = WAITING_IN_LINE_MESSAGE;

        private void setStatusMessage(String status) {
            this.statusMessage = status;
        }

        private CLDRLocale loc;

        public CLDRLocale getLocale() {
            return loc;
        }

        private void setLocale(CLDRLocale loc) {
            this.loc = loc;
        }

        @Override
        public void run() {
            statusCode = Status.WAITING;
            try {
                setStatusMessage("Waiting...");
                OnlyOneVetter.acquire();
                try {
                    makeVxml();
                } finally {
                    OnlyOneVetter.release();
                }
                if (stop) {
                    setStatusMessage("Stopped on request.");
                    statusCode = Status.STOPPED;
                } else {
                    setStatusMessage("Finished.");
                    statusCode = Status.SUCCEEDED;
                }
            } catch (RuntimeException | InterruptedException | ExecutionException re) {
                SurveyLog.logException(logger, re, "While generating VXML, " + taskDescription());
                setStatusMessage("Exception! " + re + ", " + taskDescription());
                statusCode = Status.STOPPED;
            }
        }

        private void makeVxml() throws ExecutionException {
            setStatusMessage("Beginning");
            statusCode = Status.PROCESSING;
            VxmlGenerator vg = new VxmlGenerator();
            n = 0;
            maxn = vg.getLocales().size();
            vg.setProgressCallback(new VxmlProgressCallback(Thread.currentThread()));
            vg.generate(entry.results);
            entry.done = true;
        }

        private String taskDescription() {
            return "thread " + myThread.getId() + ", " + LocalTime.now();
        }

        private int getPercent() {
            return CompletionPercent.calculate(n, maxn);
        }
    }

    public static class Args {
        private final QueueMemberId qmi;
        private final RequestType requestType;

        public Args(QueueMemberId qmi, RequestType requestType) {
            this.qmi = qmi;
            this.requestType = requestType;
        }
    }

    public static class Results {
        public Status status = Status.WAITING;

        public String generationMessage;

        public File directory;

        private int percent = 0;

        public Number getPercent() {
            return this.percent;
        }

        public void setPercent(int i) {
            this.percent = i;
        }

        private CLDRLocale locale;

        public CLDRLocale getLocale() {
            return locale;
        }

        public void setLocale(CLDRLocale locale) {
            this.locale = locale;
        }

        private int localesDone = 0;

        public int getLocalesDone() {
            return localesDone;
        }

        public void setLocalesDone(int n) {
            localesDone = n;
        }

        private int localesTotal = 0;

        public int getLocalesTotal() {
            return localesTotal;
        }

        public void setLocalesTotal(int n) {
            localesTotal = n;
        }

        public VxmlGenerator.VerificationStatus verificationStatus =
                VxmlGenerator.VerificationStatus.INCOMPLETE;

        public void setVerificationStatus(VxmlGenerator.VerificationStatus vStatus) {
            this.verificationStatus = vStatus;
        }

        public ArrayList<String> verificationFailures = new ArrayList<>();

        public ArrayList<String> verificationWarnings = new ArrayList<>();

        public void addVerificationFailure(String s) {
            verificationFailures.add(s);
        }

        public void addVerificationWarning(String s) {
            verificationWarnings.add(s);
        }
    }

    /**
     * Start running, or continue running, the long-running task that generates VXML. This is called
     * once for each request, including the initial request that starts VXML generation, and
     * subsequent requests that query the status or trigger early termination of the task.
     *
     * @param args the VxmlQueue.Args
     * @return the Results
     */
    public synchronized Results getResults(Args args) {
        QueueEntry entry = getEntry(args.qmi);
        if (args.requestType == RequestType.START) {
            getResultsStart(entry);
        } else if (args.requestType == RequestType.CONTINUE) {
            getResultsContinue(entry);
        } else if (args.requestType == RequestType.CANCEL) {
            getResultsStop(entry);
        } else {
            logger.severe("Invalid request type for VXML, treating as STOP: " + args.requestType);
            getResultsStop(entry);
        }
        return entry.results;
    }

    private void getResultsStart(QueueEntry entry) {
        entry.reset();
        // Note: similar code in VettingViewerQueue.getPriorityItemsSummaryOutput includes
        // a comment suggesting an alternative: SurveyThreadManager.getExecutorService().invoke()
        Task t = entry.currentTask = new Task(entry);
        t.myThread = SurveyThreadManager.getThreadFactory().newThread(t);
        t.myThread.start();
        entry.results.status = Status.PROCESSING;
        final String waitStr = waitingString();
        if (WAITING_IN_LINE_MESSAGE.equals(t.statusMessage) && waitStr.isEmpty()) {
            // Simplify “Started new task: Waiting in line” to "Waiting in line"
            entry.results.generationMessage = WAITING_IN_LINE_MESSAGE;
        } else {
            entry.results.generationMessage =
                    VXML_MESSAGE_STARTED + ": " + waitStr + t.statusMessage;
        }
    }

    private void getResultsContinue(QueueEntry entry) {
        Task t = entry.currentTask;
        if (t != null) {
            entry.results.setLocale(t.getLocale());
            entry.results.setLocalesDone(t.n);
            entry.results.setLocalesTotal(t.maxn);
            entry.results.setPercent(t.getPercent());
        }
        if (entry.done) {
            if (entry.results.verificationStatus == VxmlGenerator.VerificationStatus.SUCCESSFUL) {
                entry.results.status = Status.SUCCEEDED;
                entry.results.generationMessage = VXML_MESSAGE_SUCCESS;
            } else {
                entry.results.status = Status.STOPPED;
                entry.results.generationMessage = VXML_MESSAGE_PROBLEM;
            }
            stop(entry);
        } else if (t == null) {
            entry.results.status = Status.STOPPED;
            entry.results.generationMessage = VXML_MESSAGE_NOT_LOADING;
        } else {
            String waiting = waitingString();
            entry.results.status = Status.PROCESSING;
            if (t.myThread.isAlive()) {
                entry.results.status = t.statusCode;
                if (entry.results.status != Status.WAITING) {
                    waiting = "";
                }
                entry.results.generationMessage =
                        VXML_MESSAGE_PROGRESS + ": " + waiting + t.statusMessage;
            } else {
                entry.results.generationMessage =
                        VXML_MESSAGE_STOPPED_STUCK + " " + t.statusMessage;
            }
        }
    }

    private void getResultsStop(QueueEntry entry) {
        stop(entry);
        entry.results.status = Status.STOPPED;
        entry.results.generationMessage = VXML_MESSAGE_STOPPED_ON_REQUEST;
    }

    private String waitingString() {
        int aheadOfMe = OnlyOneVetter.getQueueLength();
        return (aheadOfMe > 0) ? (aheadOfMe + " users waiting - ") : "";
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

    private QueueEntry getEntry(QueueMemberId qmi) {
        QueueEntry entry = (QueueEntry) qmi.get(KEY);
        if (entry == null) {
            entry = new QueueEntry();
            qmi.put(KEY, entry);
        }
        return entry;
    }
}
