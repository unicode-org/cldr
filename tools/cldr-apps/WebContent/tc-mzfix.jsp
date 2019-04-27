<%@page import="org.unicode.cldr.web.DataSection.DataRow.CandidateItem"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	import='java.sql.*,org.unicode.cldr.util.*,java.util.*'
    pageEncoding="UTF-8"%><%@ include file="/WEB-INF/jspf/session.jspf" %>
<% if(cs.user.userlevel > UserRegistry.ADMIN) { response.sendRedirect(surveyUrl); return; } 
boolean reallymove = request.getParameter("reallymove")!=null;
%>
<html>
	<head>
		<title>Unicode | MZ Fixup</title>
		<link rel='stylesheet' type='text/css' href='./surveytool.css' />
	</head>
    
    <body>

<%!
	XPathTable xpt = null;

	Map<CLDRLocale,Set<Integer>> addTo(Map<CLDRLocale,Set<Integer>> map, String loc, int xpath) {
		if(map==null) map =  new TreeMap<CLDRLocale,Set<Integer>>();
		CLDRLocale locale = CLDRLocale.getInstance(loc);
		Set<Integer> s = map.get(locale);
		if(s==null) {
			map.put(locale,(s=new HashSet<Integer>()));
		}
		s.add(xpath);
		return map;
	}

	Map<Integer,List<String>> addTo(Map<Integer,List<String>>  map, int base_xpath, String msg) {
		//if(map==null) map =  new TreeMap<CLDRLocale,Set<Integer>>();
		List<String> s = map.get(base_xpath);
		if(s==null) {
			map.put(base_xpath,(s=new ArrayList<String>()));
		}
		s.add(msg);
		return map;
	}
	
	String xp(int xp) {
		return "<span style='border:1px solid gray;background-color:#dde;'>"+xpt.getById(xp) + "<sup style='font-size: 0.5em;'>#"+xp+"</sup></span>";
	}
	
	IntHash<Integer> badToGood = new IntHash<Integer>();
	
	int fixXpath(int bad) {
		return fixXpath(bad, null);
	}
	
	int fixXpath(int bad, String badString, SurveyMain sm) {
		if(sm!=null && this.xpt==null) {
			this.xpt=sm.xpt;
		}
		return fixXpath(bad,badString);
	}
	int fixXpath(int bad, String badString) {
		if(bad==XPathTable.NO_XPATH) {
			return bad;
		}
		Integer good = badToGood.get(bad);
		if(good==null) {
			if(badString==null) badString=xpt.getById(bad);
			if(badString==null) return XPathTable.NO_XPATH;
			XPathParts xpp = XPathParts.getTestInstance(badString);
			
			// is it an intact metazone?
			String mz = xpp.getElement(3);
			
			if(mz.equals("metazone")) {
				// reduplicated type.
				if(xpp.size()>5) {
					xpp.putAttributeValue(5,"type",null);  //As in: (3 and 5 added for clarity )  //ldml/dates/timeZoneNames/3metazone[@type="Brasilia"]/long/5generic[@type="Brasilia"]/long/generic 
				}
				if(xpp.size()>7 && xpp.getElement(5).equals(xpp.getElement(7))) {
					xpp.putAttributeValue(6,"alt",xpp.getAttributeValue(7,"alt"));
					xpp.putAttributeValue(6,"draft",xpp.getAttributeValue(7,"draft"));
					xpp.removeElement(7);
				}
				if(xpp.size()>6 && xpp.getElement(4).equals(xpp.getElement(6))) {
					xpp.putAttributeValue(5,"alt",xpp.getAttributeValue(6,"alt"));
					xpp.putAttributeValue(5,"draft",xpp.getAttributeValue(6,"draft"));
					xpp.removeElement(6);
				}
				good = xpt.getByXpath(xpp.toString());
			} else  if(mz.startsWith("metazone")){
				xpp.setElement(3,"metazone");  //As in: (where '*' is snowman)  //ldml/dates/timeZoneNames/metazone*America[@type="Argentina_Western"]/long/generic
				good = xpt.getByXpath(xpp.toString());
			} else {
				throw new InternalError("Don't know how to handle bad xpath " + badString + " #" +bad);
			}
			
			badToGood.put(bad,good);
		}
		return good;
	}
%>
<%

this.xpt = cs.sm.xpt;
int moveDataTotal=0;
int nukeTotal=0;
int moveVoteTotal=0;

Connection conn = null;
PreparedStatement nukeData = null, moveVote=null, moveData=null, nukeVote=null;
	try {
		conn = cs.sm.dbUtils.getDBConnection();
		
		moveVote = cs.sm.dbUtils.prepareForwardReadOnly(conn, "update cldr_vet set base_xpath=?,vote_xpath=? where id=?");
		moveData = cs.sm.dbUtils.prepareForwardReadOnly(conn, "update cldr_data set xpath=?,origxpath=?,base_xpath=? where id=?");
		nukeData = cs.sm.dbUtils.prepareForwardReadOnly(conn, "delete from cldr_data where locale=? and base_xpath=?");
		nukeVote = cs.sm.dbUtils.prepareForwardReadOnly(conn, "delete from cldr_vet where id=?");

		long n = System.currentTimeMillis();
		Object dataHits[][] = cs.sm.dbUtils.sqlQueryArrayArrayObj(conn,"select cldr_xpaths.id,cldr_data.locale from cldr_xpaths  join cldr_data    on     (cldr_data.base_xpath = cldr_xpaths.id)  where     cldr_xpaths.xpath like '//ldml/dates/timeZoneNames/metazone%'  and     ( cldr_xpaths.xpath like '//ldml/dates/timeZoneNames/metazone[@type=%@type=%'   or    cldr_xpaths.xpath like '%â˜ƒ%')  order by    cldr_data.locale,cldr_xpaths.xpath;");		
		Object vetHits[][] = cs.sm.dbUtils.sqlQueryArrayArrayObj(conn, "  select cldr_xpaths.id,cldr_vet.locale from cldr_xpaths  join cldr_vet    on     (cldr_vet.base_xpath = cldr_xpaths.id)  where     cldr_xpaths.xpath like '//ldml/dates/timeZoneNames/metazone%'  and     ( cldr_xpaths.xpath like '//ldml/dates/timeZoneNames/metazone[@type=%@type=%'   or    cldr_xpaths.xpath like '%â˜ƒ%')  order by    cldr_vet.locale,cldr_xpaths.xpath;");
%>
<a href="<%=request.getContextPath()%>/survey?do=list">Return to the SurveyTool <img src='STLogo.png' style='float:right;' /></a>
<hr/>
<h2>Special Fixup</h2>

Loaded <%= dataHits.length %> data items and <%= vetHits.length %> votes in <%= com.ibm.icu.dev.util.ElapsedTimer.elapsedTime(n) %>
<br>

<%

Map<CLDRLocale,Set<Integer>> data = null;
Map<CLDRLocale,Set<Integer>> votes = null;

for(Object o[] : dataHits) {
	data = addTo(data,(String)o[1],(Integer)o[0]);
}
for(Object o[] : vetHits) {
	votes = addTo(votes,(String)o[1],(Integer)o[0]);
}

Set<CLDRLocale> allLocs = new TreeSet<CLDRLocale>();
XPathParts xpp = new XPathParts(null,null);
if(data!=null) {
	allLocs.addAll(data.keySet());
}
if(votes!=null) {
	allLocs.addAll(votes.keySet());
}
%>
<h2>Jump To</h2>
<%
for(CLDRLocale l : allLocs) {
%><a href='#<%= l %>'><%= l %></a> <% 
} %>

<%
for(CLDRLocale l : allLocs) { 
	// fetch data for this locale
Set<Integer> thisData = (data!=null)?data.get(l):null;
Set<Integer> thisVotes = (votes!=null)?votes.get(l):null;

%>
	<h3><a name="<%= l %>"><%= l %> (<%= l.getDisplayName(SurveyMain.BASELINE_LOCALE) %>)</a></h3>
	<%= (thisData!=null)?(thisData.size()):0 %> data items, 
	<%= (thisVotes!=null)?(thisVotes.size()):0 %> votes to fix.<br/>

	<blockquote>
	
	<%
		Map<Integer,List<String>> toFix = new TreeMap<Integer,List<String>>();	

	// fix votes first.
	   if(thisVotes!=null) {
			for(int xpath : thisVotes) {
				String badString = cs.sm.xpt.getById(xpath);
				int base_xpath = fixXpath(xpath, badString, cs.sm);
				
				// todo: optimize *
				Map<String, Object> possData[] = cs.sm.dbUtils.sqlQueryArrayAssoc(conn, "select cldr_data.id,cldr_data.base_xpath,cldr_data.xpath,cldr_data.value,cldr_data.origxpath from cldr_data where cldr_data.locale=? and ( cldr_data.base_xpath=? or cldr_data.base_xpath=?)", l, xpath, base_xpath);
				Map<String, Object> theseVotes[] = cs.sm.dbUtils.sqlQueryArrayAssoc(conn, "select cldr_vet.submitter,cldr_vet.id,cldr_vet.vote_xpath,cldr_vet.modtime,cldr_vet.base_xpath from cldr_vet where cldr_vet.locale=? and cldr_vet.base_xpath=? or cldr_vet.base_xpath=?", l, xpath, base_xpath);
				
				if(theseVotes==null||theseVotes.length==0) {
					throw new InternalError("Couldn't load votes for " + l + " : " + xpath);
				}
				Set<Integer> goodSubmit = new TreeSet<Integer>();
				Map<Integer,Integer> badSubmit = new HashMap<Integer,Integer>();

				Map<String,Integer> valToData = new HashMap<String,Integer>();
				for(Map<String,Object> d : possData) {
					int aBase = (Integer)d.get("base_xpath");
					if(aBase!=base_xpath) continue; // BAD data, skip
					int aXpath = (Integer)d.get("xpath");
					String aVal = (String)d.get("value");
					valToData.put(aVal,aXpath);
				}

				Map<Integer,Map<String,Object>> submitterToGoodVote = new TreeMap<Integer,Map<String,Object>>();

				// collect 'good' votes
				for(Map<String,Object> m : theseVotes) {
					int aVoteBaseXpath = (Integer)m.get("base_xpath");
					int aSubmitter = (Integer)m.get("submitter");
					if(aVoteBaseXpath == base_xpath) { 
						// already fixed
						submitterToGoodVote.put(aSubmitter,m);
					}
				}

				for(Map<String,Object> m : theseVotes) {
					int aId = (Integer)m.get("id");
					int aVoteXpath = (Integer)m.get("vote_xpath");
					int aFixVoteXpath = fixXpath(aVoteXpath);
					int aSubmitter = (Integer)m.get("submitter");
					int aVoteBaseXpath = (Integer)m.get("base_xpath");
					
					if(aVoteBaseXpath == base_xpath) { 
						// already collected
						continue;
					}

					int nukeVoteId = -1; // vote to remove?					

					int moveTo = XPathTable.NO_XPATH; // target XPATH of vote move
					
					Map<String,Object>goodm = submitterToGoodVote.get(aSubmitter); // row of VOTE row for this summitter, for good data
					Map<String,Object> dataToMove = null; // map of data row that must move

					if(goodm != null) {
						Timestamp badTimestamp  = (Timestamp)m.get("modtime");
						Timestamp goodTimestamp  = (Timestamp)goodm.get("modtime");
						int goodId  = (Integer)goodm.get("id");
						
						if(goodTimestamp.after(badTimestamp)) {
							nukeVoteId = aId;
						} else {
							nukeVoteId = goodId;
							addTo(toFix, base_xpath, "Note: Newer BAD vote by " + aSubmitter + " - nuke GOOD vote " + goodId);
						}
					}
					
					if(nukeVoteId==aId) {
						addTo(toFix, base_xpath, "Note: Newer vote by " + aSubmitter + " - nuke bad vote " + aId);
						goodSubmit.add(aSubmitter);
						// don't analyze further.
					} else if(aVoteXpath==XPathTable.NO_XPATH) {
						addTo(toFix, base_xpath, "GOOD abstention by " + aSubmitter + " " + xp(xpath) + " for " + xp(aVoteXpath) );
						goodSubmit.add(aSubmitter);
					} else {
						
						// first, look in the good data
							
						
						for(Map<String,Object> d : possData) {
							int aBase = (Integer)d.get("base_xpath");
							int aXpath = (Integer)d.get("xpath");
							String aVal = (String)d.get("value");

							Integer gooDataXpath = null;
							if(aVal!=null) gooDataXpath = valToData.get(aVal);

							if(aBase==base_xpath) {  // GOOD path
								// good
								if(aXpath==aVoteXpath) { // 
									addTo(toFix, base_xpath, "BAD vote by " + aSubmitter + " " + xp(xpath) + " for GOOD DATA " + xp(aVoteXpath) );
									moveTo = aVoteXpath; // Just need to move the base path, no need to change values.
								} else if(aXpath==aFixVoteXpath) {
									addTo(toFix, base_xpath, "BAD vote by " + aSubmitter + " " + xp(xpath) + " for GOOD fixable (a) " + xp(aFixVoteXpath) );
									moveTo = aFixVoteXpath; // move base and vote xpath, all OK
								} else {
									//addTo(toFix, base_xpath, "(nonex (a) " + xp(aBase));
								}
							} else if(aXpath==aVoteXpath) {
								if(gooDataXpath!=null) {
									moveTo=gooDataXpath;
									addTo(toFix, base_xpath, "BAD vote by " + aSubmitter + " " + xp(xpath) + " for " + xp(aVoteXpath) + " REDIR <br>" + xp(moveTo) );
								} else {
									// base needs to move, and data needs to move..
									dataToMove=d;
									addTo(toFix, base_xpath, "BAD vote by " + aSubmitter + " " + xp(xpath) + " for found but BAD data " + xp(aVoteXpath)  + " - NEED DATA MOVE");
								}								
								goodSubmit.add(aSubmitter);
							} else if(aXpath==aFixVoteXpath) {
								if(true) throw new InternalError("Doesn't occur. not imp.");
								if(gooDataXpath!=null) {
									moveTo=gooDataXpath;
									addTo(toFix, base_xpath, "BAD vote by " + aSubmitter + " " + xp(xpath) + " for fixable " + xp(aVoteXpath) + " REDIR <br>" + xp(moveTo) );
								} else {
									addTo(toFix, base_xpath, "BAD vote by " + aSubmitter + " " + xp(xpath) + " for fixable " + xp(aFixVoteXpath) + " - NEED DATA MOVE");
									// Data has a bad base path, but a good final path.
									// Data needs to move.
									dataToMove=d;
									moveTo = aFixVoteXpath;
								}
							} else {
								//addTo(toFix, base_xpath, "(nonex (b) " + xp(aBase));
							}
						}
						
						if(moveTo==XPathTable.NO_XPATH) {
							badSubmit.put(aSubmitter,aFixVoteXpath);
						} else {
							goodSubmit.add(aSubmitter);
						}
					}

					
					// Now, do the moves
					if(reallymove) {
						if(dataToMove != null) {
							int adBase = (Integer)dataToMove.get("base_xpath");
							int adXpath = (Integer)dataToMove.get("xpath");
							int adId = (Integer)dataToMove.get("id");
							int adoXpath = (Integer)dataToMove.get("origxpath");
							String adVal = (String)dataToMove.get("value");
							//moveData = cs.sm.dbUtils.prepareForwardReadOnly(conn, "update cldr_data set xpath=?,origxpath=?,base_xpath=? where id=?");
							if(moveTo==-1) {
								// we don't know the right vxpath.  Make up something.
								//xpp.clear().initialize(xpt.getById());
								moveTo=fixXpath(adXpath);
								valToData.put(adVal,moveTo); // for other votes, don't move the data again!
								moveData.setInt(1,moveTo);
								moveData.setInt(2,fixXpath(adoXpath));
								moveData.setInt(3,base_xpath);
								moveData.setInt(4,adId);
								
								int rw = moveData.executeUpdate();
								moveDataTotal+=rw;
								
								addTo(toFix, base_xpath, "Moved data in id " + adId + " == " + rw);									
							} else {
								throw new InternalError("not imp.");
							}
						}
						
						if(nukeVoteId>=0) {
							nukeVote.setInt(1, nukeVoteId);
							int rw = nukeVote.executeUpdate();
							addTo(toFix, base_xpath, "Deleted vote id " + nukeVoteId+ " of " + aSubmitter + " == " + rw);
							moveVoteTotal+=rw;
						}
						
						// now, move the vote
						//moveVote = cs.sm.dbUtils.prepareForwardReadOnly(conn, "update cldr_vet set base_xpath=?,vote_xpath=? where id=?");
						{
							moveVote.setInt(1, base_xpath); // rebase
							moveVote.setInt(2, moveTo);
							moveVote.setInt(3, aId);
							
							int rw = moveVote.executeUpdate();
							addTo(toFix, base_xpath, "Moved vote in id " + aId+ " of " + xp(xpath) + " == " + rw);
							moveVoteTotal+=rw;
						}
					}

				}
				for(int good : goodSubmit) {
					badSubmit.remove(good);
				}
				for(Map.Entry<Integer,Integer> bad : badSubmit.entrySet()) {
					addTo(toFix, base_xpath, "Unfound vote by " + bad.getKey() + " " + xp(xpath) + " for NO DATA @  "+ xp(bad.getValue()));
				}
			}
	   }

		if(thisData!=null)
			for(int xpath : thisData) {
				String badString = cs.sm.xpt.getById(xpath);
				int base_xpath = fixXpath(xpath, badString, cs.sm);

				Map<String, Object> badData[] = cs.sm.dbUtils.sqlQueryArrayAssoc(conn, "select * from cldr_data where cldr_data.locale=? and ( cldr_data.base_xpath=? or cldr_data.base_xpath=?)", l, xpath, base_xpath);
				if(badData==null || badData.length==0) {
					addTo(toFix, base_xpath, "data: " + badString + " #"+xpath + " - query failed" );
					continue;
				}
				
				for(Map<String,Object> m : badData) {
					int aXpath = (Integer)m.get("xpath");
					int aId = (Integer)m.get("id");
					int aOxpath = (Integer)m.get("origxpath");
					int aBaseXpath = (Integer)m.get("base_xpath");
					
					if(aBaseXpath==base_xpath) {
						addTo(toFix, base_xpath, "EXIST GOOD: " + xp(aOxpath));
					} else {
						
						int fixOpath = fixXpath(aOxpath,null,cs.sm);
						int fixXpath = fixXpath(aXpath,null,cs.sm);
						
						addTo(toFix, base_xpath, "EXIST BAD : xp:"+xp(aXpath)+">>"+xp(fixXpath)+", <br>ox:"+xp(aOxpath)+">>"+xp(fixOpath));
					}
				}
				if(reallymove) {
					nukeData.setString(1,l.toString());
					nukeData.setInt(2,xpath);
					int rw = nukeData.executeUpdate();
					addTo(toFix, base_xpath, "Removed " + rw + " data rows from " + xp(xpath));
					nukeTotal+=rw;
				}
			}
	   
	   for(Map.Entry<Integer,List<String>> e : toFix.entrySet()) {
	%>
		<hr/> 
		<span style='border: 1px solid gray; margin: 3px; float: right; font-family: sans;'><%= cs.sm.xpt.getPrettyPath(e.getKey()) %></span><br/>
	 	<b><font size='-1'><%= cs.sm.xpt.getById(e.getKey()) %></font> - #<%= e.getKey() %> 
				  </b><ul>
		<% for(String s : e.getValue()) { %>
			<li><%= s %></li>
		<% } %>
		</ul>
	<% } %>
	</blockquote>

<%
}
%>

Moved  <%= moveDataTotal %> data items, <%= moveVoteTotal %> votes, and deleted <%= nukeTotal %> bad (unreferenced) data items. <br>


<%		
	} finally {
		DBUtils.close(moveData,nukeVote,nukeData,moveVote,conn);
	}
%>

</body>
</html>
