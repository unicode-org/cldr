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
import org.unicode.cldr.web.SurveyAjax.AjaxType;
import org.unicode.cldr.web.Vetting.DataSubmissionResultHandler;
import org.unicode.cldr.web.WebContext.HTMLDirection;
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
    public static final String TMPL_PATH = "/WEB-INF/tmpl/";
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
    HttpServletRequest request;
    HttpServletResponse response;

    /**
     * 
     * @return the output PrintWriter
     */
    public PrintWriter getOut() { 
        return pw;
    }
    
    /**
     * Flush output content. This is useful when JSPs are mixed in with servlet code.
     * @see java.io.PrintWriter#flush()
     */
    public void flush() {
    	pw.flush();
    }
    

    /**
     * Return the parameter map of the underlying request.
     * @return {@link ServletRequest#getParameterMap()}
     */
    public Map getParameterMap() { 
        return request.getParameterMap();
    }
    
    /**
     * Construct a new WebContext from the servlet request and response. This is the normal constructor to use 
     * when a top level servlet or JSP spins up.  Embedded JSPs should use fromRequest.
     * @see #fromRequest(ServletRequest, ServletResponse, Writer)
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
    
    /**
     * Change the output stream to a different writer. If it isn't a PrintWriter, it will be wrapped in one.
     * The WebContext will assume it does not own the stream, and will not close it when done.
     * @param w
     */
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
     * Extract (or create) a WebContext from a request/response.
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
     * Construct a new, fake WebContext - for testing purposes.
     * Writes all output to stdout.
     * @param fake ignored
     */
    public WebContext(boolean fake)throws IOException  {
        dontCloseMe=false;
        out=openUTF8Writer(System.out);
        pw = new PrintWriter(out);
    }
    
    /**
     * Copy one WebContext to another. This is useful when you wish to create a sub-context
     * which has a different base URL (such as for processing a certain form or widget).
     * @param other the other WebContext to copy from
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
     * @return the field value
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
     * get a field's value as an integer, or -1 if not found
     * @param x field name
     * @return the field's value as an integer, or -1 if it was not found
     */
    final int fieldInt(String x) {
        return fieldInt(x,-1);
    }
    
    /**
     * get a field's value, or the default
     * @param x field name
     * @param def default value
     * @return the field's value as an integer, or the default value if the field was not found.
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
    * get a field's value as a long, or -1
    * @param x field name
    * @return the field's value as a long, or -1 value if the field was not found.
    */
   final long fieldLong(String x) {
       return fieldLong(x,-1);
   }
   
   /**
    * get a field's value, or the default
    * @param x field name
    * @param def default value
    * @return the field's value as a long, or the default value if the field was not found.
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
     * @param x field name
     * @return true if the field is present
     */
    public boolean hasField(String x) {
        return(request.getParameter(x)!=null);
    }
    
    /**
     * return a field's value, else ""
     * @param x field name
     * @return the field value, or else ""
     */
    public final String field(String x) {
        return field(x, "");
    }

    /**
     * return a field's values, or a 0-length array if none
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
     * return a field's value, else default
     * @param x field name
     * @param def default value
     * @return the field's value as a string, otherwise the default
     */
    public String field(String x, String def) {
        if(request==null) { 
            return def; // support testing
        }

        String res = request.getParameter(x);
         if(res == null) {       
            return def;    // don't try to transcode null.
        }
		return decodeFieldString(res);
	}

	/*
	 * Decode a single string from URL format into Unicode
	 * @param res UTF-8 'encoded' bytes (expanded to a string)
	 * @return Unicode string (will return 'res' if no high bits were detected)
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
     * @return preference value (or false)
     */
    public boolean prefBool(String x) {
        return prefBool(x,false);
    }

    /**
     * Get a preference's value as an integer. Defaults to 'def'
     * @param x field name
     * @param def default value
     * @return the prefence's value
     */
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
    
    /**
     * Get a preference's value as an integer, or else -1
     * @param x field name
     * @return preference value or -1 
     */
    int prefInt(String x) {
        return prefInt(x, -1);
    }
    
    int codesPerPage = -1;
    
    /**
     * Special preference: Number of codes to show on a single page
     * @return The preferred value (minimum: 5)
     * @see SurveyMain#CODES_PER_PAGE
     */
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
     * @return the preference value
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
     * @return string preference value or "" if otherwise not found.
     */
    String pref(String x) {
        return pref(x, "");
    }
    
    /**
     * get a pref that is a string, 
     * @param x the field name and pref name
     * @param def default value
     * @return pref value or def
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
    
    /**
     * Get the target keyword and value for an 'a href' HTML tag.
     * @return the target to use (defaults to something like  'target=_blank')
     */
    String atarget() {
        return atarget("_blank");
    }
    
    /**
     * Get the target  keyword and value for an 'a href' HTML tag
     * @param target the target name to use
     * @return the 'target=...' string - may be blank if the user has requested no popups
     */
    public String atarget(String target) {
        if(prefBool(SurveyMain.PREF_NOPOPUPS)) {
            return "";
        } else {
            return "target='SurveyTool:"+target+"' ";
        }
    }

    /**
     * Add a parameter to the output URL
     * @param k key
     * @param v value
     */
    public void addQuery(String k, String v) {
        outQueryMap.put(k,v);
        if(outQuery == null) {
            outQuery = k + "=" + v;
        } else {
            outQuery = outQuery + "&amp;" + k + "=" + v;
        }
    }
    
    /**
     * Add a boolean parameter to the output URL as 't' or 'f'
     * @param k key
     * @param v value
     */
    void addQuery(String k, boolean v) {
        addQuery(k,v?"t":"f");
    }
    
    /**
     * Set a parameter on the output URL, replacing an existing value if any
     * @param k key
     * @param v value
     */
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

    /**
     * Set a query from an integer
     * @param k
     * @param v
     */
	public void setQuery(String k, int v) {
		setQuery(k, new Integer(v).toString());
	}
	
    /**
     * Remove the specified key from the query. Has no effect if the field doesn't exist.
     * @param k key
     */
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

    /**
     * Return the output URL 
     * @return the output URL
     */
    public String url() {
        if(outQuery == null) {
            return base();
        } else {
            return base() + "?" + outQuery;
        }
    }

    /**
     * Returns the string that must be appended to the URL to start the next parameter - either ? or &amp;
     * @return the connecting string
     */
    final String urlConnector() {
        return (url().indexOf('?')!=-1)?"&amp;":"?";
    }
    
    /**
     * Get the base URL (servlet path)
     * @return the servlet path in context
     */
    String base() { 
        return context() + request.getServletPath();
    }
    
    /**
     * Get the base URL for some request
     * @param request
     * @return base URL
     */
    public static String base(HttpServletRequest request) {
        return schemeHostPort(request)+request.getContextPath() + request.getServletPath();
    }
    
    /**
     * Get the context path
     * @return the context path
     */
    public String context() { 
        return request.getContextPath();
    }

    /**
     * Get the context path for a certain resource
     * @param s resource URL
     * @return the context path for the specified resource
     */
    public String context(String s) { 
        return context(request,s);
    }
    /**
     * Get the context path for a certain resource
     * @param s resource URL
     * @return the context path for the specified resource
     */
    public static String context(HttpServletRequest request, String s) { 
        return request.getContextPath() + "/" + s;
    }
    
    /**
     * Get a link (HTML URL) to  a JSP
     * @param s resource to link to
     * @return the URL suitable for HTML
     */
    public String jspLink(String s) {
        return context(s)+"?a=" + base() +
            ((outQuery!=null)?("&amp;" + outQuery):
                ((session!=null)?("&amp;s="+session.id):"")
            );
     }
    /**
     * Get a link (Text URL) to  a JSP
     * @param s resource to link to
     * @return the URL suitable for Text
     */
    public String jspUrl(String s) {
        return context(s)+"?a=" + base() +
            ((outQuery!=null)?("&" + outQuery):
                ((session!=null)?("&s="+session.id):"")
            );
     }
    
    /**
     * Output the full current output URL in hidden field format.
     */
    void printUrlAsHiddenFields() {
        for(Iterator<String> e = outQueryMap.keySet().iterator();e.hasNext();) {
            String k = e.next().toString();
            String v = outQueryMap.get(k).toString();
            println("<input type='hidden' name='" + k + "' value='" + v + "'/>");
        }
    }
    
    /**
     * return the IP of the remote user. If they are behind a proxy, return the actual original URL.
     * @return a URL
     */
    String userIP() {
    	return userIP(request);
    }
	
    /**
     * return the IP of the remote user given a request. If they are behind a proxy, return the actual original URL.
     * @param request the request to use
     * @return a URL
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
     * @return the Server Name
     */
    String serverName() {
        return request.getServerName();
    }
    
    /**
     * return the hostname of the web server given a request
     * @return the Server name
     */
    static String serverName(HttpServletRequest request) {
        return request.getServerName();
    }
    
    /**
     * Returns the host:port of the server
     * @return the "host:port:
     */
    String serverHostport() {
        return serverHostport(request);
    }
    
    /**
     * Returns the host:port of the server
     * @param request a specific request
     * @return the "host:port:
     */
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
    /**
     * Returns the scheme://host:port
     * @return the "scheme://host:port"
     */
    String schemeHostPort() {
        return schemeHostPort(request);
    }
    
    /**
     * Returns the scheme://host:port
     * @return the "scheme://host:port"
     * @param request the request portion
     */
    static String schemeHostPort(HttpServletRequest request) {
        return request.getScheme()+"://" + serverHostport(request);
    }
    
    /**
     * Print out a line
     * @param s line to print
     * @see PrintWriter#println(String)
     */
    public final void println(String s) {
        pw.println(s);
    }
    
    /**
     * @param s
     * @see PrintWriter#print(String)
     */
    public final void print(String s) {
        pw.print(s);
    }
    
    /**
     * Print out a Throwable as HTML.
     * @param t throwable to print
     */
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

    /**
     * Send the user to another URL.  Won't work if there was already some output.
     * @param where
     * @see HttpServletResponse#sendRedirect(String)
     */
    void redirect(String where) {
        try {
            response.sendRedirect(where);
            out.close();
            close();
        } catch(IOException ioe) {
            throw new RuntimeException(ioe.toString() + " while redirecting to "+where);
        }
    }
    
    /**
     * Close the stream. Normally not called directly.
     * @throws IOException
     */
    void close() throws IOException {
        if(!dontCloseMe) {
            out.close();
            out = null;
        } else {
            // ? 
        }
    }
    
// doc api

    /**
     * the current processor
     * @see DisplayAndInputProcessor
     */
    public DisplayAndInputProcessor processor = null;
    
    /**
     * Set this context to be handling a certain locale
     * @param l locale to set
     */
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
     * Cached direction of this locale.
     */
    private HTMLDirection direction = null;
    
    /**
     * Return the HTML direction of this locale, ltr or rtl. Returns ltr by default.
     * TODO: should return display locale's directionality by default.
     * @return directionality
     */
    public HTMLDirection getDirectionForLocale() {
        if((locale != null) && (direction == null)) {
            direction = sm.getHTMLDirectionFor(getLocale());
        }
        if(direction==null) {
            return HTMLDirection.LEFT_TO_RIGHT;
        } else {
            return direction;
        }
    }
    
    /**
     * Return the current locale as a string. Deprecated, please use getLocale instead.
     * @deprecated use getLocale().toString() - 
     * @see #getLocale()
     */
    public final String localeString() {
        if(locale == null) {
            throw new InternalError("localeString is null, locale="+ locale);
        }
        return locale.toString();
    }

    /**
     * Get an object out of the session data  
     * @param key 
     * @param aLocale locale to fetch
     * @return the object or null
     */
    public final Object getByLocale(String key, String aLocale) {
        return session.getByLocale(key,aLocale);
    }

    /**
     * Put an object into the session data
     * @param key 
     * @param locale
     * @param value object to put
     */
    public final void putByLocale(String key, String locale, Object value) {
        session.putByLocale(key,locale,value);
    }
        
    /**
     * Remove an object from the session data
     * @param key
     * @param aLocale
     */
    public final void removeByLocale(String key, String aLocale) {
        session.removeByLocale(key,aLocale);
    }
    
    /**
     * Remove an object from the current locale's session data
     * @param key
     */
    public final void removeByLocale(String key) {
        removeByLocale(key, locale.toString());
    }
    /**
     * Put an object into the current locale's session data
     * @param key
     * @param value
     */
    public final void putByLocale(String key, Object value) {
        putByLocale(key, locale.toString(), value);
    }
    /**
     * Get an object from the current locale's session data
     * @param key
     * @return the object
     */
    public final Object getByLocale(String key) {
		return getByLocale(key, locale.toString());
	}
    
    // Static data
    static Hashtable<CLDRLocale, Hashtable<String, Object>> staticStuff = new Hashtable<CLDRLocale, Hashtable<String, Object>>();
    
    /**
     * Debugging: print a Reference object
     * @param o
     * @return number of sub-objects including this object
     */
    public int staticInfo_Reference(Object o) {
        int s = 0;
        Object oo = ((Reference)o).get();
        println("Reference -&gt; <ul>");
        s += staticInfo_Object(oo);
        println("</ul>");
        return s;
    }
    
    /**
     * Debugging: print a DataPod object
     * @param o
     * @return number of sub-objects including this object
     */
    public int staticInfo_DataPod(Object o) {
        int s = 0;
        DataSection section = (DataSection)o;
        
        print(o.toString());
        
        return 1;
    }

    /**
     * Debugging: print an Object object
     * @param o
     * @return number of sub-objects including this object
     */
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
    
    /**
     * Debugging: print a Hashtable object
     * @param o
     * @return number of sub-objects including this object
     */
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

    /**
     * Debugging: print a String object
     * @param o
     * @return number of sub-objects including this object
     */
    public int staticInfo_String(Object o) {
        String obj = (String)o;
        println("("+obj+")<br>");
        return 1;
    }

    /**
     * Debugging: print a Boolean object
     * @param o
     * @return number of sub-objects including this object
     */
    public int staticInfo_Boolean(Object o) {
        Boolean obj = (Boolean)o;
        boolean b = (boolean)obj;
        println(obj.toString()+"<br>");
        return 1;
    }
    
    /**
     * Debugging: print out all static objects
     * @return the number of sub items
     */
    public final int staticInfo() {
        println("<h4>Static Info</h4>");
        int s = staticInfo_Object(staticStuff);
        println(staticStuff.size() + " locales, " + s+ " sub items.");
        println("<hr>");
        return s;
    }
    
    /**
     * Put an object into the current locale's static store
     * @param key
     * @param value
     */
    public final void putByLocaleStatic(String key, Object value) {
        putByLocaleStatic(key, locale, value);
    }
    /**
     * Get an object from the current locale's static store
     * @param key
     * @return the object
     */
    public final Object getByLocaleStatic(String key) {
		return getByLocaleStatic(key, locale);
	}
    // bottlenecks for static access
    /**
     * Get an object from the specified static stuff
     */
    public static synchronized final Object getByLocaleStatic(String key, CLDRLocale aLocale) {
        Hashtable subHash = staticStuff.get(aLocale);
        if(subHash == null) {
            return null;
        }
        return subHash.get(key);
    }
    /**
     * Put an object into the current locale's static stuff
     * @param key
     * @param locale
     * @param value
     */
    public static final synchronized void putByLocaleStatic(String key, CLDRLocale locale, Object value) {
        Hashtable<String, Object> subHash = staticStuff.get(locale);
        if(subHash == null) {
            subHash = new Hashtable<String, Object>();
            staticStuff.put(locale, subHash);
        }
        subHash.put(key,value);
    }
    static private StandardCodes sc = null;
    
    /**
     * Get a copy of the StandardCodes
     * @return the StandardCodes
     * @see StandardCodes#make()
     */
    static private synchronized StandardCodes getSC() {
        if(sc == null) {
        	// TODO: this is a singleton. NO need to cache.
            sc = StandardCodes.make();
        }
        return sc;
    }
    
    /**
     * Get the default coverage level type, possibly for the user's org
     * @return the default type
     */
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
    
    /**
     * Get the effective locale type
     * @param org
     * @return
     * @see StandardCodes#getEffectiveLocaleType(String)
     */
    static String getEffectiveLocaleType(String org) {
        try {
            return  getSC().getEffectiveLocaleType(org);
        } catch (java.io.IOException ioe) {
            return org;
        }
    }
   
    /**
     * Get a list of all locale types
     * @return a list of locale types
     * @see StandardCodes#getLocaleCoverageOrganizations()
     */
   static String[] getLocaleTypes() {
       return getSC().getLocaleCoverageOrganizations().toArray(new String[0]);
   }
    
   /**
    * Return the type of the current locale according to coverage
    * @return the type for locale coverage
    */
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
    
    /**
     * Get the basic and WebContext Options map
     * @return the map
     * @see #getOptionsMap(Map)
     * @see org.unicode.cldr.test.CheckCoverage#check(String, String, String, Map, List)
     * @see SurveyMain#basicOptionsMap()
     */
    public Map<String, String> getOptionsMap() {
        return getOptionsMap(sm.basicOptionsMap());
    }
    
    /**
     * Append the WebContext Options map to the specified map
     * @return the map
     * @see #getOptionsMap(Map)
     * @see org.unicode.cldr.test.CheckCoverage#check(String, String, String, Map, List)
     * @see SurveyMain#basicOptionsMap()
     */
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
     * Get the DataSection for the given xpath prefix and default ptype, even if it may be no longer valid.
     * May be null.
     * @param prefix the xpath prefix
     * @return the existing data section or null if it is invalid
     */
    DataSection getExistingSection(String prefix) {
        return getExistingSection(prefix, defaultPtype());
    }
    
    /**
     * Get the DataSection for the given xpath prefix and default ptype, even if it may be no longer valid.
     * May be null.
     * @param prefix the xpath prefix
     * @param ptype the ptype to use to distinguish
     * @return the existing data section or null if it is invalid
     */
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
     * Get a currently valid DataSection.. creating it if need be.
     * prints informative notes to the ctx in case of a long delay.
     * @param prefix
     */
    DataSection getSection(String prefix) {
        return getSection(prefix, defaultPtype());
    }
    
    /** 
     * Get a currently valid DataSection for the specified ptype.. creating it if need be.
     * prints informative notes to the ctx in case of a long delay.
     * @param prefix
     */
    DataSection getSection(String prefix, String ptype) {
        if(hasField("srl_veryslow")&&sm.isUnofficial) {
        	// TODO: parameterize
        	// test case: make the data section 50x
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
    /**
     * Open a UTF 8 writer (convenience function)/
     */
    public static PrintWriter openUTF8Writer(OutputStream out) throws IOException {
        return openWriter(out,"UTF-8");
    }
    
    /**
     * Open a Writer in the specified encoding
     * @param out
     * @param encoding
     * @return
     * @throws IOException
     */
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

	/**
	 * Get help on a certain xpath in html.
 	 * The HTML help given is independent of locale. 
	 * @param xpath
	 * @see HelpMessages
	 */
    public void printHelpHtml(String xpath) {
        String helpHtml = surveyToolHelpMessages.find(xpath);
        if(helpHtml != null)  {
            println("<div class='helpHtml'><!-- "+xpath+" -->\n"+helpHtml+"</div>");
        }        
    }
    
    /**
     * Print a link to help with the title 'Help'
     * @param what
     * @see #printHelpLink(String, String)
     */
    public void printHelpLink(String what)  {
        printHelpLink(what, "Help");
    }
    
    /**
     * Print a link to help with  a specified title
     * @param what the help to link to
     * @param title the title of the help
     */
    public void printHelpLink(String what, String title)
    {
        printHelpLink(what, title, true);
    }
    /**
     * @param what
     * @param title
     * @param doEdit
     * @deprecated editing is deprecated
     */
    public void printHelpLink(String what, String title, boolean doEdit) {
        printHelpLink(what,title,doEdit,true);
    }
    /**
     * @deprecated editing is deprecated
     * @param what
     * @param title
     * @param doEdit
     * @param parens
     */
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
    
    /**
     * Get HTML for the 'modify' thing, with the hand icon
     * @param message
     * @see #iconHtml(String, String)
     * @return HTML for the message
     */
    public String modifyThing(String message) {
        return iconHtml("hand",message);
    }
    /**
     * get HTML for an icon with a certain message
     * @param icon
     * @param message
     * @return the HTML for the icon and message
     */
    public String iconHtml(String icon, String message) {
        return iconHtml(request, icon, message);
    }
    
    public static String iconHtml(HttpServletRequest request, String icon, String message) {
        if(message==null) {
            message = "[" + icon +"]";
        }
        return "<img border='0' alt='["+icon+"]' style='width: 16px; height: 16px;' src='"+context(request, icon+".png")+"' title='"+message+"' />";
    }
    
    /**
     * Clone (copy construct) the context
     * @see #WebContext(WebContext)
     */
    public Object clone() {
        return new WebContext(this);
    }
    
    /**
     * Include a template fragment from /WEB-INF/tmpl
     * @param filename
     */
    public void includeFragment(String filename)  {
        try {
            WebContext.includeFragment(request, response, filename);
        } catch(Throwable t) {
            this.println("<div class='ferrorbox'><B>Error</b> while including template <tt class='code'>"+filename+"</tt>:<br>");
            this.print(t);
            this.println("</div>");
            System.err.println("While expanding "+TMPL_PATH+filename + ": " +t.toString());
            t.printStackTrace();
        }
    }
    /**
     * Include a template fragment from /WEB-INF/tmpl
     * @param request
     * @param response
     * @param filename
     * @throws ServletException
     * @throws IOException
     */
    public static void includeFragment(HttpServletRequest request, HttpServletResponse response, String filename) throws ServletException, IOException {
            RequestDispatcher dp = request.getRequestDispatcher(TMPL_PATH+filename);
            dp.include(request,response);
    }

    /**
     * Put something into the temporary (context, non session data) store
     * @param string
     * @param object
     */
    public void put(String string, Object object) {
        temporaryStuff.put(string, object);
    }
    /**
     * Get something from the temporary (context, non session data) store
     * @param string
     * @return the object
     */
    public Object get(String string) {
        return temporaryStuff.get(string);
    }

    /**
     * @return the CLDRLocale with which this WebCOntext currently pertains.
     * @see CLDRLocale
     */
    public CLDRLocale getLocale() {
        return locale;
    }

	// Display Context Data
	protected Boolean canModify = null;
	private Boolean zoomedIn = null;
    /**
     * A direction, suitable for html 'dir=...'
     * @author srl
     * @see getDirectionForLocale
     */
    public enum HTMLDirection {
        LEFT_TO_RIGHT("ltr"),
        RIGHT_TO_LEFT("rtl");
        
        private String str;
        HTMLDirection(String str) {
            this.str = str;
        }
        public String toString() {
            return str;
        }
        
        /**
         * Convert a CLDR direction to an enum
         * @param dir CLDR direction string
         * @return HTML direction enum
         */
        public static HTMLDirection fromCldr(String dir) {
            if(dir.equals("left-to-right")) {
                return HTMLDirection.LEFT_TO_RIGHT;
            } else if(dir.equals("right-to-left")) {
                return HTMLDirection.RIGHT_TO_LEFT;
            } else if(dir.equals("top-to-bottom")) {
                return HTMLDirection.LEFT_TO_RIGHT; // !
            } else {
                return HTMLDirection.LEFT_TO_RIGHT;
            }
        }
    }

	/**
	 * Set whether this user can modify this locale
	 * @param canModify
	 * @return true if the user can modify this locale
	 */
	public boolean setCanModify(boolean canModify) {
		this.canModify = canModify;
		return canModify;
	}
	
	/**
	 * Return true if the user can modify this locale
	 * @return true if the user can modify this locale
	 */
	public Boolean canModify() {
		if(canModify==null) throw new InternalError("canModify()- not set.");
		return canModify;
	}

	/**
	 * Set the zoomed-in state of this context
	 * @param zoomedIn true if this context is in 'zoomed-in' state
	 * @see #zoomedIn()
	 */
	public void setZoomedIn(Boolean zoomedIn) {
		this.zoomedIn = zoomedIn;
	}

	/**
	 * @return the zoomedIn state
	 * @see #setZoomedIn(Boolean)
	 */
	public Boolean zoomedIn() {
		if(canModify==null) throw new InternalError("zoomedIn()- not set.");
		return zoomedIn;
	}


    public void includeAjaxScript(AjaxType type)  {
        try {
            SurveyAjax.includeAjaxScript(request, response, type);
        } catch(Throwable t) {
            this.println("<div class='ferrorbox'><B>Error</b> while including template :<br>");
            this.print(t);
            this.println("</div>");
            System.err.println("While expanding ajax: " +t.toString());
            t.printStackTrace();
        }
    }
    
    public SurveyMain.UserLocaleStuff getUserFile() {
    	return sm.getUserFile(session, getLocale());
    }
    public CLDRFile getCLDRFile() {
    	return getUserFile().cldrfile;
    }
    
    /**
     * Get the user settings.
     * @return
     */
    UserSettings settings() {
        return session.settings();
    }
}
