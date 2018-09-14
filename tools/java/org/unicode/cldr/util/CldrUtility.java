/*
 **********************************************************************
 * Copyright (c) 2002-2013, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.RegexLookup.Finder;

import com.google.common.base.Splitter;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.Freezable;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.TimeZone;

public class CldrUtility {

    public static final Charset UTF8 = Charset.forName("utf-8");
    public static final boolean BETA = false;

    public static final String LINE_SEPARATOR = "\n";
    public final static Pattern SEMI_SPLIT = PatternCache.get("\\s*;\\s*");

    private static final boolean HANDLEFILE_SHOW_SKIP = false;
    // Constant for "∅∅∅". Indicates that a child locale has no value for a
    // path even though a parent does.
    public static final String NO_INHERITANCE_MARKER = new String(new char[] { 0x2205, 0x2205, 0x2205 });

    /**
     * Define the constant INHERITANCE_MARKER for "↑↑↑", used by Survey Tool to indicate a "passthru" vote to the parent locale.
     * If CLDRFile ever finds this value in a data field, writing of the field should be suppressed.
     */
    public static final String INHERITANCE_MARKER = new String(new char[] { 0x2191, 0x2191, 0x2191 });

    public static final UnicodeSet DIGITS = new UnicodeSet("[0-9]").freeze();

    /**
     * Very simple class, used to replace variables in a string. For example
     * <p>
     *
     * <pre>
     * static VariableReplacer langTag = new VariableReplacer()
     * 			.add("$alpha", "[a-zA-Z]")
     * 			.add("$digit", "[0-9]")
     * 			.add("$alphanum", "[a-zA-Z0-9]")
     * 			.add("$x", "[xX]");
     * 			...
     * 			String langTagPattern = langTag.replace(...);
     * </pre>
     */
    public static class VariableReplacer {
        // simple implementation for now
        private Map<String, String> m = new TreeMap<String, String>(Collections.reverseOrder());

        public VariableReplacer add(String variable, String value) {
            m.put(variable, value);
            return this;
        }

        public String replace(String source) {
            String oldSource;
            do {
                oldSource = source;
                for (Iterator<String> it = m.keySet().iterator(); it.hasNext();) {
                    String variable = it.next();
                    String value = m.get(variable);
                    source = replaceAll(source, variable, value);
                }
            } while (!source.equals(oldSource));
            return source;
        }

        public String replaceAll(String source, String key, String value) {
            while (true) {
                int pos = source.indexOf(key);
                if (pos < 0) return source;
                source = source.substring(0, pos) + value + source.substring(pos + key.length());
            }
        }
    }

    public interface LineHandler {
        /**
         * Return false if line was skipped
         *
         * @param line
         * @return
         */
        boolean handle(String line) throws Exception;
    }

    public static String getPath(String path, String filename) {
        if (path == null) {
            return null;
        }
        final File file = filename == null ? new File(path)
            : new File(path, filename);
        try {
            return file.getCanonicalPath() + File.separatorChar;
        } catch (IOException e) {
            return file.getPath() + File.separatorChar;
        }
    }

    static String getPath(String path) {
        return getPath(path, null);
    }

    public static final String ANALYTICS = "<script type=\"text/javascript\">\n"
        + "var gaJsHost = ((\"https:\" == document.location.protocol) ? \"https://ssl.\" : \"http://www.\");\n"
        + "document.write(unescape(\"%3Cscript src='\" + gaJsHost + \"google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E\"));\n"
        + "</script>\n"
        + "<script type=\"text/javascript\">\n"
        + "try {\n"
        + "var pageTracker = _gat._getTracker(\"UA-7672775-1\");\n"
        + "pageTracker._trackPageview();\n"
        + "} catch(err) {}</script>";

    public static final List<String> MINIMUM_LANGUAGES = Arrays.asList(new String[] { "ar", "en", "de", "fr", "hi",
        "it", "es", "pt", "ru", "zh", "ja" }); // plus language itself
    public static final List<String> MINIMUM_TERRITORIES = Arrays.asList(new String[] { "US", "GB", "DE", "FR", "IT",
        "JP", "CN", "IN", "RU", "BR" });

    public interface LineComparer {
        static final int LINES_DIFFERENT = -1, LINES_SAME = 0, SKIP_FIRST = 1, SKIP_SECOND = 2;

        /**
         * Returns LINES_DIFFERENT, LINES_SAME, or if one of the lines is ignorable, SKIP_FIRST or SKIP_SECOND
         *
         * @param line1
         * @param line2
         * @return
         */
        int compare(String line1, String line2);
    }

    public static class SimpleLineComparator implements LineComparer {
        public static final int TRIM = 1, SKIP_SPACES = 2, SKIP_EMPTY = 4, SKIP_CVS_TAGS = 8;
        StringIterator si1 = new StringIterator();
        StringIterator si2 = new StringIterator();
        int flags;

        public SimpleLineComparator(int flags) {
            this.flags = flags;
        }

        public int compare(String line1, String line2) {
            // first, see if we want to skip one or the other lines
            int skipper = 0;
            if (line1 == null) {
                skipper = SKIP_FIRST;
            } else {
                if ((flags & TRIM) != 0) line1 = line1.trim();
                if ((flags & SKIP_EMPTY) != 0 && line1.length() == 0) skipper = SKIP_FIRST;
            }
            if (line2 == null) {
                skipper = SKIP_SECOND;
            } else {
                if ((flags & TRIM) != 0) line2 = line2.trim();
                if ((flags & SKIP_EMPTY) != 0 && line2.length() == 0) skipper += SKIP_SECOND;
            }
            if (skipper != 0) {
                if (skipper == SKIP_FIRST + SKIP_SECOND) return LINES_SAME; // ok, don't skip both
                return skipper;
            }

            // check for null
            if (line1 == null) {
                if (line2 == null) return LINES_SAME;
                return LINES_DIFFERENT;
            }
            if (line2 == null) {
                return LINES_DIFFERENT;
            }

            // now check equality
            if (line1.equals(line2)) return LINES_SAME;

            // if not equal, see if we are skipping spaces
            if ((flags & SKIP_CVS_TAGS) != 0) {
                if (line1.indexOf('$') >= 0 && line2.indexOf('$') >= 0) {
                    line1 = stripTags(line1);
                    line2 = stripTags(line2);
                    if (line1.equals(line2)) return LINES_SAME;
                } else if (line1.startsWith("<!DOCTYPE ldml SYSTEM \"../../common/dtd/")
                    && line2.startsWith("<!DOCTYPE ldml SYSTEM \"../../common/dtd/")) {
                    return LINES_SAME;
                }
            }
            if ((flags & SKIP_SPACES) != 0 && si1.set(line1).matches(si2.set(line2))) return LINES_SAME;
            return LINES_DIFFERENT;
        }

        // private Matcher dtdMatcher = PatternCache.get(
        // "\\Q<!DOCTYPE ldml SYSTEM \"http://www.unicode.org/cldr/dtd/\\E.*\\Q/ldml.dtd\">\\E").matcher("");

        private String[] CVS_TAGS = { "Revision", "Date" };

        private String stripTags(String line) {
            // $
            // Revision: 8994 $
            // $
            // Date: 2013-07-03 21:31:17 +0200 (Wed, 03 Jul 2013) $
            int pos = line.indexOf('$');
            if (pos < 0) return line;
            pos++;
            int endpos = line.indexOf('$', pos);
            if (endpos < 0) return line;
            for (int i = 0; i < CVS_TAGS.length; ++i) {
                if (!line.startsWith(CVS_TAGS[i], pos)) continue;
                line = line.substring(0, pos + CVS_TAGS[i].length()) + line.substring(endpos);
            }
            return line;
        }

    }

    /**
     *
     * @param file1
     * @param file2
     * @param failureLines
     *            on input, String[2], on output, failing lines
     * @param lineComparer
     * @return
     * @throws IOException
     */
    public static boolean areFileIdentical(String file1, String file2, String[] failureLines,
        LineComparer lineComparer) throws IOException {
        try (BufferedReader br1 = new BufferedReader(new FileReader(file1), 32 * 1024);
            BufferedReader br2 = new BufferedReader(new FileReader(file2), 32 * 1024);) {
            String line1 = "";
            String line2 = "";
            int skip = 0;

            while (true) {
                if ((skip & LineComparer.SKIP_FIRST) == 0) line1 = br1.readLine();
                if ((skip & LineComparer.SKIP_SECOND) == 0) line2 = br2.readLine();
                if (line1 == null && line2 == null) return true;
                if (line1 == null || line2 == null) {
                    // System.out.println("debug");
                }
                skip = lineComparer.compare(line1, line2);
                if (skip == LineComparer.LINES_DIFFERENT) {
                    break;
                }
            }
            failureLines[0] = line1 != null ? line1 : "<end of file>";
            failureLines[1] = line2 != null ? line2 : "<end of file>";
            return false;
        }
    }

    /*
     * static String getLineWithoutFluff(BufferedReader br1, boolean first, int flags) throws IOException {
     * while (true) {
     * String line1 = br1.readLine();
     * if (line1 == null) return line1;
     * if ((flags & TRIM)!= 0) line1 = line1.trim();
     * if ((flags & SKIP_EMPTY)!= 0 && line1.length() == 0) continue;
     * return line1;
     * }
     * }
     */

    public final static class StringIterator {
        String string;
        int position = 0;

        char next() {
            while (true) {
                if (position >= string.length()) return '\uFFFF';
                char ch = string.charAt(position++);
                if (ch != ' ' && ch != '\t') return ch;
            }
        }

        StringIterator reset() {
            position = 0;
            return this;
        }

        StringIterator set(String string) {
            this.string = string;
            position = 0;
            return this;
        }

        boolean matches(StringIterator other) {
            while (true) {
                char c1 = next();
                char c2 = other.next();
                if (c1 != c2) return false;
                if (c1 == '\uFFFF') return true;
            }
        }

        /**
         * @return Returns the position.
         */
        public int getPosition() {
            return position;
        }
    }

    public static String[] splitArray(String source, char separator) {
        return splitArray(source, separator, false);
    }

    public static String[] splitArray(String source, char separator, boolean trim) {
        List<String> piecesList = splitList(source, separator, trim);
        String[] pieces = new String[piecesList.size()];
        piecesList.toArray(pieces);
        return pieces;
    }

    public static String[] splitCommaSeparated(String line) {
        // items are separated by ','
        // each item is of the form abc...
        // or "..." (required if a comma or quote is contained)
        // " in a field is represented by ""
        List<String> result = new ArrayList<String>();
        StringBuilder item = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); ++i) {
            char ch = line.charAt(i); // don't worry about supplementaries
            switch (ch) {
            case '"':
                inQuote = !inQuote;
                // at start or end, that's enough
                // if get a quote when we are not in a quote, and not at start, then add it and return to inQuote
                if (inQuote && item.length() != 0) {
                    item.append('"');
                    inQuote = true;
                }
                break;
            case ',':
                if (!inQuote) {
                    result.add(item.toString());
                    item.setLength(0);
                } else {
                    item.append(ch);
                }
                break;
            default:
                item.append(ch);
                break;
            }
        }
        result.add(item.toString());
        return result.toArray(new String[result.size()]);
    }

    public static List<String> splitList(String source, char separator) {
        return splitList(source, separator, false, null);
    }

    public static List<String> splitList(String source, char separator, boolean trim) {
        return splitList(source, separator, trim, null);
    }

    public static List<String> splitList(String source, char separator, boolean trim, List<String> output) {
        return splitList(source, Character.toString(separator), trim, output);
    }

    public static List<String> splitList(String source, String separator) {
        return splitList(source, separator, false, null);
    }

    public static List<String> splitList(String source, String separator, boolean trim) {
        return splitList(source, separator, trim, null);
    }

    public static List<String> splitList(String source, String separator, boolean trim, List<String> output) {
        if (output == null) output = new ArrayList<String>();
        if (source.length() == 0) return output;
        int pos = 0;
        do {
            int npos = source.indexOf(separator, pos);
            if (npos < 0) npos = source.length();
            String piece = source.substring(pos, npos);
            if (trim) piece = piece.trim();
            output.add(piece);
            pos = npos + 1;
        } while (pos < source.length());
        return output;
    }

    /**
     * Protect a collection (as much as Java lets us!) from modification.
     * Really, really ugly code, since Java doesn't let us do better.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> T protectCollection(T source) {
        // TODO - exclude UnmodifiableMap, Set, ...
        if (source instanceof Map) {
            Map sourceMap = (Map) source;
            Map resultMap = clone(sourceMap);
            if (resultMap == null) return (T) sourceMap; // failed
            resultMap.clear();
            for (Object key : sourceMap.keySet()) {
                resultMap.put(protectCollection(key), protectCollection(sourceMap.get(key)));
            }
            return resultMap instanceof SortedMap ? (T) Collections.unmodifiableSortedMap((SortedMap) resultMap)
                : (T) Collections.unmodifiableMap(resultMap);
        } else if (source instanceof Collection) {
            Collection sourceCollection = (Collection) source;
            Collection<Object> resultCollection = clone(sourceCollection);
            if (resultCollection == null) return (T) sourceCollection; // failed
            resultCollection.clear();

            for (Object item : sourceCollection) {
                resultCollection.add(protectCollection(item));
            }

            return sourceCollection instanceof List ? (T) Collections.unmodifiableList((List) sourceCollection)
                : sourceCollection instanceof SortedSet ? (T) Collections
                    .unmodifiableSortedSet((SortedSet) sourceCollection)
                    : sourceCollection instanceof Set ? (T) Collections.unmodifiableSet((Set) sourceCollection)
                        : (T) Collections.unmodifiableCollection(sourceCollection);
        } else if (source instanceof Freezable) {
            Freezable freezableSource = (Freezable) source;
            if (freezableSource.isFrozen()) return source;
            return (T) ((Freezable) (freezableSource.cloneAsThawed())).freeze();
        } else {
            return source; // can't protect
        }
    }

    /**
     * Protect a collections where we don't need to clone.
     * @param source
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> T protectCollectionX(T source) {
        // TODO - exclude UnmodifiableMap, Set, ...
        if (isImmutable(source)) {
            return source;
        }
        if (source instanceof Map) {
            Map sourceMap = (Map) source;
            // recurse
            LinkedHashMap tempMap = new LinkedHashMap<>(sourceMap); // copy contents
            sourceMap.clear();
            for (Object key : tempMap.keySet()) {
                sourceMap.put(protectCollection(key), protectCollectionX(tempMap.get(key)));
            }
            return sourceMap instanceof SortedMap ? (T) Collections.unmodifiableSortedMap((SortedMap) sourceMap)
                : (T) Collections.unmodifiableMap(sourceMap);
        } else if (source instanceof Collection) {
            Collection sourceCollection = (Collection) source;
            LinkedHashSet tempSet = new LinkedHashSet<>(sourceCollection); // copy contents

            sourceCollection.clear();
            for (Object item : tempSet) {
                sourceCollection.add(protectCollectionX(item));
            }

            return sourceCollection instanceof List ? (T) Collections.unmodifiableList((List) sourceCollection)
                : sourceCollection instanceof SortedSet ? (T) Collections
                    .unmodifiableSortedSet((SortedSet) sourceCollection)
                    : sourceCollection instanceof Set ? (T) Collections.unmodifiableSet((Set) sourceCollection)
                        : (T) Collections.unmodifiableCollection(sourceCollection);
        } else if (source instanceof Freezable) {
            Freezable freezableSource = (Freezable) source;
            return (T) freezableSource.freeze();
        } else {
            throw new IllegalArgumentException("Can’t protect: " + source.getClass().toString());
        }
    }

    private static final Set<Object> KNOWN_IMMUTABLES = new HashSet<Object>(Arrays.asList(
        String.class));

    public static boolean isImmutable(Object source) {
        return source == null
            || source instanceof Enum
            || source instanceof Number
            || KNOWN_IMMUTABLES.contains(source.getClass());
    }

    /**
     * Clones T if we can; otherwise returns null.
     *
     * @param <T>
     * @param source
     * @return
     */
    @SuppressWarnings("unchecked")
    private static <T> T clone(T source) {
        final Class<? extends Object> class1 = source.getClass();
        try {
            final Method declaredMethod = class1.getDeclaredMethod("clone", (Class<?>) null);
            return (T) declaredMethod.invoke(source, (Object) null);
        } catch (Exception e) {
        }
        try {
            final Constructor<? extends Object> declaredMethod = class1.getConstructor((Class<?>) null);
            return (T) declaredMethod.newInstance((Object) null);
        } catch (Exception e) {
        }
        return null; // uncloneable
    }

    /**
     * Appends two strings, inserting separator if either is empty
     */
    public static String joinWithSeparation(String a, String separator, String b) {
        if (a.length() == 0) return b;
        if (b.length() == 0) return a;
        return a + separator + b;
    }

    /**
     * Appends two strings, inserting separator if either is empty. Modifies first map
     */
    public static Map<String, String> joinWithSeparation(Map<String, String> a, String separator, Map<String, String> b) {
        for (Iterator<String> it = b.keySet().iterator(); it.hasNext();) {
            String key = it.next();
            String bvalue = b.get(key);
            String avalue = a.get(key);
            if (avalue != null) {
                if (avalue.trim().equals(bvalue.trim())) continue;
                bvalue = joinWithSeparation(avalue, separator, bvalue);
            }
            a.put(key, bvalue);
        }
        return a;
    }

    public static <T> String join(Collection<T> c, String separator) {
        return join(c, separator, null);
    }

    public static String join(Object[] c, String separator) {
        return join(c, separator, null);
    }

    public static <T> String join(Collection<T> c, String separator, Transform<T, String> transform) {
        StringBuffer output = new StringBuffer();
        boolean isFirst = true;
        for (T item : c) {
            if (isFirst) {
                isFirst = false;
            } else {
                output.append(separator);
            }
            output.append(transform != null ? transform.transform(item) : item == null ? item : item.toString());
        }
        return output.toString();
    }

    public static <T> String join(T[] c, String separator, Transform<T, String> transform) {
        return join(Arrays.asList(c), separator, transform);
    }

    /**
     * Utility like Arrays.asList()
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> asMap(Object[][] source, Map<K, V> target, boolean reverse) {
        int from = 0, to = 1;
        if (reverse) {
            from = 1;
            to = 0;
        }
        for (int i = 0; i < source.length; ++i) {
            if (source[i].length != 2) {
                throw new IllegalArgumentException("Source must be array of pairs of strings: "
                    + Arrays.asList(source[i]));
            }
            target.put((K) source[i][from], (V) source[i][to]);
        }
        return target;
    }

    public static <K, V> Map<K, V> asMap(Object[][] source) {
        return asMap(source, new HashMap<K, V>(), false);
    }

    /**
     * Returns the canonical name for a file.
     */
    public static String getCanonicalName(String file) {
        try {
            return new File(file).getCanonicalPath();
        } catch (Exception e) {
            return file;
        }
    }

    /**
     * Convert a UnicodeSet into a string that can be embedded into a Regex. Handles strings that are in the UnicodeSet,
     * Supplementary ranges, and escaping
     *
     * @param source
     *            The source set
     * @param escaper
     *            A transliterator that is used to escape the characters according to the requirements of the regex.
     * @return
     */
    public static String toRegex(UnicodeSet source) {
        return toRegex(source, null, false);
    }

    private static final Transliterator DEFAULT_REGEX_ESCAPER = Transliterator.createFromRules(
        "foo",
        "([ \\- \\\\ \\[ \\] ]) > '\\' $1 ;"
            // + " ([:c:]) > &hex($1);"
            + " ([[:control:][[:z:]&[:ascii:]]]) > &hex($1);",
        Transliterator.FORWARD);

    /**
     * Convert a UnicodeSet into a string that can be embedded into a Regex.
     * Handles strings that are in the UnicodeSet, Supplementary ranges, and
     * escaping
     *
     * @param source
     *            The source set
     * @param escaper
     *            A transliterator that is used to escape the characters according
     *            to the requirements of the regex. The default puts a \\ before [, -,
     *            \, and ], and converts controls and Ascii whitespace to hex.
     *            Alternatives can be supplied. Note that some Regex engines,
     *            including Java 1.5, don't really deal with escaped supplementaries
     *            well.
     * @param onlyBmp
     *            Set to true if the Regex only accepts BMP characters. In that
     *            case, ranges of supplementary characters are converted to lists of
     *            ranges. For example, [\uFFF0-\U0010000F \U0010100F-\U0010300F]
     *            converts into:
     *
     *            <pre>
     *          [\uD800][\uDC00-\uDFFF]
     *          [\uD801-\uDBBF][\uDC00-\uDFFF]
     *          [\uDBC0][\uDC00-\uDC0F]
     * </pre>
     *
     *            and
     *
     *            <pre>
     *          [\uDBC4][\uDC0F-\uDFFF]
     *          [\uDBC5-\uDBCB][\uDC00-\uDFFF]
     *          [\uDBCC][\uDC00-\uDC0F]
     * </pre>
     *
     *            These are then coalesced into a list of alternatives by sharing
     *            parts where feasible. For example, the above turns into 3 pairs of ranges:
     *
     *            <pre>
     *          [\uDBC0\uDBCC][\uDC00-\uDC0F]|\uDBC4[\uDC0F-\uDFFF]|[\uD800-\uDBBF\uDBC5-\uDBCB][\uDC00-\uDFFF]
     * </pre>
     *
     * @return escaped string. Something like [a-z] or (?:[a-m]|{zh}) if there is
     *         a string zh in the set, or a more complicated case for
     *         supplementaries. <br>
     *         Special cases: [] returns "", single item returns a string
     *         (escaped), like [a] => "a", or [{abc}] => "abc"<br>
     *         Supplementaries are handled specially, as described under onlyBmp.
     */
    public static String toRegex(UnicodeSet source, Transliterator escaper, boolean onlyBmp) {
        if (escaper == null) {
            escaper = DEFAULT_REGEX_ESCAPER;
        }
        UnicodeSetIterator it = new UnicodeSetIterator(source);
        // if there is only one item, return it
        if (source.size() == 0) {
            return "";
        }
        if (source.size() == 1) {
            it.next();
            return escaper.transliterate(it.getString());
        }
        // otherwise, we figure out what is in the set, and will return
        StringBuilder base = new StringBuilder("[");
        StringBuilder alternates = new StringBuilder();
        Map<UnicodeSet, UnicodeSet> lastToFirst = new TreeMap<UnicodeSet, UnicodeSet>(new UnicodeSetComparator());
        int alternateCount = 0;
        while (it.nextRange()) {
            if (it.codepoint == UnicodeSetIterator.IS_STRING) {
                ++alternateCount;
                alternates.append('|').append(escaper.transliterate(it.string));
            } else if (!onlyBmp || it.codepointEnd <= 0xFFFF) { // BMP
                addBmpRange(it.codepoint, it.codepointEnd, escaper, base);
            } else { // supplementary
                if (it.codepoint <= 0xFFFF) {
                    addBmpRange(it.codepoint, 0xFFFF, escaper, base);
                    it.codepoint = 0x10000; // reset the range
                }
                // this gets a bit ugly; we are trying to minimize the extra ranges for supplementaries
                // we do this by breaking up X-Y based on the Lead and Trail values for X and Y
                // Lx [Tx - Ty]) (if Lx == Ly)
                // Lx [Tx - DFFF] | Ly [DC00-Ty] (if Lx == Ly - 1)
                // Lx [Tx - DFFF] | [Lx+1 - Ly-1][DC00-DFFF] | Ly [DC00-Ty] (otherwise)
                int leadX = UTF16.getLeadSurrogate(it.codepoint);
                int trailX = UTF16.getTrailSurrogate(it.codepoint);
                int leadY = UTF16.getLeadSurrogate(it.codepointEnd);
                int trailY = UTF16.getTrailSurrogate(it.codepointEnd);
                if (leadX == leadY) {
                    addSupplementalRange(leadX, leadX, trailX, trailY, escaper, lastToFirst);
                } else {
                    addSupplementalRange(leadX, leadX, trailX, 0xDFFF, escaper, lastToFirst);
                    if (leadX != leadY - 1) {
                        addSupplementalRange(leadX + 1, leadY - 1, 0xDC00, 0xDFFF, escaper, lastToFirst);
                    }
                    addSupplementalRange(leadY, leadY, 0xDC00, trailY, escaper, lastToFirst);
                }
            }
        }
        // add in the supplementary ranges
        if (lastToFirst.size() != 0) {
            for (UnicodeSet last : lastToFirst.keySet()) {
                ++alternateCount;
                alternates.append('|').append(toRegex(lastToFirst.get(last), escaper, onlyBmp))
                    .append(toRegex(last, escaper, onlyBmp));
            }
        }
        // Return the output. We separate cases in order to get the minimal extra apparatus
        base.append("]");
        if (alternateCount == 0) {
            return base.toString();
        } else if (base.length() > 2) {
            return "(?:" + base + "|" + alternates.substring(1) + ")";
        } else if (alternateCount == 1) {
            return alternates.substring(1);
        } else {
            return "(?:" + alternates.substring(1) + ")";
        }
    }

    private static void addSupplementalRange(int leadX, int leadY, int trailX, int trailY, Transliterator escaper,
        Map<UnicodeSet, UnicodeSet> lastToFirst) {
        System.out.println("\tadding: " + new UnicodeSet(leadX, leadY) + "\t" + new UnicodeSet(trailX, trailY));
        UnicodeSet last = new UnicodeSet(trailX, trailY);
        UnicodeSet first = lastToFirst.get(last);
        if (first == null) {
            lastToFirst.put(last, first = new UnicodeSet());
        }
        first.add(leadX, leadY);
    }

    private static void addBmpRange(int start, int limit, Transliterator escaper, StringBuilder base) {
        base.append(escaper.transliterate(UTF16.valueOf(start)));
        if (start != limit) {
            base.append("-").append(escaper.transliterate(UTF16.valueOf(limit)));
        }
    }

    public static class UnicodeSetComparator implements Comparator<UnicodeSet> {
        public int compare(UnicodeSet o1, UnicodeSet o2) {
            return o1.compareTo(o2);
        }
    }

    public static class CollectionComparator<T extends Comparable<T>> implements Comparator<Collection<T>> {
        public int compare(Collection<T> o1, Collection<T> o2) {
            return UnicodeSet.compare(o1, o2, UnicodeSet.ComparisonStyle.SHORTER_FIRST);
        }
    }

    public static class ComparableComparator<T extends Comparable<T>> implements Comparator<T> {
        public int compare(T arg0, T arg1) {
            return Utility.checkCompare(arg0, arg1);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void addTreeMapChain(Map coverageData, Object... objects) {
        Map<Object, Object> base = coverageData;
        for (int i = 0; i < objects.length - 2; ++i) {
            Map<Object, Object> nextOne = (Map<Object, Object>) base.get(objects[i]);
            if (nextOne == null) base.put(objects[i], nextOne = new TreeMap<Object, Object>());
            base = nextOne;
        }
        base.put(objects[objects.length - 2], objects[objects.length - 1]);
    }

    public static abstract class CollectionTransform<S, T> implements Transform<S, T> {
        public abstract T transform(S source);

        public Collection<T> transform(Collection<S> input, Collection<T> output) {
            return CldrUtility.transform(input, this, output);
        }

        public Collection<T> transform(Collection<S> input) {
            return transform(input, new ArrayList<T>());
        }
    }

    public static <S, T, SC extends Collection<S>, TC extends Collection<T>> TC transform(SC source, Transform<S, T> transform, TC target) {
        for (S sourceItem : source) {
            T targetItem = transform.transform(sourceItem);
            if (targetItem != null) {
                target.add(targetItem);
            }
        }
        return target;
    }

    public static <SK, SV, TK, TV, SM extends Map<SK, SV>, TM extends Map<TK, TV>> TM transform(
        SM source, Transform<SK, TK> transformKey, Transform<SV, TV> transformValue, TM target) {
        for (Entry<SK, SV> sourceEntry : source.entrySet()) {
            TK targetKey = transformKey.transform(sourceEntry.getKey());
            TV targetValue = transformValue.transform(sourceEntry.getValue());
            if (targetKey != null && targetValue != null) {
                target.put(targetKey, targetValue);
            }
        }
        return target;
    }

    public static abstract class Apply<T> {
        public abstract void apply(T item);

        public <U extends Collection<T>> void applyTo(U collection) {
            for (T item : collection) {
                apply(item);
            }
        }
    }

    public static abstract class Filter<T> {

        public abstract boolean contains(T item);

        public <U extends Collection<T>> U retainAll(U c) {
            for (Iterator<T> it = c.iterator(); it.hasNext();) {
                if (!contains(it.next())) it.remove();
            }
            return c;
        }

        public <U extends Collection<T>> U extractMatches(U c, U target) {
            for (Iterator<T> it = c.iterator(); it.hasNext();) {
                T item = it.next();
                if (contains(item)) {
                    target.add(item);
                }
            }
            return target;
        }

        public <U extends Collection<T>> U removeAll(U c) {
            for (Iterator<T> it = c.iterator(); it.hasNext();) {
                if (contains(it.next())) it.remove();
            }
            return c;
        }

        public <U extends Collection<T>> U extractNonMatches(U c, U target) {
            for (Iterator<T> it = c.iterator(); it.hasNext();) {
                T item = it.next();
                if (!contains(item)) {
                    target.add(item);
                }
            }
            return target;
        }
    }

    public static class MatcherFilter<T> extends Filter<T> {
        private Matcher matcher;

        public MatcherFilter(String pattern) {
            this.matcher = PatternCache.get(pattern).matcher("");
        }

        public MatcherFilter(Matcher matcher) {
            this.matcher = matcher;
        }

        public MatcherFilter<T> set(Matcher matcher) {
            this.matcher = matcher;
            return this;
        }

        public MatcherFilter<T> set(String pattern) {
            this.matcher = PatternCache.get(pattern).matcher("");
            return this;
        }

        public boolean contains(T o) {
            return matcher.reset(o.toString()).matches();
        }
    }

    // static final class HandlingTransform implements Transform<String, Handling> {
    // @Override
    // public Handling transform(String source) {
    // return Handling.valueOf(source);
    // }
    // }

    public static final class PairComparator<K extends Comparable<K>, V extends Comparable<V>> implements java.util.Comparator<Pair<K, V>> {

        private Comparator<K> comp1;
        private Comparator<V> comp2;

        public PairComparator(Comparator<K> comp1, Comparator<V> comp2) {
            this.comp1 = comp1;
            this.comp2 = comp2;
        }

        @Override
        public int compare(Pair<K, V> o1, Pair<K, V> o2) {
            {
                K o1First = o1.getFirst();
                K o2First = o2.getFirst();
                int diff = o1First == null ? (o2First == null ? 0 : -1)
                    : o2First == null ? 1
                        : comp1 == null ? o1First.compareTo(o2First)
                            : comp1.compare(o1First, o2First);
                if (diff != 0) {
                    return diff;
                }
            }
            V o1Second = o1.getSecond();
            V o2Second = o2.getSecond();
            return o1Second == null ? (o2Second == null ? 0 : -1)
                : o2Second == null ? 1
                    : comp2 == null ? o1Second.compareTo(o2Second)
                        : comp2.compare(o1Second, o2Second);
        }

    }

    /**
     * Fetch data from jar
     *
     * @param name
     *            a name residing in the org/unicode/cldr/util/data/ directory, or loading from a jar will break.
     */
    public static BufferedReader getUTF8Data(String name) {
        if (new File(name).isAbsolute()) {
            throw new IllegalArgumentException(
                "Path must be relative to org/unicode/cldr/util/data  such as 'file.txt' or 'casing/file.txt', but got '"
                    + name + "'.");
        }

        return FileReaders.openFile(CldrUtility.class, "data/" + name);
    }

    /**
     * Fetch data from jar
     *
     * @param name
     *            a name residing in the org/unicode/cldr/util/data/ directory, or loading from a jar will break.
     */
    public static InputStream getInputStream(String name) {
        if (new File(name).isAbsolute()) {
            throw new IllegalArgumentException(
                "Path must be relative to org/unicode/cldr/util/data  such as 'file.txt' or 'casing/file.txt', but got '"
                    + name + "'.");
        }
        return getInputStream(CldrUtility.class, "data/" + name);
    }

    @SuppressWarnings("resource")
    public static InputStream getInputStream(Class<?> callingClass, String relativePath) {
        InputStream is = callingClass.getResourceAsStream(relativePath);
        // add buffering
        return InputStreamFactory.buffer(is);
    }

    /**
     * Takes a Map that goes from Object to Set, and fills in the transpose
     *
     * @param source_key_valueSet
     * @param output_value_key
     */
    public static void putAllTransposed(Map<Object, Set<Object>> source_key_valueSet, Map<Object, Object> output_value_key) {
        for (Iterator<Object> it = source_key_valueSet.keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            Set<Object> values = source_key_valueSet.get(key);
            for (Iterator<Object> it2 = values.iterator(); it2.hasNext();) {
                Object value = it2.next();
                output_value_key.put(value, key);
            }
        }
    }

    public static int countInstances(String source, String substring) {
        int count = 0;
        int pos = 0;
        while (true) {
            pos = source.indexOf(substring, pos) + 1;
            if (pos <= 0) break;
            count++;
        }
        return count;
    }

    public static void registerTransliteratorFromFile(String id, String dir, String filename) {
        registerTransliteratorFromFile(id, dir, filename, Transliterator.FORWARD, true);
        registerTransliteratorFromFile(id, dir, filename, Transliterator.REVERSE, true);
    }

    public static void registerTransliteratorFromFile(String id, String dir, String filename, int direction,
        boolean reverseID) {
        if (filename == null) {
            filename = id.replace('-', '_');
            filename = filename.replace('/', '_');
            filename += ".txt";
        }
        String rules = getText(dir, filename);
        Transliterator t;
        int pos = id.indexOf('-');
        String rid;
        if (pos < 0) {
            rid = id + "-Any";
            id = "Any-" + id;
        } else {
            rid = id.substring(pos + 1) + "-" + id.substring(0, pos);
        }
        if (!reverseID) rid = id;

        if (direction == Transliterator.FORWARD) {
            Transliterator.unregister(id);
            t = Transliterator.createFromRules(id, rules, Transliterator.FORWARD);
            Transliterator.registerInstance(t);
            System.out.println("Registered new Transliterator: " + id);
        }

        /*
         * String test = "\u049A\u0430\u0437\u0430\u049B";
         * System.out.println(t.transliterate(test));
         * t = Transliterator.getInstance(id);
         * System.out.println(t.transliterate(test));
         */

        if (direction == Transliterator.REVERSE) {
            Transliterator.unregister(rid);
            t = Transliterator.createFromRules(rid, rules, Transliterator.REVERSE);
            Transliterator.registerInstance(t);
            System.out.println("Registered new Transliterator: " + rid);
        }
    }

    public static String getText(String dir, String filename) {
        try {
            BufferedReader br = FileUtilities.openUTF8Reader(dir, filename);
            StringBuffer buffer = new StringBuffer();
            while (true) {
                String line = br.readLine();
                if (line == null) break;
                if (line.length() > 0 && line.charAt(0) == '\uFEFF') line = line.substring(1);
                if (line.startsWith("//")) continue;
                buffer.append(line).append(CldrUtility.LINE_SEPARATOR);
            }
            br.close();
            String rules = buffer.toString();
            return rules;
        } catch (IOException e) {
            throw (IllegalArgumentException) new IllegalArgumentException("Can't open " + dir + ", " + filename)
                .initCause(e);
        }
    }

    public static void callMethod(String methodNames, Class<?> cls) {
        for (String methodName : methodNames.split(",")) {
            try {
                Method method;
                try {
                    method = cls.getMethod(methodName, (Class[]) null);
                    try {
                        method.invoke(null, (Object[]) null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    System.out.println("No such method: " + methodName);
                    showMethods(cls);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static void showMethods(Class<?> cls) throws ClassNotFoundException {
        System.out.println("Possible methods of " + cls.getCanonicalName() + " are: ");
        Method[] methods = cls.getMethods();
        Set<String> names = new TreeSet<String>();
        for (int i = 0; i < methods.length; ++i) {
            if (methods[i].getGenericParameterTypes().length != 0) continue;
            //int mods = methods[i].getModifiers();
            // if (!Modifier.isStatic(mods)) continue;
            String name = methods[i].getName();
            names.add(name);
        }
        for (Iterator<String> it = names.iterator(); it.hasNext();) {
            System.out.println("\t" + it.next());
        }
    }

    /**
     * Breaks lines if they are too long, or if matcher.group(1) != last. Only breaks just before matcher.
     *
     * @param input
     * @param separator
     * @param matcher
     *            must match each possible item. The first group is significant; if different, will cause break
     * @return
     */
    static public String breakLines(CharSequence input, String separator, Matcher matcher, int width) {
        StringBuffer output = new StringBuffer();
        String lastPrefix = "";
        int lastEnd = 0;
        int lastBreakPos = 0;
        matcher.reset(input);
        while (true) {
            boolean match = matcher.find();
            if (!match) {
                output.append(input.subSequence(lastEnd, input.length()));
                break;
            }
            String prefix = matcher.group(1);
            if (!prefix.equalsIgnoreCase(lastPrefix) || matcher.end() - lastBreakPos > width) { // break before?
                output.append(separator);
                lastBreakPos = lastEnd;
            } else if (lastEnd != 0) {
                output.append(' ');
            }
            output.append(input.subSequence(lastEnd, matcher.end()).toString().trim());
            lastEnd = matcher.end();
            lastPrefix = prefix;
        }
        return output.toString();
    }

    public static void showOptions(String[] args) {
        // Properties props = System.getProperties();
        System.out.println("Arguments: " + join(args, " ")); // + (props == null ? "" : " " + props));
    }

    public static double roundToDecimals(double input, int places) {
        double log10 = Math.log10(input); // 15000 => 4.xxx
        double intLog10 = Math.floor(log10);
        double scale = Math.pow(10, intLog10 - places + 1);
        double factored = Math.round(input / scale) * scale;
        // System.out.println("###\t" +input + "\t" + factored);
        return factored;
    }

    /**
     * Get a property value, returning the value if there is one (eg -Dkey=value),
     * otherwise the default value (for either empty or null).
     *
     * @param key
     * @param valueIfNull
     * @param valueIfEmpty
     * @return
     */
    public static String getProperty(String key, String defaultValue) {
        return getProperty(key, defaultValue, defaultValue);
    }

    /**
     * Get a property value, returning the value if there is one, otherwise null.
     */
    public static String getProperty(String key) {
        return getProperty(key, null, null);
    }

    /**
     * Get a property value, returning the value if there is one (eg -Dkey=value),
     * the valueIfEmpty if there is one with no value (eg -Dkey) and the valueIfNull
     * if there is no property.
     *
     * @param key
     * @param valueIfNull
     * @param valueIfEmpty
     * @return
     */
    public static String getProperty(String key, String valueIfNull, String valueIfEmpty) {
        String result = CLDRConfig.getInstance().getProperty(key);
        if (result == null) {
            result = valueIfNull;
        } else if (result.length() == 0) {
            result = valueIfEmpty;
        }
        return result;
    }

    public static String hex(byte[] bytes, int start, int end, String separator) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < end; ++i) {
            if (result.length() != 0) {
                result.append(separator);
            }
            result.append(Utility.hex(bytes[i] & 0xFF, 2));
        }
        return result.toString();
    }

    public static boolean getProperty(String string, boolean b) {
        return getProperty(string, b ? "true" : "false", "true").matches("(?i)T|TRUE");
    }

    public static String checkValidDirectory(String sourceDirectory) {
        return checkValidFile(sourceDirectory, true, null);
    }

    public static String checkValidDirectory(String sourceDirectory, String correction) {
        return checkValidFile(sourceDirectory, true, correction);
    }

    public static String checkValidFile(String sourceDirectory, boolean checkForDirectory, String correction) {
        File file = null;
        String canonicalPath = null;
        try {
            file = new File(sourceDirectory);
            canonicalPath = file.getCanonicalPath() + File.separatorChar;
        } catch (Exception e) {
        }
        if (file == null || canonicalPath == null || checkForDirectory && !file.isDirectory()) {
            throw new RuntimeException("Directory not found: " + sourceDirectory
                + (canonicalPath == null ? "" : " => " + canonicalPath)
                + (correction == null ? "" : CldrUtility.LINE_SEPARATOR + correction));
        }
        return canonicalPath;
    }

    /**
     * Copy up to matching line (not included). If output is null, then just skip until.
     *
     * @param oldFile
     *            file to copy
     * @param readUntilPattern
     *            pattern to search for. If null, goes to end of file.
     * @param output
     *            into to copy into. If null, just skips in the input.
     * @param includeMatchingLine
     *            inclde the matching line when copying.
     * @throws IOException
     */
    public static void copyUpTo(BufferedReader oldFile, final Pattern readUntilPattern,
        final PrintWriter output, boolean includeMatchingLine) throws IOException {
        Matcher readUntil = readUntilPattern == null ? null : readUntilPattern.matcher("");
        while (true) {
            String line = oldFile.readLine();
            if (line == null) {
                break;
            }
            if (line.startsWith("\uFEFF")) {
                line = line.substring(1);
            }
            if (readUntil != null && readUntil.reset(line).matches()) {
                if (includeMatchingLine && output != null) {
                    output.println(line);
                }
                break;
            }
            if (output != null) {
                output.println(line);
            }
        }
    }

    private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'GMT'");
    private static DateFormat DATE_ONLY = new SimpleDateFormat("yyyy-MM-dd");
    static {
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        DATE_ONLY.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public static String isoFormat(Date date) {
        synchronized (df) {
            return df.format(date);
        }
    }

    public static String isoFormatDateOnly(Date date) {
        synchronized (DATE_ONLY) {
            return DATE_ONLY.format(date);
        }
    }

    public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap() {
        // http://ria101.wordpress.com/2011/12/12/concurrenthashmap-avoid-a-common-misuse/
        return new ConcurrentHashMap<K, V>(4, 0.9f, 1);
    }

    public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap(Map<K, V> source) {
        ConcurrentHashMap<K, V> result = newConcurrentHashMap();
        result.putAll(source);
        return result;
    }

    public static boolean equals(Object a, Object b) {
        return a == b ? true
            : a == null || b == null ? false
                : a.equals(b);
    }

    public static String getDoubleLink(String code) {
        final String anchorSafe = TransliteratorUtilities.toHTML.transliterate(code).replace(" ", "_");
        return "<a name='" + anchorSafe + "' href='#" + anchorSafe + "'>";
    }

    public static String getDoubleLinkedText(String anchor, String anchorText) {
        return getDoubleLink(anchor) + TransliteratorUtilities.toHTML.transliterate(anchorText).replace("_", " ")
            + "</a>";
    }

    public static String getDoubleLinkedText(String anchor) {
        return getDoubleLinkedText(anchor, anchor);
    }

    public static String getDoubleLinkMsg() {
        return "<a name=''{0}'' href=''#{0}''>{0}</a>";
    }

    public static String getDoubleLinkMsg2() {
        return "<a name=''{0}{1}'' href=''#{0}{1}''>{0}</a>";
    }

    public static String getCopyrightString() {
        // now do the rest
        return "Copyright \u00A9 1991-" + Calendar.getInstance().get(Calendar.YEAR) + " Unicode, Inc." + CldrUtility.LINE_SEPARATOR
            + "For terms of use, see http://www.unicode.org/copyright.html" + CldrUtility.LINE_SEPARATOR
            + "Unicode and the Unicode Logo are registered trademarks of Unicode, Inc. in the U.S. and other countries." + CldrUtility.LINE_SEPARATOR
            + "CLDR data files are interpreted according to the LDML specification " + "(http://unicode.org/reports/tr35/)";
    }

    // TODO Move to collection utilities
    /**
     * Type-safe get
     * @param map
     * @param key
     * @return value
     */
    public static <K, V, M extends Map<K, V>> V get(M map, K key) {
        return map.get(key);
    }

    /**
     * Type-safe contains
     * @param map
     * @param key
     * @return value
     */
    public static <K, C extends Collection<K>> boolean contains(C collection, K key) {
        return collection.contains(key);
    }

    public static <E extends Enum<E>> EnumSet<E> toEnumSet(Class<E> classValue, Collection<String> stringValues) {
        EnumSet<E> result = EnumSet.noneOf(classValue);
        for (String s : stringValues) {
            result.add(Enum.valueOf(classValue, s));
        }
        return result;
    }

    public static <K, V, M extends Map<K, V>> M putNew(M map, K key, V value) {
        if (!map.containsKey(key)) {
            map.put(key, value);
        }
        return map;
    }

    public static String[] cleanSemiFields(String line) {
        line = cleanLine(line);
        return line.isEmpty() ? null : SEMI_SPLIT.split(line);
    }

    private static String cleanLine(String line) {
        int comment = line.indexOf("#");
        if (comment >= 0) {
            line = line.substring(0, comment);
        }
        if (line.startsWith("\uFEFF")) {
            line = line.substring(1);
        }
        return line.trim();
    }

    public static void handleFile(String filename, LineHandler handler) throws IOException {
        try (BufferedReader in = getUTF8Data(filename);) {
            String line = null;
            while ((line = in.readLine()) != null) {
                //                String line = in.readLine();
                //                if (line == null) {
                //                    break;
                //                }
                try {
                    if (!handler.handle(line)) {
                        if (HANDLEFILE_SHOW_SKIP) {
                            System.out.println("Skipping line: " + line);
                        }
                    }
                } catch (Exception e) {
                    throw (RuntimeException) new IllegalArgumentException("Problem with line: " + line)
                        .initCause(e);
                }
            }
        }
        //        in.close();
    }

    public static <T> T ifNull(T x, T y) {
        return x == null
            ? y
            : x;
    }

    public static <T> T ifSame(T source, T replaceIfSame, T replacement) {
        return source == replaceIfSame ? replacement : source;
    }

    public static <T> T ifEqual(T source, T replaceIfSame, T replacement) {
        return Objects.equals(source, replaceIfSame) ? replacement : source;
    }

    public static <T> Set<T> intersect(Set<T> a, Collection<T> b) {
        Set<T> result = new LinkedHashSet<>(a);
        result.retainAll(b);
        return result;
    }

    public static <T> Set<T> subtract(Set<T> a, Collection<T> b) {
        Set<T> result = new LinkedHashSet<>(a);
        result.removeAll(b);
        return result;
    }

    public static <T> void logRegexLookup(TestFmwk testFramework, RegexLookup<T> lookup, String toLookup) {
        Output<String[]> arguments = new Output<>();
        Output<Finder> matcherFound = new Output<>();
        List<String> failures = new ArrayList<String>();
        lookup.get(toLookup, null, arguments, matcherFound, failures);
        testFramework.logln("lookup arguments: " + (arguments.value == null ? "null" : Arrays.asList(arguments.value)));
        testFramework.logln("lookup matcherFound: " + matcherFound);
        for (String s : failures) {
            testFramework.logln(s);
        }
    }

    public static boolean deepEquals(Object... pairs) {
        for (int item = 0; item < pairs.length;) {
            if (!Objects.deepEquals(pairs[item++], pairs[item++])) {
                return false;
            }
        }
        return true;
    }

    public static String[] array(Splitter splitter, String source) {
        List<String> list = splitter.splitToList(source);
        return list.toArray(new String[list.size()]);
    }

    public static String toHex(String in, boolean javaStyle) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < in.length(); ++i) {
            result.append(toHex(in.charAt(i), javaStyle));
        }
        return result.toString();
    }

    public static String toHex(int j, boolean javaStyle) {
        if (j == '\"') {
            return "\\\"";
        } else if (j == '\\') {
            return "\\\\";
        } else if (0x20 < j && j < 0x7F) {
            return String.valueOf((char) j);
        }
        final String hexString = Integer.toHexString(j).toUpperCase();
        int gap = 4 - hexString.length();
        if (gap < 0) {
            gap = 0;
        }
        String prefix = javaStyle ? "\\u" : "U+";
        return prefix + "000".substring(0, gap) + hexString;
    }

    /**
     * get string format for debugging, since Java has a useless display for many items
     * @param item
     * @return
     */
    public static String toString(Object item) {
        if (item instanceof Object[]) {
            return toString(Arrays.asList((Object[]) item));
        } else if (item instanceof Entry) {
            return toString(((Entry) item).getKey()) + "≔" + toString(((Entry) item).getValue());
        } else if (item instanceof Map) {
            return "{" + toString(((Map) item).entrySet()) + "}";
        } else if (item instanceof Collection) {
            List<String> result = new ArrayList<>();
            for (Object subitem : (Collection) item) {
                result.add(toString(subitem));
            }
            return result.toString();
        }
        return item.toString();
    }
}
