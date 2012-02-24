package org.unicode.cldr.web;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.PathUtilities;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.web.UserRegistry.User;

import com.ibm.icu.util.ULocale;

/**
 * TODO: this object exists just to capture the following code from the being-removed Vetting object.
 * @author srl
 *
 */
public class DisputePageManager {
    static void doDisputed(WebContext ctx){
        ctx.sm.printHeader(ctx, "Disputed Items Page");
        ctx.sm.printUserTableWithHelp(ctx, "/DisputedItems");
        
        ctx.println("<a href='"+ctx.url()+"'>Return to SurveyTool</a><hr>");

        //doOrgDisputePage(ctx);

        ctx.addQuery(ctx.sm.QUERY_DO,"disputed");

        ctx.println("<h2>Disputed Items</h2>");
        
        ctx.println("<i>TODO</i>");

        //doDisputePage(ctx);
        
        ctx.sm.printFooter(ctx);
    } 

    public static void showGeneralInfoPage(WebContext ctx) {
         ctx.includeFragment("generalinfo.jsp");
    }
//
//    /**
//     * Compose mail to certain users with details about disputed items.
//     *  
//     * @param mailBucket the bucket of outbound mail
//     * @param intUsers map of locale -> users
//     * @param group which group this locale is in
//     * @param message special message 
//     * @param users ONLY send mail to these users
//     */
//    void doDisputeNag(Map mailBucket, Set intUsers, String group, Set locales, String message, String org) {
//    	//**NB: As this function was copied from doNag(), it will have some commented out parts from that function
//    	//**    for future features.
//    
//    	// First, are there any problems here?
//    	String complain = null;
//    
//    	if((intUsers==null) || intUsers.isEmpty()) {
//    		// if noone cares ...
//    		return;
//    	}
//    	boolean didPrint =false;
//    
//    	try {
//    		Connection conn = null;
//    		PreparedStatement listBadResults = null;
//    		try {
//    			conn = sm.dbUtils.getDBConnection();
//    			listBadResults= prepare_listBadResults(conn);
//    
//    			for(Iterator li=locales.iterator();li.hasNext();) {
//    				CLDRLocale loc = CLDRLocale.getInstance(li.next().toString());
//    
//    				int locStatus = status(loc);
//    				if((locStatus&(RES_DISPUTED|RES_ERROR))>0) {  // RES_BAD_MASK
//    					//                int numNoVotes = countResultsByType(loc,RES_NO_VOTES);
//    					//                int numInsufficient = countResultsByType(loc,RES_INSUFFICIENT);
//    					int numDisputed = countResultsByType(loc,RES_DISPUTED);
//    
//    					if(complain == null) {
//    						//                    System.err.println(" -nag: " + group);
//    						complain = "\n\n* Group '" + group + "' ("+new ULocale(group).getDisplayName()+")  needs attention:  DISPUTED VOTES: "+numDisputed+" \n";
//    					}
//    					//                System.err.println("  -nag: " + loc + " - " + typeToStr(locStatus));
//    					String problem = "";
//    					//                if((numNoVotes+numInsufficient)>0) {
//    					//                    problem = problem + " INSUFFICIENT VOTES: "+(numNoVotes+numInsufficient)+" ";
//    					//                }
//    					if(numDisputed>0) {
//    						problem = problem + " DISPUTED VOTES: "+numDisputed+"\n\n";
//    					}
//    
//    					// Get the actual XPaths
//    					//				Hashtable insItems = new Hashtable();
//    					Hashtable disItems = new Hashtable();
//    					try { // moderately expensive.. since we are tying up vet's connection..
//    		                listBadResults.setString(1,loc.toString());    		                
//    		                ResultSet rs = listBadResults.executeQuery();
//    						while(rs.next()) {
//    							int xp = rs.getInt(1);
//    							int type = rs.getInt(2);
//    
//    							String path = sm.xpt.getById(xp);
//    
//    							String theMenu = PathUtilities.xpathToMenu(path);
//    
//    							if(theMenu != null) {
//    								if(type == Vetting.RES_DISPUTED) {
//    									disItems.put(theMenu, "");
//    
//    								} /* else {
//    								insItems.put(theMenu, "");
//    							}*/ 
//    							}
//    						}
//    						rs.close();
//    					} catch (SQLException se) {
//    						throw new RuntimeException("SQL error on " + loc + " listing bad results - " + DBUtils.unchainSqlException(se));
//    					}
//    					//WebContext subCtx = new WebContext(ctx);
//    					//subCtx.addQuery("_",ctx.locale.toString());
//    					//subCtx.removeQuery("x");
//    
//    					if(numDisputed>0) {
//    						for(Iterator li2 = disItems.keySet().iterator();li2.hasNext();) {
//    							String item = (String)li2.next();
//    
//    							complain = complain + "http://www.unicode.org/cldr/apps/survey?_="+loc+"&amp;x="+item.replaceAll(" ","+")+"&only=disputed\n";
//    						}
//    					}
//    					//complain = complain + "\n "+ new ULocale(loc).getDisplayName() + " - " + problem + "\n    http://www.unicode.org/cldr/apps/survey?_="+loc;
//    				}
//    			}
//    			if(complain != null) {
//    				for(Iterator li = intUsers.iterator();li.hasNext();) {
//    					UserRegistry.User u = (UserRegistry.User)li.next();
//    					if((org != null) && (!u.org.equals(org))) {
//    						continue;
//    					}
//    					//				if(!users.contains(u)) continue; /* TODO: optimize as a single boolean op */
//    					Integer intid = new Integer(u.id);
//    					String body = (String)mailBucket.get(intid);
//    					if(body == null) {
//    						body = message + "\n\nYou will need to be logged-in before making changes at these URLs.\n\n";
//    					}
//    					body = body + complain + "\n";
//    					mailBucket.put(intid,body);
//    				}
//    			}
//    		} finally {
//    			DBUtils.close(listBadResults,conn);
//    		}
//    	} catch (SQLException se) {
//    		throw new RuntimeException("SQL error  listing bad results - " + DBUtils.unchainSqlException(se));
//    	}
//    }
//
//    /**
//         * Send out dispute nags for one organization.
//         * @param  message your special message
//         * @param org the organization
//         * @return number of mails sent.
//         */
//        int doDisputeNag(String message, String org) {
//    if(true == true)    throw new InternalError("removed from use.");
//            Map mailBucket = new HashMap(); // mail bucket: 
//        
//            Map intGroups = sm.getIntGroups();
//            Map intUsers = sm.getIntUsers(intGroups);
//            
//            System.err.println("--- nag ---");
//            
//            for(Iterator li = intGroups.keySet().iterator();li.hasNext();) {
//                String group = (String)li.next();
//                Set s = (Set)intGroups.get(group);            
//                doDisputeNag(mailBucket, (Set)intUsers.get(group), group, s, message, org);
//            }
//            
//            if(mailBucket.isEmpty()) {
//                System.err.println("--- nag: nothing to send.");
//    			return 0;
//            } else {
//                int n= sendBucket(mailBucket, "CLDR Dispute Report for " + ((org!=null)?org:" SurveyTool"));
//                System.err.println("--- nag: " + n + " emails sent.");
//    			return n;
//            }
//        }
//
//    /**
//     * Show the 'disputed' page.
//     * @param ctx webcontext for IN/OUT stuff
//     */
//    void doDisputePage(WebContext ctx) {
//    	Map<String, Map<String, Set<String>>> m = new  TreeMap<String, Map<String, Set<String>>> ();
//    	Set<String> badLocales = new TreeSet<String>(); 
//    	WebContext subCtx = new WebContext(ctx);
//    	subCtx.setQuery("do","");
//    	
//    	CLDRLocale onlyLoc = ctx.getLocale();
//    	
//        int skippedDueToCoverage = 0;
//    	int n = 0;
//    	int locs=0;
//    	Map<String,CoverageLevel2> covs = new HashMap<String, CoverageLevel2>();
//    	Map<String,String> covLvls = new HashMap<String, String>();
//    	try {
//    		Connection conn = null;
//    		Statement s = null;
//    		ResultSet rs=null;
//    		SupplementalDataInfo sdi = sm.getSupplementalDataInfo();
//    		try {
//    			conn = sm.dbUtils.getDBConnection();
//    			// select CLDR_RESULT.locale,CLDR_XPATHS.xpath from CLDR_RESULT,CLDR_XPATHS where CLDR_RESULT.type=4 AND CLDR_RESULT.base_xpath=CLDR_XPATHS.id order by CLDR_RESULT.locale
//    			s = conn.createStatement();
//    
//    			if(ctx.hasField("only_err")) {
//    				ctx.println("<h1>Only showing ERROR (disqualified winner) items</h1>");
//    				rs = s.executeQuery("select "+CLDR_RESULT+".locale,"+CLDR_RESULT+".base_xpath from "+CLDR_RESULT+" where ("+CLDR_RESULT+".type="+RES_ERROR+")");
//    			} else {
//    				rs = s.executeQuery("select "+CLDR_RESULT+".locale,"+CLDR_RESULT+".base_xpath from "+CLDR_RESULT+" where ("+CLDR_RESULT+".type>"+RES_INSUFFICIENT+") AND ("+CLDR_RESULT+".type<="+RES_BAD_MAX+")");
//    			}
//    			
//    			
//    			while(rs.next()) {
//    				String aLoc = rs.getString(1);
//    				
//    				if(onlyLoc!=null && !aLoc.equals(onlyLoc.toString())) {
//    					continue;
//    				}
//    				
//    				int aXpath = rs.getInt(2);
//    				String path = sm.xpt.getById(aXpath);
//    				
//    
//    				CoverageLevel2 cov = covs.get(aLoc);
//    				String covLvl;
//    				if(cov==null) {
//    					cov = CoverageLevel2.getInstance(sdi, aLoc);
//    					covs.put(aLoc, cov);
//    					covLvls.put(aLoc, covLvl = ctx.getEffectiveCoverageLevel(aLoc));
//    				} else {
//    					covLvl = covLvls.get(aLoc);
//    				}
//    
//    		        int workingCoverageValue = SupplementalDataInfo.CoverageLevelInfo.strToCoverageValue(covLvl);
//    	            int	coverageValue = cov.getIntLevel(path);
//    		        if ( coverageValue > workingCoverageValue ) {
//    		            if ( coverageValue <= 100 ) {
//    		                skippedDueToCoverage++;
//    		            } // else: would never be shown, don't care
//    		            continue;
//    		        }
//    
//    		        n++;
//    		        
//    				Map<String, Set<String>> ht = m.get(aLoc);
//    				if(ht==null) {
//    					locs++;
//    					ht = new TreeMap<String, Set<String>>();
//    					m.put(aLoc,ht);
//    					badLocales.add(sm.getLocaleDisplayName(CLDRLocale.getInstance(aLoc)));
//    				} // add the locale before showing disputes
//    
//    				String theMenu = PathUtilities.xpathToMenu(path);
//    
//    				if(theMenu==null) {
//    					ctx.println("<div class='ferrbox'>Couldn't find menu for " + path + " ("+aLoc+":"+aXpath+")</div><br>");
//    					theMenu="unknown";
//    				}
//    				Set<String> st = ht.get(theMenu);
//    				if(st==null) {
//    					st = new TreeSet<String>();
//    					ht.put(theMenu,st);
//    				}
//    				st.add(path);
//    			}
//    		} finally {
//    			DBUtils.close(rs,s,conn);
//    		}
//    	} catch ( SQLException se ) {
//    		String complaint = "Vetter:  couldn't do DisputePage - " + DBUtils.unchainSqlException(se);
//    		SurveyLog.logger.severe(complaint);
//    		se.printStackTrace();
//    		throw new RuntimeException(complaint);
//    	}
//    	boolean showAllXpaths = ctx.prefBool(sm.PREF_GROTTY);
//    
//    	
//    	if(onlyLoc!=null) {
//    		if(badLocales.isEmpty())
//    		{
//    			return;
//    		}
//    		ctx.println("<h3>Disputed Sections in This Locale</h3>");
//    
//    		Map<String,Set<String>> ht = m.get(onlyLoc.toString());
//    		
//    		int jj=0;
//    		for(Map.Entry<String, Set<String>> ii : ht.entrySet()) {
//    			if((jj++)>0) {
//    				ctx.print(", ");
//    			}
//    			String theMenu = ii.getKey();
//    			Set<String> subSet = ii.getValue();
//    			ctx.print("<a href='"+ctx.base()+"?"+
//    					"_="+onlyLoc+"&amp;x="+theMenu+ 
//    					"&amp;p_sort=interest"+
//    					/* "&amp;only=disputed"+ */  // disputed only is broken.
//    					"#"+DataSection.CHANGES_DISPUTED+"'>"+
//    					theMenu.replaceAll(" ","\\&nbsp;")+"</a>&nbsp;("+ subSet.size()+")");
//    
//    			if(showAllXpaths) {
//    				ctx.print("<br>");
//    				for(Iterator<String> iii = (subSet).iterator();iii.hasNext();) {
//    					String xp = (String)iii.next();
//    					ctx.println("&nbsp;<a "+ subCtx.atarget()+" href='"+SurveyForum.forumUrl(subCtx, onlyLoc.toString(), sm.xpt.getByXpath(xp)) + "'>"+  xp + "</a><br>");
//    				}
//    			}
//    		}
//    		
//    		
//    		if(skippedDueToCoverage>0) {
//    			ctx.println("<i>Note: " +skippedDueToCoverage +" disputed items in this locale not shown due to coverage level.</i>");
//    		}
//    		return;
//    	}
//    
//    
//    	if(skippedDueToCoverage>0) {
//    		ctx.println("<i>Note: " +skippedDueToCoverage +" items not shown due to coverage level.</i>");
//    	}
//    	ctx.println("<table class='list'>");
//    	ctx.println("<tr class='botbar'>"+
//    			//            "<th>Errs</th>" +
//    	"<th>#</th><th align='left' class='botgray'>Locale</th><th align='left'  class='botgray'>Disputed Sections</th></tr>");
//    	int nn=0;
//    	// todo: sort list..
//    	/*
//        if(lm == null) {
//            busted("Can't load CLDR data files from " + fileBase);
//            throw new RuntimeException("Can't load CLDR data files from " + fileBase);
//        }
//    
//        ctx.println("<table summary='Locale List' border=1 class='list'>");
//        int n=0;
//        for(Iterator li = lm.keySet().iterator();li.hasNext();) {
//            n++;
//    	 */
//    	LocaleTree lm = sm.getLocaleTree();
//    
//    	Iterator i;
//    
//    	//        i = lm.keySet().iterator();
//    	//i = m.keySet().iterator();
//    	i = badLocales.iterator();
//    	for(;i.hasNext();) {
//    		String locName = (String)i.next();
//    		String loc = sm.getLocaleCode(locName).toString();
//    		if(loc==null) loc = locName;
//    		Map<String, Set<String>> ht = m.get(loc);
//    		// calculate the # of total disputed items in this locale
//    		int totalbad = 0;
//    
//    		String groupName = new ULocale(loc).getLanguage();
//    
//    		int genCount = 0;
//    		//genCount = sm.externalErrorCount(loc);
//    
//    		if(ht != null) {
//    			for(Set<String> subSet : ht.values()) {
//    				totalbad += subSet.size();
//    			}
//    		}
//    
//    		if(totalbad==0 && genCount==0) {
//    			if(sm.isUnofficial) {
//    				ctx.println("<tr class='row"+(nn++ % 2)+"'>");
//    				ctx.print("<th align='left'>"+totalbad+"</th>");
//    				ctx.print("<th class='hang' align='left' title='Coverage: "+covLvls.get(loc)+"'>");
//    				sm.printLocaleLink(subCtx,CLDRLocale.getInstance(loc),new ULocale(loc).getDisplayName().replaceAll("\\(",
//    				"<br>(")); // subCtx = no 'do' portion, for now.
//    				ctx.println("</th>");
//    				ctx.println("</tr>");
//    			}
//    			continue;
//    		}
//    		
//    
//    		ctx.println("<tr class='row"+(nn++ % 2)+"'>");
//    		/*
//            if(genCount>0) {
//                ctx.print("<th align='left'><a href='"+sm.externalErrorUrl(groupName)+"'>"+genCount+"&nbsp;errs</a></th>");
//            } else {
//                ctx.print("<th></th>");
//            }
//    		 */
//    		ctx.print("<th align='left'  title='Coverage: "+covLvls.get(loc)+"'>"+totalbad+"</th>");
//    		ctx.print("<th class='hang' align='left'>");
//    		sm.printLocaleLink(subCtx,CLDRLocale.getInstance(loc),new ULocale(loc).getDisplayName().replaceAll("\\(",
//    		"<br>(")); // subCtx = no 'do' portion, for now.
//    		ctx.println("</th>");
//    		if(totalbad > 0) {
//    			ctx.println("<td>");
//    			int jj=0;
//    			for(Map.Entry<String, Set<String>> ii : ht.entrySet()) {
//    				if((jj++)>0) {
//    					ctx.print(", ");
//    				}
//    				String theMenu = ii.getKey();
//    				Set<String> subSet = ii.getValue();
//    				ctx.print("<a href='"+ctx.base()+"?"+
//    						"_="+loc+"&amp;x="+theMenu+ 
//    						"&amp;p_sort=interest"+
//    						/* "&amp;only=disputed"+ */  // disputed only is broken.
//    						"#"+DataSection.CHANGES_DISPUTED+"'>"+
//    						theMenu.replaceAll(" ","\\&nbsp;")+"</a>&nbsp;("+ subSet.size()+")");
//    
//    				if(showAllXpaths) {
//    					ctx.print("<br><pre>");
//    					for(Iterator<String> iii = (subSet).iterator();iii.hasNext();) {
//    						String xp = (String)iii.next();
//    						ctx.println(xp);
//    					}
//    					ctx.print("</pre>");
//    				}
//    			}
//    			ctx.print("</td>");
//    		}
//    		ctx.println("</tr>");
//    	}
//    	ctx.println("</table>");
//    	ctx.println("<hr>"+n+" disputed total in " + m.size() + " locales.<br>");			
//    }
//
//    /**
//     * Send out the Nag emails.
//     */
//    int doNag() {
//        //Map mailBucket = new HashMap(); // mail bucket: 
//    
//        Map<CLDRLocale, Set<CLDRLocale>> intGroups = sm.getIntGroups();
//        
//        System.err.println("--- nag ---");
//        int skipped=0;
//        int mailed = 0;
//        for(Iterator<CLDRLocale> li = intGroups.keySet().iterator();li.hasNext();) {
//        	CLDRLocale group = li.next();
//           /* if(sm.isUnofficial && !group.equals("tlh") && !group.equals("und")) {
//                skipped++;
//                continue;
//            }*/
//            Set<CLDRLocale> s = (Set<CLDRLocale>)intGroups.get(group);
//            mailed += doNag(group.getBaseName(), s);
//        }
//        if((skipped>0)||(mailed>0)) {
//            System.err.println("--- nag: skipped " + skipped +", mailed " + mailed);
//        }
//        return mailed;
//    }
//
//    /** 
//         * compose nags for one group
//         * @param intUsers interested users in this group
//         * @param group the interest group being processed
//         * @param s the list of locales contained in interest group 'group'
//         */
//        int doNag(String group, Set<CLDRLocale> s) {
//            // First, are there any problems here?
//            String complain = null;
//            int mailsent=0;
//            boolean didPrint =false;
//            
//            Set<String> cc_emails = new HashSet<String>();
//            Set<String> bcc_emails = new HashSet<String>();
//            
//            int emailCount = sm.fora.gatherInterestedUsers(group, cc_emails, bcc_emails);
//            
//            int genCountTotal=0;
//    
//            if(emailCount == 0) {
//                return 0; // no interested users.
//            }
//            
//            // for each locale in this interest group..
//            for(Iterator<CLDRLocale> li=s.iterator();li.hasNext();) {
//                CLDRLocale loc = li.next();
//                
//                int locStatus = status(loc);
//                int genCount = sm.externalErrorCount(loc);
//                genCountTotal += genCount;
//                if((genCount>0) || (locStatus>=0 && (locStatus&RES_BAD_MASK)>1)) {
//                    int numNoVotes = countResultsByType(loc,RES_NO_VOTES);
//                    int numInsufficient = countResultsByType(loc,RES_INSUFFICIENT);
//                    int numDisputed = countResultsByType(loc,RES_DISPUTED);
//                    int numErrored = countResultsByType(loc,RES_ERROR);
//                    
//                    boolean localeIsDefaultContent = (null!=sm.supplemental.defaultContentToParent(loc.toString()));
//                    
//                    if(localeIsDefaultContent) {
//                        //System.err.println(loc +" - default content, not sending notice. ");
//                        continue;
//                    }
//                    
//                    if(numDisputed==0 && sm.isUnofficial) {
//                        System.err.println("got one @ " + genCount + " -- " + loc);
//                    }
//                    
//                    if(complain == null) {
//                        complain = "";
//                    }
//    /*
//                    if(complain == null) {
//    //                    System.err.println(" -nag: " + group);
//                        complain = "\n\n* Group '" + group + "' ("+new ULocale(group).getDisplayName()+")  needs attention:  ";
//                    }
//    //                System.err.println("  -nag: " + loc + " - " + typeToStr(locStatus));
//    */
//    /*
//                    String problem = "";
//                    if((numNoVotes+numInsufficient)>0) {
//                        problem = problem + " INSUFFICIENT VOTES: "+(numNoVotes+numInsufficient)+" ";
//                    }
//                    if(numDisputed>0) {
//                        problem = problem + " DISPUTED VOTES: "+numDisputed+"";
//                    }
//                    if(numErrored>0) {
//                        problem = problem + " ERROR ITEMS: "+numErrored+"";
//                    }
//                    if(genCount>0) {
//                        problem = problem + " ERROR ITEMS: "+numErrored+"";
//                    }
//    */
//                    complain = complain +  loc.toULocale().getDisplayName() + "\n    http://www.unicode.org/cldr/apps/survey?_="+loc+"\n\n";
//                }
//            }
//            
//            if(complain != null) { // anything to send?
//                String from = sm.survprops.getProperty("CLDR_FROM","nobody@example.com");
//                String smtp = sm.survprops.getProperty("CLDR_SMTP",null);
//                
//                String disp = new ULocale(group).getDisplayName();
//                
//                String otherErr = "";
//                if(genCountTotal>0) {
//                    otherErr = "\nAlso, there are  " +genCountTotal+" other errors for these locales listed at:\n    "+
//                        sm.externalErrorUrl(group)+"\n\n";
//                }
//                
//                String subject = "CLDR Vetting update: "+group + " (" + disp + ")";
//                String body = "There are errors or disputes remaining in the locale data for "+disp+".\n"+
//                    "\n"+
//                    "Please go to http://www.unicode.org/cldr/vetting.html and follow the instructions to address the problems.\n"+
//                    "\n"+
//    //                "WARNING: there are some problems in computing the error and dispute counts, so please read that page even if you have read it before!\n" +
//                    "\n" + complain + "\n"+                otherErr+
//                    "Once you think that all the problems are addressed, forward this email message to surveytool@unicode.org, asking for your locale to be verified as done. We are working on a short time schedule, so we'd appreciate your resolving the issues as soon as possible. Remember that you will need to be logged-in before making changes.\n"+
//                    "\n\nThis is an automatic message, periodically generated to update vetters on the progress on their locales.\n\n";
//    
//                if(!bcc_emails.isEmpty()) {
//                    mailsent++;
//                    MailSender.sendBccMail(smtp, null, null, from, bcc_emails, subject, body);
//                }
//                if(!cc_emails.isEmpty()) {
//                    mailsent++;
//                    MailSender.sendToMail (smtp, null, null, from,  cc_emails, subject, body);
//                }
//                
//            }
//            return mailsent;
//        }
//
//    void doOrgDisputePage(WebContext ctx) {
//    	if(ctx.session.user == null ||
//    			ctx.session.user.org == null) {
//    		return;
//    	}
//    
//    	String loc = ctx.field("_");
//    	final String org = ctx.session.user.voterInfo().getOrganization().name();
//    	if(loc.equals("")) {
//    
//    		try {
//    			Connection conn = null;
//    			PreparedStatement orgDisputeLocs = null;
//    			try {
//    				conn = sm.dbUtils.getDBConnection();
//    				orgDisputeLocs = prepare_orgDisputeLocs(conn);
//    				orgDisputeLocs.setString(1,org);
//    
//    				ResultSet rs = orgDisputeLocs.executeQuery();
//    				if(rs.next()) {
//    					ctx.println("<h4>Vetting Disputes for "+ctx.session.user.org+" ("+org+")</h4>");
//    
//    					do {
//    						loc = rs.getString(1);
//    						int cnt = rs.getInt(2);
//    						CLDRLocale cloc = CLDRLocale.getInstance(loc);
//    						sm.printLocaleLink(ctx,cloc,cloc.getDisplayName(ctx.displayLocale));
//    						ctx.println(" "+cnt+" vetting disputes for " + org + "<br>");
//    					} while(rs.next());
//    				}
//    				rs.close();
//    			} finally {
//    				DBUtils.close(orgDisputeLocs,conn);
//    			}
//    		} catch ( SQLException se ) {
//    			String complaint = "Vetter:  couldn't  query orgdisputes " + DBUtils.unchainSqlException(se);
//    			SurveyLog.logger.severe(complaint);
//    			se.printStackTrace();
//    			throw new RuntimeException(complaint);
//    		}
//    	} else {
//    		// individual locale
//    	}
//    }
//
//    public boolean test(CLDRLocale locale, int xpath, int fxpath, String value) {
//        return test(locale, sm.xpt.getById(xpath), sm.xpt.getById(fxpath), value);
//    }
//
//    public boolean test(CLDRLocale locale, String xpath, String fxpath, String value) {
//        DataTester tester = get(locale);
//        return tester.test(xpath, fxpath, value);
//    }
//
//}    static int xpathMax=0;
//
///**
// * Write a single vote file
// * @param conn2 DB connection to use
// * @param source source of current locale
// * @param file CLDRFile to read from
// * @param ourDate canonical date for generation
// * @throws SQLException 
// */
//public boolean[] writeVoteFile(PrintWriter out, Connection conn2, XMLSource source, CLDRFile file, String ourDate, boolean xpathSet[]) throws SQLException {
//    boolean embeddedXpathTable = false;
//
//    if(xpathSet == null) {
//        //            xpathSet = new TreeSet<Integer>();
//        xpathSet = new boolean[xpathMax];
//        embeddedXpathTable=true;
//    }
//
//    String locale = source.getLocaleID();
////  XPathParts xpp = new XPathParts(null,null);
//    boolean isResolved = source.isResolving();
//    String oldVersion = SurveyMain.getOldVersion();
//    String newVersion = SurveyMain.getNewVersion();
//    out.println("<locale-votes date=\""+ourDate+"\" "+
//            "oldVersion=\""+oldVersion+"\" currentVersion=\""+newVersion+"\" "+
//            "locale=\""+locale+"\">");
//
//    ResultSet base_result=null,results=null;
//    PreparedStatement resultsByBase=null,votesByValue=null;
//    
//    try {
//    
//    resultsByBase = conn2.prepareStatement(
//            "select distinct cldr_vet.vote_xpath, cldr_data.value,cldr_vet.base_xpath  from cldr_data,cldr_vet,cldr_xpaths where cldr_data.locale=cldr_vet.locale " +
//            " and cldr_data.locale=? and " +
//    " cldr_data.xpath=cldr_vet.vote_xpath and cldr_vet.base_xpath=cldr_xpaths.id order by cldr_xpaths.xpath ");
//    votesByValue = conn2.prepareStatement("select submitter from cldr_vet where locale=? and vote_xpath=?");
//
//    resultsByBase.setString(1, locale);
//    votesByValue.setString(1, locale);
//
//    base_result = resultsByBase.executeQuery();
//
//
//    int n=0;
//
//    int lastBase=-1;
//
//    while(base_result.next()) {
//        n++;
//        int voteXpath = base_result.getInt(1);
//        String voteValue = DBUtils.getStringUTF8(base_result, 2);
//        int baseXpath = base_result.getInt(3);
//
////      out.println("<!-- base:"+baseXpath+", vote:"+voteXpath+", voteValue:"+voteValue+" -->\n");
//        
//        if(baseXpath!=lastBase) {
//            if(lastBase!=-1) {
//                out.println("  </item>");
//            }
//            out.println("  <item baseXpath=\""+sm.xpt.getStringIDString(baseXpath)+"\">");
//            lastBase=baseXpath;
//        }
//        boolean hadRow = false;
//
//        votesByValue.setInt(2, voteXpath);
//        results = votesByValue.executeQuery();
//        StringBuilder votes = new StringBuilder();
//        while(results.next()) {
//            int submitter = results.getInt(1);
//            if(votes.length()!=0) {
//                votes.append(' ');
//            }
//            votes.append(Integer.toString(submitter));
//        }
//        int outFull = baseXpath;
//        if(outFull>=xpathSet.length) {
//            if(xpathMax==0) {
//                xpathMax=sm.xpt.count()+IntHash.CHUNKSIZE;
//            }
//            int max = java.lang.Math.max(outFull, outFull);
//            xpathMax = java.lang.Math.max(max+IntHash.CHUNKSIZE, xpathMax);
//            boolean newXpathSet[] = new boolean[xpathMax];
//            System.arraycopy(xpathSet, 0, newXpathSet, 0, xpathSet.length);
//            xpathSet=newXpathSet;
//            System.err.println("VV:XPT:Expand to "+xpathMax);
//        }
////      out.println("<item baseXpath=\""+baseXpath+"\">"); // result=\""+resultXpath+"\">");
//        //xpathSet.add(baseXpath);
//        xpathSet[baseXpath]=true;
//        //out.println("\t\t<xpath type=\"base\" id=\""+baseXpath+"\">"+xmlescape(sm.xpt.getById(baseXpath))+"</xpath>");
//
//
//        out.println("   <vote users=\""+votes+"\">"+voteValue+"</vote>");
//    }
//    if(lastBase!=-1) {
//        out.println("  </item>");
//    } else {
//        return null;
//    }
//
////  if(embeddedXpathTable) {
////        out.println(" <xpathTable max=\""+xpathSet.length+"\">");
////        writeXpathFragment(out,xpathSet);
////        out.println(" </xpathTable>");
////    }
//    out.println("</locale-votes>");
//    return xpathSet;
//    } finally {
//        DBUtils.close(results,base_result,resultsByBase,votesByValue);
//    }
//}

    
    public static int getOrgDisputeCount(WebContext ctx) {
        // TODO Auto-generated method stub
        // ctx.session.user.voterOrg(),locale
        return 0;
    }
    /**
     * mailBucket:
     *   mail waiting to go out.
     * Hashmap:
     *    Integer(userid)   ->   String body-of-mail-to-send
     * 
     * This way, users only get one mail per service.
     * 
     * this function sends out mail waiting in the buckets.
     * 
     * @param vetting TODO
     * @param mailBucket map of mail going out already. (IN)
     * @param title the title of this mail
     * @return number of mails sent.
     */
//    int sendBucket(Vetting vetting, Map mailBucket, String title) {
//        int n =0;
//        String from = survprops.getProperty("CLDR_FROM","nobody@example.com");
//        String smtp = survprops.getProperty("CLDR_SMTP",null);
//    //        System.err.println("FS: " + from + " | " + smtp);
//        boolean noMail = (smtp==null);
//    
//    ///*srl*/       noMail = true;
//        
//        for(Iterator li = mailBucket.keySet().iterator();li.hasNext();) {
//            Integer user = (Integer)li.next();
//            String s = (String)mailBucket.get(user);            
//            User u = reg.getInfo(user.intValue());
//            
//            if(!UserRegistry.userIsTC(u)) {
//                s = "Note: If you have questions about this email,  instead of replying here,\n " +
//                    "please contact your CLDR-TC representiative for your organization ("+u.org+").\n"+
//                    "You can find the TC users listed near the top if you click '[List "+u.org+" Users] in the SurveyTool,\n" +
//                    "Or, at http://www.unicode.org/cldr/apps/survey?do=list\n"+
//                    "If you are unable to contact them, then you may reply to this email. Thank you.\n\n\n"+s;
//            }
//            
//            
//            if(!noMail) {
//                MailSender.sendMail(smtp,null,null,from,u.email, title, s);
//            } else {
//                System.err.println("--------");
//                System.err.println("- To  : " + u.email);
//                System.err.println("- Subj: " + title);
//                System.err.println("");
//                System.err.println(s);
//            }
//            n++;
//            if((n%50==0)) {
//                System.err.println("Vetter.MailBucket: sent email " + n + "/"+mailBucket.size());
//            }
//        }
//        return n;
//    }

    public static boolean getOrgDisputeCount(String voterOrg,
            CLDRLocale locale, int xpathId) {
        // TODO Auto-generated method stub
        return false;
    }
}
