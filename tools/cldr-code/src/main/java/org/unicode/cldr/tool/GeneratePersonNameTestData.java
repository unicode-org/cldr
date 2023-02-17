package org.unicode.cldr.tool;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.personname.PersonNameFormatter;
import org.unicode.cldr.util.personname.PersonNameFormatter.FormatParameters;
import org.unicode.cldr.util.personname.PersonNameFormatter.ModifiedField;
import org.unicode.cldr.util.personname.PersonNameFormatter.Order;
import org.unicode.cldr.util.personname.PersonNameFormatter.SampleType;
import org.unicode.cldr.util.personname.SimpleNameObject;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.text.Collator;
import com.ibm.icu.util.ULocale;

public class GeneratePersonNameTestData {
    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    private static final CLDRFile ENGLISH = CLDR_CONFIG.getEnglish();

    static final Comparator<String> LENGTH_FIRST = Comparator.comparingInt(String::length).reversed()
        .thenComparing(Collator.getInstance(Locale.ROOT));

    public static void main(String[] args) {
        File dir = new File(CLDRPaths.TEST_DATA, "personNameTest");
        Factory factory = CLDR_CONFIG.getCldrFactory();
        for (String locale : factory.getAvailable()) {
            if (!locale.equals("de")) {
                continue;
            }
            try {
                CLDRFile cldrFile = factory.make(locale, true);
                Map<SampleType, SimpleNameObject>names = PersonNameFormatter.loadSampleNames(cldrFile);
                if (names.isEmpty()) {
                    continue;
                }
                PersonNameFormatter formatter = new PersonNameFormatter(cldrFile);

                // we have to jump through some hoops to get locales corresponding to the order

                Map<ULocale, Order> localeToOrder = formatter.getNamePatternData().getLocaleToOrder();
                ULocale myLocale = new ULocale(locale);
                Order myOrder = localeToOrder.get(myLocale);
                String myOrderString = myOrder.toString();

                Multimap<Order, ULocale> orderToLocale = TreeMultimap.create();
                Multimaps.invertFrom(Multimaps.forMap(localeToOrder), orderToLocale);
                Order otherOrder = myOrder == Order.givenFirst ? Order.surnameFirst : Order.givenFirst;
                Collection<ULocale>  otherLocales = orderToLocale.get(otherOrder);
                String otherLocaleString = otherLocales.isEmpty() ? null : otherLocales.iterator().next().toString();

                StringWriter output = new StringWriter();


                for (Entry<SampleType, SimpleNameObject> entry : names.entrySet()) {
                    // write the name information
                    SampleType sampleType = entry.getKey();
                    output.write("\n");
                    for (Entry<ModifiedField, String> x : entry.getValue().getModifiedFieldToValue().entrySet()) {
                        output.write("name ; " + x.getKey() + "; " + x.getValue() + "\n");
                    }

                    // handle that ICU's formatter doesn't give us low-level access
                    // so we have to use the name locale to set the direction

                    Order nameOrder;
                    if (sampleType.isNative()) {
                        output.write("name ; " + "locale" + "; " + myLocale + "\n");
                        nameOrder = myOrder;
                    } else if (otherLocaleString == null) {
                        continue;
                    } else {
                        output.write("name ; " + "locale" + "; " + otherLocaleString + "\n");
                        nameOrder = otherOrder;
                    }

                    Multimap<String, String> valueToSource = TreeMultimap.create(LENGTH_FIRST, Comparator.naturalOrder());
                    for (FormatParameters parameters : FormatParameters.allCldr()) {
                        String orderString = "n/a";
                        Order order = parameters.getOrder();

                        if (order == nameOrder) {
                            // cool
                        } else if (order == Order.sorting && nameOrder == myOrder) {
                            orderString = "sorting";
                        } else {
                            continue;
                        }

                        String formatted = formatter.format(entry.getValue(), parameters);
                        if (formatted.isEmpty()) {
                            continue;
                        }
                        valueToSource.put(formatted,
                            orderString + "; "
                                + parameters.getLength() + "; "
                                + parameters.getUsage() + "; "
                                + parameters.getFormality());
                    }
                    // write out the result, and then all the parameters that give produce it.
                    for (Entry<String, Collection<String>> entry2 : valueToSource.asMap().entrySet()) {
                        output.write("\nexpectedResult; " + entry2.getKey() + "\n\n");
                        entry2.getValue().forEach(x -> output.write("parameters; " + x + "\n"));
                    }
                    output.write("\nendName\n");
                }


                try (PrintWriter output2 = FileUtilities.openUTF8Writer(dir, locale + ".txt");) {
                    output2.write("# CLDR person name formatting test data for: " + locale + "\n"
                        + "#\n"
                        + "# Test lines have the following structure:\n"
                        + "#\n"
                        + "# name ; <field> ; <value>\n"
                        + "#   A sequence of these is to be used to build a person name object with the given field values.\n"
                        + "#   If the <field> is 'locale', then the value is the locale of the name.\n"
                        + "#     That will always be the last field in the name. \n"
                        + "#\n"
                        + "# expectedResult; <value>\n"
                        + "#   This line follows a sequence of name lines, and indicates the that all the following parameter lines have this expected value.\n"
                        + "#\n"
                        + "# parameters; <options>; <length>; <usage>; <formality>\n"
                        + "#   Each of these parameter lines should be tested to see that when formatting the current name with these parameters, "
                        + "#   the expected value is produced.\n"
                        + "#\n"
                        + "# endName\n"
                        + "#   Indicates the end of the values to be tested with the current name.\n"
                        + "#\n"
                        + "# =====\n"
                        + "# Example:\n"
                        + "#     name ; given; Iris\n"
                        + "#     name ; surname; Falke\n"
                        + "#     name ; locale; de\n"
                        + "#\n"
                        + "#     expectedResult; Falke, Iris\n"
                        + "#\n"
                        + "#     parameters; sorting; long; referring; formal\n"
                        + "#     parameters; sorting; medium; referring; informal\n"
                        + "#\n"
                        + "#     endName\n"
                        + "#\n"
                        + "#     name ; given; Max\n"
                        + "#     name ; given2; Ben\n"
                        + "#     name ; surname; Mustermann\n"
                        + "#     …\n"
                        + "#\n"
                        );
                    output2.write(output.toString());
                }
            } catch (Exception e) {
                System.out.println("Skipping " + locale + "\t" + e);
                continue;
            }

        }
    }
}
