/*
 ******************************************************************************
 * Copyright (C) 2005, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */

package org.unicode.cldr.test;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.InternalCldrException;
import org.unicode.cldr.util.CldrUtility;

import com.ibm.icu.dev.test.util.ElapsedTimer;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.Transliterator;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides a foundation for both console-driven CLDR tests, and
 * Survey Tool Tests.
 * <p>
 * To add a test, subclass CLDRFile and override handleCheck and possibly
 * setCldrFileToCheck. Then put the test into getCheckAll.
 * <p>
 * To use the test, take a look at the main in ConsoleCheckCLDR. Note that you need to call
 * setDisplayInformation with the CLDRFile for the locale that you want the
 * display information (eg names for codes) to be in.<br>
 * Some options are passed in the Map options. Examples:
 *       boolean SHOW_TIMES = options.containsKey("SHOW_TIMES"); // for printing times for doing setCldrFileToCheck
 * 
 * @author davis
 */
abstract public class CheckCLDR {
  private static CLDRFile displayInformation;
  public static String finalErrorType = CheckStatus.errorType;

  private CLDRFile cldrFileToCheck;
  private CLDRFile resolvedCldrFileToCheck;
  private boolean skipTest = false;
  private Phase phase;
  
  enum Phase {
    SUBMISSION, VETTING, FINAL_TESTING;

    static Phase forString(String value) {
      return value == null ? null : Phase.valueOf(value.toUpperCase(Locale.ENGLISH));
    }
  }
  
  public boolean isSkipTest() {
    return skipTest;
  }

  // this should only be set for the test in setCldrFileToCheck
  public void setSkipTest(boolean skipTest) {
    this.skipTest = skipTest;
  }

  /**
   * Here is where the list of all checks is found. 
   * @param nameMatcher Regex pattern that determines which checks are run,
   * based on their class name (such as .* for all checks, .*Collisions.* for CheckDisplayCollisions, etc.)
   * @return
   */
  public static CompoundCheckCLDR getCheckAll(String nameMatcher) {
    return new CompoundCheckCLDR()
    .setFilter(Pattern.compile(nameMatcher,Pattern.CASE_INSENSITIVE).matcher(""))
    .add(new CheckAttributeValues())
    .add(new CheckChildren())
    .add(new CheckCoverage())
    .add(new CheckDates())
    .add(new CheckDisplayCollisions())
    .add(new CheckExemplars())
    .add(new CheckForExemplars())
    .add(new CheckNumbers())
    // .add(new CheckZones()) this doesn't work; many spurious errors that user can't correct
    .add(new CheckMetazones())
    .add(new CheckAlt())
   .add(new CheckCurrencies())
    .add(new CheckCasing())
    //.add(new CheckConsistentCasing())
    //.add(new CheckNew()) // this is at the end; it will check for other certain other errors and warnings and not add a message if there are any.
    ;
  }
  
  /**
   * These determine what language is used to display information. Must be set before use.
   * @param locale
   * @return
   */
  public static synchronized CLDRFile getDisplayInformation() {
    return displayInformation;
  }
  public static synchronized void setDisplayInformation(CLDRFile inputDisplayInformation) {
    displayInformation = inputDisplayInformation;
  }
  /**
   * [Warnings - please zoom in]  dates/timeZoneNames/singleCountries   
(empty)
        [refs][hide] Ref:     [Zoom...]
[Warnings - please zoom in]   dates/timeZoneNames/hours   {0}/{1}   {0}/{1}
        [refs][hide] Ref:     [Zoom...]
[Warnings - please zoom in]   dates/timeZoneNames/hour  +HH:mm;-HH:mm   
+HH:mm;-HH:mm
      [refs][hide] Ref:     [Zoom...]
[ok]  layout/orientation     (empty)
        [refs][hide] Ref:     [Zoom...]
[ok]  dates/localizedPatternChars  GyMdkHmsSEDFwWahKzYeugAZvcL 
GaMjkHmsSEDFwWxhKzAeugXZvcL
        [refs][hide] Ref:     [Zoom...]*/
  
  public static final Pattern skipShowingInSurvey = Pattern.compile(
      ".*/(" +
      "beforeCurrency" + // hard to explain, use bug
      "|afterCurrency" + // hard to explain, use bug
      "|orientation" + // hard to explain, use bug
      "|appendItems" + // hard to explain, use bug
      "|singleCountries" + // hard to explain, use bug
      // from deprecatedItems in supplemental metadata
      "|hoursFormat" + // deprecated
      "|localizedPatternChars" + // deprecated
      "|abbreviationFallback" + // deprecated
      "|default" + // deprecated
      "|mapping" + // deprecated
      "|measurementSystem" + // deprecated
      "|preferenceOrdering" + // deprecated
      ")((\\[|/).*)?", Pattern.COMMENTS); // the last bit is to ensure whole element
  
  /**
   * These are paths for items that are complicated, and we need to force a zoom on.
   */
  public static final Pattern FORCE_ZOOMED_EDIT = Pattern.compile(
      ".*/(" +
      "exemplarCharacters" +
      /*"|metazone" +*/
      "|pattern" +
      "|dateFormatItem" +
      "|relative" +
      "|hourFormat" +
      "|gmtFormat" +
      "|regionFormat" +
      ")((\\[|/).*)?", Pattern.COMMENTS); // the last bit is to ensure whole element

  /**
   * Get the CLDRFile.
   * @param cldrFileToCheck
   */
  public final CLDRFile getCldrFileToCheck() {
    return cldrFileToCheck;
  }
  
  public final CLDRFile getResolvedCldrFileToCheck() {
    if (resolvedCldrFileToCheck == null) resolvedCldrFileToCheck = cldrFileToCheck.getResolved();
    return resolvedCldrFileToCheck;
  }
  /**
   * Set the CLDRFile. Must be done before calling check. If null is called, just skip
   * Often subclassed for initializing. If so, make the first 2 lines:
   * 		if (cldrFileToCheck == null) return this;
   * 		super.setCldrFileToCheck(cldrFileToCheck);
   * 		do stuff
   * @param cldrFileToCheck
   * @param options TODO
   * @param possibleErrors TODO
   */
  public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Map<String, String> options, List<CheckStatus> possibleErrors) {
    this.cldrFileToCheck = cldrFileToCheck;
    resolvedCldrFileToCheck = null;
    return this;
  }
  /**
   * Status value returned from check
   */
  public static class CheckStatus {
    public static final String 
    alertType = "Comment", 
    warningType = "Warning", 
    errorType = "Error", 
    exampleType = "Example",
    demoType = "Demo";
    enum Subtype {none, noUnproposedVariant, deprecatedAttribute, illegalPlural, invalidLocale, 
      incorrectCasing, valueAlwaysOverridden, nullChildFile, internalError, coverageLevel, 
      missingPluralInfo, currencySymbolTooWide, incorrectDatePattern, abbreviatedDateFieldTooWide, 
      displayCollision, illegalExemplarSet, missingAuxiliaryExemplars, missingPlaceholders, shouldntHavePlaceholders,
      couldNotAccessExemplars, noExemplarCharacters, modifiedEnglishValue, invalidCurrencyMatchSet, 
      multipleMetazoneMappings, noMetazoneMapping, noMetazoneMappingAfter1970, noMetazoneMappingBeforeNow, 
      cannotCreateZoneFormatter, insufficientCoverage, missingLanguageTerritoryInfo, missingEuroCountryInfo,
      deprecatedAttributeWithReplacement, missingOrExtraDateField, internalUnicodeSetFormattingError, 
      auxiliaryExemplarsOverlap,
      
      charactersNotInCurrencyExemplars, asciiCharactersNotInCurrencyExemplars,
      charactersNotInMainOrAuxiliaryExemplars, asciiCharactersNotInMainOrAuxiliaryExemplars,
      
      narrowDateFieldTooWide, illegalCharactersInExemplars, orientationDisagreesWithExemplars,
      illegalDatePattern, missingMainExemplars, discouragedCharactersInTranslation, mustNotStartOrEndWithSpace,
      illegalCharactersInNumberPattern, numberPatternNotCanonical, currencyPatternMissingCurrencySymbol,
      percentPatternMissingPercentSymbol, illegalNumberFormat, unexpectedAttributeValue, metazoneContainsDigit;
        public String toString() {
          return TO_STRING.matcher(name()).replaceAll(" $1").toLowerCase();
        }
        static Pattern TO_STRING = Pattern.compile("([A-Z])");
      };
    private String type;
    private Subtype subtype = Subtype.none;
    private String messageFormat;
    private Object[] parameters;
    private String htmlMessage;
    private CheckCLDR cause;
    private boolean checkOnSubmit = true;
    
    public CheckStatus() {
      
    }
    public boolean isCheckOnSubmit() {
      return checkOnSubmit;
    }
    public CheckStatus setCheckOnSubmit(boolean dependent) {
      this.checkOnSubmit = dependent;
      return this;
    }
    public String getType() {
      return type;
    }
    public CheckStatus setMainType(String type) {
      this.type = type;
      return this;
    }
    public String getMessage() {
      if (messageFormat == null) return messageFormat;
      return MessageFormat.format(MessageFormat.autoQuoteApostrophe(messageFormat), parameters);
    }
    /*
     * If this is not null, wrap it in a <form>...</form> and display. When you get a submit, call getDemo
     * to get a demo that you can call to change values of the fields. See CheckNumbers for an example. 
     */
    public String getHTMLMessage() {
      return htmlMessage;
    }
    public CheckStatus setHTMLMessage(String message) {
      htmlMessage = message;
      return this;
    }
    public CheckStatus setMessage(String message) {
      this.messageFormat = message;
      return this;
    }
    public CheckStatus setMessage(String message, Object... messageArguments) {
      this.messageFormat = message;
      this.parameters = messageArguments;
      return this;
    }
    public String toString() {
      return getType() + ": " + getMessage();
    }
    /**
     * Warning: don't change the contents of the parameters after retrieving.
     */
    public Object[] getParameters() {
      return parameters;
    }
    /**
     * Warning: don't change the contents of the parameters after passing in.
     */
    public CheckStatus setParameters(Object[] parameters) {
      this.parameters = parameters;
      return this;
    }
    public SimpleDemo getDemo() {
      return null;
    }
    public CheckCLDR getCause() {
      return cause;
    }
    public CheckStatus setCause(CheckCLDR cause) {
      this.cause = cause;
      return this;
    }
    protected Subtype getSubtype() {
      return subtype;
    }
    protected CheckStatus setSubtype(Subtype subtype) {
      this.subtype = subtype;
      return this;
    }
  }
  
  public static abstract class SimpleDemo {
    Map internalPostArguments = new HashMap();
    
    /**
     * @param postArguments A read-write map containing post-style arguments. eg TEXTBOX=abcd, etc.
     * <br>The first time this is called, the Map should be empty.
     * @return true if the map has been changed
     */ 
    public abstract String getHTML(Map postArguments) throws Exception;
    
    /**
     * Only here for compatibiltiy. Use the other getHTML instead
     */
    public final String getHTML(String path, String fullPath, String value) throws Exception {
      return getHTML(internalPostArguments);
    }
    
    /**
     * THIS IS ONLY FOR COMPATIBILITY: you can call this, then the non-postArguments form of getHTML; or better, call
     * getHTML with the postArguments.
     * @param postArguments A read-write map containing post-style arguments. eg TEXTBOX=abcd, etc.
     * @return true if the map has been changed
     */ 
    public final boolean processPost(Map postArguments) {
      internalPostArguments.clear();
      internalPostArguments.putAll(postArguments);
      return true;
    }
//  /**
//  * Utility for setting map. Use the paradigm in CheckNumbers.
//  */
//  public boolean putIfDifferent(Map inout, String key, String value) {
//  Object oldValue = inout.put(key, value);
//  return !value.equals(oldValue);
//  }
  }
  
  public static abstract class FormatDemo extends SimpleDemo {
    protected String currentPattern, currentInput, currentFormatted, currentReparsed;
    protected ParsePosition parsePosition = new ParsePosition(0);
    
    protected abstract String getPattern();
    protected abstract String getSampleInput(); 
    protected abstract void getArguments(Map postArguments);
    
    public String getHTML(Map postArguments) throws Exception {
      getArguments(postArguments);
      StringBuffer htmlMessage = new StringBuffer();
      FormatDemo.appendTitle(htmlMessage);
      FormatDemo.appendLine(htmlMessage, currentPattern, currentInput, currentFormatted, currentReparsed);
      htmlMessage.append("</table>");
      return htmlMessage.toString();
    }
    
    public String getPlainText(Map postArguments) {
      getArguments(postArguments);
      return MessageFormat.format("<\"\u200E{0}\u200E\", \"{1}\"> \u2192 \"\u200E{2}\u200E\" \u2192 \"{3}\"",
          new String[] {currentPattern, currentInput, currentFormatted, currentReparsed});
    }
    
    /**
     * @param htmlMessage
     * @param pattern
     * @param input
     * @param formatted
     * @param reparsed
     */
    public static void appendLine(StringBuffer htmlMessage, String pattern, String input, String formatted, String reparsed) {
      htmlMessage.append("<tr><td><input type='text' name='pattern' value='")
      .append(TransliteratorUtilities.toXML.transliterate(pattern))
      .append("'></td><td><input type='text' name='input' value='")
      .append(TransliteratorUtilities.toXML.transliterate(input))
      .append("'></td><td>")
      .append("<input type='submit' value='Test' name='Test'>")
      .append("</td><td>" + "<input type='text' name='formatted' value='")
      .append(TransliteratorUtilities.toXML.transliterate(formatted))
      .append("'></td><td>" + "<input type='text' name='reparsed' value='")
      .append(TransliteratorUtilities.toXML.transliterate(reparsed))
      .append("'></td></tr>");
    }
    
    /**
     * @param htmlMessage
     */
    public static void appendTitle(StringBuffer htmlMessage) {
      htmlMessage.append("<table border='1' cellspacing='0' cellpadding='2'" +
          //" style='border-collapse: collapse' style='width: 100%'" +
          "><tr>" +
          "<th>Pattern</th>" +
          "<th>Unlocalized Input</th>" +
          "<th></th>" +
          "<th>Localized Format</th>" +
          "<th>Re-Parsed</th>" +
      "</tr>");
    }
  }
  
  /**
   * Checks the path/value in the cldrFileToCheck for correctness, according to some criterion.
   * If the path is relevant to the check, there is an alert or warning, then a CheckStatus is added to List.
   * @param path Must be a distinguished path, such as what comes out of CLDRFile.iterator()
   * @param fullPath Must be the full path
   * @param value the value associated with the path
   * @param options A set of test-specific options. Set these with code of the form:<br>
   * options.put("CoverageLevel.localeType", "G0")<br>
   * That is, the key is of the form <testname>.<optiontype>, and the value is of the form <optionvalue>.<br>
   * There is one general option; the following will select only the tests that should be run during this phase.<br>
   * options.put("phase", Phase.<something>);
   * It can be used for new data entry.
   * @param result
   */
  public final CheckCLDR check(String path, String fullPath, String value, Map<String, String> options, List<CheckStatus> result) {
    if(cldrFileToCheck == null) {
      throw new InternalCldrException("CheckCLDR problem: cldrFileToCheck must not be null");
    }
    if (path == null) {
      throw new InternalCldrException("CheckCLDR problem: path must not be null");
    }
//    if (fullPath == null) {
//      throw new InternalError("CheckCLDR problem: fullPath must not be null");
//    }
//    if (value == null) {
//      throw new InternalError("CheckCLDR problem: value must not be null");
//    }
    result.clear();
    return handleCheck(path, fullPath, value, options, result);
  }
  
  
  /**
   * Returns any examples in the result parameter. Both examples and demos can
   * be returned. A demo will have getType() == CheckStatus.demoType. In that
   * case, there will be no getMessage or getHTMLMessage available; instead,
   * call getDemo() to get the demo, then call getHTML() to get the initial
   * HTML.
   */ 
  public final CheckCLDR getExamples(String path, String fullPath, String value, Map options, List result) {
    result.clear();
    return handleGetExamples(path, fullPath, value, options, result);		
  }
  
  
  protected CheckCLDR handleGetExamples(String path, String fullPath, String value, Map options2, List result) {
    return this; // NOOP unless overridden
  }
  
  /**
   * This is what the subclasses override.
   * If they ever use pathParts or fullPathParts, they need to call initialize() with the respective
   * path. Otherwise they must NOT change pathParts or fullPathParts.
   * <p>If something is found, a CheckStatus is added to result. This can be done multiple times in one call,
   * if multiple errors or warnings are found. The CheckStatus may return warnings, errors,
   * examples, or demos. We may expand that in the future.
   * <p>The code to add the CheckStatus will look something like::
   * <pre> result.add(new CheckStatus()
   * 		.setType(CheckStatus.errorType)
   *		.setMessage("Value should be {0}", new Object[]{pattern}));				
   * </pre>
   * @param options TODO
   */
  abstract public CheckCLDR handleCheck(String path, String fullPath, String value,
      Map<String, String> options, List<CheckStatus> result);
  
  /**
   * Internal class used to bundle up a number of Checks.
   * @author davis
   *
   */
  static class CompoundCheckCLDR extends CheckCLDR {
    private Matcher filter;
    private List checkList = new ArrayList();
    private List filteredCheckList = new ArrayList();
    
    public CompoundCheckCLDR add(CheckCLDR item) {
      checkList.add(item);
      if (filter == null || filter.reset(item.getClass().getName()).matches()) {
        filteredCheckList.add(item);
      }
      return this;
    }
    public CheckCLDR handleCheck(String path, String fullPath, String value,
        Map<String, String> options, List<CheckStatus> result) {
      result.clear();
      for (Iterator it = filteredCheckList.iterator(); it.hasNext(); ) {
        CheckCLDR item = (CheckCLDR) it.next();
        // skip proposed items in final testing.
        if (Phase.FINAL_TESTING == item.getPhase()) {
          if (path.contains("proposed") && path.contains("[@alt=")) {
            continue;
          }
        }
        try {
          if (!item.isSkipTest()) {
            item.handleCheck(path, fullPath, value, options, result);
          }
        } catch (Exception e) {
          addError(result, item, e);
          return this;
        }
      }
      return this;
    }
    
    protected CheckCLDR handleGetExamples(String path, String fullPath, String value, Map options, List result) {
      result.clear();
      for (Iterator it = filteredCheckList.iterator(); it.hasNext(); ) {
        CheckCLDR item = (CheckCLDR) it.next();
        try {
          item.handleGetExamples(path, fullPath, value, options, result);
        } catch (Exception e) {
          addError(result, item, e);
          return this;
        }
      }
      return this;
    }
    
    private void addError(List<CheckStatus> result, CheckCLDR item, Exception e) {
      result.add(new CheckStatus().setMainType(CheckStatus.errorType).setSubtype(Subtype.internalError)
          .setMessage("Internal error in {0}. Exception: {1}, Message: {2}, Trace: {3}", 
              new Object[]{item.getClass().getName(), e.getClass().getName(), e, 
              Arrays.asList(e.getStackTrace())}));
    }
    
    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Map<String, String> options, List<CheckStatus> possibleErrors) {
      ElapsedTimer testTime = null, testOverallTime = null;
      if (cldrFileToCheck == null) return this;
      boolean SHOW_TIMES = options.containsKey("SHOW_TIMES");
      setPhase(Phase.forString(options.get("phase")));
      if(SHOW_TIMES) testOverallTime = new ElapsedTimer("Test setup time for setCldrFileToCheck: {0}");
      super.setCldrFileToCheck(cldrFileToCheck,options,possibleErrors);
      possibleErrors.clear();

      for (Iterator it = filteredCheckList.iterator(); it.hasNext(); ) {
        CheckCLDR item = (CheckCLDR) it.next();
        if(SHOW_TIMES) testTime = new ElapsedTimer("Test setup time for " + item.getClass().toString() + ": {0}");
        try {
          item.setPhase(getPhase());
          item.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
          if(SHOW_TIMES) {
            if (item.isSkipTest()) {
              System.out.println("Disabled : " + testTime);
            } else {
              System.out.println("OK : " + testTime);
            }
          }
        } catch (RuntimeException e) {
          addError(possibleErrors, item, e);
          if(SHOW_TIMES) System.out.println("ERR: " + testTime + " - " + e.toString());
        }
      }
      if(SHOW_TIMES) System.out.println("Overall: " + testOverallTime + ": {0}");
      return this;
    }
    public Matcher getFilter() {
      return filter;
    }
    
    public CompoundCheckCLDR setFilter(Matcher filter) {
      this.filter = filter;
      filteredCheckList.clear();
      for (Iterator it = checkList.iterator(); it.hasNext(); ) {
        CheckCLDR item = (CheckCLDR) it.next();
        if (filter == null || filter.reset(item.getClass().getName()).matches()) {
          filteredCheckList.add(item);
          item.setCldrFileToCheck(getCldrFileToCheck(), null, null);
        }
      }
      return this;
    }
  }
  
  //static Transliterator prettyPath = getTransliteratorFromFile("ID", "prettyPath.txt");
  
  public static Transliterator getTransliteratorFromFile(String ID, String file) {
    try {
      BufferedReader br = CldrUtility.getUTF8Data(file);
      StringBuffer input = new StringBuffer();
      while (true) {
        String line = br.readLine();
        if (line == null) break;
        if (line.startsWith("\uFEFF")) line = line.substring(1); // remove BOM
        input.append(line);
        input.append('\n');
      }
      return Transliterator.createFromRules(ID, input.toString(), Transliterator.FORWARD);
    } catch (IOException e) {
      throw (IllegalArgumentException) new IllegalArgumentException("Can't open transliterator file " + file).initCause(e);
    }
  }

  public Phase getPhase() {
    return phase;
  }

  public void setPhase(Phase phase) {
    this.phase = phase;
  }
  
}
