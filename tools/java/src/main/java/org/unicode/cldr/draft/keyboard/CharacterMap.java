package org.unicode.cldr.draft.keyboard;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * Object representing a 1 to 1 mapping between an ISO Position ({@link IsoLayoutPosition}) and the
 * provided output that is received when pressing that particular key. This object also includes any
 * keys that are available by "long-pressing" on the key (prominent on mobile phones).
 */
public final class CharacterMap implements Comparable<CharacterMap> {
    private final IsoLayoutPosition position;
    private final String output;
    private final ImmutableList<String> longPressKeys;
    private final boolean transformNo;

    private CharacterMap(IsoLayoutPosition position, String output,
        ImmutableList<String> longPressKeys, boolean transformNo) {
        this.position = Preconditions.checkNotNull(position);
        this.output = Preconditions.checkNotNull(output);
        this.longPressKeys = Preconditions.checkNotNull(longPressKeys);
        this.transformNo = transformNo;
    }

    /** Creates a new character map from the given position and output. */
    public static CharacterMap of(IsoLayoutPosition position, String output) {
        return new CharacterMap(position, output, ImmutableList.<String> of(), false);
    }

    /** Creates a new character map from the given position, output and long press keys. */
    public static CharacterMap of(IsoLayoutPosition position, String output,
        ImmutableList<String> longPressKeys) {
        return new CharacterMap(position, output, longPressKeys, false);
    }

    public IsoLayoutPosition position() {
        return position;
    }

    public String output() {
        return output;
    }

    public ImmutableList<String> longPressKeys() {
        return longPressKeys;
    }

    public CharacterMap markAsTransformNo() {
        return new CharacterMap(position, output, longPressKeys, true);
    }

    public boolean isTransformNo() {
        return transformNo;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("position", position)
            .add("output", output)
            .add("longPressKeys", longPressKeys)
            .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof CharacterMap) {
            CharacterMap other = (CharacterMap) o;
            return position.equals(other.position) && output.equals(other.output)
                && longPressKeys.equals(other.longPressKeys);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(position, output, longPressKeys);
    }

    /** Sort character maps based on their ISO layout positions. */
    @Override
    public int compareTo(CharacterMap o) {
        return position.compareTo(o.position);
    }

    static Function<CharacterMap, IsoLayoutPosition> isoLayoutPositionFunction() {
        return CharacterMapToIsoLayoutFunction.INSTANCE;
    }

    private enum CharacterMapToIsoLayoutFunction
        implements Function<CharacterMap, IsoLayoutPosition> {
        INSTANCE;

        @Override
        public IsoLayoutPosition apply(CharacterMap character) {
            return character.position;
        }
    }
}
