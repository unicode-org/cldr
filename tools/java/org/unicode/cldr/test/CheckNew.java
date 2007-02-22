package org.unicode.cldr.test;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CheckNew extends CheckCLDR {
    // this list should be updated with each release.
    static final Matcher stuffToCheckFor = Pattern.compile(".*/(" 
    		// segmentations|preferenceOrdering|singleCountries|currencySpacing|abbreviationFallback|
            + "inList"
            // + "|availableFormats"
            + "|singleCountries|hourFormat|hoursFormat|gmtFormat|regionFormat|fallbackFormat"
            + "|relative"
            + "|calendars.*/fields"
            //+ "|exemplarCharacters\\[.*auxiliary"
            + ").*").matcher("");
    //  dateTimes/availableDateFormats/NEW
    // //ldml/dates/calendars/calendar[@type="gregorian"]/fields/field[@type="second"]/displayName

    public CheckCLDR handleCheck(String path, String fullPath, String value, Map<String, String> options, List<CheckStatus> result) {
        if (stuffToCheckFor.reset(path).matches()) {
          for (CheckStatus resultItem : result) {
            if (resultItem.getCause().getClass() == CheckCoverage.class) {
              return this;
            }
          }
            result.add(new CheckStatus()
                    .setCause(this).setType(CheckStatus.warningType)
                    .setMessage("Relatively new field: may need translation or fixing."));
        }
        return this;
    }
}