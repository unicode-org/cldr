package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class StateDictionaryBuilder extends Dictionary implements
    Dictionary.Builder {
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
  private static final boolean DEBUG_FLATTEN = false;

  private java.util.ArrayList<Row> rows = new ArrayList<Row>();

  private Row baseRow = makeRow();

  int maxByteLength;

  private int totalBytes;

  public int getRowCount() {
    return rows.size();
  }

  private Row makeRow() {
    Row row = new Row();
    rows.add(row);
    return row;
  }

  static class Row implements Comparable {
    // maps byte to cells
    private TreeMap<Byte, Cell> cells = new TreeMap<Byte, Cell>();

    // keeps track of the number of cells with returns
    private transient int returnCount;

    private transient int terminatingReturnCount;

    private static int oldest = 0;

    private int age = oldest++;

    private int newAge = Integer.MAX_VALUE;

    public int nonTerminating() {
      return cells.size() - terminatingReturnCount;
    }

    public int nonReturn() {
      return cells.size() - returnCount;
    }

    public int depth() {
      int result = 0;
      for (Cell cell : cells.values()) {
        if (cell.nextRow != null) {
          int temp = cell.nextRow.depth() + 1;
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
      if (0 != (result = depth() - other.depth()))
        return result;
      if (0 != (result = cells.size() - other.cells.size()))
        return result;
      // otherwise, try alphabetic among the keys. We are guaranteed that the
      // sizes are the same
      java.util.Iterator<Byte> otherIt = other.cells.keySet().iterator();
      for (byte key : cells.keySet()) {
        int otherKey = otherIt.next();
        if (0 != (result = key - otherKey)) {
          return result;
        }
        // at this point, we are guaranteed that the keys are the same. Compare
        // deltaResults, and row
        Cell cell = cells.get(key);
        Cell otherCell = other.cells.get(key);
        if (0 != (result = cell.deltaResult - otherCell.deltaResult)) {
          return result;
        }
      }
      // if we fail completely, use the age.
      return age - other.age;
    }

    public String toString() {
      StringBuilder buffer = new StringBuilder();
      buffer.append(getAge() + ":");
      for (Byte key : cells.keySet()) {
        buffer.append(key);
        buffer.append(cells.get(key).toString(getAge()));
        buffer.append(' ');
      }
      return buffer.toString();
    }

    public String toStringCells() {
      StringBuilder buffer = new StringBuilder();
      for (Byte key : cells.keySet()) {
        buffer.append(key);
        buffer.append(cells.get(key).toString(getAge()));
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
      for (Byte key : cells.keySet()) {
        target[pos++] = key;
        pos = cells.get(key).addBytes(target, pos, 0);
      }
      target[pos++] = 0;
      return pos;
    }
  }

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

    public String toString() {
      if (nextRow == null) {
        return String.format("[%d%s]", deltaResult,
            (returns ? ", returns" : ""));
      }
      return String.format("[%d%s, Row%d]", deltaResult, (returns ? ", returns"
          : ""), nextRow.getAge());
    }

    public int addBytes(byte[] target, int pos, int rowDelta) {
      pos = StateDictionaryBuilder.addBytes(deltaResult, target, pos);
      int rowOffset = nextRow == null ? 0 : rowDelta - nextRow.getAge();
      rowOffset <<= 1; // make room for returns
      if (returns)
        rowOffset |= 1;
      return StateDictionaryBuilder.addBytes(rowOffset, target, pos);
    }

    public String toString(int deltaRow) {
      if (nextRow == null) {
        return String.format("[%d%s]", deltaResult,
            (returns ? ", returns" : ""));
      }
      return String.format("[%d%s, Row%d]", deltaResult, (returns ? ", returns"
          : ""), nextRow.getAge() - deltaRow);
    }
  }

  private CharSequence lastEntry = "";

  public Builder addMapping(CharSequence text, int result) {
    if (compare(text,lastEntry) <= 0) {
      throw new IllegalArgumentException("Each string must be greater than the previous one.");
    }
    lastEntry = text;
    currentAddRow = baseRow;
    int bytesUsed = 0;
    int lastCharIndex = text.length() - 1;
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
        result = add((byte) (0x80 | ((ch >>> 12) & 0x3F)), result, false);
        result = add((byte) (0x80 | (ch & 0x3F)), result, i == lastCharIndex);
        bytesUsed += 3;
      }
    }
    totalBytes += bytesUsed;
    totalStrings += 1;
    if (maxByteLength < bytesUsed) {
      maxByteLength = bytesUsed;
    }
    return this;
  }

  Row currentAddRow;

  private int add(byte key, int result, boolean last) {
    Cell matchingCell = currentAddRow.cells.get(key);
    if (matchingCell != null) {
      if (matchingCell.nextRow == null && !last) {
        matchingCell.nextRow = makeRow();
        --currentAddRow.terminatingReturnCount; // we add a continuation, so
                                                // decrement
      }
      currentAddRow = matchingCell.nextRow;
      return result - matchingCell.deltaResult;
    }
    Cell cell = new Cell();
    currentAddRow.cells.put(key, cell);
    cell.deltaResult = result;
    if (last) {
      cell.returns = true;
      ++currentAddRow.returnCount;
      ++currentAddRow.terminatingReturnCount;
    } else {
      cell.nextRow = currentAddRow = makeRow();
    }
    // when we create a new cell for the first time, we deposit the
    // result, so we can clear it now
    return 0;
  }

  Row currentRow;

  private int totalStrings;

  @Override
  public Dictionary setOffset(int offset) {
    currentRow = baseRow;
    return super.setOffset(offset);
  }

  @Override
  public Status next() {
    if (currentRow == null) {
      matchValue = Integer.MIN_VALUE;
      return Status.NONE;
    }
    Status result = Status.PARTIAL;
    while (matchEnd < text.length()) {
      char ch = text.charAt(matchEnd);
      // use UTF-8 significant bits
      if (ch < 0x80) {
        result = nextByte(ch);
      } else if (ch < 0x800) {
        result = nextByte(0xC0 | (ch >>> 6));
        result = nextByte(0x80 | (ch & 0x3F));
      } else {
        result = nextByte(0xE0 | (ch >>> 12));
        result = nextByte(0x80 | ((ch >>> 12) & 0x3F));
        result = nextByte(0x80 | (ch & 0x3F));
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
        if (matchEnd > offset && currentRow.cells.size() > 0) {
          result = Status.PARTIAL;
        }
        if (result == Status.NONE) {
          matchValue = Integer.MIN_VALUE;
        }
        break;
      }
    }
    lastRow = currentRow;
    currentRow = null;
    return result;
  }
  
  Row lastRow;

  public boolean nextUniquePartial() {
    return !doesSplit(lastRow);
  }

  /**
   * Returns NONE if we cannot go any farther, MATCH if there was a match, and PARTIAL otherwise.
   * If we couldn't go any farther, then the currentRow is left alone.
   * @param chunk
   * @return
   */
  private Status nextByte(int chunk) {
    Cell cell = currentRow.cells.get((byte) chunk);
    if (cell == null) {
      return Status.NONE;
    }
    matchValue += cell.deltaResult;
    currentRow = cell.nextRow;
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
      size = myRow.cells.size();
      if (size != 1) {
        result = true;
      }
      Cell firstCell = myRow.cells.get(myRow.cells.firstKey());
      // if we have a returns flag AND a next row, then we are splitting
      // plus, whenever we get a returns flag, we stop adding values
      matchValue += firstCell.deltaResult;
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
    TreeSet<Row> rowSet = new TreeSet(rows);
    for (Row row : rowSet) {
      result.append(row.toString()).append("\r\n");
    }
    return result.toString();
  }

  public Map<CharSequence, Integer> getMapping() {
    return new TextFetcher().getWords();
  }

  private class TextFetcher {
    Map<CharSequence, Integer> result = new TreeMap<CharSequence, Integer>();

    byte[] soFar = new byte[maxByteLength];

    StringBuilder buffer = new StringBuilder();

    public Map<CharSequence, Integer> getWords() {
      result.clear();
      getWords(0, 0, baseRow);
      return result;
    }

    // recurse through the strings
    private void getWords(int byteLength, int resultSoFar, Row row) {
      for (Byte key : row.cells.keySet()) {
        Cell cell = row.cells.get(key);
        soFar[byteLength] = key;
        int currentValue = resultSoFar + cell.deltaResult;
        if (cell.returns) {
          result.put(stringFromBytes(soFar, byteLength + 1), currentValue);
        }
        if (cell.nextRow != null) {
          getWords(byteLength + 1, currentValue, cell.nextRow);
        }
      }
    }

    private String stringFromBytes(byte[] soFar, int len) {
      buffer.setLength(0);
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
      return buffer.toString();
    }
  }

  public void flatten() {
    TreeSet<Row> s = new TreeSet<Row>(rows);
    int count = 0;
    int oldDepth = 999;
    String oldCell = "";
    int uniqueCount = 0;
    int cellCount = 0;
    byte[] target = new byte[500];
    int totalBytesCompacted = 0;
    for (Row row : s) {
      row.newAge = count++;
      int depth = row.depth();
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
        cellCount += row.cells.size();
      }
      oldCell = newCell;

      for (Cell cell : row.cells.values()) {
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
    System.out.println("TotalBytes: " + totalBytes);
    System.out.println("TotalBytesCompacted: " + totalBytesCompacted);
    System.out.println("TotalStrings: " + totalStrings);
  }
}