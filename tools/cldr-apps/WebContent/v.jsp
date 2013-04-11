<%@page import="org.unicode.cldr.web.WebContext"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%--
If not booted, attempt to boot..
 --%>
<%
final String survURL = request.getContextPath() + "/survey";
SurveyMain sm = SurveyMain.getInstance(request);
if(SurveyMain.isBusted!=null) {
    %>
    <html>
    <head>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/surveytool.css" />
    <title>SurveyTool | Offline.</title>
    </head>
    <body>
    <p class='ferrorbox'>
    <a href="<%= request.getContextPath() + "/survey" %>">Survey Tool</a> is offline.
    </p>
    </body>
    </html>
    
    <%
    return;
} else if(sm==null || !SurveyMain.isSetup) {
    String url = request.getContextPath() + request.getServletPath(); // TODO add query
        %>
    <html>
    <head>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/surveytool.css" />
    <title>SurveyTool | Starting</title>
                  <script type="application/javascript">
                  window.setTimeout(function(){
                      window.location.reload(true);                
                        //document.location='<%= url %>' + document.location.search +  document.location.hash;
                  },10000);
                  </script>
    </head>
    <body>
        <div id='st_err'></div>
        <%@include file="/WEB-INF/tmpl/ajax_status.jsp" %>
        
        <div class='finfobox'>
        <p>        Attempting to start SurveyTool..</p>
        </div>
        
        <%
            // JavaScript based redirect
            %>
            <noscript>
            <h1>
                JavaScript is required.
            </h1></noscript>
              If you are not redirected in a few seconds, you can click: <a id='redir' href='<%= url %>'>here</a> to retry, or <a id='redir2' href='<%= survURL %>'>here</a>.
              <script type="application/javascript">
                            document.getElementById("redir").href = '<%= url %>' + document.location.search +  document.location.hash;

                               dojo.ready(function(){
                            	   window.setTimeout(function(){
                            		    document.write('<title>CLDR SurveyTool | Please Wait..');
                            		    document.write('    <link rel="stylesheet" href="<%= request.getContextPath() %>/surveytool.css" />');
                            		    document.write('<img src="<%= request.getContextPath() %>/STLogo.png" align=right>');
                            		    document.write('<h1>CLDR SurveyTool | Please Wait</h1><hr>');
                            		    document.write('<i class="loadingMsg">Loading…</i>');
                            		    dojo.xhrGet({url: '<%= survURL %>', load: function() {   window.location.reload(true); if(false)   window.setTimeout(function(){     window.location.search='?'+window.location.search.substr(1)+'&'; },5000);  }  }); 
                            	   }, 2000);
                           	   });

                            
                            </script>
            <%
            
            if(sm!=null) {
        %>
                <%= sm.startupThread.htmlStatus() %>
                <hr>
            </body></html>                                
        <%
            }
        return;
}else
%>
<%--
    Validate the session. If we don't have a session, go back to SurveyMain.
 --%>
 <%
 WebContext ctx = new WebContext(request,response);
 String status = ctx.setSession();
if(false) { // if we need to redirect for some reason..
	 ctx.addAllParametersAsQuery();
 	 String url = request.getContextPath() + "/survey?" + ctx.query().replaceAll("&amp;", "\\&") + "&fromv=true";
	 // JavaScript based redirect
	 %>
	 <head>
    	 <title>SurveyTool: Redirect</title>
	       <script type="application/javascript">
	        document.location='<%= url %>' + document.location.hash;
	       </script>
     </head>
	 <body>
	   If you are not redirected, please click: <a href='<%= url %>'>here</a>, or <a id='redir2' href='<%= survURL %>'>here</a> (may lose your place).
	 <%
 }
%>
<html class='claro'>
<head class='claro'>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>CLDR  <%= ctx.sm.getNewVersion() %> SurveyTool | view</title>

<meta name='robots' content='noindex,nofollow'>
<meta name="gigabot" content="noindex">
<meta name="gigabot" content="noarchive">
<meta name="gigabot" content="nofollow">
<link rel="stylesheet" href="<%= request.getContextPath() %>/surveytool.css" />
<%@include file="/WEB-INF/tmpl/ajax_status.jsp" %>
<script type="text/javascript">
// set from incoming session
surveySessionId = '<%= ctx.session.id %>';
survURL = '<%= survURL %>';
  showV();
</script>
</head>
<body class='claro'>

<div data-dojo-type="dijit/Dialog" data-dojo-id="ariDialog" title="Disconnected from Survey Tool"
    execute="" data-dojo-props="onHide: function(){ariReload.style.display='';ariRetry.style.display='none';   if(disconnected) { unbust();}}">

    <div id='ariContent' class="dijitDialogPaneContentArea">
    	<div id='ariHelp'><a href='http://cldr.unicode.org/index/survey-tool#disconnected'>Help</a></div>
        <p id='ariMessage'>
            This page is still loading. 
        </p>
        <h3 id='ariSub'>Details:</h3>
        <p id='ariScroller'>
        </p>
    </div>

    <div class="dijitDialogPaneActionBar">
    <%--
        <button data-dojo-type="dijit/form/Button" type="submit" onClick="return ariDialog.isValid();">
            Report Bug…
        </button>
        --%>
        <button id='ariMain' style='margin-right: 2em;' data-dojo-type="dijit/form/Button" type="button" onClick="window.location = survURL;">
            Back to Locales
        </button>
        <button id='ariRetryBtn'  data-dojo-type="dijit/form/Button" type="button" onClick="ariRetry()">
            <b>Try Again</b>
        </button>
    </div>
</div>

<%--
<h1>ARITester</h1>
<p>When pressing this button the dialog will popup:</p>
<button id="buttonThree" data-dojo-type="dijit/form/Button" type="button" onClick="ariDialog.show();">
    Show me!
</button>
--%>

 <div data-dojo-type="dijit/layout/BorderContainer" data-dojo-props="design:'headline', gutters:true, liveSplitters:true" id="borderContainer">
    <div id="topstuff" data-dojo-type="dijit/layout/ContentPane" data-dojo-props="splitter:true, region:'top'" >
    <% if(status!=null) { %>
        <div class="v-status"><%= status %></div>
        <% } %>

        <!-- top info -->
         <%@include file="/WEB-INF/tmpl/stnotices.jspf" %>


        <%-- abbreviated form of usermenu  --%>
        <div id='toptitle'>
        <div id='title-cldr-container' class='menu-container' >
        <a href='<%= survURL %>'>
         <span class='title-cldr'>CLDR <%= ctx.sm.getNewVersion() %> Survey Tool
        <%=  (ctx.sm.phase()!=SurveyMain.Phase.SUBMIT)?ctx.sm.phase().toString():"" %>
         </span>
         </a>
         </div>
      
         <div id='title-locale-container' class='menu-container'>
                <span id='title-locale'></span>
                <span id='title-dcontent-container'><span id='title-dcontent'></span> <a href='http://cldr.unicode.org/translation/default-content' id='title-dcontent-link'>TITLE_DCONTENT_LINK</a></span>
	   <%--      <div id='title-locale' data-dojo-type="dijit/form/DropDownButton">
	              <span>(locale)</span>
	              <div id='menu-locale' data-dojo-type="dijit/DropDownMenu">
	                        <div data-dojo-type="dijit/MenuItem"
	                            data-dojo-props=" onClick:function(){  window.location='<%= survURL %>?_=' + surveyCurrentLocale;    }">
	                            General Info</div>
	                        
	              </div>
	         </div>  --%>
         </div>
         
         <div id='title-section-container' class='menu-container'>
	         <div id='title-section' data-dojo-type="dijit/form/DropDownButton">
	              <span>(section)</span>
	              <div id='menu-section' data-dojo-type="dijit/DropDownMenu"></div>
	         </div>
         </div>

        <div id='title-page-container' class='menu-container'>
	         <div id='title-page' data-dojo-type="dijit/form/DropDownButton">
	              <span>(page)</span>
	              <div id='menu-page' data-dojo-type="dijit/DropDownMenu"></div>
	         </div>
         </div>
         
         <div id='title-item-container' class='menu-container'>
             <span title="id" class='titlePart'  id='title-item'></span>
         </div>
         
        </div> <%-- end of toptitle --%>         
        <div id="lowerstuff">
            <% if(ctx.session !=null && ctx.session.user != null) {
                  boolean haveCookies = (ctx.getCookie(SurveyMain.QUERY_EMAIL)!=null&&ctx.getCookie(SurveyMain.QUERY_PASSWORD)!=null);
                  String cookieMessage = haveCookies?"<!-- and Forget Me-->":"";
              %>
              <span  class='userinfo'>
               <span class='user_email'>&lt;<%= ctx.session.user.email %>&gt;</span>
               <span class='user_name'><%= ctx.session.user.name %></span>
               <span class='user_org'>(<%= ctx.session.user.org %>)</span>
              </span>
              
               <a class='notselected' href='<%= ctx.base() + "?do=logout" %>'>Logout<%= cookieMessage %></a>
               |
            <% } else { %>
                <a href='<%= request.getContextPath() %>/login.jsp' id='loginlink' class='notselected'>Login…</a> |
            <% } %>
            <a class='notselected' href='<%= survURL  %>?do=options'>Manage</a> 
            |
            <a id='generalHelpLink' class='notselected'  href='<%= SurveyMain.GENERAL_HELP_URL %>'><%= SurveyMain.GENERAL_HELP_NAME %></a>
					|

					<div data-dojo-type="dijit/form/DropDownButton" id="reportMenu">
					<span>Review</span>
					<div data-dojo-type="dijit/DropDownMenu">
					         <% for (SurveyMain.ReportMenu m : SurveyMain.ReportMenu.values()) { %>
						<div data-dojo-type="dijit/MenuItem"
							data-dojo-props=" onClick:function(){
                                                window.location='<%= survURL + "?" + m.urlQuery() %>&_=' + surveyCurrentLocale;
                                        }"><%= m.display() %></div>
          <% } %>
                    </div>
                   </div>

            <label id='title-coverage'>
                Coverage:
            </label>
        </div> <%-- end of lowerstuff --%>
    </div> <%-- end of topstuff --%>
    <div data-dojo-type="dijit/layout/ContentPane" id="MainContentPane" data-dojo-props="splitter:true, region:'center'" >
        <div id="LoadingMessageSection"><%-- Loading messages --%>Please Wait</div>
        <div  id="DynamicDataSection" ><%-- the actual scrolling table --%></div>
        <div id="OtherSection"><%-- other content --%></div>
    </div>
    <div id="itemInfo" data-dojo-type="dijit/layout/ContentPane" data-dojo-props="splitter:true, region:'trailing'" ></div>
</div>
</body>
</html>