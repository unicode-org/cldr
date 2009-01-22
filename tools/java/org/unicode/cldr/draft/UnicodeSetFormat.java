package org.unicode.cldr.draft;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.BitSet;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.PatternFixer.Target;
import org.unicode.cldr.util.PrettyPrinter;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class UnicodeSetFormat extends Format {

  
  public UnicodeSetFormat(Target target) {
    this.target = target;
  }

  public UnicodeSetFormat(Target target, int patternOptions) {
    this.target = target;
    this.options = patternOptions;
  }
  
  // main methods

  @Override
  // TODO clean up prototype
  public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
    PrettyPrinter foo;
    // API for Format calls for StringBuffer, but should update to StringBuilder
    int startPos = toAppendTo.length();
    Set<String> strings = null;
    toAppendTo.append('[');
    for (UnicodeSetIterator it = new UnicodeSetIterator((UnicodeSet) obj); it.nextRange();) {
      if (it.codepoint == UnicodeSetIterator.IS_STRING) {
        if (strings == null) {
          strings = new TreeSet<String>();
        }
        strings.add(it.string);
        continue;
      }
      appendQuoted(toAppendTo, it.codepoint);
      if (it.codepointEnd != it.codepoint) {
        appendQuoted(toAppendTo.append('-'), it.codepointEnd);
      }
    }
    toAppendTo.append(']');
    if (strings != null) { // edge case
      StringBuffer extras = new StringBuffer("(?:");
      for (String string : strings) {
        appendQuoted(extras, string).append('|');
      }
      toAppendTo.insert(startPos, extras);
      toAppendTo.append(')');
    }
    return toAppendTo;
  }

  // TODO optimize this to only quote what is needed for the particular target 
  // and (possibly) the given location in the character class
  private StringBuffer appendQuoted(StringBuffer target, int codePoint) {
    switch (codePoint) {
      case '[': // SET_OPEN:
      case ']': // SET_CLOSE:
      case '-': // HYPHEN:
      case '^': // COMPLEMENT:
      case '&': // INTERSECTION:
      case '\\': //BACKSLASH:
      case '{':
      case '}':
      case '$':
      case ':':
        target.append('\\');
        break;
      default:
        if (toQuote.contains(codePoint)) {
          if (codePoint > 0xFFFF) {
            target.append("\\u");
            target.append(Utility.hex(UTF16.getLeadSurrogate(codePoint),4));
            codePoint = UTF16.getTrailSurrogate(codePoint);
          }
          target.append("\\u");
          target.append(Utility.hex(codePoint,4));                    
          return target;
        }
    }
    UTF16.append(target, codePoint);
    return target;
  }

  private StringBuffer appendQuoted(StringBuffer target, String string) {
    for (int i = 0; i < string.length(); ++i) {
      appendQuoted(target, string.charAt(i));
      // don't worry about surrogates; this works in Java
      // for other Targets we may have to fix.
    }
    return target;
  }

  @Override
  public final UnicodeSet parseObject(String pattern, ParsePosition pos) {
    return new UnicodeSet(pattern, pos, null);
  }

  // settings
  
  public Target getTarget() {
    return target;
  }

  public UnicodeSetFormat setTarget(Target target) {
    this.target = target;
    return this;
  }

  public int getOptions() {
    return options;
  }

  public UnicodeSetFormat setOptions(int options) {
    this.options = options;
    return this;
  }

  public Extension[] getExtensions() {
    return extensions;
  }

  public UnicodeSetFormat setExtensions(Extension... extensions) {
    this.extensions = extensions;
    return this;
  }

  public abstract class Extension {

     /**
      * Is called every time an unquoted $ is found. Should parse out variables as appropriate
      * and return how far we got, and the replacement string. Returns null if doesn't match a variable.
      * @pos on input should be set to the position just before the dollar sign.
      * On output should be set to the end of the text to replace.
      */
     public abstract String replaceVariable(String pattern, ParsePosition pos);
     
     /**
       * Resolves anything that looks like a property, eg:
       * <br>encountering \p{whitespace} or [:whitespace:] would call
       * getProperty("whitespace", "", false, result)<br>while
       * \p{bidi_class=neutral} would call getProperty("bidi_class", "neutral",
       * false, result) and <br>\p{name=/DOT/} would call
       * getProperty("bidi_class", "neutral", false, result)
       * <br>(for an example of the latter, see
       * {@linkplain http://unicode.org/cldr/utility/list-unicodeset.jsp?a=\p{name=/WITH%20DOT%20ABOVE/}}
       * @param regex Set to true if the property value is a regex "find" expression. In that case,
       * the return value should be the set of Unicode characters that match the regex.
       */
     public abstract boolean getProperty(String propertyName, String propertyValue, boolean regex, UnicodeSet result);

  }
  

  public String formatWithProperties(UnicodeSet original, boolean addOthers, UnicodeSet expandBlockIgnorables, int... properties) {
    UnicodeSet remainder = new UnicodeSet().addAll(original);
    Set<String> propSet = new TreeSet<String>();
    BitSet props = new BitSet();

    for (int i = 0; i < properties.length; ++i) {
      reduceByProperty(original, expandBlockIgnorables, properties[i], remainder, propSet);
      props.set(i);
    }
    if (addOthers) {
      for (int i = UProperty.INT_START; i < UProperty.INT_LIMIT; ++i) {
        if (props.get(i)) continue;
        reduceByProperty(original, expandBlockIgnorables, i, remainder, propSet);
      }
    }
    StringBuffer result = new StringBuffer("[ ");
    for (String prop : propSet) {
      result.append(prop).append(" ");
    }
    if (expandBlockIgnorables != null) {
      result.append("- ").append(expandBlockIgnorables.toPattern(true));
    } 
    if (remainder.size() > 0) {
      result.append(" ").append(remainder.toPattern(true));
    }
    result.append("]");
    return result.toString();
  }
  
  static final int blockEnum = UCharacter.getPropertyEnum("block");

  private void reduceByProperty(UnicodeSet original, UnicodeSet expandBlockIgnorables, int property, UnicodeSet remainder, Set<String> result) {
    String propertyAlias = UCharacter.getPropertyName(property, UProperty.NameChoice.SHORT);
    UnicodeSet valueChars = new UnicodeSet();
    for (int i = UCharacter.getIntPropertyMinValue(property); i <= UCharacter.getIntPropertyMaxValue(property); ++i) {
      String valueAlias = UCharacter.getPropertyValueName(property, i, UProperty.NameChoice.SHORT);
      if (valueAlias == null) {
        valueAlias = UCharacter.getPropertyValueName(property, i, UProperty.NameChoice.LONG);
      }
      if (valueAlias == null) continue;

      valueChars.clear();
      valueChars.applyPropertyAlias(propertyAlias, valueAlias);
      if (remainder.containsSome(valueChars)) {
        if (original.containsAll(valueChars)) {
          result.add("[:" + propertyAlias + '=' + valueAlias + ":]");
          remainder.removeAll(valueChars);
        } else if (property == blockEnum && expandBlockIgnorables != null) {
          UnicodeSet hasScript = new UnicodeSet(valueChars).removeAll(expandBlockIgnorables);
          if (hasScript.size() > 5 && original.containsAll(hasScript)) {
            System.out.println("Broadening to block: " + valueAlias);
            result.add("[:" + propertyAlias + '=' + valueAlias + ":]");
            remainder.removeAll(valueChars);
          }
        }
      }
    }
  }
  // ===== PRIVATES =====
  private static final long serialVersionUID = 1L;
  private Target target;
  private int options;
  private Extension[] extensions;
  private static final UnicodeSet toQuote = (UnicodeSet) new UnicodeSet("[[:Cn:][:Default_Ignorable_Code_Point:][:patternwhitespace:]]").freeze();

}
