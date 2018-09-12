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
        // return
        // (SurveyMain.isPhaseSubmit()||SurveyMain.isPhaseVetting())?submitMemberships:vettingMemberships;
        return memberships;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.unicode.cldr.web.SortMode#createComparator()
     */
    @Override
    Comparator<DataRow> createComparator() {
        return comparator();
    }

    static Comparator<DataRow> comparator() {
        final int ourKey = SortMode.SortKeyType.SORTKEY_INTEREST.ordinal();

        final Comparator<DataRow> nameComparator = NameSort.comparator();
        final Collator collator = CodeSortMode.createCollator();
        return new Comparator<DataRow>() {

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

                // if(rv == 0) { // try to avoid a compare
                // String p1d = null;
                // String p2d = null;
                // if(canName) {
                // p1d = p1.displayName;
                // p2d = p2.displayName;
                // }
                // if(p1d == null ) {
                // p1d = p1.type;
                // if(p1d == null) {
                // p1d = "(null)";
                // }
                // }
                // if(p2d == null ) {
                // p2d = p2.type;
                // if(p2d == null) {
                // p2d = "(null)";
                // }
                // }
                // rv = myCollator.compare(p1d, p2d);
                // }
                //
                // if(rv == 0) {
                // // Question for Steven. It doesn't appear that the null
                // checks would be needed, since they aren't in COMPARE_BY_CODE
                // String p1d = p1.type;
                // String p2d = p2.type;
                // if(p1d == null ) {
                // p1d = "(null)";
                // }
                // if(p2d == null ) {
                // p2d = "(null)";
                // }
                // rv = myCollator.compare(p1d, p2d);
                // }
                //
                // if(rv < 0) {
                // return -1;
                // } else if(rv > 0) {
                // return 1;
                // } else {
                // return 0;
                // }
            }
        };
    }

    private static Partition.Membership memberships[] = { new Partition.Membership("Errors") {
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
            public boolean isMember(DataRow p) {
                return p.winningXpathId != -1 && p.confirmStatus != Status.approved && p.confirmStatus != Status.contributed;
            }
        }, new Partition.Membership("Approved") {
            public boolean isMember(DataRow p) {
                return p.winningXpathId != -1; // will be APPROVED
            }
        }, new Partition.Membership("Missing") {
            public boolean isMember(DataRow p) {
                // found inherited item (extrapaths and some special paths may not
                // have an inherited item)
                /*
                 * TODO: if inheritedLocale moves from CandidateItem to DataRow,
                 * then p.inheritedItem.inheritedLocale may be simplified to p.inheritedLocale here.
                 * Also, should change all "DataRow p" to "DataRow row".
                 */
                return p.inheritedItem != null &&
                    (CLDRLocale.ROOT == p.inheritedItem.inheritedLocale || XMLSource.CODE_FALLBACK_ID
                        .equals(p.inheritedItem.inheritedLocale.getBaseName()));
            }
        }, new Partition.Membership("Inherited") {
            public boolean isMember(DataRow p) {
                return true;
            }
        } };

    @Override
    String getDisplayName() {
        return "Priority";
    }

}
