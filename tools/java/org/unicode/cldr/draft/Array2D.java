package org.unicode.cldr.draft;

import java.lang.reflect.Array;
import java.util.ArrayList;

class Array2D<T> {
    private ArrayList<ArrayList<T>> data = new ArrayList<ArrayList<T>>();
    private int columnSize;

    void set(int row, int column, T value) {
        ensureSize(data, row + 1);
        ArrayList<T> rowList = data.get(row);
        if (rowList == null) {
            data.set(row, rowList = new ArrayList<T>());
        }
        ensureSize(rowList, column + 1);
        if (columnSize < rowList.size()) {
            columnSize = rowList.size();
        }
        rowList.set(column, value);
    }

    T get(int row, int column) {
        if (row >= data.size()) {
            return null;
        }
        ArrayList<T> rowList = data.get(row);
        return rowList == null || column >= rowList.size() ? null : rowList.get(column);
    }

    @SuppressWarnings("unchecked")
    T[][] to(Class<?> cls) {
        T[][] result = (T[][]) Array.newInstance(cls, rows(), columns());
        for (int row = 0; row < data.size(); ++row) {
            result[row] = (T[]) Array.newInstance(cls, columns());
            for (int column = 0; column < columnSize; ++column) {
                result[row][column] = get(row, column);
            }
        }
        return result;
    }

    int rows() {
        return data.size();
    }

    int columns() {
        return columnSize;
    }

    public static void ensureSize(ArrayList<?> list, int size) {
        // Prevent excessive copying while we're adding
        list.ensureCapacity(size);
        while (list.size() < size) {
            list.add(null);
        }
    }
}