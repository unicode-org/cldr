package org.unicode.cldr.util;

public class LocaleCompletionData {
    final private int error;
    final private int missing;
    final private int provisional;

    public LocaleCompletionData(Counter<VettingViewer.Choice> problemCounter) {
        error = (int) problemCounter.get(VettingViewer.Choice.error);
        missing = (int) problemCounter.get(VettingViewer.Choice.missingCoverage);
        provisional = (int) problemCounter.get(VettingViewer.Choice.notApproved);
    }

    public int errorCount() {
        return error;
    }

    public int missingCount() {
        return missing;
    }

    public int provisionalCount() {
        return provisional;
    }

    public int problemCount() {
        return error + missing + provisional;
    }
}
