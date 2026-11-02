package org.unicode.cldr.tool.unitsinfo;

import static com.google.common.base.Preconditions.checkArgument;

import com.ibm.icu.util.Output;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.unicode.cldr.tool.unitsinfo.UnitsInfo.Complexity;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.ConversionInfo;
import org.unicode.cldr.util.UnitConverter.UnitId;

/** Finds each unit's {@link Complexity} and {@link Conversion} with CLDR's unit converter. */
public final class UnitConversions {

    /**
     * Units that CLDR formats but can't convert: temperature-generic ("25°", with Celsius or
     * Fahrenheit clear from context). Any other unit without a conversion makes the tool fail.
     */
    private static final Set<String> FORMAT_ONLY_UNITS = Set.of("generic");

    private static final Pattern EXPRESSION_TOKENS = Pattern.compile("[*/\\s]+");
    private static final SupplementalDataInfo SDI = SupplementalDataInfo.getInstance();
    private static final UnitConverter CONVERTER = SDI.getUnitConverter();

    /**
     * Units in units.xml convertUnits that don't start with a unit prefix on another convertible
     * unit: meter, gram, pound, and kilogram-force, but not kilogram (kilo + gram).
     */
    private static final Set<String> SIMPLE_UNITS = simpleUnits();

    /** Constants marked approximate in units.xml, and constants computed from them. */
    private static final Set<String> APPROXIMATE_CONSTANTS =
            SDI.getRationalParser().getConstantStatus().entrySet().stream()
                    .filter(e -> "approximate".equals(e.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toUnmodifiableSet());

    private UnitConversions() {}

    /** The conversion of {@code unit} to its base unit. */
    static Conversion of(String unit) {
        if (FORMAT_ONLY_UNITS.contains(unit)) {
            return Conversion.NONE;
        }
        Output<String> baseUnitId = new Output<>();
        ConversionInfo info = CONVERTER.parseUnitId(unit, baseUnitId, false);
        checkArgument(info != null && baseUnitId.value != null, "Can't convert %s", unit);
        String quantity = CONVERTER.getQuantityFromBaseUnit(baseUnitId.value);
        String baseUnit = quantity == null ? null : CONVERTER.getBaseUnitFromQuantity(quantity);
        checkArgument(baseUnit != null, "No quantity for %s (%s)", unit, baseUnitId.value);
        if (info.special != null && !info.special.isEmpty()) {
            return Conversion.special(quantity, baseUnit, info.special);
        }
        return Conversion.linear(quantity, baseUnit, info.factor, info.offset, isApproximate(unit));
    }

    /** The reciprocal of a base unit: cubic-meter-per-meter ↔ meter-per-cubic-meter. */
    static String reciprocalOf(String baseUnit) {
        return CONVERTER.reciprocalOf(baseUnit);
    }

    /**
     * The {@link Complexity} of {@code unit}; {@code convertible} is its replacement when {@code
     * unit} is a deprecated alias that the converter no longer parses directly (like metric-ton).
     */
    public static Complexity complexityOf(String unit, String convertible) {
        if (unit.contains("-and-")) {
            return Complexity.MIXED;
        }
        if (!unit.contains("-")) {
            return isPrefixedUnit(unit, SIMPLE_UNITS) ? Complexity.PREFIXED : Complexity.SIMPLE;
        }
        UnitId id = CONVERTER.createUnitId(convertible);
        if (!id.factor.equals(Rational.ONE)
                || id.numUnitsToPowers.size() != 1
                || !id.denUnitsToPowers.isEmpty()) {
            return Complexity.CORE;
        }
        Map.Entry<String, Integer> single = id.numUnitsToPowers.entrySet().iterator().next();
        if (single.getValue() != 1) {
            return Complexity.SINGLE;
        }
        return SIMPLE_UNITS.contains(single.getKey()) ? Complexity.SIMPLE : Complexity.PREFIXED;
    }

    private static Set<String> simpleUnits() {
        Set<String> convertible = CONVERTER.getSimpleUnits();
        return convertible.stream()
                .filter(unit -> !isPrefixedUnit(unit, convertible))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static boolean isPrefixedUnit(String unit, Set<String> units) {
        return SDI.getUnitPrefixes().stream()
                .anyMatch(
                        prefix ->
                                unit.startsWith(prefix)
                                        && units.contains(unit.substring(prefix.length())));
    }

    /** True if a simple unit in {@code unit} converts with an approximate constant, like PI. */
    private static boolean isApproximate(String unit) {
        UnitId id = CONVERTER.createUnitId(unit);
        return Stream.concat(
                        id.numUnitsToPowers.keySet().stream(),
                        id.denUnitsToPowers.keySet().stream())
                .map(part -> UnitConverter.stripPrefix(part, new Output<>(Rational.ONE)))
                .map(simpleUnit -> CONVERTER.getInternalConversionData().get(simpleUnit))
                .filter(Objects::nonNull)
                .flatMap(target -> target.inputParameters.values().stream())
                .filter(Objects::nonNull)
                .flatMap(EXPRESSION_TOKENS::splitAsStream)
                .anyMatch(APPROXIMATE_CONSTANTS::contains);
    }
}
