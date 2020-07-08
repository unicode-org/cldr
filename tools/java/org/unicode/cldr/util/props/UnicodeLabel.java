/*
 *******************************************************************************
 * Copyright (C) 1996-2012, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package org.unicode.cldr.util.props;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UTF16;

public abstract class UnicodeLabel implements com.ibm.icu.text.Transform<Integer, String> {

    public abstract String getValue(int codepoint, boolean isShort);

    @Override
    public String transform(Integer codepoint) {
        return getValue(codepoint, true);
    }

    public String getValue(String s, String separator, boolean withCodePoint) {
        if (s.length() == 1) { // optimize simple case
            return getValue(s.charAt(0), withCodePoint);
        }
        StringBuffer sb = new StringBuffer();
        int cp;
        for (int i = 0; i < s.length(); i+=UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(s,i);
            if (i != 0) sb.append(separator);
            sb.append(getValue(cp, withCodePoint));
        }
        return sb.toString();
    }

    public int getMaxWidth(boolean isShort) {
        return 0;
    }

    private static class Hex extends UnicodeLabel {
        @Override
        public String getValue(int codepoint, boolean isShort) {
            if (isShort) return Utility.hex(codepoint,4);
            return "U+" + Utility.hex(codepoint,4);
        }
    }

    public static class Constant extends UnicodeLabel {
        private String value;
        public Constant(String value) {
            if (value == null) value = "";
            this.value = value;
        }
        @Override
        public String getValue(int codepoint, boolean isShort) {
            return value;
        }
        @Override
        public int getMaxWidth(boolean isShort) {
            return value.length();
        }
    }
    public static final UnicodeLabel NULL = new Constant("");
    public static final UnicodeLabel HEX = new Hex();
}
