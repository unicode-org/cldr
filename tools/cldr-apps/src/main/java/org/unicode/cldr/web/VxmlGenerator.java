package org.unicode.cldr.web;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.unicode.cldr.util.CLDRLocale;

public class VxmlGenerator {

    private final Set<CLDRLocale> locales;

    public VxmlGenerator() {
        this.locales = OutputFileManager.createVxmlLocaleSet();
    }

    public void generate(VxmlQueue.Results results) throws ExecutionException {
        try {
            OutputFileManager.generateVxml(this, results);
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    public Set<CLDRLocale> getLocales() {
        return locales;
    }

    public enum VerificationStatus {
        SUCCESSFUL,
        FAILED,
        INCOMPLETE
    }

    /**
     * Class that allows the relaying of progress information. Extended by
     * VxmlQueue.Task.VxmlProgressCallback
     */
    public static class ProgressCallback {
        public void nudge(CLDRLocale loc) {}

        public boolean isStopped() {
            return false;
        }
    }

    private ProgressCallback progressCallback;

    public void setProgressCallback(ProgressCallback newCallback) {
        progressCallback = newCallback;
    }

    public void update(CLDRLocale loc) {
        if (progressCallback.isStopped()) {
            throw new RuntimeException("Requested to stop");
        }
        progressCallback.nudge(loc);
    }
}
