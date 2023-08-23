package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.text.Collator;
import com.ibm.icu.util.ULocale;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.personname.PersonNameFormatter;
import org.unicode.cldr.util.personname.PersonNameFormatter.Field;
import org.unicode.cldr.util.personname.PersonNameFormatter.Formality;
import org.unicode.cldr.util.personname.PersonNameFormatter.FormatParameters;
import org.unicode.cldr.util.personname.PersonNameFormatter.Length;
import org.unicode.cldr.util.personname.PersonNameFormatter.ModifiedField;
import org.unicode.cldr.util.personname.PersonNameFormatter.Modifier;
import org.unicode.cldr.util.personname.PersonNameFormatter.Order;
import org.unicode.cldr.util.personname.PersonNameFormatter.SampleType;
import org.unicode.cldr.util.personname.PersonNameFormatter.Usage;
import org.unicode.cldr.util.personname.SimpleNameObject;

public class GeneratePersonNameTestData {
    private static final Joiner COMMA_JOINER = Joiner.on(", ");
    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    private static final CLDRFile ENGLISH = CLDR_CONFIG.getEnglish();

    static final Comparator<String> LENGTH_FIRST =
            Comparator.comparingInt(String::length)
                    .reversed()
                    .thenComparing(Collator.getInstance(Locale.ROOT))
                    .thenComparing(Comparator.naturalOrder());

    enum Options {
        none,
        sorting
    }

    public static void main(String[] args) {
        File dir = new File(CLDRPaths.TEST_DATA, "personNameTest");
        Factory factory = CLDR_CONFIG.getCldrFactory();

        Matcher localeMatcher = null;
        if (args.length >= 1) {
            localeMatcher = Pattern.compile(args[0]).matcher("");
        }

        FormatParameters testParameters =
                FormatParameters.from(
                        "order=surnameFirst; length=long; usage=monogram; formality=informal");

        ULocale undLocale = new ULocale("und");

        for (String locale : factory.getAvailable()) {
            if (localeMatcher != null && !localeMatcher.reset(locale).lookingAt()) {
                continue;
            }

            try {
                CLDRFile cldrFile = factory.make(locale, true);
                CLDRFile unresolved = cldrFile.getUnresolved();

                // Check that we have person data
                {
                    String givenOrder =
                            unresolved.getStringValue(
                                    "//ldml/personNames/nameOrderLocales[@order=\"givenFirst\"]");
                    if (givenOrder == null) {
                        continue; // skip unless we have person data
                    }
                    String surnameOrder =
                            unresolved.getStringValue(
                                    "//ldml/personNames/nameOrderLocales[@order=\"surnameFirst\"]");
                    if (surnameOrder == null) {
                        continue; // skip unless we have person data
                    }
                }
                Map<SampleType, SimpleNameObject> names;
                PersonNameFormatter formatter;
                try {
                    names = PersonNameFormatter.loadSampleNames(cldrFile);
                    formatter = new PersonNameFormatter(cldrFile);
                } catch (Exception e) {
                    continue;
                }
                if (names.isEmpty()) {
                    continue;
                }

                // We have to jump through some hoops to get locales corresponding to the order
                // First get the locale for native sample names

                ULocale myLocale = new ULocale(locale);

                Order myOrder = formatter.getOrderFromLocale(myLocale);
                if (myOrder == null) {
                    formatter.getOrderFromLocale(myLocale);
                    throw new IllegalArgumentException("Missing order for: " + locale);
                }

                // Now get the locale for non-native sample names
                // We see if we can get a locale of the other direction
                // Otherwise we pick either English or German

                Order otherOrder =
                        myOrder == Order.givenFirst ? Order.surnameFirst : Order.givenFirst;
                Map<ULocale, Order> localeToOrder =
                        formatter.getNamePatternData().getLocaleToOrder();
                Multimap<Order, ULocale> orderToLocale =
                        Multimaps.invertFrom(
                                Multimaps.forMap(localeToOrder), TreeMultimap.create());
                ULocale otherLocale = null;
                for (ULocale tryLocale : orderToLocale.get(otherOrder)) {
                    if (!undLocale.equals(tryLocale)) {
                        otherLocale = tryLocale;
                        break;
                    }
                }
                if (otherLocale == null) {
                    otherLocale = myLocale.equals(ULocale.FRENCH) ? ULocale.GERMAN : ULocale.FRENCH;
                }

                // now change region to AQ, just to check for inheritance
                myLocale = addRegionIfMissing(myLocale, "AQ");
                otherLocale = addRegionIfMissing(otherLocale, "AQ");

                // Start collecting output

                StringWriter output = new StringWriter();
                output.write("\n");
                writeChoices("field", Field.ALL, output);
                writeChoices("modifiers", Modifier.ALL, output);
                writeChoices("order", Order.ALL, output);
                writeChoices("length", Length.ALL, output);
                writeChoices("usage", Usage.ALL, output);
                writeChoices("formality", Formality.ALL, output);

                for (Entry<SampleType, SimpleNameObject> entry : names.entrySet()) {
                    // write the name information
                    SampleType sampleType = entry.getKey();
                    if (!sampleType.isNative() && otherLocale == null) {
                        continue;
                    }
                    final SimpleNameObject nameObject = entry.getValue();

                    output.write("\n");
                    output.write("# " + sampleType + "\n");
                    for (Entry<ModifiedField, String> x :
                            nameObject.getModifiedFieldToValue().entrySet()) {
                        output.write("name ; " + x.getKey() + "; " + x.getValue() + "\n");
                    }

                    // handle the situation that ICU's formatter doesn't give us low-level access
                    // so we have to use the name locale to set the direction

                    Order nameOrder;
                    if (sampleType.isNative()) {
                        output.write("name ; " + "locale" + "; " + myLocale + "\n");
                        nameOrder = myOrder;
                    } else {
                        output.write("name ; " + "locale" + "; " + otherLocale + "\n");
                        nameOrder = otherOrder;
                    }

                    // Group the formatted names, longest first

                    Multimap<String, FormatParameters> valueToSources =
                            TreeMultimap.create(LENGTH_FIRST, Comparator.naturalOrder());
                    for (FormatParameters parameters : FormatParameters.allCldr()) {

                        //                        boolean debugPoint = locale.startsWith("th") &&
                        // parameters.equals(testParameters)
                        //                            && sampleType == SampleType.nativeGS;
                        //                        if (debugPoint) {
                        //                            System.out.println(sampleType + "; " +
                        // nameObject + "; " + testParameters);
                        //                            int debug = 0;
                        //                        }

                        String formatted =
                                formatter
                                        .format(nameObject, parameters)
                                        .replace("ᵛ", ""); // remove special CLDR ST hack

                        if (formatted.isEmpty()) {
                            continue;
                        }
                        valueToSources.put(formatted, parameters);
                    }
                    // write out the result, and then all the parameters that give produce it.
                    for (Entry<String, Collection<FormatParameters>> entry2 :
                            valueToSources.asMap().entrySet()) {
                        final String expectedResult = entry2.getKey();
                        output.write("\nexpectedResult; " + expectedResult + "\n\n");
                        entry2.getValue()
                                .forEach(
                                        x -> {
                                            output.write(
                                                    "parameters; "
                                                            + x.getOrder()
                                                            + "; "
                                                            + x.getLength()
                                                            + "; "
                                                            + x.getUsage()
                                                            + "; "
                                                            + x.getFormality()
                                                            + "\n");
                                        });
                    }
                    output.write("\nendName\n");
                }

                try (PrintWriter output2 = FileUtilities.openUTF8Writer(dir, locale + ".txt"); ) {
                    output2.write(
                            "# Test data for unit conversions"
                                    + "\n#  Copyright © 1991-2023 Unicode, Inc."
                                    + "\n#  For terms of use, see http://www.unicode.org/copyright.html"
                                    + "\n#  SPDX-License-Identifier: Unicode-DFS-2016"
                                    + "\n#  CLDR data files are interpreted according to the LDML specification (http://unicode.org/reports/tr35/)"
                                    + "\n#"
                                    + "\n# CLDR person name formatting test data for: "
                                    + locale
                                    + "\n#"
                                    + "\n# Test lines have the following structure:"
                                    + "\n#"
                                    + "\n# enum ; <type> ; <value>(', ' <value)"
                                    + "\n#   For all the elements in <…> below, the possible choices that could appear in the file."
                                    + "\n#   For example, <field> could be any of title, given, … credentials."
                                    + "\n#   Verify that all of these values work with the implementation."
                                    + "\n#"
                                    + "\n# name ; <field>('-'<modifier>) ; <value>"
                                    + "\n#   A sequence of these is to be used to build a person name object with the given field values."
                                    + "\n#   If the <field> is 'locale', then the value is the locale of the name."
                                    + "\n#     That will always be the last field in the name."
                                    + "\n#     NOTE: the locale for the name (where different than the test file's locale) will generally not match the text."
                                    + "\n#     It is chosen to exercise the person name formatting, by having a different given-surname order than the file's locale."
                                    + "\n#"
                                    + "\n# expectedResult; <value>"
                                    + "\n#   This line follows a sequence of name lines, and indicates the that all the following parameter lines have this expected value."
                                    + "\n#"
                                    + "\n# parameters; <options>; <length>; <usage>; <formality>"
                                    + "\n#   Each of these parameter lines should be tested to see that when formatting the current name with these parameters, "
                                    + "\n#   the expected value is produced."
                                    + "\n#"
                                    + "\n# endName"
                                    + "\n#   Indicates the end of the values to be tested with the current name."
                                    + "\n#"
                                    + "\n# ====="
                                    + "\n# Example:"
                                    + "\n#     enum ; field ; title, given, given2, surname, surname2, generation, credentials"
                                    + "\n#     …"
                                    + "\n#"
                                    + "\n#     name ; given; Iris"
                                    + "\n#     name ; surname; Falke"
                                    + "\n#     name ; locale; de"
                                    + "\n#"
                                    + "\n#     expectedResult; Falke, Iris"
                                    + "\n#"
                                    + "\n#     parameters; sorting; long; referring; formal"
                                    + "\n#     parameters; sorting; medium; referring; informal"
                                    + "\n#"
                                    + "\n#     endName"
                                    + "\n#"
                                    + "\n#     name ; given; Max"
                                    + "\n#     name ; given2; Ben"
                                    + "\n#     name ; surname; Mustermann"
                                    + "\n#     …"
                                    + "\n# ====="
                                    + "\n");
                    output2.write(output.toString());
                }
            } catch (Exception e) {
                System.out.println("Skipping " + locale);
                e.printStackTrace();
                continue;
            }
        }
    }

    public static ULocale addRegionIfMissing(ULocale myLocale, String region) {
        return !myLocale.getCountry().isEmpty()
                ? myLocale
                : new ULocale.Builder().setLocale(myLocale).setRegion(region).build();
    }

    public static <T> void writeChoices(String kind, Collection<T> choices, StringWriter output) {
        output.write("enum ; " + kind + " ; " + COMMA_JOINER.join(choices) + "\n");
    }
}
