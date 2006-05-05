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

import java.util.*;
import javax.mail.*;

import javax.mail.internet.*;


public class MailSender {
    public static final String footer =             
                "\n----------\n"+
                "This email was generated automatically as part of the CLDR survey process\n"+
                "http://www.unicode.org/cldr\n"+                
                "If you have any questions about it, please do not hesitate to contact:   surveytool@unicode.org\n";
                
/*    public static void sendMail(String to, String subject, String body) {
        String from = System.getProperty("CLDR_FROM");
        String smtp = System.getProperty("CLDR_SMTP");
        sendMail(smtp,from,to,subject,body);
    }
    */
    public static synchronized void sendMail(String smtp, String from, String to, String subject, String body) {
        if(to.equals("admin@")) {
            System.err.println("Not emailing admin.");
            return;
        }
        try {
            Properties env = System.getProperties();
            env.put("mail.host", smtp);
            MimeMessage ourMessage = new MimeMessage(Session.getInstance(env, null));
            ourMessage.setFrom(new InternetAddress(from));
            ourMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            ourMessage.setSubject(subject);
            ourMessage.setText(body+footer);
            Transport.send(ourMessage);
        } catch(Throwable t) {  
            System.err.println(t.toString());
            t.printStackTrace();
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
