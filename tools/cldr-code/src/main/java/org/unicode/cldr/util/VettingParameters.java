package org.unicode.cldr.util;

import java.util.EnumSet;

public class VettingParameters {
    final EnumSet<NotificationCategory> choices;
    final CLDRLocale locale;
    final Level coverageLevel;
    int userId = 0;
    Organization organization = null;
    CLDRFile sourceFile = null;
    CLDRFile baselineFile = null;

    /**
     * A path like "//ldml/dates/timeZoneNames/zone[@type="America/Guadeloupe"]/short/daylight",
     * or null. If it is null, check all paths, otherwise only check this path.
     */
    String specificSinglePath = null;

    public VettingParameters(EnumSet<NotificationCategory> choices, CLDRLocale locale, Level coverageLevel) {
        this.choices = choices;
        this.locale = locale;
        this.coverageLevel = coverageLevel;
    }

    public void setFiles(CLDRFile sourceFile, CLDRFile baselineFile) {
        this.sourceFile = sourceFile;
        this.baselineFile = baselineFile;
    }

    public void setXpath(String xpath) {
        this.specificSinglePath = xpath;
    }

    public void setUserAndOrganization(int userId, Organization organization) {
        this.userId = userId;
        this.organization = organization;
    }

    public CLDRFile getBaselineFile() {
        return baselineFile;
    }

    public CLDRFile getSourceFile() {
        return sourceFile;
    }

    public int getUserId() {
        return userId;
    }

    public CLDRLocale getLocale() {
        return locale;
    }

    public boolean isOnlyForSinglePath() {
        return specificSinglePath != null;
    }
}
