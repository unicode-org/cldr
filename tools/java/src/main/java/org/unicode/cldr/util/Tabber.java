
/*
 *******************************************************************************
 * Copyright (C) 2002-2012, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.List;

import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.UnicodeSet;

public abstract class Tabber {
    public static final byte LEFT = 0, CENTER = 1, RIGHT = 2;
    private static final String[] ALIGNMENT_NAMES = { "Left", "Center", "Right" };

    /**
     * Repeats a string n times
     * @param source
     * @param times
     */
    // TODO - optimize repeats using doubling?
    public static String repeat(String source, int times) {
        if (times <= 0) return "";
        if (times == 1) return source;
        StringBuffer result = new StringBuffer();
        for (; times > 0; --times) {
            result.append(source);
        }
        return result.toString();
    }

    public String process(String source) {
        StringBuffer result = new StringBuffer();
        int lastPos = 0;
        for (int count = 0; lastPos < source.length(); ++count) {
            int pos = source.indexOf('\t', lastPos);
            if (pos < 0) pos = source.length();
            process_field(count, source, lastPos, pos, result);
            lastPos = pos + 1;
        }
        return prefix + result.toString() + postfix;
    }

    private String prefix = "";
    private String postfix = "";

    public abstract void process_field(int count, String source, int start, int limit, StringBuffer output);

    public Tabber clear() {
        return this;
    }

    public static class MonoTabber extends Tabber {
        int minGap = 0;

        private List stops = new ArrayList();
        private List types = new ArrayList();

        public Tabber clear() {
            stops.clear();
            types.clear();
            minGap = 0;
            return this;
        }

        public String toString() {
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < stops.size(); ++i) {
                if (i != 0) buffer.append("; ");
                buffer
                    .append(ALIGNMENT_NAMES[((Integer) types.get(i)).intValue()])
                    .append(",")
                    .append(stops.get(i));
            }
            return buffer.toString();
        }

        /**
         * Adds tab stop and how to align the text UP TO that stop
         * @param tabPos
         * @param type
         */
        public MonoTabber addAbsolute(int tabPos, int type) {
            stops.add(new Integer(tabPos));
            types.add(new Integer(type));
            return this;
        }

        /**
         * Adds relative tab stop and how to align the text UP TO that stop
         */
        public Tabber add(int fieldWidth, byte type) {
            int last = getStop(stops.size() - 1);
            stops.add(new Integer(last + fieldWidth));
            types.add(new Integer(type));
            return this;
        }

        public int getStop(int fieldNumber) {
            if (fieldNumber < 0) return 0;
            if (fieldNumber >= stops.size()) fieldNumber = stops.size() - 1;
            return ((Integer) stops.get(fieldNumber)).intValue();
        }

        public int getType(int fieldNumber) {
            if (fieldNumber < 0) return LEFT;
            if (fieldNumber >= stops.size()) return LEFT;
            return ((Integer) types.get(fieldNumber)).intValue();
        }

        public void process_field(int count, String source, int start, int limit, StringBuffer output) {
            String piece = source.substring(start, limit);
            int startPos = getStop(count - 1);
            int endPos = getStop(count) - minGap;
            int type = getType(count);
            final int pieceLength = getMonospaceWidth(piece);
            switch (type) {
            case LEFT:
                break;
            case RIGHT:
                startPos = endPos - pieceLength;
                break;
            case CENTER:
                startPos = (startPos + endPos - pieceLength + 1) / 2;
                break;
            }

            int gap = startPos - getMonospaceWidth(output);
            if (count != 0 && gap < minGap) gap = minGap;
            if (gap > 0) output.append(repeat(" ", gap));
            output.append(piece);
        }

        static final UnicodeSet IGNOREABLE = new UnicodeSet("[:di:]");

        private int getMonospaceWidth(CharSequence piece) {
            int len = 0;
            for (int cp : CharSequences.codePoints(piece)) {
                if (!IGNOREABLE.contains(cp)) {
                    ++len;
                }
            }
            return len;
        }
    }

    public static Tabber NULL_TABBER = new Tabber() {
        public void process_field(int count, String source, int start, int limit, StringBuffer output) {
            if (count > 0) output.append("\t");
            output.append(source.substring(start, limit));
        }
    };

    public static class HTMLTabber extends Tabber {
        private List<String> parameters = new ArrayList();
        private String element = "td";
        {
            setPrefix("<tr>");
            setPostfix("</tr>");
        }

        public HTMLTabber setParameters(int count, String params) {
            // fill in
            while (parameters.size() <= count) {
                parameters.add(null);
            }
            parameters.set(count, params);
            return this;
        }

        public String getElement() {
            return element;
        }

        public HTMLTabber setElement(String element) {
            this.element = element;
            return this;
        }

        public void process_field(int count, String source, int start, int limit, StringBuffer output) {
            output.append("<" + element);
            String params = null;
            if (count < parameters.size()) {
                params = parameters.get(count);
            }
            if (params != null) {
                output.append(' ');
                output.append(params);
            }
            output.append(">");
            output.append(source.substring(start, limit));
            // TODO Quote string
            output.append("</" + element + ">");
        }
    }

    /**
     */
    public String getPostfix() {
        return postfix;
    }

    /**
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @param string
     */
    public Tabber setPostfix(String string) {
        postfix = string;
        return this;
    }

    /**
     * @param string
     */
    public Tabber setPrefix(String string) {
        prefix = string;
        return this;
    }

    public Tabber add(int i, byte left2) {
        // does nothing unless overridden
        return this;
    }

}
