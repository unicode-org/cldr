/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Mark Davis
**********************************************************************
*/

package org.unicode.cldr.util;

public final class Pair<T extends Comparable, U extends Comparable> implements java.lang.Comparable, Cloneable {

  public T first;
  public U second;

  public Pair (T first, U second) {
    this.first = first;
    this.second = second;
  }

  public int hashCode() {
    return first.hashCode() * 37 + second.hashCode();
  }

  public boolean equals(Object other) {
    try {
      Pair that = (Pair)other;
      return first.equals(that.first) && second.equals(that.second);
    } catch (Exception e) {
      return false;
    }
  }

    public int compareTo(Object other) {
        Pair that = (Pair)other;
        int trial = first.compareTo(that.first);
        if (trial != 0) return trial;
        return second.compareTo(that.second);
    }
    
    public Object clone() {
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
}