package org.unicode.cldr.test;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CheckNew extends CheckCLDR {
    // this list should be updated with each release.
    static final Matcher stuffToCheckFor = Pattern.compile(".*/(" 
            + "segmentations|measurementSystemNames|inList|quarters|availableFormats"
            + "|appendItems|singleCountries|hourFormat|hoursFormat|gmtFormat|regionFormat|fallbackFormat"
            + "|abbreviationFallback|preferenceOrdering|singleCountries|relative|currencySpacing"
            + "|calendars.*/fields"
            + "|exemplarCharacters\\[.*auxiliary"
            + ").*").matcher("");
    //  dateTimes/availableDateFormats/NEW
    // //ldml/dates/calendars/calendar[@type="gregorian"]/fields/field[@type="second"]/displayName

    public CheckCLDR handleCheck(String path, String fullPath, String value, Map options, List result) {
        if (stuffToCheckFor.reset(path).matches()) {
            result.add(new CheckStatus()
                    .setCause(this).setType(CheckStatus.warningType)
                    .setMessage("New field: may need translation or fixing."));
        }
        return this;
    }
}