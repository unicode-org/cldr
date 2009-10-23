//
//  SurveyForum.java
//  fivejay
//
//  Created by Steven R. Loomis on 27/10/2006.
//  Copyright 2006-2008 IBM. All rights reserved.
//

package org.unicode.cldr.web;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;

import java.sql.Connection;
import java.sql.Timestamp;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.PreparedStatement;

import com.ibm.icu.util.ULocale;
import com.ibm.icu.dev.test.util.ElapsedTimer;

import org.unicode.cldr.util.*;
import org.unicode.cldr.web.DataSection.DataRow;
import org.unicode.cldr.web.SurveyMain.UserLocaleStuff;
import org.unicode.cldr.web.Vetting.DataSubmissionResultHandler;
import org.unicode.cldr.test.CheckCLDR;

import com.sun.syndication.feed.synd.*;
import com.sun.syndication.io.SyndFeedOutput;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;

// servlet imports
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * This class implements a discussion forum per language (ISO code)
 */
public class SurveyForum {
    private static java.util.logging.Logger logger;


    public static String DB_FORA = "sf_fora";    //  forum name -> id
    public static String DB_POSTS = "sf_posts";  // 
    public static String DB_READERS = "sf_readers";  // 
    
    public static String DB_LOC2FORUM = "sf_loc2forum";  // locale -> forum.. for selects.

    /* --------- FORUM ------------- */
    static final String F_FORUM = "forum";
    static final String F_XPATH = "xpath";
    static final String F_PATH = "path";
    static final String F_DO = "d";
    static final String F_LIST = "list";
    static final String F_VIEW = "view";
    static final String F_ADD = "add";
    static final String F_REPLY = "reply";
    static final String F_POST = "post";
    
    static final String POST_SPIEL = "Post a comment to other vetters. (Don't use this to report SurveyTool issues or propose data changes: use the bug forms.)";    
    /** 
     * prepare text for posting
     */
    static public String preparePostText(String intext) {
        return 
            intext.replaceAll("\r","")
                  .replaceAll("\n","<p>");
    }

    public static String HTMLSafe(String s) {
        if(s==null) return null;
        
        return 
            s.replaceAll("&","&amp;")
             .replaceAll("<","&lt;")
             .replaceAll(">","&gt;")
             .replaceAll("\"","&quot;");
    }
    
    Hashtable numToName = new Hashtable();
    Hashtable nameToNum = new Hashtable();
    
    static final int GOOD_FORUM = 0; // 0 or greater
    static final int BAD_FORUM = -1;
    static final int NO_FORUM = -2;
    
    synchronized int getForumNumber(String forum) {
        if(forum.length()==0) {
            return NO_FORUM; // all forums
        }
        // make sure it is a valid src!
        if((forum==null)||(forum.indexOf('_')>=0)||!sm.isValidLocale(CLDRLocale.getInstance(forum))) {
//            // <explain>
//            StringBuffer why = new StringBuffer();
//            if(forum==null) why.append("(forum==null) ");
//            if(forum.indexOf('_')>=0) why.append("forum.indexOf(_)>=0) ");
//            if(!sm.isValidLocale(CLDRLocale.getInstance(forum))) why.append("!valid ");
//            // </explain>
            throw new RuntimeException("Invalid forum: " + forum );
        }
        
        // now with that out of the way..
        Integer i = (Integer)nameToNum.get(forum);
        if(i==null) {
            return createForum(forum);
        } else {
            return i.intValue();
        }
    }
    
    private int getForumNumberFromDB(String forum) {
        try {
            fGetByLoc.setString(1, forum);
            ResultSet rs = fGetByLoc.executeQuery();
            if(!rs.next()) {
                rs.close();
                return BAD_FORUM;
            } else {
                int j = rs.getInt(1);
                rs.close();
                return j;
            }
        } catch (SQLException se) {
            String complaint = "SurveyForum:  Couldn't add forum " +forum + " - " + SurveyMain.unchainSqlException(se) + " - fGetByLoc";
            logger.severe(complaint);
            throw new RuntimeException(complaint);
        }
    }
    
    private int createForum(String forum) {
        int num =  getForumNumberFromDB(forum);
        if(num == BAD_FORUM) {
            try {
                fAdd.setString(1, forum);
                fAdd.executeUpdate();
                conn.commit();
            } catch (SQLException se) {
                String complaint = "SurveyForum:  Couldn't add forum " +forum + " - " + SurveyMain.unchainSqlException(se) + " - fAdd";
                logger.severe(complaint);
                throw new RuntimeException(complaint);
            }
            num = getForumNumberFromDB(forum);
        }
        
        if(num==BAD_FORUM) {
            throw new RuntimeException("Couldn't query ID for forum " + forum);
        }
        // Add to list
        Integer i = new Integer(num);
        numToName.put(forum,i);
        nameToNum.put(i,forum);
        return num;
    }

    public int gatherInterestedUsers(String forum, Set<String> cc_emails, Set<String> bcc_emails) {
        int emailCount = 0;
        synchronized(conn) {
            try {
                pIntUsers.setString(1, forum);
                
                ResultSet rs = pIntUsers.executeQuery();
                
                while(rs.next()) {
                    int uid = rs.getInt(1);
                    
                    UserRegistry.User u = sm.reg.getInfo(uid);
                    if(u != null && u.email != null && u.email.length()>0) {
                        if(UserRegistry.userIsVetter(u)) {
                            cc_emails.add(u.email);
                        } else {
                            bcc_emails.add(u.email);
                        }
                        emailCount++;
                    }
                }
            } catch ( SQLException se ) {
                String complaint = "SurveyForum:  Couldn't gather interested users for " +forum + " - " + SurveyMain.unchainSqlException(se) + " - pIntUsers";
                logger.severe(complaint);
                throw new RuntimeException(complaint);
            }
        }
        return emailCount;
    }
    
    void doForum(WebContext ctx, String sessionMessage) throws IOException { 
        /* OK, let's see what we are doing here. */
        String forum = ctx.field(F_FORUM);
        int base_xpath = ctx.fieldInt(F_XPATH);
        if(ctx.hasField(F_XPATH)&&!ctx.hasField(F_FORUM)) {
            forum = localeToForum(ctx.field("_"));
        }
        int forumNumber = getForumNumber(forum);
        String pD = ctx.field(F_DO); // do
        boolean loggedout = ((ctx.session==null)||(ctx.session.user==null));
        boolean canModify = false;

        if((ctx.getLocale() == null) && (forumNumber >= GOOD_FORUM)) {
            ctx.setLocale(CLDRLocale.getInstance(forum));
        }
        
        if(!loggedout && (forumNumber >= GOOD_FORUM)) {
            canModify = (UserRegistry.userCanModifyLocale(ctx.session.user,ctx.getLocale()));
        }
        
        /* can we accept a string xpath? (ignore if 'forum' is set) */
        if(!ctx.hasField(F_FORUM)&&base_xpath==-1 && ctx.hasField(F_XPATH)&&ctx.field(F_XPATH).length()>0) {
        	String xstr = ctx.field(F_XPATH);
        	String msg = null;

        	if(xstr.startsWith("//ldml")) {
        		base_xpath = sm.xpt.peekByXpath(xstr);
        		if(base_xpath==-1) {
            		msg = "XPath lookup failed.";
        		}
        	}

        	if(base_xpath==-1 && !xstr.startsWith("//ldml")) {
        		// try prettypath
        		String ostr = sm.xpt.getOriginal(xstr);
        		if(ostr != null) {
        			base_xpath = sm.xpt.peekByXpath(ostr);
        			if(base_xpath==-1) {
        				msg = "PrettyPath lookup resulted in unfound xpath";
        			}
        		} else {
        			msg = "PrettyPath lookup failed.";
        		}
        	}
        	
        	if(base_xpath==-1) {
        		String str =  ctx.jspUrl("xpath.jsp") 
                        + "&_="+URLEncoder.encode(ctx.getLocale().toString())
                        + "&xpath="+URLEncoder.encode(xstr)
                        + "&msg="+URLEncoder.encode(msg)
                        ;
        		ctx.redirect(str);
        		return;
        	}
//        	System.err.println("XP ["+xstr+"] -> " + base_xpath);
        	// TODO: may need fixup here.
        }
        
        // Are they just zooming in?
        if(!canModify && (base_xpath!=-1)) {
            doZoom(ctx, base_xpath, sessionMessage);
            return;
        }
        
        // User isnt logged in.
        if(loggedout) {
            sm.printHeader(ctx,"Fora | Please login.");
            sm.printUserTable(ctx);
            if(sessionMessage != null) {
                ctx.println(sessionMessage+"<hr>");
            }
            ctx.println("<hr><strong>You aren't logged in. Please login to continue.</strong><p>");
            sm.printFooter(ctx);
            return;
        }
        
        // User has an account, but does not have access to this forum.
        if(!canModify && !(forumNumber==NO_FORUM && UserRegistry.userIsVetter(ctx.session.user))) {
            sm.printHeader(ctx,"Fora | Access Denied.");
            sm.printUserTable(ctx);
            if(sessionMessage != null) {
                ctx.println(sessionMessage+"<hr>");
            }
            ctx.println("<hr><strong>You do not have access to this forum. If you believe this to be in error, contact your CLDR TC member, and/or the person who set up your account.</strong><p>");
            sm.printFooter(ctx);
            return;
        }
        
        // User is logged in and has access.
        
        if((base_xpath != -1) || (ctx.hasField("replyto"))) {
            // Post to a specific xpath
            doXpathPost(ctx, forum, forumNumber, base_xpath);
        } else if(F_VIEW.equals(pD) && ctx.hasField("id")) {
            doForumView(ctx, forum, forumNumber);
        } else if(forumNumber == BAD_FORUM) {
            sm.printHeader(ctx,"Fora");
            sm.printUserTable(ctx);
            // no forum or bad forum. Do general stuff.
            // doForumForum(ctx, pF, pD);
        } else {
            // list what is in a certain forum
            doForumForum(ctx, forum, forumNumber);
        }
        
        if(sessionMessage != null) {
            ctx.println("<hr>"+sessionMessage);
        }
        sm.printFooter(ctx);
    }
    
    String returnUrl(WebContext ctx, CLDRLocale locale, int base_xpath) {
        String xpath = sm.xpt.getById(base_xpath);
        if(xpath == null) return ctx.base()+"?"+"_="+locale;
        String theMenu = PathUtilities.xpathToMenu(xpath);
        if(theMenu==null) {
            theMenu="raw";
        }
        return ctx.base()+"?"+
					"_="+locale+"&amp;x="+theMenu+"&amp;xfind="+base_xpath+"#x"+base_xpath;
    }
    
    void doZoom(WebContext ctx, int base_xpath, String sessionMessage) {
        String xpath = sm.xpt.getById(base_xpath);
        if((xpath == null)&&(base_xpath != -1)) {
            sm.printHeader(ctx, "Missing Item");
            sm.printUserTable(ctx);
            ctx.println("<div class='ferrbox'>Sorry, the item you were attempting to view does not exist.  Note that zoomed-in URLs are not permanent.</div>");
            sm.printFooter(ctx);
            return;
        }
        String prettyPath = sm.xpt.getPrettyPath(base_xpath);
        if(prettyPath != null) {
            sm.printHeader(ctx, prettyPath.replaceAll("\\|", " | ") ); // TODO: pretty path?
        } else {
            sm.printHeader(ctx, "Forum");
        }
        sm.printUserTable(ctx);
        if(prettyPath != null) {
            ctx.println("<h2>"+prettyPath+"</h2>");
        }
        if(sessionMessage != null) {
            ctx.println(sessionMessage+"<hr>");
        }
        boolean nopopups = ctx.prefBool(SurveyMain.PREF_NOPOPUPS);
        String returnText = returnText(ctx, base_xpath);
        if(nopopups) {
            ctx.println(returnText+"<hr>");
        }
        showXpath(ctx, xpath, base_xpath, ctx.getLocale());
        if(nopopups) {
            ctx.println("<hr>"+returnText+"<br/>");
        }
        sm.printFooter(ctx);
    }
    
    void doXpathPost(WebContext ctx, String forum, int forumNumber, int base_xpath) throws IOException {
        String xpath = sm.xpt.getById(base_xpath);
        int replyTo = ctx.fieldInt("replyto",-1);
        
        // Don't want to call printHeader here - becasue if we do a post, we're going ot be
        // redirecting outta here. 
        String subj = HTMLSafe(ctx.field("subj"));
        String text = HTMLSafe(ctx.field("text"));
        boolean defaultSubj = false;
        if((subj == null) || (subj.trim().length()==0)) {
            defaultSubj = true;
            if(xpath != null) {
                //I could really use '#if 0' right here
                subj = sm.xpt.getPrettyPath(xpath);
            /*
                int n = xpath.lastIndexOf("/");
                if(n!=-1) {
                    subj = xpath.substring(n+1,xpath.length());
                }
            */
                
            }
            if(subj == null){
                subj = "item";
            }
            
            subj = ctx.getLocale() + ": " +subj;
        } else {
            subj = subj.trim();
        }

        if(text!=null && text.length()>0) {
            if(ctx.field("post").length()>0) {
                // do the post!
                
                if(forumNumber == BAD_FORUM) {
                    throw new RuntimeException("Bad forum: " + forum);
                }
                
                synchronized(conn) {                
                    try {
                        pAdd.setInt(1,ctx.session.user.id);
                        pAdd.setString(2, subj);
                        pAdd.setString(3, preparePostText(text));
                        pAdd.setInt(4, forumNumber);
                        pAdd.setInt(5, -1); // no parent
                        pAdd.setString(6,ctx.getLocale().toString());
                        pAdd.setInt(7, base_xpath);
                        
                        int n = pAdd.executeUpdate();
                        conn.commit();
                        
                        if(n!=1) {
                            throw new RuntimeException("Couldn't post to " + forum + " - update failed.");
                        }
                    } catch ( SQLException se ) {
                        String complaint = "SurveyForum:  Couldn't add post to " +forum + " - " + SurveyMain.unchainSqlException(se) + " - pAdd";
                        logger.severe(complaint);
                        throw new RuntimeException(complaint);
                    }
                }
                
                // Apparently, it posted.
				
                ElapsedTimer et = new ElapsedTimer("Sending email to "+forum);
                int emailCount=0;
				// Do email- 
                Set<String> cc_emails = new HashSet<String>();
                Set<String> bcc_emails = new HashSet<String>();
                
                emailCount = gatherInterestedUsers(forum, cc_emails, bcc_emails);
                
                String from = sm.survprops.getProperty("CLDR_FROM","nobody@example.com");
                String smtp = sm.survprops.getProperty("CLDR_SMTP",null);
                
                String subject = "New CLDR forum post for: " + forum;
                
                String body = "This is a post to the CLDR "+forum+" forum, in the subject:\n  "+subj+"\n\n"+
                                "For details and to respond, login to survey tool and then click on this link:\n\t " + "http://" + ctx.serverHostport() + forumUrl(ctx,forum) + "\n" +
                                    "(Note, if the text does not display properly, please click the link to view the entire message)\n\n-----------------\n\n"+text;
                
                if(!bcc_emails.isEmpty()) {
                    MailSender.sendBccMail(smtp, null, null, from, bcc_emails, subject, body);
                }
                if(!cc_emails.isEmpty()) {
                    String theFrom = from;
                    if(UserRegistry.userIsVetter(ctx.session.user)) {
                        theFrom = ctx.session.user.email;
                        if(theFrom.equals("admin@")) {
                            theFrom = from;
                        }
                        // cc mails, of Vetters, get to see the From address, if they themselves are a vetter.
                        MailSender.sendCcMail(smtp, theFrom, ctx.session.user.name, from, cc_emails, subject, body);
                    } else {
                        MailSender.sendCcMail(smtp, null, null, from, cc_emails, subject, body);
                    }
                }

                
                System.err.println(et.toString()+" - # of users:" + emailCount);
                
                
                ctx.redirect(ctx.base()+"?_="+ctx.getLocale().toString()+"&"+F_FORUM+"="+forum+"&didpost=t");
                return;
                
            } else {
                sm.printHeader(ctx,"Fora | " + forum + " | Preview post on #" + base_xpath);
                printForumMenu(ctx, forum);
            }

            ctx.println("<div class='odashbox'><h3>Preview</h3>");
            ctx.println("<b>Subject</b>: "+subj+"<br>");
            ctx.println("<b>Xpath</b>: <tt>"+xpath+"</tt><br>");
            ctx.println("<br><br> <div class='response'>"+(text==null?("<i>none</i>"):preparePostText(text))+"</div><p>");
            ctx.println("</div><hr>");
        } else {
            String prettyPath = sm.xpt.getPrettyPath(base_xpath);
            if(prettyPath != null) {
                sm.printHeader(ctx, prettyPath.replaceAll("\\|", " | "));
            } else {
                sm.printHeader(ctx, forum + " | Forum");
            }
            printForumMenu(ctx, forum);
            ctx.println("<a href='"+forumUrl(ctx,forum)+"'>" + forum + " forum</a>");
            if(prettyPath != null) {
                ctx.println("<h2>"+prettyPath+"</h2>");
            }
        }
        
        if(replyTo != -1) {
            ctx.println("<h4>In Reply To:</h4><span class='reply-to'>");
            String subj2 = showItem(ctx, forum, forumNumber, replyTo, false); 
            ctx.println("</span>");
            if(defaultSubj) {
                subj = subj2;
                if(!subj.startsWith("Re: ")) {
                    subj = "Re: "+subj;
                }
            }
        }

        if((ctx.field("text").length()==0) &&
           (ctx.field("subj").length()==0) &&
            base_xpath != -1) {
            // hide the 'post comment' thing
            String warnHash = "post_comment"+base_xpath+"_"+forum;
            ctx.println("<div id='h_"+warnHash+"'><a href='javascript:show(\"" + warnHash + "\")'>" + 
                        "<b>+</b> "+POST_SPIEL+"</a></div>");
            ctx.println("<!-- <noscript>Warning: </noscript> -->" + 
                        "<div style='display: none' class='pager' id='" + warnHash + "'>" );
            ctx.println("<a href='javascript:hide(\"" + warnHash + "\")'>" + 
                        "(<b>-</b> Don't post comment)</a>");
        }
                
        ctx.print("<a name='replyto'></a>");
        ctx.println("<form method='POST' action='"+ctx.base()+"'>");
        ctx.println("<input type='hidden' name='"+F_FORUM+"' value='"+forum+"'>");
        ctx.println("<input type='hidden' name='"+F_XPATH+"' value='"+base_xpath+"'>");
        ctx.println("<input type='hidden' name='_' value='"+ctx.getLocale()+"'>");
        ctx.println("<input type='hidden' name='replyto' value='"+replyTo+"'>");

        if(sm.isPhaseBeta()) {
            ctx.println("<div class='ferrbox'>Please remember that the SurveyTool is in Beta, therefore your post will be deleted when the beta period closes.</div>");
        }

        ctx.println("<b>Subject</b>: <input name='subj' size=40 value='"+subj+"'><br>");
        ctx.println("<textarea rows=12 cols=60 name='text'>"+(text==null?"":text)+"</textarea>");
        ctx.println("<br>");
        ctx.println("<input name=post "+
            //(ctx.hasField("text")?"":"disabled")+ // require preview
            " type=submit value=Post>");
        ctx.println("<input type=submit name=preview value=Preview><br>");
        if(sm.isPhaseBeta()) {
            ctx.println("<div class='ferrbox'>Please remember that the SurveyTool is in Beta, therefore your post will be deleted when the beta period closes.</div>");
        }
        ctx.println("</form>");
        
        if(ctx.field("post").length()>0) {
            if(text==null || text.length()==0) {
                ctx.println("<b>Please type some text.</b><br>");
            } else {
              //  ...
            }
        }

        if((ctx.field("text").length()==0) &&
           (ctx.field("subj").length()==0) &&
            base_xpath != -1) {
            ctx.println("</div>");
        }
        
        if(base_xpath != -1) {
            boolean nopopups = ctx.prefBool(SurveyMain.PREF_NOPOPUPS);
            String returnText = returnText(ctx, base_xpath);
            if(nopopups) {
                ctx.println(returnText+"<hr>");
            }
            showXpath(ctx, xpath, base_xpath, ctx.getLocale());
            if(nopopups) {
                ctx.println("<hr>"+returnText+"<br/>");
            }
        }
     }
    
    public static String showXpath(WebContext baseCtx, String section_xpath, int item_xpath) {
        String base_xpath = section_xpath;
        CLDRLocale loc = baseCtx.getLocale();
        WebContext ctx = new WebContext(baseCtx);
        ctx.setLocale(loc);
        boolean canModify = (UserRegistry.userCanModifyLocale(ctx.session.user,ctx.getLocale()));
        String podBase = DataSection.xpathToSectionBase(section_xpath);
        baseCtx.sm.printPathListOpen(ctx);
        if(canModify) {
            /* hidden fields for that */
//            ctx.println("<input type='hidden' name='"+F_FORUM+"' value='"+ctx.locale.getLanguage()+"'>");
//            ctx.println("<input type='hidden' name='"+F_XPATH+"' value='"+base_xpath+"'>");
//            ctx.println("<input type='hidden' name='_' value='"+loc+"'>");
//
//            ctx.println("<input type='submit' value='" + ctx.sm.getSaveButtonText() + "'><br>"); //style='float:right' 
//            ctx.sm.vet.processPodChanges(ctx, podBase);
        } else {
//            ctx.println("<br>cant modify " + ctx.locale + "<br>");
        }
        
        DataSection section = ctx.getSection(podBase);
        
        baseCtx.sm.printSectionTableOpen(ctx, section, true);
        baseCtx.sm.showPeas(ctx, section, canModify, item_xpath, true);
        baseCtx.sm.printSectionTableClose(ctx, section);
        baseCtx.sm.printPathListClose(ctx);
        
//        ctx.printHelpHtml(section, item_xpath);
        return podBase;
    }

    /**
     * 
     * @param ctx
     * @param baseXpath
     * @return true if no errors were detected, otherwise false.
     */
    public static SummarizingSubmissionResultHandler processDataSubmission(WebContext ctx, String baseXpath) {
    	return processDataSubmission(ctx,baseXpath,null);
    }
    /**
     * 
     * @param ctx
     * @param baseXpath
     * @param ssrh ResultHandler, if null one will be created.
     * @return true if no errors were detected, otherwise false.
     */
    public static SummarizingSubmissionResultHandler processDataSubmission(WebContext ctx, String baseXpath, SummarizingSubmissionResultHandler ssrh) {
    	if(ssrh == null) {
    		ssrh = new SummarizingSubmissionResultHandler();
    	}
        ctx.sm.vet.processPodChanges(ctx, DataSection.xpathToSectionBase(baseXpath), ssrh);
        return ssrh;
    }
    
    /**
     * Output the beginning of a data submission form.
     * @param baseCtx
     * @param section_xpath
     */
    public static void beginSurveyToolForm(WebContext ctx, String section_xpath) {
        CLDRLocale loc = ctx.getLocale();
        boolean canModify = (UserRegistry.userCanModifyLocale(ctx.session.user,ctx.getLocale()));
        String podBase = DataSection.xpathToSectionBase(section_xpath);
        ctx.put(WebContext.CAN_MODIFY, canModify);
        //ctx.sm.printPathListOpen(ctx);
        if(canModify) {
            /* hidden fields for that */
            ctx.println("<input type='hidden' name='_' value='"+ctx.getLocale().toString()+"'>");
            ctx.println("<input type='submit' value='" + SurveyMain.getSaveButtonText() + "'><br>"); //style='float:right' 
        }
    }
    
    public static void endSurveyToolForm(WebContext ctx) {
        ctx.sm.printPathListClose(ctx);    	
    }
    
    public static void printSectionTableOpenShort(WebContext ctx, String base_xpath) {
    	DataSection section = null;
    	if(base_xpath != null) {
	        String podBase = DataSection.xpathToSectionBase(base_xpath);
	        section = ctx.getSection(podBase);   
    	}
    	SurveyMain.printSectionTableOpenShort(ctx, section);
    }
    
    public static void printSectionTableCloseShort(WebContext ctx,String base_xpath) {
    	DataSection section = null;
    	if(base_xpath != null) {
	        String podBase = DataSection.xpathToSectionBase(base_xpath);
	        section = ctx.getSection(podBase);   
    	}
        ctx.sm.printSectionTableClose(ctx, section);
    }
    
    /**
     * @param baseCtx
     * @param section_xpath
     * @param item_xpath
     * @return pod base used 
     */
    public static String showXpathShort(WebContext baseCtx, String section_xpath, int item_xpath) {
        String base_xpath = section_xpath;
        CLDRLocale loc = baseCtx.getLocale();
        WebContext ctx = new WebContext(baseCtx);
        ctx.setLocale(loc);
        boolean canModify = (UserRegistry.userCanModifyLocale(ctx.session.user,ctx.getLocale()));

        ctx.put(WebContext.CAN_MODIFY, canModify);
        ctx.put(WebContext.ZOOMED_IN, true);
        String podBase = DataSection.xpathToSectionBase(base_xpath);        
        DataSection section = ctx.getSection(podBase);
        baseCtx.sm.showPeasShort(ctx, section, item_xpath);
        return podBase;
    }

    public static String showXpathShort(WebContext baseCtx, String section_xpath, String item_xpath) {
        return showXpathShort(baseCtx, section_xpath, baseCtx.sm.xpt.getByXpath(item_xpath));
    }

    public static String showXpathShort(WebContext baseCtx, String item_xpath) {
        String section_xpath = DataSection.xpathToSectionBase(item_xpath);
        return showXpathShort(baseCtx, section_xpath, baseCtx.sm.xpt.getByXpath(item_xpath));
    }

    public static String showXpath(WebContext baseCtx, String section_xpath, String item_xpath) {
    	return showXpath(baseCtx, section_xpath, baseCtx.sm.xpt.getByXpath(item_xpath));
    }
    
    /**
     * Get CLDR File
     */
    public static CLDRFile getCLDRFile(WebContext ctx) {
        synchronized (ctx.session) { // session sync
            UserLocaleStuff uf = ctx.sm.getUserFile(ctx, (ctx.session.user==null)?null:ctx.session.user, ctx.getLocale());
            CLDRFile cf = uf.cldrfile;
            return cf;
        }
    }
    
    public static void showSubmitButton(WebContext baseCtx) {
        CLDRLocale loc = baseCtx.getLocale();
        WebContext ctx = new WebContext(baseCtx);
        ctx.setLocale(loc);
        boolean canModify = (UserRegistry.userCanModifyLocale(ctx.session.user,ctx.getLocale()));
        if(canModify) {
            /* hidden fields for that */
//            ctx.println("<input type='hidden' name='"+F_FORUM+"' value='"+ctx.locale.getLanguage()+"'>");
//            ctx.println("<input type='hidden' name='"+F_XPATH+"' value='"+base_xpath+"'>");
//            ctx.println("<input type='hidden' name='_' value='"+loc+"'>");

            ctx.println("<input type='submit' value='" + ctx.sm.getSaveButtonText() + "'>"); //style='float:right' 
            
        }
    }
    

    public void showXpath(WebContext baseCtx, String xpath, int base_xpath, CLDRLocale locale) {
        WebContext ctx = new WebContext(baseCtx);
        ctx.setLocale(locale);
        // Show the Pod in question:
//        ctx.println("<hr> \n This post Concerns:<p>");
        boolean canModify = (UserRegistry.userCanModifyLocale(ctx.session.user,ctx.getLocale()));
        String podBase = DataSection.xpathToSectionBase(xpath);
        
        sm.printPathListOpen(ctx);

        if(canModify) {
            /* hidden fields for that */
            ctx.println("<input type='hidden' name='"+F_FORUM+"' value='"+ctx.getLocale().getLanguage()+"'>");
            ctx.println("<input type='hidden' name='"+F_XPATH+"' value='"+base_xpath+"'>");
            ctx.println("<input type='hidden' name='_' value='"+locale+"'>");

            ctx.println("<input type='submit' value='" + sm.getSaveButtonText() + "'><br>"); //style='float:right' 
            sm.vet.processPodChanges(ctx, podBase, new DefaultDataSubmissionResultHandler(ctx));
        } else {
//            ctx.println("<br>cant modify " + ctx.locale + "<br>");
        }
        
        DataSection section = ctx.getSection(podBase);
        
        sm.printSectionTableOpen(ctx, section, true);
        sm.showPeas(ctx, section, canModify, base_xpath, true);
        sm.printSectionTableClose(ctx, section);
        sm.printPathListClose(ctx);
        
        ctx.printHelpHtml(xpath);
    }
    
    void printForumMenu(WebContext ctx, String forum) {
        ctx.println("<table width='100%' border='0'><tr><td>");
        ctx.println("<a href=\"" + ctx.url() + "\">" + "<b>Locales</b>" + "</a><br>");
        sm.printListFromInterestGroup(ctx, forum);
        ctx.println("</td><td align='right'>");
        sm.printUserTable(ctx);
        ctx.println("</td></tr></table>");
    }


    /*
     * Show the latest posts in a forum.
     * called by doForum()
     */
    static final int MSGS_PER_PAGE = 9925;
     
    private void doForumForum(WebContext ctx, String forum, int forumNumber) {
        boolean didpost = ctx.hasField("didpost");
        int skip = ctx.fieldInt("skip",0);
        int count = 0;
        
        // print header
        sm.printHeader(ctx, "Fora | " + forum);
        printForumMenu(ctx, forum);
                
        ctx.print(forumFeedIcon(ctx, forum));
        
        ctx.println("<hr>");
        if(didpost) {
            ctx.println("<b>Posted your response.</b><hr>");
        }

        ctx.println("<a href='"+forumUrl(ctx,forum)+"&amp;replyto='><b>+</b> "+POST_SPIEL+"</a><br>");
        
        int hidCount = 0;
        boolean showOld = ctx.prefBool("SHOW_OLD_MSGS");
        
        synchronized (conn) {
            try {
                pList.setInt(1, forumNumber);
                ResultSet rs = pList.executeQuery();

                while(count<MSGS_PER_PAGE && rs.next()) {
                    if(count==0) {
                        // HEADER 
                    }
                    
                    int poster = rs.getInt(1);
                    String subj = SurveyMain.getStringUTF8(rs, 2);
                    String text = SurveyMain.getStringUTF8(rs, 3);
                    int id = rs.getInt(4);
                    java.sql.Timestamp lastDate = rs.getTimestamp(5);
                    String loc = rs.getString(6);
                    int xpath = rs.getInt(7);
                 
                    if(lastDate.before(oldOnOrBefore) && !showOld) {
                    	hidCount++;
                    } else {
                    	showPost(ctx, forum, poster, subj, text, id, lastDate, CLDRLocale.getInstance(loc), xpath);
                    }
                 
                    count++;
                }
            } catch (SQLException se) {
                String complaint = "SurveyForum:  Couldn't add forum " +forum + " - " + SurveyMain.unchainSqlException(se) + " - fGetByLoc";
                logger.severe(complaint);
                throw new RuntimeException(complaint);
            }
        }
        
        ctx.println("<a href='"+forumUrl(ctx,forum)+"&amp;replyto='><b>+</b> "+POST_SPIEL+"..</a><br>");
        ctx.println("<hr>"+count+" posts <br>");
        String nold = "";
        if(hidCount>0) {
        	nold = hidCount+" ";
        }
        WebContext subCtx = new WebContext(ctx);
        subCtx.setQuery("skip", skip);
        // didpost not important
        subCtx.setQuery("_", ctx.field("_"));
        subCtx.setQuery("forum",ctx.field("forum"));
        
        sm.showTogglePref(subCtx, "SHOW_OLD_MSGS", "Show "+nold+"old messages?");
    }

    private void doForumView(WebContext ctx, String forum, int forumNumber) {
    
        int id = ctx.fieldInt("id",-1);
        if(id == -1) {
            doForumForum(ctx,forum,forumNumber);
            return;
        }
        
        showItem(ctx, forum, forumNumber, id, true);
    }
    
    String showItem(WebContext ctx, String forum, int forumNumber, int id, boolean doTitle) {
        synchronized (conn) {
            try {
                pGet.setInt(1, id);
                ResultSet rs = pGet.executeQuery();

                if(!rs.next()){
                    throw new RuntimeException("could not quer forum posting id "+id);
                }
              
                int poster = rs.getInt(1);
                String subj = SurveyMain.getStringUTF8(rs, 2);
                String text = SurveyMain.getStringUTF8(rs, 3);
                //int id = rs.getInt(4);
                java.sql.Timestamp lastDate = rs.getTimestamp(5);
                String loc = rs.getString(6);
                int xpath = rs.getInt(7);
                
                
                // now, show the normal heading
    
                if(doTitle) {
                    // print header
                    sm.printHeader(ctx, "Fora | " + forum + " | Post: " + subj);
                    printForumMenu(ctx, forum);
                            
                    ctx.println("<hr>");
    //                if(didpost) {
    //                    ctx.println("<b>Posted your response.</b><hr>");
    //                }
                 }
                
                 showPost(ctx, forum, poster, subj, text, id, lastDate, CLDRLocale.getInstance(loc), xpath);
                             
                if(xpath != -1) {
                    String xpath_string = sm.xpt.getById(xpath);
                    if(xpath_string != null) {
                        showXpath(ctx, xpath_string, xpath, CLDRLocale.getInstance(loc));
                    }
                }              
                
                return subj;
                 
            } catch (SQLException se) {
                String complaint = "SurveyForum:  Couldn't add forum " +forum + " - " + SurveyMain.unchainSqlException(se) + " - fGetByLoc";
                logger.severe(complaint);
                throw new RuntimeException(complaint);
            }
        }
    }

    /*
     * Show one post, "long" form.
     */
    void showPost(WebContext ctx, String forum, int poster, String subj, String text, int id, Timestamp time, CLDRLocale loc, int xpath) {
    	boolean old = time.before(oldOnOrBefore);
        ctx.println("<div "+(old?"style='background-color: #dde;' ":"")+" class='respbox'>");
        if(old) {
        	ctx.println("<i style='background-color: white;'>Note: This is an old post, from a previous period of CLDR vetting.</i><br><br>");
        }
        String name = getNameLinkFromUid(ctx,poster);
        ctx.println("<div class='person'><a href='"+ctx.url()+"?x=list&u="+poster+"'>"+        name+"</a><br>"+time.toString()+"<br>");
        if(loc != null) {
            ctx.println("<span class='reply'>");
            sm.printLocaleLink(ctx, loc, loc.toString());
            ctx.println("</span> * ");
        }
        if(xpath != -1) {
            ctx.println("<span class='reply'><a href='"+
                forumUrl(ctx,forum)+"&"+F_DO+"="+F_VIEW+"&id="+id+"'>View Item</a></span> * ");
        }
        ctx.println("<span class='reply'><a href='"+
            forumUrl(ctx,forum)+
                ((loc!=null)?("&_="+loc):"")+
                "&"+F_DO+"="+F_REPLY+"&replyto="+id+"#replyto'>Reply</a></span>");
        ctx.println("</div>");
        ctx.println("<h3>"+subj+" </h3>");
            
        ctx.println("<div class='response'>"+preparePostText(text)+"</div>");
        ctx.println("</div>");
    }

   String getNameLinkFromUid(UserRegistry.User me, int uid) {
        UserRegistry.User theU = null;
        theU = sm.reg.getInfo(uid);
        String aLink = null;
        if((theU!=null)&&
           (me!=null)&&
                ((uid==me.id) ||   //if it's us or..
                (UserRegistry.userIsTC(me) ||  //or  TC..
                (UserRegistry.userIsVetter(me) && (true ||  // approved vetter or ..
                                                me.org.equals(theU.org)))))) { // vetter&same org
            if((me==null)||(me.org == null)) {
                throw new InternalError("null: c.s.u.o");
            }
            if((theU!=null)&&(theU.org == null)) {
                throw new InternalError("null: theU.o");
            }
//                boolean sameOrg = (ctx.session.user.org.equals(theU.org));
            aLink = "<a href='mailto:"+theU.email+"'>" + theU.name + " (" + theU.org + ")</a>";
        } else if(theU != null) {
            aLink = "("+theU.org+" vetter #"+uid+")";
        } else {
            aLink = "(#"+uid+")";
        }
        
        return aLink;
   }

   String getNameTextFromUid(UserRegistry.User me, int uid) {
        UserRegistry.User theU = null;
        theU = sm.reg.getInfo(uid);
        String aLink = null;
        if((theU!=null)&&
           (me!=null)&&
                ((uid==me.id) ||   //if it's us or..
                (UserRegistry.userIsTC(me) ||  //or  TC..
                (UserRegistry.userIsVetter(me) && (true ||  // approved vetter or ..
                                                me.org.equals(theU.org)))))) { // vetter&same org
            if((me==null)||(me.org == null)) {
                throw new InternalError("null: c.s.u.o");
            }
            if((theU!=null)&&(theU.org == null)) {
                throw new InternalError("null: theU.o");
            }
//                boolean sameOrg = (ctx.session.user.org.equals(theU.org));
            aLink = theU.name + " (" + theU.org + ")";
        } else if(theU != null) {
            aLink = "("+theU.org+" vetter #"+uid+")";
        } else {
            aLink = "(#"+uid+")";
        }
        
        return aLink;
   }

    String getNameLinkFromUid(WebContext ctx, int uid) {
        if(ctx.session==null || ctx.session.user==null) {
            return getNameLinkFromUid((UserRegistry.User)null, uid);
        } else {
            return getNameLinkFromUid(ctx.session.user, uid);
        }
    }

    /** 
     * Called by SM to create the reg
     * @param xlogger the logger to use
     * @param ourConn the conn to use
     * @param isNew  true if should CREATE TABLEs
     */
    public static SurveyForum createTable(java.util.logging.Logger xlogger, Connection ourConn, SurveyMain sm) throws SQLException {
        SurveyForum reg = new SurveyForum(xlogger,ourConn,sm);
        reg.setupDB(); // always call - we can figure it out.
        reg.myinit();
//        logger.info("SurveyForum DB: Created.");
        return reg;
    }

    public SurveyForum(java.util.logging.Logger xlogger, Connection ourConn, SurveyMain ourSm) {
        logger = xlogger;
        conn = ourConn;
        sm = ourSm;
    }
    
    /**
     * Called by SM to shutdown
     */
    public void shutdownDB() throws SQLException {
        synchronized(conn) {
            conn.close();
            conn = null;
        }
    }
    
    Date oldOnOrBefore = null;
    
    public void reloadLocales() throws SQLException  {
        String sql="";
        String what = "";
    	synchronized(conn) {
    		  {
                  ElapsedTimer et = new ElapsedTimer("setting up DB_LOC2FORUM");
                      Statement s = conn.createStatement();
                  if(!sm.hasTable(conn, DB_LOC2FORUM)) { // user attribute
                      what=DB_LOC2FORUM;
                      sql="";
                      
                     // System.err.println("setting up "+DB_LOC2FORUM);
                      sql = "CREATE TABLE " + DB_LOC2FORUM +
                            " ( " + 
                                " locale VARCHAR(255) NOT NULL, " +
                                " forum VARCHAR(255) NOT NULL" +
                              " )";
                      s.execute(sql);
                      sql = "CREATE UNIQUE INDEX " + DB_LOC2FORUM + "_loc ON " + DB_LOC2FORUM + " (locale) ";
                      s.execute(sql); 
                      sql = "CREATE INDEX " + DB_LOC2FORUM + "_f ON " + DB_LOC2FORUM + " (forum) ";
                      s.execute(sql); 
                  } else {
                      int n = s.executeUpdate("delete from " + DB_LOC2FORUM);
                      //System.err.println("Deleted " + n + " from " + DB_LOC2FORUM);
                  }
                  s.close();
                  
                  PreparedStatement initbl = prepareStatement("initbl", "INSERT INTO " + DB_LOC2FORUM + " (locale,forum) VALUES (?,?)");
                  int updates = 0;
                  int errs = 0;
                  for(CLDRLocale l: SurveyMain.getLocalesSet()) {
                      initbl.setString(1,l.toString());
                      String forum = l.getLanguage();
                      initbl.setString(2,forum);
                      try {
                          int n = initbl.executeUpdate();
                          if(n>0) {
                              updates++;
                          }
                      } catch(SQLException se) {
                          if(errs==0) {
                              System.err.println("While updating " + DB_LOC2FORUM + " -  " + SurveyMain.unchainSqlException(se) + " - " + l+":"+forum+",  [This and further errors, ignored]");
                          }
                          errs++;
                      }
                  }
                  initbl.close();
                  conn.commit();
                  //System.err.println("Updated "+DB_LOC2FORUM+": " + et + ", "+updates+" updates, and " + errs + " SQL complaints");
                  what="?";
              }
    		 }
    	
    }

    /**
     * internal - called to setup db
     */
    private void setupDB() throws SQLException
    {
    	String onOrBefore = sm.survprops.getProperty("CLDR_OLD_POSTS_BEFORE", "12/31/69");
		DateFormat sdf = DateFormat.getDateInstance(DateFormat.SHORT, ULocale.US);
    	try {
    		oldOnOrBefore = sdf.parse(onOrBefore);
    	} catch(Throwable t) {
    		System.err.println("Error in parsing CLDR_OLD_POSTS_BEFORE : " + onOrBefore + " - err " + t.toString());
    		t.printStackTrace();
    		oldOnOrBefore = null;
    	}
    	if(oldOnOrBefore == null) {
    		oldOnOrBefore = new Date(0);
    	}
    	System.err.println("CLDR_OLD_POSTS_BEFORE: date: " + sdf.format(oldOnOrBefore) + " (format: mm/dd/yy)");
        synchronized(conn) {
            String what="";
            String sql = null;
//            logger.info("SurveyForum DB: initializing...");
            String locindex = "loc";
            if(sm.db_Mysql) {
                locindex = "loc(999)";
            }

            if(!sm.hasTable(conn, DB_FORA)) { // user attribute
                Statement s = conn.createStatement();
                what=DB_FORA;
                sql="";
                
                sql = "CREATE TABLE " + DB_FORA +
                      " ( " + 
                          " id INT NOT NULL "+sm.DB_SQL_IDENTITY+", " + 
                          " loc VARCHAR(999) NOT NULL, " + // interest locale
                          " first_time "+sm.DB_SQL_TIMESTAMP0+" NOT NULL "+sm.DB_SQL_WITHDEFAULT+" "+sm.DB_SQL_CURRENT_TIMESTAMP0+", " +
                          " last_time TIMESTAMP NOT NULL "+sm.DB_SQL_WITHDEFAULT+" CURRENT_TIMESTAMP" + 
                        " )";
                s.execute(sql);
                sql="";
                s.close();
                conn.commit();
                what="?";
            }
            if(!sm.hasTable(conn, DB_POSTS)) { // user attribute
                Statement s = conn.createStatement();
                what=DB_POSTS;
                sql="";
                
                sql = "CREATE TABLE " + DB_POSTS +
                      " ( " + 
                          " id INT NOT NULL "+sm.DB_SQL_IDENTITY+", " +
                          " forum INT NOT NULL, " + // which forum (DB_FORA), i.e. de
                          " poster INT NOT NULL, " + 
                          " subj "+sm.DB_SQL_UNICODE+", " + 
                          " text "+sm.DB_SQL_UNICODE+" NOT NULL, " +
                          " parent INT "+sm.DB_SQL_WITHDEFAULT+" -1, " +
                          " loc VARCHAR(999), " + // specific locale, i.e. de_CH
                          " xpath INT, " + // base xpath 
                          " first_time "+sm.DB_SQL_TIMESTAMP0+" NOT NULL "+sm.DB_SQL_WITHDEFAULT+" "+sm.DB_SQL_CURRENT_TIMESTAMP0+", " +
                          " last_time TIMESTAMP NOT NULL "+sm.DB_SQL_WITHDEFAULT+" CURRENT_TIMESTAMP" + 
                        " )";
                s.execute(sql);
                sql = "CREATE UNIQUE INDEX " + DB_POSTS + "_id ON " + DB_POSTS + " (id) ";
                s.execute(sql); 
                sql = "CREATE INDEX " + DB_POSTS + "_ut ON " + DB_POSTS + " (poster, last_time) ";
                s.execute(sql); 
                sql = "CREATE INDEX " + DB_POSTS + "_utt ON " + DB_POSTS + " (id, last_time) ";
                s.execute(sql); 
                sql = "CREATE INDEX " + DB_POSTS + "_chil ON " + DB_POSTS + " (parent) ";
                s.execute(sql); 
                sql = "CREATE INDEX " + DB_POSTS + "_loc ON " + DB_POSTS + " ("+locindex+") ";
                s.execute(sql); 
                sql = "CREATE INDEX " + DB_POSTS + "_x ON " + DB_POSTS + " (xpath) ";
                s.execute(sql); 
                sql="";
                s.close();
                conn.commit();
                what="?";
            }
            
          reloadLocales();
        }
    }
    
    public Connection conn = null;
    SurveyMain sm = null;
    
    public String statistics() {
        return "SurveyForum: nothing to report";
    }
    
    
    public PreparedStatement prepareStatement(String name, String sql) {
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql,ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
        } catch ( SQLException se ) {
            String complaint = "Vetter:  Couldn't prepare " + name + " - " + SurveyMain.unchainSqlException(se) + " - " + sql;
            logger.severe(complaint);
            throw new RuntimeException(complaint);
        }
        return ps;
    }
    
    PreparedStatement fAdd = null;
    PreparedStatement fGetById = null;
    PreparedStatement fGetByLoc = null;
    
    PreparedStatement pList = null;
    PreparedStatement pAdd = null;
    PreparedStatement pAll = null;
    PreparedStatement pForMe = null;
    PreparedStatement pGet = null;
    PreparedStatement pCount = null;
  //  PreparedStatement pAllN = null;
  
    PreparedStatement pIntUsers = null;
    
    public void myinit() throws SQLException {
        synchronized(conn) {
            fGetById = prepareStatement("fGetById", "SELECT loc FROM " + DB_FORA + " where id=?");
            fGetByLoc = prepareStatement("fGetByLoc", "SELECT id FROM " + DB_FORA + " where loc=?");
            fAdd = prepareStatement("fAdd", "INSERT INTO " + DB_FORA + " (loc) values (?)");
            
            pList = prepareStatement("pList", "SELECT poster,subj,text,id,last_time,loc,xpath FROM " + DB_POSTS + 
            " WHERE (forum = ?) ORDER BY last_time DESC ");
            pCount = prepareStatement("pCount", "SELECT COUNT(*) from " + DB_POSTS + 
                " WHERE (forum = ?)");
            pGet = prepareStatement("pGet", "SELECT poster,subj,text,id,last_time,loc,xpath,forum FROM " + DB_POSTS + 
            " WHERE (id = ?)");
            pAdd = prepareStatement("pAdd", "INSERT INTO " + DB_POSTS + " (poster,subj,text,forum,parent,loc,xpath) values (?,?,?,?,?,?,?)");
            
            final String pAllResult = DB_POSTS+".poster,"+DB_POSTS+".subj,"+DB_POSTS+".text,"+DB_POSTS+".last_time,"+DB_POSTS+".id,"+DB_POSTS+".forum,"+DB_FORA+".loc";
            pAll = prepareStatement("pAll", "SELECT "+pAllResult+" FROM " + DB_POSTS + ","+DB_FORA+" WHERE ("+DB_POSTS+".forum = "+DB_FORA+".id) ORDER BY "+DB_POSTS+".last_time DESC");
//            pMine = prepareStatement("pAll", "SELECT "+DB_POSTS+".poster,"+DB_POSTS+".subj,"+DB_POSTS+".text,"+DB_POSTS+".last_time,"+DB_POSTS+".id,"+DB_POSTS+".forum,"+DB_FORA+".loc"+" FROM " + DB_POSTS + ","+DB_FORA+" WHERE ("+DB_POSTS+".forum = "+DB_FORA+".id) ORDER BY "+DB_POSTS+".last_time DESC");
            //pAllN = prepareStatement("pAllN", "SELECT "+DB_POSTS+".poster,"+DB_POSTS+".subj,"+DB_POSTS+".text,"+DB_POSTS+".last_time,"+DB_POSTS+".id,"+DB_POSTS+".forum,"+DB_FORA+".loc"+" FROM " + DB_POSTS + ","+DB_FORA+" WHERE ("+DB_POSTS+".forum = "+DB_FORA+".id) ORDER BY "+DB_POSTS+".last_time DESC");
            pForMe = prepareStatement("pForMe", 
                "SELECT "+ pAllResult + " FROM " + DB_POSTS +","+DB_FORA+" " // same as pAll  
                    +" where ("+DB_POSTS+".forum="+DB_FORA+".id) AND exists ( select "+UserRegistry.CLDR_INTEREST+".forum from "+UserRegistry.CLDR_INTEREST+","+DB_FORA+" where "+UserRegistry.CLDR_INTEREST+".uid=? AND "+UserRegistry.CLDR_INTEREST+".forum="+DB_FORA+".loc AND "+DB_FORA+".id="+DB_POSTS+".forum) ORDER BY "+DB_POSTS+".last_time DESC");
                    
            pIntUsers = prepareStatement("pIntUsers",
                "SELECT uid from "+UserRegistry.CLDR_INTEREST+" where forum=?");
        }
    } 
    
       
    /**
     * @deprecated section is not needed.
     * @param ctx
     * @param section
     * @param p
     * @param xpath
     * @param contents
     */
    void showForumLink(WebContext ctx, DataSection section, DataSection.DataRow p, int xpath, String contents) {
    	showForumLink(ctx, p, xpath, contents);
    }
    void showForumLink(WebContext ctx, DataSection.DataRow p, int xpath, String contents) {
        //if(ctx.session.user == null) {     
        //    return; // no user?
        //}
//        String title;
/*        if(!ctx.session.user.interestedIn(forum)) {
            title = " (not on your interest list)";
        }*/
//        title = null /*+ title*/;
        ctx.println("<a target='"+ctx.atarget("n:"+ctx.getLocale())+"' class='forumlink' href='"+forumUrl(ctx,p,xpath)+"' >" // title='"+title+"'
            +contents+ "</a>");
    }
    void showForumLink(WebContext ctx, DataSection section, DataSection.DataRow p, int xpath) {
            showForumLink(ctx,section,p,xpath,ctx.iconHtml("zoom","zoom"));
    }
    // "link" UI
    static public String forumUrl(WebContext ctx, DataSection.DataRow p, int xpath) {
        return ctx.base()+"?_="+ctx.getLocale()+"&"+F_FORUM+"="+p.getIntgroup()+"&"+F_XPATH+"="+xpath;
    }
    static public String localeToForum(String locale) {
        return localeToForum(new ULocale(locale));
    }
    static public String localeToForum(ULocale locale) {
        return locale.getLanguage();
    }
    static public String forumUrl(WebContext ctx, String locale, int xpath) {
        ULocale u = new ULocale(locale);
        return ctx.base()+"?_="+locale+"&"+F_FORUM+"="+u.getLanguage()+"&"+F_XPATH+"="+xpath;
    }
    static String forumUrl(WebContext ctx, String forum) {
        return (ctx.base()+"?"+F_FORUM+"="+forum);
    }
    String returnText(WebContext ctx, int base_xpath) {
        return "Zoom out to <a href='"+returnUrl(ctx,ctx.getLocale(),base_xpath)+"'>"+ctx.iconHtml("zoom","zoom out to " + ctx.getLocale())+" "+ ctx.getLocale()+"</a>";
    }
    
    // XML/RSS
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    
    private static void sendErr(HttpServletRequest request, HttpServletResponse response, String err) 
    throws IOException {
        response.setContentType("text/html; charset=utf-8");
        WebContext xctx = new WebContext(request,response);
        xctx.println("Error: " + err );
        xctx.close();
        return;
    }

public boolean doFeed(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException {
    
        response.setContentType("application/rss+xml; charset=utf-8");
        
        String feedType = request.getParameter("feed");
        if(feedType == null) {
            feedType = "rss_0.94";
        }
        
        String email = request.getParameter("email");
        String pw = request.getParameter("pw");
        
        if(email==null || pw==null) {
            sendErr(request, response, "URL error.");
            return true;
        }
        
        UserRegistry.User user;
        user = sm.reg.get(pw,email,"RSS@"+request.getRemoteAddr());
        
        if(user == null) {
            sendErr(request, response, "authentication err");
            return true;
        }
        
        String base = "http://"+request.getServerName()+":"+request.getServerPort()+request.getContextPath() + request.getServletPath();
        String kind = request.getParameter("kind");
        String loc = request.getParameter("_");
        
        if((loc!=null) && (!UserRegistry.userCanModifyLocale(user, CLDRLocale.getInstance(loc)))) {
            sendErr(request, response, "permission denied for locale "+loc);
            return true;
        }
        
        DateFormat dateParser = new SimpleDateFormat(DATE_FORMAT);
        
        try {
            SyndFeed feed = new SyndFeedImpl();
            feed.setFeedType(feedType);
            List entries = new ArrayList();
            feed.setLink(base);

            if(loc != null) {
                feed.setTitle("CLDR Feed for " + loc);
                feed.setDescription("test feed");
                
                SyndEntry entry;
                SyndContent description;
                
                synchronized (conn) {
                    
                    int forumNumber = getForumNumberFromDB(loc);
                    int count=0;
                    if(forumNumber >= GOOD_FORUM) try {
                        pList.setInt(1, forumNumber);
                        ResultSet rs = pList.executeQuery();
                        
                        while(rs.next()) {                        
                            int poster = rs.getInt(1);
                            String subj = rs.getString(2);
                            String text = rs.getString(3);
                            int id = rs.getInt(4);
                            java.sql.Timestamp lastDate = rs.getTimestamp(5);
                            String ploc = rs.getString(6);
                            int xpath = rs.getInt(7);
                            
                            String nameLink = getNameTextFromUid(user,poster);
                            
                            entry = new SyndEntryImpl();
                            entry.setTitle(subj);
                            entry.setAuthor(nameLink);
                            entry.setLink(base+"?forum="+loc+"&amp;"+F_DO+"="+F_VIEW+"&amp;id="+id+"&amp;email="+
                                          email + "&amp;pw="+pw);
                            entry.setPublishedDate(lastDate); // dateParser.parse("2004-06-08"));
                            description = new SyndContentImpl();
                            description.setType("text/html");
                            description.setValue("From: "+nameLink+"<br><hr>"+ shortenText(text));
                            entry.setDescription(description);
                            entries.add(entry);
                            
                            count++;
                        }
                    } catch (SQLException se) {
                        String complaint = "SurveyForum:  Couldn't use forum " +loc + " - " + SurveyMain.unchainSqlException(se) + " - fGetByLoc";
                        logger.severe(complaint);
                        throw new RuntimeException(complaint);
                    }
                }
            } else {  
                feed.setTitle("CLDR Feed for " + user.email);
                String locs[] = user.getInterestList();
                if(locs == null) {
                    feed.setDescription("All CLDR locales.");
                } else if(locs.length == 0) {
                    feed.setDescription("No locales.");
                } else {
                    String locslist = null;
                    for(String l : locs) {
                        if(locslist == null) {
                            locslist = l;
                        } else {
                            locslist = locslist + " " + l;
                        }
                    }
                    feed.setDescription("Your locales: "+locslist);
                }
                
                SyndEntry entry;
                SyndContent description;
                // not a specific locale, but ALL [of my locales]
                
                try {
                    // first, articles.
                    ResultSet rs = null; // list of articles.
                    
                    if(locs == null) {
                        rs = pAll.executeQuery();
                    } else {
                        pForMe.setInt(1, user.id);
                        rs = pForMe.executeQuery();
                    }
                    
                    int count =0;

                    while(rs.next() && true) {
                        int poster = rs.getInt(1);
                        String subj = rs.getString(2);
                        String text = rs.getString(3);
                        java.sql.Timestamp lastDate = rs.getTimestamp(4); //TODO: timestamp
                        int id = rs.getInt(5);
                        String forum = rs.getString(6);
                        String ploc = rs.getString(7);
                        
                        String forumText = new ULocale(ploc).getLanguage();
                        String nameLink = getNameTextFromUid(user,poster);
                        
                        entry = new SyndEntryImpl();
                        String forumPrefix = forumText+":";
                        if(subj.startsWith(forumText)) {
                            entry.setTitle(subj);
                        } else {
                            entry.setTitle(forumText+":"+subj);
                        }
                        entry.setAuthor(nameLink);
                        entry.setLink(base+"?forum="+forumText+"&amp;"+F_DO+"="+F_VIEW+"&amp;id="+id+"&amp;email="+
                                      email + "&amp;pw="+pw);
                        entry.setPublishedDate(lastDate); // dateParser.parse("2004-06-08"));
                        description = new SyndContentImpl();
                        description.setType("text/html");
                        description.setValue("From: "+nameLink+"<br><hr>"+ shortenText(text));
                        entry.setDescription(description);
                        entries.add(entry);
                        
                        count++;
                    }
                    
                    // now, data??
                    // select CLDR_DATA.value,SF_LOC2FORUM.locale from SF_LOC2FORUM,CLDR_INTEREST,CLDR_DATA where SF_LOC2FORUM.forum=CLDR_INTEREST.forum AND CLDR_INTEREST.uid=2 AND CLDR_DATA.locale=SF_LOC2FORUM.locale AND CLDR_DATA.submitter is null ORDER BY CLDR_DATA.modtime DESC
                    
                } catch (SQLException se) {
                    String complaint = "SurveyForum:  Couldn't use forum s for RSS- " + SurveyMain.unchainSqlException(se) + " - fGetByLoc";
                    logger.severe(complaint);
                    throw new RuntimeException(complaint);
                }
            }
            feed.setEntries(entries);
            
            Writer writer = response.getWriter();
            SyndFeedOutput output = new SyndFeedOutput();
            output.output(feed,writer);
            //writer.close();
            
            //System.out.println("The feed has been written to the file ["+fileName+"]");
        } catch (Throwable ie) {
            System.err.println("Error getting RSS feed: " + ie.toString());
            ie.printStackTrace();
            // todo: err
        }
        return true;
    }
    
    private static String shortenText(String s) {
        if(s.length()<100) {
            return s;
        } else {
            return s.substring(0,100)+"...";
        }
    }
    
    String forumFeedStuff(WebContext ctx) {
        if(ctx.session == null ||
           ctx.session.user == null ||
           !UserRegistry.userIsStreet(ctx.session.user)) {
            return "";
        }
        String feedUrl = ctx.schemeHostPort()+  ctx.base()+("/feed?_="+ctx.getLocale().getLanguage()+"&amp;email="+ctx.session.user.email+"&amp;pw="+
            ctx.session.user.password+"&amp;");
        return 
            /* "<link rel=\"alternate\" type=\"application/atom+xml\" title=\"Atom 1.0\" href=\""+feedUrl+"&feed=atom_1.0\">" + */
              "<link rel=\"alternate\" type=\"application/rdf+xml\" title=\"RSS 1.0\" href=\""+feedUrl+"&feed=rss_1.0\">"+
             "<link rel=\"alternate\" type=\"application/rss+xml\" title=\"RSS 2.0\" href=\""+feedUrl+"&feed=rss_2.0\">" 
           ;
    }
    
    
    
    String forumFeedIcon(WebContext ctx, String forum) {
        if(ctx.session == null ||
           ctx.session.user == null ||
           !UserRegistry.userIsStreet(ctx.session.user)) {
            return "";
        }
        String feedUrl = ctx.schemeHostPort()+  ctx.base()+("/feed?_="+ctx.getLocale().getLanguage()+"&amp;email="+ctx.session.user.email+"&amp;pw="+
            ctx.session.user.password+"&amp;");
        
        return  " <a href='"+feedUrl+"&feed=rss_2.0"+"'>"+ctx.iconHtml("feed","RSS 2.0")+"rss</a>"; /* | " +
                "<a href='"+feedUrl+"&feed=rss_2.0"+"'>"+ctx.iconHtml("feed","RSS 1.0")+"RSS 1.0</a>"; */
            
    }


    String mainFeedStuff(WebContext ctx) {
        if(ctx.session == null ||
           ctx.session.user == null ||
           !UserRegistry.userIsStreet(ctx.session.user)) {
            return "";
        }

        String feedUrl = ctx.schemeHostPort()+  ctx.base()+("/feed?email="+ctx.session.user.email+"&amp;pw="+
            ctx.session.user.password+"&amp;");
        return 
             "<link rel=\"alternate\" type=\"application/atom+xml\" title=\"Atom 1.0\" href=\""+feedUrl+"&feed=atom_1.0\">" +
              "<link rel=\"alternate\" type=\"application/rdf+xml\" title=\"RSS 1.0\" href=\""+feedUrl+"&feed=rss_1.0\">"+
             "<link rel=\"alternate\" type=\"application/rss+xml\" title=\"RSS 2.0\" href=\""+feedUrl+"&feed=rss_2.0\">" 
           ;
    }
    
    
    String mainFeedIcon(WebContext ctx) {
        if(ctx.session == null ||
           ctx.session.user == null ||
           !UserRegistry.userIsStreet(ctx.session.user)) {
            return "";
        }
        String feedUrl = ctx.schemeHostPort()+  ctx.base()+("/feed?email="+ctx.session.user.email+"&amp;pw="+
            ctx.session.user.password+"&amp;");
        
        return  "<a href='"+feedUrl+"&feed=rss_2.0"+"'>"+ctx.iconHtml("feed","RSS 2.0")+"RSS 2.0</a>"; /* | " +
                "<a href='"+feedUrl+"&feed=rss_2.0"+"'>"+ctx.iconHtml("feed","RSS 1.0")+"RSS 1.0</a>"; */
            
    }    
}

