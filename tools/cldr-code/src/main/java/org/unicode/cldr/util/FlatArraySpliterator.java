package org.unicode.cldr.util;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * Optimized Spliterator for fixed-depth nested maps. Returns a Stream of Object[] where: [0...N-1]
 * are Keys [N] is the Value. Doesn't parallelize.
 */
public class FlatArraySpliterator implements Spliterator<Object[]> {
    private final int keyCount;
    private final Iterator<Entry<?, ?>>[] stack;
    private final Object[] buffer; // Keys + 1 slot for Value
    private int remainingKeys = 0;

    @SuppressWarnings("unchecked")
    public FlatArraySpliterator(Map<?, ?> root, int keyCount) {
        this.keyCount = keyCount;
        this.stack = (Iterator<Entry<?, ?>>[]) new Iterator[keyCount];
        this.buffer = new Object[keyCount + 1];
        this.stack[0] = (Iterator<Entry<?, ?>>) (Iterator<?>) root.entrySet().iterator();
        // very ugly cast but works
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean tryAdvance(Consumer<? super Object[]> action) {
        while (remainingKeys >= 0) {
            Iterator<Entry<?, ?>> it = stack[remainingKeys];

            if (!it.hasNext()) {
                remainingKeys--;
                continue;
            }

            Entry<?, ?> entry = it.next();
            buffer[remainingKeys] = entry.getKey(); // Place key in its specific slot

            if (remainingKeys < keyCount - 1) {
                // Navigate deeper: Cast is safe per user guarantee
                stack[++remainingKeys] =
                        (Iterator<Entry<?, ?>>)
                                (Iterator<?>) ((Map<?, ?>) entry.getValue()).entrySet().iterator();
                // very ugly cast but works
            } else {
                // Leaf reached: Place value in the final slot
                buffer[keyCount] = entry.getValue();

                // Clone creates a snapshot [K1, K2, ... KN, V]
                action.accept(buffer.clone());
                return true;
            }
        }
        return false;
    }

    @Override
    public Spliterator<Object[]> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return IMMUTABLE | NONNULL | DISTINCT;
    }
}
