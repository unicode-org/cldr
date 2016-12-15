package org.unicode.cldr.draft;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.draft.IntDistanceNode.IntDistanceTable;
import org.unicode.cldr.draft.XLikelySubtags.LSR;
import org.unicode.cldr.draft.XLocaleDistance.RegionMapper.Builder;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

public class XLocaleDistance {

    private static final int ABOVE_THRESHOLD = 99;
    static final String ANY = "�"; // make * into a large character for debugging
    private static String fixAny(String string) {
        return "*".equals(string) ? ANY : string;
    }

    // For now, get data directly from CLDR

    static final SupplementalDataInfo SDI = CLDRConfig.getInstance().getSupplementalDataInfo();

    private static List<R4<String, String, Integer, Boolean>> xGetLanguageMatcherData() {
        return SDI.getLanguageMatcherData("written");
    }

    static final Multimap<String,String> CONTAINER_TO_CONTAINED;
    static final Multimap<String,String> CONTAINER_TO_CONTAINED_FINAL;
    static {
        TreeMultimap<String, String> containerToContainedTemp = TreeMultimap.create();
        fill("001", containerToContainedTemp);
        CONTAINER_TO_CONTAINED = ImmutableMultimap.copyOf(containerToContainedTemp);
        ImmutableMultimap.Builder<String, String> containerToFinalContainedBuilder = new ImmutableMultimap.Builder<>();
        for (Entry<String, Collection<String>> entry : CONTAINER_TO_CONTAINED.asMap().entrySet()) {
            String container = entry.getKey();
            for (String contained : entry.getValue()) {
                if (SDI.getContained(contained) == null) {
                    containerToFinalContainedBuilder.put(container, contained);
                }
            }
        }
        CONTAINER_TO_CONTAINED_FINAL = containerToFinalContainedBuilder.build();
    }
    // TODO make this a single pass
    private static Collection<String> fill(String region, Multimap<String, String> toAddTo) {
        Set<String> contained = SDI.getContained(region);
        if (contained != null) {
            toAddTo.putAll(region, contained);
            for (String subregion : contained) {
                toAddTo.putAll(region, fill(subregion, toAddTo));
            }
            return toAddTo.get(region);
        }
        return Collections.emptySet();
    }

    final static private Set<String> ALL_FINAL_REGIONS = ImmutableSet.copyOf(CONTAINER_TO_CONTAINED_FINAL.get("001"));

    // end of data from CLDR

    private final DistanceTable languageDesired2Supported;
    private final RegionMapper regionMapper;
    private final int threshold = 40;

    static abstract class DistanceTable {
        abstract int getDistance(String desiredLang, String supportedlang, Output<DistanceTable> table, boolean starEquals);
        abstract Set<String> getCloser(int threshold);
    }

    static class DistanceNode {
        final int distance;

        public DistanceNode(int distance) {
            this.distance = distance;
        }

        public DistanceTable getDistanceTable() {
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

    private interface IdMapper<K,V> {
        public V toId(K source);
    }

    static class IdMakerFull<T> implements IdMapper<T,Integer> {
        private final Map<T, Integer> objectToInt = new HashMap<>();
        private final List<T> intToObject = new ArrayList<>();
        final String name; // for debugging

        IdMakerFull(String name) {
            this.name = name;
        }

        IdMakerFull(String name, T zeroValue) {
            this(name);
            add(zeroValue);
        }

        public Integer add(T source) {
            Integer result = objectToInt.get(source);
            if (result == null) {
                Integer newResult = intToObject.size();
                objectToInt.put(source, newResult);
                intToObject.add(source);
                return newResult;
            } else {
                return result;
            }
        }

        public Integer toId(T source) {
            Integer value = objectToInt.get(source); 
            return value == null ? 0 : value; 
        }

        public T fromId(int id) {
            return intToObject.get(id); 
        }

        public T intern(T source) {
            return fromId(add(source));
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
            IdMakerFull<T> other = (IdMakerFull) obj;
            return intToObject.equals(other.intToObject);
        }
        @Override
        public int hashCode() {
            return intToObject.hashCode();
        }
    }

    static class StringDistanceNode {
        final int distance;
        StringDistanceTable distanceTable;

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

    public XLocaleDistance(DistanceTable datadistancetable2, RegionMapper regionMapper) {
        languageDesired2Supported = datadistancetable2;
        this.regionMapper = regionMapper;
    }

    private static Map newMap() { // for debugging
        return new TreeMap();
    }

    static class StringDistanceTable extends DistanceTable {
        final Map<String, Map<String, StringDistanceNode>> subtables = newMap();

        @Override
        public boolean equals(Object obj) {
            StringDistanceTable other = (StringDistanceTable) obj;
            return subtables.equals(other.subtables);
        }
        @Override
        public int hashCode() {
            return subtables.hashCode();
        }

        public int getDistance(String desired, String supported, Output<DistanceTable> distanceTable, boolean starEquals) {
            boolean star = false;
            Map<String, StringDistanceNode> sub2 = subtables.get(desired);
            if (sub2 == null) {
                sub2 = subtables.get(ANY); // <*, supported>
                star = true;
            }
            StringDistanceNode value = sub2.get(supported);   // <*/desired, supported>
            if (value == null) {
                value = sub2.get(ANY);  // <*/desired, *>
                if (value == null && !star) {
                    sub2 = subtables.get(ANY);   // <*, supported>
                    value = sub2.get(supported);
                    if (value == null) {
                        value = sub2.get(ANY);   // <*, *>
                    }
                }
                star = true;
            }
            if (distanceTable != null) {
                distanceTable.value = value.distanceTable;
            }
            return starEquals && star & desired.equals(supported) ? 0 : value.distance;
        }

        public void copy(StringDistanceTable other) {
            for (Entry<String, Map<String, StringDistanceNode>> e1 : other.subtables.entrySet()) {
                for (Entry<String, StringDistanceNode> e2 : e1.getValue().entrySet()) {
                    StringDistanceNode value = e2.getValue();
                    StringDistanceNode subNode = addSubtable(e1.getKey(), e2.getKey(), value.distance);
                }
            }
        }

        StringDistanceNode addSubtable(String desired, String supported, int distance) {
            Map<String, StringDistanceNode> sub2 = subtables.get(desired);
            if (sub2 == null) {
                subtables.put(desired, sub2 = newMap());
            }
            StringDistanceNode oldNode = sub2.get(supported);
            if (oldNode != null) {
                return oldNode;
            }

            final StringDistanceNode newNode = new StringDistanceNode(distance);
            sub2.put(supported, newNode);
            return newNode;
        }

        /**
         * Return null if value doesn't exist
         */
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
                int distance = getDistance(desired, supported, node2, true);
                // now add it
                node = addSubtable(desired, supported, distance);
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
                if (desiredIsKey || desiredLang.equals(ANY)) {
                    for (Entry<String, StringDistanceNode> e2 : e1.getValue().entrySet()) {
                        String key2 = e2.getKey();
                        final boolean supportedIsKey = supportedLang.equals(key2);
                        haveKeys |= (desiredIsKey && supportedIsKey);
                        if (supportedIsKey || supportedLang.equals(ANY)) {
                            StringDistanceNode value = e2.getValue();
                            if (value.distanceTable == null) {
                                value.distanceTable = new StringDistanceTable();
                            }
                            value.distanceTable.addSubtable(desiredScript, supportedScript, percentage);
                        }
                    }
                }
            }
            // now add the sequence explicitly
            StringDistanceTable dt = new StringDistanceTable();
            dt.addSubtable(desiredScript, supportedScript, percentage);
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
                if (desiredIsKey || desiredLang.equals(ANY)) {
                    for (Entry<String, StringDistanceNode> e2 : e1.getValue().entrySet()) {
                        String key2 = e2.getKey();
                        final boolean supportedIsKey = supportedLang.equals(key2);
                        haveKeys |= (desiredIsKey && supportedIsKey);
                        if (supportedIsKey || supportedLang.equals(ANY)) {
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
            dt.addSubtable(desiredRegion, supportedRegion, percentage);
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
                    buffer.append("\n" + indent + "<" + e1.getKey() + ", " + e2.getKey() + "> => " + value.distance);
                    if (value.distanceTable != null) {
                        value.distanceTable.toString(indent+"\t", buffer);
                    }
                }
            }
            return buffer;
        }
        @Override
        public Set<String> getCloser(int threshold) {
            Set<String> result = new HashSet<>();
            for (Entry<String, Map<String, StringDistanceNode>> e1 : subtables.entrySet()) {
                String desired = e1.getKey();
                for (Entry<String, StringDistanceNode> e2 : e1.getValue().entrySet()) {
                    if (e2.getValue().distance < threshold) {
                        result.add(desired);
                        break;
                    }
                }
            }
            return result;
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


    public int distance(ULocale desired, ULocale supported) {
        return distance(desired.getLanguage(), supported.getLanguage(), 
            desired.getScript(), supported.getScript(), 
            desired.getCountry(), supported.getCountry());
    }

    public int distance(LSR desired, LSR supported) {
        return distance(desired.language, supported.language, 
            desired.script, supported.script, 
            desired.region, supported.region);
    }

    public int distance(
        String desiredLang, String supportedlang, 
        String desiredScript, String supportedScript, 
        String desiredRegion, String supportedRegion) {

        Output<DistanceTable> subtable = new Output<>();

        int distance = languageDesired2Supported.getDistance(desiredLang, supportedlang, subtable, true);
        if (distance > threshold) {
            return ABOVE_THRESHOLD;
        }

        distance += subtable.value.getDistance(desiredScript, supportedScript, subtable, true);
        if (distance > threshold) {
            return ABOVE_THRESHOLD;
        }

        if (desiredRegion.equals(supportedRegion)) {
            return distance;
        }

        // From here on we know the regions are not equal

        final String desiredPartition = regionMapper.toId(desiredRegion);
        final String supportedPartition = regionMapper.toId(supportedRegion);
        int subdistance;

        // check for macros. If one is found, we take the maximum distance
        // this could be optimized by adding some more structure, but probably not worth it.

        Collection<String> desiredPartitions = desiredPartition.isEmpty() ? regionMapper.macroToPartitions.get(desiredRegion) : null;
        Collection<String> supportedPartitions = supportedPartition.isEmpty() ? regionMapper.macroToPartitions.get(supportedRegion) : null;
        if (desiredPartitions != null || supportedPartitions != null) {
            subdistance = 0;
            // make the code simple for now
            if (desiredPartitions == null) {
                desiredPartitions = Collections.singleton(desiredPartition);
            }
            if (supportedPartitions == null) {
                supportedPartitions = Collections.singleton(supportedPartition);
            }

            for (String desiredPartition2 : desiredPartitions) {
                for (String supportedPartition2 : supportedPartitions) {
                    int tempSubdistance = subtable.value.getDistance(desiredPartition2, supportedPartition2, null, false);
                    if (subdistance < tempSubdistance) {
                        subdistance = tempSubdistance;
                    }
                }
            }
        } else {
            subdistance = subtable.value.getDistance(desiredPartition.toString(), supportedPartition.toString(), null, false);
        }
        distance += subdistance;
        return distance;
    }

    public static final StringDistanceTable DEFAULT_DISTANCE_TABLE;
    public static final RegionMapper DEFAULT_REGION_MAPPER;

    static final boolean PRINT_OVERRIDES = false;

    static {
        String[][] variableOverrides = {
            {"$enUS", "AS|GU|MH|MP|PR|UM|US|VI"},

            {"$cnsar", "HK|MO"},

            {"$americas", "019"},

            {"$maghreb", "MA|DZ|TN|LY|MR|EH"},
        };
        String[] paradigmRegions = {
            "en", "en-GB", "es", "es-419", "pt-BR", "pt-PT"
        };
        String[][] regionRuleOverrides = {
            {"ar_*_$maghreb", "ar_*_$maghreb", "96"},
            {"ar_*_$!maghreb", "ar_*_$!maghreb", "96"},
            {"ar_*_*", "ar_*_*", "95"},

            {"en_*_$enUS", "en_*_$enUS", "96"},
            {"en_*_$!enUS", "en_*_$!enUS", "96"},
            {"en_*_*", "en_*_*", "95"},

            {"es_*_$americas", "es_*_$americas", "96"},
            {"es_*_$!americas", "es_*_$!americas", "96"},
            {"es_*_*", "es_*_*", "95"},

            {"pt_*_$americas", "pt_*_$americas", "96"},
            {"pt_*_$!americas", "pt_*_$!americas", "96"},
            {"pt_*_*", "pt_*_*", "95"},

            {"zh_Hant_$cnsar", "zh_Hant_$cnsar", "96"},
            {"zh_Hant_$!cnsar", "zh_Hant_$!cnsar", "96"},
            {"zh_Hant_*", "zh_Hant_*", "95"},

            {"*_*_*", "*_*_*", "96"},
        };

        Builder rmb = new RegionMapper.Builder().addParadigms(paradigmRegions);
        for (String[] variableRule : variableOverrides) {
            rmb.add(variableRule[0], variableRule[1]);
            if (PRINT_OVERRIDES) System.out.println("<matchVariable groupVariable=\"" + variableRule[0]
                + "\" groupValue=\""
                + variableRule[1]
                    + "\"/>");
        }


        DEFAULT_REGION_MAPPER = rmb.build();
        DEFAULT_DISTANCE_TABLE = new StringDistanceTable();

        Splitter bar = Splitter.on('_');

        List<Row.R3<List<String>, List<String>, Integer>>[] sorted = new ArrayList[3];
        sorted[0] = new ArrayList<>();
        sorted[1] = new ArrayList<>();
        sorted[2] = new ArrayList<>();

        // sort the rules so that the language-only are first, then the language-script, and finally the language-script-region.
        for (R4<String, String, Integer, Boolean> info : xGetLanguageMatcherData()) {
            List<String> desired = bar.splitToList(info.get0());
            List<String> supported = bar.splitToList(info.get1());
            final int distance = 100-info.get2();
            int size = desired.size();

            // for now, skip size == 3
            if (size == 3) continue;

            sorted[size-1].add(Row.of(desired, supported, distance));
            if (info.get3() != Boolean.TRUE && !desired.equals(supported)) {
                sorted[size-1].add(Row.of(supported, desired, distance));
            }
        }

        for (List<Row.R3<List<String>, List<String>, Integer>> item1 : sorted) {
            int debug = 0;
            for (Row.R3<List<String>, List<String>, Integer> item2 : item1) {
                add(DEFAULT_DISTANCE_TABLE, item2.get0(), item2.get1(), item2.get2());
            }
        }

        // add new size=3
        for (String[] rule : regionRuleOverrides) {
            if (PRINT_OVERRIDES) System.out.println("<languageMatch  desired=\""
                + rule[0]
                    + "\" supported=\""
                    + rule[1]
                        + "\"   percent=\""
                        + rule[2]
                            + "\"/>");
            if (rule[0].equals("en_*_*") || rule[1].equals("*_*_*")) {
                int debug = 0;
            }
            List<String> desiredBase = new ArrayList<>(bar.splitToList(rule[0]));
            List<String> supportedBase = new ArrayList<>(bar.splitToList(rule[1]));
            Integer distance = 100-Integer.parseInt(rule[2]);

            Collection<String> desiredRegions = DEFAULT_REGION_MAPPER.getIdsFromVariable(desiredBase.get(2));
            if (desiredRegions.isEmpty()) {
                throw new IllegalArgumentException("Bad region variable: " + desiredBase.get(2));
            }
            Collection<String> supportedRegions = DEFAULT_REGION_MAPPER.getIdsFromVariable(supportedBase.get(2));
            if (supportedRegions.isEmpty()) {
                throw new IllegalArgumentException("Bad region variable: " + supportedBase.get(2));
            }
            for (String desiredRegion2 : desiredRegions) {
                desiredBase.set(2, desiredRegion2.toString()); // fix later
                for (String supportedRegion2 : supportedRegions) {
                    supportedBase.set(2, supportedRegion2.toString()); // fix later
                    add(DEFAULT_DISTANCE_TABLE, desiredBase, supportedBase, distance);
                    add(DEFAULT_DISTANCE_TABLE, supportedBase, desiredBase, distance);
                }
            }
        }
        //add(DEFAULT_DISTANCE_TABLE, new ArrayList<>(bar.splitToList("*_*_*")), new ArrayList<>(bar.splitToList("*_*_*")), 4);

        if (PRINT_OVERRIDES) {
            System.out.println(DEFAULT_REGION_MAPPER);
            System.out.println(DEFAULT_DISTANCE_TABLE);
        }
    }

    static public void add(StringDistanceTable languageDesired2Supported, List<String> desired, List<String> supported, int percentage) {
        int size = desired.size();
        if (size != supported.size() || size < 1 || size > 3) {
            throw new IllegalArgumentException();
        }
        final String desiredLang = fixAny(desired.get(0));
        final String supportedLang = fixAny(supported.get(0));
        if (size == 1) {
            languageDesired2Supported.addSubtable(desiredLang, supportedLang, percentage);
        } else {
            final String desiredScript = fixAny(desired.get(1));
            final String supportedScript = fixAny(supported.get(1));
            if (size == 2) {
                languageDesired2Supported.addSubtables(desiredLang, supportedLang, desiredScript, supportedScript, percentage);
            } else {
                final String desiredRegion = fixAny(desired.get(2));
                final String supportedRegion = fixAny(supported.get(2));
                languageDesired2Supported.addSubtables(desiredLang, supportedLang, desiredScript, supportedScript, desiredRegion, supportedRegion, percentage);
            }
        }
    }

    @Override
    public String toString() {
        return regionMapper + "\n" + languageDesired2Supported;
    }

    private static final XLocaleDistance DEFAULT = new XLocaleDistance(DEFAULT_DISTANCE_TABLE, DEFAULT_REGION_MAPPER);

    public static XLocaleDistance getDefault() {
        return DEFAULT;
    }

    public static XLocaleDistance createDefaultInt() {
        IntDistanceTable d = new IntDistanceTable(DEFAULT_DISTANCE_TABLE);
        return new XLocaleDistance(d, DEFAULT_REGION_MAPPER);
    }

    static Set<String> getContainingMacrosFor(Collection<String> input, Set<String> output) {
        output.clear();
        for (Entry<String, Collection<String>> entry : CONTAINER_TO_CONTAINED.asMap().entrySet()) {
            if (input.containsAll(entry.getValue())) { // example; if all southern Europe are contained, then add S. Europe
                output.add(entry.getKey());
            }
        }
        return output;
    }

    static class RegionMapper implements IdMapper<String,String> { 
        /**
         * Used for processing rules. At the start we have a variable setting like $A1=US|CA|MX. We generate a mapping from $A1 to a set of partitions {P1, P2}
         * When we hit a rule that contains a variable, we replace that rule by multiple rules for the partitions.
         */
        final Multimap<String,String> variableToPartition;
        /**
         * Used for executing the rules. We map a region to a partition before processing.
         */
        final Map<String,String> regionToPartition;
        /**
         * Used to support es_419 compared to es_AR, etc.
         * @param variableToPartitionIn
         * @param regionToPartitionIn
         */
        final Multimap<String,String> macroToPartitions;
        /**
         * Used to get the paradigm region for a cluster, if there is one
         */
        final ImmutableSet<ULocale> paradigms;

        private RegionMapper(
            Multimap<String, String> variableToPartitionIn,
            Map<String, String> regionToPartitionIn,
            Multimap<String,String> macroToPartitionsIn,
            Set<ULocale> paradigmsIn) {
            variableToPartition = ImmutableMultimap.copyOf(variableToPartitionIn);
            regionToPartition = ImmutableMap.copyOf(regionToPartitionIn);
            macroToPartitions = ImmutableMultimap.copyOf(macroToPartitionsIn);
            paradigms = ImmutableSet.copyOf(paradigmsIn);
        }

        public String toId(String region) {
            String result = regionToPartition.get(region);
            return result == null ? "" : result;
        }

        public Collection<String> getIdsFromVariable(String variable) {
            if (variable.equals("*")) {
                return Collections.singleton("*");
            }
            Collection<String> result = variableToPartition.get(variable);
            if (result == null || result.isEmpty()) {
                throw new IllegalArgumentException("Variable not defined: " + variable);
            }
            return result;
        }

        public Set<String> regions() {
            return regionToPartition.keySet();
        }

        public Set<String> variables() {
            return variableToPartition.keySet();
        }

        @Override
        public String toString() {
            TreeMultimap<String, String> partitionToVariables = Multimaps.invertFrom(variableToPartition, TreeMultimap.create());
            TreeMultimap<String, String> partitionToRegions = TreeMultimap.create();
            for (Entry<String, String> e : regionToPartition.entrySet()) {
                partitionToRegions.put(e.getValue(), e.getKey());
            }
            StringBuilder buffer = new StringBuilder();
            buffer.append("Partition ➠ Variables ➠ Regions (final)");
            for (Entry<String, Collection<String>> e : partitionToVariables.asMap().entrySet()) {
                buffer.append('\n');
                buffer.append(e.getKey() + "\t" + e.getValue() + "\t" + partitionToRegions.get(e.getKey()));
            }
            buffer.append("\nMacro ➠ Partitions");
            for (Entry<String, Collection<String>> e : macroToPartitions.asMap().entrySet()) {
                buffer.append('\n');
                buffer.append(e.getKey() + "\t" + e.getValue());
            }

            return buffer.toString();
        }

        static class Builder {
            final private Multimap<String, String> regionToRawPartition = TreeMultimap.create();
            final private RegionSet regionSet = new RegionSet();
            final private Set<ULocale> paradigms = new LinkedHashSet<>();

            void add(String variable, String barString) {
                Set<String> tempRegions = regionSet.parseSet(barString);

                for (String region : tempRegions) {
                    regionToRawPartition.put(region, variable);
                }

                // now add the inverse variable

                Set<String> inverse = regionSet.inverse();
                String inverseVariable = "$!" + variable.substring(1);
                for (String region : inverse) {
                    regionToRawPartition.put(region, inverseVariable);
                }
            }

            public Builder addParadigms(String... paradigmRegions) {
                for (String paradigm : paradigmRegions) {
                    paradigms.add(new ULocale(paradigm));
                }
                return this;
            }

            RegionMapper build() {
                final IdMakerFull<Collection<String>> id = new IdMakerFull<>("partition");
                Multimap<String,String> variableToPartitions = TreeMultimap.create(); 
                Map<String,String> regionToPartition = new TreeMap<>();
                Multimap<String,String> partitionToRegions = TreeMultimap.create();

                for (Entry<String, Collection<String>> e : regionToRawPartition.asMap().entrySet()) {
                    final String region = e.getKey();
                    final Collection<String> rawPartition = e.getValue();
                    String partition = String.valueOf((char)('α' + id.add(rawPartition)));

                    regionToPartition.put(region, partition);
                    partitionToRegions.put(partition, region);

                    for (String variable : rawPartition) {
                        variableToPartitions.put(variable, partition);
                    }
                }

                // we get a mapping of each macro to the partitions it intersects with
                Multimap<String,String> macroToPartitions = TreeMultimap.create();
                for (Entry<String, Collection<String>> e : CONTAINER_TO_CONTAINED.asMap().entrySet()) {
                    String macro = e.getKey();
                    for (Entry<String, Collection<String>> e2 : partitionToRegions.asMap().entrySet()) {
                        String partition = e2.getKey();
                        if (!Collections.disjoint(e.getValue(), e2.getValue())) {
                            macroToPartitions.put(macro, partition);
                        }
                    }
                }

                return new RegionMapper(
                    variableToPartitions,
                    regionToPartition,
                    macroToPartitions,
                    paradigms);
            }
        }
    }

    /**
     * Parses a string of regions like "US|005-BR" and produces a set of resolved regions. 
     * All macroregions are fully resolved to sets of non-macro regions.
     * <br>Syntax is simple for now:
     * <pre>regionSet := region ([-|] region)*</pre>
     * No precedence, so "x|y-y|z" is (((x union y) minus y) union z) = x union z, NOT x
     */
    private static class RegionSet {
        private enum Operation {add, remove}
        // temporaries used in processing
        final private Set<String> tempRegions = new TreeSet<>();
        private Operation operation = null;

        private Set<String> parseSet(String barString) {
            operation = Operation.add;
            int last = 0;
            tempRegions.clear();
            int i = 0;
            for (; i < barString.length(); ++i) {
                char c = barString.charAt(i); // UTF16 is ok, since syntax is only ascii
                switch(c) {
                case '|':
                    add(barString, last, i);
                    last = i+1;
                    operation = Operation.add; 
                    break;
                case '-': 
                    add(barString, last, i);
                    last = i+1;
                    operation = Operation.remove; 
                    break;
                }
            }
            add(barString, last, i);
            return tempRegions;
        }

        private Set<String> inverse() {
            TreeSet<String> result = new TreeSet<>(ALL_FINAL_REGIONS);
            result.removeAll(tempRegions);
            return result;
        }

        private void add(String barString, int last, int i) {
            if (i > last) {
                String region = barString.substring(last,i);
                changeSet(operation, region);
            }
        }

        private void changeSet(Operation operation, String region) {
            Collection<String> contained = CONTAINER_TO_CONTAINED_FINAL.get(region);
            if (contained != null && !contained.isEmpty()) {
                if (Operation.add == operation) {
                    tempRegions.addAll(contained);
                } else {
                    tempRegions.removeAll(contained);
                }
            } else if (Operation.add == operation) {
                tempRegions.add(region);
            } else {
                tempRegions.remove(region);
            }
        }
    }

    public static <K,V> Multimap<K,V> invertMap(Map<V,K> map) {
        return Multimaps.invertFrom(Multimaps.forMap(map), ArrayListMultimap.create());
    }

    public static void main(String[] args) {
//        for (Entry<String, Collection<String>> entry : containerToContained.asMap().entrySet()) {
//            System.out.println(entry.getKey() + "\t⥢" + entry.getValue() + "; " + containerToFinalContained.get(entry.getKey()));
//        }
//        final Multimap<String,String> regionToMacros = ImmutableMultimap.copyOf(Multimaps.invertFrom(containerToContained, TreeMultimap.create()));
//        for (Entry<String, Collection<String>> entry : regionToMacros.asMap().entrySet()) {
//            System.out.println(entry.getKey() + "\t⥤ " + entry.getValue());
//        }
        System.out.println(getDefault().toString());
    }

    public Set<ULocale> getParadigms() {
        return regionMapper.paradigms;
    }
}
