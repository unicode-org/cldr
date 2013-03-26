<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%><%@ include file="/WEB-INF/jspf/stcontext.jspf" %>
<!--  DynamicDataSection.jsp -->



<div data-dojo-type="dijit/layout/BorderContainer" data-dojo-props="design:'sidebar', gutters:true, liveSplitters:true" id="borderContainer">
    <div id="topstuff" data-dojo-type="dijit/layout/ContentPane" data-dojo-props="splitter:true, region:'top'" ></div>
    <div id="DynamicDataSection" data-dojo-type="dijit/layout/ContentPane" data-dojo-props="splitter:true, region:'center'" ></div>
    <div id="itemInfo" data-dojo-type="dijit/layout/ContentPane" data-dojo-props="splitter:true, region:'trailing'" ></div>
</div>


