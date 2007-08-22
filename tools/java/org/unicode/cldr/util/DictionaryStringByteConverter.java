/*
 **********************************************************************
 * Copyright (c) 2006-2007, Google and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;

import org.unicode.cldr.util.Dictionary.Matcher;
import org.unicode.cldr.util.Dictionary.Matcher.Status;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

public class DictionaryStringByteConverter extends StringByteConverter {
  private final Dictionary<String> dictionary;
  private final Matcher<String> matcher;
  private final StringByteConverter byteMaker;
  private final StringBuilder buffer = new StringBuilder();
  private final int maxBytesPerChar;
  private Matcher<CharSequence> backMatcher = null;
  
  public Dictionary<String> getDictionary() {
    return dictionary;
  }
  
  public DictionaryStringByteConverter(Dictionary<String> dictionary, StringByteConverter byteMaker) {
    super();
    this.dictionary = dictionary;
    matcher = dictionary.getMatcher();
    matcher.setText(buffer);
    this.byteMaker = byteMaker;
    int mBytesPerChar = 0;
    for (Iterator<Entry<CharSequence, String>> m = dictionary.getMapping(); m.hasNext();) {
      Entry<CharSequence, String> entry = m.next();
      //System.out.println("** " + key + "\t\t" + value);
      int bytesPerChar = entry.getValue().length() * byteMaker.getMaxBytesPerChar(); // all bytes are generated from last char
      if (mBytesPerChar < bytesPerChar) {
        mBytesPerChar = bytesPerChar;
      }
    }
    maxBytesPerChar = mBytesPerChar;
  }
  
  @Override
  public int getMaxBytesPerChar() {
    return maxBytesPerChar;
  }
  
  @Override
  public int toBytes(char ch, byte[] output, int bytePosition) {
    buffer.append(ch);
    return toBytes(output, bytePosition, true);
  }
  
  @Override
  public int toBytes(byte[] output, int bytePosition) {
    return toBytes(output, bytePosition, false);
  }
  
  public int toBytes(byte[] output, int bytePosition, boolean stopOnFinalPartial) {
    // keep converting until the buffer is empty, or unless we get a PARTIAL at the end
    while (buffer.length() != 0) {
      matcher.setText(buffer); // reset the matcher to the start
      // find last, best status
      Status status = Status.NONE;
      int bestEnd = 0;
      String bestValue = null;
      main:
        while (true) {
          Status tempStatus = matcher.next();
          switch (tempStatus) {
            case NONE:
              break main;
            case PARTIAL:
              // if the partial is at the end, then wait for more input
              if (stopOnFinalPartial && matcher.getMatchEnd() == buffer.length()) {
                if (true) matcher.nextUniquePartial(); // for debugging
                return bytePosition;
              }
              continue; // otherwise ignore
            default:
              // MATCH
              status = tempStatus;
            bestEnd = matcher.getMatchEnd();
            bestValue = matcher.getMatchValue();
            break;
          }
        }
      // we've now come out, and have either MATCH or not
      // so replace what we came up with, and continue
      switch (status) {
        case MATCH:
          bytePosition = byteMaker.toBytes(bestValue, output, bytePosition);
          buffer.delete(0, bestEnd);
          break;
        default:
          bytePosition = byteMaker.toBytes(buffer.charAt(0), output, bytePosition);
        buffer.delete(0,1);
        break;
      }
    }
    return bytePosition;
  }
  @Override
  public Appendable fromBytes(byte[] input, int byteStart, int byteLength, Appendable result) {
    // first convert from bytes
    StringBuffer internal = new StringBuffer();
    byteMaker.fromBytes(input, byteStart, byteLength, internal);
    // then convert using dictionary
    if (backMatcher == null) {
      Map<CharSequence, CharSequence> back = new TreeMap<CharSequence, CharSequence>(Dictionary.CHAR_SEQUENCE_COMPARATOR);
      for (Iterator<Entry<CharSequence, String>> m = dictionary.getMapping(); m.hasNext();) {
        Entry<CharSequence, String> entry = m.next();
        if (entry.getValue().length() != 0) {
          if (!back.containsKey(entry.getValue())) {// may lose info
            back.put(entry.getValue(), entry.getKey()); 
          }
        }
      }
      backMatcher = new StateDictionaryBuilder<CharSequence>().make(back).getMatcher();
    }
    backMatcher.setText(internal).convert(result);
    return result;
  }
}