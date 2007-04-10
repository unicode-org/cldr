//
//  WebContext.java
//  cldrtools
//
//  Created by Steven R. Loomis on 3/11/2005.
//  Copyright 2005-2007 IBM. All rights reserved.
//
package org.unicode.cldr.web;

import org.w3c.dom.Document;
import java.io.*;
import java.util.*;
import org.unicode.cldr.util.*;
import com.ibm.icu.util.ULocale;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.ref.SoftReference;

// servlet imports
import javax.servlet.*;
import javax.servlet.http.*;


// sql imports
import java.sql.Connection;

import com.ibm.icu.dev.test.util.ElapsedTimer;

/**
 * This is the per-client context passed to basically all functions
 * it has print*() like functions, and so can be written to.
 */
public class WebContext {
    public static java.util.logging.Logger logger = SurveyMain.logger;
// USER fields
    public SurveyMain sm = null;
    public CLDRDBSource dbsrc = null;
    public Document doc[]= new Document[0];
    public ULocale locale = null;
    public String  localeString = null;
    public ULocale displayLocale = SurveyMain.BASELINE_LOCALE;
    public String docLocale[] = new String[0];
    public String localeName = null; 
    public CookieSession session = null;
    public Connection conn = null;
    public ElapsedTimer reqTimer = null;
    public Hashtable temporaryStuff = new Hashtable();
    
// private fields
    protected PrintWriter out = null;
    String outQuery = null;
    TreeMap outQueryMap = new TreeMap();
    boolean dontCloseMe = false;

    public PrintWriter getOut() { 
        return out;
    }
    HttpServletRequest request;
    HttpServletResponse response;
    
    public Map getParameterMap() { 
        return request.getParameterMap();
    }
    
    /**
     * Construct a new WebContext from the servlet request and response 
     */
    public WebContext(HttpServletRequest irq, HttpServletResponse irs) throws IOException {
         request = irq;
         response = irs;
        out = response.getWriter();
        dontCloseMe = true;
        
    }
    
    /**
     * Construct a new fake WebContext - for testing purposes.
     * Writes to stderr. 
     */
    public WebContext(boolean fake)throws IOException  {
        dontCloseMe=false;
        out=openUTF8Writer(System.err);
    }
    
    /**
     * Copy one WebContext to another.
     * useful for sub-contexts with different base urls.
     */
    public WebContext( WebContext other) {
        doc = other.doc;
        docLocale = other.docLocale;
        displayLocale = other.displayLocale;
        out = other.out;
        outQuery = other.outQuery;
        localeName = other.localeName;
        locale = other.locale;
        if(locale != null) {
            localeString = locale.getBaseName();
        }
        session = other.session;
        outQueryMap = (TreeMap)other.outQueryMap.clone();
        dontCloseMe = true;
        request = other.request;
        response = other.response;
        conn = other.conn;
        dbsrc = other.dbsrc;
        sm = other.sm;
        temporaryStuff = other.temporaryStuff;
    }
    
    /**
     * get a field's value as a boolean
     * @param x field name
     * @param def default value if field is not found.
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

    /**
     * get a field's value, or -1
     * @param x field name
     */
    final int fieldInt(String x) {
        return fieldInt(x,-1);
    }
    
    /**
     * get a field's value, or the default
     * @param x field name
     * @param def default value
     */
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
    
    /**
     * Return true if the field is present
     */
    public boolean hasField(String x) {
        return(request.getParameter(x)!=null);
    }
    
    /**
     * return a field's value, else ""
     * @param x field name
     */
    public final String field(String x) {
        return field(x, "");
    }

    /**
     * return a field's value, else def
     * @param x field name
     * @param def default value
     */
    public String field(String x, String def) {
        if(request==null) { 
            return def; // support testing
        }

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
// preference api    
    /**
     * get a preference's value as a boolean. defaults to false.
     * @param x pref name
     */
    boolean prefBool(String x) {
        return prefBool(x,false);
    }

    int prefInt(String x, int def) {
        String f;
        if((f=pref(x,"")).length()>0) {
            try {
                return new Integer(f).intValue();
            } catch(Throwable t) {
                return def;
            }
        } else {
            return def;
        }
    }
    
    int prefInt(String x) {
        return prefInt(x, -1);
    }
    
    int codesPerPage = -1;
    int prefCodesPerPage() {
        if(codesPerPage == -1) {
            codesPerPage =  prefInt(SurveyMain.PREF_CODES_PER_PAGE, SurveyMain.CODES_PER_PAGE);
            codesPerPage = Math.max(codesPerPage,5);
        } 
        return codesPerPage;
    }

    /**
     * get a preference's value as a boolean. defaults to defVal.
     * @param x preference name
     * @param defVal default value
     */
    boolean prefBool(String x, boolean defVal)
    {
        if(session == null) {
            return defVal;
        }
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
    
    /**
     * get a pref that is a string, 
     * @param x the field name and pref name
     * @param def default value
     */
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
    
// convenience - atarget
    String atarget() {
        return atarget("_blank");
    }
    
    String atarget(String target) {
        if(prefBool(SurveyMain.PREF_NOPOPUPS)) {
            return "";
        } else {
            return "target='SurveyTool:"+target+"' ";
        }
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

    void removeQuery(String k) {
        if(outQueryMap.get(k)!=null) { // if it was there..
            // rebuild query string:
            outQuery=null;
            TreeMap oldMap = outQueryMap;
            oldMap.remove(k); // replace
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

    void redirect(String where) {
        try {
            response.sendRedirect(where);
            out.close();
            close();
        } catch(IOException ioe) {
            throw new RuntimeException(ioe.toString() + " while redirecting to "+where);
        }
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

    // TODO: replace with overloads
    public static String getParent(Object locale) {
        String id;
        if(locale instanceof ULocale) {
            id = ((ULocale)locale).getBaseName();
        } else {
            id = locale.toString();
        }
        return LDMLUtilities.getParent(id);
    }
    

    void setLocale(ULocale l) {
        locale = l;
        localeString = locale.getBaseName();
        String parents = null;
        Vector localesVector = new Vector();
        parents = l.toString();
        if(false) { // TODO: change
            do {
                try {
                    localesVector.add(parents);
                } catch(Throwable t) {
                    logger.log(java.util.logging.Level.SEVERE,"Error fetching " + parents + "<br/>",t);
                    // error is shown elsewhere.
                }
                parents = getParent(parents);
            } while(parents != null);
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
    
    public final String localeString() {
        if(localeString == null) {
            throw new InternalError("localeString is null, locale="+ locale);
        }
        return localeString;
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
    
    public int staticInfo_Reference(Object o) {
        int s = 0;
        Object oo = ((Reference)o).get();
        println("Reference -&gt; <ul>");
        s += staticInfo_Object(oo);
        println("</ul>");
        return s;
    }
    
    public int staticInfo_DataPod(Object o) {
        int s = 0;
        DataPod pod = (DataPod)o;
        
        print(o.toString());
        
        return 1;
    }

    public int staticInfo_Object(Object o) {
        if(o == null) {
            println("null");
            return 0;
        } else if(o instanceof String) {
            return staticInfo_String(o);
        } else if(o instanceof Boolean) {
            return staticInfo_Boolean(o);
        } else if(o instanceof Reference) {
            return staticInfo_Reference(o);
        } else if(o instanceof Hashtable) {
            return staticInfo_Hashtable(o);
        } else if(o instanceof DataPod) {
            return staticInfo_DataPod(o);
        } else { 
            println(o.getClass().toString());
            return 1;
        }
    }
    
    public int staticInfo_Hashtable(Object o) {
        int s = 0;
        Hashtable subHash = (Hashtable)o;
        println("<ul>");
        for(Iterator ee = subHash.keySet().iterator();ee.hasNext();) {
            String kk = ee.next().toString();
            println(kk + ":");
            Object oo = subHash.get(kk);
            s += staticInfo_Object(oo);
            println("<br>");
        }
        println("</ul>");
        return s;
    }

    public int staticInfo_String(Object o) {
        String obj = (String)o;
        println("("+obj+")<br>");
        return 1;
    }

    public int staticInfo_Boolean(Object o) {
        Boolean obj = (Boolean)o;
        boolean b = (boolean)obj;
        println(obj.toString()+"<br>");
        return 1;
    }
    
    public final int staticInfo() {
        println("<h4>Static Info</h4>");
        int s = staticInfo_Object(staticStuff);
        println(staticStuff.size() + " locales, " + s+ " sub items.");
        println("<hr>");
        return s;
    }
    
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
       return getSC().getLocaleCoverageOrganizations().toArray(new String[0]);
   }
    
    public String getChosenLocaleType() {
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
        }
        
        String org = getEffectiveLocaleType(getChosenLocaleType());
        if(org!=null) {
            options.put("CoverageLevel.localeType",org);
        }
        
        // the following is highly suspicious. But, CheckCoverage seems to require it.
        options.put("submission", "true");
                
        return options;
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
            Reference sr = (Reference)getByLocaleStatic(DATA_POD+prefix+":"+ptype);  // GET******
            if(sr == null) {
                return null; // wasn't never there
            }
            DataPod dp = (DataPod)sr.get();
            if(dp == null) {
//                System.err.println("SR expired: " + locale + ":"+ prefix+":"+ptype);
            }
            return dp;
        }
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
            if((pod != null) && (!pod.isValid())) {
                pod = null;
                loadString = "data was re-loaded due to a new user submission.";
            }
            if(pod == null) {
                long t0 = System.currentTimeMillis();
                ElapsedTimer podTimer = new ElapsedTimer("There was a delay of {0} as " + loadString);
                pod = DataPod.make(this, locale.toString(), prefix, false);
                if((System.currentTimeMillis()-t0) > 10 * 1000) {
                    println("<i><b>" + podTimer + "</b></i><br/>");
                }
                synchronized (staticStuff) {
                    pod.register();
//                    SoftReference sr = (SoftReference)getByLocaleStatic(DATA_POD+prefix+":"+ptype);  // GET******
                      putByLocaleStatic(DATA_POD+prefix+":"+ptype, new SoftReference(pod)); // PUT******
                      putByLocale("__keeper:"+prefix+":"+ptype, pod); // put into user's hash so it wont go out of scope
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
    
    public void printHelpHtml(DataPod pod, String xpath) {
        String helpHtml = pod.exampleGenerator.getHelpHtml(xpath,null);
        if(helpHtml != null)  {
            println("<div class='helpHtml'><!-- "+xpath+" -->\n"+helpHtml+"</div>");
        }        
    }
    
    
    public void printHelpLink(String what)  {
        printHelpLink(what, "Help");
    }
    
    public void printHelpLink(String what, String title)
    {
//        boolean doEdit = UserRegistry.userIsTC(session.user);
    
        printHelpLink(what, title, true);
    }
    public void printHelpLink(String what, String title, boolean doEdit) {
        printHelpLink(what,title,doEdit,true);
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
        return iconHtml("hand",message);
    }
    public String iconHtml(String icon, String message) {
        return "<img border='0' alt='["+icon+"]' style='width: 16px; height: 16px;' src='"+context(icon+".png")+"' title='"+message+"' />";
    }
}
