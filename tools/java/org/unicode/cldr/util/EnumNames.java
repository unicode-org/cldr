package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EnumNames<T extends Enum<?>> {
    private Map<String, T> namesToEnum = new HashMap<String, T>();
    private ArrayList<String> enumToPreferredName = new ArrayList<String>();

    public void add(T enumItem, String... alternateNames) {
        final String name = enumItem.name();
        namesToEnum.put(name, enumItem);
        namesToEnum.put(name.toLowerCase(Locale.ENGLISH), enumItem);
        // add() is safe, because we are guaranteed that they are added in order.
        if (alternateNames.length == 0) {
            enumToPreferredName.add(name);
        } else {
            enumToPreferredName.add(alternateNames[0]);
            for (String other : alternateNames) {
                namesToEnum.put(other, enumItem);
                namesToEnum.put(other.toLowerCase(Locale.ENGLISH), enumItem);
            }
        }
    }

    public T forString(String name) {
        T result = namesToEnum.get(name);
        if (result != null) {
            return result;
        }
        result = namesToEnum.get(name.toLowerCase(Locale.ENGLISH));
        if (result != null) {
            return result;
        }
        throw new IllegalArgumentException("No enum value for " + name + ", should be one of " + namesToEnum.keySet());
    }

    public String toString(T item) {
        return enumToPreferredName.get(item.ordinal());
    }
}