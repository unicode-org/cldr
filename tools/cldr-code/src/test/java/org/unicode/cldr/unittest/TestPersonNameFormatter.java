package org.unicode.cldr.unittest;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.personname.PersonNameFormatter;
import org.unicode.cldr.util.personname.PersonNameFormatter.FallbackFormatter;
import org.unicode.cldr.util.personname.PersonNameFormatter.Field;
import org.unicode.cldr.util.personname.PersonNameFormatter.FormatParameters;
import org.unicode.cldr.util.personname.PersonNameFormatter.Length;
import org.unicode.cldr.util.personname.PersonNameFormatter.ModifiedField;
import org.unicode.cldr.util.personname.PersonNameFormatter.Modifier;
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
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.ULocale;

public class TestPersonNameFormatter extends TestFmwk{

    public static final boolean DEBUG = System.getProperty("TestPersonNameFormatter.DEBUG") != null;

    final FallbackFormatter FALLBACK_FORMATTER = new FallbackFormatter(ULocale.ENGLISH, "{0}*", "{0} {1}");
    final CLDRFile ENGLISH = CLDRConfig.getInstance().getEnglish();
    final PersonNameFormatter ENGLISH_NAME_FORMATTER = new PersonNameFormatter(ENGLISH);
    final Map<SampleType, SimpleNameObject> ENGLISH_SAMPLES = PersonNameFormatter.loadSampleNames(ENGLISH);
    final Factory factory = CLDRConfig.getInstance().getCldrFactory();

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
            "order=givenFirst", "{prefix} {given} {given2} {surname} {surname2} {suffix}",
            "order=surnameFirst", "{surname} {surname2} {prefix} {given} {given2} {suffix}",
            "order=sorting", "{surname} {surname2}, {prefix} {given} {given2} {suffix}");

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
        final String[][] invalidPatterns = {
            {"{", "Unmatched {: «{❌»"},
            {"}", "Unexpected }: «❌}»"},
            {"{}", "Empty field '{}' is not allowed : «{❌}»"},
            {"\\", "Invalid character: : «❌\\»"},
            {"blah {given", "Unmatched {: «blah {given❌»"},
            {"blah {given\\}", "Unmatched {: «blah {given\\}❌»"},                           /* blah {given\} */
            {"blah {given} yadda {}", "Empty field '{}' is not allowed : «blah {given} yadda {❌}»"},
            {"blah \\n", "Escaping character 'n' is not supported: «blah ❌\\n»"}                                  /* blah \n */
        };
        for (final String[] pattern : invalidPatterns) {
            assertThrows(String.format("Pattern '%s'", pattern[0]), () -> {
                NamePattern.from(0, pattern[0]);
            }, pattern[1]);
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
            "order=givenFirst", "1{prefix}1 2{given}2 3{given2}3 4{surname}4 5{surname2}5 6{suffix}6");

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

    private void assertThrows(String subject, Runnable code, String expectedMessage) {
        try {
            code.run();
            fail(String.format("%s was supposed to throw an exception.", subject));
        }
        catch (Exception e) {
            assertEquals(subject + " threw exception as expected", expectedMessage, e.getMessage());
        }
    }

    // TODO Mark test that the order of the NamePatterns is maintained when expanding, compacting

    public void TestExampleGenerator() {

        // first test some specific examples

        ExampleGenerator exampleGenerator = new ExampleGenerator(ENGLISH, ENGLISH, "");
        String[][] tests = {
            {
                "//ldml/personNames/personName[@length=\"long\"][@usage=\"referring\"][@style=\"formal\"][@order=\"givenFirst\"]/namePattern",
                "〖Katherine Johnson〗〖Alberto Pedro Calderón〗〖John Blue〗〖John William Brown〗〖Dorothy Lavinia Brown M.D.〗〖Erich Oswald Hans Carl Maria von Stroheim〗〖Sinbad〗"
            },{
                "//ldml/personNames/personName[@length=\"monogram\"][@style=\"informal\"][@order=\"surnameFirst\"]/namePattern",
                "〖JK〗〖CA〗〖BJ〗〖BJ〗〖BD〗〖VE〗〖S〗"
            },{
                "//ldml/personNames/nameOrderLocales[@order=\"givenFirst\"]",
                "〖und = «any other»〗"
            },{
                "//ldml/personNames/nameOrderLocales[@order=\"surnameFirst\"]",
                "〖ja = Japanese〗〖zh = Chinese〗〖ko = Korean〗"
            }
        };
        for (String[] test : tests) {
            String path = test[0];
            String value = ENGLISH.getStringValue(path);
            String expected = test[1];
            checkExampleGenerator(exampleGenerator, path, value, expected);
        }

        // next test that the example generator returns non-null for all expected cases


        for (String localeId : Arrays.asList("en", "fr")) {
            final CLDRFile cldrFile = factory.make(localeId, true);
            ExampleGenerator exampleGenerator2 = new ExampleGenerator(cldrFile, ENGLISH, "");
            for (String path : cldrFile) {
                if (path.startsWith("//ldml/personNames") && !path.endsWith("/alias")) {
                    XPathParts parts = XPathParts.getFrozenInstance(path);
                    String value = ENGLISH.getStringValue(path);
                    String example = exampleGenerator2.getExampleHtml(path, value);
                    String actual = ExampleGenerator.simplify(example);
                    switch(parts.getElement(2)) {
                    case "initialPattern":
                    case "sampleName":
                        // expect null
                        break;
                    case "nameOrderLocales":
                    case "personName":
                        if (!assertNotNull("Locale " + localeId + " example for " + value, actual)) {
                            example = exampleGenerator.getExampleHtml(path, value); // redo for debugging
                        }
                        break;
                    }
                }
            }
        }
    }

    private void checkExampleGenerator(ExampleGenerator exampleGenerator, String path, String value, String expected) {
        final String example = exampleGenerator.getExampleHtml(path, value);
        String actual = ExampleGenerator.simplify(example);
        assertEquals("Example for " + value, expected, actual);
    }

    public void TestExampleDependencies() {
        Factory cldrFactory = CLDRConfig.getInstance().getCldrFactory();
        CLDRFile root = cldrFactory.make("root", false);
        CLDRFile en = cldrFactory.make("en", false);

        // we create a new factory, and add root and a writable English cldrfile

        CLDRFile enWritable = en.cloneAsThawed();

        TestFactory factory = new TestFactory();
        factory.addFile(root);
        factory.addFile(enWritable);
        CLDRFile resolved = factory.make("en", true);

        // List all the paths that have dependencies, so we can verify they are ok

        PathStarrer ps = new PathStarrer().setSubstitutionPattern("*");
        for (String path : resolved) {
            if (path.startsWith("//ldml/personNames") && !path.endsWith("/alias")) {
                logln(ps.set(path));
            }
        }

        // First test the example for the regular value

        ExampleGenerator exampleGenerator = new ExampleGenerator(resolved, ENGLISH, "");
        String path = "//ldml/personNames/personName[@length=\"long\"][@usage=\"referring\"][@style=\"formal\"][@order=\"givenFirst\"]/namePattern";
        String expected = "〖Katherine Johnson〗〖Alberto Pedro Calderón〗〖John Blue〗〖John William Brown〗〖Dorothy Lavinia Brown M.D.〗〖Erich Oswald Hans Carl Maria von Stroheim〗〖Sinbad〗";
        String value = enWritable.getStringValue(path);

        checkExampleGenerator(exampleGenerator, path, value, expected);

        // Then change one of the sample names to make sure it alters the example correctly

        String namePath = "//ldml/personNames/sampleName[@item=\"givenSurname\"]/nameField[@type=\"given\"]";
        enWritable.add(namePath, "KATHY");
        exampleGenerator.updateCache(namePath);

        String expected2 = "〖KATHY Johnson〗〖Alberto Pedro Calderón〗〖John Blue〗〖John William Brown〗〖Dorothy Lavinia Brown M.D.〗〖Erich Oswald Hans Carl Maria von Stroheim〗〖Sinbad〗";
        checkExampleGenerator(exampleGenerator, path, value, expected2);
    }

    public void TestFormatAll() {
        PersonNameFormatter personNameFormatter = ENGLISH_NAME_FORMATTER;
        StringBuilder sb = DEBUG && isVerbose() ? new StringBuilder() : null;

        // Cycle through parameter combinations, check for exceptions even if locale has no data

        for (FormatParameters parameters : FormatParameters.all()) {
            assertNotNull(SampleType.full + " + " + parameters.toLabel(), personNameFormatter.format(ENGLISH_SAMPLES.get(SampleType.full), parameters));
            assertNotNull(SampleType.multiword + " + " + parameters.toLabel(), personNameFormatter.format(ENGLISH_SAMPLES.get(SampleType.multiword), parameters));
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
        final String[][] invalidPatterns = {
            {"given2-initial=B","Every field must have a completely modified value given2={[initial]=B}"}
        };
        for (final String[] pattern : invalidPatterns) {
            assertThrows("Invalid Name object: " + pattern,
                () -> SimpleNameObject.from(pattern[0]), pattern[1]);
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

    public void TestNameSamples() {
        // TODO Mark move this to a Check

        // define a set with the standard fields we ask for
        Set<ModifiedField> expectedBase = new TreeSet<>();
        for (Field field : Field.ALL) {
            expectedBase.add(new ModifiedField(field));
        }
        expectedBase.add(new ModifiedField(Field.given, Modifier.informal));
        ImmutableSet<ModifiedField> expected = ImmutableSet.copyOf(expectedBase);

        for (String localeId : Arrays.asList("en", "fr")) {
            final CLDRFile cldrFile = factory.make(localeId, true);

            // check that all fields are covered

            final Map<SampleType, SimpleNameObject> sampleMap = PersonNameFormatter.loadSampleNames(cldrFile);
            Multimap<ModifiedField, SampleType> samples = getInvertedSamples(sampleMap);
            assertEquals("Locale " + localeId + " expected field coverage in samples", expected, samples.keySet());
            if (isVerbose()) {
                logln("\n\t" + Joiner.on("\n\t").join(samples.asMap().entrySet()));
            }
        }
    }

    private ImmutableListMultimap<ModifiedField, SampleType> getInvertedSamples(Map<SampleType, SimpleNameObject> map) {
        Multimap<ModifiedField, SampleType> result = TreeMultimap.create();
        for (Entry<SampleType, SimpleNameObject> sampleEntry : map.entrySet()) {
            SampleType sampleType = sampleEntry.getKey();
            SimpleNameObject nameObject = sampleEntry.getValue();
            for (Entry<Field, Map<Set<Modifier>, String>> entry : nameObject.getPatternData().entrySet()) {
                Field field = entry.getKey();
                Map<Set<Modifier>, String> modsToValue = entry.getValue();
                for (Entry<Set<Modifier>, String> set : modsToValue.entrySet()) {
                    ModifiedField mf = new ModifiedField(field, set.getKey());
                    result.put(mf, sampleType);
                }
            }
        }
        return ImmutableListMultimap.copyOf(result);
    }
}
