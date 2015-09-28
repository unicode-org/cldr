/*
 ******************************************************************************
 * Copyright (C) 2012, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package org.unicode.cldr.tool;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.ZoneParser;

/**
 * Simple program to generate the valid tzids for supplementalMetadata.xml
 */
public class GenerateSupplementalZoneData {

    /**
     * Count the data.
     *
     * @throws IOException
     */
    public static void main(String[] args) throws Exception {
        ZoneParser zp = new ZoneParser();
        Set<String> tzids = new TreeSet<String>();
        tzids.addAll(zp.getZoneData().keySet());
        // Add POSIX legacy IDs
        tzids.add("EST5EDT");
        tzids.add("CST6CDT");
        tzids.add("MST7MDT");
        tzids.add("PST8PDT");

        StringBuffer tzbuf = new StringBuffer();
        boolean first = true;
        for (String z : tzids) {
            if (!first) {
                tzbuf.append(' ');
            } else {
                first = false;
            }
            tzbuf.append(z);
        }

        String sep = CldrUtility.LINE_SEPARATOR + "                ";
        String broken = CldrUtility.breakLines(tzbuf, sep, PatternCache.get(
            "((?:[-+_A-Za-z0-9]+[/])+[-+_A-Za-z0-9])[-+_A-Za-z0-9]*").matcher(""),
            80);
        System.out.println("            <variable id=\"$tzid\" type=\"choice\">" + broken
            + CldrUtility.LINE_SEPARATOR + "            </variable>");
    }
}
