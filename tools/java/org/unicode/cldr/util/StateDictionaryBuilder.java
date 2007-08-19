package org.unicode.cldr.util;

import org.unicode.cldr.util.IntMap.BasicIntMapFactory;
import org.unicode.cldr.util.StateDictionary.Cell;
import org.unicode.cldr.util.StateDictionary.Row;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class StateDictionaryBuilder<T> {
  
  private static final boolean SHOW_SIZE = true;
  
  // only used while building
  private Row buildingCurrentAddRow;
  private CharSequence buildingLastEntry;
  private int builtTotalBytes;
  private int builtTotalStrings;
  
  // results of build
  private ArrayList<Row> builtRows;
  private Row builtBaseRow;
  private IntMap<T> builtResults;
  private int builtMaxByteLength;
  
  private StringByteConverter byteConverter = new ByteString(true); //new StringUtf8Converter(); // new ByteString(false); // new ByteString(true); //
  
  public StringByteConverter getByteConverter() {
    return byteConverter;
  }
  
  public void setByteConverter(StringByteConverter byteConverter) {
    this.byteConverter = byteConverter;
  }
  
  public StateDictionary make(Map<CharSequence, T> source) {
    // clear out state
    buildingCurrentAddRow = null;
    buildingLastEntry = "";
    builtTotalBytes = builtTotalStrings = builtMaxByteLength = 0;
    builtRows = new ArrayList<Row>();
    builtBaseRow = makeRow();
    builtResults = new BasicIntMapFactory<T>().make(source.values());
    if (SHOW_SIZE) System.out.println("***VALUE STORAGE: " + builtResults.approximateStorage());
    
    Map<T, Integer> valueToInt = builtResults.getValueMap();
    for (CharSequence text : source.keySet()) {
      addMapping(text, valueToInt.get(source.get(text)));
    }
    
    // now compact the rows
    // first find out which rows are equivalent (recursively)
    Map<Row,Row> replacements = new HashMap<Row,Row>();
    {
      Map<Row,Row> equivalents = new TreeMap<Row,Row>(StateDictionary.rowComparator);
      for (Row row : builtRows) {
        Row cardinal = equivalents.get(row);
        if (cardinal == null) {
          equivalents.put(row,row);
        } else {
          replacements.put(row,cardinal);
        }
      }
    }
    if (SHOW_SIZE) System.out.println("***ROWS: " + builtRows.size() + "\t REPLACEMENTS: " + replacements.size());
    
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
    if (SHOW_SIZE) System.out.println("***ROWS: " + builtRows.size());
    return new StateDictionary<T>(builtBaseRow, builtRows, builtResults, builtMaxByteLength, byteConverter);
  }
  
  private Row makeRow() {
    Row row = new Row();
    builtRows.add(row);
    return row;
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
  
  
  private void addMapping(CharSequence text, int result) {
    if (Dictionary.compare(text,buildingLastEntry) <= 0) {
      throw new IllegalArgumentException("Each string must be greater than the previous one.");
    }
    buildingLastEntry = text;
    buildingCurrentAddRow = builtBaseRow;
    int bytesUsed = 0;

    byte[] output = new byte[text.length()*3];
    int lastIndex = byteConverter.toBytes(text,output,0) - 1;
    for (int i = 0; i <= lastIndex; ++i) {
      result = add(output[i], result, i == lastIndex);
      ++bytesUsed;
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
        // we add a continuation, so decrement
        --buildingCurrentAddRow.terminatingReturnCount;
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
  
}