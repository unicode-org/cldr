package org.unicode.cldr.util;

import com.ibm.icu.dev.util.UnicodeMap;

public class ApproximateWidth {
    static UnicodeMap<Integer> data = new UnicodeMap<Integer>();
    static Integer defaultWidth;

    public static Integer getWidth(int cp) {
        Integer result = data.get(cp);
        return result == null ? defaultWidth : result;
    }

    /** 
     * Return # of ems * 10 
     */
    public static int getWidth(CharSequence s) {
        int result = 0;
        int cp;
        for (int i = 0; i < s.length(); i += Character.charCount(cp)) {
            cp = Character.codePointAt(s, i);
            result += getWidth(cp);
        }
        return result;
    }

    static {
        SemiFileReader MyFileHander = new SemiFileReader() {
            @Override
            public void handleComment(String line, int commentCharPosition) {
                if (line.contains("@missing")) {
                    String[] items = SPLIT.split(line);
                    defaultWidth = Integer.parseInt(items[1]);
                }
            };

            @Override
            protected boolean handleLine(int lineCount, int start, int end, String[] items) {
                data.putAll(start, end, Integer.parseInt(items[1]));
                return true;
            }

        };

        MyFileHander.process(ApproximateWidth.class, "data/ApproximateWidth.txt");
    }

    public static void main(String[] args) {
        for (String arg : args) {
            System.out.println(arg + ":\t" + getWidth(arg));
        }
    }
}
