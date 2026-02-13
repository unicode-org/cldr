package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Merges lists together, maintaining consistency with all the components. That is, [[date,
 * time],[time,zone]] would merge to [date, time, zone]. Exceptions will be thrown in 2 instances:
 *
 * <ul>
 *   <li>One of the input lists has duplicate elements, eg [time, date, time], making it impossible
 *       to add. However, that will just cause an add() to fail; catching and continuing make give
 *       results (but wouldn't account for the missing add().
 *   <li>There is a cycle, such as [[A, B, C] [C, D, A]]. In that case an exception is thrown. Some
 *       internal information is attached to the exception.
 * </ul>
 *
 * @param <T>
 */
public class MergeLists<T> {

    final Collection<Collection<T>> source = new ArrayList<>();
    final Set<T> orderedWorkingSet;
    final Set<T> onlyFirsts;

    public MergeLists() {
        this(new LinkedHashSet<T>(), new LinkedHashSet<T>());
    }

    /**
     * The sets determine the ordering of items that don't have a defined ordering.<br>
     * For example, [[A C] [B C]] could result in either of two orderings: [A B C] or [B C A].<br>
     * The preference between them can be handled by using two TreeSets (with or without a
     * comparator). If A < B naturally, [A B C] would be favored. If you want [B C A], use a
     * comparator that reverses the natural ordering.
     *
     * @param orderedWorkingSet
     * @param first
     */
    public MergeLists(Set<T> onlyFirsts, Set<T> orderedWorkingSet) {
        this.orderedWorkingSet = orderedWorkingSet;
        this.onlyFirsts = onlyFirsts;
    }

    public MergeLists<T> add(Collection<T> orderedItems) {
        if (orderedItems.size() == 0) { // skip empties
            return this;
        }
        final LinkedHashSet<T> linkedHashSet = new LinkedHashSet<>(orderedItems);
        if (linkedHashSet.size() != orderedItems.size()) {
            throw new IllegalArgumentException("Multiple items in ordering!");
        }
        source.add(linkedHashSet);
        return this;
    }

    @SuppressWarnings("unchecked")
    public MergeLists<T> add(T... stuff) {
        return add(Arrays.asList(stuff));
    }

    public <U extends Collection<T>> MergeLists<T> addAll(Collection<U> collectionsOfOrderedItems) {
        for (Collection<T> orderedItems : collectionsOfOrderedItems) {
            add(orderedItems);
        }
        return this;
    }

    public List<T> merge() {
        List<T> result = new ArrayList<>();

        for (Collection<T> sublist : source) {
            orderedWorkingSet.addAll(sublist);
        }

        // now that we have things ordered, we take the first one that is only at the front of a
        // list
        // this is slower, but puts things into as much of the order specified as possible
        // could be optimized further, but we don't care that much

        while (orderedWorkingSet.size() != 0) {
            getFirsts(onlyFirsts);
            if (onlyFirsts.size() == 0) {
                Map<T, Collection<T>> reasons = new LinkedHashMap<>();
                getFirsts(onlyFirsts, reasons);
                throw new MergeListException(
                        "Conflicting requested ordering",
                        result,
                        orderedWorkingSet,
                        reasons.values());
            }
            // now get first item that is in first
            T best = extractFirstOk(orderedWorkingSet, onlyFirsts); // removes from working set
            // remaining items now contains no non-first items
            removeFromSource(best);
            result.add(best);
        }
        return result;
    }

    @SuppressWarnings("rawtypes")
    public static class MergeListException extends IllegalArgumentException {
        // can't use generics!
        private static final long serialVersionUID = 1L;
        public final Collection partialResult;
        public final Collection orderedWorkingSet;
        public final Collection<Collection> problems;

        public MergeListException(
                String message,
                Collection partialResult,
                Set orderedWorkingSet,
                Collection problems) {
            super(message);
            this.partialResult = partialResult;
            this.orderedWorkingSet = orderedWorkingSet;
            this.problems = problems;
        }
    }

    public static <T> boolean hasConsistentOrder(Collection<T> a, Collection<T> b) {
        LinkedHashSet<T> remainder = new LinkedHashSet<>(a);
        remainder.retainAll(b);
        if (remainder.size() == 0) {
            return true;
        }
        // remainder is now in a's order, and contains only the items that are in both
        Iterator<T> bi = b.iterator();
        T current = bi.next();
        for (T item : remainder) {
            if (item.equals(current)) {
                if (!bi.hasNext()) {
                    return true;
                }
                current = bi.next();
            }
        }
        return !bi.hasNext(); // if we have any left over, we failed
    }

    public static <T, U extends Collection<T>> Collection<T> hasConsistentOrderWithEachOf(
            Collection<T> a, Collection<U> bs) {
        for (Collection<T> b : bs) {
            if (!hasConsistentOrder(a, b)) {
                return b;
            }
        }
        return null;
    }

    // could be optimized since we know the item will only occur at the head of a list
    private void removeFromSource(T item) {
        for (Iterator<Collection<T>> iterator = source.iterator(); iterator.hasNext(); ) {
            Collection<T> sublist = iterator.next();
            sublist.remove(item);
            if (sublist.size() == 0) {
                iterator.remove();
            }
        }
    }

    /** Get the first item that is also in the ok set. */
    private T extractFirstOk(Collection<T> remainingItems, Set<T> ok) {
        for (Iterator<T> it = remainingItems.iterator(); it.hasNext(); ) {
            T item = it.next();
            if (ok.contains(item)) {
                it.remove();
                return item;
            }
        }
        throw new IllegalArgumentException("Internal Error");
    }

    public void getFirsts(Set<T> result) {
        getFirsts(result, null);
    }

    /** Get first of each sets. Guaranteed non-empty */
    public void getFirsts(Set<T> result, Map<T, Collection<T>> reasons) {
        result.clear();
        result.addAll(orderedWorkingSet);
        for (Collection<T> sublist : source) {
            // get all the first items
            final Iterator<T> iterator = sublist.iterator();
            iterator.next(); // skip first
            while (iterator.hasNext()) {
                final T nextItem = iterator.next();
                boolean changed = result.remove(nextItem);
                if (changed && reasons != null) {
                    reasons.put(nextItem, sublist);
                }
            }
        }
    }
}
