package org.unicode.cldr.unittest;

import java.util.EnumSet;
import java.util.Set;

import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.VoteResolver.Organization;

import com.google.common.collect.Sets;

public class TestCLDRLocaleCoverage extends TestFmwkPlus {
    private static  StandardCodes sc=StandardCodes.make();
    public static void main(String[] args) {
        new TestCLDRLocaleCoverage().run(args);
    }
    
    /**
     * Test whether there are any locales for the organization CLDR
     */
    public void TestCLDROrganizationPresence() {
        Set<String> cldrLocales=sc.getLocaleCoverageLocales(Organization.cldr.name(), EnumSet.of(Level.MODERN));
        assertTrue("Expected locales for CLDR, but found none.",cldrLocales!=null && !cldrLocales.isEmpty());
    }
    /**
    * Tests the validity of the file names and of the English localeDisplayName types. 
    * Also tests for aliases outside root
    */
    public void TestModernCLDRCoverage() {
        Set<String> googleLocales=sc.getLocaleCoverageLocales(Organization.google.name(),EnumSet.of(Level.MODERN));
        Set<String> cldrLocales=sc.getLocaleCoverageLocales(Organization.cldr.name(), EnumSet.of(Level.MODERN));
        if (cldrLocales!=null) {
            if (!cldrLocales.equals(googleLocales)) {
                Set<String> diff1=Sets.difference(cldrLocales, googleLocales);
                if (!diff1.isEmpty()) {
                    warnln("The following CLDR modern locales were not also in the Google set:"+diff1.toString());
                }
                Set<String> diff2=Sets.difference(googleLocales, cldrLocales);
                if (!diff2.isEmpty()) {
                    warnln("The following Google modern locales were not also in the CLDR set:"+diff2.toString());
                }
            }
            assertTrue("Expected Google and CLDR locales to contain the same elements, but they did not.", cldrLocales.equals(googleLocales)); 
        }
    }
}
