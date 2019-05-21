package org.unicode.cldr.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.Builder;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.FileReaders;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;

import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.ULocale;

public class GeneratePluralList {
    static final String stock = "km|lo|ne|br|dz|nl|si|en|ar|de|es|fr|it|ja|ko|nl|pl|ru|th|tr|pt|zh|zh_Hant|bg|ca|cs|da|el|fa|fi|fil|hi|hr|hu|id|lt|lv|ro|sk|sl|sr|sv|uk|vi|he|nb|et|ms|am|bn|gu|is|kn|ml|mr|sw|ta|te|ur|eu|gl|af|zu|en_GB|es_419|pt_PT|fr_CA|zh_Hant_HK";
    private static final Map<String, Integer> keywordIndex = Builder.with(new HashMap<String, Integer>())
        .put("zero", 0)
        .put("one", 1)
        .put("two", 2)
        .put("few", 3)
        .put("many", 4)
        .put("other", 5)
        .get();

    private DecimalFormat format = new DecimalFormat();
    private PrintWriter out;
    private PluralRules rules;

    private GeneratePluralList(PrintWriter out) {
        if (out == null) {
            out = new PrintWriter(System.out);
        }
        this.out = out;
    }

    private Map<String, Map<String, String>> localesToNouns = new HashMap<String, Map<String, String>>();

    private void loadNouns() throws IOException {
        BufferedReader reader = FileReaders.openFile(GeneratePluralList.class, "fractionnum.csv");
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            String[] fields = line.split(",");
            String locale = fields[0];
            String count = fields[1];
            String format = fields[5];
            Map<String, String> nouns = localesToNouns.get(locale);
            if (nouns == null) {
                localesToNouns.put(locale, nouns = new HashMap<String, String>());
            }
            nouns.put(count, format);
        }
    }

    private class ExampleManager implements Iterable<String> {
        // integer, fraction and last digit are 3 different types
        private Set<String> list3;

        public ExampleManager() {
            list3 = new HashSet<String>();
        }

        public void add(String example) {
            list3.add(example);
        }

        @Override
        public String toString() {
            return list3.toString();
        }

        @Override
        public Iterator<String> iterator() {
            return list3.iterator();
        }

        public Set<String> getAll() {
            return list3;
        }
    }

    public static GeneratePluralList build(PrintWriter out) {
        GeneratePluralList generator = new GeneratePluralList(out);
        return generator;
    }

    private void getExamples(String locale) {
        rules = PluralRules.forLocale(new ULocale(locale));
        // Setup.
        Count[] digits = new Count[1000];
        // 0 is always considered a plural type even if the plural rules say otherwise.
        Set<Count> missingTypes = new HashSet<Count>();
        for (String keyword : rules.getKeywords()) {
            missingTypes.add(Count.valueOf(keyword));
        }
        Map<String, List<Integer>> integerMap = new HashMap<String, List<Integer>>();
        digits[0] = Count.zero;
        missingTypes.remove(Count.zero);
        put(integerMap, "zero", 0);
        put(integerMap, "zero|zero", 0);
        for (int i = 1; i < digits.length; i++) {
            Count type = Count.valueOf(rules.select(i));
            digits[i] = type;
            missingTypes.remove(type);
            put(integerMap, type.toString(), i);
            Count digitType = (i < 10) ? type : digits[i % 10];
            String key = type.toString() + '|' + digitType;
            put(integerMap, key, i);
        }

        missingTypes.remove(Count.other);

        if (missingTypes.size() > 0) {
            System.out.println("WARNING: the following plural types may not be represented fully for " + locale + ": " + missingTypes);
            for (Count type : missingTypes) {
                Collection<Double> values = rules.getSamples(type.toString());
                if (values != null) {
                    int value = values.iterator().next().intValue();
                    put(integerMap, type.toString(), value);
                }
            }
        }

        for (int i = 1; i <= 3; i++) {
            getExamples(locale, integerMap, i);
        }
    }

    private void getExamples(String locale, Map<String, List<Integer>> integerMap, int numDigits) {
        Map<String, String> nouns = localesToNouns.get(locale);
        if (nouns == null) return;

        // Load fractions as whole numbers.
        int limit = (int) Math.pow(10, numDigits);

        // Generate all examples.
        Map<String, String> exampleMap = new HashMap<String, String>();
        Map<String, ExampleManager> positionedExamples = new HashMap<String, ExampleManager>();
        Set<String> allKeywords = new HashSet<String>(rules.getKeywords());
        allKeywords.add("zero");
        allKeywords.retainAll(integerMap.keySet());
        List<Integer> values;
        format.setMinimumIntegerDigits(numDigits);
        for (String x : allKeywords) {
            values = integerMap.get(x);
            int integer = values.get(values.size() > 1 ? 1 : 0); // get new set of examples if possible
            for (String y : integerMap.keySet()) {
                if (!y.contains("|")) continue;
                values = integerMap.get(y);
                int fraction = values.get(values.size() > 1 ? 1 : 0);
                if (fraction >= limit) continue; // TODO: handle bg other
                String key = x + '|' + y;
                String[] keywords = key.split("\\|");
                if (!keywords[0].equals("zero") && keywords[0].equals(keywords[1])) {
                    continue;
                }
                for (int i = 0; i < keywords.length; i++) {
                    String position = i + keywords[i];
                    ExampleManager manager = positionedExamples.get(position);
                    if (manager == null) {
                        positionedExamples.put(position, manager = new ExampleManager());
                    }
                    manager.add(key);
                }
                String example = integer + "." + format.format(fraction);
                exampleMap.put(key, example);
            }
        }

        // Output examples to file.
        Set<String> finalExamples = new TreeSet<String>(new Comparator<String>() {
            @Override
            public int compare(String arg0, String arg1) {
                String[] forms1 = arg1.split("\\|");
                String[] forms0 = arg0.split("\\|");
                for (int i = 0; i < forms0.length; i++) {
                    int compare = keywordIndex.get(forms0[i]) - keywordIndex.get(forms1[i]);
                    if (compare != 0) return compare;
                }
                return 0;
            }
        });

        for (ExampleManager manager : positionedExamples.values()) {
            finalExamples.addAll(manager.getAll());
        }
        String realZeroType = rules.select(0);
        for (String category : finalExamples) {
            String exampleValue = exampleMap.get(category);
            //String overallCategory = rules.select(Double.valueOf(exampleValue));
            //String exampleFormat = nouns.get(overallCategory);

            out.println(locale + "\t" + exampleValue + "\t" +
                category.replace("zero", realZeroType).replace('|', '\t'));
        }
        out.flush();
    }

    private static <A, B> void put(Map<A, List<B>> map, A key, B value) {
        List<B> list = map.get(key);
        if (list == null) {
            map.put(key, list = new ArrayList<B>());
        }
        list.add(value);
    }

    static String[] units = { "second", "minute", "hour", "day", "month", "year" };

    private void getForms(CLDRFile file) {
        rules = PluralRules.forLocale(new ULocale(file.getLocaleID()));
        System.out.println(file.getLocaleID());
        for (String plural : rules.getKeywords()) {
            out.print(file.getLocaleID() + '\t' + plural + '\t' +
                rules.getSamples(plural).iterator().next());
            for (String unit : units) {
                printUnit(file, unit, plural);
                printUnit(file, unit + "-past", plural);
                printUnit(file, unit + "-future", plural);
            }
            out.println();
            out.flush();
        }
    }

    private void printUnit(CLDRFile file, String unit, String plural) {
        String path = "//ldml/units/unit[@type=\"" + unit + "\"]/unitPattern[@count=\"" + plural + "\"]";
        String value = file.getStringValue(path);
        out.print('\t');
        if (value == null) {
            System.out.println(file.getLocaleID() + " has no example for " + plural + " " + unit);
        } else {
            out.print(value);
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        PrintWriter out = FileUtilities.openUTF8Writer("/Users/jchye/Desktop", "plurals.tsv");
        GeneratePluralList generator = new GeneratePluralList(out);
        generator.loadNouns();

        Factory factory = Factory.make(CLDRPaths.MAIN_DIRECTORY, stock);
        for (String locale : factory.getAvailable()) {
            generator.getExamples(locale);
            //generator.getForms(factory.make(locale, true));
        }
        out.close();
    }
}
