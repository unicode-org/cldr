/**
 * Copyright (C) 2011-2012 IBM Corporation and Others. All Rights Reserved.
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

    /*
     * (non-Javadoc)
     *
     * @see org.unicode.cldr.web.SortMode#getName()
     */
    @Override
    String getName() {
        return name;
    }

    private static final Partition.Membership memberships[] = {
        new Partition.Membership("Central Africa",
            "The Central Africa time zone is used by many countries in central and southern Africa. "
                + "The time zone is 2 hours ahead of UTC (UTC+2) and does not use daylight savings time.") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Africa_Central:.++"));
            }
        },
        new Partition.Membership("East Africa", "The East Africa time zone is used by many countries in eastern Africa. "
            + "The time zone is 3 hours ahead of UTC (UTC+3) and does not use daylight savings time.") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Africa_Eastern:.++"));
            }
        },
        new Partition.Membership("South Africa",
            "The South Africa time zone is used in the countries of South Africa, Swaziland, and Lesotho. "
                + "The time zone is 2 hours ahead of UTC (UTC+2) and does not use daylight savings time.") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Africa_Southern:.++"));
            }
        },
        new Partition.Membership("West Africa",
            "The West Africa time zone is used by many countries in west-central Africa. "
                + "Most countries in this time zone do not use daylight savings time. "
                + "An exception to this rule is Namibia, which does observe daylight savings time. "
                + "When DST is not in effect, the time zone is 1 hour ahead of UTC (UTC+1). "
                + "When DST is in effect, the time zone is 2 hours ahead of UTC (UTC+2).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Africa_Western:.++"));
            }
        },
        new Partition.Membership("North America - Eastern",
            "The North American Eastern time zone is used in many parts of the eastern United "
                + "States and Canada.  It is also used in some Caribbean nations such as Jamaica"
                + "and Haiti, and also in some central American countries such as Panama."
                + "Daylight savings time is observed in the US and Canada, but not observed in "
                + "many of the central American and Caribbean nations.  In countries that actually "
                + "use this time zone, it is referred to as simply \"Eastern Time\". "
                + "When DST is not in effect, the time zone is 5 hours behind UTC (UTC-5). "
                + "When DST is in effect, the time zone is 4 hours behind UTC (UTC-4).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|America_Eastern:.++"));
            }
        },
        new Partition.Membership("North America - Central",
            "The North American Central time zone is used in many parts of the central United "
                + "States and Canada, as well as most of Mexico.  It is also used in some central American "
                + "countries such as Belize, Honduras, Costa Rica, El Salvador and Guatemala. "
                + "Daylight savings time is observed in the US, Canada and Mexico, but not observed in "
                + "many of the central American nations.  In countries that actually "
                + "use this time zone, such as the US and Canada, it is referred to as simply "
                + "\"Central Time\". " + "When DST is not in effect, the time zone is 6 hours behind UTC (UTC-6). "
                + "When DST is in effect, the time zone is 5 hours behind UTC (UTC-5).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|America_Central:.++"));
            }
        },
        new Partition.Membership("North America - Mountain",
            "The North American Mountain time zone is used in many parts of the west-central United "
                + "States and Canada.  It is also used in some portions of Mexico. "
                + "Daylight savings time is observed in most areas that use this time zone. "
                + "In countries that actually "
                + "use this time zone, such as the US and Canada, it is referred to as simply "
                + "\"Mountain Time\". " + "When DST is not in effect, the time zone is 5 hours behind UTC (UTC-5). "
                + "When DST is in effect, the time zone is 4 hours behind UTC (UTC-4).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|America_Mountain:.++"));
            }
        },
        new Partition.Membership(
            "North America - Pacific",
            "The North American Pacific time zone is used in many parts of the western United "
                + "States, Canada, and Mexico. "
                + "Daylight savings time is observed in most areas using this time zone.  In countries that actually "
                + "use this time zone, it is referred to as simply \"Pacific Time\". "
                + "When DST is not in effect, the time zone is 8 hours behind UTC (UTC-8). "
                + "When DST is in effect, the time zone is 7 hours behind UTC (UTC-7).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|America_Pacific:.++"));
            }
        },
        new Partition.Membership("Alaska", "The Alaska time zone is used most parts of Alaska in the United States. "
            + "Daylight savings time is observed in this time zone."
            + "When DST is not in effect, the time zone was 9 hours behind UTC (UTC-9). "
            + "When DST is in effect, the time zone was 8 hours behind UTC (UTC-8).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Alaska:.++"));
            }
        },
        new Partition.Membership("Alaska-Hawaii",
            "The Alaska-Hawaii time zone was used in the states of Alaska and Hawaii in the "
                + "United States until 1983.  It is not currently in use. "
                + "Daylight savings time was observed briefly in some areas, but was not the norm."
                + "When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). "
                + "When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Alaska_Hawaii:.++"));
            }
        },
        new Partition.Membership("Bering", "The Bering time zone was used in portions of western Alaska in the "
            + "United States until 1983.  It is not currently in use. "
            + "Daylight savings time was observed briefly in some areas, but was not the norm."
            + "When DST was not in effect, the time zone was 11 hours behind UTC (UTC-11). "
            + "When DST was in effect, the time zone was 10 hours behind UTC (UTC-10).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Bering:.++"));
            }
        },
        new Partition.Membership("Yukon", "The Yukon time zone was used in the states of Alaska and Hawaii in the "
            + "United States until 1983.  It is not currently in use. "
            + "Daylight savings time was observed briefly in some areas, but was not the norm."
            + "When DST was not in effect, the time zone was 10 hours behind UTC (UTC-10). "
            + "When DST was in effect, the time zone was 9 hours behind UTC (UTC-9).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Yukon:.++"));
            }
        },
        new Partition.Membership("Atlantic", "The Atlantic time zone is used in portions of eastern Canada, as well as  "
            + "in the Caribbean territories of Bermuda, Dominican Republic, Trinidad and Tobago."
            + "Daylight savings time is observed in the parts of Canada that use this time zone."
            + "When DST is not in effect, the time zone is 4 hours behind UTC (UTC-4). "
            + "When DST is in effect, the time zone is 3 hours behind UTC (UTC-3).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Atlantic:.++"));
            }
        },
        new Partition.Membership("Newfoundland",
            "The Newfoundland time zone is used in the province of Newfoundland in Canada. "
                + "Daylight savings time is observed in this time zone."
                + "When DST is not in effect, the time zone is 3 1/2 hours behind UTC (UTC-3:30). "
                + "When DST is in effect, the time zone is 2 1/2 hours behind UTC (UTC-2:30).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Newfoundland:.++"));
            }
        },
        new Partition.Membership("Pierre and Miquelon",
            "The Pieere and Miquelon time zone is used in the territories of St. Pierre and Miquelon, "
                + "just south of Newfoundland, Canada. " + "Daylight savings time is observed in this time zone."
                + "When DST is not in effect, the time zone is 3 hours behind UTC (UTC-3). "
                + "When DST is in effect, the time zone is 2 hours behind UTC (UTC-2).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Pierre_Miquelon:.++"));
            }
        },
        new Partition.Membership("Brasilia", "The Brasilia time zone is the predominant time zone used in Brazil, "
            + "covering the eastern portion of the country. "
            + "Daylight savings time is observed in some areas, and is not observed in others."
            + "When DST is not in effect, the time zone is 3 hours behind UTC (UTC-3). "
            + "When DST is in effect, the time zone is 2 hours behind UTC (UTC-2).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Brasilia:.++"));
            }
        },
        new Partition.Membership("Amazon", "The Amazon time zone is used in portions of western Brazil. "
            + "Daylight savings time is observed in some areas, but not in others."
            + "When DST is not in effect, the time zone is 4 hours behind UTC (UTC-4). "
            + "When DST is in effect, the time zone is 3 hours behind UTC (UTC-3).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Amazon:.++"));
            }
        },
        new Partition.Membership("Noronha", "The Noronha zone is used in some islands off the eastern coast of Brazil "
            + "Daylight savings time is not currently observed in this time zone, but has "
            + "been observed in some years prior to 2002."
            + "When DST is not in effect, the time zone is 2 hours behind UTC (UTC-2). "
            + "When DST was in effect, the time zone was 1 hour behind UTC (UTC-1).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Noronha:.++"));
            }
        },
        new Partition.Membership("Acre",
            "The Acre time zone was used in the state of Acre and some other parts of Brazil until 2008. "
                + "It is not currently in use, but may be reinstated by the Brazilian government at some "
                + "point in the future. The time zone did observe daylight savings time."
                + "When DST was not in effect, the time zone was 5 hours behind UTC (UTC-5). "
                + "When DST was in effect, the time zone was 4 hours behind UTC (UTC-4).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Acre:.++"));
            }
        },
        new Partition.Membership("Argentina", "The Argentina time zone is used in most parts of Argentina. "
            + "Daylight savings time is not currently observed, but has been observed in some previous years. "
            + "When DST is not in effect, the time zone is 3 hours behind UTC (UTC-3). "
            + "When DST was in effect, the time zone was 2 hours behind UTC (UTC-2).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Argentina::.++"));
            }
        },
        new Partition.Membership("Western Argentina",
            "The western Argentina time zone is used in small portions of western Argentina. "
                + "Daylight savings time has observed in some years, but is not currently being used. "
                + "When DST is not in effect, the time zone is 4 hours behind UTC (UTC-4). "
                + "When DST was in effect, the time zone was 3 hours behind UTC (UTC-3).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Argentina_Western:.++"));
            }
        },
        new Partition.Membership("Bolivia", "The Bolivia time zone is used in the country of Bolivia only. "
            + "The time zone is 4 hours behind UTC (UTC-4) and does not use daylight savings time.") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Bolivia:.++"));
            }
        },
        new Partition.Membership("Chile", "The Chile time zone is used in most portions of the mainland of Chile. "
            + "Daylight savings time is observed in this time zone."
            + "When DST is not in effect, the time zone is 4 hours behind UTC (UTC-4). "
            + "When DST is in effect, the time zone is 3 hours behind UTC (UTC-3).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Chile:.++"));
            }
        },
        new Partition.Membership("Colombia", "The Colombia time zone is used in the country of Colombia only. "
            + "The time zone is 5 hours behind UTC (UTC-5) and does not use daylight savings time."
            + "DST was observed for a brief period in 1992, during which time the time zone was UTC-4.") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Colombia:.++"));
            }
        },
        new Partition.Membership("Cuba", "The Cuba time zone is used in the country of Cuba only. "
            + "Daylight savings time is observed in this time zone. "
            + "When DST is not in effect, the time zone is 5 hours behind UTC (UTC-5). "
            + "When DST is in effect, the time zone is 4 hours behind UTC (UTC-4).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Cuba:.++"));
            }
        },
        new Partition.Membership("Dutch Guiana", "The Dutch Guiana time zone was used in the country of Suriname "
            + "until 1975.  It is not currently in use. " + "Daylight savings time was not used in this time zone."
            + "The time zone was 3 1/2 hours behind UTC (UTC-3:30). ") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Dutch_Guiana:.++"));
            }
        },
        new Partition.Membership("Ecuador",
            "The Ecuador time zone is used in the mainland portions Ecuador only (not the Galapagos Islands). "
                + "The time zone is 5 hours behind UTC (UTC-5) and does not use daylight savings time.") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Ecuador:.++"));
            }
        },
        new Partition.Membership("French Guiana",
            "The French Guiana time zone is used only in the territory of French Guiana in South America. "
                + "The time zone is 3 hours behind UTC (UTC-3) and does not use daylight savings time.") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|French_Guiana:.++"));
            }
        },
        new Partition.Membership("Eastern Greenland",
            "The Eastern Greenland time zone is used in a small portion of Greenland on the "
                + "eastern coast. Daylight savings time is observed in this time zone. "
                + "When DST is not in effect, the time zone is 1 hour behind UTC (UTC-1). "
                + "When DST is in effect, the time zone is the same as UTC.") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Greenland_Eastern:.++"));
            }
        },
        new Partition.Membership("Central Greenland", "The Central Greenland time zone was used in portions of Greenland "
            + "until 1981.  It is not currently in use. "
            + "Daylight savings time was observed when the time zone was in use."
            + "When DST was not in effect, the time zone was 2 hours behind UTC (UTC-2). "
            + "When DST was in effect, the time zone was 1 hour behind UTC (UTC-1).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Greenland_Central:.++"));
            }
        },
        new Partition.Membership("Western Greenland",
            "The Western Greenland time zone is used vast majority of areas of Greenland. "
                + "Daylight savings time is observed in this time zone. "
                + "When DST is not in effect, the time zone is 3 hours behind UTC (UTC-3). "
                + "When DST is in effect, the time zone is 2 hours behind UTC (UTC-2).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Greenland_Western:.++"));
            }
        },
        new Partition.Membership("Guyana", "The Guyana time zone is used in the country of Guyana only. "
            + "The time zone is 4 hours behind UTC (UTC-4) and does not use daylight savings time.") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Guyana:.++"));
            }
        },
        new Partition.Membership("Paraguay", "The Paraguay time zone is used in the country of Paraguay only. "
            + "Daylight savings time is observed in this time zone. "
            + "When DST is not in effect, the time zone is 4 hours behind UTC (UTC-4). "
            + "When DST is in effect, the time zone is 3 hours behind UTC (UTC-3).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Paraguay:.++"));
            }
        },
        new Partition.Membership("Peru", "The Peru time zone is used in the country of Peru only. "
            + "The time zone is 5 hours behind UTC (UTC-5) and does not use daylight savings time.") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Peru:.++"));
            }
        },
        new Partition.Membership("Suriname", "The Suriname time zone is used in the country of Suriname in South America. "
            + "The time zone is 3 hours behind UTC (UTC-3) and does not use daylight savings time.") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Suriname:.++"));
            }
        },
        new Partition.Membership("Uruguay", "The Uruguay time zone is used in the country of Uruguay only. "
            + "Daylight savings time is observed in this time zone. "
            + "When DST is not in effect, the time zone is 3 hours behind UTC (UTC-3). "
            + "When DST is in effect, the time zone is 2 hours behind UTC (UTC-2).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Uruguay:.++"));
            }
        },
        new Partition.Membership("Venezuela", "The Venezuela time zone is used in the country of Venezuela only. "
            + "The time zone is 4 1/2 hours behind UTC (UTC-4:30) and does not use daylight savings time.") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Venezuela:.++"));
            }
        },
        new Partition.Membership("Casey", "The Casey time zone is used by the Casey station in Antarctica. "
            + "The time zone is 8 hours ahead of UTC (UTC+8) and does not use daylight savings time.") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Casey:.++"));
            }
        },
        new Partition.Membership("Davis", "The Davis time zone is used by the Davis station in Antarctica. "
            + "The time zone is 7 hours ahead of UTC (UTC+7) and does not use daylight savings time.") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Davis:.++"));
            }
        },
        new Partition.Membership("Dumont d&apos;Urville",
            "The Dumont d&apos;Urville time zone is used by the Dumont d&apos;Urville station in Antarctica. "
                + "The time zone is 10 hours ahead of UTC (UTC+10) and does not use daylight savings time.") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|DumontDUrville:.++"));
            }
        },
        new Partition.Membership("Macquarie",
            "The Macquarie time zone was used on Macquarie Island during portions of the year 2010. "
                + "It is no longer an active time zone, as Macquarie Island now uses Australian Eastern Time."
                + "The time zone was 11 hours ahead of UTC (UTC+11) and did not use daylight savings time.") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Macquarie:.++"));
            }
        },
        new Partition.Membership("Mawson", "The Mawson time zone is used by the Mawson station in Antarctica. "
            + "The time zone is 5 hours ahead of UTC (UTC+5) and does not use daylight savings time.") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Mawson:.++"));
            }
        },
        new Partition.Membership("Rothera", "The Rothera time zone is used by the Rothera station in Antarctica. "
            + "The time zone is 3 hours behind UTC (UTC-3) and does not use daylight savings time.") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Rothera:.++"));
            }
        },
        new Partition.Membership("Syowa", "The Syowa time zone is used by the Syowa station in Antarctica. "
            + "The time zone is 3 hours ahead of UTC (UTC+3) and does not use daylight savings time.") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Syowa:.++"));
            }
        },
        new Partition.Membership("Vostok", "The Vostok time zone is used by the Vostok station in Antarctica. "
            + "The time zone is 6 hours ahead of UTC (UTC+6) and does not use daylight savings time.") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Vostok:.++"));
            }
        },
        new Partition.Membership("Afghanistan", "The Afghanistan time zone is used in the country of Afghanistan only. "
            + "The time zone is 4 1/2 hours ahead of UTC (UTC+4:30) and does not use daylight savings time.") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Afghanistan:.++"));
            }
        },
        new Partition.Membership("Aktyubinsk", "The Aktyubinsk time zone was used in some areas of Kazakhstan "
            + "until 1991.  It is not currently in use. " + "Daylight savings time was observed in this time zone."
            + "When DST was not in effect, the time zone was 5 hours ahead of UTC (UTC+5). "
            + "When DST was in effect, the time zone was 6 hours ahead of UTC (UTC+6).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Aktyubinsk:.++"));
            }
        },
        new Partition.Membership("Almaty", "The Almaty time zone was used in some areas of Kazakhstan "
            + "until 2005.  It is not currently in use. " + "Daylight savings time was observed in this time zone."
            + "When DST was not in effect, the time zone was 6 hours ahead of UTC (UTC+6). "
            + "When DST was in effect, the time zone was 7 hours ahead of UTC (UTC+7).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Almaty:.++"));
            }
        },
        new Partition.Membership("Aqtau", "The Aqtau time zone was used in some areas of Kazakhstan "
            + "until 2005.  It is not currently in use. " + "Daylight savings time was observed in this time zone."
            + "When DST was not in effect, the time zone was 4 hours ahead of UTC (UTC+4). "
            + "When DST was in effect, the time zone was 5 hours ahead of UTC (UTC+5).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Aqtau:.++"));
            }
        },
        new Partition.Membership("Aqtobe", "The Aqtobe time zone was used in some areas of Kazakhstan "
            + "until 2005.  It is not currently in use. " + "Daylight savings time was observed in this time zone."
            + "When DST was not in effect, the time zone was 5 hours ahead of UTC (UTC+5). "
            + "When DST was in effect, the time zone was 6 hours ahead of UTC (UTC+6).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Aqtobe:.++"));
            }
        },
        new Partition.Membership("Arabian", "The Arabian time zone is used in the countries of Saudi Arabia, "
            + "Bahrain, Iraq, Kuwait, Qatar, and Yemen. "
            + "Daylight savings time is usually not observed, but has been observed during some periods,"
            + "especially in Iraq. " + "When DST is not in effect, the time zone is 3 hours ahead of UTC (UTC+3). "
            + "When DST is in effect, the time zone is 4 hours ahead of UTC (UTC+4).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Arabian:.++"));
            }
        },
        new Partition.Membership("Armenia", "The Armenia time zone is used in the country of Armenia only. "
            + "Daylight savings time is observed in this time zone. "
            + "When DST is not in effect, the time zone is 4 hours ahead of UTC (UTC+4). "
            + "When DST is in effect, the time zone is 5 hours ahead of UTC (UTC+5).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Armenia:.++"));
            }
        },
        new Partition.Membership("Ashkhabad", "The Ashkhabad time zone was used in the country of Turkmenistan "
            + "until 1991.  It is not currently in use. " + "Daylight savings time was observed in this time zone."
            + "When DST was not in effect, the time zone was 5 hours ahead of UTC (UTC+5). "
            + "When DST was in effect, the time zone was 6 hours ahead of UTC (UTC+6).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Ashkhabad:.++"));
            }
        },
        new Partition.Membership("Azerbaijan", "The Azerbaijan time zone is used in the country of Azerbaijan only. "
            + "Daylight savings time is observed in this time zone. "
            + "When DST is not in effect, the time zone is 4 hours ahead of UTC (UTC+4). "
            + "When DST is in effect, the time zone is 5 hours ahead of UTC (UTC+5).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Azerbaijan:.++"));
            }
        },
        new Partition.Membership("Baku",
            "The Baku time zone was used in the country of Azerbaijan before its independence in 1991. "
                + "It is not currently in use. Daylight savings time was observed in this time zone. "
                + "When DST was not in effect, the time zone was 3 hours ahead of UTC (UTC+3). "
                + "When DST was in effect, the time zone was 4 hours ahead of UTC (UTC+4).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Baku:.++"));
            }
        },
        new Partition.Membership("Bangladesh", "The Bangladesh time zone is used in the country of Bangladesh only. "
            + "Daylight savings time is observed in this time zone. "
            + "When DST is not in effect, the time zone is 4 hours ahead of UTC (UTC+6). "
            + "When DST is in effect, the time zone is 5 hours ahead of UTC (UTC+7).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Bangladesh:.++"));
            }
        },
        new Partition.Membership("Bhutan", "The Bhutan time zone is used in the country of Bhutan only. "
            + "The time zone is 6 hours ahead of UTC (UTC+6) and does not use daylight savings time.") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Bhutan:.++"));
            }
        },
        new Partition.Membership("Borneo", "The Borneo time zone was used in the country of Malaysia "
            + "until 1982.  It is not currently in use. " + "Daylight savings time was observed in this time zone."
            + "When DST was not in effect, the time zone was 8 hours ahead of UTC (UTC+8). "
            + "When DST was in effect, the time zone was 9 hours ahead of UTC (UTC+9).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Borneo:.++"));
            }
        },
        new Partition.Membership("Brunei", "The Brunei time zone is used in the country of Brunei only. "
            + "The time zone is 8 hours ahead of UTC (UTC+8) and does not use daylight savings time.") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Brunei:.++"));
            }
        },
        new Partition.Membership("Changbai",
            "The Changbai time zone was used in portions of China prior to 1980. It is not currently in use."
                + "The time zone was 8 1/2 hours ahead of UTC (UTC+8:30) and did not use daylight savings time.") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Changbai:.++"));
            }
        },
        new Partition.Membership("China",
            "The China time zone is used throughout the country of China. China normally does not observe "
                + "daylight savings time, but has done so for some brief periods during its history. "
                + "When DST is not in effect, the time zone is 8 hours ahead of UTC (UTC+8). "
                + "When DST is in effect, the time zone is 9 hours ahead of UTC (UTC+9).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|China:.++"));
            }
        },
        new Partition.Membership("Choibalsan",
            "The Choibalsan time zone was used in portions of eastern Mongolia prior to 2008. It is not currently in use."
                + "Daylight savings time was observed in this time zone."
                + "When DST was not in effect, the time zone was 9 hours ahead of UTC (UTC+9). "
                + "When DST was in effect, the time zone was 10 hours ahead of UTC (UTC+10).") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Choibalsan:.++"));
            }
        },
        new Partition.Membership("Dacca",
            "The Dacca time zone was used in portions of Bangladesh prior to 1971. It is not currently in use."
                + "The time zone was 6 hours ahead of UTC (UTC+6) and did not use daylight savings time.") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Dacca:.++"));
            }
        }, new Partition.Membership("Dushanbe", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Dushanbe:.++"));
            }
        }, new Partition.Membership("East Timor", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|East_Timor:.++"));
            }
        }, new Partition.Membership("Frunze", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Frunze:.++"));
            }
        }, new Partition.Membership("Georgia", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Georgia:.++"));
            }
        }, new Partition.Membership("Gulf", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Gulf:.++"));
            }
        }, new Partition.Membership("Hong Kong", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Hong_Kong:.++"));
            }
        }, new Partition.Membership("Hovd", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Hovd:.++"));
            }
        }, new Partition.Membership("India", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|India:.++"));
            }
        }, new Partition.Membership("Indochina", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Indochina:.++"));
            }
        }, new Partition.Membership("Central Indonesia", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Indonesia_Central:.++"));
            }
        }, new Partition.Membership("Eastern Indonesia", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Indonesia_Eastern:.++"));
            }
        }, new Partition.Membership("Western Indonesia", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Indonesia_Western:.++"));
            }
        }, new Partition.Membership("Iran", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Iran:.++"));
            }
        }, new Partition.Membership("Irkutsk", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Irkutsk:.++"));
            }
        }, new Partition.Membership("Israel", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Israel:.++"));
            }
        }, new Partition.Membership("Japan", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Japan:.++"));
            }
        }, new Partition.Membership("Karachi", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Karachi:.++"));
            }
        }, new Partition.Membership("Kashgar", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Kashgar:.++"));
            }
        }, new Partition.Membership("Eastern Kazakhstan", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Kazakhstan_Eastern:.++"));
            }
        }, new Partition.Membership("Western Kazakhstan", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Kazakhstan_Western:.++"));
            }
        }, new Partition.Membership("Kizilorda", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Kizilorda:.++"));
            }
        }, new Partition.Membership("Korea", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Korea:.++"));
            }
        }, new Partition.Membership("Krasnoyarsk", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Krasnoyarsk:.++"));
            }
        }, new Partition.Membership("Kyrgystan", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Kyrgystan:.++"));
            }
        }, new Partition.Membership("Lanka", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Lanka:.++"));
            }
        }, new Partition.Membership("Long Shu", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Long_Shu:.++"));
            }
        }, new Partition.Membership("Macau", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Macau:.++"));
            }
        }, new Partition.Membership("Magadan", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Magadan:.++"));
            }
        }, new Partition.Membership("Malaya", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Malaya:.++"));
            }
        }, new Partition.Membership("Malaysia", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Malaysia:.++"));
            }
        }, new Partition.Membership("Mongolia", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Mongolia:.++"));
            }
        }, new Partition.Membership("Myanmar", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Myanmar:.++"));
            }
        }, new Partition.Membership("Nepal", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Nepal:.++"));
            }
        }, new Partition.Membership("Novosibirsk", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Novosibirsk:.++"));
            }
        }, new Partition.Membership("Omsk", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Omsk:.++"));
            }
        }, new Partition.Membership("Pakistan", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Pakistan:.++"));
            }
        }, new Partition.Membership("Philippines", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Philippines:.++"));
            }
        }, new Partition.Membership("Qyzylorda", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Qyzylorda:.++"));
            }
        }, new Partition.Membership("Sakhalin", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Sakhalin:.++"));
            }
        }, new Partition.Membership("Samarkand", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Samarkand:.++"));
            }
        }, new Partition.Membership("Shevchenko", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Shevchenko:.++"));
            }
        }, new Partition.Membership("Singapore", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Singapore:.++"));
            }
        }, new Partition.Membership("Sverdlovsk", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Sverdlovsk:.++"));
            }
        }, new Partition.Membership("Taipei", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Taipei:.++"));
            }
        }, new Partition.Membership("Tajikistan", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Tajikistan:.++"));
            }
        }, new Partition.Membership("Tashkent", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Tashkent:.++"));
            }
        }, new Partition.Membership("Tbilisi", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Tbilisi:.++"));
            }
        }, new Partition.Membership("Turkmenistan", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Turkmenistan:.++"));
            }
        }, new Partition.Membership("Uralsk", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Uralsk:.++"));
            }
        }, new Partition.Membership("Urumqi", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Urumqi:.++"));
            }
        }, new Partition.Membership("Uzbekistan", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Uzbekistan:.++"));
            }
        }, new Partition.Membership("Vladivostok", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Vladivostok:.++"));
            }
        }, new Partition.Membership("Yakutsk", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Yakutsk:.++"));
            }
        }, new Partition.Membership("Yekaterinburg", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Yekaterinburg:.++"));
            }
        }, new Partition.Membership("Yerevan", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Yerevan:.++"));
            }
        }, new Partition.Membership("Central Australia", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Australia_Central:.++"));
            }
        }, new Partition.Membership("Central Western Australia", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Australia_CentralWestern:.++"));
            }
        }, new Partition.Membership("Eastern Australia", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Australia_Eastern:.++"));
            }
        }, new Partition.Membership("Western Australia", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Australia_Western:.++"));
            }
        }, new Partition.Membership("Lord Howe Island", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Lord_Howe:.++"));
            }
        }, new Partition.Membership("Central Europe", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Europe_Central:.++"));
            }
        }, new Partition.Membership("Eastern Europe", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Europe_Eastern:.++"));
            }
        }, new Partition.Membership("Western Europe", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Europe_Western:.++"));
            }
        }, new Partition.Membership("Kuybyshev", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Kuybyshev:.++"));
            }
        }, new Partition.Membership("Moscow", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Moscow:.++"));
            }
        }, new Partition.Membership("Turkey", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Turkey:.++"));
            }
        }, new Partition.Membership("Volgograd", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Volgograd:.++"));
            }
        }, new Partition.Membership("Azores", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Azores:.++"));
            }
        }, new Partition.Membership("Cape Verde", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Cape_Verde:.++"));
            }
        }, new Partition.Membership("Falkland Islands", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Falkland:.++"));
            }
        }, new Partition.Membership("Greenwich Mean Time", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|GMT:.++"));
            }
        }, new Partition.Membership("South Georgia", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|South_Georgia:.++"));
            }
        }, new Partition.Membership("Christmas Island", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Christmas:.++"));
            }
        }, new Partition.Membership("Cocos", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Cocos:.++"));
            }
        }, new Partition.Membership("French Southern", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|French_Southern:.++"));
            }
        }, new Partition.Membership("Indian Ocean", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Indian_Ocean:.++"));
            }
        }, new Partition.Membership("Maldives", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Maldives:.++"));
            }
        }, new Partition.Membership("Mauritius", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Mauritius:.++"));
            }
        }, new Partition.Membership("Reunion", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Reunion:.++"));
            }
        }, new Partition.Membership("Seychelles", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Seychelles:.++"));
            }
        }, new Partition.Membership("Chamorro", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Chamorro:.++"));
            }
        }, new Partition.Membership("Chatham", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Chatham:.++"));
            }
        }, new Partition.Membership("Cook Islands", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Cook:.++"));
            }
        }, new Partition.Membership("Easter Island", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Easter:.++"));
            }
        }, new Partition.Membership("Fiji", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Fiji:.++"));
            }
        }, new Partition.Membership("Galapagos", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Galapagos:.++"));
            }
        }, new Partition.Membership("Gambier", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Gambier:.++"));
            }
        }, new Partition.Membership("Gilbert Islands", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Gilbert_Islands:.++"));
            }
        }, new Partition.Membership("Guam", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Guam:.++"));
            }
        }, new Partition.Membership("Hawaii-Aleutian", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Hawaii_Aleutian:.++"));
            }
        }, new Partition.Membership("Kosrae", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Kosrae:.++"));
            }
        }, new Partition.Membership("Kwajalein", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Kwajalein:.++"));
            }
        }, new Partition.Membership("Line Islands", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Line_Islands:.++"));
            }
        }, new Partition.Membership("Marquesas", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Marquesas:.++"));
            }
        }, new Partition.Membership("Marshall Islands", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Marshall_Islands:.++"));
            }
        }, new Partition.Membership("Nauru", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Nauru:.++"));
            }
        }, new Partition.Membership("New Caledonia", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|New_Caledonia:.++"));
            }
        }, new Partition.Membership("New Zealand", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|New_Zealand:.++"));
            }
        }, new Partition.Membership("Niue", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Niue:.++"));
            }
        }, new Partition.Membership("Norfolk Islands", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Norfolk:.++"));
            }
        }, new Partition.Membership("North Mariana Islands", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|North_Mariana:.++"));
            }
        }, new Partition.Membership("Palau", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Palau:.++"));
            }
        }, new Partition.Membership("Papua New Guinea", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Papua_New_Guinea:.++"));
            }
        }, new Partition.Membership("Phoenix Islands", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Phoenix_Islands:.++"));
            }
        }, new Partition.Membership("Pitcairn", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Pitcairn:.++"));
            }
        }, new Partition.Membership("Ponape", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Ponape:.++"));
            }
        }, new Partition.Membership("Samoa", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Samoa:.++"));
            }
        }, new Partition.Membership("Solomon Islands", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Solomon:.++"));
            }
        }, new Partition.Membership("Tahiti", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Tahiti:.++"));
            }
        }, new Partition.Membership("Tokelau", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Tokelau:.++"));
            }
        }, new Partition.Membership("Tonga", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Tonga:.++"));
            }
        }, new Partition.Membership("Truk (Chuuk)", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Truk:.++"));
            }
        }, new Partition.Membership("Tuvalu", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Tuvalu:.++"));
            }
        }, new Partition.Membership("Vanuatu", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Vanuatu:.++"));
            }
        }, new Partition.Membership("Wake Island", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Wake:.++"));
            }
        }, new Partition.Membership("Wallis and Futuna", "Description TBD") {
            public boolean isMember(DataRow p) {
                String pp = p.getPrettyPath();
                return (pp != null && pp.matches("0-names\\|metazone\\|Wallis:.++"));
            }
        }, };

    @Override
    Partition.Membership[] memberships() {
        return memberships;
    }

    @Override
    Comparator<DataRow> createComparator() {
        return comparator();
    }

    public static Comparator<DataRow> comparator() {
        final int ourKey = SortMode.SortKeyType.SORTKEY_CALENDAR.ordinal();
        final Comparator<DataRow> codeComparator = CodeSortMode.comparator();
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

                return codeComparator.compare(p1, p2); // fall back to code

            }
        };
    }

    @Override
    String getDisplayName() {
        return "Type";
    }

}