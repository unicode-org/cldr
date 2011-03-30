<%@page import="com.ibm.icu.util.ULocale"%>
<%@page import="org.xml.sax.SAXParseException"%>
<%@page import="org.unicode.cldr.util.CLDRFile.DraftStatus"%>
<%@page import="org.unicode.cldr.util.*"%>
<%@page import="org.unicode.cldr.web.*"%>
<%@page import="org.unicode.cldr.util.CLDRFile.SimpleXMLSource"%>
<%@page import="org.unicode.cldr.util.CLDRFile"%>
<%@page import="org.unicode.cldr.util.CLDRFile.SimpleXMLSource"%>
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
/*
	final static String IDENTITY = "//ldml/identity/";
	public static CLDRLocale locFromFile(CLDRFile file) {
		Map<String,String> parts = new HashMap<String,String>();
		XPathParts xpp = new XPathParts(null,null);
		ULocale.Builder lb = new ULocale.Builder();
		for(String xpath:file) { // TODO: iterator
			if(!xpath.startsWith(IDENTITY)) {
				continue;
			}
			xpp.set(xpath);
			String k = xpp.getElement(-1);
			String v = xpp.getAttributeValue(-1,"type");
			//System.err.println(">>"+k+","+v);
			parts.put(k,v);
			if(k.equals("language")) {
				lb = lb.setLanguage(v);
			} else if(k.equals("script")) {
				lb = lb.setScript(v);
			} else if(k.equals("territory")) {
				lb = lb.setRegion(v);
			} else if(k.equals("variant")) {
				lb = lb.setVariant(v);
			}
		}
		return CLDRLocale.getInstance(lb.build());
	}
	*/

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
	String sid = fields.get("s");
	CookieSession cs = CookieSession.retrieve(sid);
	if(cs==null) {
		response.sendRedirect(request.getContextPath()+"/survey");
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

<a href="upload.jsp?s=<%= sid %>">Re-Upload File/Try Another</a> | <a href="<%=request.getContextPath()%>/survey">Return to the SurveyTool <img src='STLogo.png' style='float:right;' /></a>
<hr/>
<h3>SurveyTool File Check | <%=title%>  | <%= cs.user.name %> </h3>

<i>Checking upload...</i>

<%
try {
	cf = CLDRFile.make(SurveyMain.fileBase+"/upload.xml",fis,DraftStatus.unconfirmed);
} catch(Throwable t) {
	out.println("<h3>Failed to parse.</h3>");
	while(t!=null) {
		t.printStackTrace();
		out.println(" " + t.toString()+"<br/>");
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

<h3>Locale</h3>
 <tt class='codebox'><%= loc + "</tt> <br/>Name:  " + loc.getDisplayName(SurveyMain.BASELINE_LOCALE)  %><br/>
<%
if(!cs.sm.getLocalesSet().contains(loc)) {
	%><h1>Error: Locale doesn't exist in the Survey Tool.</h1><%
} else if(!UserRegistry.userCanModifyLocale(cs.user,loc)) {
	%><h1>Error: <%= cs.user.name %> not authorized to submit data for this locale</h1><%
} else {
	cs.stuff.put("SubmitLocale",cf);
%>
<form action='<%= request.getContextPath()+"/submit.jsp" %>' method='POST'>
<input  type='hidden' name='s' value='<%= sid %>'/>
<input type='submit' value='Submit <%= loc %>...'/>
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