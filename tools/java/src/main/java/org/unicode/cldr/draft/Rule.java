package org.unicode.cldr.draft;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Rule {
    private final Pattern prematch;
    private final boolean prematchFindAtEnd;
    private final Pattern postmatch;
    private final List<Item> results = new ArrayList<Item>();
    /**
     * if negative, is position before start position; otherwise is position relative to end of replacement.
     * To put *at* the start position, put a CURSOR at the start of the results.
     * Default is zero, meaning at the end of the replacement unless there is a CURSOR item.
     */
    private int outOfBoundsCursor = 0;

    /**
     * contains a replacement text with a set of replacements (up to 10) plus a new cursor point
     * 
     * @author markdavis
     */
    // TODO add function calls &foo(....)

    public interface Item {
        String compose(Matcher prematcher, Matcher postmatcher);
    }

    public static class StringItem implements Item {
        String replacement;

        public StringItem(String s) {
            replacement = s;
        }

        public String compose(Matcher prematcher, Matcher postmatcher) {
            return replacement;
        }

        public String toString() {
            return replacement;
        }
    }

    public static class NumberedItem implements Item {
        boolean post;
        int number;

        public NumberedItem(int parseInt, boolean b) {
            number = parseInt;
            post = b;
        }

        public String compose(Matcher prematcher, Matcher postmatcher) {
            return post ? postmatcher.group(number) : prematcher.group(number);
        }

        public String toString() {
            return "$" + number;
        }
    }

    public static class CursorItem implements Item {
        static Item CURSOR = new CursorItem();

        public String compose(Matcher prematcher, Matcher postmatcher) {
            throw new UnsupportedOperationException();
        }

        public String toString() {
            return "|";
        }
    }

    public Rule(String pre, String post, List<String> results2) {
        Pattern tempPrematch;
        boolean tempPrematchFindAtEnd = false;
        if (pre.length() == 0) {
            tempPrematch = null;
        } else {
            try {
                tempPrematch = Pattern.compile("(?<=" + pre + ")", Pattern.COMMENTS + Pattern.DOTALL);
                tempPrematchFindAtEnd = true;
            } catch (Exception e) {
                tempPrematch = Pattern.compile(pre + "$", Pattern.COMMENTS + Pattern.DOTALL);
            }
        }
        prematch = tempPrematch;
        prematchFindAtEnd = tempPrematchFindAtEnd;
        postmatch = Pattern.compile(post, Pattern.COMMENTS + Pattern.DOTALL);
        for (String s : results2) {
            if (s.startsWith("$")) {
                results.add(new NumberedItem(Integer.parseInt(s.substring(1)), true));
            } else if (s.length() > 0) {
                results.add(new StringItem(s));
            }
        }
    }

    /**
     * Modifies result, returns new cursor
     * 
     * @param result
     * @param offset
     * @param matcher
     * @return
     */
    int append(StringBuilder result, Matcher prematcher, Matcher postmatcher) {
        int startPosition = result.length();
        int cursor = -1;
        for (Item item : results) {
            if (item == CursorItem.CURSOR) {
                cursor = result.length();
            } else {
                final String insertion = item.compose(prematcher, postmatcher);
                result.append(insertion);
            }
        }
        return cursor >= 0 ? cursor
            : outOfBoundsCursor < 0 ? startPosition + outOfBoundsCursor
                : result.length() + outOfBoundsCursor;
    }

    public String toString() {
        String main = postmatch.toString();
        return (prematch == null ? "" : prematch.toString())
            + main + " > " + results + " ; ";
    }

    public Matcher getPrematcher(CharSequence processedAlready) {
        return prematch == null ? null
            : prematch.matcher(processedAlready);
    }

    public Matcher getPostmatcher(CharSequence toBeProcessed) {
        return postmatch.matcher(toBeProcessed);
    }

    public boolean prematch(Matcher prematcher, CharSequence processedAlready) {
        return prematchFindAtEnd
            ? prematcher.find(processedAlready.length())
            : prematcher.find();
    }
}
