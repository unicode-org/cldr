package org.unicode.cldr.util;

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.Set;

public class LogicalGrouping {
    
    public static final String[] metazonesUsingDST = { 
        "Acre", "Africa_Western", "Aktyubinsk", "Alaska", "Alaska_Hawaii", "Almaty", "Amazon",
        "America_Central", "America_Eastern", "America_Mountain", "America_Pacific", "Anadyr", 
        "Aqtau", "Aqtobe", "Arabian", "Argentina", "Argentina_Western", "Armenia", "Ashkhabad",
        "Atlantic", "Australia_Central", "Australia_CentralWestern", "Australia_Eastern", "Australia_Western",
        "Azerbaijan", "Azores", "Baku", "Bangladesh", "Bering", "Borneo", "Brasilia", "Cape_Verde",
        "Chatham", "Chile", "China", "Choibalsan", "Colombia", "Cook", "Cuba", "Dushanbe", "Easter", 
        "Europe_Central", "Europe_Eastern", "Europe_Western", "Falkland", "Fiji", "Frunze", "Georgia", 
        "Greenland_Central", "Greenland_Eastern", "Greenland_Western", "Hawaii_Aleutian", "Hong_Kong", "Hovd",
        "Iran", "Irkutsk", "Israel", "Japan", "Kamchatka", "Kizilorda", "Korea", "Krasnoyarsk", "Kuybyshev",
        "Lord_Howe", "Macau", "Magadan", "Mauritius", "Mongolia", "Moscow", "New_Caledonia", "New_Zealand",
        "Newfoundland", "Noronha", "Novosibirsk", "Omsk", "Pakistan", "Paraguay", "Peru", "Philippines",
        "Pierre_Miquelon", "Qyzylorda", "Sakhalin", "Samara", "Samarkand", "Shevchenko", "Sverdlovsk",
        "Taipei", "Tashkent", "Tbilisi", "Tonga", "Turkey", "Turkmenistan", "Uralsk", "Uruguay", "Uzbekistan",
        "Vanuatu", "Vladivostok", "Volgograd", "Yakutsk", "Yekaterinburg", "Yerevan", "Yukon" };
    public static final List<String> metazonesDSTList = Arrays.asList(metazonesUsingDST);
    
    public static final String[] days = { "sun", "mon", "tue", "wed", "thu", "fri", "sat" };
    public static final List<String> daysList = Arrays.asList(days);
    
    public static final String[] calendarsWith13Months = { "coptic", "ethiopic", "hebrew" };
    public static final List<String> calendarsWith13MonthsList = Arrays.asList(calendarsWith13Months);
   

    /**
     * Return the set of paths that are in the same logical set as the given path
     * @param path - the distinguishing xpath 
     */
    public static Set<String> getPaths(String path) {
        String[] metazone_string_types = { "generic", "standard", "daylight" };

        Set<String> result = new TreeSet<String>();
        if (path == null) return result;
        result.add(path);

        XPathParts parts = new XPathParts();

        if (path.indexOf("/metazone") > 0) {
            parts.set(path);
            String metazoneName = parts.getAttributeValue(3, "type");
            if ( metazonesDSTList.contains(metazoneName)) {
                for ( String str : metazone_string_types ) {
                    result.add(path.substring(0,path.lastIndexOf('/')+1)+str);
                }
            }
        } else if ( path.indexOf("/days") > 0 ) {
            parts.set(path);
            String dayName = parts.size() > 7 ? parts.getAttributeValue(7, "type") : null;
            if ( dayName != null && daysList.contains(dayName)) { // This is just a quick check to make sure the path is good.
                for ( String str : days ) {
                    parts.setAttribute("day", "type", str);
                    result.add(parts.toString());
                }
            }
        } else if ( path.indexOf("/quarters") > 0 ) {
            parts.set(path);
            String quarterName = parts.size() > 7 ? parts.getAttributeValue(7, "type") : null;
            Integer quarter = quarterName == null ? 0 : Integer.valueOf(quarterName);
            if ( quarter > 0 && quarter <= 4) { // This is just a quick check to make sure the path is good.
                for ( Integer i = 1 ; i <= 4 ; i++ ) {
                    parts.setAttribute("quarter", "type", i.toString());
                    result.add(parts.toString());
                }
            }
        } else if ( path.indexOf("/months") > 0 ) {
            parts.set(path);
            String calType = parts.size() > 3 ? parts.getAttributeValue(3, "type") : null;
            String monthName = parts.size() > 7 ? parts.getAttributeValue(7, "type") : null;
            Integer month = monthName == null ? 0 : Integer.valueOf(monthName);
            int calendarMonthMax = calendarsWith13MonthsList.contains(calType) ? 13 : 12;
            if ( month > 0 && month <= calendarMonthMax) { // This is just a quick check to make sure the path is good.
                    for ( Integer i = 1 ; i <= calendarMonthMax ; i++ ) {
                        parts.setAttribute("month", "type", i.toString());
                        result.add(parts.toString());
                    }
                    if ("hebrew".equals(calType)) { // Add extra hebrew calendar leap month
                        parts.setAttribute("month", "type", Integer.toString(7));
                        parts.setAttribute("month", "yeartype", "leap");
                        result.add(parts.toString());
                    }
        }
    }
        
        return result;
    }
}
