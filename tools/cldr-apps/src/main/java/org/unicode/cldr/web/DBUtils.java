/*
 * Copyright (C) 2004-2014 IBM Corporation and Others. All Rights Reserved.
 */
package org.unicode.cldr.web;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
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

import org.apache.ibatis.jdbc.ScriptRunner;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRConfig.Environment;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.StackTracker;
import org.unicode.cldr.web.SurveyMain.Phase;

import com.ibm.icu.dev.util.ElapsedTimer;

/**
 * Singleton utility class for simple(r) DB access.
 */
public class DBUtils {
    private static final String DEFAULT_DATA_SOURCE = "java:comp/DefaultDataSource";
    private static final boolean DEBUG = true;// CldrUtility.getProperty("TEST",
    // false);

    private static final boolean DEBUG_SQL = false; // show "all" SQL
    private static final String JDBC_SURVEYTOOL = ("jdbc/SurveyTool");
    private static DataSource datasource = null;
    private String connectionUrl = null;
    // DB stuff
    public static String db_driver = null;
    public static String db_protocol = null;
    public static String CLDR_DB_U = null;
    public static String CLDR_DB_P = null;
    public static String cldrdb_u = null;
    public static String CLDR_DB;
    // public static String cldrdb = null;
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
    /**
     * the StackTracker can track unclosed connections
     */
    private static final StackTracker tracker = DEBUG
        ? new StackTracker() : null;
    public Appendable stats(Appendable output) throws IOException {
        return output.append("DBUtils: currently open: " + db_number_open).append(", max open: " + db_max_open)
            .append(", total used: " + db_number_used);
    }

    public Appendable statsShort(Appendable output) throws IOException {
        return output.append("" + db_number_open).append("/" + db_max_open);
    }

    /**
     * Close a connection.
     * Note that Connection is AutoClosable, so Connnections may be closed without going through this
     * function. In other words, with newer call sites, the db_number_open metrics may be off.
     * @param conn
     */
    public static void closeDBConnection(Connection conn) {
        if (conn != null) {
            if (SurveyMain.isUnofficial() && tracker != null) {
                tracker.remove(conn);
            }
            try {
                if (db_Derby &&
                    !conn.isClosed() &&
                    !conn.getAutoCommit()) {
                    conn.commit();
                    // commit on close if we are using Derby directly
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
            int j = (b) & 0xff;
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
            int j = (b) & 0xff;
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
            int j = (b) & 0xff;
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

    public final static DBUtils getInstance() {
        return DBUtilsHelper.SINGLETON;
    }

    /**
     * For testing use, injecting a specific data source.
     * For production use, call getInstance().
     */
    public static void makeInstanceFrom(DataSource dataSource2, String url) {
        System.err.println("DBUtils: Note: changing the DBUtils singleton instance to " + dataSource2 + " @ " + url);
        DBUtilsHelper.SINGLETON = new DBUtils(dataSource2, url);
    }

    private static final class DBUtilsHelper {
        static DBUtils SINGLETON = new DBUtils();
        static void shutdown(DBUtils fromThis) {
            if(SINGLETON != fromThis) {
                throw new RuntimeException("DBUtils Shutdown with some other instance");
            } else {
                SINGLETON = null;
            }
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
        getInstance();
        return DBUtils.hasTable(table);
    }

    @Deprecated
    private boolean hasTableDerby(String table) {
        String canonName = canonTableName(table);
        // TODO: OLD TO REMOVE
        try {
            Connection conn = getDBConnection();
            ResultSet rs = null;
            Statement s = null;
            try {
                DatabaseMetaData dbmd = conn.getMetaData();
                rs = dbmd.getTables(null, null, canonName, null);

                if (rs.next() == true) {
                    if (DEBUG) SurveyLog.warnOnce("table " + canonName + " already existed.");
                    return true;
                } else {
                    SurveyLog.warnOnce("table " + canonName + " did not exist.");
                    return false;
                }
            } finally {
                DBUtils.close(s, rs, conn);
            }
        } catch (SQLException se) {
            SurveyMain.busted("While looking for table '" + table + "': ", se);
            return false; // NOTREACHED
        }
        // TODO: OLD TO REMOVE
    }

    public final static boolean hasTable(String table) {
        return getInstance().tableExists(table);
    }

    public boolean tableExists(String table) {
        if (db_Derby) return hasTableDerby(table);

        String canonName = canonTableName(table);
        try (Connection conn = DBUtils.getInstance().getAConnection();
            ResultSet rs = conn.getMetaData().getTables(null, null, canonName, null)) {
            final boolean hadTable = (rs.next() == true);

            if (hadTable) {
                // flush remaining rows
                while (rs.next())
                    ;
            }

            if (hadTable) {
                if (DEBUG) SurveyLog.warnOnce("table " + canonName + " already existed.");
                return true;
            } else {
                SurveyLog.warnOnce("table " + canonName + " did not exist.");
                return false;
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
        try (Connection conn = DBUtils.getInstance().getAConnection()) {
            return sqlCount(conn, sql, args);
        } catch (SQLException e) {
            SurveyMain.busted("sqlCount", e);
            return -1;
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
            ArrayList<String[]> al = new ArrayList<>();
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
        return !hasDataSource();
    }

    /**
     * Wrapper for getting InitialContext
     * @return
     */
    static InitialContext getInitialContext() {
        InitialContext context;
        try {
            context = new InitialContext();
            return context;
        } catch (NamingException e) {
            SurveyMain.busted("Could not get InitialContext", e);
            throw new RuntimeException("Cannot Get InitialContext", e);
        }
    }


    private DBUtils() {
        if(CLDRConfig.getInstance().getEnvironment() == Environment.UNITTEST) {
            System.err.println("NOT initializing datasource: UNITTEST environment. Must call DBUtils.makeInstanceFrom() before DB operations will work."); //  makeInstanceFrom() must be called.
            return;
        }
        // Initialize DB context
        System.err.println("Loading datasource: java:comp/env " + JDBC_SURVEYTOOL);
        ElapsedTimer et = new ElapsedTimer();

        try {
            final DataSource myOtherDB = InitialContext.doLookup(DEFAULT_DATA_SOURCE);

            if(myOtherDB != null) {
                System.out.println("DefaultDataSource was ok");
                datasource = myOtherDB;
            } else {
                System.err.println("DefaultDataSource was not OK");
                Context initialContext = getInitialContext();
                Context eCtx = (Context) initialContext.lookup("java:comp/env");
                datasource = (DataSource) eCtx.lookup(JDBC_SURVEYTOOL);
            }

            if (datasource != null) {
                System.out.println("Got datasource: " + datasource.toString() + " in " + et);
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

    /**
     * Constructor for DBUtils.
     * @param dataSource2 DataSource to be used
     * @param curl For TESTS ONLY: URL for connection
     */
    public DBUtils(DataSource dataSource2, String curl) {
        datasource = dataSource2;
        Connection c = null;
        try {
            if (datasource != null) {
                c = datasource.getConnection();
            } else if(curl != null && !curl.isEmpty()) {
                this.connectionUrl = curl;
                c = getDBConnection();
                DatabaseMetaData dmd = c.getMetaData();
                dbInfo = dmd.getDatabaseProductName() + " v" + dmd.getDatabaseProductVersion();
                setupSqlForServerType();
            } else {
                throw new NullPointerException("DBUtils(): DataSource and URL are both null/empty");
            }
            DatabaseMetaData dmd = c.getMetaData();
            dbInfo = dmd.getDatabaseProductName() + " v" + dmd.getDatabaseProductVersion();
            setupSqlForServerType();
            c.setAutoCommit(false);
            boolean autoCommit = c.getAutoCommit();
            if (autoCommit == true) {
                throw new IllegalArgumentException("autoCommit was true, expected false. Check your configuration.");
            }
            SurveyLog.debug("Metadata: " + dbInfo + ", autocommit: " + autoCommit);
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
        System.out.println("setting up SQL for database type " + dbInfo);
        if (dbInfo.contains("Derby")) {
            db_Derby = true;
            System.out.println("Note: Derby (embedded) mode. ** some features may not work as expected **");
            db_UnicodeType = java.sql.Types.VARCHAR;
        } else if (dbInfo.contains("MySQL")) {
            System.out.println("Note: MySQL mode");
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
            System.out.println("DBUtils: removing my instance. " + DBUtils.db_number_open + " connections still open?\n" + tracker);
            System.out.println("(Note: AutoClosed connections may not be calculated properly.)");
        }
        if (tracker != null)
            tracker.clear();
        DBUtilsHelper.shutdown(this);
    }

    long lastMsg = -1;
    private int db_max_open = 0;


    /**
     * Returns an AutoCommit=false connection.
     * This connection MAY NOT be held in an object. Hold it and then close it ( DBUtils.close() )
     * @return
     */
    public final Connection getDBConnection() {
        Connection c = getAConnection();
        try {
            c.setAutoCommit(false);
        } catch (SQLException se) {
            se.printStackTrace();
            SurveyMain.busted("Fatal in getDBConnection", se);
            return null;
        }
        return c;
    }

    /**
     * Get an Autocommit Connection. Will be AutoCommit=true
     * @return
     */
    public final Connection getAConnection() {
        try {
            if(connectionUrl != null) {
                Connection c = getDBConnectionFor(connectionUrl);
                c.setAutoCommit(true);
                return c;
            }
            return datasource.getConnection();
        } catch (SQLException se) {
            se.printStackTrace();
            SurveyMain.busted("Fatal in getConnection()", se);
            return null;
        }
    }

    /**
     * For TESTS only
     * @param connectionUrl
     * @return
     */
    private static Connection getDBConnectionFor(final String connectionUrl) {
        try {
            return DriverManager.getConnection(connectionUrl);
        } catch (SQLException e) {
            throw new RuntimeException("getConnection() failed for url", e);
        }
    }

    public void validateDatasourceExists(SurveyMain sm, CLDRProgressIndicator.CLDRProgressTask progress) {
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
                    // Here's the problem.
                    // https://commons.apache.org/proper/commons-dbcp/api-1.2.2/org/apache/commons/dbcp/BasicDataSource.html#close()
                    // We do NOT link to this library directly, but Derby may use it.
                    // It's not AutoClosable.
                    // But, we want to be able to close it if possible.
                    final Class<? extends Object> theClass = o.getClass();
                    final String simpleName = theClass.getSimpleName();
                    if (simpleName.equals("BasicDataSource")) { // could expand this later, if we want to generically call close()
                        // We check the class name to verify that we aren't
                        try {
                            // try to find a "close"
                            final Method m = theClass.getDeclaredMethod("close");
                            if (m != null) {
                                System.err.println("Attempting to call close() on " + theClass.getName());
                                m.invoke(o);
                            }
                        } catch (Exception nsm) {
                            nsm.printStackTrace();
                            System.err.println("Caught exception " + nsm + " - so, don't know how to close " + simpleName + " "
                                + theClass.getName());
                        }
                    } else {
                        throw new IllegalArgumentException("Don't know how to close " + simpleName + " "
                            + theClass.getName());
                    }
                }
            } catch (SQLException e) {
                System.err.println(unchainSqlException(e));
            }
        }
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
        ArrayList<String[]> al = new ArrayList<>();
        while (rs.next()) {
            al.add(arrayOfResult(rs));
        }
        return al.toArray(new String[al.size()][]);
    }

    public static Object[][] resultToArrayArrayObj(ResultSet rs) throws SQLException {
        ArrayList<Object[]> al = new ArrayList<>();
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
        ArrayList<Map<String, Object>> al = new ArrayList<>();
        while (rs.next()) {
            al.add(assocOfResult(rs, rsm));
        }
        return al.toArray(new Map[al.size()]);
    }

    public static Map<String, Object> assocOfResult(ResultSet rs) throws SQLException {
        return assocOfResult(rs, rs.getMetaData());
    }

    private static Map<String, Object> assocOfResult(ResultSet rs, ResultSetMetaData rsm) throws SQLException {
        Map<String, Object> m = new HashMap<>(rsm.getColumnCount());
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

    public static void writeCsv(final String query, Writer out) throws SQLException, IOException {
        Connection conn = null;
        ResultSet rs = null;
        try {
            conn = DBUtils.getInstance().getDBConnection();
            rs = DBUtils.prepareForwardReadOnly(conn, query).executeQuery();
            writeCsv(rs, out);
        } catch (java.sql.SQLException se) {
            SurveyLog.logException(se, "running csv: " + se);
            throw se;
        } finally {
            DBUtils.close(rs, conn);
        }
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

    private Map<String, Reference<JSONObject>> cachedJsonQuery = new ConcurrentHashMap<>();

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
            ref = new SoftReference<>(result);
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
        return
            "</pre><script src='../js/cldr-setup.js'></script>" +
            "see <a href='http://cldr.unicode.org/development/running-survey-tool/cldr-properties/db'> http://cldr.unicode.org/development/running-survey-tool/cldr-properties/db </a>" +
            "For MySQL, click: <button onclick='return mysqlhelp()'>MySQL Configurator</button><pre>";
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
         * The (false, false) constructor makes isVersioned, hasBeta both false for
         * the FORUM_POSTS and FORUM_TYPES tables: these tables are not version-specific.
         *
         * The LOCKED_XPATHS(false, true) constructor makes isVersioned, hasBeta both false.
         *
         * Other constants here have default constructor equivalent to (true, true).
         */
        VOTE_VALUE, VOTE_VALUE_ALT, VOTE_FLAGGED, FORUM_POSTS(false, false), FORUM_TYPES(false, false),
        REVIEW_HIDE, REVIEW_POST, IMPORT, IMPORT_AUTO, LOCKED_XPATHS(false, false);

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
        @Override
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

    /**
     * Run a SQL script
     * @param sqlName path to script, will be in resource folder org.unicode.cldr.web.sql
     * @throws IOException
     * @throws SQLException
     */
    public static void execSql(String sqlName) throws IOException, SQLException {
        System.err.println("Running SQL:  sql/"+sqlName);
        try (Connection conn = getInstance().getDBConnection();
            InputStream s = DBUtils.class.getResourceAsStream("sql/"+sqlName);
            Reader r = new InputStreamReader(s);) {
            ScriptRunner runner = new ScriptRunner(conn);
            runner.runScript(r);
            System.err.println("SQL OK: sql/"+sqlName);
        }
    }
}
