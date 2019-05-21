<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jspf/stcontext.jspf" %>
<!--  begin st_footer.jsp --><%

// TODO: rewrite as actual JSP…

		ctx.println("<div id='footer'>");
        ctx.println("<hr>");
        ctx.print("<div id='hits' style='float: right; font-size: 60%;'>");
        ctx.print(SurveyMain.getCurrev());
        ctx.print("<span class='notselected'>validate <a href='http://jigsaw.w3.org/css-validator/check/referer'>css</a>, "
                + "<a href='http://validator.w3.org/check?uri=referer'>html</a></span>");
        ctx.print(" \u00b7 <span id='visitors'>");
        ctx.print(SurveyMain.getGuestsAndUsers());
        ctx.print("</span> \u00b7 ");
        ctx.print(" served in " + ctx.reqTimer + " <span id='dynload'></span></div>");
        
        
        ctx.println("<a href='http://www.unicode.org'>Unicode</a> | <a href='" + SurveyMain.URL_CLDR + "'>Common Locale Data Repository</a>");
       // if (ctx.request != null)
            try {
                Map m = new TreeMap(ctx.getParameterMap());
                m.remove("sql");
                m.remove("pw");
                m.remove(SurveyMain.QUERY_PASSWORD_ALT);
                m.remove("email");
                m.remove("dump");
                m.remove("s");
                m.remove("udump");
                String u = "";
                for (Enumeration e = request.getParameterNames(); e.hasMoreElements();) {
                    String k = e.nextElement().toString();
                    String v;
                    if (k.equals("sql") || k.equals("pw") || k.equals("email") || k.equals("dump") || k.equals("s")
                            || k.equals("udump")) {
                        v = "";
                    } else {
                        v = request.getParameterValues(k)[0];
                    }
                    u = u + "|" + k + "=" + v;
                }
                ctx.println("| <a " + (SurveyMain.isUnofficial() ? "title" : "href") + "='" + SurveyMain.bugFeedbackUrl("Feedback on URL ?" + u)
                        + "'>Report Problem in Tool</a>");
            } catch (Throwable t) {
                SurveyLog.logException(t, ctx);
                SurveyLog.logger.warning(t.toString());
                t.printStackTrace();
            }
        
        ctx.println("</div>");        
        ctx.println("</body>");
        ctx.println("</html>");
%>
<!--  end st_footer.jsp -->