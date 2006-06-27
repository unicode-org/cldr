/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Mark Davis
**********************************************************************
*/
package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR;

import com.ibm.icu.text.Replaceable;
import com.ibm.icu.text.Transliterator;

public class PrettyPath {
	private Transliterator prettyPathTransform = CheckCLDR.getTransliteratorFromFile("ID", "prettyPath.txt");
	private Map prettyPath_path = new HashMap();
	private Map path_prettyPath = new HashMap();
 	
	/**
	 * Gets sortable form of the pretty path, and caches the mapping for faster later mapping.
	 * @param path
	 * @return
	 */
 	public String getPrettyPath(String path) {
		String prettyString = (String) path_prettyPath.get(path);
		if (path_prettyPath.get(path) == null) {
			prettyString = prettyPathTransform.transliterate(path);
			// some internal errors, shown here for debugging for now.
			// later make exceptions.
			if (prettyString.indexOf("%%") >= 0) {
				System.out.println("Warning: Incomplete translit:\t" + prettyString + "\t " + path);

			} else if (Utility.countInstances(prettyString, "|") != 2) {
				System.out.println("Warning: path length != 3: " + prettyString);
			}
			// add to caches
			path_prettyPath.put(path, prettyString);
			String old = (String) prettyPath_path.get(prettyString);
			if (old != null) {
				System.out.println("Warning: Failed bijection, " + prettyString);
				System.out.println("\tPath1: " + path);
				System.out.println("\tPath2: " + old);
			}
			prettyPath_path.put(prettyString, path); // bijection
		}
		return prettyString;
 	}
 	
 	/**
 	 * Get original path. ONLY works if getPrettyPath was called with the original!
 	 * @param prettyPath
 	 * @return
 	 */
 	public String getOriginal(String prettyPath) {
 		return (String) prettyPath_path.get(prettyPath);
 	}

	public String getOutputForm(String prettyPath) {
		return matcher.reset(prettyPath).replaceAll("|");
	}
	
	private static Matcher matcher = Pattern.compile("[|]([0-9]+-)?").matcher("");
}