<%@ page language="java" import="org.unicode.cldr.web.*" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%><%
    
    SurveyMain sm = CookieSession.sm;
    if(sm==null || !sm.isSetup || sm.isBusted()) {
    	response.sendError(500);
    }
    final String xpstrid = request.getParameter("xpstrid");
    final String xpath = sm.xpt.getByStringID(xpstrid);
%>
<%=CookieSession.sm.getTranslationHintsExample().getHelpHtml(xpath, sm.getTranslationHintsFile().getStringValue(xpath))%>
<!--  
  xpath: <%= xpath %>
  strid: <%= xpstrid %> 
-->