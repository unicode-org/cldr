/**
 * 
 */
package org.unicode.cldr.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Map;
import java.util.TreeMap;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.VettingViewer;
import org.unicode.cldr.util.VettingViewer.UsersChoice;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.util.VoteResolver.Organization;
import org.unicode.cldr.web.CLDRProgressIndicator.CLDRProgressTask;

import com.ibm.icu.dev.test.util.ElapsedTimer;

/**
 * @author srl
 *
 */
public class VettingViewerQueue {
	
	static VettingViewerQueue instance = new VettingViewerQueue();
	
	/**
	 * Get the singleton instance of the queue
	 * @return
	 */
	public static VettingViewerQueue getInstance() { 
		return instance;
	}
	
    static int gMax = -1;

    /**
     * Count the # of paths in this CLDRFile
     * @param file
     * @return
     */
    private static int pathCount(CLDRFile f)
    {
        int jj=0;
        for(@SuppressWarnings("unused") String s: f) {
            jj++;
        }
        return jj;
    }

    /**
     * Get the max expected items in a CLDRFile
     * @param f
     * @return
     */
    private synchronized int getMax(CLDRFile f) {
        if(gMax==-1) {
            gMax = pathCount(f);
        }
        return gMax;
    }

	/**
	 * A unique key for hashes
	 */
	private static final String KEY = VettingViewerQueue.class.getName();
    
	
	/**
	 * Status of a Task
	 * @author srl
	 *
	 */
    public enum Status {
    	/** Waiting on other users/tasks */
    	WAITING, 
    	/** Processing in progress */
    	PROCESSING, 
    	/** Contents are available */
    	READY, 
    	/** Stopped, due to some err */
    	STOPPED 
    };
    
    /**
     * What policy should be used when querying the queue?
     * @author srl
     *
     */
    public enum LoadingPolicy {
    	/** (Default) - start if not started */
    	START,
    	/** Don't start if not started. Just check. */
    	NOSTART,
    	/** Force a restart, ignoring old contents */
    	FORCERESTART,
    };
    
    private static class QueueEntry {
    	public Task currentTask=null;
    	public Map<CLDRLocale,StringBuffer> output = new TreeMap<CLDRLocale,StringBuffer>();
    }
    
	public class Task extends SurveyThread.SurveyTask {
		public CLDRLocale locale;
		private QueueEntry entry;
		SurveyMain sm;
		VettingViewer<VoteResolver.Organization> vv;
		public int maxn;
		public int n=0;
		public long start =-1;
		public long last;
		public long rem = -1;
		final Level usersLevel;
		final Organization usersOrg;
		String status = "(Waiting for other users)";
		public Status statusCode = Status.STOPPED;
		void setStatus(String status) {
			this.status = status;
		}
		public float progress() {
			if(maxn<=0) return (float)0.0;
			return ((float)n)/((float)maxn);
		}
		StringBuffer aBuffer = new StringBuffer();
		Task(QueueEntry entry, CLDRLocale locale, WebContext ctx) {
			super("VettingTask:"+locale.toString());
			this.locale = locale;
			this.entry = entry;
			this.sm = ctx.sm;
			this.vv = getVettingViewer(ctx);
			maxn = getMax(ctx.sm.getBaselineFile());
			usersLevel = Level.get(ctx.getEffectiveCoverageLevel());
			usersOrg = VoteResolver.Organization.fromString(ctx.session.user.voterOrg());
		}

		@Override
		public void run() throws Throwable {
			statusCode = Status.WAITING;
			final CLDRProgressTask progress = openProgress("vv:"+locale,maxn+100);
						
			try {
				status="Waiting...";
				progress.update("Waiting...");
				synchronized(vv) {
					if(!running()) { 
						status="Stopped on request.";
						statusCode=Status.STOPPED;  
						return;
					}
					status="Beginning Process, Calculating";
					progress.update("Got VettingViewer");
					statusCode = Status.PROCESSING;
					start = System.currentTimeMillis();
					last=start;
					n=0;
					vv.setProgressCallback(new VettingViewer.ProgressCallback(){
						public String setRemStr(long now) {
							double per = (double)(now-start)/(double)n;
							rem = (long)((maxn-n)*per);
							String remStr = ElapsedTimer.elapsedTime(now,now+rem) + " " + /*"("+rem+"/"+per+") "+*/"remaining";
							setStatus(remStr);
							return remStr;
						}
						public void nudge() { 
							if(!running()) {
								throw new RuntimeException("Not Running- stop now.");
							}
							long now = System.currentTimeMillis();
							n++;
							//        System.err.println("Nudged: " + n);
							if(n>(maxn-5)) maxn=n+10;


							if((now-last)>1200) {
								last=now;
								//								StringBuffer bar = SurveyProgressManager.appendProgressBar(new StringBuffer(),n,ourmax);
								//								String remStr="";
								if(n>500) {
									progress.update(n,setRemStr(now));
								} else {
									progress.update(n);
								}
								//								try {
								//									mout.println("<script type=\"text/javascript\">document.getElementById('LoadingBar').innerHTML=\""+bar+ " ("+n+" items loaded"  + remStr + ")" + "\";</script>");
								//									mout.flush();
								//								} catch (java.io.IOException e) {
								//									System.err.println("Nudge: got IOException  " + e.toString() + " after " + n);
								//									throw new RuntimeException(e); // stop processing
								//								}
							}
						}
						public void done() { progress.update("Done!"); }
					});
					
					EnumSet <VettingViewer.Choice> choiceSet = EnumSet.allOf(VettingViewer.Choice.class);

					vv.generateHtmlErrorTables(aBuffer, choiceSet, locale.getBaseName(), usersOrg, usersLevel);
					if(running()) {
						aBuffer.append("<hr/>"+PRE+"Processing time: "+ElapsedTimer.elapsedTime(start)+POST );
						entry.output.put(locale, aBuffer);
					}
				}
			} catch (RuntimeException re) {
				// We're done.
			} finally {
				if(progress!=null) progress.close();
			}
		}

		public String status() {
			StringBuffer bar = SurveyProgressManager.appendProgressBar(new StringBuffer(),n,maxn);
			return status + bar;
		}

	}
	
	private static final String PRE = "<DIV class='pager'>";
	private static final String POST = "</DIV>";
	/**
	 * Return the status of the vetting viewer output request. 
	 * If a different locale is requested, the previous request will be cancelled.
	 * @param ctx
	 * @param locale
	 * @return
	 */
	public synchronized String getVettingViewerOutput(WebContext ctx, CookieSession sess, CLDRLocale locale, Status[] status, LoadingPolicy forceRestart) {
		if(sess==null) sess = ctx.session;
		QueueEntry entry = getEntry(sess);
		if(status==null) status = new Status[1];
		if(forceRestart!=LoadingPolicy.FORCERESTART) {
			StringBuffer res = entry.output.get(locale);
			if(res != null) {
				status[0]=Status.READY;
				return res.toString();
			}
		} else { /* force restart */
			stop(ctx, locale, entry);
		}
		
		Task t = entry.currentTask;
		CLDRLocale didKill = null;
		
		if(t != null) { 
			String waiting = waitingString(t.sm.startupThread);
			if (t.locale.equals(locale)) {
				status[0]=Status.PROCESSING;
				if(t.running()) {
					// get progress from current thread
					status[0]=t.statusCode;
					if(status[0]!=Status.WAITING) waiting="";
					return PRE+"In Progress: " + waiting+ t.status()+POST;
				} else {
					return PRE+"Stopped (refresh if stuck) " + t.status()+POST;
				}
			} else if(forceRestart==LoadingPolicy.NOSTART){
				status[0]=Status.STOPPED;
				if(t.running()) {
					return PRE+" You have another locale being loaded: " + t.locale+POST;
				} else {
					return PRE+"Refresh if stuck."+POST;
				}
			} else {
				if(t.running()) {
					didKill = t.locale;
				}
				stop(ctx, t.locale, entry);
			}
		}
		
		if(forceRestart==LoadingPolicy.NOSTART){
			status[0]=Status.STOPPED;
			return PRE+"Not loading. Click the Refresh button to load."+POST;
		}
		
		t = entry.currentTask = new Task(entry, locale, ctx);
		ctx.sm.startupThread.addTask(entry.currentTask);
		
		status[0] = Status.PROCESSING;
		String killMsg = "";
		if(didKill!=null) {
			killMsg = " (Note: Stopped loading: "+didKill.toULocale().getDisplayName(SurveyMain.BASELINE_LOCALE)+")";
		}
		return PRE+"Started new task: " +waitingString(t.sm.startupThread)+ t.status()+"<hr/>"+killMsg+POST;
	}
	
	
	
	
    private String waitingString(SurveyThread startupThread) {
		int aheadOfMe=(totalUsersWaiting(startupThread));
		String waiting = (aheadOfMe>0)?(""+aheadOfMe+" users waiting - "):"";
		return waiting;
	}

	private void stop(WebContext ctx, CLDRLocale locale, QueueEntry entry) {
    	Task t = entry.currentTask;
    	if(t != null) {
    		if(t.running()) {
    			t.stop();
    		}
    		entry.currentTask=null;
    	}
    	entry.output.remove(locale);
	}

	private QueueEntry getEntry(CookieSession session) {
    	QueueEntry entry = (QueueEntry)session.get(KEY);
    	if(entry==null) {
    		entry = new QueueEntry();
    		session.put(KEY, entry);
    	}
    	return entry;
	}


	private VettingViewer<VoteResolver.Organization> gVettingViewer = null;
    private synchronized VettingViewer<VoteResolver.Organization> getVettingViewer(WebContext ctx) {
        CLDRProgressTask p = null;
        if(gVettingViewer==null)  try {
            p = ctx.sm.openProgress("Setting up vettingViewer...");
            p.update("opening..");
            gVettingViewer = new VettingViewer<VoteResolver.Organization>(
                    ctx.sm.getSupplementalDataInfo(), ctx.sm.dbsrcfac, ctx.sm.getOldFactory(),
                    getUsersChoice(ctx.sm), "CLDR "+ctx.sm.getOldVersion(), "Winning "+ctx.sm.getNewVersion());
            gVettingViewer.setBaseUrl(ctx.base());
           // gVettingViewer.setErrorChecker(ctx.sm.dbsrcfac.getErrorChecker());
            p.update("OK");
        } finally {
            p.close();
        }
        return gVettingViewer;
    }
    private UsersChoice<VoteResolver.Organization> getUsersChoice(final SurveyMain sm) { 
        return new UsersChoice<VoteResolver.Organization>() {
            @Override
            public String getWinningValueForUsersOrganization(
                    CLDRFile cldrFile, String path, VoteResolver.Organization user) {
                CLDRLocale loc = CLDRLocale.getInstance(cldrFile.getLocaleID());
                int base_xpath = sm.xpt.xpathToBaseXpathId(path);
                Race r;
                Connection conn = null;
                try { 
                    conn = sm.dbUtils.getDBConnection();
                    r = sm.vet.getRace(loc, base_xpath, conn);
                    VoteResolver.Organization org = (Organization) user;
                    Race.Chad c =  r.getOrgVote(org);
                    if(c==null) {
                        //System.err.println("Error: organization " + org + " vote null for " + path + " [#"+base_xpath+"]");
                        return null;
                    }
                    return c.value;
                }catch (SQLException e) {
                    throw new RuntimeException(e);
                } finally {
                    DBUtils.closeDBConnection(conn);
                }

            }

        };
    }
    
    private static int totalUsersWaiting(SurveyThread st) {
    	return (st.tasksRemaining(Task.class));
    }
}
