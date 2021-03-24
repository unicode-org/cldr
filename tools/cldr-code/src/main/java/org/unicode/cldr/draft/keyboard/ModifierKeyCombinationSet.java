package org.unicode.cldr.draft.keyboard;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

/**
 * This class wraps a set of modifier key combinations. This class also includes the necessary
 * functions to simplify and output these combinations according to the LDML Keyboard Standard.
 *
 * <p>
 * A modifier key combination set is active if any single contained modifier key combination is
 * active. That is, there is a disjunctive relationship between the combinations.
 *
 * <p>
 * Combination1 OR Combination2 OR Combination3 ...
 */
public final class ModifierKeyCombinationSet implements Comparable<ModifierKeyCombinationSet> {
    private final ImmutableSortedSet<ModifierKeyCombination> combinations;

    private ModifierKeyCombinationSet(ImmutableSortedSet<ModifierKeyCombination> combinations) {
        this.combinations = checkNotNull(combinations);
    }

    /**
     * Creates a modifier key combination set from a set of combinations. Simplifies the set to its
     * simplest form.
     */
    public static ModifierKeyCombinationSet of(Set<ModifierKeyCombination> combinations) {
        ImmutableSet<ModifierKeyCombination> simplifiedSet = ModifierKeySimplifier
            .simplifySet(combinations);
        return new ModifierKeyCombinationSet(ImmutableSortedSet.copyOf(simplifiedSet));
    }

    /**
     * Merge multiple modifier key combinations into a single one. This method is useful when
     * consolidating identical key maps together.
     */
    public static ModifierKeyCombinationSet combine(Iterable<ModifierKeyCombinationSet> sets) {
        ImmutableSet.Builder<ModifierKeyCombination> builder = ImmutableSet.builder();
        for (ModifierKeyCombinationSet combinationSet : sets) {
            builder.addAll(combinationSet.combinations);
        }
        return ModifierKeyCombinationSet.of(builder.build());
    }

    public ImmutableSortedSet<ModifierKeyCombination> combinations() {
        return combinations;
    }

    /**
     * Determines if this modifier key combination set is a base set. That is, is it active when no
     * modifiers are pressed?
     */
    public boolean isBase() {
        for (ModifierKeyCombination combination : combinations) {
            if (combination.isBase()) {
                return true;
            }
        }
        return false;
    }

    private static final Joiner SPACE_JOINER = Joiner.on(" ");

    @Override
    public String toString() {
        return SPACE_JOINER.join(combinations);
    }

    @Override
    public int compareTo(ModifierKeyCombinationSet o) {
        Iterator<ModifierKeyCombination> iterator1 = combinations.iterator();
        Iterator<ModifierKeyCombination> iterator2 = o.combinations.iterator();
        // Compare combinations until a difference is found.
        while (iterator1.hasNext() && iterator2.hasNext()) {
            ModifierKeyCombination combination1 = iterator1.next();
            ModifierKeyCombination combination2 = iterator2.next();
            if (combination1.compareTo(combination2) < 0) {
                return -1;
            } else if (combination1.compareTo(combination2) > 0) {
                return 1;
            }
        }
        // Otherwise compare them based on size.
        return combinations.size() - o.combinations.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof ModifierKeyCombinationSet) {
            ModifierKeyCombinationSet other = (ModifierKeyCombinationSet) o;
            return combinations.equals(other.combinations);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(combinations);
    }
}
