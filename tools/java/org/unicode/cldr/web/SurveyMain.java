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

import org.unicode.cldr.util.*;
import org.unicode.cldr.icu.*;


import com.fastcgi.FCGIInterface;
import com.fastcgi.FCGIGlobalDefs;
import com.ibm.icu.lang.UCharacter;
import org.html.*;
import org.html.utility.*;
import org.html.table.*;

class SurveyMain {

    static String fileBase = System.getProperty("CLDR_COMMON") + "/main";

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
        
        // get the context
        
        CookieSession mySession = null;
        String myNum = ctx.field("s");
        if(myNum != null) {
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
        WebContext baseContext = new WebContext(ctx);
        ctx.println("<i>using session " + mySession.id + " </i><br/>");
        
        // print 'shopping cart'
        {
            Hashtable lh = ctx.session.getLocales();
            Enumeration e = lh.keys();
            if(e.hasMoreElements()) { 
                ctx.println("<B>Changed locales: </B> ");
            }
            for(;e.hasMoreElements();) {
                String k = e.nextElement().toString();
                ctx.println("<a href=\"" + baseContext.url() + "&_=" + k + "\">" + 
                        new ULocale(k).getDisplayName() + "</a> ");
            }
            ctx.println("<br/>");
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
            File inFiles[] = baseDir.listFiles(myFilter);
            int nrInFiles = inFiles.length;
            String doMajor = ctx.field("MXM");
            if((doMajor!=null)&&(doMajor.length()<=0)) {
                doMajor = null;
            }
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
                        if(doMajor != null) {
                            fetchDoc(ctx, localeName);
                        }
                    }
                }
            }
            
            ctx.println("<br/>");
        
        
            ctx.println("<form action='/II/surv.sh' method=GET>Locale: <input name='_'><br/>" +
                    "Resolve? [enter 'Y'] <input name='res'><br/>" +
                    "<input type=submit></form>");
        } else {
            ctx.locale = new ULocale(locale);
            ctx.println("<b>" + ctx.locale + "</b><br/>\n");
            Document doc = fetchDoc(ctx,ctx.locale.toString());
            if(doc != null) {                
                //ctx.println("Parsed. Huh. " + doc.toString() + "<hr>(");
                //LDMLUtilities.printDOMTree(doc,out);
                //ctx.println(")<ul>");
                /// dumpIt(out, doc, 0);
                //ctx.println("</ul>");
                ctx.doc = doc;
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
    Hashtable docTable = new Hashtable();
    
    protected Document fetchDoc(WebContext ctx, String locale) {
     Document doc = null;
     doc = (Document)docTable.get(locale);
     if(doc!=null) {
         ctx.println("<i>cached..</i>");
        return doc;
      } else {
        ctx.println("Not cached. " + locale + "<br/>");
    }
      try {
         String fileName = fileBase + File.separator + locale + ".xml";
         File f = new File(fileName);
         boolean ex  = f.exists();
         boolean cr  = f.canRead();
         ctx.println(f.toString() + " : " + ((ex)?"exists":"NOEXISTS") + ", " + ((cr)?"read":"NOREAD"));
         String res  = null; /* request.getParameter("res"); */ /* ALWAYS resolve */
         if((res!=null)&&(res.length()>0)) {
         ctx.println("<i>INFO: Resolving " + fileName + "</i><br/>");
            doc= LDMLUtilities.getFullyResolvedLDML(fileBase, locale, 
                                                false, false, 
                                                    false);
            } else {
                 ctx.println("<i>INFO: Parsing " + fileName + "</i><br/>");
                //public static Document parseAndResolveAliases(String locale, String sourceDir, boolean ignoreError){
                doc = LDMLUtilities.parse(fileName, false);
                //doc = LDMLUtilities.parseAndResolveAliases(locale + ".xml",
                //    fileBase, false);
            }
        } catch(Throwable t) {
            ctx.println("<B>err</B>: " + t.toString());
            doc = null;
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

    public void showLocale(WebContext ctx)
    {
        ctx.println("<a href=\"" + ctx.url() + "\">" + ctx.locale + "  [ click to change locale]</a><br/>");
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
        if(xLANG.equals(which)) {
            NodeSet.NodeSetTexter texter = new NodeSet.NodeSetTexter() { 
                    public String text(NodeSet.NodeSetEntry e) {
                        return new ULocale(e.type).getDisplayLanguage(inLocale);
                    }
            };
            doSimpleCodeList(subCtx, "//ldml/localeDisplayNames/" + which, texter);
        } else if(xSCRP.equals(which)) {
            NodeSet.NodeSetTexter texter = new NodeSet.NodeSetTexter() { 
                    public String text(NodeSet.NodeSetEntry e) {
                        return new ULocale("_"+e.type).getDisplayScript(inLocale);
                    }
            };
            doSimpleCodeList(subCtx, "//ldml/localeDisplayNames/" + which, texter);
        } else if(xREGN.equals(which)) {
            NodeSet.NodeSetTexter texter = new NodeSet.NodeSetTexter() { 
                    public String text(NodeSet.NodeSetEntry e) {
                        return new ULocale("_"+e.type).getDisplayCountry(inLocale);
                    }
            };
            doSimpleCodeList(subCtx, "//ldml/localeDisplayNames/" + which, texter);
        } else if(xVARI.equals(which)) {
             NodeSet.NodeSetTexter texter = new NodeSet.NodeSetTexter() { 
                    public String text(NodeSet.NodeSetEntry e) {
                        return new ULocale("__"+e.type).getDisplayVariant(inLocale);
                    }
            };
           doSimpleCodeList(subCtx, "//ldml/localeDisplayNames/" + which, texter);
        } else {
            doMain(subCtx);
        }
    }
 
    public void doMain(WebContext ctx) {
    }

    public void doSimpleCodeList(WebContext ctx, String xpath, NodeSet.NodeSetTexter tx) {
        Node root = LDMLUtilities.getNode(ctx.doc, xpath);
        if(root == null) {
            ctx.println("Err, can't load xpath " + xpath);
            return;
        }
        final int CODES_PER_PAGE = 20;
        int count = 0;
        int dispCount = 0;
        int total = 0;
        int skip = 0;
        NodeSet mySet = NodeSet.loadFromRoot(root);
        total = mySet.count();
        Map sortedMap = mySet.getSorted(new DraftFirstTexter(tx));
        Hashtable changes = (Hashtable)ctx.getByLocale(xpath);
        
        
        /* SRL TEST */ if(changes==null) { ctx.println("Getting ready a new hash..<br/>");changes = new Hashtable(); }
       //         else { ctx.println (" <blockquote>old hash: " + changes.toString() + "</blockquote>\n"); }
        
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

        int from = skip+1;
        int to = from + CODES_PER_PAGE-1;
        if(to >= total) {
            to = total;
        }
        
        ctx.println("Displaying items " + from + " to " + to + " of " + total + "<br/>");
        
        ctx.println("<table border=1>");

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
            boolean mainDraft = false;
            String prop = null;
            if(f.main != null) {
                main = LDMLUtilities.getNodeValue(f.main);
                mainDraft = LDMLUtilities.isNodeDraft(f.main);
            }
            if(f.proposed != null) {
                prop = LDMLUtilities.getNodeValue(f.proposed);
            }

            ctx.println("<tr><th align=left rowspan=3>" + tx.text(f) + "<br/><tt>" + type + "</tt>");
            ctx.println("</th>");
            ctx.println("<td bgcolor=\"" + (mainDraft?"#DDDDDD":"#FFFFFF") + "\">");
            if(main != null) {
                ctx.println(main);
            }
            ctx.println("</td>");
            ctx.println("<td bgcolor=\"" + (mainDraft?"#DDDDDD":"#FFFFFF") + "\">");
            if(mainDraft) {
                ctx.println("<i>draft</i>");
                ctx.println("<input type=checkbox " +
                  (  ((changes!=null)&&(changes.get(type+"//d")!=null)) ?
                            " CHECKED " : "" )
                    + " name=\"" + xpath +"/" + type + "//d" + "\"> <br/>");
                /* SRL TEST */ if(changes!=null) { changes.put(type + "//d","X"); } 
            }
            ctx.println("</td>");

            ctx.println("</tr>");
            
            ctx.println("<tr>");
            if(prop != null) {
                ctx.println("<td bgcolor=\"" + ((prop!=null)?"#DDFFDD":"#FFFFFF") + "\">");
                ctx.println(prop);
            ctx.println("</td>");
            }
            if(prop != null) {
                ctx.println("<td bgcolor=\"" + ((prop!=null)?"#DDFFDD":"#FFFFFF") + "\">");
                ctx.println("<i>proposed</i>");
                ctx.println("<input type=checkbox " + 
                  (  ((changes!=null)&&(changes.get(type+"//p")!=null)) ?
                            " CHECKED " : "" )
                    + " name=\"" + xpath +"/" + type + "//p" + "\"> <br/>");
                ctx.println("</td>");
                /* SRL TEST */ if(changes!=null) { changes.put(type + "//p","X"); } 
            }
            ctx.println("</tr>");
            
            ctx.println("<tr>");
            ctx.println("<td bgcolor=\"#FFFFFF\" colspan=2 rowspan=\"" + ((prop!=null)?"1":"1") + "\">");
            String change = "";
            if(changes != null) {
                change = (String)changes.get(type + "//n");
            }
            ctx.println("<input size=50 value=\"" + 
                  (  (change!=null) ? change : "" )
                    + "\" name=\"" + xpath +"/" + type + "//n" + "\">");
            /* SRL TEST */ if((changes!=null)&&(change==null)) { changes.put(type + "//n",getSillyText()); } 
            ctx.println("</td>");            
            ctx.println("</tr>");
            ctx.println("<tr><td colspan=4 bgcolor=\"#EEEEDD\"><font size=-8>&nbsp;</font></td></tr>");

            // -----
            
            if(dispCount >= CODES_PER_PAGE) {
                break;
            }
        }
        ctx.println("</table>");
        if(dispCount >= CODES_PER_PAGE) {
                ctx.println("<a href=\"" + ctx.url() + 
                        "&skip=" + new Integer(count) + "\">" +
                        "More..." +
                        "</a> ");
        }
        
    /* SRL TEST */ if(changes!=null) { ctx.println("Putting in locale: " + xpath + ", changes<br/>");
                    ctx.putByLocale(xpath,changes); }  else {ctx.println("Changes = null..<br/>"); }

    }
    
    public static void main (String args[]) {
        int status = 0;
        SurveyMain m = new SurveyMain();
        while(new FCGIInterface().FCGIaccept()>= 0) {
            try {
             m.go(System.out);
            } catch(Throwable t) {
                System.out.println("Content-type: text/html\n\n\n");
                System.out.flush();
                System.out.println("<B>err</B>: " + t.toString());
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
}
