package org.unicode.cldr.draft;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.regex.Pattern;

import org.unicode.cldr.util.PatternCache;

/**
 * Immutable class that allows people to fix regex pattern strings to be provide for internationalization support
 * (as per UTS 18 Unicode Regular Expressions). The chief problem with the internationalization of
 * regex engines is that character classes (eg "[\p{alphanum}@#$]") and property support in most
 * regex engines are missing properties, have the wrong values for properties,
 * have out-of-date values for properties, or are missing the syntax to
 * combine properties or character classes (eg to get the assigned characters in the Devanagari block).
 * The reason for supporting more than just Java is so that this can be use in build-time tools for generating
 * fixed regex pattern strings.
 * <p>
 * TODO add options for controlling whether to change \w, \b, etc.
 * <p>
 * TODO be sensitive to COMMENTS
 * <p>
 * TODO add support for (?#)
 * 
 * @author markdavis
 */
public class PatternFixer {
    /**
     * Regex engine type, will be added to over time: PERL, PYTHON, PCRE, and so on.
     * <p>
     * The reason for supporting more than just Java is so that this can be use in build-time tools for generating fixed
     * regex pattern strings.
     */
    public enum Target {
        JAVA
    }

    /**
     * Create for particular regex target.
     * 
     * @param target
     */
    public PatternFixer(Target target) {
        this.target = target;
    }

    public Target getTarget() {
        return target;
    }

    private enum State {
        BASE, HAVE_SLASH, HAVE_Q, HAVE_Q_SLASH
    };

    /**
     * Produce a modified pattern that fixes character classes. (See class description.)
     * 
     * @param regexPattern
     * @param patternOptions
     * @return
     */
    public String fix(String regexPattern, int patternOptions) {
        // TODO optimize
        // TODO handle (?#), #, ...
        UnicodeSetBuilder builder = new UnicodeSetBuilder(); // target, patternOptions
        ParsePosition parsePosition = new ParsePosition(0);
        StringBuffer result = new StringBuffer();
        State state = State.BASE;
        for (int i = 0; i < regexPattern.length(); ++i) {
            try {
                char ch = regexPattern.charAt(i);
                switch (state) {
                case BASE:
                    switch (ch) {
                    case '\\':
                        state = State.HAVE_SLASH;
                        break;
                    case '[':
                        i = parseUnicodeSet(regexPattern, builder, parsePosition, result, i) - 1;
                        continue;
                    }
                    break;
                case HAVE_SLASH:
                    switch (ch) {
                    case 'p':
                    case 'P':
                    case 'N':
                        i = parseUnicodeSet(regexPattern, builder, parsePosition, result, i) - 1;
                        continue;
                    case 'Q':
                        state = State.HAVE_Q;
                        break;
                    default:
                        state = State.BASE;
                        break;
                    }
                    break;
                case HAVE_Q:
                    switch (ch) {
                    case '\\':
                        state = State.HAVE_Q_SLASH;
                        break;
                    }
                    break;
                case HAVE_Q_SLASH:
                    switch (ch) {
                    case 'E':
                        state = State.BASE;
                        break;
                    default:
                        state = State.HAVE_Q;
                    }
                    break;
                }
                result.append(ch);
            } catch (ParseException e) {
                throw (IllegalArgumentException) new IllegalArgumentException().initCause(e);
            }
        }
        return result.toString();
    }

    public String fix(String regexPattern) {
        return fix(regexPattern, 0);
    }

    // convenience functions
    public static Pattern compile(String regexPattern, int patternOptions) {
        return Pattern.compile(new PatternFixer(Target.JAVA).fix(regexPattern, patternOptions), patternOptions);
    }

    public static Pattern compile(String regexPattern) {
        return PatternCache.get(new PatternFixer(Target.JAVA).fix(regexPattern));
    }

    // convenience functions
    public static String fixJava(String regexPattern, int patternOptions) {
        return new PatternFixer(Target.JAVA).fix(regexPattern, patternOptions);
    }

    public static String fixJava(String regexPattern) {
        return new PatternFixer(Target.JAVA).fix(regexPattern);
    }

    // =============== PRIVATES ========================

    private Target target;

    private int parseUnicodeSet(String regexPattern, UnicodeSetBuilder builder,
        ParsePosition parsePosition, StringBuffer result, int i) throws ParseException {
        return 0;
        // UnicodeSet set;
        // parsePosition.setIndex(i);
        // set = builder.parseObject(regexPattern, parsePosition);
        // if (parsePosition.getIndex() == i) {
        // throw new ParseException(regexPattern, i);
        // }
        // builder.format(set,result,null);
        // return parsePosition.getIndex();
    }

}
