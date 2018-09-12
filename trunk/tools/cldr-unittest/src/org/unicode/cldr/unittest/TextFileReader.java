package org.unicode.cldr.unittest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that reads in a file, and that provides an Iterable over its contents.
 * Encoding is assumed to be UTF8
 *
 * @author ribnitz
 *
 */
class TextFileReader<E> {
    /**
     * General purpose interface, used by TestFileReader;
     *
     * @author ribnitz
     *
     * @param <E>
     */
    static interface ProcessableLine<E> {
        /**
         * Return true if the line needs to be processed
         *
         * @param line
         * @return
         */
        boolean lineNeedsProcessing(String line);

        /**
         * Process the line, returning a result; Implementing classes should
         * also handle updating the old values
         *
         * @param line
         * @param oldValues
         * @return
         */
        E processLine(String line, E oldValues);

        /**
         * Return the old values
         *
         * @return
         */
        E getOldValues();
    }

    private final String source;

    /**
     * Initialize using the filename given, file encoding is assumed to be in
     * UTF8.
     *
     * @param file
     * @throws IOException
     */
    public TextFileReader(String file) throws IOException {
        this(new FileReader(file));
    }

    /**
     * Initialize using the Reader given, encoding is assumed to be in UTF8.
     *
     * @param rdr
     * @param cs
     * @throws IOException
     */
    public TextFileReader(Reader rdr) throws IOException {
        try (BufferedReader br = new BufferedReader(rdr)) {
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                // append a line break...
                sb.append("\r\n");
            }
            source = sb.toString();
        }

    }

    private BufferedReader bufferedReader() {
        return new BufferedReader(new StringReader(source));
    }

    /**
     * Provide an Iterable; calling proc.lineNeedsProcessing, and
     * proc.processLine for all lines.
     *
     * @param proc
     * @return
     * @throws IOException
     */
    public Iterable<E> getLines(ProcessableLine<E> proc) throws IOException {
        if (proc == null) {
            throw new IllegalArgumentException(
                "Please call with non-null processor");
        }
        final List<E> result = new ArrayList<>();
        try (BufferedReader rdr = bufferedReader()) {
            E oldLine = null;
            String line = null;
            while ((line = rdr.readLine()) != null) {
                if (proc.lineNeedsProcessing(line)) {
                    result.add(proc.processLine(line, oldLine));
                    oldLine = proc.getOldValues();
                }
            }
        }
        return result;
    }
}