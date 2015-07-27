package org.unicode.cldr.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo.AttributeValidityInfo;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.CollectionUtilities.ObjectMatcher;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.Output;

public class AttributeValueValidity {

    public enum Status {ok, deprecated, illegal, noTest}
    public enum LocaleSpecific {pluralCardinal, pluralOrdinal, dayPeriodFormat, dayPeriodSelection}

    static final Splitter BAR = Splitter.on('|').trimResults().omitEmptyStrings();
    static final Splitter SPACE = Splitter.on(Pattern.compile("\\s+")).trimResults().omitEmptyStrings();

    private static final Set<DtdType> ALL_DTDs = Collections.unmodifiableSet(EnumSet.allOf(DtdType.class));

    private static final SupplementalDataInfo supplementalData = CLDRConfig.getInstance().getSupplementalDataInfo();

    private static Map<DtdType,Map<String, Map<String, MatcherPattern>>> dtd_element_attribute_validity = new EnumMap<>(DtdType.class);
    private static Map<String, MatcherPattern> common_attribute_validity = new LinkedHashMap<String, MatcherPattern>();
    private static Map<String, MatcherPattern> variables = new LinkedHashMap<String, MatcherPattern>();
    //private static Map<String, Map<String, String>> code_type_replacement = new TreeMap<String, Map<String, String>>();
    private static Map<String, Set<String>> BCP47_KEY_VALUES;
    private static final RegexMatcher NOT_DONE_YET = new RegexMatcher(".*", Pattern.COMMENTS);
    private static final Set<AttributeValidityInfo> failures = new LinkedHashSet<>();
    private static final boolean DEBUG = false;

    static {

        Map<String, Set<String>> temp = new LinkedHashMap<>();
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
            addCollectionVariable("$_bcp47_" + key, fullValues);

            //variables.put(keyName+"_"+key, m);
            // add aliased keys
            Set<String> aliases = supplementalData.getBcp47Aliases().getAll(Row.of(key, ""));
            if (aliases != null) {
                for (String aliasKey : aliases) {
                    temp.put(aliasKey, fullValues);
                    addCollectionVariable("$_bcp47_" + aliasKey, fullValues);
                }
            }
        }
        temp.put("x", Collections.<String>emptySet()); // Hack for 'x', private use.
        //addCollectionVariable("$_bcp47_x", Collections.<String>emptySet());
        BCP47_KEY_VALUES = Collections.unmodifiableMap(temp);
        addCollectionVariable("$_bcp47_keys", BCP47_KEY_VALUES.keySet());

        Validity validity = Validity.getInstance();
        for (Entry<LstrType, Map<Validity.Status, Set<String>>> item1 : validity.getData().entrySet()) {
            LstrType key = item1.getKey();
            String keyName = "$_"+key;
            Set<String> all = new LinkedHashSet<>();
            Set<String> regularAndUnknown = new LinkedHashSet<>();
            for (Entry<Validity.Status, Set<String>> item2 : item1.getValue().entrySet()) {
                Validity.Status status = item2.getKey();
                Set<String> validItems = item2.getValue();
                if (key == LstrType.variant) { // uppercased in CLDR
                    Set<String> temp2 = new LinkedHashSet<>(validItems);
                    for (String item : validItems) {
                        temp2.add(item.toUpperCase(Locale.ROOT));
                    }
                    validItems = temp2;
                }
                all.addAll(validItems);
                if (status == Validity.Status.regular || status == Validity.Status.unknown) {
                    regularAndUnknown.addAll(validItems);
                }
                addCollectionVariable(keyName+"_"+status, validItems);
//                MatcherPattern m = new MatcherPattern(key.toString(), validItems.toString(), new CollectionMatcher(validItems));
//                variables.put(keyName+"_"+status, m);
            }
            addCollectionVariable(keyName, all);
            addCollectionVariable(keyName+"_plus", regularAndUnknown);

//            MatcherPattern m = new MatcherPattern(key.toString(), all.toString(), new CollectionMatcher(all));
//            variables.put(keyName, m);
//            MatcherPattern m2 = new MatcherPattern(key.toString(), regularAndUnknown.toString(), new CollectionMatcher(regularAndUnknown));
//            variables.put(keyName + "_plus", m2);
        }

        Map<String, R2<String, String>> rawVariables = supplementalData.getValidityInfo();
        for (Entry<String, R2<String, String>> item : rawVariables.entrySet()) {
            String id = item.getKey();
            String type = item.getValue().get0();
            String value = item.getValue().get1();
            MatcherPattern mp = getMatcherPattern2(type, value);
            if (mp != null) {
                variables.put(id, mp);
                // variableReplacer.add(id, value);
            } else {
                throw new IllegalArgumentException("Duplicate element " + mp);
            }
        }
        //System.out.println("Variables: " + variables.keySet());

        Set<AttributeValidityInfo> rawAttributeValueInfo = supplementalData.getAttributeValidity();
        int x = 0;
        for (AttributeValidityInfo item : rawAttributeValueInfo) {
            //System.out.println(item);
            MatcherPattern mp = getMatcherPattern2(item.getType(), item.getValue());
            if (mp == null) {
                failures.add(item);
                continue;
            }
            Set<DtdType> dtds = item.getDtds();
            if (dtds == null) {
                dtds = ALL_DTDs;
            }
            for (DtdType dtdType : dtds) {
                DtdData data = DtdData.getInstance(dtdType);
                Map<String, Map<String, MatcherPattern>> element_attribute_validity = dtd_element_attribute_validity.get(dtdType);
                if (element_attribute_validity == null) {
                    dtd_element_attribute_validity.put(dtdType, element_attribute_validity = new TreeMap<String, Map<String, MatcherPattern>>());
                }

                //             <attributeValues dtds="supplementalData" elements="currency" attributes="before from to">$currencyDate</attributeValues>

                Set<String> attributeList = item.getAttributes();
                Set<String> elementList = item.getElements();
                if (elementList.size() == 0) {
                    addAttributes(attributeList, common_attribute_validity, mp);
                } else {
                    for (String element : elementList) {
                        // check if unnecessary
                        DtdData.Element elementInfo = data.getElementFromName().get(element);
                        if (elementInfo == null) {
                            throw new ICUException(
                                "Illegal <attributeValues>, element not valid: " 
                                    + dtdType
                                    + ", element: " + element
                                );
                        } else {
                            for (String attribute : attributeList) {
                                DtdData.Attribute attributeInfo = elementInfo.getAttributeNamed(attribute);
                                if (attributeInfo == null) {
                                    throw new ICUException(
                                        "Illegal <attributeValues>, attribute not valid: "
                                            + dtdType
                                            + ", element: " + element
                                            + ", attribute: " + attribute);
                                } else if (!attributeInfo.values.isEmpty()) {
//                                    if (false) {
//                                        System.out.println("Unnecessary <attributeValues …>, the DTD has specific list: element: " + element + ", attribute: " + attribute + ", " + attributeInfo.values);
//                                    }
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
        // show values
//        for (Entry<DtdType, Map<String, Map<String, MatcherPattern>>> entry1 : dtd_element_attribute_validity.entrySet()) {
//            final DtdType dtdType = entry1.getKey();
//            Map<String, Map<String, MatcherPattern>> element_attribute_validity = entry1.getValue();
//            DtdData dtdData2 = DtdData.getInstance(dtdType);
//            for (Element element : dtdData2.getElements()) {
//                Set<Attribute> attributes = element.getAttributes().keySet();
//                
//            }
//            for (Entry<String, Map<String, MatcherPattern>> entry2 : entry1.getValue().entrySet()) {
//                for (Entry<String, MatcherPattern> entry3 : entry2.getValue().entrySet()) {
//                    System.out.println(dtdType + "\t" + entry2.getKey() + "\t" + entry3.getKey() + "\t" + entry3.getValue());
//                }
//            }
//        }

//        private LocaleIDParser localeIDParser = new LocaleIDParser();
        //
//        @Override
//        public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Options options,
//            List<CheckStatus> possibleErrors) {
//            if (cldrFileToCheck == null) return this;
//            if (Phase.FINAL_TESTING == getPhase() || Phase.BUILD == getPhase()) {
//                setSkipTest(false); // ok
//            } else {
//                setSkipTest(true);
//                return this;
//            }
        //
//            pluralInfo = supplementalData.getPlurals(PluralType.cardinal, cldrFileToCheck.getLocaleID());
//            super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
//            isEnglish = "en".equals(localeIDParser.set(cldrFileToCheck.getLocaleID()).getLanguage());
//            synchronized (elementOrder) {
//                if (!initialized) {
//                    getMetadata();
//                    initialized = true;
//                    localeMatcher = LocaleMatcher.make();
//                }
//            }
//            if (!localeMatcher.matches(cldrFileToCheck.getLocaleID())) {
//                possibleErrors.add(new CheckStatus()
//                .setCause(null).setMainType(CheckStatus.errorType).setSubtype(Subtype.invalidLocale)
//                .setMessage("Invalid Locale {0}",
//                    new Object[] { cldrFileToCheck.getLocaleID() }));
        //
//            }
//            return this;
//        }
    }

    private static void addCollectionVariable(String name, Set<String> validItems) {
        variables.put(name, new CollectionMatcher(validItems));
    }

    public static Relation<String, String> getAllPossibleMissing(DtdType dtdType) {
        Relation<String,String> missing = Relation.of(new TreeMap<String,Set<String>>(), LinkedHashSet.class);

        if (dtdType == DtdType.ldmlICU) {
            return missing;
        }

        DtdData dtdData2 = DtdData.getInstance(dtdType);
        Map<String, Map<String, MatcherPattern>> element_attribute_validity = CldrUtility.ifNull(
            dtd_element_attribute_validity.get(dtdType), 
            Collections.<String, Map<String, MatcherPattern>>emptyMap());

        for (DtdData.Element element : dtdData2.getElements()) {
            if (element.isDeprecated()) {
                continue;
            }
            Map<String, MatcherPattern> attribute_validity = CldrUtility.ifNull(
                element_attribute_validity.get(element.name),
                Collections.<String, MatcherPattern>emptyMap());
            for (DtdData.Attribute attribute : element.getAttributes().keySet()) {
                if (attribute.isDeprecated()) {
                    continue;
                }
                if (!attribute.values.isEmpty()) {
                    continue;
                }
                MatcherPattern validity = attribute_validity.get(attribute.name);
                if (validity != null) {
                    continue;
                }
                //            <attributeValues attributes="alt" type="choice">$alt</attributeValues>
                //             <attributeValues dtds="supplementalData" elements="character" attributes="value" type="regex">.</attributeValues>
                missing.put(attribute.name, 
                    getElementLine(dtdType, element.name, attribute.name, "$xxx"));
            }
        }
        return missing;
    }

    private static abstract class MatcherPattern {

        public abstract boolean matches(String value, Output<String> reason);
        public String getPattern() {
            String temp = _getPattern();
            return temp.length() <= MAX_STRING ? temp : temp.substring(0,MAX_STRING) + "…";
        }
        public abstract String _getPattern();

        public String toString() {
            return getClass().getName() + "\t" + getPattern();
        }
    }

    private static MatcherPattern getBcp47MatcherPattern(String key) {
        // <key type="calendar">Calendar</key>
        // <type key="calendar" type="chinese">Chinese Calendar</type>

        //<attributeValues elements="key" attributes="type" type="bcp47">key</attributeValues>
        //<attributeValues elements="type" attributes="key" type="bcp47">key</attributeValues>
        //<attributeValues elements="type" attributes="type" type="bcp47">use-key</attributeValues>

        Set<String> values;
        if (key.equals("key")) {
            values = BCP47_KEY_VALUES.keySet();
        } else {
            values = BCP47_KEY_VALUES.get(key);
        }
        return new CollectionMatcher(values);
    }

    enum MatcherTypes {
        single,
        choice,
        list,
        unicodeSet,
        regex,
        locale,
        bcp47,
        subdivision,
        localeSpecific,
        TODO;
    }

    private static MatcherPattern getMatcherPattern2(String type, String value) {
        final MatcherTypes matcherType = type == null ? MatcherTypes.single : MatcherTypes.valueOf(type);

        if (matcherType != MatcherTypes.TODO && value.startsWith("$")) {
            MatcherPattern result = getVariable(matcherType, value);
            if (result != null) {
                return result;
            }
            throw new IllegalArgumentException("Unknown variable: " + value);
        }

        MatcherPattern result;

        switch(matcherType) {
        case single:
            result = new CollectionMatcher(Collections.singleton(value.trim()));
            break;
        case choice:
            result = new CollectionMatcher(SPACE.splitToList(value));
            break;
        case unicodeSet:
            result = new UnicodeSetMatcher(new UnicodeSet(value));
            break;
        case bcp47:
            return getBcp47MatcherPattern(value);
        case regex:
            result = new RegexMatcher(value, Pattern.COMMENTS); // Pattern.COMMENTS to get whitespace
            break;
        case locale:
            result = value.equals("all") ? LocaleMatcher.ALL_LANGUAGES : LocaleMatcher.REGULAR;
            break;
        case localeSpecific:
            result = LocaleSpecificMatcher.getInstance(value);
            break;
        case TODO:
            result = NOT_DONE_YET;
            break;
        case list:
            result = new ListMatcher(new CollectionMatcher(SPACE.splitToList(value)));
        default:
            return null;
        }

        return result;
    }


    private static MatcherPattern getVariable(final MatcherTypes matcherType, String value) {
        List<String> values = BAR.splitToList(value); //value.trim().split("|");
        MatcherPattern[] reasons = new MatcherPattern[values.size()];
        for (int i = 0; i < values.size(); ++i) {
            reasons[i] = variables.get(values.get(i));
        }
        MatcherPattern result;

        if (reasons.length == 1) {
            result = reasons[0];
        } else {
            result = new OrMatcher(reasons);
        }
        if (matcherType == MatcherTypes.list) {
            result = new ListMatcher(result);
        }
        return result;
    }

    private static void addAttributes(Set<String> attributes, Map<String, MatcherPattern> attribute_validity, MatcherPattern mp) {
        for (String attribute : attributes) {
            MatcherPattern old = attribute_validity.get(attribute);
            if (old != null) {
                mp = new OrMatcher(old, mp);
            }
            attribute_validity.put(attribute, mp);
        }
    }

    public static class RegexMatcher extends MatcherPattern {

        private java.util.regex.Matcher matcher;

        public RegexMatcher(String pattern, int flags) {
            matcher = Pattern.compile(pattern, flags).matcher("");
        }

        public boolean matches(String value, Output<String> reason) {
            matcher.reset(value.toString());
            boolean result = matcher.matches();
            if (!result && reason != null) {
                reason.value = RegexUtilities.showMismatch(matcher, value.toString());
            }
            return result;
        }

        @Override
        public String _getPattern() {
            return matcher.toString();
        }
    }

    private static EnumMap<LocaleSpecific, Set<String>> LOCALE_SPECIFIC = null;

    /** WARNING, not thread-safe. Needs cleanup **/
    public static void setLocaleSpecifics(EnumMap<LocaleSpecific, Set<String>> newValues) {
        LOCALE_SPECIFIC = newValues;
    }

    public static class LocaleSpecificMatcher extends MatcherPattern {
        final LocaleSpecific ls;

        public LocaleSpecificMatcher(LocaleSpecific ls) {
            this.ls = ls;
        }

        public static LocaleSpecificMatcher getInstance(String value) {
            return new LocaleSpecificMatcher(LocaleSpecific.valueOf(value));
        }

        public boolean matches(String value) {
            return LOCALE_SPECIFIC.get(ls).contains(value);
        }

        static final int MAX_STRING = 64;

        public boolean matches(String value, Output<String> reason) {
            boolean result = LOCALE_SPECIFIC.get(ls).contains(value);
            if (!result && reason != null) {
                reason.value = "∉ " + getPattern();
            }
            return result;
        }

        @Override
        public String _getPattern() {
            return LOCALE_SPECIFIC.get(ls).toString();
        }
    }

    static final int MAX_STRING = 64;

    public static class CollectionMatcher extends MatcherPattern {
        private final Collection<String> collection;

        public CollectionMatcher(Collection<String> collection) {
            this.collection = Collections.unmodifiableCollection(new LinkedHashSet<>(collection));
        }
        @Override
        public boolean matches(String value, Output<String> reason) {
            boolean result = collection.contains(value);
            if (!result && reason != null) {
                reason.value = "∉ " + getPattern();
            }
            return result;
        }
        @Override
        public String _getPattern() {
            return collection.toString();
        }
    }

    public static class UnicodeSetMatcher extends MatcherPattern {
        private final UnicodeSet collection;

        public UnicodeSetMatcher(UnicodeSet collection) {
            this.collection = collection.freeze();
        }
        @Override
        public boolean matches(String value, Output<String> reason) {
            final UnicodeSet valueSet = new UnicodeSet(value);
            boolean result = collection.containsAll(valueSet);
            if (!result && reason != null) {
                reason.value = "∉ " + getPattern();
            }
            return result;
        }
        @Override
        public String _getPattern() {
            return collection.toPattern(false);
        }
    }

    public static class OrMatcher extends MatcherPattern {
        private final MatcherPattern[] operands;

        public OrMatcher(MatcherPattern... operands) {
            this.operands = operands;
        }

        public boolean matches(String value, Output<String> reason) {
            for (MatcherPattern operand : operands) {
                if (operand.matches(value, reason)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String _getPattern() {
            StringBuffer result = new StringBuffer();
            for (MatcherPattern operand : operands) {
                if (result.length() != 0) {
                    result.append('|');
                }
                result.append(operand._getPattern());
            }
            return result.toString();
        }
    }

    public static class ListMatcher extends MatcherPattern {
        private MatcherPattern other;

        public ListMatcher(MatcherPattern other) {
            this.other = other;
        }

        public boolean matches(String value, Output<String> reason) {
            List<String> values = SPACE.splitToList(value);
            if (values.isEmpty()) return true;
            for (String valueItem : values) {
                if (!other.matches(valueItem, reason)) {
                    if (reason != null) {
                        reason.value = "«" +valueItem + "» ∉ " + other.getPattern();
                    }
                    return false;
                }
            }
            return true;
        }

        @Override
        public String _getPattern() {
            return "List of " + other._getPattern();
        }
    }

    public static class LocaleMatcher extends MatcherPattern {
        //final ObjectMatcherReason grandfathered = variables.get("$grandfathered").matcher;
        final MatcherPattern language;
        final MatcherPattern script = variables.get("$_script");
        final MatcherPattern territory = variables.get("$_region");
        final MatcherPattern variant = variables.get("$_variant");
        final LocaleIDParser lip = new LocaleIDParser();

        public static LocaleMatcher REGULAR = new LocaleMatcher(false);
        public static LocaleMatcher ALL_LANGUAGES = new LocaleMatcher(true);

        private LocaleMatcher(boolean allowAll) {
            language = allowAll 
                ? variables.get("$_language") 
                    : variables.get("$_language_plus");
        }

        public boolean matches(String value, Output<String> reason) {
//            if (grandfathered.matches(value, reason)) {
//                return true;
//            }
            lip.set((String) value);
            String field = lip.getLanguage();
            if (!language.matches(field, reason)) {
                if (reason != null) {
                    reason.value = "invalid base language";
                }
                return false;
            }
            field = lip.getScript();
            if (field.length() != 0 && !script.matches(field, reason))  {
                if (reason != null) {
                    reason.value = "invalid script";
                }
                return false;
            }
            field = lip.getRegion();
            if (field.length() != 0 && !territory.matches(field, reason))  {
                if (reason != null) {
                    reason.value = "invalid region";
                }
                return false;
            }
            String[] fields = lip.getVariants();
            for (int i = 0; i < fields.length; ++i) {
                if (!variant.matches(fields[i], reason))  {
                    if (reason != null) {
                        reason.value = "invalid variant";
                    }
                    return false;
                }
            }
            return true;
        }

        @Override
        public String _getPattern() {
            return "Unicode_Language_Subtag";
        }
    } 

    /**
     * return Status
     * @param attribute_validity
     * @param attribute
     * @param attributeValue
     * @param result
     * @return
     */
    private static Status check(Map<String, MatcherPattern> attribute_validity, 
        String element, String attribute, String attributeValue,
        Output<String> reason) {

        if (attribute_validity == null) {
            return Status.noTest; // no test
        }
        MatcherPattern matcherPattern = attribute_validity.get(attribute);
        if (matcherPattern == null) {
            return Status.noTest; // no test
        }
        if (matcherPattern.matches(attributeValue, reason)) {
            return Status.ok;
        }
        return Status.illegal;
    }

    public static String getElementLine(DtdType dtdType, String element, String attribute, String attributeValues) {
        return "<attributeValues"
            + " dtds='" + dtdType + "\'" 
            + " elements='" + element + "\'" 
            + " attributes='" + attribute + "\'" 
            + " type='TODO\'>"
            + attributeValues
            + "</attributeValues>";
    }

    public static Status check(DtdData dtdData, String element, String attribute, String attributeValue, Output<String> reason) {
        if (dtdData.isDeprecated(element, attribute, attributeValue)) {
            return Status.deprecated;
        }
        Status haveTest = check(common_attribute_validity, element, attribute, attributeValue, reason);

        if (haveTest == Status.noTest) {
            final Map<String, Map<String, MatcherPattern>> element_attribute_validity = dtd_element_attribute_validity.get(dtdData.dtdType);
            if (element_attribute_validity == null) {
                return Status.noTest;
            }

            Map<String, MatcherPattern> attribute_validity = element_attribute_validity.get(element);
            if (attribute_validity == null) {
                return Status.noTest;
            }

            haveTest = check(attribute_validity, element, attribute, attributeValue, reason);
        }
        return haveTest;
    }

    public static Set<R3<DtdType, String, String>> getTodoTests() {
        Set<Row.R3<DtdType,String,String>> result = new LinkedHashSet<>();
        for (Entry<DtdType, Map<String, Map<String, MatcherPattern>>> entry1 : dtd_element_attribute_validity.entrySet()) {
            for (Entry<String, Map<String, MatcherPattern>> entry2 : entry1.getValue().entrySet()) {
                for (Entry<String, MatcherPattern> entry3 : entry2.getValue().entrySet()) {
                    if (entry3.getValue() == NOT_DONE_YET) {
                        result.add(Row.of(entry1.getKey(), entry2.getKey(), entry3.getKey()));
                    }
                }
            }
        }
        return result;
    }

    public static Set<AttributeValidityInfo> getReadFailures() {
        return Collections.unmodifiableSet(failures);
    }

}
