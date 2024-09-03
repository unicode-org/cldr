package org.unicode.cldr.web;

import java.io.Writer;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.unicode.cldr.util.CLDRLocale;

public class VxmlGenerator {
    public void generate(Set<CLDRLocale> sortSet, Writer out) throws ExecutionException {
        this.sortSet = sortSet;
        try {
            // TODO: get the booleans somewhere, or remove the parameters
            // -- in practice, we want them all true
            // Reference: https://unicode-org.atlassian.net/browse/CLDR-14913
            OutputFileManager.generateVxml(
                    this, out, true, // outputFiles
                    true, // removeEmpty
                    true); // verifyConsistent
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    private Set<CLDRLocale> sortSet = null;

    public Set<CLDRLocale> getSortSet() {
        return sortSet;
    }

    public enum VerificationStatus {
        SUCCESSFUL,
        FAILED,
        INCOMPLETE
    }

    private VerificationStatus verificationStatus = VerificationStatus.INCOMPLETE;

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus status) {
        verificationStatus = status;
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
