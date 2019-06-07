package org.unicode.cldr.api;

import java.util.Map;

/**
 * Base class for any non-trivial implementation of the CldrData interface. The main benefit of
 * using this class is that it implements prefix visitation automatically using {@link
 * PrefixVisitorHost} to reconstruct path hierarchy visitation from a sequence of leaf paths and
 * values. It also enforces policy checks to avoid emitting potentially duplicate paths.
 */
abstract class AbstractDataSource implements CldrData {
    AbstractDataSource() {}

    // Implements prefix visitation by accepting values in nested grouping order to reconstruct
    // intermediate prefix paths.
    @Override
    public final void accept(PathOrder order, PrefixVisitor visitor) {
        PrefixVisitorHost.accept(this::accept, order, visitor);
    }

    void safeVisit(
        CldrPath cldrPath, String value, Map<AttributeKey, String> valueAttributes, ValueVisitor visitor) {

        if (shouldEmit(cldrPath)) {
            CldrValue cldrValue = CldrValue.create(value, valueAttributes, cldrPath);
            safeVisit(cldrValue, visitor);
        }
    }

    private boolean shouldEmit(CldrPath path) {
        // Hacky way to detect the version declaration in LDML files (which must always be
        // skipped since they are duplicate paths and reveal XML file boundaries). The path is
        // always "//ldmlXxxx/version" for some DTD type.
        if (path.getLength() == 2 && path.getName().equals("version")) {
            return false;
        }
        // Other tests can go here if we need to skip other paths/values.
        return true;
    }

    // Helper to wrap the visit method and report errors that occur.
    static void safeVisit(CldrValue v, ValueVisitor visitor) {
        try {
            visitor.visit(v);
        } catch (RuntimeException e) {
            // TODO: Throw wrapped exception but ensure it's not rewrapped by parent visitors!
            System.err.format("Exception thrown by value visitor for value: %s\n", v);
            System.err.println(e);
            e.printStackTrace(System.err);
        }
    }
}
