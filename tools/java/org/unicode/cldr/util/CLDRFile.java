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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.XMLReaderFactory;

//import javax.xml.parsers.*;

/**
 * This is a simple class that walks through the CLDR hierarchy and does 2 things.
 * First, it determines all the places where the CLDR is not minimal: where there
 * are redundancies with inheritance. It generates new files in the target directory.
 * Second, it gathers together all the items from all the locales that share the
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
public class CLDRFile implements Lockable {
	private static boolean DEBUG_LOGGING = false;	
	private static final String NEWLINE = "\n";
	private static MapComparator LDMLComparator = new MapComparator();
    
    private Map map = new TreeMap(LDMLComparator);
    private String finalComment = "";
    private String key;
    private CLDRFile(){}
	
    /**
     * Create a CLDRFile for the given localename. (Normally a Factory is used to create CLDRFiles.)
     * @param localeName
     */
    public static CLDRFile make(String localeName) {
    	CLDRFile result = new CLDRFile();
		result.key = localeName;
		return result;
    }
    
    /**
     * Produce a CLDRFile from a localeName, given a directory. (Normally a Factory is used to create CLDRFiles.)
     * @param localeName
     * @param dir directory 
     * @throws SAXNotSupportedException
     * @throws SAXNotRecognizedException
     * @throws IOException
     */
    // TODO make the directory a URL
    public static CLDRFile make(String localeName, String dir) {
        File f = new File(dir + localeName + ".xml");
        /* if (DEBUG_LOGGING) {
         	System.out.println("Parsing: " + f.getCanonicalPath());
         	if (log != null) log.println("Parsing: " + f.getCanonicalPath());
	    }
	    */
        try {
			FileInputStream fis = new FileInputStream(f);
	    	CLDRFile result = make(localeName, fis);
			fis.close();
			return result;
		} catch (IOException e) {
			throw (IllegalArgumentException)new IllegalArgumentException("Can't read " + localeName).initCause(e);
		}
    }
    
    /**
     * Produce a CLDRFile from a file input stream. (Normally a Factory is used to create CLDRFiles.)
     * @param localeName
     * @param fis
     * @throws IOException
     * @throws SAXException
     */
    public static CLDRFile make(String localeName, FileInputStream fis) {
    	try {
    		CLDRFile result = make(localeName);
			MyDeclHandler DEFAULT_DECLHANDLER = new MyDeclHandler(result);
			XMLReader xmlReader = createXMLReader(true);
			xmlReader.setContentHandler(DEFAULT_DECLHANDLER);
			xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", DEFAULT_DECLHANDLER);
			xmlReader.setProperty("http://xml.org/sax/properties/declaration-handler", DEFAULT_DECLHANDLER);
			xmlReader.parse(new InputSource(fis));
			return result;
		} catch (SAXException e) {
			throw (IllegalArgumentException)new IllegalArgumentException("Can't read " + localeName).initCause(e);
		} catch (IOException e) {
			throw (IllegalArgumentException)new IllegalArgumentException("Can't read " + localeName).initCause(e);
		}    	 
    }
    
    /**
     * Clone the object. Produces unlocked version (see Lockable).
     */
    public Object clone() {
    	try {
			CLDRFile result = (CLDRFile) super.clone();
			result.locked = false;
			result.map = (Map)((TreeMap)map).clone();
			return result;
		} catch (CloneNotSupportedException e) {
			throw new InternalError("should never happen");
		}
    }
	

	/**
	 * Write the corresponding XML file out, with the normal formatting and indentation.
	 * @param pw
	 * @param key
	 */
	public void write(PrintWriter pw) {
		pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
		pw.println("<!DOCTYPE ldml SYSTEM \"http://www.unicode.org/cldr/dtd/1.2/ldml.dtd\">");

		XPathParts last = new XPathParts();
		XPathParts current = new XPathParts();
		for (Iterator it2 = map.keySet().iterator(); it2.hasNext();) {
			String xpath = (String)it2.next();
			Value v = (Value) map.get(xpath);
			current.set(v.fullXPath);
			current.writeDifference(pw, last, v);
			XPathParts temp = current;
			current = last;
			last = temp;
		}
		current.clear().writeDifference(pw, last, null);
		writeComment(pw, 0, finalComment);
	}

	/**
	 * Get a value from an xpath.
	 */
    public Value getValue(String xpath) {
    	return (Value) map.get(xpath);
    }
    
    /**
     * Add a new element to a CLDRFile.
     * @param xpath
     * @param comment
     * @param currentFullXPath
     * @param value
     */
    public void add(String xpath, String comment, String currentFullXPath, String value) {
    	if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
    	StringValue v = new StringValue(value, comment, currentFullXPath);
    	xpath = xpath.intern();
    	LDMLComparator.add(xpath);
        map.put(xpath, v);
    }
    
    /**
     * Merges elements from another CLDR file. Note: when both have the same xpath key, 
     * the keepMine determines whether "my" values are kept
     * or the other files values are kept.
     * @param other
     * @param keepMine if true, keep my values in case of conflict; otherwise keep the other's values.
     */
    public void addAll(CLDRFile other, boolean keepMine) {
    	if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
    	if (keepMine) {
    		Map temp = new TreeMap(LDMLComparator);
    		temp.putAll(other.map);
    		temp.putAll(map);
    		map = temp;
    	} else {
    		map.putAll(other.map);
    	}
    }
    
    /**
     * Removes an element from a CLDRFile.
     * @param xpath
     */
    public void remove(String xpath) {
    	if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
    	map.remove(xpath);
    }
    
	/**
	 * @return Returns the finalComment.
	 */
	public String getFinalComment() {
		return finalComment;
	}
	/**
	 * @return Returns the key.
	 */
	public String getKey() {
		return key;
	}

	private boolean locked;
	
	/* (non-Javadoc)
	 * @see org.unicode.cldr.util.Lockable#isLocked()
	 */
	public synchronized boolean isLocked() {
		return locked;
	}

	/* (non-Javadoc)
	 * @see org.unicode.cldr.util.Lockable#lock()
	 */
	public synchronized void lock() {
		locked = true;
	}
	/**
	 * @param finalComment The finalComment to set.
	 */
	public void setFinalComment(String finalComment) {
    	if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
		this.finalComment = finalComment;
	}
	
	// ========== STATIC UTILITIES ==========
	
    /**
     * Utility to write a comment.
	 * @param pw
	 * @param v
	 */
	static void writeComment(PrintWriter pw, int indent, String comment) {
		// now write the comment
		if (comment.length() != 0) {
			indent(pw, indent);
			pw.print("<!-- ");
			if (comment.indexOf(NEWLINE) > 0) {
				pw.println(comment);
				indent(pw, indent);
			} else {
				pw.print(comment);
			}
			pw.println("-->");
		}
	}
	
	/**
	 * Utility to indent by a certain number of tabs.
	 * @param out
	 * @param count
	 */
	static void indent(PrintWriter out, int count) {
        for (int i = 0; i < count; ++i) {
            out.print('\t');
        }
    }
	
	/**
	 * Utility to restrict to files matching a given regular expression. The expression does not contain ".xml".
	 * Note that supplementalData is always skipped, and root is always included.
	 * @param sourceDir
	 * @param localeRegex
	 * @return
	 */
    public static Set getMatchingXMLFiles(String sourceDir, String localeRegex) {
        Matcher m = Pattern.compile(localeRegex).matcher("");
        Set s = new TreeSet();
        File[] files = new File(sourceDir).listFiles();
        for (int i = 0; i < files.length; ++i) {
            String name = files[i].getName();
            if (!name.endsWith(".xml")) continue;
            if (name.startsWith("supplementalData")) continue;
            String locale = name.substring(0,name.length()-4); // drop .xml
            if (!locale.equals("root") && !m.reset(locale).matches()) continue;
            s.add(locale);
        }
        return s;
    }

    /**
     * Utility to get the parent of a locale. If the input is "root", then the output is null.
     * @param localeName
     * @return
     */
    public static String getParent(String localeName) {
        int pos = localeName.lastIndexOf('_');
        if (pos >= 0) {
            return localeName.substring(0,pos);
        }
        if (localeName.equals("root")) return null;
        return "root";
    }
    
    /**
     * Utility to determine if this a language locale? 
     * Note: a script is included with the language, if there is one.
     * @param in
     * @return
     */
    public static boolean isLanguage(String in) {
    	int pos = in.indexOf('_');
    	if (pos < 0) return true;
    	if (in.indexOf('_', pos+1) >= 0) return false; // no more than 2 subtags
    	if (in.length() != pos + 5) return false; // second must be 4 in length
    	return true;
    }
    
    /**
     * Returns -1 if parent isn't really a parent, 0 if they are identical, and 1 if parent is a proper parent
     * @param parent
     * @param possibleSublocale
     * @return
     */
    public static int isSubLocale(String parent, String possibleSublocale) {
    	if (parent.length() > possibleSublocale.length()) return -1;
    	if (!possibleSublocale.startsWith(parent)) return -1;
    	if (parent.length() == possibleSublocale.length()) return 0;
    	if (possibleSublocale.charAt(parent.length()) != '_') return -1; // last subtag too long
    	return 1;
    }
    
    public Set keySet() {
    	return Collections.unmodifiableSet(map.keySet());
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
		|| (attribute.equals("type") && !elementName.equals("default") && !elementName.equals("mapping"));
	}
	
	/**
	 * Utility to create a validating XML reader.
	 * @param validating
	 * @return
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
		private PrintWriter log;
		private Set localeList = new TreeSet();
		private Map mainCache = new TreeMap();
		private Map resolvedCache = new TreeMap();  
		private Map supplementalCache = new TreeMap();
		private Factory() {}		
		/**
		 * Create a factory from a source directory, matchingString, and an optional log file.
		 * For the matchString meaning, see getMatchingXMLFiles
		 * @param sourceDirectory
		 * @param matchString
		 * @param optionalLog
		 * @return
		 */
		public static Factory make(String sourceDirectory, String matchString, PrintWriter optionalLog) {
			Factory result = new Factory();
			result.sourceDirectory = sourceDirectory;
			result.log = optionalLog;
			result.matchString = matchString;
			result.localeList = getMatchingXMLFiles(sourceDirectory, matchString);
			return result;
		}

		/**
		 * Get a set of the available locales for the factory.
		 * @return
		 */
	    public Set getAvailable() {
	    	return Collections.unmodifiableSet(localeList);
	    }
	    
	    /**
	     * Get a set of the available language locales (according to isLanguage).
	     * @return
	     */
	    public Set getAvailableLanguages() {
	    	Set result = new TreeSet();
	    	for (Iterator it = localeList.iterator(); it.hasNext();) {
	    		String s = (String) it.next();
	    		if (isLanguage(s)) result.add(s);
	    	}
	    	return result;
	    }
	    
	    /**
	     * Get a set of the locales that have the given parent (according to isSubLocale())
	     * @param parent
	     * @param isProper if false, then parent itself will match
	     * @return
	     */
	    public Set getAvailableWithParent(String parent, boolean isProper) {
	    	Set result = new TreeSet();
	    	for (Iterator it = localeList.iterator(); it.hasNext();) {
	    		String s = (String) it.next();
	    		int relation = isSubLocale(parent, s);
	    		if (relation >= 0 && !(isProper && relation == 0)) result.add(s);
	    	}
	    	return result;
	    }
	    
	    /**
	     * Make a CLDR file. The result is a locked file, so that it can be cached. If you want to modify it,
	     * use clone().
	     * @param localeName
	     * @param resolved if true, produces a resolved version.
	     * @return
	     * @throws SAXException
	     * @throws IOException
	     */
	    // TODO resolve aliases
		public CLDRFile make(String localeName, boolean resolved) {
	    	Map cache = resolved ? resolvedCache : mainCache;
	    	CLDRFile result = (CLDRFile) cache.get(localeName);
	    	if (result == null) {
	    		if (!resolved) {
	    			result = CLDRFile.make(localeName, sourceDirectory);
	    		} else {
	    			// get resolved version
	    			// get parent first if possible
	    			String parentName = getParent(localeName);
	    			if (parentName == null) {
	    				// is root, so just get unresolved file.
	    				result = make(localeName, false);
	    			} else {
	    				CLDRFile parent = make(parentName, true); // will recurse!
	    				result = (CLDRFile) make(localeName, false).clone();
	    				result.addAll(parent, true);
	    			}
	    		}
	    		result.lock();
	    		cache.put(localeName, result);
	    	}
	    	return result;
	    }
	}

    /**
     * Immutable class that defines the value at a particular xpath.
     * Normally a string, unless the item does not inherit (like collation).
     */
    static public abstract class Value {
    	private String comment;
    	private String fullXPath;
		/**
		 * Create a value.
		 * @param value
		 * @param comment2
		 */
		public Value(String comment, String currentFullXPath) {
	        this.comment = comment.intern();
	        this.fullXPath = currentFullXPath.intern();
		}
		/**
		 * @return Returns the comment.
		 */
		public String getComment() {
			return comment;
		}
		/**
		 * @return Returns the fullXPath.
		 */
		public String getFullXPath() {
			return fullXPath;
		}
		/**
		 * boilerplate
		 */
    	public boolean equals(Object other) {
			if (other == null || !getClass().equals(other.getClass())) return false;
    		Value that = (Value)other;
    		return comment.equals(that.comment) && fullXPath.equals(that.fullXPath);
    	}
    	/**
    	 * Must be overridden.
    	 * @return
    	 */
    	public abstract String getStringValue();
		/**
		 * boilerplate
		 */
    	public String toString() {
    		return fullXPath + ";\t" + getStringValue() + ";\t" + comment; 
    	}
    }
    
    /**
     * Value that contains a single string
     */
    static public class StringValue extends Value {
    	private String stringValue;
    	/**
		 * @param value
		 * @param comment
		 * @param currentFullXPath
		 */
		public StringValue(String value, String comment, String currentFullXPath) {
			super(comment, currentFullXPath);
	        this.stringValue = value.intern();
		}
		/**
		 * boilerplate
		 */
		public boolean equals(Object other) {
    		if (!super.equals(other)) return false;
    		return stringValue.equals(((StringValue)other).stringValue);
    	}
		/**
		 * boilerplate
		 */
		public String getStringValue() {
			return stringValue;
		}
    }
    /**
     * Value that contains a node. WARNING: this is not done yet, and may change.
     * In particular, we don't want to return a Node, since that is mutable, and makes caching unsafe!!
     */
    static public class NodeValue extends Value {
    	private Node nodeValue;
    	/**
    	 * Creation. WARNING, may change.
    	 * @param value
    	 * @param comment
    	 * @param currentFullXPath
    	 */
		public NodeValue(Node value, String comment, String currentFullXPath) {
			super(comment, currentFullXPath);
	        this.nodeValue = value;
		}
		/**
		 * boilerplate
		 */
    	public boolean equals(Object other) {
    		if (super.equals(other)) return false;
    		return nodeValue.equals(((NodeValue)other).nodeValue);
    	}
		/**
		 * boilerplate
		 */
		public String getStringValue() {
			return nodeValue.toString();
		}
    }

    private static class MyDeclHandler implements DeclHandler, ContentHandler, LexicalHandler {
    	private static final boolean SHOW_ALL = true;
    	private static final boolean SHOW_START_END = false;
    	private PrintWriter log = null; // set to non-null if you want logging
    	private int commentStack;
    	private boolean justPopped = false;
    	private String lastChars = "";
    	private String currentXPath = "";
    	private String currentFullXPath = "";
    	private String comment = "";
    	private Map attributeOrder = new TreeMap();
    	private CLDRFile target;
    	
    	MyDeclHandler(CLDRFile target) {
    		this.target = target;
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
    		if (SHOW_ALL && log != null) log.println("Attribute\t" + qName + "\t" + show(attributes));
    		currentXPath += "/" + qName;
    		currentFullXPath += "/" + qName;
    		if (attributes.getLength() > 0) {
    			attributeOrder.clear();
	    		for (int i = 0; i < attributes.getLength(); ++i) {    			
	    			String attribute = attributes.getQName(i);
	    			String value = attributes.getValue(i);
	    			attributeOrder.put(attribute, value);
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
            justPopped = false;
    		if (SHOW_ALL && log != null) log.println("currentXPath\t" + currentXPath + "\tcurrentFullXPath\t" + currentFullXPath);
    	}
    	
		private void pop(String qName) {
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

		public void elementDecl(String name, String model) throws SAXException {
        	if (SHOW_ALL && log != null) log.println("Attribute\t" + name + "\t" + model);
        }
        public void attributeDecl(String eName, String aName, String type, String mode, String value) throws SAXException {
            if (SHOW_ALL && log != null) log.println("Attribute\t" + eName + "\t" + aName + "\t" + type + "\t" + mode + "\t" + value);
        }
        public void internalEntityDecl(String name, String value) throws SAXException {
        	if (SHOW_ALL && log != null) log.println("Internal Entity\t" + name + "\t" + value);
        }
        public void externalEntityDecl(String name, String publicId, String systemId) throws SAXException {
        	if (SHOW_ALL && log != null) log.println("Internal Entity\t" + name + "\t" + publicId + "\t" + systemId);
        }

        public void startElement(
            String uri,
            String localName,
            String qName,
            Attributes attributes)
            throws SAXException {
        		if ((SHOW_ALL || SHOW_START_END) && log != null) log.println("startElement uri\t" + uri
        				+ "\tlocalName " + localName
        				+ "\tqName " + qName
        				+ "\tattributes " + show(attributes)
						);
                try {
                	assert lastChars.length() == 0;
                    push(qName, attributes);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    throw e;
                }
        }
        public void endElement(String uri, String localName, String qName)
            throws SAXException {
    			if ((SHOW_ALL || SHOW_START_END) && log != null) log.println("endElement uri\t" + uri + "\tlocalName " + localName
    				+ "\tqName " + qName);
                try {
                    if (lastChars.length() != 0 || justPopped == false) {
                        target.add(currentXPath, comment, currentFullXPath, lastChars);
                        lastChars = "";
                        comment="";
                    }
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
                    if (SHOW_ALL && log != null) log.println("characters:\t" + value);
                    lastChars += value;
                    justPopped = false;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    throw e;
                }
            }

        // just for debugging

        public void notationDecl (String name, String publicId, String systemId)
        throws SAXException {
            if (SHOW_ALL && log != null) log.println("notationDecl: " + name
            + ", " + publicId
            + ", " + systemId
            );
        }

        public void processingInstruction (String target, String data)
        throws SAXException {
            if (SHOW_ALL && log != null) log.println("processingInstruction: " + target + ", " + data);
        }

        public void skippedEntity (String name)
        throws SAXException {
            if (SHOW_ALL && log != null) log.println("skippedEntity: " + name);
        }

        public void unparsedEntityDecl (String name, String publicId,
                        String systemId, String notationName) {
            if (SHOW_ALL && log != null) log.println("unparsedEntityDecl: " + name
            + ", " + publicId
            + ", " + systemId
            + ", " + notationName
            );
        }
        public void setDocumentLocator(Locator locator) {
            if (SHOW_ALL && log != null) log.println("setDocumentLocator Locator " + locator);
        }
        public void startDocument() throws SAXException {
            if (SHOW_ALL && log != null) log.println("startDocument");
            commentStack = 0; // initialize
        }
        public void endDocument() throws SAXException {
        	target.setFinalComment(comment);
            if (SHOW_ALL && log != null) log.println("endDocument");
        }
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            if (SHOW_ALL && log != null) log.println("startPrefixMapping prefix: " + prefix +
                    ", uri: " + uri);
        }
        public void endPrefixMapping(String prefix) throws SAXException {
            if (SHOW_ALL && log != null) log.println("endPrefixMapping prefix: " + prefix);
        }
        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            if (SHOW_ALL && log != null) log.println("ignorableWhitespace length: " + length);
        }
        public void startDTD(String name, String publicId, String systemId) throws SAXException {
            if (SHOW_ALL && log != null) log.println("startDTD name: " + name
                    + ", publicId: " + publicId
                    + ", systemId: " + systemId
            );
            commentStack++;
        }
        public void endDTD() throws SAXException {
            if (SHOW_ALL && log != null) log.println("endDTD");
            commentStack--;
        }
        public void startEntity(String name) throws SAXException {
            if (SHOW_ALL && log != null) log.println("startEntity name: " + name);
        }
        public void endEntity(String name) throws SAXException {
            if (SHOW_ALL && log != null) log.println("endEntity name: " + name);
        }
        public void startCDATA() throws SAXException {
            if (SHOW_ALL && log != null) log.println("startCDATA");
        }
        public void endCDATA() throws SAXException {
            if (SHOW_ALL && log != null) log.println("endCDATA");
        }
        public void comment(char[] ch, int start, int length) throws SAXException {
            if ((SHOW_ALL || SHOW_START_END) && log != null) log.println(commentStack + " comment " + new String(ch, start,length));
            if (commentStack != 0) return;
            String comment0 = new String(ch, start,length).trim();
            if (comment.length() == 0) comment = comment0;
            else comment += NEWLINE + comment0;
        }
    }
}
