package org.unicode.cldr.tool.unitsinfo;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.VersionInfo;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.unicode.cldr.tool.ToolConstants;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.SupplementalDataInfo;

/**
 * Reads which units CLDR names: the active units (unit ID → category) in this checkout and at each
 * release tag, and the replacements of deprecated unit IDs.
 */
final class CldrUnits {

    /** CLDR 24 is the first release with category-prefixed unit IDs, such as "length-meter". */
    private static final int FIRST_HISTORY_RELEASE = 24;

    private static final Splitter WHITESPACE =
            Splitter.on(CharMatcher.whitespace()).omitEmptyStrings();
    private static final Pattern UNIT_TYPE = Pattern.compile("<unit type=\"([^\"]+)\"");
    private static final Pattern DEPRECATED_UNITS =
            Pattern.compile(
                    "<id\\s+type=['\"]unit['\"]\\s+idStatus=['\"]deprecated['\"]>([^<]*)</id>");

    private CldrUnits() {}

    /** The active units in this checkout, from every locale file. */
    static Map<String, String> current() {
        try (Stream<Path> files = Files.list(Path.of(CLDRPaths.MAIN_DIRECTORY))) {
            return activeUnits(
                    files.filter(file -> file.toString().endsWith(".xml")).map(CldrUnits::read),
                    read(Path.of(CLDRPaths.COMMON_DIRECTORY, "validity", "unit.xml")));
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    /**
     * The active units in a release, from its tag in {@code gitDir}. Like ICU, it only looks at
     * root.xml and en.xml.
     */
    static Map<String, String> atRelease(String gitDir, int release) {
        String tag = "release-" + release;
        List<String> locales =
                List.of(
                        requiredGitFile(gitDir, tag, "common/main/root.xml"),
                        requiredGitFile(gitDir, tag, "common/main/en.xml"));
        String validity = gitFile(gitDir, tag, "common/validity/unit.xml"); // added in CLDR 29
        return activeUnits(locales.stream(), validity == null ? "" : validity);
    }

    /** Major releases from CLDR 24 up to, but not including, the one in development. */
    static List<Integer> historyReleases() {
        int dev = VersionInfo.getInstance(ToolConstants.DEV_VERSION).getMajor();
        return ToolConstants.CLDR_VERSIONS.stream()
                .map(version -> VersionInfo.getInstance(version).getMajor())
                .filter(major -> major >= FIRST_HISTORY_RELEASE && major < dev)
                .distinct()
                .sorted()
                .toList();
    }

    /** Deprecated unit IDs and their replacements, from the unit aliases in units.xml. */
    static Map<String, String> aliases() {
        Map<String, String> aliases = new TreeMap<>();
        SupplementalDataInfo.getInstance()
                .getLocaleAliasInfo()
                .getOrDefault("unit", Map.of())
                .forEach(
                        (unit, alias) -> {
                            List<String> replacements = alias.get0();
                            if (replacements != null && !replacements.isEmpty()) {
                                aliases.put(unit, replacements.get(0));
                            }
                        });
        return aliases;
    }

    /**
     * Maps the unit in each {@code <unit type="category-unit">} to its category, skipping IDs that
     * validity/unit.xml marks deprecated.
     */
    private static Map<String, String> activeUnits(Stream<String> localeXmls, String validityXml) {
        Set<String> deprecated = new HashSet<>();
        Matcher block = DEPRECATED_UNITS.matcher(validityXml);
        while (block.find()) {
            deprecated.addAll(WHITESPACE.splitToList(block.group(1)));
        }
        Map<String, String> units = new TreeMap<>();
        localeXmls.forEach(
                xml -> {
                    Matcher type = UNIT_TYPE.matcher(xml);
                    while (type.find()) {
                        String longId = type.group(1);
                        int dash = longId.indexOf('-');
                        if (dash > 0 && !deprecated.contains(longId)) {
                            units.put(longId.substring(dash + 1), longId.substring(0, dash));
                        }
                    }
                });
        return units;
    }

    /** Reads a file at a release tag, and fails with a hint if the tag is missing. */
    private static String requiredGitFile(String gitDir, String tag, String path) {
        String text = gitFile(gitDir, tag, path);
        checkState(
                text != null,
                "Can't read %s at %s in %s. Fetch the release tags with: git -C %s fetch"
                        + " https://github.com/unicode-org/cldr.git"
                        + " 'refs/tags/release-*:refs/tags/release-*'",
                path,
                tag,
                gitDir,
                gitDir);
        return text;
    }

    /** Reads a file at a git tag, or returns null if git can't (missing tag or file). */
    private static String gitFile(String gitDir, String tag, String path) {
        try {
            Process git =
                    new ProcessBuilder("git", "-C", gitDir, "show", tag + ":" + path)
                            .redirectError(Redirect.DISCARD)
                            .start();
            String text = new String(git.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return git.waitFor() == 0 ? text : null;
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }
}
