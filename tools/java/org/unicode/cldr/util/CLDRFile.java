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
import java.io.PrintWriter;
import java.util.ArrayList;
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
public class CLDRFile {
	static boolean DEBUG_LOGGING = false;
	
	public static class Factory {
		String sourceDirectory;
		String matchString;
		PrintWriter log;
		
		public static Factory make(String sourceDirectory, String matchString, PrintWriter optionalLog) {
			Factory result = new Factory();
			result.sourceDirectory = sourceDirectory;
			result.log = optionalLog;
			result.matchString = matchString;
			result.localeList = getMatchingXMLFiles(sourceDirectory, matchString);
			return result;
		}
		
		private Factory() {}
		
		Set localeList = new TreeSet();
		Map mainCache = new TreeMap();
		Map resolvedCache = new TreeMap();  
		Map supplementalCache = new TreeMap();

	    public Set getAvailable() {
	    	return localeList;
	    }
	    
	    public Set getAvailableLanguages() {
	    	Set result = new TreeSet();
	    	for (Iterator it = localeList.iterator(); it.hasNext();) {
	    		String s = (String) it.next();
	    		if (isLanguage(s)) result.add(s);
	    	}
	    	return result;
	    }
	    
	    public Set getAvailableWithParent(String parent, boolean isProper) {
	    	Set result = new TreeSet();
	    	for (Iterator it = localeList.iterator(); it.hasNext();) {
	    		String s = (String) it.next();
	    		int relation = isSubLocale(parent, s);
	    		if (relation >= 0 && !(isProper && relation == 0)) result.add(s);
	    	}
	    	return result;
	    }
	    
		public CLDRFile make(String localeName, boolean resolved) {
	    	Map cache = resolved ? resolvedCache : mainCache;
	    	CLDRFile result = (CLDRFile) cache.get(localeName);
	    	if (result == null) {
	    		if (!resolved) {
	    			result = new CLDRFile(sourceDirectory, localeName, this);
	    		} else {
	    			// get resolved version
	    			// get parent first if possible
	        		result = new CLDRFile();
	    			String parentName = getParent(localeName);
	    			if (parentName != null) {
	    				CLDRFile parent = make(parentName, true); // will recurse!
	    				result.map.putAll(parent.map);
	    			}
	        		CLDRFile other = make(localeName, false);
	        		result.map.putAll(other.map);
	    		}
	    		cache.put(localeName, result);
	    	}
	    	return result;
	    }
	}
	
	private static final String NEWLINE = "\n";
	static MapComparator LDMLComparator = new MapComparator();
	static MapComparator AttributeComparator = new MapComparator().add("alt").add("draft").add("type");
    
    Map map = new TreeMap(LDMLComparator);
    Factory factory;
    String finalComment = "";

    /**
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
	
	static void indent(PrintWriter out, int count) {
        for (int i = 0; i < count; ++i) {
            out.print('\t');
        }
    }
	
	private static void writeTrailing(String lastXPath, String fullXPath) {
		// TODO Auto-generated method stub
		
	}
	
	private static void writeLeading(String lastXPath, String fullXPath) {
		
	}

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

    public static String getParent(String localeName) {
        int pos = localeName.lastIndexOf('_');
        if (pos >= 0) {
            return localeName.substring(0,pos);
        }
        if (localeName.equals("root")) return null;
        return "root";
    }
    
    public static class XPathParts {
    	// a piece is an element: '/'....('[' | '/')
    	// or an attribute/value: '[@'...'="'...'"]'
    	/*static class Attribute {
    		String attribute;
    		String value;
    		public Attribute(String attribute, String value) {
    			this.attribute = attribute;
    			this.value = value;
    		}
    		public String toString() {
    			return "[@" + attribute + "=\"" + value + "\"]"; // TODO quote any " in value
    		}
    		public boolean equals(Object other) {
    			if (other == null || !getClass().equals(other.getClass())) return false;
    			Attribute that = (Attribute)other;
    			return attribute.equals(that.attribute) && value.equals(that.value);
    		}
    		public int hashCode() {
    			return attribute.hashCode()*37 + value.hashCode();
    		}
    	}
    	*/
    	private static class Element {
    		private String element;
    		private Map attributes = new TreeMap(AttributeComparator);
    		public Element(String element) {
    			this.element = element;
    		}
    		static final int XPATH_STYLE = 0, XML_OPEN = 1, XML_CLOSE = 2, XML_NO_VALUE = 3;
    		public String toString() {
    			throw new IllegalArgumentException("Don't use");
    		}
    		public String toString(int style) {
    			StringBuffer result = new StringBuffer();
				switch (style) {
				case XPATH_STYLE:
					result.append('/').append(element);
					for (Iterator it = attributes.keySet().iterator(); it.hasNext();) {
						String attribute = (String) it.next();
						String value = (String) attributes.get(attribute);
						if (attribute.equals("type") && value.equals("standard")) continue; // HACK
						if (attribute.equals("version") && value.equals("1.2")) continue; // HACK
						result.append("[@").append(attribute).append("=\"")
								.append(value).append("\"]");
					}
					break;
				case XML_OPEN:
				case XML_NO_VALUE:
					result.append('<').append(element);
					for (Iterator it = attributes.keySet().iterator(); it.hasNext();) {
						String attribute = (String) it.next();
						String value = (String) attributes.get(attribute);
						if (attribute.equals("type") && value.equals("standard")) continue; // HACK
						if (attribute.equals("version") && value.equals("1.2")) continue; // HACK
						result.append(' ').append(attribute).append("=\"")
								.append(value).append('\"');
					}
					if (style == XML_NO_VALUE) result.append('/');
					result.append('>');
					break;
				case XML_CLOSE:
					result.append("</").append(element).append('>');
					break;
				}
    			return result.toString();
    		}
    		public boolean equals(Object other) {
    			if (other == null || !getClass().equals(other.getClass())) return false;
    			Element that = (Element)other;
    			return element.equals(that.element) && attributes.equals(that.attributes);
    		}
    		public int hashCode() {
    			return element.hashCode()*37 + attributes.hashCode();
    		}
    	}

    	List elements = new ArrayList();
    	
    	public boolean containsElement(String element) {
    		for (int i = 0; i < elements.size(); ++i) {
    			if (((Element)elements.get(i)).element.equals(element)) return true;
    		}
    		return false;
    	}
    	/**
		 * @return
		 */
		public XPathParts clear() {
			elements.clear();
			return this;
		}
		/**
		 * @param pw
		 * @param last
		 */
		public void writeDifference(PrintWriter pw, XPathParts last, Value v) {
			int limit = findFirstDifference(last);
			// write the end of the last one
			for (int i = last.size()-2; i >= limit; --i) {
				indent(pw, i);
				pw.println(((Element)last.elements.get(i)).toString(Element.XML_CLOSE));
			}
			if (v == null) return; // end
			// now write the start of the current
			for (int i = limit; i < size()-1; ++i) {
				indent(pw, i);
				pw.println(((Element)elements.get(i)).toString(Element.XML_OPEN));
			}
			writeComment(pw, size()-1, v.comment);
			// now write element itself
			indent(pw, size()-1);
			Element e = (Element)elements.get(size()-1);
			String eValue = ((StringValue)v).stringValue;
			if (eValue.length() == 0) {
				pw.println(e.toString(Element.XML_NO_VALUE));
			} else {
				pw.print(e.toString(Element.XML_OPEN));
				pw.print(eValue);
				pw.println(e.toString(Element.XML_CLOSE));
			}
			//if (v.)
			pw.flush();
		}
		
		public int findFirstDifference(XPathParts last) {
			int min = elements.size();
			if (last.elements.size() < min) min = last.elements.size();
			for (int i = 0; i < min; ++i) {
				Element e1 = (Element) elements.get(i);
				Element e2 = (Element) last.elements.get(i);
				if (!e1.equals(e2)) return i;
			}
			return min;
		}
		public boolean containsAttribute(String attribute) {
    		for (int i = 0; i < elements.size(); ++i) {
    			Element element = (Element) elements.get(i);
    			if (element.attributes.keySet().contains(attribute)) return true;
    		}
    		return false;
    	}
    	public boolean containsAttributeValue(String attribute, String value) {
    		for (int i = 0; i < elements.size(); ++i) {
    			Map attributes = ((Element)elements.get(i)).attributes;
    			for (Iterator it = attributes.keySet().iterator(); it.hasNext();) {
    				String a = (String) it.next();
    				if (a.equals(attribute)) {
    					String v = (String)attributes.get(a);
    					if (v.equals(value)) return true;
    				}
    			}
    		}
    		return false;
    	}
    	
    	public void addElement(String element) {
    		elements.add(new Element(element));
    	}
    	public void addAttribute(String attribute, String value) {
    		Element e = (Element)elements.get(elements.size()-1);
    		attribute = attribute.intern();
    		AttributeComparator.add(attribute);
    		e.attributes.put(attribute, value);
    	}

    	public boolean set(String xPath) {
        	elements.clear();
        	String lastAttributeName = "";
    		if (xPath.length() == 0 || xPath.charAt(0) != '/') return parseError(xPath, 0);
    		int stringStart = 1;
    		char state = 'p';
    		// since only ascii chars are relevant, use char
    		for (int i = 1; i < xPath.length(); ++i) {
    			char cp = xPath.charAt(i);
				if (cp != state && (state == '\"' || state == '\'')) continue; // stay in quotation
    			switch(cp) {
    			case '/':
    				if (state != 'p' || stringStart >= i) return parseError(xPath,i);
	    			if (stringStart > 0) addElement(xPath.substring(stringStart, i));
    				stringStart = i+1;
    				break;
    			case '[':
    				if (state != 'p' || stringStart >= i) return parseError(xPath,i);
    				if (stringStart > 0) addElement(xPath.substring(stringStart, i));
    				state = cp;
    				break;
    			case '@': 
    				if (state != '[') return parseError(xPath,i);
    				stringStart = i+1;
    				state = cp;
    				break;
    			case '=': 
    				if (state != '@' || stringStart >= i) return parseError(xPath,i);
    				lastAttributeName = xPath.substring(stringStart, i);
    				state = cp;
    				break;
    			case '\"':
    			case '\'':
    				if (state == cp) { // finished
    					if (stringStart >= i) return parseError(xPath,i);
    					addAttribute(lastAttributeName, xPath.substring(stringStart, i));
    					state = 'e';
    					break;
    				}
    				if (state != '=') return parseError(xPath,i);
    				stringStart = i+1;
    				state = cp;
    				break;
    			case ']': 
    				if (state != 'e') return parseError(xPath,i);
    				state = 'p';
    				stringStart = -1;
    				break;
    			}
    		}
    		// check to make sure terminated
			if (state != 'p' || stringStart >= xPath.length()) return parseError(xPath,xPath.length());
			if (stringStart > 0) addElement(xPath.substring(stringStart, xPath.length()));
    		return true;
    	}
    	boolean parseError(String s, int i) {
    		throw new IllegalArgumentException("Malformed xPath " + s + " at " + i);
    	}
    	int size() {
    		return elements.size();
    	}
    	String getElement(int elementIndex) {
    		return ((Element)elements.get(elementIndex)).element;
    	}
    	Map getAttributes(int elementIndex) {
    		return ((Element)elements.get(elementIndex)).attributes;
    	}
    	public String toString() {
    		String result = "";
    		for (int i = 0; i < elements.size(); ++i) {
    			result += elements.get(i);
    		}
    		return result + "/";
    	}
		public boolean equals(Object other) {
			if (other == null || !getClass().equals(other.getClass())) return false;
			XPathParts that = (XPathParts)other;
			if (elements.size() != that.elements.size()) return false;
			for (int i = 0; i < elements.size(); ++i) {
				if (!elements.get(i).equals(that.elements.get(i))) return false;
			}
			return true;
		}
		public int hashCode() {
			int result = elements.size();
			for (int i = 0; i < elements.size(); ++i) {
				result = result*37 + elements.get(i).hashCode();
			}
			return result;
		}
    }
    
    static public abstract class Value {
		/**
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
    	private String comment;
    	private String fullXPath;
    	public boolean equals(Object other) {
			if (other == null || !getClass().equals(other.getClass())) return false;
    		Value that = (Value)other;
    		return comment.equals(that.comment) && fullXPath.equals(that.fullXPath);
    	}
    	public abstract String getStringValue();
    	public String toString() {
    		return fullXPath + ";\t" + getStringValue() + ";\t" + comment; 
    	}
    	/*
    	void write(PrintWriter pw, Value last) {
    		// write the closing values, all but the last element in the xpath
    		String[] pair = new String[2];
    		getDifference(fullXPath, last.fullXPath, pair);
    		showTrailing(pair[0]);
    		showLeading(pair[1]);
    		writeValue(pw);
    	}
    	void getDifference(String path1, String path2, String[] result) {
    		
    	}
    	*/
    }
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
		public boolean equals(Object other) {
    		if (!super.equals(other)) return false;
    		return stringValue.equals(((StringValue)other).stringValue);
    	}
		public String getStringValue() {
			return stringValue;
		}
    }
    static public class NodeValue extends Value {
		public NodeValue(Node value, String comment, String currentFullXPath) {
			super(comment, currentFullXPath);
	        this.nodeValue = value;
		}
    	private Node nodeValue;
    	public boolean equals(Object other) {
    		if (super.equals(other)) return false;
    		return nodeValue.equals(((NodeValue)other).nodeValue);
    	}
		public String getStringValue() {
			return nodeValue.toString();
		}
    }
    public Value getValue(String xpath) {
    	return (Value) map.get(xpath);
    }
    
    private void add(String xpath, String comment, String currentFullXPath, String value) {
    	StringValue v = new StringValue(value, comment, currentFullXPath);
    	xpath = xpath.intern();
    	LDMLComparator.add(xpath);
        map.put(xpath, v);
    }
    
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
    	return map.keySet();
    }
    
    private CLDRFile() {}

    private CLDRFile(String dir, String localeName, Factory factory) {
    	try {
    		this.factory = factory;
    		MyDeclHandler DEFAULT_DECLHANDLER = new MyDeclHandler();
			XMLReader xmlReader = createXMLReader(true);
			xmlReader.setContentHandler(DEFAULT_DECLHANDLER);
			xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", DEFAULT_DECLHANDLER);
			xmlReader.setProperty("http://xml.org/sax/properties/declaration-handler", DEFAULT_DECLHANDLER);
	        File f = new File(dir + localeName + ".xml");
	        if (DEBUG_LOGGING) {
	        	System.out.println("Parsing: " + f.getCanonicalPath());
		        if (factory.log != null) factory.log.println("Parsing: " + f.getCanonicalPath());
	        }
	        FileInputStream fis = new FileInputStream(f);
	        xmlReader.parse(new InputSource(fis));
	        fis.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
	/**
	 * @param name
	 * @param attribute
	 * @return
	 */
	private static boolean isDistinguishing(String name, String attribute) {
		return attribute.equals("key") 
		|| attribute.equals("registry") 
		|| attribute.equals("alt")
		|| attribute.equals("iso4217")
		|| attribute.equals("iso3166")
		|| (attribute.equals("type") && !name.equals("default") && !name.equals("mapping"));
	}

	public static String stripAfter(String input, String qName) {
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
	
    class MyDeclHandler implements DeclHandler, ContentHandler, LexicalHandler {
    	static final boolean SHOW_ALL = true;
    	static final boolean SHOW_START_END = false;
    	int commentStack;
    	boolean justPopped = false;
    	String lastChars = "";
    	String currentXPath = "";
    	String currentFullXPath = "";
    	String comment = "";
    	
    	Map attributeOrder = new TreeMap();
    	
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
    		if (SHOW_ALL && factory.log != null) factory.log.println("Attribute\t" + qName + "\t" + show(attributes));
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
    		if (SHOW_ALL && factory.log != null) factory.log.println("currentXPath\t" + currentXPath + "\tcurrentFullXPath\t" + currentFullXPath);
    	}
    	
		private void pop(String qName) {
            currentXPath = stripAfter(currentXPath, qName);
    		currentFullXPath = stripAfter(currentFullXPath, qName);    
            justPopped = true;
    	}
    	
        public void elementDecl(String name, String model) throws SAXException {
        	if (SHOW_ALL && factory.log != null) factory.log.println("Attribute\t" + name + "\t" + model);
        }
        public void attributeDecl(String eName, String aName, String type, String mode, String value) throws SAXException {
            if (SHOW_ALL && factory.log != null) factory.log.println("Attribute\t" + eName + "\t" + aName + "\t" + type + "\t" + mode + "\t" + value);
        }
        public void internalEntityDecl(String name, String value) throws SAXException {
        	if (SHOW_ALL && factory.log != null) factory.log.println("Internal Entity\t" + name + "\t" + value);
        }
        public void externalEntityDecl(String name, String publicId, String systemId) throws SAXException {
        	if (SHOW_ALL && factory.log != null) factory.log.println("Internal Entity\t" + name + "\t" + publicId + "\t" + systemId);
        }

        public void startElement(
            String uri,
            String localName,
            String qName,
            Attributes attributes)
            throws SAXException {
        		if ((SHOW_ALL || SHOW_START_END) && factory.log != null) factory.log.println("startElement uri\t" + uri
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
    			if ((SHOW_ALL || SHOW_START_END) && factory.log != null) factory.log.println("endElement uri\t" + uri + "\tlocalName " + localName
    				+ "\tqName " + qName);
                try {
                    if (lastChars.length() != 0 || justPopped == false) {
                        add(currentXPath, comment, currentFullXPath, lastChars);
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
                    if (SHOW_ALL && factory.log != null) factory.log.println("characters:\t" + value);
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
            if (SHOW_ALL && factory.log != null) factory.log.println("notationDecl: " + name
            + ", " + publicId
            + ", " + systemId
            );
        }

        public void processingInstruction (String target, String data)
        throws SAXException {
            if (SHOW_ALL && factory.log != null) factory.log.println("processingInstruction: " + target + ", " + data);
        }

        public void skippedEntity (String name)
        throws SAXException {
            if (SHOW_ALL && factory.log != null) factory.log.println("skippedEntity: " + name);
        }

        public void unparsedEntityDecl (String name, String publicId,
                        String systemId, String notationName) {
            if (SHOW_ALL && factory.log != null) factory.log.println("unparsedEntityDecl: " + name
            + ", " + publicId
            + ", " + systemId
            + ", " + notationName
            );
        }
        public void setDocumentLocator(Locator locator) {
            if (SHOW_ALL && factory.log != null) factory.log.println("setDocumentLocator Locator " + locator);
        }
        public void startDocument() throws SAXException {
            if (SHOW_ALL && factory.log != null) factory.log.println("startDocument");
            commentStack = 0; // initialize
        }
        public void endDocument() throws SAXException {
        	finalComment = comment;
            if (SHOW_ALL && factory.log != null) factory.log.println("endDocument");
        }
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            if (SHOW_ALL && factory.log != null) factory.log.println("startPrefixMapping prefix: " + prefix +
                    ", uri: " + uri);
        }
        public void endPrefixMapping(String prefix) throws SAXException {
            if (SHOW_ALL && factory.log != null) factory.log.println("endPrefixMapping prefix: " + prefix);
        }
        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            if (SHOW_ALL && factory.log != null) factory.log.println("ignorableWhitespace length: " + length);
        }
        public void startDTD(String name, String publicId, String systemId) throws SAXException {
            if (SHOW_ALL && factory.log != null) factory.log.println("startDTD name: " + name
                    + ", publicId: " + publicId
                    + ", systemId: " + systemId
            );
            commentStack++;
        }
        public void endDTD() throws SAXException {
            if (SHOW_ALL && factory.log != null) factory.log.println("endDTD");
            commentStack--;
        }
        public void startEntity(String name) throws SAXException {
            if (SHOW_ALL && factory.log != null) factory.log.println("startEntity name: " + name);
        }
        public void endEntity(String name) throws SAXException {
            if (SHOW_ALL && factory.log != null) factory.log.println("endEntity name: " + name);
        }
        public void startCDATA() throws SAXException {
            if (SHOW_ALL && factory.log != null) factory.log.println("startCDATA");
        }
        public void endCDATA() throws SAXException {
            if (SHOW_ALL && factory.log != null) factory.log.println("endCDATA");
        }
        public void comment(char[] ch, int start, int length) throws SAXException {
            if ((SHOW_ALL || SHOW_START_END) && factory.log != null) factory.log.println(commentStack + " comment " + new String(ch, start,length));
            if (commentStack != 0) return;
            String comment0 = new String(ch, start,length).trim();
            if (comment.length() == 0) comment = comment0;
            else comment += NEWLINE + comment0;
        }
    };

    public XMLReader createXMLReader(boolean validating) {
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
    
	/*
	XPathParser p = new XPathParser();
	p.set("/ldml/id[@foo='bar']/a[@b=\"c'd\"][@e='f']/");
	System.out.println(p);
	for (int i = 0; i < p.size(); ++i) {
		System.out.println("element: " + p.getElement(i));
		for (int j = 0; j < p.getAttributeCount(i); ++j) {
			System.out.println("\tattribute: " + p.getAttribute(i, j));
			System.out.println("\tvalue: " + p.getValue(i,j));
		}
	}
	System.out.println(p.containsElement("id"));
	System.out.println(p.containsElement("id2"));
	System.out.println(p.containsAttribute("foo"));
	System.out.println(p.containsAttribute("foo2"));
	System.out.println(p.containsAttributeValue("foo", "bar"));
	System.out.println(p.containsAttributeValue("foo", "bar2"));
	if (true) return;
	*/

}
