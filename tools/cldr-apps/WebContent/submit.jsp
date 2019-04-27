<%@page import="org.unicode.cldr.icu.LDMLConstants"%>
<%@page import="org.unicode.cldr.util.PathHeader.SurveyToolStatus"%>
<%@page
	import="org.unicode.cldr.web.CLDRProgressIndicator.CLDRProgressTask"%>
<%@page import="com.ibm.icu.util.ULocale"%>
<%@page import="org.xml.sax.SAXParseException"%>
<%@page import="org.unicode.cldr.util.CLDRFile.DraftStatus"%>
<%@page import="org.unicode.cldr.util.*"%>
<%@page import="org.unicode.cldr.util.CLDRInfo.CandidateInfo"%>
<%@page import="org.unicode.cldr.util.CLDRInfo.UserInfo"%>
<%@page import="org.unicode.cldr.test.*"%>
<%@page import="org.unicode.cldr.web.*"%>
<%@page import="org.unicode.cldr.util.CLDRFile"%>
<%@page import="org.unicode.cldr.util.SimpleXMLSource"%>
<%@page import="org.unicode.cldr.util.XMLSource"%>
<%@page import="org.unicode.cldr.util.CoverageInfo" %>
<%@page import="java.io.*"%><%@page
	import="java.util.*,org.apache.commons.fileupload.*,org.apache.commons.fileupload.servlet.*,org.apache.commons.io.FileCleaningTracker,org.apache.commons.fileupload.util.*,org.apache.commons.fileupload.disk.*,java.io.File"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%
	String sid = request.getParameter("s");
	if (!request.getMethod().equals("POST") || (sid == null)) {
		response.sendRedirect(request.getContextPath() + "/upload.jsp");
		return;
	}

	CLDRFile cf = null;

	String email = request.getParameter("email");
	final CookieSession cs = CookieSession.retrieve(sid);
	if (cs == null || cs.user == null) {
		response.sendRedirect(request.getContextPath() + "/survey");
		return;
	}
	cs.userDidAction(); // mark user as not idle
	UserRegistry.User theirU = cs.sm.reg.get(email.trim());
	if (theirU == null
			|| (!theirU.equals(cs.user) && !cs.user.isAdminFor(theirU))) {
		response.sendRedirect(request.getContextPath()
				+ "/upload.jsp?s=" + sid + "&email=" + email.trim()
				+ "&emailbad=t");
		return;
	}
	boolean isSubmit = true;
	final String submitButtonText = "NEXT: Submit as " + theirU.email;

	String ident = "";
	if (theirU.id != cs.user.id) {
		ident = "&email=" + theirU.email + "&pw="
				+ cs.sm.reg.getPassword(null, theirU.id);
	}

	boolean doFinal = (request.getParameter("dosubmit") != null);

	String title = "Submitted as " + theirU.email;

	if (!doFinal) {
		title = title + " <i>(Trial)</i>";
	}

	cf = (CLDRFile) cs.stuff.get("SubmitLocale");
%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>SurveyTool File Submission | <%=title%></title>
<link rel='stylesheet' type='text/css' href='./surveytool.css' />

			<script>
			// TODO: from survey.js
				function testsToHtml(tests) {
					var newHtml = "";
					for ( var i = 0; i < tests.length; i++) {
						var testItem = tests[i];
						newHtml += "<p class='tr_" + testItem.type + "' title='" + testItem.type
								+ "'>";
						if (testItem.type == 'Warning') {
							newHtml += warnIcon;
							// what='warn';
						} else if (testItem.type == 'Error') {
							//td.className = "tr_err";
							newHtml += stopIcon;
				//			what = 'error';
						}
						newHtml += tests[i].message;
						newHtml += "</p>";
					}
					return newHtml;
				}
			
			// TODO: from ajax_status.jsp
			var warnIcon = "<%= WebContext.iconHtml(request,"warn","Test Warning") %>";
			var stopIcon = "<%= WebContext.iconHtml(request,"stop","Test Error") %>";
			</script>


</head>
<body>

	<a href="upload.jsp?s=<%=sid%>&email=<%= theirU.email %>">Re-Upload File/Try Another</a> |
	<a href="<%=request.getContextPath()%>/survey">Return to the
		SurveyTool <img src='STLogo.png' style='float: right;' />
	</a>
	<hr />
	<h3>
		SurveyTool File Submission |
		<%=title%>
		|
		<%=theirU.name%></h3>

	<i>Checking upload...</i>

	<%
		final CLDRLocale loc = CLDRLocale.getInstance(cf.getLocaleID());
                
                if(!ident.isEmpty()) {
	%>
        <div class='fnotebox'>
                Note: Clicking the following links will switch to the user <%= theirU.email %>
        </div>
        <%
                        }
        %>

	<h3>
		Locale:
		<%=loc + " - "
					+ loc.getDisplayName(SurveyMain.BASELINE_LOCALE)%></h3>
	<%
		//cs.stuff.put("SubmitLocale",cf);
		CLDRFile baseFile = cs.sm.getSTFactory().make(loc.getBaseName(),
				false);
		XMLSource stSource = cs.sm.getSTFactory().makeSource(
				loc.getBaseName());

		Set<String> all = new TreeSet<String>();
		for (String x : cf) {
			if (x.startsWith("//ldml/identity")) {
				continue;
			}
			all.add(x);
		}
		int updCnt = 0;
	%>

	<h4>
		Please review these
		<%=all.size()%>
		entries.
	</h4>
<% request.setAttribute("BULK_STAGE", doFinal?"submit":"test"); %>
<%@include file="/WEB-INF/jspf/bulkinfo.jspf" %>

<% if(!doFinal) { %>
	<div class='helpHtml'>
		Please review these items carefully. The "NEXT" button will not appear until the page fully loads. Pressing NEXT will
		submit these votes.
		<br>
		For help, see: <a target='CLDR-ST-DOCS' href='http://cldr.unicode.org/index/survey-tool/upload'>Using Bulk Upload</a> 
	</div>
	
	<%-- <form action='<%=request.getContextPath() + request.getServletPath()%>'
		method='POST'>
		<input type='hidden' name='s' value='<%=sid%>' />
                <input type='hidden' name='email' value='<%=theirU.email%>' /><input
			type='submit' name='dosubmit' value='<%= submitButtonText %>' />
	</form> --%>
<% } else { %>
	<div class='bulkNextButton'>
	<b>Submitted!</b><br/>
	<a href="upload.jsp?s=<%=sid%>&email=<%= theirU.email %>">Another?</a>
	</div>
	<div class='helpHtml'>
		Items listed have been submitted as <%= theirU.email %>
		<br>
		For help, see: <a target='CLDR-ST-DOCS' href='http://cldr.unicode.org/index/survey-tool/upload'>Using Bulk Upload</a> 
	</div>
<% } %>

	<table class='data'>

		<thead>
			<tr>
				<th>xpath</th>
<!-- 				<th>Current Winner</th> -->
				<th>My Value</th>
				<th>Comment</th>
			</tr>
		</thead>

<%
	DisplayAndInputProcessor processor = new DisplayAndInputProcessor(loc,false);
	STFactory stf = CookieSession.sm.getSTFactory();
    BallotBox<UserRegistry.User> ballotBox = stf.ballotBoxForLocale(loc);
    SupplementalDataInfo sdi = cs.sm.getSupplementalDataInfo();


	int r = 0;
	XPathParts xppMine = new XPathParts(null, null);
	XPathParts xppBase = new XPathParts(null, null);
    final List<CheckCLDR.CheckStatus> checkResult = new ArrayList<CheckCLDR.CheckStatus>();
    TestCache.TestResultBundle cc = stf.getTestResult(loc, DataSection.getOptions(null, cs, loc));
	UserRegistry.User u = theirU;
	CheckCLDR.Phase cPhase = CLDRConfig.getInstance().getPhase();
	Set<String> allValidPaths = stf.getPathsForFile(loc);
	CLDRProgressTask progress = cs.sm.openProgress("Bulk:" + loc, all.size());
	try {
	
		CoverageInfo coverageInfo = CLDRConfig.getInstance()
				.getCoverageInfo();
		for (String x : all) {
			//progress.update(r++);

			String full = cf.getFullXPath(x);
			String alt = XPathTable.getAlt(full, xppMine);
			String valOrig = cf.getStringValue(x);
			Exception exc[] = new Exception[1];
			final String val0 = processor.processInput(x, valOrig, exc);
			String altPieces[] = LDMLUtilities.parseAlt(alt);
			String base = XPathTable.xpathToBaseXpath(x, xppMine);
			xppMine.removeAttribute(-1, LDMLConstants.DRAFT);
			base = xppMine.toString();
			//base = XPathTable.removeDraft(base, xppMine);
			int base_xpath_id = cs.sm.xpt.getByXpath(base);

			String valb = baseFile.getWinningValue(base);

			String style = "";
			String stylea = "";
			String valm = val0;
			if (valb == null) {
				valb = "(<i>none</i>)";
				stylea = "background-color: #fdd";
				style = "background-color: #bfb;";
			} else if (!val0.equals(valb)) {
				style = "font-weight: bold; background-color: #bfb;";
			} else {
				style = "opacity: 0.9;";
			}
			int vet_type[] = new int[1];

			int j = -1;
			Set<String> resultPaths = new HashSet<String>();
			XPathParts xpp = XPathParts.getTestInstance(base);
			xpp.removeAttribute(-1, LDMLConstants.ALT);
			String baseNoAlt = xpp.toString();
			int root_xpath_id = cs.sm.xpt.getByXpath(baseNoAlt);

			int coverageValue = 0;

			try {
				coverageValue = coverageInfo.getCoverageValue(base,
						loc.getBaseName());
			} catch (Throwable t) {
				SurveyLog.warnOnce("getCoverageValue failed for "
						+ loc.getBaseName() + ": " + t.getMessage());
			}

			String result = "";
			String resultStyle = "";

			String resultIcon = "okay";

			PathHeader ph = stf.getPathHeader(base);

			if (!allValidPaths.contains(base)) {
				result = "Item is not a valid XPath.";
				resultIcon = "stop";
			} else if (ph == null) {
				result = "Item is not a SurveyTool-visible LDML entity.";
				resultIcon = "stop";
			} else {
				checkResult.clear();
				cc.check(base, checkResult, val0);

				SurveyToolStatus phStatus = ph.getSurveyToolStatus();

				DataSection section = DataSection.make(null, null, cs, loc, base, null, false,
						Level.COMPREHENSIVE.toString());
				section.setUserAndFileForVotelist(cs.user, null);

				DataSection.DataRow pvi = section.getDataRow(base);
				final Level covLev = pvi.getCoverageLevel();
				//final int coverageValue = covLev.getLevel();
				CheckCLDR.StatusAction showRowAction = pvi
						.getStatusAction();

				if (showRowAction.isForbidden()) {
					result = "Item may not be modified. ("
							+ showRowAction + ")";
					resultIcon = "stop";
				} else {
					CandidateInfo ci;
					if (val0 == null) {
						ci = null; // abstention
					} else {
						ci = pvi.getItem(val0); // existing
												// item?
						if (ci == null) { // no, new item
							ci = new CandidateInfo() {
								@Override
								public String getValue() {
									return val0;
								}

								@Override
								public Collection<UserInfo> getUsersVotingOn() {
									return Collections.emptyList(); // No
																	// users
																	// voting
																	// -
																	// yet.
								}

								@Override
								public List<CheckCLDR.CheckStatus> getCheckStatusList() {
									return checkResult;
								}
							};
						}
					}
					CheckCLDR.StatusAction status = cPhase
							.getAcceptNewItemAction(ci, pvi,
									CheckCLDR.InputMethod.BULK,
									phStatus, cs.user);

					if (status != CheckCLDR.StatusAction.ALLOW) {
						result = "Item will be skipped. (" + status
								+ ")";
						resultIcon = "stop";
					} else {
						if (doFinal) {
							ballotBox.voteForValue(u, base, val0);
							result = "Vote accepted";
							resultIcon = "vote";
						} else {
							result = "Ready to submit.";
						}
						updCnt++;
					}
				}
			}
%>
		<tr class='r<%=(r) % 2%>'>
			<th title='<%=base + " #" + base_xpath_id%>'
				style='text-align: left; font-size: smaller;'><a
				target='<%=WebContext.TARGET_ZOOMED%>'
				href='<%=request.getContextPath()
							+ "/survey?_="+ loc + "&strid=" + cs.sm.xpt.getStringIDString(base_xpath_id) + ident %>'>
					<%=ph.toString()%></a>
			</a><br><tt><%= base %></tt></tt></th>
		<!--  	<td style='<%=stylea%>'><%=valb%></td> -->
			<td style='<%=style%>'><%=valm%>
			<% if(!valm.equals(valOrig)) { %>
				<div class='graybox' title='original text'><%= valOrig %></div>
			<% } %>
				</td>
			<td title='vote:' style='<%=resultStyle%>'>
			<% if(!checkResult.isEmpty()){  %>
			<script>
				document.write(testsToHtml(<%= SurveyAjax.JSONWriter.wrap(checkResult) %>));				
			</script>
			<% }  %>
				<%=WebContext.iconHtml(request, resultIcon, result)%><%=result%>
		</tr>
		<%
			}
			} finally {
				progress.close();
			}
		%>

	</table>

	<hr />
	<%	if(doFinal) { %>
	Voted on
	<%  } else { %>
	Ready to submit
	<%  } %>
	<%=updCnt%>
	votes.
	<%
		if(!doFinal && updCnt>0) {
	%>
		<form action='<%=request.getContextPath() + request.getServletPath()%>'
			method='POST'>
			<input type='hidden' name='s' value='<%=sid%>' />
                        <input type='hidden' name='email' value='<%=email%>' /><input
                class='bulkNextButton'
				type='submit' name='dosubmit' value='<%= submitButtonText %>' />
		</form>
	<%
		 }
	%>

</body>
</html>