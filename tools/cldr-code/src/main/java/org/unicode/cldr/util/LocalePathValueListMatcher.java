package org.unicode.cldr.util;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class LocalePathValueListMatcher {

    private static final Splitter SPLIT_SEMI_COLON = Splitter.on(';');

    public static class LocalePathValueMatcher {
        final Pattern localePattern;
        final Pattern pathPattern;
        final Pattern valuePattern;

        public LocalePathValueMatcher(List<String> parts) {
            localePattern =
                    parts.size() < 1 || parts.get(0).isEmpty()
                            ? null
                            : Pattern.compile(parts.get(0));
            pathPattern =
                    parts.size() < 2 || parts.get(1).isEmpty()
                            ? null
                            : Pattern.compile(parts.get(1).replace("[@", "\\[@"));
            valuePattern =
                    parts.size() < 3 || parts.get(1).isEmpty()
                            ? null
                            : Pattern.compile(parts.get(2));
        }

        public boolean lookingAt(String locale, String path, String value) {
            return (localePattern == null || localePattern.matcher(locale).lookingAt())
                    && (pathPattern == null || pathPattern.matcher(path).lookingAt())
                    && (valuePattern == null || valuePattern.matcher(value).lookingAt());
        }

        @Override
        public String toString() {
            return String.format("%s\t%s\t%s", localePattern, pathPattern, valuePattern);
        }
    }

    final List<LocalePathValueListMatcher.LocalePathValueMatcher> matchData;

    public LocalePathValueListMatcher(
            List<LocalePathValueListMatcher.LocalePathValueMatcher> _matchData) {
        matchData = ImmutableList.copyOf(_matchData);
    }

    public static LocalePathValueListMatcher load(Path path) {
        try {
            return load(Files.lines(path));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static LocalePathValueListMatcher load(Stream<String> lines) {
        List<LocalePathValueListMatcher.LocalePathValueMatcher> _matchData = new ArrayList<>();
        lines.forEach(line -> load(line, _matchData));
        return new LocalePathValueListMatcher(_matchData);
    }

    public boolean lookingAt(String locale, String path, String value) {
        for (LocalePathValueListMatcher.LocalePathValueMatcher lpv : matchData) {
            if (!lpv.lookingAt(locale, path, value)) {
                return false;
            }
        }
        return true;
    }

    private static void load(
            String line, List<LocalePathValueListMatcher.LocalePathValueMatcher> _matchData) {
        line = line.trim();
        if (line.startsWith("#")) {
            return;
        }
        _matchData.add(
                new LocalePathValueMatcher(SPLIT_SEMI_COLON.trimResults().splitToList(line)));
    }
}
