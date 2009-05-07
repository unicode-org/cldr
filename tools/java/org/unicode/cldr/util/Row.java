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
public class Row<C0 extends Comparable, C1 extends Comparable, C2 extends Comparable, C3 extends Comparable, C4 extends Comparable> implements java.lang.Comparable, Cloneable, Freezable{
  protected Comparable[] items;
  protected boolean frozen;
  
  /**
   * Convenience Methods
   */
  public static <C0 extends Comparable, C1 extends Comparable> R2<C0,C1> make(C0 p0, C1 p1) {
    return new R2<C0,C1>(p0,p1);
  }
  public static <C0 extends Comparable, C1 extends Comparable, C2 extends Comparable> R3<C0,C1,C2> make(C0 p0, C1 p1, C2 p2) {
    return new R3<C0,C1,C2>(p0,p1,p2);
  }
  public static <C0 extends Comparable, C1 extends Comparable, C2 extends Comparable, C3 extends Comparable> R4<C0,C1,C2,C3> make(C0 p0, C1 p1, C2 p2, C3 p3) {
    return new R4<C0,C1,C2,C3>(p0,p1,p2,p3);
  }
  public static <C0 extends Comparable, C1 extends Comparable, C2 extends Comparable, C3 extends Comparable, C4 extends Comparable> R5<C0,C1,C2,C3,C4> make(C0 p0, C1 p1, C2 p2, C3 p3, C4 p4) {
    return new R5<C0,C1,C2,C3,C4>(p0,p1,p2,p3,p4);
  }
  
  public static class R2<C0 extends Comparable, C1 extends Comparable> extends Row<C0, C1, C1, C1, C1> {
    public R2(C0 a, C1 b)  {
      items = new Comparable[] {a, b};
    }
  }
  public static class R3<C0 extends Comparable, C1 extends Comparable, C2 extends Comparable> extends Row<C0, C1, C2, C2, C2> {
    public R3(C0 a, C1 b, C2 c)  {
      items = new Comparable[] {a, b, c};
    }
  }
  public static class R4<C0 extends Comparable, C1 extends Comparable, C2 extends Comparable, C3 extends Comparable> extends Row<C0, C1, C2, C3, C3> {
    public R4(C0 a, C1 b, C2 c, C3 d)  {
      items = new Comparable[] {a, b, c, d};
    }
  }
  public static class R5<C0 extends Comparable, C1 extends Comparable, C2 extends Comparable, C3 extends Comparable, C4 extends Comparable> extends Row<C0, C1, C2, C3, C4> {
    public R5(C0 a, C1 b, C2 c, C3 d, C4 e)  {
      items = new Comparable[] {a, b, c, d, e};
    }
  }

  public Row set0(C0 item) {
    return (Row) set(0, item);
  }
  public C0 get0() {
    return (C0) items[0];
  }
  public Row set1(C1 item) {
    return (Row) set(1, item);
  }
  public C1 get1() {
    return (C1) items[1];
  }
  public Row set2(C2 item) {
    return (Row) set(2, item);
  }
  public C2 get2() {
    return (C2) items[2];
  }
  public Row set3(C3 item) {
    return (Row) set(3, item);
  }
  public C3 get3() {
    return (C3) items[3];
  }
  public Row set4(C4 item) {
    return (Row) set(4, item);
  }
  public C4 get4() {
    return (C4) items[4];
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

