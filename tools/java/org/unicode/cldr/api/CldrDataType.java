package org.unicode.cldr.api;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.function.Function.identity;
import static org.unicode.cldr.util.DtdData.AttributeStatus.distinguished;
import static org.unicode.cldr.util.DtdData.AttributeStatus.value;
import static org.unicode.cldr.util.DtdData.Mode.OPTIONAL;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdData.Attribute;
import org.unicode.cldr.util.DtdData.Element;
import org.unicode.cldr.util.DtdType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Data types for non-locale based CLDR data. For the canonical specification for LDML data can
 * be found at <a href="https://unicode.org/reports/tr35">Unicode Locale Data Markup Language<\a>.
 *
 * <p>This enum is largely a wrapper for functionality found in the underlying CLDR classes, but
 * repackaged for convenience and to minimize surface area (and to avoid anyone needing to import
 * classes from outside the "api" package).
 */
public enum CldrDataType {
    /**
     * Non-locale based BCP47 data, typically associated with international identifiers such as
     * currency symbols, timezone identifiers etc.
     */
    BCP47(DtdType.ldmlBCP47),
    /**
     * Non-locale based supplemental data, typically associated with character tables (e.g. for
     * break iterator).
     */
    SUPPLEMENTAL(DtdType.supplementalData),
    /**
     * Locale based LDML data consisting of internationalization information and translations on a
     * per locale basis. LDML data for one locale may be inherited from other locales.
     */
    LDML(DtdType.ldml, DtdType.ldmlICU);

    private static final ImmutableMap<String, CldrDataType> NAME_MAP =
        Arrays.stream(values()).collect(toImmutableMap(t -> t.mainType.name(), identity()));

    /**
     * Returns a CLDR data type given its XML name (the root element name in a CLDR path).
     *
     * @param name the XML path root (e.g. "ldml" or "supplementalData").
     * @return the associated data type instance.
     */
    public static CldrDataType forXmlName(String name) {
        CldrDataType type = NAME_MAP.get(name);
        checkArgument(type != null, "unsupported DTD type: %s", name);
        return type;
    }

    static CldrDataType forRawType(DtdType rawType) {
        return forXmlName(rawType.name());
    }

    private final DtdType mainType;
    private final ImmutableList<DtdType> extraTypes;
    private final Comparator<String> elementComparator;
    private final Comparator<String> attributeComparator;

    CldrDataType(DtdType mainType, DtdType... extraTypes) {
        this.mainType = mainType;
        this.extraTypes = ImmutableList.copyOf(extraTypes);
        // There's no need to cache the DtdData instance since getInstance() already does that.
        DtdData dtd = DtdData.getInstance(mainType);
        // Note that the function passed in to the wrapped comparators needs to be fast, since it's
        // called for each comparison. We assume getElementFromName() and getAttributesFromName()
        // are efficient, and if not we'll need to cache.
        this.elementComparator =
            wrapToHandleUnknownNames(
                dtd.getElementComparator(),
                dtd.getElementFromName()::containsKey);
        this.attributeComparator =
            wrapToHandleUnknownNames(
                dtd.getAttributeComparator(),
                dtd.getAttributesFromName()::containsKey);
    }

    String getLdmlName() {
        return mainType.name();
    }

    Stream<Path> getSourceDirectories() {
        return mainType.directories.stream().map(Paths::get);
    }

    /**
     * Returns all elements known for this DTD type in undefined order. This can include elements
     * in external namespaces (e.g. "icu:xxx").
     */
    Stream<Element> getElements() {
        Stream<Element> elements = elementsFrom(mainType);
        if (!extraTypes.isEmpty()) {
            elements =
                Stream.concat(elements, extraTypes.stream().flatMap(CldrDataType::elementsFrom));
        }
        return elements;
    }

    private static Stream<Element> elementsFrom(DtdType dataType) {
        // NOTE: DO NOT call getElements() here because it makes a new set every time!!
        return DtdData.getInstance(dataType).getElementFromName().values().stream();
    }

    Attribute getAttribute(String elementName, String attributeName) {
        Attribute attr = DtdData.getInstance(mainType).getAttribute(elementName, attributeName);
        if (attr == null) {
            for (DtdType t : extraTypes) {
                attr = DtdData.getInstance(t).getAttribute(elementName, attributeName);
                if (attr != null) {
                    break;
                }
            }
        }
        return attr;
    }

    Comparator<String> getElementComparator() {
        return elementComparator;
    }

    Comparator<String> getAttributeComparator() {
        return attributeComparator;
    }

    // Unknown elements outside the DTD (such as "//ldml/special" icu:xxx elements) are not
    // handled properly by the underlying element/attribute name comparators (they throw an
    // exception) so we have to detect these cases first and handle them manually (even though
    // they are very rare). Assume that:
    // * known DTD elements come before any unknown ones, and
    // * unknown element names can be sorted lexicographically using their qualified name.
    private static Comparator<String> wrapToHandleUnknownNames(
        Comparator<String> compare, Predicate<String> isKnown) {
        // This code should only return "signum" values for ordering (i.e. {-1, 0, 1}).
        return (lname, rname) -> {
            if (isKnown.test(lname)) {
                return isKnown.test(rname) ? compare.compare(lname, rname) : -1;
            } else {
                return isKnown.test(rname) ? 1 : lname.compareTo(rname);
            }
        };
    }

    // We shouldn't need to check special cases (e.g. "_q") here because this should only be being
    // called _after_ those have been filtered out.
    // The only time that both these methods return false should be for known attributes that are
    // either marked as deprecated or as metatadata attributes.
    boolean isDistinguishingAttribute(String elementName, String attributeName) {
        Attribute attribute = getAttribute(elementName, attributeName);
        if (attribute != null) {
            return attribute.attributeStatus == distinguished && !attribute.isDeprecated();
        }
        // This can happen if attribute keys are speculatively generated, which sometimes happens
        // in transformation logic. Ideally this would end up being an error.
        return false;
    }

    /** Returns whether the specified attribute is a "value" attribute. */
    boolean isValueAttribute(String elementName, String attributeName) {
        Attribute attribute = getAttribute(elementName, attributeName);
        if (attribute != null) {
            return attribute.attributeStatus == value && !attribute.isDeprecated();
        }
        return true;
    }

    /** Returns whether the specified attribute is a "value" attribute.  */
    boolean isValueAttribute(AttributeKey key) {
        return isValueAttribute(key.getElementName(), key.getAttributeName());
    }

    /**
     * Returns whether the specified attribute is optional. Attributes unknown to the DTD are also
     * considered optional, which can happen if attribute keys are speculatively generated, which
     * sometimes happens in transformation logic.
     */
    boolean isOptionalAttribute(AttributeKey key) {
        Attribute attribute = getAttribute(key.getElementName(), key.getAttributeName());
        return attribute == null || attribute.mode == OPTIONAL;
    }
}
