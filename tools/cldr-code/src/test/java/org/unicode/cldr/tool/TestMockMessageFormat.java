package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.unicode.cldr.tool.MockMessageFormat.FunctionVariable;
import org.unicode.cldr.tool.MockMessageFormat.MFContext;

public class TestMockMessageFormat {
    private static final Joiner LINE_JOINER = Joiner.on("\n\t");

    /**
     * Run some simple examples
     *
     * @param args
     */
    public static void main(String[] args) {
        System.out.println(
                "Message Format:\n\t" + LINE_JOINER.join(checkOffsetMessageLines) + "\n");
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
        final MFContext context =
                new MFContext()
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
            MFContext context = new MFContext().addInput(source);
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

    private static void debug(String variableName, MFContext context) {
        if (MockMessageFormat.DEBUG) {
            final FunctionVariable functionVariable = context.get(variableName);
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
