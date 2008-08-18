package org.unicode.cldr.draft;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.Timer;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeFilter;

public class TransformTest extends TestFmwk {

  public static void main(String[] args) {
    new TransformTest().run(args);
  }

  public void TestBasic() {
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
    String test = "";
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
        logln("New:\r\n" + transform.toString());
        oldTransform = Transliterator.createFromRules("foo", rules, Transliterator.FORWARD);
        show(oldTransform);
        setRules = false;
        continue;
      }
      test = testCase;
      int iterations = 10000;
      check(iterations, testCase, transform, transform);
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
    logln("new time: " + newDuration/1.0/iterations + "\told time: " + oldDuration/1.0/iterations + "\tnew%: " + (newDuration*100/oldDuration) + "%");
  }

  private void show(Transliterator oldTransform) {
    UnicodeFilter filter = oldTransform.getFilter();
    logln("Old:\r\n" + (filter == null ? "" : filter.toString() + ";\r\n") + oldTransform.toRules(true));
  }

  public void TestGreek() {
    Transliterator oldGreek = Transliterator.getInstance("Latin-Greek");
    String rules = oldGreek.toRules(false);
    logln(rules);
    StringTransform newGreek = RegexTransformBuilder.createFromRules(rules);
    logln(newGreek.toString());
    String[] tests = {
            "abc", "Def", "ango"
    };
    for (String test : tests) {
      check(1000, test, newGreek, oldGreek);
    }
  }
}
