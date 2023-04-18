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
     * A path like "//ldml/dates/timeZoneNames/zone[@type="America/Guadeloupe"]/short/daylight", or
     * null. If it is null, check all paths, otherwise only check this path.
     */
    String specificSinglePath = null;

    public VettingParameters(
            EnumSet<NotificationCategory> choices, CLDRLocale locale, Level coverageLevel) {
        this.choices = choices;
        this.locale = locale;
        this.coverageLevel = coverageLevel;
    }

    public void setFiles(CLDRLocale locale, Factory sourceFactory, Factory baselineFactory) {
        /*
         * sourceFile provides the current winning values, taking into account recent votes.
         * baselineFile provides the "baseline" (a.k.a. "trunk") values, i.e., the values that
         * are in the current XML in the cldr version control repository. The baseline values
         * are generally the last release values plus any changes that have been made by the
         * technical committee by committing directly to version control rather than voting.
         */
        final String localeId = locale.getBaseName();
        final CLDRFile sourceFile = sourceFactory.make(localeId, true /* resolved */);
        final CLDRFile baselineFile = baselineFactory.make(localeId, true);
        setFiles(sourceFile, baselineFile);
    }

    /**
     * Setup to calculate the baseline ('HEAD') error count
     *
     * @param locale the CLDRLocale
     * @param baselineFactory the baseline factory
     */
    public void setFilesForBaseline(CLDRLocale locale, Factory baselineFactory) {
        final String localeId = locale.getBaseName();
        final CLDRFile baselineFile = baselineFactory.make(localeId, true);
        /*
         * sourceFile must be resolved, otherwise VettingViewer.getMissingStatus is liable
         * to get a null value and return MissingStatus.ABSENT where a resolved file
         * could result in a non-null inherited value (such as en_CA inheriting from en)
         * and MissingStatus.PRESENT. Any such inconsistencies interfere with comparing
         * the current and baseline stats.
         */
        final CLDRFile sourceFile = baselineFactory.make(localeId, true /* resolved */);
        setFiles(sourceFile, baselineFile);
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
