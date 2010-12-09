/*
 **********************************************************************
 * Copyright (c) 2006-2007, Google and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.unicode.cldr.util.StateDictionary.Row.Uniqueness;

public class StateDictionary<T> extends Dictionary<T> {

  private static final boolean DEBUG_FLATTEN = false;

  // results of build
  private final ArrayList<Row> builtRows;

  private final Row builtBaseRow;
  
  private final IntMap<T> builtResults;

  private final int builtMaxByteLength;

  private final StringByteConverter byteString;
  
  // TODO remove before deployment; not thread safe
  private static int debugReferenceCount = 0;
  private int debugReferenceNumber = debugReferenceCount++;

  /** Only should be called by StateDictionaryBuilder
   * @param builtBaseRow2
   * @param builtRows2
   * @param builtResults2
   * @param builtMaxByteLength TODO
   * @param byteConverter TODO
   */
  StateDictionary(Row builtBaseRow2, ArrayList<Row> builtRows2,
      IntMap<T> builtResults2, int builtMaxByteLength,
      StringByteConverter byteConverter) {
    builtBaseRow = builtBaseRow2;
    builtRows = builtRows2;
    builtResults = builtResults2;
    this.builtMaxByteLength = builtMaxByteLength;
    this.byteString = byteConverter;
    //builtBaseValue = builtResults.get(0);
  }

  @Override
  public Matcher<T> getMatcher() {
    return new StateMatcher();
  }

  public String toString() {
    StringBuilder result = new StringBuilder();
    //TreeSet<Row> rowSet = new TreeSet<Row>(builtRows);
    for (Row row : builtRows) {
      result.append(row.toString()).append(CldrUtility.LINE_SEPARATOR);
    }
    Map<T, Integer> map = builtResults.getValueMap();
    Set<Pair<Integer, String>> sorted = new TreeSet<Pair<Integer, String>>();
    for (T item : map.keySet()) {
      sorted.add(new Pair(map.get(item), item.toString()));
    }
    for (Pair<Integer, String> pair : sorted) {
      result.append(pair.getFirst()).append("*=").append(pair.getSecond()).append(CldrUtility.LINE_SEPARATOR);
    }
    return result.toString();
  }

  public Iterator<Entry<CharSequence, T>> getMapping() {
    // TODO Optimize this to only return the items on demand
    return new TextFetcher().getWords().entrySet().iterator();
  }

  @Override
  public String debugShow() {
    return new TextFetcher().debugShow();
  }

  public int getRowCount() {
    return builtRows.size();
  }

  /**
   * Internals. The text is transformed into a byte stream. A state table is
   * used to successively map {state, byte, result} to {newstate, newresult,
   * isReturn}. A state is represented by a Row, which is a mapping from byte to
   * a Cell, where each cell has the {nextRow, delta result, returns flag}.
   * 
   * <pre>
   *  state = next state (row)
   *  result += delta result
   *  if (returns) return the result
   *  &lt;pre&gt;
   *  However, the result and state are preserved for the next call on next().
   * 
   */

  static class Row implements Comparable {
    enum Uniqueness {
      // the unknown value is only used in building
      UNIQUE, AMBIGUOUS, UNKNOWN;

      public String debugName() {
        switch (this) {
          case UNIQUE:
            return ("¹");
          case AMBIGUOUS:
            return "²";
          default:
            return "?";
        }
      }
    }

    Uniqueness hasUniqueValue = Uniqueness.UNKNOWN;
    
    final TreeMap<Byte, Cell> byteToCell = new TreeMap<Byte, Cell>();

    // keeps track of the number of cells with returns
    transient int returnCount;

    transient int terminatingReturnCount;

    private int referenceNumber;
    
    Row(int rowNumber) {
      referenceNumber = rowNumber;
    }

    public int nonTerminating() {
      return byteToCell.size() - terminatingReturnCount;
    }

    public int nonReturn() {
      return byteToCell.size() - returnCount;
    }

    public int maximumDepth() {
      int result = 0;
      for (Cell cell : byteToCell.values()) {
        if (cell.nextRow != null) {
          int temp = cell.nextRow.maximumDepth() + 1;
          if (result < temp) {
            result = temp;
          }
        }
      }
      return result;
    }

    public int compareTo(Object o) {
      Row other = (Row) o;
      int result;
      // we want to sort items first with the fewest number of non-terminating
      // returns
      // cells, then most
      // number of terminating returns, then most number of returns
      if (0 != (result = maximumDepth() - other.maximumDepth()))
        return result;
      if (0 != (result = byteToCell.size() - other.byteToCell.size()))
        return result;
      // otherwise, try alphabetic among the keys. We are guaranteed that the
      // sizes are the same
      java.util.Iterator<Byte> otherIt = other.byteToCell.keySet().iterator();
      for (byte key : byteToCell.keySet()) {
        int otherKey = otherIt.next();
        if (0 != (result = key - otherKey)) {
          return result;
        }
        // at this point, we are guaranteed that the keys are the same. Compare
        // deltaResults, and row
        Cell cell = byteToCell.get(key);
        Cell otherCell = other.byteToCell.get(key);
        if (0 != (result = cell.deltaResult - otherCell.deltaResult)) {
          return result;
        }
      }
      // if we fail completely, use the age.
      return referenceNumber - other.referenceNumber;
    }

    public String toString() {
      StringBuilder buffer = new StringBuilder();
      buffer.append("R" + getReferenceNumber() + hasUniqueValue.debugName() + "{");
      boolean first = true;
      Set<Byte> sorted = new TreeSet<Byte>(unsignedByteComparator);
      sorted.addAll(byteToCell.keySet());
      for (Byte key : sorted) {
        if (first) {
          first = false;
        } else {
          buffer.append(' ');
        }
        buffer.append(com.ibm.icu.impl.Utility.hex(key & 0xFF, 2));
        buffer.append('=');
        buffer.append(byteToCell.get(key));
      }
      buffer.append('}');
      return buffer.toString();
    }

    public String toStringCells() {
      StringBuilder buffer = new StringBuilder();
      for (Byte key : byteToCell.keySet()) {
        buffer.append(com.ibm.icu.impl.Utility.hex(key & 0xFF, 2));
        buffer.append(byteToCell.get(key).toString());
        buffer.append(' ');
      }
      return buffer.toString();
    }

    public int getReferenceNumber() {
      return referenceNumber;
    }

    int compact(byte[] target) {
      int pos = 0;
      for (Byte key : byteToCell.keySet()) {
        target[pos++] = key;
        pos = byteToCell.get(key).addBytes(target, pos, 0);
      }
      target[pos++] = 0;
      return pos;
    }
  }

  static class Cell {
    public Row nextRow; // next state

    public int deltaResult;

    public boolean returns;

    public int addBytes(byte[] target, int pos, int rowDelta) {
      pos = StateDictionary.addBytes(deltaResult, target, pos);
      int rowOffset = nextRow == null ? 0 : rowDelta - nextRow.getReferenceNumber();
      rowOffset <<= 1; // make room for returns
      if (returns)
        rowOffset |= 1;
      return StateDictionary.addBytes(rowOffset, target, pos);
    }

    public String toString() {
      String result = deltaResult == 0 ? "" : String.valueOf(deltaResult);
      if (returns) {
        result += "*";
      }
      if (nextRow != null) {
        if (result.length() != 0) {
          result += "/";
        }
        result += "R" + nextRow.getReferenceNumber();
      }
      return result;
    }
  }

  // should be private, but easier to debug if package private
  class StateMatcher extends Matcher<T> {
    private static final boolean SHOW_DEBUG = false;

    final private byte[] matchByteBuffer = new byte[byteString
        .getMaxBytesPerChar()];

    private int matchByteStringIndex;

    private int matchByteBufferLength;

    // only used in matching
    private Row matchCurrentRow;

    private int matchIntValue = -1;

    private Row matchLastRow;

    @Override
    public Matcher<T> setOffset(int offset) {
      matchCurrentRow = builtBaseRow;
      partialLastRow = null; // can remove this later, only for debugging
      partialMatchValue = 0; // ditto
      matchIntValue = 0;
      myMatchEnd = offset;
      matchValue = null;
      byteString.clear();
      matchByteStringIndex = offset;
      return super.setOffset(offset);
    }

    int myMatchEnd;

    private Row partialLastRow;

    private int partialMatchValue;
    
    public Status next() {
      if (SHOW_DEBUG) {
        System.out.println("NEXT: " + this);
      }
      if (matchCurrentRow == null) {
        matchIntValue = -1;
        matchValue = null;
        return Status.NONE;
      }
      Status result = Status.PARTIAL;

      while (text.hasCharAt(myMatchEnd)) {
        // get more bytes IF matchEnd is set right
        if (myMatchEnd == matchByteStringIndex) {
          if (true) { // matchEnd < text.length()
            char ch = text.charAt(matchByteStringIndex++);
            matchByteBufferLength = byteString.toBytes(ch, matchByteBuffer, 0);
            if (SHOW_DEBUG) {
              System.out.println("\tChar: " + ch + "\t" + com.ibm.icu.impl.Utility.hex(ch) + "\t->\t" + CldrUtility.hex(matchByteBuffer, 0, matchByteBufferLength, " "));
            }
          } else {
            matchByteBufferLength = byteString.toBytes(matchByteBuffer, 0);
          }
        }
        for (int i = 0; i < matchByteBufferLength; ++i) {
          result = nextByte(matchByteBuffer[i]);
          if (result != Status.PARTIAL) {
            break;
          }
        }
        // Normally, we will never have a return value except at the end of a character, so we don't need
        // to check after each nextByte. However, if the byteString converts C to a sequence of bytes that
        // is a prefix of what it converts D into, then we will get a partial match *WITHIN* a character

        if (result == Status.PARTIAL) {
          ++myMatchEnd;
          // and continue with the loop
        } else if (result == Status.MATCH) {
          ++myMatchEnd;
          matchValue = builtResults.get(matchIntValue);
          matchEnd = myMatchEnd;
          if (SHOW_DEBUG) {
            System.out.println("NEXT RESULT: " + result + "\t" + this);
          }
          return result;
        } else {
          // if we didn't get a MATCH, we have NONE. But in reality, there could be a possible match
          // so we check to see whether the current row allows for any continuation.
          if (myMatchEnd > offset && matchCurrentRow.byteToCell.size() > 0) {
            result = Status.PARTIAL;
          }
          if (result == Status.NONE) {
            matchIntValue = -1;
            matchValue = null;
          }
          break;
        }
      }
      matchLastRow = matchCurrentRow;
      matchCurrentRow = null;
      if (result == Status.PARTIAL) {
        matchValue = builtResults.get(matchIntValue);
        matchEnd = myMatchEnd;
        partialLastRow = matchLastRow;
        partialMatchValue = matchIntValue;
        if (SHOW_DEBUG) {
          System.out.println("NEXT RESULT: " + result + "\t" + this);
        }
      }
      return result;
    }

    /**
     * Returns NONE if we cannot go any farther, MATCH if there was a match, and PARTIAL otherwise.
     * If we couldn't go any farther, then the currentRow is left alone.
     * @param chunk
     * @return
     */
    private Status nextByte(int chunk) {
      if (SHOW_DEBUG) {
        System.out.println("\t"  + debugReferenceNumber + "\tRow: " + matchCurrentRow);
      }
      Cell cell = matchCurrentRow.byteToCell.get((byte) chunk);
      if (cell == null) {
        return Status.NONE;
      }
      matchIntValue += cell.deltaResult;
      matchCurrentRow = cell.nextRow;
      if (cell.returns) {
        return Status.MATCH;
      }
      return Status.PARTIAL;
    }

    public int getIntMatchValue() {
      return matchIntValue;
    }

    public boolean nextUniquePartial() {
      if (partialLastRow.hasUniqueValue == Uniqueness.UNIQUE) {
        matchValue = builtResults.get(partialMatchValue);
        matchEnd = myMatchEnd;
        return true;
      }
      return false;
    }

    @Override
    public StateDictionary<T> getDictionary() {
      return StateDictionary.this;
    }
  }

  static final Comparator<Byte> unsignedByteComparator = new Comparator<Byte>() {

    public int compare(Byte o1, Byte o2) {
      int b1 = o1 & 0xFF;
      int b2 = o2 & 0xFF;
      return b1 < b2 ? -1 : b1 > b2 ? 1 : 0;
    }

  };

  static final Comparator<Row> rowComparator = new Comparator<Row>() {

    public int compare(Row row1, Row row2) {
      if (row1 == row2) {
        return 0;
      } else if (row1 == null) {
        return -1;
      } else if (row2 == null) {
        return 1;
      }
      int result;
      if (0 != (result = row1.byteToCell.size() - row2.byteToCell.size())) {
        return result;
      }
      java.util.Iterator<Byte> otherIt = row2.byteToCell.keySet().iterator();
      for (byte key : row1.byteToCell.keySet()) {
        byte otherKey = otherIt.next();
        if (0 != (result = key - otherKey)) {
          return result;
        }
        // at this point, we are guaranteed that the keys are the same. Compare
        // deltaResults, returns, and then recurse on the the row
        Cell cell1 = row1.byteToCell.get(key);
        Cell cell2 = row2.byteToCell.get(key);
        if (0 != (result = cell1.deltaResult - cell2.deltaResult)) {
          return result;
        }
        if (cell1.returns != cell2.returns) {
          return cell1.returns ? 1 : -1;
        }
        if (0 != (result = compare(cell1.nextRow, cell2.nextRow))) {
          return result;
        }
      }
      return 0;

    }

  };

  static int addBytes(int source, byte[] target, int pos) {
    // swap the top bit
    if (source < 0) {
      source = ((-source) << 1) | 1;
    } else {
      source <<= 1;
    }
    // emit the rest as 7 bit quantities with 1 as termination bit
    while (true) {
      byte b = (byte) (source & 0x7F);
      source >>>= 7;
      if (source == 0) {
        b |= 0x80;
        target[pos++] = b;
        return pos;
      }
      target[pos++] = b;
    }
  }

  private class TextFetcher {

    Map<CharSequence, T> result = new TreeMap<CharSequence, T>();

    byte[] soFar = new byte[builtMaxByteLength];

    BitSet shown = new BitSet();

    StringBuilder buffer = new StringBuilder();

    StringBuilder debugTreeView = new StringBuilder();

    private HashSet<Row> rowsSeen = new HashSet<Row>();

    public Map<CharSequence, T> getWords() {
      result.clear();
      getWords(0, 0, builtBaseRow);
      return result;
    }

    public String debugShow() {
      rowsSeen.clear();
      Counter debugCounter = new Counter();
      getDebugWords(0, 0, builtBaseRow, Integer.MAX_VALUE);
      for (Row row : builtRows) {
        debugCounter.add(row.byteToCell.size(), 1);
      }
      for (Integer item : (Collection<Integer>) debugCounter
          .getKeysetSortedByKey()) {
        debugTreeView.append("cells in row=\t").append(item).append(
            "\trows with count=\t").append(debugCounter.getCount(item)).append(
            CldrUtility.LINE_SEPARATOR);
      }
      return debugTreeView.toString();
    }

    private void getDebugWords(int byteLength, int resultSoFar, Row row,
        int suppressAbove) {
      // we do this to show when rows have already been seen, and so the structure has been compacted
      if (rowsSeen.contains(row)) {
        // reset if bigger
        if (suppressAbove > byteLength) {
          suppressAbove = byteLength;
        }
      } else {
        rowsSeen.add(row);
      }
      // walk through the cells, display and recurse
      Set<Byte> sorted = new TreeSet<Byte>(unsignedByteComparator);
      sorted.addAll(row.byteToCell.keySet());
      for (Byte key : sorted) {
        Cell cell = row.byteToCell.get(key);
        soFar[byteLength] = key;
        shown.set(byteLength, false);
        int currentValue = resultSoFar + cell.deltaResult;
        if (cell.returns) {
          CharSequence key2 = stringFromBytes(soFar, byteLength + 1);
          T value2 = builtResults.get(currentValue);
          for (int i = 0; i <= byteLength; ++i) {
            debugTreeView.append(' ');
            if (i >= suppressAbove) {
              debugTreeView.append("++");
            } else if (shown.get(i)) {
              debugTreeView.append("--");
            } else {
              debugTreeView.append(com.ibm.icu.impl.Utility.hex(
                  soFar[i] & 0xFF, 2));
              shown.set(i);
            }
          }
          debugTreeView.append("\t<").append(key2).append(">\t<")
              .append(value2).append(">" + CldrUtility.LINE_SEPARATOR);
        }
        if (cell.nextRow != null) {
          getDebugWords(byteLength + 1, currentValue, cell.nextRow,
              suppressAbove);
        }
      }
    }

    // recurse through the strings
    private void getWords(int byteLength, int resultSoFar, Row row) {
      for (Byte key : row.byteToCell.keySet()) {
        Cell cell = row.byteToCell.get(key);
        soFar[byteLength] = key;
        int currentValue = resultSoFar + cell.deltaResult;
        if (cell.returns) {
          CharSequence key2 = stringFromBytes(soFar, byteLength + 1);
          T value2 = builtResults.get(currentValue);
          result.put(key2, value2);
        }
        if (cell.nextRow != null) {
          getWords(byteLength + 1, currentValue, cell.nextRow);
        }
      }
    }

    private CharSequence stringFromBytes(byte[] soFar, int len) {
      buffer.setLength(0);
      byteString.fromBytes(soFar, 0, len, buffer);
      return buffer.toString();
    }
  }

  /** Just for testing flattening.
   * 
   *
   */
  public void flatten() {
    TreeSet<Row> s = new TreeSet<Row>(builtRows);
    int count = 0;
    int oldDepth = 999;
    String oldCell = "";
    int uniqueCount = 0;
    int cellCount = 0;
    byte[] target = new byte[500];
    int totalBytesCompacted = 0;
    for (Row row : s) {
      row.referenceNumber = count++;
      int depth = row.maximumDepth();
      if (depth != oldDepth) {
        if (DEBUG_FLATTEN) {
          System.out.println("*** " + depth + "***");
        }
        oldDepth = depth;
      }
      int bytesCompacted = row.compact(target);
      if (DEBUG_FLATTEN) {
        System.out.println(bytesCompacted + "\t" + row);
      }
      String newCell = row.toStringCells();
      if (!newCell.equals(oldCell)) {
        uniqueCount++;
        totalBytesCompacted += bytesCompacted;
        cellCount += row.byteToCell.size();
      }
      oldCell = newCell;

      for (Cell cell : row.byteToCell.values()) {
        if (cell.nextRow != null && cell.nextRow.referenceNumber > row.referenceNumber) {
          if (DEBUG_FLATTEN) {
            System.out.println("*** Fail");
          }
          break;
        }
      }
    }
    System.out.println("Count: " + count);
    System.out.println("UniqueCount: " + uniqueCount);
    System.out.println("CellCount: " + cellCount);
    System.out.println("TotalBytesCompacted: " + totalBytesCompacted);
  }

}