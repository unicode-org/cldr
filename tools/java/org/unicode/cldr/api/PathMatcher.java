// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.cldr.api;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndex;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static org.unicode.cldr.api.AttributeKey.keyOf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * An immutable matcher for {@link CldrPath} instances.
 *
 * <p>A path matcher pattern looks like:
 * <ul>
 *     <li>{@code //ldml/path/prefix/element[@attribute="value"]}
 *     <li>{@code //ldml/path/prefix/element[@attribute=*]}
 *     <li>{@code //ldml/path/prefix/*[@attribute="value"]}
 *     <li>{@code //ldml/path/prefix/element[@foo=*]/*[@bar="value"]}
 * </ul>
 * where element names and attribute values can be wildcards. No capturing of wildcard values is
 * done, and currently wildcards are limited to the entire field (element name, or attribute value).
 *
 * <h2>Matching an exact path</h2>
 * <pre>{@code
 * private static final PathMatcher ALIAS =
 *     PathMatcher.of("//supplementalData/likelySubtags/likelySubtag[@from=*][@to=*]");
 *
 * ...
 *
 * if (ALIAS.matches(path)) {
 *     // Path represents a subtag alias.
 * }
 * }</pre>
 *
 * <h2>Matching path prefixes and hierarchical matching</h2>
 * <pre>{@code
 * private static final PathMatcher KEY = PathMatcher.of("//ldmlBCP47/keyword/key[@name=\"ca\"]");
 * private static final PathMatcher TYPE = key.withSuffix("type[@name=*]");
 *
 * ...
 *
 * if (KEY.matchesPrefixOf(path)) {
 *     // Once the KEY prefix is matched, we can just "locally" match the suffix.
 *     if (TYPE.locallyMatches(path)) {
 *         // Path is a key-type mapping.
 *     }
 * }
 * }</pre>
 */
public final class PathMatcher {
    private static final Pattern ROOT_PATH_SPEC = Pattern.compile("//(\\w+)(/|$)");

    /**
     * Parses the full or partial (prefix) path pattern into a matcher. Path patterns given to this
     * method must start with {@code "//xxx/..."} where {@code xxx} is a known CLDR XML path prefix
     * (such as {@code "ldml"}).
     */
    public static PathMatcher of(String pattern) {
        Matcher m = ROOT_PATH_SPEC.matcher(pattern);
        checkArgument(m.lookingAt(), "invalid path pattern: %s", pattern);
        // This throws IllegalArgumentException if the type isn't valid.
        CldrDataType.forXmlName(m.group(1));
        return new PathMatcher(null, parse(pattern.substring(2)));
    }

    private final Optional<PathMatcher> parent;
    private final ImmutableList<Predicate<CldrPath>> elementMatchers;
    private final int totalElementCount;

    private PathMatcher(PathMatcher parent, List<Predicate<CldrPath>> elementMatchers) {
        this.parent = Optional.ofNullable(parent);
        this.elementMatchers = ImmutableList.copyOf(elementMatchers);
        this.totalElementCount =
            elementMatchers.size() + (parent != null ? parent.totalElementCount : 0);
    }

    /**
     * Extends this matcher by the given path pattern suffix.
     *
     * <p>For example if we define:
     * <pre>{@code
     * private static final PathMatcher COLLATIONS = PathMatcher.of("//ldml/collations");
     * private static final PathMatcher RULE = COLLATIONS.extendBy("collation/cr");
     * }</pre>
     * Then {@code RULE} would match paths like {@code "//ldml/collations/collation/cr"}.
     */
    public PathMatcher withSuffix(String pattern) {
        return new PathMatcher(this, parse(pattern));
    }

    /**
     * Returns whether this matcher fully matches the given path, including taking into account
     * any parent matchers from which this matcher might have been extended.
     */
    public boolean matches(CldrPath path) {
        if (!locallyMatches(path)) {
            return false;
        }
        if (!parent.isPresent()) {
            // No parent means this is a "root" matcher, so we've finished matching everything.
            return true;
        }
        while (path.getLength() > totalElementCount - elementMatchers.size()) {
            path = path.getParent();
        }
        return parent.get().locallyMatches(path);
    }

    /**
     * Returns whether this matcher matches a proper prefix of the given path, including taking
     * into account any parent matchers from which this matcher might have been extended. Note that
     * the matcher for {@code "//ldml/foo/bar"} is not considered a prefix match for the path
     * {@code //ldml/foo/barbaz}, since partial matching of an element name is not allowed.
     */
    public boolean matchesPrefixOf(CldrPath path) {
        if (path.getLength() < totalElementCount) {
            return false;
        }
        while (path.getLength() > totalElementCount) {
            path = path.getParent();
        }
        return matches(path);
    }

    /**
     * Returns whether this matcher matches the given path only in relation to the pattern
     * used to generate this matcher. This method is useful for nested visitation via {@link
     * org.unicode.cldr.api.CldrData.PrefixVisitor PrefixVisitor} when path prefixes are visited
     * before values.
     *
     * <p>This method assumes that any parent matchers from which this matcher was extended already
     * match the path. The possible length of the parent matchers is taken into account however to
     * ensure that the sub-sequence match for this matcher starts at the correct point in the path.
     */
    public boolean locallyMatches(CldrPath path) {
        return (path.getLength() == totalElementCount)
            && matchRegion(path, totalElementCount - elementMatchers.size());
    }

    private boolean matchRegion(CldrPath path, int offset) {
        // offset is the path element corresponding the the "top most" element matcher, it
        // must be in the range 0 ... (path.length() - elementMatchers.size()).
        checkPositionIndex(offset, path.getLength() - elementMatchers.size());
        // First jump over the path parents until we find the last matcher.
        int matchPathLength = offset + elementMatchers.size();
        while (path.getLength() > matchPathLength) {
            path = path.getParent();
        }
        return matchForward(path, elementMatchers.size() - 1);
    }

    private boolean matchForward(CldrPath path, int matcherIndex) {
        if (matcherIndex < 0) {
            return true;
        }
        return matchForward(path.getParent(), matcherIndex - 1)
            && elementMatchers.get(matcherIndex).test(path);
    }

    // --- Parsing of matcher path patterns ----

    // Make a new, non-interned, unique instance here which we can test by reference to
    // determine if the argument is to be captured (needed as ImmutableMap prohibits null).
    // DO NOT change this code to assign "*" as the value directly, it MUST be a new instance.
    @SuppressWarnings("StringOperationCanBeSimplified")
    private static final String WILDCARD = new String("*");

    private static final Pattern ELEMENT_START_REGEX =
        Pattern.compile("(\\*|[-:\\w]+)(?:/|\\[|$)");
    private static final Pattern ATTRIBUTE_REGEX =
        Pattern.compile("\\[@([-:\\w]+)=(?:\\*|\"([^\"]*)\")]");

    // element := foo, foo[@bar="baz"], foo[@bar=*]
    // pathspec := element{/element}*
    private static List<Predicate<CldrPath>> parse(String pattern) {
        List<Predicate<CldrPath>> specs = new ArrayList<>();
        int pos = 0;
        do {
            pos = parse(pattern, pos, specs);
        } while (pos >= 0);
        return specs;
    }

    // Return next start index or -1.
    private static int parse(String pattern, int pos, List<Predicate<CldrPath>> specs) {
        Matcher m = ELEMENT_START_REGEX.matcher(pattern).region(pos, pattern.length());
        checkArgument(m.lookingAt(), "invalid path pattern (index=%s): %s", pos, pattern);
        String name = m.group(1);
        Map<String, String> attributes = ImmutableMap.of();
        pos = m.end(1);
        if (pos < pattern.length() && pattern.charAt(pos) == '[') {
            // We have attributes to add.
            attributes = new LinkedHashMap<>();
            do {
                m = ATTRIBUTE_REGEX.matcher(pattern).region(pos, pattern.length());
                checkArgument(m.lookingAt(),
                    "invalid path pattern (index=%s): %s", pos, pattern);
                // Null if we matched the '*' wildcard.
                String value = m.group(2);
                attributes.put(m.group(1), value != null ? value : WILDCARD);
                pos = m.end();
            } while (pos < pattern.length() && pattern.charAt(pos) == '[');
        }
        // Wildcard matching is less efficient because attribute keys cannot be made in advance, so
        // since it's also very rare, we special case it.
        Predicate<CldrPath> matcher = name.equals(WILDCARD)
            ? new WildcardElementMatcher(attributes)::match
            : new ElementMatcher(name, attributes)::match;
        specs.add(matcher);
        if (pos == pattern.length()) {
            return -1;
        }
        checkState(pattern.charAt(pos) == '/',
            "invalid path pattern (index=%s): %s", pos, pattern);
        return pos + 1;
    }

    // Matcher for path elements like "foo[@bar=*]" where the name is known in advance.
    private static final class ElementMatcher {
        private final String name;
        private final ImmutableMap<AttributeKey, String> attributes;

        private ElementMatcher(String name, Map<String, String> attributes) {
            this.name = checkNotNull(name);
            this.attributes = attributes.entrySet().stream()
                .collect(toImmutableMap(e -> keyOf(name, e.getKey()), Map.Entry::getValue));
        }

        @SuppressWarnings("StringEquality")
        boolean match(CldrPath path) {
            if (!path.getName().equals(name)) {
                return false;
            }
            for (Map.Entry<AttributeKey, String> e : attributes.entrySet()) {
                String actual = path.get(e.getKey());
                if (actual == null) {
                    return false;
                }
                String expected = e.getValue();
                // DO NOT change this to use expected.equals(WILDCARD).
                if (expected != WILDCARD && !expected.equals(actual)) {
                    return false;
                }
            }
            return true;
        }
    }

    // Matcher for path elements like "*[@bar=*]", where the name isn't known until match time.
    private static final class WildcardElementMatcher {
        private final ImmutableMap<String, String> attributes;

        private WildcardElementMatcher(Map<String, String> attributes) {
            this.attributes = ImmutableMap.copyOf(attributes);
        }

        @SuppressWarnings("StringEquality")
        private boolean match(CldrPath path) {
            // The wildcard matcher never fails due to the element name but must create new key
            // instances every time matching occurs (because the key name is dynamic). Since this
            // is rare, it's worth making into a separate case.
            for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                String actual = path.get(keyOf(path.getName(), attribute.getKey()));
                if (actual == null) {
                    return false;
                }
                String expected = attribute.getValue();
                // DO NOT change this to use expected.equals(WILDCARD).
                if (expected != WILDCARD && !expected.equals(actual)) {
                    return false;
                }
            }
            return true;
        }
    }
}
