/**
 * Copyright (C) 2012
 */
package org.unicode.cldr.unittest.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;

import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.DBUtils;
import org.unicode.cldr.web.SurveyMain;
import org.unicode.cldr.web.XPathTable;

import com.ibm.icu.dev.test.TestFmwk;

/**
 * @author srl
 *
 */
public class TestXPathTable extends TestFmwk {
	public static void main(String[] args) {
		new TestXPathTable().run(args);
	}

	public TestXPathTable() {
		TestAll.setupTestDb();
	}


	public static final int TEST_COUNT=200;

	public void TestPutGet() throws SQLException {
		logln("Testing " + TEST_COUNT + " xpaths");
		Connection conn = DBUtils.getInstance().getDBConnection();
		XPathTable xpt = XPathTable.createTable(conn, new SurveyMain());
		DBUtils.closeDBConnection(conn);
		HashMap<Integer,String> s = new HashMap<Integer,String>();
		for(int i=0;i<TEST_COUNT;i++) {
			int n = i;
			String str = "//test/"+n+"/[@hash=\""+CookieSession.cheapEncode(i)+"\"]/item";
			int xpid = xpt.getByXpath(str);
			if(s.containsKey(xpid)) {
				errln("Error: Duplicate id " + xpid + " for " + str + " and " + s.get(xpid)+"\n");
			}
			//logln("#"+i+" - xpath " + str + " id " + xpid);
			s.put(xpid,str);
		}

		int ii=0;
		for(int n : s.keySet()) {
			++ii;
			String expect = s.get(n);
			String xpath = xpt.getById(n);
			int expectId = xpt.getByXpath(xpath);
			if(expectId != n) {
				errln("Error: id is " + n + " expected " + expectId);
			}else if(!xpath.equals(expect)) {
				errln("Error: xpath is " + xpath + " expected " + expect);
			}else {
				//logln("XP#"+n+" -> " + xpath);
			}
		}
		logln("OK: Tested "+ii+" values");
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
	    
	    for(int i=0;i<inout.length;i+=2) {
	        final String in = inout[i+0];
	        final String out = inout[i+1];
	        final String got = XPathTable.removeDraftAltProposed(in);
	        
	        logln("-- case #" + i/2);
	        logln("<< "+ in);
	        logln(">> "+ got);
	        if(!got.equals(out)) {
	            logln("!= " + out);
	            errln(in + " to " + got + " , expected " + out);
	        }
	    }
	}
}
