/**
 * Copyright (C) 2011 IBM Corporation and Others. All Rights Reserved.
 *
 */
package org.unicode.cldr.web;

import java.util.Comparator;

import org.unicode.cldr.web.DataSection.DataRow;

/**
 * @author jce
 *
 */
public class MetazoneSortMode extends SortMode {
	public static String name = SurveyMain.PREF_SORTMODE_METAZONE;
	/* (non-Javadoc)
	 * @see org.unicode.cldr.web.SortMode#getName()
	 */
	@Override
	String getName() {
		return name;
	}
	
	private static final Partition.Membership memberships[]  = {                 
			new Partition.Membership("Central Africa",
					"The Central Africa time zone is used by many countries in central and southern Africa. " +
			        "The time zone is 2 hours ahead of UTC (UTC+2) and does not use daylight savings time.") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Africa_Central.*"));
				}
			},
			new Partition.Membership("East Africa",
					"The East Africa time zone is used by many countries in eastern Africa. " +
					"The time zone is 3 hours ahead of UTC (UTC+3) and does not use daylight savings time.") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Africa_Eastern.*"));
				}
			},
			new Partition.Membership("South Africa",
					"The South Africa time zone is used in the countries of South Africa, Swaziland, and Lesotho. " +
					"The time zone is 2 hours ahead of UTC (UTC+2) and does not use daylight savings time.") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Africa_Southern.*"));
				}
			},
			new Partition.Membership("West Africa",
					"The West Africa time one is used by many countries in west-central Africa. " +
					"Most countries in this time zone do not use daylight savings time. " +
					"An exception to this rule is Namibia, which does observe daylight savings time. " +
					"When DST is not in effect, the time zone is 1 hour ahead of UTC (UTC+1). " +
					"When DST is in effect, the time zone is 2 hours ahead of UTC (UTC+2). ") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Africa_Western.*"));
				}
			}
	};
	
    
    @Override
    Partition.Membership[] memberships() {
    	return memberships;
    }

	
	@Override
	Comparator<DataRow> createComparator() {
		return comparator();
	}
	
	public static
	Comparator<DataRow> comparator() {
		final int ourKey = SortMode.SortKeyType.SORTKEY_CALENDAR.ordinal();
		final Comparator<DataRow> codeComparator = CodeSortMode.comparator();
		return new Comparator<DataRow>() {
			public int compare(DataRow p1, DataRow p2){
				if(p1==p2) {
					return 0;
				}

				int rv = 0; // neg:  a < b.  pos: a> b
				
				rv = compareMembers(p1,p2,memberships, ourKey);
				if(rv != 0) {
					return rv;
				}

				return codeComparator.compare(p1,p2); // fall back to code

			}
		};
	}


}