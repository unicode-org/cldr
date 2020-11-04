/*
 **********************************************************************
 * Copyright (c) 2006-2007, Google and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.unittest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Random;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CompactStringByteConverter;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.StringByteConverter;
import org.unicode.cldr.util.Utf8StringByteConverter;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UnicodeSet;

public class TestStringByteConverter {
    static Random random = new Random(0);

    enum Type {
        utf8, normal, compact
    };

    public static void main(String[] args) throws IOException {

        String testString = "Mauritania";
        for (int i = 0; i < testString.length(); ++i) {
            testInt(testString.charAt(i));
        }

        // if (true) return;

        testInt(0);
        testInt(0x3F);
        testInt(0x40);
        testInt(-0x3F);
        testInt(-0x40);

        // test read/write int
        for (int i = 0; i < 100; ++i) {
            int test = (int) (random.nextGaussian() * 0xFF);
            testInt(test);
        }
        for (int i = -1; i != 0; i >>>= 1) {
            testInt(i);
        }
        for (int i = 1; i != 0; i <<= 1) {
            testInt(i);
        }

        // test read/write string
        {
            String test = new String("\uD800\uDC00");
            checkUtf8(test);
        }

        UnicodeSet repertoire = new UnicodeSet(
            "[[\\u0000-\\u03FF]&[:script=Greek:]]");
        for (int i = 0; i < 100; ++i) {
            String test = getRandomString(1, 6, repertoire);
            testString(test);
        }
        System.out.println("Bigger is better");
        System.out.println("el rand utf8/comp: " + totalUtf8Bytes + "/"
            + totalBytes + " = " + (totalUtf8Bytes / (double) totalBytes));
        System.out.println();

        testWithLocale("en");
        testWithLocale("fr");
        testWithLocale("el");
        testWithLocale("ja");
        testWithLocale("hi");
        testWithLocale("ar");
    }

    private static void testString(String test) throws IOException {
        for (Type type : Type.values()) {
            testString(test, type);
        }
    }

    private static void testWithLocale(String locale) throws IOException {
        for (Type type : Type.values()) {
            testWithLocale(locale, type);
        }
        System.out.println();
    }

    private static void testWithLocale(String locale, Type type)
        throws IOException {
        totalUtf8Bytes = totalBytes = 0;
        Factory cldrFactory = Factory.make(
            org.unicode.cldr.util.CLDRPaths.MAIN_DIRECTORY, ".*");
        CLDRFile file = cldrFactory.make(locale, false);
        for (String path : file) {
            if (path.contains("exemplarCh")) {
                continue;
            }
            String value = file.getStringValue(path);
            testString(value, type);
        }
        System.out.println(locale + "\t" + type + "\tutf8/comp: "
            + totalUtf8Bytes + "/" + totalBytes + " = "
            + (totalUtf8Bytes / (double) totalBytes));
    }

    static int counter = 1;

    private static void testString(String test, Type type) throws IOException {
        checkUtf8(test);
        byte[] bytes = new byte[2000];
        byte[] bytes2 = new byte[4];
        if (CompactStringByteConverter.DEBUG) {
            System.out.println(counter++ + ": " + Utility.hex(test) + ", \t"
                + test);
        }
        StringByteConverter byteString = type == Type.utf8 ? new Utf8StringByteConverter()
            : type == Type.normal ? new CompactStringByteConverter(false)
                : new CompactStringByteConverter(true);
        int byteLen = byteString.toBytes(test, bytes, 0);

        // verify that incremental gets the same results
        byteString.clear();
        int lastPosition = 0;
        for (int i = 0; i < test.length(); ++i) {
            int byteCount = byteString.toBytes(test.charAt(i), bytes2, 0);
            if (byteCount == 0)
                break;
            for (int j = 0; j < byteCount; ++j) {
                if (bytes2[j] != bytes[lastPosition + j]) {
                    throw new IllegalArgumentException("Fails incremental: <"
                        + Utility.hex(test) + ">, <" + (lastPosition + j)
                        + ">, <" + bytes[lastPosition + j] + ">, <"
                        + bytes2[j] + ">");
                }
            }
            lastPosition += byteCount;
        }

        totalBytes += byteLen;
        if (CompactStringByteConverter.DEBUG) {
            System.out.println("\t" + hex(bytes, 0, byteLen, " "));
        }
        byte[] utf8bytes = test.getBytes("utf-8");
        if (CompactStringByteConverter.DEBUG) {
            System.out.println("\t" + hex(utf8bytes, 0, utf8bytes.length, " "));
        }
        totalUtf8Bytes += utf8bytes.length;
        StringBuilder chars = new StringBuilder();
        byteString.fromBytes(bytes, 0, byteLen, chars);
        if (!test.equals(chars.toString())) {
            throw new IllegalArgumentException("Fails: <" + Utility.hex(test)
                + ">, <" + chars + ">");
        }
    }

    private static void checkUtf8(String test) throws IOException {
        Utf8StringByteConverter conv = new Utf8StringByteConverter();
        byte[] output1 = new byte[1000];
        int len = conv.toBytes(test, output1, 0);
        byte[] output2 = test.getBytes("utf-8");
        if (len != output2.length) {
            throw new IllegalArgumentException("Fails: length");
        }
        for (int i = 0; i < len; ++i) {
            if (output1[i] != output2[i]) {
                conv.toBytes(test, output1, 0);
                System.out.println(Utility.hex(test) + "\t" + i + "\t"
                    + Utility.hex(output1[i] & 0xFF, 2) + "\t"
                    + Utility.hex(output2[i] & 0xFF, 2));
                throw new IllegalArgumentException("Fails: byte");
            }
        }
        Appendable back = conv.fromBytes(output1, 0, len, new StringBuilder());
        if (!test.equals(back.toString())) {
            throw new IllegalArgumentException("Fails: reverse");
        }
    }

    static int totalBytes = 0;
    static int totalUtf8Bytes = 0;

    private static void testInt(int test) throws UnsupportedEncodingException {
        if (CompactStringByteConverter.DEBUG) {
            System.out.println(counter++ + ": " + Utility.hex(test));
        }
        testInt(test, false);
        testInt(test, true);
        if (test >= 0 && test < 0x110000) {
            String test2 = new String(Character.toChars(test));
            byte[] utf8bytes = test2.getBytes("utf-8");
            if (CompactStringByteConverter.DEBUG) {
                System.out.println("\tutf: "
                    + hex(utf8bytes, 0, utf8bytes.length, " "));
            }
        }
    }

    private static void testInt(int test, boolean unsigned)
        throws UnsupportedEncodingException {
        byte[] bytes = new byte[1000];
        int[] ioBytePosition = new int[1];
        int len = unsigned ? CompactStringByteConverter.writeUnsignedInt(test,
            bytes, 0) : CompactStringByteConverter.writeInt(test, bytes, 0);
        if (CompactStringByteConverter.DEBUG) {
            System.out.println("\tcm" + (unsigned ? "u" : "s") + ": "
                + hex(bytes, 0, len, " "));
        }
        ioBytePosition[0] = 0;
        int retest = unsigned ? CompactStringByteConverter.readUnsignedInt(
            bytes, ioBytePosition) : CompactStringByteConverter.readInt(
                bytes, ioBytePosition);
        int lengthRead = ioBytePosition[0];
        if (test != retest)
            throw new IllegalArgumentException();
        if (len != lengthRead)
            throw new IllegalArgumentException();
    }

    private static String hex(byte[] bytes, int start, int end, String separator) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < end; ++i) {
            if (result.length() != 0) {
                result.append(separator);
            }
            result.append(Utility.hex(bytes[i] & 0xFF, 2));
        }
        return result.toString();
    }

    private static String getRandomString(int minLen, int maxLen,
        UnicodeSet repertoire) {
        StringBuilder result = new StringBuilder();
        int len = random.nextInt(maxLen - minLen + 1) + minLen;
        for (int i = 0; i < len; ++i) {
            result.appendCodePoint(repertoire.charAt(random.nextInt(repertoire
                .size())));
        }
        return result.toString();
    }
}