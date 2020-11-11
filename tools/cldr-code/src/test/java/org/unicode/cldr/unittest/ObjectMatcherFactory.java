package org.unicode.cldr.unittest;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.unicode.cldr.util.PatternCache;

import com.google.common.base.Splitter;

/**
 * Factory for ObjectMatchers that are not tightly coupled
 *
 * @author ribnitz
 *
 */
class ObjectMatcherFactory {
    /**
     * Create a RegexMatcher
     *
     * @param pattern
     * @return
     */
    public static Predicate<String> createRegexMatcher(String pattern) {
        return new RegexMatcher().set(pattern);
    }

    /**
     * Create a RegexMatcher
     *
     * @param pattern
     * @param flags
     * @return
     */
    public static Predicate<String> createRegexMatcher(String pattern,
        int flags) {
        return new RegexMatcher().set(pattern, flags);
    }

    /**
     * Create a CollectionMatcher
     *
     * @param col
     * @return
     */
    public static Predicate<String> createCollectionMatcher(
        Collection<String> col) {
        return new CollectionMatcher().set(col);
    }

    public static Predicate<String> createOrMatcher(
        Predicate<String> m1, Predicate<String> m2) {
        return new OrMatcher().set(m1, m2);
    }

    public static Predicate<String> createListMatcher(
        Predicate<String> matcher) {
        return new ListMatcher().set(matcher);
    }

    /**
     * Create a Matcher that will always return the value provided
     *
     * @return
     */
    public static Predicate<String> createDefaultingMatcher(boolean retVal) {
        return new DefaultingMatcher(retVal);
    }

    /**
     * Create a matcher based on the value accessible with key, in the map; if
     * there is no key, use a DefaultingMatcher to return valueIfAbsent
     *
     * @param m
     * @param key
     * @param valueIfAbsent
     * @return
     */
    public static Predicate<String> createNullHandlingMatcher(
        Map<String, ObjectMatcherFactory.MatcherPattern> m, String key,
        boolean valueIfAbsent) {
        return new NullHandlingMatcher(m, key, valueIfAbsent);
    }

    /***
     * Create a matcher that will return true, if the String provided is
     * matched; comparison is done using equals()
     *
     * @param toMatch
     * @return
     */
    public static Predicate<String> createStringMatcher(String toMatch) {
        return new StringMatcher(toMatch);
    }

    private static class RegexMatcher implements Predicate<String> {
        private java.util.regex.Matcher matcher;

        public Predicate<String> set(String pattern) {
            matcher = PatternCache.get(pattern).matcher("");
            return this;
        }

        public Predicate<String> set(String pattern, int flags) {
            matcher = Pattern.compile(pattern, flags).matcher("");
            return this;
        }

        @Override
        public boolean test(String value) {
            matcher.reset(value.toString());
            return matcher.matches();
        }
    }

    private static class CollectionMatcher implements Predicate<String> {
        private Collection<String> collection;

        public Predicate<String> set(Collection<String> collection) {
            this.collection = collection;
            return this;
        }

        @Override
        public boolean test(String value) {
            return collection.contains(value);
        }
    }

    private static class OrMatcher implements Predicate<String> {
        private Predicate<String> a;
        private Predicate<String> b;

        public Predicate<String> set(Predicate<String> a,
            Predicate<String> b) {
            this.a = a;
            this.b = b;
            return this;
        }

        @Override
        public boolean test(String value) {
            return a.test(value) || b.test(value);
        }
    }

    private static class ListMatcher implements Predicate<String> {
        private Predicate<String> other;
        private static final Splitter WHITESPACE_SPLITTER = Splitter
            .on(PatternCache.get("\\s+"));

        public Predicate<String> set(Predicate<String> other) {
            this.other = other;
            return this;
        }

        @Override
        public boolean test(String value) {
            List<String> values = WHITESPACE_SPLITTER.splitToList(value.trim());
            if (values.size() == 1 && values.get(0).length() == 0)
                return true;
            for (String toMatch : values) {
                if (!other.test(toMatch)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static class DefaultingMatcher implements Predicate<String> {
        private final boolean defaultValue;

        public DefaultingMatcher(boolean val) {
            defaultValue = val;
        }

        @Override
        public boolean test(String o) {
            return defaultValue;
        }
    }

    private static class NullHandlingMatcher implements Predicate<String> {

        final Predicate<String> matcher;

        public NullHandlingMatcher(
            Map<String, ObjectMatcherFactory.MatcherPattern> col,
            String key, boolean defaultVal) {
            ObjectMatcherFactory.MatcherPattern mpTemp = col.get(key);
            matcher = mpTemp == null ? new DefaultingMatcher(defaultVal)
                : mpTemp.matcher;
        }

        @Override
        public boolean test(String o) {
            return matcher.test(o);
        }

    }

    public static class MatcherPattern {
        public String value;
        public Predicate<String> matcher;
        public String pattern;

        @Override
        public String toString() {
            return matcher.getClass().getName() + "\t" + pattern;
        }
    }

    private static class StringMatcher implements Predicate<String> {
        private final String value;

        public StringMatcher(String value) {
            this.value = value;
        }

        @Override
        public boolean test(String o) {
            return o.equals(value);
        }

    }

}