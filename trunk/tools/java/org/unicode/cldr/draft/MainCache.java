package org.unicode.cldr.draft;

import java.util.HashMap;
import java.util.Map;

public abstract class MainCache {
  private static Map<Class,Map> cache = new HashMap<Class,Map>();
  protected synchronized Object get(Object key) {
    Class classKey = this.getClass();
    Map submap = cache.get(classKey);
    if (submap == null) {
      cache.put(classKey, submap = new HashMap());
    }
    Object result = submap.get(key);
    if (result == null) {
      result = createObject(key);
      submap.put(key, result);
    }
    return result;
  }
  abstract protected Object createObject(Object key);
}
