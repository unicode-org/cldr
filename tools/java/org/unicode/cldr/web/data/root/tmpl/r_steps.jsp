<%@ include file="stcontext.jspf" %><%-- setup 'ctx' --%>
<%@ include file="report.jspf"  %>

<%

subCtx.setQuery(SurveyMain.QUERY_LOCALE,ctx.localeString());
%>

<%--
    <%@ include file="report_top.jspf" %>
--%>

<%!
	/**
	 * convert a stage to a base xpath
	 */
		String stageToBaseXpath(int step) {
			try {
				return base_xpaths[step-1];
			} catch(Throwable t) {
				return null;
			}
		}
%>

<%
	subCtx.setQuery(SurveyMain.QUERY_SECTION,subCtx.field(SurveyMain.QUERY_SECTION));
	// step parameter determines which 'stage' we are at. Stage 1: characters, stage 2: two currencies, stage 3: congratulations.

	// get the stage
	int myStage = ctx.fieldInt("step", 1);         // requested 'new' stage
	int oldStage = ctx.fieldInt("old_step", -1);   // outgoing 'old' stage (where we came from). 
	
	// what's the base xpath for the previous stage (for data submission)?
	String baseXpathForOldStage = stageToBaseXpath(oldStage);
	
	boolean moveForward = true;
	SummarizingSubmissionResultHandler ssrh = null;
	
	if(baseXpathForOldStage!=null) {  // handle submit of old data?
			%>  <h2>Processing data for xpath <%= baseXpathForOldStage %>  </h2>   <%
		ssrh = SurveyForum.processDataSubmission(ctx, baseXpathForOldStage);
		ctx.put("ssrh",ssrh);
		moveForward = !ssrh.hadErrors();
		if(moveForward == false) {
			%><b>Please correct the errs first</b><br><%
			myStage = oldStage; // stuck here, until no errs.
		} else {
			%><i>No errors - OK to move forward.</i><%
		}
	} else{
		%><i>no base xpath, so nothing to process.</i><br><%
	}
	

	
	// What is the base xpath for the next stage?
	String baseXpathForNextStage = stageToBaseXpath(myStage);

	// begin the form.
	if(baseXpathForNextStage != null) {
		SurveyForum.beginSurveyToolForm(ctx, baseXpathForNextStage);
		
		// print hidden fields, as needed
		out.println("<input type='hidden' value='"+ctx.field(SurveyMain.QUERY_SECTION)+"' name='"+SurveyMain.QUERY_SECTION+"'>");
		out.println("<input type='hidden' name='old_step' value='"+myStage+"'>");
	}
%>

	Old stage: <%= oldStage %>, Current stage: <%= myStage %><br>
	<hr>

<% 
	int nextStage = myStage;

	if(myStage<1) myStage=1;
	if(myStage>(reports.length))  {
		myStage=reports.length+1;
		%>
		<h2> congratulations </h2>
		<p> Great, thanks for your work on the <%= ctx.localeName %> locale.</p>
		<%
	} else {
		ctx.put("thisBaseXpath",baseXpathForNextStage);
		ctx.put("thisStep",(Integer)myStage);
		ctx.includeFragment(reports[myStage-1]+".jsp");
		nextStage = myStage+1;
	}
	

	// now, enter the close of the form
	
	if(baseXpathForNextStage != null) {  // if we have a form open..
		// send them to the next section
%>      
		<input type='hidden' name='step' value='<%= nextStage %>'>
		<input type='submit' value='Submit'>
<%
	}
%>


