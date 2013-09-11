package org.unicode.cldr.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.util.CollectionUtilities.ObjectMatcher;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.UnicodeSet;

public class CheckAttributeValues extends FactoryCheckCLDR {
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
    SupplementalDataInfo supplementalData;

    boolean isEnglish;
    PluralInfo pluralInfo;

    XPathParts parts = new XPathParts(null, null);
    static final UnicodeSet DIGITS = new UnicodeSet("[0-9]").freeze();

    public CheckAttributeValues(Factory factory) {
        super(factory);
    }

    public CheckCLDR handleCheck(String path, String fullPath, String value, Map<String, String> options,
        List<CheckStatus> result) {
        if (fullPath == null) return this; // skip paths that we don't have
        if (fullPath.indexOf('[') < 0) return this; // skip paths with no attributes
        String locale = getCldrFileToCheck().getSourceLocaleID(path, null);

        // skip paths that are not in the immediate locale
        if (!getCldrFileToCheck().getLocaleID().equals(locale)) {
            return this;
        }
        parts.set(fullPath);
        for (int i = 0; i < parts.size(); ++i) {
            if (parts.getAttributeCount(i) == 0) continue;
            Map<String, String> attributes = parts.getAttributes(i);
            String element = parts.getElement(i);

            Map<String, MatcherPattern> attribute_validity = element_attribute_validity.get(element);
            for (Iterator<String> it = attributes.keySet().iterator(); it.hasNext();) {
                String attribute = it.next();
                String attributeValue = attributes.get(attribute);
                // check the common attributes first
                check(common_attribute_validity, attribute, attributeValue, result);
                // then for the specific element
                check(attribute_validity, attribute, attributeValue, result);

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

    private void check(Map<String, MatcherPattern> attribute_validity, String attribute, String attributeValue,
        List<CheckStatus> result) {
        if (attribute_validity == null) return; // no test
        MatcherPattern matcherPattern = attribute_validity.get(attribute);
        if (matcherPattern == null) return; // no test
        if (matcherPattern.matcher.matches(attributeValue)) return;
        // special check for deprecated codes
        String replacement = getReplacement(matcherPattern.value, attributeValue);
        if (replacement != null) {
            if (isEnglish) return; // don't flag English
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

    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Map<String, String> options,
        List<CheckStatus> possibleErrors) {
        if (cldrFileToCheck == null) return this;
        if (Phase.FINAL_TESTING == getPhase() || Phase.BUILD == getPhase()) {
            setSkipTest(false); // ok
        } else {
            setSkipTest(true);
            return this;
        }

        supplementalData = SupplementalDataInfo.getInstance(cldrFileToCheck.getSupplementalDirectory());
        pluralInfo = supplementalData.getPlurals(PluralType.cardinal, cldrFileToCheck.getLocaleID());
        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        isEnglish = "en".equals(localeIDParser.set(cldrFileToCheck.getLocaleID()).getLanguage());
        synchronized (elementOrder) {
            if (!initialized) {
                CLDRFile metadata = getFactory().getSupplementalMetadata();
                getMetadata(metadata, supplementalData);
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

    private void getMetadata(CLDRFile metadata, SupplementalDataInfo sdi) {
        // sorting is expensive, but we need it here.
        for (Iterator<String> it = metadata.iterator(null, CLDRFile.ldmlComparator); it.hasNext();) {
            String path = it.next();
            String value = metadata.getStringValue(path);
            path = metadata.getFullXPath(path);
            parts.set(path);
            String lastElement = parts.getElement(-1);
            if (lastElement.equals("elementOrder")) {
                elementOrder.addAll(Arrays.asList(value.trim().split("\\s+")));
            } else if (lastElement.equals("attributeOrder")) {
                attributeOrder.addAll(Arrays.asList(value.trim().split("\\s+")));
            } else if (lastElement.equals("suppress")) {
                // skip for now
            } else if (lastElement.equals("serialElements")) {
                // skip for now
            } else if (lastElement.equals("attributes")) {
                // skip for now
            } else if (lastElement.equals("variable")) {
                // String oldValue = value;
                // value = variableReplacer.replace(value);
                // if (!value.equals(oldValue)) System.out.println("\t" + oldValue + " => " + value);
                Map<String, String> attributes = parts.getAttributes(-1);
                MatcherPattern mp = getMatcherPattern(value, attributes, path, sdi);
                if (mp != null) {
                    String id = attributes.get("id");
                    variables.put(id, mp);
                    // variableReplacer.add(id, value);
                }
            } else if (lastElement.equals("attributeValues")) {
                try {
                    Map<String, String> attributes = parts.getAttributes(-1);

                    MatcherPattern mp = getMatcherPattern(value, attributes, path, sdi);
                    if (mp == null) {
                        // System.out.println("Failed to make matcher for: " + value + "\t" + path);
                        continue;
                    }
                    String[] attributeList = (attributes.get("attributes")).trim().split("\\s+");
                    String elementsString = (String) attributes.get("elements");
                    if (elementsString == null) {
                        addAttributes(attributeList, common_attribute_validity, mp);
                    } else {
                        String[] elementList = elementsString.trim().split("\\s+");
                        for (int i = 0; i < elementList.length; ++i) {
                            String element = elementList[i];
                            // System.out.println("\t" + element);
                            Map<String, MatcherPattern> attribute_validity = element_attribute_validity.get(element);
                            if (attribute_validity == null)
                                element_attribute_validity.put(element,
                                    attribute_validity = new TreeMap<String, MatcherPattern>());
                            addAttributes(attributeList, attribute_validity, mp);
                        }
                    }

                } catch (RuntimeException e) {
                    System.err
                        .println("Problem with: " + path + ", \t" + value);
                    e.printStackTrace();
                }
            } else if (lastElement.equals("version")) {
                // skip for now
            } else if (lastElement.equals("generation")) {
                // skip for now
            } else if (lastElement.endsWith("Alias")) {
                String code = "$" + lastElement.substring(0, lastElement.length() - 5);
                Map<String, String> type_replacement = code_type_replacement.get(code);
                if (type_replacement == null) {
                    code_type_replacement.put(code, type_replacement = new TreeMap<String, String>());
                }
                Map<String, String> attributes = parts.getAttributes(-1);
                String type = attributes.get("type");
                String replacement = attributes.get("replacement");
                if (replacement == null) {
                    replacement = "";
                }
                type_replacement.put(type, replacement);
            } else if (lastElement.equals("territoryAlias")) {
                // skip for now
            } else if (lastElement.equals("deprecatedItems")) {
                // skip for now
            } else if (lastElement.endsWith("Coverage")) {
                // skip for now
            } else if (lastElement.endsWith("skipDefaultLocale")) {
                // skip for now
            } else if (lastElement.endsWith("defaultContent")) {
                // skip for now
            } else if (lastElement.endsWith("distinguishingItems")) {
                // skip for now
            } else if (lastElement.endsWith("blockingItems")) {
                // skip for now
            } else {
                System.out.println("Unknown final element: " + path);
            }
        }
    }

    private MatcherPattern getBcp47MatcherPattern(SupplementalDataInfo sdi, String key) {
        MatcherPattern m = new MatcherPattern();
        Relation<R2<String, String>, String> bcp47Aliases = sdi.getBcp47Aliases();
        Set<String> values = new TreeSet<String>();
        for (String value : sdi.getBcp47Keys().getAll(key)) {
            if (key.equals("cu")) { // Currency codes are in upper case.
                values.add(value.toUpperCase());
            } else {
                values.add(value);
            }
            R2<String, String> keyValue = R2.of(key, value);
            Set<String> aliases = bcp47Aliases.getAll(keyValue);
            if (aliases != null) {
                values.addAll(aliases);
            }
        }

        // Special case exception for generic calendar, since we don't want to expose it in bcp47
        if (key.equals("ca")) {
            values.add("generic");
        }

        m.value = key;
        m.pattern = values.toString();
        m.matcher = new CollectionMatcher().set(values);
        return m;

    }

    private MatcherPattern getMatcherPattern(String value, Map<String, String> attributes, String path,
        SupplementalDataInfo sdi) {
        String typeAttribute = attributes.get("type");
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
        if ("choice".equals(typeAttribute)
            || "given".equals(attributes.get("order"))) {
            result.matcher = new CollectionMatcher()
                .set(new HashSet<String>(Arrays.asList(value.trim().split("\\s+"))));
        } else if ("bcp47".equals(typeAttribute)) {
            result = getBcp47MatcherPattern(sdi, value);
        } else if ("regex".equals(typeAttribute)) {
            result.matcher = new RegexMatcher().set(value, Pattern.COMMENTS); // Pattern.COMMENTS to get whitespace
        } else if ("locale".equals(typeAttribute)) {
            result.matcher = LocaleMatcher.make();
        } else if ("notDoneYet".equals(typeAttribute) || "notDoneYet".equals(value)) {
            result.matcher = new RegexMatcher().set(".*", Pattern.COMMENTS);
        } else {
            System.out.println("unknown type; value: <" + value + ">,\t" + typeAttribute + ",\t" + attributes + ",\t"
                + path);
            return null;
        }
        return result;
    }

    private void addAttributes(String[] attributes, Map<String, MatcherPattern> attribute_validity, MatcherPattern mp) {
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
            matcher = Pattern.compile(pattern).matcher("");
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