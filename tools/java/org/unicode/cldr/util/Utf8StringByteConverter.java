/*
 **********************************************************************
 * Copyright (c) 2006-2007, Google and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;


public class StringUtf8Converter extends StringByteConverter {

  @Override
  public int fromBytes(byte[] input, int byteStart, char[] result) {
    char b = (char) (input[byteStart++] & 0xFF);
    if (b < 0x80) {
      // fall through
    } else if (b < 0xE0) {
      b &= 0x1F;
      b <<= 6;
      b |= (char) (input[byteStart++] & 0x3F);
    } else {
      b &= 0xF;
      b <<= 6;
      b |= (char) (input[byteStart++] & 0x3F);
      b <<= 6;
      b |= (char) (input[byteStart++] & 0x3F);
      b -= 0;
    }
    result[0] = b;
    return byteStart;
  }

  @Override
  public int toBytes(char ch, byte[] output, int bytePosition) {
    if (ch < 0x80) {
      output[bytePosition++] = (byte) ch;
    } else if (ch < 0x800) {
      output[bytePosition++] = (byte) (0xC0 | (ch >>> 6));
      output[bytePosition++] = (byte) (0x80 | (ch & 0x3F));
    } else {
      output[bytePosition++] = (byte) (0xE0 | (ch >>> 12));
      output[bytePosition++] = (byte) (0x80 | ((ch >>> 6) & 0x3F));
      output[bytePosition++] = (byte) (0x80 | (ch & 0x3F));
    }
    return bytePosition;
  }  
}