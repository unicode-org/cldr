/*
 ******************************************************************************
 * Copyright (C) 2010 International Business Machines Corporation and    *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */

package org.unicode.cldr.icu;

import org.unicode.cldr.icu.ICUResourceWriter.Resource;
import org.w3c.dom.Node;

public class WinTZConverter extends SimpleLDMLConverter {
    private static final String []WINTZ_TABLES = {
        LDMLConstants.WINTZ,
        LDMLConstants.TIMEZONE_DATA,
    };
    
    public WinTZConverter(ICULog log, String fileName, String supplementalDir) {
        super(log, fileName, supplementalDir, WINTZ_TABLES);
    }
    
    protected Resource parseInfo(Node root, StringBuilder xpath) {
        if (root != null && root.getNodeName().equals(LDMLConstants.TIMEZONE_DATA)) {
            return TimeZoneDataParser.parseTimeZoneData(root, xpath, log);
        }
        
        return null;
    }
};