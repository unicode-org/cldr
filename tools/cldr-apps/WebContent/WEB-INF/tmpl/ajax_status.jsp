<%@page import="org.unicode.cldr.util.VettingViewer"%>
<%@ page import="org.unicode.cldr.web.*" %>
<%@ page import="org.unicode.cldr.util.*" %>
<!--  begin ajax_status.jsp -->
<link rel="stylesheet" href="<%= request.getContextPath() %>/dojoroot/dijit/themes/claro/claro.css" />
<%= VettingViewer.getHeaderStyles() %>

<script type='text/javascript'>dojoConfig = {parseOnLoad: true}</script>
<script type='text/javascript' src='<%= request.getContextPath()+"/dojoroot/dojo/dojo.js" %>'></script>
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
var surveyLocaleUrl='&<%= SurveyMain.QUERY_LOCALE %>=<%= surveyCurrentLocale %>';
var surveyCurrentLocale = '<%= surveyCurrentLocale %>';
var surveyCurrentLocaleName = '<%= surveyCurrentLocaleName %>';
var surveyCurrentSection  = '<%= surveyCurrentSection %>';
var surveyOfficial = <%= !SurveyMain.isUnofficial() %>;
var surveyCurrentSection  = '<%= surveyCurrentSection %>';
var surveyVersion = '<%=SurveyMain.getNewVersion() %>';
var BUG_URL_BASE = '<%= SurveyMain.BUG_URL_BASE %>';
var surveyCurrentLocaleStamp = 0;
<% }else{ %>
var surveyCurrentLocale = null;
var surveyCurrentLocaleName = null;
var surveyCurrentLocaleStamp = 0;
var surveyCurrentSection  = '';
var surveyLocaleUrl='';
<% } 

String sessid = request.getParameter("s");
if(sessid!=null) {
%>
var surveySessionId='<%= sessid %>';
<% } else { %>
var surveySessionId=null;
<% } %>
var warnIcon = "<%= WebContext.iconHtml(request,"warn","Test Warning") %>";
var stopIcon = "<%= WebContext.iconHtml(request,"stop","Test Error") %>";
var WHAT_GETROW = "<%= SurveyAjax.WHAT_GETROW %>";
var WHAT_SUBMIT = "<%= SurveyAjax.WHAT_SUBMIT %>";
var TARGET_DOCS = "<%= WebContext.TARGET_DOCS %>";
var BASELINE_LOCALE = "<%= SurveyMain.BASELINE_LOCALE %>";
var BASELINE_LANGUAGE_NAME = "<%= SurveyMain.BASELINE_LANGUAGE_NAME %>";
</script>
<script type='text/javascript' src='<%= request.getContextPath() %>/js/survey.js'></script>


<%= (!SurveyMain.isUnofficial()) ? (org.unicode.cldr.tool.ShowData.ANALYTICS) : "" %>
<!--  end ajax_status.jsp -->
