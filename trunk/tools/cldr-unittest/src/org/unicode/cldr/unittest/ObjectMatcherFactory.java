package org.unicode.cldr.unittest;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.unicode.cldr.util.PatternCache;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.util.CollectionUtilities.ObjectMatcher;

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
    public static ObjectMatcher<String> createRegexMatcher(String pattern) {
        return new RegexMatcher().set(pattern);
    }

    /**
     * Create a RegexMatcher
     *
     * @param pattern
     * @param flags
     * @return
     */
    public static ObjectMatcher<String> createRegexMatcher(String pattern,
        int flags) {
        return new RegexMatcher().set(pattern, flags);
    }

    /**
     * Create a CollectionMatcher
     *
     * @param col
     * @return
     */
    public static ObjectMatcher<String> createCollectionMatcher(
        Collection<String> col) {
        return new CollectionMatcher().set(col);
    }

    public static ObjectMatcher<String> createOrMatcher(
        ObjectMatcher<String> m1, ObjectMatcher<String> m2) {
        return new OrMatcher().set(m1, m2);
    }

    public static ObjectMatcher<String> createListMatcher(
        ObjectMatcher<String> matcher) {
        return new ListMatcher().set(matcher);
    }

    /**
     * Create a Matcher that will always return the value provided
     *
     * @return
     */
    public static ObjectMatcher<String> createDefaultingMatcher(boolean retVal) {
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
    public static ObjectMatcher<String> createNullHandlingMatcher(
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
    public static ObjectMatcher<String> createStringMatcher(String toMatch) {
        return new StringMatcher(toMatch);
    }

    private static class RegexMatcher implements ObjectMatcher<String> {
        private java.util.regex.Matcher matcher;

        public ObjectMatcher<String> set(String pattern) {
            matcher = PatternCache.get(pattern).matcher("");
            return this;
        }

        public ObjectMatcher<String> set(String pattern, int flags) {
            matcher = Pattern.compile(pattern, flags).matcher("");
            return this;
        }

        public boolean matches(String value) {
            matcher.reset(value.toString());
            return matcher.matches();
        }
    }

    private static class CollectionMatcher implements ObjectMatcher<String> {
        private Collection<String> collection;

        public ObjectMatcher<String> set(Collection<String> collection) {
            this.collection = collection;
            return this;
        }

        public boolean matches(String value) {
            return collection.contains(value);
        }
    }

    private static class OrMatcher implements ObjectMatcher<String> {
        private ObjectMatcher<String> a;
        private ObjectMatcher<String> b;

        public ObjectMatcher<String> set(ObjectMatcher<String> a,
            ObjectMatcher<String> b) {
            this.a = a;
            this.b = b;
            return this;
        }

        public boolean matches(String value) {
            return a.matches(value) || b.matches(value);
        }
    }

    private static class ListMatcher implements ObjectMatcher<String> {
        private ObjectMatcher<String> other;
        private static final Splitter WHITESPACE_SPLITTER = Splitter
            .on(PatternCache.get("\\s+"));

        public ObjectMatcher<String> set(ObjectMatcher<String> other) {
            this.other = other;
            return this;
        }

        public boolean matches(String value) {
            List<String> values = WHITESPACE_SPLITTER.splitToList(value.trim());
            if (values.size() == 1 && values.get(0).length() == 0)
                return true;
            for (String toMatch : values) {
                if (!other.matches(toMatch)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static class DefaultingMatcher implements ObjectMatcher<String> {
        private final boolean defaultValue;

        public DefaultingMatcher(boolean val) {
            defaultValue = val;
        }

        public boolean matches(String o) {
            return defaultValue;
        }
    }

    private static class NullHandlingMatcher implements ObjectMatcher<String> {

        final ObjectMatcher<String> matcher;

        public NullHandlingMatcher(
            Map<String, ObjectMatcherFactory.MatcherPattern> col,
            String key, boolean defaultVal) {
            ObjectMatcherFactory.MatcherPattern mpTemp = col.get(key);
            matcher = mpTemp == null ? new DefaultingMatcher(defaultVal)
                : mpTemp.matcher;
        }

        @Override
        public boolean matches(String o) {
            return matcher.matches(o);
        }

    }

    public static class MatcherPattern {
        public String value;
        public ObjectMatcher<String> matcher;
        public String pattern;

        public String toString() {
            return matcher.getClass().getName() + "\t" + pattern;
        }
    }

    private static class StringMatcher implements ObjectMatcher<String> {
        private final String value;

        public StringMatcher(String value) {
            this.value = value;
        }

        @Override
        public boolean matches(String o) {
            return o.equals(value);
        }

    }

}