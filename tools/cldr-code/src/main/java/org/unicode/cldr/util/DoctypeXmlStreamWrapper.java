package org.unicode.cldr.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.unicode.cldr.icu.LDMLConstants;
import org.xml.sax.InputSource;

public class DoctypeXmlStreamWrapper {
    private static final String DOCTYPE = "<!DOCTYPE";
    private static final char DOCTYPE_CHARS[] = DOCTYPE.toCharArray();
    private static final byte DOCTYPE_BYTES[] = DOCTYPE.getBytes(StandardCharsets.UTF_8);
    // the string to look for:  xmlns="
    private static final String XMLNS_EQUALS = LDMLConstants.XMLNS + "=\"";
    /**
     * Size of the input buffer, needs to be able to handle any expansion when the header is updated
     */
    public static int BUFFER_MAX_SIZE = 1024;
    /** Size of the first read, needs to contain xmlns="..." and be less than BUFFER_MAX_SIZE */
    public static int BUFFER_READ_SIZE = 512;

    /**
     * Wrap an InputSource in something that will automatically insert a DTD reference in place of
     * an xmlns directive.
     *
     * @throws IOException
     */
    public static InputSource wrap(InputSource src) throws IOException {
        Reader r = src.getCharacterStream();
        InputStream is = src.getByteStream();
        if (r != null) {
            src.setCharacterStream(wrap(r));
        } else if (is != null) {
            src.setByteStream(wrap(is, src.getEncoding()));
        } else {
            throw new NullPointerException(
                    "Internal error: Character and Byte stream are both null");
        }
        return src;
    }

    /** wrap a byte oriented stream */
    public static InputStream wrap(InputStream src, String encoding) throws IOException {
        if (encoding == null) {
            encoding = "UTF-8";
        }
        PushbackInputStream pr = new PushbackInputStream(src, BUFFER_MAX_SIZE);
        byte inbuf[] = pr.readNBytes(BUFFER_READ_SIZE);
        if (!hasDocType(inbuf, encoding)) {
            inbuf = fixup(inbuf, encoding).getBytes(encoding);
        }
        pr.unread(inbuf);
        return pr;
    }

    /** wrap a char oriented stream */
    public static Reader wrap(Reader src) throws IOException {
        PushbackReader pr = new PushbackReader(src, BUFFER_MAX_SIZE);
        char inbuf[] = new char[BUFFER_READ_SIZE];
        int readlen = pr.read(inbuf);
        if (!hasDocType(inbuf, readlen)) {
            char buf2[] = Arrays.copyOf(inbuf, readlen);
            inbuf = fixup(new String(buf2)).toCharArray();
            readlen = inbuf.length;
        }
        pr.unread(inbuf, 0, readlen);
        return pr;
    }

    /** Fix an input byte array, including the DOCTYPE */
    private static String fixup(byte[] inbuf, String encoding) {
        try {
            final String s = new String(inbuf, encoding);
            return fixup(s);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("While parsing " + encoding, e);
        }
    }

    /** Fix an input String, including DOCTYPE */
    private static String fixup(final String s) {
        // exit if nothing matches
        for (final DtdType d : DtdType.values()) {
            if (s.contains(XMLNS_EQUALS + d.getNsUrl())) {
                return fixup(s, d);
            }
        }
        // couldn't fix it, just pass through
        return s;
    }

    /** Fix an input String given a specific DtdType. */
    private static String fixup(String s, DtdType d) {
        int n = s.indexOf("?>");
        if (n == -1) {
            throw new IllegalArgumentException("Invalid XML prefix: ?> not found.");
        }
        n += 2; // move the cut-point to the end of the "?>" sequence

        final String doctype = "\n" + d.getDoctype() + "\n";
        final String s2 = s.substring(0, n) + doctype + s.substring(n);
        return s2;
    }

    private static final boolean hasDocType(byte[] inbuf, String encoding) {
        if (inbuf == null || inbuf.length == 0) return false;

        // Try as utf-8/ASCII bytes - this will be the common case
        if (arrayContains(inbuf, inbuf.length, DOCTYPE_BYTES)) return true;

        // break out here
        if (encoding == null || encoding.equals("UTF-8")) return false;

        // Try 2, with encoding
        try {
            final String s = new String(inbuf, encoding);
            return s.contains(DOCTYPE);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("While parsing " + encoding, e);
        }
    }

    private static final boolean hasDocType(char[] inbuf, int readlen) {
        if (inbuf == null || readlen <= 0) {
            return false;
        }
        return arrayContains(inbuf, readlen, DOCTYPE_CHARS);
    }

    private static boolean arrayContains(char[] inbuf, int inlen, char[] testbuf) {
        final int testlen = testbuf.length;
        int t = 0;
        for (int i = 0; i < inlen; i++) {
            if (inbuf[i] == testbuf[t]) {
                t++;
                if (t == testlen) return true;
            } else {
                t = 0;
            }
        }
        return false;
    }

    private static boolean arrayContains(byte[] inbuf, int inlen, byte[] testbuf) {
        final int testlen = testbuf.length;
        int t = 0;
        for (int i = 0; i < inlen; i++) {
            if (inbuf[i] == testbuf[t]) {
                t++;
                if (t == testlen) return true;
            } else {
                t = 0;
            }
        }
        return false;
    }
}
