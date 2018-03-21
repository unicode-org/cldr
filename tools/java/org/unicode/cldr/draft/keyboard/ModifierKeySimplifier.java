package org.unicode.cldr.draft.keyboard;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

/**
 * A helper class which helps simplify single key combinations. That is, keys which come in a parent
 * and child variants.
 *
 * The strategy used to build this simplification table was to minimize the number of terms in the
 * boolean algebra by simplifying most keys into a "don't care (?)" state as much as possible. For
 * example, to represent an empty combination of keys, we use "Parent = 0, Left Child = ?, Right
 * Child = ?" as opposed to "Parent = 0, Left Child = 0, Right Child = 0". (See the table above for
 * more details). Both forms are functionally equivalent but we feel that the first form is much
 * simpler to represent.
 */
public final class ModifierKeySimplifier {
    /**
     * A mapping from input (given by a ON set and a DON'T CARE set) to the internal representation of
     * the combination. The order is always {@code <PARENT><LEFT_CHILD><RIGHT_CHILD>}.
     * <p>
     * Notation:
     * <ul>
     * <li>"-" = Missing (not in any set)
     * <li>"1" = In the ON set
     * <li>"?" = In the DON'T CARE set
     * <li>"0" = In the OFF set
     * <ul>
     */
    private static final ImmutableMap<String, String> INPUT_COMBINATION_TO_INTERNAL = ImmutableMap
        .<String, String> builder().put("---", "0??").put("--1", "?01").put("--?", "?0?")
        .put("-1-", "?10").put("-11", "?11").put("-1?", "?1?").put("-?-", "??0").put("-?1", "??1")
        .put("-??", "???").put("1--", "1??").put("1-1", "??1").put("1-?", "1??").put("11-", "?1?")
        .put("111", "?11").put("11?", "?1?").put("1?-", "1??").put("1?1", "??1").put("1??", "1??")
        .put("?--", "???").put("?-1", "??1").put("?-?", "?0?").put("?1-", "?1?").put("?11", "?11")
        .put("?1?", "?1?").put("??-", "??0").put("??1", "??1").put("???", "???").build();

    /**
     * A mapping which maps the result of an OR between two combinations. Takes two combinations
     * (represented in the internal notation) and returns the simplified combination (also in the
     * internal notation).
     *
     * <p>
     * For example, "A? AL" simplifies to "A?", "AL AL+AR" simplifies to "AL+AR?" and so on. The
     * equivalence table is included in the document linked in the class header.
     *
     * <p>
     * Notation:
     * <ul>
     * <li>"%" = No simplification possible, both combinations must stay.
     * <li>"1" = In the ON set
     * <li>"?" = In the DON'T CARE set
     * <li>"0" = In the OFF set
     * <ul>
     */
    private static final ImmutableTable<String, String, String> COMBINATIONS_TO_SIMPLIFCATION = ImmutableTable
        .<String, String, String> builder().put("1??", "0??", "???").put("?10", "0??", "??0")
        .put("?1?", "0??", "%").put("??0", "0??", "??0").put("?11", "0??", "%")
        .put("?01", "0??", "?0?").put("??1", "0??", "%").put("?0?", "0??", "?0?")
        .put("???", "0??", "???").put("?10", "1??", "1??").put("?1?", "1??", "1??")
        .put("??0", "1??", "???").put("?11", "1??", "1??").put("?01", "1??", "1??")
        .put("??1", "1??", "1??").put("?0?", "1??", "???").put("???", "1??", "???")
        .put("?1?", "?10", "?1?").put("??0", "?10", "??0").put("?11", "?10", "?1?")
        .put("?01", "?10", "%").put("??1", "?10", "%").put("?0?", "?10", "%")
        .put("???", "?10", "???").put("??0", "?1?", "%").put("?11", "?1?", "?1?")
        .put("?01", "?1?", "%").put("??1", "?1?", "1??").put("?0?", "?1?", "???")
        .put("???", "?1?", "???").put("?11", "??0", "%").put("?01", "??0", "%")
        .put("??1", "??0", "???").put("?0?", "??0", "%").put("???", "??0", "???")
        .put("?01", "?11", "??1").put("??1", "?11", "??1").put("?0?", "?11", "%")
        .put("???", "?11", "???").put("??1", "?01", "??1").put("?0?", "?01", "?0?")
        .put("???", "?01", "???").put("?0?", "??1", "%").put("???", "??1", "???")
        .put("???", "?0?", "???").build();

    /**
     * Given a set of ON keys and DON'T CARE keys, simplify and determine the internal representation
     * of the combination.
     */
    public static ModifierKeyCombination simplifyInput(Set<ModifierKey> onKeys, Set<ModifierKey> dontCareKeys) {
        checkArgument(Sets.intersection(onKeys, dontCareKeys).size() == 0);
        ImmutableSet.Builder<ModifierKey> onKeysBuilder = ImmutableSet.builder();
        ImmutableSet.Builder<ModifierKey> offKeysBuilder = ImmutableSet.builder();
        // Add parent keys and their children.
        for (ModifierKey parentKey : ModifierKey.parents()) {
            StringBuilder inputRepresentation = new StringBuilder();
            // Parent key.
            inputRepresentation.append(getInputKeyState(parentKey, onKeys, dontCareKeys));
            // Children.
            for (ModifierKey child : parentKey.children()) {
                inputRepresentation.append(getInputKeyState(child, onKeys, dontCareKeys));
            }
            // Get the internal representation
            String result = INPUT_COMBINATION_TO_INTERNAL.get(inputRepresentation.toString());
            checkNotNull(result, "No internal mapping for %s", inputRepresentation);
            // Transform the String representation into the internal representation and add them to the ON
            // and OFF sets.
            addInternalRepresentationFromString(parentKey, result, onKeysBuilder, offKeysBuilder);
        }
        // Add single keys.
        for (ModifierKey singleKey : ModifierKey.singles()) {
            if (onKeys.contains(singleKey)) {
                onKeysBuilder.add(singleKey);
            } else if (!dontCareKeys.contains(singleKey)) {
                offKeysBuilder.add(singleKey);
            }
        }
        return ModifierKeyCombination.of(onKeysBuilder.build(), offKeysBuilder.build());
    }

    /** Find the state of the given modifier key by evaluating the given sets. */
    private static char getInputKeyState(ModifierKey modifierKey, Set<ModifierKey> onKeys,
        Set<ModifierKey> dontCareKeys) {
        return onKeys.contains(modifierKey) ? '1' : dontCareKeys.contains(modifierKey) ? '?' : '-';
    }

    private static Joiner PLUS_JOINER = Joiner.on('+');

    /**
     * Given a set of ON keys and OFF keys in the internal representation, simplify the combination
     * and produce a string representing the combination in the format defined by the LDML Keyboard
     * Standard.
     * <p>
     * Namely:
     * <ul>
     * <li>All keys are separated by a '+'.
     * <li>All don't care keys are suffixed by a '?'.
     * <li>ON keys are grouped together and are displayed first, followed by the don't care keys.
     * <li>The modifier keys should be in the order defined in the standard within a group.
     * <li>The combination should be in its simplest form.
     * </ul>
     */
    public static String simplifyToString(ModifierKeyCombination combination) {
        ImmutableSet<ModifierKey> onKeys = combination.onKeys();
        ImmutableSet<ModifierKey> offKeys = combination.offKeys();
        TreeSet<ModifierKey> onKeysForOutput = Sets.newTreeSet();
        TreeSet<ModifierKey> dontCareKeysForOutput = Sets.newTreeSet();
        for (ModifierKey parentKey : ModifierKey.parents()) {
            String result = getStringFromInternalRepresentation(parentKey, onKeys, offKeys);
            char parentState = result.charAt(0);
            char leftChildState = result.charAt(1);
            char rightChildState = result.charAt(2);
            // If both children are don't cares, output the parent only in its state (don't output the OFF
            // ones).
            if (leftChildState == '?' && rightChildState == '?') {
                if (parentState == '1') {
                    onKeysForOutput.add(parentKey);
                } else if (parentState == '?') {
                    dontCareKeysForOutput.add(parentKey);
                }
            }
            // Otherwise, add the child keys in their states (don't output the OFF ones).
            else {
                ImmutableList<ModifierKey> children = parentKey.children();
                if (leftChildState == '1') {
                    onKeysForOutput.add(children.get(0));
                } else if (leftChildState == '?') {
                    dontCareKeysForOutput.add(children.get(0));
                }
                if (rightChildState == '1') {
                    onKeysForOutput.add(children.get(1));
                } else if (rightChildState == '?') {
                    dontCareKeysForOutput.add(children.get(1));
                }
            }
        }
        // Add single keys
        for (ModifierKey singleKey : ModifierKey.singles()) {
            if (onKeys.contains(singleKey)) {
                onKeysForOutput.add(singleKey);
            } else if (!offKeys.contains(singleKey)) {
                dontCareKeysForOutput.add(singleKey);
            }
        }
        // Join on-keys.
        String onKeysString = PLUS_JOINER.join(onKeysForOutput);
        // Join don't care keys.
        List<String> dontCareKeysList = Lists.newArrayList();
        for (ModifierKey dontCareKey : dontCareKeysForOutput) {
            dontCareKeysList.add(dontCareKey.toString() + "?");
        }
        String dontCareKeysString = PLUS_JOINER.join(dontCareKeysList);
        return dontCareKeysString.isEmpty() ? onKeysString
            : onKeysString.isEmpty() ? dontCareKeysString
                : PLUS_JOINER.join(onKeysString, dontCareKeysString);
    }

    /** Find the state of the given modifier key by evaluating the given sets. */
    private static char getInternalKeyState(ModifierKey modifierKey, Set<ModifierKey> onKeys,
        Set<ModifierKey> offKeys) {
        return onKeys.contains(modifierKey) ? '1' : offKeys.contains(modifierKey) ? '0' : '?';
    }

    /**
     * Simplifies the set of combinations into its most simple forms and returns a modifier key
     * combination set.
     */
    public static ImmutableSet<ModifierKeyCombination> simplifySet(Set<ModifierKeyCombination> combinations) {
        // Make a defensive copy of the input.
        Set<ModifierKeyCombination> finalCombinations = Sets.newHashSet(combinations);
        // Keep simplifying the combination until a stable version is attained.
        int sizeChange = Integer.MAX_VALUE;
        while (sizeChange != 0) {
            int initialSize = finalCombinations.size();
            finalCombinations = simplifyCombinationsOnePass(finalCombinations);
            sizeChange = initialSize - finalCombinations.size();
        }
        return ImmutableSet.copyOf(finalCombinations);
    }

    /**
     * Make a single pass over the set of combinations to attempt to simplify them. Multiple calls to
     * this method are necessary to achieve the simplest form.
     */
    private static Set<ModifierKeyCombination> simplifyCombinationsOnePass(
        Set<ModifierKeyCombination> combinations) {
        if (combinations.size() < 2) {
            return combinations;
        }
        Iterator<ModifierKeyCombination> iterator = Sets.newTreeSet(combinations).iterator();
        Set<ModifierKeyCombination> finalCombinations = Sets.newHashSet();
        // Take two consecutive objects in the sorted set and attempt to simplify them.
        ModifierKeyCombination combination1 = iterator.next();
        while (iterator.hasNext()) {
            ModifierKeyCombination combination2 = iterator.next();
            Set<ModifierKeyCombination> result = simplifyTwoCombinations(combination1, combination2);
            // If the simplification was successful, use it as a new pointer.
            if (result.size() == 1) {
                combination1 = result.iterator().next();
            } else {
                finalCombinations.add(combination1);
                combination1 = combination2;
            }
        }
        finalCombinations.add(combination1);
        return finalCombinations;
    }

    /**
     * Given two modifier key combinations, attempt to simplify them into a single combination. If no
     * simplification is possible, the method simply returns a set containing the two original
     * combinations.
     */
    private static ImmutableSet<ModifierKeyCombination> simplifyTwoCombinations(
        ModifierKeyCombination combination1, ModifierKeyCombination combination2) {
        // If the combinations are identical, the simplification is trivial.
        if (combination1.equals(combination2)) {
            return ImmutableSet.of(combination1);
        }
        SetView<ModifierKey> onKeyDifferences = Sets.symmetricDifference(combination1.onKeys(),
            combination2.onKeys());
        SetView<ModifierKey> offKeyDifferences = Sets.symmetricDifference(combination1.offKeys(),
            combination2.offKeys());
        // Simplification is only possible if there is some sort of relationship between the keys (and
        // even then, simplification is not guaranteed.
        if (!keysAreRelated(onKeyDifferences, offKeyDifferences)) {
            return ImmutableSet.of(combination1, combination2);
        }
        // Get the modifier key parent in question.
        ModifierKey key = null;
        if (onKeyDifferences.size() > 0) {
            key = onKeyDifferences.iterator().next();
        } else {
            key = offKeyDifferences.iterator().next();
        }
        ModifierKey parentKey = key.parent();
        // Set up a new combination with all the common keys from the two combinations.
        Sets.SetView<ModifierKey> onKeysIntersect = Sets.intersection(combination1.onKeys(),
            combination2.onKeys());
        EnumSet<ModifierKey> onKeys = onKeysIntersect.isEmpty() ? EnumSet.noneOf(ModifierKey.class)
            : EnumSet.copyOf(onKeysIntersect);
        Sets.SetView<ModifierKey> offKeysIntersect = Sets.intersection(combination1.offKeys(), combination2.offKeys());
        EnumSet<ModifierKey> offKeys = offKeysIntersect.isEmpty() ? EnumSet.noneOf(ModifierKey.class)
            : EnumSet.copyOf(offKeysIntersect);
        // Get the internal state of both combinations for this particular modifier key
        String combination1States = getStringFromInternalRepresentation(parentKey,
            combination1.onKeys(), combination1.offKeys());
        String combination2States = getStringFromInternalRepresentation(parentKey,
            combination2.onKeys(), combination2.offKeys());
        // Attempt to get simplification (may need to reverse the col/row keys because we are just
        // storing a triangular matrix with the simplification codes).
        String result = COMBINATIONS_TO_SIMPLIFCATION.get(combination1States, combination2States);
        if (result == null) {
            result = COMBINATIONS_TO_SIMPLIFCATION.get(combination2States, combination1States);
        }
        checkNotNull(result, "Unknown combination %s", combination1States + "," + combination2States);
        // The "%" return code means that the two combinations cannot be combined.
        if (result.equals("%")) {
            return ImmutableSet.of(combination1, combination2);
        }
        // Transform the String representation into the internal representation and add them to the ON
        // and OFF sets.
        ImmutableSet.Builder<ModifierKey> onKeysBuilder = ImmutableSet.builder();
        ImmutableSet.Builder<ModifierKey> offKeysBuilder = ImmutableSet.builder();
        addInternalRepresentationFromString(parentKey, result, onKeysBuilder, offKeysBuilder);
        onKeysBuilder.addAll(onKeys);
        offKeysBuilder.addAll(offKeys);
        return ImmutableSet
            .of(ModifierKeyCombination.of(onKeysBuilder.build(), offKeysBuilder.build()));
    }

    /**
     * Given the set difference between two combinations ON keys and OFF keys, determine if the
     * differences in both sets are related (simplification is only possible if there is a
     * relationship between the different keys).
     */
    private static boolean keysAreRelated(Set<ModifierKey> onKeys, Set<ModifierKey> offKeys) {
        // Combine all keys.
        Set<ModifierKey> allKeys = EnumSet.noneOf(ModifierKey.class);
        allKeys.addAll(onKeys);
        allKeys.addAll(offKeys);
        // Get a test key.
        ModifierKey testKey = allKeys.iterator().next();
        // Remove all keys which have some sort of relationship to the test key from all keys.
        allKeys.remove(testKey);
        allKeys.remove(testKey.parent());
        allKeys.remove(testKey.sibling());
        allKeys.removeAll(testKey.children());
        // Check that set is empty, if it isn't there are some extra keys.
        return allKeys.size() == 0;
    }

    /**
     * Return a length 3 String representing the state of a parent key and its two children in their
     * internal representation given a set of ON keys and OFF keys (in internal representation).
     */
    private static String getStringFromInternalRepresentation(
        ModifierKey parentKey, Set<ModifierKey> onKeys, Set<ModifierKey> offKeys) {
        StringBuilder internalRepresentationBuilder = new StringBuilder();
        internalRepresentationBuilder.append(getInternalKeyState(parentKey, onKeys, offKeys));
        // Children
        ImmutableList<ModifierKey> children = parentKey.children();
        // If there are no children, mark them as ?? (effectively removing them from the boolean
        // equation).
        if (children.size() == 0) {
            internalRepresentationBuilder.append("??");
        } else {
            internalRepresentationBuilder.append(getInternalKeyState(children.get(0), onKeys, offKeys));
            internalRepresentationBuilder.append(getInternalKeyState(children.get(1), onKeys, offKeys));
        }
        return internalRepresentationBuilder.toString();
    }

    /**
     * Transform a length 3 String containing the state of a modifier key and its children and add it
     * to the onKeys and offKeys builders.
     */
    private static void addInternalRepresentationFromString(
        ModifierKey parentKey,
        String modifierKeyState,
        ImmutableSet.Builder<ModifierKey> onKeysOut,
        ImmutableSet.Builder<ModifierKey> offKeysOut) {
        checkArgument(modifierKeyState.length() == 3, modifierKeyState);
        ImmutableList<ModifierKey> children = parentKey.children();
        List<ModifierKey> keys = children.isEmpty()
            ? Lists.newArrayList(parentKey, parentKey, parentKey)
            : Lists.newArrayList(parentKey, children.get(0), children.get(1));
        for (int i = 0; i < modifierKeyState.length(); i++) {
            char state = modifierKeyState.charAt(i);
            ModifierKey key = keys.get(i);
            if (state == '1') {
                onKeysOut.add(key);
            } else if (state == '0') {
                offKeysOut.add(key);
            }
        }
    }

    private ModifierKeySimplifier() {
    }
}
