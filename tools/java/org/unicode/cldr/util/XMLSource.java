/*
 ******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */

package org.unicode.cldr.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.util.XPathParts.Comments;

import com.ibm.icu.util.Freezable;

public abstract class XMLSource implements Freezable {
	private String localeID;
	private boolean isSupplemental;
	protected boolean locked;
	
	public String getLocaleID() {
		return localeID;
	}
	
	public void setLocaleID(String localeID) {
		this.localeID = localeID;
	}
	public void putAll(Map tempMap, int conflict_resolution) {
		for (Iterator it = tempMap.keySet().iterator(); it.hasNext();) {
			String path = (String) it.next();
			if (conflict_resolution == CLDRFile.MERGE_KEEP_MINE && getValue(path) != null) continue;
			putPathValue(path, (String) tempMap.get(path));
		}
	}
	public void putAll(XMLSource otherSource, int conflict_resolution) {
		for (Iterator it = otherSource.keySet().iterator(); it.hasNext();) {
			String path = (String) it.next();
			if (conflict_resolution == CLDRFile.MERGE_KEEP_MINE && getValue(path) != null) continue;
			putPathValue(otherSource.getFullXPath(path), otherSource.getValue(path));
		}
	}
	
	public void removeAll(Collection xpaths) {
		for (Iterator it = xpaths.iterator(); it.hasNext();) {
			remove((String) it.next());
		}
	}
	transient XPathParts parts = new XPathParts(null, null);
	
	public boolean isDraft(String path) {
		String fullpath = getFullXPath(path);
		if (fullpath.indexOf("[@draft") < 0) return false;
		return parts.set(fullpath).containsAttribute("draft");
	}
	
	public boolean isFrozen() {
		return locked;
	}
	
	public void putPathValue(String xpath, String value) {
		if (locked) throw new UnsupportedOperationException("Attempt to modify locked object");
		String distinguishingXPath = CLDRFile.getDistinguishingXPath(xpath);		
		putValue(distinguishingXPath, value);
		if (!xpath.equals(distinguishingXPath)) {
			putFullPath(distinguishingXPath, xpath);
		}
	}
	
	public static class Alias {
		//public String oldLocaleID;
		public String oldPath;
		public String newLocaleID;
		public String newPath;
		
		private static XPathParts tempParts = new XPathParts(null, null);
		
		public static Alias make(String aliasPath) {
			int pos = aliasPath.indexOf("/alias");
			if (pos < 0) return null; // quickcheck
			if (!tempParts.set(aliasPath).containsElement("alias")) return null;
			Alias result = new Alias();
			result.oldPath = aliasPath.substring(0,pos); // this is safe
			Map attributes = tempParts.getAttributes(tempParts.size()-1);
			result.newLocaleID = (String) attributes.get("source");
			if (result.newLocaleID != null && result.newLocaleID.equals("locale")) result.newLocaleID = null;
			String relativePath = (String) attributes.get("path");
			if (relativePath == null) result.newPath = result.oldPath;
			else result.newPath = tempParts.trimLast().addRelative(relativePath).toString();
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
		for (Iterator it = keySet().iterator(); it.hasNext();) {
			String path = (String) it.next();
			String fullPath = getFullXPath(path);
			Alias temp = Alias.make(fullPath);
			if (temp == null) continue;
			output.add(temp);
		}
		return output;
	}
	
	// must be overriden
	
	abstract protected void putFullPath(String distinguishingXPath, String fullxpath);
	abstract protected void putValue(String distinguishingXPath, String value);
	abstract public String getValue(String path);
	abstract public String getFullXPath(String path);
	abstract public Comments getXpathComments();
	abstract public void setXpathComments(Comments path);
	abstract public int size();
	abstract public void remove(String xpath);
	abstract public Set keySet();
	abstract public XMLSource make(String localeID);
	abstract public Set getAvailableLocales();
	/**
	 * Warning: must be overriden
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
		for (Iterator it = keySet().iterator(); it.hasNext();) {
			String path = (String) it.next();
			String value = getValue(path);
			String fullpath = getFullXPath(path);
			result.append(fullpath).append(" =\t ").append(value).append("\r\n");
		}
		return result.toString();
	}

	public boolean isSupplemental() {
		return isSupplemental;
	}

	public void setSupplemental(boolean isSupplemental) {
		this.isSupplemental = isSupplemental;
	}
}