/*
**********************************************************************
* Copyright (c) 2002-2012, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Mark Davis
**********************************************************************
*/package org.unicode.cldr.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A collection that is like a sorted set, except that it allows
 * multiple elements that compare as equal
 * @author medavis
 */
// TODO replace use of Set with a collection that takes an Equator
public class SortedBag implements Collection {
    /**
     * A map of sets, where each corresponds to one sorted element.
     * The sets are never empty
     */
    private Map m;
    private int size;

    public SortedBag(Comparator c) {
        m = new TreeMap(c);
    }

    @Override
    public boolean add(Object s) {
        Set o = (Set) m.get(s);
        if (o == null) {
            o = new HashSet(1);
            m.put(s, o);
        }
        boolean result = o.add(s);
        if (result) size++;
        return result;
    }

    @Override
    public Iterator iterator() {
        return new MyIterator();
    }

    static Iterator EMPTY_ITERATOR = new HashSet().iterator();

    private class MyIterator implements Iterator {
        private Iterator mapIterator = m.keySet().iterator();
        private Iterator setIterator = null;

        private MyIterator() {
            mapIterator = m.keySet().iterator();
            setIterator = getSetIterator();
        }

        private Iterator getSetIterator() {
            if (!mapIterator.hasNext()) return EMPTY_ITERATOR;
            return ((Set) m.get(mapIterator.next())).iterator();
        }

        @Override
        public boolean hasNext() {
            return setIterator.hasNext() || mapIterator.hasNext();
        }

        @Override
        public Object next() {
            if (!setIterator.hasNext()) {
                setIterator = getSetIterator();
            }
            return setIterator.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     *
     */
    @Override
    public void clear() {
        m.clear();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        Set set = (Set) m.get(o);
        if (set == null) return false;
        return set.contains(o);
    }

    @Override
    public Object[] toArray() {
        return toArray(new Object[size]);
    }

    @Override
    public Object[] toArray(Object[] a) {
        int count = 0;
        for (Iterator it = iterator(); it.hasNext();) {
            a[count++] = it.next();
        }
        return a;
    }

    /* (non-Javadoc)
     * @see java.util.Collection#remove(java.lang.Object)
     */
    @Override
    public boolean remove(Object o) {
        Set set = (Set) m.get(o);
        if (set == null) return false;
        if (!set.remove(o)) return false;
        if (set.size() == 0) m.remove(o);
        size--;
        return true;
    }

    @Override
    public boolean containsAll(Collection c) {
        for (Iterator it = c.iterator(); it.hasNext();) {
            if (!contains(it.next())) return false;
        }
        return true;
    }

    @Override
    public boolean addAll(Collection c) {
        boolean result = false;
        for (Iterator it = c.iterator(); it.hasNext();) {
            if (add(it.next())) result = true;
        }
        return result;
    }

    @Override
    public boolean removeAll(Collection c) {
        boolean result = false;
        for (Iterator it = c.iterator(); it.hasNext();) {
            if (remove(it.next())) result = true;
        }
        return result;
    }

    @Override
    public boolean retainAll(Collection c) {
        // WARNING: this may not work if the comparator does not distinguish
        // all items that are equals().
        Set stuffToRemove = new HashSet(); // have to do this since iterator may not allow removal!
        for (Iterator it = iterator(); it.hasNext();) {
            Object item = it.next();
            if (!c.contains(item)) stuffToRemove.add(item);
        }
        return removeAll(stuffToRemove);
    }
}
