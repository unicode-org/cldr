<%@page import="com.ibm.icu.dev.util.ElapsedTimer"%><%@page import="org.unicode.cldr.web.SurveyMain"%><%@page 
        import="org.unicode.cldr.web.WebContext, java.util.List"%><%@ page 
        language="java" contentType="application/json; charset=UTF-8"
        import="com.ibm.icu.util.ULocale,org.unicode.cldr.util.*,org.unicode.cldr.web.*,org.json.*" %><%--
 Copyright (C) 2012 IBM and Others. All Rights Reserved  --%> <%
 	if(!SurveyMain.isSetup || SurveyMain.isBusted()) {
     response.sendError(500,
             "SurveyTool not ready.");
     return;
 }
  /*
  <label>XPath:<input id='xpath' size=160></label><br />
 <label>PathHeader:<input id='pathheader' size=160></label><br />
 <label>XPath strid:<input id='strid' onchange="lookup_xpath('strid')" size=10></label>
 <label>XPathID (dec#)<input id='xpathid' size=10></label>
 */

 String from  = request.getParameter("from");
 String q = WebContext.decodeFieldString(request.getParameter("q").trim());
 if(q==null||q.length()==0 || from==null || from.isEmpty())  {
     new JSONWriter(out).object().key("err").value("No parameter")
     .endObject();
     return;
 }
 ElapsedTimer et = new ElapsedTimer("xpath lookup");
 final XPathTable xpt = CookieSession.sm.xpt;
 String xpath = null;
 String pathheaderstring = null;
 PathHeader pathheader = null;
 String strid = null;
 int xpathid = XPathTable.NO_XPATH;

 try {
 	if(from.equals("strid")) {
 		strid = q;
 	} else if(from.equals("xpath")) {
 		// don't mint new xpaths!
 		xpath = q;
 	} else if(from.equals("xpathid")) {
 		xpathid = Integer.parseInt(q);
 		if(xpathid < 0 || xpathid > xpt.count()) {
 			xpathid =xpt.NO_XPATH;
 		}
 	}
 	
     // * to xpid - do this first to reject bad xpaths
    if(xpathid == xpt.NO_XPATH && xpath != null) {
          xpathid = xpt.peekByXpath(xpath);
          if(xpathid == xpt.NO_XPATH) {
              xpath=null; // do not mint new paths
          }
      }

 	// *  to XPath 
 	if(xpath==null && xpathid != xpt.NO_XPATH) {  // decid to xpath
 		xpath = xpt.getById(xpathid);
 	}
 	 	
 	if(xpath==null && strid != null) {  // strid to xpath
 		xpath = xpt.getByStringID(strid);
 	}
 	
 	// * to strid
 	if(strid == null && xpath != null) { // xpath to strid
 		strid = CookieSession.sm.xpt.getStringIDString(xpath);
 	}
 	
 	// * to PH
 	if(pathheader==null && xpath != null) {
 		pathheader = CookieSession.sm.getSTFactory().getPathHeader(xpath);
 	}


 	JSONObject paths = new JSONObject().put("xpath",xpath).     
 	put("pathheader",pathheader)
 	.put("strid",strid)
 	.put("xpathid",xpathid);
 	
 	new JSONWriter(out).object().key("err").value("OK: " + et)
 	.key("paths").value(paths)
 	.endObject();
 } catch (Throwable t) {
     new JSONWriter(out).object().key("err").value(t.toString())
 //    .key("paths").value(paths)
     .endObject();
 }
 %>
