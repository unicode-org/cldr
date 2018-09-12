package org.unicode.cldr.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Freezable;

public class UnicodeRelation<T> implements Freezable<UnicodeRelation<T>> {

    final UnicodeMap<Set<T>> data = new UnicodeMap<>();
    final SetMaker<T> maker;

    public interface SetMaker<T> {
        Set<T> make();
    }

    public static SetMaker<Object> HASHSET_MAKER = new SetMaker<Object>() {
        @Override
        public Set<Object> make() {
            return new HashSet<Object>();
        }
    };

    public static final SetMaker<Object> LINKED_HASHSET_MAKER = new SetMaker<Object>() {
        public Set<Object> make() {
            return new LinkedHashSet<Object>();
        }
    };

    public UnicodeRelation(SetMaker<T> maker) {
        this.maker = maker;
    }

    public UnicodeRelation() {
        this.maker = (SetMaker<T>) HASHSET_MAKER;
    }

    public int size() {
        return data.size();
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public boolean containsKey(int key) {
        return data.containsKey(key);
    }

    public boolean containsKey(String key) {
        return data.containsKey(key);
    }

    public boolean containsValue(T value) {
        for (Set<T> v : data.values()) {
            if (v.contains(value)) {
                return true;
            }
        }
        return false;
    }

    public Set<T> get(int key) {
        return data.get(key);
    }

    public Set<T> get(String key) {
        return data.get((String) key);
    }

    public UnicodeSet getKeys(T value) {
        UnicodeSet result = new UnicodeSet();
        for (Entry<String, Set<T>> entry : data.entrySet()) {
            if (entry.getValue().contains(value)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public UnicodeRelation<T> add(String key, T value) {
        Set<T> newValues = addValue(data.get(key), value);
        if (newValues != null) {
            data.put(key, newValues);
        }
        return this;
    }

    public UnicodeRelation<T> add(int key, T value) {
        Set<T> newValues = addValue(data.get(key), value);
        if (newValues != null) {
            data.put(key, newValues);
        }
        return this;
    }

    public UnicodeRelation<T> addAll(String key, Collection<T> values) {
        Set<T> newValues = addValues(data.get(key), values);
        if (newValues != null) {
            data.put(key, newValues);
        }
        return this;
    }

    public UnicodeRelation<T> addAll(Map<String, T> m) {
        for (Entry<String, T> entry : m.entrySet()) {
            add(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public UnicodeRelation<T> addAll(UnicodeSet keys, Collection<T> values) {
        for (String key : keys) {
            addAll(key, values);
        }
        return this;
    }

    public UnicodeRelation<T> addAll(UnicodeSet keys, T... values) {
        return addAll(keys, Arrays.asList(values));
    }

    public UnicodeRelation<T> addAll(UnicodeSet keys, T value) {
        for (String key : keys) {
            add(key, value);
        }
        return this;
    }

    private Set<T> addValue(Set<T> oldValues, T value) {
        if (oldValues == null) {
            return Collections.singleton(value);
        } else if (oldValues.contains(value)) {
            return null;
        } else {
            Set<T> newValues = make(oldValues);
            newValues.add(value);
            return Collections.unmodifiableSet(newValues);
        }
    }

    private final Set<T> make(Collection<T> oldValues) {
        Set<T> newValues = maker.make();
        newValues.addAll(oldValues);
        return newValues;
    }

    private Set<T> addValues(Set<T> oldValues, Collection<T> values) {
        if (oldValues == null) {
            if (values.size() == 1) {
                return Collections.singleton(values.iterator().next());
            } else {
                return Collections.unmodifiableSet(make(values));
            }
        } else if (oldValues.containsAll(values)) {
            return null;
        } else {
            Set<T> newValues = make(oldValues);
            newValues.addAll(values);
            return Collections.unmodifiableSet(newValues);
        }
    }

    private Set<T> removeValues(Set<T> oldValues, Collection<T> values) {
        if (oldValues == null) {
            return null;
        } else if (Collections.disjoint(oldValues, values)) {
            return null;
        } else {
            Set<T> newValues = make(oldValues);
            newValues.removeAll(values);
            return newValues.size() == 0 ? Collections.EMPTY_SET : Collections.unmodifiableSet(newValues);
        }
    }

    public UnicodeRelation<T> remove(int key) {
        data.remove(key);
        return this;
    }

    public UnicodeRelation<T> remove(String key) {
        data.remove(key);
        return this;
    }

    public UnicodeRelation<T> removeValue(T value) {
        UnicodeSet toChange = getKeys(value);
        for (String key : toChange) {
            remove(key, value);
        }
        return this;
    }

    public UnicodeRelation<T> remove(int key, T value) {
        Set<T> values = data.getValue(key);
        if (values != null && values.contains(value)) {
            removeExisting(key, value, values);
        }
        return this;
    }

    public UnicodeRelation<T> remove(String key, T value) {
        Set<T> values = data.getValue(key);
        if (values != null && values.contains(value)) {
            removeExisting(key, value, values);
        }
        return this;
    }

    public UnicodeRelation<T> removeAll(String key, Collection<T> values) {
        Set<T> newValues = removeValues(data.get(key), values);
        if (newValues != null) {
            if (newValues == Collections.EMPTY_SET) {
                data.remove(key);
            } else {
                data.put(key, newValues);
            }
        }
        return this;
    }

    public UnicodeRelation<T> removeAll(Map<String, T> m) {
        for (Entry<String, T> entry : m.entrySet()) {
            remove(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public UnicodeRelation<T> removeAll(UnicodeSet keys, Collection<T> values) {
        for (String key : keys) {
            removeAll(key, values);
        }
        return this;
    }

    public UnicodeRelation<T> removeAll(UnicodeSet keys, T... values) {
        return removeAll(keys, Arrays.asList(values));
    }

    public UnicodeRelation<T> removeAll(UnicodeSet keys, T value) {
        for (String key : keys) {
            remove(key, value);
        }
        return this;
    }

    private void removeExisting(int key, T value, Set<T> values) {
        if (values.size() == 1) {
            data.remove(key);
        } else {
            Set<T> newValues = make(values);
            newValues.remove(value);
            data.put(key, Collections.unmodifiableSet(newValues));
        }
    }

    private void removeExisting(String key, T value, Set<T> values) {
        if (values.size() == 1) {
            data.remove(key);
        } else {
            Set<T> newValues = make(values);
            newValues.remove(value);
            data.put(key, Collections.unmodifiableSet(newValues));
        }
    }

    public void clear() {
        data.clear();
    }

    public UnicodeSet keySet() {
        return data.keySet();
    }

    public Collection<T> values() {
        Set<T> result = maker.make();
        for (Set<T> v : data.values()) {
            result.addAll(v);
        }
        return result;
    }

    public Iterable<Entry<String, Set<T>>> keyValues() {
        return data.entrySet();
    }

    @Override
    public String toString() {
        return data.toString();
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof UnicodeRelation && data.equals(((UnicodeRelation) obj).data);
    }

    @Override
    public boolean isFrozen() {
        return data.isFrozen();
    }

    @Override
    public UnicodeRelation<T> freeze() {
        data.freeze();
        return this;
    }

    @Override
    public UnicodeRelation<T> cloneAsThawed() {
        throw new UnsupportedOperationException();
    }
}
