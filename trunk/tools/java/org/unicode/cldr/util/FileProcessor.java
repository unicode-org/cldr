package org.unicode.cldr.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import com.ibm.icu.util.ICUUncheckedIOException;

public class FileProcessor {
    private int lineCount;
    protected boolean doHash = true;

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
            BufferedReader in = FileReaders.openFile(classLocation, fileName);
            return process(in, fileName);
        } catch (Exception e) {
            throw (RuntimeException) new IllegalArgumentException(lineCount + ":\t" + 0).initCause(e);
        }

    }

    public FileProcessor process(String fileName) {
        try {
            FileInputStream fileStream = new FileInputStream(fileName);
            InputStreamReader reader = new InputStreamReader(fileStream, CldrUtility.UTF8);
            BufferedReader bufferedReader = new BufferedReader(reader, 1024 * 64);
            return process(bufferedReader, fileName);
        } catch (Exception e) {
            throw (RuntimeException) new IllegalArgumentException(lineCount + ":\t" + 0).initCause(e);
        }
    }

    public FileProcessor process(String directory, String fileName) {
        try {
            FileInputStream fileStream = new FileInputStream(directory + File.separator + fileName);
            InputStreamReader reader = new InputStreamReader(fileStream, CldrUtility.UTF8);
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
                if (comment == 0 || doHash && comment >= 0) {
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
        } catch (IOException e) {
            throw new ICUUncheckedIOException(lineCount + ":\t" + line, e);
        }
        return this;
    }
}