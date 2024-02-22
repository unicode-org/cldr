package org.unicode.cldr.unittest;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.util.Output;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.Rational.FormatStyle;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.UnitIdComponentType;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.ConversionInfo;
import org.unicode.cldr.util.UnitConverter.TargetInfo;
import org.unicode.cldr.util.UnitConverter.UnitId;
import org.unicode.cldr.util.UnitConverter.UnitSystem;
import org.unicode.cldr.util.UnitParser;

public class GenerateNewUnits {

    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    private static final CLDRConfig info = CLDR_CONFIG;
    private static final SupplementalDataInfo SDI = info.getSupplementalDataInfo();
    private static final UnitConverter converter = SDI.getUnitConverter();

    private static final String CHECK_UNIT = "kilogram-force";

    public static void main(String[] args) {
        Set<String> argSet = Set.copyOf(Arrays.asList(args));
        boolean SI_ACCEPTED = argSet.isEmpty() || argSet.contains("accepted");
        boolean SYMBOLS = argSet.isEmpty() || argSet.contains("symbols");
        boolean GENERATE_XML = argSet.isEmpty() || argSet.contains("xml");
        boolean OTHER = argSet.isEmpty() || argSet.contains("plain");

        boolean TEST_PARSER = argSet.contains("parser");
        Output<String> base = new Output<>();

        int count = 0;
        Set<ExternalUnitConversionData> cleanedNist = clean(NistUnits.externalConversionData);
        UnitParser up = new UnitParser();
        String lastQuantity = "";

        if (SI_ACCEPTED) {
            System.out.println("\n# SI_Accepted\n");
            for (ExternalUnitConversionData data : NistUnits.externalConversionData) {
                if (data.systems.contains(UnitSystem.si_acceptable)) {
                    ConversionInfo convert = converter.parseUnitId(data.source, base, false);
                    System.out.println((convert == null ? "OUT" : "IN") + "\t" + data);
                }
            }
        }
        if (SYMBOLS) {
            System.out.println("# Symbols\n");

            Multimap<String, String> unitsToSymbols = TreeMultimap.create();
            Multimap<String, String> symbolsToUnits = TreeMultimap.create();
            Matcher simple = Pattern.compile("^[^ ·/0-9]*$").matcher("");
            for (ExternalUnitConversionData data : NistUnits.externalConversionData) {
                if (data.symbol != null) {
                    ConversionInfo convert = converter.parseUnitId(data.source, base, false);
                    if (convert != null && simple.reset(data.symbol).matches()) {
                        unitsToSymbols.put(data.source, data.symbol);
                        symbolsToUnits.put(data.symbol, data.source);
                    }
                }
            }
            for (Entry<String, Collection<String>> entry : unitsToSymbols.asMap().entrySet()) {
                final String sourceUnit = entry.getKey();
                System.out.println(
                        converter.getQuantityFromUnit(sourceUnit, false) //
                                + "\t"
                                + sourceUnit //
                                + "\t"
                                + Joiner.on('\t').join(entry.getValue())
                                + "\t"
                                + converter.getSystems(sourceUnit));
            }
            for (Entry<String, Collection<String>> entry : symbolsToUnits.asMap().entrySet()) {
                if (entry.getValue().size() > 1) {
                    System.out.println(
                            "Ambiguous! "
                                    + entry.getKey()
                                    + "\t"
                                    + Joiner.on('\t').join(entry.getValue()));
                }
            }
        }
        if (GENERATE_XML) {
            System.out.println("\n# XML missing\n");
            for (ExternalUnitConversionData data : cleanedNist) {

                if (!lastQuantity.equals(data.quantity)) {
                    System.out.println("<!-- " + data.quantity + "-->");
                    lastQuantity = data.quantity;
                }
                System.out.println(
                        "\t\t<convertUnit"
                                + " source='"
                                + data.source
                                + "'"
                                + (data.symbol == null ? "" : " symbol='" + data.source + "'")
                                + " baseUnit='"
                                + data.target
                                + "'"
                                + " factor='"
                                + data.info.factor
                                + "'"
                                + " systems=\'???\'"
                                + "/>");
            }
            if (OTHER) {
                System.out.println("\n# Missing\n");
                for (ExternalUnitConversionData data : cleanedNist) {
                    System.out.println(
                            ++count
                                    + "\t"
                                    + data.quantity
                                    + "\t"
                                    + data.source
                                    + "\t"
                                    + data.symbol
                                    + "\t⟹\t"
                                    + data.info.factor
                                    + " × "
                                    + data.target
                                    + "\t"
                                    + data.info.factor.toString(FormatStyle.approx));
                }
            }
            if (TEST_PARSER) {
                System.out.println("\n# Check parser\n");
                for (ExternalUnitConversionData data : cleanedNist) {
                    up.set(data.source);
                    try {
                        List<Pair<UnitIdComponentType, String>> list = up.getRemaining();
                        System.out.println(data.source + "\t" + list.size() + "\t⟹\t" + list);
                    } catch (Exception e1) {
                        System.out.println(e1.getMessage() + ", " + data);
                        e1.printStackTrace();
                    }
                }
            }
        }
    }

    /** Filter out the most useful conversions */
    private static Set<ExternalUnitConversionData> clean(
            Set<ExternalUnitConversionData> externalconversiondata) {
        Multimap<String, ExternalUnitConversionData> cleaned = TreeMultimap.create();
        Output<String> base = new Output<>();
        for (ExternalUnitConversionData data : NistUnits.externalConversionData) {
            if (data.source.equals(CHECK_UNIT)) {
                SDI.getUnitIdComponentType(CHECK_UNIT);
            }

            // skip the ones we have already

            ConversionInfo convert = converter.parseUnitId(data.source, base, false);
            if (convert == null) {
                cleaned.put(data.source, data);
            }
        }
        Set<ExternalUnitConversionData> result = new TreeSet<>();
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
            ExternalUnitConversionData revisedItem = findBestMapping(baseUnit, source, mappings);
            result.add(revisedItem);
        }
        return result;
    }

    /**
     * Get the best mapping
     *
     * @param baseUnit
     * @param source
     * @param mappings
     * @return
     */
    public static ExternalUnitConversionData findBestMapping(
            Output<String> baseUnit,
            final String source,
            final Collection<ExternalUnitConversionData> mappings) {
        String bestTarget = null;
        Rational bestFactor = null;
        String bestQuantity = null;
        String bestSymbol = null;
        for (ExternalUnitConversionData data : mappings) {
            ConversionInfo conversionInfo = converter.parseUnitId(data.target, baseUnit, false);
            if (conversionInfo != null) {
                String target = baseUnit.value;
                Rational endFactor = conversionInfo.convert(data.info.factor);
                if (bestTarget == null) {
                    bestTarget = target;
                    bestFactor = endFactor;
                    bestQuantity = data.quantity;
                    bestSymbol = data.symbol;
                }
            } else {
                TargetInfo baseUnit2 = NistUnits.derivedUnitToConversion.get(data.target);
                if (baseUnit2 != null) {
                    bestTarget = baseUnit2.target;
                    bestFactor = baseUnit2.unitInfo.factor.multiply(data.info.factor);
                    bestQuantity = data.quantity;
                    bestSymbol = data.symbol;
                }
            }
        }
        UnitId targetId = converter.createUnitId(bestTarget);
        bestTarget = targetId == null ? bestTarget : targetId.resolve().toString();
        String quantity = converter.getQuantityFromUnit(bestTarget, false);
        bestQuantity = quantity == null ? "•" + bestQuantity : quantity;
        return new ExternalUnitConversionData(
                bestQuantity,
                source,
                bestSymbol,
                bestTarget,
                bestFactor,
                Rational.ZERO,
                null,
                null,
                null);
    }

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
