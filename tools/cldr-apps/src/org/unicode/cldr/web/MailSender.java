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
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.json.JSONException;
import org.json.JSONObject;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.StackTracker;

/**
 * Helper class. Sends mail with a simple interface
 */
public class MailSender implements Runnable {
    private static final int countLimit = 25;
    private static final int sendDelay = 5000;
    
    private static final String CLDR_MAIL = "cldr_mail";
    
    private static final String COUNTLEFTSQL = "select count(*) from " + CLDR_MAIL + " where sent_date is NULL and try_count < 3";

    private UserRegistry.User getUser(int user) {
        if(user<1) user=1;
        UserRegistry.User u = CookieSession.sm.reg.getInfo(user);
        if(u==null || UserRegistry.userIsLocked(u)) {
            return null;
        }
        return u;
    }
    
    private String getEmailForUser(int user) {
        UserRegistry.User u = CookieSession.sm.reg.getInfo(user);
        if(u==null || UserRegistry.userIsLocked(u)) {
            return null;
        }
        
        if(u.email.equals("admin@")) {
            return null; // no mail to admin
        }
        
        return u.email;
    }
    
    public JSONObject getMailFor(int user) throws SQLException, IOException, JSONException {
        if(user==1) { // for admin only
            return DBUtils.queryToJSON("select user, id,subject,text,queue_date,read_date, post, locale, xpath, try_count, sent_date from " + CLDR_MAIL + " ORDER BY queue_date DESC");
        } else {
            return DBUtils.queryToJSON("select id,subject,text,queue_date,read_date, post, locale, xpath, try_count, sent_date from " + CLDR_MAIL + " where user=? ORDER BY queue_date DESC", user);
        }
    }
    
    /**
     * mark an item as read
     * @param user
     * @param id
     * @return true if OK, false if err
     */
    public boolean setRead(int user, int id) {
        if(user==1) {
            return true; // no effect on admin
        }
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            DBUtils db = DBUtils.getInstance();
            conn = db.getDBConnection();
            ps = DBUtils.prepareStatementWithArgs(conn, "update " + CLDR_MAIL + " set read_date=? where id=? and user=?",  DBUtils.sqlNow(), id, user);
            if(ps.executeUpdate()==1) {
                conn.commit();
                return true;
            } else {
                return false;
            }
        } catch (SQLException se) {
            SurveyLog.logException(se, "mark ing as read id#"+id+" user="+user);
            return false;
        } finally {
            DBUtils.close(ps,conn);
        }
    }
    
    private MailSender() {
        Connection conn = null;
        PreparedStatement s = null, s2=null;
        try {
            DBUtils db = DBUtils.getInstance();
            conn = db.getDBConnection();
            conn.setAutoCommit(false);
            if(!DBUtils.getInstance().hasTable(conn, CLDR_MAIL)) {
                    System.out.println("Creating " + CLDR_MAIL);
                    s  = db.prepareStatementWithArgs(conn, "CREATE TABLE "+CLDR_MAIL+" (id INT NOT NULL " + DBUtils.DB_SQL_IDENTITY + ", "   // PK:  id
                            + "user int not null, " // userid TO
                            + "sender int not null DEFAULT -1 , " // userid TO
                            + "subject " + DBUtils.DB_SQL_UNICODE + " not null, " // mail subj
                            + "cc varchar(16384) DEFAULT NULL," 
                            + "text " + DBUtils.DB_SQL_UNICODE + " not null, " // email body
                            +  "queue_date DATETIME not null , "  // when queued?
                            +  "try_date DATETIME DEFAULT NULL , "  // when tried to send?
                            + "try_count INT DEFAULT 0, " // count tried
                            + "read_date DATETIME DEFAULT NULL, "  // when read by user in-app?
                            + "sent_date DATETIME DEFAULT NULL, "  // when successfully sent?
                            + "audit varchar(16384) DEFAULT NULL , " // history
                            + "post INT default NULL," // forum item
                            + "locale varchar(127) DEFAULT NULL," // optional locale id
                            + "xpath INT DEFAULT NULL "
                            +   (!DBUtils.db_Mysql ? ",primary key(id)" : "") + ")");
                            s.execute();
                       s2 = db.prepareStatementWithArgs(conn, "INSERT INTO " + CLDR_MAIL + "(user,subject,text,queue_date) VALUES(?,?,?,?)", 
                                   1, "Hello", "Hello from the SurveyTool!", DBUtils.sqlNow());
                       s2.execute();
                       conn.commit();
                       System.out.println("Setup " + CLDR_MAIL);
            }
            // set some defaults
            Properties env = getProperties();
            env.getProperty("mail.host", env.getProperty("CLDR_SMTP",null));
            //  env.getProperty("mail.smtp.port", env.getProperty("CLDR_SMTP_PORT", "25"));
            env.getProperty("mail.smtp.connectiontimeout", "25");
            env.getProperty("mail.smtp.timeout", "25");
            
            // reap old items
            java.sql.Timestamp aWeekAgo = new java.sql.Timestamp(System.currentTimeMillis() -  (1000 * 60 * 60 * 24 * 7 * 3 )); // reap mail after about 3 weeks
            s2 = db.prepareStatementWithArgs(conn, "delete from " + CLDR_MAIL +" where queue_date < ? ", aWeekAgo);
            
            int countDeleted = s2.executeUpdate();
            conn.commit();
            
            if(countDeleted > 0) {
                System.out.println("MailSender:  reaped " + countDeleted + " expired messages");
            }
            
            int firstTime = 5;
            int eachTime = 6; // 63;
            periodicThis = SurveyMain.getTimer().scheduleWithFixedDelay(this, firstTime, eachTime, TimeUnit.SECONDS);
            System.out.println("Set up mail thread every " + eachTime + "s starting in " + firstTime + "s - waiting count = " + db.sqlCount(COUNTLEFTSQL));
        } catch(SQLException se) {
            SurveyMain.busted("Cant set up " + CLDR_MAIL, se);
        } finally {
            DBUtils.close(s, s2, conn);
        }
    }
    
    ScheduledFuture<?> periodicThis = null;

    static void shutdown() {
        try {
            if (fInstance != null && fInstance.periodicThis!=null && !fInstance.periodicThis.isCancelled()) {
                System.out.println("Interrupting running mail thread");
                fInstance.periodicThis.cancel(true);
            } else {
                System.out.println("MailSender not running");
            }
        } catch (Throwable t) {
            SurveyLog.logException(t, "shutting down mailSender");
        }
    }

    private boolean KEEP_ALIVE = true;
    // int port = Integer.getInteger(CLDR_SMTP_PORT);

    static private MailSender fInstance = null;

    public static synchronized MailSender getInstance() {
        if (fInstance == null) {
            fInstance = new MailSender();
        }
        return fInstance;
    }
    
    public void queue(Integer fromUser, int toUser, String subject, String body) {
        queue(fromUser, toUser,subject,body,null,null,null,null);
    }
    public void queue(Integer fromUser, int toUser, String subject, String body, CLDRLocale locale, Integer xpath, Integer post, Set<Integer> cc) {
        String ccstr = null;
        if(cc != null && !cc.isEmpty()) {
            StringBuilder sb = null;
            for(int u : cc) {
                if(sb == null) {
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
        if(fromUser == null || fromUser == UserRegistry.NO_USER) { 
            fromUser = -1;
        }
        if(xpath!=null && xpath == -1) {
            xpath = null;
        }
        Connection conn = null;
        PreparedStatement s = null, s2=null;
        try {
            DBUtils db = DBUtils.getInstance();
            conn = db.getDBConnection();
            s2 = db.prepareStatementWithArgs(conn, "INSERT INTO " + CLDR_MAIL + "(sender, user,subject,text,queue_date,cc,locale,xpath,post) VALUES(?,?,?,?,?,?,?,?,?)", 
                    fromUser, toUser, DBUtils.prepareUTF8(subject), DBUtils.prepareUTF8(body), DBUtils.sqlNow(), ccstr, locale, xpath, post);
            s2.execute();
            conn.commit();
            log("user#"+toUser,"Enqueued mail:"+subject,null);
        } catch(SQLException se) {
            SurveyLog.logException(se,"Enqueuing mail to #"+toUser+":"+subject);
            throw new InternalError("Failed to enqueue mail to " + toUser + " - " + se.getMessage());
        } finally {
            DBUtils.close(s, s2, conn);
        }
    }

    public void queue(int fromUser, Set<Integer> cc_emails, Set<Integer> bcc_emails, String subject, String body,
            CLDRLocale locale, int xpath, Integer post) {
        if(cc_emails != null) {
            for(int tocc: cc_emails ) {
                queue(fromUser, tocc, subject, body, locale, xpath, post, cc_emails);
            }
        }
        if(bcc_emails != null) {
            for(int tobcc: bcc_emails ) {
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
            + "If you have any questions about it,\nplease contact your organization's CLDR Technical Committee member,\nor: surveytool@unicode.org\n"
            + "TO UNSUBSCRIBE: You must permanently disable your account to stop receiving these emails. See: <http://st.unicode.org/cldr-apps/lock.jsp>";

      private Properties getProperties() {
          CLDRConfig env = CLDRConfig.getInstance();

          // set up some presets
        return env;
    }

//    private synchronized void queue(MimeMessage ourMessage) throws MessagingException {
//        messageQueue.addFirst(ourMessage);
//        System.err.println("Mail: Enqueued (queue size " + messageQueue.size() + ")");
//        if (!isAlive()) {
//            System.err.println("Mail: Starting thread (queue size " + messageQueue.size() + ")");
//            start();
//        }
//    }
    
//    public void kick() {
//        if (!isAlive()) {
//            System.err.println("Mail: Starting thread (queue size " + messageQueue.size() + ")");
//            start();
//        }
//    }
    
    private int lastIdProcessed=-2; // spinner 

    public void run() {
        if(lastIdProcessed == -2) {
            System.out.println("mailsender periodic looking..: " + new Date());
            lastIdProcessed = -1; // print it the first time
        }
        Thread.currentThread().setName("SurveyTool Periodic Mail Sender: queue size " + messageQueue.size());
        if(SurveyMain.isUnofficial()) {
            int countLeft = DBUtils.sqlCount(COUNTLEFTSQL); 
            if(countLeft > 0) {
                System.err.println("MailSender: waiting mails: " + countLeft);
            }
        }
        
        Connection conn = null;
        PreparedStatement s = null, s2=null;
        ResultSet rs = null;
        Throwable badException = null;
        try {
            DBUtils db = DBUtils.getInstance();
            conn = db.getDBConnection();
            conn.setAutoCommit(false);
            java.sql.Timestamp sqlnow = db.sqlNow();
            s = db.prepareForwardUpdateable(conn, "select * from " + CLDR_MAIL + " where sent_date is NULL and id > ?  and try_count < 3 order by id limit 1");
            s.setInt(1, lastIdProcessed);
            rs = s.executeQuery();
            
            if(rs.first()==false) {
                if(lastIdProcessed > 0) {
                    if(SurveyMain.isUnofficial()) {
                        System.out.println("reset lastidprocessed to -1");
                    }
                    lastIdProcessed = -1;
                }
                return; // nothing to do
            }
            
            Properties env = getProperties();
            
            Session ourSession = Session.getInstance(env, null);
            if(env.getProperty("CLDR_SMTP_DEBUG", null) != null) {
                ourSession.setDebug(true);
            }
            
            try {
                
                
                lastIdProcessed = rs.getInt("id"); // update ID
                System.out.println("Processing id " + lastIdProcessed);
                MimeMessage ourMessage = new MimeMessage(ourSession);
                
                // from - sending user or surveytool
                Integer from = rs.getInt("sender");
                UserRegistry.User fromUser = getUser(from);
                
                if(from>1) {
                    ourMessage.setFrom(new InternetAddress(fromUser.email,fromUser.name + " (SurveyTool)"));
                } else {
                    ourMessage.setFrom(new InternetAddress("surveytool@unicode.org", "CLDR SurveyTool"));
                }
                
                // to
                Integer to = rs.getInt("user");
                UserRegistry.User toUser = getUser(to);
                if(to>1) {
                    ourMessage.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(toUser.email, toUser.name));
                } else {
                    ourMessage.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress("surveytool@unicode.org", "CLDR SurveyTool"));
                }
                
    //            if (mailFromAddress != null) {
    //                Address replyTo[] = { new InternetAddress(mailFromAddress, mailFromName + " (CLDR)") };
    //                ourMessage.setReplyTo(replyTo);
    //            }
    
                ourMessage.setSubject(DBUtils.getStringUTF8(rs, "subject"));
                ourMessage.setText((SurveyMain.isUnofficial()?" == UNOFFICIAL SURVEYTOOL  - This message was sent from a TEST machine== \n":"")+DBUtils.getStringUTF8(rs, "text") + footer);

                if(env.getProperty("CLDR_SMTP", null) != null) {
                    Transport.send(ourMessage);
                } else {
                    System.err.println("Not really sending - CLDR_SMTP is not set.");
                }
                
                System.out.println("Successful send of id " + lastIdProcessed + " to " + toUser);
                
                rs.updateTimestamp("sent_date", sqlnow);
                System.out.println("Mail: Row updated: #id " + lastIdProcessed + " to " + toUser);
                rs.updateRow();
                System.out.println("Mail: do updated: #id " + lastIdProcessed + " to " + toUser);
                conn.commit();
                System.out.println("Mail: comitted: #id " + lastIdProcessed + " to " + toUser);
            } catch (MessagingException mx) {
                if(SurveyMain.isUnofficial()) {
                    SurveyLog.logException(mx, "Trying to process mail id#"+lastIdProcessed);
                }
                badException = mx;
            } catch (UnsupportedEncodingException e) {
                if(SurveyMain.isUnofficial()) {
                    SurveyLog.logException(e, "Trying to process mail id#"+lastIdProcessed);
                }
                badException = e;
            }
            
            if(badException != null) {
                int badCount = 0;
                rs.updateInt("try_count",(badCount = ( rs.getInt("try_count")+1)));
                rs.updateString("audit", badException.getMessage() + badException.getCause());
                rs.updateTimestamp("try_date", sqlnow);
                rs.updateRow();
                conn.commit();
                System.out.println("Mail: badness count of " + badCount + " updated: #id " + lastIdProcessed  + "  - " + badException.getCause());
            }

 //            
//            if(!DBUtils.getInstance().hasTable(conn, CLDR_MAIL)) {
//                    System.out.println("Creating " + CLDR_MAIL);
//                    s  = db.prepareStatementWithArgs(conn, "CREATE TABLE "+CLDR_MAIL+" (id INT NOT NULL " + DBUtils.DB_SQL_IDENTITY + ", "   // PK:  id
//                            + "user int not null, " // userid TO
//                            + "sender int not null DEFAULT -1 , " // userid TO
//                            + "subject " + DBUtils.DB_SQL_UNICODE + " not null, " // mail subj
//                            + "cc varchar(16384) DEFAULT NULL," 
//                            + "text " + DBUtils.DB_SQL_UNICODE + " not null, " // email body
//                            +  "queue_date DATETIME not null , "  // when queued?
//                            +  "try_date DATETIME DEFAULT NULL , "  // when tried to send?
//                            + "try_count INT DEFAULT 0, " // count tried
//                            + "read_date DATETIME DEFAULT NULL, "  // when read by user in-app?
//                            + "sent_date DATETIME DEFAULT NULL, "  // when successfully sent?
//                            + "audit varchar(16384) DEFAULT NULL , " // history
//                            + "post INT default NULL," // forum item
//                            + "locale varchar(127) DEFAULT NULL," // optional locale id
//                            + "xpath INT DEFAULT NULL "
//                            +   (!DBUtils.db_Mysql ? ",primary key(id)" : "") + ")");
//                            s.execute();
        } catch(SQLException se) {
            SurveyLog.logException(se, "procssing mail");
        } finally {
            DBUtils.close(rs, s, s2, conn);
        }
    }

    BlockingDeque<MimeMessage> messageQueue = new LinkedBlockingDeque<MimeMessage>();
}
