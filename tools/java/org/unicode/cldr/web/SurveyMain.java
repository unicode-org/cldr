 /*
 ******************************************************************************
 * Copyright (C) 2004-2005, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package org.unicode.cldr.web;

import java.io.*;
import java.util.*;
import java.lang.ref.SoftReference;

// servlet imports
import javax.servlet.*;
import javax.servlet.http.*;

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

// sql imports
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Properties;


import com.ibm.icu.lang.UCharacter;
//import org.html.*;
//import org.html.utility.*;
//import org.html.table.*;

public class SurveyMain extends HttpServlet {
    public static final String CLDR_HELP_LINK = "http://bugs.icu-project.org/cgibin/cldrwiki.pl?SurveyToolHelp";
    public static final String CLDR_ALERT_ICON = "/~srloomis/alrt.gif";
    public static final String SUBVETTING = "//v"; // vetting context
    public static final String SUBNEW = "//n"; // new context
    public static final String NOCHANGE = "nochange";
    public static final String CURRENT = "current";
    public static final String PROPOSED = "proposed";
    public static final String NEW = "new";
    public static final String DRAFT = "draft";
    public static final String UNKNOWNCHANGE = "Click to suggest replacement";
    
    // SYSTEM PROPERTIES
    public static  String vap = System.getProperty("CLDR_VAP"); // Vet Access Password
    public static  String vetdata = System.getProperty("CLDR_VET_DATA"); // dir for vetted data
    public static  String vetweb = System.getProperty("CLDR_VET_WEB"); // dir for web data
    public static  String cldrLoad = System.getProperty("CLDR_LOAD_ALL"); // preload all locales?
    static String fileBase = System.getProperty("CLDR_COMMON") + "/main"; // not static - may change lager
    static String specialMessage = System.getProperty("CLDR_MESSAGE"); // not static - may change lager
    public static java.util.Properties survprops = null;
    public static String cldrHome = null;
    
    public final void init(final ServletConfig config)
      throws ServletException {
        super.init(config);

        survprops = new java.util.Properties(); 
        cldrHome = config.getInitParameter("cldr.home");
        if(cldrHome == null) {
            cldrHome = System.getProperty("catalina.home");
            if(cldrHome == null) {  
                busted("no $(catalina.home) set - please use it or set a servlet parameter cldr.home");
                return;
            } 
            File homeFile = new File(cldrHome, "cldr");
            if(!homeFile.isDirectory()) {
                busted("$(catalina.home)/cldr isn't working as a CLDR home. Not a directory: " + homeFile.getAbsolutePath());
                return;
            }
            cldrHome = homeFile.getAbsolutePath();
        }

        try {
            java.io.FileInputStream is = new java.io.FileInputStream(new java.io.File(cldrHome, "cldr.properties"));
            survprops.load(is);
            is.close();
        } catch(java.io.IOException ioe) {
            /*throw new UnavailableException*/
            busted("Couldn't load cldr.properties file from '" + cldrHome + "/cldr.properties': "
                + ioe.toString()); /* .initCause(ioe);*/
            return;
        }

        vap = survprops.getProperty("CLDR_VAP"); // Vet Access Password
        if((vap==null)||(vap.length()==0)) {
            /*throw new UnavailableException*/
            busted("No vetting password set. (CLDR_VAP in cldr.properties)");
            return;
        }
        vetdata = survprops.getProperty("CLDR_VET_DATA", cldrHome+"/vetdata"); // dir for vetted data
        System.out.println("CVD= " + vetdata);
        vetweb = survprops.getProperty("CLDR_VET_WEB",cldrHome+"/vetdata"); // dir for web data
        cldrLoad = survprops.getProperty("CLDR_LOAD_ALL"); // preload all locales?
        fileBase = survprops.getProperty("CLDR_COMMON",cldrHome+"/common") + "/main"; // not static - may change lager
        specialMessage = survprops.getProperty("CLDR_MESSAGE"); // not static - may change lager

        if(!new File(fileBase).isDirectory()) {
            busted("CLDR_COMMON isn't a directory: " + fileBase);
            return;
        }

        if(!new File(vetdata).isDirectory()) {
            busted("CLDR_VET_DATA isn't a directory: " + vetdata);
            return;
        }
        if(!new File(vetweb).isDirectory()) {
            busted("CLDR_VET_WEB isn't a directory: " + vetweb);
            return;
        }

        startTime = System.currentTimeMillis();
        doStartup();
        //      throw new UnavailableException("Document path not set.");
        
        File cacheDir = new File(cldrHome, "cache");
        appendLog("Cache Dir: " + cacheDir.getAbsolutePath() + " - creating and emptying..");
        CachingEntityResolver.setCacheDir(cacheDir.getAbsolutePath());
        CachingEntityResolver.createAndEmptyCacheDir();
    }

    public static final String LOGFILE = "cldr.log";        // log file of all changes
    public static final ULocale inLocale = new ULocale("en"); // locale to use to 'localize' things
    
    static final String PREF_SHOWCODES = "p_codes";
    static final String PREF_SORTALPHA = "p_sorta";
    
    // types of data
    static final String LOCALEDISPLAYNAMES = "//ldml/localeDisplayNames/";
    public static final String NUMBERSCURRENCIES = LDMLConstants.NUMBERS + "/currencies";
    public static final String CURRENCYTYPE = "//ldml/numbers/currencies/currency[@type='";
    // All of the data items under LOCALEDISPLAYNAMES (menu items)
    static final String LOCALEDISPLAYNAMES_ITEMS[] = { 
        LDMLConstants.LANGUAGES, LDMLConstants.SCRIPTS, LDMLConstants.TERRITORIES,
        LDMLConstants.VARIANTS, LDMLConstants.KEYS, LDMLConstants.TYPES
    };
    
    // 
    public static final String OTHERROOTS_ITEMS[] = {
        LDMLConstants.CHARACTERS,
        NUMBERSCURRENCIES,
        LDMLConstants.NUMBERS + "/",
        LDMLConstants.DATES + "/timeZoneNames",
        LDMLConstants.DATES + "/calendars",
        LDMLConstants.DATES + "/"
    };
    
//    public static String xMAIN = "General";
    public static String xOTHER = "Misc";
    public static String xNODESET = "NodeSet@"; // pseudo-type used to store nodeSets in the hash
    public static String xREMOVE = "REMOVE";
    public UserRegistry reg = null;
    
    public static String xREVIEW = "Review and Submit";
    public static String xSAVE = "Add Changes on This Page";
    
    private int n = 0;
    synchronized int getN() {
        return n++;
    }
    
    long startTime = 0;
    public SurveyMain() {
    }
    
    /**
     * output MIME header, build context, and run code..
     */
     public static String isBogus = null;
    public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
    {
        response.setContentType("text/html; charset=utf-8");

        pages++;
        
        if(isBogus != null) {
            PrintWriter out = response.getWriter();
            out.println("<html>");
            out.println("<head>");
            out.println("<title>CLDR tool broken</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>CLDR Survey Tool is down, because:</h1>");
            out.println("<tt>" + isBogus + "</tt><br />");
            out.println("<hr />");
            out.println("Please try this link for info: <a href='http://dev.icu-project.org/cgi-bin/cldrwiki.pl?SurveyTool/Status'>http://dev.icu-project.org/cgi-bin/cldrwiki.pl?SurveyTool/Status</a><br />");
            out.println("<hr />");
            out.println("<i>Note: to prevent thrashing, this servlet is down until someone reloads it. " + 
                " (you are unhappy visitor #" + pages + ")</i>");
            return;        
        }
        
        WebContext ctx = new WebContext(request,response);
        
        if(ctx.field("vap").equals(vap)) {  // if we have a Vetting Administration Password, special case
            doVap(ctx);
        } else if(ctx.field("dump").equals(vap)) {
            doDump(ctx);
        } else if(ctx.field("xpaths").length()>0) {
            doXpaths(ctx); 
        } else {
            doSession(ctx); // Session-based Survey main
        }

        ctx.close();
    }

    public static String timeDiff(long a) {
        return timeDiff(a,System.currentTimeMillis());
    }
    
    public static String timeDiff(long a, long b) {
        com.ibm.icu.text.RuleBasedNumberFormat durationFormatter
                    = new com.ibm.icu.text.RuleBasedNumberFormat(Locale.US,
                    com.ibm.icu.text.RuleBasedNumberFormat.DURATION);

        long age = b - a;
        return durationFormatter.format(age/1000);
        /*
        double hours = (age * 1.0) / 3600000.0;
        if(hours < 1.0) {
            double mins = hours * 60.0;
            if(mins < 1.0) {
                double secs = hours * 60.0;
                return new Double(secs).toString + "sec";
            } else {
                return new Double(mins).toString() + "min";
            }
        } else {
            return new Double(hours).toString() + "hr";
        }  
        */
    }

    private void doDump(WebContext ctx)
    {
        printHeader(ctx, "Current Sessions");
        ctx.println("<h1>System info</h1>");
        ctx.println("<div class='pager'>");
        ctx.println("Uptime: " + timeDiff(startTime) + ", " + pages + " pages served.<br/>");
        Runtime r = Runtime.getRuntime();
        double total = r.totalMemory();
        total = total / 1024;
        double free = r.freeMemory();
        free = free / 1024;
        ctx.println("Free memory: " + free + "M / " + total + "M.<br/>");
        r.gc();
        ctx.println("Ran gc();<br/>");
        {
            Hashtable nodeHash = getNodeHash();            
            if(nodeHash != null) {
                ctx.println("Node hash has " + nodeHash.size() + " items");
            } else {
                ctx.println("Node hash is null");
            }
            double avgPuts = ((double)nodeHashPuts)/((double)nodeHashCreates);
            ctx.println(", and was created " +  nodeHashCreates + " times. <br/>");
            ctx.println("(For " + nodeHashPuts + " puts, that's an average of " + avgPuts + ")<br/>");
        }
        ctx.println("String hash has " + stringHash.size() + " items.<br/>");
        ctx.println("xString hash has " + xstringHash.size() + " items.<br/>");
        ctx.println("xpath hash has " + allXpaths.size() + " items.<br/>");
        ctx.println("</div>");
        ctx.println("<hr/>");
        ctx.println("<h1>Current Sessions</h1>");
        ctx.println("<table border=1><tr><th>id</th><th>age (h:mm:ss)</th><th>user</th><th>what</th></tr>");
        for(Iterator li = CookieSession.getAll();li.hasNext();) {
            CookieSession cs = (CookieSession)li.next();
            ctx.println("<tr><td>" + cs.id + "</td>");
            ctx.println("<td>" + timeDiff(cs.last) + "</td>");
            ctx.println("<td>" + cs.user.email + "<br/>" + 
                                 cs.user.real + "<br/>" + 
                                 cs.user.sponsor + "</td>");

            ctx.println("<td>");
            Hashtable lh = cs.getLocales();
            Enumeration e = lh.keys();
            if(e.hasMoreElements()) { 
                for(;e.hasMoreElements();) {
                    String k = e.nextElement().toString();
                    ctx.println(new ULocale(k).getDisplayName() + " ");
                }
            }
            ctx.println("</td>");
            ctx.println("</tr>");
        }
        ctx.println("</table>");
        
        printFooter(ctx);
    }
    
    private void doXpaths(WebContext ctx)
    {
        printHeader(ctx, "Xpaths");
        ctx.println("<h1>Xpaths</h1>");
        ctx.println("<table border=1>");
        for(Iterator li = allXpaths.keySet().iterator();li.hasNext();) {
            String ln = (String)li.next();
//            String l = (String)allXpaths.get(ln);
            ctx.println("<tr>");
            ctx.println(" <td>" + ln + "</td>");
//            ctx.println(" <td>" + l + "</td>");
            ctx.println("</tr>");
        }
        ctx.println("</table>");
        printFooter(ctx);
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
        ctx.println("<link rel='stylesheet' type='text/css' href='" + ctx.context("surveytool.css") + "' />");
        // + "http://www.unicode.org/webscripts/standard_styles.css'>"
        ctx.println("<title>CLDR Vetting | ");
        if(ctx.locale != null) {
            ctx.print(ctx.locale.getDisplayName() + " | ");
        }
        ctx.println(title + "</title>");
        ctx.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">");
        ctx.println("</head>");
        ctx.println("<body>");
            
        ctx.println("<script type='text/javascript'><!-- \n" +
                    "function show(what)\n" +
                    "{document.getElementById(what).style.display=\"block\";}\n" +
                    "--></script>");
        
    }

    public void printFooter(WebContext ctx)
    {
        ctx.println("</body>");
        ctx.println("<hr>");
        ctx.println("<div style='float: right'>" + pages + "</div>");
        ctx.println("<a href='http://www.unicode.org'>Unicode</a> | <a href='http://www.unicode.org/cldr'>Common Locale Data Repository</a> <br/>");
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
        if(ctx.field("submit").length()<=0) {
            printHeader(ctx, title);
        } else {
            if(ctx.field("submit").equals("preview")) {
                printHeader(ctx, "Review changes");
            } else {
                printHeader(ctx, "Submitted changes");
            }
        }
        
        // Not doing vetting admin --------
        
        WebContext baseContext = new WebContext(ctx);
        if((ctx.locale != null) /* && (ctx.field("submit").length()<=0)*/ ) {
            // unless we are submitting - process any pending form data.
            processChanges(ctx, which);
        }
        
        // print 'shopping cart'
        {
            if(ctx.session.user != null) {
                if(ctx.field("submit").length()==0) {            
                    ctx.println("<form method=POST action='" + ctx.base() + "'>");
                }
                ctx.println("<b>Welcome " + ctx.session.user.real + " (" + ctx.session.user.sponsor + ") !</b> <a href=\"" + ctx.base() + "\">[Sign Out]</a><br/>");
            }
            ctx.println(" &nbsp; <span style='font-size:large;'>");
            ctx.printHelpLink("","Instructions"); // base help
            ctx.println("</span>");
            if(ctx.field("submit").length()==0) {
                Hashtable lh = ctx.session.getLocales();
                Enumeration e = lh.keys();
                if((ctx.session.user != null) && (ctx.locale != null) ) {
                    if(ctx.field("submit").length()==0) {
                        ctx.println("<div>");
                        ctx.println("<input type=submit value='" + xSAVE + "'>");
                        ctx.println("</div>");
                    }
                }
                if(e.hasMoreElements()) { 
                    ctx.println("<B>Changed locales: </B> ");
                    for(;e.hasMoreElements();) {
                        String k = e.nextElement().toString();
                        ctx.println("<a href=\"" + baseContext.url() + "&_=" + k + "\">" + 
                                new ULocale(k).getDisplayName() + "</a> ");
                    }
                    if(ctx.session.user != null) {
                        ctx.println("<div>");
                        ctx.println("Your changes are <b><font color=red>not</font></b> permanently saved until you: ");
                        ctx.println("<input name=submit type=submit value='" + xREVIEW +"'>");
                        ctx.println("</div>");
                    }
                }
            } else {
                ctx.println("<a href='" + ctx.url() + "'><b>List of Locales</b></a>");
            }
            ctx.println("<hr/>");
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
            if(inFiles == null) {
                busted("Can't load CLDR data files from " + fileBase);
                throw new RuntimeException("Can't load CLDR data files from " + fileBase);
            }
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
        boolean hasDraft = draftSet.contains(localeName);
        ctx.print(hasDraft?"<b>":"") ;
        ctx.print("<a " + (hasDraft?"class='draftl'":"class='nodraft'") 
            +" title='" + localeName + "' href=\"" + ctx.url() 
            + "&" + "_=" + localeName + "\">" +
            n + "</a>");
        ctx.print(hasDraft?"</b>":"") ;
    }
    
    void doLocaleList(WebContext ctx, WebContext baseContext) {
        boolean showCodes = ctx.prefBool(PREF_SHOWCODES);
        
        {
            WebContext nuCtx = new WebContext(ctx);
            nuCtx.addQuery(PREF_SHOWCODES, !showCodes);
            nuCtx.println("<div class='pager' style='float: right;'>");
            nuCtx.println("<a href='" + nuCtx.url() + "'>" + ((!showCodes)?"Show":"Hide") + " locale codes</a>");
            nuCtx.println("</div>");
        }
        ctx.println("<h1>Locales</h1>");
        TreeMap lm = getLocaleListMap();
        if(lm == null) {
            busted("Can't load CLDR data files from " + fileBase);
            throw new RuntimeException("Can't load CLDR data files from " + fileBase);
        }
        ctx.println("<table border=1 class='list'>");
        int n=0;
        for(Iterator li = lm.keySet().iterator();li.hasNext();) {
            n++;
            String ln = (String)li.next();
            String l = (String)lm.get(ln);
            ctx.print("<tr class='row" + (n%2) + "'>");
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
        ctx.println("(Locales containing draft items are shown in <b><a class='draftl' href='#'>bold</a></b>)<br/>");
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
            ctx.print("<b class='selected'>");
        } else {
            ctx.print("<a class='notselected' href=\"" + ctx.url() +  "&x=" + menu +
            "\">");
        }
        if(menu.endsWith("/")) {
            ctx.print(menu + "<font size=-1>(other)</font>");
        } else {
            ctx.print(menu);
        }
        if(menu.equals(which)) {
            ctx.print("</b>");
        } else {
            ctx.print("</a>");
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
            u = reg.add(ctx, email, sponsor, name, requester);
            appendLog(ctx, "User added: " + name + " <" + email + ">, Sponsor: " + sponsor + " <" + requester + ">" + 
                " (user ID " + u.id + " )" );
            notifyUser(ctx, u, requester);
        }
        printFooter(ctx);
    }
   
    void notifyUser(WebContext ctx, UserRegistry.User u, String requester) {
        String body = requester +  " has created a CLDR vetting account for you.\n" +
            "To access it, visit: \n" +
            "   http://" + ctx.serverName() + ctx.base() + "?uid=" + u.id + "&email=" + u.email + "\n" +
            "\n" +
            " Please keep this link to yourself. Thanks.\n" +
            " \n";
        ctx.println("<hr/><pre>" + body + "</pre><hr/>");
    
        String from = survprops.getProperty("CLDR_FROM","nobody@example.com");
        String smtp = survprops.getProperty("CLDR_SMTP","127.0.0.1");
         if(smtp == null) {
            ctx.println("<i>Not sending mail- SMTP disabled.</i><br/>");
            smtp = "NONE";
        } else {
            MailSender.sendMail(u.email, "CLDR Registration for " + u.email,
                body);
            ctx.println("Mail sent to " + u.email + " from " + from + " via " + smtp + "<br/>\n");
        }
        appendLog(ctx, "Login URL sent to " + u.email + " (#" + u.id + ") from " + from + " via " + smtp);
        /* some debugging. */
    }

    public void doSubmit(WebContext ctx)
    {
        if((ctx.session.user == null) ||
            (ctx.session.user.id == null)) {  
            ctx.println("Not logged in... please see this help link: ");
            ctx.printHelpLink("/NoUser");
            ctx.println("<p>");
            ctx.println("<i>Note: if you used the 'back' button on your browser, please instead use " + 
                " the URL link that was emailed to you. </i>");
            printFooter(ctx);
            return;
        }
        UserRegistry.User u = ctx.session.user;
        if(u == null) {
            u = reg.getEmptyUser();
        }
        WebContext subContext = new WebContext(ctx);
        subContext.addQuery("submit","post");
        boolean post = (ctx.field("submit").equals("post"));
        if(post == false) {
            ctx.println("<p class='hang'><B>Please read the following carefully. If there are any errors, hit Back and correct them.  " + 
                "Your submission will NOT be recorded unless you click the 'Submit'  button on this page!</b></p>");
            {
                subContext.println("<form method=POST action='" + subContext.base() + "'>");
                subContext.printUrlAsHiddenFields();
                subContext.println("<input type=submit value='Submit'>");
                subContext.println("</form>");
            }
        } else {
            ctx.println("<h2>Submitted the following changes:</h2><br/>");
             {
                ctx.println("<form method=GET action='" + ctx.base() + "'>");
                ctx.println("<input type=hidden name=uid value='" + ctx.session.user.id + "'> " + // NULL PTR
                            "<input type=hidden name=email value='" + ctx.session.user.email + "'>");
                ctx.println("<input type=submit value='Login Again'>");
                ctx.println("</form>");
            }
       }
        ctx.println("<hr/>");
        ctx.println("<div class=pager>");
        ctx.println("You:  " + u.real + " &lt;" + u.email + "&gt;<br/>");
        ctx.println("Your sponsor: " + u.sponsor);
        ctx.println("</div>");
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
                if(!post) {
                    ctx.println("<a class='pager' style='float: right;' href='" + ctx.url() + "&_=" + k + "&x=" + xREMOVE + "'>[Cancel This Edit]</a>");
                }
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
                        PrintWriter pw2 = BagFormatter.openUTF8Writer(sessDir + File.separator, k+".xml");
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
            subContext.println("<input type=submit value='Submit'>");
            subContext.println("</form>");
        } else {        
            String body = "User:  " + u.real + " <" + u.email + "> for  " + u.sponsor + "\n" +
             "Submitted data for: " + changedList + "\n" +
             "Session ID: " + ctx.session.id + "\n";
            String smtp = survprops.getProperty("CLDR_SMTP","127.0.0.1");
            if(smtp == null) {
                ctx.println("<i>Not sending mail- SMTP disabled.</i><br/>");
            } else {
                MailSender.sendMail(u.email, "CLDR: Receipt of your data submission ",
                        "Submission from IP: " + ctx.userIP() + "\n" + body  +
                            "\n The files submitted are attached below: \n" + fullBody );
                MailSender.sendMail(survprops.getProperty("CLDR_NOTIFY"), "CLDR: from " + u.sponsor + 
                        "/" + u.email + ": " + changedList,
                        "URL: " + survprops.getProperty("CLDR_VET_WEB_URL","file:///dev/null") + u.email + "/" + ctx.session.id + "\n" +
                        body);
                ctx.println("Thank you..   An email has been sent to the CLDR Vetting List and to you at " + u.email + ".<br/>");
            }
            appendLog(ctx, "Data submitted: " + u.real + " <" + u.email + "> Sponsor: " + u.sponsor + ": " +
                changedList + " " + 
                " (user ID " + u.id + ", session " + ctx.session.id + " )" );
            // destroy session
            {
                ctx.println("<form method=GET action='" + ctx.base() + "'>");
                ctx.println("<input type=hidden name=uid value='" + ctx.session.user.id + "'> " + // NULL PTR
                            "<input type=hidden name=email value='" + ctx.session.user.email + "'>");
                ctx.println("<input type=submit value='Login Again'>");
                ctx.println("</form>");
            }
            ctx.session.remove();
        }
        printFooter(ctx);
    }
    
    /**
     * Append codes to a CLDRFile 
     **/
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
                String newxpath;
                if(xpath != null) {
                    newxpath = xpath + subtype + "[@type='" + type + "']";
                    if(nse.key != null) {
                        newxpath = newxpath + "[@key='" + nse.key + "']";
                    }
                } else {
                    newxpath = nse.xpath; 
                    if(type == null) {
                        type = xpath + "/"; // FIXME: don't set the xpath this way!
                    }
                }
                newxpath=newxpath.substring(1); // remove initial /     
                if(vet.equals(DRAFT)) {
                    if((nse.main != null) && nse.mainDraft) {
                        file.add(newxpath, newxpath, LDMLUtilities.getNodeValue(nse.main));
                    } else {
                        file.addComment(newxpath, "Can't find draft data! " + type, XPathParts.Comments.POSTBLOCK);
                    }
                } else if(vet.equals(CURRENT)) {
                    if(nse.fallback != null)  {
                        file.add(newxpath, newxpath, LDMLUtilities.getNodeValue(nse.fallback));
                    } else if((nse.main != null)&&!LDMLUtilities.isNodeDraft(nse.main)) {
                        file.add(newxpath, newxpath, LDMLUtilities.getNodeValue(nse.main));
                    } else {
                        file.add(newxpath, newxpath, type);
                    }
                } else if(vet.startsWith(PROPOSED)) {
                    String whichProposed = vet.substring(PROPOSED.length());
                    newxpath=newxpath+"[@alt='" + whichProposed + "']"; // append alt if user selected.
                    file.add(newxpath, newxpath, LDMLUtilities.getNodeValue((Node)nse.alts.get(whichProposed)));
                } else if(vet.equals(NEW)) {
                    String newString = (String)data.get(type + SUBNEW); //type could be xpath here
                    if(newString == null) {
                        newString = "";
                    }
                    if(nse.main != null) { // If there is already an existing main (which might be draft)
                       newxpath = newxpath + "[@alt='proposed-new']";
                    }
                    newxpath = newxpath + "[@draft='true']"; // always draft
///*srl*/                 ctx.println("<tt>CLDRFile.add(<b>" + newxpath + "</b>, \"\", blah);</tt><br/>");
                    file.add(newxpath, newString);
                    if(newString.length() ==0) {
                        file.addComment(newxpath, "Item marked as wrong:  " + type, XPathParts.Comments.LINE);
///*srl*/                 ctx.println("<tt>CLDRFile.addComment(<b>" + newxpath + "</b>, blah, LINE);</tt><br/>");
                    }
                } else {
                    // ignored:  current, etc.
                }
            }
        }
    }
    
    /**
     * Convert from the parent to a child type.  i.e. 'languages' -> 'language'
     */
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
    
    /**
     * Append the codes for a certain hashtable into the CLDRFile 
     */
    private void appendCodes(WebContext ctx, CLDRFile file, String xpath, String type, Hashtable data) {
        String fullXpath = xpath + type;
        Hashtable items = (Hashtable)data.get(fullXpath);
        String subtype = typeToSubtype(type);
        appendCodeList(ctx, file, xpath, subtype, items);
    }

    private void appendOtherCodes(WebContext ctx, CLDRFile file, Hashtable data) {
        Hashtable items = (Hashtable)data.get(xOTHER);
        appendCodeList(ctx, file, null, null, items);
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
        appendOtherCodes(ctx, file, data);
        return file;
    }
    
    /**
     * process (convert user field -> hashtable entries) any form items needed.
     * @param ctx context
     * @param which value of 'x' parameter.
     */
    public void processChanges(WebContext ctx, String which)
    {
        NodeSet ns = getNodeSet(ctx, which);
        // locale display names
        for(int n =0 ; n < LOCALEDISPLAYNAMES_ITEMS.length; n++) {
            if(which.equals(LOCALEDISPLAYNAMES_ITEMS[n])) {
                processCodeListChanges(ctx, LOCALEDISPLAYNAMES +LOCALEDISPLAYNAMES_ITEMS[n], ns);
                return;
            }
        }
        
        processOther(ctx, which, ns);
    }

    /**
     * Parse query fields, update hashes, etc.
     * later, we'll see if we can generalize this function.
     */
    public void processCodeListChanges(WebContext ctx, String xpath, NodeSet mySet) {
            Hashtable changes = (Hashtable)ctx.getByLocale(xpath);
            // prepare a new hashtable
            if(changes==null) { 
                changes = new Hashtable(); 
            }
        // process items..
        for(Iterator e = mySet.iterator();e.hasNext();) {
            NodeSet.NodeSetEntry f = (NodeSet.NodeSetEntry)e.next();
            processUserInput(ctx, changes, xpath, f.type, f); 
        }
        if((changes!=null) && (!changes.isEmpty())) { 
            ctx.putByLocale(xpath,changes); 
        }
    }
    
    /**
     * process one of the 'other' (non localedisplay items)
     */
    void processOther(WebContext ctx, String which, NodeSet mySet) {
        Hashtable changes = (Hashtable)ctx.getByLocale(xOTHER);
        // prepare a new hashtable
        if(changes==null) { 
            changes = new Hashtable(); 
        }
        // process items..
        for(Iterator e = mySet.iterator();e.hasNext();) {
            NodeSet.NodeSetEntry f = (NodeSet.NodeSetEntry)e.next();
            processUserInput(ctx, changes, f.xpath, null, f);
        }
        if((changes!=null) && (!changes.isEmpty())) { 
            ctx.putByLocale(xOTHER,changes); 
        }
    }

    /**
     * process user input (form field) for a single data item
     * @param f opaque object to be inserted
     */
    void processUserInput(WebContext ctx, Hashtable changes, String xpath, String type, Object f) {
        // Analyze user input.
        String checked = null;  // What option has the user checked?
        String newString = null;  // What option has the user checked?
        String fieldBase = fieldsToHtml(xpath,type);
        String hashBase;
        if(type == null) {
            type = "";
            hashBase = xpath;
        } else {
            hashBase = type;
        }
        if(changes != null) {
            checked = (String)changes.get(hashBase + SUBVETTING); // fetch VETTING data
            newString = (String)changes.get(hashBase + SUBNEW); // fetch NEW data
        }
        if(checked == null) {
            checked = NOCHANGE;
        }
        
        if(fieldBase == null) {
            ctx.print("<h1>SEVERE ERROR: f2h gave null in " + xpath + "|" + type + "</h1>");
            return;
        }
        
        String formChecked = ctx.field(fieldBase);
        
        if((formChecked != null) && (formChecked.length()>0)) {   
            // Don't consider the 'new text' form, unless we know the 'changes...' checkbox is present.
            // this is because we can't distinguish between an empty and a missing field.
            String formNew = ctx.field(fieldBase + SUBNEW );
            if((formNew.length()>0) && !formNew.equals(UNKNOWNCHANGE)) {
                changes.put(hashBase + SUBNEW, formNew);
                changes.put(hashBase, f); // get the NodeSet in for later use
                newString = formNew;
                if(formChecked.equals(NOCHANGE)) {
                    formChecked = NEW;
                    changes.put(xOTHER + "/" + NEW, fieldBase);
                }
            } else if((newString !=null) && (newString.length()>0)) {
                changes.remove(hashBase + SUBNEW);
                newString = null;
            }

            if(!checked.equals(formChecked)) {
                checked = formChecked;
                if(checked.equals(NOCHANGE)) {
                    changes.remove(hashBase + SUBVETTING); // remove 'current' 
                } else {
/////*srl*/            ctx.println("<tt>Form: " + fieldBase + " - " + formChecked + " - " + xpath + "</tt><br/>");
                    changes.put(hashBase + SUBVETTING, checked); // set
                    changes.put(hashBase, f);
                }
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
        int j;
        int n = ctx.docLocale.length;
        if(which.equals(xREMOVE)) {
            ctx.println("<b><a href=\"" + ctx.url() + "\">" + "List of Locales" + "</a></b><br/>");
            ctx.session.getLocales().remove(ctx.field("_"));
            ctx.println("<h2>Your session for " + ctx.field("_") + " has been removed.</h2>");
            doMain(ctx);
            return;
        }

        ctx.println("<table width='95%' border=0><tr><td width='25%'>");
        ctx.println("<b><a href=\"" + ctx.url() + "\">" + "Locales" + "</a></b><br/>");
        for(i=(n-1);i>0;i--) {
            for(j=0;j<(n-i);j++) {
                ctx.print("&nbsp;&nbsp;");
            }
            ctx.println("\u2517&nbsp;<a href=\"" + ctx.url() + "&_=" + ctx.docLocale[i] + "\">" + ctx.docLocale[i] + "</a> " + new ULocale(ctx.docLocale[i]).getDisplayName() + "<br/>");
        }
        for(j=0;j<n;j++) {
            ctx.print("&nbsp;&nbsp;");
        }
        ctx.println("\u2517&nbsp;<font size=+2><b>" + ctx.locale + "</b></font> " + ctx.locale.getDisplayName() + "<br/>");
        ctx.println("</td><td>");
        
        if((which == null) ||
            which.equals("")) {
            which = LOCALEDISPLAYNAMES_ITEMS[0]; // was xMAIN
        }
        

        WebContext subCtx = new WebContext(ctx);
        subCtx.addQuery("_",ctx.locale.toString());
//        printMenu(subCtx, which, xMAIN);
        subCtx.println("<p class='hang'> Locale Display: ");
        for(n =0 ; n < LOCALEDISPLAYNAMES_ITEMS.length; n++) {        
            //if(n>0) ctx.print(", ");
            printMenu(subCtx, which, LOCALEDISPLAYNAMES_ITEMS[n]);
        }
        subCtx.println("</p> <p class='hang'>Other Items: ");
        for(n =0 ; n < OTHERROOTS_ITEMS.length; n++) {        
            if(n>0) ctx.print(" ");
            printMenu(subCtx, which, OTHERROOTS_ITEMS[n]);
        }
        ctx.print(" ");
        printMenu(subCtx, which, xOTHER);
        subCtx.println("</td></tr></table>");

        subCtx.addQuery("x",which);
        for(n =0 ; n < LOCALEDISPLAYNAMES_ITEMS.length; n++) {        
            if(LOCALEDISPLAYNAMES_ITEMS[n].equals(which)) {
                showLocaleCodeList(subCtx, which);
                return;
            }
        }
        
        // handle from getNodeSet for these . . .
        for(j=0;j<OTHERROOTS_ITEMS.length;j++) {
            if(OTHERROOTS_ITEMS[j].equals(which)) {
                doOtherList(subCtx, which);
                return;
            }
        }
        // fall through if wasn't one of the other roots
        if(xOTHER.equals(which)) {
            doOtherList(subCtx, which);
        } else {
            doMain(subCtx);
        }
    }
 
    /**
     * Show the 'main info about this locale' panel.
     */
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
     * Get a texter for a specific data type
     */
    private static final NodeSet.NodeSetTexter getLanguagesTexter(ULocale l) {
        final ULocale inLocale = l;
        return new NodeSet.NodeSetTexter() { 
                    public String text(NodeSet.NodeSetEntry e) {
                        return new ULocale(e.type).getDisplayLanguage(inLocale);
                    }
            };
    }

    /**
     * Get a texter for a specific data type
     */
    private static final NodeSet.NodeSetTexter getScriptsTexter(ULocale l) {
        final ULocale inLocale = l;
        return new NodeSet.NodeSetTexter() { 
                    public String text(NodeSet.NodeSetEntry e) {
                        return new ULocale("_"+e.type).getDisplayScript(inLocale);
                    }
            };
    }

    /**
     * Get a texter for a specific data type
     */
    private static final NodeSet.NodeSetTexter getTerritoriesTexter(ULocale l) {
        final ULocale inLocale = l;
        return new NodeSet.NodeSetTexter() { 
                    public String text(NodeSet.NodeSetEntry e) {
                        return new ULocale("_"+e.type).getDisplayCountry(inLocale);
                    }
            };
    }

    private static final NodeSet.NodeSetTexter getXpathTexter(NodeSet.NodeSetTexter tx) {
        final NodeSet.NodeSetTexter txl = tx;
        if(tx == null)  {
            return new NodeSet.NodeSetTexter() { 
                        public String text(NodeSet.NodeSetEntry e) {
                            return e.xpath;
                        }
                };
        } else {
            return new NodeSet.NodeSetTexter() { 
                        public String text(NodeSet.NodeSetEntry e) {
                            return txl.text(e) + "|" + e.xpath;
                        }
                };
        }
    }

    /**
     * Get a texter for a specific data type
     */
    private static final NodeSet.NodeSetTexter getVariantsTexter(ULocale l) {
        final ULocale inLocale = l;
        return new NodeSet.NodeSetTexter() { 
                    public String text(NodeSet.NodeSetEntry e) {
                        return new ULocale("__"+e.type).getDisplayVariant(inLocale);
                    }
            };
    }
    
    /**
     * Get the appropriate texter for the type
     */
    NodeSet.NodeSetTexter getTexter(WebContext ctx, String which) {
        if(LDMLConstants.LANGUAGES.equals(which)) {
            return new StandardCodeTexter(which);
        } else if(LDMLConstants.SCRIPTS.equals(which)) {
            return new StandardCodeTexter(which);
        } else if(LDMLConstants.TERRITORIES.equals(which)) {        
            return new StandardCodeTexter(which);
        } else if(LDMLConstants.VARIANTS.equals(which)) {
            return getVariantsTexter(inLocale);  // no default variant list
        } else if(LDMLConstants.KEYS.equals(which)) {
            return new StandardCodeTexter(which);  // no default  list
        } else if(LDMLConstants.TYPES.equals(which)) {
            return new StandardCodeTexter(which);  // no default  list
        } else {
            return null;
        }
    }
    
    /** 
     * Fetch the NodeSet  [ set of resolved items ] from the cache if possible
     */
    private SoftReference nodeHashReference = null;
    int nodeHashPuts = 0;
    private final Hashtable getNodeHash() {
        Hashtable nodeHash = null;
        if((nodeHashReference == null) ||
            ((nodeHash=(Hashtable)nodeHashReference.get())==null)) {
            return null;
        }
        return nodeHash;
    }
    
    int nodeHashCreates = 0;
    NodeSet getNodeSet(WebContext ctx, String which) {
        NodeSet ns = null;
        
        {
            Hashtable nodeHash = getNodeHash();
            if(nodeHash != null) {
                ns = (NodeSet)nodeHash.get(ctx.locale.toString() + "/" + which);
            } 
        }

        if(ns != null) {
            return ns;
        }

        StandardCodes standardCodes = StandardCodes.make();
        Set defaultSet = standardCodes.getAvailableCodes(typeToSubtype(which));
        
        // handle the 'locale display names' cases
        for(int n =0 ; n < LOCALEDISPLAYNAMES_ITEMS.length; n++) {    
            if(LOCALEDISPLAYNAMES_ITEMS[n].equals(which)) {
                ns = getNodeSet(ctx, LOCALEDISPLAYNAMES + LOCALEDISPLAYNAMES_ITEMS[n], 
                    null /* no texter */ , defaultSet);  // no default  list
            }
        }
        if(ns == null) {
            ns = getNodeSetOther(ctx, which);            
        }
        if(ns != null) {
            Hashtable nodeHash = getNodeHash();
            if(nodeHash == null) {
                // create the nodehash
                nodeHashReference = new SoftReference(nodeHash = new Hashtable());   
                ++nodeHashCreates;
   //             double avgPuts = ((double)nodeHashPuts)/((double)nodeHashCreates);
   // /*srl*/            System.err.println("Created nodeHash [#" + nodeHashCreates + "] - avg p/h " + avgPuts + "\n");
            }
            ++nodeHashPuts;
            nodeHash.put(ctx.locale.toString() + "/" + which, ns);
        }
        return ns;
    }
    
    /**
     * Get the node set for an xpath
     */
    NodeSet getNodeSet(WebContext ctx, String xpath, 
                NodeSet.NodeSetTexter texter, Set defaultSet) {
///*srl*/        ctx.println("<tt>load: " + xpath + ", " + defaultSet.size() + "</tt><br/>");
        return NodeSet.loadFromPath(ctx, xpath, defaultSet);
    }
    
    /**
     * Get the node set for an 'other' type. 
     */
    NodeSet getNodeSetOther(WebContext ctx, String which) {
        // handle 'other'
        final String myPrefix = "//ldml/" + which;
        NodeSet.XpathFilter myFilter = null;
        if(which.equals(xOTHER)) {
            myFilter = new NodeSet.XpathFilter() {
                public boolean okay(String path) {
                    if(path.startsWith(LOCALEDISPLAYNAMES)) {
                        return false;
                    }
                    for(int i=0;i<OTHERROOTS_ITEMS.length;i++) {
                        if(path.startsWith("//ldml/"+OTHERROOTS_ITEMS[i])) {
                            return false;
                        }
                    }
                    return true;
                }
            };
        } else {
            if(myPrefix.endsWith("/")) { // filter out all other prefixes..
                myFilter = new NodeSet.XpathFilter() {
                    public boolean okay(String path) {
                        if(!path.startsWith(myPrefix)) {
                            return false;
                        }
                        for(int i=0;i<OTHERROOTS_ITEMS.length;i++) {
                            String aPath = "//ldml/"+OTHERROOTS_ITEMS[i];
                            if(!myPrefix.equals(aPath)) {
                                if(path.startsWith(aPath)) {
                                    return false;
                                }
                            }
                        }
                        return true;
                    }
                };
            } else {
                myFilter = new NodeSet.XpathFilter() {
                    public boolean okay(String path) {
                        return path.startsWith(myPrefix); 
                    }
                };
            }
        }
        NodeSet ns = NodeSet.loadFromXpaths(ctx, allXpaths, myFilter);
        if(which.equals(NUMBERSCURRENCIES)) { // not using defaultSet because the default expansion is more complex..
            StandardCodes standardCodes = StandardCodes.make();
            Set s = standardCodes.getAvailableCodes("currency");
            for(Iterator e = s.iterator();e.hasNext();) {
                String f = (String)e.next();
                ns.addXpath("//ldml/" + which + "/currency[@type='" + f + "']/displayName", f);
                ns.addXpath("//ldml/" + which + "/currency[@type='" + f + "']/symbol", f);
            }
        } else if(which.equals("dates/timeZoneNames")) { // not using defaultSet because the default expansion is more complex..
            StandardCodes standardCodes = StandardCodes.make();
            Set s = standardCodes.getAvailableCodes("tzid");
            for(Iterator e = s.iterator();e.hasNext();) {
                String f = (String)e.next();
                ns.addXpath("//ldml/" + which + "/zone[@type='" + f + "']/exemplarCity", f);
            }
        }
        return ns;
    }

    /**
     * show the list for an 'other' item, i.e. not a locale data item
     */
    void doOtherList(WebContext ctx, String which) {
        showNodeList(ctx, null, getNodeSet(ctx,which), new NodeSet.NodeSetTexter() {
            StandardCodes standardCodes = StandardCodes.make();

            public String text(NodeSet.NodeSetEntry e) {
                if(e.xpath.startsWith(CURRENCYTYPE)) {
                    return standardCodes.getData("currency", e.type);
                } else {
                    return e.type;
                }
            }
        });
    }
    
    /**
     * show the webpage for one of the 'locale codes' items.. 
     * @param ctx the web context
     * @param xpath xpath to the root of the structure
     * @param tx the texter to use for presentation of the items
     * @param fullSet the set of tags denoting the expected full-set, or null if none.
     */
    public void showLocaleCodeList(WebContext ctx, String which) {
        showNodeList(ctx, LOCALEDISPLAYNAMES+which, getNodeSet(ctx, which), getTexter(ctx,which));
    }
    
    final int CODES_PER_PAGE = 80;  // was 51

    /**
     * @param xpath null if 'individual paths'
     */
    public void showNodeList(WebContext ctx, String xpath, NodeSet mySet, NodeSet.NodeSetTexter tx) {
        int count = 0;
        int dispCount = 0;
        int total = 0;
        int skip = 0;
        total = mySet.count();
        boolean sortAlpha = ctx.prefBool(PREF_SORTALPHA);
        NodeSet.NodeSetTexter sortTexter;

        if(sortAlpha) {
            if(xpath == null) {
                sortTexter = getXpathTexter(tx);
            } else {
                sortTexter = tx;
            }
        } else {
            sortTexter = new DraftFirstTexter(tx);
        }
        
        Map sortedMap = mySet.getSorted(sortTexter);
        String hashName = (xpath != null)?xpath:xOTHER;
        Hashtable changes = (Hashtable)ctx.getByLocale(hashName);
        
        if(tx == null) {
            tx = new NullTexter();
        }
        
        // prepare a new hashtable
        if(changes==null) {
            changes = new Hashtable();  // ?? TODO: do we need to create a hashtable here?
        }

        // NAVIGATION .. calculate skips.. 
        skip = showSkipBox(ctx, total, sortedMap, tx);
        if(changes.get(xOTHER + "/" + NEW)!=null) {
            changes.remove(xOTHER + "/" + NEW);
            ctx.println("<div class='missing'><b>Warning: Remember to click the 'change' radio button after typing in a change.  Please check the status of change boxes below.</b></div><br/>");
        }


        
        // Form: 
        ctx.printUrlAsHiddenFields();
        ctx.println("<table class='list' border=1>");
        ctx.println("<tr>");
        ctx.println(" <th class='heading' bgcolor='#DDDDDD' colspan=2>Name<br/><div style='border: 1px solid gray; width: 6em;' align=left><tt>Code</tt></span></th>");
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
            
            if(f.xpath != null) {
                ctx.println("<tr class='xpath'><td colspan=4>"+  f.xpath + "</td></tr>");
            }
            
            if(f.isAlias == true) {
                ctx.println("<tr class='alias'><td colspan=4>" + f.type +
                        "<a href=\"" + ctx.base() + "?x=" + ctx.field("x") + "&_=" + f.fallbackLocale + "\">" + 
                        new ULocale(f.fallbackLocale).getDisplayName() +                        
                        "</a>");
                ctx.println("</td></tr>");
                continue;
            }
            
            
            String type = f.type;
            String base = ((xpath==null)?f.xpath:type);
            String main = null;
            String mainFallback = null;
            int mainDraft = 0; // count
            if(f.main != null) {
                main = LDMLUtilities.getNodeValue(f.main);
                if(f.mainDraft) {
                    mainDraft = 1; // for now: one draft
                }
            }
            String fieldName = ((xpath!=null)?xpath:f.xpath);
            String fieldPath = ((xpath!=null)?type:null);
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
            if(f.alts != null) {
                nRows += f.alts.size();
            }

            // Analyze user input.
            String checked = null;  // What option has the user checked?
            String newString = null;  // What option has the user checked?
            if(changes != null) {
                checked = (String)changes.get(base + SUBVETTING); // fetch VETTING data
                newString = (String)changes.get(base + SUBNEW); // fetch NEW data
///*srl*/                ctx.println(":"+base+SUBVETTING + ":");
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
            
            // see if there's a warning.
            String myxpath = f.xpath;
            if(myxpath == null) {
                myxpath = xpath;
                if(f.main != null) {
                    myxpath = myxpath + "/" + f.main.getNodeName();
                } else {
//                    ctx.println("<B>problem - missing item</B>");
                    myxpath = null;
                }
                if(f.type != null) {
                    myxpath = myxpath + "[@type='" + f.type + "']";
                }
                if(f.key != null) {
                    myxpath = myxpath + "[@key='" + f.key + "']";
                }
                
///*srl*/                ctx.println("[<tt>" + ctx.locale + " " + myxpath + "</tt>]");
            }

            ctx.println("</th>");
            
            // Now there are a pair of columns for each of the following. 
            // 2. fallback
            if(mainFallback != null) {
                ctx.println("<td align=right class='fallback'>");
                ctx.println("from " + 
                        "<a href=\"" + ctx.base() + "?x=" + ctx.field("x") + "&_=" + mainFallback + "\">" + 
                        new ULocale(mainFallback).getDisplayName() +                        
                        "</a>");
                writeRadio(ctx,fieldName,fieldPath,CURRENT,checked);
                ctx.println("</td>");
                ctx.println("<td class='fallback'>");
                ctx.println(LDMLUtilities.getNodeValue(f.fallback));
                ctx.println("</td>");
            } else if((main!=null)&&(mainDraft==0)) {
                ctx.println("<td align=right class='current'>");
                ctx.println("current");
                writeRadio(ctx,fieldName,fieldPath,CURRENT,checked);
                ctx.println("</td>");
                ctx.println("<td class='current'>");
                printWarning(ctx, myxpath);
                ctx.println(main);
                ctx.println("</td>");
            } else /*  if(main == null) */ {
                ctx.println("<td align=right class='missing'>");
                
                if(f.fallbackLocale != null) {
                    ctx.println("see " + 
                            "<a href=\"" + ctx.base() + "?x=" + ctx.field("x") + "&_=" + f.fallbackLocale + "\">" + 
                            new ULocale(f.fallbackLocale).getDisplayName() +                        
                            "</a>");
                } else {
                    ctx.println("<i>current</i>");
                    writeRadio(ctx,fieldName,fieldPath,CURRENT,checked);
                }
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
                writeRadio(ctx,fieldName,fieldPath,DRAFT,checked);
                ctx.println("</td>");
                ctx.println("<td class='draft'>");
                ctx.println(main);
                ctx.println("</td>");
                ctx.println("</tr>");
                ctx.println("<tr>");
            }

            // Proposed item
            if(f.alts != null) {
                Enumeration ee = f.alts.keys();
                for(;ee.hasMoreElements();) {
                    String k = (String)(ee.nextElement());
                    ctx.println("<td align=right class='proposed'>");
                    ctx.println(k);
                    writeRadio(ctx,fieldName,fieldPath,PROPOSED + k,checked);
                    ctx.println("</td>");
                    ctx.println("<td class='proposed'>");
                    printWarning(ctx, myxpath+"[@alt='" + k + "']");
                    ctx.println(LDMLUtilities.getNodeValue((Node)f.alts.get(k)));
                    ctx.println("</td>");
                    ctx.println("</tr>");
                    ctx.println("<tr>");
                }
            }
            
            //'nochange' and type
            ctx.println("<th class='type'>");
            if(type == null) {
                type = "NULL??";
            }
            int lastSlash = type.lastIndexOf('/');
            String showType;
            if(lastSlash != -1) {
                showType = type.substring(lastSlash+1);
            } else {
                showType = type;
            }
            ctx.println("<tt>" + showType + "</tt>");
            ctx.println("</th>");
            ctx.println("<td class='nochange'>");
            ctx.println("Don't care");
            writeRadio(ctx,fieldName,fieldPath,NOCHANGE,checked);
            ctx.println("</td>");

            // edit text
            ctx.println("<td align=right class='new'>");
            ctx.println(((mainDraft>0) || ((f.alts)!=null))?"change":"incorrect");
            writeRadio(ctx,fieldName,fieldPath,NEW,checked);
            ctx.println("</td>");
            ctx.println("<td class='new'>");
            String change = "";
            if(changes != null) {
           //     change = (String)changes.get(type + "//n");
            }
            if((mainDraft>0) || ((f.alts)!=null)) {
                // this is supposed to Automatically check the 'change' button when user types something in.
                // but it doesn't work.
                String blurCheckScript = ""; //" else {  document.forms[0].elements['" + fieldsToHtml(fieldName,fieldPath) + "'][3].checked=true; "
                ctx.print("<input size=50 class='inputbox' ");
                ctx.print("onblur=\"if (value == '') {value = '" + UNKNOWNCHANGE + "'} " + blurCheckScript + " }\" onfocus=\"if (value == '" + 
                    UNKNOWNCHANGE + "') {value =''}\" ");
                ctx.print("value=\"" + 
                      (  (newString!=null) ? newString : UNKNOWNCHANGE )
                        + "\" name=\"" + fieldsToHtml(fieldName,fieldPath) + SUBNEW + "\">");
            } else {
                ctx.print("Item is incorrect.");
            }
            ctx.println("</td>");
            /*
            if((newString != null) && ((checked!=null)&&(!checked.equals(NEW)))) {
                ctx.println("<td class='pager'><b>Don't forget to click \"change\" when submitting a change.</b></td>");
            }
            */
            ctx.println("</tr>");

            ctx.println("<tr class='sep'><td class='sep' colspan=4 bgcolor=\"#CCCCDD\"></td></tr>");

            // -----
            
            if(dispCount >= CODES_PER_PAGE) {
                break;
            }
        }
        ctx.println("</table>");
        /* skip = */ showSkipBox(ctx, total, sortedMap, tx);
        if(ctx.session.user != null) {
            ctx.println("<div style='margin-top: 1em;' align=right><input type=submit value='" + xSAVE + " for " + 
                    ctx.locale.getDisplayName() +"'></div>");
        }
        ctx.println("</form>");
    }
    
    int showSkipBox(WebContext ctx, int total, Map m, NodeSet.NodeSetTexter tx) {
        int skip;
        ctx.println("<div class='pager'>");
 
        {
            WebContext nuCtx = new WebContext(ctx);
            boolean sortAlpha = ctx.prefBool(PREF_SORTALPHA);
            nuCtx.addQuery(PREF_SORTALPHA, !sortAlpha);

            nuCtx.println("<p style='float: right; margin-left: 3em;'> " + 
                "Sorted ");
            if(!sortAlpha) {
                nuCtx.print("<a class='notselected' href='" + nuCtx.url() + "'>");
            } else {
                nuCtx.print("<span class='selected'>");
            }
            nuCtx.print("Alphabetically");
            if(!sortAlpha) {
                nuCtx.println("</a>");
            } else {
                nuCtx.println("</span>");
            }
            
            nuCtx.println(" ");

            if(sortAlpha) {
                nuCtx.print("<a class='notselected' href='" + nuCtx.url() + "'>");
            } else {
                nuCtx.print("<span class='selected'>");
            }
            nuCtx.print("Draft-First");
            if(sortAlpha) {
                nuCtx.println("</a>");
            } else {
                nuCtx.println("</span>");
            }
            nuCtx.println("</p>");
        }

       String str = ctx.field("skip");
        if((str!=null)&&(str.length()>0)) {
            skip = new Integer(str).intValue();
        } else {
            skip = 0;
        }
        if(skip<=0) {
            skip = 0;
        } 

        // calculate nextSkip
        int from = skip+1;
        int to = from + CODES_PER_PAGE-1;
        if(to >= total) {
            to = total;
        }
        
        // Print navigation
        ctx.println("Displaying items " + from + " to " + to + " of " + total);        

        if(total>=(CODES_PER_PAGE)) {
            ctx.println("<br/>");
        }
        
        if(skip>0) {
            int prevSkip = skip - CODES_PER_PAGE;
            if(prevSkip<0) {
                prevSkip = 0;
            }
                ctx.println("<a href=\"" + ctx.url() + 
                        "&skip=" + new Integer(prevSkip) + "\">" +
                        "&lt;&lt;&lt; prev " + CODES_PER_PAGE + "" +
                        "</a> &nbsp;");
            if(skip>=total) {
                skip = 0;
            }
        } else if(total>=(CODES_PER_PAGE)) {
                ctx.println("<span>&lt;&lt;&lt; prev " + CODES_PER_PAGE + "" +
                        "</span> &nbsp;");        
        }

        if(total>=(CODES_PER_PAGE)) {
            for(int i=0;i<total;i+= CODES_PER_PAGE) {
                int end = i + CODES_PER_PAGE-1;
                if(end>=total) {
                    end = total-1;
                }
                boolean isus = (i == skip);
                if(isus) {
                    ctx.println(" <b class='selected'>");
                } else {
                    ctx.println(" <a class='notselected' href=\"" + ctx.url() + 
                        "&skip=" + new Integer(i) + "\">");
                }
                ctx.print( (i+1) + "-" + (end+1));
                if(isus) {
                    ctx.println("</b> ");
                } else {
                    ctx.println("</a> ");
                }
            }
        }
        int nextSkip = skip + CODES_PER_PAGE; 
        if(nextSkip >= total) {
            nextSkip = -1;
            if(total>=(CODES_PER_PAGE)) {
                ctx.println(" <span >" +
                        "next " + CODES_PER_PAGE + "&gt;&gt;&gt;" +
                        "</span>");
            }
        } else {
            ctx.println(" <a href=\"" + ctx.url() + 
                    "&skip=" + new Integer(nextSkip) + "\">" +
                    "next " + CODES_PER_PAGE + "&gt;&gt;&gt;" +
                    "</a>");
        }
        ctx.println("</div>");
        return skip;
    }
    
    static int pages=0;
    /**
     * Main - setup, preload and listen..
     */
    static  boolean isSetup = false;
    public synchronized void doStartup() {
        
        if(isSetup == true) {
            return;
        }
        
        isSetup = true;
        
        int status = 0;
        appendLog("SurveyTool starting up. root=" + new File(cldrHome).getAbsolutePath());
        if ((specialMessage!=null)&&(specialMessage.length()>0)) {
            appendLog("SurveyTool with CLDR_MESSAGE: " + specialMessage);
        }
        SurveyMain m = new SurveyMain();
/*
        if(!m.reg.read()) {
            busted("Couldn't load user registry [at least put an empty file there]   - exiting");
            return;
        }
        */
        if(!readWarnings()) {
            // already busted
            return;
        }
        if((cldrLoad != null) && cldrLoad.length()>0) {
            m.loadAll();
        }
        doStartupDB();
        
        appendLog("SurveyTool ready for requests. Memory in use: " + usedK());
    }
    
    public void destroy() {
        appendLog("SurveyTool shutting down..");
        doShutdownDB();
        busted("servlet destroyed");
        super.destroy();
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
        ctx.println("<input type=radio name='" + fieldsToHtml(xpath,type) + "' value='" + value + "' " +
            (checked?" CHECKED ":"") + "/>");
///*srl*/        ctx.println("<sup><tt>" + fieldsToHtml(xpath,type) + "</tt></sup>");
    }


    public static final com.ibm.icu.text.Transliterator hexXML = com.ibm.icu.text.Transliterator.getInstance(
        "[^\\u0009\\u000A\\u0020-\\u007E\\u00A0-\\u00FF] Any-Hex/XML");

    // like above, but quote " 
    public static final com.ibm.icu.text.Transliterator quoteXML = com.ibm.icu.text.Transliterator.getInstance(
        "[^\\u0009\\u000A\\u0020-\\u0021\\u0023-\\u007E\\u00A0-\\u00FF] Any-Hex/XML");
        
    // cache of documents
    static Hashtable docTable = new Hashtable();
    static Hashtable docVersions = new Hashtable();
    
    public static Document fetchDoc( String locale) {
        Document doc = null;
        locale = pool(locale);
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
            docVersions.put(locale, pool(ver));
        }
        if((res!=null)&&(res.length()>0)) {
            // throws exception
            doc = LDMLUtilities.getFullyResolvedLDML(fileBase, locale, 
                   false, false, false, false);
        } else {
            doc = LDMLUtilities.parse(fileName, false);
        }
        if(doc != null) {
            // add to cache
            docTable.put(locale, doc);
        }
        collectXpaths(doc, locale, "/");
        if((cldrLoad != null) && (cldrLoad.length()>0) && !cldrLoad.startsWith("u")) {
            System.err.print('\b');
            System.err.print('x'); 
            System.err.flush();
        }
        return doc;
    }

    static int usedK() {
        Runtime r = Runtime.getRuntime();
        double total = r.totalMemory();
        total = total / 1024;
        double free = r.freeMemory();
        free = free / 1024;
        return (int)(Math.floor(total-free));
    }
        
        
    void loadAll() {   
        boolean ultra = cldrLoad.startsWith("u");
        System.err.println("Pre-Loading cache... " + usedK() + "K used so far.\n");
        appendLog("SurveyTool pre-loading cache.");
        if(ultra) {
            System.err.println("Ultra Mode [loading ALL nodesets]");
        }
        File[] inFiles = getInFiles();
        int nrInFiles = inFiles.length;
        if(inFiles == null) {
            busted("Can't load CLDR data files from " + fileBase);
            return;
        }
        int ti = 0;
        for(int i=0;i<nrInFiles;i++) {
            String localeName = inFiles[i].getName();
            if(ultra) {
                System.err.print(i + "/" + nrInFiles + ":           " + localeName + "           ");
            }
            int dot = localeName.indexOf('.');
            if(dot !=  -1) {
                localeName = localeName.substring(0,dot);
                if(!ultra && (i>0)&&((i%50)==0)) {
                    System.err.println("   "+ i + "/" + nrInFiles + ". " + usedK() + "K mem used");
                }
                System.err.print('.');
                System.err.flush();
                try {
                    fetchDoc(localeName);
                    
                    if(ultra) {
                        int j;
                        WebContext ctx = new WebContext(false);
                        ctx.setLocale(new ULocale(localeName));
                        for(j =0 ; j < LOCALEDISPLAYNAMES_ITEMS.length; j++) {                                
                            NodeSet ns = getNodeSet(ctx, LOCALEDISPLAYNAMES_ITEMS[j]);
                            ti += ns.count();
                            System.err.print("l");
                            System.err.flush();
                        }
                        for(j =0 ; j < OTHERROOTS_ITEMS.length; j++) {                                
                            NodeSet ns = getNodeSet(ctx, OTHERROOTS_ITEMS[j]);
                            ti += ns.count();
                            System.err.print("o");
                            System.err.flush();
                        }
                        NodeSet ns = getNodeSet(ctx, xOTHER);
                        ti += ns.count();
                        System.err.print("O");
                        System.err.flush();
                        String nodeHashInfo;
                        {
                            Hashtable nodeHash = getNodeHash();            
                            if(nodeHash != null) {
                                nodeHashInfo = "nh" + nodeHash.size();
                            } else {
                                nodeHashInfo = "nhNULL";
                            }
                        }
                        System.err.print("           " + ti + " nodes loaded.. (sc" + stringHash.size() + 
///*srl*/                            "[-" + stringHashHit + "=" + stringHashIdentity + "]" + 
                            ", " + 
                            "xsc" + xstringHash.size() + ", " + nodeHashInfo + ", nhc" + nodeHashCreates + ")         \r");
                        //ctx.close();
                        /* if you want to print otu the string cache for any reason, this is probably as good a place as any. */
                      /*  {                        
                            Enumeration e = stringHash.keys();
                            for(;e.hasMoreElements();) {
                                String k = e.nextElement().toString();
                                System.err.print(k +",");
                            }
                            return; 
                        } */
                    }
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
        System.err.println(" " + usedK() + "K used so far.\n");
        {
            String nodeHashInfo;
            {
                Hashtable nodeHash = getNodeHash();            
                if(nodeHash != null) {
                    nodeHashInfo = "nh" + nodeHash.size();
                } else {
                    nodeHashInfo = "nhNULL";
                }
            }
            System.err.println("Caches:  (sc" + stringHash.size() + 
    ///*srl*/                            "[-" + stringHashHit + "=" + stringHashIdentity + "]" + 
                ", " + 
                "xsc" + xstringHash.size() + ", " + nodeHashInfo + ", nhc" + nodeHashCreates + ")");
        }
    }
    
    private void busted(String what) {
        System.err.println("SurveyTool busted: " + what);
        isBogus = what;
        appendLog(what); // in case the log writing fails.
    }

    private void appendLog(WebContext ctx, String what) {
          String ipInfo =  ctx.userIP();
          appendLog(what, ipInfo);
    }

    public void appendLog(String what) {
          appendLog(what, null);
    }
    
    public synchronized void appendLog(String what, String ipInfo) {
        try {
          OutputStream file = new FileOutputStream(new File(vetdata,LOGFILE), true); // Append
          PrintWriter pw = new PrintWriter(file);
          if(ipInfo == null) { ipInfo = "?"; }
          pw.println(new Date().toString()  + '\t' +
                    ipInfo + '\t' + 
                     what);
         pw.close();
         file.close();
        }
        catch(IOException exception){
          System.err.println(exception);
          isBogus = "Failed to write to log file!" + exception.toString() + " (original message: " + what + "/" + ipInfo + ")";
          System.err.println("CLDR: fatal: " + isBogus);
        }
    }

    static TreeMap allXpaths = new TreeMap();    
    public static Set draftSet = Collections.synchronizedSet(new HashSet());


    static final public String[] distinguishingAttributes =  { "key", "registry", "alt", "iso4217", "iso3166", "type", "default",
                    "measurementSystem", "mapping", "abbreviationFallback", "preferenceOrdering" };

    static int xpathCode = 0;
    static void collectXpaths(Node root, String locale, String xpath) {
        for(Node node=root.getFirstChild(); node!=null; node=node.getNextSibling()){
            if(node.getNodeType()!=Node.ELEMENT_NODE){
                continue;
            }
            String nodeName = node.getNodeName();
            String newPath = xpath + "/" + nodeName;
            for(int i=0;i<distinguishingAttributes.length;i++) {
            String da = distinguishingAttributes[i];
                String nodeAtt = LDMLUtilities.getAttributeValue(node,da);
                if((nodeAtt != null) && 
                    !(da.equals(LDMLConstants.ALT)
                            /* &&nodeAtt.equals(LDMLConstants.PROPOSED) */ )) { // no alts for now
                    newPath = newPath + "[@"+da+"='" + nodeAtt + "']";
                }
            }
            String draft=LDMLUtilities.getAttributeValue(node,LDMLConstants.DRAFT);
            if((draft != null)&&(draft.equals("true"))) {
                draftSet.add(locale);
            }
                
            allXpaths.put(poolx(newPath), CookieSession.j + "X" + CookieSession.cheapEncode(xpathCode++));
            collectXpaths(node, locale, newPath);
        }
    }
    
    /**
     * convert a XPATH:TYPE form to an html field.
     * if type is null, means:  hash the xpath
     */
    static String fieldsToHtml(String xpath, String type)
    {
        if(type == null) {
            String r = (String)allXpaths.get(xpath);
            if(r == null) {
                // we've found a totally new xpath. Mint a new key.
                r = CookieSession.j + "Y" + CookieSession.cheapEncode(xpathCode++);
                allXpaths.put(poolx(xpath), r);
            }
            return r;
        } else {
            return xpath + "/" + type;
        }
    }

    private void printWarning(WebContext ctx, String myxpath) {
        if(myxpath == null) {
            return;
        }
        String warnHash = "W"+fieldsToHtml(myxpath,null);
        String warning = getWarning(ctx.locale.toString(), myxpath);
        if(warning != null) {
            String warningTitle = quoteXML.transliterate(warning);
            ctx.println("<a href='javascript:show(\"" + warnHash + "\")'>" + 
                "<img border=0 align=right src='" + CLDR_ALERT_ICON + "' width=16 height=16 alt=\"" +   
                warningTitle + "\" title=\"" + warningTitle + "\"></a>");
            ctx.println("<!-- <noscript>Warning: </noscript> -->" + 
                "<div style='display: none' class='warning' id='" + warnHash + "'>" +
                warningTitle + "</div>");
        } // else {
///*srl*/                ctx.println("[<tt>" + ctx.locale + " " + myxpath + "</tt>]");
//      }
    }
    
    Hashtable xpathWarnings = new Hashtable();
    
    String getWarning(String locale, String xpath) {
        return (String)xpathWarnings.get(locale+" "+xpath);
    }


    boolean readWarnings() {
        int lines  = 0;
        try {
            BufferedReader in
               = BagFormatter.openUTF8Reader(cldrHome, "surveyInfo.txt");
            String line;
            while ((line = in.readLine())!=null) {
                lines++;
                if((line.length()<=0) ||
                  (line.charAt(0)=='#')) {
                    continue;
                }
                String[] result = line.split("\t");
                // result[0];  locale
                // result[1];  xpath
                // result[2];  warning
                xpathWarnings.put(result[0] + " /" + result[1], result[2]);
            }
        } catch (java.io.FileNotFoundException t) {
//            System.err.println(t.toString());
//            t.printStackTrace();
            appendLog("Warning: Can't read xpath warnings file.  " + cldrHome + "/surveyInfo.txt - To remove this warning, create an empty file there.");
            return true;
        }  catch (java.io.IOException t) {
            System.err.println(t.toString());
            t.printStackTrace();
            busted("Error: trying to read xpath warnings file.  " + cldrHome + "/surveyInfo.txt");
            return true;
        }

//        System.err.println("xpathWarnings" + ": " + lines + " lines read.");
        return true;
    }

    private static Hashtable stringHash = new Hashtable();
    private static Hashtable xstringHash = new Hashtable();
    
    static int stringHashIdentity = 0; // # of x==y hits
    static int stringHashHit = 0;
    
    static final String pool(String x) {
        if(x==null) {
            return null;
        }
        String y = (String)stringHash.get(x);
        if(y==null) {
            stringHash.put(x,x);
            return x;
        } else {
///*srl*/            if(x==y) { stringHashIdentity++; throw new RuntimeException("pool(x)==x"); }
///*srl*/            stringHashHit++;
            return y;
        }
    }
    
    static final String poolx(String x) {
        if(x==null) {
            return null;
        }
        String y = (String)xstringHash.get(x);
        if(y==null) {
            xstringHash.put(x,x);
            return x;
        } else {
            return y;
        }
    }

    // DB stuff
    public String db_driver = "org.apache.derby.jdbc.EmbeddedDriver";
    public String db_protocol = "jdbc:derby:";
    public static final String CLDR_DB = "cldrdb";
    public String cldrdb = null;
   
    private Connection getDBConnection()
    {
        return getDBConnection("");
    }
    
    private Connection getDBConnection(String options)
    {
        try{ 
            Properties props = new Properties();
            props.put("user", "cldr_user");
            props.put("password", "cldr_password");
            cldrdb =  dbDir.getAbsolutePath();
            return DriverManager.getConnection(db_protocol +
                    cldrdb + options, props);
        } catch (SQLException se) {
            busted("Fatal in getDBConnection: " + unchainSqlException(se));
            return null;
        }
    }
    
    File dbDir = null;
     
    private void doStartupDB()
    {
        dbDir = new File(cldrHome,CLDR_DB);
        boolean doesExist = dbDir.isDirectory();
        
        appendLog("SurveyTool setting up database.. " + dbDir.getAbsolutePath());
        try
        {
            Class.forName(db_driver).newInstance();
            appendLog("loaded driver");
            Connection conn = getDBConnection(";create=true");

            System.out.println("Connected to database " + cldrdb);

            conn.setAutoCommit(false);

            if(doesExist == false) {
                appendLog("DB didn't exist - initializing...");
                Statement s = conn.createStatement();
                s.execute("create table junk(num int, addr varchar(40))");
                appendLog("DB: Created table junk");
                s.close();
                UserRegistry.setupDB(this, conn);
                conn.commit();
            }
            
            conn.close();
            appendLog("DB: Committed");
            appendLog("DB: done.");
        }
        catch (SQLException e)
        {
            busted("On DB startup: " + unchainSqlException(e));
        }
        catch (Throwable t)
        {
            busted("Some error on DB startup: " + t.toString());
            t.printStackTrace();
        }
        
        // set up subsidiaries
        reg = new UserRegistry(this, getDBConnection());

    }    
    
    public static final String unchainSqlException(SQLException e) {
        String echain = "SQL exception: ";
        while(e!=null) {
            echain = echain + " * " + e.toString();
            e = e.getNextException();
        }
        return echain;
    }
    
    void doShutdownDB() {
        boolean gotSQLExc = false;

            try
            {
                DriverManager.getConnection("jdbc:derby:;shutdown=true");
            }
            catch (SQLException se)
            {
                gotSQLExc = true;
                appendLog("DB: while shutting down: " + se.toString());
            }

            if (!gotSQLExc)
            {
                appendLog("Database did not shut down normally");
            }
            else
            {
                appendLog("Database shut down normally");
            }
        }
}
