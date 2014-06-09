package org.unicode.cldr.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

/**
 * Small helper class to convert an Array of Strings (key, value, key, value,...) into an immutable Map
 * @author ribnitz
 *
 */
public class StringArrayToMap {
    /**
     * Convert an Array of String (key,value, key, value, ...) into an immutable Map
     * @param arr  the array of Strings to convert
     * @return an Immutable Map of the Array
     * @throws IllegalArgumentException If the array is null
     */
    public static Map<String,String> from(String[] arr) {
       // sanity checks
        if (arr==null) {
            throw new IllegalArgumentException("Unable to handle null Array.");
        }
        if (arr.length % 2 !=0) {
            throw new IllegalArgumentException("Please provide an array with an even number of entries, (key, value,...)");
        }
        // Empty array -> empty map
        if (arr.length==0) {
            return Collections.emptyMap();
        }
        // actually do the work
        Map<String,String> result=new HashMap<>(arr.length/2);
        for (int i=0;i<arr.length/2;i+=2) {
            result.put(arr[i],arr[i+1]);
        }
        return from(result);
    }
    /**
     * Make a Map of Strings immutable
     * @param aMap
     * @return
     */
    public static Map<String,String> from(Map<String,String> aMap) {
        if (aMap==null) {
            throw new IllegalArgumentException("Please call with non-null map");
        }
        if (aMap.isEmpty()) {
            return Collections.emptyMap();
        }

        return ImmutableMap.copyOf(aMap);
    }
}