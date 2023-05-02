package org.unicode.cldr.unittest;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.util.Output;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.UnitIdComponentType;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.ConversionInfo;
import org.unicode.cldr.util.UnitConverter.TargetInfo;
import org.unicode.cldr.util.UnitConverter.UnitId;
import org.unicode.cldr.util.UnitParser;

public class GenerateNewUnits {
    private static final String CHECK_UNIT = "kilogram-force";
    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    private static final CLDRConfig info = CLDR_CONFIG;
    private static final SupplementalDataInfo SDI = info.getSupplementalDataInfo();
    static final UnitConverter converter = SDI.getUnitConverter();

    public static void main(String[] args) {

        Multimap<String, ExternalUnitConversionData> seen = HashMultimap.create();
        Set<ExternalUnitConversionData> couldAdd = new LinkedHashSet<>();
        int count = 0;
        Output<String> sourceBase = new Output<>();
        Output<String> targetBase = new Output<>();

        List<SimpleConversionData> cleanedNist = clean(NistUnits.externalConversionData);
        UnitParser up = new UnitParser();

        for (SimpleConversionData data : cleanedNist) {
            if (false) {
                System.out.println(data.source + "⟹" + data.info.factor + " * " + data.target);
            }

            up.set(data.source);
            try {
                List<Pair<UnitIdComponentType, String>> list = up.getRemaining();
                System.out.println(data.source + "\t" + list.size() + "\t⟹\t" + list);
            } catch (Exception e1) {
                System.out.println(e1.getMessage() + ", " + data);
                e1.printStackTrace();
            }
            if (true) continue;

            //            ConversionInfo sourceConversionInfo = converter.parseUnitId(data.source,
            // sourceBase, false);
            //
            ////            Rational cldrResult = converter.convert(Rational.ONE, data.source,
            // data.target, false);
            ////
            ////            if (!cldrResult.equals(Rational.NaN)) {
            ////                continue;
            ////            }
            //            if (sourceConversionInfo != null) {
            //                continue;
            //            }
            //
            //            Output<String> baseUnit = new Output<>();
            //            String target = data.target;
            //            Rational endFactor = data.info.factor;
            //            String mark = "";
            //            TargetInfo baseUnit2 = NistUnits.derivedUnitToConversion.get(data.target);
            //            if (baseUnit2 != null) {
            //                target = baseUnit2.target;
            //                endFactor = baseUnit2.unitInfo.factor;
            //                mark="¹";
            //            } else {
            //                ConversionInfo conversionInfo = converter.getUnitInfo(data.target,
            // baseUnit);
            //                if (conversionInfo != null && !data.target.equals(baseUnit.value)) {
            //                    target = baseUnit.value;
            //                    endFactor = conversionInfo.convert(data.info.factor);
            //                    mark="²";
            //                }
            //            }

            String quantity;
            try {
                quantity = converter.getQuantityFromUnit(data.target, false);
            } catch (Exception e) {
                quantity = null;
            }
            if (quantity == null) {
                quantity = quantityFromUnit.get(data.target);
            }

            System.out.println(
                    ++count
                            + "\t"
                            + quantity
                            + "\t"
                            + data.quantity
                            + "\t"
                            + data.source
                            + "\t"
                            + data.info.factor
                            + "\t"
                            + data.target
                            + "\t"
                            + minimize(data.target)
                            + "\n\t\t<convertUnit"
                            + " source='"
                            + data.source
                            + "'"
                            + " baseUnit='"
                            + data.target
                            + "'"
                            + " factor='"
                            + data.info.factor
                            + "'"
                            + " systems=\'metric si\'"
                            + "/>");
        }
    }

    /**
     * Return a simplified unit, eg kilogram-per-meter-square-second ==> pascal TODO handle complex
     * units that don't match a simple quantity, eg kilogram-ampere-per-meter-square-second =>
     * pascal-ampere
     *
     * @param unit
     * @return
     */
    private static String minimize(String unit) {
        UnitId unitId = converter.createUnitId(unit);
        String resolved = unitId.resolve().toString();
        return converter.getStandardUnit(resolved.isBlank() ? unit : resolved);
        //        String quantity;
        //        try {
        //            quantity = converter.getQuantityFromUnit(unit, false);
        //        } catch (Exception e) {
        //            return unit;
        //        }
        //        Set<String> simpleUnits = converter.getSimpleUnits(quantity);
        //        for (String simpleUnit : simpleUnits) {
        //            if (unit.equals(simpleUnit)) {
        //                continue;
        //            }
        //            Rational ratio = converter.convert(Rational.ONE, unit, simpleUnit, false);
        //            if (ratio.equals(Rational.ONE)) {
        //                return simpleUnit;
        //            }
        //        }
        //        return unit;
    }

    static final Comparator<ExternalUnitConversionData> myComparator =
            new Comparator<>() {
                @Override
                public int compare(ExternalUnitConversionData o1, ExternalUnitConversionData o2) {
                    return ComparisonChain.start()
                            .compare(o1.source, o2.source)
                            .compare(o1.target, o2.target)
                            .result();
                }
            };

    private static List<SimpleConversionData> clean(
            List<ExternalUnitConversionData> externalconversiondata) {
        Multimap<String, ExternalUnitConversionData> cleaned =
                TreeMultimap.create(Comparator.naturalOrder(), myComparator);
        Output<String> base = new Output<>();
        for (ExternalUnitConversionData data : NistUnits.externalConversionData) {
            //            if (data.from.equals(NistUnits.NIST_CONVERSIONS)) {
            //                continue; // FOR NOW
            //            }
            if (data.source.equals(CHECK_UNIT)) {
                int debug = 0;
            }
            SDI.getUnitIdComponentType(CHECK_UNIT);
            ConversionInfo convert = converter.parseUnitId(data.source, base, false);
            if (convert == null) {
                cleaned.put(data.source, data);
            }
        }
        List<SimpleConversionData> result = new ArrayList<>();
        ImmutableSet<String> absoluteTemperature =
                ImmutableSet.of("celsius", "fahrenheit", "kelvin", "rankine");
        Output<String> baseUnit = new Output<>();
        for (Entry<String, Collection<ExternalUnitConversionData>> items :
                cleaned.asMap().entrySet()) {
            final String source = items.getKey();
            if (absoluteTemperature.contains(source)) {
                continue;
            }

            final Collection<ExternalUnitConversionData> mappings = items.getValue();
            if (source.equals(CHECK_UNIT)) {
                int debug = 0;
            }
            SimpleConversionData revisedItem = findBestMapping(baseUnit, source, mappings);
            result.add(revisedItem);
        }
        return result;
    }

    public static SimpleConversionData findBestMapping(
            Output<String> baseUnit,
            final String source,
            final Collection<ExternalUnitConversionData> mappings) {
        String bestTarget = null;
        Rational bestFactor = null;
        String bestQuantity = null;
        for (ExternalUnitConversionData data : mappings) {
            ConversionInfo conversionInfo = converter.parseUnitId(data.target, baseUnit, false);
            if (conversionInfo != null) {
                String target = baseUnit.value;
                Rational endFactor = conversionInfo.convert(data.info.factor);
                if (bestTarget == null) {
                    bestTarget = target;
                    bestFactor = endFactor;
                    bestQuantity = data.quantity;
                }
            } else {
                TargetInfo baseUnit2 = NistUnits.derivedUnitToConversion.get(data.target);
                if (baseUnit2 != null) {
                    bestTarget = baseUnit2.target;
                    bestFactor = baseUnit2.unitInfo.factor.multiply(data.info.factor);
                    bestQuantity = data.quantity;
                }
            }
            //            else {
            //                if (!target.equals(bestTarget)
            //                    || !approximatelyEquals(bestFactor, endFactor)
            //                    || !data.quantity.equals(bestQuantity)
            //                    ) {
            //                    throw new IllegalArgumentException();
            //                }
            //            }
        }
        return new SimpleConversionData(
                bestQuantity, source, new ConversionInfo(bestFactor, Rational.ZERO), bestTarget);
    }

    static final Rational EPSILON = Rational.of(1, 1000000);

    private static boolean approximatelyEquals(Rational a, Rational b) {
        if (a.equals(b)) {
            return true;
        }
        // approximately equal when abs(a-b) / (a + b) < epsilon
        Rational a_b = a.subtract(b).abs().divide(a.add(b));
        return a_b.compareTo(EPSILON) < 0;
    }

    static class SimpleConversionData {
        public SimpleConversionData(
                String quantity, String source, ConversionInfo conversionInfo, String target) {
            this.quantity = quantity;
            this.source = source;
            this.target = target;
            this.info = conversionInfo;
        }

        final String quantity;
        final String source;
        final ConversionInfo info;
        final String target;

        @Override
        public String toString() {
            return quantity + ", " + source + ", " + info + ", " + target;
        }
    }

    //    private static ExternalUnitConversionData best(String source,
    // Collection<ExternalUnitConversionData> collection) {
    //        if (collection.size() != 1) {
    //            for (ExternalUnitConversionData item : collection) {
    //
    //                System.out.println(source + "\t" + item.info.factor + "\t" + item.target);
    //            }
    //            System.out.println();
    //        }
    //        return collection.iterator().next();
    //    }

    static final ImmutableMap<String, String> quantityFromUnit =
            ImmutableMap.<String, String>builder()
                    .put("curie", "radioactivity")
                    .put("kayser", "spectroscopy")
                    .put("gon", "angle")
                    .put(
                            "cubic-second-square-ampere-per-kilogram-square-meter",
                            "electrical-conductance")
                    .put("gram-per-meter", "dup")
                    .put("joule-per-kelvin", "entropy")
                    .put("joule-per-kilogram-kelvin", "specific-heat")
                    .put("joule-per-square-meter", "radiant-exposure")
                    .put("kelvin-per-watt", "absolute-thermal-resistance")
                    .put("kilogram-per-meter", "linear-density")
                    .put("kilogram-per-second", "mass-flow-rate")
                    .put("kilogram-per-square-meter", "surface-density")
                    .put("kilogram-per-square-second-ampere", "magnetic-flux-density")
                    .put("kilogram-square-meter-per-square-second-ampere", "magnetic-flux")
                    .put("kilogram-square-meter-per-square-second-square-ampere", "inductance")
                    .put("meter-kelvin-per-watt", "thermal-resistance-coefficient")
                    .put("meter-per-meter", "angle")
                    .put("mole-per-second", "enzymatic-activity")
                    .put("ohm-meter", "electric-resistivity")
                    .put("ohm-square-millimeter-per-meter", "electrical-resistivity")
                    .put("pascal-second", "dynamic-viscosity")
                    .put("per-pascal-second", "dup")
                    .put("per-second", "radioactivity")
                    .put("pow4-second-square-ampere-per-kilogram-square-meter", "capacitance")
                    .put("second-ampere", "emu-of-charge")
                    .put("square-meter-per-second", "kinematic-viscosity")
                    .put("square-meter-per-square-meter", "solid-angle")
                    .put("square-meter-per-square-second", "dose")
                    .put("watt-per-square-meter", "irradiance")
                    .put("watt-per-square-meter-kelvin", "thermal-heat-transfer-coefficient")
                    .put("watt-per-meter-kelvin", "thermal-conductivity")
                    .put("square-meter-kelvin-per-watt", "thermal-insulance")
                    .put(
                            "kilogram-square-meter-per-meter-cubic-second-kelvin",
                            "thermal-conductivity")
                    .put("kilogram-second-per-meter-square-second", "viscosity")
                    .put("kilogram-square-meter-per-square-meter-cubic-second", "surface-tension")
                    // kilogram-square-meter-per-cubic-second
                    .build();
}
