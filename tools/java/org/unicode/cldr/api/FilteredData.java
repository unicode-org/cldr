// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.cldr.api;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Filters underlying {@link CldrData} source data to transform values or remove values on the fly.
 * The order of transformed values produced by the class is the same order as the underlying data.
 *
 * <p>This class does not currently support injecting additional values due to the difficulty in
 * ensuring correct ordering semantics.
 *
 * <p>A subclass should implement the {@link FilteredData#filter(CldrValue)} method to achieve the
 * desired results. Note however that this API is currently unstable and is expected to change once
 * DTD path ordering is enforced everywhere.
 */
// TODO: Once DTD ordering is the only allowed order, this can be extended to allow adding paths.
public abstract class FilteredData implements CldrData {
    private final CldrData src;

    /** Constructs the parent class with a {@link CldrData} source. */
    protected FilteredData(CldrData src) {
        this.src = checkNotNull(src);
    }

    /** Returns the underlying source data to sub-class implementations. */
    protected CldrData getSourceData() {
        return src;
    }

    /**
     * Returns a filtered CLDR value, replacing or removing the original value during visitation.
     * The filtered value can only differ in its base value or value attributes, and must have
     * the same {@link CldrPath} associated with it.
     *
     * @return the filtered value to be replaced, or {@code null} to remove the value.
     */
    protected abstract CldrValue filter(CldrValue value);

    @Override
    public final void accept(PathOrder order, ValueVisitor visitor) {
        src.accept(order, v -> visitFiltered(v, visitor));
    }

    @Override
    public final CldrValue get(CldrPath path) {
        CldrValue value = src.get(path);
        return value != null ? checkFiltered(value) : null;
    }

    private void visitFiltered(CldrValue value, ValueVisitor visitor) {
        CldrValue filteredValue = checkFiltered(value);
        if (filteredValue != null) {
            visitor.visit(filteredValue);
        }
    }

    private CldrValue checkFiltered(CldrValue value) {
        CldrValue filteredValue = filter(value);
        checkArgument(filteredValue == null || filteredValue.getPath().equals(value.getPath()),
            "filtering is not permitted to modify distinguishing paths: source=%s, filtered=%s",
            value, filteredValue);
        return filteredValue;
    }
}
