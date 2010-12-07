<%@ page import="org.unicode.cldr.web.*" %>
<script type='text/javascript' src='<%= request.getContextPath()+"/dojoroot/dojo/dojo.js" %>'
    djConfig='parseOnLoad: true, isDebug: false'></script>
<script type="text/javascript">
var timerID = -1;

function updateIf(id, txt) {
    var something = document.getElementById(id);
    if(something != null) {
        something.innerHTML = txt;
    }
}

var wasBusted = false;
var wasOk = false;
var loadOnOk = null;
var clickContinue = null;

function updateStatus() {
    dojo.xhrGet({
        url:"<%= request.getContextPath() %>/SurveyAjax?what=status",
        handleAs:"json",
        load: function(json){
            if(json.isBusted == 1) {
                wasBusted = true;
            }
            var st_err =  document.getElementById('st_err');
            if(json.err.length > 0) {
               st_err.innerHTML=json.err;
               st_err.className = "ferrbox";
               wasBusted = true;
            } else {
                if(wasBusted == true && (json.isBusted == 0) && (json.isSetup == 1)) {
                    st_err.innerHTML="Note: Lost connection with Survey Tool or it restarted.";
                    if(clickContinue != null) {
                        st_err.innerHTML = st_err.innerHTML + " Please <a href='"+clickContinue+"'>click here</a> to continue.";
                    } else {
                    	st_err.innerHTML = st_err.innerHTML + " Please reload this page to continue.";
                    }
                    st_err.className = "ferrbox";
                } else {
                   st_err.className = "";
                   st_err.innerHTML="";
                }
            }
            updateIf('progress',json.progress);
            updateIf('uptime',json.uptime);
            updateIf('visitors',json.visitors);
            if((wasBusted == false) && (json.isSetup == 1) && (loadOnOk != null)) {
                window.location.replace(loadOnOk);
            }
        },
        error: function(err, ioArgs){
            var st_err =  document.getElementById('st_err');
            wasBusted = true;
            st_err.className = "ferrbox";
            st_err.innerHTML="Disconnected from Survey Tool: "+err.name + " <br> " + err.message;
            updateIf('progress','<hr><i>(disconnected from Survey Tool)</i></hr>');
            updateIf('uptime','down');
            updateIf('visitors','nobody');
        }
    });
}

// for validating CLDR data values
// do_change(hash, value, xpid, locale)
// divs:    e_HASH = div under teh item
//           v_HASH - the entire td
function do_change(fieldhash, value, xpid, locale, session) {
	var e_div = document.getElementById('e_'+fieldhash);
	var v_td = document.getElementById('i_'+fieldhash);
	var v_tr = document.getElementById('r_'+fieldhash);
	
	e_div.innerHTML = '<i>Checking...</i>';
	e_div.className="";
	v_tr.className="tr_checking";
    var st_err =  document.getElementById('st_err');

    dojo.xhrPost({
        url:"<%= request.getContextPath() %>/SurveyAjax?what=verify&xpath="+xpid +"&_="+locale+"&fhash="+fieldhash+"&s="+session,
        postData: value,
        handleAs:"json",
        load: function(json){
//            if(json.isBusted == 1) {
//                wasBusted = true;
//            }
			var newHtml = "";
			if(json.err.length >0) {
				v_tr.className="tr_err";
				newHtml = "<%= WebContext.iconHtml(request,"stop","Test Error") %> Could not check value. Try reloading the page.<br>"+json.err;
			} else if(json.testResults.length == 0) {
				e_div.className="";
				v_tr.className="tr_submit";
				newHtml = "<i>Ready to Submit</i>";
			} else {
				e_div.className="";
				v_td.className="v_warn";
				v_tr.className="tr_warn";
				newHtml = "";
				for(var i=0;i<json.testResults.length;i++) {
					var tr = json.testResults[i];
					newHtml += "<p class='tr_"+tr.type+"' title='"+tr.type+"'>";
					if(tr.type == 'Warning') {
						newHtml += "<%= WebContext.iconHtml(request,"warn","Test Warning") %>";
					} else if(tr.type == 'Error') {
						v_tr.className="tr_err";
						newHtml += "<%= WebContext.iconHtml(request,"stop","Test Error") %>";
					}
					newHtml += json.testResults[i].message;
					newHtml += "</p>";
				}
			}
			e_div.innerHTML = newHtml;
        },
        error: function(err, ioArgs){
            var st_err =  document.getElementById('st_err');
            wasBusted = true;
            st_err.className = "ferrbox";
            st_err.innerHTML="Disconnected from Survey Tool while processing a field: "+err.name + " <br> " + err.message;
            updateIf('progress','<hr><i>(disconnected from Survey Tool)</i></hr>');
            updateIf('uptime','down');
            updateIf('visitors','nobody');
        }
    });
}

var timerSpeed = 15000;

function setTimerOn() {
    updateStatus();
    timerID = setInterval(updateStatus, timerSpeed);
}

function resetTimerSpeed(speed) {
	timerSpeed = speed;
	clearInterval(timerID);
	timerID = setInterval(updateStatus, timerSpeed);
}

window.onload = setTimerOn;

</script>
