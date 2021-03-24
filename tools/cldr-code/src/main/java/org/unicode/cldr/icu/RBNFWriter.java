/*
 **********************************************************************
 * Copyright (c) 2008, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: John Emmons
 **********************************************************************
 */
package org.unicode.cldr.icu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.Date;

import org.unicode.cldr.draft.FileUtilities;

import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;

/**
 * Class to generate CLDR's RBNF rules from existing ICU RBNF text files.
 *
 * @author John C. Emmons
 */

public class RBNFWriter {

    private static final int SOURCEDIR = 2,
        DESTDIR = 3,
        FROMFILE = 4,
        TOFILE = 5,
        SPEC = 6,
        COPYRIGHT = 7;

    private static final UOption[] options = {
        UOption.HELP_H(),
        UOption.HELP_QUESTION_MARK(),
        UOption.create("sourcedir", 's', UOption.REQUIRES_ARG).setDefault("."),
        UOption.create("destdir", 'd', UOption.REQUIRES_ARG).setDefault("."),
        UOption.create("fromfile", 'f', UOption.REQUIRES_ARG).setDefault("root.txt"),
        UOption.create("tofile", 't', UOption.REQUIRES_ARG).setDefault("root.xml"),
        UOption.create("spec", 'x', UOption.REQUIRES_ARG).setDefault("false"),
        UOption.create("copyright", 'c', UOption.REQUIRES_ARG).setDefault("true")
    };

    public static void main(String[] args) throws IOException {
        UOption.parseArgs(args, options);

        String fromfile = options[FROMFILE].value;
        String tofile = options[TOFILE].value;

        int dot = fromfile.indexOf('.');
        String localeSpec;

        if (dot > 0)
            localeSpec = fromfile.substring(0, dot);
        else
            localeSpec = fromfile;

        String[] pieces = localeSpec.split("_");
        String language = pieces[0];
        Date now = Calendar.getInstance().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        System.out.println(tofile);

        PrintWriter out = FileUtilities.openUTF8Writer(options[DESTDIR].value + File.separator, tofile);
        FileInputStream inFileStream = new FileInputStream(options[SOURCEDIR].value + File.separator + fromfile);
        InputStreamReader inFileReader = new InputStreamReader(inFileStream, "UTF-8");
        BufferedReader in = new BufferedReader(inFileReader);

        String line = in.readLine();
        boolean firstRuleset = true;
        BigInteger currentRuleValue = BigInteger.ZERO;
        char LARROW = 0x2190;
        char RARROW = 0x2192;

        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        out.println("<!DOCTYPE ldml SYSTEM \"../../common/dtd/ldml.dtd\">");
        if (options[COPYRIGHT].value.equals("true")) {
            sdf.applyPattern("yyyy");
            out.println("<!--");
            out.println("Copyright © 1991-" + sdf.format(now) + " Unicode, Inc.\n" +
                "CLDR data files are interpreted according to the LDML specification (http://unicode.org/reports/tr35/)\n" +
                "For terms of use, see http://www.unicode.org/copyright.html");
            out.println("-->");
            sdf.applyPattern("yyyy/MM/dd HH:mm:ss");

        }
        out.println("<ldml>");
        out.println("    <identity>");
        out.println("        <version number=\"$Revision$\"/>");
        out.println("        <language type=\"" + language + "\"/>");

        if (pieces.length > 1)
            if (pieces[1].length() == 2)
            out.println("        <territory type=\"" + pieces[1] + "\"/>");
            else
            out.println("        <script type=\"" + pieces[1] + "\"/>");

        out.println("    </identity>");
        if (options[SPEC].value.equals("true")) {
            out.println("</ldml>");
            out.close();
            in.close();
            return;
        }

        out.println("    <rbnf>");
        out.println("        <rulesetGrouping type=\"SpelloutRules\">");
        while (line != null) {
            String workingLine = Utility.unescape(line).trim();
            boolean printRule = true;
            if (workingLine.startsWith("//")) {
                // Do nothing - this is a comment
            } else {
                String ruleText = workingLine;
                if (workingLine.startsWith("\"")) {
                    ruleText = workingLine.substring(1, workingLine.indexOf("\"", 1));
                }
                String numberString = null;
                String radixString = null;
                String ruleString = null;
                if (ruleText.contains(":")) {
                    String[] parts = ruleText.split(":");
                    if (parts[0].startsWith("%")) {
                        if (!firstRuleset) {
                            out.println("            </ruleset>");
                        }
                        int idStart = parts[0].lastIndexOf("%") + 1;
                        String tag = parts[0].substring(idStart);
                        out.print("            <ruleset type=\"" + tag + "\"");

                        if (idStart == 2) {
                            out.println(" access=\"private\">");
                        } else {
                            out.println(">");
                        }

                        firstRuleset = false;
                        printRule = false;
                        currentRuleValue = BigInteger.ZERO;
                        if (parts.length > 1 && parts[1].trim().length() > 0) {
                            printRule = true;
                            ruleString = parts[1].trim();
                            numberString = currentRuleValue.toString();
                        }
                    } else {
                        numberString = parts[0];
                        ruleString = parts[1];
                        if (numberString.contains("x") || numberString.contains(">")
                            || numberString.equals("Inf") || numberString.equals("NaN")) {
                            currentRuleValue = new BigInteger("-1");
                            numberString = numberString.replace('>', RARROW).replaceAll(",", "");
                        } else {
                            if (numberString.contains("/")) {
                                String[] numparts = numberString.split("/");
                                numberString = numparts[0];
                                radixString = numparts[1];
                            }
                            try {
                                currentRuleValue = new BigInteger(numberString.replaceAll(",", ""));
                            } catch (NumberFormatException ex) {
                                currentRuleValue = new BigInteger("-1");
                            }
                            numberString = currentRuleValue.toString();
                        }
                    }
                } else {
                    ruleString = ruleText;
                    numberString = currentRuleValue.toString();
                }
                if (printRule) {
                    if (firstRuleset) {
                        out.println("            <ruleset type=\"spellout\">");
                        firstRuleset = false;
                    }
                    if (radixString != null) {
                        out.println("                <rbnfrule value=\"" + numberString + "\" radix=\""
                            + radixString + "\">" + ruleString.trim().replace('<', LARROW).replace('>', RARROW)
                            + "</rbnfrule>");
                    } else {
                        out.println("                <rbnfrule value=\"" + numberString + "\">"
                            + ruleString.trim().replace('<', LARROW).replace('>', RARROW) + "</rbnfrule>");
                    }
                    int i = ruleString.indexOf(";");
                    while (i != -1) {
                        i = ruleString.indexOf(";", i + 1);
                        currentRuleValue = currentRuleValue.add(BigInteger.ONE);
                    }
                }
            }
            line = in.readLine();
        }
        in.close();
        out.println("            </ruleset>");
        out.println("        </rulesetGrouping>");
        out.println("    </rbnf>");
        out.println("</ldml>");
        out.close();
    }
}
