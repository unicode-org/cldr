package org.unicode.cldr.draft;

import java.util.Map;
import java.util.TreeMap;

import org.unicode.cldr.util.Timer;

import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UnicodeSet;

public class NormalizedIdentifierParser {
    enum Status {
        NotNameChar, NameContinue, Name, UNKNOWN, DONE
    }

    enum CharType {
        Illegal, NameContinueNFC, NameContinueOther, NameStartNFC, NameStartOther, Whitespace, Other
    }

    static final UnicodeSet XmlNameStartChar = (UnicodeSet) new UnicodeSet("[\\: A-Z _ a-z " +
        "\\u00C0-\\u00D6 \\u00D8-\\u00F6 \\u00F8-\\u02FF \\u0370-\\u037D \\u037F-\\u1FFF \\u200C-\\u200D" +
        "\\u2070-\\u218F \\u2C00-\\u2FEF \\u3001-\\uD7FF \\uF900-\\uFDCF \\uFDF0-\\uFFFD \\U00010000-\\U000EFFFF]")
            .freeze();
    static final UnicodeSet XmlNameContinueChar = (UnicodeSet) new UnicodeSet(
        "[- . 0-9 \\u00B7 \\u0300-\\u036F \\u203F-\\u2040]").freeze();
    static final UnicodeSet XmlNameChar = (UnicodeSet) new UnicodeSet(XmlNameStartChar).addAll(XmlNameContinueChar)
        .freeze();
    static final UnicodeSet XmlWhiteSpace = (UnicodeSet) new UnicodeSet("[\\u0009\\u000D\\u000A\\u0020]").freeze();
    static final UnicodeSet XmlIllegal = (UnicodeSet) new UnicodeSet(
        "[^\\u0009\\u000D\\u000A\\u0020-\uD7FF\\uE000-\\uFFFD\\U00010000-\\U000EFFFF]").freeze();
    static final UnicodeSet NfcSafe = (UnicodeSet) new UnicodeSet("[:nfkcqc=yes:]").freeze();

    private String input;
    private int endPosition;
    private int startPosition;
    private Status status;
    private boolean knownNfc;

    public NormalizedIdentifierParser set(String input, int position) {
        this.input = input;
        startPosition = endPosition = position;
        status = Status.UNKNOWN;
        return this;
    }

    public Status next() {
        startPosition = endPosition;
        if (endPosition >= input.length()) {
            return status = Status.DONE;
        }
        // since the vast majority of characters by frequency are Latin-1 (<FF),
        // this can also be optimized by having a special loop for Latin-1
        // and only dropping into the full check if a non-Latin-1 characters

        // check the first character specially
        int codePoint = input.codePointAt(endPosition);
        CharType type = getCharType(codePoint);
        endPosition += codePoint < 0x10000 ? 1 : 2;
        switch (type) {
        case NameStartNFC:
            knownNfc = true;
            status = Status.Name;
            break;
        case NameStartOther:
            knownNfc = false;
            status = Status.Name;
            break;
        case NameContinueNFC:
            knownNfc = true;
            status = Status.NameContinue;
            break;
        case NameContinueOther:
            knownNfc = false;
            status = Status.NameContinue;
            break;
        default:
            knownNfc = NfcSafe.contains(codePoint); // we don't care about the value, so production code wouldn't check
            return Status.NotNameChar;
        }

        loop: while (endPosition < input.length()) {
            codePoint = input.codePointAt(endPosition);
            type = getCharType(codePoint);
            switch (type) {
            case NameStartOther:
            case NameContinueOther:
                knownNfc = false;
                break;
            case NameStartNFC:
            case NameContinueNFC:
                break;
            default:
                break loop;
            }
            endPosition += codePoint < 0x10000 ? 1 : 2;
        }
        return status;
    }

    public CharType getCharType(int codePoint) {
        // Normally this would just be a trie lookup, but we simulate it here
        if (XmlNameContinueChar.contains(codePoint)) {
            return NfcSafe.contains(codePoint) ? CharType.NameContinueNFC : CharType.NameContinueOther;
        } else if (XmlNameStartChar.contains(codePoint)) {
            return NfcSafe.contains(codePoint) ? CharType.NameStartNFC : CharType.NameStartOther;
        } else if (XmlIllegal.contains(codePoint)) {
            return CharType.Illegal;
        } else if (XmlWhiteSpace.contains(codePoint)) {
            return CharType.Whitespace;
        } else {
            return CharType.Other;
        }
    }

    public String getToken() {
        return input.substring(startPosition, endPosition);
    }

    public Status getStatus() {
        return status;
    }

    public int getEndPosition() {
        return endPosition;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public boolean isKnownNfc() {
        return knownNfc;
    }

    Status getIdStatus(String s) {
        set(s, 0).next();
        if (endPosition != s.length()) {
            return Status.NotNameChar;
        }
        return getStatus();
    }

    static void showDiffs() {
        Map<Status, Map<Status, UnicodeSet>> map = new TreeMap<Status, Map<Status, UnicodeSet>>();
        NormalizedIdentifierParser parser = new NormalizedIdentifierParser();
        for (int codePoint = 0; codePoint <= 0x10FFFF; ++codePoint) {
            String source = new StringBuilder().appendCodePoint(codePoint).toString();
            Status sourceStatus = parser.getIdStatus(source);
            String target = Normalizer.normalize(codePoint, Normalizer.NFC);
            Status targetStatus = parser.getIdStatus(target);
            if (sourceStatus == targetStatus) {
                continue;
            }
            Map<Status, UnicodeSet> map2 = map.get(sourceStatus);
            if (map2 == null) {
                map.put(sourceStatus, map2 = new TreeMap<Status, UnicodeSet>());
            }
            UnicodeSet set = map2.get(targetStatus);
            if (set == null) {
                map2.put(targetStatus, set = new UnicodeSet());
            }
            set.add(codePoint);
        }
        for (Status sourceStatus : map.keySet()) {
            Map<Status, UnicodeSet> map2 = map.get(sourceStatus);
            for (Status targetStatus : map2.keySet()) {
                UnicodeSet set = map2.get(targetStatus);
                System.out.println(sourceStatus + "\t=>\t" + targetStatus + "\t" + set);
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("NameStart: " + XmlNameStartChar);
        System.out.println("NameContinue: " + XmlNameContinueChar);
        System.out.println("Whitespace: " + XmlWhiteSpace);
        System.out.println("Illegal: " + XmlIllegal);

        compareNormalizer();

        NormalizedIdentifierParser parser = new NormalizedIdentifierParser();
        parser.set("\u0308ghi)j\u0308$abc+def*(", 0);
        for (Status status = parser.next(); status != Status.DONE; status = parser.next()) {
            System.out.println(status + ": \t" + parser.getToken() + (!parser.isKnownNfc() ? "\tNot Known NFC!" : ""));
        }
    }

    private static void compareNormalizer() {
        final int iterations = 100000000;
        compareNormalizer("nörmalization", iterations);
        compareNormalizer("No\u0308rmalization", iterations);
    }

    private static void compareNormalizer(String test, int iterations) {
        String s;
        s = test.toLowerCase();
        s = Normalizer.normalize(test, Normalizer.NFC);
        Timer timer = new Timer();
        timer.start();
        for (int i = 0; i < iterations; ++i) {
            s = test.toLowerCase();
        }
        timer.stop();
        final long lowercaseDuration = timer.getDuration();
        System.out.println("Java Lowercasing: " + lowercaseDuration * 1000.0d / iterations
            + "µs; for " + test);

        timer.start();
        for (int i = 0; i < iterations; ++i) {
            s = Normalizer.normalize(test, Normalizer.NFC);
        }
        timer.stop();
        final long nfcDuration = timer.getDuration();
        System.out.println("ICU Normalizing: " + nfcDuration * 1000.0d / iterations
            + "µs = " + (nfcDuration * 100.0d / lowercaseDuration - 1)
            + "%; for " + test);
    }
}
