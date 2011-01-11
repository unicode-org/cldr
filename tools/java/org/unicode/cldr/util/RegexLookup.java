package org.unicode.cldr.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;

import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.Transform;

/**
 * Lookup items according to a set of regex patterns. Returns the value according to the first pattern that matches. Not thread-safe.
 * @param <T>
 */
public class RegexLookup<T> {
    private final Map<String, Row.R2<Matcher,T>> entries = new LinkedHashMap<String, Row.R2<Matcher,T>>();
    private final Transform<String, Pattern> patternTransform;
    private final Transform<String, T> valueTransform;
    private final Merger<T> valueMerger;

    /**
     * Allows for merging items of the same type.
     * @param <T>
     */
    public interface Merger<T> {
        T merge(T a, T into);
    }

    /**
     * Returns the result of a regex lookup.
     * @param source
     * @return
     */
    public final T get(String source) {
        return get(source, null);
    }

    /**
     * Returns the result of a regex lookup, with the group arguments that matched.
     * @param source
     * @return
     */
    public T get(String source, CldrUtility.Output<String[]> arguments) {
        while (true) {
            for (R2<Matcher, T> entry : entries.values()) {
                Matcher matcher = entry.get0();
                if (matcher.reset(source).find()) {
                    if (arguments != null) {
                        int limit = matcher.groupCount() + 1;
                        arguments.value = new String[limit];
                        for (int i = 0; i < limit; ++i) {
                            arguments.value[i] = matcher.group(i);
                        }
                    }
                    return entry.get1();
                }
            }
            break;
        }
        return null;
    }

    /**
     * Create a RegexLookup. It will take a list of key/value pairs, where the key is a regex pattern and the value is what gets returned.
     */
    public RegexLookup() {
        this(null, null, null);
    }

    /**
     * Create a RegexLookup. It will take a list of key/value pairs, where the key is a regex pattern and the value is what gets returned.
     * @param patternTransform Used to transform string patterns into a Pattern. Can be used to process replacements (like variables).
     * @param valueTransform Used to transform string values into another form.
     * @param valueMerger Used to merge values with the same key.
     */
    public static <T> RegexLookup<T> of(Transform<String, Pattern> patternTransform, Transform<String, T> valueTransform, Merger<T> valueMerger) {
        return new RegexLookup<T>(patternTransform, valueTransform, valueMerger);
    }

    private RegexLookup(Transform<String, Pattern> patternTransform, Transform<String, T> valueTransform, Merger<T> valueMerger) {
        this.valueTransform = valueTransform;
        this.valueMerger = valueMerger;
        this.patternTransform = patternTransform;
    }

    /**
     * Load a RegexLookup from a file. Opens a file relative to the class, and adds lines separated by "; ". Lines starting with # are comments.
     */
    public RegexLookup<T> loadFromFile(Class<?> baseClass, String filename) {
        try {
            BufferedReader file = FileUtilities.openFile(baseClass, filename);
            for (int lineNumber = 0;; ++lineNumber) {
                String line = file.readLine();
                if (line == null) {
                    break;
                }
                line = line.trim();
                if (line.length() == 0 || line.startsWith("#")) {
                    continue;
                }
                int pos = line.indexOf("; ");
                if (pos < 0) {
                    throw new IllegalArgumentException("Failed to read RegexLookup File " + filename + "\t\t(" + lineNumber + ") " + line);
                }
                String source = line.substring(0,pos).trim();
                String target = line.substring(pos+2).trim();
                add(source, target);
            }
            return this;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Add a pattern/value pair, transforming the target according to the constructor valueTransform (if not null).
     * @param stringPattern
     * @param target
     * @return this, for chaining
     */
    public RegexLookup<T> add(String stringPattern, String target) {
        @SuppressWarnings("unchecked")
        T result = valueTransform == null ? (T) target : valueTransform.transform(target);
        return add(stringPattern, result);
    }

    /**
     * Add a pattern/value pair.
     * @param stringPattern
     * @param target
     * @return this, for chaining
     */
    public RegexLookup<T> add(String stringPattern, T target) {
        Pattern pattern0 = patternTransform != null ? patternTransform.transform(stringPattern) : Pattern.compile(stringPattern, Pattern.COMMENTS);
        return add(pattern0, target);
    }

    /**
     * Add a pattern/value pair.
     * @param pattern
     * @param target
     * @return this, for chaining
     */
    public RegexLookup<T> add(Pattern pattern, T target) {
        Matcher matcher = pattern.matcher("");
        R2<Matcher, T> old = entries.get(pattern.pattern());
        if (old == null) {
            entries.put(pattern.pattern(), Row.of(matcher, target));
        } else if (valueMerger != null) {
            valueMerger.merge(target, old.get1());
        } else {
            throw new IllegalArgumentException("Duplicate matcher without Merger defined");
        }
        return this;
    }
}