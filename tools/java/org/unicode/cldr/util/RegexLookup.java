package org.unicode.cldr.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;


import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.Transform;

public class RegexLookup<T> {

    public interface Merger<T> {
        T merge(T a, T into);
    }

    private final Map<String, Row.R2<Matcher,T>> entries = new LinkedHashMap<String, Row.R2<Matcher,T>>();
    private final Transform<String, T> transform;
    private final Merger<T> merger;

    public T get(String source) {
        while (true) {
            for (R2<Matcher, T> entry : entries.values()) {
                Matcher matcher = entry.get0();
                if (matcher.reset(source).find()) {
                    return entry.get1();
                }
            }
            break;
        }
        return null;
    }

    public RegexLookup(Transform<String, T> transform, Merger<T> merger) {
        this.transform = transform;
        this.merger = merger;
    }

    public RegexLookup(Class baseClass, String filename, Transform<String, T> transform, Merger<T> merger) {
        this(transform, merger);
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
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public RegexLookup<T> add(String pattern, String target) {
        T result = transform == null ? (T) target : transform.transform(target);
        Matcher matcher = Pattern.compile(pattern,Pattern.COMMENTS).matcher("");
        R2<Matcher, T> old = entries.get(pattern);
        if (old == null) {
            entries.put(pattern, Row.of(matcher, result));
        } else if (merger != null) {
            merger.merge(result, old.get1());
        } else {
            throw new IllegalArgumentException("Duplicate matcher without Merger defined");
        }
        return this;
    }
}