// TODO: fix this. currently disabled.

//
//  MailSender.java
//  cldrtools
//
//  Created by Steven R. Loomis on 3/24/2005.
//  Copyright 2005 IBM. All rights reserved.
//
//  Rewritten to use JavaMail  http://java.sun.com/products/javamail/

package org.unicode.cldr.web;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Properties;
import java.util.Set;


/**
 * Helper class. Sends mail with a simple interface */
public class MailSender {
    /**
     * Intrnal function - write something to the log
     */
    public static synchronized void log(String to, String what, Throwable t) {
        try{ 
          OutputStream file = new FileOutputStream(SurveyMain.cldrHome +  "/cldrmail.log", true); // Append
          PrintWriter pw = new PrintWriter(file);
          
          pw.println (new Date().toString() + ": " + to + " : " + what);
          if(t != null) {
            pw.println(t.toString());
            t.printStackTrace(pw);
          }
          
          pw.close();
          file.close();
        } catch(IOException ioe) {
            System.err.println("MailSender::log:  " +ioe.toString() + " whilst processing " +  to + " - " + what);
        }
    }

    /** 
     * Footer to be placed at the bottom of emails
     */
    public static final String footer =             
                "\n----------\n"+
                "This email was generated automatically as part of the CLDR survey process\n"+
                "http://www.unicode.org/cldr\n"+                
                "If you have any questions about it,\nplease contact your organization's CLDR Technical Committee member,\nor: surveytool@unicode.org\n";
                
/*    public static void sendMail(String to, String subject, String body) {
        String from = System.getProperty("CLDR_FROM");
        String smtp = System.getProperty("CLDR_SMTP");
        sendMail(smtp,from,to,subject,body);
    }
    */
    /**
     * Send a piece of mail
     * @param smtp smtp server
     * @param from From: user
     * @param to to: user
     * @param subject mail subject
     * @param body mail body
     */
    public static synchronized void sendMail(String smtp, String mailFromAddress, String mailFromName, String from, String to, String subject, String body) {
        if(to.equals("admin@")) {
            log(to,"Suppressed: " + subject,null);        
//            System.err.println("Not emailing admin.");
            return;
        }
        try {
            Properties env = System.getProperties();
            env.put("mail.host", smtp);
            MimeMessage ourMessage = new MimeMessage(Session.getInstance(env, null));
            ourMessage.setFrom(new InternetAddress(from, "CLDR Survey Tool"));
            ourMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            if(mailFromAddress != null) {
                Address replyTo[] = { new InternetAddress(mailFromAddress, mailFromName) };
                ourMessage.setReplyTo(replyTo);
            }
            ourMessage.setSubject(subject);
            Charset charset = Charset.forName("UTF-8");
            ourMessage.setText(body+footer, charset.name());
            if(smtp != null) {
                Transport.send(ourMessage);
            }
            log(to,subject,null);
        } catch(Throwable t) {  
            System.err.println("MAIL ERR: for" + to + ", " + t.toString() + " - check cldrmail.log");
            //t.printStackTrace();
            log(to,"FAIL: "+subject,t);
        }
    }
    
    public static void sendBccMail(String smtp, String mailFromAddress, String mailFromName, String from, Set<String> bcc, String subject, String body) {
        sendMail(smtp, mailFromAddress, mailFromName, from, Message.RecipientType.BCC, bcc, subject, body);
    }
    public static void sendCcMail(String smtp, String mailFromAddress, String mailFromName, String from, Set<String> bcc, String subject, String body) {
        sendMail(smtp, mailFromAddress, mailFromName, from, Message.RecipientType.CC, bcc, subject, body);
    }
    public static void sendToMail(String smtp, String mailFromAddress, String mailFromName, String from, Set<String> bcc, String subject, String body) {
        sendMail(smtp, mailFromAddress, mailFromName, from, Message.RecipientType.TO, bcc, subject, body);
    }
    /**
     * Send a piece of mail
     * @param smtp smtp server
     * @param from From: user
     * @param bcc list of addresses to BCC
     * @param subject mail subject
     * @param body mail body
     */
    public static synchronized void sendMail(String smtp, String mailFromAddress, String mailFromName, String from, Message.RecipientType recipType, Set<String> bcc, String subject, String body) {
        String typeStr = "?";
        if(recipType == Message.RecipientType.BCC) {
            typeStr = "BCC";
        } else if(recipType == Message.RecipientType.TO) {
            typeStr = "TO";
        } else if(recipType == Message.RecipientType.CC) {
            typeStr = "CC";
        }
        
        try {
            Properties env = System.getProperties();
            if(smtp != null) {
                env.put("mail.host", smtp);
            }
            MimeMessage ourMessage = new MimeMessage(Session.getInstance(env, null));
            ourMessage.setFrom(new InternetAddress(from, (mailFromName!=null)?mailFromName + " (CLDR)":"CLDR Survey Tool"));
            for(String anaddr : bcc) {
                if(anaddr.equals("admin@")) continue;
                ourMessage.addRecipients(recipType, InternetAddress.parse(anaddr));
            }
            if(mailFromAddress != null) {
                Address replyTo[] = { new InternetAddress(mailFromAddress, mailFromName + " (CLDR)") };
                ourMessage.setReplyTo(replyTo);
            }
            ourMessage.setSubject(subject);
            ourMessage.setText(body+footer);
            if(smtp != null) {
                if(bcc.size() >0) {
                    Transport.send(ourMessage);
                }
            } else {
                String out = "";
                for(String anaddr : bcc) { 
                    out = out + ", " + anaddr;
                }
                System.err.println("from: " + from + "/"+mailFromName+"/"+mailFromAddress+" subj:  " + subject + " - "+typeStr+" to " + out + ", body: " + body);
            }
            log(""+typeStr+" to " + bcc.size() +" addrs",subject,null);
        } catch(Throwable t) {  
            System.err.println("MAIL ERR: "+typeStr+" to " + bcc.size() +" addrs, " + t.toString() + " - check cldrmail.log");
            t.printStackTrace();
            log("MAIL ERR: "+typeStr+" to " + bcc.size() +" addrs, ","FAIL: "+subject,t);
        }
    }
    
    /*
    public static synchronized void sendMail(String smtp, String from, Address[] bcc, String subject, String body) {
    try {
            Properties env = System.getProperties();
            env.put("mail.host", smtp);
            MimeMessage ourMessage = new MimeMessage(Session.getInstance(env, null));
            ourMessage.setFrom(new InternetAddress(from));
            ourMessage.setRecipients(Message.RecipientType.BCC, bcc);
            ourMessage.setSubject(subject);
            ourMessage.setText(body+footer);
            Transport.send(ourMessage);
        } catch(javax.mail.MessagingException t) {  
            System.err.println("Err while mailing: " + t.toString());
            t.printStackTrace();
        }
    }
    */
}
