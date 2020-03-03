package org.unicode.cldr.util;

import java.util.Collection;
import java.util.LinkedHashMap;
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
import com.ibm.icu.impl.locale.XCldrStub.ImmutableMap;
import com.ibm.icu.util.Freezable;

public class UnitPreferences implements Freezable<UnitPreferences> {
    Map<String, Map<String, Multimap<Set<String>, UnitPreference>>> quantityToUsageToRegionsToInfo = new TreeMap<>();
    Set<String> usages = new TreeSet<>();

    /**
     * Special class encapsulating 
     * @author markdavis
     *
     */
    public static final class UnitPreference implements Comparable<UnitPreference>{
        public final Rational geq;
        public final String unit;
        public final String skeleton;

        public UnitPreference(Rational geq, String unit, String skeleton) {
            this.geq = geq;
            this.unit = unit;
            this.skeleton = skeleton == null ? "" : skeleton;
        }

        @Override
        public int compareTo(UnitPreference o) {
            int diff = geq.compareTo(o.geq);
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
        public String toString(String baseUnit) {
            return geq + (baseUnit == null ? "": " " + baseUnit) + ", " + unit + (skeleton.isEmpty() ? "" : ", " + skeleton);
        }
        public String toString() {
            return toString(null);
        }
    }

    static private final Splitter SPLIT_SPACE = Splitter.on(' ').trimResults().omitEmptyStrings();
    static public Splitter SPLIT_AND = Splitter.on("-and-");

    public void add(String quantity, String usage, String regions, String geq, String skeleton, String unit) {
        usages.add(usage);
        Map<String, Multimap<Set<String>, UnitPreference>> usageToRegionsToInfo = quantityToUsageToRegionsToInfo.get(quantity);
        if (usageToRegionsToInfo == null) {
            quantityToUsageToRegionsToInfo.put(quantity, usageToRegionsToInfo = new TreeMap<>());
        }
        Multimap<Set<String>, UnitPreference> regionsToInfo = usageToRegionsToInfo.get(usage);
        if (regionsToInfo == null) {
            usageToRegionsToInfo.put(usage, regionsToInfo = LinkedHashMultimap.create());
        }
        Rational newGeq = geq == null || geq.isEmpty() ? Rational.ONE : Rational.of(geq);
        final UnitPreference newUnitPref = new UnitPreference(newGeq, unit, skeleton);

        regionsToInfo.put(ImmutableSet.copyOf(new TreeSet<>(SPLIT_SPACE.splitToList(regions))), newUnitPref);
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
            quantityToUsageToRegionsToInfo = CldrUtility.protectCollection(quantityToUsageToRegionsToInfo);
            usages = ImmutableSet.copyOf(usages);
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
        return quantityToUsageToRegionsToInfo;
    }

    static final Joiner JOIN_SPACE = Joiner.on(' ');

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        int order = 0;
        for (Entry<String, Map<String, Multimap<Set<String>, UnitPreference>>> entry1 : quantityToUsageToRegionsToInfo.entrySet()) {
            String quantity = entry1.getKey();
            for (Entry<String, Multimap<Set<String>, UnitPreference>> entry2 : entry1.getValue().entrySet()) {
                String usage = entry2.getKey();
                for (Entry<Set<String>, Collection<UnitPreference>> entry : entry2.getValue().asMap().entrySet()) {
                    Set<String> regions = entry.getKey();
                    for (UnitPreference up : entry.getValue()) {
                        buffer.append("\n" + up.unit + "\t;\t" + getPath(order++, quantity, usage, regions, up.geq, up.skeleton));
                    }
                }
            }
        }
        return buffer.toString();
    }

    public String getPath(int order, String quantity, String usage, Collection<String> regions, Rational geq, String skeleton) {
        //      <unitPreferences category="length" usage="person" scope="small">
        // <unitPreference regions="001">centimeter</unitPreference>
        return "//supplementalData/unitPreferenceData/unitPreferences"
        + "[@category=\"" + quantity + "\"]"
        + "[@usage=\"" + usage + "\"]"
        + "/unitPreference"
        + "[@_q=\"" + order + "\"]"
        + "[@regions=\"" + JOIN_SPACE.join(regions) + "\"]"
        + (geq == Rational.ONE ? "" : "[@geq=\"" + geq + "\"]")
        + (skeleton.isEmpty() ? "" : "[@skeleton=\"" + skeleton + "\"]")
        ;
    }

    /**
     * Returns the data converted to single regions, and using base units
     * @return
     */
    public Map<String, Map<String, Map<String, UnitPreference>>> getFastMap(UnitConverter converter) {
        Map<String, Map<String, Map<String, UnitPreference>>> result = new LinkedHashMap<>();
        for (Entry<String, Map<String, Multimap<Set<String>, UnitPreference>>> entry1 : quantityToUsageToRegionsToInfo.entrySet()) {
            String quantity = entry1.getKey();
            Map<String, Map<String, UnitPreference>> result2 = new LinkedHashMap<>();
            result.put(quantity, result2);

            for (Entry<String, Multimap<Set<String>, UnitPreference>> entry2 : entry1.getValue().entrySet()) {
                String usage = entry2.getKey();
                Map<String, UnitPreference> result3 = new LinkedHashMap<>();
                result2.put(usage, result3);
                for (Entry<Set<String>, Collection<UnitPreference>> entry : entry2.getValue().asMap().entrySet()) {
                    Set<String> regions = entry.getKey();
                    for (UnitPreference up : entry.getValue()) {
                        String unit = SPLIT_AND.split(up.unit).iterator().next(); // first unit
                        quantity = converter.getQuantityFromUnit(unit, false);
                        String baseUnit = converter.getBaseUnitFromQuantity(quantity);
                        Rational geq = converter.parseRational(String.valueOf(up.geq));
                        Rational value = converter.convert(geq, unit, baseUnit, false);
                        if (value.equals(Rational.NaN)) {
                            converter.convert(geq, unit, baseUnit, true); // debug
                        }
                        UnitPreference up2 = new UnitPreference(value, up.unit, up.skeleton);
                        for (String region : regions) {
                            result3.put(region, up2);
                        }
                    }
                }
            }
        }
        return ImmutableMap.copyOf(result);
    }

    public Set<String> getUsages() {
        return usages;
    }
}
