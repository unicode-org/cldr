//
//  MailSender.java
//  cldrtools
//
//  Created by Steven R. Loomis on 3/24/2005.
//  Copyright 2005 IBM. All rights reserved.
//

package org.unicode.cldr.web;

import java.util.*;
import com.blankenhorn.net.mail.*;

public class MailSender {
    static String from = System.getProperty("CLDR_FROM");
    static String smtp = System.getProperty("CLDR_SMTP");
    
    public static void sendMail(String to, String subject, String body) {
        try {
            EasySMTPConnection myConnection = new EasySMTPConnection(smtp);

            String[] source= {
                "From: CLDR Survey <" + from + ">", 
                "To: " + to, 
                "Subject: " + subject,
//                "Date: datumeintragen", 
                "Content-Type: text/plain; charset=utf-8", 
 //               "Message-ID: <" + EasySMTPConnection.createMessageID("cldr-survey.unicode.org") + ">", 
                "",
                body,
                "----------",
                "This email was generated automatically as part of the CLDR survey process",
                "http://www.unicode.org/cldr",
                "If you have any questions about it, please do not hesitate to contact:   srl@icu-project.org",
                ""
            };
            Message msg = new Message(source);
            myConnection.sendMessage(msg);
        } catch(Throwable t) {  
            System.err.println(t.toString());
            t.printStackTrace();
        }
    }

}
