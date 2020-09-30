//
//  WebContext.java
//  cldrtools
//
//  Created by Steven R. Loomis on 3/11/2005.
//  Copyright 2005-2012 IBM. All rights reserved.
//
package org.unicode.cldr.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.DisplayAndInputProcessor;
import org.unicode.cldr.test.HelpMessages;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.web.CLDRProgressIndicator.CLDRProgressTask;
import org.unicode.cldr.web.SurveyAjax.AjaxType;
import org.unicode.cldr.web.SurveyMain.Phase;
import org.unicode.cldr.web.SurveyMain.UserLocaleStuff;
import org.unicode.cldr.web.UserRegistry.LogoutException;
import org.unicode.cldr.web.UserRegistry.User;
import org.w3c.dom.Document;

import com.ibm.icu.dev.util.ElapsedTimer;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

/**
 * This is the per-client context passed to basically all functions it has
 * print*() like functions, and so can be written to.
 */
public class WebContext implements Cloneable, Appendable {
    public static final String TMPL_PATH = "/WEB-INF/tmpl/";
    public static java.util.logging.Logger logger = SurveyLog.logger;
    // USER fields
    public SurveyMain sm = null;
    public Document doc[] = new Document[0];
    private CLDRLocale locale = null;
    public ULocale displayLocale = SurveyMain.TRANS_HINT_LOCALE;
    public CLDRLocale docLocale[] = new CLDRLocale[0];
    public CookieSession session = null;
    public ElapsedTimer reqTimer = null;
    public Hashtable<String, Object> temporaryStuff = new Hashtable<>();
    public static final String CLDR_WEBCONTEXT = "cldr_webcontext";

    public static final String TARGET_ZOOMED = "CLDR-ST-ZOOMED";
    public static final String TARGET_EXAMPLE = "CLDR-ST-EXAMPLE";
    public static final String TARGET_DOCS = "CLDR-ST-DOCS";

    // private fields
    protected Writer out = null;
    private PrintWriter pw = null;
    String outQuery = null;
    TreeMap<String, String> outQueryMap = new TreeMap<>();
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
     * Flush output content. This is useful when JSPs are mixed in with servlet
     * code.
     *
     * @see java.io.PrintWriter#flush()
     */
    public void flush() {
        pw.flush();
    }

    /**
     * Return the parameter map of the underlying request.
     *
     * @return {@link ServletRequest#getParameterMap()}
     */
    public Map<?, ?> getParameterMap() {
        return request.getParameterMap();
    }

    /**
     * Construct a new WebContext from the servlet request and response. This is
     * the normal constructor to use when a top level servlet or JSP spins up.
     * Embedded JSPs should use fromRequest.
     *
     * @see #fromRequest(ServletRequest, ServletResponse, Writer)
     */
    public WebContext(HttpServletRequest irq, HttpServletResponse irs) throws IOException {
        setRequestResponse(irq, irs);
        setStream(irs.getWriter());
    }

    /**
     * Internal function to setup the WebContext to point at a servlet req/resp.
     * Also registers the WebContext with the Request.
     *
     * @param irq
     * @param irs
     * @throws IOException
     */
    protected void setRequestResponse(HttpServletRequest irq, HttpServletResponse irs) throws IOException {
        request = irq;
        response = irs;
        // register us - only if another webcontext is not already registered.
        if (request.getAttribute(CLDR_WEBCONTEXT) == null) {
            request.setAttribute(CLDR_WEBCONTEXT, this);
        }
    }

    /**
     * Change the output stream to a different writer. If it isn't a
     * PrintWriter, it will be wrapped in one. The WebContext will assume it
     * does not own the stream, and will not close it when done.
     *
     * @param w
     */
    protected void setStream(Writer w) {
        out = w;
        if (out instanceof PrintWriter) {
            pw = (PrintWriter) out;
        } else {
            pw = new PrintWriter(out, true);
        }
        dontCloseMe = true; // do not close the stream if the Response owns it.
    }

    /**
     * Extract (or create) a WebContext from a request/response. Call this from
     * a .jsp which is embedded in survey tool to extract the WebContext object.
     * The WebContext will have its output stream set to point to the request
     * and response, so you can mix write calls from the JSP with ST calls.
     *
     * @param request
     * @param response
     * @param out
     * @return the new WebContext, which was cloned from the one posted to the
     *         Request
     * @throws IOException
     */
    public static JspWebContext fromRequest(ServletRequest request, ServletResponse response, Writer out) throws IOException {
        WebContext ctx = (WebContext) request.getAttribute(CLDR_WEBCONTEXT);
        if (ctx == null) {
            throw new InternalError("WebContext: could not load fromRequest. Are you trying to load a JSP directly?");
        }
        JspWebContext subCtx = new JspWebContext(ctx); // clone the important
        // fields..
        subCtx.setRequestResponse((HttpServletRequest) request, // but use the
            // req/resp of
            // the current
            // situation
            (HttpServletResponse) response);
        subCtx.setStream(out);
        return subCtx;
    }

    /**
     * Copy one WebContext to another. This is useful when you wish to create a
     * sub-context which has a different base URL (such as for processing a
     * certain form or widget).
     *
     * @param other
     *            the other WebContext to copy from
     */
    public WebContext(WebContext other) {
        if ((other instanceof URLWebContext) && !(this instanceof URLWebContext)) {
            throw new InternalError("Can't slice a URLWebContext - use clone()");
        }
        init(other);
    }

    /**
     * get a field's value as a boolean
     *
     * @param x
     *            field name
     * @param def
     *            default value if field is not found.
     * @return the field value
     */
    boolean fieldBool(String x, boolean def) {
        if (field(x).length() > 0) {
            if (field(x).charAt(0) == 't') {
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
     *
     * @param x
     *            field name
     * @return the field's value as an integer, or -1 if it was not found
     */
    public final int fieldInt(String x) {
        return fieldInt(x, -1);
    }

    /**
     * get a field's value, or the default
     *
     * @param x
     *            field name
     * @param def
     *            default value
     * @return the field's value as an integer, or the default value if the
     *         field was not found.
     */
    public int fieldInt(String x, int def) {
        String f;
        if ((f = field(x)).length() > 0) {
            try {
                return new Integer(f).intValue();
            } catch (Throwable t) {
                return def;
            }
        } else {
            return def;
        }
    }

    /**
     * Return true if the field is present
     *
     * @param x
     *            field name
     * @return true if the field is present
     */
    public boolean hasField(String x) {
        return (request.getParameter(x) != null);
    }

    /**
     * return a field's value, else ""
     *
     * @param x
     *            field name
     * @return the field value, or else ""
     */
    public final String field(String x) {
        return field(x, "");
    }

    /**
     * return a field's value, else default
     *
     * @param x
     *            field name
     * @param def
     *            default value
     * @return the field's value as a string, otherwise the default
     */
    public String field(String x, String def) {
        if (request == null) {
            return def; // support testing
        }

        String res = request.getParameter(x);
        if (res == null) {
            return def; // don't try to transcode null.
        }
        return decodeFieldString(res);
    }

    /*
     * Decode a single string from URL format into Unicode
     *
     * @param res UTF-8 'encoded' bytes (expanded to a string)
     *
     * @return Unicode string (will return 'res' if no high bits were detected)
     */
    public static String decodeFieldString(String res) {
        if (res == null)
            return null;
        byte asBytes[] = new byte[res.length()];
        boolean wasHigh = false;
        int n;
        for (n = 0; n < res.length(); n++) {
            asBytes[n] = (byte) (res.charAt(n) & 0x00FF);
            // println(" n : " + (int)asBytes[n] + " .. ");
            if (asBytes[n] < 0) {
                wasHigh = true;
            }
        }
        if (wasHigh == false) {
            return res; // no utf-8
        } else {
            // println("[ trying to decode on: " + res + "]");
        }
        try {
            res = new String(asBytes, "UTF-8");
        } catch (Throwable t) {
            return res;
        }

        return res;
    }

    // preference api
    /**
     * get a preference's value as a boolean. defaults to false.
     *
     * @param x
     *            pref name
     * @return preference value (or false)
     */
    public boolean prefBool(String x) {
        return prefBool(x, false);
    }

    /**
     * Get a preference's value as an integer. Defaults to 'def'
     *
     * @param x
     *            field name
     * @param def
     *            default value
     * @return the prefence's value
     */
    int prefInt(String x, int def) {
        String f;
        if ((f = pref(x, "")).length() > 0) {
            try {
                return new Integer(f).intValue();
            } catch (Throwable t) {
                return def;
            }
        } else {
            return def;
        }
    }

    /**
     * Get a preference's value as an integer, or else -1
     *
     * @param x
     *            field name
     * @return preference value or -1
     */
    int prefInt(String x) {
        return prefInt(x, -1);
    }

    int codesPerPage = -1;

    /**
     * Special preference: Number of codes to show on a single page
     *
     * @return The preferred value (minimum: 5)
     * @see SurveyMain#CODES_PER_PAGE
     */
    int prefCodesPerPage() {
        if (codesPerPage == -1) {
            codesPerPage = prefInt(SurveyMain.PREF_CODES_PER_PAGE, SurveyMain.CODES_PER_PAGE);
            codesPerPage = Math.max(codesPerPage, 5);
        }
        return codesPerPage;
    }

    /**
     * get a preference's value as a boolean. defaults to defVal.
     *
     * @param x
     *            preference name
     * @param defVal
     *            default value
     * @return the preference value
     */
    boolean prefBool(String x, boolean defVal) {
        if (session == null) {
            return defVal;
        }
        boolean ret = fieldBool(x, session.prefGetBool(x, defVal));
        session.prefPut(x, ret);
        return ret;
    }

    /**
     * get a pref that is a string,
     *
     * @param x
     *            the field name and pref name
     * @return string preference value or "" if otherwise not found.
     */
    String pref(String x) {
        return pref(x, "");
    }

    /**
     * get a pref that is a string,
     *
     * @param x
     *            the field name and pref name
     * @param def
     *            default value
     * @return pref value or def
     */
    String pref(String x, String def) {
        String ret = field(x, session.prefGet(x));
        if (ret != null) {
            session.prefPut(x, ret);
        }
        if ((ret == null) || (ret.length() <= 0)) {
            ret = def;
        }
        return ret;
    }

    /**
     * Get the target keyword and value for an 'a href' HTML tag
     *
     * @param target
     *            the target name to use
     * @return the 'target=...' string - may be blank if the user has requested
     *         no popups
     */
    public String atarget(String t) {
        if (prefBool(SurveyMain.PREF_NOPOPUPS)) {
            return "";
        } else {
            return "target='" + t + "' ";
        }
    }

    /**
     * Get the target keyword and value for an 'a href' HTML tag on
     * TARGET_ZOOMED
     *
     * @return the 'target=...' string - may be blank if the user has requested
     *         no popups
     * @see #TARGET_ZOOMED
     */
    public String atarget() {
        return atarget(TARGET_ZOOMED);
    }

    /**
     * Add a parameter to the output URL
     *
     * @param k
     *            key
     * @param v
     *            value
     */
    public void addQuery(String k, String v) {
        outQueryMap.put(k, v);
        if (outQuery == null) {
            outQuery = k + "=" + v;
        } else {
            outQuery = outQuery + "&amp;" + k + "=" + v;
        }
    }

    /**
     * Copy from request queries to output query
     */
    public void addAllParametersAsQuery() {
        for (Entry<String, String[]> e : request.getParameterMap().entrySet()) {
            addQuery(e.getKey(), e.getValue()[0]);
        }
    }

    /**
     * Add a boolean parameter to the output URL as 't' or 'f'
     *
     * @param k
     *            key
     * @param v
     *            value
     */
    void addQuery(String k, boolean v) {
        addQuery(k, v ? "t" : "f");
    }

    /**
     * Set a parameter on the output URL, replacing an existing value if any
     *
     * @param k
     *            key
     * @param v
     *            value
     */
    public void setQuery(String k, String v) {
        if (outQueryMap.get(k) == null) { // if it wasn't there..
            addQuery(k, v); // then do a simple append
        } else {
            // rebuild query string:
            outQuery = null;
            TreeMap<String, String> oldMap = outQueryMap;
            oldMap.put(k, v); // replace
            outQueryMap = new TreeMap<>();
            for (Iterator<String> i = oldMap.keySet().iterator(); i.hasNext();) {
                String somek = i.next();
                addQuery(somek, oldMap.get(somek));
            }
        }
    }

    /**
     * Set a query from an integer
     *
     * @param k
     * @param v
     */
    public void setQuery(String k, int v) {
        setQuery(k, new Integer(v).toString());
    }

    /**
     * Remove the specified key from the query. Has no effect if the field
     * doesn't exist.
     *
     * @param k
     *            key
     */
    public void removeQuery(String k) {
        if (outQueryMap.get(k) != null) { // if it was there..
            // rebuild query string:
            outQuery = null;
            TreeMap<String, String> oldMap = outQueryMap;
            oldMap.remove(k); // replace
            outQueryMap = new TreeMap<>();
            for (Iterator<String> i = oldMap.keySet().iterator(); i.hasNext();) {
                String somek = i.next();
                addQuery(somek, oldMap.get(somek));
            }
        }
    }

    /**
     * Return the output URL
     *
     * @return the output URL
     */
    public String url() {
        if (outQuery == null) {
            return base();
        } else {
            return base() + "?" + outQuery;
        }
    }

    /**
     * Return the raw query string, or null
     * @return
     */
    public String query() {
        return outQuery;
    }

    /**
     * Returns the string that must be appended to the URL to start the next
     * parameter - either ? or &amp;
     *
     * @return the connecting string
     */
    public final String urlConnector() {
        return (url().indexOf('?') != -1) ? "&amp;" : "?";
    }

    /**
     * Get the base URL (servlet path)
     *
     * @return the servlet path in context
     */
    public String base() {
        if (theServletPath == null) {
            return context() + request.getServletPath();
        } else {
            return context() + theServletPath;
        }
    }

    public String vurl(CLDRLocale loc) {
        return vurl(loc, null, null, null);
    }

    /**
     * Get the new '/v' viewing URL. Note that this will include a fragment, do NOT append to the result (pass in something in queryAppend)
     * @param loc locale to view.
     * @param page pageID to view. Example:  PageId.Africa (shouldn't be null- yet)
     * @param strid strid to view. Example: "12345678" or null
     * @param queryAppend  this will be appended as the query. Example: "?email=foo@bar"
     * @return
     */
    public String vurl(CLDRLocale loc, PageId page, String strid, String queryAppend) {
        StringBuilder sb = new StringBuilder(request.getContextPath());
        return WebContext.appendContextVurl(sb, loc, page, strid, queryAppend).toString();
    }

    public static StringBuilder appendContextVurl(StringBuilder sb, CLDRLocale loc, PageId page, String strid, String queryAppend) {

        sb.append("/v");
        if (queryAppend != null && !queryAppend.isEmpty()) {
            sb.append(queryAppend);
        }
        sb.append('#'); // hash

        // locale
        sb.append('/');
        sb.append(loc.getBaseName());

        // page
        sb.append('/');
        if (page != null) {
            sb.append(page.name());
        }
        if (strid != null && !strid.isEmpty()) {
            sb.append('/');
            sb.append(strid);
        }

        return sb;
    }

    public void redirectToVurl(String vurl) {
        println("<a class='vredirect' href='" + vurl + "'>Redirecting to " + vurl + "</a>");
        println("<script>window.location=' " + vurl + "/'+window.location.hash.substring(1);</script>");
    }

    public void setServletPath(String path) {
        theServletPath = path;
    }

    private String theServletPath = null;

    /**
     * Get the base URL for some request
     *
     * @param request
     * @return base URL
     */
    public static String base(HttpServletRequest request) {
        return contextBase(request) + request.getServletPath();
    }

    /**
     * The base not including /servlet
     * @param request
     * @return
     */
    public static String contextBase(HttpServletRequest request) {
        return schemeHostPort(request) + request.getContextPath();
    }

    /**
     * Get the context path
     *
     * @return the context path
     */
    public String context() {
        return request.getContextPath();
    }

    /**
     * Get the context path for a certain resource
     *
     * @param s
     *            resource URL
     * @return the context path for the specified resource
     */
    public String context(String s) {
        if (request == null)
            return s;
        return context(request, s);
    }

    /**
     * Get the context path for a certain resource
     *
     * @param s
     *            resource URL
     * @return the context path for the specified resource
     */
    public static String context(HttpServletRequest request, String s) {
        if (request == null)
            return "/" + s;
        return request.getContextPath() + "/" + s;
    }

    /**
     * Get a link (HTML URL) to a JSP
     *
     * @param s
     *            resource to link to
     * @return the URL suitable for HTML
     */
    public String jspLink(String s) {
        return context(s) + "?a=" + base()
            + ((outQuery != null) ? ("&amp;" + outQuery) : ((session != null) ? ("&amp;s=" + session.id) : ""));
    }

    /**
     * Get a link (Text URL) to a JSP
     *
     * @param s
     *            resource to link to
     * @return the URL suitable for Text
     */
    public String jspUrl(String s) {
        return context(s) + "?a=" + base()
            + ((outQuery != null) ? ("&" + outQuery) : ((session != null) ? ("&s=" + session.id) : ""));
    }

    /**
     * Output the full current output URL in hidden field format.
     */
    void printUrlAsHiddenFields() {
        for (Iterator<String> e = outQueryMap.keySet().iterator(); e.hasNext();) {
            String k = e.next().toString();
            String v = outQueryMap.get(k).toString();
            print("<input type='hidden' name='" + k + "' value='" + v + "'/>");
        }
    }

    /**
     * return the IP of the remote user. If they are behind a proxy, return the
     * actual original URL.
     *
     * @return a URL
     */
    String userIP() {
        return userIP(request);
    }

    /**
     * return the IP of the remote user given a request. If they are behind a
     * proxy, return the actual original URL.
     *
     * @param request
     *            the request to use
     * @return a URL
     */
    public static String userIP(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * return the hostname of the web server
     *
     * @return the Server Name
     */
    String serverName() {
        return request.getServerName();
    }

    /**
     * return the hostname of the web server given a request
     *
     * @return the Server name
     */
    static String serverName(HttpServletRequest request) {
        return request.getServerName();
    }

    /**
     * Returns the host:port of the server
     *
     * @return the "host:port:
     */
    String serverHostport() {
        return serverHostport(request);
    }

    /**
     * Returns the host:port of the server
     *
     * @param request
     *            a specific request
     * @return the "host:port:
     */
    static String serverHostport(HttpServletRequest request) {
        int port = request.getServerPort();
        String scheme = request.getScheme();
        if (port == 80 && "http".equals(scheme)) {
            return serverName(request);
        } else if (port == 443 && "https".equals(scheme)) {
            return serverName(request);
        } else {
            return serverName(request) + ":" + port;
        }
    }

    /**
     * Returns the scheme://host:port
     *
     * @return the "scheme://host:port"
     */
    String schemeHostPort() {
        return schemeHostPort(request);
    }

    /**
     * Returns the scheme://host:port
     *
     * @return the "scheme://host:port"
     * @param request
     *            the request portion
     */
    static String schemeHostPort(HttpServletRequest request) {
        return request.getScheme() + "://" + serverHostport(request);
    }

    /**
     * Print out a line
     *
     * @param s
     *            line to print
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
     *
     * @param t
     *            throwable to print
     */
    void print(Throwable t) {
        print("<pre style='border: 2px dashed red; margin: 1em; padding: 1'>" + t.toString() + "<br />");
        StringWriter asString = new StringWriter();
        if (t instanceof SQLException) {
            println("SQL: " + DBUtils.unchainSqlException((SQLException) t));
        } else {
            t.printStackTrace(new PrintWriter(asString));
        }
        print(asString.toString());
        print("</pre>");
    }

    /**
     * Send the user to another URL. Won't work if there was already some
     * output.
     *
     * @param where
     * @see HttpServletResponse#sendRedirect(String)
     */
    void redirect(String where) {
        try {
            response.sendRedirect(where);
            out.close();
            close();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe.toString() + " while redirecting to " + where);
        }
    }

    /**
     * Close the stream. Normally not called directly, except in outermost
     * processor.
     *
     * @throws IOException
     */
    void close() throws IOException {
        if (!dontCloseMe) {
            out.close();
            out = null;
        } else {
            closeUserFile();
        }
    }

    // doc api

    /**
     * the current processor
     *
     * @see DisplayAndInputProcessor
     */
    public DisplayAndInputProcessor processor = null;

    /**
     * Set this context to be handling a certain locale
     *
     * @param l
     *            locale to set
     */
    public void setLocale(CLDRLocale l) {
        if (!SurveyMain.getLocalesSet().contains(l)) { // bogus
            locale = null;
            return;
        }
        locale = l;
        // localeString = locale.getBaseName();
        processor = new DisplayAndInputProcessor(l, false);
        Vector<CLDRLocale> localesVector = new Vector<>();
        for (CLDRLocale parents : locale.getParentIterator()) {
            localesVector.add(parents);
        }
        docLocale = localesVector.toArray(docLocale);
        // logger.info("NOT NOT NOT fetching locale: " + l.toString() +
        // ", count: " + doc.length);
    }

    /**
     * Cached direction of this locale.
     */
    private HTMLDirection direction = null;

    /**
     * Return the HTML direction of this locale, ltr or rtl. Returns ltr by
     * default. TODO: should return display locale's directionality by default.
     *
     * @return directionality
     */
    public HTMLDirection getDirectionForLocale() {
        if ((locale != null) && (direction == null)) {
            direction = sm.getHTMLDirectionFor(getLocale());
        }
        if (direction == null) {
            return HTMLDirection.LEFT_TO_RIGHT;
        } else {
            return direction;
        }
    }

    /**
     * Return the current locale as a string. Deprecated, please use getLocale
     * instead.
     *
     * @deprecated use getLocale().toString() -
     * @see #getLocale()
     */
    @Deprecated
    public final String localeString() {
        if (locale == null) {
            throw new InternalError("localeString is null, locale=" + locale);
        }
        return locale.toString();
    }

    /**
     * Print the coverage level for a certain locale.
     */
    public void showCoverageLevel() {
        String itsLevel = getEffectiveCoverageLevel();
        String recLevel = getRecommendedCoverageLevel();
        String def = getRequiredCoverageLevel();
        if (def.equals(COVLEV_RECOMMENDED)) {
            print("Coverage Level: <tt class='codebox'>" + itsLevel.toString() + "</tt><br>");
        } else {
            print("Coverage Level: <tt class='codebox'>" + def + "</tt>  (overriding <tt>" + itsLevel.toString() + "</tt>)<br>");
        }
        print("Recommended level: <tt class='codebox'>" + recLevel.toString() + "</tt><br>");
        print("<ul><li>To change your default coverage level, see ");
        SurveyMain.printMenu(this, "", "options", "My Options", SurveyMain.QUERY_DO);
        println("</li></ul>");
    }

    public String getEffectiveCoverageLevel(CLDRLocale locale) {
        return getEffectiveCoverageLevel(locale.getBaseName());
    }

    public String getEffectiveCoverageLevel(String locale) {
        String level = getRequiredCoverageLevel();
        if ((level == null) || (level.equals(COVLEV_RECOMMENDED)) || (level.equals("default"))) {
            // fetch from org
            level = session.getOrgCoverageLevel(locale);
        }
        return level;
    }

    public String getRecommendedCoverageLevel() {
        String myOrg = session.getUserOrg();
        if ((myOrg == null) || !isCoverageOrganization(myOrg)) {
            return COVLEV_DEFAULT_RECOMMENDED_STRING;
        } else {
            CLDRLocale loc = getLocale();
            if (loc != null) {
                Level lev = StandardCodes.make().getLocaleCoverageLevel(myOrg, loc.toString());
                if (lev == Level.UNDETERMINED) {
                    lev = COVLEVEL_DEFAULT_RECOMMENDED;
                }
                return lev.toString();
            } else {
                return COVLEVEL_DEFAULT_RECOMMENDED.toString();
            }
        }
    }

    public String showCoverageSetting() {
        String rv = sm.showListSetting(this, SurveyMain.PREF_COVLEV, "Coverage", WebContext.PREF_COVLEV_LIST);
        return rv;
    }

    public String showCoverageSettingForLocale() {
        String rv = sm.showListSetting(this, SurveyMain.PREF_COVLEV, "Coverage", WebContext.PREF_COVLEV_LIST,
            getRecommendedCoverageLevel());
        return rv;
    }

    public String getCoverageSetting() {
        return sm.getListSetting(this, SurveyMain.PREF_COVLEV, WebContext.PREF_COVLEV_LIST, false);
    }

    public static final String COVLEV_RECOMMENDED = "default";
    public static final String PREF_COVLEV_LIST[] = { COVLEV_RECOMMENDED,
        Level.COMPREHENSIVE.toString(), Level.MODERN.toString(), Level.MODERATE.toString(), Level.BASIC.toString() };

    /**
     * The default level, if no organization is available.
     */
    public static final Level COVLEVEL_DEFAULT_RECOMMENDED = org.unicode.cldr.util.Level.MODERN;
    public static final String COVLEV_DEFAULT_RECOMMENDED_STRING = COVLEVEL_DEFAULT_RECOMMENDED.name().toLowerCase();

    /**
     * Is it an organization that participates in coverage?
     *
     * @param org the name of the organization
     * @return true if the organization participates in coverage, else false
     */
    public static boolean isCoverageOrganization(String org) {
        return (org != null && StandardCodes.make().getLocaleCoverageOrganizationStrings().contains(org.toLowerCase()));
    }

    /**
     * Get a list of all locale types
     *
     * @return a list of locale types
     * @see StandardCodes#getLocaleCoverageOrganizations()
     */
    static String[] getLocaleCoverageOrganizations() {
        Set<Organization> set = StandardCodes.make().getLocaleCoverageOrganizations();
        Organization[] orgArray = set.toArray(new Organization[0]);
        String[] stringArray = new String[orgArray.length];
        int i = 0;
        for (Organization org : orgArray) {
            stringArray[i++] = org.toString();
        }
        return stringArray;
        // There was ArrayStoreException here:
        // return StandardCodes.make().getLocaleCoverageOrganizations().toArray(new String[0]);
    }

    /**
     * Append the WebContext Options map to the specified map
     *
     * @return the map
     */
    public CheckCLDR.Options getOptionsMap() {
        String def = getRequiredCoverageLevel();
        String org = getEffectiveCoverageLevel();

        return new CheckCLDR.Options(getLocale(), SurveyMain.getTestPhase(), def, org);
    }

    /**
     * @return
     */
    public String getEffectiveCoverageLevel() {
        String org = getEffectiveCoverageLevel(getLocale().toString());
        return org;
    }

    /**
     * @return
     */
    public String getRequiredCoverageLevel() {
        String def = sm.getListSetting(this, SurveyMain.PREF_COVLEV, WebContext.PREF_COVLEV_LIST, false);
        return def;
    }

    public enum LoadingShow {
        dontShowLoading, showLoading
    }

    private static final boolean CACHE_DATA_SECTION = false; // TESTING, not ready for use

    private static final Map<String, DataSection> dataSectionCache = CACHE_DATA_SECTION ? new ConcurrentHashMap<>() : null;

    /**
     * Get a DataSection
     *
     * @param prefix a string such as "//ldml"; or null
     * @param matcher the XPathMatcher (which is ... ?); or null
     * @param pageId the PageId, with a name such as "Generic" and a SectionId with a name such as "DateTime"; or null
     * @return the DataSection
     *
     * Called only by SurveyAjax.getRow, twice:
     *    ctx.getDataSection(null [prefix], null [matcher], pageId);
     *    ctx.getDataSection(baseXp [prefix], matcher, null [pageId]);
     *
     * TODO: as part of DataSection performance improvement, consider moving this code to a different
     * module, maybe DataSection itself, especially if we can make DataSection not be user-specific.
     * WebContext is user-specific, and even request-specific.
     *
     * Renamed 2019-05-15 from getSection (5 args) to getDataSection
     *
     * Reference: https://unicode-org.atlassian.net/browse/CLDR-12020
     */
    public DataSection getDataSection(String prefix, XPathMatcher matcher, PageId pageId) {

        DataSection section = null;
        synchronized (this) {
            String cacheKey = null;
            if (CACHE_DATA_SECTION) {
                cacheKey = locale.toString(); // not enough!
                section = dataSectionCache.get(cacheKey);
            }
            if (section == null) {
                CLDRProgressTask progress = sm.openProgress("Loading");
                try {
                    progress.update("<span title='" + sm.xpt.getPrettyPath(prefix) + "'>" + locale + "</span>");
                    flush();
                    synchronized (session) {
                        section = DataSection.make(pageId, this /* ctx */, this.session, locale, prefix, matcher);
                    }
                } finally {
                    progress.close(); // TODO: this can trigger "State Error: Closing an already-closed CLDRProgressIndicator"
                }
                if (section == null) {
                    throw new InternalError("No section.");
                }
                if (CACHE_DATA_SECTION) {
                    dataSectionCache.put(cacheKey, section);
                }
            }
        }
        return section;
    }

    // Internal Utils

    static final HelpMessages surveyToolHelpMessages = new HelpMessages("test_help_messages.html");
    public static final String CAN_MODIFY = "canModify";
    public static final String DATA_SECTION = "DataSection";
    public static final String ZOOMED_IN = "zoomedIn";
    public static final String DATA_ROW = "DataRow";
    public static final String BASE_EXAMPLE = "baseExample";
    public static final String BASE_VALUE = "baseValue";

    /**
     * Print a link to help with a specified title
     *
     * @param what
     *            the help to link to
     * @param title
     *            the title of the help
     */
    public void printHelpLink(String what, String title) {
        printHelpLink(what, title, true);
    }

    /**
     * @param what
     * @param title
     * @param doEdit
     * @deprecated editing is deprecated
     */
    @Deprecated
    public void printHelpLink(String what, String title, boolean doEdit) {
        printHelpLink(what, title, doEdit, true);
    }

    /**
     * @deprecated editing is deprecated
     * @param what
     * @param title
     * @param doEdit
     * @param parens
     */
    @Deprecated
    private void printHelpLink(String what, String title, boolean doEdit, boolean parens) {
        if (parens) {
            print("(");
        }
        print("<a href=\"" + (SurveyMain.CLDR_HELP_LINK) + what + "\">" + title + "</a>");
        if (parens) {
            println(")");
        }
    }

    /**
     * Get HTML for the 'modify' thing, with the hand icon
     *
     * @param message
     * @see #iconHtml(String, String)
     * @return HTML for the message
     */
    public String modifyThing(String message) {
        return iconHtml("hand", message);
    }

    /**
     * get HTML for an icon with a certain message
     *
     * @param icon
     * @param message
     * @return the HTML for the icon and message
     */
    public String iconHtml(String icon, String message) {
        return iconHtml(request, icon, message);
    }

    public static String iconHtml(HttpServletRequest request, String icon, String message) {
        if (message == null) {
            message = "[" + icon + "]";
        }
        return "<img alt='[" + icon + "]' style='width: 16px; height: 16px; border: 0;' src='" + context(request, icon + ".png")
            + "' title='" + message + "' />";
    }

    /**
     * Clone (copy construct) the context
     *
     * @see #WebContext(WebContext)
     */
    @Override
    public Object clone() {
        Object o;
        try {
            o = super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
        WebContext n = (WebContext) o;
        n.init(this);

        return o;
    }

    private void init(WebContext other) {
        doc = other.doc;
        docLocale = other.docLocale;
        displayLocale = other.displayLocale;
        out = other.out;
        pw = other.pw;
        outQuery = other.outQuery;
        locale = other.locale;
        session = other.session;
        outQueryMap = (TreeMap<String, String>) other.outQueryMap.clone();
        dontCloseMe = true;
        request = other.request;
        response = other.response;
        sm = other.sm;
        processor = other.processor;
        temporaryStuff = other.temporaryStuff;
        theServletPath = other.theServletPath;
    }

    /**
     * Include a template fragment from /WEB-INF/tmpl
     *
     * @param filename
     */
    public void includeFragment(String filename) {
        try {
            WebContext.includeFragment(request, response, filename);
        } catch (Throwable t) {
            SurveyLog.logException(t, "Including template " + filename);
            this.println("<div class='ferrorbox'><B>Error</b> while including template <tt class='code'>" + filename
                + "</tt>:<br>");
            this.print(t);
            this.println("</div>");
            System.err.println("While expanding " + TMPL_PATH + filename + ": " + t.toString());
            t.printStackTrace();
        }
    }

    /**
     * Include a template fragment from /WEB-INF/tmpl
     *
     * @param request
     * @param response
     * @param filename
     * @throws ServletException
     * @throws IOException
     *             , NullPointerException
     */
    public static void includeFragment(HttpServletRequest request, HttpServletResponse response, String filename)
        throws ServletException, IOException {
        RequestDispatcher dp = request.getRequestDispatcher(TMPL_PATH + filename);
        dp.include(request, response);
    }

    /**
     * Put something into the temporary (context, non session data) store
     *
     * @param string
     * @param object
     */
    public void put(String string, Object object) {
        if (object == null) {
            temporaryStuff.remove(string);
        } else {
            temporaryStuff.put(string, object);
        }
    }

    /**
     * Get something from the temporary (context, non session data) store
     *
     * @param string
     * @return the object
     */
    public Object get(String string) {
        return temporaryStuff.get(string);
    }

    public String getString(String k) {
        return (String) get(k);
    }

    /**
     * @return the CLDRLocale with which this WebContext currently pertains.
     * @see CLDRLocale
     */
    public CLDRLocale getLocale() {
        return locale;
    }

    // Display Context Data
    protected Boolean canModify = null;

    /**
     * A direction, suitable for html 'dir=...'
     *
     * @author srl
     * @see getDirectionForLocale
     */
    public enum HTMLDirection {
        LEFT_TO_RIGHT("ltr"), RIGHT_TO_LEFT("rtl");

        private String str;

        HTMLDirection(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }

        /**
         * Convert a CLDR direction to an enum
         *
         * @param dir
         *            CLDR direction string
         * @return HTML direction enum
         */
        public static HTMLDirection fromCldr(String dir) {
            if (dir.equals("left-to-right")) {
                return HTMLDirection.LEFT_TO_RIGHT;
            } else if (dir.equals("right-to-left")) {
                return HTMLDirection.RIGHT_TO_LEFT;
            } else if (dir.equals("top-to-bottom")) {
                return HTMLDirection.LEFT_TO_RIGHT; // !
            } else {
                return HTMLDirection.LEFT_TO_RIGHT;
            }
        }
    }

    /**
     * Return true if the user can modify this locale
     *
     * @return true if the user can modify this locale
     */
    public Boolean canModify() {
        if (STFactory.isReadOnlyLocale(locale) || SurveyMain.phase() == Phase.READONLY)
            return (canModify = false);
        if (canModify == null) {
            if (session != null && session.user != null) {
                canModify = UserRegistry.userCanModifyLocale(session.user, locale);
            } else {
                canModify = false;
            }
        }
        return canModify;
    }

    public void includeAjaxScript(AjaxType type) {
        try {
            SurveyAjax.includeAjaxScript(request, response, type);
        } catch (Throwable t) {
            this.println("<div class='ferrorbox'><B>Error</b> while expanding ajax template ajax_" + type.name().toLowerCase()
                + ".jsp:<br>");
            this.print(t);
            this.println("</div>");
            System.err.println("While expanding ajax: " + t.toString());
            t.printStackTrace();
        }
    }

    public SurveyMain.UserLocaleStuff getUserFile() {
        SurveyMain.UserLocaleStuff uf = peekUserFile();
        if (uf == null) {
            uf = sm.getUserFile(session, getLocale());
            put("UserFile", uf);
        }
        return uf;
    }

    private UserLocaleStuff peekUserFile() {
        return (UserLocaleStuff) get("UserFile");
    }

    public void closeUserFile() {
        UserLocaleStuff uf = (UserLocaleStuff) temporaryStuff.remove("UserFile");
        if (uf != null) {
            uf.close();
        }
    }

    public CLDRFile getCLDRFile() {
        return getUserFile().cldrfile;
    }

    /**
     * Get the user settings.
     *
     * @return
     */
    UserSettings settings() {
        return session.settings();
    }

    public void no_js_warning() {
        boolean no_js = prefBool(SurveyMain.PREF_NOJAVASCRIPT);
        if (!no_js) {
            WebContext nuCtx = (WebContext) clone();
            nuCtx.setQuery(SurveyMain.PREF_NOJAVASCRIPT, "t");
            println("<noscript><h1>");
            println(iconHtml("warn", "JavaScript disabled") + "JavaScript is disabled. Please enable JavaScript..");
            println("</h1></noscript>");
        }
    }

    /**
     * Return the ID of this user, or -1 (UserRegistry.NO_USER)
     *
     * @return user's id, or -1 (UserRegistry.NO_USER) if not found/not set
     */
    public int userId() {
        if (session != null && session.user != null) {
            return session.user.id;
        } else {
            return UserRegistry.NO_USER;
        }
    }

    private User user() {
        if (session != null && session.user != null) {
            return session.user;
        } else {
            return null;
        }
    }

    /**
     * Get a certain cookie
     *
     * @param id
     * @return
     */
    public Cookie getCookie(String id) {
        return getCookie(request, id);
    }

    public static Cookie getCookie(HttpServletRequest request, String id) {
        if (request == null) return null;
        Cookie cooks[] = request.getCookies();
        if (cooks == null)
            return null;
        for (Cookie c : cooks) {
            if (c.getName().equals(id)) {
                return c;
            }
        }
        return null;
    }

    /**
     * Get a cookie value or null
     *
     * @param id
     * @return
     */
    public String getCookieValue(String id) {
        Cookie c = getCookie(id);
        if (c != null) {
            return c.getValue();
        }
        return null;
    }

    /**
     * Set a cookie
     *
     * @param id
     * @param value
     * @param expiry
     */
    Cookie addCookie(String id, String value, int expiry) {
        Cookie c = new Cookie(id, value);
        c.setMaxAge(expiry);
        response.addCookie(c);
        return c;
    }

    @Override
    public Appendable append(CharSequence csq) throws IOException {
        pw.append(csq);
        return this;
    }

    @Override
    public Appendable append(char c) throws IOException {
        pw.append(c);
        return this;
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end) throws IOException {
        pw.append(csq, start, end);
        return this;
    }

    @Override
    public String toString() {
        return "{ " + this.getClass().getName() + ": url=" + url() + ", ip=" + this.userIP() + ", user=" + this.user() + "}";
    }

    /**
     * @return
     */
    public String getAlign() {
        String ourAlign = "left";
        if (getDirectionForLocale().equals(HTMLDirection.RIGHT_TO_LEFT)) {
            ourAlign = "right";
        }
        return ourAlign;
    }

    /**
     * @return
     */
    public boolean getCanModify() {
        boolean canModify = (UserRegistry.userCanModifyLocale(session.user, getLocale()));
        return canModify;
    }

    /**
     * @param locale
     * @return
     */
    String urlForLocale(CLDRLocale locale) {
        return vurl(locale, null, null, null);
    }

    public String getLocaleDisplayName(String loc) {
        return getLocaleDisplayName(CLDRLocale.getInstance(loc));
    }

    public String getLocaleDisplayName(CLDRLocale instance) {
        return instance.getDisplayName();
    }

    /**
     * @return
     */
    public boolean hasAdminPassword() {
        return field("dump").equals(SurveyMain.vap);
    }

    public boolean hasTestPassword() {
        return field("dump").equals(SurveyMain.testpw) || hasAdminPassword();
    }

    private static UnicodeSet csvSpecial = new UnicodeSet("[, \"]").freeze();

    public static final void csvWrite(Writer out, String str) throws IOException {
        str = str.trim();
        if (csvSpecial.containsSome(str)) {
            out.write('"');
            out.write(str.replaceAll("\"", "\"\""));
            out.write('"');
        } else {
            out.write(str);
        }
    }

    private boolean checkedPage = false;
    private PageId pageId = null;

    public PageId getPageId() {
        if (!checkedPage) {
            String pageField = field(SurveyMain.QUERY_SECTION);
            pageId = getPageId(pageField);
            checkedPage = true;
        }
        return pageId;
    }

    /**
     * set the session. Only call this once.
     */
    public String setSession() {
        if (request == null && session != null) {
            return "using canned session"; // already set - for testing
        }

        if (this.session != null) return "Internal error - session already set.";

        CookieSession.checkForExpiredSessions(); // If a session has expired, remove it

        String message = null; // return message, explaining what happened with the session.

        String myNum = field(SurveyMain.QUERY_SESSION); // s
        String password = field(SurveyMain.QUERY_PASSWORD);
        if (password.isEmpty()) {
            password = field(SurveyMain.QUERY_PASSWORD_ALT);
        }
        boolean letmein = SurveyMain.vap.equals(field("letmein")); // back door, using master password
        String email = field(SurveyMain.QUERY_EMAIL);
        if ("admin@".equals(email) && SurveyMain.vap.equals(password)) {
            letmein = true;
        }

        // if there was an email/password in the cookie, use that.
        {
            String myEmail = getCookieValue(SurveyMain.QUERY_EMAIL);
            String myPassword = getCookieValue(SurveyMain.QUERY_PASSWORD);
            if (myEmail != null && (email == null || email.isEmpty())) {
                email = myEmail;
                if (myPassword != null && (password == null || password.isEmpty())) {
                    password = myPassword;
                }
            } else {
                if (myEmail != null && !myEmail.equals(email)) {
                    removeLoginCookies(request, response);
                }
            }
        }

        User user = null;
        // if an email/password given, try to fetch a user
        try {
            user = CookieSession.sm.reg.get(password, email, userIP(), letmein);
        } catch (LogoutException e) {
            logout(); // failed login, so logout this session.
        }
        if (user != null) {
            user.touch(); // mark this user as active.
        }

        HttpSession httpSession = request.getSession(true); // create httpsession

        //boolean idFromSession = false; // did the id come from the httpsession? (and why do we care?)

        if (myNum.equals(SurveyMain.SURVEYTOOL_COOKIE_NONE)) { // "0"- for testing
            httpSession.removeAttribute(SurveyMain.SURVEYTOOL_COOKIE_SESSION);
        }

        // we just logged in- see if there's already a user session for this user..
        if (user != null) {
            session = CookieSession.retrieveUser(user.email); // is this user already logged in?
            if (session != null) {
                if (null == CookieSession.retrieve(session.id)) { // double check- is the session still valid?
                    session = null; // don't allow dead sessions to show up
                    // via the user list.
                }
            }
        }

        // Retreive a number from the httpSession if present
        if ((httpSession != null) && (session == null)) {
            String aNum = (String) httpSession.getAttribute(SurveyMain.SURVEYTOOL_COOKIE_SESSION);
            if (aNum != null) {
                session = CookieSession.retrieve(aNum);
                if (CookieSession.DEBUG_INOUT) {
                    System.err.println("From httpsession " + httpSession.getId() + " = ST session " + aNum + " = " + session);
                }
            }
        }

        if (session != null && session.user != null && user != null && user.id != session.user.id) {
            session = null; // user was already logged in as 'session.user', replacing this with 'user'
        }

        if ((user == null) &&
            (hasField(SurveyMain.QUERY_PASSWORD) || hasField(SurveyMain.QUERY_EMAIL))) {
            if (CookieSession.DEBUG_INOUT) {
                System.out.println("Logging out - mySession=" + session + ",user=" + user + ", and had em/pw");
            }
            logout(); // zap cookies if some id/pw failed to work
        }

        if (session == null && user == null) {
            session = CookieSession.checkForAbuseFrom(userIP(), SurveyMain.BAD_IPS, request.getHeader("User-Agent"));
            if (session != null) {
                println("<h1>Note: Your IP, " + userIP() + " has been throttled for making " + SurveyMain.BAD_IPS.get(userIP())
                    + " connections. Try turning on cookies, or obeying the 'META ROBOTS' tag.</h1>");
                flush();
                session = null;
                return "Bad IP.";
            }
        }

        if (CookieSession.DEBUG_INOUT) System.out.println("Session Now=" + session + ", user=" + user);

        // guest?
        if (letmein || (user != null && UserRegistry.userIsTC(user))) {
            // allow in administrator or TC.
        } else if ((user != null) && (session == null)) { // user trying to log in-
            if (CookieSession.tooManyUsers()) {
                System.err.println("Refused login for " + email + " from " + userIP() + " - too many users ( " + CookieSession.getUserCount() + ")");
                return "We are swamped with about " + CookieSession.getUserCount()
                    + " people using the SurveyTool right now! Please try back in a little while.";
            }
        } else if (session == null || (session.user == null)) { // guest user
            if (CookieSession.tooManyGuests()) {
                if (session != null) {
                    System.err.println("Logged-out guest  " + session.id + " from " + userIP() + " - too many users ( " + CookieSession.getUserCount() + ")");
                    session.remove(); // remove guests at this point
                    session = null;
                }
                logout(); // clear session cookie
                return "We have too many people browsing the CLDR Data on the Survey Tool. Please try again later when the load has gone down.";
            }
        }

        // New up a session, if we don't already have one.
        if (session == null) {
            session = CookieSession.newSession(user == null, userIP(), httpSession.getId());
            if (!myNum.equals(SurveyMain.SURVEYTOOL_COOKIE_NONE)) {
                // ctx.println("New session: " + mySession.id + "<br>");
            }
        }

        // should we add "&s=.#####" to the URL?
        if (httpSession.isNew()) { // If it's a new session..
            addQuery(SurveyMain.QUERY_SESSION + "__", session.id);
        } else {
            // ctx.println("['s' suppressed]");
        }

        // store the session id in the HttpSession
        httpSession.setAttribute(SurveyMain.SURVEYTOOL_COOKIE_SESSION, session.id);
        httpSession.setMaxInactiveInterval(-1); // never expire

        if (user != null) {
            session.setUser(user); // this will replace any existing session by this user.
            session.user.ip = userIP();
        } else {
            if ((email != null) && (email.length() > 0) && (session.user == null)) {
                String encodedEmail;
                try {
                    encodedEmail = URLEncoder.encode(email, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    // The server doesn't support UTF-8?  (Should never happen)
                    throw new RuntimeException(e);
                }
                message = iconHtml("stop", "failed login") + "login failed. <a href='"
                    + request.getContextPath() + "/reset.jsp"
                    + "?email=" + encodedEmail
                    + "&s=" + session.id
                    + "' id='notselected'>recover password?</a><br>";
            }
        }
        // processs the 'remember me'
        if (session != null && session.user != null) {
            if (hasField(SurveyMain.QUERY_SAVE_COOKIE)) {
                if (SurveyMain.isUnofficial()) {
                    System.out.println("Remembering user: " + session.user);
                }
                loginRemember(session.user);
            }
        }
        return message;
    }

    /**
     * Logout this ctx
     */
    public void logout() {
        logout(request, response);
    }

    /**
     * Logout this req/response (zap cookie)
     * Zaps http session
     * @param request
     * @param response
     */
    public static void logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false); // dont create
        if (session != null) {
            String sessionId = (String) session.getAttribute(SurveyMain.SURVEYTOOL_COOKIE_SESSION);
            if (CookieSession.DEBUG_INOUT) {
                System.err.println("logout() of session " + session.getId() + " and cookieSession " + sessionId);
            }
            if (sessionId != null) {
                CookieSession sess = CookieSession.retrieveWithoutTouch(sessionId);
                if (sess != null) {
                    sess.remove(); // forcibly remove session
                }
            }
            session.removeAttribute(SurveyMain.SURVEYTOOL_COOKIE_SESSION);
        }
        removeLoginCookies(request, response);
    }

    /**
     * @param request
     * @param response
     */
    public static void removeLoginCookies(HttpServletRequest request, HttpServletResponse response) {
        Cookie c0 = WebContext.getCookie(request, SurveyMain.QUERY_EMAIL);
        if (c0 != null) { // only zap extant cookies
            c0.setValue("");
            c0.setMaxAge(0);
            response.addCookie(c0);
        }
        Cookie c1 = WebContext.getCookie(request, SurveyMain.QUERY_PASSWORD);
        if (c1 != null) {
            c1.setValue("");
            c1.setMaxAge(0);
            response.addCookie(c1);
        }
    }

    public static PageId getPageId(String pageField) {
        if (pageField != null && !pageField.isEmpty()) {
            try {
                return PathHeader.PageId.forString(pageField);
            } catch (Exception e) {
                // ignore.
            }
        }
        return null;
    }

    /**
     * Remember this login (adds cookie to ctx.response )
     */
    public void loginRemember(User user) {
        addCookie(SurveyMain.QUERY_EMAIL, user.email, SurveyMain.TWELVE_WEEKS);
        addCookie(SurveyMain.QUERY_PASSWORD, user.password, SurveyMain.TWELVE_WEEKS);
    }
}
