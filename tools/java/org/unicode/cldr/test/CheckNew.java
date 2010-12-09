package org.unicode.cldr.test;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile;

public class CheckNew extends CheckCLDR {
  // this list should be updated with each release.

  static final Matcher stuffToAlwaysFlag = Pattern.compile(".*/(" +
          // segmentations|preferenceOrdering|singleCountries|currencySpacing|abbreviationFallback|
          //"inList" +
          // + "|availableFormats"
          //"|hourFormat" +
          //"|gmtFormat" +
          //"|regionFormat" +
          //"|fallbackFormat" + 
          //"|relative" + 
          //"|calendars.*/fields" +
          //"|languages.*_"
          //"currency\\[@type=\"TR[LY]\"\\]/displayName"
          //+ "|script\\[@type=\"Cans\"\\]"
          //+ "|localeDisplayPattern"
          //"|displayName.*\\[@count=\""
          //+ "|exemplarCharacters\\[.*auxiliary"
          "xxxxxx" // need something to avoid always matching
          + ").*").matcher("");

  //  dateTimes/availableDateFormats/NEW
  // //ldml/dates/calendars/calendar[@type="gregorian"]/fields/field[@type="second"]/displayName

  static final Matcher shouldntBeRoot = Pattern.compile(".*/(" + "codePatterns" + "fields" + ").*").matcher("");

  private boolean isEnglishOrRoot;
  private boolean isNotBase;

  private CLDRFile root;

  @Override
  public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Map<String, String> options, List<CheckStatus> possibleErrors) {
    if (cldrFileToCheck == null)
      return this;
    if (Phase.SUBMISSION == getPhase()) {
      setSkipTest(false); // ok
    } else {
      setSkipTest(true);
      return this;
    }

    super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
    String locale = cldrFileToCheck.getLocaleID();
    isEnglishOrRoot = locale.startsWith("en_") || locale.equals("en") || locale.equals("root");
    isNotBase = locale.contains("_");
    root = cldrFileToCheck.make("root", true);
    return this;
  }

  public CheckCLDR handleCheck(String path, String fullPath, String value, Map<String, String> options, List<CheckStatus> result) {
    // if (fullPath == null) return this; // skip paths that we don't have
    // skip if the user voted for the current item
//    if (fullPath != null && getCldrFileToCheck().isWinningPath(fullPath)) {
//      return this;
//    }
    
    boolean skip = false;
    // now see if our value is the same as Root's for certain items
//    if (!isEnglishOrRoot && shouldntBeRoot.reset(path).matches()) {
//      if (value.equals(root.getWinningValue(path))) {
//        skip = true;
//        result.add(new CheckStatus().setCause(this).setType(CheckStatus.warningType).setMessage(
//            "There may have been a conflict introduced as a result of fixing default contents -- please confirm the right value.", new Object[] {}));
//      }
//    }
//    // We check certain root values that were pushed into children. So the the locale is not a direct decendent of root, and we have a value the same as root's...
//    if (!skip && isNotBase && !path.startsWith("//ldml/identity/") && value!=null && value.equals(root.getWinningValue(path))) {
//      skip = true;
//      result.add(new CheckStatus().setCause(this).setType(CheckStatus.warningType).setMessage(
//          "There may have been a conflict introduced as a result of fixing default contents: please confirm the right value.", new Object[] {}));
//    }
    
    if (skip) return this;
    
    // flag as new, but only if there aren't other issues
    
    if (stuffToAlwaysFlag.reset(path).matches()) {
      // bail if it is already part of coverage
      if (hasCoverageError(result)) return this;
      
      result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.warningType)
              .setSubtype(Subtype.modifiedEnglishValue)
              .setMessage("Modified English value: may need translation or fixing."));
    }

    return this;
  }

  private boolean hasCoverageError(List<CheckStatus> result) {
    for (CheckStatus resultItem : result) {
      if (resultItem.getCause().getClass() == CheckCoverage.class) {
        return true;
      }
    }
    return false;
  }
}
