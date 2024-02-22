<%@ page language="java" contentType="text/plain; charset=UTF-8"
import="java.util.Map"
    pageEncoding="UTF-8"%>

<% Map<String,String> map = org.unicode.cldr.web.CookieSession.sm.getSupplementalDataInfo().getMetazoneToContinentMap(); %>

private static final String mzToContinentStatic[] = { 
<% for(Map.Entry<String,String> e : map.entrySet()) { %>
	"<%= e.getKey() %>", "<%= e.getValue() %>",<% }; %>
