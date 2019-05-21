<%@ page import="org.unicode.cldr.util.VettingViewer"%>
<%@ page import="org.unicode.cldr.web.*" %>
<%@ page import="org.unicode.cldr.util.*" %>
<!--  begin ajax_status.jsp -->
<link rel="stylesheet" href="//ajax.googleapis.com/ajax/libs/dojo/1.10.4/dijit/themes/claro/claro.css" />

<!-- Bootstrap core CSS -->
<% // TODO: when v.jsp includes ajax_status.js, avoid redundant links for bootstrap, surveytool.css, redesign.css  %>
<%@include file="/WEB-INF/jspf/bootstrap_css.jspf" %>

<!-- Custom styles for this template -->
<link href="<%= request.getContextPath() %>/css/redesign.css" rel="stylesheet">
<%= VettingViewer.getHeaderStyles() %>

<script type='text/javascript'>
dojoConfig = {
		parseOnLoad: true,
		};</script>
<% // TODO: encapsulate dojo version in one place, cf. dojoheader.jspf  %>
<script type='text/javascript' src='//ajax.googleapis.com/ajax/libs/dojo/1.10.4/dojo/dojo.js'></script>
<script type='text/javascript'>
require(["dojo/parser", "dijit/layout/ContentPane", "dijit/layout/BorderContainer"]);
</script>
<script type="text/javascript">
// just things that must be JSP generated
var surveyRunningStamp = '<%= SurveyMain.surveyRunningStamp.current() %>';
var contextPath = '<%= request.getContextPath() %>';
var surveyCurrentId = '';
var surveyCurrentPage = '';
var surveyCurrentSpecial = null; //  null for locale, else oldvotes, etc
<%
String surveyCurrentLocale = request.getParameter(SurveyMain.QUERY_LOCALE);
//locale can have either - or _
surveyCurrentLocale = (surveyCurrentLocale == null) ? null : surveyCurrentLocale.replace("-", "_");
String surveyCurrentLocaleName = "";
if(surveyCurrentLocale!=null) {
	CLDRLocale aloc = CLDRLocale.getInstance(surveyCurrentLocale);
	surveyCurrentLocaleName = aloc.getDisplayName();
}
String surveyCurrentSection = request.getParameter(SurveyMain.QUERY_SECTION);
if(surveyCurrentSection==null) surveyCurrentSection="";
String surveyCurrentForum = request.getParameter(SurveyForum.F_XPATH);
if(surveyCurrentLocale!=null&&surveyCurrentLocale.length()>0&&
    (surveyCurrentSection!=null||surveyCurrentForum!=null)) {
%>
var surveyCurrentLocale = '<%= surveyCurrentLocale %>';
var surveyCurrentLocaleName = '<%= surveyCurrentLocaleName %>';
var surveyCurrentSection  = '<%= surveyCurrentSection %>';
var surveyCurrentSection  = '<%= surveyCurrentSection %>';
<% }else{ %>
var surveyCurrentLocale = null;
var surveyCurrentLocaleName = null;
var surveyCurrentSection  = '';
<% } %>
var surveyBaselineLocale = '<%= SurveyMain.BASELINE_LOCALE.getBaseName() %>';
var surveyCurrentLocaleStamp = 0;
var surveyCurrentLocaleStampId = '';
var surveyVersion = '<%=SurveyMain.getNewVersion() %>';
var surveyOldVersion = '<%= SurveyMain.getOldVersion() %>';
var surveyLastVoteVersion = '<%= SurveyMain.getLastVoteVersion() %>';
var surveyOfficial = <%= !SurveyMain.isUnofficial() %>;
var surveyCurrev = '<%= SurveyMain.getCurrevStr() %>';
var BUG_URL_BASE = '<%= SurveyMain.BUG_URL_BASE %>';
var surveyCurrentPhase = '<%= SurveyMain.phase().getCPhase() %>';
var surveyCurrev = '<%= SurveyMain.getCurrevStr() %>';
var surveyBeta = <%= SurveyMain.isPhaseBeta() %>;
<%

String sessid = request.getParameter("s");
HttpSession hsession = request.getSession(false);
if(sessid == null) {
	if(hsession != null) {
		sessid = hsession.getId();
		%><!-- httpsession: <%= sessid %> --><%
	}
} else {
	%><!-- s=: <%= sessid %> , hsession: <%= ((hsession!=null)?hsession.getId():"null") %> --> <%
}
CookieSession mySession = null;
UserRegistry.User myUser = null;
if(sessid != null )  {
	mySession = CookieSession.retrieveWithoutTouch(sessid);
}
if(mySession == null) {
	sessid = null;
} else {
	sessid = mySession.id;
	myUser = mySession.user;
}
%><%
if(sessid!=null) {
%>
var surveySessionId='<%= sessid %>';
<% } else { %>
var surveySessionId=null;
<% } 
SurveyMain curSurveyMain = null;
curSurveyMain = SurveyMain.getInstance(request);
if(myUser!=null) {
%>
var surveyUser= '<%= myUser.toJSONString() %>';
var userEmail= '<%= myUser.email %>'; 
var userPWD= '<%= myUser.password %>'; 
var userID= '<%= myUser.id %>'; 
var organizationName = '<%= myUser.getOrganization().getDisplayName() %>'; 
var org = '<%= myUser.org %>';

var surveyUserPerms = {
        userExist: (surveyUser != null),
        userCanImportOldVotes: <%= myUser.canImportOldVotes() %>,
        userCanUseVettingSummary: <%= UserRegistry.userCanUseVettingSummary(myUser) %>,
        userIsTC: <%=UserRegistry.userIsTC(myUser) %>,
        userIsVetter: <%= !UserRegistry.userIsTC(myUser) && UserRegistry.userIsVetter(myUser)%>,
        userIsLocked: <%= !UserRegistry.userIsTC(myUser) && !UserRegistry.userIsVetter(myUser) && !UserRegistry.userIsLocked(myUser)%>,
        hasDataSource: <%= curSurveyMain.dbUtils.hasDataSource() %>,
};
var surveyUserURL = {
        myAccountSetting: "survey?do=listu",
        disableMyAccount: "lock.jsp?email='+userEmail"+userEmail,

        recentActivity: "myvotes.jsp?user="+userID+"&s="+surveySessionId,
        xmlUpload: "upload.jsp?a=/cldr-apps/survey&s="+surveySessionId,
        
        manageUser: "survey?do=list",

        flag: "tc-flagged.jsp?s="+surveySessionId,
        RSS: "survey/feed?email=" + userEmail + "&pw=" + userPWD+ "&&feed=rss_2.0",
                
        about: "about.jsp",
        browse: "browse.jsp"
};
<%
if(UserRegistry.userIsAdmin(myUser)) {
%>
	surveyUserURL.adminPanel = 'survey?dump=<%= SurveyMain.vap %>';
<%
}
%>

<% } else { 
	// User session not present. Set a few things so that we don't fail.
%>
var surveyUser=null;
var surveyUserURL = {};
var organizationName =null ; 
var org = null;
var surveyUserPerms = {
        userExist: false,
};
<% }%>

var surveyImgInfo = {
        flag: { 
            src: "flag.png",
            alt: "flag",
            title: "flag",
            border: 0,
        },
        RSS: {
            src: "/cldr-apps/feed.png",
            alt: "[feed]",
            title: "RSS 2.0",
            border: 0,
        }
};
var warnIcon = "<%= WebContext.iconHtml(request,"warn","Test Warning") %>";
var stopIcon = "<%= WebContext.iconHtml(request,"stop","Test Error") %>";
var WHAT_GETROW = "<%= SurveyAjax.WHAT_GETROW %>";
var WHAT_SUBMIT = "<%= SurveyAjax.WHAT_SUBMIT %>";
var TARGET_DOCS = "<%= WebContext.TARGET_DOCS %>";
var BASELINE_LOCALE = "<%= SurveyMain.BASELINE_LOCALE %>";
var BASELINE_LANGUAGE_NAME = "<%= SurveyMain.BASELINE_LANGUAGE_NAME %>";
</script>

<%--TODO Refactor to add this at the end of every page instead of top, will increase performance --%>
<%@include file="/WEB-INF/tmpl/js_include.jsp" %>

<!--  end ajax_status.jsp -->
