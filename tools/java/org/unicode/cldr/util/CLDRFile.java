/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Mark Davis
**********************************************************************
*/
package org.unicode.cldr.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.XMLReaderFactory;


//import javax.xml.parsers.*;

/**
 * This is a class that represents the contents of a CLDR file, as <key,value> pairs,
 * where the key is a "cleaned" xpath (with non-distinguishing attributes removed),
 * and the value is an object that contains the full
 * xpath plus a value, which is a string, or a node (the latter for atomic elements).
 * <p><b>WARNING: The API on this class is likely to change.</b> Having the full xpath on the value is clumsy;
 * I need to change it to having the key be an object that contains the full xpath, but then sorts as if
 * it were clean.
 * <p>Each instance also contains a set of associated comments for each xpath.
 * @author medavis
 */
/*
Notes:
http://xml.apache.org/xerces2-j/faq-grammars.html#faq-3
http://developers.sun.com/dev/coolstuff/xml/readme.html
http://lists.xml.org/archives/xml-dev/200007/msg00284.html
http://java.sun.com/j2se/1.4.2/docs/api/org/xml/sax/DTDHandler.html
 */
public class CLDRFile implements Lockable {
	public static boolean HACK_ORDER = false;
	private static boolean DEBUG_LOGGING = false;
	private static boolean SHOW_ALIAS_FIXES = false;
	
	public static final String SUPPLEMENTAL_NAME = "supplementalData";
	public static final String GEN_VERSION = "1.3";
    
    private Map xpath_value;
    private String key;
    private XPathParts.Comments xpath_comments = new XPathParts.Comments(); // map from paths to comments.
    private boolean isSupplemental;
    
    private CLDRFile(boolean isSupplemental){
    	this.isSupplemental = isSupplemental;
    	xpath_value = isSupplemental ? new TreeMap() : new TreeMap(ldmlComparator);
    }
	
    /**
     * Create a CLDRFile for the given localename. (Normally a Factory is used to create CLDRFiles.)
     * @param localeName
     */
    public static CLDRFile make(String localeName) {
    	CLDRFile result = new CLDRFile(localeName.equals(SUPPLEMENTAL_NAME));
		result.key = localeName;
		return result;
    }
    
    /**
     * Produce a CLDRFile from a localeName, given a directory. (Normally a Factory is used to create CLDRFiles.)
     * @param localeName
     * @param dir directory 
     */
    // TODO make the directory a URL
    public static CLDRFile make(String localeName, String dir) {
        String name = dir + localeName + ".xml";
        File f = new File(name);
        try {
        	name = f.getCanonicalPath();
            if (DEBUG_LOGGING) {
             	System.out.println("Parsing: " + name);
             	Log.logln("Parsing: " + name);
    	    }
			FileInputStream fis = new FileInputStream(f);
	    	CLDRFile result = make(name, localeName, fis);
			fis.close();
			return result;
		} catch (IOException e) {
			throw (IllegalArgumentException)new IllegalArgumentException("Can't read " + name).initCause(e);
		}
    }
    
    /**
     * Produce a CLDRFile from a file input stream. (Normally a Factory is used to create CLDRFiles.)
     * @param localeName
     * @param fis
     */
    public static CLDRFile make(String fileName, String localeName, InputStream fis) {
    	try {
    		fis = new StripUTF8BOMInputStream(fis);
    		CLDRFile result = make(localeName);
			MyDeclHandler DEFAULT_DECLHANDLER = new MyDeclHandler(result);
			XMLReader xmlReader = createXMLReader(true);
			xmlReader.setContentHandler(DEFAULT_DECLHANDLER);
			xmlReader.setErrorHandler(DEFAULT_DECLHANDLER);
			xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", DEFAULT_DECLHANDLER);
			xmlReader.setProperty("http://xml.org/sax/properties/declaration-handler", DEFAULT_DECLHANDLER);
			InputSource is = new InputSource(fis);
			is.setSystemId(fileName);
			xmlReader.parse(is);
			return result;
    	} catch (SAXParseException e) {
    		System.out.println(CLDRFile.showSAX(e));
    		throw (IllegalArgumentException)new IllegalArgumentException("Can't read " + localeName).initCause(e);
		} catch (SAXException e) {
			throw (IllegalArgumentException)new IllegalArgumentException("Can't read " + localeName).initCause(e);
		} catch (IOException e) {
			throw (IllegalArgumentException)new IllegalArgumentException("Can't read " + localeName).initCause(e);
		}    	 
    }
    
    static class StripUTF8BOMInputStream extends InputStream {
    	InputStream base;
    	StripUTF8BOMInputStream(InputStream base) {
    		this.base = base;
    	}
    	boolean checkForUTF8BOM = true;
		/* (non-Javadoc)
		 * @see java.io.InputStream#read()
		 */
		public int read() throws IOException {
			int result = base.read();
			if (!checkForUTF8BOM) return result;
			// complicated by still wanting to do one delegate read per read
			// so we just skip first char if it starts with EF, assuming valid UTF-8
			checkForUTF8BOM = false;
			if (result != 0xEF) return result;
			result = base.read();
			result = base.read();
			result = base.read();
			return result;
		}
    	
    }
    
    /**
     * Clone the object. Produces unlocked version (see Lockable).
     */
    public Object clone() {
    	try {
			CLDRFile result = (CLDRFile) super.clone();
			result.locked = false;
			result.xpath_value = (Map)((TreeMap)xpath_value).clone();
			result.xpath_comments = (XPathParts.Comments)xpath_comments.clone();
			return result;
		} catch (CloneNotSupportedException e) {
			throw new InternalError("should never happen");
		}
    }
	
    /**
     * Prints the contents of the file (the xpaths/values) to the console.
     *
     */
    public CLDRFile show() {
		for (Iterator it2 = xpath_value.keySet().iterator(); it2.hasNext();) {
			String xpath = (String)it2.next();
			Value v = (Value) xpath_value.get(xpath);
			System.out.println(v.getFullXPath() + " =>\t" + v.getStringValue());
		}
		return this;
    }

    static DateFormat myDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");  
    static {
        myDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

	/**
	 * Write the corresponding XML file out, with the normal formatting and indentation.
	 * Will update the identity element, including generation, version, and other items.
	 */
	public CLDRFile write(PrintWriter pw) {
		pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
		pw.println("<!DOCTYPE ldml SYSTEM \"http://www.unicode.org/cldr/dtd/" + GEN_VERSION + "/ldml.dtd\">");
		/*
<identity>
<version number="1.2"/>
<generation date="2004-08-27"/>
<language type="en"/>
		 */
		// if ldml has any attributes, get them.		
		String ldml_identity = "/ldml/identity";
		if (xpath_value.size() > 0) {
			String firstPath = (String) xpath_value.keySet().iterator().next();
			Value firstValue = (Value) xpath_value.get(firstPath);
			String firstFullPath = firstValue.getFullXPath();
			XPathParts parts = new XPathParts(null,null).set(firstFullPath);
			if (firstFullPath.indexOf("/identity") >= 0) {
				ldml_identity = parts.toString(2);
			} else {
				ldml_identity = parts.toString(1) + "/identity";			
			}
		}
		
		add(ldml_identity + "/version[@number=\"$Revision$\"]","");
		add(ldml_identity + "/generation[@date=\"$Date$\"]","");
		LocaleIDParser lip = new LocaleIDParser();
		lip.set(key);
		add(ldml_identity + "/language[@type=\"" + lip.getLanguage() + "\"]","");
		if (lip.getScript().length() != 0) {
			add(ldml_identity + "/script[@type=\"" + lip.getScript() + "\"]","");
		}
		if (lip.getRegion().length() != 0) {
			add(ldml_identity + "/territory[@type=\"" + lip.getRegion() + "\"]","");
		}
		String[] variants = lip.getVariants();
		for (int i = 0; i < variants.length; ++i) {
			add(ldml_identity + "/variant[@type=\"" + variants[i] + "\"]","");
		}
		// now do the rest
		
		XPathParts.writeComment(pw, 0, xpath_comments.getInitialComment(), false);
		
		XPathParts.Comments tempComments = (XPathParts.Comments) xpath_comments.clone();
		
		MapComparator modAttComp = attributeOrdering;
		if (HACK_ORDER) modAttComp = new MapComparator()
			.add("alt").add("draft").add(modAttComp.getOrder());

		XPathParts last = new XPathParts(attributeOrdering, defaultSuppressionMap);
		XPathParts current = new XPathParts(attributeOrdering, defaultSuppressionMap);
		XPathParts lastFiltered = new XPathParts(attributeOrdering, defaultSuppressionMap);
		XPathParts currentFiltered = new XPathParts(attributeOrdering, defaultSuppressionMap);
		for (Iterator it2 = xpath_value.keySet().iterator(); it2.hasNext();) {
			String xpath = (String)it2.next();
			Value v = (Value) xpath_value.get(xpath);
			currentFiltered.set(xpath);
			current.set(v.fullXPath);
			current.writeDifference(pw, currentFiltered, last, lastFiltered, v.getStringValue(), tempComments);
			// exchange pairs of parts
			XPathParts temp = current;
			current = last;
			last = temp;
			temp = currentFiltered;
			currentFiltered = lastFiltered;
			lastFiltered = temp;
		}
		current.clear().writeDifference(pw, null, last, lastFiltered, null, tempComments);
		String finalComment = xpath_comments.getFinalComment();
		
		// write comments that no longer have a base
		List x = tempComments.removeFinal();
		if (x.size() != 0) {
			String extras = "Comments without bases" + XPathParts.NEWLINE;
			for (Iterator it = x.iterator(); it.hasNext();) {
				String key = (String) it.next();
				//Log.logln("Writing extra comment: " + key);
				extras += XPathParts.NEWLINE + key;
			}
			finalComment += XPathParts.NEWLINE + extras;
		}
		XPathParts.writeComment(pw, 0, finalComment, true);
		return this;
	}

	/**
	 * @param fullxpath
	 * @param value
	 */
	public CLDRFile add(String fullxpath, String value) {
		add(getDistinguishingXPath(fullxpath), fullxpath, value);
		return this;
	}

	/**
	 * Get a value from an xpath.
	 */
    private Value getValue(String xpath) {
    	return (Value) xpath_value.get(xpath);
    }
    
	/**
	 * Get a string value from an xpath.
	 */
    public String getStringValue(String xpath) {
    	Value v = (Value) xpath_value.get(xpath);
    	if (v == null) return null;
    	return v.getStringValue();
    }
    
	/**
	 * Get a string value from an xpath.
	 */
    public String getFullXPath(String xpath) {
    	Value v = (Value) xpath_value.get(xpath);
    	if (v == null) return null;
    	return v.getFullXPath();
    }
    
    /**
     * Add a new element to a CLDRFile.
     * @param xpath
     * @param currentFullXPath
     * @param value
     */
    public CLDRFile add(String xpath, String currentFullXPath, String value) {
    	if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
    	StringValue v = new StringValue(value, currentFullXPath);
    	Log.logln("ADDING: \t" + xpath + " \t" + v);
    	xpath = xpath.intern();
        xpath_value.put(xpath, v);
        return this;
    }
    
    public CLDRFile addComment(String xpath, String comment, int type) {
    	if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
    	// System.out.println("Adding comment: <" + xpath + "> '" + comment + "'");
    	Log.logln("ADDING Comment: \t" + type + "\t" + xpath + " \t" + comment);
    	if (xpath == null || xpath.length() == 0) {
    		xpath_comments.setFinalComment(
    				Utility.joinWithSeparation(xpath_comments.getFinalComment(), XPathParts.NEWLINE, comment));
    	} else {
	        xpath_comments.add(type, xpath, comment);
    	}
    	return this;
    }

    static final public int MERGE_KEEP_MINE = 0, MERGE_REPLACE_MINE = 1, MERGE_ADD_ALTERNATE = 2;
    /**
     * Merges elements from another CLDR file. Note: when both have the same xpath key, 
     * the keepMine determines whether "my" values are kept
     * or the other files values are kept.
     * @param other
     * @param keepMine if true, keep my values in case of conflict; otherwise keep the other's values.
     */
    public CLDRFile putAll(CLDRFile other, int conflict_resolution) {
    	if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
    	if (conflict_resolution == MERGE_KEEP_MINE) {
    		Map temp = isSupplemental ? new TreeMap() : new TreeMap(ldmlComparator);
    		temp.putAll(other.xpath_value);
    		temp.putAll(xpath_value);
    		xpath_value = temp;    		
    	} else if (conflict_resolution == MERGE_REPLACE_MINE) {
    		xpath_value.putAll(other.xpath_value);
    	} else { // MERGE_ADD_ALTERNATE
    		XPathParts parts = new XPathParts(null, null);
    		for (Iterator it = other.xpath_value.keySet().iterator(); it.hasNext();) {
    			String key = (String) it.next();
    			Value otherValue = (Value) other.xpath_value.get(key);
    			Value myValue = (Value) xpath_value.get(key);
    			if (myValue == null) {
    				xpath_value.put(getDistinguishingXPath(otherValue.getFullXPath()), otherValue);
    			} else if (!myValue.equalsIgnoringDraft(otherValue) && !key.startsWith("/ldml/identity")){
    				for (int i = 0; ; ++i) {
    					String fullPath = parts.set(otherValue.getFullXPath()).addAttribute("alt", "proposed" + i).toString();
    					String path = getDistinguishingXPath(fullPath);
    					if (xpath_value.get(path) != null) continue;
    					xpath_value.put(path, new StringValue(otherValue.getStringValue(), fullPath));
    					break;
    				}
    			}
    		}
    	}
    	xpath_comments.setInitialComment(
    			Utility.joinWithSeparation(xpath_comments.getInitialComment(),
    					XPathParts.NEWLINE, 
						other.xpath_comments.getInitialComment()));
    	xpath_comments.setFinalComment(
    			Utility.joinWithSeparation(xpath_comments.getFinalComment(), 
    					XPathParts.NEWLINE, 
						other.xpath_comments.getFinalComment()));
    	xpath_comments.joinAll(other.xpath_comments);
		/*
		 *     private Map xpath_value;
private String initialComment = "";
private String finalComment = "";
private String key;
private XPathParts.Comments xpath_comments = new XPathParts.Comments(); // map from paths to comments.
private boolean isSupplemental;

		 */
    	return this;
    }
    
	/**
     * Removes an element from a CLDRFile.
     */
    public CLDRFile remove(String xpath) {
    	remove(xpath, false);
    	return this;
    }

	/**
     * Removes an element from a CLDRFile.
     */
    public CLDRFile remove(String xpath, boolean butComment) {
    	if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
    	if (butComment) {
    		CLDRFile.Value v = getValue(xpath);
    		appendFinalComment(v.getFullXPath()+ "::<" + v.getStringValue() + ">");
    	}
    	xpath_value.remove(xpath);
    	return this;
    }
    
	/**
     * Removes all xpaths from a CLDRFile.
     */
   public CLDRFile removeAll(Set xpaths, boolean butComment) {
   		if (butComment) appendFinalComment("Illegal attributes removed:");
    	for (Iterator it = xpaths.iterator(); it.hasNext();) {
    		remove((String) it.next(), butComment);
    	}
    	return this;
	}

    
    /**
     * Removes all items with same value
     */
    public CLDRFile removeDuplicates(CLDRFile other, boolean butComment) {
    	if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
    	boolean first = true;
    	for (Iterator it = other.xpath_value.keySet().iterator(); it.hasNext();) {
    		String xpath = (String)it.next();
    		Value currentValue = (Value) xpath_value.get(xpath);
    		if (currentValue == null) continue;
    		Value otherValue = (Value) other.xpath_value.get(xpath);
    		if (!currentValue.equalsIgnoringDraft(otherValue)) continue;
    		if (first) {
    			first = false;
    			if (butComment) appendFinalComment("Duplicates removed:");
    		}
    		remove(xpath, butComment);
    	}
    	return this;
    }
    
	/**
	 * @return Returns the finalComment.
	 */
	public String getFinalComment() {
		return xpath_comments.getFinalComment();
	}
	/**
	 * @return Returns the finalComment.
	 */
	public String getInitialComment() {
		return xpath_comments.getInitialComment();
	}
	/**
	 * @return Returns the xpath_comments. Cloned for safety.
	 */
	public XPathParts.Comments getXpath_comments() {
		return (XPathParts.Comments) xpath_comments.clone();
	}
	/**
	 * @return Returns the key.
	 */
	public String getKey() {
		return key;
	}

	private boolean locked;
	
	/**
	 * @see org.unicode.cldr.util.Lockable#isLocked()
	 */
	public synchronized boolean isLocked() {
		return locked;
	}

	/**
	 * @see org.unicode.cldr.util.Lockable#lock()
	 */
	public synchronized Object lock() {
		locked = true;
		return this;
	}
	
	public CLDRFile clearComments() {
		xpath_comments = new XPathParts.Comments();
		return this;
	}
	/**
	 * Sets a final comment, replacing everything that was there.
	 */
	public CLDRFile setFinalComment(String comment) {
    	if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
		xpath_comments.setFinalComment(comment);
		return this;
	}

	/**
	 * Adds a comment to the final list of comments.
	 */
	public CLDRFile appendFinalComment(String comment) {
    	if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
		xpath_comments.setFinalComment(Utility.joinWithSeparation(xpath_comments.getFinalComment(), XPathParts.NEWLINE, comment));
		return this;
	}

	/**
	 * Sets the initial comment, replacing everything that was there.
	 */
	public CLDRFile setInitialComment(String comment) {
    	if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
    	xpath_comments.setInitialComment(comment);
    	return this;
	}

	// ========== STATIC UTILITIES ==========
	
    /**
	 * Utility to restrict to files matching a given regular expression. The expression does not contain ".xml".
	 * Note that supplementalData is always skipped, and root is always included.
	 */
    public static Set getMatchingXMLFiles(String sourceDir, String localeRegex) {
        Matcher m = Pattern.compile(localeRegex).matcher("");
        Set s = new TreeSet();
        File[] files = new File(sourceDir).listFiles();
        for (int i = 0; i < files.length; ++i) {
            String name = files[i].getName();
            if (!name.endsWith(".xml")) continue;
            if (name.startsWith(SUPPLEMENTAL_NAME)) continue;
            String locale = name.substring(0,name.length()-4); // drop .xml
            if (!locale.equals("root") && !m.reset(locale).matches()) continue;
            s.add(locale);
        }
        return s;
    }

    /**
     * Returns a collection containing the keys for this file.
      */
    public Set keySet() {
    	return Collections.unmodifiableSet(xpath_value.keySet());
    }
    
    private String getDistinguishingXPath(String xpath) {
    	XPathParts parts = new XPathParts(null,null).set(xpath);
    	for (int i = 0; i < parts.size(); ++i) {
    		String element = parts.getElement(i);
    		Map attributes = parts.getAttributes(i);
    		for (Iterator it = attributes.keySet().iterator(); it.hasNext();) {
    			String attribute = (String) it.next();
    			if (!isDistinguishing(element, attribute)) {
    				it.remove();
    			}
    		}
    	}
    	return parts.toString();
    }
    
    private static String getNondraftXPath(String xpath) {
    	XPathParts parts = new XPathParts(null,null).set(xpath);
    	for (int i = 0; i < parts.size(); ++i) {
    		String element = parts.getElement(i);
    		Map attributes = parts.getAttributes(i);
    		for (Iterator it = attributes.keySet().iterator(); it.hasNext();) {
    			String attribute = (String) it.next();
    			if (attribute.equals("draft")) it.remove();
    		}
    	}
    	return parts.toString();
    }
    
	/**
	 * Determine if an attribute is a distinguishing attribute.
	 * @param elementName
	 * @param attribute
	 * @return
	 */
	private static boolean isDistinguishing(String elementName, String attribute) {
		return attribute.equals("key") 
		|| attribute.equals("registry") 
		|| attribute.equals("alt")
		|| attribute.equals("iso4217")
		|| attribute.equals("iso3166")
		|| (attribute.equals("type") 
				&& !elementName.equals("default") 
				&& !elementName.equals("measurementSystem") 
				&& !elementName.equals("mapping")
				&& !elementName.equals("abbreviationFallback")
				&& !elementName.equals("preferenceOrdering"));
	}
	
	/**
	 * Utility to create a validating XML reader.
	 */
    public static XMLReader createXMLReader(boolean validating) {
    	String[] testList = {
    			"org.apache.xerces.parsers.SAXParser",
				"org.apache.crimson.parser.XMLReaderImpl",
				"gnu.xml.aelfred2.XmlReader",
				"com.bluecast.xml.Piccolo",
				"oracle.xml.parser.v2.SAXParser",
				""
    	};
        XMLReader result = null;
        for (int i = 0; i < testList.length; ++i) {
	        try {
	            result = (testList[i].length() != 0) 
					? XMLReaderFactory.createXMLReader(testList[i])
			        : XMLReaderFactory.createXMLReader();
	            result.setFeature("http://xml.org/sax/features/validation", validating);
	            break;
	        } catch (SAXException e1) {}
        }
        if (result == null) throw new NoClassDefFoundError("No SAX parser is available, or unable to set validation correctly");
        try {
            result.setEntityResolver(new CachingEntityResolver());
        } catch (Throwable e) {
            System.err
                    .println("WARNING: Can't set caching entity resolver  -  error "
                            + e.toString());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * A factory is the normal method to produce a set of CLDRFiles from a directory of XML files.
     */
	public static class Factory {
		private String sourceDirectory;
		private String matchString;
		private Set localeList = new TreeSet();
		private Map mainCache = new TreeMap();
		private Map resolvedCache = new TreeMap();  
		private Map supplementalCache = new TreeMap();
		private Factory() {}		

		/**
		 * Create a factory from a source directory, matchingString, and an optional log file.
		 * For the matchString meaning, see {@link getMatchingXMLFiles}
		 */
		public static Factory make(String sourceDirectory, String matchString) {
			Factory result = new Factory();
			result.sourceDirectory = sourceDirectory;
			result.matchString = matchString;
			result.localeList = getMatchingXMLFiles(sourceDirectory, matchString);
			return result;
		}

		/**
		 * Get a set of the available locales for the factory.
		 */
	    public Set getAvailable() {
	    	return Collections.unmodifiableSet(localeList);
	    }
	    
	    /**
	     * Get a set of the available language locales (according to isLanguage).
	     */
	    public Set getAvailableLanguages() {
	    	Set result = new TreeSet();
	    	for (Iterator it = localeList.iterator(); it.hasNext();) {
	    		String s = (String) it.next();
	    		if (XPathParts.isLanguage(s)) result.add(s);
	    	}
	    	return result;
	    }
	    
	    /**
	     * Get a set of the locales that have the given parent (according to isSubLocale())
	     * @param isProper if false, then parent itself will match
	     */
	    public Set getAvailableWithParent(String parent, boolean isProper) {
	    	Set result = new TreeSet();
	    	for (Iterator it = localeList.iterator(); it.hasNext();) {
	    		String s = (String) it.next();
	    		int relation = XPathParts.isSubLocale(parent, s);
	    		if (relation >= 0 && !(isProper && relation == 0)) result.add(s);
	    	}
	    	return result;
	    }
	    
	    private boolean needToReadRoot = true;
	    
	    /**
	     * Make a CLDR file. The result is a locked file, so that it can be cached. If you want to modify it,
	     * use clone().
	     */
	    // TODO resolve aliases
		public CLDRFile make(String localeName, boolean resolved) {
			// TODO fix hack: 
			// read root first so that we get the ordering right.
/*			if (needToReadRoot) {
				if (!localeName.equals("root")) make("root", false);
				needToReadRoot = false;
			}
*/			// end of hack
	    	Map cache = resolved ? resolvedCache : mainCache;
	    	CLDRFile result = (CLDRFile) cache.get(localeName);
	    	if (result == null) {
	    		if (!resolved) {
	    			result = CLDRFile.make(localeName, sourceDirectory);
	    		} else {
    				// this is a bit tricky because of aliases
    				result = (CLDRFile) make(localeName, false).clone();
    				result.fixAliases(this);
    				String currentName = localeName;
    				while (true) {
    					// we do it in this order, WITHOUT resolving the parent
    					// so that aliases work right
    					currentName = CLDRFile.getParent(currentName);
    					if (currentName == null) break;
    					CLDRFile parent = make(currentName, false);
    					result.putAll(parent, MERGE_KEEP_MINE);
    					result.fixAliases(this);	    					
    				}
    				// now add "constructed" items
    				if (constructedItems == null) {   					
    					constructedItems = new CLDRFile(false);
    					StandardCodes sc = StandardCodes.make();
    					Map countries_zoneSet = sc.getCountryToZoneSet();
    					Map zone_countries = sc.getZoneToCounty();

    					//Set types = sc.getAvailableTypes();
    					for (int typeNo = 0; typeNo < LIMIT_TYPES; ++typeNo ) {
    						String type = TYPE_NAME[typeNo];
    						//int typeNo = typeNameToCode(type);
    						//if (typeNo < 0) continue;
    						String type2 = (typeNo == CURRENCY_SYMBOL) ? TYPE_NAME[CURRENCY_NAME] : type;
    						Set codes = sc.getAvailableCodes(type2);
    						String prefix = NameTable[typeNo][0];
    						String postfix = NameTable[typeNo][1];
    						String prefix2 = "/ldml" + prefix.substring(5); // [@version=\"" + GEN_VERSION + "\"]
        					for (Iterator codeIt = codes.iterator(); codeIt.hasNext(); ) {
        						String code = (String)codeIt.next();
        						String value = code;
        						if (typeNo == TZID) { // skip single-zone countries
        	    					String country = (String) zone_countries.get(code);
        							Set s = (Set) countries_zoneSet.get(country);
        							if (s != null && s.size() == 1) continue;
        							value = TimezoneFormatter.getFallbackName(value);
        						}
        						String path = prefix + code + postfix;
        						String fullpath = prefix2 + code + postfix;
        						//System.out.println(fullpath + "\t=> " + code);
        						constructedItems.add(path, fullpath, value);
        					}
    					}
    					constructedItems.lock();
    				}
    				result.putAll(constructedItems, MERGE_KEEP_MINE);
 	    		}
	    		result.lock();
	    		cache.put(localeName, result);
	    	}
	    	return result;
	    }
	}
	
	static String[] keys = {"calendar", "collation", "currency"};
	
	static String[] calendar_keys = {"buddhist", "chinese", "gregorian", "hebrew", "islamic", "islamic-civil", "japanese"};
	static String[] collation_keys = {"phonebook", "traditional", "direct", "pinyin", "stroke", "posix", "big5han", "gb2312han"};
	
	
	static CLDRFile constructedItems = null;

    /**
     * Immutable class that defines the value at a particular xpath.
     * Normally a string, unless the item does not inherit (like collation).
     */
    static private abstract class Value {
    	//private String comment;
    	private String fullXPath;
		/**
		 * Create a value.
		 */
		public Value(String currentFullXPath) {
	        //this.comment = comment.intern();
	        this.fullXPath = currentFullXPath.intern();
		}
		/**
		 * @return Returns the comment.
		 */
/*		public String getComment() {
			return comment;
		}
*/		/**
		 * @return Returns the fullXPath.
		 */
		public String getFullXPath() {
			return fullXPath;
		}
		/**
		 * boilerplate
		 */
    	final public boolean equals(Object other) {
    		if (!hasSameValue(other)) return false;
			return fullXPath.equals(((Value)other).fullXPath);
    	}

    	final public boolean equalsIgnoringDraft(Object other) {
    		if (!hasSameValue(other)) return false;
			return getNondraftXPath(fullXPath).equals(getNondraftXPath(((Value)other).fullXPath));
    	}
    	
    	public boolean hasSameValue(Object other) {
			return other != null && getClass().equals(other.getClass());
    	}
    	/**
    	 * Must be overridden.
    	 */
    	public abstract String getStringValue();
		/**
		 * boilerplate
		 */
    	public String toString() {
    		return fullXPath + ";\t" + getStringValue(); 
    	}
    	
		/**
		 * Gets whether the file is draft or not.
		 */
		public boolean isDraft() {
			return fullXPath.indexOf("[@draft=\"true\"]") >= 0;
		}
		/**
		 * clone, but change the path.
		 * @param string
		 * @return
		 */
		abstract public Value changePath(String string);
    }
    
    /**
     * Value that contains a single string
     */
    static private class StringValue extends Value {
    	private String stringValue;
    	/**
		 * @param value
		 * @param currentFullXPath
		 */
		public StringValue(String value, String currentFullXPath) {
			super(currentFullXPath);
	        this.stringValue = value.intern();
		}
		/**
		 * boilerplate
		 */
		public boolean hasSameValue(Object other) {
    		if (!super.hasSameValue(other)) return false;
    		return stringValue.equals(((StringValue)other).stringValue);
    	}
		/**
		 * boilerplate
		 */
		public String getStringValue() {
			return stringValue;
		}
		/* (non-Javadoc)
		 * @see org.unicode.cldr.util.CLDRFile.Value#changePath(java.lang.String)
		 */
		public Value changePath(String string) {
			// TODO Auto-generated method stub
			return new StringValue(stringValue, string);
		}
    }
/*    *//**
     * Value that contains a node. WARNING: this is not done yet, and may change.
     * In particular, we don't want to return a Node, since that is mutable, and makes caching unsafe!!
     *//*
    static public class NodeValue extends Value {
    	private Node nodeValue;
    	*//**
    	 * Creation. WARNING, may change.
    	 * @param value
    	 * @param currentFullXPath
    	 *//*
		public NodeValue(Node value, String currentFullXPath) {
			super(currentFullXPath);
	        this.nodeValue = value;
		}
		*//**
		 * boilerplate
		 *//*
    	public boolean hasSameValue(Object other) {
    		if (super.hasSameValue(other)) return false;
    		return nodeValue.equals(((NodeValue)other).nodeValue);
    	}
		*//**
		 * boilerplate
		 *//*
		public String getStringValue() {
			return nodeValue.toString();
		}
		 (non-Javadoc)
		 * @see org.unicode.cldr.util.CLDRFile.Value#changePath(java.lang.String)
		 
		public Value changePath(String string) {
			return new NodeValue(nodeValue, string);
		}
    }*/

    private static class MyDeclHandler implements DeclHandler, ContentHandler, LexicalHandler, ErrorHandler {
    	private static final boolean SHOW_ALL = false;
    	private static final boolean SHOW_START_END = false;
    	private int commentStack;
    	private boolean justPopped = false;
    	private String lastChars = "";
    	private String currentXPath = "";
    	private String currentFullXPath = "";
        private String comment = null;
    	private Map attributeOrder = new TreeMap(attributeOrdering);
    	private CLDRFile target;
    	private String lastActiveLeafNode;
    	private String lastLeafNode;
    	private boolean isSupplemental;
    	
    	MyDeclHandler(CLDRFile target) {
    		this.target = target;
    		isSupplemental = target.key.equals(SUPPLEMENTAL_NAME);
    		if (!isSupplemental) attributeOrder = new TreeMap(attributeOrdering);
    		else attributeOrder = new TreeMap();
     	}
    		
    	private String show(Attributes attributes) {
    		if (attributes == null) return "null";
    		String result = "";
    		for (int i = 0; i < attributes.getLength(); ++i) {    			
    			String attribute = attributes.getQName(i);
    			String value = attributes.getValue(i);
     			result += "[@" + attribute + "=\"" + value + "\"]"; // TODO quote the value??
    		}
    		return result;
    	}
    	
    	private void push(String qName, Attributes attributes) {
    		//SHOW_ALL && 
    		Log.logln("push\t" + qName + "\t" + show(attributes));
        	if (lastChars.length() != 0) throw new IllegalArgumentException("Internal Error");
    		currentXPath += "/" + qName;
    		currentFullXPath += "/" + qName;
    		//if (!isSupplemental) ldmlComparator.addElement(qName);
    		if (attributes.getLength() > 0) {
    			attributeOrder.clear();
	    		for (int i = 0; i < attributes.getLength(); ++i) {    			
	    			String attribute = attributes.getQName(i);
	    			String value = attributes.getValue(i);
	    			//if (!isSupplemental) ldmlComparator.addAttribute(attribute); // must do BEFORE put
	    			//ldmlComparator.addValue(value);
	    			// special fix to remove version
	    			if (qName.equals("ldml") && attribute.equals("version")) {
	    				// do nothing!
	    			} else {
	    				attributeOrder.put(attribute, value);
	    			}
	    		}
	    		for (Iterator it = attributeOrder.keySet().iterator(); it.hasNext();) {
	    			String attribute = (String)it.next();
	    			String value = (String)attributeOrder.get(attribute);
	    			String both = "[@" + attribute + "=\"" + value + "\"]"; // TODO quote the value??
	    			currentFullXPath += both;
	    			// distinguishing = key, registry, alt, and type (except for the type attribute on the elements default and mapping).
	    			if (isDistinguishing(qName, attribute)) {
	    				currentXPath += both;
	    			}
	    		}
    		}
    		if (comment != null) {
    			target.addComment(currentXPath, comment, XPathParts.Comments.PREBLOCK);
    			comment = null;
    		}
            justPopped = false;
            lastActiveLeafNode = null;
    		Log.logln(SHOW_ALL, "currentXPath\t" + currentXPath + "\tcurrentFullXPath\t" + currentFullXPath);
    	}
    	
		private void pop(String qName) {
			Log.logln("pop\t" + qName);
            if (lastChars.length() != 0 || justPopped == false) {
                target.add(currentXPath, currentFullXPath, lastChars);
                lastChars = "";
                lastLeafNode = lastActiveLeafNode = currentXPath;
            } else {
            	Log.logln(lastActiveLeafNode != null, "pop: zeroing last leafNode: " + lastActiveLeafNode);
            	lastActiveLeafNode = null;
        		if (comment != null) {
        			target.addComment(lastLeafNode, comment, XPathParts.Comments.POSTBLOCK);
        			comment = null;
        		}
            }
			currentXPath = stripAfter(currentXPath, qName);
    		currentFullXPath = stripAfter(currentFullXPath, qName);    
            justPopped = true;
    	}
    	
		private static String stripAfter(String input, String qName) {
			int pos = findLastSlash(input);
			if (qName != null) assert input.substring(pos+1).startsWith(qName);
			return input.substring(0,pos);
		}
		
		private static int findLastSlash(String input) {
			int braceStack = 0;
			for (int i = input.length()-1; i >= 0; --i) {
				char ch = input.charAt(i);
				switch(ch) {
				case '/': if (braceStack == 0) return i; break;
				case '[': --braceStack; break;
				case ']': ++braceStack; break;
				}
			}
			return -1;
		}

		// SAX items we need to catch
		
        public void startElement(
            String uri,
            String localName,
            String qName,
            Attributes attributes)
            throws SAXException {
        		Log.logln(SHOW_ALL || SHOW_START_END, "startElement uri\t" + uri
        				+ "\tlocalName " + localName
        				+ "\tqName " + qName
        				+ "\tattributes " + show(attributes)
						);
        		try {
            		push(qName, attributes);                    
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    throw e;
                }
        }
        public void endElement(String uri, String localName, String qName)
            throws SAXException {
    			Log.logln(SHOW_ALL || SHOW_START_END, "endElement uri\t" + uri + "\tlocalName " + localName
    				+ "\tqName " + qName);
                try {
                    pop(qName);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    throw e;
                }
            }
        public void characters(char[] ch, int start, int length)
            throws SAXException {
                try {
                    String value = new String(ch,start,length);
                    Log.logln(SHOW_ALL, "characters:\t" + value);
                    lastChars += value;
                    justPopped = false;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    throw e;
                }
            }

        public void startDTD(String name, String publicId, String systemId) throws SAXException {
            Log.logln(SHOW_ALL, "startDTD name: " + name
                    + ", publicId: " + publicId
                    + ", systemId: " + systemId
            );
            commentStack++;
        }
        public void endDTD() throws SAXException {
            Log.logln(SHOW_ALL, "endDTD");
            commentStack--;
        }
        
        public void comment(char[] ch, int start, int length) throws SAXException {
            Log.logln(SHOW_ALL, commentStack + " comment " + new String(ch, start,length));
            try {
				if (commentStack != 0) return;
				String comment0 = new String(ch, start,length);
				if (lastActiveLeafNode != null) {
					target.addComment(lastActiveLeafNode, comment0, XPathParts.Comments.LINE);
				} else {
					comment = (comment == null ? comment0 : comment + XPathParts.NEWLINE + comment0);
				}
			} catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
			}
        }
        
        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            Log.logln(SHOW_ALL, "ignorableWhitespace length: " + length);
            for (int i = 0; i < ch.length; ++i) {
            	if (ch[i] == '\n') {
            		Log.logln(lastActiveLeafNode != null, "\\n: zeroing last leafNode: " + lastActiveLeafNode);
            		lastActiveLeafNode = null;
            	}
            }
        }
        public void startDocument() throws SAXException {
            Log.logln(SHOW_ALL, "startDocument");
            commentStack = 0; // initialize
        }

        public void endDocument() throws SAXException {
            Log.logln(SHOW_ALL, "endDocument");
            try {
				if (comment != null) target.addComment(null, comment, XPathParts.Comments.LINE);
			} catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
			}
        }

        // ==== The following are just for debuggin =====

		public void elementDecl(String name, String model) throws SAXException {
        	Log.logln(SHOW_ALL, "Attribute\t" + name + "\t" + model);
        }
        public void attributeDecl(String eName, String aName, String type, String mode, String value) throws SAXException {
            Log.logln(SHOW_ALL, "Attribute\t" + eName + "\t" + aName + "\t" + type + "\t" + mode + "\t" + value);
        }
        public void internalEntityDecl(String name, String value) throws SAXException {
        	Log.logln(SHOW_ALL, "Internal Entity\t" + name + "\t" + value);
        }
        public void externalEntityDecl(String name, String publicId, String systemId) throws SAXException {
        	Log.logln(SHOW_ALL, "Internal Entity\t" + name + "\t" + publicId + "\t" + systemId);
        }

        public void notationDecl (String name, String publicId, String systemId){
            Log.logln(SHOW_ALL, "notationDecl: " + name
            + ", " + publicId
            + ", " + systemId
            );
        }

        public void processingInstruction (String target, String data)
        throws SAXException {
            Log.logln(SHOW_ALL, "processingInstruction: " + target + ", " + data);
        }

        public void skippedEntity (String name)
        throws SAXException {
            Log.logln(SHOW_ALL, "skippedEntity: " + name);
        }

        public void unparsedEntityDecl (String name, String publicId,
                        String systemId, String notationName) {
            Log.logln(SHOW_ALL, "unparsedEntityDecl: " + name
            + ", " + publicId
            + ", " + systemId
            + ", " + notationName
            );
        }
        
        public void setDocumentLocator(Locator locator) {
            Log.logln(SHOW_ALL, "setDocumentLocator Locator " + locator);
        }
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            Log.logln(SHOW_ALL, "startPrefixMapping prefix: " + prefix +
                    ", uri: " + uri);
        }
        public void endPrefixMapping(String prefix) throws SAXException {
            Log.logln(SHOW_ALL, "endPrefixMapping prefix: " + prefix);
        }
        public void startEntity(String name) throws SAXException {
            Log.logln(SHOW_ALL, "startEntity name: " + name);
        }
        public void endEntity(String name) throws SAXException {
            Log.logln(SHOW_ALL, "endEntity name: " + name);
        }
        public void startCDATA() throws SAXException {
            Log.logln(SHOW_ALL, "startCDATA");
        }
        public void endCDATA() throws SAXException {
            Log.logln(SHOW_ALL, "endCDATA");
        }

		/* (non-Javadoc)
		 * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
		 */
		public void error(SAXParseException exception) throws SAXException {
			Log.logln(SHOW_ALL, "error: " + showSAX(exception));
			throw exception;
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
		 */
		public void fatalError(SAXParseException exception) throws SAXException {
			Log.logln(SHOW_ALL, "fatalError: " + showSAX(exception));
			throw exception;
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
		 */
		public void warning(SAXParseException exception) throws SAXException {
			Log.logln(SHOW_ALL, "warning: " + showSAX(exception));
			throw exception;
		}
    }

	/**
	 * Show a SAX exception in a readable form.
	 */
	public static String showSAX(SAXParseException exception) {
		return exception.getMessage() 
		+ ";\t SystemID: " + exception.getSystemId() 
		+ ";\t PublicID: " + exception.getPublicId() 
		+ ";\t LineNumber: " + exception.getLineNumber() 
		+ ";\t ColumnNumber: " + exception.getColumnNumber() 
		;
	}

	/**
	 * Only gets called on (mostly) resolved stuff
	 */
	private CLDRFile fixAliases(Factory factory) {
		// walk through the entire tree. If we ever find an alias, 
		// remove every peer of that alias,
		// then add everything from the resolved source of the alias.
		List aliases = new ArrayList();
		for (Iterator it = xpath_value.keySet().iterator(); it.hasNext();) {
			String xpath = (String) it.next();
			if (xpath.indexOf("/alias") >= 0) { // quick check; have more rigorous one later.
				aliases.add(xpath);
			}
		}
		if (aliases.size() == 0) return this;
		XPathParts parts = new XPathParts(attributeOrdering, defaultSuppressionMap);
		XPathParts fullParts = new XPathParts(attributeOrdering, defaultSuppressionMap);
		XPathParts otherParts = new XPathParts(attributeOrdering, defaultSuppressionMap);
		for (Iterator it = aliases.iterator(); it.hasNext();) {
			String xpathKey = (String) it.next();
			if (SHOW_ALIAS_FIXES) System.out.println("Doing Alias for: " + xpathKey);
			Value v = (Value) xpath_value.get(xpathKey);
			parts.set(xpathKey);
			int index = parts.findElement("alias"); // can have no children
			if (index < 0) continue;
			parts.trimLast();
			fullParts.set(v.getFullXPath());
			Map attributes = fullParts.getAttributes(index);
			fullParts.trimLast();
			// <alias source="<locale_ID>" path="..."/>
			String source = (String) attributes.get("source");
			if (source == null || source.equals("locale")) source = key;
			otherParts.set(parts);
			String otherPath = (String) attributes.get("path");
			if (otherPath != null) {
				otherParts.addRelative(otherPath);
			}
			removeChildren(parts); 
			CLDRFile other;
			if (source.equals(key)) {
				other = this; 
			} else {
				other = factory.make(source,true);
			}
			addChildren(parts, fullParts, other, otherParts);
		}		
		return this;
	}

	/**
	 * @param parts
	 * @param other
	 * @param otherParts
	 */
	private CLDRFile addChildren(XPathParts parts, XPathParts fullParts, CLDRFile other, XPathParts otherParts) {
		String otherPath = otherParts + "/";
		XPathParts temp = new XPathParts(attributeOrdering, defaultSuppressionMap);
		XPathParts fullTemp = new XPathParts(attributeOrdering, defaultSuppressionMap);
		Map tempMap = new HashMap();
		for (Iterator it = other.xpath_value.keySet().iterator(); it.hasNext();) {
			String path = (String)it.next();
			if (path.startsWith(otherPath)) {
				Value value = (Value) other.xpath_value.get(path);
				temp.set(path);
				temp.replace(otherParts.size(), parts);
				fullTemp.set(value.getFullXPath());
				fullTemp.replace(otherParts.size(), fullParts);
				String newPath = temp.toString();
				value = value.changePath(fullTemp.toString());
				if (SHOW_ALIAS_FIXES) System.out.println("Adding*: " + path + ";\r\n\t" + newPath + ";\r\n\t" + value);
				tempMap.put(newPath, value);
				// to do, fix path
			}
		}
		xpath_value.putAll(tempMap);
		return this;
	}

	/**
	 * @param parts
	 */
	private CLDRFile removeChildren(XPathParts parts) {
		String mypath = parts + "/";
		for (Iterator it = xpath_value.keySet().iterator(); it.hasNext();) {
			String path = (String)it.next();
			if (path.startsWith(mypath)) {
				if (false) System.out.println("Removing: " + xpath_value.get(path));
				it.remove();
			}
		}
		return this;
	}

	/**
	 * Says whether the whole file is draft
	 */
	public boolean isDraft() {
		String item = (String) xpath_value.keySet().iterator().next();
		return item.indexOf("[@draft=\"true\"]") >= 0;
	}
	
	/**
	 * Gets the type of a given xpath, eg script, territory, ...
	 * TODO move to separate class
	 * @param xpath
	 * @return
	 */
	public static int getNameType(String xpath) {
		for (int i = 0; i < NameTable.length; ++i) {
			if (xpath.startsWith(NameTable[i][0]) && xpath.endsWith(NameTable[i][1])) return i;
		}
		return -1;
	}
	
	/**
	 * Gets the display name for a type
	 */
	public static String getNameTypeName(int index) {
		try {
			return TYPE_NAME[index];
		} catch (Exception e) {
			return "Illegal Type Name: " + index;
		}
	}
	
	private static final String[][] NameTable = {
			{"/ldml/localeDisplayNames/languages/language[@type=\"", "\"]", "language"},
			{"/ldml/localeDisplayNames/scripts/script[@type=\"", "\"]", "script"},
			{"/ldml/localeDisplayNames/territories/territory[@type=\"", "\"]", "territory"},
			{"/ldml/localeDisplayNames/variants/variant[@type=\"", "\"]", "variant"},
			{"/ldml/numbers/currencies/currency[@type=\"", "\"]/displayName", "currency"},
			{"/ldml/numbers/currencies/currency[@type=\"", "\"]/symbol", "currency-symbol"},
			{"/ldml/dates/timeZoneNames/zone[@type=\"", "\"]/exemplarCity", "tzid"},
	};

	public static final int NO_NAME = -1, LANGUAGE_NAME = 0, SCRIPT_NAME = 1, TERRITORY_NAME = 2, VARIANT_NAME = 3,
		CURRENCY_NAME = 4, CURRENCY_SYMBOL = 5, TZID = 6, LIMIT_TYPES = 7;
	private static final String[] TYPE_NAME = {"language", "script", "territory", "variant", "currency", "currency-symbol", "tzid"};
	
	/**
	 * @return the key used to access  data of a given type
	 */
	public static String getKey(int type, String code) {
		return NameTable[type][0] + code + NameTable[type][1];
	}
	/**
	 * Utility for getting the name, given a code.
	 * @param type
	 * @param code
	 * @param skipDraft
	 * @return
	 */
	public String getName(int type, String code, boolean skipDraft) {
		Value v = getValue(getKey(type, code));
		if (v == null || skipDraft && v.isDraft()) return null;
		return v.getStringValue();
	}
	
	/**
	 * Utility for getting a name, given a type and code.
	 */
	public String getName(String type, String code, boolean skipDraft) {
		return getName(typeNameToCode(type), code, skipDraft);
	}
	
	/**
	 * @param type
	 * @return
	 */
	private static int typeNameToCode(String type) {
		for (int i = 0; i < TYPE_NAME.length; ++i) {
			if (type.equalsIgnoreCase(TYPE_NAME[i])) return i;
		}
		return -1;
	}

	LanguageTagParser lparser = new LanguageTagParser();
	
	public synchronized String getName(String locale, boolean skipDraft) {
		lparser.set(locale);
		String name = getName(LANGUAGE_NAME, lparser.getLanguage(), skipDraft);
		String sname = lparser.getScript();
		if (sname.length() != 0) name += " - " + getName(SCRIPT_NAME, sname, skipDraft);
		String extras = "";
		sname = lparser.getRegion();
		if (sname.length() != 0) {
			if (extras.length() != 0) extras += ", ";
			extras += getName(TERRITORY_NAME, sname, skipDraft);
		}
		List variants = lparser.getVariants();
		for (int i = 0; i < variants.size(); ++i) {
			if (extras.length() != 0) extras += ", ";
			extras += getName(VARIANT_NAME, (String)variants.get(i), skipDraft);
		}
		return name + (extras.length() == 0 ? "" : "(" + extras + ")");
	}
	
	/**
	 * Returns the name of a type.
	 */
	public String getNameName(int choice) {
		return NameTable[choice][2];
	}
	
	/**
	 * Get standard ordering for elements.
	 * @return ordered collection with items.
	 */
	public static Collection getElementOrder() {
		return elementOrdering.getOrder(); // already unmodifiable
	}

	/**
	 * Get standard ordering for attributes.
	 * @return ordered collection with items.
	 */
	public static Collection getAttributeOrder() {
		return attributeOrdering.getOrder(); // already unmodifiable
	}

	/**
	 * Get standard ordering for attributes.
	 * @return ordered collection with items.
	 */
	public static Comparator getAttributeComparator() {
		return attributeOrdering; // already unmodifiable
	}


	/**
	 * Get standard ordering for attribute values.
	 * @return ordered collection with items.
	 */
	public static Collection getValueOrder() {
		return valueOrdering.getOrder(); // already unmodifiable
	}
	
	/**
	 * Utility to get the parent of a locale. If the input is "root", then the output is null.
	 */
	public static String getParent(String localeName) {
	    int pos = localeName.lastIndexOf('_');
	    if (pos >= 0) {
	        return localeName.substring(0,pos);
	    }
	    if (localeName.equals("root") || localeName.equals("supplementalData")) return null;
	    return "root";
	}

	private static MapComparator elementOrdering = (MapComparator) new MapComparator().add(new String[] {
			"ldml", "identity", "alias",
			"localeDisplayNames", "layout", "characters", "delimiters",
			"measurement", "dates", "numbers", "collations", "posix",
			"version", "generation", "language", "script", "territory",
			"variant", "languages", "scripts", "territories", "variants",
			"keys", "types", "key", "type", "orientation",
			"exemplarCharacters", "mapping", "cp", "quotationStart",
			"quotationEnd", "alternateQuotationStart",
			"alternateQuotationEnd", "measurementSystem", "paperSize",
			"height", "width", "localizedPatternChars", "calendars",
			"timeZoneNames", "months", "monthNames", "monthAbbr", "days",
			"dayNames", "dayAbbr", "week", "am", "pm", "eras",
			"dateFormats", "timeFormats", "dateTimeFormats", "fields",
			"month", "day", "minDays", "firstDay", "weekendStart",
			"weekendEnd", "eraNames", "eraAbbr", "era", "pattern",
			"displayName", "hourFormat", "hoursFormat", "gmtFormat",
			"regionFormat", "fallbackFormat", "abbreviationFallback",
			"preferenceOrdering", "default", "calendar", "monthContext",
			"monthWidth", "dayContext", "dayWidth", "dateFormatLength",
			"dateFormat", "timeFormatLength", "timeFormat",
			"dateTimeFormatLength", "dateTimeFormat", "zone", "long",
			"short", "exemplarCity", "generic", "standard", "daylight",
			"field", "relative", "symbols", "decimalFormats",
			"scientificFormats", "percentFormats", "currencyFormats",
			"currencies", "decimalFormatLength", "decimalFormat",
			"scientificFormatLength", "scientificFormat",
			"percentFormatLength", "percentFormat", "currencyFormatLength",
			"currencyFormat", "currency", "symbol", "decimal", "group",
			"list", "percentSign", "nativeZeroDigit", "patternDigit",
			"plusSign", "minusSign", "exponential", "perMille", "infinity",
			"nan", "collation", "messages", "yesstr", "nostr",
			"yesexpr", "noexpr", "references", "reference",
			"special", }).lock();
	
	static MapComparator attributeOrdering = (MapComparator) new MapComparator().add(new String[] {
			"type", "key", "registry", "alt",
			"source", "path",
			"day", "date",
			"version", "count",
			"lines", "characters",
			"before",
			"number", "time",
			"validSubLocales",
			"standard", "references",
			"uri",
			"draft",
			}).lock();
	static MapComparator valueOrdering = (MapComparator) new MapComparator().setErrorOnMissing(false).lock();
	/*
	
	//RuleBasedCollator valueOrdering = (RuleBasedCollator) Collator.getInstance(ULocale.ENGLISH);
    static {

    	
    	// others are alphabetical
       	String[] valueOrder = {
       			"full", "long", "medium", "short",
       			"abbreviated", "narrow", "wide",
    			//"collation", "calendar", "currency",
				"buddhist", "chinese", "gregorian", "hebrew", "islamic", "islamic-civil", "japanese", "direct",				
				//"japanese", "buddhist", "islamic", "islamic-civil", "hebrew", "chinese", "gregorian", "phonebook", "traditional", "direct",

				"sun", "mon", "tue", "wed", "thu", "fri", "sat", // removed, since it is a language tag
				"America/Vancouver",
				"America/Los_Angeles",
				"America/Edmonton",
				"America/Denver",
				"America/Phoenix",
				"America/Winnipeg",
				"America/Chicago",
				"America/Montreal",
				"America/New_York",
				"America/Indianapolis",
				"Pacific/Honolulu",
				"America/Anchorage",
				"America/Halifax",
				"America/St_Johns",
				"Europe/Paris",
				"Europe/Belfast",
				"Europe/Dublin",
				"Etc/GMT",
				"Africa/Casablanca",
				"Asia/Jerusalem",
				"Asia/Tokyo",
				"Europe/Bucharest",
				"Asia/Shanghai",
				};       	
    	valueOrdering.add(valueOrder).lock();
    	//StandardCodes sc = StandardCodes.make();
    }
    */
    static MapComparator dayValueOrder = (MapComparator) new MapComparator().add(new String[] {
    		"sun", "mon", "tue", "wed", "thu", "fri", "sat"}).lock();
    static MapComparator widthOrder = (MapComparator) new MapComparator().add(new String[] {
    		"abbreviated", "narrow", "wide"}).lock();
    static MapComparator lengthOrder = (MapComparator) new MapComparator().add(new String[] {
    		"full", "long", "medium", "short"}).lock();
    static MapComparator dateFieldOrder = (MapComparator) new MapComparator().add(new String[] {
    		"era", "year", "month", "week", "day", "weekday", "dayperiod",
			"hour", "minute", "second", "zone"}).lock();
    static Comparator zoneOrder = StandardCodes.make().getTZIDComparator();
    
    /**
     * Comparator for attributes in CLDR files
     */
	public static Comparator ldmlComparator = new LDMLComparator();

	static class LDMLComparator implements Comparator {

		transient XPathParts a = new XPathParts(attributeOrdering, null);
		transient XPathParts b = new XPathParts(attributeOrdering, null);
		
		public void addElement(String a) {
			//elementOrdering.add(a);
		}
		public void addAttribute(String a) {
			if ( false && (a.equals("buddhist") ||
					a.equals("gregorian"))) {
				System.out.println("here2");
			}
			//attributeOrdering.add(a);
		}
		public void addValue(String a) {
			//valueOrdering.add(a);
		}
		public int compare(Object o1, Object o2) {
			int result;
			if (false && (o1.toString().indexOf("alt") >= 0 ||
					o2.toString().indexOf("alt") >= 0)) {
				System.out.println("here");
			}
			a.set((String)o1);
			b.set((String)o2);
			int minSize = a.size();
			if (b.size() < minSize) minSize = b.size();
			for (int i = 0; i < minSize; ++i) {
				String aname = a.getElement(i);
				String bname = b.getElement(i);
				if (0 != (result = elementOrdering.compare(aname, bname))) return result;
				Map am = a.getAttributes(i);
				Map bm = b.getAttributes(i);
				int minMapSize = am.size();
				if (bm.size() < minMapSize) minMapSize = bm.size();
				if (minMapSize != 0) {
					Iterator ait = am.keySet().iterator();
					Iterator bit = bm.keySet().iterator();
					for (int j = 0; j < minMapSize; ++j) {
						String akey = (String) ait.next();
						String bkey = (String) bit.next();
						if (0 != (result = attributeOrdering.compare(akey, bkey))) return result;
						String avalue = (String) am.get(akey);
						String bvalue = (String) bm.get(bkey);
						Comparator comp = valueOrdering;
						if (akey.equals("day") && aname.startsWith("weekend")) {
							comp = dayValueOrder;
						} else if (akey.equals("type")) {
							if (aname.endsWith("FormatLength ")) comp = lengthOrder;
							else if (aname.endsWith("Width")) comp = widthOrder;
							else if (aname.equals("day")) comp = dayValueOrder;
							else if (aname.equals("field")) comp = dateFieldOrder;
							else if (aname.equals("zone")) comp = zoneOrder;
						}
						if (0 != (result = comp.compare(avalue, bvalue))) return result;
					}
				}
				if (am.size() < bm.size()) return -1;
				if (am.size() > bm.size()) return 1;				
			}
			if (a.size() < b.size()) return -1;
			if (a.size() > b.size()) return 1;
			return 0;
		}		
	}
	
	private final static Map defaultSuppressionMap; 
	static {
		String[][] data = {
				{"ldml", "version", GEN_VERSION},
				{"orientation", "characters", "left-to-right"},
				{"orientation", "lines", "top-to-bottom"},
				{"weekendStart", "time", "00:00"},
				{"weekendEnd", "time", "24:00"},
				{"dateFormat", "type", "standard"},
				{"timeFormat", "type", "standard"},
				{"dateTimeFormat", "type", "standard"},
				{"decimalFormat", "type", "standard"},
				{"scientificFormat", "type", "standard"},
				{"percentFormat", "type", "standard"},
				{"currencyFormat", "type", "standard"},
				{"pattern", "type", "standard"},
				{"currency", "type", "standard"},
				{"collation", "type", "standard"},
		};
		Map tempmain = new HashMap();
		for (int i = 0; i < data.length; ++i) {
			Map temp = (Map) tempmain.get(data[i][0]);
			if (temp == null) {
				temp = new HashMap();
				tempmain.put(data[i][0], temp);
			}
			temp.put(data[i][1], data[i][2]);
		}
		defaultSuppressionMap = Collections.unmodifiableMap(tempmain);
	}
	/**
	 * Removes a comment.
	 */
	public CLDRFile removeComment(String string) {
    	if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
		// TODO Auto-generated method stub
    	xpath_comments.removeComment(string);
    	return this;
	}

	/**
	 * 
	 */
	public CLDRFile makeDraft() {
    	if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
    	XPathParts parts = new XPathParts(null,null);
    	for (Iterator it = xpath_value.keySet().iterator(); it.hasNext();) {
    		String path = (String) it.next();
    		Value v = (Value) xpath_value.get(path);
    		if (!(v instanceof StringValue)) continue;
    		parts.set(v.getFullXPath()).addAttribute("draft", "true");
    		xpath_value.put(path, new StringValue(v.getStringValue(), parts.toString()));
    	}
		return this;
	}
}
