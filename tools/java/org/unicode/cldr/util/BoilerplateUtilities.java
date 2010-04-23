package org.unicode.cldr.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class BoilerplateUtilities {
  
  public static String toStringHelper(Object object) {
    StringBuffer result = new StringBuffer("[");
    Class<?> cls = object.getClass();
    Field[] fields = cls.getDeclaredFields();
    boolean gotOne = false;
    for (int i = 0; i < fields.length; ++i) {
      int mods = fields[i].getModifiers();
      if (Modifier.isStatic(mods)) continue;
      Object value = "no-access";
      try {
        value = fields[i].get(object);
      } catch (Exception e) {}
      if (value == null) continue;
      if (gotOne) result.append(", ");
      result.append(fields[i].getName()).append('=').append(value);
      gotOne = true;
    }
    result.append("]");
    return result.toString();
  }
  
  public static int compareToHelper(Object a, Object b, int depth) {
    if (a == null) {
      return b == null ? 0 : -1;
    } else if (b == null) {
      return 1;
    }
    Class<?> aClass = a.getClass();
    Class<?> bClass = b.getClass();
    if (aClass != bClass) {
      return aClass.getName().compareTo(bClass.getName());
    }
    if (depth != 0 && a instanceof Comparable<?>) {
        return ((Comparable<Object>)a).compareTo(b);
    }
    if (a instanceof Number) {
      double aDouble = ((Number)a).doubleValue();
      double bDouble = ((Number)b).doubleValue();
      return aDouble < bDouble ? -1 : aDouble == bDouble ? 0 : -1;
    }
    Field[] fields = aClass.getDeclaredFields();
    for (int i = 0; i < fields.length; ++i) {
      int mods = fields[i].getModifiers();
      if (Modifier.isStatic(mods)) continue;
      try {
        fields[i].get(a);
        fields[i].get(b);
      } catch (Exception e) {}
      int result = compareToHelper(a,b, depth+1);
      if (result != 0) return result;
    }
    return 0;
  }
}