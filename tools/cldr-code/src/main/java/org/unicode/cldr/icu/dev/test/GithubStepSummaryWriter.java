package org.unicode.cldr.icu.dev.test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class GithubStepSummaryWriter implements AutoCloseable {
    private static final String GITHUB_STEP_SUMMARY = System.getenv("GITHUB_STEP_SUMMARY");

    private final File stepSummaryFile;
    private final Writer stepSummaryStream;

    /**
     * write to GITHUB_STEP_SUMMARY if it exists. Do nothing if not, or on null input. Adds a
     * trailing newline.
     */
    public synchronized void writeStepSummary(final CharSequence s) {
        if (s != null && stepSummaryStream != null) {
            try {
                stepSummaryStream.append(s).append('\n');
                stepSummaryStream.flush();
            } catch (IOException e) {
            }
        }
    }

    /** write to GITHUB_STEP_SUMMARY if it exists. Do nothing if not, or on null input */
    public void writeStepSummary(Supplier<CharSequence> s) {
        if (stepSummaryStream != null && s != null) {
            // not synchronizedm so we bottleneck on the underlying version
            writeStepSummary(s.get());
        }
    }

    public GithubStepSummaryWriter() {
        if (GITHUB_STEP_SUMMARY != null && !GITHUB_STEP_SUMMARY.isEmpty()) {
            stepSummaryFile = new File(GITHUB_STEP_SUMMARY);
        } else {
            stepSummaryFile = null;
        }
        stepSummaryStream = openStream(stepSummaryFile);
    }

    private Writer openStream(File f) {
        if (f == null || !f.canWrite()) {
            return null;
        }
        try {
            return new FileWriter(f, StandardCharsets.UTF_8, true);
        } catch (IOException e) {
            System.err.println("Could not write to ${GITHUB_STEP_SUMMARY} " + GITHUB_STEP_SUMMARY);
            e.printStackTrace();
            return null;
        }
    }

    @Override
    /**
     * close is somewhat superfluous from a data perpective as we are flushing each time, but we
     * provide it to reclaim resources
     */
    public void close() throws Exception {
        if (stepSummaryStream != null) {
            stepSummaryStream.close();
        }
    }
}
