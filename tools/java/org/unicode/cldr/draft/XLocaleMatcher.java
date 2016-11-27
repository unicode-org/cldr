package org.unicode.cldr.draft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.util.LocaleMatcher;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

public class XLocaleMatcher {

    DistanceTable languageDesired2Supported;
    private final int threshold = 40;

    interface DistanceTable {
        int getDistance(String desiredLang, String supportedlang, Output<DistanceTable> table);
    }

    static class DistanceNode {
        final int distance;

        public DistanceNode(int distance) {
            this.distance = distance;
        }

        public IntDistanceTable getDistanceTable() {
            return null;
        }

        @Override
        public boolean equals(Object obj) {
            DistanceNode other = (DistanceNode) obj;
            return distance == other.distance;
        }
        @Override
        public int hashCode() {
            return distance;
        }
        @Override
        public String toString() {
            return "\ndistance: " + distance;
        }
    }

    static final class IntDistanceNode extends DistanceNode {
        final IntDistanceTable distanceTable;

        public IntDistanceNode(int distance, IntDistanceTable distanceTable) {
            super(distance);
            this.distanceTable = distanceTable;
        }

        public IntDistanceTable getDistanceTable() {
            return distanceTable;
        }

        @Override
        public boolean equals(Object obj) {
            IntDistanceNode other = (IntDistanceNode) obj;
            return distance == other.distance && Objects.equal(distanceTable, other.distanceTable);
        }
        @Override
        public int hashCode() {
            return distance ^ Objects.hashCode(distanceTable);
        }
        @Override
        public String toString() {
            return "\ndistance: " + distance + ", " + distanceTable;
        }

        public static DistanceNode from(int distance, IntDistanceTable otherTable) {
            return otherTable == null ? new DistanceNode(distance) : new IntDistanceNode(distance, otherTable);
        }
    }

    static class IntDistanceTable implements DistanceTable {
        private static final Id[] ids = {new Id<String>("lang", "*"), new Id<String>("script", "*"), new Id<String>("region", "*")};
        private static final Id<IntDistanceTable> cache = new Id<>("table");

        private final Id<String> id;
        private final DistanceNode[][] distanceNodes; // map from desired, supported => node

        public IntDistanceTable(StringDistanceTable source) {
            this(source, loadIds(source, 0));
        }

        private static int loadIds(StringDistanceTable source, int idNumber) {
            Id id = ids[idNumber]; // use different Id for language, script, region
            for (Entry<String, Map<String, StringDistanceNode>> e1 : source.subtables.entrySet()) {
                int desired = id.add(e1.getKey());
                for (Entry<String, StringDistanceNode> e2 : e1.getValue().entrySet()) {
                    int supported = id.add(e2.getKey());
                    StringDistanceNode oldNode = e2.getValue();
                    if (oldNode.distanceTable != null) {
                        loadIds(oldNode.distanceTable, idNumber+1);
                    }
                }
            }
            return 0;
        }

        private IntDistanceTable(StringDistanceTable source, int idNumber) { // move construction out later
            id = ids[idNumber]; // use different Id for language, script, region
            int size = id.size();
            distanceNodes = new DistanceNode[size][size];

            // fill in the values in the table
            for (Entry<String, Map<String, StringDistanceNode>> e1 : source.subtables.entrySet()) {
                int desired = id.add(e1.getKey());
                for (Entry<String, StringDistanceNode> e2 : e1.getValue().entrySet()) {
                    int supported = id.add(e2.getKey());
                    StringDistanceNode oldNode = e2.getValue();
                    IntDistanceTable otherTable = oldNode.distanceTable == null ? null 
                        : cache.intern(new IntDistanceTable(oldNode.distanceTable, idNumber+1));
                    DistanceNode node = IntDistanceNode.from(oldNode.distance, otherTable);
                    distanceNodes[desired][supported] = node;
                }
            }
            // now, to make star work, 
            // copy all the zero columns/rows down to any null value
            for (int row = 0; row < size; ++row) {
                for (int column = 0; column < size; ++column) {
                    DistanceNode value = distanceNodes[row][column];
                    if (value != null) {
                        continue;
                    }
                    value = distanceNodes[0][column];
                    if (value == null) {
                        value = distanceNodes[row][0];
                        if (value == null) {
                            value = distanceNodes[0][0];
                        }
                    }
                    distanceNodes[row][column] = value;
                }
            }
        }

        @Override
        public int getDistance(String desired, String supported, Output<DistanceTable> distanceTable) {
            final int desiredId = id.from(desired);
            final int supportedId = id.from(supported); // can optimize later
            DistanceNode value = distanceNodes[desiredId][supportedId];
            distanceTable.value = value.getDistanceTable();
            return desiredId == supportedId && (desiredId != 0 || desired.equals(supported)) ? 0 
                : value.distance;
        }

        @Override
        public boolean equals(Object obj) {
            IntDistanceTable other = (IntDistanceTable) obj;
            if (!id.equals(other.id)) {
                return false;
            };
            return Arrays.deepEquals(distanceNodes, other.distanceNodes);
        }
        @Override
        public int hashCode() {
            return id.hashCode() ^ Arrays.deepHashCode(distanceNodes);
        }

        @Override
        public String toString() {
            return abbreviate("\t", new HashMap<DistanceNode,Integer>(), new StringBuilder(id.name + ": ")).toString();
        }

        private StringBuilder abbreviate(String indent, Map<DistanceNode,Integer> cache, StringBuilder result) {
            for (int i = 0; i < distanceNodes.length; ++i) {
                DistanceNode[] row = distanceNodes[i];
                for (int j = 0; j < row.length; ++j) {
                    DistanceNode value = row[j];
                    if (value == null) {
                        continue;
                    }
                    result.append(value.distance);
                    IntDistanceTable dt = value.getDistanceTable();
                    if (dt == null) {
                        result.append(";");
                        continue;
                    }
                    Integer old = cache.get(value);
                    result.append("/");
                    if (old != null) {
                        result.append(old + ";");
                    } else {
                        final int table = cache.size();
                        cache.put(value, table);
                        result.append("\n" + indent + table + "=" + dt.id.name + ": ");
                        dt.abbreviate(indent+"\t", cache, result);
                    }
                }
            }
            return result;
        }
    }

    private static class Id<T> {
        private final Map<T, Integer> objectToInt = new HashMap<>();
        private final List<T> intToObject = new ArrayList<>();
        private final String name;

        Id(String name) {
            this.name = name;
        }

        Id(String name, T zeroValue) {
            this(name);
            add(zeroValue);
        }

        public int add(T source) {
            Integer result = objectToInt.get(source);
            if (result == null) {
                final int newResult = intToObject.size();
                objectToInt.put(source, newResult);
                intToObject.add(source);
                return newResult;
            } else {
                return result;
            }
        }

        public int from(T source) {
            Integer value = objectToInt.get(source); 
            return value == null ? 0 : value; 
        }

        public T to(int id) {
            return intToObject.get(id); 
        }

        public T intern(T source) {
            return to(add(source));
        }

        public int size() {
            return intToObject.size();
        }

        @Override
        public String toString() {
            return size() + ": " + intToObject;
        }
        @Override
        public boolean equals(Object obj) {
            Id other = (Id) obj;
            return intToObject.equals(other.intToObject);
        }
        @Override
        public int hashCode() {
            return intToObject.hashCode();
        }
    }

    private static class StringDistanceNode {
        private final int distance;
        private StringDistanceTable distanceTable;

        @Override
        public boolean equals(Object obj) {
            StringDistanceNode other = (StringDistanceNode) obj;
            return distance == other.distance && Objects.equal(distanceTable, other.distanceTable);
        }
        @Override
        public int hashCode() {
            return distance ^ Objects.hashCode(distanceTable);
        }

        StringDistanceNode(int distance) {
            this.distance = distance;
        }

        public void addSubtables(String desiredSub, String supportedSub, Reset r) {
            if (distanceTable == null) {
                distanceTable = new StringDistanceTable();
            }
            distanceTable.addSubtables(desiredSub, supportedSub, r);
        }
        @Override
        public String toString() {
            return "distance: " + distance + "\n" + distanceTable;
        }

        public void copyTables(StringDistanceTable value) {
            if (value != null) {
                distanceTable = new StringDistanceTable();
                distanceTable.copy(value);
            }
        }
    }

    public XLocaleMatcher(DistanceTable datadistancetable2) {
        languageDesired2Supported = dataDistanceTable;
    }

    private static Map newMap() {
        return new TreeMap();
    }

    private static class StringDistanceTable implements DistanceTable {
        private final Map<String, Map<String, StringDistanceNode>> subtables = newMap();

        @Override
        public boolean equals(Object obj) {
            StringDistanceTable other = (StringDistanceTable) obj;
            return subtables.equals(other.subtables);
        }
        @Override
        public int hashCode() {
            return subtables.hashCode();
        }

        public int getDistance(String desired, String supported, Output<DistanceTable> distanceTable) {
            boolean star = false;
            Map<String, StringDistanceNode> sub2 = subtables.get(desired);
            if (sub2 == null) {
                sub2 = subtables.get("*"); // <*, supported>
                star = true;
            }
            StringDistanceNode value = sub2.get(supported);   // <*/desired, supported>
            if (value == null) {
                value = sub2.get("*");  // <*/desired, *>
                if (value == null && !star) {
                    sub2 = subtables.get("*");   // <*, supported>
                    value = sub2.get(supported);
                    if (value == null) {
                        value = sub2.get("*");   // <*, *>
                    }
                }
                star = true;
            }
            distanceTable.value = value.distanceTable;
            return star & desired.equals(supported) ? 0 : value.distance;
        }

        public void copy(StringDistanceTable other) {
            for (Entry<String, Map<String, StringDistanceNode>> e1 : other.subtables.entrySet()) {
                for (Entry<String, StringDistanceNode> e2 : e1.getValue().entrySet()) {
                    StringDistanceNode value = e2.getValue();
                    StringDistanceNode subNode = addSubtable(e1.getKey(), e2.getKey(), value.distance, false);
                }
            }
        }

        StringDistanceNode addSubtable(String desired, String supported, int distance, boolean skipIfSet) {
            Map<String, StringDistanceNode> sub2 = subtables.get(desired);
            if (sub2 == null) {
                subtables.put(desired, sub2 = newMap());
            }
            StringDistanceNode oldNode = sub2.get(supported);
            if (oldNode != null) {
                if (oldNode.distance == distance || skipIfSet) {
                    return oldNode;
                } else {
                    throw new IllegalArgumentException("Overriding values for " + desired + ", " + supported
                        + ", old distance: " + oldNode.distance + ", new distance: " + distance);
                }
            }

            final StringDistanceNode newNode = new StringDistanceNode(distance);
            sub2.put(supported, newNode);
            return newNode;
        }

        private StringDistanceNode getNode(String desired, String supported) {
            Map<String, StringDistanceNode> sub2 = subtables.get(desired);
            if (sub2 == null) {
                return null;
            }
            return sub2.get(supported);
        }


        /** add table for each subitem that matches and doesn't have a table already
         */
        public void addSubtables(
            String desired, String supported, 
            Predicate<StringDistanceNode> action) {
            int count = 0;
            StringDistanceNode node = getNode(desired, supported);
            if (node == null) {
                // get the distance it would have
                Output<DistanceTable> node2 = new Output<>();
                int distance = getDistance(desired, supported, node2);
                // now add it
                node = addSubtable(desired, supported, distance, true);
                if (node2.value != null) {
                    node.copyTables((StringDistanceTable)(node2.value));
                }
            }
            action.apply(node);
        }

        public void addSubtables(String desiredLang, String supportedLang, 
            String desiredScript, String supportedScript, 
            int percentage) {

            // add to all the values that have the matching desiredLang and supportedLang
            boolean haveKeys = false;
            for (Entry<String, Map<String, StringDistanceNode>> e1 : subtables.entrySet()) {
                String key1 = e1.getKey();
                final boolean desiredIsKey = desiredLang.equals(key1);
                if (desiredIsKey || desiredLang.equals("*")) {
                    for (Entry<String, StringDistanceNode> e2 : e1.getValue().entrySet()) {
                        String key2 = e2.getKey();
                        final boolean supportedIsKey = supportedLang.equals(key2);
                        haveKeys |= (desiredIsKey && supportedIsKey);
                        if (supportedIsKey || supportedLang.equals("*")) {
                            StringDistanceNode value = e2.getValue();
                            if (value.distanceTable == null) {
                                value.distanceTable = new StringDistanceTable();
                            }
                            value.distanceTable.addSubtable(desiredScript, supportedScript, percentage, true);
                        }
                    }
                }
            }
            // now add the sequence explicitly
            StringDistanceTable dt = new StringDistanceTable();
            dt.addSubtable(desiredScript, supportedScript, percentage, false);
            Reset r = new Reset(dt);
            addSubtables(desiredLang, supportedLang, r);
        }

        public void addSubtables(String desiredLang, String supportedLang, 
            String desiredScript, String supportedScript, 
            String desiredRegion, String supportedRegion, 
            int percentage) {

            // add to all the values that have the matching desiredLang and supportedLang
            boolean haveKeys = false;
            for (Entry<String, Map<String, StringDistanceNode>> e1 : subtables.entrySet()) {
                String key1 = e1.getKey();
                final boolean desiredIsKey = desiredLang.equals(key1);
                if (desiredIsKey || desiredLang.equals("*")) {
                    for (Entry<String, StringDistanceNode> e2 : e1.getValue().entrySet()) {
                        String key2 = e2.getKey();
                        final boolean supportedIsKey = supportedLang.equals(key2);
                        haveKeys |= (desiredIsKey && supportedIsKey);
                        if (supportedIsKey || supportedLang.equals("*")) {
                            StringDistanceNode value = e2.getValue();
                            if (value.distanceTable == null) {
                                value.distanceTable = new StringDistanceTable();
                            }
                            value.distanceTable.addSubtables(desiredScript, supportedScript, desiredRegion, supportedRegion, percentage);
                        }
                    }
                }
            }
            // now add the sequence explicitly

            StringDistanceTable dt = new StringDistanceTable();
            dt.addSubtable(desiredRegion, supportedRegion, percentage, false);
            AddSub r = new AddSub(desiredScript, supportedScript, dt);
            addSubtables(desiredLang,  supportedLang,  r);  
        }

        @Override
        public String toString() {
            return toString("", new StringBuilder()).toString();
        }

        public StringBuilder toString(String indent, StringBuilder buffer) {
            for (Entry<String, Map<String, StringDistanceNode>> e1 : subtables.entrySet()) {
                for (Entry<String, StringDistanceNode> e2 : e1.getValue().entrySet()) {
                    StringDistanceNode value = e2.getValue();
                    buffer.append("\n" + indent + "\t<" + e1.getKey() + ", " + e2.getKey() + "> => " + value.distance);
                    if (value.distanceTable != null) {
                        value.distanceTable.toString(indent+"\t", buffer);
                    }
                }
            }
            return buffer;
        }
    }

    static class Reset implements Predicate<StringDistanceNode> {
        private final StringDistanceTable resetIfNotNull;
        Reset(StringDistanceTable resetIfNotNull) {
            this.resetIfNotNull = resetIfNotNull;
        }
        @Override
        public boolean apply(StringDistanceNode node) {
            if (node.distanceTable == null) {
                node.distanceTable = resetIfNotNull;
            }
            return true;
        }
    }

    static class AddSub implements Predicate<StringDistanceNode> {
        private final String desiredSub;
        private final String supportedSub;
        private final Reset r;

        AddSub(String desiredSub, String supportedSub, StringDistanceTable newSubsubtable) {
            this.r = new Reset(newSubsubtable);
            this.desiredSub = desiredSub;
            this.supportedSub = supportedSub;
        }
        @Override
        public boolean apply(StringDistanceNode node) {
            if (node == null) {
                throw new IllegalArgumentException("bad structure");
            } else {
                node.addSubtables(desiredSub, supportedSub, r);
            }
            return true;
        }
    }


    public double distance(ULocale desired, ULocale supported) {
        String desiredLang = desired.getLanguage();
        String supportedlang = supported.getLanguage();
        String desiredScript = desired.getScript();
        String supportedScript = supported.getScript();
        String desiredRegion = desired.getCountry();
        String supportedRegion = supported.getCountry();

        return distance(desiredLang, supportedlang, desiredScript, supportedScript, desiredRegion, supportedRegion);
    }

    public double distance(
        String desiredLang, String supportedlang, 
        String desiredScript, String supportedScript, 
        String desiredRegion, String supportedRegion) {
        
        Output<DistanceTable> subtable = new Output<>();

        int distance = languageDesired2Supported.getDistance(desiredLang, supportedlang, subtable);
        if (distance > threshold) {
            return 666;
        }

        distance += subtable.value.getDistance(desiredScript, supportedScript, subtable);
        if (distance > threshold) {
            return 666;
        }

        distance += subtable.value.getDistance(desiredRegion, supportedRegion, subtable);
        return distance;
    }

    public static final StringDistanceTable dataDistanceTable = new StringDistanceTable();
    static {
        Splitter bar = Splitter.on('_');
        SupplementalDataInfo sdi = CLDRConfig.getInstance().getSupplementalDataInfo();
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
                    add(dataDistanceTable, item2.get0(), item2.get1(), item2.get2());
                    System.out.println(s + "\t" + item2);
                }
                System.out.println(dataDistanceTable);
            }
        }
    }

    static public void add(StringDistanceTable languageDesired2Supported, List<String> desired, List<String> supported, int percentage) {
        int size = desired.size();
        if (size != supported.size() || size < 1 || size > 3) {
            throw new IllegalArgumentException();
        }
        final String desiredLang = desired.get(0);
        final String supportedLang = supported.get(0);
        if (size == 1) {
            languageDesired2Supported.addSubtable(desiredLang, supportedLang, percentage, false);
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

    public static void main(String[] args) {
        XLocaleMatcher localeMatcher = new XLocaleMatcher(dataDistanceTable);
        System.out.println(localeMatcher.toString());

        IntDistanceTable d = new IntDistanceTable(dataDistanceTable);
        XLocaleMatcher intLocaleMatcher = new XLocaleMatcher(d);
        System.out.println(localeMatcher.toString());

        String lastRaw = "no";
        String[] testsRaw = {"no_DE", "nb", "no", "no", "da", "zh_Hant", "zh_Hans", "en", "en_GB", "en_Cyrl", "fr"};
        ULocale last = new ULocale(lastRaw);
        final int testCount = testsRaw.length;
        ULocale[] tests = new ULocale[testCount];
        int i = 0;
        for (String testRaw : testsRaw) {
            tests[i++] = new ULocale(testRaw);
        }

        LocaleMatcher oldLocaleMatcher = new LocaleMatcher("");

        long likelyTime = 0;
        long extractTime = 0;
        long newTime = 0;
        long intTime = 0;
        long oldTime = 0;
        final int maxIterations = 1000;
        for (int iterations = maxIterations; iterations > 0; --iterations) {
            ULocale desired = last;
            int count=0;
            for (ULocale test : tests) {
                final ULocale supported = test;

                long temp = System.nanoTime();
                final ULocale desiredMax = ULocale.addLikelySubtags(desired);
                likelyTime += System.nanoTime()-temp;
                
                final ULocale supportedMax = ULocale.addLikelySubtags(supported);
                
                temp = System.nanoTime();
                String desiredLang = desired.getLanguage();
                String desiredScript = desired.getScript();
                String desiredRegion = desired.getCountry();
                extractTime += System.nanoTime()-temp;
                
                String supportedlang = supported.getLanguage();
                String supportedScript = supported.getScript();
                String supportedRegion = supported.getCountry();


                temp = System.nanoTime();
                double dist1 = localeMatcher.distance(desiredLang, supportedlang, desiredScript, supportedScript, desiredRegion, supportedRegion);
                double dist2 = localeMatcher.distance(supportedlang, desiredLang, supportedScript, desiredScript, supportedRegion, desiredRegion);
                newTime += System.nanoTime()-temp;

                temp = System.nanoTime();
                double distInt1 = intLocaleMatcher.distance(desiredLang, supportedlang, desiredScript, supportedScript, desiredRegion, supportedRegion);
                double distInt2 = intLocaleMatcher.distance(supportedlang, desiredLang, supportedScript, desiredScript, supportedRegion, desiredRegion);
                intTime += System.nanoTime()-temp;

                temp = System.nanoTime();
                double distOld1 = oldLocaleMatcher.match(desired, desiredMax, supported, supportedMax);
                double distOld2 = oldLocaleMatcher.match(supported, supportedMax, desired, desiredMax);
                oldTime += System.nanoTime()-temp;

                if (iterations == 1) {
                    System.out.println(desired + (dist1 != dist2 ? "\t => \t" : "\t <=> \t") + test
                        + "\t = \t" + dist1 
                        + "; \t" + distInt1
                        + "; \t" + 100*(1-distOld1)
                        );
                    if (dist1 != dist2) {
                        System.out.println(supported + "\t => \t" + desired
                            + "\t = \t" + dist2 
                            + "; \t" + distInt2
                            + "; \t" + 100*(1-distOld2));
                    }
                }

                desired = supported;
            }
        }
        System.out.println("oldTime:\t" + oldTime/maxIterations);
        System.out.println("newTime:\t" + newTime/maxIterations);
        System.out.println("newIntTime:\t" + intTime/maxIterations);
        System.out.println("likelyTime:\t" + likelyTime/maxIterations);
        System.out.println("extractTime:\t" + extractTime/maxIterations);
    }
}
