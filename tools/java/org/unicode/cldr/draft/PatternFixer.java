package org.unicode.cldr.draft;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.regex.Pattern;

import com.ibm.icu.text.UnicodeSet;

public class PatternFixer {
  /** 
   * added to over time: , PERL, PYTHON, PCRE...
   */
  public enum Target {JAVA}

  private Target target;

  public PatternFixer(Target target) {
    this.target = target;
  }

  public String fix(String regexPattern, int patternOptions) {
    // TODO optimize
    // TODO handle \Q...\E, (?#), #, ...
    UnicodeSetBuilder builder = new UnicodeSetBuilder(target, patternOptions);
    UnicodeSet set;
    ParsePosition parsePosition = new ParsePosition(0);
    StringBuffer result = new StringBuffer();
    int state = 0;
    for (int i = 0; i < regexPattern.length(); ++i) {
      try {
        char ch = regexPattern.charAt(i);
        switch (state) {
          case 0:
            switch (ch) {
              case '\\':
                state = 1;
                break;
              case '[':
                i = parseUnicodeSet(regexPattern, builder, parsePosition, result, i) - 1;
                continue;
            }
            break;
          case 1:
            switch (ch) {
              case 'p': case 'P': case 'N':
                i = parseUnicodeSet(regexPattern, builder, parsePosition, result, i) - 1;
                continue;
            }
            state = 0;
            break;
        }
        result.append(ch);
      } catch (ParseException e) {
        throw (IllegalArgumentException) new IllegalArgumentException().initCause(e);
      }
    }
    return result.toString();
  }

    private int parseUnicodeSet(String regexPattern, UnicodeSetBuilder builder,
            ParsePosition parsePosition, StringBuffer result, int i) throws ParseException {
      UnicodeSet set;
      parsePosition.setIndex(i);
      set = builder.parseObject(regexPattern, parsePosition);
      if (parsePosition.getIndex() == i) {
        throw new ParseException(regexPattern, i);
      }
      builder.format(set,result,null);
      return parsePosition.getIndex();
    }

    public String fix(String regexPattern) {
      return fix(regexPattern,0);
    }

    // convenience functions
    public Pattern compile(String regexPattern, int patternOptions) {
      return Pattern.compile(new PatternFixer(Target.JAVA).fix(regexPattern, patternOptions), patternOptions);
    }

    public Pattern compile(String regexPattern) {
      return Pattern.compile(new PatternFixer(Target.JAVA).fix(regexPattern));
    }

  }
