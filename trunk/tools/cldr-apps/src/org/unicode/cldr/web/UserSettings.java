/* Copyright (C) 2010-2011 IBM Corporation and Others. All Rights Reserved. */

package org.unicode.cldr.web;

public abstract class UserSettings implements Comparable<UserSettings> {
    /**
     * Get a string, or the default
     *
     * @param name
     *            name of setting to get
     * @param defaultValue
     *            default value to return (may be null)
     * @return the result, or default
     */
    public abstract String get(String name, String defaultValue);

    /**
     * Set a string
     *
     * @param name
     *            should be ASCII
     * @param value
     *            may be any Unicode string
     */
    public abstract void set(String name, String value);

    /**
     * Get an integer.
     *
     * @param name
     * @param defaultValue
     *            default value to return
     * @return the value, or the default
     */
    public int get(String name, int defaultValue) {
        String asStr = get(name, null);
        if (asStr == null) {
            return defaultValue;
        } else {
            return Integer.parseInt(asStr);
        }
    }

    /**
     * Set an integer
     *
     * @param name
     *            should be ASCII
     * @param value
     */
    public void set(String name, int value) {
        set(name, Integer.toString(value));
    }

    public boolean get(String name, boolean defaultValue) {
        String asStr = get(name, null);
        if (asStr == null) {
            return defaultValue;
        } else {
            return Boolean.parseBoolean(asStr);
        }
    }

    public void set(String name, boolean value) {
        set(name, Boolean.toString(value));
    }

    public boolean persistent() {
        return false;
    }
}
