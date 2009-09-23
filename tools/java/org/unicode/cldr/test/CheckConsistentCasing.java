package org.unicode.cldr.test;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.PrettyPath;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Status;

import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.util.ULocale;

import java.text.CharacterIterator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class CheckConsistentCasing extends CheckCLDR {

  // remember to add this class to the list in CheckCLDR.getCheckAll
  // to run just this test, on just locales starting with 'nl', use CheckCLDR with -fnl.* -t.*Currencies.*

  XPathParts parts = new XPathParts(); // used to parse out a path
  ULocale uLocale = null;
  BreakIterator breaker = null;
  private String locale;
  private CLDRFile resolvedCldrFileToCheck2;
  PrettyPath pretty = new PrettyPath();


  public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Map<String, String> options, List<CheckStatus> possibleErrors) {
    if (cldrFileToCheck == null) return this;
    super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
    locale = cldrFileToCheck.getLocaleID();

    resolvedCldrFileToCheck2 = getResolvedCldrFileToCheck();
    getSamples(resolvedCldrFileToCheck2);
    return this;
  }

  // If you don't need any file initialization or postprocessing, you only need this one routine
  public CheckCLDR handleCheck(String path, String fullPath, String value, Map<String, String> options, List<CheckStatus> result) {
    // it helps performance to have a quick reject of most paths
    if (fullPath == null) return this; // skip paths that we don't have
    //    if (resolvedCldrFileToCheck2.getStringValue(path) == null) {
    //      System.out.println("???");
    //    }

    String locale2 = getCldrFileToCheck().getSourceLocaleID(path, null);
    if (!locale2.equals(locale)) {
      //System.out.println(locale + "\t" + uLocale);
    } else if (value != null && value.length() > 0) {
      int index = getIndex(path);
      if (index >= 0) {
        checkConsistentCasing(index, path, fullPath, value, options, result);
      }
    }
    return this;
  }


  /**
   * The type of the first letter
   */
  enum FirstLetterType {upper, lower, other;
  public static FirstLetterType from(String s) {
    if (s == null || s.length() == 0) {
      return other;
    }
    int cp;
    for (int i = 0; i < s.length(); i += Character.charCount(cp)) {
      cp = s.codePointAt(i);
      int type = UCharacter.getType(cp);
      switch(type) {
      case UCharacter.LOWERCASE_LETTER: return lower;
      case UCharacter.UPPERCASE_LETTER:
      case UCharacter.TITLECASE_LETTER: return upper;
      case UCharacter.OTHER_LETTER: return other;
      }
    }
    return null;
  }
  }


  /**
   * These are the buckets we use for comparison. A path goes into the first bucket that matches.
   */
  String[][] typesICareAbout = {
          {"//ldml/localeDisplayNames/languages/language"},
          {"//ldml/localeDisplayNames/scripts/script"},
          {"//ldml/localeDisplayNames/territories/territory"},
          {"//ldml/localeDisplayNames/variants/variant"},

          {"//ldml/dates/calendars/calendar", "/months", "narrow"},
          {"//ldml/dates/calendars/calendar", "/months", "format"},
          {"//ldml/dates/calendars/calendar", "/months"},

          {"//ldml/dates/calendars/calendar", "/days", "narrow"},
          {"//ldml/dates/calendars/calendar", "/days", "format"},
          {"//ldml/dates/calendars/calendar", "/days"},

          {"//ldml/dates/calendars/calendar", "/eras", "narrow"},
          {"//ldml/dates/calendars/calendar", "/eras", "format"},
          {"//ldml/dates/calendars/calendar", "/eras"},

          {"//ldml/dates/calendars/calendar", "/quarters", "narrow"},
          {"//ldml/dates/calendars/calendar", "/quarters", "abbreviated"},
          {"//ldml/dates/calendars/calendar", "/quarters", "format"},
          {"//ldml/dates/calendars/calendar", "/quarters"},

          {"//ldml/dates/calendars/calendar", "/fields"},
          {"//ldml/dates/timeZoneNames/zone", "/exemplarCity"},
          {"//ldml/dates/timeZoneNames/zone", "/short"},
          {"//ldml/dates/timeZoneNames/zone"},
          {"//ldml/dates/timeZoneNames/metazone", "/commonlyUsed"},
          {"//ldml/dates/timeZoneNames/metazone", "/short"},
          {"//ldml/dates/timeZoneNames/metazone"},
          {"//ldml/numbers/currencies/currency", "/symbol"},
          {"//ldml/numbers/currencies/currency", "/displayName", "@count"},
          {"//ldml/numbers/currencies/currency", "/displayName"},
  };
  FirstLetterType[] types = new FirstLetterType[typesICareAbout.length];
  String[] sample = new String[typesICareAbout.length];
  String[] prettyPaths = new String[typesICareAbout.length];

  // //ldml/numbers/currencies/currency[@type="ADP"]/displayName
  // //ldml/numbers/currencies/currency[@type="RON"]/displayName[@count="other"]
  // //ldml/numbers/currencies/currency[@type="BYB"]/symbol

  int getIndex(String path) {
    main:
      for (int i = 0; i < typesICareAbout.length; ++i) {
        if (!path.startsWith(typesICareAbout[i][0])) {
          continue;
        }
        for (int j = 1; j < typesICareAbout[i].length; ++j) {
          if (!path.contains(typesICareAbout[i][j])) {
            continue main;
          }
        }
        return i;
      }
  return -1;
  }

  private void getSamples(CLDRFile unresolved) {
    for (int i = 0; i < typesICareAbout.length; ++i) {
      types[i] = null;
    }
    Set<String> items = new TreeSet<String>(CLDRFile.ldmlComparator);
    Iterator<String> it = unresolved.iterator();
    CollectionUtilities.addAll(it, items);
    unresolved.getExtraPaths(items);
    boolean isRoot = "root".equals(unresolved.getLocaleID());

    int count = typesICareAbout.length;
    for (String path : items) {
      //      if (path.contains("displayName") && path.contains("count")) {
      //        System.out.println("count");
      //      }
      if (!isRoot) {
        String locale2 = getCldrFileToCheck().getSourceLocaleID(path, null);
        if (locale2.equals("root") || locale2.equals("code-fallback")) {
          continue;
        }
      }
      // System.out.println(locale2 + "\t\t" + path);
      int i = getIndex(path);
      if (i >= 0 && types[i] == null) {
        String value = unresolved.getStringValue(path);
        if (value == null || value.length() == 0) continue;
        FirstLetterType ft = FirstLetterType.from(value);
        if (ft == null) continue;
        sample[i] = value;
        prettyPaths[i] = pretty.getPrettyPath(path, false);
        types[i] = ft;
        --count;
        if (count == 0) {
          break;
        }
      }
    }
  }

  private void checkConsistentCasing(int i, String path, String fullPath, String value,
          Map<String, String> options, List<CheckStatus> result) {
    //    if (path.contains("displayName") && path.contains("count")) {
    //      System.out.println("count");
    //    }
    FirstLetterType ft = FirstLetterType.from(value);
    if (ft != types[i] && ft != null) {
      result.add(new CheckStatus().setCause(this)
              .setMainType(CheckStatus.warningType)
              .setSubtype(Subtype.incorrectCasing) // typically warningType or errorType
              .setMessage("First letter case of <{0}>={1} doesn't match that of <{2}>={3} ({4}).", new Object[]{value, ft, sample[i], types[i], prettyPaths[i]})); // the message; can be MessageFormat with arguments
    }
  }
}