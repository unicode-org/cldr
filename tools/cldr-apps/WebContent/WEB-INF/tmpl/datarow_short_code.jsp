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
<% } %><tr>
	<th><tt><%= dataRow.type %></tt></th>
  <th><%=
	  dataRow.getDisplayName() %></th>
  <td> <input name="<%= dataRow.fullFieldHash() %>" value="<%= SurveyMain.CHANGETO %>" type='hidden'>
  <% if(ctx.canModify()) { %>
	  <input dir="<%= htmlDirection %>" class="inputbox" name="<%= dataRow.fullFieldHash() %>_v" value="<%= vToShow %>">
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
