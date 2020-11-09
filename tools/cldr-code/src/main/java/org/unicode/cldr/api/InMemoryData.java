package org.unicode.cldr.api;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/** An in-memory representation of CldrData based on a simple map. */
final class InMemoryData implements CldrData {
    // Allow arbitrary ordering of specified path/value pairs (possibly not even NESTED_GROUPING).
    private final ImmutableMap<CldrPath, CldrValue> pathValuePairs;

    InMemoryData(Iterable<CldrValue> values) {
        this.pathValuePairs = Maps.uniqueIndex(values, CldrValue::getPath);
        // Test that none of the paths in the map are prefixes of any other paths. This is a
        // requirement for distinguishing paths.
        Set<CldrPath> pathPrefixes = new HashSet<>();
        for (CldrPath p : pathValuePairs.keySet()) {
            checkArgument(pathPrefixes.add(p),
                "distinguishing paths must not be prefixes of other paths: %s", p);
            // Add the rest of the path prefixes for this path (until we hit existing values).
            for (p = p.getParent(); p != null && pathPrefixes.add(p); p = p.getParent()) {}
        }
    }

    @Override
    public void accept(PathOrder order, ValueVisitor visitor) {
        sortKeys(order).forEach(p -> visitor.visit(pathValuePairs.get(p)));
    }

    @Override
    public CldrValue get(CldrPath path) {
        return pathValuePairs.get(path);
    }

    private Stream<CldrPath> sortKeys(PathOrder order) {
        Stream<CldrPath> rawOrder = pathValuePairs.keySet().stream();
        switch (order) {
        case ARBITRARY:
            return rawOrder;
        case NESTED_GROUPING:
            // toString() will give nested grouping if no paths are prefixes of each other.
            // More importantly for testing, this is NOT DTD order.
            return rawOrder.sorted(comparing(Object::toString));
        case DTD:
            // Paths are naturally DTD order.
            return rawOrder.sorted(naturalOrder());
        default:
            throw new AssertionError("Unknown path order!!: " + order);
        }
    }
}
