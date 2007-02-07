package org.unicode.cldr.tool;

import com.ibm.icu.dev.test.util.ArrayComparator;
import com.ibm.icu.text.MessageFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public class TablePrinter<T extends Comparable> {
  public static void main(String[] args) {
    // quick test;
    TablePrinter tablePrinter = new TablePrinter()
      .setTableAttributes("style='border-collapse: collapse' border='1'")
      .setSortPriorities(0, 1)
      .addColumn("Language", null, null, null, true)
      .addColumn("Territory", "bgcolor='green'", null, "align='right'", true);
    String s = tablePrinter.toTable(new Comparable[][] {
        {"German", 3}, 
        {"French", 2}, 
        {"English", 2}
        });
    System.out.println(s);
  }
  private List<Column> columns = new ArrayList();
  private String tableAttributes;
  private transient Column[] columnsFlat;
  
  public String getTableAttributes() {
    return tableAttributes;
  }

  public TablePrinter setTableAttributes(String tableAttributes) {
    this.tableAttributes = tableAttributes;
    return this;
  }

  public TablePrinter setSortPriorities(int... sortPriorities) {
    columnSorter.setSortPriorities(sortPriorities);
    return this;
  }
  
  private static class Column {
    String header;
    String headerAttributes;
    String cellAttributes;

    boolean spanRows;
    MessageFormat cellPattern;
    
    public Column(String header, String headerAttributes, String cellPattern, String cellAttributes, boolean spanRows) {
      this.header = header;
      this.headerAttributes = headerAttributes;
      this.cellAttributes = cellAttributes;
      this.cellPattern = cellPattern == null ? null : new MessageFormat(cellPattern);
      this.spanRows = spanRows;
    }
  }
  
  public TablePrinter addColumn(String header, String headerAttributes, String cellPattern, String cellAttributes, boolean spanRows) {
    columns.add(new Column(header, headerAttributes, cellPattern, cellAttributes, spanRows));
    return this;
  }
  
  public String toTable(T[][] data) {
    return toTable(Arrays.asList(data));
  }
  
  public String toTable(Collection<T[]> data) {
    T[][] sortedFlat = (T[][]) (data.toArray());
    return toTableInternal(sortedFlat);
  }
  
  static class ColumnSorter<T extends Comparable> implements Comparator<T[]> {
    private int[] sortPriorities;
    private boolean[] backward;
    
    public int compare(T[] o1, T[] o2) {
      int result;
      for (int k = 0; k < sortPriorities.length; ++k) {
        if (0 != (result = o1[sortPriorities[k]].compareTo(o2[sortPriorities[k]]))) {
          if (backward[k]) {
            return -result;
          }
          return result;
        }
      }
      return 0;
    }

    public int[] getSortPriorities() {
      return sortPriorities;
    }

    public void setSortPriorities(int[] inSortPriorities) {
      this.sortPriorities = inSortPriorities.clone();
      backward = new boolean[sortPriorities.length];
      for (int i = 0; i < sortPriorities.length; ++i) {
        if (sortPriorities[i] < 0) {
          sortPriorities[i] = ~sortPriorities[i];
          backward[i] = true;
        }
      }
    }
  }
  
  ColumnSorter columnSorter = new ColumnSorter();
  
  public String toTableInternal(T[][] sortedFlat) {
    //TreeSet<String[]> sorted = new TreeSet();
    //sorted.addAll(data);
    Arrays.sort(sortedFlat, columnSorter);
    
    columnsFlat = columns.toArray(new Column[0]);
    
    StringBuilder result = new StringBuilder();
    if (tableAttributes != null) {
    result.append("<table");
    if (tableAttributes != null) {
      result.append(' ').append(tableAttributes);
    }
    result.append(">\r\n");
    }
    result.append("\t<tr>");
    for (int j = 0; j < columnsFlat.length; ++j) {
        result.append("<th");
        if (columnsFlat[j].headerAttributes != null) {
          result.append(' ').append(columnsFlat[j].headerAttributes);
        }
        result.append('>').append(columnsFlat[j].header).append("</th>");

    }
    result.append("</tr>\r\n");
    
    for (int i = 0; i < sortedFlat.length; ++i) {
      result.append("\t<tr>");
      for (int j = 0; j < sortedFlat[i].length; ++j) {
        int identical = findIdentical(sortedFlat, i, j);
        if (identical == 0) continue;
        result.append("<td");
        boolean gotSpace = false;
        if (columnsFlat[j].cellAttributes != null) {
          
          result.append(' ').append(columnsFlat[j].cellAttributes);
          gotSpace = true;
        }
        if (identical != 1) {
          if (!gotSpace) result.append(' ');
          result.append("rowSpan='").append(identical).append('\'');
        }
        result.append('>');
       
        if (columnsFlat[j].cellPattern != null) {
          result.append(columnsFlat[j].cellPattern.format(new Object[]{sortedFlat[i][j]}));
        } else {
          result.append(sortedFlat[i][j]);
        }
        result.append("</td>");
      }
      result.append("</tr>\r\n");
    }
    if (tableAttributes != null) {
      result.append("</table>");
    }
    return result.toString();
  }

  /**
   * Return 0 if the item is the same as in the row above, otherwise the rowSpan (of equal items)
   * @param sortedFlat
   * @param i
   * @param j
   * @return
   */
  private int findIdentical(T[][] sortedFlat, int i, int j) {
    if (!columnsFlat[j].spanRows) return 1;
    T item = sortedFlat[i][j];
    if (i > 0 && item.equals(sortedFlat[i-1][j])) {
      return 0;
    }
    for (int k = i+1; k < sortedFlat.length; ++k) {
      if (!item.equals(sortedFlat[k][j])) {
        return k - i;
      }
    }
    return sortedFlat.length - i;
  }
  // to-do: prevent overlap when it would cause information to be lost.
}