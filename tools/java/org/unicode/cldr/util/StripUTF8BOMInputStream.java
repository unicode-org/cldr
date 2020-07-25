/*
 ******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */

package org.unicode.cldr.util;

import java.io.IOException;
import java.io.InputStream;

public class StripUTF8BOMInputStream extends InputStream {
    InputStream base;

    public StripUTF8BOMInputStream(InputStream base) {
        this.base = InputStreamFactory.buffer(base);
    }

    boolean checkForUTF8BOM = true;

    /*
     * (non-Javadoc)
     *
     * @see java.io.InputStream#read()
     */
    @Override
    public int read() throws IOException {
        int result = base.read();
        if (!checkForUTF8BOM) return result;
        // complicated by still wanting to do one delegate read per read
        // so we just skip first char if it starts with EF, assuming valid UTF-8
        checkForUTF8BOM = false;
        if (result != 0xEF) return result;
        result = base.read();
        result = base.read();
        result = base.read();
        return result;
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (base != null) {
            base.close();
            base = null;
        }
    }

}