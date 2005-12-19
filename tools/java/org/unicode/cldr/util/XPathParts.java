/*
 ******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
*/
package org.unicode.cldr.util;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.icu.dev.test.util.TransliteratorUtilities;

/**
 * Parser for XPath
 */
public class XPathParts {
	private boolean DEBUGGING = false;
	private List elements = new ArrayList();
	Comparator attributeComparator;
	Map suppressionMap;
	
	public XPathParts(Comparator attributeComparator, Map suppressionMap) {
		if (attributeComparator == null) attributeComparator = CLDRFile.getAttributeComparator();
		this.attributeComparator = attributeComparator;
		this.suppressionMap = suppressionMap;
	}
	
	//private static MapComparator AttributeComparator = new MapComparator().add("alt").add("draft").add("type");

	/**
	 * See if the xpath contains an element
	 */
	public boolean containsElement(String element) {
		for (int i = 0; i < elements.size(); ++i) {
			if (((Element)elements.get(i)).element.equals(element)) return true;
		}
		return false;
	}
	/**
	 * Empty the xpath (pretty much the same as set(""))
	 */
	public XPathParts clear() {
		elements.clear();
		return this;
	}
	
	/**
	 * Write out the difference form this xpath and the last, putting the value in the right place. Closes up the elements
	 * that were not closed, and opens up the new.
	 * @param pw
	 * @param filteredXPath TODO
	 * @param lastFullXPath
	 * @param filteredLastXPath TODO
	 */
	public XPathParts writeDifference(PrintWriter pw, XPathParts filteredXPath, XPathParts lastFullXPath,
			XPathParts filteredLastXPath, String v, Comments xpath_comments) {
		int limit = findFirstDifference(lastFullXPath);
		// write the end of the last one
		for (int i = lastFullXPath.size()-2; i >= limit; --i) {
			Utility.indent(pw, i);
			pw.println(((Element)lastFullXPath.elements.get(i)).toString(XML_CLOSE));
		}
		if (v == null) return this; // end
		// now write the start of the current
		for (int i = limit; i < size()-1; ++i) {
			filteredXPath.writeComment(pw, xpath_comments, i+1, Comments.PREBLOCK);
			Utility.indent(pw, i);
			pw.println(((Element)elements.get(i)).toString(XML_OPEN));
		}
		filteredXPath.writeComment(pw, xpath_comments, size(), Comments.PREBLOCK);

		// now write element itself
		Utility.indent(pw, size()-1);
		Element e = (Element)elements.get(size()-1);
		String eValue = v;
		if (eValue.length() == 0) {
			pw.print(e.toString(XML_NO_VALUE));
		} else {
			pw.print(e.toString(XML_OPEN));
			pw.print(TransliteratorUtilities.toHTML.transliterate(eValue));
			pw.print(e.toString(XML_CLOSE));
		}
		filteredXPath.writeComment(pw, xpath_comments, size(), Comments.LINE);
		pw.println();
		filteredXPath.writeComment(pw, xpath_comments, size(), Comments.POSTBLOCK);
		pw.flush();
		return this;
	}
	
	//public static final char BLOCK_PREFIX = 'B', LINE_PREFIX = 'L';
	
	public static class Comments implements Cloneable {
		public static final int LINE = 0, PREBLOCK = 1, POSTBLOCK = 2;
		private HashMap[] comments = new HashMap[3];
		public Comments () {
			comments[LINE] = new HashMap();
			comments[PREBLOCK] = new HashMap();
			comments[POSTBLOCK] = new HashMap();
		}
		public Comments addComment(int style, String xpath, String comment) {
			String existing = (String) comments[style].get(xpath);
	        if (existing != null) {
	        	comment = existing + XPathParts.NEWLINE + comment;
	        }
			comments[style].put(xpath, comment);
			return this;
		}
		public String removeComment(int style, String xPath) {
			String result = (String) comments[style].get(xPath);
			if (result != null) comments[style].remove(xPath);
			return result;
		}
		public List extractCommentsWithoutBase() {
			List result = new ArrayList();
			for (int i = 0; i < comments.length; ++i) {
				for (Iterator it = comments[i].keySet().iterator(); it.hasNext();) {
					Object key = (String) it.next();
					Object value = comments[i].get(key);
					result.add(value + "\t - was on: " + key);
					it.remove();
				}
			}
			return result;
		}
		
		public Object clone() {
	    	try {
	    		Comments result = (Comments) super.clone();
	    		result.comments = new HashMap[3];
				result.comments[LINE] = (HashMap) comments[LINE].clone();
				result.comments[PREBLOCK] = (HashMap) comments[PREBLOCK].clone();
				result.comments[POSTBLOCK] = (HashMap) comments[POSTBLOCK].clone();
				return result;
			} catch (CloneNotSupportedException e) {
				throw new InternalError("should never happen");
			}			
		}
		/**
		 * @param other
		 */
		public Comments joinAll(Comments other) {
			Utility.joinWithSeparation(comments[LINE], XPathParts.NEWLINE, other.comments[LINE]);
			Utility.joinWithSeparation(comments[PREBLOCK], XPathParts.NEWLINE, other.comments[PREBLOCK]);
			Utility.joinWithSeparation(comments[POSTBLOCK], XPathParts.NEWLINE, other.comments[POSTBLOCK]);
			return this;
		}
		/**
		 * @param string
		 */
		public Comments removeComment(String string) {
			if (initialComment.equals(string)) initialComment = "";
			if (finalComment.equals(string)) finalComment = "";
			for (int i = 0; i < comments.length; ++i) {
				for (Iterator it = comments[i].keySet().iterator(); it.hasNext();) {
					Object key = (String) it.next();
					Object value = comments[i].get(key);
					if (!value.equals(string)) continue;
					it.remove();
				}
			}
			return this;
		}
		private String initialComment = "";
		private String finalComment = "";
		/**
		 * @return Returns the finalComment.
		 */
		public String getFinalComment() {
			return finalComment;
		}
		/**
		 * @param finalComment The finalComment to set.
		 */
		public Comments setFinalComment(String finalComment) {
			this.finalComment = finalComment;
			return this;
		}
		/**
		 * @return Returns the initialComment.
		 */
		public String getInitialComment() {
			return initialComment;
		}
		/**
		 * @param initialComment The initialComment to set.
		 */
		public Comments setInitialComment(String initialComment) {
			this.initialComment = initialComment;
			return this;
		}
	}
	
	/**
	 * @param pw
	 * @param xpath_comments
	 * @param index TODO
	 */
	private XPathParts writeComment(PrintWriter pw, Comments xpath_comments, int index, int style) {
		if (index == 0) return this;
		String xpath = toString(index);
		Log.logln(DEBUGGING, "Checking for: " + xpath);
		String comment = (String) xpath_comments.removeComment(style, xpath);
		if (comment != null) {
			XPathParts.writeComment(pw, index-1, comment, style != Comments.LINE);
		}
		return this;
	}

	/**
	 * Finds the first place where the xpaths differ.
	 */
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
    /**
     * Checks if the new xpath given is like the this one. 
     * The only diffrence may be extra alt and draft attributes but the
     * value of type attribute is the same
     * @param last
     * @return
     */
    public boolean isLike(XPathParts last) {
        int min = elements.size();
        if (last.elements.size() < min) min = last.elements.size();
        for (int i = 0; i < min; ++i) {
            Element e1 = (Element) elements.get(i);
            Element e2 = (Element) last.elements.get(i);
            if (!e1.equals(e2)){
                /* is the current element the last one*/
                if(i == min-1 ){
                    String et1 = (String)e1.attributes.get("type");
                    String et2 = (String)e2.attributes.get("type");
                    if(et1.equals(et2)){
                        return true;
                    }
                }else{
                    return false;
                }
            }
        }
        return false;
    }
	/**
	 * Does this xpath contain the attribute at all?
	 */
	public boolean containsAttribute(String attribute) {
		for (int i = 0; i < elements.size(); ++i) {
			Element element = (Element) elements.get(i);
			if (element.attributes.keySet().contains(attribute)) return true;
		}
		return false;
	}
	/**
	 * Does it contain the attribute/value pair?
	 */
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
	
	/**
	 * How many elements are in this xpath?
	 */
	public int size() {
		return elements.size();
	}
	
	/**
	 * Get the nth element. Negative values are from end
	 */
	public String getElement(int elementIndex) {
		if (elementIndex < 0) elementIndex += size();
		return ((Element)elements.get(elementIndex)).element;
	}
	
	/**
	 * Get the attributes for the nth element (negative index is from end). Returns null or an empty map if there's nothing.
	 */
	public Map getAttributes(int elementIndex) {
		if (elementIndex < 0) elementIndex += size();
		return ((Element)elements.get(elementIndex)).attributes;
	}
	
	/**
	 * Get the attributes for the nth element. Returns null or an empty map if there's nothing.
	 */
	public Map findAttributes(String elementName) {
		int index = findElement(elementName);
		if (index == -1) return null;
		return getAttributes(index);
	}
	
	/**
	 * Find the attribute value
	 */
	public String findAttributeValue(String elementName, String attributeName) {
		Map attributes = findAttributes(elementName);
		if (attributes == null) return null;
		return (String)attributes.get(attributeName);
	}


	/**
	 * Add an element
	 */
	public XPathParts addElement(String element) {
		elements.add(new Element(element));
		return this;
	}
	/**
	 * Add an attribute/value pair to the current last element.
	 */
	public XPathParts addAttribute(String attribute, String value) {
		Element e = (Element)elements.get(elements.size()-1);
		attribute = attribute.intern();
		//AttributeComparator.add(attribute);
		e.attributes.put(attribute, value);
		return this;
	}

	/**
	 * Parse out an xpath, and pull in the elements and attributes.
	 * @param xPath
	 * @return
	 */
	public XPathParts set(String xPath) {
    	return addInternal(xPath, true);
	}
	
	/**
	 * Set an xpath, but ONLY if 'this' is clear (size = 0)
	 * @param xPath
	 * @return
	 */
	public XPathParts initialize(String xPath) {
		if (size() != 0) return this;
    	return addInternal(xPath, true);
	}
	
	private XPathParts addInternal(String xPath, boolean initial) {
    	String lastAttributeName = "";
    	//if (xPath.length() == 0) return this;
    	String requiredPrefix = "/";
    	if (initial) {
        	elements.clear();
        	requiredPrefix = "//";
    	}
    	if (!xPath.startsWith(requiredPrefix)) return parseError(xPath, 0);
    	int stringStart = requiredPrefix.length(); // skip prefix
		char state = 'p';
		// since only ascii chars are relevant, use char
		for (int i = 2; i < xPath.length(); ++i) {
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
					if (stringStart > i) return parseError(xPath,i);
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
		return this;
	}

	/**
	 * boilerplate
	 */
	public String toString() {
		return toString(elements.size());
	}
	
	public String toString(int limit) {
		String result = "/";
		for (int i = 0; i < limit; ++i) {
			result += ((Element)elements.get(i)).toString(XPATH_STYLE);
		}
		return result;
	}
	public String toString(int start, int limit) {
		String result = "";
		for (int i = start; i < limit; ++i) {
			result += ((Element)elements.get(i)).toString(XPATH_STYLE);
		}
		return result;
	}
	/**
	 * boilerplate
	 */
	public boolean equals(Object other) {
		if (other == null || !getClass().equals(other.getClass())) return false;
		XPathParts that = (XPathParts)other;
		if (elements.size() != that.elements.size()) return false;
		for (int i = 0; i < elements.size(); ++i) {
			if (!elements.get(i).equals(that.elements.get(i))) return false;
		}
		return true;
	}
	/**
	 * boilerplate
	 */
	public int hashCode() {
		int result = elements.size();
		for (int i = 0; i < elements.size(); ++i) {
			result = result*37 + elements.get(i).hashCode();
		}
		return result;
	}
	
	// ========== Privates ==========
	
	private XPathParts parseError(String s, int i) {
		throw new IllegalArgumentException("Malformed xPath '" + s + "' at " + i);
	}

	public static final int XPATH_STYLE = 0, XML_OPEN = 1, XML_CLOSE = 2, XML_NO_VALUE = 3;
	public static final String NEWLINE = "\n";
	
	private class Element {
		private String element;
		private Map attributes = new TreeMap(attributeComparator); // = new TreeMap(AttributeComparator);

		public Element(String element) {
			this.element = element;
		}
		
		public String toString() {
			throw new IllegalArgumentException("Don't use");
		}
		/**
		 * @param style from XPATH_STYLE
		 * @return
		 */
		public String toString(int style) {
			StringBuffer result = new StringBuffer();
			//Set keys;
			switch (style) {
			case XPathParts.XPATH_STYLE:
				result.append('/').append(element);
				writeAttributes(element, "[@", "\"]", false, result);
				break;
			case XPathParts.XML_OPEN:
			case XPathParts.XML_NO_VALUE:
				result.append('<').append(element);
				if (false && element.equals("orientation")) {
					System.out.println();
				}
				writeAttributes(element, " ", "\"", true, result);
				/*
				keys = attributes.keySet();
				if (attributeComparator != null) {
					Set temp = new TreeSet(attributeComparator);
					temp.addAll(keys);
					keys = temp;
				}
				for (Iterator it = keys.iterator(); it.hasNext();) {
					String attribute = (String) it.next();
					String value = (String) attributes.get(attribute);
					if (attribute.equals("type") && value.equals("standard")) continue; // HACK
					if (attribute.equals("version") && value.equals("1.2")) continue; // HACK
					result.append(' ').append(attribute).append("=\"")
							.append(value).append('\"');
				}
				*/
				if (style == XML_NO_VALUE) result.append('/');
				if (CLDRFile.HACK_ORDER && element.equals("ldml")) result.append(' ');
				result.append('>');
				break;
			case XML_CLOSE:
				result.append("</").append(element).append('>');
				break;
			}
			return result.toString();
		}
		/**
		 * @param element TODO
		 * @param prefix TODO
		 * @param postfix TODO
		 * @param removeLDMLExtras TODO
		 * @param result
		 */
		private Element writeAttributes(String element, String prefix, String postfix,
				boolean removeLDMLExtras, StringBuffer result) {
			Set keys = attributes.keySet();
			if (attributeComparator != null) {
				Set temp = new TreeSet(attributeComparator);
				temp.addAll(keys);
				keys = temp;
			}
			for (Iterator it = keys.iterator(); it.hasNext();) {
				String attribute = (String) it.next();
				String value = (String) attributes.get(attribute);
				if (removeLDMLExtras && suppressionMap != null) {
					Map attribute_value = (Map) suppressionMap.get(element);
					if (attribute_value == null) attribute_value = (Map) suppressionMap.get("*");
					if (attribute_value != null) {
						Object suppressValue = attribute_value.get(attribute);
						if (suppressValue == null) suppressValue = attribute_value.get("*");
						if (suppressValue != null) {
							if (value.equals(suppressValue) || suppressValue.equals("*")) continue;
						}
					}
				}
				result.append(prefix).append(attribute).append("=\"")
						.append(value).append(postfix);
			}
			return this;
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

	/**
	 * @param elementName
	 * @return
	 */
	public int findElement(String elementName) {
		for (int i = 0; i < elements.size(); ++i) {
			Element e = (Element) elements.get(i);
			if (!e.element.equals(elementName)) continue;
			return i;
		}
		return -1;
	}
	/**
	 * add a relative path to this XPathParts.
	 */
	public XPathParts addRelative(String path) {
		if (path.startsWith("//")) {
			elements.clear();
			path = path.substring(1); // strip one
		} else {
			while(path.startsWith("../")) {
				path = path.substring(3);
				trimLast();
			}
			if (!path.startsWith("/")) path = "/" + path;
		}
		return addInternal(path, false);
	}
	/**
	 */
	public XPathParts trimLast() {
		elements.remove(elements.size()-1);
		return this;
	}
	/**
	 * @param parts
	 */
	public XPathParts set(XPathParts parts) {
		elements.clear();
		elements.addAll(parts.elements);
		return this;
	}
	/**
	 * Replace up to i with parts
	 * @param i
	 * @param parts
	 */
	public XPathParts replace(int i, XPathParts parts) {
		List temp = elements;
		elements = new ArrayList();
		set(parts);
		for (;i < temp.size(); ++i) {
			elements.add(temp.get(i));
		}
		return this;
	}

	/**
	 * Utility to write a comment.
	 * @param pw
	 * @param blockComment TODO
	 * @param indent
	 */
	static void writeComment(PrintWriter pw, int indent, String comment, boolean blockComment) {
		// now write the comment
		if (comment.length() == 0) return;
		if (blockComment) {
			Utility.indent(pw, indent);
		} else {
			pw.print(" ");
		}
		pw.print("<!--");
		if (comment.indexOf(NEWLINE) > 0) {
			boolean first = true;
			int countEmptyLines = 0;
			// trim the line iff the indent != 0.
			for (Iterator it = Utility.splitList(comment, '\n', indent != 0, null).iterator(); it.hasNext();) {
				String line = (String) it.next();
				if (line.length() == 0) {
					++countEmptyLines;
					continue;
				}
				if (countEmptyLines != 0) {
					for (int i = 0; i < countEmptyLines; ++i) pw.println();
					countEmptyLines = 0;
				}
				if (first) {
					first = false;
					line = line.trim();
					pw.print(" ");
				} else if (indent != 0) {
					Utility.indent(pw, indent+1);
					pw.print(" ");
				}
				pw.println(line);
			}
			Utility.indent(pw, indent);
		} else {
			pw.print(" ");
			pw.print(comment.trim());
			pw.print(" ");
		}
		pw.print("-->");
		if (blockComment) {
			pw.println();
		}
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
	 */
	public static int isSubLocale(String parent, String possibleSublocale) {
		if (parent.length() > possibleSublocale.length()) return -1;
		if (!possibleSublocale.startsWith(parent)) return -1;
		if (parent.length() == possibleSublocale.length()) return 0;
		if (possibleSublocale.charAt(parent.length()) != '_') return -1; // last subtag too long
		return 1;
	}

	/**
	 * Sets an attribute/value on the first matching element.
	 */
	public XPathParts setAttribute(String elementName, String attributeName, String attributeValue) {
		Map m = findAttributes(elementName);
		m.put(attributeName, attributeValue);
		return this;
	}
}