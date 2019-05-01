<%@page import="com.ibm.icu.util.ULocale"%>
<%@page import="org.xml.sax.SAXParseException"%>
<%@page import="org.unicode.cldr.util.CLDRFile.DraftStatus"%>
<%@page import="org.unicode.cldr.util.*"%>
<%@page import="org.unicode.cldr.web.*"%>
<%@page import="org.unicode.cldr.util.CLDRFile"%>
<%@page import="org.unicode.cldr.util.SimpleXMLSource"%>
<%@page import="org.unicode.cldr.util.XMLSource"%>
<%@page import="java.io.*"%><%@page import="java.util.*,org.apache.commons.fileupload.*,org.apache.commons.fileupload.servlet.*,org.apache.commons.io.FileCleaningTracker,org.apache.commons.fileupload.util.*,org.apache.commons.fileupload.disk.*,java.io.File" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%!
	public static DiskFileItemFactory newDiskFileItemFactory(
			ServletContext context, File repository) {
		FileCleaningTracker fileCleaningTracker = org.apache.commons.fileupload.servlet.FileCleanerCleanup
				.getFileCleaningTracker(context);
		DiskFileItemFactory factory = new DiskFileItemFactory(
				/* DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD */ 1024000, repository);
		factory.setFileCleaningTracker(fileCleaningTracker);
		return factory;
	}

%>
<%
	File tmpDir = new File(System.getProperty("java.io.tmpdir"));

	DiskFileItemFactory factory = newDiskFileItemFactory(pageContext.getServletContext(),tmpDir);
	ServletFileUpload upload = new ServletFileUpload(factory);

	boolean isBad = !ServletFileUpload.isMultipartContent(request);

	if (!request.getMethod().equals("POST") || isBad) {
		response.sendRedirect(request.getContextPath() + "/upload.jsp");
	}
	
	CLDRFile cf = null;
	InputStream fis = null;
	List<FileItem> list = upload.parseRequest(request);
	Map<String,String> fields = new HashMap<String,String>();
	for(FileItem item : list) {
		if(item.isFormField()) {
	fields.put(item.getFieldName(),item.getString());
		} else {
	if(fis!=null) throw new InternalError("Error, already got one");
	fis = item.getInputStream();
		}
	}
	
	boolean isSubmit = fields.containsKey("submit");
	boolean isBulk = fields.containsKey("bulk");
        String email = fields.get("email");
	String sid = fields.get("s");
	CookieSession cs = CookieSession.retrieve(sid);
	if(cs==null || cs.user==null) {
		response.sendRedirect(request.getContextPath()+"/survey");
		return;
	}
	cs.userDidAction(); // mark user as not idle
	if(email==null||email.trim().isEmpty()) {
		response.sendRedirect(request.getContextPath()+"/upload.jsp?s="+sid);
		return;
	}
	UserRegistry.User theirU = CookieSession.sm.reg.get(email.trim());
	if (theirU == null
			|| (!theirU.equals(cs.user) && !cs.user.isAdminFor(theirU))) {
				// if no/bad file was given, kick them back o the upload form
		response.sendRedirect(request.getContextPath()
				+ "/upload.jsp?s=" + sid + "&email=" + email.trim()
				+ "&emailbad=t");
		return;
	}
	if (fis==null || fis.available()==0) {
				// if no/bad file was given, kick them back o the upload form
		response.sendRedirect(request.getContextPath()
				+ "/upload.jsp?s=" + sid + "&email=" + email.trim()
				+ "&filebad=t");
		return;
	}
	String title = isSubmit ? "Submitted As You"
			: "Submitted As Your Org";
%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>SurveyTool File Submission | <%=title%></title>
		<link rel='stylesheet' type='text/css' href='./surveytool.css' />
</head>
<body>

<a href="upload.jsp?s=<%= sid %>&email=<%= email %>">Re-Upload File/Try Another</a> | <a href="<%=request.getContextPath()%>/survey">Return to the SurveyTool <img src='STLogo.png' style='float:right;' /></a>
<hr/>
<h3>SurveyTool File Check | <%=title%>  | Submitted as: <%= theirU.name %> </h3>

	<div class='helpHtml'>
		Your file is being tested.
		<br>
		For help, see: <a target='CLDR-ST-DOCS' href='http://cldr.unicode.org/index/survey-tool/upload'>Using Bulk Upload</a>.<br/>
		Verify that there are no errors, then click the NEXT button.
	</div>

<% request.setAttribute("BULK_STAGE", "check"); %>
<%@include file="/WEB-INF/jspf/bulkinfo.jspf" %>

<div style='padding: 1em;'>
<%
try {
	cf = SimpleFactory.makeFile(SurveyMain.fileBase+"/upload.xml",fis,DraftStatus.unconfirmed);
} catch(Throwable t) {
	SurveyLog.logException(t, "while "+email+"uploading bulk file ");
	out.println("<h3>Failed to parse.</h3>");
	while(t!=null) {
		%>
		<div class='adminThreadStack'><%= StackTracker.stackToString(t.getStackTrace(), 1) %></div>
		<pre class='adminExceptionMESSAGE'><%= t %></pre>	
		<%
		t = t.getCause();
		if(t!=null) {
			out.println("<i>caused by</i><br/>");
		}
	}
	out.println("<hr>Please fix the problems before proceeding.<br/>");
	out.flush();
	return;
}

CLDRLocale loc = CLDRLocale.getInstance(cf.getLocaleID());
cs.stuff.remove("SubmitLocale");
%>
</div>

<h3>Locale</h3>
 <tt class='codebox'><%= loc + "</tt> <br/>Name:  " + loc.getDisplayName(SurveyMain.BASELINE_LOCALE)  %><br/>
<%
UserRegistry.ModifyDenial md = UserRegistry.userCanModifyLocaleWhy(theirU,loc);
if(!cs.sm.getLocalesSet().contains(loc)) {
	%><h1 class='ferrbox'>Error: Locale doesn't exist in the Survey Tool.</h1><%
} else if(cs.sm.getReadOnlyLocales().contains(loc)) {
	%><h1 class='ferrbox'>Error: <%= loc.getDisplayName() %> may not be modified: <%= SpecialLocales.getComment(loc) %></h1><%
} else if(md != null) {
	%><h1 class='ferrbox'>Error: <%= theirU.name %>  (<%= theirU.email %>) may not modify <%= loc.getDisplayName() %>: <%= md.getReason() %></h1><%
} else {
	cs.stuff.put("SubmitLocale",cf);
%>
<form action='<%= request.getContextPath()+"/submit.jsp" %>' method='POST'>
<input  type='hidden' name='s' value='<%= sid %>'/>
<input  type='hidden' name='email' value='<%= email %>'/>
<input type='submit' class='bulkNextButton' value='NEXT: Run CLDR tests'/>
</form>

<pre>
<%
	for(String x : cf) {
		out.println(x + " : " +  cf.getStringValue(x));
	}
%>
</pre>

<% }  %>



</body>
</html>