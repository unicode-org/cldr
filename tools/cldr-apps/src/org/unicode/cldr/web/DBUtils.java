/*
 * Copyright (C) 2004-2011 IBM Corporation and Others. All Rights Reserved.
 */
package org.unicode.cldr.web;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.tomcat.dbcp.dbcp.BasicDataSource;

/**
 * All of the database related stuff has been moved here.
 * 
 * @author srl
 *
 */
public class DBUtils {
	private static DBUtils instance = null;
	private static final String JDBC_SURVEYTOOL = ("jdbc/SurveyTool");
	private static DataSource datasource = null;
	// DB stuff
	public static String db_driver = null;
	public static String db_protocol = null;
	public static String CLDR_DB_U = null;
	public static String CLDR_DB_P = null;
	public static String cldrdb_u = null;
	public static String CLDR_DB;
	public static String cldrdb = null;
	public static String CLDR_DB_CREATESUFFIX = null;
	public static String CLDR_DB_SHUTDOWNSUFFIX = null;
	public static boolean db_Derby = false;
	public static boolean db_Mysql = false;
	// === DB workarounds :(
	public static String DB_SQL_IDENTITY = "GENERATED ALWAYS AS IDENTITY";
	public static String DB_SQL_VARCHARXPATH = "varchar(1024)";
	public static String DB_SQL_WITHDEFAULT = "WITH DEFAULT";
	public static String DB_SQL_TIMESTAMP0 = "TIMESTAMP";
	public static String DB_SQL_CURRENT_TIMESTAMP0 = "CURRENT_TIMESTAMP";
	public static String DB_SQL_MIDTEXT = "VARCHAR(1024)";
	public static String DB_SQL_BIGTEXT = "VARCHAR(16384)";
	public static String DB_SQL_UNICODE = "VARCHAR(16384)"; // unicode type
															// string
	public static String DB_SQL_ALLTABLES = "select tablename from SYS.SYSTABLES where tabletype='T'";
	public static String DB_SQL_BINCOLLATE = "";
	public static String DB_SQL_BINTRODUCER = "";
	static int db_number_cons = 0;
	static int db_number_pool_cons = 0;
	public static void closeDBConnection(Connection aconn) {
		if (aconn != null) {
			try {
				aconn.close();
			} catch (SQLException e) {
				System.err.println(DBUtils.unchainSqlException(e));
				e.printStackTrace();
			}
			db_number_cons--;
			if (datasource != null) {
				db_number_pool_cons--;
			}
			if (SurveyMain.isUnofficial) {
				System.err.println("SQL -conns: "
						+ db_number_cons
						+ " "
						+ ((datasource == null) ? ""
								: (" pool:" + db_number_pool_cons)));
			}
		}
	}
	public static final String escapeBasic(byte what[]) {
		return escapeLiterals(what);
	}

	public static final String escapeForMysql(byte what[]) {
		boolean hasEscapeable = false;
		boolean hasNonEscapeable = false;
		for (byte b : what) {
			int j = ((int) b) & 0xff;
			char c = (char) j;
			if (escapeIsBasic(c)) {
				continue;
			} else if (escapeIsEscapeable(c)) {
				hasEscapeable = true;
			} else {
				hasNonEscapeable = true;
			}
		}
		if (hasNonEscapeable) {
			return escapeHex(what);
		} else if (hasEscapeable) {
			return escapeLiterals(what);
		} else {
			return escapeBasic(what);
		}
	}

	public static String escapeForMysql(String what)
			throws UnsupportedEncodingException {
		if (what == null) {
			return "NULL";
		} else if (what.length() == 0) {
			return "\"\"";
		} else {
			return escapeForMysql(what.getBytes("ASCII"));
		}
	}

	public static String escapeForMysqlUtf8(String what)
			throws UnsupportedEncodingException {
		if (what == null) {
			return "NULL";
		} else if (what.length() == 0) {
			return "\"\"";
		} else {
			return escapeForMysql(what.getBytes("UTF-8"));
		}
	}

	public static final String escapeHex(byte what[]) {
		StringBuffer out = new StringBuffer("x'");
		for (byte b : what) {
			int j = ((int) b) & 0xff;
			if (j < 0x10) {
				out.append('0');
			}
			out.append(Integer.toHexString(j));
		}
		out.append("'");
		return out.toString();
	}

	public static final boolean escapeIsBasic(char c) {
		return ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
				|| (c >= '0' && c <= '9') || (c == ' ' || c == '.' || c == '/'
				|| c == '[' || c == ']' || c == '=' || c == '@' || c == '_'
				|| c == ',' || c == '&' || c == '-' || c == '(' || c == ')'
				|| c == '#' || c == '$' || c == '!'));
	}

	public static final boolean escapeIsEscapeable(char c) {
		return (c == 0 || c == '\'' || c == '"' || c == '\b' || c == '\n'
				|| c == '\r' || c == '\t' || c == 26 || c == '\\');
	}

	public static final String escapeLiterals(byte what[]) {
		StringBuffer out = new StringBuffer("'");
		for (byte b : what) {
			int j = ((int) b) & 0xff;
			char c = (char) j;
			switch (c) {
			case 0:
				out.append("\\0");
				break;
			case '\'':
				out.append("'");
				break;
			case '"':
				out.append("\\");
				break;
			case '\b':
				out.append("\\b");
				break;
			case '\n':
				out.append("\\n");
				break;
			case '\r':
				out.append("\\r");
				break;
			case '\t':
				out.append("\\t");
				break;
			case 26:
				out.append("\\z");
				break;
			case '\\':
				out.append("\\\\");
				break;
			default:
				out.append(c);
			}
		}
		out.append("'");
		return out.toString();
	}

	public synchronized static DBUtils getInstance() {
		if (instance == null) {
			instance = new DBUtils();
		}
		return instance;
	}

	// fix the UTF-8 fail
	public static final String getStringUTF8(ResultSet rs, int which)
			throws SQLException {
		if (db_Derby) { // unicode
			return rs.getString(which);
		}
		byte rv[] = rs.getBytes(which);
		if (rv != null) {
			String unicode;
			try {
				unicode = new String(rv, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				throw new InternalError(e.toString());
			}
			return unicode;
		} else {
			return null;
		}
	}

	public static boolean hasTable(Connection conn, String table) {
		String canonName = db_Derby ? table.toUpperCase() : table;
		try {
			ResultSet rs;

			if (db_Derby) {
				DatabaseMetaData dbmd = conn.getMetaData();
				rs = dbmd.getTables(null, null, canonName, null);
			} else {
				Statement s = conn.createStatement();
				rs = s.executeQuery("show tables like '" + canonName + "'");
			}

			if (rs.next() == true) {
				rs.close();
				// System.err.println("table " + canonName + " did exist.");
				return true;
			} else {
				System.err.println("table " + canonName + " did not exist.");
				return false;
			}
		} catch (SQLException se) {
			SurveyMain.busted("While looking for table '" + table + "': ", se);
			return false; // NOTREACHED
		}
	}

	public static final void setStringUTF8(PreparedStatement s, int which,
			String what) throws SQLException {
		if (db_Derby) {
			s.setString(which, what);
		} else {
			byte u8[];
			if (what == null) {
				u8 = null;
			} else {
				try {
					u8 = what.getBytes("UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					throw new InternalError(e.toString());
				}
			}
			s.setBytes(which, u8);
		}
	}

	static int sqlCount(WebContext ctx, Connection conn, PreparedStatement ps) {
		int rv = -1;
		try {
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				rv = rs.getInt(1);
			}
			rs.close();
		} catch (SQLException se) {
			String complaint = " Couldn't query count - "
					+ unchainSqlException(se) + " -  ps";
			System.err.println(complaint);
			ctx.println("<hr><font color='red'>ERR: " + complaint
					+ "</font><hr>");
		}
		return rv;
	}

	static int sqlCount(WebContext ctx, Connection conn, String sql) {
		int rv = -1;
		try {
			Statement s = conn.createStatement();
			ResultSet rs = s.executeQuery(sql);
			if (rs.next()) {
				rv = rs.getInt(1);
			}
			rs.close();
			s.close();
		} catch (SQLException se) {
			String complaint = " Couldn't query count - "
					+ unchainSqlException(se) + " - " + sql;
			System.err.println(complaint);
			ctx.println("<hr><font color='red'>ERR: " + complaint
					+ "</font><hr>");
		}
		return rv;
	}
	
	public String[] sqlQueryArray(Connection conn, String str) throws SQLException {
		return sqlQueryArrayArray(conn,str)[0];
	}
	
	public String[][] sqlQueryArrayArray(Connection conn, String str) throws SQLException {
		Statement s  = null;
		ResultSet rs  = null;
		try {
			s = conn.createStatement();
			rs = s.executeQuery(str);
			ArrayList<String[]> al = new ArrayList<String[]>();
			while(rs.next()) {
				al.add(arrayOfResult(rs));
			}
			return al.toArray(new String[al.size()][]);
		} finally {
			if(rs!=null) {
				rs.close();
			}
			if(s!=null) {
				s.close();
			}
		}
	}

	private String[] arrayOfResult(ResultSet rs) throws SQLException {
		ResultSetMetaData rsm = rs.getMetaData();
		String ret[] = new String[rsm.getColumnCount()];
		for(int i=0;i<ret.length;i++) {
			ret[i]=rs.getString(i+1);
		}
		return ret;
	}
	public String sqlQuery(Connection conn, String str) throws SQLException {
		return sqlQueryArray(conn,str)[0];
	}
	

	static int sqlUpdate(WebContext ctx, Connection conn, PreparedStatement ps) {
		int rv = -1;
		try {
			rv = ps.executeUpdate();
		} catch (SQLException se) {
			String complaint = " Couldn't sqlUpdate  - "
					+ unchainSqlException(se) + " -  ps";
			System.err.println(complaint);
			ctx.println("<hr><font color='red'>ERR: " + complaint
					+ "</font><hr>");
		}
		return rv;
	}

	public static final String unchainSqlException(SQLException e) {
		String echain = "SQL exception: \n ";
		SQLException laste = null;
		while (e != null) {
			laste = e;
			echain = echain + " -\n " + e.toString();
			e = e.getNextException();
		}
		String stackStr = "\n unknown Stack";
		try {
			StringWriter asString = new StringWriter();
			laste.printStackTrace(new PrintWriter(asString));
			stackStr = "\n Stack: \n " + asString.toString();
		} catch (Throwable tt) {
			stackStr = "\n unknown stack (" + tt.toString() + ")";
		}
		return echain + stackStr;
	}

	File dbDir = null;

	// File dbDir_u = null;
	static String dbInfo = null;

	private DBUtils() {
		// Initialize DB context
		try {
			Context initialContext = new InitialContext();
//			Context envContext = (Context) initialContext
//					.lookup("java:comp/env");
			datasource = (DataSource) initialContext.lookup("java:comp/env/" + JDBC_SURVEYTOOL);
			//datasource = (DataSource) envContext.lookup("ASDSDASDASDASD");
			
			if(datasource instanceof BasicDataSource && ((BasicDataSource)datasource).getUrl()==null) {
				datasource = null;
				//throw new InternalError("URL is null for datasource (tried once) " + JDBC_SURVEYTOOL);
			} else {
				System.err.println("daaSource class = " + datasource.getClass().getName());
			}
			if(datasource!=null) {
				System.err.println("Got datasource: " + datasource.toString());
			}
			Connection c = null;
			try {
				if(datasource!=null) {
					c = datasource.getConnection();
				}
			} catch (SQLException  t) {
				System.err
						.println(getClass().getName()+": WARNING: in the future, we will require a JNDI datasource.  "
								+ "'"+JDBC_SURVEYTOOL+"'"
								+ ".getConnection() returns : "
								+ t.toString()+"\n"+unchainSqlException(t));
				datasource = null;
			} finally {
				if (c != null)
					try {
						c.close();
					} catch (Throwable tt) {
						System.err.println("Couldn't close datasource's conn: "
								+ tt.toString());
						tt.printStackTrace();
					}
			}
		} catch (NamingException nc) {
			nc.printStackTrace();
			datasource = null;
			throw new Error("Couldn't load context " + JDBC_SURVEYTOOL
					+ " - not using datasource.",nc);
		}
		
	}

	public void doShutdown() throws SQLException {
		if (db_Derby) {
			DriverManager.getConnection(CLDR_DB_SHUTDOWNSUFFIX);
		}
		datasource = null;
		System.err.println("DBUtils: removing my instance.");
		instance = null;
	}

	public Connection getDBConnection(SurveyMain surveyMain) {
		return getDBConnection(surveyMain, "");
	}

	Connection getDBConnection(SurveyMain surveyMain, String options) {
		try {
			db_number_cons++;

			if (datasource != null) {
				db_number_pool_cons++;
				if (SurveyMain.isUnofficial) {
					System.err.println("SQL  +conns: " + db_number_cons
							+ " Pconns: " + db_number_pool_cons);
				}
				Connection c = datasource.getConnection();
				c.setAutoCommit(false);
				return c;
			}
			if(false) throw new InternalError("**************** FAIL: no DataSource ********************\n");
			if (SurveyMain.isUnofficial) {
				System.err.println("SQL +conns: " + db_number_cons);
			}
			// if(db_number_cons >= 12) {
			// throw new InternalError("too many..");
			// }
			Properties props = new Properties();
			if (CLDR_DB_U != null) {
				props.put("user", CLDR_DB_U);
				props.put("password", CLDR_DB_P);
			}
			Connection nc = DriverManager.getConnection(db_protocol + cldrdb
					+ options, props);
			nc.setAutoCommit(false);
			return nc;
		} catch (SQLException se) {
			se.printStackTrace();
			SurveyMain.busted("Fatal in getDBConnection", se);
			return null;
		}
	}

	void setupDBProperties(SurveyMain surveyMain, Properties cldrprops) {
		db_driver = cldrprops.getProperty("CLDR_DB_DRIVER",
				"org.apache.derby.jdbc.EmbeddedDriver");
		db_protocol = cldrprops.getProperty("CLDR_DB_PROTOCOL", "jdbc:derby:");
		if (db_protocol.indexOf("derby") >= 0) {
			db_Derby = true;
		} else if (db_protocol.indexOf("mysql") >= 0) {
			System.err.println("Note: mysql mode");
			db_Mysql = true;
			DB_SQL_IDENTITY = "AUTO_INCREMENT PRIMARY KEY";
			DB_SQL_BINCOLLATE = " COLLATE latin1_bin ";
			DB_SQL_VARCHARXPATH = "TEXT(1000) CHARACTER SET latin1 "
					+ DB_SQL_BINCOLLATE;
			DB_SQL_BINTRODUCER = "_latin1";
			DB_SQL_WITHDEFAULT = "DEFAULT";
			DB_SQL_TIMESTAMP0 = "DATETIME";
			DB_SQL_CURRENT_TIMESTAMP0 = "'1999-12-31 23:59:59'"; // NOW?
			DB_SQL_MIDTEXT = "TEXT(1024)";
			DB_SQL_BIGTEXT = "TEXT(16384)";
			DB_SQL_UNICODE = "BLOB";
			DB_SQL_ALLTABLES = "show tables";
		} else {
			System.err.println("WARNING: Don't know what kind of database is "
					+ db_protocol + " - might be interesting!");
		}
		CLDR_DB_U = cldrprops.getProperty("CLDR_DB_U", null);
		CLDR_DB_P = cldrprops.getProperty("CLDR_DB_P", null);
		CLDR_DB = cldrprops.getProperty("CLDR_DB", "cldrdb");
		dbDir = new File(SurveyMain.cldrHome, CLDR_DB);
		cldrdb = cldrprops.getProperty("CLDR_DB_LOCATION",
				dbDir.getAbsolutePath());
		CLDR_DB_CREATESUFFIX = cldrprops.getProperty("CLDR_DB_CREATESUFFIX",
				";create=true");
		CLDR_DB_SHUTDOWNSUFFIX = cldrprops.getProperty(
				"CLDR_DB_SHUTDOWNSUFFIX", "jdbc:derby:;shutdown=true");
	}

	public void startupDB(SurveyMain sm,
			CLDRProgressIndicator.CLDRProgressTask progress) {
		try {
			// if(false) { // pooling listener disabled by default
			// progress.update( "Load pool "+STPoolingListener.ST_ATTRIBUTE); //
			// restore
			// datasource = (DataSource)
			// getServletContext().getAttribute(STPoolingListener.ST_ATTRIBUTE);
			// }

			if (DBUtils.datasource != null) {

				progress.update("Load " + db_driver); // restore
				Object o = Class.forName(db_driver).newInstance();
				try {
					java.sql.Driver drv = (java.sql.Driver) o;
					progress.update("Check " + db_driver); // restore
					dbInfo = "v" + drv.getMajorVersion() + "."
							+ drv.getMinorVersion();
					// dbInfo = dbInfo + " "
					// +org.apache.derby.tools.sysinfo.getProductName()+" "
					// +org.apache.derby.tools.sysinfo.getVersionString();
				} catch (Throwable t) {
					dbInfo = "unknown";
				}
				if (sm != null)
					sm.logger.info("loaded " + db_driver + " driver " + o
							+ " - " + dbInfo);
				progress.update("Create DB"); // restore
				Connection conn = getDBConnection(sm, CLDR_DB_CREATESUFFIX);
				// logger.info("Connected to database " + cldrdb);
				// /*U*/ Connection conn_u = getU_DBConnection(";create=true");
				// logger.info("Connected to user database " + cldrdb_u);

				// set up our main tables.
				progress.update("Commit DB"); // restore
				conn.commit();
				conn.close();
			} else {
				progress.update("Using datasource..."); // restore
			}

			// /*U*/ conn_u.commit();
			// /*U*/ conn_u.close();
		} catch (SQLException e) {
			if (sm != null)
				SurveyMain.busted("On database startup", e);
			return;
		} catch (Throwable t) {
			if (sm != null)
				SurveyMain.busted("Other error on database startup", t);
			t.printStackTrace();
			return;
		}
	}
}
