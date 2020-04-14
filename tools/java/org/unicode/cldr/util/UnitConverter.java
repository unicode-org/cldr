package org.unicode.cldr.util;

import java.math.BigInteger;
import java.math.MathContext;
import java.text.MessageFormat;
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
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.Rational.FormatStyle;
import org.unicode.cldr.util.Rational.RationalParser;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.Validity.Status;

import com.google.common.base.Joiner;
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
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.Freezable;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

public class UnitConverter implements Freezable<UnitConverter> {

    public static final Integer INTEGER_ONE = Integer.valueOf(1);

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
    private Comparator<String> quantityComparator; 
    private Map<String,String> fixDenormalized;
    private ImmutableMap<String, UnitId> idToUnitId;

    public static final BiMap<String,String> SHORT_TO_LONG_ID;
    static {
        Set<String> VALID_UNITS = Validity.getInstance().getStatusToCodes(LstrType.unit).get(Status.regular);
        Map<String,String> _SHORT_TO_LONG_ID = new LinkedHashMap<>();
        for (String longUnit : VALID_UNITS) {
            int dashPos = longUnit.indexOf('-');
            String coreUnit = longUnit.substring(dashPos+1);
            _SHORT_TO_LONG_ID.put(coreUnit, longUnit);
        }
        SHORT_TO_LONG_ID = ImmutableBiMap.copyOf(_SHORT_TO_LONG_ID);
    }

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
        if (status != null) {
            baseUnitToStatus.put(baseUnit, status);
        }
        quantityToSimpleUnits.put(quantity, baseUnit);
    }


    public static final Set<String> BASE_UNIT_PARTS = ImmutableSet.<String>builder()
        .add("per").add("square").add("cubic").addAll(BASE_UNITS)
        .build();

    static final Pattern PLACEHOLDER = Pattern.compile("[ \\u00A0\\u200E\\u200F]*\\{0\\}[ \\u00A0\\u200E\\u200F]*");
    public static final boolean HACK = true;
    private static final ULocale GREEK = new ULocale("el");

    @Override
    public boolean isFrozen() {
        return frozen;
    }

    @Override
    public UnitConverter freeze() {
        if (!frozen) {
            frozen = true;
            rationalParser.freeze();
            sourceToTargetInfo = ImmutableMap.copyOf(sourceToTargetInfo);
            quantityToSimpleUnits = ImmutableMultimap.copyOf(quantityToSimpleUnits);
            quantityComparator = getQuantityComparator(baseUnitToQuantity, baseUnitToStatus);
            sourceToSystems = ImmutableMultimap.copyOf(sourceToSystems);    
            // other fields are frozen earlier in processing
            Builder<String> builder = ImmutableSet.<String>builder()
                .addAll(BASE_UNITS);
            for (TargetInfo s : sourceToTargetInfo.values()) {
                builder.add(s.target);
            }
            baseUnits = builder.build();
            continuations = ImmutableMultimap.copyOf(continuations);
            targetInfoComparator = new TargetInfoComparator();

            Map<String, UnitId> _idToUnitId = new TreeMap<>();
            for (Entry<String, String> shortAndLongId : SHORT_TO_LONG_ID.entrySet()) {
                String shortId = shortAndLongId.getKey();
                String longId = shortAndLongId.getKey();
                UnitId uid = createUnitId(shortId).freeze();
                boolean doTest = false;
                Output<Rational> deprefix = new Output<>();
                for (Entry<String, Integer> entry : uid.numUnitsToPowers.entrySet()) {
                    final String unitPart = entry.getKey();
                    UnitConverter.stripPrefix(unitPart, deprefix );
                    if (!deprefix.value.equals(Rational.ONE) || !entry.getValue().equals(INTEGER_ONE)) {
                        doTest = true;
                        break;
                    }
                }
                if (!doTest) {
                    for (Entry<String, Integer> entry : uid.denUnitsToPowers.entrySet()) {
                        final String unitPart = entry.getKey();
                        UnitConverter.stripPrefix(unitPart, deprefix);
                        if (!deprefix.value.equals(Rational.ONE)) {
                            doTest = true;
                            break;
                        }
                    }
                }
                if (doTest) {
                    _idToUnitId.put(shortId, uid);
                    _idToUnitId.put(longId, uid);
                }
            }
            idToUnitId = ImmutableMap.copyOf(_idToUnitId);
        }
        return this;
    }

    @Override
    public UnitConverter cloneAsThawed() {
        throw new UnsupportedOperationException();
    }


    public static final class ConversionInfo implements Comparable<ConversionInfo> {
        public final Rational factor;
        public final Rational offset;

        static final ConversionInfo IDENTITY = new ConversionInfo(Rational.ONE, Rational.ZERO);

        public ConversionInfo(Rational factor, Rational offset) {
            this.factor = factor;
            this.offset = offset;
        }

        public Rational convert(Rational source) {
            return source.multiply(factor).add(offset);
        }

        public Rational convertBackwards(Rational source) {
            return source.subtract(offset).divide(factor);
        }

        public ConversionInfo invert() {
            Rational factor2 = factor.reciprocal();
            Rational offset2 = offset.equals(Rational.ZERO) ? Rational.ZERO : offset.divide(factor).negate();
            return new ConversionInfo(factor2, offset2);
            // TODO fix reciprocal
        }

        @Override
        public String toString() {
            return toString("x");
        }
        public String toString(String unit) {
            return factor.toString(FormatStyle.simple) 
                + " * " + unit
                + (offset.equals(Rational.ZERO) ? "" : 
                    (offset.compareTo(Rational.ZERO) < 0 ? " - " : " - ")
                    + offset.abs().toString(FormatStyle.simple));
        }

        public String toDecimal() {
            return toDecimal("x");
        }
        public String toDecimal(String unit) {
            return factor.toBigDecimal(MathContext.DECIMAL64) 
                +  " * " + unit
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
            return offset.compareTo(o.offset);
        }
        @Override
        public boolean equals(Object obj) {
            return 0 == compareTo((ConversionInfo)obj);
        }
        @Override
        public int hashCode() {
            return Objects.hash(factor, offset);
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

    public void addRaw(String source, String target, String factor, String offset, String systems) {
        ConversionInfo info = new ConversionInfo(
            factor == null ? Rational.ONE : rationalParser.parse(factor), 
                offset == null ? Rational.ZERO : rationalParser.parse(offset));
        Map<String, String> args = new LinkedHashMap<>();
        if (factor != null) {
            args.put("factor", factor);
        }
        if (offset != null) {
            args.put("offset", offset);
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

    private Comparator<String> getQuantityComparator(Map<String, String> baseUnitToQuantity2, Map<String, String> baseUnitToStatus2) {
        // We want to sort all the quantities so that we have a natural ordering within compound units. So kilowatt-hour, not hour-kilowatt.
        // For simple quantities, just use the ordering from baseUnitToStatus
        MapComparator<String> simpleBaseUnitComparator = new MapComparator<>(baseUnitToStatus2.keySet()).freeze();
        // For non-symbol quantities, use the ordering of the UnitIds
        Map<UnitId, String> unitIdToQuantity = new TreeMap<>();
        for (Entry<String, String> buq : baseUnitToQuantity2.entrySet()) {
            UnitId uid = new UnitId(simpleBaseUnitComparator).add(continuations, buq.getKey(), true, 1).freeze();
            unitIdToQuantity.put(uid, buq.getValue());
        }
        System.out.println(Joiner.on("\n").join(unitIdToQuantity.values()));
        return new MapComparator<String>(unitIdToQuantity.values()).freeze();
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

    public Rational convertDirect(Rational source, String sourceUnit, String targetUnit) {
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

    /**
     * Takes a derived unit id, and produces the equivalent derived base unit id and UnitInfo to convert to it
     * @author markdavis
     * @param showYourWork TODO
     *
     */
    public ConversionInfo parseUnitId (String derivedUnit, Output<String> metricUnit, boolean showYourWork) {
        metricUnit.value = null;

        UnitId outputUnit = new UnitId(UNIT_COMPARATOR);
        Rational numerator = Rational.ONE;
        Rational denominator = Rational.ONE;
        boolean inNumerator = true;
        int power = 1;

        Output<Rational> deprefix = new Output<>();      
        Rational offset = Rational.ZERO;
        int countUnits = 0;
        for (Iterator<String> it = Continuation.split(derivedUnit, continuations).iterator(); it.hasNext();) {
            String unit = it.next();
            ++countUnits;
            if (unit.equals("square")) {
                if (power != 1) {
                    throw new IllegalArgumentException("Can't have power of " + unit);
                }
                power = 2;
                if (showYourWork) System.out.println(showRational("\t " + unit + ": ", Rational.of(power), "power"));
            } else if (unit.equals("cubic")) {
                if (power != 1) {
                    throw new IllegalArgumentException("Can't have power of " + unit);
                }
                power = 3;
                if (showYourWork) System.out.println(showRational("\t " + unit + ": ", Rational.of(power), "power"));
            } else if (unit.startsWith("pow")) {
                if (power != 1) {
                    throw new IllegalArgumentException("Can't have power of " + unit);
                }
                power = Integer.parseInt(unit.substring(3));
                if (showYourWork) System.out.println(showRational("\t " + unit + ": ", Rational.of(power), "power"));
            } else if (unit.equals("per")) {
                if (power != 1) {
                    throw new IllegalArgumentException("Can't have power of per");
                }
                if (showYourWork && inNumerator) System.out.println("\tper");
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
                if (showYourWork) {
                    if (!deprefix.value.equals(Rational.ONE)) {
                        System.out.println(showRational("\tprefix: ", deprefix.value, unit));
                    } else {
                        System.out.println("\t" + unit);
                    }
                }

                Rational value = deprefix.value;
                if (!isSimpleBaseUnit(unit)) {
                    TargetInfo info = sourceToTargetInfo.get(unit);
                    if (info == null) {
                        if (showYourWork) System.out.println("\t⟹ no conversion for: " + unit);
                        return null; // can't convert
                    }
                    String baseUnit = info.target;

                    value = info.unitInfo.factor.multiply(value);
                    //if (showYourWork && !info.unitInfo.factor.equals(Rational.ONE)) System.out.println(showRational("\tfactor: ", info.unitInfo.factor, baseUnit));
                    // Special handling for offsets. We disregard them if there are any other units.
                    if (countUnits == 1 && !it.hasNext()) {
                        offset = info.unitInfo.offset;
                        if (showYourWork && !info.unitInfo.offset.equals(Rational.ZERO)) System.out.println(showRational("\toffset: ", info.unitInfo.offset, baseUnit));
                    }
                    unit = baseUnit;
                }
                for (int p = 1; p <= power; ++p) {
                    String title = "";
                    if (value.equals(Rational.ONE)) {
                        if (showYourWork) System.out.println("\t(already base unit)");
                        continue;
                    } else if (inNumerator) {
                        numerator = numerator.multiply(value);
                        title = "\t× ";
                    } else {
                        denominator = denominator.multiply(value);
                        title = "\t÷ ";
                    }
                    if (showYourWork) System.out.println(showRational("\t× ", value, " ⟹ " + unit) + "\t" + numerator.divide(denominator) + "\t" + numerator.divide(denominator).doubleValue());
                }
                // create cleaned up target unitid
                outputUnit.add(continuations, unit, inNumerator, power);
                power = 1;
            }
        }
        metricUnit.value = outputUnit.toString();
        return new ConversionInfo(numerator.divide(denominator), offset);
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
    public enum PathType {perUnit, times, per, prefix, power, unit}

    /** 
     * Only handles the canonical units; no kilo-, only normalized, etc.
     * @author markdavis
     *
     */
    public class UnitId implements Freezable<UnitId>, Comparable<UnitId> {
        public Map<String, Integer> numUnitsToPowers;
        public Map<String, Integer> denUnitsToPowers;
        public EntrySetComparator<String, Integer> entrySetComparator;
        private boolean frozen = false;

        private UnitId(Comparator<String> comparator) {
            numUnitsToPowers = new TreeMap<>(comparator);
            denUnitsToPowers = new TreeMap<>(comparator);
            entrySetComparator = new EntrySetComparator<String, Integer>(comparator, Comparator.naturalOrder());
        } // 

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

        public String toString(CLDRFile resolvedFile, String width, String _pluralCategory, String caseVariant) {
            return toString(resolvedFile, width, _pluralCategory, caseVariant, null, null);
        }

        public String toString(CLDRFile resolvedFile, String width, String _pluralCategory, String caseVariant, String genderVariant, Multimap<PathType, String> partsUsed) {
            // special-case single unit
            String result = getTrans(PathType.unit, resolvedFile, width, this.toString(), _pluralCategory, caseVariant, genderVariant, partsUsed);
            if (result != null) {
                return result;
            }

            result = null;
            String numerator = null;
            String timesPattern = null;
            boolean before = false;
            String placeholderPattern = null;
            Output<Integer> deprefix = new Output<>();
            Matcher placeholderMatcher = PLACEHOLDER.matcher("");
            PluralInfo pluralInfo = CLDRConfig.getInstance().getSupplementalDataInfo().getPlurals(resolvedFile.getLocaleID());
            PluralRules pluralRules = pluralInfo.getPluralRules();
            String singularPluralCategory = pluralRules.select(1d);
            final ULocale locale = new ULocale(resolvedFile.getLocaleID());

            for (int i = 1; i >= 0; --i) { // two passes, numerator then den.
                boolean positivePass = i > 0;
                Map<String, Integer> target = positivePass ? numUnitsToPowers : denUnitsToPowers;
                int unitsLeft = target.size();
                for (Entry<String, Integer> entry : target.entrySet()) {
                    String unit = entry.getKey();
                    int power = entry.getValue();
                    unitsLeft--;
                    String pluralCategory = unitsLeft == 0 && positivePass ? _pluralCategory : singularPluralCategory;

                    if (!positivePass) {
                        if (numerator == null) {
                            numerator = result; // from now on, result ::= denominator
                            result = null;
                        }
                        if (result == null && numerator != null && denUnitsToPowers.size() == 1) { // special case exact match for per form, and no previous result
                            switch (power) {
                            case 1: 
                                break;
                            case 2: 
                                unit = "square-" + unit;
                                break;
                            case 3: 
                                unit = "cubic-" + unit;
                                break;
                            default: 
                                throw new IllegalArgumentException("No power pattern > 3: " + this);
                            }
                            String fullPerPattern = getTrans(PathType.perUnit, resolvedFile, width, unit, _pluralCategory, caseVariant, genderVariant, partsUsed);
                            if (fullPerPattern != null) {
                                numerator = MessageFormat.format(fullPerPattern, numerator);
                                continue;
                            }
                        }
                    }

                    // TODO for times pattern, use singular for all but LAST form

                    String powerPatternPath = null;
                    switch (power) {
                    case 1: 
                        break;
                    case 2: 
                        powerPatternPath = "//ldml/units/unitLength[@type=\"" 
                            + width + "\"]/compoundUnit[@type=\"power2\"]/compoundUnitPattern1[@count=\"" 
                            + pluralCategory + "\"]";
                        // example: Quadrat{0}
                        break;
                    case 3: 
                        powerPatternPath = "//ldml/units/unitLength[@type=\"" 
                            + width + "\"]/compoundUnit[@type=\"power3\"]/compoundUnitPattern1[@count=\"" 
                            + pluralCategory + "\"]";
                        break;
                    default: 
                        throw new IllegalArgumentException("No power pattern > 3: " + this);
                    }

                    // handle prefix, like kilo-
                    unit = stripPrefixInt(unit, deprefix);
                    String prefixPattern = null;
                    if (deprefix.value != 1) {
                        prefixPattern = getTrans(PathType.prefix, resolvedFile, width, "10p" + deprefix.value, _pluralCategory, caseVariant, genderVariant, partsUsed);
                    }

                    // get the core pattern. Detect and remove the the placeholder (and surrounding spaces)
                    String unitPattern = getTrans(PathType.unit, resolvedFile, width, unit, pluralCategory, caseVariant, genderVariant, partsUsed);
                    if (unitPattern == null) {
                        return null; // unavailable
                    }
                    // we are set up for 2 kinds of placeholder patterns for units. {0}\s?stuff or stuff\s?{0}, or nothing(Eg Arabic)
                    if (placeholderMatcher.reset(unitPattern).find()) {
                        if (placeholderMatcher.start() == 0) {
                            before = true;
                            unitPattern = unitPattern.substring(placeholderMatcher.end());
                        } else if (placeholderMatcher.end() == unitPattern.length()) {
                            before = false;
                            unitPattern = unitPattern.substring(0, placeholderMatcher.start());
                        } else {
                            throw new IllegalArgumentException("Unexpected pattern «" + unitPattern + "»\t" + Utility.hex(unitPattern));
                        }
                        placeholderPattern = placeholderMatcher.group();
                    }

                    // we have all the pieces, so build it up
                    if (prefixPattern != null) {
                        unitPattern = combineLowercasing(locale, width, prefixPattern, unitPattern);
                    }

                    String powerPattern = null;
                    switch (power) {
                    case 1: 
                        break;
                    case 2: 
                        powerPattern = getTrans(PathType.power, resolvedFile, width, "power2", pluralCategory, caseVariant, genderVariant, partsUsed);
                        break;
                    case 3: 
                        powerPattern = getTrans(PathType.power, resolvedFile, width, "power3", pluralCategory, caseVariant, genderVariant, partsUsed);
                        break;
                    default: 
                        throw new IllegalArgumentException("No power pattern > 3: " + this);
                    }

                    if (powerPattern != null) {
                        unitPattern = combineLowercasing(locale, width, powerPattern, unitPattern);
                    }

                    if (result != null) {
                        if (timesPattern == null) {
                            if (HACK) {                                // TODO fix hack!
                                if (locale.equals(ULocale.ENGLISH)) {
                                    timesPattern = "{0}-{1}";
                                }
                            }
                            if (timesPattern == null) {
                                // timesPattern = getTrans(PathType.times, resolvedFile, width, null, null, partsUsed);
                                String timesPatternPath = "//ldml/units/unitLength[@type=\""
                                    + width + "\"]/compoundUnit[@type=\"times\"]/compoundUnitPattern";
                                timesPattern = resolvedFile.getStringValue(timesPatternPath);
                            }
                        }
                        result = MessageFormat.format(timesPattern, result, unitPattern);
                    } else {
                        result = unitPattern;
                    }
                }
            }
            // glue the two parts together, if we have them
            if (numerator != null) {
                if (result == null) {
                    result = numerator;
                } else {
                    String perPattern = getTrans(PathType.per, resolvedFile, width, null, _pluralCategory, caseVariant, genderVariant, partsUsed);
                    result = MessageFormat.format(perPattern, numerator, result);
                }
            }
            return placeholderPattern == null ? result
                : before ? placeholderPattern + result
                    : result + placeholderPattern;
        }

        private String combineLowercasing(final ULocale locale, String width, String prefixPattern, String unitPattern) {
            // catch special case, ZentiLiter
            if (width.equals("long") && !prefixPattern.contains(" {")) {
                unitPattern = UCharacter.toLowerCase(locale, unitPattern);
            }
            unitPattern = MessageFormat.format(prefixPattern, unitPattern);
            return unitPattern;
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
            UnitId result = new UnitId(UNIT_COMPARATOR);
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

        @Override
        public int compareTo(UnitId o) {
            int diff = compareEntrySets(numUnitsToPowers.entrySet(), o.numUnitsToPowers.entrySet(), entrySetComparator);
            if (diff != 0) return diff;
            return compareEntrySets(denUnitsToPowers.entrySet(), o.denUnitsToPowers.entrySet(), entrySetComparator);
        }
    }

    public static class EntrySetComparator<K extends Comparable<K>,V> implements Comparator<Entry<K, V>> {
        Comparator<K> kComparator;
        Comparator<V> vComparator;
        public EntrySetComparator(Comparator<K> kComparator, Comparator<V> vComparator) {
            this.kComparator = kComparator;
            this.vComparator = vComparator;
        }
        @Override
        public int compare(Entry<K, V> o1, Entry<K, V> o2) {
            int diff = kComparator.compare(o1.getKey(), o2.getKey());
            if (diff != 0) {
                return diff;
            }
            diff = vComparator.compare(o1.getValue(), o2.getValue());
            if (diff != 0) {
                return diff;
            }
            return o1.getKey().compareTo(o2.getKey());
        }
    }

    public static <K extends Comparable<K>, V extends Comparable<V>, T extends Entry<K, V>> int compareEntrySets(Collection<T> o1, Collection<T> o2, Comparator<T> comparator) {
        Iterator<T> iterator1 = o1.iterator();
        Iterator<T> iterator2 = o2.iterator();
        while (true) {
            if (!iterator1.hasNext()) {
                return iterator2.hasNext() ? -1 : 0;
            } else if (!iterator2.hasNext()) {
                return 1;
            }
            T item1 = iterator1.next();
            T item2 = iterator2.next();
            int diff = comparator.compare(item1, item2);
            if (diff != 0) {
                return diff;
            }
        }
    }

    private ConcurrentHashMap<String, UnitId> UNIT_ID = new ConcurrentHashMap<>();
    // TODO This is safe but should use regular cache
    public final UnitId createUnitId(String unit) {
        UnitId result = UNIT_ID.get(unit);
        if (result == null) {
            result = new UnitId(UNIT_COMPARATOR).add(continuations, unit, true, 1).freeze();
            UNIT_ID.put(unit, result);
        }
        return result;
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
    public static final ImmutableMap<String, Integer> PREFIX_POWERS = ImmutableMap.<String, Integer>builder()
        .put("yocto", -24)
        .put("zepto", -21)
        .put("atto", -18)
        .put("femto", -15)
        .put("pico", -12)
        .put("nano", -9)
        .put("micro", -6)
        .put("milli", -3)
        .put("centi", -2)
        .put("deci", -1)
        .put("deka", 1)
        .put("hecto", 2)
        .put("kilo", 3)
        .put("mega", 6)
        .put("giga", 9)
        .put("tera", 12)
        .put("peta", 15)
        .put("exa", 18)
        .put("zetta", 21)
        .put("yotta", 24)        
        .build();

    public static final ImmutableMap<String, Rational> PREFIXES;
    static {
        Map<String, Rational> temp = new LinkedHashMap<>();
        for (Entry<String, Integer> entry : PREFIX_POWERS.entrySet()) {
            temp.put(entry.getKey(), Rational.pow10(entry.getValue()));
        }
        PREFIXES = ImmutableMap.copyOf(temp);
    }

    static final Set<String> SKIP_PREFIX = ImmutableSet.of(
        "millimeter-ofhg", 
        "kilogram"
        );

    /** 
     * If there is no prefix, return the unit and ONE.
     * If there is a prefix return the unit (with prefix stripped) and the prefix factor 
     * */
    private static <V> String stripPrefixCommon(String unit, Output<V> deprefix, Map<String, V> unitMap) {
        if (SKIP_PREFIX.contains(unit)) {
            return unit;
        }

        for (Entry<String, V> entry : unitMap.entrySet()) {
            String prefix = entry.getKey();
            if (unit.startsWith(prefix)) {
                deprefix.value = entry.getValue();
                return unit.substring(prefix.length());
            }
        }
        return unit;
    }

    public static String stripPrefix(String unit, Output<Rational> deprefix) {
        deprefix.value = Rational.ONE;
        return stripPrefixCommon(unit, deprefix, PREFIXES);
    }

    public static String stripPrefixInt(String unit, Output<Integer> deprefix) {
        deprefix.value = Integer.valueOf(1);
        return stripPrefixCommon(unit, deprefix, PREFIX_POWERS);
    }

    public BiMap<String, String> getBaseUnitToQuantity() {
        return (BiMap<String, String>) baseUnitToQuantity;
    }

    public String getQuantityFromUnit(String unit, boolean showYourWork) {
        Output<String> metricUnit = new Output<>();
        unit = fixDenormalized(unit);
        ConversionInfo unitInfo = parseUnitId(unit, metricUnit, showYourWork);
        return metricUnit == null ? null : getQuantityFromBaseUnit(metricUnit.value);
    }

    public String getQuantityFromBaseUnit(String baseUnit) {
        if (baseUnit == null) {
            throw new NullPointerException("baseUnit");
        }
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

    private String getQuantityFromBaseUnit2(String baseUnit) {
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

    public Set<String> getSystems(String unit) {
        Set<String> result = new TreeSet<>();
        UnitId id = createUnitId(unit);
        for (String subunit : id.denUnitsToPowers.keySet()) {
            addSystems(result, subunit);
        }
        for (String subunit : id.numUnitsToPowers.keySet()) {
            addSystems(result, subunit);
        }
        return result;
    }

    public static final Set<String> OTHER_SYSTEM = ImmutableSet.of(
        "g-force", "dalton", "calorie", "earth-radius", 
        "solar-radius", "solar-radius", "astronomical-unit", "light-year", "parsec", "earth-mass", 
        "solar-mass", "bit", "byte", "karat", "solar-luminosity", "ofhg", "atmosphere", 
        "pixel", "dot", "permillion", "permyriad", "permille", "percent", "karat", "portion",
        "minute", "hour", "day", "day-person", "week", "week-person",
        "year", "year-person", "decade", "month", "month-person", "century",
        "arc-second", "arc-minute", "degree", "radian", "revolution",
        "electronvolt", 
        // quasi-metric
        "dunam", "mile-scandinavian", "carat", "cup-metric", "pint-metric"
        );

    private void addSystems(Set<String> result, String subunit) {
        Collection<String> systems = sourceToSystems.get(subunit);
        if (!systems.isEmpty()) {
            result.addAll(systems);
        } else if (!OTHER_SYSTEM.contains(subunit)) {
            result.add("metric");
        }
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

    public String showRational(String title, Rational rational, String unit) {
        String doubleString = showRational2(rational, " = ", " ≅ ");
        final String endResult = title + rational + doubleString + (unit != null ? " " + unit: "");
        return endResult;
    }

    public String showRational(Rational rational, String approximatePrefix) {
        String doubleString = showRational2(rational, "", approximatePrefix);
        return doubleString.isEmpty() ? rational.numerator.toString() : doubleString;
    }

    public String showRational2(Rational rational, String equalPrefix, String approximatePrefix) {
        String doubleString = "";
        if (!rational.denominator.equals(BigInteger.ONE)) {
            String doubleValue = String.valueOf(rational.toBigDecimal(MathContext.DECIMAL32).doubleValue());
            Rational reverse = parseRational(doubleValue);
            doubleString = (reverse.equals(rational) ? equalPrefix : approximatePrefix) + doubleValue;
        }
        return doubleString;
    }

    public Rational convert(Rational sourceValue, String sourceUnit, final String targetUnit, boolean showYourWork) {
        if (showYourWork) {
            System.out.println(showRational("\nconvert:\t", sourceValue, sourceUnit) + " ⟹ " + targetUnit);
        }
        sourceUnit = fixDenormalized(sourceUnit);
        Output<String> sourceBase = new Output<>();
        Output<String> targetBase = new Output<>();
        ConversionInfo sourceConversionInfo = parseUnitId(sourceUnit, sourceBase, showYourWork);
        if (sourceConversionInfo == null) {
            if (showYourWork) System.out.println("! unknown unit: " + sourceUnit);
            return Rational.NaN;
        }
        Rational intermediateResult = sourceConversionInfo.convert(sourceValue);
        if (showYourWork) System.out.println(showRational("intermediate:\t", intermediateResult, sourceBase.value));
        if (showYourWork) System.out.println("invert:\t" + targetUnit);
        ConversionInfo targetConversionInfo = parseUnitId(targetUnit, targetBase, showYourWork);
        if (targetConversionInfo == null) {
            if (showYourWork) System.out.println("! unknown unit: " + targetUnit);
            return Rational.NaN;
        }
        if (!sourceBase.value.equals(targetBase.value)) {
            // try resolving
            String sourceBaseFixed = createUnitId(sourceBase.value).resolve().toString();
            String targetBaseFixed = createUnitId(targetBase.value).resolve().toString();
            // try reciprocal
            if (!sourceBaseFixed.equals(targetBaseFixed)) {
                String reciprocalUnit = reciprocalOf(sourceBase.value);
                if (reciprocalUnit == null || !targetBase.value.equals(reciprocalUnit)) {
                    if (showYourWork) System.out.println("! incomparable units: " + sourceUnit + " and " + targetUnit);
                    return Rational.NaN;
                }
                intermediateResult = intermediateResult.reciprocal();
                if (showYourWork) System.out.println(showRational(" ⟹ 1/intermediate:\t", intermediateResult, reciprocalUnit));
            }
        }
        Rational result = targetConversionInfo.convertBackwards(intermediateResult);
        if (showYourWork) System.out.println(showRational("target:\t", result, targetUnit));
        return result;
    }

    public String fixDenormalized(String unit) {
        String fixed = fixDenormalized.get(unit);
        return fixed == null ? unit : fixed;
    }

    public Map<String, Rational> getConstants() {
        return rationalParser.getConstants();
    }

    public String getBaseUnitFromQuantity(String unitQuantity) {
        boolean invert = false;
        if (unitQuantity.endsWith("-inverse")) {
            invert = true;
            unitQuantity = unitQuantity.substring(0,unitQuantity.length()-8);
        }
        String bu = ((BiMap<String,String>) baseUnitToQuantity).inverse().get(unitQuantity);
        if (bu == null) {
            return null;
        }
        return invert ? reciprocalOf(bu) : bu;
    }

    public Set<String> getQuantities() {
        return getBaseUnitToQuantity().inverse().keySet();
    }

    public static String getTrans(PathType type, CLDRFile resolvedFile, String width, String shortUnitId, String pluralCategory, String caseVariant, String genderVariant, Multimap<PathType, String> partsUsed) {
        final String pathPrefix = "//ldml/units/unitLength[@type=\"" + width;
        String path;
        String longUnitId;
        switch (type) {
        case times: 
            path = pathPrefix + "\"]/compoundUnit[@type=\"" + "times" + "\"]/compoundUnitPattern";
            break;
        case per: 
            path = pathPrefix + "\"]/compoundUnit[@type=\"" + "per" + "\"]/compoundUnitPattern";
            break;
        case prefix: 
            longUnitId = SHORT_TO_LONG_ID.get(shortUnitId);
            path = pathPrefix + "\"]/compoundUnit[@type=\"" + longUnitId + "\"]/unitPrefixPattern";
            break;
        case power: 
           longUnitId = SHORT_TO_LONG_ID.get(shortUnitId);
           path  = pathPrefix + "\"]/compoundUnit[@type=\"" + longUnitId + "\"]/compoundUnitPattern1[@count=\"" + pluralCategory + "\"]";
            break;
        case unit: 
            longUnitId = SHORT_TO_LONG_ID.get(shortUnitId);
            path =  pathPrefix + "\"]/unit[@type=\""  + longUnitId + "\"]/unitPattern[@count=\"" + pluralCategory + "\"]";
            break;
        case perUnit: 
            path = pathPrefix + "\"]/unit[@type=\"" + shortUnitId + "\"]/perUnitPattern";
            break; 
        default: throw new IllegalArgumentException("PathType: " + type);
        }
        String result = resolvedFile.getStringValue(path);
        if (result == null) {
            int debug = 0;
        }
        if (partsUsed != null) {
            CLDRFile.Status status = new CLDRFile.Status();
            String foundLocale = resolvedFile.getSourceLocaleID(path, status );
            final String state = (foundLocale.equals(resolvedFile.getLocaleID()) ? "=" : "≠") + (status.pathWhereFound.equals(path) ? "=" : "≠");
            partsUsed.put(type, (result != null ? result : "∅") + (state.equals("==") ? "" : "; "
                + state + "; "
                + width + "; "
                + (shortUnitId != null ? shortUnitId : "∅") + "; "
                + (pluralCategory != null ? pluralCategory : "∅"))
                );
        }
        return result;
    }

    public enum UnitComplexity {simple, non_simple}
    
    private ConcurrentHashMap<String, UnitComplexity> COMPLEXITY = new ConcurrentHashMap<>();
    // TODO This is safe but should use regular cache

    public UnitComplexity getComplexity (String longOrShortId){
        UnitComplexity result = COMPLEXITY.get(longOrShortId);
        if (result == null) {
            String shortId;
            String longId = SHORT_TO_LONG_ID.get(longOrShortId);
            if (longId == null) {
                longId = longOrShortId;
                shortId = SHORT_TO_LONG_ID.inverse().get(longId);
            } else {
                shortId = longOrShortId;
            }
            UnitId uid = createUnitId(shortId);
            result = UnitComplexity.simple;

            if (uid.numUnitsToPowers.size() != 1 || !uid.denUnitsToPowers.isEmpty()) {
                result = UnitComplexity.non_simple;
            } else {
                Output<Rational> deprefix = new Output<>();
                for (Entry<String, Integer> entry : uid.numUnitsToPowers.entrySet()) {
                    final String unitPart = entry.getKey();
                    UnitConverter.stripPrefix(unitPart, deprefix );
                    if (!deprefix.value.equals(Rational.ONE) || !entry.getValue().equals(INTEGER_ONE)) {
                        result = UnitComplexity.non_simple;
                        break;
                    }
                }
                if (result == UnitComplexity.simple) {
                    for (Entry<String, Integer> entry : uid.denUnitsToPowers.entrySet()) {
                        final String unitPart = entry.getKey();
                        UnitConverter.stripPrefix(unitPart, deprefix);
                        if (!deprefix.value.equals(Rational.ONE)) {
                            result = UnitComplexity.non_simple;
                            break;
                        }
                    }
                }
            }
            COMPLEXITY.put(shortId, result);
            COMPLEXITY.put(longId, result);
        }
        return result;
    }
}
