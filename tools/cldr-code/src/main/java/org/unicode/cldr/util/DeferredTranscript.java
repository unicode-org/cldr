package org.unicode.cldr.util;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * lazily-calculates the transcript. Thread safe to call get() multiple times once commit() is
 * called and before clear() is called.
 */
public class DeferredTranscript implements Supplier<String> {

    private final class FormatEntry {
        public final CharSequence fmt;
        public final Object[] args;

        public FormatEntry(String fmt, Object[] args) {
            this.fmt = fmt;
            this.args = args;
        }

        public final CharSequence format() {
            if (args == null || args.length == 0) {
                return fmt;
            } else {
                return String.format(fmt.toString(), args);
            }
        }
    }

    public DeferredTranscript() {
        clear();
    }

    private Supplier<String> delegate = null;
    private List<FormatEntry> formats;

    /** reset this transcript so it can be used again. */
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
     * Add a formatted entry. Will be ignored if get() has been called since clear()
     *
     * @param fmt the string or pattern string
     * @param args if non-null, arguments to String.format()
     */
    final DeferredTranscript add(String fmt, Object... args) {
        formats.add(new FormatEntry(fmt, args));
        return this;
    }
}
