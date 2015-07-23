package org.unicode.cldr.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.unicode.cldr.util.SupplementalDataInfo.AttributeValidityInfo;

import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.dev.util.CollectionUtilities.ObjectMatcher;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.UnicodeSet;

public class AttributeValueValidity {

    public enum Status {ok, deprecated, illegal, noTest}

    private static final Set<DtdType> ALL_DTDs = Collections.unmodifiableSet(EnumSet.allOf(DtdType.class));

    private static final SupplementalDataInfo supplementalData = CLDRConfig.getInstance().getSupplementalDataInfo();

    private static Map<DtdType,Map<String, Map<String, MatcherPattern>>> dtd_element_attribute_validity = new EnumMap<>(DtdType.class);
    private static Map<String, MatcherPattern> common_attribute_validity = new HashMap<String, MatcherPattern>();
    private static Map<String, MatcherPattern> variables = new HashMap<String, MatcherPattern>();
    //private static Map<String, Map<String, String>> code_type_replacement = new TreeMap<String, Map<String, String>>();
    private static Map<String, Set<String>> BCP47_KEY_VALUES;
    private static final ObjectMatcher<String> NOT_DONE_YET = new RegexMatcher().set(".*", Pattern.COMMENTS);

    private static final boolean DEBUG = false;

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
            temp.put("x", Collections.<String>emptySet()); // Hack for 'x', private use.
        }
        BCP47_KEY_VALUES = Collections.unmodifiableMap(temp);

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
            } else {
                throw new IllegalArgumentException();
            }
        }
        //System.out.println("Variables: " + variables.keySet());

        try {
            Set<AttributeValidityInfo> rawAttributeValueInfo = supplementalData.getAttributeValidity();
            int x = 0;
            for (AttributeValidityInfo item : rawAttributeValueInfo) {
                //System.out.println(item);
                MatcherPattern mp = getMatcherPattern2(item.getType(), item.getValue());
                if (mp == null) {
                    if (DEBUG) {
                        System.out.println("Failed to make matcher for: " + item);
                    }
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
                                System.out.println("Illegal <attributeValues>, element not valid: element: " + element);
                            } else {
                                for (String attribute : attributeList) {
                                    DtdData.Attribute attributeInfo = elementInfo.getAttributeNamed(attribute);
                                    if (attributeInfo == null) {
                                        System.out.println("Illegal <attributeValues>, attribute not valid: element: " + element + ", attribute: " + attribute);
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
        } catch (Exception e) {
            e.printStackTrace();
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

    private static class MatcherPattern {
        public String value;
        ObjectMatcher<String> matcher;
        String pattern;

        public String toString() {
            return matcher.getClass().getName() + "\t" + pattern;
        }
    }

    private static MatcherPattern getBcp47MatcherPattern(String key) {
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

    enum MatcherTypes {
        choice,
        list,
        unicodeSet,
        regex,
        locale,
        bcp47,
        dayperiod,
        ordinal,
        plural,
        subdivision,
        TODO;
    }

    private static MatcherPattern getMatcherPattern2(String type, String value) {
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

        switch(MatcherTypes.valueOf(typeAttribute)) {
        case choice:
            result.matcher = new CollectionMatcher().set(new HashSet<String>(Arrays.asList(value.trim().split("\\s+"))));
            break;
        case unicodeSet:
            result.matcher = new UnicodeSetMatcher().set(new UnicodeSet(value));
            break;
        case bcp47:
            result = getBcp47MatcherPattern(value);
            break;
        case regex:
            result.matcher = new RegexMatcher().set(value, Pattern.COMMENTS); // Pattern.COMMENTS to get whitespace
            break;
        case locale:
            result.matcher = LocaleMatcher.make();
            break;
        case TODO:
        case plural:
        case dayperiod:
        case ordinal:
            result.matcher = NOT_DONE_YET;
            break;
        case list:
            // only use with variables
        default:
            return null;
        }
        return result;
    }

    private static void addAttributes(Set<String> attributes, Map<String, MatcherPattern> attribute_validity, MatcherPattern mp) {
        for (String attribute : attributes) {
            MatcherPattern old = attribute_validity.get(attribute);
            if (old != null) {
                mp.matcher = new OrMatcher().set(old.matcher, mp.matcher);
                mp.pattern = old.pattern + " OR " + mp.pattern;
            }
            attribute_validity.put(attribute, mp);
        }
    }

    public static class RegexMatcher implements ObjectMatcher<String> {

        private java.util.regex.Matcher matcher;

        public ObjectMatcher<String> set(String pattern) {
            matcher = Pattern.compile(pattern).matcher("");
            return this;
        }

        public ObjectMatcher<String> set(String pattern, int flags) {
            matcher = Pattern.compile(pattern, flags).matcher("");
            return this;
        }

        public boolean matches(String value) {
            matcher.reset(value.toString());
            boolean result = matcher.matches();
            if (!result && DEBUG) {
                System.out.println("Mismatched regex: " + RegexUtilities.showMismatch(matcher, value.toString()));
            }
            return result;
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

    public static class UnicodeSetMatcher implements ObjectMatcher<String> {
        private UnicodeSet collection;

        public ObjectMatcher<String> set(UnicodeSet collection) {
            this.collection = collection;
            return this;
        }

        public boolean matches(String value) {
            return collection.containsAll(new UnicodeSet(value));
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

    /**
     * return Status
     * @param attribute_validity
     * @param attribute
     * @param attributeValue
     * @param result
     * @return
     */
    private static Status check(Map<String, MatcherPattern> attribute_validity, 
        String element, String attribute, String attributeValue) {

        if (attribute_validity == null) {
            return Status.noTest; // no test
        }
        MatcherPattern matcherPattern = attribute_validity.get(attribute);
        if (matcherPattern == null) {
            return Status.noTest; // no test
        }
        if (matcherPattern.matcher.matches(attributeValue)) {
            return Status.ok;
        }
        return Status.illegal;
    }

    public static String getElementLine(DtdType dtdType, String element, String attribute, String attributeValues) {
        return "<attributeValues"
            + " dtds='" + dtdType + "\'" 
            + " elements='" + element + "\'" 
            + " attributes='" + attribute + "\'" 
            + " type='choice\'>"
            + attributeValues
            + "</attributeValues>";
    }

    public static Status check(DtdData dtdData, String element, String attribute, String attributeValue) {
        if (dtdData.isDeprecated(element, attribute, attributeValue)) {
            return Status.deprecated;
        }
        Status haveTest = check(common_attribute_validity, element, attribute, attributeValue);

        if (haveTest == Status.noTest) {
            final Map<String, Map<String, MatcherPattern>> element_attribute_validity = dtd_element_attribute_validity.get(dtdData.dtdType);
            if (element_attribute_validity == null) {
                return Status.noTest;
            }

            Map<String, MatcherPattern> attribute_validity = element_attribute_validity.get(element);
            if (attribute_validity == null) {
                return Status.noTest;
            }

            haveTest = check(attribute_validity, element, attribute, attributeValue);
        }
        return haveTest;
    }
}
