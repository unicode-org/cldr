/*
 ******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and   *
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
    public static final String CURRENT = "current";
    public static final String PROPOSED = "proposed";
    public static final String NEW = "new";
    public static final String DRAFT = "draft";
    public static String vap = System.getProperty("CLDR_VAP");
    public String vetdata = System.getProperty("CLDR_VET_DATA");
    public String vetweb = System.getProperty("CLDR_VET_WEB");
    static String fileBase = System.getProperty("CLDR_COMMON") + "/main";
    
    static final String LOCALEDISPLAYNAMES = "//ldml/localeDisplayNames/";

    public UserRegistry reg = new UserRegistry(vetdata);
    
    private int n = 0;
    synchronized int getN() {
        return n++;
    }
    private SurveyMain() {}
    
    
    private void go(PrintStream out) throws IOException
    {
        WebContext ctx = new WebContext(out);
        ctx.println("Content-type: text/html; charset=\"utf-8\"\n\n");
        doGet(ctx);
  
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

    public void doGet(WebContext ctx)
    {
        ctx.println("<html>");
        ctx.println("<head>");
        ctx.println("<title>Interim Test Tool - s/n " + getN() + "</title>");
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
                    ".sep { height: 5px; background-color: #DDDDDD; } \n" +
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
        
        if(ctx.field("vap").equals(vap)) {
            doVap(ctx);
            ctx.println("</body>");
            ctx.println("</html>");
            return;
        }
        
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
                ctx.println("<i>Reconnecting you to your previous session, " + myNum + "</i></br>");
            }
        }        
        if((mySession == null) && (myNum != null) && (myNum.length()>0)) {
            mySession = CookieSession.retrieve(myNum);
            if(mySession == null) {
                ctx.println("<i>Warning: ignoring expired session " + myNum + "</i><br/>");
            }
        }
        if(mySession == null) {
            mySession = new CookieSession();
        }
        ctx.session = mySession;
        ctx.addQuery("s", mySession.id);
        if(user != null) {
            ctx.session.setUser(user); // this will replace any existing session by this user.
        }
        WebContext baseContext = new WebContext(ctx);
        ctx.println("<i>using session " + mySession.id + " </i><br/>");
        
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
                ctx.println("<a href=\"" + baseContext.url() + "&submit=yes\"><b>Submit Data</b></a>");
            }
            ctx.println("<br/>");
        }

        if(ctx.field("submit").length()>0) {
            doSubmit(ctx);
            ctx.println("</body>");
            ctx.println("</html>");
            return;
        }

        ctx.println("<h1>Locales</h1>");
        String locale = ctx.field("_");
        if(locale != null) {  // knock out some bad cases
            if((locale.indexOf('.') != -1) ||
               (locale.indexOf('/') != -1)) {
               locale = null;
            }
        }
        if((locale==null)||(locale.length()<=0)) {
            ctx.println("<b>locales: </b> <br/>");
            File inFiles[] = getInFiles();
            int nrInFiles = inFiles.length;
            for(int i=0;i<nrInFiles;i++) {
                String localeName = inFiles[i].getName();
                int dot = localeName.indexOf('.');
                if(dot !=  -1) {
                    localeName = localeName.substring(0,dot);
                    if(i != 0) {
                        ctx.println("<a href=\"" + baseContext.url() 
                            + "&" + "_=" + localeName + "\">" +
                            new ULocale(localeName).getDisplayName() +
                            " - <tt>" + localeName + "</tt>" +                                 
                            "</a> ");
                        ctx.println("<br/> ");
                    }
                }
            }
            
            ctx.println("<br/>");
        } else {
            ctx.setLocale(new ULocale(locale));
            if(ctx.doc[0] != null) {                
                //ctx.println("Parsed. Huh. " + doc.toString() + "<hr>(");
                //LDMLUtilities.printDOMTree(doc,out);
                //ctx.println(")<ul>");
                /// dumpIt(out, doc, 0);
                //ctx.println("</ul>");
                showLocale(ctx);
            } else {
                ctx.println("<i>err, couldn't fetch " + ctx.locale.toString() + "</i><br/>");
            }
        }
        ctx.println("</body>");
        ctx.println("</html>");
     //   out.close();
    }
    
    // cache of documents
    static Hashtable docTable = new Hashtable();
    static Hashtable docVersions = new Hashtable();
    
    public static Document fetchDoc( String locale) {
     Document doc = null;
     doc = (Document)docTable.get(locale);
     if(doc!=null) {
        return doc;
      } else {
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
        doc= LDMLUtilities.getFullyResolvedLDML(fileBase, locale, 
                                            false, false, 
                                                false);
        } else {
            doc = LDMLUtilities.parse(fileName, false);
        }
        if(doc != null) {
            // add to cache
            docTable.put(locale, doc);
        }
        return doc;
    }
    
    public static String xMAIN = "main";
    public static String xLANG = "languages";
    public static String xSCRP = "scripts";
    public static String xREGN = "territories";
    public static String xVARI = "variants";
    
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
        String email = ctx.field("email");
        String name = ctx.field("name");
        
        UserRegistry.User u = reg.get(email);
        if(u != null) {
            ctx.println("User exists!   " + u.real + " <" + u.email + ">  (" + u.sponsor + ") -  ID: " + u.id + "<br/>");
            if(ctx.field("resend").length()>0) {
                notifyUser(ctx, u);
            } else {
                WebContext my = new WebContext(ctx);
                my.addQuery("vap",vap);
                my.addQuery("email",email);
                my.addQuery("resend","y");
                ctx.println("<a href=\"" + my.url() + "\">Resend their password email</a>");
            }
            return;
        }

        if( (sponsor.length()<=0) ||
            (email.length()<=0) ||
            (name.length()<=0) ) {
            ctx.println("One or more of the Sponsor, Email, Name fields aren't filled out.  Please hit Back and try again.");
            return;
        }
        
        u = reg.add(email, sponsor, name);
        
        notifyUser(ctx, u);
    }
   
    void notifyUser(WebContext ctx, UserRegistry.User u) {
        String body = "Someone at " + u.sponsor + " has created a CLDR vetting account for you.\n" +
            "To access it, visit: \n" +
            "   http://" + ctx.serverName() + ctx.base() + "?uid=" + u.id + "&email=" + u.email + "\n" +
            "\n" +
            " Please keep this link to yourself. Thanks.\n" +
            " \n";
        ctx.println("<hr/><pre>" + body + "</pre><hr/>");
    
        MailSender.sendMail(u.email, "CLDR Registration for " + u.email,
            body);
        /* some debugging. */
         String from = System.getProperty("CLDR_FROM");
         String smtp = System.getProperty("CLDR_SMTP");
        ctx.println("Mail sent to " + u.email + " from " + from + " via " + smtp + "<br/>\n");
    }

    public void doSubmit(WebContext ctx)
    {
        if((ctx.session.user) == null) {   
            ctx.println("No Vetting Account found... please see this help link: ");
            ctx.printHelpLink("/NoUser");
            ctx.println("<br/>");
        }
        UserRegistry.User u = ctx.session.user;
        ctx.println("<hr/>");
        ctx.println("You:  " + u.real + " &lt;" + u.email + "&gt;<br/>");
        ctx.println("Your sponsor: " + u.sponsor);
        
        Hashtable lh = ctx.session.getLocales();
        Enumeration e = lh.keys();
        if(e.hasMoreElements()) { 
            for(;e.hasMoreElements();) {
                String k = e.nextElement().toString();
                ctx.println("<hr/>");
                ctx.println("<H3><a href=\"" + ctx.base() + "&_=" + k + "\">" + 
                        new ULocale(k).getDisplayName() + "</a></h3>");
                CLDRFile f = createCLDRFile(ctx, k, (Hashtable)lh.get(k));
                
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                f.write(pw);
                String asString = sw.toString();
                String asHtml = BagFormatter.toHTML.transliterate(asString);
                ctx.println("<pre>" + asHtml + "</pre>");
            }
        }
    }
    private void appendCodeList(WebContext ctx, CLDRFile file, String xpath, String subtype, Hashtable data) {
        if(data == null) {
            return;
        }
        for(Enumeration e = data.keys();e.hasMoreElements();) {
            String k = e.nextElement().toString();
            if(k.endsWith(SUBVETTING)) { // we ONLY care about SUBVETTING items. (
                String key = k.substring(0,k.length()-SUBVETTING.length());
                String vet = (String)data.get(k);
                // Now, what's happening? 
                NodeSet.NodeSetEntry nse = (NodeSet.NodeSetEntry)data.get(key);
                String newxpath = xpath + "/" + subtype + "[@type='" + key + "']";
                newxpath=newxpath.substring(1); // remove initial /     
                if(vet.equals(DRAFT)) {
                    file.add(newxpath, newxpath, LDMLUtilities.getNodeValue(nse.main));
                } else if(vet.equals(PROPOSED)) {
                    file.add(newxpath, newxpath, LDMLUtilities.getNodeValue(nse.proposed));
                } else if(vet.equals(NEW)) {
                    String newString = (String)data.get(key + SUBNEW);
                    file.add(newxpath, newxpath, newString);
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
        } /* else if(subtype.endsWith("s")) {
            subtype = subtype.substring(0,subtype.length()-1);
        }
        */
        return subtype;
    }
    private void appendLocaleCodes(WebContext ctx, CLDRFile file, String type, Hashtable data) {
        String xpath = LOCALEDISPLAYNAMES + type;
        Hashtable items = (Hashtable)data.get(xpath);
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
                                "IP: " + /* ctx. */WebContext.userIP() + "\n" + 
                                "Locale: " + locale +"\n" +
                                "CVS Version: " + cvsVer + "\n"
                                );
                                
        if(data == null) {
            file.appendFinalComment("No data.");
            return file;
        }

        appendLocaleCodes(ctx, file, xLANG, data);
        appendLocaleCodes(ctx, file, xSCRP, data);
        appendLocaleCodes(ctx, file, xREGN, data);
        appendLocaleCodes(ctx, file, xVARI, data);

        file.appendFinalComment("That's it.");
        return file;
    }

    public void showLocale(WebContext ctx)
    {
        int i;
        ctx.println("<br/>");
        ctx.println("<font size=+1><b>" + ctx.locale + "</b></font> " + ctx.locale.getDisplayName() + "<br/>");
        ctx.println("<i>Switch to: </i> " );
        for(i=1;i<ctx.docLocale.length;i++) {
            ctx.println("<a href=\"" + ctx.url() + "&_=" + ctx.docLocale[i] + "\">" + ctx.docLocale[i] + "</a> ");
        }
        ctx.println("<a href=\"" + ctx.url() + "\">" + "Other..." + "</a><br/>");
        ctx.println("<hr/>");
        
        String which = ctx.field("x");
        if((which == null) ||
            which.equals("")) {
            which = xMAIN;
        }
        WebContext subCtx = new WebContext(ctx);
        subCtx.addQuery("_",ctx.locale.toString());
        printMenu(subCtx, which, xMAIN);
        printMenu(subCtx, which, xLANG);
        printMenu(subCtx, which, xSCRP);
        printMenu(subCtx, which, xREGN);
        printMenu(subCtx, which, xVARI);
        
        ctx.println("<br/>");
        subCtx.addQuery("x",which);
        final ULocale inLocale = new ULocale("en_US");
        StandardCodes standardCodes = StandardCodes.make();
        if(xLANG.equals(which)) {
            NodeSet.NodeSetTexter texter = new NodeSet.NodeSetTexter() { 
                    public String text(NodeSet.NodeSetEntry e) {
                        return new ULocale(e.type).getDisplayLanguage(inLocale);
                    }
            };
            doLocaleCodeList(subCtx, LOCALEDISPLAYNAMES + which, texter,
                standardCodes.getAvailableCodes("language"));
        } else if(xSCRP.equals(which)) {
            NodeSet.NodeSetTexter texter = new NodeSet.NodeSetTexter() { 
                    public String text(NodeSet.NodeSetEntry e) {
                        return new ULocale("_"+e.type).getDisplayScript(inLocale);
                    }
            };
            doLocaleCodeList(subCtx, LOCALEDISPLAYNAMES + which, texter,
                standardCodes.getAvailableCodes("script"));
        } else if(xREGN.equals(which)) {
            NodeSet.NodeSetTexter texter = new NodeSet.NodeSetTexter() { 
                    public String text(NodeSet.NodeSetEntry e) {
                        return new ULocale("_"+e.type).getDisplayCountry(inLocale);
                    }
            };
            doLocaleCodeList(subCtx,LOCALEDISPLAYNAMES + which, texter,
                standardCodes.getAvailableCodes("region"));
        } else if(xVARI.equals(which)) {
             NodeSet.NodeSetTexter texter = new NodeSet.NodeSetTexter() { 
                    public String text(NodeSet.NodeSetEntry e) {
                        return new ULocale("__"+e.type).getDisplayVariant(inLocale);
                    }
            };
           doLocaleCodeList(subCtx, LOCALEDISPLAYNAMES + which, texter,
                null);  // no default variant list
        } else {
            doMain(subCtx);
        }
    }
 
    public void doMain(WebContext ctx) {
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
        Map sortedMap = mySet.getSorted(new DraftFirstTexter(tx));
        Hashtable changes = (Hashtable)ctx.getByLocale(xpath);
        
        
        // prepare a new hashtable
        if(changes==null) { 
            changes = new Hashtable(); 
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
        
        
        // Form: 
        ctx.println("<form method=POST action='" + ctx.base() + "'>");
        ctx.printUrlAsHiddenFields();
        ctx.println("<table style='border-collapse: collapse' border=1>");
        ctx.println("<input type=submit>");
        ctx.println("<tr>");
        ctx.println(" <th class='heading' bgcolor='#DDDDDD' >Name<br/><tt>Code</tt></th>");
        ctx.println(" <th class='heading' bgcolor='#DDDDDD' colspan=2>Contents</th>");
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
                checked = CURRENT;
            }
            
            String formChecked = ctx.field(xpath + "/" + type);
            String formNew = ctx.field(xpath + "/" + type + SUBNEW );
            
            if((formChecked != null) && (formChecked.length()>0)) {   
                if(!checked.equals(formChecked)) {
                    checked = formChecked;
                    if(checked.equals(CURRENT)) {
                        changes.remove(type + SUBVETTING); // remove 'current' 
                    } else {
                        changes.put(type + SUBVETTING, checked); // set
                        changes.put(type, f);
                    }
                }
            }
            
            if(formNew.length()>0) {
                changes.put(type + SUBNEW, formNew);
                changes.put(type, f);
                newString = formNew;
            } else if((newString !=null) && (newString.length()>0)) {
                changes.remove(type + SUBNEW);
                newString = null;
            }
            
            ctx.println("<tr>");
            // 1. name/code
            ctx.println("<th class='code' rowspan=" + nRows + ">" + tx.text(f) + "<br/><tt>" + type + "</tt>");
            ctx.println(" ( " + nRows + " rows )");
            ctx.println("</th>");
            
            // Now there are a pair of columns for each of the following. 
            // 2. fallback
            if(mainFallback != null) {
                ctx.println("<td align=right class='fallback'>");
                ctx.println("<i>from " + new ULocale(mainFallback).getDisplayName(new ULocale("en_US")) + "</i>");
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
                ctx.println("<i>missing</i>");
                writeRadio(ctx,xpath,type,CURRENT,checked);
                ctx.println("</td>");
                ctx.println("<td class='missing'>");
                // nothing
                ctx.println("</td>");
            }
            ctx.println("</tr>");
            
            // Draft item
            if(mainDraft > 0) {
                ctx.println("<tr>");
                ctx.println("<td align=right class='draft'>");
                ctx.println("<i>draft</i>");
                writeRadio(ctx,xpath,type,DRAFT,checked);
                ctx.println("</td>");
                ctx.println("<td class='draft'>");
                ctx.println(main);
                ctx.println("</td>");
                ctx.println("</tr>");
            }

            // Proposed item
            if(prop != null) {
                ctx.println("<tr>");
                ctx.println("<td align=right class='proposed'>");
                ctx.println("<i>proposed</i>");
                writeRadio(ctx,xpath,type,PROPOSED,checked);
                ctx.println("</td>");
                ctx.println("<td class='proposed'>");
                ctx.println(prop);
                ctx.println("</td>");
                ctx.println("</tr>");
            }
            
            
            // edit text
            ctx.println("<tr>");
            ctx.println("<td align=right class='new'>");
            ctx.println("<i>other:</i>");
            writeRadio(ctx,xpath,type,NEW,checked);
            ctx.println("</td>");
            ctx.println("<td class='new'>");
            String change = "";
            if(changes != null) {
           //     change = (String)changes.get(type + "//n");
            }
            ctx.println("<input size=50 value=\"" + 
                  (  (newString!=null) ? newString : "" )
                    + "\" name=\"" + xpath +"/" + type + SUBNEW + "\">");
            ctx.println("</td>");
            ctx.println("</tr>");
            

            ctx.println("<tr class='sep'><td class='sep' colspan=3 bgcolor=\"#EEEEDD\"><font size=-8>&nbsp;</font></td></tr>");

            // -----
            
            if(dispCount >= CODES_PER_PAGE) {
                break;
            }
        }
        ctx.println("</table>");
        ctx.println("<input type=submit>");
        ctx.println("</form>");
        if(nextSkip >= 0) {
                ctx.println("<a href=\"" + ctx.url() + 
                        "&skip=" + new Integer(nextSkip) + "\">" +
                        "More..." +
                        "</a> ");
        }
        
        if((changes!=null) && (!changes.isEmpty())) { 
            ctx.println("Putting in locale: " + xpath + ", changes<br/>");
            ctx.putByLocale(xpath,changes); 
        }  else {
            ctx.println("no changes..<br/>"); 
        }

    }
    
    public static void main (String args[]) {
        int status = 0;
        SurveyMain m = new SurveyMain();
        if(!m.reg.read()) {
            System.err.println("Couldn't load user registry - exiting.");
            System.exit(1);
        }
        String cldrLoad = System.getProperty("CLDR_LOAD_ALL");
        if((cldrLoad != null) && cldrLoad.equals("y")) {
            m.loadAll();
        }
        while(new FCGIInterface().FCGIaccept()>= 0) {
            System.setErr(System.out);
            try {
             m.go(System.out);
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
    
    static int qqq = 0;
    static final String foo[] = { "Jellyfish", "Sucralose", "Henry F. Talbot", 
                            "Rio Grande", "Zirconium", "Natto", "The Taj Mahal" };
    static String getSillyText() { 
        qqq++;
        return foo[qqq%foo.length];
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

    void writeRadio(WebContext ctx,String xpath,String type,String value,String checked) {
        writeRadio(ctx, xpath, type, value, checked.equals(value));        
    }

    void writeRadio(WebContext ctx,String xpath,String type,String value,boolean checked) {
        ctx.println("<input type=radio name='" + xpath + "/" + type + "' value='" + value + "' " +
            (checked?" CHECKED ":"") + "/>");
    }



}
