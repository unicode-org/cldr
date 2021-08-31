/*
 **********************************************************************
 * Copyright (c) 2005-2011, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: John Emmons
 **********************************************************************
 */
package org.unicode.cldr.posix;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.icu.SimpleConverter;
import org.unicode.cldr.util.CldrUtility;

import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

/**
 * Class to generate POSIX format charmap
 *
 * @author John C. Emmons
 */

public class GenerateCharmap {

    private static final int DESTDIR = 2,
        UNICODESET = 3,
        CHARSET = 4;

    private static final UOption[] options = {
        UOption.HELP_H(),
        UOption.HELP_QUESTION_MARK(),
        UOption.create("destdir", 'd', UOption.REQUIRES_ARG).setDefault("."),
        UOption.create("unicodeset", 'u', UOption.REQUIRES_ARG).setDefault("[\\u0000-\\U0010FFFF]"),
        UOption.create("charset", 'c', UOption.REQUIRES_ARG).setDefault("UTF-8"),
    };

    public static void main(String[] args) throws IOException {
        UOption.parseArgs(args, options);
        String codeset = options[CHARSET].value;
        GenerateCharmap gp = new GenerateCharmap(new UnicodeSet(options[UNICODESET].value),
            Charset.forName(codeset), codeset);
        PrintWriter out = FileUtilities.openUTF8Writer(options[DESTDIR].value + File.separator, codeset + ".cm");
        gp.write(out);
        out.close();
    }

    public class CharmapLine implements Comparable<Object> {
        public String CharacterValue;
        public String CharacterName;
        public String CharacterAltName;

        public CharmapLine(String Name, String AltName, String Value) {
            CharacterName = Name;
            CharacterAltName = AltName;
            CharacterValue = Value;
            if (Name.equals(AltName))
                CharacterAltName = "";
        }

        @Override
        public int compareTo(Object o) {
            CharmapLine c = (CharmapLine) o;
            return (CharacterValue.compareTo(c.CharacterValue));
        }
    }

    UnicodeSet chars;
    Charset cs;
    String codeset;

    public GenerateCharmap(UnicodeSet chars, Charset cs, String codeset) {
        this.cs = cs;
        if (cs != null && !cs.name().equals("UTF-8")) {
            UnicodeSet csset = new SimpleConverter(cs).getCharset();
            chars = new UnicodeSet(chars).retainAll(csset);
        }
        this.chars = chars;
        this.codeset = codeset;

    }

    public void write(PrintWriter out) {
        out.println("######################");
        out.println("# POSIX charmap ");
        out.println("# Generated automatically from the Unicode Character Database and Common Locale Data Repository");
        out.println("# see http://www.opengroup.org/onlinepubs/009695399/basedefs/xbd_chap07.html");
        out.println("# charset:\t" + codeset);
        out.println("######################");
        out.println("#################################################################################################");
        out.println(CldrUtility.getCopyrightString("# "));
        out.println();
        doCharmap(out, cs);
        out.println("######################");
        out.println();
    }

    /**
     * @param out
     */
    private void doCharmap(PrintWriter out, Charset cs) {

        // print character types, restricted to the charset
        int LongestCharNameLength = 0;
        int LongestCharValueLength = 0;
        UnicodeSet us = new UnicodeSet("[^[:Noncharacter_Code_Point:][:Cn:][:Cs:]]").retainAll(chars);
        List<CharmapLine> cml = new ArrayList<>();
        CharmapLine current;
        for (UnicodeSetIterator it = new UnicodeSetIterator(us); it.next();) {
            String Name = POSIXUtilities.POSIXCharFullName(it.getString());
            String AltName = POSIXUtilities.POSIXCharName(it.getString());
            String Value = getCodepointValue(it.getString(), cs);
            current = new CharmapLine(Name, AltName, Value);
            cml.add(current);
            if (current.CharacterName.length() > LongestCharNameLength)
                LongestCharNameLength = current.CharacterName.length();
            if (current.CharacterValue.length() > LongestCharValueLength)
                LongestCharValueLength = current.CharacterValue.length();
        }

        Collections.sort(cml);

        out.print("<code_set_name> \"");
        out.print(codeset);
        out.println("\"");
        out.println("<mb_cur_min>    1");
        out.print("<mb_cur_max>    ");
        out.print(LongestCharValueLength / 4);
        out.println();
        out.println();
        out.println("CHARMAP");

        for (ListIterator<CharmapLine> li = cml.listIterator(); li.hasNext();) {
            current = li.next();

            out.print(current.CharacterName);
            for (int i = LongestCharNameLength + 1; i > current.CharacterName.length(); i--)
                out.print(" ");
            out.println(current.CharacterValue);
            if (current.CharacterAltName.length() > 0) {
                out.print(current.CharacterAltName);
                for (int i = LongestCharNameLength + 1; i > current.CharacterAltName.length(); i--)
                    out.print(" ");
                out.println(current.CharacterValue);
            }
        }

        out.println();
        out.println("END CHARMAP");
        out.println();

    }

    private String getCodepointValue(String cp, Charset cs) {
        StringBuffer result = new StringBuffer();
        ByteBuffer bb = cs.encode(cp);
        int i;
        while (bb.hasRemaining()) {
            result.append("\\x");
            byte b = bb.get();
            if (b < 0)
                i = b + 256;
            else
                i = b;

            result.append(Utility.hex(i, 2));
        }
        return result.toString();
    }
}
