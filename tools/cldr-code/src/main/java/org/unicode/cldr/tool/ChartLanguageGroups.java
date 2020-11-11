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

    private static final String SHOULD_NOT_BE_LEAF_NODE = "🍂";
    private static final String LEAF_NODES = "🍃";
    private static final String TREE_NODES = "🌲";

    public static void main(String[] args) {
        new ChartLanguageGroups().writeChart(null);
    }

    static final Set<String> COLLECTIONS;
    static {
        Map<String, Map<LstrField, String>> languages = StandardCodes.getEnumLstreg().get(LstrType.language);
        Builder<String> _collections = ImmutableSet.<String> builder();
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
        return "<p>This chart shows draft language groups based on data extracted from wikidata. "
            + "The <b>Status</b> cell indicates the nature of the items in the adjacent <b>Contained</b> cell:<p>"
            + "<ul>\n"
            + "<li>A " + TREE_NODES
            + " indicates that the contained languages are tree nodes (contain other languages or langauge groups), "
            + "and will be listed further down in the chart in a <b>Language Group</b> cell.</li>\n"
            + "<li>A " + LEAF_NODES
            + " indicates that the contained languages are leaf nodes (contain nothing).</li>\n"
            + "<li>A " + SHOULD_NOT_BE_LEAF_NODE
            + " before an item <i>in</i> a <b>Contained</b> cell indicates a leaf node that shouldn’t be — that is, its ISO 639 Scope is "
            + "<a href='http://www-01.sil.org/iso639-3/scope.asp#C' target='_blank'>Collection</a>.</li>\n"
            + "</ul>\n"
            + "<p><b>Caveats:</b> Only the wikidata containment for "
            + "<a href='http://unicode.org/reports/tr35/#unicode_language_subtag'>valid language codes</a> is used."
            + "The containment data is not complete: "
            + "if a language doesn't appear in the chart it could be an isolate, or just be missing data."
            + "The data doesn't completely match wikipedia’s; there are some patches for CLDR languages.</p>\n";
    }

    Collator ENGLISH_ORDER = Collator.getInstance(ULocale.ENGLISH);

    @Override
    public void writeContents(FormattedFileWriter pw) throws IOException {

        Multimap<String, String> lg = CLDRConfig.getInstance().getSupplementalDataInfo().getLanguageGroups();

        TablePrinter tablePrinter = new TablePrinter()
            .addColumn("Language Group", "class='source'", CldrUtility.getDoubleLinkMsg(), "class='source'", true)
            .setBreakSpans(true)
            .addColumn("Name", "class='source'", null, "class='source'", true)
            .addColumn("St.", "class='source'", null, "class='source'", true)
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
        TreeSet<Pair<String, String>> nameAndCode = new TreeSet<>(new Comparator<Pair<String, String>>() {
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
        StringBuilder treeList = new StringBuilder();
        StringBuilder leafList = new StringBuilder();
        LinkedHashSet<Pair<String, String>> nameAndCodeWithChildren = new LinkedHashSet<>();
        for (Pair<String, String> pair : nameAndCode) {
            String code = pair.getSecond();
            if (lg.containsKey(code)) {
                addChildren(treeList, TREE_NODES, pair, false);
                nameAndCodeWithChildren.add(pair);
            } else if (!code.equals("und")) {
                addChildren(leafList, LEAF_NODES, pair, true);
            }
        }
        if (treeList.length() != 0) {
            addRow(parent, tablePrinter, TREE_NODES, treeList);
        }
        if (leafList.length() != 0) {
            addRow(parent, tablePrinter, LEAF_NODES, leafList);
        }

        for (Pair<String, String> pair : nameAndCodeWithChildren) {
            show(lg, pair.getSecond(), tablePrinter);
        }
    }

    private void addRow(String parent, TablePrinter tablePrinter, String marker, StringBuilder treeList) {
        tablePrinter.addRow()
            .addCell(parent)
            .addCell(getLangName(parent))
            .addCell(marker)
            .addCell(treeList.toString())
            .finishRow();
    }

    private void addChildren(StringBuilder treeList, String marker, Pair<String, String> pair, boolean showCollections) {
        if (treeList.length() != 0) {
            treeList.append("; ");
        }
        treeList.append(getPairName(pair, showCollections));
    }

    private String getPairName(Pair<String, String> pair, boolean showCollection) {
        return (showCollection && COLLECTIONS.contains(pair.getSecond())
            ? SHOULD_NOT_BE_LEAF_NODE + " " : "")
            + pair.getSecond() + " “" + pair.getFirst() + "”";
    }

    private String getLangName(String langCode) {
        return langCode.equals("mul") ? "All"
            : langCode.equals("zh") ? "Mandarin Chinese"
                : ENGLISH.getName(CLDRFile.LANGUAGE_NAME, langCode).replace(" (Other)", "").replace(" languages", "");
    }
}
