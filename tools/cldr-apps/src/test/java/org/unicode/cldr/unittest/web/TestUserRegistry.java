package org.unicode.cldr.unittest.web;

import org.unicode.cldr.util.Organization;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.UserRegistry;
import org.unicode.cldr.web.UserRegistry.User;

import com.ibm.icu.dev.test.TestFmwk;

public class TestUserRegistry extends TestFmwk {
    public static void main(String[] args) {
        new TestUserRegistry().run(args);
    }

    public TestUserRegistry() {
        TestAll.setupTestDb();
    }

    /**
     * Test the ability of a user to change another user's level,
     * especially the aspects of canSetUserLevel that depend on
     * org.unicode.cldr.web.UserRegistry for info that isn't available to
     * org.unicode.cldr.unittest.TestUtilities.TestCanCreateOrSetLevelTo()
     */
    public void TestCanSetUserLevel() {
        if (TestAll.skipIfDerby(this)) return;
        UserRegistry reg = CookieSession.sm.reg;
        int id = 2468;

        User a = reg.new User(id++);
        a.userlevel = UserRegistry.ADMIN;
        a.org = "SurveyTool";

        User b = reg.new User(id++);
        b.userlevel = UserRegistry.TC;
        b.org = "Bangladesh";

        User c = reg.new User(id++);
        c.userlevel = UserRegistry.MANAGER;
        c.org = "Mozilla";

        User d = reg.new User(id++);
        d.userlevel = UserRegistry.VETTER;
        d.org = "Bangladesh";

        if (reg.canSetUserLevel(a, a, UserRegistry.ADMIN)) {
            errln("Can't change your own level");
        }
        if (reg.canSetUserLevel(b, a, UserRegistry.TC)) {
            errln("Can't change the level of anyone whose current level is more privileged than yours");
        }
        if (reg.canSetUserLevel(b, c, UserRegistry.TC)) {
            errln("Can't change the level of someone in a different org, unless you are admin");
        }
        if (!reg.canSetUserLevel(a, c, UserRegistry.TC)) {
            errln("Admin should be able to change manager to TC");
        }
        if (!reg.canSetUserLevel(b, d, UserRegistry.TC)) {
            errln("TC should be able to change vetter in same org to TC");
        }
    }

    /**
     * A copy of what UserRegistry.getOrgList returned on 2020-03-22
     * using the production database
     */
    final String[] orgListCopy = {
        "Adlam",
        "Adobe",
        "Afghan_csa",
        "Afghan_mcit",
        "Afrigen",
        "Apple",
        "Bangladesh",
        "Bangladesh Computer Council",
        "Bangor_univ",
        "Bhutan",
        "Breton",
        "Cherokee",
        "cldr",
        "Gaeilge",
        "Georgia_isi",
        "Gnome",
        "Google",
        "Government of Pakistan - National Language Authority",
        "Guest",
        "IBM",
        "India",
        "Iran-HCI",
        "Kendra",
        "Kotoistus",
        "Lakota_lc",
        "Lao_dpt",
        "Long Now",
        "Meta",
        "Microsoft",
        "Mozilla",
        "Netflix",
        "OpenInstitute",
        "openoffice.org",
        "Oracle",
        "Rumantscha",
        "SIL",
        "srilanka",
        "SurveyTool",
        "Utilika",
        "Utilika Foundation",
        "Welsh LC",
        "Wikimedia",
        "Yahoo",
    };

    /**
     * Test whether all organizations in the users db table are recognized
     * by Organization.fromString
     *
     * On 2020-03-22 this test reported three errors:
     *
     * Organization.fromString returned null for Government of Pakistan - National Language Authority
     * Organization.fromString returned null for Utilika
     * Organization.fromString returned null for Utilika Foundation
     *
     * Those have been fixed by revising UserRegistry.getOrgList.
     *
     * This test highlights the strangeness of UserRegistry.getOrgList. It might make
     * more sense if UserRegistry.getOrgList simply used the list in Organization.java,
     * and if the database were constrained to use only those values.
     */
    public void TestOrgList() {
        /*
         * It won't work to call UserRegistry.getOrgList() here since the tests don't
         * use the real database. Instead, use a copy of what UserRegistry.getOrgList
         * returned at one point in time, based on the production database.
         */
        for (String name : orgListCopy) {
            try {
                if (Organization.fromString(name) == null) {
                    errln("Organization.fromString returned null for " + name);
                }
            } catch (IllegalArgumentException e) {
                errln("Organization.fromString threw exception for " + name);
            }
        }
    }
}
