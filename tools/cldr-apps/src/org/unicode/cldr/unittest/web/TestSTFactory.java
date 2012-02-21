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
import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.ElapsedTimer;

public class TestSTFactory extends TestFmwk {
	
	private static final String CACHETEST = "cachetest";

	TestAll.WebTestInfo testInfo = WebTestInfo.getInstance();
	
	STFactory gFac = null;
	UserRegistry.User gUser = null;
	
	public static void main(String[] args) {
		new TestSTFactory().run(TestAll.doResetDb(args));
	}
	public void TestFactory() throws SQLException {
		STFactory fac = getFactory();
	}
		
	public void TestReadonlyLocales() throws SQLException {
		STFactory fac = getFactory();
		
		verifyReadOnly(fac.make("root",false));
		verifyReadOnly(fac.make("en",false));
	}
	
	private static final String ANY = "*";
	private static final String NULL = "<NULL>";
	
	private String expect(String path,
										String expectString,
										boolean expectVoted,
										CLDRFile file,
										BallotBox<User> box) {
		CLDRLocale locale = CLDRLocale.getInstance(file.getLocaleID());
		String currentWinner = file.getStringValue(path);
		boolean didVote = box.userDidVote(getMyUser(),path);
		StackTraceElement them =  Thread.currentThread().getStackTrace()[3];
		String where = them.getFileName()+":"+them.getLineNumber()+": ";
				
		if(expectString==null) expectString = NULL;
		if(currentWinner==null) currentWinner=NULL;
		
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
		STFactory fac = getFactory();
		
		final String somePath =  "//ldml/localeDisplayNames/keys/key[@type=\"collation\"]";
		String originalValue = null;
		String changedTo = null;
		
		CLDRLocale locale = CLDRLocale.getInstance("mt");
		{
			CLDRFile mt = fac.make(locale, false);
			BallotBox<User> box = fac.ballotBoxForLocale(locale);
			
			originalValue  = expect(somePath,ANY,false,
					mt,box);
			
			changedTo = "COLL_ATION!!!";
			
			if(originalValue.equals(changedTo)) {
				errln("for " + locale + " value " + somePath + " winner is already= " + originalValue);
			}

			box.voteForValue(getMyUser(), somePath, changedTo);
			
			expect(somePath,changedTo,true,
					mt,box);
		}
		
		// Restart STFactory.
		fac = resetFactory();
		{
			CLDRFile mt = fac.make(locale, false);
			BallotBox<User> box = fac.ballotBoxForLocale(locale);

			expect(somePath,changedTo,true,
					mt,box);
			
			// unvote
			box.voteForValue(getMyUser(), somePath, null);

			expect(somePath,originalValue,true,
					mt,box);
		}
		fac = resetFactory();
		{
			CLDRFile mt = fac.make(locale, false);
			BallotBox<User> box = fac.ballotBoxForLocale(locale);


			expect(somePath,originalValue,true,
					mt,box);

			// vote for ____2
			changedTo = changedTo+"2";
			
			logln("VoteFor: " + changedTo);
			box.voteForValue(getMyUser(), somePath, changedTo);

			expect(somePath,changedTo,true,
					mt,box);
			
			logln("Write out..");
			File targDir = TestAll.getEmptyDir(TestSTFactory.class.getName()+"_output");
			File outFile = new File(targDir,locale.getBaseName()+".xml");
			PrintWriter pw = BagFormatter.openUTF8Writer(targDir.getAbsolutePath(), locale.getBaseName()+".xml");
			mt.write(pw,noDtdPlease);
			pw.close();
			
			logln("Read back..");
			CLDRFile readBack = null;
			try {
			    readBack = CLDRFile.loadFromFile(outFile, locale.getBaseName(), DraftStatus.unconfirmed);
			} catch (IllegalArgumentException iae) {
			    iae.getCause().printStackTrace();
			    System.err.println(iae.getCause().toString());
			    handleException(iae);
			}
			String reRead = readBack.getStringValue(somePath);
			
			logln("reread:  " + outFile.getAbsolutePath()+ " value " + somePath + " = " + reRead);
			if(!changedTo.equals(reRead)) {
				logln("reread:  " + outFile.getAbsolutePath()+ " value " + somePath + " = " + reRead + ", should be " + changedTo);
			}
		}
	}
	
	
	public void TestVoteSparse() throws SQLException, IOException {
		STFactory fac = getFactory();

		final String somePath2 =  "//ldml/localeDisplayNames/keys/key[@type=\"calendar\"]";
		String originalValue2 = null;
		String changedTo2 = null;
		CLDRLocale locale2 = CLDRLocale.getInstance("mt_MT");
		// test sparsity
		{
			CLDRFile mt_MT = fac.make(locale2, false);
			BallotBox<User> box = fac.ballotBoxForLocale(locale2);
			
			originalValue2  = expect(somePath2,null,false,
					mt_MT,box);
			
			changedTo2= "CAL_ENDA!!!";
			
			if(originalValue2.equals(changedTo2)) {
				errln("for " + locale2 + " value " + somePath2 + " winner is already= " + originalValue2);
			}

			box.voteForValue(getMyUser(), somePath2, changedTo2);
			
			expect(somePath2,changedTo2,true,
					mt_MT,box);
		}
		// Restart STFactory.
		fac = resetFactory();
		{
			CLDRFile mt_MT = fac.make(locale2, false);
			BallotBox<User> box = fac.ballotBoxForLocale(locale2);

			expect(somePath2,changedTo2,true,
					mt_MT,box);
			
			// unvote
			box.voteForValue(getMyUser(), somePath2, null);

			expect(somePath2,null,true, mt_MT,box); // Expect null - no one has voted.
		}
		fac = resetFactory();
		{
			CLDRFile mt_MT = fac.make(locale2, false);
			BallotBox<User> box = fac.ballotBoxForLocale(locale2);


			expect(somePath2,null,true,mt_MT,box);

			// vote for ____2
			changedTo2 = changedTo2+"2";
			
			logln("VoteFor: " + changedTo2);
			box.voteForValue(getMyUser(), somePath2, changedTo2);

			expect(somePath2,changedTo2,true,
					mt_MT,box);
			
			logln("Write out..");
			File targDir = TestAll.getEmptyDir(TestSTFactory.class.getName()+"_output");
			File outFile = new File(targDir,locale2.getBaseName()+".xml");
            PrintWriter pw = BagFormatter.openUTF8Writer(targDir.getAbsolutePath(), locale2.getBaseName()+".xml");
			mt_MT.write(pw,noDtdPlease);
			pw.close();
			
			logln("Read back..");
			CLDRFile readBack = CLDRFile.loadFromFile(outFile, locale2.getBaseName(), DraftStatus.unconfirmed);
			
			String reRead = readBack.getStringValue(somePath2);
			
			logln("reread:  " + outFile.getAbsolutePath()+ " value " + somePath2+ " = " + reRead);
			if(!changedTo2.equals(reRead)) {
				logln("reread:  " + outFile.getAbsolutePath()+ " value " + somePath2 + " = " + reRead + ", should be " + changedTo2);
			}
		}
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
			long start = System.currentTimeMillis();
			TestAll.setupTestDb();
			logln("Set up test DB: " + ElapsedTimer.elapsedTime(start));
			
			ElapsedTimer et0=new ElapsedTimer("clearing directory");
			File cacheDir = TestAll.getEmptyDir(CACHETEST);
			logln(et0.toString());
			
			et0=new ElapsedTimer("setup SurveyMain");
			SurveyMain sm = new SurveyMain();
			logln(et0.toString());
			
			sm.fileBase = CldrUtility.MAIN_DIRECTORY;
			sm.fileBaseSeed = new File(CldrUtility.BASE_DIRECTORY,"seed/main/").getAbsolutePath();
			sm.setFileBaseOld(CldrUtility.BASE_DIRECTORY);
			sm.twidPut(Vetting.TWID_VET_VERBOSE, true); // set verbose vetting
			SurveyLog.logger = Logger.getAnonymousLogger();

			et0=new ElapsedTimer("setup DB");
			Connection conn = DBUtils.getInstance().getDBConnection();
			logln(et0.toString());
			
			et0=new ElapsedTimer("setup Registry");
			sm.reg = UserRegistry.createRegistry(SurveyLog.logger, sm);
			logln(et0.toString());
			
			et0=new ElapsedTimer("setup XPT");
			sm.xpt = XPathTable.createTable(conn, sm);
			logln(et0.toString());
			DBUtils.closeDBConnection(conn);
			//			sm.vet = Vetting.createTable(sm.logger, sm);
			
//			CLDRDBSourceFactory fac = new CLDRDBSourceFactory(sm, sm.fileBase, Logger.getAnonymousLogger(), cacheDir);
//			logln("Setting up DB");
//			sm.setDBSourceFactory(fac);ignore
//			fac.setupDB(DBUtils.getInstance().getDBConnection());
//			logln("Vetter Ready (this will take a while..)");
//			fac.vetterReady(TestAll.getProgressIndicator(this));
			
			et0 = new ElapsedTimer("Set up STFactory");
			gFac = sm.getSTFactory();
			logln(et0.toString());
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
