<%@ tag language="java" pageEncoding="utf-8"%>
<%@ attribute name="which" required="false" %>
<%@ attribute name="key" required="false" %>
<%@ attribute name="name" required="false" %>
<%@ attribute name="menu" required="false" %>
<%@ tag import="org.unicode.cldr.web.*" %>
<jsp:doBody var="title"/>
<%

JspWebContext ctx = WebContext.fromRequest(request,response,out);

String key = (String) jspContext.getAttribute("key");
if(key == null) {
    key = SurveyMain.QUERY_SECTION;
}

String title = (String) jspContext.getAttribute("title");

String which = (String) jspContext.getAttribute("which");
if(which==null) {
    which = title.trim().toLowerCase();
}

String menu = (String) jspContext.getAttribute("menu");
if(menu==null) {
    menu = request.getParameter(key);
}

%>
<%= SurveyMain.getMenu(ctx,which,menu,title,key,null) %>