package org.unicode.cldr.tool;

import com.google.common.collect.Sets;
import com.ibm.icu.util.Output;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.Factory;
import org.unicode.cldr.util.RegexLookup.Finder;

public class ReviewRegexLookup {
    private static final boolean DEBUG = false;
    static CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();
    private static final org.unicode.cldr.util.Factory CLDR_FACTORY = CLDR_CONFIG.getCldrFactory();

    public static void main(String[] args) {
        TreeMap<String, String> matchersFound = new TreeMap<>();
        int skipped = 0;
        Factory phf = PathHeader.getFactory();
        Set<String> paths = new TreeSet<>();
        paths.addAll(Sets.newHashSet(CLDR_CONFIG.getEnglish().iterator()));
        paths.addAll(Sets.newHashSet(CLDR_FACTORY.make("zh", true).iterator()));
        for (String path : CLDR_CONFIG.getEnglish()) {
            if (path.endsWith("/alias") || path.contains("//supplementalData")) {
                continue;
            }
            List<String> failures = new ArrayList<>();
            Output<Finder> matcher = new Output<>();
            PathHeader ph = phf.fromPathInternal(path, matcher, failures);
            if (matchersFound.containsKey(matcher.value.toString())) {
                skipped++;
                continue;
            }
            matchersFound.put(matcher.value.toString(), path);
            if (DEBUG) System.out.println(path + "\t" + ph);
        }
        System.out.println("shared: " + skipped);
        Set<String> unmatchedRegexes = phf.getUnmatchedRegexes();
        System.out.println("\nUnmatched Path Header regexes: " + unmatchedRegexes.size() + "\n");
        unmatchedRegexes.stream()
                .filter(x -> !(x.contains("//supplementalData") || x.contains("//ldmlBCP47")))
                .forEach(System.out::println);
        System.out.println("\nMatched Path Header regexes: " + matchersFound.size() + "\n");
        matchersFound.entrySet().stream()
                .forEach(x -> System.out.println(x.getKey() + "\t sample:\t" + x.getValue()));
    }
}
