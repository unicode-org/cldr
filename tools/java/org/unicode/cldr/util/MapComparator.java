/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Mark Davis
**********************************************************************
*/
package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Collection;

public class MapComparator implements Comparator {
    Map ordering = new TreeMap(); // maps from name to rank
    List rankToName = new ArrayList();

    MapComparator(){}
    
    MapComparator(Comparable[] data) {
    	add(data);
    }
    MapComparator(Collection c) {
    	add(c);
    }
    public MapComparator add(Object newObject) {
        Object already = ordering.get(newObject);
        if (already == null) {
            ordering.put(newObject, new Integer(rankToName.size()));
            rankToName.add(newObject);
        }
        return this;
    }
    public MapComparator add(Collection c) {
        for (Iterator it = c.iterator(); it.hasNext();) {
            add(it.next());
        }
        return this;
    }
    public MapComparator add(Comparable[] data) {
        for (int i = 0; i < data.length; ++i) {
            add(data[i]);
        }
        return this;
    }
    public int compare(Object a, Object b) {
        Comparable aa = (Comparable) ordering.get(a);
        Comparable bb = (Comparable) ordering.get(b);
        if (aa == null || bb == null) return ((Comparable)a).compareTo(b);
        return aa.compareTo(bb);
    }
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        boolean isFirst = true;
        for (Iterator it = rankToName.iterator(); it.hasNext();) {
            Object key = it.next();
            if (isFirst) isFirst = false;
            else buffer.append(" ");
            buffer.append("<").append(key).append(">");
        }
        return buffer.toString();
    }
}