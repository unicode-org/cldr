package org.unicode.cldr.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;

import org.unicode.cldr.util.With.SimpleIterator;

import com.ibm.icu.util.ICUUncheckedIOException;

/**
 * Class to get an Iterable for Strings from a File, returning one line at a time.
 * @author ribnitz
 *
 */
public class StringIterables {
    /**
     * Simple API to iterate over file lines. Example:
     * for (String s : FileUtilities.in(directory,name)) {
     * ...
     * }
     *
     * @author markdavis
     *
     */
    public static Iterable<String> in(Class<?> class1, String file) {
        return With.in(new FileLines(FileReaders.openFile(class1, file, CldrUtility.UTF8)));
    }

    /**
     * Simple API to iterate over file lines. Example:
     * for (String s : FileUtilities.in(directory,name)) {
     * ...
     * }
     *
     * @author markdavis
     *
     */
    public static Iterable<String> in(Class<?> class1, String file, Charset charset) {
        return With.in(new FileLines(FileReaders.openFile(class1, file, charset)));
    }

    /**
     * Simple API to iterate over file lines. Example:
     * for (String s : FileUtilities.in(directory,name)) {
     * ...
     * }
     *
     * @author markdavis
     *
     */
    public static Iterable<String> in(String directory, String file) {
        return With.in(new FileLines(FileReaders.openFile(directory, file, CldrUtility.UTF8)));
    }

    /**
     * Simple API to iterate over file lines. Example:
     * for (String s : FileUtilities.in(directory,name)) {
     * ...
     * }
     *
     * @author markdavis
     *
     */
    public static Iterable<String> in(BufferedReader reader) {
        return With.in(new FileLines(reader));
    }

    /**
     * Simple API to iterate over file lines. Example:
     * for (String s : FileUtilities.in(directory,name)) {
     * ...
     * }
     *
     * @author markdavis
     *
     */
    public static Iterable<String> in(String directory, String file, Charset charset) {
        return With.in(new FileLines(FileReaders.openFile(directory, file, charset)));
    }

    private static class FileLines implements SimpleIterator<String> {
        private BufferedReader input;

        public FileLines(BufferedReader input) {
            this.input = input;
        }

        @Override
        public String next() {
            try {
                String result = input.readLine();
                if (result == null) {
                    input.close();
                }
                return result;
            } catch (IOException e) {
                throw new ICUUncheckedIOException(e); // handle dang'd checked exception
            }
        }

    }
}