/*
 **********************************************************************
 * Copyright (c) 2006-2007, Google and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;

import org.unicode.cldr.util.Dictionary.DictionaryBuilder;
import org.unicode.cldr.util.IntMap.BasicIntMapFactory;
import org.unicode.cldr.util.IntMap.IntMapFactory;
import org.unicode.cldr.util.StateDictionary.Cell;
import org.unicode.cldr.util.StateDictionary.Row;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

/**
 * A simple state-table based dictionary builder.
 * @author markdavis
 * @param <T> the return type for the dictionary
 */
public class StateDictionaryBuilder<T> implements DictionaryBuilder<T>{
  
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

  private IntMapFactory<T> intMapFactory = new BasicIntMapFactory<T>();
  
  /**
   * Get/set the IntMapFactory used to store the values for T. The default is BasicIntMapFactory.
   * @return
   */
  public IntMapFactory<T> getIntMapFactory() {
    return intMapFactory;
  }

  public StateDictionaryBuilder setIntMapFactory(IntMapFactory<T> intMapFactory) {
    this.intMapFactory = intMapFactory;
    return this;
  }

  /**
   * Get/Set the StringByteConverter used to convert strings to bytes and back. The default is a compacted form: ByteString(true).
   * @return
   */
  public StringByteConverter getByteConverter() {
    return byteConverter;
  }
  
  public StateDictionaryBuilder setByteConverter(StringByteConverter byteConverter) {
    this.byteConverter = byteConverter;
    return this;
  }
  
  /**
   * Create a new simple StateDictionary. This format is relatively fast to
   * produce, and has a fair amount of compaction. The Map must be sorted
   * according to Dictionary.CHAR_SEQUENCE_COMPARATOR. It must not contain the key "".
   * 
   * @param source
   * @return
   */
  public StateDictionary<T> make(Map<CharSequence, T> source) {
    // clear out state
    buildingCurrentAddRow = null;
    buildingLastEntry = "";
    builtTotalBytes = builtTotalStrings = builtMaxByteLength = 0;
    builtRows = new ArrayList<Row>();
    builtBaseRow = makeRow();
    builtResults = intMapFactory.make(source.values());
    if (SHOW_SIZE) System.out.println("***VALUE STORAGE: " + builtResults.approximateStorage());
    
    Map<T, Integer> valueToInt = builtResults.getValueMap();
    Map<byte[],Integer> sorted = new TreeMap(SHORTER_BYTE_ARRAY_COMPARATOR);
    for (CharSequence text : source.keySet()) {
      sorted.put(byteConverter.toBytes(text), valueToInt.get(source.get(text)));
    }

    for (byte[] key : sorted.keySet()) {
      addMapping(key, sorted.get(key));
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
  
  private void addMapping(byte[] key, int result) {
    buildingCurrentAddRow = builtBaseRow;

    int lastIndex = key.length - 1;
    for (int i = 0; i <=lastIndex; ++i) {
      result = add(key[i], result, i == lastIndex);
    }
    builtTotalBytes += key.length;
    builtTotalStrings += 1;
    if (builtMaxByteLength < key.length) {
      builtMaxByteLength = key.length;
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
  
  static final Comparator<byte[]> SHORTER_BYTE_ARRAY_COMPARATOR = new Comparator<byte[]>() {

    public int compare(byte[] o1, byte[] o2) {
      int minLen = o1.length;
      if (minLen > o2.length) {
        minLen = o2.length;
      }
      for (int i = 0; i < minLen; ++i) {
        if (o1[i] != o2[i]) {
          return o1[i] < o2[i] ? -1 : 1; // return lesser first
        }
      }
      return o1.length < o2.length ? -1 : o1.length > o2.length ? 1 : 0;
    }

  };
}