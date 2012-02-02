	/**
	 * Copyright (C) 2011 IBM Corporation and Others. All Rights Reserved.
	 */
	package org.unicode.cldr.web;
	
	import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.text.StyleContext.SmallAttributeSet;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.VettingViewer.DefaultErrorStatus;
import org.unicode.cldr.util.VettingViewer.ErrorChecker;
import org.unicode.cldr.util.VettingViewer.ErrorChecker.Status;
import org.unicode.cldr.web.CLDRProgressIndicator.CLDRProgressTask;

import com.ibm.icu.dev.test.util.ElapsedTimer;
	
	public class ErrorCheckManager {

//		// TODO: replace with enm, or fix 
		//		public static final int ERR_OK=0;
		//		public static final int ERR_WARN=1;
		//		public static final int ERR_ERR=2;
		//
		//		private int statusToInt(Status s) {
		//			switch(s) {
		//			case ok:
		//				return ERR_OK;
		//			case warning:
		//				return ERR_WARN;
		//			case error:
		//			default:
		//				return ERR_ERR;
		//			}
		//		}
		//
		//		private Status intToStatus(int s) {
		//			switch(s) {
		//			case ERR_OK:
		//				return Status.ok;
		//			case ERR_WARN:
		//				return Status.warning;
		//			case ERR_ERR:
		//			default:
		//				return Status.error;
		//			}
		//		}

		/**
		 * @author srl
		 *
		 */
		public static class CachingErrorChecker implements ErrorChecker {
			private class StatusString {
				Status status;
				String builder;
				StatusString(Status status,StringBuilder builder) {
					this.status = status;
					this.builder = builder.toString();
				}
				Status get(StringBuilder builder) {
					builder.append(this.builder);
					return status;
				}
			}
			DefaultErrorStatus des;
			IntHash<StatusString> hash=null;
			private SurveyMain sm;
			/**
			 * 
			 */
			public CachingErrorChecker(SurveyMain sm) {
				this.sm = sm;
				des = new DefaultErrorStatus(sm.dbsrcfac);
			}

			/* (non-Javadoc)
			 * @see org.unicode.cldr.util.VettingViewer.ErrorChecker#initErrorStatus(org.unicode.cldr.util.CLDRFile)
			 */
			@Override
			public Status initErrorStatus(CLDRFile cldrFile) {
				Status rs = des.initErrorStatus(cldrFile); 
				hash =  new IntHash<StatusString>();
				
				for(String xpath : cldrFile) {
					StringBuilder nullAppendable = new StringBuilder();
					Status s = des.getErrorStatus(xpath, cldrFile.getWinningValue(xpath), nullAppendable);
					hash.put(sm.xpt.getByXpath(xpath), new StatusString(s,nullAppendable));
				}
				return rs;
			}

			/* (non-Javadoc)
			 * @see org.unicode.cldr.util.VettingViewer.ErrorChecker#getErrorStatus(java.lang.String, java.lang.String, java.lang.StringBuilder)
			 */
			@Override
			public Status getErrorStatus(String path, String value,
					StringBuilder statusMessage) {
				StatusString ss = hash.get(sm.xpt.getByXpath(path));
				return ss.get(statusMessage);
			}

		}

		public static String CLDR_ERRORS = "cldr_errors";
	
		private SurveyMain sm;
		
		private final boolean DEBUG=false;
	
		public ErrorCheckManager(SurveyMain sm) {
			this.sm = sm;
			String sql = null;
			
			Connection conn = null;
			
			try {
				try {
					conn = sm.dbUtils.getDBConnection();
					
					if(!DBUtils.hasTable(conn, CLDR_ERRORS)) {
						sm.dbUtils.sqlUpdate(conn, sql= "create table " + CLDR_ERRORS + " ( " +
								"xpath INT not null, " + 
								"locale varchar(30) not null, " +
								"status SMALLINT not null, " +
								"modtime TIMESTAMP not null " +
								" )");
						sm.dbUtils.sqlUpdate(conn, sql="CREATE UNIQUE INDEX unique_err on " + CLDR_ERRORS +"(xpath,locale)");
						System.err.println("Set up table " + CLDR_ERRORS);
					}				
				} finally {
					DBUtils.close(conn);
				}
			} catch(SQLException se) {
				String msg = "Error setting up ErrCheckManager [last sql:"+sql+"]:" + se.toString();
				sm.busted(msg, se);
				throw new RuntimeException(msg,se);
			}
		}
		
		/**
		 * Check if there's an update needed. 
		 * @param loc
		 * @param doUpdate
		 * @return inthash if one had to be updated
		 */
		public IntHash<Status> checkLocaleStatus(CLDRLocale loc, boolean doUpdate) {
			String locStr = loc.getBaseName();
			CLDRProgressTask progress = sm.openProgress("Checking status of "+loc);
			Connection conn=null;
			try {
				conn = sm.dbUtils.getDBConnection();
				try {
					String retAmt = sm.dbUtils.sqlQuery(conn, "select count(*) from cldr_errors where cldr_errors.locale=? and cldr_errors.xpath="+XPathTable.NO_XPATH+" and "+
							" not exists  ( select * from cldr_result where cldr_result.locale=cldr_errors.locale and cldr_result.modtime > cldr_errors.modtime  );", locStr);
					if(DEBUG) System.err.println("Ret: " + retAmt);
					if(retAmt==null||retAmt.equals("0")) {
						if(DEBUG) System.err.println("Must refresh: " + locStr);
						
						if(doUpdate) {
							return doUpdate(conn,loc);
						} else {
							return new IntHash<Status>();
						}
					}
				} finally {
					DBUtils.close(conn);
				}
			} catch(SQLException se) {
				String msg = "Error checking locale [:"+loc+"]:" + DBUtils.unchainSqlException(se);
				System.err.println(msg);
	//			sm.busted(msg, se);
				throw new RuntimeException(msg,se);
			} finally {
				if(progress!=null) progress.close();
			}
			return null;
		}
	
		/**
		 * Do an update.
		 * @param conn
		 * @param locStr
		 * @throws SQLException 
		 */
		private IntHash<Status> doUpdate(Connection conn, CLDRLocale loc) throws SQLException {
			String locStr=loc.getBaseName();
			synchronized(ErrorCheckManager.class) {
				sm.dbUtils.sqlUpdate(conn, "delete from " + CLDR_ERRORS + " where locale=?", loc);
				PreparedStatement ps = sm.dbUtils.prepareStatementWithArgs(conn, "insert into "+CLDR_ERRORS+" (locale,xpath,status,modtime) VALUES (?,?,?,CURRENT_TIMESTAMP)",locStr);
				try {
					IntHash<Status> hash = new IntHash<Status>();

					DefaultErrorStatus des = new DefaultErrorStatus(sm.dbsrcfac);
					// TODO: DBSRC entry
					CLDRFile cldrFile = sm.dbsrcfac.make(loc.toString(), true);
					des.initErrorStatus(cldrFile);
					
	
					StringBuilder nullAppendable = new StringBuilder();
					for(String xpath : cldrFile) {
						Status s = des.getErrorStatus(xpath, cldrFile.getWinningValue(xpath), nullAppendable);
						nullAppendable.delete(0, nullAppendable.length());
						
						int id = sm.xpt.getByXpath(xpath);
						int val = s.ordinal();
						
						ps.setInt(2, id);
						ps.setInt(3, val);
						ps.executeUpdate();
						
						hash.put(id, s);
					}
					
					Status overall = Status.ok;
					ps.setInt(2,XPathTable.NO_XPATH);
					ps.setInt(3,overall.ordinal());
					
					//hash.put(XPathTable.NO_XPATH, overall);
					
					ps.executeUpdate();
					conn.commit();
					
					return hash;
					
				} finally {
					DBUtils.close(ps);
				}
			}
		}
	
		
		public void invalidate(CLDRLocale locale) {
			CLDRLocale loc = locale;
			synchronized(ErrorCheckManager.class) {
				Connection conn=null;
				PreparedStatement ps=null;
				try{
					try {
						conn = sm.dbUtils.getDBConnection();
						ps = sm.dbUtils.prepareStatementWithArgs(conn, "delete from cldr_errors where locale=?");						
						while(loc!=null) {
							ps.setString(1, loc.toString());
							ps.executeUpdate();
							if(DEBUG) System.err.println("ERRCHK: invalidated " + loc);
							loc = loc.getParent();
						}
						conn.commit();
					} finally {
						DBUtils.close(ps,conn);
					}
				}catch(SQLException se) {
					String msg = "Error invalidating locale :"+locale+"]:" + se.toString();
					//					sm.busted(msg, se);
					throw new RuntimeException(msg,se);
				}
			}
		}
	
		public ErrorChecker getErrorChecker() {
			if(DEBUG) System.err.println("getErrorChecker");
			return new ErrorChecker(){
				//DefaultErrorStatus dec = new DefaultErrorStatus();
				IntHash<Status> statusHash = null;
				CLDRLocale loc;
				@Override
				public Status initErrorStatus(CLDRFile cldrFile) {
					CLDRLocale loc = CLDRLocale.getInstance(cldrFile.getLocaleID());
					if(DEBUG) System.err.println("getErrorChecker- initerrstatus");
					synchronized(ErrorCheckManager.class) { /* sync - for update*/
						ElapsedTimer et2 = new ElapsedTimer("Checking status:"+cldrFile.getLocaleID());
						statusHash = checkLocaleStatus(loc,true);
						if(DEBUG||true) System.err.println(et2 + " update:"+(statusHash==null));
						if(statusHash==null) {	
							statusHash=getStatusHash(loc);
						}
					}
	//				ElapsedTimer et = new ElapsedTimer("Initting DEC:"+cldrFile.getLocaleID());
	//				try {
	//					if(dec!=null) {
	//						return dec.initErrorStatus(cldrFile);
	//					}
	//				} finally {
	//					System.err.println("EC: " + et);
	//				}
					return Status.ok; // statusHash.get(XPathTable.NO_XPATH);
				}
	
				@Override
				public Status getErrorStatus(String path, String value,
						StringBuilder statusMessage) {
	//					if(dec!=null) {
	//						return dec.getErrorStatus(path, value, statusMessage);
	//					}
	//					return Status.ok;
					return statusHash.get(sm.xpt.getByXpath(path));
				}};
		}
	
		protected IntHash<Status> getStatusHash(CLDRLocale loc) {
			String locStr=loc.getBaseName();
			synchronized(ErrorCheckManager.class) {
				// TODO Auto-generated method stub
				IntHash<Status> hash = new IntHash<Status>();

				Connection conn=null;
				PreparedStatement ps=null;
				ResultSet rs = null;
				Status[] statuses = Status.values();
				try{
					try {
						conn = sm.dbUtils.getDBConnection();
						ps = sm.dbUtils.prepareStatementWithArgs(conn, "select xpath,status from cldr_errors where locale=?", locStr);
						rs = ps.executeQuery();
						while(rs.next()) {
							int id = rs.getInt(1);
							if(id!=XPathTable.NO_XPATH) {
								hash.put(id, statuses[rs.getInt(2)]);
							}
						}
					} finally {
						DBUtils.close(rs,ps,conn);
					}
				}catch(SQLException se) {
					String msg = "Error getting status hash  for :"+loc+"]:" + se.toString();
					//					sm.busted(msg, se);
					throw new RuntimeException(msg,se);
				}


				return hash;
			}
		}
	
	}
