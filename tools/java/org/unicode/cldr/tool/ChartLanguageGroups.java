package org.unicode.cldr.tool;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Pair;

import com.google.common.collect.Multimap;
import com.ibm.icu.text.Collator;
import com.ibm.icu.util.ULocale;

public class ChartLanguageGroups extends Chart {

    public static void main(String[] args) {
        new ChartLanguageGroups().writeChart(null);
    }

    @Override
    public String getDirectory() {
        return FormattedFileWriter.CHART_TARGET_DIR;
    }

    @Override
    public String getTitle() {
        return "Language Groups";
    }

    @Override
    public String getExplanation() {
        return "<p>This chart shows language groups extracted from wikidata. "
            + "A * indicates that the contained languages are themselves containers. "
            + "Other contained languages are listed in one cell, separated by ‘;’. "
            + "Only the wikidata containment information for <a href='http://unicode.org/reports/tr35/#unicode_language_subtag'>valid language codes</a> is used. "
            + "<p>";
    }

    Collator ENGLISH_ORDER = Collator.getInstance(ULocale.ENGLISH);

    @Override
    public void writeContents(FormattedFileWriter pw) throws IOException {

        Multimap<String, String> lg = CLDRConfig.getInstance().getSupplementalDataInfo().getLanguageGroups();

        TablePrinter tablePrinter = new TablePrinter()
            .addColumn("Language Group", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
            .addColumn("Name", "class='source'", null, "class='source'", true)
            .addColumn("Contained", "class='source'", null, "class='target'", true)
            .setBreakSpans(true);

        show(lg, "mul", tablePrinter);
        pw.write(tablePrinter.toTable());
    }

    private void show(Multimap<String, String> lg, String parent, TablePrinter tablePrinter) {
        Collection<String> children = lg.get(parent);
        if (children == null || children.isEmpty()) {
            return;
        }
        TreeSet<Pair<String,String>> nameAndCode = new TreeSet<>(new Comparator<Pair<String,String>>() {
            @Override
            public int compare(Pair<String, String> o1, Pair<String, String> o2) {
                int diff = ENGLISH_ORDER.compare(o1.getFirst(), o2.getFirst());
                if (diff != 0) {
                    return diff;
                }
                return o1.getSecond().compareTo(o2.getSecond());
            }

        });
        for (String lang : children) {
            nameAndCode.add(Pair.of(getLangName(lang), lang));
        }
        StringBuilder childrenString = new StringBuilder();
        LinkedHashSet<Pair<String,String>> nameAndCodeWithChildren = new LinkedHashSet<>();
        for (Pair<String, String> pair : nameAndCode) {
            String code = pair.getSecond();
            if (lg.containsKey(code)) {
                nameAndCodeWithChildren.add(pair);
                tablePrinter.addRow()
                .addCell(parent)
                .addCell(getLangName(parent) + "*")
                .addCell(getPairName(pair))
                .finishRow();
            } else if (!code.equals("und")){
                if (childrenString.length() != 0) {
                    childrenString.append("; ");
                }
                childrenString.append(getPairName(pair));
            }
        }
        if (childrenString.length() != 0) {
            tablePrinter.addRow()
            .addCell(parent)
            .addCell(getLangName(parent))
            .addCell(childrenString.toString())
            .finishRow();
        }

        for (Pair<String, String> pair : nameAndCodeWithChildren) {
            show(lg, pair.getSecond(), tablePrinter);
        }
    }

    private String getPairName(Pair<String, String> pair) {
        return pair.getSecond() + " “" + pair.getFirst() + "”";
    }

    private String getLangName(String parent) {
        return parent.equals("mul") ? "All" : ENGLISH.getName(CLDRFile.LANGUAGE_NAME, parent).replace(" (Other)", "").replace(" languages", "");
    }
}
