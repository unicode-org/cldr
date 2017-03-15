/**
 * Copyright (C) 2012 IBM Corporation and Others. All Rights Reserved.
 */

package org.unicode.cldr.web;

public class Stamp implements Comparable<Stamp> {
    private static long startStamp = System.currentTimeMillis();
    private static long lastStamp = startStamp;

    protected static synchronized long nextStampTime() {
        return ++lastStamp;
    }

    public static Stamp getInstance() {
        return new Stamp(nextStampTime());
    }

    protected long stamp;

    protected Stamp(long stamp) {
        this.stamp = stamp;
    }

    @Override
    public int compareTo(Stamp other) {
        if (this == other || this.stamp == other.stamp) {
            return 0;
        } else if (this.stamp < other.stamp) {
            return -1;
        } else {
            return 1;
        }
    }

    @Override
    public boolean equals(Object other) {
        return (this == other || (other instanceof Stamp && this.stamp == ((Stamp) other).stamp));
    }

    @Override
    public int hashCode() {
        return (int) stamp;
    }

    /**
     * Get the stamp's current value
     *
     * @return
     */
    public long current() {
        return stamp;
    }

}
