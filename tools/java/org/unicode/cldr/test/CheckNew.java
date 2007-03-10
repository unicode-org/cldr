package org.unicode.cldr.test;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.util.CLDRFile;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CheckNew extends CheckCLDR {
  // this list should be updated with each release.
  static final Matcher stuffToCheckFor = Pattern.compile(".*/(" +
      // segmentations|preferenceOrdering|singleCountries|currencySpacing|abbreviationFallback|
      "inList" +
      // + "|availableFormats"
      "|hourFormat" +
      "|gmtFormat" +
      "|regionFormat" +
      "|fallbackFormat" + 
      "|relative" + 
      "|calendars.*/fields" +
      "|languages.*_"
      //+ "|exemplarCharacters\\[.*auxiliary"
      + ").*").matcher("");

  //  dateTimes/availableDateFormats/NEW
  // //ldml/dates/calendars/calendar[@type="gregorian"]/fields/field[@type="second"]/displayName

  static final Matcher shouldntBeRoot = Pattern.compile(".*/(" + "codePatterns" + "fields" + ").*").matcher("");

  private boolean isEnglishOrRoot;
  private boolean isNotBase;

  private CLDRFile root;

  @Override
  public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Map options, List possibleErrors) {
    if (cldrFileToCheck == null)
      return this;
    String locale = cldrFileToCheck.getLocaleID();
    isEnglishOrRoot = locale.startsWith("en_") || locale.equals("en") || locale.equals("root");
    isNotBase = locale.contains("_");
    root = cldrFileToCheck.make("root", true);
    return this;
  }

  public CheckCLDR handleCheck(String path, String fullPath, String value, Map<String, String> options, List<CheckStatus> result) {
    boolean skip = false;

    // now see if our value is the same as Root's for certain items
    if (!isEnglishOrRoot && shouldntBeRoot.reset(path).matches()) {
      if (value.equals(root.getStringValue(path))) {
        skip = true;
        result.add(new CheckStatus().setCause(this).setType(CheckStatus.warningType).setMessage(
            "There may have been a conflict introduced as a result of fixing default contents -- please confirm the right value.", new Object[] {}));
      }
    }
    // We check certain root values that were pushed into children. So the the locale is not a direct decendent of root, and we have a value the same as root's...
    if (!skip && isNotBase && !path.startsWith("//ldml/identity/") && value.equals(root.getStringValue(path))) {
      skip = true;
      result.add(new CheckStatus().setCause(this).setType(CheckStatus.warningType).setMessage(
          "There may have been a conflict introduced as a result of fixing default contents: please confirm the right value.", new Object[] {}));
    }
    
    // flag as new, but only if there aren't other issues
    if (!skip && stuffToCheckFor.reset(path).matches()) {
      // bail if it is already part of coverage
      for (CheckStatus resultItem : result) {
        if (resultItem.getCause().getClass() == CheckCoverage.class) {
          skip = true;
          break;
        }
      }
      if (!skip) {
        result.add(new CheckStatus().setCause(this).setType(CheckStatus.warningType).setMessage("Relatively new field: may need translation or fixing."));
      }
    }

    // We also check 
    return this;
  }
}