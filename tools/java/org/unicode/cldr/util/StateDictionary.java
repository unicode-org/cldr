/*
 **********************************************************************
 * Copyright (c) 2006-2007, Google and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class StateDictionary<T> extends Dictionary<T> {
  
  private static final boolean DEBUG_FLATTEN = false;
  
  // results of build
  private final ArrayList<Row> builtRows;
  private final Row builtBaseRow;
  private final IntMap<T> builtResults;
  private final int builtMaxByteLength;
  private final StringByteConverter byteString;
  
  
  /** Only should be called by StateDictionaryBuilder
   * @param builtBaseRow2
   * @param builtRows2
   * @param builtResults2
   * @param builtMaxByteLength TODO
   * @param byteConverter TODO
   */
  StateDictionary(Row builtBaseRow2, ArrayList<Row> builtRows2, IntMap<T> builtResults2, int builtMaxByteLength, StringByteConverter byteConverter) {
    builtBaseRow = builtBaseRow2;
    builtRows = builtRows2;
    builtResults = builtResults2;
    this.builtMaxByteLength = builtMaxByteLength;
    this.byteString = byteConverter;
  }
  
  @Override
  public Matcher<T> getMatcher() {
    return new StateMatcher();
  }
  
  public String toString() {
    StringBuilder result = new StringBuilder();
    //TreeSet<Row> rowSet = new TreeSet<Row>(builtRows);
    for (Row row : builtRows) {
      result.append(row.toString()).append("\r\n");
    }
    return result.toString();
  }
  
  public Map<CharSequence, T> getMapping() {
    return new TextFetcher().getWords();
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
    // maps byte to cells
    TreeMap<Byte, Cell> byteToCell = new TreeMap<Byte, Cell>();
    
    // keeps track of the number of cells with returns
    transient int returnCount;
    
    transient int terminatingReturnCount;
    
    private static int oldest = 0;
    
    private int age = oldest++;
    
    private int newAge = Integer.MAX_VALUE;
    
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
      return age - other.age;
    }
    
    public String toString() {
      StringBuilder buffer = new StringBuilder();
      buffer.append("R" + getAge() + "{");
      boolean first = true;
      Set<Byte> sorted = new TreeSet<Byte>(unsignedByteComparator);
      sorted.addAll(byteToCell.keySet());
      for (Byte key : sorted) {
        if (first) {
          first = false;
        } else {
          buffer.append(' ');
        }
        buffer.append(com.ibm.icu.impl.Utility.hex(key&0xFF,2));
        buffer.append('=');
        buffer.append(byteToCell.get(key));
      }
      buffer.append('}');
      return buffer.toString();
    }
    
    public String toStringCells() {
      StringBuilder buffer = new StringBuilder();
      for (Byte key : byteToCell.keySet()) {
        buffer.append(com.ibm.icu.impl.Utility.hex(key&0xFF,2));
        buffer.append(byteToCell.get(key).toString());
        buffer.append(' ');
      }
      return buffer.toString();
    }
    
    public int getAge() {
      if (newAge != Integer.MAX_VALUE)
        return newAge;
      return age;
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
      int rowOffset = nextRow == null ? 0 : rowDelta - nextRow.getAge();
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
        result += "R" + nextRow.getAge();
      }
      return result;
    }
  }
  
  private class StateMatcher extends Matcher<T> {
    private byte[] matchByteBuffer = new byte[4];
    
    private int matchByteStringIndex;
    private int matchByteBufferLength;
    // only used in matching
    private Row matchCurrentRow;
    private int matchIntValue = -1;
    private Row matchLastRow;
    
    
    
    public T getMatchValue() {
      try {
        return builtResults.get(matchIntValue);
      } catch (Exception e) {
        return null;
      }
    }
      
      
      @Override
      public Matcher<T> setText(CharSequence text) {
        super.setText(text);
        if (byteString != null) {
          byteString.clear();
        }
        return this;
      }
      
      @Override
      public Matcher<T> setOffset(int offset) {
        matchCurrentRow = builtBaseRow;
        matchIntValue = 0;
        if (byteString != null) {
          byteString.clear();
        }
        matchByteStringIndex = offset;
        return super.setOffset(offset);
      }
      
      public Status next() {
        if (matchCurrentRow == null) {
          matchIntValue = -1;
          return Status.NONE;
        }
        Status result = Status.PARTIAL;
        
        while (matchEnd < text.length()) {
          
          // get more bytes IF matchEnd is set right
          if (matchEnd == matchByteStringIndex) {
            char ch = text.charAt(matchByteStringIndex++);
            matchByteBufferLength = byteString.toBytes(ch, matchByteBuffer, 0);
          }
          for (int i = 0; i < matchByteBufferLength; ++i) {
            result = nextByte(matchByteBuffer[i]);
          }
          
          // we will never have a return value except at the end of a character, so we don't need
          // to check after each nextByte
          
          if (result == Status.PARTIAL) {
            ++matchEnd;
            // and continue with the loop
          } else if (result == Status.MATCH) {
            ++matchEnd;
            return result;
          } else {
            // if we didn't get a MATCH, we have NONE. But in reality, there could be a possible match
            // so we check to see whether the current row allows for any continuation.
            if (matchEnd > offset && matchCurrentRow.byteToCell.size() > 0) {
              result = Status.PARTIAL;
            }
            if (result == Status.NONE) {
              matchIntValue = -1;
            }
            break;
          }
        }
        matchLastRow = matchCurrentRow;
        matchCurrentRow = null;
        return result;
      }
      
      public boolean nextUniquePartial() {
        return !doesSplit(matchLastRow);
      }
      
      /**
       * Returns NONE if we cannot go any farther, MATCH if there was a match, and PARTIAL otherwise.
       * If we couldn't go any farther, then the currentRow is left alone.
       * @param chunk
       * @return
       */
      private Status nextByte(int chunk) {
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
    
    /**
     * Determine if there is some path that splits, eg a row has 2 cells.
     * @return
     */
    public boolean doesSplit(Row myRow) {
      boolean result = false;
      int size;
      while (myRow != null) {
        size = myRow.byteToCell.size();
        if (size != 1) {
          result = true;
        }
        Cell firstCell = myRow.byteToCell.get(myRow.byteToCell.firstKey());
        // if we have a returns flag AND a next row, then we are splitting
        // plus, whenever we get a returns flag, we stop adding values
        matchIntValue += firstCell.deltaResult;
        myRow = firstCell.nextRow;
        if (firstCell.returns) {
          if (myRow != null) {
            return true;
          }
          return result;
        }
      }
      return result;
    }
    
    
  }
  
  
  static final Comparator<Byte> unsignedByteComparator = new Comparator<Byte>() {
    
    public int compare(Byte o1, Byte o2) {
      int b1 = o1&0xFF;
      int b2 = o2&0xFF;
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
      getDebugWords(0,0,builtBaseRow, Integer.MAX_VALUE);
      for (Row row : builtRows) {
        debugCounter.add(row.byteToCell.size(), 1);
      }
      for (Integer item : (Collection<Integer>) debugCounter.getKeysetSortedByKey()) {
        debugTreeView.append("cells in row=\t").append(item).append("\trows with count=\t").append(debugCounter.getCount(item)).append("\r\n");
      }
      return debugTreeView.toString();
    }
    
    private void getDebugWords(int byteLength, int resultSoFar, Row row, int suppressAbove) {
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
              debugTreeView.append(com.ibm.icu.impl.Utility.hex(soFar[i]&0xFF,2));
              shown.set(i);
            }
          }
          debugTreeView.append("\t<").append(key2).append(">\t<").append(value2).append(">\r\n");
        }
        if (cell.nextRow != null) {
          getDebugWords(byteLength + 1, currentValue, cell.nextRow, suppressAbove);
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
      
      try {
        byteString.fromBytes(soFar, 0, len, buffer);
      } catch (IOException e) { } // will never happen
      
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
      row.newAge = count++;
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
        if (cell.nextRow != null && cell.nextRow.newAge > row.newAge) {
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