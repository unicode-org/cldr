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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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

import org.unicode.cldr.util.XPathParts.Comments;
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

import com.ibm.icu.impl.CollectionUtilities;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Freezable;

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
public class CLDRFile implements Freezable {
	private static boolean LOG_PROGRESS = false;

	public static boolean HACK_ORDER = false;
	private static boolean DEBUG_LOGGING = false;
	private static boolean SHOW_ALIAS_FIXES = false;
	
    public static final String SUPPLEMENTAL_NAME = "supplementalData";
    public static final String SUPPLEMENTAL_METADATA = "supplementalMetadata";
    public static final String SUPPLEMENTAL_PREFIX = "supplemental";
	public static final String GEN_VERSION = "1.4";
	
	private boolean locked;
	private XMLSource dataSource;
	
	public static class SimpleXMLSource extends XMLSource {
		private HashMap xpath_value = new HashMap(); // TODO change to HashMap, once comparator is gone
		private HashMap xpath_fullXPath = new HashMap();
		private Comments xpath_comments = new Comments(); // map from paths to comments.
		private Factory factory; // for now, fix later
		
		public SimpleXMLSource(Factory factory, String localeID) {
			this.factory = factory;
			this.setLocaleID(localeID);
		}
		public String getValueAtDPath(String xpath) {
			return (String)xpath_value.get(xpath);
		}
		public String getFullPathAtDPath(String xpath) {
			String result = (String) xpath_fullXPath.get(xpath);
			if (result != null) return result;
			if (xpath_value.get(xpath) != null) return xpath; // we don't store duplicates
            //System.err.println("WARNING: "+getLocaleID()+": path not present in data: " + xpath);
            //return xpath;
			return null; // throw new IllegalArgumentException("Path not present in data: " + xpath);
		}
		public Comments getXpathComments() {
			return xpath_comments;
		}
		public void setXpathComments(Comments xpath_comments) {
			this.xpath_comments = xpath_comments;
		}
//		public void putPathValue(String xpath, String value) {
//			if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
//			String distinguishingXPath = CLDRFile.getDistinguishingXPath(xpath, fixedPath);	
//			xpath_value.put(distinguishingXPath, value);
//			if (!fixedPath[0].equals(distinguishingXPath)) {
//				xpath_fullXPath.put(distinguishingXPath, fixedPath[0]);
//			}
//		}
		public void removeValueAtDPath(String distinguishingXPath) {
			xpath_value.remove(distinguishingXPath);
			xpath_fullXPath.remove(distinguishingXPath);
		}
		public Iterator iterator() { // must be unmodifiable or locked
			return Collections.unmodifiableSet(xpath_value.keySet()).iterator();
		}
		public Object freeze() {
			locked = true;
			return this;
		}
		public Object cloneAsThawed() {
			SimpleXMLSource result = (SimpleXMLSource) super.cloneAsThawed();
			result.xpath_comments = (Comments) result.xpath_comments.clone();
			result.xpath_fullXPath = (HashMap) result.xpath_fullXPath.clone();
			result.xpath_value = (HashMap) result.xpath_value.clone();
			return result;
		}
		public void putFullPathAtDPath(String distinguishingXPath, String fullxpath) {
			xpath_fullXPath.put(distinguishingXPath, fullxpath);
		}
		public void putValueAtDPath(String distinguishingXPath, String value) {
			xpath_value.put(distinguishingXPath, value);
		}
		public XMLSource make(String localeID) {
			if (localeID == null) return null;
			CLDRFile file = factory.make(localeID, false);
			if (file == null) return null;
			return file.dataSource;
		}
		public Set getAvailableLocales() {
			return factory.getAvailable();
		}
	}
		

	
	public String toString() {
		return "locked: " + locked + "\r\n" + dataSource;
	}
	
	public String toString(String regex) {
		return "locked: " + locked + " [" + regex +  "]\r\n" + dataSource.toString(regex);
	}
	
    // for refactoring
    
	public CLDRFile setNonInheriting(boolean isSupplemental) {
    	if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
		dataSource.setNonInheriting(isSupplemental);
		return this;
	}

	public boolean isNonInheriting() {
		return dataSource.isNonInheriting();
	}

	public CLDRFile(XMLSource dataSource, boolean resolved){
    	if (dataSource == null) dataSource = new SimpleXMLSource(null, null);
    	if (resolved && !dataSource.isResolving()) {
    		dataSource = dataSource.getResolving();
    	}
    	if (!resolved && dataSource.isResolving()) {
    		throw new IllegalArgumentException("Can't create unresolved file from resolved one");
    	}
    	this.dataSource = dataSource;
    	//source.xpath_value = isSupplemental ? new TreeMap() : new TreeMap(ldmlComparator);
    }
	
    /**
     * Create a CLDRFile for the given localename. (Normally a Factory is used to create CLDRFiles.)
     * @param localeName
     */
    public static CLDRFile make(String localeName) {
    	CLDRFile result = new CLDRFile(null, false);
		result.dataSource.setLocaleID(localeName);
		return result;
    }

    /**
     * Create a CLDRFile for the given localename. (Normally a Factory is used to create CLDRFiles.)
     * @param localeName
     */
    public static CLDRFile makeSupplemental(String localeName) {
    	CLDRFile result = new CLDRFile(null, false);
		result.dataSource.setLocaleID(localeName);
		result.setNonInheriting(true);
		return result;
    }
    
    /**
     * Produce a CLDRFile from a localeName and filename, given a directory. (Normally a Factory is used to create CLDRFiles.)
     * @param localeName
     * @param dir directory 
     */
	public static CLDRFile make(String localeName, String dir, boolean includeDraft) {
		return makeFromFile(dir + File.separator + localeName + ".xml", localeName, includeDraft);
    }
	
    /**
     * Produce a CLDRFile from a localeName, given a directory. (Normally a Factory is used to create CLDRFiles.)
     * @param localeName
     * @param dir directory 
     */
    // TODO make the directory a URL
    public static CLDRFile makeFromFile(String fullFileName, String localeName, boolean includeDraft) {
        File f = new File(fullFileName);
        try {
        	fullFileName = f.getCanonicalPath();
            if (DEBUG_LOGGING) {
             	System.out.println("Parsing: " + fullFileName);
             	Log.logln(LOG_PROGRESS, "Parsing: " + fullFileName);
    	    }
			FileInputStream fis = new FileInputStream(f);
	    	CLDRFile result = make(fullFileName, localeName, fis, includeDraft);
			fis.close();
			return result;
		} catch (Exception e) {
			//e.printStackTrace();
			throw (IllegalArgumentException)new IllegalArgumentException("Can't read " + fullFileName).initCause(e);
		}
    }
    
    /**
     * Produce a CLDRFile from a file input stream. (Normally a Factory is used to create CLDRFiles.)
     * @param localeName
     * @param fis
     */
    public static CLDRFile make(String fileName, String localeName, InputStream fis, boolean includeDraft) {
    	try {
    		fis = new StripUTF8BOMInputStream(fis);
    		CLDRFile result = make(localeName);
			MyDeclHandler DEFAULT_DECLHANDLER = new MyDeclHandler(result, includeDraft);
            result.setNonInheriting(DEFAULT_DECLHANDLER.isSupplemental);

            // now fill it.
            
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
    
    /**
     * Clone the object. Produces unlocked version (see Lockable).
     */
    public Object cloneAsThawed() {
    	try {
			CLDRFile result = (CLDRFile) super.clone();
			result.locked = false;
			result.dataSource = (XMLSource)result.dataSource.cloneAsThawed();
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
		for (Iterator it2 = iterator(); it2.hasNext();) {
			String xpath = (String)it2.next();
			System.out.println(getFullXPath(xpath) + " =>\t" + getStringValue(xpath));
		}
		return this;
    }

	/**
	 * Write the corresponding XML file out, with the normal formatting and indentation.
	 * Will update the identity element, including generation, version, and other items.
	 */
	public CLDRFile write(PrintWriter pw) {
		Set orderedSet = new TreeSet(ldmlComparator);
		CollectionUtilities.addAll(dataSource.iterator(), orderedSet);

		pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
		pw.println("<!DOCTYPE ldml SYSTEM \"http://www.unicode.org/cldr/dtd/" + GEN_VERSION + "/ldml" 
				+ (isNonInheriting() ? "Supplemental" : "")
				+ ".dtd\">");
		/*
<identity>
<version number="1.2"/>
<generation date="2004-08-27"/>
<language type="en"/>
		 */
		// if ldml has any attributes, get them.
		Set identitySet = new TreeSet(ldmlComparator);
		if (isNonInheriting()) {
			//identitySet.add("//supplementalData[@version=\"" + GEN_VERSION + "\"]/version[@number=\"$Revision$\"]");
			//identitySet.add("//supplementalData[@version=\"" + GEN_VERSION + "\"]/generation[@date=\"$Date$\"]");
		} else {
			String ldml_identity = "//ldml/identity";
			if (orderedSet.size() > 0) {
				String firstPath = (String) orderedSet.iterator().next();
				//Value firstValue = (Value) getXpath_value().get(firstPath);
				String firstFullPath = getFullXPath(firstPath);
				XPathParts parts = new XPathParts(null,null).set(firstFullPath);
				if (firstFullPath.indexOf("/identity") >= 0) {
					ldml_identity = parts.toString(2);
				} else {
					ldml_identity = parts.toString(1) + "/identity";			
				}
			}
			
			identitySet.add(ldml_identity + "/version[@number=\"$Revision$\"]");
			identitySet.add(ldml_identity + "/generation[@date=\"$Date$\"]");
			LocaleIDParser lip = new LocaleIDParser();
			lip.set(dataSource.getLocaleID());
			identitySet.add(ldml_identity + "/language[@type=\"" + lip.getLanguage() + "\"]");
			if (lip.getScript().length() != 0) {
				identitySet.add(ldml_identity + "/script[@type=\"" + lip.getScript() + "\"]");
			}
			if (lip.getRegion().length() != 0) {
				identitySet.add(ldml_identity + "/territory[@type=\"" + lip.getRegion() + "\"]");
			}
			String[] variants = lip.getVariants();
			for (int i = 0; i < variants.length; ++i) {
				identitySet.add(ldml_identity + "/variant[@type=\"" + variants[i] + "\"]");
			}
		}
		// now do the rest
		
		XPathParts.writeComment(pw, 0, dataSource.getXpathComments().getInitialComment(), false);
		
		XPathParts.Comments tempComments = (XPathParts.Comments) dataSource.getXpathComments().clone();
		
		MapComparator modAttComp = attributeOrdering;
		if (HACK_ORDER) modAttComp = new MapComparator()
			.add("alt").add("draft").add(modAttComp.getOrder());

		XPathParts last = new XPathParts(attributeOrdering, defaultSuppressionMap);
		XPathParts current = new XPathParts(attributeOrdering, defaultSuppressionMap);
		XPathParts lastFiltered = new XPathParts(attributeOrdering, defaultSuppressionMap);
		XPathParts currentFiltered = new XPathParts(attributeOrdering, defaultSuppressionMap);

		for (Iterator it2 = identitySet.iterator(); it2.hasNext();) {
			String xpath = (String)it2.next();
			currentFiltered.set(xpath);
			current.set(xpath);
			current.writeDifference(pw, currentFiltered, last, lastFiltered, "", tempComments);
			// exchange pairs of parts
			XPathParts temp = current;
			current = last;
			last = temp;
			temp = currentFiltered;
			currentFiltered = lastFiltered;
			lastFiltered = temp;
		}
		
		for (Iterator it2 = orderedSet.iterator(); it2.hasNext();) {
			String xpath = (String)it2.next();
			//Value v = (Value) getXpath_value().get(xpath);
			currentFiltered.set(xpath);
			if (currentFiltered.getElement(1).equals("identity")) continue;
			current.set(getFullXPath(xpath));
			current.writeDifference(pw, currentFiltered, last, lastFiltered, getStringValue(xpath), tempComments);
			// exchange pairs of parts
			XPathParts temp = current;
			current = last;
			last = temp;
			temp = currentFiltered;
			currentFiltered = lastFiltered;
			lastFiltered = temp;
		}
		current.clear().writeDifference(pw, null, last, lastFiltered, null, tempComments);
		String finalComment = dataSource.getXpathComments().getFinalComment();
		
		// write comments that no longer have a base
		List x = tempComments.extractCommentsWithoutBase();
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
	 * Get a string value from an xpath.
	 */
    public String getStringValue(String xpath) {
    	return dataSource.getValueAtPath(xpath);
    }
    
	/**
	 * Get a string value from an xpath.
	 */
    public String getFullXPath(String xpath) {
    	return dataSource.getFullPath(xpath);
    }
    
	/**
	 * Find out where the value was found (for resolving locales)
	 * @param status the distinguished path where the item was found. Pass in null if you don't care.
	 */
    public String getSourceLocaleID(String xpath, CLDRFile.Status status) {
    	return dataSource.getSourceLocaleID(xpath, status);
    }
    
    /**
     * Add a new element to a CLDRFile.
     * @param currentFullXPath
     * @param value
     */
    public CLDRFile add(String currentFullXPath, String value) {
    	if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
    	//StringValue v = new StringValue(value, currentFullXPath);
    	Log.logln(LOG_PROGRESS, "ADDING: \t" + currentFullXPath + " \t" + value + "\t" + currentFullXPath);
    	//xpath = xpath.intern();
        try {
			dataSource.putValueAtPath(currentFullXPath, value);
		} catch (RuntimeException e) {
			throw (IllegalArgumentException) new IllegalArgumentException("failed adding " + currentFullXPath + ",\t" + value).initCause(e);
		}
        return this;
    }
    
    public CLDRFile addComment(String xpath, String comment, int type) {
    	if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
    	// System.out.println("Adding comment: <" + xpath + "> '" + comment + "'");
    	Log.logln(LOG_PROGRESS, "ADDING Comment: \t" + type + "\t" + xpath + " \t" + comment);
    	if (xpath == null || xpath.length() == 0) {
    		dataSource.getXpathComments().setFinalComment(
    				Utility.joinWithSeparation(dataSource.getXpathComments().getFinalComment(), XPathParts.NEWLINE, comment));
    	} else {
         	xpath = getDistinguishingXPath(xpath, null, false);
	        dataSource.getXpathComments().addComment(type, xpath, comment);
    	}
    	return this;
    }

    static final public int 
		MERGE_KEEP_MINE = 0, 
		MERGE_REPLACE_MINE = 1, 
		MERGE_ADD_ALTERNATE = 2, 
		MERGE_REPLACE_MY_DRAFT = 3;
    /**
     * Merges elements from another CLDR file. Note: when both have the same xpath key, 
     * the keepMine determines whether "my" values are kept
     * or the other files values are kept.
     * @param other
     * @param keepMine if true, keep my values in case of conflict; otherwise keep the other's values.
     */
    public CLDRFile putAll(CLDRFile other, int conflict_resolution) {
    	if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
		XPathParts parts = new XPathParts(null, null);
    	if (conflict_resolution == MERGE_KEEP_MINE) {
    		Map temp = isNonInheriting() ? new TreeMap() : new TreeMap(ldmlComparator);
    		dataSource.putAll(other.dataSource, MERGE_KEEP_MINE);
    	} else if (conflict_resolution == MERGE_REPLACE_MINE) {
    		dataSource.putAll(other.dataSource, MERGE_REPLACE_MINE);
    	} else if (conflict_resolution == MERGE_REPLACE_MY_DRAFT) {
    		// first find all my alt=..proposed items
    		Set hasDraftVersion = new HashSet();
    		for (Iterator it = dataSource.iterator(); it.hasNext();) {
    			String cpath = (String) it.next();
    			String fullpath = getFullXPath(cpath);
    			if (fullpath.indexOf("[@draft") >= 0) {
    				hasDraftVersion.add(getNondraftNonaltXPath(cpath)); // strips the alt and the draft
    			}
    		}
    		// only replace draft items!
    		// this is either an item with draft in the fullpath
    		// or an item with draft and alt in the full path
    		for (Iterator it = other.iterator(); it.hasNext();) {
				String cpath = (String) it.next();
				//Value otherValueOld = (Value) other.getXpath_value().get(cpath);
				// fix the data
				//cpath = Utility.replace(cpath, "[@type=\"ZZ\"]", "[@type=\"QO\"]"); // fix because tag meaning changed after beta
				cpath = getNondraftNonaltXPath(cpath);
				String newValue = other.getStringValue(cpath);
				String newFullPath = getNondraftNonaltXPath(other.getFullXPath(cpath));
				// newFullPath = Utility.replace(newFullPath, "[@type=\"ZZ\"]", "[@type=\"QO\"]");
				// another hack; need to add references back in
				newFullPath = addReferencesIfNeeded(newFullPath, getFullXPath(cpath));
				//Value otherValue = new StringValue(newValue, newFullPath);
				
				if (!hasDraftVersion.contains(cpath)) {
					if (cpath.startsWith("//ldml/identity/")) continue; // skip, since the error msg is not needed.
					String myVersion = getStringValue(cpath);
					if (myVersion == null || !newValue.equals(myVersion)) {
						Log.logln(getLocaleID() + "\tDenied attempt to replace non-draft\r\n\tcurr: [" + cpath + ",\t"
								+ myVersion + "]\r\n\twith: [" + newValue + "]");
						continue;
					}
				}
				Log.logln(getLocaleID() + "\tVETTED: [" + newFullPath + ",\t" + newValue + "]");
				dataSource.putValueAtPath(newFullPath, newValue);
    		}
    	} else if (conflict_resolution == MERGE_ADD_ALTERNATE){
    		for (Iterator it = other.iterator(); it.hasNext();) {
    			String key = (String) it.next();
    			String otherValue = other.getStringValue(key);
    			String myValue = dataSource.getValueAtPath(key);
    			if (myValue == null) {
    				dataSource.putValueAtPath(other.getFullXPath(key), otherValue);
    			} else if (!(myValue.equals(otherValue) 
    						&& equalsIgnoringDraft(getFullXPath(key), other.getFullXPath(key)))
    					&& !key.startsWith("//ldml/identity")){
    				for (int i = 0; ; ++i) {
    					String prop = "proposed" + (i == 0 ? "" : String.valueOf(i));
    					String fullPath = parts.set(other.getFullXPath(key)).addAttribute("alt", prop).toString();
    					String path = getDistinguishingXPath(fullPath, null, false);
    					if (dataSource.getValueAtPath(path) != null) continue;
    					dataSource.putValueAtPath(fullPath, otherValue);
    					break;
    				}
    			}
    		}
    	} else throw new IllegalArgumentException("Illegal operand: " + conflict_resolution);
    	    	
    	dataSource.getXpathComments().setInitialComment(
    			Utility.joinWithSeparation(dataSource.getXpathComments().getInitialComment(),
    					XPathParts.NEWLINE, 
						other.dataSource.getXpathComments().getInitialComment()));
    	dataSource.getXpathComments().setFinalComment(
    			Utility.joinWithSeparation(dataSource.getXpathComments().getFinalComment(), 
    					XPathParts.NEWLINE, 
						other.dataSource.getXpathComments().getFinalComment()));
    	dataSource.getXpathComments().joinAll(other.dataSource.getXpathComments());
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
	 * 
	 */
	private String addReferencesIfNeeded(String newFullPath, String fullXPath) {
		if (fullXPath == null || fullXPath.indexOf("[@references=") < 0) return newFullPath;
		XPathParts parts = new XPathParts(null, null).set(fullXPath);
		String accummulatedReferences = null;
		for (int i = 0; i < parts.size(); ++i) {
			Map attributes = parts.getAttributes(i);
			String references = (String) attributes.get("references");
			if (references == null) continue;
			if (accummulatedReferences == null) accummulatedReferences = references;
			else accummulatedReferences += ", " + references; 
		}
		if (accummulatedReferences == null) return newFullPath;
		XPathParts newParts = new XPathParts(null, null).set(newFullPath);
		Map attributes = newParts.getAttributes(newParts.size()-1);
		String references = (String) attributes.get("references");
		if (references == null) references = accummulatedReferences;
		else references += ", " + accummulatedReferences;
		attributes.put("references", references);
		System.out.println("Changing " + newFullPath + " plus " + fullXPath + " to " + newParts.toString());
		return newParts.toString();
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
    		//CLDRFile.Value v = getValue(xpath);
    		appendFinalComment(dataSource.getFullPath(xpath)+ "::<" + dataSource.getValueAtPath(xpath) + ">");
    	}
    	dataSource.removeValueAtPath(xpath);
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
    	for (Iterator it = other.iterator(); it.hasNext();) {
    		String xpath = (String)it.next();
    		String currentValue = dataSource.getValueAtPath(xpath);
    		if (currentValue == null) continue;
    		String otherValue = other.dataSource.getValueAtPath(xpath);
    		if (!currentValue.equals(otherValue)) continue;
    		String currentFullXPath = dataSource.getFullPath(xpath);
    		String otherFullXPath = other.dataSource.getFullPath(xpath);
    		if (!equalsIgnoringDraft(currentFullXPath, otherFullXPath)) continue;
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
		return dataSource.getXpathComments().getFinalComment();
	}
	/**
	 * @return Returns the finalComment.
	 */
	public String getInitialComment() {
		return dataSource.getXpathComments().getInitialComment();
	}
	/**
	 * @return Returns the xpath_comments. Cloned for safety.
	 */
	public XPathParts.Comments getXpath_comments() {
		return (XPathParts.Comments) dataSource.getXpathComments().clone();
	}
	/**
	 * @return Returns the locale ID. In the case of a supplemental data file, it is SUPPLEMENTAL_NAME.
	 */
	public String getLocaleID() {
		return dataSource.getLocaleID();
	}

	/**
	 * @see com.ibm.icu.util.Freezable#isFrozen()
	 */
	public synchronized boolean isFrozen() {
		return locked;
	}

	/**
	 * @see com.ibm.icu.util.Freezable#freeze()
	 */
	public synchronized Object freeze() {
		locked = true;
		dataSource.freeze();
		return this;
	}
	
	public CLDRFile clearComments() {
    	if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
		dataSource.setXpathComments(new XPathParts.Comments());
		return this;
	}
	/**
	 * Sets a final comment, replacing everything that was there.
	 */
	public CLDRFile setFinalComment(String comment) {
    	if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
		dataSource.getXpathComments().setFinalComment(comment);
		return this;
	}

	/**
	 * Adds a comment to the final list of comments.
	 */
	public CLDRFile appendFinalComment(String comment) {
    	if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
		dataSource.getXpathComments().setFinalComment(Utility.joinWithSeparation(dataSource.getXpathComments().getFinalComment(), XPathParts.NEWLINE, comment));
		return this;
	}

	/**
	 * Sets the initial comment, replacing everything that was there.
	 */
	public CLDRFile setInitialComment(String comment) {
    	if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
    	dataSource.getXpathComments().setInitialComment(comment);
    	return this;
	}

	// ========== STATIC UTILITIES ==========
	
    /**
	 * Utility to restrict to files matching a given regular expression. The expression does not contain ".xml".
	 * Note that supplementalData is always skipped, and root is always included.
	 */
    public static Set getMatchingXMLFiles(String sourceDir, Matcher m) {
        Set s = new TreeSet();
        File[] files = new File(sourceDir).listFiles();
        for (int i = 0; i < files.length; ++i) {
            String name = files[i].getName();
            if (!name.endsWith(".xml")) continue;
            //if (name.startsWith(SUPPLEMENTAL_NAME)) continue;
            String locale = name.substring(0,name.length()-4); // drop .xml
            if (!m.reset(locale).matches()) continue;
            s.add(locale);
        }
        return s;
    }

    /**
     * Returns a collection containing the keys for this file.
      */
//    public Set keySet() {
//    	return (Set) CollectionUtilities.addAll(dataSource.iterator(), new HashSet());
//     }
    
    public Iterator iterator() {
    	return dataSource.iterator();
     }

    public Iterator iterator(String prefix) {
    	return dataSource.iterator(prefix);
     }

    public Iterator iterator(String prefix, Comparator comparator) {
        Iterator it = (prefix == null || prefix.length() == 0) 
            ? dataSource.iterator() 
            : dataSource.iterator(prefix);
        if (comparator == null) return it;
        Set orderedSet = new TreeSet(CLDRFile.ldmlComparator);
        CollectionUtilities.addAll(it, orderedSet);
        return orderedSet.iterator();
     }

 
    public static String getDistinguishingXPath(String xpath, String[] normalizedPath, boolean nonInheriting) {
    	return distinguishedXPath.getDistinguishingXPath(xpath, normalizedPath, nonInheriting);
    }
    
    private static boolean equalsIgnoringDraft(String path1, String path2) {
    	// TODO: optimize
    	if (path1.indexOf("[@draft=") < 0 && path2.indexOf("[@draft=") < 0) return path1.equals(path2);
		return getNondraftNonaltXPath(path1).equals(getNondraftNonaltXPath(path2));
    }
    
    static XPathParts nondraftParts = new XPathParts(null,null);
    
    private static String getNondraftNonaltXPath(String xpath) {
    	if (xpath.indexOf("draft=\"") < 0 && xpath.indexOf("alt=\"") < 0 ) return xpath;
    	synchronized (nondraftParts) {
	    	XPathParts parts = new XPathParts(null,null).set(xpath);
	    	String restore;
	    	for (int i = 0; i < parts.size(); ++i) {
	    		String element = parts.getElement(i);
	    		Map attributes = parts.getAttributes(i);
	    		restore = null;
	    		for (Iterator it = attributes.keySet().iterator(); it.hasNext();) {
	    			String attribute = (String) it.next();
	    			if (attribute.equals("draft")) it.remove();
	    			else if (attribute.equals("alt")) {
	    				String value = (String) attributes.get(attribute);		
		    			int proposedPos = value.indexOf("proposed");
		    			if (proposedPos >= 0) {
		    				it.remove();
		    				if (proposedPos > 0) {
		    					restore = value.substring(0, proposedPos-1); // is of form xxx-proposedyyy
		    				}
		    			}
	    			}
	    		}
	    		if (restore != null) attributes.put("alt", restore);
	    	}
	    	return parts.toString();
    	}
    }

//    private static String getNondraftXPath(String xpath) {
//    	if (xpath.indexOf("draft=\"") < 0) return xpath;
//    	synchronized (nondraftParts) {
//	    	XPathParts parts = new XPathParts(null,null).set(xpath);
//	    	for (int i = 0; i < parts.size(); ++i) {
//	    		Map attributes = parts.getAttributes(i);
//	    		for (Iterator it = attributes.keySet().iterator(); it.hasNext();) {
//	    			String attribute = (String) it.next();
//	    			if (attribute.equals("draft")) it.remove();
//	    		}
//	    	}
//	    	return parts.toString();
//    	}
//    }
    
	/**
	 * Determine if an attribute is a distinguishing attribute.
	 * @param elementName
	 * @param attribute
	 * @return
	 */
	private static boolean isDistinguishing(String elementName, String attribute) {
		boolean result =
		attribute.equals("key") 
        || attribute.equals("request") 
        || attribute.equals("id") 
		|| attribute.equals("_q") 
		|| attribute.equals("registry") 
		|| attribute.equals("alt")
		|| attribute.equals("iso4217")
		|| attribute.equals("iso3166")
		|| (attribute.equals("type") 
				&& !elementName.equals("default") 
				&& !elementName.equals("measurementSystem") 
				&& !elementName.equals("mapping")
				&& !elementName.equals("abbreviationFallback")
				&& !elementName.equals("preferenceOrdering"))
		|| elementName.equals("deprecatedItems");
//		if (result != matches(distinguishingAttributeMap, new String[]{elementName, attribute}, true)) {
//			matches(distinguishingAttributeMap, new String[]{elementName, attribute}, true);
//			throw new IllegalArgumentException("Failed: " + elementName + ", " + attribute);
//		}
		return result;
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
        private Map mainCacheNoDraft = new TreeMap();
        private Map resolvedCacheNoDraft = new TreeMap();  
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
            Matcher m = Pattern.compile(matchString).matcher("");
			result.localeList = getMatchingXMLFiles(sourceDirectory, m);
            try {
                result.localeList.addAll(getMatchingXMLFiles(sourceDirectory + "/../supplemental/", m));
            } catch(Throwable t) {
                throw new Error("CLDRFile unable to load Supplemental data: couldn't getMatchingXMLFiles("+sourceDirectory + "/../supplemental"+")",t);
            }
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
	    
        public CLDRFile make(String localeName, boolean resolved) {
        	return make(localeName, resolved, true);
        }
	    /**
	     * Make a CLDR file. The result is a locked file, so that it can be cached. If you want to modify it,
	     * use clone().
	     */
	    // TODO resolve aliases
		public CLDRFile make(String localeName, boolean resolved, boolean includeDraft) {
			// TODO fix hack: 
			// read root first so that we get the ordering right.
/*			if (needToReadRoot) {
				if (!localeName.equals("root")) make("root", false);
				needToReadRoot = false;
			}
*/			// end of hack
	    	Map cache = includeDraft ? (resolved ? resolvedCache : mainCache) 
                    : (resolved ? resolvedCacheNoDraft : mainCacheNoDraft);
	    	CLDRFile result = (CLDRFile) cache.get(localeName);
	    	if (result == null) {
    			result = CLDRFile.make(localeName, isSupplementalName(localeName) ? sourceDirectory + File.separator + "../supplemental/" : sourceDirectory, includeDraft);
    	    	((SimpleXMLSource)result.dataSource).factory = this;
    			if (resolved) {
    				result.dataSource = result.dataSource.getResolving();
	    		} else {
		    		result.freeze();	    			
	    		}
	    		cache.put(localeName, result);
	    	}
	    	return result;
	    }

	}
	
    public static boolean isSupplementalName(String localeName) {
        return localeName.startsWith(SUPPLEMENTAL_PREFIX) || localeName.equals("characters");
    }

//	static String[] keys = {"calendar", "collation", "currency"};
//	
//	static String[] calendar_keys = {"buddhist", "chinese", "gregorian", "hebrew", "islamic", "islamic-civil", "japanese"};
//	static String[] collation_keys = {"phonebook", "traditional", "direct", "pinyin", "stroke", "posix", "big5han", "gb2312han"};
	
	
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
        private static UnicodeSet whitespace = new UnicodeSet("[:whitespace:]");
    	private boolean includeDraft;
		private static final boolean SHOW_START_END = false;
    	private int commentStack;
    	private boolean justPopped = false;
    	private String lastChars = "";
    	//private String currentXPath = "/";
    	private String currentFullXPath = "/";
        private String comment = null;
    	private Map attributeOrder = new TreeMap(attributeOrdering);
    	private CLDRFile target;
    	private String lastActiveLeafNode;
    	private String lastLeafNode;
    	private boolean isSupplemental;
		private int orderedCounter;
    	
    	MyDeclHandler(CLDRFile target, boolean includeDraft) {
    		this.target = target;
            this.includeDraft = includeDraft;
    		isSupplemental = isSupplementalName(target.getLocaleID());
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
    		Log.logln(LOG_PROGRESS, "push\t" + qName + "\t" + show(attributes));
        	if (lastChars.length() != 0) {
                if (whitespace.containsAll(lastChars)) lastChars = "";
                else throw new IllegalArgumentException("Internal Error");
            }
    		//currentXPath += "/" + qName;
    		currentFullXPath += "/" + qName;
    		//if (!isSupplemental) ldmlComparator.addElement(qName);
    		if (orderedElements.contains(qName)) {
    			currentFullXPath += "[@_q=\"" + (orderedCounter++) + "\"]";
    		}
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
	    			//if (isDistinguishing(qName, attribute)) {
	    			//	currentXPath += both;
	    			//}
	    		}
    		}
    		if (comment != null) {
    			target.addComment(currentFullXPath, comment, XPathParts.Comments.PREBLOCK);
    			comment = null;
    		}
            justPopped = false;
            lastActiveLeafNode = null;
    		Log.logln(LOG_PROGRESS, "currentFullXPath\t" + currentFullXPath);
    	}
    	
		private void pop(String qName) {
			Log.logln(LOG_PROGRESS, "pop\t" + qName);
            if (lastChars.length() != 0 || justPopped == false) {
                if (includeDraft || currentFullXPath.indexOf("[@draft=\"true\"]") < 0) {
                    if (false && currentFullXPath.indexOf("i-klingon") >= 0) {
                        System.out.println(currentFullXPath);
                    }
                	target.add(currentFullXPath, lastChars);
                    lastLeafNode = lastActiveLeafNode = currentFullXPath;
                }
                lastChars = "";
            } else {
            	Log.logln(LOG_PROGRESS && lastActiveLeafNode != null, "pop: zeroing last leafNode: " + lastActiveLeafNode);
            	lastActiveLeafNode = null;
        		if (comment != null) {
        			target.addComment(lastLeafNode, comment, XPathParts.Comments.POSTBLOCK);
        			comment = null;
        		}
            }
			//currentXPath = stripAfter(currentXPath, qName);
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
        		Log.logln(LOG_PROGRESS || SHOW_START_END, "startElement uri\t" + uri
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
    			Log.logln(LOG_PROGRESS || SHOW_START_END, "endElement uri\t" + uri + "\tlocalName " + localName
    				+ "\tqName " + qName);
                try {
                    pop(qName);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    throw e;
                }
            }
        
        static final char XML_LINESEPARATOR = (char)0xA;
        static final String XML_LINESEPARATOR_STRING = String.valueOf(XML_LINESEPARATOR);
        
        public void characters(char[] ch, int start, int length)
            throws SAXException {
                try {
                    String value = new String(ch,start,length);
                    Log.logln(LOG_PROGRESS, "characters:\t" + value);
                    while (value.startsWith(XML_LINESEPARATOR_STRING) && lastChars.length() == 0) {
                    	value = value.substring(1);
                    }
                    if (value.indexOf(XML_LINESEPARATOR) >= 0) value = value.replace(XML_LINESEPARATOR, '\u0020');
                    lastChars += value;
                    justPopped = false;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    throw e;
                }
            }

        public void startDTD(String name, String publicId, String systemId) throws SAXException {
            Log.logln(LOG_PROGRESS, "startDTD name: " + name
                    + ", publicId: " + publicId
                    + ", systemId: " + systemId
            );
            commentStack++;
        }
        public void endDTD() throws SAXException {
            Log.logln(LOG_PROGRESS, "endDTD");
            commentStack--;
        }
        
        public void comment(char[] ch, int start, int length) throws SAXException {
            Log.logln(LOG_PROGRESS, commentStack + " comment " + new String(ch, start,length));
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
            Log.logln(LOG_PROGRESS, "ignorableWhitespace length: " + length);
            for (int i = 0; i < ch.length; ++i) {
            	if (ch[i] == '\n') {
            		Log.logln(LOG_PROGRESS && lastActiveLeafNode != null, "\\n: zeroing last leafNode: " + lastActiveLeafNode);
            		lastActiveLeafNode = null;
            	}
            }
        }
        public void startDocument() throws SAXException {
            Log.logln(LOG_PROGRESS, "startDocument");
            commentStack = 0; // initialize
        }

        public void endDocument() throws SAXException {
            Log.logln(LOG_PROGRESS, "endDocument");
            try {
				if (comment != null) target.addComment(null, comment, XPathParts.Comments.LINE);
			} catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
			}
        }

        // ==== The following are just for debuggin =====

		public void elementDecl(String name, String model) throws SAXException {
        	Log.logln(LOG_PROGRESS, "Attribute\t" + name + "\t" + model);
        }
        public void attributeDecl(String eName, String aName, String type, String mode, String value) throws SAXException {
            Log.logln(LOG_PROGRESS, "Attribute\t" + eName + "\t" + aName + "\t" + type + "\t" + mode + "\t" + value);
        }
        public void internalEntityDecl(String name, String value) throws SAXException {
        	Log.logln(LOG_PROGRESS, "Internal Entity\t" + name + "\t" + value);
        }
        public void externalEntityDecl(String name, String publicId, String systemId) throws SAXException {
        	Log.logln(LOG_PROGRESS, "Internal Entity\t" + name + "\t" + publicId + "\t" + systemId);
        }

        public void notationDecl (String name, String publicId, String systemId){
            Log.logln(LOG_PROGRESS, "notationDecl: " + name
            + ", " + publicId
            + ", " + systemId
            );
        }

        public void processingInstruction (String target, String data)
        throws SAXException {
            Log.logln(LOG_PROGRESS, "processingInstruction: " + target + ", " + data);
        }

        public void skippedEntity (String name)
        throws SAXException {
            Log.logln(LOG_PROGRESS, "skippedEntity: " + name);
        }

        public void unparsedEntityDecl (String name, String publicId,
                        String systemId, String notationName) {
            Log.logln(LOG_PROGRESS, "unparsedEntityDecl: " + name
            + ", " + publicId
            + ", " + systemId
            + ", " + notationName
            );
        }
        
        public void setDocumentLocator(Locator locator) {
            Log.logln(LOG_PROGRESS, "setDocumentLocator Locator " + locator);
        }
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            Log.logln(LOG_PROGRESS, "startPrefixMapping prefix: " + prefix +
                    ", uri: " + uri);
        }
        public void endPrefixMapping(String prefix) throws SAXException {
            Log.logln(LOG_PROGRESS, "endPrefixMapping prefix: " + prefix);
        }
        public void startEntity(String name) throws SAXException {
            Log.logln(LOG_PROGRESS, "startEntity name: " + name);
        }
        public void endEntity(String name) throws SAXException {
            Log.logln(LOG_PROGRESS, "endEntity name: " + name);
        }
        public void startCDATA() throws SAXException {
            Log.logln(LOG_PROGRESS, "startCDATA");
        }
        public void endCDATA() throws SAXException {
            Log.logln(LOG_PROGRESS, "endCDATA");
        }

		/* (non-Javadoc)
		 * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
		 */
		public void error(SAXParseException exception) throws SAXException {
			Log.logln(LOG_PROGRESS || true, "error: " + showSAX(exception));
			throw exception;
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
		 */
		public void fatalError(SAXParseException exception) throws SAXException {
			Log.logln(LOG_PROGRESS, "fatalError: " + showSAX(exception));
			throw exception;
		}

		/* (non-Javadoc)
		 * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
		 */
		public void warning(SAXParseException exception) throws SAXException {
			Log.logln(LOG_PROGRESS, "warning: " + showSAX(exception));
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
	private CLDRFile fixAliases(Factory factory, boolean includeDraft) {
		// walk through the entire tree. If we ever find an alias, 
		// remove every peer of that alias,
		// then add everything from the resolved source of the alias.
		List aliases = new ArrayList();
		for (Iterator it = iterator(); it.hasNext();) {
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
			//Value v = (Value) getXpath_value().get(xpathKey);
			parts.set(xpathKey);
			int index = parts.findElement("alias"); // can have no children
			if (index < 0) continue;
			parts.trimLast();
			fullParts.set(dataSource.getFullPath(xpathKey));
			Map attributes = fullParts.getAttributes(index);
			fullParts.trimLast();
			// <alias source="<locale_ID>" path="..."/>
			String source = (String) attributes.get("source");
			if (source == null || source.equals("locale")) source = dataSource.getLocaleID();
			otherParts.set(parts);
			String otherPath = (String) attributes.get("path");
			if (otherPath != null) {
				otherParts.addRelative(otherPath);
			}
			//removeChildren(parts);  WARNING: leave the alias in the resolved one, for now.
			CLDRFile other;
			if (source.equals(dataSource.getLocaleID())) {
				other = this; 
			} else {				
				try {
					other = factory.make(source, true, includeDraft);
				} catch (RuntimeException e) {
					System.err.println("Bad alias");
					e.printStackTrace();
					throw e;
				}
			}
			addChildren(parts, fullParts, other, otherParts);
		}		
		return this;
	}

	/**
	 * Search through the other CLDRFile, and add anything that starts with otherParts.
	 * The new path will be fullParts + 
	 * @param parts
	 * @param other
	 * @param otherParts
	 */
	private CLDRFile addChildren(XPathParts parts, XPathParts fullParts, CLDRFile other, XPathParts otherParts) {
		String otherPath = otherParts + "/";
		XPathParts temp = new XPathParts(attributeOrdering, defaultSuppressionMap);
		XPathParts fullTemp = new XPathParts(attributeOrdering, defaultSuppressionMap);
		Map stuffToAdd = new HashMap();
		for (Iterator it = other.iterator(); it.hasNext();) {
			String path = (String)it.next();
			if (path.startsWith(otherPath)) {
				//Value value = (Value) other.getXpath_value().get(path);
				//temp.set(path);
				//temp.replace(otherParts.size(), parts);
				fullTemp.set(other.dataSource.getFullPath(path));
				fullTemp.replace(otherParts.size(), fullParts);
				String newPath = fullTemp.toString();
				String value = dataSource.getValueAtPath(path);
				//value = value.changePath(fullTemp.toString());
				if (SHOW_ALIAS_FIXES) System.out.println("Adding*: " + path + ";\r\n\t" + newPath + ";\r\n\t" 
						+ dataSource.getValueAtPath(path));
				stuffToAdd.put(newPath, value);
				// to do, fix path
			}
		}
		dataSource.putAll(stuffToAdd, MERGE_REPLACE_MINE);
		return this;
	}

	/**
	 * @param parts
	 */
	private CLDRFile removeChildren(XPathParts parts) {
		String mypath = parts + "/";
		Set temp = new HashSet();
		for (Iterator it = iterator(); it.hasNext();) {
			String path = (String)it.next();
			if (path.startsWith(mypath)) {
				//if (false) System.out.println("Removing: " + getXpath_value().get(path));
				temp.add(path);
			}
		}
		dataSource.removeAll(temp);
		return this;
	}

	/**
	 * Says whether the whole file is draft
	 */
	public boolean isDraft() {
		String item = (String) iterator().next();
		return item.startsWith("//ldml[@draft=\"true\"]");
	}
	
//	public Collection keySet(Matcher regexMatcher, Collection output) {
//		if (output == null) output = new ArrayList(0);
//		for (Iterator it = keySet().iterator(); it.hasNext();) {
//			String path = (String)it.next();
//			if (regexMatcher.reset(path).matches()) {
//				output.add(path);
//			}
//		}
//		return output;
//	}
	
//	public Collection keySet(String regexPattern, Collection output) {
//		return keySet(Pattern.compile(regexPattern).matcher(""), output);
//	}
	
	/**
	 * Gets the type of a given xpath, eg script, territory, ...
	 * TODO move to separate class
	 * @param xpath
	 * @return
	 */
	public static int getNameType(String xpath) {
		for (int i = 0; i < NameTable.length; ++i) {
			if (!xpath.startsWith(NameTable[i][0])) continue;
			if (xpath.indexOf(NameTable[i][1], NameTable[i][0].length()) >= 0) return i;
		}
		return -1;
	}
	
	/**
	 * Gets the display name for a type
	 */
	public static String getNameTypeName(int index) {
		try {
			return getNameName(index);
		} catch (Exception e) {
			return "Illegal Type Name: " + index;
		}
	}
	
	public static final int NO_NAME = -1, LANGUAGE_NAME = 0, SCRIPT_NAME = 1, TERRITORY_NAME = 2, VARIANT_NAME = 3,
	CURRENCY_NAME = 4, CURRENCY_SYMBOL = 5, 
	TZ_EXEMPLAR = 6, TZ_START = TZ_EXEMPLAR,
	TZ_GENERIC_LONG = 7, TZ_GENERIC_SHORT = 8,
	TZ_STANDARD_LONG = 9, TZ_STANDARD_SHORT = 10,
	TZ_DAYLIGHT_LONG = 11, TZ_DAYLIGHT_SHORT = 12,
	TZ_LIMIT = 13,
	LIMIT_TYPES = 13;

	private static final String[][] NameTable = {
			{"//ldml/localeDisplayNames/languages/language[@type=\"", "\"]", "language"},
			{"//ldml/localeDisplayNames/scripts/script[@type=\"", "\"]", "script"},
			{"//ldml/localeDisplayNames/territories/territory[@type=\"", "\"]", "territory"},
			{"//ldml/localeDisplayNames/variants/variant[@type=\"", "\"]", "variant"},
			{"//ldml/numbers/currencies/currency[@type=\"", "\"]/displayName", "currency"},
			{"//ldml/numbers/currencies/currency[@type=\"", "\"]/symbol", "currency-symbol"},
			{"//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/exemplarCity", "exemplar-city"},
			{"//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/long/generic", "tz-generic-long"},
			{"//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/short/generic", "tz-generic-short"},
			{"//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/long/standard", "tz-standard-long"},
			{"//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/short/standard", "tz-standard-short"},
			{"//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/long/daylight", "tz-daylight-long"},
			{"//ldml/dates/timeZoneNames/zone[@type=\"", "\"]/short/daylight", "tz-daylight-short"},
			
			/**
			 * <long>
<generic>Newfoundland Time</generic>
<standard>Newfoundland Standard Time</standard>
<daylight>Newfoundland Daylight Time</daylight>
</long>
-
	<short>
<generic>NT</generic>
<standard>NST</standard>
<daylight>NDT</daylight>
</short>
			 */
	};

//	private static final String[] TYPE_NAME = {"language", "script", "territory", "variant", "currency", "currency-symbol",
//		"tz-exemplar",
//		"tz-generic-long", "tz-generic-short"};
	
	/**
	 * @return the key used to access  data of a given type
	 */
	public static String getKey(int type, String code) {
		return NameTable[type][0] + code + NameTable[type][1];
	}
	/**
	 * @return the code used to access  data of a given type from the path. Null if not found.
	 */
	public static String getCode(String path) {
		int type = getNameType(path);
		if (type < 0) {
			throw new IllegalArgumentException("Illegal type in path: " + path);
		}
		int start = NameTable[type][0].length();
		int end = path.indexOf(NameTable[type][1], start);
		return path.substring(start, end);
	}
	/**
	 * Utility for getting the name, given a code.
	 * @param type
	 * @param code
	 * @param skipDraft
	 * @return
	 */
	public String getName(int type, String code, boolean skipDraft) {
		String path = getKey(type, code);
		if (skipDraft && dataSource.isDraft(path)) return null;
		return getStringValue(path);
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
	public static int typeNameToCode(String type) {
		for (int i = 0; i < LIMIT_TYPES; ++i) {
			if (type.equalsIgnoreCase(getNameName(i))) return i;
		}
		return -1;
	}

	transient LanguageTagParser lparser = new LanguageTagParser();
	
	public synchronized String getName(String localeOrTZID, boolean skipDraft) {
		lparser.set(localeOrTZID);
		String original;
		String name = getName(LANGUAGE_NAME, original = lparser.getLanguage(), skipDraft);
		if (name == null) name = original;
		String sname = original = lparser.getScript();
		if (sname.length() != 0) {
			sname = getName(SCRIPT_NAME, sname, skipDraft);
			name += " - " + (sname == null ? original : sname);
		}
		String extras = "";
		original = sname = lparser.getRegion();
		if (sname.length() != 0) {
			if (extras.length() != 0) extras += ", ";
			sname = getName(TERRITORY_NAME, sname, skipDraft);
			extras += (sname == null ? original : sname);
		}
		List variants = lparser.getVariants();
		for (int i = 0; i < variants.size(); ++i) {
			if (extras.length() != 0) extras += ", ";
			sname = getName(VARIANT_NAME, original = (String)variants.get(i), skipDraft);
			extras += (sname == null ? original : sname);
		}
		return name + (extras.length() == 0 ? "" : " (" + extras + ")");
	}
	
	/**
	 * Returns the name of a type.
	 */
	public static String getNameName(int choice) {
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

	// note: run FindDTDOrder to get this list
	
	static MapComparator elementOrdering = (MapComparator) new MapComparator()
			.add(
					new String[] { "ldml", "identity", "alias",
							"localeDisplayNames", "layout", "characters",
							"delimiters", "measurement", "dates", "numbers",
							"collations", "posix", "segmentations",
							"references", "version", "generation", "language",
							"script", "territory", "variant", "languages",
							"scripts", "territories", "variants", "keys",
							"types", "measurementSystemNames", "key", "type",
							"measurementSystemName", "orientation", "inList",
							"exemplarCharacters", "mapping", "quotationStart",
							"quotationEnd", "alternateQuotationStart",
							"alternateQuotationEnd", "measurementSystem",
							"paperSize", "height", "width",
							"localizedPatternChars", "calendars",
							"timeZoneNames", "months", "monthNames",
							"monthAbbr", "days", "dayNames", "dayAbbr",
							"quarters", "week", "am", "pm", "eras",
							"dateFormats", "timeFormats", "dateTimeFormats",
							"fields", "month", "day", "quarter", "minDays",
							"firstDay", "weekendStart", "weekendEnd",
							"eraNames", "eraAbbr", "era", "pattern",
							"displayName", "dateFormatItem", "appendItem",
							"hourFormat", "hoursFormat", "gmtFormat",
							"regionFormat", "fallbackFormat",
							"abbreviationFallback", "preferenceOrdering",
							"singleCountries", "default", "calendar",
							"monthContext", "monthWidth", "dayContext",
							"dayWidth", "quarterContext", "quarterWidth",
							"dateFormatLength", "dateFormat",
							"timeFormatLength", "timeFormat",
							"dateTimeFormatLength", "availableFormats",
							"appendItems", "dateTimeFormat", "zone", "long",
							"short", "exemplarCity", "generic", "standard",
							"daylight", "field", "relative", "symbols",
							"decimalFormats", "scientificFormats",
							"percentFormats", "currencyFormats", "currencies",
							"decimalFormatLength", "decimalFormat",
							"scientificFormatLength", "scientificFormat",
							"percentFormatLength", "percentFormat",
							"currencySpacing", "currencyFormatLength",
							"beforeCurrency", "afterCurrency", "currencyMatch",
							"surroundingMatch", "insertBetween",
							"currencyFormat", "currency", "symbol", "decimal",
							"group", "list", "percentSign", "nativeZeroDigit",
							"patternDigit", "plusSign", "minusSign",
							"exponential", "perMille", "infinity", "nan",
							"collation", "messages", "yesstr", "nostr",
							"yesexpr", "noexpr", "segmentation", "variables",
							"segmentRules", "special", "variable", "rule", "comment",
							// collation
							"base", "settings", "suppress_contractions", "optimize", "rules"})
			.setErrorOnMissing(false).freeze();
	
	static MapComparator attributeOrdering = (MapComparator) new MapComparator()
			.add(new String[] { "_q", "type", 
							// always after
						    "key", "registry", "source", "target",
							"path", "day", "date", "version", "count", "lines",
							"characters", "before", "iso4217", "from", "to", "number",
							"time", "casing", "list", "uri",
							"digits", "rounding", "iso3166", "hex",
							"id", "request",
							"direction",
							// collation stuff
							"alternate", "backwards", "caseFirst", "caseLevel",
							"hiraganaQuarternary", "hiraganaQuaternary",
							"normalization", "numeric", "strength",
							// always near the end
							"validSubLocales", "standard", "references",
							"elements","element","attributes","attribute",
                            "scripts", "mostPopulousTerritory", "territories", 
							// these are always at the end
							"alt", "draft", }).setErrorOnMissing(false)
			.freeze();
	static MapComparator valueOrdering = (MapComparator) new MapComparator().setErrorOnMissing(false).freeze();
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
    		"sun", "mon", "tue", "wed", "thu", "fri", "sat"}).freeze();
    static MapComparator widthOrder = (MapComparator) new MapComparator().add(new String[] {
    		"abbreviated", "narrow", "wide"}).freeze();
    static MapComparator lengthOrder = (MapComparator) new MapComparator().add(new String[] {
    		"full", "long", "medium", "short"}).freeze();
    static MapComparator dateFieldOrder = (MapComparator) new MapComparator().add(new String[] {
    		"era", "year", "month", "week", "day", "weekday", "dayperiod",
			"hour", "minute", "second", "zone"}).freeze();
    static Comparator zoneOrder = StandardCodes.make().getTZIDComparator();
    
    static Set orderedElements = Collections.unmodifiableSet(new HashSet(java.util.Arrays
			.asList(new String[] {
					"variable", "comment", "tRule", "attributeValues", 
					// collation
					"base", "settings", "suppress_contractions", "optimize", "rules",
			//"dateFormatItem",
			// collation
					"reset", "p", "pc", "s", "sc", "t", "tc", "q", "qc", "i",
					"ic", "x", "extend", "first_variable", "last_variable",
					"first_tertiary_ignorable", "last_tertiary_ignorable",
					"first_secondary_ignorable", "last_secondary_ignorable",
					"first_primary_ignorable", "last_primary_ignorable",
					"first_non_ignorable", "last_non_ignorable",
					"first_trailing", "last_trailing" })));
	/**
	 * 
	 */
	public static Comparator getAttributeValueComparator(String element, String attribute) {
		Comparator comp = valueOrdering;
		if (attribute.equals("day")) { //  && (element.startsWith("weekend")
			comp = dayValueOrder;
		} else if (attribute.equals("type")) {
			if (element.endsWith("FormatLength")) comp = lengthOrder;
			else if (element.endsWith("Width")) comp = widthOrder;
			else if (element.equals("day")) comp = dayValueOrder;
			else if (element.equals("field")) comp = dateFieldOrder;
			else if (element.equals("zone")) comp = zoneOrder;
		}
		return comp;
	}		
    
    /**
     * Comparator for attributes in CLDR files
     */
	public static Comparator ldmlComparator = new LDMLComparator();

	static class LDMLComparator implements Comparator {

		transient XPathParts a = new XPathParts(attributeOrdering, null);
		transient XPathParts b = new XPathParts(attributeOrdering, null);
        transient Set attSet1 = new TreeSet(attributeOrdering);
        transient Set attSet2 = new TreeSet(attributeOrdering);
		
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
            if (o1 == o2) return 0; // quick test for common case
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
				if (0 != (result = elementOrdering.compare(aname, bname))) {
                    // if they are different, then 
                    // all ordered items are equal, and > than all unordered
                    boolean aOrdered = orderedElements.contains(aname);
                    boolean bOrdered = orderedElements.contains(bname);
                    // if both ordered, continue, return result
                    if (aOrdered && bOrdered) {
                        // continue with comparison
                    } else {
                        if (aOrdered == bOrdered) return result; // both off
                        return aOrdered ? 1 : 0;
                    }
                }
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
						Comparator comp = getAttributeValueComparator(aname, akey);
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
	
	static String[][] distinguishingData = {
				{"*", "key"},
				{"*", "id"},
				{"*", "_q"},
				{"*", "alt"},
				{"*", "iso4217"},
				{"*", "iso3166"},
				{"default", "type"},
				{"measurementSystem", "type"},
				{"mapping", "type"},
				{"abbreviationFallback", "type"},
				{"preferenceOrdering", "type"},
				{"deprecatedItems", "iso3166"},
		};

	private final static Map distinguishingAttributeMap = asMap(distinguishingData, true); 
	
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
				{"*", "_q", "*"},
		};
		Map tempmain = asMap(data, true);
		defaultSuppressionMap = Collections.unmodifiableMap(tempmain);
	}
	
	private static boolean matches(Map map, Object[] items, boolean doStar) {
		for (int i = 0; i < items.length - 2; ++i) {
			Map tempMap = (Map) map.get(items[i]);
			if (doStar && map == null) map = (Map) map.get("*");
			if (map == null) return false;
			map = tempMap;
		}
		return map.get(items[items.length-2]) == items[items.length-1];
	}

	private static Map asMap(String[][] data, boolean tree) {
		Map tempmain = tree ? (Map) new TreeMap() : new HashMap();
		int len = data[0].length; // must be same for all elements
		for (int i = 0; i < data.length; ++i) {
			Map temp = tempmain;
			if (len != data[i].length) {
				throw new IllegalArgumentException("Must be square array: fails row " + i);
			}
			for (int j = 0; j < len - 2; ++j) {
				Map newTemp = (Map) temp.get(data[i][j]);
				if (newTemp == null) temp.put(data[i][j], newTemp = tree ? (Map) new TreeMap() : new HashMap());
				temp = newTemp;
			}
			temp.put(data[i][len-2], data[i][len-1]);
		}
		return tempmain;
	}
	/**
	 * Removes a comment.
	 */
	public CLDRFile removeComment(String string) {
    	if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
		// TODO Auto-generated method stub
    	dataSource.getXpathComments().removeComment(string);
    	return this;
	}

	/**
	 * 
	 */
	public CLDRFile makeDraft() {
    	if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
    	XPathParts parts = new XPathParts(null,null);
    	for (Iterator it = dataSource.iterator(); it.hasNext();) {
    		String path = (String) it.next();
    		//Value v = (Value) getXpath_value().get(path);
    		//if (!(v instanceof StringValue)) continue;
    		parts.set(dataSource.getFullPath(path)).addAttribute("draft", "true");
    		dataSource.putValueAtPath(parts.toString(), dataSource.getValueAtPath(path));
    	}
		return this;
	}
	
	public UnicodeSet getExemplarSet(String type) {
		if (type.length() != 0) type = "[@type=\"" + type + "\"]";
		String v = getStringValue("//ldml/characters/exemplarCharacters" + type);
		if (v == null) return null;
		UnicodeSet result = new UnicodeSet(v, UnicodeSet.CASE);
		result.remove(0x20);
		return result;
	}

	transient CLDRFile resolvedVersion;
	
	public CLDRFile getResolved() {
		if (dataSource.isResolving()) return this;
		if (resolvedVersion == null) {
			resolvedVersion = new CLDRFile(dataSource, true);
		}
		return resolvedVersion;
	}

	public Set getAvailableLocales() {
		return dataSource.getAvailableLocales();
	}

	public CLDRFile make(String locale, boolean resolved) {
		if (dataSource == null) throw new UnsupportedOperationException("Make not supported");
		return new CLDRFile(dataSource.make(locale), resolved);
	}
    
    // WARNING: this must go AFTER attributeOrdering is set; otherwise it uses a null comparator!!
    private static final DistinguishedXPath distinguishedXPath = new DistinguishedXPath();
    
    //private static Set atomicElements = Collections.unmodifiableSet(new HashSet(Arrays.asList(new String[]{"collation", "segmentation"})));

    public static final String distinguishedXPathStats() {
        return distinguishedXPath.stats();
    }
    
    private static class DistinguishedXPath {
        public static final String stats() {
            return "distinguishingMap:" + distinguishingMap.size() + " " +
                   "normalizedPathMap:" + normalizedPathMap.size();
        }
        private static Map distinguishingMap = new HashMap();
        private static Map normalizedPathMap = new HashMap();
        private static XPathParts distinguishingParts;
        static { 
            distinguishingParts = new XPathParts(attributeOrdering,null);
        }
        
        public static String getDistinguishingXPath(String xpath, String[] normalizedPath, boolean nonInheriting) {
            synchronized (distinguishingMap) {
                String result = (String) distinguishingMap.get(xpath);
                if (result == null) {
                    distinguishingParts.set(xpath);
                    boolean inheriting = distinguishingParts.getElement(0).equals("ldml");
                    
                    // first clean up draft and alt
                    
                    String draft = null;
                    String alt = null;
                    String references = "";
                    // note: we only need to clean up items that are NOT on the last element,
                    // so we go up to size() - 1.

                    for (int i = 0; i < distinguishingParts.size() - 1; ++i) {
                    	// String element = distinguishingParts.getElement(i);
                    	//if (atomicElements.contains(element)) break;
                        Map attributes = distinguishingParts.getAttributes(i);
                        for (Iterator it = attributes.keySet().iterator(); it.hasNext();) {
                        	String attribute = (String) it.next();
                        	if (attribute.equals("draft")) {
                        		draft = (String) attributes.get(attribute);
                        		it.remove();
                        	} else if (attribute.equals("alt")) {
                        		alt = (String) attributes.get(attribute);
                        		it.remove();
                        	} else if (attribute.equals("references")) {
                        		if (references.length() != 0) references += " ";
                        		references += (String) attributes.get("references");
                        		it.remove();
                        	}
                        }
                    }
                    if (draft != null || alt != null || references.length() != 0) {
                        // get the last element that is not ordered.
                        int placementIndex = distinguishingParts.size() - 1;
                        while (true) {
                            String element = distinguishingParts.getElement(placementIndex);
                            if (!orderedElements.contains(element)) break;
                            --placementIndex;
                        }
                        Map attributes = distinguishingParts.getAttributes(placementIndex);
                        if (draft != null) attributes.put("draft", draft);
                        if (alt != null) attributes.put("alt", alt);
                        if (references.length() != 0) attributes.put("references", references);
                        String newXPath = distinguishingParts.toString();
                        if (!newXPath.equals(xpath)) {
                            normalizedPathMap.put(xpath, newXPath); // store differences
                            //System.err.println("fixing " + xpath + " => " + newXPath);
                        }
                    }
                    
                    // now remove non-distinguishing attributes (if non-inheriting)
                    
                    if (inheriting) {
                        for (int i = 0; i < distinguishingParts.size(); ++i) {
                            String element = distinguishingParts.getElement(i);
                            Map attributes = distinguishingParts.getAttributes(i);
                            for (Iterator it = attributes.keySet().iterator(); it.hasNext();) {
                                String attribute = (String) it.next();
                                if (!isDistinguishing(element, attribute)) {
                                    it.remove();
                                }
                            }
                        }
                    }
                    
                    result = distinguishingParts.toString();
                    distinguishingMap.put(xpath, result);
                }
                if (normalizedPath != null) {
                    normalizedPath[0] = (String) normalizedPathMap.get(xpath);
                    if (normalizedPath[0] == null) normalizedPath[0] = xpath;
                }
                return result;
            }
        }
    }



    public static class Status {
		public String pathWhereFound;
	}



	public static boolean isLOG_PROGRESS() {
        return LOG_PROGRESS;
    }

    public static void setLOG_PROGRESS(boolean log_progress) {
        LOG_PROGRESS = log_progress;
    }

	public boolean isEmpty() {
		return !dataSource.iterator().hasNext();
	}
   
}
