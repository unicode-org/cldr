<%@ include file="/WEB-INF/jspf/stcontext.jspf" %><!--  possibleProblems.jspf begin -->


<div id='possibleProblems'></div>
<%
if(ctx.field("x").equals(SurveyMain.xMAIN) || ctx.field("x").isEmpty()) {
%>
<script>
//console.log("dojo= " + dojo);
//console.log("spp=" + showPossibleProblems);

require(["dojo/ready"], function(ready) {
	ready(function() {
		showPossibleProblems('possibleProblems', '<%= ctx.getLocale() %>', '<%= ctx.session.id %>',
			'<%= ctx.getEffectiveCoverageLevel() %>', '<%= ctx.getRequiredCoverageLevel() %>');
	});
});

</script>

<%
}
%>