package org.unicode.cldr.tool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.unicode.cldr.util.CldrUtility;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.ULocale;

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
    tablePrinter.addRow().addCell("Foo").addCell(1.5d).addCell(99).finishRow();
    
    String s = tablePrinter.toTable();
    System.out.println(s);
  }
  
  private List<Column> columns = new ArrayList();
  private String tableAttributes;
  private transient Column[] columnsFlat;
  private BitSet blockingRows = new BitSet();
  private List<Comparable[]> rows = new ArrayList();
  private String caption;
  
  public String getTableAttributes() {
    return tableAttributes;
  }
  
  public TablePrinter setTableAttributes(String tableAttributes) {
    this.tableAttributes = tableAttributes;
    return this;
  }
  
  public TablePrinter setCaption(String caption) {
    this.caption = caption;
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
    MessageFormat cellAttributes;
    
    boolean spanRows;
    MessageFormat cellPattern;
    private boolean repeatHeader = false;
    private boolean hidden = false;
    private boolean isHeader = false;
    private boolean divider = false;
    
    public Column(String header) {
      this.header = header;
    }
    
    public Column setCellAttributes(String cellAttributes) {
      this.cellAttributes = new MessageFormat(MessageFormat.autoQuoteApostrophe(cellAttributes));
      return this;
    }
    
    public Column setCellPattern(String cellPattern) {
      this.cellPattern = cellPattern == null ? null : new MessageFormat(MessageFormat.autoQuoteApostrophe(cellPattern));
      return this;
    }
    
    public Column setCellPattern(MessageFormat cellPattern) {
      this.cellPattern = cellPattern;
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

    public void setRepeatHeader(boolean b) {
      repeatHeader = b;
    }

    public void setHidden(boolean b) {
      hidden = b;
    }

    public void setHeaderCell(boolean b) {
      isHeader = b;
    }

    public void setDivider(boolean b) {
      divider = b;
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
  
  Collection<Comparable> partialRow;
  
  public TablePrinter addRow() {
    if (partialRow != null) {
      throw new IllegalArgumentException("Cannot add partial row before calling finishRow()");
    }
    partialRow = new ArrayList();
    return this;
  }
  
  public TablePrinter addCell(Comparable cell) {
    if (rows.size() > 0) {
      int i = partialRow.size();
      Comparable cell0 = rows.get(0)[i];
      try {
        cell.compareTo(cell0);
      } catch (RuntimeException e) {
        throw new IllegalArgumentException("Can't compare column " + i + ", " + cell + ", " + cell0);
      }
      
    }
    partialRow.add(cell);
    return this;
  }
  
  public TablePrinter finishRow() {
    if (partialRow.size() != columns.size()) {
      throw new IllegalArgumentException("Items in row (" + partialRow.size() 
              + " not same as number of columns" + columns.size());
    }
    addRow(partialRow);
    partialRow = null;
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
  
  public String toString() {
    return toTable();
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
        result = o1[curr] instanceof String ? englishCollator.compare((String)o1[curr],(String)o2[curr])
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
    Object[] patternArgs = new Object[columns.size() + 1];

    Arrays.sort(sortedFlat, columnSorter);
    
    columnsFlat = columns.toArray(new Column[0]);
    
    StringBuilder result = new StringBuilder();

    result.append("<table");
    if (tableAttributes != null) {
      result.append(' ').append(tableAttributes);
    }
    result.append(">" + CldrUtility.LINE_SEPARATOR);
    
    if (caption != null) {
      result.append("<caption>").append(caption).append("</caption>");
    }

    showHeader(result);
    int visibleWidth = 0;
    for (int j = 0; j < columns.size(); ++j) {
      if (!columnsFlat[j].hidden) {
        ++visibleWidth;
      }
    }
    
    for (int i = 0; i < sortedFlat.length; ++i) {
      System.arraycopy(sortedFlat[i], 0, patternArgs, 1, sortedFlat[i].length);
      // check to see if we repeat the header
      if (i != 0) {
        boolean divider = false;
        for (int j = 0; j < sortedFlat[i].length; ++j) {
          final Column column = columns.get(j);
          if (column.repeatHeader && !sortedFlat[i-1][j].equals(sortedFlat[i][j])) {
              showHeader(result);
              break;
          } else if (column.divider && !sortedFlat[i-1][j].equals(sortedFlat[i][j])) {
            divider = true;
          }
        }
        if (divider) {
          result.append("\t<tr><td class='divider' colspan='" + visibleWidth + "'></td></tr>");
        }
      }
      result.append("\t<tr>");
      for (int j = 0; j < sortedFlat[i].length; ++j) {
        int identical = findIdentical(sortedFlat, i, j);
        if (identical == 0) continue;
        if (columnsFlat[j].hidden) {
          continue;
        }
        patternArgs[0] = sortedFlat[i][j];
        result.append(columnsFlat[j].isHeader ? "<th" : "<td");
        if (columnsFlat[j].cellAttributes != null) {
          try {
            result.append(' ').append(columnsFlat[j].cellAttributes.format(patternArgs));
          } catch (RuntimeException e) {
            throw (RuntimeException) new IllegalArgumentException("cellAttributes<" + i + ", " + j + "> = " + sortedFlat[i][j]).initCause(e);
          }
        }
        if (identical != 1) {
          result.append(" rowSpan='").append(identical).append('\'');
        }
        result.append('>');
        
        if (columnsFlat[j].cellPattern != null) {
          try {
            patternArgs[0] = sortedFlat[i][j];
            System.arraycopy(sortedFlat[i], 0, patternArgs, 1, sortedFlat[i].length);
            result.append(columnsFlat[j].cellPattern.format(patternArgs));
          } catch (RuntimeException e) {
            throw (RuntimeException) new IllegalArgumentException("cellPattern<" + i + ", " + j + "> = " + sortedFlat[i][j]).initCause(e);
          }
        } else {
          result.append(sortedFlat[i][j]);
        }
        result.append(columnsFlat[j].isHeader ? "</th>" : "</td>");
      }
      result.append("</tr>" + CldrUtility.LINE_SEPARATOR);
    }
    result.append("</table>");
    return result.toString();
  }
  
  private void showHeader(StringBuilder result) {
    result.append("\t<tr>");
    for (int j = 0; j < columnsFlat.length; ++j) {
      if (columnsFlat[j].hidden) {
        continue;
      }
      result.append("<th");
      if (columnsFlat[j].headerAttributes != null) {
        result.append(' ').append(columnsFlat[j].headerAttributes);
      }
      result.append('>').append(columnsFlat[j].header).append("</th>");
      
    }
    result.append("</tr>" + CldrUtility.LINE_SEPARATOR);
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
  
  public TablePrinter setRepeatHeader(boolean b) {
    columns.get(columns.size()-1).setRepeatHeader(b);
    if (b) {
      breaksSpans.set(columns.size()-1, true);
    }
    return this;
  }

  /**
   * In the style section, have something like:
   * <style>
   * <!--
   * .redbar       { border-style: solid; border-width: 1px; padding: 0; background-color:red; border-collapse: collapse}
   * -->
   * </style> 
   * @param color
   * @return
   */
  public static String bar(String htmlClass, double value, double max, boolean log) {
    double width = 100*(log ? Math.log(value)/Math.log(max) : value/max);
    if (!(width>=0.5)) return ""; // do the comparison this way to catch NaN
    return "<table class='" + htmlClass + "' width='" + width + "%'><tr><td>\u200B</td></tr></table>";
  }

  public TablePrinter setHidden(boolean b) {
    columns.get(columns.size()-1).setHidden(b);
    return this;
  }

  public TablePrinter setHeaderCell(boolean b) {
    columns.get(columns.size()-1).setHeaderCell(b);
    return this;
  }

  public TablePrinter setRepeatDivider(boolean b) {
    columns.get(columns.size()-1).setDivider(b);
    return this;
  }
}