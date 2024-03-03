package org.unicode.cldr.tool;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.unicode.cldr.tool.MockMessageFormat.MfContext;
import org.unicode.cldr.tool.MockMessageFormat.MfResolvedVariable;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;

public class TestMockMessageFormat {
    private static final Joiner JOINER_EMPTY = Joiner.on("");
    private static final Joiner JOINER_LF_TAB = Joiner.on("\n\t");

    /**
     * Run some simple examples
     *
     * @param args
     */
    public static void main(String[] args) {
        testAgainstMf1();
        if (true) return;
        System.out.println(
                "Message Format:\n\t" + JOINER_LF_TAB.join(checkOffsetMessageLines) + "\n");
        checkOffset("Jim", 0, "", "");
        checkOffset("Sarah", 1, "Water Polo", "Ping Pong");
        checkOffset("Tom", 2, "Baseball", "Billiards");
        checkOffset("John", 3, "Football", "Chess");
        checkOffset("Jane", 4, "Lacrosse", "Cycling");
        System.out.println();
        checkParsing();
        //        System.out.println(checkFormat(Locale.forLanguageTag("en-US"), 1, "John", "188
        // meter"));
        //        System.out.println(checkFormat(Locale.forLanguageTag("en"), 1, "Sarah", "1100
        // meter"));
        //        System.out.println(checkFormat(Locale.forLanguageTag("de"), 3.456, "John", "188
        // meter"));
        //        System.out.println(checkFormat(Locale.forLanguageTag("fr"), 0, "John", "188
        // meter"));
    }

    static final List<String> checkOffsetMessageLines =
            List.of( //
                    ".input {$count :number u:offset=2}",
                    ".match {$count}",
                    "0 {{{$name :string} doesn’t like any sports.}}",
                    "1 {{{$name :string} likes one sport, {$sport1 :string}.}}",
                    "2 {{{$name :string} likes a pair of sports, {$sport1 :string} and {$sport1 :string}.}}",
                    "one {{{$name :string} likes {$sport1 :string}, {$sport1 :string}, and {$count} other.}}",
                    "* {{{$name :string} likes {$sport1 :string}, {$sport1 :string}, and {$count} others.}}");

    private static void checkOffset(String name, int input, String sport1, String sport2) {
        final MfContext context =
                new MfContext()
                        .addInput("$locale", Locale.forLanguageTag("ar-u-nu-arab"))
                        .addInput("$name", name)
                        .addInput("$count", input)
                        .addInput("$sport1", sport1)
                        .addInput("$sport2", sport2);
        MockMessageFormat mf = new MockMessageFormat();
        mf.add(checkOffsetMessageLines);
        System.out.println("Input parameters:\t" + context.inputParameters);
        String actual = mf.format(context);
        System.out.println("Result:\t“" + actual + "”\n");
        //            Multimap<Integer, String> scores = LinkedHashMultimap.create();
        //            for (String key : Arrays.asList("0", "1", "one", "other")) {
        //                int matchScore = temp.match(context, key);
        //                scores.put(matchScore, key);
        //            }
        //            for (Entry<Integer, Collection<String>> entry : scores.asMap().entrySet()) {
        //                System.out.println(
        //                        "match score= " + entry.getKey() + ", matchValues=" +
        // entry.getValue());
        //            }
    }

    public static void checkParsing() {
        System.out.println("Details\n");
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
            String result =
                    actual == null ? "Fail" : expected.equals(actual.toString()) ? "OK" : "Fail";
            System.out.println(
                    String.format(
                            "%s %s\n\texpected:\t%s,\n\tactual:  \t%s\n\tvariables:\t%s",
                            result, source, expected, actual, mf.variables.keySet()));
        }
        mf.freeze();

        System.out.println(
                String.format(
                        "Stats:\n\trequiredInput:\t%s\n\trequiredVariables:\t%s\n\tunused:\t%s",
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
                ImmutableMap.of("$locale", Locale.ENGLISH, "$var", 1234, "$name", "John"),
                "There are +1,234 books for JOHN."
            },
            {
                ImmutableMap.of("$locale", Locale.GERMAN, "$var", 1234, "$name", "Hans"),
                "There are +1.234 books for HANS."
            },
        };
        for (Object[] test : Arrays.asList(tests2)) {
            Map<String, Object> source = (Map<String, Object>) test[0];
            String expected = test[1].toString();
            MfContext context = new MfContext().addInput(source);
            String actual = mf.format(context);
            System.out.println(
                    String.format(
                            "%s\t %s\n\texpected:\t%s\n\tactual:\t%s",
                            Objects.equal(expected, actual) ? "OK" : "Fail",
                            source,
                            expected,
                            actual));
        }
    }

    public static void testAgainstMf1() {
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
        MessageFormat mf1 = new MessageFormat(JOINER_EMPTY.join(mf1Pattern.stream().map(x -> x.trim()).collect(Collectors.toUnmodifiableList())));

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

        System.out.println("MF2 Pattern:\n\t" + JOINER_LF_TAB.join(mf1Pattern));
        System.out.println("MF1 Pattern:\n\t" + JOINER_LF_TAB.join(mf2Pattern));

        for (int num_guests : Arrays.asList(0, 1, 2, 3)) {
            Map<String, Object> inputParameters1 = Map.of("host", "Sarah", "gender_of_host", "female", "guest", "Mike", "num_guests", num_guests);
            String result1 = mf1.format(inputParameters1);
            Map<String, Object> inputParameters2 = Map.of("$host", "Sarah", "$gender_of_host", "female", "$guest", "Mike", "$num_guests", num_guests);
            String result2 = mf2.format(new MfContext().addInput(inputParameters2));
            System.out.println(String.format("%s with input: %s\n\tMF1: %s\n\tMF2: %s", Objects.equal(result1, result2) ? "OK" : "Fail", inputParameters2, result1, result2));
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
