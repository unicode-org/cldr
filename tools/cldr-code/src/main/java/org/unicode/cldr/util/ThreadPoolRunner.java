package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * basically implements something like ForkJoinPool but using ThreadFactory.
 * Why? Because “ForkJoinPool does not work under J2EE”.
 * Source: https://stackoverflow.com/a/20859857/185799
 */
public abstract class ThreadPoolRunner<T extends ThreadPoolRunner.TPRWriteContext> extends RecursiveAction {
    private static final Logger logger = Logger.getLogger(ThreadPoolRunner.class.getSimpleName());

    protected final int length;
    protected final int start;
    protected final T context;

    /**
     * Base class for WriteContexts used with this class.
     * Encapsulates the parallelism setting.
     */
    public static abstract class TPRWriteContext {
        private int configParallel;
        private boolean stopped = false;

        public void stop() {
            stopped = true;
            logger.info(toString() + " - stopped");
        }

        public boolean isStopped() {
            return stopped;
        }

        public TPRWriteContext() {
            // setup env
            CLDRConfig config = CLDRConfig.getInstance();

            // parallelism. 0 means "let Java decide"
            configParallel = Math.max(config.getProperty("CLDR_THREADPOOLRUNNER_PARALLEL", 0), 0);
            if (configParallel < 1) {
                configParallel = java.lang.Runtime.getRuntime().availableProcessors(); // matches ForkJoinPool() behavior
            }
            logger.info("CLDR_THREADPOOLRUNNER_PARALLEL=" + configParallel);
        }

        public int getConfigParallel() {
            return configParallel;
        }

        /**
         * @return number of elements in this context
         */
        public abstract int size();

        /**
         * Default is each item is a chunk.
         * @return
         */
        public int getChunkSize() {
            return 1;
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + start + "+" + length + ")";
    }

    /**
     * Clone this runner
     * @param start
     * @param length
     * @return
     */
    public abstract ThreadPoolRunner<T> clone(int start, int length);

    /**
     * Constructor for the entirety of the context
     * @param context
     */
    public ThreadPoolRunner(T context) {
        this(context, 0, context.size());
    }

    public ThreadPoolRunner(T context, int start, int length) {
        this.context = context;
        this.start = start;
        this.length = length;
        logger.finer(() -> toString() + " of " + context.size());
    }

    /**
     * This is the ForkJoinPool override.
     * This implementation allows us to be FJP callable
     */
    @Override
    protected void compute() {
        if (length == 0) {
            return;
        } else if (length <= context.getChunkSize()) {
            computeAll();
        } else {
            int split = length / 2;
            // subdivide
            invokeAll(clone(start, split),
                clone(start + split, length - split));
        }
    }

    protected void compute(ThreadFactory inFactory) throws InterruptedException {
        if (length == 0) {
            return;
        } else if (length <= context.getChunkSize()) {
            // below this threshhold, just call directly
            computeAll();
        } else {
            ArrayList<Thread> threads = new ArrayList<>(context.getConfigParallel());
            int chunkEach = Math.max(length / context.getConfigParallel(), context.getChunkSize());
            int threadCount = length / chunkEach;
            if (threadCount > context.getConfigParallel()) {
                threadCount = context.getConfigParallel(); // pin the thread count
            }
            for (int i=start; i<start+length; ) {
                int remain = (start+length) - i;
                int chunk = chunkEach;
                if (chunk > remain) {
                    chunk = remain; // last
                }
                final ThreadPoolRunner<T> a = clone(i, chunk);
                Thread t = inFactory.newThread(new Runnable() {
                    @Override
                    public void run() {
                        System.err.println("START: " + a);
                        a.computeAll();
                        System.err.println("DONE: " + a);
                    }
                });
                t.start();
                threads.add(t);
                i += chunk;
            }
            System.err.println("VettingViewer: spun up " + threads.size() + " thread(s).");
            // now wait for the threads to finish
            if (logger.isLoggable(Level.FINE)) {
                int liveCount = threads.size();
                while(!context.isStopped() && liveCount > 0) {
                    liveCount = 0;
                    for(final Thread t : threads) {
                        if (t.isAlive()) {
                            liveCount++;
                        }
                    }
                    logger.fine("THREAD COUNT: " + liveCount+"/"+threads.size() + " alive");
                    if(liveCount > 0) {
                        try {
                            Thread.sleep(1000);
                        } catch(InterruptedException e) {

                        }
                    }
                }
            }
            // just wait for all threads to finish
            for (Thread t : threads) {
                t.join();
            }
            logger.fine("All threads done.");
        }
    }

    /**
     * Compute this entire task.
     * Can call this to run this step as a single thread.
     */
    public void computeAll() {
        logger.fine(() -> "ComputeAll: " + start + ".." + (start + length));
        // do this many at once
        for (int n = start; n < (start + length); n++) {
            computeOne(n);
        }
    }

    /**
     * Compute one element. This is the worker function.
     * @param n
     */
    protected abstract void computeOne(int n);
}
