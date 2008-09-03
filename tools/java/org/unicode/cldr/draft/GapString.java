package org.unicode.cldr.draft;

/**
 * Class that permits quick edits; does insertions/deletions faster, in general, than StringBuilder. This is based on the fact
 * that such changes are generally not random; instead they tend to happen more often in either nearby or successive locations.
 * The structure of the class keeps 2 buffers; one for the front and one for the end. 
 * @author markdavis
 */
// TODO investigate whether keeping the future in reverse order is the right approach in terms of performance.
public class GapString implements CharSequence {
  private char[] past = new char[10];
  private int pastLength = 0;
  private char[] future = new char[10]; // reverse order!!
  private int futureLength = 0;
  
  public GapString() {}
  public GapString(CharSequence s) {
    insert(0,s);
  }
  
  public char charAt(int index) {
    try {
      if (index < pastLength) {
        return past[index];
      }
      return future[futureLength - (index - pastLength)];
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new StringIndexOutOfBoundsException(index);
    }
  }

  public int length() {
    return pastLength + futureLength;
  }

  public CharSequence subSequence(int start, int end) {
    // TODO optimize this
    return toString().subSequence(start, end);
  }
  
  public String toString() {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < pastLength; ++i) {
      b.append(past[i]);
    }
    for (int i = futureLength - 1; i >= 0; --i) {
      b.append(future[i]);
    }
    return b.toString();
  }
  
  public GapString insert(int index, CharSequence s) {
    if (pastLength != index) {
      shiftTo(index);
    }
    for (int i = 0; i < s.length(); ++i) {
      past[pastLength++] = s.charAt(i);
    }
    return this;
  }
  
  public GapString insert(int index, char s) {
    if (pastLength != index) {
      shiftTo(index);
    }
    past[pastLength++] = s;
    return this;
  }
  
  public GapString delete(int index, int end) {
    // TODO optimize this
    // if pastLength is between index and end, then can remove from both sides.
    // if not, only move enough to get to one end or the other.
    if (pastLength != index) {
      shiftTo(index);
    }
    futureLength -= (end - index);
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
    final int len = length();
    if (other.length() != len) {
      return false;
    }
    for (int i = 0; i < pastLength; ++i) {
      if (past[i] != other.charAt(i)) {
        return false;
      }
    }
    int delta = len - 1;
    for (int i = futureLength-1; i >= 0; --i) {
      if (future[i] != other.charAt(delta - i)) {
        return false;
      }
    }
    return true;
  }
  
  public int hashCode() {
    int result = 0;
    for (int i = 0; i < pastLength; ++i) {
      result = 37*result + past[i];
    }
    for (int i = futureLength-1; i >= 0; --i) {
      result = 37*result + future[i];
    }
    return result;
  }
  // ======== PRIVATES ===========
  
  private void shiftTo(int index) {
    if (pastLength < index) {
      if (past.length < index) {
        past = grow(past, pastLength, index*3/2 + 10);
      }
      final int end = index - pastLength;
      for (int i = 0; i < end; ++i) {
        past[pastLength++] = future[--futureLength];
      }
    } else if (pastLength > index) {
      int futureNeeded = length() - index;
      if (future.length < futureNeeded) {
        future = grow(future, futureLength, futureNeeded*3/2 + 10);
      }
      final int end = pastLength - index;
      for (int i = 0; i < end; ++i) {
        future[futureLength++] = past[--pastLength];
      }
    }
  }

  private char[] grow(char[] charArray, int oldLengthUsed, int neededLength) {
    char[] temp = new char[neededLength];
    System.arraycopy(charArray, 0, temp, 0, oldLengthUsed);
    return temp;
  }

}
