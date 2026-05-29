package org.unicode.cldr.util;

public class LocaleCompletionData {
    private final int error;
    private final int missing;
    private final int provisional;
    private final int newSinceLastVote;

    public LocaleCompletionData(Counter<NotificationCategory> problemCounter) {
        error = (int) problemCounter.get(NotificationCategory.error);
        missing = (int) problemCounter.get(NotificationCategory.missingCoverage);
        provisional = (int) problemCounter.get(NotificationCategory.notApproved);
        newSinceLastVote = (int) problemCounter.get(NotificationCategory.newSinceLastVote);
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

    public int newCount() {
        return newSinceLastVote;
    }
}
