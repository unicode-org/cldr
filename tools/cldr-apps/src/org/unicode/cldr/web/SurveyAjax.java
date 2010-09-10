package org.unicode.cldr.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Servlet implementation class SurveyAjax
 */
public class SurveyAjax extends HttpServlet {
    public static final class JSONWriter {
        private final JSONObject j = new JSONObject();
        
        public JSONWriter() {
        }
        
        public final void put(String k, String v) {
            try {
                j.put(k, v);
            } catch (JSONException e) {
                throw new IllegalArgumentException(e.toString(),e);
            }
        }
        
        public final String toString() {
            return j.toString();
        }
    }

    
    private static final long serialVersionUID = 1L;
    public static final String REQ_WHAT = "what";
    private static final String WHAT_STATUS = "status";
    public static final String AJAX_STATUS_SCRIPT = "ajax_status.jspf";
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SurveyAjax() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    request.setCharacterEncoding("UTF-8");
	    response.setCharacterEncoding("UTF-8");
	    SurveyMain sm = SurveyMain.getInstance(request);
	    PrintWriter out = response.getWriter();
	    String what = request.getParameter(REQ_WHAT);
	    if(sm == null) {
	        sendNoSurveyMain(out);
	    } else if(what==null) {
	        sendError(out, "Missing parameter: " + REQ_WHAT);
	    } else if(what.equals(WHAT_STATUS)) {
	        sendStatus(sm,out);
	    } else {
	        sendError(out,"Unknown Request: " + what);
	    }
	}

    private void sendStatus(SurveyMain sm, PrintWriter out) throws IOException {
        JSONWriter r = newJSON();
        r.put("SurveyOK","1");
        r.put("isSetup", (sm.isSetup)?"1":"0");
        r.put("isBusted", (sm.isBusted!=null)?"1":"0");
        r.put("visitors", sm.getGuestsAndUsers());
        r.put("uptime", sm.uptime.toString());
        r.put("progress", sm.getTopBox());
//        StringBuffer progress = new StringBuffer(sm.getProgress());
//        String threadInfo = sm.startupThread.htmlStatus();
//        if(threadInfo!=null) {
//            progress.append("<br/><b>Processing:"+threadInfo+"</b><br>");
//        }
        //r.put("progress", progress.toString());
        send(r,out);
    }

    private JSONWriter newJSON() {
        JSONWriter r = new JSONWriter();
        r.put("progress", "");
        r.put("visitors", "");
        r.put("uptime", "");
        r.put("err", "");
        r.put("SurveyOK","0");
        r.put("isSetup","0");
        r.put("isBusted","0");
        return r;
    }

	private void sendNoSurveyMain(PrintWriter out) throws IOException {
        JSONWriter r = newJSON();
        r.put("SurveyOK","0");
        r.put("err","The Survey Tool is not running.");
        send(r,out);
    }

    private void sendError(PrintWriter out, String string) throws IOException {
        JSONWriter r = newJSON();
        r.put("SurveyOK","0");
        r.put("err",string);
        send(r,out);
    }

    private void send(JSONWriter r, PrintWriter out) throws IOException {
        out.print(r.toString());
    }

    /**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    doGet(request, response);
	}

}
