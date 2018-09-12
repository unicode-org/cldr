<%@ include file="/WEB-INF/jspf/stcontext.jspf" %><%-- setup 'ctx' --%>
<%
SummarizingSubmissionResultHandler ssrh = (SummarizingSubmissionResultHandler)ctx.get("ssrh");
SummarizingSubmissionResultHandler.ItemInfo itemInfo = null;
if(ssrh != null) itemInfo = ssrh.infoFor(dataRow);
String vToShow = dataRow.getWinningValue() ;
if(itemInfo!=null && itemInfo.getProposedValue()!=null) {
	vToShow = itemInfo.getProposedValue();
}
if(false){ // debug
%><tr>
 <td colspan=3>
 	ssrh=<%= ssrh %>, itemInfo=<%= itemInfo %>, vToShow=<%= vToShow %>
 </td>
</tr>
<% } %><tr class="tr_unknown" id="r_<%= dataRow.fieldHash() %>">
	<th><tt><%=dataRow.prettyPath%></tt></th>
  <th><%=
	  dataRow.getDisplayName() %></th>
  <td id="i_<%= dataRow.fieldHash() %>"> <input name="<%= dataRow.fieldHash() %>" value="<%= SurveyMain.CHANGETO %>" type='hidden'>
  <% if(ctx.canModify()) { %>
	  <input dir="<%= htmlDirection %>" 
	  			onchange="do_change('<%= dataRow.fieldHash() %>',this.value,<%= dataRow.getXpathId() %>,'<%= dataRow.getLocale() %>', '<%= ctx.session %>')"
	  			class="inputbox" name="<%= dataRow.fieldHash() %>_v" value="<%= vToShow %>">
	  <div id="e_<%= dataRow.fieldHash() %>" ><!--  errs for this item --></div>
  <% } else { %>
  		<%= vToShow %>
  <% } %>	
  </td>

<%
if(itemInfo != null) {
	String iconHtml;
	if(itemInfo.getStatus()==SummarizingSubmissionResultHandler.ItemStatus.ITEM_GOOD) {
		iconHtml = ctx.iconHtml("okay","Item OK");
	} else if(itemInfo.getStatus()==SummarizingSubmissionResultHandler.ItemStatus.ITEM_BAD) {
		iconHtml = ctx.iconHtml("stop", "Item Not OK");
	} else {
		iconHtml = ctx.iconHtml("ques", "Unknown State");
	}
  %>
  	<td><%= iconHtml %><%= itemInfo.getDescription() %></td>
  <%
}
%>

</tr>

<% if(itemInfo!=null && itemInfo.getErrors() !=null && !itemInfo.getErrors().isEmpty()) { %>
 <tr>
 	<td colspan="3">
 		<% for(org.unicode.cldr.test.CheckCLDR.CheckStatus status : itemInfo.getErrors()) { %>
 			<%= status.toString() %><br>
 		<% } %>
 	</td>
 </tr>
<% } %>
