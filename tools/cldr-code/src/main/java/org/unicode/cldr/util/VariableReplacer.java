/*
 *******************************************************************************
 * Copyright (C) 2002-2012, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package org.unicode.cldr.util;

import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class VariableReplacer {
    // simple implementation for now
    Comparator<String> LONGEST_FIRST = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            int result = (o2.length() - o1.length());
            if (result != 0) {
                return result;
            }
            return o1.compareTo(o2);
        }

    };

    private Map<String,String> m = new TreeMap<>(LONGEST_FIRST);

    // TODO - fix to do streams also, clean up implementation

    public VariableReplacer add(String variable, String value) {
        m.put(variable, value);
        return this;
    }

    public String replace(String source) {
        String oldSource;
        do {
            oldSource = source;
            for (Entry<String, String> entry : m.entrySet()) {
                String variable = entry.getKey();
                String value = entry.getValue();
                source = replaceAll(source, variable, value);
            }
        } while (!source.equals(oldSource));
        return source;
    }

    public String replaceAll(String source, String key, String value) {
        // TODO optimize
        while (true) {
            int pos = source.indexOf(key);
            if (pos < 0) {
                return source;
            }
            source = source.substring(0, pos) + value + source.substring(pos + key.length());
        }
    }
}
