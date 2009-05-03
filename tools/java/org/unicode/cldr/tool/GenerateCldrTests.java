/*
 **********************************************************************
 * Copyright (c) 2002-2004, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;

import com.ibm.icu.dev.test.util.ArrayComparator;
import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.dev.test.util.UnicodeMap;
//import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateFormatSymbols;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.dev.test.util.SortedBag;
import com.ibm.icu.dev.tool.UOption;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.ICUServiceBuilder;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Log;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.TimezoneFormatter;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRFile.Factory;

//import org.unicode.cldr.tool.GenerateCldrDateTimeTests;

/**
 * Generated tests for CLDR.
 * @author medavis
 */

public class GenerateCldrTests {

  protected static final boolean METAZONES_WORK = false;

  //static private PrintWriter log;
  PrintWriter out;

  private static final int HELP1 = 0, HELP2 = 1, SOURCEDIR = 2, DESTDIR = 3,
      LOGDIR = 4, MATCH = 5, NOT_RESOLVED = 6, LANGUAGES = 7, TZADIR = 8,
      SHOW = 9;

  private static final UOption[] options = {
      UOption.HELP_H(),
      UOption.HELP_QUESTION_MARK(),
      UOption.SOURCEDIR().setDefault(Utility.COMMON_DIRECTORY),
      UOption.DESTDIR().setDefault(Utility.GEN_DIRECTORY + "test/"),
      UOption.create("log", 'l', UOption.REQUIRES_ARG).setDefault(
          Utility.GEN_DIRECTORY + "test/"),
      UOption.create("match", 'm', UOption.REQUIRES_ARG).setDefault(".*"),
      UOption.create("notresolved", 'n', UOption.NO_ARG),
      UOption.create("languages", 'g', UOption.NO_ARG),
      UOption.create("tzadir", 't', UOption.REQUIRES_ARG).setDefault(
          Utility.UTIL_DATA_DIR),
      // "C:\\ICU4J\\icu4j\\src\\com\\ibm\\icu\\dev\\tool\\cldr\\"),
      UOption.create("show", 's', UOption.NO_ARG), };

  GenerateCldrCollationTests cldrCollations;

  static String logDir = null, destDir = null;

  public static boolean hasLocalizedLanguageFor(ULocale locale,
      ULocale otherLocale) {
    String lang = otherLocale.getLanguage();
    String localizedVersion = otherLocale.getDisplayLanguage(locale);
    return !lang.equals(localizedVersion);
  }

  public static boolean hasLocalizedCountryFor(ULocale locale,
      ULocale otherLocale) {
    String country = otherLocale.getCountry();
    if (country.equals(""))
      return true;
    String localizedVersion = otherLocale.getDisplayCountry(locale);
    return !country.equals(localizedVersion);
  }

  public static void main(String[] args) throws Exception {
    double deltaTime = System.currentTimeMillis();
    UOption.parseArgs(args, options);
    Log.setLog(options[LOGDIR].value + "log.txt");
    //log = BagFormatter.openUTF8Writer(options[LOGDIR].value, "log.txt");
    try {
      if (options[LANGUAGES].doesOccur) {
        GenerateStatistics.generateSize(
            options[GenerateCldrTests.SOURCEDIR].value + "main/",
            options[GenerateCldrTests.LOGDIR].value, options[TZADIR].value,
            options[MATCH].value, true);
        return;
      }
      //compareAvailable();

      //if (true) return;
      //System.out.println(createCaseClosure(new UnicodeSet("[a{bc}{def}{oss}]")));
      //System.out.println(createCaseClosure(new UnicodeSet("[a-z\u00c3\u0178{aa}]")));
      GenerateCldrTests t = new GenerateCldrTests();
      //t.generate(new ULocale("hu"), null);
      t.generate(options[MATCH].value);
    } finally {
      Log.close();
      deltaTime = System.currentTimeMillis() - deltaTime;
      System.out.println("Elapsed: " + deltaTime / 1000.0 + " seconds");
      System.out.println("Done");
    }
  }

  /*
   private static void compareAvailable() {
   ULocale[] cols = Collator.getAvailableULocales();
   Locale[] alocs = NumberFormat.getAvailableLocales();
   Set sCols = filter(cols);
   Set sLocs = filter(alocs);
   Set oldSLocs = new TreeSet(sCols);
   sLocs.removeAll(sCols);
   log.println("main - collation");
   showLocales(sLocs);
   sCols.removeAll(oldSLocs);
   log.println();
   log.println("collation - main");
   showLocales(sCols);
   }
   */

  /**
   * 
   */
  /*	private static void checkLocaleNames() {
   ULocale[] locales = ULocale.getAvailableLocales();
   for (int i = 0; i < locales.length; ++i) {
   if (!hasLocalizedCountryFor(ULocale.ENGLISH, locales[i])
   || !hasLocalizedLanguageFor(ULocale.ENGLISH, locales[i])
   || !hasLocalizedCountryFor(locales[i], locales[i])
   || !hasLocalizedLanguageFor(locales[i], locales[i])) {
   Log.getLog().print("FAILURE\t");
   } else {
   Log.getLog().print("       \t");
   }
   Log.logln(locales[i] + "\t" + locales[i].getDisplayName(ULocale.ENGLISH) + "\t" + locales[i].getDisplayName(locales[i]));
   }
   }
   */
  /**
   * @param sLocs
   */
  private static void showLocales(Set sLocs) {
    for (Iterator it = sLocs.iterator(); it.hasNext();) {
      String s = (String) it.next();
      Log.logln(s + "\t" + ULocale.getDisplayLanguage(s, "en"));
    }
  }

  /**
   * @param cols
   * @return
   */
  private static Set filter(Object[] cols) {
    Set result = new TreeSet();
    for (int i = 0; i < cols.length; ++i) {
      String s = cols[i].toString();
      if (s.indexOf('_') >= 0)
        continue;
      result.add(s);
    }
    return result;
  }

  Set addULocales(Object[] objects, Set target) {
    for (int i = 0; i < objects.length; ++i) {
      target.add(new ULocale(objects[i].toString()));
    }
    return target;
  }

  LanguageTagParser ltp = new LanguageTagParser();

  private void addLocale(String locale) {
    String lang;
    try {
      lang = ltp.set(locale).getLanguage();
      if (lang.length() == 0)
        return; // skip root
    } catch (RuntimeException e) {
      return; // illegal locale name, must be supplemental
    }
    //ULocale parent = new ULocale(lang);
    //System.out.println(item + ", " + parent);
    parentToLocales.add(new ULocale(lang), new ULocale(locale));
    /*
     RuleBasedCollator col = cldrCollations.getInstance(item);
     if (col == null) {
     System.out.println("No collator for: " + item);
     }
     String rules = col.getRules(); // ((RuleBasedCollator)Collator.getInstance(item)).getRules();
     rulesToLocales.add(rules, item);
     localesToRules.put(item, rules);
     */
  }

  Set collationLocales = new TreeSet(ULocaleComparator); //  = addULocales(Collator.getAvailableULocales(), new TreeSet(ULocaleComparator));

  //Set numberLocales = addULocales(NumberFormat.getAvailableLocales(), new TreeSet(ULocaleComparator));
  //Set dateLocales = addULocales(DateFormat.getAvailableLocales(), new TreeSet(ULocaleComparator));
  Set allLocales = new TreeSet(ULocaleComparator);

  Map localesToRules = new HashMap();

  Relation.CollectionFactory cm = new Relation.CollectionMaker(
      ULocaleComparator);

  Relation rulesToLocales = new Relation(new TreeMap(), cm);

  Relation parentToLocales = new Relation(new TreeMap(ULocaleComparator), cm);

  /*    void getLocaleList() {
   collationLocales = new TreeSet(ULocaleComparator);
   collationLocales.addAll(cldrCollations.getAvailableSet());
   
   collationLocales = addULocales(new String[] { // HACK
   "ga",
   "nl",
   "pt",
   "de@collation=phonebook",
   "es@collation=traditional",
   "hi@collation=direct",
   "zh@collation=pinyin",
   "zh@collation=stroke",
   "zh@collation=traditional",
   }, collationLocales);
   
   allLocales.addAll(collationLocales);
   allLocales.addAll(numberLocales);
   allLocales.addAll(dateLocales);
   // HACK
   // get all collations with same rules
   
   for (Iterator it = allLocales.iterator(); it.hasNext();) {
   addLocale((ULocale) it.next());
   }
   
   String[] others = new String[] {
   "de@collation=phonebook",
   "es@collation=traditional",
   "hi@collation=direct",
   "zh@collation=pinyin",
   "zh@collation=stroke",
   "zh@collation=traditional",
   };
   for (int i = 0; i < others.length; ++i) {
   addLocale(new ULocale(others[i]));
   }
   
   }*/

  //GenerateCldrDateTimeTests cldrOthers;
  Factory mainCldrFactory;

  ICUServiceBuilder icuServiceBuilder;

  void generate(String pat) throws Exception {
    mainCldrFactory = Factory.make(options[SOURCEDIR].value + "main"
        + File.separator, pat);
    Factory collationCldrFactory = Factory.make(options[SOURCEDIR].value
        + "collation" + File.separator, pat);
    Factory supplementalCldrFactory = Factory.make(options[SOURCEDIR].value
        + "supplemental" + File.separator, ".*");

    allLocales.addAll(mainCldrFactory.getAvailable());
    allLocales.addAll(collationCldrFactory.getAvailable());

    cldrCollations = new GenerateCldrCollationTests(options[SOURCEDIR].value
        + "collation" + File.separator, pat, allLocales);
    if (options[SHOW].doesOccur)
      cldrCollations.show();

    collationLocales.addAll(allLocales);
    for (Iterator it = cldrCollations.getAvailableSet().iterator(); it
        .hasNext();) {
      collationLocales.add(it.next().toString());
    }
    // TODO HACK
    //collationLocales.remove("ar_IN");
    icuServiceBuilder = new ICUServiceBuilder();
    /*        cldrOthers = new GenerateCldrDateTimeTests(options[SOURCEDIR].value + "main" + File.separator, pat, 
     !options[GenerateCldrTests.NOT_RESOLVED].doesOccur);
     if (options[SHOW].doesOccur) cldrOthers.show();
     */
    //getLocaleList();
    for (Iterator it = collationLocales.iterator(); it.hasNext();) {
      addLocale((String) it.next());
    }

    Matcher m = Pattern.compile(pat).matcher("");
    for (Iterator it = parentToLocales.keySet().iterator(); it.hasNext();) {
      String p = it.next().toString();
      if (!m.reset(p).matches())
        continue;
      generate(new ULocale(p));
    }
  }

  private void generate(ULocale locale) throws Exception {
    out = BagFormatter.openUTF8Writer(options[DESTDIR].value, locale + ".xml");
    out.println("<?xml version='1.0' encoding='UTF-8' ?>");
    out
        .println("<!DOCTYPE cldrTest SYSTEM 'http://www.unicode.org/cldr/dtd/1.5/cldrTest.dtd'>");
    out.println("<!-- For information, see readme.html -->");
    out.println(" <cldrTest version='1.5' base='" + locale + "'>");
    out.println(" <!-- "
        + TransliteratorUtilities.toXML.transliterate(locale
            .getDisplayName(ULocale.ENGLISH)
            + " [" + locale.getDisplayName(locale)) + "] -->");
    generateItems(locale, allLocales, NumberShower);
    generateItems(locale, allLocales, DateShower);
    generateItems(locale, allLocales, ZoneFieldShower);
    generateItems(locale, collationLocales, CollationShower);
    out.println(" </cldrTest>");
    out.close();
    Utility.generateBat(options[SOURCEDIR].value + "test" + File.separator,
        locale + ".xml", options[DESTDIR].value, locale + ".xml",
        new Utility.SimpleLineComparator(0));
  }

  /*
   *
   // first pass through and get all the functional equivalents
   Map uniqueLocales = new TreeMap();
   
   String[] keywords = Collator.getKeywords();
   boolean [] isAvailable = new boolean[1];
   for (int i = 0; i < locales.length; ++i) {
   add(locales[i], uniqueLocales);
   if (true) continue; // TODO restore once Vladimir fixes
   for (int j = 0; j < keywords.length; ++j) {
   String[] values = Collator.getKeywordValues(keywords[j]);
   for (int k = 0; k < values.length; ++k) {
   // TODO -- for a full job, would do all combinations of different keywords!
   if (values[k].equals("standard")) continue;
   add(new ULocale(locales[i] + "@" + keywords[j] + "=" + values[k]), uniqueLocales);
   //ULocale other = Collator.getFunctionalEquivalent(keywords[j], locales[i], isAvailable);
   }
   }
   }
   for (int i = 0; i < extras.length; ++i) {
   add(new ULocale(extras[i]), uniqueLocales);
   }
   // items are now sorted by rules. So resort by locale
   Map toDo = new TreeMap(ULocaleComparator);
   for (Iterator it = uniqueLocales.keySet().iterator(); it.hasNext();) {
   Object rules = it.next();
   Set s = (Set) uniqueLocales.get(rules);
   ULocale ulocale = (ULocale) s.iterator().next(); // get first one
   toDo.put(ulocale, s);
   }
   for (Iterator it = toDo.keySet().iterator(); it.hasNext();) {
   ULocale ulocale = (ULocale) it.next();
   Set s = (Set) toDo.get(ulocale);
   generate(ulocale);
   }
   */

  /**
   * add locale into list. Replace old if shorter
   * @param locale
   */
  void add(ULocale locale, Map uniqueLocales) {
    try {
      RuleBasedCollator col = cldrCollations.getInstance(locale); // (RuleBasedCollator) Collator.getInstance(locale);
      // for our purposes, separate locales if we are using different exemplars
      String key = col.getRules() + "\uFFFF" + getExemplarSet(locale, 0, DraftStatus.unconfirmed);
      Set s = (Set) uniqueLocales.get(key);
      if (s == null) {
        s = new TreeSet(ULocaleComparator);
        uniqueLocales.put(key, s);
      }
      System.out.println("Adding " + locale);
      s.add(locale);
    } catch (Throwable e) { // skip
      System.out.println("skipped " + locale);
    }
  }

  /**
   * Work-around
   */
  public UnicodeSet getExemplarSet(ULocale locale, int options,
      DraftStatus minimalDraftStatus) {
    String n = locale.toString();
    int pos = n.indexOf('@');
    if (pos >= 0)
      locale = new ULocale(n.substring(0, pos));
    CLDRFile cldrFile = mainCldrFactory.make(locale.toString(), true,
        minimalDraftStatus);
    String v = cldrFile.getStringValue("//ldml/characters/exemplarCharacters");
    UnicodeSet result = new UnicodeSet(v);
    v = cldrFile
        .getStringValue("//ldml/characters/exemplarCharacters[@type=\"auxiliary\"]");
    if (v != null) {
      result.addAll(new UnicodeSet(v));
    }
    if (options == 0)
      result.closeOver(UnicodeSet.CASE);
    return result;
  }

  public static final Comparator ULocaleComparator = new Comparator() {
    public int compare(Object o1, Object o2) {
      return o1.toString().compareTo(o2.toString());
    }
  };

  /*    public interface Equator {
   public boolean equals(Object o1, Object o2);
   }
   */
  static boolean intersects(Collection a, Collection b) {
    for (Iterator it = a.iterator(); it.hasNext();) {
      if (b.contains(it.next()))
        return true;
    }
    return false;
  }

  /*    static Collection extract(Object x, Collection a, Equator e, Collection output) {
   List itemsToRemove = new ArrayList();
   for (Iterator it = a.iterator(); it.hasNext();) {
   Object item = it.next();
   if (e.equals(x, item)) {
   itemsToRemove.add(item); // have to do this because iterator may not allow
   output.add(item);
   }
   }
   a.removeAll(itemsToRemove);
   return output;
   }
   */
  class ResultsPrinter {
    private Set listOfSettings = new LinkedHashSet();

    private transient LinkedHashMap settings = new LinkedHashMap();

    ResultsPrinter() {
    }

    ResultsPrinter(ResultsPrinter rpIncludeDraft, ResultsPrinter rpNoDraft) {
      Set listOfSettings1 = rpIncludeDraft.getListOfSettings();
      Set listOfSettings2 = rpNoDraft.getListOfSettings();
      if (listOfSettings1.size() != listOfSettings2.size()) {
        throw new InternalError("can't combine");
      }
      Iterator it1 = listOfSettings1.iterator();
      Iterator it2 = listOfSettings2.iterator();
      while (it1.hasNext()) {
        Map settings1 = (Map) it1.next();
        Map settings2 = (Map) it2.next();
        if (settings1.equals(settings2)) {
          settings1.put("draft", "unconfirmed approved");
          addToListOfSettings(settings1);
        } else {
          // they should only differ by result!
          settings1.put("draft", "unconfirmed");
          addToListOfSettings(settings1);
          settings2.put("draft", "approved");
          addToListOfSettings(settings2);
        }
      }
    }

    private void addToListOfSettings(Map settings1) {
      for (Object key : settings1.keySet()) {
        if (key == null || settings1.get(key) == null) {
          throw new IllegalArgumentException("null key or value in settings.");
        }
      }
      listOfSettings.add(settings1);
    }

    void set(String name, String value) {
      if (name == null || value == null) {
        throw new IllegalArgumentException("null key or value in settings.");
      }
      settings.put(name, value);
    }

    void setResult(String result) {
      if (result == null) {
        throw new IllegalArgumentException("null key or value in settings.");
      }
      settings.put("result", result);
      addToListOfSettings((Map)settings.clone());
    }

    void print() {
      Map oldSettings = new TreeMap();
      for (Iterator it2 = getListOfSettings().iterator(); it2.hasNext();) {
        Map settings = (Map) it2.next();
        String result = (String) settings.get("result");
        out.print("   <result");
        for (Iterator it = settings.keySet().iterator(); it.hasNext();) {
          Object key = it.next();
          if (key.equals("result"))
            continue;
          Object value = settings.get(key);
          if (!value.equals(oldSettings.get(key))) {
            out.print(" " + key + "='"
                + TransliteratorUtilities.toXML.transliterate(value.toString())
                + "'");
          }
        }
        out.println(">" + TransliteratorUtilities.toXML.transliterate(result)
            + "</result>");
        oldSettings = settings;
      }
    }

    public boolean equals(Object other) {
      try {
        ResultsPrinter that = (ResultsPrinter) other;
        return getListOfSettings().equals(that.getListOfSettings());
      } catch (Exception e) {
        return false;
      }
    }

    public int hashCode() {
      throw new IllegalArgumentException();
    }
    /**
     * 
     */

    private void setListOfSettings(Set listOfSettings) {
      this.listOfSettings = listOfSettings;
    }

    private Set getListOfSettings() {
      return Collections.unmodifiableSet(listOfSettings);
    }
  }

  abstract class DataShower {
    abstract ResultsPrinter show(ULocale first, DraftStatus minimalDraftStatus)
        throws Exception;

    ResultsPrinter show(ULocale first) throws Exception {
      ResultsPrinter rpIncludeDraft = show(first, DraftStatus.unconfirmed);
      ResultsPrinter rpNoDraft = show(first, DraftStatus.approved);
      return new ResultsPrinter(rpIncludeDraft, rpNoDraft);
    }

    abstract String getElement();
  }

  interface DataShower2 {
    void show(ULocale first, Collection others) throws Exception;
  }

  private void generateItems(ULocale locale, Collection onlyLocales,
      DataShower generator) throws Exception {
    Collection sublocales = new TreeSet(ULocaleComparator);
    sublocales.add(locale);
    parentToLocales.get(locale, sublocales);
    sublocales.retainAll(onlyLocales);
    Map locale_results = new TreeMap(ULocaleComparator);
    for (Iterator it = sublocales.iterator(); it.hasNext();) {
      ULocale current = (ULocale) it.next();
      locale_results.put(current, generator.show(current));
    }
    // do it this way so that the locales stay in order
    Set matchingLocales = new TreeSet(ULocaleComparator);
    while (sublocales.size() != 0) {
      ULocale first = (ULocale) sublocales.iterator().next();
      ResultsPrinter r = (ResultsPrinter) locale_results.get(first);
      for (Iterator it = sublocales.iterator(); it.hasNext();) {
        ULocale other = (ULocale) it.next();
        ResultsPrinter r2 = (ResultsPrinter) locale_results.get(other);
        if (r2.equals(r))
          matchingLocales.add(other);
      }
      showLocales(generator.getElement(), matchingLocales);
      r.print();
      out.println("  </" + generator.getElement() + ">");
      sublocales.removeAll(matchingLocales);
      matchingLocales.clear();
    }
    Comparator c;
  }

  public void showLocales(String elementName, Collection others) {
    //System.out.println(elementName + ": " + locale);
    out.println("  <" + elementName + " ");
    StringBuffer comment = new StringBuffer();
    if (others != null && others.size() != 0) {
      out.print("locales='");
      boolean first = true;
      for (Iterator it = others.iterator(); it.hasNext();) {
        if (first)
          first = false;
        else {
          out.print(" ");
          comment.append("; ");
        }
        ULocale loc = (ULocale) it.next();
        out.print(loc);
        comment.append(loc.getDisplayName(ULocale.ENGLISH) + " ["
            + loc.getDisplayName(loc) + "]");
      }
      out.print("'");
    }
    out.println(">");
    out.println("<!-- "
        + TransliteratorUtilities.toXML.transliterate(comment.toString())
        + " -->");
  }

  DataShower ZoneFieldShower = new DataShower() {
    StandardCodes sc = StandardCodes.make();

    //Set zones = new TreeSet(sc.getAvailableCodes("tzid"));
    List zones = Arrays.asList(new String[] { "America/Los_Angeles",
        "America/Argentina/Buenos_Aires", "America/Buenos_Aires",
        "America/Havana", "Australia/ACT", "Australia/Sydney", "Europe/London",
        "Europe/Moscow", "Etc/GMT+3" });

    String[] perZoneSamples = { "Z", "ZZZZ", "z", "zzzz", "v", "vvvv", "V", "VVVV" };

    String[] dates = { "2004-01-15T12:00:00Z", "2004-07-15T12:00:00Z" };

    public ResultsPrinter show(ULocale first, DraftStatus minimalDraftStatus)
        throws Exception {
      TimezoneFormatter tzf = new TimezoneFormatter(mainCldrFactory, first
              .toString(), minimalDraftStatus);
          ResultsPrinter rp = new ResultsPrinter();
      if (!METAZONES_WORK) {
        return rp;
      }
      // TODO Auto-generated method stub
      ParsePosition parsePosition = new ParsePosition(0);
      for (Iterator it = zones.iterator(); it.hasNext();) {
        String tzid = (String) it.next();
        rp.set("zone", tzid);
        for (int j = 0; j < dates.length; ++j) {
          String date = dates[j];
          Date datetime = ICUServiceBuilder.isoDateParse(date);
          rp.set("date", dates[j]);
          for (int i = 0; i < perZoneSamples.length; ++i) {
            try {
              String pattern = perZoneSamples[i];
              if (!METAZONES_WORK && (pattern.contains("z") || pattern.contains("V"))) {
                continue;
              }
              rp.set("field", pattern);
              String formatted = tzf.getFormattedZone(tzid, pattern, datetime.getTime(), false);
              parsePosition.setIndex(0);
              String parsed = tzf.parse(formatted, parsePosition);
              if (parsed == null) {
                // for debugging
                formatted = tzf.getFormattedZone(tzid, pattern, datetime.getTime(), false);
                parsePosition.setIndex(0);
                parsed = tzf.parse(formatted, parsePosition);
              }
              rp.set("parse", parsed);
              rp.setResult(formatted);
            } catch (RuntimeException e) {
              throw (IllegalArgumentException) new IllegalArgumentException(
                  "Failure in " + first).initCause(e);
            }
          }
        }
      }
      return rp;
      /*
       *               Date datetime = ICUServiceBuilder.isoDateParse(samples[j]);
       rp.set("input", ICUServiceBuilder.isoDateFormat(datetime));
       
       */
    }

    public String getElement() {
      return "zoneFields";
    }
  };

  DataShower DateShower = new DataShower() {
    public ResultsPrinter show(ULocale locale, DraftStatus minimalDraftStatus)
        throws ParseException {
      String[] samples = { "1900-01-31T00:00:00Z", "1909-02-28T00:00:01Z",
          "1918-03-26T00:59:59Z", "1932-04-24T01:00:00Z",
          "1945-05-20T01:00:01Z", "1952-06-18T11:59:59Z",
          "1973-07-16T12:00:00Z", "1999-08-14T12:00:01Z",
          "2000-09-12T22:59:59Z", "2001-10-08T23:00:00Z",
          "2004-11-04T23:00:01Z", "2010-12-01T23:59:59Z", };
      CLDRFile cldrFile = mainCldrFactory.make(locale.toString(), true,
          minimalDraftStatus);
      icuServiceBuilder.setCldrFile(cldrFile);
      ResultsPrinter rp = new ResultsPrinter();
      for (int j = 0; j < samples.length; ++j) {
        Date datetime = ICUServiceBuilder.isoDateParse(samples[j]);
        rp.set("input", ICUServiceBuilder.isoDateFormat(datetime));
        for (int i = 0; i < ICUServiceBuilder.LIMIT_DATE_FORMAT_INDEX; ++i) {
          rp.set("dateType", icuServiceBuilder.getDateNames(i));
          for (int k = 0; k < ICUServiceBuilder.LIMIT_DATE_FORMAT_INDEX; ++k) {
            if (i == 0 && k == 0)
              continue;
            DateFormat df = icuServiceBuilder.getDateFormat("gregorian", i, k);
            String pattern = ((SimpleDateFormat)df).toPattern();
            if (!METAZONES_WORK && (pattern.contains("z") || pattern.contains("V"))) {
              continue;
            }
            rp.set("timeType", icuServiceBuilder.getDateNames(k));
            if (false && i == 2 && k == 0) {
              System.out.println("debug: date "
                  + icuServiceBuilder.getDateNames(i) + ", time "
                  + icuServiceBuilder.getDateNames(k) + " = "
                  + df.format(datetime));
            }
            rp.setResult(df.format(datetime));
          }
        }
      }
      return rp;
    }

    public String getElement() {
      return "date";
    }
  };

  DataShower NumberShower = new DataShower() {
    public ResultsPrinter show(ULocale locale, DraftStatus minimalDraftStatus)
        throws ParseException {
      CLDRFile cldrFile = mainCldrFactory.make(locale.toString(), true,
          minimalDraftStatus);
      icuServiceBuilder.setCldrFile(cldrFile);

      double[] samples = { 0, 0.01, -0.01, 1, -1, 123.456, -123.456, 123456.78,
          -123456.78, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
          Double.NaN };
      ResultsPrinter rp = new ResultsPrinter();
      for (int j = 0; j < samples.length; ++j) {
        double sample = samples[j];
        rp.set("input", String.valueOf(sample));
        for (int i = 0; i < ICUServiceBuilder.LIMIT_NUMBER_INDEX; ++i) {
          rp.set("numberType", icuServiceBuilder.getNumberNames(i));
          DecimalFormat nf = icuServiceBuilder.getNumberFormat(i);
          String formatted = nf.format(sample);
          if (formatted.indexOf("NaNNaN") >= 0) {
            formatted = nf.format(sample); // for debugging
          }
          rp.setResult(formatted);
        }
      }
      return rp;
    }

    public String getElement() {
      return "number";
    }
  };

  // ========== COLLATION ==========

  /*    Equator CollationEquator = new Equator() {
   *//**
   * Must both be ULocales
   */
  /*
   public boolean equals(Object o1, Object o2) {
   try {
   ULocale loc1 = (ULocale) o1;
   ULocale loc2 = (ULocale) o2;
   if (loc1.equals(loc2)) return true;
   return cldrCollations.getInstance(loc1).equals(cldrCollations.getInstance(loc2)); 
   } catch (RuntimeException e) {
   System.out.println("Failed on: " + o1 + " ;\t" + o2);
   throw e;
   }        
   }
   };
   */static ULocale zhHack = new ULocale("zh"); // FIXME hack for zh

  DataShower CollationShower = new DataShower() {
    public ResultsPrinter show(ULocale locale, DraftStatus minimalDraftStatus) {
      //if (locale.equals(zhHack)) return;

      Collator col = cldrCollations.getInstance(locale); // Collator.getInstance(locale);

      UnicodeSet tailored = new UnicodeSet();
      if (col != null) {
        tailored = col.getTailoredSet();
        if (locale.getLanguage().equals("zh")) {
          tailored.addAll(new UnicodeSet("[[a-z]-[v]]"));
          Log.logln("HACK for Pinyin");
        }
        tailored = createCaseClosure(tailored);
        tailored = nfc(tailored);
      } else {
        System.out.println("No collation for: " + locale);
        col = cldrCollations.getInstance(ULocale.ROOT);
      }
      //System.out.println(tailored.toPattern(true));

      UnicodeSet exemplars = getExemplarSet(locale, UnicodeSet.CASE,
          minimalDraftStatus);
      // add all the exemplars

      exemplars = createCaseClosure(exemplars);
      exemplars = nfc(exemplars);
      //System.out.println(exemplars.toPattern(true));
      tailored.addAll(exemplars);
      //UnicodeSet tailoredMinusHan = new
      // UnicodeSet(tailored).removeAll(SKIP_COLLATION_SET);
      if (!exemplars.containsAll(tailored)) {
        //BagFormatter bf = new BagFormatter();
        Log.logln("In Tailored, but not Exemplar; Locale: " + locale + "\t"
            + locale.getDisplayName());
        Log.logln(new UnicodeSet(tailored).removeAll(exemplars)
            .toPattern(false));
        //bf.(log,"tailored", tailored, "exemplars", exemplars);
        Log.getLog().flush();
      }
      tailored.addAll(new UnicodeSet("[\\ .02{12}]"));
      tailored.removeAll(SKIP_COLLATION_SET);

      SortedBag bag = new SortedBag(col);
      return doCollationResult(col, tailored, bag);
    }

    public String getElement() {
      return "collation";
    }
  };

  /*
   public void show(ULocale locale, Collection others) {
   showLocales("collation", others);
   
   Collator col = cldrCollations.getInstance(locale); // Collator.getInstance(locale);
   
   UnicodeSet tailored = col.getTailoredSet();
   if (locale.getLanguage().equals("zh")) {
   tailored.addAll(new UnicodeSet("[[a-z]-[v]]"));
   log.println("HACK for Pinyin");
   }
   tailored = createCaseClosure(tailored);
   tailored = nfc(tailored);
   //System.out.println(tailored.toPattern(true));
   
   UnicodeSet exemplars = getExemplarSet(locale, UnicodeSet.CASE);
   // add all the exemplars
   if (false) for (Iterator it = others.iterator(); it.hasNext(); ) {
   exemplars.addAll(getExemplarSet((ULocale)it.next(), UnicodeSet.CASE));
   }
   
   exemplars = createCaseClosure(exemplars);
   exemplars = nfc(exemplars);
   //System.out.println(exemplars.toPattern(true));
   tailored.addAll(exemplars);
   //UnicodeSet tailoredMinusHan = new UnicodeSet(tailored).removeAll(SKIP_COLLATION_SET);
   if (!exemplars.containsAll(tailored)) {
   //BagFormatter bf = new BagFormatter();
   log.println("In Tailored, but not Exemplar; Locale: " + locale + "\t" + locale.getDisplayName());
   log.println(new UnicodeSet(tailored).removeAll(exemplars).toPattern(false));
   //bf.(log,"tailored", tailored, "exemplars", exemplars);
   log.flush();
   }
   tailored.addAll(new UnicodeSet("[\\ .02{12}]"));
   tailored.removeAll(SKIP_COLLATION_SET);
   
   SortedBag bag = new SortedBag(col);
   doCollationResult(col, tailored, bag);
   out.println("  </collation>");
   }};
   */
  static final UnicodeSet SKIP_COLLATION_SET = new UnicodeSet(
      "[[:script=han:][:script=hangul:]-[\u4e00-\u4eff \u9f00-\u9fff \uac00-\uacff \ud700-\ud7ff]]");

  /**
   * @param col
   * @param tailored
   * @param bag
   */
  private ResultsPrinter doCollationResult(Collator col, UnicodeSet tailored,
      SortedBag bag) {
    for (UnicodeSetIterator usi = new UnicodeSetIterator(tailored); usi.next();) {
      String s = usi.getString();
      bag.add('x' + s);
      bag.add('X' + s);
      bag.add('x' + s + 'x');
    }
    //out.println("   <set locale='" + locale + "'/>");
    /*
     if (others != null) for (Iterator it = others.iterator(); it.hasNext(); ) {
     ULocale uloc = (ULocale) it.next();
     if (uloc.equals(locale)) continue;
     out.println("   <other locale='" + uloc + "'/>");
     }
     */
    String last = "";
    boolean needEquals = false;
    StringBuffer tempResult = new StringBuffer(Utility.LINE_SEPARATOR);
    for (Iterator it = bag.iterator(); it.hasNext();) {
      String s = (String) it.next();
      if (col.compare(s, last) != 0) {
        if (needEquals)
          tempResult.append(last).append(Utility.LINE_SEPARATOR);
        needEquals = false;
        last = s;
      } else {
        needEquals = true;
      }
      tempResult.append(TransliteratorUtilities.toXML.transliterate(s)).append(
          Utility.LINE_SEPARATOR);
    }
    ResultsPrinter result = new ResultsPrinter();
    result.setResult(tempResult.toString());
    return result;
  }

  static public Set getMatchingXMLFiles(String dir, String localeRegex) {
    Matcher m = Pattern.compile(localeRegex).matcher("");
    Set s = new TreeSet();
    File[] files = new File(dir).listFiles();
    for (int i = 0; i < files.length; ++i) {
      String name = files[i].getName();
      if (!name.endsWith(".xml"))
        continue;
      if (name.startsWith("supplementalData"))
        continue;
      String locale = name.substring(0, name.length() - 4); // drop .xml
      if (!locale.equals("root") && !m.reset(locale).matches())
        continue;
      s.add(locale);
    }
    return s;
  }

  /*    public static boolean isDraft(Node node) {
   for (; node.getNodeType() != Node.DOCUMENT_NODE; node = node.getParentNode()){
   NamedNodeMap attributes = node.getAttributes();
   if (attributes == null) continue;
   for (int i = 0; i < attributes.getLength(); ++i) {
   Node attribute = attributes.item(i);
   if (attribute.getNodeName().equals("draft") && attribute.getNodeValue().equals("true")) return true;
   }
   }
   return false;
   }
   */
  public static String getXPath(Node node) {
    StringBuffer xpathFragment = new StringBuffer();
    StringBuffer xpath = new StringBuffer();
    for (; node.getNodeType() != Node.DOCUMENT_NODE; node = node
        .getParentNode()) {
      xpathFragment.setLength(0);
      xpathFragment.append('/').append(node.getNodeName());
      NamedNodeMap attributes = node.getAttributes();
      if (attributes != null) {
        for (int i = 0; i < attributes.getLength(); ++i) {
          Node attribute = attributes.item(i);
          xpathFragment.append("[@").append(attribute.getNodeName())
              .append('=').append(attribute.getNodeValue()).append(']');
        }
      }
      xpath.insert(0, xpathFragment);
    }
    xpath.insert(0, '/');
    return xpath.toString();
  }

  public static String getParent(String locale) {
    int pos = locale.lastIndexOf('_');
    if (pos >= 0) {
      return locale.substring(0, pos);
    }
    if (!locale.equals("root"))
      return "root";
    return null;
  }

  public static String replace(String source, String pattern, String replacement) {
    // dumb code for now
    for (int pos = source.indexOf(pattern, 0); pos >= 0; pos = source.indexOf(
        pattern, pos + 1)) {
      source = source.substring(0, pos) + replacement
          + source.substring(pos + pattern.length());
    }
    return source;
  }

  public static interface Apply {
    String apply(String source);
  }

  static UnicodeSet apply(UnicodeSet source, Apply apply) {
    UnicodeSet target = new UnicodeSet();
    for (UnicodeSetIterator usi = new UnicodeSetIterator(source); usi.next();) {
      String s = usi.getString();
      target.add(apply.apply(s));
    }
    return target;
  }

  static UnicodeSet nfc(UnicodeSet source) {
    return apply(source, new Apply() {
      public String apply(String source) {
        return Normalizer.compose(source, false);
      }
    });
  }

  public static interface CloseCodePoint {
    /**
     * @param cp code point to get closure for
     * @param toAddTo Unicode set for the closure
     * @return toAddTo (for chaining)
     */
    UnicodeSet close(int cp, UnicodeSet toAddTo);
  }

  public static UnicodeSet createCaseClosure(UnicodeSet source) {
    UnicodeSet target = new UnicodeSet();
    for (UnicodeSetIterator usi = new UnicodeSetIterator(source); usi.next();) {
      String s = usi.getString();
      UnicodeSet temp = createClosure(s, CCCP);
      if (temp == null)
        target.add(s);
      else
        target.addAll(temp);
    }
    return target;
  }

  public static class UnicodeSetComparator implements Comparator {
    UnicodeSetIterator ait = new UnicodeSetIterator();

    UnicodeSetIterator bit = new UnicodeSetIterator();

    public int compare(Object o1, Object o2) {
      if (o1 == o2)
        return 0;
      if (o1 == null)
        return -1;
      if (o2 == null)
        return 1;
      UnicodeSet a = (UnicodeSet) o1;
      UnicodeSet b = (UnicodeSet) o2;
      if (a.size() != b.size()) {
        return a.size() < b.size() ? -1 : 1;
      }
      ait.reset(a);
      bit.reset(b);
      while (ait.nextRange()) {
        bit.nextRange();
        if (ait.codepoint != bit.codepoint) {
          return ait.codepoint < bit.codepoint ? -1 : 1;
        }
        if (ait.codepoint == UnicodeSetIterator.IS_STRING) {
          int result = ait.string.compareTo(bit.string);
          if (result != 0)
            return result;
        } else if (ait.codepointEnd != bit.codepointEnd) {
          return ait.codepointEnd < bit.codepointEnd ? -1 : 1;
        }
      }
      return 0;
    }
  }

  public static final CloseCodePoint CCCP = new CloseCodePoint() {
    Locale locale = Locale.ENGLISH;

    UnicodeSet NONE = new UnicodeSet();

    UnicodeMap map = new UnicodeMap(); // new UnicodeSetComparator()

    public UnicodeSet close(int cp, UnicodeSet toAddTo) {
      UnicodeSet result = (UnicodeSet) map.getValue(cp);
      if (result == null) {
        result = new UnicodeSet();
        result.add(cp);
        String s = UCharacter.toLowerCase(locale, UTF16.valueOf(cp));
        result.add(s);
        s = UCharacter.toUpperCase(locale, UTF16.valueOf(cp));
        result.add(s);
        s = UCharacter.toTitleCase(locale, UTF16.valueOf(cp), null);
        result.add(s);
        // special hack
        if (result.contains("SS"))
          result.add("sS").add("ss");
        if (result.size() == 1)
          result = NONE;
        map.put(cp, result);
      }
      if (result != NONE)
        toAddTo.addAll(result);
      else
        toAddTo.add(cp);
      return toAddTo;
    }
  };

  public static UnicodeSet createClosure(String source, CloseCodePoint closer) {
    return createClosure(source, 0, closer);
  }

  public static UnicodeSet createClosure(String source, int position,
      CloseCodePoint closer) {
    UnicodeSet result = new UnicodeSet();
    // if at end, return empty set
    if (position >= source.length())
      return result;
    int cp = UTF16.charAt(source, position);
    // if last character, return its set
    int endPosition = position + UTF16.getCharCount(cp);
    if (endPosition >= source.length())
      return closer.close(cp, result);
    // otherwise concatenate its set with the remainder
    UnicodeSet remainder = createClosure(source, endPosition, closer);
    return createAppend(closer.close(cp, result), remainder);
  }

  /**
   * Produce the result of appending each element of this to each element of other.
   * That is, [a{cd}] + [d{ef}] => [{ad}{aef}{cdd}{cdef}]
   */
  public static UnicodeSet createAppend(UnicodeSet a, UnicodeSet b) {
    UnicodeSet target = new UnicodeSet();
    for (UnicodeSetIterator usi = new UnicodeSetIterator(a); usi.next();) {
      String s = usi.getString();
      for (UnicodeSetIterator usi2 = new UnicodeSetIterator(b); usi2.next();) {
        String s2 = usi2.getString();
        target.add(s + s2);
      }
    }
    return target;
  }
}