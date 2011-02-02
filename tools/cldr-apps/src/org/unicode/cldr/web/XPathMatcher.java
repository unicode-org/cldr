/**
 * Copyright (C) 2011 IBM Corporation and Others. All Rights Reserved.
 *
 */
package org.unicode.cldr.web;

import java.util.regex.Pattern;

/**
 * @author srl
 *
 * Class to encapsulate matching an xpath.
 */
public abstract class XPathMatcher {
	/**
	 * XPathTable for use.
	 */
	protected XPathTable xpt = null;
	
	/**
	 * If this matcher matches a single xpath by number, return it.
	 * @return
	 */
	public int getXPath() {
		return XPathTable.NO_XPATH;
	}
	
	/**
	 * Return the prefix which is matched. Defaults to all.
	 * @return
	 */
	public String getPrefix() {
		return "//ldml";
	}
	
	/**
	 * Is this xpath matched?
	 * @param xpath string to match
	 * @param xpid ( may be XPathTable.NO_XPATH )
	 */
	public abstract boolean matches(String xpath, int xpid);
	
	/**
	 * Create a new matcher that matches the intersection of a and b
	 * @param a
	 * @param b
	 * @return a new matcher
	 */
	public static XPathMatcher intersection(final XPathMatcher a, final XPathMatcher b) {
		if(a==null) return b;
		if(b==null) return a;
		return new XPathMatcher() {
			private final XPathMatcher left = a;
			private final XPathMatcher right = b;
			@Override
			public boolean matches(String xpath, int xpid) {
				return left.matches(xpath,xpid)&&right.matches(xpath,xpid);
			}
			@Override
			public int getXPath() {
				int r;
				if(left.getXPath() == (r=right.getXPath())) {
					return r;
				} else {
					return XPathTable.NO_XPATH;
				}
			}
			@Override
			public String getPrefix() {
				String l = left.getPrefix();
				String r = right.getPrefix();
				if(l==null) return r;
				if(r==null) return l;
				if(l!=null&&l.equals(r)) {
					return l;
				} else {
					return null;
				}
			}
		};
	}
	
	/**
	 * Create a new matcher that matches a regex-limited subset of a
	 * @param pattern regex pattern to use
	 * @return
	 */
	public static XPathMatcher regex(final XPathMatcher x, final Pattern pattern) {
		if(pattern == null) return x;
		return new XPathMatcher() {
			@Override
			public int getXPath() {
				return (x==null)?XPathTable.NO_XPATH:x.getXPath();
			}
			@Override
			public String getPrefix() {
				return (x==null)?null:x.getPrefix();
			}
			@Override
			public boolean matches(String xpath, int xpid) {
				if((x!=null) && !x.matches(xpath,xpid)) {
					return false;
				} else {
					return pattern.matcher(xpath).matches();
				}
			}
		};
	}
	
	public static XPathMatcher regex(final Pattern pattern) {
		return regex(null, pattern);
	}
}
