/**
 * Copyright (C) 2011-2012 IBM Corporation and Others. All Rights Reserved.
 *
 */
package org.unicode.cldr.web;

import java.util.Comparator;

import org.unicode.cldr.web.DataSection.DataRow;
import org.unicode.cldr.web.Partition.Membership;

import com.ibm.icu.text.Collator;

/**
 * @author srl
 *
 */
public class NameSort extends SortMode {

    public static String name = SurveyMain.PREF_SORTMODE_NAME;

    /*
     * (non-Javadoc)
     *
     * @see org.unicode.cldr.web.SortMode#getName()
     */
    @Override
    String getName() {
        // TODO Auto-generated method stub
        return name;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.unicode.cldr.web.SortMode#memberships()
     */
    @Override
    Membership[] memberships() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.unicode.cldr.web.SortMode#createComparator()
     */
    @Override
    Comparator<DataRow> createComparator() {
        // TODO Auto-generated method stub
        return comparator();
    }

    public static Comparator<DataRow> comparator() {
        final Collator myCollator = CodeSortMode.createCollator();
        return new Comparator<DataRow>() {
            // com.ibm.icu.text.Collator myCollator = rbc;
            public int compare(DataRow p1, DataRow p2) {
                if (p1 == p2) {
                    return 0;
                }
                String p1d = p1.getDisplayName();
                if (p1.getDisplayName() == null) {
                    p1d = p1.prettyPath;
                    // throw new InternalError("item p1 w/ null display: " +
                    // p1.type);
                }
                String p2d = p2.getDisplayName();
                if (p2.getDisplayName() == null) {
                    p2d = p2.prettyPath;
                    // throw new InternalError("item p2 w/ null display: " +
                    // p2.type);
                }
                int rv = myCollator.compare(p1d, p2d);
                if (rv == 0) {
                    p1d = p1.prettyPath;
                    p2d = p2.prettyPath;
                    if (p1d == null) {
                        p1d = "(null)";
                    }
                    if (p2d == null) {
                        p2d = "(null)";
                    }
                    rv = myCollator.compare(p1d, p2d);
                }
                return rv;
            }
        };
    }

    @Override
    String getDisplayName() {
        return SurveyMain.BASELINE_LANGUAGE_NAME + " Name";
    }

}
