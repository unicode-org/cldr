package org.unicode.cldr.util;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Recommended utility methods for normalizing paths used throughout the CLDR
 * libraries.
 *
 * <p>The methods in this class are used to normalize file and directory paths
 * such that resulting paths are:
 * <ul>
 *     <li>Absolute with respect to the current working directory (if relative).
 *     <li>Normalized with respect to "upward" parent path segments.
 * </ul>
 *
 * <p>For example if the current directory is {@code "/home/user/work/cldr"}:
 * <pre>{@code
 * // Append to current directory.
 * getNormalizedPathString("foo/bar") == "/home/user/work/cldr/foo/bar"
 * // Resolve parent path segments.
 * getNormalizedPathString("../bar") == "/home/user/work/bar"
 * // Retain (but normalize) absolute paths.
 * getNormalizedPathString("/tmp/foo/../bar") == "/tmp/bar"
 * }</pre>
 *
 * <p>Note that it is very important to realize that this is NOT the same as
 * obtaining the "canonical" path (e.g. via {@link File#getCanonicalPath()}
 * since the methods in this class <em>do not follow symbolic links</em>.
 *
 * <p>This is important because in some build systems (e.g. Bazel), file
 * hierarchies are created by mapping files using symbolic links, and there's
 * no necessary reason that the canonical file path preserves the same relative
 * relationship between files.
 *
 * <p>For example Bazel uses a content addressed file cache, so every file used
 * at build time has a canonical path of something like:
 * <pre>{@code
 *     /tmp/build/cache/<hex-formatted-content-fingerprint>
 * }</pre>
 *
 * <p>These files are them mapped (via symbolic links) to a hierarchy such as:
 * <pre>{@code
 *     /<buid-root>/common/supplemental/plurals.xml
 *     /<buid-root>/common/supplemental/pluralRanges.xml
 *     ...
 *     /<buid-root>/common/dtd/ldmlSupplemental.dtd
 * }</pre>
 *
 * <p>When the XML files are parsed by the CLDR library, the DTD file is found
 * via the relative path {@code "../../common/dtd/ldmlSupplemental.dtd}.
 *
 * <p>If the canonical path for these XML files were given to the XML parser, it
 * would attempt to resolve the DTD file location as:
 * <pre>{@code
 * /tmp/build/cache/<hex-formatted-content-fingerprint>/../../common/dtd/ldmlSupplemental.dtd
 * }</pre>
 * which is just:
 * <pre>{@code
 * /tmp/build/common/dtd/ldmlSupplemental.dtd
 * }</pre>
 * which will obviously not work.
 *
 * <p>Over time the CLDR libraries should transition to using {@link Path}
 * instances (in favour of {@link File} or strings) when handling file paths and
 * hopefully some of these methods can eventually be deprecated and removed.
 */
public final class PathUtilities {
    /** Returns the normalized, absolute path string for the given path. */
    public static String getNormalizedPathString(String first, String... rest) {
        return getNormalizedPath(first, rest).toString();
    }

    /** Returns the normalized, absolute path string of the given file. */
    public static String getNormalizedPathString(File file) {
        return getNormalizedPath(file).toString();
    }

    /** Returns the normalized, absolute path string of the given path. */
    public static String getNormalizedPathString(Path path) {
        return getNormalizedPath(path).toString();
    }

    /** Returns the normalized, absolute path of the given path segments. */
    public static Path getNormalizedPath(String first, String... rest) {
        return getNormalizedPath(Paths.get(first, rest));
    }

    /** Returns the normalized, absolute path of the given file. */
    public static Path getNormalizedPath(File file) {
        return getNormalizedPath(Paths.get(file.getPath()));
    }

    /** Returns the normalized, absolute path of the given path. */
    public static Path getNormalizedPath(Path path) {
        return path.toAbsolutePath().normalize();
    }

    private PathUtilities() {}
}
