/*
 **********************************************************************
 * Copyright (c) 2002-2004, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.icu;

import java.io.File;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.CLDRFile.DraftStatus;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.unicode.cldr.test.CLDRTest;
import org.unicode.cldr.tool.GenerateCldrTests;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.icu.util.Currency;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.lang.UScript;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateFormatSymbols;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

/**
 * This is a file that runs the CLDR tests for ICU4J, to verify that ICU4J implements them
 * correctly. It has gotten out of sync with the format, so needs to be fixed.
 * @author medavis
 */
public class TestCldr extends TestFmwk {
  static final boolean DEBUG = false;

  //ULocale uLocale = ULocale.ENGLISH;
  //Locale oLocale = Locale.ENGLISH; // TODO Drop once ICU4J has ULocale everywhere
  //static PrintWriter log;
  SAXParser SAX;

  static String MATCH;

  public static void main(String[] args) throws Exception {
    MATCH = System.getProperty("XML_MATCH");
    if (MATCH == null)
      MATCH = ".*";
    else
      System.out.println("Resetting MATCH:" + MATCH);

    new TestCldr().run(args);
  }

  String directory;

  Set allLocales = new TreeSet();

  public void TestScripts() {
    CLDRFile.Factory cldrFactory = CLDRFile.Factory.make(
        CldrUtility.MAIN_DIRECTORY, MATCH, DraftStatus.contributed);
    ULocale locales[] = ULocale.getAvailableLocales();
    for (int i = 0; i < locales.length; ++i) {
      ULocale locale = locales[i];
      logln(locale.toString());
      int[] scriptNumbers = UScript.getCode(locale);
      Set ICUScripts = new TreeSet();
      for (int j = 0; j < scriptNumbers.length; ++j) {
        ICUScripts.add(UScript.getShortName(scriptNumbers[j]));
      }

      CLDRFile cfile = cldrFactory.make(locale.toString(), true, true);
      UnicodeSet exemplars = getExemplarSet(cfile, "").addAll(
          getExemplarSet(cfile, "auxiliary"));
      Set CLDRScripts = getScriptsFromUnicodeSet(exemplars);
      if (!CLDRScripts.equals(ICUScripts)) {
        errln(locale + "\tscripts not equals.\tCLDR: " + CLDRScripts
            + ",\tICU: " + ICUScripts);
      } else {
        logln("\tCLDR:\t" + CLDRScripts + ",\tICU: " + ICUScripts);
      }
    }
  }

  public Set getScriptsFromUnicodeSet(UnicodeSet exemplars) {
    // use bits first, since that's faster
    BitSet scriptBits = new BitSet();
    boolean show = false;
    for (UnicodeSetIterator it = new UnicodeSetIterator(exemplars); it.next();) {
      if (show)
        System.out.println(Integer.toHexString(it.codepoint));
      if (it.codepoint != it.IS_STRING) {
        scriptBits.set(UScript.getScript(it.codepoint));
      } else {
        int cp;
        for (int i = 0; i < it.string.length(); i += UTF16.getCharCount(cp)) {
          scriptBits.set(UScript.getScript(cp = UTF16.charAt(it.string, i)));
        }
      }
    }
    scriptBits.clear(UScript.COMMON);
    scriptBits.clear(UScript.INHERITED);
    Set scripts = new TreeSet();
    for (int j = 0; j < scriptBits.size(); ++j) {
      if (scriptBits.get(j)) {
        scripts.add(UScript.getShortName(j));
      }
    }
    return scripts;
  }

  public UnicodeSet getExemplarSet(CLDRFile cldrfile, String type) {
    if (type.length() != 0)
      type = "[@type=\"" + type + "\"]";
    String v = cldrfile.getStringValue("//ldml/characters/exemplarCharacters"
        + type);
    if (v == null)
      return new UnicodeSet();
    return new UnicodeSet(v);
  }

  public void TestFiles() throws SAXException, IOException {
    directory = CldrUtility.TEST_DIR;
    /*
     Set s = GenerateCldrTests.getMatchingXMLFiles(directory, ".*");
     for (Iterator it = s.iterator(); it.hasNext();) {
     _test((String) it.next());
     }
     */
    // only get ICU's locales
    Set s = new TreeSet();
    addLocales(NumberFormat.getAvailableULocales(), s);
    addLocales(DateFormat.getAvailableULocales(), s);
    addLocales(Collator.getAvailableULocales(), s);

    Matcher m = Pattern.compile(MATCH).matcher("");
    for (Iterator it = s.iterator(); it.hasNext();) {
      String locale = (String) it.next();
      if (!m.reset(locale).matches())
        continue;
      _test(locale);
    }
  }

  public void addLocales(ULocale[] list, Collection s) {
    LanguageTagParser lsp = new LanguageTagParser();
    for (int i = 0; i < list.length; ++i) {
      allLocales.add(list[i].toString());
      String loc = list[i].toString();
      s.add(lsp.set(loc).getLanguage());
    }
  }

  public void _test(String localeName) throws SAXException, IOException {
    //uLocale = new ULocale(localeName);
    //oLocale = uLocale.toLocale();

    File f = new File(directory, localeName + ".xml");
    logln("Testing " + f.getCanonicalPath());
    SAX.parse(f, DEFAULT_HANDLER);
  }

  static Transliterator toUnicode = Transliterator.getInstance("any-hex");

  static public String showString(String in) {
    return "\u00AB" + in + "\u00BB (" + toUnicode.transliterate(in) + ")";
  }

  // ============ SAX Handler Infrastructure ============
  enum AttributeName {
    numberType, dateType, timeType, date, field, zone, parse, input, draft
  };

  abstract public class Handler {

    Map<AttributeName, String> settings = new TreeMap<AttributeName, String>();

    String name;

    List currentLocales = new ArrayList();

    int failures = 0;

    protected boolean approved = true;

    void setName(String name) {
      this.name = name;
    }

    void set(AttributeName attributeName, String attributeValue) {
      //if (DEBUG) logln(attributeName + " => " + attributeValue);
      settings.put(attributeName, attributeValue);
    }

    void checkResult(String value) {
      ULocale ul = new ULocale("xx");
      try {
        for (int i = 0; i < currentLocales.size(); ++i) {
          ul = (ULocale) currentLocales.get(i);
          //loglnSAX("  Checking " + ul + "(" + ul.getDisplayName(ULocale.ENGLISH) + ")" + " for " + name);
          handleResult(ul, value);
          if (failures != 0) {
            errln("\tTotal Failures: " + failures + "\t" + ul + "("
                + ul.getDisplayName(ULocale.ENGLISH) + ")");
            failures = 0;
          }
        }
      } catch (Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.flush();
        errln("Exception: Locale: " + ul + ",\tValue: <" + value + ">" + CldrUtility.LINE_SEPARATOR
            + sw.toString());
      }
    }

    public void loglnSAX(String message) {
      String temp = message + "\t[" + name;
      for (Iterator<AttributeName> it = settings.keySet().iterator(); it
          .hasNext();) {
        AttributeName attributeName = it.next();
        String attributeValue = (String) settings.get(attributeName);
        temp += " " + attributeName + "=<" + attributeValue + ">";
      }
      logln(temp + "]");
    }

    int lookupValue(Object x, Object[] list) {
      for (int i = 0; i < list.length; ++i) {
        if (x.equals(list[i]))
          return i;
      }
      loglnSAX("Unknown String: " + x);
      return -1;
    }

    abstract void handleResult(ULocale currentLocale, String value)
        throws Exception;

    /**
     * @param attributes
     */
    public void setAttributes(Attributes attributes) {
      String localeList = attributes.getValue("locales");
      String[] currentLocaleString = new String[50];
      com.ibm.icu.impl.Utility.split(localeList, ' ', currentLocaleString);
      currentLocales.clear();
      for (int i = 0; i < currentLocaleString.length; ++i) {
        if (currentLocaleString[i].length() == 0)
          continue;
        if (allLocales.contains("")) {
          logln("Skipping locale, not in ICU4J: " + currentLocaleString[i]);
          continue;
        }
        currentLocales.add(new ULocale(currentLocaleString[i]));
      }
      if (DEBUG)
        logln("Setting locales: " + currentLocales);
    }
  }

  public Handler getHandler(String name, Attributes attributes) {
    if (DEBUG)
      logln("Creating Handler: " + name);
    Handler result = (Handler) RegisteredHandlers.get(name);
    if (result == null)
      logln("Unexpected test type: " + name);
    else {
      result.setAttributes(attributes);
    }
    return result;
  }

  public void addHandler(String name, Handler handler) {
    handler.setName(name);
    RegisteredHandlers.put(name, handler);
  }

  Map RegisteredHandlers = new HashMap();

  // ============ Statics for Date/Number Support ============

  static TimeZone utc = TimeZone.getTimeZone("GMT");

  static DateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
  {
    iso.setTimeZone(utc);
  }

  static int[] DateFormatValues = { -1, DateFormat.SHORT, DateFormat.MEDIUM,
      DateFormat.LONG, DateFormat.FULL };

  static String[] DateFormatNames = { "none", "short", "medium", "long", "full" };

  static String[] NumberNames = { "standard", "integer", "decimal", "percent",
      "scientific", "GBP" };

  // ============ Handler for Collation ============ 
  static UnicodeSet controlsAndSpace = new UnicodeSet("[:cc:]");

  static String remove(String in, UnicodeSet toRemove) {
    int cp;
    StringBuffer result = new StringBuffer();
    for (int i = 0; i < in.length(); i += UTF16.getCharCount(cp)) {
      cp = UTF16.charAt(in, i);
      if (!toRemove.contains(cp))
        UTF16.append(result, cp);
    }
    return result.toString();
  }

  {
    addHandler("collation", new Handler() {
      public void handleResult(ULocale currentLocale, String value) {
        Collator col = Collator.getInstance(currentLocale);
        String lastLine = "";
        int count = 0;
        for (int pos = 0; pos < value.length();) {
          int nextPos = value.indexOf('\n', pos);
          if (nextPos < 0)
            nextPos = value.length();
          String line = value.substring(pos, nextPos);
          line = remove(line, controlsAndSpace); // HACK for SAX
          if (line.trim().length() != 0) { // HACK for SAX
            int comp = col.compare(lastLine, line);
            if (comp > 0) {
              failures++;
              errln("\tLine " + (count + 1) + "\tFailure: "
                  + showString(lastLine) + " should be leq " + showString(line));
            } else if (DEBUG) {
              logln("OK: " + line);
            }
            lastLine = line;
          }
          pos = nextPos + 1;
          count++;
        }
      }
    });

    // ============ Handler for Numbers ============ 
    addHandler("number", new Handler() {
      String numberType = null;

      public void handleResult(ULocale locale, String result) {
        NumberFormat nf = null;
        double v = Double.NaN;
        for (Iterator<AttributeName> it = settings.keySet().iterator(); it
            .hasNext();) {
          AttributeName attributeName = it.next();
          String attributeValue = settings.get(attributeName);
          switch (attributeName) {
            case input:
              v = Double.parseDouble(attributeValue);
              continue;
            case draft:
              approved = attributeValue.contains("approved");
              break;
            case numberType:
              numberType = attributeValue;
              int index = lookupValue(attributeValue, NumberNames);
              if (DEBUG)
                logln("Getting number format for " + locale);
              switch (index) {
                case 0:
                  nf = NumberFormat.getInstance(locale);
                  break;
                case 1:
                  nf = NumberFormat.getIntegerInstance(locale);
                  break;
                case 2:
                  nf = NumberFormat.getNumberInstance(locale);
                  break;
                case 3:
                  nf = NumberFormat.getPercentInstance(locale);
                  break;
                case 4:
                  nf = NumberFormat.getScientificInstance(locale);
                  break;
                default:
                  nf = NumberFormat.getCurrencyInstance(locale);
                  nf.setCurrency(Currency.getInstance(attributeValue));
                  break;
              }
              break;
            default:
              throw new IllegalArgumentException("Unexpected Attribute Name: "
                  + attributeName);
          }

        }
        if (!approved) {
          return;
        }
        String temp = nf.format(v).trim();
        result = result.trim(); // HACK because of SAX
        if (!temp.equals(result)) {
          errln("Number: Locale: " + locale + ",\tType: " + numberType
              + ",\tCLDR: <" + result + ">, ICU: <" + temp + ">");
        }
      }
    });

    // ============ Handler for Dates ============
    addHandler("date", new Handler() {
      int dateFormat = 0;

      int timeFormat = 0;

      public void handleResult(ULocale locale, String result)
          throws ParseException {
        Date date = new Date();
        for (Iterator<AttributeName> it = settings.keySet().iterator(); it
            .hasNext();) {
          AttributeName attributeName = it.next();
          String attributeValue = settings.get(attributeName);
          switch (attributeName) {
            case input:
              date = iso.parse(attributeValue);
              continue;
            case dateType:
              dateFormat = lookupValue(attributeValue, DateFormatNames);
              break;
            case timeType:
              timeFormat = lookupValue(attributeValue, DateFormatNames);
              break;
            case draft:
              approved = attributeValue.contains("approved");
              break;
            default:
              throw new IllegalArgumentException("Unexpected Attribute Name: "
                  + attributeName);
          }
        }
        if (!approved) {
          return;
        }
        SimpleDateFormat dt = getDateFormat(locale, dateFormat, timeFormat);
        dt.setTimeZone(utc);
        String temp = dt.format(date).trim();
        result = result.trim(); // HACK because of SAX
        if (!temp.equals(result)) {
          errln("DateTime: Locale: " + locale + ",\tDate: "
              + DateFormatNames[dateFormat] + ",\tTime: "
              + DateFormatNames[timeFormat] + ",\tCLDR: <" + result
              + ">, ICU: <" + temp + ">");
        }
      }

      /**
       * 
       */
      private SimpleDateFormat getDateFormat(ULocale locale, int dateFormat,
          int timeFormat) {
        if (DEBUG)
          logln("Getting date/time format for " + locale);
        if (DEBUG && "ar_EG".equals(locale.toString())) {
          System.out.println("debug here");
        }
        DateFormat dt;
        if (dateFormat == 0) {
          dt = DateFormat.getTimeInstance(DateFormatValues[timeFormat], locale);
          if (DEBUG)
            System.out.print("getTimeInstance");
        } else if (timeFormat == 0) {
          dt = DateFormat.getDateInstance(DateFormatValues[dateFormat], locale);
          if (DEBUG)
            System.out.print("getDateInstance");
        } else {
          dt = DateFormat.getDateTimeInstance(DateFormatValues[dateFormat],
              DateFormatValues[timeFormat], locale);
          if (DEBUG)
            System.out.print("getDateTimeInstance");
        }
        if (DEBUG)
          System.out.println("\tinput:\t" + dateFormat + ", " + timeFormat
              + " => " + ((SimpleDateFormat) dt).toPattern());
        return (SimpleDateFormat) dt;
      }
    });

    // ============ Handler for Zones ============
    addHandler("zoneFields", new Handler() {
      String date = "";

      String zone = "";

      String parse = "";

      String pattern = "";

      public void handleResult(ULocale locale, String result)
          throws ParseException {
        for (Iterator<AttributeName> it = settings.keySet().iterator(); it
            .hasNext();) {
          AttributeName attributeName = it.next();
          String attributeValue = settings.get(attributeName);
          switch (attributeName) {
            case date:
              date = attributeValue;
              break;
            case field:
              pattern = attributeValue;
              break;
            case zone:
              zone = attributeValue;
              break;
            case parse:
              parse = attributeValue;
              break;
            case draft:
              approved = attributeValue.contains("approved");
              break;
          }
        }
        if (!approved) {
          return;
        }
        Date dateValue = iso.parse(date);
        SimpleDateFormat field = new SimpleDateFormat(pattern, locale);
        field.setTimeZone(TimeZone.getTimeZone(zone));
        String temp = field.format(dateValue).trim();
        // SKIP PARSE FOR NOW
        result = result.trim(); // HACK because of SAX
        if (!temp.equals(result)) {
          errln("Zone Format: Locale: " + locale + ", \tZone: " + zone
              + ", \tDate: " + date + ", \tField: " + pattern + ", \tCLDR: <"
              + result + ">, \tICU: <" + temp + ">");
        }
      }
    });
  }

  // ============ Gorp for SAX ============

  {
    try {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setValidating(true);
      SAX = factory.newSAXParser();
    } catch (Exception e) {
      throw new IllegalArgumentException("can't start");
    }
  }

  DefaultHandler DEFAULT_HANDLER = new DefaultHandler() {
    static final boolean DEBUG = false;

    StringBuffer lastChars = new StringBuffer();

    boolean justPopped = false;

    Handler handler;

    public void startElement(String uri, String localName, String qName,
        Attributes attributes) throws SAXException {
      //data.put(new ContextStack(contextStack), lastChars);
      //lastChars = "";
      try {
        if (qName.equals("cldrTest")) {
          // skip
        } else if (qName.equals("result")) {
          for (int i = 0; i < attributes.getLength(); ++i) {
            handler.set(AttributeName.valueOf(attributes.getQName(i)),
                attributes.getValue(i));
          }
        } else {
          handler = getHandler(qName, attributes);
          //handler.set("locale", uLocale.toString());
        }
        //if (DEBUG) logln("startElement:\t" + contextStack);
        justPopped = false;
      } catch (RuntimeException e) {
        e.printStackTrace();
        throw e;
      }
    }

    public void endElement(String uri, String localName, String qName)
        throws SAXException {
      try {
        //if (DEBUG) logln("endElement:\t" + contextStack);
        if (qName.equals("result"))
          handler.checkResult(lastChars.toString());
        else if (qName.length() != 0) {
          //logln("Unexpected contents of: " + qName + ", <" + lastChars + ">");
        }
        lastChars.setLength(0);
        justPopped = true;
      } catch (RuntimeException e) {
        e.printStackTrace();
        throw e;
      }
    }

    // Have to hack around the fact that the character data might be in pieces
    public void characters(char[] ch, int start, int length)
        throws SAXException {
      try {
        String value = new String(ch, start, length);
        if (DEBUG)
          logln("characters:\t" + value);
        lastChars.append(value);
        justPopped = false;
      } catch (RuntimeException e) {
        e.printStackTrace();
        throw e;
      }
    }

    // just for debugging

    public void notationDecl(String name, String publicId, String systemId)
        throws SAXException {
      logln("notationDecl: " + name + ", " + publicId + ", " + systemId);
    }

    public void processingInstruction(String target, String data)
        throws SAXException {
      logln("processingInstruction: " + target + ", " + data);
    }

    public void skippedEntity(String name) throws SAXException {
      logln("skippedEntity: " + name);
    }

    public void unparsedEntityDecl(String name, String publicId,
        String systemId, String notationName) throws SAXException {
      logln("unparsedEntityDecl: " + name + ", " + publicId + ", " + systemId
          + ", " + notationName);
    }

  };
}