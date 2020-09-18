package org.unicode.cldr.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Random;

import org.unicode.cldr.draft.FileUtilities;

import com.ibm.icu.util.ICUUncheckedIOException;

/**
 * Simple utility to create a temporary file, write into it, then close it.
 * If the file differs from the old file (except for date), then it is deleted.
 * Otherwise it replaces the target file. Moved from UnicodeTools.
 * @author markdavis
 */
public class TempPrintWriter extends Writer {
    final PrintWriter tempPrintWriter;
    final String tempName;
    final String filename;
    boolean noReplace = false;


    public static TempPrintWriter openUTF8Writer(String filename) {
        return new TempPrintWriter(new File(filename));
    }

    public static TempPrintWriter openUTF8Writer(String dir, String filename) {
        return new TempPrintWriter(new File(dir, filename));
    }

    public TempPrintWriter(String dir, String filename) {
        this(new File(dir, filename));
    }

    public TempPrintWriter(File file) {
        super();
        final String parentFile = file.getParent();
        this.filename = file.toString();
        Random rand = new Random();
        try {
            File tempFile;
            do {
                tempFile = new File(parentFile, (0xFFFF & rand.nextInt()) + "-" + file.getName());
            } while (tempFile.exists());
            tempName = tempFile.toString();
            tempPrintWriter = FileUtilities.openUTF8Writer(parentFile, tempFile.getName());
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    public void dontReplaceFile() {
        noReplace = true;
    }

    @Override
    public void close() {
        tempPrintWriter.close();
        try {
            if (noReplace) {
                new File(tempName).delete();
            } else {
                replaceDifferentOrDelete(filename, tempName, false);
            }
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }

    @Override
    public void write(char[] cbuf, int off, int len) {
        tempPrintWriter.write(cbuf, off, len);
    }

    @Override
    public void flush() {
        tempPrintWriter.flush();
    }

    public void println(Object line) {
        tempPrintWriter.println(line);
    }

    public void print(Object line) {
        tempPrintWriter.print(line);
    }

    public void println() {
        tempPrintWriter.println();
    }

    /**
     * If contents(newFile) ≠ contents(oldFile), rename newFile to old. Otherwise delete newfile. Return true if replaced. *
     */
    private static boolean replaceDifferentOrDelete(String oldFile, String newFile, boolean skipCopyright) throws IOException {
        final File oldFile2 = new File(oldFile);
        if (oldFile2.exists()) {
            final String lines[] = new String[2];
            final boolean identical = filesAreIdentical(oldFile, newFile, skipCopyright, lines);
            if (identical) {
                new File(newFile).delete();
                return false;
            }
            System.out.println("Found difference in : " + oldFile + ", " + newFile);
            final int diff = compare(lines[0], lines[1]);
            System.out.println(" File1: '" + lines[0].substring(0,diff) + "', '" + lines[0].substring(diff) + "'");
            System.out.println(" File2: '" + lines[1].substring(0,diff) + "', '" + lines[1].substring(diff) + "'");
        }
        new File(newFile).renameTo(oldFile2);
        return true;
    }

    private static boolean filesAreIdentical(String file1, String file2, boolean skipCopyright, String[] lines) throws IOException {
        if (file1 == null) {
            lines[0] = null;
            lines[1] = null;
            return false;
        }
        final BufferedReader br1 = new BufferedReader(new FileReader(file1), 32*1024);
        final BufferedReader br2 = new BufferedReader(new FileReader(file2), 32*1024);
        String line1 = "";
        String line2 = "";
        try {
            for (int lineCount = 0; ; ++lineCount) {
                line1 = getLineWithoutFluff(br1, lineCount == 0, skipCopyright);
                line2 = getLineWithoutFluff(br2, lineCount == 0, skipCopyright);
                if (line1 == null) {
                    if (line2 == null) {
                        return true;
                    }
                    break;
                }
                if (!line1.equals(line2)) {
                    break;
                }
            }
            lines[0] = line1;
            lines[1] = line2;
            if (lines[0] == null) {
                lines[0] = "<end of file>";
            }
            if (lines[1] == null) {
                lines[1] = "<end of file>";
            }
            return false;
        } finally {
            br1.close();
            br2.close();
        }
    }

    private static String getLineWithoutFluff(BufferedReader br1, boolean first, boolean skipCopyright) throws IOException {
        while (true) {
            String line1 = br1.readLine();
            if (line1 == null) {
                return line1;
            }
            line1 = line1.trim();
            if (line1.length() == 0) {
                continue;
            }
            if (line1.equals("#")) {
                continue;
            }
            if (line1.startsWith("# Generated")) {
                continue;
            }
            if (line1.startsWith("# Date")) {
                continue;
            }
            if (skipCopyright && line1.startsWith("# Copyright")) {
                continue;
            }
            if (line1.startsWith("<p><b>Date:</b>")) {
                continue;
            }
            if (line1.startsWith("<td valign=\"top\">20") && line1.endsWith("GMT</td>")) {
                continue;
            }

            if (line1.equals("# ================================================")) {
                continue;
            }
            if (first && line1.startsWith("#")) {
                first = false;
                continue;
            }
            return line1;
        }
    }

    /**
     * Returns -1 if strings are equal; otherwise the first position they are different at.
     */
    public static int compare(String a, String b) {
        int len = a.length();
        if (len > b.length()) {
            len = b.length();
        }
        for (int i = 0; i < len; ++i) {
            if (a.charAt(i) != b.charAt(i)) {
                return i;
            }
        }
        if (a.length() != b.length()) {
            return len;
        }
        return -1;
    }

}