package org.unicode.cldr.unittest;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.tool.ToolConfig;
import org.unicode.cldr.util.personname.PersonNameFormatter;
import org.unicode.cldr.util.personname.PersonNameFormatter.FormatParameters;
import org.unicode.cldr.util.personname.PersonNameFormatter.NameObject;
import org.unicode.cldr.util.personname.PersonNameFormatter.NamePattern;
import org.unicode.cldr.util.personname.PersonNameFormatter.NamePatternData;
import org.unicode.cldr.util.personname.PersonNameFormatter.Order;
import org.unicode.cldr.util.personname.PersonNameFormatter.ParameterMatcher;
import org.unicode.cldr.util.personname.SimpleNameObject;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.ULocale;

public class TestPersonNameFormatter extends TestFmwk{

    public static void main(String[] args) {
        new TestPersonNameFormatter().run(args);
    }

    private final NameObject sampleNameObject1 = new SimpleNameObject(
        "locale=fr, prefix=Mr., given=John, given2-initial=B, given2= Bob, surname=Smith, surname2= Barnes Pascal, suffix=Jr.");

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
            "length=short medium; style=formal; usage=addressing", "{given} {given2-initial}. {surname}",
            "length=short medium; style=formal; usage=addressing", "{given} {surname}",
            "", "{prefix} {given} {given2} {surname} {surname2} {suffix}"
            );

        PersonNameFormatter personNameFormatter = new PersonNameFormatter(namePatternData);

        check(personNameFormatter, sampleNameObject1, "length=short; style=formal; usage=addressing", "John B. Smith");
        check(personNameFormatter, sampleNameObject1, "length=long; style=formal; usage=addressing", "Mr. John Bob Smith Barnes Pascal Jr.");

        checkFormatterData(personNameFormatter);
    }

    public void TestWithCLDR() {
        PersonNameFormatter personNameFormatter = new PersonNameFormatter(ToolConfig.getToolInstance().getEnglish());

        // TODO Rich once the code is fixed to strip empty fields, fix this test

        check(personNameFormatter, sampleNameObject1, "length=short; usage=sorting", "Smith, null");
        check(personNameFormatter, sampleNameObject1, "length=long; style=formal; usage=referring", "John Bob Smith Jr.");

        checkFormatterData(personNameFormatter);
    }

    private void checkFormatterData(PersonNameFormatter personNameFormatter) {
        // check that no exceptions happen
        // sort by the output patterns
        Multimap<String, FormatParameters> values = TreeMultimap.create();
        for (FormatParameters item : FormatParameters.all()) {
            Collection<NamePattern> actual = personNameFormatter.getBestMatchSet(sampleNameObject1, item);
            values.put(actual.toString(), item);
        }

        StringBuilder sb = new StringBuilder("\n");
        int count = 0;
        sb.append("\nEXPANDED:\n");
        for (Entry<String, Collection<FormatParameters>> entry : values.asMap().entrySet()) {
            sb.append(++count + ")\n");
            for (FormatParameters value : entry.getValue()) {
                sb.append("\t" + value + "\n");
            }
            sb.append("\t⇒\t︎" + entry.getKey() + "\n");
        }

        count = 0;
        sb.append("\nCOMPACTED:\n");
        for (Entry<String, Collection<FormatParameters>> entry : values.asMap().entrySet()) {
            sb.append(++count + ")\n");
            Set<ParameterMatcher> compacted = compact(entry.getValue());
            for (ParameterMatcher value : compacted) {
                sb.append("\t︎ " + value + "\n");
            }
            sb.append("\t⇒\t︎" + entry.getKey() + "\n");
        }

        logln(sb.toString());
    }

    private static Set<ParameterMatcher> compact(Collection<FormatParameters> expanded) {
        Set<ParameterMatcher> result = new TreeSet<>();
        for (FormatParameters item : expanded) {
            result.add(new ParameterMatcher(item));
        }
        // try merging each pair
        // if we can merge, then start over from the top
        // look at optimizing later
        main:
            while (true) {
                for (ParameterMatcher item1 : result) {
                    for (ParameterMatcher item2: result) {
                        if (item1 == item2) { // skip ourselves
                            continue;
                        }
                        ParameterMatcher item12 = item1.merge(item2); // merge if possible
                        if (item12 != null) {
                            result.remove(item1);
                            result.remove(item2);
                            result.add(item12);
                            continue main; // retry everything
                        }
                    }
                }
                break;
            }

        // now replace any "complete" items by empty.
        Set<ParameterMatcher> result2 = new TreeSet<>();
        for (ParameterMatcher item1 : result) {
            result2.add(item1.slim());
        }
        return ImmutableSet.copyOf(result2);
    }
}
