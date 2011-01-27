/*
 ******************************************************************************
 * Copyright (C) 2005-2008, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package org.unicode.cldr.test;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.InternalCldrException;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.CLDRFile.Status;

import com.ibm.icu.dev.test.util.PrettyPrinter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class CheckForExemplars extends CheckCLDR {
  //private final UnicodeSet commonAndInherited = new UnicodeSet(CheckExemplars.Allowed).complement(); 
  // "[[:script=common:][:script=inherited:][:alphabetic=false:]]");
  static String[] EXEMPLAR_SKIPS = {"/currencySpacing", "/hourFormat", "/exemplarCharacters", "/pattern",
    "/localizedPatternChars", "/segmentations", "/dateFormatItem", "/references", "/unitPattern",
    "/intervalFormatItem",
    "/localeDisplayNames/variants/",
    "/commonlyUsed"
  };
  
  private UnicodeSet exemplars;
  private UnicodeSet scriptRegionExemplars;
  private UnicodeSet scriptRegionExemplarsWithParens;
  private UnicodeSet currencySymbolExemplars;
  private boolean skip;
  private Collator col;
  private Collator spaceCol;
  private String informationMessage;
  private Status otherPathStatus = new Status();
  private Matcher patternMatcher = ExampleGenerator.PARAMETER.matcher("");
  static final Pattern SUPPOSED_TO_BE_MESSAGE_FORMAT_PATTERN = Pattern.compile("/(" +
          "codePattern" +
          "|dateRangePattern" +
          "|dateTimeFormat[^/]*?/pattern" +
          "|appendItem" +
          "|intervalFormatFallback" +
          "|hoursFormat" +
          "|gmtFormat" +
          "|regionFormat" +
          "|fallbackFormat" +
          "|unitPattern" +
          "|localePattern" +
          "|listPatternPart" +
  ")");
  private Matcher supposedToBeMessageFormat = SUPPOSED_TO_BE_MESSAGE_FORMAT_PATTERN.matcher("");

  static final Pattern LEAD_OR_TRAIL_WHITESPACE_OK = Pattern.compile("/(" +
          "localeSeparator" +
          "|references/reference" +
          "|insertBetween" +
  ")");
  private Matcher leadOrTrailWhitespaceOk = LEAD_OR_TRAIL_WHITESPACE_OK.matcher("");
  
  private static UnicodeSet ASCII = (UnicodeSet) new UnicodeSet("[A-Z]").freeze();

  static final Pattern IS_COUNT_ZERO_ONE_TWO = Pattern.compile("/units.*\\[@count=\"(zero|one|two)\"");
  private Matcher isCountZeroOneTwo = IS_COUNT_ZERO_ONE_TWO.matcher("");
  private boolean hasSpecialPlurals;

  public CheckCLDR setCldrFileToCheck(CLDRFile cldrFile, Map<String, String> options, List<CheckStatus> possibleErrors) {
    if (cldrFile == null) return this;
    skip = true;
    super.setCldrFileToCheck(cldrFile, options, possibleErrors);
    if (cldrFile.getLocaleID().equals("root")) {
      return this;
    }
    String locale = cldrFile.getLocaleID();
    hasSpecialPlurals = locale.equals("ar") || locale.startsWith("ar_");
    informationMessage = "<a href='http://unicode.org/cldr/apps/survey?_=" + locale + "&x=characters'>characters</a>";
    col = Collator.getInstance(new ULocale(locale));
    spaceCol = Collator.getInstance(new ULocale(locale));
    spaceCol.setStrength(col.PRIMARY);

    CLDRFile resolvedFile = cldrFile.getResolved();
    boolean[] ok = new boolean[1];
    exemplars = safeGetExemplars("", possibleErrors, resolvedFile, ok);
    if (!ok[0]) exemplars = new UnicodeSet();

    if (exemplars == null) {
      CheckStatus item = new CheckStatus().setCause(this).setMainType(CheckStatus.errorType).setSubtype(Subtype.noExemplarCharacters)
      .setMessage("No Exemplar Characters: {0}", new Object[]{this.getClass().getName()});
      possibleErrors.add(item);
      return this;
    }
    //UnicodeSet temp = resolvedFile.getExemplarSet("standard");
    //if (temp != null) exemplars.addAll(temp);
    UnicodeSet auxiliary = safeGetExemplars("auxiliary", possibleErrors, resolvedFile, ok); // resolvedFile.getExemplarSet("auxiliary", CLDRFile.WinningChoice.WINNING);
    if (auxiliary != null) exemplars.addAll(auxiliary);
    exemplars.addAll(CheckExemplars.AlwaysOK).freeze();

    scriptRegionExemplars = (UnicodeSet) new UnicodeSet(exemplars).removeAll("();,").freeze();
    scriptRegionExemplarsWithParens = (UnicodeSet) new UnicodeSet(exemplars).removeAll(";,").freeze();

    currencySymbolExemplars = safeGetExemplars("currencySymbol", possibleErrors, resolvedFile, ok); // resolvedFile.getExemplarSet("currencySymbol", CLDRFile.WinningChoice.WINNING);
    if (currencySymbolExemplars == null) {
      currencySymbolExemplars = new UnicodeSet(exemplars);
    } else {
      currencySymbolExemplars.addAll(exemplars);
    }
    skip = false;
    return this;
  }

  private UnicodeSet safeGetExemplars(String type, List possibleErrors, CLDRFile resolvedFile, boolean[] ok) {
    UnicodeSet result = null;
    try {
      result = resolvedFile.getExemplarSet(type, CLDRFile.WinningChoice.WINNING);
      ok[0] = true;
    } catch(IllegalArgumentException iae) {
      possibleErrors.add(new CheckStatus()
      .setCause(this).setMainType(CheckStatus.errorType).setSubtype(Subtype.couldNotAccessExemplars)
      .setMessage("Could not get exemplar set: " + iae.toString()));
      ok[0] = false;
    }
    return result;
  }

  public CheckCLDR handleCheck(String path, String fullPath, String value,
          Map<String, String> options, List<CheckStatus> result) {
    if (fullPath == null) return this; // skip paths that we don't have
    if (value == null) return this; // skip values that we don't have ?
    if (skip) return this;
    if(path == null) { 
      throw new InternalCldrException("Empty path!");
    } else if(getCldrFileToCheck() == null) {
      throw new InternalCldrException("no file to check!");
    }
    String sourceLocale = getResolvedCldrFileToCheck().getSourceLocaleID(path, otherPathStatus);

    // if we are an alias to another path, then skip
    if (!path.equals(otherPathStatus.pathWhereFound)) {
      return this;
    }

    // now check locale source
    if (XMLSource.CODE_FALLBACK_ID.equals(sourceLocale)) {
      return this;
    } else if ("root".equals(sourceLocale)) {
      // skip eras for non-gregorian
      if (true) return this;
      if (path.indexOf("/calendar") >= 0 && path.indexOf("gregorian") <= 0) return this;
    }

    // add checks for patterns. Make sure that all and only the message format patterns have {n}
    boolean hasMessageFormatFields = patternMatcher.reset(value).find();
    boolean supposedToHaveMessageFormatFields = 
      supposedToBeMessageFormat.reset(path).find()
      && !(hasSpecialPlurals 
              && isCountZeroOneTwo.reset(path).find());
    if (hasMessageFormatFields != supposedToHaveMessageFormatFields) {
      result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.errorType)
              .setSubtype(supposedToHaveMessageFormatFields ? Subtype.missingPlaceholders : Subtype.shouldntHavePlaceholders)
              .setMessage(supposedToHaveMessageFormatFields 
                      ? "This field is a message pattern, and should have '{0}, {1},' etc. See the English for an example."
                              : "This field is not a message pattern, and should not have '{0}, {1},' etc. See the English for an example.",
                              new Object[]{}));

    }
    if (supposedToHaveMessageFormatFields) {
      // check the other characters in the message format patterns
      value = patternMatcher.replaceAll("#");
    } else {
      // end checks for patterns
      for (int i = 0; i < EXEMPLAR_SKIPS.length; ++i) {
        if (path.indexOf(EXEMPLAR_SKIPS[i]) > 0 ) return this; // skip some items.
      }
    }

    if (path.startsWith("//ldml/posix/messages")) return this;

    if (path.contains("/currency") && path.endsWith("/symbol")) {
      if (!currencySymbolExemplars.containsAll(value)) {
        UnicodeSet missing = new UnicodeSet().addAll(value).removeAll(currencySymbolExemplars);
        String fixedMissing = new PrettyPrinter()
        .setOrdering(col != null ? col : Collator.getInstance(ULocale.ROOT))
        .setSpaceComparator(col != null ? col : Collator.getInstance(ULocale.ROOT)
                .setStrength2(Collator.PRIMARY))
                .setCompressRanges(true)
                .format(missing);
        String ascii = "";
        Subtype subtype = Subtype.charactersNotInCurrencyExemplars;
        if (ASCII.containsAll(missing)) {
          subtype = Subtype.asciiCharactersNotInCurrencyExemplars;
          ascii = "(ASCII) ";
        }

        result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.warningType).setSubtype(subtype)
                .setMessage("The characters \u200E{0}\u200E " + ascii + "are not used in currency symbols in this language, according to " + informationMessage + ".", new Object[]{fixedMissing}));
      }
    } else if (!exemplars.containsAll(value)) {
      UnicodeSet missing = new UnicodeSet().addAll(value).removeAll(exemplars);
      String fixedMissing = new PrettyPrinter()
    .setOrdering(col != null ? col : Collator.getInstance(ULocale.ROOT))
    .setSpaceComparator(col != null ? col : Collator.getInstance(ULocale.ROOT)
            .setStrength2(Collator.PRIMARY))
            .setCompressRanges(true).format(missing);
      String ascii = "";
      Subtype subtype = Subtype.charactersNotInMainOrAuxiliaryExemplars;
      if (ASCII.containsAll(missing)) {
        subtype = Subtype.asciiCharactersNotInMainOrAuxiliaryExemplars;
        ascii = "(ASCII) ";
      }

      result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.warningType).setSubtype(subtype)
              .setMessage("The characters \u200E{0}\u200E " + ascii + "are not used in this language, according to " + informationMessage + ".", new Object[]{fixedMissing}));
      
    } else if (path.contains("/localeDisplayNames") && !path.contains("/localeDisplayPattern")) {
      UnicodeSet appropriateExemplars = path.contains("_") ? scriptRegionExemplarsWithParens : scriptRegionExemplars;
      if (!appropriateExemplars.containsAll(value)) {
        UnicodeSet missing = new UnicodeSet().addAll(value).removeAll(appropriateExemplars);
        String fixedMissing = new PrettyPrinter()
        .setOrdering(col != null ? col : Collator.getInstance(ULocale.ROOT))
        .setSpaceComparator(col != null ? col : Collator.getInstance(ULocale.ROOT)
                .setStrength2(Collator.PRIMARY))
                .setCompressRanges(true)
                .setToQuote(null)
                .setQuoter(null).format(missing);
        result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.warningType).setSubtype(Subtype.discouragedCharactersInTranslation)
                .setMessage("The characters \u200E{1}\u200E are discouraged in display names. Please choose the best single name.", new Object[]{null,fixedMissing}));
        // note: we are using {1} so that we don't include these in the console summary of bad characters.
      }
    }
    // check for spaces 
    if (!value.equals(value.trim())) {
      if (!leadOrTrailWhitespaceOk.reset(path).find()) {
        result.add(new CheckStatus().setCause(this).setMainType(CheckStatus.warningType).setSubtype(Subtype.mustNotStartOrEndWithSpace)
                .setMessage("This item must not start or end with whitespace."));
      }
    }
    return this;
  }
}
