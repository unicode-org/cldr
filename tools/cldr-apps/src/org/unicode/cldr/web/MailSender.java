//
//  MailSender.java
//  cldrtools
//
//  Created by Steven R. Loomis on 3/24/2005.
//  Copyright 2005-2012 IBM. All rights reserved.
//
//  Rewritten to use JavaMail  http://java.sun.com/products/javamail/

package org.unicode.cldr.web;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.unicode.cldr.util.CldrUtility;

/**
 * Helper class. Sends mail with a simple interface
 */
public class MailSender extends Thread {
    private static final int countLimit = 25;
    private static final int sendDelay = 5000;

    static void shutdown() {
        try {
            if (fInstance != null && fInstance.isAlive()) {
                fInstance.interrupt();
            }
        } catch (Throwable t) {
            SurveyLog.logException(t, "shutting down mailSender");
        }
    }

    String CLDR_SMTP = CldrUtility.getProperty("CLDR_SMTP", null);
    String CLDR_SMTP_PORT = CldrUtility.getProperty("CLDR_SMTP_PORT", "25");
    private boolean KEEP_ALIVE = true;
    // int port = Integer.getInteger(CLDR_SMTP_PORT);

    static private MailSender fInstance = null;

    public static synchronized MailSender getInstance() {
        if (fInstance == null) {
            fInstance = new MailSender();
        }
        return fInstance;
    }

    /**
     * Intrnal function - write something to the log
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

    /*
     * public static void sendMail(String to, String subject, String body) {
     * String from = System.getProperty("CLDR_FROM"); String smtp =
     * System.getProperty("CLDR_SMTP"); sendMail(smtp,from,to,subject,body); }
     */
    /**
     * Send a piece of mail
     * 
     * @param smtp
     *            smtp server
     * @param from
     *            From: user
     * @param to
     *            to: user
     * @param subject
     *            mail subject
     * @param body
     *            mail body
     */
    public static synchronized void sendMail(String x, String mailFromAddress, String mailFromName, String from, String to,
            String subject, String body) {
        final String smtp = getInstance().CLDR_SMTP;
        if (to.equals("admin@")) {
            log(to, "Suppressed: " + subject, null);
            // System.err.println("Not emailing admin.");
            return;
        }
        try {
            Properties env = getInstance().getProperties();
            MimeMessage ourMessage = new MimeMessage(Session.getInstance(env, null));
            ourMessage.setFrom(new InternetAddress(from, "CLDR Survey Tool"));
            ourMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to.trim()));
            if (mailFromAddress != null) {
                Address replyTo[] = { new InternetAddress(mailFromAddress, mailFromName.trim()) };
                ourMessage.setReplyTo(replyTo);
            }
            ourMessage.setSubject(subject);
            Charset charset = Charset.forName("UTF-8");
            ourMessage.setText(body + footer, charset.name());
            if (smtp != null) {
                getInstance().queue(ourMessage);
            }
            log(to, SurveyMain.isUnofficial()? (subject +"="+body):subject, null);
        } catch (Throwable t) {
            System.err.println("MAIL ERR: for" + to + ", " + t.toString() + " - check cldrmail.log");
            // t.printStackTrace();
            log(to, "FAIL: " + subject, t);
        }
    }

    public static void sendBccMail(String smtp, String mailFromAddress, String mailFromName, String from, Set<String> bcc,
            String subject, String body) {
        sendMail(smtp, mailFromAddress, mailFromName, from, Message.RecipientType.BCC, bcc, subject, body);
    }

    public static void sendCcMail(String smtp, String mailFromAddress, String mailFromName, String from, Set<String> bcc,
            String subject, String body) {
        sendMail(smtp, mailFromAddress, mailFromName, from, Message.RecipientType.CC, bcc, subject, body);
    }

    public static void sendToMail(String smtp, String mailFromAddress, String mailFromName, String from, Set<String> bcc,
            String subject, String body) {
        sendMail(smtp, mailFromAddress, mailFromName, from, Message.RecipientType.TO, bcc, subject, body);
    }

    /**
     * Send a piece of mail
     * 
     * @param smtp
     *            smtp server
     * @param from
     *            From: user
     * @param bcc
     *            list of addresses to BCC
     * @param subject
     *            mail subject
     * @param body
     *            mail body
     */
    public static synchronized void sendMail(String x, String mailFromAddress, String mailFromName, String from,
            Message.RecipientType recipType, Set<String> bcc, String subject, String body) {
        if (bcc == null || bcc.isEmpty() || bcc.size() < countLimit) {
            sendMailInternal(x, mailFromAddress, mailFromName, from, recipType, bcc, subject, body);
        } else {
            Set<String> subSet = new HashSet<String>();

            for (String s : bcc) {
                // System.err.println(" -> " + s);
                subSet.add(s);
                if (subSet.size() == countLimit) {
                    // System.err.println(" -- breaking up mail into " +
                    // subSet.size() + " recips");
                    sendMailInternal(x, mailFromAddress, mailFromName, from, recipType, subSet, subject, body);
                    subSet.clear();
                }
            }
            if (!subSet.isEmpty()) {
                // System.err.println(" -- breaking up mail into " +
                // subSet.size() + " recips, last chunk");
                sendMailInternal(x, mailFromAddress, mailFromName, from, recipType, subSet, subject, body);
            }
        }
    }

    public static synchronized void sendMailInternal(String x, String mailFromAddress, String mailFromName, String from,
            Message.RecipientType recipType, Set<String> bcc, String subject, String body) {
        String typeStr = "?";
        final String smtp = getInstance().CLDR_SMTP;
        if (recipType == Message.RecipientType.BCC) {
            typeStr = "BCC";
        } else if (recipType == Message.RecipientType.TO) {
            typeStr = "TO";
        } else if (recipType == Message.RecipientType.CC) {
            typeStr = "CC";
        }

        try {
            Properties env = getInstance().getProperties();
            if (smtp != null) {
                env.put("mail.host", smtp);
            }

            MimeMessage ourMessage = new MimeMessage(Session.getInstance(env, null));
            ourMessage.setFrom(new InternetAddress(from, (mailFromName != null) ? mailFromName + " (CLDR)" : "CLDR Survey Tool"));
            for (String anaddr : bcc) {
                if (anaddr.equals("admin@"))
                    continue;
                ourMessage.addRecipients(recipType, InternetAddress.parse(anaddr.trim()));
            }
            if (mailFromAddress != null) {
                Address replyTo[] = { new InternetAddress(mailFromAddress, mailFromName + " (CLDR)") };
                ourMessage.setReplyTo(replyTo);
            }
            ourMessage.setSubject(subject);
            ourMessage.setText(body + footer);
            if (smtp != null) {
                if (bcc.size() > 0) {
                    getInstance().queue(ourMessage);
                }
            } else {
                String out = "";
                for (String anaddr : bcc) {
                    out = out + ", " + anaddr;
                }
                System.err.println("from: " + from + "/" + mailFromName + "/" + mailFromAddress + " subj:  " + subject + " - "
                        + typeStr + " to " + out + ", body: " + body);
            }
            log("" + typeStr + " to " + bcc.size() + " addrs", subject, null);
        } catch (Throwable t) {
            System.err
                    .println("MAIL ERR: " + typeStr + " to " + bcc.size() + " addrs, " + t.toString() + " - check cldrmail.log");
            t.printStackTrace();
            log("MAIL ERR: " + typeStr + " to " + bcc.size() + " addrs, ", "FAIL: " + subject, t);
        }
    }

    private Properties getProperties() {
        Properties env = System.getProperties();
        if(CLDR_SMTP!=null) {
            env.put("mail.host", CLDR_SMTP);
        } else {
            System.out.println("CLDR_SMTP is null, not putting into arrays");
        }
        env.put("mail.smtp.port", CLDR_SMTP_PORT);
        // TODO: user, etc
        // env.put("mail.smtp.port", CLDR_SMTP_PORT);
        return env;
    }

    private synchronized void queue(MimeMessage ourMessage) throws MessagingException {
        messageQueue.addFirst(ourMessage);
        System.err.println("Mail: Enqueued (queue size " + messageQueue.size() + ")");
        if (!isAlive()) {
            System.err.println("Mail: Starting thread (queue size " + messageQueue.size() + ")");
            start();
        }
    }

    public void run() {
        setName("SurveyTool Mail Sender: queue size " + messageQueue.size());
        while (KEEP_ALIVE || !messageQueue.isEmpty()) {
            if (isInterrupted())
                return;
            try {
                setName("SurveyTool Mail Sender: taking from queue " + messageQueue.size());
                MimeMessage next = messageQueue.takeLast();
                setName("SurveyTool Mail Sender: processing, queue " + messageQueue.size());
                // System.err.println("Mail: run() (queue size " +
                // messageQueue.size()+")");

                Transport.send(next);
                // System.err.println("Mail: sent " + next.toString()+")");

                // System.err.println("Mail: sleeping...");
                setName("SurveyTool Mail Sender: sleeping, queue " + messageQueue.size());
                Thread.sleep(sendDelay);

            } catch (MessagingException ex) {
                SurveyLog.logException(ex, "sending mail", null);
                Logger.getLogger(MailSender.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                SurveyLog.logException(ex, "getting mail", null);
                Logger.getLogger(MailSender.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
        }
    }

    BlockingDeque<MimeMessage> messageQueue = new LinkedBlockingDeque<MimeMessage>();
}
