package org.unicode.cldr.util.fixedcandidates;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.RegexUtilities;
import org.unicode.cldr.util.fixedcandidates.FixedCandidateProvider.CompoundFixedCandidateProvider;

public class TestFixedCandidateProvider {
    private static final String MASS_GENDER_XPATH =
            "//ldml/units/unitLength[@type=\"short\"]/unit[@type=\"mass-ton\"]/gender";

    private static final String RATIONAL_USAGE_XPATH =
            "//ldml/numbers/rationalFormats[@numberSystem=\"latn\"]/rationalUsage";

    @Test
    void testFixed() {
        final String colorList[] = {"red", "green", "blue"};
        final FixedCandidateProvider colors =
                FixedCandidateProvider.forXPath(
                        "//colors/primary", ImmutableSet.of("red", "green", "blue"));
        assertAll(
                "check colors",
                () -> assertNull(colors.apply("//shapes/euclidean")),
                () -> assertArrayEquals(colorList, colors.apply("//colors/primary").toArray()));
    }

    @Test
    void testPattern() {
        final String colorList[] = {"red", "green", "blue"};
        final FixedCandidateProvider colors =
                FixedCandidateProvider.forXPathPattern(
                        "//colors/.*", ImmutableSet.of("red", "green", "blue"));
        assertAll(
                "check colors",
                () -> assertNull(colors.apply("//shapes/euclidean")),
                () -> assertArrayEquals(colorList, colors.apply("//colors/primary").toArray()),
                () -> assertArrayEquals(colorList, colors.apply("//colors/tertiary").toArray()));
    }

    public enum Colors {
        red,
        green,
        blue
    }

    @Test
    void testEnum() {
        final String colorList[] = {"red", "green", "blue"};
        final FixedCandidateProvider colors =
                FixedCandidateProvider.forXPathAndEnum("//colors/primary", Colors.values());
        assertAll(
                "check colors",
                () -> assertNull(colors.apply("//shapes/euclidean")),
                () -> assertArrayEquals(colorList, colors.apply("//colors/primary").toArray()));
        final Colors stopGoColors[] = {Colors.red, Colors.green};
        final String stopGoColorsList[] = {"red", "green"};
        final FixedCandidateProvider stopGo =
                FixedCandidateProvider.forXPathAndEnum("//colors/stopGo", stopGoColors);
        assertAll(
                "check colors",
                () -> assertNull(stopGo.apply("//shapes/euclidean")),
                () ->
                        assertArrayEquals(
                                stopGoColorsList, stopGo.apply("//colors/stopGo").toArray()));
    }

    @Test
    void testRationalUsage() {
        final CLDRLocale en = CLDRLocale.getInstance("en");
        final FixedCandidateProvider candidates_en = DefaultFixedCandidates.forLocale(en);
        Collection<String> actualRaw = candidates_en.apply(RATIONAL_USAGE_XPATH);
        Set<String> actual = actualRaw == null ? null : Set.copyOf(actualRaw);

        // When a regex fails, this can be used to debug the failure.
        if (false) {
            Matcher matcher =
                    Pattern.compile(
                                    "//ldml/numbers/rationalFormats\\[@numberSystem=\"[^\"]*\"\\]/rationalUsage")
                            .matcher(RATIONAL_USAGE_XPATH);
            String mismatch = RegexUtilities.showMismatch(matcher, RATIONAL_USAGE_XPATH);
            System.out.println(mismatch);
        }

        assertEquals(Set.of("never", "sometimes"), actual, RATIONAL_USAGE_XPATH);
    }

    @Test
    void testDefaultCandidates() {
        final CLDRLocale en = CLDRLocale.getInstance("en");
        final CLDRLocale de = CLDRLocale.getInstance("de");
        final CLDRLocale mul = CLDRLocale.getInstance("mul");
        final FixedCandidateProvider candidates_en = DefaultFixedCandidates.forLocale(en);
        final FixedCandidateProvider candidates_de = DefaultFixedCandidates.forLocale(de);
        final FixedCandidateProvider candidates_mul = DefaultFixedCandidates.forLocale(mul);
        assertNotNull(candidates_en);
        assertNotNull(candidates_de);
        assertNotNull(candidates_mul);

        assertAll(
                "check en",
                () -> assertNull(candidates_en.apply("//foo/bar")),
                () ->
                        assertNotNull(
                                candidates_en.apply(
                                        "//ldml/personNames/parameterDefault[@parameter=\"length\"]")));
        /** see PersonNameFormatter.Length */
        final String lengths[] = {"long", "medium", "short"};
        assertArrayEquals(
                lengths,
                candidates_en
                        .apply("//ldml/personNames/parameterDefault[@parameter=\"length\"]")
                        .toArray());

        final String tonGrammarDe[] = candidates_de.apply(MASS_GENDER_XPATH).toArray(new String[0]);
        final String expectTonGrammarDe[] = {"feminine", "masculine", "neuter"};
        assertArrayEquals(expectTonGrammarDe, tonGrammarDe);
        final String expectTonGrammarEn[] = {};
        final String tonGrammarEn[] = candidates_en.apply(MASS_GENDER_XPATH).toArray(new String[0]);
        assertArrayEquals(expectTonGrammarEn, tonGrammarEn); // no grammar here
        assertArrayEquals(
                expectTonGrammarEn,
                candidates_mul
                        .apply(MASS_GENDER_XPATH)
                        .toArray(new String[0])); // no grammar in this locale
    }

    @Test
    void testNoOverlap() {
        final CLDRLocale locale = CLDRLocale.getInstance("de");
        final CompoundFixedCandidateProvider candidates =
                (CompoundFixedCandidateProvider) DefaultFixedCandidates.forLocale(locale);
        final CLDRFile f =
                CLDRConfig.getInstance().getCldrFactory().make(locale.getBaseName(), true);

        final ArrayList<FixedCandidateProvider> matches = new ArrayList<>();
        for (final String x : f.fullIterable()) {
            matches.clear();
            for (final FixedCandidateProvider d : candidates.getDelegates()) {
                final Collection<String> r = d.apply(x);
                if (r != null && !r.isEmpty()) {
                    matches.add(d);
                }
            }
            assertTrue(
                    matches.size() <= 1,
                    () -> x + " Matched more than one candidate provider: " + matches);
        }
    }
}
