/*
 **********************************************************************
 * Copyright (c) 2002-2004, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */

package org.unicode.cldr.util;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.util.Freezable;

public final class Pair<T extends Comparable<T>, U extends Comparable<U>> implements java.lang.Comparable<Pair<T, U>>,
    Cloneable, Freezable<Object> {

    private T first;
    private U second;
    private boolean frozen;

    public static <T extends Comparable<T>, U extends Comparable<U>> Pair<T, U> of(T arg0, U arg1) {
        return new Pair<T, U>(arg0, arg1);
    }

    public static <T extends Comparable<T>, U extends Comparable<U>> Pair<T, U> ofFrozen(T arg0, U arg1) {
        return of(arg0, arg1).freeze();
    }

    public Pair<T, U> setFirst(T first) {
        if (frozen) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
        this.first = first;
        return this;
    }

    public T getFirst() {
        return first;
    }

    public Pair<T, U> setSecond(U second) {
        if (frozen) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
        this.second = second;
        return this;
    }

    public U getSecond() {
        return second;
    }

    public Pair<T, U> set(Pair<T, U> name) {
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
        return Utility.checkHash(first) * 37 + Utility.checkHash(second);
    }

    public boolean equals(Object other) {
        try {
            Pair<?, ?> that = (Pair<?, ?>) other;
            return Utility.objectEquals(first, that.first) && Utility.objectEquals(second, that.second);
        } catch (Exception e) {
            return false;
        }
    }

    public int compareTo(Pair<T, U> that) {
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

    public Pair<T, U> freeze() {
        frozen = true;
        return this;
    }

    public Object cloneAsThawed() {
        try {
            Pair<?, ?> result = (Pair<?, ?>) super.clone();
            result.frozen = false;
            return result;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}