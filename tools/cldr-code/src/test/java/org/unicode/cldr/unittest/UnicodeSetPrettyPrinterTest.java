/*
 **********************************************************************
 * Copyright (c) 2009-2012, Google, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 */
package org.unicode.cldr.unittest;

import java.text.Collator;
import java.util.Locale;
import java.util.Set;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.ExemplarType;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SimpleUnicodeSetFormatter;
import org.unicode.cldr.util.SimpleUnicodeSetFormatter.CodePointEscaper;
import org.unicode.cldr.util.UnicodeSetPrettyPrinter;

import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.UnicodeSet;

public class UnicodeSetPrettyPrinterTest extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new UnicodeSetPrettyPrinterTest().run(args);
    }

    public static final UnicodeSet TO_QUOTE =
            new UnicodeSet("[[:z:][:me:][:mn:][:di:][:c:]-[\u0020]]");

    public void TestBasicUnicodeSet() {

        Collator spaceComp = Collator.getInstance(Locale.ENGLISH);
        spaceComp.setStrength(Collator.PRIMARY);

        final UnicodeSetPrettyPrinter PRETTY_PRINTER =
                new UnicodeSetPrettyPrinter()
                        .setOrdering(Collator.getInstance(Locale.ENGLISH))
                        .setSpaceComparator(spaceComp)
                        .setToQuote(TO_QUOTE);

        UnicodeSet expected = new UnicodeSet("[:L:]");
        String formatted = PRETTY_PRINTER.format(expected);
        logln(formatted);
        UnicodeSet actual = new UnicodeSet(formatted);
        assertEquals("PrettyPrinter preserves meaning", expected, actual);
    }

    boolean showAnyway = false;

    public void testCodePointEscaper() {
        String [][] tests = {
            {" ", "SP"},
            {"\u000F", "F"},
        };
        for (String[] test : tests) {
            final int source = test[0].codePointAt(0);
            String actual = CodePointEscaper.fromCodePoint(source);
            int expectedRoundtrip = CodePointEscaper.toCodePoint(actual);
            String expected = test.length<2 ? actual : test[1];
            assertEquals("", expected, actual);
            assertEquals("", expectedRoundtrip, source);
        }
    }

    public void testSimpleUnicodeSetFormatter() {
        String [][] tests = {
            {"[\\u0020]", "❰SP❱"},
            {"[\u000F]", "❰F❱"},
       };
        SimpleUnicodeSetFormatter susf = new SimpleUnicodeSetFormatter(SimpleUnicodeSetFormatter.BASIC_COLLATOR, null);

        for (String[] test : tests) {
            final UnicodeSet source = new UnicodeSet(test[0]);
            String actual = susf.format(source);
            UnicodeSet expectedRoundtrip = null;
            try {
                expectedRoundtrip = susf.parse(actual);
            } catch (Exception e) {}
            String expected = test.length<2 ? actual : test[1];
            assertEquals(source + " to format", expected, actual);
            assertEquals(source + " roundtrip", expectedRoundtrip, source);
        }
    }

    Set<String> SHOW_LOCALES = ImmutableSet.of("hu", "ar");

    public void TestSimpleUnicodeSetFormatterWithLocales() {
        final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
        Factory cldrFactory = CLDR_CONFIG.getCldrFactory();
        SimpleUnicodeSetFormatter susf = new SimpleUnicodeSetFormatter(SimpleUnicodeSetFormatter.BASIC_COLLATOR, null);

        check(susf, "", ExemplarType.main, new UnicodeSet("[a-b]"), false);
        check(susf, "", ExemplarType.main, new UnicodeSet("[-a-ceg-h{ef}]"), false);
        check(susf, "", ExemplarType.main, new UnicodeSet("[\\u200D]"), false);
        check(susf, "", ExemplarType.main, new UnicodeSet("[\\u200D-\\u200f]"), false);
        check(susf, "", ExemplarType.main, new UnicodeSet("[\\u200D\\u200e]"), false);
        // TODO also allow hex in strings check(susf, "", ExemplarType.main, new UnicodeSet("[{\\u200D\\u200e}]"), false);

        for (String locale : cldrFactory.getAvailableLanguages()) {
            CLDRFile cldrFile = cldrFactory.make(locale, true);
            for (ExemplarType type : ExemplarType.values()) {
                UnicodeSet expected = cldrFile.getExemplarSet(type, WinningChoice.WINNING);
                expected = SimpleUnicodeSetFormatter.transform(expected, x -> SimpleUnicodeSetFormatter.nfc.normalize(x)); //current CLDR might not be normalized
                check(susf, locale, type, expected, showAnyway || SHOW_LOCALES.contains(locale));
            }
        }
    }

    void check(SimpleUnicodeSetFormatter susf, String locale, ExemplarType type, UnicodeSet source, boolean showAnyway) {
        String formatted = susf.format(source);
        UnicodeSet roundtrip = susf.parse(formatted);
        final boolean isOk = assertEquals(locale + ", " + type, source, roundtrip);
        if (showAnyway || !isOk) {
            UnicodeSet roundtrip_source = new UnicodeSet(roundtrip).removeAll(source);
            UnicodeSet source_roundtrip = new UnicodeSet(source).removeAll(roundtrip);
            System.out.println(locale + "\t" + type + "\tsource:  \t" + showInvisible(source, SimpleUnicodeSetFormatter.FORCE_HEX));
            System.out.println(locale + "\t" + type + "\tformatted:\t" + formatted);
            if (!roundtrip_source.isEmpty()) {
                System.out.println(locale + "\t" + type + "\tFAIL, roundtrip-source:  \t" + showInvisible(roundtrip_source, SimpleUnicodeSetFormatter.FORCE_HEX));
            }
            if (!source_roundtrip.isEmpty()) {
                System.out.println(locale + "\t" + type + "\tFAIL, source_roundtrip:  \t" + showInvisible(source_roundtrip, SimpleUnicodeSetFormatter.FORCE_HEX));
            }

            if (!isOk) {
                // for debugging
                String formattedDebug = susf.format(source);
                UnicodeSet roundtripDebug = susf.parse(formatted);
            }
        }
    }

    public static String showInvisible(UnicodeSet input, UnicodeSet forceHex) {
        return SimpleUnicodeSetFormatter.appendWithHex(new StringBuilder(), input.toPattern(false), forceHex).toString();
    }
}
