/**
 *******************************************************************************
 * Copyright (C) 2012 International Business Machines Corporation and          *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package org.unicode.cldr.util;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

/**
 * Debugging utility.
 *
 * A StackTracker tracks which stack frame various objects were created from.
 * For example, call add() and remove() alongside some cache, and then StackTracker's toString() will
 * print out the stack frame of all adds() not balanced by remove().
 *
 * Objects must be Comparable.
 *
 * Example use is in the main() at the bottom. Outputs:
 *
 * "{StackTracker:
 * Held Obj #1/2: the
 * org.unicode.cldr.util.StackTracker.currentStack(StackTracker.java:92)
 * org.unicode.cldr.util.StackTracker.add(StackTracker.java:34)
 * org.unicode.cldr.util.StackTracker.main(StackTracker.java:118)
 * ...}"
 *
 * @author srl
 */
@CLDRTool(alias = "test.stacktracker", description = "Test for StackTracker", hidden = "test")
public class StackTracker implements Iterable<Object> {
    private Hashtable<Object, String> stacks = new Hashtable<Object, String>();

    /**
     * Add object (i.e. added to cache)
     *
     * @param o
     */
    public void add(Object o) {
        String stack = currentStack();
        stacks.put(o, stack);
    }

    /**
     * remove obj (i.e. removed from cache)
     *
     * @param o
     */
    public void remove(Object o) {
        stacks.remove(o);
    }

    /**
     * internal - convert a stack to string
     *
     * @param stackTrace
     * @param skip
     *            start at this index (skip the top stuff)
     * @return
     */
    public static String stackToString(StackTraceElement[] stackTrace, int skip) {
        StringBuffer sb = new StringBuffer();
        for (int i = skip; i < stackTrace.length; i++) {
            sb.append(stackTrace[i].toString() + "\n");
        }
        return sb.toString();
    }

    /**
     * Get this tracker as a string. Prints any leaked objects, and the stack frame of where they were constructed.
     */
    @Override
    public String toString() {
        if (stacks.isEmpty()) {
            return "{StackTracker: empty}";
        }
        StringBuffer sb = new StringBuffer();

        sb.append("{StackTracker:\n");
        int n = 0;
        for (Map.Entry<Object, String> e : stacks.entrySet()) {
            sb.append("Held Obj #" + (++n) + "/" + stacks.size() + ": " + e.getKey() + "\n");
            sb.append(e.getValue() + "\n");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Purges all held objects.
     */
    public void clear() {
        stacks.clear();
    }

    /**
     * Convenience function, gets the current stack trace.
     *
     * @return current stack trace
     */
    public static String currentStack() {
        return stackToString(Thread.currentThread().getStackTrace(), 2);
    }

    /**
     * Convenience function, gets the current element
     *
     * @param stacks
     *            to skip - 0 for immediate caller, 1, etc
     */
    public static StackTraceElement currentElement(int skip) {
        return Thread.currentThread().getStackTrace()[3 + skip];
    }

    /**
     *
     * @return true if there are no held objects
     */
    public boolean isEmpty() {
        return stacks.isEmpty();
    }

    /**
     * Iterate over held objects.
     */
    @Override
    public Iterator<Object> iterator() {
        return stacks.keySet().iterator();
    }

    /**
     * Example use.
     *
     * @param args
     *            ignored
     */
    public static void main(String args[]) {
        StackTracker tracker = new StackTracker();
        System.out.println("At first: " + tracker);

        tracker.add("Now");
        tracker.add("is");
        tracker.add("the");
        tracker.add("time");
        tracker.add("for");
        tracker.add("time");
        tracker.remove("Now");
        tracker.remove("for");
        tracker.remove("time");

        // any leaks?
        System.out.println("At end: " + tracker);
    }
}
