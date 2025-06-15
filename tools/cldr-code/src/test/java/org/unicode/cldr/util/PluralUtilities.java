package org.unicode.cldr.util;

import static java.util.Comparator.comparing;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.text.PluralRules;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;

public class PluralUtilities {
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

    public static Multimap<String, String> getRepresentativeToLocales() {
        return representativeForLocales;
    }

    public static Multimap<String, String> getCategorySetToRepresentativeLocales() {
        return categorySetToRepresentativeLocales;
    }

    public String getRepresentativeLocaleForPluralRules(String sourceLocale) {
        String result = localeToRepresentative.get(sourceLocale);
        return result == null ? "und" : result;
    }

    private static final Map<String, String> localeToRepresentative;
    private static final Multimap<String, String> representativeForLocales;
    private static final Multimap<String, String> categorySetToRepresentativeLocales;

    static {
        Map<String, String> _localeToRepresentative = new TreeMap<>();
        Multimap<String, String> _representativeForLocales = TreeMultimap.create();
        TreeMultimap<String, String> _categorySetToLocales = TreeMultimap.create();

        Multimap<PluralRules, String> rulesToLocales = LinkedHashMultimap.create();
        CLDRConfig testInfo = CLDRConfig.getInstance();
        SupplementalDataInfo supp = testInfo.getSupplementalDataInfo();
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
                            String first = set.iterator().next();

                            set.stream().forEach(y -> _localeToRepresentative.put(y, first));
                            _representativeForLocales.putAll(first, set);
                            PluralInfo pluralInfo = supp.getPlurals(first);
                            PluralRules rules = pluralInfo.getPluralRules();
                            Set<String> categories = rules.getKeywords();
                            _categorySetToLocales.put(Joiners.SP.join(categories), first);
                        });
        localeToRepresentative = ImmutableMap.copyOf(_localeToRepresentative);
        representativeForLocales = ImmutableMultimap.copyOf(_representativeForLocales);
        categorySetToRepresentativeLocales = ImmutableMultimap.copyOf(_categorySetToLocales);
    }
}
