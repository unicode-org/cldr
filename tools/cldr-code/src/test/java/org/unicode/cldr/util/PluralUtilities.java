package org.unicode.cldr.util;

import static java.util.Comparator.comparing;

import com.google.common.base.Splitter;
import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.text.PluralRules;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;

public class PluralUtilities {

    private static CLDRConfig testInfo = CLDRConfig.getInstance();
    private static SupplementalDataInfo supp = testInfo.getSupplementalDataInfo();

    public static final MapComparator<String> PLURAL_OPERAND =
            new MapComparator<String>()
                    .add("n", "i", "v", "w", "f", "t", "c", "e")
                    .setErrorOnMissing(false)
                    .freeze();

    public static final Comparator<String> PLURAL_RELATION =
            comparing(
                            (String foo) ->
                                    foo == null || foo.isEmpty() ? " " : foo.substring(0, 1),
                            PLURAL_OPERAND)
                    .thenComparing(foo -> foo.contains("%"))
                    .thenComparing(foo -> foo.contains("!="))
                    .thenComparing(foo -> foo);

    public static final Splitter AND_OR =
            Splitter.on(Pattern.compile("\\b(or|and)\\b")).trimResults().omitEmptyStrings();

    // Utility methods and backing data

    /**
     * Return a representative locale that has the same rules as the sourceLocale. If two locales
     * have the same plural rules, they have the same representative. All locales without plural
     * rules OR that inherit, return "und"
     *
     * @param sourceLocale
     * @return
     */
    public String getRepresentativeLocaleForPluralRules(String sourceLocale) {
        String result = localeToRepresentative.get(sourceLocale);
        return result == null ? "und" : result;
    }

    /**
     * Return a map from representative locale to those with the same rules as the sourceLocale. If
     * two locales have the same plural rules, they have the same representative.
     *
     * @param sourceLocale
     * @return
     */
    public static ImmutableMap<String, String> getLocaleToRepresentative() {
        return localeToRepresentative;
    }

    /**
     * The inverse of getRepresentativeLocaleForPluralRules; returns a mapping of representitive
     * locales.
     */
    public static ImmutableMultimap<String, String> getRepresentativeToLocales() {
        return representativeToLocales;
    }

    /**
     * Get map from representative To
     *
     * @return
     */
    public static ImmutableMap<String, PluralRules> getRepresentativeToPluralRules() {
        return representativeToPluralRules;
    }

    /**
     * TTturns a mapping of category sets (eg {one, many}) to representative locales
     *
     * @return
     */
    public static Multimap<String, String> getCategorySetToRepresentativeLocales() {
        return categorySetToRepresentativeLocales;
    }

    public static Comparator<String> ORDER_LOCALES_BY_POP =
            new Comparator<>() {
                @Override
                public int compare(String o1, String o2) {
                    return Comparator.comparing(
                                    (String x) -> {
                                        PopulationData popData = supp.getLanguagePopulationData(x);
                                        return popData == null
                                                ? 0
                                                : popData.getLiteratePopulation();
                                    })
                            .reversed()
                            .compare(o1, o2);
                }
            };

    public static ImmutableMap<String, Set<Count>> getRepresentativeToCountSet() {
        return representativeToCountSet;
    }

    public static <T, S extends Comparable<T>> Comparator<Iterable<S>> iterableComparator() {
        return Comparators.lexicographical(Comparator.<Comparable>naturalOrder().reversed());
    }

    private static final ImmutableMap<String, String> localeToRepresentative;
    private static final ImmutableMultimap<String, String> categorySetToRepresentativeLocales;

    private static final ImmutableMap<String, PluralRules> representativeToPluralRules;
    private static final ImmutableMultimap<String, String> representativeToLocales;
    private static final ImmutableMap<String, Set<Count>> representativeToCountSet;

    static {
        Map<String, String> _localeToRepresentative = new TreeMap<>();
        Map<String, PluralRules> _representativeToPluralRules = new TreeMap<>();
        Multimap<String, String> _representativeToLocales = TreeMultimap.create();
        Multimap<String, String> _categorySetToLocales = TreeMultimap.create();
        Multimap<Set<Count>, String> _countSetToRepresentative =
                TreeMultimap.create(
                        iterableComparator(),
                        ORDER_LOCALES_BY_POP); // sort by count set, then locale population

        Multimap<PluralRules, String> rulesToLocales = LinkedHashMultimap.create();
        supp.getPluralLocales().stream()
                .forEach(
                        locale -> {
                            PluralInfo pluralInfo = supp.getPlurals(locale);
                            PluralRules rules = pluralInfo.getPluralRules();
                            rulesToLocales.put(rules, locale);
                        });

        // now that we have the rules mapping to the same set of locales, make the other data

        rulesToLocales.asMap().entrySet().stream()
                .forEach(
                        x -> {
                            // sort the set to get the first
                            ImmutableSortedSet<String> set =
                                    ImmutableSortedSet.copyOf(x.getValue());
                            TreeSet<String> sortedByPop = new TreeSet<>(ORDER_LOCALES_BY_POP);
                            sortedByPop.addAll(x.getValue());
                            String representative =
                                    sortedByPop.iterator().next(); // pick first in population

                            set.stream()
                                    .forEach(y -> _localeToRepresentative.put(y, representative));
                            _representativeToLocales.putAll(representative, set);
                            PluralInfo pluralInfo = supp.getPlurals(representative);
                            PluralRules rules = pluralInfo.getPluralRules();
                            Set<Count> categories =
                                    ImmutableSet.copyOf(
                                            rules.getKeywords().stream()
                                                    .map(y -> Count.valueOf(y))
                                                    .collect(Collectors.toList()));
                            _categorySetToLocales.put(Joiners.SP.join(categories), representative);
                            _representativeToPluralRules.put(representative, rules);
                            _countSetToRepresentative.put(categories, representative);
                        });

        Map<String, Set<Count>> temp = new LinkedHashMap<>();
        for (Map.Entry<? extends Set<Count>, ? extends String> entry :
                _countSetToRepresentative.entries()) {
            if (temp.put(entry.getValue(), entry.getKey()) != null) {
                throw new IllegalArgumentException("Should never happen!");
            }
        }

        localeToRepresentative = ImmutableMap.copyOf(_localeToRepresentative);
        representativeToCountSet = ImmutableMap.copyOf(temp);
        representativeToPluralRules = ImmutableMap.copyOf(_representativeToPluralRules);
        representativeToLocales = ImmutableMultimap.copyOf(_representativeToLocales);
        categorySetToRepresentativeLocales = ImmutableMultimap.copyOf(_categorySetToLocales);
    }
}
