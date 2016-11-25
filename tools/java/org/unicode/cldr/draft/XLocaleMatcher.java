package org.unicode.cldr.draft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.util.LocaleMatcher;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

public class XLocaleMatcher {

    LocaleDistance langDistance;

    private static class Id<T> {
        private Map<T,Integer> stringToId = new HashMap<>();
        private List<T> idToString = new ArrayList<>();
        public int add(T source) {
            Integer result = stringToId.get(source);
            if (result == null) {
                final int newResult = idToString.size();
                stringToId.put(source, newResult);
                idToString.add(source);
                return newResult;
            } else {
                return result;
            }
        }
        public Integer from(T source) {
            return stringToId.get(source); 
        }
        public T to(int id) {
            return idToString.get(id); 
        }
    }

    private static class DistanceNode {
        int distance;
        DistanceTable distanceTable;

        DistanceNode(int distance) {
            this.distance = distance;
        }

        public void addSubtables(String desiredSub, String supportedSub, Reset r) {
            if (distanceTable == null) {
                distanceTable = new DistanceTable();
            }
            distanceTable.addSubtables(desiredSub, supportedSub, r);
        }
        @Override
        public String toString() {
            return "distance: " + distance + "\n" + distanceTable;
        }

        public void copyTables(DistanceNode value) {
            if (value.distanceTable != null) {
                distanceTable = new DistanceTable();
                distanceTable.copy(value.distanceTable);
            }
        }
    }

    private static class DistanceTable {
        final Map<String, Map<String, DistanceNode>> subtables = new TreeMap<>();

        int getDistance(String desired, String supported, Output<DistanceNode> distanceTable) {
            boolean star = false;
            Map<String, DistanceNode> sub2 = subtables.get(desired);
            if (sub2 == null) {
                sub2 = subtables.get("*");
                star = true;
            }
            DistanceNode value = sub2.get(supported);
            if (value == null) {
                value = sub2.get("*");
                star = true;
            }
            distanceTable.value = value;
            return star & desired.equals(supported) ? 0 :  value.distance;
        }

        public void copy(DistanceTable other) {
            for (Entry<String, Map<String, DistanceNode>> e1 : other.subtables.entrySet()) {
                for (Entry<String, DistanceNode> e2 : e1.getValue().entrySet()) {
                    DistanceNode value = e2.getValue();
                    DistanceNode subNode = addSubtable(e1.getKey(), e2.getKey(), value.distance);
                }
            }
        }

        DistanceNode addSubtable(String desired, String supported, int distance) {
            Map<String, DistanceNode> sub2 = subtables.get(desired);
            if (sub2 == null) {
                subtables.put(desired, sub2 = new TreeMap<>());
            }
            final DistanceNode newNode = new DistanceNode(distance);

            DistanceNode oldNode = sub2.put(supported, newNode);
            if (oldNode != null) {
                throw new IllegalArgumentException("Overriding values for " + desired + ", " + supported);
            }
            return newNode;
        }

        private DistanceNode getNode(String desired, String supported) {
            Map<String, DistanceNode> sub2 = subtables.get(desired);
            if (sub2 == null) {
                return null;
            }
            return sub2.get(supported);
        }


        /** add table for each subitem that matches and doesn't have a table already
         */
        public void addSubtables(String desired, String supported, Predicate<DistanceNode> action) {
            int count = 0;
            DistanceNode node = getNode(desired, supported);
            if (node == null) {
                // get the distance it would have
                Output<DistanceNode> node2 = new Output<>();
                int distance = getDistance(desired, supported, node2);
                // now add it
                node = addSubtable(desired, supported, distance);
                if (node2.value != null) {
                    node.copyTables(node2.value);
                }
            }
            action.apply(node);
        }

        @Override
        public String toString() {
            return toString("", new StringBuilder()).toString();
        }

        public StringBuilder toString(String indent, StringBuilder buffer) {
            for (Entry<String, Map<String, DistanceNode>> e1 : subtables.entrySet()) {
                for (Entry<String, DistanceNode> e2 : e1.getValue().entrySet()) {
                    DistanceNode value = e2.getValue();
                    buffer.append("\n" + indent + "\t<" + e1.getKey() + ", " + e2.getKey() + "> => " + value.distance);
                    if (value.distanceTable != null) {
                        value.distanceTable.toString(indent+"\t", buffer);
                    }
                }
            }
            return buffer;
        }

        public void addSubtables(String desiredLang, String supportedLang, String desiredScript, String supportedScript, int percentage) {

            // clone all the values that we need. 
            boolean haveKeys = false;
            for (Entry<String, Map<String, DistanceNode>> e1 : subtables.entrySet()) {
                String key1 = e1.getKey();
                final boolean desiredIsKey = desiredLang.equals(key1);
                if (desiredIsKey || desiredLang.equals("*")) {
                    for (Entry<String, DistanceNode> e2 : e1.getValue().entrySet()) {
                        String key2 = e2.getKey();
                        final boolean supportedIsKey = supportedLang.equals(key2);
                        haveKeys |= (desiredIsKey && supportedIsKey);
                        if (supportedIsKey || supportedLang.equals("*")) {
                            DistanceNode value = e2.getValue();
                            value.distanceTable = new DistanceTable();
                            value.distanceTable.addSubtable(desiredScript, supportedScript, percentage);
                        }
                    }
                }
            }
            if (!haveKeys) {
                DistanceTable dt = new DistanceTable();
                dt.addSubtable(desiredScript, supportedScript, percentage);
                Reset r = new Reset(dt);
                addSubtables(desiredLang, supportedLang, r);
            }
        }

        public void addSubtables(String desiredLang, String supportedLang, String desiredScript, String supportedScript, String desiredRegion,
            String supportedRegion, int percentage) {
            DistanceTable dt = new DistanceTable();
            dt.addSubtable(desiredRegion, supportedRegion, percentage);
            AddSub r = new AddSub(desiredScript, supportedScript, dt);
            addSubtables(desiredLang,  supportedLang,  r);  
        }
    }

    static class Reset implements Predicate<DistanceNode> {
        private final DistanceTable resetIfNotNull;
        Reset(DistanceTable resetIfNotNull) {
            this.resetIfNotNull = resetIfNotNull;
        }
        @Override
        public boolean apply(DistanceNode node) {
            if (node.distanceTable == null) {
                node.distanceTable = resetIfNotNull;
            }
            return true;
        }
    }

    static class AddSub implements Predicate<DistanceNode> {
        private final String desiredSub;
        private final String supportedSub;
        private final Reset r;

        AddSub(String desiredSub, String supportedSub, DistanceTable newSubsubtable) {
            this.r = new Reset(newSubsubtable);
            this.desiredSub = desiredSub;
            this.supportedSub = supportedSub;
        }
        @Override
        public boolean apply(DistanceNode node) {
            if (node == null) {
                throw new IllegalArgumentException("bad structure");
            } else {
                node.addSubtables(desiredSub, supportedSub, r);
            }
            return true;
        }
    }

    static final int DEFAULT_LANGUAGE_DISTANCE = 80;

    private static class LocaleDistance {
        private DistanceTable languageDesired2Supported = new DistanceTable();

        public void add(List<String> desired, List<String> supported, int percentage) {
            int size = desired.size();
            if (size != supported.size() || size < 1 || size > 3) {
                throw new IllegalArgumentException();
            }
            final String desiredLang = desired.get(0);
            final String supportedLang = supported.get(0);
            if (size == 1) {
                languageDesired2Supported.addSubtable(desiredLang, supportedLang, percentage);
            } else {
                final String desiredScript = desired.get(1);
                final String supportedScript = supported.get(1);
                if (size == 2) {
                    languageDesired2Supported.addSubtables(desiredLang, supportedLang, desiredScript, supportedScript, percentage);
                } else {
                    final String desiredRegion = desired.get(2);
                    final String supportedRegion = supported.get(2);
                    languageDesired2Supported.addSubtables(desiredLang, supportedLang, desiredScript, supportedScript, desiredRegion, supportedRegion, percentage);
                }
            }
        }

        double distance(ULocale desired, ULocale supported) {
            Output<DistanceNode> table = new Output<>();
            String desiredLang = desired.getLanguage();
            String supportedlang = supported.getLanguage();
            int distance = languageDesired2Supported.getDistance(desiredLang, supportedlang, table);

            String desiredScript = desired.getScript();
            String supportedScript = supported.getScript();
            distance += table.value.distanceTable.getDistance(desiredScript, supportedScript, table);

            String desiredRegion = desired.getCountry();
            String supportedRegion = supported.getCountry();
            distance += table.value.distanceTable.getDistance(desiredScript, supportedScript, table);
            return distance;
        }

        @Override
        public String toString() {
            return languageDesired2Supported.toString();
        }
    }

    public double distance(ULocale desired, ULocale supported) {
        return langDistance.distance(desired, supported);
    }

    @Override
    public String toString() {
        return langDistance.toString();
    }

    private XLocaleMatcher(LocaleDistance langDistance) {
        this.langDistance = langDistance;
    }

    public static XLocaleMatcher getInstance() {
        Splitter bar = Splitter.on('_');
        SupplementalDataInfo sdi = CLDRConfig.getInstance().getSupplementalDataInfo();
        LocaleDistance langDistance = new LocaleDistance();
        int last = -1;
        for (String s : sdi.getLanguageMatcherKeys()) {
            /*
            <languageMatch desired="hy" supported="ru" percent="90" oneway="true"/>

            written  [am_*_*, en_*_GB, 90, true]
            written [ay, es, 90, true]
            written [az, ru, 90, true]
            written [az_Latn, ru_Cyrl, 90, true]
             */
            List<Row.R3<List<String>, List<String>, Integer>>[] sorted = new ArrayList[3];
            sorted[0] = new ArrayList<>();
            sorted[1] = new ArrayList<>();
            sorted[2] = new ArrayList<>();

            for (R4<String, String, Integer, Boolean> info : sdi.getLanguageMatcherData(s)) {
                List<String> desired = bar.splitToList(info.get0());
                List<String> supported = bar.splitToList(info.get1());
                final int distance = 100-info.get2();
                int size = desired.size();
                sorted[size-1].add(Row.of(desired, supported, distance));
                if (info.get3() != Boolean.TRUE && !desired.equals(supported)) {
                    sorted[size-1].add(Row.of(supported, desired, distance));
                }
            }
            for (List<Row.R3<List<String>, List<String>, Integer>> item1 : sorted) {
                int debug = 0;
                for (Row.R3<List<String>, List<String>, Integer> item2 : item1) {
                    langDistance.add(item2.get0(), item2.get1(), item2.get2());
                    System.out.println(s + "\t" + item2);
                }
                System.out.println(langDistance);
            }
        }
        return new XLocaleMatcher(langDistance);
    }


    public static void main(String[] args) {
        XLocaleMatcher localeMatcher = XLocaleMatcher.getInstance();
        System.out.println(localeMatcher.toString());

        String lastRaw = "no";
        String[] testsRaw = {"nb", "no", "da", "zh_Hant", "zh_Hans"};
        ULocale last = new ULocale(lastRaw);
        ULocale[] tests = new ULocale[testsRaw.length];
        int i = 0;
        for (String testRaw : testsRaw) {
            tests[i++] = new ULocale(testRaw);
        }

        LocaleMatcher lm = new LocaleMatcher("");

        long newTime = 0;
        long oldTime = 0;
        long likelyTime = 0;
        final int maxIterations = 10000;
        for (int iterations = maxIterations; iterations > 0; --iterations) {
            ULocale desired = last;
            for (ULocale test : tests) {
                final ULocale supported = test;

                long temp = System.nanoTime();
                final ULocale desiredMax = ULocale.addLikelySubtags(desired);
                final ULocale supportedMax = ULocale.addLikelySubtags(supported);
                likelyTime += System.nanoTime()-temp;

                temp = System.nanoTime();
                double dist1 = localeMatcher.distance(desiredMax, supportedMax);
                double dist2 = localeMatcher.distance(supportedMax, desiredMax);
                newTime += System.nanoTime()-temp;

                temp = System.nanoTime();
                double distOld1 = lm.match(desired, desiredMax, supported, supportedMax);
                double distOld2 = lm.match(supported, supportedMax, desired, desiredMax);
                oldTime += System.nanoTime()-temp;

                if (iterations == 1) {
                    System.out.println(desired + (dist1 != dist2 ? "\t => \t" : "\t <=> \t") + test
                        + "\t = \t" + dist1 
                        + "; \t" + 100*(1-distOld1));
                    if (dist1 != dist2) {
                        System.out.println(supported + "\t => \t" + desired
                            + "\t = \t" + dist2 
                            + "; \t" + 100*(1-distOld2));
                    }
                }

                desired = supported;
            }
        }
        System.out.println("likelyTime: " + likelyTime/maxIterations);
        System.out.println("newTime: " + newTime/maxIterations);
        System.out.println("oldTime: " + oldTime/maxIterations);
    }
}
