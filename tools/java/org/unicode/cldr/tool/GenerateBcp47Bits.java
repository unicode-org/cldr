package org.unicode.cldr.tool;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.tool.GenerateBcp47Bits.Bcp47StringBitTransform.Type;
import org.unicode.cldr.util.Iso639Data;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.CldrUtility;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class GenerateBcp47Bits {
  static final boolean QUICK = false; // true for debugging

  /**
   * Generate the data, throwing an exception if anything goes wrong.
   * @param args
   */
  public static void main(String[] args) {
    // Get the data to compress
    final Collection<String> languages;
    final Collection<String> regions;
    final long[] regionStaticTest;
    final long[] languageStaticTest;
    
    if (QUICK) {
      languages = Arrays.asList("aa zz aaa abc zzz".split("\\s"));
      languageStaticTest = null;
      regions = Arrays.asList("AA AB ZZ 000 003 999".split("\\s"));
      regionStaticTest = null;
    } else {
      languages = new Iso639Data().getAvailable();
      languageStaticTest = null ; // javaTestLanguages;
      regions = StandardCodes.make().getAvailableCodes("territory");
      regionStaticTest = null; // javaTestRegions;
    }

    // Compress it
    System.out.println("\t<bits>");
    writeInfo("region", regions, new Bcp47StringBitTransform(Bcp47StringBitTransform.Type.REGION), regionStaticTest);
    writeInfo("language", languages, new Bcp47StringBitTransform(Bcp47StringBitTransform.Type.LANGUAGE), languageStaticTest);
    System.out.println("\t</bits>");
  }

  /**
   * Utility for writing compression. Work is done in writeBits
   * @param element
   * @param codes
   * @param transform
   * @param staticTest2
   */
  private static void writeInfo(String element, Collection<String> codes, StringBitTransform transform, long[] testStatic) {
    System.out.println("<!-- " + codes + " -->");
    System.out.println("\t\t<" + element + ">");
    writeBits(codes, transform, "\t\t\t\t", testStatic);
    System.out.println("\t\t</" + element + ">");
  }

  /**
   * Write out the bits in the source, using the transform.
   * @param source
   * @param transform
   * @param indent
   * @param testStatic
   */
  private static void writeBits(final Collection<String> source, StringBitTransform transform, String indent, long[] testStatic) {
    Set<String> codes = new TreeSet<String>(source);

    // Transform into bits
    Bits bits = new Bits(transform.getLimit());
    if (QUICK) System.out.println(bits);

    for (String code : codes) {
      int bit = transform.toBit(code);
      String verify = transform.fromBit(bit);
      bits.set(bit);
      // Verify that our transform and bitset worked
      if (QUICK) {
        System.out.println(bit + "\t" + bits);
      }

      if (!verify.equalsIgnoreCase(code)) {
        bit = transform.toBit(code);
        verify = transform.fromBit(bit);
        throw new IllegalArgumentException("StringBitTransform failure with " + code);
      }
      if (!bits.get(bit)) {
        throw new IllegalArgumentException("Bit failure with " + code);
      }
    }
    
    // Verify that the sets are the same afterwards
    if (QUICK) System.out.println(bits);
    Set<String> verifySet = new TreeSet<String>();
    for (int i = 0; i < transform.getLimit(); ++i) {
      if (!bits.get(i)) continue;
      String verify = transform.fromBit(i);
//      if (!codes.contains(verify)) {
//        bits.get(i);
//        transform.fromBit(i);
//        throw new IllegalArgumentException("Roundtrip failure with " + verify);
//      }
      verifySet.add(verify);
    }
    if (QUICK) System.out.println(verifySet);
    if (!verifySet.equals(codes)) {
      throw new IllegalArgumentException("Roundtrip failure with " + verifySet.toString());
    }
    
    // Actually write the result
    final String stringForm = bits.toString(16, indent, 4);
    System.out.println(stringForm);
    
    // Verify that the string form of the bits roundtrips
    Bits reversed = Bits.fromString(transform.getLimit(), stringForm, 16);
    if (QUICK) System.out.println(reversed);
    if (QUICK) System.out.println(reversed.toString(16, indent, 4));
    if (!reversed.equals(bits)) {
      int diff = reversed.firstDifference(bits);
      throw new IllegalArgumentException("Reversal failure" + bits + " != " + reversed);
    }
    
    // Verify that the static versions are the same
    if (testStatic != null) {
      Bits staticTest = Bits.fromInts(transform.getLimit(), testStatic);
      if (!staticTest.equals(bits)) {
        throw new IllegalArgumentException("Static failure");
      }
    }
  }

  /**
   * Simple Bitset (java BitSet doesn't do enough).
   * Note that bits are stored in order, eg 0 => 800..., 1 => 40...
   */
  static class Bits {
    private final long[] bits;
    static final int SHIFT = 6;
    static final int MASK = (1<<SHIFT) - 1;
    
    Bits(int size) {
      bits = new long[((size - 1)>>SHIFT) + 1];
    }
    
    public Bits set(int bit) {
      final int index = bit >> SHIFT;
      final int remainder = bit & MASK;
      int restore = (index << SHIFT) | remainder;
      final long mask = 1L << remainder;
      bits[index] |= mask;
      return this;
    }
    
    public boolean get(int bit) {
      final int index = bit >> SHIFT;
      final int remainder = bit & MASK;
      int restore = (index << SHIFT) | remainder;
      final long mask = 1L << remainder;
      final long masked = bits[index] & mask;
      return 0 != masked;
    }
    
    public String toString(int radix, String indent, int perLineMax) {
      StringBuilder buffer = new StringBuilder();
      buffer.append(indent);
      int count = perLineMax;
      // strip final zeros
      int last = bits.length;
      while(bits[--last] == 0) {}
      // write out up to those
      for (int i = 0; i <= last; ++i) {
        if (i != 0) {
          if (--count == 0) {
            buffer.append(",\n");
            buffer.append(indent);
            count = perLineMax;
          } else {
            buffer.append(", ");
          }
        }
        buffer.append(Long.toString(bits[i], radix));
      }
      return buffer.toString();
    }
    static Bits fromString(int size, String value, int radix) {
      String[] parts = value.trim().split(",\\s*|\\s+");
      if (parts.length > size) {
        throw new IllegalArgumentException("Too many integers");
      }
      Bits result = new Bits(size);
      for (int i = 0; i < parts.length; ++i) {
        result.bits[i] = Long.parseLong(parts[i], radix);
      }
      return result;
    }
    static Bits fromInts(int size, long[] ints) {
      Bits result = new Bits(size);
      if (ints.length > result.bits.length) {
        throw new IllegalArgumentException("Too many integers");
      }
      for (int i = 0; i < ints.length; ++i) {
        result.bits[i] = ints[i];
      }
      return result;
    }
    public boolean equals(Bits other) {
      if (bits.length != other.bits.length) {
        return false;
      }
      for (int i = 0; i < bits.length; ++i) {
        if (bits[i] != other.bits[i]) {
          return false;
        }
      }
      return true;
    }
    public int firstDifference(Bits other) {
      int limit = other.bits.length;
      if (limit < bits.length) limit = bits.length;
      limit = limit << SHIFT;
      for (int i = 0; i < limit; ++i) {
        if (get(i) != other.get(i)) {
          return i;
        }
      }
      return -1;
    }
    
    public String toString() {
      StringBuilder result = new StringBuilder("[");
      int limit = bits.length;
      limit = limit << SHIFT;
      for (int i = 0; i < limit; ++i) {
        if (get(i)) {
          if (result.length() > 1) {
            result.append(",");
          }
          result.append(i);
        }
      }
      result.append("]");
      return result.toString();
    }
  }

  /**
   * Encapsulates a transformation between string and bit
   */
  static interface StringBitTransform {
    public int getLimit();
    public int toBit(String string);
    public String fromBit(int bit);
  }
  
  static class Bcp47StringBitTransform implements StringBitTransform {
    enum Type {LANGUAGE, REGION};

    Type type;
    int limit;
    
    Bcp47StringBitTransform(Type type) {
      this.type = type;
      limit = type == Type.LANGUAGE ? 26*26 + 26*26*26 : 26*26 + 1000;
    }
    
    public int getLimit() {
      return limit;
    }
    
    static final UnicodeSet alpha = (UnicodeSet) new UnicodeSet("[a-z]").freeze();
    static final UnicodeSet num = (UnicodeSet) new UnicodeSet("[0-9]").freeze();

    public int toBit(String string) {
      string = UCharacter.toLowerCase(ULocale.ENGLISH, string);
      switch (string.length()) {
        case 2:
          if (!alpha.containsAll(string)) {
            throw new IllegalArgumentException(string);
          }
          return
          (string.codePointAt(0)- 'a')*26
          + (string.codePointAt(1)- 'a');
        case 3:
          if (alpha.containsAll(string)) {
            return 26*26 +
            (string.codePointAt(0)- 'a')*26*26
            + (string.codePointAt(1)- 'a')*26
            + (string.codePointAt(2)- 'a');
          }
          if (!num.containsAll(string)) {
            throw new IllegalArgumentException(string);
          }
          return 26*26 + Integer.parseInt(string);
        default:
          throw new IllegalArgumentException(string);
      }
    }

    public String fromBit(int bit) {
      StringBuilder result = new StringBuilder();
      if (bit < 0) {
        throw new IllegalArgumentException(String.valueOf(bit));
      }
      if (bit < 26*26) {
        result.appendCodePoint('a' + bit / 26)
        .appendCodePoint('a' + bit % 26);
      } else {
        bit -= 26*26;
        if (type == Type.LANGUAGE) {
          if (bit >= 26*26 + 26*26*26) {
            throw new IllegalArgumentException(String.valueOf(bit));
          }
          result.appendCodePoint('a' + bit / (26*26));
          bit %= (26*26);
          result.appendCodePoint('a' + bit / 26)
          .appendCodePoint('a' + bit % 26);
        } else {
          if (bit >= 26*26 + 1000) {
            throw new IllegalArgumentException(String.valueOf(bit));
          }
          result.append(bit / (10*10));
          bit %= (10*10);
          result.append(bit / 10)
          .append(bit % 10);

        }
      }
      if (type == Type.REGION) return result.toString().toUpperCase(Locale.ENGLISH);
      return result.toString();
    }
  }
  
  /**
   * The following are for testing, using generated data.
   */
  
  static long [] javaTestRegions = {};

  static long [] javaTestLanguages = {};
  
}
