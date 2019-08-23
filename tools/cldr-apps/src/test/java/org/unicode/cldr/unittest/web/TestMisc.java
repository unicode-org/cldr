package org.unicode.cldr.unittest.web;

import org.junit.jupiter.api.Test;

/**
 * Copyright (C) 2012
 */

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRConfigImpl;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.web.STFactory;
import org.unicode.cldr.web.WebContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.ibm.icu.dev.test.TestFmwk;


/**
 * @author srl
 *
 */
public class TestMisc extends TestAll {
    @Test
    public void testLocaleMaxSizer() {
        logln("Creating new sizer..");
        STFactory.LocaleMaxSizer lms = new STFactory.LocaleMaxSizer();

        Factory f = CLDRConfig.getInstance().getCldrFactory();
        logln("Populating sizer..");
        for (CLDRLocale l : f.getAvailableCLDRLocales()) {
            lms.add(l);
        }

        final String TESTCASES[] = {
            "root", "//foo/bar", Integer.toString(STFactory.LocaleMaxSizer.MAX_VAL_LEN),
            "zh", "//foo/bar", Integer.toString(STFactory.LocaleMaxSizer.MAX_VAL_LEN),
            "zh", "//ldml/characters/exemplarCharacters", Integer.toString(STFactory.LocaleMaxSizer.EXEMPLAR_CHARACTERS_MAX),
            "zh_Hans_CN", "//ldml/characters/exemplarCharacters", Integer.toString(STFactory.LocaleMaxSizer.EXEMPLAR_CHARACTERS_MAX),
            "zh_Hans_CN", "//ldml/characters/exemplarCharacters", Integer.toString(STFactory.LocaleMaxSizer.EXEMPLAR_CHARACTERS_MAX),
            "zh_Hant", "//ldml/characters/exemplarCharacters[@type=\"auxiliary\"]", Integer.toString(STFactory.LocaleMaxSizer.EXEMPLAR_CHARACTERS_MAX),
            "zh_Hant_HK", "//ldml/characters/exemplarCharacters", Integer.toString(STFactory.LocaleMaxSizer.EXEMPLAR_CHARACTERS_MAX),
        };

        for (int i = 0; i < TESTCASES.length; i += 3) {
            final CLDRLocale loc = CLDRLocale.getInstance(TESTCASES[i + 0]);
            final String xpath = TESTCASES[i + 1];
            final Integer expectedSize = Integer.parseInt(TESTCASES[i + 2]);

            assertEquals((long) expectedSize, (long) lms.getSize(loc, xpath), loc + ":" + xpath);
        }
    }
 
    /**
     * Test that the function WebContext.isCoverageOrganization returns
     * true for "Microsoft" and "microsoft", and false for "FakeOrgName".
     *
     * Reference: https://unicode.org/cldr/trac/ticket/10289
     */
    @Test
    public void testIsCoverageOrganization() {
        String orgName = "Microsoft";
        try {
            if (!WebContext.isCoverageOrganization(orgName)) {
                errln("❌ isCoverageOrganization(" + orgName + ") false, expected true.");
                return;
            }
            orgName = orgName.toLowerCase();
            if (!WebContext.isCoverageOrganization(orgName)) {
                errln("❌ isCoverageOrganization(" + orgName + ") false, expected true.");
                return;
            }
            orgName = "FakeOrgName";
            if (WebContext.isCoverageOrganization(orgName)) {
                errln("❌ isCoverageOrganization(" + orgName + ") true, expected false.");
                return;
            }
        } catch (Exception e) {
            errln("❌ isCoverageOrganization(" + orgName + "). Unexpected exception: " + e.toString() + " - " + e.getMessage());
            return;
        }
        System.out.println("✅");
    }
    
    @Test
    public void testGitHash() {
        String appsVersion = CLDRConfigImpl.getGitHashForSlug("CLDR-Apps");
        assertNotNull(appsVersion, "getting CLDR-Apps version");
//        System.out.println("Apps: " + appsVersion);
        String toolsVersion = CLDRConfigImpl.getGitHashForSlug("CLDR-Tools");
        assertNotNull(toolsVersion, "getting CLDR-Tools version");
//        System.out.println("Tools: " + toolsVersion);
    }
}
