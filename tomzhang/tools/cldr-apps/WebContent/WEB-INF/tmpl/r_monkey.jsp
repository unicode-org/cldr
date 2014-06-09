<%
	if(true==true) throw new InternalError("Not implemented- broken.");
%>

<%@ include file="/WEB-INF/jspf/stcontext.jspf"%><%-- setup 'ctx' --%>
<%@ include file="/WEB-INF/jspf/report.jspf"%>
<%@ page
	import="org.unicode.cldr.util.*,java.math.*,org.unicode.cldr.test.*"%>
<%!Random rand = new Random(System.currentTimeMillis());

	enum Actions {
		value(100.0), confirm(300), chloc(1);
		
		Actions(double d) {
			this.weight = d;
		}
		double weight;
		
		static double getTotalWeight() {
			double tweight = 0;
			for(Actions a : values()) {
				tweight += a.weight;
			}
			return tweight;
		}
	   public static double totalWeight = Actions.getTotalWeight();
	};
	
    
    Actions getRandomAction() {
        double r = rand.nextDouble()*Actions.totalWeight;
        
        for(Actions a : Actions.values()) {
        	r -= a.weight;
        	if(r<=0) {
        		return a;
        	}
        }
        return null; // shouldn't happen?
    }

	String randString(UnicodeSet exem) {
		StringBuilder str = new StringBuilder(10);
		int length = rand.nextInt(10) + 1;
		if(rand.nextDouble()<0.002 || exem.size()<2) {
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
   boolean rl = ctx.hasField("randloc");
    CLDRLocale locs[] = ctx.sm.getLocales();   
    CLDRLocale locale = ctx.getLocale();
	List<String> allXpaths = new ArrayList<String>();
    CLDRFile f = ctx.sm.getSTFactory().make(
            locale.getBaseName(), true);
    UnicodeSet exem = f.getExemplarSet("",
            CLDRFile.WinningChoice.WINNING);
	BallotBox<UserRegistry.User> ballotBox = ctx.sm.getSTFactory()
			.ballotBoxForLocale(locale);
	TestCache.TestResultBundle bund = ctx.sm.getSTFactory()
			.getTestResult(locale, null /* TODO: ctx.getOptionsMap()*/);
	List<CheckCLDR.CheckStatus> result = new ArrayList<CheckCLDR.CheckStatus>();
	//Actions allActions[] = Actions.values();

	for (Iterator<String> i = f.iterator(); i.hasNext();) {
		String s = i.next();
		allXpaths.add(s);
	}
%>

<ol>
	<%
		for (int i = 0; i < 1000; i++) {
			Actions nextAction = getRandomAction(); // allActions[rand.nextInt(allActions.length)];
			String xpath = allXpaths.get(rand.nextInt(allXpaths.size()));
			String randStr = randString(exem);
	switch (nextAction) {
    	case chloc:
            if(rl) {
                locale = locs[rand.nextInt(locs.length)];
                   f = ctx.sm.getSTFactory().make(
                          locale.getBaseName(), true);
                  ballotBox = ctx.sm.getSTFactory()
                          .ballotBoxForLocale(locale);
                  bund = ctx.sm.getSTFactory()
                          .getTestResult(locale, null /* TODO:  ctx.getOptionsMap()*/);
                   exem = f.getExemplarSet("",
                          CLDRFile.WinningChoice.WINNING);
                  out.println("Now in locale " + locale + "<br>");
            }
            break;
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
