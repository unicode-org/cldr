package org.unicode.cldr.unittest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

/**
 * Build a total order from a partial order.
 * TODO optimize!
 */
public class TotalOrderBuilder<T> {
    private boolean DEBUG = true;
    private LinkedHashSet<List<T>> rows = new LinkedHashSet<>();
    static final List<String> TEST = ImmutableList.of("meter", "kilogram");

    public TotalOrderBuilder<T> add(List<T> items) {
        if (TEST.equals(items)) {
            int debug = 0;
        }
        LinkedHashSet<T> rowCheck = new LinkedHashSet<>(items);
        if (rowCheck.size() != items.size()) {
            throw new IllegalArgumentException("Duplicate items in input");
        }
        if (!items.isEmpty()) {
            rows.add(new ArrayList<>(items));
        }
        return this;
    }

    public TotalOrderBuilder<T>  add(T... items) {
        return add(Arrays.asList(items));
    }

    public List<T> build() {
        List<T> result = new ArrayList<>();
        // whenever a first item in a row is not in any other row (other than first position) put it into the result, and remove
        main:
        while (true) {
            boolean failed = false;
            if (rows.size() == 6) {
                int debug = 0;
            }

            for (List<T> row : rows) {
                if (row.isEmpty()) {
                    continue;
                }
                T first = row.iterator().next();
                if (inNonFirstPosition(first)) {
                    failed = true;
                    continue;
                }
                // got through the gauntlet
                if (DEBUG) {
                    System.out.println("Removing " + first);
                }
                result.add(first);
                removeFromRows(first);
                failed = false;
                continue main;
            }
            if (failed) {
                final String items = toString();
                rows.clear();
                throw new IllegalArgumentException("incompatible orderings, eg:\n" + items );
            }
            rows.clear();
            return result;
        }
    }

    private void removeFromRows(T first) {
        for (List<T> row : rows) {
            if (DEBUG && row.contains("kilogram")) {
                int debug = 0;
            }
            row.remove(first);
        }
        rows = new LinkedHashSet<>(rows); // consolidate
    }

    private boolean inNonFirstPosition(T item) {
        for (List<T> row : rows) {
            if (DEBUG && row.contains("kilogram")) {
                int debug = 0;
            }
            if (!row.isEmpty()
                && row.contains(item)
                && !row.iterator().next().equals(item)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return Joiner.on('\n').join(new LinkedHashSet<>(rows));
    }
}