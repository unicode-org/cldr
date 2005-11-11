/*
 ******************************************************************************
 * Copyright (C) 2005, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */

package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.dev.test.util.CollectionUtilities.PrefixIterator;

import org.unicode.cldr.util.CLDRFile.SimpleXMLSource;
import org.unicode.cldr.util.XPathParts.Comments;

import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Freezable;

public abstract class XMLSource implements Freezable {
	public static final String CODE_FALLBACK_ID = "code-fallback";
	private transient XPathParts parts = new XPathParts(null, null);

	private String localeID;
	private boolean isSupplemental;
	protected boolean locked;
	transient String[] fixedPath = new String[1];
	
	public String getLocaleID() {
		return localeID;
	}
	
	public void setLocaleID(String localeID) {
		this.localeID = localeID;
	}
	/**
	 * Adds all the path,value pairs in tempMap.
	 * The paths must be Full Paths.
	 * @param tempMap
	 * @param conflict_resolution
	 */
	public void putAll(Map tempMap, int conflict_resolution) {
		for (Iterator it = tempMap.keySet().iterator(); it.hasNext();) {
			String path = (String) it.next();
			if (conflict_resolution == CLDRFile.MERGE_KEEP_MINE && getValueAtPath(path) != null) continue;
			putValueAtPath(path, (String) tempMap.get(path));
		}
	}
	/**
	 * Adds all the path, value pairs in otherSource.
	 * @param otherSource
	 * @param conflict_resolution
	 */
	public void putAll(XMLSource otherSource, int conflict_resolution) {
		for (Iterator it = otherSource.iterator(); it.hasNext();) {
			String path = (String) it.next();
			if (conflict_resolution == CLDRFile.MERGE_KEEP_MINE && getValueAtDPath(path) != null) continue;
			putValueAtPath(otherSource.getFullPathAtDPath(path), otherSource.getValueAtDPath(path));
		}
	}
	
	/**
	 * Removes all the paths in the collection.
	 * WARNING: must be distinguishedPaths
	 * @param xpaths
	 */
	public void removeAll(Collection xpaths) {
		for (Iterator it = xpaths.iterator(); it.hasNext();) {
			removeValueAtDPath((String) it.next());
		}
	}
	
	/**
	 * Tests whether the full path for this dpath is draft or now.
	 * @param path
	 * @return
	 */
	public boolean isDraft(String path) {
		String fullpath = getFullPath(path);
		if (fullpath.indexOf("[@draft") < 0) return false;
		return parts.set(fullpath).containsAttribute("draft");
	}
	
	public boolean isFrozen() {
		return locked;
	}
	
	/**
	 * Adds the path,value pair. The path must be full path.
	 * @param xpath
	 * @param value
	 */
	public void putValueAtPath(String xpath, String value) {
		if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
		String distinguishingXPath = CLDRFile.getDistinguishingXPath(xpath, fixedPath);	
		putValueAtDPath(distinguishingXPath, value);
		if (!fixedPath[0].equals(distinguishingXPath)) {
			putFullPathAtDPath(distinguishingXPath, fixedPath[0]);
		}
	}
	
	/**
	 * Internal class
	 */
	private static class Alias {
		//public String oldLocaleID;
		public String oldPath;
		public String newLocaleID;
		public String newPath;
		
		public static Alias make(String aliasPath) {
			XPathParts tempAliasParts = new XPathParts(null, null);
			
			int pos = aliasPath.indexOf("/alias");
			if (pos < 0) return null; // quickcheck
			if (!tempAliasParts.set(aliasPath).containsElement("alias")) return null;
			Alias result = new Alias();
			result.oldPath = aliasPath.substring(0,pos); // this is safe
			Map attributes = tempAliasParts.getAttributes(tempAliasParts.size()-1);
			result.newLocaleID = (String) attributes.get("source");
			if (result.newLocaleID != null && result.newLocaleID.equals("locale")) result.newLocaleID = null;
			String relativePath = (String) attributes.get("path");
			if (relativePath == null) result.newPath = result.oldPath;
			else result.newPath = tempAliasParts.trimLast().addRelative(relativePath).toString();
			if (result.newPath.equals(result.oldPath) && result.newLocaleID == null) {
				throw new IllegalArgumentException("Alias must have different path or different source");
			}
			return result;
		}
		public String toString() {
			return 
			//"oldLocaleID: " + oldLocaleID + ", " +
			"oldPath: " + oldPath + ", "
			+ "newLocaleID: " + newLocaleID + ", "
			+ "newPath: " + newPath;
		}
		/**
		 * This function is called on the full path, when we know the distinguishing path matches the oldPath.
		 * So we just want to modify the 
		 * @param oldPath 
		 * @param newPath 
		 * @param result
		 * @return
		 */
		public static String changeNewToOld(String fullPath, String newPath, String oldPath) {
			// for now, just assume check that there are no goofy bits
			if (!fullPath.startsWith(newPath)) {
				throw new IllegalArgumentException("Failure to fix path. "
						+ "\r\n\tfullPath: " + fullPath
						+ "\r\n\toldPath: " + oldPath
						+ "\r\n\tnewPath: " + newPath
						);
			}
			return oldPath + fullPath.substring(newPath.length());
		}
	}
	// should be overriden 
	/**
	 * returns a map from the aliases' parents in the keyset to the alias path
	 */
	public List addAliases(List output) {
		for (Iterator it = iterator(); it.hasNext();) {
			String path = (String) it.next();
			String fullPath = getFullPathAtDPath(path);
			Alias temp = Alias.make(fullPath);
			if (temp == null) continue;
			output.add(temp);
		}
		return output;
	}
	
	/**
	 * Return the localeID of the XMLSource where the path was found
	 * SUBCLASSING: must be overridden in a resolving locale
	 * @param path
	 * @return
	 */
	public String getSourceLocaleID(String path) {
		return getLocaleID();
	}
	
	/**
	 * Remove the value.
	 * SUBCLASSING: must be overridden in a resolving locale
	 * @param xpath
	 */
	public void removeValueAtPath(String xpath) {
		if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
		removeValueAtDPath(CLDRFile.getDistinguishingXPath(xpath, null));
	}
	/**
	 * Get the value.
	 * SUBCLASSING: must be overridden in a resolving locale
	 * @param xpath
	 * @return
	 */
	public String getValueAtPath(String xpath) {
		return getValueAtDPath(CLDRFile.getDistinguishingXPath(xpath, null));
	}
	/**
	 * Get the full path for a distinguishing path
	 * SUBCLASSING: must be overridden in a resolving locale
	 * @param xpath
	 * @return
	 */
	public String getFullPath(String xpath) {
		return getFullPathAtDPath(CLDRFile.getDistinguishingXPath(xpath, null));
	}

	/**
	 * Put the full path for this distinguishing path
	 * The caller will have processed the path, and only call this with the distinguishing path
	 * SUBCLASSING: must be overridden
	 */
	abstract public void putFullPathAtDPath(String distinguishingXPath, String fullxpath);
	/**
	 * Put the distinguishing path, value.
	 * The caller will have processed the path, and only call this with the distinguishing path
	 * SUBCLASSING: must be overridden
	 */
	abstract public void putValueAtDPath(String distinguishingXPath, String value);
	/**
	 * Remove the path, and the full path, and value corresponding to the path.
	 * The caller will have processed the path, and only call this with the distinguishing path
	 * SUBCLASSING: must be overridden
	 */
	abstract public void removeValueAtDPath(String distinguishingXPath);
	
	/**
	 * Get the value at the given distinguishing path
	 * The caller will have processed the path, and only call this with the distinguishing path
	 * SUBCLASSING: must be overridden
	 */
	abstract public String getValueAtDPath(String path);

	/**
	 * Get the full path at the given distinguishing path
	 * The caller will have processed the path, and only call this with the distinguishing path
	 * SUBCLASSING: must be overridden
	 */
	abstract public String getFullPathAtDPath(String path);
	
	/**
	 * Get the comments for the source.
	 * TODO: integrate the Comments class directly into this class
	 * SUBCLASSING: must be overridden
	 */
	abstract public Comments getXpathComments();
	/**
	 * Set the comments for the source.
	 * TODO: integrate the Comments class directly into this class
	 * SUBCLASSING: must be overridden
	 */
	abstract public void setXpathComments(Comments comments);
	/**
	 * @return an iterator over the distinguished paths
	 */
	abstract public Iterator iterator();
	/**
	 * @return an XMLSource for the given localeID; null if unavailable
	 */
	abstract public XMLSource make(String localeID);
	/**
	 * @return all localeIDs for which make(...) returns a non-null value
	 */
	abstract public Set getAvailableLocales();

	/**
	 * @return an iterator over the distinguished paths that start with the prefix.
	 * SUBCLASSING: Normally overridden for efficiency
	 */
	public Iterator iterator(String prefix) {
		return new com.ibm.icu.dev.test.util.CollectionUtilities.PrefixIterator().set(iterator(), prefix);
	}

	/**
	 * @return returns whether resolving or not
	 * SUBCLASSING: Only changed for resolving subclasses
	 */
	public boolean isResolving() {
		return false;
	}
	
	/**
	 * @return returns a resolving class (same one if we are resolving already)
	 * SUBCLASSING: Don't override; there should be only one ResolvingSource
	 */
	public XMLSource getResolving() {
		if (isResolving()) return this;
		return new ResolvingSource(this);
	}

	/**
	 * SUBCLASSING: must be overridden
	 */
	public Object cloneAsThawed() { 
		try {
			XMLSource result = (XMLSource) super.clone();
			result.locked = false;
			return result;
		} catch (CloneNotSupportedException e) {
			throw new InternalError("should never happen");
		}
	}
	/**
	 * for debugging only
	 */
	public String toString() {
		StringBuffer result = new StringBuffer();
		for (Iterator it = iterator(); it.hasNext();) {
			String path = (String) it.next();
			String value = getValueAtDPath(path);
			String fullpath = getFullPathAtDPath(path);
			result.append(fullpath).append(" =\t ").append(value).append("\r\n");
		}
		return result.toString();
	}

	/**
	 * @return returns whether supplemental or not
	 */
	public boolean isSupplemental() {
		return isSupplemental;
	}

	/**
	 * @return sets whether supplemental. Normally only called internall.
	 */
	public void setSupplemental(boolean isSupplemental) {
		this.isSupplemental = isSupplemental;
	}
	
	/**
	 * Internal class for doing resolution
	 * @author davis
	 *
	 */
	private static class ResolvingSource extends XMLSource {
		private XMLSource mySource;
		private transient ParentAndPath parentAndPath = new ParentAndPath();
		
		private static class ParentAndPath {
			String parentID;
			String path;
			XMLSource source;
			String desiredLocaleID;
			transient List aliases = new ArrayList();
			
			public ParentAndPath set(String xpath, XMLSource source, String desiredLocaleID) {
				parentID = source.getLocaleID();
				path = xpath;
				this.source = source;
				this.desiredLocaleID = desiredLocaleID;
				return this;
			}
			public ParentAndPath next() {
				aliases.clear();
				source.addAliases(aliases);
				if (aliases.size() != 0) for (Iterator it = aliases.iterator(); it.hasNext();) {
					Alias alias = (Alias)it.next();
					if (!path.startsWith(alias.oldPath)) continue;
					// TODO fix parent, path, and return
					parentID = alias.newLocaleID;
					if (parentID == null) parentID = desiredLocaleID;
					source = source.make(parentID);
					path = alias.newPath + path.substring(alias.oldPath.length());
					return this;
				}
				parentID = LocaleIDParser.getParent(parentID);
				source = parentID == null ? null : source.make(parentID);
				return this;
			}
		}
		
		public boolean isResolving() {
			return true;
		}

		/*
		 * If there is an alias, then inheritance gets tricky.
		 * If there is a path //ldml/xyz/.../uvw/alias[@path=...][@source=...]
		 * then the parent for //ldml/xyz/.../uvw/abc/.../def/
		 * is source, and the path to search for is really: //ldml/xyz/.../uvw/path/abc/.../def/
		 */
		public static final boolean TRACE_VALUE = false;
		
		public String getValueAtDPath(String xpath) {
			XMLSource currentSource = mySource;
			if (TRACE_VALUE) System.out.println("xpath: " + xpath
					+ "\r\n\tsource: " + currentSource.getClass().getName()
					+ "\r\n\tlocale: " + currentSource.getLocaleID()
					);
	    	String result = currentSource.getValueAtDPath(xpath);
	    	if (result != null) {
	    		if (TRACE_VALUE) System.out.println("result: " + result);
	    		return result;
	    	}
	    	parentAndPath.set(xpath, currentSource, getLocaleID()).next();
	    	while (true) {
	    		if (parentAndPath.parentID == null) {
	    			return constructedItems.getValueAtDPath(xpath);
	    		}
	    		currentSource = make(parentAndPath.parentID); // factory.make(parentAndPath.parentID, false).dataSource;
				if (TRACE_VALUE) System.out.println("xpath: " + parentAndPath.path
						+ "\r\n\tsource: " + currentSource.getClass().getName()
						+ "\r\n\tlocale: " + currentSource.getLocaleID()
						);
	    		result = currentSource.getValueAtDPath(parentAndPath.path);
	    		if (result != null) {
		    		if (TRACE_VALUE) System.out.println("result: " + result);
		    		return result;
		    	}
	    		parentAndPath.next();
	    	}
		}
		public String getFullPathAtDPath(String xpath) {
			XMLSource currentSource = mySource;
	    	String result = currentSource.getValueAtDPath(xpath);
	    	if (result != null) return currentSource.getFullPathAtDPath(xpath);
	    	parentAndPath.set(xpath, currentSource, getLocaleID()).next();
	    	while (true) {
	    		if (parentAndPath.parentID == null) {
	    			return constructedItems.getFullPathAtDPath(xpath);
	    		}
	    		currentSource = make(parentAndPath.parentID); // factory.make(parentAndPath.parentID, false).dataSource;
	    		result = currentSource.getValueAtDPath(parentAndPath.path);
	    		if (result != null) {
	    			result = currentSource.getFullPathAtDPath(parentAndPath.path);
	    			return Alias.changeNewToOld(result, parentAndPath.path, xpath);
	    		}
	    		parentAndPath.next();
	    	}
		}
		public String getSourceLocaleID(String xpath) {
			xpath = CLDRFile.getDistinguishingXPath(xpath, null);
			XMLSource currentSource = mySource;
	    	String result = currentSource.getValueAtDPath(xpath);
	    	if (result != null) return mySource.getLocaleID();
	    	parentAndPath.set(xpath, currentSource, getLocaleID()).next();
	    	while (true) {
	    		if (parentAndPath.parentID == null) {
	    			return CODE_FALLBACK_ID;
	    		}
	    		currentSource = make(parentAndPath.parentID);
	    		result = currentSource.getValueAtDPath(parentAndPath.path);
	    		if (result != null) {
	    			result = currentSource.getFullPathAtDPath(parentAndPath.path);
	    			return currentSource.getLocaleID();
	    		}
	    		parentAndPath.next();
	    	}
		}
		/**
		 * We have to go through the source, add all the paths, then recurse to parents
		 * However, aliases are tricky, so watch it.
		 */
		static final boolean TRACE_FILL = false;
		private void fillKeys(int level, XMLSource currentSource, Alias alias, List excludedAliases, Set resultKeySet) {
			if (TRACE_FILL) {
				if (level > 10) throw new IllegalArgumentException("Stack overflow");
				System.out.println(Utility.repeat("\t",level) + "mySource.getLocaleID(): " + currentSource.getLocaleID());
				System.out.println(Utility.repeat("\t",level) + "currentSource.getClass().getName(): " + currentSource.getClass().getName());
				System.out.println(Utility.repeat("\t",level) + "alias: " + alias);
				System.out.println(Utility.repeat("\t",level) + "cachedKeySet.size(): " + resultKeySet.size());
				System.out.println(Utility.repeat("\t",level) + "excludedAliases: " + excludedAliases);
			}
			List collectedAliases = null;
			// make a pass through, filling all the direct paths, excluding aliases, and collecting others
			for (Iterator it = currentSource.iterator(); it.hasNext();) {
    			String path = (String) it.next();
    			String originalPath = path;
    			if (alias != null) {
    				if (!path.startsWith(alias.newPath)) continue; // skip unless matches alias
					if (!alias.oldPath.equals(alias.newPath)) { // substitute OLD path
						path = alias.oldPath + path.substring(alias.newPath.length());
					}
    			}
    			if (excludedAliases != null && startsWith(path, excludedAliases)) {
    				//System.out.println("Skipping: " + path);
    				continue;
    			}
    			if (path.indexOf("/alias") >= 0) { // quick check
    				String fullPath = currentSource.getFullPathAtDPath(originalPath);
    				// it's ok that the fullpath is not mapped to the old path, since 
    				// the only thing the Alias.make cares about is the last bit
	    			Alias possibleAlias = Alias.make(fullPath);
	    			if (possibleAlias != null) {
	    				if (collectedAliases == null) collectedAliases = new ArrayList();
	    				collectedAliases.add(possibleAlias);
	    			}
    			}
    			resultKeySet.add(path); // Note: we add the aliases
			}
			
			// recurse on the parent, unless at the end of the line (constructedItems
			if (currentSource != constructedItems) { // end of the line?
				if (TRACE_FILL) {
					System.out.println(Utility.repeat("\t",level) + "Recursing on Parent: ");
				}
				XMLSource parentSource = constructedItems; // default
	    		String parentID = LocaleIDParser.getParent(currentSource.getLocaleID());
	    		if (parentID != null) parentSource = make(parentID); // factory.make(parentID, false).dataSource;
	    		if (collectedAliases != null) {
	    			if (excludedAliases == null) excludedAliases = new ArrayList();
	    			else excludedAliases.addAll(collectedAliases);
	    		}
	    		fillKeys(level+1, parentSource, alias, excludedAliases, resultKeySet);
			}
    		
    		// now recurse on the aliases we found
			if (collectedAliases != null) for (Iterator it = collectedAliases.iterator(); it.hasNext();) {
    			if (TRACE_FILL) {
    				System.out.println(Utility.repeat("\t",level) + "Recursing on Alias: ");
    			}
    			Alias foundAlias = (Alias)it.next();
    			// this is important. If the new source is null, use *this* (the desired locale)
    			XMLSource aliasSource = mySource;
    			if (foundAlias.newLocaleID != null) {
    				aliasSource = make(foundAlias.newLocaleID); // factory.make(foundAlias.newLocaleID, false).dataSource;
    			}
    			fillKeys(level+1, aliasSource, foundAlias, null, resultKeySet);
    		}
			if (TRACE_FILL) System.out.println(Utility.repeat("\t",level) + "=> cachedKeySet.size(): " + resultKeySet.size());
		}
		
		transient Set cachedKeySet = null;
		/**
		 * This function is kinda tricky. What it does it come up with the set of all the paths that
		 * would return a value, fully resolved. This wouldn't be a problem but for aliases.
		 * Whenever there is an alias oldpath = p relativePath = x source=y
		 * Then you have to *not* add any of the oldpath... from the normal inheritance heirarchy
		 * Instead from source, you see everything that matches oldpath+relativePath + z, and for each one
		 * add oldpath+z
		 */
		public Iterator iterator() {
	    	return getCachedKeySet().iterator();
		}
		private Set getCachedKeySet() {
			if (cachedKeySet == null) {
				cachedKeySet = new HashSet();
				fillKeys(0, mySource, null, null, cachedKeySet);
		    	//System.out.println("CachedKeySet: " + cachedKeySet);
		    	//cachedKeySet.addAll(constructedItems.keySet());
		    	cachedKeySet = Collections.unmodifiableSet(cachedKeySet);
			}
			return cachedKeySet;
		}
		private static boolean startsWith(String path, List aliasPaths) {
    		for (Iterator it = aliasPaths.iterator(); it.hasNext();) {
    			if (path.startsWith(((Alias)it.next()).oldPath)) return true;
    		}
    		return false;
		}
		public void putFullPathAtDPath(String distinguishingXPath, String fullxpath) {
			throw new UnsupportedOperationException("Resolved CLDRFiles are read-only");
		}
		public void putValueAtDPath(String distinguishingXPath, String value) {
			throw new UnsupportedOperationException("Resolved CLDRFiles are read-only");
		}
		public Comments getXpathComments() {
			return mySource.getXpathComments();
		}
		public void setXpathComments(Comments path) {
			throw new UnsupportedOperationException("Resolved CLDRFiles are read-only");		
		}
		public void removeValueAtDPath(String xpath) {
			throw new UnsupportedOperationException("Resolved CLDRFiles are read-only");
		}
		public Object freeze() {
			throw new UnsupportedOperationException("Resolved CLDRFiles can't be locked");
		}
		public ResolvingSource(/*Factory factory, */XMLSource source) {
			super();
			//this.factory = factory;
			mySource = source;
		}
		public String getLocaleID() {
			return mySource.getLocaleID();
		}
		
		static XMLSource constructedItems = new SimpleXMLSource(null, null);
		static {
			StandardCodes sc = StandardCodes.make();
			Map countries_zoneSet = sc.getCountryToZoneSet();
			Map zone_countries = sc.getZoneToCounty();
			
			//Set types = sc.getAvailableTypes();
			for (int typeNo = 0; typeNo <= CLDRFile.TZ_START; ++typeNo ) {
				String type = CLDRFile.getNameName(typeNo);
				//int typeNo = typeNameToCode(type);
				//if (typeNo < 0) continue;
				String type2 = (typeNo == CLDRFile.CURRENCY_SYMBOL) ? CLDRFile.getNameName(CLDRFile.CURRENCY_NAME)
						: (typeNo >= CLDRFile.TZ_START) ? "tzid"
						: type;				
				Set codes = sc.getGoodAvailableCodes(type2);
				//String prefix = CLDRFile.NameTable[typeNo][0];
				//String postfix = CLDRFile.NameTable[typeNo][1];
				//String prefix2 = "//ldml" + prefix.substring(6); // [@version=\"" + GEN_VERSION + "\"]
				for (Iterator codeIt = codes.iterator(); codeIt.hasNext(); ) {
					String code = (String)codeIt.next();
					String value = code;
					if (typeNo == CLDRFile.TZ_EXEMPLAR) { // skip single-zone countries
						String country = (String) zone_countries.get(code);
						Set s = (Set) countries_zoneSet.get(country);
						if (s != null && s.size() == 1) continue;
						value = TimezoneFormatter.getFallbackName(value);
					}
					//String path = prefix + code + postfix;
					String fullpath = CLDRFile.getKey(typeNo, code);
					//System.out.println(fullpath + "\t=> " + code);
					constructedItems.putValueAtPath(fullpath, value);
				}
			}
			constructedItems.freeze();
	    	//System.out.println("constructedItems: " + constructedItems);
		}
		public XMLSource make(String localeID) {
			return mySource.make(localeID);
		}
		public Set getAvailableLocales() {
			return mySource.getAvailableLocales();
		}
	}
}