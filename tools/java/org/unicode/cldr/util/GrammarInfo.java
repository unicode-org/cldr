package org.unicode.cldr.util;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.ibm.icu.util.Freezable;

public class GrammarInfo implements Freezable<GrammarInfo>{
    
    public enum GrammaticalTarget {nominal}

    public enum GrammaticalFeature {grammaticalCase("case", "Ⓒ"), grammaticalDefiniteness("definiteness", "Ⓓ"), grammaticalGender("gender", "Ⓖ");
        private final String shortName;
        private final String symbol;
        GrammaticalFeature(String shortName, String symbol) {
            this.shortName = shortName;
            this.symbol = symbol;
        }
        public String getShortName() {
            return shortName;
        }
        public CharSequence getSymbol() {
            return symbol;
        }
    }

    public enum GrammaticalScope {general, units};

    private Map<GrammaticalTarget, Map<GrammaticalFeature, Multimap<GrammaticalScope,String>>> targetToFeatureToUsageToValues = new TreeMap<>();
    private boolean frozen = false;

    public void add(GrammaticalTarget target, GrammaticalFeature feature, GrammaticalScope usage, String value) {
        Map<GrammaticalFeature, Multimap<GrammaticalScope, String>> featureToUsageToValues = targetToFeatureToUsageToValues.get(target);
        if (featureToUsageToValues == null) {
            targetToFeatureToUsageToValues.put(target, featureToUsageToValues = new LinkedHashMap<>());
        }
        if (feature != null) {
            Multimap<GrammaticalScope, String> usageToValues = featureToUsageToValues.get(feature);
            if (usageToValues == null) {
                featureToUsageToValues.put(feature, usageToValues = LinkedHashMultimap.create());
            }
            usageToValues.put(usage, value);
        }
    }

    /**
     * Note: when there is known to be no features, the featureRaw will be null
     */
    public void add(String targetsRaw, String featureRaw, String usagesRaw, String valuesRaw) {
        for (String targetString : SupplementalDataInfo.split_space.split(targetsRaw)) {
            GrammaticalTarget target = GrammaticalTarget.valueOf(targetString);
            if (featureRaw == null) {
                add(target, null, null, null);
            } else {
                final GrammaticalFeature feature = GrammaticalFeature.valueOf(featureRaw);

                List<String> usages = usagesRaw == null ? Collections.singletonList(GrammaticalScope.general.toString()) : SupplementalDataInfo.split_space.splitToList(usagesRaw);

                List<String> values = valuesRaw == null ? null : SupplementalDataInfo.split_space.splitToList(valuesRaw);
                for (String usageRaw : usages) {
                    GrammaticalScope usage = GrammaticalScope.valueOf(usageRaw);
                    for (String value : values) {
                        add(target, feature, usage, value);
                    }
                }
            }
        }
    }

    @Override
    public boolean isFrozen() {
        return frozen;
    }

    @Override
    public GrammarInfo freeze() {
        if (!frozen) {
            frozen = true;
            targetToFeatureToUsageToValues = CldrUtility.protectCollection(targetToFeatureToUsageToValues);
        }
        return this;
    }

    @Override
    public GrammarInfo cloneAsThawed() {
        GrammarInfo result = new GrammarInfo();
        this.forEach((t,f,u,v) -> result.add(t,f,u,v));
        return result;
    }
    
    static interface Handler<T,F,U,V> {
        void apply(T t, F f, U u, V v);
    }
    
    public void forEach(Handler<GrammaticalTarget, GrammaticalFeature, GrammaticalScope, String> handler) {
        for (Entry<GrammaticalTarget, Map<GrammaticalFeature, Multimap<GrammaticalScope, String>>> entry1 : targetToFeatureToUsageToValues.entrySet()) {
            GrammaticalTarget target = entry1.getKey();
            final Map<GrammaticalFeature, Multimap<GrammaticalScope, String>> featureToUsageToValues = entry1.getValue();
            if (featureToUsageToValues.isEmpty()) {
                handler.apply(target, null, null, null);
            } else 
                for (Entry<GrammaticalFeature, Multimap<GrammaticalScope, String>> entry2 : featureToUsageToValues.entrySet()) {
                    GrammaticalFeature feature = entry2.getKey();
                    for (Entry<GrammaticalScope, String> entry3 : entry2.getValue().entries()) {
                        final GrammaticalScope usage = entry3.getKey();
                        final String value = entry3.getValue();
                        handler.apply(target, feature, usage, value);
                    }
                }
        }
    }

    public Collection<String> get(GrammaticalTarget target, GrammaticalFeature feature, GrammaticalScope usage) {
        Map<GrammaticalFeature, Multimap<GrammaticalScope, String>> featureToUsageToValues = targetToFeatureToUsageToValues.get(target);
        if (featureToUsageToValues == null) {
            return Collections.emptySet();
        }
        Multimap<GrammaticalScope, String> usageToValues = featureToUsageToValues.get(feature);
        if (usageToValues == null) {
            return Collections.emptySet(); 
        }
        Collection<String> result = usageToValues.get(usage);
        return result.isEmpty() 
            ? usageToValues.get(GrammaticalScope.general) 
                : result; 
    }

    public boolean hasInfo(GrammaticalTarget target) {
        return targetToFeatureToUsageToValues.containsKey(target);
    }
    
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        this.forEach((t,f,u,v) -> result.append("\t" + t + "\t" + f + "\t" + u + "\t" + v + "\n"));
        return result.toString();
    }
    
    /**
     * TODO: change this to be data-file driven
     */
    public static final Set<String> TRANSLATION_UNITS = ImmutableSet.of(
        // new in v38
        "mass-grain", 
        "volume-dessert-spoon", 
        "volume-dessert-spoon-imperial",
        "volume-drop", 
        "volume-dram-fluid", 
        "volume-jigger", 
        "volume-pinch", 
        "volume-quart-imperial",
        "volume-pint-imperial",
        
        "acceleration-meter-per-square-second", "area-acre", "area-hectare", 
        "area-square-centimeter", "area-square-foot", "area-square-kilometer", "area-square-mile", "concentr-percent", "consumption-mile-per-gallon", 
        "consumption-mile-per-gallon-imperial", "duration-day", "duration-hour", "duration-minute", "duration-month", "duration-second", "duration-week", 
        "duration-year", "energy-foodcalorie", "energy-kilocalorie", "length-centimeter", "length-foot", "length-inch", "length-kilometer", "length-meter", 
        "length-mile", "length-millimeter", "length-parsec", "length-picometer", "length-solar-radius", "length-yard", "light-solar-luminosity", "mass-dalton", 
        "mass-earth-mass", "mass-milligram", "mass-solar-mass", "pressure-kilopascal", "speed-kilometer-per-hour", "speed-meter-per-second", "speed-mile-per-hour", 
        "temperature-celsius", "temperature-fahrenheit", "temperature-generic", "temperature-kelvin", "acceleration-g-force", "consumption-liter-per-100-kilometer", 
        "mass-gram", "mass-kilogram", "mass-ounce", "mass-pound", "volume-centiliter", "volume-cubic-centimeter", "volume-cubic-foot", "volume-cubic-mile", 
        "volume-cup", "volume-deciliter", "volume-fluid-ounce", "volume-fluid-ounce-imperial", "volume-gallon", "volume-gallon", "volume-gallon-imperial", 
        "volume-liter", "volume-milliliter", "volume-pint", "volume-quart", "volume-tablespoon", "volume-teaspoon");
    // compounds
    // "kilogram-per-cubic-meter", "kilometer-per-liter", "concentr-gram-per-mole", "speed-mile-per-second", "volumetricflow-cubic-foot-per-second", 
    // "volumetricflow-cubic-meter-per-second", "gram-per-cubic-centimeter", 
}