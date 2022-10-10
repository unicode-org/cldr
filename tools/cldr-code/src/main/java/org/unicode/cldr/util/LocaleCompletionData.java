package org.unicode.cldr.util;

public class LocaleCompletionData {
    final private int error;
    final private int missing;
    final private int provisional;

    public LocaleCompletionData(Counter<NotificationCategory> problemCounter) {
        error = (int) problemCounter.get(NotificationCategory.error);
        missing = (int) problemCounter.get(NotificationCategory.missingCoverage);
        provisional = (int) problemCounter.get(NotificationCategory.notApproved);
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
