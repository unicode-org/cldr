/**
 * Copyright (C) 2010-2011 IBM Corporation and others. All Rights Reserved.
 */
package org.unicode.cldr.web;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.StackTracker;
import org.unicode.cldr.web.CLDRProgressIndicator.CLDRProgressTask;

/**
 * @author srl
 *
 */
public class UserSettingsData {
    public static final String SET_KINDS = "set_kinds";
    public static final String SET_VALUES = "set_values";

    CLDRProgressIndicator sm = null;

    private static final boolean debug = CldrUtility.getProperty("CLDR_SETTINGS_DEBUG", false);

    // End caller API

    // One of these per user. Caches the values.
    public class DBUserSettings extends UserSettings {
        private int id;

        HashMap<String, String> values = new HashMap<String, String>();

        /**
         * @return true - this type does persist
         */
        public boolean persistent() {
            return true;
        }

        @Override
        public int compareTo(UserSettings arg0) {
            if (!(arg0 instanceof DBUserSettings)) {
                return -1;
            }
            return id - ((DBUserSettings) arg0).id;
        }

        private DBUserSettings(int id) {
            this.id = id;
        }

        public int hashCode() {
            return id;
        }

        /**
         * Get a string, or the default
         *
         * @param name
         *            name of setting to get
         * @param defaultValue
         *            default value to return (may be null)
         * @return the result, or default
         */
        public String get(String name, String defaultValue) {
            try {
                String value = values.get(name);
                if (value == null) {
                    value = internalGet(id, name);
                }
                if (value == null) {
                    values.remove(name);
                    return defaultValue;
                } else {
                    values.put(name, value);
                    return value;
                }
            } catch (SQLException se) {
                se.printStackTrace();
                SurveyMain.busted("getting " + name + " for user id " + id + " : " + se.toString(), se);
                throw new InternalError("SQL err: " + DBUtils.unchainSqlException(se));
            }
        }

        /**
         * Set a string
         *
         * @param name
         *            should be ASCII
         * @param value
         *            may be any Unicode string
         */
        public void set(String name, String value) {
            String ivalue = values.get(name);
            if (value == null && ivalue == null) {
                return; // both null - OK.
            } else if (value != null && value.equals(ivalue)) { // allow set of null
                return; // already set.
            }

            try {
                Connection conn = null;
                try {
                    conn = DBUtils.getInstance().getDBConnection();
                    internalSet(id, name, value, conn);
                } finally {
                    DBUtils.closeDBConnection(conn);
                    if (value == null) {
                        values.remove(name);
                    } else {
                        values.put(name, value); // store for next time.
                    }
                    if (debug) {
                        System.out.println("DB Settings: Set value " + name + " = " + value + " for user " + id);
                    }
                }
            } catch (SQLException se) {
                se.printStackTrace();
                SurveyMain.busted("getting " + name + " for user id " + id + " : " + se.toString(), se);
                throw new InternalError("SQL err: " + DBUtils.unchainSqlException(se));
            }
        }

    }

    public UserSettingsData(CLDRProgressIndicator sm) throws SQLException {
        this.sm = sm;

        setupDB();
    }

    // DB stuff

    private void setupDB() throws SQLException {

        String sql = null;
        Connection conn = DBUtils.getInstance().getDBConnection();
        CLDRProgressTask progress = (sm != null) ? sm.openProgress("Setup " + UserSettingsData.class.getName() + " database")
            : null;
        try {

            if (!DBUtils.hasTable(conn, SET_KINDS)) {
                Statement s = conn.createStatement();
                if (progress != null)
                    progress.update("Creating table " + SET_KINDS);
                sql = ("create table " + SET_KINDS + "(set_id INT NOT NULL " + DBUtils.DB_SQL_IDENTITY + ", "
                    + "set_name varchar(128) not null UNIQUE " + (!DBUtils.db_Mysql ? ",primary key(set_id)" : "") + ")");
                s.execute(sql);
                if (DBUtils.hasTable(conn, SET_VALUES)) {
                    if (progress != null)
                        progress.update("Clearing old values");
                    sql = "drop table " + SET_VALUES;
                    s.execute(sql);
                }
                s.close();
                conn.commit();
            }

            if (!DBUtils.hasTable(conn, SET_VALUES)) {
                Statement s = conn.createStatement();
                if (progress != null)
                    progress.update("Creating table " + SET_VALUES);
                sql = ("create table " + SET_VALUES + "(usr_id INT NOT NULL, " + "set_id INT NOT NULL, " + "set_value "
                    + DBUtils.DB_SQL_UNICODE + " not null " + ",primary key(usr_id,set_id))");
                s.execute(sql);
                s.close();
                conn.commit();
            }
            DBUtils.closeDBConnection(conn);
            conn = null;
            if (progress != null)
                progress.update("done");
        } catch (SQLException se) {
            se.printStackTrace();
            System.err.println("SQL err: " + DBUtils.unchainSqlException(se));
            System.err.println("Last SQL run: " + sql);
            throw se;
        } finally {
            progress.close();
        }
    }

    private String internalGet(int id, String name) throws SQLException {
        Connection conn = DBUtils.getInstance().getDBConnection();

        String sql = "select " + SET_VALUES + ".set_value from " + SET_VALUES + "," + SET_KINDS + " where " + SET_VALUES
            + ".usr_id=? AND " + SET_KINDS + ".set_id=" + SET_VALUES + ".set_id AND " + SET_KINDS + ".set_name=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setInt(1, id);
            ps.setString(2, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String value = DBUtils.getStringUTF8(rs, 1);
                if (debug) {
                    System.out.println("DB Settings: GET  value " + name + " = " + value + " for user " + id);
                }
                return value;
            }
        } finally {
            ps.close();
            DBUtils.closeDBConnection(conn);
        }
        if (debug) {
            System.out.println("DB Settings: Get missing  value " + name + " = " + "NULL" + " for user " + id);
        }
        return null;
    }

    private void internalSet(int id, String name, String value, Connection conn) throws SQLException {
        String sql;
        int set_id = getSetId(name, conn);

        if (DBUtils.db_Mysql) { /* use 'on duplicate key' syntax */
            if (value == null) {
                sql = "DELETE FROM " + SET_VALUES + " WHERE usr_id=? AND set_id=?";
                PreparedStatement d0 = conn.prepareStatement(sql);

                d0.setInt(1, id);
                d0.setInt(2, set_id);

                d0.executeUpdate();
            } else {
                sql = "INSERT INTO " + SET_VALUES + " (usr_id,set_id,set_value) values (?,?,?) "
                    + "ON DUPLICATE KEY UPDATE set_value=?";
                PreparedStatement d0 = conn.prepareStatement(sql);

                d0.setInt(1, id);
                d0.setInt(2, set_id);
                DBUtils.setStringUTF8(d0, 3, value);
                DBUtils.setStringUTF8(d0, 4, value);

                d0.executeUpdate();
            }
        } else {
            sql = "DELETE FROM " + SET_VALUES + " where usr_id=? AND set_id=?";
            PreparedStatement d0 = conn.prepareStatement(sql);
            sql = "INSERT INTO " + SET_VALUES + " (usr_id,set_id,set_value) values (?,?,?)";
            PreparedStatement i1 = conn.prepareStatement(sql);

            d0.setInt(1, id);
            d0.setInt(2, set_id);
            i1.setInt(1, id);
            i1.setInt(2, set_id);
            DBUtils.setStringUTF8(i1, 3, value);
            d0.executeUpdate();
            if (value != null) {
                i1.executeUpdate();
            }
        }

        conn.commit();

        if (debug) {
            System.out.println("SET: " + name + " = " + value + " - " + StackTracker.currentStack());
        }
    }

    private int getSetId(String name, Connection conn) throws SQLException {
        String sql;
        synchronized (knownSettings) {
            Integer id = knownSettings.get(name);
            if (id == null) {
                sql = "SELECT set_id FROM " + SET_KINDS + " where set_name=?";
                PreparedStatement ps0 = conn.prepareStatement(sql);
                ps0.setString(1, name);
                ResultSet rs = ps0.executeQuery();
                if (rs.next()) {
                    id = rs.getInt(1);
                    if (debug)
                        System.err.println("set_kind: " + name + " = " + id);
                    knownSettings.put(name, id);
                } else {
                    sql = "INSERT INTO " + SET_KINDS + " (set_name) VALUES (?)";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, name);
                    if (debug)
                        System.err.println("set_kind Trying to add " + name);
                    ps.executeUpdate();

                    ResultSet rs1 = ps0.executeQuery();
                    if (rs1.next()) {
                        id = rs1.getInt(1);
                        if (debug)
                            System.err.println("set_kind: NOW " + name + " = " + id);
                        knownSettings.put(name, id);
                    } else {
                        SurveyMain.busted("Could not insert settings kind " + name);
                    }
                }
            }
            return id;
        }
    }

    /**
     * New up a UserSettingsData object.
     *
     * @param sm
     *            progress indicator (such as SurveyMain ) - can be null for no
     *            progress indication.
     * @return
     * @throws SQLException
     */
    public static UserSettingsData getInstance(CLDRProgressIndicator sm) throws SQLException {
        return new UserSettingsData(sm);
    }

    public UserSettings getSettings(int id) {
        if (id == -1) {
            return new EphemeralSettings();
        }
        synchronized (idToSettings) {
            UserSettings us = idToSettings.get(id);

            if (us == null) {
                us = new DBUserSettings(id);
                idToSettings.put(id, us);
            }
            return us;
        }
    }

    private Map<Integer, UserSettings> idToSettings = new HashMap<Integer, UserSettings>();
    private Map<String, Integer> knownSettings = new HashMap<String, Integer>();

}
