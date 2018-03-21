//
//  MailSender.java
//  cldrtools
//
//  Created by Steven R. Loomis on 3/24/2005.
//  Copyright 2005-2013 IBM. All rights reserved.
//
//  Rewritten to use JavaMail  http://java.sun.com/products/javamail/

package org.unicode.cldr.web;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.json.JSONException;
import org.json.JSONObject;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRLocale;

/**
 * Helper class. Sends mail with a simple interface
 */
public class MailSender implements Runnable {

    private long waitTill = 0;
    private static final String CLDR_MAIL = "cldr_mail";

    private static final String COUNTLEFTSQL = "select count(*) from " + CLDR_MAIL + " where sent_date is NULL and try_count < 3";

    public final boolean DEBUG = CLDRConfig.getInstance().getProperty("CLDR_DEBUG_MAIL", false) || (SurveyMain.isUnofficial() && SurveyLog.isDebug());
    public final boolean CLDR_SENDMAIL = CLDRConfig.getInstance().getProperty("CLDR_SENDMAIL", true);

    private UserRegistry.User getUser(int user) {
        if (user < 1) user = 1;
        UserRegistry.User u = CookieSession.sm.reg.getInfo(user);
        if (u == null || UserRegistry.userIsLocked(u)) {
            return null;
        }
        return u;
    }

    private String getEmailForUser(int user) {
        UserRegistry.User u = CookieSession.sm.reg.getInfo(user);
        if (u == null || UserRegistry.userIsLocked(u)) {
            return null;
        }

        if (u.email.equals("admin@")) {
            return null; // no mail to admin
        }

        return u.email;
    }

    public JSONObject getMailFor(int user) throws SQLException, IOException, JSONException {
        if (user == 1) { // for admin only
            return DBUtils.queryToJSON("select " + USER + ", id,subject,text,queue_date,read_date, post, locale, xpath, try_count, sent_date from " + CLDR_MAIL
                + " ORDER BY queue_date DESC");
        } else {
            return DBUtils.queryToJSON("select id,subject,text,queue_date,read_date, post, locale, xpath, try_count, sent_date from " + CLDR_MAIL + " where "
                + USER + "=? ORDER BY queue_date DESC", user);
        }
    }

    /**
     * mark an item as read
     * @param user
     * @param id
     * @return true if OK, false if err
     */
    public boolean setRead(int user, int id) {
        if (user == 1) {
            return true; // no effect on admin
        }
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            DBUtils db = DBUtils.getInstance();
            conn = db.getDBConnection();
            ps = DBUtils.prepareStatementWithArgs(conn, "update " + CLDR_MAIL + " set read_date=? where id=? and " + USER + "=?", DBUtils.sqlNow(), id, user);
            if (ps.executeUpdate() == 1) {
                conn.commit();
                return true;
            } else {
                return false;
            }
        } catch (SQLException se) {
            SurveyLog.logException(se, "mark ing as read id#" + id + " user=" + user);
            return false;
        } finally {
            DBUtils.close(ps, conn);
        }
    }

    final String USER;

    private MailSender() {
        DBUtils db = DBUtils.getInstance();
        USER = DBUtils.db_Mysql ? "user" : "to_user";
        Connection conn = null;
        PreparedStatement s = null, s2 = null;
        try {
            conn = db.getDBConnection();
            conn.setAutoCommit(false);
            DBUtils.getInstance();
            if (!DBUtils.hasTable(conn, CLDR_MAIL)) {
                System.out.println("Creating " + CLDR_MAIL);
                s = DBUtils.prepareStatementWithArgs(conn, "CREATE TABLE " + CLDR_MAIL + " (id INT NOT NULL " + DBUtils.DB_SQL_IDENTITY + ", " // PK:  id
                    + USER + " int not null, " // userid TO
                    + "sender int not null DEFAULT -1 , " // userid TO
                    + "subject " + DBUtils.DB_SQL_MIDTEXT + " not null, " // mail subj
                    + "cc varchar(8000) DEFAULT NULL,"
                    + "text " + DBUtils.DB_SQL_UNICODE + " not null, " // email body
                    + "queue_date " + DBUtils.DB_SQL_TIMESTAMP0 + " not null , " // when queued?
                    + "try_date " + DBUtils.DB_SQL_TIMESTAMP0 + " DEFAULT NULL , " // when tried to send?
                    + "try_count INT DEFAULT 0, " // count tried
                    + "read_date " + DBUtils.DB_SQL_TIMESTAMP0 + " DEFAULT NULL, " // when read by user in-app?
                    + "sent_date " + DBUtils.DB_SQL_TIMESTAMP0 + " DEFAULT NULL, " // when successfully sent?
                    + "audit " + DBUtils.DB_SQL_MIDTEXT + " DEFAULT NULL , " // history
                    + "post INT default NULL," // forum item
                    + "locale varchar(127) DEFAULT NULL," // optional locale id
                    + "xpath INT DEFAULT NULL "
                    + (!DBUtils.db_Mysql ? ",primary key(id)" : "") + ")");
                s.execute();
                s2 = DBUtils.prepareStatementWithArgs(conn, "INSERT INTO " + CLDR_MAIL + "(" + USER + ",subject,text,queue_date) VALUES(?,?,?,?)",
                    1, "Hello", "Hello from the SurveyTool!", DBUtils.sqlNow());
                s2.execute();
                conn.commit();
                System.out.println("Setup " + CLDR_MAIL);
            }
            // set some defaults
            Properties env = getProperties();
            env.getProperty("mail.host", env.getProperty("CLDR_SMTP", null));
            //  env.getProperty("mail.smtp.port", env.getProperty("CLDR_SMTP_PORT", "25"));
            env.getProperty("mail.smtp.connectiontimeout", "25");
            env.getProperty("mail.smtp.timeout", "25");

            // reap old items
            java.sql.Timestamp aWeekAgo = new java.sql.Timestamp(System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 7 * 3)); // reap mail after about 3 weeks
            s2 = db.prepareStatementWithArgs(conn, "delete from " + CLDR_MAIL + " where queue_date < ? ", aWeekAgo);

            int countDeleted = s2.executeUpdate();
            conn.commit();

            if (countDeleted > 0) {
                System.out.println("MailSender:  reaped " + countDeleted + " expired messages");
            }

            if (!CLDR_SENDMAIL) {
                SurveyLog.warnOnce("*** Mail processing disabled per cldr.properties. To enable, set CLDR_SENDMAIL=true ***");
            } else if (DBUtils.db_Derby) {
                SurveyLog.warnOnce("************* mail processing disabled for derby. Sorry. **************");
            } else {
                int firstTime = SurveyMain.isUnofficial() ? 5 : 60; // for official use, give some time for ST to settle before starting on mail s ending.
                int eachTime = 60; /* Check for outbound mail every 60 seconds */
                periodicThis = SurveyMain.getTimer().scheduleWithFixedDelay(this, firstTime, eachTime, TimeUnit.SECONDS);
                System.out.println("Set up mail thread every " + eachTime + "s starting in " + firstTime + "s - waiting count = "
                    + DBUtils.sqlCount(COUNTLEFTSQL));
            }
        } catch (SQLException se) {
            SurveyMain.busted("Cant set up " + CLDR_MAIL, se);
        } finally {
            DBUtils.close(s, s2, conn);
        }
    }

    ScheduledFuture<?> periodicThis = null;

    static void shutdown() {
        try {
            if (fInstance != null && fInstance.periodicThis != null && !fInstance.periodicThis.isCancelled()) {
                System.out.println("Interrupting running mail thread");
                fInstance.periodicThis.cancel(true);
            } else {
                System.out.println("MailSender not running");
            }
        } catch (Throwable t) {
            SurveyLog.logException(t, "shutting down mailSender");
        }
    }

    static private MailSender fInstance = null;

    public static synchronized MailSender getInstance() {
        if (fInstance == null) {
            fInstance = new MailSender();
        }
        return fInstance;
    }

    public void queue(Integer fromUser, int toUser, String subject, String body) {
        queue(fromUser, toUser, subject, body, null, null, null, null);
    }

    public void queue(Integer fromUser, int toUser, String subject, String body, CLDRLocale locale, Integer xpath, Integer post, Set<Integer> cc) {
        String ccstr = null;
        if (cc != null && !cc.isEmpty()) {
            StringBuilder sb = null;
            for (int u : cc) {
                if (sb == null) {
                    sb = new StringBuilder();
                } else {
                    sb.append(", ");
                }
                sb.append('<');
                sb.append(getEmailForUser(u));
                sb.append('>');
            }
            ccstr = sb.toString();
        }
        if (fromUser == null || fromUser == UserRegistry.NO_USER) {
            fromUser = -1;
        }
        if (xpath != null && xpath == -1) {
            xpath = null;
        }
        Connection conn = null;
        PreparedStatement s = null, s2 = null;
        try {
            DBUtils db = DBUtils.getInstance();
            conn = db.getDBConnection();
            final String sql = "INSERT INTO " + CLDR_MAIL + "(sender, " + USER + ",subject,text,queue_date,cc,locale,xpath,post) VALUES(?,?,?,?,?,?,?,?,?)";
            if (!DBUtils.db_Derby) { // hack around derby
                s2 = DBUtils.prepareStatementWithArgs(conn, sql,
                    fromUser, toUser, DBUtils.prepareUTF8(subject), DBUtils.prepareUTF8(body), DBUtils.sqlNow(),
                    ccstr, locale, xpath, post);

            } else {
                s2 = DBUtils.prepareStatementWithArgs(conn, sql,
                    fromUser, toUser, DBUtils.prepareUTF8(subject), DBUtils.prepareUTF8(body), DBUtils.sqlNow()); // just the ones that can't be null
                if (ccstr == null) {
                    s2.setNull(6, java.sql.Types.VARCHAR);
                } else {
                    s2.setString(6, ccstr);
                }
                if (locale == null) {
                    s2.setNull(7, java.sql.Types.VARCHAR);
                } else {
                    s2.setString(7, locale.getBaseName());
                }
                if (xpath == null) {
                    s2.setNull(8, java.sql.Types.INTEGER);
                } else {
                    s2.setInt(8, xpath);
                }
                if (post == null) {
                    s2.setNull(9, java.sql.Types.INTEGER);
                } else {
                    s2.setInt(9, xpath);
                }
            }
            s2.execute();
            conn.commit();
            log("user#" + toUser, "Enqueued mail:" + subject, null);
        } catch (SQLException se) {
            SurveyLog.logException(se, "Enqueuing mail to #" + toUser + ":" + subject);
            throw new InternalError("Failed to enqueue mail to " + toUser + " - " + se.getMessage());
        } finally {
            DBUtils.close(s, s2, conn);
        }
    }

    public void queue(int fromUser, Set<Integer> cc_emails, Set<Integer> bcc_emails, String subject, String body,
        CLDRLocale locale, int xpath, Integer post) {
        if (cc_emails != null) {
            for (int tocc : cc_emails) {
                queue(fromUser, tocc, subject, body, locale, xpath, post, cc_emails);
            }
        }
        if (bcc_emails != null) {
            for (int tobcc : bcc_emails) {
                queue(fromUser, tobcc, subject, body, locale, xpath, post, cc_emails);
            }
        }
    }

    /**
     * Internal function - write something to the log
     */
    public static synchronized void log(String to, String what, Throwable t) {
        try {
            OutputStream file = new FileOutputStream(SurveyMain.getSurveyHome() + "/cldrmail.log", true); // Append
            PrintWriter pw = new PrintWriter(file);

            pw.println(new Date().toString() + ": " + to + " : " + what);
            if (t != null) {
                pw.println(t.toString());
                t.printStackTrace(pw);
            }

            pw.close();
            file.close();
        } catch (IOException ioe) {
            System.err.println("MailSender::log:  " + ioe.toString() + " whilst processing " + to + " - " + what);
        }
    }

    /**
     * Footer to be placed at the bottom of emails
     */
    public static final String footer = "\n----------\n"
        + "This email was generated automatically as part of the CLDR survey process\n"
        + "http://www.unicode.org/cldr\n"
        + "If you have any questions about it,\nplease contact your organization's CLDR Technical Committee member,\nor: surveytool at unicode.org\n"
        + "TO UNSUBSCRIBE: You must permanently disable your account to stop receiving these emails. See: <http://st.unicode.org/cldr-apps/lock.jsp>";

    private Properties getProperties() {
        CLDRConfig env = CLDRConfig.getInstance();

        // set up some presets
        return env;
    }

    private int lastIdProcessed = -1; // spinner

    public void run() {
        if (!CLDR_SENDMAIL) {
            SurveyLog.warnOnce("*** Mail processing disabled per cldr.properties. To enable, set CLDR_SENDMAIL=true ***");
            return;
        }

        if (DBUtils.db_Derby) {
            SurveyLog.warnOnce("************* mail processing disabled for derby. Sorry. **************");
            return;
        }

//        if (System.currentTimeMillis() < waitTill) {
//            SurveyLog.warnOnce("************* delaying mail processing due to previous errors. **************");
//            return; // wait a bit
//        }

        System.out.println("MailSender: processing mail queue");
        String oldName = Thread.currentThread().getName();
        try {
            int countLeft = DBUtils.sqlCount(COUNTLEFTSQL);
            Thread.currentThread().setName("SurveyTool MailSender: waiting count=" + countLeft);
//            if (SurveyMain.isUnofficial()) {
            if (countLeft > 0) {
                if (DEBUG) System.err.println("MailSender: waiting mails: " + countLeft);
            } else {
                //if(DEBUG) System.err.println("Countleft: 0");
            }
//            }

            Connection conn = null;
            PreparedStatement s = null, s2 = null;
            ResultSet rs = null;
            Throwable badException = null;
            try {
                DBUtils db = DBUtils.getInstance();
                conn = db.getDBConnection();
                conn.setAutoCommit(false);
                java.sql.Timestamp sqlnow = DBUtils.sqlNow();
                s = DBUtils.prepareForwardUpdateable(conn, "select * from " + CLDR_MAIL + " where sent_date is NULL and id > ?  and try_count < 3 order by id "
                    + (DBUtils.db_Mysql ? "limit 1" : ""));
                s.setInt(1, lastIdProcessed);
                rs = s.executeQuery();

                if (rs.first() == false) {
                    if (lastIdProcessed > 0) {
                        if (DEBUG) {
                            System.out.println("reset lastidprocessed to -1");
                        }
                        lastIdProcessed = -1;
                    }
                    if (DEBUG) {
                        System.out.println("No mail to check.");
                    }
                    return; // nothing to do
                }

                Properties env = getProperties();

                Session ourSession = Session.getInstance(env, null);
                if (DEBUG) {
                    ourSession.setDebug(true);
                }

                try {

                    lastIdProcessed = rs.getInt("id"); // update ID
                    if (DEBUG) System.out.println("Processing id " + lastIdProcessed);
                    MimeMessage ourMessage = new MimeMessage(ourSession);

                    // from - sending user or surveytool
                    Integer from = rs.getInt("sender");
                    UserRegistry.User fromUser = getUser(from);

                    String all_from = env.getProperty("CLDR_FROM", "set_CLDR_FROM_in_cldr.properties@example.com");

                    if (false && from > 1) { // ticket:6334 - don't use individualized From: messages
                        ourMessage.setFrom(new InternetAddress(fromUser.email, fromUser.name + " (SurveyTool)"));
                    } else {
                        ourMessage.setFrom(new InternetAddress(all_from, "CLDR SurveyTool"));
                    }

                    // date
                    final Timestamp queue_date = rs.getTimestamp("queue_date");
                    ourMessage.setSentDate(queue_date); // slices

                    // to
                    Integer to = rs.getInt("user");
                    UserRegistry.User toUser = getUser(to);
                    if (toUser == null) {
                        String why;
                        int badCount;
                        UserRegistry.User u = CookieSession.sm.reg.getInfo(to);
                        if (u != null && UserRegistry.userIsLocked(u)) {
                            why = "user " + u + " is locked";
                        } else {
                            why = "user (#" + to + ") does not exist";
                        }
                        rs.updateInt("try_count", (badCount = (rs.getInt("try_count") + 999))); // Don't retry.
                        rs.updateString("audit", why);
                        rs.updateTimestamp("try_date", sqlnow);
                        rs.updateRow();
                        conn.commit();
                        System.out.println("Abandoning mail id # " + lastIdProcessed + " because: " + why);
                        processMail();
                        return;
                    } else if (to > 1) {
                        ourMessage.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(toUser.email, toUser.name));
                    } else {
                        ourMessage.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(all_from, "CLDR SurveyTool"));
                    }

                    String theFrom = "";
                    if (from >= 1) {
                        ourMessage.addHeader("X-SurveyTool-From-User-Id", Integer.toString(from));
                        theFrom = "This message is being sent to you on behalf of " + fromUser.name + "\" <" + fromUser.email + "> ("
                            + fromUser.getOrganization().getDisplayName() + ") - user #" + from + " \n";
                    }
                    ourMessage.addHeader("X-SurveyTool-To-User-Id", Integer.toString(to));
                    ourMessage.addHeader("X-SurveyTool-Queue-Id", Integer.toString(lastIdProcessed));

                    //            if (mailFromAddress != null) {
                    //                Address replyTo[] = { new InternetAddress(mailFromAddress, mailFromName + " (CLDR)") };
                    //                ourMessage.setReplyTo(replyTo);
                    //            }
                    final String header = SurveyMain.isUnofficial() ? " == UNOFFICIAL SURVEYTOOL  - This message was sent from a TEST machine== \n" : "";
                    ourMessage.setSubject(DBUtils.getStringUTF8(rs, "subject"), "UTF-8");
                    ourMessage.setText(header + theFrom + DBUtils.getStringUTF8(rs, "text") + footer, "UTF-8");

                    if (env.getProperty("CLDR_SMTP", null) != null) {
                        Transport.send(ourMessage);
                    } else {
                        SurveyLog
                            .warnOnce(
                                "Pretending to send mail - CLDR_SMTP is not set. Browse to    http://st.unicode.org/cldr-apps/v#mail (or equivalent) to read the messages.");
                    }

                    if (DEBUG) System.out.println("Successful send of id " + lastIdProcessed + " to " + toUser);

                    if (!DBUtils.updateTimestamp(rs, "sent_date", sqlnow)) {
                        SurveyLog.warnOnce("Sorry, mail isn't supported without SQL update. You may need to use a different database or JDBC driver.");
                        shutdown();
                        return;
                    } else {
                        if (DEBUG) System.out.println("Mail: Row updated: #id " + lastIdProcessed + " to " + toUser);
                        rs.updateRow();
                    }
                    if (DEBUG) System.out.println("Mail: do updated: #id " + lastIdProcessed + " to " + toUser);
                    conn.commit();
                    if (DEBUG) System.out.println("Mail: committed: #id " + lastIdProcessed + " to " + toUser);

                    // do more?
                    countLeft = DBUtils.sqlCount(COUNTLEFTSQL);
                    if (countLeft > 0) {
                        processMail();
                    }
                } catch (MessagingException mx) {
                    if (SurveyMain.isUnofficial()) {
                        SurveyLog.logException(mx, "Trying to process mail id#" + lastIdProcessed);
                    }
                    badException = mx;
                } catch (UnsupportedEncodingException e) {
                    if (SurveyMain.isUnofficial()) {
                        SurveyLog.logException(e, "Trying to process mail id#" + lastIdProcessed);
                    }
                    badException = e;
                }

                if (badException != null) {
                    int badCount = 0;
                    rs.updateInt("try_count", (badCount = (rs.getInt("try_count") + 1)));
                    rs.updateString("audit", badException.getMessage() + badException.getCause());
                    rs.updateTimestamp("try_date", sqlnow);
                    rs.updateRow();
                    conn.commit();
                    if (DEBUG) System.out.println("Mail retry count of " + badCount + " updated: #id " + lastIdProcessed + "  - " + badException.getCause());
                }
            } catch (SQLException se) {
                SurveyLog.logException(se, "processing mail");
                waitTill = System.currentTimeMillis() + (1000 * 60 * 5); // backoff 5 minutes
            } finally {
                DBUtils.close(rs, s, s2, conn);
            }
        } finally {
            Thread.currentThread().setName(oldName);
        }
    }

    private void processMail() {
        CookieSession.sm.getTimer().submit(this); // Cause a quick retry.
    }
}
