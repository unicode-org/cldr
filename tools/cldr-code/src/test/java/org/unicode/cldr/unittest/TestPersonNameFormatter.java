package org.unicode.cldr.unittest;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.personname.PersonNameFormatter;
import org.unicode.cldr.util.personname.PersonNameFormatter.FallbackFormatter;
import org.unicode.cldr.util.personname.PersonNameFormatter.Field;
import org.unicode.cldr.util.personname.PersonNameFormatter.FormatParameters;
import org.unicode.cldr.util.personname.PersonNameFormatter.Length;
import org.unicode.cldr.util.personname.PersonNameFormatter.NameObject;
import org.unicode.cldr.util.personname.PersonNameFormatter.NamePattern;
import org.unicode.cldr.util.personname.PersonNameFormatter.NamePatternData;
import org.unicode.cldr.util.personname.PersonNameFormatter.Order;
import org.unicode.cldr.util.personname.PersonNameFormatter.ParameterMatcher;
import org.unicode.cldr.util.personname.PersonNameFormatter.SampleType;
import org.unicode.cldr.util.personname.PersonNameFormatter.Style;
import org.unicode.cldr.util.personname.PersonNameFormatter.Usage;
import org.unicode.cldr.util.personname.SimpleNameObject;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.ULocale;

public class TestPersonNameFormatter extends TestFmwk{

    public static final boolean DEBUG = System.getProperty("TestPersonNameFormatter.DEBUG") != null;

    final FallbackFormatter FALLBACK_FORMATTER = new FallbackFormatter(ULocale.ENGLISH, "{0}*", "{0} {1}");
    final CLDRFile ENGLISH = CLDRConfig.getInstance().getEnglish();
    final PersonNameFormatter ENGLISH_NAME_FORMATTER = new PersonNameFormatter(ENGLISH);

    public static void main(String[] args) {
        new TestPersonNameFormatter().run(args);
    }

    private final NameObject sampleNameObject1 = SimpleNameObject.from(
        "locale=fr, prefix=Mr., given=John, given2-initial=B., given2= Bob, surname=Smith, surname2= Barnes Pascal, suffix=Jr.");
    private final NameObject sampleNameObject2 = SimpleNameObject.from(
        "locale=fr, prefix=Mr., given=John, surname=Smith, surname2= Barnes Pascal, suffix=Jr.");
    private final NameObject sampleNameObject3 = SimpleNameObject.from(
        "locale=fr, prefix=Mr., given=John Bob, surname=Smith, surname2= Barnes Pascal, suffix=Jr.");
    private final NameObject sampleNameObject4 = SimpleNameObject.from(
        "locale=ja, given=Shinzō, surname=Abe");

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
            "length=short; style=formal; usage=addressing; order=surnameFirst", "{surname-allCaps} {given}",
            "length=short medium; style=formal; usage=addressing", "{given} {given2-initial} {surname}",
            "length=short medium; style=formal; usage=addressing", "{given} {surname}",
            "length=monogram; style=formal; usage=addressing", "{given-initial}{surname-initial}",
            "", "{prefix} {given} {given2} {surname} {surname2} {suffix}");

        PersonNameFormatter personNameFormatter = new PersonNameFormatter(namePatternData, FALLBACK_FORMATTER);

        check(personNameFormatter, sampleNameObject1, "length=short; style=formal; usage=addressing", "John B. Smith");
        check(personNameFormatter, sampleNameObject2, "length=short; style=formal; usage=addressing", "John Smith");
        check(personNameFormatter, sampleNameObject1, "length=long; style=formal; usage=addressing", "Mr. John Bob Smith Barnes Pascal Jr.");
        check(personNameFormatter, sampleNameObject3, "length=monogram; style=formal; usage=addressing", "JS");
        check(personNameFormatter, sampleNameObject4, "length=short; style=formal; usage=addressing; order=surnameFirst", "ABE Shinzō");

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
        if (PersonNameFormatter.DEBUG) {
            logln(ENGLISH_NAME_FORMATTER.toString());
        }

        check(ENGLISH_NAME_FORMATTER, sampleNameObject1, "length=short; order=sorting", "Smith, J. B.");
        check(ENGLISH_NAME_FORMATTER, sampleNameObject1, "length=long; usage=referring; style=formal", "John Bob Smith Jr.");

        checkFormatterData(ENGLISH_NAME_FORMATTER);
    }

    public static final Joiner JOIN_SPACE = Joiner.on(' ');

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
            sb.setLength(sb.length()-1); // remove final \n
            showPattern("\t⇒", key, sb);
        }

        count = 0;
        sb.append("\nCOMPACTED:\n");

        Multimap<ParameterMatcher, NamePattern> compacted = PersonNameFormatter.compact(patternsToParameters);

        for (Entry<ParameterMatcher, Collection<NamePattern>> entry : compacted.asMap().entrySet()) {
            final ParameterMatcher key = entry.getKey();
            final Collection<NamePattern> value = entry.getValue();

            String prefix = ++count + ")";
            sb.append(prefix
                + "\t" + JOIN_SPACE.join(key.getLength())
                + "\t" + JOIN_SPACE.join(key.getUsage())
                + "\t" + JOIN_SPACE.join(key.getStyle())
                + "\t" + JOIN_SPACE.join(key.getOrder())
                );
            prefix = "";
            showPattern("\t⇒", value, sb);
        }
        logln(sb.toString());
    }

    private <T> void showPattern(String prefix, Iterable<T> iterable, StringBuilder sb) {
        for (T item : iterable) {
            sb.append(prefix + "\t︎" + item + "\n");
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

        FormatParameters testFormatParameters = new FormatParameters(Length.short_name, Style.formal, Usage.referring, Order.givenFirst);
        assertEquals("label test", "short-referring-formal-givenFirst",
            testFormatParameters.toLabel());

        // test just one example for ParameterMatcher, since there are too many combinations
        ParameterMatcher test = new ParameterMatcher(removeFirst(Length.ALL), removeFirst(Style.ALL), removeFirst(Usage.ALL), removeFirst(Order.ALL));
        assertEquals("label test", "medium-short-monogram-monogramNarrow-addressing-informal-givenFirst-surnameFirst",
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
        ExampleGenerator exampleGenerator = new ExampleGenerator(ENGLISH, ENGLISH, "");
        String[][] tests = {
            {
                "//ldml/personNames/personName[@length=\"long\"][@usage=\"referring\"][@style=\"formal\"][@order=\"givenFirst\"]/namePattern",
                "〖Katherine Johnson〗〖Alberto Pedro Calderón〗〖John Blue〗〖John William Brown〗〖Dorothy Lavinia Brown M.D.〗〖Erich Oswald Hans Carl Maria von Stroheim〗"
            },{
                "//ldml/personNames/personName[@length=\"monogram\"][@style=\"informal\"][@order=\"surnameFirst\"]/namePattern",
                "〖JK〗〖CA〗〖BJ〗〖BJ〗〖BD〗〖VE〗"
            }
        };
        for (String[] test : tests) {
            String path = test[0];
            String value = ENGLISH.getStringValue(path);
            String expected = test[1];
            final String example = exampleGenerator.getExampleHtml(path, value);
            String actual = ExampleGenerator.simplify(example);
            assertEquals("Example for " + value, expected, actual);
        }
    }

    public void TestFormatAll() {
        PersonNameFormatter personNameFormatter = ENGLISH_NAME_FORMATTER;
        Map<SampleType, NameObject> samples = PersonNameFormatter.loadSampleNames(ENGLISH);
        StringBuilder sb = DEBUG && isVerbose() ? new StringBuilder() : null;

        // Cycle through parameter combinations, check for exceptions even if locale has no data

        for (FormatParameters parameters : FormatParameters.all()) {
            assertNotNull(SampleType.full + " + " + parameters.toLabel(), personNameFormatter.format(samples.get(SampleType.full), parameters));
            assertNotNull(SampleType.multiword + " + " + parameters.toLabel(), personNameFormatter.format(samples.get(SampleType.multiword), parameters));
            Collection<NamePattern> nps = personNameFormatter.getBestMatchSet(parameters);
            if (sb != null) {
                for (NamePattern np : nps) {
                    sb.append(parameters.getLength()
                        + "\t" + parameters.getStyle()
                        + "\t" + parameters.getUsage()
                        + "\t" + parameters.getOrder()
                        + "\t" + np
                        + "\n"
                        );
                }
            }
        }
        if (sb != null) {
            System.out.println(sb);
        }
    }

    public void TestInvalidNameObjectThrows() {
        final String[] invalidPatterns = new String[] {
            "given2-initial=B",
        };
        for (final String pattern : invalidPatterns) {
            assertThrows("Invalid Name object " + pattern,
                () -> SimpleNameObject.from(pattern));
        }
    }

    public void TestEnglishComma() {
        boolean messageShown = false;
        for (Entry<ParameterMatcher, NamePattern> matcherAndPattern : ENGLISH_NAME_FORMATTER.getNamePatternData().getMatcherToPatterns().entries()) {
            ParameterMatcher parameterMatcher = matcherAndPattern.getKey();
            NamePattern namePattern = matcherAndPattern.getValue();

            Set<Order> orders = parameterMatcher.getOrder();
            Set<Length> lengths = parameterMatcher.getLength();

            // TODO Mark Look at whether it would be cleaner to replace empty values by ALL on building

            Set<Order> resolvedOrders = orders.isEmpty() ? Order.ALL : orders;
            Set<Length> resolvedLengths = lengths.isEmpty() ? Length.ALL : lengths;

            Set<Field> fields = namePattern.getFields();

            boolean givenAndSurname = (fields.contains(Field.given) || fields.contains(Field.given2))
                && (fields.contains(Field.surname) || fields.contains(Field.surname2));

            boolean commaRequired = givenAndSurname
                && resolvedOrders.contains(Order.sorting)
                && !resolvedLengths.contains(Length.monogram)
                && !resolvedLengths.contains(Length.monogramNarrow);

            boolean hasComma = namePattern.firstLiteralContaining(",") != null;

            if (!assertEquals("Comma right?\t" + parameterMatcher + " ➡︎ " + namePattern + "\t", commaRequired, hasComma)) {
                if (!messageShown) {
                    System.out.println("\t\tNOTE: In English, comma is required IFF the pattern has both given and surname, "
                + "and order has sorting, and length has neither monogram nor monogramNarrow,");
                    messageShown = true;
                }
            }
        }
    }
}
