package org.unicode.cldr.draft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.SetComparator;

/**
 * A class which represents a particular modifier combination (or combinations
 * of combinations).
 * <p>
 * For example {@code alt+cmd?} gets transformed into a native format consisting of sets of ON modifiers. In this case
 * it would get transformed into {@code altL+cmd, altR+cmd, altL+altR+cmd, altL, altR, altL+altR} .
 * <p>
 * This definition can be expanded across multiple combinations. For example {@code optR+caps? cmd+shift} gets
 * transformed into {@code optR+caps, optR,
 * cmd+shiftL, cmd+shiftR, cmd+shiftL+shiftR} .
 * 
 * <h1>Usage</h1>
 * <p>
 * There is a 1 to 1 relationship between a {@link KeyboardModifierSet} and a particular key map (a mapping from
 * physical keys to their output).
 * 
 * <pre>
 * {@code 
 * // Create the set from the XML modifier=".." attribute
 * ModifierSet modifierSet = ModifierSet.parseSet(<modifier=".." value from XML>); 
 * // Test if this set is active for a particular input combination provided by the keyboard
 * modifierSet.contains(<some combination to test>);
 * }
 * </pre>
 * 
 * @author rwainman@google.com (Raymond Wainman)
 */
public class KeyboardModifierSet {
    /**
     * Enum of all possible modifier keys.
     */
    public enum Modifier {
        cmd, ctrlL, ctrlR, caps, altL, altR, optL, optR, shiftL, shiftR;
    }

    static final SetComparator<Modifier> SINGLETON_COMPARATOR = new SetComparator<Modifier>();

    /** Initial input string */
    private final String input;
    /** Internal representation of all the possible combination variants */
    private final Set<Set<Modifier>> variants;

    /**
     * Private constructor. See factory {@link #parseSet} method.
     * 
     * @param variants
     *            A set containing all possible variants of the combination
     *            provided in the input string.
     */
    private KeyboardModifierSet(String input, Set<EnumSet<Modifier>> variants) {
        this.input = input;
        Set<Set<Modifier>> safe = new TreeSet<Set<Modifier>>(SINGLETON_COMPARATOR);
        for (EnumSet<Modifier> item : variants) {
            safe.add(Collections.unmodifiableSet(item));
        }
        this.variants = safe;
    }

    /**
     * Return all possible variants for this combination.
     * 
     * @return Set containing all possible variants.
     */
    public Set<Set<Modifier>> getVariants() {
        return variants;
    }

    /**
     * Determines if the given combination is valid within this set.
     * 
     * @param combination
     *            A combination of Modifier elements.
     * @return True if the combination is valid, false otherwise.
     */
    public boolean contains(EnumSet<Modifier> combination) {
        return variants.contains(combination);
    }

    public String getInput() {
        return input;
    }

    @Override
    public String toString() {
        return input + " => " + variants;
    }

    @Override
    public boolean equals(Object arg0) {
        return arg0 == null ? false : variants.equals(((KeyboardModifierSet) arg0).variants);
    }

    @Override
    public int hashCode() {
        return variants.hashCode();
    }

    /**
     * Parse a set containing one or more modifier sets. Each modifier set is
     * separated by a single space and modifiers within a modifier set are
     * separated by a '+'. For example {@code "ctrl+opt?+caps?+shift? alt+caps+cmd?"} has two modifier sets,
     * namely:
     * <ul>
     * <li>{@code "ctrl+opt?+caps?+shift?"}
     * <li>{@code "alt+caps+cmd?"}
     * </ul>
     * <p>
     * The '?' symbol appended to some modifiers indicates that this modifier is optional (it can be ON or OFF).
     * 
     * @param input
     *            String representing the sets of modifier sets. This string
     *            must match the format defined in the LDML Keyboard Standard.
     * @return A {@link KeyboardModifierSet} containing all possible variants of
     *         the specified combinations.
     * @throws IllegalArgumentException
     *             if the input string is incorrectly formatted.
     */
    public static KeyboardModifierSet parseSet(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input string cannot be null");
        }

        String modifierSetInputs[] = input.trim().split(" ");
        Set<EnumSet<Modifier>> variants = new HashSet<EnumSet<Modifier>>();
        for (String modifierSetInput : modifierSetInputs) {
            variants.addAll(parseSingleSet(modifierSetInput));
        }
        return new KeyboardModifierSet(input, variants);
    }

    /**
     * Parse a modifier set. The set typically looks something like {@code ctrl+opt?+caps?+shift?} or
     * {@code alt+caps+cmd?} and return a set
     * containing all possible variants for that particular modifier set.
     * <p>
     * For example {@code alt+caps+cmd?} gets expanded into {@code alt+caps+cmd?, alt+caps} .
     * 
     * @param input
     *            The input string representing the modifiers. This String must
     *            match the format defined in the LDML Keyboard Standard.
     * @return {@link KeyboardModifierSet}.
     * @throws IllegalArgumentException
     *             if the input string is incorrectly formatted.
     */
    private static Set<EnumSet<Modifier>> parseSingleSet(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input string cannot be null");
        }
        if (input.contains(" ")) {
            throw new IllegalArgumentException("Input string contains more than one combination");
        }

        String modifiers[] = input.trim().split("\\+");

        List<EnumSet<Modifier>> variants = new ArrayList<EnumSet<Modifier>>();
        variants.add(EnumSet.noneOf(Modifier.class)); // Add an initial set
                                                      // which is empty

        // Trivial case
        if (input.isEmpty()) {
            return new HashSet<EnumSet<Modifier>>(variants);
        }

        for (String modifier : modifiers) {
            String modifierElementString = modifier.replace("?", "");

            // Attempt to parse the modifier as a parent
            if (ModifierParent.isParentModifier(modifierElementString)) {
                ModifierParent parentModifier = ModifierParent.valueOf(modifierElementString);

                // Keep a collection of the new variants that need to be added
                // while iterating over the
                // existing ones
                Set<EnumSet<Modifier>> newVariants = new HashSet<EnumSet<Modifier>>();
                for (EnumSet<Modifier> variant : variants) {
                    // A parent key gets exploded into {Left, Right, Left+Right}
                    // or {Left, Right, Left+Right,
                    // (empty)} if it is a don't care

                    // {Left}
                    EnumSet<Modifier> leftVariant = EnumSet.copyOf(variant);
                    leftVariant.add(parentModifier.leftChild);
                    newVariants.add(leftVariant);

                    // {Right}
                    EnumSet<Modifier> rightVariant = EnumSet.copyOf(variant);
                    rightVariant.add(parentModifier.rightChild);
                    newVariants.add(rightVariant);

                    // {Left+Right}
                    // If it is a don't care, we need to leave the empty case
                    // {(empty)}
                    if (modifier.contains("?")) {
                        EnumSet<Modifier> bothChildrenVariant = EnumSet.copyOf(variant);
                        bothChildrenVariant.add(parentModifier.rightChild);
                        bothChildrenVariant.add(parentModifier.leftChild);
                        newVariants.add(bothChildrenVariant);
                    }
                    // No empty case, it is safe to add to the existing variants
                    else {
                        variant.add(parentModifier.rightChild);
                        variant.add(parentModifier.leftChild);
                    }
                }
                variants.addAll(newVariants);
            }
            // Otherwise, parse as a regular modifier
            else {
                Modifier modifierElement = Modifier.valueOf(modifierElementString);
                // On case, add the modifier to all existing variants
                if (!modifier.contains("?")) {
                    for (EnumSet<Modifier> variant : variants) {
                        variant.add(modifierElement);
                    }
                }
                // Don't care case, make a copy of the existing variants and add
                // the new key to it.
                else {
                    List<EnumSet<Modifier>> newVariants = new ArrayList<EnumSet<Modifier>>();
                    for (EnumSet<Modifier> variant : variants) {
                        EnumSet<Modifier> newVariant = EnumSet.copyOf(variant);
                        newVariant.add(modifierElement);
                        newVariants.add(newVariant);
                    }
                    variants.addAll(newVariants);
                }
            }
        }

        return new HashSet<EnumSet<Modifier>>(variants);
    }

    /**
     * Enum of all parent modifier keys. Defines the relationships with their
     * children.
     */
    private enum ModifierParent {
        ctrl(Modifier.ctrlL, Modifier.ctrlR), alt(Modifier.altL, Modifier.altR), opt(
            Modifier.optL, Modifier.optR), shift(Modifier.shiftL, Modifier.shiftR);

        private final Modifier leftChild;
        private final Modifier rightChild;

        private ModifierParent(Modifier leftChild, Modifier rightChild) {
            this.leftChild = leftChild;
            this.rightChild = rightChild;
        }

        /**
         * Determines if the String passed in is a valid parent key.
         * 
         * @param modifier
         *            The modifier string to verify.
         * @return True if it is a parent key, false otherwise.
         */
        private static boolean isParentModifier(String modifier) {
            try {
                ModifierParent.valueOf(modifier);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }

    public boolean containsSome(KeyboardModifierSet keyMapModifiers) {
        for (Set<Modifier> item : keyMapModifiers.variants) {
            if (variants.contains(item)) {
                return true;
            }
        }
        return false;
    }

    public String getShortInput() {
        int pos = input.indexOf(' ');
        if (pos < 0) return input;
        return input.substring(0, pos) + "…";
    }
}
