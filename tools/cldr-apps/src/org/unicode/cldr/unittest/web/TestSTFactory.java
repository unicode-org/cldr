package org.unicode.cldr.unittest.web;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.unicode.cldr.unittest.web.TestAll.WebTestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.web.BallotBox;
import org.unicode.cldr.web.CLDRDBSourceFactory;
import org.unicode.cldr.web.DBUtils;
import org.unicode.cldr.web.STFactory;
import org.unicode.cldr.web.SurveyLog;
import org.unicode.cldr.web.SurveyMain;
import org.unicode.cldr.web.UserRegistry;
import org.unicode.cldr.web.UserRegistry.User;
import org.unicode.cldr.web.Vetting;
import org.unicode.cldr.web.XPathTable;

import com.ibm.icu.dev.test.TestFmwk;

public class TestSTFactory extends TestFmwk {
	
	private static final String CACHETEST = "cachetest";

	TestAll.WebTestInfo testInfo = WebTestInfo.getInstance();
	
	STFactory gFac = null;
	UserRegistry.User gUser = null;
	
	public static void main(String[] args) {
		new TestSTFactory().run(TestAll.doResetDb(args));
	}

	public void TestReadonlyLocales() throws SQLException {
		logln("Setting up factory..");
		STFactory fac = getFactory();
		
		verifyReadOnly(fac.make("root",false));
		verifyReadOnly(fac.make("en",false));
		
		logln("Test done.");
	}
	
	private static final String ANY = "*";
	
	
	private String expect(String path,
										String expectString,
										boolean expectVoted,
										
										CLDRLocale locale,
										CLDRFile file,
										BallotBox<User> box) {
		String currentWinner = file.getStringValue(path);
		boolean didVote = box.userDidVote(getMyUser(),path);
		StackTraceElement them =  Thread.currentThread().getStackTrace()[3];
		String where = them.getFileName()+":"+them.getLineNumber()+": ";
				
		if(expectString!=ANY && !expectString.equals(currentWinner)) {
			errln ("ERR:" + where+"Expected '"+expectString+"': " + locale+":"+path+" ='"+currentWinner+"', " +   votedToString(didVote) + box.getResolver(path));
		} else if(expectVoted!=didVote) {
			errln ("ERR:" + where+"Expected VOTING="+votedToString(expectVoted)+":  " + locale+":"+path+" ='"+currentWinner+"', " +   votedToString(didVote) + box.getResolver(path));
		} else {
			logln(where+locale+":"+path+" ='"+currentWinner+"', " +   votedToString(didVote) + box.getResolver(path));
		}
		return currentWinner;
	}

	/**
	 * @param didVote
	 * @return
	 */
	private String votedToString(boolean didVote) {
		return didVote?"(I VOTED)":"( did NOT VOTE) ";
	}
	
	public void TestVoteBasic() throws SQLException, IOException {
		logln("Setting up factory..");
		STFactory fac = getFactory();
		
		final String somePath =  "//ldml/localeDisplayNames/keys/key[@type=\"collation\"]";
		String originalValue = null;
		String changedTo = null;
		
		CLDRLocale locale = CLDRLocale.getInstance("mt");
		{
			CLDRFile mt = fac.make(locale, false);
			BallotBox<User> box = fac.ballotBoxForLocale(locale);
			
			originalValue  = expect(somePath,ANY,false,
					locale,mt,box);
			
			changedTo = "COLL_ATION!!!";
			
			if(originalValue.equals(changedTo)) {
				errln("for " + locale + " value " + somePath + " winner is already= " + originalValue);
			}

			box.voteForValue(getMyUser(), somePath, changedTo);
			
			expect(somePath,changedTo,true,
					locale,mt,box);
		}
		
		// Restart STFactory.
		fac = resetFactory();
		{
			CLDRFile mt = fac.make(locale, false);
			BallotBox<User> box = fac.ballotBoxForLocale(locale);

			expect(somePath,changedTo,true,
					locale,mt,box);
			
			// unvote
			box.voteForValue(getMyUser(), somePath, null);

			expect(somePath,originalValue,true,
					locale,mt,box);
		}
		fac = resetFactory();
		{
			CLDRFile mt = fac.make(locale, false);
			BallotBox<User> box = fac.ballotBoxForLocale(locale);


			expect(somePath,originalValue,true,
					locale,mt,box);

			// vote for ____2
			changedTo = changedTo+"2";
			
			logln("VoteFor: " + changedTo);
			box.voteForValue(getMyUser(), somePath, changedTo);

			expect(somePath,changedTo,true,
					locale,mt,box);
			
			logln("Write out..");
			File targDir = TestAll.getEmptyDir(TestSTFactory.class.getName()+"_output");
			File outFile = new File(targDir,locale.getBaseName()+".xml");
			FileOutputStream fos = new FileOutputStream(outFile);
			PrintWriter pw = new PrintWriter(fos);
			mt.write(pw,noDtdPlease);
			pw.close();
			
			logln("Read back..");
			CLDRFile readBack = CLDRFile.loadFromFile(outFile, locale.getBaseName(), DraftStatus.unconfirmed);
			
			String reRead = readBack.getStringValue(somePath);
			
			logln("reread:  " + outFile.getAbsolutePath()+ " value " + somePath + " = " + reRead);
			if(!changedTo.equals(reRead)) {
				logln("reread:  " + outFile.getAbsolutePath()+ " value " + somePath + " = " + reRead + ", should be " + changedTo);
			}
		}
		
		
		
		
		logln("Test done.");
	}
	
	
	
	private void verifyReadOnly(CLDRFile f) {
		String loc = f.getLocaleID();
		try {
			f.add("//ldml/foo", "bar");
			errln("Error: " +  loc + " is supposed to be readonly.");
		} catch(Throwable t) {
			logln("Pass: " +  loc + " is readonly, caught " + t.toString());
		}
	}
	String someLocales[] = { "mt" };

	public UserRegistry.User getMyUser()  {
		if(gUser ==null) {
			try {
				gUser = getFactory().sm.reg.get(null,"admin@","[::1]",true);
			} catch (SQLException e) {
				handleException(e);
			}
		}
		return gUser;
	}
	
	private STFactory getFactory() throws SQLException {
		if(gFac==null) {
			TestAll.setupTestDb();

			File cacheDir = TestAll.getEmptyDir(CACHETEST);
			logln("Setting up STFactory");
			SurveyMain sm = new SurveyMain();
			sm.setFileBaseOld(CldrUtility.BASE_DIRECTORY);
			sm.twidPut(Vetting.TWID_VET_VERBOSE, true); // set verbose vetting
			SurveyLog.logger = Logger.getAnonymousLogger();
			Connection conn = DBUtils.getInstance().getDBConnection();
			
			sm.reg = UserRegistry.createRegistry(SurveyLog.logger, sm);
			
			sm.xpt = XPathTable.createTable(conn, sm);
			DBUtils.closeDBConnection(conn);
			//			sm.vet = Vetting.createTable(sm.logger, sm);
			
			sm.fileBase = CldrUtility.MAIN_DIRECTORY;
			sm.fileBaseSeed = new File(CldrUtility.BASE_DIRECTORY,"seed/main/").getAbsolutePath();
//			CLDRDBSourceFactory fac = new CLDRDBSourceFactory(sm, sm.fileBase, Logger.getAnonymousLogger(), cacheDir);
//			logln("Setting up DB");
//			sm.setDBSourceFactory(fac);ignore
//			fac.setupDB(DBUtils.getInstance().getDBConnection());
//			logln("Vetter Ready (this will take a while..)");
//			fac.vetterReady(TestAll.getProgressIndicator(this));
			
			gFac = sm.getSTFactory();
		}
		return gFac;
	}
	
	private STFactory resetFactory() throws SQLException {
		logln("--- resetting STFactory() ----- [simulate reload] ------------");
		return gFac = getFactory().TESTING_shutdownAndRestart();
	}

	
	static final Map<String,Object> noDtdPlease = new TreeMap<String,Object>();
	static {
		noDtdPlease.put("DTD_DIR", CldrUtility.COMMON_DIRECTORY+File.separator+"dtd" + File.separator);
	}
}
