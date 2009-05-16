<%@ include file="stcontext.jspf" %><%-- setup 'ctx' --%>

<tr>
  <th><%= ctx.get(WebContext.BASE_EXAMPLE) %></th>
  <td><input name="<%= dataRow.fieldHash() %>" value="someNewValue"></td>
 </tr>
 