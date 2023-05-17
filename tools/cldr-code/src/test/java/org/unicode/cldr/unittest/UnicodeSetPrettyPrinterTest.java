/*
 **********************************************************************
 * Copyright (c) 2009-2012, Google, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 */
package org.unicode.cldr.unittest;

import com.google.common.base.Joiner;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.ExemplarType;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.CodePointEscaper;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.SimpleUnicodeSetFormatter;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.UnicodeSetPrettyPrinter;
import org.unicode.cldr.util.XPathParts;

public class UnicodeSetPrettyPrinterTest extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new UnicodeSetPrettyPrinterTest().run(args);
    }

    public static final UnicodeSet TO_QUOTE =
            new UnicodeSet("[[:z:][:me:][:mn:][:di:][:c:]-[\u0020]]");

    final UnicodeSetPrettyPrinter PRETTY_PRINTER =
            UnicodeSetPrettyPrinter.fromIcuLocale(ULocale.ENGLISH.toString()).setToQuote(TO_QUOTE);

    public void TestBasicUnicodeSet() {
        UnicodeSet expected = new UnicodeSet("[:L:]");
        String formatted = PRETTY_PRINTER.format(expected);
        logln(formatted);
        UnicodeSet actual = new UnicodeSet(formatted);
        assertEquals("PrettyPrinter preserves meaning", expected, actual);
    }

    public void testSimpleUnicodeSetFormatter() {
        String[][] unicodeToDisplay = {
            {"[\u000F]", "❰F❱"},
            {"[\\u0024\\uFE69\\uFF04]", "$ ＄ ﹩"},
            {"[\\u0024﹩＄]", "$ ＄ ﹩"},
            {"[\\u0020]", "❰SP❱"},
            {
                "[\\u0020-\\u0023 \\u00AB-\\u00AD \\u0081-\\u0083]",
                "❰81❱ ❰82❱ ❰83❱ ❰SHY❱ ❰SP❱ ! \" « # ¬"
                // Note: don't currently form ranges with escaped characters in display
                // But they they parse (see below)
            },
            {"[A-Z]", "A B C D E F G H I J K L M N O P Q R S T U V W X Y Z"},
            {
                "[A Á B C {CS} D {DZ} {DZS} E É F G {GY} H I Í J K L {LY} M N {NY} O Ó Ö Ő P Q R S {SZ} T {TY} U Ú Ü Ű V W X Y Z {ZS}]",
                "A Á B C CS D DZ DZS E É F G GY H I Í J K L LY M N NY O Ó Ö Ő P Q R S SZ T TY U Ú Ü Ű V W X Y Z ZS"
            },
            {"[:block=Hangul_Jamo:]", "ᄀ➖ᇿ"},
            {"[:block=CJK_Unified_Ideographs:]", "一➖鿿"},
            {"LOCALE", "no"},
            {"[ĂÅ z]", "Ă z Å"}, // Ensure that order is according to the locale
            {
                "[ÅÅ]", "Å Å"
            }, // Ensure it doesn't merge two different characters with same NFC, even though a
            // collator is used
            {"[\\u001E-!]", "❰1E❱ ❰1F❱ ❰SP❱ !"},
            {"[a\\u0020]", "❰SP❱ a"},
            {"[abcq]", "a b c q"},
            {"[ab{cq}]", "a b cq"},
            {
                "[{2️⃣} 🪷-🪺 🫃{🫃🏻}{🇿🇼} {🏴\\U000E0067\\U000E0062\\U000E0065\\U000E006E\\U000E0067\\U000E007F}]",
                "🇿🇼 🏴󠁧󠁢󠁥󠁮󠁧󠁿 🪷 🪸 🪹 🪺 🫃 🫃🏻 2️⃣"
            },
            // TODO, handle {🐈‍⬛} . Not necessary at this point, because emoji don't occur in our
            // UnicodeSets
            {"[{\\u0020\u0FFF}]", "❰SP❱❰FFF❱"},
            {"[{a\\u0020b\\u0FFFc}]", "a❰SP❱b❰FFF❱c"},
        };

        SimpleUnicodeSetFormatter susf = new SimpleUnicodeSetFormatter();

        int count = 0;
        for (String[] test : unicodeToDisplay) {
            if ("LOCALE".equals(test[0])) {
                susf = SimpleUnicodeSetFormatter.fromIcuLocale(test[1]);
                continue;
            }
            final UnicodeSet source = new UnicodeSet(test[0]);
            String actual = susf.format(source);
            String expected = test.length < 2 ? actual : test[1];
            assertEquals(++count + ") " + source + " to format", expected, actual);

            UnicodeSet expectedRoundtrip = null;
            try {
                expectedRoundtrip = susf.parse(expected);
            } catch (Exception e) {
            }
            assertEquals(count + ") " + source + " roundtrip", expectedRoundtrip, source);
        }

        String[][] displayToUnicode = {
            {"❰81❱➖❰83❱ «➖❰SHY❱ ❰SP❱➖#", "[\\u0020-\\u0023 \\u00AB-\\u00AD \\u0081-\\u0083]"},
            {"«➖❰SHY❱", "[\\u00AB-\\u00AD]"},
            {"❰81❱➖❰83❱", "[\\u0081-\\u0083]"},
            {"❰SP❱➖#", "[\\ -#]"},
        };

        for (String[] test : displayToUnicode) {
            final String display = test[0];
            final UnicodeSet expectedUnicodeSet = new UnicodeSet(test[1]);
            UnicodeSet actualUnicodeSet = null;
            try {
                actualUnicodeSet = susf.parse(display);
            } catch (Exception e) {
            }
            assertEquals(display, expectedUnicodeSet, actualUnicodeSet);
        }
    }

    public void testSimpleUnicodesetSyntaxErrors() {
        String[][] tests = {
            {"➖", "Must have exactly one character before '➖': ❌➖"},
            {"0➖", "Must have exactly one character after '➖': 0➖❌"},
            {"➖9", "Must have exactly one character before '➖': ❌➖9"},
            {"10➖9", "Must have exactly one character before '➖': 10❌➖9"},
            {"❰SP", "Missing end escape ❱: ❰SP❌"},
            {"❰", "Missing end escape ❱: ❰❌"},
            {"❰110000❱", "Illegal code point: ❰110000❌❱"},
            {"SP❱", "Missing start escape ❰: SP❌❱"},
            {"❱", "Missing start escape ❰: ❌❱"},
        };
        SimpleUnicodeSetFormatter susf =
                new SimpleUnicodeSetFormatter(SimpleUnicodeSetFormatter.BASIC_COLLATOR);
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
        // TODO also allow hex in strings check(susf, "", ExemplarType.main, new
        // UnicodeSet("[{\\u200D\\u200e}]"), false);
    }

    public void testSimpleUnicodeSetFormatterWithLocales() {
        havePrintln = false;
        StringBuilder needsEscapeReport = new StringBuilder();
        final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
        Factory cldrFactory = CLDR_CONFIG.getCldrFactory();
        SimpleUnicodeSetFormatter susf =
                new SimpleUnicodeSetFormatter(SimpleUnicodeSetFormatter.BASIC_COLLATOR, null);

        UnicodeSet needsEscape = new UnicodeSet();
        UnicodeSet localeNeedsEscape = new UnicodeSet();
        boolean longTest = getInclusion() > 5;
        Set<String> testLocales =
                longTest
                        ? cldrFactory.getAvailableLanguages()
                        : StandardCodes.make()
                                .getLocaleCoverageLocales(
                                        Organization.cldr, Collections.singleton(Level.MODERN));

        for (String locale : testLocales) {
            localeNeedsEscape.clear();
            CLDRFile cldrFile = cldrFactory.make(locale, true);
            boolean showAnyway = matchLocale == null ? false : matchLocale.reset(locale).matches();
            for (ExemplarType type : ExemplarType.values()) {
                UnicodeSet source = cldrFile.getExemplarSet(type, WinningChoice.WINNING);
                // source = SimpleUnicodeSetFormatter.transform(source, x ->
                // SimpleUnicodeSetFormatter.nfc.normalize(x)); //current CLDR might not be
                // normalized
                check(susf, locale, type.toString(), source, showAnyway);
                localeNeedsEscape.addAll(source);
            }
            CLDRFile cldrFile2 = cldrFactory.make(locale, false); // just existing paths
            for (String path : cldrFile2) {
                String value = cldrFile2.getStringValue(path);
                if (value.equals("↑↑↑")) {
                    continue;
                }
                if (path.contains("/parseLenient")) {
                    String label =
                            Joiner.on('/')
                                    .join(
                                            XPathParts.getFrozenInstance(path)
                                                    .getAttributes(-1)
                                                    .values());
                    final UnicodeSet source = new UnicodeSet(value);
                    check(susf, locale, label, source, showAnyway);
                    localeNeedsEscape.addAll(source);
                }
                if (CodePointEscaper.FORCE_ESCAPE.containsSome(value)) {
                    localeNeedsEscape.addAll(value); // add more than we need
                }
            }
            localeNeedsEscape.retainAll(CodePointEscaper.FORCE_ESCAPE);
            if (showAnyway) {
                needsEscapeReport.append(
                        "*\t"
                                + locale
                                + "\tNeeds Escape:\t"
                                + susf.format(localeNeedsEscape)
                                + "\n");
            }
            needsEscape.addAll(localeNeedsEscape);
        }
        if (isVerbose()) {
            if (needsEscapeReport.length() != 0) {
                System.out.print(needsEscapeReport);
            }
            System.out.println("*\tALL\tNeeds Escape:\t" + susf.format(needsEscape));
            System.out.println("*\tALL\tNamed Escapes:\t" + CodePointEscaper.getNamedEscapes());
        }
        final UnicodeSet missing =
                new UnicodeSet(needsEscape).removeAll(CodePointEscaper.getNamedEscapes());
        assertEquals("*\tMissing\tNamed Escapes:\t", "", susf.format(missing));
    }

    boolean havePrintln = false;
    Set<String> seenAlready = new HashSet<>();

    void check(
            SimpleUnicodeSetFormatter susf,
            String locale,
            String typeString,
            UnicodeSet source,
            boolean showAnyway) {
        String formatted = susf.format(source);
        String key = formatted + source.toPattern(false);
        if (seenAlready.contains(key)) {
            return;
        }
        seenAlready.add(key);

        UnicodeSet roundtrip = susf.parse(formatted);
        final boolean isOk = assertEquals(locale + ", " + typeString, source, roundtrip);
        if (showAnyway || !isOk) {
            UnicodeSet roundtrip_source = new UnicodeSet(roundtrip).removeAll(source);
            UnicodeSet source_roundtrip = new UnicodeSet(source).removeAll(roundtrip);
            if (isVerbose()) {
                if (!havePrintln) {
                    System.out.println();
                    havePrintln = true;
                }
                System.out.println(
                        locale
                                + "\t"
                                + typeString
                                + "\tsource:  \t"
                                + PRETTY_PRINTER.format(source));
                System.out.println(locale + "\t" + typeString + "\tformatted:\t" + formatted);
                if (!roundtrip_source.isEmpty()) {
                    System.out.println(
                            locale
                                    + "\t"
                                    + typeString
                                    + "\tFAIL, roundtrip-source:  \t"
                                    + showInvisible(
                                            roundtrip_source, CodePointEscaper.FORCE_ESCAPE));
                }
                if (!source_roundtrip.isEmpty()) {
                    System.out.println(
                            locale
                                    + "\t"
                                    + typeString
                                    + "\tFAIL, source_roundtrip:  \t"
                                    + showInvisible(
                                            source_roundtrip, CodePointEscaper.FORCE_ESCAPE));
                }

                if (!isOk) {
                    // for debugging
                    String formattedDebug = susf.format(source);
                    UnicodeSet roundtripDebug = susf.parse(formatted);
                }
            }
        }
    }

    public static String showInvisible(UnicodeSet input, UnicodeSet forceHex) {
        return SimpleUnicodeSetFormatter.appendWithHex(
                        new StringBuilder(), input.toPattern(false), forceHex)
                .toString();
    }

    public void TestCodePointEscaper() {
        ArrayList<String> collection = new ArrayList<>();
        CodePointEscaper.getNamedEscapes().addAllTo(collection);
        collection.add("\u0000");
        collection.add("\u00AD");
        collection.add("\uFEFF");
        collection.add("\uFFFF");
        collection.add(new StringBuilder().appendCodePoint(0x10FFFF).toString());
        for (String item : collection) {
            final int cp = item.codePointAt(0);
            String display = CodePointEscaper.codePointToEscaped(cp);
            int roundtrip = CodePointEscaper.escapedToCodePoint(display);
            assertEquals(
                    "\tU+"
                            + Utility.hex(cp, 4)
                            + " "
                            + UCharacter.getExtendedName(cp)
                            + " ⇒ "
                            + display
                            + "\t",
                    cp,
                    roundtrip);
        }
        if (isVerbose()) {
            System.out.println("Abbr.\tCode Point\tName");
            for (CodePointEscaper item : CodePointEscaper.values()) {
                System.out.println(
                        item.codePointToEscaped()
                                + "\tU+"
                                + Utility.hex(item.getCodePoint(), 4)
                                + "\t"
                                + item.getShortName());
            }
            System.out.println(
                    CodePointEscaper.ESCAPE_START
                            + "…"
                            + CodePointEscaper.ESCAPE_END
                            + "\tU+…\tOther; … = hex notation");
        } else {
            warnln("Use -v to see list of escapes");
        }
    }

    public void TestStringEscaper() {
        String[][] tests = {
            {"xyz", "xyz"},
            {null, "❰WNJ❱xyz❰47❱", "\u200BxyzG"},
            {"\u200Bxyz\u200B", "❰WNJ❱xyz❰WNJ❱"},
            {"A\u200B\u00ADB", "A❰WNJ❱❰SHY❱B"},
        };
        for (String[] test : tests) {
            String source = test[0];
            String expected = test[1];
            String expectedRoundtrip = test.length < 3 ? test[0] : test[2];
            if (source != null) {
                String actual = CodePointEscaper.toEscaped(source);
                assertEquals(source, expected, actual);
            }
            String actualRoundtrip = CodePointEscaper.toUnescaped(expected);
            assertEquals(expected, expectedRoundtrip, actualRoundtrip);
        }
    }
}
