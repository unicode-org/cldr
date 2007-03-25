/*
 **********************************************************************
 * Copyright (c) 2002-2004, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */

package org.unicode.cldr.util;

import com.ibm.icu.util.Freezable;

public final class Pair<T extends Comparable, U extends Comparable> implements java.lang.Comparable, Cloneable, Freezable {
  
  private T first;
  private U second;
  private boolean frozen;
  
  public Pair setFirst(T first) {
    if (frozen) {
      throw new UnsupportedOperationException("Attempt to modify frozen object");
    }
    this.first = first;
    return this;
  }
  
  public T getFirst() {
    return first;
  }
  
  public Pair setSecond(U second) {
    if (frozen) {
      throw new UnsupportedOperationException("Attempt to modify frozen object");
    }
    this.second = second;
    return this;
  }
  
  public U getSecond() {
    return second;
  }
  
  public Pair (T first, U second) {
    this.first = first;
    this.second = second;
  }
  
  public int hashCode() {
    return Utility.checkHash(first) * 37 + Utility.checkHash(second);
  }
  
  public boolean equals(Object other) {
    try {
      Pair that = (Pair)other;
      return Utility.checkEquals(first, that.first) && Utility.checkEquals(second, that.second);
    } catch (Exception e) {
      return false;
    }
  }
  
  public int compareTo(Object other) {
    Pair that = (Pair)other;
    int trial = Utility.checkCompare(first, that.first);
    if (trial != 0) return trial;
    return Utility.checkCompare(second, that.second);
  }
  
  public Object clone() {
    if (frozen) return this;
    try {
      return super.clone();
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }
  
  public String toString() {
    return '(' + (first == null ? "null" : first.toString())
    + ',' + (second == null ? "null" : second.toString()) + ')';
  }
  
  public boolean isFrozen() {
    return frozen;
  }
  
  public Object freeze() {
    frozen = true;
    return this;
  }
  
  public Object cloneAsThawed() {
    try {
      Pair result = (Pair) super.clone();
      result.frozen = false;
      return result;
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }
  
}