package org.unicode.cldr.tool;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.unicode.cldr.util.CollatorHelper;

public class TablePrinter {

    public static final String LS = System.lineSeparator();

    public static void main(String[] args) {
        // quick test;
        TablePrinter tablePrinter =
                new TablePrinter()
                        .setTableAttributes("style='border-collapse: collapse' border='1'")
                        .addColumn("Language")
                        .setSpanRows(true)
                        .setSortPriority(0)
                        .setBreakSpans(true)
                        .addColumn("Junk")
                        .setSpanRows(true)
                        .addColumn("Territory")
                        .setHeaderAttributes("bgcolor='green'")
                        .setCellAttributes("align='right'")
                        .setSpanRows(true)
                        .setSortPriority(1)
                        .setSortAscending(false);
        Comparable<?>[][] data = {
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

    private List<Column> columns = new ArrayList<>();
    private String tableAttributes;
    private transient Column[] columnsFlat;
    private List<Comparable<Object>[]> rows = new ArrayList<>();
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
        columnSorter.setSortPriority(columns.size() - 1, priority);
        sort = true;
        return this;
    }

    public TablePrinter setSortAscending(boolean ascending) {
        columnSorter.setSortAscending(columns.size() - 1, ascending);
        return this;
    }

    public TablePrinter setBreakSpans(boolean breaks) {
        breaksSpans.set(columns.size() - 1, breaks);
        return this;
    }

    private static class Column {
        String header;
        String headerAttributes;
        MessageFormat cellAttributes;

        boolean spanRows;
        MessageFormat cellPattern;
        private boolean hidden = false;
        private boolean isHeader = false;

        public Column(String header) {
            this.header = header;
        }

        public Column setCellAttributes(String cellAttributes) {
            this.cellAttributes =
                    new MessageFormat(
                            MessageFormat.autoQuoteApostrophe(cellAttributes), ULocale.ENGLISH);
            return this;
        }

        public Column setCellPattern(String cellPattern) {
            this.cellPattern =
                    cellPattern == null
                            ? null
                            : new MessageFormat(
                                    MessageFormat.autoQuoteApostrophe(cellPattern),
                                    ULocale.ENGLISH);
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

        public void setHidden(boolean b) {
            hidden = b;
        }

        public void setHeaderCell(boolean b) {
            isHeader = b;
        }
    }

    public TablePrinter addColumn(
            String header,
            String headerAttributes,
            String cellPattern,
            String cellAttributes,
            boolean spanRows) {
        columns.add(
                new Column(header)
                        .setHeaderAttributes(headerAttributes)
                        .setCellPattern(cellPattern)
                        .setCellAttributes(cellAttributes)
                        .setSpanRows(spanRows));
        setSortAscending(true);
        return this;
    }

    public TablePrinter addColumn(String header) {
        columns.add(new Column(header));
        setSortAscending(true);
        return this;
    }

    public TablePrinter addRow(Comparable<Object>[] data) {
        if (data.length != columns.size()) {
            throw new IllegalArgumentException(
                    String.format(
                            "Data size (%d) != column count (%d)", data.length, columns.size()));
        }
        // make sure we can compare; get exception early
        if (rows.size() > 0) {
            Comparable<Object>[] data2 = rows.get(0);
            for (int i = 0; i < data.length; ++i) {
                try {
                    data[i].compareTo(data2[i]);
                } catch (RuntimeException e) {
                    throw new IllegalArgumentException(
                            "Can't compare column " + i + ", " + data[i] + ", " + data2[i]);
                }
            }
        }
        rows.add(data);
        return this;
    }

    Collection<Comparable<Object>> partialRow;

    public TablePrinter addRow() {
        if (partialRow != null) {
            throw new IllegalArgumentException("Cannot add partial row before calling finishRow()");
        }
        partialRow = new ArrayList<>();
        return this;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public TablePrinter addCell(Comparable cell) {
        if (rows.size() > 0) {
            int i = partialRow.size();
            Comparable cell0 = rows.get(0)[i];
            try {
                cell.compareTo(cell0);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException(
                        "Can't compare column " + i + ", " + cell + ", " + cell0);
            }
        }
        partialRow.add(cell);
        return this;
    }

    public TablePrinter finishRow() {
        if (partialRow.size() != columns.size()) {
            throw new IllegalArgumentException(
                    "Items in row ("
                            + partialRow.size()
                            + " not same as number of columns"
                            + columns.size());
        }
        addRow(partialRow);
        partialRow = null;
        return this;
    }

    @SuppressWarnings("unchecked")
    public TablePrinter addRow(Collection<Comparable<Object>> data) {
        addRow(data.toArray(new Comparable[data.size()]));
        return this;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public TablePrinter addRows(Collection data) {
        for (Object row : data) {
            if (row instanceof Collection) {
                addRow((Collection) row);
            } else {
                addRow((Comparable[]) row);
            }
        }
        return this;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public TablePrinter addRows(Comparable[][] data) {
        for (Comparable[] row : data) {
            addRow(row);
        }
        return this;
    }

    @Override
    public String toString() {
        return toTable();
    }

    public void toTsv(PrintWriter tsvFile) {
        Comparable[][] sortedFlat = (rows.toArray(new Comparable[rows.size()][]));
        toTsvInternal(sortedFlat, tsvFile);
    }

    @SuppressWarnings("rawtypes")
    public String toTable() {
        Comparable[][] sortedFlat = (rows.toArray(new Comparable[rows.size()][]));
        return toTableInternal(sortedFlat);
    }

    @SuppressWarnings("rawtypes")
    static class ColumnSorter<T extends Comparable> implements Comparator<T[]> {
        private int[] sortPriorities = new int[0];
        private BitSet ascending = new BitSet();
        Collator englishCollator = CollatorHelper.ROOT_COLLATOR;

        @Override
        @SuppressWarnings("unchecked")
        public int compare(T[] o1, T[] o2) {
            int result = 0;
            for (int curr : sortPriorities) {
                final T c1 = o1[curr];
                final T c2 = o2[curr];
                result =
                        c1 instanceof String
                                ? englishCollator.compare((String) c1, (String) c2)
                                : c1.compareTo(c2);
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
                int[] temp = new int[priority + 1];
                System.arraycopy(sortPriorities, 0, temp, 0, sortPriorities.length);
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

    @SuppressWarnings("rawtypes")
    ColumnSorter<Comparable> columnSorter = new ColumnSorter<>();

    private boolean sort;

    public void toTsvInternal(
            @SuppressWarnings("rawtypes") Comparable[][] sortedFlat, PrintWriter tsvFile) {
        String sep0 = "#";
        for (Column column : columns) {
            if (column.hidden) {
                continue;
            }
            tsvFile.print(sep0);
            tsvFile.print(column.header);
            sep0 = "\t";
        }
        tsvFile.println();

        Object[] patternArgs = new Object[columns.size() + 1];
        if (sort) {
            Arrays.sort(sortedFlat, columnSorter);
        }
        columnsFlat = columns.toArray(new Column[0]);
        for (int i = 0; i < sortedFlat.length; ++i) {
            System.arraycopy(sortedFlat[i], 0, patternArgs, 1, sortedFlat[i].length);

            String sep = "";
            for (int j = 0; j < sortedFlat[i].length; ++j) {
                if (columnsFlat[j].hidden) {
                    continue;
                }
                final Comparable value = sortedFlat[i][j];
                patternArgs[0] = value;

                //                if (false && columnsFlat[j].cellPattern != null) {
                //                    try {
                //                        patternArgs[0] = value;
                //                        System.arraycopy(sortedFlat[i], 0, patternArgs, 1,
                // sortedFlat[i].length);
                //
                // tsvFile.append(sep).append(format(columnsFlat[j].cellPattern.format(patternArgs)).replace("<br>", " "));
                //                    } catch (RuntimeException e) {
                //                        throw (RuntimeException) new
                // IllegalArgumentException("cellPattern<" + i + ", " + j + "> = "
                //                            + value).initCause(e);
                //                    }
                //                } else
                {
                    tsvFile.append(sep).append(tsvFormat(value));
                }
                sep = "\t";
            }
            tsvFile.println();
        }
    }

    private String tsvFormat(Comparable value) {
        if (value == null) {
            return "n/a";
        }
        if (value instanceof Number) {
            int debug = 0;
        }
        String s = value.toString().replace(LS, " • ");
        return BIDI.containsNone(s) ? s : RLE + s + PDF;
    }

    @SuppressWarnings("rawtypes")
    public String toTableInternal(Comparable[][] sortedFlat) {
        // TreeSet<String[]> sorted = new TreeSet();
        // sorted.addAll(data);
        Object[] patternArgs = new Object[columns.size() + 1];

        if (sort) {
            Arrays.sort(sortedFlat, columnSorter);
        }

        columnsFlat = columns.toArray(new Column[0]);

        StringBuilder strBuilder = new StringBuilder();

        strBuilder.append(LS);
        strBuilder.append(
                "<!-- table generated by TablePrinter.java in https://github.com/unicode-org/cldr/ -->"
                        + LS);
        strBuilder.append("<table");
        if (tableAttributes != null) {
            strBuilder.append(' ').append(tableAttributes);
        }
        strBuilder.append(">" + LS);

        if (caption != null) {
            strBuilder.append("\t<caption>").append(caption).append("</caption>");
        }

        // Create primary table header. After a user scrolls the header will stay at the top of the
        // screen
        addHeaderRow(strBuilder);

        // Create table body
        strBuilder.append("\t<tbody>" + LS);
        for (int iRow = 0; iRow < sortedFlat.length; ++iRow) {
            System.arraycopy(sortedFlat[iRow], 0, patternArgs, 1, sortedFlat[iRow].length);
            strBuilder.append("\t\t<tr>" + LS);
            for (int iCol = 0; iCol < sortedFlat[iRow].length; ++iCol) {
                int identical = findIdentical(sortedFlat, iRow, iCol);
                if (identical == 0) continue;
                if (columnsFlat[iCol].hidden) {
                    continue;
                }
                patternArgs[0] = sortedFlat[iRow][iCol];
                strBuilder.append("\t\t\t").append(columnsFlat[iCol].isHeader ? "<th" : "<td");
                if (columnsFlat[iCol].cellAttributes != null) {
                    try {
                        strBuilder
                                .append(' ')
                                .append(columnsFlat[iCol].cellAttributes.format(patternArgs));
                    } catch (RuntimeException e) {
                        throw (RuntimeException)
                                new IllegalArgumentException(
                                                "cellAttributes<"
                                                        + iRow
                                                        + ", "
                                                        + iCol
                                                        + "> = "
                                                        + sortedFlat[iRow][iCol])
                                        .initCause(e);
                    }
                }
                if (identical != 1) {
                    strBuilder.append(" rowSpan='").append(identical).append('\'');
                }
                strBuilder.append('>');

                if (columnsFlat[iCol].cellPattern != null) {
                    try {
                        patternArgs[0] = sortedFlat[iRow][iCol];
                        System.arraycopy(
                                sortedFlat[iRow], 0, patternArgs, 1, sortedFlat[iRow].length);
                        strBuilder.append(
                                format(columnsFlat[iCol].cellPattern.format(patternArgs)));
                    } catch (RuntimeException e) {
                        throw (RuntimeException)
                                new IllegalArgumentException(
                                                "cellPattern<"
                                                        + iRow
                                                        + ", "
                                                        + iCol
                                                        + "> = "
                                                        + sortedFlat[iRow][iCol])
                                        .initCause(e);
                    }
                } else {
                    strBuilder.append(format(sortedFlat[iRow][iCol]));
                }
                strBuilder.append((columnsFlat[iCol].isHeader ? "</th>" : "</td>") + LS);
            }
            strBuilder.append("\t\t</tr>" + LS);
        }
        strBuilder.append("\t</tbody>" + LS);
        strBuilder.append("</table>" + LS);

        return strBuilder.toString();
    }

    static final UnicodeSet BIDI = new UnicodeSet("[[:bc=R:][:bc=AL:]]");
    static final char RLE = '\u202B';
    static final char PDF = '\u202C';

    @SuppressWarnings("rawtypes")
    private String format(Comparable comparable) {
        if (comparable == null) {
            return null;
        }
        String s = comparable.toString().replace(LS, "<br>");
        return BIDI.containsNone(s) ? s : RLE + s + PDF;
    }

    private void addHeaderRow(StringBuilder strBuilder) {
        strBuilder.append("\t<thead style='position: sticky; top: 0;'>" + LS);
        strBuilder.append("\t\t<tr>" + LS);
        for (int j = 0; j < columnsFlat.length; ++j) {
            if (columnsFlat[j].hidden) {
                continue;
            }
            strBuilder.append("\t\t\t<th");
            if (columnsFlat[j].headerAttributes != null) {
                strBuilder.append(' ').append(columnsFlat[j].headerAttributes);
            }
            strBuilder.append('>').append(columnsFlat[j].header).append("</th>" + LS);
        }
        strBuilder.append("\t\t</tr>" + LS);
        strBuilder.append("\t</thead>" + LS);
    }

    /**
     * Return 0 if the item is the same as in the row above, otherwise the rowSpan (of equal items)
     *
     * @param sortedFlat
     * @param rowIndex
     * @param colIndex
     * @return
     */
    @SuppressWarnings("rawtypes")
    private int findIdentical(Comparable[][] sortedFlat, int rowIndex, int colIndex) {
        if (!columnsFlat[colIndex].spanRows) return 1;
        Comparable item = sortedFlat[rowIndex][colIndex];
        if (rowIndex > 0 && item.equals(sortedFlat[rowIndex - 1][colIndex])) {
            if (!breakSpans(sortedFlat, rowIndex, colIndex)) {
                return 0;
            }
        }
        for (int k = rowIndex + 1; k < sortedFlat.length; ++k) {
            if (!item.equals(sortedFlat[k][colIndex]) || breakSpans(sortedFlat, k, colIndex)) {
                return k - rowIndex;
            }
        }
        return sortedFlat.length - rowIndex;
    }

    // to-do: prevent overlap when it would cause information to be lost.
    private BitSet breaksSpans = new BitSet();

    /**
     * Only called with rowIndex > 0
     *
     * @param rowIndex
     * @param colIndex2
     * @return
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean breakSpans(Comparable[][] sortedFlat, int rowIndex, int colIndex2) {
        final int limit = Math.min(breaksSpans.length(), colIndex2);
        for (int colIndex = 0; colIndex < limit; ++colIndex) {
            if (breaksSpans.get(colIndex)
                    && sortedFlat[rowIndex][colIndex].compareTo(sortedFlat[rowIndex - 1][colIndex])
                            != 0) {
                return true;
            }
        }
        return false;
    }

    public TablePrinter setCellAttributes(String cellAttributes) {
        columns.get(columns.size() - 1).setCellAttributes(cellAttributes);
        return this;
    }

    public TablePrinter setCellPattern(String cellPattern) {
        columns.get(columns.size() - 1).setCellPattern(cellPattern);
        return this;
    }

    public TablePrinter setHeaderAttributes(String headerAttributes) {
        columns.get(columns.size() - 1).setHeaderAttributes(headerAttributes);
        return this;
    }

    public TablePrinter setSpanRows(boolean spanRows) {
        columns.get(columns.size() - 1).setSpanRows(spanRows);
        return this;
    }

    /**
     * In the style section, have something like: <style>
     * <!--
     * .redbar { border-style: solid; border-width: 1px; padding: 0; background-color:red; border-collapse: collapse}
     * -->
     * </style>
     *
     * @param color
     * @return
     */
    public static String bar(String htmlClass, double value, double max, boolean log) {
        double width = 100 * (log ? Math.log(value) / Math.log(max) : value / max);
        if (!(width >= 0.5)) return ""; // do the comparison this way to catch NaN
        return "<table class='"
                + htmlClass
                + "' width='"
                + width
                + "%'><tr><td>\u200B</td></tr></table>";
    }

    public TablePrinter setHidden(boolean b) {
        columns.get(columns.size() - 1).setHidden(b);
        return this;
    }

    public TablePrinter setHeaderCell(boolean b) {
        columns.get(columns.size() - 1).setHeaderCell(b);
        return this;
    }

    public void clearRows() {
        rows.clear();
    }
}
