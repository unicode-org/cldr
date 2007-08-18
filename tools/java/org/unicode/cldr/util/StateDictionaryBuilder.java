package org.unicode.cldr.util;

import org.unicode.cldr.util.IntMap.BasicIntMapFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class StateDictionaryBuilder<T> extends Dictionary<T> {
  
  private static final boolean DEBUG_FLATTEN = false;

  private static final boolean DEBUG1 = true;
  private static final boolean DEBUG2 = true;
  private static final boolean DEBUG3 = true;

  // only used while building
  private Row buildingCurrentAddRow;
  private CharSequence buildingLastEntry = "";

  // results of build
  private ArrayList<Row> builtRows = new ArrayList<Row>();
  private Row builtBaseRow = makeRow();
  private IntMap<T> builtResults;
  private int builtMaxByteLength;
  private int builtTotalBytes;
  private int builtTotalStrings;

  // only used in matching
  private Row matchCurrentRow;
  private int matchIntValue = -1;
  private Row matchLastRow;

  public StateDictionaryBuilder(Map<CharSequence, T> source) {
    // if T is not an integer, then get the results, and assign each a number
    //Map<T, Integer> valueToInt = new HashMap<T,Integer>();
    builtResults = new BasicIntMapFactory<T>().make(source.values());
    System.out.println("***VALUE STORAGE: " + builtResults.approximateStorage());
//    new ArrayList<T>();
//    int count = 0;
//    for (T value : source.values()) {
//      Integer oldValue = valueToInt.get(value);
//      if (oldValue == null) {
//        builtResults.add(value);
//        valueToInt.put(value, count++);
//      }
//    }
    Map<T, Integer> valueToInt = builtResults.getValueMap();
    for (CharSequence text : source.keySet()) {
      //addMapping(text, valueToInt.get(source.get(text)));
      addMapping(text, valueToInt.get(source.get(text)));
    }
    
    // now compact the rows
    // first find out which rows are equivalent (recursively)
    Map<Row,Row> replacements = new HashMap<Row,Row>();
    {
      Map<Row,Row> equivalents = new TreeMap<Row,Row>(rowComparator);
      for (Row row : builtRows) {
        Row cardinal = equivalents.get(row);
        if (cardinal == null) {
          equivalents.put(row,row);
        } else {
          replacements.put(row,cardinal);
        }
      }
    }
    System.out.println("***ROWS: " + builtRows.size() + "\t REPLACEMENTS: " + replacements.size());
    
    // now replace all references to rows by their equivalents
    for (Row row : builtRows) {
      for (Byte key : row.byteToCell.keySet()) {
        Cell cell = row.byteToCell.get(key);
        Row newRow = replacements.get(cell.nextRow);
        if (newRow != null) {
          cell.nextRow = newRow;
        }
      }
    }
    // now compact the rows array
    ArrayList<Row> newRows = new ArrayList<Row>();
    for (Row row : builtRows) {
      if (!replacements.containsKey(row)) {
        newRows.add(row);
      }
    }
    builtRows = newRows;
    System.out.println("***ROWS: " + builtRows.size());
  }
  
  public T getMatchValue() {
    try {
      return builtResults.get(matchIntValue);
    } catch (Exception e) {
      return null;
    }
  }
  
  public int getIntMatchValue() {
    return matchIntValue;
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
  public int getRowCount() {
    return builtRows.size();
  }

  private Row makeRow() {
    Row row = new Row();
    builtRows.add(row);
    return row;
  }

  static class Row implements Comparable {
    // maps byte to cells
    private TreeMap<Byte, Cell> byteToCell = new TreeMap<Byte, Cell>();

    // keeps track of the number of cells with returns
    private transient int returnCount;

    private transient int terminatingReturnCount;

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
  
  static final Comparator<Byte> unsignedByteComparator = new Comparator<Byte>() {
    
    public int compare(Byte o1, Byte o2) {
      int b1 = o1&0xFF;
      int b2 = o2&0xFF;
      return b1 < b2 ? -1 : b1 > b2 ? 1 : 0;
    }
    
  };
  
  static final Comparator rowComparator = new Comparator<Row>() {

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

  private byte[] matchByteBuffer = new byte[4];

  private int matchByteStringIndex;

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

  static class Cell {
    public Row nextRow; // next state

    public int deltaResult;

    public boolean returns;

    public int addBytes(byte[] target, int pos, int rowDelta) {
      pos = StateDictionaryBuilder.addBytes(deltaResult, target, pos);
      int rowOffset = nextRow == null ? 0 : rowDelta - nextRow.getAge();
      rowOffset <<= 1; // make room for returns
      if (returns)
        rowOffset |= 1;
      return StateDictionaryBuilder.addBytes(rowOffset, target, pos);
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

  private void addMapping(CharSequence text, int result) {
    if (compare(text,buildingLastEntry) <= 0) {
      throw new IllegalArgumentException("Each string must be greater than the previous one.");
    }
    buildingLastEntry = text;
    buildingCurrentAddRow = builtBaseRow;
    int bytesUsed = 0;
    int lastCharIndex = text.length() - 1;
    if (DEBUG1) {
      byte[] output = new byte[text.length()*3];
      int lastIndex = byteString.toBytes(text,output,0) - 1;
      for (int i = 0; i <= lastIndex; ++i) {
        result = add(output[i], result, i == lastIndex);
        ++bytesUsed;
      }
    } else {
      for (int i = 0; i <= lastCharIndex; ++i) {
        // use UTF-8 significant bits
        char ch = text.charAt(i);
        if (ch < 0x80) {
          result = add((byte) ch, result, i == lastCharIndex);
          ++bytesUsed;
        } else if (ch < 0x800) {
          result = add((byte) (0xC0 | (ch >>> 6)), result, false);
          result = add((byte) (0x80 | (ch & 0x3F)), result, i == lastCharIndex);
          bytesUsed += 2;
        } else {
          result = add((byte) (0xE0 | (ch >>> 12)), result, false);
          result = add((byte) (0x80 | ((ch >>> 6) & 0x3F)), result, false);
          result = add((byte) (0x80 | (ch & 0x3F)), result, i == lastCharIndex);
          bytesUsed += 3;
        }
      }
    }
    builtTotalBytes += bytesUsed;
    builtTotalStrings += 1;
    if (builtMaxByteLength < bytesUsed) {
      builtMaxByteLength = bytesUsed;
    }
  }

  private int add(byte key, int result, boolean last) {
    Cell matchingCell = buildingCurrentAddRow.byteToCell.get(key);
    if (matchingCell != null) {
      if (matchingCell.nextRow == null && !last) {
        matchingCell.nextRow = makeRow();
        --buildingCurrentAddRow.terminatingReturnCount; // we add a continuation, so
                                                // decrement
      }
      buildingCurrentAddRow = matchingCell.nextRow;
      return result - matchingCell.deltaResult;
    }
    Cell cell = new Cell();
    buildingCurrentAddRow.byteToCell.put(key, cell);
    cell.deltaResult = result;
    if (last) {
      cell.returns = true;
      ++buildingCurrentAddRow.returnCount;
      ++buildingCurrentAddRow.terminatingReturnCount;
    } else {
      cell.nextRow = buildingCurrentAddRow = makeRow();
    }
    // when we create a new cell for the first time, we deposit the
    // result, so we can clear it now
    return 0;
  }
  
  @Override
  public Dictionary setText(CharSequence text) {
    super.setText(text);
    if (byteString != null) {
      byteString.clear();
    }
    return this;
  }

  @Override
  public Dictionary setOffset(int offset) {
    matchCurrentRow = builtBaseRow;
    matchIntValue = 0;
    if (byteString != null) {
      byteString.clear();
    }
    matchByteStringIndex = offset;
    return super.setOffset(offset);
  }

  @Override
  public Status next() {
    if (matchCurrentRow == null) {
      matchIntValue = -1;
      return Status.NONE;
    }
    Status result = Status.PARTIAL;
    byte[] output = null;
    int outputLen = 0;
    while (matchEnd < text.length()) {
      if (DEBUG2) {
        // get more bytes IF matchEnd is set right
        if (matchEnd == matchByteStringIndex) {
          char ch = text.charAt(matchByteStringIndex++);
          matchByteBufferLength = byteString.toBytes(ch, matchByteBuffer, 0);
        }
        for (int i = 0; i < matchByteBufferLength; ++i) {
          result = nextByte(matchByteBuffer[i]);
        }
      } else {
        char ch = text.charAt(matchEnd);
        // use UTF-8 significant bits
        if (ch < 0x80) {
          result = nextByte(ch);
        } else if (ch < 0x800) {
          result = nextByte(0xC0 | (ch >>> 6));
          result = nextByte(0x80 | (ch & 0x3F));
        } else {
          result = nextByte(0xE0 | (ch >>> 12));
          result = nextByte(0x80 | ((ch >>> 6) & 0x3F));
          result = nextByte(0x80 | (ch & 0x3F));
        }
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

  private StringByteConverter byteString = new StringUtf8Converter(); // new ByteString(false);

  private int matchByteBufferLength;
  
  private class TextFetcher {

    Map<CharSequence, T> result = new TreeMap<CharSequence, T>();

    byte[] soFar = new byte[builtMaxByteLength];
    BitSet shown = new BitSet();

    StringBuilder buffer = new StringBuilder();
    StringBuilder debugTreeView = new StringBuilder();

    private HashSet<Row> rowsSeen = new HashSet();

    public Map<CharSequence, T> getWords() {
      result.clear();
      getWords(0, 0, builtBaseRow);
      return result;
    }

    public String debugShow() {
      rowsSeen.clear();
      getDebugWords(0,0,builtBaseRow, Integer.MAX_VALUE);
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
      if (DEBUG3) {
        try {
          byteString.fromBytes(soFar, 0, len, buffer);
        } catch (IOException e) { } // will never happen
      } else {
        for (int i = 0; i < len;) {
          char b = (char) (soFar[i++] & 0xFF);
          if (b < 0x80) {
            // fall through
          } else if (b < 0xE0) {
            b &= 0x1F;
            b <<= 6;
            b |= (char) (soFar[i++] & 0x3F);
          } else {
            b &= 0xF;
            b <<= 6;
            b |= (char) (soFar[i++] & 0x3F);
            b <<= 6;
            b |= (char) (soFar[i++] & 0x3F);
            b -= 0;
          }
          buffer.append(b);
        }
      }
      return buffer.toString();
    }
  }

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
    System.out.println("TotalBytes: " + builtTotalBytes);
    System.out.println("TotalBytesCompacted: " + totalBytesCompacted);
    System.out.println("TotalStrings: " + builtTotalStrings);
  }
}