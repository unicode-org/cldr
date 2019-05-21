package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import com.google.common.collect.TreeMultiset;

/**
 * Helper class that allows logging the use of regular expressions. A class that will summarize them will get a
 * NavigabSet of PatternCountInterface instances.
 *
 * @author ribnitz
 *
 */
public class RegexLogger {
    /**
     * Should debugging be done? - if not, a null implementation will be used
     */
    private static final boolean DEBUG = false;
    /**
     * Instance
     */
    private static RegexLoggerInterface instance = null;

    public static RegexLoggerInterface getInstance() {
        if (instance == null) {
            if (DEBUG) {
                instance = new RegexLoggerImpl();
            } else {
                instance = new NullRegexLogger();
            }
        }
        return instance;
    }

    private static class PatternStringWithBoolean implements Comparable<PatternStringWithBoolean> {
        private final String pattern;
        private final boolean calledFromRegexFinder;
        private final int hashCode;

        public PatternStringWithBoolean(String patternStr, boolean calledFromRegexFinder) {
            this.pattern = patternStr.trim();
            this.calledFromRegexFinder = calledFromRegexFinder;
            hashCode = Objects.hash(this.pattern, this.calledFromRegexFinder);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        public String getPattern() {
            return pattern;
        }

        public boolean isCalledFromRegexFinder() {
            return calledFromRegexFinder;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            PatternStringWithBoolean other = (PatternStringWithBoolean) obj;
            if (calledFromRegexFinder != other.calledFromRegexFinder) {
                return false;
            }
            if (hashCode != other.hashCode) {
                return false;
            }
            if (other.pattern != null) {
                return false;
            }
            if (!pattern.equals(other.pattern)) {
                return false;
            }
            return true;
        }

        @Override
        public int compareTo(PatternStringWithBoolean o) {
            if (o == null) {
                return 1;
            }
            if (this == o) {
                return 0;
            }
            return pattern.compareTo(o.pattern);
        }
    }

    /**
     * Interface used for logging Regular expressions
     * @author ribnitz
     *
     */
    public static interface RegexLoggerInterface {
        /**
         * Log that the given pattern was applied on the given matchStr, whether it matched, and
         * what the type of the log was. Cls conains the calling class.
         * @param pattern
         * @param matchStr
         * @param matched
         * @param type
         * @param cls
         */
        void log(String pattern, String matchStr, boolean matched, LogType type, Class<?> cls);

        void log(Matcher matcher, String matchStr, boolean matched, LogType type, Class<?> cls);

        void log(Pattern pattern, String matchStr, boolean matched, LogType type, Class<?> cls);

        void log(String pattern, String matchStr, boolean matched, double time, LogType type, Class<?> cls);

        /**
         * Get all the entries that matched
         * @return
         */
        NavigableSet<PatternCountInterface> getEntries();

        /**
         * Get the entries that occurred at least minCount times. If there are no matches, an empty set is returned
         * @param minCount
         * @return
         */
        NavigableSet<PatternCountInterface> getEntries(final int minCount);

        boolean isEnabled();
    }

    /**
     * Three of the methods can be delegations, which reduces the actual implementation to two methods
     * @author ribnitz
     *
     */
    private static abstract class AbstractRegexLogger implements RegexLoggerInterface {

        @Override
        public void log(Matcher matcher, String matchStr, boolean matched, LogType type, Class<?> cls) {
            log(matcher.pattern(), matchStr, matched, type, cls);

        }

        public void log(Pattern pattern, String matchStr, boolean matched, LogType type, Class<?> cls) {
            log(pattern.pattern(), matchStr, matched, type, cls);
        }

        public void log(String pattern, String matchStr, boolean matched, LogType type, Class<?> cls) {
            log(pattern, matchStr, matched, 0, type, cls);
        }

        /**
         * Get all entries
         */
        public NavigableSet<PatternCountInterface> getEntries() {
            return getEntries(1);
        }

        @Override
        public boolean isEnabled() {
            return DEBUG;
        }

    }

    /**
     * Null implementation
     * @author ribnitz
     *
     */
    private static class NullRegexLogger extends AbstractRegexLogger {

        @Override
        public void log(String pattern, String matchStr, boolean matched, double time, LogType type, Class<?> cls) {
            // do nothing
        }

        @Override
        public NavigableSet<PatternCountInterface> getEntries(int minCount) {
            NavigableSet<PatternCountInterface> returned = (NavigableSet<PatternCountInterface>) Sets.newTreeSet(Collections.EMPTY_SET);
            return returned;
        }
    }

    /**
     * Inetface used for the entries returnred by the RegexLogger
     * @author ribnitz
     *
     */
    public static interface PatternCountInterface {
        /**
         * Get the pattern used
         * @return
         */
        String getPattern();

        /**
         * Get the number of successful matches obtained through FIND
         * @return
         */
        int getNumberOfFindMatches();

        /**
         * Get the number of unsuccessful matches obtained through FIND
         * @return
         */
        int getNumberOfFindFailures();

        /**
         * Get the number of successful matches obtained through MATCH
         * @return
         */
        int getNumberOfMatchMatches();

        /**
         * Get the number of unsuccessful matches obtained through FIND
         * @return
         */
        int getNumberOfMatchFailures();

        /**
         * Return true if this call was made from RegexFinder
         * @return
         */
        boolean isCalledFromRegexFinder();

        /**
         * Get a set of all call locations
         * @return
         */
        Set<String> getCallLocations();

    }

    /**
     * GetAll uses this class to add all the entries of a multiSet to the result set, constructing
     * the object to return for each pattern. Objects will only be added once.
     *
     * This is the implementatioon that adds all items.
     * @author ribnitz
     *
     */
    private static class AddAllEntryProcessor {
        protected final int minCount;
        protected final CountSets c;
        protected final Set<PatternStringWithBoolean> seen = new HashSet<>();
        protected final NavigableSet<PatternCountInterface> result = new TreeSet<>();

        public AddAllEntryProcessor(int minCount, CountSets c) {
            this.minCount = minCount;
            this.c = c;
        }

        public NavigableSet<PatternCountInterface> getResult() {
            return result;
        }

        public void process(PatternStringWithBoolean item, Multiset<PatternStringWithBoolean> countSet) {
            if (!seen.contains(item)) {
                result.add(new RegexKeyWithCount(item, c));
                seen.add(item);
            }
        }
    }

    /**
     * Sometimes getEntries is called with a minCount; this Class filters and only adds the
     * items that occur at least minCount times.
     * @author ribnitz
     *
     */
    private static class EntryProcessor extends AddAllEntryProcessor {
        public EntryProcessor(int minCount, CountSets c) {
            super(minCount, c);
        }

        public void process(PatternStringWithBoolean item, Multiset<PatternStringWithBoolean> countSet) {
            if (countSet.count(item) >= minCount) {
                super.process(item, countSet);
            }
        }
    }

    /**
     * Since all the inner classes are static, this object is used to pass around the refernces to the
     * different sets/the state
     *
     * @author ribnitz
     *
     */
    private static class CountSets {
        final Multiset<PatternStringWithBoolean> matchedFindSet;
        final Multiset<PatternStringWithBoolean> failedFindSet;
        final Multiset<PatternStringWithBoolean> matchedMatchSet;
        final Multiset<PatternStringWithBoolean> failedMatchSet;
        final Multimap<PatternStringWithBoolean, String> stacktraces;

        public CountSets(Multiset<PatternStringWithBoolean> matchedFindSet, Multiset<PatternStringWithBoolean> failedFindSet,
            Multiset<PatternStringWithBoolean> matchedMatchSet, Multiset<PatternStringWithBoolean> failedMatchSet,
            Multimap<PatternStringWithBoolean, String> occurrences) {
            this.failedFindSet = failedFindSet;
            this.failedMatchSet = failedMatchSet;
            this.matchedMatchSet = matchedMatchSet;
            this.stacktraces = occurrences;
            this.matchedFindSet = matchedFindSet;
        }
    }

    private static class RegexKeyWithCount implements PatternCountInterface, Comparable<PatternCountInterface> {
        private final String pattern;
        private final int findMatchCount;
        private final int findFailCount;
        private final int matchMatchCount;
        private final int matchFailCount;
        private final boolean calledFromRegexFinder;
        private final Set<String> callLocations = new HashSet<>();
        private final int hashCode;

        public RegexKeyWithCount(PatternStringWithBoolean key, CountSets bean) {
            this.pattern = key.getPattern();
            this.calledFromRegexFinder = key.isCalledFromRegexFinder();
            this.findMatchCount = bean.matchedFindSet.count(key);
            this.findFailCount = bean.failedFindSet.count(key);
            this.matchMatchCount = bean.matchedMatchSet.count(key);
            this.matchFailCount = bean.failedMatchSet.count(key);
            Collection<String> tmp = bean.stacktraces.get(key);
            for (String cur : tmp) {
                if (!callLocations.contains(cur)) {
                    callLocations.add(cur);
                }
            }
            this.hashCode = Objects.hash(this.pattern,
                this.findMatchCount,
                this.findFailCount,
                this.matchFailCount,
                this.matchMatchCount,
                this.calledFromRegexFinder,
                this.callLocations);
        }

        public String getPattern() {
            return pattern;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public int getNumberOfFindMatches() {
            return findMatchCount;
        }

        @Override
        public int getNumberOfFindFailures() {
            return findFailCount;
        }

        @Override
        public int getNumberOfMatchMatches() {
            return matchMatchCount;
        }

        @Override
        public int getNumberOfMatchFailures() {
            return matchFailCount;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (hashCode != obj.hashCode()) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            RegexKeyWithCount other = (RegexKeyWithCount) obj;
            if (matchFailCount != other.matchFailCount) {
                return false;
            }
            if (matchMatchCount != other.matchMatchCount) {
                return false;
            }
            if (findFailCount != other.findFailCount) {
                return false;
            }
            if (findMatchCount != other.findMatchCount) {
                return false;
            }
            if (!pattern.equals(other.pattern)) {
                return false;
            }
            if (calledFromRegexFinder != other.calledFromRegexFinder) {
                return false;
            }
            if (callLocations != other.callLocations) {
                return false;
            }
            return true;
        }

        @Override
        public int compareTo(PatternCountInterface o) {
            if (o == null) {
                return 1;
            }
            return new Integer(matchFailCount + matchMatchCount + findFailCount + findMatchCount).compareTo(
                o.getNumberOfFindFailures() + o.getNumberOfFindMatches() + o.getNumberOfMatchFailures() + o.getNumberOfMatchMatches());
        }

        @Override
        public boolean isCalledFromRegexFinder() {
            return calledFromRegexFinder;
        }

        @Override
        public Set<String> getCallLocations() {
            return callLocations;
        }

    }

    public enum LogType {
        FIND, MATCH
    }

    private static interface IterableTransformer<E, F> {
        Iterable<F> transform(Iterable<E> input);
    }

    private static class StringIterableTransformer implements IterableTransformer<String, String> {

        @Override
        public Iterable<String> transform(Iterable<String> input) {
            List<String> returned = new ArrayList<>(Iterables.size(input));
            String lastClass = null;
            for (String current : input) {
                String transformed = current;
                if (lastClass != null) {
                    if (lastClass.startsWith("RegexLookup") && !current.startsWith("org.unicode.cldr.util.RegexLookup")) {
                        returned.add(lastClass);
                    }
                    break;
                }
                if (current.startsWith("org.unicode.cldr.test.CheckCLDR") &&
                    !lastClass.startsWith("org.unicode.cldr.test.CheckCLDR")) {
                    lastClass = current;
                    // leave out
                    continue;
                }
                // remove org.unicode.cldr
                if (current.startsWith("org.unicode.cldr.util.")) {
                    transformed = current.substring("org.unicode.cldr.util.".length());
                }
                // only the last RegexLookup will be added
                if (!transformed.startsWith("RegexLookup")) {
                    returned.add(transformed);
                }
                lastClass = transformed;
            }
            return returned;
        }
    }

    private static class ClassnameOnlyStringTransformer implements IterableTransformer<String, String> {

        @Override
        public Iterable<String> transform(Iterable<String> input) {
            List<String> returned = new ArrayList<>(Iterables.size(input));
            String lastClass = null;
            for (String current : input) {
                if (current.lastIndexOf(".") > 0) {
                    current = current.substring(current.lastIndexOf("."));
                }
                if (lastClass != null) {
                    if (lastClass.startsWith("RegexLookup") && !current.startsWith("RegexLookup")) {
                        returned.add(lastClass);
                    }
                    if (lastClass.startsWith("VettingViewer")) {
                        break;
                    }
                    if (current.startsWith("CheckCLDR") && !lastClass.startsWith("CheckCLDR")) {
                        lastClass = current;
                        // leave out
                        continue;
                    }
                }
                // only the last RegexLookup will be added
                if (!current.startsWith("RegexLookup")) {
                    returned.add(current);
                }
                lastClass = current;
            }
            return returned;
        }
    }

    /**
     * This is the class doing the bulk of the work.
     * @author ribnitz
     */
    private static class RegexLoggerImpl extends AbstractRegexLogger {

        /*
         * Each has more than 1m hits, together they account for about 14m (of the 26m total)
         */
        private static final Set<String> exactMatchSet = new HashSet<>(Arrays.asList(new String[] {
            "^//ldml.*",
            "^//ldml/dates.*",
            "^//ldml/units.*",
            "^//ldml/characters/ellipsis[@type=\"(final|initial|medial)\"]",
            "^//ldml/characters.*",
            "^//ldml/listPatterns/listPattern.*",
            "^//ldml/units/unitLength[@type=\"(long|short|narrow)\"].*",
        }));
        private static final Set<String> patternSet = new HashSet<>(Arrays.asList(new String[] {
            "^//ldml/dates/fields",
            "^//ldml/dates/calendars/calendar",
            "/(availableFormats",
        }));
        private final Multiset<PatternStringWithBoolean> matchedFindSet = TreeMultiset.create();
        private final Multiset<PatternStringWithBoolean> failedFindSet = TreeMultiset.create();
        private final Multiset<PatternStringWithBoolean> matchedMatchSet = TreeMultiset.create();
        private final Multiset<PatternStringWithBoolean> failedMatchSet = TreeMultiset.create();

        private final Multimap<PatternStringWithBoolean, String> occurrences = TreeMultimap.create();
        private final IterableTransformer<String, String> transformer = new StringIterableTransformer();

        public void log(String pattern, String matchStr, boolean matched, double time, LogType type, Class<?> cls) {
            boolean isRegexFinder = findClassName("org.unicode.cldr.util.RegexLookup", 10);
            PatternStringWithBoolean key = new PatternStringWithBoolean(pattern, isRegexFinder);
            Collection<PatternStringWithBoolean> collectionToAdd = determineCollectionToUse(matched, type);
            if (collectionToAdd != null) {
                collectionToAdd.add(key);
            }
            if (shouldLogPattern(pattern, isRegexFinder)) {
                addElementToList(key);
            }
        }

        private Collection<PatternStringWithBoolean> determineCollectionToUse(boolean matched, LogType type) {
            Collection<PatternStringWithBoolean> collectionToAdd = null;
            switch (type) {
            case FIND:
                if (matched) {
                    collectionToAdd = matchedFindSet;
                } else {
                    collectionToAdd = failedFindSet;
                }
                break;
            case MATCH:
                if (matched) {
                    collectionToAdd = matchedMatchSet;
                } else {
                    collectionToAdd = failedMatchSet;
                }
                break;
            }
            return collectionToAdd;
        }

        private boolean shouldLogPattern(String pattern, boolean isRegexFinder) {
            if (!isRegexFinder) {
                return true;
            } else {
                if (exactMatchSet.contains(pattern)) {
                    return true;
                } else {
                    for (String cur : patternSet) {
                        if (pattern.startsWith(cur)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private boolean findClassName(String className, int depth) {
            StackTraceElement[] st = Thread.currentThread().getStackTrace();
            int startPos = (st.length > 2) ? 2 : 0;
            int endPos = (startPos + depth > st.length) ? st.length : startPos + depth;
            for (int i = startPos; i < endPos; i++) {
                StackTraceElement cur = st[i];
                String curClass = cur.getClassName();
                if (curClass.startsWith(className)) {
                    return true;
                }
            }
            return false;
        }

        private final static Joiner JOINER = Joiner.on(";");

        private void addElementToList(PatternStringWithBoolean key) {
            List<String> stList = processStackTrace("org.unicode.cldr.util.RegexLookup", 0);

            if (!stList.isEmpty()) {
                occurrences.put(key, JOINER.join(transformer.transform(stList)));
            }
        }

        private List<String> processStackTrace(String classNameToStartAt, int depth) {
            StackTraceElement[] st = Thread.currentThread().getStackTrace();
            if (depth == 0) {
                depth = st.length;
            }
            int startPos;
            if (depth < 0) {
                startPos = depth + st.length;
                depth = Math.abs(depth);
            } else {
                startPos = (st.length > 2) ? 2 : 0;
            }
            int pos;
            boolean found = false;
            for (pos = startPos; pos < st.length; pos++) {
                if (st[pos].getClassName().startsWith(classNameToStartAt)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return Collections.emptyList();
            }
            int endPos = (pos + depth > st.length) ? st.length : startPos + depth;
            List<String> ret = new ArrayList<>(depth + 2);
            for (int i = pos; i < endPos; i++) {
                StackTraceElement cur = st[i];
                String curClass = cur.getClassName();
                ret.add(curClass + ":" + cur.getLineNumber());
            }
            return ret;
        }

        public NavigableSet<PatternCountInterface> getEntries(final int minCount) {
            CountSets c = new CountSets(matchedFindSet, failedFindSet, matchedMatchSet, failedMatchSet, occurrences);
            final AddAllEntryProcessor processor = (minCount == 1) ? new AddAllEntryProcessor(minCount, c) : new EntryProcessor(minCount, c);
            for (PatternStringWithBoolean item : matchedFindSet) {
                processor.process(item, matchedFindSet);
            }
            for (PatternStringWithBoolean item : failedFindSet) {
                processor.process(item, failedFindSet);
            }
            for (PatternStringWithBoolean item : matchedMatchSet) {
                processor.process(item, matchedMatchSet);
            }
            for (PatternStringWithBoolean item : failedMatchSet) {
                processor.process(item, failedMatchSet);
            }
            return Sets.unmodifiableNavigableSet(processor.getResult());
        }
    }
}
