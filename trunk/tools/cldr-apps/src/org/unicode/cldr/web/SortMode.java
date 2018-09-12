//  Copyright 2011-2012 IBM Corporation and Others. All rights reserved.

package org.unicode.cldr.web;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.unicode.cldr.web.DataSection.DataRow;
import org.unicode.cldr.web.Partition.Membership;

/**
 * This class represents a mode of sorting: i.e., by code, etc.
 *
 * @author srl
 *
 */

public abstract class SortMode {

    static String getSortMode(WebContext ctx, String prefix) {
        String sortMode = null;
        sortMode = ctx.pref(SurveyMain.PREF_SORTMODE, SurveyMain.PREF_SORTMODE_DEFAULT);
        return sortMode;
    }

    public static String getSortMode(WebContext ctx, DataSection section) {
        return getSortMode(ctx, section.xpathPrefix);
    }

    public static SortMode getInstance(String mode) {
        if (mode.equals(CodeSortMode.name)) {
            return new CodeSortMode();
        } else if (mode.equals(CalendarSortMode.name)) {
            return new CalendarSortMode();
        } else if (mode.equals(MetazoneSortMode.name)) {
            return new MetazoneSortMode();
        } else if (mode.equals(InterestSort.name)) {
            return new InterestSort();
        } else if (mode.equals(NameSort.name)) {
            return new NameSort();
        } else if (mode.equals(PathHeaderSort.name)) {
            return new PathHeaderSort();
        } else {
            return new CodeSortMode();
        }
    }

    public static List<String> getSortModesFor(String xpath) {
        List<String> list = new LinkedList<String>();
        if (xpath.contains("/calendars")) {
            list.add(CalendarSortMode.name);
        } else if (xpath.contains("zone")) {
            list.add(MetazoneSortMode.name);
        } else {
            list.add(CodeSortMode.name);
        }
        if (false) { // hide others
            list.add(NameSort.name);
            list.add(InterestSort.name);
        }
        return list;
    }

    /**
     * For subclasses
     *
     * @param p
     * @param memberships
     * @return
     */
    protected static final int categorizeDataRow(DataRow p, Partition.Membership[] memberships) {
        int rv = -1;
        for (int i = 0; (rv == -1) && (i < memberships.length); i++) {
            if (memberships[i].isMember(p)) {
                rv = i;
            }
        }
        return rv;
    }

    /**
     * Name of this mode.
     *
     * @return
     */
    abstract String getName();

    abstract String getDisplayName();

    /**
     *
     * @return
     */
    abstract Partition.Membership[] memberships();

    /**
     *
     * @return
     */
    abstract Comparator<DataRow> createComparator();

    public String getDisplayName(DataRow p) {
        if (p == null) {
            return "(null)";
        } else if (p.getDisplayName() != null) {
            return p.getDisplayName();
        } else {
            return p.prettyPath;
        }
    }

    enum SortKeyType {
        SORTKEY_INTEREST, SORTKEY_CALENDAR, SORTKEY_METAZONE
    };

    public static final int[] reserveForSort() {
        int[] x = new int[SortKeyType.values().length];
        for (int i = 0; i < x.length; i++) {
            x[i] = -1;
        }
        return x;
    }

    protected static int compareMembers(DataRow p1, DataRow p2, Membership[] memberships, int ourKey) {
        if (p1.reservedForSort[ourKey] == -1) {
            p1.reservedForSort[ourKey] = categorizeDataRow(p1, memberships);
        }
        if (p2.reservedForSort[ourKey] == -1) {
            p2.reservedForSort[ourKey] = categorizeDataRow(p2, memberships);
        }

        if (p1.reservedForSort[ourKey] < p2.reservedForSort[ourKey]) {
            return -1;
        } else if (p1.reservedForSort[ourKey] > p2.reservedForSort[ourKey]) {
            return 1;
        } else {
            return 0;
        }
    }

    public Partition[] createPartitions(DataRow[] rows) {
        return createPartitions(memberships(), rows);
    }

    /**
     * Create partitions based on the membership in the rows
     *
     * @param memberships
     * @param rows
     * @return
     */
    protected Partition[] createPartitions(Membership[] memberships, DataRow[] rows) {
        Vector<Partition> v = new Vector<Partition>();
        if (memberships != null) { // something with partitions
            Partition testPartitions[] = createPartitions(memberships);

            // find the starts
            int lastGood = 0;
            for (int i = 0; i < rows.length; i++) {
                DataRow p = rows[i];

                for (int j = lastGood; j < testPartitions.length; j++) {
                    if (testPartitions[j].pm.isMember(p)) {
                        if (j > lastGood) {
                            lastGood = j;
                        }
                        if (testPartitions[j].start == -1) {
                            testPartitions[j].start = i;
                        }
                        break; // sit here until we fail membership
                    }

                    if (testPartitions[j].start != -1) {
                        testPartitions[j].limit = i;
                    }
                }
            }
            // catch the last item
            if ((testPartitions[lastGood].start != -1) && (testPartitions[lastGood].limit == -1)) {
                testPartitions[lastGood].limit = rows.length; // limit = off the
                // end.
            }

            for (int j = 0; j < testPartitions.length; j++) {
                if (testPartitions[j].start != -1) {
                    if (testPartitions[j].start != 0 && v.isEmpty()) {
                        // v.add(new
                        // Partition("Other",0,testPartitions[j].start));
                    }
                    v.add(testPartitions[j]);
                }
            }
        } else {
            // default partition - e'erthing.
            v.add(new Partition(null, 0, rows.length));
        }
        return (Partition[]) v.toArray(new Partition[0]); // fold it up
    }

    /**
     * Create empty partitions
     *
     * @param memberships
     * @return
     */
    public static Partition[] createPartitions(Membership[] memberships) {
        if (memberships == null) {
            Partition empty[] = new Partition[1];
            empty[0] = new Partition(null, 0, 0);
            return empty;
        }
        Partition testPartitions[] = new Partition[memberships.length];
        for (int i = 0; i < memberships.length; i++) {
            testPartitions[i] = new Partition(memberships[i]);
        }
        return testPartitions;
    }

    public DataSection.DisplaySet createDisplaySet(XPathMatcher matcher, Collection<DataRow> values) {
        DataRow rows[] = createSortedList(createComparator(), matcher, values);
        return new DataSection.DisplaySet(rows, this, createPartitions(rows));
    }

    protected DataRow[] createSortedList(Comparator<DataRow> comparator, XPathMatcher matcher, Collection<DataRow> rows) {
        // partitions = sortMode.createPartitions(rows);
        // DisplaySet aDisplaySet = new DisplaySet(createSortedList(sortMode,
        // matcher,rowsHash.values()), sortMode);
        Set<DataRow> newSet;

        newSet = new TreeSet<DataRow>(comparator);

        if (matcher == null) {
            newSet.addAll(rows); // sort it
        } else {
            for (Object o : rows) {
                DataRow p = (DataRow) o;

                // /*srl*/ /*if(p.type.indexOf("Australia")!=-1)*/ {
                // System.err.println("xp: "+p.xpathSuffix+":"+p.type+"- match: "+(matcher.matcher(p.type).matches()));
                // }

                if (!matcher.matches(p.getXpath(), p.getXpathId())) {
                    if (false)
                        System.err.println("not match: " + p.xpathId + " / " + p.getXpath());
                    continue;

                } else {
                    newSet.add(p);
                }
            }
        }
        String matchName = "(*)";
        if (matcher != null) {
            matchName = matcher.getName();
        }
        if (SurveyMain.isUnofficial())
            System.err.println("Loaded " + newSet.size() + " from " + matchName + " - base xpath (" + rows.size() + ")  = "
                + getName());
        return newSet.toArray(new DataRow[newSet.size()]);
    }
}
