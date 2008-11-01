package org.unicode.cldr.draft;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;

import org.unicode.cldr.draft.StateMachine.StateAction;
import org.unicode.cldr.draft.StateMachine.StateObjectBuilder;
import org.unicode.cldr.draft.StateMachine.StateObjectBuilderFactory;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class StateMachineTest {
  private static final class MyObjectBuilderFactory implements
  StateObjectBuilderFactory<UnicodeSet> {
    public StateObjectBuilder<UnicodeSet> getInstance() {
      return new MyObjectBuilder();
    }
  }

  private static final class MyObjectBuilder extends StateObjectBuilder<UnicodeSet> {

    private enum MyActions {unhandled, doSetLiteral, doSetLiteralEscaped, doHex, doSetRange, doSetNegate, doName, 
      doPropName, doPropRelation, doPropValue, doStartSetProp, doSetBeginUnion, doSetEnd, 
      doSetBeginDifference1, doSetBeginIntersection1, doSetDifference2, doSetIntersection2, };
    
      private enum Operation {union, difference, intersection, symmetric};
    
    private static class Info {
      UnicodeSet set;
      boolean negated;
      Operation operation;
      public Info(UnicodeSet set, boolean negated, Operation operation) {
        super();
        this.set = set;
        this.negated = negated;
        this.operation = operation;
      }
    }

      UnicodeSet current = new UnicodeSet();
      private Operation operation;
      private boolean negateSet;
      
      private String propertyName;
      private String valueName;
      private boolean negateProp;
      
      private List<Info> setStack = new ArrayList<Info>();

      private int lastLiteral = 0;
      private int lastPosition;

      @Override
      protected void init(CharSequence string, StateMachine<UnicodeSet> stateMachine, int start) {
        // TODO Auto-generated method stub
        super.init(string, stateMachine, start);
        lastPosition = start;
      }

      @Override
      protected UnicodeSet getResult() {
        return current;
      }

      @Override
      protected void handle(int position, StateAction action) {
        final String actionName = getActionName(action.action);
        MyActions myAction = null;
        try {
          myAction = MyActions.valueOf(actionName);
        } catch (IllegalArgumentException e) {
          myAction = MyActions.unhandled;
        }
        if (StateMachine.SHOW_STATE_TRANSITIONS) {
          System.out.println("\t\t" + myAction + (myAction == MyActions.unhandled ? ":" + actionName : ""));
        }
        UnicodeSet propSet;
        switch(myAction) {
          case doSetNegate:
            negateSet = true;
            break;
          case doSetLiteral:
          case doSetLiteralEscaped:
            current.add(lastLiteral = UTF16.charAt(string, position));
            break;
          case doHex: // of form {612}
            current.add(lastLiteral = Integer.parseInt(string.toString().substring(lastPosition +2, position), 16));
            break;
          case doSetRange:
            current.add(lastLiteral + 1, lastLiteral = UTF16.charAt(string, position));
            break;
          case doName:
            current.add(lastLiteral = UCharacter.getCharFromExtendedName(string.toString().substring(lastPosition +2, position)));
            break;
          case doStartSetProp:
            negateProp = UTF16.charAt(string, position) == 'P';
            break;
          case doPropName:
            propertyName = string.toString().substring(lastPosition +2, position);
            propSet = new UnicodeSet().applyPropertyAlias(propertyName, "", null);
            if (negateProp) {
              propSet = propSet.complement();
            }
            current.addAll(propSet);
            break;
          case doPropRelation:
            propertyName = string.toString().substring(lastPosition +2, position);
            if (UTF16.charAt(string, position) != '=') {
              negateProp = !negateProp;
            }
            break;
          case doPropValue:
            valueName = string.toString().substring(lastPosition +1, position);
            propSet = new UnicodeSet().applyPropertyAlias(propertyName, valueName, null);
            if (negateProp) {
              propSet = propSet.complement();
            }
            current.addAll(propSet);
            break;
          case doSetBeginUnion:
            pushInfo(Operation.union);
            break;
          case doSetBeginDifference1:
          case doSetDifference2:
            pushInfo(Operation.difference);
            break;
          case doSetBeginIntersection1:
          case doSetIntersection2:
            pushInfo(Operation.intersection);
            break;
          case doSetEnd:
            if (negateSet) {
              current.complement();
            }
            final int size = setStack.size();
            if (size != 0) {
              Info popped = setStack.remove(size-1);
              UnicodeSet recent = current;
              current = popped.set;
              switch (operation) {
                case union:
                  current.addAll(recent);
                  break;
                case difference:
                  current.removeAll(recent);
                  break;
                case intersection:
                  current.retainAll(recent);
                  break;
              }
              negateSet = popped.negated;
              operation = popped.operation;
            }
            break;
        }
        if (StateMachine.SHOW_STATE_TRANSITIONS) {
          System.out.println("\t\tLiteral:" + Integer.toHexString(lastLiteral));
        }
        //System.out.println("String: " + string.subSequence(lastPosition, position) 
        //       + "\tAction: " + stateMachine.toString(action));
        lastPosition = position;
      }

      private void pushInfo(Operation operation2) {
        setStack.add(new Info(current, negateSet, operation));
        current = new UnicodeSet();
        negateSet = false;
        operation = operation2;
      }
  }

  private static final boolean SHOW_LINES = false;
  private static final boolean SHOW_MACHINE = false;
  private static int failureCount;

  public static void main(String[] args) throws IOException {
    System.out.println(new File(".").getCanonicalPath());
    BufferedReader in = BagFormatter.openUTF8Reader("../", "cldr-code/java/org/unicode/cldr/draft/UnicodeSetStates.txt"
    );
    // icu4c-trunk/source/common/rbbirpt.txt
    // "icu4c-trunk/source/i18n/regexcst.txt"
    StateMachineBuilder<UnicodeSet> builder = new StateMachineBuilder<UnicodeSet>();
    /*builder.add("escaped:=[?]");
    builder.add("white_space:=[:Pattern_Whitespace:]");
    builder.add("name_start_char:=[:XID_Start:]");
    builder.add("name_char:=[:XID_Continue:]");
    builder.add("rule_char:=[?]");
     */
    builder.add("quoted:=[@]");
    builder.add("rule_char:=[^\\*\\?\\+\\[\\(/)\\{\\}\\^\\$\\|\\\\\\.]");
    builder.add("digit_char:=[0-9]");


    /*
    escaped                term                  ^break-rule-end    doExprStart                       
    white_space          n start                     
    '$'                    scan-var-name         ^assign-or-rule    doExprStart
    '!'                  n rev-option                             
    ';'                  n start                                                  # ignore empty rules.
    eof                    exit              
    default                term                  ^break-rule-end    doExprStart
     */
    while (true) {
      String line = in.readLine();
      if (line == null) break;
      if (SHOW_LINES) {
        System.out.println("Line: " + line);
      }
      builder.add(line);
    }
    in.close();
    StateMachine<UnicodeSet> machine = builder.build(new MyObjectBuilderFactory());

    if (SHOW_MACHINE) {
      System.out.println(machine);
    }
    System.out.println(machine.getActionNames());

    String[][] testLines = {
            {"[[a-z]", "doSetNoCloseError"},
            {"[[a-z]-[b-m][s]]"},
            {"[[a-z]--[b-m][s]]", "[[a-z]-[[b-m][s]]]"},
            {"[a-z--b-ms]", "[[a-z]-[[b-m][s]]]"},
            {"[[a-z]&[b-m][s]]"},
            {"[[a-z]&&[b-m][s]]", "[[a-z]&[[b-m][s]]]"},
            {"[a-z&&b-ms]", "[[a-z]&[[b-m][s]]]"},
            {"[ab[^a-z]e]"},
            {"[b\\x{AC00}cd-mf]"},
            {"[^abc]"},
            {"[\\p{Nd}]"},
            {"[\\P{Nd}]"},
            {"[\\p{gc=Nd}]"},
            {"[\\P{gc=Nd}]"},
            {"[\\p{gc≠Nd}]", "[\\P{gc=Nd}]"},
            {"[\\P{gc≠Nd}]", "[\\p{gc=Nd}]"},
    };
    ParsePosition parsePosition = new ParsePosition(0);
    failureCount = 0;
    main:
    for (String[] testLinePair : testLines) {
      String testLine = testLinePair[0];
      String expectedString = testLinePair[testLinePair.length > 1 ? 1 : 0];
      UnicodeSet expected = expectedString.startsWith("[") ? new UnicodeSet(expectedString) : null;
      System.out.println();
      System.out.println("Test: " + testLine);
      int i = 0;
      parsePosition.setIndex(0);
      do {
        UnicodeSet result = null;
        try {
          result = machine.parse(testLine, parsePosition);
        } catch (Exception e) {
          String actualString = e.getMessage();
          if (!expectedString.equals(actualString)) {
            showFailure(testLine, expectedString, actualString);
          }
          continue main;
        }
        if (expected == null) {
          showFailure(testLine, expectedString, "<no failure>");
          continue main;
        }
        int j = parsePosition.getIndex();
        if (j <= i) {
          System.out.println("FAILURE TO ADVANCE" + parsePosition);
          break;
        }
        if (!result.equals(expected)) {
          showFailure(testLine, expected.toPattern(false), result.toPattern(false));
        }
        i = j;
      } while (i < testLine.length());
    }
    System.out.println();
    System.out.println("TOTAL Failures: " + failureCount);
  }

  private static void showFailure(String testLine, String expectedString, String actualString) {
    System.out.println("***\r\nFAILURE with: " + testLine 
            + "\tExpected: " + expectedString 
            + "\tActual: " + actualString
            + "\r\n***");
    failureCount++;
  }
}
