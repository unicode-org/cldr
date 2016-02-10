package org.unicode.cldr.tool;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTransforms;
import org.unicode.cldr.util.CLDRTransforms.ParsedTransformID;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.XMLSource;

import com.ibm.icu.dev.util.BagFormatter;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;

/**
 * Transforms the contents of a CLDRFile.
 *
 * @author jchye
 */
public class CLDRFileTransformer {
    /**
     * Contains all supported locale-to-locale conversions along with information
     * needed to convert each locale. Each enum value is named after the locale that results
     * from the conversion.
     */
    public enum LocaleTransform {
        sr_Latn("sr", "Serbian-Latin-BGN.xml", Transliterator.FORWARD, "[:script=Cyrl:]"),
        sr_Latn_BA("sr_Cyrl_BA", "Serbian-Latin-BGN.xml", Transliterator.FORWARD, "[:script=Cyrl:]"),
        sr_Latn_ME("sr_Cyrl_ME", "Serbian-Latin-BGN.xml", Transliterator.FORWARD, "[:script=Cyrl:]"),
        sr_Latn_XK("sr_Cyrl_XK", "Serbian-Latin-BGN.xml", Transliterator.FORWARD, "[:script=Cyrl:]"),
        yo_BJ("yo", "yo-yo_BJ.xml", Transliterator.FORWARD, "[ẹ ọ ṣ Ẹ Ọ Ṣ]");

        private final String inputLocale;
        private final String transformFilename;
        private final int direction;
        private final UnicodeSet inputChars;

        private LocaleTransform(String inputLocale, String transformFilename, int direction, String inputCharPattern) {
            this.inputLocale = inputLocale;
            this.transformFilename = transformFilename;
            this.direction = direction;
            this.inputChars = new UnicodeSet(inputCharPattern);
        }

        /**
         * @return the locale that used for conversion
         */
        public String getInputLocale() {
            return inputLocale;
        }

        /**
         * @return the locale that used for conversion
         */
        public String getOutputLocale() {
            return this.toString();
        }

        /**
         * @return the filename of the transform used to make the conversion
         */
        public String getTransformFilename() {
            return transformFilename;
        }

        /**
         * @return the direction of the transformation
         */
        public int getDirection() {
            return direction;
        }

        /**
         * @return the set of characters in the input locale that should have been removed after
         *         transformation, used for internal debugging
         */
        private UnicodeSet getInputChars() {
            return inputChars;
        }
    }

    private UnicodeSet unconverted = new UnicodeSet();
    private Factory factory;
    private Map<LocaleTransform, Transliterator> transliterators = new HashMap<LocaleTransform, Transliterator>();
    private String transformDir;

    /**
     * @param factory
     *            the factory to get locale data from
     * @param transformDir
     *            the directory containing the transform files
     */
    public CLDRFileTransformer(Factory factory, String transformDir) {
        this.factory = factory;
        this.transformDir = transformDir;
    }

    public Transliterator loadTransliterator(LocaleTransform localeTransform) {
        if (transliterators.containsKey(localeTransform)) {
            return transliterators.get(localeTransform);
        }
        ParsedTransformID directionInfo = new ParsedTransformID();
        String ruleString = CLDRTransforms.getIcuRulesFromXmlFile(
            transformDir, localeTransform.getTransformFilename(), directionInfo);
        Transliterator transliterator = Transliterator.createFromRules(directionInfo.getId(),
            ruleString, localeTransform.getDirection());
        transliterators.put(localeTransform, transliterator);
        return transliterator;
    }

    /**
     * NOTE: This method does not currently handle nested transliterators.
     *
     * @param input
     * @return
     */
    public CLDRFile transform(LocaleTransform localeTransform) {
        Transliterator transliterator = loadTransliterator(localeTransform);
        CLDRFile input = factory.make(localeTransform.getInputLocale(), false);
        CLDRFile output = factory.make(localeTransform.getOutputLocale(), false);
        XMLSource outputSource = new SimpleXMLSource(localeTransform.toString());
        for (String xpath : input) {
            String fullPath = input.getFullXPath(xpath);
            String value = input.getStringValue(xpath);
            String oldValue = output.getStringValue(xpath);
            value = transformValue(transliterator, localeTransform.getInputChars(), xpath, value, oldValue);
            outputSource.putValueAtPath(fullPath, value);
        }
        return new CLDRFile(outputSource);
    }

    /**
     * Transforms a CLDRFile value into another form.
     */
    private String transformValue(Transliterator transliterator, UnicodeSet inputChars, String path, String value,
        String oldValue) {
        String transliterated;
        // TODO: Don't transform dates/patterns.
        // For now, don't try to transliterate the exemplar characters - use the ones from the original locale.
        // In the future, we can probably control this better with a config file - similar to CLDRModify's config file.
        if (path.contains("exemplarCharacters")) {
            if (oldValue != null) {
                transliterated = oldValue;
            } else {
                transliterated = value;
            }
        } else {
            transliterated = transliterator.transliterate(value);
            transliterated = Normalizer.compose(transliterated, false);
        }
        if (inputChars.containsSome(transliterated)) {
            unconverted.addAll(new UnicodeSet().addAll(inputChars).retainAll(transliterated));
        }
        return transliterated;
    }

    public static void main(String[] args) throws Exception {
        Factory factory = Factory.make(CLDRPaths.MAIN_DIRECTORY, ".*");
        CLDRFileTransformer transformer = new CLDRFileTransformer(factory, CLDRPaths.COMMON_DIRECTORY + "transforms" + File.separator);
        for (LocaleTransform localeTransform : LocaleTransform.values()) {
            CLDRFile output = transformer.transform(localeTransform);
            String outputDir = CLDRPaths.GEN_DIRECTORY + "main" + File.separator;
            String outputFile = output.getLocaleID() + ".xml";
            PrintWriter out = BagFormatter.openUTF8Writer(outputDir, outputFile);
            System.out.println("Generating locale file: " + outputDir + outputFile);
            output.write(out);
            out.close();
        }
        if (!transformer.unconverted.isEmpty()) {
            System.out.println("Untransformed characters: " + transformer.unconverted);
        }
    }
}
