package org.unicode.cldr.draft;

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

    private static final double DEFAULT_THRESHOLD = 0.5;
    private static final int DEMOTION_PER_ADDITIONAL_USER_LANGUAGE = 5;

    // normally the default values, but can be set via constructor

    private final XLikelySubtags likelySubtags;
    private final XLocaleDistance localeDistance;
    private final int threshold;

    // built based on application's supported languages in constructor

    private final Map<LSR, Collection<ULocale>> supportedLanguages; // the locales in the collection are ordered!
    private final Set<ULocale> exactSupportedLocales; // the locales in the collection are ordered!
    private final ULocale defaultLanguage; 


    public XLocaleMatcher(String languagePriorityList) {
        this(LocalePriorityList.add(languagePriorityList).build(), XLocaleDistance.getDefault(), DEFAULT_THRESHOLD, XLikelySubtags.getDefault());
    }

    public XLocaleMatcher(LocalePriorityList languagePriorityList) {
        this(languagePriorityList, XLocaleDistance.getDefault(), DEFAULT_THRESHOLD, XLikelySubtags.getDefault());
    }

    public XLocaleMatcher(LocalePriorityList languagePriorityList, XLocaleDistance localeDistance) {
        this(languagePriorityList, localeDistance, DEFAULT_THRESHOLD, XLikelySubtags.getDefault());
    }

    public XLocaleMatcher(LocalePriorityList languagePriorityList, XLocaleDistance localeDistance, double thresholdMatch) {
        this(languagePriorityList, localeDistance, DEFAULT_THRESHOLD, XLikelySubtags.getDefault());
    }

    public XLocaleMatcher(LocalePriorityList languagePriorityList, XLocaleDistance localeDistance, double thresholdMatch, XLikelySubtags likelySubtags) {
        this.localeDistance = localeDistance;
        this.threshold = (int)(100*(1-thresholdMatch));
        this.likelySubtags = likelySubtags;
        // only do after above are set
        final Multimap<LSR, ULocale> temp2 = extractLsrMap(languagePriorityList);
        supportedLanguages = temp2.asMap();
        exactSupportedLocales = ImmutableSet.copyOf(temp2.values());
        defaultLanguage = supportedLanguages.isEmpty() ? null : supportedLanguages.entrySet().iterator().next().getValue().iterator().next(); // first language
    }

    private Set<LSR> extractLsrSet(LocalePriorityList languagePriorityList) {
        Set<LSR> builder = new LinkedHashSet<>();
        for (ULocale item : languagePriorityList) {
            builder.add(item.equals(UND_LOCALE) ? UND : likelySubtags.addLikelySubtags(item));
        }
        return ImmutableSet.copyOf(builder);
    }

    private Multimap<LSR,ULocale> extractLsrMap(LocalePriorityList languagePriorityList) {
        Multimap<LSR, ULocale> builder = LinkedHashMultimap.create();
        for (ULocale item : languagePriorityList) {
            LSR canonical = LSR.canonicalize(item);
            final LSR max = item.equals(UND_LOCALE) ? UND : likelySubtags.addLikelySubtags(canonical);
            builder.put(max, item);
        }
        return ImmutableMultimap.copyOf(builder);
    }

    public ULocale getBestMatch(LocalePriorityList desiredLanguages) {
        return getBestMatch(desiredLanguages, null);
    }
    
    public ULocale getBestMatch(LocalePriorityList desiredLanguages, Output<ULocale> outputBestDesired) {
        // TODO produce optimized version for single desired ULocale
        Multimap<LSR, ULocale> desiredLSRs = extractLsrMap(desiredLanguages);
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
                    int distance = delta + localeDistance.distance(desiredLSR, supportedLsrAndLocale.getKey());
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestDesiredLocale = desiredLocale;
                        bestSupportedLocales = supportedLsrAndLocale.getValue();
                        if (distance == 0) {
                            break mainLoop;
                        }
                    }
                }
                delta += DEMOTION_PER_ADDITIONAL_USER_LANGUAGE;
            }
        if (bestDistance > threshold) {
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

    public ULocale getBestMatch(String languageList) {
        return getBestMatch(LocalePriorityList.add(languageList).build());
    }
    public ULocale getBestMatch(ULocale... locales) {
        return getBestMatch(LocalePriorityList.add(locales).build());
    }
    public ULocale getBestMatch(ULocale locale) {
        return getBestMatch(LocalePriorityList.add(locale).build());
    }

    public double match(ULocale desired, ULocale supported) {
        return (100-distance(desired, supported))/100.0;
    }

    public double distance(ULocale desired, ULocale supported) {
        return localeDistance.distance(likelySubtags.addLikelySubtags(desired), likelySubtags.addLikelySubtags(supported));
    }

    public double distance(String desired, String supported) {
        return localeDistance.distance(likelySubtags.addLikelySubtags(new ULocale(desired)), likelySubtags.addLikelySubtags(new ULocale(supported)));
    }

    @Override
    public String toString() {
        return exactSupportedLocales.toString();
    }
}
