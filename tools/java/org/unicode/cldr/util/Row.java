/*
 **********************************************************************
 * Copyright (c) 2002-2004, Google, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;

import com.ibm.icu.util.Freezable;


@SuppressWarnings("unchecked")
public abstract class Row<C0 extends Comparable, C1 extends Comparable, C2 extends Comparable, C3 extends Comparable, C4 extends Comparable> implements java.lang.Comparable, Cloneable, Freezable{
  protected Comparable[] items;
  protected boolean frozen;

  public Row(C0 a)  {
    items = new Comparable[] {a};
  }
  public Row(C0 a, C1 b)  {
      items = new Comparable[] {a, b};
  }
  public Row(C0 a, C1 b, C2 c)  {
    items = new Comparable[] {a, b, c};
  }
  public Row(C0 a, C1 b, C2 c, C3 d)  {
    items = new Comparable[] {a, b, c, d};
  }

  public Row set0(C0 item) {
    return (Row) set(0, item);
  }
  public C0 get0() {
    return (C0) items[0];
  }
  public Row set1(C1 item) {
    return (Row) set(0, item);
  }
  public C1 get1() {
    return (C1) items[0];
  }
  public Row set2(C2 item) {
    return (Row) set(0, item);
  }
  public C0 get2() {
    return (C0) items[2];
  }
  public Row set3(C3 item) {
    return (Row) set(0, item);
  }
  public C3 get3() {
    return (C3) items[2];
  }

  protected Row set(int i, Comparable item) {
    if (frozen) {
      throw new UnsupportedOperationException("Attempt to modify frozen object");
    }
    items[i] = item;
    return this;
  }

  public int hashCode() {
    int sum = items.length;
    for (Comparable item : items) {
      sum = sum*37 + Utility.checkHash(item);
    }
    return sum;
  }

  public boolean equals(Object other) {
    try {
      Row that = (Row)other;
      if (items.length != that.items.length) {
        return false;
      }
      int i = 0;
      for (Comparable item : items) {
        if (!Utility.checkEquals(item, that.items[i++])) {
          return false;
        }
      }
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public int compareTo(Object other) {
    int result;
    Row that = (Row)other;
    result = items.length - that.items.length;
    if (result != 0) {
      return result;
    }
    int i = 0;
    for (Comparable item : items) {
      result = Utility.checkCompare(item, that.items[i++]);
      if (result != 0) {
        return result;
      }
    }
    return 0;
  }

  public String toString() {
    StringBuilder result = new StringBuilder("[");
    boolean first = true;
    for (Comparable item : items) {
      if (first) {
        first = false;
      } else {
        result.append(", ");
      }
      result.append(item);
    }
    return result.append("]").toString();
  }

  public boolean isFrozen() {
    return frozen;
  }

  public Object freeze() {
    frozen = true;
    return this;
  }

  public Object clone() {
    if (frozen) return this;
    try {
      Row result = (Row) super.clone();
      items = items.clone();
      return result;
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }

  public Object cloneAsThawed() {
    try {
      Row result = (Row) super.clone();
      items = items.clone();
      result.frozen = false;
      return result;
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }
}

