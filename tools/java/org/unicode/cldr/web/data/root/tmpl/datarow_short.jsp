<%@ include file="stcontext.jspf" %><%-- setup 'ctx' --%>

<tr>
  <th><%= dataRow.getDisplayName() %></th>
  <td><input name="<%= dataRow.fieldHash() %>" value="someNewValue"></td>
 </tr>
 
