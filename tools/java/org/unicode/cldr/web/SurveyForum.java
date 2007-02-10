//
//  SurveyForum.java
//  fivejay
//
//  Created by Steven R. Loomis on 27/10/2006.
//  Copyright 2006-2007 IBM. All rights reserved.
//

package org.unicode.cldr.web;

import java.io.*;
import java.util.*;

import java.sql.Connection;
import java.sql.Timestamp;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.PreparedStatement;

import com.ibm.icu.util.ULocale;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.test.CheckCLDR;

/**
 * This class implements a discussion forum per language (ISO code)
 */
public class SurveyForum {
    private static java.util.logging.Logger logger;


    public static String DB_FORA = "SF_FORA";
    public static String DB_POSTS = "SF_POSTS";

    /* --------- FORUM ------------- */
    static final String F_FORUM = "forum";
    static final String F_XPATH = "xpath";
    static final String F_DO = "d";
    static final String F_LIST = "list";
    static final String F_VIEW = "view";
    static final String F_ADD = "add";
    static final String F_REPLY = "reply";
    static final String F_POST = "post";
    
    
    /** 
     * prepare text for posting
     */
    static public String preparePostText(String intext) {
        return 
            intext.replaceAll("\r","")
                  .replaceAll("\n","<p>");
    }

    static String HTMLSafe(String s) {
        if(s==null) return null;
        
        return 
            s.replaceAll("&","&amp;")
             .replaceAll("<","&lt;")
             .replaceAll(">","&gt;")
             .replaceAll("\"","&quot;");
    }
    
    Hashtable numToName = new Hashtable();
    Hashtable nameToNum = new Hashtable();
    
    synchronized int getForumNumber(String forum) {
        // make sure it is a valid src!
        if((forum==null)||(forum.indexOf('_')>=0)||!sm.isValidLocale(forum)) {
            throw new RuntimeException("Invalid forum: " + forum);
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
                return -1;
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
        if(num == -1) {
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
        
        if(num==-1) {
            throw new RuntimeException("Couldn't query ID for forum " + forum);
        }
        // Add to list
        Integer i = new Integer(num);
        numToName.put(forum,i);
        nameToNum.put(i,forum);
        return num;
    }
    
    void doForum(WebContext ctx, String sessionMessage) throws IOException { 
        /* OK, let's see what we are doing here. */
        String forum = ctx.field(F_FORUM);
        int base_xpath = ctx.fieldInt(F_XPATH);
        int forumNumber = getForumNumber(forum);
        String pD = ctx.field(F_DO); // do
        boolean loggedout = ((ctx.session==null)||(ctx.session.user==null));
        boolean canModify = false;

        if((ctx.locale == null) && (forumNumber != -1)) {
            ctx.setLocale(new ULocale(forum));
        }
        
        if(!loggedout) {
            canModify = (UserRegistry.userCanModifyLocale(ctx.session.user,ctx.locale.toString()));
        }
        
        // Are they just zooming in?
        if(!canModify && (base_xpath!=-1)) {
            doZoom(ctx, sessionMessage);
            return;
        }
        
        // User isnt logged in.
        if(loggedout) {
            sm.printHeader(ctx,"Fora | Please login.");
            sm.printUserMenu(ctx);
            if(sessionMessage != null) {
                ctx.println(sessionMessage+"<hr>");
            }
            ctx.println("<hr><strong>You aren't logged in. Please login to continue.</strong><p>");
            sm.printFooter(ctx);
            return;
        }
        
        // User has an account, but does not have access to this forum.
        if(!canModify) {
            sm.printHeader(ctx,"Fora | Access Denied.");
            sm.printUserMenu(ctx);
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
        } else if(forumNumber == -1) {
            sm.printHeader(ctx,"Fora");
                sm.printUserMenu(ctx);
            // no forum or bad forum. Do general stuff.
//            doForumForum(ctx, pF, pD);
        } else {
            // list what is in a certain forum
            doForumForum(ctx, forum, forumNumber);
        }
        
        if(sessionMessage != null) {
            ctx.println("<hr>"+sessionMessage);
        }
        sm.printFooter(ctx);
    }
    
    String returnUrl(WebContext ctx, String locale, int base_xpath) {
        String xpath = sm.xpt.getById(base_xpath);
        String theMenu = SurveyMain.xpathToMenu(xpath);
        if(theMenu==null) {
            theMenu="raw";
        }
        return ctx.base()+"?"+
					"_="+locale+"&amp;x="+theMenu+"&amp;xfind="+base_xpath+"#x"+base_xpath;
    }
    
    void doZoom(WebContext ctx, String sessionMessage) {
        int base_xpath = ctx.fieldInt(F_XPATH);
        String xpath = sm.xpt.getById(base_xpath);
        sm.printHeader(ctx,"Zoom | " + ctx.locale + " | Zoom on #" + base_xpath); // TODO: pretty path?
        sm.printUserMenu(ctx);
        if(sessionMessage != null) {
            ctx.println(sessionMessage+"<hr>");
        }
        ctx.println("Return to <a href='"+returnUrl(ctx,ctx.locale.toString(),base_xpath)+"'>"+ctx.locale+"</a><hr>");
        showXpath(ctx, xpath, base_xpath, ctx.locale);
        ctx.println("<hr>Return to <a href='"+returnUrl(ctx,ctx.locale.toString(),base_xpath)+"'>"+ctx.locale+"</a><br/>");
        sm.printFooter(ctx);
    }
    
    void doXpathPost(WebContext ctx, String forum, int forumNumber, int base_xpath) throws IOException {
        String xpath = sm.xpt.getById(base_xpath);
        int replyTo = ctx.fieldInt("replyto");
        
        // Don't want to call printHeader here - becasue if we do a post, we're going ot be
        // redirecting outta here. 
        String subj = HTMLSafe(ctx.field("subj"));
        String text = HTMLSafe(ctx.field("text"));
        
        if((subj == null) || (subj.trim().length()==0)) {
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
            
            subj = ctx.locale + ": " +subj;
        } else {
            subj = subj.trim();
        }

        if(text!=null && text.length()>0) {
            if(ctx.field("post").length()>0) {
                // do the post!
                
                if(forumNumber == -1) {
                    throw new RuntimeException("Bad forum: " + forum);
                }
                
                synchronized(conn) {                
                    try {
                        pAdd.setInt(1,ctx.session.user.id);
                        pAdd.setString(2, subj);
                        pAdd.setString(3, preparePostText(text));
                        pAdd.setInt(4, forumNumber);
                        pAdd.setInt(5, -1); // no parent
                        pAdd.setString(6,ctx.locale.toString());
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
                
                
                ctx.redirect(ctx.base()+"?_="+ctx.locale.toString()+"&"+F_FORUM+"="+forum+"&didpost=t");
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
            sm.printHeader(ctx,"Fora | " + forum + " | Zoom on #" + base_xpath);
            printForumMenu(ctx, forum);
        }
        
        
        if(ctx.field("text").length()==0 &&
           ctx.field("subj").length()==0) {
            // hide the 'post comment' thing
            String warnHash = "post_comment"+base_xpath+"_"+forum;
            ctx.println("<div id='h_"+warnHash+"'><a href='javascript:show(\"" + warnHash + "\")'>" + 
                        "<b>+</b> Ask a question or post a comment..</a></div>");
            ctx.println("<!-- <noscript>Warning: </noscript> -->" + 
                        "<div style='display: none' class='pager' id='" + warnHash + "'>" );
            ctx.println("<a href='javascript:hide(\"" + warnHash + "\")'>" + 
                        "(<b>-</b> Don't post comment)</a>");
        }
                
        ctx.println("<form method='POST' action='"+ctx.base()+"'>");
        
        ctx.println("<input type='hidden' name='"+F_FORUM+"' value='"+forum+"'>");
        ctx.println("<input type='hidden' name='"+F_XPATH+"' value='"+base_xpath+"'>");
        ctx.println("<input type='hidden' name='_' value='"+ctx.locale+"'>");
        ctx.println("<input type='hidden' name='replyto' value='"+replyTo+"'>");

        ctx.println("<b>Subject</b>: <input name='subj' size=40 value='"+subj+"'><br>");
        ctx.println("<textarea rows=12 cols=60 name='text'>"+(text==null?"":text)+"</textarea>");
        ctx.println("<br><input name=post type=submit value=Post><input type=submit name=preview value=Preview><br>");
        ctx.println("</form>");
        
        if(ctx.field("post").length()>0) {
            if(text==null || text.length()==0) {
                ctx.println("<b>Please type some text.</b><br>");
            } else {
              //  ...
            }
        }

        if(ctx.field("text").length()==0 &&
           ctx.field("subj").length()==0) {
            ctx.println("</div>");
        }
        
        if(replyTo != -1) {
            ctx.println("<h4>In Reply To:</h4><span class='reply-to'>");
            String subj2 = showItem(ctx, forum, forumNumber, replyTo, false); 
            ctx.println("</span>");
        }
        if(base_xpath != -1) {
            ctx.println("Return to <a href='"+returnUrl(ctx,ctx.locale.toString(),base_xpath)+"'>"+ctx.locale+"</a><hr>");
            showXpath(ctx, xpath, base_xpath, ctx.locale);
            ctx.println("<hr>Return to <a href='"+returnUrl(ctx,ctx.locale.toString(),base_xpath)+"'>"+ctx.locale+"</a><br/>");
        }
     }
    
    private void showXpath(WebContext baseCtx, String xpath, int base_xpath, ULocale loc) {
        WebContext ctx = new WebContext(baseCtx);
        ctx.setLocale(loc);
        // Show the Pod in question:
//        ctx.println("<hr> \n This post Concerns:<p>");
        boolean canModify = (UserRegistry.userCanModifyLocale(ctx.session.user,ctx.locale.toString()));
        String podBase = DataPod.xpathToPodBase(xpath);
        
        sm.printPathListOpen(ctx);

        if(canModify) {
            /* hidden fields for that */
            ctx.println("<input type='hidden' name='"+F_FORUM+"' value='"+ctx.locale.getLanguage()+"'>");
            ctx.println("<input type='hidden' name='"+F_XPATH+"' value='"+base_xpath+"'>");
            ctx.println("<input type='hidden' name='_' value='"+loc+"'>");

            ctx.println("<input style='float:right' type='submit' value='" + sm.xSAVE + "'>");
            synchronized (ctx.session) { // session sync
                // first, do submissions.
                DataPod oldPod = ctx.getExistingPod(podBase);
                
                // *** copied from SurveyMain.showLocale() ... TODO: refactor
                SurveyMain.UserLocaleStuff uf = sm.getUserFile(ctx, (ctx.session.user==null)?null:ctx.session.user, ctx.locale);
                CLDRFile cf = uf.cldrfile;
                if(cf == null) {
                    throw new InternalError("CLDRFile is null!");
                }
                CLDRDBSource ourSrc = uf.dbSource; // TODO: remove. debuggin'
                
                if(ourSrc == null) {
                    throw new InternalError("oursrc is null! - " + (SurveyMain.USER_FILE + SurveyMain.CLDRDBSRC) + " @ " + ctx.locale );
                }
                synchronized(ourSrc) { 
                    // Set up checks
                    CheckCLDR checkCldr = (CheckCLDR)uf.getCheck(ctx); //make it happen
                
                    if(sm.processPeaChanges(ctx, oldPod, cf, ourSrc)) {
                        int j = sm.vet.updateResults(oldPod.locale); // bach 'em
                        ctx.println("<br> You submitted data or vote changes, and " + j + " results were updated. As a result, your items may show up under the 'priority' or 'proposed' categories.<br>");
                    }
                }
            }
        } else {
//            ctx.println("<br>cant modify " + ctx.locale + "<br>");
        }
        
        DataPod pod = ctx.getPod(podBase);
        
        SurveyMain.printPodTableOpen(ctx, pod, true);
        sm.showPeas(ctx, pod, canModify, base_xpath, true);
        SurveyMain.printPodTableClose(ctx, pod);
        if(canModify) {
            ctx.println("<hr>");
            String warnHash = "add_an_alt";
            ctx.println("<div id='h_"+warnHash+"'><a href='javascript:show(\"" + warnHash + "\")'>" + 
                        "<b>+</b> Add Alternate..</a></div>");
            ctx.println("<!-- <noscript>Warning: </noscript> -->" + 
                        "<div style='display: none' class='pager' id='" + warnHash + "'>" );
            ctx.println("<a href='javascript:hide(\"" + warnHash + "\")'>" + 
                        "(<b>-</b>)</a>");
            ctx.println("<label>To add a new alternate, type it in here and modify any item above:<br> <input name='new_alt'></label><br>");                        
            ctx.println("</div>");

        }
        sm.printPathListClose(ctx);
    }
    
    void printForumMenu(WebContext ctx, String forum) {
        ctx.println("<table width='100%' border='0'><tr><td>");
        ctx.println("<a href=\"" + ctx.url() + "\">" + "<b>Locales</b>" + "</a><br>");
        sm.printListFromInterestGroup(ctx, forum);
        ctx.println("</td><td align='right'>");
        sm.printUserMenu(ctx);
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
                
        ctx.println("<hr>");
        if(didpost) {
            ctx.println("<b>Posted your response.</b><hr>");
        }
        
        synchronized (conn) {
            try {
                pList.setInt(1, forumNumber);
                ResultSet rs = pList.executeQuery();

                while(count<MSGS_PER_PAGE && rs.next()) {
                    if(count==0) {
                        // HEADER 
                    }
                    
                    int poster = rs.getInt(1);
                    String subj = rs.getString(2);
                    String text = rs.getString(3);
                    int id = rs.getInt(4);
                    java.sql.Timestamp lastDate = rs.getTimestamp(5);
                    String loc = rs.getString(6);
                    int xpath = rs.getInt(7);
                 
                    showPost(ctx, forum, poster, subj, text, id, lastDate, loc, xpath);
                 
                    count++;
                }
            } catch (SQLException se) {
                String complaint = "SurveyForum:  Couldn't add forum " +forum + " - " + SurveyMain.unchainSqlException(se) + " - fGetByLoc";
                logger.severe(complaint);
                throw new RuntimeException(complaint);
            }
        }
        
        ctx.println("<hr>"+count+" posts ");
        
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
                String subj = rs.getString(2);
                String text = rs.getString(3);
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
                
                 showPost(ctx, forum, poster, subj, text, id, lastDate, loc, xpath);
                             
                if(xpath != -1) {
                    String xpath_string = sm.xpt.getById(xpath);
                    if(xpath_string != null) {
                        showXpath(ctx, xpath_string, xpath, new ULocale(loc));
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
    void showPost(WebContext ctx, String forum, int poster, String subj, String text, int id, Timestamp time, String loc, int xpath) {
        ctx.println("<div class='respbox'>");
        String name = getNameLinkFromUid(ctx,poster);
        ctx.println("<div class='person'><a href='"+ctx.url()+"?x=list&u="+poster+"'>"+        name+"</a><br>"+time.toString()+"<br>");
        if(loc != null) {
            ctx.println("<span class='reply'>");
            sm.printLocaleLink(ctx, loc, loc);
            ctx.println("</span> * ");
        }
        ctx.println("<span class='reply'><a href='"+
            forumUrl(ctx,forum)+"&"+F_DO+"="+F_VIEW+"&id="+id+"'>View Item</a></span> * ");
        ctx.println("<span class='reply'><a href='"+
            forumUrl(ctx,forum)+
                ((loc!=null)?("&_="+loc):"")+
                "&"+F_DO+"="+F_REPLY+"&replyto="+id+"'>Reply</a></span>");
        ctx.println("</div>");
        ctx.println("<h3>"+subj+" </h3>");
            
        ctx.println("<div class='response'>"+preparePostText(text)+"</div>");
        ctx.println("</div>");
    }


    String getNameLinkFromUid(WebContext ctx, int uid) {
        UserRegistry.User theU = null;
        theU = sm.reg.getInfo(uid);
        String aLink = null;
        if((theU!=null)&&
           (ctx.session.user!=null)&&
                ((uid==ctx.session.user.id) ||   //if it's us or..
                (UserRegistry.userIsTC(ctx.session.user) ||  //or  TC..
                (UserRegistry.userIsVetter(ctx.session.user) && (true ||  // approved vetter or ..
                                                ctx.session.user.org.equals(theU.org)))))) { // vetter&same org
            if((ctx.session.user==null)||(ctx.session.user.org == null)) {
                throw new InternalError("null: c.s.u.o");
            }
            if((theU!=null)&&(theU.org == null)) {
                throw new InternalError("null: theU.o");
            }
//                boolean sameOrg = (ctx.session.user.org.equals(theU.org));
            aLink = "<a href='mailto:"+theU.email+"'>" + theU.name + " (" + theU.org + ")</a>";
        } else if(theU != null) {
            aLink = "("+theU.org+" vetter "+uid+")";
        } else {
            aLink = "(#"+uid+")";
        }
        
        return aLink;
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

    /**
     * internal - called to setup db
     */
    private void setupDB() throws SQLException
    {
        synchronized(conn) {
            String what="";
            String sql = null;
//            logger.info("SurveyForum DB: initializing...");

            if(!sm.hasTable(conn, DB_FORA)) { // user attribute
                Statement s = conn.createStatement();
                what=DB_FORA;
                sql="";
                
                sql = "CREATE TABLE " + DB_FORA +
                      " ( " + 
                          " id INT NOT NULL GENERATED ALWAYS AS IDENTITY, " + 
                          " loc VARCHAR(1024) NOT NULL, " + // interest locale
                          " first_time TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP, " +
                          " last_time TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP" + 
                        " )";
                s.execute(sql);
                sql = "CREATE UNIQUE INDEX " + DB_FORA + "_id_loc ON " + DB_FORA + " (id,loc) ";
                s.execute(sql); 
                sql = "CREATE UNIQUE INDEX " + DB_FORA + "_loc ON " + DB_FORA + " (loc) ";
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
                          " id INT NOT NULL GENERATED ALWAYS AS IDENTITY, " +
                          " forum INT NOT NULL, " + // which forum (DB_FORA), i.e. de
                          " poster INT NOT NULL, " + 
                          " subj VARCHAR(1024), " + 
                          " text VARCHAR(16384) NOT NULL, " +
                          " parent INT WITH DEFAULT -1, " +
                          " loc VARCHAR(1024), " + // specific locale, i.e. de_CH
                          " xpath INT, " + // base xpath 
                          " first_time TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP, " +
                          " last_time TIMESTAMP NOT NULL WITH DEFAULT CURRENT_TIMESTAMP" + 
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
                sql = "CREATE INDEX " + DB_POSTS + "_loc ON " + DB_POSTS + " (loc) ";
                s.execute(sql); 
                sql = "CREATE INDEX " + DB_POSTS + "_x ON " + DB_POSTS + " (xpath) ";
                s.execute(sql); 
                sql="";
                s.close();
                conn.commit();
                what="?";
            }
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
    PreparedStatement pGet = null;
    PreparedStatement pCount = null;

    
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
            
            pAll = prepareStatement("pAll", "SELECT "+DB_POSTS+".poster,"+DB_POSTS+".subj,"+DB_POSTS+".text,"+DB_POSTS+".last_time,"+DB_POSTS+".id,"+DB_POSTS+".forum,"+DB_FORA+".loc"+" FROM " + DB_POSTS + ","+DB_FORA+" WHERE ("+DB_POSTS+".forum = "+DB_FORA+".id) ORDER BY "+DB_POSTS+".last_time DESC");
        }
    } 
    
       
    // "link" UI
    void showForumLink(WebContext ctx, DataPod pod, DataPod.Pea p, int xpath) {
        //if(ctx.session.user == null) {     
        //    return; // no user?
        //}
        String title;
/*        if(!ctx.session.user.interestedIn(forum)) {
            title = " (not on your interest list)";
        }*/
        title = "Zoom..." /*+ title*/;
        ctx.println("<a class='forumlink' href='"+ctx.base()+"?_="+ctx.locale.toString()+"&"+F_FORUM+"="+pod.intgroup+"&"+F_XPATH+"="+xpath+"' title='"+title+"'>"
            +ctx.iconThing("zoom",title) + "</a>");
    }

    static String forumUrl(WebContext ctx, String forum) {
        return (ctx.base()+"?"+F_FORUM+"="+forum);
    }

}
