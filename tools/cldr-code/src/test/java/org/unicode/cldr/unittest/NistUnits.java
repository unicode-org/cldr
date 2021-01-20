package org.unicode.cldr.unittest;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.UnitConverter.ConversionInfo;
import org.unicode.cldr.util.UnitConverter.TargetInfo;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.util.ICUUncheckedIOException;

final class NistUnits {
    private static final boolean DEBUG = false;

    final static Multimap<String,String> unitToQuantity;
    final static Map<String, TargetInfo> derivedUnitToConversion;
    final static List<ExternalUnitConversionData> externalConversionData;
    final static Multimap<String, String> idChanges;
    final static Set<String> skipping;

    static final Splitter SPLIT_MIDDOT = Splitter.on('·').trimResults();
    static final Pattern flatExponent = Pattern.compile("([a-zA-Z]+)(-?[0-9]+)?");
    static final Splitter SPLIT_TABS = Splitter.on('\t').trimResults();
    static final Splitter SPLIT_COMMAS = Splitter.on(',').trimResults();
    static final Splitter SPLIT_PARENS = Splitter.on('(').trimResults();


    static {
        try {
            Multimap<String, String> _idChanges = LinkedHashMultimap.create();
            Set<String> _skipping = new LinkedHashSet<>();

            List<ExternalUnitConversionData> _externalConversionData = new ArrayList<>();
            try (BufferedReader in = CldrUtility.getUTF8Data("external/nistConversions.txt")) {
                String quantity = null;
                try (Stream<String> s = in.lines()) {
                    for (String line : (Iterable<String>) s::iterator) {
                        if (line.startsWith("#")
                            || line.equals("To convert from\tto\tMultiply by")
                            || line.startsWith("degree Fahrenheit hour square foot per British thermal unitth inch") // bad NIST data
                            ) {
                            continue;
                        }
                        List<String> parts = SPLIT_TABS.splitToList(line);
                        switch(parts.size()) {
                        case 1:
                            quantity = parts.get(0);
                            break;
                        case 4:
                            Rational factor = Rational.of((parts.get(2) + parts.get(3)).replace(" ", ""));
                            ExternalUnitConversionData data = new ExternalUnitConversionData(quantity, parts.get(0), parts.get(1), factor, line, _idChanges);
                            _externalConversionData.add(data);
                            break;
                        default:
                            _skipping.add(line);
                        }
                    }
                }
            }

            Map<String, TargetInfo> unitToTargetInfo = new TreeMap<>();
            Map<String,String> _symbolToUnit = new TreeMap<>();
            Multimap<String,String> _unitToQuantity = TreeMultimap.create();
            try (BufferedReader in = CldrUtility.getUTF8Data("external/nistBaseUnits.txt")) {
                try (Stream<String> s = in.lines()) {
                    for (String line : (Iterable<String>) s::iterator) {
                        if (line.startsWith("#")) {
                            continue;
                        }
                        List<String> parts = SPLIT_TABS.splitToList(line);
                        //#Base quantity  Name    Symbol
                        String quantity2 = parts.get(0);
                        String name = parts.get(1);
                        String symbol = parts.get(2);
                        switch(parts.size()) {
                        case 3:
                            _symbolToUnit.put(symbol, name);
                            _unitToQuantity.put(name, quantity2);
                            break;
                        }
                    }
                }
            }

            try (BufferedReader in = CldrUtility.getUTF8Data("external/nistDerivedUnits.txt")) {
                try (Stream<String> s = in.lines()) {
                    for (String line : (Iterable<String>) s::iterator) {
                        if (line.startsWith("#")) {
                            continue;
                        }
                        List<String> parts = SPLIT_TABS.splitToList(line);
                        // #Quantity   Special Name    Special symbol  Expression in terms of other SI units   Expression in terms of SI base units

                        String quantity = parts.get(0);
                        List<String> quantities = SPLIT_COMMAS.splitToList(quantity).stream()
                            .map(x ->  SPLIT_PARENS.split(parts.get(0)).iterator().next())
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

                        final String targetUnit = getUnitFromSymbols(expressionInBaseSymbols, _symbolToUnit);
                        unitToTargetInfo.put(name, new TargetInfo(targetUnit, new ConversionInfo(Rational.ONE, Rational.ZERO), Collections.emptyMap()));

                        ExternalUnitConversionData data = new ExternalUnitConversionData(quantity, name, targetUnit, Rational.ONE, line, _idChanges);
                        _externalConversionData.add(data);

                    }
                }
            }

            // Protect everything

            skipping = ImmutableSet.copyOf(_skipping);
            idChanges = ImmutableMultimap.copyOf(_idChanges);
            externalConversionData = ImmutableList.copyOf(_externalConversionData);
            unitToQuantity = ImmutableMultimap.copyOf(_unitToQuantity);
            derivedUnitToConversion = ImmutableMap.copyOf(unitToTargetInfo);
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    public static String getUnitFromSymbols(String expressionInBaseSymbols, Map<String, String> symbolToUnit) {
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
                    switch(Math.abs(power)) {
                    case 0: case 1: break;// skip
                    case 2: pow = "square-"; break;
                    case 3: pow = "cubic-"; break;
                    default: pow = "pow" + Math.abs(power) + "-"; break;
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
            result = (numerator.length() == 0 ? "" : numerator)
                + (denominator.length() == 0 ? "" :
                    (numerator.length() == 0 ? "per-" : "-per-") + denominator);
        }
        if (DEBUG) System.out.println(expressionInBaseSymbols + " => " + result);
        return result;
    }

}