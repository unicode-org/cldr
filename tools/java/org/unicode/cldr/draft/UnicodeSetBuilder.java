package org.unicode.cldr.draft;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

import org.unicode.cldr.draft.PatternFixer.Target;

import com.ibm.icu.text.UnicodeSet;

public class UnicodeSetBuilder extends Format {
  private Target target;
  private int options;
  private Extension[] extensions;
  
  public UnicodeSetBuilder(Target target) {
    this.target = target;
  }

  public UnicodeSetBuilder(Target target, int patternOptions) {
    this.target = target;
    this.options = options;
  }

  @Override
  public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
    // TODO fix to be real
    toAppendTo.append(((UnicodeSet)obj).complement().complement().toPattern(false));
    return toAppendTo;
  }

  @Override
  public final UnicodeSet parseObject(String pattern, ParsePosition pos) {
    return new UnicodeSet(pattern, pos, null);
  }

  public Target getTarget() {
    return target;
  }

  public UnicodeSetBuilder setTarget(Target target) {
    this.target = target;
    return this;
  }

  public int getOptions() {
    return options;
  }

  public UnicodeSetBuilder setOptions(int options) {
    this.options = options;
    return this;
  }

  public Extension[] getExtensions() {
    return extensions;
  }

  public UnicodeSetBuilder setExtensions(Extension... extensions) {
    this.extensions = extensions;
    return this;
  }

  public abstract class Extension {

     // Is called every time a sequence like $abc is found.

     public abstract String replaceVariable(String dollarVariable);

     // resolves properties, eg:
     // Encountering \p{whitespace} would call getProperty("whitespace", "", false, result)
     // while \p{bidi_class=neutral} would call getProperty("bidi_class", "neutral", false, result)
     // and \p{name=/DOT/} would call getProperty("bidi_class", "neutral", false, result)
     // (for an example of the latter, see http://unicode.org/cldr/utility/list-unicodeset.jsp?a=\p{name=/WITH%20DOT%20ABOVE/})

     public abstract boolean getProperty(String propertyName, String propertyValue, boolean regex, UnicodeSet result);

  }
}
