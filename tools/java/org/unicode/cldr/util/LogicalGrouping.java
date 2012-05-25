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

    /**
     * Return the set of paths that are in the same logical set as the given path
     * @param path - the distinguishing xpath 
     */
    public static Set<String> getPaths(String path) {
        String[] metazone_string_types = { "generic", "standard", "daylight" };

        Set<String> result = new TreeSet<String>();
        if (path == null) return result;
        result.add(path);

        if (path.indexOf("/metazone") > 0) {
            XPathParts parts = new XPathParts();
            parts.set(path);
            String metazoneName = parts.getAttributeValue(3, "type");
            if ( metazonesDSTList.contains(metazoneName)) {
                for ( String str : metazone_string_types ) {
                    result.add(path.substring(0,path.lastIndexOf('/')+1)+str);
                }
            }
        }
        
        return result;
    }
}
