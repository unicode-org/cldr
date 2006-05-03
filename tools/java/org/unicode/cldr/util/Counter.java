/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Date$
* $Revision$
*
*******************************************************************************
*/

package org.unicode.cldr.util;


import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class Counter {
    Map map = new HashMap();

    static public final class RWInteger implements Comparable {
        static int uniqueCount;
        public int value;
        private int forceUnique = uniqueCount++;

        // public RWInteger() {
          //  forceUnique

        public int compareTo(Object other) {
            RWInteger that = (RWInteger) other;
            if (that.value < value) return -1;
            else if (that.value > value) return 1;
            else if (that.forceUnique < forceUnique) return -1;
            else if (that.forceUnique > forceUnique) return 1;
            return 0;
        }
        public String toString() {
            return String.valueOf(value);
        }
    }

    public void add(Object obj, int countValue) {
        RWInteger count = (RWInteger)map.get(obj);
        if (count == null) map.put(obj, count = new RWInteger());
        count.value += countValue;
    }
    
    public int getCount(Object obj) {
        RWInteger count = (RWInteger) map.get(obj);
        return count == null ? 0 : count.value;
    }
    
    public void clear() {
        map.clear();
    }
    
    public int getTotal() {
        int count = 0;
        for (Iterator it = map.keySet().iterator(); it.hasNext();) {
            count += ((RWInteger) map.get(it.next())).value;
        }
        return count;
    }
    
    public int getItemCount() {
        return map.size();
    }

    public Set getKeysetSortedByCount(boolean ascending) {
        Map count_key = new TreeMap();
        int counter = 0; // original order
        for (Iterator it =  map.keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            long count = getCount(key);
            if (!ascending) count = Integer.MAX_VALUE-count;
            count_key.put(new Long((count<<32) + (counter++)), key);
        }
        Set result = new LinkedHashSet();
        for (Iterator it = count_key.keySet().iterator(); it.hasNext();) {
            Object count = it.next();
            Object key = count_key.get(count);
            result.add(key);
        }
        return result;
    }
    
    public Set getKeysetSortedByKey() {
        return new TreeSet(map.keySet());
    }

    public Set getKeysetSortedByKey(Comparator comparator) {
    	Set s = new TreeSet(comparator);
    	s.addAll(map.keySet());
        return s;
    }


    public Map getKeyToKey() {
        Map result = new HashMap();
        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            Object key = it.next();
            result.put(key, key);
        }
        return result;
    }

    public Set keySet() {
        return map.keySet();
    }
    
    public Map getMap() {
        return Collections.unmodifiableMap(map);
    }

    public int size() {
        return map.size();
    }
    
    public String toString() {
    	return map.toString();
    }
}