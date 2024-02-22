package org.unicode.cldr.util;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A deferred list of formatted strings, one per line. The formatting is deferred until when (and
 * if) get() is called. It's thread safe to call get() multiple times, but add() should be called
 * from a single thread.
 *
 * <p>Once get() is called, the contents are 'resolved' into a string, and subsequent get() calls
 * will return the same string (even if add() was called). To re-use the object, call clear() and
 * then new content can be added with add().
 *
 * <p>In testing, there was a significant speed-up for the use case where get() might not be called.
 */
public class DeferredTranscript implements Supplier<String> {

    private final class FormatEntry {
        public final CharSequence fmt;
        public final Object[] args;

        /** Add a new FormatEntry as a deferred format */
        public FormatEntry(String fmt, Object[] args) {
            this.fmt = fmt;
            this.args = args;
        }

        /** Output this entry as a CharSequence, performing any formatting needed. */
        public final CharSequence format() {
            if (args == null || args.length == 0) {
                return fmt;
            } else {
                return String.format(fmt.toString(), args);
            }
        }
    }

    /** clear the DeferredTranscript on startup */
    public DeferredTranscript() {
        clear();
    }

    /** The first time this is read, it will calculate the entire string. */
    private Supplier<String> delegate = null;

    /** cached formats */
    private List<FormatEntry> formats;

    /** reset this transcript so it can be added to again. */
    public void clear() {
        // the delegate only calculates the value once, the first time it is accessed.
        delegate =
                Suppliers.memoize(
                        new Supplier<String>() {

                            @Override
                            public String get() {
                                return formats.stream()
                                        .map(FormatEntry::format)
                                        .collect(Collectors.joining("\n"));
                            }
                        });
        formats = new LinkedList<>();
    }

    @Override
    public final String get() {
        return delegate.get();
    }

    /**
     * Add a formatted entry. Will have no effect if get() has been called since clear() or object
     * construction.
     *
     * @param fmt the string or pattern string
     * @param args if non-null, arguments to String.format()
     */
    final DeferredTranscript add(String fmt, Object... args) {
        formats.add(new FormatEntry(fmt, args));
        return this;
    }
}
