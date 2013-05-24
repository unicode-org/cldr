<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%><%@ include file="/WEB-INF/jspf/stcontext.jspf"%>

<!--  this is generalinfo.jsp  -->

<%
	CLDRLocale dcParent = ctx.sm.getSupplementalDataInfo()
			.getBaseFromDefaultContent(ctx.getLocale());
	CLDRLocale dcChild = ctx.sm.getSupplementalDataInfo()
			.getDefaultContentFromBase(ctx.getLocale());
	if (ctx.sm.getReadOnlyLocales().contains(ctx.getLocale())) {
		String comment = SpecialLocales.getComment(ctx.getLocale());
		if (comment == null)
			comment = "Editing of this locale has been disabled by the SurveyTool administrators.";
%>
<%=ctx.iconHtml("lock", comment)%><i><%=comment%></i>
<%
	return;
	} else if (dcParent != null) {
		ctx.println("<b><a href=\"" + ctx.url() + "\">" + "Locales"
				+ "</a></b><br/>");
		ctx.println("<h1 title='" + ctx.getLocale().getBaseName()
				+ "'>" + ctx.getLocale().getDisplayName() + "</h1>");
		ctx.println("<div class='ferrbox'>This locale is the "
				+ SurveyMain.DEFAULT_CONTENT_LINK
				+ " for <b>"
				+ ctx.sm.getLocaleLink(ctx, dcParent, null)
				+ "</b>; thus editing and viewing is disabled. Please view and/or propose changes in <b>"
				+ ctx.sm.getLocaleLink(ctx, dcParent, null)
				+ "</b> instead.");
		//                ctx.printHelpLink("/DefaultContent","Help with Default Content");
		ctx.print("</div>");

		//printLocaleTreeMenu(ctx, which);
		ctx.sm.printFooter(ctx);
		return; // Disable viewing of default content

	} else if (dcChild != null) {
		String dcChildDisplay = ctx.getLocaleDisplayName(dcChild);
		ctx.println("<div class='fnotebox'>This locale supplies the "
				+ SurveyMain.DEFAULT_CONTENT_LINK
				+ " for <b>"
				+ dcChildDisplay
				+ "</b>. Please make sure that all the changes that you make here are appropriate for <b>"
				+ dcChildDisplay
				+ "</b>. If there are multiple acceptable choices, please try to pick the one that would work for the most sublocales. ");
		//ctx.printHelpLink("/DefaultContent","Help with Default Content");
		ctx.print("</div>");
		ctx.redirectToVurl(ctx.vurl(ctx.getLocale(), null, null, null));
	} else {
		ctx.redirectToVurl(ctx.vurl(ctx.getLocale(), null, null, null));
	}
%>


<%
	if (false && SurveyMain.isUnofficial()) {
%>
<h3>Recent Items in This Locale</h3>
<div id='submitItems'></div>

<script>
showRecent('submitItems', '<%=ctx.getLocale()%>
	')
</script>

<%
	}
%>