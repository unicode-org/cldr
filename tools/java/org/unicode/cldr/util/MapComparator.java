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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Collection;

import javax.naming.OperationNotSupportedException;

import com.ibm.icu.text.UnicodeSet;

public class MapComparator implements Comparator, Lockable {
    private Map ordering = new TreeMap(); // maps from name to rank
    private List rankToName = new ArrayList();
    private boolean errorOnMissing = true;
    private boolean locked = false;
    
	/**
	 * @return Returns the errorOnMissing.
	 */
	public boolean isErrorOnMissing() {
		return errorOnMissing;
	}
	/**
	 * @param errorOnMissing The errorOnMissing to set.
	 */
	public MapComparator setErrorOnMissing(boolean errorOnMissing) {
		this.errorOnMissing = errorOnMissing;
		return this;
	}
	
	/**
	 * @return Returns the rankToName.
	 */
	public List getOrder() {
		return Collections.unmodifiableList(rankToName);
	}
	
    public MapComparator(){}
    
    public MapComparator(Comparable[] data) {
    	add(data);
    }
    public MapComparator(Collection c) {
    	add(c);
    }
    public MapComparator add(Object newObject) {
        Object already = ordering.get(newObject);
        if (already == null) {
        	if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
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
    
    private static final UnicodeSet numbers = new UnicodeSet("[0-9]");
    
    public int compare(Object a, Object b) {
		if (false && (a.equals("lines") || b.equals("lines"))) {
			System.out.println();
		}
        Comparable aa = (Comparable) ordering.get(a);
        Comparable bb = (Comparable) ordering.get(b);
        if (aa != null && bb != null) return aa.compareTo(bb);
        if (errorOnMissing) throw new IllegalArgumentException("Missing value(s): " 
        		+ (aa == null ? a : "") + "\t"
				+ (bb == null ? b : "")
		);
        // must handle halfway case, otherwise we are not transitive!!!
        if (aa == null && bb != null) return 1;
        if (aa != null && bb == null) return -1;
        // do numeric
        boolean anumeric = numbers.containsAll((String)a);
        boolean bnumeric = numbers.containsAll((String)b);
        if (anumeric && bnumeric) {
        	long an = Long.parseLong((String)a);
        	long bn = Long.parseLong((String)b);
        	if (an < bn) return -1;
        	if (an > bn) return 1;
        	return 0;
        }
        // must handle halfway case, otherwise we are not transitive!!!
        if (!anumeric && bnumeric) return 1;
        if (anumeric && !bnumeric) return -1;
        
        // do fallback
        return ((Comparable)a).compareTo(b);
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
	/* (non-Javadoc)
	 * @see org.unicode.cldr.util.Lockable#isLocked()
	 */
	public boolean isLocked() {
		return locked;
	}
	/* (non-Javadoc)
	 * @see org.unicode.cldr.util.Lockable#lock()
	 */
	public void lock() {
		locked = true;	
	}
	/* (non-Javadoc)
	 * @see org.unicode.cldr.util.Lockable#clone()
	 */
	public Object clone() {
    	try {
    		MapComparator result = (MapComparator) super.clone();
			result.locked = false;
			result.ordering = (Map)((TreeMap)ordering).clone();
			result.rankToName = (List)((TreeMap)rankToName).clone();
			return result;
		} catch (CloneNotSupportedException e) {
			throw new InternalError("should never happen");
		}		
	}
}