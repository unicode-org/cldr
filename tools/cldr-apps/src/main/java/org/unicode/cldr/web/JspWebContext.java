/**
 * Copyright (C) 2009-2010 IBM Corp. and Others. All Rights Reserved.
 */
package org.unicode.cldr.web;

import java.io.IOException;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.SupplementalDataInfo;

/**
 * This class has routines on it helpful for JSPs.
 *
 * See this URL for documentation about the Easy Steps system:
 * http://cldr.unicode.org/development/coding-cldr-tools/easy-steps
 *
 * @author srl
 *
 */
public class JspWebContext extends WebContext {

    private enum MainFormState {
        OPEN, CLOSED
    };

    MainFormState mainFormState = MainFormState.CLOSED;
    /**
     * List of xpaths indicating the types of data being submitted (numbers,
     * currency, etc)
     */
    Set<String> podBases = null;

    /**
     * Name of the JSP to be used for showing a row of output
     */
    private static final Object DATAROW_JSP_CODE = "datarow_short_code.jsp";

    /**
     * Create a JspContext from a raw HTTP connection.
     *
     * @param irq
     * @param irs
     * @throws IOException
     */
    public JspWebContext(HttpServletRequest irq, HttpServletResponse irs) throws IOException {
        super(irq, irs);
    }

    /**
     * For creating a JspContext from another WebContext. Slicing is fine here
     * (for now), as there isn't any extra state in a JspWebContext
     *
     * @param other
     */
    public JspWebContext(WebContext other) {
        super(other);
        if (other instanceof JspWebContext) {
            this.podBases = ((JspWebContext) other).podBases;
        }
    }

    /**
     * @return the resolved CLDRFile for the current locale.
     * @deprecated leaks UserFile
     */
    public CLDRFile resolvedFile() {
        return SurveyForum.getResolvedFile(this);
    }

    /**
     * @return the non-resolved CLDRFile for the current locale.
     * @deprecated leaks UserFile
     */
    public CLDRFile cldrFile() {
        return SurveyForum.getCLDRFile(this);
    }

    /**
     * @return a copy of the SupplementalDataInfo
     * @see SupplementalDataInfo
     */
    public SupplementalDataInfo supplementalDataInfo() {
        return sm.getSupplementalDataInfo();
    }

    /**
     * Is JSP debugging on?
     *
     * @return true if the 'JSP debugging' switch is on
     */
    public boolean debugJsp() {
        return this.prefBool(SurveyMain.PREF_DEBUGJSP);
    }
}
