package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.unicode.cldr.tool.MockMessageFormat.MfContext;
import org.unicode.cldr.tool.MockMessageFormat.MfResolvedVariable;

public class TestMockMessageFormat extends TestFmwk {
    public static final boolean SHOW_EXAMPLES =
            Boolean.valueOf(System.getProperty("SHOW_EXAMPLES", "false"));
    private static final Joiner JOINER_EMPTY = Joiner.on("");
    private static final Joiner JOINER_LF_TAB = Joiner.on("\n\t");

    /**
     * Run some simple examples
     *
     * @param args
     */
    public static void main(String[] args) {
        new TestMockMessageFormat().run(args);
    }

    public void testMeasureUnits() {
        if (SHOW_EXAMPLES) System.out.println();
        {
            final List<String> unitMessageEnglish =
                    List.of( //
                            ".input {$distance :u:measure usage=road}",
                            ".match {$user:gender}",
                            "*\t{{You returned after {$distance}.}}");

            final ImmutableMap<Measure, String> testDataEnglish =
                    ImmutableMap.of(
                            new Measure(12, MeasureUnit.METER),
                            "You returned after 40 feet.",
                            new Measure(123, MeasureUnit.METER),
                            "You returned after 400 feet.",
                            new Measure(1, MeasureUnit.MILE),
                            "You returned after 1 mile.",
                            new Measure(12345, MeasureUnit.METER),
                            "You returned after 7.7 miles.");

            checkData(
                    unitMessageEnglish,
                    "$distance",
                    testDataEnglish,
                    Map.of("$user:locale", Locale.US));
        }
        {
            final List<String> unitMessageFrench =
                    List.of( //
                            ".input {$distance :u:measure usage=road}",
                            ".match {$user:gender}",
                            "feminine\t{{Tu es revenue après {$distance}.}}",
                            "*\t{{Tu es revenu après {$distance}.}}");

            final ImmutableMap<Measure, String> testDataFrenchMale =
                    ImmutableMap.of(
                            new Measure(12, MeasureUnit.METER),
                            "Tu es revenu après 10 mètres.",
                            new Measure(123, MeasureUnit.METER),
                            "Tu es revenu après 120 mètres.",
                            new Measure(1, MeasureUnit.MILE),
                            "Tu es revenu après 1,6 kilomètre.",
                            new Measure(12345, MeasureUnit.METER),
                            "Tu es revenu après 12 kilomètres.");
            final ImmutableMap<Measure, String> testDataFrenchFemale =
                    ImmutableMap.of(
                            new Measure(10, MeasureUnit.METER),
                            "Tu es revenue après 10 mètres.",
                            new Measure(123, MeasureUnit.METER),
                            "Tu es revenue après 120 mètres.",
                            new Measure(1, MeasureUnit.MILE),
                            "Tu es revenue après 1,6 kilomètre.",
                            new Measure(12345, MeasureUnit.METER),
                            "Tu es revenue après 12 kilomètres.");

            checkData(
                    unitMessageFrench,
                    "$distance",
                    testDataFrenchMale,
                    Map.of("$user:gender", "masculine", "$user:locale", Locale.FRENCH));
            checkData(
                    unitMessageFrench,
                    "$distance",
                    testDataFrenchFemale,
                    Map.of("$user:gender", "feminine", "$user:locale", Locale.FRENCH));
        }
        //      final List<String> unitMessageSlovenian =
        //      List.of( //
        //          ".match {$count :u:measure usage=road}",
        //  "one {{Od cilja ste oddaljeni 1 kilometer}}",
        //  "Od cilja ste oddaljeni 2 kilometra.",
        //  "Od cilja ste oddaljeni 3 kilometre.",
        //  "Od cilja ste oddaljeni 5 kilometrov.",
        //  "Od cilja ste oddaljeni 101 kilometer."
        //  );
        //  final ImmutableMap<Measure, String> testDataSlovenian =
        //      ImmutableMap.of(
        //              new Measure(0, MeasureUnit.METER),
        //              "",
        //              new Measure(0, MeasureUnit.METER),
        //              "",
        //              new Measure(0, MeasureUnit.METER),
        //              "",
        //              new Measure(0, MeasureUnit.KILOMETER),
        //              "");

    }

    public void testChoice() {
        if (SHOW_EXAMPLES) System.out.println();
        // ICU example:
        // "-1#is negative| 0#is zero or fraction | 1#is one |1.0<is 1+ |2#is two |2<is more than
        // 2."
        // Example is taken from the ICU user guide, and modified for ranges

        final List<String> choiceMessageLines =
                List.of( //
                        ".match {$count :number u:choice=true}",
                        "[,0)\t{{{$count} is negative}}",
                        "[0,1)\t{{{$count} is zero or fraction}}",
                        "1\t{{{$count} is one}}",
                        "[1,2)\t{{{$count} is 1+}}",
                        "2\t{{{$count} is two}}",
                        "(2,]\t{{{$count} is more than 2.}}",
                        "* {{{$count} is not a number.}}");

        // Example is taken from the ICU user guide, and modified for ranges
        final ImmutableMap<Double, String> testData =
                ImmutableMap.of(
                        Double.NEGATIVE_INFINITY,
                        "-Infinity is negative",
                        -1.0,
                        "-1.0 is negative",
                        0.0,
                        "0.0 is zero or fraction",
                        0.9,
                        "0.9 is zero or fraction",
                        1.0,
                        "1.0 is one",
                        1.5,
                        "1.5 is 1+",
                        2.0,
                        "2.0 is two",
                        2.1,
                        "2.1 is more than 2.",
                        Double.NaN,
                        "NaN is not a number.",
                        Double.POSITIVE_INFINITY,
                        "Infinity is more than 2.");

        checkData(choiceMessageLines, "$count", testData, Map.of());
    }

    public <T> void checkData(
            final List<String> messageLines,
            String matchVariable,
            final ImmutableMap<T, String> testData,
            Map<String, Object> otherInput) {
        if (SHOW_EXAMPLES)
            System.out.println("MessageFormat:\n\t" + JOINER_LF_TAB.join(messageLines));
        MockMessageFormat mf = new MockMessageFormat();
        mf.add(messageLines);
        for (Entry<T, String> inputAndExpected : testData.entrySet()) {
            String expected = inputAndExpected.getValue();
            Map<String, Object> input = new LinkedHashMap<>();
            input.put(matchVariable, inputAndExpected.getKey());
            input.putAll(otherInput);
            final MfContext context = new MfContext().addInput(input);
            String actual = mf.format(context);
            assertEquals(input.toString(), expected, actual);
            if (SHOW_EXAMPLES) {
                System.out.println(
                        String.format("Input:\t%s\tResult:\t%s", context.inputParameters, actual));
            }
        }
    }

    static final List<String> checkOffsetMessageLines =
            List.of(
                    ".input {$count :number u:offset=2}",
                    ".match {$count}",
                    "0 {{{$name :string} doesn’t like any sports.}}",
                    "1 {{{$name :string} likes one sport, {$sport1 :string}.}}",
                    "2 {{{$name :string} likes a pair of sports, {$sport1 :string} and {$sport1 :string}.}}",
                    "one {{{$name :string} likes {$sport1 :string}, {$sport1 :string}, and {$count} other.}}",
                    "* {{{$name :string} likes {$sport1 :string}, {$sport1 :string}, and {$count} others.}}");

    public void testOffset() {
        if (SHOW_EXAMPLES) System.out.println();
        if (SHOW_EXAMPLES)
            System.out.println(
                    "Message Format:\n\t" + JOINER_LF_TAB.join(checkOffsetMessageLines) + "\n");
        checkOffset("John likes Football, Football, and ١ other.", "John", 3, "Football", "Chess");

        checkOffset("Jim doesn’t like any sports.", "Jim", 0, "", "");
        checkOffset("Sarah likes one sport, Water Polo.", "Sarah", 1, "Water Polo", "");
        checkOffset(
                "Tom likes a pair of sports, Baseball and Baseball.",
                "Tom",
                2,
                "Baseball",
                "Billiards");
        checkOffset("John likes Football, Football, and ١ other.", "John", 3, "Football", "Chess");
        checkOffset(
                "Jane likes Lacrosse, Lacrosse, and ٢ others.", "Jane", 4, "Lacrosse", "Cycling");
    }

    private void checkOffset(
            String expected, String name, int input, String sport1, String sport2) {
        final MfContext context =
                new MfContext()
                        .addInput("$user:locale", Locale.forLanguageTag("ar-u-nu-arab"))
                        .addInput("$name", name)
                        .addInput("$count", input)
                        .addInput("$sport1", sport1)
                        .addInput("$sport2", sport2);
        MockMessageFormat mf = new MockMessageFormat();
        mf.add(checkOffsetMessageLines);
        String actual = mf.format(context);
        assertEquals(String.format("%s\n\t", context.inputParameters), expected, actual);
        if (SHOW_EXAMPLES) {
            System.out.println(
                    String.format("Input:\t%s\tResult:\t%s", context.inputParameters, actual));
        }
    }

    public void testParsing() {
        if (SHOW_EXAMPLES) System.out.println();
        String[][] tests = {
            {
                ".input {$var :number maxFractionDigits=2 minFractionDigits=1}",
                "[type: input, operandId: $var, function: :number, options: {maxFractionDigits=2, minFractionDigits=1}]"
            },
            {".input {$name :string}", "[type: input, operandId: $name, function: :string]"},
            {".input {$amount :number}", "[type: input, operandId: $amount, function: :number]"},
            {
                ".local $var2 = {$var :number maxFractionDigits=2}",
                "[type: variable, operandId: $var, function: :number, options: {maxFractionDigits=2}]"
            },
            {
                ".input {$distance :u:measure maxFractionDigits=3 usage=road width=long}",
                "[type: input, operandId: $distance, function: :u:measure, options: {maxFractionDigits=3, usage=road, width=long}]"
            },
            {
                ".local $distance2 = {$distance :u:measure maxFractionDigits=2 usage=road width=long}",
                "[type: variable, operandId: $distance, function: :u:measure, options: {maxFractionDigits=2, usage=road, width=long}]"
            },
            {
                ".match {$var2} {$name}",
                "[[type: variable, operandId: $var, function: :number, options: {maxFractionDigits=2}], [type: input, operandId: $name, function: :string]]"
            },
            {
                "0 hi {{There are no books for the {$name}.}}",
                "There are no books for the [type: input, operandId: $name, function: :string]."
            },
            {
                "one hi {{There is {$var2} book for {$name}.}}",
                "There is [type: variable, operandId: $var, function: :number, options: {maxFractionDigits=2}] book for [type: input, operandId: $name, function: :string]."
            },
            {
                "* * {{There are {$var2 :number signDisplay=always} books for {$name :string u:casing=upper}.}}",
                "There are [type: variable, operandId: $var2, function: :number, options: {signDisplay=always}] books for [type: variable, operandId: $name, function: :string, options: {u:casing=upper}]."
            },
        };
        MockMessageFormat mf = new MockMessageFormat();

        for (String[] test : Arrays.asList(tests)) {
            String source = test[0];
            String expected = test[1];
            Object actual = null;
            try {
                actual = mf.dumbParseInput(source);
            } catch (Exception e) {
                actual = e.getMessage();
            }
            assertEquals(
                    String.format("%s\n\t%s\n\t", source, mf.variables.keySet()),
                    expected,
                    actual.toString());
        }
        mf.freeze();

        if (SHOW_EXAMPLES)
            System.out.println(
                    String.format(
                            "\n\tStats:\n\trequiredInput:\t%s\n\trequiredVariables:\t%s\n\tunused:\t%s",
                            mf.requiredInput, mf.requiredVariables, mf.unused));
        Map<String, Object> inputParameters =
                Map.of(
                        "$var",
                        3,
                        "$name",
                        "John",
                        "$amount",
                        3,
                        "$distance",
                        new Measure(1.88, MeasureUnit.METER));
        Object[][] tests2 = {
            {
                ImmutableMap.of("$user:locale", Locale.ENGLISH, "$var", 1234, "$name", "John"),
                "There are +1,234 books for JOHN."
            },
            {
                ImmutableMap.of("$user:locale", Locale.GERMAN, "$var", 1234, "$name", "Hans"),
                "There are +1.234 books for HANS."
            },
        };
        for (Object[] test : Arrays.asList(tests2)) {
            Map<String, Object> source = (Map<String, Object>) test[0];
            String expected = test[1].toString();
            MfContext context = new MfContext().addInput(source);
            String actual = mf.format(context);

            assertEquals(String.format("%s\n\t", source), expected, actual);
            if (SHOW_EXAMPLES) {
                System.out.println(
                        String.format("Input:\t%s\tResult:\t%s", context.inputParameters, actual));
            }
        }
    }

    public void testAgainstMf1() {
        if (SHOW_EXAMPLES) System.out.println();
        // spotless:off
        final List<String> mf1Pattern = List.of(
         "{gender_of_host, select, ",
           " female {",
            "  {num_guests, plural, offset:1 ",
              "   =0 {{host} does not give a party.}",
              "   =1 {{host} invites {guest} to her party.}",
              "   =2 {{host} invites {guest} and one other person to her party.}",
              "   other {{host} invites {guest} and # other people to her party.}}}",
           " male {",
             "  {num_guests, plural, offset:1 ",
               "   =0 {{host} does not give a party.}",
               "   =1 {{host} invites {guest} to his party.}",
              "   =2 {{host} invites {guest} and one other person to his party.}",
              "   other {{host} invites {guest} and # other people to his party.}}}",
           " other {",
             "  {num_guests, plural, offset:1 ",
               "   =0 {{host} does not give a party.}",
               "   =1 {{host} invites {guest} to their party.}",
              "   =2 {{host} invites {guest} and one other person to their party.}",
               "   other {{host} invites {guest} and # other people to their party.}}}}");
        // spotless:on
        MessageFormat mf1 =
                new MessageFormat(
                        JOINER_EMPTY.join(
                                mf1Pattern.stream()
                                        .map(x -> x.trim())
                                        .collect(Collectors.toUnmodifiableList())));

        // spotless:off
        final List<String> mf2Pattern = List.of(
            ".input {$num_guests :number u:offset=1}", // must be separate line
            ".match {$gender_of_host}{$num_guests}", // {$host}, {$guest} not matched
                " female 0 {{{$host} does not give a party.}}",
                " female 1 {{{$host} invites {$guest} to her party.}}",
                " female 2 {{{$host} invites {$guest} and one other person to her party.}}",
                " female * {{{$host} invites {$guest} and {$num_guests} other people to her party.}}",
                " male 0 {{{$host} does not give a party.}}",
                " male 1 {{{$host} invites {$guest} to his party.}}",
                " male 2 {{{$host} invites {$guest} and one other person to his party.}}",
                " male * {{{$host} invites {$guest} and {$num_guests} other people to his party.}}",
                " * 0 {{{$host} does not give a party.}}",
                " * 1 {{{$host} invites {$guest} to their party.}}",
                " * 2 {{{$host} invites {$guest} and one other person to their party.}}",
                " * * {{{$host} invites {$guest} and {$num_guests} other people to their party.}}");
        // spotless:on

        MockMessageFormat mf2 = new MockMessageFormat().add(mf2Pattern).freeze();

        if (SHOW_EXAMPLES) System.out.println("MF1 Pattern:\n\t" + JOINER_LF_TAB.join(mf1Pattern));
        if (SHOW_EXAMPLES) System.out.println("MF2 Pattern:\n\t" + JOINER_LF_TAB.join(mf2Pattern));

        for (int num_guests : Arrays.asList(0, 1, 2, 3)) {
            Map<String, Object> inputParameters1 =
                    Map.of(
                            "host",
                            "Sarah",
                            "gender_of_host",
                            "female",
                            "guest",
                            "Mike",
                            "num_guests",
                            num_guests);
            String expected = mf1.format(inputParameters1);
            Map<String, Object> inputParameters2 =
                    Map.of(
                            "$host",
                            "Sarah",
                            "$gender_of_host",
                            "female",
                            "$guest",
                            "Mike",
                            "$num_guests",
                            num_guests);
            final MfContext context = new MfContext().addInput(inputParameters2);
            String actual = mf2.format(context);
            assertEquals(String.format("%s\n\t", inputParameters2), expected, actual);
            if (SHOW_EXAMPLES) {
                System.out.println(
                        String.format("Input:\t%s\tResult:\t%s", context.inputParameters, actual));
            }
        }
    }

    private static void debug(String variableName, MfContext context) {
        if (MockMessageFormat.DEBUG) {
            final MfResolvedVariable functionVariable = context.get(variableName);
            final String formatted = functionVariable.format(context);
            System.out.println(
                    "# "
                            + variableName
                            + " 🡆 «"
                            + formatted
                            + "» "
                            + functionVariable.getOptions());
        }
    }
}
