package org.unicode.cldr.draft;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.draft.XLikelySubtags.LSR;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.ibm.icu.util.LocalePriorityList;
import com.ibm.icu.util.ULocale;

public class XLocaleMatcher {
    private final Map<LSR, Collection<ULocale>> supportedLanguages; // ordered!
    private final Set<ULocale> exactSupportedLocales; // ordered!
    private final XLocaleDistance localeDistance;
    private final int threshold;
    private final XLikelySubtags likelySubtags = XLikelySubtags.getDefault();
    private final ULocale defaultLanguage;
    private final LSR UND = new LSR("und","","");
    private final ULocale UND_LOCALE = new ULocale("und","","");

    private static final double DEFAULT_THRESHOLD = 0.5;

    public XLocaleMatcher(String languagePriorityList) {
        this(LocalePriorityList.add(languagePriorityList).build(), XLocaleDistance.getDefault());
    }

    public XLocaleMatcher(LocalePriorityList languagePriorityList) {
        this(languagePriorityList, XLocaleDistance.getDefault());
    }

    public XLocaleMatcher(LocalePriorityList languagePriorityList, XLocaleDistance localeDistance) {
        this(languagePriorityList, localeDistance, DEFAULT_THRESHOLD);
    }

    public XLocaleMatcher(LocalePriorityList languagePriorityList, XLocaleDistance localeDistance, double threshold) {
        final Multimap<LSR, ULocale> temp2 = extractLsrMap(languagePriorityList);
        supportedLanguages = temp2.asMap();
        exactSupportedLocales = ImmutableSet.copyOf(temp2.values());
        defaultLanguage = supportedLanguages.isEmpty() ? null : supportedLanguages.entrySet().iterator().next().getValue().iterator().next(); // first language
        this.localeDistance = localeDistance;
        this.threshold = (int)(100*(1-threshold));
    }

    private Set<LSR> extractLsrSet(LocalePriorityList languagePriorityList) {
        Set<LSR> builder = new LinkedHashSet<>();
        for (ULocale item : languagePriorityList) {
            builder.add(item.equals(UND_LOCALE) ? UND : likelySubtags.addLikelySubtags(item));
        }
        return ImmutableSet.copyOf(builder);
    }

    private Multimap<LSR,ULocale> extractLsrMap(LocalePriorityList languagePriorityList) {
        HashMultimap<LSR, ULocale> builder = HashMultimap.create();
        for (ULocale item : languagePriorityList) {
            final LSR max = item.equals(UND_LOCALE) ? UND : likelySubtags.addLikelySubtags(item);
            builder.put(max, item);
        }
        return ImmutableMultimap.copyOf(builder);
    }

    public ULocale getBestMatch(LocalePriorityList desiredLanguages) {
        Multimap<LSR, ULocale> desiredLSRs = extractLsrMap(desiredLanguages);
        int bestDistance = Integer.MAX_VALUE;
        ULocale bestDesiredLocale = null;
        Collection<ULocale> bestSupportedLocales = null;
        mainLoop:
            for (final ULocale desiredLocale : desiredLanguages) {
                // quick check for exact match
                if (exactSupportedLocales.contains(desiredLocale)) {
                    return desiredLocale;
                }
                LSR desiredLSR = likelySubtags.addLikelySubtags(desiredLocale);
                // quick check for maximized locale
                Collection<ULocale> found = supportedLanguages.get(desiredLSR);
                if (found != null) {
                    return found.iterator().next(); // if so, return first (lowest). We already know the exact one isn't there.
                }
                for (final Entry<LSR, Collection<ULocale>> supportedLsrAndLocale : supportedLanguages.entrySet()) {
                    int distance = localeDistance.distance(desiredLSR, supportedLsrAndLocale.getKey());
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestDesiredLocale = desiredLocale;
                        bestSupportedLocales = supportedLsrAndLocale.getValue();
                        if (distance == 0) {
                            break mainLoop;
                        }
                    }
                }
            }
        if (bestDistance > threshold) {
            return defaultLanguage;
        }
        // pick exact match if there is one
        if (bestSupportedLocales.contains(bestDesiredLocale)) {
            return bestDesiredLocale;
        }
        // otherwise return first supported
        return bestSupportedLocales.iterator().next();
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
}
