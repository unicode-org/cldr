<%@ include file="/WEB-INF/jspf/report_top.jspf" %>

<%@ page import="com.ibm.icu.util.Currency" %>
<%@ page import="com.ibm.icu.util.ULocale" %>


<h2>New items for 1.8, or cases where the English has changed</h2>
<%

//  Copy "x=___"  from input to output URL

%><p>The first set are new territories. All of these should be translated.</p><%


subCtx.openTable(); 

             // (OPTIONAL array notation)  
subCtx.showXpath(new String[] 
                 {"//ldml/localeDisplayNames/territories/territory[@type=\"AC\"]",
				  "//ldml/localeDisplayNames/territories/territory[@type=\"CP\"]",
				  "//ldml/localeDisplayNames/territories/territory[@type=\"DG\"]",
				  "//ldml/localeDisplayNames/territories/territory[@type=\"EA\"]",
				  "//ldml/localeDisplayNames/territories/territory[@type=\"IC\"]",
				  "//ldml/localeDisplayNames/territories/territory[@type=\"TA\"]"});

subCtx.closeTable();


             
%><p>The following have alternative values or newly modified English. All of these should be translated.</p><%

out.flush();

subCtx.openTable(); 

subCtx.showXpath("//ldml/localeDisplayNames/territories/territory[@type=\"MM\"]");
subCtx.showXpath("//ldml/localeDisplayNames/territories/territory[@type=\"PS\"]");

subCtx.showXpath("//ldml/localeDisplayNames/territories/territory[@type=\"CD\"]");
subCtx.showXpath("//ldml/localeDisplayNames/territories/territory[@type=\"CD\"][@alt=\"variant\"]");
subCtx.showXpath("//ldml/localeDisplayNames/territories/territory[@type=\"CG\"]");
subCtx.showXpath("//ldml/localeDisplayNames/territories/territory[@type=\"CG\"][@alt=\"variant\"]");
subCtx.showXpath("//ldml/localeDisplayNames/territories/territory[@type=\"CI\"]");
subCtx.showXpath("//ldml/localeDisplayNames/territories/territory[@type=\"CI\"][@alt=\"variant\"]");
subCtx.showXpath("//ldml/localeDisplayNames/territories/territory[@type=\"FK\"]");
subCtx.showXpath("//ldml/localeDisplayNames/territories/territory[@type=\"FK\"][@alt=\"variant\"]");
subCtx.showXpath("//ldml/localeDisplayNames/territories/territory[@type=\"HK\"][@alt=\"short\"]");
subCtx.showXpath("//ldml/localeDisplayNames/territories/territory[@type=\"HK\"]");
subCtx.showXpath("//ldml/localeDisplayNames/territories/territory[@type=\"MK\"]");
subCtx.showXpath("//ldml/localeDisplayNames/territories/territory[@type=\"MK\"][@alt=\"variant\"]");
subCtx.showXpath("//ldml/localeDisplayNames/territories/territory[@type=\"MO\"][@alt=\"short\"]");
subCtx.showXpath("//ldml/localeDisplayNames/territories/territory[@type=\"MO\"]");
subCtx.showXpath("//ldml/localeDisplayNames/territories/territory[@type=\"TL\"]");
subCtx.showXpath("//ldml/localeDisplayNames/territories/territory[@type=\"TL\"][@alt=\"variant\"]");

subCtx.closeTable();
%><p>The following are new cases of language or language variants. Translating these is optional but recommended.</p><%

subCtx.openTable(); 

subCtx.showXpath("//ldml/localeDisplayNames/languages/language[@type=\"yue\"]");
subCtx.showXpath("//ldml/localeDisplayNames/languages/language[@type=\"swb\"]");
            
subCtx.showXpath("//ldml/localeDisplayNames/variants/variant[@type=\"PINYIN\"]");
subCtx.showXpath("//ldml/localeDisplayNames/variants/variant[@type=\"WADEGILE\"]");

subCtx.closeTable();
subCtx.doneWithXpaths(); // print hidden field notifying which bases to accept submission for. 

%>
-->