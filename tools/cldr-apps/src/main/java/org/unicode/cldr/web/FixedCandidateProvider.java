package org.unicode.cldr.web;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import org.unicode.cldr.util.PatternCache;

abstract class FixedCandidateProvider
        implements java.util.function.Function<String, Collection<String>> {
    /**
     * @returns a list of values (or null if not applicable) of any 'fixed' candidates for this
     *     XPath
     */
    public abstract Collection<String> apply(String xpath);

    /** helper function to convert an Enum to an array of strings */
    static final <T extends Enum<T>> Collection<String> enumValueStrings(T forValues[]) {
        final List<String> l = new ArrayList<String>(forValues.length);
        for (final T t : forValues) {
            l.add(t.toString());
        }
        return ImmutableList.copyOf(l);
    }

    /** Candidate provider using a Pattern Cache */
    abstract static class PatternCacheCandidateProvider extends FixedCandidateProvider {
        final Pattern pattern;

        public PatternCacheCandidateProvider(String patternString) {
            pattern = PatternCache.get(patternString);
        }

        public Collection<String> apply(String xpath) {
            if (pattern.matcher(xpath).matches()) {
                return getCandidates();
            } else {
                return null; // not applicable
            }
        }

        protected abstract Collection<String> getCandidates();
    }
    /** Candidate provider for a single string (not regex) */
    abstract static class StringCandidateProvider extends FixedCandidateProvider {
        final String pattern;

        public StringCandidateProvider(String xpath) {
            pattern = xpath;
        }

        public Collection<String> apply(String xpath) {
            if (pattern.equals(xpath)) {
                return getCandidates();
            } else {
                return null; // not applicable
            }
        }

        protected abstract Collection<String> getCandidates();
    }

    /** create a provider that matches an fixed XPath and returns a set of values */
    public static <T extends Enum<T>> FixedCandidateProvider forEnumWithFixedXpath(
            String xpath, T[] values) {
        return new EnumStringCandidateProvider(
                xpath, FixedCandidateProvider.enumValueStrings(values));
    }

    private static class EnumStringCandidateProvider extends StringCandidateProvider {
        private final Collection<String> values;

        @Override
        protected Collection<String> getCandidates() {
            return values;
        }

        EnumStringCandidateProvider(final String xpath, final Collection<String> values) {
            super(xpath);
            this.values = values;
        }
    }
}
