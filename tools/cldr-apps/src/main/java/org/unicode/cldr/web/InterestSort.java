/**
 * Copyright (C) 2011-2012 IBM Corporation and Others. All Rights Reserved.
 *
 */
package org.unicode.cldr.web;

import java.util.Comparator;

import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.VoteResolver.Status;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.web.DataSection.DataRow;
import org.unicode.cldr.web.Partition.Membership;

import com.ibm.icu.text.Collator;

/**
 * @author srl
 *
 */
public class InterestSort extends SortMode {

    public static String name = SurveyMain.PREF_SORTMODE_WARNING;

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
        return memberships;
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

    private static final class ComparatorHelper {
        static final Comparator<DataRow> COMPARATOR = new Comparator<DataRow>() {
            final int ourKey = SortMode.SortKeyType.SORTKEY_INTEREST.ordinal();

            final Comparator<DataRow> nameComparator = NameSort.internalGetComparator();
            final Collator collator = CodeSortMode.internalGetCollator();

            @Override
            public int compare(DataRow p1, DataRow p2) {
                if (p1 == p2) {
                    return 0;
                }

                int rv = 0; // neg: a < b. pos: a> b

                rv = compareMembers(p1, p2, memberships, ourKey);
                if (rv != 0) {
                    return rv;
                }

                final boolean p1IsName = p1.isName();
                final boolean p2IsName = p2.isName();
                if (p1IsName != p2IsName) { // do this for transitivity, so that
                    // names sort first if there are
                    // mixtures
                    return p1IsName ? -1 : 1;
                } else if (p1IsName) {
                    return nameComparator.compare(p1, p2);
                }
                // Sort by xpath if all else fails.
                return collator.compare(p1.getXpath(), p2.getXpath());
            }
        };
    }

    private static Partition.Membership memberships[] = { new Partition.Membership("Errors") {
        @Override
        public boolean isMember(DataRow p) {
            return (p.hasErrors);
        }
    },
        // new Partition.Membership("Disputed") {
        // public boolean isMember(DataRow p) {
        // return ((p.allVoteType & Vetting.RES_DISPUTED)>0) ; // not sure
        // why "allVoteType" is needed
        // }
        // },
        new Partition.Membership("Warnings") {
            @Override
            public boolean isMember(DataRow p) {
                return (p.hasWarnings);
            }
        },
        // Later, we might want more groups.
        // INDETERMINATE (-1),
        // APPROVED (0),
        // CONTRIBUTED (1),
        // PROVISIONAL (2),
        // UNCONFIRMED (3);
        new Partition.Membership("Not (minimally) Approved") {
            @Override
            public boolean isMember(DataRow p) {
                return p.winningXpathId != -1 && p.confirmStatus != Status.approved && p.confirmStatus != Status.contributed;
            }
        }, new Partition.Membership("Approved") {
            @Override
            public boolean isMember(DataRow p) {
                return p.winningXpathId != -1; // will be APPROVED
            }
        }, new Partition.Membership("Missing") {
            @Override
            public boolean isMember(DataRow p) {
                // extrapaths and some special paths may not have an inherited item
                return p.getInheritedLocale() != null &&
                    (CLDRLocale.ROOT == p.getInheritedLocale() ||
                    XMLSource.CODE_FALLBACK_ID.equals(p.getInheritedLocale().getBaseName()));
            }
        }, new Partition.Membership("Inherited") {
            @Override
            public boolean isMember(DataRow p) {
                return true;
            }
        } };

    @Override
    String getDisplayName() {
        return "Priority";
    }

}
