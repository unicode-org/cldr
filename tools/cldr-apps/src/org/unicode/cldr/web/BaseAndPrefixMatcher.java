/**
 * Copyright (C) 2011 IBM Corporation and Others. All Rights Reserved.
 *
 */
package org.unicode.cldr.web;

/**
 * @author srl
 * @deprecated better to use a custom subclass of XPathMatcher
 */
public class BaseAndPrefixMatcher extends XPathMatcher {

	int only_base_xpath = XPathTable.NO_XPATH;
	String only_prefix_xpath = null;
	
	private BaseAndPrefixMatcher(int only_base_xpath, String only_prefix_xpath) {
		this.only_base_xpath = only_base_xpath;
		this.only_prefix_xpath = only_prefix_xpath;
	}

	@Override
	public boolean matches(String xpath, int xpid) {
		if(only_prefix_xpath != null && xpath.startsWith(only_prefix_xpath)) {
			return true;
		} else if((xpid != XPathTable.NO_XPATH) && (only_base_xpath == xpid)) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public int getXPath() {
		return only_base_xpath;
	}
	
	@Override
	public String getPrefix() {
		return only_prefix_xpath;
	}

	public static BaseAndPrefixMatcher getInstance(int only_base_xpath2,
			String only_prefix_xpath2) {
		if(only_base_xpath2 != XPathTable.NO_XPATH && only_prefix_xpath2 != null) {
			return new BaseAndPrefixMatcher(only_base_xpath2, only_prefix_xpath2);
		} else {
			return null; // not needed.
		}
	}
}
