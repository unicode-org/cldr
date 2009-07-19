package org.unicode.cldr.unittest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.DisplayAndInputProcessor;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CharacterFallbacks;
import org.unicode.cldr.util.PrettyPath;
import org.unicode.cldr.util.Relation;
import org.unicode.cldr.util.Row;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.Row.R2;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.LocaleData;
import com.ibm.icu.util.ULocale;

public class TestBasic extends TestFmwk {
  static TestInfo testInfo = TestInfo.getInstance();

  /**
   * Simple test that loads each file in the cldr directory, thus verifying that
   * the DTD works, and also checks that the PrettyPaths work.
   * 
   * @author markdavis
   */

  public static void main(String[] args) {
    new TestBasic().run(args);
  }

  private static final Set<String> skipAttributes    = new HashSet<String>(Arrays.asList("alt", "draft",
  "references"));
  private static final Matcher  skipPaths = Pattern.compile("/identity" + "|/alias" + "|\\[@alt=\"proposed")
  .matcher("");


  private final String           localeRegex = Utility.getProperty("locale", ".*");

  private final boolean          showInfo          = Utility.getProperty("showinfo", false);

  private final String           commonDirectory  = Utility.COMMON_DIRECTORY;

  private final String           mainDirectory = Utility.MAIN_DIRECTORY;

  //private final boolean          showForceZoom = Utility.getProperty("forcezoom", false);

  private final boolean          resolved = Utility.getProperty("resolved", false);

  private final Exception[]      internalException = new Exception[1];

  private boolean pretty = Utility.getProperty("pretty", true);

  public void TestDtds() throws IOException {
    checkDtds(commonDirectory + "/collation");
    checkDtds(commonDirectory + "/main");
    checkDtds(commonDirectory + "/rbnf");
    checkDtds(commonDirectory + "/segments");
    checkDtds(commonDirectory + "/supplemental");
    checkDtds(commonDirectory + "/transforms");
    checkDtds(commonDirectory + "/../test");
  }

  private void checkDtds(String directory) throws IOException {
    File directoryFile = new File(directory);
    File[] listFiles = directoryFile.listFiles();
    String canonicalPath = directoryFile.getCanonicalPath();
    if (listFiles == null) {
      throw new IllegalArgumentException("Empty directory: " + canonicalPath);
    }
    logln("Checking files for DTD errors in: " + canonicalPath);
    for (File fileName : listFiles) {
      if (!fileName.toString().endsWith(".xml")) {
        continue;
      }
      check(fileName);
    }
  }

  class MyErrorHandler implements ErrorHandler {
    public void error(SAXParseException exception) throws SAXException {
      errln("error: " + XMLFileReader.showSAX(exception));
      throw exception;
    }

    public void fatalError(SAXParseException exception) throws SAXException {
      errln("fatalError: " + XMLFileReader.showSAX(exception));
      throw exception;
    }

    public void warning(SAXParseException exception) throws SAXException {
      errln("warning: " + XMLFileReader.showSAX(exception));
      throw exception;
    }
  }

  public void check(File systemID) {
    try {
      FileInputStream fis = new FileInputStream(systemID);
      XMLReader xmlReader = XMLFileReader.createXMLReader(true);
      xmlReader.setErrorHandler(new MyErrorHandler());
      InputSource is = new InputSource(fis);
      is.setSystemId(systemID.toString());
      xmlReader.parse(is);
      fis.close();
    } catch (SAXParseException e) {
      errln("\t" + "Can't read " + systemID + "\t" + e.getClass() + "\t" + e.getMessage());
    } catch (SAXException e) {
      errln("\t" + "Can't read " + systemID + "\t" + e.getClass() + "\t" + e.getMessage());
    } catch (IOException e) {
      errln("\t" + "Can't read " + systemID + "\t" + e.getClass() + "\t" + e.getMessage());
    }
  }

  public void TestCurrencyFallback() {
    XPathParts parts = new XPathParts();
    Factory cldrFactory = Factory.make(mainDirectory, localeRegex);
    CLDRFile english = cldrFactory.make("en", true);
    Set<String> currencies = StandardCodes.make().getAvailableCodes("currency");

    final UnicodeSet CHARACTERS_THAT_SHOULD_HAVE_FALLBACKS = (UnicodeSet) new UnicodeSet("[[:sc:]-[\\u0000-\\u00FF]]").freeze();

    CharacterFallbacks fallbacks = CharacterFallbacks.make();

    for (String locale : cldrFactory.getAvailable()) {
      CLDRFile file = cldrFactory.make(locale, false);
      if (file.isNonInheriting())
        continue;

      final UnicodeSet OK_CURRENCY_FALLBACK = (UnicodeSet) new UnicodeSet("[\\u0000-\\u00FF]")
      .addAll(safeExemplars(file, ""))
      .addAll(safeExemplars(file, "auxiliary"))
      .addAll(safeExemplars(file, "currencySymbol"))
      .freeze();
      UnicodeSet badSoFar = new UnicodeSet();


      for (Iterator<String> it = file.iterator(); it.hasNext();) {
        String path = it.next();
        if (path.endsWith("/alias")) {
          continue;
        }
        String value = file.getStringValue(path);

        // check for special characters
        
        if (CHARACTERS_THAT_SHOULD_HAVE_FALLBACKS.containsSome(value)) {

          parts.set(path);
          if (!parts.getElement(-1).equals("symbol")) {
            continue;
          }
          String currencyType = parts.getAttributeValue(-2, "type");

          UnicodeSet fishy = new UnicodeSet().addAll(value).retainAll(CHARACTERS_THAT_SHOULD_HAVE_FALLBACKS).removeAll(badSoFar);
          for (UnicodeSetIterator it2 = new UnicodeSetIterator(fishy); it2.next();) {
            final int fishyCodepoint = it2.codepoint;
            List<String> fallbackList = fallbacks.getSubstitutes(fishyCodepoint);

            String nfkc = Normalizer.normalize(fishyCodepoint, Normalizer.NFKC);
            if (!nfkc.equals(UTF16.valueOf(fishyCodepoint))) {
              if (fallbackList == null) {
                fallbackList = new ArrayList<String>();
              } else {
                fallbackList = new ArrayList<String>(fallbackList); // writable
              }
              fallbackList.add(nfkc);
            }
            // later test for all Latin-1
            if (fallbackList == null) {
              errln("Locale:\t" + locale + ";\tCharacter with no fallback:\t" + it2.getString() + "\t" + UCharacter.getName(fishyCodepoint));
              badSoFar.add(fishyCodepoint);
            } else {
              String fallback = null;
              for (String fb : fallbackList) {
                if (OK_CURRENCY_FALLBACK.containsAll(fb)) {
                  if (!fb.equals(currencyType) && currencies.contains(fb)) {
                    errln("Locale:\t" + locale +  ";\tCurrency:\t" + currencyType + ";\tFallback converts to different code!:\t" + fb
                            + "\t" + it2.getString() + "\t" + UCharacter.getName(fishyCodepoint));
                  }
                  if (fallback == null) {
                    fallback = fb;
                  }
                }
              }
              if (fallback == null) {
                errln("Locale:\t" + locale + ";\tCharacter with no good fallback (exemplars+Latin1):\t" + it2.getString() + "\t" + UCharacter.getName(fishyCodepoint));
                badSoFar.add(fishyCodepoint);
              } else {
                errln("Locale:\t" + locale + ";\tCharacter with good fallback:\t"
                        + it2.getString() + " " + UCharacter.getName(fishyCodepoint)
                        + " => " + fallback);
                badSoFar.add(fishyCodepoint);
              }
            }
          }
        }
      }
    }
  }


  public void TestPaths() {
    Relation<String, String> distinguishing = new Relation(new TreeMap(), TreeSet.class, null);
    Relation<String, String> nonDistinguishing = new Relation(new TreeMap(), TreeSet.class, null);
    XPathParts parts = new XPathParts();
    Factory cldrFactory = Factory.make(mainDirectory, localeRegex);
    CLDRFile english = cldrFactory.make("en", true);


    Relation<String, String> pathToLocale = new Relation(new TreeMap(CLDRFile.ldmlComparator),
            TreeSet.class, null);
    for (String locale : cldrFactory.getAvailable()) {
      // if (locale.equals("root") && !localeRegex.equals("root"))
      // continue;
      CLDRFile file = cldrFactory.make(locale, resolved);
      if (file.isNonInheriting())
        continue;
      DisplayAndInputProcessor displayAndInputProcessor = new DisplayAndInputProcessor(file);


      logln(locale + "\t-\t" + english.getName(locale));


      for (Iterator<String> it = file.iterator(); it.hasNext();) {
        String path = it.next();
        if (path.endsWith("/alias")) {
          continue;
        }
        String value = file.getStringValue(path);
        if (value == null) {
          throw new IllegalArgumentException(locale + "\tError: in null value at " + path);
        }


        String displayValue = displayAndInputProcessor.processForDisplay(path, value);
        if (!displayValue.equals(value)) {
          logln("\t" + locale + "\tdisplayAndInputProcessor changes display value <" + value
                  + ">\t=>\t<" + displayValue + ">\t\t" + path);
        }
        String inputValue = displayAndInputProcessor.processInput(path, value, internalException);
        if (internalException[0] != null) {
          errln("\t" + locale + "\tdisplayAndInputProcessor internal error <" + value + ">\t=>\t<"
                  + inputValue + ">\t\t" + path);
          internalException[0].printStackTrace(System.out);
        }
        if (isVerbose() && !inputValue.equals(value)) {
          displayAndInputProcessor.processInput(path, value, internalException); // for
          // debugging
          logln("\t" + locale + "\tdisplayAndInputProcessor changes input value <" + value
                  + ">\t=>\t<" + inputValue + ">\t\t" + path);
        }

        pathToLocale.put(path, locale);

        // also check for non-distinguishing attributes
        if (path.contains("/identity"))
          continue;

        // make sure we don't have problem alts
        if (false && path.contains("proposed")) {
          String sourceLocale = file.getSourceLocaleID(path, null);
          if (locale.equals(sourceLocale)) {
            String nonAltPath = file.getNondraftNonaltXPath(path);
            if (!path.equals(nonAltPath)) {
              String nonAltLocale = file.getSourceLocaleID(nonAltPath, null);
              String nonAltValue = file.getStringValue(nonAltPath);
              if (nonAltValue == null || !locale.equals(nonAltLocale)) {
                errln("\t" + locale + "\tProblem alt=proposed <" + value + ">\t\t" + path);
              }
            }
          }
        }

        String fullPath = file.getFullXPath(path);
        parts.set(fullPath);
        for (int i = 0; i < parts.size(); ++i) {
          if (parts.getAttributeCount(i) == 0)
            continue;
          String element = parts.getElement(i);
          for (String attribute : parts.getAttributeKeys(i)) {
            if (skipAttributes.contains(attribute))
              continue;
            if (CLDRFile.isDistinguishing(element, attribute)) {
              distinguishing.put(element, attribute);
            } else {
              nonDistinguishing.put(element, attribute);
            }
          }
        }
      }
    }

    if (isVerbose()) {

      System.out.format("Distinguishing Elements: %s" + Utility.LINE_SEPARATOR, distinguishing);
      System.out.format("Nondistinguishing Elements: %s" + Utility.LINE_SEPARATOR, nonDistinguishing);
      System.out.format("Skipped %s" + Utility.LINE_SEPARATOR, skipAttributes);

      logln(Utility.LINE_SEPARATOR + "Paths to skip in Survey Tool");
      for (String path : pathToLocale.keySet()) {
        if (CheckCLDR.skipShowingInSurvey.matcher(path).matches()) {
          logln("Skipping: " + path);
        }
      }

      logln(Utility.LINE_SEPARATOR + "Paths to force zoom in Survey Tool");
      for (String path : pathToLocale.keySet()) {
        if (CheckCLDR.FORCE_ZOOMED_EDIT.matcher(path).matches()) {
          logln("Forced Zoom Edit: " + path);
        }
      }
    }

    if (pretty) {
      if (showInfo) {
        logln(Utility.LINE_SEPARATOR + "Showing Path to PrettyPath mapping" + Utility.LINE_SEPARATOR);
      }
      PrettyPath prettyPath = new PrettyPath().setShowErrors(true);
      Set<String> badPaths = new TreeSet();
      for (String path : pathToLocale.keySet()) {
        String prettied = prettyPath.getPrettyPath(path, false);
        if (showInfo)
          logln(prettied + "\t\t" + path);
        if (prettied.contains("%%") && !path.contains("/alias")) {
          badPaths.add(path);
        }
      }
      // now remove root

      if (showInfo) {
        logln(Utility.LINE_SEPARATOR + "Showing Paths not in root" + Utility.LINE_SEPARATOR);
      }

      CLDRFile root = cldrFactory.make("root", true);
      for (Iterator<String> it = root.iterator(); it.hasNext();) {
        pathToLocale.removeAll(it.next());
      }
      if (showInfo)
        for (String path : pathToLocale.keySet()) {
          if (skipPaths.reset(path).find()) {
            continue;
          }
          logln(path + "\t" + pathToLocale.getAll(path));
        }

      if (badPaths.size() != 0) {
        errln("Error: " + badPaths.size()
                + " Paths were not prettied: use -DSHOW and look for ones with %% in them.");
      }
    }
  }

  private String doFormat(ULocale locale, Currency currency, double number) {
    //    ULocale myLocale = null;
    //    Currency myCurrency = null;
    //    double someNumber = 12345.678;
    // old
    NumberFormat format = NumberFormat.getCurrencyInstance(locale);
    format.setCurrency(currency);

    // if ICU 4.2 / CLDR 1.7, use ugly hack
    fixFormatIfCantDisplay(format, locale, currency);

    String result = format.format(number);
    return result;
  }

  final static boolean DO_JOHNS = false;

  private void fixFormatIfCantDisplay(NumberFormat format, ULocale locale, Currency currency) {
    // Ugly code, unoptimized; just presented here for illustration

    // John's suggestion; use currency if not for locale
    if (DO_JOHNS) {
      String[] codes = Currency.getAvailableCurrencyCodes(locale, new Date());

      // this is only an approximation, since the currency may have been used previously in the locale,
      // but ICU doesn't make that CLDR information accessible.

      for (String code : codes) {
        if (code.equals(currency.toString())) {
          return; // skip
        }
      }
    } else {
      // This also means that perfectly reasonable, well-established symbols like "Rp" for "INR"
      // will be unavailable for all locales but "IN".

      // Alternative, use hack to figure out if the user of the locale is likely to have the fonts
      // If we are using plural formatting, we have to take a different code path, so this would need enhancement!

      boolean[] isChoiceFormat = new boolean[1];
      String name = currency.getName(locale, Currency.SYMBOL_NAME, isChoiceFormat);

      // We actually would like to get the currencySymbol Exemplars, but those aren't available in ICU - another hole!
      // So we just assume Latin1.
      // LocaleData.getExemplarSet(locale, 0);

      final UnicodeSet OK_CURRENCY = (UnicodeSet) new UnicodeSet("[\\u0000-\\u00FF]").freeze();
      if (OK_CURRENCY.containsAll(name)) {
        return;
      }
    }

    // use bad hack to just use Intl Currency symbol. That means instead of getting "Rp", the user gets "INR".
    // If ICU exposed the characters.xml file, then we could use the fallbacks there instead

    DecimalFormat format2 = (DecimalFormat) format;
    String pattern = format2.toPattern();
    pattern = pattern.replace("\u00a4", "\u00a4\u00a4");
    format2.applyPattern(pattern);

    // Even if we wanted to use the fallbacks, we'd have to define our own CurrencyCode override,
    // because the ICU API is not rich enough to let us create a Currency instance that changes the right information.
    // That is what ICUServiceBuilder in CLDR has to do, which is a royal pain.
  }

  public void TestCurrency() {
    Map<String,Set<R2>> results = new TreeMap<String,Set<R2>>(Collator.getInstance(ULocale.ENGLISH));
    for (ULocale locale : ULocale.getAvailableLocales()) {
      if (locale.getCountry().length() != 0) {
        continue;
      }
      for (int i = 1; i < 4; ++i) {
        NumberFormat format = getCurrencyInstance(locale, i);
        for (Currency c : new Currency[] {Currency.getInstance("USD"), Currency.getInstance("EUR"), Currency.getInstance("INR")}) {
          format.setCurrency(c);
          final String formatted = format.format(12345.67);
          Set<R2> set = results.get(formatted);
          if (set == null) {
            results.put(formatted, set = new TreeSet<R2>());
          }
          set.add(Row.of(locale.toString(), i));
        }
      }
    }
    for (String formatted : results.keySet()) {
      System.out.println(formatted + "\t" + results.get(formatted)); 
    }
  }

  private static NumberFormat getCurrencyInstance(ULocale locale, int type) {
    NumberFormat format = NumberFormat.getCurrencyInstance(locale);
    if (type > 1) {
      DecimalFormat format2 = (DecimalFormat) format;
      String pattern = format2.toPattern();
      String replacement = "\u00a4\u00a4";
      for (int i = 2; i < type; ++i) {
        replacement += "\u00a4";
      }
      pattern = pattern.replace("\u00a4", replacement);
      format2.applyPattern(pattern);
    }
    return format;
  }

  private UnicodeSet safeExemplars(CLDRFile file, String string) {
    final UnicodeSet result = file.getExemplarSet(string, WinningChoice.NORMAL);
    return result != null ? result : new UnicodeSet();
  }

  public void TestAPath() {
    // <month type="1">1</month>
    String path = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"abbreviated\"]/month[@type=\"1\"]";
    CLDRFile root = testInfo.getRoot();
    logln("path: " + path);
    String fullpath = root.getFullXPath(path);
    logln("fullpath: " + fullpath);
    String value = root.getStringValue(path);
    logln("value: " + value);
    Status status = new Status();
    String source = root.getSourceLocaleID(path, status);
    logln("locale: " + source);
    logln("status: " + status);
  }

  public void TestDefaultContents() {
    Set<String> defaultContents = testInfo.getSupplementalDataInfo().getDefaultContentLocales();
    for (String locale : defaultContents) {
      CLDRFile cldrFile;
      try {
        cldrFile = testInfo.getCldrFactory().make(locale, false);
      } catch (RuntimeException e) {
        logln("Can't open default content file:\t" + locale);
        continue;
      }
      for (Iterator<String> it = cldrFile.iterator(); it.hasNext();) {
        String path = it.next();
        if (path.contains("/identity")) {
          continue;
        }
        errln("Default content file not empty:\t" + locale);
        showDifferences(locale);
        break;
      }
    }
  }

  private void showDifferences(String locale) {
    CLDRFile cldrFile = testInfo.getCldrFactory().make(locale, false);
    final String localeParent = cldrFile.getParent(locale);
    CLDRFile parentFile = testInfo.getCldrFactory().make(localeParent, true);
    int funnyCount = 0;
    for (Iterator<String> it = cldrFile.iterator("", CLDRFile.ldmlComparator); it.hasNext();) {
      String path = it.next();
      if (path.contains("/identity")) {
        continue;
      }
      final String fullXPath = cldrFile.getFullXPath(path);
      if (fullXPath.contains("[@draft=\"unconfirmed\"]") || fullXPath.contains("[@draft=\"provisional\"]")) {
        funnyCount++;
        continue;
      }
      logln("\tpath:\t" + path);
      logln("\t\t" + locale + " value:\t<" + cldrFile.getStringValue(path) + ">");
      final String parentFullPath = parentFile.getFullXPath(path);
      logln("\t\t" + localeParent + " value:\t<" + parentFile.getStringValue(path) + ">");
      logln("\t\t" + locale + " fullpath:\t" + fullXPath);
      logln("\t\t" + localeParent + " fullpath:\t" + parentFullPath);
    }
    logln("\tCount of non-approved:\t" + funnyCount);
  }
}
