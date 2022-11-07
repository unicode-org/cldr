package org.unicode.cldr.unittest;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTransforms;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PathUtilities;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.UnicodeRelation;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterEnums.ECharacterCategory;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class TestTransforms extends TestFmwkPlus {
    CLDRConfig testInfo = CLDRConfig.getInstance();

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

            Set<String> latinFromCyrillicSucceeds = new TreeSet<>();
            Set<String> latinFromCyrillicFails = new TreeSet<>();
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
                + Joiner.on("\n").join(latinFromCyrillicSucceeds));
            logln("\nFAILS!" + latinFromCyrillicFails.size() + "\n"
                + Joiner.on("\n").join(latinFromCyrillicFails));
        }
    }

    private String getPrefix(Transliterator cyrillicToLatin,
        String prefixSource, String suffix) {
        String result = cyrillicToLatin.transform(prefixSource);
        if (!result.isEmpty()
            && UCharacter.getType(suffix.codePointAt(0)) != ECharacterCategory.UPPERCASE_LETTER
            && UCharacter.getType(result.codePointAt(0)) == ECharacterCategory.UPPERCASE_LETTER) {
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
                isVerbose() ? getLogPrintWriter() : null, true);
            registered = true;
        }
    }

    enum Options {
        transliterator, roundtrip
    }

    private String makeLegacyTransformID(String source, String target, String variant) {
        if (variant != null) {
            return source + "-" + target + "/" + variant;
        } else {
            return source + "-" + target;
        }
    }

    private void checkTransformID(String id, File file) {
        if (id.indexOf("-t-") > 0) {
            String expected = ULocale.forLanguageTag(id).toLanguageTag();
            if (!id.equals(expected)) {
                errln(file.getName() + ": BCP47-T identifier \"" +
                    id + "\" should be \"" + expected + "\"");
            }
        }
    }

    private void addTransformID(String id, File file, Map<String, File> ids) {
        File oldFile = ids.get(id);
        if (oldFile == null || oldFile.equals(file)) {
            ids.put(id, file);
        } else {
            errln(file.getName() + ": Transform \"" + id +
                "\" already defined in " + oldFile.getName());
        }
    }

    private void addTransformIDs(File file, XPathParts parts, int element, Map<String, File> ids) {
        String source = parts.getAttributeValue(element, "source");
        String target = parts.getAttributeValue(element, "target");
        String variant = parts.getAttributeValue(element, "variant");
        String direction = parts.getAttributeValue(element, "direction");

        if (source != null && target != null) {
            if ("forward".equals(direction)) {
                addTransformID(makeLegacyTransformID(source, target, variant), file, ids);
            } else if ("both".equals(direction)) {
                addTransformID(makeLegacyTransformID(source, target, variant), file, ids);
                addTransformID(makeLegacyTransformID(target, source, variant), file, ids);
            }
        }

        String alias = parts.getAttributeValue(element, "alias");
        if (alias != null) {
            for (String id : alias.split("\\s+")) {
                addTransformID(id, file, ids);
            }
        }

        String backwardAlias = parts.getAttributeValue(element, "backwardAlias");
        if (backwardAlias != null) {
            if (!"both".equals(direction)) {
                errln(file.getName() + ": Expected direction=\"both\" " +
                    "when backwardAlias is present");
            }

            for (String id : backwardAlias.split("\\s+")) {
                addTransformID(id, file, ids);
            }
        }
    }

    private Map<String, File> getTransformIDs(String transformsDirectoryPath) {
        Map<String, File> ids = new HashMap<>();
        File dir = new File(transformsDirectoryPath);
        if (!dir.exists()) {
            errln("Cannot find transforms directory at " + transformsDirectoryPath);
            return ids;
        }

        for (File file : dir.listFiles()) {
            if (!file.getName().endsWith(".xml")) {
                continue;
            }
            List<Pair<String, String>> data = new ArrayList<>();
            XMLFileReader.loadPathValues(file.getPath(), data, true);
            for (Pair<String, String> entry : data) {
                final String xpath = entry.getFirst();
                if (xpath.startsWith("//supplementalData/transforms/transform[")) {
                    String fileName = file.getName();
                    XPathParts parts = XPathParts.getFrozenInstance(xpath);
                    addTransformIDs(file, parts, 2, ids);
                }
            }
        }
        return ids;
    }

    final ImmutableSet<String> OK_MISSING_FROM_OLD = ImmutableSet.of("und-Sarb-t-und-ethi",
        "Ethi-Sarb", "und-Ethi-t-und-latn", "Musnad-Ethiopic", "und-Ethi-t-und-sarb",
        "Sarb-Ethi", "Ethiopic-Musnad");

    public void TestTransformIDs() {
        Map<String, File> transforms = getTransformIDs(CLDRPaths.TRANSFORMS_DIRECTORY);
        for (Map.Entry<String, File> entry : transforms.entrySet()) {
            checkTransformID(entry.getKey(), entry.getValue());
        }

        // Only run the rest in exhaustive mode since it requires CLDR_ARCHIVE_DIRECTORY.
        if (getInclusion() <= 5) {
            return;
        }

        Set<String> removedTransforms = new HashSet<>();
        removedTransforms.add("ASCII-Latin"); // http://unicode.org/cldr/trac/ticket/9163

        Map<String, File> oldTransforms = getTransformIDs(CLDRPaths.LAST_TRANSFORMS_DIRECTORY);
        for (Map.Entry<String, File> entry : oldTransforms.entrySet()) {
            String id = entry.getKey();
            if (!transforms.containsKey(id)
                && !removedTransforms.contains(id)
                && !OK_MISSING_FROM_OLD.contains(id)) {
                File oldFile = entry.getValue();
                errln("Missing transform \"" + id +
                    "\"; the previous CLDR release had defined it in " + oldFile.getName());
            }
        }
    }

    public void Test1461() {
        register();

        String[][] tests = {
            { "transliterator=", "Katakana-Latin" },
            { "\u30CF \u30CF\uFF70 \u30CF\uFF9E \u30CF\uFF9F",
            "ha hā ba pa" },
            { "transliterator=", "Hangul-Latin" },
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

    private Pattern rfc6497Pattern = Pattern.compile("([a-zA-Z0-9-]+)-t-([a-zA-Z0-9-]+?)(?:-m0-([a-zA-Z0-9-]+))?");

    // cs-fonipa --> cs_fonipa; und-deva --> deva
    // TODO: Remove this workaround once ICU supports BCP47-T identifiers.
    // http://bugs.icu-project.org/trac/ticket/12599
    private String getLegacyCode(String code) {
        code = code.replace('-', '_');
        if (code.startsWith("und_") && code.length() == 8) {
            code = code.substring(4);
        }
        return code;
    }

    private Transliterator getTransliterator(String id) {
        return Transliterator.getInstance(getOldTranslitId(id));
    }

    private String getOldTranslitId(String id) {
        // TODO: Pass unmodified transform name to ICU, once
        // ICU can handle transform identifiers according to
        // BCP47 Extension T (RFC 6497). The rewriting below
        // is just a temporary workaround, allowing us to use
        // BCP47-T identifiers for naming test data files.
        // http://bugs.icu-project.org/trac/ticket/12599
        if (id.equalsIgnoreCase("und-t-d0-publish")) {
            return ("Any-Publishing");
        } else if (id.equalsIgnoreCase("und-t-s0-publish")) {
            return ("Publishing-Any");
        } else if (id.equalsIgnoreCase("de-t-de-d0-ascii")) {
            return ("de-ASCII");
        } else if (id.equalsIgnoreCase("my-t-my-s0-zawgyi")) {
            return ("Zawgyi-my");
        } else if (id.equalsIgnoreCase("my-t-my-d0-zawgyi")) {
            return "my-Zawgyi";
        } else if (id.equalsIgnoreCase("und-t-d0-ascii")) {
            return ("Latin-ASCII");
        }

        Matcher rfc6497Matcher = rfc6497Pattern.matcher(id);
        if (rfc6497Matcher.matches()) {
            String targetLanguage = getLegacyCode(rfc6497Matcher.group(1));
            String originalLanguage = getLegacyCode(rfc6497Matcher.group(2));
            String mechanism = rfc6497Matcher.group(3);
            id = originalLanguage + "-" + targetLanguage;
            if (mechanism != null && !mechanism.isEmpty()) {
                id += "/" + mechanism.replace('-', '_');
            }
        }
        return id;
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
            File fileDirectory = new File(CLDRPaths.TEST_DATA + "transforms/");
            String fileDirectoryName = PathUtilities.getNormalizedPathString(fileDirectory);
            assertTrue(fileDirectoryName, fileDirectory.exists());

            logln("Testing files in: " + fileDirectoryName);

            Set<String> foundTranslitsLower = new TreeSet();

            for (String file : fileDirectory.list()) {
                if (!file.endsWith(".txt") || file.startsWith("_readme")) {
                    continue;
                }
                logln("Testing file: " + file);
                String transName = file.substring(0, file.length() - 4);
                if (transName.equals("ka-Latn-t-ka-m0-bgn")) {
                    logKnownIssue("cldrbug:10566", "Jenkins build failing on translit problem");
                    continue; // failures like the following need to be fixed first.
                    // Error: (TestTransforms.java:434) : ka-Latn-t-ka-m0-bgn 2 Transform უფლება: expected "up’leba", got "upleba"
                }

                Transliterator trans = getTransliterator(transName);
                String id = trans.getID().toLowerCase(Locale.ROOT);
                foundTranslitsLower.add(id);

                BufferedReader in = FileUtilities.openUTF8Reader(fileDirectoryName, file);
                int counter = 0;
                while (true) {
                    String line = in.readLine();
                    if (line == null)
                        break;
                    line = line.trim();
                    counter += 1;
                    if (line.startsWith("#")) {
                        continue;
                    }
                    String[] parts = line.split("\t");
                    String source = parts[0];
                    String expected = parts[1];
                    String result = trans.transform(source);
                    assertEquals(transName + " " + counter + " Transform "
                        + source, expected, result);
                }
                in.close();
            }
            Set<String> allTranslitsLower = oldEnumConvertLower(Transliterator.getAvailableIDs(), new TreeSet<>());
            // see which are missing tests
            for (String s : allTranslitsLower) {
                if (!foundTranslitsLower.contains(s)) {
                    warnln("Translit with no test file:\t" + s);
                }
            }

            // all must be superset of found tests
            for (String s : foundTranslitsLower) {
                if (!allTranslitsLower.contains(s)) {
                    warnln("Test file with no translit:\t" + s);
                }
            }

        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private <T, U extends Collection<T>> U oldEnumConvert(Enumeration<T> source, U target) {
        while (source.hasMoreElements()) {
            target.add(source.nextElement());
        }
        return target;
    }

    private <U extends Collection<String>> U oldEnumConvertLower(Enumeration<String> source, U target) {
        while (source.hasMoreElements()) {
            target.add(source.nextElement().toLowerCase(Locale.ROOT));
        }
        return target;
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
            "ΟΔΟΣ ΟΔΟΣ ΣΟ ΣΟ OΣ ΟΣ Σ ΕΞ", greekSource, true); // now true due to ICU #5456

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
        // The following test was formerly skipped with
        // !logKnownIssue("11094", "Fix ICU4J UCharacter.toTitleCase/toLowerCase for lt").
        // However [https://unicode-org.atlassian.net/browse/ICU-11094] is supposedly
        // fixed in the version of ICU4J currently in CLDR, but removing the logKnownIssue
        // to execute the test results in test failures, mainly for  i\u0307\u0308.
        // So I am changing the logKnownIssue to reference a CLDR ticket about
        // investigating the test (it may be wrong).
        if (!logKnownIssue("cldrbug:13313",
            "Investigate the Lithuanian casing test, it may be wrong")) {
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

    public void Test9925() {
        register();
        Transliterator pinyin = getTransliterator("und-Latn-t-und-hani");
        assertEquals("賈 bug", "jiǎ", pinyin.transform("賈"));
    }

    public void TestHiraKata() { // for CLDR-13127 and ...
        register();
        Transliterator hiraKata = getTransliterator("Hiragana-Katakana");
        assertEquals("Hira-Kata", hiraKata.transform("゛゜ わ゙ ゟ"), "゛゜ ヷ ヨリ");
    }

    public void TestZawgyiToUnicode10899() {
        // Some tests for the transformation of Zawgyi font encoding to Unicode Burmese.
        Transliterator z2u = getTransliterator("my-t-my-s0-zawgyi");

        String z1 =
            "\u1021\u102C\u100F\u102C\u1015\u102D\u102F\u1004\u1039\u1031\u1010\u103C";
        String expected =
            "\u1021\u102C\u100F\u102C\u1015\u102D\u102F\u1004\u103A\u1010\u103D\u1031";

        String actual = z2u.transform(z1);

        assertEquals("z1 to u1", expected, actual);

        String z2 = "တကယ္ဆို အျငိႈးေတြမဲ႔ေသာလမ္းေသာလမ္းမွာ တိုႈျပန္ဆံုျကတဲ႔အခါ ";
        expected = "တကယ်ဆို အငြှိုးတွေမဲ့သောလမ်းသောလမ်းမှာ တှိုပြန်ဆုံကြတဲ့အခါ ";
        actual = z2u.transform(z2);
        assertEquals("z2 to u2", expected, actual);

        String z3 = "ျပန္လမ္းမဲ့ကၽြန္းအပိုင္း၄";
        expected = "ပြန်လမ်းမဲ့ကျွန်းအပိုင်း၎";
        actual = z2u.transform(z3);
        assertEquals("z3 to u3", expected, actual);
    }

    public void TestUnicodeToZawgyi111107() {
        // Some tests for the transformation from Unicode to Zawgyi font encoding
        Transliterator u2z = getTransliterator("my-t-my-d0-zawgyi");

        String expected =
            "\u1021\u102C\u100F\u102C\u1015\u102D\u102F\u1004\u1039\u1031\u1010\u103C";
        String u1 =
            "\u1021\u102C\u100F\u102C\u1015\u102D\u102F\u1004\u103A\u1010\u103D\u1031";

        String actual = u2z.transform(u1);

        assertEquals("u1 to z1", expected, actual);

        expected = "တကယ္ဆို အၿငႇိဳးေတြမဲ့ေသာလမ္းေသာလမ္းမွာ တိႈျပန္ဆံုၾကတဲ့အခါ ";
        String u2 = "တကယ်ဆို အငြှိုးတွေမဲ့သောလမ်းသောလမ်းမှာ တှိုပြန်ဆုံကြတဲ့အခါ ";
        actual = u2z.transform(u2);
        assertEquals("u2 to z2", expected, actual);

        expected = "ျပန္လမ္းမဲ့ကြၽန္းအပိုင္း၄";
        String u3 = "ပြန်လမ်းမဲ့ကျွန်းအပိုင်း၎";
        actual = u2z.transform(u3);
        assertEquals("u3 to z3", expected, actual);
    }

    static final boolean QUICKTEST = true; // System.getProperty("TestTransforms:QUICKTEST") != null;

    public void TestLocales() {
        Set<String> modernCldr = StandardCodes.make().getLocaleCoverageLocales(Organization.cldr, ImmutableSet.of(Level.MODERN));
        Set<String> special = StandardCodes.make().getLocaleCoverageLocales(Organization.special, ImmutableSet.of(Level.MODERN));
        Factory factory = CLDRConfig.getInstance().getCommonAndSeedAndMainAndAnnotationsFactory();
        Set<String> missing = new TreeSet<>();
        SampleDataSet badPlusSample = new SampleDataSet();
        SampleDataSet allMissing = new SampleDataSet();

        // QUICKTEST is used to just register particular items, for faster development testing
        if (QUICKTEST) {
            warnln(" — Quicktest");
            String sinhalaTest = "මානව අයිතිවාසිකම් පිළිබඳ විශ්ව ප්‍රකාශනය\n"
                + "1948 දෙසැම්බර් මස 10 වෙනි දින එක්සත් ජාතීන්ගේ මහා මණ්ඩලයෙන් සම්මත කරනු ලදුව ප්‍රකාශයට පත් කළ මානව අයිතිවාසිකම් පිළිබඳ විශ්ව ප්‍රකාශනය මෙහි අන්තර්ගත වේ. මෙම ඓතිහාසික සිද්ධියෙන් මෙම ප්‍රකාශනයේ අඩංගු වගන්ති ප්‍රචාරයට පත් කරන මෙන් ද “ඒ ඒ රටවල පවතින දේශපාලන තත්ත්වය පිළිබඳව වෙනසක් නොකර මෙම ප්‍රකාශනය ප්‍රධාන වශයෙන් පාසල් හා අධ්‍යාපන ආයතනයන් මඟින් පැතිර වීමට, ප්‍රදර්ශනය වීමට, පාඨනය කරවීමට හා පැහැදිලි කරවීමට සලස්වන මෙන්ද” එක්සත් ජාතීන්ගේ මහා මණ්ඩලය විසින් සියලුම සාමාජික රාජ්‍යයන් ගෙන් ඉල්ලා සිටින ලද්දේය."
                ;
            String khmerTest = "សេចក្ដីប្រកាសជាសកលស្ដីពីសិទ្ធិមនុស្ស\n"
                + "អនុម័តនិងប្រកាសដោយសេចក្ដីសម្រេចចិត្ដនៃមហាសន្និបាតលេខ ២១៧ A (III) នៅថ្ងៃទី ១០ ខែធ្នូ ឆ្នាំ១៩៤៨\n"
                + "បុព្វកថា\n"
                + "ដោយយល់ឃើញថា ការទទួលស្គាល់សេចក្ដីថ្លៃថ្នូរជាប់ពីកំណើត និងសិទ្ធិស្មើភាពគ្នា និងសិទ្ធិមិនអាច លក់ ដូរ ផ្ទេរ ឬដកហូតបានរបស់សមាជិកទាំងអស់នៃគ្រួសារមនុស្ស គឺជាគ្រឹះនៃសេរីភាព យុត្ដិធម៌ និងសន្ដិភាពក្នុងពិភពលោក។\n"
                ;
            String laoTest = "ປະກາດສາກົນ ກ່ຽວກັບສິດຂອງມະນຸດ\n"
                + "ວັນທີ 20 ທັນວາ ຄ.ສ 1958\n"
                + "ກອງປະຊຸມໃຫຍ່ສະຫະປະຊາຊາດໄດ້ຮັບຮອງ ແລະ ປະກາດສິດຂອງມວນມະນຸດຊື່ງພວກເຮົາໄດ້ຈັດພິມຂື້ນຕະຫຼອດບົດຫຼັງການປະກາດອັນເປັນປະຫວັດການນີ້ກອງປະຊຸມໃຫຍ່ໄດ້ຊີ້ແຈງກັບສະມາຊິກທຸກໆທ່ານຂໍຈົ່ງຢ່າໄດ້ປະລະເລີຍໂອກາດ ແລະ ວິທີທາງອັນໃດຊື່ງສາມາດຈະໄດ້ຮັບໃນອານາຄົດ, ເພື່ອເຜີຍແຜ່ໃຫ້ປະຊາຊົນໄດ້ຮັບແຈກຈ່າຍອ່ານ ແລະ ວິຈານສີ່ງສຳຄັນໃນໂຮງຮຽນ ແລະ ສະຖານສຶກສາໃດໆໂດຍບໍ່ຄຳນືງເຖິງລັດທິ, ການເມືອງຂອງເຮົາ ຫຼື ປະເທດໃດເລີຍ.\n"
                + "ສຳນັກງານຖະແຫຼງຂ່າວຂອງອົງການສະຫະປະຊາຊາດ ຄ.ສ 1958.\n"
                ;
            // NEW
            registerTranslit("Lao-Latin", "ບ", "b", laoTest);
            registerTranslit("Khmer-Latin", "ឥ", "ĕ", khmerTest);
            registerTranslit("Sinhala-Latin", "ක", "ka", sinhalaTest);
            registerTranslit("Japn-Latn", "譆", "aa", null);
            // MODIFIED
            registerTranslit("Han-SpacedHan", "《", "«", null);
            registerTranslit("Greek-Latin", "΄", "´", null);
            registerTranslit("Hebrew-Latin", "־", "-", null);
            registerTranslit("Cyrillic-Latin", "ө", "ö", null);
            registerTranslit("Myanmar-Latin", "ဿ", "s", null);
            registerTranslit("Latin-Armenian", "’", "՚", null);
            registerTranslit("Malayalam-Interindic", "ൺ", "", null);
            registerTranslit("Interindic-Latin", "॰", ".", null);
            registerTranslit("Devanagari-Interindic", "ॲ", "\uE084", null);
            registerTranslit("Arabic-Latin", "؉", "‰", null);
        } else {
            warnln(" — Registering all");
            CLDRTransforms.registerCldrTransforms(null, ".*", null, false);
        }

        for (String locale : modernCldr) {
            if (special.contains(locale)) {
                continue;
            }
            ltp.set(locale);
            if (!ltp.getRegion().isEmpty()) {
                continue;
            }
            ltp.set(locale);
            String max = ls.maximize(locale);
            final String script = ltp.set(max).getScript();

            String id = script + "-Latn";

            switch(script) {
            case "Latn":
                continue;
            case "Khmr":
                id = "Khmr-Latn/UNGEGN";
                break;
            case "Laoo":
                id = "Laoo-Latn/UNGEGN";
                break;
            case "Sinh":
                id = "Sinh-Latn/UNGEGN";
                break;
            case "Japn":
                id = "Jpan-Latn";
                break;
            case "Hant": case "Hans":
                id = "Hani-Latn";
                break;
            }
            Transliterator t;
            try {
                t = Transliterator.getInstance(id);
            } catch (Exception e) {
                missing.add(locale);
                continue;
            }
            badPlusSample.clear();
            CLDRFile file = factory.make(locale, false);
            for (String path : file) {
                if (path.contains("/exemplar") || path.contains("/parseLenients")) {
                    continue;
                }
                String value = file.getStringValue(path);
                String transformed = t.transform(value);
                badPlusSample.addNonLatin(locale, script, path, value, transformed);
            }
            if (!badPlusSample.isEmpty()) {
                logln(locale + " " + script + " transform doesn't handle " + badPlusSample.size()
                + " code points:\n" + badPlusSample);
                allMissing.addAll(badPlusSample);
            }
        }
        if (!allMissing.isEmpty()) {
            warnln("Summary Any-Latn transform doesn't handle " + allMissing.size()
            + " code points:\n" + allMissing.dataSet.keySet().toPattern(false)
                );
            for (String value : allMissing.scriptMissing.values()) {
                final UnicodeSet missingFoScript = allMissing.scriptMissing.getKeys(value);
                errln("Transliterator for\t" + value + "\tmissing\t" + missingFoScript.size()
                + ":\t" + missingFoScript.toPattern(false));
            }
        }
    }

    static LikelySubtags ls = new LikelySubtags();
    static LanguageTagParser ltp = new LanguageTagParser();

    static String getScript(String locale) {
        ltp.set(locale);
        String max = ls.maximize(locale);
        return ltp.set(max).getScript();
    }
    static UnicodeRelation<String> pathSegmentsOk = new UnicodeRelation<>();
    static {
        // need to add exceptions to CheckForExemplars
        pathSegmentsOk.add('\u03A9', "/unit[@type=\"electric-ohm\"]");
        pathSegmentsOk.add('\u03BC', "/unit[@type=\"length-micrometer\"]");
        pathSegmentsOk.add('\u03BC', "/unit[@type=\"mass-microgram\"]");
        pathSegmentsOk.add('\u03BC', "/unit[@type=\"duration-microsecond\"]");
        pathSegmentsOk.add('\u03BC', "//ldml/annotations/annotation[@cp=\"µ\"]");
        pathSegmentsOk.add('\u03BC', "/compoundUnit[@type=\"10p-6\"]/unitPrefixPattern");
        pathSegmentsOk.add('\u03C0', "/unit[@type=\"angle-radian\"]");
        pathSegmentsOk.add('\u03C9', "/compoundUnit[@type=\"10p-6\"]/unitPrefixPattern");
        pathSegmentsOk.add('\u0440', "/currency[@type=\"BYN\"]");
        pathSegmentsOk.add('\u0440', "/currency[@type=\"RUR\"]");
        pathSegmentsOk.add('\u10DA', "/currency[@type=\"GEL\"]");
        pathSegmentsOk.addAll(new UnicodeSet("[؉ ٪ ٫ ۰ ۱ ؉ا س ا س ٬ ٬ ؜ ؛  ]"), "//ldml/numbers/symbols[@numberSystem=\"arab");

        // need to fix data in locale files
        pathSegmentsOk.addAll(new UnicodeSet("[コサ割可合営得指月有満無申祝禁秘空割祝秘]"), "//ldml/annotations/annotation");
        pathSegmentsOk.addAll(new UnicodeSet("[ا ر ل ی]"), "//ldml/annotations/annotation[@cp=\"﷼\"]");
        pathSegmentsOk.addAll(new UnicodeSet("[Р а в д е з л о п р т у ы ь]"), "//ldml/annotations/annotation[@cp=\"🪬\"]");
        //ω Grek    lo; Laoo;   //ldml/units/unitLength[@type="short"]/unit[@type="electric-ohm"]/unitPattern[@count="other"];
        pathSegmentsOk.freeze();
    }

    final static UnicodeSet CHARS_OK = new UnicodeSet("[\u202F\\p{Sc}]").freeze();

    static class SampleDataSet {
        UnicodeMap<SampleData> dataSet = new UnicodeMap<>();
        UnicodeRelation<String> scriptMissing = new UnicodeRelation<>();

        static class SampleData {
            final String locale;
            final String path;
            final String value;
            final String transformed;
            public SampleData(String locale, String path, String value, String transformed) {
                this.locale = locale;
                this.path = path;
                this.value = value;
                this.transformed = transformed;
            }
            @Override
            public String toString() {
                return String.format("%s;\t%s;\t%s;\t%s;\t%s", locale, getScript(locale), path, value, transformed);
            }
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result
            .append("size=\t").append(dataSet.size())
            .append("\nkeys=\t").append(dataSet.keySet().toPattern(false))
            .append("\ndetails=")
            ;
            for (Entry<String, SampleData> entry : dataSet.entrySet()) {
                final String key = entry.getKey();
                final int cp = key.codePointAt(0);
                result
                .append("\n").append(Utility.hex(cp))
                .append("\t").append(key)
                .append("\t").append(UScript.getShortName(UScript.getScript(cp)))
                .append("\t").append(entry.getValue())
                ;
            }
            return result.toString();
        }

        private void addNonLatin(String locale, String script, String path, String source, String transformed) {
            int cp = 0;
            BitSet bs = new BitSet();
            for (int ci = 0; ci < transformed.length(); ci += Character.charCount(cp)) {
                cp = transformed.codePointAt(ci);
                if (CHARS_OK.contains(cp) || checkCharWithPath(path, cp)) {
                    continue;
                }
                int scriptCode = UScript.getScriptExtensions(cp, bs);
                switch(scriptCode) {
                case UScript.LATIN: case UScript.COMMON: case UScript.INHERITED:
                    continue;
                default:
                    // add(locale, script, path, source, transformed, cp);
                    if (scriptCode >= 0) { // no extensions, not latin, etc.
                        add(locale, script, path, source, transformed, cp);
                    } else {
                        bs.clear(UScript.LATIN);
                        if (!bs.isEmpty()) {
                            add(locale, script, path, source, transformed, cp);
                        }
                    }
                }
            }
        }

        public boolean checkCharWithPath(String path, int cp) {
            Set<String> pathCheckSet = pathSegmentsOk.get(cp);
            if (pathCheckSet == null) {
                return false;
            }
            for (String pathCheck : pathCheckSet) {
                if (path.contains(pathCheck)) {
                    return true;
                }
            }
            return false;
        }

        public int size() {
            return dataSet.size();
        }

        public boolean isEmpty() {
            return dataSet.isEmpty();
        }

        public void clear() {
            dataSet.clear();
        }

        private void add(String locale, String script, String path, String source, String transformed, int cp) {
            SampleData old = dataSet.get(cp);
            if (old == null || old.transformed.length() > transformed.length()) {
                dataSet.put(cp, new SampleData(locale, path, source, transformed));
                scriptMissing.add(cp, script);
            }
        }
        private void addAll(SampleDataSet badPlusSample) {
            for (String c : badPlusSample.scriptMissing.keySet()) {
                scriptMissing.addAll(c, badPlusSample.scriptMissing.get(c));
            }
            for (Entry<String, SampleData> entry : badPlusSample.dataSet.entrySet()) {
                SampleData newData = entry.getValue();
                SampleData old = dataSet.get(entry.getKey());
                if (old == null || old.transformed.length() > newData.transformed.length()) {
                    dataSet.put(entry.getKey(), newData);
                }
            }
        }
    }

    public static final String TRANSFORM_DIR = (CLDRPaths.COMMON_DIRECTORY + "transforms/");
    private CLDRTransforms r = CLDRTransforms.getInstance();
    private ImmutableList<String> noSkip = ImmutableList.of();

    public String registerTranslit(String ID, String sourceTest, String targetTest, String sample) {
        String internalId = r.registerTransliteratorsFromXML(TRANSFORM_DIR, ID, noSkip, true);
        Transliterator t = null;
        try {
            t = Transliterator.getInstance(internalId);
        } catch (Exception e) {
            System.out.println("For " + ID);
            e.printStackTrace();
            return null;
        }
        String target = t.transform(sourceTest);
        if (!target.equals(targetTest)) {
            errln(ID + " For " + sourceTest + ", expected " + targetTest + ", got " + target);
        }
        if (sample != null) {
            logln(ID + " sample:\n" + t.transform(sample));
        }
        return internalId;
    }
}
