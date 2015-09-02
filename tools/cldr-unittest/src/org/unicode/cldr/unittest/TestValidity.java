package org.unicode.cldr.unittest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.Validity;

public class TestValidity extends TestFmwkPlus {

    public static void main(String[] args) {
        new TestValidity().run(args);
    }
    
    Validity v = Validity.getInstance(CLDRPaths.COMMON_DIRECTORY);

    public void TestBasicValidity() {
        Object[][] tests = {
            {LstrType.language, Validity.Status.regular, true, "aa", "en"},
            {LstrType.language, Validity.Status.regular, false, "eng"},
            {LstrType.language, Validity.Status.special, false, "root"},
            {LstrType.region, Validity.Status.unknown, false, "ZZ"},
        };
        for (Object[] test : tests) {
            LstrType lstr = (LstrType) test[0];
            Validity.Status subtype = (Validity.Status) test[1];
            Boolean desired = (Boolean) test[2];
            Set<String> types = new HashSet(Arrays.asList(test).subList(3, test.length-1));
            
            Set<String> actual = v.getData().get(lstr).get(subtype);
            assertRelation("Validity", true, actual, TestFmwkPlus.CONTAINS_ALL, types);
        }
        if (isVerbose()) {
            for (Entry<LstrType, Map<Validity.Status, Set<String>>> entry: v.getData().entrySet()) {
                logln(entry.getKey().toString());
                for (Entry<Validity.Status, Set<String>> entry2: entry.getValue().entrySet()) {
                    logln("\t" + entry2.getKey());
                    logln("\t\t" + entry2.getValue());
                }
            }
        }
    }
}
