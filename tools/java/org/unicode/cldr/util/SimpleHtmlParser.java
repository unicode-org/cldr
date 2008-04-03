package org.unicode.cldr.util;

import java.io.IOException;
import java.io.Reader;

/**
 * Extremely simple class for parsing HTML. Extremely lenient. Call next() until
 * DONE is returned.
 * <p>
 * Element content will be returned in the following sequence:
 * 
 * <pre>
 *  ELEMENT_START  
 *  ELEMENT strong
 *  ELEMENT_END 
 *  ELEMENT_CONTENT Alphabetic code
 *  ELEMENT_START 
 *  ELEMENT_POP 
 *  ELEMENT strong
 *  ELEMENT_END 
 * </pre>
 * 
 * while attributes will be returned as:
 * 
 * <pre>
 *  ELEMENT_START 
 *  ELEMENT div
 *  ATTRIBUTE id
 *  ATTRIBUTE_CONTENT mainContent
 *  ELEMENT_END
 * </pre>
 * 
 * 
 * @author markdavis
 * 
 */
public class SimpleHtmlParser {
  public enum Type {
    DONE, ELEMENT_START, ELEMENT, ATTRIBUTE, ATTRIBUTE_CONTENT, ELEMENT_END, ELEMENT_POP, QUOTE, ELEMENT_CONTENT
  };

  private enum State {
    BASE, IN_ELEMENT, AFTER_ELEMENT, IN_CONTENT, IN_ATTRIBUTE, IN_ATTRIBUTE_CONTENT, IN_ATTRIBUTE_CONTENT1, IN_ATTRIBUTE_CONTENT2, ELEMENT_STOP, IN_QUOTE
  };

  private Reader input;

  private State state;

  private Type bufferedReturn;

  public SimpleHtmlParser setReader(Reader input) {
    this.input = input;
    state = State.IN_CONTENT;
    bufferedReturn = null;
    return this;
  }

  public Type next(StringBuilder result) throws IOException {
    result.setLength(0);
    if (bufferedReturn != null) {
      if (bufferedReturn == Type.DONE) { // once DONE, stay DONE
        return Type.DONE;
      }
      Type temp = bufferedReturn;
      bufferedReturn = null;
      return temp;
    }
    while (true) {
      char ch;
      {
        int chi = input.read();
        if (chi < 0) {
          bufferedReturn = Type.DONE;
          chi = 0;
        }
        ch = (char) chi;
      }

      switch (state) {
        case BASE:
          if (ch == 0xFEFF)
            break;
        // fall through!

        case IN_CONTENT:
          if (ch == '<') {
            state = State.IN_ELEMENT;
            bufferedReturn = Type.ELEMENT_START;
            return Type.ELEMENT_CONTENT;
          }
          if (ch == 0) {
            return Type.ELEMENT_CONTENT;
          }
          result.append(ch);
          break;

        case IN_ELEMENT:
          if (ch <= ' ') {
            if (equals(result, "!--")) {
              state = State.IN_QUOTE;
              result.setLength(0);
              break;
            }
            state = State.AFTER_ELEMENT;
            return Type.ELEMENT;
          }
          if (ch == '>') {
            state = State.IN_CONTENT;
            bufferedReturn = Type.ELEMENT_END;
            return Type.ELEMENT;
          }
          if (ch == '/') {
            return Type.ELEMENT_POP;
          }
          result.append(ch);
          break;

        case AFTER_ELEMENT:
          if (ch <= ' ')
            break;
          if (ch == '>') {
            state = State.IN_CONTENT;
            return Type.ELEMENT_END;
          }
          result.append(ch);
          state = State.IN_ATTRIBUTE;
          break;

        case IN_ATTRIBUTE:
          if (ch <= ' ') {
            state = State.AFTER_ELEMENT;
            return Type.ATTRIBUTE;
          }
          if (ch == '>') {
            state = State.IN_CONTENT;
            bufferedReturn = Type.ELEMENT_END;
            return Type.ATTRIBUTE;
          }
          if (ch == '=') {
            state = State.IN_ATTRIBUTE_CONTENT;
            return Type.ATTRIBUTE;
          }
          result.append(ch);
          break;

        case IN_ATTRIBUTE_CONTENT:
          if (ch <= ' ') {
            break;
          }
          if (ch == '>') {
            state = State.IN_CONTENT;
            bufferedReturn = Type.ELEMENT_END;
            return Type.ATTRIBUTE_CONTENT;
          }
          if (ch == '\'') {
            state = State.IN_ATTRIBUTE_CONTENT1;
            break;
          }
          if (ch == '"') {
            state = State.IN_ATTRIBUTE_CONTENT2;
            break;
          }
          result.append(ch);
          break;

        case IN_ATTRIBUTE_CONTENT1:
          if (ch == 0 || ch == '\'') {
            state = State.AFTER_ELEMENT;
            return Type.ATTRIBUTE_CONTENT;
          }
          result.append(ch);
          break;

        case IN_ATTRIBUTE_CONTENT2:
          if (ch == 0 || ch == '"') {
            state = State.AFTER_ELEMENT;
            return Type.ATTRIBUTE_CONTENT;
          }
          result.append(ch);
          break;

        case IN_QUOTE:
          if (ch == 0) {
            state = State.IN_CONTENT;
            return Type.QUOTE;
          }
          if (ch == '>' && endsWith(result, "--")) {
            result.setLength(result.length()-2);
            state = State.IN_CONTENT;
            return Type.QUOTE;
          }
          result.append(ch);
          break;
      }
    }
  }

  public static final boolean endsWith(CharSequence a, CharSequence b) {
    int aStart = a.length() - b.length();
    if (aStart < 0) {
      return false;
    }
    return regionEquals(a, aStart , b, 0, b.length());
  }

  public static final boolean equals(CharSequence a, CharSequence b) {
    int len = a.length();
    if (len != b.length()) {
      return false;
    }
    return regionEquals(a, 0, b, 0, len);
  }

  public static boolean regionEquals(CharSequence a, int i, CharSequence b, int j, int len) {
    for (; --len >= 0; ++i, ++j){
      if (a.charAt(i) != b.charAt(j)) {
        return false;
      }
    }
    return true;
  }
  
  public static void writeResult(Type type, StringBuilder result, Appendable writer) throws IOException {
    switch (type) {
      case ELEMENT:
        writer.append(result);
        break;
      case ELEMENT_START:
        writer.append('<');
        break;
      case ELEMENT_END:
        writer.append('>');
        break;
      case ATTRIBUTE:
        writer.append(' ').append(result);
        break;
      case ATTRIBUTE_CONTENT:
        writer.append("=\"").append(result).append('"');
        break;
      case ELEMENT_CONTENT:
        writer.append(result);
        break;
      case ELEMENT_POP:
        writer.append('/');
        break;
      case QUOTE:
        writer.append(result);
        break;
      case DONE:
        break;
      default:
        throw new IllegalArgumentException("Missing case: " + type);         
    }
  }
}