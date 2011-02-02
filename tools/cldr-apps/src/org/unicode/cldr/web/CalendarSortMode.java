/**
 * Copyright (C) 2011 IBM Corporation and Others. All Rights Reserved.
 *
 */
package org.unicode.cldr.web;

import java.util.Comparator;

import org.unicode.cldr.web.DataSection.DataRow;
import org.unicode.cldr.web.Partition.Membership;

/**
 * @author srl
 *
 */
public class CalendarSortMode extends SortMode {
	public static String name = SurveyMain.PREF_SORTMODE_CODE_CALENDAR;
	/* (non-Javadoc)
	 * @see org.unicode.cldr.web.SortMode#getName()
	 */
	@Override
	String getName() {
		return name;
	}
	
	private static final Partition.Membership memberships[]  = {                 
			new Partition.Membership("Date Formats") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("calendar-.*\\|pattern\\|date-.*"));
				}
			},
			new Partition.Membership("Time Formats") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("calendar-.*\\|pattern\\|time-.*"));
				}
			},
			new Partition.Membership("Date/Time Combination Formats") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("calendar-.*\\|pattern\\|datetime-.*"));
				}
			},
			new Partition.Membership("Wide Month Names") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("calendar-.*\\|month\\|.*-format-wide"));
				}
			},
			new Partition.Membership("Abbreviated Month Names") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("calendar-.*\\|month\\|.*-format-abbreviated"));
				}
			},
			new Partition.Membership("Narrow Month Names") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("calendar-.*\\|month\\|.*-stand-alone-narrow"));
				}
			},
			new Partition.Membership("Wide Month Names (Stand Alone Context)") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("calendar-.*\\|month\\|.*-stand-alone-wide"));
				}
			},
			new Partition.Membership("Abbreviated Month Names (Stand Alone Context)") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("calendar-.*\\|month\\|.*-stand-alone-abbreviated"));
				}
			},
			new Partition.Membership("Narrow Month Names (Format Context)") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("calendar-.*\\|month\\|.*-format-narrow"));
				}
			},
			new Partition.Membership("Wide Day Names") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("calendar-.*\\|day\\|.*:format-wide"));
				}
			},
			new Partition.Membership("Abbreviated Day Names") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("calendar-.*\\|day\\|.*:format-abbreviated"));
				}
			},
			new Partition.Membership("Narrow Day Names") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("calendar-.*\\|day\\|.*:stand-alone-narrow"));
				}
			},
			new Partition.Membership("Wide Day Names (Stand Alone Context)") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("calendar-.*\\|day\\|.*:stand-alone-wide"));
				}
			},
			new Partition.Membership("Abbreviated Day Names (Stand Alone Context)") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("calendar-.*\\|day\\|.*:stand-alone-abbreviated"));
				}
			},
			new Partition.Membership("Narrow Day Names (Format Context)") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("calendar-.*\\|day\\|.*:format-narrow"));
				}
			},
			new Partition.Membership("Wide Quarter Names") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("calendar-.*\\|quarter\\|.*-format-wide"));
				}
			},
			new Partition.Membership("Abbreviated Quarter Names") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("calendar-.*\\|quarter\\|.*-format-abbreviated"));
				}
			},
			new Partition.Membership("Narrow Quarter Names") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("calendar-.*\\|quarter\\|.*-stand-alone-narrow"));
				}
			},
			new Partition.Membership("Wide Quarter Names (Stand Alone Context)") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("calendar-.*\\|quarter\\|.*-stand-alone-wide"));
				}
			},
			new Partition.Membership("Abbreviated Quarter Names (Stand Alone Context)") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("calendar-.*\\|quarter\\|.*-stand-alone-abbreviated"));
				}
			},
			new Partition.Membership("Narrow Quarter Names (Format Context)") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("calendar-.*\\|quarter\\|.*-format-narrow"));
				}
			},
			new Partition.Membership("Day Periods") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("calendar-.*\\|dayPeriod.*"));
				}
			},
			new Partition.Membership("Eras") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("calendar-.*\\|era\\|.*"));
				}
			},
			new Partition.Membership("Relative Field Names") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("calendar-.*\\|fields\\|.*"));
				}
			},
			new Partition.Membership("Calendar Field Labels") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("calendar-.*\\|field-label\\|.*"));
				}
			},
			new Partition.Membership("Flexible Date/Time Formats") { 
				public boolean isMember(DataRow p) {
					int xpint = p.getXpathId();
					String xp = p.getXpath();
					return (xpint == -1 || (xp != null && xp.indexOf("availableFormats")>-1));
				}
			},
			new Partition.Membership("Interval Formats") { 
				public boolean isMember(DataRow p) {
					String xp = p.getXpath();
					return (xp != null && xp.indexOf("intervalFormats")>-1);
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
