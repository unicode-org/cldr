package org.unicode.cldr.draft.keyboard;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Object representing the result of one transform from the tree. A transform has two components:
 * <ul>
 * <li>the sequence of characters that must be input in order to activate this transform
 * <li>the output of the transformation
 * </ul>
 *
 * <p>
 * For example, the character sequence for a particular transform could be '^e' and its resulting
 * output 'ê'.
 */
public final class Transform implements Comparable<Transform> {
    private final String sequence;
    private final String output;

    private Transform(String sequence, String output) {
        this.sequence = checkNotNull(sequence);
        this.output = checkNotNull(output);
    }

    /** Creates a transform from the given source sequence and resulting output. */
    public static Transform of(String sequence, String output) {
        return new Transform(sequence, output);
    }

    /** Returns the sequence of characters that must be typed in order to activate this transform. */
    public String sequence() {
        return sequence;
    }

    /** Returns the result of the transform. */
    public String output() {
        return output;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("sequence", sequence)
            .add("output", output)
            .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof Transform) {
            Transform other = (Transform) o;
            return sequence.equals(other.sequence) && output.equals(other.output);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(sequence, output);
    }

    @Override
    public int compareTo(Transform o) {
        return sequence.compareTo(o.sequence);
    }
}
