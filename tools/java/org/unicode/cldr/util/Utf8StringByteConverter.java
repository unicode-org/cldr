/*
 **********************************************************************
 * Copyright (c) 2006-2007, Google and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;

import java.io.IOException;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;

/**
 * @author markdavis
 */
// TODO optimize this
public class Utf8StringByteConverter extends StringByteConverter {
  char lead = 0;
  
  @Override
  public int toBytes(char ch, byte[] output, int bytePosition) {
    // we may have state, if we were processing a supplemental char
    if (lead != 0) {
      if (UTF16.isTrailSurrogate(ch)) {
        int cp = UCharacter.getCodePoint(lead, ch);
        output[bytePosition++] = (byte) (0xF0 | (cp >>> 18));
        output[bytePosition++] = (byte) (0x80 | ((cp >>> 12) & 0x3F));
        output[bytePosition++] = (byte) (0x80 | ((cp >>> 6) & 0x3F));
        output[bytePosition++] = (byte) (0x80 | (cp & 0x3F));
        lead = 0;
        return bytePosition;
      }
      // write lead
      output[bytePosition++] = (byte) (0xE0 | (lead >>> 12));
      output[bytePosition++] = (byte) (0x80 | ((lead >>> 6) & 0x3F));
      output[bytePosition++] = (byte) (0x80 | (lead & 0x3F));
      lead = 0; 
    }
    if (ch < 0x80) {
      output[bytePosition++] = (byte) ch;
    } else if (ch < 0x800) {
      output[bytePosition++] = (byte) (0xC0 | (ch >>> 6));
      output[bytePosition++] = (byte) (0x80 | (ch & 0x3F));
    } else if (ch >= 0xD800 && ch < 0xDC00) {
      lead = ch;
    } else {
      output[bytePosition++] = (byte) (0xE0 | (ch >>> 12));
      output[bytePosition++] = (byte) (0x80 | ((ch >>> 6) & 0x3F));
      output[bytePosition++] = (byte) (0x80 | (ch & 0x3F));
    }
    return bytePosition;
  }
  
  @Override
  public int toBytes(byte[] output, int bytePosition) {
    if (lead != 0) {
      output[bytePosition++] = (byte) (0xE0 | (lead >>> 12));
      output[bytePosition++] = (byte) (0x80 | ((lead >>> 6) & 0x3F));
      output[bytePosition++] = (byte) (0x80 | (lead & 0x3F));
      lead = 0; 
    }
    return bytePosition;
  }

  @Override
  public int getMaxBytesPerChar() {
    return 4;
  }

  @Override
  public Appendable fromBytes(byte[] input, int byteStart, int byteLength,
      Appendable result) {
    try {
      while (byteStart < byteLength) {
        char b = (char) (input[byteStart++] & 0xFF);
        if (b < 0x80) {
          // fall through
        } else if (b < 0xE0) {
          b &= 0x1F;
          b <<= 6;
          b |= (char) (input[byteStart++] & 0x3F);
        } else if (b < 0xF0) {
          b &= 0xF;
          b <<= 6;
          b |= (char) (input[byteStart++] & 0x3F);
          b <<= 6;
          b |= (char) (input[byteStart++] & 0x3F);
        } else {
          // surrogate
          int cp = (b & 0x7) << 6;
          cp |= (char) (input[byteStart++] & 0x3F);
          cp <<= 6;
          cp |= (char) (input[byteStart++] & 0x3F);
          cp <<= 6;
          cp |= (char) (input[byteStart++] & 0x3F);
          result.append(UTF16.getLeadSurrogate(cp));
          b = UTF16.getTrailSurrogate(cp);
        }
        result.append(b);
      }
      return result;
    } catch (IOException e) {
      throw (IllegalArgumentException) new IllegalArgumentException("Internal error").initCause(e);
    }
  }
}