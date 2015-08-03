package org.unicode.cldr.util;

import java.util.regex.Pattern;

public abstract class SemiFileReader extends FileProcessor {
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