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
import org.unicode.cldr.util.personname.PersonNameFormatter.SampleType;
import org.unicode.cldr.util.personname.SimpleNameObject;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.text.Collator;

public class GeneratePersonNameTestData {
    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    static final CLDRFile ENGLISH = CLDR_CONFIG.getEnglish();
    static final String DIR = CLDRPaths.TEST_DATA;
    static final Map<SampleType, SimpleNameObject> ENGLISH_NAMES = PersonNameFormatter.loadSampleNames(ENGLISH);

    enum Filter {Main, Sorting, Monogram}

    static final Comparator<String> LENGTH_FIRST = Comparator.comparingInt(String::length).reversed()
        .thenComparing(Collator.getInstance(Locale.ROOT));

    public static void main(String[] args) {
        File dir = new File(DIR, "personNameTest");
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

                StringWriter output = new StringWriter();


                for (Entry<SampleType, SimpleNameObject> entry : names.entrySet()) {
                    // write the name information
                    output.write("\n");
                    for (Entry<ModifiedField, String> x : entry.getValue().getModifiedFieldToValue().entrySet()) {
                        output.write("name ; " + x.getKey() + "; " + x.getValue() + "\n");
                    }

                    Multimap<String, String> valueToSource = TreeMultimap.create(LENGTH_FIRST, Comparator.naturalOrder());
                    for (FormatParameters parameters : FormatParameters.allCldr()) {
                        String formatted = formatter.format(entry.getValue(), parameters);
                        if (formatted.isEmpty()) {
                            continue;
                        }
                        valueToSource.put(formatted,
                            parameters.getOrder() + "; "
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
                    output2.write("# Person name test data for: " + locale + "\n"
                        + "#\n"
                        + "# Test lines have the following structure:\n"
                        + "#\n"
                        + "# name ; <field> ; <value>\n"
                        + "#   A sequence of these is to be used to build a person name object with the given field values.\n"
                        + "#\n"
                        + "# expectedResult; <value>\n"
                        + "#   This line follows a sequence of name lines, and indicates the that all the following parameter lines have this expected value.\n"
                        + "#\n"
                        + "# parameters; <order>; <length>; <usage>; <formality>\n"
                        + "#   Each of these parameters should be tested to see that when formatting the current name with these parameters produces the expected value.\n"
                        + "#\n"
                        + "# endName\n"
                        + "#   Indicates the end of the values to be tested with the current name.\n"
                        + "#\n"
                        + "# =====\n"
                        + "# Example:\n"
                        + "#     name ; given; Iris\n"
                        + "#     name ; surname; Falke\n"
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
