package org.unicode.cldr.unittest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.unicode.cldr.test.CheckAccessor;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckPersonNames;
import org.unicode.cldr.test.CheckPlaceHolders;
import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRTransforms;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.personname.PersonNameFormatter;
import org.unicode.cldr.util.personname.PersonNameFormatter.FallbackFormatter;
import org.unicode.cldr.util.personname.PersonNameFormatter.Field;
import org.unicode.cldr.util.personname.PersonNameFormatter.Formality;
import org.unicode.cldr.util.personname.PersonNameFormatter.FormatParameters;
import org.unicode.cldr.util.personname.PersonNameFormatter.Length;
import org.unicode.cldr.util.personname.PersonNameFormatter.ModifiedField;
import org.unicode.cldr.util.personname.PersonNameFormatter.Modifier;
import org.unicode.cldr.util.personname.PersonNameFormatter.NameObject;
import org.unicode.cldr.util.personname.PersonNameFormatter.NamePattern;
import org.unicode.cldr.util.personname.PersonNameFormatter.NamePatternData;
import org.unicode.cldr.util.personname.PersonNameFormatter.Order;
import org.unicode.cldr.util.personname.PersonNameFormatter.SampleType;
import org.unicode.cldr.util.personname.PersonNameFormatter.Usage;
import org.unicode.cldr.util.personname.SimpleNameObject;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

public class TestPersonNameFormatter extends TestFmwk{

    public static final boolean DEBUG = System.getProperty("TestPersonNameFormatter.DEBUG") != null;
    public static final boolean SHOW = System.getProperty("TestPersonNameFormatter.SHOW") != null;

    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    final FallbackFormatter FALLBACK_FORMATTER = new FallbackFormatter(ULocale.ENGLISH, "{0}*", "{0} {1}", null, false);
    final CLDRFile ENGLISH = CONFIG.getEnglish();
    final PersonNameFormatter ENGLISH_NAME_FORMATTER = new PersonNameFormatter(ENGLISH);
    final Map<SampleType, SimpleNameObject> ENGLISH_SAMPLES = PersonNameFormatter.loadSampleNames(ENGLISH);
    final Factory factory = CONFIG.getCldrFactory();
    final CLDRFile jaCldrFile = factory.make("ja", true);

    public static void main(String[] args) {
        new TestPersonNameFormatter().run(args);
    }

    private final NameObject sampleNameObject1 = SimpleNameObject.from(
        "locale=fr, title=Dr., given=John, given2-initial=B., given2= Bob, surname=Smith, surname2= Barnes Pascal, generation=Jr, credentials=MD");
    private final NameObject sampleNameObject2 = SimpleNameObject.from(
        "locale=fr, title=Dr., given=John, surname=Smith, surname2= Barnes Pascal, generation=Jr, credentials=MD");
    private final NameObject sampleNameObject3 = SimpleNameObject.from(
        "locale=fr, title=Dr., given=John Bob, surname=Smith, surname2= Barnes Pascal, generation=Jr, credentials=MD");
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
            "length=short; usage=addressing; formality=formal", "{given} {given2-initial} {surname}",
            "length=medium; usage=addressing; formality=formal", "{given} {given2-initial} {surname}",
            "length=medium; usage=addressing; formality=formal", "{given} {surname}",
            "length=medium; usage=addressing; formality=formal", "{given} {surname}",
            "length=long; usage=monogram; formality=formal", "{given-initial}{surname-initial}",
            "order=givenFirst", "{title} {given} {given2} {surname} {surname2} {credentials}",
            "order=surnameFirst", "{surname} {surname2} {title} {given} {given2} {credentials}",
            "order=sorting", "{surname} {surname2}, {title} {given} {given2} {credentials}");

        PersonNameFormatter personNameFormatter = new PersonNameFormatter(namePatternData, FALLBACK_FORMATTER);

        check(personNameFormatter, sampleNameObjectPrefixCore, "length=short; usage=addressing; formality=formal", "Mary van der Beck");

        check(personNameFormatter, sampleNameObject1, "length=short; usage=addressing; formality=formal", "John B. Smith");
        check(personNameFormatter, sampleNameObject2, "length=short; usage=addressing; formality=formal", "John Smith");
        check(personNameFormatter, sampleNameObject1, "length=long; usage=addressing; formality=formal", "Dr. John Bob Smith Barnes Pascal MD");
        check(personNameFormatter, sampleNameObject3, "length=long; usage=monogram; formality=formal", "J* B*S*"); // TODO This is wrong
        check(personNameFormatter, sampleNameObject4, "order=surnameFirst; length=short; usage=addressing; formality=formal", "ABE Shinzō");
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
        check(ENGLISH_NAME_FORMATTER, sampleNameObject1, "length=long; usage=referring; formality=formal", "Dr. John Bob Smith, MD");

//        checkFormatterData(ENGLISH_NAME_FORMATTER);
    }

    public static final Joiner JOIN_SPACE = Joiner.on(' ');


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

        // test just one example for FormatParameters
        FormatParameters test = new FormatParameters(Order.sorting, Length.medium, Usage.addressing, Formality.informal);
        assertEquals("label test", "sorting-medium-addressing-informal",
            test.toLabel());
    }

    public void TestLiteralTextElision() {
        ImmutableMap<ULocale, Order> localeToOrder = ImmutableMap.of(); // don't worry about using the order from the locale right now.

        NamePatternData namePatternData = new NamePatternData(
            localeToOrder,
            "order=givenFirst", "1{title}1 2{given}2 3{given2}3 4{surname}4 5{surname2}5 6{credentials}6");

        PersonNameFormatter personNameFormatter = new PersonNameFormatter(namePatternData, FALLBACK_FORMATTER);

        check(personNameFormatter,
            SimpleNameObject.from(
                "locale=en, title=Mr., given=John, given2= Bob, surname=Smith, surname2= Barnes Pascal, generation=Jr, credentials=MD"),
            "length=short; usage=addressing; formality=formal",
            "1Mr.1 2John2 3Bob3 4Smith4 5Barnes Pascal5 6MD6"
            );

        check(personNameFormatter,
            SimpleNameObject.from(
                "locale=en, given2= Bob, surname=Smith, surname2= Barnes Pascal, generation=Jr, credentials=MD"),
            "length=short; usage=addressing; formality=formal",
            "Bob3 4Smith4 5Barnes Pascal5 6MD6"
            );

        check(personNameFormatter,
            SimpleNameObject.from(
                "locale=en, title=Mr., given=John, given2= Bob, surname=Smith"),
            "length=short; usage=addressing; formality=formal",
            "1Mr.1 2John2 3Bob3 4Smith"
            );

        check(personNameFormatter,
            SimpleNameObject.from(
                "locale=en, title=Mr., surname=Smith, surname2= Barnes Pascal, generation=Jr, credentials=MD"),
            "length=short; usage=addressing; formality=formal",
            "1Mr.1 4Smith4 5Barnes Pascal5 6MD6"
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
            "order=givenFirst",     "¹{title}₁²{given}₂³{given2}₃⁴{surname}₄⁵{surname2}₅⁶{credentials}₆",
            "order=surnameFirst",   "¹{surname-allCaps}₁²{surname2}₂³{title}₃⁴{given}₄⁵{given2}₅⁶{credentials}₆",
            "order=sorting",        "¹{surname}₁²{surname2},³{title}₃⁴{given}₄⁵{given2}₅⁶{credentials}₆");

        PersonNameFormatter personNameFormatter2 = new PersonNameFormatter(namePatternData2, FALLBACK_FORMATTER);

        check(personNameFormatter2, sampleNameObject5, "order=givenFirst", "Mary₂³₃⁴Smith"); // TODO Rich, make ₁²Mary₂³₃⁴Smith₄ or we change desc.
        check(personNameFormatter2, sampleNameObject5, "order=surnameFirst", "¹SMITH₁²₃⁴Mary"); // similar
        check(personNameFormatter2, sampleNameObject5, "order=sorting", "¹Smith₁²₃⁴Mary"); //  TODO RICH should be ¹Smith,³₃⁴Mary₄
    }

    public void TestLiteralTextElisionSpaces() {

        // Also used to generate examples in the user guide
        if (SHOW) {
            logln("Patterns for User Guide:");
            final String pattern = "¹{title}₁ ²{given}₂ ³{given2}₃ ⁴{surname}₄";
            final String patternNoSpaces = "¹{title}₁²{given}₂³{given2}₃⁴{surname}₄";
            System.out.println(pattern.replace(" ", "⌴"));
            System.out.println(patternNoSpaces);
            System.out.println("[" + pattern.replace(" ", "]⌴[") + "]");
            System.out.println("[" + patternNoSpaces.replace("}", "}[").replace("{", "]{") + "]");
        }

        ImmutableMap<ULocale, Order> localeToOrder = ImmutableMap.of(); // don't worry about using the order from the locale right now.

        NamePatternData namePatternData2 = new NamePatternData(
            localeToOrder,
            "order=givenFirst",     "¹{title}₁ ²{given}₂ ³{given2}₃ ⁴{surname}₄ ⁵{surname2}₅ ⁶{credentials}₆",
            "order=surnameFirst",   "¹{surname-allCaps}₁ ²{surname2}₂ ₃{title}₃ ⁴{given}₄ ⁵{given2}₅ ⁶{credentials}₆",
            "order=sorting",        "¹{surname}₁ ²{surname2}, ₃{title}₃ ⁴{given}₄ ⁵{given2}₅ ⁶{credentials}₆");

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

//    private <T> Set<T> removeFirst(Set<T> all) {
//        Set<T> result = new LinkedHashSet<>(all);
//        T first = all.iterator().next();
//        result.remove(first);
//        return result;
//    }

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

        String[][] tests = {
            {
                "//ldml/personNames/personName[@order=\"givenFirst\"][@length=\"long\"][@usage=\"referring\"][@formality=\"formal\"]/namePattern",
                "〖Native:〗〖Zendaya〗〖Irene Adler〗〖John Hamish Watson〗〖Ms. Ada Cornelia Eva Sophia Wolf, M.D. Ph.D.〗〖Foreign:〗〖Sinbad〗〖Käthe Müller〗〖Zäzilia Hamish Stöber〗〖Prof. Dr. Ada Cornelia César Martín von Brühl, MD DDS〗"
            },{
                "//ldml/personNames/personName[@order=\"surnameFirst\"][@length=\"long\"][@usage=\"monogram\"][@formality=\"informal\"]/namePattern",
                "〖Native:〗〖Z〗〖AI〗〖WJ〗〖WN〗〖Foreign:〗〖S〗〖MK〗〖SZ〗〖VN〗"
            },{
                "//ldml/personNames/nameOrderLocales[@order=\"givenFirst\"]",
                "〖und = «any other»〗〖en = English〗"
            },{
                "//ldml/personNames/nameOrderLocales[@order=\"surnameFirst\"]",
                "〖ja = Japanese〗〖ko = Korean〗〖vi = Vietnamese〗〖yue = Cantonese〗〖zh = Chinese〗"
            }
        };
        ExampleGenerator exampleGenerator = checkExamples(ENGLISH, tests);

        String[][] jaTests = {
            {
                "//ldml/personNames/personName[@order=\"givenFirst\"][@length=\"long\"][@usage=\"referring\"][@formality=\"formal\"]/namePattern",
                "〖Native:〗〖慎太郎〗〖一郎 安藤〗〖太郎 トーマス 山田〗〖Foreign:〗〖アルベルト・アインシュタイン〗〖英子・ソフィア・内田ドクター〗"
            }
        };
        ExampleGenerator jaExampleGenerator = checkExamples(jaCldrFile, jaTests);


        // next test that the example generator returns non-null for all expected cases

        for (String localeId : Arrays.asList("en")) {
            final CLDRFile cldrFile = factory.make(localeId, true);
            ExampleGenerator exampleGenerator2 = new ExampleGenerator(cldrFile, ENGLISH);
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

    private ExampleGenerator checkExamples(CLDRFile cldrFile, String[][] tests) {
        ExampleGenerator exampleGenerator = new ExampleGenerator(cldrFile, ENGLISH);
        for (String[] test : tests) {
            String path = test[0];
            String value = cldrFile.getStringValue(path);
            String expected = test[1];
            checkExampleGenerator(exampleGenerator, path, value, expected);
        }
        return exampleGenerator;
    }

    private void checkExampleGenerator(ExampleGenerator exampleGenerator, String path, String value, String expected) {
        final String example = exampleGenerator.getExampleHtml(path, value);
        String actual = ExampleGenerator.simplify(example);
        if (!assertEquals("Example for " + value, expected, actual)) {
            int debug = 0;
        }
    }

    public void TestForeignNonSpacingNames() {
        Map<SampleType, SimpleNameObject> names = PersonNameFormatter.loadSampleNames(jaCldrFile);
        SimpleNameObject name = names.get(SampleType.foreignGS);
        assertEquals("albert", "アルベルト", name.getBestValue(ModifiedField.from("given"), new HashSet<>()));
        assertEquals("einstein", "アインシュタイン", name.getBestValue(ModifiedField.from("surname"), new HashSet<>()));
    }

    public void TestExampleDependencies() {
        Factory cldrFactory = CONFIG.getCldrFactory();
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

        ExampleGenerator exampleGenerator = new ExampleGenerator(resolved, ENGLISH);
        String path = checkPath("//ldml/personNames/personName[@order=\"givenFirst\"][@length=\"long\"][@usage=\"referring\"][@formality=\"formal\"]/namePattern");
        String value2 = enWritable.getStringValue(path); // check that English is as expected
        assertEquals(path, "{given} {given2} {surname} {credentials}", value2);

        String expected = "〖Native:〗〖Zendaya〗〖Irene Adler〗〖John Hamish Watson〗〖Ms. Ada Cornelia Eva Sophia Wolf, M.D. Ph.D.〗〖Foreign:〗〖Sinbad〗〖Käthe Müller〗〖Zäzilia Hamish Stöber〗〖Prof. Dr. Ada Cornelia César Martín von Brühl, MD DDS〗";
        String value = enWritable.getStringValue(path);

        checkExampleGenerator(exampleGenerator, path, value, expected);

        // Then change one of the sample names to make sure it alters the example correctly

        String namePath = checkPath("//ldml/personNames/sampleName[@item=\"nativeGS\"]/nameField[@type=\"given\"]");
        String value3 = enWritable.getStringValue(namePath);
        assertEquals(namePath, "Irene", value3); // check that English is as expected

        enWritable.add(namePath, "IRENE");
        exampleGenerator.updateCache(namePath);

        String expected2 =  "〖Native:〗〖Zendaya〗〖Irene Adler〗〖John Hamish Watson〗〖Ms. Ada Cornelia Eva Sophia Wolf, M.D. Ph.D.〗〖Foreign:〗〖Sinbad〗〖Käthe Müller〗〖Zäzilia Hamish Stöber〗〖Prof. Dr. Ada Cornelia César Martín von Brühl, MD DDS〗";
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
            assertNotNull(SampleType.foreignFull + " + " + parameters.toLabel(), personNameFormatter.format(ENGLISH_SAMPLES.get(SampleType.foreignFull), parameters));
            assertNotNull(SampleType.nativeGGS + " + " + parameters.toLabel(), personNameFormatter.format(ENGLISH_SAMPLES.get(SampleType.nativeGGS), parameters));
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
        for (Entry<FormatParameters, NamePattern> matcherAndPattern : ENGLISH_NAME_FORMATTER.getNamePatternData().getMatcherToPatterns().entries()) {
            FormatParameters parameterMatcher = matcherAndPattern.getKey();
            NamePattern namePattern = matcherAndPattern.getValue();

            Set<Field> fields = namePattern.getFields();

            boolean givenAndSurname = (fields.contains(Field.given) || fields.contains(Field.given2))
                && (fields.contains(Field.surname) || fields.contains(Field.surname2));

            boolean commaRequired = givenAndSurname
                && parameterMatcher.matchesOrder(Order.sorting)
                || fields.contains(Field.credentials)
//                && !parameterMatcher.matchesUsage(Usage.monogram)
                ;

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
                    assertEquals("Name values consistent: title=" + prefix + ", core=" + core + ", plain=" + plain, expected, check);
                }
            }
        }
    }

    public void TestCheckForErrorsAndGetLocales() {
        String[][] tests = {
            {"und ja ja", "", "ja und"},
            {"und ja$ ja", "ja$", "ja und"},
            {"unda de ing en_CA", "unda ing", "de en_CA"},
        };
        Set<String> resultSet = new TreeSet<>();

        for (String[] row : tests) {
            String localeList = row[0];
            String expectedErrors = row[1];
            String expectedResults = row[2];
            resultSet.clear();
            Set<String> errors = CheckPlaceHolders.checkForErrorsAndGetLocales(null, localeList, resultSet);
            assertEquals("The error-returns match", expectedErrors, errors == null ? "" : Joiner.on(' ').join(errors));
            assertEquals("The results match", expectedResults, Joiner.on(' ').join(resultSet));
        }
    }

    static class CheckAccessorStub implements CheckAccessor {
        public enum Resolution {resolvedAndUnresolved, onlyResolved}
        final Map<String,String> resolvedMap = new TreeMap<>();
        final Map<String,String> unresolvedMap = new TreeMap<>();
        private String localeID;

        public CheckAccessorStub(String localeID) {
            this.localeID = localeID;
        }

        public CheckAccessorStub put(String path, String value) {
            return put(path, value, Resolution.resolvedAndUnresolved);
        }

        public CheckAccessorStub put(String path, String value, Resolution resolutions) {
            resolvedMap.put(path, value);
            if (resolutions == Resolution.resolvedAndUnresolved) {
                unresolvedMap.put(path, value);
            }
            return this;
        }

        @Override
        public String getStringValue(String path) {
            return resolvedMap.get(path);
        }

        @Override
        public String getUnresolvedStringValue(String path) {
            return unresolvedMap == null ? null : unresolvedMap.get(path);
        }

        @Override
        public String getLocaleID() {
            return localeID;
        }

        @Override
        public CheckCLDR getCause() {
            throw new UnsupportedOperationException("not available in stub");
        }
        @Override
        public String toString() {
            // TODO Auto-generated method stub
            return "{locale=" + localeID + ", resolved: " + resolvedMap  + ", unresolved: " + unresolvedMap + "}";
        }
    }

    private String joinCheckStatus(List<CheckStatus> results) {
        StringBuilder returnValue = new StringBuilder();
        for (CheckStatus result : results) {
            if (returnValue.length() != 0) {
                returnValue.append('|');
            }
            returnValue.append(result.getType() + ": " + result.getMessage());
        }
        return returnValue.toString();
    }

    final String initialSequencePattern = "//ldml/personNames/initialPattern[@type=\"initialSequence\"]";
    final String initialPattern = "//ldml/personNames/initialPattern[@type=\"initial\"]";

    /**
     * Check for overlapping initial patterns
     * */
    public void TestCheckInitials() {
        List<CheckStatus> results = new ArrayList<>();
        String[][] tests = {
            // initial, initialSequence, expected-error due to conflict between them
            {"{0}.", "{0}.{1}.", "Error: The initialSequence pattern must not contain initial pattern literals: «.»"},
            {"{0}:", "{0}.{1}:", "Error: The initialSequence pattern must not contain initial pattern literals: «:»"},
            {"{0}:", "{0}.{1}.", "Warning: Non-space characters are discouraged in the initialSequence pattern: «..»"},
            {"{0}", "{0} {1}", ""},
            {"{0}", "{0}{1}", ""},
        };
        int i = 0;
        for (String[] row : tests) {
            ++i;
            String initial = row[0];
            String initialSequence = row[1];
            String expectedErrors = row[2];
            CheckAccessorStub stub = new CheckAccessorStub("fr")
                .put(initialPattern, initial)
                ;

            results.clear();
            CheckPlaceHolders.checkInitialPattern(stub, initialSequencePattern, initialSequence, results);
            assertEquals(i + ") Matching error returns", expectedErrors, joinCheckStatus(results));
        }
    }

    public void TestCheckForeignSpaceReplacement() {
        List<CheckStatus> results = new ArrayList<>();
        String[][] tests = {
            // value, expected-error
            {" ", ""},
            {"・", ""},
            {"·", ""},
            {"↑↑↑", ""},
            {"∅∅∅", "Error: Invalid choice, must be punctuation or a space: «∅∅∅»"},
            {"ofifo", "Error: Invalid choice, must be punctuation or a space: «ofifo»"},
        };
        CheckAccessorStub stub = new CheckAccessorStub("fr"); // we don't depend on any values
        int i = 0;
        for (String[] row : tests) {
            ++i;
            String value = row[0];
            String expectedErrors = row[1];
            results.clear();
            CheckPlaceHolders.checkForeignSpaceReplacement(stub, value, results);
            assertEquals(i + ") Matching error returns", expectedErrors, joinCheckStatus(results));
        }
    }

    final String givenFirstPath = "//ldml/personNames/nameOrderLocales[@order=\"givenFirst\"]";
    final String surnameFirstPath = "//ldml/personNames/nameOrderLocales[@order=\"surnameFirst\"]";

    public void TestCheckNameOrderLocales() {
        String[][] tests = {
            // givenFirst-locales, surnameFirst-locales, givenFirstValueErrors, surnameFirstValueErrors
            {"und fr", "fr", "Error: Locale codes can occur only once: fr", "Error: Locale codes can occur only once: fr"},
            {"und zzz", "fr", "Error: Invalid locales: zzz", ""},
            {"und $", "fr", "Error: Invalid locales: $", ""},
            {"und fr", "", "", ""},
        };
        List<CheckStatus> results = new ArrayList<>();
        int i = 0;
        for (String[] row : tests) {
            ++i;
            String givenFirst = row[0];
            String surnameFirst = row[1];
            String expectedGivenErrors = row[2];
            String expectedSurnameErrors = row[3];
            CheckAccessorStub stub = new CheckAccessorStub("fr")
                .put(givenFirstPath, givenFirst)
                .put(surnameFirstPath, surnameFirst)
                ;
            results.clear();
            CheckPlaceHolders.checkNameOrder(stub, givenFirstPath, givenFirst, results);
            assertEquals(i + ") Matching error returns", expectedGivenErrors, joinCheckStatus(results));

            results.clear();
            CheckPlaceHolders.checkNameOrder(stub, surnameFirstPath, surnameFirst, results);
            assertEquals(i + ") Matching error returns", expectedSurnameErrors, joinCheckStatus(results));
        }
    }

    final String sampleNamePath = "//ldml/personNames/sampleName[@item=\"full\"]/nameField[@type=\"given\"]";

    public void TestCheckSampleNames() {
        String[][] tests = {
            // sample-name-component, error
            {"zxx", "Error: Illegal name field; zxx is only appropriate for NameOrder locales"},
            {"Fred", ""},
        };
        List<CheckStatus> results = new ArrayList<>();
        XPathParts parts = XPathParts.getFrozenInstance(sampleNamePath);
        int i = 0;
        for (String[] row : tests) {
            ++i;
            String sampleNameComponent = row[0];
            String expectedErrors = row[1];
            CheckAccessorStub stub = new CheckAccessorStub("fr")
                .put(sampleNamePath, sampleNameComponent)
                ;
            results.clear();
            CheckPlaceHolders.checkSampleNames(stub, parts, sampleNameComponent, results);
            assertEquals(i + ") Matching error returns", expectedErrors, joinCheckStatus(results));
        }
    }

    final String sampleMonogramPath = "//ldml/personNames/personName[@order=\"givenFirst\"][@length=\"long\"][@usage=\"monogram\"][@formality=\"formal\"]/namePattern";
    final String sampleNonMonogramPath = "//ldml/personNames/personName[@order=\"givenFirst\"][@length=\"short\"][@usage=\"referring\"][@formality=\"informal\"]/namePattern";

    public void TestCheckPatterns() {
        String[][] tests = {
            // sample-pattern, errorWhenMonogram, errorWhenNonMonogram
            {"{title} {given-monogram-allCaps}",
                "Error: Disallowed when usage=monogram: {title…}",
                "Warning: -monogram is strongly discouraged when usage≠monogram, in {given-allCaps-monogram}"
            },
            {"{given-informal-initial}.",
                "Error: -monogram is required when usage=monogram, in {given-informal-initial}|Warning: “.” is discouraged when usage=monogram, in \"{given-informal-initial}.\"",
                ""
            },
            {"{surname-monogram}",
                "Warning: -allCaps is strongly encouraged with -monogram, in {surname-monogram}",
                "Warning: -monogram is strongly discouraged when usage≠monogram, in {surname-monogram}"
            },
            {"{surname-core-monogram-allCaps}",
                "",
                "Warning: -monogram is strongly discouraged when usage≠monogram, in {surname-allCaps-monogram-core}"
            },
            {"{surname-core-monogram}",
                "Warning: -allCaps is strongly encouraged with -monogram, in {surname-monogram-core}",
                "Warning: -monogram is strongly discouraged when usage≠monogram, in {surname-monogram-core}"
            },
            {"{given}.{surname}",
                "Error: -monogram is required when usage=monogram, in {given}|Warning: “.” is discouraged when usage=monogram, in \"{given}.{surname}\"|Error: -monogram is required when usage=monogram, in {surname}",
                ""
            },
        };
        List<CheckStatus> results = new ArrayList<>();
        XPathParts monogramPathParts = XPathParts.getFrozenInstance(sampleMonogramPath);
        XPathParts nonMonogramPathParts = XPathParts.getFrozenInstance(sampleNonMonogramPath);
        int i = 0;
        for (String[] row : tests) {
            ++i;
            String samplePattern = row[0];
            String expectedMonogramErrors = row[1];
            String expectedNonMonogramErrors = row.length < 3 ? row[1] : row[2];
            CheckAccessorStub stub = new CheckAccessorStub("fr")
                .put(sampleMonogramPath, samplePattern)
                .put(sampleNonMonogramPath, samplePattern)
                ;
            results.clear();
            CheckPlaceHolders.checkPersonNamePatterns(stub, sampleMonogramPath, monogramPathParts, samplePattern, results);
            assertEquals(i + ") monogram, " + samplePattern, expectedMonogramErrors, joinCheckStatus(results));
            results.clear();
            CheckPlaceHolders.checkPersonNamePatterns(stub, sampleNonMonogramPath, nonMonogramPathParts, samplePattern, results);
            assertEquals(i + ") non-monogram, " + samplePattern, expectedNonMonogramErrors, joinCheckStatus(results));
        }
    }

    public void TestAll() {
        for (String locale : StandardCodes.make().getLocaleCoverageLocales(Organization.cldr)) {
            if (Level.MODERN != StandardCodes.make().getLocaleCoverageLevel(Organization.cldr, locale)) {
                continue;
            }
            CLDRFile cldrFile = factory.make(locale, true);
            Map<SampleType, SimpleNameObject> names = PersonNameFormatter.loadSampleNames(cldrFile);
            if (names.isEmpty()) {
                continue;
            }
            PersonNameFormatter formatter = new PersonNameFormatter(cldrFile);
            Multimap<String, FormatParameters> formattedToParameters = TreeMultimap.create();
            for (Entry<SampleType, SimpleNameObject> entry : names.entrySet()) {
                final SampleType sampleType = entry.getKey();
                final SimpleNameObject nameObject = entry.getValue();
                // abbreviated for now
                if (sampleType != SampleType.foreignFull) {
                    continue;
                }
                for (FormatParameters parameters : FormatParameters.allCldr()) {
                    String result = formatter.format(nameObject, parameters);
                    formattedToParameters.put(result, parameters);
                }
                for (Entry<String, Collection<FormatParameters>> entry2 : formattedToParameters.asMap().entrySet()) {
                    final Set<String> shortSet = entry2.getValue().stream().map(x -> x.abbreviated()).collect(Collectors.toSet());
                    logln("\t" + locale
                        + "\t" + sampleType
                        + "\t" + entry2.getKey()
                        + "\t" + shortSet);
                }
            }
        }
    }

    public void TestSupplementalConsistency() {
        Multimap<Order, String> defaultOrderToString = CONFIG.getSupplementalDataInfo().getPersonNameOrder();
        Multimap<Order, String> orderToString = TreeMultimap.create();
        final Set<String> defaultSurnameFirst = ImmutableSet.copyOf(defaultOrderToString.get(Order.surnameFirst));
        for (String locale : factory.getAvailableLanguages()) {
            CLDRFile cldrFile = factory.make(locale, false);
            getValues(cldrFile, locale, Order.givenFirst, orderToString);
            List<String> surnameItems = getValues(cldrFile, locale, Order.surnameFirst, orderToString);
            // Check that a locale doesn't list a non-surname.
            // Might not be an issue, but need to review and make an exception set if ok.
            if (!defaultSurnameFirst.containsAll(surnameItems))
                warnln(defaultSurnameFirst + " ⊉ " + surnameItems);
        }
        assertEquals(Order.givenFirst.toString(), Collections.singletonList("und"), defaultOrderToString.get(Order.givenFirst));
        // If this test fails, it is usally an indication that a new surnameFirst locale has been added,
        // but the supplemental data hasn't been updated.
        assertEquals(Order.surnameFirst.toString(), orderToString.get(Order.surnameFirst), new TreeSet<>(defaultSurnameFirst));
    }

    private static Splitter SPLIT_SPACE = Splitter.on(' ').trimResults().omitEmptyStrings();

    public List<String> getValues(CLDRFile cldrFile, String locale, Order order, Multimap<Order, String> orderToString) {
        String givenFirstLocales = cldrFile.getStringValue("//ldml/personNames/nameOrderLocales[@order=\""
            + order
            + "\"]");
        if (givenFirstLocales != null && !givenFirstLocales.equals("↑↑↑")) {
            List<String> locales = SPLIT_SPACE.splitToList(givenFirstLocales);
            orderToString.putAll(order, locales);
            logln("Checking\t" + locale + "\t" + ENGLISH.getName(locale) + "\t" + order + "\t" + givenFirstLocales);
            return locales;
        }
        return Collections.emptyList();
    }

    static class TransformingNameObject implements NameObject {
        NameObject other;
        StringTransform stringTransform;
        StringTransform titleTransform = Transliterator.getInstance("title");

        TransformingNameObject(NameObject other, StringTransform stringTransform) {
            this.other = other;
            this.stringTransform = stringTransform; // TODO use CLDR
        }
        @Override
        public ULocale getNameLocale() {
            return other.getNameLocale();
        }
        @Override
        public ImmutableMap<ModifiedField, String> getModifiedFieldToValue() {
            throw new IllegalArgumentException("Not needed");
        }
        @Override
        public Set<Field> getAvailableFields() {
            return other.getAvailableFields();
        }
        @Override
        public String getBestValue(ModifiedField modifiedField, Set<Modifier> remainingModifers) {
            String result = other.getBestValue(modifiedField, remainingModifers);
            return result == null ? null : titleTransform.transform(stringTransform.transform(result));
        }
    }

    public void testTransliteratorName() {
        boolean verbose = isVerbose();
        CLDRTransforms.registerCldrTransforms(null, null, null, true);

        FormatParameters parameters = FormatParameters.from("length=long; formality=formal");
        LikelySubtags ls = new LikelySubtags();
        LanguageTagParser ltp = new LanguageTagParser();
        Set<String> missing = new TreeSet<>();

        for (String locale : factory.getAvailableLanguages()) {
            ltp.set(locale);
            if (!ltp.getRegion().isEmpty()) {
                continue;
            }
            String max = ls.maximize(locale);
            final String lang = ltp.set(max).getLanguage();
            final String script = ltp.set(max).getScript();

            Map<SampleType, SimpleNameObject> names = PersonNameFormatter.loadSampleNames(factory.make(locale, true));
            if (names == null || names.isEmpty()) {
                continue;
            }

            boolean isLatin = script.equals("Latn");


            Transliterator t = isLatin ? null : CLDRTransforms.getScriptTransform(script);
            if (t == null && !isLatin) {
                missing.add(script);
            }

            if (verbose) {
                System.out.println();
            }

            for (Entry<SampleType, SimpleNameObject> entry : names.entrySet()) {
                SampleType sampleType = entry.getKey();
                final SimpleNameObject simpleNameObject = entry.getValue();
                String formatted = ENGLISH_NAME_FORMATTER.format(simpleNameObject, parameters);
                String formattedWithTranslit = formatted;
                if (t != null) {
                    TransformingNameObject tno = new TransformingNameObject(simpleNameObject, t);
                    formattedWithTranslit = ENGLISH_NAME_FORMATTER.format(tno, parameters);
                }
                if (verbose) {
                    System.out.println(lang
                    + "\t" + script
                    + "\t" + sampleType
                    + "\t" + formatted
                    + (!formatted.equals(formattedWithTranslit) ? "\t➡︎\t" + formattedWithTranslit : "")
                    );
                }
            }
        }
        assertEquals("Missing scripts: ", Collections.emptySet(), missing);
    }
}
