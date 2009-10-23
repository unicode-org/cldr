/**
 * Copyright (C) 2009 IBM Corp. and Others. All Rights Reserved.
 */
package org.unicode.cldr.web;

import java.io.IOException;

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
}
