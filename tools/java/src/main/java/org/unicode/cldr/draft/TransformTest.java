package org.unicode.cldr.draft;

import org.unicode.cldr.util.Timer;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeFilter;

public class TransformTest extends TestFmwk {

    private static final boolean SHOW = false;
    private static final int TIMING_ITERATIONS = 10000;

    public static void main(String[] args) {
        new TransformTest().run(args);
    }

    public void TestFix() {
        String[][] tests = {
            { "!=", "([:m:])*" },
            { "==", "(\\[:m:])*" },
            { "==", "\\Q([:m:])*\\E" },
            { "a(?:gh|[b])", "a[b{gh}]" },
        };
        for (String[] test : tests) {
            if (test[0].equals("!=")) {
                assertNotEquals("Should be different", test[1],
                    PatternFixer.fixJava(test[1]));
            } else {
                assertEquals("Should be equal", test[0].equals("==") ? test[1] : test[0],
                    PatternFixer.fixJava(test[1]));
            }
        }
    }

    public void TestSomeBasic() {
        String[] tests = {
            "RULES",
            "ab > AB; ::NULL; BA > CD;",
            "ABAB",
            "RULES",
            "ab > AB; BA > CD;",
            "ABAB",
            "RULES",
            "D { a > A;" +
                "c {(d)} e > X$1;" +
                "d > D",
            "dabcdefgd",
            "ad",
            "Da",
            "RULES",
            "::[a-z];" +
                "[:L:] { b } > B;" +
                "B > M;" +
                "z > Z;" +
                "w > W;" +
                "x > X;" +
                "q > Q;" +
                "C > Y;",
            "zB",
            "abXbCabXbCabXbCabXbCabXbCabXbCabXbCabXbCabXbCabXbCabX"
        };
        boolean setRules = true;
        String rules;
        StringTransform transform = null;
        Transliterator oldTransform = null;
        for (String testCase : tests) {
            if (testCase.equalsIgnoreCase("rules")) {
                setRules = true;
                continue;
            }
            if (setRules) {
                rules = testCase;
                transform = RegexTransformBuilder.createFromRules(rules);
                if (SHOW) logln("New:\n" + transform.toString());
                oldTransform = Transliterator.createFromRules("foo", rules, Transliterator.FORWARD);
                if (SHOW) show(oldTransform);
                setRules = false;
                continue;
            }
            check(TIMING_ITERATIONS, testCase, transform, transform);
        }
    }

    public void TestCyrillic() {
        checkAgainstCurrent("Latin-Cyrillic", "abc", "Def", "ango");
    }

    public void TestGreek() {
        checkAgainstCurrent("Latin-Greek", "abk", "Delpho", "ango", "ago");
    }

    public void checkAgainstCurrent(String translitId, String... tests) {
        Transliterator oldGreek = Transliterator.getInstance(translitId);
        String rules = oldGreek.toRules(false);
        if (SHOW) logln(rules);
        StringTransform newGreek = RegexTransformBuilder.createFromRules(rules);
        if (SHOW) logln(newGreek.toString());
        for (String test : tests) {
            check(TIMING_ITERATIONS, test, newGreek, oldGreek);
        }
    }

    private void check(int iterations, String test, StringTransform newTransform, StringTransform oldTransform) {

        Timer t = new Timer();
        String result = null;
        String oldResult = null;
        t.start();
        for (int i = 0; i < iterations; ++i) {
            result = newTransform.transform(test);
        }
        t.stop();
        long newDuration = t.getDuration();

        t.start();
        for (int i = 0; i < iterations; ++i) {
            oldResult = oldTransform.transform(test);
        }
        t.stop();
        final long oldDuration = t.getDuration();

        if (!result.equals(oldResult)) {
            errln("BAD:\t" + test + "\t=>\tnew:\t" + result + "\t!=\told:\t" + oldResult);
        } else {
            logln("OK:\t" + test + "\t=>\t" + result);
        }
        final String percent = oldDuration == 0 ? "INF" : String.valueOf(newDuration * 100 / oldDuration - 100);
        logln("new time: " + newDuration / 1.0 / iterations + "\told time: " + oldDuration / 1.0 / iterations
            + "\tnew%: " + percent + "%");
    }

    private void show(Transliterator oldTransform) {
        UnicodeFilter filter = oldTransform.getFilter();
        logln("Old:\n" + (filter == null ? "" : filter.toString() + ";\n") + oldTransform.toRules(true));
    }

}
