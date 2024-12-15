package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.personname.PersonNameFormatter;
import org.unicode.cldr.util.personname.PersonNameFormatter.FormatParameters;
import org.unicode.cldr.util.personname.PersonNameFormatter.Order;
import org.unicode.cldr.util.personname.PersonNameFormatter.SampleType;
import org.unicode.cldr.util.personname.PersonNameFormatter.Usage;
import org.unicode.cldr.util.personname.SimpleNameObject;

public class ChartPersonName extends Chart {
    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    static final CLDRFile ENGLISH = CLDR_CONFIG.getEnglish();
    static final String DIR = ChartPersonNames.DIR;
    static final Map<SampleType, SimpleNameObject> ENGLISH_NAMES =
            PersonNameFormatter.loadSampleNames(ENGLISH);

    private final String locale;

    public ChartPersonName(String locale) {
        super();
        this.locale = locale;
    }

    @Override
    public String getDirectory() {
        return DIR;
    }

    @Override
    public String getTitle() {
        return ENGLISH.nameGetter().getNameFromIdentifier(locale) + ": Person Names";
    }

    @Override
    public String getExplanation() {
        return "<p>This chart shows the different ways that the sample native names and sample foreign names would be formatted.</p>";
    }

    @Override
    public String getFileName() {
        return locale;
    }

    enum Filter {
        Main,
        Sorting,
        Monogram
    }

    enum Source {
        NativeSamples,
        ForeignSamples
    }

    @Override
    public void writeContents(Writer pw, Factory factory) throws IOException {
        CLDRFile cldrFile = factory.make(locale, true);
        Map<SampleType, SimpleNameObject> names = PersonNameFormatter.loadSampleNames(cldrFile);
        if (names.isEmpty()) {
            pw.write("<p>No sample names to display.</p>");
            return;
        }
        pw.write("<div class='ReportChart'>\n");
        PersonNameFormatter formatter = new PersonNameFormatter(cldrFile);

        for (Source source : Source.values()) {
            for (Filter filter : Filter.values()) {
                TablePrinter tablePrinter =
                        new TablePrinter()
                                .addColumn("Order", "class='source'", null, "class='source'", true)
                                .addColumn("Length", "class='source'", null, "class='source'", true)
                                .addColumn("Usage", "class='source'", null, "class='source'", true)
                                .addColumn(
                                        "Formality",
                                        "class='source'",
                                        null,
                                        "class='source'",
                                        true);

                for (SampleType sampleType : SampleType.ALL) {
                    if (sampleType.isNative() == (source == Source.NativeSamples)) {
                        tablePrinter.addColumn(
                                sampleType.toString(),
                                "class='target'",
                                null,
                                "class='target'",
                                true);
                    }
                }
                tablePrinter.addColumn("view", "class='source'", null, "class='source'", true);

                for (FormatParameters parameters : FormatParameters.allCldr()) {
                    if ((filter == Filter.Monogram) != (parameters.getUsage() == Usage.monogram)) {
                        continue;
                    } else if ((filter == Filter.Sorting)
                            != (parameters.getOrder() == Order.sorting)) {
                        continue;
                    }
                    tablePrinter
                            .addRow()
                            .addCell(parameters.getOrder())
                            .addCell(parameters.getLength())
                            .addCell(parameters.getUsage())
                            .addCell(parameters.getFormality());

                    for (SampleType sampleType : SampleType.ALL) {
                        if (sampleType.isNative() == (source == Source.NativeSamples)) {
                            final SimpleNameObject nameObject = names.get(sampleType);
                            String value =
                                    nameObject == null
                                            ? ""
                                            : formatter.format(nameObject, parameters);
                            // TODO This test needs to be in CheckCLDR. It is a bit more complicated
                            // since multiple paths are involved,
                            // so filed https://unicode-org.atlassian.net/browse/CLDR-16354 for
                            // doing it later
                            if (parameters.getUsage() == Usage.monogram) {
                                if (value.contains("..")
                                        || value.contains(". .")
                                        || value.endsWith(".")
                                        || value.startsWith(".")) {
                                    System.err.println(
                                            "Error: "
                                                    + locale
                                                    + "\t"
                                                    + parameters
                                                    + "\t"
                                                    + value
                                                    + "\t"
                                                    + nameObject.getAvailableFields());
                                }
                            }
                            tablePrinter.addCell(value.isBlank() ? "<i>missing</i>" : value);
                        }
                    }
                    String path =
                            "//ldml/personNames/personName[@order=\""
                                    + parameters.getOrder()
                                    + "\"][@length=\""
                                    + parameters.getLength()
                                    + "\"][@usage=\""
                                    + parameters.getUsage()
                                    + "\"][@formality=\""
                                    + parameters.getFormality()
                                    + "\"]/namePattern";
                    tablePrinter.addCell(getFixLinkFromPath(cldrFile, path));
                    tablePrinter.finishRow();
                }
                pw.write("\n<h2>" + source + ": " + filter + "</h2>\n");
                pw.write(tablePrinter.toTable());
                tablePrinter.clearRows();
            }
        }
        pw.write("</div> <!-- ReportChart -->\n");
    }
}
