package org.unicode.cldr.unittest;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.test.CheckPersonNames;
import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.personname.PersonNameFormatter;
import org.unicode.cldr.util.personname.PersonNameFormatter.FallbackFormatter;
import org.unicode.cldr.util.personname.PersonNameFormatter.Field;
import org.unicode.cldr.util.personname.PersonNameFormatter.Formality;
import org.unicode.cldr.util.personname.PersonNameFormatter.FormatParameters;
import org.unicode.cldr.util.personname.PersonNameFormatter.Length;
import org.unicode.cldr.util.personname.PersonNameFormatter.Modifier;
import org.unicode.cldr.util.personname.PersonNameFormatter.NameObject;
import org.unicode.cldr.util.personname.PersonNameFormatter.NamePattern;
import org.unicode.cldr.util.personname.PersonNameFormatter.NamePatternData;
import org.unicode.cldr.util.personname.PersonNameFormatter.Order;
import org.unicode.cldr.util.personname.PersonNameFormatter.ParameterMatcher;
import org.unicode.cldr.util.personname.PersonNameFormatter.SampleType;
import org.unicode.cldr.util.personname.PersonNameFormatter.Usage;
import org.unicode.cldr.util.personname.SimpleNameObject;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

public class TestPersonNameFormatter extends TestFmwk{

    public static final boolean DEBUG = System.getProperty("TestPersonNameFormatter.DEBUG") != null;
    public static final boolean SHOW = System.getProperty("TestPersonNameFormatter.SHOW") != null;

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
    private final NameObject sampleNameObject5 = SimpleNameObject.from(
        "locale=en, given=Mary, surname=Smith");
    private final NameObject sampleNameObjectPrefixCore = SimpleNameObject.from(
        "locale=en, given=Mary, surname-prefix=van der, surname-core=Beck");


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
            "order=surnameFirst; length=short; usage=addressing; formality=formal", "{surname-allCaps} {given}",
            "length=short medium; usage=addressing; formality=formal", "{given} {given2-initial} {surname}",
            "length=short medium; usage=addressing; formality=formal", "{given} {surname}",
            "length=long; usage=monogram; formality=formal", "{given-initial}{surname-initial}",
            "order=givenFirst", "{prefix} {given} {given2} {surname} {surname2} {suffix}",
            "order=surnameFirst", "{surname} {surname2} {prefix} {given} {given2} {suffix}",
            "order=sorting", "{surname} {surname2}, {prefix} {given} {given2} {suffix}");

        PersonNameFormatter personNameFormatter = new PersonNameFormatter(namePatternData, FALLBACK_FORMATTER);

        check(personNameFormatter, sampleNameObjectPrefixCore, "length=short; usage=addressing; formality=formal", "Mary van der Beck");

        check(personNameFormatter, sampleNameObject1, "length=short; usage=addressing; formality=formal", "John B. Smith");
        check(personNameFormatter, sampleNameObject2, "length=short; usage=addressing; formality=formal", "John Smith");
        check(personNameFormatter, sampleNameObject1, "length=long; usage=addressing; formality=formal", "Mr. John Bob Smith Barnes Pascal Jr.");
        check(personNameFormatter, sampleNameObject3, "length=long; usage=monogram; formality=formal", "J* B*S*"); // TODO This is wrong
        check(personNameFormatter, sampleNameObject4, "order=surnameFirst; length=short; usage=addressing; formality=formal", "ABE Shinzō");

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
        if (SHOW) {
            warnln(ENGLISH_NAME_FORMATTER.toString());
        } else {
            warnln("To see the contents of the English patterns, use -DTestPersonNameFormatter.SHOW");
        }

        check(ENGLISH_NAME_FORMATTER, sampleNameObject1, "order=sorting; length=short", "Smith, J. B.");
        check(ENGLISH_NAME_FORMATTER, sampleNameObject1, "length=long; usage=referring; formality=formal", "John Bob Smith Jr.");

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
                + "\t" + JOIN_SPACE.join(key.getOrder())
                + "\t" + JOIN_SPACE.join(key.getLength())
                + "\t" + JOIN_SPACE.join(key.getUsage())
                + "\t" + JOIN_SPACE.join(key.getFormality())
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
        for (Set<? extends Enum<?>> set : Arrays.asList(Order.ALL, Length.ALL, Usage.ALL, Formality.ALL)) {
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

        FormatParameters testFormatParameters = new FormatParameters(Order.givenFirst, Length.short_name, Usage.referring, Formality.formal);
        assertEquals("label test", "givenFirst-short-referring-formal",
            testFormatParameters.toLabel());

        // test just one example for ParameterMatcher, since there are too many combinations
        ParameterMatcher test = new ParameterMatcher(removeFirst(Order.ALL), removeFirst(Length.ALL), removeFirst(Usage.ALL), removeFirst(Formality.ALL));
        assertEquals("label test", "surnameFirst-sorting-medium-short-addressing-monogram-informal",
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
            "length=short; usage=addressing; formality=formal",
            "1Mr.1 2John2 3Bob3 4Smith4 5Barnes Pascal5 6Jr.6"
            );

        check(personNameFormatter,
            SimpleNameObject.from(
                "locale=en, given2= Bob, surname=Smith, surname2= Barnes Pascal, suffix=Jr."),
            "length=short; usage=addressing; formality=formal",
            "Bob3 4Smith4 5Barnes Pascal5 6Jr.6"
            );

        check(personNameFormatter,
            SimpleNameObject.from(
                "locale=en, prefix=Mr., given=John, given2= Bob, surname=Smith"),
            "length=short; usage=addressing; formality=formal",
            "1Mr.1 2John2 3Bob3 4Smith"
            );

        check(personNameFormatter,
            SimpleNameObject.from(
                "locale=en, prefix=Mr., surname=Smith, surname2= Barnes Pascal, suffix=Jr."),
            "length=short; usage=addressing; formality=formal",
            "1Mr.1 4Smith4 5Barnes Pascal5 6Jr.6"
            );

        check(personNameFormatter,
            SimpleNameObject.from(
                "locale=en, given=John, surname=Smith"),
            "length=short; usage=addressing; formality=formal",
            "John2 4Smith"
            );
    }

    public void TestLiteralTextElisionNoSpaces() {

        // Also used to generate examples in the user guide

        ImmutableMap<ULocale, Order> localeToOrder = ImmutableMap.of(); // don't worry about using the order from the locale right now.

        NamePatternData namePatternData2 = new NamePatternData(
            localeToOrder,
            "order=givenFirst",     "¹{prefix}₁²{given}₂³{given2}₃⁴{surname}₄⁵{surname2}₅⁶{suffix}₆",
            "order=surnameFirst",   "¹{surname-allCaps}₁²{surname2}₂³{prefix}₃⁴{given}₄⁵{given2}₅⁶{suffix}₆",
            "order=sorting",        "¹{surname}₁²{surname2},³{prefix}₃⁴{given}₄⁵{given2}₅⁶{suffix}₆");

        PersonNameFormatter personNameFormatter2 = new PersonNameFormatter(namePatternData2, FALLBACK_FORMATTER);

        check(personNameFormatter2, sampleNameObject5, "order=givenFirst", "Mary₂³₃⁴Smith"); // TODO Rich, make ₁²Mary₂³₃⁴Smith₄ or we change desc.
        check(personNameFormatter2, sampleNameObject5, "order=surnameFirst", "¹SMITH₁²₃⁴Mary"); // similar
        check(personNameFormatter2, sampleNameObject5, "order=sorting", "¹Smith₁²₃⁴Mary"); //  TODO RICH should be ¹Smith,³₃⁴Mary₄
    }

    public void TestLiteralTextElisionSpaces() {

        // Also used to generate examples in the user guide
        if (SHOW) {
            logln("Patterns for User Guide:");
            final String pattern = "¹{prefix}₁ ²{given}₂ ³{given2}₃ ⁴{surname}₄";
            final String patternNoSpaces = "¹{prefix}₁²{given}₂³{given2}₃⁴{surname}₄";
            System.out.println(pattern.replace(" ", "⌴"));
            System.out.println(patternNoSpaces);
            System.out.println("[" + pattern.replace(" ", "]⌴[") + "]");
            System.out.println("[" + patternNoSpaces.replace("}", "}[").replace("{", "]{") + "]");
        }

        ImmutableMap<ULocale, Order> localeToOrder = ImmutableMap.of(); // don't worry about using the order from the locale right now.

        NamePatternData namePatternData2 = new NamePatternData(
            localeToOrder,
            "order=givenFirst",     "¹{prefix}₁ ²{given}₂ ³{given2}₃ ⁴{surname}₄ ⁵{surname2}₅ ⁶{suffix}₆",
            "order=surnameFirst",   "¹{surname-allCaps}₁ ²{surname2}₂ ₃{prefix}₃ ⁴{given}₄ ⁵{given2}₅ ⁶{suffix}₆",
            "order=sorting",        "¹{surname}₁ ²{surname2}, ₃{prefix}₃ ⁴{given}₄ ⁵{given2}₅ ⁶{suffix}₆");

        PersonNameFormatter personNameFormatter2 = new PersonNameFormatter(namePatternData2, FALLBACK_FORMATTER);

        check(personNameFormatter2, sampleNameObject5, "order=givenFirst", "Mary₂ ⁴Smith"); // TODO Rich to make ²Mary₂ ⁴Smith₄ (or we change desc.
        check(personNameFormatter2, sampleNameObject5, "order=surnameFirst", "¹SMITH₁ ⁴Mary"); // similar
        check(personNameFormatter2, sampleNameObject5, "order=sorting", "¹Smith₁ ⁴Mary"); //  TODO RICH should be ¹Smith, ⁴Mary₄

        // TODO Rich: The behavior is not quite what I describe in the guide.
        // Example: ²{given}₂ means that ² and ₂ "belong to" Mary, so the results should have ²Mary₂.
        //  But the 2's are being removed except for very leading/trailing fields.
        //  So we need to decide whether the code needs to change or the description does.
        //  Same for no spaces: we should decide what the desired behavior is.
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
                "//ldml/personNames/personName[@order=\"givenFirst\"][@length=\"long\"][@usage=\"referring\"][@formality=\"formal\"]/namePattern",
                "〖Sinbad〗〖Irene Adler〗〖John Hamish Watson〗〖Ada Cornelia Eva Sophia van den Wolf M.D. Ph.D.〗"
            },{
                "//ldml/personNames/personName[@order=\"surnameFirst\"][@length=\"long\"][@usage=\"monogram\"][@formality=\"informal\"]/namePattern",
                "〖S〗〖AI〗〖WJ〗〖VN〗"
            },{
                "//ldml/personNames/nameOrderLocales[@order=\"givenFirst\"]",
                "〖und = «any other»〗〖en = English〗"
            },{
                "//ldml/personNames/nameOrderLocales[@order=\"surnameFirst\"]",
                "〖ja = Japanese〗〖ko = Korean〗〖zh = Chinese〗"
            }
        };
        for (String[] test : tests) {
            String path = test[0];
            String value = ENGLISH.getStringValue(path);
            String expected = test[1];
            checkExampleGenerator(exampleGenerator, path, value, expected);
        }

        // next test that the example generator returns non-null for all expected cases

        for (String localeId : Arrays.asList("en")) {
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
        String path = checkPath("//ldml/personNames/personName[@order=\"givenFirst\"][@length=\"long\"][@usage=\"referring\"][@formality=\"formal\"]/namePattern");
        String value2 = enWritable.getStringValue(path); // check that English is as expected
        assertEquals(path, "{given} {given2} {surname} {suffix}", value2);

        String expected = "〖Sinbad〗〖Irene Adler〗〖John Hamish Watson〗〖Ada Cornelia Eva Sophia van den Wolf M.D. Ph.D.〗";
        String value = enWritable.getStringValue(path);

        checkExampleGenerator(exampleGenerator, path, value, expected);

        // Then change one of the sample names to make sure it alters the example correctly

        String namePath = checkPath("//ldml/personNames/sampleName[@item=\"givenSurnameOnly\"]/nameField[@type=\"given\"]");
        String value3 = enWritable.getStringValue(namePath);
        assertEquals(namePath, "Irene", value3); // check that English is as expected

        enWritable.add(namePath, "IRENE");
        exampleGenerator.updateCache(namePath);

        String expected2 =  "〖Sinbad〗〖IRENE Adler〗〖John Hamish Watson〗〖Ada Cornelia Eva Sophia van den Wolf M.D. Ph.D.〗";
        checkExampleGenerator(exampleGenerator, path, value, expected2);
    }

    private String checkPath(String path) {
        assertEquals("Path is in canonical form", XPathParts.getFrozenInstance(path).toString(), path);
        return path;
    }

    public void TestFormatAll() {
        PersonNameFormatter personNameFormatter = ENGLISH_NAME_FORMATTER;
        StringBuilder sb = DEBUG && isVerbose() ? new StringBuilder() : null;

        // Cycle through parameter combinations, check for exceptions even if locale has no data

        for (FormatParameters parameters : FormatParameters.all()) {
            assertNotNull(SampleType.full + " + " + parameters.toLabel(), personNameFormatter.format(ENGLISH_SAMPLES.get(SampleType.full), parameters));
            assertNotNull(SampleType.given12Surname + " + " + parameters.toLabel(), personNameFormatter.format(ENGLISH_SAMPLES.get(SampleType.given12Surname), parameters));
            Collection<NamePattern> nps = personNameFormatter.getBestMatchSet(parameters);
            if (sb != null) {
                for (NamePattern np : nps) {
                    sb.append(parameters.getOrder()
                        + "\t" + parameters.getLength()
                        + "\t" + parameters.getUsage()
                        + "\t" + parameters.getFormality()
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
            Set<Usage> usages = parameterMatcher.getUsage();

            // TODO Mark Look at whether it would be cleaner to replace empty values by ALL on building

            Set<Order> resolvedOrders = orders.isEmpty() ? Order.ALL : orders;
            Set<Usage> resolvedUsages = usages.isEmpty() ? Usage.ALL : usages;

            Set<Field> fields = namePattern.getFields();

            boolean givenAndSurname = (fields.contains(Field.given) || fields.contains(Field.given2))
                && (fields.contains(Field.surname) || fields.contains(Field.surname2));

            boolean commaRequired = givenAndSurname
                && resolvedOrders.contains(Order.sorting)
                && !resolvedUsages.contains(Usage.monogram);

            boolean hasComma = namePattern.firstLiteralContaining(",") != null;

            if (!assertEquals("Comma right?\t" + parameterMatcher + " ➡︎ " + namePattern + "\t", commaRequired, hasComma)) {
                if (!messageShown) {
                    System.out.println("\t\tNOTE: In English, comma is required IFF the pattern has both given and surname, "
                        + "and order has sorting, and usage is not monogram");
                    messageShown = true;
                }
            }
        }
    }

    public void TestCheckPersonNames() {
        Map<SampleType, SimpleNameObject> names = PersonNameFormatter.loadSampleNames(ENGLISH);
        assertEquals("REQUIRED contains all sampleTypes", SampleType.ALL, CheckPersonNames.REQUIRED.keySet());
        for (SampleType sampleType : SampleType.ALL) {
            assertTrue(sampleType + " doesn't have conflicts", Collections.disjoint(
                CheckPersonNames.REQUIRED.get(sampleType),
                CheckPersonNames.REQUIRED_EMPTY.get(sampleType)));
        }
    }



    public void TestFallbackFormatter() {
        FormatParameters testFormatParameters = new FormatParameters(Order.givenFirst, Length.short_name, Usage.referring, Formality.formal);
        final FallbackFormatter fallbackInfo = ENGLISH_NAME_FORMATTER.getFallbackInfo();
        for (Modifier m : Modifier.ALL) {
            String actual = fallbackInfo.applyModifierFallbacks(testFormatParameters, ImmutableSet.of(m), "van Berk");
            String expected;
            switch(m) {
            case allCaps:
                expected = "VAN BERK"; break;
            case initial:
                expected = "v. B."; break;
            case initialCap:
                expected = "Van Berk"; break;
            case monogram:
                expected = "v"; break;
            case prefix:
                expected = null; break;
            case informal:
                expected = "van Berk"; break;
            case core:
                expected = "van Berk"; break; // TODO fix core
            default: throw new IllegalArgumentException("Need to add modifier test for " + m);
            }
            assertEquals(m.toString(), expected, actual);
        }
    }

    public void TestInconsistentModifiers() {
        String[][] expectedFailures = {
            {"allCaps", "initialCap", "Inconsistent modifiers: [allCaps, initialCap]"},
            {"initial", "monogram", "Inconsistent modifiers: [initial, monogram]"},
            {"prefix", "core","Inconsistent modifiers: [core, prefix]"},
        };
        for (Modifier first : Modifier.ALL) {
            for (Modifier second: Modifier.ALL) {
                if (first.compareTo(second) > 0) {
                    continue;
                }
                String expected = null;
                if (first.compareTo(second) == 0) {
                    expected = "Duplicate modifiers: " + first;
                }
                for (String[] row : expectedFailures) {
                    if (first ==  Modifier.valueOf(row[0])
                        && second ==  Modifier.valueOf(row[1])
                        ) {
                        expected = row[2];
                    }
                }

                final ImmutableList<Modifier> test = ImmutableList.of(first, second);
                Output<String> check = new Output<>();
                Modifier.getCleanSet(test, check);
                assertEquals("Modifier set consistent " + test, expected, check.value);
            }
        }
    }

    public void TestInconsistentNameValues() {
        String[][] expectedFailures = {
            {"van", "Berg", "van Wolf", "-core value and -prefix value are inconsistent with plain value"},
            {"van", null, "van Berg", "cannot have -prefix without -core"},
            {"van", null, "van Wolf", "cannot have -prefix without -core"},
            {"van", null, null, "cannot have -prefix without -core"},
            {null, "Berg", "van Berg", "There is no -prefix, but there is a -core and plain that are unequal"},
            {null, "Berg", "van Wolf", "There is no -prefix, but there is a -core and plain that are unequal"},
        };

        for (String prefix : Arrays.asList("van", null)) {
            for (String core : Arrays.asList("Berg", null)) {
                for (String plain : Arrays.asList("van Berg", "van Wolf", null)) {
                    String check = Modifier.inconsistentPrefixCorePlainValues(prefix, core, plain);
                    String expected = null;
                    for (String[] row : expectedFailures) {
                        if (Objects.equal(prefix, row[0])
                            && Objects.equal(core, row[1])
                            && Objects.equal(plain, row[2])
                            ) {
                            expected = row[3];
                        }
                    }
                    assertEquals("Name values consistent: prefix=" + prefix + ", core=" + core + ", plain=" + plain, expected, check);
                }
            }
        }
    }
}
