<%@ include file="/WEB-INF/jspf/stcontext.jspf"%><%-- setup 'ctx' --%>
<%@ include file="/WEB-INF/jspf/report.jspf"%>
<%@ page
	import="org.unicode.cldr.util.*,java.math.*,org.unicode.cldr.test.*"%>
<%!Random rand = new Random(System.currentTimeMillis());

	enum Actions {
		value, confirm
	};
	

	String randString(UnicodeSet exem) {
		StringBuilder str = new StringBuilder(10);
		int length = rand.nextInt(10) + 1;
		if(rand.nextDouble()<0.002) {
			for(int i=1;i<length;i++)
				str.appendCodePoint(65+rand.nextInt(18));
		} else 
		for (; length > 0; length--) {
			int n  =rand.nextInt(exem.size()-1);
			int cp = exem.charAt(n);
			if(cp==-1) {
				continue;
			}
			str.appendCodePoint(cp);
		}
		return str.toString();
	}%>
<%
	if (!ctx.field("vap").equals(ctx.sm.vap)) {
%><h1>Not Authorized.</h1>
<%
	response.sendRedirect(request.getContextPath());
		return;
	}
	CLDRFile f = ctx.sm.getSTFactory().make(
			ctx.getLocale().getBaseName(), true);
	List<String> allXpaths = new ArrayList<String>();
	UnicodeSet exem = f.getExemplarSet("",
			CLDRFile.WinningChoice.WINNING);
	BallotBox<UserRegistry.User> ballotBox = ctx.sm.getSTFactory()
			.ballotBoxForLocale(ctx.getLocale());

	TestCache.TestResultBundle bund = ctx.sm.getSTFactory()
			.getTestResult(ctx.getLocale(), ctx.getOptionsMap());
	List<CheckCLDR.CheckStatus> result = new ArrayList<CheckCLDR.CheckStatus>();
	Actions allActions[] = Actions.values();

	for (Iterator<String> i = f.iterator(); i.hasNext();) {
		String s = i.next();
		allXpaths.add(s);
	}
%>

<ol>
	<%
		for (int i = 0; i < 1000; i++) {
			Actions nextAction = allActions[rand.nextInt(allActions.length)];
			String xpath = allXpaths.get(rand.nextInt(allXpaths.size()));
			String randStr = randString(exem);
	switch (nextAction) {
		case confirm:
			Set<String> strs = ballotBox.getValues(xpath);
			if(strs!=null&&!strs.isEmpty()) {
				if(strs.size()==1) {
					randStr = strs.iterator().next().toString();
				} else {
					randStr = strs.toArray()[rand.nextInt(strs.size()-1)].toString();
				}
			}
			/* falls through */
		case value:
			%><li><%= nextAction %>:<tt class='codebox' title='<%=xpath%>'><%=ctx.sm.xpt.getPrettyPath(xpath)%></tt><br>
			<span class='selected'><%=randStr%></span><br>
			<%
			result.clear();
			bund.check(xpath, result, randStr);
			boolean hadErr = false;
			%><ul><%
			for (CheckCLDR.CheckStatus cs : result) {
				%><li><%
				if (cs.getType()
						.equals(CheckCLDR.CheckStatus.errorType)) {
					hadErr = true;
					out.print(ctx.iconHtml("stop", "error"));
				}
				if(cs.getType().equals(CheckCLDR.CheckStatus.warningType)) {
					out.print(ctx.iconHtml("warn", "warning"));
					if(!hadErr&& rand.nextInt(20)==0) {
						hadErr = true; // reconsider
						out.print(ctx.iconHtml("squo","reconsidered"));
					}
				}
				out.println("<b>"+cs.getType()+"</b> "+cs.getSubtype()+":"+"<span style='font-size: x-small;'>"+cs.toString() + "</span>");
				%></li><%
			}
			%></ul><%
			if (!hadErr) {
				out.print(ctx.iconHtml("vote", "Voted!"));
				ballotBox.voteForValue(ctx.session.user, xpath, randStr);
				String newWin = f.getStringValue(xpath);
				if(!newWin.equals(randStr)) {
					out.print("<span class='winner'>"+newWin+"</span>");
				}
			}
			break;
		}
	}
%>
</ol>
