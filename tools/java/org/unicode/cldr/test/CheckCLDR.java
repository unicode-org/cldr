/*
 ******************************************************************************
 * Copyright (C) 2005, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
*/

package org.unicode.cldr.test;

import java.text.FieldPosition;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.impl.CollectionUtilities;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.UnicodeSet;

/**
 * This class provides a foundation for both console-driven CLDR tests, and Survey Tool Tests.
 * <p>To add a test, subclass CLDRFile and override handleCheck and possibly setCldrFileToCheck.
 * Then put the test into getCheckAll.
 * <p>To use the test, take a look at the main below. Note that you need to call setDisplayInformation
 * with the CLDRFile for the locale that you want the display information (eg names for codes) to be in.
 * <p>TODO
 * <br>add CheckCoverage
 * <br>add CheckAttributeValue
 * @author davis
 */
abstract public class CheckCLDR {
	private CLDRFile cldrFileToCheck;
	private CLDRFile resolvedCldrFileToCheck;
	private static CLDRFile displayInformation;
	
	static boolean SHOW_LOCALE = true;
    static boolean SHOW_EXAMPLES = false;
	
	/**
	 * Here is where the list of all checks is found. 
	 * @param nameMatcher Regex pattern that determines which checks are run,
	 * based on their class name (such as .* for all checks, .*Collisions.* for CheckDisplayCollisions, etc.)
	 * @return
	 */
	public static CompoundCheckCLDR getCheckAll(String nameMatcher) {
		return new CompoundCheckCLDR()
			.setFilter(Pattern.compile(nameMatcher).matcher(""))
			.add(new CheckForExemplars())
			.add(new CheckDisplayCollisions())
			.add(new CheckExemplars())
			.add(new CheckNumbers())
			.add(new CheckChildren())
			.add(new CheckAttributeValues())
			.add(new CheckDates())
			.add(new CheckCoverage())
		;
	}
	
	/**
	 * These determine what language is used to display information. Must be set before use.
	 * @param locale
	 * @return
	 */
	public static CLDRFile getDisplayInformation() {
		return displayInformation;
	}
	public static void setDisplayInformation(CLDRFile displayInformation) {
		CheckCLDR.displayInformation = displayInformation;
	}
	
	/**
	 * This will be the test framework way of using these tests. It is preliminary for now.
	 * The Survey Tool will call setDisplayInformation, and getCheckAll.
	 * For each cldrfile, it will set the cldrFile.
	 * Then on each path in the file it will call check.
	 * Right now it doesn't work with resolved files, so just use unresolved ones.
	 * @param args
	 */
	public static void main(String[] args) {
        double deltaTime = System.currentTimeMillis();
        String factoryFilter = args.length <= 0 ? ".*" : args[0]; // eg de.*
        String checkFilter = args.length <= 1 ? ".*" : args[1]; // eg .*Collision.* 
        System.out.println("factoryFilter: " + factoryFilter);
        System.out.println("checkFilter: " + checkFilter);
        
        // set up the test
		Factory cldrFactory = CLDRFile.Factory.make(Utility.MAIN_DIRECTORY, factoryFilter);
		CheckCLDR checkCldr = getCheckAll(checkFilter);
		setDisplayInformation(cldrFactory.make("en", true));
		
		// call on the files
		Set locales = cldrFactory.getAvailable();
		List result = new ArrayList();
		XPathParts pathParts = new XPathParts(null, null);
		XPathParts fullPathParts = new XPathParts(null, null);
		Set paths = new TreeSet(CLDRFile.ldmlComparator);
		Map m = new TreeMap();
		double testNumber = 0;
		for (Iterator it = locales.iterator(); it.hasNext();) {
			String localeID = (String) it.next();
			if (SHOW_LOCALE) System.out.println("Locale:\t" + getLocaleAndName(localeID) + "\t");
			CLDRFile file = cldrFactory.make(localeID, true);
			checkCldr.setCldrFileToCheck(file, null, result);
			for (Iterator it3 = result.iterator(); it3.hasNext();) {
				System.out.print("Locale:\t" + getLocaleAndName(localeID) + "\t");
				System.out.println(it3.next().toString());
			}
			paths.clear();
			CollectionUtilities.addAll(file.iterator(), paths);
			UnicodeSet missingExemplars = new UnicodeSet();
			for (Iterator it2 = paths.iterator(); it2.hasNext();) {
				String path = (String) it2.next();
				String value = file.getStringValue(path);
				String fullPath = file.getFullXPath(path);
				checkCldr.check(path, fullPath, value, pathParts, fullPathParts, result);
				for (Iterator it3 = result.iterator(); it3.hasNext();) {
					CheckStatus status = (CheckStatus) it3.next();
					if (status.getType().equals(status.exampleType)) {
						if (!SHOW_EXAMPLES) continue;
						System.out.print("Locale:\t" + getLocaleAndName(localeID) + "\t");
						System.out.println("\t" + status);
						System.out.print("Locale:\t" + getLocaleAndName(localeID) + "\t");
						System.out.println(status.getHTMLMessage());
						SimpleDemo d = status.getDemo();
						if (d != null) {
							m.clear();
							// for now, assume CheckNumber
							m.put("T1", String.valueOf(testNumber += Math.PI));
							if (d.processPost(m)) System.out.println(m);
						}
						continue;
					}
					String statusString = status.toString(); // com.ibm.icu.impl.Utility.escape(
					System.out.print("Locale:\t" + getLocaleAndName(localeID) + "\t");
					System.out.println("Value: " + value + "\t Full Path: " + fullPath);
					System.out.print("Locale:\t" + getLocaleAndName(localeID) + "\t");
					System.out.println("\t" + statusString);
					Object[] parameters = status.getParameters();
					if (parameters != null) for (int i = 0; i < parameters.length; ++i) {
						if (parameters[i] instanceof Throwable) {
							((Throwable)parameters[i]).printStackTrace();
						}
						if (status.getMessage().startsWith("Not in exemplars")) {
							missingExemplars.addAll(new UnicodeSet(parameters[i].toString()));
						}
					}
					// survey tool will use: if (status.hasHTMLMessage()) System.out.println(status.getHTMLMessage());
				}
			}
			if (missingExemplars.size() != 0) {
				System.out.print("Locale:\t" + getLocaleAndName(localeID) + "\t");
				System.out.println("Total missing:\t" + missingExemplars);
			}
		}
		
        deltaTime = System.currentTimeMillis() - deltaTime;
        System.out.println("Elapsed: " + deltaTime/60000 + " minutes");
	}
	/**
	 * Get the CLDRFile.
	 * @param cldrFileToCheck
	 */
	public final CLDRFile getCldrFileToCheck() {
		return cldrFileToCheck;
	}

	public final CLDRFile getResolvedCldrFileToCheck() {
		if (resolvedCldrFileToCheck == null) resolvedCldrFileToCheck = cldrFileToCheck.getResolved();
		return resolvedCldrFileToCheck;
	}
	/**
	 * Set the CLDRFile. Must be done before calling check. If null is called, just skip
	 * Often subclassed for initializing. If so, make the first 2 lines:
	 * 		if (cldrFileToCheck == null) return this;
	 * 		super.setCldrFileToCheck(cldrFileToCheck);
	 * 		do stuff
	 * @param cldrFileToCheck
	 * @param options TODO
	 * @param possibleErrors TODO
	 */
	public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Map options, List possibleErrors) {
		this.cldrFileToCheck = cldrFileToCheck;
		resolvedCldrFileToCheck = null;
		return this;
	}
	/**
	 * Status value returned from check
	 */
	public static class CheckStatus {
		public static final String alertType = "Comment", 
            warningType = "Warning", 
            errorType = "Error", 
            exampleType = "Example";
		private String type;
		private String messageFormat;
		private Object[] parameters;
		private String htmlMessage;
		
		public String getType() {
			return type;
		}
		public CheckStatus setType(String type) {
			this.type = type;
			return this;
		}
		public String getMessage() {
			return MessageFormat.format(MessageFormat.autoQuoteApostrophe(messageFormat), parameters);
		}
		/*
		 * If this is not null, wrap it in a <form>...</form> and display. When you get a submit, call getDemo
		 * to get a demo that you can call to change values of the fields. See CheckNumbers for an example. 
		 */
		public String getHTMLMessage() {
			return htmlMessage;
		}
		public CheckStatus setHTMLMessage(String message) {
			htmlMessage = message;
			return this;
		}
		public CheckStatus setMessage(String message) {
			this.messageFormat = message;
			return this;
		}
		public CheckStatus setMessage(String message, Object[] messageArguments) {
			this.messageFormat = message;
			this.parameters = messageArguments;
			return this;
		}
		public String toString() {
			return getType() + ": " + getMessage();
		}
		/**
		 * Warning: don't change the contents of the parameters after retrieving.
		 */
		public Object[] getParameters() {
			return parameters;
		}
		/**
		 * Warning: don't change the contents of the parameters after passing in.
		 */
		public void setParameters(Object[] parameters) {
			this.parameters = parameters;
		}
		public SimpleDemo getDemo() {
			return null;
		}
	}
	
	public static class SimpleDemo {
		/**
		 * If the getHTMLMessage is not null, then call this in response to a submit.
		 * @param PostArguments A read-write map containing post-style arguments. eg TEXTBOX=abcd, etc.
		 * @return true if the map has been changed
		 */ 
		public boolean processPost(Map PostArguments) {
			return false;
		}
		/**
		 * Utility for setting map. Use the paradigm in CheckNumbers.
		 */
		public boolean putIfDifferent(Map inout, String key, String value) {
			Object oldValue = inout.put(key, value);
			return !value.equals(oldValue);
		}
	}

	/**
	 * Checks the path/value in the cldrFileToCheck for correctness, according to some criterion.
	 * If the path is relevant to the check, there is an alert or warning, then a CheckStatus is added to List.
	 * @param path
	 * @param result
	 */
	public final CheckCLDR check(String path, String fullPath, String value,
			XPathParts pathParts, XPathParts fullPathParts, List result) {
		if (path == null || value == null || fullPath == null) {
			throw new InternalError("XMLSource problem: path, value, fullpath must not be null");
		}
		pathParts.clear();
		fullPathParts.clear();
		result.clear();
		return handleCheck(path, fullPath, value, null, result);
	}

	/**
	 * This is what the subclasses override.
	 * If they ever use pathParts or fullPathParts, they need to call initialize() with the respective
	 * path. Otherwise they must NOT change pathParts or fullPathParts.
	 * <p>If something is found, a CheckStatus is added to result. This can be done multiple times in one call,
	 * if multiple errors or warnings are found. The CheckStatus may return warnings, errors,
	 * examples, or demos. We may expand that in the future.
	 * <p>The code to add the CheckStatus will look something like::
	 * <pre> result.add(new CheckStatus()
	 * 		.setType(CheckStatus.errorType)
	 *		.setMessage("Value should be {0}", new Object[]{pattern}));				
	 * </pre>
	 * @param options TODO
	 */
	abstract public CheckCLDR handleCheck(String path, String fullPath, String value,
			Map options, List result);
	
	/**
	 * Internal class used to bundle up a number of Checks.
	 * @author davis
	 *
	 */
	static class CompoundCheckCLDR extends CheckCLDR {
		private Matcher filter;
		private List checkList = new ArrayList();
		private List filteredCheckList = new ArrayList();
		private CLDRFile cldrFileToCheck;
		
		public CompoundCheckCLDR add(CheckCLDR item) {
			checkList.add(item);
			if (filter == null || filter.reset(item.getClass().getName()).matches()) {
				filteredCheckList.add(item);
				item.setCldrFileToCheck(cldrFileToCheck, null, null);
			}
			return this;
		}
		public CheckCLDR handleCheck(String path, String fullPath, String value,
				Map options, List result) {
			result.clear();
			for (Iterator it = filteredCheckList.iterator(); it.hasNext(); ) {
				CheckCLDR item = (CheckCLDR) it.next();
				try {
					item.handleCheck(path, fullPath, value, null, result);
				} catch (Exception e) {
					e.printStackTrace();
			    	CheckStatus status = new CheckStatus().setType(CheckStatus.errorType)
			    	.setMessage("Internal error in {0}. Exception: {1}, Message: {2}", 
			    			new Object[]{item.getClass().getName(), e.getClass().getName(), e});
			    	result.add(status);
			    	return this;
				}
			}
			return this;
		}
		public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Map options, List possibleErrors) {
			possibleErrors.clear();
			for (Iterator it = filteredCheckList.iterator(); it.hasNext(); ) {
				CheckCLDR item = (CheckCLDR) it.next();
				try {
					item.setCldrFileToCheck(cldrFileToCheck, null, possibleErrors);
				} catch (RuntimeException e) {
			    	CheckStatus status = new CheckStatus().setType(CheckStatus.warningType)
			    	.setMessage("Internal error in {0}. Exception: {1}, Message: {2}", 
			    			new Object[]{item.getClass().getName(), e.getClass().getName(), e.getMessage()});
			    	possibleErrors.add(status);
			    	return this;
				}
			}
			return this;
		}
		public Matcher getFilter() {
			return filter;
		}

		public CompoundCheckCLDR setFilter(Matcher filter) {
			this.filter = filter;
			filteredCheckList.clear();
			for (Iterator it = checkList.iterator(); it.hasNext(); ) {
				CheckCLDR item = (CheckCLDR) it.next();
				if (filter == null || filter.reset(item.getClass().getName()).matches()) {
					filteredCheckList.add(item);
					item.setCldrFileToCheck(cldrFileToCheck, null, null);
				}
			}
			return this;
		}
	}
	
	/**
	 * Utility for getting information.
	 * @param locale
	 * @return
	 */
	public static String getLocaleAndName(String locale) {
		String localizedName = displayInformation.getName(locale, false);
		if (localizedName == null || localizedName.equals(locale)) return locale;
		return locale + " [" + localizedName + "]";
	}
}
