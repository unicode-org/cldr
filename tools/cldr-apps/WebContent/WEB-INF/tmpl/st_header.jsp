<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!--  begin st_header.jsp -->
<%-- TODO merge st_header.jsp and st_top.jsp --%>
<%@ include file="/WEB-INF/jspf/stcontext.jspf" %> 
<%
	String htmlClass = "stOther";
	if(ctx!=null && ctx.getPageId()!=null) {
		htmlClass = "claro";
	}
%>
<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" "http://www.w3.org/TR/html4/loose.dtd">
<html class="<%= htmlClass %>">
<head>
<meta name='robots' content='noindex,nofollow'>
<meta name="gigabot" content="noindex">
<meta name="gigabot" content="noarchive">
<meta name="gigabot" content="nofollow">

<!--  end st_header.jsp -->