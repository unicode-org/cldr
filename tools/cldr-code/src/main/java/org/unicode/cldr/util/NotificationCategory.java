package org.unicode.cldr.util;

public enum NotificationCategory {
    /** There is a console-check error */
    error('E', "Error", "The Survey Tool detected an error in the winning value."),

    /** Given the users' coverage, some items are missing */
    missingCoverage(
            'M',
            "Missing",
            "Your current coverage level requires the item to be present. "
                    + "(During the vetting phase, this is informational: you can’t add new values.)"),

    /** Provisional: there are not enough votes to be approved */
    notApproved(
            'P',
            "Provisional",
            "There are not enough votes for this item to be approved (and used)."),

    /** There is a dispute. */
    hasDispute(
            'D',
            "Disputed",
            "Different organizations are choosing different values. Please review to approve or reach consensus."),

    /** My choice is not the winning item */
    weLost(
            'L',
            "Losing",
            "The value that your organization chose (overall) is either not the winning value, or doesn’t have enough votes to be approved. "
                    + "This might be due to a dispute between members of your organization."),

    /** There is a console-check warning */
    warning('W', "Warning", "The Survey Tool detected a warning about the winning value."),

    /** The English value for the path changed AFTER the current value for the locale. */
    englishChanged(
            'U',
            "English Changed",
            "The English value has changed in CLDR, but the corresponding value for your language has not. "
                    + "Check if any changes are needed in your language."),

    /** The value changed from the baseline */
    changedOldValue(
            'C',
            "Changed",
            "The winning value was altered from the baseline value. (Informational)"),

    /**
     * The inherited (bailey) value changed from the baseline, given that the winning value is
     * inherited
     */
    inheritedChanged(
            'I',
            "Inherited Changed",
            "The winning inherited value was altered from its baseline value. (Informational)"),

    /** You have abstained, or not yet voted for any value */
    abstained('A', "Abstained", "You have abstained, or not yet voted for any value."),

    other('O', "Other", "All other values");

    public final char abbreviation;
    public final String buttonLabel;
    public final String jsonLabel;

    /**
     * This human-readable description is used for Priority Items Summary, which still creates html
     * on the back end. For Dashboard, identical descriptions are on the front end. When Priority
     * Items Summary is modernized to be more like Dashboard, these descriptions on the back end
     * should become unnecessary.
     */
    public final String description;

    NotificationCategory(char abbreviation, String label, String description) {
        this.abbreviation = abbreviation;
        this.jsonLabel = label.replace(' ', '_');
        this.buttonLabel = TransliteratorUtilities.toHTML.transform(label);
        this.description = TransliteratorUtilities.toHTML.transform(description);
    }
}
