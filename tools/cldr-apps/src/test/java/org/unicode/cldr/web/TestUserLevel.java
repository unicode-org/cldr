package org.unicode.cldr.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.SQLException;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.unittest.web.TestAll;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.VoteResolver;

public class TestUserLevel {
    static UserRegistry reg;

    private static int id = 9468; // Start user IDs here

    @BeforeAll
    static void setup() throws SQLException {
        TestAll.doResetDb(null);
        reg = getRegistry();
        assertNotNull(reg, "reg is null");
    }

    @AfterAll
    static void cleanup() throws SQLException {
        // Reset again.
        TestAll.doResetDb(null);
        CookieSession.sm = null;
        reg = null;
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/org/unicode/cldr/web/TestUserLevel.csv", numLinesToSkip=1)
    void TestCompatUserLevelDataDriven(String org, String levelStr, String operation, String expStr) throws SQLException {
        Boolean expected;
        try {
            expected = Boolean.parseBoolean(expStr);
        } catch(Throwable t) {
            System.err.println(t);
            expected = null; // could not parse
        }
        reg = getRegistry();
        assertNotNull(reg, "reg is null");
        final Organization o = Organization.valueOf(org);
        assertNotNull(o, () -> "Could not parse organization " + org);
        VoteResolver.Level vrLevel = VoteResolver.Level.valueOf(levelStr);
        assertNotNull(vrLevel, () -> "Could not parse VR level " + levelStr);
        // just make a new user
        UserRegistry.User u = reg.getTestUser(id++, o, vrLevel);
        Supplier<String> onFail = () -> (u.toString())+"."+operation+"()≠"+expStr;
        switch (operation) {
        case "isAdminForSameOrg":
            assertEquals(expected, u.isAdminForOrg(org), onFail);
            break;
        case "isAdminForNko":
            assertEquals(expected, u.isAdminForOrg(Organization.wod_nko.name()), onFail);
            break;
        case "votes":
            assertEquals(Integer.parseInt(expStr), u.getVoteCount(), onFail);
            break;
        case "canImportOldVotesSUBMISSION":
            assertEquals(expected, u.canImportOldVotes(CheckCLDR.Phase.SUBMISSION), onFail);
            break;
        case "userCanDoList":
            assertEquals(expected, UserRegistry.userCanDoList(u), onFail);
            break;
        case "userCanCreateUsers":
            assertEquals(expected, UserRegistry.userCanCreateUsers(u), onFail);
            break;
        case "userCanEmailUsers":
            assertEquals(expected, UserRegistry.userCanEmailUsers(u), onFail);
            break;
        case "userCanModifyUsers":
            assertEquals(expected, UserRegistry.userCanModifyUsers(u), onFail);
            break;
        case "userCreateOtherOrgs":
            assertEquals(expected, UserRegistry.userCreateOtherOrgs(u), onFail);
            break;
        case "userIsExactlyManager":
            assertEquals(expected, UserRegistry.userIsExactlyManager(u), onFail);
            break;
        case "userIsManagerOrStronger":
            assertEquals(expected, UserRegistry.userIsManagerOrStronger(u), onFail);
            break;
        case "userIsVetter":
            assertEquals(expected, UserRegistry.userIsVetter(u), onFail);
            break;
        case "userIsAdmin":
            assertEquals(expected, UserRegistry.userIsAdmin(u), onFail);
            break;
        case "userIsTC":
            assertEquals(expected, UserRegistry.userIsTC(u), onFail);
            break;
        case "userIsStreet":
            assertEquals(expected, UserRegistry.userIsStreet(u), onFail);
            break;
        case "userIsLocked":
            assertEquals(expected, UserRegistry.userIsLocked(u), onFail);
            break;
        case "userIsExactlyAnonymous":
            assertEquals(expected, UserRegistry.userIsExactlyAnonymous(u), onFail);
            break;
        case "userCanUseVettingSummary":
            assertEquals(expected, UserRegistry.userCanUseVettingSummary(u), onFail);
            break;
        case "userCanSubmit_SUBMIT":
            assertEquals(expected, UserRegistry.userCanSubmit(u, SurveyMain.Phase.SUBMIT), onFail);
            break;
        case "userCanCreateSummarySnapshot":
            assertEquals(expected, UserRegistry.userCanCreateSummarySnapshot(u), onFail);
            break;
        case "userCanMonitorForum":
            assertEquals(expected, UserRegistry.userCanMonitorForum(u), onFail);
            break;
        case "userCanSetInterestLocales":
            assertEquals(expected, UserRegistry.userCanSetInterestLocales(u), onFail);
            break;
        case "userCanGetEmailList":
            assertEquals(expected, UserRegistry.userCanGetEmailList(u), onFail);
            break;
        default:
            assertFalse(true, "Unsupported operation in TestUserLevel.csv: " + operation);
        }
    }

    static UserRegistry getRegistry() throws SQLException {
        // We need a real UserRegistry to make this work
        TestAll.setupTestDb();

        if (CookieSession.sm == null) {
            SurveyMain sm = new SurveyMain();
            CookieSession.sm = sm; // hack - of course.
        }

        if (CookieSession.sm.reg == null) {
            CookieSession.sm.reg = UserRegistry.createRegistry(CookieSession.sm);
        }

        assertNotNull(CookieSession.sm.reg, "cs.sm.reg is null");
        return CookieSession.sm.reg;
    }
}
