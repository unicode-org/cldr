package org.unicode.cldr.unittest.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.unicode.cldr.web.IntHash;

public class TestIntHash  {
    // static TestInfo testInfo = TestInfo.getInstance();

    public static final int TEST_COUNT = 10000;

    @Test
    public void testGetPut() {
        IntHash<Integer> hash = new IntHash<Integer>();
        Set<Integer> s = new HashSet<Integer>();
        for (int i = 0; i < TEST_COUNT; i++) {
            int n = (i * 12) + i;
            hash.put(n, n);
            s.add(n);
            hash.put(i, i);
            s.add(i);
        }

        // int ii = 0;
        for (int n : s) {
            int j = hash.get(n);
            assertEquals(j, n, "hash.get() failed");
            // ++ii;
            // log(".."+n+(((ii)%30)==0?"\n":""));
        }
    }

}
