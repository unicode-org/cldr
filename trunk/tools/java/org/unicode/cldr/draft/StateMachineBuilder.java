package org.unicode.cldr.draft;

import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.draft.StateMachine.StateAction;
import org.unicode.cldr.draft.StateMachine.StateObjectBuilderFactory;

import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class StateMachineBuilder<T> {
  private static final UnicodeSet WHITESPACE = (UnicodeSet) new UnicodeSet("[:Pattern_Whitespace:]").freeze();
  private static final UnicodeSet XID_START = (UnicodeSet) new UnicodeSet("[:XID_Start:]").freeze();
  private static final UnicodeSet XID_PART = (UnicodeSet) new UnicodeSet("[-[:XID_Continue:]]").freeze();

  // TODO intern the actions

  private short currentState = StateMachine.UNDEFINED;
  private UnicodeMap currentMap = null;
  private List<UnicodeMap> stateToData = new ArrayList<UnicodeMap>();
  private ParsePosition parsePosition = new ParsePosition(0);
  private Map<String, UnicodeSet> variables = new LinkedHashMap<String, UnicodeSet>();

  private Map<String, Short> stateToNumber = new LinkedHashMap<String, Short>();
  private List<String> numberToState = new ArrayList<String>();
  private Map<String, Short> actionToNumber = new LinkedHashMap<String, Short>();
  private List<String> numberToAction = new ArrayList<String>();

  private StateAction defaultAction;
  
  {
    add("eof:=[\\uFFFF]");
  }

  public StateMachine<T> build(StateObjectBuilderFactory<T> factory) {
    Set<String> missingStates = new HashSet<String>();
    for (int i = 0; i < stateToData.size(); ++i) {
      UnicodeMap action = stateToData.get(i);
      if (action == null) {
        missingStates.add(numberToState.get(i));
      }
    }
    if (missingStates.size() > 0) {
      throw new IllegalArgumentException("Missing states: " + missingStates);
    }
    fixDefaultAction();
    return new StateMachine<T>(stateToData, factory, numberToState, numberToAction);
  }

  // build state machines with rules on icu4c-trunk/source/common/rbbirpt.txt
  /**
   * #
# Here is the syntax of the state definitions in this file:
#
#
#StateName:
#   input-char           n next-state           ^push-state     action    
#   input-char           n next-state           ^push-state     action    
#       |                |   |                      |             |
#       |                |   |                      |             |--- action to be performed by state machine
#       |                |   |                      |                  See function RBBIRuleScanner::doParseActions()
#       |                |   |                      |
#       |                |   |                      |--- Push this named state onto the state stack.
#       |                |   |                           Later, when next state is specified as "pop",
#       |                |   |                           the pushed state will become the current state.
#       |                |   |
#       |                |   |--- Transition to this state if the current input character matches the input
#       |                |        character or char class in the left hand column.  "pop" causes the next
#       |                |        state to be popped from the state stack.
#       |                |
#       |                |--- When making the state transition specified on this line, advance to the next
#       |                     character from the input only if 'n' appears here.
#       |
#       |--- Character or named character classes to test for.  If the current character being scanned
#            matches, peform the actions and go to the state specified on this line.
#            The input character is tested sequentally, in the order written.  The characters and
#            character classes tested for do not need to be mutually exclusive.  The first match wins.
#            
Example:
#
#  start state, scan position is at the beginning of the rules file, or in between two rules.
#
start:
    escaped                term                  ^break-rule-end    doExprStart                       
    white_space          n start                     
    '$'                    scan-var-name         ^assign-or-rule    doExprStart
    '!'                  n rev-option                             
    ';'                  n start                                                  # ignore empty rules.
    eof                    exit              
    default                term                  ^break-rule-end    doExprStart
   */
  void add(String rules) {
    for (String rule : rules.split("[\r\n]")) {
      rule = rule.trim();
      // TODO allow # in the first field
      int commentPos = rule.indexOf('#');
      if (commentPos >= 0 && !rule.startsWith("'#")) {
        rule = rule.substring(0,commentPos).trim();
      }
      if (rule.length() == 0) {
        continue; // skip null lines
      }
      parsePosition.setIndex(0);
      if (rule.endsWith(":")) {
        if (currentState >= 0) {
          fixDefaultAction();
        }
        String stateString = scanForVariable(rule, parsePosition);
        scanOver(rule, WHITESPACE, parsePosition);
        checkAndSkip(rule, ":", "Malformed state label: ");
        currentState = getStateNumber(stateString);
        if (currentState < 0) {
          throw new IllegalArgumentException("Cannot define reserved state: " + rule);
        }
        if (stateToData.size() > currentState && stateToData.get(currentState) != null) {
          throw new IllegalArgumentException("Cannot define state twice: " + rule);
        }
        currentMap = new UnicodeMap();
        while (stateToData.size() <= currentState) {
          stateToData.add(null); // TODO make this more efficient
        }
        stateToData.set(currentState, currentMap);
      } else if (rule.contains(":=")) {
        String variable = scanForVariable(rule, parsePosition);
        if (variables.containsKey(variable)) {
          throw new IllegalArgumentException("Cannot redefine variable: " + rule);
        }
        scanOver(rule, WHITESPACE, parsePosition);
        checkAndSkip(rule, ":=", "Malformed variable rule: ");
        scanOver(rule, WHITESPACE, parsePosition);
        UnicodeSet value = getUnicodeSet(rule);
        variables.put(variable, value);
      } else {
        UnicodeSet set = getUnicodeSet(rule);
        // first field is literal character, variable, or UnicodeSet
        StateAction action = new StateAction();

        // now get the remaining terms
        // could have 'n' nextstate, OR nextstate
        // would be cleaner syntax if the n were after the state
        scanOver(rule, WHITESPACE, parsePosition);
        String item = scanForVariable(rule, parsePosition);
        if (item.equals("n")) {
          action.advanceToNextCodePoint = true;
          scanOver(rule, WHITESPACE, parsePosition);
          item = scanForVariable(rule, parsePosition);
        }
        action.nextState = getStateNumber(item);

        scanOver(rule, WHITESPACE, parsePosition);
        int index = parsePosition.getIndex();
        if (index < rule.length()) {
          if (rule.charAt(index) == '^') {
            parsePosition.setIndex(parsePosition.getIndex()+1); // skip cp
            scanOver(rule, WHITESPACE, parsePosition);
            item = scanForVariable(rule, parsePosition);
            action.pushState=getStateNumber(item);
            scanOver(rule, WHITESPACE, parsePosition);
          }

          item = scanForVariable(rule, parsePosition);
          if (item.length() > 0) {
            action.action = getItemNumber(item, actionToNumber, numberToAction);
          }
        }
        if (set == null) { // default case
          if (defaultAction != null) {
            throw new IllegalArgumentException("Cannot have more than one defaultAction: " + rule);
          } else {
            defaultAction = action;
          }
        } else {
          currentMap.putAll(set, action);
        }
      }
      // check for junk at end
      scanOver(rule, WHITESPACE, parsePosition);
      if (parsePosition.getIndex() != rule.length()) {
        throw new IllegalArgumentException("Extra stuff at end: " + rule);
      }
    }    
  }
  // invariants:
  // must have a default case for every state
  // first state must be "start"
  // pop and exit are special

  private short getStateNumber(String stateString) {
    if (stateString.equals("pop")) {
      return StateMachine.POP;
    } else if (stateString.equals("exit")) {
      return StateMachine.EXIT;
    } else if (stateString.equals("errorDeath")) {
      return StateMachine.ERROR;
    }
    return getItemNumber(stateString, stateToNumber, numberToState);
  }

  private void fixDefaultAction() {
    if (defaultAction == null) {
      throw new IllegalArgumentException("Missing default action for: " + numberToState.get(currentState));
    } else {
      currentMap.putAll(currentMap.keySet(null), defaultAction);
      defaultAction = null;
    }
  }

  private void checkAndSkip(String rule, String checkAndSkip, String errorMessage) {
    if (!rule.regionMatches(parsePosition.getIndex(), checkAndSkip, 0, checkAndSkip.length())) {
      throw new IllegalArgumentException(errorMessage + rule);
    }
    parsePosition.setIndex(parsePosition.getIndex()+checkAndSkip.length()); // skip cp
  }

  private UnicodeSet getUnicodeSet(String rule) {
    UnicodeSet set;
    if (rule.startsWith("'")) {
      // TODO: handle escaped ''
      int cp = UTF16.charAt(rule,1);
      int charCount = UTF16.getCharCount(cp);
      if (rule.charAt(1+charCount) != '\'') {
        throw new IllegalArgumentException("Illegal literal character in: " + rule);
      }
      parsePosition.setIndex(2 + charCount);
      set = new UnicodeSet(cp,cp);
    } else if (UnicodeSet.resemblesPattern(rule, parsePosition.getIndex())) {
      set = new UnicodeSet(rule, parsePosition , null, 0);
    } else {
      String variable = scanForVariable(rule, parsePosition);
      if (variable.equals("default")) {
        return null;
      }
      set = variables.get(variable);
      if (set == null) {
        throw new IllegalArgumentException("Variable used before defined: " + rule);
      }
    }
    return set;
  }

  private short getItemNumber(String item, Map<String,Short> itemToNumber, List<String> numberToItem) {
    Short result = itemToNumber.get(item);
    if (result == null) {
      result = (short) itemToNumber.size();
      itemToNumber.put(item, result);
      numberToItem.add(item);
    }
    return result;
  }



  public static int scanOver(String string, UnicodeSet set, ParsePosition parsePosition) {
    int i;
    for (i = parsePosition.getIndex(); i < string.length();) {
      // TODO make this public API
      // fix description: returns the new index, and may be index -1
      int match = set.matchesAt(string, i);
      if (match >= i) {
        i = match;
        continue;
      }
      break;
    }
    parsePosition.setIndex(i);
    return i;
  }

  public static String scanForVariable(String rule, ParsePosition parsePosition) {
    int start = parsePosition.getIndex();
    if (start < 0 || start >= rule.length()) {
      return "";
    }
    int match = XID_START.matchesAt(rule, start);
    if (match <= start) {
      return "";
    }
    parsePosition.setIndex(match);
    scanOver(rule, XID_PART, parsePosition);
    return rule.substring(start, parsePosition.getIndex());
  }
}
