/** Copyright (C) 2012 */
package org.unicode.cldr.unittest.web;

import com.ibm.icu.dev.test.TestFmwk;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.DBUtils;
import org.unicode.cldr.web.XPathTable;

/**
 * @author srl
 */
public class TestXPathTable extends TestFmwk {
    public static void main(String[] args) {
        new TestXPathTable().run(args);
    }

    public TestXPathTable() {
        if (TestAll.skipIfNoDb()) return;
        TestAll.setupTestDb();
    }

    public static final int TEST_COUNT = 200;

    public void TestPutGet() throws SQLException {
        if (TestAll.skipIfNoDb()) return;
        logln("Testing " + TEST_COUNT + " xpaths");
        Connection conn = DBUtils.getInstance().getAConnection();
        XPathTable xpt = XPathTable.createTable(conn);
        DBUtils.closeDBConnection(conn);
        HashMap<Integer, String> s = new HashMap<>();
        for (int i = 0; i < TEST_COUNT; i++) {
            int n = i;
            String str = "//test/" + n + "/[@hash=\"" + CookieSession.cheapEncode(i) + "\"]/item";
            int xpid = xpt.getByXpath(str);
            if (s.containsKey(xpid)) {
                errln("Error: Duplicate id " + xpid + " for " + str + " and " + s.get(xpid) + "\n");
            }
            // logln("#"+i+" - xpath " + str + " id " + xpid);
            s.put(xpid, str);
        }

        int ii = 0;
        for (int n : s.keySet()) {
            ++ii;
            String expect = s.get(n);
            String xpath = xpt.getById(n);
            int expectId = xpt.getByXpath(xpath);
            if (expectId != n) {
                errln("Error: id is " + n + " expected " + expectId);
            } else if (!xpath.equals(expect)) {
                errln("Error: xpath is " + xpath + " expected " + expect);
            } else {
                // logln("XP#"+n+" -> " + xpath);
            }
            String xpathString = xpt.getStringIDString(xpath);
            assertNotNull("hex for " + xpath, xpathString);
            String xpathFromString = xpt.getByStringID(xpathString);
            assertEquals("for hex " + xpathString, xpath, xpathFromString);
        }
        logln("OK: Tested " + ii + " values");
        assertNull(
                "for hex 'Not Really Hex'",
                xpt.getByStringID("Not Really Hex")); // null, parse failure
    }

    public void TestRemoveDraftAltProposed() {
        String inout[] = {
            "//ldml/foo/bar[@draft=\"true\"]",
            "//ldml/foo/bar",
            "//ldml/foo/bar[@alt=\"variant\"]",
            "//ldml/foo/bar[@alt=\"variant\"]",
            "//ldml/foo/bar[@alt=\"variant\"][@draft=\"true\"]",
            "//ldml/foo/bar[@alt=\"variant\"]",
            "//ldml/foo/bar[@alt=\"proposed-x222\"]",
            "//ldml/foo/bar",
            "//ldml/foo/bar[@alt=\"proposed-x222\"][@draft=\"true\"]",
            "//ldml/foo/bar",
            "//ldml/foo/bar[@alt=\"variant-proposed-x333\"]",
            "//ldml/foo/bar[@alt=\"variant\"]",
            "//ldml/foo/bar[@draft=\"true\"][@alt=\"variant-proposed-x333\"]",
            "//ldml/foo/bar[@alt=\"variant\"]",
            "//ldml/dates/calendars/calendar[@type=\"chinese\"]/dateFormats/dateFormatLength[@type=\"medium\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"][@numbers=\"hanidec\"]",
            "//ldml/dates/calendars/calendar[@type=\"chinese\"]/dateFormats/dateFormatLength[@type=\"medium\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"][@numbers=\"hanidec\"]",
            "//ldml/dates/calendars/calendar[@type=\"chinese\"]/dateFormats/dateFormatLength[@type=\"medium\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"][@numbers=\"hanidec\"][@draft=\"true\"]",
            "//ldml/dates/calendars/calendar[@type=\"chinese\"]/dateFormats/dateFormatLength[@type=\"medium\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"][@numbers=\"hanidec\"]",
            "//ldml/dates/calendars/calendar[@type=\"chinese\"]/dateFormats/dateFormatLength[@type=\"medium\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"][@alt=\"proposedx333\"][@numbers=\"hanidec\"][@draft=\"true\"]",
            "//ldml/dates/calendars/calendar[@type=\"chinese\"]/dateFormats/dateFormatLength[@type=\"medium\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"][@numbers=\"hanidec\"]",
            "//ldml/dates/calendars/calendar[@type=\"chinese\"]/dateFormats/dateFormatLength[@type=\"medium\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"][@alt=\"variant-proposedx333\"][@numbers=\"hanidec\"][@draft=\"true\"]",
            "//ldml/dates/calendars/calendar[@type=\"chinese\"]/dateFormats/dateFormatLength[@type=\"medium\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"][@numbers=\"hanidec\"][@alt=\"variant\"]",
        };

        for (int i = 0; i < inout.length; i += 2) {
            final String in = inout[i + 0];
            final String out = inout[i + 1];
            final String got = XPathTable.removeDraftAltProposed(in);

            logln("-- case #" + i / 2);
            logln("<< " + in);
            logln(">> " + got);
            if (!got.equals(out)) {
                logln("!= " + out);
                errln(in + " to " + got + " , expected " + out);
            }
        }
    }

    public void TestNonDistinguishing() throws SQLException {
        if (TestAll.skipIfNoDb()) return;
        Connection conn = DBUtils.getInstance().getAConnection();
        XPathTable xpt = XPathTable.createTable(conn);
        DBUtils.closeDBConnection(conn);

        String xpaths[] = {
            "//ldml/characters/moreInformation[@draft=\"true\"]",
            "",
            "//ldml/characters/moreInformation[@alt=\"variant\"]",
            "",
            "//ldml/dates/calendars/calendar[@type=\"chinese\"]/dateFormats/dateFormatLength[@type=\"medium\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"][@numbers=\"hanidec\"]",
            "numbers=hanidec",
            "//ldml/dates/calendars/calendar[@type=\"chinese\"]/dateFormats/dateFormatLength[@type=\"medium\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"][@numbers=\"hanidec\"][@draft=\"true\"]",
            "numbers=hanidec",
            "//ldml/dates/calendars/calendar[@type=\"hebrew\"]/dateFormats/dateFormatLength[@type=\"medium\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"][@alt=\"proposedx333\"][@numbers=\"hebrew\"][@draft=\"true\"]",
            "numbers=hebrew",
            "//ldml/dates/calendars/calendar[@type=\"chinese\"]/dateFormats/dateFormatLength[@type=\"medium\"]/dateFormat[@type=\"standard\"]/pattern[@type=\"standard\"][@alt=\"variant-proposedx333\"][@numbers=\"hanidec\"][@draft=\"true\"]",
            "numbers=hanidec",
            "//ldml/personNames/foreignSpaceReplacement[@xml:space=\"preserve\"]",
            "",
        };

        for (int i = 0; i < xpaths.length; i += 2) {
            String xpath = xpaths[i + 0];
            String expect = xpaths[i + 1];
            Map<String, String> ueMap =
                    xpt.getUndistinguishingElementsFor(
                            xpath); // just calls XPathParts.getSpecialNondistinguishingAttributes()
            if (ueMap != null) {
                logln(xpath + "\n -> " + ueMap.toString() + " expect " + expect);
            } else {
                logln(xpath + "\n -> 0, expect" + expect);
            }
            if (expect.isEmpty()) {
                if (ueMap != null && !ueMap.isEmpty()) {
                    errln(
                            "Error for xpath "
                                    + xpath
                                    + " expected nondistinguishing =EMPTY got "
                                    + ueMap.toString());
                }
            } else {
                if (ueMap == null || ueMap.isEmpty()) {
                    errln(
                            "Error for xpath "
                                    + xpath
                                    + " expected nondistinguishing ="
                                    + expect
                                    + " got "
                                    + ueMap.toString());
                } else {
                    Map<String, String> mymap = new TreeMap<>();
                    Map<String, String> uemap2 = new TreeMap<>(ueMap);

                    for (String pair : expect.split(",")) {
                        String kv[] = pair.split("=");
                        mymap.put(kv[0], kv[1]);
                    }
                    if (!mymap.equals(uemap2)) {
                        errln("Error for " + xpath + "\n Got " + uemap2 + " expected " + mymap);
                    } else {
                        logln("PASS for " + xpath + "\n Got " + uemap2 + " expected " + mymap);
                    }
                }
            }
        }
    }
}
