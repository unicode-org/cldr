package org.unicode.cldr.util;

import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UnicodeSet;

import java.io.UnsupportedEncodingException;
import java.util.Random;

public class ByteString {
  static boolean DEBUG = false;
  
  static Random random = new Random(0);
  
  public static void main(String[] args) throws UnsupportedEncodingException {
    byte[] bytes = new byte[1000];
    StringBuilder chars = new StringBuilder();
    int[] ioBytePosition = new int[1];
    
    String testString = "Mauritania";
    for (int i = 0; i < testString.length(); ++i) {
      testInt(testString.charAt(i));
    }

    //if (true) return;

    testInt( 0);
    testInt(0x3F);
    testInt(0x40);
    testInt(-0x3F);
    testInt(-0x40);

    // test read/write int
    for (int i = 0; i < 100; ++i) {
      int test = (int) (random.nextGaussian()*0xFF);
      testInt(test);
    }
    for (int i = -1; i != 0; i >>>= 1) {
      testInt(i);
    }
    for (int i = 1; i != 0; i <<= 1) {
      testInt(i);
    }
    
    // test read/write string
    UnicodeSet repertoire = new UnicodeSet("[[\\u0000-\\u03FF]&[:script=Greek:]]").complement().complement();
    for (int i = 0; i < 100; ++i) {
      String test = getRandomString(1, 6, repertoire);
      testString(test, false);
      testString(test, true);
    }
    System.out.println("utf8/comp: " + totalUtf8Bytes + "/" + totalBytes + " = " + (totalUtf8Bytes/(double)totalBytes));
    
    
    testWithLocale("en", true);
    testWithLocale("en", false);
    testWithLocale("el", true);
    testWithLocale("el", false);
    testWithLocale("ja", true);
    testWithLocale("ja", false);
    testWithLocale("hi", true);
    testWithLocale("hi", false);
    testWithLocale("ar", true);
    testWithLocale("ar", false);
  }

  private static void testWithLocale(String locale, boolean deltaEncoded) throws UnsupportedEncodingException {
    totalUtf8Bytes = totalBytes = 0;
    Factory cldrFactory = Factory.make(org.unicode.cldr.util.Utility.MAIN_DIRECTORY, ".*");
    CLDRFile file = cldrFactory.make(locale, false);
    for (String path : file) {
      if (path.contains("exemplarCh")) {
        continue;
      }
      String value = file.getStringValue(path);
      testString(value, deltaEncoded);
    }
    System.out.println(locale + "\t" + (deltaEncoded ? 'd' : 'u') + "\tutf8/comp: " + totalUtf8Bytes + "/" + totalBytes + " = " + (totalUtf8Bytes/(double)totalBytes));
  }
  
  static int counter = 1;

  private static void testString(String test, boolean deltaEncoded) throws UnsupportedEncodingException {
    byte[] bytes = new byte[2000];
    StringBuilder chars = new StringBuilder();
    if (DEBUG) {
      System.out.println(counter++ + ": " + Utility.hex(test) + ", \t" + test);
    }
    int byteLen = toBytes(test,0,bytes, deltaEncoded);
    totalBytes += byteLen;
    if (DEBUG) {
      System.out.println("\t" + hex(bytes,byteLen," "));
    }
    byte[] utf8bytes = test.getBytes("utf-8");
    if (DEBUG) {
      System.out.println("\t" + hex(utf8bytes,utf8bytes.length," "));
    }
    totalUtf8Bytes += utf8bytes.length;
    chars.setLength(0);
    StringBuilder retest = fromBytes(bytes, byteLen, chars, deltaEncoded);
    if (!test.equals(retest.toString())) {
      throw new IllegalArgumentException("Fails: <" + Utility.hex(test) + ">, <" + retest + ">");
    }
  }
  static int totalBytes = 0;
  static int totalUtf8Bytes = 0;

  private static void testInt(int test) throws UnsupportedEncodingException {
    if (DEBUG) {
      System.out.println(counter++ + ": " + Utility.hex(test));
    }
    testInt(test, false);
    testInt(test, true);
    if (test >= 0 && test < 0x110000) {
      String test2 = new String(Character.toChars(test));
      byte[] utf8bytes = test2.getBytes("utf-8");
      if (DEBUG) {
        System.out.println("\tutf: " + hex(utf8bytes,utf8bytes.length," "));
      }
    }
  }
  
  private static void testInt(int test, boolean unsigned) throws UnsupportedEncodingException {
    byte[] bytes = new byte[1000];
    int[] ioBytePosition = new int[1];
    int len = unsigned ? writeUnsignedInt(test, bytes, 0) : writeInt(test, bytes, 0);
    if (DEBUG) {
      System.out.println("\tcm" + (unsigned ? "u" : "s") + ": " + hex(bytes, len, " "));
    }
    ioBytePosition[0] = 0;
    int retest = unsigned ? readUnsignedInt(bytes, ioBytePosition) : readInt(bytes, ioBytePosition);
    int lengthRead = ioBytePosition[0];
    if (test != retest) throw new IllegalArgumentException();
    if (len != lengthRead) throw new IllegalArgumentException();
  }
  
  private static String hex(byte[] bytes, int len, String separator) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < len; ++i) {
      if (result.length() != 0) {
        result.append(separator);
      }
      result.append(Utility.hex(bytes[i]&0xFF,2));
    }
    return result.toString();
  }

  private static String getRandomString(int minLen, int maxLen, UnicodeSet repertoire) {
    StringBuilder result = new StringBuilder();
    int len = random.nextInt(maxLen - minLen + 1) + minLen;
    for (int i = 0; i < len; ++i) {
      result.appendCodePoint(repertoire.charAt(random.nextInt(repertoire.size())));
    }
    return result.toString();
  }

  public static int toBytes(String source, int bytePosition, byte[] output, boolean deltaEncoded) {
    if (deltaEncoded) {
      int cp = 0;
      int last = 0x40;
      for (int i = 0; i < source.length(); i += Character.charCount(cp)) {
        cp = source.codePointAt(i);
        // get the delta from the previous
        int delta = cp - last;
        bytePosition = writeInt(delta, output, bytePosition);
        last = cp;
        last = (last & ~0x7F) | 0x40; // position in middle of 128 block
      }
    } else {
      int cp = 0;
      for (int i = 0; i < source.length(); i += Character.charCount(cp)) {
        cp = source.codePointAt(i);
        bytePosition = writeUnsignedInt(cp, output, bytePosition);
      }
    }
    return bytePosition;
  }
  
  
  public static StringBuilder fromBytes(byte[] input, int byteLength, StringBuilder result, boolean deltaEncoded) {
    int[] ioBytePosition = new int[1];
    ioBytePosition[0] = 0;
    if (deltaEncoded) {
      int last = 0x40;
      while (ioBytePosition[0] < byteLength) {
        int delta = readInt(input, ioBytePosition);
        last += delta;
        if (DEBUG) {
          System.out.println("\t\t" + Utility.hex(last));
        }
        result.appendCodePoint(last);
        last = (last & ~0x7F) | 0x40; // position in middle of 128 block
      }
    } else {
      while (ioBytePosition[0] < byteLength) {
        int last = readUnsignedInt(input, ioBytePosition);
        if (DEBUG) {
          System.out.println("\t\t" + Utility.hex(last));
        }
        result.appendCodePoint(last);
      }
    }
    return result;
  }

  public static int writeInt(int source, byte[] output, int bytePosition) {
    // grab sign bit
    int sign = 0;
    if (source < 0) {
      sign = 0x40;
      source = ~source;
    }
    // now output into bytes, 7 bits at a time
    // stop when we only have 6 bits left
    if (DEBUG) {
      System.out.println(Utility.hex(source));
    }
    // find the first non-zero byte. We have 6 bits in the bottom byte, otherwise seven per
    // so that leave 32 - 6 + 3*7 = 5
    int mask = ~0x3F;
    int offset = -1;
    while ((source & mask) != 0) {
      offset += 7;
      mask <<= 7;
    }
    for ( ; offset > 0; offset -= 7) {
      output[bytePosition++] = (byte)((source >> offset) & 0x7F); 
      if (DEBUG) {
        System.out.println(Utility.hex(output[bytePosition-1]&0xFF,2));
      }
    }
    // last byte is signed, with real sign in bit 6
    output[bytePosition++] = (byte)(0x80 | sign | (source & 0x3F));
    if (DEBUG) {
      System.out.println(Utility.hex(output[bytePosition-1]&0xFF,2));
    }
    return bytePosition;
  }
  
  public static int readInt(byte[] input, int[] ioBytePosition) {
    int result = 0;
    int bytePosition = ioBytePosition[0];
    while (true) {
      // add byte
      int nextByte = input[bytePosition++];
      if (nextByte >= 0) {
        result <<= 7;
        result |= nextByte;
        if (DEBUG) {
          System.out.println(Utility.hex(nextByte&0xFF,2) + ", " + Utility.hex(result));
        }
      } else { // < 0
        result <<= 6;
        result |= nextByte & 0x3F;
        if ((nextByte & 0x40) != 0) {
          result = ~result;
        }
        if (DEBUG) {
          System.out.println(Utility.hex(nextByte&0xFF,2) + ", " + Utility.hex(result));
        }
        ioBytePosition[0] = bytePosition;
        return result;
      }
    }
  }
  
  public static int writeUnsignedInt(int source, byte[] output, int bytePosition) {
    if (DEBUG) {
      System.out.println(Utility.hex(source));
    }
    // find the first non-zero byte. We have 6 bits in the bottom byte, otherwise seven per
    // so that leave 32 - 6 + 3*7 = 5
    int mask = ~0x7F;
    int offset = 0;
    while ((source & mask) != 0) {
      offset += 7;
      mask <<= 7;
    }
    for ( ; offset > 0; offset -= 7) {
      output[bytePosition++] = (byte)((source >> offset) & 0x7F); 
      if (DEBUG) {
        System.out.println(Utility.hex(output[bytePosition-1]&0xFF,2));
      }
    }
    output[bytePosition++] = (byte)(0x80 | source);
    if (DEBUG) {
      System.out.println(Utility.hex(output[bytePosition-1]&0xFF,2));
    }
    return bytePosition;
  }
  
  public static int readUnsignedInt(byte[] input, int[] ioBytePosition) {
    int result = 0;
    int bytePosition = ioBytePosition[0];
    while (true) {
      // add byte
      int nextByte = input[bytePosition++];
      if (nextByte >= 0) {
        result <<= 7;
        result |= nextByte;
        if (DEBUG) {
          System.out.println(Utility.hex(nextByte&0xFF,2) + ", " + Utility.hex(result));
        }
      } else { // < 0
        result <<= 7;
        result |= nextByte & 0x7F;
        if (DEBUG) {
          System.out.println(Utility.hex(nextByte&0xFF,2) + ", " + Utility.hex(result));
        }
        ioBytePosition[0] = bytePosition;
        return result;
      }
    }
  }

}