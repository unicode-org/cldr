package org.unicode.cldr.util;

import org.unicode.cldr.util.Dictionary.DictionaryBuilder;
import org.unicode.cldr.util.Dictionary.Matcher;
import org.unicode.cldr.util.Dictionary.Matcher.Filter;
import org.unicode.cldr.util.Dictionary.Matcher.Status;
import org.unicode.cldr.util.LenientDateParser.Token.Type;
import org.unicode.cldr.util.SupplementalDataInfo.BasicLanguageData;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;

import com.ibm.icu.dev.test.util.ArrayComparator;
import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateFormatSymbols;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.DateTimePatternGenerator.FormatParser;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.SimpleTimeZone;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

import java.sql.Time;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Immutable class that will parse dates and times for a particular ULocale.
 * @author markdavis
 */
public  class LenientDateParser {
  public static  boolean DEBUG = false;

  private static final UnicodeSet disallowedInSeparator = (UnicodeSet) new UnicodeSet("[:alphabetic:]").freeze();
  private static final EnumSet<Type> dateTypes = EnumSet.of(Type.DAY, Type.MONTH, Type.YEAR, Type.WEEKDAY, Type.ERA);
  private static final EnumSet<Type> timeTypes = EnumSet.of(Type.HOUR, Type.MINUTE, Type.SECOND, Type.AMPM, Type.TIMEZONE);
  private static final EnumSet<Type> integerDateTypes = EnumSet.of(Type.DAY, Type.MONTH, Type.YEAR);
  private static final EnumSet<Type> integerTimeTypes = EnumSet.of(Type.HOUR, Type.MINUTE, Type.SECOND);

  public class Parser {
    final List<Token> tokens = new ArrayList<Token>();
    final SoFar haveSoFar = new SoFar();
    Token previous;
    final BreakIterator breakIterator;
    Calendar calendar;
    private int twoDigitYearOffset;
    {
      set2DigitYearStart(new Date(new Date().getYear()-80,1,1));
    }
    
   Parser(BreakIterator breakIterator) {
      this.breakIterator = breakIterator;
    }

    public void parse(String text, Calendar cal, ParsePosition parsePosition) {
      calendar = cal;
      parse(new CharUtilities.CharListWrapper(text), parsePosition);
    }
    
    private boolean addSeparator(StringBuilder separatorBuffer) {
      // for now, disallow arbitrary separators
      return false;
//      if (separatorBuffer.length() != 0) {
//        tokens.add(new Token<String>(trim(separatorBuffer.toString()), Type.OTHER));
//        separatorBuffer.setLength(0);
//      }
    }
    
    boolean addToken(Token token) {
      if (haveSoFar.contains(token.getType())) {
        if (DEBUG) {
          System.out.println("Already have: " + token.getType());
        }
        return false;
      }
      switch (token.getType()) {
        case ERA:  case MONTH: case WEEKDAY: case TIMEZONE: case AMPM:
          if (!token.checkAllowableTypes(previous, haveSoFar, tokens)) {
            return false;
          }
          break;
        case INTEGER:
          if (!token.checkAllowableTypes(previous, haveSoFar, tokens)) {
            return false;
          }
          break;
        case SEPARATOR:
          EnumSet<Type> beforeTypes = ((SeparatorToken) token).getAllowsBefore();
          // see if there is a restriction on the previous type
          if (tokens.size() > 0) {
            if (!beforeTypes.contains(previous.getType())) {
              if (DEBUG) {
                System.out.println("Have " + token + ", while previous token is  " + previous);
              }
              return false;
            }
          }
          if (previous != null && previous.getType() == Type.INTEGER) {
            IntegerToken integerToken = (IntegerToken) previous;
            if (!integerToken.restrictAndSetCalendarFieldIfPossible(beforeTypes, haveSoFar, tokens)) {
              return false; // couldn't add
            }
          }
          // see what first required type is
          haveSoFar.setFirstType(beforeTypes);
          EnumSet<Type> afterTypes = ((SeparatorToken) token).getAllowsAfter();
          haveSoFar.setFirstType(afterTypes);

          break;
      }
      tokens.add(token);
      previous = token;
      return true;
    }

    private boolean checkPreviousType(Token token) {
      if (tokens.size() > 0) {
        Token previous = tokens.get(tokens.size() - 1);
        if (previous.getType() == Type.SEPARATOR) {
          Set<Type> allowable =  ((SeparatorToken) previous).getAllowsBefore();
          if (!allowable.contains(token.getType())) {
            if (DEBUG) {
              System.out.println("Have " + token + ", while previous token is " + previous);
            }
            return false;
          }
        }
      }
      return true;
    }

    //Type firstType;
    
    public Date parse(CharList charlist,  ParsePosition parsePosition) {
      calendar.clear();
      tokens.clear();
      previous = null;
      haveSoFar.clear();
      parsePosition.setErrorIndex(-1);
      StringBuilder separatorBuffer = new StringBuilder();
      matcher.setText(charlist);
      breakIterator.setText(charlist.toString());
      
      boolean haveStringMonth = false;
      
      int i = charlist.fromSourceOffset(parsePosition.getIndex());
      while (charlist.hasCharAt(i)) {
        if (DEBUG) {
          System.out.println(charlist.subSequence(0,i) + "|" + charlist.charAt(i) + "\t\t" + tokens);
        }
        Status status = matcher.setOffset(i).next(Filter.LONGEST_UNIQUE);
        if (status != Status.NONE) {
          addSeparator(separatorBuffer);
          if (!breakIterator.isBoundary(i)) {
            parsePosition.setErrorIndex(i);
            return null;
          }
          // TODO check for other calendars
          // Gregorian doesn't need WeekDay, so discard.
         
          final Token matchValue = matcher.getMatchValue();
          //if (matchValue.getType() != Type.WEEKDAY) {
            if (matchValue.getType() == Type.MONTH) {
              haveStringMonth = true;
            }
            if (!addToken(matchValue)) {
              break;
            }         
         // }
          i = matcher.getMatchEnd();
          continue;
        }

        // getting char instead of code point is safe, since we only use this
        // for digits, and we only care about those on the BMP.
        char ch = charlist.charAt(i);
        if (UCharacter.isDigit(ch)) {
          addSeparator(separatorBuffer);
          // the cast is safe, since we are only getting digits.
          int result = (int) UCharacter.getUnicodeNumericValue(ch);
          // following may advance 1 too far, so we'll correct later
          int j = i;
          while (charlist.hasCharAt(++j)) {
            ch = charlist.charAt(j);
            if (!UCharacter.isDigit(ch)) {
              break;
            }
            result *= 10;
            result += (int) UCharacter.getUnicodeNumericValue(ch);
          }
          if (!addToken(new IntegerToken(result))) {
            break;
          }
          i = j; // we are at least (i+1). 
          // make another pass at same point. Slightly less efficient, but makes the loop easier.
        } else if (disallowedInSeparator.contains(ch)) {
          break;
        } else {
          break; // for now, disallow arbitrary separators
//          separatorBuffer.append(ch);
//          ++i;
        }
      }
      if (DEBUG) {
        System.out.println(charlist.subSequence(0,i) + "|" + "\t\t" + tokens);
      }
      parsePosition.setIndex(charlist.toSourceOffset(i));
      
      // we now have a list of tokens. Figure out what the date is

      // first get all the string fields, and separators
      
      // we use a few facts from CLDR.
      // All patterns have date then time.
      // All patterns have the order hour, minute, second.
      // Date patterns are:
      // dMy
      
//          case INTEGER:
//            int value = token.getIntValue();
//            tokenToAllowed.put(token, allowed);
//            break;
      
      // TODO look at the separators
      // now get the integers
      Set<Type> ordering = new LinkedHashSet();
      if (false && haveSoFar.firstType == Type.HOUR) {
        ordering.addAll(integerTimeTypes);
        ordering.addAll(haveStringMonth ? dateOrdering.yd : dateOrdering.ymd);
      } else {
        ordering.addAll(haveStringMonth ? dateOrdering.yd : dateOrdering.ymd);       
        ordering.addAll(integerTimeTypes);
      }

      main:
      for (Token token : tokens) {
        if (token.getType() == Type.INTEGER) {
          IntegerToken integerToken = (IntegerToken) token;
          // pick the first ordering item that fits
          EnumSet<Type> possible = integerToken.allowsAt;
          for (Iterator<Type> it = ordering.iterator(); it.hasNext();) {
            Type item = it.next();
            if (haveSoFar.contains(item)) {
              continue;
            }
            if (possible.contains(item)) {
              integerToken.restrictAndSetCalendarFieldIfPossible(EnumSet.of(item), haveSoFar, tokens);
              continue main;
            }
          }
          // if we get this far, then none of the orderings work; we failed
          if (DEBUG) {
            System.out.println("failed to find option for " + token + " in " + possible);
          }
          return null;
        }
      }
      
      for (Token token : tokens) {
        int value = token.getIntValue();
        switch (token.getType()) {
          case ERA:
            calendar.set(calendar.ERA, value);
            break;
        case YEAR: 
            if (value < 100) {
              value = (twoDigitYearOffset / 100)*100 + value;
              if (value < twoDigitYearOffset) {
                value += 100;
              }
            }
            calendar.set(calendar.YEAR, value);
            break;
        case DAY:
          calendar.set(calendar.DAY_OF_MONTH, value);
          break;
        case MONTH: 
          calendar.set(calendar.MONTH, value - 1);
          break;
        case HOUR: 
          calendar.set(calendar.HOUR, value);
          break;
        case MINUTE: 
          calendar.set(calendar.MINUTE, value);
          break;
        case SECOND: 
          calendar.set(calendar.SECOND, value);
          break;
        case TIMEZONE: 
          calendar.setTimeZone(TimeZone.getTimeZone(ZONE_INT_MAP.get(value)));
          break;
        }
      }
//      if (!haveSoFar.contains(Type.YEAR)) {
//        calendar.set(calendar.YEAR, new Date().getYear() + 1900);
//      }
      return calendar.getTime();
    }

    @Override
    public String toString() {
      return tokens.toString();
    }

    public String debugShow() {
      return matcher.getDictionary().toString();
    }
    
    /**
     * Sets the 100-year period 2-digit years will be interpreted as being in
     * to begin on the date the user specifies.
     * @param startDate During parsing, two digit years will be placed in the range
     * <code>startDate</code> to <code>startDate + 100 years</code>.
     * @stable ICU 2.0
     */
    public void set2DigitYearStart(Date startDate) {
        twoDigitYearOffset = startDate.getYear() + 1900;
    }
  }
  
  static class SoFar {
    final EnumSet<Type> haveSoFarSet = EnumSet.noneOf(Type.class);
    Type firstType;
    public void clear() {
      haveSoFarSet.clear();
      firstType = null;
    }
    @Override
    public String toString() {
      return "{" + firstType + ", " + haveSoFarSet + "}";
    }
    public void setFirstType(EnumSet<Type> set) {
      if (firstType != null) {
        // skip
      } else {
        boolean hasDate = CollectionUtilities.containsSome(dateTypes, set);
        boolean hasTime = CollectionUtilities.containsSome(timeTypes, set);
        if (hasDate != hasTime) {
          firstType = hasDate ? Type.YEAR : Type.HOUR;
        }
      }
    }
      
    public boolean add(Token token) {
      Type o = token.getType();
      setFirstType(o);
      return haveSoFarSet.add(o);
    }
    private void setFirstType(Type o) {
      if (firstType != null) {
        // fall out
      } else if (dateTypes.contains(o)) {
        firstType = Type.YEAR;
      } else if (timeTypes.contains(o)) {
        firstType = Type.HOUR;
      }
    }
    public boolean contains(Object o) {
      return haveSoFarSet.contains(o);
    }
  }
  
  static String toShortString(Set<Type> set) {
    StringBuilder result = new StringBuilder();
    for (Type t : Type.values()) {
      if (set.contains(t)) {
        result.append(t.toString().charAt(0));
      } else {
        result.append("-");
      }
    }
    return result.toString();
  }
  
  /**
   * Tokens can be integers, separator strings, or date elements (Timezones, Months, Days, Eras)
   * @author markdavis
   *
   */
  static class Token {
    
    enum Type {
      ERA, YEAR, MONTH, WEEKDAY, DAY, HOUR, MINUTE, SECOND, AMPM, TIMEZONE, 
      INTEGER, 
      SEPARATOR,
      UNKNOWN;
      
      static Type getType(Object field) {
        char ch = field.toString().charAt(0);
        switch (ch) {
          case 'G': return Type.ERA;
          case 'y': case 'Y': case 'u': return Type.YEAR;
          //case 'Q': return Type.QUARTER;
          case 'M': case 'L': return Type.MONTH;
          //case 'w': case 'W': return Type.WEEK;
          case 'e': case 'E': case 'c': return Type.WEEKDAY;
          case 'd': case 'D': case 'F': case 'g': return Type.DAY;
          case 'a': return Type.AMPM;
          case 'h': case 'H': case 'k': case 'K': return Type.HOUR;
          case 'm': return Type.MINUTE;
          case 's': case 'S': case 'A': return Type.SECOND;
          case 'v': case 'z': case 'Z': case 'V': return Type.TIMEZONE;
        }
        return UNKNOWN;
      }
    };
      
    private final int value;
    private final Type type;

    public Type getType() {
      return type;
    }
    
    public boolean checkAllowableTypes(Token previous, SoFar haveSoFar, Collection<Token> tokensToFix) {
      if (haveSoFar.contains(getType())) {
        if (DEBUG) {
          System.out.println("Have " + this + ", but already had " + haveSoFar);
        }
        return false;
      }
      EnumSet<Type> allowable = null;
      if (previous != null && previous.getType() == Type.SEPARATOR) {
        allowable =  ((SeparatorToken) previous).getAllowsAfter();
        if (!allowable.contains(getType())) {
          if (DEBUG) {
            System.out.println("Have " + this + ", while previous token is " + previous);
          }
          return false;
        }
      }

      switch (getType()) {

      case INTEGER:        
        IntegerToken integerToken = (IntegerToken) this; // slightly kludgy to call subclass, but simpler
        return integerToken.restrictAndSetCalendarFieldIfPossible(allowable, haveSoFar, tokensToFix);
      case SEPARATOR:
        return true;
      }
      // only if set value
      return haveSoFar == null ? true : haveSoFar.add(this);
    }

    public int getIntValue() {
      return value;
    }

    public Token(int value, Type type) {
      this.value = value;
      this.type = type;
    }

    public int get() {
      return value;
    }
    @Override
    public String toString() {
      return "{" + getType() + ":" + value + (getType() == Type.TIMEZONE ? "/" + ZONE_INT_MAP.get(value) : "") + "}";
    }
    
    @Override
    public boolean equals(Object obj) {
      Token other = (Token) obj;
      return getType() == other.getType() && value == other.value;
    }
    @Override
    public int hashCode() {
      return getType().hashCode() ^ value;
    }
  }
  
  static class SeparatorToken extends Token {

    final EnumSet<Type> allowsBefore;
    final EnumSet<Type> allowsAfter;

    public SeparatorToken(EnumSet<Type> before, EnumSet<Type> after, int value) {
      this(before, after, value, Type.SEPARATOR);
    }
    
    protected SeparatorToken(EnumSet<Type> before, EnumSet<Type> after, int value, Type type) {
      super(value, type);
      allowsBefore = before.clone();
      allowsAfter = after.clone();
    }

    public EnumSet<Type> getAllowsAfter() {
      return allowsAfter;
    }

    public EnumSet<Type> getAllowsBefore() {
      return allowsBefore;
    }
    
    public String toString() {
      return "{"  +  getType() + ":" + getIntValue() + "/"  + toShortString(allowsBefore) + "/"+  toShortString(allowsAfter) +  "}";
    }
    
    @Override
    public boolean equals(Object obj) {
      if (!super.equals(obj)) {
        return false;
      }
      SeparatorToken other = (SeparatorToken) obj;
      return allowsBefore.equals(other.allowsBefore) && allowsAfter.equals(other.allowsAfter);
    }
    // don't bother with hashcode
  }
  
  // This is the only mutable one
  static class IntegerToken extends Token {

    public Type revisedType = null;
    EnumSet<Type> allowsAt;


    public IntegerToken(int value) {
      super(value, Type.INTEGER);
      allowsAt = 
      value == 0 ? EnumSet.of(Type.HOUR, Type.MINUTE, Type.SECOND)
      : value < 12 ? EnumSet.of(Type.YEAR, Type.MONTH, Type.DAY, Type.HOUR, Type.MINUTE, Type.SECOND)
          : value < 25 ?  EnumSet.of(Type.YEAR, Type.DAY, Type.HOUR, Type.MINUTE, Type.SECOND)
              : value < 32 ?  EnumSet.of(Type.YEAR, Type.DAY, Type.MINUTE, Type.SECOND)
                  : value < 60 ?  EnumSet.of(Type.YEAR, Type.MINUTE, Type.SECOND)
                      : EnumSet.of(Type.YEAR);
    }

    public boolean restrictAndSetCalendarFieldIfPossible(EnumSet<Type> allowable, SoFar haveSoFar, Collection<Token> tokensToFix) {
      if (getType() != Type.INTEGER) {
        throw new IllegalArgumentException();
      }
      EnumSet<Type> ok = allowsAt.clone();
      // TODO optimize the following
      ok.removeAll(haveSoFar.haveSoFarSet);
      if (allowable != null) {
        ok.retainAll(allowable);
      }
      if (ok.size() == 0) {
        if (DEBUG) {
          System.out.println("No possibilities for " + this + ": " + allowable + "\t" + haveSoFar);
        }
        return false; // nothing works
      }
      allowsAt = ok;
      if (ok.size() == 1) {
        revisedType = ok.iterator().next();
        haveSoFar.add(this);
        if (revisedType == Type.INTEGER) {
          throw new IllegalArgumentException();
        }
        // now look through all the other values to see if they need fixing
        for (Token token : tokensToFix) {
          // look at the other tokens to see if they need fixing
          if (token != this && token.getType() == Type.INTEGER) {
            IntegerToken other = (IntegerToken) token;
            if (!other.restrictAndSetCalendarFieldIfPossible(EnumSet.complementOf(ok), haveSoFar, tokensToFix)) {
              return false;
            }
          }
        }
        return true;
      }
      return true;
    }

    public Type getType() {
      return revisedType == null ? super.getType() : revisedType;
    }

    public Set<Type> getAllowsAt() {
      return allowsAt;
    }
    
    public String toString() {
      return "{"  +  getType() + ":" + getIntValue() + "/"  + toShortString(allowsAt) +  "}";
    }
    
    @Override
    public boolean equals(Object obj) {
      if (!super.equals(obj)) {
        return false;
      }
      IntegerToken other = (IntegerToken) obj;
      return allowsAt.equals(other.allowsAt);
    }
  }

  
  private final Matcher<Token> matcher;
  private final BreakIterator breakIterator;
  private final DateOrdering dateOrdering;
  
  public LenientDateParser(Matcher<Token> matcher, BreakIterator iterator, DateOrdering dateOrdering) {
    this.matcher = matcher;
    breakIterator = iterator;
    this.dateOrdering = dateOrdering;
  }
  
  public static LenientDateParser getInstance(ULocale locale) {
    DateOrdering dateOrdering = new DateOrdering();
    //final RuleBasedCollator col = (RuleBasedCollator) Collator.getInstance(locale);
    //CollationStringByteConverter converter = new CollationStringByteConverter(col, new StringUtf8Converter()); // new ByteString(true)
    //Matcher<String> matcher = converter.getDictionary().getMatcher();
    // later, cache this dictionary

    Map<CharSequence, Token> map = DEBUG ? new TreeMap<CharSequence, Token>() : new HashMap<CharSequence, Token>();
    DateFormatSymbols symbols = new DateFormatSymbols(locale);
    // load the data
    loadArray(map, symbols.getAmPmStrings(), Type.AMPM);
    loadArray(map, symbols.getEraNames(), Type.ERA);
    loadArray(map, symbols.getEras(), Type.ERA);
    // TODO skip Narrow??
    for (int context = 0; context < DateFormatSymbols.DT_CONTEXT_COUNT; ++context) {
      for (int width = 0; width < DateFormatSymbols.DT_WIDTH_COUNT; ++width) {
        loadArray(map, symbols.getMonths(context, width), Type.MONTH);
//        try {
//          loadArray(map, symbols.getQuarters(context, width), Type.QUARTERS);
//        } catch (NullPointerException e) {} // skip these
        loadArray(map, symbols.getWeekdays(context, width), Type.WEEKDAY);
      }
    }
    
    Date now = new Date();
    Calendar temp = Calendar.getInstance();

    String[] zoneFormats = {"z", "zzzz", "Z", "ZZZZ", "v", "vvvv", "V", "VVVV"};
    List<SimpleDateFormat> zoneFormatList = new ArrayList<SimpleDateFormat>();
    for (String zoneFormat : zoneFormats) {
      zoneFormatList.add(new SimpleDateFormat(zoneFormat, locale));
    }
    ParsePosition pos = new ParsePosition(0);
    TimeZone unknownZone = new SimpleTimeZone(-31415, "Etc/Unknown");
    
    Relation<String, String> stringToZones = new  Relation(new TreeMap(), TreeSet.class, new BestTimeZone(locale));
    
//    final UTF16.StringComparator stringComparator = new UTF16.StringComparator(true, false, 0);
//    Set<String[]> zoneRemaps = new TreeSet(new ArrayComparator(new Comparator[] {stringComparator, stringComparator, stringComparator, stringComparator}));
    
    for (String timezone : ZONE_VALUE_MAP.keySet()) {
      for (SimpleDateFormat format : zoneFormatList) {
        format.setTimeZone(TimeZone.getTimeZone(timezone));
        String formatted = format.format(now);
        stringToZones.put(formatted, timezone);
//
//        pos.setIndex(0);
//        temp.setTimeZone(unknownZone);
//        format.parse(formatted, temp, pos);
//        if (pos.getIndex() != formatted.length()) {
//          continue; // unable to parse
//        }
//        TimeZone otherZone = temp.getTimeZone();
////        if (!otherZone.getID().equals(timezone.getID())) {
////          zoneRemaps.add(new String[] {timezone.getID(), format.toPattern(), formatted, otherZone.getID()} );
////        }
//        if (!otherZone.getID().equals(unknownZone.getID())) {
//          stringToZones.put(formatted, timezone);  
//        }
      }
    }
    for (String formatted : stringToZones.keySet()) {
      final Set<String> possibilities = stringToZones.getAll(formatted);
      //System.out.println("Parsing \t\"" + formatted + "\"\tgets\t" + uniquenessStatus(possibilities) + "\t" + possibilities);
      String bestValue = possibilities.iterator().next(); // pick first value
      loadItem(map, formatted, ZONE_VALUE_MAP.get(bestValue), Type.TIMEZONE);
    }
    // get separators from formats
    // we walk through to see what can come before or after a separator, accumulating them all together
    FormatParser formatParser = new FormatParser();
    Map<String, EnumSet<Type>> beforeTypes = new HashMap();
    Map<String, EnumSet<Type>> afterTypes = new HashMap();
    EnumSet<Type> nonDateTypes = EnumSet.allOf(Type.class);
    nonDateTypes.removeAll(dateTypes);
    EnumSet<Type> nonTimeTypes = EnumSet.allOf(Type.class);
    nonTimeTypes.removeAll(timeTypes);
    for (int style = 0; style < 4; ++style) {
      addSeparatorInfo((SimpleDateFormat) DateFormat.getDateInstance(style, locale), formatParser, beforeTypes, afterTypes, nonDateTypes, dateOrdering);
      addSeparatorInfo((SimpleDateFormat) DateFormat.getTimeInstance(style, locale), formatParser, beforeTypes, afterTypes, nonTimeTypes, dateOrdering);
    }
    // now allow spaces between date and type
    add(beforeTypes, " ", dateTypes);
    add(afterTypes, " ", dateTypes);
    add(beforeTypes, " ", timeTypes);
    add(afterTypes, " ", timeTypes);

    Set<String> allSeparators = new HashSet(beforeTypes.keySet());
    allSeparators.addAll(afterTypes.keySet());
    for (String item : allSeparators) {
      loadItem(map, item, beforeTypes.get(item), afterTypes.get(item));
    }
    
    if (dateOrdering.yd.size() == 0) {
      dateOrdering.yd.addAll(dateOrdering.ymd);
    }
    
    // TODO remove the setByteConverter; it's just for debugging
    DictionaryBuilder<Token> builder = new StateDictionaryBuilder<Token>().setByteConverter(new StringUtf8Converter());
    if (DEBUG) {
      System.out.println(map);
    }

    Dictionary<Token> dict = builder.make(map);
    // System.out.println(dict.debugShow());
    //DictionaryCharList x = new DictionaryCharList(converter.getDictionary(),  string);

    LenientDateParser result = new LenientDateParser(dict.getMatcher(), BreakIterator.getWordInstance(locale), dateOrdering);
    return result;
  }
  

  private static String uniquenessStatus(Set<String> possibilities) {
    int count = 0;
    for (String zone : possibilities) {
      if (supplementalData.isCanonicalZone(zone)) {
        count++;
      }
    }
    return count == 0 ? "ZERO!!" : count == 1 ? "OK" : "AMBIGUOUS:" + count;
  }


  static final IntMap<String> ZONE_INT_MAP;
  static final Map<String, Integer> ZONE_VALUE_MAP;
  final static SupplementalDataInfo supplementalData = SupplementalDataInfo.getInstance("C:/cvsdata/unicode/cldr/common/supplemental/");
  static {
    Set<String> canonicalZones = supplementalData.getCanonicalZones();
    // get all the CLDR IDs
    Set <String> allCLDRZones = new TreeSet<String>(canonicalZones);
    for (String canonicalZone : canonicalZones) {
      allCLDRZones.addAll(supplementalData.getZone_aliases(canonicalZone));
    }
    // get all the ICU IDs
    Set<String> allIcuZones = new TreeSet<String>();
    for (String canonicalZone:TimeZone.getAvailableIDs()) {
      allIcuZones.add(canonicalZone);
      for (int i = 0; i < TimeZone.countEquivalentIDs(canonicalZone); ++i) {
        allIcuZones.add(TimeZone.getEquivalentID(canonicalZone, i));
      }
    }
    
    System.out.println("Zones in CLDR but not ICU:" + getFirstMinusSecond(allCLDRZones, allIcuZones));
    final Set<String> icuMinusCldr_all = getFirstMinusSecond(allIcuZones, allCLDRZones);
    System.out.println("Zones in ICU but not CLDR:" + icuMinusCldr_all);
    
    for (String canonicalZone : canonicalZones) {
      Set<String> aliases = supplementalData.getZone_aliases(canonicalZone);
      LinkedHashSet<String> icuAliases = getIcuEquivalentZones(canonicalZone);
      icuAliases.remove(canonicalZone); // difference in APIs
      icuAliases.removeAll(icuMinusCldr_all);
      if (!aliases.equals(icuAliases)) {
        System.out.println("Difference in Aliases for: " + canonicalZone);
        Set<String> cldrMinusIcu = getFirstMinusSecond(aliases, icuAliases);
        if (cldrMinusIcu.size() != 0) {
          System.out.println("\tCLDR - ICU: " + cldrMinusIcu);
        }
        Set<String> icuMinusCldr = getFirstMinusSecond(icuAliases, aliases);
        if (icuMinusCldr.size() != 0) {
          System.out.println("\tICU - CLDR: " + icuMinusCldr);
        }
      }
    }
    
    List<String> values = new ArrayList<String>();
    for (String id : TimeZone.getAvailableIDs()) {
      values.add(id);
    }
    ZONE_INT_MAP =  new IntMap.BasicIntMapFactory<String>().make(values);
    ZONE_VALUE_MAP = Collections.unmodifiableMap(ZONE_INT_MAP.getValueMap());
  }

  private static Set<String> getFirstMinusSecond(Set<String> first, Set<String> second) {
    Set<String> difference = new TreeSet(first);
    difference.removeAll(second);
    return difference;
  }
  
  /**
   * The best timezone is the lower one.
   */
  static class BestTimeZone implements  Comparator<String> {
    HashMap<String, Integer> bestRegions = new HashMap<String,Integer>();
    public BestTimeZone(ULocale locale) {
      int count = 0;
      String region = locale.getCountry();
      if (region.length() != 0) { // add the explicit region if there is one
        bestRegions.put(region, count++);
      }
      // now find the other regions
      String language = locale.getLanguage();
      String script = locale.getScript();
      if (script.length() != 0) {
        count = add(language + "_" + script, count);
      }
      count = add(language, count);
    }
    private int add(String language, int count) {
      Set<String> data = supplementalData.getTerritoriesForPopulationData(language);
      System.out.println("???" + language + "\t" + data);
      for (String region : data) {
        bestRegions.put(region, count++);
      }
      return count;
    }
    
    public int compare(String o1, String o2) {
      boolean c1 = supplementalData.isCanonicalZone(o1);
      boolean c2 = supplementalData.isCanonicalZone(o2);
      // canonical is lower (-1)
      if (c1 != c2) {
        return c1 ? -1 : 1;
      }
      Integer w1 = bestRegions.get(supplementalData.getZone_territory(o1));
      Integer w2 = bestRegions.get(supplementalData.getZone_territory(o2));
      if (w1 == null) w1 = 9999;
      if (w2 == null) w2 = 9999;
      int comparison = w1.compareTo(w2);
      if (comparison != 0) {
        return comparison;
      }
      // if both or neither canonical, return string comparison
      return o1.compareTo(o2);
    }
  };
  
  private static LinkedHashSet getIcuEquivalentZones(String zoneID) {
    LinkedHashSet result = new LinkedHashSet();
    final int count = TimeZone.countEquivalentIDs(zoneID);
    for (int i = 0; i < count; ++i) {
      result.add(TimeZone.getEquivalentID(zoneID, i));
    }
    return result;
  }
  
  
  static class DateOrdering {
    LinkedHashSet ymd = new LinkedHashSet();
    LinkedHashSet yd = new LinkedHashSet();
  }

  private static void addSeparatorInfo(SimpleDateFormat d, FormatParser formatParser, Map<String, EnumSet<Type>> beforeTypes, Map<String, EnumSet<Type>> afterTypes, EnumSet<Type> allowedContext, DateOrdering dateOrdering) {
    String pattern = d.toPattern();
    if (DEBUG) {
      System.out.println("Adding Pattern:\t" + pattern);
    }
    formatParser.set(pattern);
    List list = formatParser.getItems();
    List<Type> temp = new ArrayList();
    for (int i = 0; i <  list.size(); ++i) {
      Object item = list.get(i);
      if (item instanceof String) {
        String sItem = trim((String) item);
        if (i == 0) {
          add(beforeTypes, sItem, allowedContext);
        } else {
          add(beforeTypes, sItem, Type.getType(list.get(i-1)));
          add(beforeTypes, sItem, Type.INTEGER);
        }
        if (i >= list.size() - 1) {
          add(afterTypes, sItem, allowedContext);          
        } else {
          add(afterTypes, sItem, Type.getType(list.get(i+1)));         
          add(afterTypes, sItem, Type.INTEGER);         
        }
      } else {
        String var = item.toString();
        Type type = Type.getType(var);
        switch (type) {
          case MONTH:
            if (var.length() < 3) {
              temp.add(type);
            }
            break;
          case DAY: case YEAR:
          temp.add(type);
          break;
        }
      }
    }
    if (temp.contains(Type.MONTH)) {
      dateOrdering.ymd.addAll(temp);
    } else if (temp.size() != 0) {
      dateOrdering.yd.addAll(temp);
    }
  }
  



  
  private static void add(Map<String, EnumSet<Type>> stringToTypes, String item, Type type) {
    Set<Type> set = stringToTypes.get(item);
    if (set == null)  {
      stringToTypes.put(item, EnumSet.of(type));
    } else {
      set.add(type);
    }
  }

  private static void add(Map<String, EnumSet<Type>> stringToTypes, String item, EnumSet<Type> types) {
    Set<Type> set = stringToTypes.get(item);
    if (set == null)  {
      stringToTypes.put(item, EnumSet.copyOf(types));
    } else {
      set.addAll(types);
    }
  }
  
  static String trim(String source) {
    if (source.length() == 0) return source;
    source = source.trim();
    if (source.length() == 0) source = " ";
    return source;
  }

  private static void loadItem(Map<CharSequence, Token> map, String item, EnumSet<Type> before, EnumSet<Type> after) {
    map.put(item, new SeparatorToken(before, after, -1, Type.SEPARATOR));
  }

  private static void loadArray(Map<CharSequence, Token> map, final String[] array, Type type) {
    int i = type == Type.MONTH ? 1 : 0; // special case months
    for (String item : array) {
      if (item != null && item.length() != 0) {
        loadItem(map, item, i++, type);
      }
    }
  }

  private static void loadItem(Map<CharSequence, Token> map, String item, int i, Type type) {
    map.put(item, new Token(i, type));
  }
  
  public Parser getParser() {
    return new Parser((BreakIterator) breakIterator.clone());
  }
}