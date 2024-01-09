//
//  MailSender.java
//  cldrtools
//
//  Created by Steven R. Loomis on 3/24/2005.
//  Copyright 2005-2013 IBM. All rights reserved.
//
//  Rewritten to use JavaMail  http://java.sun.com/products/javamail/

package org.unicode.cldr.web;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.json.JSONException;
import org.json.JSONObject;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.EmailValidator;

/** Helper class. Sends mail with a simple interface */
public class MailSender implements Runnable {
    static final Logger logger = SurveyLog.forClass(MailSender.class);

    private static final String CLDR_MAIL = "cldr_mail";

    private static final String COUNTLEFTSQL =
            "select count(*) from " + CLDR_MAIL + " where sent_date is NULL and try_count < 3";

    private static final class MailConfig {
        final boolean CLDR_SENDMAIL;
        /** How many mails to process at a time? 0 = unlimited */
        final int CLDR_MAIL_BATCHSIZE;

        final int CLDR_MAIL_DELAY_EACH;
        final int CLDR_MAIL_DELAY_FIRST;
        /** How many seconds to wait between mails in batch processing? 0 = no delay */
        final int CLDR_MAIL_DELAY_BATCH_ITEM;

        static final MailConfig INSTANCE = new MailConfig();

        MailConfig() {
            CLDR_SENDMAIL = CLDRConfig.getInstance().getProperty("CLDR_SENDMAIL", true);
            CLDR_MAIL_DELAY_BATCH_ITEM =
                    CLDRConfig.getInstance().getProperty("CLDR_MAIL_DELAY_BATCH_ITEM", 0);
            CLDR_MAIL_BATCHSIZE = CLDRConfig.getInstance().getProperty("CLDR_MAIL_BATCHSIZE", 100);
            // for official use, give some time for ST to settle before starting on mail s ending.
            CLDR_MAIL_DELAY_FIRST =
                    CLDRConfig.getInstance()
                            .getProperty(
                                    "CLDR_MAIL_DELAY_FIRST", SurveyMain.isUnofficial() ? 5 : 60);
            // Check for outbound mail every 60 seconds
            CLDR_MAIL_DELAY_EACH = CLDRConfig.getInstance().getProperty("CLDR_MAIL_DELAY_EACH", 60);
        }
    }

    private UserRegistry.User getUser(int user) {
        if (user < 1) user = 1;
        UserRegistry.User u = CookieSession.sm.reg.getInfo(user);
        if (u == null || UserRegistry.userIsLocked(u) || UserRegistry.userIsExactlyAnonymous(u)) {
            return null;
        }
        return u;
    }

    private String getEmailForUser(int user) {
        UserRegistry.User u = CookieSession.sm.reg.getInfo(user);
        if (u == null || UserRegistry.userIsLocked(u) || UserRegistry.userIsExactlyAnonymous(u)) {
            return null;
        }
        if (!EmailValidator.passes(u.email)) {
            return null;
        }
        if (u.email.equals(UserRegistry.ADMIN_EMAIL)) {
            return null; // no mail to admin
        }
        return u.email;
    }

    public JSONObject getMailFor(int user) throws SQLException, IOException, JSONException {
        if (user == 1) { // for admin only
            return DBUtils.queryToJSON(
                    "select "
                            + USER
                            + ", id,subject,text,queue_date,read_date, post, locale, xpath, try_count, sent_date from "
                            + CLDR_MAIL
                            + " ORDER BY queue_date DESC");
        } else {
            return DBUtils.queryToJSON(
                    "select id,subject,text,queue_date,read_date, post, locale, xpath, try_count, sent_date from "
                            + CLDR_MAIL
                            + " where "
                            + USER
                            + "=? ORDER BY queue_date DESC",
                    user);
        }
    }

    /**
     * mark an item as read
     *
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
            if (conn == null) {
                return false;
            }
            ps =
                    DBUtils.prepareStatementWithArgs(
                            conn,
                            "update "
                                    + CLDR_MAIL
                                    + " set read_date=? where id=? and "
                                    + USER
                                    + "=?",
                            DBUtils.sqlNow(),
                            id,
                            user);
            if (ps.executeUpdate() == 1) {
                conn.commit();
                return true;
            } else {
                return false;
            }
        } catch (SQLException se) {
            SurveyLog.logException(logger, se, "mark ing as read id#" + id + " user=" + user);
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
        PreparedStatement s = null, s2 = null, s3 = null;
        try {
            conn = db.getDBConnection();
            if (conn == null) {
                return;
            }
            conn.setAutoCommit(false);
            if (!DBUtils.hasTable(CLDR_MAIL)) {
                logger.info("Creating " + CLDR_MAIL);
                s =
                        DBUtils.prepareStatementWithArgs(
                                conn,
                                "CREATE TABLE "
                                        + CLDR_MAIL
                                        + " (id INT NOT NULL "
                                        + DBUtils.DB_SQL_IDENTITY
                                        + ", " // PK:  id
                                        + USER
                                        + " int not null, " // userid TO
                                        + "sender int not null DEFAULT -1 , " // userid TO
                                        + "subject "
                                        + DBUtils.DB_SQL_MIDTEXT
                                        + " not null, " // mail subj
                                        + "cc varchar(8000) DEFAULT NULL,"
                                        + "text "
                                        + DBUtils.DB_SQL_UNICODE
                                        + " not null, " // email body
                                        + "queue_date "
                                        + DBUtils.DB_SQL_TIMESTAMP0
                                        + " not null , " // when queued?
                                        + "try_date "
                                        + DBUtils.DB_SQL_TIMESTAMP0
                                        + " DEFAULT NULL , " // when tried to send?
                                        + "try_count INT DEFAULT 0, " // count tried
                                        + "read_date "
                                        + DBUtils.DB_SQL_TIMESTAMP0
                                        + " DEFAULT NULL, " // when read by user in-app?
                                        + "sent_date "
                                        + DBUtils.DB_SQL_TIMESTAMP0
                                        + " DEFAULT NULL, " // when successfully sent?
                                        + "audit "
                                        + DBUtils.DB_SQL_MIDTEXT
                                        + " DEFAULT NULL , " // history
                                        + "post INT default NULL," // forum item
                                        + "locale varchar(127) DEFAULT NULL," // optional locale id
                                        + "xpath INT DEFAULT NULL "
                                        + (!DBUtils.db_Mysql ? ",primary key(id)" : "")
                                        + ")");
                s.execute();
                s2 =
                        DBUtils.prepareStatementWithArgs(
                                conn,
                                "INSERT INTO "
                                        + CLDR_MAIL
                                        + "("
                                        + USER
                                        + ",subject,text,queue_date) VALUES(?,?,?,?)",
                                1,
                                "Hello",
                                "Hello from the SurveyTool!",
                                DBUtils.sqlNow());
                s2.execute();
                conn.commit();
                logger.info("Setup " + CLDR_MAIL);
            }
            // set some defaults
            // these getters cause the properties (e.g. mail.host) to be set in the current
            // CLDRConfig.
            // More docs  at:
            // https://javaee.github.io/javamail/docs/api/com/sun/mail/smtp/package-summary.html
            Properties env = getProperties();
            env.getProperty("mail.smtp.connectiontimeout", "25");
            env.getProperty("mail.smtp.timeout", "25");

            // reap old items
            java.sql.Timestamp aWeekAgo =
                    new java.sql.Timestamp(
                            System.currentTimeMillis()
                                    - (1000 * 60 * 60 * 24 * 7
                                            * 3)); // reap mail after about 3 weeks
            s3 =
                    DBUtils.prepareStatementWithArgs(
                            conn, "delete from " + CLDR_MAIL + " where queue_date < ? ", aWeekAgo);

            int countDeleted = s3.executeUpdate();
            conn.commit();

            if (countDeleted > 0) {
                logger.info("MailSender:  reaped " + countDeleted + " expired messages");
            }

            if (!MailConfig.INSTANCE.CLDR_SENDMAIL) {
                SurveyLog.warnOnce(
                        logger,
                        "*** Mail processing disabled per cldr.properties. To enable, set CLDR_SENDMAIL=true ***");
            } else {
                final int firstTime = MailConfig.INSTANCE.CLDR_MAIL_DELAY_FIRST;
                final int eachTime = MailConfig.INSTANCE.CLDR_MAIL_DELAY_EACH;

                // The following line schedules MailSender.run() to be run periodically
                periodicThis =
                        SurveyThreadManager.getScheduledExecutorService()
                                .scheduleWithFixedDelay(
                                        this, firstTime, eachTime, TimeUnit.SECONDS);
                logger.info(
                        "Set up mail thread every "
                                + eachTime
                                + "s starting in "
                                + firstTime
                                + "s - waiting count = "
                                + DBUtils.sqlCount(COUNTLEFTSQL));
            }
        } catch (SQLException se) {
            SurveyMain.busted("Cant set up " + CLDR_MAIL, se);
        } finally {
            DBUtils.close(s, s2, s3, conn);
        }
    }

    ScheduledFuture<?> periodicThis = null;

    static void shutdown() {
        try {
            if (fInstance != null
                    && fInstance.periodicThis != null
                    && !fInstance.periodicThis.isCancelled()) {
                logger.info("Interrupting running mail thread");
                fInstance.periodicThis.cancel(true);
            } else {
                logger.info("MailSender not running");
            }
        } catch (Throwable t) {
            SurveyLog.logException(logger, t, "shutting down mailSender");
        }
    }

    private static MailSender fInstance = null;

    public static synchronized MailSender getInstance() {
        if (fInstance == null) {
            fInstance = new MailSender();
        }
        return fInstance;
    }

    public void queue(Integer fromUser, int toUser, String subject, String body) {
        queue(fromUser, toUser, subject, body, null, null, null, null);
    }

    public void queue(
            Integer fromUser,
            int toUser,
            String subject,
            String body,
            CLDRLocale locale,
            Integer xpath,
            Integer post,
            Set<Integer> cc) {
        String ccstr = buildCcStringFor(cc);
        if (fromUser == null || fromUser == UserRegistry.NO_USER) {
            fromUser = -1;
        }
        if (xpath != null && xpath == -1) {
            xpath = null;
        }
        final DBUtils dbutils = DBUtils.getInstance();
        try (Connection conn = dbutils.getDBConnection();
                PreparedStatement s2 =
                        DBUtils.prepareStatementWithArgs(
                                conn,
                                "INSERT INTO "
                                        + CLDR_MAIL
                                        + "(sender, "
                                        + USER
                                        + ",subject,text,queue_date,cc,locale,xpath,post) VALUES(?,?,?,?,?,?,?,?,?)",
                                fromUser,
                                toUser,
                                DBUtils.prepareUTF8(subject),
                                DBUtils.prepareUTF8(body),
                                DBUtils.sqlNow(),
                                ccstr,
                                locale,
                                xpath,
                                post)) {
            s2.execute();
            conn.commit();
            logger.info(
                    () -> String.format("%s : %s", "user#" + toUser, "Enqueued mail:" + subject));
        } catch (SQLException se) {
            SurveyLog.logException(logger, se, "Enqueuing mail to #" + toUser + ":" + subject);
            throw new InternalError(
                    "Failed to enqueue mail to " + toUser + " - " + se.getMessage());
        }
    }

    private String buildCcStringFor(Set<Integer> cc) {
        String ccstr = null;
        if (cc != null && !cc.isEmpty()) {
            StringBuilder sb = null;
            for (int u : cc) {
                String email = getEmailForUser(u);
                if (email == null) {
                    continue;
                }
                if (sb == null) {
                    sb = new StringBuilder();
                } else {
                    sb.append(", ");
                }
                sb.append('<');
                sb.append(email);
                sb.append('>');
            }
            ccstr = sb.toString();
        }
        return ccstr;
    }

    public void queue(
            int fromUser,
            Set<Integer> cc_emails,
            Set<Integer> bcc_emails,
            String subject,
            String body,
            CLDRLocale locale,
            int xpath,
            Integer post) {
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

    /** Footer to be placed at the bottom of emails */
    public static final String footer =
            "\n----------\n"
                    + "This email was generated automatically as part of the CLDR survey process\n"
                    + "http://www.unicode.org/cldr\n"
                    + "If you have any questions about it,\nplease contact your organization's CLDR Technical Committee member,\nor: surveytool at unicode.org\n"
                    + "TO UNSUBSCRIBE: You must permanently disable your account to stop receiving these emails."
                    + " In Survey Tool, log in and choose Lock My Account from the main menu."
                    + " You may request a password reset if needed.";

    private Properties getProperties() {

        // set up some presets
        return CLDRConfig.getInstance();
    }

    private int lastIdProcessed = -1; // spinner

    @Override
    public void run() {
        if (!MailConfig.INSTANCE.CLDR_SENDMAIL) {
            SurveyLog.warnOnce(
                    logger,
                    "*** Mail processing disabled per cldr.properties. To enable, set CLDR_SENDMAIL=true ***");
            return;
        }

        logger.finest("processing all mail…");
        Transport transport = processAllMail();
        try {
            if (transport != null) {
                transport.close();
            }
        } catch (MessagingException me) {
            logger.log(Level.WARNING, "Could not close transport", me);
        }
        logger.finer("MailSender.run() done");
    }

    /** Process all mail until it is done. */
    private Transport processAllMail() {
        logger.finer("Processing mail");

        int batchSize = 0;
        Session ourSession = getMailSession();
        Transport transport = null;

        for (; ; ) {
            final int countLeft = DBUtils.sqlCount(COUNTLEFTSQL);
            if (countLeft == 0) {
                logger.finer("No [more] mails");
                return transport;
            }

            ++batchSize;
            logger.fine(
                    String.format(
                            "MailSender: %d waiting mails [%d/%d] in batch",
                            countLeft, batchSize, MailConfig.INSTANCE.CLDR_MAIL_BATCHSIZE));
            if (MailConfig.INSTANCE.CLDR_MAIL_BATCHSIZE > 0
                    && batchSize > MailConfig.INSTANCE.CLDR_MAIL_BATCHSIZE) {
                logger.finer("Exitting, too many mails for batch");
                return transport;
            }
            transport = processOneMail(ourSession, transport);
            logger.finest(
                    () ->
                            String.format(
                                    "Sleep %ds", MailConfig.INSTANCE.CLDR_MAIL_DELAY_BATCH_ITEM));
            try {
                Thread.sleep(MailConfig.INSTANCE.CLDR_MAIL_DELAY_BATCH_ITEM * 1000L);
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "processAllMail() interrupted sleep", e);
            }
        }
    }

    private Transport processOneMail(Session ourSession, Transport transport) {
        DBUtils db = DBUtils.getInstance();
        java.sql.Timestamp sqlnow = DBUtils.sqlNow();

        try (Connection conn = db.getAConnection();
                PreparedStatement s =
                        DBUtils.prepareStatementWithArgsUpdateable(
                                conn,
                                "select * from "
                                        + CLDR_MAIL
                                        + " where sent_date is NULL and id > ? and try_count < 3 order by id "
                                        + (DBUtils.db_Mysql ? "limit 1" : ""),
                                lastIdProcessed);
                ResultSet rs = s.executeQuery()) {
            try {
                // Inner try while we still have the ResultSet
                if (!rs.next()) { // No mail to send.
                    resetLastIdProcessed();
                    logger.fine("No mail to check.");
                    return transport; // nothing to do
                }

                // Get the ID of the user
                lastIdProcessed = rs.getInt("id"); // update ID
                logger.fine("Processing id " + lastIdProcessed);

                // Make sure the user exists
                Integer to = rs.getInt("user");
                UserRegistry.User toUser = getUser(to);
                if (toUser == null) {
                    handleMissingUser(sqlnow, rs, to);
                    return transport;
                }

                // collect other fieds
                final int from = rs.getInt("sender");
                final UserRegistry.User fromUser = getUser(from); // Sending user, or SurveyTool
                final Timestamp queue_date = rs.getTimestamp("queue_date");
                final String subject = DBUtils.getStringUTF8(rs, "subject");
                final String text = DBUtils.getStringUTF8(rs, "text");

                MimeMessage ourMessage =
                        createMimeMessage(
                                ourSession, subject, to, toUser, from, fromUser, queue_date, text);

                transport = sendMimeMessage(ourSession, toUser, ourMessage, transport);

                updateMailStatus(sqlnow, rs, toUser);
            } catch (SQLException | MessagingException | UnsupportedEncodingException e) {
                handleMailException(sqlnow, rs, e);
            }
        } catch (SQLException se) {
            // We no longer have a ResultSet, etc. Just log.
            SurveyLog.logException(
                    logger, se, String.format("processing mail (last %d)", lastIdProcessed));
        }
        return transport;
    }

    private void handleMailException(java.sql.Timestamp sqlnow, ResultSet rs, Exception e)
            throws SQLException {
        // Catch these and mark the mail
        SurveyLog.logException(logger, e, "Trying to process mail id#" + lastIdProcessed);
        int badCount;
        rs.updateInt("try_count", (badCount = (rs.getInt("try_count") + 1)));
        rs.updateString("audit", e.getMessage() + e.getCause());
        rs.updateTimestamp("try_date", sqlnow);
        rs.updateRow();
        logger.finer(
                "Mail retry count of "
                        + badCount
                        + " updated: #id "
                        + lastIdProcessed
                        + "  - "
                        + e.getCause());
    }

    private void updateMailStatus(java.sql.Timestamp sqlnow, ResultSet rs, UserRegistry.User toUser)
            throws SQLException {
        if (!DBUtils.updateTimestamp(rs, "sent_date", sqlnow)) {
            SurveyMain.busted(
                    "Sorry, mail isn't supported without SQL update."
                            + " You may need to use a different database or JDBC driver.");
        } else {
            logger.fine("Mail: Row updated: #id " + lastIdProcessed + " to " + toUser);
            rs.updateRow();
        }
        logger.fine("Mail: updated: #id " + lastIdProcessed + " to " + toUser);
    }

    private Transport sendMimeMessage(
            Session ourSession,
            UserRegistry.User toUser,
            MimeMessage ourMessage,
            Transport transport)
            throws MessagingException {
        if (ourSession.getProperties().getProperty("mail.host", null) != null) {
            if (transport != null) {
                try {
                    if (!transport.isConnected()) {
                        logger.finest("reconnecting to " + transport);
                        transport.connect();
                    }
                } catch (MessagingException me) {
                    logger.log(Level.WARNING, "Could not reconnect transport", me);
                    transport = null;
                }
            }
            if (transport == null) {
                transport = ourSession.getTransport();
                logger.finest("re/got transport, connecting to " + transport.toString());
                transport.connect();
            }
            ourMessage.saveChanges();
            transport.sendMessage(ourMessage, ourMessage.getAllRecipients());
        } else {
            SurveyLog.warnOnce(
                    logger,
                    "Pretending to send mail - mail.host is not set. "
                            + "Browse to    https://st.unicode.org/cldr-apps/v#mail (or equivalent) to read the messages.");
        }

        logger.fine("Successful send of id " + lastIdProcessed + " to " + toUser);
        return transport;
    }

    private MimeMessage createMimeMessage(
            Session ourSession,
            final String subject,
            Integer to,
            UserRegistry.User toUser,
            final Integer from,
            final UserRegistry.User fromUser,
            final Timestamp queue_date,
            final String text)
            throws MessagingException, UnsupportedEncodingException, SQLException {
        MimeMessage ourMessage = new MimeMessage(ourSession);
        ourMessage.setSentDate(queue_date); // slices

        /*
         * The "from" address does not depend on the sender, due in part to problems with
         * authenticity checking schemes (such as DMARC). Instead it is a unicode.org
         * address, specific to the recipient user's organization, so that any bounce messages
         * get redirected (forwarded) back to the managers or tech-committee members for that
         * organization. Example: cldr-tc-reply-xyz@unicode.org, where xyz is replaced by
         * the organization name (lowercase, no spaces). CLDR_FROM in cldr.properties is obsolete.
         * References:
         *   https://unicode-org.atlassian.net/browse/CLDR-6334
         *   https://unicode-org.atlassian.net/browse/CLDR-10340
         */
        String fromEmail = "cldr-tc-reply-" + toUser.voterOrg() + "@unicode.org";
        ourMessage.setFrom(new InternetAddress(fromEmail, "CLDR SurveyTool"));

        InternetAddress toAddress;
        if (to > 1) {
            toAddress = new InternetAddress(toUser.email, toUser.name);
        } else {
            toAddress = new InternetAddress(fromEmail, "CLDR SurveyTool");
        }
        ourMessage.addRecipient(MimeMessage.RecipientType.TO, toAddress);

        ourMessage.addHeader("X-SurveyTool-To-User-Id", Integer.toString(to));
        ourMessage.addHeader("X-SurveyTool-Queue-Id", Integer.toString(lastIdProcessed));

        final String header =
                SurveyMain.isUnofficial()
                        ? " == UNOFFICIAL SURVEYTOOL  - This message was sent from a TEST machine== \n"
                        : "";
        ourMessage.setSubject(subject, "UTF-8");
        String theFrom = "";
        if (from >= 1) {
            ourMessage.addHeader("X-SurveyTool-From-User-Id", Integer.toString(from));
            theFrom =
                    "This message is being sent to you on behalf of "
                            + fromUser.name
                            + "\" <"
                            + fromUser.email
                            + "> ("
                            + fromUser.getOrganization().getDisplayName()
                            + ") - user #"
                            + from
                            + " \n";
        }
        ourMessage.setText(header + theFrom + text + footer, "UTF-8");
        return ourMessage;
    }

    private void handleMissingUser(java.sql.Timestamp sqlnow, ResultSet rs, Integer to)
            throws SQLException {
        String why;
        UserRegistry.User u = CookieSession.sm.reg.getInfo(to);
        if ((UserRegistry.userIsLocked(u) || UserRegistry.userIsExactlyAnonymous(u))) {
            why = "user " + u + " is locked or anonymous";
        } else {
            why = "user (#" + to + ") does not exist";
        }
        rs.updateInt("try_count", rs.getInt("try_count") + 999); // Don't retry.
        rs.updateString("audit", why);
        rs.updateTimestamp("try_date", sqlnow);
        rs.updateRow();
        logger.info("Abandoning mail id # " + lastIdProcessed + " because: " + why);
    }

    private void resetLastIdProcessed() {
        if (lastIdProcessed > 0) {
            // This will start again at the first message looking for unprocessed mail
            logger.finer("reset lastidprocessed to -1");
            lastIdProcessed = -1;
        }
    }

    private Session getMailSession() {
        // setup the session
        Properties env = getProperties();

        // Note: set mail.debug=true in cldr.properties to turn on session debugging.
        return getMailSession(env);
    }

    private Session getMailSession(Properties env) {
        return Session.getInstance(
                env,
                new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                env.getProperty("CLDR_SMTP_USER"),
                                env.getProperty("CLDR_SMTP_PASSWORD"));
                    }
                });
    }
}
