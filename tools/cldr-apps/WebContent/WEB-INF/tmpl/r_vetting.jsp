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
<div class='pager' id='LoadingMessage'><hr/><h3>Loading Vetting View</h3><br/>
<span id='LoadingBar'>
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

//if(subCtx.userId() == UserRegistry.NO_USER || (subCtx.session.user.userlevel>UserRegistry.TC)) {
//    out.println("<i>You must be logged in and a TC to use this function.</i>");
//} else 
{
    viewer.generateHtmlErrorTables(subCtx.getOut(), choiceSet, ctx.getLocale().getBaseName(), VoteResolver.Organization.fromString(ctx.session.user.voterOrg()), usersLevel);
}

    subCtx.flush();

%>
<style type="text/css">
.vve {}
.vvn {}
.vvl {}
.vvm {}
.vvu {}
.vvw {}
</style>
<script type="text/javascript">

document.getElementById('LoadingMessage').style.display = 'none';

function changeStyle(show) {
    for (m in document.styleSheets) {
        var theRules;
        if (document.styleSheets[m].cssRules) {
            theRules = document.styleSheets[m].cssRules;
        } else if (document.styleSheets[m].rules) {
            theRules = document.styleSheets[m].rules;
        }
        for (n in theRules) {
            var rule = theRules[n];
            var sel = rule.selectorText;
            if (sel != undefined && sel.match(/vv/))   {
                if (sel.match(show)) {
                    rule.style.display = 'table-row';
                } else {
                    rule.style.display = 'none';
                }
            }
        }
    }
}

function setStyles() {
    var regexString = "";
    for (i=0; i < document.checkboxes.elements.length; i++){
        var item = document.checkboxes.elements[i];
        if (item.checked) {
            if (regexString.length != 0) {
                regexString += "|";
            }
            regexString += item.name;
        }
    }
    var myregexp = new RegExp(regexString);
    changeStyle(myregexp);
}
</script>

<hr/>
Loaded Vetting view in <%= t %><br/>

