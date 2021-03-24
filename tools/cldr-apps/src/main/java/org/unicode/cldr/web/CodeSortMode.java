/**
 * Copyright (C) 2011-2012 IBM Corporation and Others. All Rights Reserved.
 *
 */
package org.unicode.cldr.web;

import java.util.Comparator;

import org.unicode.cldr.web.DataSection.DataRow;
import org.unicode.cldr.web.Partition.Membership;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;

/**
 * @author srl
 *
 */
public class CodeSortMode extends SortMode {

    public static String name = SurveyMain.PREF_SORTMODE_CODE;

    /*
     * (non-Javadoc)
     *
     * @see org.unicode.cldr.web.SortMode#getName()
     */
    @Override
    String getName() {
        return name;
    }

    @Override
    Membership[] memberships() {
        return null;
    }

    @Override
    Comparator<DataRow> getComparator() {
        return ComparatorHelper.COMPARATOR;
    }

    static final private class CollatorHelper {
        private static Collator createCollator() {
            RuleBasedCollator rbc = ((RuleBasedCollator) Collator.getInstance());
            rbc.setNumericCollation(true);
            return rbc.freeze();
        }
        static final Collator COLLATOR = createCollator();
    }
    static final private class ComparatorHelper {
        static final Comparator<DataRow> COMPARATOR = new Comparator<DataRow>() {
            final Collator myCollator = CollatorHelper.COLLATOR;

            @Override
            public int compare(DataRow p1, DataRow p2) {
                if (p1 == p2) {
                    return 0;
                }
                return myCollator.compare(p1.getPrettyPath(), p2.getPrettyPath());
            }
        };

    }

    public static Collator internalGetCollator() {
        return CollatorHelper.COLLATOR;
    }

    public static Comparator<DataRow> internalGetComparator() {
        return ComparatorHelper.COMPARATOR;
    }

    @Override
    public String getDisplayName(DataRow p) {
        return p.getPrettyPath(); // always code.
    }

    @Override
    String getDisplayName() {
        return "Code";
    }

}
