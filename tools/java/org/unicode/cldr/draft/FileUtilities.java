package org.unicode.cldr.draft;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.With;
import org.unicode.cldr.util.With.SimpleIterator;

import com.ibm.icu.util.ICUUncheckedIOException;

public final class FileUtilities {
    public static final boolean SHOW_FILES;
    static {
        boolean showFiles = false;
        try {
            showFiles = System.getProperty("SHOW_FILES") != null;
        } catch (SecurityException ignored) {
        }
        SHOW_FILES = showFiles;
    }

    public static final PrintWriter CONSOLE = new PrintWriter(System.out, true);

    private static PrintWriter log = CONSOLE;

    public static BufferedReader openUTF8Reader(String dir, String filename) throws IOException {
        return openReader(dir, filename, "UTF-8");
    }

    public static BufferedReader openReader(String dir, String filename, String encoding) throws IOException {
        File file = dir.length() == 0 ? new File(filename) : new File(dir, filename);
        if (SHOW_FILES && log != null) {
            log.println("Opening File: "
                + file.getCanonicalPath());
        }
        return new BufferedReader(
            new InputStreamReader(
                new FileInputStream(file),
                encoding),
            4 * 1024);
    }

    public static PrintWriter openUTF8Writer(String dir, String filename) throws IOException {
        return openWriter(dir, filename, "UTF-8");
    }

    public static PrintWriter openWriter(String dir, String filename, String encoding) throws IOException {
        File file = new File(dir, filename);
        if (SHOW_FILES && log != null) {
            log.println("Creating File: "
                + file.getCanonicalPath());
        }
        String parentName = file.getParent();
        if (parentName != null) {
            File parent = new File(parentName);
            parent.mkdirs();
        }
        return new PrintWriter(
            new BufferedWriter(
                new OutputStreamWriter(
                    new FileOutputStream(file),
                    encoding),
                4 * 1024));
    }

    public static abstract class SemiFileReader extends FileProcessor {
        public final static Pattern SPLIT = PatternCache.get("\\s*;\\s*");

        protected abstract boolean handleLine(int lineCount, int start, int end, String[] items);

        protected void handleEnd() {
        }

        protected boolean isCodePoint() {
            return true;
        }

        protected String[] splitLine(String line) {
            return SPLIT.split(line);
        }

        @Override
        protected boolean handleLine(int lineCount, String line) {
            String[] parts = splitLine(line);
            int start, end;
            if (isCodePoint()) {
                String source = parts[0];
                int range = source.indexOf("..");
                if (range >= 0) {
                    start = Integer.parseInt(source.substring(0, range), 16);
                    end = Integer.parseInt(source.substring(range + 2), 16);
                } else {
                    start = end = Integer.parseInt(source, 16);
                }
            } else {
                start = end = -1;
            }
            return handleLine(lineCount, start, end, parts);
        }
    }

    public static class FileProcessor {
        private int lineCount;

        protected void handleStart() {
        }

        /**
         * Return false to abort
         * 
         * @param lineCount
         * @param line
         * @return
         */
        protected boolean handleLine(int lineCount, String line) {
            return true;
        }

        protected void handleEnd() {
        }

        public int getLineCount() {
            return lineCount;
        }

        public void handleComment(String line, int commentCharPosition) {
        }

        public FileProcessor process(Class<?> classLocation, String fileName) {
            try {
                BufferedReader in = openFile(classLocation, fileName);
                return process(in, fileName);
            } catch (Exception e) {
                throw new ICUUncheckedIOException(lineCount + ":\t" + 0, e);
            }

        }

        public FileProcessor process(String fileName) {
            try {
                FileInputStream fileStream = new FileInputStream(fileName);
                InputStreamReader reader = new InputStreamReader(fileStream, UTF8);
                BufferedReader bufferedReader = new BufferedReader(reader, 1024 * 64);
                return process(bufferedReader, fileName);
            } catch (Exception e) {
                throw new ICUUncheckedIOException(lineCount + ":\t" + 0, e);
            }
        }

        public FileProcessor process(String directory, String fileName) {
            try {
                FileInputStream fileStream = new FileInputStream(directory + File.separator + fileName);
                InputStreamReader reader = new InputStreamReader(fileStream, UTF8);
                BufferedReader bufferedReader = new BufferedReader(reader, 1024 * 64);
                return process(bufferedReader, fileName);
            } catch (Exception e) {
                throw new ICUUncheckedIOException(lineCount + ":\t" + 0, e);
            }
        }

        public FileProcessor process(BufferedReader in, String fileName) {
            handleStart();
            String line = null;
            lineCount = 1;
            try {
                for (;; ++lineCount) {
                    line = in.readLine();
                    if (line == null) {
                        break;
                    }
                    int comment = line.indexOf("#");
                    if (comment >= 0) {
                        handleComment(line, comment);
                        line = line.substring(0, comment);
                    }
                    if (line.startsWith("\uFEFF")) {
                        line = line.substring(1);
                    }
                    line = line.trim();
                    if (line.length() == 0) {
                        continue;
                    }
                    if (!handleLine(lineCount, line)) {
                        break;
                    }
                }
                in.close();
                handleEnd();
            } catch (Exception e) {
                throw (RuntimeException) new ICUUncheckedIOException(lineCount + ":\t" + line, e);
            }
            return this;
        }
    }

    //
    // public static SemiFileReader fillMapFromSemi(Class classLocation, String fileName, SemiFileReader handler) {
    // return handler.process(classLocation, fileName);
    // }
    public static BufferedReader openFile(Class<?> class1, String file) {
        return openFile(class1, file, UTF8);
    }

    public static BufferedReader openFile(Class<?> class1, String file, Charset charset) {
        // URL path = null;
        // String externalForm = null;
        try {
            // //System.out.println("Reading:\t" + file1.getCanonicalPath());
            // path = class1.getResource(file);
            // externalForm = path.toExternalForm();
            // if (externalForm.startsWith("file:")) {
            // externalForm = externalForm.substring(5);
            // }
            // File file1 = new File(externalForm);
            // boolean x = file1.canRead();
            // final InputStream resourceAsStream = new FileInputStream(file1);
            final InputStream resourceAsStream = class1.getResourceAsStream(file);
            // String foo = class1.getResource(".").toString();
            if (charset == null) {
                charset = UTF8;
            }
            InputStreamReader reader = new InputStreamReader(resourceAsStream, charset);
            BufferedReader bufferedReader = new BufferedReader(reader, 1024 * 64);
            return bufferedReader;
        } catch (Exception e) {
            String className = class1 == null ? null : class1.getCanonicalName();
            String canonicalName = null;
            try {
                String relativeFileName = getRelativeFileName(class1, "../util/");
                canonicalName = new File(relativeFileName).getCanonicalPath();
            } catch (Exception e1) {
                throw new ICUUncheckedIOException("Couldn't open file: " + file + "; relative to class: "
                    + className, e);
            }
            throw new ICUUncheckedIOException("Couldn't open file " + file + "; in path " + canonicalName + "; relative to class: "
                + className, e);
        }
    }

    public static BufferedReader openFile(String directory, String file, Charset charset) {
        try {
            return new BufferedReader(new InputStreamReader(new FileInputStream(new File(directory, file)), charset));
        } catch (FileNotFoundException e) {
            throw new ICUUncheckedIOException(e); // handle dang'd checked exception
        }
    }

    public static BufferedReader openFile(File file, Charset charset) {
        try {
            return new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
        } catch (FileNotFoundException e) {
            throw new ICUUncheckedIOException(e); // handle dang'd checked exception
        }
    }

    public static BufferedReader openFile(File file) {
        return openFile(file, UTF8);
    }

    public static BufferedReader openFile(String directory, String file) {
        return openFile(directory, file, UTF8);
    }

    public static final Charset UTF8 = Charset.forName("utf-8");

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

    public static void appendFile(Class<?> class1, String filename, PrintWriter out) {
        appendFile(class1, filename, UTF8, null, out);
    }

    public static void appendFile(Class<?> class1, String filename, String[] replacementList, PrintWriter out) {
        appendFile(class1, filename, UTF8, replacementList, out);
    }

    public static void appendFile(Class<?> class1, String filename, Charset charset, String[] replacementList,
        PrintWriter out) {
        BufferedReader br = openFile(class1, filename, charset);
        try {
            try {
                appendBufferedReader(br, out, replacementList);
            } finally {
                br.close();
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e); // wrap darn'd checked exception
        }
    }

    public static void appendFile(String filename, String encoding, PrintWriter output) throws IOException {
        appendFile(filename, encoding, output, null);
    }

    public static void appendFile(String filename, String encoding, PrintWriter output, String[] replacementList) throws IOException {
        BufferedReader br = openReader("", filename, encoding);
        try {
            appendBufferedReader(br, output, replacementList);
        } finally {
            br.close();
        }
    }

    public static void appendBufferedReader(BufferedReader br,
        PrintWriter output, String[] replacementList) throws IOException {
        while (true) {
            String line = br.readLine();
            if (line == null) break;
            if (replacementList != null) {
                for (int i = 0; i < replacementList.length; i += 2) {
                    line = replace(line, replacementList[i], replacementList[i + 1]);
                }
            }
            output.println(line);
        }
        br.close();
    }

    /**
     * Replaces all occurrences of piece with replacement, and returns new String
     */
    public static String replace(String source, String piece, String replacement) {
        if (source == null || source.length() < piece.length()) return source;
        int pos = 0;
        while (true) {
            pos = source.indexOf(piece, pos);
            if (pos < 0) return source;
            source = source.substring(0, pos) + replacement + source.substring(pos + piece.length());
            pos += replacement.length();
        }
    }

    public static String replace(String source, String[][] replacements) {
        return replace(source, replacements, replacements.length);
    }

    public static String replace(String source, String[][] replacements, int count) {
        for (int i = 0; i < count; ++i) {
            source = replace(source, replacements[i][0], replacements[i][1]);
        }
        return source;
    }

    public static String replace(String source, String[][] replacements, boolean reverse) {
        if (!reverse) return replace(source, replacements);
        for (int i = 0; i < replacements.length; ++i) {
            source = replace(source, replacements[i][1], replacements[i][0]);
        }
        return source;
    }

    public static String anchorize(String source) {
        String result = source.toLowerCase(Locale.ENGLISH).replaceAll("[^\\p{L}\\p{N}]+", "_");
        if (result.endsWith("_")) result = result.substring(0, result.length() - 1);
        if (result.startsWith("_")) result = result.substring(1);
        return result;
    }

    public static void copyFile(Class<?> class1, String sourceFile, String targetDirectory) {
        copyFile(class1, sourceFile, targetDirectory, sourceFile, null);
    }

    public static void copyFile(Class<?> class1, String sourceFile, String targetDirectory, String newName) {
        copyFile(class1, sourceFile, targetDirectory, newName, null);
    }

    public static void copyFile(Class<?> class1, String sourceFile, String targetDirectory, String newName, String[] replacementList) {
        try {
            PrintWriter out = openUTF8Writer(targetDirectory, newName);
            appendFile(class1, sourceFile, UTF8, replacementList, out);
            out.close();
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e); // dang'd checked exceptions
        }
    }

    public static String getRelativeFileName(Class<?> class1, String filename) {
        URL resource = class1.getResource(filename);
        String resourceString = resource.toString();
        if (resourceString.startsWith("file:")) {
            return resourceString.substring(5);
        } else if (resourceString.startsWith("jar:file:")) {
            return resourceString.substring(9);
        } else {
            throw new ICUUncheckedIOException("File not found: " + resourceString);
        }
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
    public static Iterable<String> in(Class<?> class1, String file) {
        return With.in(new FileLines(openFile(class1, file, UTF8)));
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
        return With.in(new FileLines(openFile(class1, file, charset)));
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
        return With.in(new FileLines(openFile(directory, file, UTF8)));
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
        return With.in(new FileLines(openFile(directory, file, charset)));
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

    public static String cleanLine(String line) {
        int comment = line.indexOf("#");
        if (comment >= 0) {
            line = line.substring(0, comment);
        }
        if (line.startsWith("\uFEFF")) {
            line = line.substring(1);
        }
        return line.trim();
    }

    public final static Pattern SEMI_SPLIT = PatternCache.get("\\s*;\\s*");
    private static final boolean SHOW_SKIP = false;

    public static String[] cleanSemiFields(String line) {
        line = cleanLine(line);
        return line.isEmpty() ? null : SEMI_SPLIT.split(line);
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

    public static void handleFile(String filename, LineHandler handler) throws IOException {
        BufferedReader in = CldrUtility.getUTF8Data(filename);
        while (true) {
            String line = in.readLine();
            if (line == null) {
                break;
            }
            try {
                if (!handler.handle(line)) {
                    if (SHOW_SKIP) System.out.println("Skipping line: " + line);
                }
            } catch (Exception e) {
                throw new ICUUncheckedIOException("Problem with line: " + line, e);
            }
        }
        in.close();
    }

    public static Iterable<String> in(File file) {
        return With.in(new FileLines(openFile(file, UTF8)));
    }
}
