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
					"The West Africa time zone is used by many countries in west-central Africa. " +
					"Most countries in this time zone do not use daylight savings time. " +
					"An exception to this rule is Namibia, which does observe daylight savings time. " +
					"When DST is not in effect, the time zone is 1 hour ahead of UTC (UTC+1). " +
					"When DST is in effect, the time zone is 2 hours ahead of UTC (UTC+2).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Africa_Western.*"));
				}
			},
			new Partition.Membership("North America - Eastern",
					"The North American Eastern time zone is used in many parts of the eastern United " +
					"States and Canada.  It is also used in some Caribbean nations such as Jamaica" +
					"and Haiti, and also in some central American countries such as Panama." +
					"Daylight savings time is observed in the US and Canada, but not observed in " +
					"many of the central American and Caribbean nations.  In countries that actually " +
					"use this time zone, it is referred to as simply \"Eastern Time\". " +
					"When DST is not in effect, the time zone is 5 hours behind UTC (UTC-5). " +
					"When DST is in effect, the time zone is 4 hours behind UTC (UTC-4).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|America_Eastern.*"));
				}
			},
			new Partition.Membership("North America - Central",
					"The North American Central time zone is used in many parts of the central United " +
					"States and Canada, as well as most of Mexico.  It is also used in some central American " +
					"countries such as Belize, Honduras, Costa Rica, El Salvador and Guatemala. " +
					"Daylight savings time is observed in the US, Canada and Mexico, but not observed in " +
					"many of the central American nations.  In countries that actually " +
					"use this time zone, such as the US and Canada, it is referred to as simply " +
					"\"Central Time\". " +
					"When DST is not in effect, the time zone is 6 hours behind UTC (UTC-6). " +
					"When DST is in effect, the time zone is 5 hours behind UTC (UTC-5).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|America_Central.*"));
				}
			},
			new Partition.Membership("North America - Mountain",
					"The North American Mountain time zone is used in many parts of the west-central United " +
					"States and Canada.  It is also used in some portions of Mexico. " +
					"Daylight savings time is observed in most areas that use this time zone. " +
					"In countries that actually " +
					"use this time zone, such as the US and Canada, it is referred to as simply " +
					"\"Mountain Time\". " +
					"When DST is not in effect, the time zone is 5 hours behind UTC (UTC-5). " +
					"When DST is in effect, the time zone is 4 hours behind UTC (UTC-4).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|America_Mountain.*"));
				}
			},
			new Partition.Membership("North America - Pacific",
					"The North American Pacific time zone is used in many parts of the eastern United " +
					"States and Canada.  It is also used in some Caribbean nations such as Jamaica" +
					"and Haiti, and also in some central American countries such as Panama." +
					"Daylight savings time is observed in the US and Canada, but not observed in " +
					"many of the central American and Caribbean nations.  In countries that actually " +
					"use this time zone, such as the US and Canada, it is referred to as simply " +
					"\"Pacific Time\". " +
					"When DST is not in effect, the time zone is 5 hours behind UTC (UTC-5). " +
					"When DST is in effect, the time zone is 4 hours behind UTC (UTC-4).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|America_Pacific.*"));
				}
			},
			new Partition.Membership("Alaska",
					"The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). " +
					"When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Alaska:.*"));
				}
			},
			new Partition.Membership("Alaska-Hawaii",
					"The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). " +
					"When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Alaska_Hawaii.*"));
				}
			},
			new Partition.Membership("Bering",
					"The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). " +
					"When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Bering.*"));
				}
			},
			new Partition.Membership("Yukon",
					"The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). " +
					"When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Yukon.*"));
				}
			},
			new Partition.Membership("Atlantic",
					"The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). " +
					"When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Atlantic.*"));
				}
			},
			new Partition.Membership("Newfoundland",
					"The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). " +
					"When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Newfoundland.*"));
				}
			},
			new Partition.Membership("Pierre and Miquelon",
					"The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). " +
					"When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Pierre_Miquelon.*"));
				}
			},
			new Partition.Membership("Brasilia",
					"The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). " +
					"When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Brasilia.*"));
				}
			},
			new Partition.Membership("Amazon",
					"The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). " +
					"When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Amazon.*"));
				}
			},
			new Partition.Membership("Noronha",
					"The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). " +
					"When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Noronha.*"));
				}
			},
			new Partition.Membership("Acre",
					"The Acre time zone was used in the state of Acre and some other parts of Brazil until 2008. " +
					"It is not currently in use, but may be reinstated by the Brazilian government at some " +
					"point in the future. The time zone did observe daylight savings time." +
					"When DST was not in effect, the time zone was 5 hours behind UTC (UTC-5). " +
					"When DST was in effect, the time zone was 4 hours behind UTC (UTC-4).") { 
				public boolean isMember(DataRow p) {
						String pp = p.getPrettyPath();
						return (pp != null && pp.matches("0-names\\|metazone\\|Acre.*"));
					}
			},
			new Partition.Membership("Argentina",
					"The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). " +
					"When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Argentina:.*"));
				}
			},
			new Partition.Membership("Western Argentina",
					"The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). " +
					"When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Argentina_Western.*"));
				}
			},
			new Partition.Membership("Bolivia",
					"The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). " +
					"When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Bolivia.*"));
				}
			},
			new Partition.Membership("Chile",
					"The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). " +
					"When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Chile.*"));
				}
			},
			new Partition.Membership("Colombia",
					"The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). " +
					"When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Colombia.*"));
				}
			},
			new Partition.Membership("Cuba",
					"The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). " +
					"When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Cuba.*"));
				}
			},
			new Partition.Membership("Dutch Guiana",
					"The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). " +
					"When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Dutch_Guiana.*"));
				}
			},
			new Partition.Membership("Ecuador",
					"The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). " +
					"When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Ecuador.*"));
				}
			},
			new Partition.Membership("French Guiana",
					"The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). " +
					"When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|French_Guiana.*"));
				}
			},
			new Partition.Membership("Eastern Greenland",
					"The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). " +
					"When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Greenland_Eastern.*"));
				}
			},
			new Partition.Membership("Central Greenland",
					"The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). " +
					"When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Greenland_Central.*"));
				}
			},
			new Partition.Membership("Western Greenland",
					"The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). " +
					"When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Greenland_Western.*"));
				}
			},
			new Partition.Membership("Guyana",
					"The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). " +
					"When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Guyana.*"));
				}
			},
			new Partition.Membership("Paraguay",
					"The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). " +
					"When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Paraguay.*"));
				}
			},
			new Partition.Membership("Peru",
					"The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). " +
					"When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Peru.*"));
				}
			},
			new Partition.Membership("Suriname",
					"The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). " +
					"When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Suriname.*"));
				}
			},
			new Partition.Membership("Uruguay",
					"The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). " +
					"When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Uruguay.*"));
				}
			},
			new Partition.Membership("Venezuela",
					"The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). " +
					"When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Venezuela.*"));
				}
			},

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