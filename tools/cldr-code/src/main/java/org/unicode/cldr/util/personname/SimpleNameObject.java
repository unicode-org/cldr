package org.unicode.cldr.util.personname;

import java.util.EnumSet;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.util.personname.PersonNameFormatter.Field;
import org.unicode.cldr.util.personname.PersonNameFormatter.ModifiedField;
import org.unicode.cldr.util.personname.PersonNameFormatter.Modifier;
import org.unicode.cldr.util.personname.PersonNameFormatter.NameObject;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.util.ULocale;

/**
     * Simple implementation for testing and using in CLDR examples.
     * Immutable
     */
    public class SimpleNameObject implements NameObject {
        private final ULocale nameLocale;
        private final ImmutableMap<ModifiedField, String> patternData;

        @Override
        public Set<Field> getAvailableFields() {
            Set<Field> result = EnumSet.noneOf(Field.class);
            for (Entry<ModifiedField, String> entry : patternData.entrySet()) {
                result.add(entry.getKey().getField());
            }
            return ImmutableSet.copyOf(result);
        }

        @Override
        public String getBestValue(ModifiedField modifiedField, Set<Modifier> outputUnhandledModifiers) {
            // TODO flesh out the SimpleNameObject to handle modifiers
            // and return the unhandled ones
            // For now just return the item.
            return patternData.get(modifiedField);
        }

        @Override
        public ULocale getNameLocale() {
            return nameLocale;
        }

        /**
         * TODO Replace by builder
         */
        public SimpleNameObject(ULocale nameLocale, ImmutableMap<ModifiedField, String> patternData) {
            this.nameLocale = nameLocale == null ? ULocale.ROOT : nameLocale;
            this.patternData = patternData;
        }

        //TODO: add methods to
        // (a) maximize the pattern data (have every possible set of modifiers for each field, so order is irrelevant).
        //      Brute force is just try all combinations. That is probably good enough for CLDR.
        // (b) minimize the pattern data (produce a compact version that minimizes the number of strings (not necessarily the absolute minimum)
        //      Thoughts: gather together all of the keys that have the same value, and coalesce. Exact method TBD

        @Override
        public String toString() {
            return "{locale=" + nameLocale + " " + "patternData=" + show(patternData) + "}";
        }

        private String show(ImmutableMap<ModifiedField, String> patternData2) {
            return patternData2.toString();
        }
    }