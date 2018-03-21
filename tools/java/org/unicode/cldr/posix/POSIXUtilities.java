/*
 **********************************************************************
 * Copyright (c) 2002-2013, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: John Emmons
 **********************************************************************
 */
package org.unicode.cldr.posix;

import java.text.StringCharacterIterator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.unicode.cldr.util.CLDRFile;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class POSIXUtilities {

    private static UnicodeSet repertoire = new UnicodeSet(0x0000, 0x10FFFF);
    private static CLDRFile char_fallbk;
    private static Map<Integer, String> controlCodeNames = new HashMap<Integer, String>();

    // Since UCharacter.getExtendedName() in ICU doesn't provide the names for control characters
    // we have to force the issue here. Required elements for the POSIX portable character set will be
    // used when necessary (in lower case). Otherwise, the name from the Unicode data file is used.
    private static void initControlCodeNames() {
        controlCodeNames.put(0x0000, "NULL");
        controlCodeNames.put(0x0001, "START_OF_HEADING");
        controlCodeNames.put(0x0002, "START_OF_TEXT");
        controlCodeNames.put(0x0003, "END_OF_TEXT");
        controlCodeNames.put(0x0004, "END_OF_TRANSMISSION");
        controlCodeNames.put(0x0005, "ENQUIRY");
        controlCodeNames.put(0x0006, "ACKNOWLEDGE");
        controlCodeNames.put(0x0007, "ALERT");
        controlCodeNames.put(0x0008, "BACKSPACE");
        controlCodeNames.put(0x0009, "tab"); // Required element for POSIX portable character set
        controlCodeNames.put(0x000A, "newline"); // Required element for POSIX portable character set
        controlCodeNames.put(0x000B, "vertical-tab"); // Required element for POSIX portable character set
        controlCodeNames.put(0x000C, "form-feed"); // Required element for POSIX portable character set
        controlCodeNames.put(0x000D, "carriage-return"); // Required element for POSIX portable character set
        controlCodeNames.put(0x000E, "SHIFT_OUT");
        controlCodeNames.put(0x000F, "SHIFT_IN");
        controlCodeNames.put(0x0010, "DATA_LINK_ESCAPE");
        controlCodeNames.put(0x0011, "DEVICE_CONTROL_ONE");
        controlCodeNames.put(0x0012, "DEVICE_CONTROL_TWO");
        controlCodeNames.put(0x0013, "DEVICE_CONTROL_THREE");
        controlCodeNames.put(0x0014, "DEVICE_CONTROL_FOUR");
        controlCodeNames.put(0x0015, "NEGATIVE_ACKNOWLEDGE");
        controlCodeNames.put(0x0016, "SYNCHRONOUS_IDLE");
        controlCodeNames.put(0x0017, "END_OF_TRANSMISSION_BLOCK");
        controlCodeNames.put(0x0018, "CANCEL");
        controlCodeNames.put(0x0019, "END_OF_MEDIUM");
        controlCodeNames.put(0x001A, "SUBSTITUTE");
        controlCodeNames.put(0x001B, "ESCAPE");
        controlCodeNames.put(0x001C, "INFORMATION_SEPARATOR_FOUR");
        controlCodeNames.put(0x001D, "INFORMATION_SEPARATOR_THREE");
        controlCodeNames.put(0x001E, "INFORMATION_SEPARATOR_TWO");
        controlCodeNames.put(0x001F, "INFORMATION_SEPARATOR_ONE");
        controlCodeNames.put(0x007F, "DELETE");
        controlCodeNames.put(0x0080, "CONTROL-0080");
        controlCodeNames.put(0x0081, "CONTROL-0081");
        controlCodeNames.put(0x0082, "BREAK_PERMITTED_HERE");
        controlCodeNames.put(0x0083, "NO_BREAK_HERE");
        controlCodeNames.put(0x0084, "CONTROL-0084");
        controlCodeNames.put(0x0085, "NEXT_LINE");
        controlCodeNames.put(0x0086, "START_OF_SELECTED_AREA");
        controlCodeNames.put(0x0087, "END_OF_SELECTED_AREA");
        controlCodeNames.put(0x0088, "CHARACTER_TABULATION_SET");
        controlCodeNames.put(0x0089, "CHARACTER_TABULATION_WITH_JUSTIFICATION");
        controlCodeNames.put(0x008A, "LINE_TABULATION_SET");
        controlCodeNames.put(0x008B, "PARTIAL_LINE_FORWARD");
        controlCodeNames.put(0x008C, "PARTIAL_LINE_BACKWARD");
        controlCodeNames.put(0x008D, "REVERSE_LINE_FEED");
        controlCodeNames.put(0x008E, "SINGLE_SHIFT_TWO");
        controlCodeNames.put(0x008F, "SINGLE_SHIFT_THREE");
        controlCodeNames.put(0x0090, "DEVICE_CONTROL_STRING");
        controlCodeNames.put(0x0091, "PRIVATE_USE_ONE");
        controlCodeNames.put(0x0092, "PRIVATE_USE_TWO");
        controlCodeNames.put(0x0093, "SET_TRANSMIT_STATE");
        controlCodeNames.put(0x0094, "CANCEL_CHARACTER");
        controlCodeNames.put(0x0095, "MESSAGE_WAITING");
        controlCodeNames.put(0x0096, "START_OF_GUARDED_AREA");
        controlCodeNames.put(0x0097, "END_OF_GUARDED_AREA");
        controlCodeNames.put(0x0098, "START_OF_STRING");
        controlCodeNames.put(0x0099, "CONTROL-0099");
        controlCodeNames.put(0x009A, "SINGLE_CHARACTER_INTRODUCER");
        controlCodeNames.put(0x009B, "CONTROL_SEQUENCE_INTRODUCER");
        controlCodeNames.put(0x009C, "STRING_TERMINATOR");
        controlCodeNames.put(0x009D, "OPERATING_SYSTEM_COMMAND");
        controlCodeNames.put(0x009E, "PRIVACY_MESSAGE");
        controlCodeNames.put(0x009F, "APPLICATION_PROGRAM_COMMAND");
    }

    public static void setRepertoire(UnicodeSet rep) {
        repertoire = rep;
    }

    public static void setCharFallback(CLDRFile fallbk) {
        char_fallbk = fallbk;
    }

    public static String POSIXContraction(String s) {
        int cp;
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(s, i);
            result.append(POSIXCharName(cp));
        }
        return result.toString().replaceAll("><", "-");
    }

    public static String POSIXCharName(String s) {
        int cp;
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(s, i);
            result.append(POSIXCharName(cp));
        }
        return result.toString();
    }

    public static String POSIXCharName(int cp) {

        StringBuffer result = new StringBuffer();
        result.append("<");
        if ((cp >= 0x0041 && cp <= 0x005A) ||
            (cp >= 0x0061 && cp <= 0x007A)) // Latin letters
            result.append((char) cp);
        else if (cp >= 0x0030 && cp <= 0x0039) // digits
        {
            String n = UCharacter.getExtendedName(cp);
            result.append(n.replaceAll(" ", "_").replaceAll("DIGIT_", "").toLowerCase());
        } else if ((cp >= 0x0000 && cp <= 0x001F) || (cp >= 0x007F && cp <= 0x009F)) { // Controls
            if (controlCodeNames.isEmpty()) {
                initControlCodeNames();
            }
            result.append(controlCodeNames.get(cp));
        } else if (cp == 0x0020)
            result.append("space"); // Required elements for POSIX portable character set
        else // everything else
        {
            String n = UCharacter.getExtendedName(cp);
            result.append(n.replaceAll(" ", "_").replaceAll("<", "").replaceAll(">", "").toUpperCase());
        }

        int i = result.indexOf("_(");
        if (i >= 0)
            result.setLength(i);

        result.append(">");

        if (!repertoire.contains(cp)) {
            System.out.println("WARNING: character " + result.toString() + " is not in the target codeset.");

            String substituteString = "";
            boolean SubFound = false;
            String SearchLocation = "//supplementalData/characters/character-fallback/character[@value=\""
                + UCharacter.toString(cp) + "\"]/substitute";

            for (Iterator<String> it = char_fallbk.iterator(SearchLocation, char_fallbk.getComparator()); it.hasNext()
                && !SubFound;) {
                String path = it.next();
                substituteString = char_fallbk.getStringValue(path);
                if (repertoire.containsAll(substituteString))
                    SubFound = true;
            }

            if (SubFound) {
                System.out.println("	Substituted: " + POSIXUtilities.POSIXCharName(substituteString));
                result = new StringBuffer(POSIXUtilities.POSIXCharName(substituteString));
            } else
                System.out.println("	No acceptable substitute found. The resulting locale source may not compile.");
        }

        return result.toString();
    }

    public static String POSIXCharFullName(String s) {
        int cp;
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(s, i);
            result.append(POSIXCharFullName(cp));
        }
        return result.toString();
    }

    public static String POSIXCharFullName(int cp) {
        StringBuffer result = new StringBuffer();
        result.append("<");
        String n = UCharacter.getExtendedName(cp);
        result.append(n.replaceAll(" ", "_").replaceAll("<", "").replaceAll(">", "").toUpperCase());

        int i = result.indexOf("_(");
        if (i >= 0)
            result.setLength(i);

        result.append(">");

        return result.toString();
    }

    // POSIXCharNameNP replaces all non-portable characters with their expanded POSIX character name.

    public static String POSIXCharNameNP(String s) {
        int cp;
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(s, i);
            if (cp <= 0x007F)
                result.append((char) cp);
            else
                result.append(POSIXCharName(cp));
        }
        return result.toString();
    }

    public static String POSIXDateTimeFormat(String s, boolean UseAltDigits, POSIXVariant variant) {

        // This is an array of the POSIX date / time field descriptors and their corresponding representations
        // in LDML. We use these to replace the LDML fields with POSIX field descriptors.

        String[][] FieldDescriptors = {
            { "/", "<SOLIDUS>", "<SOLIDUS>", "<SOLIDUS>" },
            { "DDD", "%j", "%j", "%j" },
            { "EEEE", "%A", "%A", "%A" },
            { "EEE", "%a", "%a", "%a" },
            { "G", "%N", "%N", "%N" },
            { "HH", "%H", "%OH", "%H" },
            { "H", "%H", "%OH", "%k" }, // solaris defines exact mapping for "H""
            { "KK", "%I", "%OI", "%I" },
            { "K", "%I", "%OI", "%l" },
            { "MMMM", "%B", "%B", "%B" },
            { "MMM", "%b", "%b", "%b" },
            { "MM", "%m", "%Om", "%m" },
            { "M", "%m", "%Om", "%m" },
            { "VVVV", "%Z", "%Z", "%Z" },
            { "V", "%Z", "%Z", "%Z" },
            { "a", "%p", "%p", "%p" },
            { "dd", "%d", "%Od", "%d" },
            { "d", "%e", "%Oe", "%e" },
            { "hh", "%I", "%OI", "%I" },
            { "h", "%I", "%OI", "%l" }, // solaris defines exact mapping for "h"
            { "kk", "%H", "%OH", "%H" },
            { "k", "%H", "%OH", "%k" },
            { "mm", "%M", "%OM", "%M" },
            { "m", "%M", "%OM", "%M" },
            { "vvvv", "%Z", "%Z", "%Z" },
            { "v", "%Z", "%Z", "%Z" },
            { "yyyy", "%Y", "%Oy", "%Y" },
            { "yy", "%y", "%Oy", "%y" },
            { "y", "%Y", "%Oy", "%Y" },
            { "zzzz", "%Z", "%Z", "%Z" },
            { "zzz", "%Z", "%Z", "%Z" },
            { "zz", "%Z", "%Z", "%Z" },
            { "z", "%Z", "%Z", "%Z" },
            { "ss", "%S", "%OS", "%S" },
            { "s", "%S", "%OS", "%S" }
        };

        boolean inquotes = false;
        StringBuffer result = new StringBuffer("");

        for (int pos = 0; pos < s.length();) {
            boolean replaced = false;
            for (int i = 0; i < FieldDescriptors.length && !replaced && !inquotes; i++) {
                if (s.indexOf(FieldDescriptors[i][0], pos) == pos) {
                    if (UseAltDigits)
                        result.append(FieldDescriptors[i][2]);
                    else if (variant.platform.equals(POSIXVariant.SOLARIS))
                        result.append(FieldDescriptors[i][3]);
                    else
                        result.append(FieldDescriptors[i][1]);
                    replaced = true;
                    pos += FieldDescriptors[i][0].length();
                }
            }

            if (!replaced) {
                if (s.charAt(pos) == '\'') {
                    if (pos < (s.length() - 1) && s.charAt(pos + 1) == '\'') {
                        result.append('\'');
                        pos++;
                    } else
                        inquotes = !inquotes;
                } else
                    result.append(s.charAt(pos));
                pos++;
            }
        }
        return result.toString();

    }

    public static String POSIXGrouping(String grouping_pattern) {

        // Parse the decimal pattern to get the number of digits to use in the POSIX style pattern.

        int i = grouping_pattern.indexOf(".");
        int j;
        boolean first_grouping = true;
        String result;

        if (i < 0)
            result = "-1";
        else {
            result = new String();
            while ((j = grouping_pattern.lastIndexOf(",", i - 1)) > 0) {
                if (!first_grouping)
                    result = result.concat(";");
                Integer num_digits = new Integer(i - j - 1);
                result = result.concat(num_digits.toString());

                first_grouping = false;
                i = j;
            }
        }

        if (result.length() == 0)
            result = "-1";

        return result;

    }

    public static boolean isBetween(int a, int b, int c) {
        return ((a < b && b < c) || (c < b && b < a));
    }

    public static String POSIXYesNoExpr(String s) {
        StringBuffer result = new StringBuffer();
        String[] YesNoElements;
        YesNoElements = s.split(":");
        for (int i = 0; i < YesNoElements.length; i++) {
            String cur = YesNoElements[i];
            if (cur.length() >= 1 && cur.toLowerCase().equals(cur)) {
                if (result.length() > 0)
                    result.append(")|(");
                else
                    result.append("^((");

                StringCharacterIterator si = new StringCharacterIterator(cur);
                boolean OptLastChars = false;
                for (char c = si.first(); c != StringCharacterIterator.DONE; c = si.next()) {
                    if (c != Character.toUpperCase(c)) {
                        if (si.getIndex() == 1) {
                            result.append("(");
                            OptLastChars = true;
                        }
                        result.append("[");
                        result.append(c);
                        result.append(Character.toUpperCase(c));
                        result.append("]");
                    } else
                        result.append(c);
                }
                if (OptLastChars)
                    result.append(")?");
            }
        }
        result.append("))");
        return (POSIXCharNameNP(result.toString()));
    }
}
