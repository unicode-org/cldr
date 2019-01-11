package org.unicode.cldr.util;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.text.Transform;

/**
 * An immutable object that contains the structure of a DTD.
 * @author markdavis
 */
public class DtdData extends XMLFileReader.SimpleHandler {
    private static final String COMMENT_PREFIX = System.lineSeparator() + "    ";
    private static final boolean SHOW_ALL = CldrUtility.getProperty("show_all", false);
    private static final boolean USE_SYNTHESIZED = false;

    private static final boolean DEBUG = false;
    private static final Pattern FILLER = PatternCache.get("[^-a-zA-Z0-9#_:]");

    private final Relation<String, Attribute> nameToAttributes = Relation.of(new TreeMap<String, Set<Attribute>>(), LinkedHashSet.class);
    private Map<String, Element> nameToElement = new HashMap<String, Element>();
    private MapComparator<String> elementComparator;
    private MapComparator<String> attributeComparator;

    public final Element ROOT;
    public final Element PCDATA = elementFrom("#PCDATA");
    public final Element ANY = elementFrom("ANY");
    public final DtdType dtdType;
    public final String version;
    private Element lastElement;
    private Attribute lastAttribute;
    private Set<String> preCommentCache;
    private DtdComparator dtdComparator;

    public enum AttributeStatus {
        distinguished, value, metadata
    }

    public enum Mode {
        REQUIRED("#REQUIRED"), OPTIONAL("#IMPLIED"), FIXED("#FIXED"), NULL("null");

        public final String source;

        Mode(String s) {
            source = s;
        }

        public static Mode forString(String mode) {
            for (Mode value : Mode.values()) {
                if (value.source.equals(mode)) {
                    return value;
                }
            }
            if (mode == null) {
                return NULL;
            }
            throw new IllegalArgumentException(mode);
        }
    }

    public enum AttributeType {
        CDATA, ID, IDREF, IDREFS, ENTITY, ENTITIES, NMTOKEN, NMTOKENS, ENUMERATED_TYPE
    }

    public static class Attribute implements Named {
        public final String name;
        public final Element element;
        public final Mode mode;
        public final String defaultValue;
        public final AttributeType type;
        public final Map<String, Integer> values;
        private final Set<String> commentsPre;
        private Set<String> commentsPost;
        private boolean isDeprecatedAttribute;
        private AttributeStatus attributeStatus = AttributeStatus.distinguished; // default unless reset by annotations
        private Set<String> deprecatedValues = Collections.emptySet();
        private MatchValue matchValue;
        private final Comparator<String> attributeValueComparator;

        private Attribute(DtdType dtdType, Element element2, String aName, Mode mode2, String[] split, String value2, Set<String> firstComment) {
            commentsPre = firstComment;
            element = element2;
            name = aName.intern();
            if (name.equals("draft") // normally never permitted on elements with children, but special cases...
                && !element.getName().equals("collation")
                && !element.getName().equals("transform")) {
                int elementChildrenCount = element.getChildren().size();
                if (elementChildrenCount > 1
                    || elementChildrenCount == 1 && !element.getChildren().keySet().iterator().next().getName().equals("cp")) {
                    isDeprecatedAttribute = true;
                    if (DEBUG) {
                        System.out.println(element.getName() + ":" + element.getChildren());
                    }
                }
            }
            mode = mode2;
            defaultValue = value2 == null ? null
                : value2.intern();
            AttributeType _type = AttributeType.ENUMERATED_TYPE;
            Map<String, Integer> _values = Collections.emptyMap();
            if (split.length == 1) {
                try {
                    _type = AttributeType.valueOf(split[0]);
                } catch (Exception e) {
                }
            }
            type = _type;

            if (_type == AttributeType.ENUMERATED_TYPE) {
                LinkedHashMap<String, Integer> temp = new LinkedHashMap<String, Integer>();
                for (String part : split) {
                    if (part.length() != 0) {
                        temp.put(part.intern(), temp.size());
                    }
                }
                _values = Collections.unmodifiableMap(temp);
            }
            values = _values;
            attributeValueComparator = getAttributeValueComparator(dtdType, element.name, name);
        }

        @Override
        public String toString() {
            return element.name + ":" + name;
        }

        public StringBuilder appendDtdString(StringBuilder b) {
            Attribute a = this;
            b.append("<!ATTLIST " + element.name + " " + a.name);
            boolean first;
            if (a.type == AttributeType.ENUMERATED_TYPE) {
                b.append(" (");
                first = true;
                for (String s : a.values.keySet()) {
                    if (deprecatedValues.contains(s)) {
                        continue;
                    }
                    if (first) {
                        first = false;
                    } else {
                        b.append(" | ");
                    }
                    b.append(s);
                }
                b.append(")");
            } else {
                b.append(' ').append(a.type);
            }
            if (a.mode != Mode.NULL) {
                b.append(" ").append(a.mode.source);
            }
            if (a.defaultValue != null) {
                b.append(" \"").append(a.defaultValue).append('"');
            }
            b.append(" >");
            return b;
        }

        public String features() {
            return (type == AttributeType.ENUMERATED_TYPE ? values.keySet().toString() : type.toString())
                + (mode == Mode.NULL ? "" : ", mode=" + mode)
                + (defaultValue == null ? "" : ", default=" + defaultValue);
        }

        @Override
        public String getName() {
            return name;
        }

        private static Splitter COMMA = Splitter.on(',').trimResults();

        public void addComment(String commentIn) {
            if (commentIn.startsWith("@")) {
                // there are exactly 2 cases: deprecated and ordered
                switch (commentIn) {
                case "@METADATA":
                    attributeStatus = AttributeStatus.metadata;
                    break;
                case "@VALUE":
                    attributeStatus = AttributeStatus.value;
                    break;
                case "@DEPRECATED":
                    isDeprecatedAttribute = true;
                    break;
                default:
                    int colonPos = commentIn.indexOf(':');
                    if (colonPos < 0) {
                        throw new IllegalArgumentException("Unrecognized annotation: " + commentIn);
                    }
                    String command = commentIn.substring(0, colonPos);
                    String argument = commentIn.substring(colonPos + 1);
                    switch(command) {
                    case "@DEPRECATED":
                        deprecatedValues = Collections.unmodifiableSet(new HashSet<>(COMMA.splitToList(argument)));
                        break;
                    case "@MATCH":
                        if (matchValue != null) {
                            throw new IllegalArgumentException("Confliting @MATCH: " + matchValue.getName() + " & " + argument);
                        }
                        matchValue = MatchValue.of(argument);
                        break;
                    default:
                        throw new IllegalArgumentException("Unrecognized annotation: " + commentIn);
                    }
                }
                return;
            }
            commentsPost = addUnmodifiable(commentsPost, commentIn.trim());
        }

        /**
         * Special version of identity; only considers name and name of element
         */
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Attribute)) {
                return false;
            }
            Attribute that = (Attribute) obj;
            return name.equals(that.name)
                && element.name.equals(that.element.name) // don't use plain element: circularity
                // not relevant to identity
                //                && Objects.equals(comment, that.comment)
                //                && mode.equals(that.mode)
                //                && Objects.equals(defaultValue, that.defaultValue)
                //                && type.equals(that.type)
                //                && values.equals(that.values)
                ;
        }

        /**
         * Special version of identity; only considers name and name of element
         */
        @Override
        public int hashCode() {
            return name.hashCode() * 37
                + element.name.hashCode() // don't use plain element: circularity
                // not relevant to identity
                //                ) * 37 + Objects.hashCode(comment)) * 37
                //                + mode.hashCode()) * 37
                //                + Objects.hashCode(defaultValue)) * 37
                //                + type.hashCode()) * 37
                //                + values.hashCode()
                ;
        }

        public boolean isDeprecated() {
            return isDeprecatedAttribute;
        }

        public boolean isDeprecatedValue(String value) {
            return deprecatedValues.contains(value);
        }

        public AttributeStatus getStatus() {
            return attributeStatus;
        }

        public ValueStatus getValueStatus(String value) {
            return deprecatedValues.contains(value) 
                ? ValueStatus.invalid
                    : type == AttributeType.ENUMERATED_TYPE 
                    ? (values.containsKey(value) 
                        ? ValueStatus.valid 
                            : ValueStatus.invalid)
                        : matchValue == null 
                        ? ValueStatus.unknown :
                            matchValue.is(value) 
                            ? ValueStatus.valid 
                                : ValueStatus.invalid;
        }
    }

    public enum ValueStatus {invalid, unknown, valid}

    private DtdData(DtdType type, String version) {
        this.dtdType = type;
        this.ROOT = elementFrom(type.rootType.toString());
        this.version = version;
    }

    private void addAttribute(String eName, String aName, String type, String mode, String value) {
        Attribute a = new Attribute(dtdType, nameToElement.get(eName), aName, Mode.forString(mode), FILLER.split(type), value, preCommentCache);
        preCommentCache = null;
        getAttributesFromName().put(aName, a);
        CldrUtility.putNew(a.element.attributes, a, a.element.attributes.size());
        lastElement = null;
        lastAttribute = a;
    }

    public enum ElementType {
        EMPTY, ANY, PCDATA("(#PCDATA)"), CHILDREN;
        public final String source;

        private ElementType(String s) {
            source = s;
        }

        private ElementType() {
            source = name();
        }
    }

    interface Named {
        String getName();
    }

    public enum ElementStatus {
        regular, metadata
    }

    public static class Element implements Named {
        public final String name;
        private String rawModel;
        private ElementType type;
        private final Map<Element, Integer> children = new LinkedHashMap<Element, Integer>();
        private final Map<Attribute, Integer> attributes = new LinkedHashMap<Attribute, Integer>();
        private Set<String> commentsPre;
        private Set<String> commentsPost;
        private String model;
        private boolean isOrderedElement;
        private boolean isDeprecatedElement;
        private ElementStatus elementStatus = ElementStatus.regular;

        private Element(String name2) {
            name = name2.intern();
        }

        private void setChildren(DtdData dtdData, String model, Set<String> precomments) {
            this.commentsPre = precomments;
            rawModel = model;
            this.model = clean(model);
            if (model.equals("EMPTY")) {
                type = ElementType.EMPTY;
                return;
            }
            type = ElementType.CHILDREN;
            for (String part : FILLER.split(model)) {
                if (part.length() != 0) {
                    if (part.equals("#PCDATA")) {
                        type = ElementType.PCDATA;
                    } else if (part.equals("ANY")) {
                        type = ElementType.ANY;
                    } else {
                        CldrUtility.putNew(children, dtdData.elementFrom(part), children.size());
                    }
                }
            }
            if ((type == ElementType.CHILDREN) == (children.size() == 0)
                && !model.startsWith("(#PCDATA|cp")) {
                throw new IllegalArgumentException("CLDR does not permit Mixed content. " + name + ":" + model);
            }
        }

        static final Pattern CLEANER1 = PatternCache.get("([,|(])(?=\\S)");
        static final Pattern CLEANER2 = PatternCache.get("(?=\\S)([|)])");

        private String clean(String model2) {
            // (x) -> ( x );
            // x,y -> x, y
            // x|y -> x | y
            String result = CLEANER1.matcher(model2).replaceAll("$1 ");
            result = CLEANER2.matcher(result).replaceAll(" $1");
            return result.equals(model2)
                ? model2
                    : result; // for debugging
        }

        public boolean containsAttribute(String string) {
            for (Attribute a : attributes.keySet()) {
                if (a.name.equals(string)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return name;
        }

        public String toDtdString() {
            return "<!ELEMENT " + name + " " + getRawModel() + " >";
        }

        public ElementType getType() {
            return type;
        }

        public Map<Element, Integer> getChildren() {
            return Collections.unmodifiableMap(children);
        }

        public Map<Attribute, Integer> getAttributes() {
            return Collections.unmodifiableMap(attributes);
        }

        @Override
        public String getName() {
            return name;
        }

        public Element getChildNamed(String string) {
            for (Element e : children.keySet()) {
                if (e.name.equals(string)) {
                    return e;
                }
            }
            return null;
        }

        public Attribute getAttributeNamed(String string) {
            for (Attribute a : attributes.keySet()) {
                if (a.name.equals(string)) {
                    return a;
                }
            }
            return null;
        }

        public void addComment(String addition) {
            if (addition.startsWith("@")) {
                // there are exactly 3 cases: deprecated, ordered, and metadata
                switch (addition) {
                case "@ORDERED":
                    isOrderedElement = true;
                    break;
                case "@DEPRECATED":
                    isDeprecatedElement = true;
                    break;
                case "@METADATA":
                    elementStatus = ElementStatus.metadata;
                    break;
                default:
                    throw new IllegalArgumentException("Unrecognized annotation: " + addition);
                }
                return;
            }
            commentsPost = addUnmodifiable(commentsPost, addition.trim());
        }

        /**
         * Special version of equals. Only the name is considered in the identity.
         */
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Element)) {
                return false;
            }
            Element that = (Element) obj;
            return name.equals(that.name)
                // not relevant to the identity of the object
                //                && Objects.equals(comment, that.comment)
                //                && type == that.type
                //                && attributes.equals(that.attributes)
                //                && children.equals(that.children)
                ;
        }

        /**
         * Special version of hashcode. Only the name is considered in the identity.
         */
        @Override
        public int hashCode() {
            return name.hashCode()
                // not relevant to the identity of the object
                // * 37 + Objects.hashCode(comment)
                //) * 37 + Objects.hashCode(type)
                //                ) * 37 + attributes.hashCode()
                //                ) * 37 + children.hashCode()
                ;
        }

        public boolean isDeprecated() {
            return isDeprecatedElement;
        }

        public ElementStatus getElementStatus() {
            return elementStatus;
        }

        /**
         * @return the rawModel
         */
        public String getRawModel() {
            return rawModel;
        }
    }

    private Element elementFrom(String name) {
        Element result = nameToElement.get(name);
        if (result == null) {
            nameToElement.put(name, result = new Element(name));
        }
        return result;
    }

    private void addElement(String name2, String model) {
        Element element = elementFrom(name2);
        element.setChildren(this, model, preCommentCache);
        preCommentCache = null;
        lastElement = element;
        lastAttribute = null;
    }

    private void addComment(String comment) {
        comment = comment.trim();
        if (preCommentCache != null || comment.startsWith("#")) { // the precomments are "sticky"
            if (comment.startsWith("@")) {
                throw new IllegalArgumentException("@ annotation comment must follow element or attribute, without intervening # comment");
            }
            preCommentCache = addUnmodifiable(preCommentCache, comment);
        } else if (lastElement != null) {
            lastElement.addComment(comment);
        } else if (lastAttribute != null) {
            lastAttribute.addComment(comment);
        } else {
            if (comment.startsWith("@")) {
                throw new IllegalArgumentException("@ annotation comment must follow element or attribute, without intervening # comment");
            }
            preCommentCache = addUnmodifiable(preCommentCache, comment);
        }
    }

    // TODO hide this
    /**
     * @deprecated
     */
    @Override
    public void handleElementDecl(String name, String model) {
        if (SHOW_ALL) {
            // <!ELEMENT ldml (identity, (alias | (fallback*, localeDisplayNames?, layout?, contextTransforms?, characters?, delimiters?, measurement?, dates?, numbers?, units?, listPatterns?, collations?, posix?, segmentations?, rbnf?, annotations?, metadata?, references?, special*))) >
            System.out.println(System.lineSeparator() + "<!ELEMENT " + name + " " + model + " >");
        }
        addElement(name, model);
    }

    // TODO hide this
    /**
     * @deprecated
     */
    @Override
    public void handleStartDtd(String name, String publicId, String systemId) {
        DtdType explicitDtdType = DtdType.valueOf(name);
        if (explicitDtdType != dtdType && explicitDtdType != dtdType.rootType) {
            throw new IllegalArgumentException("Mismatch in dtdTypes");
        }
    };

    /**
     * @deprecated
     */
    @Override
    public void handleAttributeDecl(String eName, String aName, String type, String mode, String value) {
        if (SHOW_ALL) {
            // <!ATTLIST ldml draft ( approved | contributed | provisional | unconfirmed | true | false ) #IMPLIED >
            // <!ATTLIST version number CDATA #REQUIRED >
            // <!ATTLIST version cldrVersion CDATA #FIXED "27" >

            System.out.println("<!ATTLIST " + eName
                + " " + aName
                + " " + type
                + " " + mode
                + (value == null ? "" : " \"" + value + "\"")
                + " >");
        }
        // HACK for 1.1.1
        if (eName.equals("draft")) {
            eName = "week";
        }
        addAttribute(eName, aName, type, mode, value);
    }

    /**
     * @deprecated
     */
    @Override
    public void handleComment(String path, String comment) {
        if (SHOW_ALL) {
            // <!-- true and false are deprecated. -->
            System.out.println("<!-- " + comment.trim() + " -->");
        }
        addComment(comment);
    }

    // TODO hide this
    /**
     * @deprecated
     */
    @Override
    public void handleEndDtd() {
        throw new XMLFileReader.AbortException();
    }

    //    static final Map<CLDRFile.DtdType, String> DTD_TYPE_TO_FILE;
    //    static {
    //        EnumMap<CLDRFile.DtdType, String> temp = new EnumMap<CLDRFile.DtdType, String>(CLDRFile.DtdType.class);
    //        temp.put(CLDRFile.DtdType.ldml, CldrUtility.BASE_DIRECTORY + "common/dtd/ldml.dtd");
    //        temp.put(CLDRFile.DtdType.supplementalData, CldrUtility.BASE_DIRECTORY + "common/dtd/ldmlSupplemental.dtd");
    //        temp.put(CLDRFile.DtdType.ldmlBCP47, CldrUtility.BASE_DIRECTORY + "common/dtd/ldmlBCP47.dtd");
    //        temp.put(CLDRFile.DtdType.keyboard, CldrUtility.BASE_DIRECTORY + "keyboards/dtd/ldmlKeyboard.dtd");
    //        temp.put(CLDRFile.DtdType.platform, CldrUtility.BASE_DIRECTORY + "keyboards/dtd/ldmlPlatform.dtd");
    //        DTD_TYPE_TO_FILE = Collections.unmodifiableMap(temp);
    //    }

    /**
     * Normal version of DtdData
     * Note that it always gets the trunk version
     */
    public static DtdData getInstance(DtdType type) {
        return CACHE.get(type);
    }

    /**
     * Special form using version, used only by tests, etc.
     */
    public static DtdData getInstance(DtdType type, String version) {
        DtdData simpleHandler = new DtdData(type, version);
        XMLFileReader xfr = new XMLFileReader().setHandler(simpleHandler);
        File directory = version == null ? CLDRConfig.getInstance().getCldrBaseDirectory()
            : new File(CLDRPaths.ARCHIVE_DIRECTORY + "/cldr-" + version);

        if (type != type.rootType) {
            // read the real first, then add onto it.
            readFile(type.rootType, xfr, directory);
        }
        readFile(type, xfr, directory);
        // HACK
        if (type == DtdType.ldmlICU) {
            Element special = simpleHandler.nameToElement.get("special");
            for (String extraElementName : Arrays.asList(
                "icu:breakIteratorData",
                "icu:UCARules",
                "icu:scripts",
                "icu:transforms",
                "icu:ruleBasedNumberFormats",
                "icu:isLeapMonth",
                "icu:version",
                "icu:breakDictionaryData",
                "icu:depends")) {
                Element extraElement = simpleHandler.nameToElement.get(extraElementName);
                special.children.put(extraElement, special.children.size());
            }
        }
        if (simpleHandler.ROOT.children.size() == 0) {
            throw new IllegalArgumentException(); // should never happen
        }
        simpleHandler.finish();
        simpleHandler.freeze();
        return simpleHandler;
    }

    private void finish() {
        dtdComparator = new DtdComparator();
    }

    public static void readFile(DtdType type, XMLFileReader xfr, File directory) {
        File file = new File(directory, type.dtdPath);
        StringReader s = new StringReader("<?xml version='1.0' encoding='UTF-8' ?>"
            + "<!DOCTYPE " + type
            + " SYSTEM '" + file.getAbsolutePath() + "'>");
        xfr.read(type.toString(), s, -1, true); //  DTD_TYPE_TO_FILE.get(type)
    }

    private void freeze() {
        if (version == null) { // only generate for new versions
            MergeLists<String> elementMergeList = new MergeLists<String>();
            elementMergeList.add(dtdType.toString());
            MergeLists<String> attributeMergeList = new MergeLists<String>();
            attributeMergeList.add("_q");

            for (Element element : nameToElement.values()) {
                if (element.children.size() > 0) {
                    Collection<String> names = getNames(element.children.keySet());
                    elementMergeList.add(names);
                    if (DEBUG) {
                        System.out.println(element.getName() + "\t→\t" + names);
                    }
                }
                if (element.attributes.size() > 0) {
                    Collection<String> names = getNames(element.attributes.keySet());
                    attributeMergeList.add(names);
                    if (DEBUG) {
                        System.out.println(element.getName() + "\t→\t@" + names);
                    }
                }
            }
            List<String> elementList = elementMergeList.merge();
            List<String> attributeList = attributeMergeList.merge();
            if (DEBUG) {
                System.out.println("Element Ordering:\t" + elementList);
                System.out.println("Attribute Ordering:\t" + attributeList);
            }
            // double-check
            //        for (Element element : elements) {
            //            if (!MergeLists.hasConsistentOrder(elementList, element.children.keySet())) {
            //                throw new IllegalArgumentException("Failed to find good element order: " + element.children.keySet());
            //            }
            //            if (!MergeLists.hasConsistentOrder(attributeList, element.attributes.keySet())) {
            //                throw new IllegalArgumentException("Failed to find good attribute order: " + element.attributes.keySet());
            //            }
            //        }
            elementComparator = new MapComparator<String>(elementList).setErrorOnMissing(true).freeze();
            attributeComparator = new MapComparator<String>(attributeList).setErrorOnMissing(true).freeze();
        }
        nameToAttributes.freeze();
        nameToElement = Collections.unmodifiableMap(nameToElement);
    }

    private Collection<String> getNames(Collection<? extends Named> keySet) {
        List<String> result = new ArrayList<String>();
        for (Named e : keySet) {
            result.add(e.getName());
        }
        return result;
    }

    public enum DtdItem {
        ELEMENT, ATTRIBUTE, ATTRIBUTE_VALUE
    }

    public interface AttributeValueComparator {
        public int compare(String element, String attribute, String value1, String value2);
    }

    public Comparator<String> getDtdComparator(AttributeValueComparator avc) {
        return dtdComparator;
    }

    private class DtdComparator implements Comparator<String> {
        @Override
        public int compare(String path1, String path2) {
            XPathParts a = XPathParts.getFrozenInstance(path1);
            XPathParts b = XPathParts.getFrozenInstance(path2);
            // there must always be at least one element
            String baseA = a.getElement(0);
            String baseB = b.getElement(0);
            if (!ROOT.name.equals(baseA) || !ROOT.name.equals(baseB)) {
                throw new IllegalArgumentException("Comparing different DTDs: " + ROOT.name + ", " + baseA + ", " + baseB);
            }
            int min = Math.min(a.size(), b.size());
            Element parent = ROOT;
            Element elementA;
            for (int i = 1; i < min; ++i, parent = elementA) {
                // add extra test for "fake" elements, used in diffing. they always start with _
                String elementRawA = a.getElement(i);
                String elementRawB = b.getElement(i);
                if (elementRawA.startsWith("_")) {
                    return elementRawB.startsWith("_") ? elementRawA.compareTo(elementRawB) : -1;
                } else if (elementRawB.startsWith("_")) {
                    return 1;
                }
                //
                elementA = nameToElement.get(elementRawA);
                Element elementB = nameToElement.get(elementRawB);
                if (elementA != elementB) {
                    int aa = parent.children.get(elementA);
                    int bb = parent.children.get(elementB);
                    return aa - bb;
                }
                int countA = a.getAttributeCount(i);
                int countB = b.getAttributeCount(i);
                if (countA == 0 && countB == 0) {
                    continue;
                }
                // we have two ways to compare the attributes. One based on the dtd,
                // and one based on explicit comparators

                // at this point the elements are the same and correspond to elementA
                // in the dtd

                // Handle the special added elements
                String aqValue = a.getAttributeValue(i, "_q");
                if (aqValue != null) {
                    String bqValue = b.getAttributeValue(i, "_q");
                    if (!aqValue.equals(bqValue)) {
                        int aValue = Integer.parseInt(aqValue);
                        int bValue = Integer.parseInt(bqValue);
                        return aValue - bValue;
                    }
                    --countA;
                    --countB;
                }

                attributes: for (Entry<Attribute, Integer> attr : elementA.attributes.entrySet()) {
                    Attribute main = attr.getKey();
                    String valueA = a.getAttributeValue(i, main.name);
                    String valueB = b.getAttributeValue(i, main.name);
                    if (valueA == null) {
                        if (valueB != null) {
                            return -1;
                        }
                    } else if (valueB == null) {
                        return 1;
                    } else if (valueA.equals(valueB)) {
                        --countA;
                        --countB;
                        if (countA == 0 && countB == 0) {
                            break attributes;
                        }
                        continue; // TODO
                    } else if (main.attributeValueComparator != null) {
                        return main.attributeValueComparator.compare(valueA, valueB);
                    } else if (main.values.size() != 0) {
                        int aa = main.values.get(valueA);
                        int bb = main.values.get(valueB);
                        return aa - bb;
                    } else {
                        return valueA.compareTo(valueB);
                    }
                }
                if (countA != 0 || countB != 0) {
                    throw new IllegalArgumentException();
                }
            }
            return a.size() - b.size();
        }
    }

    public MapComparator<String> getAttributeComparator() {
        return attributeComparator;
    }

    public MapComparator<String> getElementComparator() {
        return elementComparator;
    }

    public Relation<String, Attribute> getAttributesFromName() {
        return nameToAttributes;
    }

    public Map<String, Element> getElementFromName() {
        return nameToElement;
    }

    //    private static class XPathIterator implements SimpleIterator<Node> {
    //        private String path;
    //        private int position; // at the start of the next element, or at the end of the string
    //        private Node node = new Node();
    //
    //        public void set(String path) {
    //            if (!path.startsWith("//")) {
    //                throw new IllegalArgumentException();
    //            }
    //            this.path = path;
    //            this.position = 2;
    //        }
    //
    //        @Override
    //        public Node next() {
    //            // starts with /...[@...="...."]...
    //            if (position >= path.length()) {
    //                return null;
    //            }
    //            node.elementName = "";
    //            node.attributes.clear();
    //            int start = position;
    //            // collect the element
    //            while (true) {
    //                if (position >= path.length()) {
    //                    return node;
    //                }
    //                char ch = path.charAt(position++);
    //                switch (ch) {
    //                case '/':
    //                    return node;
    //                case '[':
    //                    node.elementName = path.substring(start, position);
    //                    break;
    //                }
    //            }
    //            // done with element, we hit a [, collect the attributes
    //
    //            if (path.charAt(position++) != '@') {
    //                throw new IllegalArgumentException();
    //            }
    //            while (true) {
    //                if (position >= path.length()) {
    //                    return node;
    //                }
    //                char ch = path.charAt(position++);
    //                switch (ch) {
    //                case '/':
    //                    return node;
    //                case '[':
    //                    node.elementName = path.substring(start, position);
    //                    break;
    //                }
    //            }
    //        }
    //    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        // <!ELEMENT ldml (identity, (alias | (fallback*, localeDisplayNames?, layout?, contextTransforms?, characters?, delimiters?, measurement?, dates?, numbers?, units?, listPatterns?, collations?, posix?, segmentations?, rbnf?, metadata?, references?, special*))) >
        // <!ATTLIST ldml draft ( approved | contributed | provisional | unconfirmed | true | false ) #IMPLIED > <!-- true and false are deprecated. -->
//        if (firstComment != null) {
//            b.append("\n<!--").append(firstComment).append("-->");
//        }
        Seen seen = new Seen(dtdType);
        seen.seenElements.add(ANY);
        seen.seenElements.add(PCDATA);
        toString(ROOT, b, seen);

        // Hack for ldmlIcu: catch the items that are not mentioned in the original
        int currentEnd = b.length();
        for (Element e : nameToElement.values()) {
            toString(e, b, seen);
        }
        if (currentEnd != b.length()) {
            b.insert(currentEnd,
                System.lineSeparator() + System.lineSeparator()
                + "<!-- Elements not reachable from root! -->"
                + System.lineSeparator());
        }
        return b.toString();
    }

    static final class Seen {
        Set<Element> seenElements = new HashSet<Element>();
        Set<Attribute> seenAttributes = new HashSet<Attribute>();

        public Seen(DtdType dtdType) {
            if (dtdType.rootType == dtdType) {
                return;
            }
            DtdData otherData = DtdData.getInstance(dtdType.rootType);
            walk(otherData, otherData.ROOT);
            seenElements.remove(otherData.nameToElement.get("special"));
        }

        private void walk(DtdData otherData, Element current) {
            seenElements.add(current);
            seenAttributes.addAll(current.attributes.keySet());
            for (Element e : current.children.keySet()) {
                walk(otherData, e);
            }
        }
    }

    public Set<Element> getDescendents(Element start, Set<Element> toAddTo) {
        if (!toAddTo.contains(start)) {
            toAddTo.add(start);
            for (Element e : start.children.keySet()) {
                getDescendents(e, toAddTo);
            }
        }
        return toAddTo;
    }

    //static final SupplementalDataInfo supplementalDataInfo = CLDRConfig.getInstance().getSupplementalDataInfo();

    private void toString(Element current, StringBuilder b, Seen seen) {
//        if ("calendar".equals(current.name) || current.commentsPost != null && current.commentsPost.contains("use of fields")) {
//            int debug = 0;
//        }
        boolean first = true;
        if (seen.seenElements.contains(current)) {
            return;
        }
        seen.seenElements.add(current);
        boolean elementDeprecated = isDeprecated(current.name, "*", "*");

        showComments(b, current.commentsPre, true);
        b.append("\n\n<!ELEMENT " + current.name + " " + current.model + " >");
        if (USE_SYNTHESIZED) {
            Element aliasElement = getElementFromName().get("alias");
            //b.append(current.rawChildren);
            if (!current.children.isEmpty()) {
                LinkedHashSet<Element> elements = new LinkedHashSet<Element>(current.children.keySet());
                boolean hasAlias = aliasElement != null && elements.remove(aliasElement);
                //boolean hasSpecial = specialElement != null && elements.remove(specialElement);
                if (hasAlias) {
                    b.append("(alias |");
                }
                b.append("(");
                // <!ELEMENT transformNames ( alias | (transformName | special)* ) >
                // <!ELEMENT layout ( alias | (orientation*, inList*, inText*, special*) ) >

                for (Element e : elements) {
                    if (first) {
                        first = false;
                    } else {
                        b.append(", ");
                    }
                    b.append(e.name);
                    if (e.type != ElementType.PCDATA) {
                        b.append("*");
                    }
                }
                if (hasAlias) {
                    b.append(")");
                }
                b.append(")");
            } else {
                b.append(current.type == null ? "???" : current.type.source);
            }
            b.append(">");
        }
        showComments(b, current.commentsPost, false);
        if (isOrdered(current.name)) {
            b.append(COMMENT_PREFIX + "<!--@ORDERED-->");
        }
        if (current.getElementStatus() != ElementStatus.regular) {
            b.append(COMMENT_PREFIX + "<!--@"
                + current.getElementStatus().toString().toUpperCase(Locale.ROOT)
                + "-->");
        }
        if (elementDeprecated) {
            b.append(COMMENT_PREFIX + "<!--@DEPRECATED-->");
        }

        LinkedHashSet<String> deprecatedValues = new LinkedHashSet<>();

        for (Attribute a : current.attributes.keySet()) {
            if (seen.seenAttributes.contains(a)) {
                continue;
            }
            seen.seenAttributes.add(a);
            boolean attributeDeprecated = elementDeprecated || isDeprecated(current.name, a.name, "*");

            deprecatedValues.clear();

            showComments(b, a.commentsPre, true);
            b.append("\n<!ATTLIST " + current.name + " " + a.name);
            if (a.type == AttributeType.ENUMERATED_TYPE) {
                b.append(" (");
                first = true;
                for (String s : a.values.keySet()) {
                    if (first) {
                        first = false;
                    } else {
                        b.append(" | ");
                    }
                    b.append(s);
                    if (!attributeDeprecated && isDeprecated(current.name, a.name, s)) {
                        deprecatedValues.add(s);
                    }
                }
                b.append(")");
            } else {
                b.append(' ').append(a.type);
            }
            if (a.mode != Mode.NULL) {
                b.append(" ").append(a.mode.source);
            }
            if (a.defaultValue != null) {
                b.append(" \"").append(a.defaultValue).append('"');
            }
            b.append(" >");
            showComments(b, a.commentsPost, false);
//            if (attributeDeprecated != deprecatedComment) {
//                System.out.println("*** BAD DEPRECATION ***" + a);
//            }
            if (METADATA.contains(a.name) || a.attributeStatus == AttributeStatus.metadata) {
                b.append(COMMENT_PREFIX + "<!--@METADATA-->");
            } else if (!isDistinguishing(current.name, a.name)) {
                b.append(COMMENT_PREFIX + "<!--@VALUE-->");
            }
            if (attributeDeprecated) {
                b.append(COMMENT_PREFIX + "<!--@DEPRECATED-->");
            } else if (!deprecatedValues.isEmpty()) {
                b.append(COMMENT_PREFIX + "<!--@DEPRECATED:" + CollectionUtilities.join(deprecatedValues, ", ") + "-->");
            }
            if (a.matchValue != null) {
                b.append(COMMENT_PREFIX + "<!--@MATCH:" + a.matchValue.getName() + "-->");
            }
        }
        if (current.children.size() > 0) {
            for (Element e : current.children.keySet()) {
                toString(e, b, seen);
            }
        }
    }

    private void showComments(StringBuilder b, Set<String> comments, boolean separate) {
        if (comments == null) {
            return;
        }
        if (separate && b.length() != 0) {
            b.append(System.lineSeparator());
        }
        for (String c : comments) {
            boolean deprecatedComment = false; // the following served its purpose... c.toLowerCase(Locale.ENGLISH).contains("deprecat");
            if (!deprecatedComment) {
                if (separate) {
                    // special handling for very first comment
                    if (b.length() == 0) {
                        b.append("<!--")
                        .append(System.lineSeparator())
                        .append(c)
                        .append(System.lineSeparator())
                        .append("-->");
                        continue;
                    }
                    b.append(System.lineSeparator());
                } else {
                    b.append(COMMENT_PREFIX);
                }
                b.append("<!-- ").append(c).append(" -->");
            }
        }
    }

    public static <T> T removeFirst(Collection<T> elements, Transform<T, Boolean> matcher) {
        for (Iterator<T> it = elements.iterator(); it.hasNext();) {
            T item = it.next();
            if (matcher.transform(item) == Boolean.TRUE) {
                it.remove();
                return item;
            }
        }
        return null;
    }

    public Set<Element> getElements() {
        return new LinkedHashSet<Element>(nameToElement.values());
    }

    public Set<Attribute> getAttributes() {
        return new LinkedHashSet<Attribute>(nameToAttributes.values());
    }

    public boolean isDistinguishing(String elementName, String attribute) {
        return getAttributeStatus(elementName, attribute) == AttributeStatus.distinguished;
    }

    static final Set<String> METADATA = new HashSet<>(Arrays.asList("references", "standard", "draft"));

    static final Set<String> addUnmodifiable(Set<String> comment, String addition) {
        if (comment == null) {
            return Collections.singleton(addition);
        } else {
            comment = new LinkedHashSet<>(comment);
            comment.add(addition);
            return Collections.unmodifiableSet(comment);
        }
    }

    public class IllegalByDtdException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public final String elementName;
        public final String attributeName;
        public final String attributeValue;

        public IllegalByDtdException(String elementName, String attributeName, String attributeValue) {
            this.elementName = elementName;
            this.attributeName = attributeName;
            this.attributeValue = attributeValue;
        }

        @Override
        public String getMessage() {
            return "Dtd " + dtdType
                + " doesn’t allow "
                + "element=" + elementName
                + (attributeName == null ? "" : ", attribute: " + attributeName)
                + (attributeValue == null ? "" : ", attributeValue: " + attributeValue);
        }
    }

    //@SuppressWarnings("unused")
    public boolean isDeprecated(String elementName, String attributeName, String attributeValue) {
        Element element = nameToElement.get(elementName);
        if (element == null) {
            throw new IllegalByDtdException(elementName, attributeName, attributeValue);
        } else if (element.isDeprecatedElement) {
            return true;
        }
        if ("*".equals(attributeName) || "_q".equals(attributeName)) {
            return false;
        }
        Attribute attribute = element.getAttributeNamed(attributeName);
        if (attribute == null) {
            throw new IllegalByDtdException(elementName, attributeName, attributeValue);
        } else if (attribute.isDeprecatedAttribute) {
            return true;
        }
        return attribute.deprecatedValues.contains(attributeValue); // don't need special test for "*"
    }

    public boolean isOrdered(String elementName) {
        Element element = nameToElement.get(elementName);
        if (element == null) {
            if (elementName.startsWith("icu:")) {
                return false;
            }
            throw new IllegalByDtdException(elementName, null, null);
        }
        return element.isOrderedElement;
    }

    public AttributeStatus getAttributeStatus(String elementName, String attributeName) {
        if ("_q".equals(attributeName)) {
            return AttributeStatus.distinguished; // special case
        }
        if ("#PCDATA".equals(elementName)) {
            int debug = 1;
        }
        Element element = nameToElement.get(elementName);
        if (element == null) {
            if (elementName.startsWith("icu:")) {
                return AttributeStatus.distinguished;
            }
            throw new IllegalByDtdException(elementName, attributeName, null);
        }
        Attribute attribute = element.getAttributeNamed(attributeName);
        if (attribute == null) {
            if (elementName.startsWith("icu:")) {
                return AttributeStatus.distinguished;
            }
            throw new IllegalByDtdException(elementName, attributeName, null);
        }
        return attribute.attributeStatus;
    }

    // The default is a map comparator, which compares numbers as numbers, and strings with UCA
    private static MapComparator<String> valueOrdering = new MapComparator<String>().setErrorOnMissing(false).freeze();

    static MapComparator<String> dayValueOrder = new MapComparator<String>().add(
        "sun", "mon", "tue", "wed", "thu", "fri", "sat").freeze();
    static MapComparator<String> dayPeriodOrder = new MapComparator<String>().add(
        "midnight", "am", "noon", "pm",
        "morning1", "morning2", "afternoon1", "afternoon2", "evening1", "evening2", "night1", "night2",
        // The ones on the following line are no longer used actively. Can be removed later?
        "earlyMorning", "morning", "midDay", "afternoon", "evening", "night", "weeHours").freeze();
    static MapComparator<String> listPatternOrder = new MapComparator<String>().add(
        "start", "middle", "end", "2", "3").freeze();
    static MapComparator<String> widthOrder = new MapComparator<String>().add(
        "abbreviated", "narrow", "short", "wide", "all").freeze();
    static MapComparator<String> lengthOrder = new MapComparator<String>().add(
        "full", "long", "medium", "short").freeze();
    static MapComparator<String> dateFieldOrder = new MapComparator<String>().add(
        "era", "era-short", "era-narrow",
        "year", "year-short", "year-narrow",
        "quarter", "quarter-short", "quarter-narrow",
        "month", "month-short", "month-narrow",
        "week", "week-short", "week-narrow",
        "weekOfMonth", "weekOfMonth-short", "weekOfMonth-narrow",
        "day", "day-short", "day-narrow",
        "dayOfYear", "dayOfYear-short", "dayOfYear-narrow",
        "weekday", "weekday-short", "weekday-narrow",
        "weekdayOfMonth", "weekdayOfMonth-short", "weekdayOfMonth-narrow",
        "sun", "sun-short", "sun-narrow",
        "mon", "mon-short", "mon-narrow",
        "tue", "tue-short", "tue-narrow",
        "wed", "wed-short", "wed-narrow",
        "thu", "thu-short", "thu-narrow",
        "fri", "fri-short", "fri-narrow",
        "sat", "sat-short", "sat-narrow",
        "dayperiod-short", "dayperiod", "dayperiod-narrow",
        "hour", "hour-short", "hour-narrow",
        "minute", "minute-short", "minute-narrow",
        "second", "second-short", "second-narrow",
        "zone", "zone-short", "zone-narrow").freeze();
    static MapComparator<String> unitOrder = new MapComparator<String>().add(
        "acceleration-g-force", "acceleration-meter-per-second-squared",
        "angle-revolution", "angle-radian", "angle-degree", "angle-arc-minute", "angle-arc-second",
        "area-square-kilometer", "area-hectare", "area-square-meter", "area-square-centimeter",
        "area-square-mile", "area-acre", "area-square-yard", "area-square-foot", "area-square-inch",
        "area-dunam",
        "concentr-karat",
        "concentr-milligram-per-deciliter", "concentr-millimole-per-liter",
        "concentr-part-per-million", "concentr-percent", "concentr-permille", "concentr-permyriad",
        "concentr-mole",
        "consumption-liter-per-kilometer", "consumption-liter-per-100kilometers",
        "consumption-mile-per-gallon", "consumption-mile-per-gallon-imperial",
        "digital-petabyte", "digital-terabyte", "digital-terabit", "digital-gigabyte", "digital-gigabit",
        "digital-megabyte", "digital-megabit", "digital-kilobyte", "digital-kilobit",
        "digital-byte", "digital-bit",
        "duration-century",
        "duration-year", "duration-year-person",
        "duration-month", "duration-month-person",
        "duration-week", "duration-week-person",
        "duration-day", "duration-day-person",
        "duration-hour", "duration-minute", "duration-second",
        "duration-millisecond", "duration-microsecond", "duration-nanosecond",
        "electric-ampere", "electric-milliampere", "electric-ohm", "electric-volt",
        "energy-kilocalorie", "energy-calorie", "energy-foodcalorie", "energy-kilojoule", "energy-joule", "energy-kilowatt-hour",
        "energy-electronvolt",
        "energy-british-thermal-unit",
        "force-pound-force",
        "force-newton",
        "frequency-gigahertz", "frequency-megahertz", "frequency-kilohertz", "frequency-hertz",
        "length-kilometer", "length-meter", "length-decimeter", "length-centimeter",
        "length-millimeter", "length-micrometer", "length-nanometer", "length-picometer",
        "length-mile", "length-yard", "length-foot", "length-inch",
        "length-parsec", "length-light-year", "length-astronomical-unit",
        "length-furlong", "length-fathom",
        "length-nautical-mile", "length-mile-scandinavian",
        "length-point",
        "length-solar-radius",
        "light-lux",
        "light-solar-luminosity",
        "mass-metric-ton", "mass-kilogram", "mass-gram", "mass-milligram", "mass-microgram",
        "mass-ton", "mass-stone", "mass-pound", "mass-ounce",
        "mass-ounce-troy", "mass-carat",
        "mass-dalton",
        "mass-earth-mass",
        "mass-solar-mass",
        "power-gigawatt", "power-megawatt", "power-kilowatt", "power-watt", "power-milliwatt",
        "power-horsepower",
        "pressure-hectopascal", "pressure-millimeter-of-mercury",
        "pressure-pound-per-square-inch", "pressure-inch-hg", "pressure-millibar", "pressure-atmosphere",
        "pressure-kilopascal",
        "pressure-megapascal",
        "speed-kilometer-per-hour", "speed-meter-per-second", "speed-mile-per-hour", "speed-knot",
        "temperature-generic", "temperature-celsius", "temperature-fahrenheit", "temperature-kelvin",
        "torque-pound-foot",
        "torque-newton-meter",
        "volume-cubic-kilometer", "volume-cubic-meter", "volume-cubic-centimeter",
        "volume-cubic-mile", "volume-cubic-yard", "volume-cubic-foot", "volume-cubic-inch",
        "volume-megaliter", "volume-hectoliter", "volume-liter", "volume-deciliter", "volume-centiliter", "volume-milliliter",
        "volume-pint-metric", "volume-cup-metric",
        "volume-acre-foot",
        "volume-bushel", "volume-gallon", "volume-gallon-imperial", "volume-quart", "volume-pint", "volume-cup",
        "volume-fluid-ounce", "volume-fluid-ounce-imperial", "volume-tablespoon", "volume-teaspoon",
        "volume-barrel").freeze();

    static MapComparator<String> countValueOrder = new MapComparator<String>().add(
        "0", "1", "zero", "one", "two", "few", "many", "other").freeze();
    static MapComparator<String> unitLengthOrder = new MapComparator<String>().add(
        "long", "short", "narrow").freeze();
    static MapComparator<String> currencyFormatOrder = new MapComparator<String>().add(
        "standard", "accounting").freeze();
    static Comparator<String> zoneOrder = StandardCodes.make().getTZIDComparator();

    static final Comparator<String> COMP = (Comparator) CLDRConfig.getInstance().getCollator();

    // Hack for US
    static final Comparator<String> UNICODE_SET_COMPARATOR = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            if (o1.contains("{")) {
                o1 = o1.replace("{", "");
            }
            if (o2.contains("{")) {
                o2 = o2.replace("{", "");
            }
            return COMP.compare(o1, o2);
        }

    };

    public static Comparator<String> getAttributeValueComparator(String element, String attribute) {
        return getAttributeValueComparator(DtdType.ldml, element, attribute);
    }

    static Comparator<String> getAttributeValueComparator(DtdType type, String element, String attribute) {
        // The default is a map comparator, which compares numbers as numbers, and strings with UCA
        Comparator<String> comp = valueOrdering;
        if (type != DtdType.ldml && type != DtdType.ldmlICU) {
            return comp;
        }
        if (attribute.equals("day")) { // && (element.startsWith("weekend")
            comp = dayValueOrder;
        } else if (attribute.equals("type")) {
            if (element.endsWith("FormatLength")) {
                comp = lengthOrder;
            } else if (element.endsWith("Width")) {
                comp = widthOrder;
            } else if (element.equals("day")) {
                comp = dayValueOrder;
            } else if (element.equals("field")) {
                comp = dateFieldOrder;
            } else if (element.equals("zone")) {
                comp = zoneOrder;
            } else if (element.equals("listPatternPart")) {
                comp = listPatternOrder;
            } else if (element.equals("currencyFormat")) {
                comp = currencyFormatOrder;
            } else if (element.equals("unitLength")) {
                comp = unitLengthOrder;
            } else if (element.equals("unit")) {
                comp = unitOrder;
            } else if (element.equals("dayPeriod")) {
                comp = dayPeriodOrder;
            }
        } else if (attribute.equals("count") && !element.equals("minDays")) {
            comp = countValueOrder;
        } else if (attribute.equals("cp") && element.equals("annotation")) {
            comp = UNICODE_SET_COMPARATOR;
        }
        return comp;
    }

    /**
     * Comparator for attributes in CLDR files
     */
    private static AttributeValueComparator ldmlAvc = new AttributeValueComparator() {
        @Override
        public int compare(String element, String attribute, String value1, String value2) {
            Comparator<String> comp = getAttributeValueComparator(element, attribute);
            return comp.compare(value1, value2);
        }
    };

    public boolean hasValue(String elementName) {
        return nameToElement.get(elementName).type == ElementType.PCDATA;
    }

    public boolean isMetadata(XPathParts pathPlain) {
        for (String s : pathPlain.getElements()) {
            Element e = getElementFromName().get(s);
            if (e.elementStatus == ElementStatus.metadata) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMetadataOld(DtdType dtdType2, XPathParts pathPlain) {
        // TODO Don't use hard-coded list; instead add to DTD annotations
        final String element1 = pathPlain.getElement(1);
        final String element2 = pathPlain.getElement(2);
        final String elementN = pathPlain.getElement(-1);
        switch (dtdType2) {
        case ldml:
            switch (element1) {
            case "generation":
            case "metadata":
                return true;
            }
            break;
        case ldmlBCP47:
            switch (element1) {
            case "generation":
            case "version":
                return true;
            }
            break;
            ////supplementalData/transforms/transform[@source="am"][@target="am_FONIPA"][@direction="forward"]/comment
        case supplementalData:
            // these are NOT under /metadata/ but are actually metadata
            switch (element1) {
            case "generation":
            case "version":
            case "validity":
            case "references":
            case "coverageLevels":
                return true;
            case "transforms":
                return elementN.equals("comment");
            case "metadata":
                // these ARE under /metadata/, but many others under /metadata/ are NOT actually metadata.
                switch (element2) {
                case "validity":
                case "serialElements":
                case "suppress":
                case "distinguishing":
                case "blocking":
                case "casingData":
                    return true;
                }
                break;
            }
            break;
        default:
        }
        return false;
    }

    public boolean isDeprecated(XPathParts pathPlain) {
        for (int i = 0; i < pathPlain.size(); ++i) {
            String elementName = pathPlain.getElement(i);
            if (isDeprecated(elementName, "*", null)) {
                return true;
            }
            for (String attribute : pathPlain.getAttributeKeys(i)) {
                String attributeValue = pathPlain.getAttributeValue(i, attribute);
                if (isDeprecated(elementName, attribute, attributeValue)) {
                    return true;
                }
            }
        }
        return false;
    }

    public final static Splitter SPACE_SPLITTER = Splitter.on(CharMatcher.whitespace()).trimResults().omitEmptyStrings();
    public final static Splitter BAR_SPLITTER = Splitter.on('|').trimResults().omitEmptyStrings();
    public final static Splitter CR_SPLITTER = Splitter.on(CharMatcher.anyOf("\n\r")).trimResults().omitEmptyStrings();

    private static class XPathPartsSet {
        private final Set<XPathParts> list = new LinkedHashSet<>();

        private void addElement(String element) {
            if (list.isEmpty()) {
                list.add(new XPathParts().addElement(element));
            } else {
                for (XPathParts item : list) {
                    item.addElement(element);
                }
            }
        }

        private void addAttribute(String attribute, String attributeValue) {
            for (XPathParts item : list) {
                item.addAttribute(attribute, attributeValue);
            }
        }

        private void setElement(int i, String string) {
            for (XPathParts item : list) {
                item.setElement(i, string);
            }
        }

//        private int size() {
//            return list.iterator().next().size();
//        }
//
//        private void removeElement(int i) {
//            for (XPathParts item : list) {
//                item.removeElement(i);
//            }
//        }

        private void addAttributes(String attribute, List<String> attributeValues) {
            if (attributeValues.size() == 1) {
                addAttribute(attribute, attributeValues.iterator().next());
            } else {
                // duplicate all the items in the list with the given values 
                Set<XPathParts> newList = new LinkedHashSet<>();
                for (XPathParts item : list) {
                    for (String attributeValue : attributeValues) {
                        XPathParts newItem = item.cloneAsThawed();
                        newItem.addAttribute(attribute, attributeValue);
                        newList.add(newItem);
                    }
                }
                list.clear();
                list.addAll(newList);
            }
        }

        private ImmutableSet<String> toStrings() {
            Builder<String> result = new ImmutableSet.Builder<>();

            for (XPathParts item : list) {
                result.add(item.toString());
            }
            return result.build();
        }

        @Override
        public String toString() {
            return list.toString();
        }
    }

    public Set<String> getRegularizedPaths(XPathParts pathPlain, Multimap<String, String> extras) {
        extras.clear();
        Map<String, String> valueAttributes = new HashMap<>();
        XPathPartsSet pathResult = new XPathPartsSet();
        String element = null;
        for (int i = 0; i < pathPlain.size(); ++i) {
            element = pathPlain.getElement(i);
            pathResult.addElement(element);
            valueAttributes.clear();
            for (String attribute : pathPlain.getAttributeKeys(i)) {
                AttributeStatus status = getAttributeStatus(element, attribute);
                final String attributeValue = pathPlain.getAttributeValue(i, attribute);
                switch (status) {
                case distinguished:
                    AttributeType attrType = getAttributeType(element, attribute);
                    if (attrType == AttributeType.NMTOKENS) {
                        pathResult.addAttributes(attribute, SPACE_SPLITTER.splitToList(attributeValue));
                    } else {
                        pathResult.addAttribute(attribute, attributeValue);
                    }
                    break;
                case value:
                    valueAttributes.put(attribute, attributeValue);
                    break;
                case metadata:
                    break;
                }
            }
            if (!valueAttributes.isEmpty()) {
                boolean hasValue = hasValue(element);
                // if it doesn't have a value, we construct new child elements, with _ prefix
                // if it does have a value, we have to play a further trick, since
                // we can't have a value and child elements at the same level. 
                // So we use a _ suffix on the element.
                if (hasValue) {
                    pathResult.setElement(i, element + "_");
                } else {
                    int debug = 0;
                }
                for (Entry<String, String> attributeAndValue : valueAttributes.entrySet()) {
                    final String attribute = attributeAndValue.getKey();
                    final String attributeValue = attributeAndValue.getValue();

                    Set<String> pathsShort = pathResult.toStrings();
                    AttributeType attrType = getAttributeType(element, attribute);
                    for (String pathShort : pathsShort) {
                        pathShort += "/_" + attribute;
                        if (attrType == AttributeType.NMTOKENS) {
                            for (String valuePart : SPACE_SPLITTER.split(attributeValue)) {
                                extras.put(pathShort, valuePart);
                            }
                        } else {
                            extras.put(pathShort, attributeValue);
                        }
                    }
                }
                if (hasValue) {
                    pathResult.setElement(i, element); // restore
                }
            }
        }
        // Only add the path if it could have a value, looking at the last element
        if (!hasValue(element)) {
            return null;
        }
        return pathResult.toStrings();
    }

    public AttributeType getAttributeType(String elementName, String attributeName) {
        Element element = nameToElement.get(elementName);
        if (element == null) {
            return null;
        }
        Attribute attr = element.getAttributeNamed(attributeName);
        if (attr == null) {
            return null;
        }
        return attr.type;
    }

    // TODO: add support for following to DTD annotations, and rework API

    static final Set<String> SPACED_VALUES = ImmutableSet.of(
        "idValidity",
        "languageGroup");

    public static Splitter getValueSplitter(XPathParts pathPlain) {
        if (!Collections.disjoint(pathPlain.getElements(), SPACED_VALUES)) {
            return SPACE_SPLITTER;
        } else if (pathPlain.getElement(-1).equals("annotation")
            && !pathPlain.getAttributeKeys(-1).contains("tts")) {
            return BAR_SPLITTER;
        }
        return CR_SPLITTER;
    }

    public static boolean isComment(XPathParts pathPlain, String line) {
        if (pathPlain.contains("transform")) {
            if (line.startsWith("#")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isExtraSplit(String extraPath) {
        if (extraPath.endsWith("/_type") && extraPath.startsWith("//supplementalData/metaZones/mapTimezones")) {
            return true;
        }
        return false;
    }

    /**
     * Return the value status for an EAV
     */
    public ValueStatus getValueStatus(String elementName, String attributeName, String value) {
        Element element = nameToElement.get(elementName);
        if (element == null) {
            return ValueStatus.invalid;
        }
        Attribute attr = element.getAttributeNamed(attributeName);
        if (attr == null) {
            return ValueStatus.invalid;
        }
        return attr.getValueStatus(value);
    }
    
    /**
     * Return element-attribute pairs with non-enumerated values, for quick checks.
     */
    public Multimap<String, String> getNonEnumerated(Map<String,String> matchValues) {
        Multimap<String,String> nonEnumeratedElementToAttribute = TreeMultimap.create(); // make tree for ease of debugging
        for (Entry<String, Element> entry : nameToElement.entrySet()) {
            Element element = entry.getValue();
            for (Attribute attribute : element.attributes.keySet()) {
                if (attribute.type != AttributeType.ENUMERATED_TYPE) {
                    String elementName = element.getName();
                    String attrName = attribute.getName();
                    nonEnumeratedElementToAttribute.put(elementName, attrName);
                    if (attribute.matchValue != null) {
                        matchValues.put(elementName + "\t" + attrName, attribute.matchValue.getName());
                    }
                }
            }
        }
        return ImmutableSetMultimap.copyOf(nonEnumeratedElementToAttribute);
    }
    
    // ALWAYS KEEP AT END, FOR STATIC INIT ORDER
    private static final Map<DtdType, DtdData> CACHE;
    static {
        EnumMap<DtdType, DtdData> temp = new EnumMap<DtdType, DtdData>(DtdType.class);
        for (DtdType type : DtdType.values()) {
            temp.put(type, getInstance(type, null));
        }
        CACHE = Collections.unmodifiableMap(temp);
    }
    // ALWAYS KEEP AT END, FOR STATIC INIT ORDER
}
