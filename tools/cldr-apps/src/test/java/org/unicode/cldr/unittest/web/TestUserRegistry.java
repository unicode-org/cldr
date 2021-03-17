package org.unicode.cldr.unittest.web;

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
}
