package org.unicode.cldr.util.fixedcandidates;

import java.util.LinkedList;
import java.util.List;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.fixedcandidates.FixedCandidateProvider.CompoundFixedCandidateProvider;

/** This class encapsulates all default fixed candidate providers. */
public class DefaultFixedCandidates {
    /**
     * Call this to get a compound provider for the locale. Does not cache internally, as it is
     * expected that the caller will cache this with per-locale data.
     */
    public static final FixedCandidateProvider forLocale(final CLDRLocale locale) {
        final List<FixedCandidateProvider> providers = new LinkedList<>();
        // add all providers here.

        // add any lists of fixed providers
        providers.addAll(PersonNameCandidateProviders.personNameProviders);
        providers.add(
                FixedCandidateProvider.forXPathPattern(
                        "//ldml/numbers/rationalFormats\\[@numberSystem=\"[^]]*\"\\]/rationalUsage",
                        List.of("never", "sometimes")));

        // add any special locale-sensitive providers.

        FixedCandidateProvider grammarProvider = GrammarCandidateProviders.forLocale(locale);
        providers.add(grammarProvider);

        // end of providers
        return new CompoundFixedCandidateProvider(providers);
    }
}
