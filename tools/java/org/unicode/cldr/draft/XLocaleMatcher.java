package org.unicode.cldr.draft;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.draft.XLikelySubtags.LSR;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.ibm.icu.util.LocalePriorityList;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

/**
 * Immutable class that picks best match between user's desired locales and application's supported locales.
 * @author markdavis
 */
public class XLocaleMatcher {
    private static final LSR UND = new LSR("und","","");
    private static final ULocale UND_LOCALE = new ULocale("und");

    // normally the default values, but can be set via constructor

    private final XLocaleDistance localeDistance;
    private final int thresholdDistance;
    private final int demotionPerAdditionalUserLanguage = 5;

    // built based on application's supported languages in constructor

    private final Map<LSR, Collection<ULocale>> supportedLanguages; // the locales in the collection are ordered!
    private final Set<ULocale> exactSupportedLocales; // the locales in the collection are ordered!
    private final ULocale defaultLanguage; 


    /** Convenience method */
    public XLocaleMatcher(String languagePriorityList) {
        this(asSet(LocalePriorityList.add(languagePriorityList).build()), XLocaleDistance.getDefault().getDefaultScriptDistance(), 5);
    }
    /** Convenience method */
    public XLocaleMatcher(LocalePriorityList languagePriorityList) {
        this(asSet(languagePriorityList), XLocaleDistance.getDefault().getDefaultScriptDistance(), 5);
    }
    /** Convenience method */
    public XLocaleMatcher(Set<ULocale> languagePriorityList) {
        this(languagePriorityList, XLocaleDistance.getDefault().getDefaultScriptDistance(), 5);
    }
    /** Convenience method */
    public XLocaleMatcher(Set<ULocale> languagePriorityList, int thresholdDistance) {
        this(languagePriorityList, thresholdDistance, 5);
    }
    /** Convenience method */
    public XLocaleMatcher(String languagePriorityList, int thresholdDistance) {
        this(asSet(LocalePriorityList.add(languagePriorityList).build()), thresholdDistance, 5);
    }
    /** Convenience method */
    public XLocaleMatcher(Set<ULocale> languagePriorityList, int thresholdDistance, int demotionPerAdditionalDesiredLocale) {
        this(languagePriorityList, thresholdDistance, demotionPerAdditionalDesiredLocale, XLocaleDistance.getDefault());
    }

    /**
     * Create a locale matcher with the given parameters.
     * @param languagePriorityList
     * @param thresholdDistance
     * @param demotionPerAdditionalDesiredLocale
     * @param localeDistance
     * @param likelySubtags
     */
    public XLocaleMatcher(Set<ULocale> languagePriorityList, int thresholdDistance, 
        int demotionPerAdditionalDesiredLocale, XLocaleDistance localeDistance) {
        this.localeDistance = localeDistance;
        this.thresholdDistance = thresholdDistance;
        // only do after above are set
        Set<LSR> paradigms = extractLsrSet(localeDistance.getParadigms());
        final Multimap<LSR, ULocale> temp2 = extractLsrMap(languagePriorityList, paradigms);
        supportedLanguages = temp2.asMap();
        exactSupportedLocales = ImmutableSet.copyOf(temp2.values());
        defaultLanguage = supportedLanguages.isEmpty() ? null : supportedLanguages.entrySet().iterator().next().getValue().iterator().next(); // first language
    }

    // Result is not immutable!
    private Set<LSR> extractLsrSet(Set<ULocale> languagePriorityList) {
        Set<LSR> result = new LinkedHashSet<>();
        for (ULocale item : languagePriorityList) {
            final LSR max = item.equals(UND_LOCALE) ? UND : LSR.fromMaximalized(item);
            result.add(max);
        }
        return result;
    }
    private Multimap<LSR,ULocale> extractLsrMap(Set<ULocale> languagePriorityList, Set<LSR> priorities) {
        Multimap<LSR, ULocale> builder = LinkedHashMultimap.create();
        for (ULocale item : languagePriorityList) {
            final LSR max = item.equals(UND_LOCALE) ? UND : LSR.fromMaximalized(item);
            builder.put(max, item);
        }
        if (builder.size() > 1 && priorities != null) {
            // for the supported list, we put any priorities before all others, except for the first.
            Multimap<LSR, ULocale> builder2 = LinkedHashMultimap.create();

            // copy the long way so the priorities are in the same order as in the original
            boolean first = true;
            for (Entry<LSR, Collection<ULocale>> entry : builder.asMap().entrySet()) {
                final LSR key = entry.getKey();
                if (first || priorities.contains(key)) {
                    builder2.putAll(key, entry.getValue());
                    first = false;
                }
            }
            // now copy the rest
            builder2.putAll(builder);
            if (!builder2.equals(builder)) {
                throw new IllegalArgumentException();
            }
            builder = builder2;
        }
        return ImmutableMultimap.copyOf(builder);
    }


    /** Convenience method */
    public ULocale getBestMatch(ULocale ulocale) {
        return getBestMatch(ulocale, null);
    }
    /** Convenience method */
    public ULocale getBestMatch(String languageList) {
        return getBestMatch(LocalePriorityList.add(languageList).build(), null);
    }
    /** Convenience method */
    public ULocale getBestMatch(ULocale... locales) {
        return getBestMatch(new LinkedHashSet<>(Arrays.asList(locales)), null);
    }
    /** Convenience method */
    public ULocale getBestMatch(Set<ULocale> desiredLanguages) {
        return getBestMatch(desiredLanguages, null);
    }
    /** Convenience method */
    public ULocale getBestMatch(LocalePriorityList languageList) {
        return getBestMatch(languageList, null);
    }
    /** Convenience method */
    public ULocale getBestMatch(LocalePriorityList languageList, Output<ULocale> outputBestDesired) {
        return getBestMatch(asSet(languageList), outputBestDesired);
    }

    // TODO add LocalePriorityList method asSet() for ordered Set view backed by LocalePriorityList
    private static Set<ULocale> asSet(LocalePriorityList languageList) {
        Set<ULocale> temp = new LinkedHashSet<>(); // maintain order
        for (ULocale locale : languageList) {
            temp.add(locale);
        };
        return temp;
    }

    /** 
     * Get the best match between the desired languages and supported languages
     * @param desiredLanguages Typically the supplied user's languages, in order of preference, with best first.
     * @param outputBestDesired The one of the desired languages that matched best.
     * Set to null if the best match was not below the threshold distance.
     * @return
     */
    public ULocale getBestMatch(Set<ULocale> desiredLanguages, Output<ULocale> outputBestDesired) {
        // fast path for singleton
        if (desiredLanguages.size() == 1) {
            return getBestMatch(desiredLanguages.iterator().next(), outputBestDesired);
        }
        // TODO produce optimized version for single desired ULocale
        Multimap<LSR, ULocale> desiredLSRs = extractLsrMap(desiredLanguages,null);
        int bestDistance = Integer.MAX_VALUE;
        ULocale bestDesiredLocale = null;
        Collection<ULocale> bestSupportedLocales = null;
        int delta = 0;
        mainLoop:
            for (final Entry<LSR, ULocale> desiredLsrAndLocale : desiredLSRs.entries()) {
                // quick check for exact match
                ULocale desiredLocale = desiredLsrAndLocale.getValue();
                LSR desiredLSR = desiredLsrAndLocale.getKey();
                if (delta < bestDistance) {
                    if (exactSupportedLocales.contains(desiredLocale)) {
                        if (outputBestDesired != null) {
                            outputBestDesired.value = desiredLocale;
                        }
                        return desiredLocale;
                    }
                    // quick check for maximized locale
                    Collection<ULocale> found = supportedLanguages.get(desiredLSR);
                    if (found != null) {
                        // if we find one in the set, return first (lowest). We already know the exact one isn't there.
                        if (outputBestDesired != null) {
                            outputBestDesired.value = desiredLocale;
                        }
                        return found.iterator().next();
                    }
                }
                for (final Entry<LSR, Collection<ULocale>> supportedLsrAndLocale : supportedLanguages.entrySet()) {
                    int distance = delta + localeDistance.distance(desiredLSR, supportedLsrAndLocale.getKey(), thresholdDistance);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestDesiredLocale = desiredLocale;
                        bestSupportedLocales = supportedLsrAndLocale.getValue();
                        if (distance == 0) {
                            break mainLoop;
                        }
                    }
                }
                delta += demotionPerAdditionalUserLanguage;
            }
        if (bestDistance >= thresholdDistance) {
            if (outputBestDesired != null) {
                outputBestDesired.value = null;
            }
            return defaultLanguage;
        }
        if (outputBestDesired != null) {
            outputBestDesired.value = bestDesiredLocale;
        }
        // pick exact match if there is one
        if (bestSupportedLocales.contains(bestDesiredLocale)) {
            return bestDesiredLocale;
        }
        // otherwise return first supported, combining variants and extensions from bestDesired
        return bestSupportedLocales.iterator().next();
    }

    /** 
     * Get the best match between the desired languages and supported languages
     * @param desiredLanguages Typically the supplied user's languages, in order of preference, with best first.
     * @param outputBestDesired The one of the desired languages that matched best.
     * Set to null if the best match was not below the threshold distance.
     * @return
     */
    public ULocale getBestMatch(ULocale desiredLocale, Output<ULocale> outputBestDesired) {
        int bestDistance = Integer.MAX_VALUE;
        ULocale bestDesiredLocale = null;
        Collection<ULocale> bestSupportedLocales = null;
        
        // quick check for exact match, with hack for und
        final LSR desiredLSR = desiredLocale.equals(UND_LOCALE) ? UND : LSR.fromMaximalized(desiredLocale);

        if (exactSupportedLocales.contains(desiredLocale)) {
            if (outputBestDesired != null) {
                outputBestDesired.value = desiredLocale;
            }
            return desiredLocale;
        }
        // quick check for maximized locale
        Collection<ULocale> found = supportedLanguages.get(desiredLSR);
        if (found != null) {
            // if we find one in the set, return first (lowest). We already know the exact one isn't there.
            if (outputBestDesired != null) {
                outputBestDesired.value = desiredLocale;
            }
            return found.iterator().next();
        }
        for (final Entry<LSR, Collection<ULocale>> supportedLsrAndLocale : supportedLanguages.entrySet()) {
            int distance = localeDistance.distance(desiredLSR, supportedLsrAndLocale.getKey(), thresholdDistance);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestDesiredLocale = desiredLocale;
                bestSupportedLocales = supportedLsrAndLocale.getValue();
                if (distance == 0) {
                    break;
                }
            }
        }
        if (bestDistance >= thresholdDistance) {
            if (outputBestDesired != null) {
                outputBestDesired.value = null;
            }
            return defaultLanguage;
        }
        if (outputBestDesired != null) {
            outputBestDesired.value = bestDesiredLocale;
        }
        // pick exact match if there is one
        if (bestSupportedLocales.contains(bestDesiredLocale)) {
            return bestDesiredLocale;
        }
        // otherwise return first supported, combining variants and extensions from bestDesired
        return bestSupportedLocales.iterator().next();
    }

    /** Combine features of the desired locale into those of the supported, and return result. */
    public static ULocale combine(ULocale bestSupported, ULocale bestDesired) {
        // for examples of extensions, variants, see
        //  http://unicode.org/repos/cldr/tags/latest/common/bcp47/
        //  http://unicode.org/repos/cldr/tags/latest/common/validity/variant.xml

        if (!bestSupported.equals(bestDesired)) {
            // add region, variants, extensions
            ULocale.Builder b = new ULocale.Builder().setLocale(bestSupported);

            // copy the region from the desired, if there is one
            String region = bestDesired.getCountry();
            if (!region.isEmpty()) {
                b.setRegion(region);
            }

            // copy the variants from desired, if there is one
            // note that this will override any subvariants. Eg "sco-ulster-fonipa" + "…-fonupa" => "sco-fonupa" (nuking ulster)
            String variants = bestDesired.getVariant();
            if (!variants.isEmpty()) {
                b.setVariant(variants);
            }

            // copy the extensions from desired, if there are any
            // note that this will override any subkeys. Eg "th-u-nu-latn-ca-buddhist" + "…-u-nu-native" => "th-u-nu-native" (nuking calendar)
            for (char extensionKey : bestDesired.getExtensionKeys()) {
                b.setExtension(extensionKey, bestDesired.getExtension(extensionKey));
            }
            bestSupported = b.build();
        }
        return bestSupported;
    }

    /** Returns the distance between the two languages. The values are not necessarily symmetric.
     * @param desired
     * @param supported
     * @return A return of 0 is a complete match, and 100 is a failure case (above the thresholdDistance).
     * A language is first maximized with add likely subtags, then compared.
     */
    public int distance(ULocale desired, ULocale supported) {
        return localeDistance.distance(
            LSR.fromMaximalized(desired), 
            LSR.fromMaximalized(supported), thresholdDistance);
    }

    /** Convenience method */
    public int distance(String desiredLanguage, String supportedLanguage) {
        return localeDistance.distance(
            LSR.fromMaximalized(new ULocale(desiredLanguage)), 
            LSR.fromMaximalized(new ULocale(supportedLanguage)), 
            thresholdDistance);
    }

    @Override
    public String toString() {
        return exactSupportedLocales.toString();
    }

    /** Return the inverse of the distance: that is, 1-distance(desired, supported) */
    public double match(ULocale desired, ULocale supported) {
        return (100-distance(desired, supported))/100.0;
    }

    /**
     * Returns a fraction between 0 and 1, where 1 means that the languages are a
     * perfect match, and 0 means that they are completely different. This is (100-distance(desired, supported))/100.0.
     * <br>Note that
     * the precise values may change over time; no code should be made dependent
     * on the values remaining constant.
     * @param desired Desired locale
     * @param desiredMax Maximized locale (using likely subtags)
     * @param supported Supported locale
     * @param supportedMax Maximized locale (using likely subtags)
     * @return value between 0 and 1, inclusive.
     * @deprecated Use the form with 2 parameters instead.
     */
    public double match(ULocale desired, ULocale desiredMax, ULocale supported, ULocale supportedMax) {
        return match(desired, supported);
    }

    /**
     * Canonicalize a locale (language). Note that for now, it is canonicalizing
     * according to CLDR conventions (he vs iw, etc), since that is what is needed
     * for likelySubtags.
     * @param ulocale language/locale code
     * @return ULocale with remapped subtags.
     * @stable ICU 4.4
     */
    public ULocale canonicalize(ULocale ulocale) {
        // TODO
        return null;
    }

    /**
     * @return the thresholdDistance. Any distance above this value is treated as a match failure.
     */
    public int getThresholdDistance() {
        return thresholdDistance;
    }
}
