/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source$
 * $Revision$
 *
 *******************************************************************************
 */

package org.unicode.cldr.util;

import java.io.IOException;
import java.io.Writer;

final public class DualWriter extends Writer {

    private boolean autoflush;
    private Writer a;
    private Writer b;

    public DualWriter(Writer a, Writer b) {
        this.a = a;
        this.b = b;
    }

    public DualWriter(Writer a, Writer b, boolean autoFlush) {
        this.a = a;
        this.b = b;
        autoflush = autoFlush;
    }

    public void setAutoFlush(boolean value) {
        autoflush = value;
    }

    public boolean getAutoFlush() {
        return autoflush;
    }

    public void write(char cbuf[],
        int off,
        int len) throws IOException {
        a.write(cbuf, off, len);
        b.write(cbuf, off, len);
        if (autoflush) flush();
    }

    public void close() throws IOException {
        a.close();
        b.close();
    }

    public void flush() throws IOException {
        a.flush();
        b.flush();
    }
}
