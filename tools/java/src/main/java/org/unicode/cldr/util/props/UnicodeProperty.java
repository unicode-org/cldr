/*
 *******************************************************************************
 * Copyright (C) 1996-2014, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package org.unicode.cldr.util.props;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.SymbolTable;
import com.ibm.icu.text.UFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeMatcher;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public abstract class UnicodeProperty extends UnicodeLabel {

    public static final UnicodeSet NONCHARACTERS = new UnicodeSet("[:noncharactercodepoint:]").freeze();
    public static final UnicodeSet PRIVATE_USE = new UnicodeSet("[:gc=privateuse:]").freeze();
    public static final UnicodeSet SURROGATE = new UnicodeSet("[:gc=surrogate:]").freeze();

    public static final UnicodeSet HIGH_SURROGATES = new UnicodeSet("[\\uD800-\\uDB7F]").freeze();
    public static final int SAMPLE_HIGH_SURROGATE = HIGH_SURROGATES.charAt(0);
    public static final UnicodeSet HIGH_PRIVATE_USE_SURROGATES = new UnicodeSet("[\\uDB80-\\uDBFF]").freeze();
    public static final int SAMPLE_HIGH_PRIVATE_USE_SURROGATE = HIGH_PRIVATE_USE_SURROGATES.charAt(0);
    public static final UnicodeSet LOW_SURROGATES = new UnicodeSet("[\\uDC00-\\uDFFF]").freeze();
    public static final int SAMPLE_LOW_SURROGATE = LOW_SURROGATES.charAt(0);

    public static final UnicodeSet PRIVATE_USE_AREA = new UnicodeSet("[\\uE000-\\uF8FF]").freeze();
    public static final int SAMPLE_PRIVATE_USE_AREA = PRIVATE_USE_AREA.charAt(0);
    public static final UnicodeSet PRIVATE_USE_AREA_A = new UnicodeSet("[\\U000F0000-\\U000FFFFD]").freeze();
    public static final int SAMPLE_PRIVATE_USE_AREA_A = PRIVATE_USE_AREA_A.charAt(0);
    public static final UnicodeSet PRIVATE_USE_AREA_B = new UnicodeSet("[\\U00100000-\\U0010FFFD]").freeze();
    public static final int SAMPLE_PRIVATE_USE_AREA_B = PRIVATE_USE_AREA_B.charAt(0);

    // The following are special. They are used for performance, but must be changed if the version of Unicode for the UnicodeProperty changes.
    private static UnicodeSet UNASSIGNED;
    private static int SAMPLE_UNASSIGNED;
    private static UnicodeSet SPECIALS;
    private static UnicodeSet STUFF_TO_TEST;
    private static UnicodeSet STUFF_TO_TEST_WITH_UNASSIGNED;

    public static synchronized UnicodeSet getUNASSIGNED() {
        if (UNASSIGNED == null) {
            UNASSIGNED = new UnicodeSet("[:gc=unassigned:]").freeze();
        }
        return UNASSIGNED;
    }

    public static synchronized UnicodeSet contractUNASSIGNED(UnicodeSet toBeUnassigned) {
        UnicodeSet temp = UNASSIGNED;
        ResetCacheProperties();
        UNASSIGNED = temp == null ? toBeUnassigned.freeze() : new UnicodeSet(temp).retainAll(toBeUnassigned).freeze();
        return UNASSIGNED;
    }

    public static synchronized int getSAMPLE_UNASSIGNED() {
        if (SAMPLE_UNASSIGNED == 0) {
            SAMPLE_UNASSIGNED = getUNASSIGNED().charAt(0);
        }
        return SAMPLE_UNASSIGNED;
    }

    public static synchronized UnicodeSet getSPECIALS() {
        if (SPECIALS == null) {
            SPECIALS = new UnicodeSet(getUNASSIGNED()).addAll(PRIVATE_USE).addAll(SURROGATE).freeze();
        }
        return SPECIALS;
    }

    public static synchronized UnicodeSet getSTUFF_TO_TEST() {
        if (STUFF_TO_TEST == null) {
            STUFF_TO_TEST = new UnicodeSet(getSPECIALS()).complement()
            .addAll(NONCHARACTERS)
            .add(getSAMPLE_UNASSIGNED())
            .add(SAMPLE_HIGH_SURROGATE)
            .add(SAMPLE_HIGH_PRIVATE_USE_SURROGATE)
            .add(SAMPLE_LOW_SURROGATE)
            .add(SAMPLE_PRIVATE_USE_AREA)
            .add(SAMPLE_PRIVATE_USE_AREA_A)
            .add(SAMPLE_PRIVATE_USE_AREA_B)
            .freeze();
        }
        return STUFF_TO_TEST;
    }

    public static synchronized UnicodeSet getSTUFF_TO_TEST_WITH_UNASSIGNED() {
        if (STUFF_TO_TEST_WITH_UNASSIGNED == null) {
            STUFF_TO_TEST_WITH_UNASSIGNED = new UnicodeSet(getSTUFF_TO_TEST()).addAll(getUNASSIGNED()).freeze();
        }
        return STUFF_TO_TEST_WITH_UNASSIGNED;
    }

    /**
     * Reset the cache properties. Must be done if the version of Unicode is different than the ICU one, AND any UnicodeProperty has already been instantiated.
     * TODO make this a bit more robust.
     * @internal
     */
    public static synchronized void ResetCacheProperties() {
        UNASSIGNED = null;
        SAMPLE_UNASSIGNED = 0;
        SPECIALS = null;
        STUFF_TO_TEST = null;
        STUFF_TO_TEST_WITH_UNASSIGNED = null;
    }

    public static boolean DEBUG = false;

    public static String CHECK_NAME = "FC_NFKC_Closure";

    public static int CHECK_VALUE = 0x037A;

    private String name;

    private String firstNameAlias = null;

    private int type;

    private Map valueToFirstValueAlias = null;

    private boolean hasUniformUnassigned = true;

    /*
     * Name: Unicode_1_Name Name: ISO_Comment Name: Name Name: Unicode_1_Name
     *
     */

    public static final int UNKNOWN = 0, BINARY = 2, EXTENDED_BINARY = 3,
    ENUMERATED = 4, EXTENDED_ENUMERATED = 5, CATALOG = 6,
    EXTENDED_CATALOG = 7, MISC = 8, EXTENDED_MISC = 9, STRING = 10,
    EXTENDED_STRING = 11, NUMERIC = 12, EXTENDED_NUMERIC = 13,
    START_TYPE = 2, LIMIT_TYPE = 14, EXTENDED_MASK = 1,
    CORE_MASK = ~EXTENDED_MASK, BINARY_MASK = (1 << BINARY)
    | (1 << EXTENDED_BINARY), STRING_MASK = (1 << STRING)
    | (1 << EXTENDED_STRING),
    STRING_OR_MISC_MASK = (1 << STRING) | (1 << EXTENDED_STRING)
    | (1 << MISC) | (1 << EXTENDED_MISC),
    ENUMERATED_OR_CATALOG_MASK = (1 << ENUMERATED)
    | (1 << EXTENDED_ENUMERATED) | (1 << CATALOG)
    | (1 << EXTENDED_CATALOG);

    private static final String[] TYPE_NAMES = { "Unknown", "Unknown",
        "Binary", "Extended Binary", "Enumerated", "Extended Enumerated",
        "Catalog", "Extended Catalog", "Miscellaneous",
        "Extended Miscellaneous", "String", "Extended String", "Numeric",
        "Extended Numeric", };

    public static String getTypeName(int propType) {
        return TYPE_NAMES[propType];
    }

    public final String getName() {
        return name;
    }

    public final int getType() {
        return type;
    }

    public String getTypeName() {
        return TYPE_NAMES[type];
    }

    public final boolean isType(int mask) {
        return ((1 << type) & mask) != 0;
    }

    protected final void setName(String string) {
        if (string == null)
            throw new IllegalArgumentException("Name must not be null");
        name = string;
    }

    protected final void setType(int i) {
        type = i;
    }

    public String getVersion() {
        return _getVersion();
    }

    public String getValue(int codepoint) {
        if (DEBUG && CHECK_VALUE == codepoint && CHECK_NAME.equals(getName())) {
            String value = _getValue(codepoint);
            System.out.println(getName() + "(" + Utility.hex(codepoint) + "):"
                    + (getType() == STRING ? Utility.hex(value) : value));
            return value;
        }
        return _getValue(codepoint);
    }

    // public String getValue(int codepoint, boolean isShort) {
    // return getValue(codepoint);
    // }

    public List<String> getNameAliases(List<String> result) {
        if (result == null)
            result = new ArrayList(1);
        return _getNameAliases(result);
    }

    public List<String> getValueAliases(String valueAlias, List<String> result) {
        if (result == null)
            result = new ArrayList(1);
        result = _getValueAliases(valueAlias, result);
        if (!result.contains(valueAlias)) { // FIX && type < NUMERIC
            result = _getValueAliases(valueAlias, result); // for debugging
            throw new IllegalArgumentException("Internal error: " + getName()
                    + " doesn't contain " + valueAlias + ": "
                    + new BagFormatter().join(result));
        }
        return result;
    }

    public List<String> getAvailableValues(List<String> result) {
        if (result == null)
            result = new ArrayList(1);
        return _getAvailableValues(result);
    }

    protected abstract String _getVersion();

    protected abstract String _getValue(int codepoint);

    protected abstract List<String> _getNameAliases(List<String> result);

    protected abstract List<String> _getValueAliases(String valueAlias, List<String> result);

    protected abstract List<String> _getAvailableValues(List<String> result);

    // conveniences
    public final List<String> getNameAliases() {
        return getNameAliases(null);
    }

    public final List<String> getValueAliases(String valueAlias) {
        return getValueAliases(valueAlias, null);
    }

    public final List<String> getAvailableValues() {
        return getAvailableValues(null);
    }

    @Override
    public final String getValue(int codepoint, boolean getShortest) {
        String result = getValue(codepoint);
        if (type >= MISC || result == null || !getShortest)
            return result;
        return getFirstValueAlias(result);
    }

    public final String getFirstNameAlias() {
        if (firstNameAlias == null) {
            firstNameAlias = getNameAliases().get(0);
        }
        return firstNameAlias;
    }

    public final String getFirstValueAlias(String value) {
        if (valueToFirstValueAlias == null)
            _getFirstValueAliasCache();
        return valueToFirstValueAlias.get(value).toString();
    }

    private void _getFirstValueAliasCache() {
        maxValueWidth = 0;
        maxFirstValueAliasWidth = 0;
        valueToFirstValueAlias = new HashMap(1);
        Iterator it = getAvailableValues().iterator();
        while (it.hasNext()) {
            String value = (String) it.next();
            String first = getValueAliases(value).get(0);
            if (first == null) { // internal error
                throw new IllegalArgumentException(
                        "Value not in value aliases: " + value);
            }
            if (DEBUG && CHECK_NAME.equals(getName())) {
                System.out.println("First Alias: " + getName() + ": " + value
                        + " => " + first
                        + new BagFormatter().join(getValueAliases(value)));
            }
            valueToFirstValueAlias.put(value, first);
            if (value.length() > maxValueWidth) {
                maxValueWidth = value.length();
            }
            if (first.length() > maxFirstValueAliasWidth) {
                maxFirstValueAliasWidth = first.length();
            }
        }
    }

    private int maxValueWidth = -1;

    private int maxFirstValueAliasWidth = -1;

    @Override
    public int getMaxWidth(boolean getShortest) {
        if (maxValueWidth < 0)
            _getFirstValueAliasCache();
        if (getShortest)
            return maxFirstValueAliasWidth;
        return maxValueWidth;
    }

    public final UnicodeSet getSet(String propertyValue) {
        return getSet(propertyValue, null);
    }

    public final UnicodeSet getSet(PatternMatcher matcher) {
        return getSet(matcher, null);
    }

    /** Adds the property value set to the result. Clear the result first if you don't want to keep the original contents.
     */
    public final UnicodeSet getSet(String propertyValue, UnicodeSet result) {
        if (isType(BINARY_MASK)) {
            if (binaryYesSet == null) {
                binaryYesSet = getSet(YES_MATCHER, null);
            }
            Boolean binValue = binaryValueOrNull(propertyValue);
            if (binValue == Boolean.TRUE) {
                if (result == null) {
                    return binaryYesSet.cloneAsThawed();
                } else {
                    return result.addAll(binaryYesSet);
                }
            } else if (binValue == Boolean.FALSE && !unicodeMapHasStringKeys) {
                // We could cache this directly, too.
                UnicodeSet inverse = binaryYesSet.cloneAsThawed().complement();
                if (result == null) {
                    return inverse;
                } else {
                    return result.addAll(inverse);
                }
            }
        }
        Predicate<String> matcher = isType(STRING_OR_MISC_MASK) ?
            new StringEqualityMatcher(propertyValue) : new NameMatcher(propertyValue);
        return getSet(matcher, result);
    }

    private static final Boolean binaryValueOrNull(String value) {
        if ("Yes".equals(value)) {  // fastpath
            return Boolean.TRUE;
        }
        if (value == null) {
            return null;
        }
        switch (toSkeleton(value)) {
        case "n":
        case "no":
        case "f":
        case "false":
            return Boolean.FALSE;
        case "y":
        case "yes":
        case "t":
        case "true":
            return Boolean.TRUE;
        default:
            return null;
        }
    }

    private UnicodeMap unicodeMap = null;
    private boolean unicodeMapHasStringKeys = false;
    private UnicodeSet binaryYesSet = null;

    public static final String UNUSED = "??";

    public UnicodeSet getSet(Predicate matcher, UnicodeSet result) {
        if (result == null)
            result = new UnicodeSet();
        boolean uniformUnassigned = hasUniformUnassigned();
        if (isType(STRING_OR_MISC_MASK)) {
            for (UnicodeSetIterator usi = getStuffToTest(uniformUnassigned); usi.next();) { // int i = 0; i <= 0x10FFFF; ++i
                int i = usi.codepoint;
                String value = getValue(i);
                if (value != null && matcher.test(value)) {
                    result.add(i);
                }
            }
            return addUntested(result, uniformUnassigned);
        }
        List temp = new ArrayList(1); // to avoid reallocating...
        UnicodeMap um = getUnicodeMap_internal();
        Iterator it = um.getAvailableValues(null).iterator();
        main: while (it.hasNext()) {
            String value = (String) it.next();
            if (matcher.test(value)) {  // fastpath
                um.keySet(value, result);
                continue main;
            }
            temp.clear();
            Iterator it2 = getValueAliases(value, temp).iterator();
            while (it2.hasNext()) {
                String value2 = (String) it2.next();
                // System.out.println("Values:" + value2);
                if (matcher.test(value2)
                        || matcher.test(toSkeleton(value2))) {
                    um.keySet(value, result);
                    continue main;
                }
            }
        }
        return result;
    }

    /*
     * public UnicodeSet getMatchSet(UnicodeSet result) { if (result == null)
     * result = new UnicodeSet(); addAll(matchIterator, result); return result; }
     *
     * public void setMatchSet(UnicodeSet set) { matchIterator = new
     * UnicodeSetIterator(set); }
     */

    /**
     * Utility for debugging
     */
    public static String getStack() {
        Exception e = new Exception();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.flush();
        return "Showing Stack with fake " + sw.getBuffer().toString();
    }

    // TODO use this instead of plain strings
    public static class Name implements Comparable {
        private String skeleton;

        private String pretty;

        public final int RAW = 0, TITLE = 1, NORMAL = 2;

        public Name(String name, int style) {
            if (name == null)
                name = "";
            if (style == RAW) {
                skeleton = pretty = name;
            } else {
                pretty = regularize(name, style == TITLE);
                skeleton = toSkeleton(pretty);
            }
        }

        @Override
        public int compareTo(Object o) {
            return skeleton.compareTo(((Name) o).skeleton);
        }

        @Override
        public boolean equals(Object o) {
            return skeleton.equals(((Name) o).skeleton);
        }

        @Override
        public int hashCode() {
            return skeleton.hashCode();
        }

        @Override
        public String toString() {
            return pretty;
        }
    }

    /**
     * @return the unicode map
     */
    public UnicodeMap getUnicodeMap() {
        return getUnicodeMap(false);
    }

    /**
     * @return the unicode map
     */
    public UnicodeMap getUnicodeMap(boolean getShortest) {
        if (!getShortest)
            return getUnicodeMap_internal().cloneAsThawed();
        UnicodeMap result = new UnicodeMap();
        boolean uniformUnassigned = hasUniformUnassigned();

        for (UnicodeSetIterator usi = getStuffToTest(uniformUnassigned); usi.next();) { // int i = 0; i <= 0x10FFFF; ++i
            int i = usi.codepoint;
            // if (DEBUG && i == 0x41) System.out.println(i + "\t" +
            // getValue(i));
            String value = getValue(i, true);
            result.put(i, value);
        }
        return addUntested(result, uniformUnassigned);
    }

    /**
     * @return the unicode map
     */
    public UnicodeMap getUnicodeMap_internal() {
        if (unicodeMap == null) {
            unicodeMap = _getUnicodeMap();
            Set<String> stringKeys = unicodeMap.stringKeys();
            unicodeMapHasStringKeys = stringKeys != null && !stringKeys.isEmpty();
        }
        return unicodeMap;
    }

    protected UnicodeMap _getUnicodeMap() {
        UnicodeMap result = new UnicodeMap();
        HashMap myIntern = new HashMap();
        boolean uniformUnassigned = hasUniformUnassigned();

        for (UnicodeSetIterator usi = getStuffToTest(uniformUnassigned); usi.next();) { // int i = 0; i <= 0x10FFFF; ++i
            int i = usi.codepoint;
            // if (DEBUG && i == 0x41) System.out.println(i + "\t" +
            // getValue(i));
            String value = getValue(i);
            String iValue = (String) myIntern.get(value);
            if (iValue == null)
                myIntern.put(value, iValue = value);
            result.put(i, iValue);
        }
        addUntested(result, uniformUnassigned);

        if (DEBUG) {
            for (UnicodeSetIterator usi = getStuffToTest(uniformUnassigned); usi.next();) { // int i = 0; i <= 0x10FFFF; ++i
                int i = usi.codepoint;
                // if (DEBUG && i == 0x41) System.out.println(i + "\t" +
                // getValue(i));
                String value = getValue(i);
                String resultValue = (String) result.getValue(i);
                if (!value.equals(resultValue)) {
                    throw new RuntimeException("Value failure at: "
                            + Utility.hex(i));
                }
            }
        }
        if (DEBUG && CHECK_NAME.equals(getName())) {
            System.out.println(getName() + ":\t" + getClass().getName() + "\t"
                    + getVersion());
            System.out.println(getStack());
            System.out.println(result);
        }
        return result;
    }

    private static UnicodeSetIterator getStuffToTest(boolean uniformUnassigned) {
        return new UnicodeSetIterator(uniformUnassigned ? getSTUFF_TO_TEST() : getSTUFF_TO_TEST_WITH_UNASSIGNED());
    }

    /**
     * Really ought to create a Collection UniqueList, that forces uniqueness.
     * But for now...
     */
    public static Collection addUnique(Object obj, Collection result) {
        if (obj != null && !result.contains(obj))
            result.add(obj);
        return result;
    }

    /**
     * Utility for managing property & non-string value aliases
     */
    public static final Comparator PROPERTY_COMPARATOR = new Comparator() {
        @Override
        public int compare(Object o1, Object o2) {
            return compareNames((String) o1, (String) o2);
        }
    };

    /**
     * Utility for managing property & non-string value aliases
     *
     */
    // TODO optimize
    public static boolean equalNames(String a, String b) {
        if (a == b)
            return true;
        if (a == null)
            return false;
        return toSkeleton(a).equals(toSkeleton(b));
    }

    /**
     * Utility for managing property & non-string value aliases
     */
    // TODO optimize
    public static int compareNames(String a, String b) {
        if (a == b)
            return 0;
        if (a == null)
            return -1;
        if (b == null)
            return 1;
        return toSkeleton(a).compareTo(toSkeleton(b));
    }

    /**
     * Utility for managing property & non-string value aliases
     */
    // TODO account for special names, tibetan, hangul
    public static String toSkeleton(String source) {
        if (source == null)
            return null;
        StringBuilder skeletonBuffer = new StringBuilder();
        boolean gotOne = false;
        // remove spaces, '_', '-'
        // we can do this with char, since no surrogates are involved
        for (int i = 0; i < source.length(); ++i) {
            char ch = source.charAt(i);
            if (i > 0 && (ch == '_' || ch == ' ' || ch == '-')) {
                gotOne = true;
            } else {
                char ch2 = Character.toLowerCase(ch);
                if (ch2 != ch) {
                    gotOne = true;
                    skeletonBuffer.append(ch2);
                } else {
                    skeletonBuffer.append(ch);
                }
            }
        }
        if (!gotOne)
            return source; // avoid string creation
        return skeletonBuffer.toString();
    }

    // get the name skeleton
    public static String toNameSkeleton(String source) {
        if (source == null)
            return null;
        StringBuffer result = new StringBuffer();
        // remove spaces, medial '-'
        // we can do this with char, since no surrogates are involved
        for (int i = 0; i < source.length(); ++i) {
            char ch = source.charAt(i);
            if (('0' <= ch && ch <= '9') || ('A' <= ch && ch <= 'Z')
                    || ch == '<' || ch == '>') {
                result.append(ch);
            } else if (ch == ' ') {
                // don't copy ever
            } else if (ch == '-') {
                // only copy non-medials AND trailing O-E
                if (0 == i
                        || i == source.length() - 1
                        || source.charAt(i - 1) == ' '
                            || source.charAt(i + 1) == ' '
                                || (i == source.length() - 2
                                        && source.charAt(i - 1) == 'O' && source
                                        .charAt(i + 1) == 'E')) {
                    System.out.println("****** EXCEPTION " + source);
                    result.append(ch);
                }
                // otherwise don't copy
            } else {
                throw new IllegalArgumentException("Illegal Name Char: U+"
                        + Utility.hex(ch) + ", " + ch);
            }
        }
        return result.toString();
    }

    /**
     * These routines use the Java functions, because they only need to act on
     * ASCII Changes space, - into _, inserts _ between lower and UPPER.
     */
    public static String regularize(String source, boolean titlecaseStart) {
        if (source == null)
            return source;
        /*
         * if (source.equals("noBreak")) { // HACK if (titlecaseStart) return
         * "NoBreak"; return source; }
         */
        StringBuffer result = new StringBuffer();
        int lastCat = -1;
        boolean haveFirstCased = true;
        for (int i = 0; i < source.length(); ++i) {
            char c = source.charAt(i);
            if (c == ' ' || c == '-' || c == '_') {
                c = '_';
                haveFirstCased = true;
            }
            if (c == '=')
                haveFirstCased = true;
            int cat = Character.getType(c);
            if (lastCat == Character.LOWERCASE_LETTER
                    && cat == Character.UPPERCASE_LETTER) {
                result.append('_');
            }
            if (haveFirstCased
                    && (cat == Character.LOWERCASE_LETTER
                            || cat == Character.TITLECASE_LETTER || cat == Character.UPPERCASE_LETTER)) {
                if (titlecaseStart) {
                    c = Character.toUpperCase(c);
                }
                haveFirstCased = false;
            }
            result.append(c);
            lastCat = cat;
        }
        return result.toString();
    }

    /**
     * Utility function for comparing codepoint to string without generating new
     * string.
     *
     * @param codepoint
     * @param other
     * @return true if the codepoint equals the string
     */
    public static final boolean equals(int codepoint, String other) {
        if (other == null) return false;
        if (other.length() == 1) {
            return codepoint == other.charAt(0);
        }
        if (other.length() == 2) {
            return other.equals(UTF16.valueOf(codepoint));
        }
        return false;
    }

    /**
     * Utility function for comparing objects that may be null
     * string.
     */
    public static final <T extends Object> boolean equals(T a, T b) {
        return a == null ? b == null
                : b == null ? false
                        : a.equals(b);
    }

    /**
     * Utility that should be on UnicodeSet
     *
     * @param source
     * @param result
     */
    static public void addAll(UnicodeSetIterator source, UnicodeSet result) {
        while (source.nextRange()) {
            if (source.codepoint == UnicodeSetIterator.IS_STRING) {
                result.add(source.string);
            } else {
                result.add(source.codepoint, source.codepointEnd);
            }
        }
    }

    /**
     * Really ought to create a Collection UniqueList, that forces uniqueness.
     * But for now...
     */
    public static Collection addAllUnique(Collection source, Collection result) {
        for (Iterator it = source.iterator(); it.hasNext();) {
            addUnique(it.next(), result);
        }
        return result;
    }

    /**
     * Really ought to create a Collection UniqueList, that forces uniqueness.
     * But for now...
     */
    public static Collection addAllUnique(Object[] source, Collection result) {
        for (int i = 0; i < source.length; ++i) {
            addUnique(source[i], result);
        }
        return result;
    }

    static public class Factory {
        static boolean DEBUG = false;

        Map<String, UnicodeProperty> canonicalNames = new TreeMap<>();

        Map skeletonNames = new TreeMap();

        Map propertyCache = new HashMap(1);

        public final Factory add(UnicodeProperty sp) {
            String name2 = sp.getName();
            if (name2.length() == 0) {
                throw new IllegalArgumentException();
            }
            canonicalNames.put(name2, sp);
            skeletonNames.put(toSkeleton(name2), sp);
            List c = sp.getNameAliases(new ArrayList(1));
            Iterator it = c.iterator();
            while (it.hasNext()) {
                skeletonNames.put(toSkeleton((String) it.next()), sp);
            }
            return this;
        }

        public UnicodeProperty getProperty(String propertyAlias) {
            return (UnicodeProperty) skeletonNames
            .get(toSkeleton(propertyAlias));
        }

        public final List<String> getAvailableNames() {
            return getAvailableNames(null);
        }

        public final List<String> getAvailableNames(List<String> result) {
            if (result == null)
                result = new ArrayList(1);
            Iterator it = canonicalNames.keySet().iterator();
            while (it.hasNext()) {
                addUnique(it.next(), result);
            }
            return result;
        }

        public final List getAvailableNames(int propertyTypeMask) {
            return getAvailableNames(propertyTypeMask, null);
        }

        public final List getAvailableNames(int propertyTypeMask, List result) {
            if (result == null)
                result = new ArrayList(1);
            Iterator it = canonicalNames.keySet().iterator();
            while (it.hasNext()) {
                String item = (String) it.next();
                UnicodeProperty property = getProperty(item);
                if (DEBUG)
                    System.out.println("Properties: " + item + ","
                            + property.getType());
                if (!property.isType(propertyTypeMask)) {
                    // System.out.println("Masking: " + property.getType() + ","
                    // + propertyTypeMask);
                    continue;
                }
                addUnique(property.getName(), result);
            }
            return result;
        }

        InversePatternMatcher inverseMatcher = new InversePatternMatcher();

        /**
         * Format is: propname ('=' | '!=') propvalue ( '|' propValue )*
         */
        public final UnicodeSet getSet(String propAndValue,
                PatternMatcher matcher, UnicodeSet result) {
            int equalPos = propAndValue.indexOf('=');
            String prop = propAndValue.substring(0, equalPos);
            String value = propAndValue.substring(equalPos + 1);
            boolean negative = false;
            if (prop.endsWith("!")) {
                prop = prop.substring(0, prop.length() - 1);
                negative = true;
            }
            prop = prop.trim();
            UnicodeProperty up = getProperty(prop);
            if (matcher == null) {
                matcher = new SimpleMatcher(value, up
                        .isType(STRING_OR_MISC_MASK) ? null
                                : PROPERTY_COMPARATOR);
            }
            if (negative) {
                inverseMatcher.set(matcher);
                matcher = inverseMatcher;
            }
            return up.getSet(matcher.set(value), result);
        }

        public final UnicodeSet getSet(String propAndValue,
                PatternMatcher matcher) {
            return getSet(propAndValue, matcher, null);
        }

        public final UnicodeSet getSet(String propAndValue) {
            return getSet(propAndValue, null, null);
        }

        public final SymbolTable getSymbolTable(String prefix) {
            return new PropertySymbolTable(prefix);
        }

        private class MyXSymbolTable extends UnicodeSet.XSymbolTable {
            @Override
            public boolean applyPropertyAlias(String propertyName,
                    String propertyValue, UnicodeSet result) {
                if (false)
                    System.out.println(propertyName + "=" + propertyValue);
                UnicodeProperty prop = getProperty(propertyName);
                if (prop == null)
                    return false;
                result.clear();
                UnicodeSet x = prop.getSet(propertyValue, result);
                return x.size() != 0;
            }
        }

        public final UnicodeSet.XSymbolTable getXSymbolTable() {
            return new MyXSymbolTable();
        }

        private class PropertySymbolTable implements SymbolTable {
            static final boolean DEBUG = false;

            private String prefix;

            RegexMatcher regexMatcher = new RegexMatcher();

            PropertySymbolTable(String prefix) {
                this.prefix = prefix;
            }

            @Override
            public char[] lookup(String s) {
                if (DEBUG)
                    System.out.println("\t(" + prefix + ")Looking up " + s);
                // ensure, again, that prefix matches
                int start = prefix.length();
                if (!s.regionMatches(true, 0, prefix, 0, start))
                    return null;

                int pos = s.indexOf(':', start);
                if (pos < 0) { // should never happen
                    throw new IllegalArgumentException(
                            "Internal Error: missing =: " + s + "\r\n");
                }
                UnicodeProperty prop = getProperty(s.substring(start, pos));
                if (prop == null) {
                    throw new IllegalArgumentException("Invalid Property in: "
                            + s + "\r\nUse " + showSet(getAvailableNames()));
                }
                String value = s.substring(pos + 1);
                UnicodeSet set;
                if (value.startsWith("\u00AB")) { // regex!
                    set = prop.getSet(regexMatcher.set(value.substring(1, value
                            .length() - 1)));
                } else {
                    set = prop.getSet(value);
                }
                if (set.size() == 0) {
                    throw new IllegalArgumentException(
                            "Empty Property-Value in: " + s + "\r\nUse "
                            + showSet(prop.getAvailableValues()));
                }
                if (DEBUG)
                    System.out.println("\t(" + prefix + ")Returning "
                            + set.toPattern(true));
                return set.toPattern(true).toCharArray(); // really ugly
            }

            private String showSet(List list) {
                StringBuffer result = new StringBuffer("[");
                boolean first = true;
                for (Iterator it = list.iterator(); it.hasNext();) {
                    if (!first)
                        result.append(", ");
                    else
                        first = false;
                    result.append(it.next().toString());
                }
                result.append("]");
                return result.toString();
            }

            @Override
            public UnicodeMatcher lookupMatcher(int ch) {
                return null;
            }

            @Override
            public String parseReference(String text, ParsePosition pos,
                    int limit) {
                if (DEBUG)
                    System.out.println("\t(" + prefix + ")Parsing <"
                            + text.substring(pos.getIndex(), limit) + ">");
                int start = pos.getIndex();
                // ensure that it starts with 'prefix'
                if (!text
                        .regionMatches(true, start, prefix, 0, prefix.length()))
                    return null;
                start += prefix.length();
                // now see if it is of the form identifier:identifier
                int i = getIdentifier(text, start, limit);
                if (i == start)
                    return null;
                String prop = text.substring(start, i);
                String value = "true";
                if (i < limit) {
                    if (text.charAt(i) == ':') {
                        int j;
                        if (text.charAt(i + 1) == '\u00AB') { // regular
                            // expression
                            j = text.indexOf('\u00BB', i + 2) + 1; // include
                            // last
                            // character
                            if (j <= 0)
                                return null;
                        } else {
                            j = getIdentifier(text, i + 1, limit);
                        }
                        value = text.substring(i + 1, j);
                        i = j;
                    }
                }
                pos.setIndex(i);
                if (DEBUG)
                    System.out.println("\t(" + prefix + ")Parsed <" + prop
                            + ">=<" + value + ">");
                return prefix + prop + ":" + value;
            }

            private int getIdentifier(String text, int start, int limit) {
                if (DEBUG)
                    System.out.println("\tGetID <"
                            + text.substring(start, limit) + ">");
                int cp = 0;
                int i;
                for (i = start; i < limit; i += UTF16.getCharCount(cp)) {
                    cp = UTF16.charAt(text, i);
                    if (!com.ibm.icu.lang.UCharacter
                            .isUnicodeIdentifierPart(cp)
                            && cp != '.') {
                        break;
                    }
                }
                if (DEBUG)
                    System.out.println("\tGotID <" + text.substring(start, i)
                            + ">");
                return i;
            }
        }
    }

    public static class FilteredProperty extends UnicodeProperty {
        private UnicodeProperty property;

        protected StringFilter filter;

        protected UnicodeSetIterator matchIterator = new UnicodeSetIterator(
                new UnicodeSet(0, 0x10FFFF));

        protected HashMap backmap;

        boolean allowValueAliasCollisions = false;

        public FilteredProperty(UnicodeProperty property, StringFilter filter) {
            this.property = property;
            this.filter = filter;
        }

        public StringFilter getFilter() {
            return filter;
        }

        public UnicodeProperty setFilter(StringFilter filter) {
            this.filter = filter;
            return this;
        }

        List temp = new ArrayList(1);

        @Override
        public List _getAvailableValues(List result) {
            temp.clear();
            return filter.addUnique(property.getAvailableValues(temp), result);
        }

        @Override
        public List _getNameAliases(List result) {
            temp.clear();
            return filter.addUnique(property.getNameAliases(temp), result);
        }

        @Override
        public String _getValue(int codepoint) {
            return filter.remap(property.getValue(codepoint));
        }

        @Override
        public List _getValueAliases(String valueAlias, List result) {
            if (backmap == null) {
                backmap = new HashMap(1);
                temp.clear();
                Iterator it = property.getAvailableValues(temp).iterator();
                while (it.hasNext()) {
                    String item = (String) it.next();
                    String mappedItem = filter.remap(item);
                    if (backmap.get(mappedItem) != null
                            && !allowValueAliasCollisions) {
                        throw new IllegalArgumentException(
                                "Filter makes values collide! " + item + ", "
                                + mappedItem);
                    }
                    backmap.put(mappedItem, item);
                }
            }
            valueAlias = (String) backmap.get(valueAlias);
            temp.clear();
            return filter.addUnique(property.getValueAliases(valueAlias, temp),
                    result);
        }

        @Override
        public String _getVersion() {
            return property.getVersion();
        }

        public boolean isAllowValueAliasCollisions() {
            return allowValueAliasCollisions;
        }

        public FilteredProperty setAllowValueAliasCollisions(boolean b) {
            allowValueAliasCollisions = b;
            return this;
        }

    }

    public static abstract class StringFilter implements Cloneable {
        public abstract String remap(String original);

        public final List addUnique(Collection source, List result) {
            if (result == null)
                result = new ArrayList(1);
            Iterator it = source.iterator();
            while (it.hasNext()) {
                UnicodeProperty.addUnique(remap((String) it.next()), result);
            }
            return result;
        }
        /*
         * public Object clone() { try { return super.clone(); } catch
         * (CloneNotSupportedException e) { throw new
         * IllegalStateException("Should never happen."); } }
         */
    }

    public static class MapFilter extends StringFilter {
        private Map valueMap;

        public MapFilter(Map valueMap) {
            this.valueMap = valueMap;
        }

        @Override
        public String remap(String original) {
            Object changed = valueMap.get(original);
            return changed == null ? original : (String) changed;
        }

        public Map getMap() {
            return valueMap;
        }
    }

    /** Matches using .equals(). */
    private static final class StringEqualityMatcher implements Predicate<String> {
        private final String pattern;

        StringEqualityMatcher(String pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean test(String value) {
            return pattern.equals(value);
        }
    }

    /** Matches skeleton strings. Computes the pattern skeleton only once. */
    private static final class NameMatcher implements Predicate<String> {
        private final String pattern;
        private final String skeleton;

        NameMatcher(String pattern) {
            this.pattern = pattern;
            this.skeleton = toSkeleton(pattern);
        }

        @Override
        public boolean test(String value) {
            return pattern.equals(value) || skeleton.equals(toSkeleton(value));
        }
    }

    private static final NameMatcher YES_MATCHER = new NameMatcher("Yes");

    public interface PatternMatcher extends Predicate {
        PatternMatcher set(String pattern);
    }

    public static class InversePatternMatcher implements PatternMatcher {
        PatternMatcher other;

        public PatternMatcher set(PatternMatcher toInverse) {
            other = toInverse;
            return this;
        }

        @Override
        public boolean test(Object value) {
            return !other.test(value);
        }

        @Override
        public PatternMatcher set(String pattern) {
            other.set(pattern);
            return this;
        }
    }

    public static class SimpleMatcher implements PatternMatcher {
        Comparator comparator;

        String pattern;

        public SimpleMatcher(String pattern, Comparator comparator) {
            this.comparator = comparator;
            this.pattern = pattern;
        }

        @Override
        public boolean test(Object value) {
            if (comparator == null)
                return pattern.equals(value);
            return comparator.compare(pattern, value) == 0;
        }

        @Override
        public PatternMatcher set(String pattern) {
            this.pattern = pattern;
            return this;
        }
    }

    public static class RegexMatcher implements UnicodeProperty.PatternMatcher {
        private java.util.regex.Matcher matcher;

        @Override
        public UnicodeProperty.PatternMatcher set(String pattern) {
            matcher = Pattern.compile(pattern).matcher("");
            return this;
        }
        UFormat foo;
        @Override
        public boolean test(Object value) {
            matcher.reset(value.toString());
            return matcher.find();
        }
    }

    public enum AliasAddAction {IGNORE_IF_MISSING, REQUIRE_MAIN_ALIAS, ADD_MAIN_ALIAS}

    public static abstract class BaseProperty extends UnicodeProperty {
        private static final String[] NO_VALUES = {"No", "N", "F", "False"};

        private static final String[] YES_VALUES = {"Yes", "Y", "T", "True"};

        /**
         *
         */
        private static final String[][] YES_NO_ALIASES = new String[][] {YES_VALUES, NO_VALUES};

        protected List propertyAliases = new ArrayList(1);

        protected Map toValueAliases;

        protected String version;

        public BaseProperty setMain(String alias, String shortAlias,
                int propertyType, String version) {
            setName(alias);
            setType(propertyType);
            propertyAliases.add(shortAlias);
            propertyAliases.add(alias);
            if (propertyType == BINARY) {
                addValueAliases(YES_NO_ALIASES, AliasAddAction.ADD_MAIN_ALIAS);
            }
            this.version = version;
            return this;
        }

        @Override
        public String _getVersion() {
            return version;
        }

        @Override
        public List _getNameAliases(List result) {
            addAllUnique(propertyAliases, result);
            return result;
        }

        public BaseProperty addValueAliases(String[][] valueAndAlternates,
                AliasAddAction aliasAddAction) {
            if (toValueAliases == null)
                _fixValueAliases();
            for (int i = 0; i < valueAndAlternates.length; ++i) {
                for (int j = 1; j < valueAndAlternates[0].length; ++j) {
                    addValueAlias(valueAndAlternates[i][0],
                            valueAndAlternates[i][j], aliasAddAction);
                }
            }
            return this;
        }

        public void addValueAlias(String value, String valueAlias,
                AliasAddAction aliasAddAction) {
            List result = (List) toValueAliases.get(value);
            if (result == null) {
                switch(aliasAddAction) {
                case IGNORE_IF_MISSING: return;
                case REQUIRE_MAIN_ALIAS: throw new IllegalArgumentException("Can't add alias for mising value: " + value);
                case ADD_MAIN_ALIAS:
                    toValueAliases.put(value, result = new ArrayList(0));
                    break;
                }
            }
            addUnique(value, result);
            addUnique(valueAlias, result);
        }

        @Override
        protected List _getValueAliases(String valueAlias, List result) {
            if (toValueAliases == null)
                _fixValueAliases();
            List a = (List) toValueAliases.get(valueAlias);
            if (a != null)
                addAllUnique(a, result);
            return result;
        }

        protected void _fixValueAliases() {
            if (toValueAliases == null)
                toValueAliases = new HashMap(1);
            for (Iterator it = getAvailableValues().iterator(); it.hasNext();) {
                Object value = it.next();
                _ensureValueInAliases(value);
            }
        }

        protected void _ensureValueInAliases(Object value) {
            List result = (List) toValueAliases.get(value);
            if (result == null)
                toValueAliases.put(value, result = new ArrayList(1));
            addUnique(value, result);
        }

        public BaseProperty swapFirst2ValueAliases() {
            for (Iterator it = toValueAliases.keySet().iterator(); it.hasNext();) {
                List list = (List) toValueAliases.get(it.next());
                if (list.size() < 2)
                    continue;
                Object first = list.get(0);
                list.set(0, list.get(1));
                list.set(1, first);
            }
            return this;
        }

        /**
         * @param string
         * @return
         */
        public UnicodeProperty addName(String string) {
            throw new UnsupportedOperationException();
        }

    }

    public static abstract class SimpleProperty extends BaseProperty {
        LinkedHashSet values;

        @Override
        public UnicodeProperty addName(String alias) {
            propertyAliases.add(alias);
            return this;
        }

        public SimpleProperty setValues(String valueAlias) {
            _addToValues(valueAlias, null);
            return this;
        }

        public SimpleProperty addAliases(String valueAlias, String... aliases) {
            _addToValues(valueAlias, null);
            return this;
        }

        public SimpleProperty setValues(String[] valueAliases,
                String[] alternateValueAliases) {
            for (int i = 0; i < valueAliases.length; ++i) {
                if (valueAliases[i].equals(UNUSED))
                    continue;
                _addToValues(
                        valueAliases[i],
                        alternateValueAliases != null ? alternateValueAliases[i]
                                                                              : null);
            }
            return this;
        }

        public SimpleProperty setValues(List valueAliases) {
            this.values = new LinkedHashSet(valueAliases);
            for (Iterator it = this.values.iterator(); it.hasNext();) {
                _addToValues((String) it.next(), null);
            }
            return this;
        }

        @Override
        public List _getAvailableValues(List result) {
            if (values == null)
                _fillValues();
            result.addAll(values);
            return result;
        }

        protected void _fillValues() {
            List newvalues = (List) getUnicodeMap_internal()
            .getAvailableValues(new ArrayList());
            for (Iterator it = newvalues.iterator(); it.hasNext();) {
                _addToValues((String) it.next(), null);
            }
        }

        private void _addToValues(String item, String alias) {
            if (values == null)
                values = new LinkedHashSet();
            if (toValueAliases == null)
                _fixValueAliases();
            addUnique(item, values);
            _ensureValueInAliases(item);
            addValueAlias(item, alias, AliasAddAction.REQUIRE_MAIN_ALIAS);
        }
        /*        public String _getVersion() {
         return version;
         }
         */
    }

    public static class UnicodeMapProperty extends BaseProperty {
        /*
         * Example of usage:
         * new UnicodeProperty.UnicodeMapProperty() {
         {
         unicodeMap = new UnicodeMap();
         unicodeMap.setErrorOnReset(true);
         unicodeMap.put(0xD, "CR");
         unicodeMap.put(0xA, "LF");
         UnicodeProperty cat = getProperty("General_Category");
         UnicodeSet temp = cat.getSet("Line_Separator")
         .addAll(cat.getSet("Paragraph_Separator"))
         .addAll(cat.getSet("Control"))
         .addAll(cat.getSet("Format"))
         .remove(0xD).remove(0xA).remove(0x200C).remove(0x200D);
         unicodeMap.putAll(temp, "Control");
         UnicodeSet graphemeExtend = getProperty("Grapheme_Extend").getSet("true");
         unicodeMap.putAll(graphemeExtend,"Extend");
         UnicodeProperty hangul = getProperty("Hangul_Syllable_Type");
         unicodeMap.putAll(hangul.getSet("L"),"L");
         unicodeMap.putAll(hangul.getSet("V"),"V");
         unicodeMap.putAll(hangul.getSet("T"),"T");
         unicodeMap.putAll(hangul.getSet("LV"),"LV");
         unicodeMap.putAll(hangul.getSet("LVT"),"LVT");
         unicodeMap.setMissing("Other");
         }
         }.setMain("Grapheme_Cluster_Break", "GCB", UnicodeProperty.ENUMERATED, version)
         */
        protected UnicodeMap unicodeMap;

        @Override
        protected UnicodeMap _getUnicodeMap() {
            return unicodeMap;
        }

        public UnicodeMapProperty set(UnicodeMap map) {
            unicodeMap = map.freeze();
            return this;
        }

        @Override
        protected String _getValue(int codepoint) {
            return (String) unicodeMap.getValue(codepoint);
        }

        /* protected List _getValueAliases(String valueAlias, List result) {
         if (!unicodeMap.getAvailableValues().contains(valueAlias)) return result;
         result.add(valueAlias);
         return result; // no other aliases
         }
         */@Override
        protected List _getAvailableValues(List result) {
             unicodeMap.getAvailableValues(result);
             if (toValueAliases != null) {
                 for (Object s : toValueAliases.keySet()) {
                     if (!result.contains(s)) {
                         result.add(s);
                     }
                 }
             }
             return result;
         }
    }

    public boolean isValidValue(String propertyValue) {
        if (isType(STRING_OR_MISC_MASK)) {
            return true;
        }
        Collection<String> values = getAvailableValues();
        for (String valueAlias : values) {
            if (UnicodeProperty.compareNames(valueAlias, propertyValue) == 0) {
                return true;
            }
            for (String valueAlias2 : (Collection<String>) getValueAliases(valueAlias)) {
                if (UnicodeProperty.compareNames(valueAlias2, propertyValue) == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<String> getValueAliases() {
        List<String> result = new ArrayList();
        if (isType(STRING_OR_MISC_MASK)) {
            return result;
        }
        Collection<String> values = getAvailableValues();
        for (String valueAlias : values) {
            UnicodeProperty.addAllUnique(getValueAliases(valueAlias), result);
        }
        result.removeAll(values);
        return result;
    }


    public static UnicodeSet addUntested(UnicodeSet result, boolean uniformUnassigned) {
        if (uniformUnassigned && result.contains(UnicodeProperty.getSAMPLE_UNASSIGNED())) {
            result.addAll(UnicodeProperty.getUNASSIGNED());
        }

        if (result.contains(UnicodeProperty.SAMPLE_HIGH_SURROGATE)) {
            result.addAll(UnicodeProperty.HIGH_SURROGATES);
        }
        if (result.contains(UnicodeProperty.SAMPLE_HIGH_PRIVATE_USE_SURROGATE)) {
            result.addAll(UnicodeProperty.HIGH_PRIVATE_USE_SURROGATES);
        }
        if (result.contains(UnicodeProperty.SAMPLE_LOW_SURROGATE)) {
            result.addAll(UnicodeProperty.LOW_SURROGATES);
        }

        if (result.contains(UnicodeProperty.SAMPLE_PRIVATE_USE_AREA)) {
            result.addAll(UnicodeProperty.PRIVATE_USE_AREA);
        }
        if (result.contains(UnicodeProperty.SAMPLE_PRIVATE_USE_AREA_A)) {
            result.addAll(UnicodeProperty.PRIVATE_USE_AREA_A);
        }
        if (result.contains(UnicodeProperty.SAMPLE_PRIVATE_USE_AREA_B)) {
            result.addAll(UnicodeProperty.PRIVATE_USE_AREA_B);
        }

        return result;
    }

    public static UnicodeMap addUntested(UnicodeMap result, boolean uniformUnassigned) {
        Object temp;
        if (uniformUnassigned && null != (temp = result.get(UnicodeProperty.getSAMPLE_UNASSIGNED()))) {
            result.putAll(UnicodeProperty.getUNASSIGNED(), temp);
        }

        if (null != (temp = result.get(UnicodeProperty.SAMPLE_HIGH_SURROGATE))) {
            result.putAll(UnicodeProperty.HIGH_SURROGATES, temp);
        }
        if (null != (temp = result.get(UnicodeProperty.SAMPLE_HIGH_PRIVATE_USE_SURROGATE))) {
            result.putAll(UnicodeProperty.HIGH_PRIVATE_USE_SURROGATES, temp);
        }
        if (null != (temp = result.get(UnicodeProperty.SAMPLE_LOW_SURROGATE))) {
            result.putAll(UnicodeProperty.LOW_SURROGATES, temp);
        }

        if (null != (temp = result.get(UnicodeProperty.SAMPLE_PRIVATE_USE_AREA))) {
            result.putAll(UnicodeProperty.PRIVATE_USE_AREA, temp);
        }
        if (null != (temp = result.get(UnicodeProperty.SAMPLE_PRIVATE_USE_AREA_A))) {
            result.putAll(UnicodeProperty.PRIVATE_USE_AREA_A, temp);
        }
        if (null != (temp = result.get(UnicodeProperty.SAMPLE_PRIVATE_USE_AREA_B))) {
            result.putAll(UnicodeProperty.PRIVATE_USE_AREA_B, temp);
        }
        return result;
    }

    public boolean isDefault(int cp) {
        String value = getValue(cp);
        if (isType(STRING_OR_MISC_MASK)) {
            return equals(cp, value);
        }
        String defaultValue = getValue(getSAMPLE_UNASSIGNED());
        return defaultValue == null ? value == null : defaultValue.equals(value);
    }

    public boolean hasUniformUnassigned() {
        return hasUniformUnassigned;
    }
    protected UnicodeProperty setUniformUnassigned(boolean hasUniformUnassigned) {
        this.hasUniformUnassigned = hasUniformUnassigned;
        return this;
    }

    public static class UnicodeSetProperty extends BaseProperty {
        protected UnicodeSet unicodeSet;
        private static final String[] YESNO_ARRAY = new String[]{"Yes", "No"};
        private static final List YESNO = Arrays.asList(YESNO_ARRAY);

        public UnicodeSetProperty set(UnicodeSet set) {
            unicodeSet = set.freeze();
            return this;
        }

        public UnicodeSetProperty set(String string) {
            // TODO Auto-generated method stub
            return set(new UnicodeSet(string).freeze());
        }

        @Override
        protected String _getValue(int codepoint) {
            return YESNO_ARRAY[unicodeSet.contains(codepoint) ? 0 : 1];
        }

        @Override
        protected List _getAvailableValues(List result) {
            return YESNO;
        }
    }

    //    private static class StringTransformProperty extends SimpleProperty {
    //        Transform<String,String> transform;
    //
    //        public StringTransformProperty(Transform<String,String> transform, boolean hasUniformUnassigned) {
    //            this.transform = transform;
    //            setUniformUnassigned(hasUniformUnassigned);
    //        }
    //        protected String _getValue(int codepoint) {
    //            return transform.transform(UTF16.valueOf(codepoint));
    //        }
    //    }
    //
    //    private static class CodepointTransformProperty extends SimpleProperty {
    //        Transform<Integer,String> transform;
    //
    //        public CodepointTransformProperty(Transform<Integer,String> transform, boolean hasUniformUnassigned) {
    //            this.transform = transform;
    //            setUniformUnassigned(hasUniformUnassigned);
    //        }
    //        protected String _getValue(int codepoint) {
    //            return transform.transform(codepoint);
    //        }
    //    }
}

