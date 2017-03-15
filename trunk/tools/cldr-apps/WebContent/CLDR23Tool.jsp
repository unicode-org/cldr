<%@page import="com.ibm.icu.util.ULocale"%>
<%@page import="com.ibm.icu.text.AlphabeticIndex"%>
<%@page import="org.unicode.cldr.util.Level" %>
<%@page import="org.unicode.cldr.util.CoverageInfo" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"
        import="org.unicode.cldr.web.*,org.unicode.cldr.util.*, java.util.TreeSet,  com.ibm.icu.text.AlphabeticIndex"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>


<%!
        public boolean isValidSurveyToolVote(UserRegistry.User user, String xpath, PathHeader ph, CLDRLocale locale) {
            
            if(ph==null) return false;
            if(ph.getSurveyToolStatus()==PathHeader.SurveyToolStatus.DEPRECATED) {
            	return false;
            }
            if(ph.getSurveyToolStatus()==PathHeader.SurveyToolStatus.HIDE  || 
                    ph.getSurveyToolStatus()==PathHeader.SurveyToolStatus.READ_ONLY) {
                if(user==null || !UserRegistry.userIsTC(user)) return false;
            }
          	CoverageInfo cInfo=CLDRConfig.getInstance().getCoverageInfo();
            if(cInfo.getCoverageValue(xpath, locale.getBaseName())>
            	Level.COMPREHENSIVE.getLevel()) {
            		return false;
            }
            return true;
        }
%>

<%
    String p = request.getParameter("l");
CLDRLocale l = null;
    if(p!=null)  {
        l = CLDRLocale.getInstance(p);
    }
    String title = "CLDR23Tool";
    if(l!=null) {
    	title = title +  " | " + l.getDisplayName() + " [ " + l.getBaseName() + "]"; 
    }
    
    /**
     * @deprecated
     */
    SurveyMain sm = CookieSession.sm;

    CLDRFile rootRes = sm.getBaselineFile();


    XPathMatcher m = DataSection.getHackyExcludeMatcher();
    
    TreeSet<String> allBadPaths = new TreeSet<String>();
%>

<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title><%= title %></title>
</head>
<body>

<%
   if(sm==null) {
	    %><i>SurveyTool not started yet, please come back</i>
	    <%
   }
%>

<% if(l==null) { %>
<!--  no locale -->

<i>Choose a locale to analyze</i>

<hr/>

<div style='display: table'>
<%

// Create a simple index where the values for the strings are Integers, and add the strings
   TreeSet<CLDRLocale> ts = new TreeSet<CLDRLocale>(SurveyMain.getLocalesSet());


   AlphabeticIndex<CLDRLocale> index = new AlphabeticIndex<CLDRLocale>(ULocale.getDefault());

for (CLDRLocale a: ts) {
    index.addRecord(a.getDisplayName(), a); 
}


// Show the buckets with their contents, skipping empty buckets

for (AlphabeticIndex.Bucket<CLDRLocale> bucket : index) {
    if (bucket.size() != 0) {
        %><p style='float: right;'><%
        for (AlphabeticIndex.Bucket<CLDRLocale> bucket2 : index) {
            if (false || bucket2.size() != 0) {
                %><a href='#<%= bucket2.getLabel() %>'><%= bucket2.getLabel() %></a> <%
            }
        }
        %><h3 id='<%= bucket.getLabel() %>'><%= bucket.getLabel() %></h3><%
        for (AlphabeticIndex.Record<CLDRLocale> item : bucket) {
           CLDRLocale a = item.getData();

			%></p>


           <a href='<%= request.getContextPath() + request.getServletPath() + "?l="+a.getBaseName() %>'>
               <%= a.getDisplayName() + " [" + a +  "]" %>
           </a>
           
           <%
               CompareCLDRFile ccf = new CompareCLDRFile();
    
			    CompareCLDRFile.Entry trunk = ccf.add(sm.getDiskFactory().make(a.getBaseName(), false),    "trunk-for-"+sm.getNewVersion());
			    CompareCLDRFile.Entry release = ccf.add(sm.getOldFactory().make(a.getBaseName(), false),   "release-"+ sm.getOldVersion());
			    CompareCLDRFile.Entry vetting = ccf.add(sm.getSTFactory().make(a.getBaseName(), false),    "vetting-"+sm.getNewVersion());
		    %>           
                           <%
                   TreeSet<String> vMinusT= new TreeSet<String>(vetting.getXPaths());
                   vMinusT.removeAll(trunk.getXPaths());
                   allBadPaths.addAll(vMinusT);
                   
                   %>
           
           <% if(!vMinusT.isEmpty()) { %>
           
           
           <br/>
			   <table border=1>
			       <tr>
			           <th>
			               Source
			           </th>
			           <th>
			               Total XPaths
			           </th>
			           <th>
			               Unique XPaths
			           </th>
			        </tr>
			        
			        
			        <%
			        
			        for(CompareCLDRFile.Entry e : ccf.getResults()) {
			            %><tr>
			                      <th><%= e %></th>
			                      
			                      <td><%= e.getXPaths().size() %></td>
			                      <td><%= e.getUniqueXPaths().size() %></td>
			             </tr><%
			        }
			        %>
			        
			           <tr>
			               <th><b>TOTAL</b></th>
			               <td><%= ccf.getAllXPaths().size() %></td>
			               <td>-</td>
			           </tr>
			   </table>


    <% } %>
				   
				   <%
				   
				   if(false && !vMinusT.isEmpty()) {
				   %>
               <h3>XPaths in <%= vetting %> but not in <%= trunk %></h3>
                    <% }  %>				
				   <%
				     for(String x : vMinusT) {
				         String style = "";
				         
				         if(m.matches(x, -1)) {
				             %>(skip)<%
				             style = "background-color: gray; color: white;";
				             
				             if(rootRes.isPathExcludedForSurvey(x)) {
				                 %>(pathExcludedForSurvey)<%
				             } else {
				                 %><b>(pathNOTEXCLUDEDFORSURVEY)</b><%
				             }
				         }
				         
				         
				         %><tt style='<%= style %>'><%= x %></tt><br/><%
				                 
				     }
				   %>
				          
           
           &nbsp; 
       <%
        }
    }
}
%>

</div>

<h3>ALL bads..</h3>
                   <%
                     for(String x : allBadPaths) {
                         String style = "";
                         
                         if(m.matches(x, -1)) {
                             %>(skip)<%
                             style = "background-color: gray; color: white;";
                             
                             if(rootRes.isPathExcludedForSurvey(x)) {
                                 %>(pathExcludedForSurvey)<%
                             } else {
                                 %><b>(pathNOTEXCLUDEDFORSURVEY)</b><%
                             }
                         }
                         
                         
                         %><tt style='<%= style %>'><%= x %></tt><br/><%
                                 
                     }
                   %>



<% } else { %>
<a href="<%= request.getContextPath() + request.getServletPath() %>">[back]</a>
<h2><%= l.getDisplayName() %>  [<%= l.getBaseName() %>]</h2>
<!--  locale -->

    <%
    
    CompareCLDRFile ccf = new CompareCLDRFile();
    
    CompareCLDRFile.Entry trunk = ccf.add(sm.getDiskFactory().make(l.getBaseName(), false),    "trunk-for-"+sm.getNewVersion());
    CompareCLDRFile.Entry release = ccf.add(sm.getOldFactory().make(l.getBaseName(), false),   "release-"+ sm.getOldVersion());
    CompareCLDRFile.Entry vetting = ccf.add(sm.getSTFactory().make(l.getBaseName(), false),    "vetting-"+sm.getNewVersion());
    
    %>
    <br/>
    
    
    <table border=1>
        <tr>
            <th>
                Source
            </th>
            <th>
                Total XPaths
            </th>
            <th>
                Unique XPaths
            </th>
         </tr>
         
         
         <%
         
         for(CompareCLDRFile.Entry e : ccf.getResults()) {
        	 %><tr>
        	           <th><%= e %></th>
        	           
                       <td><%= e.getXPaths().size() %></td>
                       <td><%= e.getUniqueXPaths().size() %></td>
        	  </tr><%
         }
         %>
         
            <tr>
                <th><b>TOTAL</b></th>
                <td><%= ccf.getAllXPaths().size() %></td>
                <td>-</td>
            </tr>
    </table>
    
    <hr>
    
    <h3>XPaths in <%= vetting %> but not in <%= trunk %></h3>
    
    <%
    TreeSet<String> vMinusT= new TreeSet<String>(vetting.getXPaths());
    vMinusT.removeAll(trunk.getXPaths());
    %>

    <%
      for(String x : vMinusT) {
    	  String style = "";
    	  
    	  if(m.matches(x, -1)) {
    		  %>(skip)<%
    		  style = "background-color: gray; color: white;";
    		  
    		  if(rootRes.isPathExcludedForSurvey(x)) {
    			  %>(pathExcludedForSurvey)<%
    		  } else {
    			  %><b>(pathNOTEXCLUDEDFORSURVEY)</b><%
    			  
    			  
    			  PathHeader ph = sm.getSTFactory().getPathHeader(x);
    			  
    			  %> ph=<%= ph %> <%
    		  }
          }
    	  
    	  
    	  %><tt style='<%= style %>'><%= x %></tt><br/><%
    			  
      }
    %>

<hr>
<a href="<%= request.getContextPath() + request.getServletPath() %>">[back]</a>

<% } %>


</body>
</html>