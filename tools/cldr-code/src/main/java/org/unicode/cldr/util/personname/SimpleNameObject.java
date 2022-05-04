package org.unicode.cldr.util.personname;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.personname.PersonNameFormatter.Field;
import org.unicode.cldr.util.personname.PersonNameFormatter.ModifiedField;
import org.unicode.cldr.util.personname.PersonNameFormatter.Modifier;
import org.unicode.cldr.util.personname.PersonNameFormatter.NameObject;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.util.ULocale;

/**
 * Simple implementation for testing and using in CLDR examples.
 * Note: an invariant is that if there is a value for a modified field, there must be a value for the completely unmodified field.
 * Immutable
 */
public class SimpleNameObject implements NameObject {
    private final ULocale nameLocale;
    private final Map<Field, Map<Set<Modifier>, String>> patternData;
    private ImmutableMap<ModifiedField, String> modifiedFieldToValue;

    @Override
    public Set<Field> getAvailableFields() {
        return patternData.keySet();
    }

    @Override
    public ImmutableMap<ModifiedField, String> getModifiedFieldToValue() {
        return modifiedFieldToValue;
    }

    /**
     * Return the best fit.
     *  The data is organized by field, and then by modifier sets.
     *  The ordering is lexicographic among items with the same number of elements, so we favor earlier values in Modifier.values();
     * We are guaranteed by construction that the last one is empty
     */
    @Override
    public String getBestValue(ModifiedField modifiedField, Set<Modifier> remainingModifers) {
        final Set<Modifier> modifiers = modifiedField.getModifiers();
        remainingModifers.clear(); // just in case caller didn't
        remainingModifers.addAll(modifiers); // we may reduce below.

        // First check for match to field
        Map<Set<Modifier>, String> fieldData = patternData.get(modifiedField.getField());
        if (fieldData == null) {
            return null;
        }

        final Set<Entry<Set<Modifier>, String>> fieldDataEntries = fieldData.entrySet();

        // Catch the very common case, one choice
        // It will have no modifiers, so we add all to remaining

        if (fieldDataEntries.size() == 1) {
            return fieldDataEntries.iterator().next().getValue();
        }

        // Find the longest match for the field modifiers
        // If there are multiple, choose the lexicographically first, by construction
        // Note: we could shortcut the case where there are no modifiers, but probably not worth it since the lists will be short

        String bestValue = null;
        Set<Modifier> bestModifiers = ImmutableSet.of(); // empty

        String lastValue = null; // we return this if there is no match
        int largestIntersectionSize = -1;
        for (Entry<Set<Modifier>, String> entry : fieldDataEntries) {
            lastValue = entry.getValue();
            Set<Modifier> dataModifiers = entry.getKey();
            if (dataModifiers.size() <= largestIntersectionSize) {
                // we know that we can't get any longer than we have, so we can skip anything else
                break;
            }
            int intersectionSize = PersonNameFormatter.getIntersectionSize(dataModifiers, modifiers);
            if (intersectionSize != 0 && intersectionSize > largestIntersectionSize) {
                bestValue = lastValue;
                bestModifiers = dataModifiers;
                largestIntersectionSize = intersectionSize;
            }
        }

        // Remove any modifiers we used
        remainingModifers.removeAll(bestModifiers);
        // return the last value if there was no match
        return bestValue == null ? lastValue : bestValue;
    }

    @Override
    public ULocale getNameLocale() {
        return nameLocale;
    }

    public SimpleNameObject(ULocale nameLocale, Map<ModifiedField, String> patternData) {
        this.nameLocale = nameLocale == null ? ULocale.ROOT : nameLocale;
        this.modifiedFieldToValue = ImmutableMap.copyOf(patternData);

        Map<Field, Map<Set<Modifier>, String>> _patternData = new EnumMap<>(Field.class);
        for (Entry<ModifiedField, String> entry : patternData.entrySet()) {
            ModifiedField modifiedField = entry.getKey();
            final Field field = modifiedField.getField();
            final Set<Modifier> modifiers = modifiedField.getModifiers();
            final String value = entry.getValue();
            putChain(_patternData, field, modifiers, value);
        }

        // check data, and adjust as necessary

        Map<Field, Map<Set<Modifier>, String>> additions = null;
        for (Entry<Field, Map<Set<Modifier>, String>> entry : _patternData.entrySet()) {
            Map<Set<Modifier>, String> map = entry.getValue();
            if (map.get(Modifier.EMPTY) == null) {

                // ok to have no empty map if there exists a core
                // in that case, we manufacture a name
                String coreValue = map.get(ImmutableSet.of(Modifier.core));
                if (coreValue == null) {
                    throw new IllegalArgumentException("Every field must have a completely modified value " + entry);
                }
                String prefixValue = map.get(ImmutableSet.of(Modifier.prefix));
                if (additions == null) {
                    additions = new EnumMap<>(Field.class);
                }
                Field field = entry.getKey();
                putChain(additions, field, Modifier.EMPTY, prefixValue == null ? coreValue : prefixValue + " " + coreValue);
            }
        }
        if (additions != null) { // copy in additions
            for (Entry<Field, Map<Set<Modifier>, String>> entry : additions.entrySet()) {
                Field field = entry.getKey();
                for (Entry<Set<Modifier>, String> entry2 : entry.getValue().entrySet()) {
                    putChain(_patternData, field, entry2.getKey(), entry2.getValue());
                }
            }
        }
        this.patternData = CldrUtility.protectCollection(_patternData);
    }

    private void putChain(Map<Field, Map<Set<Modifier>, String>> _patternData, final Field field, final Set<Modifier> modifiers, final String value) {
        Map<Set<Modifier>, String> fieldData = _patternData.get(field);
        if (fieldData == null) {
            _patternData.put(field, fieldData = new TreeMap<>(Modifier.LONGEST_FIRST));
        }
        fieldData.put(modifiers, value);
    }

    /*
     * Takes string in form locale=fr, given=John Bob, given2=Edwin ...
     */
    public static SimpleNameObject from(String namePattern) {
        Map<ModifiedField, String> patternData = new LinkedHashMap<>();
        ULocale nameLocale = ULocale.ROOT;
        for (String setting : PersonNameFormatter.SPLIT_COMMA.split(namePattern)) {
            List<String> parts = PersonNameFormatter.SPLIT_EQUALS.splitToList(setting);
            if (parts.size() != 2) {
                throw new IllegalArgumentException("Bad format, should be like: given=John Bob, given2=Edwin, …: " + namePattern);
            }
            final String key = parts.get(0);
            final String value = parts.get(1);
            switch(key) {
            case "locale":
                nameLocale = new ULocale(value);
                break;
            default:
                patternData.put(ModifiedField.from(key), value);
                break;
            }
        }
        return new SimpleNameObject(nameLocale, patternData);
    }

    @Override
    public String toString() {
        return "{locale=" + nameLocale + " " + "patternData=" + show(patternData) + "}";
    }

    private String show(Map<Field, Map<Set<Modifier>, String>> patternData2) {
        // make a bit more concise
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Entry<Field, Map<Set<Modifier>, String>> entry : patternData2.entrySet()) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append('=');
            Map<Set<Modifier>, String> map = entry.getValue();
            if (map.size() == 1) {
                sb.append(map.values().iterator().next()); // if there is one value, we are guaranteed the key is empty
            } else {
                sb.append(map);
            }
        }
        return sb.append("}").toString();
    }

    public Map<Field, Map<Set<Modifier>, String>> getPatternData() {
        return patternData;
    }
}