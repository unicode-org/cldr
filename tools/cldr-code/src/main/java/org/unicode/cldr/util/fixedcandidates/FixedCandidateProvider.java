package org.unicode.cldr.util.fixedcandidates;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.unicode.cldr.util.PatternCache;

/**
 * A FixedCandidateProvider transforms an XPath into a set of Strings which should be the only
 * candidates for that XPath in the survey tool.
 */
public abstract class FixedCandidateProvider
        implements java.util.function.Function<String, Collection<String>> {
    /**
     * If null is returned, it indicates that that XPath does not participate in the fixed candidate
     * scheme. As a contrived example, <code>//colors/primary</code> might have the return values
     * "red", "green", "blue".
     *
     * @param xpath the input xpath to evaluate
     * @returns a list of values (or null if not applicable) of any 'fixed' candidates for this
     */
    public abstract Collection<String> apply(String xpath);

    /**
     * This class handles the common case where there is a matcher function and fixed set of
     * results.
     */
    public static class PredicateCollectionProvider extends FixedCandidateProvider {
        final Predicate<String> matcher;
        final Collection<String> collection;

        PredicateCollectionProvider(Predicate<String> matcher, Collection<String> collection) {
            this.matcher = matcher;
            this.collection = collection;
        }

        @Override
        public Collection<String> apply(String xpath) {
            if (matcher.test(xpath)) return collection;
            return null;
        }
    }

    /**
     * A provider that works based on an array of delegate providers. It is assumed that the
     * providers don't overlap.
     */
    public static class CompoundFixedCandidateProvider extends FixedCandidateProvider {
        final Collection<FixedCandidateProvider> delegates;

        @Override
        public Collection<String> apply(String xpath) {
            for (final FixedCandidateProvider fcp : delegates) {
                final Collection<String> r = fcp.apply(xpath);
                if (r != null) return r;
            }
            return null;
        }

        public CompoundFixedCandidateProvider(final Collection<FixedCandidateProvider> providers) {
            delegates = providers;
        }

        /** For testing - extract the delegates */
        Collection<FixedCandidateProvider> getDelegates() {
            return delegates;
        }
    }

    /** Create a provider that matches an XPath by regex */
    public static FixedCandidateProvider forXPathPattern(
            Pattern pattern, Collection<String> collection) {
        return new PredicateCollectionProvider(
                (String xpath) -> pattern.matcher(xpath).matches(), collection);
    }

    /** Create a provider that matches an XPath by regex */
    public static FixedCandidateProvider forXPathPattern(
            String patternString, Collection<String> collection) {
        return forXPathPattern(PatternCache.get(patternString), collection);
    }

    /** Create a provider that matches a fixed XPath */
    public static FixedCandidateProvider forXPath(
            final String string, final Collection<String> collection) {
        return new PredicateCollectionProvider((String xpath) -> string.equals(xpath), collection);
    }

    /**
     * Create a provider that matches an fixed XPath and returns a set of enum values
     *
     * @param xpath fixed xpath to match
     * @param values T.values() (or subset thereof)
     */
    public static <T extends Enum<T>> FixedCandidateProvider forXPathAndEnum(
            String xpath, T[] values) {
        return forXPath(xpath, FixedCandidateProvider.enumValueStrings(values));
    }

    /**
     * Create a provider that matches an XPath pattern and returns a set of enum values
     *
     * @param xpathPattern fixed xpath to match
     * @param values (or subset thereof)
     */
    public static <T extends Enum<T>> FixedCandidateProvider forXPathPatternAndEnum(
            String xpathPattern, T[] values) {
        return forXPathPattern(xpathPattern, FixedCandidateProvider.enumValueStrings(values));
    }

    /** helper function to convert an Enum to an array of strings */
    static final <T extends Enum<T>> Collection<String> enumValueStrings(T forValues[]) {
        final List<String> l = new ArrayList<String>(forValues.length);
        for (final T t : forValues) {
            l.add(t.toString());
        }
        return ImmutableList.copyOf(l);
    }
}
