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
					"The North American Pacific time zone is used in many parts of the western United " +
					"States, Canada, and Mexico. " +
					"Daylight savings time is observed in most areas using this time zone.  In countries that actually " +
					"use this time zone, it is referred to as simply \"Pacific Time\". " +
					"When DST is not in effect, the time zone is 8 hours behind UTC (UTC-8). " +
					"When DST is in effect, the time zone is 7 hours behind UTC (UTC-7).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|America_Pacific.*"));
				}
			},
			new Partition.Membership("Alaska",
					"The Alaska time zone is used most parts of Alaska in the United States. " +
					"Daylight savings time is observed in this time zone." +
					"When DST is not in effect, the time zone was 9 hours behind UTC (UTC-9). " +
					"When DST is in effect, the time zone was 8 hours behind UTC (UTC-8).") { 
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
					"The Bering time zone was used in portions of western Alaska in the " +
					"United States until 1983.  It is not currently in use. " +
					"Daylight savings time was observed briefly in some areas, but was not the norm." +
					"When DST was not in effect, the time zone was 11 hours behind UTC (UTC-11). " +
					"When DST was in effect, the time zone was 10 hours behind UTC (UTC-10).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Bering.*"));
				}
			},
			new Partition.Membership("Yukon",
					"The Yukon time zone was used in the states of Alaska and Hawaii in the " +
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
					"The Atlantic time zone is used in portions of eastern Canada, as well as  " +
					"in the Caribbean territories of Bermuda, Dominican Republic, Trinidad and Tobago." +
					"Daylight savings time is observed in the parts of Canada that use this time zone." +
					"When DST is not in effect, the time zone is 4 hours behind UTC (UTC-4). " +
					"When DST is in effect, the time zone is 3 hours behind UTC (UTC-3).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Atlantic.*"));
				}
			},
			new Partition.Membership("Newfoundland",
					"The Newfoundland time zone is used in the province of Newfoundland in Canada. " +
					"Daylight savings time is observed in this time zone." +
					"When DST is not in effect, the time zone is 3 1/2 hours behind UTC (UTC-3:30). " +
					"When DST is in effect, the time zone is 2 1/2 hours behind UTC (UTC-2:30).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Newfoundland.*"));
				}
			},
			new Partition.Membership("Pierre and Miquelon",
					"The Pieere and Miquelon time zone is used in the territories of St. Pierre and Miquelon, " +
					"just south of Newfoundland, Canada. " +
					"Daylight savings time is observed in this time zone." +
					"When DST is not in effect, the time zone is 3 hours behind UTC (UTC-3). " +
					"When DST is in effect, the time zone is 2 hours behind UTC (UTC-2).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Pierre_Miquelon.*"));
				}
			},
			new Partition.Membership("Brasilia",
					"The Brasilia time zone is the predominant time zone used in Brazil, " +
					"covering the eastern portion of the country. " +
					"Daylight savings time is observed in some areas, and is not observed in others." +
					"When DST is not in effect, the time zone is 3 hours behind UTC (UTC-3). " +
					"When DST is in effect, the time zone is 2 hours behind UTC (UTC-2).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Brasilia.*"));
				}
			},
			new Partition.Membership("Amazon",
					"The Amazon time zone is used in portions of western Brazil. " +
					"Daylight savings time is observed in some areas, but not in others." +
					"When DST is not in effect, the time zone is 4 hours behind UTC (UTC-4). " +
					"When DST is in effect, the time zone is 3 hours behind UTC (UTC-3).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Amazon.*"));
				}
			},
			new Partition.Membership("Noronha",
					"The Noronha zone is used in some islands off the eastern coast of Brazil " +
					"Daylight savings time is not currently observed in this time zone, but has " +
					"been observed in some years prior to 2002." +
					"When DST is not in effect, the time zone is 2 hours behind UTC (UTC-2). " +
					"When DST was in effect, the time zone was 1 hour behind UTC (UTC-1).") { 
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
					"The Argentina time zone is used in most parts of Argentina. " +
					"Daylight savings time is not currently observed, but has been observed in some previous years. " +
					"When DST is not in effect, the time zone is 3 hours behind UTC (UTC-3). " +
					"When DST was in effect, the time zone was 2 hours behind UTC (UTC-2).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Argentina:.*"));
				}
			},
			new Partition.Membership("Western Argentina",
					"The western Argentina time zone is used in small portions of western Argentina. " +
					"Daylight savings time has observed in some years, but is not currently being used. " +
					"When DST is not in effect, the time zone is 4 hours behind UTC (UTC-4). " +
					"When DST was in effect, the time zone was 3 hours behind UTC (UTC-3).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Argentina_Western.*"));
				}
			},
			new Partition.Membership("Bolivia",
					"The Bolivia time zone is used in the country of Bolivia only. " +
			        "The time zone is 4 hours behind UTC (UTC-4) and does not use daylight savings time." ) {
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Bolivia.*"));
				}
			},
			new Partition.Membership("Chile",
					"The Chile time zone is used in most portions of the mainland of Chile. " +
					"Daylight savings time is observed in this time zone." +
					"When DST is not in effect, the time zone is 4 hours behind UTC (UTC-4). " +
					"When DST is in effect, the time zone is 3 hours behind UTC (UTC-3).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Chile.*"));
				}
			},
			new Partition.Membership("Colombia",
					"The Colombia time zone is used in the country of Colombia only. " +
			        "The time zone is 5 hours behind UTC (UTC-5) and does not use daylight savings time." + 
					"DST was observed for a brief period in 1992, during which time the time zone was UTC-4." ) {
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Colombia.*"));
				}
			},
			new Partition.Membership("Cuba",
					"The Cuba time zone is used in the country of Cuba only. " +
					"Daylight savings time is observed in this time zone. " +
					"When DST is not in effect, the time zone is 5 hours behind UTC (UTC-5). " +
					"When DST is in effect, the time zone is 4 hours behind UTC (UTC-4).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Cuba.*"));
				}
			},
			new Partition.Membership("Dutch Guiana",
					"The Dutch Guiana time zone was used in the country of Suriname " +
					"until 1975.  It is not currently in use. " +
					"Daylight savings time was not used in this time zone." +
					"The time zone was 3 1/2 hours behind UTC (UTC-3:30). " ) {
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Dutch_Guiana.*"));
				}
			},
			new Partition.Membership("Ecuador",
					"The Ecuador time zone is used in the mainland portions Ecuador only (not the Galapagos Islands). " +
	                "The time zone is 5 hours behind UTC (UTC-5) and does not use daylight savings time.") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Ecuador.*"));
				}
			},
			new Partition.Membership("French Guiana",
					"The French Guiana time zone is used only in the territory of French Guiana in South America. " +
	                "The time zone is 3 hours behind UTC (UTC-3) and does not use daylight savings time.") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|French_Guiana.*"));
				}
			},
			new Partition.Membership("Eastern Greenland",
					"The Eastern Greenland time zone is used in a small portion of Greenland on the " +
					"eastern coast. Daylight savings time is observed in this time zone. " +
					"When DST is not in effect, the time zone is 1 hour behind UTC (UTC-1). " +
					"When DST is in effect, the time zone is the same as UTC.") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Greenland_Eastern.*"));
				}
			},
			new Partition.Membership("Central Greenland",
					"The Central Greenland time zone was used in portions of Greenland " +
					"until 1981.  It is not currently in use. " +
					"Daylight savings time was observed when the time zone was in use." +
					"When DST was not in effect, the time zone was 2 hours behind UTC (UTC-2). " +
					"When DST was in effect, the time zone was 1 hour behind UTC (UTC-1).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Greenland_Central.*"));
				}
			},
			new Partition.Membership("Western Greenland",
					"The Western Greenland time zone is used vast majority of areas of Greenland. " +
					"Daylight savings time is observed in this time zone. " +
					"When DST is not in effect, the time zone is 3 hours behind UTC (UTC-3). " +
					"When DST is in effect, the time zone is 2 hours behind UTC (UTC-2).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Greenland_Western.*"));
				}
			},
			new Partition.Membership("Guyana",
					"The Guyana time zone is used in the country of Guyana only. " +
					"The time zone is 4 hours behind UTC (UTC-4) and does not use daylight savings time." ) {
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Guyana.*"));
				}
			},
			new Partition.Membership("Paraguay",
					"The Paraguay time zone is used in the country of Paraguay only. " +
    				"Daylight savings time is observed in this time zone. " +
	    			"When DST is not in effect, the time zone is 4 hours behind UTC (UTC-4). " +
		    		"When DST is in effect, the time zone is 3 hours behind UTC (UTC-3).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Paraguay.*"));
				}
			},
			new Partition.Membership("Peru",
					"The Peru time zone is used in the country of Peru only. " +
					"The time zone is 5 hours behind UTC (UTC-5) and does not use daylight savings time." ) {
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Peru.*"));
				}
			},
			new Partition.Membership("Suriname",
					"The Suriname time zone is used in the country of Suriname in South America. " +
					"The time zone is 3 hours behind UTC (UTC-3) and does not use daylight savings time." ) {
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Suriname.*"));
				}
			},
			new Partition.Membership("Uruguay",
					"The Uruguay time zone is used in the country of Uruguay only. " +
		    		"Daylight savings time is observed in this time zone. " +
			    	"When DST is not in effect, the time zone is 3 hours behind UTC (UTC-3). " +
				    "When DST is in effect, the time zone is 2 hours behind UTC (UTC-2).") { 
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Uruguay.*"));
				}
			},
			new Partition.Membership("Venezuela",
					"The Venezuela time zone is used in the country of Venezuela only. " +
					"The time zone is 4 1/2 hours behind UTC (UTC-4:30) and does not use daylight savings time." ) {
				public boolean isMember(DataRow p) {
					String pp = p.getPrettyPath();
					return (pp != null && pp.matches("0-names\\|metazone\\|Venezuela.*"));
				}
			},

	};
	// TODO: Add rest of continents to the above.
    
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