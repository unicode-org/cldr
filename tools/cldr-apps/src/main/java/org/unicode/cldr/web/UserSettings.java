/* Copyright (C) 2010-2011 IBM Corporation and Others. All Rights Reserved. */

package org.unicode.cldr.web;

import com.google.gson.Gson;

public abstract class UserSettings implements Comparable<UserSettings> {
    /**
     * Get a string, or the default
     *
     * @param name name of setting to get
     * @param defaultValue default value to return (may be null)
     * @return the result, or default
     */
    public abstract String get(String name, String defaultValue);

    /**
     * Set a string
     *
     * @param name should be ASCII
     * @param value may be any Unicode string
     */
    public abstract void set(String name, String value);

    /**
     * Get an integer.
     *
     * @param name
     * @param defaultValue default value to return
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
     * Get a long.
     *
     * @param name
     * @param defaultValue default value to return
     * @return the value, or the default
     */
    public long get(String name, long defaultValue) {
        String asStr = get(name, null);
        if (asStr == null) {
            return defaultValue;
        } else {
            return Long.parseLong(asStr);
        }
    }

    /**
     * Set an integer
     *
     * @param name should be ASCII
     * @param value
     */
    public void set(String name, int value) {
        set(name, Integer.toString(value));
    }

    /**
     * Set an long
     *
     * @param name should be ASCII
     * @param value
     */
    public void set(String name, long value) {
        set(name, Long.toString(value));
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

    public void setJson(String name, Object o) {
        final Gson gson = new Gson();
        set(name, gson.toJson(o));
    }

    public <T> T getJson(String name, Class<T> clazz) {
        final Gson gson = new Gson();
        final String j = get(name, null);
        if (j == null || j.isBlank()) return null;
        return gson.fromJson(j, clazz);
    }
}
