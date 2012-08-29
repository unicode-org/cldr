package org.unicode.cldr.unittest;

import java.util.EnumSet;
import java.util.Map;
import java.util.Map.Entry;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.VoteResolver.Organization;

import com.ibm.icu.dev.test.TestFmwk;

public class StandardCodesTest extends TestFmwk {
    public static void main(String[] args) {
        new StandardCodesTest().run(args);
    }
    
    TestInfo testInfo = TestInfo.getInstance();
    
    public void testCoverage() {
        Map<String, Map<String, Level>> map = StandardCodes.make().getLocaleTypes();
        EnumSet<Organization> missing = EnumSet.noneOf(Organization.class);
        for (Organization org : Organization.values()) {
            // Sun  ;   ar  ;   modern 
            final Map<String, Level> entrySet = map.get(org.toString());
            if (entrySet == null) {
                missing.add(org);
                continue;
            }
            for (Entry<String, Level> entry2 : entrySet.entrySet()) {
                final String locale = entry2.getKey();
                String name = locale.equals("*") ? "ALL" : testInfo.getEnglish().getName(locale);
                logln(org + "\t;\t" + locale + "\t;\t" + entry2.getValue() + "\t;\t" + name);
            }
        }
        for (Organization org : missing) {
            errln("Organization missing Locale.txt information " + org);
        }
    }
}
