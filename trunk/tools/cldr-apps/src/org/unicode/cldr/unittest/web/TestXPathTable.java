/**
 * 
 */
package org.unicode.cldr.unittest.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.unicode.cldr.unittest.TestPaths;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.DBUtils;
import org.unicode.cldr.web.IntHash;
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
		Connection conn = DBUtils.getInstance().getDBConnection();
		XPathTable xpt = XPathTable.createTable(null, conn, new SurveyMain());
		DBUtils.closeDBConnection(conn);
		HashMap<Integer,String> s = new HashMap<Integer,String>();
		for(int i=0;i<TEST_COUNT;i++) {
			int n = i;
			String str = "//test/"+n+"/[@hash=\""+CookieSession.cheapEncode(i)+"\"]/item";
			int xpid = xpt.getByXpath(str);
			if(s.containsKey(xpid)) {
				errln("Error: Duplicate id " + xpid + " for " + str + " and " + s.get(xpid)+"\n");
			}
			logln("#"+i+" - xpath " + str + " id " + xpid);
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
				log("XP#"+n+" -> " + xpath);
			}
		}
		log("OK: Tested "+ii+" values\n");
	}
}
