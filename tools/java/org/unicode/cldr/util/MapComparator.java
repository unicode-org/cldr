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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Freezable;
import com.ibm.icu.util.ULocale;

public class MapComparator<K> implements Comparator<K>, Freezable {
  private RuleBasedCollator uca = (RuleBasedCollator) Collator.getInstance(ULocale.ROOT);
  {
    uca.setNumericCollation(true);
  }
  private Map<K,Integer> ordering = new TreeMap<K,Integer>(); // maps from name to rank
  private List<K> rankToName = new ArrayList<K>();
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
  public MapComparator<K> setErrorOnMissing(boolean errorOnMissing) {
    if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
    this.errorOnMissing = errorOnMissing;
    return this;
  }

  /**
   * @return Returns the rankToName.
   */
  public List<K> getOrder() {
    return Collections.unmodifiableList(rankToName);
  }

  public MapComparator(){}

  public MapComparator(K[] data) {
    add(data);
  }
  public MapComparator(Collection<K> c) {
    add(c);
  }
  public MapComparator<K> add(K newObject) {
    Integer already = ordering.get(newObject);
    if (already == null) {
      if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
      ordering.put(newObject, new Integer(rankToName.size()));
      rankToName.add(newObject);
    }
    return this;
  }
  public MapComparator<K> add(Collection<K> c) {
    for (Iterator<K> it = c.iterator(); it.hasNext();) {
      add(it.next());
    }
    return this;
  }
  public MapComparator<K> add(K[] data) {
    for (int i = 0; i < data.length; ++i) {
      add(data[i]);
    }
    return this;
  }

  private static final UnicodeSet numbers = new UnicodeSet("[\\-0-9.]");

  public int compare(K a, K b) {
    if (false && (a.equals("lines") || b.equals("lines"))) {
      System.out.println();
    }
    Integer aa = ordering.get(a);
    Integer bb = ordering.get(b);
    if (aa != null && bb != null) {
      return aa.compareTo(bb);
    }
    if (errorOnMissing) throw new IllegalArgumentException("Missing Map Comparator value(s): " 
            + (aa == null ? a : "") + "\t"
            + (bb == null ? b : "")
    );
    // must handle halfway case, otherwise we are not transitive!!!
    if (aa == null && bb != null) {
      return 1;
    }
    if (aa != null && bb == null) {
      return -1;
    }
    // do numeric
    // first we do a quick check, then parse.
    // for transitivity, we have to check both.
    boolean anumeric = numbers.containsAll((String)a);
    double an = Double.NaN, bn = Double.NaN;
    if (anumeric) {
      try {
        an = Double.parseDouble((String)a);
      } catch (NumberFormatException e) {
        anumeric = false;
      }
    }
    boolean bnumeric = numbers.containsAll((String)b);
    if (bnumeric) {
      try {
        bn = Double.parseDouble((String)b);
      } catch (NumberFormatException e) {
        bnumeric = false;
      }
    }
    if (anumeric && bnumeric) {
      if (an < bn) return -1;
      if (an > bn) return 1;
      return 0;
    }
    // must handle halfway case, otherwise we are not transitive!!!
    if (!anumeric && bnumeric) return 1;
    if (anumeric && !bnumeric) return -1;
    
    if (a instanceof CharSequence) {
      if (b instanceof CharSequence) {
        int result = uca.compare(a.toString(), b.toString());
        if (result != 0) {
          return result;
        }
      } else {
        return 1; // handle for transitivity
      }
    } else {
      return -1; // handle for transitivity
    }

    // do fallback
    return ((Comparable)a).compareTo(b);
  }
  
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    boolean isFirst = true;
    for (Iterator<K> it = rankToName.iterator(); it.hasNext();) {
      K key = it.next();
      if (isFirst) isFirst = false;
      else buffer.append(" ");
      buffer.append("<").append(key).append(">");
    }
    return buffer.toString();
  }
  /* (non-Javadoc)
   * @see com.ibm.icu.dev.test.util.Freezeble
   */
  public boolean isFrozen() {
    return locked;
  }
  /* (non-Javadoc)
   * @see com.ibm.icu.dev.test.util.Freezeble
   */
  public Object freeze() {
    locked = true;	
    return this;
  }
  /* (non-Javadoc)
   * @see com.ibm.icu.dev.test.util.Freezeble
   */
  @SuppressWarnings("unchecked")
  public Object cloneAsThawed() {
    try {
      MapComparator<K> result = (MapComparator<K>) super.clone();
      result.locked = false;
      result.ordering = (Map<K,Integer>)((TreeMap<K,Integer>)ordering).clone();
      result.rankToName = (List<K>)((ArrayList<K>)rankToName).clone();
      return result;
    } catch (CloneNotSupportedException e) {
      throw new InternalError("should never happen");
    }		
  }
}