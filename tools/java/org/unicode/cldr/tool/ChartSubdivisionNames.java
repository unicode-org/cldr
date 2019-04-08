package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.tool.FormattedFileWriter.Anchors;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.FileCopier;
import org.unicode.cldr.util.LanguageGroup;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;

import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.util.ULocale;

public class ChartSubdivisionNames extends Chart {

    private static final String MAIN_HEADER = "<p>This chart shows the subdivision names. "
        + "Most of them (except in English and for a few others) are derived from wikidata, with some filtering.</p>";
    private static final boolean DEBUG = false;
    private static final String DIR = CLDRPaths.CHART_DIRECTORY + "subdivisionNames/";

    public static void main(String[] args) {
        new ChartSubdivisionNames().writeChart(null);
    }

    @Override
    public String getDirectory() {
        return DIR;
    }

    @Override
    public String getTitle() {
        return "Subdivision Name Charts";
    }

    @Override
    public String getFileName() {
        return "index";
    }

    @Override
    public String getExplanation() {
        return MAIN_HEADER + "<p>The charts are presented in groups of related languages, for easier comparison.<p>";
    }

    public void writeContents(FormattedFileWriter pw) throws IOException {
        FileCopier.ensureDirectoryExists(DIR);
        FileCopier.copy(Chart.class, "index.css", DIR);
        FormattedFileWriter.copyIncludeHtmls(DIR);

        FormattedFileWriter.Anchors anchors = new FormattedFileWriter.Anchors();
        writeSubcharts(anchors);
        pw.setIndex("Main Chart Index", "../index.html");
        pw.write(anchors.toString());
    }

    public void writeSubcharts(Anchors anchors) throws IOException {
        CLDRConfig cldrConfig = CLDRConfig.getInstance();
        File[] paths = {
            new File(CLDRPaths.SUBDIVISIONS_DIRECTORY) };

        Factory factory = SimpleFactory.make(paths, ".*");
        CLDRFile english = factory.make("en", true);

        Set<String> subdivisions = Validity.getInstance().getStatusToCodes(LstrType.subdivision).get(Status.regular);
        // set up right order for columns

        Map<String, String> nameToCode = new LinkedHashMap<String, String>();
        Relation<LanguageGroup, R3<Integer, String, String>> groupToNameAndCodeSorted = Relation.of(
            new EnumMap<LanguageGroup, Set<R3<Integer, String, String>>>(LanguageGroup.class),
            TreeSet.class);

        Set<String> locales = factory.getAvailable();
        for (String locale : locales) {
            if (locale.equals("root")) {
                continue;
            }
            if (locale.equals("en")) { // make first
                continue;
            }
            if (locale.startsWith("en")) {
                int debug = 0;
            }
            String name = ENGLISH.getName(locale, true);
            int baseEnd = locale.indexOf('_');
            ULocale loc = new ULocale(baseEnd < 0 ? locale : locale.substring(0, baseEnd));
            LanguageGroup group = LanguageGroup.get(loc);
            int rank = LanguageGroup.rankInGroup(loc);
            groupToNameAndCodeSorted.put(group, Row.of(rank, name, locale));
        }

        for (Entry<LanguageGroup, Set<R3<Integer, String, String>>> groupPairs : groupToNameAndCodeSorted.keyValuesSet()) {
            LanguageGroup group = groupPairs.getKey();
            String ename = ENGLISH.getName("en", true);
            nameToCode.clear();
            nameToCode.put(ename, "en"); // always have english first

            // add English variants if they exist

            for (R3<Integer, String, String> pair : groupPairs.getValue()) {
                String name = pair.get1();
                String locale = pair.get2();
                if (locale.startsWith("en_")) {
                    nameToCode.put(name, locale);
                }
            }

            for (R3<Integer, String, String> pair : groupPairs.getValue()) {
                String name = pair.get1();
                String locale = pair.get2();

                nameToCode.put(name, locale);
            }

            // now build table with right order for columns
            double width = ((int) ((99.0 / (locales.size() + 1)) * 1000)) / 1000.0;
            //String widthString = "class='source' width='"+ width + "%'";
            String widthStringTarget = "class='target' width='" + width + "%'";

            TablePrinter tablePrinter = new TablePrinter()
                .addColumn("Name", "class='source' width='1%'", null, "class='source'", true)
                .addColumn("Code", "class='source' width='1%'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
            //.addColumn("Formal Name", "class='source' width='" + width + "%'", null, "class='source'", true)
            ;

            for (Entry<String, String> entry : nameToCode.entrySet()) {
                String name = entry.getKey();
                tablePrinter.addColumn(name, widthStringTarget, null, "class='target'", true);
            }
            // sort the characters
            for (String code : subdivisions) {
                tablePrinter
                    .addRow()
                    .addCell(english.getName(CLDRFile.TERRITORY_NAME, code.substring(0, 2).toUpperCase(Locale.ENGLISH)))
                    .addCell(code);
                for (Entry<String, String> nameAndLocale : nameToCode.entrySet()) {
                    String name = nameAndLocale.getKey();
                    String locale = nameAndLocale.getValue();
                    CLDRFile cldrFile = factory.make(locale, false);
                    String name2 = cldrFile.getStringValue("//ldml/localeDisplayNames/subdivisions/subdivision[@type=\"" + code
                        + "\"]");
                    tablePrinter.addCell(name2 == null ? "" : name2);
                }
                tablePrinter.finishRow();
            }
            final String name = group.toString();
            new Subchart(name + " Subdivision Names", FileUtilities.anchorize(name), tablePrinter).writeChart(anchors);
        }
    }

    private class Subchart extends Chart {
        String title;
        String file;
        private TablePrinter tablePrinter;

        @Override
        public boolean getShowDate() {
            return false;
        }

        public Subchart(String title, String file, TablePrinter tablePrinter) {
            super();
            this.title = title;
            this.file = file;
            this.tablePrinter = tablePrinter;
        }

        @Override
        public String getDirectory() {
            return DIR;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getFileName() {
            return file;
        }

        @Override
        public String getExplanation() {
            return "<p>This table shows the subdivision names for a group of related languages (plus English) for easier comparison. "
                + "The coverage depends on the availability of data in wikidata for these names.</p>\n";
        }

        @Override
        public void writeContents(FormattedFileWriter pw) throws IOException {
            pw.write(tablePrinter.toTable());
        }
    }
}
