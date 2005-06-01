/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Mark Davis
**********************************************************************
*/
package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.xml.sax.SAXException;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.tool.UOption;

/**
 * This is a simple class that walks through the CLDR hierarchy.
 * It gathers together all the items from all the locales that share the
 * same element chain, and thus presents a "sideways" view of the data, in files called
 * by_type/X.html, where X is a type. X may be the concatenation of more than more than
 * one element, where the file would otherwise be too large.
 * @author medavis
 */
/*
Notes:
http://xml.apache.org/xerces2-j/faq-grammars.html#faq-3
http://developers.sun.com/dev/coolstuff/xml/readme.html
http://lists.xml.org/archives/xml-dev/200007/msg00284.html
http://java.sun.com/j2se/1.4.2/docs/api/org/xml/sax/DTDHandler.html
 */
public class GenerateSidewaysView {
    // debug flags
    static final boolean DEBUG = false;
    static final boolean DEBUG2 = false;
    static final boolean DEBUG_SHOW_ADD = false;
    static final boolean DEBUG_ELEMENT = false;
    static final boolean DEBUG_SHOW_BAT = false;

    static final boolean FIX_ZONE_ALIASES = true;

    private static final int
        HELP1 = 0,
        HELP2 = 1,
        SOURCEDIR = 2,
        DESTDIR = 3,
        MATCH = 4,
        SKIP = 5,
        TZADIR = 6,
        NONVALIDATING = 7,
        SHOW_DTD = 8,
		TRANSLIT = 9;

    private static final String NEWLINE = "\n";

    private static final UOption[] options = {
            UOption.HELP_H(),
            UOption.HELP_QUESTION_MARK(),
            UOption.SOURCEDIR().setDefault(Utility.MAIN_DIRECTORY),
            UOption.DESTDIR().setDefault(Utility.GEN_DIRECTORY + "charts\\by_type\\"),
            UOption.create("match", 'm', UOption.REQUIRES_ARG).setDefault(".*"),
            UOption.create("skip", 'z', UOption.REQUIRES_ARG).setDefault("zh_(C|S|HK|M).*"),
            UOption.create("tzadir", 't', UOption.REQUIRES_ARG).setDefault("C:\\ICU4J\\icu4j\\src\\com\\ibm\\icu\\dev\\tool\\cldr\\"),
            UOption.create("nonvalidating", 'n', UOption.NO_ARG),
            UOption.create("dtd", 'w', UOption.NO_ARG),
            UOption.create("transliterate", 'y', UOption.NO_ARG),
    };
    private static String timeZoneAliasDir = null;
    private static Map path_value_locales = new TreeMap();
    private static XPathParts parts = new XPathParts(null, null);

    public static void main(String[] args) throws SAXException, IOException {
        UOption.parseArgs(args, options);
        Factory cldrFactory = CLDRFile.Factory.make(options[SOURCEDIR].value, options[MATCH].value);
        Set alllocales = cldrFactory.getAvailable();
        // gather all information
        // TODO tweek for value-laden attributes
        for (Iterator it = alllocales.iterator(); it.hasNext();) {
        	String localeID = (String) it.next();
        	System.out.println("Loading: " + localeID);
        	CLDRFile cldrFile = cldrFactory.make(localeID, false);
        	for (Iterator it2 = cldrFile.keySet().iterator(); it2.hasNext();) {
        		String path = (String) it2.next();
        		String value = cldrFile.getStringValue(path);
        		String fullPath = cldrFile.getFullXPath(path);
        		Map value_locales = (Map) path_value_locales.get(path);
        		if (value_locales == null ) path_value_locales.put(path, value_locales = new TreeMap());
        		Set locales = (Set) value_locales.get(value);
        		if (locales == null) value_locales.put(value, locales = new TreeSet());
        		locales.add(localeID);
        	}
        }
        String oldMain = "";
        PrintWriter out = null;
        
        System.out.println("Getting types");
        String[] partial = {""};
        Set types = new TreeSet();
        for (Iterator it = path_value_locales.keySet().iterator(); it.hasNext();) {       	
        	String path = (String)it.next();
        	String main = getFileName(path, partial);
        	if (!main.equals(oldMain)) {
        		oldMain = main;
         		types.add(main);
        	}
        }

        System.out.println("Printing files");
        for (Iterator it = path_value_locales.keySet().iterator(); it.hasNext();) {       	
        	String path = (String)it.next();
        	String main = getFileName(path, partial);
        	if (!main.equals(oldMain)) {
        		oldMain = main;
         		out = start(out, main, types);
        	}
        	out.println("<tr><th colSpan='2' class='path'>" + partial[0] + "</th><tr>");
        	Map value_locales = (Map) path_value_locales.get(path);
        	for (Iterator it2 = value_locales.keySet().iterator(); it2.hasNext();) {
            	String value = (String)it2.next();
            	out.println("<tr><th width='1%' nowrap>" + value + "</th><td>");
            	Set locales = (Set) value_locales.get(value);
            	boolean first = true;
            	for (Iterator it3 = locales.iterator(); it3.hasNext();) {
                	String locale = (String)it3.next();
                	if (first) first = false;
                	else out.print(" ");
                	out.print("\u00B7" + locale + "\u00B7");           		
            	}
            	out.println("</td><tr>");
        	}
        }
        finish(out);
        System.out.println("Done");
    }

	/**
	 * 
	 */
	private static String getFileName(String path, String[] partial) {
		parts.set(path);
		int start = 1;
		String main = parts.getElement(start);
		if (main.equals("localeDisplayNames")
				|| main.equals("dates")
				|| main.equals("numbers")) {
			start = 2;
			String part2 = parts.getElement(start);
			main += "_" + part2;
			if (part2.equals("calendars")) {
				start = 3;
				Map m = parts.getAttributes(start);
				part2 = (String) m.get("type");
				main += "_" + part2;				
			}
		}
		partial[0] = parts.toString(start + 1, parts.size());
		return main;
	}

	/**
	 * 
	 */
	private static PrintWriter start(PrintWriter out, String main, Set types) throws IOException {
   		finish(out);
   		String title = "CLDR Sideways Data for ";
		out = BagFormatter.openUTF8Writer(options[DESTDIR].value, main + ".html");
		out.println("<html><head>");
		out.println("<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
		out.println("<title>" + title  + main + "</title>");
		out.println("<style>");
		out.println("<!--");
		out.println(".path        { background-color: #00FF00 }");
		out.println("td,th        { text-align:left; vertical-align:top; border: 1px solid blue; padding: 2 }");
		out.println("table        { border-collapse: collapse }");
		out.println("-->");
		out.println("</style>");
		out.println("</head>");
		out.println("<body><h1>" + title + "<i>" +  main + "</i></h1><p>");
		boolean first = true;
		for (Iterator it = types.iterator(); it.hasNext();) {
			String fileName = (String) it.next();
			if (first) first = false;
			else out.println(" | ");
			out.println("<a href='" + fileName + 
					".html'>" + fileName +
					"</a>");
		}
		out.println("</p><table>");
		return out;
	}

	/**
	 * 
	 */
	private static void finish(PrintWriter out) {
		if (out == null) return;
		out.println("</table></body></html>");
		out.close();
	}
}
