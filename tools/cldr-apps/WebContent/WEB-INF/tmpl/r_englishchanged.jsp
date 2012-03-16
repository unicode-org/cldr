<%@ include file="/WEB-INF/jspf/report_top.jspf" %>

<p> Some Clever Text Here. Note, you are in locale <tt class='codebox'><%= ctx.getLocale() %></tt>
 which we display as <tt class='codebox'><%= ctx.getLocaleDisplayName() %></tt> </p>


<%

out.println("<b>HTML here</b>");

String xpaths[] = {
        "//ldml/localeDisplayNames/scripts/script[@type=\"Hant\"]", 
        "//ldml/localeDisplayNames/keys/key[@type=\"calendar\"]"
};

out.println("<ul>");
for(String x : xpaths) {
    String url = "?_="+ctx.getLocale()+"&amp;xpath=" + java.net.URLEncoder.encode( x  );
    out.println("<li><a href=\"" +url+ "\">"+x+"</a></li>");
}
out.println("</ul>");

%>
