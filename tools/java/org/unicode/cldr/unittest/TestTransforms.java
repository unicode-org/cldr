package org.unicode.cldr.unittest;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.unicode.cldr.util.CLDRTransforms;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.text.Transliterator;

public class TestTransforms extends TestFmwk {

    public static void main(String[] args) throws Exception {
        new TestTransforms().run(args);
    }

    boolean registered = false;

    void register() {
        if (!registered) {
            CLDRTransforms.registerCldrTransforms(null, null, isVerbose() ? getLogPrintWriter() : null);
            registered = true;
        }
    }

    enum Options {transliterator, roundtrip};

    public void Test1461() {
        register();
        System.out.println("hi");

        String[][] tests = {
                { "transliterator=", "Katakana-Latin"},
                { "\u30CF \u30CF\uFF70 \u30CF\uFF9E \u30CF\uFF9F", "ha hā ba pa" },
                { "transliterator=", "Hangul-Latin"},
                { "roundtrip=", "true"},
                { "갗", "gach"},
                { "느", "neu"},
        };

        Transliterator transform = null;
        Transliterator inverse = null;
        String id = null;
        boolean roundtrip = false;
        for (String[] items : tests) {
            String source = items[0];
            String target = items[1];
            if (source.endsWith("=")) {
                switch (Options.valueOf(source.substring(0,source.length()-1).toLowerCase(Locale.ENGLISH))) {
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
    public void TestZZZ() {}
}