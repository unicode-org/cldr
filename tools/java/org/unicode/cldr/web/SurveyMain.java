/*
 ******************************************************************************
 * Copyright (C) 2004-2005, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package org.unicode.cldr.web;

import java.io.*;
import java.util.*;

// DOM imports
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.lang.UCharacter;

import com.ibm.icu.dev.test.util.BagFormatter;

import org.unicode.cldr.util.*;
import org.unicode.cldr.icu.*;

import com.fastcgi.FCGIInterface;
import com.fastcgi.FCGIGlobalDefs;
import com.ibm.icu.lang.UCharacter;
//import org.html.*;
//import org.html.utility.*;
//import org.html.table.*;

class SurveyMain {
    public static final String SUBVETTING = "//v"; // vetting context
    public static final String SUBNEW = "//n"; // new context
    public static final String NOCHANGE = "nochange";
    public static final String CURRENT = "current";
    public static final String PROPOSED = "proposed";
    public static final String NEW = "new";
    public static final String DRAFT = "draft";
    public static final String UNKNOWNCHANGE = "Click to suggest replacement";
    public static final String vap = System.getProperty("CLDR_VAP");
    public static final String vetdata = System.getProperty("CLDR_VET_DATA");
    public static final String vetweb = System.getProperty("CLDR_VET_WEB");
    public static final String LOGFILE = "cldr.log";
    
    
    static final String PREF_SHOWCODES = "p_codes";
    static final String PREF_SORTALPHA = "p_sorta";
    static String fileBase = System.getProperty("CLDR_COMMON") + "/main"; // not static - may change lager
    
    // types of data
    static final String LOCALEDISPLAYNAMES = "//ldml/localeDisplayNames/";

    // All of the data items under LOCALEDISPLAYNAMES
    static final String LOCALEDISPLAYNAMES_ITEMS[] = { 
        LDMLConstants.LANGUAGES, LDMLConstants.SCRIPTS, LDMLConstants.TERRITORIES,
        LDMLConstants.VARIANTS, LDMLConstants.KEYS, LDMLConstants.TYPES
    };
    
    public static String xMAIN = "main";

    public UserRegistry reg = new UserRegistry(vetdata);
    
    private int n = 0;
    synchronized int getN() {
        return n++;
    }
    
    private SurveyMain() {}
    
    
    /**
     * output MIME header, build context, and run code..
     */
    private void runSurvey(PrintStream out) throws IOException
    {
        WebContext ctx = new WebContext(out);
        ctx.println("Content-type: text/html; charset=\"utf-8\"\n\n");
        
        if(ctx.field("vap").equals(vap)) {  // if we have a Vetting Administration Password, special case
            doVap(ctx);
        } else {
            doSession(ctx); // Session-based Survey main
        }

        ctx.close();
    }
 
    private static void dumpIt(WebContext ctx, Node root, int level)
    {
        ctx.println("<br>"); // <li>
        ctx.println(root.getNodeName());
        
        NamedNodeMap attr = root.getAttributes();
        if((attr!=null) && attr.getLength()>0){ //TODO: make this a fcn
                                                  // add an element for each attribute different for each attribute
            for(int i=0; i<attr.getLength(); i++){
                Node item = attr.item(i);
                String attrName =item.getNodeName();
                String attrValue = item.getNodeValue();
                ctx.println(attrName + "=\u201c" + attrValue + "\u201d ");
            }
        }
        String value = null;
        Node firstChild = root.getFirstChild();
        if(firstChild != null) {
            value = firstChild.getNodeValue();
        }
        if((value!=null)&&(value.length()>0)) {
            ctx.println("<tt>" + value + "</tt><br/>");
        }
        ctx.println("<br>\n"); // <ul>
        for(Node node=root.getFirstChild();node!=null;node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
//            dumpIt(out, node, level+1);
        }
        ctx.println("<hr/>"); // </ul>
        ctx.println("<br/>"); // </li>
    }
    
    /**
     * print the header of the thing
     */
    public void printHeader(WebContext ctx, String title)
    {
        ctx.println("<html>");
        ctx.println("<head>");
        ctx.println("<title>CLDR Vetting | ");
        if(ctx.locale != null) {
            ctx.print(ctx.locale.getDisplayName() + " | ");
        }
        ctx.println(title + "</title>");
        ctx.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">");
        ctx.println("</head>");
        ctx.println("<body>");
        
        
        // STYLE - move to fyle
        ctx.println("<style type=text/css>\n" +
                    "<!-- \n" + 
                    ".missing { background-color: #FF0000; } \n" +
                    ".fallback { background-color: #FFDDDD; } \n" +
                    ".draft { background-color: #88FFAA; } \n" +
                    ".proposed { background-color: #88DDAA; } \n" +
                    ".sep { height: 5px; background-color: #44778C; } \n" +
                    ".name { background-color: #EEEEFF; } \n" +
                    ".inputbox { width: 100%; height: 100% } \n" +
                    ".list { border-collapse: collapse } \n" +
                    ".list.td .list.th { padding-top: 3px; padding-bottom: 4px; } \n" + 
                    "-->\n" +
                    "</style>\n");
                    
        /*
          u_fprintf(lx->OUT, "%s",  "\r\n<style type=text/css>\r\n"
            "<!--\r\n"
            ".box0 { border: 1px inset gray; margin: 1px }\r\n"
            ".box1 { border: 1px inset gray; margin: 1px; background-color: #CCEECC }\r\n"
            ".wide        { width: 100% }\r\n"
            ".high        { height: 100% }\r\n"
            ".fill        { width: 100%; height: 100% }\r\n"
            ".box0        { background-color: white; border: 1px inset gray; margin: 1px }\r\n"
            ".box1        { background-color: #CCEECC; border: 1px inset gray; margin: 1px }\r\n");
    u_fprintf(lx->OUT, "%s",    
            "#main        { border-spacing: 0; border-collapse: collapse; border: 1px solid black }\r\n"
            "#main tr th, #main tr td       { border-spacing: 0; border-collapse: collapse; font-family: \r\n"
            "               'Lucida Sans Unicode', 'Arial Unicode MS', Arial, sans-serif; \r\n"
            "               color: black; vertical-align: top; border: 1px solid black; \r\n"
            "               padding: 5px }\r\n");
        u_fprintf(lx->OUT, "%s",
            ".noborder    { border: 1px none white }\r\n"
            ".widenoborder { width: 100%; border: 1px none white }\r\n"
            ".icustuff    { background-color: #AAEEAA; border: 1px none white }\r\n"
            ".icugray     { background-color: #afa8af; height: 2px; border: 1px none white }\r\n"
            ".icublack    { background-color: #000000; height: 2px; border: 1px none white }\r\n"
            "tt.count { font-size: 80%; color: #0000FF }\r\n"
            "tt.key { font-size: 70%; color: #666666 }\r\n"
            "-->\r\n</style>\r\n");
  
        */
    }

    public void printFooter(WebContext ctx)
    {
        ctx.println("</body>");
        ctx.println("</html>");
    }
    
    /**
     * process the '_' parameter, if present, and set the locale.
     */
    public void setLocale(WebContext ctx)
    {
        String locale = ctx.field("_");
        if(locale != null) {  // knock out some bad cases
            if((locale.indexOf('.') != -1) ||
               (locale.indexOf('/') != -1)) {
               locale = null;
            }
        }
        if(locale != null && (locale.length()>0)) {
            ctx.setLocale(new ULocale(locale));
        }
    }
    
    /**
     * set the session.
     */
     
    String setSession(WebContext ctx) {
        String message = null;
        // get the context
        CookieSession mySession = null;
        String myNum = ctx.field("s");
        
        // get the uid
        String uid = ctx.field("uid");
        String email = ctx.field("email");
        UserRegistry.User user;
        user = reg.get(uid,email);
        
        if(user != null) {
            mySession = CookieSession.retrieveUser(user.id);
            if(mySession != null) {
                message = "Reconnecting your session: " + myNum;
            }
        }
        if((mySession == null) && (myNum != null) && (myNum.length()>0)) {
            mySession = CookieSession.retrieve(myNum);
            if(mySession == null) {
                message = "ignoring expired session: " + myNum;
            }
        }
        if(mySession == null) {
            mySession = new CookieSession(user==null);
        }
        ctx.session = mySession;
        ctx.addQuery("s", mySession.id);
        if(user != null) {
            ctx.session.setUser(user); // this will replace any existing session by this user.
        }
        
        return message;
    }

    public void doSession(WebContext ctx)
    {
        // which 
        String which = ctx.field("x");

        setLocale(ctx);
        
        String sessionMessage = setSession(ctx);
        
        String title = " - " + which;
        printHeader(ctx, title);
        
        
        // Not doing vetting admin --------
        
        WebContext baseContext = new WebContext(ctx);
        if(ctx.field("submit").length()<=0) {
            // unless we are submitting - process any pending form data.
            processLocale(ctx, which);
        }
        
        // print 'shopping cart'
        {
            if(ctx.session.user != null) {
                ctx.println("<b>Welcome " + ctx.session.user.real + "!</b> <a href=\"" + ctx.base() + "\">[Sign Out]</a><br/>");
            }
            Hashtable lh = ctx.session.getLocales();
            Enumeration e = lh.keys();
            if(e.hasMoreElements()) { 
                ctx.println("<B>Changed locales: </B> ");
                for(;e.hasMoreElements();) {
                    String k = e.nextElement().toString();
                    ctx.println("<a href=\"" + baseContext.url() + "&_=" + k + "\">" + 
                            new ULocale(k).getDisplayName() + "</a> ");
                }
                if(ctx.session.user != null) {
                    ctx.println("<a href=\"" + baseContext.url() + "&submit=preview\"><b>Submit Data</b></a>");
                }
            }
            ctx.println("<br/>");
        }

        if(ctx.field("submit").length()>0) {
            doSubmit(ctx);
        } else {
            doLocale(ctx, baseContext, which);
        }
    }
    
    
    /**
     * TreeMap of all locales. 
     *
     * localeListMap =  TreeMap
     *     [  (String  langScriptDisplayName)  ,    (String localecode) ]  
     *  subLocales = Hashtable
     *       [ localecode, TreeMap ]
     *         -->   TreeMap [ langScriptDisplayName,   String localeCode ]
     *  example
     *  
     *   localeListMap
     *     English  -> en
     *     Serbian  -> sr
     *     Serbian (Cyrillic) -> sr_Cyrl
     *    sublocales
     *       en ->  
     *           [  "English (US)" -> en_US ],   [ "English (Australia)" -> en_AU ] ...
     *      sr ->
     *           "Serbian (Yugoslavia)" -> sr_YU
     */
     
    TreeMap localeListMap = null;
    Hashtable subLocales = null;
   
    private void addLocaleToListMap(String localeName)
    {
        ULocale u = new ULocale(localeName);
            
        String l = u.getLanguage();
        if((l!=null)&&(l.length()==0)) {
            l = null;
        }
        String s = u.getScript();
        if((s!=null)&&(s.length()==0)) {
            s = null;
        }
        String t = u.getCountry();
        if((t!=null)&&(t.length()==0)) {
            t = null;
        }
        String v = u.getVariant();
        if((v!=null)&&(v.length()==0)) {
            v = null;
        }
        
        if(l==null) {
            return; // no language?? 
        }
        
        String ls = ((s==null)?l:(l+"_"+s)); // language and script
        
        ULocale lsl = new ULocale(ls);
        localeListMap.put(lsl.getDisplayName(),ls);
        
        TreeMap lm = (TreeMap)subLocales.get(ls);
        if(lm == null) {
            lm = new TreeMap();
            subLocales.put(ls, lm); 
        }
        
        if(t != null) {
            if(v == null) {
                lm.put(u.getDisplayCountry(), localeName);
            } else {
                lm.put(u.getDisplayCountry() + " (" + u.getDisplayVariant() + ")", localeName);
            }
        }
    }
      
    private synchronized TreeMap getLocaleListMap()
    {
        if(localeListMap == null) {
            localeListMap = new TreeMap();
            subLocales = new Hashtable();
            File inFiles[] = getInFiles();
            int nrInFiles = inFiles.length;
            
            for(int i=0;i<nrInFiles;i++) {
                String localeName = inFiles[i].getName();
                int dot = localeName.indexOf('.');
                if(dot !=  -1) {
                    localeName = localeName.substring(0,dot);
                    if(i != 0) {
                        addLocaleToListMap(localeName);
                    }
                }
            }
        }
        return localeListMap;
    }

    void printLocaleLink(WebContext ctx, String localeName, String n) {
        if(n == null) {
            n = new ULocale(localeName).getDisplayName() ;
        }
        ctx.print("<a title='" + localeName +"' href=\"" + ctx.url() 
            + "&" + "_=" + localeName + "\">" +
            n + "</a>");
    }
    
    void doLocaleList(WebContext ctx, WebContext baseContext) {
        boolean showCodes = ctx.prefBool(PREF_SHOWCODES);
        
        ctx.printHelpLink(""); // base help
        ctx.println("<h1>Locales</h1>");
        TreeMap lm = getLocaleListMap();
        {
            WebContext nuCtx = new WebContext(ctx);
            nuCtx.addQuery(PREF_SHOWCODES, !showCodes);
            nuCtx.println("<a href='" + nuCtx.url() + "'>" + ((!showCodes)?"Show":"Hide") + " locale codes</a><br/>");
        }
        ctx.println("<table border=1 class='list'>");
        int n=0;
        for(Iterator li = lm.keySet().iterator();li.hasNext();) {
            n++;
            String ln = (String)li.next();
            String l = (String)lm.get(ln);
            ctx.print("<tr " + (((n%2)==0)?"bgcolor='#DDDDDD'":"") + ">");
            ctx.print(" <td>");
            printLocaleLink(baseContext, l, ln);
            ctx.println(" </td>");
            if(showCodes) {
                ctx.print(" <td>");
                ctx.println("<tt>" + l + "</tt>");
                ctx.println(" </td>");
            }
            
            TreeMap sm = (TreeMap)subLocales.get(l);
            
            ctx.println("<td>");
            int j = 0;
            for(Iterator si = sm.keySet().iterator();si.hasNext();) {
                if(j>0) { 
                    ctx.println(", ");
                }
                String sn = (String)si.next();
                String s = (String)sm.get(sn);
                if(s.length()>0) {
                    printLocaleLink(baseContext, s, sn);
                    if(showCodes) {
                        ctx.println("&nbsp;-&nbsp;<tt>" + s + "</tt>");
                    }
                }
                j++;
            }
            ctx.println("</td");
            ctx.println("</tr>");
        }
        ctx.println("</table> ");
    }
    
    /**  old nested-list code
    
    {
        ctx.printHelpLink(""); // base help
        ctx.println("<h1>Locales</h1>");
        TreeMap lm = getLocaleListMap();
        ctx.println("<ul>");
        for(Iterator li = lm.keySet().iterator();li.hasNext();) {
            String ln = (String)li.next();
            String l = (String)lm.get(ln);
            ctx.print("<li> <b>");
            printLocaleLink(baseContext, l, ln);
            ctx.println("</b><br/>");
            
            TreeMap sm = (TreeMap)subLocales.get(l);
            
            ctx.println("<ul>");
            for(Iterator si = sm.keySet().iterator();si.hasNext();) {
                String sn = (String)si.next();
                String s = (String)sm.get(sn);
                String sl;
                ctx.print("<li>");
                if(s.length()>0) {
                    sl = l + "_" + s;
                    ctx.println("<b>");
                    printLocaleLink(baseContext, s, sn);
                    ctx.println("</b>");
                }
                ctx.println("</li>");
            }
            ctx.println("</ul><br/></li>");
        }
        ctx.println("</ul><br/> ");
    }
    */
    
    void doLocale(WebContext ctx, WebContext baseContext, String which) {
        String locale = null;
        if(ctx.locale != null) {
            locale = ctx.locale.toString();
        }
        if((locale==null)||(locale.length()<=0)) {
            doLocaleList(ctx, baseContext);            
            ctx.println("<br/>");
        } else {
            if((ctx.doc == null) || (ctx.doc.length < 1)) {
                ctx.println("<i>ERR: No docs fetched.</i>");
            } else if(ctx.doc[0] != null) {                
                showLocale(ctx, which);
            } else {
                ctx.println("<i>err, couldn't fetch " + ctx.locale.toString() + "</i><br/>");
            }
        }
        printFooter(ctx);
    }
    
    protected void printMenu(WebContext ctx, String which, String menu) {
        if(menu.equals(which)) {
            ctx.println("<b>");
        }
        ctx.println("<a href=\"" + ctx.url() +  "&x=" + menu +
            "\">" + menu + "</a>");
        if(menu.equals(which)) {
            ctx.println("</b>");
        }            
    }
    
    public void doVap(WebContext ctx)
    {
        String sponsor = ctx.field("sponsor");
        String requester = ctx.field("requester");
        String email = ctx.field("email");
        String name = ctx.field("name");
        
        printHeader(ctx, "Vetting Administration");
        
        if(requester.indexOf('@')==-1) {
            ctx.println("Please supply a valid requester email. Click Back and try again.");
            printFooter(ctx);
            return;
        }
        
        UserRegistry.User u = reg.get(email);
        if(u != null) {
            ctx.println("User exists!   " + u.real + " <" + u.email + ">  (" + u.sponsor + ") -  ID: " + u.id + "<br/>");
            if(ctx.field("resend").length()>0) {
                notifyUser(ctx, u, requester);
            } else {
                WebContext my = new WebContext(ctx);
                my.addQuery("vap",vap);
                my.addQuery("email",email);
                my.addQuery("requester",requester);
                my.addQuery("resend","y");
                ctx.println("<a href=\"" + my.url() + "\">Resend their password email</a>");
            }
        } else if( (sponsor.length()<=0) ||
            (email.indexOf('@')==-1) ||
            (name.length()<=0) ) {
            ctx.println("One or more of the Sponsor, Email, Name fields aren't filled out.  Please hit Back and try again.");
        } else {
            u = reg.add(email, sponsor, name, requester);
            appendLog("User added: " + name + " <" + email + ">, Sponsor: " + sponsor + " <" + requester + ">" + 
                " (user ID " + u.id + " )" );
            notifyUser(ctx, u, requester);
        }
        printFooter(ctx);
    }
   
    void notifyUser(WebContext ctx, UserRegistry.User u, String requester) {
        String body = requester + " at " + u.sponsor + " has created a CLDR vetting account for you.\n" +
            "To access it, visit: \n" +
            "   http://" + ctx.serverName() + ctx.base() + "?uid=" + u.id + "&email=" + u.email + "\n" +
            "\n" +
            " Please keep this link to yourself. Thanks.\n" +
            " \n";
        ctx.println("<hr/><pre>" + body + "</pre><hr/>");
    
        String from = System.getProperty("CLDR_FROM");
        String smtp = System.getProperty("CLDR_SMTP");
         if(smtp == null) {
            ctx.println("<i>Not sending mail- SMTP disabled.</i><br/>");
            smtp = "NONE";
        } else {
            MailSender.sendMail(u.email, "CLDR Registration for " + u.email,
                body);
            ctx.println("Mail sent to " + u.email + " from " + from + " via " + smtp + "<br/>\n");
        }
        appendLog("Login URL sent to " + u.email + " (#" + u.id + ") from " + from + " via " + smtp);
        /* some debugging. */
    }

    public void doSubmit(WebContext ctx)
    {
        if((ctx.session.user == null) ||
            (ctx.session.user.id == null)) {   
            ctx.println("No Vetting Account found... please see this help link: ");
            ctx.printHelpLink("/NoUser");
            ctx.println("<br/>");
        }
        UserRegistry.User u = ctx.session.user;
        if(u == null) {
            u = reg.getEmptyUser();
        }
        WebContext subContext = new WebContext(ctx);
        subContext.addQuery("submit","post");
        boolean post = (ctx.field("submit").equals("post"));
        if(post == false) {
            ctx.println("<B>Please read the following carefully. If there are any errors, hit Back and correct them.  " + 
                "The button to finalize the submission is at the very bottom of this page.</b><br/>");
        } else {
            ctx.println("Posting the following changes:<br/>");
        }
        ctx.println("<hr/>");
        ctx.println("You:  " + u.real + " &lt;" + u.email + "&gt;<br/>");
        ctx.println("Your sponsor: " + u.sponsor);
        File sessDir = new File(vetweb + "/" + u.email + "/" + ctx.session.id);
        if(post) {
            sessDir.mkdirs();
        }
        String changedList = "";
        Hashtable lh = ctx.session.getLocales();
        Enumeration e = lh.keys();
        String fullBody = "";
        if(e.hasMoreElements()) { 
            for(;e.hasMoreElements();) {
                String k = e.nextElement().toString();
                String displayName = new ULocale(k).getDisplayName();
                ctx.println("<hr/>");
                ctx.println("<H3>" + 
                        displayName+ "</h3>");
                CLDRFile f = createCLDRFile(ctx, k, (Hashtable)lh.get(k));
                
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                f.write(pw);
                String asString = sw.toString();
                fullBody = fullBody + "-------------" + "\n" + k + ".xml - " + displayName + "\n" + 
                    hexXML.transliterate(asString);
                String asHtml = BagFormatter.toHTML.transliterate(asString);
                ctx.println("<pre>" + asHtml + "</pre>");
                File xmlFile = new File(sessDir, k + ".xml");
                if(post) {
                    try {
                        changedList = changedList + " " + k;
                        PrintWriter pw2 = BagFormatter.openUTF8Writer(xmlFile);
                        f.write(pw2);
                        pw2.close();
                        ctx.println("<b>File Written.</b><br/>");
                    } catch(Throwable t) {
                        // TODO: log??
                        ctx.println("<b>Couldn't write the file "+ k + ".xml</b> because: <br/>");
                        ctx.println(t.toString());
                        t.printStackTrace();
                        ctx.println("<p>");
                    }
                }
            }
        }
        ctx.println("<hr/>");
        if(post == false) {
            subContext.println("<form method=POST action='" + subContext.base() + "'>");
            subContext.printUrlAsHiddenFields();
            subContext.println("<input type=submit value='Post Changes and Logout'>");
            subContext.println("</form>");
        } else {        
            String body = "User:  " + u.real + " <" + u.email + "> for  " + u.sponsor + "\n" +
             "Submitted data for: " + changedList + "\n" +
             "Session ID: " + ctx.session.id + "\n";
            String smtp = System.getProperty("CLDR_SMTP");
            if(smtp == null) {
                ctx.println("<i>Not sending mail- SMTP disabled.</i><br/>");
            } else {
                MailSender.sendMail(u.email, "CLDR: Receipt of your data submission ",
                        "Submission from IP: " + WebContext.userIP() + "\n" + body  +
                            "\n The files submitted are attached below: \n" + fullBody );
                MailSender.sendMail(System.getProperty("CLDR_NOTIFY"), "CLDR: from " + u.sponsor + 
                        "/" + u.email + ": " + changedList,
                        "URL: " + System.getProperty("CLDR_VET_WEB_URL") + u.email + "/" + ctx.session.id + "\n" +
                        body);
                ctx.println("Thank you..   An email has been sent to the CLDR Vetting List and to you at " + u.email + ".<br/>");
            }
            appendLog("Data submitted: " + u.real + " <" + u.email + "> Sponsor: " + u.sponsor + ": " +
                changedList + " " + 
                " (user ID " + u.id + ", session " + ctx.session.id + " )" );
            // destroy session
            ctx.println("<form method=GET action='" + ctx.base() + "'>");
            ctx.println("<input type=hidden name=uid value='" + ctx.session.user.id + "'> " +
                        "<input type=hidden name=email value='" + ctx.session.user.email + "'>");
            ctx.println("<input type=submit value='Login Again'>");
            ctx.println("</form>");
            ctx.session.remove();
        }
        printFooter(ctx);
    }
    
    private void appendCodeList(WebContext ctx, CLDRFile file, String xpath, String subtype, Hashtable data) {
        if(data == null) {
            return;
        }
        for(Enumeration e = data.keys();e.hasMoreElements();) {
            String k = e.nextElement().toString();
            if(k.endsWith(SUBVETTING)) { // we ONLY care about SUBVETTING items. (
                String type = k.substring(0,k.length()-SUBVETTING.length());
                String vet = (String)data.get(k);
                // Now, what's happening? 
                NodeSet.NodeSetEntry nse = (NodeSet.NodeSetEntry)data.get(type);
                String newxpath = xpath + subtype + "[@type='" + type + "']";
                if(nse.key != null) {
                    newxpath = newxpath + "[@key='" + nse.key + "']";
                }
                newxpath=newxpath.substring(1); // remove initial /     
                if(vet.equals(DRAFT)) {
                    if((nse.main != null) && LDMLUtilities.isNodeDraft(nse.main)) {
                        file.add(newxpath, newxpath, LDMLUtilities.getNodeValue(nse.main));
                    } else {
                        file.addComment(newxpath, "Can't find draft data! " + type, XPathParts.Comments.POSTBLOCK);
                    }
                } else if(vet.equals(CURRENT)) {
                    if(nse.fallback != null)  {
                        file.add(newxpath, newxpath, LDMLUtilities.getNodeValue(nse.fallback));
                    } else if(nse.main != null) {
                        file.add(newxpath, newxpath, LDMLUtilities.getNodeValue(nse.main));
                    } else {
                        file.add(newxpath, newxpath, type);
                    }
                } else if(vet.equals(PROPOSED)) {
                    file.add(newxpath, newxpath, LDMLUtilities.getNodeValue(nse.proposed));
                } else if(vet.equals(NEW)) {
                    String newString = (String)data.get(type + SUBNEW);
                    if(newString == null) {
                        newString = "";
                    }
                    if(nse.main != null) { // If there is already an existing main (which might be draft)
                       newxpath = newxpath + "[@alt='proposed']";
                    }
                    newxpath = newxpath + "[@draft='true']"; // always draft
/*SRL*/                 ctx.println("<tt>CLDRFile.add(<b>" + newxpath + "</b>, \"\", blah);</tt><br/>");
                    file.add(newxpath, newxpath, newString);
                    if(newString.length() ==0) {
                        file.addComment(newxpath, "Item marked as wrong:  " + type, XPathParts.Comments.POSTBLOCK);
/*SRL*/                 ctx.println("<tt>CLDRFile.addComment(<b>" + newxpath + "</b>, blah, POSTBLOCK);</tt><br/>");
                    }
                } else {
                    // ignored:  current, etc.
                }
            }
        }
    }
    
    public static final String typeToSubtype(String type)
    {
        String subtype = type;
        if(type.equals(LDMLConstants.LANGUAGES)) {
            subtype = LDMLConstants.LANGUAGE;
        } else if(type.equals(LDMLConstants.SCRIPTS)) {
            subtype = LDMLConstants.SCRIPT;
        } else if(type.equals(LDMLConstants.TERRITORIES)) {
            subtype = LDMLConstants.TERRITORY;
        } else if(type.equals(LDMLConstants.VARIANTS)) {
            subtype = LDMLConstants.VARIANT;
        } else if(type.equals(LDMLConstants.KEYS)) {
            subtype = LDMLConstants.KEY;
        } else if(type.equals(LDMLConstants.TYPES)) {
            subtype = LDMLConstants.TYPE;
        } /* else if(subtype.endsWith("s")) {
            subtype = subtype.substring(0,subtype.length()-1);
        }
        */
        return subtype;
    }
    
    private void appendCodes(WebContext ctx, CLDRFile file, String xpath, String type, Hashtable data) {
        String fullXpath = xpath + type;
        Hashtable items = (Hashtable)data.get(fullXpath);
        String subtype = typeToSubtype(type);
        appendCodeList(ctx, file, xpath, subtype, items);
    }
    
    private CLDRFile createCLDRFile(WebContext ctx, String locale, Hashtable data) {
        CLDRFile file = CLDRFile.make(locale);
        String cvsVer = (String)docVersions.get(locale);
        if(cvsVer == null) {
            cvsVer = "(unknown)";
        }
        file.setInitialComment(
                                "Date: " + new Date().toString() + "\n" +
                                "From: " + ctx.session.user.real + "\n" +
                                "Email: " + ctx.session.user.email + "\n" +
                                "Sponsor: " + ctx.session.user.sponsor + "\n" +
                            /*    "IP: " + WebContext.userIP() + "\n" + */
                                "Locale: " + locale +"\n" +
                                "CVS Version: " + cvsVer + "\n"
                                );
                                
        if(data == null) {
            file.appendFinalComment("No data.");
            return file;
        }

        for(int n =0 ; n < LOCALEDISPLAYNAMES_ITEMS.length; n++) {        
            appendCodes(ctx, file, LOCALEDISPLAYNAMES, LOCALEDISPLAYNAMES_ITEMS[n], data);
        }
        return file;
    }
    
    /**
     * process any form items needed.
     * @param ctx context
     * @param which value of 'x' parameter.
     */
    public void processLocale(WebContext ctx, String which)
    {
        for(int n =0 ; n < LOCALEDISPLAYNAMES_ITEMS.length; n++) {
            if(which.equals(LOCALEDISPLAYNAMES_ITEMS[n])) {
                processLocaleCodeList(ctx, LOCALEDISPLAYNAMES +LOCALEDISPLAYNAMES_ITEMS[n], null);
            }
        }
    }

    /**
     * show the actual locale data..
     * @param ctx context
     * @param which value of 'x' parameter.
     */
    public void showLocale(WebContext ctx, String which)
    {
        int i;
        ctx.println("<b><a href=\"" + ctx.url() + "\">" + "Go to a different Locale" + "</a></b><br/>");
        ctx.println("<br/>");
        ctx.println("<font size=+1><b>" + ctx.locale + "</b></font> " + ctx.locale.getDisplayName() + "<br/>");
        ctx.println("<i>Switch to: </i> " );
        for(i=1;i<ctx.docLocale.length;i++) {
            ctx.println("<a href=\"" + ctx.url() + "&_=" + ctx.docLocale[i] + "\">" + ctx.docLocale[i] + "</a> ");
        }
        ctx.println("<hr/>");
        
        if((which == null) ||
            which.equals("")) {
            which = xMAIN;
        }

        WebContext subCtx = new WebContext(ctx);
        subCtx.addQuery("_",ctx.locale.toString());
        printMenu(subCtx, which, xMAIN);
        subCtx.println("<br/>  Locale Display Names: ");
        for(int n =0 ; n < LOCALEDISPLAYNAMES_ITEMS.length; n++) {        
            printMenu(subCtx, which, LOCALEDISPLAYNAMES_ITEMS[n]);
        }
        
        ctx.println("<br/>");
        subCtx.addQuery("x",which);
        final ULocale inLocale = new ULocale("en_US");
        StandardCodes standardCodes = StandardCodes.make();
        Set defaultSet = standardCodes.getAvailableCodes(typeToSubtype(which));
        if(LDMLConstants.LANGUAGES.equals(which)) {
            NodeSet.NodeSetTexter texter = new NodeSet.NodeSetTexter() { 
                    public String text(NodeSet.NodeSetEntry e) {
                        return new ULocale(e.type).getDisplayLanguage(inLocale);
                    }
            };
            doLocaleCodeList(subCtx, LOCALEDISPLAYNAMES + which, texter,
                standardCodes.getAvailableCodes("language"));
        } else if(LDMLConstants.SCRIPTS.equals(which)) {
            NodeSet.NodeSetTexter texter = new NodeSet.NodeSetTexter() { 
                    public String text(NodeSet.NodeSetEntry e) {
                        return new ULocale("_"+e.type).getDisplayScript(inLocale);
                    }
            };
            doLocaleCodeList(subCtx, LOCALEDISPLAYNAMES + which, texter,
                standardCodes.getAvailableCodes("script"));
        } else if(LDMLConstants.TERRITORIES.equals(which)) {
            NodeSet.NodeSetTexter texter = new NodeSet.NodeSetTexter() { 
                    public String text(NodeSet.NodeSetEntry e) {
                        return new ULocale("_"+e.type).getDisplayCountry(inLocale);
                    }
            };
            doLocaleCodeList(subCtx,LOCALEDISPLAYNAMES + which, texter,
                defaultSet);
        } else if(LDMLConstants.VARIANTS.equals(which)) {
             NodeSet.NodeSetTexter texter = new NodeSet.NodeSetTexter() { 
                    public String text(NodeSet.NodeSetEntry e) {
                        return new ULocale("__"+e.type).getDisplayVariant(inLocale);
                    }
            };
           doLocaleCodeList(subCtx, LOCALEDISPLAYNAMES + which, texter,
                defaultSet);  // no default variant list
        } else if(LDMLConstants.KEYS.equals(which)) {
             NodeSet.NodeSetTexter texter = new StandardCodeTexter(which);
             doLocaleCodeList(subCtx, LOCALEDISPLAYNAMES + which, texter,
                defaultSet);  // no default  list
        } else if(LDMLConstants.TYPES.equals(which)) {
             NodeSet.NodeSetTexter texter = new StandardCodeTexter(which);
             doLocaleCodeList(subCtx, LOCALEDISPLAYNAMES + which, texter,
                defaultSet);  // no default  list
        } else {
            doMain(subCtx);
        }
    }
 
    public void doMain(WebContext ctx) {
        String ver = (String)docVersions.get(ctx.locale.toString());
        ctx.println("<hr/><p><p>");
        ctx.println("<h3>Basic information about the Locale</h3>");
        
        if(ver != null) {
            ctx.println( LDMLUtilities.getCVSLink(ctx.locale.toString(), ver) + "CVS version #" + ver + "</a><br/>");
        }
        
        // print some basic stuff
        ver = getAttributeValue(ctx.doc[0], "//ldml/identity/version", "number");
        if(ver != null) {
            ctx.println("XML Version number: " + ver + "<br/>");
        }
        ver = getAttributeValue(ctx.doc[0], "//ldml/identity/generation", "date");
        if(ver != null) {
            ctx.println("Generation date: " + ver + "<br/>");
        }
    }
    
    public static String getAttributeValue(Document doc, String xpath, String attribute) {
        if(doc != null) {
            Node n = LDMLUtilities.getNode(doc, xpath);
            if(n != null) {
                return LDMLUtilities.getAttributeValue(n, attribute);
            }
        }
        return null;
    }

    /**
     * Parse query fields, update hashes, etc.
     * later, we'll see if we can generalize this function.
     */
    public void processLocaleCodeList(WebContext ctx, String xpath, NodeSet.NodeSetTexter tx) {
            NodeSet mySet = NodeSet.loadFromPath(ctx, xpath, null);
            Hashtable changes = (Hashtable)ctx.getByLocale(xpath);
            // prepare a new hashtable
            if(changes==null) { 
                changes = new Hashtable(); 
            }
        // process items..
        for(Iterator e = mySet.iterator();e.hasNext();) {
            NodeSet.NodeSetEntry f = (NodeSet.NodeSetEntry)e.next();
            
            String type = f.type;
            String main = null;
            String mainFallback = null;
            int mainDraft = 0; // count
            String prop = null;
            if(f.main != null) {
                main = LDMLUtilities.getNodeValue(f.main);
                if(LDMLUtilities.isNodeDraft(f.main)) {
                    mainDraft = 1; // for now: one draft
                }
            }
            // are we showing a fallback locale in the 'current' slot?
            if( (f.fallback != null) && // if we have a fallback
                ( (mainDraft > 0) || (f.main == null) ) ) {
                mainFallback = f.fallbackLocale;
            }
            if(f.proposed != null) {
                prop = LDMLUtilities.getNodeValue(f.proposed);
            }

            // Analyze user input.
            String checked = null;  // What option has the user checked?
            String newString = null;  // What option has the user checked?
            if(changes != null) {
                checked = (String)changes.get(type + SUBVETTING); // fetch VETTING data
                newString = (String)changes.get(type + SUBNEW); // fetch NEW data
            }
            if(checked == null) {
                checked = NOCHANGE;
            }
            
            String formChecked = ctx.field(xpath + "/" + type);
            
            if((formChecked != null) && (formChecked.length()>0)) {   
                if(!checked.equals(formChecked)) {
                    checked = formChecked;
                    if(checked.equals(NOCHANGE)) {
                        changes.remove(type + SUBVETTING); // remove 'current' 
                    } else {
                        changes.put(type + SUBVETTING, checked); // set
                        changes.put(type, f);
                    }
                }

                // Don't consider the 'new text' form, unless we know the 'changes...' checkbox is present.
                // this is because we can't distinguish between an empty and a missing field.
                String formNew = ctx.field(xpath + "/" + type + SUBNEW );
                if((formNew.length()>0) && !formNew.equals(UNKNOWNCHANGE)) {
                    changes.put(type + SUBNEW, formNew);
                    changes.put(type, f); // get the NodeSet in for later use
                    newString = formNew;
                } else if((newString !=null) && (newString.length()>0)) {
                    changes.remove(type + SUBNEW);
                    newString = null;
                }
            }            
        }
        if((changes!=null) && (!changes.isEmpty())) { 
            ctx.putByLocale(xpath,changes); 
        }
    }
    /**
     * @param ctx the web context
     * @param xpath xpath to the root of the structure
     * @param tx the texter to use for presentation of the items
     * @param fullSet the set of tags denoting the expected full-set, or null if none.
     */
    public void doLocaleCodeList(WebContext ctx, String xpath, NodeSet.NodeSetTexter tx, Set fullSet) {
        final int CODES_PER_PAGE = 51;
        int count = 0;
        int dispCount = 0;
        int total = 0;
        int skip = 0;
        NodeSet mySet = NodeSet.loadFromPath(ctx, xpath, fullSet);
        total = mySet.count();
        boolean sortAlpha = ctx.prefBool(PREF_SORTALPHA);
        Map sortedMap = mySet.getSorted(sortAlpha?tx:new DraftFirstTexter(tx));
        Hashtable changes = (Hashtable)ctx.getByLocale(xpath);
        
        if(tx == null) {
            tx = new NullTexter();
        }
        
        // prepare a new hashtable
        if(changes==null) { 
            changes = new Hashtable();  // ?? TODO: do we need to create a hashtable here?
        }

        // NAVIGATION .. calculate skips.. 
        String str = ctx.field("skip");
        if((str!=null)&&(str.length()>0)) {
            skip = new Integer(str).intValue();
        }
        if(skip<=0) {
            skip = 0;
        } else {
            int prevSkip = skip - CODES_PER_PAGE;
            if(prevSkip<0) {
                prevSkip = 0;
            }
                ctx.println("<a href=\"" + ctx.url() + 
                        "&skip=" + new Integer(prevSkip) + "\">" +
                        "Back..." +
                        "</a><br/> ");
            if(skip>=total) {
                skip = 0;
            }
        }

        // calculat nextSkip
        int from = skip+1;
        int nextSkip = skip + CODES_PER_PAGE; 
        if(nextSkip >= total) {
            nextSkip = -1;
        } else {
            ctx.println("<a href=\"" + ctx.url() + 
                    "&skip=" + new Integer(nextSkip) + "\">" +
                    "More..." +
                    "</a> ");
        }
        int to = from + CODES_PER_PAGE-1;
        if(to >= total) {
            to = total;
        }
        
        // Print navigation
        ctx.println("Displaying items " + from + " to " + to + " of " + total + "<br/>");        
        {
            WebContext nuCtx = new WebContext(ctx);
            nuCtx.addQuery(PREF_SORTALPHA, !sortAlpha);
            nuCtx.println("Sorted <a href='" + nuCtx.url() + "'>" + ((sortAlpha)?"Alphabetically":"Draft-first") +
                    "</a><br/>");
        }
        
        
        // Form: 
        ctx.println("<form method=POST action='" + ctx.base() + "'>");
        ctx.printUrlAsHiddenFields();
        ctx.println("<table class='list' border=1>");
        if(ctx.session.user != null) {
            ctx.println("<input type=submit value=Save>");
        }
        ctx.println("<tr>");
        ctx.println(" <th class='heading' bgcolor='#DDDDDD' colspan=2>Name<br/><tt>Code</tt></th>");
        ctx.println(" <th class='heading' bgcolor='#DDDDDD' colspan=1>Best<br/>");
        ctx.printHelpLink("/Best");
        ctx.println("</th>");
        ctx.println(" <th class='heading' bgcolor='#DDDDDD' colspan=1>Contents</th>");
        ctx.println("</tr>");
        
        // process items..
        for(Iterator e = sortedMap.values().iterator();e.hasNext();) {
            NodeSet.NodeSetEntry f = (NodeSet.NodeSetEntry)e.next();
            count++;
            if(skip > 0) {
                --skip;
                continue;
            }
            dispCount++;
            
            String type = f.type;
            String main = null;
            String mainFallback = null;
            int mainDraft = 0; // count
            String prop = null;
            if(f.main != null) {
                main = LDMLUtilities.getNodeValue(f.main);
                if(LDMLUtilities.isNodeDraft(f.main)) {
                    mainDraft = 1; // for now: one draft
                }
            }
            int nRows = 1;         // the 'new' line (user edited)
            nRows ++; // 'main'
            // are we showing a fallback locale in the 'current' slot?
            if( (f.fallback != null) && // if we have a fallback
                ( (mainDraft > 0) || (f.main == null) ) ) {
                mainFallback = f.fallbackLocale;
                if(mainDraft > 0) {
                    nRows ++; // fallback
                }
            }else if (mainDraft > 0) {
                nRows ++; // for the Draft entry
            }
            if(f.proposed != null) {
                nRows ++;
                prop = LDMLUtilities.getNodeValue(f.proposed);
            }

            // Analyze user input.
            String checked = null;  // What option has the user checked?
            String newString = null;  // What option has the user checked?
            if(changes != null) {
                checked = (String)changes.get(type + SUBVETTING); // fetch VETTING data
                newString = (String)changes.get(type + SUBNEW); // fetch NEW data
            }
            if(checked == null) {
                checked = NOCHANGE;
            }
            
            ctx.println("<tr>");
            // 1. name/code
            ctx.println("<th valign=top align=left class='name' colspan=2 rowspan=" + (nRows-1) + ">" + tx.text(f) + "");
            if(f.key != null) {
                ctx.println("<br/><tt>(" + f.key + ")</tt>");
            }
            ctx.println("</th>");
            
            // Now there are a pair of columns for each of the following. 
            // 2. fallback
            if(mainFallback != null) {
                ctx.println("<td align=right class='fallback'>");
                ctx.println("from " + 
                        "<a href=\"" + ctx.base() + "?x=" + ctx.field("x") + "&_=" + mainFallback + "\">" + 
                        new ULocale(mainFallback).getDisplayName(new ULocale("en_US")) +                        
                        "</a>");
                writeRadio(ctx,xpath,type,CURRENT,checked);
                ctx.println("</td>");
                ctx.println("<td class='fallback'>");
                ctx.println(LDMLUtilities.getNodeValue(f.fallback));
                ctx.println("</td>");
            } else if((main!=null)&&(mainDraft==0)) {
                ctx.println("<td align=right class='current'>");
                ctx.println("current");
                writeRadio(ctx,xpath,type,CURRENT,checked);
                ctx.println("</td>");
                ctx.println("<td class='current'>");
                ctx.println(main);
                ctx.println("</td>");
            } else /*  if(main == null) */ {
                ctx.println("<td align=right class='missing'>");
                ctx.println("<i>current</i>");
                writeRadio(ctx,xpath,type,CURRENT,checked);
                ctx.println("</td>");
                ctx.println("<td title=\"Data missing - raw code shown.\" class='missing'><tt>");
                ctx.println(type); // in typewriter <tt> to show that it is a code.
                ctx.println("</tt></td>");
            }
            ctx.println("</tr>");
            
            ctx.println("<tr>");

            // Draft item
            if(mainDraft > 0) {
                ctx.println("<td align=right class='draft'>");
                ctx.println("draft");
                writeRadio(ctx,xpath,type,DRAFT,checked);
                ctx.println("</td>");
                ctx.println("<td class='draft'>");
                ctx.println(main);
                ctx.println("</td>");
                ctx.println("</tr>");
                ctx.println("<tr>");
            }

            // Proposed item
            if(prop != null) {
                ctx.println("<td align=right class='proposed'>");
                ctx.println("proposed");
                writeRadio(ctx,xpath,type,PROPOSED,checked);
                ctx.println("</td>");
                ctx.println("<td class='proposed'>");
                ctx.println(prop);
                ctx.println("</td>");
                ctx.println("</tr>");
                ctx.println("<tr>");
            }
            
            //'nochange' and type
            ctx.println("<th class='type'>");
            ctx.println("<tt>" + type + "</tt>");
            ctx.println("</th");
            ctx.println("<td class='nochange'>");
            writeRadio(ctx,xpath,type,NOCHANGE,checked);
            ctx.println("<font size=-2>No change</font>");
            ctx.println("</td>");

            // edit text
            ctx.println("<td align=right class='new'>");
            ctx.println("change");
            writeRadio(ctx,xpath,type,NEW,checked);
            ctx.println("</td>");
            ctx.println("<td class='new'>");
            String change = "";
            if(changes != null) {
           //     change = (String)changes.get(type + "//n");
            }
            ctx.print("<input size=50 class='inputbox' ");
            ctx.print("onblur=\"if (value == '') {value = '" + UNKNOWNCHANGE + "'}\" onfocus=\"if (value == '" + 
                UNKNOWNCHANGE + "') {value =''}\" ");
            ctx.print("value=\"" + 
                  (  (newString!=null) ? newString : UNKNOWNCHANGE )
                    + "\" name=\"" + xpath +"/" + type + SUBNEW + "\">");
            ctx.println("</td>");
            ctx.println("</tr>");
            

            ctx.println("<tr class='sep'><td class='sep' style='height: 3px; overflow: hidden;' colspan=4 bgcolor=\"#CCCCDD\"></td></tr>");

            // -----
            
            if(dispCount >= CODES_PER_PAGE) {
                break;
            }
        }
        ctx.println("</table>");
        if(ctx.session.user != null) {
            ctx.println("<input type=submit value=Save>");
        }
        ctx.println("</form>");
        if(nextSkip >= 0) {
                ctx.println("<a href=\"" + ctx.url() + 
                        "&skip=" + new Integer(nextSkip) + "\">" +
                        "More..." +
                        "</a> ");
        }
    }
    
    static String cldrLoad = System.getProperty("CLDR_LOAD_ALL");

    
    public static void main (String args[]) {
        int status = 0;
        appendLog("SurveyTool starting up.");
        SurveyMain m = new SurveyMain();
        if(!m.reg.read()) {
            appendLog("Couldn't load user registry - exiting");
            System.err.println("Couldn't load user registry - exiting.");
            System.exit(1);
        }
        if((cldrLoad != null) && cldrLoad.equals("y")) {
            m.loadAll();
        }
        appendLog("SurveyTool ready for connections.");
        while(new FCGIInterface().FCGIaccept()>= 0) {
            System.setErr(System.out);
            try {
             m.runSurvey(System.out);
            } catch(Throwable t) {
                System.out.println("Content-type: text/html\n\n\n<pre>");
                System.out.flush();
                System.out.println("<B>err</B>: <pre>" + t.toString() + "</pre>");
                t.printStackTrace();
            }
        }
    }
    
    protected void startCell(WebContext ctx, String background) {
        ctx.println("<td bgcolor=\"" + background + "\">");
    }
    
    protected void endCell(WebContext ctx) {
        ctx.println("</td>");
    }
    
    protected void doCell(WebContext ctx, String type, String value) {
        startCell(ctx,"#FFFFFF");
        ctx.println(value);
        endCell(ctx);
    }
    
    protected void doDraftCell(WebContext ctx, String type, String value) {
        startCell(ctx,"#DDDDDD");
        ctx.println("<i>Draft</i><br/>");
        ctx.println(value);
        endCell(ctx);
    }
    
    protected void doPropCell(WebContext ctx, String type, String value) {
        startCell(ctx,"#DDFFDD");
        ctx.println("<i>Proposed:</i><br/>");
        ctx.println(value);
        endCell(ctx);
    }

    // utils
    private Node getVettedNode(Node context, String resToFetch){
        NodeList list = LDMLUtilities.getChildNodes(context, resToFetch);
        Node node =null;
        if(list!=null){
            for(int i =0; i<list.getLength(); i++){
                node = list.item(i);
                if(LDMLUtilities.isNodeDraft(node)){
                    continue;
                }
/*
                if(isAlternate(node)){
                    continue;
                }
*/
                return node;
            }
        }
        return null;
    }
    
    static protected File[] getInFiles() {
        // 1. get the list of input XML files
        FileFilter myFilter = new FileFilter() {
            public boolean accept(File f) {
                String n = f.getName();
                return(!f.isDirectory()
                       &&n.endsWith(".xml")
                       &&!n.startsWith("supplementalData") // not a locale
                       /*&&!n.startsWith("root")*/); // root is implied, will be included elsewhere.
            }
        };
        File baseDir = new File(fileBase);
        return baseDir.listFiles(myFilter);
    }
    

    void writeRadio(WebContext ctx,String xpath,String type,String value,String checked) {
        writeRadio(ctx, xpath, type, value, checked.equals(value));        
    }

    void writeRadio(WebContext ctx,String xpath,String type,String value,boolean checked) {
        ctx.println("<input type=radio name='" + xpath + "/" + type + "' value='" + value + "' " +
            (checked?" CHECKED ":"") + "/>");
    }


    public static final com.ibm.icu.text.Transliterator hexXML = com.ibm.icu.text.Transliterator.getInstance(
        "[^\\u0009\\u000A\\u0020-\\u007E\\u00A0-\\u00FF] Any-Hex/XML");
        
    // cache of documents
    static Hashtable docTable = new Hashtable();
    static Hashtable docVersions = new Hashtable();
    
    public static Document fetchDoc( String locale) {
        Document doc = null;
        doc = (Document)docTable.get(locale);
        if(doc!=null) {
            return doc;
        }
        String fileName = fileBase + File.separator + locale + ".xml";
        File f = new File(fileName);
        boolean ex  = f.exists();
        boolean cr  = f.canRead();
        String res  = null; /* request.getParameter("res"); */ /* ALWAYS resolve */
        String ver = LDMLUtilities.getCVSVersion(fileBase, locale + ".xml");
        if(ver != null) {
            docVersions.put(locale, ver);
        }
        if((res!=null)&&(res.length()>0)) {
            // throws exception
            doc = LDMLUtilities.getFullyResolvedLDML(fileBase, locale, 
                   false, false, false);
        } else {
            doc = LDMLUtilities.parse(fileName, false);
        }
        if(doc != null) {
            // add to cache
            docTable.put(locale, doc);
        }
        collectXpaths(doc, "/");
        if((cldrLoad != null) && cldrLoad.equals("y")) {
            System.err.print('x'); 
            System.err.flush();
        }
        return doc;
    }
        
    void loadAll() {   
        System.err.println("Pre-Loading cache...");
        File[] inFiles = getInFiles();
        int nrInFiles = inFiles.length;
        for(int i=0;i<nrInFiles;i++) {
            String localeName = inFiles[i].getName();
            int dot = localeName.indexOf('.');
            if(dot !=  -1) {
                localeName = localeName.substring(0,dot);
                System.err.print('.');
                System.err.flush();
                try {
                    fetchDoc(localeName);
                } catch(Throwable t) {
                    System.err.println();
                    System.err.println(localeName + " - err: " + t.toString());
                    t.printStackTrace();
                    System.err.println(localeName + " - skipped!");
                }
            }
        }
        System.err.println();
        System.err.println("Done. Fetched " + nrInFiles + " files.");
    }
    
    private static synchronized void appendLog(String what) {
        try {
          OutputStream file = new FileOutputStream(new File(vetdata,LOGFILE), true); // Append
          PrintWriter pw = new PrintWriter(file);
          pw.println(new Date().toString()  + '\t' +
                     WebContext.userIP() + '\t' + 
                     what);
         pw.close();
         file.close();
        }
        catch(IOException exception){
          System.err.println(exception);
          // TODO: log this ... 
        }
    }

    static TreeMap allXpaths = new TreeMap();

    static void collectXpaths(Node root, String xpath) {
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            String nodeName = node.getNodeName();
            String nodeType = LDMLUtilities.getAttributeValue(node,LDMLConstants.TYPE);
            String newPath = xpath + "/" + nodeName;
            if((nodeType != null)&&(nodeType.length()>0)) {
                newPath = newPath + "[@type='" + nodeType + "']";
            }
            allXpaths.put(newPath, newPath);
            collectXpaths(node, newPath);
        }
    }



}
