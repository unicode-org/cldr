package org.unicode.cldr.unittest;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRTransforms;
import org.unicode.cldr.util.Factory;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class TestTransforms extends TestFmwkPlus {
    TestInfo testInfo = TestInfo.getInstance();

    public static void main(String[] args) {
        new TestTransforms().run(args);
    }

    public void TestUzbek() {
        register();
        Transliterator cyrillicToLatin = Transliterator
            .getInstance("uz_Cyrl-uz_Latn");
        Transliterator latinToCyrillic = cyrillicToLatin.getInverse();
        // for (Transliterator t2 : t.getElements()) {
        // System.out.println(t2.getSourceSet().toPattern(false) + " => " +
        // t2.getTargetSet().toPattern(false));
        // }
        String cyrillic = "аА бБ вВ гГ ғҒ   дД ЕеЕ    ЁёЁ    жЖ зЗ иИ йЙ кК қҚ лЛ мМ нН оО пП рР сС тТ уУ ўЎ   фФ хХ ҳҲ ЦцЦ    ЧчЧ    ШшШ    бъ Ъ эЭ ЮюЮ    ЯяЯ";
        String latin = "aA bB vV gG gʻGʻ dD YeyeYE YoyoYO jJ zZ iI yY kK qQ lL mM nN oO pP rR sS tT uU oʻOʻ fF xX hH TstsTS ChchCH ShshSH bʼ ʼ eE YuyuYU YayaYA";
        UnicodeSet vowelsAndSigns = new UnicodeSet(
            "[аА еЕёЁ иИ оО уУўЎ эЭ юЮ яЯ ьЬ ъЪ]").freeze();
        UnicodeSet consonants = new UnicodeSet().addAll(cyrillic)
            .removeAll(vowelsAndSigns).remove(" ").freeze();

        // UnicodeSet englishVowels = new UnicodeSet();
        // for (String s : vowelsAndSigns) {
        // String result = cyrillicToLatin.transform(s);
        // if (!result.isEmpty()) {
        // englishVowels.add(result);
        // }
        // }
        // System.out.println(englishVowels.toPattern(false));

        String[] cyrillicSplit = cyrillic.split("\\s+");
        String[] latinSplit = latin.split("\\s+");
        for (int i = 0; i < cyrillicSplit.length; ++i) {
            assertTransformsTo("Uzbek to Latin", latinSplit[i],
                cyrillicToLatin, cyrillicSplit[i]);
            assertTransformsTo("Uzbek to Cyrillic", cyrillicSplit[i],
                latinToCyrillic, latinSplit[i]);
        }

        // # е → 'ye' at the beginning of a syllable, after a vowel, ъ or ь,
        // otherwise 'e'

        assertEquals("Uzbek to Latin", "Belgiya",
            cyrillicToLatin.transform("Бельгия"));
        UnicodeSet lower = new UnicodeSet("[:lowercase:]");
        for (String e : new UnicodeSet("[еЕ]")) {
            String ysuffix = lower.containsAll(e) ? "ye" : "YE";
            String suffix = lower.containsAll(e) ? "e" : "E";
            for (String s : vowelsAndSigns) {
                String expected = getPrefix(cyrillicToLatin, s, ysuffix);
                assertTransformsTo("Uzbek to Latin ye", expected,
                    cyrillicToLatin, s + e);
            }
            for (String s : consonants) {
                String expected = getPrefix(cyrillicToLatin, s, suffix);
                assertTransformsTo("Uzbek to Latin e", expected,
                    cyrillicToLatin, s + e);
            }
            for (String s : Arrays.asList(" ", "")) { // start of string,
                // non-letter
                String expected = getPrefix(cyrillicToLatin, s, ysuffix);
                assertTransformsTo("Uzbek to Latin ye", expected,
                    cyrillicToLatin, s + e);
            }
        }

        if (isVerbose()) {
            // Now check for correspondences
            Factory factory = testInfo.getCldrFactory();
            CLDRFile uzLatn = factory.make("uz_Latn", false);
            CLDRFile uzCyrl = factory.make("uz", false);

            Set<String> latinFromCyrillicSucceeds = new TreeSet<String>();
            Set<String> latinFromCyrillicFails = new TreeSet<String>();
            for (String path : uzCyrl) {
                String latnValue = uzLatn.getStringValue(path);
                if (latnValue == null) {
                    continue;
                }
                String cyrlValue = uzCyrl.getStringValue(path);
                if (cyrlValue == null) {
                    continue;
                }
                String latnFromCyrl = cyrillicToLatin.transform(latnValue);
                if (latnValue.equals(latnFromCyrl)) {
                    latinFromCyrillicSucceeds.add(latnValue + "\t←\t"
                        + cyrlValue);
                } else {
                    latinFromCyrillicFails.add(latnValue + "\t≠\t"
                        + latnFromCyrl + "\t←\t" + cyrlValue);
                }
            }
            logln("Success! " + latinFromCyrillicSucceeds.size() + "\n"
                + CollectionUtilities.join(latinFromCyrillicSucceeds, "\n"));
            logln("\nFAILS!" + latinFromCyrillicFails.size() + "\n"
                + CollectionUtilities.join(latinFromCyrillicFails, "\n"));
        }
    }

    private String getPrefix(Transliterator cyrillicToLatin,
        String prefixSource, String suffix) {
        String result = cyrillicToLatin.transform(prefixSource);
        if (!result.isEmpty()
            && UCharacter.getType(suffix.codePointAt(0)) != UCharacter.UPPERCASE_LETTER
            && UCharacter.getType(result.codePointAt(0)) == UCharacter.UPPERCASE_LETTER) {
            result = UCharacter.toTitleCase(result, null);
        }
        return result + suffix;
    }

    public void TestBackslashHalfwidth() throws Exception {
        register();
        // CLDRTransforms.registerCldrTransforms(null,
        // "(?i)(Fullwidth-Halfwidth|Halfwidth-Fullwidth)", isVerbose() ?
        // getLogPrintWriter() : null);
        // Transliterator.DEBUG = true;

        String input = "＼"; // FF3C
        String expected = "\\"; // 005C
        Transliterator t = Transliterator.getInstance("Fullwidth-Halfwidth");
        String output = t.transliterate(input);
        assertEquals("To Halfwidth", expected, output);

        input = "\\"; // FF3C
        expected = "＼"; // 005C
        Transliterator t2 = t.getInverse();
        output = t2.transliterate(input);
        assertEquals("To FullWidth", expected, output);
    }

    public void TestASimple() {
        Transliterator foo = Transliterator.getInstance("cs-cs_FONIPA");
    }

    boolean registered = false;

    void register() {
        if (!registered) {
            CLDRTransforms.registerCldrTransforms(null, null,
                isVerbose() ? getLogPrintWriter() : null);
            registered = true;
        }
    }

    enum Options {
        transliterator, roundtrip
    };

    public void Test1461() {
        register();

        String[][] tests = {
            { "transliterator=", "Katakana-Latin" },
            { "\u30CF \u30CF\uFF70 \u30CF\uFF9E \u30CF\uFF9F",
            "ha hā ba pa" }, { "transliterator=", "Hangul-Latin" },
            { "roundtrip=", "true" }, { "갗", "gach" }, { "느", "neu" }, };

        Transliterator transform = null;
        Transliterator inverse = null;
        String id = null;
        boolean roundtrip = false;
        for (String[] items : tests) {
            String source = items[0];
            String target = items[1];
            if (source.endsWith("=")) {
                switch (Options.valueOf(source
                    .substring(0, source.length() - 1).toLowerCase(
                        Locale.ENGLISH))) {
                        case transliterator:
                            id = target;
                            transform = Transliterator.getInstance(id);
                            inverse = Transliterator.getInstance(id,
                                Transliterator.REVERSE);
                            break;
                        case roundtrip:
                            roundtrip = target.toLowerCase(Locale.ENGLISH).charAt(0) == 't';
                            break;
                }
                continue;
            }
            String result = transform.transliterate(source);
            assertEquals(id + ":from " + source, target, result);
            if (roundtrip) {
                String result2 = inverse.transliterate(target);
                assertEquals(id + " (inv): from " + target, source, result2);
            }
        }
    }

    public void Test8921() {
        register();
        Transliterator trans = Transliterator.getInstance("Latin-ASCII");
        assertEquals("Test8921", "Kornil'ev Kirill",
            trans.transliterate("Kornilʹev Kirill"));
    }

    public void TestData() {
        register();
        try {
            // get the folder name
            String name = TestTransforms.class.getResource(".").toString();
            if (!name.startsWith("file:")) {
                throw new IllegalArgumentException("Internal Error");
            }
            name = name.substring(5);
            File fileDirectory = new File(name
                + "/../unittest/data/transformtest/");
            String fileDirectoryName = fileDirectory.getCanonicalPath(); // TODO:
            // use
            // resource,
            // not
            // raw
            // file
            logln("Testing files in: " + fileDirectoryName);

            for (String file : fileDirectory.list()) {
                if (!file.endsWith(".txt")) {
                    continue;
                }
                logln("Testing file: " + file);
                String transName = file.substring(0, file.length() - 4);
                Transliterator trans = Transliterator.getInstance(transName);

                BufferedReader in = BagFormatter.openUTF8Reader(
                    fileDirectoryName, file);
                int counter = 0;
                while (true) {
                    String line = in.readLine();
                    if (line == null)
                        break;
                    line = line.trim();
                    if (line.startsWith("#")) {
                        continue;
                    }
                    String[] parts = line.split("\t");
                    String source = parts[0];
                    String expected = parts[1];
                    String result = trans.transform(source);
                    assertEquals(transName + " " + (++counter) + " Transform "
                        + source, expected, result);
                }
                in.close();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    enum Casing {
        Upper, Title, Lower
    }

    public void TestCasing() {
        register();
        String greekSource = "ΟΔΌΣ Οδός Σο ΣΟ oΣ ΟΣ σ ἕξ";
        // Transliterator.DEBUG = true;
        Transliterator elTitle = checkString("el", Casing.Title,
            "Οδός Οδός Σο Σο Oς Ος Σ Ἕξ", greekSource, true);
        Transliterator elLower = checkString("el", Casing.Lower,
            "οδός οδός σο σο oς ος σ ἕξ", greekSource, true);
        Transliterator elUpper = checkString("el", Casing.Upper,
            "ΟΔΟΣ ΟΔΟΣ ΣΟ ΣΟ OΣ ΟΣ Σ ΕΞ", greekSource, false);

        String turkishSource = "Isiİ İsıI";
        Transliterator trTitle = checkString("tr", Casing.Title, "Isii İsıı",
            turkishSource, true);
        Transliterator trLower = checkString("tr", Casing.Lower, "ısii isıı",
            turkishSource, true);
        Transliterator trUpper = checkString("tr", Casing.Upper, "ISİİ İSII",
            turkishSource, true);
        Transliterator azTitle = checkString("az", Casing.Title, "Isii İsıı",
            turkishSource, true);
        Transliterator azLower = checkString("az", Casing.Lower, "ısii isıı",
            turkishSource, true);
        Transliterator azUpper = checkString("az", Casing.Upper, "ISİİ İSII",
            turkishSource, true);

        String lithuanianSource = "I \u00CF J J\u0308 \u012E \u012E\u0308 \u00CC \u00CD \u0128 xi\u0307\u0308 xj\u0307\u0308 x\u012F\u0307\u0308 xi\u0307\u0300 xi\u0307\u0301 xi\u0307\u0303 XI X\u00CF XJ XJ\u0308 X\u012E X\u012E\u0308";
        if (!logKnownIssue("11094",
            "Fix ICU4J UCharacter.toTitleCase/toLowerCase for lt")) {
            Transliterator ltTitle = checkString(
                "lt",
                Casing.Title,
                "I \u00CF J J\u0308 \u012E \u012E\u0308 \u00CC \u00CD \u0128 Xi\u0307\u0308 Xj\u0307\u0308 X\u012F\u0307\u0308 Xi\u0307\u0300 Xi\u0307\u0301 Xi\u0307\u0303 Xi Xi\u0307\u0308 Xj Xj\u0307\u0308 X\u012F X\u012F\u0307\u0308",
                lithuanianSource, true);
            Transliterator ltLower = checkString(
                "lt",
                Casing.Lower,
                "i i\u0307\u0308 j j\u0307\u0308 \u012F \u012F\u0307\u0308 i\u0307\u0300 i\u0307\u0301 i\u0307\u0303 xi\u0307\u0308 xj\u0307\u0308 x\u012F\u0307\u0308 xi\u0307\u0300 xi\u0307\u0301 xi\u0307\u0303 xi xi\u0307\u0308 xj xj\u0307\u0308 x\u012F x\u012F\u0307\u0308",
                lithuanianSource, true);
        }
        Transliterator ltUpper = checkString(
            "lt",
            Casing.Upper,
            "I \u00CF J J\u0308 \u012E \u012E\u0308 \u00CC \u00CD \u0128 X\u00CF XJ\u0308 X\u012E\u0308 X\u00CC X\u00CD X\u0128 XI X\u00CF XJ XJ\u0308 X\u012E X\u012E\u0308",
            lithuanianSource, true);

        String dutchSource = "IJKIJ ijkij IjkIj";
        Transliterator nlTitle = checkString("nl", Casing.Title,
            "IJkij IJkij IJkij", dutchSource, true);
        // Transliterator nlLower = checkString("nl", Casing.Lower, "ısii isıı",
        // turkishSource);
        // Transliterator nlUpper = checkString("tr", Casing.Upper, "ISİİ İSII",
        // turkishSource);
    }

    private Transliterator checkString(String locale, Casing casing,
        String expected, String source, boolean sameAsSpecialCasing) {
        Transliterator translit = Transliterator.getInstance(locale + "-"
            + casing);
        String result = checkString(locale, expected, source, translit);
        ULocale ulocale = new ULocale(locale);
        String specialCasing;
        Normalizer2 normNFC = Normalizer2.getNFCInstance(); // UCharacter.toXxxCase
        // doesn't
        // normalize,
        // Transliterator
        // does
        switch (casing) {
        case Upper:
            specialCasing = normNFC.normalize(UCharacter.toUpperCase(ulocale,
                source));
            break;
        case Title:
            specialCasing = normNFC.normalize(UCharacter.toTitleCase(ulocale,
                source, null));
            break;
        case Lower:
            specialCasing = normNFC.normalize(UCharacter.toLowerCase(ulocale,
                source));
            break;
        default:
            throw new IllegalArgumentException();
        }
        if (sameAsSpecialCasing) {
            if (!assertEquals(locale + "-" + casing + " Vs SpecialCasing",
                specialCasing, result)) {
                showFirstDifference("Special: ", specialCasing, "Transform: ",
                    result);
            }
        } else {
            assertNotEquals(locale + "-" + casing + "Vs SpecialCasing",
                specialCasing, result);
        }
        return translit;
    }

    private String checkString(String locale, String expected, String source,
        Transliterator translit) {
        String transformed = translit.transform(source);
        if (!assertEquals(locale, expected, transformed)) {
            showTransliterator(translit);
        }
        return transformed;
    }

    private void showFirstDifference(String titleA, String a, String titleB,
        String b) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < Math.min(a.length(), b.length()); ++i) {
            char aChar = a.charAt(i);
            char bChar = b.charAt(i);
            if (aChar == bChar) {
                buffer.append(aChar);
            } else {
                errln("\t" + buffer + "\n\t\t" + titleA + "\t"
                    + Utility.hex(a.substring(i)) + "\n\t\t" + titleB
                    + "\t" + Utility.hex(b.substring(i)));
                return;
            }
        }
        errln("different length");
    }

    private void showTransliterator(Transliterator t) {
        org.unicode.cldr.test.TestTransforms.showTransliterator("", t, 999);
    }
}
