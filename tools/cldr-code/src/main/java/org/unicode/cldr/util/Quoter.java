/*
 *******************************************************************************
 * Copyright (C) 2002-2012, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package org.unicode.cldr.util;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UTF16;

public abstract class Quoter {
    private static boolean DEBUG = false;

    protected boolean quoting = false;
    protected StringBuffer output = new StringBuffer();

    public void setQuoting(boolean value) {
        quoting = value;
    }

    public boolean isQuoting() {
        return quoting;
    }

    public void clear() {
        quoting = false;
        output.setLength(0);
    }

    public int length() {
        return output.length();
    }

    public Quoter append(String string) {
        output.append(string);
        return this;
    }

    public Quoter append(int codepoint) {
        return append(UTF16.valueOf(codepoint));
    }

    // warning, allows access to internals
    @Override
    public String toString() {
        setQuoting(false); // finish quoting
        return output.toString();
    }

    /**
     * Implements standard ICU rule quoting
     */
    public static class RuleQuoter extends Quoter {
        private StringBuffer quoteBuffer = new StringBuffer();

        @Override
        public void setQuoting(boolean value) {
            if (quoting == value) return;
            if (quoting) { // stop quoting
                Utility.appendToRule(output, -1, true, false, quoteBuffer); // close previous quote
            }
            quoting = value;
        }

        @Override
        public Quoter append(String s) {
            if (DEBUG) System.out.println("\"" + s + "\"");
            if (quoting) {
                Utility.appendToRule(output, s, false, false, quoteBuffer);
            } else {
                output.append(s);
            }
            return this;
        }
    }
}