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
        return name;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.unicode.cldr.web.SortMode#memberships()
     */
    @Override
    Membership[] memberships() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.unicode.cldr.web.SortMode#createComparator()
     */
    @Override
    Comparator<DataRow> getComparator() {
        return ComparatorHelper.COMPARATOR;
    }

    public static Comparator<DataRow> internalGetComparator() {
        return ComparatorHelper.COMPARATOR;
    }

    private static final class ComparatorHelper {
        static final Comparator<DataRow> COMPARATOR = new Comparator<DataRow>() {
            final Collator myCollator = CodeSortMode.internalGetCollator();
            // com.ibm.icu.text.Collator myCollator = rbc;
            @Override
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
        return SurveyMain.TRANS_HINT_LANGUAGE_NAME + " Name";
    }

}
