/* Copyright (C) 2007-2010 Google and others.  All Rights Reserved. */
/* Copyright (C) 2007-2010 IBM Corp. and others. All Rights Reserved. */

package org.unicode.cldr.test;

import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckExemplars.ExemplarType;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.With;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.util.PrettyPrinter;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateTimePatternGenerator.FormatParser;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;

/**
 * Class for processing the input and output of CLDR data for use in the
 * Survey Tool and other tools.
 */
public class DisplayAndInputProcessor {

    public static final boolean DEBUG_DAIP = CldrUtility.getProperty("DEBUG_DAIP", false);
    
    public static final UnicodeSet RTL = new UnicodeSet("[[:Bidi_Class=Arabic_Letter:][:Bidi_Class=Right_To_Left:]]").freeze();

    public static final UnicodeSet TO_QUOTE = (UnicodeSet) new UnicodeSet(
            "[[:Cn:]" +
            "[:Default_Ignorable_Code_Point:]" +
            "[:patternwhitespace:]" +
            "[:Me:][:Mn:]]" // add non-spacing marks
    ).freeze();

    private Collator col;

    private Collator spaceCol;

    private FormatParser formatDateParser = new FormatParser();

    private PrettyPrinter pp;

    /**
     * Constructor, taking cldrFile.
     * @param cldrFileToCheck
     */
    public DisplayAndInputProcessor(CLDRFile cldrFileToCheck) {
        String locale = cldrFileToCheck.getLocaleID();
	init(locale);
    }
     void init(String locale) {
        col = Collator.getInstance(new ULocale(locale));
        spaceCol = Collator.getInstance(new ULocale(locale));

        pp = new PrettyPrinter().setOrdering(Collator.getInstance(ULocale.ROOT)).setSpaceComparator(Collator.getInstance(ULocale.ROOT).setStrength2(Collator.PRIMARY))
        .setCompressRanges(true)
        .setToQuote(new UnicodeSet(TO_QUOTE))
        .setOrdering(col)
        .setSpaceComparator(spaceCol);
    }

    /**
     * Constructor, taking locale.
     * @param locale
     */
    public DisplayAndInputProcessor(ULocale locale) {
	init(locale.toString());
    }

    /**
     * Process the value for display. The result is a string for display in the
     * Survey tool or similar program.
     * 
     * @param path
     * @param value
     * @param fullPath
     * @return
     */
    public synchronized String processForDisplay(String path, String value) {
        if (path.contains("exemplarCharacters")) {
            if (value.startsWith("[") && value.endsWith("]")) {
                value = value.substring(1, value.length() - 1);
            }
            
            value = replace(NEEDS_QUOTE1, value, "$1\\\\$2$3");
            value = replace(NEEDS_QUOTE2, value, "$1\\\\$2$3");

//            if (RTL.containsSome(value) && value.startsWith("[") && value.endsWith("]")) {
//                return "\u200E[\u200E" + value.substring(1,value.length()-2) + "\u200E]\u200E";
//            }
        } else if (path.contains("stopword")) {
            return value.trim().isEmpty() ? "NONE" : value;
        }
        return value;
    }

    /**
     * Process the value for input. The result is a cleaned-up value. For example,
     * an exemplar set is modified to be in the normal format, and any missing [ ]
     * are added (a common omission on entry). If there are any failures then the
     * original value is returned, so that the proper error message can be given.
     * 
     * @param path
     * @param value
     * @param internalException TODO
     * @param fullPath
     * @return
     */
    public synchronized String processInput(String path, String value, Exception[] internalException) {
        String original = value;
        if (internalException != null) {
            internalException[0] = null;
        }
        try {
            // fix grouping separator if space
            if (path.startsWith("//ldml/numbers/symbols/group")) {
                if (value.equals(" ")) {
                    value = "\u00A0";
                }
            }
            // all of our values should not have leading or trailing spaces, except insertBetween
            if (!path.contains("/insertBetween") && !path.contains("/localeSeparator")) {
                value = value.trim();
            }

            // fix date patterns
            if (hasDatetimePattern(path)) {
                formatDateParser.set(value);
                String newValue = formatDateParser.toString();
                if (!value.equals(newValue)) {
                    value = newValue;
                }
            }

            if (path.startsWith("//ldml/numbers") && path.indexOf("Format[")>=0&& path.indexOf("/pattern")>=0) {
                String newValue = value.replaceAll("([%\u00A4]) ", "$1\u00A0")
                .replaceAll(" ([%\u00A4])", "\u00A0$1");
                if (!value.equals(newValue)) {
                    value = newValue;
                }
            }

            // check specific cases
            if (path.contains("/exemplarCharacters")) {
                // clean up the user's input.
                // first, fix up the '['
                value = value.trim();

                value = replace(NEEDS_QUOTE1, value, "$1\\\\$2$3");
                value = replace(NEEDS_QUOTE2, value, "$1\\\\$2$3");

                if (!value.startsWith("[")) {
                    value = "[" + value;
                }

                if (!value.endsWith("]")) {
                    value = value + "]";
                }

                UnicodeSet exemplar = new UnicodeSet(value);
                XPathParts parts = new XPathParts().set(path);
                final String type = parts.getAttributeValue(-1, "type");
                ExemplarType exemplarType = type == null ? ExemplarType.main : ExemplarType.valueOf(type);
                value = getCleanedUnicodeSet(exemplar, pp, exemplarType);
            } else if (path.contains("stopword")) {
                if (value.equals("NONE")) {
                    value ="";
                }
            }
            return value;
        } catch (RuntimeException e) {
            if (internalException != null) {
                internalException[0] = e;
            }
            return original;
        }
    }
    private String replace(Pattern pattern, String value, String replacement) {
        String value2 = pattern.matcher(value).replaceAll(replacement);
        if (!value.equals(value2)) {
            System.out.println("\n" + value + " => " + value2);
        }
        return value2;
    }
    
    static Pattern REMOVE_QUOTE1 = Pattern.compile("(\\s)(\\\\[-\\}\\]\\&])()");
    static Pattern REMOVE_QUOTE2 = Pattern.compile("(\\\\[\\-\\{\\[\\&])(\\s)"); //([^\\])([\\-\\{\\[])(\\s)

    static Pattern NEEDS_QUOTE1 = Pattern.compile("(\\s|$)([-\\}\\]\\&])()");
    static Pattern NEEDS_QUOTE2 = Pattern.compile("([^\\\\])([\\-\\{\\[\\&])(\\s)"); //([^\\])([\\-\\{\\[])(\\s)
    
    public static boolean hasDatetimePattern(String path) {
        return path.indexOf("/dates") >= 0
        && ((path.indexOf("/pattern") >= 0 && path.indexOf("/dateTimeFormat") < 0)
                || path.indexOf("/dateFormatItem") >= 0
                || path.contains("/intervalFormatItem"));
    }

    public static String getCleanedUnicodeSet(UnicodeSet exemplar, PrettyPrinter prettyPrinter, ExemplarType exemplarType) {
        String value;
        prettyPrinter.setCompressRanges(exemplar.size() > 100);
        value = exemplar.toPattern(false);
        UnicodeSet toAdd = new UnicodeSet();

        for (UnicodeSetIterator usi = new UnicodeSetIterator(exemplar); usi.next(); ) {
            String string = usi.getString();
            if (string.equals("ß") || string.equals("İ")) {
                toAdd.add(string);
                continue;
            }
            if (exemplarType.convertUppercase) {
                string = UCharacter.toLowerCase(ULocale.ENGLISH, string);
            }
            toAdd.add(string);
            String composed = Normalizer.compose(string, false);
            if (!string.equals(composed)) {
                toAdd.add(composed);
            }
        }

        toAdd.removeAll(exemplarType.toRemove);
        
        if (DEBUG_DAIP && !toAdd.equals(exemplar)) {
            UnicodeSet oldOnly = new UnicodeSet(exemplar).removeAll(toAdd);
            UnicodeSet newOnly = new UnicodeSet(toAdd).removeAll(exemplar);
            System.out.println("Exemplar:\t" + exemplarType + ",\tremoved\t" + oldOnly + ",\tadded\t" + newOnly);
        }

        String fixedExemplar = prettyPrinter.format(toAdd);
        UnicodeSet doubleCheck = new UnicodeSet(fixedExemplar);
        if (!toAdd.equals(doubleCheck)) {
            // something went wrong, leave as is
        } else if (!value.equals(fixedExemplar)) { // put in this condition just for debugging
            if (DEBUG_DAIP) {
                System.out.println(TestMetadata.showDifference(
                        With.codePoints(value), 
                        With.codePoints(fixedExemplar),
                        "\n"));
            }
            value = fixedExemplar;
        }
        return value;
    }
    
}
