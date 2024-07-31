package org.unicode.cldr.web;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableSet;
import java.sql.SQLException;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.VoteResolver;

public class TestUserLevel {
    static UserRegistry reg = null;

    private static int id = 9468; // Start user IDs here

    @ParameterizedTest
    @CsvFileSource(resources = "/org/unicode/cldr/web/TestUserLevel.csv", numLinesToSkip = 1)
    void TestCompatUserLevelDataDriven(
            String org,
            String levelStr,
            String operation,
            String expStr,
            String otherOrg,
            String otherLevel)
            throws SQLException {
        final Organization o = Organization.valueOf(org);
        assertNotNull(o, () -> "Could not parse organization " + org);
        VoteResolver.Level vrLevel = VoteResolver.Level.valueOf(levelStr);
        assertNotNull(vrLevel, () -> "Could not parse VR level " + levelStr);
        // just make a new user
        final UserRegistry reg2 = new UserRegistry() {
                    // Hack. Not nearly functional.
                };
        assertNotNull(reg2, "getRegistry() returned null");
        UserRegistry.User u = reg2.getTestUser(id++, o, vrLevel);
        testCompatAction(operation, expStr, otherOrg, otherLevel, o, u);
    }

    /** Test that a null user can't do stuff. Not shared with the other test (CSV). */
    @ParameterizedTest
    @CsvSource({
        // List of all operations of the form UserRegistry.<operation>(null)
        // org, null, operation, expStr, otherOrg, otherLevel
        "adlam, null, userCanListUsers, false, wod_nko, vetter",
        "adlam, null, userCanUseVettingParticipation, false, wod_nko, vetter",
        "adlam, null, userCanCreateUsers, false, wod_nko, vetter",
        "adlam, null, userCanEmailUsers, false, wod_nko, vetter",
        "adlam, null, userCanModifyUsers, false, wod_nko, vetter",
        "adlam, null, userCreateOtherOrgs, false, wod_nko, vetter",
        "adlam, null, userIsExactlyManager, false, wod_nko, vetter",
        "adlam, null, userIsManagerOrStronger, false, wod_nko, vetter",
        "adlam, null, userIsVetter, false, wod_nko, vetter",
        "adlam, null, userIsAdmin, false, wod_nko, vetter",
        "adlam, null, userIsTC, false, wod_nko, vetter",
        "adlam, null, userIsGuest, false, wod_nko, vetter",
        "adlam, null, userIsLocked, false, wod_nko, vetter",
        "adlam, null, userIsExactlyAnonymous, false, wod_nko, vetter",
        "adlam, null, userCanUseVettingSummary, false, wod_nko, vetter",
        "adlam, null, userCanSubmit_SUBMIT, false, wod_nko, vetter",
        "adlam, null, userCanCreateSummarySnapshot, false, wod_nko, vetter",
        "adlam, null, userCanMonitorForum, false, wod_nko, vetter",
        "adlam, null, userCanSetInterestLocales, false, wod_nko, vetter",
        "adlam, null, userCanGetEmailList, false, wod_nko, vetter"
    })
    void TestNullUserCompatibility(
            String org,
            String levelStr,
            String operation,
            String expStr,
            String otherOrg,
            String otherLevel)
            throws SQLException {
        // test that a null user can't do stuff
        assertEquals("null", levelStr);
        UserRegistry.User u = null;
        final Organization o = Organization.valueOf(org);
        assertNotNull(o, () -> "Could not parse organization " + org);
        testCompatAction(operation, expStr, otherOrg, otherLevel, o, u);
    }

    /** Test an action using compatibility */
    private void testCompatAction(
            String operation,
            String expStr,
            String otherOrg,
            String otherLevel,
            final Organization o,
            UserRegistry.User u) {
        Organization otherO = null;
        if (otherOrg != null && !otherOrg.isBlank()) {
            otherO = Organization.valueOf(otherOrg);
        }
        VoteResolver.Level otherL = null;
        if (otherLevel != null && !otherLevel.isBlank()) {
            otherL = VoteResolver.Level.valueOf(otherLevel);
        }
        Boolean expected;
        try {
            expected = Boolean.parseBoolean(expStr);
        } catch (Throwable t) {
            System.err.println(t);
            expected = null; // could not parse
        }
        Supplier<String> onFail =
                () -> (u) + "." + operation + "(" + otherOrg + "," + otherLevel + ")≠" + expStr;

        switch (operation) {
            case "isAdminForOrg":
                assertEquals(expected, u.isAdminForOrg(otherO.name()), onFail);
                break;
            case "votes":
                assertEquals(Integer.parseInt(expStr), u.getVoteCount(), onFail);
                break;
            case "canImportOldVotesSUBMISSION":
                assertEquals(expected, u.canImportOldVotes(CheckCLDR.Phase.SUBMISSION), onFail);
                break;
            case "userCanListUsers":
                assertEquals(expected, UserRegistry.userCanListUsers(u), onFail);
                break;
            case "userCanUseVettingParticipation":
                assertEquals(expected, UserRegistry.userCanUseVettingParticipation(u), onFail);
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
            case "userIsGuest":
                assertEquals(expected, UserRegistry.userIsGuest(u), onFail);
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
                assertEquals(
                        expected, UserRegistry.userCanSubmit(u, SurveyMain.Phase.SUBMIT), onFail);
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
            case "canManageSomeUsers":
                assertEquals(expected, u.getLevel().canManageSomeUsers(), onFail);
                break;
            case "isManagerFor":
                assertEquals(expected, u.getLevel().isManagerFor(o, otherL, otherO), onFail);
                break;
            case "canCreateOrSetLevelTo":
                assertEquals(expected, u.getLevel().canCreateOrSetLevelTo(otherL), onFail);
                break;
            case "canVoteWithCount":
                assertTrue(u.getLevel().canVoteWithCount(o, Integer.parseInt(expStr)), onFail);
                break;
            case "canNOTVoteWithCount":
                assertFalse(u.getLevel().canVoteWithCount(o, Integer.parseInt(expStr)), onFail);
                break;
            case "canDeleteUsers":
                assertEquals(expected, u.getLevel().canDeleteUsers(), onFail);
                break;
            default:
                assertFalse(true, "Unsupported operation in TestUserLevel.csv: " + operation);
        }
    }

    /**
     * TODO: NOTE: in theory this test could move OUT of cldr-apps into cldr-code, and perhaps it
     * should. However, it shares the data file, TestUserLevel.csv Perhaps a git symlink would do?
     */
    @ParameterizedTest
    @CsvFileSource(resources = "/org/unicode/cldr/web/TestUserLevel.csv", numLinesToSkip = 1)
    void TestVoteResolverLevel(
            String org,
            String levelStr,
            String operation,
            String expStr,
            String otherOrg,
            String otherLevel)
            throws SQLException {
        Boolean expected;
        try {
            expected = Boolean.parseBoolean(expStr);
        } catch (Throwable t) {
            System.err.println(t);
            expected = null; // could not parse
        }
        final Organization o = Organization.valueOf(org);
        assertNotNull(o, () -> "Could not parse organization " + org);
        VoteResolver.Level l = VoteResolver.Level.valueOf(levelStr);
        assertNotNull(l, () -> "Could not parse VR level " + levelStr);
        Organization otherO = null;
        if (otherOrg != null && !otherOrg.isBlank()) {
            otherO = Organization.valueOf(otherOrg);
        }
        VoteResolver.Level otherL = null;
        if (otherLevel != null && !otherLevel.isBlank()) {
            otherL = VoteResolver.Level.valueOf(otherLevel);
        }
        Supplier<String> onFail =
                () ->
                        (o.name() + "-" + l.name())
                                + "."
                                + operation
                                + "("
                                + otherOrg
                                + ","
                                + otherLevel
                                + ")≠"
                                + expStr;

        switch (operation) {
            case "isAdminForOrg":
                assertEquals(expected, l.isAdminForOrg(o, otherO), onFail);
                break;
            case "votes":
                assertEquals(Integer.parseInt(expStr), l.getVotes(o), onFail);
                break;
            case "canImportOldVotesSUBMISSION":
                assertEquals(expected, l.canImportOldVotes(CheckCLDR.Phase.SUBMISSION), onFail);
                break;
            case "userCanListUsers":
                assertEquals(expected, l.canListUsers(), onFail);
                break;
            case "userCanUseVettingParticipation":
                assertEquals(expected, l.canUseVettingParticipation(), onFail);
                break;
            case "userCanCreateUsers":
                assertEquals(expected, l.canCreateUsers(), onFail);
                break;
            case "userCanEmailUsers":
                assertEquals(expected, l.canEmailUsers(), onFail);
                break;
            case "userCanModifyUsers":
                assertEquals(expected, l.canModifyUsers(), onFail);
                break;
            case "userCreateOtherOrgs":
                assertEquals(expected, l.canCreateOtherOrgs(), onFail);
                break;
            case "userIsExactlyManager":
                assertEquals(expected, l.isExactlyManager(), onFail);
                break;
            case "userIsManagerOrStronger":
                assertEquals(expected, l.isManagerOrStronger(), onFail);
                break;
            case "userIsVetter":
                assertEquals(expected, l.isVetter(), onFail);
                break;
            case "userIsAdmin":
                assertEquals(expected, l.isAdmin(), onFail);
                break;
            case "userIsTC":
                assertEquals(expected, l.isTC(), onFail);
                break;
            case "userIsGuest":
                assertEquals(expected, l.isGuest(), onFail);
                break;
            case "userIsLocked":
                assertEquals(expected, l.isLocked(), onFail);
                break;
            case "userIsExactlyAnonymous":
                assertEquals(expected, l.isExactlyAnonymous(), onFail);
                break;
            case "userCanUseVettingSummary":
                assertEquals(expected, l.canUseVettingSummary(), onFail);
                break;
            case "userCanSubmit_SUBMIT":
                assertEquals(
                        expected, l.canSubmit(SurveyMain.Phase.SUBMIT.toCheckCLDRPhase()), onFail);
                break;
            case "userCanCreateSummarySnapshot":
                assertEquals(expected, l.canCreateSummarySnapshot(), onFail);
                break;
            case "userCanMonitorForum":
                assertEquals(expected, l.canMonitorForum(), onFail);
                break;
            case "userCanSetInterestLocales":
                assertEquals(expected, l.canSetInterestLocales(), onFail);
                break;
            case "userCanGetEmailList":
                assertEquals(expected, l.canGetEmailList(), onFail);
                break;
            case "canManageSomeUsers":
                assertEquals(expected, l.canManageSomeUsers(), onFail);
                break;
            case "isManagerFor":
                assertEquals(expected, l.isManagerFor(o, otherL, otherO), onFail);
                break;
            case "canCreateOrSetLevelTo":
                assertEquals(expected, l.canCreateOrSetLevelTo(otherL), onFail);
                break;
            case "canVoteWithCount":
                assertTrue(l.canVoteWithCount(o, Integer.parseInt(expStr)), onFail);
                break;
            case "canNOTVoteWithCount":
                assertFalse(l.canVoteWithCount(o, Integer.parseInt(expStr)), onFail);
                break;
            case "canDeleteUsers":
                assertEquals(expected, l.canDeleteUsers(), onFail);
                break;
            default:
                assertFalse(true, "Unsupported operation in TestUserLevel.csv: " + operation);
        }
    }

    @Test
    public void testVoteMenu() {
        assertAll(
                "VoteResolver.Level tests",
                () ->
                        assertEquals(
                                ImmutableSet.of(1, 4, 6, 50, 1000),
                                VoteResolver.Level.tc.getVoteCountMenu(Organization.apple)),
                () ->
                        assertEquals(
                                ImmutableSet.of(1, 4, 6, 50, 1000),
                                VoteResolver.Level.tc.getVoteCountMenu(Organization.unaffiliated)),
                () ->
                        assertEquals(
                                ImmutableSet.of(1, 4, 6, 50, 100, 1000),
                                VoteResolver.Level.admin.getVoteCountMenu(Organization.surveytool)),
                () -> assertNull(VoteResolver.Level.vetter.getVoteCountMenu(Organization.apple)),
                () ->
                        assertNull(
                                VoteResolver.Level.vetter.getVoteCountMenu(
                                        Organization.unaffiliated)),
                () ->
                        assertNull(
                                VoteResolver.Level.guest.getVoteCountMenu(
                                        Organization.unaffiliated)));
    }
}
