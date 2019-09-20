package org.unicode.cldr.api;

import java.util.function.Consumer;

/**
 * An immutable, reusable CLDR data instance on which visitors can be accepted to process paths
 * and values, or for which values can be looked up by their corresponding distinguishing path.
 */
public interface CldrData {
    /**
     * Accepts the given visitor over all path/value pairs of this CLDR data instance. Note
     * that value visitors only visit complete "leaf" paths which have associated values, and
     * never see partial prefix paths.
     *
     * <p>Since a value visitor never sees partial path prefixes, a value visitor can never
     * defer to a prefix visitor (since there's nothing "below" the paths that a value
     * visitor visits).
     *
     * @param order the order in which visitation should occur.
     * @param visitor the visitor to process CLDR data.
     */
    void accept(PathOrder order, ValueVisitor visitor);

    /**
     * Accepts the given visitor over all partial path prefixes of this CLDR data instance.
     * Note that, on its own, this visitor will never see CDLR values, or even complete paths.
     * It only sees the prefix paths under which values can exist.
     *
     * <p>Typically an instance of a {@link PrefixVisitor} would by used to identify a specific
     * sub-hierarchy of data based on the prefix path, and then defer to a {@link ValueVisitor
     * value visitor} or another {@link PrefixVisitor prefix visitor} to handle it.
     *
     * <p>Since {@link PrefixVisitor} requires that paths are visited in at least {@link
     * PathOrder#NESTED_GROUPING nested grouping} order, the actual order of visitation may be
     * more strict than the specified value.
     *
     * @param order the order in which visitation should occur.
     * @param visitor the visitor to process CLDR data.
     */
    default void accept(PathOrder order, PrefixVisitor visitor) {
        PrefixVisitorHost.accept(this::accept, order, visitor);
    }

    /**
     * Returns a {@link CldrValue} for a given distinguishing path.
     *
     * @param path the complete distinguishing path associated with a CLDR value.
     * @return the CldrValue for the given path, if it exists, or else {@code null}.
     */
    /* @Nullable */ CldrValue get(CldrPath path);

    /** Ordering options for path visitation. */
    // TODO (CLDR-13275): Remove PathOrder and stabilize tools to use only DTD order.
    enum PathOrder {
        /**
         * Visits {@code CldrPath}s in an arbitrary, potentially unstable, order. Only use this
         * ordering if your visitation code is completely robust against changes to visitation
         * ordering. This is expected to be the fastest ordering option, but may change over time.
         *
         * <p>Note that if this value is specified for a method which requires a stricter ordering
         * for correctness, then the stricter ordering will be used. Note also that the ordering of
         * visitation may change between visitations if a more strictly ordered visitation was
         * required in the meantime (since paths may be re-ordered and cached).
         */
        ARBITRARY,

        /**
         * Visits {@code CldrPath}s in an order which enforces the grouping of nested sub-paths.
         * With this ordering, all common "parent" path prefixes will be visited consecutively,
         * grouping together all "child" paths. However it is important to note that no other
         * promises are made and this ordering is still unstable and can change over time.
         *
         * <p>This ordering is useful for constructing nested visitors over some subset of paths
         * (e.g. processing all paths with a certain prefix consecutively).
         *
         * <p>For example, using {@code NESTED_GROUPING} the paths {@code //a/b/c/d},
         * {@code //a/b/e/f}, {@code //a/x/y/z} could be ordered like any of the following:
         * <ul>
         * <li>{@code //a/b/c/d} &lt; {@code //a/b/e/f} &lt; {@code //a/x/y/z}
         * <li>{@code //a/b/e/f} &lt; {@code //a/b/c/d} &lt; {@code //a/x/y/z}
         * <li>{@code //a/x/y/z} &lt; {@code //a/b/c/d} &lt; {@code //a/b/e/f}
         * <li>{@code //a/x/y/z} &lt; {@code //a/b/e/f} &lt; {@code //a/b/c/d}
         * </ul>
         * The only disallowed ordering here is one in which {@code //a/x/y/z} lies between the
         * other paths.
         */
        NESTED_GROUPING,

        /**
         * Visits {@code CldrPath}s in the order defined by the CLDR DTD declaration. This ordering
         * naturally enforces "nested grouping" of paths but additionally sorts paths so they are
         * visited in the order defined by the DTD of the {@link CldrDataType}.
         *
         * <p>This ordering is more stable than {@link PathOrder#NESTED_GROUPING}, but may still
         * change between different DTD versions.
         */
        DTD
    }

    /** A visitor for complete "leaf" paths and their associated values. */
    interface ValueVisitor {
        /** Callback method invoked for each value encountered by this visitor. */
        void visit(CldrValue value);
    }

    /** A visitor for partial path prefixes. */
    @SuppressWarnings("unused") // For unused arguments in no-op default methods.
    interface PrefixVisitor {
        /**
         * A controller API for allow prefix visitors to delegate sub-hierarchy visitation. A
         * context is passed into the {@link #visitPrefixStart(CldrPath, Context)} method in order
         * to allow the visitor to "install" a new visitor to handle the sub-hierarchy root at the
         * current path prefix.
         */
        interface Context {
            /**
             * Installs a value visitor at the current point in the visitation. The given visitor
             * will be called for all the values below this point in the path hierarchy, and the
             * current visitor will be automatically restored once visitation is complete.
             *
             * @param visitor a visitor to process the CLDR data sub-hierarchy rooted at the
             *                current path prefix.
             */
            default void install(ValueVisitor visitor) {
                install(visitor, v -> {});
            }

            /**
             * Installs a value visitor at the current point in the visitation. The given visitor
             * will be called for all the values below this point in the path hierarchy, and the
             * current visitor will be automatically restored once visitation is complete.
             *
             * @param visitor a visitor to process the CLDR data sub-hierarchy rooted at the
             *                current path prefix.
             * @param doneHandler a handler invoked just before the visitor is uninstalled.
             */
            <T extends ValueVisitor> void install(T visitor, Consumer<T> doneHandler);

            /**
             * Installs a prefix visitor at the current point in the visitation. The given visitor
             * will be called for all the start/end events below this point in the path hierarchy,
             * and the current visitor will be automatically restored once visitation is complete.
             *
             * @param visitor a visitor to process the CLDR data sub-hierarchy rooted at the
             *                current path prefix.
             */
            default void install(PrefixVisitor visitor) {
                install(visitor, v -> {});
            }

            /**
             * Installs a prefix visitor at the current point in the visitation. The given visitor
             * will be called for all the start/end events below this point in the path hierarchy,
             * and the current visitor will be automatically restored once visitation is complete.
             *
             * @param visitor a visitor to process the CLDR data sub-hierarchy rooted at the
             *                current path prefix.
             * @param doneHandler a handler invoked just before the visitor is uninstalled.
             */
            <T extends PrefixVisitor> void install(T visitor, Consumer<T> doneHandler);
        }

        /**
         * Callback method invoked for each partial path prefix encountered by this visitor.
         *
         * <p>A typical implementation of this method would test the given path to see if it's the
         * root of a desired sub-hierarchy and (if it matches) begin some sub-hierarchy processing,
         * which would often include installing a new visitor via the given context.
         *
         * @param prefix a path prefix processed as part of some visitation over CLDR data.
         * @param context a mechanism for installing sub-hierarchy visitors rooted at this point in
         *                the visitation.
         */
        default void visitPrefixStart(CldrPath prefix, Context context) {}

        /**
         * Callback method invoked to signal the end of some sub-hierarchy visitation. This method
         * is invoked exactly once for each call to {@link #visitPrefixStart(CldrPath, Context)},
         * in the opposite "stack" order with the same path prefix. This means that if this visitor
         * installs a sub-visitor during a call to {@code visitPrefixStart()} then the next
         * callback made to this visitor will be a call to {@code visitPrefixEnd()} with the same
         * path prefix.
         *
         * <p>A typical implementation of this method would detect the end of some expected
         * sub-visitation and do post-processing on the data.
         *
         * @param prefix a path prefix corresponding to the end of some previously started
         *               sub-hierarchy visitation.
         */
        default void visitPrefixEnd(CldrPath prefix) {}
    }
}
