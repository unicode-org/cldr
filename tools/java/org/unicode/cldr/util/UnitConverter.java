package org.unicode.cldr.util;

import java.math.BigInteger;
import java.math.MathContext;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.util.Rational.RationalParser;

import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.util.Freezable;
import com.ibm.icu.util.Output;

public class UnitConverter implements Freezable<UnitConverter> {

    static final Splitter BAR_SPLITTER = Splitter.on('-');
    static final Splitter SPACE_SPLITTER = Splitter.on(' ').trimResults().omitEmptyStrings();

    final RationalParser rationalParser;

    private Map<String,String> baseUnitToQuantity = new LinkedHashMap<>();
    private Map<String,String> baseUnitToStatus = new LinkedHashMap<>();
    private Map<String, TargetInfo> sourceToTargetInfo = new LinkedHashMap<>();
    private Multimap<String, String> quantityToSimpleUnits = LinkedHashMultimap.create();
    private Multimap<String, String> sourceToSystems = LinkedHashMultimap.create();
    private Set<String> baseUnits;
    private Multimap<String, Continuation> continuations = TreeMultimap.create();
    private MapComparator<String> quantityComparator; 
    private Map<String,String> fixDenormalized;

    private boolean frozen = false;

    public TargetInfoComparator targetInfoComparator;

    /** Warning: ordering is important; determines the normalized output */
    public static final Set<String> BASE_UNITS = ImmutableSet.of(
        "candela",
        "kilogram", 
        "meter", 
        "second",
        "ampere", 
        "kelvin",
        // non-SI
        "year", 
        "bit", 
        "item", 
        "pixel", 
        "em", 
        "revolution",
        "portion"
        );

    public void addQuantityInfo(String baseUnit, String quantity, String status) {
        if (baseUnitToQuantity.containsKey(baseUnit)) {
            throw new IllegalArgumentException();
        }
        baseUnitToQuantity.put(baseUnit, quantity);
        baseUnitToStatus.put(baseUnit, quantity);
        quantityToSimpleUnits.put(quantity, baseUnit);
    }

    @Override
    public boolean isFrozen() {
        return frozen;
    }

    @Override
    public UnitConverter freeze() {
        frozen = true;
        rationalParser.freeze();
        sourceToTargetInfo = ImmutableMap.copyOf(sourceToTargetInfo);
        quantityToSimpleUnits = ImmutableMultimap.copyOf(quantityToSimpleUnits);
        sourceToSystems = ImmutableMultimap.copyOf(sourceToSystems);    
        // other fields are frozen earlier in processing
        Builder<String> builder = ImmutableSet.<String>builder()
            .addAll(BASE_UNITS);
        for (TargetInfo s : sourceToTargetInfo.values()) {
            builder.add(s.target);
        }
        baseUnits = builder.build();
        for (String denormalized : fixDenormalized.keySet()) {
            Continuation.addIfNeeded(denormalized, continuations);
        }
        continuations = ImmutableMultimap.copyOf(continuations);
        targetInfoComparator = new TargetInfoComparator();
        return this;
    }

    @Override
    public UnitConverter cloneAsThawed() {
        throw new UnsupportedOperationException();
    }


    public static final class ConversionInfo implements Comparable<ConversionInfo> {
        public final Rational factor;
        public final Rational offset;
        public final boolean reciprocal;

        static final ConversionInfo IDENTITY = new ConversionInfo(Rational.ONE, Rational.ZERO, false);

        public ConversionInfo(Rational factor, Rational offset, boolean reciprocal) {
            this.factor = factor;
            this.offset = offset;
            this.reciprocal = reciprocal;
        }

        public Rational convert(Rational source) {
            if (reciprocal) {
                source = source.reciprocal();
            }
            return source.multiply(factor).add(offset);
        }

        public Rational convertBackwards(Rational source) {
            Rational result = source.subtract(offset).divide(factor);
            return (reciprocal ? result.reciprocal() : result);
        }

        public ConversionInfo invert() {
            Rational factor2 = factor.reciprocal();
            Rational offset2 = offset.equals(Rational.ZERO) ? Rational.ZERO : offset.divide(factor).negate();
            return new ConversionInfo(factor2, offset2, reciprocal);
            // TODO fix reciprocal
        }

        @Override
        public String toString() {
            return toString("x");
        }
        public String toString(String unit) {
            return factor 
                + (reciprocal ? " / " : " * ") + unit
                + (offset.equals(Rational.ZERO) ? "" : 
                    (offset.compareTo(Rational.ZERO) < 0 ? " - " : " - ")
                    + offset.abs());
        }

        public String toDecimal() {
            return toDecimal("x");
        }
        public String toDecimal(String unit) {
            return factor.toBigDecimal(MathContext.DECIMAL64) 
                + (reciprocal ? " / " : " * ") + unit
                + (offset.equals(Rational.ZERO) ? "" : 
                    (offset.compareTo(Rational.ZERO) < 0 ? " - " : " - ")
                    + offset.toBigDecimal(MathContext.DECIMAL64).abs());
        }

        @Override
        public int compareTo(ConversionInfo o) {
            int diff;
            if (0 != (diff = factor.compareTo(o.factor))) {
                return diff;
            }
            if (0 != (diff = offset.compareTo(o.offset))) {
                return diff;
            }
            return Boolean.compare(reciprocal, o.reciprocal);
        }
        @Override
        public boolean equals(Object obj) {
            return 0 == compareTo((ConversionInfo)obj);
        }
        @Override
        public int hashCode() {
            return Objects.hash(factor, offset, reciprocal);
        }
    }

    public static class Continuation implements Comparable<Continuation> {
        public final List<String> remainder;
        public final String result;

        public static void addIfNeeded(String source, Multimap<String, Continuation> data) {
            List<String> sourceParts = BAR_SPLITTER.splitToList(source);
            if (sourceParts.size() > 1) {
                Continuation continuation = new Continuation(ImmutableList.copyOf(sourceParts.subList(1, sourceParts.size())), source);
                data.put(sourceParts.get(0), continuation);
            }
        }
        public Continuation(List<String> remainder, String source) {
            this.remainder = remainder;
            this.result = source;
        }
        /**
         * The ordering is designed to have longest continuation first so that matching works.
         * Otherwise the ordering doesn't matter, so we just use the result.
         */
        @Override
        public int compareTo(Continuation other) {
            int diff = other.remainder.size() - remainder.size();
            if (diff != 0) {
                return diff;
            }
            return result.compareTo(other.result);
        }

        public boolean match(List<String> parts, final int startIndex) {
            if (remainder.size() > parts.size() - startIndex) {
                return false;
            }
            int i = startIndex;
            for (String unitPart : remainder) {
                if (!unitPart.equals(parts.get(i++))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return remainder + " 🢣 " + result;
        }

        public static Iterable<String> split(String derivedUnit, Multimap<String, Continuation> continuations) {
            return new UnitIterator(derivedUnit, continuations);
        }

        public static class UnitIterator implements Iterable<String>, Iterator<String> {
            final List<String> parts;
            final Multimap<String, Continuation> continuations;
            int nextIndex = 0;

            public UnitIterator(String derivedUnit, Multimap<String, Continuation> continuations) {
                parts = BAR_SPLITTER.splitToList(derivedUnit);
                this.continuations = continuations;
            }

            @Override
            public boolean hasNext() {
                return nextIndex < parts.size();
            }

            @Override
            public String next() {
                String result = parts.get(nextIndex++);
                Collection<Continuation> continuationOptions = continuations.get(result);
                for (Continuation option : continuationOptions) {
                    if (option.match(parts, nextIndex)) {
                        nextIndex += option.remainder.size();
                        return option.result;
                    }
                }
                return result;
            }

            @Override
            public Iterator<String> iterator() {
                return this;
            }

        }
    }

    public UnitConverter(RationalParser rationalParser) {
        this.rationalParser = rationalParser;
    }

    public void addRaw(String source, String target, String factor, String offset, String reciprocal, String systems) {
        ConversionInfo info = new ConversionInfo(
            factor == null ? Rational.ONE : rationalParser.parse(factor), 
                offset == null ? Rational.ZERO : rationalParser.parse(offset), 
                    reciprocal == null ? false : reciprocal.equalsIgnoreCase("true") ? true : false);
        Map<String, String> args = new LinkedHashMap<>();
        if (factor != null) {
            args.put("factor", factor);
        }
        if (offset != null) {
            args.put("offset", offset);
        }
        if (reciprocal != null) {
            args.put("reciprocal", reciprocal);
        }

        addToSourceToTarget(source, target, info, args, systems);
        Continuation.addIfNeeded(source, continuations);
    }

    public static class TargetInfo{
        public final String target;
        public final ConversionInfo unitInfo;
        public final Map<String, String> inputParameters;
        public TargetInfo(String target, ConversionInfo unitInfo, Map<String, String> inputParameters) {
            this.target = target;
            this.unitInfo = unitInfo;
            this.inputParameters = ImmutableMap.copyOf(inputParameters);
        }
        @Override
        public String toString() {
            return unitInfo + " (" + target + ")";
        }
        public String formatOriginalSource(String source) {
            StringBuilder result = new StringBuilder()
                .append("<convertUnit source='")
                .append(source)
                .append("' baseUnit='")
                .append(target)
                .append("'")
                ;
            for (Entry<String, String> entry : inputParameters.entrySet()) {
                if (entry.getValue() != null) {
                    result.append(" " + entry.getKey() + "='" + entry.getValue() + "'");
                }
            }
            result.append("/>");
//            if (unitInfo.equals(UnitInfo.IDENTITY)) {
//                result.append("\t<!-- IDENTICAL -->");
//            } else {
//                result.append("\t<!-- ~")
//                .append(unitInfo.toDecimal(target))
//                .append(" -->");
//            }
            return result.toString();
        }
    }
    public class TargetInfoComparator implements Comparator<TargetInfo> {
        @Override
        public int compare(TargetInfo o1, TargetInfo o2) {
            String quality1 = baseUnitToQuantity.get(o1.target);
            String quality2 = baseUnitToQuantity.get(o2.target);
            int diff;
            if (0 != (diff = quantityComparator.compare(quality1, quality2))) {
                return diff;
            }
            if (0 != (diff = o1.unitInfo.compareTo(o2.unitInfo))) {
                return diff;
            }
            return o1.target.compareTo(o2.target);
        }
    }

    private void addToSourceToTarget(String source, String target, ConversionInfo info, 
        Map<String, String> inputParameters, String systems) {
        if (sourceToTargetInfo.isEmpty()) {
            baseUnitToQuantity = ImmutableBiMap.copyOf(baseUnitToQuantity);
            baseUnitToStatus = ImmutableMap.copyOf(baseUnitToStatus);
            quantityComparator = new MapComparator<>(baseUnitToQuantity.values()).freeze();
        } else if (sourceToTargetInfo.containsKey(source)) {
            throw new IllegalArgumentException("Duplicate source: " + source + ", " + target);
        }
        sourceToTargetInfo.put(source, new TargetInfo(target, info, inputParameters));
        String targetQuantity = baseUnitToQuantity.get(target);
        if (targetQuantity == null) {
            throw new IllegalArgumentException();
        }
        quantityToSimpleUnits.put(targetQuantity, source);
        if (systems != null) {
            sourceToSystems.putAll(source, SPACE_SPLITTER.split(systems));
        }
    }

    public Set<String> canConvertBetween(String unit) {
        TargetInfo targetInfo = sourceToTargetInfo.get(unit);
        if (targetInfo == null) {
            return Collections.emptySet();
        }
        String quantity = baseUnitToQuantity.get(targetInfo.target);
        return ImmutableSet.copyOf(quantityToSimpleUnits.get(quantity));
    }

    public Set<String> canConvert() {
        return sourceToTargetInfo.keySet();
    }

    public Rational convert(Rational source, String sourceUnit, String targetUnit) {
        if (sourceUnit.equals(targetUnit)) {
            return source;
        }
        TargetInfo toPivotInfo = sourceToTargetInfo.get(sourceUnit);
        if (toPivotInfo == null) {
            return Rational.NaN;
        }
        TargetInfo fromPivotInfo = sourceToTargetInfo.get(targetUnit);
        if (fromPivotInfo == null) {
            return Rational.NaN;
        }
        if (!toPivotInfo.target.equals(fromPivotInfo.target)) {
            return Rational.NaN;
        }
        Rational toPivot = toPivotInfo.unitInfo.convert(source);
        Rational fromPivot = fromPivotInfo.unitInfo.convertBackwards(toPivot);
        return fromPivot;
    }

    // TODO fix to guarantee single mapping

    public ConversionInfo getUnitInfo(String sourceUnit, Output<String> baseUnit) {
        if (isBaseUnit(sourceUnit)) {
            baseUnit.value = sourceUnit;
            return ConversionInfo.IDENTITY;
        }
        TargetInfo targetToInfo = sourceToTargetInfo.get(sourceUnit);
        if (targetToInfo == null) {
            return null;
        }
        baseUnit.value = targetToInfo.target;
        return targetToInfo.unitInfo;
    }

    public String getBaseUnit(String simpleUnit) {
        TargetInfo targetToInfo = sourceToTargetInfo.get(simpleUnit);
        if (targetToInfo == null) {
            return null;
        }
        return targetToInfo.target;
    }


    // final Map<String, String> FIX_DENORMALIZED = fixDenormalized;
//        ImmutableMap.of(
//        "meter-per-second-squared", "meter-per-square-second",
//        "liter-per-100kilometers", "liter-per-100-kilometer",
//        "pound-foot", "pound-force-foot",
//        "pound-per-square-inch", "pound-force-per-square-inch");

    /**
     * Takes a derived unit id, and produces the equivalent derived base unit id and UnitInfo to convert to it
     * @author markdavis
     * @param showYourWork TODO
     *
     */
    public ConversionInfo parseUnitId (String derivedUnit, Output<String> metricUnit, boolean showYourWork) {
        metricUnit.value = null;

        if (derivedUnit.equals("liter-per-100kilometers")) {
            int debug = 0;
        }
        UnitId outputUnit = new UnitId();
        Rational numerator = Rational.ONE;
        Rational denominator = Rational.ONE;
        boolean inNumerator = true;
        int power = 1;

        Output<Rational> deprefix = new Output<>();

        String fixed = fixDenormalized.get(derivedUnit); // TODO replace derivation of FIX_...
        if (fixed != null) {
            derivedUnit = fixed;
            if (showYourWork) System.out.println("\t⟹ de-alias: " + derivedUnit);
        }
        Rational offset = Rational.ZERO;
        int countUnits = 0;
        for (String unit : Continuation.split(derivedUnit, continuations)) {
            ++countUnits;
            if (unit.equals("square")) {
                if (power != 1) {
                    throw new IllegalArgumentException("Can't have power of " + unit);
                }
                power = 2;
                if (showYourWork) System.out.println(showRational("\t⟹ power: ", Rational.of(power), unit));
            } else if (unit.equals("cubic")) {
                if (power != 1) {
                    throw new IllegalArgumentException("Can't have power of " + unit);
                }
                power = 3;
                if (showYourWork) System.out.println(showRational("\t⟹ power: ", Rational.of(power), unit));
            } else if (unit.startsWith("pow")) {
                if (power != 1) {
                    throw new IllegalArgumentException("Can't have power of " + unit);
                }
                power = Integer.parseInt(unit.substring(3));
                if (showYourWork) System.out.println(showRational("\t⟹ power: ", Rational.of(power), unit));
            } else if (unit.equals("per")) {
                if (power != 1) {
                    throw new IllegalArgumentException("Can't have power of per");
                }
                if (showYourWork && inNumerator == false) System.out.println("\t⟹ per: ");
                inNumerator = false; // ignore multiples
//            } else if ('9' >= unit.charAt(0)) {
//                if (power != 1) {
//                    throw new IllegalArgumentException("Can't have power of " + unit);
//                }
//                Rational factor = Rational.of(Integer.parseInt(unit));
//                if (inNumerator) {
//                    numerator = numerator.multiply(factor);
//                } else {
//                    denominator = denominator.multiply(factor);
//                }
            } else {
                // kilo etc.
                unit = stripPrefix(unit, deprefix);
                if (showYourWork && deprefix.value != null) System.out.println(showRational("\t⟹ source: ", deprefix.value, unit));

                Rational value = deprefix.value;
                if (!isSimpleBaseUnit(unit)) {
                    TargetInfo info = sourceToTargetInfo.get(unit);
                    if (info == null) {
                        if (showYourWork) System.out.println("\t⟹ no conversion for: " + unit);
                        return null; // can't convert
                    }
                    String baseUnit = info.target;
                    value = info.unitInfo.factor.multiply(value);
                    if (showYourWork && !info.unitInfo.factor.equals(Rational.ONE)) System.out.println(showRational("\t⟹ factor: ", info.unitInfo.factor, baseUnit));
                    offset = offset.add(info.unitInfo.offset);
                    if (showYourWork && !info.unitInfo.offset.equals(Rational.ZERO)) System.out.println(showRational("\t⟹ offset: ", info.unitInfo.offset, baseUnit));
                    unit = baseUnit;
                }
                for (int p = 1; p <= power; ++p) {
                    if (inNumerator) {
                        numerator = numerator.multiply(value);
                    } else {
                        denominator = denominator.multiply(value);
                    }
                }
                // create cleaned up target unitid
                outputUnit.add(continuations, unit, inNumerator, power);
                power = 1;
            }
        }
        metricUnit.value = outputUnit.toString();
        return new ConversionInfo(numerator.divide(denominator), countUnits == 1 ? offset : Rational.ZERO, false);
    }


    /** Only for use for simple base unit comparison */
    private class UnitComparator implements Comparator<String>{
        // TODO, use order in units.xml

        @Override
        public int compare(String o1, String o2) {
            if (o1.equals(o2)) {
                return 0;
            }
            Output<Rational> deprefix1 = new Output<>();
            o1 = stripPrefix(o1, deprefix1);
            TargetInfo targetAndInfo1 = sourceToTargetInfo.get(o1);
            String quantity1 = baseUnitToQuantity.get(targetAndInfo1.target);

            Output<Rational> deprefix2 = new Output<>();
            o2 = stripPrefix(o2, deprefix2);
            TargetInfo targetAndInfo2 = sourceToTargetInfo.get(o2);
            String quantity2 = baseUnitToQuantity.get(targetAndInfo2.target);

            int diff;
            if (0 != (diff = quantityComparator.compare(quantity1, quantity2))) {
                return diff;
            }
            Rational factor1 = targetAndInfo1.unitInfo.factor.multiply(deprefix1.value);
            Rational factor2 = targetAndInfo2.unitInfo.factor.multiply(deprefix2.value);
            if (0 != (diff = factor1.compareTo(factor2))) {
                return diff;
            }
            return o1.compareTo(o2);
        }
    };

    Comparator<String> UNIT_COMPARATOR = new UnitComparator();

    public static final Set<String> BASE_UNIT_PARTS = ImmutableSet.<String>builder()
        .add("per").add("square").add("cubic").addAll(BASE_UNITS)
        .build();

    /** 
     * Only handles the canonical units; no kilo-, only normalized, etc.
     * @author markdavis
     *
     */
    public class UnitId implements Freezable<UnitId> {
        private Map<String, Integer> numUnitsToPowers = new TreeMap<>(UNIT_COMPARATOR);
        private Map<String, Integer> denUnitsToPowers = new TreeMap<>(UNIT_COMPARATOR);
        private boolean frozen = false;

        private UnitId() {} // 

        private UnitId add(Multimap<String, Continuation> continuations, String compoundUnit, boolean groupInNumerator, int groupPower) {
            if (frozen) {
                throw new UnsupportedOperationException("Object is frozen.");
            }
            boolean inNumerator = true;
            int power = 1;
            // maybe refactor common parts with above code.
            for (String unitPart : Continuation.split(compoundUnit, continuations)) {
                switch (unitPart) {
                case "square": power = 2; break;
                case "cubic": power = 3; break;
                case "per": inNumerator = false; break; // sticky, ignore multiples
                default: 
                    if (unitPart.startsWith("pow")) {
                        power = Integer.parseInt(unitPart.substring(3));
                    } else {
                        Map<String, Integer> target = inNumerator == groupInNumerator ? numUnitsToPowers : denUnitsToPowers;
                        Integer oldPower = target.get(unitPart);
                        // we multiply powers, so that weight-square-volume => weight-pow4-length
                        int newPower = groupPower * power + (oldPower == null ? 0 : oldPower);
                        target.put(unitPart, newPower);
                        power = 1;
                    }
                }
            }
            return this;
        }
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            boolean firstDenominator = true;
            for (int i = 1; i >= 0; --i) { // two passes, numerator then den.
                boolean positivePass = i > 0;
                Map<String, Integer> target = positivePass ? numUnitsToPowers : denUnitsToPowers;
                for (Entry<String, Integer> entry : target.entrySet()) {
                    String unit = entry.getKey();
                    int power = entry.getValue();
                    // NOTE: zero (eg one-per-one) gets counted twice
                    if (builder.length() != 0) {
                        builder.append('-');
                    }
                    if (!positivePass) {
                        if (firstDenominator) {
                            firstDenominator = false;
                            builder.append("per-");
                        }
                    }
                    switch (power) {
                    case 1: 
                        break;
                    case 2: 
                        builder.append("square-"); break;
                    case 3: 
                        builder.append("cubic-"); break;
                    default: 
                        if (power > 3) {
                            builder.append("pow" + power + "-");
                        } else {
                            throw new IllegalArgumentException("Unhandled power: " + power);
                        }
                        break;
                    }
                    builder.append(unit);

                }
            }
            return builder.toString();
        }
        @Override
        public boolean equals(Object obj) {
            UnitId other = (UnitId) obj;
            return numUnitsToPowers.equals(other.numUnitsToPowers) 
                && denUnitsToPowers.equals(other.denUnitsToPowers);
        }
        @Override
        public int hashCode() {
            return Objects.hash(numUnitsToPowers, denUnitsToPowers);
        }
        @Override
        public boolean isFrozen() {
            return frozen;
        }
        @Override
        public UnitId freeze() {
            frozen = true;
            numUnitsToPowers = ImmutableMap.copyOf(numUnitsToPowers);
            denUnitsToPowers = ImmutableMap.copyOf(denUnitsToPowers);
            return this;
        }
        @Override
        public UnitId cloneAsThawed() {
            throw new UnsupportedOperationException();
        }

        public UnitId resolve() {
            UnitId result = new UnitId();
            result.numUnitsToPowers.putAll(numUnitsToPowers);
            result.denUnitsToPowers.putAll(denUnitsToPowers);
            for (Entry<String, Integer> entry : numUnitsToPowers.entrySet()) {
                final String key = entry.getKey();
                Integer denPower = denUnitsToPowers.get(key);
                if (denPower == null) {
                    continue;
                }
                int power = entry.getValue() - denPower;
                if (power > 0) {
                    result.numUnitsToPowers.put(key, power);
                    result.denUnitsToPowers.remove(key);
                } else if (power < 0) {
                    result.numUnitsToPowers.remove(key);
                    result.denUnitsToPowers.put(key, -power);
                } else { // 0, so
                    result.numUnitsToPowers.remove(key);
                    result.denUnitsToPowers.remove(key);
                }
            }
            return result.freeze();
        }
    }

    public final UnitId createUnitId(String unit) {
        return new UnitId().add(continuations, unit, true, 1).freeze();
    }

    public boolean isBaseUnit(String unit) {
        return baseUnits.contains(unit);
    }

    public boolean isSimpleBaseUnit(String unit) {
        return BASE_UNITS.contains(unit);
    }

    public Set<String> baseUnits() {
        return baseUnits;
    }

    // TODO change to TRIE if the performance isn't good enough, or restructure with regex
    static final ImmutableMap<String, Rational> PREFIXES = ImmutableMap.<String, Rational>builder()
        .put("yocto", Rational.pow10(-24))
        .put("zepto", Rational.pow10(-21))
        .put("atto", Rational.pow10(-18))
        .put("femto", Rational.pow10(-15))
        .put("pico", Rational.pow10(-12))
        .put("nano", Rational.pow10(-9))
        .put("micro", Rational.pow10(-6))
        .put("milli", Rational.pow10(-3))
        .put("centi", Rational.pow10(-2))
        .put("deci", Rational.pow10(-1))
        .put("deka", Rational.pow10(1))
        .put("hecto", Rational.pow10(2))
        .put("kilo", Rational.pow10(3))
        .put("mega", Rational.pow10(6))
        .put("giga", Rational.pow10(9))
        .put("tera", Rational.pow10(12))
        .put("peta", Rational.pow10(15))
        .put("exa", Rational.pow10(18))
        .put("zetta", Rational.pow10(21))
        .put("yotta", Rational.pow10(24))        
        .build();

    static final Set<String> SKIP_PREFIX = ImmutableSet.of(
        "millimeter-of-mercury", 
        "kilogram"
        );

    /** 
     * If there is no prefix, return the unit and Rational.ONE.
     * If there is a prefix return the unit (with prefix stripped) and the prefix factor 
     * */
    private String stripPrefix(String unit, Output<Rational> deprefix) {
        deprefix.value = Rational.ONE;
        if (SKIP_PREFIX.contains(unit)) {
            return unit;
        }

        for (Entry<String, Rational> entry : PREFIXES.entrySet()) {
            String prefix = entry.getKey();
            if (unit.startsWith(prefix)) {
                deprefix.value = entry.getValue();
                return unit.substring(prefix.length());
            }
        }
        return unit;
    }

    public BiMap<String, String> getBaseUnitToQuantity() {
        return (BiMap<String, String>) baseUnitToQuantity;
    }
    
    public String getQuantityFromUnit(String unit, boolean showYourWork) {
        Output<String> metricUnit = new Output<>();
        ConversionInfo unitInfo = parseUnitId(unit, metricUnit, showYourWork);
        return getQuantityFromBaseUnit(metricUnit.value);
    }

    public String getQuantityFromBaseUnit(String baseUnit) {
        String result = getQuantityFromBaseUnit2(baseUnit);
        if (result != null) {
            return result;
        }
        result = getQuantityFromBaseUnit2(reciprocalOf(baseUnit));
        if (result != null) {
            result += "-inverse";
        }
        return result;
    }

    public String getQuantityFromBaseUnit2(String baseUnit) {
        String result = baseUnitToQuantity.get(baseUnit);
        if (result != null) {
            return result;
        }
        UnitId unitId = createUnitId(baseUnit);
        UnitId resolved = unitId.resolve();
        return baseUnitToQuantity.get(resolved.toString());
    }

    public Set<String> getSimpleUnits() {
        return sourceToTargetInfo.keySet();
    }

    public void addAliases(Map<String, R2<List<String>, String>> tagToReplacement) {
        fixDenormalized = new TreeMap<>();
        for (Entry<String, R2<List<String>, String>> entry : tagToReplacement.entrySet()) {
            final String badCode = entry.getKey();
            final List<String> replacements = entry.getValue().get0();
            fixDenormalized.put(badCode, replacements.iterator().next());
        }
        fixDenormalized = ImmutableMap.copyOf(fixDenormalized);
    }

    public Map<String, TargetInfo> getInternalConversionData() {
        return sourceToTargetInfo; 
    }

    public Multimap<String, String> getSourceToSystems() {
        return sourceToSystems;
    }

    public String reciprocalOf(String value) {
        // quick version, input guarantteed to be normalized
        int index = value.indexOf("-per-");
        if (index < 0) {
            return null;
        }
        return value.substring(index+5) + "-per-" + value.substring(0, index);
    }

    public Rational parseRational(String source) {
        return rationalParser.parse(source);
    }

    private String showRational(String title, Rational rational, String unit) {
        String doubleString = "";
        if (!rational.denominator.equals(BigInteger.ONE)) {
            String doubleValue = String.valueOf(rational.toBigDecimal(MathContext.DECIMAL32).doubleValue());
            Rational reverse = parseRational(doubleValue);
            doubleString = (reverse.equals(rational) ? " = " : " ≅ ") + doubleValue;
        }
        final String endResult = title + rational + doubleString + (unit != null ? " " + unit: "");
        return endResult;
    }

    public Rational convert(final Rational sourceValue, final String sourceUnit, final String targetUnit, boolean showYourWork) {
        if (showYourWork) {
            System.out.println(showRational("\nconvert: ", sourceValue, sourceUnit) + " ⟹ " + targetUnit);
        }
        Output<String> sourceBase = new Output<>();
        Output<String> targetBase = new Output<>();
        ConversionInfo sourceConversionInfo = parseUnitId(sourceUnit, sourceBase, showYourWork);
        if (sourceConversionInfo == null) {
            if (showYourWork) System.out.println("! unknown unit: " + sourceUnit);
            return Rational.NaN;
        }
        Rational intermediateResult = sourceConversionInfo.convert(sourceValue);
        if (showYourWork) System.out.println(showRational(" ⟹ intermediate: ", intermediateResult, sourceBase.value));
        if (showYourWork) System.out.println(" ⟹ " + targetUnit);
        ConversionInfo targetConversionInfo = parseUnitId(targetUnit, targetBase, showYourWork);
        if (targetConversionInfo == null) {
            if (showYourWork) System.out.println("! unknown unit: " + targetUnit);
            return Rational.NaN;
        }
        if (!sourceBase.value.equals(targetBase.value)) {
            String reciprocalUnit = reciprocalOf(sourceBase.value);
            if (reciprocalUnit == null || !targetBase.value.equals(reciprocalUnit)) {
                if (showYourWork) System.out.println("! incomparable units: " + sourceUnit + " and " + targetUnit);
                return Rational.NaN;
            }
            intermediateResult = intermediateResult.reciprocal();
            if (showYourWork) System.out.println(showRational(" ⟹ 1/intermediate: ", intermediateResult, reciprocalUnit));
        }
        Rational result = targetConversionInfo.convertBackwards(intermediateResult);
        if (showYourWork) System.out.println(showRational(" ⟹ target: ", result, targetUnit));
        return result;
    }
}
