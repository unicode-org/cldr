package org.unicode.cldr.util;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.ibm.icu.util.Freezable;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.util.UnitConverter.ConversionInfo;

public class UnitPreferences implements Freezable<UnitPreferences> {
    Map<String, Map<String, Multimap<Set<String>, UnitPreference>>> quantityToUsageToRegionsToInfo =
            new TreeMap<>();
    Set<String> usages = new TreeSet<>();

    /**
     * Special class encapsulating
     *
     * @author markdavis
     */
    public static final class UnitPreference implements Comparable<UnitPreference> {
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
            return compareTo((UnitPreference) obj) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(geq, unit);
        }

        public String toString(String baseUnit) {
            return geq
                    + (baseUnit == null ? "" : " " + baseUnit)
                    + ", "
                    + unit
                    + (skeleton.isEmpty() ? "" : ", " + skeleton);
        }

        @Override
        public String toString() {
            return toString(null);
        }
    }

    private static final Splitter SPLIT_SPACE = Splitter.on(' ').trimResults().omitEmptyStrings();
    public static Splitter SPLIT_AND = Splitter.on("-and-");

    public void add(
            String quantity,
            String usage,
            String regions,
            String geq,
            String skeleton,
            String unit) {
        usages.add(usage);
        Map<String, Multimap<Set<String>, UnitPreference>> usageToRegionsToInfo =
                quantityToUsageToRegionsToInfo.get(quantity);
        if (usageToRegionsToInfo == null) {
            quantityToUsageToRegionsToInfo.put(quantity, usageToRegionsToInfo = new TreeMap<>());
        }
        Multimap<Set<String>, UnitPreference> regionsToInfo = usageToRegionsToInfo.get(usage);
        if (regionsToInfo == null) {
            usageToRegionsToInfo.put(usage, regionsToInfo = LinkedHashMultimap.create());
        }
        Rational newGeq = geq == null || geq.isEmpty() ? Rational.ONE : Rational.of(geq);
        final UnitPreference newUnitPref = new UnitPreference(newGeq, unit, skeleton);

        final ImmutableSet<String> regionSet =
                ImmutableSet.copyOf(new TreeSet<>(SPLIT_SPACE.splitToList(regions)));
        boolean old = regionsToInfo.put(regionSet, newUnitPref);
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
            quantityToUsageToRegionsToInfo =
                    CldrUtility.protectCollection(quantityToUsageToRegionsToInfo);
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
     *
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
        for (Entry<String, Map<String, Multimap<Set<String>, UnitPreference>>> entry1 :
                quantityToUsageToRegionsToInfo.entrySet()) {
            String quantity = entry1.getKey();
            for (Entry<String, Multimap<Set<String>, UnitPreference>> entry2 :
                    entry1.getValue().entrySet()) {
                String usage = entry2.getKey();
                for (Entry<Set<String>, Collection<UnitPreference>> entry :
                        entry2.getValue().asMap().entrySet()) {
                    Set<String> regions = entry.getKey();
                    for (UnitPreference up : entry.getValue()) {
                        buffer.append(
                                "\n"
                                        + up.unit
                                        + "\t;\t"
                                        + getPath(
                                                order++,
                                                quantity,
                                                usage,
                                                regions,
                                                up.geq,
                                                up.skeleton));
                    }
                }
            }
        }
        return buffer.toString();
    }

    public String getPath(
            int order,
            String quantity,
            String usage,
            Collection<String> regions,
            Rational geq,
            String skeleton) {
        //      <unitPreferences category="length" usage="person" scope="small">
        // <unitPreference regions="001">centimeter</unitPreference>
        return "//supplementalData/unitPreferenceData/unitPreferences"
                + "[@category=\""
                + quantity
                + "\"]"
                + "[@usage=\""
                + usage
                + "\"]"
                + "/unitPreference"
                + "[@_q=\""
                + order
                + "\"]"
                + "[@regions=\""
                + JOIN_SPACE.join(regions)
                + "\"]"
                + (geq == Rational.ONE ? "" : "[@geq=\"" + geq + "\"]")
                + (skeleton.isEmpty() ? "" : "[@skeleton=\"" + skeleton + "\"]");
    }

    /**
     * Returns the data converted to single regions, and using base units
     *
     * @return
     */
    private Map<String, Map<String, Multimap<String, UnitPreference>>> getRawFastMap() {
        UnitConverter converter = SupplementalDataInfo.getInstance().getUnitConverter();
        Map<String, Map<String, Multimap<String, UnitPreference>>> result = new LinkedHashMap<>();
        for (Entry<String, Map<String, Multimap<Set<String>, UnitPreference>>> entry1 :
                quantityToUsageToRegionsToInfo.entrySet()) {
            String quantity = entry1.getKey();
            Map<String, Multimap<String, UnitPreference>> result2 = new LinkedHashMap<>();
            result.put(quantity, result2);

            for (Entry<String, Multimap<Set<String>, UnitPreference>> entry2 :
                    entry1.getValue().entrySet()) {
                String usage = entry2.getKey();
                Multimap<String, UnitPreference> result3 = LinkedHashMultimap.create();
                result2.put(usage, result3);

                // split the regions
                for (Entry<Set<String>, Collection<UnitPreference>> entry :
                        entry2.getValue().asMap().entrySet()) {
                    Set<String> regions = entry.getKey();
                    int len = entry.getValue().size();
                    for (UnitPreference up : entry.getValue()) {
                        String unit = SPLIT_AND.split(up.unit).iterator().next(); // first unit
                        quantity = converter.getQuantityFromUnit(unit, false);
                        String baseUnit = converter.getBaseUnitFromQuantity(quantity);
                        Rational baseGeq;
                        if (--len == 0) { // set last value to least possible
                            baseGeq = Rational.NEGATIVE_INFINITY;
                        } else {
                            Rational geq = converter.parseRational(String.valueOf(up.geq));
                            baseGeq = converter.convert(geq, unit, baseUnit, false);
                            if (baseGeq.equals(Rational.NaN)) {
                                converter.convert(geq, unit, baseUnit, true); // debug
                            }
                        }
                        UnitPreference up2 = new UnitPreference(baseGeq, up.unit, up.skeleton);
                        for (String region : regions) {
                            result3.put(region, up2);
                        }
                    }
                }
            }
        }
        return CldrUtility.protectCollection(result);
    }

    Supplier<Map<String, Map<String, Multimap<String, UnitPreference>>>>
            quantityToUsageToRegionToInfo = Suppliers.memoize(() -> getRawFastMap());

    public Map<String, Map<String, Multimap<String, UnitPreference>>> getFastMap() {
        return quantityToUsageToRegionToInfo.get();
    }

    public UnitPreference getUnitPreference(
            Rational sourceAmount, String sourceUnit, String usage, ULocale locale) {
        UnitConverter converter = SupplementalDataInfo.getInstance().getUnitConverter();
        sourceUnit = converter.fixDenormalized(sourceUnit);

        String mu = locale.getUnicodeLocaleType("mu");
        // TODO if the value is not a unit, skip
        if (mu != null) {
            Rational conversion = converter.convert(sourceAmount, sourceUnit, mu, false);
            if (!conversion.equals(Rational.NaN)) { // if we could successfully convert
                return new UnitPreference(conversion, mu, null);
            }
        }
        String region = resolveRegion(locale);

        return getUnitPreference(sourceAmount, sourceUnit, usage, region);
    }

    public UnitPreference getUnitPreference(
            Rational sourceAmount, String sourceUnit, String usage, String region) {
        UnitConverter converter = SupplementalDataInfo.getInstance().getUnitConverter();
        String quantity = converter.getQuantityFromUnit(sourceUnit, false);

        Map<String, Multimap<String, UnitPreference>> usageToRegionsToInfo =
                getFastMap().get(quantity);

        // If there is no quantity among the preferences,
        // return the metric UnitPreference
        if (usageToRegionsToInfo == null) {
            String standardUnit = converter.getStandardUnit(sourceUnit);
            if (!sourceUnit.equals(standardUnit)) {
                Rational conversion =
                        converter.convert(sourceAmount, sourceUnit, standardUnit, false);
                return new UnitPreference(conversion, standardUnit, null);
            }
            return new UnitPreference(sourceAmount, sourceUnit, null);
        }

        Multimap<String, UnitPreference> regionToInfo = usageToRegionsToInfo.get(usage);

        if (regionToInfo == null) {
            regionToInfo = usageToRegionsToInfo.get("default");
        }

        // normalize for matching
        sourceAmount = sourceAmount.abs();
        if (sourceAmount.equals(Rational.NaN)) {
            sourceAmount = Rational.NEGATIVE_ONE;
        }

        Collection<UnitPreference> infoList = regionToInfo.get(region);
        if (infoList == null || infoList.isEmpty()) {
            infoList = regionToInfo.get("001");
        }

        Output<String> baseUnitOutput = new Output<>();
        ConversionInfo sourceConversionInfo =
                converter.parseUnitId(sourceUnit, baseUnitOutput, false);
        Rational baseValue = sourceConversionInfo.convert(sourceAmount);

        for (UnitPreference info : infoList) { // data is built to always terminate
            if (baseValue.compareTo(info.geq) >= 0) {
                return info;
            }
        }
        throw new IllegalArgumentException("Fast map should always terminate");
    }

    public String resolveRegion(ULocale locale) {
        // https://unicode.org/reports/tr35/tr35-info.html#Unit_Preferences
        // en-u-rg-uszzzz-ms-ussystem
        String ms = locale.getUnicodeLocaleType("ms");
        if (ms != null) {
            switch (ms) {
                case "metric":
                    return "001";
                case "uksystem":
                    return "GB";
                case "ussystem":
                    return "US";
                default:
                    throw new IllegalArgumentException(
                            "Illegal ms value in: " + locale.toLanguageTag());
            }
        }
        String rg = locale.getUnicodeLocaleType("rg");
        if (rg != null) {
            // TODO: check for illegal rg value
            return rg.substring(0, 2).toUpperCase(Locale.ROOT);
        }
        String region = locale.getCountry();
        if (!region.isEmpty()) {
            return region;
        }
        LikelySubtags LIKELY = new LikelySubtags();
        String maximized = LIKELY.maximize(locale.toLanguageTag());
        if (maximized != null) {
            return ULocale.getCountry(maximized);
        }
        return "001";
    }

    public Set<String> getUsages() {
        return usages;
    }

    public Set<String> getQuantities() {
        return getFastMap().keySet();
    }
}
