<%@ include file="stcontext.jspf" %><%-- setup 'ctx' --%>

<!-- 
xpath: <%= dataSection.xpathPrefix %>  (will be //ldml for misc ..)
zoomed in: <%= zoomedIn %>

 -->

<i>For details and help on any item, zoom in by clicking on the status icon: 
             <%=   ctx.iconHtml("okay",null) +", "+
                ctx.iconHtml("ques",null) + ", " +
                ctx.iconHtml("warn",null) + ", " +
                ctx.iconHtml("stop",null) + ", " +
                ctx.iconHtml("squo",null) + " "  %>
                    </i>
                    <br>
                    
                    To see other voters, hover over the <b>
                    <%= ctx.iconHtml("vote","Voting Mark") %></b> symbol. 
                    The item with the star, <b><%= ctx.iconHtml("star","Star Mark") %>
                    </b>  was the one released with CLDR <%= ctx.sm.getOldVersion() %>
                    . A green value indicates that it is tentatively confirmed.
                    <hr>
                    