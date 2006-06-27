/*
 ******************************************************************************
 * Copyright (C) 2005, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
*/
package org.unicode.cldr.tool;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.PrettyPath;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.impl.CollectionUtilities;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.util.ULocale;

public class ShowData {
	private static final int
	HELP1 = 0,
	HELP2 = 1,
	SOURCEDIR = 2,
	DESTDIR = 3,
	MATCH = 4
	;
	
	private static final UOption[] options = {
		UOption.HELP_H(),
		UOption.HELP_QUESTION_MARK(),
		UOption.SOURCEDIR().setDefault(Utility.MAIN_DIRECTORY),
		UOption.DESTDIR().setDefault(Utility.GEN_DIRECTORY + "main/charts/locales/"),
		UOption.create("match", 'm', UOption.REQUIRES_ARG).setDefault(".*"),
	};
	
	static RuleBasedCollator uca = (RuleBasedCollator) Collator.getInstance(ULocale.ROOT);
	{
		uca.setNumericCollation(true);
	}

	static PrettyPath prettyPathMaker = new PrettyPath();
	static CLDRFile english;
	static Set locales;
	static Factory cldrFactory;
	
	public static void main(String[] args) throws Exception {
        try {
    		UOption.parseArgs(args, options);
    		String sourceDir = options[SOURCEDIR].value;	// Utility.COMMON_DIRECTORY + "main/";
    		String targetDir = options[DESTDIR].value;	// Utility.GEN_DIRECTORY + "main/";
			cldrFactory = Factory.make(sourceDir, ".*");
			locales = new TreeSet(cldrFactory.getAvailable());
			new Utility.MatcherFilter(options[MATCH].value).retainAll(locales);
			//Set paths = new TreeSet();
        	Set prettySet = new TreeSet(uca);
        	Set skipList = new HashSet(Arrays.asList(new String[]{"id"}));
        	
        	english = (CLDRFile) cldrFactory.make("en", true);
        	CLDRFile.Status status = new CLDRFile.Status();

			List nonDistinguishingAttributes = new ArrayList();
			for (Iterator it = locales.iterator(); it.hasNext();) {				
				String locale = (String) it.next();
				if (locale.startsWith("supplem") || locale.startsWith("character")) continue;
				
				CLDRFile file = (CLDRFile) cldrFactory.make(locale, true);
				if (file.isNonInheriting()) continue; // for now, skip supplementals				
				
				// put into set of simpler paths
				// and filter if necessary
				int skippedCount = 0;
				int aliasedCount = 0;
				int inheritedCount = 0;
				prettySet.clear();
            	for (Iterator it2 = file.iterator(); it2.hasNext();) {
            		String path = (String)it2.next();
					if (path.indexOf("/alias") >= 0) {
						skippedCount++;
						continue; // skip code fllback
					}
            		String prettyString = prettyPathMaker.getPrettyPath(path);
            		prettySet.add(prettyString);
            	}
            	
				PrintWriter pw = BagFormatter.openUTF8Writer(targetDir, locale + ".html");
				pw.println("<html><head>");
				pw.println("<style type='text/css'>");
				pw.println("<!--");
				pw.println(".e {background-color: #FFFF00}");
				pw.println(".i {background-color: #FFFFCC}");
				pw.println(".v {background-color: #DDDDDD}");
				pw.println(".a {background-color: #9999FF}");
				pw.println(".ah {background-color: #FF99FF}");
				pw.println(".h {background-color: #FF9999}");
				pw.println(".n {color: #999999}");
				pw.println(".g {background-color: #99FF99}");
				pw.println("-->");
				pw.println("</style>");
				pw.println("<script>");
				pw.println("if (location.href.split('?')[1].split(',')[0]=='hide') {");
				pw.println("document.write('<style>');");
				pw.println("document.write('.xx {display:none}');");
				pw.println("document.write('</style>');");
				pw.println("}");
				pw.println("</script>");
				pw.println("<title>" + getName(locale) + "</title>");
				pw.println("</head><body>");
				pw.println("<h1>" + getName(locale) + "</h1>");
				pw.println("<p>");
				showLinks(pw, locale);
				pw.println("</p><p>");
				showChildren(pw, locale);
				pw.println("</p>");
				pw.println("<p>Aliased/Inherited: <a href='" + locale + ".html?hide'>Hide</a> <a href='" + locale + ".html'>Show </a></p>");
				pw.println("<table border=\"1\" cellpadding=\"2\" cellspacing=\"0\">");

				pw.println("<tr><th>No</th><th colSpan=3>Path</th><th>Native</th><th>English</th><th>Nat. Att.</th><th>Eng. Att.</th><tr>");

				int count = 0;
				String[] oldParts = new String[4];
				for (Iterator it2 = prettySet.iterator(); it2.hasNext();) {
					String prettyPath = (String) it2.next();
					String path = prettyPathMaker.getOriginal(prettyPath);
					boolean showEnglish = path.indexOf("/references") < 0;
					
					String source = file.getSourceLocaleID(path, status);
					boolean isAliased = !status.pathWhereFound.equals(path);
					if (isAliased) {
						aliasedCount++;
					}
					boolean isInherited = !source.equals(locale);
					if (isInherited) {
						inheritedCount++;
					}

					String value = file.getStringValue(path);
					String englishValue = english.getStringValue(path);
					if (!showEnglish || englishValue == null) englishValue = "";
					String fullPath = file.getFullXPath(path);
					String nda = "";
					if (!fullPath.equals(path)) {
						file.getNonDistinguishingAttributes(fullPath, nonDistinguishingAttributes, skipList);
						if (nonDistinguishingAttributes.size() != 0) nda = nonDistinguishingAttributes.toString();
						nda = nda.replaceAll("[/]", "/\u200B");
					}
					String englishFullPath = english.getFullXPath(path);
					String englishNda = "";
					if (showEnglish && englishFullPath != null && !englishFullPath.equals(path)) {
						file.getNonDistinguishingAttributes(englishFullPath, nonDistinguishingAttributes, skipList);
						if (nonDistinguishingAttributes.size() != 0) englishNda = nonDistinguishingAttributes.toString();
						englishNda = englishNda.replaceAll("([=/.?_&])", "$1\u200B");
					}
					prettyPath = TransliteratorUtilities.toHTML.transliterate(prettyPathMaker.getOutputForm(prettyPath));
					String[] pathParts = prettyPath.split("[|]");
					// count the <td>'s and pad
					//int countBreaks = Utility.countInstances(prettyPath, "</td><td>");
					//prettyPath += Utility.repeat("</td><td>", 3-countBreaks);
					prettyPath = "";
					for (int i = 0; i < 3; ++i) {
						String newPart = i < pathParts.length ? pathParts[i] : "";
						if (newPart.equals(oldParts[i])) {
							prettyPath += "</td><td class='n'>";
						} else {
							if (newPart.length() == 0) {
								prettyPath += "</td><td>";
							} else {
								prettyPath += "</td><td class='g'>";								
							}
							oldParts[i] = newPart;
						}
						prettyPath += newPart;
					}
					String statusClass = isAliased ? (isInherited ? " class='ah'" : " class='a'") 
							: (isInherited ? " class='h'" : "");
						
					pw.println((isAliased || isInherited ? "<tr class='xx'><td" : "<tr><td") + statusClass + ">" + (++count) 
							+ prettyPath
							//+ "</td><td>" + TransliteratorUtilities.toHTML.transliterate(lastElement)
							+ (value.length() == 0 ? "</td><td>n/a" : "</td><td class='v'>" + TransliteratorUtilities.toHTML.transliterate(value))
							+ (englishValue.length() == 0 ? "</td><td>n/a" 
									: englishValue.equals(value) ? "</td><td class='i'>==" 
											: "</td><td class='e'>" + TransliteratorUtilities.toHTML.transliterate(englishValue))
							+ "</td><td>" + (nda.length() == 0 ? "&nbsp;" : TransliteratorUtilities.toHTML.transliterate(nda))
							+ (englishNda.length() == 0 ? "</td><td>n/a" 
									: englishNda.equals(nda) ? "</td><td class='i'>==" 
											: "</td><td class='e'>" + TransliteratorUtilities.toHTML.transliterate(englishNda))
							
							+ "</td></tr>");
				}
				pw.println("</table>");
				pw.println("<p class='a'>Aliased items: " + aliasedCount + "</p>");
				pw.println("<p class='h'>Inherited items: " + inheritedCount + "</p>");
				if (skippedCount != 0) pw.println("<p>Fallback items skipped: " + skippedCount + "</p>");
				pw.println("</body></html>");
				pw.close();
			}
        } finally{
            System.out.println("Done");
        }
	}

	private static void showLinks(PrintWriter pw, String locale) {
		String parent = LocaleIDParser.getParent(locale);
		if (parent != null) {
			showLinks(pw, parent);
			pw.print("&gt; ");
		}
		showLocale(pw, locale);
	}

	private static void showChildren(PrintWriter pw, String locale) {
		boolean first = true;
		for (Iterator it = cldrFactory.getAvailableWithParent(locale, true).iterator(); it.hasNext();) {
			String possible = (String)it.next();
			if (possible.startsWith("supplem") || possible.startsWith("character")) continue;
			if (LocaleIDParser.getParent(possible).equals(locale)) {
				if (first) {
					first = false;
					pw.print("&gt; ");
				} else {
					pw.print(" | ");
				}
				showLocale(pw, possible);
			}
		}
	}

	private static void showLocale(PrintWriter pw, String locale) {	
		pw.println("<a href='" + locale + ".html'>" + getName(locale) + "</a>");
	}

	private static String getName(String locale) {
		String name = english.getName(locale, false);
		return locale + " [" + name + "]";
	}
}