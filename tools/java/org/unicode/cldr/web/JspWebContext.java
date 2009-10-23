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
import org.unicode.cldr.util.SupplementalDataInfo;

/**
 * This class has routines on it helpful for JSPs.
 * @author srl
 *
 */
public class JspWebContext extends WebContext {

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
	
	public void openTable() {
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
}
