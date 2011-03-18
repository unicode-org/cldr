<%@ include file="/WEB-INF/jspf/stcontext.jspf" %><%-- setup 'ctx' --%>
<%@ include file="/WEB-INF/jspf/report.jspf"  %>
<%@ page import="org.unicode.cldr.util.*" %>

<%
/* set the '_' parameter, to the current locale */
subCtx.setQuery(SurveyMain.QUERY_LOCALE,ctx.localeString());
/* flush output (sync between subCtx' stream and JSP stream ) */
subCtx.flush();
%>
<%@ include file="/WEB-INF/jspf/debug_jsp.jspf" %>
<%!
    static int gMax = -1;

private int pathCount(CLDRFile f)
{
    int jj=0;
    for(String s : f) {
        jj++;
    }
    return jj;
}

private synchronized int getMax(CLDRFile f) {
    if(gMax==-1) {
        gMax = pathCount(f);
    }
    return gMax;
}

%>

<%--
    <%@ include file="/WEB-INF/jspf/report_top.jspf" %>
--%>
<div id='LoadingMessage'><hr/><h3>Loading Vetting View</h3>...<br/>
<span id='LoadingBar'>
    <%= SurveyProgressManager.appendProgressBar(new StringBuffer(),0,100) %>
</span></div>
<%
subCtx.flush();
out.flush();
final int maxn = getMax(ctx.sm.getBaselineFile());
    // set up the 'x' parameter to the current secrtion (r_steps, etc)
subCtx.setQuery(SurveyMain.QUERY_SECTION,subCtx.field(SurveyMain.QUERY_SECTION));
ctx.setQuery(SurveyMain.QUERY_LOCALE,subCtx.field(SurveyMain.QUERY_LOCALE));

/**
 * Set query fields to be propagated to the individual steps 
 */
WebContext topCtx = (WebContext) request.getAttribute(WebContext.CLDR_WEBCONTEXT);
topCtx.setQuery(SurveyMain.QUERY_SECTION, subCtx.field(SurveyMain.QUERY_SECTION));
topCtx.setQuery(SurveyMain.QUERY_LOCALE, subCtx.field(SurveyMain.QUERY_LOCALE));


VettingViewer<VoteResolver.Organization> viewer = subCtx.sm.getVettingViewer(topCtx);
EnumSet <VettingViewer.Choice> choiceSet = EnumSet.allOf(VettingViewer.Choice.class);
Level usersLevel = Level.get(ctx.getEffectiveCoverageLevel());
final com.ibm.icu.dev.test.util.ElapsedTimer t = new com.ibm.icu.dev.test.util.ElapsedTimer();
final JspWriter mout = out;
viewer.setProgressCallback(new VettingViewer.ProgressCallback(){
    int n = 0;
    int ourmax = maxn;
    long start = System.currentTimeMillis();
    long last = start;
    public void nudge() { 
        long now = System.currentTimeMillis();
        n++;
//        System.err.println("Nudged: " + n);
        if(n>(ourmax-5)) ourmax=n+10;
         
        
        if((now-last)>1200) {
            last=now;
            StringBuffer bar = SurveyProgressManager.appendProgressBar(new StringBuffer(),n,ourmax);
            long rem = -1;
            String remStr="";
            if(n>500) {
                double per = (double)(now-start)/(double)n;
                rem = (long)((ourmax-n)*per);
                remStr = ", " + com.ibm.icu.dev.test.util.ElapsedTimer.elapsedTime(now,now+rem) + " " + /*"("+rem+"/"+per+") "+*/"remaining";
            }
            try {
                mout.println("<script type=\"text/javascript\">document.getElementById('LoadingBar').innerHTML=\""+bar+ " ("+n+" items loaded"  + remStr + ")" + "\";</script>");
                mout.flush();
            } catch (java.io.IOException e) {
                System.err.println("Nudge: got IOException  " + e.toString() + " after " + n);
                throw new RuntimeException(e); // stop processing
            }
        }
    }
    public void done() { }
 }
);
viewer.generateHtmlErrorTables(subCtx.getOut(), choiceSet, ctx.getLocale().getBaseName(), VoteResolver.Organization.fromString(ctx.session.user.voterOrg()), usersLevel);
    // now, enter the close of the form
    
    
    subCtx.flush();

%>
<script type="text/javascript">
document.getElementById('LoadingMessage').style.display = 'none';
</script>

<hr/>
Loaded Vetting view in <%= t %><br/>

