<%@ include file="/WEB-INF/jspf/stcontext.jspf" %><%-- setup 'ctx' --%><%

VoteResolver<String> r = (VoteResolver<String>)ctx.get("resolver"); 
if(r==null) { %><i>null resolver</i> <%  } else {
WebContext.HTMLDirection dir = ctx.getDirectionForLocale();
String winner = r.getWinningValue();
%>

<!--

	THIS FILE IS BELIEVED TO BE UNUSED (DEAD CODE).
	
-->


<b class='selected'>
<% if(r.isDisputed()) { %>
	<%= ctx.iconHtml("warn", "Disputed") %>Disputed 
<% } %>
<%= r.getWinningStatus().toString().toUpperCase() %> Value</b>: 
 <span dir='<%= dir %>' class='winner' title='#'><%= winner %></span>
    <br/>

<table id='voteinfo' class='tzbox'>
	<thead>
		<tr>
			<th>Org</th>
			<th>Organization's vote</th>
			<th>Dissenting Votes</th>
		</tr>
	</thead>

<%
	String lastRelease = r.getLastReleaseValue();
if(lastRelease != null ) {
%>
<b><%= r.getLastReleaseStatus().toString().toUpperCase() %> Last Release:</b> 		<span class='<%= lastRelease.equals(winner)?"winner":"value" %>' dir='<%= dir %>' title='#'><%= lastRelease %></span>
<% } else { %><i>(was not in <%= ctx.sm.getOldVersion() %>)</i><br/><% }  %>
<!--  now, per org.. -->
 <%
 
 	EnumSet<Organization> conflictedOrgs = r.getConflictedOrganizations();
 	for(Organization o: Organization.values()) {
 		String orgVote = r.getOrgVote(o);
 		if(orgVote==null) continue;
 		
 		Map<String,Long> votes = r.getOrgToVotes(o);
 %>
		<tr>
		<th class='org_<%= r.getStatusForOrganization(o) %>'><%= o %></th>
		
		<td>
				  	<span class='<%= orgVote.equals(winner)?"winner":"value" %>' dir='<%= dir %>' title='#'><%= orgVote %></span>
				  <span dir='ltr' class='scorebox'><%= votes.get(orgVote) %>: <%= r.getStatusForOrganization(o) %></span>
		</td>
		
		<td>
			<% for(Map.Entry<String,Long> e:votes.entrySet()) { 
				if(e.getKey().equals(orgVote)) continue;
			%>
				  	<span class='<%= e.getKey().equals(winner)?"winner":"value" %>'  dir='<%= dir %>' title='#'><%= e.getKey() %></span>
				  <span dir='ltr' class='scorebox'><%= e.getValue() %></span>
				  	<br/>
			<% } %>
		</td>
		
		</tr>	
 <%
 	}
 %>

</table>


<%
//ctx.println("<i>no voting results available-</i><br>resolver: " + getSTFactory().ballotBoxForLocale(section.locale()).getResolver(p.getXpath()));
        // ( Byte code err 47 (!!) in the following code...)
//        else {
//            Vetting.DataTester tester  = null;
//            /*try */ {
//                //tester = vet.getTester(uf.dbSource);
//                //Race r =  null; // vet.getRace(section.locale, p.getXpathId(),uf.dbEntry.getConnectionAlias(),tester);
//                Race r = new Race(getSTFactory().ballotBoxForLocale(section.locale()),p.getXpath());
//                ctx.println("<i>Voting results by organization:</i><br>");
//                ctx.print("<table class='list' border=1 summary='voting results by organization'>");
//                ctx.print("<tr class='heading'><th>Organization</th><th>Organization's Vote</th><th>Item</th><th>Score</th><th>Conflicting Votes</th></tr>");
//                int onn=0;
//                EnumSet<VoteResolver.Organization> conflictedOrgs;
//                if(r!=null)  conflictedOrgs= r.resolver.getConflictedOrganizations();
//                if(r!=null)  for(VoteResolver.Organization org : VoteResolver.Organization.values()) {
//                    String orgVote = r.getOrgVote(org);
//                    Map<String,Long> o2c = r.getOrgToVotes(org);
//
//                    ctx.println("<tr class='row"+(onn++ % 2)+"'>");
//                    long score=0;
//
//                    CandidateItem oitem = null;
//                    int on = -1;
//                    if(orgVote != null)
//                    {
//                        int nn=0;
//                        for(CandidateItem citem : numberedItemsList) {
//                            nn++;
//                            if(citem==null) continue;
//                            if(citem.value.equals(orgVote)) {
//                                oitem = citem;
//                                on=nn;
//                            }
//                        }
//                        if(oitem!=null) {
//                            Long l = o2c.get(oitem.value);
//                            if(l != null) {
//                                score = l;
//                                //				    SurveyLog.logger.warning(org.name+": ox " + oitem.xpathId + " -> l " + l + ", nn="+nn);
//                                if(on>=0) {
//                                    totals[on-1]+=score;
//                                }
//                            }
//                        }
//                    }
//
//                    ctx.print("<th>"+org.name()+"</th>");
//                    ctx.print("<td>");
//                    if(orgVote == null) {
//                        ctx.print("<i>(No vote.)</i>");
//                        if(conflictedOrgs.contains(org)) {
//                            ctx.print("<br>");
//                            ctx.print(ctx.iconHtml("disp","Vetter Dispute"));
//                            ctx.print(" (Dispute among "+org.name()+" vetters) ");
//                        }
//                    } else {
//                        //	    			String theValue = orgVote;
//                        //	    			if(theValue == null) {  
//                        //	    				if(orgVote.xpath == r.base_xpath) {
//                        //	    					theValue = "<i>(Old Vote for Status Quo)</i>";
//                        //	    				} else {
//                        //	    					theValue = "<strike>(Old Vote for Other Item)</strike>";
//                        //	    				}
//                        //	    			}
//                        //	    			if(orgVote.disqualified) {
//                        //	    				ctx.print("<strike>");
//                        //	    			}
//                        ctx.print("<span dir='"+ctx.getDirectionForLocale()+"'>");
//                        //						ctx.print(VoteResolver.getOrganizationToMaxVote(section.locale).
//                        //						            get(VoteResolver.Organization.valueOf(org.name)).toString().replaceAll("street","guest")
//                        ctx.print(ctx.iconHtml("vote","#")+orgVote+"</span>");
//                        //	    			if(orgVote.disqualified) {
//                        //	    				ctx.print("</strike>");
//                        //	    			}
//                        //	    			if(org.votes.isEmpty()/* && (r.winner.orgsDefaultFor!=null) && (r.winner.orgsDefaultFor.contains(org))*/) {
//                        //	    				ctx.print(" (default vote)");
//                        //	    			}
//                        // TODO: print which users voted here.
//
//                    }
//                    ctx.print("</td>");
//
//                    ctx.print("<td class='warningReference'>#"+on);
//                    if(on==-1) {
//                        ctx.print("<br/><b class='graybox'>Error: this value is missing.</b>");
//                    }
//                    ctx.print("</td> ");
//
//                    ctx.print("<td>"+score+"</td>");
//
//                    ctx.print("<td>");
//                    if(!o2c.isEmpty()) for(Map.Entry<String,Long> item : o2c.entrySet()) {
//                        if(item.getKey().equals(orgVote)) continue;
//
//                        ctx.print("<span dir='"+ctx.getDirectionForLocale()+"' class='notselected' title='#"+""+"'>"+item.getKey()+"</span>");
//                        ctx.print("<br>");
//                        ctx.print(" - Score:"+item.getValue());
//                        //	    			for(UserRegistry.User u : item.voters)  { 
//                        //	    				if(!u.voterOrg().equals(org.name)) continue;
//                        //	    				ctx.print(u.toHtml(ctx.session.user)+", ");
//                        //	    			}
//                        // TODO: print which users voted
//                        ctx.println("<hr>");
//                    }
//                    ctx.println("</td>");
//                    ctx.print("</tr>");
//                }
//                ctx.print("</table>"); // end of votes-by-organization
//
//                if(isUnofficial || UserRegistry.userIsTC(ctx.session.user)) {
//                    ctx.println("<div class='graybox'>"+r.resolverToString());
//                    ctx.print("</div>");
//                }
//                //
//                // TODO: explain teh winning.
//                //
//                //	    	if((r.nexthighest > 0) && (r.winner!=null)&&(r.winner.score==0)) {
//                //	    		// This says that the optimal value was NOT the numeric winner.
//                //	    		ctx.print("<i>not enough votes to overturn approved item</i><br>");
//                //	    	} else if(!r.disputes.isEmpty()) {
//                //	    		ctx.print(" "+ctx.iconHtml("warn","Warning")+"Disputed with: ");
//                //	    		for(Race.Chad disputor : r.disputes) {
//                //	    			ctx.print("<span title='#"+disputor.xpath+"'>"+disputor.value+"</span> ");
//                //	    		}
//                //	    		ctx.print("");
//                //	    		ctx.print("<br>");
//                //	    	} else if(r.hadDisqualifiedWinner) {
//                //	    		ctx.print("<br><b>"+ctx.iconHtml("warn","Warning")+"Original winner of votes was disqualified due to errors.</b><br>");
//                //	    	}
//                //	    	if(isUnofficial && r.hadOtherError) {
//                //	    		ctx.print("<br><b>"+ctx.iconHtml("warn","Warning")+"Had Other Error.</b><br>");
//                //	    	}
//
//                //	    	ctx.print("<br><hr><i>Voting results by item:</i>");
//                //	    	ctx.print("<table class='list' border=1 summary='voting results by item'>");
//                //	    	ctx.print("<tr class='heading'><th>Value</th><th>Item</th><th>Score</th><th>O/N</th><th>Status "+oldVersion+"</th><th>Status "+newVersion+"</th></tr>");
//                //	    	int nn=0;
//                //
//                //	    	int lastReleaseValue= r.getLastReleaseValue();
//                //	    	String lastReleaseStatus = r.getLastReleaseStatus().toString();
//                //
//                //	    	for(CandidateItem citem : numberedItemsList) {
//                //	    		ctx.println("<tr class='row"+(nn++ % 2)+"'>");
//                //	    		if(citem==null) {ctx.println("</tr>"); continue; } 
//                //	    		String theValue = citem.value;
//                //	    		String title="X#"+citem.xpathId;
//                //
//                //	    		// find Chad item that matches citem
//                //
//                //	    		long score = -1;
//                //	    		Race.Chad item = null;
//                //	    		if(citem.inheritFrom==null) {
//                //	    			for(Race.Chad anitem : r.chads.values()) {
//                //	    				if(anitem.xpath==citem.xpathId) {
//                //	    					item = anitem;
//                //	    					title="#"+item.xpath;
//                //	    				}
//                //	    			}
//                //	    		}
//                //
//                //	    		//for(Race.Chad item : r.chads.values()) {
//                //	    		if(item!=null&&theValue == null) {  
//                //	    			if(item.xpath == r.base_xpath) {
//                //	    				theValue = "<i>(Old Vote for Status Quo)</i>";
//                //	    			} else {
//                //	    				theValue = "<strike>(Old Vote for Other Item"+")</strike>";
//                //	    			}
//                //	    		}
//                //
//                //	    		ctx.print("<td>");
//                //	    		if(item!=null) {
//                //	    			if(item == r.winner) {
//                //	    				ctx.print("<b>");
//                //	    			}
//                //	    			if(item.disqualified) {
//                //	    				ctx.print("<strike>");
//                //	    			}
//                //	    		}
//                //	    		ctx.print("<span dir='"+ctx.getDirectionForLocale()+"' title='"+title+"'>"+theValue+"</span> ");
//                //	    		if(item!=null) {
//                //	    			if(item.disqualified) {
//                //	    				ctx.print("</strike>");
//                //	    			}
//                //	    			if(item == r.winner) {
//                //	    				ctx.print("</b>");
//                //	    			}
//                //	    			if(item == r.existing) {
//                //	    				ctx.print(ctx.iconHtml("star","existing item"));
//                //	    			}
//                //	    		}
//                //	    		ctx.print("</td>");
//                //
//                //	    		ctx.println("<th class='warningReference'>#"+nn+"</th>");
//                //	    		if(item!=null) {
//                //	    			ctx.print("<td>"+ totals[nn-1] +"</td>");
//                //	    			if(item == r.Ochad) {
//                //	    				ctx.print("<td>O</td>");
//                //	    			} else if(item == r.Nchad) {
//                //	    				ctx.print("<td>N</td>");
//                //	    			} else {
//                //	    				ctx.print("<td></td>");
//                //	    			}
//                //	    			if(item.xpath == lastReleaseXpath) {
//                //	    				ctx.print("<td>"+lastReleaseStatus.toString().toLowerCase()+"</td>");
//                //	    			} else {
//                //	    				ctx.print("<td></td>");
//                //	    			}
//                //	    			if(item == r.winner) {
//                //	    				ctx.print("<td>"+r.vrstatus.toString().toLowerCase()+"</td>");
//                //	    			} else {
//                //	    				ctx.print("<td></td>");
//                //	    			}
//                //
//                //	    		} else {
//                //	    			/* no item */
//                //	    			if(citem.inheritFrom!=null) {
//                //	    				ctx.println("<td colspan=4><i>Inherited from "+citem.inheritFrom+"</i></td>");
//                //	    			} else {
//                //	    				ctx.println("<td colspan=4><i>Item not found!</i>");
//                //		    			if(isUnofficial) {
//                //		    				ctx.println("Looking for xpid " + citem.xpathId+"=="+xpt.getById(citem.xpathId)+"<br/>");
//                //		    				for(Race.Chad anitem : r.chads.values()) {
//                //		    					ctx.println(anitem+"="+anitem.xpath+"=="+xpt.getById(anitem.xpath)+"<br/>");
//                //			    			}
//                //		    			}
//                //	    				ctx.println("</td>");
//                //	    			}
//                //	    		}
//                //	    		ctx.print("</tr>");
//                //	    	}
//                //	    	ctx.print("</table>");
//
//
//                //if(UserRegistry.userIsTC(ctx.session.user)) {
//                //	    	if()
//                //	    	if(r.winner != null ) {
//                //	    		CandidateItem witem = null;
//                //	    		int wn = -1;
//                //	    		nn=0;
//                //	    		for(CandidateItem citem : numberedItemsList) {
//                //	    			nn++;
//                //	    			if(citem == null) continue;
//                //	    			if(r.winner.xpath==citem.xpathId) {
//                //	    				witem = citem;
//                //	    				wn=nn;
//                //	    			}
//                //	    		}
//                ctx.print("<b class='selected'>Optimal field</b>: "+r.resolver.getWinningStatus()+" <span dir='"+ctx.getDirectionForLocale()+"' class='winner' title='#'>"+r.resolver.getWinningValue()+"</span>");
//                //	    	}
//
//                ctx.println("For more information, see <a href='http://cldr.unicode.org/index/process#Voting_Process'>Voting Process</a><br>");
//                //	    } catch (SQLException se) {
//                //		ctx.println("<div class='ferrbox'>Error fetching vetting results:<br><pre>"+se.toString()+"</pre></div>");
//            }
//        }
%>

<% if(SurveyMain.isUnofficial()) { %>
<tt>
 <%= r.toString() %>
 </tt>
<% } %>

<% } /* end null resolver */ %>