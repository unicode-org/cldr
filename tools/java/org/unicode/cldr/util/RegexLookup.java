package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CldrUtility.VariableReplacer;
import org.unicode.cldr.util.RegexFileParser.RegexLineParser;
import org.unicode.cldr.util.RegexFileParser.VariableProcessor;
import org.unicode.cldr.util.RegexLookup.Finder;

import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.Transform;

/**
 * Lookup items according to a set of regex patterns. Returns the value according to the first pattern that matches. Not
 * thread-safe.
 * 
 * @param <T>
 */
public class RegexLookup<T> implements Iterable<Row.R2<Finder, T>> {
    private VariableReplacer variables = new VariableReplacer();
    private static final boolean DEBUG = true;
    private final Map<Finder, Row.R2<Finder, T>> entries = new LinkedHashMap<Finder, Row.R2<Finder, T>>();
    private Transform<String, ? extends Finder> patternTransform = RegexFinderTransform;
    private Transform<String, ? extends T> valueTransform;
    private Merger<T> valueMerger;
    private final boolean allowNull = false;

    public abstract static class Finder {
        abstract public String[] getInfo();

        abstract public boolean find(String item, Object context);

        public int getFailPoint(String source) {
            return -1;
        }
        // must also define toString
    }

    public static class RegexFinder extends Finder {
        protected final Matcher matcher;

        public RegexFinder(String pattern) {
            matcher = Pattern.compile(pattern, Pattern.COMMENTS).matcher("");
        }

        public boolean find(String item, Object context) {
            try {
                return matcher.reset(item).find();
            } catch (StringIndexOutOfBoundsException e) {
                // We don't know what causes this error (cldrbug 5051) so
                // make the exception message more detailed.
                throw new IllegalArgumentException("Matching error caused by pattern: ["
                    + matcher.toString() + "] on text: [" + item + "]", e);
            }
        }

        @Override
        public String[] getInfo() {
            int limit = matcher.groupCount() + 1;
            String[] value = new String[limit];
            for (int i = 0; i < limit; ++i) {
                value[i] = matcher.group(i);
            }
            return value;
        }

        public String toString() {
            return matcher.pattern().pattern();
        }

        @Override
        public boolean equals(Object obj) {
            return toString().equals(obj.toString());
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public int getFailPoint(String source) {
            return RegexUtilities.findMismatch(matcher, source);
        }
    }

    public static Transform<String, RegexFinder> RegexFinderTransform = new Transform<String, RegexFinder>() {
        public RegexFinder transform(String source) {
            return new RegexFinder(source);
        }
    };

    /**
     * The same as a RegexFinderTransform, except that [@ is changed to \[@, and ^ is added before //. To work better
     * with XPaths.
     */
    public static Transform<String, RegexFinder> RegexFinderTransformPath = new Transform<String, RegexFinder>() {
        public RegexFinder transform(String source) {
            final String newSource = source.replace("[@", "\\[@");
            return new RegexFinder(newSource.startsWith("//")
                ? "^" + newSource
                : newSource);
        }
    };

    /**
     * Allows for merging items of the same type.
     * 
     * @param <T>
     */
    public interface Merger<T> {
        T merge(T a, T into);
    }

    /**
     * Returns the result of a regex lookup.
     * 
     * @param source
     * @return
     */
    public final T get(String source) {
        return get(source, null, null, null, null);
    }

    /**
     * Returns the result of a regex lookup, with the group arguments that matched.
     * 
     * @param source
     * @param context
     *            TODO
     * @return
     */
    public T get(String source, Object context, CldrUtility.Output<String[]> arguments) {
        return get(source, context, arguments, null, null);
    }

    /**
     * Returns the result of a regex lookup, with the group arguments that matched. Supplies failure cases for
     * debugging.
     * 
     * @param source
     * @param context
     *            TODO
     * @return
     */
    public T get(String source, Object context, CldrUtility.Output<String[]> arguments,
        CldrUtility.Output<Finder> matcherFound, List<String> failures) {
        for (R2<Finder, T> entry : entries.values()) {
            Finder matcher = entry.get0();
            if (matcher.find(source, context)) {
                if (arguments != null) {
                    arguments.value = matcher.getInfo();
                }
                if (matcherFound != null) {
                    matcherFound.value = matcher;
                }
                return entry.get1();
            } else if (failures != null) {
                int failPoint = matcher.getFailPoint(source);
                String show = source.substring(0, failPoint) + "☹" + source.substring(failPoint) + "\t"
                    + matcher.toString();
                failures.add(show);
            }
        }
        // not really necessary, but makes debugging easier.
        if (arguments != null) {
            arguments.value = null;
        }
        if (matcherFound != null) {
            matcherFound.value = null;
        }
        return null;
    }

    /**
     * Returns all results of a regex lookup, with the group arguments that matched. Supplies failure cases for
     * debugging.
     * 
     * @param source
     * @param context
     *            TODO
     * @return
     */
    public List<T> getAll(String source, Object context, List<Finder> matcherList, List<String> failures) {
        List<T> matches = new ArrayList<T>();
        for (R2<Finder, T> entry : entries.values()) {
            Finder matcher = entry.get0();
            if (matcher.find(source, context)) {
                if (matcherList != null) {
                    matcherList.add(matcher);
                }
                matches.add(entry.get1());
            } else if (failures != null) {
                int failPoint = matcher.getFailPoint(source);
                String show = source.substring(0, failPoint) + "☹" + source.substring(failPoint) + "\t"
                    + matcher.toString();
                failures.add(show);
            }
        }
        return matches;
    }

    /**
     * Find the patterns that haven't been matched. Requires the caller to collect the patterns that have, using
     * matcherFound.
     * 
     * @return outputUnmatched
     */
    public Map<String, T> getUnmatchedPatterns(Set<String> matched, Map<String, T> outputUnmatched) {
        outputUnmatched.clear();
        for (R2<Finder, T> entry : entries.values()) {
            String pattern = entry.get0().toString();
            if (!matched.contains(pattern)) {
                outputUnmatched.put(pattern, entry.get1());
            }
        }
        return outputUnmatched;
    }

    /**
     * Create a RegexLookup. It will take a list of key/value pairs, where the key is a regex pattern and the value is
     * what gets returned.
     * 
     * @param patternTransform
     *            Used to transform string patterns into a Pattern. Can be used to process replacements (like
     *            variables).
     * @param valueTransform
     *            Used to transform string values into another form.
     * @param valueMerger
     *            Used to merge values with the same key.
     */
    public static <T, U> RegexLookup<T> of(Transform<String, Finder> patternTransform,
        Transform<String, T> valueTransform, Merger<T> valueMerger) {
        return new RegexLookup<T>().setPatternTransform(patternTransform).setValueTransform(valueTransform)
            .setValueMerger(valueMerger);
    }

    public static <T> RegexLookup<T> of(Transform<String, T> valueTransform) {
        return new RegexLookup<T>().setValueTransform(valueTransform).setPatternTransform(RegexFinderTransform);
    }

    public static <T> RegexLookup<T> of() {
        return new RegexLookup<T>().setPatternTransform(RegexFinderTransform);
    }

    public RegexLookup<T> setValueTransform(Transform<String, ? extends T> valueTransform) {
        this.valueTransform = valueTransform;
        return this;
    }

    public RegexLookup<T> setPatternTransform(Transform<String, ? extends Finder> patternTransform) {
        this.patternTransform = patternTransform != null ? patternTransform : RegexFinderTransform;
        return this;
    }

    public RegexLookup<T> setValueMerger(Merger<T> valueMerger) {
        this.valueMerger = valueMerger;
        return this;
    }

    /**
     * Load a RegexLookup from a file. Opens a file relative to the class, and adds lines separated by "; ". Lines
     * starting with # are comments.
     */
    public RegexLookup<T> loadFromFile(Class<?> baseClass, String filename) {
        RegexFileParser parser = new RegexFileParser();
        parser.setLineParser(new RegexLineParser() {
            @Override
            public void parse(String line) {
                int pos = line.indexOf("; ");
                if (pos < 0) {
                    throw new IllegalArgumentException();
                }
                String source = line.substring(0, pos).trim();
                String target = line.substring(pos + 2).trim();
                try {
                    @SuppressWarnings("unchecked")
                    T result = valueTransform == null ? (T) target : valueTransform.transform(target);
                    add(source, result);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Failed to add <" + source + "> => <" + target + ">", e);
                }
            }
        });
        parser.setVariableProcessor(new VariableProcessor() {
            @Override
            public void add(String variable, String variableName) {
                addVariable(variable, variableName);
            }

            @Override
            public String replace(String str) {
                return variables.replace(str);
            }
        });
        parser.parse(baseClass, filename);
        return this;
    }

    public RegexLookup<T> addVariable(String variable, String variableValue) {
        if (!variable.startsWith("%")) {
            throw new IllegalArgumentException("Variables must start with %");
        }
        variables.add(variable.trim(), variableValue.trim());
        return this;
    }

    /**
     * Add a pattern/value pair.
     * 
     * @param stringPattern
     * @param target
     * @return this, for chaining
     */
    public RegexLookup<T> add(String stringPattern, T target) {
        if (stringPattern.contains("%")) {
            stringPattern = variables.replace(stringPattern);
        }
        Finder pattern0 = patternTransform.transform(stringPattern);
        return add(pattern0, target);
    }

    /**
     * Add a pattern/value pair.
     * 
     * @param pattern
     * @param target
     * @return this, for chaining
     */
    public RegexLookup<T> add(Finder pattern, T target) {
        if (!allowNull && target == null) {
            throw new NullPointerException("null disallowed, unless allowNull(true) is called.");
        }
        R2<Finder, T> old = entries.get(pattern);
        if (old == null) {
            entries.put(pattern, Row.of(pattern, target));
        } else if (valueMerger != null) {
            valueMerger.merge(target, old.get1());
        } else {
            throw new IllegalArgumentException("Duplicate matcher without Merger defined " + pattern + "; old: " + old
                + "; new: " + target);
        }
        return this;
    }

    @Override
    public Iterator<R2<Finder, T>> iterator() {
        return Collections.unmodifiableCollection(entries.values()).iterator();
    }

    public static String replace(String lookup, String... arguments) {
        StringBuilder result = new StringBuilder();
        int last = 0;
        while (true) {
            int pos = lookup.indexOf("$", last);
            if (pos < 0) {
                result.append(lookup.substring(last, lookup.length()));
                break;
            }
            result.append(lookup.substring(last, pos));
            final int arg = lookup.charAt(pos + 1) - '0';
            try {
                result.append(arguments[arg]);
            } catch (Exception e) {
                throw new IllegalArgumentException("Replacing $" + arg + " in <" + lookup
                    + ">, but too few arguments supplied.");
            }
            last = pos + 2;
        }
        return result.toString();
    }

    /**
     * @return the number of entries
     */
    public int size() {
        return entries.size();
    }
}