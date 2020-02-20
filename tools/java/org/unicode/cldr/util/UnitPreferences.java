package org.unicode.cldr.util;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.ibm.icu.util.Freezable;

public class UnitPreferences implements Freezable<UnitPreferences> {
    Map<String, Map<String, Multimap<Set<String>, UnitPreference>>> quantityToUsageToRegionToInfo = new TreeMap<>();
    /**
     * Special class encapsulating 
     * @author markdavis
     *
     */
    public static final class UnitPreference implements Comparable<UnitPreference>{
        public final double geq;
        public final String unit;
        public final String skeleton;

        public UnitPreference(double geq, String unit, String skeleton) {
            this.geq = geq;
            this.unit = unit;
            this.skeleton = skeleton == null ? "" : skeleton;
        }

        @Override
        public int compareTo(UnitPreference o) {
            int diff = Double.compare(geq, o.geq);
            if (diff != 0) {
                return diff;
            }
            return unit.compareTo(o.unit);
        }
        @Override
        public boolean equals(Object obj) {
            return compareTo((UnitPreference)obj) == 0;
        }
        @Override
        public int hashCode() {
            return Objects.hash(geq, unit);
        }
        @Override
            public String toString() {
                return geq + ", " + unit + (skeleton.isEmpty() ? "" : ", " + skeleton);
            }
    }

    static private final Splitter SPLIT_SPACE = Splitter.on(' ').trimResults().omitEmptyStrings();

    public void add(String quantity, String usage, String regions, String geq, String skeleton, String unit) {
        Map<String, Multimap<Set<String>, UnitPreference>> usageToRegionToInfo = quantityToUsageToRegionToInfo.get(quantity);
        if (usageToRegionToInfo == null) {
            quantityToUsageToRegionToInfo.put(quantity, usageToRegionToInfo = new TreeMap<>());
        }
        Multimap<Set<String>, UnitPreference> regionToInfo = usageToRegionToInfo.get(usage);
        if (regionToInfo == null) {
            usageToRegionToInfo.put(usage, regionToInfo = LinkedHashMultimap.create());
        }
        double newGeq = geq == null || geq.isEmpty() ? 1d : Double.valueOf(geq);
        final UnitPreference newUnitPref = new UnitPreference(newGeq, unit, skeleton);
        
        regionToInfo.put(ImmutableSet.copyOf(new TreeSet<>(SPLIT_SPACE.splitToList(regions))), newUnitPref);
    }

    boolean frozen;

    @Override
    public boolean isFrozen() {
        return frozen;
    }

    @Override
    public UnitPreferences freeze() {
        if (!frozen) {
            frozen = true;
            quantityToUsageToRegionToInfo = CldrUtility.protectCollection(quantityToUsageToRegionToInfo);
        }
        return this;
    }

    @Override
    public UnitPreferences cloneAsThawed() {
        throw new UnsupportedOperationException();
    }

    /**
     * quantity => usage => region => geq => [unit, skeleton]
     * @return
     */
    public Map<String, Map<String, Multimap<Set<String>, UnitPreference>>> getData() {
        return quantityToUsageToRegionToInfo;
    }
    
    static final Joiner JOIN_SPACE = Joiner.on(' ');

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        int order = 0;
        for (Entry<String, Map<String, Multimap<Set<String>, UnitPreference>>> entry1 : quantityToUsageToRegionToInfo.entrySet()) {
            String quantity = entry1.getKey();
            for (Entry<String, Multimap<Set<String>, UnitPreference>> entry2 : entry1.getValue().entrySet()) {
                String usage = entry2.getKey();
                for (Entry<Set<String>, Collection<UnitPreference>> entry : entry2.getValue().asMap().entrySet()) {
                    Set<String> regions = entry.getKey();
                    for (UnitPreference up : entry.getValue()) {
                        System.out.println(up.unit + "\t;\t" + getPath(order++, quantity, usage, regions, up.geq, up.skeleton));
                    }
                }
            }
        }
        return "";
    }

    public String getPath(int order, String quantity, String usage, Collection<String> regions, double geq, String skeleton) {
        //      <unitPreferences category="length" usage="person" scope="small">
        // <unitPreference regions="001">centimeter</unitPreference>
        return "//supplementalData/unitPreferenceData/unitPreferences"
        + "[@category=\"" + quantity + "\"]"
            + "[@usage=\"" + usage + "\"]"
            + "/unitPreference"
            + "[@_q=\"" + order + "\"]"
            + "[@regions=\"" + JOIN_SPACE.join(regions) + "\"]"
            + (geq == 1d ? "" : "[@geq=\"" + geq + "\"]")
            + (skeleton.isEmpty() ? "" : "[@skeleton=\"" + skeleton + "\"]")
            ;
    }
}
