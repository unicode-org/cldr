package org.unicode.cldr.api;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableTable.toImmutableTable;
import static java.util.function.Function.identity;
import static org.unicode.cldr.util.DtdData.AttributeStatus.distinguished;
import static org.unicode.cldr.util.DtdData.AttributeStatus.value;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.unicode.cldr.util.DtdData.Attribute;

import com.google.common.base.Ascii;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;

/**
 * Immutable identifier which holds both an attribute's name and the path element it is associated
 * with. It is expected that key instances will be created as static constants in code rather than
 * being generated each time they are used.
 *
 * <p>As well as providing a key for looking up attribute values from {@link CldrPath} or {@link
 * CldrValue}, this class offers accessor methods to provide additional common semantics. This
 * includes checking and parsing boolean values, and splitting lists. It is generally preferred to
 * use the methods from this class rather than accessing the raw attribute value.
 *
 * <p>For example, prefer:
 * <pre>{@code
 *   // The attribute value cannot be null.
 *   String attribute = REQUIRED_ATTRIBUTE_KEY.valueFrom(path);
 * }</pre>
 * to:
 * <pre>{@code
 *   // This could be null.
 *   String attribute = path.get(REQUIRED_ATTRIBUTE_KEY);
 * }</pre>
 *
 */
// Note: Using Guava's @AutoValue library would remove all this boiler-plate.
public final class AttributeKey {
    // Unsorted cache of all possible known attribute keys (not including keys for elements in
    // external namespaces (e.g. "icu:").
    private static final ImmutableTable<String, String, AttributeKey> KNOWN_KEYS =
        Arrays.stream(CldrDataType.values())
            .flatMap(CldrDataType::getElements)
            .flatMap(e -> e.getAttributes().keySet().stream()
                .filter(AttributeKey::isKnownAttribute)
                .map(a -> new AttributeKey(e.getName(), a.getName())))
            .distinct()
            .collect(toImmutableTable(
                AttributeKey::getElementName, AttributeKey::getAttributeName, identity()));

    private static boolean isKnownAttribute(Attribute attr) {
        return !attr.isDeprecated() &&
            (attr.attributeStatus == distinguished || attr.attributeStatus == value);
    }

    private static final Splitter LIST_SPLITTER =
        Splitter.on(CharMatcher.whitespace()).omitEmptyStrings();

    /**
     * Common interface to permit both {@link CldrPath} and {@link CldrValue} to have attributes
     * processed by the methods in this class.
     */
    interface AttributeSupplier {
        /** Returns the raw attribute value, or null. */
        /* @Nullable */ String get(AttributeKey k);

        /** Returns the data type of this supplier. */
        CldrDataType getDataType();
    }

    /**
     * Returns a key which identifies an attribute in either {@link CldrValue} or {@link CldrPath}.
     *
     * <p>It is expected that callers will typically store the keys for desired attributes as
     * constant static fields rather than creating new keys each time they are needed.
     *
     * @param elementName the CLDR path element name.
     * @param attributeName the CLDR attribute name in the specified element.
     * @return a key to uniquely identify the specified attribute.
     */
    public static AttributeKey keyOf(String elementName, String attributeName) {
        // No namespace for the element means that:
        // 1) we don't expect the attribute name to have a namespace either,
        // 2) the attribute key should be in our cache of known instances.
        if (elementName.indexOf(':') == -1) {
            checkArgument(attributeName.indexOf(':') == -1,
                "attributes in an external namespace cannot be present in elements in the default"
                    + " namespace: %s:%s",
                elementName, attributeName);
            return checkNotNull(KNOWN_KEYS.get(elementName, attributeName),
                "unknown attribute (was it deprecated?): %s:%s",
                elementName, attributeName);
        }
        // An element in an external namespace _can_ have an attribute in the default namespace!
        // (e.g. <icu:dictionary type="Thai" icu:dependency="thaidict.dict"/>)
        return new AttributeKey(elementName, attributeName);
    }

    private final String elementName;
    private final String attributeName;

    private AttributeKey(String elementName, String attributeName) {
        this.elementName = checkValidLabel(elementName, "element name");
        this.attributeName = checkValidLabel(attributeName, "attribute name");
    }

    /** @return the non-empty element name of this key. */
    public String getElementName() {
        return elementName;
    }

    /** @return the non-empty attribute name of this key. */
    public String getAttributeName() {
        return attributeName;
    }

    /**
     * Accessor for required attribute values on a {@link CldrPath} or {@link CldrValue}. Use this
     * method in preference to the instance's own {@code get()} method in cases where the value is
     * required or takes an implicit value.
     *
     * @param src the {@link CldrPath} or {@link CldrValue} from which the value is to be obtained.
     * @return the attribute value or, if not present, the specified default.
     * @throws IllegalStateException if this attribute is optional for the given supplier.
     */
    public String valueFrom(AttributeSupplier src) {
        checkState(!src.getDataType().isOptionalAttribute(this),
            "attribute %s is optional in %s, it should be accessed by an optional accessor",
            this, src.getDataType());
        // If this fails, it's a sign of an issue in the DTD and/or parser.
        return checkNotNull(src.get(this), "missing required attribute: %s", this);
    }

    /**
     * Accessor for optional attribute values on a {@link CldrPath} or {@link CldrValue}. Use this
     * method in preference to the instance's own {@code get()} method, unless efficiency is vital.
     *
     * @param src the {@link CldrPath} or {@link CldrValue} from which the value is to be obtained.
     * @return the attribute value or, if not present, the specified default.
     * @throws IllegalStateException if this attribute is not optional for the given supplier.
     */
    public Optional<String> optionalValueFrom(AttributeSupplier src) {
        checkState(src.getDataType().isOptionalAttribute(this),
            "attribute %s is not optional in %s, it should not be accessed by an optional accessor",
            this, src.getDataType());
        return Optional.ofNullable(src.get(this));
    }

    /**
     * Accessor for attribute values on a {@link CldrPath} or {@link CldrValue}. Use this method
     * in preference to the instance's own {@code get()} method in cases where a non-null value is
     * required.
     *
     * @param src the {@link CldrPath} or {@link CldrValue} from which the value is to be obtained.
     * @param defaultValue a non-null default returned if the value is not present.
     * @return the attribute value or, if not present, the specified default.
     * @throws IllegalStateException if this attribute is not optional for the given supplier.
     */
    public String valueFrom(AttributeSupplier src, String defaultValue) {
        checkState(src.getDataType().isOptionalAttribute(this),
            "attribute %s is not optional in %s, it should not be accessed by an optional accessor",
            this, src.getDataType());
        checkNotNull(defaultValue, "default value must not be null");
        String v = src.get(this);
        return v != null ? v : defaultValue;
    }

    /**
     * Accessor for attribute values on a {@link CldrPath} or {@link CldrValue}. Use this method
     * in preference to the instance's own {@code get()} method when an attribute is expected to
     * only contain a legitimate boolean value.
     *
     * @param src the {@link CldrPath} or {@link CldrValue} from which the value is to be obtained.
     * @param defaultValue a default returned if the value is not present.
     * @return the attribute value or, if not present, the specified default.
     */
    // TODO: Enforce that this is only called for #ENUMERATION attributes with boolean values.
    public boolean booleanValueFrom(AttributeSupplier src, boolean defaultValue) {
        String v = src.get(this);
        if (v == null) {
            return defaultValue;
        } else if (Ascii.equalsIgnoreCase(v, "true")) {
            return true;
        } else if (Ascii.equalsIgnoreCase(v, "false")) {
            return false;
        }
        throw new IllegalArgumentException("value of attribute " + this + " is not boolean: " + v);
    }

    /**
     * Accessor for attribute values on a {@link CldrPath} or {@link CldrValue}. Use this method
     * in preference to the instance's own {@code get()} method when an attribute is expected to
     * contain a whitespace separated list of values.
     *
     * @param src the {@link CldrPath} or {@link CldrValue} from which values are to be obtained.
     * @return a list of split attribute values, possible empty if the attribute does not exist.
     */
    public List<String> listOfValuesFrom(AttributeSupplier src) {
        String v = src.get(this);
        return v != null ? LIST_SPLITTER.splitToList(v) : ImmutableList.of();
    }

    /**
     * Accessor for attribute values on a {@link CldrPath} or {@link CldrValue} which map to a known
     * enum. Use this method in preference to the instance's own {@code get()} method in cases where
     * a non-null value is required.
     *
     * @param src the {@link CldrPath} or {@link CldrValue} from which the value is to be obtained.
     * @param enumType the enum class type of the result.
     * @return an enum value instance from the underlying attribute value by name.
     */
    // TODO: Handle optional enumerations (e.g. PluralRange#start/end).
    public <T extends Enum<T>> T valueFrom(AttributeSupplier src, Class<T> enumType) {
        return Enum.valueOf(enumType, valueFrom(src));
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof AttributeKey)) {
            return false;
        }
        AttributeKey other = (AttributeKey) obj;
        return this.elementName.equals(other.elementName)
            && this.attributeName.equals(other.attributeName);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(elementName, attributeName);
    }

    /** Returns a debug-only representation of the qualified attribute key. */
    @Override
    public String toString() {
        return elementName + ":" + attributeName;
    }

    // Note: This can be modified if necessary but care must be taken to never allow various
    // meta-characters in element or attribute names (see CldrPath for the full list).
    private static String checkValidLabel(String value, String description) {
        checkArgument(!value.isEmpty(), "%s cannot be empty", description);
        checkArgument(CharMatcher.ascii().matchesAllOf(value),
            "non-ascii character in %s: %s", description, value);
        return value;
    }
}
