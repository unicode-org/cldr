package org.unicode.cldr.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;


/**
 * Servlet implementation class SurveyAjax
 */
public class SurveyAjax extends HttpServlet {
	public static final class JSONWriter {
		private final JSONObject j = new JSONObject();

		public JSONWriter() {
		}

		public final void put(String k, Object v) {
			try {
				j.put(k, v);
			} catch (JSONException e) {
				throw new IllegalArgumentException(e.toString(),e);
			}
		}

		public final String toString() {
			return j.toString();
		}

		public static JSONObject wrap(CheckStatus status) throws JSONException {
			final CheckStatus cs = status;
			return new JSONObject() {
				{
					put("message", cs.getMessage());
					put("htmlMessage", cs.getHTMLMessage());
					put("type", cs.getType());
					put("cause", wrap(cs.getCause()));
					put("subType", cs.getSubtype().name());
				}
			};
		}
		
		public static JSONObject wrap(CheckCLDR check) throws JSONException {
			final CheckCLDR cc = check;
			return new JSONObject() {
				{
					put("class", cc.getClass().getSimpleName());
					put("phase", cc.getPhase());
				}
			};
		}

		public static List<Object> wrap(List<CheckStatus> list) throws JSONException {
			List<Object> newList = new ArrayList<Object>();
			for(final CheckStatus cs : list) {
				newList.add(wrap(cs));
			}
			return newList;
		}
	}


	private static final long serialVersionUID = 1L;
	public static final String REQ_WHAT = "what";
	public static final String REQ_SESS = "s";
	public static final String WHAT_STATUS = "status";
	public static final String AJAX_STATUS_SCRIPT = "ajax_status.jspf";
	public static final Object WHAT_VERIFY = "verify";

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public SurveyAjax() {
		super();
	}


	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		setup(request,response);
		StringBuilder sb = new StringBuilder();
		Reader r = request.getReader();
		int ch;
		while((ch = r.read())>-1) {
//			System.err.println(" >> " + Integer.toHexString(ch));
			sb.append((char)ch);
		}		
		processRequest(request, response, sb.toString());
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {	
		setup(request,response);
		processRequest(request,response,WebContext.decodeFieldString(request.getParameter(SurveyMain.QUERY_VALUE_SUFFIX)));
	}
	
	private void setup(HttpServletRequest request, HttpServletResponse response)  throws ServletException, IOException  {
		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/json");
	}

	private void processRequest(HttpServletRequest request, HttpServletResponse response, String val)  throws ServletException, IOException  {
//		if(val != null) {
//			System.err.println("val="+val);
//		}
		SurveyMain sm = SurveyMain.getInstance(request);
		PrintWriter out = response.getWriter();
		String what = request.getParameter(REQ_WHAT);
		String sess = request.getParameter(SurveyMain.QUERY_SESSION);
		String loc = request.getParameter(SurveyMain.QUERY_LOCALE);
		String xpath = request.getParameter(SurveyForum.F_XPATH);
		String fieldHash = request.getParameter(SurveyMain.QUERY_FIELDHASH);
		CookieSession mySession = null;
		try {
			if(sm == null) {
				sendNoSurveyMain(out);
			} else if(what==null) {
				sendError(out, "Missing parameter: " + REQ_WHAT);
			} else if(what.equals(WHAT_STATUS)) {
				sendStatus(sm,out);

			} else if(sess!=null && !sess.isEmpty()) { // this and following: session needed
				mySession = CookieSession.retrieve(sess);
				if(mySession==null) {
					sendError(out, "Missing Session: " + sess);
				} else {
					if(what.equals(WHAT_VERIFY)) {
						CheckCLDR cc = sm.createCheckWithoutCollisions();
						int id = Integer.parseInt(xpath);
						String xp = sm.xpt.getById(id);
						Map<String, String> options = null;
						List<CheckStatus> result = new ArrayList<CheckStatus>();
						//CLDRFile file = CLDRFile.make(loc);
						//CLDRFile file = mySession.
						CLDRFile file = sm.getUserFile(mySession, CLDRLocale.getInstance(loc)).cldrfile;
						cc.setCldrFileToCheck(file, SurveyMain.basicOptionsMap(), result);
						cc.check(xp, file.getFullXPath(xp), val, options, result);

						JSONWriter r = newJSONStatus(sm);
						r.put(SurveyMain.QUERY_FIELDHASH, fieldHash);

						r.put("testResults", JSONWriter.wrap(result));
						r.put("testsRun", cc.toString());
						r.put("testsV", val);
						r.put("testsLoc", loc);
						r.put("xpathTested", xp);
						r.put("dataEmpty", Boolean.toString(file.isEmpty()));

						send(r,out);
					}
				}
			} else {
				sendError(out,"Unknown Request: " + what);
			}
		} catch (JSONException e) {
			e.printStackTrace();
			sendError(out, "JSONException: " + e);
		}
	}


	private void sendStatus(SurveyMain sm, PrintWriter out) throws IOException {
		JSONWriter r = newJSONStatus(sm);
		//        StringBuffer progress = new StringBuffer(sm.getProgress());
		//        String threadInfo = sm.startupThread.htmlStatus();
		//        if(threadInfo!=null) {
		//            progress.append("<br/><b>Processing:"+threadInfo+"</b><br>");
		//        }
		//r.put("progress", progress.toString());
		send(r,out);
	}

	private void setupStatus(SurveyMain sm, JSONWriter r) {
		r.put("SurveyOK","1");
		r.put("isSetup", (sm.isSetup)?"1":"0");
		r.put("isBusted", (sm.isBusted!=null)?"1":"0");
		r.put("visitors", sm.getGuestsAndUsers());
		r.put("uptime", sm.uptime.toString());
		r.put("progress", sm.getTopBox(false));
	}

	private JSONWriter newJSONStatus(SurveyMain sm) {
		JSONWriter r = newJSON();
		setupStatus(sm, r);
		return r;
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

	public enum AjaxType { STATUS, VERIFY  };

	/**
	 * Helper function for getting the basic AJAX status script included.
	 */
	public static void includeAjaxScript(HttpServletRequest request, HttpServletResponse response, AjaxType type) throws ServletException, IOException
	{
		WebContext.includeFragment(request, response, "ajax_"+type.name().toLowerCase()+".jsp");
	}

}
