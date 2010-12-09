package org.unicode.cldr.draft;

import java.text.ParsePosition;
import java.util.Arrays;
import java.util.List;

import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class StateMachine<T> {
  static boolean SHOW_STATE_TRANSITIONS = false; // Utility.getProperty("transitions", false);

  private static final short START = 0;
  static final short EXIT = -1;
  static final short POP = -2;
  static final short ERROR = -3;
  static final short UNDEFINED = -4;

  private final UnicodeMap[] stateToData;
  private final StateObjectBuilderFactory<T> factory;
  private String[] stateNames;
  private String[] actionNames;

  StateMachine(List<UnicodeMap> stateToData, StateObjectBuilderFactory<T> factory, 
          List<String> stateNames, List<String> actionNames) {
    this.stateToData = stateToData.toArray(new UnicodeMap[stateToData.size()]);
    this.stateNames = stateNames.toArray(new String[stateNames.size()]);
    this.actionNames = actionNames.toArray(new String[actionNames.size()]);
    this.factory = factory;
  }

  /**
   * Immutable internal object that contains the row of a state machine
   */
  public static class StateAction {
    boolean advanceToNextCodePoint = false;
    short nextState = StateMachine.UNDEFINED;
    short pushState = StateMachine.UNDEFINED;
    short action = -1;
    public boolean equals(Object other) {
      StateAction that = (StateAction) other;
      return advanceToNextCodePoint == that.advanceToNextCodePoint
      && nextState == that.nextState
      && pushState == that.pushState
      && action == that.action;
    }
  }

  public interface StateObjectBuilderFactory<T> {
    public StateObjectBuilder<T> getInstance();
  }

  public static class StateObjectBuilder<T> {
    protected CharSequence string;
    private StateMachine<T> stateMachine;
    private short[] stateStack = new short[100];
    private int stackSize = 0;
    private final void push(short state) {
      stateStack[stackSize++] = state;
    }
    private final short pop() {
      return stateStack[--stackSize];
    }
    protected void init(CharSequence string, StateMachine<T> stateMachine, int start) {
      this.string = string;
      this.stateMachine = stateMachine;
    }
    protected T getResult() {
      return null;
    }
    protected String getActionName(short action) {
      return stateMachine.getActionName(action);
    }
    protected void handle(int position, StateAction action) {

    }
    public String toString() {
      StringBuilder result = new StringBuilder("[");
      for (int i = stackSize-1; i >= 0; --i) {
        if (i != stackSize - 1) {
          result.append(", ");
        }
        result.append(stateMachine.getStateName(stateStack[i]));
      }
      return result.append("]").toString();
    }
  }

  public String toString(StateAction action) {
    return "{" 
    + (action.advanceToNextCodePoint ? "+" : "")
    + getStateName(action.nextState)
    + (action.pushState == StateMachine.UNDEFINED ? "" : " ^" + getStateName(action.pushState))
    + (action.action < 0 ? "" : " " + getActionName(action.action))
    + "}";
  }

  private String getStateName(short nextState) {
    switch (nextState) {
      case POP: return "pop";
      case EXIT: return "exit";
      case ERROR: return "errorDeath";
      default: return (stateNames == null ? String.valueOf(nextState) : stateNames[nextState]);
    }
  }
  
  private String getActionName(short action) {
    return (actionNames == null ? String.valueOf(action) : actionNames[action]);
  }
  
  public List<String> getActionNames() {
    return Arrays.asList(actionNames);
  }

  public String toString() {
    StringBuffer output = new StringBuffer();
    int i = 0;
    for (UnicodeMap unicodeMap : stateToData) {
      String stateName = stateNames == null ? String.valueOf(i) : stateNames[i];
      output.append(stateName).append(":\r\n");
      if (unicodeMap == null) {
        output.append("\tnull\r\n");
      } else {
        for (Object action : unicodeMap.getAvailableValues()) {
          UnicodeSet sources = unicodeMap.keySet(action);
          output.append("\t" + sources.toPattern(false) + "\t" + toString((StateAction)action) + "\r\n");
        }
      }
      i++;
    }
    return output.toString();
  }

  public T parse(CharSequence string, ParsePosition parsePosition) {
    int i = parsePosition.getIndex();
    if (i < 0 || i >= string.length()) {
      throw new StringIndexOutOfBoundsException(i);
    }
    int cp;
    short state = START;
    StateObjectBuilder<T> stateObject = factory.getInstance();
    stateObject.init(string, this, i);
    cp = Character.codePointAt(string, 0);
    if (SHOW_STATE_TRANSITIONS) {
      System.out.println("@Fetched: " + UTF16.valueOf(cp));
    }
    while (true) {
      StateAction action = (StateAction) stateToData[state].getValue(cp);
      if (action.pushState >= 0) {
        stateObject.push(action.pushState);
        if (SHOW_STATE_TRANSITIONS) {
          System.out.println("\t@Pushed " + stateObject);
        }
      }
      if (action.action >= 0) {
        stateObject.handle(i, action);
      }
      switch (state = action.nextState) {
        default:
          if (SHOW_STATE_TRANSITIONS) {
            System.out.println("\t@NextState " + getStateName(state));
          }
        break;
        case POP:
          if (SHOW_STATE_TRANSITIONS) {
            System.out.println("\t@Popping " + stateObject);
          }
          state = stateObject.pop();
          break;
        case EXIT:
          parsePosition.setIndex(i);
          return stateObject.getResult();
        case ERROR:
          parsePosition.setErrorIndex(i);
          throw new IllegalArgumentException(getActionName(action.action));
      }
      if (action.advanceToNextCodePoint) {
        i += UTF16.getCharCount(cp);
        cp = i < string.length() ? Character.codePointAt(string, i) : 0xFFFF;
        if (SHOW_STATE_TRANSITIONS) {
          System.out.println("@Fetched: " + UTF16.valueOf(cp));
        }
      }
    }
  }
}
