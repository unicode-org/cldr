//
//  WebContext.java
//  cldrtools
//
//  Created by Steven R. Loomis on 3/11/2005.
//  Copyright 2005-2008 IBM. All rights reserved.
//
package org.unicode.cldr.web;

import org.w3c.dom.Document;
import java.io.*;
import java.util.*;

import org.unicode.cldr.util.*;
import org.unicode.cldr.web.Vetting.DataSubmissionResultHandler;
import org.unicode.cldr.test.*;
import org.unicode.cldr.test.ExampleGenerator.HelpMessages;

import com.ibm.icu.util.ULocale;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.ref.SoftReference;

// servlet imports
import javax.servlet.*;
import javax.servlet.http.*;


// sql imports
import java.sql.Connection;
import java.sql.SQLException;

import com.ibm.icu.dev.test.util.ElapsedTimer;

/**
 * This is the per-client context passed to basically all functions
 * it has print*() like functions, and so can be written to.
 */
public class WebContext implements Cloneable {
    public static java.util.logging.Logger logger = SurveyMain.logger;
// USER fields
    public SurveyMain sm = null;
    public Document doc[]= new Document[0];
    private CLDRLocale locale = null;
//    public String  localeString = null;
    public ULocale displayLocale = SurveyMain.BASELINE_LOCALE;
    public CLDRLocale docLocale[] = new CLDRLocale[0];
    public String localeName = null; 
    public CookieSession session = null;
    public ElapsedTimer reqTimer = null;
    public Hashtable temporaryStuff = new Hashtable();
    public static final String CLDR_WEBCONTEXT="cldr_webcontext";
    
    
// private fields
    protected Writer out = null;
    private PrintWriter pw = null;
    String outQuery = null;
    TreeMap<String, String> outQueryMap = new TreeMap<String, String>();
    boolean dontCloseMe = false;

    public PrintWriter getOut() { 
        return pw;
    }
    
    public void flush() {
    	pw.flush();
    }
    
    HttpServletRequest request;
    HttpServletResponse response;
    
    public Map getParameterMap() { 
        return request.getParameterMap();
    }
    
    /**
     * Construct a new WebContext from the servlet request and response. This is the normal constructor to use 
     * when a top level servlet or JSP spins up.  Embedded JSPs should use fromRequest.
     */
    public WebContext(HttpServletRequest irq, HttpServletResponse irs) throws IOException {
        setRequestResponse(irq, irs);
        setStream(irs.getWriter());
    }
    
    /**
     * Internal function to setup the WebContext to point at a servlet req/resp.  Also registers the WebContext with the Request.
     * @param irq
     * @param irs
     * @throws IOException
     */
    protected void setRequestResponse(HttpServletRequest irq, HttpServletResponse irs) throws IOException {
       request = irq;
       response = irs;
       // register us - only if another webcontext is not already registered.
       if(request.getAttribute(CLDR_WEBCONTEXT)==null) {
           request.setAttribute(CLDR_WEBCONTEXT, this);
       }
    }
    
    protected void setStream(Writer w) {
        out = w;
        if(out instanceof PrintWriter) {
            pw = (PrintWriter)out;
        } else {
            pw = new PrintWriter(out,true);
        }
        dontCloseMe = true; // do not close the stream if the Response owns it.
    }
    
    /**
     * Call this from a .jsp which is embedded in survey tool to extract the WebContext object.
     * The WebContext will have its output stream set to point to the request and response, so you
     * can mix write calls from the JSP with ST calls.
     * @param request
     * @param response
     * @param out
     * @return the new WebContext, which was cloned from the one posted to the Request
     * @throws IOException
     */
    public static JspWebContext fromRequest(ServletRequest request, ServletResponse response, Writer out) throws IOException {
        WebContext ctx = (WebContext) request.getAttribute(CLDR_WEBCONTEXT);
        if(ctx == null) {
            throw new InternalError("WebContext: could not load fromRequest. Are you trying to load a JSP directly?");
        }
        JspWebContext subCtx = new JspWebContext(ctx); // clone the important fields..
        subCtx.setRequestResponse((HttpServletRequest)request,  // but use the req/resp of the current situation
                         (HttpServletResponse)response );
        subCtx.setStream(out);
        return subCtx;
    }
    
    /**
     * Construct a new fake WebContext - for testing purposes.
     * Writes to stderr. 
     */
    public WebContext(boolean fake)throws IOException  {
        dontCloseMe=false;
        out=openUTF8Writer(System.out);
    }
    
    /**
     * Copy one WebContext to another.
     * useful for sub-contexts with different base urls.
     */
    public WebContext( WebContext other) {
        if((other instanceof URLWebContext) && !(this instanceof URLWebContext)) {
            throw new InternalError("Can't slice a URLWebContext - use clone()");
        }
        doc = other.doc;
        docLocale = other.docLocale;
        displayLocale = other.displayLocale;
        out = other.out;
        pw = other.pw;
        outQuery = other.outQuery;
        localeName = other.localeName;
        locale = other.locale;
//        if(locale != null) {
//            localeString = locale.getBaseName();
//        }
        session = other.session;
        outQueryMap = (TreeMap<String, String>)other.outQueryMap.clone();
        dontCloseMe = true;
        request = other.request;
        response = other.response;
        sm = other.sm;
        processor = other.processor;
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
    public int fieldInt(String x, int def) {
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
    

    /*
    * get a field's value, or -1
    * @param x field name
    */
   final long fieldLong(String x) {
       return fieldLong(x,-1);
   }
   
   /**
    * get a field's value, or the default
    * @param x field name
    * @param def default value
    */
   long fieldLong(String x, long def) {
       String f;
       if((f=field(x)).length()>0) {
           try {
               return new Long(f).longValue();
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
     * return a field's values, or a 0 length array if none
     * @param x field name
     */
    public final String[] fieldValues(String x) {
        String values[] = request.getParameterValues(x);
		if(values == null) {
			// make it a 0-length array.
			values = new String[0];
		} else {
			// decode utf-8, etc.
			for(int n=0;n<values.length;n++) {
				values[n] = decodeFieldString(values[n]);
			}
		}
		
		return values;
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
        
		return decodeFieldString(res);
	}

	/*
	 * Decode a single string
	 */
	public static String decodeFieldString(String res) {
		if(res==null) return null;
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
    
    public void setQuery(String k, String v) {
        if(outQueryMap.get(k)==null) { // if it wasn't there..
            addQuery(k,v); // then do a simple append
        } else {
            // rebuild query string:
            outQuery=null;
            TreeMap<String, String> oldMap = outQueryMap;
            oldMap.put(k,v); // replace
            outQueryMap=new TreeMap<String, String>();
            for(Iterator<String> i=oldMap.keySet().iterator();i.hasNext();) {
                String somek = i.next();
                addQuery(somek,oldMap.get(somek));
            }
        }
    }

    void removeQuery(String k) {
        if(outQueryMap.get(k)!=null) { // if it was there..
            // rebuild query string:
            outQuery=null;
            TreeMap<String, String> oldMap = outQueryMap;
            oldMap.remove(k); // replace
            outQueryMap=new TreeMap<String, String>();
            for(Iterator<String> i=oldMap.keySet().iterator();i.hasNext();) {
                String somek = i.next();
                addQuery(somek,oldMap.get(somek));
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
        return context() + request.getServletPath();
    }
    
    public static String base(HttpServletRequest request) {
        return schemeHostPort(request)+request.getContextPath() + request.getServletPath();
    }
    
    public String context() { 
        return request.getContextPath();
    }

    public String context(String s) { 
        return context() + "/" + s;
    }
    
    public String jspLink(String s) {
        return context(s)+"?a=" + base() +
            ((outQuery!=null)?("&amp;" + outQuery):
                ((session!=null)?("&amp;s="+session.id):"")
            );
     }
    public String jspUrl(String s) {
        return context(s)+"?a=" + base() +
            ((outQuery!=null)?("&" + outQuery):
                ((session!=null)?("&s="+session.id):"")
            );
     }
    
    void printUrlAsHiddenFields() {
        for(Iterator<String> e = outQueryMap.keySet().iterator();e.hasNext();) {
            String k = e.next().toString();
            String v = outQueryMap.get(k).toString();
            println("<input type='hidden' name='" + k + "' value='" + v + "'/>");
        }
    }
    
    /**
     * return the IP of the remote user.
     */
    String userIP() {
	return userIP(request);
    }
	
    /**
     * return the IP of the remote user, who might be behind a proxy
     */
    public static String userIP(HttpServletRequest request) {
	String ip = request.getHeader("x-forwarded-for");
	if(ip==null || ip.length()==0) {
		ip = request.getRemoteAddr();
	}
	return ip;
    }

    /**
     * return the hostname of the web server
     */
    String serverName() {
        return request.getServerName();
    }
    
    /**
     * return the hostname of the web server
     */
    static String serverName(HttpServletRequest request) {
        return request.getServerName();
    }
    
    String serverHostport() {
        return serverHostport(request);
    }
    static String serverHostport(HttpServletRequest request) {
        int port = request.getServerPort();
        String scheme = request.getScheme(); 
        if(port == 80 && "http".equals(scheme)) {
            return serverName(request);
        } else if(port == 443 && "https".equals(scheme)) {
            return serverName(request);
        } else {
            return serverName(request) + ":"+port;
        }
    }
    String schemeHostPort() {
        return schemeHostPort(request);
    }
    
    static String schemeHostPort(HttpServletRequest request) {
        return request.getScheme()+"://" + serverHostport(request);
    }
    
// print api
    final void println(String s) {
        pw.println(s);
    }
    
    final void print(String s) {
        pw.print(s);
    }
    
    void print(Throwable t) {
        print("<pre style='border: 2px dashed red; margin: 1em; padding: 1'>" +                        
                t.toString() + "<br />");
        StringWriter asString = new StringWriter();
        if(t instanceof SQLException) {
        	println("SQL: "+SurveyMain.unchainSqlException((SQLException)t));
        } else {
        	t.printStackTrace(new PrintWriter(asString));
        }
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
    
    void close() throws IOException {
        if(!dontCloseMe) {
            out.close();
            out = null;
        } else {
            // ? 
        }
    }
    
// doc api

    
    public DisplayAndInputProcessor processor = null;
    
    void setLocale(CLDRLocale l) {
        locale = l;
//        localeString = locale.getBaseName();
        processor = new DisplayAndInputProcessor(l.toULocale());
        Vector<CLDRLocale> localesVector = new Vector<CLDRLocale>();
        for(CLDRLocale parents : locale.getParentIterator()) {
                localesVector.add(parents);
        }
        docLocale = localesVector.toArray(docLocale);        
           // logger.info("NOT NOT NOT fetching locale: " + l.toString() + ", count: " + doc.length);
    }
    
    /**
     * @deprecated use getLocale().toString()
     */
    public final String localeString() {
        if(locale == null) {
            throw new InternalError("localeString is null, locale="+ locale);
        }
        return locale.toString();
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
    static Hashtable<CLDRLocale, Hashtable<String, Object>> staticStuff = new Hashtable<CLDRLocale, Hashtable<String, Object>>();
    
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
        DataSection section = (DataSection)o;
        
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
        } else if(o instanceof DataSection) {
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
    public static synchronized final Object getByLocaleStatic(String key, CLDRLocale aLocale) {
        Hashtable subHash = staticStuff.get(aLocale);
        if(subHash == null) {
            return null;
        }
        return subHash.get(key);
    }
    public static final synchronized void putByLocaleStatic(String key, CLDRLocale locale, Object value) {
        Hashtable<String, Object> subHash = staticStuff.get(locale);
        if(subHash == null) {
            subHash = new Hashtable<String, Object>();
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
        if(sm.isPhaseSubmit()) {
            String def = pref(SurveyMain.PREF_COVLEV,"default");
            if(!def.equals("default")) {
                return def;
            } else {
                String org = getChosenLocaleType();
                String ltype = getEffectiveLocaleType(org);
                return ltype;
            }
        } else {
            return "Defaults";
        }
    }
    
    static String getEffectiveLocaleType(String org) {
        try {
            return  getSC().getEffectiveLocaleType(org);
        } catch (java.io.IOException ioe) {
            return org;
        }
    }
    
   static String[] getLocaleTypes() {
       return getSC().getLocaleCoverageOrganizations().toArray(new String[0]);
   }
    
    public String getChosenLocaleType() {
        if(sm.isPhaseSubmit()) { 
            String org = pref(SurveyMain.PREF_COVTYP, "default");
            if(org.equals("default")) {
                org = null;
            }
            if((org==null) && 
               (session.user != null)) {
                org = session.user.org;
            }
            return org;
        } else {
            return defaultPtype();
        }
    }
    
    public Map<String, String> getOptionsMap() {
        return getOptionsMap(sm.basicOptionsMap());
    }
    
    public Map<String, String> getOptionsMap(Map<String, String> options) {
        if(sm.isPhaseSubmit()) { 
            String def = pref(SurveyMain.PREF_COVLEV,"default");
            if(!def.equals("default")) {
                options.put("CheckCoverage.requiredLevel",def);
            }
            
            String org = getEffectiveLocaleType(getChosenLocaleType());
            if(org!=null) {
                options.put("CoverageLevel.localeType",org);
            }
        }
                
        return options;
    }
    
// DataPod functions
    private static final String DATA_POD = "DataPod_";
    
    /**
     * Get a pod.. even if it may be no longer invalid.
     * May be null.
     */
    DataSection getExistingSection(String prefix) {
        return getExistingSection(prefix, defaultPtype());
    }
    
    DataSection getExistingSection(String prefix, String ptype) {
        synchronized(this) {
            Reference sr = (Reference)getByLocaleStatic(DATA_POD+prefix+":"+ptype);  // GET******
            if(sr == null) {
                return null; // wasn't never there
            }
            DataSection dp = (DataSection)sr.get();
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
    DataSection getSection(String prefix) {
        return getSection(prefix, defaultPtype());
    }
    
    DataSection getSection(String prefix, String ptype) {
        if(hasField("srl_veryslow")&&sm.isUnofficial) {
            for(int q=0;q<50;q++) {
                DataSection.make(this, locale, prefix, false);
            }
        }
    
        String loadString = "data was loaded.";
	DataSection section = null;
        synchronized(this) {
            section = getExistingSection(prefix, ptype);
            if((section != null) && (!section.isValid())) {
                section = null;
                loadString = "data was re-loaded due to a new user submission.";
            }
            if(section == null) {
                long t0 = System.currentTimeMillis();
                ElapsedTimer podTimer = new ElapsedTimer("There was a delay of {0} as " + loadString);
                section = DataSection.make(this, locale, prefix, false);
                if((System.currentTimeMillis()-t0) > 10 * 1000) {
                    println("<i><b>" + podTimer + "</b></i><br/>");
                }
            }
                    section.register();
//                    SoftReference sr = (SoftReference)getByLocaleStatic(DATA_POD+prefix+":"+ptype);  // GET******
	}
            putByLocaleStatic(DATA_POD+prefix+":"+ptype, new SoftReference<DataSection>(section)); // PUT******
            putByLocale("__keeper:"+prefix+":"+ptype, section); // put into user's hash so it wont go out of scope
            section.touch();
            return section;
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
    
    static final HelpMessages surveyToolHelpMessages = new HelpMessages("test_help_messages.html");
	public static final String CAN_MODIFY = "canModify";
	public static final String DATA_SECTION = "DataSection";
	public static final String ZOOMED_IN = "zoomedIn";
	public static final String DATA_ROW = "DataRow";
	public static final String BASE_EXAMPLE = "baseExample";
	public static final String BASE_VALUE = "baseValue";

    public void printHelpHtml(String xpath) {
        /*
     * TODO: Mark says:   "getHelpHtml() is independent of locale. Whichever locale you 
     * call it on, you'll get the same answer. So you can call it always on English 
     * (or any other language) if you want. Its code just uses the static class 
     * HelpMessages, and a file for the source. So you can create your own static class
     * once for that, like:
     *    static final HelpMessages surveyToolHelpMessages = new HelpMessages("test_help_messages.html");
     *  That is what I'd recommend. "
     
*/
        String helpHtml = surveyToolHelpMessages.find(xpath);
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
        if(message==null) {
            message = "[" + icon +"]";
        }
        return "<img border='0' alt='["+icon+"]' style='width: 16px; height: 16px;' src='"+context(icon+".png")+"' title='"+message+"' />";
    }
    
    public Object clone() {
        return new WebContext(this);
    }
    
    public void includeFragment(String filename)  {
        RequestDispatcher dp = request.getRequestDispatcher("/WEB-INF/tmpl/"+filename);
        try {
            dp.include(request,response);
        } catch(Throwable t) {
            this.println("<div class='ferrorbox'><B>Error</b> while including template <tt class='code'>"+filename+"</tt>:<br>");
            this.print(t);
            this.println("</div>");
            System.err.println("While expanding /WEB-INF/tmpl/"+filename + ": " +t.toString());
            t.printStackTrace();
        }
    }

    public void put(String string, Object object) {
        temporaryStuff.put(string, object);
    }
    public Object get(String string) {
        return temporaryStuff.get(string);
    }

    /**
     * Return the CLDRLocale with which this WebCOntext currently pertains.
     * @return
     */
    public CLDRLocale getLocale() {
        return locale;
    }

	public void setQuery(String k, int v) {
		setQuery(k, new Integer(v).toString());
	}
	
	// Display Context Data
	protected Boolean canModify = null;
	private Boolean zoomedIn = null;

	public boolean setCanModify(boolean canModify) {
		this.canModify = canModify;
		return canModify;
	}
	
	public Boolean canModify() {
		if(canModify==null) throw new InternalError("canModify()- not set.");
		return canModify;
	}

	/**
	 * @param zoomedIn the zoomedIn to set
	 */
	public void setZoomedIn(Boolean zoomedIn) {
		this.zoomedIn = zoomedIn;
	}

	/**
	 * @return the zoomedIn
	 */
	public Boolean zoomedIn() {
		if(canModify==null) throw new InternalError("zoomedIn()- not set.");
		return zoomedIn;
	}
}
