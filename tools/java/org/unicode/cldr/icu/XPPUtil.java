// Copyright 2009 Google Inc. All Rights Reserved.

package org.unicode.cldr.icu;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.XPathParts;

// Notes:
// - xpp.set (currently) resets the entire state of the xpp, so there are no side effects
// of these calls.
// - This is not used in a multi-threaded environment, so a static instance is ok.  If
// this turns out not to be true, make thread-local.

public class XPPUtil {
    public static String getXpathName(String xpath) {
        XPathParts xpp = XPathParts.getTestInstance(xpath);
        return xpp.getElement(-1);
    }

    public static String getXpathName(String xpath, int pos) {
        XPathParts xpp = XPathParts.getTestInstance(xpath);
        return xpp.getElement(pos);
    }

    public static String getAttributeValue(String xpath, String element, String attribute) {
        XPathParts xpp = XPathParts.getTestInstance(xpath);
        int el = xpp.findElement(element);
        if (el == -1) {
            return null;
        }
        return xpp.getAttributeValue(el, attribute);
    }

    public static String getAttributeValue(String xpath, String attribute) {
        XPathParts xpp = XPathParts.getTestInstance(xpath);
        return xpp.getAttributeValue(-1, attribute);
    }

    public static String getBasicAttributeValue(CLDRFile whichFile, String xpath, String attribute) {
        String fullPath = whichFile.getFullXPath(xpath);
        if (fullPath == null) {
            return null;
        }
        return getAttributeValue(fullPath, attribute);
    }

    public static String findAttributeValue(CLDRFile file, String xpath, String attribute) {
        String fullPath = file.getFullXPath(xpath);
        XPathParts xpp = XPathParts.getTestInstance(fullPath);
        for (int j = 1; j <= xpp.size(); j++) {
            String v = xpp.getAttributeValue(0 - j, attribute);
            if (v != null)
                return v;
        }
        return null;
    }
}