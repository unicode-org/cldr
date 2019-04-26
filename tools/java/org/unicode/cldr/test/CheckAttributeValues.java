package org.unicode.cldr.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdData.Attribute;
import org.unicode.cldr.util.DtdData.Element;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.AttributeValidityInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.util.CollectionUtilities.ObjectMatcher;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.UnicodeSet;

public class CheckAttributeValues extends FactoryCheckCLDR {

    private static final ObjectMatcher<String> NOT_DONE_YET = new RegexMatcher().set(".*", Pattern.COMMENTS);
    private static final boolean FIND_MISSING = CldrUtility.getProperty("FIND_MISSING_ATTRIBUTE_TESTS", false); // turn on to show <attributeValues> that are missing.
    private static final boolean SHOW_UNNECESSARY = false; // turn on to show <attributeValues> we should delete.

    static LinkedHashSet<String> elementOrder = new LinkedHashSet<String>();
    static LinkedHashSet<String> attributeOrder = new LinkedHashSet<String>();
    static LinkedHashSet<String> serialElements = new LinkedHashSet<String>();
    static Map<String, Map<String, MatcherPattern>> element_attribute_validity = new HashMap<String, Map<String, MatcherPattern>>();
    static Map<String, MatcherPattern> common_attribute_validity = new HashMap<String, MatcherPattern>();
    static Map<String, MatcherPattern> variables = new HashMap<String, MatcherPattern>();
    // static VariableReplacer variableReplacer = new VariableReplacer(); // note: this can be coalesced with the above
    // -- to do later.
    static boolean initialized = false;
    static LocaleMatcher localeMatcher;
    static Map<String, Map<String, String>> code_type_replacement = new TreeMap<String, Map<String, String>>();
    static final SupplementalDataInfo supplementalData = CLDRConfig.getInstance().getSupplementalDataInfo();
    static DtdData ldmlDtdData = DtdData.getInstance(DtdType.ldml);

    boolean isEnglish;
    PluralInfo pluralInfo;
    Relation<String, String> missingTests = Relation.of(new TreeMap(), TreeSet.class);

    static final UnicodeSet DIGITS = new UnicodeSet("[0-9]").freeze();

    public CheckAttributeValues(Factory factory) {
        super(factory);
    }

    public void handleFinish() {
        for (Entry<String, Set<String>> entry : missingTests.keyValuesSet()) {
            System.out.println("Missing element: " + entry.getKey() + ", attributes: " + entry.getValue());
        }
    }

    public CheckCLDR handleCheck(String path, String fullPath, String value, Options options,
        List<CheckStatus> result) {
        if (fullPath == null) return this; // skip paths that we don't have
        if (fullPath.indexOf('[') < 0) return this; // skip paths with no attributes
        String locale = getCldrFileToCheck().getSourceLocaleID(path, null);

        // skip paths that are not in the immediate locale
        if (!getCldrFileToCheck().getLocaleID().equals(locale)) {
            return this;
        }
        XPathParts parts = XPathParts.getTestInstance(fullPath);
        for (int i = 0; i < parts.size(); ++i) {
            if (parts.getAttributeCount(i) == 0) {
                continue;
            }
            Map<String, String> attributes = parts.getAttributes(i);
            String element = parts.getElement(i);
            Element elementInfo = ldmlDtdData.getElementFromName().get(element);

            Map<String, MatcherPattern> attribute_validity = element_attribute_validity.get(element);
            for (String attribute : attributes.keySet()) {
                Attribute attributeInfo = elementInfo.getAttributeNamed(attribute);
                if (!attributeInfo.values.isEmpty()) {
                    // we don't need to check, since the DTD will enforce values
                    continue;
                }
                String attributeValue = attributes.get(attribute);

                // special hack for         // <type key="calendar" type="chinese">Chinese Calendar</type>
                if (element.equals("type") && attribute.equals("type")) {
                    Set<String> typeValues = BCP47_KEY_VALUES.get(attributes.get("key"));
                    if (!typeValues.contains(attributeValue)) {
                        result.add(new CheckStatus()
                            .setCause(this).setMainType(CheckStatus.errorType).setSubtype(Subtype.unexpectedAttributeValue)
                            .setMessage("Unexpected Attribute Value {0}={1}: expected: {2}",
                                new Object[] { attribute, attributeValue, typeValues }));
                    }
                    continue;
                }
                // check the common attributes first
                boolean haveTest = check(common_attribute_validity, attribute, attributeValue, result);
                // then for the specific element
                haveTest = haveTest || check(attribute_validity, attribute, attributeValue, result);
                if (!haveTest && FIND_MISSING) {
                    missingTests.put(element, attribute);
                }

                // now for plurals

                if (attribute.equals("count")) {
                    if (DIGITS.containsAll(attributeValue)) {
                        // ok, keep going
                    } else {
                        final Count countValue = PluralInfo.Count.valueOf(attributeValue);
                        if (!pluralInfo.getCounts().contains(countValue)
                            && !isPluralException(countValue, locale)) {
                            result.add(new CheckStatus()
                                .setCause(this).setMainType(CheckStatus.errorType).setSubtype(Subtype.illegalPlural)
                                .setMessage("Illegal plural value {0}; must be one of: {1}",
                                    new Object[] { countValue, pluralInfo.getCounts() }));
                        }
                    }
                }

                // TODO check other variable elements, like dayPeriods
            }
        }
        return this;
    }

    static final Relation<PluralInfo.Count, String> PLURAL_EXCEPTIONS = Relation.of(
        new EnumMap<PluralInfo.Count, Set<String>>(PluralInfo.Count.class), HashSet.class);

    static {
        PLURAL_EXCEPTIONS.put(PluralInfo.Count.many, "hr");
        PLURAL_EXCEPTIONS.put(PluralInfo.Count.many, "sr");
        PLURAL_EXCEPTIONS.put(PluralInfo.Count.many, "sh");
        PLURAL_EXCEPTIONS.put(PluralInfo.Count.many, "bs");
        PLURAL_EXCEPTIONS.put(PluralInfo.Count.few, "ru");
    }

    static boolean isPluralException(Count countValue, String locale) {
        Set<String> exceptions = PLURAL_EXCEPTIONS.get(countValue);
        if (exceptions == null) {
            return false;
        }
        if (exceptions.contains(locale)) {
            return true;
        }
        int bar = locale.indexOf('_'); // catch bs_Cyrl, etc.
        if (bar > 0) {
            String base = locale.substring(0, bar);
            if (exceptions.contains(base)) {
                return true;
            }
        }
        return false;
    }

    /**
     * return true if we performed a test
     * @param attribute_validity
     * @param attribute
     * @param attributeValue
     * @param result
     * @return
     */
    private boolean check(Map<String, MatcherPattern> attribute_validity, String attribute, String attributeValue,
        List<CheckStatus> result) {
        if (attribute_validity == null) {
            return false; // no test
        }
        MatcherPattern matcherPattern = attribute_validity.get(attribute);
        if (matcherPattern == null) {
            return false; // no test
        }
        if (matcherPattern.matcher.matches(attributeValue)) {
            return true;
        }
        // special check for deprecated codes
        String replacement = getReplacement(matcherPattern.value, attributeValue);
        if (replacement != null) {
            if (isEnglish) {
                return true; // don't flag English
            }
            if (replacement.length() == 0) {
                result.add(new CheckStatus()
                    .setCause(this).setMainType(CheckStatus.warningType).setSubtype(Subtype.deprecatedAttribute)
                    .setMessage("Deprecated Attribute Value {0}={1}. Consider removing.",
                        new Object[] { attribute, attributeValue }));
            } else {
                result
                    .add(new CheckStatus()
                        .setCause(this)
                        .setMainType(CheckStatus.warningType)
                        .setSubtype(Subtype.deprecatedAttributeWithReplacement)
                        .setMessage(
                            "Deprecated Attribute Value {0}={1}. Consider removing, and possibly modifying the related value for {2}.",
                            new Object[] { attribute, attributeValue, replacement }));
            }
        } else {
            result.add(new CheckStatus()
                .setCause(this).setMainType(CheckStatus.errorType).setSubtype(Subtype.unexpectedAttributeValue)
                .setMessage("Unexpected Attribute Value {0}={1}: expected: {2}",
                    new Object[] { attribute, attributeValue, matcherPattern.pattern }));
        }
        return true;
    }

    /**
     * Returns replacement, or null if there is none. "" if the code is deprecated, but without a replacement.
     * Input is of the form $language
     *
     * @return
     */
    String getReplacement(String value, String attributeValue) {
        Map<String, String> type_replacement = code_type_replacement.get(value);
        if (type_replacement == null) {
            return null;
        }
        return type_replacement.get(attributeValue);
    }

    LocaleIDParser localeIDParser = new LocaleIDParser();

    @Override
    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Options options,
        List<CheckStatus> possibleErrors) {
        if (cldrFileToCheck == null) return this;
        if (Phase.FINAL_TESTING == getPhase() || Phase.BUILD == getPhase()) {
            setSkipTest(false); // ok
        } else {
            setSkipTest(true);
            return this;
        }

        pluralInfo = supplementalData.getPlurals(PluralType.cardinal, cldrFileToCheck.getLocaleID());
        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        isEnglish = "en".equals(localeIDParser.set(cldrFileToCheck.getLocaleID()).getLanguage());
        synchronized (elementOrder) {
            if (!initialized) {
                getMetadata();
                initialized = true;
                localeMatcher = LocaleMatcher.make();
            }
        }
        if (!localeMatcher.matches(cldrFileToCheck.getLocaleID())) {
            possibleErrors.add(new CheckStatus()
                .setCause(this).setMainType(CheckStatus.errorType).setSubtype(Subtype.invalidLocale)
                .setMessage("Invalid Locale {0}",
                    new Object[] { cldrFileToCheck.getLocaleID() }));

        }
        return this;
    }

    private void getMetadata() {

        // sorting is expensive, but we need it here.

        Map<String, R2<String, String>> rawVariables = supplementalData.getValidityInfo();
        for (Entry<String, R2<String, String>> item : rawVariables.entrySet()) {
            String id = item.getKey();
            String type = item.getValue().get0();
            String value = item.getValue().get1();
            MatcherPattern mp = getMatcherPattern2(type, value);
            if (mp != null) {
                variables.put(id, mp);
                // variableReplacer.add(id, value);
            }
        }
        //System.out.println("Variables: " + variables.keySet());

        Map<AttributeValidityInfo, String> rawAttributeValueInfo = supplementalData.getAttributeValidity();

        for (Entry<AttributeValidityInfo, String> entry : rawAttributeValueInfo.entrySet()) {
            AttributeValidityInfo item = entry.getKey();
            String value = entry.getValue();
            MatcherPattern mp = getMatcherPattern2(item.getType(), value);
            if (mp == null) {
                System.out.println("Failed to make matcher for: " + item);
                continue;
            }
            if (FIND_MISSING && mp.matcher == NOT_DONE_YET) {
                missingTests.put(item.getElements().toString(), item.getAttributes().toString());
            }

            Set<DtdType> dtds = item.getDtds();
            // TODO handle other DTDs
            if (!dtds.contains(DtdType.ldml)) {
                continue;
            }
            Set<String> attributeList = item.getAttributes();
            Set<String> elementList = item.getElements();
            if (elementList.size() == 0) {
                addAttributes(attributeList, common_attribute_validity, mp);
            } else {
                for (String element : elementList) {
                    // check if unnecessary
                    Element elementInfo = ldmlDtdData.getElementFromName().get(element);
                    if (elementInfo == null) {
                        System.out.println("Illegal <attributeValues>, element not valid: element: " + element);
                    } else {
                        for (String attribute : attributeList) {
                            Attribute attributeInfo = elementInfo.getAttributeNamed(attribute);
                            if (attributeInfo == null) {
                                System.out.println("Illegal <attributeValues>, attribute not valid: element: " + element + ", attribute: " + attribute);
                            } else if (!attributeInfo.values.isEmpty()) {
                                if (SHOW_UNNECESSARY) {
                                    System.out.println("Unnecessary <attributeValues …>, the DTD has specific list: element: " + element + ", attribute: "
                                        + attribute + ", " + attributeInfo.values);
                                }
                            }
                        }
                    }
                    // System.out.println("\t" + element);
                    Map<String, MatcherPattern> attribute_validity = element_attribute_validity.get(element);
                    if (attribute_validity == null) {
                        element_attribute_validity.put(element, attribute_validity = new TreeMap<String, MatcherPattern>());
                    }
                    addAttributes(attributeList, attribute_validity, mp);
                }
            }
        }
    }

    final static Map<String, Set<String>> BCP47_KEY_VALUES;
    static {
        Map<String, Set<String>> temp = new HashMap<>();
        Relation<R2<String, String>, String> bcp47Aliases = supplementalData.getBcp47Aliases();
        for (Entry<String, Set<String>> keyValues : supplementalData.getBcp47Keys().keyValuesSet()) {
            Set<String> fullValues = new TreeSet<>();
            String key = keyValues.getKey();
            Set<String> rawValues = keyValues.getValue();
            for (String value : rawValues) {
                if (key.equals("cu")) { // Currency codes are in upper case.
                    fullValues.add(value.toUpperCase());
                } else {
                    fullValues.add(value);
                }
                R2<String, String> keyValue = R2.of(key, value);
                Set<String> aliases = bcp47Aliases.getAll(keyValue);
                if (aliases != null) {
                    fullValues.addAll(aliases);
                }
            }
            // Special case exception for generic calendar, since we don't want to expose it in bcp47
            if (key.equals("ca")) {
                fullValues.add("generic");
            }
            fullValues = Collections.unmodifiableSet(fullValues);
            temp.put(key, fullValues);
            // add aliased keys
            Set<String> aliases = supplementalData.getBcp47Aliases().getAll(Row.of(key, ""));
            if (aliases != null) {
                for (String aliasKey : aliases) {
                    temp.put(aliasKey, fullValues);
                }
            }
            temp.put("x", Collections.EMPTY_SET); // Hack for 'x', private use.
        }
        BCP47_KEY_VALUES = Collections.unmodifiableMap(temp);
    }

    private MatcherPattern getBcp47MatcherPattern(String key) {
        // <key type="calendar">Calendar</key>
        // <type key="calendar" type="chinese">Chinese Calendar</type>

        //<attributeValues elements="key" attributes="type" type="bcp47">key</attributeValues>
        //<attributeValues elements="type" attributes="key" type="bcp47">key</attributeValues>
        //<attributeValues elements="type" attributes="type" type="bcp47">use-key</attributeValues>

        MatcherPattern m = new MatcherPattern();
        Set<String> values;
        if (key.equals("key")) {
            values = BCP47_KEY_VALUES.keySet();
        } else {
            values = BCP47_KEY_VALUES.get(key);
        }
        m.value = key;
        m.pattern = values.toString();
        m.matcher = new CollectionMatcher().set(values);
        return m;
    }

    private MatcherPattern getMatcherPattern2(String type, String value) {
        String typeAttribute = type;
        MatcherPattern result = variables.get(value);
        if (result != null) {
            MatcherPattern temp = new MatcherPattern();
            temp.pattern = result.pattern;
            temp.matcher = result.matcher;
            temp.value = value;
            result = temp;
            if ("list".equals(typeAttribute)) {
                temp.matcher = new ListMatcher().set(result.matcher);
            }
            return result;
        }

        result = new MatcherPattern();
        result.pattern = value;
        result.value = value;
        if ("choice".equals(typeAttribute)) {
            result.matcher = new CollectionMatcher()
                .set(new HashSet<String>(Arrays.asList(value.trim().split("\\s+"))));
        } else if ("bcp47".equals(typeAttribute)) {
            result = getBcp47MatcherPattern(value);
        } else if ("regex".equals(typeAttribute)) {
            result.matcher = new RegexMatcher().set(value, Pattern.COMMENTS); // Pattern.COMMENTS to get whitespace
        } else if ("locale".equals(typeAttribute)) {
            result.matcher = LocaleMatcher.make();
        } else if ("notDoneYet".equals(typeAttribute) || "notDoneYet".equals(value)) {
            result.matcher = NOT_DONE_YET;
        } else {
            System.out.println("unknown type; value: <" + value + ">,\t" + typeAttribute);
            return null;
        }
        return result;
    }

    private void addAttributes(Set<String> attributes, Map<String, MatcherPattern> attribute_validity, MatcherPattern mp) {
        for (String attribute : attributes) {
            MatcherPattern old = attribute_validity.get(attribute);
            if (old != null) {
                mp.matcher = new OrMatcher().set(old.matcher, mp.matcher);
                mp.pattern = old.pattern + " OR " + mp.pattern;
            }
            attribute_validity.put(attribute, mp);
        }
    }

    private static class MatcherPattern {
        public String value;
        ObjectMatcher<String> matcher;
        String pattern;

        public String toString() {
            return matcher.getClass().getName() + "\t" + pattern;
        }
    }

    public static class RegexMatcher implements ObjectMatcher<String> {
        private java.util.regex.Matcher matcher;

        public ObjectMatcher<String> set(String pattern) {
            matcher = PatternCache.get(pattern).matcher("");
            return this;
        }

        public ObjectMatcher<String> set(String pattern, int flags) {
            matcher = Pattern.compile(pattern, flags).matcher("");
            return this;
        }

        public boolean matches(String value) {
            matcher.reset(value.toString());
            return matcher.matches();
        }
    }

    public static class CollectionMatcher implements ObjectMatcher<String> {
        private Collection<String> collection;

        public ObjectMatcher<String> set(Collection<String> collection) {
            this.collection = collection;
            return this;
        }

        public boolean matches(String value) {
            return collection.contains(value);
        }
    }

    public static class OrMatcher implements ObjectMatcher<String> {
        private ObjectMatcher<String> a;
        private ObjectMatcher<String> b;

        public ObjectMatcher<String> set(ObjectMatcher<String> a, ObjectMatcher<String> b) {
            this.a = a;
            this.b = b;
            return this;
        }

        public boolean matches(String value) {
            return a.matches(value) || b.matches(value);
        }
    }

    public static class ListMatcher implements ObjectMatcher<String> {
        private ObjectMatcher<String> other;

        public ObjectMatcher<String> set(ObjectMatcher<String> other) {
            this.other = other;
            return this;
        }

        public boolean matches(String value) {
            String[] values = value.trim().split("\\s+");
            if (values.length == 1 && values[0].length() == 0) return true;
            for (int i = 0; i < values.length; ++i) {
                if (!other.matches(values[i])) {
                    return false;
                }
            }
            return true;
        }
    }

    public static class LocaleMatcher implements ObjectMatcher<String> {
        ObjectMatcher<String> grandfathered = variables.get("$grandfathered").matcher;
        ObjectMatcher<String> language = variables.get("$language").matcher;
        ObjectMatcher<String> script = variables.get("$script").matcher;
        ObjectMatcher<String> territory = variables.get("$territory").matcher;
        ObjectMatcher<String> variant = variables.get("$variant").matcher;
        LocaleIDParser lip = new LocaleIDParser();
        static LocaleMatcher singleton = null;
        static Object sync = new Object();

        private LocaleMatcher(boolean b) {
        }

        public static LocaleMatcher make() {
            synchronized (sync) {
                if (singleton == null) {
                    singleton = new LocaleMatcher(true);
                }
            }
            return singleton;
        }

        public boolean matches(String value) {
            if (grandfathered.matches(value)) return true;
            lip.set((String) value);
            String field = lip.getLanguage();
            if (!language.matches(field)) return false;
            field = lip.getScript();
            if (field.length() != 0 && !script.matches(field)) return false;
            field = lip.getRegion();
            if (field.length() != 0 && !territory.matches(field)) return false;
            String[] fields = lip.getVariants();
            for (int i = 0; i < fields.length; ++i) {
                if (!variant.matches(fields[i])) return false;
            }
            return true;
        }
    }

}