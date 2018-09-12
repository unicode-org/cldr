package org.unicode.cldr.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Map;

public class FileCopier {

    /**
     * Copy the contents of the reader to the Writer, performing all the replacements specified in the map. This method will close
     * the reader, but will leave the Writer open. The contents of the reader are read one line at a time.
     * @param rdr
     * @param replacements
     * @param out
     * @throws IOException
     */
    public static void copyAndReplace(Reader rdr, Map<String, String> replacements, Writer out) throws IOException {
        if (replacements == null || replacements.isEmpty()) {
            copy(rdr, out);
            return;
        }
        PrintWriter pw = new PrintWriter(out);
        try (BufferedReader br = new BufferedReader(rdr);) {
            String line = null;
            while ((line = br.readLine()) != null) {
                for (String key : replacements.keySet()) {
                    if (line.contains(key)) {
                        line = line.replaceAll(key, replacements.get(key));
                    }
                }
                pw.println(line);
            }
        } finally {
            pw.flush();
        }
    }

    /**
     * Copy the resource srcFile to the Writer out, using a Reader with the charset specified; Reader will be closed, Writer will be
     * flushed and left open. The replacements as specified in the Map will be performed.
     * @param cls
     * @param srcFile
     * @param charSet
     * @param replacements
     * @param out
     * @throws IOException
     */
    public static void copyAndReplace(Class<?> cls, String srcFile, Charset charSet, Map<String, String> replacements,
        Writer out) throws IOException {
        copyAndReplace(new InputStreamReader(cls.getResourceAsStream(srcFile), charSet), replacements, out);
    }

    /**
     * Append all the lines read from the Reader to the writer. Will close the reader, but leave the
     * writer open, flushing it
     * @param rdr
     * @param wr
     * @throws IOException
     */
    public static void copy(Reader rdr, Writer wr) throws IOException {
        PrintWriter pw = new PrintWriter(wr);
        try (BufferedReader br = new BufferedReader(rdr)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                pw.println(line);
            }
        } finally {
            wr.flush();
        }
    }

    /***
     * Copy all the contents of the reader to the writer, performing line-by-line replacements, as specified by the map. Closes the
     * reader, and flushes the writer, but leaves it open.
     * @param rdr
     * @param wr
     * @param replacements
     * @throws IOException
     */
    public static void copyAndReplace(Reader rdr, Writer wr, Map<String, String> replacements) throws IOException {
        if (replacements == null || replacements.isEmpty()) {
            copy(rdr, wr);
            return;
        }
        PrintWriter pw = new PrintWriter(wr);
        try (BufferedReader br = new BufferedReader(rdr);) {
            String line = null;
            while ((line = br.readLine()) != null) {
                for (String key : replacements.keySet()) {
                    if (line.contains(key)) {
                        line = line.replaceAll(key, replacements.get(key));
                    }
                }
                pw.println(line);
            }
        } finally {
            pw.flush();
        }
    }

    /**
     * Copy the resource denoted by sourcefile to the target directory, giving it the new name newName.
     * @param cls
     * @param sourceFile
     * @param targetDirectory
     * @param newName
     * @throws IOException
     */
    public static void copy(Class<?> cls, String sourceFile, String targetDirectory, String newName) throws IOException {
        try (InputStream is = cls.getResourceAsStream(sourceFile);
            Writer wr = new FileWriter(Paths.get(targetDirectory, newName).toFile());) {
            copy(new InputStreamReader(is), wr);
        }
    }

    /**
     * Writes the resource named sourceFile to the Writer, leaving the writer open, but flushing it. UTF-8 will be used as a charSet
     * @param cls
     * @param sourceFile
     * @param out
     * @throws IOException
     */
    public static void copy(Class<?> cls, String sourceFile, Writer out) throws IOException {
        copy(new InputStreamReader(cls.getResourceAsStream(sourceFile), Charset.forName("UTF-8")), out);
    }

    /**
     * Writes the resource given as sourceFile to the Writer, using the specified CharSet. The Writer will be left open, but flushed.
     * @param cls
     * @param sourceFile
     * @param charset
     * @param out
     * @throws IOException
     */
    public static void copy(Class<?> cls, String sourceFile, Charset charset, Writer out) throws IOException {
        copy(new InputStreamReader(cls.getResourceAsStream(sourceFile), charset), out);
    }

    /**
     * Copies the resource accessible as sourceFile to the TargetDirectory, also naming it sourceFile
     * @param cls
     * @param sourceFile
     * @param targetDirectory
     * @throws IOException
     */
    public static void copy(Class<?> cls, String sourceFile, String targetDirectory) throws IOException {
        copy(cls, sourceFile, targetDirectory, sourceFile);
    }

    /**
     * Ensure that directory exists
     */
    public static void ensureDirectoryExists(String targetDirectory) {
        final File targetDir = new File(targetDirectory);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
    }

    public static void copyAndReplace(Class<?> cls, String srcFile, String destDir, String destFile, Map<String, String> replacements) throws IOException {
        copyAndReplace(new InputStreamReader(cls.getResourceAsStream(srcFile)),
            replacements, new FileWriter(Paths.get(destDir, destFile).toFile()));

    }
}