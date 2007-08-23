package org.unicode.cldr.util;

import org.unicode.cldr.util.Dictionary.DictionaryBuilder;
import org.unicode.cldr.util.Dictionary.Matcher;
import org.unicode.cldr.util.Dictionary.Matcher.Filter;
import org.unicode.cldr.util.Dictionary.Matcher.Status;
import org.unicode.cldr.util.LenientDateParser.Token.Type;
import org.unicode.cldr.util.StateDictionary.StateMatcher;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.DateFormatSymbols;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.ULocale;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Immutable class that will parse dates and times for a particular ULocale.
 * @author markdavis
 */
public  class LenientDateParser {
  private static final UnicodeSet disallowedInSeparator = (UnicodeSet) new UnicodeSet("[:alphabetic:]").freeze();

  public class Parser {
    final List<Token> tokens = new ArrayList<Token>();
    final Set<Type> haveSoFar = EnumSet.noneOf(Type.class);
    final BreakIterator breakIterator;
    final Calendar calendar = Calendar.getInstance();
    
   Parser(BreakIterator breakIterator) {
      this.breakIterator = breakIterator;
    }

    public Date parse(CharSequence charlist, ParsePosition parsePosition) {
      return parse(new CharUtilities.CharListWrapper(charlist), parsePosition);
    }
    
    boolean addToken(Token token) {
      if (haveSoFar.contains(token.type)) {
        return false;
      }
      switch (token.type) {
        case ERA:
          calendar.set(calendar.ERA, token.getIntValue());
          haveSoFar.add(token.type);
          break;
        case MONTH:
          calendar.set(calendar.MONTH, token.getIntValue());
          haveSoFar.add(token.type);
          break;
        case WEEKDAYS:
          calendar.set(calendar.DAY_OF_WEEK, token.getIntValue());
          haveSoFar.add(token.type);
          break;
        case AMPM:
          calendar.set(calendar.AM_PM, token.getIntValue());
          haveSoFar.add(token.type);
          break;
        case INTEGER: 
          if (token.getIntValue() > 60) {
            if (haveSoFar.contains(Type.YEAR)) {
              return false;
            }
            calendar.set(calendar.YEAR, token.getIntValue());
            haveSoFar.add(Type.YEAR);
          }
          break;
      }
      tokens.add(token);
      return true;
    }
    
    public Date parse(CharList charlist,  ParsePosition parsePosition) {
      tokens.clear();
      haveSoFar.clear();
      parsePosition.setErrorIndex(-1);
      StringBuilder separatorBuffer = new StringBuilder();
      matcher.setText(charlist);
      int i = parsePosition.getIndex();
      for (; charlist.hasCharAt(i); ++i) {
        Status status = matcher.setOffset(i).next(Filter.LONGEST_UNIQUE);
        if (status != Status.NONE) {
          addSeparator(separatorBuffer);
          if (!breakIterator.isBoundary(i)) {
            parsePosition.setErrorIndex(i);
            return null;
          }
          if (!addToken(matcher.getMatchValue())) {
            break;
          }
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
          if (!addToken(new Token<Integer>(result, Type.INTEGER))) {
            break;
          }
          i = j - 1;  // backup one
        } else if (disallowedInSeparator.contains(ch)) {
          break;
        } else {
          separatorBuffer.append(ch);
        }
      }
      parsePosition.setIndex(i);
      
      // we now have a list of tokens. Figure out what the date is
      calendar.clear();
      // first get all the string fields, and separators
      
      // we use a few facts from CLDR.
      // All patterns have date then time.
      // All patterns have the order hour, minute, second.
      // Date patterns are:
      // dMy
      
//          case INTEGER:
//            int value = token.getIntValue();
//            int allowed = 
//              value == 0 ? (1<< Calendar.HOUR) | (1<<Calendar.MINUTE) | (1<<Calendar.SECOND)
//              : value < 12 ? (1<< Calendar.YEAR) | (1<< Calendar.MONTH) | (1<< Calendar.DAY_OF_MONTH) | (1<< Calendar.HOUR) | (1<<Calendar.MINUTE) | (1<<Calendar.SECOND)
//                  : value < 25 ?  (1<< Calendar.YEAR) | (1<< Calendar.DAY_OF_MONTH) | (1<< Calendar.HOUR) | (1<<Calendar.MINUTE) | (1<<Calendar.SECOND)
//                      : value < 32 ?  (1<< Calendar.YEAR) | (1<< Calendar.DAY_OF_MONTH) | (1<<Calendar.MINUTE) | (1<<Calendar.SECOND)
//                          : value < 60 ?  (1<< Calendar.YEAR) | (1<<Calendar.MINUTE) | (1<<Calendar.SECOND)
//                              : (1<< Calendar.YEAR);
//            tokenToAllowed.put(token, allowed);
//            break;
      
      // TODO look at the separators
      // now get the integers
      // for testing, just assume it is yyyy mmm dd hh mm ss
      int soFar = 0;
      for (Token token : tokens) {
        if (token.type == Type.INTEGER) {
          int value = token.getIntValue();
          switch (soFar) {
            case 0: 
              calendar.set(calendar.DAY_OF_MONTH, value);
              soFar = haveSoFar.contains(Type.MONTH) ? 2 : 1;
              break;
            case 1: 
              calendar.set(calendar.MONTH, value-1);
              soFar++;
              break;
            case 2: 
              calendar.set(calendar.YEAR, value);
              soFar++;
              break;
            case 3: 
              calendar.set(calendar.HOUR, value);
              soFar++;
              break;
            case 4: 
              calendar.set(calendar.MINUTE, value);
              soFar++;
              break;
            case 5: 
              calendar.set(calendar.SECOND, value);
              soFar++;
              break;
          }
        }
      }
      return calendar.getTime();
    }

    private void addSeparator(StringBuilder separatorBuffer) {
      if (separatorBuffer.length() != 0) {
        tokens.add(new Token<String>(separatorBuffer.toString(), Type.OTHER_SEPARATOR));
        separatorBuffer.setLength(0);
      }
    }
    
    @Override
    public String toString() {
      return tokens.toString();
    }

    public String debugShow() {
      return matcher.getDictionary().toString();
    }
  }
  
  /**
   * Tokens can be integers, separator strings, or date elements (Timezones, Months, Days, Eras)
   * @author markdavis
   *
   */
  static class Token<T> {
    
    enum SeparatorType {
      AFTER_HOUR,
      BEFORE_MINUTE,
      AFTER_MINUTE,
      BEFORE_SECOND,
    }
    
    enum Type {
      ERA, YEAR, MONTH, WEEKDAYS, DAY, HOUR, MINUTE, SECOND, AMPM, TIMEZONE, 
      INTEGER, 
      KNOWN_SEPARATOR,
      OTHER_SEPARATOR};
      
    final T value;
    final Type type;
   
    public Type getType() {
      return type;
    }

    public int getIntValue() {
      return (Integer)value;
    }

    public Token(T value, Type type) {
      this.value = value;
      this.type = type;
    }

    T get() {
      return value;
    }
    @Override
    public String toString() {
      return "{" + type + ":" + value.toString() + "}";
    }
    
    @Override
    public boolean equals(Object obj) {
      Token other = (Token) obj;
      return type == other.type && value.equals(other.value);
    }
    @Override
    public int hashCode() {
      return type.hashCode() ^ value.hashCode();
    }
  }
  
  private Matcher<Token> matcher;
  BreakIterator breakIterator;
  
  public LenientDateParser(Matcher<Token> matcher, BreakIterator iterator) {
    this.matcher = matcher;
    breakIterator = iterator;
  }
  
  public static LenientDateParser getInstance(ULocale locale) {
    //final RuleBasedCollator col = (RuleBasedCollator) Collator.getInstance(locale);
    //CollationStringByteConverter converter = new CollationStringByteConverter(col, new StringUtf8Converter()); // new ByteString(true)
    //Matcher<String> matcher = converter.getDictionary().getMatcher();
    // later, cache this dictionary
    // TODO change to HashMap unless debugging
    Map<CharSequence, Token> map = new TreeMap<CharSequence, Token>();
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
        loadArray(map, symbols.getWeekdays(context, width), Type.WEEKDAYS);
      }
    }
    // TODO: get separators from formats
    
    // TODO remove the setByteConverter; it's just for debugging
    DictionaryBuilder<Token> builder = new StateDictionaryBuilder<Token>().setByteConverter(new StringUtf8Converter());
    System.out.println(map);
    Dictionary<Token> dict = builder.make(map);
    System.out.println(dict.debugShow());
    //DictionaryCharList x = new DictionaryCharList(converter.getDictionary(),  string);

    LenientDateParser result = new LenientDateParser(dict.getMatcher(), BreakIterator.getWordInstance(locale));
    return result;
  }
  
  private static void loadArray(Map<CharSequence, Token> map, final String[] array, Type type) {
    int i = 0;
    for (String item : array) {
      if (item != null && item.length() != 0) {
        map.put(item, new Token(i++, type));
      }
    }
  }
  
  public Parser getParser() {
    return new Parser((BreakIterator) breakIterator.clone());
  }
}