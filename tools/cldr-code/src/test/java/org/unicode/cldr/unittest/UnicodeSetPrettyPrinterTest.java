/*
 **********************************************************************
 * Copyright (c) 2009-2012, Google, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 */
package org.unicode.cldr.unittest;

import java.text.Collator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.ExemplarType;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.CodePointEscaper;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SimpleUnicodeSetFormatter;
import org.unicode.cldr.util.UnicodeSetPrettyPrinter;
import org.unicode.cldr.util.XPathParts;

import com.google.common.base.Joiner;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.UnicodeSet;

public class UnicodeSetPrettyPrinterTest extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new UnicodeSetPrettyPrinterTest().run(args);
    }

    public static final UnicodeSet TO_QUOTE =
            new UnicodeSet("[[:z:][:me:][:mn:][:di:][:c:]-[\u0020]]");

    private final Collator ROOT_COLLATOR = Collator.getInstance(Locale.ENGLISH);

    Collator spaceComp = Collator.getInstance(Locale.ENGLISH);
    {
        spaceComp.setStrength(Collator.PRIMARY);
    }

    final UnicodeSetPrettyPrinter PRETTY_PRINTER = new UnicodeSetPrettyPrinter()
        .setOrdering(ROOT_COLLATOR)
        .setSpaceComparator(spaceComp)
        .setToQuote(TO_QUOTE);


    public void TestBasicUnicodeSet() {
        UnicodeSet expected = new UnicodeSet("[:L:]");
        String formatted = PRETTY_PRINTER.format(expected);
        logln(formatted);
        UnicodeSet actual = new UnicodeSet(formatted);
        assertEquals("PrettyPrinter preserves meaning", expected, actual);
    }

    public void testCodePointEscaper() {
        String [][] tests = {
            {"\u000F", "F"},
            {" ", "SP"},
        };
        for (String[] test : tests) {
            final int source = test[0].codePointAt(0);
            String actual = CodePointEscaper.toAbbreviationOrHex(source);
            int expectedRoundtrip = CodePointEscaper.fromAbbreviationOrHex(actual);
            String expected = test.length<2 ? actual : test[1];
            assertEquals("", expected, actual);
            assertEquals("", expectedRoundtrip, source);
        }
    }

    public void testSimpleUnicodeSetFormatter() {
        String [][] tests = {
            {"[\u000F]", "⦕F⦖"},
            {"[\\u0020]", "⦕SP⦖"},
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

    public void testSimpleUnicodesetSyntax() {
        String [][] tests = {
            {"➖", "A range mark must have characters on both sides: ➖"},
            {"0➖", "A range mark must have characters on both sides: 0➖"},
            {"➖9", "A range mark must have characters on both sides: ➖9"},
            {"10➖9", "A range mark must have single code points on both sides: 10➖9"},
            {"⦕SP", "Escape start ⦕ without escape end ⦖: ⦕SP"},
            {"⦕", "Escape start ⦕ without escape end ⦖: ⦕"},
            {"⦕110000⦖", "Code point out of bounds: 110000"},
            {"SP⦖", "Escape end ⦖ without escape start ⦕: SP⦖"},
            {"⦖", "Escape end ⦖ without escape start ⦕: ⦖"},
        };
        SimpleUnicodeSetFormatter susf = new SimpleUnicodeSetFormatter(SimpleUnicodeSetFormatter.BASIC_COLLATOR);
        for (String[] test : tests) {
            String actual = null;
            try {
                UnicodeSet uset = susf.parse(test[0]);
                actual = uset.toPattern(false);
            } catch (Exception e) {
                actual = e.getMessage();
            }
            assertEquals(test[0], test[1], actual);
        }

    }
    final Matcher matchLocale; // fine-grained control for verbose
    // use -DUnicodeSetPrettyPrinterTest:showAnyway=.* for all
    {
        String matchString = System.getProperty("UnicodeSetPrettyPrinterTest:showAnyway");
        if (matchString == null) {
            matchLocale = null;
        } else {
            matchLocale = Pattern.compile(matchString).matcher("");
        }
    }

    public void testSimpleUnicodeSetFormatter2() {
        SimpleUnicodeSetFormatter susf = new SimpleUnicodeSetFormatter();

        check(susf, "", "constant", new UnicodeSet("[a-b]"), false);
        check(susf, "", "constant", new UnicodeSet("[-a-ceg-h{ef}]"), false);
        check(susf, "", "constant", new UnicodeSet("[\\u200D]"), false);
        check(susf, "", "constant", new UnicodeSet("[\\u200D-\\u200f]"), false);
        check(susf, "", "constant", new UnicodeSet("[\\u200D\\u200e]"), false);
        check(susf, "", "constant", new UnicodeSet("[\\--/]"), false);
        // TODO also allow hex in strings check(susf, "", ExemplarType.main, new UnicodeSet("[{\\u200D\\u200e}]"), false);
    }

    public void testSimpleUnicodeSetFormatterWithLocales() {
        havePrintln = false;
        StringBuilder needsEscapeReport = new StringBuilder();
        final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
        Factory cldrFactory = CLDR_CONFIG.getCldrFactory();
        SimpleUnicodeSetFormatter susf = new SimpleUnicodeSetFormatter(SimpleUnicodeSetFormatter.BASIC_COLLATOR, null);

        UnicodeSet needsEscape = new UnicodeSet();
        UnicodeSet localeNeedsEscape = new UnicodeSet();

        for (String locale : cldrFactory.getAvailableLanguages()) {
            localeNeedsEscape.clear();
            CLDRFile cldrFile = cldrFactory.make(locale, true);
            boolean showAnyway = matchLocale == null ? false : matchLocale.reset(locale).matches();
            for (ExemplarType type : ExemplarType.values()) {
                UnicodeSet source = cldrFile.getExemplarSet(type, WinningChoice.WINNING);
                // source = SimpleUnicodeSetFormatter.transform(source, x -> SimpleUnicodeSetFormatter.nfc.normalize(x)); //current CLDR might not be normalized
                check(susf, locale, type.toString(), source, showAnyway);
            }
            CLDRFile cldrFile2 = cldrFactory.make(locale, false); // just existing paths
            for (String path : cldrFile2) {
                String value = cldrFile2.getStringValue(path);
                if (value.equals("↑↑↑")) {
                    continue;
                }
                if (path.contains("/parseLenient")) {
                    String label = Joiner.on('/').join(XPathParts.getFrozenInstance(path).getAttributes(-1).values());
                    check(susf, locale, label, new UnicodeSet(value), showAnyway);
                }
                if (CodePointEscaper.FORCE_ESCAPE.containsSome(value)) {
                    localeNeedsEscape.addAll(value); // add more than we need
                }
            }
            localeNeedsEscape.retainAll(CodePointEscaper.FORCE_ESCAPE);
            if (showAnyway) {
                needsEscapeReport.append("*\t" + locale + "\tNeeds Escape:\t" + needsEscape + "\n");
            }
            needsEscape.addAll(localeNeedsEscape);
        }
        if (needsEscapeReport.length() != 0) {
            System.out.print(needsEscapeReport);
        }
        System.out.println("*\tALL\tNeeds Escape:\t" + needsEscape);
        System.out.println("*\tALL\tNamed Escapes:\t" + CodePointEscaper.getNamedEscapes());
    }

    boolean havePrintln = false;
    Set<String> seenAlready = new HashSet<>();

    void check(SimpleUnicodeSetFormatter susf, String locale, String typeString, UnicodeSet source, boolean showAnyway) {
        String formatted = susf.format(source);
        String key = formatted+source.toPattern(false);
        if (seenAlready.contains(key)) {
            return;
        }
        seenAlready.add(key);

        UnicodeSet roundtrip = susf.parse(formatted);
        final boolean isOk = assertEquals(locale + ", " + typeString, source, roundtrip);
        if (showAnyway || !isOk) {
            UnicodeSet roundtrip_source = new UnicodeSet(roundtrip).removeAll(source);
            UnicodeSet source_roundtrip = new UnicodeSet(source).removeAll(roundtrip);
            if (!havePrintln) {
                System.out.println();
                havePrintln = true;
            }
            System.out.println(locale + "\t" + typeString + "\tsource:  \t" + PRETTY_PRINTER.format(source));
            System.out.println(locale + "\t" + typeString + "\tformatted:\t" + formatted);
            if (!roundtrip_source.isEmpty()) {
                System.out.println(locale + "\t" + typeString + "\tFAIL, roundtrip-source:  \t" + showInvisible(roundtrip_source, CodePointEscaper.FORCE_ESCAPE));
            }
            if (!source_roundtrip.isEmpty()) {
                System.out.println(locale + "\t" + typeString + "\tFAIL, source_roundtrip:  \t" + showInvisible(source_roundtrip, CodePointEscaper.FORCE_ESCAPE));
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
