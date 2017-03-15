/**
 * Copyright (C) 2009-2010 IBM Corp. and Others. All Rights Reserved.
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

    /**
     * Process data, without any hint.
     *
     * @return the result handler for the operation
     */
    public SummarizingSubmissionResultHandler processDataSubmission() {
        return processDataSubmission(null);
    }

    /**
     * Process data.
     *
     * @param xpathHint
     *            base xpath to identify which fields ar ebeing submitted.
     */
    public SummarizingSubmissionResultHandler processDataSubmission(String xpathHint) {
        SummarizingSubmissionResultHandler ssrh = null;

        String podBases = this.field("pod_bases", null);
        if (podBases == null) {
            if (debugJsp()) {
                this.println("Submitting data for base " + xpathHint + "<br>");
            }
            ssrh = SurveyForum.processDataSubmission(this, xpathHint, ssrh);
        } else {
            for (String base : podBases.split(",")) {
                if (base != null && !base.isEmpty()) {
                    int xpathNumber = Integer.parseInt(base);
                    String xpath = sm.xpt.getById(xpathNumber);
                    if (debugJsp()) {
                        this.println("Submitting data for base " + xpathNumber + ":" + xpath + "<br>");
                    }
                    ssrh = SurveyForum.processDataSubmission(this, xpath, ssrh);
                }
            }
        }
        if (debugJsp()) {
            this.println(".. done submitting. SSRH:" + ssrh.toString() + "<br>");
        }
        this.put("ssrh", ssrh); // TODO: move to constant or API
        return ssrh;
    }

    /**
     * This adds a base to the list of paths to process/
     *
     * @param base
     *            base xpath to add
     * @see #doneWithXpaths()
     * @internal
     */
    private void addPodBase(String base) {
        if (podBases == null) {
            podBases = new HashSet<String>();
        }
        podBases.add(base);
    }

    /**
     * Open up the main form, if not already open. May be called multiple times.
     */
    public void openMainForm() {
        if (mainFormState == MainFormState.CLOSED) {
            // if(debugJsp()) {
            // this.println("<h4>&lt;form url='"+url()+"'&gt;</h4>");
            // }
            sm.printPathListOpen(this);
            // printUrlAsHiddenFields();
            mainFormState = MainFormState.OPEN;
        } else if (debugJsp()) {
            this.println("<h4><i>duplicate &lt;form/&gt;</i></h4>");
        }

    }

    /**
     * Close the main <form> iff it is already open. Throws an error if it was
     * not open.
     */
    public void closeMainForm() {
        if (mainFormState == MainFormState.OPEN) {
            String nextStep = (String) this.get("nextStep");
            if (nextStep != null) {
                this.println("Not implemented:  nextStep");
                STFactory.unimp();
                // this.println("<input type='hidden' name='step' value='"+
                // nextStep+"'>");
            }
            // if(canModify()) {
            // this.println("<input type='submit' value='Submit'>");
            // } else {
            // this.println("<input type='submit' value='Continue'>");
            // }
            sm.printPathListClose(this);
            mainFormState = MainFormState.CLOSED;
            if (debugJsp()) {
                this.println("<h4>&lt;/form&gt;</h4>");
            }
        } else {
            throw new InternalError("Error: main form is already " + mainFormState.name());
        }
    }

    /**
     * Open a table with a 'code' column.
     *
     * @see #closeTable()
     */
    public void openTable() {
        openMainForm();
        this.put(SurveyMain.DATAROW_JSP, DATAROW_JSP_CODE);
        SurveyMain.printSectionTableOpenCode(this);
    }

    /**
     * Open a table, but without the 'code' column.
     *
     * @see #closeTableNoCode()
     */
    public void openTableNoCode() {
        openMainForm();
        this.put(SurveyMain.DATAROW_JSP, SurveyMain.DATAROW_JSP_DEFAULT);
        SurveyForum.printSectionTableOpenShort(this, null);
    }

    /**
     * Show a single xpath's data to the user for submission/vetting.
     *
     * @param xpath
     *            path to show
     * @see #doneWithXpaths()
     */
    public void showXpath(String xpath) {
        openMainForm();
        String podBase = SurveyForum.showXpathShort(this, xpath, xpath);
        addPodBase(podBase);
    }

    /**
     * Show a list of xpaths. This is the same as calling showXpath repeatedly.
     *
     * @param xpaths
     *            list of xpaths to show.
     */
    public void showXpath(String[] xpaths) {
        for (String xpath : xpaths) {
            showXpath(xpath);
        }
    }

    /**
     * Close the table.
     *
     * @see #openTable()
     */
    public void closeTable() {
        SurveyMain.printSectionTableCloseCode(this);
    }

    /**
     * Close the table without a 'code' column.
     *
     * @see #openTableNoCode()
     */
    public void closeTableNoCode() {
        SurveyForum.printSectionTableCloseShort(this, null);
    }

    /**
     * This should be called after all calls to showXpath are done.
     *
     * @see #showXpath(String)
     */
    public void doneWithXpaths() {
        StringBuffer sb = null;
        if (podBases == null) {
            this.println("<!-- no items included. No hidden field needed -->");
            return;
        }
        for (String base : podBases) {
            int xpath = sm.xpt.getByXpath(base);
            if (sb == null) {
                sb = new StringBuffer(Integer.toString(xpath));
            } else {
                sb.append(',');
                sb.append(Integer.toString(xpath));
            }
        }
        if (debugJsp()) {
            this.println("Submitting hidden field for bases: " + sb + "<br>");
        }
        this.println("<input name='pod_bases' type='hidden' value='" + sb + "'>");
        closeMainForm();
    }

    /**
     * @return URL to the 'top' of a survey tool locale.
     */
    public String urlToLocale() {
        return urlToLocale(getLocale());
    }

    /**
     * @return URL to the top of a certain survey tool locale.
     * @param locale
     *            as a CLDRLocale
     */
    public String urlToLocale(CLDRLocale locale) {
        return base() + "?_=" + locale.getBaseName();
    }

    /**
     * @return URL to a particular section in the current locale
     * @param sectionName
     *            section name
     * @see DataSection#xpathToSectionBase(String)
     */
    public String urlToSection(String sectionName) {
        return urlToLocale() + "&x=" + sectionName;
    }

    /**
     * URL to a particular section in a particular locale
     *
     * @param locale
     * @param sectionName
     * @see DataSection#xpathToSectionBase(String)
     */
    public String urlToSection(CLDRLocale locale, String sectionName) {
        return urlToLocale(locale) + "&x=" + sectionName;
    }
}
