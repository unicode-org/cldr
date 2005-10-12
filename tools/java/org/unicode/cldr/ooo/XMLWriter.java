/*
 *******************************************************************************
 * Copyright (C) 2002-2005, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package org.unicode.cldr.ooo;

import java.io.*;
import java.util.*;

/**
 * A LocaleWriter takes locale data in standard form and
 * writes it to standard output in a form suitable for
 * loading programatically.
 */
public abstract class XMLWriter {
    protected static final String INDENT_CHARS = 
        "                                        "+
        "                                        "+
        "                                        "+
        "                                        "+
        "                                        ";
        
    protected static final int INDENT_SIZE = 2;  //OO wants 2
    private int indentLevel;
    protected String indentString = "";
    protected  boolean needsIndent;
    protected  int lineLength;
    protected PrintStream out;
    protected PrintStream err;
    
    public XMLWriter(PrintStream out) {
        this.out = out;
        this.err = out;
    }
    public XMLWriter(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    protected void indent() {
        indent(2);
    }
    protected void indent(int amount) {
        indentLevel += amount;
        indentString = INDENT_CHARS.substring(0, indentLevel*INDENT_SIZE);
    }
    
    protected void outdent() {
        outdent(2);
    }
    protected void outdent(int amount) {
        indentLevel -= amount;
        indentString = INDENT_CHARS.substring(0, indentLevel*INDENT_SIZE);
    }

}
