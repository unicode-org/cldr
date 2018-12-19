/*
 * Copyright (C) 2004-2014 IBM Corporation and Others. All Rights Reserved.
 */
package org.unicode.cldr.web;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRConfigImpl;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.StackTracker;
import org.unicode.cldr.web.SurveyMain.Phase;

import com.ibm.icu.dev.util.ElapsedTimer;
import com.ibm.icu.text.UnicodeSet;

/**
 * Singleton utility class for simple(r) DB access.
 */
public class DBUtils {
    private static final boolean DEBUG = false;// CldrUtility.getProperty("TEST",
    // false);
    private static final boolean DEBUG_QUICKLY = false;// CldrUtility.getProperty("TEST",
    // false);

    private static final boolean DEBUG_SQL = false; // show "all" SQL
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
    // public static String cldrdb = null;
    public static String CLDR_DB_CREATESUFFIX = null;
    public static String CLDR_DB_SHUTDOWNSUFFIX = null;
    public static boolean db_Derby = false;
    public static boolean db_Mysql = false;

    /**
     * Return a string as to which SQL flavor is in use.
     *
     * @return
     */
    public static final String getDBKind() {
        if (db_Derby) {
            return "Derby";
        } else if (db_Mysql) {
            return "MySql";
        } else {
            return "Unknown";
        }
    }

    public String getDBInfo() {
        return dbInfo;
    }

    // === DB workarounds :( - derby by default
    public static String DB_SQL_IDENTITY = "GENERATED ALWAYS AS IDENTITY";
    public static String DB_SQL_VARCHARXPATH = "varchar(1024)";
    public static String DB_SQL_WITHDEFAULT = "WITH DEFAULT";
    public static String DB_SQL_TIMESTAMP0 = "TIMESTAMP";
    public static String DB_SQL_CURRENT_TIMESTAMP0 = "CURRENT_TIMESTAMP";
    public static String DB_SQL_MIDTEXT = "VARCHAR(1024)";
    public static String DB_SQL_BIGTEXT = "VARCHAR(16384)";
    public static String DB_SQL_UNICODE = "VARCHAR(16384)"; // unicode type
    // string
    public static String DB_SQL_LAST_MOD_TYPE = "TIMESTAMP";
    public static String DB_SQL_LAST_MOD = " last_mod TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP  ";
    public static String DB_SQL_ALLTABLES = "select tablename from SYS.SYSTABLES where tabletype='T'";
    public static String DB_SQL_BINCOLLATE = "";
    public static String DB_SQL_ENGINE_INNO = "";
    public static String DB_SQL_MB4 = "";
    public static int db_number_open = 0;
    public static int db_number_used = 0;
    private static int db_UnicodeType = java.sql.Types.VARCHAR; /*
                                                                * for setNull -
                                                                * see
                                                                * java.sql.Types
                                                                */
    private static final StackTracker tracker = DEBUG ? new StackTracker() : null; // new

    // StackTracker();
    // -
    // enable,
    // to
    // track
    // unclosed
    // connections

    public Appendable stats(Appendable output) throws IOException {
        return output.append("DBUtils: currently open: " + db_number_open).append(", max open: " + db_max_open)
            .append(", total used: " + db_number_used);
    }

    public Appendable statsShort(Appendable output) throws IOException {
        return output.append("" + db_number_open).append("/" + db_max_open);
    }

    public static void closeDBConnection(Connection conn) {
        if (conn != null) {
            if (SurveyMain.isUnofficial() && tracker != null) {
                tracker.remove(conn);
            }
            try {
                if (db_Derby &&
                    datasource instanceof EmbeddedDataSource &&
                    !conn.isClosed() &&
                    !conn.getAutoCommit()) {
                    // commit on close if we are using Derby directly
                    conn.commit();
                }
                conn.close();
            } catch (SQLException e) {
                System.err.println(DBUtils.unchainSqlException(e));
                e.printStackTrace();
            }
            db_number_open--;
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

    public static String escapeForMysql(String what) throws UnsupportedEncodingException {
        if (what == null) {
            return "NULL";
        } else if (what.length() == 0) {
            return "\"\"";
        } else {
            return escapeForMysql(what.getBytes("ASCII"));
        }
    }

    public static String escapeForMysqlUtf8(String what) throws UnsupportedEncodingException {
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
        return ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || (c == ' ' || c == '.' || c == '/'
            || c == '[' || c == ']' || c == '=' || c == '@' || c == '_' || c == ',' || c == '&' || c == '-' || c == '('
            || c == ')' || c == '#' || c == '$' || c == '!'));
    }

    public static final boolean escapeIsEscapeable(char c) {
        return (c == 0 || c == '\'' || c == '"' || c == '\b' || c == '\n' || c == '\r' || c == '\t' || c == 26 || c == '\\');
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

    public static DBUtils peekInstance() {
        return instance;
    }

    public synchronized static DBUtils getInstance() {
        if (instance == null) {
            instance = new DBUtils();
        }
        return instance;
    }

    public synchronized static void makeInstanceFrom(DataSource dataSource2) {
        if (instance == null) {
            instance = new DBUtils(dataSource2);
        } else {
            throw new IllegalArgumentException("Already initted.");
        }
    }

    public static String getStringUTF8(ResultSet rs, String which) throws SQLException {
        if (db_Derby) { // unicode
            String str = rs.getString(which);
            if (rs.wasNull())
                return null;
            return str;
        }
        byte rv[] = rs.getBytes(which);
        if (rs.wasNull())
            return null;
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

    // fix the UTF-8 fail
    public static final String getStringUTF8(ResultSet rs, int which) throws SQLException {
        if (db_Derby) { // unicode
            String str = rs.getString(which);
            if (rs.wasNull())
                return null;
            return str;
        }
        byte rv[] = rs.getBytes(which);
        if (rs.wasNull())
            return null;
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
        String canonName = canonTableName(table);
        try {
            ResultSet rs = null;
            Statement s = null;
            try {
                if (db_Derby) {
                    DatabaseMetaData dbmd = conn.getMetaData();
                    rs = dbmd.getTables(null, null, canonName, null);
                } else {
                    s = conn.createStatement();
                    rs = s.executeQuery("show tables like '" + canonName + "'");
                }

                if (rs.next() == true) {
                    SurveyLog.warnOnce("table " + canonName + " did exist.");
                    return true;
                } else {
                    SurveyLog.warnOnce("table " + canonName + " did not exist.");
                    return false;
                }
            } finally {
                DBUtils.close(s, rs);
            }
        } catch (SQLException se) {
            SurveyMain.busted("While looking for table '" + table + "': ", se);
            return false; // NOTREACHED
        }
    }

    private static String canonTableName(String table) {
        String canonName = db_Derby ? table.toUpperCase() : table;
        return canonName;
    }

    public static boolean tableHasColumn(Connection conn, String table, String column) {
        final String canonTable = canonTableName(table);
        final String canonColumn = canonTableName(column);
        try {
            if (db_Derby) {
                ResultSet rs;
                DatabaseMetaData dbmd = conn.getMetaData();
                rs = dbmd.getColumns(null, null, canonTable, canonColumn);
                if (rs.next() == true) {
                    rs.close();
                    //System.err.println("column " + table +"."+column + " did exist.");
                    return true;
                } else {
                    SurveyLog.debug("column " + table + "." + column + " did not exist.");
                    return false;
                }
            } else {
                return sqlCount(conn, "select count(*) from information_schema.COLUMNS where table_name=? and column_name=?", canonTable, canonColumn) > 0;
            }
        } catch (SQLException se) {
            SurveyMain.busted("While looking for column '" + table + "." + column + "': ", se);
            return false; // NOTREACHED
        }
    }

    private static final byte[] encode_u8(String what) {
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
        return u8;
    }

    public static final void setStringUTF8(PreparedStatement s, int which, String what) throws SQLException {
        if (what == null) {
            s.setNull(which, db_UnicodeType);
        }
        if (db_Derby) {
            s.setString(which, what);
        } else {
            s.setBytes(which, encode_u8(what));
        }
    }

    public static final Object prepareUTF8(String what) {
        if (what == null) return null;
        if (db_Derby) {
            return what; // sanity
        } else {
            return encode_u8(what);
        }
    }

    /**
     * Returns an integer value (such as a count) from the specified sql.
     * @param sql
     * @param args
     * @return
     */
    public static int sqlCount(String sql, Object... args) {
        Connection conn = null;
        try {
            conn = DBUtils.getInstance().getDBConnection();
            return sqlCount(conn, sql, args);
        } finally {
            DBUtils.close(conn);
        }
    }

    public static int sqlCount(Connection conn, String sql, Object... args) {
        PreparedStatement ps = null;
        try {
            ps = prepareForwardReadOnly(conn, sql);
            setArgs(ps, args);
            return sqlCount(conn, ps);
        } catch (SQLException sqe) {
            SurveyLog.logException(sqe, "running sqlcount " + sql);
            return -1;
        } finally {
            DBUtils.close(ps);
        }
    }

    public static boolean hasTable(String table) {
        Connection conn = null;
        try {
            conn = DBUtils.getInstance().getDBConnection();
            return hasTable(conn, table);
        } finally {
            DBUtils.close(conn);
        }
    }

    static int sqlCount(Connection conn, PreparedStatement ps) throws SQLException {
        int rv = -1;
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            rv = rs.getInt(1);
        }
        rs.close();
        return rv;
    }

    static int sqlCount(WebContext ctx, Connection conn, PreparedStatement ps) {
        try {
            return sqlCount(conn, ps);
        } catch (SQLException se) {
            String complaint = " Couldn't query count - " + unchainSqlException(se) + " -  ps";
            System.err.println(complaint);
            ctx.println("<hr><font color='red'>ERR: " + complaint + "</font><hr>");
            return -1;
        }
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
            String complaint = " Couldn't query count - " + unchainSqlException(se) + " - " + sql;
            System.err.println(complaint);
            ctx.println("<hr><font color='red'>ERR: " + complaint + "</font><hr>");
        }
        return rv;
    }

    public static String[] sqlQueryArray(Connection conn, String str) throws SQLException {
        return sqlQueryArrayArray(conn, str)[0];
    }

    public static String[][] sqlQueryArrayArray(Connection conn, String str) throws SQLException {
        Statement s = null;
        ResultSet rs = null;
        try {
            s = conn.createStatement();
            try {
                rs = s.executeQuery(str);
            } catch (SQLException se) {
                SurveyLog.logException(se, "Error [SQL was: " + str + "]");
                throw se; // rethrow
            }
            ArrayList<String[]> al = new ArrayList<String[]>();
            while (rs.next()) {
                al.add(arrayOfResult(rs));
            }
            return al.toArray(new String[al.size()][]);
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (s != null) {
                s.close();
            }
        }
    }

    //
    // private String[] arrayOfResult(ResultSet rs) throws SQLException {
    // ResultSetMetaData rsm = rs.getMetaData();
    // String ret[] = new String[rsm.getColumnCount()];
    // for(int i=0;i<ret.length;i++) {
    // ret[i]=rs.getString(i+1);
    // }
    // return ret;
    // }
    public static String sqlQuery(Connection conn, String str) throws SQLException {
        return sqlQueryArray(conn, str)[0];
    }

    public static int sqlUpdate(WebContext ctx, Connection conn, PreparedStatement ps) {
        int rv = -1;
        try {
            rv = ps.executeUpdate();
        } catch (SQLException se) {
            String complaint = " Couldn't sqlUpdate  - " + unchainSqlException(se) + " -  ps";
            System.err.println(complaint);
            ctx.println("<hr><font color='red'>ERR: " + complaint + "</font><hr>");
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

    public boolean isBogus() {
        return (datasource == null);
    }

    private DBUtils() {
        // Initialize DB context
        System.err.println("Loading datasource: java:comp/env " + JDBC_SURVEYTOOL);
        ElapsedTimer et = new ElapsedTimer();
        try {
            Context initialContext = new InitialContext();
            Context eCtx = (Context) initialContext.lookup("java:comp/env");
            datasource = (DataSource) eCtx.lookup(JDBC_SURVEYTOOL);
            // datasource = (DataSource) envContext.lookup("ASDSDASDASDASD");

            if (datasource != null) {
                System.err.println("Got datasource: " + datasource.toString() + " in " + et);
            }
            Connection c = null;
            try {
                if (datasource != null) {
                    c = datasource.getConnection();
                    DatabaseMetaData dmd = c.getMetaData();
                    dbInfo = dmd.getDatabaseProductName() + " v" + dmd.getDatabaseProductVersion() + " " +
                        "driver " + dmd.getDriverName() + " ver " + dmd.getDriverVersion();
                    setupSqlForServerType();
                    SurveyLog.debug("Metadata: " + dbInfo);
                    handleHaveDatasource(datasource);
                }
            } catch (SQLException t) {
                datasource = null;
                throw new IllegalArgumentException(getClass().getName() + ": WARNING: we require a JNDI datasource.  " + "'"
                    + JDBC_SURVEYTOOL + "'" + ".getConnection() returns : " + t.toString() + "\n" + unchainSqlException(t));
            } finally {
                if (c != null)
                    try {
                    c.close();
                    } catch (Throwable tt) {
                    System.err.println("Couldn't close datasource's conn: " + tt.toString());
                    tt.printStackTrace();
                    }
            }
        } catch (NamingException nc) {
            nc.printStackTrace();
            datasource = null;
            throw new Error("Couldn't load context " + JDBC_SURVEYTOOL + " - not using datasource.", nc);
        }

    }

    private void handleHaveDatasource(DataSource datasource2) {
        // process migrations here..
    }

    public DBUtils(DataSource dataSource2) {
        datasource = dataSource2;
        Connection c = null;
        try {
            if (datasource != null) {
                c = datasource.getConnection();
                DatabaseMetaData dmd = c.getMetaData();
                dbInfo = dmd.getDatabaseProductName() + " v" + dmd.getDatabaseProductVersion();
                setupSqlForServerType();
                if (db_Derby) {
                    c.setAutoCommit(false);
                }
                boolean autoCommit = c.getAutoCommit();
                if (autoCommit == true) {
                    throw new IllegalArgumentException("autoCommit was true, expected false. Check your configuration.");
                }
                SurveyLog.debug("Metadata: " + dbInfo + ", autocommit: " + autoCommit);
                handleHaveDatasource(datasource);
            }
        } catch (SQLException t) {
            datasource = null;
            throw new IllegalArgumentException(getClass().getName() + ": WARNING: we require a JNDI datasource.  " + "'"
                + JDBC_SURVEYTOOL + "'" + ".getConnection() returns : " + t.toString() + "\n" + unchainSqlException(t));
        } finally {
            if (c != null)
                try {
                c.close();
                } catch (Throwable tt) {
                System.err.println("Couldn't close datasource's conn: " + tt.toString());
                tt.printStackTrace();
                }
        }
    }

    private void setupSqlForServerType() {
        SurveyLog.debug("setting up SQL for database type " + dbInfo);
        System.err.println("setting up SQL for database type " + dbInfo);
        if (dbInfo.contains("Derby")) {
            db_Derby = true;
            System.err.println("Note: Derby (embedded) mode. ** some features may not work as expected **");
            db_UnicodeType = java.sql.Types.VARCHAR;
        } else if (dbInfo.contains("MySQL")) {
            System.err.println("Note: MySQL mode");
            db_Mysql = true;
            DB_SQL_IDENTITY = "AUTO_INCREMENT PRIMARY KEY";
            DB_SQL_BINCOLLATE = " COLLATE latin1_bin ";
            DB_SQL_MB4 = " CHARACTER SET utf8mb4 COLLATE utf8mb4_bin";
            DB_SQL_VARCHARXPATH = "TEXT(1000)";
            DB_SQL_WITHDEFAULT = "DEFAULT";
            DB_SQL_TIMESTAMP0 = "DATETIME";
            DB_SQL_LAST_MOD_TYPE = "TIMESTAMP";
            DB_SQL_LAST_MOD = " last_mod TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP ";
            DB_SQL_CURRENT_TIMESTAMP0 = "'1999-12-31 23:59:59'"; // NOW?
            DB_SQL_MIDTEXT = "TEXT(1024)";
            DB_SQL_BIGTEXT = "TEXT(16384)";
            DB_SQL_UNICODE = "BLOB";
            db_UnicodeType = java.sql.Types.BLOB;
            DB_SQL_ALLTABLES = "show tables";

            DB_SQL_ENGINE_INNO = "ENGINE=InnoDB";
        } else {
            System.err.println("*** WARNING: Don't know what kind of database is " + dbInfo + " - don't know what kind of hacky nonportable SQL to use!");
        }
    }

    public void doShutdown() throws SQLException {
        try {
            DBUtils.close(datasource);
        } catch (IllegalArgumentException iae) {
            System.err.println("DB Shutdown in progress, ignoring: " + iae);
        }
        datasource = null;
        if (db_Derby) {
            try {
                DriverManager.getConnection("jdbc:derby:;shutdown=true");
            } catch (Throwable t) {
                // ignore
            }
        }
        if (DBUtils.db_number_open > 0) {
            System.err.println("DBUtils: removing my instance. " + DBUtils.db_number_open + " still open?\n" + tracker);
        }
        if (tracker != null)
            tracker.clear();
        instance = null;
    }

    /**
     * @deprecated Use {@link #getDBConnection()} instead
     */
    public final Connection getDBConnection(SurveyMain surveyMain) {
        return getDBConnection();
    }

    /**
     * This connection MAY NOT be held in an object. Hold it and then close it ( DBUtils.close() )
     * @return
     */
    public final Connection getDBConnection() {
        return getDBConnection("");
    }

    /**
     * @deprecated Use {@link #getDBConnection(String)} instead
     */
    public final Connection getDBConnection(SurveyMain surveyMain, String options) {
        return getDBConnection(options);
    }

    long lastMsg = -1;
    private int db_max_open = 0;

    public Connection getDBConnection(String options) {
        try {
            db_max_open = Math.max(db_max_open, db_number_open++);
            db_number_used++;

            if (DEBUG) {
                long now = System.currentTimeMillis();
                if (now - lastMsg > (DEBUG_QUICKLY ? 6000 : 3600000) /*
                                                                     * || (
                                                                     * db_number_used
                                                                     * ==5000)
                                                                     */) {
                    lastMsg = now;
                    System.err.println("DBUtils: " + db_number_open + " open, " + db_max_open + " max,  " + db_number_used
                        + " used. " + StackTracker.currentStack());
                }
            }

            Connection c = datasource.getConnection();
            if (db_Derby) {
                c.setAutoCommit(false);
            }
            if (SurveyMain.isUnofficial() && tracker != null)
                tracker.add(c);
            return c;
        } catch (SQLException se) {
            se.printStackTrace();
            SurveyMain.busted("Fatal in getDBConnection", se);
            return null;
        }
    }

    void setupDBProperties(SurveyMain surveyMain, CLDRConfig survprops) {
        // db_driver = cldrprops.getProperty("CLDR_DB_DRIVER",
        // "org.apache.derby.jdbc.EmbeddedDriver");
        // db_protocol = cldrprops.getProperty("CLDR_DB_PROTOCOL",
        // "jdbc:derby:");
        // CLDR_DB_U = cldrprops.getProperty("CLDR_DB_U", null);
        // CLDR_DB_P = cldrprops.getProperty("CLDR_DB_P", null);
        // CLDR_DB = survprops.getProperty("CLDR_DB", "cldrdb");
        // dbDir = new File(SurveyMain.cldrHome, CLDR_DB);
        // cldrdb = survprops.getProperty("CLDR_DB_LOCATION",
        // dbDir.getAbsolutePath());
        CLDR_DB_CREATESUFFIX = survprops.getProperty("CLDR_DB_CREATESUFFIX", ";create=true");
        CLDR_DB_SHUTDOWNSUFFIX = survprops.getProperty("CLDR_DB_SHUTDOWNSUFFIX", "jdbc:derby:;shutdown=true");
    }

    public void startupDB(SurveyMain sm, CLDRProgressIndicator.CLDRProgressTask progress) {
        System.err.println("StartupDB: datasource=" + datasource);
        if (datasource == null) {
            throw new RuntimeException(" - JNDI required:  " + getDbBrokenMessage());
        }

        progress.update("Using datasource..." + dbInfo); // restore

    }

    /**
     * Shortcut for certain statements.
     *
     * @param conn
     * @param str
     * @return
     * @throws SQLException
     */
    public static final PreparedStatement prepareForwardReadOnly(Connection conn, String str) throws SQLException {
        if (DEBUG_SQL) System.out.println("SQL: " + str);
        return conn.prepareStatement(str, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    }

    /**
     * Shortcut for certain statements.
     *
     * @param conn
     * @param str
     * @return
     * @throws SQLException
     */
    public static final PreparedStatement prepareForwardUpdateable(Connection conn, String str) throws SQLException {
        return conn.prepareStatement(str, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
    }

    /**
     * prepare statements for this connection
     *
     * @throws SQLException
     **/
    public static final PreparedStatement prepareStatementForwardReadOnly(Connection conn, String name, String sql)
        throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = prepareForwardReadOnly(conn, sql);
        } finally {
            if (ps == null) {
                System.err.println("Warning: couldn't initialize " + name + " from " + sql);
            }
        }
        // if(false) System.out.println("EXPLAIN EXTENDED " +
        // sql.replaceAll("\\?", "'?'")+";");
        // } catch ( SQLException se ) {
        // String complaint = "Vetter:  Couldn't prepare " + name + " - " +
        // DBUtils.unchainSqlException(se) + " - " + sql;
        // logger.severe(complaint);
        // throw new RuntimeException(complaint);
        // }
        return ps;
    }

    /**
     * prepare statements for this connection. Assumes generated keys.
     *
     * @throws SQLException
     **/
    public static final PreparedStatement prepareStatement(Connection conn, String name, String sql) throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        } finally {
            if (ps == null) {
                System.err.println("Warning: couldn't initialize " + name + " from " + sql);
            }
        }
        // if(false) System.out.println("EXPLAIN EXTENDED " +
        // sql.replaceAll("\\?", "'?'")+";");
        // } catch ( SQLException se ) {
        // String complaint = "Vetter:  Couldn't prepare " + name + " - " +
        // DBUtils.unchainSqlException(se) + " - " + sql;
        // logger.severe(complaint);
        // throw new RuntimeException(complaint);
        // }
        return ps;
    }

    /**
     * Close all of the objects in order, if not null. Knows how to close
     * Connection, Statement, ResultSet, otherwise you'll get an IAE.
     *
     * @param a1
     * @throws SQLException
     */
    public static void close(Object... list) {
        for (Object o : list) {
            // if(o!=null) {
            // System.err.println("Closing " +
            // an(o.getClass().getSimpleName())+" " + o.getClass().getName());
            // }
            try {
                if (o == null) {
                    continue;
                } else if (o instanceof Connection) {
                    DBUtils.closeDBConnection((Connection) o);
                } else if (o instanceof Statement) {
                    ((Statement) o).close();
                } else if (o instanceof ResultSet) {
                    ((ResultSet) o).close();
                } else if (o instanceof DBCloseable) {
                    ((DBCloseable) o).close();
                } else {
                    final Class theClass = o.getClass();
                    final String simpleName = theClass.getSimpleName();
                    if (simpleName.equals("BasicDataSource")) { // could expand this later, if we want to generically call close()
                        try {
                            // try to find a "close"
                            final Method m = theClass.getDeclaredMethod("close");
                            if (m != null) {
                                System.err.println("Attempting to call close() on " + theClass.getName());
                                m.invoke(o);
                            }
                        } catch (Exception nsm) {
                            nsm.printStackTrace();
                            System.err.println("Caught exception " + nsm + " - so, don't know how to close " + an(simpleName) + " "
                                + theClass.getName());
                        }
                    } else {
                        throw new IllegalArgumentException("Don't know how to close " + an(simpleName) + " "
                            + theClass.getName());
                    }
                }
            } catch (SQLException e) {
                System.err.println(unchainSqlException(e));
            }
        }
    }

    private static final UnicodeSet vowels = new UnicodeSet("[aeiouAEIOUhH]");

    /**
     * Print A or AN appropriately.
     *
     * @param str
     * @return
     */
    private static String an(String str) {
        boolean isVowel = vowels.contains(str.charAt(0));
        return isVowel ? "an" : "a";
    }

    public boolean hasDataSource() {
        return (datasource != null);
    }

    /**
     * @param conn
     * @param sql
     * @param args
     * @return
     * @throws SQLException
     */
    public static PreparedStatement prepareStatementWithArgs(Connection conn, String sql, Object... args) throws SQLException {
        PreparedStatement ps;
        ps = conn.prepareStatement(sql);

        // while (args!=null&&args.length==1&&args[0] instanceof Object[]) {
        // System.err.println("Unwrapping " + args + " to " + args[0]);
        // }
        setArgs(ps, args);
        return ps;
    }

    /**
     * @param conn
     * @param sql
     * @param args
     * @return
     * @throws SQLException
     */
    public static PreparedStatement prepareStatementWithArgsFRO(Connection conn, String sql, Object... args) throws SQLException {
        PreparedStatement ps;
        ps = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

        setArgs(ps, args);
        return ps;
    }

    /**
     * @param ps
     * @param args
     * @throws SQLException
     */
    public static void setArgs(PreparedStatement ps, Object... args) throws SQLException {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                Object o = args[i];
                if (o == null) {
                    ps.setNull(i + 1, java.sql.Types.NULL);
                } else if (o instanceof String) {
                    ps.setString(i + 1, (String) o);
                } else if (o instanceof byte[]) {
                    ps.setBytes(i + 1, (byte[]) o);
                } else if (o instanceof Integer) {
                    ps.setInt(i + 1, (Integer) o);
                } else if (o instanceof java.sql.Date) {
                    ps.setDate(i + 1, (java.sql.Date) o);
                } else if (o instanceof java.sql.Timestamp) {
                    ps.setTimestamp(i + 1, (java.sql.Timestamp) o);
                } else if (o instanceof CLDRLocale) { /*
                                                      * toString compatible
                                                      * things
                                                      */
                    ps.setString(i + 1, ((CLDRLocale) o).getBaseName());
                } else {
                    System.err.println("DBUtils: Warning: using toString for unknown object " + o.getClass().getName());
                    ps.setString(i + 1, o.toString());
                }
            }
        }
    }

    public static String[][] resultToArrayArray(ResultSet rs) throws SQLException {
        ArrayList<String[]> al = new ArrayList<String[]>();
        while (rs.next()) {
            al.add(arrayOfResult(rs));
        }
        return al.toArray(new String[al.size()][]);
    }

    public static Object[][] resultToArrayArrayObj(ResultSet rs) throws SQLException {
        ArrayList<Object[]> al = new ArrayList<Object[]>();
        ResultSetMetaData rsm = rs.getMetaData();
        int colCount = rsm.getColumnCount();
        while (rs.next()) {
            al.add(arrayOfResultObj(rs, colCount, rsm));
        }
        return al.toArray(new Object[al.size()][]);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object>[] resultToArrayAssoc(ResultSet rs) throws SQLException {
        ResultSetMetaData rsm = rs.getMetaData();
        ArrayList<Map<String, Object>> al = new ArrayList<Map<String, Object>>();
        while (rs.next()) {
            al.add(assocOfResult(rs, rsm));
        }
        return al.toArray(new Map[al.size()]);
    }

    public static Map<String, Object> assocOfResult(ResultSet rs) throws SQLException {
        return assocOfResult(rs, rs.getMetaData());
    }

    private static Map<String, Object> assocOfResult(ResultSet rs, ResultSetMetaData rsm) throws SQLException {
        Map<String, Object> m = new HashMap<String, Object>(rsm.getColumnCount());
        for (int i = 1; i <= rsm.getColumnCount(); i++) {
            Object obj = extractObject(rs, rsm, i);
            m.put(rsm.getColumnName(i).toLowerCase(), obj);
        }

        return m;
    }

    /**
     * @param rs
     * @param rsm
     * @param i
     * @return
     * @throws SQLException
     */
    private static Object extractObject(ResultSet rs, ResultSetMetaData rsm, int i) throws SQLException {
        Object obj = null;
        int colType = rsm.getColumnType(i);
        if (colType == java.sql.Types.BLOB) {
            obj = DBUtils.getStringUTF8(rs, i);
        } else if (colType == java.sql.Types.TIMESTAMP) {
            obj = rs.getTimestamp(i);
        } else if (colType == java.sql.Types.DATE) {
            obj = rs.getDate(i);
        } else { // generic
            obj = rs.getObject(i);
            if (obj != null && obj.getClass().isArray()) {
                obj = DBUtils.getStringUTF8(rs, i);
            }
        }
        return obj;
    }

    public static String sqlQuery(Connection conn, String sql, Object... args) throws SQLException {
        return sqlQueryArray(conn, sql, args)[0];
    }

    public static String[] sqlQueryArray(Connection conn, String sql, Object... args) throws SQLException {
        return sqlQueryArrayArray(conn, sql, args)[0];
    }

    public static String[][] sqlQueryArrayArray(Connection conn, String str, Object... args) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = prepareStatementWithArgs(conn, str, args);

            rs = ps.executeQuery();
            return resultToArrayArray(rs);
        } finally {
            DBUtils.close(rs, ps);
        }
    }

    public static Object[][] sqlQueryArrayArrayObj(Connection conn, String str, Object... args) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = prepareStatementWithArgs(conn, str, args);

            rs = ps.executeQuery();
            return resultToArrayArrayObj(rs);
        } finally {
            DBUtils.close(rs, ps);
        }
    }

    public static int sqlUpdate(Connection conn, String str, Object... args) throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = prepareStatementWithArgs(conn, str, args);

            return (ps.executeUpdate());
        } finally {
            DBUtils.close(ps);
        }
    }

    @SuppressWarnings("rawtypes")
    public Map[] sqlQueryArrayAssoc(Connection conn, String sql, Object... args) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = prepareStatementWithArgs(conn, sql, args);

            rs = ps.executeQuery();
            return resultToArrayAssoc(rs);
        } finally {
            DBUtils.close(rs, ps);
        }
    }

    /**
     * Standardized way of versioning a string.
     * @param sb
     * @param forVersion
     * @param isBeta
     * @return
     */
    public static StringBuilder appendVersionString(StringBuilder sb, String forVersion, Boolean isBeta) {
        if (forVersion != null) {
            sb.append('_');
            sb.append(forVersion.toLowerCase());
        }
        if (isBeta != null && isBeta) {
            sb.append("_beta");
        }
        return sb;
    }

    /**
     * Append a versioned string
     */
    public static StringBuilder appendVersionString(StringBuilder sb) {
        return appendVersionString(sb, SurveyMain.getNewVersion(), SurveyMain.phase() == SurveyMain.Phase.BETA);
    }

    private static String[] arrayOfResult(ResultSet rs) throws SQLException {
        ResultSetMetaData rsm = rs.getMetaData();
        String ret[] = new String[rsm.getColumnCount()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = rs.getString(i + 1);
        }
        return ret;
    }

    private static Object[] arrayOfResultObj(ResultSet rs, int colCount, ResultSetMetaData rsm) throws SQLException {
        Object ret[] = new Object[colCount];
        for (int i = 0; i < ret.length; i++) {
            Object obj = extractObject(rs, rsm, i + 1);
            ret[i] = obj;
        }
        return ret;
    }

    /**
     * Interface to an object that contains a held Connection
     *
     * @author srl
     *
     */
    public interface ConnectionHolder {
        /**
         * @return alias to held connection
         */
        public Connection getConnectionAlias();
    }

    /**
     * Interface to an object that DBUtils.close can close.
     *
     * @author srl
     *
     */
    public interface DBCloseable {
        /**
         * Close this object
         */
        public void close() throws SQLException;
    }

    public static void writeCsv(ResultSet rs, Writer out) throws SQLException, IOException {
        ResultSetMetaData rsm = rs.getMetaData();
        int cc = rsm.getColumnCount();
        for (int i = 1; i <= cc; i++) {
            if (i > 1) {
                out.write(',');
            }
            WebContext.csvWrite(out, rsm.getColumnName(i).toUpperCase());
        }
        out.write('\r');
        out.write('\n');

        while (rs.next()) {
            for (int i = 1; i <= cc; i++) {
                if (i > 1) {
                    out.write(',');
                }
                String v;
                try {
                    v = rs.getString(i);
                } catch (SQLException se) {
                    if (se.getSQLState().equals("S1009")) {
                        v = "0000-00-00 00:00:00";
                    } else {
                        throw se;
                    }
                }
                if (v != null) {
                    if (rsm.getColumnType(i) == java.sql.Types.LONGVARBINARY) {
                        String uni = DBUtils.getStringUTF8(rs, i);
                        WebContext.csvWrite(out, uni);
                    } else {
                        WebContext.csvWrite(out, v);
                    }
                }
            }
            out.write('\r');
            out.write('\n');
        }
    }

    public static JSONObject getJSON(ResultSet rs) throws SQLException, IOException, JSONException {
        ResultSetMetaData rsm = rs.getMetaData();
        int cc = rsm.getColumnCount();
        JSONObject ret = new JSONObject();
        JSONObject header = new JSONObject();
        JSONArray data = new JSONArray();
        //JSONArray rsm2 = new JSONArray();

        int hasxpath = -1;
        int haslocale = -1;

        for (int i = 1; i <= cc; i++) {
            String colname = rsm.getColumnName(i).toUpperCase();
            final int columnType = rsm.getColumnType(i);
            if (colname.equals("XPATH") &&
                (columnType == java.sql.Types.INTEGER)) {
                hasxpath = i;
            }
            if (colname.equals("LOCALE"))
                haslocale = i;
            header.put(colname, i - 1);
            // rsm2.put(i-1, rsm.getColumnType(i));
        }
        int cn = cc;
        if (hasxpath >= 0) {
            header.put("XPATH_STRING", cn++);
            header.put("XPATH_STRHASH", cn++);
            header.put("XPATH_CODE", cn++);
        }
        if (haslocale >= 0) {
            header.put("LOCALE_NAME", cn++);
        }

        ret.put("header", header);
        //ret.put("types", rsm2);
        final STFactory stFactory = CookieSession.sm.getSTFactory();

        while (rs.next()) {
            JSONArray item = new JSONArray();
            Integer xpath = null;
            String locale_name = null;
            for (int i = 1; i <= cc; i++) {
                String v;
                try {
                    v = rs.getString(i);
                    if (i == hasxpath && v != null) {
                        xpath = rs.getInt(i);
                    }
                    if (i == haslocale && v != null) {
                        locale_name = CLDRLocale.getInstance(v).getDisplayName();
                    }
                } catch (SQLException se) {
                    if (se.getSQLState().equals("S1009")) {
                        v = "0000-00-00 00:00:00";
                    } else {
                        throw se;
                    }
                }
                if (v != null) {
                    int type = rsm.getColumnType(i);
                    switch (type) {
                    case java.sql.Types.LONGVARBINARY:
                        String uni = DBUtils.getStringUTF8(rs, i);
                        item.put(uni);
                        break;
                    case java.sql.Types.INTEGER:
                    case java.sql.Types.TINYINT:
                    case java.sql.Types.BIGINT:
                        item.put(rs.getInt(i));
                        break;
                    case java.sql.Types.TIMESTAMP:
                        item.put(rs.getTimestamp(i).getTime()); // truncates
                        break;
                    default:
                        item.put(v);
                    }
                } else {
                    item.put(false);
                }
            }
            if (hasxpath >= 0 && xpath != null) {
                final String xpathString = CookieSession.sm.xpt.getById(xpath);
                item.put(xpathString != null ? xpathString : ""); // XPATH_STRING
                item.put(xpathString != null ? (XPathTable.getStringIDString(xpathString)) : ""); // add
                // XPATH_STRHASH
                // column
                if (xpathString == null || xpathString.isEmpty()) {
                    item.put("");
                } else {
                    final PathHeader ph = stFactory.getPathHeader(xpathString);
                    if (ph != null) {
                        item.put(ph.toString()); // add XPATH_CODE
                    } else {
                        item.put("");
                    }
                }
            }
            if (haslocale >= 0 && locale_name != null) {
                item.put(locale_name); // add LOCALE_NAME column
            }
            data.put(item);
        }
        ret.put("data", data);
        return ret;
    }

    public static JSONObject queryToJSON(String string, Object... args) throws SQLException, IOException, JSONException {
        return queryToJSONLimit(null, string, args);
    }

    public static JSONObject queryToJSONLimit(Integer limit, String string, Object... args) throws SQLException, IOException, JSONException {
        if (limit != null && DBUtils.db_Mysql) {
            string = string + " limit " + limit;
        }
        Connection conn = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {
            conn = getInstance().getDBConnection();
            s = DBUtils.prepareForwardReadOnly(conn, string);
            setArgs(s, args);
            if (limit != null && !DBUtils.db_Mysql) {
                s.setMaxRows(limit);
            }
            rs = s.executeQuery();
            return getJSON(rs);
        } finally {
            close(rs, s, conn);
        }
    }

    private Map<String, Reference<JSONObject>> cachedJsonQuery = new ConcurrentHashMap<String, Reference<JSONObject>>();

    /**
     * Run a query, caching the JSON response
     * TODO: cache exceptions..
     * @param id
     * @param cacheAge
     * @param query
     * @param args
     * @return
     * @throws SQLException
     * @throws IOException
     * @throws JSONException
     */
    public static JSONObject queryToCachedJSON(String id, long cacheAge, String query, Object... args) throws SQLException, IOException, JSONException {
        if (SurveyMain.isSetup == false || SurveyMain.isBusted()) {
            return null;
        }

        /**
         * Debug the cachedJSON
         */
        final boolean CDEBUG = SurveyMain.isUnofficial() && CldrUtility.getProperty("CLDR_QUERY_CACHEDEBUG", false);
        DBUtils instance = getInstance(); // don't want the cache to be static
        Reference<JSONObject> ref = instance.cachedJsonQuery.get(id);
        JSONObject result = null;
        if (ref != null) result = ref.get();
        long now = System.currentTimeMillis();
        if (CDEBUG) {
            System.out.println("cachedjson: id " + id + " ref=" + ref + "res?" + (result != null));
        }
        if (result != null) {
            long age = now - (Long) result.get("birth");
            if (age > cacheAge) {
                if (CDEBUG) {
                    System.out.println("cachedjson: id " + id + " expiring because age " + age + " > " + cacheAge);
                }
                result = null;
            }
        }

        if (result == null) { // have to fetch it
            if (CDEBUG) {
                System.out.println("cachedjson: id " + id + " fetching: " + query);
            }
            result = queryToJSON(query, args);
            long queryms = System.currentTimeMillis() - now;
            result.put("birth", (Long) now);
            if (CDEBUG) {
                System.out.println("cachedjson: id " + id + " fetched in " + Double.toString(queryms / 1000.0) + "s");
            }
            result.put("queryms", (Long) (queryms));
            result.put("id", id);
            ref = new SoftReference<JSONObject>(result);
            instance.cachedJsonQuery.put(id, ref);
        }

        return result;
    }

    /**
     * Get the first row of the first column.  Useful when the query is very simple, such as a count.
     * @param obj
     * @return the int
     * @throws JSONException
     */
    public static final int getFirstInt(JSONObject json) throws JSONException {
        return json.getJSONArray("data").getJSONArray(0).getInt(0);
    }

    /**
     * query to an array associative maps
     * @param string
     * @param args
     * @return
     * @throws SQLException
     * @throws IOException
     */
    public static Map<String, Object>[] queryToArrayAssoc(String string, Object... args) throws SQLException, IOException {
        Connection conn = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {
            conn = getInstance().getDBConnection();
            s = DBUtils.prepareForwardReadOnly(conn, string);
            setArgs(s, args);
            rs = s.executeQuery();
            return resultToArrayAssoc(rs);
        } finally {
            close(rs, s, conn);
        }
    }

    /**
     * query to an array of arrays of objects
     * @param string
     * @param args
     * @return
     * @throws SQLException
     * @throws IOException
     */
    public static Object[][] queryToArrayArrayObj(String string, Object... args) throws SQLException, IOException {
        Connection conn = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {
            conn = getInstance().getDBConnection();
            s = DBUtils.prepareForwardReadOnly(conn, string);
            setArgs(s, args);
            rs = s.executeQuery();
            return resultToArrayArrayObj(rs);
        } finally {
            close(rs, s, conn);
        }
    }

    public static String getDbBrokenMessage() {
        final File homeFile = CLDRConfigImpl.homeFile;
        StringBuilder sb = new StringBuilder(
            "see <a href='http://cldr.unicode.org/development/running-survey-tool/cldr-properties/db'> http://cldr.unicode.org/development/running-survey-tool/cldr-properties/db </a>");

        if (homeFile == null) {
            sb.insert(0, "(can't find our home directory, either) ");
        } else {
            final File cldrDb = new File(homeFile, "cldrdb");
            try {
                String connectURI = "jdbc:derby:"
                    + (cldrDb.getCanonicalPath().replace(System.getProperty("file.separator").charAt(0), '/'));

                if (!cldrDb.exists()) {
                    // Easier to create it for them than to explain it.
                    String createURI = connectURI + ";create=true";
                    System.err.println("Attempting to create a DB at " + createURI);
                    Driver drv = (Driver) Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
                    DriverManager.registerDriver(drv);
                    Connection conn = DriverManager.getConnection(createURI);
                    conn.close();
                }

                // Try to print something helpful
                sb.insert(
                    0,
                    "</pre>Read the rest of this notice - for derby, add the following to <b>context.xml</b> file:  <br><br><pre class='graybox adminExceptionLogsite'>"
                        + "&lt;Resource name=\"jdbc/SurveyTool\" type=\"javax.sql.DataSource\" auth=\"Container\" \n"
                        + "description=\"database for ST\" maxActive=\"100\" maxIdle=\"30\" maxWait=\"10000\" \n"
                        + " username=\"\" password=\"\" driverClassName=\"org.apache.derby.jdbc.EmbeddedDriver\" \n"
                        + " url=\"<u>"
                        + connectURI
                        + "</u>\" /&gt;\n</pre>\n"
                        + " <i>Note: if you are on a Windows system, you may have to adjust the path somewhat.</i><br><pre>");

            } catch (Throwable e) {
                SurveyLog.logException(e, "Trying to help the user out with SQL stuff in " + cldrDb.getAbsolutePath());
                sb.insert(0, "(sorry, " + e.toString() + " trying to get you some better help text. )  ");
            }
        }

        return sb.toString();
    }

    public static java.sql.Timestamp sqlNow() {
        return new java.sql.Timestamp(new Date().getTime());
    }

    public static Integer getLastId(PreparedStatement s) throws SQLException {
        if (s == null) return null;
        ResultSet rs = s.getGeneratedKeys();
        if (!rs.next()) return null;
        return rs.getInt(1);
    }

    /**
     * Table name management.
     * Manage table names according to versions.
     */
    public enum Table {
        /* These constants represent names and other attributes of database tables.
         * 
         * The FORUM_POSTS(false, false) constructor makes isVersioned, hasBeta both false for
         * the FORUM_POSTS constant, as intended for new functionality, one forum for all versions.
         * See https://unicode.org/cldr/trac/ticket/10935
         * 
         * Other constants here have default constructor equivalent to (true, true).
         */
        VOTE_VALUE, VOTE_VALUE_ALT, VOTE_FLAGGED, FORUM_POSTS(false, false), REVIEW_HIDE, REVIEW_POST, IMPORT, IMPORT_AUTO;

        /**
         * Construct a Table constant with explicit parameters for isVersioned, hasBeta.
         * 
         * @param isVersioned true for tables whose name depends on version like cldr_vote_value_33,
         *                    false for tables whose name is version independent, like forum_posts
         * @param hasBeta true for tables whose name is different for beta versions like cldr_vote_value_32_beta,
         *                false for tables whose name doesn't change for beta.
         */
        Table(boolean isVersioned, boolean hasBeta) {
            this.isVersioned = isVersioned;
            this.hasBeta = hasBeta;
        }

        /**
         * Construct a Table constant with isVersioned, hasBeta both true.
         */
        Table() {
            this.isVersioned = true;
            this.hasBeta = true;
        }

        final boolean isVersioned, hasBeta;

        String defaultString = null;

        /**
         * High runner case.
         * WARNING: Do not use in constant strings
         */
        public synchronized String toString() {
            if (defaultString == null) {
                if (!SurveyMain.isConfigSetup && CLDRConfig.getInstance().getEnvironment() != CLDRConfig.Environment.UNITTEST) {
                    throw new InternalError("Error: don't use Table.toString before CLDRConfig is setup.");
                }
                defaultString = forVersion(SurveyMain.getNewVersion(), SurveyMain.phase() == Phase.BETA).toString();
            }
            return defaultString;
        }

        public CharSequence forVersion(String forVersion, boolean isBeta) {
            StringBuilder sb = new StringBuilder("cldr_");
            sb.append(name().toLowerCase());
            DBUtils.appendVersionString(sb, isVersioned ? forVersion : null, hasBeta ? isBeta : null);
            return sb;
        }
    }

    static boolean tryUpdates = true;

    /**
     *
     * @param rs
     * @param string
     * @param sqlnow
     * @return false if caller needs to 'manually' update the item.
     * @throws SQLException
     */
    public static final boolean updateTimestamp(ResultSet rs, String string, Timestamp sqlnow) throws SQLException {
        if (tryUpdates) {
            try {
                rs.updateTimestamp(string, sqlnow);
                return true; // success- caller doesn't need to do an update.
            } catch (SQLFeatureNotSupportedException sfns) {
                tryUpdates = false;
                SurveyLog.warnOnce("SQL: Apparently updates aren't supported: " + sfns.toString() + " - falling back.");
            }
        }
        return false; // caller needs to do an update
    }

    /**
     * Set an Integer object, either as an int or as a null
     * @param ps
     * @param i
     * @param withVote
     * @throws SQLException
     */
    public static void setInteger(PreparedStatement ps, int i, Integer withVote) throws SQLException {
        if (withVote == null) {
            ps.setNull(i, java.sql.Types.INTEGER);
        } else {
            ps.setInt(i, withVote);
        }
    }
}
