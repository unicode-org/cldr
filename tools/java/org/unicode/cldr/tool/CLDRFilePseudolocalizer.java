// Copyright 2011-2017 Google Inc. All Rights Reserved.
package org.unicode.cldr.tool;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.XMLSource;

/**
 * Generates pseudolocalized contents of a CLDRFile.
 *
 * @author viarheichyk@google.com (Igor Viarheichyk)
 */
public class CLDRFilePseudolocalizer {
    private static final Pattern NUMERIC_PLACEHOLDER = Pattern.compile("\\{\\d+\\}");
    private static final Pattern QUOTED_TEXT = Pattern.compile("'.*?'");
    private static final String PSEUDOLOCALES_DIRECTORY = "pseudolocales";
    private static final String ORIGINAL_LOCALE = "en";
    private static final String EXEMPLARS_PATH = "/exemplarCharactersx";
    private static final String EXEMPLAR_PATH = "//ldml/characters/exemplarCharacters";
    private static final String EXEMPLAR_AUX_PATH = "//ldml/characters/exemplarCharacters[@type=\"auxiliary\"]";
    private static final String TERRITORY_PATTERN = "//ldml/localeDisplayNames/territories/territory[@type=\"%s\"]";
    private static final String[] EXCLUDE_LIST = { "/exemplarCharacters", "/delimiters",
        "/contextTransforms", "/numbers",
        "/units", // [ and ] are not allowed in units
        "narrow", "localeDisplayPattern", "timeZoneNames/fallbackFormat", // Expansion limits
    };
    private static final String[] PATTERN_LIST = { "/pattern", "FormatItem", "hourFormat" };

    private static class Pseudolocalizer {
        private boolean pattern;

        public Pseudolocalizer() {
            pattern = false;
        }

        public boolean getPattern() {
            return pattern;
        }

        public String start() {
            return "";
        }

        public String end() {
            return "";
        }

        public String fragment(String text) {
            return text;
        }

        protected void setPattern(boolean pattern) {
            this.pattern = pattern;
        }
    }

    private static class PseudolocalizerXA extends Pseudolocalizer {
        private static final String[] NUMBERS = {
            "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
            "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen",
            "seventeen", "eighteen", "nineteen", "twenty", "twentyone", "twentytwo",
            "twentythree", "twentyfour", "twentyfive", "twentysix", "twentyseven",
            "twentyeight", "twentynine", "thirty", "thirtyone", "thirtytwo",
            "thirtythree", "thirtyfour", "thirtyfive", "thirtysix", "thirtyseven",
            "thirtyeight", "thirtynine", "forty"
        };
        private static final Map<Integer, String> REPLACEMENTS = buildReplacementsTable();
        private int charCount = 0;

        private static Map<Integer, String> buildReplacementsTable() {
            Map<Integer, String> table = new HashMap<Integer, String>();
            table.put((int) ' ', "\u2003");
            table.put((int) '!', "\u00a1");
            table.put((int) '"', "\u2033");
            table.put((int) '#', "\u266f");
            table.put((int) '$', "\u20ac");
            table.put((int) '%', "\u2030");
            table.put((int) '&', "\u214b");
            table.put((int) '*', "\u204e");
            table.put((int) '+', "\u207a");
            table.put((int) ',', "\u060c");
            table.put((int) '-', "\u2010");
            table.put((int) '.', "\u00b7");
            table.put((int) '/', "\u2044");
            table.put((int) '0', "\u24ea");
            table.put((int) '1', "\u2460");
            table.put((int) '2', "\u2461");
            table.put((int) '3', "\u2462");
            table.put((int) '4', "\u2463");
            table.put((int) '5', "\u2464");
            table.put((int) '6', "\u2465");
            table.put((int) '7', "\u2466");
            table.put((int) '8', "\u2467");
            table.put((int) '9', "\u2468");
            table.put((int) ':', "\u2236");
            table.put((int) ';', "\u204f");
            table.put((int) '<', "\u2264");
            table.put((int) '=', "\u2242");
            table.put((int) '>', "\u2265");
            table.put((int) '?', "\u00bf");
            table.put((int) '@', "\u055e");
            table.put((int) 'A', "\u00c5");
            table.put((int) 'B', "\u0181");
            table.put((int) 'C', "\u00c7");
            table.put((int) 'D', "\u00d0");
            table.put((int) 'E', "\u00c9");
            table.put((int) 'F', "\u0191");
            table.put((int) 'G', "\u011c");
            table.put((int) 'H', "\u0124");
            table.put((int) 'I', "\u00ce");
            table.put((int) 'J', "\u0134");
            table.put((int) 'K', "\u0136");
            table.put((int) 'L', "\u013b");
            table.put((int) 'M', "\u1e40");
            table.put((int) 'N', "\u00d1");
            table.put((int) 'O', "\u00d6");
            table.put((int) 'P', "\u00de");
            table.put((int) 'Q', "\u01ea");
            table.put((int) 'R', "\u0154");
            table.put((int) 'S', "\u0160");
            table.put((int) 'T', "\u0162");
            table.put((int) 'U', "\u00db");
            table.put((int) 'V', "\u1e7c");
            table.put((int) 'W', "\u0174");
            table.put((int) 'X', "\u1e8a");
            table.put((int) 'Y', "\u00dd");
            table.put((int) 'Z', "\u017d");
            table.put((int) '[', "\u2045");
            table.put((int) '\\', "\u2216");
            table.put((int) ']', "\u2046");
            table.put((int) '^', "\u02c4");
            table.put((int) '_', "\u203f");
            table.put((int) '`', "\u2035");
            table.put((int) 'a', "\u00e5");
            table.put((int) 'b', "\u0180");
            table.put((int) 'c', "\u00e7");
            table.put((int) 'd', "\u00f0");
            table.put((int) 'e', "\u00e9");
            table.put((int) 'f', "\u0192");
            table.put((int) 'g', "\u011d");
            table.put((int) 'h', "\u0125");
            table.put((int) 'i', "\u00ee");
            table.put((int) 'j', "\u0135");
            table.put((int) 'k', "\u0137");
            table.put((int) 'l', "\u013c");
            table.put((int) 'm', "\u0271");
            table.put((int) 'n', "\u00f1");
            table.put((int) 'o', "\u00f6");
            table.put((int) 'p', "\u00fe");
            table.put((int) 'q', "\u01eb");
            table.put((int) 'r', "\u0155");
            table.put((int) 's', "\u0161");
            table.put((int) 't', "\u0163");
            table.put((int) 'u', "\u00fb");
            table.put((int) 'v', "\u1e7d");
            table.put((int) 'w', "\u0175");
            table.put((int) 'x', "\u1e8b");
            table.put((int) 'y', "\u00fd");
            table.put((int) 'z', "\u017e");
            table.put((int) '|', "\u00a6");
            table.put((int) '~', "\u02de");
            return table;
        }

        public String start() {
            charCount = 0;
            return "[";
        }

        public String end() {
            StringBuilder expansionText = new StringBuilder();
            int expansion = (charCount + 1) / 2;
            int wordIndex = 0;
            while (expansion > 0) {
                String word = NUMBERS[wordIndex++ % NUMBERS.length];
                expansionText.append(' ');
                // Protect expansion strings with single quotes for patterns.
                if (getPattern()) {
                    expansionText.append('\'');
                }
                expansionText.append(word);
                if (getPattern()) {
                    expansionText.append('\'');
                }
                expansion -= word.length() + 1;
            }
            expansionText.append(']');
            return expansionText.toString();
        }

        public String fragment(String text) {
            StringBuilder buf = new StringBuilder();
            int index = 0;
            while (index < text.length()) {
                int codePoint = text.codePointAt(index);
                charCount++;
                index += Character.charCount(codePoint);
                String replacement = REPLACEMENTS.get(codePoint);
                if (replacement != null) {
                    buf.append(replacement);
                } else {
                    buf.appendCodePoint(codePoint);
                }
            }
            return buf.toString();
        }
    }

    private static class PseudolocalizerXB extends Pseudolocalizer {
        /** Right-to-left override character. */
        private static final String RLO = "\u202e";
        /** Right-to-left mark character. */
        private static final String RLM = "\u200f";
        /** Pop direction formatting character. */
        private static final String PDF = "\u202c";
        /** Prefix to add before each LTR word */
        private static final String BIDI_PREFIX = RLM + RLO;
        /** Postfix to add after each LTR word */
        private static final String BIDI_POSTFIX = PDF + RLM;

        public String fragment(String text) {
            StringBuilder output = new StringBuilder();
            boolean wrapping = false;
            for (int index = 0; index < text.length();) {
                int codePoint = text.codePointAt(index);
                index += Character.charCount(codePoint);
                byte directionality = Character.getDirectionality(codePoint);
                boolean needsWrap = (directionality == Character.DIRECTIONALITY_LEFT_TO_RIGHT);
                if (needsWrap != wrapping) {
                    wrapping = needsWrap;
                    output.append(wrapping ? BIDI_PREFIX : BIDI_POSTFIX);
                }
                output.appendCodePoint(codePoint);
            }
            if (wrapping) {
                output.append(BIDI_POSTFIX);
            }
            return output.toString();
        }
    }

    private String outputLocale;
    private Pseudolocalizer pseudolocalizer;

    /**
     * Construct new CLDRPseudolocalization object.
     *
     * @param outputLocale
     *             name of target locale
     * @param pipeline
     *             pseudolocalization pipeline to generate target locale data
     */
    public CLDRFilePseudolocalizer(String outputLocale, Pseudolocalizer pseudolocalizer) {
        this.outputLocale = outputLocale;
        this.pseudolocalizer = pseudolocalizer;
    }

    public static CLDRFilePseudolocalizer createInstanceXA() {
        return new CLDRFilePseudolocalizer("en_XA", new PseudolocalizerXA());
    }

    public static CLDRFilePseudolocalizer createInstanceXB() {
        return new CLDRFilePseudolocalizer("ar_XB", new PseudolocalizerXB());
    }

    /**
     * Transforms a CLDRFile value into another form.
     *
     * @return pseudolocalized value.
     */
    private String transformValue(String path, String value) {
        if (containsOneOf(path, EXCLUDE_LIST)) {
            return value;
        }
        if (containsOneOf(path, PATTERN_LIST)) {
            return createMessage(value, QUOTED_TEXT, true);
        } else {
            return createMessage(value, NUMERIC_PLACEHOLDER, false);
        }
    }

    /**
     * Check if string contains any substring from the provided list.
     */
    private boolean containsOneOf(String string, String[] substrings) {
        for (String substring : substrings) {
            if (string.contains(substring)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create either localizable or non-localizable text fragment depending on flag value.
     */
    private String pseudolocalizeFragment(String text, boolean localizable) {
        return localizable ? pseudolocalizer.fragment(text) : text;
    }

    /**
     * Create a message that can contain localizable and non-localizable parts.
     */
    private String createMessage(String text, Pattern pattern,
        boolean matchIsLocalizable) {
        StringBuffer buffer = new StringBuffer(pseudolocalizer.start());
        Matcher match = pattern.matcher(text);
        int start = 0;
        pseudolocalizer.setPattern(matchIsLocalizable);
        for (; match.find(); start = match.end()) {
            if (match.start() > start) {
                buffer.append(pseudolocalizeFragment(
                    text.substring(start, match.start()), !matchIsLocalizable));
            }
            buffer.append(pseudolocalizeFragment(match.group(), matchIsLocalizable));
        }
        if (start < text.length()) {
            buffer.append(pseudolocalizeFragment(text.substring(start), !matchIsLocalizable));
        }
        buffer.append(pseudolocalizer.end());
        return buffer.toString();
    }

    /**
     * Add pseudolocale characters to exemplarCharacters entry pointed by xpath.
     */
    private String mergeExemplars(String value) {
        String pseudolocalized = createMessage(value, NUMERIC_PLACEHOLDER, false);
        StringBuffer result = new StringBuffer(value.substring(0, value.length() - 1));
        final char CLOSING_BRACKET = ']';
        for (int i = 0; i < pseudolocalized.length(); i++) {
            char c = pseudolocalized.charAt(i);
            if (c != CLOSING_BRACKET) {
                String chunk;
                if (Character.isAlphabetic(c)) {
                    chunk = String.valueOf(c);
                } else {
                    chunk = String.format("\\u%04X", (int) c);
                }
                if (result.indexOf(chunk) == -1
                    && result.indexOf(String.valueOf(c)) == -1) {
                    result.append(' ');
                    result.append(chunk);
                }
            }
        }
        result.append(CLOSING_BRACKET);
        return result.toString();
    }

    /**
     * Generate CLDRFile object. Original CLDRFile is created from .xml file and its
     * content is passed through pseudolocalization pipeline.
     */
    public CLDRFile generate() {
        Factory factory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
        // Create input CLDRFile object resolving inherited data.
        CLDRFile input = factory.make(ORIGINAL_LOCALE, false);
        XMLSource outputSource = new SimpleXMLSource(outputLocale);
        for (String xpath : input) {
            String fullPath = input.getFullXPath(xpath);
            String value = input.getStringValue(xpath);
            if (!value.isEmpty()) {
                String newValue = transformValue(xpath, value);
                if (!newValue.equals(value)) {
                    outputSource.putValueAtPath(fullPath, newValue);
                }
            }
        }
        // Pseudolocalize exemplar characters and put them into auxiliary set.
        outputSource.putValueAtPath(EXEMPLAR_AUX_PATH,
            mergeExemplars(input.getStringValue(EXEMPLAR_PATH)));
        // Create fake pseudolocales territories.
        addTerritory(outputSource, "XA");
        addTerritory(outputSource, "XB");
        return new CLDRFile(outputSource);
    }

    /**
     * Add a territory into output xml.
     */
    private void addTerritory(XMLSource outputSource, String territory) {
        String territoryPath = String.format(TERRITORY_PATTERN, territory);
        outputSource.putValueAtPath(territoryPath, String.format("[%s]", territory));
    }

    /**
     * Generate CLDRFile object and save it into .xml file.
     */
    public String generateAndSave() throws Exception {
        CLDRFile output = generate();
        String outputDir = CLDRPaths.GEN_DIRECTORY + "main" + File.separator + PSEUDOLOCALES_DIRECTORY + File.separator;
        String outputFile = output.getLocaleID() + ".xml";
        PrintWriter out = FileUtilities.openUTF8Writer(outputDir, outputFile);
        output.write(out);
        out.close();
        return (outputDir + outputFile);
    }

    public static void main(String[] args) throws Exception {
        // Generate en-XA locale (accents, brackets and expansion),
        // dump resulting file name to stdout.
        System.out.println(createInstanceXA().generateAndSave());
        // Generate ar-XB (fake Bidi) locale.
        System.out.println(createInstanceXB().generateAndSave());
    }
}
