/*
 ******************************************************************************
 * Copyright (C) 2005, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
*/

package org.unicode.cldr.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CoverageLevel.Level;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.PrettyPath;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.impl.CollectionUtilities;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.dev.test.util.ElapsedTimer;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.dev.tool.UOption;

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
	
	static boolean SHOW_LOCALE = false;
    static boolean SHOW_EXAMPLES = false;
    public static boolean SHOW_TIMES = false;
    public static boolean showStackTrace = false;
    
	/**
	 * Here is where the list of all checks is found. 
	 * @param nameMatcher Regex pattern that determines which checks are run,
	 * based on their class name (such as .* for all checks, .*Collisions.* for CheckDisplayCollisions, etc.)
	 * @return
	 */
	public static CompoundCheckCLDR getCheckAll(String nameMatcher) {
		return new CompoundCheckCLDR()
			.setFilter(Pattern.compile(nameMatcher,Pattern.CASE_INSENSITIVE).matcher(""))
			.add(new CheckAttributeValues())
			//.add(new CheckChildren()) // don't enable this; will do in code.
            .add(new CheckCoverage())
			.add(new CheckDates())
			.add(new CheckDisplayCollisions())
			.add(new CheckExemplars())
			.add(new CheckForExemplars())
            .add(new CheckNew())
			.add(new CheckNumbers())
            .add(new CheckZones())
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
	
    private static final int
    HELP1 = 0,
    HELP2 = 1,
    COVERAGE = 2,
    EXAMPLES = 3,
    FILE_FILTER = 4,
    TEST_FILTER = 5,
    DATE_FORMATS = 6,
    ORGANIZATION = 7,
    SHOWALL = 8,
    PRETTY = 9
    ;

    private static final UOption[] options = {
        UOption.HELP_H(),
        UOption.HELP_QUESTION_MARK(),
        UOption.create("coverage", 'c', UOption.REQUIRES_ARG),
        UOption.create("examples", 'e', UOption.NO_ARG),
        UOption.create("file_filter", 'f', UOption.REQUIRES_ARG).setDefault(".*"),
        UOption.create("test_filter", 't', UOption.REQUIRES_ARG).setDefault(".*"),
        UOption.create("date_formats", 'd', UOption.NO_ARG),
        UOption.create("organization", 'o', UOption.REQUIRES_ARG),
        UOption.create("showall", 's', UOption.NO_ARG),
        UOption.create("pretty", 'p', UOption.NO_ARG),
    };
    
    private static String[] HelpMessage = {
    	"-h \t This message",
    	"-fxxx \t Pick the locales (files) to check: xxx is a regular expression, eg -ffr, or -ffr.*, or -f(fr|en-.*)",
    	"-cxxx \t Set the coverage: eg -ccomprehensive or -cmodern or -cmoderate or -cbasic",
    	"-txxx \t Filter the Checks: xxx is a regular expression, eg -t.*number.*",
    	"-oxxx \t Organization (for coverage tests): ibm, google, ....",
    	"-e \t Turn on examples (actually a summary of the demo)",
    	"-d \t Turn on special date format checks",
    	"-s \t Show all paths",
    	};

	/**
	 * This will be the test framework way of using these tests. It is preliminary for now.
	 * The Survey Tool will call setDisplayInformation, and getCheckAll.
	 * For each cldrfile, it will set the cldrFile.
	 * Then on each path in the file it will call check.
	 * Right now it doesn't work with resolved files, so just use unresolved ones.
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
        double deltaTime = System.currentTimeMillis();
        UOption.parseArgs(args, options);
        if (options[HELP1].doesOccur || options[HELP2].doesOccur) {
        	for (int i = 0; i < HelpMessage.length; ++i) {
        		System.out.println(HelpMessage[i]);
        	}
        	return;
        }
        String factoryFilter = options[FILE_FILTER].value; 
        String checkFilter = options[TEST_FILTER].value; 
        
        SHOW_EXAMPLES = options[EXAMPLES].doesOccur; // eg .*Collision.* 
        boolean showAll = options[SHOWALL].doesOccur; 
        boolean checkFlexibleDates = options[DATE_FORMATS].doesOccur; 
        boolean pretty = options[PRETTY].doesOccur; 
        
        Level coverageLevel = null;
        String coverageLevelInput = options[COVERAGE].value;
        if (coverageLevelInput != null) coverageLevel = Level.get(coverageLevelInput);
        
        String organization = options[ORGANIZATION].value;
        if (organization != null) {
	        StandardCodes sc = StandardCodes.make();
	        Map foo = sc.getLocaleTypes();
	        if (!foo.keySet().contains(organization)) {
	        	throw new IllegalArgumentException("-o" + organization + "\t is invalid: must be one of: " + foo.keySet());
	        }
        }
        
        // check stuff
//        Comparator cc = StandardCodes.make().getTZIDComparator();
//        System.out.println(cc.compare("Antarctica/Rothera", "America/Cordoba"));
//        System.out.println(cc.compare("Antarctica/Rothera", "America/Indianapolis"));


        System.out.println("factoryFilter: " + factoryFilter);
        System.out.println("test filter: " + checkFilter);
        System.out.println("organization: " + organization);
        System.out.println("show examples: " + SHOW_EXAMPLES);
        System.out.println("coverage level: " + coverageLevel);
        System.out.println("checking dates: " + checkFlexibleDates);
        System.out.println("show all: " + showAll);
        
        // set up the test
		Factory cldrFactory = CLDRFile.Factory.make(Utility.MAIN_DIRECTORY, factoryFilter);
		CheckCLDR checkCldr = getCheckAll(checkFilter);
		setDisplayInformation(cldrFactory.make("en", true));
		PathShower pathShower = new PathShower();
		
		// call on the files
		Set locales = cldrFactory.getAvailable();
		List result = new ArrayList();
		Set paths = new TreeSet(CLDRFile.ldmlComparator);
		Map m = new TreeMap();
		double testNumber = 0;
        Map options = new HashMap();
        Counter totalCount = new Counter();
        Counter subtotalCount = new Counter();
        FlexibleDateFromCLDR fset = new FlexibleDateFromCLDR();
        
		for (Iterator it = locales.iterator(); it.hasNext();) {
			String localeID = (String) it.next();
            if (CLDRFile.isSupplementalName(localeID)) continue;
			if (SHOW_LOCALE) System.out.println("Locale:\t" + getLocaleAndName(localeID) + "\t");
            boolean onlyLanguageLocale = localeID.equals(new LocaleIDParser().set(localeID).getLanguageScript());
            options.clear();
            if (!onlyLanguageLocale) options.put("CheckCoverage.skip","true");
            if (coverageLevel != null) options.put("CheckCoverage.requiredLevel", coverageLevel.toString());
            if (organization != null) options.put("CoverageLevel.localeType", organization);
            
            //options.put("CheckCoverage.requiredLevel","comprehensive");

			CLDRFile file = cldrFactory.make(localeID, onlyLanguageLocale);
			checkCldr.setCldrFileToCheck(file, options, result);
			for (Iterator it3 = result.iterator(); it3.hasNext();) {
				System.out.print("Locale:\t" + getLocaleAndName(localeID) + "\t");
				System.out.println(it3.next().toString());
			}
			paths.clear();
			CollectionUtilities.addAll(file.iterator(), paths);
			UnicodeSet missingExemplars = new UnicodeSet();
            subtotalCount.clear();
            if (checkFlexibleDates) {
                fset.set(file);
            }
            pathShower.set(localeID);
            if (pretty) {
            	System.out.println("Showing Pretty Paths");
            	Map prettyMap = new TreeMap();
            	Set prettySet = new TreeSet();
            	for (Iterator it2 = paths.iterator(); it2.hasNext();) {
            		String path = (String)it2.next();
            		String prettyString = prettyPath.transliterate(path);
            		if (prettyString.indexOf("%%") >= 0) prettyString = "unmatched/" + prettyString;
            		Object old = prettyMap.get(prettyString);
            		if (old != null) {
            			System.out.println("Collision with: ");
            			System.out.println("\t" + prettyString);
            			System.out.println("\t\t" + path);
            			System.out.println("\t\t" + old);
            		}
            		prettyMap.put(prettyString, path);
            		String cleanPath = prettyString;
            		int last = prettyString.lastIndexOf('|');
            		if (last >= 0) cleanPath = cleanPath.substring(0,last);
            		prettySet.add(cleanPath);
            		System.out.println(prettyString + " => " + path);
            	}
            	System.out.println("Showing Structure");
            	String oldSplit = pathShower.getSplitChar();
            	pathShower.setSplitChar("\\|");
            	for (Iterator it2 = prettyMap.keySet().iterator(); it2.hasNext();) {
            		String prettyString = (String) it2.next();
            		String path = (String) prettyMap.get(prettyString);
            		pathShower.showHeader(prettyString, file.getStringValue(path));
            	}
            	System.out.println("Showing Non-Leaves");
            	pathShower.setSplitChar(oldSplit);
            	for (Iterator it2 = prettySet.iterator(); it2.hasNext();) {
            		String prettyString = (String) it2.next();
            		System.out.println(prettyString);
            	}
            	System.out.println("Done Showing Pretty Paths");
            	return;
            }
            
			for (Iterator it2 = paths.iterator(); it2.hasNext();) {

				String path = (String) it2.next();
				String value = file.getStringValue(path);
				String fullPath = file.getFullXPath(path);
                
                if (checkFlexibleDates) {
                    fset.checkFlexibles(path, value, fullPath);
                }

				int limit = 1;
				if (SHOW_EXAMPLES) limit = 2;
				for (int jj = 0; jj < limit; ++jj) {
					if (jj == 0) {
						checkCldr.check(path, fullPath, value, options, result);
					} else {
						checkCldr.getExamples(path, fullPath, value, options, result);
					}
					
					if (showAll) pathShower.showHeader(path, value);
					
					for (Iterator it3 = result.iterator(); it3.hasNext();) {
						CheckStatus status = (CheckStatus) it3.next();
						String statusString = status.toString(); // com.ibm.icu.impl.Utility.escape(
						String statusType = status.getType();
						pathShower.showHeader(path, value);
						
						//System.out.print("Locale:\t" + getLocaleAndName(localeID) + "\t");
						if (statusType.equals(status.demoType)) {
							SimpleDemo d = status.getDemo();
							if (d != null && d instanceof FormatDemo) {
								FormatDemo fd = (FormatDemo)d;
								m.clear();
								m.put("pattern", fd.getPattern());
								m.put("input", fd.getRandomInput());
								if (d.processPost(m)) System.out.println("\tDemo:\t" + m);
							}
							continue;
						}
						System.out.println("\t" + statusString);
						subtotalCount.add(status.type, 1);
						totalCount.add(status.type, 1);
						Object[] parameters = status.getParameters();
						if (parameters != null) for (int i = 0; i < parameters.length; ++i) {
							if (showStackTrace && parameters[i] instanceof Throwable) {
								((Throwable)parameters[i]).printStackTrace();
							}
							if (status.getMessage().startsWith("Not in exemplars")) {
								missingExemplars.addAll(new UnicodeSet(parameters[i].toString()));
							}
						}
						// survey tool will use: if (status.hasHTMLMessage()) System.out.println(status.getHTMLMessage());
					}
				}
			}
			if (missingExemplars.size() != 0) {
				System.out.print("Locale:\t" + getLocaleAndName(localeID) + "\t");
				System.out.println("Total missing:\t" + missingExemplars);
			}
            for (Iterator it2 = new TreeSet(subtotalCount.keySet()).iterator(); it2.hasNext();) {
                String type = (String)it2.next();
                System.out.println("Locale:\t" + getLocaleAndName(localeID) + "\tSubtotal " + type + ":\t" + subtotalCount.getCount(type));
            }
            if (checkFlexibleDates) {
                fset.showFlexibles();
            }
		}
        for (Iterator it2 = new TreeSet(totalCount.keySet()).iterator(); it2.hasNext();) {
            String type = (String)it2.next();
            System.out.println("Total " + type + ":\t" + totalCount.getCount(type));
        }
		
        deltaTime = System.currentTimeMillis() - deltaTime;
        System.out.println("Elapsed: " + deltaTime/1000.0 + " seconds");
	}

	static class PathShower {
		String localeID;
		boolean newLocale = true;
		String lastPath;
		String[] lastSplitPath;
		boolean showEnglish;
		String splitChar = "/";

		static String lead = "****************************************";
		
		void set(String localeID) {
			this.localeID = localeID;
			newLocale = true;
			showEnglish = localeID.equals(CheckCLDR.displayInformation.getLocaleID());
		}
		
		private void showHeader(String path, String value) {
			if (newLocale) {
				System.out.println("Locale:\t" + getLocaleAndName(localeID));
				newLocale = false;
			}
			if (path.equals(lastPath)) return;
			String[] splitPath = path.split(splitChar);
			
			for (int i = 0; i < splitPath.length; ++i) {
				if (lastSplitPath != null && i < lastSplitPath.length && splitPath[i].equals(lastSplitPath[i])) {
					continue;
				}
				lastSplitPath = null; // mark so we continue printing now
				System.out.print(lead.substring(0,i));
				System.out.print(splitPath[i]);
				if (i == splitPath.length - 1) {
					System.out.print("\tValue:\t" + value);				
					if (showEnglish) {
						System.out.print("\tEnglish Value: " + CheckCLDR.displayInformation.getStringValue(path));	
					}
				} else {
					System.out.print(":");
				}
				System.out.println();				
			}
//			String prettierPath = path;
//			if (false) {
//				prettierPath = prettyPath.transliterate(path);
//			}
			
			lastPath = path;
			lastSplitPath = splitPath;
		}

		public String getSplitChar() {
			return splitChar;
		}

		public PathShower setSplitChar(String splitChar) {
			this.splitChar = splitChar;
			return this;
		}
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
		public static final String 
			alertType = "Comment", 
            warningType = "Warning", 
            errorType = "Error", 
            exampleType = "Example",
            demoType = "Demo";
		private String type;
		private String messageFormat;
		private Object[] parameters;
		private String htmlMessage;
        private CheckCLDR cause;
		
		public String getType() {
			return type;
		}
		public CheckStatus setType(String type) {
			this.type = type;
			return this;
		}
		public String getMessage() {
			if (messageFormat == null) return messageFormat;
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
		public CheckStatus setParameters(Object[] parameters) {
			this.parameters = parameters;
            return this;
		}
		public SimpleDemo getDemo() {
			return null;
		}
        public CheckCLDR getCause() {
            return cause;
        }
        public CheckStatus setCause(CheckCLDR cause) {
            this.cause = cause;
            return this;
        }
	}
	
	public static abstract class SimpleDemo {
		
		public abstract String getHTML(String path, String fullPath, String value) throws Exception;
		
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
    
    public static abstract class FormatDemo extends SimpleDemo {
        abstract String getPattern(); // just for testing
        abstract String getRandomInput(); // just for testing
        /**
         * @param htmlMessage
         * @param pattern
         * @param input
         * @param formatted
         * @param reparsed
         */
        public static void appendLine(StringBuffer htmlMessage, String pattern, String context, String input, String formatted, String reparsed) {
            htmlMessage.append("<tr><td><input type='text' name='pattern' readonly='true' value='")
            .append(TransliteratorUtilities.toXML.transliterate(pattern))
            .append("'></td><td><input type='text' name='context' readonly='true' value='")
            .append(TransliteratorUtilities.toXML.transliterate(context))
            .append("'></td><td><input type='text' name='input' value='")
            .append(TransliteratorUtilities.toXML.transliterate(input))
            .append("'></td><td>")
            .append("<input type='button' value='Test' name='Test'>")
            .append("</td><td>" + "<input type='text' name='formatted' value='")
            .append(TransliteratorUtilities.toXML.transliterate(formatted))
            .append("'></td><td>" + "<input type='text' name='reparsed' value='")
            .append(TransliteratorUtilities.toXML.transliterate(reparsed))
            .append("'></td></tr>");
        }

        /**
         * @param htmlMessage
         */
        public static void appendTitle(StringBuffer htmlMessage) {
            htmlMessage.append("<table border='1' cellspacing='0' cellpadding='2'" +
                    //" style='border-collapse: collapse' style='width: 100%'" +
                    "><tr>" +
                    "<th>Pattern</th>" +
                    "<th>Context</th>" +
                    "<th>Unlocalized Input</th>" +
                    "<th></th>" +
                    "<th>Localized Format</th>" +
                    "<th>Re-Parsed</th>" +
                    "</tr>");
        }
    }

	/**
	 * Checks the path/value in the cldrFileToCheck for correctness, according to some criterion.
	 * If the path is relevant to the check, there is an alert or warning, then a CheckStatus is added to List.
	 * @param path Must be a distinguished path, such as what comes out of CLDRFile.iterator()
	 * @param fullPath Must be the full path
	 * @param value the value associated with the path
	 * @param options A set of test-specific options. Set these with code of the form:<br>
	 * options.put("CoverageLevel.localeType", "G0")<br>
	 * That is, use <testname>.<optiontype>, <optionvalue>)<br>
	 * There is one general option; the following will cause the tests that depend on the rest of the CLDRFile to be abbreviated.<br>
	 * options.put("submission", "true") // actually, any value will work, not just "true". Remove "submission" to restore it.
	 * It can be used for new data entry.
	 * @param result
	 */
	public final CheckCLDR check(String path, String fullPath, String value, Map options, List result) {
        if(cldrFileToCheck == null) {
            throw new InternalError("CheckCLDR problem: cldrFileToCheck must not be null");
        }
		if (path == null) {
			throw new InternalError("CheckCLDR problem: path must not be null");
		}
		if (fullPath == null) {
			throw new InternalError("CheckCLDR problem: fullPath must not be null");
		}
		if (value == null) {
			throw new InternalError("CheckCLDR problem: value must not be null");
		}
		result.clear();
		return handleCheck(path, fullPath, value, options, result);
	}
	

	/**
	 * Returns any examples in the result parameter. Both examples and demos can
	 * be returned. A demo will have getType() == CheckStatus.demoType. In that
	 * case, there will be no getMessage or getHTMLMessage available; instead,
	 * call getDemo() to get the demo, then call getHTML() to get the initial
	 * HTML.
	 */ 
	public final CheckCLDR getExamples(String path, String fullPath, String value, Map options, List result) {
		result.clear();
		return handleGetExamples(path, fullPath, value, options, result);		
	}


	protected CheckCLDR handleGetExamples(String path, String fullPath, String value, Map options2, List result) {
		return this; // NOOP unless overridden
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

		public CompoundCheckCLDR add(CheckCLDR item) {
			checkList.add(item);
			if (filter == null || filter.reset(item.getClass().getName()).matches()) {
				filteredCheckList.add(item);
			}
			return this;
		}
		public CheckCLDR handleCheck(String path, String fullPath, String value,
				Map options, List result) {
			result.clear();
			for (Iterator it = filteredCheckList.iterator(); it.hasNext(); ) {
				CheckCLDR item = (CheckCLDR) it.next();
				try {
					item.handleCheck(path, fullPath, value, options, result);
				} catch (Exception e) {
			    	addError(result, item, e);
			    	return this;
				}
			}
			return this;
		}

		protected CheckCLDR handleGetExamples(String path, String fullPath, String value, Map options, List result) {
			result.clear();
			for (Iterator it = filteredCheckList.iterator(); it.hasNext(); ) {
				CheckCLDR item = (CheckCLDR) it.next();
				try {
					item.handleGetExamples(path, fullPath, value, options, result);
				} catch (Exception e) {
			    	addError(result, item, e);
			    	return this;
				}
			}
			return this;
		}

		private void addError(List result, CheckCLDR item, Exception e) {
            result.add(new CheckStatus().setType(CheckStatus.errorType)
                    .setMessage("Internal error in {0}. Exception: {1}, Message: {2}, Trace: {3}", 
                            new Object[]{item.getClass().getName(), e.getClass().getName(), e, 
                                Arrays.asList(e.getStackTrace())}));
        }
		public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Map options, List possibleErrors) {
            ElapsedTimer testTime = null;
	  		if (cldrFileToCheck == null) return this;
	  		super.setCldrFileToCheck(cldrFileToCheck,options,possibleErrors);
			possibleErrors.clear();
			for (Iterator it = filteredCheckList.iterator(); it.hasNext(); ) {
				CheckCLDR item = (CheckCLDR) it.next();
                if(SHOW_TIMES) testTime = new ElapsedTimer("Test setup time for " + item.getClass().toString() + " {0}");
				try {
					item.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
                    if(SHOW_TIMES) System.err.println("OK : " + testTime);
				} catch (RuntimeException e) {
                    addError(possibleErrors, item, e);
                    if(SHOW_TIMES) System.err.println("ERR: " + testTime + " - " + e.toString());
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
					item.setCldrFileToCheck(getCldrFileToCheck(), null, null);
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
	
	static Transliterator prettyPath = getTransliteratorFromFile("ID", "prettyPath.txt");
	
	public static Transliterator getTransliteratorFromFile(String ID, String file) {
		try {
			BufferedReader br = Utility.getUTF8Data("prettyPath.txt");
			StringBuffer input = new StringBuffer();
			while (true) {
				String line = br.readLine();
				if (line == null) break;
				if (line.startsWith("\uFEFF")) line = line.substring(1); // remove BOM
				input.append(line);
				input.append('\n');
			}
			return Transliterator.createFromRules(ID, input.toString(), Transliterator.FORWARD);
		} catch (IOException e) {
			return null;
		}
	}
}
