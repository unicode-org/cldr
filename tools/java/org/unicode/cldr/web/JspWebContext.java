/**
 * Copyright (C) 2009 IBM Corp. and Others. All Rights Reserved.
 */
package org.unicode.cldr.web;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.SupplementalDataInfo;

/**
 * This class has routines on it helpful for JSPs.
 * @author srl
 *
 */
public class JspWebContext extends WebContext {

	private static final Object DATAROW_JSP_CODE = "datarow_short_code.jsp";

	/**
	 * For creating a JspContext from a raw input/output stream.
	 * @param irq
	 * @param irs
	 * @throws IOException
	 */
	public JspWebContext(HttpServletRequest irq, HttpServletResponse irs)
			throws IOException {
		super(irq, irs);
		// TODO Auto-generated constructor stub
	}

	/**
	 * For creating a JspContext from a WebContext.
	 * Slicing is fine here (for now), no extra state in a JspWebContext
	 * @param other
	 */
	public JspWebContext(WebContext other) {
		super(other);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Get the current CLDRFile. Not resolved.
	 * @return
	 */
	public CLDRFile cldrFile() {
		return SurveyForum.getCLDRFile(this);
	}
	
	/**
	 * Get a copy of the SupplementalDataInfo
	 * @return
	 */
	public SupplementalDataInfo supplementalDataInfo() {
		SupplementalDataInfo supplementalData = SupplementalDataInfo.getInstance(cldrFile().getSupplementalDirectory());
		return supplementalData;
	}
	
	/**
	 * Is JSP debugging on?
	 * @return
	 */
	public boolean debugJsp() {
		return this.prefBool(SurveyMain.PREF_DEBUGJSP);
	}
	
	/**
	 * Process data, without any hint.
	 * @return
	 */
	public SummarizingSubmissionResultHandler processDataSubmission() {
		return processDataSubmission(null);
	}
	
	/**
	 * Process data.
	 * @param xpathHint
	 * @return
	 */
	public SummarizingSubmissionResultHandler processDataSubmission(String xpathHint) {
		SummarizingSubmissionResultHandler ssrh  = null;
		
		String podBases = this.field("pod_bases", null);
		if(podBases==null) {
			if(debugJsp()) {
				this.println("Submitting data for base "+xpathHint+"<br>");
			}
			ssrh = SurveyForum.processDataSubmission(this, xpathHint, ssrh);
		} else {
			for (String base : podBases.split(",")) {
				if(base!=null&&!base.isEmpty()) {
					int xpathNumber = Integer.parseInt(base);
					String xpath = sm.xpt.getById(xpathNumber);
					if(debugJsp()) {
						this.println("Submitting data for base "+xpathNumber+":"+xpath+"<br>");
					}
					ssrh = SurveyForum.processDataSubmission(this, xpath, ssrh);
				}
			}
		}
		if(debugJsp()) {
			this.println(".. done submitting. SSRH:"+ssrh.toString()+"<br>");
		}
		this.put("ssrh",ssrh); // TODO: move to constant or API
		return ssrh;
	}
	
	Set<String> ourPodBases = null;
	
	private void addPodBase(String base) {
		if(ourPodBases==null) {
			ourPodBases = 	new HashSet<String>();
		}
		ourPodBases.add(base);
//		if(false || debugJsp()) {
//			this.println("<tr><td>added base: "+base+"</td></tr>");
//		}
	}
	
	/**
	 * Open a table with a 'code' column.
	 */
	public void openTable() {
		this.put(SurveyMain.DATAROW_JSP,DATAROW_JSP_CODE);
		SurveyMain.printSectionTableOpenCode(this);
	}
	/**
	 *  Also, sets the 'datarow' type to not include code.
	 */
	public void openTableNoCode() {
		this.put(SurveyMain.DATAROW_JSP, SurveyMain.DATAROW_JSP_DEFAULT);
		SurveyForum.printSectionTableOpenShort(this, null);
	}
	/**
	 * Show one xpath.
	 * @param xpath
	 */
	public void showXpath(String xpath) {
		String podBase = SurveyForum.showXpathShort(this, xpath, xpath);
		addPodBase(podBase);
	}
	/**
	 * Convenience: show xpaths from an array
	 * @param xpaths
	 */
	public void showXpath(String[] xpaths) {
		for(String xpath : xpaths) {
			showXpath(xpath);
		}
	}
	/**
	 * End of the table.
	 */
	public void closeTable() {
		SurveyMain.printSectionTableCloseCode(this);
	}
	
	public void closeTableNoCode() {
		SurveyForum.printSectionTableCloseShort(this, null);
	}
	/**
	 * Important for mixed-xpath pages. Notify the server which xpaths to check on.
	 */
	public void doneWithXpaths() {
		StringBuffer sb = null;
		for(String base : ourPodBases) {
			int xpath = sm.xpt.getByXpath(base);
			if(sb==null) {
				sb = new StringBuffer(Integer.toString(xpath));
			} else {
				sb.append(',');
				sb.append(Integer.toString(xpath));
			}
		}
		if(debugJsp()) {
			this.println("Submitting hidden field for bases: "+sb+"<br>");
		}
		this.println("<input name='pod_bases' type='hidden' value='"+sb+"'>");
	}
	
	/**
	 * Check based on user permissions.
	 */
	public Boolean canModify() {
		if(canModify == null) {
			if(session==null||session.user==null) {
				return setCanModify(false);
			}
			return setCanModify(UserRegistry.userCanModifyLocale(session.user, this.getLocale()));
		} else {
			return super.canModify();
		}
	}
	
	
	/**
	 * URL to the 'top' of a survey tool locale.
	 * @return
	 */
	public String urlToLocale() {
		return urlToLocale(getLocale());
	}

	/**
	 * URL to the top of a survey tool locale.
	 * @param locale
	 * @return
	 */
	public String urlToLocale(CLDRLocale locale) {
		return base()+"?_="+locale.getBaseName();
	}
	
	/**
	 * URL to a particular section in the current locale
	 * @param sectionName
	 * @return
	 */
	public String urlToSection(String sectionName) {
		return urlToLocale()+"&x="+sectionName;
	}

	/**
	 * URL to a particular section in a particular locale
	 * @param locale
	 * @param sectionName
	 * @return
	 */
	public String urlToSection(CLDRLocale locale, String sectionName) {
		return urlToLocale(locale)+"&x="+sectionName;
	}
}
