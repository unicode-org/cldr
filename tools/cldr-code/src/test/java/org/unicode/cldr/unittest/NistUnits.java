package org.unicode.cldr.unittest;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.Output;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.UnitConverter.ConversionInfo;
import org.unicode.cldr.util.UnitConverter.TargetInfo;
import org.unicode.cldr.util.UnitConverter.UnitSystem;

final class NistUnits {
    private static final boolean DEBUG = false;

    public static final String NIST_CONVERSIONS = "nistConversions";
    public static final String NIST_DERIVED_UNITS = "nistDerivedUnits";
    public static final String NIST_BASE_UNITS = "nistBaseUnits";
    public static final String NIST_ACCEPTED_UNITS = "nistAcceptedUnits";

    static final Splitter SPLIT_MIDDOT = Splitter.on('·').trimResults();
    static final Splitter SPLIT_TABS = Splitter.on('\t').trimResults();
    static final Splitter SPLIT_COMMAS = Splitter.on(',').trimResults();
    static final Splitter SPLIT_PARENS = Splitter.on('(').trimResults();

    static final Pattern flatExponent = Pattern.compile("([a-zA-Z]+)(-?[0-9]+)?");
    static final Pattern footnotes = Pattern.compile(" \\d+$");
    static final Pattern firstPart = Pattern.compile("([^\\[(,]*)(.*)");
    static final Pattern temperature = Pattern.compile("\\((\\d+)(?:\\.(\\d+))? °([CF])\\)");
    static final Pattern addHyphens = Pattern.compile("[- ]+");
    static final Pattern finalParens = Pattern.compile("\\(([^()]+)\\)$");

    static final Set<UnitSystem> SI_METRIC = Set.of(UnitSystem.si, UnitSystem.metric);

    static final Multimap<String, String> unitToQuantity;
    static final Map<String, TargetInfo> derivedUnitToConversion;
    static final Set<ExternalUnitConversionData> externalConversionData;
    static final Multimap<String, String> idChanges;
    static final Set<String> skipping;
    static final Multimap<String, String> unitToSystems = null;
    static final Map<String, ExternalUnitConversionData> unitToData;
    static final Set<String> SiAcceptable;

    // HACK for temperature
    /**
     * degree Celsius (°C) kelvin (K) T/K = t/°C + 273.15 degree centigrade 15 degree Celsius (°C)
     * t/°C ≈ t/deg. cent. degree Fahrenheit (°F) degree Celsius (°C) t/°C = (t/°F - 32)/1.8 degree
     * Fahrenheit (°F) kelvin (K) T/K = (t/°F + 459.67)/1.8 degree Rankine (°R) kelvin (K) T/K =
     * (T/°R)/1.8 kelvin (K) degree Celsius (°C) t/°C = T/K - 273.15
     */
    static final Map<String, Rational> temperatureHack =
            ImmutableMap.of(
                    "fahrenheit|celsius", Rational.of("-32/1.8"),
                    "fahrenheit|kelvin", Rational.of("459.67/1.8"),
                    "celsius|kelvin", Rational.of("273.15"));

    static {
        Multimap<String, String> _idChanges = LinkedHashMultimap.create();
        Set<String> _skipping = new LinkedHashSet<>();
        List<ExternalUnitConversionData> _externalConversionData = new ArrayList<>();
        Multimap<String, String> _unitToQuantity = TreeMultimap.create();
        Map<String, TargetInfo> unitToTargetInfo = new TreeMap<>();
        Map<String, ExternalUnitConversionData> _unitToData = new TreeMap<>();
        Set<String> _siAcceptable = new TreeSet<>();

        load(
                _externalConversionData,
                _unitToQuantity,
                unitToTargetInfo,
                _idChanges,
                _skipping,
                _unitToData,
                _siAcceptable);

        skipping = ImmutableSet.copyOf(_skipping);
        idChanges = ImmutableMultimap.copyOf(_idChanges);
        externalConversionData = ImmutableSortedSet.copyOf(_externalConversionData);
        unitToData = ImmutableMap.copyOf(_unitToData);
        unitToQuantity = ImmutableMultimap.copyOf(_unitToQuantity);
        derivedUnitToConversion = ImmutableMap.copyOf(unitToTargetInfo);
        SiAcceptable = ImmutableSet.copyOf(_siAcceptable);
        if (DEBUG) {
            for (ExternalUnitConversionData item : externalConversionData) {
                System.out.println(item);
            }
        }
    }

    static void load(
            List<ExternalUnitConversionData> _externalConversionData,
            Multimap<String, String> _unitToQuantity,
            Map<String, TargetInfo> unitToTargetInfo,
            Multimap<String, String> _idChanges,
            Set<String> _skipping,
            Map<String, ExternalUnitConversionData> _unitToData,
            Set<String> _siAcceptable) {
        try {
            // Get the SI acceptable units
            // Unfortunately, this page has inconsistent formats, so we just mine it for
            // the systems
            try (BufferedReader in = CldrUtility.getUTF8Data("external/nistAcceptedUnits.txt")) {
                try (Stream<String> s = in.lines()) {
                    for (String line : (Iterable<String>) s::iterator) {
                        if (line.startsWith("#") || line.isBlank()) {
                            continue;
                        }
                        List<String> parts = SPLIT_TABS.splitToList(line);
                        _siAcceptable.add(parts.get(0).toLowerCase(Locale.ROOT).replace(' ', '-'));
                    }
                }
            }
            // There is also no conversion data for the following.
            // The conversion value for daltons is given in a footnote
            // The only reason we need 'gram' is to get the systems
            _externalConversionData.add(
                    new ExternalUnitConversionData(
                            "mass",
                            "dalton",
                            "Da",
                            "kilogram",
                            Rational.of("1.660538782E-27"),
                            null,
                            Set.of(UnitSystem.si_acceptable),
                            "HACK",
                            "hack"));
            _externalConversionData.add(
                    new ExternalUnitConversionData(
                            "mass",
                            "gram",
                            "g",
                            "kilogram",
                            Rational.of("1E-3"),
                            null,
                            Set.of(UnitSystem.si),
                            "HACK",
                            "hack"));

            try (BufferedReader in = CldrUtility.getUTF8Data("external/nistConversions.txt")) {
                String quantity = null;
                try (Stream<String> s = in.lines()) {
                    for (String line : (Iterable<String>) s::iterator) {
                        if (line.startsWith("#")
                                || line.isBlank()
                                || line.equals("To convert from\tto\tMultiply by")
                                || line.startsWith(
                                        "degree Fahrenheit hour square foot per British thermal unitth inch") // bad NIST data
                        ) {
                            continue;
                        }
                        List<String> parts = SPLIT_TABS.splitToList(line);
                        switch (parts.size()) {
                            case 1:
                                quantity = parts.get(0);
                                break;
                            case 4:
                                Rational factor =
                                        Rational.of((parts.get(2) + parts.get(3)).replace(" ", ""));
                                ExternalUnitConversionData data =
                                        getExternalUnitConversionData(
                                                quantity,
                                                parts.get(0),
                                                null,
                                                parts.get(1),
                                                factor,
                                                null,
                                                _siAcceptable,
                                                NIST_CONVERSIONS,
                                                line,
                                                _idChanges);
                                _externalConversionData.add(data);
                                break;
                            default:
                                _skipping.add(line);
                        }
                    }
                }
            }

            Map<String, String> _symbolToUnit = new TreeMap<>();
            try (BufferedReader in = CldrUtility.getUTF8Data("external/nistBaseUnits.txt")) {
                try (Stream<String> s = in.lines()) {
                    for (String line : (Iterable<String>) s::iterator) {
                        if (line.startsWith("#") || line.isBlank()) {
                            continue;
                        }
                        List<String> parts = SPLIT_TABS.splitToList(line);
                        // #Base quantity  Name    Symbol
                        String quantity2 = parts.get(0);
                        String name = parts.get(1);
                        String symbol = parts.get(2);
                        switch (parts.size()) {
                            case 3:
                                _symbolToUnit.put(symbol, name);
                                _unitToQuantity.put(name, quantity2);
                                ExternalUnitConversionData data =
                                        getExternalUnitConversionData(
                                                quantity2, //
                                                name, //
                                                symbol,
                                                name, //
                                                Rational.ONE, //
                                                null, //
                                                _siAcceptable, //
                                                NIST_BASE_UNITS,
                                                line,
                                                _idChanges);
                                _externalConversionData.add(data);
                                break;
                        }
                    }
                }
            }

            try (BufferedReader in = CldrUtility.getUTF8Data("external/nistDerivedUnits.txt")) {
                try (Stream<String> s = in.lines()) {
                    for (String line : (Iterable<String>) s::iterator) {
                        if (line.startsWith("#") || line.isBlank()) {
                            continue;
                        }
                        List<String> parts = SPLIT_TABS.splitToList(line);
                        // #Quantity   Special Name    Special symbol  Expression in terms of other
                        // SI units   Expression in terms of SI base units

                        String quantity = parts.get(0);
                        List<String> quantities =
                                SPLIT_COMMAS.splitToList(quantity).stream()
                                        .map(
                                                x ->
                                                        SPLIT_PARENS
                                                                .split(parts.get(0))
                                                                .iterator()
                                                                .next())
                                        .collect(Collectors.toList());
                        quantity = Joiner.on(", ").join(quantities);

                        String name = SPLIT_PARENS.split(parts.get(1)).iterator().next();
                        if (name.equals("degree Celsius")) {
                            name = "celsius";
                        }

                        String symbol = parts.get(2);
                        String expressionInOtherSymbols = parts.get(4);
                        String expressionInBaseSymbols = parts.get(4);
                        _symbolToUnit.put(symbol, name);
                        _unitToQuantity.putAll(name, quantities);

                        final String targetUnit =
                                getUnitFromSymbols(expressionInBaseSymbols, _symbolToUnit);
                        unitToTargetInfo.put(
                                name,
                                new TargetInfo(
                                        targetUnit,
                                        new ConversionInfo(Rational.ONE, Rational.ZERO),
                                        Collections.emptyMap()));

                        ExternalUnitConversionData data =
                                getExternalUnitConversionData(
                                        quantity, //
                                        name, //
                                        symbol,
                                        targetUnit, //
                                        Rational.ONE, //
                                        null, //
                                        _siAcceptable, //
                                        NIST_DERIVED_UNITS,
                                        line,
                                        _idChanges);
                        _externalConversionData.add(data);
                    }
                }
            }
            for (ExternalUnitConversionData data : _externalConversionData) {
                _unitToData.put(data.source, data);
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    private static Set<UnitSystem> systems(
            String unit, Set<String> _siAcceptable, UnitSystem... system) {
        TreeSet<UnitSystem> result = new TreeSet<>(Arrays.asList(system));
        if (_siAcceptable.contains(unit)) {
            result.add(UnitSystem.si_acceptable);
        }
        return ImmutableSet.copyOf(result);
    }

    public static String getUnitFromSymbols(
            String expressionInBaseSymbols, Map<String, String> symbolToUnit) {
        String result;
        // handle the irregular formats
        if (expressionInBaseSymbols.equals("m/m")) {
            result = "meter-per-meter";
        } else if (expressionInBaseSymbols.equals("m2/m2")) {
            result = "square-meter-per-square-meter";
        } else {
            // m2 · kg · s-3 · A-1
            StringBuilder numerator = new StringBuilder();
            StringBuilder denominator = new StringBuilder();
            for (String part : SPLIT_MIDDOT.split(expressionInBaseSymbols)) {
                final Matcher parts = flatExponent.matcher(part);
                if (!parts.matches()) {
                    throw new IllegalArgumentException("bad symbol: " + part);
                }
                String unit = symbolToUnit.get(parts.group(1));
                String pow = null;
                int power = 0;
                final String exponent = parts.group(2);
                if (exponent != null) {
                    power = Integer.parseInt(exponent);
                    switch (Math.abs(power)) {
                        case 0:
                        case 1:
                            break; // skip
                        case 2:
                            pow = "square-";
                            break;
                        case 3:
                            pow = "cubic-";
                            break;
                        default:
                            pow = "pow" + Math.abs(power) + "-";
                            break;
                    }
                }
                StringBuilder target = power >= 0 ? numerator : denominator;
                if (target.length() != 0) {
                    target.append('-');
                }
                if (pow != null) {
                    target.append(pow);
                }
                target.append(unit);
            }
            result =
                    (numerator.length() == 0 ? "" : numerator)
                            + (denominator.length() == 0
                                    ? ""
                                    : (numerator.length() == 0 ? "per-" : "-per-") + denominator);
        }
        if (DEBUG) System.out.println(expressionInBaseSymbols + " => " + result);
        return result;
    }

    // https://www.nist.gov/pml/special-publication-811/nist-guide-si-appendix-b-conversion-factors/nist-guide-si-appendix-b9

    public static ExternalUnitConversionData getExternalUnitConversionData(
            String quantity,
            String sourceRaw,
            String symbolRaw,
            String targetRaw,
            Rational factor,
            Rational offset,
            Set<String> acceptable,
            String from,
            String line,
            Multimap<String, String> changes) {
        LinkedHashSet<String> sourceChanges = new LinkedHashSet<>();
        Output<String> symbolOut = new Output<>();
        symbolOut.value = symbolRaw;
        String source = extractUnit(quantity, sourceRaw, sourceChanges, symbolOut);
        String symbol = symbolOut.value;
        changes.putAll(source, sourceChanges);

        LinkedHashSet<String> targetChanges = new LinkedHashSet<>();
        String target = extractUnit(quantity, targetRaw, targetChanges, symbolOut);
        changes.putAll(target, targetChanges);

        offset = temperatureHack.get(source + "|" + target);
        TreeSet<UnitSystem> systems = new TreeSet<>();
        if (acceptable.contains(source)) {
            systems.add(UnitSystem.si_acceptable);
        }
        switch (from) {
            case NIST_BASE_UNITS:
                systems.addAll(SI_METRIC);
                break;
            case NIST_DERIVED_UNITS:
                systems.addAll(SI_METRIC);
                break;
        }
        return new ExternalUnitConversionData(
                quantity, source, symbol, target, factor, offset, systems, from, line);
    }

    private static String extractUnit(
            String quantity, String source, Set<String> changes, Output<String> symbolOut) {
        // drop footnotes
        source = replace(footnotes, source, "", changes);

        if (source.contains("(15 °C)")) {
            int debug = 0;
        }
        source = replace(temperature, source, " $1$2$3", changes);

        String oldSource = source;
        source = source.replace("(sidereal)", "sidereal");
        source = source.replace("(mean)", "mean");
        source = source.replace("(printer's)", "printer");

        source = source.replace("therm (U.S.)", "therm-us");
        source = source.replace("(U.S.)", "");

        source = source.replace("(long, 112 lb)", " long");
        source = source.replace("(troy or apothecary)", "troy");
        source = source.replace("(U.S. survey)", "survey");
        source = source.replace("(tropical)", "tropical");
        source = source.replace("(based on U.S. survey foot)", "survey");
        source = source.replace("(avoirdupois)", "");
        source = source.replace("(metric)", "metric");
        source = source.replace("(electric)", "electric");
        source = source.replace("(water)", "water");
        source = source.replace("(boiler)", "boiler");
        source = source.replace("(0.001 in)", "inch");
        source = source.replace("(365 days)", "365");

        source = source.replace("(U.K.)", "imperial");
        source = source.replace("[Canadian and U.K. (Imperial)]", "imperial");
        source = source.replace("[Canadian and U.K. fluid (Imperial)]", "fluid imperial");

        source = source.replace("second squared", "square second");
        source = source.replace("foot squared", "square foot");
        source = source.replace("meter squared", "square meter");
        source = source.replace("inch squared", "square inch");

        source = source.replace("mile, nautical", "nautical-mile");
        source = source.replace(", technical", " technical");
        source = source.replace(", kilogram (nutrition)", " nutrition");
        source = source.replace("(nutrition)", "nutrition");
        source = source.replace(", metric", " metric");
        source = source.replace(", assay", " assay");
        source = source.replace(", long", " long");
        source = source.replace(", register", " register");

        source = source.replace("foot to the fourth power", "pow4-foot");
        source = source.replace("inch to the fourth power", "pow4-inch");
        source = source.replace("meter to the fourth power", "pow4-meter");

        source = source.replace("Britsh", "british");
        source = source.replace("reciprocal", "per");

        source = replaceWhole(oldSource, source, changes);

        final Matcher match = firstPart.matcher(source);
        match.matches();
        String newSource = match.group(1).trim();
        String remainder = match.group(2);
        if (symbolOut.value == null) {
            Matcher endParens = finalParens.matcher(remainder);
            if (endParens.find()) {
                symbolOut.value = endParens.group(1).trim();
            }
        }
        source = replaceWhole(source, newSource, changes);

        if (remainder.contains("dry")) {
            source = replaceWhole(source, source + " dry", changes);
        } else if (remainder.contains("fluid")) {
            source = replaceWhole(source, source + " fluid", changes);
        }

        if (source.contains("squared")) {
            System.out.println("*FIX squared: " + source);
        }

        oldSource = source;

        source = source.replace("degree ", "");
        source = source.replace("metric-ton", "tonne");
        source = source.replace("ton-metric", "tonne");
        source = source.replace("psi", "pound-force-per-square-inch");
        source = source.replace("ounce fluid", "fluid-ounce");
        source = source.replace("unitthi", "unit");
        source = source.replace("calorieth", "calorie");
        source = source.replace("acceleration of free fall", "g-force");
        source = source.replace("of mercury", "ofhg");
        if (quantity.equals("ANGLE")) {
            source = source.replace("minute", "arc-minute");
            source = source.replace("second", "arc-second");
        }
        source = source.replace("British thermal unitth", "british thermal unit");
        source = source.replace("British thermal unitIT", "british thermal unit it");
        source = source.replace("calorieIT", "calorie it");

        source = replaceWhole(oldSource, source, changes);

        // don't record these
        source = source.toLowerCase(Locale.ROOT);
        source = addHyphens.matcher(source.trim()).replaceAll("-");

        return source;
    }

    private static String replaceWhole(String source, String newSource, Set<String> changes) {
        if (!newSource.equals(source)) {
            changes.add(" ⟹ " + newSource);
        }
        return newSource;
    }

    private static String replace(
            Pattern pattern, String source, String replacement, Set<String> changes) {
        String newSource = pattern.matcher(source).replaceAll(replacement);
        if (!newSource.equals(source)) {
            changes.add(" ⟹ " + newSource);
        }
        return newSource;
    }
}
