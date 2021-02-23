package org.unicode.cldr.util;

import java.util.Comparator;

public class EnumComparator<E extends Enum<E>> implements Comparator<String> {
    private Class<E> enumClass;

    public static <E extends Enum<E>> EnumComparator<E> create(Class<E> enumClass) {
        return new EnumComparator<>(enumClass);
    }
    private EnumComparator(Class<E> enumClass) {
        this.enumClass = enumClass;
    }
    @Override
    public int compare(String o1, String o2) {
        E e1 = Enum.valueOf(enumClass, o1);
        E e2 = Enum.valueOf(enumClass, o2);
        return e1.compareTo(e2);
    }
}