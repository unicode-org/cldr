package org.unicode.cldr.util;

import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.util.XPattern.XMatcher;

public class TestMiscUtilities extends TestFmwk {

    public static void main(String[] args) {
        new TestMiscUtilities().run(args);
    }

    public void testXPattern() {
        // TODO turn into test
        String[][] tests = {
            {"[ab]", "b", "true"},
            {"[ab]", "c", "false"},
            {"!!![ab]", "b", "false"},
            {"!!![ab]", "c", "true"},
            {"[ab]!!![a]", "b", "true"},
            {"[a]!!![ab]", "b", "false"},
            {"[ab]!!![a]", "a", "false"},
            {"[a]!!![ab]", "a", "false"},
            {"[ab]&&&[a]", "b", "false"},
            {"[a]&&&[ab]", "b", "false"},
            {"[ab]&&&[a]", "a", "true"},
            {"[a]&&&[ab]", "a", "true"},
            {"!!!", "b", "!!! and &&& must be followed by a regex Pattern"},
            {"&&&", "b", "!!! and &&& must be followed by a regex Pattern"},
            {"&&&&&&", "b", "!!! and &&& must be followed by a regex Pattern"},
        };
        for (String[] test : tests) {
            String pattern = test[0];
            String toMatch = test[1];
            String expected = test[2];
            String actual;
            XPattern xp = null;
            XMatcher matcher = null;
            try {
                xp = XPattern.compile(pattern);
                matcher = xp.matcher(toMatch);
                actual = String.valueOf(matcher.match());
            } catch (Exception e) {
                actual = e.getMessage();
                // e.printStackTrace();
            }
            if (!assertEquals(Joiners.TAB.join(pattern, toMatch), expected, actual)
                    || isVerbose()) {
                System.out.println("\tXPattern: " + xp + "\n\tXMatcher: " + matcher);
            }
        }
    }
}
