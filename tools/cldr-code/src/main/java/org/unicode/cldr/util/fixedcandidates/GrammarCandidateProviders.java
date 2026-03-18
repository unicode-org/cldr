package org.unicode.cldr.util.fixedcandidates;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.GrammarInfo;
import org.unicode.cldr.util.GrammarInfo.GrammaticalFeature;
import org.unicode.cldr.util.GrammarInfo.GrammaticalScope;
import org.unicode.cldr.util.GrammarInfo.GrammaticalTarget;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.SupplementalDataInfo;

class GrammarCandidateProviders {
    private static final String XPATH_PATTERN = "^//ldml/units/unitLength.*/unit.*gender";
    static final SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
    static final Pattern pattern = PatternCache.get(XPATH_PATTERN);

    /** get the provider - or null if not applicable for this locale. */
    public static final FixedCandidateProvider forLocale(final CLDRLocale locale) {
        final GrammarInfo grammarInfo = sdi.getGrammarInfo(locale.getBaseName());
        if (grammarInfo == null) {
            // if no grammar info, still have no candidates for this
            return FixedCandidateProvider.forXPathPattern(pattern, Collections.emptyList());
        }
        final Collection<String> candidates =
                grammarInfo.get(
                        GrammaticalTarget.nominal,
                        GrammaticalFeature.grammaticalGender,
                        GrammaticalScope.units);
        return FixedCandidateProvider.forXPathPattern(pattern, candidates);
    }
}
