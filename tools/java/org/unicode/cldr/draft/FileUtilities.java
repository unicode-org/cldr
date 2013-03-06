package org.unicode.cldr.draft;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.With;
import org.unicode.cldr.util.With.SimpleIterator;

import com.ibm.icu.dev.util.BagFormatter;

public final class FileUtilities {

    public static abstract class SemiFileReader extends FileProcessor {
        public final static Pattern SPLIT = Pattern.compile("\\s*;\\s*");

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
                BufferedReader in = FileUtilities.openFile(classLocation, fileName);
                return process(in, fileName);
            } catch (Exception e) {
                throw (RuntimeException) new IllegalArgumentException(lineCount + ":\t" + 0).initCause(e);
            }

        }

        public FileProcessor process(String directory, String fileName) {
            try {
                FileInputStream fileStream = new FileInputStream(directory + "/" + fileName);
                InputStreamReader reader = new InputStreamReader(fileStream, FileUtilities.UTF8);
                BufferedReader bufferedReader = new BufferedReader(reader, 1024 * 64);
                return process(bufferedReader, fileName);
            } catch (Exception e) {
                throw (RuntimeException) new IllegalArgumentException(lineCount + ":\t" + 0).initCause(e);
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
                throw (RuntimeException) new IllegalArgumentException(lineCount + ":\t" + line).initCause(e);
            }
            return this;
        }
    }

    //
    // public static SemiFileReader fillMapFromSemi(Class classLocation, String fileName, SemiFileReader handler) {
    // return handler.process(classLocation, fileName);
    // }
    public static BufferedReader openFile(Class<?> class1, String file) {
        return openFile(class1, file, FileUtilities.UTF8);
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
                throw new IllegalArgumentException("Couldn't open file: " + file + "; relative to class: "
                    + className, e);
            }
            throw new IllegalArgumentException("Couldn't open file: " + canonicalName
                + className, e);
        }
    }

    public static BufferedReader openFile(String directory, String file, Charset charset) {
        try {
            return new BufferedReader(new InputStreamReader(new FileInputStream(new File(directory, file)), charset));
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(e); // handle dang'd checked exception
        }
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
        appendFile(class1, filename, FileUtilities.UTF8, null, out);
    }

    public static void appendFile(Class<?> class1, String filename, Charset charset, String[] replacementList,
        PrintWriter out) {
        try {
            com.ibm.icu.dev.util.FileUtilities.appendBufferedReader(openFile(class1, filename, charset), out,
                replacementList); // closes file
        } catch (IOException e) {
            throw new IllegalArgumentException(e); // wrap darn'd checked exception
        }
    }

    public static void copyFile(Class<?> class1, String sourceFile, String targetDirectory) {
        copyFile(class1, sourceFile, targetDirectory, sourceFile);
    }

    public static void copyFile(Class<?> class1, String sourceFile, String targetDirectory, String newName) {
        try {
            PrintWriter out = BagFormatter.openUTF8Writer(targetDirectory, newName);
            FileUtilities.appendFile(class1, sourceFile, out);
            out.close();
        } catch (IOException e) {
            throw new IllegalArgumentException(e); // dang'd checked exceptions
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
            throw new IllegalArgumentException("File not found: " + resourceString);
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
                throw new IllegalArgumentException(e); // handle dang'd checked exception
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

    public final static Pattern SEMI_SPLIT = Pattern.compile("\\s*;\\s*");
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
                throw (RuntimeException) new IllegalArgumentException("Problem with line: " + line)
                    .initCause(e);
            }
        }
        in.close();
    }
}
