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
import com.ibm.icu.util.ULocale;

// servlet imports
import javax.servlet.*;
import javax.servlet.http.*;


// sql imports
import java.sql.Connection;

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
    public com.ibm.icu.dev.test.util.ElapsedTimer reqTimer = null;
    
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
    
    boolean prefBool(String x)
    {
        boolean ret = fieldBool(x, session.prefGetBool(x));
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
            outQuery = outQuery + "&" + k + "=" + v;
        }
    }
    
    void addQuery(String k, boolean v) {
        addQuery(k,v?"t":"f");
    }

    String url() {
        if(outQuery == null) {
            return base();
        } else {
            return base() + "?" + outQuery;
        }
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
        return context(s)+"?a=" + base() + "&" + outQuery;
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
    
// print api
    final void println(String s) {
        out.println(s);
    }
    
    final void print(String s) {
        out.print(s);
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
        
            logger.info("NOT NOT NOT fetching locale: " + l.toString() + ", count: " + doc.length);
        }
    }
        
// locale hash api
    // get the hashtable of modified locales

    Hashtable localeHash = null;
    public final Hashtable getLocaleHash() {
		return getLocaleHash(locale.toString());
	}

    public Hashtable getLocaleHash(String aLocale) {
        if(session == null) {
            throw new RuntimeException("Session is null in WebContext.getLocaleHash()!");
        }
        if(localeHash == null) {
            synchronized(session) {
                Hashtable localesHash = session.getLocales();
                if(localesHash == null) {
                    return null;
                }
                localeHash = (Hashtable)localesHash.get(aLocale);
            }
        }
        return localeHash;
    }
    
    public final Object getByLocale(String key) {
		return getByLocale(key, locale.toString());
	}
	
    public Object getByLocale(String key, String aLocale) {
        Hashtable localeHash = getLocaleHash(aLocale);
        if(localeHash != null) {
            return localeHash.get(key);
        }
        return null;
    }

    public void putByLocale(String key, Object value) {
        synchronized(session) {
            Hashtable localeHash = getLocaleHash();
            if(localeHash == null) {
                localeHash = new Hashtable();
                session.getLocales().put(locale,localeHash);
            }
            localeHash.put(key, value);
        }
    }
    
    public void removeByLocale(String key) {
        synchronized(session) {
            Hashtable localeHash = getLocaleHash();
            if(localeHash != null) {
                localeHash.remove(key);
            }
        }
    }
    
// DataPod functions
    private static final String DATA_POD = "DataPod_";
    DataPod getPod(String prefix) {
        synchronized(this) {
//            logger.info("Get POD: " + prefix);
            DataPod pod = (DataPod)getByLocale(DATA_POD+prefix);
            if((pod != null) && (!pod.isValid(sm.lcr))) {
//                logger.info("expired POD: " +  pod.toString());
                pod = null;
                println("<i>Note: some data has changed, reloading..</i><br/>");
            }
            if(pod == null) {
//                logger.info("remaking POD: " + prefix);
                pod = DataPod.make(this, locale.toString(), prefix, true);
//                logger.info("registering POD: " +  pod.toString());
                pod.register(sm.lcr);
//                logger.info("putting POD: " +  pod.toString());
                putByLocale(DATA_POD+prefix, pod);
            }
//            logger.info("returning POD: " +  pod.toString());
            return pod;
        }
    }

// Internal Utils

    // from BagFormatter
    private static PrintWriter openUTF8Writer(OutputStream out) throws IOException {
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
        println("(<a href=\"" + SurveyMain.CLDR_HELP_LINK + what + "\">" + title +"</a>)");
    }
}
