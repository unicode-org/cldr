package org.unicode.cldr.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ibm.icu.text.PluralRules;

public class PluralRulesUtil {
    /**
     * Status of the keyword for the rules, given a set of explicit values.
     */
    public enum KeywordStatus {
        /**
         * The keyword is not valid for the rules.
         */
        INVALID,
        /**
         * The keyword is valid, but unused (it is covered by the explicit values).
         */
        SUPPRESSED,
        /**
         * The keyword is valid and used, but completely covered by the explicit values.
         */
        UNIQUE,
        /**
         * The keyword is valid, used, not suppressed, and has a finite set of values.
         */
        BOUNDED,
        /**
         * The keyword is valid but not bounded; there are indefinitely many matching values.
         */
        UNBOUNDED
    }

    /**
     * Find the status for the keyword, given a certain set of explicit values.
     *
     * @param rules
     *            the PluralRules
     * @param keyword
     *            the particular keyword (call rules.getKeywords() to get the valid ones)
     * @param offset
     *            the offset used, or 0.0d if not. Internally, the offset is subtracted from each explicit value before
     *            checking against the keyword values.
     * @param explicits
     *            a set of Doubles that are used explicitly (eg [=0], "[=1]"). May be empty or null.
     * @param integerOnly
     *            In circumstances where the values are known to be integers, this parameter can be set to true.
     *            Examples: "There are 3 people in..." (integerOnly=true) vs. "There are 1.2 people per household
     *            (integerOnly=false).
     *            This may produce different results in languages where fractions have the same format as integers for
     *            some keywords.
     * @return the KeywordStatus
     *         <p>
     *         NOTE: For testing, this is a static with the first parameter being the rules. Those will disappear.
     */
    public static KeywordStatus getKeywordStatus(PluralRules rules, String keyword, int offset, Set<Double> explicits,
        boolean integerOnly) {
        if (!rules.getKeywords().contains(keyword)) {
            return KeywordStatus.INVALID;
        }
        Collection<Double> values = rules.getAllKeywordValues(keyword);
        if (values == null) {
            return KeywordStatus.UNBOUNDED;
        }
        int originalSize = values.size();

        // Quick check on whether there are multiple elements

        if (explicits == null) {
            explicits = Collections.emptySet();
        }
        if (originalSize > explicits.size()) {
            return originalSize == 1 ? KeywordStatus.UNIQUE : KeywordStatus.BOUNDED;
        }

        // Compute if the quick test is insufficient.

        HashSet<Double> subtractedSet = new HashSet<Double>(values);
        for (Double explicit : explicits) {
            // int rounded = (int) Math.round(explicit*1000000);
            subtractedSet.remove(explicit - offset);
        }
        if (subtractedSet.size() == 0) {
            return KeywordStatus.SUPPRESSED;
        }

        return originalSize == 1 ? KeywordStatus.UNIQUE : KeywordStatus.BOUNDED;
    }

    // static final Map<String,Set<String>> locale2keywords = new HashMap<String,Set<String>>();
    // static final Map<String,PluralRules> locale2pluralRules = new HashMap<String,PluralRules>();
    // static final Set<Double> explicits = new HashSet<Double>();
    // static {
    // explicits.add(0.0d);
    // explicits.add(1.0d);
    // }
    // public static Set<String> getCanonicalKeywords(String locale) {
    // synchronized (locale2keywords) {
    // Set<String> result = locale2keywords.get(locale);
    // if (result != null) {
    // return result;
    // }
    // // special caching because locales don't differ
    // int pos = locale.indexOf('_');
    // String lang = pos < 0 ? locale : locale.substring(0,pos);
    // if (pos >= 0) {
    // result = locale2keywords.get(locale);
    // if (result != null) {
    // locale2keywords.put(locale, result);
    // return result;
    // }
    // }
    // PluralInfo pluralInfo = SupplementalDataInfo.getInstance().getPlurals(SupplementalDataInfo.PluralType.cardinal,
    // lang);
    // PluralRules pluralRules = PluralRules.createRules(pluralInfo.getRules());
    // locale2pluralRules.put(lang, pluralRules);
    // result = new HashSet();
    // for (String keyword : pluralRules.getKeywords()) {
    // KeywordStatus status = getKeywordStatus(pluralRules, keyword, 0, explicits, true);
    // if (status != KeywordStatus.SUPPRESSED) {
    // result.add(keyword);
    // }
    // }
    // result = Collections.unmodifiableSet(result);
    // locale2keywords.put(locale, result);
    // if (pos >= 0) {
    // locale2keywords.put(lang, result);
    // }
    // return result;
    // }
    //
    // }
}
