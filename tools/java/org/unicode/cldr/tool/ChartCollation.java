package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.unicode.cldr.tool.FormattedFileWriter.Anchors;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRFile.NumberingSystem;
import org.unicode.cldr.util.CLDRFile.WinningChoice;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.FileCopier;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;

public class ChartCollation extends Chart {

    static final String NOT_TAILORED = "notTailored";
    static final String NOT_EXEMPLARS = "notExemplars";

    private static final String KNOWN_PROBLEMS = "<p>Known issues:</p>"
        + "<ul>" + LS
        + "<li>The ordering is illustrated with a basic list:"
        + "<ol>" + LS
        + "<li>it doesn't show the strength differences</li>" + LS
        + "<li>it does not yet take the settings or imports into account, so those are listed separately</li>" + LS
        + "<li>consult the XML file for the exact details</li>" + LS
        + "</ol>" + LS
        + "<li>The characters used in the illustration are:" + LS
        + "<ol>" + LS
        + "<li>those <span class='" + NOT_TAILORED + "'>not tailored</span> (added from standard exemplars for context)</li>" + LS
        + "<li>those <span class='" + NOT_EXEMPLARS + "'>tailored</span>, but not in any exemplars (standard, aux, punctuation)</li>" + LS
        + "<li>those both tailored and in exemplars</li>" + LS
        + "</ol>" + LS
        + "<li>The tailored characters may include:" + LS
        + "<ol>" + LS
        + "<li>some longer strings (contractions) from the rules</li>" + LS
        + "<li>generated Unicode characters (for <i>canonical closure</i>)</li>" + LS
        + "</ol>" + LS
        + "</li>" + LS
        + "</ul>" + LS;

    private static final Factory CLDR_FACTORY = CLDRConfig.getInstance().getCldrFactory();
    private static final boolean DEBUG = false;
    private static final String DIR = CLDRPaths.CHART_DIRECTORY + "collation/";

    //static Factory cldrFactory = Factory.make(CLDRPaths.COMMON_DIRECTORY + "collation/", ".*");

    public static void main(String[] args) {
        new ChartCollation().writeChart(null);
    }

    @Override
    public String getDirectory() {
        return DIR;
    }

    @Override
    public String getTitle() {
        return "Collation Charts";
    }

    @Override
    public String getFileName() {
        return "index";
    }

    @Override
    public String getExplanation() {
        return "<p>This is a <i>preliminary</i> set of charts for CLDR collation tailorings. "
            + "Collation tailorings provide language or locale-specific modifications of the standard Unicode CLDR collation order, "
            + "which is based on <a target='_blank' href='http://unicode.org/charts/collation/'>Unicode default collation charts</a>. "
            + "Locales that just use the standard CLDR order are not listed. "
            + "For more information, see the "
            + "<a target='_blank' href='http://unicode.org/reports/tr35/tr35-collation.html'>LDML Collation spec</a>. "
            + "The complete data for these charts is in "
            + "<a target='_blank' href='" + ToolConstants.CHART_SOURCE + "common/collation/'>collation/</a>.</p>" + LS;
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

    static class Data {
        RuleBasedCollator collator;
        Set<String> settings = new LinkedHashSet<>();
    }

    public void writeSubcharts(Anchors anchors) throws IOException {
        Matcher settingsMatcher = PatternCache.get(
            "//ldml/collations/collation"
                + "\\[@type=\"([^\"]+)\"]"
                + "(.*)?"
                + "/(settings|import|cr)"
                + "(.*)")
            .matcher("");
        Splitter settingSplitter = Splitter.onPattern("[\\[\\]@]").omitEmptyStrings().trimResults();
        File baseDir = new File(CLDRPaths.COMMON_DIRECTORY + "collation/");
        Transliterator fromUnicode = Transliterator.getInstance("Hex-Any");
        List<Pair<String, String>> pathValueList = new ArrayList<>();
        HashSet<String> mainAvailable = new HashSet<>(CLDR_FACTORY.getAvailable());
//        for (String xmlName : baseDir.list()) {
//            if (!xmlName.endsWith(".xml")) {
//                continue;
//            }
//            String locale = xmlName.substring(0,xmlName.length()-4);
//        }
        for (String xmlName : baseDir.list()) {
            if (!xmlName.endsWith(".xml")) {
                continue;
            }
            String locale = xmlName.substring(0, xmlName.length() - 4);
            if (!mainAvailable.contains(locale)) {
                System.out.println("Skipping locale not in main: " + locale);
                continue;
            }

            pathValueList.clear();
            XMLFileReader.loadPathValues(CLDRPaths.COMMON_DIRECTORY + "collation/" + xmlName, pathValueList, true);
            Map<String, Data> data = new TreeMap<>();

            for (Pair<String, String> entry : pathValueList) {
                String path = entry.getFirst();
                String value = entry.getSecond();
                if (path.startsWith("//ldml/identity/")) {
                    continue;
                }

                if (path.equals("//ldml/collations/defaultCollation")) {
                    addCollator(data, value, "defaultCollation", Arrays.asList("true"));
                    continue;
                }

                // Root collator being empty isn't really a failure - just skip it.
                if (xmlName.equals("root.xml") && path.equals("//ldml/collations/collation[@type=\"standard\"]")) {
                    continue;
                }
                XPathParts xpp = XPathParts.getTestInstance(path);
                DraftStatus status = DraftStatus.forString(xpp.findFirstAttributeValue("draft"));
                if (status == DraftStatus.unconfirmed) {
                    System.out.println("Skipping " + path + " in: " + xmlName + " due to draft status = " + status.toString());
                    continue;
                }

                if (!settingsMatcher.reset(path).matches()) {
                    System.out.println("Failure in " + xmlName + " with: " + path);
                    continue;
                }
                String type = settingsMatcher.group(1);
                String otherAttributes = settingsMatcher.group(2);
                String leaf = settingsMatcher.group(3);
                String values = settingsMatcher.group(4);

                if (leaf.equals("settings") || leaf.equals("import")) {
                    //ldml/collations/collation[@type="compat"][@visibility="external"]/settings[@reorder="Arab"]
                    List<String> settings = settingSplitter.splitToList(values);
                    addCollator(data, type, leaf, settings);
                    continue;
                }
                String rules = value;
                if (!rules.contains("'#⃣'")) {
                    rules = rules.replace("#⃣", "'#⃣'").replace("*⃣", "'*⃣'"); //hack for 8288
                }
                rules = fromUnicode.transform(rules);

                try {
                    RuleBasedCollator col = new RuleBasedCollator(rules);
                    col.setStrength(Collator.IDENTICAL);
                    col.freeze();
                    addCollator(data, type, col);
                } catch (Exception e) {
                    System.out.println("*** Skipping " + locale + ":" + type + ", " + e);
                }
            }
            if (data.isEmpty()) { // remove completely empty
                continue;
            }
            if (!data.containsKey("standard")) {
                addCollator(data, "standard", (RuleBasedCollator) null);
            }
            new Subchart(ENGLISH.getName(locale, true, CLDRFile.SHORT_ALTS), locale, data).writeChart(anchors);
        }
    }

    private void addCollator(Map<String, Data> data, String type, String leaf, List<String> settings) {
        if (type.startsWith("private-")) {
            type = "\uFFFF" + type;
        }
        Data dataItem = data.get(type);
        if (dataItem == null) {
            data.put(type, dataItem = new Data());
        }
        dataItem.settings.add(leaf + ":" + CollectionUtilities.join(settings, ";"));
    }

    private void addCollator(Map<String, Data> data, String type, RuleBasedCollator col) {
        if (type.startsWith("private-")) {
            type = "\uFFFF" + type;
        }
        Data dataItem = data.get(type);
        if (dataItem == null) {
            data.put(type, dataItem = new Data());
        }
        dataItem.collator = col;
    }

    //RuleBasedCollator ROOT = (RuleBasedCollator) Collator.getInstance(ULocale.ROOT);

    private class Subchart extends Chart {
        private static final String HIGH_COLLATION_PRIMARY = "\uFFFF";
        String title;
        String file;
        private Map<String, Data> data;

        @Override
        public boolean getShowDate() {
            return false;
        }

        public Subchart(String title, String file, Map<String, Data> data2) {
            this.title = title;
            this.file = file;
            this.data = data2;
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
            return "<p>This is a <i>preliminary</i> chart for the " + title
                + " collation tailorings. "
                + "The complete data for this chart is found on "
                + "<a target='_blank' href='" + ToolConstants.CHART_SOURCE + "common/collation/" + file + ".xml'>" + file + ".xml</a>.</p>"
                + KNOWN_PROBLEMS;
        }

        @Override
        public void writeContents(FormattedFileWriter pw) throws IOException {

            CLDRFile cldrFile = CLDR_FACTORY.make(file, true);
            UnicodeSet exemplars = cldrFile.getExemplarSet("", WinningChoice.WINNING).freeze();

            UnicodeSet exemplars_all = new UnicodeSet(exemplars);
            UnicodeSet exemplars_auxiliary = cldrFile.getExemplarSet("auxiliary", WinningChoice.WINNING);
            UnicodeSet exemplars_punctuation = cldrFile.getExemplarSet("punctuation", WinningChoice.WINNING);
            exemplars_all.addAll(exemplars_auxiliary)
                .addAll(exemplars_punctuation);

            for (NumberingSystem system : NumberingSystem.values()) {
                UnicodeSet exemplars_numeric = cldrFile.getExemplarsNumeric(system);
                if (exemplars_numeric != null) {
                    exemplars_all.addAll(exemplars_numeric);
                    //System.out.println(file + "\t" + system + "\t" + exemplars_numeric.toPattern(false));
                }
            }
            exemplars_all.freeze();

            TablePrinter tablePrinter = new TablePrinter()
                .addColumn("Type", "class='source'", null, "class='source'", true)
                .addColumn("Ordering", "class='target'", null, "class='target_nofont'", true);

            for (Entry<String, Data> entry : data.entrySet()) {
                // sort the characters
                String type = entry.getKey();
                if (type.startsWith(HIGH_COLLATION_PRIMARY)) {
                    type = type.substring(1);
                }
                RuleBasedCollator col = entry.getValue().collator;
                Set<String> settings = entry.getValue().settings;
                StringBuilder list = new StringBuilder();
                if (!settings.isEmpty()) {
                    list.append(CollectionUtilities.join(settings, "<br>"));
                    list.append("<br><b><i>plus</i></b><br>");
                }
                if (col == null) {
                    list.append("<i>CLDR default character order</i>");
                } else {
                    UnicodeSet tailored = new UnicodeSet(col.getTailoredSet());
                    Set<String> sorted = new TreeSet<>(col);
                    exemplars.addAllTo(sorted);
                    tailored.addAllTo(sorted);
                    boolean first = true;
                    for (String s : sorted) {
//                        if (--maxCount < 0) {
//                            list.append(" …");
//                            break;
//                        }
                        if (first) {
                            first = false;
                        } else {
                            list.append(' ');
                        }
                        if (s.startsWith("\uFDD0")) { // special CJK markers
                            int len = list.length();
                            if (len > 4 && list.substring(len - 4, len).equals("<br>")) {
                                list.append("<br>");
                            }
                            continue;
                        }
                        if (!tailored.contains(s)) {
                            list.append("<span class='" + NOT_TAILORED + "'>").append(s).append("</span>");
                        } else if (!exemplars_all.containsAll(s) && !file.equals("root")) {
                            list.append("<span class='" + NOT_EXEMPLARS + "'>").append(s).append("</span>");
                        } else {
                            list.append(s);
                        }
                    }
                }
                tablePrinter
                    .addRow()
                    .addCell(type)
                    .addCell(list.toString());
                tablePrinter.finishRow();
            }
            pw.write(tablePrinter.toTable());
        }
    }
}
