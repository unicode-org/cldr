package org.unicode.cldr.util;

import java.io.IOException;

public abstract class StringByteConverter {

  public  void clear() {
    // default implementation does nothing
  }

  /**
   * Converts char of source to output. Result may depend on previous context. Call clear() before first character.
   * @param output buffer to fill
   * @return new byte position
   */
  public abstract int toBytes(char ch, byte[] output, int bytePosition);
  
  /**
   * Converts bytes to char.
   * @param output buffer to fill
   * @return new byte position
   */
  public abstract int fromBytes(byte[] input, int bytePosition, char[] result);

  /**
   * Write a string to a byte array.
   * @param source string to write
   * @param output byte array to write into
   * @param bytePosition place in byte array to start
   * @return new position in byte array
   */
  public int toBytes(CharSequence source, byte[] output, int bytePosition) {
    for (int i = 0; i < source.length(); ++i) {
      bytePosition = toBytes(source.charAt(i), output, bytePosition);
    }
    return bytePosition;
  }

  /**
   * Read a string from a byte array
   * @param input byte array to read from
   * @param byteStart TODO
   * @param byteLength total length of the byte array
   * @param result the result to add on to
   * @return the result, for chaining.
   * @throws IOException 
   */
  public Appendable fromBytes(byte[] input, int byteStart, int byteLength, Appendable result) throws IOException {
    char[] charBuffer = new char[1];
    while (byteStart < byteLength) {
      byteStart = fromBytes(input, byteStart, charBuffer);
      result.append(charBuffer[0]);
    }
    return result;
  }

}