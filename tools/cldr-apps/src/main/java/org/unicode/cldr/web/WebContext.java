//
//  WebContext.java
//  cldrtools
//
//  Created by Steven R. Loomis on 3/11/2005.
//  Copyright 2005-2012 IBM. All rights reserved.
//
package org.unicode.cldr.web;

import com.ibm.icu.dev.util.ElapsedTimer;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.unicode.cldr.test.DisplayAndInputProcessor;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.web.SurveyMain.Phase;
import org.unicode.cldr.web.SurveyMain.UserLocaleStuff;
import org.unicode.cldr.web.UserRegistry.LogoutException;
import org.unicode.cldr.web.UserRegistry.User;
import org.unicode.cldr.web.api.Auth;
import org.w3c.dom.Document;

/**
 * This is the per-client context passed to basically all functions it has print*() like functions,
 * and so can be written to.
 */
public class WebContext implements Cloneable, Appendable {
    public static final String TMPL_PATH = "/WEB-INF/tmpl/";
    private static final java.util.logging.Logger logger = SurveyLog.forClass(WebContext.class);
    // USER fields
    public SurveyMain sm = null;
    public Document[] doc = new Document[0];
    private CLDRLocale locale = null;
    public ULocale displayLocale = SurveyMain.TRANS_HINT_LOCALE;
    public CLDRLocale[] docLocale = new CLDRLocale[0];
    public CookieSession session = null;
    public ElapsedTimer reqTimer = null;
    public Hashtable<String, Object> temporaryStuff = new Hashtable<>();
    public static final String CLDR_WEBCONTEXT = "cldr_webcontext";

    public static final String TARGET_ZOOMED = "CLDR-ST-ZOOMED";
    public static final String TARGET_DOCS = "CLDR-ST-DOCS";

    private static final String LOGIN_FAILED = "login failed";

    // private fields
    protected Writer out = null;
    private PrintWriter pw = null;
    String outQuery = null;
    TreeMap<String, String> outQueryMap = new TreeMap<>();
    boolean dontCloseMe = false;
    HttpServletRequest request;
    HttpServletResponse response;

    /**
     * @return the output PrintWriter
     */
    public PrintWriter getOut() {
        return pw;
    }

    /**
     * Flush output content. This is useful when JSPs are mixed in with servlet code.
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
     *     <p>WARNING: this is accessed by st_footer.jsp
     */
    public Map<?, ?> getParameterMap() {
        return request.getParameterMap();
    }

    /**
     * Construct a new WebContext from the servlet request and response. This is the normal
     * constructor to use when a top level servlet or JSP spins up. Embedded JSPs should use
     * fromRequest.
     *
     * @see #fromRequest(ServletRequest, ServletResponse, Writer)
     */
    public WebContext(HttpServletRequest irq, HttpServletResponse irs) throws IOException {
        setRequestResponse(irq, irs);
        setStream(irs.getWriter());
    }

    /**
     * Internal function to setup the WebContext to point at a servlet req/resp. Also registers the
     * WebContext with the Request.
     *
     * @param irq
     * @param irs
     */
    protected void setRequestResponse(HttpServletRequest irq, HttpServletResponse irs) {
        request = irq;
        response = irs;
        // register us - only if another webcontext is not already registered.
        if (request.getAttribute(CLDR_WEBCONTEXT) == null) {
            request.setAttribute(CLDR_WEBCONTEXT, this);
        }
    }

    /**
     * Change the output stream to a different writer. If it isn't a PrintWriter, it will be wrapped
     * in one. The WebContext will assume it does not own the stream, and will not close it when
     * done.
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
     * Extract (or create) a WebContext from a request/response. Call this from a .jsp which is
     * embedded in survey tool to extract the WebContext object. The WebContext will have its output
     * stream set to point to the request and response, so you can mix write calls from the JSP with
     * ST calls.
     *
     * @param request
     * @param response
     * @param out
     * @return the new WebContext, which was cloned from the one posted to the Request
     *     <p>WARNING: this is accessed by stcontext.jspf
     */
    public static JspWebContext fromRequest(
            ServletRequest request, ServletResponse response, Writer out) {
        WebContext ctx = (WebContext) request.getAttribute(CLDR_WEBCONTEXT);
        if (ctx == null) {
            throw new InternalError(
                    "WebContext: could not load fromRequest. Are you trying to load a JSP directly?");
        }
        JspWebContext subCtx = new JspWebContext(ctx); // clone the important
        // fields..
        subCtx.setRequestResponse(
                (HttpServletRequest) request, // but use the
                // req/resp of
                // the current
                // situation
                (HttpServletResponse) response);
        subCtx.setStream(out);
        return subCtx;
    }

    /**
     * Copy one WebContext to another. This is useful when you wish to create a sub-context which
     * has a different base URL (such as for processing a certain form or widget).
     *
     * @param other the other WebContext to copy from
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
     * @param x field name
     * @param def default value if field is not found.
     * @return the field value
     */
    boolean fieldBool(String x, boolean def) {
        if (field(x).length() > 0) {
            return field(x).charAt(0) == 't';
        } else {
            return def;
        }
    }

    /**
     * get a field's value, or the default
     *
     * @param x field name
     * @param def default value
     * @return the field's value as an integer, or the default value if the field was not found.
     */
    public int fieldInt(String x, int def) {
        String f;
        if ((f = field(x)).length() > 0) {
            try {
                return Integer.parseInt(f);
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
     * @param x field name
     * @return true if the field is present
     */
    public boolean hasField(String x) {
        return (request.getParameter(x) != null);
    }

    /**
     * return a field's value, else ""
     *
     * @param x field name
     * @return the field value, or else ""
     */
    public final String field(String x) {
        return field(x, "");
    }

    /**
     * return a field's value, else default
     *
     * @param x field name
     * @param def default value
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
     * Return the input string, unchanged
     *
     * This method no longer serves any purpose since CLDR now requires UTF-8 encoding
     * for all http requests and related services/configuration.
     *
     * This method is temporarily retained for compatibility with legacy code including .jsp.
     *
     * @param res the string
     *
     * @return the string
     */
    public static String decodeFieldString(String res) {
        return res;
    }

    // preference api
    /**
     * get a preference's value as a boolean. defaults to false.
     *
     * @param x pref name
     * @return preference value (or false)
     */
    public boolean prefBool(String x) {
        return prefBool(x, false);
    }

    /**
     * get a preference's value as a boolean. defaults to defVal.
     *
     * @param x preference name
     * @param defVal default value
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
     * @param x the field name and pref name
     * @param def default value
     * @return pref value or def
     */
    String pref(String x, String def) {
        String ret = field(x, session.prefGet(x));
        if (ret != null) {
            session.prefPut(x, ret);
        }
        if ((ret == null) || (ret.length() == 0)) {
            ret = def;
        }
        return ret;
    }

    /**
     * Get the target keyword and value for an 'a href' HTML tag
     *
     * @param t the target name to use
     * @return the 'target=...' string - may be blank if the user has requested no popups
     */
    public String atarget(String t) {
        if (prefBool(SurveyMain.PREF_NOPOPUPS)) {
            return "";
        } else {
            return "target='" + t + "' ";
        }
    }

    /**
     * Add a parameter to the output URL
     *
     * @param k key
     * @param v value
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
     * Add a boolean parameter to the output URL as 't' or 'f'
     *
     * @param k key
     * @param v value
     */
    void addQuery(String k, boolean v) {
        addQuery(k, v ? "t" : "f");
    }

    /**
     * Set a parameter on the output URL, replacing an existing value if any
     *
     * @param k key
     * @param v value
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
            for (String somek : oldMap.keySet()) {
                addQuery(somek, oldMap.get(somek));
            }
        }
    }

    /**
     * Set a query from an integer
     *
     * @param k
     * @param v
     *     <p>WARNING: this is accessed by debug_jsp.jspf and report.jspf
     */
    public void setQuery(String k, int v) {
        setQuery(k, Integer.toString(v));
    }

    /**
     * Remove the specified key from the query. Has no effect if the field doesn't exist.
     *
     * @param k key
     */
    public void removeQuery(String k) {
        if (outQueryMap.get(k) != null) { // if it was there..
            // rebuild query string:
            outQuery = null;
            TreeMap<String, String> oldMap = outQueryMap;
            oldMap.remove(k); // replace
            outQueryMap = new TreeMap<>();
            for (String somek : oldMap.keySet()) {
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
     *
     * @return
     */
    public String query() {
        return outQuery;
    }

    /**
     * Returns the string that must be appended to the URL to start the next parameter - either ? or
     * &amp;
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

    // WARNING: this is accessed by st_top.jsp
    public String vurl(CLDRLocale loc) {
        return vurl(loc, null, null, null);
    }

    public String vurl() {
        return vurl(null, null, null, null);
    }

    /**
     * Get the new '/v' viewing URL. Note that this will include a fragment, do NOT append to the
     * result (pass in something in queryAppend)
     *
     * @param loc locale to view.
     * @param page pageID to view. Example: PageId.Africa (shouldn't be null- yet)
     * @param strid strid to view. Example: "12345678" or null
     * @param queryAppend this will be appended as the query. Example: "?email=foo@bar". Defaults to
     *     the session key.
     * @return
     */
    public String vurl(CLDRLocale loc, PageId page, String strid, String queryAppend) {
        StringBuilder sb = new StringBuilder(request.getContextPath());
        return WebContext.appendContextVurl(sb, loc, page, strid, queryAppend).toString();
    }

    public static StringBuilder appendContextVurl(
            StringBuilder sb, CLDRLocale loc, PageId page, String strid, String queryAppend) {

        sb.append("/v");
        if (queryAppend != null && !queryAppend.isEmpty()) {
            sb.append(queryAppend);
        }
        sb.append('#'); // hash

        // locale
        sb.append('/');
        if (loc != null) {
            sb.append(loc.getBaseName());
        }

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
        println(
                "<script>window.location=' "
                        + vurl
                        + "/'+window.location.hash.substring(1);</script>");
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
     *
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
     * @param s resource URL
     * @return the context path for the specified resource
     */
    public String context(String s) {
        if (request == null) return s;
        return context(request, s);
    }

    /**
     * Get the context path for a certain resource
     *
     * @param s resource URL
     * @return the context path for the specified resource
     */
    public static String context(HttpServletRequest request, String s) {
        if (request == null) return "/" + s;
        return request.getContextPath() + "/" + s;
    }

    /**
     * Get a link (HTML URL) to a JSP
     *
     * @param s resource to link to
     * @return the URL suitable for HTML
     */
    public String jspLink(String s) {
        return context(s)
                + "?a="
                + base()
                + ((outQuery != null)
                        ? ("&amp;" + outQuery)
                        : ((session != null) ? ("&amp;s=" + session.id) : ""));
    }

    /**
     * return the IP of the remote user. If they are behind a proxy, return the actual original URL.
     *
     * @return a URL
     */
    String userIP() {
        return userIP(request);
    }

    /**
     * return the IP of the remote user given a request. If they are behind a proxy, return the
     * actual original URL.
     *
     * @param request the request to use
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
     * @param request a specific request
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
     * @param request the request portion
     */
    static String schemeHostPort(HttpServletRequest request) {
        return request.getScheme() + "://" + serverHostport(request);
    }

    /**
     * Print out a line
     *
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
     *
     * @param t throwable to print
     */
    void print(Throwable t) {
        print(
                "<pre style='border: 2px dashed red; margin: 1em; padding: 1'>"
                        + t.toString()
                        + "<br />");
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
     * Send the user to another URL. Won't work if there was already some output.
     *
     * @param where the URL for redirection, such as "/cldr-apps/v#//"
     * @see HttpServletResponse#sendRedirect(String)
     */
    void redirect(String where) {
        try {
            String port = getRedirectPort();
            if (port != null) {
                String url =
                        request.getScheme() + "://" + request.getServerName() + ":" + port + where;
                response.sendRedirect(url);
            } else {
                response.sendRedirect(where);
            }
            out.close();
            close();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe + " while redirecting to " + where);
        }
    }

    private static final String PORT_NUMBER_NOT_INITIALIZED = "?";

    /**
     * On first access, this becomes either null (for default port number 80) or a port number such
     * as "8888". Ordinarily Survey Tool uses nginx as a load-balancing server, running on the
     * default port (80). For development it may be useful to have it on a different port. This can
     * be enabled by a line such as this in cldr.properties: CLDR_REDIRECT_PORT=8888
     */
    private static String webContextRedirectPort = PORT_NUMBER_NOT_INITIALIZED;

    private String getRedirectPort() {
        if (PORT_NUMBER_NOT_INITIALIZED.equals(webContextRedirectPort)) {
            webContextRedirectPort =
                    CLDRConfig.getInstance().getProperty("CLDR_REDIRECT_PORT", null);
        }
        return webContextRedirectPort; // default null
    }

    /** Close the stream. Normally not called directly, except in outermost processor. */
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
     * @param l locale to set
     */
    public void setLocale(CLDRLocale l) {
        if (!SurveyMain.getLocalesSet().contains(l)) { // bogus
            locale = null;
            return;
        }
        locale = l;
        processor = DisplayAndInputProcessorFactory.make(locale);
        Vector<CLDRLocale> localesVector = new Vector<>();
        for (CLDRLocale parents : locale.getParentIterator()) {
            localesVector.add(parents);
        }
        docLocale = localesVector.toArray(docLocale);
    }

    /** Cached direction of this locale. */
    private HTMLDirection direction = null;

    /**
     * Return the HTML direction of this locale, ltr or rtl. Returns ltr by default. TODO: should
     * return display locale's directionality by default.
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
     * Return the current locale as a string. Deprecated, please use getLocale instead.
     *
     * @deprecated use getLocale().toString() -
     * @see #getLocale()
     */
    @Deprecated
    public final String localeString() {
        if (locale == null) {
            throw new InternalError("localeString is null");
        }
        return locale.toString();
    }

    // WARNING: this is accessed by menu_top.jsp and possibleProblems.jsp
    public String getEffectiveCoverageLevel(CLDRLocale locale) {
        return getEffectiveCoverageLevel(locale.getBaseName());
    }

    public String getEffectiveCoverageLevel(String locale) {
        String level = getRequiredCoverageLevel();
        if (level == null || level.equals(COVLEV_RECOMMENDED)) {
            // fetch from org
            level = session.getOrgCoverageLevel(locale);
        }
        return level;
    }

    public static final String COVLEV_RECOMMENDED = "default";
    public static final String[] PREF_COVLEV_LIST = {
        COVLEV_RECOMMENDED,
        Level.COMPREHENSIVE.toString(),
        Level.MODERN.toString(),
        Level.MODERATE.toString(),
        Level.BASIC.toString()
    };

    /** The default level, if no organization is available. */
    public static final Level COVLEVEL_DEFAULT_RECOMMENDED = org.unicode.cldr.util.Level.MODERN;

    public static final String COVLEV_DEFAULT_RECOMMENDED_STRING =
            COVLEVEL_DEFAULT_RECOMMENDED.name().toLowerCase();

    /**
     * Is it an organization that participates in coverage?
     *
     * @param org the name of the organization
     * @return true if the organization participates in coverage, else false
     */
    public static boolean isCoverageOrganization(String org) {
        return (org != null
                && StandardCodes.make()
                        .getLocaleCoverageOrganizationStrings()
                        .contains(org.toLowerCase()));
    }

    // WARNING: this is accessed by possibleProblems.jsp
    public String getEffectiveCoverageLevel() {
        return getEffectiveCoverageLevel(getLocale().toString());
    }

    public String getRequiredCoverageLevel() {
        return sm.getListSetting(this, SurveyMain.PREF_COVLEV, WebContext.PREF_COVLEV_LIST);
    }

    // Internal Utils

    public static final String CAN_MODIFY = "canModify";
    public static final String DATA_PAGE =
            "DataPage"; // WARNING: this is accessed by stcontext.jspf
    public static final String ZOOMED_IN =
            "zoomedIn"; // WARNING: this is accessed by stcontext.jspf
    public static final String DATA_ROW = "DataRow"; // WARNING: this is accessed by stcontext.jspf

    /**
     * Print a link to help with a specified title
     *
     * @param what the help to link to
     * @param title the title of the help
     */
    public void printHelpLink(String what, String title) {
        print("(");
        print("<a href=\"" + (SurveyMain.CLDR_HELP_LINK) + what + "\">" + title + "</a>");
        println(")");
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
        return "<img alt='["
                + icon
                + "]' style='width: 16px; height: 16px; border: 0;' src='"
                + context(request, icon + ".png")
                + "' title='"
                + message
                + "' />";
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
            this.println(
                    "<div class='ferrorbox'><B>Error</b> while including template <tt class='code'>"
                            + filename
                            + "</tt>:<br>");
            this.print(t);
            this.println("</div>");
            System.err.println("While expanding " + TMPL_PATH + filename + ": " + t);
            t.printStackTrace();
        }
    }

    /**
     * Include a template fragment from /WEB-INF/tmpl
     *
     * @param request
     * @param response
     * @param filename
     */
    public static void includeFragment(
            HttpServletRequest request, HttpServletResponse response, String filename)
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
     */
    public enum HTMLDirection {
        LEFT_TO_RIGHT("ltr"),
        RIGHT_TO_LEFT("rtl");

        private final String str;

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
         * @param dir CLDR direction string
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
        if (STFactory.isReadOnlyLocale(locale) || SurveyMain.surveyPhase(locale) == Phase.READONLY)
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

    public SurveyMain.UserLocaleStuff getUserFile() {
        SurveyMain.UserLocaleStuff uf = peekUserFile();
        if (uf == null) {
            uf = sm.getUserFile();
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

    public void no_js_warning() {
        boolean no_js = prefBool(SurveyMain.PREF_NOJAVASCRIPT);
        if (!no_js) {
            WebContext nuCtx = (WebContext) clone();
            nuCtx.setQuery(SurveyMain.PREF_NOJAVASCRIPT, "t");
            println("<noscript><h1>");
            println(
                    iconHtml("warn", "JavaScript disabled")
                            + "JavaScript is disabled. Please enable JavaScript..");
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
     * @return WARNING: this is accessed by usermenu.jsp
     */
    public Cookie getCookie(String id) {
        return getCookie(request, id);
    }

    public static Cookie getCookie(HttpServletRequest request, String id) {
        if (request == null) return null;
        Cookie[] cooks = request.getCookies();
        if (cooks == null) return null;
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
        return getCookieValue(request, id);
    }

    /**
     * Get a cookie value or null
     *
     * @param id
     * @return
     */
    public static String getCookieValue(HttpServletRequest hreq, String id) {
        Cookie c = getCookie(hreq, id);
        if (c != null) {
            return c.getValue();
        }
        return null;
    }

    static void addCookie(HttpServletResponse response, String id, String value, int expiry) {
        Cookie c = new Cookie(id, value);
        c.setMaxAge(expiry);
        c.setPath("/");
        response.addCookie(c);
    }

    /**
     * Clear a cookie
     *
     * @param request
     * @param response
     * @param c
     */
    public static void clearCookie(
            HttpServletRequest request, HttpServletResponse response, String c) {
        Cookie c0 = WebContext.getCookie(request, c);
        if (c0 != null) {
            c0.setValue("");
            c0.setPath("/");
            // c0.setMaxAge(0);
            response.addCookie(c0);
        }
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
        return "{ "
                + this.getClass().getName()
                + ": url="
                + url()
                + ", ip="
                + this.userIP()
                + ", user="
                + this.user()
                + "}";
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
        return (UserRegistry.userCanModifyLocale(session.user, getLocale()));
    }

    /**
     * @param locale
     * @return
     */
    String urlForLocale(CLDRLocale locale) {
        return vurl(locale, null, null, null);
    }

    // WARNING: this is accessed by generalinfo.jsp and st_top.jsp
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

    private static final UnicodeSet csvSpecial = new UnicodeSet("[, \"]").freeze();

    public static void csvWrite(Writer out, String str) throws IOException {
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

    /** set the session. Only call this once. */
    public void setSession() {
        if (request == null && session != null) {
            setSessionMessage("using canned session"); // already set - for testing
            return;
        }

        if (this.session != null) {
            setSessionMessage("Internal error - session already set.");
            return;
        }

        CookieSession.checkForExpiredSessions(); // If a session has expired, remove it

        String password = field(SurveyMain.QUERY_PASSWORD);
        if (password.isEmpty()) {
            password = field(SurveyMain.QUERY_PASSWORD_ALT);
        }
        String email = field(SurveyMain.QUERY_EMAIL);

        User user = null;

        // if there was an email/password in the cookie, use that.
        {
            final String jwt = getCookieValue(SurveyMain.COOKIE_SAVELOGIN);
            if (jwt != null && !jwt.isBlank()) {
                final String jwtId = CookieSession.sm.klm.getSubject(jwt);
                if (jwtId != null && !jwtId.isBlank()) {
                    if (!email.isEmpty() && !password.isEmpty()) {
                        // If the user was already logged in as Admin/TC/Manager, then used a URL
                        // with explicit email/password to log in as a different user, the old
                        // cookies (especially JWT) must be removed to prevent staying logged
                        // in as the first user
                        removeLoginCookies(request, response);
                    } else {
                        User jwtInfo = CookieSession.sm.reg.getInfo(Integer.parseInt(jwtId));
                        if (jwtInfo != null) {
                            user = jwtInfo;
                            logger.fine("Logged in " + jwtInfo + " #" + jwtId + " using JWT");
                        }
                    }
                }
            }
        }

        // if an email/password given, try to fetch a user
        if (user == null) { // if not already logged in via jwt
            try {
                user = CookieSession.sm.reg.get(password, email, userIP());
            } catch (LogoutException e) {
                logout(); // failed login, so logout this session.
            }
        }
        if (user != null) {
            logger.fine("Logged in " + user);
            user.touch(); // mark this user as active.
        }

        // we just logged in- see if there's already a user session for this user..
        if (user != null) {
            session = CookieSession.retrieveUser(user.email); // is this user already logged in?
            if (session != null) {
                if (null
                        == CookieSession.retrieve(
                                session.id)) { // double check- is the session still valid?
                    session = null; // don't allow dead sessions to show up
                    // via the user list.
                }
            }
        }

        // Retreive a number from the cookie if present
        String aNum = getCookieValue(Auth.SESSION_HEADER);
        if (aNum != null) {
            session = CookieSession.retrieve(aNum);
            logger.fine("From cookie " + Auth.SESSION_HEADER + " : " + session);
        }

        if (session != null && session.user != null && user != null && user.id != session.user.id) {
            logger.fine("Changing session " + session.id + " from " + session.user + " to " + user);
            session = null; // user was already logged in as 'session.user', replacing this with
            // 'user'
        }

        if ((user == null)
                && (hasField(SurveyMain.QUERY_PASSWORD) || hasField(SurveyMain.QUERY_EMAIL))) {
            logger.fine("Logging out - mySession=" + session + ", and had em/pw");
            logout(); // zap cookies if some id/pw failed to work
        }

        if (session == null && user == null) {
            session =
                    CookieSession.checkForAbuseFrom(
                            userIP(), SurveyMain.BAD_IPS, request.getHeader("User-Agent"));
            if (session != null) {
                logger.info("throttling session " + session.id);
                println(
                        "<h1>Note: Your IP, "
                                + userIP()
                                + " has been throttled for making "
                                + SurveyMain.BAD_IPS.get(userIP())
                                + " connections. Try turning on cookies, or obeying the 'META ROBOTS' tag.</h1>");
                flush();
                session = null;
                setSessionMessage("Bad IP.");
                return;
            }
        }

        logger.fine("Session Now=" + session + ", user=" + user);

        // allow in administrator or TC.
        if (!UserRegistry.userIsTC(user)) {
            if ((user != null) && (session == null)) { // user trying to log in-
                if (CookieSession.tooManyUsers()) {
                    System.err.println(
                            "Refused login for "
                                    + email
                                    + " from "
                                    + userIP()
                                    + " - too many users ( "
                                    + CookieSession.getUserCount()
                                    + ")");
                    setSessionMessage(
                            "We are swamped with about "
                                    + CookieSession.getUserCount()
                                    + " people using the SurveyTool right now! Please try back in a little while.");
                    return;
                }
            } else if (session == null || (session.user == null)) { // guest user
                if (CookieSession.tooManyObservers()) {
                    if (session != null) {
                        System.err.println(
                                "Logged-out observer  "
                                        + session.id
                                        + " from "
                                        + userIP()
                                        + " - too many users ( "
                                        + CookieSession.getUserCount()
                                        + ")");
                        session.remove(); // remove observers at this point
                        session = null;
                    }
                    logout(); // clear session cookie
                    setSessionMessage(
                            "We have too many people browsing the CLDR Data on the Survey Tool. Please try again later when the load has gone down.");
                    return;
                }
            }
        }

        // New up a session, if we don't already have one.
        if (session == null) {
            String newId = CookieSession.newId();
            session = CookieSession.newSession(userIP(), newId);
            logger.fine("New session #" + session + " for " + user);
        }

        if (user != null) {
            session.setUser(user); // this will replace any existing session by this user.
            session.user.ip = userIP();
            String s = getSessionMessage();
            if (s != null && s.contains(LOGIN_FAILED)) {
                setSessionMessage(null);
            }
        } else {
            if ((email != null) && (email.length() > 0) && (session.user == null)) {
                setSessionMessage(LOGIN_FAILED); // No reset at present, CLDR-7405
            }
        }
        // processs the 'remember me'
        if (session != null && session.user != null) {
            if (hasField(SurveyMain.QUERY_SAVE_COOKIE)) {
                logger.fine("Remembering user: " + session.user + " for session " + session.id);
                loginRemember(session.user);
            }
        }
        if (session != null) {
            setSessionCookie(response, session.id);
        }
    }

    /** Logout this ctx */
    public void logout() {
        logout(request, response);
    }

    /**
     * Logout this req/response (zap cookie) Zaps http session
     *
     * @param request
     * @param response
     */
    public static void logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false); // dont create
        if (session != null) {
            String sessionId = (String) session.getAttribute(SurveyMain.SURVEYTOOL_COOKIE_SESSION);
            if (CookieSession.DEBUG_INOUT) {
                System.err.println(
                        "logout() of session "
                                + session.getId()
                                + " and cookieSession "
                                + sessionId);
            }
            if (sessionId != null) {
                final UserRegistry.User user = CookieSession.remove(sessionId);
                if (user != null) {
                    user.touch(); // update user last seen time to logout time
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
    public static void removeLoginCookies(
            HttpServletRequest request, HttpServletResponse response) {
        WebContext.clearCookie(request, response, SurveyMain.QUERY_EMAIL);
        WebContext.clearCookie(request, response, SurveyMain.QUERY_PASSWORD);
        WebContext.clearCookie(request, response, SurveyMain.COOKIE_SAVELOGIN);
        WebContext.clearCookie(request, response, Auth.SESSION_HEADER);
        try {
            HttpSession s = request.getSession(false);
            if (s != null) {
                s.invalidate();
            }
        } catch (IllegalStateException ise) {
            // ignore: means the session was already logged out
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

    /** Remember this login (adds cookie to ctx.response ) */
    public void loginRemember(User user) {
        loginRemember(response, user);
    }

    /** Remember this login (adds cookie to response ) */
    public static void loginRemember(HttpServletResponse response, User user) {
        loginRemember(response, user.id);
    }

    public static void loginRemember(HttpServletResponse response, int id) {
        addCookie(
                response,
                SurveyMain.COOKIE_SAVELOGIN,
                CookieSession.sm.klm.createJwtForSubject(Integer.toString(id)),
                SurveyMain.TWELVE_WEEKS);
    }

    /** Set the Session ID cookie (not JSESSION) on the given response. */
    public static void setSessionCookie(HttpServletResponse response, String sessionId) {
        addCookie(response, Auth.SESSION_HEADER, sessionId, SurveyMain.TWELVE_WEEKS);
    }

    /**
     * Get the Session cookie (not JSESSION)
     *
     * @param request
     * @return cookie or null
     */
    public static Cookie getSessionCookie(HttpServletRequest request) {
        return getCookie(request, Auth.SESSION_HEADER);
    }

    /**
     * Get the Session string from the session cookie (not JSESSION) or null
     *
     * @param request
     * @return session ID or null
     */
    public static String getSessionIdFromCookie(HttpServletRequest request) {
        final Cookie c = getSessionCookie(request);
        if (c != null) {
            return c.getValue();
        }
        return null;
    }

    private String sessionMessage = null;

    public String getSessionMessage() {
        if (sessionMessage == null && session != null) {
            sessionMessage = session.getMessage();
        }
        return sessionMessage;
    }

    public void setSessionMessage(String s) {
        sessionMessage = s;
        if (session != null) {
            session.setMessage(s);
        }
    }
}
