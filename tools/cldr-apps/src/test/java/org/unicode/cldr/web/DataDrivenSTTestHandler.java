package org.unicode.cldr.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.util.VoteResolver.Level;
import org.unicode.cldr.util.VoteResolver.Status;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.web.BallotBox.InvalidXPathException;
import org.unicode.cldr.web.BallotBox.VoteNotAcceptedException;
import org.unicode.cldr.web.UserRegistry.User;
import org.unicode.cldr.web.api.VoteAPI;
import org.unicode.cldr.web.api.VoteAPI.RowResponse;
import org.unicode.cldr.web.api.VoteAPI.RowResponse.Row;
import org.unicode.cldr.web.api.VoteAPIHelper;
import org.unicode.cldr.web.api.VoteAPIHelper.ArgsForGet;

final class DataDrivenSTTestHandler extends XMLFileReader.SimpleHandler {
    private final Map<String, String> vars;
    private final STFactory fac;
    private final File targDir;
    private final Map<String, String> attrs;
    final Map<String, UserRegistry.User> users = new TreeMap<>();
    int pathCount = 0;

    DataDrivenSTTestHandler(
            Map<String, String> vars, STFactory fac, File targDir, Map<String, String> attrs) {
        this.vars = vars;
        this.fac = fac;
        this.targDir = targDir;
        this.attrs = attrs;
    }

    @Override
    public void handlePathValue(String path, String value) {
        ++pathCount;
        if (value != null && value.startsWith("$")) {
            String varName = value.substring(1);
            value = vars.get(varName);
            System.out.println(" $" + varName + " == '" + value + "'");
        }

        XPathParts xpp = XPathParts.getFrozenInstance(path);
        attrs.clear();
        for (String k : xpp.getAttributeKeys(-1)) {
            attrs.put(k, xpp.getAttributeValue(-1, k));
        }
        if ("mul_ZZ".equals(attrs.get("locale"))) {
            int debug = 0;
        }

        String elem = xpp.getElement(-1);
        if (false)
            System.out.println(
                    "* <" + elem + " " + attrs.toString() + ">" + value + "</" + elem + ">");
        String xpath = attrs.get("xpath");
        if (xpath != null) {
            xpath = xpath.trim().replace("'", "\"");
        }
        switch (elem) {
            case "user":
                handleElementUser(fac, attrs);
                break;
            case "setvar":
                handleElementSetvar(fac, attrs, vars, xpath);
                break;
            case "apivote":
            case "apiunvote":
                handleElementApivote(attrs, value, elem, xpath);
                break;
            case "vote":
            case "unvote":
                handleElementVote(fac, attrs, value, elem, xpath);
                break;
            case "apiverify":
                handleElementApiverify(attrs, value, xpath);
                break;
            case "verify":
                try {
                    handleElementVerify(fac, attrs, value, elem, xpath);
                } catch (IOException e) {
                    fail(e);
                }
                break;
            case "verifyUser":
                handleElementVerifyUser(attrs);
                break;
            case "echo":
            case "warn":
                handleElementEcho(value, elem);
                break;
            default:
                throw new IllegalArgumentException("Unknown test element type " + elem);
        }
    }

    private void handleElementVerify(
            STFactory fac, Map<String, String> attrs, String value, String elem, String xpath)
            throws IOException {
        value = value.trim();
        if (value.isEmpty()) value = null;
        CLDRLocale locale = CLDRLocale.getInstance(attrs.get("locale"));
        BallotBox<User> box = fac.ballotBoxForLocale(locale);
        CLDRFile cf = fac.make(locale, true);
        String stringValue = cf.getStringValue(xpath);
        String fullXpath = cf.getFullXPath(xpath);
        // System.out.println("V"+ xpath + " = " + stringValue + ", " +
        // fullXpath);
        // System.out.println("Resolver=" + box.getResolver(xpath));
        if (value == null && stringValue != null) {
            fail(
                    pathCount
                            + "a Expected null value at "
                            + locale
                            + ":"
                            + xpath
                            + " got "
                            + stringValue);
        } else if (value != null && !value.equals(stringValue)) {
            fail(
                    pathCount
                            + "b Expected "
                            + value
                            + " at "
                            + locale
                            + ":"
                            + xpath
                            + " got "
                            + stringValue);
        } else {
            System.out.println("OK: " + locale + ":" + xpath + " = " + value);
        }
        Status expStatus = Status.fromString(attrs.get("status"));

        VoteResolver<String> r = box.getResolver(xpath);
        Status winStatus = r.getWinningStatus();
        if (winStatus == expStatus) {
            System.out.println(
                    "OK: Status="
                            + winStatus
                            + " "
                            + locale
                            + ":"
                            + xpath
                            + " Resolver="
                            + box.getResolver(xpath));
        } else if (pathCount == 49 && !VoteResolver.DROP_HARD_INHERITANCE) {
            System.out.println(
                    "Ignoring status mismatch for "
                            + pathCount
                            + "c, test assumes DROP_HARD_INHERITANCE is true");
        } else {
            fail(
                    pathCount
                            + "c Expected: Status="
                            + expStatus
                            + " got "
                            + winStatus
                            + " "
                            + locale
                            + ":"
                            + xpath
                            + " Resolver="
                            + box.getResolver(xpath));
        }

        Status xpathStatus;
        CLDRFile.Status newPath = new CLDRFile.Status();
        CLDRLocale newLocale =
                CLDRLocale.getInstance(cf.getSourceLocaleIdExtended(fullXpath, newPath, false));
        final boolean localeChanged = newLocale != null && !newLocale.equals(locale);
        final boolean pathChanged =
                newPath.pathWhereFound != null && !newPath.pathWhereFound.equals(xpath);
        final boolean itMoved = localeChanged || pathChanged;
        if (localeChanged && pathChanged) {
            System.out.println(
                    "Aliased(locale+path): "
                            + locale
                            + "->"
                            + newLocale
                            + " and "
                            + xpath
                            + "->"
                            + newPath.pathWhereFound);
        } else if (localeChanged) {
            System.out.println("Aliased(locale): " + locale + "->" + newLocale);
        } else if (pathChanged) {
            System.out.println("Aliased(path): " + xpath + "->" + newPath.pathWhereFound);
        }
        if ((fullXpath == null) || itMoved) {
            xpathStatus = Status.missing;
        } else {
            XPathParts xpp2 = XPathParts.getFrozenInstance(fullXpath);
            String statusFromXpath = xpp2.getAttributeValue(-1, "draft");

            if (statusFromXpath == null) {
                statusFromXpath = "approved"; // no draft = approved
            }
            xpathStatus = Status.fromString(statusFromXpath);
        }
        if (xpathStatus != winStatus) {
            System.out.println(
                    "Warning: Winning Status="
                            + winStatus
                            + " but xpath status is "
                            + xpathStatus
                            + " "
                            + locale
                            + ":"
                            + fullXpath
                            + " Resolver="
                            + box.getResolver(xpath));
        } else if (xpathStatus == expStatus) {
            System.out.println(
                    "OK from fullxpath: Status="
                            + xpathStatus
                            + " "
                            + locale
                            + ":"
                            + fullXpath
                            + " Resolver="
                            + box.getResolver(xpath));
        } else {
            fail(
                    pathCount
                            + "d Expected from fullxpath: Status="
                            + expStatus
                            + " got "
                            + xpathStatus
                            + " "
                            + locale
                            + ":"
                            + fullXpath
                            + " Resolver="
                            + box.getResolver(xpath));
        }

        // Verify from XML
        File outFile = new File(targDir, locale.getBaseName() + ".xml");
        if (outFile.exists()) outFile.delete();
        PrintWriter pw;
        pw = FileUtilities.openUTF8Writer(targDir.getAbsolutePath(), locale.getBaseName() + ".xml");
        cf.write(pw, TestSTFactory.noDtdPlease);
        pw.close();

        // System.out.println("Read back..");
        CLDRFile readBack = null;
        readBack = CLDRFile.loadFromFile(outFile, locale.getBaseName(), DraftStatus.unconfirmed);
        String reRead = readBack.getStringValue(xpath);
        String fullXpathBack = readBack.getFullXPath(xpath);
        Status xpathStatusBack;
        if (fullXpathBack == null || itMoved) {
            xpathStatusBack = Status.missing;
        } else {
            XPathParts xpp2 = XPathParts.getFrozenInstance(fullXpathBack);
            String statusFromXpathBack = xpp2.getAttributeValue(-1, "draft");

            if (statusFromXpathBack == null) {
                statusFromXpathBack = "approved"; // no draft =
                // approved
            }
            xpathStatusBack = Status.fromString(statusFromXpathBack);
        }

        if (value == null && reRead != null) {
            fail(
                    pathCount
                            + "e Expected null value from XML at "
                            + locale
                            + ":"
                            + xpath
                            + " got "
                            + reRead);
        } else if (value != null && !value.equals(reRead)) {
            fail(
                    pathCount
                            + "f Expected from XML "
                            + value
                            + " at "
                            + locale
                            + ":"
                            + xpath
                            + " got "
                            + reRead);
        } else {
            System.out.println("OK from XML: " + locale + ":" + xpath + " = " + reRead);
        }

        if (xpathStatusBack == expStatus) {
            System.out.println(
                    "OK from XML: Status="
                            + xpathStatusBack
                            + " "
                            + locale
                            + ":"
                            + fullXpathBack
                            + " Resolver="
                            + box.getResolver(xpath));
        } else if (xpathStatusBack != winStatus) {
            System.out.println(
                    "Warning: Problem from XML: Winning Status="
                            + winStatus
                            + " got "
                            + xpathStatusBack
                            + " "
                            + locale
                            + ":"
                            + fullXpathBack
                            + " Resolver="
                            + box.getResolver(xpath));
        } else {
            fail(
                    pathCount
                            + "g Expected from XML: Status="
                            + expStatus
                            + " got "
                            + xpathStatusBack
                            + " "
                            + locale
                            + ":"
                            + fullXpathBack
                            + " Resolver="
                            + box.getResolver(xpath));
        }
        verifyOrgStatus(r, attrs);
    }

    private void handleElementEcho(String value, String elem) {
        if (value == null) {
            System.out.println("*** " + elem + "  \"" + "null" + "\"");
        } else {
            System.out.println("*** " + elem + "  \"" + value.trim() + "\"");
        }
    }

    private void handleElementVerifyUser(final Map<String, String> attrs) {
        final User u = getUserFromAttrs(attrs, "name");
        final User onUser = getUserFromAttrs(attrs, "onUser");
        final String action = attrs.get("action");
        final boolean allowed = getBooleanAttr(attrs, "allowed", true);
        boolean actualResult = true;
        //                    <!ATTLIST verifyUser action ( create |
        // delete | modify | list ) #REQUIRED>
        final Level uLevel = u.getLevel();
        final Level onLevel = onUser.getLevel();
        switch (action) {
            case "create":
                actualResult = actualResult && UserRegistry.userCanCreateUsers(u);
                if (!u.isSameOrg(onUser)) {
                    actualResult =
                            actualResult
                                    && UserRegistry.userCreateOtherOrgs(u); // if of different org
                }
                actualResult = actualResult && uLevel.canCreateOrSetLevelTo(onLevel);
                break;
            case "delete": // assume same perms for now (?)
            case "modify":
                {
                    final boolean oldTest = u.isAdminFor(onUser);
                    final boolean newTest =
                            uLevel.canManageSomeUsers()
                                    && uLevel.isManagerFor(
                                            u.getOrganization(), onLevel, onUser.getOrganization());
                    assertEquals(
                            newTest,
                            oldTest,
                            "New(ex) vs old(got) manage test: " + uLevel + "/" + onLevel);
                    actualResult = actualResult && newTest;
                }
                break;
            default:
                fail("Unhandled action: " + action);
        }
        assertEquals(
                allowed,
                actualResult,
                u.org + ":" + uLevel + " " + action + " " + onUser.org + ":" + onLevel);
    }

    private void handleElementApiverify(
            final Map<String, String> attrs, String value, String xpath) {
        // like verify, but via API
        value = value.trim();
        if (value.isEmpty()) value = null;
        UserRegistry.User u = getUserFromAttrs(attrs, "name");
        CLDRLocale locale = CLDRLocale.getInstance(attrs.get("locale"));
        final CookieSession mySession = CookieSession.getTestSession(u);
        ArgsForGet args = new ArgsForGet(locale.getBaseName(), mySession.id);
        args.xpstrid = XPathTable.getStringIDString(xpath);
        // args.getDashboard = false;
        try {
            final RowResponse r =
                    VoteAPIHelper.getRowsResponse(args, CookieSession.sm, locale, mySession, false);
            assertEquals(args.xpstrid, r.xpstrid, "xpstrid");
            assertEquals(1, r.page.rows.size(), "row count");
            final Row firstRow = r.page.rows.values().iterator().next();
            assertEquals(firstRow.xpath, xpath, "rxpath");
            assertEquals(value, firstRow.winningValue, "value for " + args.xpstrid);
        } catch (SurveyException t) {
            assertNull(t, "did not expect an exception");
        }
    }

    private void handleElementVote(
            final STFactory fac,
            final Map<String, String> attrs,
            String value,
            String elem,
            String xpath) {
        UserRegistry.User u = getUserFromAttrs(attrs, "name");
        CLDRLocale locale = CLDRLocale.getInstance(attrs.get("locale"));
        BallotBox<User> box = fac.ballotBoxForLocale(locale);
        value = value.trim();
        boolean needException = getBooleanAttr(attrs, "exception", false);
        if (elem.equals("unvote")) {
            value = null;
        }
        try {
            box.voteForValue(u, xpath, value);
            if (needException) {
                fail(
                        "ERR: path #"
                                + pathCount
                                + ", xpath="
                                + xpath
                                + ", locale="
                                + locale
                                + ": expected exception, didn't get one");
            }
        } catch (InvalidXPathException e) {
            fail("Error: invalid xpath exception " + xpath + " : " + e);
        } catch (VoteNotAcceptedException iae) {
            if (needException == true) {
                System.out.println("Caught expected: " + iae);
            } else {
                iae.printStackTrace();
                fail("Unexpected exception: " + iae);
            }
        }
        System.out.println(u + " " + elem + "d for " + xpath + " = " + value);
    }

    private void handleElementApivote(
            final Map<String, String> attrs, String value, String elem, String xpath) {
        UserRegistry.User u = getUserFromAttrs(attrs, "name");
        CLDRLocale locale = CLDRLocale.getInstance(attrs.get("locale"));
        boolean needException = getBooleanAttr(attrs, "exception", false);
        if (elem.equals("apiunvote")) {
            value = null;
        }
        assertEquals(
                CheckCLDR.Phase.BUILD,
                SurveyMain.checkCLDRPhase(locale),
                () -> "CheckCLDR Phase for " + locale);
        assertEquals(
                CheckCLDR.Phase.BUILD,
                CLDRConfig.getInstance().getPhase(),
                "CLDRConfig.getInstance().getPhase()");
        final CookieSession mySession = CookieSession.getTestSession(u);
        try {
            final VoteAPI.VoteResponse r =
                    VoteAPIHelper.getHandleVoteResponse(
                            locale.getBaseName(), xpath, value, 0, mySession, false);
            final boolean isOk = r.didVote;
            final boolean asExpected = (isOk == !needException);
            if (!asExpected) {
                fail(
                        "exception="
                                + needException
                                + " but got status "
                                + r.didNotSubmit
                                + " - "
                                + r.toString());
            } else {
                System.out.println(" status = " + r.didNotSubmit);
            }
        } catch (Throwable iae) {
            if (needException == true) {
                System.out.println("Caught expected: " + iae);
            } else {
                iae.printStackTrace();
                fail("Phase: " + SurveyMain.checkCLDRPhase(locale));
                fail("in" + attrs + "/" + elem + " / " + xpath + ": Unexpected exception: " + iae);
            }
        }
    }

    private void handleElementSetvar(
            final STFactory fac,
            final Map<String, String> attrs,
            final Map<String, String> vars,
            String xpath) {
        final String id = attrs.get("id");
        final CLDRLocale locale = CLDRLocale.getInstance(attrs.get("locale"));
        final String xvalue = fac.make(locale, true).getStringValue(xpath);
        vars.put(id, xvalue);
        System.out.println("$" + id + " = '" + xvalue + "' from " + locale + ":" + xpath);
    }

    private void handleElementUser(final STFactory fac, final Map<String, String> attrs)
            throws InternalError {
        String name = attrs.get("name");
        String org = attrs.get("org");
        String locales = attrs.get("locales");
        VoteResolver.Level level = VoteResolver.Level.valueOf(attrs.get("level").toLowerCase());
        String email = name + "@" + org + ".example.com";
        UserRegistry.User u = fac.sm.reg.get(email);
        if (u == null) {
            u = fac.sm.reg.createTestUser(name, org, locales, level, email);
        }
        assertNotNull(u, "Couldn't find/register user " + name);
        System.out.println(name + " = " + u);
        users.put(name, u);
    }

    /**
     * If a "verify" element includes "orgStatus" and "statusOrg" attributes, then report an error
     * unless getStatusForOrganization returns the specified status for the specified org
     *
     * @param r the VoteResolver
     * @param attrs the attributes
     */
    private void verifyOrgStatus(VoteResolver<String> r, Map<String, String> attrs) {
        final String expOrgStatus = attrs.get("orgStatus"); // e.g., "ok"
        final String expStatusOrg = attrs.get("statusOrg"); // e.g., "apple"
        if (expOrgStatus != null && expStatusOrg != null) {
            final Organization org = Organization.fromString(expStatusOrg);
            final String actualOrgStatus = r.getStatusForOrganization(org).toString();
            assertEquals(expOrgStatus, actualOrgStatus, "for " + expStatusOrg);
        }
    }

    /**
     * @param attrs
     * @return
     */
    public boolean getBooleanAttr(
            final Map<String, String> attrs, String attr, boolean defaultValue) {
        final String strVal = attrs.get(attr);
        if (strVal == null || strVal.isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(strVal);
    }

    /**
     * @param attrs
     * @param attr
     * @return
     * @throws IllegalArgumentException
     */
    public UserRegistry.User getUserFromAttrs(final Map<String, String> attrs, String attr) {
        final String attrValue = attrs.get(attr);
        if (attrValue == null) {
            return null;
        }
        UserRegistry.User u = users.get(attrValue);
        assertNotNull(
                u,
                "Undeclared user: "
                        + attr
                        + "=\""
                        + attrValue
                        + "\" - are you missing a <user> element?");
        return u;
    }
}
