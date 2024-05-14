package org.unicode.cldr.util;

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
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.number.UnlocalizedNumberFormatter;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.Freezable;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.unicode.cldr.util.GrammarDerivation.CompoundUnitStructure;
import org.unicode.cldr.util.GrammarDerivation.Values;
import org.unicode.cldr.util.GrammarInfo.GrammaticalFeature;
import org.unicode.cldr.util.Rational.FormatStyle;
import org.unicode.cldr.util.Rational.RationalParser;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.UnitIdComponentType;
import org.unicode.cldr.util.Validity.Status;

public class UnitConverter implements Freezable<UnitConverter> {
    public static boolean DEBUG = false;
    public static final Integer INTEGER_ONE = 1;

    static final Splitter BAR_SPLITTER = Splitter.on('-');
    static final Splitter SPACE_SPLITTER = Splitter.on(' ').trimResults().omitEmptyStrings();

    public static final Set<String> UNTRANSLATED_UNIT_NAMES =
            ImmutableSet.of("portion", "ofglucose", "100-kilometer", "ofhg");

    public static final Set<String> HACK_SKIP_UNIT_NAMES =
            ImmutableSet.of(
                    // skip dot because pixel is preferred
                    "dot-per-centimeter",
                    "dot-per-inch",
                    // skip because a component is not translated
                    "liter-per-100-kilometer",
                    "millimeter-ofhg",
                    "inch-ofhg");

    final RationalParser rationalParser;
    final Function<String, UnitIdComponentType> componentTypeData;

    private Map<String, String> baseUnitToQuantity = new LinkedHashMap<>();
    private Map<String, String> baseUnitToStatus = new LinkedHashMap<>();
    private Map<String, TargetInfo> sourceToTargetInfo = new LinkedHashMap<>();
    private Map<String, String> sourceToStandard;
    private Multimap<String, String> quantityToSimpleUnits = LinkedHashMultimap.create();
    private Multimap<String, UnitSystem> sourceToSystems = TreeMultimap.create();
    private Set<String> baseUnits;
    private MapComparator<String> quantityComparator;

    private Map<String, String> fixDenormalized;
    private ImmutableMap<String, UnitId> idToUnitId;

    public final BiMap<String, String> SHORT_TO_LONG_ID = Units.LONG_TO_SHORT.inverse();
    public final Set<String> LONG_PREFIXES = Units.TYPE_TO_CORE.keySet();

    private boolean frozen = false;

    public TargetInfoComparator targetInfoComparator;

    private final MapComparator<String> LongUnitIdOrder = new MapComparator<>();
    private final MapComparator<String> ShortUnitIdOrder = new MapComparator<>();

    public Comparator<String> getLongUnitIdComparator() {
        return LongUnitIdOrder;
    }

    public Comparator<String> getShortUnitIdComparator() {
        return ShortUnitIdOrder;
    }

    /** Warning: ordering is important; determines the normalized output */
    public static final Set<String> BASE_UNITS =
            ImmutableSet.of(
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
                    "portion",
                    "night");

    public void addQuantityInfo(String baseUnit, String quantity, String status) {
        if (baseUnitToQuantity.containsKey(baseUnit)) {
            throw new IllegalArgumentException(
                    "base unit "
                            + baseUnit
                            + " already defined for quantity "
                            + quantity
                            + " with status "
                            + status);
        }
        baseUnitToQuantity.put(baseUnit, quantity);
        if (status != null) {
            baseUnitToStatus.put(baseUnit, status);
        }
        quantityToSimpleUnits.put(quantity, baseUnit);
    }

    public static final Set<String> BASE_UNIT_PARTS =
            ImmutableSet.<String>builder()
                    .add("per")
                    .add("square")
                    .add("cubic")
                    .add("pow4")
                    .addAll(BASE_UNITS)
                    .build();

    public static final Pattern PLACEHOLDER =
            Pattern.compile(
                    "[ \\u00A0\\u200E\\u200F\\u202F]*\\{0\\}[ \\u00A0\\u200E\\u200F\\u202F]*");
    public static final boolean HACK = true;

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
            sourceToStandard = buildSourceToStandard();
            quantityToSimpleUnits = ImmutableMultimap.copyOf(quantityToSimpleUnits);
            quantityComparator = getQuantityComparator(baseUnitToQuantity, baseUnitToStatus);

            sourceToSystems = ImmutableMultimap.copyOf(sourceToSystems);
            // other fields are frozen earlier in processing
            Builder<String> builder = ImmutableSet.<String>builder().addAll(BASE_UNITS);
            for (TargetInfo s : sourceToTargetInfo.values()) {
                builder.add(s.target);
            }
            baseUnits = builder.build();
            targetInfoComparator = new TargetInfoComparator();

            buildMapComparators();

            // must be after building comparators
            idToUnitId = ImmutableMap.copyOf(buildIdToUnitId());
        }
        return this;
    }

    public void buildMapComparators() {
        Set<R4<Integer, UnitSystem, Rational, String>> all = new TreeSet<>();
        Set<String> baseSeen = new HashSet<>();
        if (DEBUG) {
            UnitParser up = new UnitParser(componentTypeData);
            Output<UnitIdComponentType> uict = new Output<>();

            for (String longUnit :
                    Validity.getInstance().getStatusToCodes(LstrType.unit).get(Status.regular)) {
                String shortUnit = getShortId(longUnit);
                up.set(shortUnit);
                List<String> items = new ArrayList<>();
                String msg = "\t";
                try {
                    while (true) {
                        String item = up.nextParse(uict);
                        if (item == null) break;
                        items.add(item);
                        // items.add(uict.value.toString());
                    }
                } catch (Exception e) {
                    msg = e.getMessage() + "\t";
                }
                System.out.println(shortUnit + "\t" + Joiner.on('\t').join(items));
            }
        }
        for (String longUnit :
                Validity.getInstance().getStatusToCodes(LstrType.unit).get(Status.regular)) {
            Output<String> base = new Output<>();
            String shortUnit = getShortId(longUnit);
            ConversionInfo conversionInfo = parseUnitId(shortUnit, base, false);
            if (base.value == null) {
                int debug = 0;
            }
            if (conversionInfo == null) {
                if (longUnit.equals("temperature-generic")) {
                    conversionInfo = parseUnitId("kelvin", base, false);
                }
            }
            String quantity;
            Integer quantityNumericOrder = null;
            try {
                quantity = getQuantityFromUnit(base.value, false);
                quantityNumericOrder = quantityComparator.getNumericOrder(quantity);
            } catch (Exception e) {
                System.out.println(
                        "Failed "
                                + shortUnit
                                + ", "
                                + base
                                + ", "
                                + quantityNumericOrder
                                + ", "
                                + e);
                continue;
            }
            if (quantityNumericOrder == null) { // try the inverse
                if (base.value.equals("meter-per-cubic-meter")) { // HACK
                    quantityNumericOrder = quantityComparator.getNumericOrder("consumption");
                }
                if (quantityNumericOrder == null) {
                    throw new IllegalArgumentException(
                            "Missing quantity for: " + base.value + ", " + shortUnit);
                }
            }

            final EnumSet<UnitSystem> systems = EnumSet.copyOf(getSystemsEnum(shortUnit));

            // to sort the right items together items together, put together a sort key
            UnitSystem sortingSystem = systems.iterator().next();
            switch (sortingSystem) {
                case metric:
                case si:
                case si_acceptable:
                case astronomical:
                case metric_adjacent:
                case person_age:
                    sortingSystem = UnitSystem.metric;
                    break;
                    // country specific
                case other:
                case ussystem:
                case uksystem:
                case jpsystem:
                    sortingSystem = UnitSystem.other;
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Add new unitSystem to a grouping: " + sortingSystem);
            }
            R4<Integer, UnitSystem, Rational, String> sortKey =
                    Row.of(quantityNumericOrder, sortingSystem, conversionInfo.factor, shortUnit);
            all.add(sortKey);
        }
        LongUnitIdOrder.setErrorOnMissing(true);
        ShortUnitIdOrder.setErrorOnMissing(true);
        for (R4<Integer, UnitSystem, Rational, String> item : all) {
            String shortId = item.get3();
            ShortUnitIdOrder.add(shortId);
            LongUnitIdOrder.add(getLongId(shortId));
        }
        LongUnitIdOrder.freeze();
        ShortUnitIdOrder.freeze();
    }

    public Map<String, UnitId> buildIdToUnitId() {
        Map<String, UnitId> _idToUnitId = new TreeMap<>();
        for (Entry<String, String> shortAndLongId : SHORT_TO_LONG_ID.entrySet()) {
            String shortId = shortAndLongId.getKey();
            String longId = shortAndLongId.getKey();
            UnitId uid;
            try {
                uid = createUnitId(shortId).freeze();
            } catch (Exception e) {
                System.out.println("Failed with " + shortId);
                continue;
            }
            boolean doTest = false;
            Output<Rational> deprefix = new Output<>();
            for (Entry<String, Integer> entry : uid.numUnitsToPowers.entrySet()) {
                final String unitPart = entry.getKey();
                UnitConverter.stripPrefix(unitPart, deprefix);
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
        return ImmutableMap.copyOf(_idToUnitId);
    }

    /**
     * Return the 'standard unit' for the source.
     *
     * @return
     */
    private Map<String, String> buildSourceToStandard() {
        Map<String, String> unitToStandard = new TreeMap<>();
        for (Entry<String, TargetInfo> entry : sourceToTargetInfo.entrySet()) {
            String source = entry.getKey();
            TargetInfo targetInfo = entry.getValue();
            if (targetInfo.unitInfo.factor.equals(Rational.ONE)
                    && targetInfo.unitInfo.offset.equals(Rational.ZERO)) {
                final String target = targetInfo.target;
                String old = unitToStandard.get(target);
                if (old == null) {
                    unitToStandard.put(target, source);
                    if (DEBUG) System.out.println(target + " ⟹ " + source);
                } else if (old.length() > source.length()) {
                    unitToStandard.put(target, source);
                    if (DEBUG)
                        System.out.println(
                                "TWO STANDARDS: " + target + " ⟹ " + source + "; was " + old);
                } else {
                    if (DEBUG)
                        System.out.println(
                                "TWO STANDARDS: " + target + " ⟹ " + old + ", was " + source);
                }
            }
        }
        return ImmutableMap.copyOf(unitToStandard);
    }

    @Override
    public UnitConverter cloneAsThawed() {
        throw new UnsupportedOperationException();
    }

    public static final class ConversionInfo implements Comparable<ConversionInfo> {
        public final Rational factor;
        public final Rational offset;
        public String special;
        public boolean specialInverse; // only used with special

        static final ConversionInfo IDENTITY = new ConversionInfo(Rational.ONE, Rational.ZERO);

        public ConversionInfo(Rational factor, Rational offset) {
            this.factor = factor;
            this.offset = offset;
            this.special = null;
            this.specialInverse = false;
        }

        public ConversionInfo(String special, boolean inverse) {
            this.factor = Rational.ZERO; // if ONE it will be treated as a base unit
            this.offset = Rational.ZERO;
            this.special = special;
            this.specialInverse = inverse;
        }

        public Rational convert(Rational source) {
            if (special != null) {
                if (special.equals("beaufort")) {
                    return (specialInverse)
                            ? baseToScale(source, minMetersPerSecForBeaufort)
                            : scaleToBase(source, minMetersPerSecForBeaufort);
                }
                return source;
            }
            return source.multiply(factor).add(offset);
        }

        public Rational convertBackwards(Rational source) {
            if (special != null) {
                if (special.equals("beaufort")) {
                    return (specialInverse)
                            ? scaleToBase(source, minMetersPerSecForBeaufort)
                            : baseToScale(source, minMetersPerSecForBeaufort);
                }
                return source;
            }
            return source.subtract(offset).divide(factor);
        }

        private static final Rational[] minMetersPerSecForBeaufort = {
            // minimum m/s values for each Bft value, plus an extra artificial value
            // from table in Wikipedia, except for artificial value
            // since 0 based, max Beaufort value is thus array dimension minus 2
            Rational.of("0.0"), // 0 Bft
            Rational.of("0.3"), // 1
            Rational.of("1.6"), // 2
            Rational.of("3.4"), // 3
            Rational.of("5.5"), // 4
            Rational.of("8.0"), // 5
            Rational.of("10.8"), // 6
            Rational.of("13.9"), // 7
            Rational.of("17.2"), // 8
            Rational.of("20.8"), // 9
            Rational.of("24.5"), // 10
            Rational.of("28.5"), // 11
            Rational.of("32.7"), // 12
            Rational.of("36.9"), // 13
            Rational.of("41.4"), // 14
            Rational.of("46.1"), // 15
            Rational.of("51.1"), // 16
            Rational.of("55.8"), // 17
            Rational.of("61.4"), // artificial end of range 17 to give reasonable midpoint
        };

        private Rational scaleToBase(Rational scaleValue, Rational[] minBaseForScaleValues) {
            BigInteger scaleRound = scaleValue.abs().add(Rational.of(1, 2)).floor();
            BigInteger scaleMax = BigInteger.valueOf(minBaseForScaleValues.length - 2);
            if (scaleRound.compareTo(scaleMax) > 0) {
                scaleRound = scaleMax;
            }
            int scaleIndex = scaleRound.intValue();
            // Return midpont of range (the final range uses an articial end to produce reasonable
            // midpoint)
            return minBaseForScaleValues[scaleIndex]
                    .add(minBaseForScaleValues[scaleIndex + 1])
                    .divide(Rational.TWO);
        }

        private Rational baseToScale(Rational baseValue, Rational[] minBaseForScaleValues) {
            int scaleIndex = Arrays.binarySearch(minBaseForScaleValues, baseValue.abs());
            if (scaleIndex < 0) {
                // since out first array entry is 0, this value will always be -2 or less
                scaleIndex = -scaleIndex - 2;
            }
            int scaleMax = minBaseForScaleValues.length - 2;
            if (scaleIndex > scaleMax) {
                scaleIndex = scaleMax;
            }
            return Rational.of(scaleIndex);
        }

        public ConversionInfo invert() {
            if (special != null) {
                return new ConversionInfo(special, !specialInverse);
            }
            Rational factor2 = factor.reciprocal();
            Rational offset2 =
                    offset.equals(Rational.ZERO) ? Rational.ZERO : offset.divide(factor).negate();
            return new ConversionInfo(factor2, offset2);
            // TODO fix reciprocal
        }

        @Override
        public String toString() {
            return toString("x");
        }

        public String toString(String unit) {
            if (special != null) {
                return "special" + (specialInverse ? "inv" : "") + ":" + special + "(" + unit + ")";
            }
            return factor.toString(FormatStyle.formatted)
                    + " * "
                    + unit
                    + (offset.equals(Rational.ZERO)
                            ? ""
                            : (offset.compareTo(Rational.ZERO) < 0 ? " - " : " + ")
                                    + offset.abs().toString(FormatStyle.formatted));
        }

        public String toDecimal() {
            return toDecimal("x");
        }

        public String toDecimal(String unit) {
            if (special != null) {
                return "special" + (specialInverse ? "inv" : "") + ":" + special + "(" + unit + ")";
            }
            return factor.toBigDecimal(MathContext.DECIMAL64)
                    + " * "
                    + unit
                    + (offset.equals(Rational.ZERO)
                            ? ""
                            : (offset.compareTo(Rational.ZERO) < 0 ? " - " : " + ")
                                    + offset.toBigDecimal(MathContext.DECIMAL64).abs());
        }

        @Override
        public int compareTo(ConversionInfo o) {
            // All specials sort at the end
            int diff;
            if (special != null) {
                if (o.special == null) {
                    return 1; // This is special, other is not
                }
                // Both are special check names
                if (0 != (diff = special.compareTo(o.special))) {
                    return diff;
                }
                // Among specials with the same name, inverses sort later
                if (specialInverse != o.specialInverse) {
                    return (specialInverse) ? 1 : -1;
                }
                return 0;
            }
            if (o.special != null) {
                return -1; // This is not special, other is
            }
            // Neither this nor other is special
            if (0 != (diff = factor.compareTo(o.factor))) {
                return diff;
            }
            return offset.compareTo(o.offset);
        }

        @Override
        public boolean equals(Object obj) {
            return 0 == compareTo((ConversionInfo) obj);
        }

        @Override
        public int hashCode() {
            return Objects.hash(factor, offset, (special == null) ? "" : special);
        }
    }

    public static class Continuation implements Comparable<Continuation> {
        public final List<String> remainder;
        public final String result;

        public static void addIfNeeded(String source, Multimap<String, Continuation> data) {
            List<String> sourceParts = BAR_SPLITTER.splitToList(source);
            if (sourceParts.size() > 1) {
                Continuation continuation =
                        new Continuation(
                                ImmutableList.copyOf(sourceParts.subList(1, sourceParts.size())),
                                source);
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
    }

    public UnitConverter(
            RationalParser rationalParser,
            Validity validity,
            Function<String, UnitIdComponentType> componentTypeData) {
        this.rationalParser = rationalParser;
        this.componentTypeData = componentTypeData;

        //        // we need to pass in the validity so it is for the same CLDR version as the
        // converter
        //        Set<String> VALID_UNITS =
        // validity.getStatusToCodes(LstrType.unit).get(Status.regular);
        //        Map<String,String> _SHORT_TO_LONG_ID = new LinkedHashMap<>();
        //        for (String longUnit : VALID_UNITS) {
        //            int dashPos = longUnit.indexOf('-');
        //            String coreUnit = longUnit.substring(dashPos+1);
        //            _SHORT_TO_LONG_ID.put(coreUnit, longUnit);
        //        }
        //        SHORT_TO_LONG_ID = ImmutableBiMap.copyOf(_SHORT_TO_LONG_ID);
    }

    public void addRaw(
            String source,
            String target,
            String factor,
            String offset,
            String special,
            String systems) {
        ConversionInfo info;
        if (special != null) {
            info = new ConversionInfo(special, false);
            if (factor != null || offset != null) {
                throw new IllegalArgumentException(
                        "Cannot have factor or offset with special=" + special);
            }
        } else {
            info =
                    new ConversionInfo(
                            factor == null ? Rational.ONE : rationalParser.parse(factor),
                            offset == null ? Rational.ZERO : rationalParser.parse(offset));
        }
        Map<String, String> args = new LinkedHashMap<>();
        if (factor != null) {
            args.put("factor", factor);
        }
        if (offset != null) {
            args.put("offset", offset);
        }
        if (special != null) {
            args.put("special", special);
        }

        addToSourceToTarget(source, target, info, args, systems);
    }

    public static class TargetInfo {
        public final String target;
        public final ConversionInfo unitInfo;
        public final Map<String, String> inputParameters;

        public TargetInfo(
                String target, ConversionInfo unitInfo, Map<String, String> inputParameters) {
            this.target = target;
            this.unitInfo = unitInfo;
            this.inputParameters = ImmutableMap.copyOf(inputParameters);
        }

        @Override
        public String toString() {
            return unitInfo + " (" + target + ")";
        }

        public String formatOriginalSource(String source) {
            StringBuilder result =
                    new StringBuilder()
                            .append("<convertUnit source='")
                            .append(source)
                            .append("' baseUnit='")
                            .append(target)
                            .append("'");
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

    private void addToSourceToTarget(
            String source,
            String target,
            ConversionInfo info,
            Map<String, String> inputParameters,
            String systems) {
        if (sourceToTargetInfo.isEmpty()) {
            baseUnitToQuantity = ImmutableBiMap.copyOf(baseUnitToQuantity);
            baseUnitToStatus = ImmutableMap.copyOf(baseUnitToStatus);
        } else if (sourceToTargetInfo.containsKey(source)) {
            throw new IllegalArgumentException("Duplicate source: " + source + ", " + target);
        }
        sourceToTargetInfo.put(source, new TargetInfo(target, info, inputParameters));
        String targetQuantity = baseUnitToQuantity.get(target);
        if (targetQuantity == null) {
            throw new IllegalArgumentException("No quantity for baseUnit: " + target);
        }
        quantityToSimpleUnits.put(targetQuantity, source);
        if (systems != null) {
            SPACE_SPLITTER
                    .splitToList(systems)
                    .forEach(x -> sourceToSystems.put(source, UnitSystem.valueOf(x)));
        }
    }

    private MapComparator<String> getQuantityComparator(
            Map<String, String> baseUnitToQuantity2, Map<String, String> baseUnitToStatus2) {
        // We want to sort all the quantities so that we have a natural ordering within compound
        // units. So kilowatt-hour, not hour-kilowatt.
        Collection<String> values;
        if (true) {
            values = baseUnitToQuantity2.values();
        } else {
            // For simple quantities, just use the ordering from baseUnitToStatus
            MapComparator<String> simpleBaseUnitComparator =
                    new MapComparator<>(baseUnitToStatus2.keySet()).freeze();
            // For non-symbol quantities, use the ordering of the UnitIds
            Map<UnitId, String> unitIdToQuantity = new TreeMap<>();
            for (Entry<String, String> buq : baseUnitToQuantity2.entrySet()) {
                UnitId uid =
                        new UnitId(simpleBaseUnitComparator).add(buq.getKey(), true, 1).freeze();
                unitIdToQuantity.put(uid, buq.getValue());
            }
            // System.out.println(Joiner.on("\n").join(unitIdToQuantity.values()));
            values = unitIdToQuantity.values();
        }
        if (DEBUG) System.out.println(values);
        return new MapComparator<>(values).freeze();
    }

    public Set<String> canConvertBetween(String unit) {
        TargetInfo targetInfo = sourceToTargetInfo.get(unit);
        if (targetInfo == null) {
            return Collections.emptySet();
        }
        String quantity = baseUnitToQuantity.get(targetInfo.target);
        return getSimpleUnits(quantity);
    }

    public Set<String> getSimpleUnits(String quantity) {
        return ImmutableSet.copyOf(quantityToSimpleUnits.get(quantity));
    }

    public Set<String> canConvert() {
        return sourceToTargetInfo.keySet();
    }

    /** Converts between units, but ONLY if they are both base units */
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
     * Return the standard unit, eg newton for kilogram-meter-per-square-second
     *
     * @param simpleUnit
     * @return
     */
    public String getStandardUnit(String unit) {
        Output<String> metricUnit = new Output<>();
        parseUnitId(unit, metricUnit, false);
        String result = sourceToStandard.get(metricUnit.value);
        if (result == null) {
            UnitId mUnit = createUnitId(metricUnit.value);
            mUnit = mUnit.resolve();
            result = sourceToStandard.get(mUnit.toString());
            if (result == null) {
                mUnit = mUnit.getReciprocal();
                result = sourceToStandard.get(mUnit.toString());
                if (result != null) {
                    result = "per-" + result;
                }
            }
        }
        return result == null ? metricUnit.value : result;
    }

    /**
     * Reduces a unit, eg square-meter-per-meter-second ==> meter-per-second
     *
     * @param unit
     * @return
     */
    public String getReducedUnit(String unit) {
        UnitId mUnit = createUnitId(unit);
        mUnit = mUnit.resolve();
        return mUnit.toString();
    }

    public String getSpecialBaseUnit(String quantity, Set<UnitSystem> unitSystem) {
        if (unitSystem.contains(UnitSystem.ussystem) || unitSystem.contains(UnitSystem.uksystem)) {
            switch (quantity) {
                case "volume":
                    return unitSystem.contains(UnitSystem.uksystem) ? "gallon-imperial" : "gallon";
                case "mass":
                    return "pound";
                case "length":
                    return "foot";
                case "area":
                    return "square-foot";
            }
        }
        return null;
    }

    // unit constants are positive integers, optionally using positive exponents
    static final Pattern CONSTANT = Pattern.compile("[0-9]+([eE][0-9]+)?");

    /**
     * Takes a derived unit id, and produces the equivalent derived base unit id and UnitInfo to
     * convert to it
     *
     * @author markdavis
     * @param showYourWork TODO
     */
    public ConversionInfo parseUnitId(
            String derivedUnit, Output<String> metricUnit, boolean showYourWork) {
        // First check whether we are dealing with a special mapping
        Output<String> testBaseUnit = new Output<>();
        ConversionInfo testInfo = getUnitInfo(derivedUnit, testBaseUnit);
        if (testInfo != null && testInfo.special != null) {
            metricUnit.value = testBaseUnit.value;
            return new ConversionInfo(testInfo.special, testInfo.specialInverse);
        }
        // Not a special mapping, proceed as usual
        metricUnit.value = null;

        UnitId outputUnit = new UnitId(UNIT_COMPARATOR);
        Rational numerator = Rational.ONE;
        Rational denominator = Rational.ONE;
        boolean inNumerator = true;
        int power = 1;

        Output<Rational> deprefix = new Output<>();
        Rational offset = Rational.ZERO;
        int countUnits = 0;
        // We need to pass in componentTypeData because we may be called while reading
        // the data for SupplementalDataInfo;
        UnitParser up = new UnitParser(componentTypeData).set(derivedUnit);
        Matcher constantMatcher = CONSTANT.matcher("");

        for (Iterator<String> upi = With.toIterator(up); upi.hasNext(); ) {
            String unit = upi.next();
            ++countUnits;
            if (constantMatcher.reset(unit).matches()) {
                Rational constant =
                        Rational.of(new BigDecimal(unit)); // guaranteed to have denominator = ONE
                if (inNumerator) {
                    numerator = numerator.multiply(constant);
                } else {
                    denominator = denominator.multiply(constant);
                }
            } else if (unit.equals("square")) {
                if (power != 1) {
                    throw new IllegalArgumentException("Can't have power of " + unit);
                }
                power = 2;
                if (showYourWork)
                    System.out.println(
                            showRational("\t " + unit + ": ", Rational.of(power), "power"));
            } else if (unit.equals("cubic")) {
                if (power != 1) {
                    throw new IllegalArgumentException("Can't have power of " + unit);
                }
                power = 3;
                if (showYourWork)
                    System.out.println(
                            showRational("\t " + unit + ": ", Rational.of(power), "power"));
            } else if (unit.startsWith("pow")) {
                if (power != 1) {
                    throw new IllegalArgumentException("Can't have power of " + unit);
                }
                power = Integer.parseInt(unit.substring(3));
                if (showYourWork)
                    System.out.println(
                            showRational("\t " + unit + ": ", Rational.of(power), "power"));
            } else if (unit.equals("per")) {
                if (power != 1) {
                    throw new IllegalArgumentException("Can't have power of per");
                }
                if (showYourWork && inNumerator) System.out.println("\tper");
                inNumerator = false; // ignore multiples
                //            } else if ('9' >= unit.charAt(0)) {
                //                if (power != 1) {
                //                    throw new IllegalArgumentException("Can't have power of " +
                // unit);
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

                    value =
                            (info.unitInfo.special == null)
                                    ? info.unitInfo.factor.multiply(value)
                                    : info.unitInfo.convert(value);
                    // if (showYourWork && !info.unitInfo.factor.equals(Rational.ONE))
                    // System.out.println(showRational("\tfactor: ", info.unitInfo.factor,
                    // baseUnit));
                    // Special handling for offsets. We disregard them if there are any other units.
                    if (countUnits == 1 && !upi.hasNext()) {
                        offset = info.unitInfo.offset;
                        if (showYourWork && !info.unitInfo.offset.equals(Rational.ZERO))
                            System.out.println(
                                    showRational("\toffset: ", info.unitInfo.offset, baseUnit));
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
                    if (showYourWork)
                        System.out.println(
                                showRational("\t× ", value, " ⟹ " + unit)
                                        + "\t"
                                        + numerator.divide(denominator)
                                        + "\t"
                                        + numerator.divide(denominator).doubleValue());
                }
                // create cleaned up target unitid
                outputUnit.add(unit, inNumerator, power);
                power = 1;
            }
        }
        metricUnit.value = outputUnit.toString();
        return new ConversionInfo(numerator.divide(denominator), offset);
    }

    /** Only for use for simple base unit comparison */
    // Thus we do not need to handle specials here
    private class UnitComparator implements Comparator<String> {
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
    }

    Comparator<String> UNIT_COMPARATOR = new UnitComparator();
    static final Pattern TRAILING_ZEROS = Pattern.compile("0+$");

    /** Only handles the canonical units; no kilo-, only normalized, etc. */
    // Thus we do not need to handle specials here
    // TODO: optimize
    // • the comparators don't have to be fields in this class;
    //   it is not a static class, so they can be on the converter.
    // • We can cache the frozen UnitIds, avoiding the parse times

    public class UnitId implements Freezable<UnitId>, Comparable<UnitId> {
        public Map<String, Integer> numUnitsToPowers;
        public Map<String, Integer> denUnitsToPowers;
        public EntrySetComparator<String, Integer> entrySetComparator;
        public Comparator<String> comparator;
        public Rational factor = Rational.ONE;

        private boolean frozen = false;

        private UnitId(Comparator<String> comparator) {
            this.comparator = comparator;
            numUnitsToPowers = new TreeMap<>(comparator);
            denUnitsToPowers = new TreeMap<>(comparator);
            entrySetComparator =
                    new EntrySetComparator<String, Integer>(comparator, Comparator.naturalOrder());
        } //

        public UnitId getReciprocal() {
            UnitId result = new UnitId(comparator);
            result.entrySetComparator = entrySetComparator;
            result.numUnitsToPowers = denUnitsToPowers;
            result.denUnitsToPowers = numUnitsToPowers;
            result.factor = factor.reciprocal();
            return result;
        }

        private UnitId add(String compoundUnit, boolean groupInNumerator, int groupPower) {
            if (frozen) {
                throw new UnsupportedOperationException("Object is frozen.");
            }
            boolean inNumerator = true;
            int power = 1;
            // We need to pass in componentTypeData because we may be called while reading
            // the data for SupplementalDataInfo;
            UnitParser up = new UnitParser(componentTypeData).set(compoundUnit);
            Matcher constantMatcher = CONSTANT.matcher("");

            for (String unitPart : With.toIterable(up)) {
                switch (unitPart) {
                    case "square":
                        power = 2;
                        break;
                    case "cubic":
                        power = 3;
                        break;
                    case "per":
                        inNumerator = false;
                        break; // sticky, ignore multiples
                    default:
                        if (constantMatcher.reset(unitPart).matches()) {
                            Rational constant =
                                    Rational.of(
                                            new BigDecimal(
                                                    unitPart)); // guaranteed to have denominator =
                            // ONE
                            if (inNumerator) {
                                factor = factor.multiply(constant);
                            } else {
                                factor = factor.divide(constant);
                            }
                        } else if (unitPart.startsWith("pow")) {
                            power = Integer.parseInt(unitPart.substring(3));
                        } else {
                            Map<String, Integer> target =
                                    inNumerator == groupInNumerator
                                            ? numUnitsToPowers
                                            : denUnitsToPowers;
                            Integer oldPower;
                            try {
                                oldPower = target.get(unitPart);
                            } catch (Exception e) {
                                throw new IllegalArgumentException(
                                        "Can't parse unitPart " + unitPart + " in " + compoundUnit,
                                        e);
                            }
                            // we multiply powers, so that weight-square-volume =>
                            // weight-pow4-length
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
                if (positivePass && !factor.numerator.equals(BigInteger.ONE)) {
                    builder.append(shortConstant(factor.numerator));
                }

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
                            if (!factor.denominator.equals(BigInteger.ONE)) {
                                builder.append(shortConstant(factor.denominator)).append('-');
                            }
                        }
                    }
                    switch (power) {
                        case 1:
                            break;
                        case 2:
                            builder.append("square-");
                            break;
                        case 3:
                            builder.append("cubic-");
                            break;
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
                if (!positivePass
                        && firstDenominator
                        && !factor.denominator.equals(BigInteger.ONE)) {
                    builder.append("-per-").append(shortConstant(factor.denominator));
                }
            }
            return builder.toString();
        }

        /**
         * Return a string format. If larger than 7 digits, use 1eN format.
         *
         * @param source
         * @return
         */
        public String shortConstant(BigInteger source) {
            // don't bother optimizing
            String result = source.toString();
            if (result.length() < 8) {
                return result;
            }
            Matcher matcher = TRAILING_ZEROS.matcher(result);
            if (matcher.find()) {
                int zeroCount = matcher.group().length();
                return result.substring(0, result.length() - zeroCount) + "e" + zeroCount;
            }
            return result;
        }

        public String toString(
                LocaleStringProvider resolvedFile,
                String width,
                String _pluralCategory,
                String caseVariant,
                Multimap<UnitPathType, String> partsUsed,
                boolean maximal) {
            if (partsUsed != null) {
                partsUsed.clear();
            }
            // TODO handle factor!!
            String result = null;
            String numerator = null;
            String timesPattern = null;
            String placeholderPattern = null;
            Output<Integer> deprefix = new Output<>();

            PlaceholderLocation placeholderPosition = PlaceholderLocation.missing;
            Matcher placeholderMatcher = PLACEHOLDER.matcher("");
            Output<String> unitPatternOut = new Output<>();

            PluralInfo pluralInfo =
                    CLDRConfig.getInstance()
                            .getSupplementalDataInfo()
                            .getPlurals(resolvedFile.getLocaleID());
            PluralRules pluralRules = pluralInfo.getPluralRules();
            String singularPluralCategory = pluralRules.select(1d);
            final ULocale locale = new ULocale(resolvedFile.getLocaleID());
            String fullPerPattern = null;
            int negCount = 0;

            for (int i = 1; i >= 0; --i) { // two passes, numerator then den.
                boolean positivePass = i > 0;
                if (!positivePass) {
                    switch (locale.toString()) {
                        case "de":
                            caseVariant = "accusative";
                            break; // German pro rule
                    }
                    numerator = result; // from now on, result ::= denominator
                    result = null;
                }

                Map<String, Integer> target = positivePass ? numUnitsToPowers : denUnitsToPowers;
                int unitsLeft = target.size();
                for (Entry<String, Integer> entry : target.entrySet()) {
                    String possiblyPrefixedUnit = entry.getKey();
                    String unit = stripPrefixPower(possiblyPrefixedUnit, deprefix);
                    String genderVariant =
                            UnitPathType.gender.getTrans(
                                    resolvedFile, "long", unit, null, null, null, partsUsed);

                    int power = entry.getValue();
                    unitsLeft--;
                    String pluralCategory =
                            unitsLeft == 0 && positivePass
                                    ? _pluralCategory
                                    : singularPluralCategory;

                    if (!positivePass) {
                        if (maximal && 0 == negCount++) { // special case exact match for per form,
                            // and no previous result
                            if (true) {
                                throw new UnsupportedOperationException(
                                        "not yet implemented fully");
                            }
                            String fullUnit;
                            switch (power) {
                                case 1:
                                    fullUnit = unit;
                                    break;
                                case 2:
                                    fullUnit = "square-" + unit;
                                    break;
                                case 3:
                                    fullUnit = "cubic-" + unit;
                                    break;
                                default:
                                    throw new IllegalArgumentException("powers > 3 not supported");
                            }
                            fullPerPattern =
                                    UnitPathType.perUnit.getTrans(
                                            resolvedFile,
                                            width,
                                            fullUnit,
                                            _pluralCategory,
                                            caseVariant,
                                            genderVariant,
                                            partsUsed);
                            // if there is a special form, we'll use it
                            if (fullPerPattern != null) {
                                continue;
                            }
                        }
                    }

                    // handle prefix, like kilo-
                    String prefixPattern = null;
                    if (deprefix.value != 1) {
                        prefixPattern =
                                UnitPathType.prefix.getTrans(
                                        resolvedFile,
                                        width,
                                        "10p" + deprefix.value,
                                        _pluralCategory,
                                        caseVariant,
                                        genderVariant,
                                        partsUsed);
                    }

                    // get the core pattern. Detect and remove the the placeholder (and surrounding
                    // spaces)
                    String unitPattern =
                            UnitPathType.unit.getTrans(
                                    resolvedFile,
                                    width,
                                    unit,
                                    pluralCategory,
                                    caseVariant,
                                    genderVariant,
                                    partsUsed);
                    if (unitPattern == null) {
                        return null; // unavailable
                    }
                    // we are set up for 2 kinds of placeholder patterns for units. {0}\s?stuff or
                    // stuff\s?{0}, or nothing(Eg Arabic)
                    placeholderPosition =
                            extractUnit(placeholderMatcher, unitPattern, unitPatternOut);
                    if (placeholderPosition == PlaceholderLocation.middle) {
                        return null; // signal we can't handle, but shouldn't happen with
                        // well-formed data.
                    } else if (placeholderPosition != PlaceholderLocation.missing) {
                        unitPattern = unitPatternOut.value;
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
                            powerPattern =
                                    UnitPathType.power.getTrans(
                                            resolvedFile,
                                            width,
                                            "power2",
                                            pluralCategory,
                                            caseVariant,
                                            genderVariant,
                                            partsUsed);
                            break;
                        case 3:
                            powerPattern =
                                    UnitPathType.power.getTrans(
                                            resolvedFile,
                                            width,
                                            "power3",
                                            pluralCategory,
                                            caseVariant,
                                            genderVariant,
                                            partsUsed);
                            break;
                        default:
                            throw new IllegalArgumentException("No power pattern > 3: " + this);
                    }

                    if (powerPattern != null) {
                        unitPattern = combineLowercasing(locale, width, powerPattern, unitPattern);
                    }

                    if (result != null) {
                        if (timesPattern == null) {
                            timesPattern = getTimesPattern(resolvedFile, width);
                        }
                        result = MessageFormat.format(timesPattern, result, unitPattern);
                    } else {
                        result = unitPattern;
                    }
                }
            }

            // if there is a fullPerPattern, then we use it instead of per pattern + first
            // denominator element
            if (fullPerPattern != null) {
                if (numerator != null) {
                    numerator = MessageFormat.format(fullPerPattern, numerator);
                } else {
                    numerator = fullPerPattern;
                    placeholderPattern = null;
                }
                if (result != null) {
                    if (timesPattern == null) {
                        timesPattern = getTimesPattern(resolvedFile, width);
                    }
                    numerator = MessageFormat.format(timesPattern, numerator, result);
                }
                result = numerator;
            } else {
                // glue the two parts together, if we have two of them
                if (result == null) {
                    result = numerator;
                } else {
                    String perPattern =
                            UnitPathType.per.getTrans(
                                    resolvedFile,
                                    width,
                                    null,
                                    _pluralCategory,
                                    caseVariant,
                                    null,
                                    partsUsed);
                    if (numerator == null) {
                        result = MessageFormat.format(perPattern, "", result).trim();
                    } else {
                        result = MessageFormat.format(perPattern, numerator, result);
                    }
                }
            }
            return addPlaceholder(result, placeholderPattern, placeholderPosition);
        }

        public String getTimesPattern(
                LocaleStringProvider resolvedFile, String width) { // TODO fix hack!
            if (HACK && "en".equals(resolvedFile.getLocaleID())) {
                return "{0}-{1}";
            }
            String timesPatternPath =
                    "//ldml/units/unitLength[@type=\""
                            + width
                            + "\"]/compoundUnit[@type=\"times\"]/compoundUnitPattern";
            return resolvedFile.getStringValue(timesPatternPath);
        }

        @Override
        public boolean equals(Object obj) {
            UnitId other = (UnitId) obj;
            return factor.equals(other.factor) & numUnitsToPowers.equals(other.numUnitsToPowers)
                    && denUnitsToPowers.equals(other.denUnitsToPowers);
        }

        @Override
        public int hashCode() {
            return Objects.hash(factor, numUnitsToPowers, denUnitsToPowers);
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
            int diff =
                    compareEntrySets(
                            numUnitsToPowers.entrySet(),
                            o.numUnitsToPowers.entrySet(),
                            entrySetComparator);
            if (diff != 0) return diff;
            diff =
                    compareEntrySets(
                            denUnitsToPowers.entrySet(),
                            o.denUnitsToPowers.entrySet(),
                            entrySetComparator);
            if (diff != 0) return diff;
            return factor.compareTo(o.factor);
        }

        /**
         * Default rules Prefixes & powers: the gender of the whole is the same as the gender of the
         * operand. In pseudocode: gender(square, meter) = gender(meter) gender(kilo, meter) =
         * gender(meter)
         *
         * <p>Per: the gender of the whole is the gender of the numerator. If there is no numerator,
         * then the gender of the denominator gender(gram per meter) = gender(gram)
         *
         * <p>Times: the gender of the whole is the gender of the last operand gender(gram-meter) =
         * gender(gram)
         *
         * @param source
         * @param partsUsed
         * @return TODO: add parameter to short-circuit the lookup if the unit is not a compound.
         */
        public String getGender(
                CLDRFile resolvedFile,
                Output<String> source,
                Multimap<UnitPathType, String> partsUsed) {
            // will not be empty

            GrammarDerivation gd = null;
            // Values power = gd.get(GrammaticalFeature.grammaticalGender,
            // CompoundUnitStructure.power); no data available yet
            // Values prefix = gd.get(GrammaticalFeature.grammaticalGender,
            // CompoundUnitStructure.prefix);

            Map<String, Integer> determiner;
            if (numUnitsToPowers.isEmpty()) {
                determiner = denUnitsToPowers;
            } else if (denUnitsToPowers.isEmpty()) {
                determiner = numUnitsToPowers;
            } else {
                if (gd == null) {
                    gd =
                            SupplementalDataInfo.getInstance()
                                    .getGrammarDerivation(resolvedFile.getLocaleID());
                }
                Values per =
                        gd.get(GrammaticalFeature.grammaticalGender, CompoundUnitStructure.per);
                boolean useFirst = per.value0.equals("0");
                determiner =
                        useFirst
                                ? numUnitsToPowers // otherwise use numerator if possible
                                : denUnitsToPowers;
                // TODO add test that the value is 0 or 1, so that if it fails we know to upgrade
                // this code.
            }

            Entry<String, Integer> bestMeasure;
            if (determiner.size() == 1) {
                bestMeasure = determiner.entrySet().iterator().next();
            } else {
                if (gd == null) {
                    gd =
                            SupplementalDataInfo.getInstance()
                                    .getGrammarDerivation(resolvedFile.getLocaleID());
                }
                Values times =
                        gd.get(GrammaticalFeature.grammaticalGender, CompoundUnitStructure.times);
                boolean useFirst = times.value0.equals("0");
                if (useFirst) {
                    bestMeasure = determiner.entrySet().iterator().next();
                } else {
                    bestMeasure = null; // we know the determiner is not empty, but this makes the
                    // compiler
                    for (Entry<String, Integer> entry : determiner.entrySet()) {
                        bestMeasure = entry;
                    }
                }
            }
            String strippedUnit = stripPrefix(bestMeasure.getKey(), null);
            String gender =
                    UnitPathType.gender.getTrans(
                            resolvedFile, "long", strippedUnit, null, null, null, partsUsed);
            if (gender != null && source != null) {
                source.value = strippedUnit;
            }
            return gender;
        }

        public UnitId times(UnitId id2) {
            UnitId result = new UnitId(comparator);
            result.factor = factor.multiply(id2.factor);
            combine(numUnitsToPowers, id2.numUnitsToPowers, result.numUnitsToPowers);
            combine(denUnitsToPowers, id2.denUnitsToPowers, result.denUnitsToPowers);
            return result;
        }

        public void combine(
                Map<String, Integer> map1,
                Map<String, Integer> map2,
                Map<String, Integer> resultMap) {
            Set<String> units = Sets.union(map1.keySet(), map2.keySet());
            for (String unit : units) {
                Integer int1 = map1.get(unit);
                Integer int2 = map2.get(unit);
                resultMap.put(unit, (int1 == null ? 0 : int1) + (int2 == null ? 0 : int2));
            }
        }
    }

    public enum PlaceholderLocation {
        before,
        middle,
        after,
        missing
    }

    public static String addPlaceholder(
            String result, String placeholderPattern, PlaceholderLocation placeholderPosition) {
        return placeholderPattern == null
                ? result
                : placeholderPosition == PlaceholderLocation.before
                        ? placeholderPattern + result
                        : result + placeholderPattern;
    }

    /**
     * Returns the location of the placeholder. Call placeholderMatcher.group() after calling this
     * to get the placeholder.
     *
     * @param placeholderMatcher
     * @param unitPattern
     * @param unitPatternOut
     * @param before
     * @return
     */
    public static PlaceholderLocation extractUnit(
            Matcher placeholderMatcher, String unitPattern, Output<String> unitPatternOut) {
        if (placeholderMatcher.reset(unitPattern).find()) {
            if (placeholderMatcher.start() == 0) {
                unitPatternOut.value = unitPattern.substring(placeholderMatcher.end());
                return PlaceholderLocation.before;
            } else if (placeholderMatcher.end() == unitPattern.length()) {
                unitPatternOut.value = unitPattern.substring(0, placeholderMatcher.start());
                return PlaceholderLocation.after;
            } else {
                unitPatternOut.value = unitPattern;
                return PlaceholderLocation.middle;
            }
        } else {
            unitPatternOut.value = unitPattern;
            return PlaceholderLocation.missing;
        }
    }

    public static String combineLowercasing(
            final ULocale locale, String width, String prefixPattern, String unitPattern) {
        // catch special case, ZentiLiter
        if (width.equals("long")
                && !prefixPattern.contains(" {")
                && !prefixPattern.contains(" {")) {
            unitPattern = UCharacter.toLowerCase(locale, unitPattern);
        }
        unitPattern = MessageFormat.format(prefixPattern, unitPattern);
        return unitPattern;
    }

    public static class EntrySetComparator<K extends Comparable<K>, V>
            implements Comparator<Entry<K, V>> {
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

    public static <K extends Comparable<K>, V extends Comparable<V>, T extends Entry<K, V>>
            int compareEntrySets(Collection<T> o1, Collection<T> o2, Comparator<T> comparator) {
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
            result = new UnitId(UNIT_COMPARATOR).add(unit, true, 1).freeze();
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
    // https://www.nist.gov/pml/owm/metric-si-prefixes
    public static final ImmutableMap<String, Integer> PREFIX_POWERS =
            ImmutableMap.<String, Integer>builder()
                    .put("quecto", -30)
                    .put("ronto", -27)
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
                    .put("ronna", 27)
                    .put("quetta", 30)
                    .build();

    public static final ImmutableMap<String, Rational> PREFIXES;

    static {
        Map<String, Rational> temp = new LinkedHashMap<>();
        for (Entry<String, Integer> entry : PREFIX_POWERS.entrySet()) {
            temp.put(entry.getKey(), Rational.pow10(entry.getValue()));
        }
        PREFIXES = ImmutableMap.copyOf(temp);
    }

    public static final Set<String> METRIC_TAKING_PREFIXES =
            ImmutableSet.of(
                    "bit", "byte", "liter", "tonne", "degree", "celsius", "kelvin", "calorie",
                    "bar");
    public static final Set<String> METRIC_TAKING_BINARY_PREFIXES = ImmutableSet.of("bit", "byte");

    static final Set<String> SKIP_PREFIX =
            ImmutableSet.of("millimeter-ofhg", "kilogram", "kilogram-force");

    static final Rational RATIONAL1000 = Rational.of(1000);

    /**
     * If there is no prefix, return the unit and ONE. If there is a prefix return the unit (with
     * prefix stripped) and the prefix factor
     */
    public static <V> String stripPrefixCommon(
            String unit, Output<V> deprefix, Map<String, V> unitMap) {
        if (SKIP_PREFIX.contains(unit)) {
            return unit;
        }

        for (Entry<String, V> entry : unitMap.entrySet()) {
            String prefix = entry.getKey();
            if (unit.startsWith(prefix)) {
                String result = unit.substring(prefix.length());
                // We have to do a special hack for kilogram, but only for the Rational case.
                // The Integer case is used for name construction, so that is ok.
                final boolean isRational = deprefix != null && deprefix.value instanceof Rational;
                boolean isGramHack = isRational && result.equals("gram");
                if (isGramHack) {
                    result = "kilogram";
                }
                if (deprefix != null) {
                    deprefix.value = entry.getValue();
                    if (isGramHack) {
                        final Rational ratValue = (Rational) deprefix.value;
                        deprefix.value = (V) ratValue.divide(RATIONAL1000);
                    }
                }
                return result;
            }
        }
        return unit;
    }

    public static String stripPrefix(String unit, Output<Rational> deprefix) {
        if (deprefix != null) {
            deprefix.value = Rational.ONE;
        }
        return stripPrefixCommon(unit, deprefix, PREFIXES);
    }

    public static String stripPrefixPower(String unit, Output<Integer> deprefix) {
        if (deprefix != null) {
            deprefix.value = 1;
        }
        return stripPrefixCommon(unit, deprefix, PREFIX_POWERS);
    }

    public BiMap<String, String> getBaseUnitToQuantity() {
        return (BiMap<String, String>) baseUnitToQuantity;
    }

    public String getQuantityFromUnit(String unit, boolean showYourWork) {
        Output<String> metricUnit = new Output<>();
        unit = fixDenormalized(unit);
        try {
            ConversionInfo unitInfo = parseUnitId(unit, metricUnit, showYourWork);
            return metricUnit.value == null ? null : getQuantityFromBaseUnit(metricUnit.value);
        } catch (Exception e) {
            System.out.println("Failed with " + unit + ", " + metricUnit + "\t" + e);
            return null;
        }
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

    public Multimap<String, UnitSystem> getSourceToSystems() {
        return sourceToSystems;
    }

    public enum UnitSystem { // TODO convert getSystems and SupplementalDataInfo to use natively
        si,
        si_acceptable,
        metric,
        metric_adjacent,
        ussystem,
        uksystem,
        jpsystem,
        astronomical,
        person_age,
        other,
        prefixable;

        public static final Set<UnitSystem> SiOrMetric =
                ImmutableSet.of(
                        UnitSystem.metric,
                        UnitSystem.si,
                        UnitSystem.metric_adjacent,
                        UnitSystem.si_acceptable);
        public static final Set<UnitSystem> ALL = ImmutableSet.copyOf(UnitSystem.values());

        public static Set<UnitSystem> fromStringCollection(Collection<String> stringUnitSystems) {
            return stringUnitSystems.stream()
                    .map(x -> UnitSystem.valueOf(x))
                    .collect(Collectors.toSet());
        }

        @Deprecated
        public static Set<String> toStringSet(Collection<UnitSystem> stringUnitSystems) {
            return new LinkedHashSet<>(
                    stringUnitSystems.stream().map(x -> x.toString()).collect(Collectors.toList()));
        }

        private static final Joiner SLASH_JOINER = Joiner.on("/");

        public static String getSystemsDisplay(Set<UnitSystem> systems) {
            List<String> result = new ArrayList<>();
            for (UnitSystem system : systems) {
                switch (system) {
                    case si_acceptable:
                    case metric:
                    case metric_adjacent:
                        return "";
                    case ussystem:
                        result.add("US");
                        break;
                    case uksystem:
                        result.add("UK");
                        break;
                    case jpsystem:
                        result.add("JP");
                        break;
                }
            }
            return result.isEmpty() ? "" : " (" + SLASH_JOINER.join(result) + ")";
        }
    }

    public Set<String> getSystems(String unit) {
        return UnitSystem.toStringSet(getSystemsEnum(unit));
    }

    public Set<UnitSystem> getSystemsEnum(String unit) {
        Set<UnitSystem> result = null;
        UnitId id = createUnitId(unit);

        // we walk through all the units in the numerator and denominator, and keep the
        // *intersection* of the units.
        // So {ussystem} and {ussystem, uksystem} => ussystem
        // Special case: {metric_adjacent} intersect {metric} => {metric_adjacent}.
        // We do that by adding metric_adjacent to any set with metric,
        // then removing metric_adjacent if there is a metric.
        // Same for si_acceptable.
        main:
        for (Map<String, Integer> unitsToPowers :
                Arrays.asList(id.denUnitsToPowers, id.numUnitsToPowers)) {
            for (String rawSubunit : unitsToPowers.keySet()) {
                String subunit = UnitConverter.stripPrefix(rawSubunit, null);

                Set<UnitSystem> systems = new TreeSet<>(sourceToSystems.get(subunit));
                if (systems.contains(UnitSystem.metric)) {
                    systems.add(UnitSystem.metric_adjacent);
                }
                if (systems.contains(UnitSystem.si)) {
                    systems.add(UnitSystem.si_acceptable);
                }

                if (result == null) {
                    result = systems; // first setting
                    if (!subunit.equals(rawSubunit)) {
                        result.remove(UnitSystem.prefixable);
                    }
                } else {
                    result.retainAll(systems);
                    result.remove(UnitSystem.prefixable); // remove if more than one
                }
                if (result.isEmpty()) {
                    break main;
                }
            }
        }
        if (result == null || result.isEmpty()) {
            return ImmutableSet.of(UnitSystem.other);
        }
        if (result.contains(UnitSystem.metric)) {
            result.remove(UnitSystem.metric_adjacent);
        }
        if (result.contains(UnitSystem.si)) {
            result.remove(UnitSystem.si_acceptable);
        }

        return ImmutableSet.copyOf(EnumSet.copyOf(result)); // the enum is to sort
    }

    //    private void addSystems(Set<String> result, String subunit) {
    //        Collection<String> systems = sourceToSystems.get(subunit);
    //        if (!systems.isEmpty()) {
    //            result.addAll(systems);
    //        }
    //    }

    public String reciprocalOf(String value) {
        // quick version, input guaranteed to be normalized, if original is
        if (value.startsWith("per-")) {
            return value.substring(4);
        }
        int index = value.indexOf("-per-");
        if (index < 0) {
            return "per-" + value;
        }
        return value.substring(index + 5) + "-per-" + value.substring(0, index);
    }

    public Rational parseRational(String source) {
        return rationalParser.parse(source);
    }

    public String showRational(String title, Rational rational, String unit) {
        String doubleString = showRational2(rational, " = ", " ≅ ");
        final String endResult = title + rational + doubleString + (unit != null ? " " + unit : "");
        return endResult;
    }

    public String showRational(Rational rational, String approximatePrefix) {
        String doubleString = showRational2(rational, "", approximatePrefix);
        return doubleString.isEmpty() ? rational.numerator.toString() : doubleString;
    }

    public String showRational2(Rational rational, String equalPrefix, String approximatePrefix) {
        String doubleString = "";
        if (!rational.denominator.equals(BigInteger.ONE)) {
            String doubleValue =
                    String.valueOf(rational.toBigDecimal(MathContext.DECIMAL32).doubleValue());
            Rational reverse = parseRational(doubleValue);
            doubleString =
                    (reverse.equals(rational) ? equalPrefix : approximatePrefix) + doubleValue;
        }
        return doubleString;
    }

    public Rational convert(
            final Rational sourceValue,
            final String sourceUnitIn,
            final String targetUnit,
            boolean showYourWork) {
        if (showYourWork) {
            System.out.println(
                    showRational("\nconvert:\t", sourceValue, sourceUnitIn) + " ⟹ " + targetUnit);
        }
        final String sourceUnit = fixDenormalized(sourceUnitIn);
        Output<String> sourceBase = new Output<>();
        Output<String> targetBase = new Output<>();
        ConversionInfo sourceConversionInfo = parseUnitId(sourceUnit, sourceBase, showYourWork);
        if (sourceConversionInfo == null) {
            if (showYourWork) System.out.println("! unknown unit: " + sourceUnit);
            return Rational.NaN;
        }
        Rational intermediateResult = sourceConversionInfo.convert(sourceValue);
        if (showYourWork)
            System.out.println(
                    showRational("intermediate:\t", intermediateResult, sourceBase.value));
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
                    if (showYourWork)
                        System.out.println(
                                "! incomparable units: " + sourceUnit + " and " + targetUnit);
                    return Rational.NaN;
                }
                intermediateResult = intermediateResult.reciprocal();
                if (showYourWork)
                    System.out.println(
                            showRational(
                                    " ⟹ 1/intermediate:\t", intermediateResult, reciprocalUnit));
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
            unitQuantity = unitQuantity.substring(0, unitQuantity.length() - 8);
        }
        String bu = ((BiMap<String, String>) baseUnitToQuantity).inverse().get(unitQuantity);
        if (bu == null) {
            return null;
        }
        return invert ? reciprocalOf(bu) : bu;
    }

    public Set<String> getQuantities() {
        return getBaseUnitToQuantity().inverse().keySet();
    }

    public enum UnitComplexity {
        simple,
        non_simple
    }

    private ConcurrentHashMap<String, UnitComplexity> COMPLEXITY = new ConcurrentHashMap<>();
    // TODO This is safe but should use regular cache

    public UnitComplexity getComplexity(String longOrShortId) {
        UnitComplexity result = COMPLEXITY.get(longOrShortId);
        if (result == null) {
            String shortId;
            String longId = getLongId(longOrShortId);
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
                    UnitConverter.stripPrefix(unitPart, deprefix);
                    if (!deprefix.value.equals(Rational.ONE)
                            || !entry.getValue().equals(INTEGER_ONE)) {
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

    public boolean isSimple(String x) {
        return getComplexity(x) == UnitComplexity.simple;
    }

    public String getLongId(String shortUnitId) {
        return CldrUtility.ifNull(SHORT_TO_LONG_ID.get(shortUnitId), shortUnitId);
    }

    public Set<String> getLongIds(Iterable<String> shortUnitIds) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String longUnitId : shortUnitIds) {
            String shortId = SHORT_TO_LONG_ID.get(longUnitId);
            if (shortId != null) {
                result.add(shortId);
            }
        }
        return ImmutableSet.copyOf(result);
    }

    public String getShortId(String longUnitId) {
        if (longUnitId == null) {
            return null;
        }
        String result = SHORT_TO_LONG_ID.inverse().get(longUnitId);
        if (result != null) {
            return result;
        }
        int dashPos = longUnitId.indexOf('-');
        if (dashPos < 0) {
            return longUnitId;
        }
        String type = longUnitId.substring(0, dashPos);
        return LONG_PREFIXES.contains(type) ? longUnitId.substring(dashPos + 1) : longUnitId;
    }

    public Set<String> getShortIds(Iterable<String> longUnitIds) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String longUnitId : longUnitIds) {
            String shortId = SHORT_TO_LONG_ID.inverse().get(longUnitId);
            if (shortId != null) {
                result.add(shortId);
            }
        }
        return ImmutableSet.copyOf(result);
    }

    public Map<String, String> getBaseUnitToStatus() {
        return baseUnitToStatus;
    }

    static final Rational LIMIT_UPPER_RELATED = Rational.of(10000);
    static final Rational LIMIT_LOWER_RELATED = LIMIT_UPPER_RELATED.reciprocal();

    public Map<Rational, String> getRelatedExamples(
            String inputUnit, Set<UnitSystem> allowedSystems) {
        Set<String> others = new LinkedHashSet<>(canConvertBetween(inputUnit));
        if (others.size() <= 1) {
            return Map.of();
        }
        // add common units
        if (others.contains("meter")) {
            others.add("kilometer");
            others.add("millimeter");
        } else if (others.contains("liter")) {
            others.add("milliliter");
        }
        // remove unusual units
        others.removeAll(
                Set.of(
                        "point",
                        "fathom",
                        "carat",
                        "grain",
                        "slug",
                        "drop",
                        "pinch",
                        "cup-metric",
                        "dram",
                        "jigger",
                        "pint-metric",
                        "bushel, barrel",
                        "dunam",
                        "rod",
                        "chain",
                        "furlong",
                        "fortnight",
                        "rankine",
                        "kelvin",
                        "calorie-it",
                        "british-thermal-unit-it",
                        "foodcalorie",
                        "nautical-mile",
                        "mile-scandinavian",
                        "knot",
                        "beaufort"));

        Map<Rational, String> result = new TreeMap<>(Comparator.reverseOrder());

        // get metric
        Output<String> sourceBase = new Output<>();
        ConversionInfo sourceConversionInfo = parseUnitId(inputUnit, sourceBase, false);
        String baseUnit = sourceBase.value;
        Rational baseUnitToInput = sourceConversionInfo.factor;

        putIfInRange(result, baseUnit, baseUnitToInput);

        // get similar IDs
        // TBD

        // get nearby in same system, and in metric

        for (UnitSystem system : allowedSystems) {
            if (system.equals(UnitSystem.si)) {
                continue;
            }
            String closestLess = null;
            Rational closestLessValue = Rational.NEGATIVE_INFINITY;
            String closestGreater = null;
            Rational closestGreaterValue = Rational.INFINITY;

            // check all the units in this system, to find the nearest above,and the nearest below

            for (String other : others) {
                if (other.equals(inputUnit)
                        || other.endsWith("-person")
                        || other.startsWith("100-")) { // skips
                    continue;
                }
                Set<UnitSystem> otherSystems = getSystemsEnum(other);
                if (!otherSystems.contains(system)) {
                    continue;
                }

                sourceConversionInfo = parseUnitId(other, sourceBase, false);
                Rational otherValue =
                        baseUnitToInput.multiply(sourceConversionInfo.factor.reciprocal());

                if (otherValue.compareTo(Rational.ONE) < 0) {
                    if (otherValue.compareTo(closestLessValue) > 0) {
                        closestLess = other;
                        closestLessValue = otherValue;
                    }
                } else {
                    if (otherValue.compareTo(closestGreaterValue) < 0) {
                        closestGreater = other;
                        closestGreaterValue = otherValue;
                    }
                }
            }
            putIfInRange(result, closestLess, closestLessValue);
            putIfInRange(result, closestGreater, closestGreaterValue);
        }

        result.remove(Rational.ONE, inputUnit); // simplest to do here
        return result;
    }

    public void putIfInRange(Map<Rational, String> result, String baseUnit, Rational otherValue) {
        if (baseUnit != null
                && otherValue.compareTo(LIMIT_LOWER_RELATED) >= 0
                && otherValue.compareTo(LIMIT_UPPER_RELATED) <= 0) {
            if (baseUnitToQuantity.get(baseUnit) != null) {
                baseUnit = getStandardUnit(baseUnit);
            }
            result.put(otherValue, baseUnit);
        }
    }

    static final Set<UnitSystem> NO_UK =
            Set.copyOf(Sets.difference(UnitSystem.ALL, Set.of(UnitSystem.uksystem)));
    static final Set<UnitSystem> NO_JP =
            Set.copyOf(Sets.difference(UnitSystem.ALL, Set.of(UnitSystem.jpsystem)));
    static final Set<UnitSystem> NO_JP_UK =
            Set.copyOf(
                    Sets.difference(
                            UnitSystem.ALL, Set.of(UnitSystem.jpsystem, UnitSystem.uksystem)));

    /**
     * Customize the systems according to the locale
     *
     * @return
     */
    public static Set<UnitSystem> getExampleUnitSystems(String locale) {
        String language = CLDRLocale.getInstance(locale).getLanguage();
        switch (language) {
            case "ja":
                return NO_UK;
            case "en":
                return NO_JP;
            default:
                return NO_JP_UK;
        }
    }

    /**
     * Resolve the unit if possible, eg gram-square-second-per-second ==> gram-second <br>
     * TODO handle complex units that don't match a simple quantity, eg
     * kilogram-ampere-per-meter-square-second => pascal-ampere
     */
    public String resolve(String unit) {
        UnitId unitId = createUnitId(unit);
        if (unitId == null) {
            return unit;
        }
        String resolved = unitId.resolve().toString();
        return getStandardUnit(resolved.isBlank() ? unit : resolved);
    }

    public String format(
            final String languageTag,
            Rational outputAmount,
            final String unit,
            UnlocalizedNumberFormatter nf3) {
        final CLDRConfig config = CLDRConfig.getInstance();
        Factory factory = config.getCldrFactory();
        int pos = languageTag.indexOf("-u");
        String localeBase =
                (pos < 0 ? languageTag : languageTag.substring(0, pos)).replace('-', '_');
        CLDRFile localeFile = factory.make(localeBase, true);
        PluralRules pluralRules =
                config.getSupplementalDataInfo()
                        .getPluralRules(
                                localeBase, com.ibm.icu.text.PluralRules.PluralType.CARDINAL);
        String pluralCategory = pluralRules.select(outputAmount.doubleValue());
        String path =
                UnitPathType.unit.getTranslationPath(
                        localeFile, "long", unit, pluralCategory, "nominative", "neuter");
        String pattern = localeFile.getStringValue(path);
        final ULocale uLocale = ULocale.forLanguageTag(languageTag);
        String cldrFormattedNumber =
                nf3.locale(uLocale).format(outputAmount.doubleValue()).toString();
        return com.ibm.icu.text.MessageFormat.format(pattern, cldrFormattedNumber);
    }
}
