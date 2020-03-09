package org.unicode.cldr.unittest;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.Rational;
import org.unicode.cldr.util.UnitConverter.ConversionInfo;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

final class ExternalUnitConversionData {
    static final Pattern footnotes = Pattern.compile(" \\d+$");
    static final Pattern firstPart = Pattern.compile("([^\\[(,]*)(.*)");
    static final Pattern temperature = Pattern.compile("\\((\\d+)(?:\\.(\\d+))? °([CF])\\)");
    static final Pattern addHyphens = Pattern.compile("[- ]+");

    // first source is https://www.nist.gov/pml/special-publication-811/nist-guide-si-appendix-b-conversion-factors/nist-guide-si-appendix-b9
    public final String quantity;
    public final String source;
    public final String target;
    public final ConversionInfo info;
    public final String line;

    public ExternalUnitConversionData(String quantity, String source, String target, Rational factor, String line, Multimap<String,String> changes) {
        super();
        this.quantity = quantity;


        LinkedHashSet<String> sourceChanges = new LinkedHashSet<>();
        this.source = extractUnit(quantity, source, sourceChanges);
        changes.putAll(source, sourceChanges);

        LinkedHashSet<String> targetChanges = new LinkedHashSet<>();
        this.target = extractUnit(quantity, target, targetChanges);
        changes.putAll(target, targetChanges);

        Rational offset = temperatureHack.get(this.source + "|" + this.target);
        this.info = new ConversionInfo(factor, offset == null ? Rational.ZERO : offset);
        this.line = line;
    }

    // HACK for temperature
    /**
        degree Celsius (°C) kelvin (K)  T/K = t/°C + 273.15
        degree centigrade 15    degree Celsius (°C) t/°C ≈ t/deg. cent.
        degree Fahrenheit (°F)  degree Celsius (°C) t/°C = (t/°F - 32)/1.8
        degree Fahrenheit (°F)  kelvin (K)  T/K = (t/°F + 459.67)/1.8
        degree Rankine (°R) kelvin (K)  T/K = (T/°R)/1.8
        kelvin (K)  degree Celsius (°C) t/°C = T/K - 273.15
     */
    static final Map<String,Rational> temperatureHack = ImmutableMap.of(
        "fahrenheit|celsius", Rational.of("-32/1.8"),
        "fahrenheit|kelvin", Rational.of("459.67/1.8"),
        "celsius|kelvin", Rational.of("273.15")
        );


    private String extractUnit(String quantity, String source, Set<String> changes) {
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

        source = replaceWhole(oldSource, source, changes);

        final Matcher match = firstPart.matcher(source);
        match.matches();
        String newSource = match.group(1).trim();
        source = replaceWhole(source, newSource, changes);

        String remainder = match.group(2);
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
        source = source.replace("tonne", "metric-ton");
        source = source.replace("ton-metric", "metric-ton");
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

        source = replaceWhole(oldSource, source, changes);

        // don't record these
        source = source.toLowerCase(Locale.ROOT);
        source = addHyphens.matcher(source.trim()).replaceAll("-");

        return source;
    }

    private String replaceWhole(String source, String newSource, Set<String> changes) {
        if (!newSource.equals(source)) {
            changes.add(" ⟹ " + newSource);
        }
        return newSource;
    }

    private String replace(Pattern pattern, String source, String replacement, Set<String> changes) {
        String newSource = pattern.matcher(source).replaceAll(replacement);
        if (!newSource.equals(source)) {
            changes.add(" ⟹ " + newSource);
        }
        return newSource;
    }

    @Override
    public String toString() {
        return quantity + "\t" + source + "\t" + target + "\t" + info + "\t«" + line + "»";
    }
}