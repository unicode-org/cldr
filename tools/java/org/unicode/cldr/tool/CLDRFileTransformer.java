package org.unicode.cldr.tool;

import java.io.File;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTransforms;
import org.unicode.cldr.util.CLDRTransforms.ParsedTransformID;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.SimpleFactory.NoSourceDirectoryException;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.XMLSource;

import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;

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
    enum PolicyIfExisting {
        RETAIN, DISCARD, MINIMIZE
    }

    public enum LocaleTransform {
        sr_Latn("sr", "Serbian-Latin-BGN.xml", Transliterator.FORWARD, "[:script=Cyrl:]", PolicyIfExisting.DISCARD), //
        sr_Latn_BA("sr_Cyrl_BA", "Serbian-Latin-BGN.xml", Transliterator.FORWARD, "[:script=Cyrl:]", PolicyIfExisting.DISCARD), //
        sr_Latn_ME("sr_Cyrl_ME", "Serbian-Latin-BGN.xml", Transliterator.FORWARD, "[:script=Cyrl:]", PolicyIfExisting.DISCARD), //
        sr_Latn_XK("sr_Cyrl_XK", "Serbian-Latin-BGN.xml", Transliterator.FORWARD, "[:script=Cyrl:]", PolicyIfExisting.DISCARD), //
        ha_NE("ha", "ha-ha_NE.xml", Transliterator.FORWARD, "[y Y ƴ Ƴ ʼ]", PolicyIfExisting.DISCARD), //
        yo_BJ("yo", "yo-yo_BJ.xml", Transliterator.FORWARD, "[ẹ ọ ṣ Ẹ Ọ Ṣ]", PolicyIfExisting.DISCARD), //
        de_CH("de", "[ß] Casefold", Transliterator.FORWARD, "[ß]", PolicyIfExisting.MINIMIZE), //
        yue_Hans("yue", "Simplified-Traditional.xml", Transliterator.REVERSE, "[:script=Hant:]", PolicyIfExisting.RETAIN), //
        // en_NZ("en_AU", "null", Transliterator.FORWARD, "[]", PolicyIfExisting.DISCARD), 
        // Needs work to fix currency symbols, handle Maori. See http://unicode.org/cldr/trac/ticket/9516#comment:6
        ;

        private final String inputLocale;
        private final String transformFilename;
        private final int direction;
        private final UnicodeSet inputChars;
        private final PolicyIfExisting policy;

        /**
         * @deprecated Use {@link #LocaleTransform(String,String,int,String,PolicyIfExisting)} instead
         */
        private LocaleTransform(String inputLocale, String transformFilename, int direction, String inputCharPattern) {
            this(inputLocale, transformFilename, direction, inputCharPattern, PolicyIfExisting.DISCARD);
        }

        private LocaleTransform(String inputLocale, String transformFilename, int direction, String inputCharPattern, PolicyIfExisting policy) {
            this.inputLocale = inputLocale;
            this.transformFilename = transformFilename;
            this.direction = direction;
            this.inputChars = new UnicodeSet(inputCharPattern);
            this.policy = policy;
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
    /*
     * The transliterators map exists, and is static, to avoid wasting a lot of time creating
     * a new Transliterator more often than necessary. (An alternative to "static" here might be to
     * create only one CLDRFileTransformer, maybe as a member of ExampleGenerator.)
     * Use ConcurrentHashMap rather than HashMap to avoid concurrency problems.
     * Reference: https://unicode.org/cldr/trac/ticket/11657
     */
    private static Map<LocaleTransform, Transliterator> transliterators = new ConcurrentHashMap<LocaleTransform, Transliterator>();
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
        Transliterator transliterator;
        if (localeTransform.getTransformFilename().contains(".xml")) {
            ParsedTransformID directionInfo = new ParsedTransformID();
            String ruleString = CLDRTransforms.getIcuRulesFromXmlFile(transformDir, localeTransform.getTransformFilename(), directionInfo);
            transliterator = Transliterator.createFromRules(directionInfo.getId(),
                ruleString, localeTransform.getDirection());
            transliterators.put(localeTransform, transliterator);
        } else {
            transliterator = Transliterator.getInstance(localeTransform.getTransformFilename());
        }
        return transliterator;
    }

    /**
     * NOTE: This method does not currently handle nested transliterators.
     *
     * @param input
     * @return null if the input file was missing, or if there is no new output file.
     */
    public CLDRFile transform(LocaleTransform localeTransform) {
        Transliterator transliterator = loadTransliterator(localeTransform);
        CLDRFile input;
        try {
            input = factory.make(localeTransform.getInputLocale(), false);
        } catch (ICUUncheckedIOException e1) {
            return null; // input file is missing (or otherwise unavailable)
        }
        boolean hadOutput = true;
        CLDRFile output;
        try {
            output = factory.make(localeTransform.getOutputLocale(), false);
        } catch (NoSourceDirectoryException e) {
            // if we can't open the file, then just make a new one.
            XMLSource dataSource = new SimpleXMLSource(localeTransform.getOutputLocale());
            output = new CLDRFile(dataSource);
            hadOutput = false;
        }
        String outputParentString = LocaleIDParser.getParent(localeTransform.getOutputLocale());
        CLDRFile outputParent = factory.make(outputParentString, true);

        outputParent = factory.make(localeTransform.getInputLocale(), false);
        XMLSource outputSource = new SimpleXMLSource(localeTransform.toString());
        for (String xpath : input) {
            String fullPath = input.getFullXPath(xpath);
            String value = input.getStringValue(xpath);
            String oldValue = output.getStringValue(xpath);
            String parentValue = outputParent.getStringValue(xpath);
            value = transformValue(transliterator, localeTransform, xpath, value, oldValue, parentValue);
            if (value != null) {
                outputSource.putValueAtPath(fullPath, value);
            }
        }
        if (!outputSource.iterator().hasNext()) { // empty new output
            if (!hadOutput) {
                return null; // don't add file if nothing to add
            }
        }
        return new CLDRFile(outputSource);
    }

    /**
     * Transforms a CLDRFile value into another form.
     * @param parentValue 
     */
    private String transformValue(Transliterator transliterator, LocaleTransform localeTransform, String path, String value,
        String oldValue, String parentValue) {

        // allows us to change only new values
        switch (localeTransform.policy) {
        case RETAIN:
        case MINIMIZE:
            if (oldValue != null) {
                return oldValue;
            }
            break;
        default:
        }

        UnicodeSet chars = localeTransform.getInputChars();
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
        if (localeTransform.policy == PolicyIfExisting.MINIMIZE) {
            if (transliterated.equals(value)) {
                return null;
            }
        }

        if (chars.containsSome(transliterated)) {
            unconverted.addAll(new UnicodeSet().addAll(chars).retainAll(transliterated));
        }
        return transliterated;
    }

    public static void main(String[] args) throws Exception {
        for (String dir : DtdType.ldml.directories) {
            if (dir.equals("casing") // skip, field contents are keywords, not localizable content
                || dir.equals("collation") // skip, field contents are complex, and can't be simply remapped
                || dir.equals("annotationsDerived") // skip, derived later
            ) {
                continue;
            }
            System.out.println("\nDirectory: " + dir);
            Factory factory = Factory.make(CLDRPaths.COMMON_DIRECTORY + dir + "/", ".*");
            CLDRFileTransformer transformer = new CLDRFileTransformer(factory, CLDRPaths.COMMON_DIRECTORY + "transforms" + File.separator);
            for (LocaleTransform localeTransform : LocaleTransform.values()) {
                CLDRFile output = transformer.transform(localeTransform);
                if (output == null) {
                    System.out.println("SKIPPING missing file: " + dir + "/" + localeTransform.inputLocale + ".xml");
                    continue;
                }
                String outputDir = CLDRPaths.GEN_DIRECTORY + "common/" + dir + File.separator;
                String outputFile = output.getLocaleID() + ".xml";
                PrintWriter out = FileUtilities.openUTF8Writer(outputDir, outputFile);
                System.out.println("Generating locale file: " + outputDir + outputFile);
                if (!transformer.unconverted.isEmpty()) {
                    System.out.println("Untransformed characters: " + transformer.unconverted);
                    transformer.unconverted.clear();
                }
                output.write(out);
                out.close();
            }
        }
    }
}
