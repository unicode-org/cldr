package org.unicode.cldr.draft.keyboard;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.ibm.icu.text.Collator;

/** Builder class to assist in constructing a keyboard object. */
public final class KeyboardBuilder {
    private final ImmutableSet.Builder<KeyboardId> keyboardIds;
    private final ImmutableList.Builder<String> names;
    private final Map<String, String> transformSequenceToOutput;
    private final Table<ModifierKeyCombination, IsoLayoutPosition, CharacterMap> modifierAndPositionToCharacter;

    public KeyboardBuilder() {
        keyboardIds = ImmutableSet.builder();
        names = ImmutableList.builder();
        transformSequenceToOutput = Maps.newHashMap();
        modifierAndPositionToCharacter = HashBasedTable.create();
    }

    public KeyboardBuilder addKeyboardIds(Iterable<KeyboardId> keyboardIds) {
        this.keyboardIds.addAll(keyboardIds);
        return this;
    }

    public KeyboardBuilder addName(String name) {
        names.add(name);
        return this;
    }

    public KeyboardBuilder addTransform(String sequence, String output) {
        if (transformSequenceToOutput.containsKey(sequence)
            && !transformSequenceToOutput.get(sequence).equals(output)) {
            String errorMessage = String.format("Duplicate entry for [%s:%s]", sequence, output);
            throw new IllegalArgumentException(errorMessage);
        }
        transformSequenceToOutput.put(sequence, output);
        return this;
    }

    public KeyboardBuilder addCharacterMap(
        ModifierKeyCombination combination, CharacterMap characterMap) {
        checkNotNull(combination);
        if (modifierAndPositionToCharacter.contains(combination, characterMap.position())) {
            CharacterMap existing = modifierAndPositionToCharacter.get(combination, characterMap.position());
            checkArgument(
                existing.equals(characterMap),
                "Duplicate entry for [%s:%s:%s]",
                combination,
                characterMap,
                existing);
        }
        modifierAndPositionToCharacter.put(combination, characterMap.position(), characterMap);
        return this;
    }

    public KeyboardBuilder addCharacterMap(
        Collection<ModifierKeyCombination> combinations, CharacterMap characterMap) {
        for (ModifierKeyCombination combination : combinations) {
            addCharacterMap(combination, characterMap);
        }
        return this;
    }

    public ImmutableList<Keyboard> build() {
        ImmutableSet<KeyboardId> keyboardIds = this.keyboardIds.build();
        checkArgument(keyboardIds.size() > 0, "KeyboardIds must contain at least one element");
        // See if key map consolidation is possible.
        ListMultimap<ImmutableSet<CharacterMap>, ModifierKeyCombination> charactersToCombinations = ArrayListMultimap.create();
        for (ModifierKeyCombination combination : modifierAndPositionToCharacter.rowKeySet()) {
            Collection<CharacterMap> characterMaps = modifierAndPositionToCharacter.row(combination).values();
            charactersToCombinations.put(ImmutableSet.copyOf(characterMaps), combination);
        }
        // Build the key maps.
        KeyboardId id = keyboardIds.iterator().next();
        ImmutableSortedSet.Builder<KeyMap> keyMaps = ImmutableSortedSet.naturalOrder();
        for (ImmutableSet<CharacterMap> characterMaps : charactersToCombinations.keySet()) {
            List<ModifierKeyCombination> combinations = charactersToCombinations.get(characterMaps);
            ModifierKeyCombinationSet combinationSet = ModifierKeyCombinationSet.of(ImmutableSet.copyOf(combinations));
            keyMaps.add(KeyMap.of(combinationSet, characterMaps));
        }
        // Add the transforms.
        ImmutableSortedSet.Builder<Transform> transforms = ImmutableSortedSet.orderedBy(collatorComparator(Collator.getInstance(id.locale())));
        for (Entry<String, String> transformEntry : transformSequenceToOutput.entrySet()) {
            transforms.add(Transform.of(transformEntry.getKey(), transformEntry.getValue()));
        }
        ImmutableList.Builder<Keyboard> keyboards = ImmutableList.builder();
        for (KeyboardId keyboardId : keyboardIds) {
            keyboards.add(Keyboard.of(keyboardId, names.build(), keyMaps.build(), transforms.build()));
        }
        return keyboards.build();
    }

    public Set<String> transformSequences() {
        return transformSequenceToOutput.keySet();
    }

    private static Comparator<Transform> collatorComparator(final Collator collator) {
        return new Comparator<Transform>() {
            @Override
            public int compare(Transform o1, Transform o2) {
                return collator.compare(o1.sequence(), o2.sequence());
            }
        };
    }
}
