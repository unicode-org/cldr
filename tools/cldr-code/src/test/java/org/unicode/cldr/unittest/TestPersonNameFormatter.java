package org.unicode.cldr.unittest;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.personname.PersonNameFormatter;
import org.unicode.cldr.util.personname.PersonNameFormatter.FallbackFormatter;
import org.unicode.cldr.util.personname.PersonNameFormatter.FormatParameters;
import org.unicode.cldr.util.personname.PersonNameFormatter.Length;
import org.unicode.cldr.util.personname.PersonNameFormatter.NameObject;
import org.unicode.cldr.util.personname.PersonNameFormatter.NamePattern;
import org.unicode.cldr.util.personname.PersonNameFormatter.NamePatternData;
import org.unicode.cldr.util.personname.PersonNameFormatter.Order;
import org.unicode.cldr.util.personname.PersonNameFormatter.ParameterMatcher;
import org.unicode.cldr.util.personname.PersonNameFormatter.Style;
import org.unicode.cldr.util.personname.PersonNameFormatter.Usage;
import org.unicode.cldr.util.personname.SimpleNameObject;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.ULocale;

public class TestPersonNameFormatter extends TestFmwk{

    private static final FallbackFormatter FALLBACK_FORMATTER = new FallbackFormatter(ULocale.ENGLISH, "{0}*", "{0} {1}");
    private static final CLDRFile ENGLISH = CLDRConfig.getInstance().getEnglish();

    public static void main(String[] args) {
        new TestPersonNameFormatter().run(args);
    }

    private final NameObject sampleNameObject1 = SimpleNameObject.from(
        "locale=fr, prefix=Mr., given=John, given2-initial=B., given2= Bob, surname=Smith, surname2= Barnes Pascal, suffix=Jr.");
    private final NameObject sampleNameObject2 = SimpleNameObject.from(
        "locale=fr, prefix=Mr., given=John, surname=Smith, surname2= Barnes Pascal, suffix=Jr.");

    private void check(PersonNameFormatter personNameFormatter, NameObject nameObject, String nameFormatParameters, String expected) {
        FormatParameters nameFormatParameters1 = FormatParameters.from(nameFormatParameters);
        String actual = personNameFormatter.format(nameObject, nameFormatParameters1);
        assertEquals("\n\t\t" + personNameFormatter + ";\n\t\t" + nameObject + ";\n\t\t" + nameFormatParameters1.toString(), expected, actual);
    }

    /**
     * TODO Mark TODO Peter flesh out and add more tests as the engine adds functionality
     */

    public void TestBasic() {

        ImmutableMap<ULocale, Order> localeToOrder = ImmutableMap.of(); // don't worry about using the order from the locale right now.

        // TOOD Mark For each example in the spec, make a test case for it.

        NamePatternData namePatternData = new NamePatternData(
            localeToOrder,
            "length=short medium; style=formal; usage=addressing", "{given} {given2-initial} {surname}",
            "length=short medium; style=formal; usage=addressing", "{given} {surname}",
            "", "{prefix} {given} {given2} {surname} {surname2} {suffix}");

        PersonNameFormatter personNameFormatter = new PersonNameFormatter(namePatternData, FALLBACK_FORMATTER);

        check(personNameFormatter, sampleNameObject1, "length=short; style=formal; usage=addressing", "John B. Smith");
        check(personNameFormatter, sampleNameObject2, "length=short; style=formal; usage=addressing", "John Smith");
        check(personNameFormatter, sampleNameObject1, "length=long; style=formal; usage=addressing", "Mr. John Bob Smith Barnes Pascal Jr.");

        checkFormatterData(personNameFormatter);
    }

    String HACK_INITIAL_FORMATTER = "{0}॰"; // use "unusual" period to mark when we are using fallbacks

    public void TestNamePatternParserThrowsWhenInvalidPatterns() {
        final String[] invalidPatterns = new String[] {
            "{",
            "}",
            "{}",
            "\\",
            "blah {given", "blah given}",
            "blah {given\\}",                           /* blah {given\} */
            "blah {given} yadda {}",
            "blah \\n"                                  /* blah \n */
        };
        for (final String pattern : invalidPatterns) {
            assertThrows(String.format("Pattern '%s'", pattern), () -> {
                NamePattern.from(0, pattern);
            });
        }
    }

    public void TestNamePatternParserRountripsValidPattern() {
        final String[] validPatterns = new String[] {
            "{given} {given2-initial}. {surname}",
            "no \\{fields\\} pattern",                  /* no \{fields\} pattern */
            "{given} \\\\ {surname}"                    /* {given} \\ {surname} */
        };
        for (final String pattern : validPatterns) {
            NamePattern namePattern = NamePattern.from(0, pattern);
            assertEquals("Failed to roundtrip valid pattern", String.format("\"%s\"", pattern), namePattern.toString());
        }
    }

    public void TestWithCLDR() {
        PersonNameFormatter personNameFormatter = new PersonNameFormatter(ENGLISH);
        if (PersonNameFormatter.DEBUG) {
            logln(personNameFormatter.toString());
        }

        // TODO Rich once the code is fixed to strip empty fields, fix this test

        check(personNameFormatter, sampleNameObject1, "length=short; usage=sorting", "Smith, J. B.");
        check(personNameFormatter, sampleNameObject1, "length=long; usage=referring; style=formal", "John Bob Smith Jr.");

        // TODO: we are getting the wrong answer for the second one obove.
        // The problem is that it is matching order='surnameFirst' since that occurs first.

        checkFormatterData(personNameFormatter);
    }

    /**
     * Check that no exceptions happen in expansion and compaction.
     * In verbose mode (-v), show results.
     */

    private void checkFormatterData(PersonNameFormatter personNameFormatter) {
        // check that no exceptions happen
        // sort by the output patterns
        Multimap<Iterable<NamePattern>, FormatParameters> patternsToParameters = PersonNameFormatter.groupByNamePatterns(personNameFormatter.expand());

        StringBuilder sb = new StringBuilder("\n");
        int count = 0;
        sb.append("\nEXPANDED:\n");
        for (Entry<Iterable<NamePattern>, Collection<FormatParameters>> entry : patternsToParameters.asMap().entrySet()) {
            final Iterable<NamePattern> key = entry.getKey();
            final Collection<FormatParameters> value = entry.getValue();

            String prefix = ++count + ")";
            for (FormatParameters parameters : value) {
                sb.append(prefix + "\t" + parameters + "\n");
                prefix = "";
            }
            showPattern("⇒", key, sb);
        }

        count = 0;
        sb.append("\nCOMPACTED:\n");

        Multimap<ParameterMatcher, NamePattern> compacted = PersonNameFormatter.compact(patternsToParameters);

        for (Entry<ParameterMatcher, Collection<NamePattern>> entry : compacted.asMap().entrySet()) {
            final ParameterMatcher key = entry.getKey();
            final Collection<NamePattern> value = entry.getValue();

            String prefix = ++count + ")";
            sb.append(prefix + "\t" + key + "\n");
            prefix = "";
            showPattern("⇒", value, sb);
        }
        logln(sb.toString());
    }

    private <T> void showPattern(String prefix, Iterable<T> iterable, StringBuilder sb) {
        for (T item : iterable) {
            sb.append(prefix + "\t\t︎" + item + "\n");
            prefix = "";
        }
    }

    public void TestFields() {
        Set<String> items = new HashSet<>();
        for (Set<? extends Enum<?>> set : Arrays.asList(Length.ALL, Style.ALL, Usage.ALL, Order.ALL)) {
            for (Enum<?> item : set) {
                boolean added = items.add(item.toString());
                assertTrue("value names are disjoint", added);
            }
        }
    }

    public void TestLabels() {
        Set<String> items = new HashSet<>();
        for (FormatParameters item : FormatParameters.all()) {
            String label = item.toLabel();
            boolean added = items.add(item.toString());
            assertTrue("label test\t"+ item + "\t" + label + "\t", added);
        }

        FormatParameters testFormatParameters = new FormatParameters(Length.short_name, Style.formal, Usage.sorting, Order.givenFirst);
        assertEquals("label test", "short-sorting-formal-givenFirst",
            testFormatParameters.toLabel());

        // test just one example for ParameterMatcher, since there are too many combinations
        ParameterMatcher test = new ParameterMatcher(removeFirst(Length.ALL), removeFirst(Style.ALL), removeFirst(Usage.ALL), removeFirst(Order.ALL));
        assertEquals("label test", "medium-short-monogram-monogramNarrow-addressing-sorting-informal-givenFirst",
            test.toLabel());
    }

    public void TestLiteralTextElision() {
        ImmutableMap<ULocale, Order> localeToOrder = ImmutableMap.of(); // don't worry about using the order from the locale right now.

        NamePatternData namePatternData = new NamePatternData(
            localeToOrder,
            "", "1{prefix}1 2{given}2 3{given2}3 4{surname}4 5{surname2}5 6{suffix}6");

        PersonNameFormatter personNameFormatter = new PersonNameFormatter(namePatternData, FALLBACK_FORMATTER);

        check(personNameFormatter,
              SimpleNameObject.from(
                    "locale=en, prefix=Mr., given=John, given2= Bob, surname=Smith, surname2= Barnes Pascal, suffix=Jr."),
              "length=short; style=formal; usage=addressing",
              "1Mr.1 2John2 3Bob3 4Smith4 5Barnes Pascal5 6Jr.6"
        );

        check(personNameFormatter,
              SimpleNameObject.from(
                    "locale=en, given2= Bob, surname=Smith, surname2= Barnes Pascal, suffix=Jr."),
              "length=short; style=formal; usage=addressing",
              "Bob3 4Smith4 5Barnes Pascal5 6Jr.6"
        );

        check(personNameFormatter,
              SimpleNameObject.from(
                    "locale=en, prefix=Mr., given=John, given2= Bob, surname=Smith"),
              "length=short; style=formal; usage=addressing",
              "1Mr.1 2John2 3Bob3 4Smith"
        );

         check(personNameFormatter,
              SimpleNameObject.from(
                    "locale=en, prefix=Mr., surname=Smith, surname2= Barnes Pascal, suffix=Jr."),
              "length=short; style=formal; usage=addressing",
              "1Mr.1 4Smith4 5Barnes Pascal5 6Jr.6"
        );

        check(personNameFormatter,
              SimpleNameObject.from(
                    "locale=en, given=John, surname=Smith"),
              "length=short; style=formal; usage=addressing",
              "John2 4Smith"
        );

   }

    private <T> Set<T> removeFirst(Set<T> all) {
        Set<T> result = new LinkedHashSet<>(all);
        T first = all.iterator().next();
        result.remove(first);
        return result;
    }

    private void assertThrows(String subject, Runnable code) {
        try {
            code.run();
            fail(String.format("%s was supposed to throw an exception.", subject));
        }
        catch (Exception e) {
            assertTrue(subject + " threw exception as expected", true);
        }
    }

    // TODO Mark test that the order of the NamePatterns is maintained when expanding, compacting

    public void TestExampleGenerator() {
        String path = "//ldml/personNames/personName[@length=\"long\"][@usage=\"referring\"][@style=\"formal\"][@order=\"givenFirst\"]/namePattern";
        String expected = "〖Katherine Johnson〗〖Alberto Pedro Calderón〗〖Dorothy Lavinia Brown M.D.〗〖Erich Oswald Hans Carl Maria von Stroheim〗";
        String value = ENGLISH.getStringValue(path);
        ExampleGenerator test = new ExampleGenerator(ENGLISH, ENGLISH, "");
        final String example = test.getExampleHtml(path, value);
        String actual = ExampleGenerator.simplify(example);
        assertEquals("Example for " + value, expected, actual);
        // TODO cycle through parameter combinations, check for now exceptions even if locale has no data
    }

    public void TestInvalidNameObjectThrows() {
        // TODO Consider other invariants.
        // Must have given.
        // No surname2 unless there is a surname
        final String[] invalidPatterns = new String[] {
            "given2-initial=B",
        };
        for (final String pattern : invalidPatterns) {
            assertThrows("Invalid Name object " + pattern,
                () -> SimpleNameObject.from(pattern));
        }
    }
}
