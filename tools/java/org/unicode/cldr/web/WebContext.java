//
//  WebContext.java
//  cldrtools
//
//  Created by Steven R. Loomis on 3/11/2005.
//  Copyright 2005 IBM. All rights reserved.
//
package org.unicode.cldr.web;

import org.w3c.dom.Document;
import java.io.*;
import java.util.*;
import org.unicode.cldr.util.*;
import com.ibm.icu.util.ULocale;

// servlet imports
import javax.servlet.*;
import javax.servlet.http.*;


// sql imports
import java.sql.Connection;

import com.ibm.icu.dev.test.util.ElapsedTimer;

public class WebContext {
    public static java.util.logging.Logger logger = SurveyMain.logger;
// USER fields
    public SurveyMain sm = null;
    public CLDRDBSource dbsrc = null;
    public Document doc[]= new Document[0];
    public ULocale locale = null;
    public String docLocale[] = new String[0];
    public String localeName = null; 
    public CookieSession session = null;
    public Connection conn = null;
    public ElapsedTimer reqTimer = null;
    
// private fields
    protected PrintWriter out = null;
    String outQuery = null;
    TreeMap outQueryMap = new TreeMap();
    boolean dontCloseMe = false;

    HttpServletRequest request;
    HttpServletResponse response;
    
    // New constructor
    public WebContext(HttpServletRequest irq, HttpServletResponse irs) throws IOException {
         request = irq;
         response = irs;
        out = response.getWriter();
        dontCloseMe = true;
        
    }
    
    public WebContext(boolean fake)throws IOException  {
        dontCloseMe=false;
        out=openUTF8Writer(System.err);
    }
    
    // copy c'tor
    public WebContext( WebContext other) {
        doc = other.doc;
        docLocale = other.docLocale;
        out = other.out;
        outQuery = other.outQuery;
        locale = other.locale;
        localeName = other.localeName;
        session = other.session;
        outQueryMap = (TreeMap)other.outQueryMap.clone();
        dontCloseMe = true;
        request = other.request;
        response = other.response;
        conn = other.conn;
        dbsrc = other.dbsrc;
        sm = other.sm;
    }
    
// More API

    /* 
     * return true if field y or n.
     * given default passed in def
     */
     
    boolean fieldBool(String x, boolean def) {
        if(field(x).length()>0) {
            if(field(x).charAt(0)=='t') {
                return true;
            } else {
                return false;
            }
        } else {
            return def;
        }
    }

    int fieldInt(String x, int def) {
        String f;
        if((f=field(x)).length()>0) {
            try {
                return new Integer(f).intValue();
            } catch(Throwable t) {
                return def;
            }
        } else {
            return def;
        }
    }
    
    boolean prefBool(String x) {
        return prefBool(x,false);
    }
    boolean prefBool(String x, boolean defVal)
    {
        boolean ret = fieldBool(x, session.prefGetBool(x, defVal));
        session.prefPut(x, ret);
        return ret;
    }
    
    /**
     * get a pref that is a string, 
     * @param x the field name and pref name
     */
    String pref(String x) {
        return pref(x, ""); // should be null?
    }
    
    String pref(String x, String def)
    {
        String ret = field(x, session.prefGet(x));
        if(ret != null) {
            session.prefPut(x, ret);
        }
        if((ret == null) || (ret.length()<=0)) {
            ret = def;
        }
        return ret;
    }
    
    public final String field(String x) {
        return field(x, "");
    }
    public String field(String x, String def) {
        String res = request.getParameter(x);
         if(res == null) {       
//              System.err.println("[[ empty query string: " + x + "]]");
            return def;    // don't try to transcode null.
        }
        
        byte asBytes[] = new byte[res.length()];
        boolean wasHigh = false;
        int n;
        for(n=0;n<res.length();n++) {
            asBytes[n] = (byte)(res.charAt(n)&0x00FF);
            //println(" n : " + (int)asBytes[n] + " .. ");
            if(asBytes[n]<0) {
                wasHigh = true;
            }
        }
        if(wasHigh == false) {
            return res; // no utf-8
        } else {
            //println("[ trying to decode on: " + res + "]");
        }
        try {
            res = new String(asBytes, "UTF-8");
        } catch(Throwable t) {
            return res;
        }
        
        return res;
    }
// query api
    void addQuery(String k, String v) {
        outQueryMap.put(k,v);
        if(outQuery == null) {
            outQuery = k + "=" + v;
        } else {
            outQuery = outQuery + "&amp;" + k + "=" + v;
        }
    }
    
    void addQuery(String k, boolean v) {
        addQuery(k,v?"t":"f");
    }
    
    void setQuery(String k, String v) {
        if(outQueryMap.get(k)==null) { // if it wasn't there..
            addQuery(k,v); // then do a simple append
        } else {
            // rebuild query string:
            outQuery=null;
            TreeMap oldMap = outQueryMap;
            oldMap.put(k,v); // replace
            outQueryMap=new TreeMap();
            for(Iterator i=oldMap.keySet().iterator();i.hasNext();) {
                String somek = (String)i.next();
                addQuery(somek,(String)oldMap.get(somek));
            }
        }
    }

    String url() {
        if(outQuery == null) {
            return base();
        } else {
            return base() + "?" + outQuery;
        }
    }

    final String urlConnector() {
        return (url().indexOf('?')!=-1)?"&amp;":"?";
    }
    
    String base() { 
        return request.getContextPath() + request.getServletPath();
    }
    
    public String context() { 
        return request.getContextPath();
    }

    public String context(String s) { 
        return request.getContextPath() + "/" + s;
    }
    
    public String jspLink(String s) {
        return context(s)+"?a=" + base() +
            ((outQuery!=null)?("&amp;" + outQuery):
                ((session!=null)?("&amp;s="+session.id):"")
            );
     }
    
    void printUrlAsHiddenFields() {
        for(Iterator e = outQueryMap.keySet().iterator();e.hasNext();) {
            String k = e.next().toString();
            String v = outQueryMap.get(k).toString();
            println("<input type='hidden' name='" + k + "' value='" + v + "'/>");
        }
    }
    
    /**
     * return the IP of the remote user.
     */
    String userIP() {
        return request.getRemoteAddr();
    }

    /**
     * return the hostname of the web server
     */
    String serverName() {
        return request.getServerName();
    }
    
    String serverHostport() {
        int port = request.getServerPort();
        if(port == 80) {
            return serverName();
        } else {
            return serverName() + ":"+port;
        }
    }
    String schemeHostPort() {
        return request.getScheme()+"://" + serverHostport();
    }
    
// print api
    final void println(String s) {
        out.println(s);
    }
    
    final void print(String s) {
        out.print(s);
    }
    
    void print(Throwable t) {
        print("<pre style='border: 2px dashed red; margin: 1em; padding: 1'>" +                        
                t.toString() + "<br />");
        StringWriter asString = new StringWriter();
        t.printStackTrace(new PrintWriter(asString));
        print(asString.toString());
        print("</pre>");
    }
    
    void close() {
        if(!dontCloseMe) {
            out.close();
            out = null;
        } else {
            // ? 
        }
    }
// doc api
    public static String getParent(Object l) {
        String locale = l.toString();
        int pos = locale.lastIndexOf('_');
        if (pos >= 0) {
            return locale.substring(0,pos);
        }
        if (!locale.equals("root")) return "root";
        return null;
    }

    void setLocale(ULocale l) {
        locale = l;
        String parents = null;
        Vector localesVector = new Vector();
        Vector docsVector = new Vector();
        parents = l.toString();
        if(false) { // TODO: change
            do {
                try {
                    Document d = sm.fetchDoc(parents);
                    localesVector.add(parents);
                    docsVector.add(d);
                } catch(Throwable t) {
                    logger.log(java.util.logging.Level.SEVERE,"Error fetching " + parents + "<br/>",t);
                    // error is shown elsewhere.
                }
                parents = getParent(parents);
            } while(parents != null);
            doc = (Document[])docsVector.toArray(doc);
            docLocale = (String[])localesVector.toArray(docLocale);
            logger.info("Fetched locale: " + l.toString() + ", count: " + doc.length);
        } else {
            // at least set up the docLocale tree
            do {
                localesVector.add(parents);
                parents = getParent(parents);
            } while(parents != null);
            docLocale = (String[])localesVector.toArray(docLocale);        
        
           // logger.info("NOT NOT NOT fetching locale: " + l.toString() + ", count: " + doc.length);
        }
    }

    // convenience.	
    public final Object getByLocale(String key, String aLocale) {
        return session.getByLocale(key,aLocale);
    }

    public final void putByLocale(String key, String locale, Object value) {
        session.putByLocale(key,locale,value);
    }
        
    
    public final void removeByLocale(String key, String aLocale) {
        session.removeByLocale(key,aLocale);
    }
    // ultra conveniences
    public final void removeByLocale(String key) {
        removeByLocale(key, locale.toString());
    }
    public final void putByLocale(String key, Object value) {
        putByLocale(key, locale.toString(), value);
    }
    public final Object getByLocale(String key) {
		return getByLocale(key, locale.toString());
	}
    
    // Static data
    static Hashtable staticStuff = new Hashtable();
    
    public final void putByLocaleStatic(String key, Object value) {
        putByLocaleStatic(key, locale, value);
    }
    public final Object getByLocaleStatic(String key) {
		return getByLocaleStatic(key, locale);
	}
    // bottlenecks for static access
    public static synchronized final Object getByLocaleStatic(String key, ULocale aLocale) {
        Hashtable subHash = (Hashtable)staticStuff.get(aLocale);
        if(subHash == null) {
            return null;
        }
        return subHash.get(key);
    }
    public static final synchronized void putByLocaleStatic(String key, ULocale locale, Object value) {
        Hashtable subHash = (Hashtable)staticStuff.get(locale);
        if(subHash == null) {
            subHash = new Hashtable();
            
            staticStuff.put(locale, subHash);
        }
        subHash.put(key,value);
    }
    
// DataPod functions
    private static final String DATA_POD = "DataPod_";
    
    /**
     * Get a pod.. even if it may be no longer invalid.
     * May be null.
     */
    DataPod getExistingPod(String prefix) {
        return getExistingPod(prefix, defaultPtype());
    }
    
    DataPod getExistingPod(String prefix, String ptype) {
        synchronized(this) {
            return (DataPod)getByLocaleStatic(DATA_POD+prefix+":"+ptype);
        }
    }
    
    static private StandardCodes sc = null;
    static private synchronized StandardCodes getSC() {
        if(sc == null) {
            sc = StandardCodes.make();
        }
        return sc;
    }
    
    public String defaultPtype() {
        String def = pref(SurveyMain.PREF_COVLEV,"default");
        if(!def.equals("default")) {
            return def;
        } else {
            String org = getChosenLocaleType();
            String ltype = getEffectiveLocaleType(org);
            return ltype;
        }
    }
    
    static synchronized String getEffectiveLocaleType(String org) {
            try {
                return  getSC().getEffectiveLocaleType(org);
            } catch (java.io.IOException ioe) {
                return org;
            }
    }
    
   static synchronized String[] getLocaleTypes() {
    try {
       return (String[])getSC().getLocaleTypes().keySet().toArray(new String[0]);
    } catch (IOException ioe) {
        return new String[0];
    }
   }
    
    private String getChosenLocaleType() {
        String org = pref(SurveyMain.PREF_COVTYP, "default");
        if(org.equals("default")) {
            org = null;
        }
        if((org==null) && 
           (session.user != null)) {
            org = session.user.org;
        }
        return org;
    }
    
    public Map getOptionsMap() {
        String def = pref(SurveyMain.PREF_COVLEV,"default");
        Map options = new HashMap();
        if(!def.equals("default")) {
            options.put("CheckCoverage.requiredLevel",def);
        } else {
            String org = getChosenLocaleType();
            if(org!=null) {
                options.put("CoverageLevel.localeType",session.user.org);
            }
        }
        return options;
    }
    /** 
     * Get a currently valid pod.. creating it if need be.
     * UI: does write informative notes to the ctx in case of a long delay.
     
     */
    DataPod getPod(String prefix) {
        return getPod(prefix, defaultPtype());
    }
    
    DataPod getPod(String prefix, String ptype) {
        String loadString = "data was loaded.";
        synchronized(this) {
            DataPod pod = getExistingPod(prefix, ptype);
            if((pod != null) && (!pod.isValid(sm.lcr))) {
                pod = null;
                loadString = "data was re-loaded due to a new user submission.";
            }
            if(pod == null) {
                long t0 = System.currentTimeMillis();
                ElapsedTimer podTimer = new ElapsedTimer("There was a delay of {0} as " + loadString);
                pod = DataPod.make(this, locale.toString(), prefix, true);
                if((System.currentTimeMillis()-t0) > 10 * 1000) {
                    println("<i><b>" + podTimer + "</b></i><br/>");
                }
                synchronized (staticStuff) {
                    pod.register(sm.lcr);
                    putByLocaleStatic(DATA_POD+prefix+":"+ptype, pod);
                }
            }
            pod.touch();
            return pod;
        }
    }

// Internal Utils

    // from BagFormatter
    public static PrintWriter openUTF8Writer(OutputStream out) throws IOException {
        return openWriter(out,"UTF-8");
    }
    
    private static PrintWriter openWriter(OutputStream out, String encoding) throws IOException {
        return new PrintWriter(
            new BufferedWriter(
                new OutputStreamWriter(
                     out,
                    encoding),
                4*1024));       
    }
    
    public void printHelpLink(String what)  {
        printHelpLink(what, "Help");
    }
    
    public void printHelpLink(String what, String title)
    {
//        boolean doEdit = UserRegistry.userIsTC(session.user);
    
        printHelpLink(what, title, true);
    }
    public static final String MOD_MSG = ("Visit help as Editable (may require login)");
    public void printHelpLink(String heresWhere, String defaultArgs, boolean areNice) {
        printHelpLink(heresWhere,defaultArgs,areNice,true);
    }
    public void printHelpLink(String what, String title, boolean doEdit, boolean parens)
    {
        if(parens) {
            print("(");
        }
        print("<a href=\"" + (SurveyMain.CLDR_HELP_LINK) + what + "\">" + title +"</a>");
//        if(doEdit) {
//            print(" <a title='"+MOD_MSG+"' href=\"" + (SurveyMain.CLDR_HELP_LINK_EDIT) + what + "\">" + modifyThing(MOD_MSG) +"</a>");
//        }
        if(parens) {
            println(")");
        }
    }
    public String modifyThing(String message) {
        return "<img border='0' style='width: 1em; height: 1em;' src='"+context("hand.gif")+"' title='"+message+"' />";
    }
}
