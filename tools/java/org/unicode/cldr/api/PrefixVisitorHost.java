package org.unicode.cldr.api;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.unicode.cldr.api.CldrData.PathOrder.NESTED_GROUPING;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.unicode.cldr.api.CldrData.PathOrder;
import org.unicode.cldr.api.CldrData.PrefixVisitor;
import org.unicode.cldr.api.CldrData.PrefixVisitor.Context;
import org.unicode.cldr.api.CldrData.ValueVisitor;

/**
 * Utility class for reconstructing nested path visitation from a sequence of path/value pairs. See
 * {@link PrefixVisitor} for more information.
 */
final class PrefixVisitorHost {
    /**
     * Accepts a prefix visitor over a nested sequence of prefix paths derived from the given data
     * instance. This method synthesizes a sequence of start/end events for all the sub-trees
     * implied by the sequence of CLDR paths produced by the data supplier.
     *
     * <p>For example, given the sequence:
     *
     * <pre>{@code
     * //ldml/foo/bar/first
     * //ldml/foo/bar/second
     * //ldml/foo/bar/third
     * //ldml/foo/baz/first
     * //ldml/foo/baz/second
     * //ldml/quux
     * }</pre>
     *
     * the following start, end and value visitation events will be derived:
     *
     * <pre>{@code
     * start: //ldml
     * start: //ldml/foo
     * start: //ldml/foo/bar
     * value: //ldml/foo/bar/first
     * value: //ldml/foo/bar/second
     * value: //ldml/foo/bar/third
     * end:   //ldml/foo/bar
     * start: //ldml/foo/baz
     * value: //ldml/foo/baz/first
     * value: //ldml/foo/baz/second
     * end:   //ldml/foo/baz
     * end:   //ldml/foo
     * value: //ldml/quux
     * end:   //ldml
     * }</pre>
     *
     * <p>Note that deriving the proper sequence of start/end events can only occur if the data is
     * provided in at least {@link PathOrder#NESTED_GROUPING NESTED_GROUPING} order. If a lower
     * path order (e.g. {@link PathOrder#ARBITRARY ARBITRARY}) is given then {@code NESTED_GROUPING}
     * will be used.
     */
    static void accept(
        BiConsumer<PathOrder, ValueVisitor> acceptFn, PathOrder order, PrefixVisitor v) {
        PrefixVisitorHost host = new PrefixVisitorHost(v);
        if (order.ordinal() < NESTED_GROUPING.ordinal()) {
            order = NESTED_GROUPING;
        }
        acceptFn.accept(order, host.visitor);
        host.endVisitation();
    }

    /**
     * Represents the root of a sub hierarchy visitation rooted at some path prefix. VisitorState
     * instances are kept in a stack; they are added when a new visitor is installed to begin a
     * sub hierarchy visitation and removed automatically once the visitation is complete.
     */
    @SuppressWarnings("unused")  // For unused arguments in no-op default methods.
    private static abstract class VisitorState {
        /** Creates a visitor state from the given visitor for the specified leaf value. */
        static <T extends ValueVisitor> VisitorState of(
            T visitor, Consumer<T> doneHandler, CldrPath prefix) {
            return new VisitorState(prefix, () -> doneHandler.accept(visitor)) {
                @Override
                public void visit(CldrValue value) {
                    visitor.visit(value);
                }
            };
        }

        /** Creates a visitor state from the given visitor rooted at the specified path prefix. */
        static <T extends PrefixVisitor> VisitorState of(
            T visitor, Consumer<T> doneHandler, CldrPath prefix) {
            return new VisitorState(prefix, () -> doneHandler.accept(visitor)) {
                @Override
                public void visitPrefixStart(CldrPath prefix, Context ctx) {
                    safeVisitPrefix(prefix, p -> visitor.visitPrefixStart(p, ctx));
                }

                @Override
                public void visitPrefixEnd(CldrPath prefix) {
                    safeVisitPrefix(prefix, visitor::visitPrefixEnd);
                }
            };
        }

        // The root of the sub hierarchy visitation.
        /* @Nullable */ private final CldrPath prefix;
        private final Runnable doneCallback;

        private VisitorState(CldrPath prefix, Runnable doneCallback) {
            this.prefix = prefix;
            this.doneCallback = doneCallback;
        }

        // These methods are the union of the public visitor methods and are used to dispatch
        // the events triggered by path processing to the currently installed visitor.
        void visit(CldrValue value) { }

        void visitPrefixStart(CldrPath prefix, Context ctx) { }

        void visitPrefixEnd(CldrPath prefix) { }

        // Helper to ensure that we don't fail the entire visitation when a single visitor fails.
        // NOTE: This could be removed once a more coherent error handling strategy is defined.
        private static void safeVisitPrefix(CldrPath prefix, Consumer<CldrPath> fn) {
            try {
                fn.accept(prefix);
            } catch (RuntimeException e) {
                System.err.format("Exception thrown by prefix visitor for path '%s'\n", prefix);
                System.err.println(e);
                e.printStackTrace(System.err);
            }
        }
    }

    // Stack of currently installed visitor.
    private final Deque<VisitorState> visitorStack = new ArrayDeque<>();
    /* @Nullable */ private CldrPath lastValuePath = null;

    // Visits a single value (with its path) and synthesizes prefix start/end calls according
    // to the state of the visitor stack.
    // This is a private field to avoid anyone accidentally calling the visit method directly.
    private final ValueVisitor visitor = value -> {
        CldrPath path = value.getPath();
        int commonLength = 0;
        if (lastValuePath != null) {
            commonLength = CldrPath.getCommonPrefixLength(lastValuePath, path);
            checkState(commonLength <= lastValuePath.getLength(),
                "unexpected child path encountered: %s is child of %s", path, lastValuePath);
            handleLastPath(commonLength);
        }
        // ... then down to the new path (which cannot be a parent of the old path either).
        checkState(commonLength <= path.getLength(),
            "unexpected parent path encountered: %s is parent of %s", path, lastValuePath);
        recursiveStartVisit(path.getParent(), commonLength, new PrefixContext());
        // This is a no-op if the head of the stack is a prefix visitor.
        visitorStack.peek().visit(value);
        lastValuePath = path;
    };

    private PrefixVisitorHost(PrefixVisitor visitor) {
        this.visitorStack.push(VisitorState.of(visitor, v -> {}, null));
    }

    // Called after visitation is complete to close out the last visited value path.
    private void endVisitation() {
        if (lastValuePath != null) {
            handleLastPath(0);
        }
    }

    // Recursively visits new prefix path elements (from top-to-bottom) for a new sub hierarchy.
    private void recursiveStartVisit(
        /* @Nullable */ CldrPath prefix, int commonLength, PrefixContext ctx) {
        if (prefix != null && prefix.getLength() > commonLength) {
            recursiveStartVisit(prefix.getParent(), commonLength, ctx);
            // Get the current visitor here (it could have been modified by the call above).
            // This is a no-op if the head of the stack is a value visitor.
            visitorStack.peek().visitPrefixStart(prefix, ctx.setPrefix(prefix));
        }
    }

    // Go up from the previous path to the common length (we _have_ already visited the leaf
    // node of the previous path and we do not allow the new path to be a sub-path) ...
    private void handleLastPath(int length) {
        for (CldrPath prefix = lastValuePath.getParent();
             prefix != null && prefix.getLength() > length;
             prefix = prefix.getParent()) {
            // Get the current visitor here (it could have been modified by the last iteration).
            //
            // Note: e.prefix can be null for the top-most entry in the stack, but that's fine
            // since it will never match "prefix" and we never want to remove it anyway.
            VisitorState e = visitorStack.peek();
            if (prefix.equals(e.prefix)) {
                e.doneCallback.run();
                visitorStack.pop();
                e = visitorStack.peek();
            }
            // This is a no-op if the head of the stack is a value visitor.
            e.visitPrefixEnd(prefix);
        }
    }

    /**
     * Implements a reusable context which captures the current prefix being processed. This is
     * used if a visitor wants to install a sub-visitor at a particular point during visitation.
     */
    private final class PrefixContext implements Context {
        // Only null until first use.
        private CldrPath prefix = null;

        // Must be called immediately prior to visiting a prefix visitor.
        private PrefixContext setPrefix(CldrPath prefix) {
            this.prefix = checkNotNull(prefix);
            return this;
        }

        @Override
        public <T extends PrefixVisitor> void install(T visitor, Consumer<T> doneHandler) {
            visitorStack.push(VisitorState.of(visitor, doneHandler, prefix));
        }

        @Override
        public <T extends ValueVisitor> void install(T visitor, Consumer<T> doneHandler) {
            visitorStack.push(VisitorState.of(visitor, doneHandler, prefix));
        }
    }
}
