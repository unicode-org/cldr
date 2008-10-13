/*
**********************************************************************
* Copyright (c) 2008, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: John Emmons
**********************************************************************
*/
package org.unicode.cldr.icu;

import java.io.*;
import java.util.*;
import java.text.ParseException;
import java.math.BigInteger;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.impl.Utility;

import com.ibm.icu.dev.tool.UOption;
import org.unicode.cldr.icu.SimpleConverter;

import com.ibm.icu.text.NumberFormat;

/**
 * Class to generate CLDR's RBNF rules from existing ICU RBNF text files.
 * @author John C. Emmons
 */

public class RBNFWriter {
    
    private static final int 
        HELP1 = 0,
        HELP2 = 1,
        SOURCEDIR = 2,
        DESTDIR = 3,
        FROMFILE = 4,
        TOFILE = 5;

    private static final UOption[] options = {
        UOption.HELP_H(),
        UOption.HELP_QUESTION_MARK(),
        UOption.create("sourcedir", 's', UOption.REQUIRES_ARG).setDefault("."),
        UOption.create("destdir", 'd', UOption.REQUIRES_ARG).setDefault("."),
        UOption.create("fromfile", 'f', UOption.REQUIRES_ARG).setDefault("root.txt"),
        UOption.create("tofile", 't', UOption.REQUIRES_ARG).setDefault("root.xml"),
    };

    public static void main(String[] args) throws IOException {
        UOption.parseArgs(args, options);

        String fromfile = options[FROMFILE].value;
        String tofile = options[TOFILE].value;
     
        PrintWriter out = BagFormatter.openUTF8Writer(options[DESTDIR].value+File.separator, tofile);
        FileReader inFileReader = new FileReader(options[SOURCEDIR].value+File.separator+fromfile);
        BufferedReader in = new BufferedReader(inFileReader);

        String line = in.readLine();
        boolean firstRuleset = true;
        long currentRuleValue = 0;
        NumberFormat nf = NumberFormat.getInstance();
        char LARROW = 0x2190;
        char RARROW = 0x2192;

        out.println("<ldml>");
        out.println("    <rbnf>");
        while ( line != null ) {
            String workingLine = Utility.unescape(line).trim();
            boolean printRule = true;
            if ( workingLine.startsWith("//")) {
               // Do nothing - this is a comment 
            } else {
                if ( workingLine.startsWith("\"") ) {
                    String ruleText = workingLine.substring(1,workingLine.indexOf("\"",1));
                    String numberString = null;
                    String ruleString = null;
                    if ( ruleText.contains(":")) {
                       String [] parts = ruleText.split(":");
                       if ( parts[0].startsWith("%")) {
                           if ( firstRuleset == false ) {
                               out.println("        </ruleset>");
                           }
                           int idStart = parts[0].lastIndexOf("%") + 1;
                           String tag = parts[0].substring(idStart);
                           out.print("        <ruleset type=\""+tag+"\"");

                           if ( idStart == 2 ) {
                               out.println(" access=\"private\">");
                           } else {
                               out.println(">");
                           }

                           firstRuleset = false;
                           printRule = false;
                           currentRuleValue = 0;
                       } else {
                           numberString = parts[0];
                           ruleString = parts[1];
                           if ( numberString.contains("x") ) {
                                  currentRuleValue = -1;
                           } else {
                               try {
                                  currentRuleValue = nf.parse(numberString).longValue();
                               } catch(ParseException ex) {
                                  currentRuleValue = -1;
                               }
                               numberString = String.valueOf(currentRuleValue);
                           }
                       }
                    }
                    else {
                        ruleString = ruleText;
                        numberString = String.valueOf(currentRuleValue);
                    }
                    if ( printRule == true ) {
                       if ( firstRuleset == true ) {
                           out.println("        <ruleset type=\"spellout\">");
                           firstRuleset = false;
                       }
                       out.println("            <rbnfrule value=\""+numberString+"\">"+ruleString.trim().replace('<',LARROW).replace('>',RARROW)+"</rbnfrule>");
                       int i = ruleString.indexOf(";");
                       while ( i != -1 ) {
                           i = ruleString.indexOf(";",i+1); 
                           currentRuleValue += 1;
                       }
                    }
                }
            }
            line = in.readLine();
        }
        out.println("        </ruleset>");
        out.println("    </rbnf>");
        out.println("</ldml>");
        out.close();
    }
}
