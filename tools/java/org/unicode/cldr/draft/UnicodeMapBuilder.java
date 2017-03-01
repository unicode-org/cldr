package org.unicode.cldr.draft;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Pattern;

import org.unicode.cldr.util.PatternCache;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;

public class UnicodeMapBuilder<T> {
    public enum Leniency {
        allowChars, allowUnicodeSet
    }

    UnicodeMap<T> result;
    Parser<T, String> parser;
    Leniency leniency;
    Pattern semi = PatternCache.get("\\s+;\\s+");

    // Matcher semi = PatternCache.get("\\s+;\\s+").matcher("");

    public UnicodeMapBuilder() {
    }

    public UnicodeMapBuilder<T> setParser(Parser<T, String> parser) {
        this.parser = parser;
        return this;
    }

    public Parser<T, String> getParser() {
        return parser;
    }

    public Leniency getLeniency() {
        return leniency;
    }

    public UnicodeMapBuilder<T> setLeniency(Leniency leniency) {
        this.leniency = leniency;
        return this;
    }

    public UnicodeMap<T> get() {
        return result;
    }

    public UnicodeMap<T> getFrozen() {
        UnicodeMap<T> myResult = result.freeze();
        result = null;
        return myResult;
    }

    public UnicodeMapBuilder<T> putFromLines(BufferedReader br) {
        if (result == null) {
            result = new UnicodeMap<T>();
        }
        UnicodeSet sources = new UnicodeSet();
        String line = null;
        try {
            while (true) {
                line = readDataLine(br, null);
                if (line == null) {
                    break;
                }
                if (line.length() == 0) {
                    continue;
                }
                sources.clear();
                final String[] pieces = semi.split(line);
                if (pieces.length < 2) {
                    throw new IllegalArgumentException("Line must be of form code ; value");
                }
                final String codelist = pieces[0].trim();
                final String valueString = pieces[1].trim();
                if (UnicodeSet.resemblesPattern(pieces[0], 0)) {
                    sources = new UnicodeSet(codelist);
                } else if (codelist.length() < 4) {
                    sources.add(codelist);
                } else {
                    final String[] codes = codelist.split("\\s+");
                    for (int i = 0; i < codes.length; ++i) {
                        final String[] range = codes[i].split("\\.\\.");
                        final int start = getCodePoint(range[0]);
                        int end = start;
                        if (range.length > 1) {
                            if (range.length > 2) {
                                throw new IllegalArgumentException("Too many ..");
                            }
                            end = getCodePoint(range[1]);
                            if (start >= end) {
                                throw new IllegalArgumentException("Range out of order");
                            }
                        }
                        sources.add(start, end);
                    }
                }
                T value = parser == null ? (T) valueString : parser.parseObject(valueString);
                result.putAll(sources, value);
            }
            br.close();
        } catch (final Exception e) {
            throw (RuntimeException) new RuntimeException("Failure on line " + line).initCause(e);
        }
        return this;
    }

    private int getCodePoint(String source) {
        if (source.startsWith("U+") || source.startsWith("\\u") || source.startsWith("\\U")) {
            source = source.substring(2);
        }
        return Integer.parseInt(source, 16);
    }

    public static String readDataLine(BufferedReader br, int[] count) throws IOException {
        String originalLine = "";
        String line = "";

        try {
            line = originalLine = br.readLine();
            if (line == null) {
                return null;
            }
            if (count != null) {
                ++count[0];
            }
            if (line.length() > 0 && line.charAt(0) == 0xFEFF) {
                line = line.substring(1);
            }
            final int commentPos = line.indexOf('#');
            if (commentPos >= 0) {
                line = line.substring(0, commentPos);
            }
            line = line.trim();
        } catch (final Exception e) {
            throw new ICUUncheckedIOException("Line \"{" + originalLine + "}\",  \"{" + line + "}\"", e);
        }
        return line;
    }

}
