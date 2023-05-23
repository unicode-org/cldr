package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.ibm.icu.impl.UnicodeRegex;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Supports a list of regex expressions, separated by §§. Each expression can be prefixed by !,
 * which inverts the find/match operation.
 *
 * @author markdavis
 */
public final class MatcherList {

    private static final Joiner MULTI_JOINER = Joiner.on("§§");
    private static final Splitter MULTI_SPLITTER = Splitter.on("§§");

    private static final class MatcherIncludeExclude {
        final Matcher matcher;
        final boolean include;

        private MatcherIncludeExclude(Matcher matcher, boolean include) {
            this.matcher = matcher;
            this.include = include;
        }

        @Override
        public String toString() {
            return (include ? "" : "!") + matcher.pattern().toString();
        }
    }

    private final List<MatcherList.MatcherIncludeExclude> list;

    private MatcherList(List<MatcherList.MatcherIncludeExclude> list) {
        this.list = ImmutableList.copyOf(list);
    }

    public static MatcherList from(String property) {
        if (property == null) {
            return null;
        }
        List<MatcherList.MatcherIncludeExclude> _result = new ArrayList<>();
        for (String item : MULTI_SPLITTER.split(property)) {
            boolean include = true;
            if (item.startsWith("!")) {
                include = false;
                item = item.substring(1);
            }
            Matcher matcher = UnicodeRegex.compile(item).matcher("");
            _result.add(new MatcherIncludeExclude(matcher, include));
        }
        return new MatcherList(_result);
    }

    public synchronized boolean find(String s) {
        for (MatcherList.MatcherIncludeExclude item : list) {
            boolean haveMatch = item.matcher.reset(s).find();
            if (haveMatch != item.include) {
                return false;
            }
        }
        return true;
    }

    public synchronized boolean matches(String s) {
        for (MatcherList.MatcherIncludeExclude item : list) {
            boolean haveMatch = item.matcher.reset(s).matches();
            if (haveMatch != item.include) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return MULTI_JOINER.join(list);
    }

    // TODO move into test
    public static void main(String[] args) {
        String[][] tests = {
            {"abc§§!def", "abc and def", "false"},
            {"abc§§!def", "abc and qrs", "true"},
        };
        for (String[] test : tests) {
            MatcherList matcherList = MatcherList.from(test[0]);
            boolean actual = matcherList.find(test[1]);
            boolean expected = Boolean.valueOf(test[2]);
            if (actual != expected) {
                System.out.println("Failed");
                matcherList.find(test[1]); // for debugging
            }
        }
    }
}
