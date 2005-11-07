package org.unicode.cldr.test;

import java.text.FieldPosition;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Utility;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.CLDRFile.Factory;

import com.ibm.icu.text.MessageFormat;

abstract public class CheckCLDR {
	private CLDRFile cldrFileToCheck;
	private CLDRFile resolvedCldrFileToCheck;
	private static CLDRFile displayInformation;
	
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
		for (Iterator it = locales.iterator(); it.hasNext();) {
			String localeID = (String) it.next();
			System.out.println(getLocaleAndName(localeID));
			CLDRFile file = cldrFactory.make(localeID, false);
			checkCldr.setCldrFileToCheck(file, result);
			for (Iterator it3 = result.iterator(); it3.hasNext();) {
				System.out.println(it3.next().toString());
			}
			paths.clear();
			paths.addAll(file.keySet());
			for (Iterator it2 = paths.iterator(); it2.hasNext();) {
				String path = (String) it2.next();
				String value = file.getStringValue(path);
				String fullPath = file.getFullXPath(path);
				checkCldr.check(path, fullPath, value, pathParts, fullPathParts, result);
				for (Iterator it3 = result.iterator(); it3.hasNext();) {
					CheckStatus status = (CheckStatus) it3.next();
					String statusString = status.toString(); // com.ibm.icu.impl.Utility.escape(
					System.out.println("Value: " + value + "\t Full Path: " + fullPath);
					System.out.println("\t" + statusString);
					Object[] parameters = status.getParameters();
					for (int i = 0; i < parameters.length; ++i) {
						if (parameters[i] instanceof Throwable) {
							((Throwable)parameters[i]).printStackTrace();
						}
					}
					// survey tool will use: if (status.hasHTMLMessage()) System.out.println(status.getHTMLMessage());
				}
			}
		}
		
        deltaTime = System.currentTimeMillis() - deltaTime;
        System.out.println("Elapsed: " + deltaTime/1000);
	}
	/**
	 * Get the CLDRFile.
	 * @param cldrFileToCheck
	 */
	public CLDRFile getCldrFileToCheck() {
		return cldrFileToCheck;
	}

	public CLDRFile getResolvedCldrFileToCheck() {
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
	 * @param possibleErrors TODO
	 */
	public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, List possibleErrors) {
		this.cldrFileToCheck = cldrFileToCheck;
		resolvedCldrFileToCheck = null;
		return this;
	}
	/**
	 * Status value returned from check
	 */
	static class CheckStatus {
		public static final String alertType = "Comment", warningType = "Warning", errorType = "Error", exampleType = "Example";
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
		public String getHTMLMessage() {
			if (htmlMessage == null) return getMessage();
			return htmlMessage;
		}
		public boolean hasHTMLMessage() {
			return (htmlMessage != null);
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
		public Object[] getParameters() {
			return (Object[]) parameters.clone();
		}
		public void setParameters(Object[] parameters) {
			this.parameters = parameters;
		}
	}
	/**
	 * Checks the path/value in the cldrFileToCheck for correctness, according to some criterion.
	 * If the path is relevant to the check, there is an alert or warning, then a CheckStatus is added to List.
	 * @param path
	 * @param result
	 */
	
	public CheckCLDR check(String path, String fullPath, String value,
			XPathParts pathParts, XPathParts fullPathParts, List result) {
		pathParts.clear();
		fullPathParts.clear();
		result.clear();
		return _check(path, fullPath, value, pathParts, fullPathParts, result);
	}

	/**
	 * This is what the subclasses override. If they ever use pathParts or fullPathParts, they need to call initialize() with the respective
	 * path. Otherwise they must NOT change pathParts or fullPathParts.
	 * If something is found, a CheckStatus is added to result.
	 */
	abstract public CheckCLDR _check(String path, String fullPath, String value,
			XPathParts pathParts, XPathParts fullPathParts, List result);
	
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
				item.setCldrFileToCheck(cldrFileToCheck, null);
			}
			return this;
		}
		public CheckCLDR _check(String path, String fullPath, String value,
				XPathParts pathParts, XPathParts fullPathParts, List result) {
			for (Iterator it = filteredCheckList.iterator(); it.hasNext(); ) {
				CheckCLDR item = (CheckCLDR) it.next();
				try {
					item._check(path, fullPath, value, pathParts, fullPathParts, result);
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
		public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, List possibleErrors) {
			for (Iterator it = filteredCheckList.iterator(); it.hasNext(); ) {
				CheckCLDR item = (CheckCLDR) it.next();
				try {
					item.setCldrFileToCheck(cldrFileToCheck, possibleErrors);
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
					item.setCldrFileToCheck(cldrFileToCheck, null);
				}
			}
			return this;
		}
	}
	
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
			.add(new CheckDates())
		;
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
