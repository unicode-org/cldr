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

public class WebContext {
// USER fields
    public Document doc[]= new Document[0];
    public ULocale locale = null;
    public String docLocale[] = new String[0];
    public String localeName = null; 
    public CookieSession session = null;

// private fields
    protected PrintWriter out = null;
    Hashtable form_data = null;
    String baseURL = cgi_lib.MyTinyURL();
    String outQuery = null;
    TreeMap outQueryMap = new TreeMap();
    boolean dontCloseMe = false;

    // New constructor
    public WebContext(OutputStream ostr) throws IOException {
        out = openUTF8Writer(ostr);
        dontCloseMe = false;
        
        // set up cgi
      form_data = cgi_lib.ReadParse(System.in);
      if (cgi_lib.MethGet()) {
//          System.out.println("REQUEST_METHOD=GET");
          }
      if (cgi_lib.MethPost()) { 
//          System.out.println("REQUEST_METHOD=POST");
       }
        
    }
    
    // copy c'tor
    public WebContext( WebContext other) {
        doc = other.doc;
        docLocale = other.docLocale;
        out = other.out;
        form_data = other.form_data;
        baseURL = other.baseURL;
        outQuery = other.outQuery;
        locale = other.locale;
        localeName = other.localeName;
        session = other.session;
        outQueryMap = (TreeMap)other.outQueryMap.clone();
        dontCloseMe = true;
    }
    
// More API
    String field(String x) {
        String res = (String)form_data.get(x);
         if(res == null) {       
//              System.err.println("[[ empty query string: " + x + "]]");
            res = "";   
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

    String url() {
        if(outQuery == null) {
            return baseURL;
        } else {
            return baseURL + "?" + outQuery;
        }
    }
    
    String base() { 
        return baseURL;
    }
    void printUrlAsHiddenFields() {
        for(Iterator e = outQueryMap.keySet().iterator();e.hasNext();) {
            String k = e.next().toString();
            String v = outQueryMap.get(k).toString();
            println("<input type='hidden' name='" + k + "' value='" + v + "'/>");
        }
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
        do {
            try {
                Document d = SurveyMain.fetchDoc(parents);
                localesVector.add(parents);
                docsVector.add(d);
            } catch(Throwable t) {
                // error is shown elsewhere.
            }
            parents = getParent(parents);
        } while(parents != null);
        doc = (Document[])docsVector.toArray(doc);
        docLocale = (String[])localesVector.toArray(docLocale);
    }
        
// locale hash api
    // get the hashtable of modified locales

    Hashtable localeHash = null;
    public Hashtable getLocaleHash() {
        if(session == null) {
            throw new RuntimeException("Session is null in WebContext.getLocaleHash()!");
        }
        if(localeHash == null) {
            Hashtable localesHash = session.getLocales();
            if(localesHash == null) { return null; }
            localeHash = (Hashtable)localesHash.get(locale);
        }
        return localeHash;
    }
    
    public Object getByLocale(String key) {
        Hashtable localeHash = getLocaleHash();
        if(localeHash != null) {
            return localeHash.get(key);
        }
        return null;
    }

    public void putByLocale(String key, Object value) {
        Hashtable localeHash = getLocaleHash();
        if(localeHash == null) {
            localeHash = new Hashtable();
            session.getLocales().put(locale,localeHash);
        }
        localeHash.put(key, value);
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
    
    public void printHelpLink(String what)
    {
        println("(<a href=\"http://bugs.icu-project.org/cgibin/cldrwiki.pl?SurveyToolHelp" + what + "\">Help</a>)");
    }
}
