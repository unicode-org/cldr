package com.ibm.icu.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class MatchElementAttribute {
    private Multimap<String, String> matchElementAttribute =
            HashMultimap.create(); // "" is a wildcard

    public MatchElementAttribute add(String... elementAttributePairs) {
        for (int i = 0; i < elementAttributePairs.length; i += 2) {
            matchElementAttribute.put(elementAttributePairs[i], elementAttributePairs[i + 1]);
        }
        return this;
    }

    public boolean matches(String element, String attribute) {
        return matchElementAttribute.containsEntry(element, attribute)
                || matchElementAttribute.containsEntry("", attribute)
                || matchElementAttribute.containsEntry(element, "");
    }
}
