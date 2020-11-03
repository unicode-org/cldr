package org.unicode.cldr.api;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.Integer.signum;
import static org.unicode.cldr.api.CldrDataType.LDML;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.XPathParts;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;

/**
 * Utilities related to CLDR paths. It's possible that one day we might wish to expose the path
 * comparator from this class, but if so we should key it off something other than {@code DtdType}.
 */
final class CldrPaths {
    // Constants to make comparator logic a bit more readable (i.e. LHS < RHS ==> return -1).
    private static final int LHS_FIRST = -1;
    private static final int RHS_FIRST = 1;

    // A synthetic attribute used by CLDR code to apply an explicit ordering to otherwise identical
    // paths (this only happens for "ORDERED" elements). We handle this differently and have the
    // sort index as an explicit field in CldrPath rather than treating it as an attribute.
    private static final String HIDDEN_SORT_INDEX_ATTRIBUTE = "_q";

    // When calculating things about elements we need to ignore deprecated ones, especially when
    // looking for "leaf" elements, since CLDR data used to allow mixed content and the DTD still
    // has elements with both data and (deprecated) child elements present.
    private static final Predicate<DtdData.Element> IS_NOT_DEPRECATED = e -> !e.isDeprecated();

    // A map of the leaf element names for each supported DTD.
    private static final ImmutableSetMultimap<CldrDataType, String> LEAF_ELEMENTS_MAP;
    // A map of the ordered element names for each supported DTD.
    private static final ImmutableSetMultimap<CldrDataType, String> ORDERED_ELEMENTS_MAP;
    static {
        ImmutableSetMultimap.Builder<CldrDataType, String> leafElementsMap =
            ImmutableSetMultimap.builder();
        ImmutableSetMultimap.Builder<CldrDataType, String> orderedElementsMap =
            ImmutableSetMultimap.builder();
        for (CldrDataType type : CldrDataType.values()) {
            // While at happened to be true (at the time of writing) that the getElements() method
            // returns a new, mutable set, this is completely undocumented so we cannot rely on it
            // (otherwise we could just do "removeIf(...)").
            //
            // Note that while the "specials" element has no children in the DTD, it is not
            // considered a "leaf" element as it is expected to contain any additional elements
            // from unknown namespaces (e.g. "icu:").
            //
            // We know that Guava's collection classes have a policy of never iterating over a
            // collection more than once, so it's safe to use the ::iterator trick to convert a
            // stream into a one-shot iterable (saves have to make temporary collections).
            leafElementsMap.putAll(
                type,
                type.getElements()
                    .filter(IS_NOT_DEPRECATED)
                    // NOTE: Some leaf elements still have deprecated children (from when mixed
                    // data was permitted).
                    .filter(e -> e.getChildren().keySet().stream().noneMatch(IS_NOT_DEPRECATED))
                    .map(DtdData.Element::getName)
                    .filter(e -> !e.equals("special"))
                    ::iterator);
            orderedElementsMap.putAll(
                type,
                type.getElements()
                    .filter(IS_NOT_DEPRECATED)
                    .filter(DtdData.Element::isOrdered)
                    .map(DtdData.Element::getName)
                    ::iterator);
        }
        // Special case "alias" is an alternate leaf element for a lot of LDML elements.
        leafElementsMap.put(LDML, "alias");
        LEAF_ELEMENTS_MAP = leafElementsMap.build();
        ORDERED_ELEMENTS_MAP = orderedElementsMap.build();
    }

    // A map of path comparators for each supported DTD.
    private static final ImmutableMap<CldrDataType, DtdPathComparator> PATH_COMPARATOR_MAP;

    // This static block must come after the attribute map is created since it uses it.
    static {
        ImmutableMap.Builder<CldrDataType, DtdPathComparator> pathMap = ImmutableMap.builder();
        for (CldrDataType type : CldrDataType.values()) {
            pathMap.put(type, new DtdPathComparator(type));
        }
        PATH_COMPARATOR_MAP = pathMap.build();
    }

    /**
     * Returns a comparator for {@link CldrPath}s which is compatible with the canonical path
     * ordering for the given DTD instance (e.g. {@link DtdData#getDtdComparator}()).
     */
    // TODO: Add tests to ensure it continues to agree to DTD ordering.
    static Comparator<CldrPath> getPathComparator(CldrDataType type) {
        return PATH_COMPARATOR_MAP.get(type);
    }

    private static final class DtdPathComparator implements Comparator<CldrPath> {
        private final Comparator<String> elementNameComparator;
        private final Comparator<String> attributeNameComparator;

        private DtdPathComparator(CldrDataType dataType) {
            this.elementNameComparator = dataType.getElementComparator();
            this.attributeNameComparator = dataType.getAttributeComparator();
        }

        // This code should only return "signum" values for ordering (i.e. {-1, 0, 1}).
        @Override
        public int compare(CldrPath lhs, CldrPath rhs) {
            int length = lhs.getLength();
            if (length == rhs.getLength()) {
                if (length == 1) {
                    // Root nodes are special as they define the DTD type and must always be equal.
                    // Paths with different types can be compared, but not via this comparator.
                    checkState(lhs.getName().equals(rhs.getName()),
                        "cannot compare paths with different DTD type: %s / %s", lhs, rhs);
                    return 0;
                }
                // Compare parent paths first and return if they give an ordering.
                int signum = compare(lhs.getParent(), rhs.getParent());
                return (signum != 0) ? signum : compareCurrentElement(lhs, rhs);
            } else if (length < rhs.getLength()) {
                // Recursively shorten the RHS path until it's the same length.
                int signum = compare(lhs, rhs.getParent());
                // If the LHS is equal to the RHS parent, then the (shorter) LHS path comes first.
                // Note this this can only happen if we are comparing non-leaf node paths.
                return signum != 0 ? signum : LHS_FIRST;
            } else {
                // Flip the comparison if LHS was longer (we do this at most once per comparison).
                return -compare(rhs,  lhs);
            }
        }

        private int compareCurrentElement(CldrPath lhs, CldrPath rhs) {
            String elementName = lhs.getName();
            int signum = signum(elementNameComparator.compare(elementName, rhs.getName()));
            if (signum != 0) {
                return signum;
            }
            // Primary order within a path element is defined by the element index (this is a
            // bit of a special value used only for "ORDERED" elements in the DTD. This always
            // trumps any attribute ordering but is almost always -1.
            signum = Integer.compare(lhs.getSortIndex(), rhs.getSortIndex());
            if (signum != 0) {
                return signum;
            }
            // Element name is the same, so test attributes. Attributes are already known to be
            // ordered by the element's DTD order, so we only need to find and compare the first
            // difference.
            int minAttributeCount =
                Math.min(lhs.getAttributeCount(), rhs.getAttributeCount());
            for (int n = 0; n < minAttributeCount && signum == 0; n++) {
                String attributeName = lhs.getLocalAttributeName(n);
                // Important: We negate the comparison result here because we want elements with
                // "missing" attributes to sort earlier.
                //
                // E.g. for two elements LHS="foo[a=x][b=y]" and RHS="foo[b=y]" we want to say
                // that "RHS < LHS" because RHS is "missing" the attribute "[a=x]". But when we
                // compare the first attributes we find "a" (LHS) < "b" (RHS), which is the
                // opposite of what we want. This is because while LHS has a lower ordered
                // attribute, that indicates that RHS is missing that attribute in the same
                // position, which should make RHS sort first.
                signum = -signum(
                    attributeNameComparator.compare(attributeName, rhs.getLocalAttributeName(n)));
                if (signum == 0) {
                    // Attribute names equal, so test attribute value.
                    signum = signum(
                        DtdData.getAttributeValueComparator(elementName, attributeName)
                            .compare(lhs.getLocalAttributeValue(n), rhs.getLocalAttributeValue(n)));
                }
            }
            if (signum == 0) {
                // Attributes match up to the minimum length, but one element might have more.
                // Elements with more attributes sort _after_ those without.
                if (lhs.getAttributeCount() > minAttributeCount) {
                    signum = RHS_FIRST;
                } else if (rhs.getAttributeCount() > minAttributeCount) {
                    signum = LHS_FIRST;
                }
            }
            return signum;
        }
    }

    /** Returns whether this path is a full path ending in a "leaf" element. */
    static boolean isLeafPath(CldrPath path) {
        String lastElementName = path.getName();
        return lastElementName.indexOf(':') != -1
            || LEAF_ELEMENTS_MAP.get(path.getDataType()).contains(lastElementName);
    }

    /**
     * Returns whether an element is "ORDERED" in the specified DTD (and should have an explicit
     * sort index).
     */
    static boolean isOrdered(CldrDataType dtdType, String elementName) {
        // Elements with namespaces unknown the the DTD are never ordered.
        if (elementName.indexOf(':') != -1) {
            return false;
        }
        // Returns empty set if DTD unknown, but it might also be the case that a valid DTD has no
        // ordered elements, so we can't reasonable check for anything here.
        return ORDERED_ELEMENTS_MAP.get(dtdType).contains(elementName);
    }

    // This can't go further up due to static initialization ordering issues.
    // TODO: Move all reading of DTDs and setup for paths into a lazy holder.
    private static final CldrPath LDML_VERSION =
        CldrPath.parseDistinguishingPath("//ldml/identity/version");

    /**
     * Returns whether this path should be emitted by a data supplier. New cases can be added
     * as needed.
     */
    static boolean shouldEmit(CldrPath path) {
        // CLDRFile seems to do some interesting things with the version based on the DTD in
        // which we see:
        //
        // <!ATTLIST version number CDATA #REQUIRED >
        //    <!--@MATCH:regex/\$Revision.*\$-->
        //    <!--@METADATA-->
        //
        // <!ATTLIST version cldrVersion CDATA #FIXED "36" >
        //    <!--@MATCH:any-->
        //    <!--@VALUE-->
        //
        // This results in conflict between values obtained via CLDRFile and those obtained
        // directly by parsing an LDML XML file (which happens for "special" XML files in ICU).
        //
        // So for now I've decided to just ignore the version (since it's hardly used in the
        // ICU converter code and always available via CldrDataSupplier#getCldrVersionString().

        // Hacky way to detect the version declaration in LDML files (which must always be
        // skipped since they are duplicate paths and reveal XML file boundaries). The path is
        // always "//ldmlXxxx/version" for some DTD type.
        if (path.getLength() == 2 && path.getName().equals("version")) {
            return false;
        }

        // Note that there is a need for some kind of versioning for some bits of data (since
        // changes to things like collation can invalidate database search indexes) but this should
        // be handled directly in the logic which builds the ICU data and isn't strictly the same
        // as the LDML version anyway.
        //
        // TODO: Remove this code if and when LDML version strings are removed.
        return !path.equals(LDML_VERSION);
    }

    /**
     * Processes a full path to extract a distinguishing CldrPath and handle any value attributes
     * present. This is designed for iterating over successive paths from CLDRFile, but can be used
     * to create paths in isolation if necessary.
     *
     * @param fullPath a parsed full path.
     * @param previousElements an optional list of previous CldrPath elements which will be used
     *     as the prefix to the new path wherever possible (e.g. if previousElements="(a,b,c,d)"
     *     and "fullPath=a/b/x/y/z", then the new path will share the path prefix up to and
     *     including 'b'). When processing sorted paths, this will greatly reduce allocations.
     * @param valueAttributeCollector a collector into which value attributes will be added (in
     *     DTD order).
     * @return a new CldrPath corresponding to the distinguishing path of {@code fullPath}.
     * @throws IllegalArgumentException if the path string is invalid.
     */
    static CldrPath processXPath(
        String fullPath,
        List<CldrPath> previousElements,
        BiConsumer<AttributeKey, String> valueAttributeCollector) {
        checkArgument(!fullPath.isEmpty(), "path must not be empty");
        // This fails if attribute names are invalid, but not if element names are invalid, and we
        // want to control/stabalize the error messages in this API.
        XPathParts pathParts;
        try {
            pathParts = XPathParts.getFrozenInstance(fullPath);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid path: " + fullPath);
        }
        int length = pathParts.size();
        checkArgument(length > 0, "cldr path must not be empty: %s", pathParts);
        DtdData dtd = pathParts.getDtdData();
        checkArgument(dtd != null, "unknown DTD type: %s", pathParts);
        CldrDataType dtdType = CldrDataType.forRawType(dtd.dtdType);

        // The path we're returning, constructed from top-to-bottom.
        CldrPath path = null;
        // Reusable key/value attributes list.
        List<String> keyValuePairs = new ArrayList<>();
        Consumer<Entry<String, String>> collectElementAttribute = e -> {
            keyValuePairs.add(e.getKey());
            keyValuePairs.add(e.getValue());
        };
        // True once we've started to create a new path rather than reusing previous elements.
        boolean diverged = false;
        for (int n = 0; n < length; n++) {
            String elementName = pathParts.getElement(n);

            // If this path is from CldrPath.toString() then the sort index is encoded in the
            // element name suffix (e.g. foo#42[@bar="x"]), otherwise it's in the synthetic "_q"
            // attribute. Most often there is no sort index however, so this code should optimize
            // to the null case.
            int sortIndex = -1;
            Map<String, String> attributes = pathParts.getAttributes(n);
            int nameEnd = elementName.indexOf('#');
            if (nameEnd != -1) {
                sortIndex = Integer.parseUnsignedInt(elementName.substring(nameEnd + 1));
                elementName = elementName.substring(0, nameEnd);
            } else {
                String sortIndexStr = attributes.get(HIDDEN_SORT_INDEX_ATTRIBUTE);
                if (sortIndexStr != null) {
                    sortIndex = Integer.parseUnsignedInt(sortIndexStr);
                }
            }

            // Note that element names need not be known to the DTD. If an element's name is
            // prefixed with an unknown namespace (e.g. "icu:") then it is always permitted.
            // Similarly, we never filter out attributes in unknown namespaces. For now we assume
            // that any explicit namespace is unknown.
            boolean hasNamespace = elementName.indexOf(':') != -1;
            checkArgument(hasNamespace || dtd.getElementFromName().containsKey(elementName),
                "invalid path: %s", fullPath);

            // The keyValuePairs list is used by the collectElementAttribute callback but we don't
            // want/need to make a new callback each time, so just clear the underlying list.
            keyValuePairs.clear();
            processPathAttributes(
                elementName, attributes, dtdType, collectElementAttribute, valueAttributeCollector);

            // WARNING: We cannot just get the draft attribute value from the attributes map (as
            // would be expected) because it throws an exception. This is because you can only
            // "get()" values from the attributes map if they are potentially valid attributes for
            // that element. Unfortunately this is due to a deliberate choice in the implementation
            // of MapComparator, which explicitly throws an exception if an unknown attribute is
            // encountered. Thus we have to check that "draft" is a possible attribute before
            // asking for its value.
            //
            // TODO(dbeaumont): Fix this properly, ideally by fixing the comparator to not throw.
            CldrDraftStatus draftStatus = dtd.getAttribute(elementName, "draft") != null
                ? CldrDraftStatus.forString(attributes.get("draft")) : null;

            if (!diverged && n < previousElements.size()) {
                CldrPath p = previousElements.get(n);
                if (p.matchesContent(elementName, sortIndex, keyValuePairs, draftStatus)) {
                    // The previous path we processed was the same at least down to this depth, so
                    // we can just reuse that element instead of creating a new one.
                    //
                    // In tests over the resolved paths for "en_GB" in DTD order, there are ~128k
                    // path elements processed, with ~103k being reused here and ~25k being newly
                    // allocated (a > 80% reuse rate). However this works best when the incoming
                    // paths are sorted, since otherwise the "previous" path is random.
                    //
                    // For unsorted paths, the reuse rate is reduced to approximately 25%. This is
                    // still saving ~32k allocations for the "en_GB" example, and it still saved
                    // about 35% in terms of measured time to process the paths.
                    path = p;
                    continue;
                }
            }
            // This path has diverged from the previous path, so we must start making new elements.
            path = new CldrPath(path, elementName, keyValuePairs, dtdType, draftStatus, sortIndex);
            diverged = true;
        }
        return path;
    }

    // Returns the element's sort index (or -1 if not present).
    static void processPathAttributes(
        String elementName,
        /* @Nullable */ Map<String, String> attributeMap,
        CldrDataType dtdType,
        Consumer<Entry<String, String>> collectElementAttribute,
        BiConsumer<AttributeKey, String> collectValueAttribute) {

        // Why not just a Map? Maps don't efficiently handle "get the Nth element" which is
        // something that's used a lot in the ICU conversion code. Distinguishing attributes in
        // paths are used more as a "list of pairs" than a map with key/value lookup. Even
        // extensions like "NavigableMap" don't give you what you want.
        //
        // This does mean that lookup-by-name is a linear search (unless you want to pay the cost
        // of also having a map here) but the average number of attributes is very small (<3).
        if (attributeMap != null && !attributeMap.isEmpty()) {
            processAttributes(
                attributeMap.entrySet().stream(), elementName, collectValueAttribute, dtdType)
                .forEach(collectElementAttribute);
        }
    }

    static Stream<Entry<String, String>> processAttributes(
        Stream<Entry<String, String>> in,
        String elementName,
        BiConsumer<AttributeKey, String> collectValueAttribute,
        CldrDataType dtdType) {
        Consumer<Entry<String, String>> collectValueAttributes = e -> {
            if (dtdType.isValueAttribute(elementName, e.getKey())) {
                collectValueAttribute.accept(AttributeKey.keyOf(elementName, e.getKey()), e.getValue());
            }
        };
        return in
            // Special case of a synthetic distinguishing attribute that's _not_ in the DTD, and
            // should be ignored.
            .filter(e -> !e.getKey().equals(HIDDEN_SORT_INDEX_ATTRIBUTE))
            .peek(collectValueAttributes)
            .filter(e -> dtdType.isDistinguishingAttribute(elementName, e.getKey()));
    }

    private CldrPaths() {}
}
