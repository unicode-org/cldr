package org.unicode.cldr.util;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.StandardCodes.LstrType;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Row.R2;

/**
 * Provides Unicode Language Identifier canonicalization for use in testing.
 * The implementation is designed to be simple, and is not at all optimized for production use.
 * It is used to verify the correctness of the specification algorithm,
 * sanity-check the supplementalMetadata.xml alias data,
 * and generate test files for use by implementations.
 */
public class LsrvCanonicalizer {

    public static final Set<LstrType> LSRV = ImmutableSet.of(LstrType.language, LstrType.script, LstrType.region, LstrType.variant);
    public static final Joiner UNDERBAR_JOINER = Joiner.on('_');

    /**
     * A representation of a Unicode Language Identifier in a format that makes it simple to process.
     * The LSRV fields are represented as multimaps, though the LSR fields restricted to have only have 0 or 1 element.
     */
    public static class XLanguageTag {
        final Multimap<LstrType, String> data;

        private XLanguageTag(Multimap<LstrType, String> result) {
            data = ImmutableMultimap.copyOf(result);
        }
        public Set<LstrType> keys() {
            return data.keySet();
        }
        public Collection<String> get(LstrType lstrType) {
            return data.get(lstrType);
        }
        public String toLocaleString() {
            StringBuilder buffer = new StringBuilder();
            final Collection<String> region = data.get(LstrType.language);
            if (!region.isEmpty()) {
                buffer.append(UNDERBAR_JOINER.join(region));
            } else {
                buffer.append("und");
            }
            addItem(buffer, LstrType.script, "", "_", UNDERBAR_JOINER);
            addItem(buffer, LstrType.region, "", "_", UNDERBAR_JOINER);
            addItem(buffer, LstrType.variant, "", "_", UNDERBAR_JOINER);

            return buffer.toString();
        }
        @Override
        public String toString() {
            StringBuilder buffer = new StringBuilder();
            addItem(buffer, LstrType.language, "", "L:", UNDERBAR_JOINER);
            addItem(buffer, LstrType.script, ";", "S:", UNDERBAR_JOINER);
            addItem(buffer, LstrType.region, ";", "R:", UNDERBAR_JOINER);
            addItem(buffer, LstrType.variant, ";", "V:", UNDERBAR_JOINER);
            return buffer.toString();
        }

        public void addItem(StringBuilder buffer, LstrType lstrType, String separator, String prefix, final Joiner dashJoiner) {
            final Collection<String> region = data.get(lstrType);
            if (!region.isEmpty()) {
                if (buffer.length() > 0) {
                    buffer.append(separator);
                }
                buffer.append(prefix).append(dashJoiner.join(region));
            }
        }

        public static XLanguageTag fromTag(LstrType lstrType, String tag) {
            Multimap<LstrType,String> result = TreeMultimap.create();
            LanguageTagParser source = new LanguageTagParser();
            final boolean isLanguage = lstrType == LstrType.language;
            String prefix = isLanguage ? "" : "und_";
            try {
                source.set(prefix + tag);
            } catch (Exception e) {
                return null;  // skip ill-formed for now
//                if (lstrType == LstrType.region && tag.length() == 3) {
//                    //result.put(LstrType.language, "und");
//                    result.put(LstrType.region, tag);
//                } else {
//                    result.put(LstrType.language, tag);
//                }
//                //System.out.println("ILLEGAL SOURCE\t" + lstrType + ":\t" + tag + " ⇒ " + result); // for debugging
//                return new XLanguageTag(result);
            }
            if (!source.getLanguage().isEmpty()
                && !source.getLanguage().contains("und")) {
                result.put(LstrType.language, source.getLanguage());
            }
            if (!source.getScript().isEmpty()) {
                result.put(LstrType.script, source.getScript());
            }
            if (!source.getRegion().isEmpty()) {
                result.put(LstrType.region, source.getRegion());
            }
            if (!source.getVariants().isEmpty()) {
                result.putAll(LstrType.variant, source.getVariants());
            }
            return new XLanguageTag(result);
        }
        @Override
        public boolean equals(Object obj) {
            return data.equals(((XLanguageTag)obj).data);
        }
        @Override
        public int hashCode() {
            return data.hashCode();
        }
        public XLanguageTag set(LstrType lstrType, String string) {
            Multimap<LstrType,String> result = TreeMultimap.create(data);
            if (lstrType != LstrType.variant) {
                result.removeAll(lstrType);
            }
            result.put(lstrType, string);
            return new XLanguageTag(result);
        }

        /**
         * containsAll is used in matching a ReplacementRule.<br>
         * It is here instead of on ReplacementRule so we can use in the denormalization utility used in testing.
         */
        public boolean containsAll(XLanguageTag type) {
            for (LstrType lstrType : LSRV) {
                final Collection<String> sources = get(lstrType);
                final Collection<String> types = type.get(lstrType);
                if (!sources.containsAll(types)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Once a rule matches, this actually does the replacement.<br>
         * It is here instead of on ReplacementRule so we can use it in the denormalization utility used in testing.
         */
        public XLanguageTag replacePartsFrom(XLanguageTag typeParts, XLanguageTag replacementParts) {
            Multimap<LstrType,String> result = TreeMultimap.create();
            for (LstrType lstrType : LSRV) {
                Collection<String> sources = get(lstrType);
                Collection<String> types = typeParts.get(lstrType);
                Collection<String> replacements = replacementParts.get(lstrType);
                result.putAll(lstrType, sources);
                if (!types.isEmpty() && !replacements.isEmpty()) {
                    removeAll(result, lstrType, types);
                    result.putAll(lstrType, replacements);
                } else if (!types.isEmpty() && replacements.isEmpty()) {
                    removeAll(result, lstrType, types);
                } else if (types.isEmpty() && !replacements.isEmpty()) {
                    if (sources.isEmpty()) {
                        result.putAll(lstrType, replacements);
                    }
                } else {
                    // otherwise both empty, skip
                }
            }
            return new XLanguageTag(result);
        }
    }

    /**
     * A representation of the alias data for Unicode Language Identifiers in the supplementalMetadata.txt file.
     */

    public static class ReplacementRule implements Comparable<ReplacementRule> {
        private final XLanguageTag typeParts;
        final XLanguageTag replacementParts;
        final List<XLanguageTag> secondaryReplacementSet; // TODO, using this information in special cases to impute the best language according to LDML
        final String reason;
        final boolean regular;

        private ReplacementRule(LstrType lstrType, String type, XLanguageTag typeParts, XLanguageTag replacementParts,
            List<XLanguageTag> secondaryReplacementSet, String reason) {
            this.typeParts = typeParts;
            this.replacementParts = replacementParts;
            this.secondaryReplacementSet = secondaryReplacementSet;
            this.reason = reason;
            this.regular = typeParts.keys().equals(replacementParts.keys()) &&
                typeParts.get(LstrType.variant).size() == replacementParts.get(LstrType.variant).size();
        }

        static ReplacementRule from(LstrType lstrType, String type, List<String> replacement, String reason) {
            XLanguageTag typeParts = XLanguageTag.fromTag(lstrType, type);
            if (typeParts == null) {
                return null; // skip ill-formed for now
            }
            XLanguageTag replacementParts = XLanguageTag.fromTag(lstrType, replacement.get(0));
            if (replacementParts == null) {
                return null; // skip ill-formed for now
            }
            List<XLanguageTag> secondaryReplacementSet = new ArrayList<>();
            for (int i = 1; i < replacement.size(); ++i) {
                secondaryReplacementSet.add(XLanguageTag.fromTag(lstrType, replacement.get(i)));
            }
            return new ReplacementRule(lstrType, type, typeParts, replacementParts, secondaryReplacementSet, reason);
        }

        @Override
        public int compareTo(ReplacementRule o) {
            return ComparisonChain.start()
                .compare(-getType().keys().size(), -o.getType().keys().size()) // sort most keys first
                .compare(getType().toString(), o.getType().toString())
                .result();
        }
        @Override
        public boolean equals(Object obj) {
            return compareTo((ReplacementRule) obj) == 0;
        }
        @Override
        public int hashCode() {
            return Objects.hashCode(getType());
        }
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                .add("type", getType())
                .add("replacement", replacementParts)
                .toString();
        }
        public XLanguageTag getType() {
            return typeParts;
        }
        public XLanguageTag getReplacement() {
            return replacementParts;
        }
    }

    /**
     * Utility to remove multiple items from Multimap
     */
    public static <K,V> Multimap<K, V> removeAll(Multimap<K, V> result, K key, Iterable<V> value) {
        for (V type : value) {
            result.remove(key, type);
        }
        return result;
    }

    private Set<ReplacementRule> rules = new TreeSet<>();
    private Multimap<LstrType, String> inType = TreeMultimap.create();
    private Map<LstrType, String> irrelevant = new TreeMap<>();

    private void add(ReplacementRule replacementRule) {
        getRules().add(replacementRule);
    }

    /**
     * Canonicalize a Unicode Language Identifier (LSRV - language, script, region, variants)
     * @param lstrType This is a special flag used to indicate which supplementalMetadata alias type the languageTag is from.
     * That determines whether to extend the type and replacement to be full LSRVs if they are partial, by adding "und_", for example.
     * @param languageTag May be partial, if the lstrType is not LstrType.language.
     */
    public String canonicalize(LstrType lstrType, String languageTag) {
        XLanguageTag newTag = canonicalizeToX(XLanguageTag.fromTag(lstrType, languageTag), null);
        return newTag.toString();
    }

    /**
     * Canonicalize a Unicode Language Identifier (LSRV - language, script, region, variants) in the XLanguageTag format.
     * Also returns the rules used in the canonicalization.<br>
     * NOT OPTIMIZED: just uses a linear search for simplicity; production code would use more efficient mechanisms
     */
    public XLanguageTag canonicalizeToX(XLanguageTag fromTag, List<ReplacementRule> rulesUsed) {
        if (rulesUsed != null) {
            rulesUsed.clear();
        }
        XLanguageTag newTag = fromTag;
        startAtTheTop:
            while (true) {
                for (ReplacementRule rule : getRules()) {
                    if (newTag.containsAll(rule.getType())) {
                        XLanguageTag temp = newTag.replacePartsFrom(rule.getType(), rule.getReplacement());
                        if (!temp.equals(newTag)) {
                            newTag = temp;
                            if (rulesUsed != null) {
                                rulesUsed.add(rule);
                            }
                            continue startAtTheTop;
                        }
                    }
                }
                return newTag;
            }
    }

    /**
     * Decanonicalize a Unicode Language Identifier (LSRV - language, script, region, variants) in the XLanguageTag format.
     * Also returns the rules used in the canonicalization. Used in test case generation
     * NOT OPTIMIZED: just for testing
     */
    public Set<XLanguageTag> decanonicalizeToX(XLanguageTag fromTag) {
        Set<XLanguageTag> result = new HashSet<>();
        result.add(fromTag);
        Set<XLanguageTag> intermediate = new HashSet<>();
        while (true) {
            for (ReplacementRule rule : getRules()) {
                if (!rule.getType().get(LstrType.variant).isEmpty()) {
                    continue;
                }
                for (XLanguageTag newTag : result) {
                    if (newTag.containsAll(rule.getReplacement())) { // reverse normal order
                        XLanguageTag changed = newTag.replacePartsFrom(rule.getReplacement(), rule.getType()); // reverse normal order
                        if (!intermediate.contains(changed)
                            && !result.contains(changed)) {
                            intermediate.add(changed);
                        }
                    }
                }
            }
            if (intermediate.isEmpty()) {
                result.remove(fromTag);
                return result;
            }
            result.addAll(intermediate);
            intermediate.clear();
        }
    }


    /**
     * Utility for getting a filtered list of rules, mostly useful in debugging.
     */
    public List<ReplacementRule> filter(LstrType lstrType, String value) {
        List<ReplacementRule> result = new ArrayList<>();
        for (ReplacementRule rule : getRules()) {
            final Collection<String> items = rule.getType().get(lstrType);
            if (value == null && !items.isEmpty()
                || value != null && items.contains(value)) {
                result.add(rule);
            }
        }
        return result;
    }

    public static final LsrvCanonicalizer getInstance() {
        return SINGLETON;
    }
    private static final LsrvCanonicalizer SINGLETON = load();

    private static LsrvCanonicalizer load() {
        SupplementalDataInfo SDI = CLDRConfig.getInstance().getSupplementalDataInfo();
        Map<String, Map<String, R2<List<String>, String>>> aliases = SDI.getLocaleAliasInfo();
        // type -> tag -> , like "language" -> "sh" -> <{"sr_Latn"}, reason>

        LsrvCanonicalizer rrs = new LsrvCanonicalizer();
        for (Entry<String, Map<String, R2<List<String>, String>>> typeTagReplacement : aliases.entrySet()) {
            String type = typeTagReplacement.getKey();
            if (type.contains("-")) {
                throw new IllegalArgumentException("Bad format for alias: should have _ instead of -.");
            }
            LstrType lstrType = LstrType.fromString(type);
            if (!LSRV.contains(lstrType)) {
                continue;
            }
            for (Entry<String, R2<List<String>, String>> tagReplacementReason : typeTagReplacement.getValue().entrySet()) {
                String tag = tagReplacementReason.getKey();
                if (tag.contains("-")) {
                    throw new IllegalArgumentException("Bad format for alias: should have _ instead of -.");
                }
                List<String> replacement = tagReplacementReason.getValue().get0();
                if (replacement == null) {
                    System.out.println("No replacement: " + tagReplacementReason);
                    continue;
                }
                String reason = tagReplacementReason.getValue().get1();
                final ReplacementRule replacementRule = ReplacementRule.from(lstrType, tag, replacement, reason);
                if (replacementRule == null) {
                    // System.out.println("No rule: " + tagReplacementReason);
                    continue;
                }
                rrs.add(replacementRule);
            }
        }
        rrs.rules = ImmutableSet.copyOf(rrs.rules);
        for (ReplacementRule rule :  rrs.rules) {
            XLanguageTag type = rule.getType();
            XLanguageTag replacement = rule.getReplacement();
            for (LstrType lstrType : LsrvCanonicalizer.LSRV) {
                rrs.inType.putAll(lstrType, type.get(lstrType));
                rrs.inType.putAll(lstrType, replacement.get(lstrType));
            }
        }
        rrs.inType = ImmutableMultimap.copyOf(rrs.inType);

        for (LstrType lstrType : LsrvCanonicalizer.LSRV) {
            Set<String> all = new LinkedHashSet<>(Validity.getInstance().getStatusToCodes(lstrType).get(Validity.Status.regular));
            all.removeAll(rrs.inType.get(lstrType));
            if (lstrType == LstrType.variant && all.contains("fonipa")) {
                rrs.irrelevant.put(lstrType, "fonipa");
            } else {
                rrs.irrelevant.put(lstrType, all.iterator().next());
            }
        }
        rrs.irrelevant = ImmutableMap.copyOf(rrs.irrelevant);
        return rrs;
    }

    /**
     * Returns the set of all the Replacement rules in the canonicalizer.
     */
    public Set<ReplacementRule> getRules() {
        return rules;
    }

    /**
     * Types of test data
     */
    public enum TestDataTypes {explicit, fromAliases, decanonicalized, withIrrelevants}

    /**
     * Returns test data for the rules, used to generate test data files.
     * @param testDataTypes if null, returns all the data; otherwise the specified set.
     * @return
     */
    public Map<TestDataTypes,Map<String, String>> getTestData(Set<TestDataTypes> testDataTypes) {
        Map<TestDataTypes,Map<String, String>> result = new TreeMap<>();

        if (testDataTypes == null) {
            testDataTypes = EnumSet.allOf(TestDataTypes.class);
        }
        Set<String> allToTest = new TreeSet<>();
        if (testDataTypes.contains(TestDataTypes.explicit)) {
            Map<String, String> testData2 = new TreeMap<>();
            String[][] tests = {
                {"hye_arevmda", "hyw"},
                {"art_lojban", "jbo"},
                {"en_arevela", "en"},
                {"hy_arevela", "hy"},
                {"en_arevmda_arevela", "en"},
                {"hy_arevmda", "hyw"},
                {"hy_arevmda_arevela", "hyw"},
                {"en_lojban", "en"},
                {"en_US_polytoni", "en_US_polyton"},
                {"en_US_heploc", "en_US_alalc97"},
                {"en_US_aaland", "en_US"},
                {"en_aaland", "en_AX"},
                {"no_nynorsk_bokmal", "nb"},
                {"no_bokmal_nynorsk", "nb"},
                {"zh_guoyu_hakka_xiang", "hak"},
                {"zh_hakka_xiang", "hak"},
            };
            for (String row[] : tests) {
                String toTest = row[0];
                String expected = row[1];
                testData2.put(toTest, expected);
            }
            allToTest.addAll(testData2.keySet());
            result.put(TestDataTypes.explicit, ImmutableMap.copyOf(testData2));
        }

        if (testDataTypes.contains(TestDataTypes.fromAliases)) {
            Map<String, String> testData2 = new TreeMap<>();
            for (ReplacementRule rule : getRules()) {
                String toTest = rule.getType().toLocaleString();
                String expected = rule.getReplacement().toLocaleString();
                if (!allToTest.contains(toTest)) {
                    testData2.put(toTest,expected);
                }
            }
            allToTest.addAll(testData2.keySet());
            result.put(TestDataTypes.fromAliases, ImmutableMap.copyOf(testData2));
        }

        if (testDataTypes.contains(TestDataTypes.decanonicalized)) {
            Map<String, String> testData2 = new TreeMap<>();
            for (String testItem: allToTest) {
                for (XLanguageTag decon : decanonicalizeToX(XLanguageTag.fromTag(LstrType.language, testItem))) {
                    XLanguageTag newTag = canonicalizeToX(decon, null);
                    final String toTest = decon.toLocaleString();
                    if (!allToTest.contains(toTest)) {
                        testData2.put(toTest, newTag.toLocaleString());
                    }
                }
            }
            allToTest.addAll(testData2.keySet());
            result.put(TestDataTypes.decanonicalized, ImmutableMap.copyOf(testData2));
        }

        if (testDataTypes.contains(TestDataTypes.withIrrelevants)) {
            Map<String, String> testData2 = new TreeMap<>();
            for (String testItem: allToTest) {
                XLanguageTag fluffedUp = fluff(XLanguageTag.fromTag(LstrType.language, testItem), irrelevant);
                XLanguageTag newTag = canonicalizeToX(fluffedUp, null);
                final String toTest = fluffedUp.toLocaleString();
                if (!allToTest.contains(toTest)) {
                    testData2.put(toTest, newTag.toLocaleString());
                }
           }
            allToTest.addAll(testData2.keySet());
            result.put(TestDataTypes.withIrrelevants, ImmutableMap.copyOf(testData2));
        }

        result = ImmutableMap.copyOf(result);
        return result;
    }

    private static XLanguageTag fluff(XLanguageTag type, Map<LstrType, String> toAddIfMissing) {
        XLanguageTag newTag = type;
        for (LstrType lstrType : LsrvCanonicalizer.LSRV) {
            if (type.get(lstrType).isEmpty() || lstrType == LstrType.variant) {
                newTag = newTag.set(lstrType, toAddIfMissing.get(lstrType));
            }
        }
        return newTag;
    }

    /**
     * Returns all the fields used in the type attribute of the alias rule.
     */
    public Collection<String> getInType(LstrType language) {
        return inType.get(language);
    }

    /**
     * Returns some sample fields that do not appear in the type attribute of the alias rule, used for testing.
     */
    public String getIrrelevantField(LstrType language) {
        return irrelevant.get(language);
    }

}
