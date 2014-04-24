package org.unicode.cldr.unittest;

import java.util.Iterator;

import org.unicode.cldr.util.LruMap;

import com.ibm.icu.dev.test.TestFmwk;

public class TestLruMap extends TestFmwk {
    public static void main(String[] args) {
        new TestLruMap().run(args);
    }

    public void TestMapOrdering() {
        LruMap<String, Integer> map = new LruMap<String, Integer>(3);
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        Iterator<String> iterator = map.keySet().iterator();
        assertEquals("Map Size", 3, map.size());
        assertEquals("Oldest item", "a", iterator.next());
        assertEquals("Second item", "b", iterator.next());
        assertEquals("Newest item", "c", iterator.next());

        map.get("b");
        iterator = map.keySet().iterator();
        assertEquals("Map Size", 3, map.size());
        assertEquals("Oldest item", "a", iterator.next());
        assertEquals("Second item", "c", iterator.next());
        assertEquals("Newest item", "b", iterator.next());

        map.put("d", 4);
        iterator = map.keySet().iterator();
        assertEquals("Map Size", 3, map.size());
        assertEquals("Oldest item", "c", iterator.next());
        assertEquals("Second item", "b", iterator.next());
        assertEquals("Newest item", "d", iterator.next());
    }
}