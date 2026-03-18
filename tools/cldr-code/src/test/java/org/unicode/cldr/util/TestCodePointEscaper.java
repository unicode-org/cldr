package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibm.icu.text.UnicodeSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.testutil.TestWithKnownIssues;

/**
 * @see org.unicode.cldr.unittest.UnicodeSetPrettyPrinterTest
 */
public class TestCodePointEscaper extends TestWithKnownIssues {
    @Test
    void testForEach() {
        for (final CodePointEscaper e : CodePointEscaper.values()) {
            assertEquals(e, CodePointEscaper.forCodePoint(e.getString()));
            assertTrue(
                    CodePointEscaper.FORCE_ESCAPE.contains(e.getCodePoint()),
                    () -> "For " + e.name());
        }
    }

    @Test
    void testRegex() {
        // The pre/post fixes vary by regex engine. For Java it is these.
        final String forceEscapeRegex =
                CodePointEscaper.regexPattern(CodePointEscaper.ESCAPE_IN_SURVEYTOOL, "\\x{", "}");

        final Matcher matcher = Pattern.compile(forceEscapeRegex).matcher("");

        int shouldFail = 'A';
        assertFalse(
                matcher.reset(stringToScan(shouldFail)).find(),
                () -> "For " + CodePointEscaper.hexEscape(shouldFail));

        int shouldSucceed = 0x202F;
        assertTrue(
                matcher.reset(stringToScan(shouldSucceed)).find(),
                () -> "For " + CodePointEscaper.hexEscape(shouldSucceed));

        // NOTE: A UnicodeSet pattern may look like regex, but the syntax is not the same.
        // So we can't use UnicodeSet.toPattern()

        for (final int e : CodePointEscaper.ESCAPE_IN_SURVEYTOOL.codePoints()) {
            assertTrue(
                    matcher.reset(stringToScan(e)).find(),
                    () -> "For " + CodePointEscaper.hexEscape(e));
        }
    }

    private String stringToScan(final int e) {
        final int[] codepoints = {'a', e, 'b'};
        return new String(codepoints, 0, codepoints.length);
    }

    @Test
    void testInRootAndEnglish() {
        final UnicodeSet dashes = new UnicodeSet("[\u2011\u00AD\u2013]").freeze();
        UnicodeSet escapeInSurveytool =
                new UnicodeSet(CodePointEscaper.ESCAPE_IN_SURVEYTOOL).removeAll(dashes).freeze();
        for (String locale : List.of("root", "en")) {
            CLDRFile engUnresolved =
                    CLDRConfig.getInstance()
                            .getCldrFactory()
                            .make(locale, false); // only actually present
            Set<PathHeader> sorted = new TreeSet<>();

            for (String path : engUnresolved) {
                String value = engUnresolved.getStringValue(path);
                if (escapeInSurveytool.containsSome(value)) {
                    sorted.add(PathHeader.getFactory().fromPath(path));
                }
            }
            for (PathHeader ph : sorted) {
                String value = engUnresolved.getStringValue(ph.getOriginalPath());
                boolean ok = true;
                switch (ph.getSectionId()) {
                    case DateTime:
                    case Numbers:
                    case Core_Data:
                    case Special:
                        continue;
                    case Currencies:
                        if (ph.getCode().startsWith("XOF")) {
                            continue; // TODO: log known issue?
                        }
                        if (ph.getCode().startsWith("SAR")
                                && logKnownIssue(
                                        "CLDR-18887",
                                        "revisit unclear test message when currency changes")) {
                            continue;
                        }
                        ok = false;
                        break;
                    case Units:
                        if (ph.getCode().startsWith("short") || ph.getCode().startsWith("narrow")) {
                            continue;
                        }
                        ok = false;
                        break;

                    default:
                        ok = false;
                        break;
                }
                assertTrue(
                        ok,
                        Joiners.TAB.join(
                                locale,
                                ph.toString(),
                                value,
                                CodePointEscaper.toEscaped(value, escapeInSurveytool)));
            }
        }
    }
}
