package org.unicode.cldr.unittest;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.unicode.cldr.util.CLDRTransforms;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.text.Transliterator;

public class TestTransforms extends TestFmwk {

    public static void main(String[] args) throws Exception {
        new TestTransforms().run(args);
    }

    public void TestBackslashHalfwidth() throws Exception {
        register();
        // CLDRTransforms.registerCldrTransforms(null, "(?i)(Fullwidth-Halfwidth|Halfwidth-Fullwidth)", isVerbose() ?
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
            CLDRTransforms.registerCldrTransforms(null, null, isVerbose() ? getLogPrintWriter() : null);
            registered = true;
        }
    }

    enum Options {
        transliterator, roundtrip
    };

    public void Test1461() {
        register();
        System.out.println("hi");

        String[][] tests = {
            { "transliterator=", "Katakana-Latin" },
            { "\u30CF \u30CF\uFF70 \u30CF\uFF9E \u30CF\uFF9F", "ha hā ba pa" },
            { "transliterator=", "Hangul-Latin" },
            { "roundtrip=", "true" },
            { "갗", "gach" },
            { "느", "neu" },
        };

        Transliterator transform = null;
        Transliterator inverse = null;
        String id = null;
        boolean roundtrip = false;
        for (String[] items : tests) {
            String source = items[0];
            String target = items[1];
            if (source.endsWith("=")) {
                switch (Options.valueOf(source.substring(0, source.length() - 1).toLowerCase(Locale.ENGLISH))) {
                case transliterator:
                    id = target;
                    transform = Transliterator.getInstance(id);
                    inverse = Transliterator.getInstance(id, Transliterator.REVERSE);
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

    public void TestData() {
        register();
        try {
            // get the folder name
            String name = TestTransforms.class.getResource(".").toString();
            if (!name.startsWith("file:")) {
                throw new IllegalArgumentException("Internal Error");
            }
            name = name.substring(5);
            File fileDirectory = new File(name + "/../util/data/test/");
            String fileDirectoryName = fileDirectory.getCanonicalPath();
            logln("Testing files in: " + fileDirectoryName);

            for (String file : fileDirectory.list()) {
                if (!file.endsWith(".txt")) {
                    continue;
                }
                logln("Testing file: " + file);
                String transName = file.substring(0, file.length() - 4);
                Transliterator trans = Transliterator.getInstance(transName);

                BufferedReader in = BagFormatter.openUTF8Reader(fileDirectoryName, file);
                int counter = 0;
                while (true) {
                    String line = in.readLine();
                    if (line == null) break;
                    line = line.trim();
                    if (line.startsWith("#")) {
                        continue;
                    }
                    String[] parts = line.split("\t");
                    String source = parts[0];
                    String expected = parts[1];
                    String result = trans.transform(source);
                    assertEquals(transName + " " + (++counter) + " Transform " + source, expected, result);
                }
                in.close();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void TestCasing() {
        register();
        String greekSource = "ΟΔΌΣ Οδός Σο ΣΟ oΣ ΟΣ σ ἕξ";
        // Transliterator.DEBUG = true;
        Transliterator elTitle = checkString("el-Title", "Οδός Οδός Σο Σο Oς Ος Σ Ἕξ", greekSource);
        Transliterator elLower = checkString("el-Lower", "οδός οδός σο σο oς ος σ ἕξ", greekSource);
        Transliterator elUpper = checkString("el-Upper", "ΟΔΟΣ ΟΔΟΣ ΣΟ ΣΟ OΣ ΟΣ Σ ΕΞ", greekSource);

        String turkishSource = "Isiİ İsıI";
        Transliterator trTitle = checkString("tr-Title", "Isii İsıı", turkishSource);
        Transliterator trLower = checkString("tr-Lower", "ısii isıı", turkishSource);
        Transliterator trUpper = checkString("tr-Upper", "ISİİ İSII", turkishSource);
        Transliterator azTitle = checkString("az-Title", "Isii İsıı", turkishSource);
        Transliterator azLower = checkString("az-Lower", "ısii isıı", turkishSource);
        Transliterator azUpper = checkString("az-Upper", "ISİİ İSII", turkishSource);

        String lituanianSource = "I Ï J J̈ Į Į̈ Ì Í Ĩ xi̇̈ xj̇̈ xį̇̈ xi̇̀ xi̇́ xi̇̃ XI XÏ XJ XJ̈ XĮ XĮ̈";
        Transliterator ltTitle = checkString("lt-Title",
            "I Ï J J̈ Į Į̈ Ì Í Ĩ Xi̇̈ Xj̇̈ Xį̇̈ Xi̇̀ Xi̇́ Xi̇̃ Xi Xi̇̈ Xj Xj̇̈ Xį Xį̇̈", lituanianSource);
        Transliterator ltLower = checkString("lt-Lower",
            "i i̇̈ j j̇̈ į į̇̈ i̇̀ i̇́ i̇̃ xi̇̈ xj̇̈ xį̇̈ xi̇̀ xi̇́ xi̇̃ xi xi̇̈ xj xj̇̈ xį xį̇̈", lituanianSource);
        Transliterator ltUpper = checkString("lt-Upper", "I Ï J J̈ Į Į̈ Ì Í Ĩ XÏ XJ̈ XĮ̈ XÌ XÍ XĨ XI XÏ XJ XJ̈ XĮ XĮ̈",
            lituanianSource);

    }

    private Transliterator checkString(String id, String expected, String source) {
        Transliterator elLower = Transliterator.getInstance(id);
        return checkString(id, expected, source, elLower);
    }

    private Transliterator checkString(String id, String expected, String source, Transliterator translit) {
        if (!assertEquals(id, expected, translit.transform(source))) {
            showTransliterator(translit);
        }
        return translit;
    }

    private void showTransliterator(Transliterator t) {
        org.unicode.cldr.test.TestTransforms.showTransliterator("", t, 999);
    }

    public void TestZZZ() {
    }
}