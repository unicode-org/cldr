
<%@page import="com.ibm.icu.dev.util.ElapsedTimer"%>
<%@page import="org.unicode.cldr.web.*"%>
<%@page import="org.unicode.cldr.util.*,java.util.*"%>
<%@page import="java.io.*"%>
<%@page import="java.sql.*"%>
<%@page import="org.unicode.cldr.test.*"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Soft to Hard Votes</title>
</head>
<body>
	<%
		/**

		 Access this like so:
		
			 http://<yourserver>/cldr-apps/admin-softToHardVotes.jsp?vap=<VAP>
			 
			 then to really vote
				 http://<yourserver>/cldr-apps/admin-softToHardVotes.jsp?vap=<VAP>&reallyVote=yes
		
		 where <VAP> is the admin@ password.
		
		 */
		long start = System.currentTimeMillis();
		ElapsedTimer overallTimer = new ElapsedTimer(
				"softVoteToHardVote update started" + new java.util.Date());
		int numupd = 0;
		final SurveyMain sm = CookieSession.sm; // workaround - get the static survey main
	%>

	<%
		if ((request.getParameter("vap") == null)
				|| !request.getParameter("vap").equals(SurveyMain.vap)) {
	%>
	Not authorized.
	<%
		return;
		}
		if (request.getParameter("startLetter") == null) {
	%>
	No start letter.
	<%
		return;
		}
	final String startLetter = request.getParameter("startLetter");
	final boolean reallyVote = request.getParameter("reallyVote") != null;
	%>


	Soft to Hard vote update
	<%
		final STFactory stf = sm.getSTFactory();
		Set<CLDRLocale> sortSet = new TreeSet<CLDRLocale>();
		sortSet.addAll(SurveyMain.getLocalesSet());
			try {
			    for (CLDRLocale loc : sortSet) {
					if (!loc.getLanguage().startsWith(startLetter) || loc.getCountry() != "")
						continue; // skip non language locales
	%>
	<h4 title='<%=loc.getBaseName()%>'><%=loc.getDisplayName()%></h4>
	<%
				final BallotBox<UserRegistry.User> bb = stf.ballotBoxForLocale(loc);
				final CLDRFile  file = stf.make(loc, true);
				int pathCount = 0;
				// now, for each xpath..
				for(final String xpath : file.fullIterable()) {
					// for each value
					final String value = "↑↑↑"; // 
					final Set<UserRegistry.User> votes = bb.getVotesForValue(xpath, value);
					if(votes != null && !votes.isEmpty()) { // did anyone vote for it?
                                                pathCount++;
						final String rightValue = file.getBaileyValue(xpath, null, null);
						%>
						<h5><%= xpath %> : <%= value %>  to <%= rightValue %> : </h5>
						<%
						for(final UserRegistry.User user : votes) {
							%>
								<b><%= user %></b>  
							<%
							
							if(reallyVote) {
								bb.voteForValue(user, xpath, rightValue);
								numupd++;
							}
						}
					}
				}
	%>
	<h4><%= pathCount %> paths</h4>
	<%
		}
	%>
	<%
		} finally {
			}
	%>
	<hr>
	Total upd:
	<%=numupd + "/" + (sortSet.size() + 2)%>
	Total time:
	<%=overallTimer%>
	:
	<%=((System.currentTimeMillis() - start) / (1000.0 * 60))%>min
	<%
		System.err.println(overallTimer + " - updated " + numupd + "/"
				+ (sortSet.size() + 2) + " in "
				+ (System.currentTimeMillis() - start) / (1000.0 * 60)
				+ " min");
	%>
</body>
</html>
