package org.unicode.cldr.api;

import static com.google.common.base.CharMatcher.anyOf;
import static com.google.common.base.CharMatcher.inRange;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toCollection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.unicode.cldr.api.AttributeKey.AttributeSupplier;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * A sequence of CLDR path elements and "distinguishing" attributes.
 *
 * <p>In CLDR, a path is composed of a series of elements and associated attributes, where the
 * attributes can be one of three types; "distinguishing", "value" and "metadata".
 *
 * <p>CldrPath instances hold only "distinguishing" attributes, with "value" attributes being held
 * by the associated {@link CldrValue} instance, and "metadata" attributes being ignored completely
 * since they are internal to the core CLDR classes. This approach ensures that {@code CldrPath}
 * instances uniquely identify values and can be used as keys in maps.
 *
 * <p>When viewing {@code CldrPath} as strings, it is sometimes necessary to introduce an suffix to
 * the path element name to indicate the "sort order". This is necessary to represent "ordered"
 * elements which can appear in the LDML data (though these are rare). This suffix takes the form of
 * {@code "{element-name}#{sort-index}"} where {@code {sort-index}} is a non-negative index which
 * corresponds to the value returned from {@link #getSortIndex()} for that path element. The natural
 * ordering of {@code CldrPath} handles the correctly, but you cannot sort paths properly by just
 * comparing their string representation.
 *
 * <p>CldrPath is an immutable value type with efficient equality semantics.
 *
 * <p>See <a href="https://www.unicode.org/reports/tr35/#Definitions">the LDML specification</a>
 * for more details.
 */
public final class CldrPath implements AttributeSupplier, Comparable<CldrPath> {
    // This is approximate and DOES NOT promise correctness, it's mainly there to catch unexpected
    // changes in the data and it can be updated or removed if needed. At the very least element
    // names must not contain '/', '[', ']', '@', '=', '#' or '"' to permit re-parsing of paths.
    private static final CharMatcher NAME_CHARACTERS =
        inRange('a', 'z').or(inRange('A', 'Z')).or(inRange('0', '9')).or(anyOf(":-_"));

    /**
     * Parses a distinguishing CLDR path string, including "distinguishing" and private "metadata"
     * attributes into a normalized {@link CldrPath} instance. Attributes will be parsed and handled
     * according to their type:
     * <ul>
     * <li>Distinguishing attributes will be added to the returned CldrPath instance.
     * <li>Non-public metadata attributes will be ignored.
     * <li>Value attributes are not permitted in distinguishing paths and will cause an error.
     * </ul>
     *
     * <p>The path string must be structured correctly (e.g. "//ldml/foo[@bar="baz]") and must
     * represent a known DTD type, based on the first path element (e.g. "ldml" or
     * "supplementalData").
     *
     * @param path the distinguishing path string, containing only distinguishing attributes.
     * @return the parsed distinguishing path instance.
     * @throws IllegalArgumentException if the path is not well formed (e.g. contains unexpected
     *      value or metadata attributes).
     */
    public static CldrPath parseDistinguishingPath(String path) {
        return CldrPaths.processXPath(path, ImmutableList.of(), (k, v) -> {
            throw new IllegalArgumentException(String.format(
                "unexpected value attribute '%s' in distinguishing path: %s", k, path));
        });
    }

    /**
     * Returns the number of common path elements from the root. This is useful when determining
     * the last common ancestor during visitation. A {@code null} path is treated as having zero
     * length (and will always result in zero being returned).
     *
     * <p>Note: This is only currently use by PrefixVisitorHost, but could be made public if needed.
     * It's only here (rather than in PrefixVisitorHost) because it depends on private methods of
     * this class.
     */
    static int getCommonPrefixLength(/* @Nullable */ CldrPath a, /* @Nullable */ CldrPath b) {
        // Null paths are sentinels with zero length.
        if (a == null || b == null) {
            return 0;
        }

        // Trim whichever path is longer until both are same length.
        int minLength = Math.min(a.getLength(), b.getLength());
        while (a.getLength() > minLength)
            a = a.getParent();
        while (b.getLength() > minLength)
            b = b.getParent();

        // Work up the paths, resetting the common length every time the elements differ.
        int commonLength = minLength;
        for (int length = minLength; length > 0; length--) {
            if (!a.localEquals(b)) {
                // Elements differ, so shortest possible common length is that of our parent path.
                commonLength = length - 1;
            }
            // Parents will both be null on the last iteration, but never used.
            a = a.getParent();
            b = b.getParent();
        }
        return commonLength;
    }

    // If the parent is null then this path element is the top level LDML descriptor.
    /* @Nullable */ private final CldrPath parent;
    // The number of elements (including this one) in this path.
    private final int length;
    private final String elementName;
    // The attribute keys and values in an alternating list.
    private final ImmutableList<String> attributeKeyValuePairs;
    // Inherits the top-most draft status in a path (which matches what CLDRFile appears to do.
    private final Optional<CldrDraftStatus> draftStatus;
    // The sort index for "ORDERED" path elements or (more commonly) -1 for non-ORDERED elements.
    private final int sortIndex;
    // The DTD type of the path (which is the same for all path elements).
    private final CldrDataType dtdType;
    // The proper DTD ordering for this path (based on the DTD type).
    private final Comparator<CldrPath> ordering;
    // Cached since ImmutableList recalculates its hash codes every time.
    private final int hashCode;
    // Cached on demand (it's useful during equality checking as well as toString() itself).
    // However for elements with attributes, it's at least as large as the name and all attribute
    // keys/values combined, so will typically double the in-memory size of the instance. We don't
    // care about making assignment atomic however, since all values would be equal anyway.
    private String localToString = null;

    CldrPath(CldrPath parent,
        String name,
        List<String> attributeKeyValuePairs,
        CldrDataType dtdType,
        /* @Nullable */ CldrDraftStatus localDraftStatus, int sortIndex) {
        checkState(parent != null || dtdType.getLdmlName().equals(name),
            "unexpected root element: expected %s, but got %s", dtdType.getLdmlName(), name);
        this.parent = parent;
        this.length = (parent != null ? parent.getLength() : 0) + 1;
        this.elementName = checkValidName(name, "element");
        this.attributeKeyValuePairs = checkKeyValuePairs(attributeKeyValuePairs);
        this.draftStatus = resolveDraftStatus(parent, localDraftStatus);
        // Ordered elements have a sort index of 0 or more, and un-ordered have an index of -1.
        if (CldrPaths.isOrdered(dtdType, elementName)) {
            checkArgument(sortIndex >= 0,
                "missing or invalid sort index '%s' for element: %s", sortIndex, elementName);
        } else {
            checkArgument(sortIndex == -1,
                "unexpected sort index '%s' for element: %s", sortIndex, elementName);
        }
        this.sortIndex = sortIndex;
        this.dtdType = checkNotNull(dtdType);
        this.ordering = CldrPaths.getPathComparator(dtdType);
        this.hashCode = Objects.hash(length, name, this.attributeKeyValuePairs, parent);
    }

    private static String checkValidName(String value, String description) {
        checkArgument(!value.isEmpty(), "%s name cannot be empty", description);
        checkArgument(NAME_CHARACTERS.matchesAllOf(value),
            "invalid character in %s name: %s", description, value);
        return value;
    }

    private static ImmutableList<String> checkKeyValuePairs(List<String> keyValuePairs) {
        // Ensure attribute values never have double-quote in them (since current we don't escape
        // value when putting into toString().
        checkArgument((keyValuePairs.size() & 1) == 0,
            "key/value pairs must have an even number of elements: %s", keyValuePairs);
        for (int n = 0; n < keyValuePairs.size(); n += 2) {
            checkValidName(keyValuePairs.get(n), "attribute");
            String v = keyValuePairs.get(n + 1);
            checkArgument(!v.contains("\""), "unsupported '\"' in attribute value: %s", v);
        }
        return ImmutableList.copyOf(keyValuePairs);
    }

    /**
     * Helper method used to test for equivalent path element content. Used to help avoid
     * unnecessary allocations during processing (see {@link CldrFileDataSource}).
     */
    boolean matchesContent(
        String name,
        int sortIndex,
        List<String> keyValuePairs, /* Nullable */ CldrDraftStatus draftStatus) {
        return this.elementName.equals(name)
            && this.sortIndex == sortIndex
            && this.attributeKeyValuePairs.equals(keyValuePairs)
            && this.draftStatus.equals(resolveDraftStatus(this.parent, draftStatus));
    }

    // Helper to resolve the current draft status of a path based on any local draft status
    // attributes and any existing parent status.
    //
    // Note that this behaviour currently matches CLDRFile behaviour as of May 2019. CLDRFile
    // does a regex match over the full path and finds the first draft status attribute that's
    // present, ignoring any later declarations (even if they are more restrictive). It seems
    // likely that the implicit expectation in CLDRFile is that there's only ever one draft
    // status attributes in any given path (see the pop() method in MyDeclHandler).
    private static Optional<CldrDraftStatus> resolveDraftStatus(
        /* @Nullable */ CldrPath parent, /* @Nullable */ CldrDraftStatus localStatus) {
        if (parent != null && parent.draftStatus.isPresent()) {
            return parent.draftStatus;
        }
        return localStatus != null ? localStatus.asOptional() : Optional.empty();
    }

    /**
     * Returns the parent path element.
     *
     * @return the parent path element (or {@code null} if this was the root element).
     */
    /* @Nullable */
    public CldrPath getParent() {
        return parent;
    }

    /**
     * Returns the non-zero length of this path (i.e. the number of elements it is made up of).
     *
     * @return the number of elements in this path.
     */
    public int getLength() {
        return length;
    }

    /**
     * Returns the name of this path element. This is the qualified XML name as it appears in the
     * CLDR XML files, including namespace (e.g. "icu:transforms") though most attributes are not
     * part of an explicit namespace.
     *
     * @return the ASCII-only name of the "lowest" element in this path.
     */
    // TODO: This method name is weak - perhaps getElementName() ?
    public String getName() {
        return elementName;
    }

    /**
     * Returns the sort index of an "ordered" path element, or {@code -1} for non-ordered elements.
     *
     * <p>The sort index is used to disambiguate and sort otherwise identical distinguishing paths.
     * The feature allows a series of values to be processed reliably in an ordered sequence. It is
     * recommended that if your data includes "ordered" elements, they are always processed in
     * {@link org.unicode.cldr.api.CldrData.PathOrder#DTD DTD} order.
     *
     * @return the element's sort index (which takes priority for element ordering over any
     *     attribute values).
     */
    public int getSortIndex() {
        return sortIndex;
    }

    /**
     * Returns the raw value of an attribute associated with this CLDR path, or null if not present.
     * For almost all use cases it is preferable to use the accessor methods on the {@link
     * AttributeKey} class, which provide additional useful semantic checking and common type
     * conversion. You should only use this method directly if there's a strong performance
     * requirement.
     *
     * @param key the key identifying an attribute.
     * @return the attribute value or {@code null} if not present.
     * @see AttributeKey
     */
    @Override
    /* @Nullable */ public String get(AttributeKey key) {
        checkArgument(!getDataType().isValueAttribute(key),
            "cannot get 'value attribute' values from a distinguishing path: %s", key);
        String v = null;
        for (CldrPath p = this; v == null && p != null; p = p.getParent()) {
            if (p.getName().equals(key.getElementName())) {
                v = p.getLocalAttributeValue(key.getAttributeName());
            }
        }
        return v;
    }

    /**
     * Returns the data type for this path, as defined by the top most element.
     *
     * @return the path's data type.
     */
    @Override
    public CldrDataType getDataType() {
        return dtdType;
    }

    /**
     * Returns whether the given element name is in this path.
     *
     * @param elementName the element name to check.
     * @return true if the name of this path element or any of its parents is equal to the given
     *     element name.
     */
    boolean containsElement(String elementName) {
        return getName().equals(elementName)
            || (parent != null && parent.containsElement(elementName));
    }

    /**
     * Returns the number of distinguishing attributes for this path element.
     *
     * @return the number of distinguishing attributes for the "lowest" element in this path.
     */
    int getAttributeCount() {
        return attributeKeyValuePairs.size() / 2;
    }

    /**
     * Returns the (non empty) name of the Nth distinguishing attribute in the leaf path element.
     *
     * @param n the index of a distinguishing attribute for the "lowest" element in this path.
     * @return the name of the Nth attribute on this path element.
     */
    String getLocalAttributeName(int n) {
        checkElementIndex(n, getAttributeCount());
        return attributeKeyValuePairs.get(2 * n);
    }

    /**
     * Returns the value of the Nth distinguishing attribute in the leaf path element.
     *
     * @param n the index of a distinguishing attribute for the "lowest" element in this path.
     * @return the value of the Nth attribute on this path element.
     */
    String getLocalAttributeValue(int n) {
        checkElementIndex(n, getAttributeCount());
        return attributeKeyValuePairs.get((2 * n) + 1);
    }

    // Helper to get an attribute by name from this path element.
    private String getLocalAttributeValue(String attributeName) {
        checkNotNull(attributeName);
        for (int i = 0; i < attributeKeyValuePairs.size(); i += 2) {
            if (attributeName.equals(attributeKeyValuePairs.get(i))) {
                return attributeKeyValuePairs.get(i + 1);
            }
        }
        return null;
    }

    /**
     * Returns the draft status for this path, which is inherited from parent path elements. Note
     * that where multiple (possibly conflicting) draft statuses are defined in a path, the "top
     * most" (i.e. closest to the root element) value is used.
     *
     * @return the potentially inherited draft status.
     */
    Optional<CldrDraftStatus> getDraftStatus() {
        return draftStatus;
    }

    /**
     * Returns a combined full path string in the XPath style {@code //foo/bar[@x="y"]/baz},
     * with value attributes inserted in correct DTD order for each path element.
     *
     * <p>Note that while in most cases the values attributes simply follow the path attributes on
     * each element, this is not necessarily always true, and DTD ordering can place value
     * attributes before path attributes in an element.
     *
     * @param value a value to be associated with this path (from which value attributes will be
     *              obtained).
     * @return the full XPath representation containing both distinguishing and value attributes.
     */
    String getFullPath(CldrValue value) {
        checkNotNull(value);
        return appendToString(new StringBuilder(), value.getValueAttributes()).toString();
    }

    /** Compares two paths in DTD order. */
    @Override
    public int compareTo(CldrPath other) {
        if (dtdType == other.dtdType) {
            return ordering.compare(this, other);
        }
        return dtdType.compareTo(other.dtdType);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof CldrPath)) {
            return false;
        }
        CldrPath other = (CldrPath) obj;
        // Check type and length first (checking length catches most non-equal paths).
        if (!getDataType().equals(other.getDataType()) || length != other.length) {
            return false;
        }
        // Do (n - 1) comparisons since we already know the root element is the same (the root
        // element never has value attributes on it and is uniquely defined by the DTD type).
        // Working up from the "leaf" of the path works well because different paths will almost
        // always be different in the leaf element (i.e. we will fail almost immediately).
        CldrPath pthis = this;
        for (int n = length - 1; n > 0; n--) {
            if (!pthis.localEquals(other)) {
                return false;
            }
            pthis = pthis.getParent();
            other = other.getParent();
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return hashCode;
    }

    /** @return the distinguishing path string in the XPath style {@code //foo/bar[@x="y"]/baz}. */
    @Override
    public String toString() {
        return appendToString(new StringBuilder(), ImmutableMap.of()).toString();
    }

    private boolean localEquals(CldrPath other) {
        // In _theory_ only length and localToString need to be checked (since the localToString is
        // an unambiguous representation of the data, but it seems a bit hacky to rely on that.
        return this.elementName.equals(other.elementName)
            && this.sortIndex == other.sortIndex
            && this.attributeKeyValuePairs.equals(other.attributeKeyValuePairs);
    }

    // XPath like toString() representation of a path element (e.g. foo[@bar="x"][@baz="y"]).
    // When a sort index is present, it's appended to the element name (e.g. "foo#42[@bar="x"]").
    private String getLocalToString() {
        if (localToString == null) {
            String str = getName();
            if (sortIndex != -1) {
                // This is very rare so it's almost certainly better to not make a StringBuilder
                // above just for this possibility.
                str += "#" + sortIndex;
            }
            if (getAttributeCount() > 0) {
                str = IntStream.range(0, getAttributeCount())
                    .mapToObj(n ->
                        String.format("[@%s=\"%s\"]", getLocalAttributeName(n), getLocalAttributeValue(n)))
                    .collect(Collectors.joining("", str, ""));
            }
            // Overwrite only once the local string is completed (this is idempotent so we don't
            // have to care about locking etc.).
            localToString = str;
        }
        return localToString;
    }

    // Recursive helper for toString().
    private StringBuilder appendToString(
        StringBuilder out, ImmutableMap<AttributeKey, String> valueAttributes) {
        CldrPath parent = getParent();
        if (parent != null) {
            parent.appendToString(out, valueAttributes).append('/');
        } else {
            out.append("//");
        }
        if (valueAttributes.isEmpty()) {
            return out.append(getLocalToString());
        }
        List<String> attributeNames =
            valueAttributes.keySet().stream()
                .filter(k -> k.getElementName().equals(getName()))
                .map(AttributeKey::getAttributeName)
                .collect(toCollection(ArrayList::new));
        if (attributeNames.isEmpty()) {
            // No value attributes for _this_ element so can use just the local toString().
            return out.append(getLocalToString());
        }
        if (getAttributeCount() > 0) {
            String lastPathAttributeName = getLocalAttributeName(getAttributeCount() - 1);
            if (dtdType.getAttributeComparator()
                .compare(lastPathAttributeName, attributeNames.get(0)) > 0) {
                // Oops, order is not as expected, so must reorder all attributes.
                appendResortedValueAttributesTo(out, attributeNames, valueAttributes);
                return out;
            }
        }
        // Value attributes all come after path attributes.
        return appendValueAttributesTo(out.append(getLocalToString()), attributeNames, valueAttributes);
    }

    private void appendResortedValueAttributesTo(
        StringBuilder out,
        List<String> attributeNames,
        ImmutableMap<AttributeKey, String> valueAttributes) {
        out.append(elementName);
        for (int n = 0; n < attributeKeyValuePairs.size(); n += 2) {
            attributeNames.add(attributeKeyValuePairs.get(n));
        }
        attributeNames.sort(dtdType.getAttributeComparator());
        for (String attrName : attributeNames) {
            String value = getLocalAttributeValue(attrName);
            if (value == null) {
                value = valueAttributes.get(AttributeKey.keyOf(elementName, attrName));
                checkState(value != null, "missing value %s:%s", elementName, attrName);
            }
            out.append(String.format("[@%s=\"%s\"]", attrName, value));
        }
    }

    private StringBuilder appendValueAttributesTo(
        StringBuilder out,
        List<String> attributeNames,
        ImmutableMap<AttributeKey, String> valueAttributes) {
        for (String attrName : attributeNames) {
            String value = valueAttributes.get(AttributeKey.keyOf(elementName, attrName));
            checkState(value != null, "missing value %s:%s", elementName, attrName);
            out.append(String.format("[@%s=\"%s\"]", attrName, value));
        }
        return out;
    }
}
