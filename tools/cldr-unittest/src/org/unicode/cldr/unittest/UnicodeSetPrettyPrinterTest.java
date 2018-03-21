/*
 **********************************************************************
 * Copyright (c) 2009-2012, Google, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 */
package org.unicode.cldr.unittest;

import java.text.Collator;
import java.util.Locale;

import org.unicode.cldr.util.UnicodeSetPrettyPrinter;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.UnicodeSet;

public class UnicodeSetPrettyPrinterTest extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new UnicodeSetPrettyPrinterTest().run(args);
    }

    public static final UnicodeSet TO_QUOTE = new UnicodeSet("[[:z:][:me:][:mn:][:di:][:c:]-[\u0020]]");

    public void TestBasicUnicodeSet() {

        Collator spaceComp = Collator.getInstance(Locale.ENGLISH);
        spaceComp.setStrength(Collator.PRIMARY);

        final UnicodeSetPrettyPrinter PRETTY_PRINTER = new UnicodeSetPrettyPrinter()
            .setOrdering(Collator.getInstance(Locale.ENGLISH))
            .setSpaceComparator(spaceComp)
            .setToQuote(TO_QUOTE);

        UnicodeSet expected = new UnicodeSet("[:L:]");
        String formatted = PRETTY_PRINTER.format(expected);
        logln(formatted);
        UnicodeSet actual = new UnicodeSet(formatted);
        assertEquals("PrettyPrinter preserves meaning", expected, actual);
    }
}
