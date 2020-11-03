package org.unicode.cldr.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.util.Freezable;
import com.ibm.icu.util.Output;

/**
 * Get the info from supplemental data, eg CLDRConfig.getInstance().getSupplementalDataInfo().getGrammarInfo("fr"); Use hasGrammarInfo() to see which locales have it.
 * @author markdavis
 *
 */
public class GrammarInfo implements Freezable<GrammarInfo>{

    public enum GrammaticalTarget {nominal}

    public enum GrammaticalFeature {
        grammaticalNumber("plural", "Ⓟ", "other"),
        grammaticalCase("case", "Ⓒ", "nominative"),
        grammaticalDefiniteness("definiteness", "Ⓓ", "indefinite"),
        grammaticalGender("gender", "Ⓖ", "neuter");

        private final String shortName;
        private final String symbol;
        private final String defaultValue;

        public static final Pattern PATH_HAS_FEATURE = Pattern.compile("\\[@(count|case|gender|definiteness)=");

        GrammaticalFeature(String shortName, String symbol, String defaultValue) {
            this.shortName = shortName;
            this.symbol = symbol;
            this.defaultValue = defaultValue;
        }
        public String getShortName() {
            return shortName;
        }
        public CharSequence getSymbol() {
            return symbol;
        }
        public String getDefault(Collection<String> values) {
            return this == grammaticalGender && values != null && !values.contains("neuter") ? "masculine" : defaultValue;
        }
        public static Matcher pathHasFeature(String path) {
            Matcher result = PATH_HAS_FEATURE.matcher(path);
            return result.find() ? result : null;
        }
        static final Map<String, GrammaticalFeature> shortNameToEnum =
            ImmutableMap.copyOf(Arrays.asList(GrammaticalFeature.values())
            .stream()
            .collect(Collectors.toMap(e -> e.shortName, e -> e)));

        public static GrammaticalFeature fromName(String name) {
            GrammaticalFeature result = shortNameToEnum.get(name);
            return result != null ? result : valueOf(name);
        }
    }

    public enum GrammaticalScope {general, units}

    private Map<GrammaticalTarget, Map<GrammaticalFeature, Map<GrammaticalScope,Set<String>>>> targetToFeatureToUsageToValues = new TreeMap<>();
    private boolean frozen = false;

    public void add(GrammaticalTarget target, GrammaticalFeature feature, GrammaticalScope usage, String value) {
        Map<GrammaticalFeature, Map<GrammaticalScope,Set<String>>> featureToUsageToValues = targetToFeatureToUsageToValues.get(target);
        if (featureToUsageToValues == null) {
            targetToFeatureToUsageToValues.put(target, featureToUsageToValues = new TreeMap<>());
        }
        if (feature != null) {
            Map<GrammaticalScope,Set<String>> usageToValues = featureToUsageToValues.get(feature);
            if (usageToValues == null) {
                featureToUsageToValues.put(feature, usageToValues = new TreeMap<>());
            }
            Set<String> values = usageToValues.get(usage);
            if (values == null) {
                usageToValues.put(usage, values = new TreeSet<>());
            }
            if (value != null) {
                values.add(value);
            } else {
                int debug = 0;
            }
        }
    }

    public void add(GrammaticalTarget target, GrammaticalFeature feature, GrammaticalScope usage, Collection<String> valueSet) {
        Map<GrammaticalFeature, Map<GrammaticalScope,Set<String>>> featureToUsageToValues = targetToFeatureToUsageToValues.get(target);
        if (featureToUsageToValues == null) {
            targetToFeatureToUsageToValues.put(target, featureToUsageToValues = new TreeMap<>());
        }
        if (feature != null) {
            Map<GrammaticalScope,Set<String>> usageToValues = featureToUsageToValues.get(feature);
            if (usageToValues == null) {
                featureToUsageToValues.put(feature, usageToValues = new TreeMap<>());
            }
            Set<String> values = usageToValues.get(usage);
            if (values == null) {
                usageToValues.put(usage, values = new TreeSet<>());
            }
            values.addAll(valueSet);
        }
    }


    /**
     * Note: when there is known to be no features, the featureRaw will be null
     */
    public void add(String targetsRaw, String featureRaw, String usagesRaw, String valuesRaw) {
        for (String targetString : SupplementalDataInfo.split_space.split(targetsRaw)) {
            GrammaticalTarget target = GrammaticalTarget.valueOf(targetString);
            if (featureRaw == null) {
                add(target, null, null, (String)null);
            } else {
                final GrammaticalFeature feature = GrammaticalFeature.valueOf(featureRaw);

                List<String> usages = usagesRaw == null ? Collections.singletonList(GrammaticalScope.general.toString()) : SupplementalDataInfo.split_space.splitToList(usagesRaw);

                List<String> values = valuesRaw == null ? Collections.emptyList() : SupplementalDataInfo.split_space.splitToList(valuesRaw);
                for (String usageRaw : usages) {
                    GrammaticalScope usage = GrammaticalScope.valueOf(usageRaw);
                    add(target, feature, usage, values);
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
            Map<GrammaticalTarget, Map<GrammaticalFeature, Map<GrammaticalScope, Set<String>>>> temp = CldrUtility.protectCollection(targetToFeatureToUsageToValues);
            if (!temp.equals(targetToFeatureToUsageToValues)) {
                throw new IllegalArgumentException();
            }
            targetToFeatureToUsageToValues = temp;
            frozen = true;
        }
        return this;
    }

    @Override
    public GrammarInfo cloneAsThawed() {
        GrammarInfo result = new GrammarInfo();
        this.forEach3((t,f,u,v) -> result.add(t,f,u,v));
        return result;
    }

    static interface Handler4<T,F,U,V> {
        void apply(T t, F f, U u, V v);
    }

    public void forEach(Handler4<GrammaticalTarget, GrammaticalFeature, GrammaticalScope, String> handler) {
        for (Entry<GrammaticalTarget, Map<GrammaticalFeature, Map<GrammaticalScope,Set<String>>>> entry1 : targetToFeatureToUsageToValues.entrySet()) {
            GrammaticalTarget target = entry1.getKey();
            final Map<GrammaticalFeature, Map<GrammaticalScope,Set<String>>> featureToUsageToValues = entry1.getValue();
            if (featureToUsageToValues.isEmpty()) {
                handler.apply(target, null, null, null);
            } else
                for (Entry<GrammaticalFeature, Map<GrammaticalScope,Set<String>>> entry2 : featureToUsageToValues.entrySet()) {
                    GrammaticalFeature feature = entry2.getKey();
                    for (Entry<GrammaticalScope, Set<String>> entry3 : entry2.getValue().entrySet()) {
                        final GrammaticalScope usage = entry3.getKey();
                        for (String value : entry3.getValue()) {
                            handler.apply(target, feature, usage, value);
                        }
                    }
                }
        }
    }

    static interface Handler3<T,F,U, V> {
        void apply(T t, F f, U u, V v);
    }

    public void forEach3(Handler3<GrammaticalTarget, GrammaticalFeature, GrammaticalScope, Collection<String>> handler) {
        for (Entry<GrammaticalTarget, Map<GrammaticalFeature, Map<GrammaticalScope,Set<String>>>> entry1 : targetToFeatureToUsageToValues.entrySet()) {
            GrammaticalTarget target = entry1.getKey();
            final Map<GrammaticalFeature, Map<GrammaticalScope,Set<String>>> featureToUsageToValues = entry1.getValue();
            if (featureToUsageToValues.isEmpty()) {
                handler.apply(target, null, null, null);
            } else
                for (Entry<GrammaticalFeature, Map<GrammaticalScope,Set<String>>> entry2 : featureToUsageToValues.entrySet()) {
                    GrammaticalFeature feature = entry2.getKey();
                    for (Entry<GrammaticalScope, Set<String>> entry3 : entry2.getValue().entrySet()) {
                        final GrammaticalScope usage = entry3.getKey();
                        final Collection<String> values = entry3.getValue();
                        handler.apply(target, feature, usage, values);
                    }
                }
        }
    }

    /** Returns null if there is no known information. Otherwise returns the information for the locale (which may be empty if there are no variants) */
    public Collection<String> get(GrammaticalTarget target, GrammaticalFeature feature, GrammaticalScope usage) {
        Map<GrammaticalFeature, Map<GrammaticalScope,Set<String>>> featureToUsageToValues = targetToFeatureToUsageToValues.get(target);
        if (featureToUsageToValues == null) {
            return Collections.emptySet();
        }
        Map<GrammaticalScope,Set<String>> usageToValues = featureToUsageToValues.get(feature);
        if (usageToValues == null) {
            return Collections.emptySet();
        }
        Collection<String> result = usageToValues.get(usage);
        return result == null
            ? usageToValues.get(GrammaticalScope.general)
                : result;
    }

    public boolean hasInfo(GrammaticalTarget target) {
        return targetToFeatureToUsageToValues.containsKey(target);
    }

    @Override
    public String toString() {
        return toString("\n");
    }
    public String toString(String lineSep) {
        StringBuilder result = new StringBuilder();
        this.forEach3((t,f,u, v) ->
        {
            result.append(lineSep);
            result.append("{" + (t == null ? "" : t.toString()) + "}"
                + "\t{" + (f == null ? "" : f.toString()) + "}"
                + "\t{" +  (u == null ? "" : u.toString()) + "}"
                + "\t{" +  (v == null ? "" : Joiner.on(' ').join(v)) + "}");
        });
        return result.toString();
    }

    static public String getGrammaticalInfoAttributes(GrammarInfo grammarInfo, UnitPathType pathType, String plural, String gender, String caseVariant) {
        String grammaticalAttributes = "";
        if (pathType.features.contains(GrammaticalFeature.grammaticalNumber)) { // count is special
            grammaticalAttributes += "[@count=\"" + (plural == null ? "other" : plural) + "\"]";
        }
        if (grammarInfo != null && gender != null
            && pathType.features.contains(GrammaticalFeature.grammaticalGender)
            ) {
            Collection<String> genders = grammarInfo.get(GrammaticalTarget.nominal, GrammaticalFeature.grammaticalGender, GrammaticalScope.units);
            if (!gender.equals(GrammaticalFeature.grammaticalGender.getDefault(genders))) {
                grammaticalAttributes += "[@gender=\"" + gender + "\"]";
            }
        }
        if (grammarInfo != null && caseVariant != null
            && pathType.features.contains(GrammaticalFeature.grammaticalCase)
            && !caseVariant.equals(GrammaticalFeature.grammaticalCase.getDefault(null))) {
            grammaticalAttributes += "[@case=\"" + caseVariant + "\"]";
        }
        return grammaticalAttributes;
    }

    public static final ImmutableMultimap<String,PluralInfo.Count> NON_COMPUTABLE_PLURALS = ImmutableListMultimap.of(
        "pl", PluralInfo.Count.one,
        "pl", PluralInfo.Count.other,
        "ru", PluralInfo.Count.one,
        "ru", PluralInfo.Count.other);
    /**
     * TODO: change this to be data-file driven
     */
    public static final Set<String> SEED_LOCALES = ImmutableSet.of("pl", "ru", "da", "de", "nb", "sv", "hi", "id", "es", "fr", "it", "nl", "pt", "en", "ja", "th", "vi", "zh", "zh_TW", "ko", "yue");

    /**
     * TODO: change this to be data-file driven
     */
    public static final Set<String> SPECIAL_TRANSLATION_UNITS = ImmutableSet.of(
        // new in v38
        "mass-grain",
        "volume-dessert-spoon",
        "volume-dessert-spoon-imperial",
        "volume-drop",
        "volume-dram",
        "volume-jigger",
        "volume-pinch",
        "volume-quart-imperial",
        // "volume-pint-imperial",

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


    public void getSourceCaseAndPlural(String locale, String gender, String value, String desiredCase, String desiredPlural,
        Output<String> sourceCase, Output<String> sourcePlural) {
        switch(locale) {
        case "pl":
            getSourceCaseAndPluralPolish(gender, value, desiredCase, desiredPlural, sourceCase, sourcePlural);
            break;
        case "ru":
            getSourceCaseAndPluralRussian(gender, value, desiredCase, desiredPlural, sourceCase, sourcePlural);
            break;
        default:
            throw new UnsupportedOperationException(locale);
        }
    }

    /** Russian rules for paucal (few) and fractional (other)
     * <pre>
     * plural = other
     * Nominative ⇒ genitive singular
     * Accusative + masculine ⇒ genitive singular
     * All other combinations of gender + case ⇒ same-case, plural
     *
     * Other
     * genitive singular
     *
     * Plurals:
     *   one,
     *   few (2~4),
     *   many, = plural
     *   other (where other is 0.0~1.5, 10.0, 100.0, 1000.0, 10000.0, 100000.0, 1000000.0)
     * </pre>
     */
    private void getSourceCaseAndPluralRussian(String gender, String value,
        String desiredCase, String desiredPlural,
        Output<String> sourceCase, Output<String> sourcePlural) {
        switch (desiredPlural) {
        case "few":
            // default source
            sourceCase.value = desiredCase;
            sourcePlural.value = "many";
            // special cases
            switch (desiredCase) {
            case "nominative":
                sourceCase.value = "genitive";
                sourcePlural.value = "one";
                break;
            case "accusative":
                switch (gender) {
                case "masculine":
                    sourceCase.value = "genitive";
                    sourcePlural.value = "one";
                    break;
                }
                break;
            }
        case "other":
            sourceCase.value = "genitive";
            sourcePlural.value = "one";
            return;
        }
    }

    /** Polish rules
     * <pre>
     * plural = few
     *
     * neuter + ending in -um + (nominative, accusative) ⇒ vocative plural
     * Feminine||neuter + (nominative, accusative) ⇒ genitive singular
     * Animate||inanimate + (nominative, accusative) ⇒ vocative plural
     * Personal + nominative ⇒ vocative plural
     * Personal + accusative ⇒ genitive plural
     * All other combinations of gender + case ⇒ same-case, plural
     *
     * plural = other
     * genitive singular
     *
     * Plurals:
     *   one,
     *   few (2~4),
     *   many, = plural
     *   other (where other is 0.0~1.5, 10.0, 100.0, 1000.0, 10000.0, 100000.0, 1000000.0)
     * </pre>
     */
    private void getSourceCaseAndPluralPolish(String gender, String value,
        String desiredCase, String desiredPlural,
        Output<String> sourceCase, Output<String> sourcePlural) {
        switch (desiredPlural) {
        case "few":
            // default
            sourceCase.value = desiredCase;
            sourcePlural.value = "many";
            // special cases
            boolean isNominative = false;
            switch (desiredCase) {
            case "nominative":
                isNominative = true;
            case "vocative":
            case "accusative":
                switch (gender) {
                case "neuter":
                    if (value.endsWith("um")) {
                        sourceCase.value = "vocative";
                        break;
                    }
                    // otherwise fall thorugh to feminine
                case "feminine":
                    sourceCase.value = "nominative";
                    sourcePlural.value = "few";
                    break;
                case "animate":
                case "inanimate":
                    sourceCase.value = "vocative";
                    break;
                case "personal":
                    sourceCase.value = isNominative ? "vocative" : "genitive";
                    break;
                }
                break;
            }
            return;
        case "other":
            sourceCase.value = "genitive";
            sourcePlural.value = "one";
            return;
        }
    }
}