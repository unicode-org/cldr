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
  
  public static <T extends Comparable, U extends Comparable> Pair<T,U> of(T arg0, U arg1) {
    return new Pair<T,U>(arg0, arg1);
  }
  
  public static <T extends Comparable, U extends Comparable> Pair<T,U> ofFrozen(T arg0, U arg1) {
    return of(arg0, arg1).freeze();
  }
  
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
  
  public Pair set(Pair<T, U> name) {
    setFirst(name.getFirst());
    setSecond(name.getSecond());
    return this;
  }
  
  public Pair(T first, U second) {
    this.first = first;
    this.second = second;
  }
  
  public Pair() {
  }
  
  public int hashCode() {
    return CldrUtility.checkHash(first) * 37 + CldrUtility.checkHash(second);
  }
  
  public boolean equals(Object other) {
    try {
      Pair that = (Pair)other;
      return CldrUtility.checkEquals(first, that.first) && CldrUtility.checkEquals(second, that.second);
    } catch (Exception e) {
      return false;
    }
  }
  
  public int compareTo(Object other) {
    Pair that = (Pair)other;
    int trial = CldrUtility.checkCompare(first, that.first);
    if (trial != 0) return trial;
    return CldrUtility.checkCompare(second, that.second);
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
  
  public Pair<T,U> freeze() {
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