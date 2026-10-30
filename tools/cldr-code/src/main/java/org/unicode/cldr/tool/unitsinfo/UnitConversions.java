package org.unicode.cldr.tool.unitsinfo;

import static com.google.common.base.Preconditions.checkArgument;

import com.ibm.icu.util.Output;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.UnitConverter;
import org.unicode.cldr.util.UnitConverter.ConversionInfo;
import org.unicode.cldr.util.UnitConverter.UnitId;

/** Finds each unit's {@link Conversion} with CLDR's unit converter. */
final class UnitConversions {

    /**
     * Units that CLDR formats but can't convert: temperature-generic ("25°", with Celsius or
     * Fahrenheit clear from context). Any other unit without a conversion makes the tool fail.
     */
    private static final Set<String> FORMAT_ONLY_UNITS = Set.of("generic");

    private static final Pattern EXPRESSION_TOKENS = Pattern.compile("[*/\\s]+");
    private static final SupplementalDataInfo SDI = SupplementalDataInfo.getInstance();
    private static final UnitConverter CONVERTER = SDI.getUnitConverter();

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
