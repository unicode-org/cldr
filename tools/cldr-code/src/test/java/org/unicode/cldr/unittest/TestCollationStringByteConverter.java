/*
 **********************************************************************
 * Copyright (c) 2006-2007, Google and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.unittest;

import java.text.ParsePosition;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.TreeMap;

import org.unicode.cldr.util.CollationStringByteConverter;
import org.unicode.cldr.util.Dictionary;
import org.unicode.cldr.util.Dictionary.DictionaryBuilder;
import org.unicode.cldr.util.Dictionary.DictionaryCharList;
import org.unicode.cldr.util.Dictionary.Matcher;
import org.unicode.cldr.util.Dictionary.Matcher.Filter;
import org.unicode.cldr.util.LenientDateParser;
import org.unicode.cldr.util.LenientDateParser.Parser;
import org.unicode.cldr.util.StateDictionaryBuilder;
import org.unicode.cldr.util.TestStateDictionaryBuilder;
import org.unicode.cldr.util.Utf8StringByteConverter;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.SimpleTimeZone;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class TestCollationStringByteConverter {

    private static final boolean DEBUG = true;

    // static interface PartCharSequence {
    // /**
    // * Replace index < x.length() with hasCharAt(index)
    // * @param index
    // * @return
    // */
    // boolean hasCharAt(int index);
    // char charAt(int index);
    // public PartCharSequence subSequence2(int start, int end);
    // /**
    // * Returns a subsequence going to the end.
    // * @param start
    // * @return
    // */
    // public PartCharSequence subSequence2(int start);
    // /**
    // * Return the length known so far. If hasCharAt(getKnownLength()) ==
    // false, then it is the real length.
    // * @return
    // */
    // public int getKnownLength();
    // }
    //
    // static class PartCharSequenceWrapper implements PartCharSequence {
    // CharSequence source;
    //
    // public boolean equals(Object anObject) {
    // return source.equals(anObject);
    // }
    //
    // public int hashCode() {
    // return source.hashCode();
    // }
    //
    // public PartCharSequenceWrapper(CharSequence source) {
    // this.source = source;
    // }
    //
    // public char charAt(int index) {
    // return source.charAt(index);
    // }
    //
    // public PartCharSequence subSequence2(int beginIndex, int endIndex) {
    // return new PartCharSequenceWrapper(source.subSequence(beginIndex,
    // endIndex));
    // }
    //
    // public PartCharSequence subSequence2(int beginIndex) {
    // return new PartCharSequenceWrapper(source.subSequence(beginIndex,
    // source.length()));
    // }
    //
    // /* (non-Javadoc)
    // * @see com.ibm.icu.text.RuleBasedCollator.PartCharSequence#hasCharAt(int)
    // */
    // public boolean hasCharAt(int index) {
    // return index < source.length();
    // }
    // /* (non-Javadoc)
    // * @see
    // com.ibm.icu.text.RuleBasedCollator.PartCharSequence#getKnownLength()
    // */
    // public int getKnownLength() {
    // return source.length();
    // }
    // }

    boolean hasBadSurrogates(String string) {
        boolean needsLow = false;
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            if (needsLow != Character.isLowSurrogate(c)) {
                return true;
            }
            needsLow = Character.isHighSurrogate(c);
        }
        return needsLow;
    }

    public static void main(String[] args) throws Exception {
        check2(ULocale.JAPANESE);
        // check2(ULocale.FRANCE);
        if (true)
            return;
        checkBasic();
        check();
    }

    private static void checkBasic() {
        System.out.println(ULocale.getDisplayName("en_GB", "en"));

        DictionaryBuilder<CharSequence> builder = new StateDictionaryBuilder<CharSequence>();
        Map<CharSequence, CharSequence> map = new TreeMap<CharSequence, CharSequence>(
            Dictionary.CHAR_SEQUENCE_COMPARATOR);
        map.put("a", "ABC");
        map.put("bc", "B"); // ß
        Dictionary<CharSequence> dict = builder.make(map);
        String[] tests = { "a/bc", "bc", "a", "d", "", "abca" };
        for (String test : tests) {
            System.out.println("TRYING: " + test);
            DictionaryCharList<CharSequence> gcs = new DictionaryCharList<CharSequence>(
                dict, test);
            for (int i = 0; gcs.hasCharAt(i); ++i) {
                char c = gcs.charAt(i);
                final int sourceOffset = gcs.toSourceOffset(i);
                final CharSequence sourceSubSequence = gcs.sourceSubSequence(i,
                    i + 1);
                System.out.println(i + "\t" + c + "\t" + sourceOffset + "\t"
                    + sourceSubSequence);
            }
            gcs.hasCharAt(Integer.MAX_VALUE);
            System.out.println("Length: " + gcs.getKnownLength());
        }
    }

    private static void check2(ULocale testLocale) {
        // Map<CharSequence,CharSequence> map = new
        // TreeMap<CharSequence,CharSequence>();
        // map.put("j", "J");
        // map.put("july", "JULY");
        // map.put("june", "JUNE"); // ß
        // map.put("august", "AUGUST"); // ß
        // DictionaryBuilder<CharSequence> builder = new
        // StateDictionaryBuilder<CharSequence>();
        // Dictionary<CharSequence> dict = builder.make(map);
        // System.out.println(map);
        // System.out.println(dict);
        // Matcher m = dict.getMatcher();
        // System.out.println(m.setText("a").next(Filter.LONGEST_UNIQUE) + "\t"
        // + m + "\t" + m.nextUniquePartial());
        // System.out.println(m.setText("j").next(Filter.LONGEST_UNIQUE) + "\t"
        // + m + "\t" + m.nextUniquePartial());
        // System.out.println(m.setText("ju").next(Filter.LONGEST_UNIQUE) + "\t"
        // + m + "\t" + m.nextUniquePartial());
        // System.out.println(m.setText("jul").next(Filter.LONGEST_UNIQUE) +
        // "\t" + m + "\t" + m.nextUniquePartial());

        TimeZone testTimeZone = TimeZone.getTimeZone("America/Chicago");
        TimeZone unknown = new SimpleTimeZone(60000, "Etc/Unknown");

        // DateFormatSymbols dfs = new DateFormatSymbols(testLocale);
        // String[][] zoneInfo = dfs.getZoneStrings();
        // for (int i = 0; i < zoneInfo.length; ++i) {
        // System.out.print(i );
        // for (int j = 0; j < zoneInfo[i].length; ++j) {
        // System.out.print("\t" + zoneInfo[i][j]);
        // }
        // System.out.println();
        // }

        LenientDateParser ldp = LenientDateParser.getInstance(testLocale);
        Parser parser = ldp.getParser();

        if (DEBUG) {
            System.out.println(parser.debugShow2());
        }

        LinkedHashSet<DateFormat> tests = new LinkedHashSet<DateFormat>();
        // Arrays.asList(new String[] {"jan 12 1963 1942 au", "1:2:3",
        // "1:3 jan"})
        Calendar testDate = Calendar.getInstance();
        // testDate.set(2007, 8, 25, 1, 2, 3);
        //
        // Calendar dateOnly = Calendar.getInstance();
        // dateOnly.set(Calendar.YEAR, testDate.get(Calendar.YEAR));
        // dateOnly.set(Calendar.MONTH, testDate.get(Calendar.MONTH));
        // dateOnly.set(Calendar.DAY_OF_MONTH,
        // testDate.get(Calendar.DAY_OF_MONTH));
        //
        // Calendar timeOnly = Calendar.getInstance();
        // timeOnly.set(Calendar.HOUR, testDate.get(Calendar.HOUR));
        // timeOnly.set(Calendar.MINUTE, testDate.get(Calendar.MINUTE));
        // timeOnly.set(Calendar.SECOND, testDate.get(Calendar.SECOND));
        //
        // Calendar hourMinuteOnly = Calendar.getInstance();
        // hourMinuteOnly.set(Calendar.HOUR, testDate.get(Calendar.HOUR));
        // hourMinuteOnly.set(Calendar.MINUTE, testDate.get(Calendar.MINUTE));

        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss VVVV");

        for (int style = 0; style < 4; ++style) {
            final DateFormat dateInstance = DateFormat.getDateInstance(style,
                testLocale);
            dateInstance.setTimeZone(testTimeZone);
            tests.add(dateInstance);
            final DateFormat timeInstance = DateFormat.getTimeInstance(style,
                testLocale);
            timeInstance.setTimeZone(testTimeZone);
            tests.add(timeInstance);
            for (int style2 = 0; style2 < 4; ++style2) {
                final DateFormat dateTimeInstance = DateFormat
                    .getDateTimeInstance(style, style, testLocale);
                dateTimeInstance.setTimeZone(testTimeZone);
                tests.add(dateTimeInstance);
                final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                    ((SimpleDateFormat) DateFormat.getTimeInstance(style2,
                        testLocale)).toPattern()
                        + " "
                        + ((SimpleDateFormat) dateInstance).toPattern(),
                    testLocale);
                simpleDateFormat.setTimeZone(testTimeZone);
                tests.add(simpleDateFormat);
                // tests.put(DateFormat.getTimeInstance(style,
                // testLocale).format(testDate) + " " + dateExample, null);
                // // reversed
            }
        }
        // DateTimePatternGenerator dtpg =
        // DateTimePatternGenerator.getInstance(testLocale);
        // Map<String,String> skeletons = dtpg.getSkeletons(new
        // LinkedHashMap());
        // for (String skeleton : skeletons.keySet()) {
        // String pattern = skeletons.get(skeleton);
        // tests.add(new SimpleDateFormat(pattern, testLocale));
        // }

        ParsePosition parsePosition = new ParsePosition(0);
        ParsePosition parsePosition2 = new ParsePosition(0);

        Calendar calendar = Calendar.getInstance();

        Calendar calendar2 = Calendar.getInstance();

        int success = 0;
        int failure = 0;
        for (DateFormat test : tests) {
            String expected = test.format(testDate);
            parsePosition.setIndex(0);
            calendar.clear();
            calendar.setTimeZone(unknown);
            parser.parse(expected, calendar, parsePosition);

            parsePosition2.setIndex(0);
            calendar2.clear();
            calendar2.setTimeZone(unknown);
            test.parse(expected, calendar2, parsePosition2);

            if (true) {

            }

            if (areEqual(calendar, calendar2)) {
                System.out.println("OK\t" + expected + "\t=>\t<"
                    + iso.format(calendar) + ">\t"
                    + show(expected, parsePosition));
                success++;
            } else {
                failure++;
                LenientDateParser.DEBUG = true;
                calendar.clear();
                parsePosition.setIndex(0);
                parser.parse(expected, calendar, parsePosition);
                LenientDateParser.DEBUG = false;

                System.out.println("FAIL\t" + expected + "\t=>\t<"
                    + iso.format(calendar) + "//"
                    + showZone(ldp, calendar.getTimeZone())
                    + ">\texpected: <" + iso.format(calendar2) + "//"
                    + showZone(ldp, calendar2.getTimeZone()) + ">\t"
                    + show(expected, parsePosition) + "\t" + parser);
            }
            System.out.println();
        }
        System.out.println("SUCCESS: " + success + "\t\tFAILURE: " + failure);
    }

    private static String showZone(LenientDateParser ldp, TimeZone zone) {
        String id = zone.getID();
        return LenientDateParser.getCountry(id) + ":" + id;
    }

    private static boolean areEqual(Calendar calendar, Calendar calendar2) {
        if (calendar.getTimeInMillis() == calendar2.getTimeInMillis()) { // &&
            // calendar.getTimeZone().equals(calendar2.getTimeZone())
            // until
            // ICU
            // gets
            // fixed
            return true;
        }
        return false; // separate for debugging
    }

    private static String show(String test, ParsePosition parsePosition) {
        return "{"
            + test.substring(0, parsePosition.getIndex())
            + "|"
            + test.substring(parsePosition.getIndex())
            + (parsePosition.getErrorIndex() == -1 ? "" : "/"
                + parsePosition.getErrorIndex())
            + "}";
    }

    public static void check() throws Exception {
        final RuleBasedCollator col = (RuleBasedCollator) Collator
            .getInstance(ULocale.ENGLISH);
        col.setStrength(Collator.PRIMARY);
        col.setAlternateHandlingShifted(true);
        CollationStringByteConverter converter = new CollationStringByteConverter(
            col, new Utf8StringByteConverter()); // new
        // ByteString(true)
        Matcher<String> matcher = converter.getDictionary().getMatcher();
        // if (true) {
        // Iterator<Entry<CharSequence, String>> x =
        // converter.getDictionary().getMapping();
        // while (x.hasNext()) {
        // Entry<CharSequence, String> entry = x.next();
        // System.out.println(entry.getKey() + "\t" +
        // Utility.hex(entry.getKey().toString())+ "\t\t" + entry.getValue()
        // + "\t" + Utility.hex(entry.getValue().toString()));
        // }
        // System.out.println(converter.getDictionary().debugShow());
        // }
        String[] tests = { "ab", "abc", "ss", "ß", "Abcde",
            "Once Upon AB Time", "\u00E0b", "A\u0300b" };
        byte[] output = new byte[1000];
        for (String test : tests) {
            String result = matcher
                .setText(
                    new DictionaryCharList<String>(converter
                        .getDictionary(), test))
                .convert(new StringBuffer()).toString();
            System.out.println(test + "\t=>\t" + result);
            int len = converter.toBytes(test, output, 0);
            String result2 = converter.fromBytes(output, 0, len,
                new StringBuilder()).toString();
            System.out.println(test + "\t(bytes) =>\t" + result2);
            for (int i = 0; i < len; ++i) {
                System.out.print(Utility.hex(output[i] & 0xFF, 2) + " ");
            }
            System.out.println();
        }

        DictionaryBuilder<String> builder = new StateDictionaryBuilder<String>(); // .setByteConverter(converter);
        Map<CharSequence, String> map = new TreeMap<CharSequence, String>(
            Dictionary.CHAR_SEQUENCE_COMPARATOR);
        map.put("ab", "found-ab");
        map.put("abc", "found-ab");
        map.put("ss", "found-ss"); // ß
        Dictionary<String> dict = builder.make(map);
        final String string = "Abcde and ab c Upon aß AB basS Time\u00E0bA\u0300b";
        DictionaryCharList<String> x = new DictionaryCharList<String>(
            converter.getDictionary(), string);
        x.hasCharAt(Integer.MAX_VALUE); // force growth
        System.out.println("Internal: "
            + x.sourceSubSequence(0, x.getKnownLength()));

        TestStateDictionaryBuilder.tryFind(string,
            new DictionaryCharList<String>(converter.getDictionary(),
                string),
            dict, Filter.ALL);

        TestStateDictionaryBuilder.tryFind(string,
            new DictionaryCharList<String>(converter.getDictionary(),
                string),
            dict, Filter.LONGEST_MATCH);

    }
}