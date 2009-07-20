package org.unicode.cldr.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;

import com.ibm.icu.dev.test.util.CollectionUtilities;
import com.ibm.icu.dev.test.util.CollectionUtilities.ObjectMatcher;

public class CheckAttributeValues extends CheckCLDR {
    static LinkedHashSet elementOrder = new LinkedHashSet();
    static LinkedHashSet attributeOrder = new LinkedHashSet();
    static LinkedHashSet serialElements = new LinkedHashSet();
    //static Map suppress = new HashMap();
    // TODO change these to HashMap, once this is all debugged.
    static Map<String,Map<String,MatcherPattern>> element_attribute_validity = new TreeMap<String,Map<String,MatcherPattern>>();
    static Map<String,MatcherPattern> common_attribute_validity = new TreeMap<String,MatcherPattern>();
    static Map variables = new TreeMap();
//    static VariableReplacer variableReplacer = new VariableReplacer(); // note: this can be coalesced with the above -- to do later.
    static boolean initialized = false;
    static LocaleMatcher localeMatcher;
    static Map code_type_replacement = new TreeMap();
    SupplementalDataInfo supplementalData;
    
    boolean isEnglish;
    PluralInfo pluralInfo;

    XPathParts parts = new XPathParts(null, null);
    
    public CheckCLDR handleCheck(String path, String fullPath, String value, Map<String, String> options, List<CheckStatus> result) {
      if (fullPath == null) return this; // skip paths that we don't have
      if (fullPath.indexOf('[') < 0) return this; // skip paths with no attributes
        parts.set(fullPath);
        for (int i = 0; i < parts.size(); ++i) {
        	if (parts.getAttributeCount(i) == 0) continue;
            Map attributes = parts.getAttributes(i);
            String element = parts.getElement(i);

            Map<String,MatcherPattern> attribute_validity = element_attribute_validity.get(element);
            for (Iterator it = attributes.keySet().iterator(); it.hasNext();) {
                String attribute = (String) it.next();
                String attributeValue = (String) attributes.get(attribute);
                // check the common attributes first
                check(common_attribute_validity, attribute, attributeValue, result);
                // then for the specific element
                check(attribute_validity, attribute, attributeValue, result);
                if (attribute.equals("count")) {
                  final Count countValue = PluralInfo.Count.valueOf(attributeValue);
                  if (!pluralInfo.getCountToExamplesMap().keySet().contains(countValue)) {
                    result.add(new CheckStatus()
                    .setCause(this).setMainType(CheckStatus.errorType).setSubtype(Subtype.illegalPlural)
                    .setMessage("Illegal plural value {0}; must be one of: {1}", 
                            new Object[]{countValue, pluralInfo.getCountToExamplesMap().keySet()}));          
                  }
                }
            }
            
        }
        return this;
    }
    private void check(Map<String,MatcherPattern> attribute_validity, String attribute, String attributeValue, List result) {
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
                                new Object[]{attribute, attributeValue}));
            } else {
                result.add(new CheckStatus()
                .setCause(this).setMainType(CheckStatus.warningType).setSubtype(Subtype.deprecatedAttributeWithReplacement)
                        .setMessage("Deprecated Attribute Value {0}={1}. Consider removing, and possibly modifying the related value for {2}.", 
                                new Object[]{attribute, attributeValue, replacement}));
            }
        } else {
            result.add(new CheckStatus()
            .setCause(this).setMainType(CheckStatus.errorType).setSubtype(Subtype.unexpectedAttributeValue)
                    .setMessage("Unexpected Attribute Value {0}={1}: expected: {2}", 
                            new Object[]{attribute, attributeValue, matcherPattern.pattern}));
        }
    }
    
    /**
     * Returns replacement, or null if there is none. "" if the code is deprecated, but without a replacement.
     * Input is of the form $language
     * @return
     */
    String getReplacement(String value, String attributeValue) {
        Map type_replacement = (Map) code_type_replacement.get(value);
        if (type_replacement == null) {
            return null;
        }
        String result = (String) type_replacement.get(attributeValue);
        return result;
    }
  
    
    LocaleIDParser localeIDParser = new LocaleIDParser();
    
    public CheckCLDR setCldrFileToCheck(CLDRFile cldrFileToCheck, Map<String,String> options, List<CheckStatus> possibleErrors) {
        if (cldrFileToCheck == null) return this;
        if (Phase.FINAL_TESTING == getPhase()) {
          setSkipTest(false); // ok
        } else {
          setSkipTest(true);
          return this;
        }
        
        supplementalData = SupplementalDataInfo.getInstance(cldrFileToCheck.getSupplementalDirectory());
        pluralInfo = supplementalData.getPlurals(cldrFileToCheck.getLocaleID());
        
        super.setCldrFileToCheck(cldrFileToCheck, options, possibleErrors);
        isEnglish = "en".equals(localeIDParser.set(cldrFileToCheck.getLocaleID()).getLanguage());
        synchronized (elementOrder) {
            if (!initialized) {
                CLDRFile metadata = cldrFileToCheck.getSupplementalMetadata();
                getMetadata(metadata);
                initialized = true;
                for (Iterator it = missing.iterator(); it.hasNext();) {
                    System.out.println("\t\t\t<variable id=\"" + it.next() + "\" type=\"list\">stuff</variable>");
                }
                localeMatcher = LocaleMatcher.make();
            }
        }
        if (!localeMatcher.matches(cldrFileToCheck.getLocaleID())) {
            possibleErrors.add(new CheckStatus()
            .setCause(this).setMainType(CheckStatus.errorType).setSubtype(Subtype.invalidLocale)
                    .setMessage("Invalid Locale {0}", 
                            new Object[]{cldrFileToCheck.getLocaleID()}));
            
        }
        return this;
    }
    
    private void getMetadata(CLDRFile metadata) {
        String lastPath = "//ldml";
        //checkTransitivity(metadata.iterator(), CLDRFile.ldmlComparator);
        // sorting is expensive, but we need it here.
        for (Iterator it = metadata.iterator(null, CLDRFile.ldmlComparator); it.hasNext();) {
            String path = (String) it.next();
            String value = metadata.getStringValue(path);
            path = metadata.getFullXPath(path);
            if (false) {
                int comp = CLDRFile.ldmlComparator.compare(lastPath, path);
                System.out.println(comp + "\t" + path);
                System.out.flush();
                lastPath = path;
            }
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
//            	String oldValue = value;
//            	value = variableReplacer.replace(value);
//            	if (!value.equals(oldValue)) System.out.println("\t" + oldValue + " => " + value);
                Map attributes = parts.getAttributes(-1);
                MatcherPattern mp = getMatcherPattern(value, attributes, path);
                if (mp != null) {
                    String id = (String) attributes.get("id");
                    variables.put(id, mp);
//                    variableReplacer.add(id, value);
                }
            } else if (lastElement.equals("attributeValues")) {
                try {
                    String originalValue = value;
                    Map attributes = parts.getAttributes(-1);
                    
                    MatcherPattern mp = getMatcherPattern(value, attributes, path);
                    if (mp == null) {
                        //System.out.println("Failed to make matcher for: " + value + "\t" + path);
                        continue;
                    }
                    String[] attributeList = ((String) attributes
                            .get("attributes")).trim().split("\\s+");
                    String elementsString = (String) attributes.get("elements");
                    if (elementsString == null) {
                        addAttributes(attributeList, common_attribute_validity, mp);
                    } else {
                        String[] elementList = elementsString.trim().split("\\s+");
                        for (int i = 0; i < elementList.length; ++i) {
                            String element = elementList[i];
                            // System.out.println("\t" + element);
                            Map attribute_validity = (Map) element_attribute_validity
                            .get(element);
                            if (attribute_validity == null)
                                element_attribute_validity.put(element,
                                        attribute_validity = new TreeMap());
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
               String code = "$" + lastElement.substring(0,lastElement.length()-5);
                Map type_replacement = (Map)code_type_replacement.get(code);
                if (type_replacement == null) code_type_replacement.put(code, type_replacement = new TreeMap());
                Map attributes = parts.getAttributes(-1);
                String type = (String) attributes.get("type");
                String replacement = (String) attributes.get("replacement");
                if (replacement == null) replacement = "";
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
    private void checkTransitivity(Iterator iterator, Comparator ldmlComparator) {
        Set set1 = new TreeSet();
        CollectionUtilities.addAll(iterator, set1);
        for (Iterator it = set1.iterator(); it.hasNext();) {
            System.out.println(it.next());
        }
        System.out.flush();
        Set set = new TreeSet(ldmlComparator);
        set.addAll(set1);
        ArrayList l = new ArrayList(set);
        String a, b, c;
        for (int i = 1; i < l.size(); ++i) {
            a = (String) l.get(i-1);
            b = (String) l.get(i);
            int comp = ldmlComparator.compare(a, b);
            System.out.println(comp + "\t" + b);
            if (comp > 0) {
                System.out.println("FAILED");
                ldmlComparator.compare(a, b);
            }
        }
        System.out.flush();
        // transitivity means A <= B && B <= C implies A <= C
        // that is, a bad case is A <= B && B <= C && A > C
        // we just see if we can find some case, with brute force
        Random r = new Random();
        a = (String) l.get(r.nextInt(l.size()));
        b = (String) l.get(r.nextInt(l.size()));
        for (int i = 0; i < 1000000; ++i) {
            c = b;
            b = a;
            a = (String) l.get(r.nextInt(l.size()));
            if (ldmlComparator.compare(a, b) <= 0
                    && ldmlComparator.compare(b, c) <= 0
                    && ldmlComparator.compare(a, c) > 0) {
                System.out.println("FAILED");
                System.out.println(a);
                System.out.println(b);
                System.out.println(c);
                ldmlComparator.compare(a, b);
                ldmlComparator.compare(b, c);
                ldmlComparator.compare(a, c);
            }
        }
        System.out.flush();
    }
    static Set missing = new TreeSet();
    
    private MatcherPattern getMatcherPattern(String value, Map attributes, String path) {
        String typeAttribute = (String) attributes.get("type");
        MatcherPattern result = (MatcherPattern) variables.get(value);
        if (result != null) {
            MatcherPattern temp = new MatcherPattern();
            temp.pattern = result.pattern;
            temp.matcher = result.matcher;
            temp.type = typeAttribute;
            temp.value = value;
            result = temp;
            if ("list".equals(typeAttribute)) {
                temp.matcher = new ListMatcher().set(result.matcher);
            }
            return result;
        }
        
        result = new MatcherPattern();
        result.pattern = value;
        result.type = typeAttribute;
        result.value = value;
        if ("choice".equals(typeAttribute)
                || "given".equals(attributes.get("order"))) {
            result.matcher = new CollectionMatcher().set(new HashSet(Arrays.asList(value.trim().split("\\s+"))));
        } else if ("regex".equals(typeAttribute)) {
            result.matcher = new RegexMatcher().set(value, Pattern.COMMENTS); // Pattern.COMMENTS to get whitespace	
        } else if ("locale".equals(typeAttribute)) {
            result.matcher = LocaleMatcher.make();
        } else if ("notDoneYet".equals(typeAttribute) || "notDoneYet".equals(value)) {
           result.matcher = new RegexMatcher().set(".*", Pattern.COMMENTS);
        } else {
            System.out.println("unknown type; value: <" + value + ">,\t" + typeAttribute + ",\t" + attributes + ",\t" + path);
            return null;
        }
        return result;
    }
    
    private void addAttributes(String[] attributes, Map attribute_validity, MatcherPattern mp) {
        for (int i = 0; i < attributes.length; ++i) {
            String attribute = attributes[i];
            MatcherPattern old = (MatcherPattern) attribute_validity.get(attribute);
            if (old != null) {
                mp.matcher = new OrMatcher().set(old.matcher, mp.matcher);
                mp.pattern = old.pattern + " OR " + mp.pattern;
            }
            attribute_validity.put(attribute, mp);
        }
    }
    
    private static class MatcherPattern {
        public String value;
        ObjectMatcher matcher;
        String pattern;
        String type;
        public String toString() {
            return matcher.getClass().getName() + "\t" + pattern;
        }
    }
    public static class RegexMatcher implements ObjectMatcher {
        private java.util.regex.Matcher matcher;
        public ObjectMatcher set(String pattern) {
            matcher = Pattern.compile(pattern).matcher("");
            return this;
        }
        public ObjectMatcher set(String pattern, int flags) {
            matcher = Pattern.compile(pattern, flags).matcher("");
            return this;
        }
        public boolean matches(Object value) {
            matcher.reset(value.toString());
            return matcher.matches();
        }
    }
    public static class CollectionMatcher implements ObjectMatcher {
        private Collection collection;
        public ObjectMatcher set(Collection collection) {
            this.collection = collection;
            return this;
        }
        public boolean matches(Object value) {
            return collection.contains(value);
        }
    }
    public static class OrMatcher implements ObjectMatcher {
        private ObjectMatcher a;
        private ObjectMatcher b;
        public ObjectMatcher set(ObjectMatcher a, ObjectMatcher b) {
            this.a = a;
            this.b = b;
            return this;
        }
        public boolean matches(Object value) {
            return a.matches(value) || b.matches(value);
        }
    }
    public static class ListMatcher implements ObjectMatcher {
        private ObjectMatcher other;
        public ObjectMatcher set(ObjectMatcher other) {
            this.other = other;
            return this;
        }
        public boolean matches(Object value) {
            String[] values = ((String)value).trim().split("\\s+");
            if (values.length == 1 && values[0].length() == 0) return true;
            for (int i = 0; i < values.length; ++i) {
                if (!other.matches(values[i])) {
                    return false;
                }
            }
            return true;
        }
    }
    public static class LocaleMatcher implements ObjectMatcher {
        ObjectMatcher grandfathered = ((MatcherPattern)variables.get("$grandfathered")).matcher;
        ObjectMatcher language = ((MatcherPattern)variables.get("$language")).matcher;
        ObjectMatcher script = ((MatcherPattern)variables.get("$script")).matcher;
        ObjectMatcher territory = ((MatcherPattern)variables.get("$territory")).matcher;
        ObjectMatcher variant = ((MatcherPattern)variables.get("$variant")).matcher;
        LocaleIDParser lip = new LocaleIDParser();
        static LocaleMatcher singleton = null;
        static Object sync = new Object();
        private LocaleMatcher(boolean b){}
        
        public static LocaleMatcher make() {
            synchronized (sync) {
                if (singleton == null) {
                    singleton = new LocaleMatcher(true);
                }
            }
            return singleton;
        }
        
        public boolean matches(Object value) {
            if (grandfathered.matches(value)) return true;
            lip.set((String)value);
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