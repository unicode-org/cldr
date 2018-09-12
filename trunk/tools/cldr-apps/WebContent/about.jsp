<%@page import="org.unicode.cldr.web.DBUtils"%>
<%@ page contentType="text/html; charset=UTF-8" %>
<!-- Copyright (C) 2012 IBM and Others. All Rights Reserved --> 
<html>
	<head>
		<title>Unicode | CLDR | About the Survey Tool</title>
		<link rel='stylesheet' type='text/css' href='./surveytool.css' />
        <script type='text/javascript' src='//ajax.googleapis.com/ajax/libs/dojo/1.10.4/dojo/dojo.js'
            djConfig='parseOnLoad: true, isDebug: false'></script>
	</head>
    
    <body>
				<img src="STLogo.png" align="right" border="0" title="[logo]" alt="[logo]" />
    <h2 class="selected">About the Survey Tool</h2>

        <p class="hang">For more information about the Survey Tool, see <a href="http://www.unicode.org/cldr">unicode.org/cldr</a>.</p>
        
    <h4 class="selected">Java Versions</h4>
        <table class="userlist" border="2">
            <%
                String props[] = {  "java.version", "java.vendor", "java.vm.version", "java.vm.vendor",
                    "java.vm.name", "os.name", "os.arch", "os.version" };
                for(int i=0;i<props.length;i++) {
                    out.println("<tr class='row"+(i%2)+"'><th><tt>"+props[i]+"</tt></th><td>"+java.lang.System.getProperty(props[i])+"</td></tr>\n");
                }
            %>
            
        </table>
        
    <h4 class="selected">Other Versions</h4>

    <% 
        { int i=0;
    %>
    
    <table class="userlist" border="2">    	
        <tr class="row<%= ((i++)%2) %>">
            <th>CLDRFile.GEN_VERSION</th>
            <td> <%= org.unicode.cldr.util.CLDRFile.GEN_VERSION %>  </td>
        </tr>
        <tr class="row<%= ((i++)%2) %>">
            <th>ICU</th>
            <td> <%= com.ibm.icu.util.VersionInfo.ICU_VERSION %>  </td>
        </tr>
        <tr class="row<%= ((i++)%2) %>">
            <th>Server</th>
            <td> <%= application.getServerInfo() %>  </td>
        </tr>
        <tr class="row<%= ((i++)%2) %>">
            <th>Java Servlet API</th>
            <td> <%= application.getMajorVersion() %>.<%= application.getMinorVersion() %>  </td>
        </tr>
        
    </table>

    <% 
        }
    %>
    
    
        <h4 class="selected">Survey Tool information</h4>
    <% 
        { int i=0;
    %>
    
    <table class="userlist" border="2">
    
        <tr class="row<%= ((i++)%2) %>">
            <th>SurveyMain.BASELINE_LOCALE</th>
            <td> <%= org.unicode.cldr.web.SurveyMain.BASELINE_LOCALE.toLanguageTag() %>  </td>
        </tr>
        <tr class="row<%= ((i++)%2) %>">
            <th>SurveyMain.BASELINE_LANGUAGE_NAME</th>
            <td> <%= org.unicode.cldr.web.SurveyMain.BASELINE_LANGUAGE_NAME %>  </td>
        </tr>
        <tr class="row<%= ((i++)%2) %>">
            <th>SVN Version</th>
            <td> <%= org.unicode.cldr.util.CldrUtility.getProperty("CLDR_CURREV", null) %>  </td>
        </tr>
    </table>    
    <%
        }
    %>

<% { org.unicode.cldr.web.DBUtils d = org.unicode.cldr.web.DBUtils.peekInstance();  if (d!=null) { %>

        <h4 class="selected">Database information</h4>
    <% 
        int i=0;
    %>
    
    <table class="userlist" border="2">
    
        <tr class="row<%= ((i++)%2) %>">
            <th>Have Datasource?</th>
            <td> <%= d.hasDataSource() %>  </td>
        </tr>
        <tr class="row<%= ((i++)%2) %>">
            <th>DB Kind / Info</th>
            <td>	<%= DBUtils.getDBKind() %>
            		 <br /> <%= d.getDBInfo() %>  </td>
        </tr>

    </table>    
    <%
        }
    }
    %>


		<hr/>
		<a href="./survey">Return to Survey Tool</a> |
		<a href="./index.jsp">Return to CLDR Applications</a> |
		<a target="_new" href="http://dev.icu-project.org/cgi-bin/cldrwiki.pl?SurveyToolHelp/About">Help on this page (in a new window)</a>
	</body>
</html>
