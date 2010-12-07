<%@ page contentType="text/html; charset=UTF-8" import="org.unicode.cldr.web.*" %>
<div id='smalllogin'>
	    <a id='login_button' class='notselected' href='#' onclick='loginclick();' onmouseover='loginclick();'>Login</a>
   	  <div id='small_login_show' style='display: none'>
		<label for="email">Email:</label><input id="email" name="email" />
		<label for="pw">Password:</label> <input id="pw" type="password"			name="pw" />
		<%-- to allow non-JS login simply add:
		
					<input type="submit" value="Login" />
		 --%>
		
		<script type="text/javascript">
            <!--
             document.write("<input type=\"submit\" value=\"Login\" />");
             var mydiv =  document.getElementById('login_button');
             mydiv.style.display="block";
             
             function loginclick() {
            	 document.getElementById("small_login_show").style.display="inline";
            	 document.getElementById("login_button").style.display="none";
             }
            // -->
            </script>
        </div>
        <noscript>
          <%= WebContext.iconHtml(request,"warn","No Javascript") %>JavaScript must be enabled to login to the Survey Tool.
        </noscript>
		
		<!--  detect javascript. Not a problem, just figure out if we can rely on it or no. -->
		<script type="text/javascript">
            <!--
             document.write("<input type='hidden' name='p_nojavascript' value='f'>");
            // -->
            </script>
		<noscript><input name='p_nojavascript' type='hidden'
			value='t'></noscript>
</div>
