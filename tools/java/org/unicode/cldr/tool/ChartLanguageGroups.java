package org.unicode.cldr.tool;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StandardCodes.LstrField;
import org.unicode.cldr.util.StandardCodes.LstrType;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Multimap;
import com.ibm.icu.text.Collator;
import com.ibm.icu.util.ULocale;

public class ChartLanguageGroups extends Chart {

    public static void main(String[] args) {
        new ChartLanguageGroups().writeChart(null);
    }

    static final Set<String> COLLECTIONS;
    static {
        Map<String, Map<LstrField, String>> languages = StandardCodes.getEnumLstreg().get(LstrType.language);
        Builder<String> _collections = ImmutableSet.<String>builder();
        for (Entry<String, Map<LstrField, String>> e : languages.entrySet()) {
            String scope = e.getValue().get(LstrField.Scope);
            if (scope != null
                && "Collection".equalsIgnoreCase(scope)) {
                _collections.add(e.getKey());
            }
        }
        COLLECTIONS = _collections.build();
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
        return "<p>This chart shows language groups based on data extracted from wikidata. "
            + "There are some patches to the wikidata, and only the wikidata containment for "
            + "<a href='http://unicode.org/reports/tr35/#unicode_language_subtag'>valid language codes</a> is used.</p>\n"
            + "<ul>\n"
            + "<li>A ʹ indicates a language collection (ISO 636 Scope).</li>\n"
            + "<li>A * in the Name column indicates that the contained languages don't contain any themselves, "
            + "and are listed in one cell, separated by ‘;’.</li>\n"
            + "</ul>\n";
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
                .addCell(collectionPrime(parent))
                .addCell(getLangName(parent))
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
            .addCell(collectionPrime(parent))
            .addCell(getLangName(parent) + "*")
            .addCell(childrenString.toString())
            .finishRow();
        }

        for (Pair<String, String> pair : nameAndCodeWithChildren) {
            show(lg, pair.getSecond(), tablePrinter);
        }
    }

    private String getPairName(Pair<String, String> pair) {
        return collectionPrime(pair.getSecond()) + " “" + pair.getFirst() + "”";
    }

    private String getLangName(String langCode) {
        return langCode.equals("mul") 
            ? "All" 
            : ENGLISH.getName(CLDRFile.LANGUAGE_NAME, langCode).replace(" (Other)", "").replace(" languages", "");
    }

    private String collectionPrime(String langCode) {
        return langCode + (COLLECTIONS.contains(langCode) 
            ? "ʹ" : 
                "");
    }
}
