package org.unicode.cldr.draft.keyboard;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.Set;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;

/**
 * Object containing the combination of modifier keys that must be on and off for a particular
 * combination to be activated. All other keys are considered "don't care keys". This simulates
 * boolean logic for a 3 state system.
 *
 * <p>
 * For example, suppose we have three keys, "A", "B" and "C". Then the boolean expression:
 * {@code A AND !B} means that A must be on, B must be off and C is a don't care.
 *
 * <p>
 * Some keys may have L and R variants (like Control which has a Left-Control and a Right-Control).
 * When the situation occurs that the parent key (the variant without a left or right suffix) is
 * included into the on keys or off keys sets, then the children are included into the don't care
 * pool and are omitted when the combination is printed out.
 *
 * <p>
 * For example, suppose we have three keys "A", "B" and "C" and "A" has a left and right variant
 * which are "A-Right" and "A-Left" respectively. Continuing the example above, the keys fall into
 * the following categories:
 * <ul>
 * <li>ON: { A }
 * <li>OFF: { B }
 * <li>DON'T CARE: { C, A-Left, A-Right }
 * </ul>
 * <p>
 * However when printing out the combination, we would exclude the A-Left and A-Right keys because
 * their parent is already included in the ON set. Therefore the output result would be:
 * {@code A+C?}.
 *
 * <p>
 * A slightly different behavior exists for parent modifier keys. When a parent modifier key is
 * given then its children are also added to the don't care keys pool. Once again, the
 * simplification is shown when printing out the combination.
 *
 * <p>
 * For example, continuing the above example but this time assuming that C has a left and right
 * variant. Therefore the breakdown looks like:
 * <ul>
 * <li>ON: { A }
 * <li>OFF: { B }
 * <li>DON'T CARE: { C, C-Left, C-Right, A-Left, A-Right }
 * </ul>
 */
public final class ModifierKeyCombination implements Comparable<ModifierKeyCombination> {
    public static final ModifierKeyCombination BASE = ModifierKeyCombination.ofOnKeys(
        ImmutableSet.<ModifierKey> of());

    private final ImmutableSet<ModifierKey> onKeys;
    private final ImmutableSet<ModifierKey> offKeys;

    private ModifierKeyCombination(ImmutableSet<ModifierKey> onKeys, ImmutableSet<ModifierKey> offKeys) {
        this.onKeys = checkNotNull(onKeys);
        this.offKeys = checkNotNull(offKeys);
    }

    /**
     * Create a modifier key combination from a set of ON keys. This is the most common factory
     * method since most sources will provide the keys that MUST be on. Simplifies the set as
     * needed.
     */
    public static ModifierKeyCombination ofOnKeys(Set<ModifierKey> onKeys) {
        return ofOnAndDontCareKeys(ImmutableSet.copyOf(onKeys), ImmutableSet.<ModifierKey> of());
    }

    /**
     * Create a modifier key combination from a set of ON keys and a set of DON'T CARE keys.
     * That is a set of keys that MUST be ON and a set of keys that CAN be ON or OFF. Simplifies
     * the sets as needed.
     */
    public static ModifierKeyCombination ofOnAndDontCareKeys(Set<ModifierKey> onKeys,
        Set<ModifierKey> dontCareKeys) {
        checkArgument(Sets.intersection(onKeys, dontCareKeys).size() == 0,
            "On keys and don't care keys must be disjoint");
        return ModifierKeySimplifier.simplifyInput(ImmutableSet.copyOf(onKeys),
            ImmutableSet.copyOf(dontCareKeys));
    }

    /** Internal. */
    static ModifierKeyCombination of(ImmutableSet<ModifierKey> onKeys,
        ImmutableSet<ModifierKey> offKeys) {
        return new ModifierKeyCombination(onKeys, offKeys);
    }

    /** Returns the set of keys that have to be ON for this combination to be active. */
    public ImmutableSet<ModifierKey> onKeys() {
        return onKeys;
    }

    /** Returns the set of keys that have to be OFF for this combination to be active. */
    public ImmutableSet<ModifierKey> offKeys() {
        return offKeys;
    }

    /**
     * Determines if this combination is a base combination. That is, is this combination valid when
     * no modifier keys are pressed?
     */
    public boolean isBase() {
        return onKeys.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof ModifierKeyCombination) {
            ModifierKeyCombination other = (ModifierKeyCombination) o;
            return onKeys.equals(other.onKeys) && offKeys.equals(other.offKeys);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(onKeys, offKeys);
    }

    @Override
    public String toString() {
        // TODO: cache this result.
        return ModifierKeySimplifier.simplifyToString(this);
    }

    @Override
    public int compareTo(ModifierKeyCombination o) {
        // Compare on keys first.
        ImmutableSortedSet<ModifierKey> sortedOnKeys1 = ImmutableSortedSet.copyOf(onKeys);
        ImmutableSortedSet<ModifierKey> sortedOnKeys2 = ImmutableSortedSet.copyOf(o.onKeys);
        int result = compareSetsDescending(sortedOnKeys1, sortedOnKeys2);
        // If they are identical, compare off keys (this will be the opposite from the result from the
        // on keys because what we really want is the order for the don't care keys which are simply the
        // converse of the off keys)
        // Here is a simple illustrative example:
        // -Suppose Alphabetic order within a combination
        // -Suppose reverse Alphabetic order between combinations
        // -Suppose four keys {A, B, C, D}
        // -Suppose two combinations, A.B.~D (A+B+C?) and A.B.~C (A+B+D?).
        // We want them ordered: A+B+D? A+B+C?
        // Clearly, AB are identical in both so we move onto the off keys: ~D and ~C respectively.
        // According to our initial comparison, ~D comes before ~C, but this is incorrect when looking
        // at the don't care keys which are C? and D? respectively. This is why we multiply the result
        // received from the off keys comparison by -1.
        //
        // More on the reverse ordering scheme between combinations:
        // Within a combination: A+B+C (A comes before B and B comes before C)
        // Between combinations: Suppose two combinations, A+C and B+C. Then the order would be B+C A+C.
        // (Reverse ordering so B comes before A).
        if (result == 0) {
            ImmutableSortedSet<ModifierKey> sortedOffKeys1 = ImmutableSortedSet.copyOf(offKeys);
            ImmutableSortedSet<ModifierKey> sortedOffKeys2 = ImmutableSortedSet.copyOf(o.offKeys);
            return -1 * compareSetsDescending(sortedOffKeys1, sortedOffKeys2);
        } else {
            return result;
        }
    }

    /**
     * Compare two sets of modifier key elements. Returns a negative integer if {@code set1} is
     * less than {@code set2}, a positive integer if {@code set1} is greater than {@code set2} and 0
     * if both sets are equal.
     *
     * <p>
     * Compares the sets based on the reverse ordering of the natural order imposed by the
     * modifier key enum. This is the convention used in the LDML Keyboard Standard.
     */
    private static int compareSetsDescending(
        ImmutableSortedSet<ModifierKey> set1, ImmutableSortedSet<ModifierKey> set2) {
        Iterator<ModifierKey> iterator1 = set1.iterator();
        Iterator<ModifierKey> iterator2 = set2.iterator();
        // Compare on keys until a difference is found.
        while (iterator1.hasNext() && iterator2.hasNext()) {
            ModifierKey modifierKey1 = iterator1.next();
            ModifierKey modifierKey2 = iterator2.next();
            if (modifierKey1.compareTo(modifierKey2) < 0) {
                return 1;
            } else if (modifierKey1.compareTo(modifierKey2) > 0) {
                return -1;
            }
        }
        // If the first x elements are identical, then the set with more modifier keys should come
        // after the set with less.
        return set1.size() - set2.size();
    }
}
