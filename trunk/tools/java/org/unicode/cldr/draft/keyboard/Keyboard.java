package org.unicode.cldr.draft.keyboard;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

/**
 * Object representing a keyboard layout. Includes identifier information, platform given names,
 * platform specific names, all the key maps for all modifier set combinations and the possible
 * transforms.
 */
public final class Keyboard {
    private final KeyboardId keyboardId;
    private final ImmutableList<String> names;
    private final ImmutableSortedSet<KeyMap> keyMaps;
    private final ImmutableSortedSet<Transform> transforms;
    private volatile KeyMap baseMap;

    private Keyboard(KeyboardId keyboardId, ImmutableList<String> names,
        ImmutableSortedSet<KeyMap> keyMaps, ImmutableSortedSet<Transform> transforms) {
        this.keyboardId = checkNotNull(keyboardId);
        this.names = checkNotNull(names);
        this.keyMaps = checkNotNull(keyMaps);
        this.transforms = checkNotNull(transforms);
    }

    /**
     * Creates a keyboard given an identifier, a list of platform given names, key maps and
     * transforms.
     */
    public static Keyboard of(KeyboardId keyboardId, ImmutableList<String> names,
        ImmutableSortedSet<KeyMap> keyMaps, ImmutableSortedSet<Transform> transforms) {
        return new Keyboard(keyboardId, names, keyMaps, transforms);
    }

    public KeyboardId keyboardId() {
        return keyboardId;
    }

    public ImmutableList<String> names() {
        return names;
    }

    public ImmutableSet<KeyMap> keyMaps() {
        return keyMaps;
    }

    public KeyMap baseMap() {
        return baseMap == null ? baseMap = getBaseMap() : baseMap;
    }

    private KeyMap getBaseMap() {
        for (KeyMap keyMap : keyMaps) {
            if (keyMap.modifierKeyCombinationSet().isBase()) {
                return keyMap;
            }
        }
        throw new IllegalStateException("Missing base map for " + keyboardId);
    }

    public ImmutableSet<Transform> transforms() {
        return transforms;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof Keyboard) {
            Keyboard other = (Keyboard) o;
            return keyboardId.equals(other.keyboardId) && names.equals(other.names)
                && keyMaps.equals(other.keyMaps) && transforms.equals(other.transforms);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(keyboardId, names, keyMaps, transforms);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("keyboardIds", keyboardId)
            .add("names", names)
            .add("keyMaps", keyMaps)
            .add("transforms", transforms)
            .toString();
    }
}
