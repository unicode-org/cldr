<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" import="org.unicode.cldr.web.*"%>
<%
String email = request.getParameter("email");
if(email==null||email.isEmpty()) {
%>
<h1>"Unsubscribe" and disable your SurveyTool account</h1>
    To permanently lock your account:
    <ol>
        <li><a href="login.jsp">Login</a> to your account - you may request a password reset if needed.</li>
        <li>Select "Permanently disable my account" from the <a href="survey?do=options">Manage</a> page.
    </ol>
    <hr>
    <a href="http://cldr.unicode.org">Unicode CLDR Homepage</a>
<%
return;
}
email = email.trim().toLowerCase();
if(email.contains("admin@")) {
    response.sendRedirect(request.getContextPath()+"/survey#err_badreq");
    return;
}

Integer sumAnswerLock = (Integer)session.getAttribute("sumAnswerLock");

String userAnswer = request.getParameter("sumAnswerLock");

int hashA = (int)(Math.random()*11.0);
int hashB = (int)(Math.random()*11.0);
int hashC = hashA+hashB;

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link rel='stylesheet' type='text/css' href='./surveytool.css' />

<title>SurveyTool | Permanently Disable Account) | <%= email %></title>
</head>
<body>
<img src='STLogo.png' align='right' />
<h3>SurveyTool | Lock (Permanently Disable Account) | <%= email %></h3>

<div style='background: url(stop.png); padding: 16px'>
<h1  style='background-color: white; margin: 0; padding: 0;'> You are requesting to permanently disable (lock) your SurveyTool account. All of your votes will be ignored. Are you absolutely sure you wish to do this?</h1>
<a href='http://cldr.unicode.org/index/survey-tool' class='notselected' style='background-color: white; padding: 0.25em; font-size: x-large;'>Help</a>
</div>

<p>

<%
int userAnswerLock=-1;
try {
	userAnswerLock=Integer.parseInt(userAnswer);
} catch(Throwable t) {}

String reason = request.getParameter("reason");

// did they get it right?
if(userAnswer!=null&&(sumAnswerLock==userAnswerLock) && (reason!=null&&!reason.trim().isEmpty())) {
%>
  <b>Attempting to lock account:</b> <%= CookieSession.sm.reg.lockAccount(email, reason, WebContext.userIP(request)) %>

<hr>

If information was entered correctly, your account should be locked. Thank you for using the SurveyTool. If you have difficulty still, contact the person who set up your account.

<%
WebContext.logout(request,response);
} else {
	// put it in the hash
	session.setAttribute("sumAnswerLock",new Integer(hashC));
	
	if(userAnswer!=null) {
%>
	<i class='ferrorbox'>Sorry, that answer was wrong.</i><br/>
<%  } %>

	<div class='graybox'>
		To <b>permanently disable</b> your account, please solve this simple math problem:  What is the sum of 
			<%= hashA %>
				+
			<%= hashB %>
				?
				
		<% if(SurveyMain.isUnofficial() ) { %><i>Hint (for smoketest):  <%= hashC %></i> <% } %> <br/>
	
		<form method='POST' action='<%= request.getContextPath()+request.getServletPath() %>'>
            <input name='sumAnswerLock' size=10 value='' />
            
            <hr>
			<label>You must retype your email address exactly in this box: <input name='email'></label><br><hr>
			<label><b>Required:</b> please enter the reason for the account lock request: <br> <textarea name='reason'></textarea></label>
<%
    if(reason!=null && reason.trim().isEmpty()) {
        %>
        <i class='ferrbox'>The reason for the request must be filled in.</i><br/>
    <%  
    }%>
			<br>
			<input type='submit' value='Permanently lock my account'/>
		</form>	
		
	</div>
<%
}
%>
<hr>
<a href='./survey'>Return to the Survey Tool</a>

</body>
</html>