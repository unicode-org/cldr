package org.unicode.cldr.tool;

import com.ibm.icu.dev.test.util.ArrayComparator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.ULocale;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public class TablePrinter {
  public static void main(String[] args) {
    // quick test;
    TablePrinter tablePrinter = new TablePrinter()
      .setTableAttributes("style='border-collapse: collapse' border='1'")
      .addColumn("Language").setSpanRows(true).setSortPriority(0).setBreakSpans(true)
      .addColumn("Junk").setSpanRows(true)
      .addColumn("Territory").setHeaderAttributes("bgcolor='green'").setCellAttributes("align='right'").setSpanRows(true)
          .setSortPriority(1).setSortAscending(false);
    Comparable[][] data = {
        {"German", 1.3d, 3}, 
        {"French", 1.3d, 2}, 
        {"English", 1.3d, 2},
        {"English", 1.3d, 4},
        {"English", 1.3d, 6},
        {"English", 1.3d, 8},
        {"Arabic", 1.3d, 5},
        {"Zebra", 1.3d, 10}
        };
    tablePrinter.addRows(data);

    String s = tablePrinter.toTable();
    System.out.println(s);
  }
  
  private List<Column> columns = new ArrayList();
  private String tableAttributes;
  private transient Column[] columnsFlat;
  private BitSet blockingRows = new BitSet();
  private List<Comparable[]> rows = new ArrayList();
  
  public String getTableAttributes() {
    return tableAttributes;
  }

  public TablePrinter setTableAttributes(String tableAttributes) {
    this.tableAttributes = tableAttributes;
    return this;
  }

  public TablePrinter setSortPriority(int priority) {
    columnSorter.setSortPriority(columns.size()-1, priority);
    return this;
  }

  public TablePrinter setSortAscending(boolean ascending) {
    columnSorter.setSortAscending(columns.size()-1, ascending);
    return this;
  }
  
  public TablePrinter setBreakSpans(boolean breaks) {
    breaksSpans.set(columns.size()-1, breaks);
    return this;
  }
  
  private static class Column {
    String header;
    String headerAttributes;
    String cellAttributes;

    boolean spanRows;
    MessageFormat cellPattern;
    
    public Column(String header) {
      this.header = header;
    }

    public Column setCellAttributes(String cellAttributes) {
      this.cellAttributes = cellAttributes;
      return this;
   }

    public Column setCellPattern(String cellPattern) {
      this.cellPattern = cellPattern == null ? null : new MessageFormat(cellPattern);
      return this;
    }

    public Column setHeader(String header) {
      this.header = header;
      return this;
    }

    public Column setHeaderAttributes(String headerAttributes) {
      this.headerAttributes = headerAttributes;
      return this;
    }

    public Column setSpanRows(boolean spanRows) {
      this.spanRows = spanRows;
      return this;
    }
  }

  public TablePrinter addColumn(String header, String headerAttributes, String cellPattern, String cellAttributes, boolean spanRows) {
    columns.add(new Column(header).setHeaderAttributes(headerAttributes).setCellPattern(cellPattern).setCellAttributes(cellAttributes).setSpanRows(spanRows));
    setSortAscending(true);
    return this;
  }
  
  public TablePrinter addColumn(String header) {
    columns.add(new Column(header));
    setSortAscending(true);
    return this;
  }
  
  public TablePrinter addRow(Comparable[] data) {
    if (data.length != columns.size()) {
      throw new IllegalArgumentException(String.format("Data size (%d) != column count (%d)", data.length, columns.size()));
    }
    // make sure we can compare; get exception early
    if (rows.size() > 0) {
      Comparable[] data2 = rows.get(0);
      for (int i = 0; i < data.length; ++i) {
        try {
          data[i].compareTo(data2[i]);
        } catch (RuntimeException e) {
          throw new IllegalArgumentException("Can't compare column " + i + ", " + data[i] + ", " + data2[i]);
        }
      }
    }
    rows.add(data);
    return this;
  }

  public TablePrinter addRow(Collection<Comparable> data) {
    addRow(data.toArray(new Comparable[data.size()]));
    return this;
  }
  
  public TablePrinter addRows(Collection data) {
    for (Object row : data) {
      if (row instanceof Collection) {
        addRow((Collection)row);
      } else {
        addRow((Comparable[])row);
      }
    }
    return this;
  }
  
  public TablePrinter addRows(Comparable[][] data) {
    for (Comparable[] row : data) {
      addRow(row);
    }
    return this;
  }

  public String toTable() {
    Comparable[][] sortedFlat = (Comparable[][]) (rows.toArray(new Comparable[rows.size()][]));
    return toTableInternal(sortedFlat);
  }
  
  static class ColumnSorter<T extends Comparable> implements Comparator<T[]> {
    private int[] sortPriorities = new int[0];
    private BitSet ascending = new BitSet();
    Collator englishCollator = Collator.getInstance(ULocale.ENGLISH);
    
    public int compare(T[] o1, T[] o2) {
      int result;
      for (int curr : sortPriorities) {
        result = o1[curr] instanceof String ? 
            englishCollator.compare(o1[curr],o2[curr])
            : o1[curr].compareTo(o2[curr]);
        if (0 != result) {
          if (ascending.get(curr)) {
            return result;
          }
          return -result;
        }
      }
      return 0;
    }

    public void setSortPriority(int column, int priority) {
      if (sortPriorities.length <= priority) {
        int[] temp = new int[priority+1];
        System.arraycopy(sortPriorities,0,temp,0,sortPriorities.length);
        sortPriorities = temp;
      }
      sortPriorities[priority] = column;
    }

    public int[] getSortPriorities() {
      return sortPriorities;
    }

    public boolean getSortAscending(int bitIndex) {
      return ascending.get(bitIndex);
    }

    public void setSortAscending(int bitIndex, boolean value) {
      ascending.set(bitIndex, value);
    }
  }
  
  ColumnSorter<Comparable> columnSorter = new ColumnSorter<Comparable>();
  
  public String toTableInternal(Comparable[][] sortedFlat) {
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
   * @param rowIndex
   * @param colIndex
   * @return
   */
  private int findIdentical(Comparable[][] sortedFlat, int rowIndex, int colIndex) {
    if (!columnsFlat[colIndex].spanRows) return 1;
    Comparable item = sortedFlat[rowIndex][colIndex];
    if (rowIndex > 0 && item.equals(sortedFlat[rowIndex-1][colIndex])) {
      if (!breakSpans(sortedFlat, rowIndex)) {
        return 0;
      }
    }
    for (int k = rowIndex+1; k < sortedFlat.length; ++k) {
      if (!item.equals(sortedFlat[k][colIndex]) || breakSpans(sortedFlat, k)) {
        return k - rowIndex;
      }
    }
    return sortedFlat.length - rowIndex;
  }
  // to-do: prevent overlap when it would cause information to be lost.
  private BitSet breaksSpans = new BitSet();

  /**
   * Only called with rowIndex > 0
   * @param rowIndex
   * @return
   */
  private boolean breakSpans(Comparable[][] sortedFlat, int rowIndex) {
    for (int colIndex = 0; colIndex < breaksSpans.length(); ++colIndex) {
      if (!breaksSpans.get(colIndex)) return false;
      if (sortedFlat[rowIndex][colIndex].compareTo(sortedFlat[rowIndex-1][colIndex]) != 0) {
        return true;
      }
    }
    return false;
  }

  public TablePrinter setCellAttributes(String cellAttributes) {
    columns.get(columns.size()-1).setCellAttributes(cellAttributes);
    return this;
  }

  public TablePrinter setCellPattern(String cellPattern) {
    columns.get(columns.size()-1).setCellPattern(cellPattern);
    return this;
  }

  public TablePrinter setHeaderAttributes(String headerAttributes) {
    columns.get(columns.size()-1).setHeaderAttributes(headerAttributes);
    return this;
  }

  public TablePrinter setSpanRows(boolean spanRows) {
    columns.get(columns.size()-1).setSpanRows(spanRows);
    return this;
  }
}