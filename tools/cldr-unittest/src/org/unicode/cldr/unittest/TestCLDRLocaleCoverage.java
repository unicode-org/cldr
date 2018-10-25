package org.unicode.cldr.unittest;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.StandardCodes;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.ibm.icu.dev.util.CollectionUtilities;

public class TestCLDRLocaleCoverage extends TestFmwkPlus {
    private static StandardCodes sc = StandardCodes.make();

    public static void main(String[] args) {
        new TestCLDRLocaleCoverage().run(args);
    }

    /**
     * Test whether there are any locales for the organization CLDR
     */
    public void TestCLDROrganizationPresence() {
        Set<String> cldrLocales = sc.getLocaleCoverageLocales(
            Organization.cldr, EnumSet.of(Level.MODERN));
        assertNotNull("Expected CLDR modern locales not to be null",
            cldrLocales);
        assertTrue("Expected locales for CLDR, but found none.",
            cldrLocales != null && !cldrLocales.isEmpty());
    }

    /**
     * Tests that cldr is a superset.
     */
    public void TestCldrSuperset() {
        checkCldrLocales(Organization.apple, ERR);
        checkCldrLocales(Organization.google, ERR);
        checkCldrLocales(Organization.microsoft, WARN);
    }

    static Set<String> SKIP_SUPERSET = ImmutableSet.of("to", "fo");
    
    private void checkCldrLocales(Organization organization, int warningLevel) {
        // use a union, so that items can be higher
        EnumSet<Level> modernModerate = EnumSet.of(Level.MODERATE, Level.MODERN);
        
        Set<String> orgLocalesModerate = sc.getLocaleCoverageLocales(organization, modernModerate);
        Set<String> cldrLocalesModerate = sc.getLocaleCoverageLocales(Organization.cldr, modernModerate);
        Set<String> failures = checkCldrLocalesSuperset(modernModerate, cldrLocalesModerate, organization, orgLocalesModerate, warningLevel,
            SKIP_SUPERSET);

        EnumSet<Level> modernSet = EnumSet.of(Level.MODERN);
        Set<String> orgLocalesModern = sc.getLocaleCoverageLocales(organization, modernSet);
        Set<String> cldrLocalesModern = sc.getLocaleCoverageLocales(Organization.cldr, modernSet);
        failures = new HashSet<>(failures);
        failures.addAll(SKIP_SUPERSET);
        checkCldrLocalesSuperset(modernSet, cldrLocalesModern, organization, orgLocalesModern, warningLevel, failures);
    }

    private Set<String> checkCldrLocalesSuperset(Set<Level> level, Set<String> cldrLocales, Organization organization, Set<String> orgLocales, int warningLevel,
        Set<String> skip) {
        if (!cldrLocales.containsAll(orgLocales)) {
            Set<String> diff2 = new LinkedHashSet<>(Sets.difference(orgLocales, cldrLocales));
            diff2.removeAll(skip);
            if (!diff2.isEmpty()) {
                String diffString = diff2.toString();
                String levelString = CollectionUtilities.join(level, "+");
                for (String localeId : diff2) {
                    diffString += "\n\t" + localeId + "\t" + CLDRConfig.getInstance().getEnglish().getName(localeId);
                }
                msg("The following " + organization.displayName + " " + levelString + " locales were absent from the "
                    + Organization.cldr.displayName + " " + levelString + " locales:" + diffString,
                    warningLevel, true, true);
            }
            return diff2;
        }
        return Collections.EMPTY_SET;
    }
}
