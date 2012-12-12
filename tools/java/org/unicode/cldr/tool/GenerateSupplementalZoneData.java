/*
 ******************************************************************************
 * Copyright (C) 2012, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package org.unicode.cldr.tool;

import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CldrUtility;
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
        double deltaTime = System.currentTimeMillis();
        deltaTime = System.currentTimeMillis() - deltaTime;
        ZoneParser zp = new ZoneParser();
        Set<String> tzids = zp.getZoneData().keySet();
        StringBuffer tzbuf = new StringBuffer();
        boolean first = true;
        for ( String z : tzids ) {
            if (!first) {
                tzbuf.append(' ');
            } else {
                first = false;
            }
            tzbuf.append(z);
        }
        
        String sep = CldrUtility.LINE_SEPARATOR + "                ";
        String broken = CldrUtility.breakLines(tzbuf, sep, Pattern.compile(
            "((?:[-+_A-Za-z0-9]+[/])+[-+_A-Za-z0-9])[-+_A-Za-z0-9]*").matcher(""),
            80);
        System.out.println("            <variable id=\"$tzid\" type=\"choice\">" + broken
            + CldrUtility.LINE_SEPARATOR + "            </variable>");

        System.out.println("Elapsed: " + deltaTime / 1000.0 + " seconds");
        System.out.println("Done");
    }
}

 