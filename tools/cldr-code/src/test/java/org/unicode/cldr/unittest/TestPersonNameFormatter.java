package org.unicode.cldr.unittest;

import java.util.Collection;
import java.util.Map.Entry;

import org.unicode.cldr.tool.ToolConfig;
import org.unicode.cldr.util.personname.PersonNameFormatter;
import org.unicode.cldr.util.personname.PersonNameFormatter.FormatParameters;
import org.unicode.cldr.util.personname.PersonNameFormatter.NameObject;
import org.unicode.cldr.util.personname.PersonNameFormatter.NamePattern;
import org.unicode.cldr.util.personname.PersonNameFormatter.NamePatternData;
import org.unicode.cldr.util.personname.PersonNameFormatter.Order;
import org.unicode.cldr.util.personname.SimpleNameObject;

import com.google.common.collect.ImmutableMap;
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
    }

    public void TestWithCLDR() {
        PersonNameFormatter personNameFormatter = new PersonNameFormatter(ToolConfig.getToolInstance().getEnglish());

        // TODO Rich once the code is fixed to strip empty fields, fix this test

        check(personNameFormatter, sampleNameObject1, "length=short; usage=sorting", "Smith, null");
        check(personNameFormatter, sampleNameObject1, "length=long; style=formal; usage=referring", "John Bob Smith Jr.");

        // check that no exceptions happen
        Multimap<String, FormatParameters> values = TreeMultimap.create();
        for (FormatParameters item : FormatParameters.all()) {
            Collection<NamePattern> actual = personNameFormatter.getBestMatchSet(sampleNameObject1, item);
            values.put(actual.toString(), item);
        }
        for (Entry<String, Collection<FormatParameters>> entry : values.asMap().entrySet()) {
            logln("\t" + entry.getKey());
            for (FormatParameters value : entry.getValue()) {
                logln("\t\t" + value);
            }
        }
    }
}
