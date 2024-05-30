package org.unicode.cldr.unittest.web;

import com.ibm.icu.dev.test.TestFmwk;
import java.sql.SQLException;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.TestSTFactory;
import org.unicode.cldr.web.UserRegistry;
import org.unicode.cldr.web.UserRegistry.User;

public class TestUserRegistry extends TestFmwk {
    public static void main(String[] args) {
        new TestUserRegistry().run(args);
    }

    public TestUserRegistry() {
        if (TestAll.skipIfNoDb()) return;
        TestAll.setupTestDb();
    }

    /**
     * Test the ability of a user to change another user's level, especially the aspects of
     * canSetUserLevel that depend on org.unicode.cldr.web.UserRegistry for info that isn't
     * available to org.unicode.cldr.unittest.TestUtilities.TestCanCreateOrSetLevelTo()
     *
     * @throws SQLException
     */
    public void TestCanSetUserLevel() throws SQLException {
        if (TestAll.skipIfNoDb()) return;
        TestSTFactory.getFactory(); // to setup the User Regitry.

        UserRegistry reg = CookieSession.sm.reg;
        int id = 2468;

        User a = reg.new User(id++);
        a.userlevel = UserRegistry.ADMIN;
        a.org = Organization.surveytool.name();

        User b = reg.new User(id++);
        b.userlevel = UserRegistry.TC;
        b.org = Organization.bangladesh.name();

        User c = reg.new User(id++);
        c.userlevel = UserRegistry.MANAGER;
        c.org = Organization.mozilla.name();

        User d = reg.new User(id++);
        d.userlevel = UserRegistry.VETTER;
        d.org = Organization.bangladesh.name();

        if (reg.canSetUserLevel(a, a, UserRegistry.ADMIN)) {
            errln("Can't change your own level");
        }
        if (reg.canSetUserLevel(b, a, UserRegistry.TC)) {
            errln(
                    "Can't change the level of anyone whose current level is more privileged than yours");
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
     * Test whether all organizations in UserRegistry.getOrgList are recognized by
     * Organization.fromString
     *
     * @throws SQLException
     */
    public void TestOrgList() throws SQLException {
        if (TestAll.skipIfNoDb()) {
            return;
        }
        TestSTFactory.getFactory(); // to setup the User Regitry.
        for (String name : UserRegistry.getOrgList()) {
            try {
                if (Organization.fromString(name) == null) {
                    errln("Organization.fromString returned null for " + name);
                }
            } catch (IllegalArgumentException e) {
                errln("Organization.fromString threw exception for " + name);
            }
        }
    }

    /**
     * Test the ability of a user to vote in a locale
     *
     * @throws SQLException
     */
    public void TestUserLocaleAuthorization() throws SQLException {
        if (TestAll.skipIfNoDb()) {
            return;
        }
        TestSTFactory.getFactory(); // to setup the User Regitry.
        UserRegistry reg = CookieSession.sm.reg;
        int id = 3579;
        CLDRLocale locA = CLDRLocale.getInstance("aa");
        CLDRLocale locZ = CLDRLocale.getInstance("zh");

        User admin = reg.new User(id++);
        admin.userlevel = UserRegistry.ADMIN;
        admin.locales = "zh";
        admin.org = "SurveyTool";
        if (UserRegistry.countUserVoteForLocaleWhy(admin, locA) != null) {
            errln("Admin can vote in any locale");
        }

        User locked = reg.new User(id++);
        locked.userlevel = UserRegistry.LOCKED;
        locked.locales = "*";
        locked.org = "Apple";
        if (UserRegistry.countUserVoteForLocaleWhy(locked, locZ)
                != UserRegistry.ModifyDenial.DENY_NO_RIGHTS) {
            errln("Locked cannot vote in any locale");
        }

        User vetter1 = reg.new User(id++);
        vetter1.userlevel = UserRegistry.VETTER;
        vetter1.locales = "aa";
        vetter1.org = "Google"; // assume Google has "aa" and/or "*" in Locales.txt
        if (UserRegistry.countUserVoteForLocaleWhy(vetter1, locA) != null) {
            errln("Vetter can vote in their locale if it is also their org locale");
        }
        if (UserRegistry.countUserVoteForLocaleWhy(vetter1, locZ)
                != UserRegistry.ModifyDenial.DENY_LOCALE_LIST) {
            errln("Vetter can only vote in their locale");
        }

        User vetter2 = reg.new User(id++);
        vetter2.userlevel = UserRegistry.VETTER;
        vetter2.locales = "aa zh";
        vetter2.org = "Adobe"; // assume Adobe has "zh" but not "aa" or "*" in Locales.txt
        if (UserRegistry.countUserVoteForLocaleWhy(vetter2, locA)
                != UserRegistry.ModifyDenial.DENY_LOCALE_LIST) {
            errln("Vetter cannot vote in their locale unless it is also their org locale");
        }
        if (UserRegistry.countUserVoteForLocaleWhy(vetter2, locZ) != null) {
            errln("Vetter can vote in one of their locales if it is also their org locale");
        }

        User guest = reg.new User(id++);
        guest.userlevel = UserRegistry.GUEST;
        guest.locales = "aa zh";
        guest.org = "Adobe"; // assume Adobe has "zh" but not "aa" or "*" in Locales.txt
        if (UserRegistry.countUserVoteForLocaleWhy(guest, locA) != null) {
            errln(
                    "Guest can vote in one of their locales regardless of whether it is their org locale");
        }
    }
}
