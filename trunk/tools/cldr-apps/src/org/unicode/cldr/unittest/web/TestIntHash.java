package org.unicode.cldr.unittest.web;

import java.util.HashSet;
import java.util.Set;

import org.unicode.cldr.web.IntHash;

import com.ibm.icu.dev.test.TestFmwk;

public class TestIntHash extends TestFmwk {
    // static TestInfo testInfo = TestInfo.getInstance();

    public static void main(String[] args) {
        new TestIntHash().run(args);
    }

    public static final int TEST_COUNT = 10000;

    public void TestGetPut() {
        IntHash<Integer> hash = new IntHash<Integer>();
        Set<Integer> s = new HashSet<Integer>();
        for (int i = 0; i < TEST_COUNT; i++) {
            int n = (i * 12) + i;
            hash.put(n, n);
            s.add(n);
            hash.put(i, i);
            s.add(i);
        }

        int ii = 0;
        for (int n : s) {
            int j = hash.get(n);
            if (j != n) {
                errln("Error: hash.get(" + n + ") returned " + j + "\n");
            } else {
                ++ii;
                // log(".."+n+(((ii)%30)==0?"\n":""));
            }
        }
        log("Tested " + ii + " values");
    }

}
