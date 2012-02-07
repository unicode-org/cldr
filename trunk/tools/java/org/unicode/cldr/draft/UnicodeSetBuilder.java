package org.unicode.cldr.draft;

import java.io.BufferedReader;
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

/**
 * Provides for parsing and formatting UnicodeSet according to different Targets and other settings.
 * @author markdavis
 */
public class UnicodeSetBuilder {
  private static final boolean SHOW_LINES = false;

  final StateMachine<UnicodeSet> machine;

  {
    try {
      BufferedReader in = BagFormatter.openUTF8Reader("../", "cldr-code/java/org/unicode/cldr/draft/UnicodeSetStates.txt");
      // icu4c-trunk/source/common/rbbirpt.txt
      // "icu4c-trunk/source/i18n/regexcst.txt"
      StateMachineBuilder<UnicodeSet> builder = new StateMachineBuilder<UnicodeSet>();
      //builder.add("quoted:=[@]");
      //builder.add("rule_char:=[^\\*\\?\\+\\[\\(/)\\{\\}\\^\\$\\|\\\\\\.]");
      //builder.add("digit_char:=[0-9]");
      while (true) {
        String line = in.readLine();
        if (line == null) break;
        if (SHOW_LINES) {
          System.out.println("Line: " + line);
        }
        builder.add(line);
      }
      in.close();
      machine = builder.build(new MyObjectBuilderFactory());
    } catch (Exception e) {
      throw (RuntimeException) new InternalError().initCause(e);
    }
  }

  private static final class MyObjectBuilderFactory implements
  StateObjectBuilderFactory<UnicodeSet> {
    public StateObjectBuilder<UnicodeSet> getInstance() {
      return new MyObjectBuilder();
    }
  }

  public enum MyActions {unhandled, doSetLiteral, doSetLiteralEscaped, doHex, doSetRange, doSetNegate, doName, 
    doPropName, doPropRelation, doPropValue, doStartSetProp, doSetBeginUnion, doSetEnd, 
    doSetBeginDifference1, doSetBeginIntersection1, doSetDifference2, doSetIntersection2, 
    doSetBackslash_s, doSetBackslash_S, doSetBackslash_w, doSetBackslash_W, doSetBackslash_d, doSetBackslash_D, 
    doSetAddAmp, doSetAddDash, };

  private static final class MyObjectBuilder extends StateObjectBuilder<UnicodeSet> {
    
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

      private static final UnicodeSet WHITESPACE = (UnicodeSet) new UnicodeSet("[:whitespace:]").freeze();

      private static final UnicodeSet NOT_WHITESPACE = (UnicodeSet) new UnicodeSet("[:^whitespace:]").freeze();

      private static final UnicodeSet WORD = (UnicodeSet) new UnicodeSet("[[:alphabetic:][:digit:]]").freeze();

      private static final UnicodeSet NOT_WORD = (UnicodeSet) new UnicodeSet("[^[:alphabetic:][:digit:]]").freeze();

      private static final UnicodeSet DIGIT = (UnicodeSet) new UnicodeSet("[:Nd:]").freeze();

      private static final UnicodeSet NOT_DIGIT = (UnicodeSet) new UnicodeSet("[:^Nd:]").freeze();

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
          case doSetAddAmp:
            current.add('&').add(lastLiteral = UTF16.charAt(string, position));
          case doSetAddDash:
            current.add('-').add(lastLiteral = UTF16.charAt(string, position));
          case doSetBackslash_s:
            current.addAll(WHITESPACE);
            break;
          case doSetBackslash_S:
            current.addAll(NOT_WHITESPACE);
            break;
          case doSetBackslash_w:
            current.addAll(WORD);
            break;
          case doSetBackslash_W:
            current.addAll(NOT_WORD);
            break;
          case doSetBackslash_d:
            current.addAll(DIGIT);
            break;
          case doSetBackslash_D:
            current.addAll(NOT_DIGIT);
            break;
          case doSetBeginUnion:
            current.addAll(NOT_WHITESPACE);
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

  public List<String> getActionNames() {
    return machine.getActionNames();
  }

  public UnicodeSet parse(String testLine, ParsePosition parsePosition) {
    return machine.parse(testLine, parsePosition);
  }
}
