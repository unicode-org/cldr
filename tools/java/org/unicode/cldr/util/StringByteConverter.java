/*
 **********************************************************************
 * Copyright (c) 2006-2007, Google and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;

import java.io.IOException;

/**
 * Class that converts strings to bytes and back. The bytes do not have to be
 * UTF-8, since they are meant to only be used serially. In particular, the only
 * restriction is that the transition between serialized characters must be discoverable by
 * looking at either the last byte of the first character or the first byte of the second character.
 * 
 * @author markdavis
 * 
 */
public abstract class StringByteConverter {

  public void clear() {
    // default implementation does nothing
  }

  /**
   * Return the maximum number of bytes per char.
   * 
   * @return
   */
  public abstract int getMaxBytesPerChar();

  /**
   * Converts char of source to output. Result may depend on previous context.
   * Call clear() before first character, and call toBytes(output, bytePosition) after done.
   * 
   * @param output
   *          buffer to fill
   * @return new byte position
   */
  public abstract int toBytes(char ch, byte[] output, int bytePosition);

  /**
   * Converts final state, if any, to output. Result may depend on previous context.
   * Call clear() before first character, and call toBytes(output, bytePosition) after done.
   * 
   * @param output
   *          buffer to fill
   * @return new byte position
   */
  public  int toBytes(byte[] output, int bytePosition) {
    return bytePosition; // default implementation does nothing
  }
  
  /**
   * Read a string from a byte array. The byte array must be well formed; eg the
   * contents from byteStart to byteLength will not cause errors, and will never
   * overrun. It will always terminate at byteLength. The results are not
   * guaranteed to be the same as would be gotten from inverting toBytes -- that
   * will happen if multiple strings map to the same bytes.
   * 
   * @param input
   *          byte array to read from
   * @param byteStart
   *          TODO
   * @param byteLength
   *          total length of the byte array
   * @param result
   *          the result to add on to
   * @return the result, for chaining.
   * @throws IOException
   */
  public abstract Appendable fromBytes(byte[] input, int byteStart,
      int byteLength, Appendable result);

  /**
   * Write a string to a byte array.
   * 
   * @param source
   *          string to write
   * @param output
   *          byte array to write into
   * @param bytePosition
   *          place in byte array to start
   * @return new position in byte array
   */
  public int toBytes(CharSequence source, byte[] output, int bytePosition) {
    for (int i = 0; i < source.length(); ++i) {
      bytePosition = toBytes(source.charAt(i), output, bytePosition);
    }
    toBytes(output, bytePosition); // cleanup
    return bytePosition;
  }
  
  public byte[]  toBytes(CharSequence source) {
    byte[] buffer = new byte[source.length()*getMaxBytesPerChar()];
    int len = toBytes(source, buffer, 0);
    byte[] result = new byte[len];
    System.arraycopy(buffer, 0, result, 0, len);
    return result;
  }
}