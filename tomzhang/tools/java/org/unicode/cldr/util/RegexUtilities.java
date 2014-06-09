package org.unicode.cldr.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtilities {

    public static int findMismatch(Matcher m, CharSequence s) {
        int i;
        for (i = 1; i < s.length(); ++i) {
            boolean matches = m.reset(s.subSequence(0, i)).matches();
            if (!matches && !m.hitEnd()) {
                break;
            }
        }
        return i - 1;
    }

    public static int findMismatch(Pattern p, CharSequence s) {
        Matcher m = p.matcher("");
        return findMismatch(m, s);
    }

    public static String showMismatch(Matcher m, CharSequence s) {
        int failPoint = findMismatch(m, s);
        String show = s.subSequence(0, failPoint) + "☹" + s.subSequence(failPoint, s.length());
        return show;
    }

    public static String showMismatch(Pattern p, CharSequence s) {
        Matcher m = p.matcher("");
        return showMismatch(m, s);
    }
}
