package org.unicode.cldr.draft.keyboard;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Modifier keys used in the LDML Keyboard XML representation. A modifier key is pressed to change
 * the behavior of the keyboard. For example, pressing the Shift key on most Latin keyboards
 * produces upper-case variants of the characters.
 *
 * <p>
 * It is important NOT to change the ordering of the declared enum members because this enumeration
 * is used for sorting purposes.
 */
public enum ModifierKey {
    COMMAND(Variant.NONE, "cmd"), CONTROL(Variant.PARENT, "ctrl"), CONTROL_LEFT(Variant.LEFT, "ctrl"), CONTROL_RIGHT(Variant.RIGHT, "ctrl"), ALT(Variant.PARENT,
        "alt"), ALT_LEFT(Variant.LEFT, "alt"), ALT_RIGHT(Variant.RIGHT, "alt"), OPTION(Variant.PARENT, "opt"), OPTION_LEFT(Variant.LEFT, "opt"), OPTION_RIGHT(
            Variant.RIGHT,
            "opt"), CAPSLOCK(Variant.NONE, "caps"), SHIFT(Variant.PARENT, "shift"), SHIFT_LEFT(Variant.LEFT, "shift"), SHIFT_RIGHT(Variant.RIGHT, "shift");

    // Map of modifier key identifiers (obtained by calling toString()) to the modifier key itself.
    private static final ImmutableMap<String, ModifierKey> STRING_TO_MODIFIER_KEY = Maps.uniqueIndex(
        Lists.newArrayList(ModifierKey.values()), Functions.toStringFunction());
    private static final ImmutableSet<ModifierKey> PARENTS = ImmutableSet.of(CONTROL, ALT, OPTION,
        SHIFT);
    private static final ImmutableSet<ModifierKey> SINGLES = ImmutableSet.of(COMMAND, CAPSLOCK);

    private final Variant variant;
    private final String keyType;

    private ModifierKey(Variant variant, String keyType) {
        this.variant = checkNotNull(variant);
        this.keyType = checkNotNull(keyType);
    }

    /** Retrieves a modifier key from its string identifier. */
    public static ModifierKey fromString(String string) {
        ModifierKey key = STRING_TO_MODIFIER_KEY.get(checkNotNull(string));
        checkArgument(key != null, string);
        return key;
    }

    /** Returns all keys that are parent keys. */
    public static ImmutableSet<ModifierKey> parents() {
        return PARENTS;
    }

    /** Returns all keys that are neither parent keys or children. */
    public static ImmutableSet<ModifierKey> singles() {
        return SINGLES;
    }

    /**
     * Returns the matching sibling of this key. For example, if this key is ctrlR return ctrlL. If
     * the key has no siblings this method simply returns itself.
     */
    public ModifierKey sibling() {
        if (variant == Variant.PARENT) {
            return this;
        }
        return fromString(keyType + variant.opposite());
    }

    /**
     * Returns the parent of this key. For example, if this key is ctrlR return ctrl. If the key is
     * already a parent key this method simply returns itself.
     */
    public ModifierKey parent() {
        if (variant == Variant.PARENT) {
            return this;
        }
        return fromString(keyType);
    }

    /**
     * Returns the children of this key. For example if this key is ctrl, return both ctrlL and ctrlR.
     * If this is not a parent key, returns an empty list. The left key is always returned first.
     */
    public ImmutableList<ModifierKey> children() {
        if (variant != Variant.PARENT) {
            return ImmutableList.of();
        }
        return ImmutableList.of(fromString(keyType + Variant.LEFT), fromString(keyType + Variant.RIGHT));
    }

    @Override
    public String toString() {
        return keyType + variant.value;
    }

    /** The variant of the key. */
    private static enum Variant {
        PARENT(""), LEFT("L"), RIGHT("R"), NONE("");

        final String value;

        Variant(String value) {
            this.value = checkNotNull(value);
        }

        /**
         * Return the opposite variant. Only applicable to the left and right variants. Returns itself
         * otherwise.
         */
        Variant opposite() {
            return this == LEFT ? RIGHT : this == RIGHT ? LEFT : this;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
