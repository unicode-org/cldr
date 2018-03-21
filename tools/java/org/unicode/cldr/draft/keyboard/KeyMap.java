package org.unicode.cldr.draft.keyboard;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;

/**
 * Object that represents a full keyboard mapping for a given modifier key combination set.
 *
 * <p>
 * For example, the English-US keyboard with the Shift modifier activated outputs:
 * <ul>
 * <li>{@code ISO=E01 US-101 keyboard=[1] = '!'}
 * <li>{@code E02 [2] = '@'}
 * <li>{@code E03 [3] = '#'}
 * <li>{@code E04 [4] = '$'}
 * <li>{@code D01 [Q] = 'Q'}
 * <li>And so on...
 * </ul>
 */
public final class KeyMap implements Comparable<KeyMap> {
    private final ModifierKeyCombinationSet modifierKeyCombinationSet;
    private final ImmutableSortedMap<IsoLayoutPosition, CharacterMap> isoLayoutToCharacterMap;

    private KeyMap(ModifierKeyCombinationSet modifierKeyCombinationSet,
        ImmutableSortedMap<IsoLayoutPosition, CharacterMap> isoLayoutToCharacterMap) {
        this.modifierKeyCombinationSet = checkNotNull(modifierKeyCombinationSet);
        this.isoLayoutToCharacterMap = checkNotNull(isoLayoutToCharacterMap);
    }

    /** Creates a key map from the given modifier key combination set and characer maps. */
    public static KeyMap of(ModifierKeyCombinationSet modifierKeyCombinationSet,
        ImmutableSet<CharacterMap> characterMaps) {
        return new KeyMap(modifierKeyCombinationSet, ImmutableSortedMap.copyOf(Maps.uniqueIndex(
            characterMaps, CharacterMap.isoLayoutPositionFunction())));
    }

    public ModifierKeyCombinationSet modifierKeyCombinationSet() {
        return modifierKeyCombinationSet;
    }

    public ImmutableSortedMap<IsoLayoutPosition, CharacterMap> isoLayoutToCharacterMap() {
        return isoLayoutToCharacterMap;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("modifierKeyCombinationSet", modifierKeyCombinationSet)
            .add("isoLayoutToCharacterMap", isoLayoutToCharacterMap)
            .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof KeyMap) {
            KeyMap other = (KeyMap) o;
            return modifierKeyCombinationSet.equals(other.modifierKeyCombinationSet)
                && isoLayoutToCharacterMap.equals(other.isoLayoutToCharacterMap);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(modifierKeyCombinationSet, isoLayoutToCharacterMap);
    }

    @Override
    public int compareTo(KeyMap o) {
        // Order the key maps by their modifier sets.
        return modifierKeyCombinationSet.compareTo(o.modifierKeyCombinationSet);
    }
}
