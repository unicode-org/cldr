package org.unicode.cldr.draft;

import java.io.BufferedReader;
import java.io.File;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.UnicodeSetBuilder.MyActions;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.text.UnicodeSet;

public class StateMachineTest {

  private static boolean SHOW_MACHINE = false;
  private static int           failureCount;
  private static boolean SHOW_IF_ERROR = true;

  public static void main(String[] args) throws Exception {
    System.out.println(new File(".").getCanonicalPath());

    BufferedReader in = BagFormatter.openUTF8Reader("../",
    "cldr-code/java/org/unicode/cldr/draft/UnicodeSetBuilderTests.txt");
    // icu4c-trunk/source/common/rbbirpt.txt
    // "icu4c-trunk/source/i18n/regexcst.txt"

    List<String[]> testLines = new ArrayList();
    while (true) {
      String line = in.readLine();
      if (line == null)
        break;
      int commentPos = line.indexOf('#');
      if (commentPos >= 0) {
        line = line.substring(0, commentPos);
      }
      if (line.length() == 0) {
        continue;
      }
      testLines.add(line.trim().split("\\s*;\\s*"));
    }
    in.close();

    UnicodeSetBuilder machine = new UnicodeSetBuilder();

    if (SHOW_MACHINE) {
      System.out.println(machine);
    }
    MyActions[] valuesUsed = UnicodeSetBuilder.MyActions.values();
    Set<String> possibleActions = new TreeSet<String>();
    Set<String> actionsUsed = new TreeSet();
    Set<String> errors = new TreeSet();
    for (MyActions valueUsed : valuesUsed) {
      final String string = valueUsed.toString();
      actionsUsed.add(string);
    }
    for (String possible : machine.getActionNames()) {
      if (possible.contains("Error")) {
        errors.add(possible);
      } else {
        possibleActions.add(possible);
      }
    }
    
    System.out.println("Errors: " + errors);
    Set<String> temp = new TreeSet();
    temp.addAll(possibleActions);
    temp.removeAll(actionsUsed);
    System.out.println("Unused Actions: " + temp);
    temp.clear();
    temp.addAll(actionsUsed );
    temp.removeAll(possibleActions);
    System.out.println("Extra Enums: " + temp);

    /*
     * String[][] testLines = { {"[[a-z]", "doSetNoCloseError"},
     * {"[[a-z]-[b-m][s]]"}, {"[[a-z]--[b-m][s]]", "[[a-z]-[[b-m][s]]]"},
     * {"[a-z--b-ms]", "[[a-z]-[[b-m][s]]]"}, {"[[a-z]&[b-m][s]]"},
     * {"[[a-z]&&[b-m][s]]", "[[a-z]&[[b-m][s]]]"}, {"[a-z&&b-ms]",
     * "[[a-z]&[[b-m][s]]]"}, {"[ab[^a-z]e]"}, {"[b\\x{AC00}cd-mf]"},
     * {"[^abc]"}, {"[\\p{Nd}]"}, {"[\\P{Nd}]"}, {"[\\p{gc=Nd}]"},
     * {"[\\P{gc=Nd}]"}, {"[\\p{gc≠Nd}]", "[\\P{gc=Nd}]"}, {"[\\P{gc≠Nd}]",
     * "[\\p{gc=Nd}]"}, };
     */
//    for (String[] testLinePair : testLines) {
//      System.out
//      .println(testLinePair[0] + " ; " + (testLinePair.length > 1 ? testLinePair[1] : ""));
//    }
    ParsePosition parsePosition = new ParsePosition(0);
    failureCount = 0;
    main: for (String[] testLinePair : testLines) {
      String testLine = testLinePair[0];
      if (testLine.startsWith("@")) {
        if (testLine.equals("@show")) {
          StateMachine.SHOW_STATE_TRANSITIONS = SHOW_MACHINE = true;
          SHOW_IF_ERROR = false;
        } else if (testLine.equals("@hide")) {
          StateMachine.SHOW_STATE_TRANSITIONS = SHOW_MACHINE = false;
          SHOW_IF_ERROR = false;
        } else if (testLine.equals("@showerror")) {
          StateMachine.SHOW_STATE_TRANSITIONS = SHOW_MACHINE = false;
          SHOW_IF_ERROR = true;
        } else {
          throw new IllegalArgumentException("Illegal test command: " + testLine);
        }
        continue;
      }
      String expectedString = testLinePair[testLinePair.length > 1 ? 1 : 0];
      UnicodeSet expected = expectedString.startsWith("[") || expectedString.startsWith("\\") ? new UnicodeSet(expectedString) : null;
      System.out.println();
      System.out.println("Test: " + testLine);
      int i = 0;
      parsePosition.setIndex(0);
      do {
        UnicodeSet result = null;
        String actualString = "<no failure>";
        try {
          result = machine.parse(testLine, parsePosition);
        } catch (Exception e) {
          actualString = e.getMessage();
        }
        if (expected == null) {
          if (!expectedString.equals(actualString)) {
            showFailure(testLine, expectedString, "<no failure>");
          }
          continue main;
        }
        int j = parsePosition.getIndex();
        if (j <= i) {
          System.out.println("ERROR: " + testLine.substring(0,parsePosition.getErrorIndex()) 
                  + "$" + testLine.substring(parsePosition.getErrorIndex()));
          repeatCall(machine, parsePosition, testLine);
          break;
        }
        if (!result.equals(expected)) {
          showFailure(testLine, expected.toPattern(false), result.toPattern(false));
          repeatCall(machine, parsePosition, testLine);
        }
        i = j;
      } while (i < testLine.length());
    }
    System.out.println();
    System.out.println("TOTAL Failures: " + failureCount);
  }

  private static void repeatCall(UnicodeSetBuilder machine, ParsePosition parsePosition,
          String testLine) {
    if (!SHOW_IF_ERROR) {
      return;
    }
    UnicodeSet result;
    boolean oldShow = SHOW_MACHINE;
    StateMachine.SHOW_STATE_TRANSITIONS = SHOW_MACHINE = true;
    try {
      result = machine.parse(testLine, parsePosition);
    } catch (Exception e) {
    }
    StateMachine.SHOW_STATE_TRANSITIONS = SHOW_MACHINE = oldShow;
  }

  private static void showFailure(String testLine, String expectedString, String actualString) {
    System.out.println("***\r\nFAILURE with: " + testLine + "\tExpected: " + expectedString
            + "\tActual: " + actualString + "\r\n***");
    failureCount++;
  }
}
