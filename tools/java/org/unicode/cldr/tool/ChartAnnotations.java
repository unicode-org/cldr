package org.unicode.cldr.tool;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.tool.FormattedFileWriter.Anchors;
import org.unicode.cldr.util.Annotations;
import org.unicode.cldr.util.Annotations.AnnotationSet;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.FileCopier;
import org.unicode.cldr.util.LanguageGroup;
import org.unicode.cldr.util.Pair;

import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class ChartAnnotations extends Chart {

    private static final String LDML_ANNOTATIONS = "<a href='http://unicode.org/repos/cldr/trunk/specs/ldml/tr35-general.html#Annotations'>LDML Annotations</a>";

    private static final String MAIN_HEADER = "<p>Annotations provide names and keywords for Unicode characters, currently focusing on emoji. "
        + "If you see any problems, please <a target='_blank' href='http://unicode.org/cldr/trac/newticket'>file a ticket</a> with the corrected values for the locale. "
        + "For the XML data used for these charts, see "
        + "<a href='http://unicode.org/repos/cldr/tags/latest/common/annotations/'>latest-release annotations </a> "
        + "or <a href='http://unicode.org/repos/cldr/tags/latest/common/annotations/'>beta annotations</a>. "
        + "For more information, see " + LDML_ANNOTATIONS + ".</p>";
    private static final boolean DEBUG = false;
    private static final String DIR = CLDRPaths.CHART_DIRECTORY + "annotations/";

    public static void main(String[] args) {
        new ChartAnnotations().writeChart(null);
    }

    @Override
    public String getDirectory() {
        return DIR;
    }

    @Override
    public String getTitle() {
        return "Annotation Charts";
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
        FileCopier.copy(Chart.class, "index.css", DIR);

        FormattedFileWriter.Anchors anchors = new FormattedFileWriter.Anchors();
        writeSubcharts(anchors);
        pw.setIndex("Main Chart Index", "../index.html");
        pw.write(anchors.toString());
    }

    static final UnicodeSet EXTRAS = new UnicodeSet()
    .add("🇪🇺")
    .add("🔟")
    .add("#️⃣")
    .add("👶🏽")
    .add("👩‍❤️‍💋‍👩")
    .add("👩‍❤️‍👩")
    .add("👩‍👩‍👧")
    .add("👨🏻‍⚕️")
    .add("👮🏿‍♂️")
    .add("👮🏽‍♀️")
    .freeze();

    public void writeSubcharts(Anchors anchors) throws IOException {
        Set<String> locales = Annotations.getAvailableLocales();

        AnnotationSet english = Annotations.getDataSet("en");
        UnicodeSet s = new UnicodeSet(english.keySet()).addAll(EXTRAS).freeze();

        // set up right order for columns

        Map<String, String> nameToCode = new LinkedHashMap<String, String>();
        Relation<LanguageGroup, Pair<String, String>> groupToNameAndCodeSorted = Relation.of(
            new EnumMap<LanguageGroup, Set<Pair<String, String>>>(LanguageGroup.class),
            TreeSet.class);

        for (String locale : locales) {
            if (locale.equals("root")) {
                continue;
            }
            if (locale.equals("en")) { // make first
                continue;
            }
            String name = ENGLISH.getName(locale, true);
            int baseEnd = locale.indexOf('_');
            ULocale loc = new ULocale(baseEnd < 0 ? locale : locale.substring(0, baseEnd));
            LanguageGroup group = LanguageGroup.get(loc);
            groupToNameAndCodeSorted.put(group, Pair.of(name, locale));
        }

        for (Entry<LanguageGroup, Set<Pair<String, String>>> groupPairs : groupToNameAndCodeSorted.keyValuesSet()) {
            LanguageGroup group = groupPairs.getKey();
            String ename = ENGLISH.getName("en", true);
            nameToCode.clear();
            nameToCode.put(ename, "en"); // always have english first

            // add English variants if they exist

            for (Pair<String, String> pair : groupPairs.getValue()) {
                String name = pair.getFirst();
                String locale = pair.getSecond();
                if (locale.startsWith("en_")) {
                    nameToCode.put(name, locale);
                }
            }

            for (Pair<String, String> pair : groupPairs.getValue()) {
                String name = pair.getFirst();
                String locale = pair.getSecond();

                nameToCode.put(name, locale);
            }

            // now build table with right order for columns
            double width = ((int)((99.0 / (locales.size() + 1))*1000))/1000.0;
            //String widthString = "class='source' width='"+ width + "%'";
            String widthStringTarget = "class='target' width='" + width + "%'";

            TablePrinter tablePrinter = new TablePrinter()
            .addColumn("Char", "class='source' width='1%'", null, "class='source-image'", true)
            //.addColumn("Formal Name", "class='source' width='" + width + "%'", null, "class='source'", true)
            ;

            for (Entry<String, String> entry : nameToCode.entrySet()) {
                String name = entry.getKey();
                tablePrinter.addColumn(name, widthStringTarget, null, "class='target'", true);
            }
            // sort the characters
            Set<String> sorted = new TreeSet<>(RBC);

            for (String cp : s.addAllTo(sorted)) {
                tablePrinter
                .addRow()
                .addCell(cp)
                //.addCell(getName(cp))
                ;
                for (Entry<String, String> nameAndLocale : nameToCode.entrySet()) {
                    String name = nameAndLocale.getKey();
                    String locale = nameAndLocale.getValue();
                    AnnotationSet annotations = Annotations.getDataSet(locale);
                    if (DEBUG) System.out.println(name + ":" + annotations.toString(cp, false));
                    tablePrinter.addCell(annotations.toString(cp, true));
                }
                tablePrinter.finishRow();
            }
            final String name = group.toString();
            new Subchart(name + " Annotations", FileUtilities.anchorize(name), tablePrinter).writeChart(anchors);
        }
    }

    static final int FIRST_REGIONAL = 0x1F1E6;
    static final int LAST_REGIONAL = 0x1F1FF;

    public static int getRegionalIndicator(int firstCodepoint) {
        return FIRST_REGIONAL <= firstCodepoint && firstCodepoint <= LAST_REGIONAL ? firstCodepoint - FIRST_REGIONAL + 'A' : -1;
    }

    private String getName(String cp) {
        int ri1 = getRegionalIndicator(cp.codePointAt(0));
        if (ri1 >= 0) {
            int ri2 = getRegionalIndicator(cp.codePointAt(2));
            return ENGLISH.getName(CLDRFile.TERRITORY_NAME, String.valueOf((char) ri1) + String.valueOf((char) ri2));
        }
        String result = NAMES80.get(cp);
        return result != null ? result : UCharacter.getName(cp, ", ");
    }

    static UnicodeMap<String> NAMES80 = new UnicodeMap<>();
    static {
        String[][] data = {
            { "🏻", "EMOJI MODIFIER FITZPATRICK TYPE-1-2" },
            { "🏼", "EMOJI MODIFIER FITZPATRICK TYPE-3" },
            { "🏽", "EMOJI MODIFIER FITZPATRICK TYPE-4" },
            { "🏾", "EMOJI MODIFIER FITZPATRICK TYPE-5" },
            { "🏿", "EMOJI MODIFIER FITZPATRICK TYPE-6" },
            { "🤐", "ZIPPER-MOUTH FACE" },
            { "🤑", "MONEY-MOUTH FACE" },
            { "🤒", "FACE WITH THERMOMETER" },
            { "🤓", "NERD FACE" },
            { "🤔", "THINKING FACE" },
            { "🙄", "FACE WITH ROLLING EYES" },
            { "🙃", "UPSIDE-DOWN FACE" },
            { "🤕", "FACE WITH HEAD-BANDAGE" },
            { "🤖", "ROBOT FACE" },
            { "🤗", "HUGGING FACE" },
            { "🤘", "SIGN OF THE HORNS" },
            { "🦀", "CRAB (also Cancer)" },
            { "🦂", "SCORPION (also Scorpio)" },
            { "🦁", "LION FACE (also Leo)" },
            { "🏹", "BOW AND ARROW (also Sagittarius)" },
            { "🏺", "AMPHORA (also Aquarius)" },
            { "🛐", "PLACE OF WORSHIP" },
            { "🕋", "KAABA" },
            { "🕌", "MOSQUE" },
            { "🕍", "SYNAGOGUE" },
            { "🕎", "MENORAH WITH NINE BRANCHES" },
            { "📿", "PRAYER BEADS" },
            { "🌭", "HOT DOG" },
            { "🌮", "TACO" },
            { "🌯", "BURRITO" },
            { "🧀", "CHEESE WEDGE" },
            { "🍿", "POPCORN" },
            { "🍾", "BOTTLE WITH POPPING CORK" },
            { "🦃", "TURKEY" },
            { "🦄", "UNICORN FACE" },
            { "🏏", "CRICKET BAT AND BALL" },
            { "🏐", "VOLLEYBALL" },
            { "🏑", "FIELD HOCKEY STICK AND BALL" },
            { "🏒", "ICE HOCKEY STICK AND PUCK" },
            { "🏓", "TABLE TENNIS PADDLE AND BALL" },
            { "🏸", "BADMINTON RACQUET AND SHUTTLECOCK" } };
        for (String[] pair : data) {
            NAMES80.put(pair[0], pair[1]);
        }
        NAMES80.freeze();
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
            return MAIN_HEADER
                + "<p>This table shows the annotations for a group of related languages (plus English) for easier comparison. "
                + "The first item is the <b>short name</b> (also the text-to-speech phrase). "
                + "It is bolded for clarity, and marked with a * for searching on this page. "
                + "The remaining phrases are <b>keywords</b> (labels), separated by “|”. "
                + "The keywords plus the words in the short name are typically used for search and predictive typing.<p>\n"
                + "<p>Most short names and keywords that can be constructed with the mechanism in " + LDML_ANNOTATIONS + " are omitted. "
                    + "However, a few are included for comparison: "
                    + CollectionUtilities.join(EXTRAS.addAllTo(new TreeSet<>()), ", ") + ". "
                + "In this chart, missing items are marked with “" + Annotations.MISSING_MARKER + "”, "
                    + "‘fallback’ constructed items with “" + Annotations.BAD_MARKER + "”, "
                    + "and substituted English values with “" + Annotations.ENGLISH_MARKER + "” . </p>\n";
        }

        @Override
        public void writeContents(FormattedFileWriter pw) throws IOException {
            pw.write(tablePrinter.toTable());
        }
    }

    static RuleBasedCollator RBC;
    static {
        Factory cldrFactory = Factory.make(CLDRPaths.COMMON_DIRECTORY + "collation/", ".*");
        CLDRFile root = cldrFactory.make("root", false);
        String rules = root.getStringValue("//ldml/collations/collation[@type=\"emoji\"][@visibility=\"external\"]/cr");

//        if (!rules.contains("'#⃣'")) {
//            rules = rules.replace("#⃣", "'#⃣'").replace("*⃣", "'*⃣'"); //hack for 8288
//        }

        try {
            RBC = new RuleBasedCollator(rules);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    static final Set<String> ENGLISH_LABELS = new LinkedHashSet<>(Arrays.asList(
        "flag", "nature", "objects", "people", "places", "symbols", "travel", "animal",
        "office", "sign", "word", "time", "food", "person", "weather", "activity",
        "vehicle", "restaurant", "communication", "emotion", "geometric", "mark",
        "education", "gesture", "japanese", "symbol", "congratulation", "body", "clothing"));

//    static class Annotations {
//
//        final UnicodeRelation<String> values = new UnicodeRelation<>();
//
//        static Factory cldrFactory = Factory.make(CLDRPaths.COMMON_DIRECTORY + "annotations/", ".*");
//
//        static Set<String> getAvailableLocales() {
//            return cldrFactory.getAvailable();
//        }
//
//        static Map<String, Annotations> cache = new ConcurrentHashMap<>();
//
//        static synchronized Annotations make(String locale) {
//            Annotations result = cache.get(locale);
//            if (result == null) {
//                CLDRFile file = cldrFactory.make(locale, false); // for now, don't resolve
//                result = new Annotations();
//                LinkedHashSet<String> values = new LinkedHashSet<>();
//                XPathParts parts = new XPathParts();
//                Splitter sp = Splitter.on(';').omitEmptyStrings().trimResults();
//                for (String path : file) {
//                    if (path.startsWith("//ldml/identity")) {
//                        continue;
//                    }
//                    String value = file.getStringValue(path);
//                    String fullPath = file.getFullXPath(path);
//                    String cpString = parts.set(fullPath).getAttributeValue(-1, "cp");
//                    UnicodeSet cps = new UnicodeSet(cpString);
//                    String tts = parts.set(fullPath).getAttributeValue(-1, "tts");
//                    values.clear();
//                    if (tts != null) {
//                        values.add(tts.trim()); // always first value
//                    }
//                    values.addAll(sp.splitToList(value));
//                    result.values.addAll(cps, values);
//                }
//
//                // remove labels
//
//                if (locale.equals("en")) {
//                    for (Entry<String, Set<String>> item : result.values.keyValues()) {
//                        String key = item.getKey();
//                        Set<String> valueSet = new LinkedHashSet<>(item.getValue());
//                        for (String skip : ENGLISH_LABELS) {
//                            if (valueSet.contains(skip)) {
//                                result.values.remove(key, skip);
//                                if (result.values.get(key) == null) {
//                                    result.values.add(key, skip); // restore
//                                    break;
//                                }
//                            }
//                        }
//                        Set<String> newSet = result.values.get(key);
//                        if (!valueSet.equals(newSet)) {
//                            if (DEBUG) System.out.println("dropping labels from " + item.getKey() + ", old: " + valueSet + ", new: " + newSet);
//                        }
//                    }
//                }
//                result.values.freeze();
//                cache.put(locale, result);
//            }
//            return result;
//        }
//    }
}
