//  Copyright 2011-2012 IBM Corporation and Others. All rights reserved.

package org.unicode.cldr.web;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Logger;
import org.unicode.cldr.web.DataPage.DataRow;

/**
 * This class represents a mode of sorting: i.e., by code, etc.
 *
 * @author srl
 */
public abstract class SortMode {
    final Logger logger = SurveyLog.forClass(SortMode.class);

    /** Name of this mode. */
    abstract String getName();

    abstract String getDisplayName();

    /**
     * @return the comparator to use
     */
    abstract Comparator<DataRow> getComparator();

    public String getDisplayName(DataRow p) {
        if (p == null) {
            return "(null)";
        } else if (p.getDisplayName() != null) {
            return p.getDisplayName();
        } else {
            return p.prettyPath;
        }
    }

    public Partition[] createPartitions(DataRow[] rows) {
        Vector<Partition> v = new Vector<>();
        v.add(new Partition(null, 0, rows.length));
        return v.toArray(new Partition[0]); // fold it up
    }

    public DataPage.DisplaySet createDisplaySet(XPathMatcher matcher, Collection<DataRow> values) {
        DataRow[] rows = createSortedList(getComparator(), matcher, values);
        return new DataPage.DisplaySet(rows, this, createPartitions(rows));
    }

    protected DataRow[] createSortedList(
            Comparator<DataRow> comparator, XPathMatcher matcher, Collection<DataRow> rows) {
        Set<DataRow> newSet = new TreeSet<>(comparator);

        if (matcher == null) {
            newSet.addAll(rows); // sort it
        } else {
            for (DataRow row : rows) {
                int xpathId = row.getXpathId();
                if (!matcher.matches(row.getXpath(), xpathId)) {
                    logger.finest("not match: " + xpathId + " / " + row.getXpath());
                } else {
                    newSet.add(row);
                }
            }
        }
        String matchName = "(*)";
        if (matcher != null) {
            matchName = matcher.getName();
        }
        logger.fine(
                "Loaded "
                        + newSet.size()
                        + " from "
                        + matchName
                        + " - base xpath ("
                        + rows.size()
                        + ")  = "
                        + getName());
        return newSet.toArray(new DataRow[0]);
    }
}
