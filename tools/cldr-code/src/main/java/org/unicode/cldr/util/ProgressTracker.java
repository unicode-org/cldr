package org.unicode.cldr.util;

import com.ibm.icu.number.IntegerWidth;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.Precision;
import com.ibm.icu.util.NoUnit;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Automatically print progress to the console. To use: - initialize with total amount - call
 * decrement() each time something is done (or started, at least) - close() at the end (or use a try
 * with resources)
 */
public class ProgressTracker implements AutoCloseable, Runnable {
    /** Wdith of the progress bar */
    static final long BAR_WIDTH = 10L;

    /** time to first update */
    static final long FIRST_UPDATE_IN_SECONDS = 5L;

    /** time between update */
    static final long UPDATE_IN_SECONDS = 30L;

    final int total;
    final AtomicInteger remaining;
    final String taskName;
    final ScheduledExecutorService svc = Executors.newScheduledThreadPool(1);
    final ScheduledFuture<?> future =
            svc.scheduleAtFixedRate(
                    this, FIRST_UPDATE_IN_SECONDS, UPDATE_IN_SECONDS, TimeUnit.SECONDS);

    // Default: output to stdout. Could have a setter to override
    Consumer<String> messageConsumer =
            new Consumer<>() {
                @Override
                public void accept(String t) {
                    System.out.println(t);
                    System.out.flush();
                }
            };

    /**
     * Setup and print initial count
     *
     * @param taskName the string to show before the percent
     * @param total total number of subtasks
     */
    public ProgressTracker(final String taskName, final int total) {
        this.taskName = taskName;
        this.remaining = new AtomicInteger(total);
        this.total = total;
        run(); // initial run
    }

    /** Print the final count */
    @Override
    public void close() throws Exception {
        future.cancel(true);
        svc.shutdownNow();
        remaining.set(0);
        run(); // final run
    }

    final LocalizedNumberFormatter percentFormatter =
            NumberFormatter.withLocale(Locale.ENGLISH)
                    .unit(NoUnit.PERCENT)
                    .integerWidth(IntegerWidth.zeroFillTo(3))
                    .precision(Precision.integer());

    @Override
    public void run() {
        // this is our scheduled runner
        final int currentRemain = remaining.get();
        final int progress = (total - currentRemain);
        final double fractionComplete = (double) progress / (double) total;
        final String percentString = percentFormatter.format(fractionComplete * 100.0).toString();
        final String progressString = progressBar(fractionComplete);
        messageConsumer.accept(
                String.format(
                        "# %s: %s %s %d/%d",
                        taskName, percentString, progressString, progress, total));
    }

    final String progressBar(double fractionComplete) {
        final long complete = Math.min(BAR_WIDTH, (long) (fractionComplete * (double) BAR_WIDTH));
        final long remain = BAR_WIDTH - complete;
        final StringBuilder sb = new StringBuilder((int) BAR_WIDTH + 2).append('[');
        for (long i = 0; i < complete; i++) {
            sb.append('=');
        }
        for (long i = 0; i < remain; i++) {
            sb.append('-');
        }
        sb.append(']');
        return sb.toString();
    }

    /** call this exactly once */
    public final void decrement() {
        remaining.decrementAndGet();
    }
}
