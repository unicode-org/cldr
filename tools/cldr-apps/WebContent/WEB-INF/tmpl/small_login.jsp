<%@ page contentType="text/html; charset=UTF-8" import="org.unicode.cldr.web.*" %>
<div id='smalllogin' class='small_login_normal'>
   	  <div id='small_login_show' class='small_login_hidden'>
		<label for="email"> Email:</label><input id="email" name="email" /> <br/>
        <label for="pw"> Password:</label> <input id="pw" type="password"            name="pw" /> <br/>
        <label for="save_cook"> Log me in automatically next time?</label> <input id="<%= SurveyMain.QUERY_SAVE_COOKIE %>" type="checkbox"            name="save_cookie" /> <br/>
		<%-- to allow non-JS login simply add:
		
					<input type="submit" value="Login" />
		 --%>
		
		<script>
            <!--
             document.write("<button type=\"submit\"><b>Login</b></button>");
//             var mydiv =  document.getElementById('login_button');
  //           mydiv.style.display="block";
             
             function loginclick() {
//            	 document.getElementById("small_login_show").style.visibility='visible';
//            	 document.getElementById("small_login_show").style.display='inline';
//            	 document.getElementById("small_login_show").style.opacity='1';
				document.getElementById("small_login_show").className       	 ="small_login_hot";
            	 //document.getElementById("login_button").style.display ="none";
            	 document.getElementById("small_login_shower").className='small_login_hidden';
             }
             function exitclick() {
//            	 document.getElementById("small_login_show").style.visibility='visible';
//            	 document.getElementById("small_login_show").style.display='inline';
//            	 document.getElementById("small_login_show").style.opacity='1';
				document.getElementById("small_login_show").className       	 ="small_login_hidden";
            	 //document.getElementById("login_button").style.display ="none";
            	 document.getElementById("small_login_shower").className='small_login_normal';
             }
            // -->
            </script>
            <button style='float: left;' type='button' onclick='exitclick();'>Cancel</button>

        <% if(org.unicode.cldr.web.SurveyMain.isUnofficial()) {
        	final String CLDR_TESTPW = org.unicode.cldr.util.CLDRConfig.getInstance().getProperty("CLDR_TESTPW", "");
			if(CLDR_TESTPW !=null && CLDR_TESTPW.length()>0) { %>
			<a href='<%= request.getContextPath() %>/login.jsp'>Need to create a test login?</a>
		<% 	}
		   } %>

        </div>
        <div id='small_login_shower' class='small_login_normal'>
        	<button type='button'  onclick='loginclick();'>Login...</button>
        </div>
        <noscript>
          <%= WebContext.iconHtml(request,"warn","No Javascript") %>JavaScript must be enabled to login to the Survey Tool.
        </noscript>
		
		<!--  detect javascript. Not a problem, just figure out if we can rely on it or no. -->
		<script>
            <!--
             document.write("<input type='hidden' name='p_nojavascript' value='f'>");
            // -->
            </script>
		<noscript><input name='p_nojavascript' type='hidden'
			value='t'></noscript>
</div>
