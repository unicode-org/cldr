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
  private Transliterator prettyPathZoneTransform;
  {
    prettyPathZoneTransform = CheckCLDR.getTransliteratorFromFile("prettyPathZone", "prettyPathZone.txt");
    Transliterator.registerInstance(prettyPathZoneTransform);
    Transliterator foo = Transliterator.getInstance("prettyPathZone");
  }
  private Transliterator prettyPathTransform = CheckCLDR.getTransliteratorFromFile("ID", "prettyPath.txt");

	private Map prettyPath_path = new HashMap();
  private Map path_prettyPath_sortable = new HashMap();
  private Map path_prettyPath = new HashMap();
  private boolean showErrors;
 	
  /**
   * Gets sortable form of the pretty path, and caches the mapping for faster later mapping; see the two argument form.
   * @param path
   * @return pretty path
   */
  public String getPrettyPath(String path) {
    return getPrettyPath(path, true);
  }
	/**
	 * Gets the pretty path, and caches the mapping for faster later mapping. If you use the sortable form, then later
   * you will want to call getOutputForm.
	 * @param path
   * @param sortable true if you want the sortable form
	 * @return pretty path
	 */
 	public String getPrettyPath(String path, boolean sortable) {
		String prettyString = (String) path_prettyPath_sortable.get(path);
		if (path_prettyPath_sortable.get(path) == null) {
			prettyString = prettyPathTransform.transliterate(path);
			// some internal errors, shown here for debugging for now.
			// later make exceptions.
			if (prettyString.indexOf("%%") >= 0) {
				if (showErrors) System.out.println("Warning:\tIncomplete translit:\t" + prettyString + "\t " + path);

			} else if (Utility.countInstances(prettyString, "|") != 2) {
        if (showErrors) System.out.println("Warning:\tpath length != 3: " + prettyString);
			}
			// add to caches
			path_prettyPath_sortable.put(path, prettyString);
      //String prettyNonSortable = sortingGorpRemoval.reset(prettyString).replaceAll("");
//      if (prettyNonSortable.equals(prettyString)) {
//        path_prettyPath.put(path, prettyString);
//      } else {
//        path_prettyPath.put(path, prettyNonSortable);
//        addBackmap(prettyNonSortable, path, prettyPath_path);
//      }
			addBackmap(prettyString, path, prettyPath_path);
		}
    if (!sortable) return getOutputForm(prettyString);
		return prettyString;
 	}
  
  private void addBackmap(String prettyString, String path, Map prettyPath_path_map) {
    String old = (String) prettyPath_path_map.get(prettyString);
    if (old != null) {
      if (showErrors) System.out.println("Warning:\tFailed bijection, " + prettyString);
      if (showErrors) System.out.println("Warning:\tPath1: " + path);
      if (showErrors) System.out.println("Warning:\tPath2: " + old);
    } else {
      prettyPath_path_map.put(prettyString, path); // bijection
    }
  }
 	
 	/**
 	 * Get original path. ONLY works if getPrettyPath was called with the original!
 	 * @param prettyPath
 	 * @return
 	 */
 	public String getOriginal(String prettyPath) {
 		return (String) prettyPath_path.get(prettyPath);
 	}

  /**
   * Return the pretty path with the sorting gorp removed. This is the form that should be displayed to the user.
   * @param prettyPath
   * @return cleaned pretty path
   */
	public String getOutputForm(String prettyPath) {
		return sortingGorpRemoval.reset(prettyPath).replaceAll("");
	}
	
	private static Matcher sortingGorpRemoval = Pattern.compile("(?<=(^|[|]))([0-9]+-)?").matcher("");

  public boolean isShowErrors() {
    return showErrors;
  }
  public PrettyPath setShowErrors(boolean showErrors) {
    this.showErrors = showErrors;
    return this;
  }
}