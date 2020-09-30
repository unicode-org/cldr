package org.unicode.cldr.unittest.web;

/**
 * Copyright (C) 2012
 */

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRConfigImpl;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.web.STFactory;
import org.unicode.cldr.web.WebContext;

import com.ibm.icu.dev.test.TestFmwk;

/**
 * @author srl
 *
 */
public class TestMisc extends TestFmwk {
    public static void main(String[] args) {
        new TestMisc().run(args);
    }

    public void TestLocaleMaxSizer() {
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

            assertEquals(loc + ":" + xpath, (long) expectedSize, (long) lms.getSize(loc, xpath));
        }
    }

    /**
     * Test that the function WebContext.isCoverageOrganization returns
     * true for "Microsoft" and "microsoft", and false for "FakeOrgName".
     *
     * Reference: https://unicode.org/cldr/trac/ticket/10289
     */
    public void TestIsCoverageOrganization() {
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

    public void TestGitHash() {
        final String hash = CldrUtility.getCldrBaseDirHash();
        assertNotNull("getCldrBaseDirHash", hash);
        if (hash != null) {
            if (!hash.matches("[0-9a-f]+")) {
                errln("❌ getCldrBaseDirHash is not hex: " + hash);
            } else if (hash.length() < 8) {
                errln("❌ getCldrBaseDirHash is shorter than 8 chars: " + hash);
            }
        }
        final String strings[] = {"CLDR-Apps", "CLDR-Tools"};
        for (String s : strings) {
            final String version = CLDRConfigImpl.getGitHashForSlug(s);
            assertNotNull("getting " + s + " version", version);
            /*
             * TODO: fail if CLDRURLS.UNKNOWN_REVISION.equals(version).
             * Currently they do equal CLDRURLS.UNKNOWN_REVISION, since SurveyMain.init isn't called for TestAll.
             */
            // if (CLDRURLS.UNKNOWN_REVISION.equals(version)) {
            //    errln("❌ " + s + " = UNKNOWN_REVISION: " + version);
            // }
        }
    }
}
