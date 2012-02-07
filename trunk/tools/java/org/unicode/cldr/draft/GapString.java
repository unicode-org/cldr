package org.unicode.cldr.draft;

/**
 * Class that permits quick edits; does insertions/deletions much faster, in general, than StringBuilder. This is based on the fact
 * that such changes are generally not random; instead they tend to happen more often in either nearby or successive locations.
 * The structure of the class keeps 2 buffers; one for the front and one for the end. 
 * @author markdavis
 */
// TODO investigate whether keeping the future in reverse order is the right approach in terms of performance.
public class GapString implements CharSequence {
  private static final int GROWTH_INCREMENT = 15;
  private static final int GROWTH_FACTOR = 3;
  private char[] buffer = new char[10];
  private int pastLength = 0;
  private int gapLength = 10;
  
  public GapString() {}
  public GapString(CharSequence s) {
    insert(0,s);
  }
  
  public char charAt(int index) {
    try {
      return buffer[index < pastLength ? index : index + gapLength];
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new StringIndexOutOfBoundsException(index);
    }
  }

  public int length() {
    return buffer.length - gapLength;
  }

  public CharSequence subSequence(int start, int end) {
    // TODO optimize this
    return toString().subSequence(start, end);
  }
  
  public String toString() {
    StringBuilder b = new StringBuilder();
    // check to see whether second argument is length or count
    b.append(buffer, 0, pastLength);
    final int futureStart = pastLength + gapLength;
    b.append(buffer, futureStart, buffer.length - futureStart);
    return b.toString();
  }
  
  public GapString insert(int index, CharSequence s, int start, int end) {
    if (s instanceof String) {
      return insert(index, (String) s, start, end);
    }
    final int gapNeeded = end - start;
    if (pastLength != index) {
      shiftTo(index, gapNeeded);
    } else {
      final int growthNeeded = gapNeeded - gapLength;
      if (growthNeeded > 0) {
        growToLength((buffer.length + growthNeeded)*GROWTH_FACTOR + GROWTH_INCREMENT); 
      }
    }
    for (int i = start; i < end; ++i) {
      buffer[pastLength++] = s.charAt(i);
    }
    gapLength -= gapNeeded;
    return this;
  }

  public GapString insert(int index, CharSequence s) {
    return insert(index, s, 0, s.length());
  }
  
  public GapString insert(int index, String s) {
    return insert(index, s, 0, s.length());
  }
  
  public GapString insert(int index, String s, int start, int end) {
    final int gapNeeded = end - start;
    if (pastLength != index) {
      shiftTo(index, gapNeeded);
    } else {
      final int growthNeeded = gapNeeded - gapLength;
      if (growthNeeded > 0) {
        growToLength((buffer.length + growthNeeded)*GROWTH_FACTOR + GROWTH_INCREMENT); 
      }
    }
    s.getChars(start, end, buffer, pastLength);
    pastLength += (end - start);
    gapLength -= gapNeeded;
    return this;
  }
  
  public GapString insert(int index, char[] s) {
    return insert(index, s, 0, s.length);
  }
  
  public GapString insert(int index, char[] s, int start, int end) {
    final int gapNeeded = end - start;
    if (pastLength != index) {
      shiftTo(index, gapNeeded);
    } else {
      final int growthNeeded = gapNeeded - gapLength;
      if (growthNeeded > 0) {
        growToLength((buffer.length + growthNeeded)*GROWTH_FACTOR + GROWTH_INCREMENT); 
      }
    }
    System.arraycopy(s, start, buffer, pastLength, gapNeeded);
    pastLength += (end - start);
    gapLength -= gapNeeded;
    return this;
  }
  
  public GapString insert(int index, char s) {
    if (pastLength != index || pastLength < index + 1) {
      shiftTo(index, 1);
    }
    buffer[pastLength++] = s;
    gapLength -= 1;
    return this;
  }
  
  
  public GapString append(boolean x) {
    return insert(length(), String.valueOf(x));
  }
  
  public GapString append(char x) {
    return insert(length(), x);
  }
  
  public GapString append(char[] x) {
    return insert(length(), x);
  }
  public GapString append(char[] x, int start, int end) {
    return insert(length(), x, start, end);
  }
  
  public GapString append(double x) {
    return insert(length(), String.valueOf(x));
  }
  
  public GapString append(float x) {
    return insert(length(), String.valueOf(x));
  }
  
  public GapString append(int x) {
    return insert(length(), String.valueOf(x));
  }
  
  public GapString append(CharSequence x, int start, int end) {
    return insert(length(), x, start, end);
  }
  
  public GapString append(Object x) {
    return insert(length(), String.valueOf(x));
  }
  
  public GapString append(long x) {
    return insert(length(), String.valueOf(x));
  }
  
  public GapString appendCodePoint(int x) {
    return insertCodePoint(length(), x);
  }
  
  private GapString insertCodePoint(int index, int x) {
    if (x < 0x10000) {
      return insert(index, (char)x);
    }
    return insert(index, Character.toChars(x)); // rare, so inefficiency doesn't matter
  }
  
  public GapString append(String s) {
    return insert(buffer.length - gapLength, s, 0, s.length());
  }
  
  public GapString append(CharSequence string) {
    return insert(buffer.length - gapLength, string);
  }
  
  public GapString delete(int start, int end) {
    // if our deletion includes the gap, we can shortcut this
    // we just reset the pastLength and gap
    if (pastLength >= start && pastLength < end) {
      pastLength = start;
    } else {
      // TODO There is a possible optimization, to only move enough to get to one end or the other.
      // However, I don't know whether it would be worth it or not: have to test.
      if (pastLength != start) {
        shiftTo(start,0);
      }
    }
    gapLength += (end - start);
    return this;
  }
  
  public GapString replace(int start, int end, CharSequence other, int otherStart, int otherEnd) {
    delete(start,end);
    return insert(start, other, otherStart, otherEnd);
  }
  
  public GapString compact() {
    if (gapLength > 0) {
      growToLength(buffer.length - gapLength); // remove any gap
    }
    return this;
  }
  
  public boolean equals(Object other) {
    try {
      return equals((CharSequence) other);
    } catch (Exception e) {
      return false;
    }
  }
  
  public boolean equals(CharSequence other) {
    final int len = buffer.length - gapLength;
    if (other.length() != len) {
      return false;
    }
    int i = 0;
    for (; i < pastLength; ++i) {
      if (buffer[i] != other.charAt(i)) {
        return false;
      }
    }
    int j = i;
    i += gapLength;
    for (; i < buffer.length; ++i) {
      if (buffer[i] != other.charAt(j++)) {
        return false;
      }
    }
    return true;
  }
  
  public int hashCode() {
    int result = 0;
    int i = 0;
    for (; i < pastLength; ++i) {
      result = 37*result + buffer[i];
    }
    i += gapLength;
    for (; i < buffer.length; ++i) {
      result = 37*result + buffer[i];
    }
    return result;
  }
  
  // ======== PRIVATES ===========
  
  /**
   * This utility function just shifts the gap, so that it starts at newPastLength. The logical contents
   * of the string are unchanged.
   */
  private void shiftTo(int newPastLength, int gapNeeded) {
    int growth = gapNeeded - gapLength;
    if (growth > 0) {
      growToLength((buffer.length + growth)*GROWTH_FACTOR + GROWTH_INCREMENT);
    }

    int newMinusOldPastLength = newPastLength - pastLength;
    if (newMinusOldPastLength == 0) {
      return;
    }
    if (newMinusOldPastLength > 0) {
      System.arraycopy(buffer, pastLength + gapLength, buffer, pastLength, newMinusOldPastLength);
    } else {
      System.arraycopy(buffer, newPastLength, buffer, pastLength + gapLength + newMinusOldPastLength, -newMinusOldPastLength);
    }
    pastLength = newPastLength;
  }

  /**
   * This utility function just grows the gap (and thus the storage). The logical contents
   * of the string are unchanged.
   */
  private void growToLength(int neededLength) {
    char[] temp = new char[neededLength];
    System.arraycopy(buffer, 0, temp, 0, pastLength);
    final int futureStart = pastLength + gapLength;
    final int futureLength = buffer.length - futureStart;
    System.arraycopy(buffer, futureStart, temp, neededLength - futureLength, futureLength);
    gapLength += neededLength - buffer.length;
    buffer = temp;
  }

}
