package org.unicode.cldr.tool;

import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUUncheckedIOException;
import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.unicode.cldr.test.DisplayAndInputProcessor;
import org.unicode.cldr.tool.CLDRFileTransformer.PolicyIfExisting;
import org.unicode.cldr.tool.Option.Params;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTool;
import org.unicode.cldr.util.CLDRTransforms;
import org.unicode.cldr.util.CLDRTransforms.ParsedTransformID;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.SimpleFactory.NoSourceDirectoryException;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.TempPrintWriter;
import org.unicode.cldr.util.XMLSource;

/**
 * Transforms the contents of a CLDRFile.
 *
 * @author jchye
 */
@CLDRTool(
        alias = "cldr-xfrm",
        url = "https://cldr.unicode.org/development/generate-algorithmic-locales",
        description = "Generates algorithmic locales")
public class CLDRFileTransformer {
    public enum PolicyIfExisting {
        RETAIN, // Do not transliterate if existing output has locale content
        DISCARD, // Replace existing output locale content
        MINIMIZE // RETAIN, plus drop values if translit is a no-op.
    ;

        /**
         * @return true if existing values
         */
        boolean keepIfExisting() {
            switch (this) {
                case RETAIN:
                case MINIMIZE:
                    return true;
                default:
                    return false;
            }
        }
    }

    /**
     * Contains all supported locale-to-locale conversions along with information needed to convert
     * each locale. Each enum value is named after the locale that results from the conversion.
     */
    public enum LocaleTransform {
        sr_Latn(
                "sr",
                "Serbian-Latin-BGN.xml",
                Transliterator.FORWARD,
                "[:script=Cyrl:]",
                PolicyIfExisting.DISCARD), //
        sr_Latn_BA(
                "sr_Cyrl_BA",
                "Serbian-Latin-BGN.xml",
                Transliterator.FORWARD,
                "[:script=Cyrl:]",
                PolicyIfExisting.DISCARD), //
        sr_Latn_ME(
                "sr_Cyrl_ME",
                "Serbian-Latin-BGN.xml",
                Transliterator.FORWARD,
                "[:script=Cyrl:]",
                PolicyIfExisting.DISCARD), //
        sr_Latn_XK(
                "sr_Cyrl_XK",
                "Serbian-Latin-BGN.xml",
                Transliterator.FORWARD,
                "[:script=Cyrl:]",
                PolicyIfExisting.DISCARD), //
        ha_NE(
                "ha",
                "ha-ha_NE.xml",
                Transliterator.FORWARD,
                "[y Y ƴ Ƴ ʼ]",
                PolicyIfExisting.DISCARD), //
        yo_BJ(
                "yo",
                "yo-yo_BJ.xml",
                Transliterator.FORWARD,
                "[ẹ ọ ṣ Ẹ Ọ Ṣ]",
                PolicyIfExisting.DISCARD), //
        de_CH("de", "[ß] Casefold", Transliterator.FORWARD, "[ß]", PolicyIfExisting.MINIMIZE), //
        yue_Hans(
                "yue",
                "Simplified-Traditional.xml",
                Transliterator.REVERSE,
                "[:script=Hant:]",
                PolicyIfExisting.RETAIN), //
    // en_NZ("en_AU", "null", Transliterator.FORWARD, "[]", PolicyIfExisting.DISCARD),
    // Needs work to fix currency symbols, handle Māori. See
    // http://unicode.org/cldr/trac/ticket/9516#comment:6
    ;

        private final String inputLocale;
        private final String transformFilename;
        private final int direction;
        private final UnicodeSet inputChars;
        private final PolicyIfExisting policy;

        /**
         * @deprecated Use {@link #LocaleTransform(String,String,int,String,PolicyIfExisting)}
         *     instead
         */
        @Deprecated
        private LocaleTransform(
                String inputLocale,
                String transformFilename,
                int direction,
                String inputCharPattern) {
            this(
                    inputLocale,
                    transformFilename,
                    direction,
                    inputCharPattern,
                    PolicyIfExisting.DISCARD);
        }

        private LocaleTransform(
                String inputLocale,
                String transformFilename,
                int direction,
                String inputCharPattern,
                PolicyIfExisting policy) {
            this.inputLocale = inputLocale;
            this.transformFilename = transformFilename;
            this.direction = direction;
            this.inputChars = new UnicodeSet(inputCharPattern);
            this.policy = policy;
        }

        /**
         * @return the policy for existing content
         */
        public PolicyIfExisting getPolicyIfExisting() {
            return policy;
        }

        /**
         * @return the locale that used for conversion
         */
        public String getInputLocale() {
            return inputLocale;
        }

        /**
         * @return the locale that used for conversion (same as name)
         */
        public String getOutputLocale() {
            return this.name();
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
         *     transformation, used for internal debugging
         */
        private UnicodeSet getInputChars() {
            return inputChars;
        }

        @Override
        public String toString() {
            return String.format(
                    "%s => %s: %s “%s”",
                    getInputLocale(),
                    getOutputLocale(),
                    getPolicyIfExisting().name(),
                    getTransformFilename());
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
    private static Map<LocaleTransform, Transliterator> transliterators = new ConcurrentHashMap<>();
    private String transformDir;

    /**
     * @param factory the factory to get locale data from
     * @param transformDir the directory containing the transform files
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
            String ruleString =
                    CLDRTransforms.getIcuRulesFromXmlFile(
                            transformDir, localeTransform.getTransformFilename(), directionInfo);
            transliterator =
                    Transliterator.createFromRules(
                            directionInfo.getId(), ruleString, localeTransform.getDirection());
        } else {
            transliterator = Transliterator.getInstance(localeTransform.getTransformFilename());
        }
        transliterators.put(localeTransform, transliterator);
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
        final String inputLocale = localeTransform.getInputLocale();
        try {
            input = factory.make(inputLocale, false);
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

        outputParent = factory.make(inputLocale, false);
        XMLSource outputSource = new SimpleXMLSource(localeTransform.getOutputLocale());
        DisplayAndInputProcessor daip = new DisplayAndInputProcessor(output, true);
        for (String xpath : input) {
            String value = input.getStringValue(xpath);
            if (CldrUtility.INHERITANCE_MARKER.equals(value)) {
                final String foundIn = input.getSourceLocaleID(xpath, null);
                // Include these only when they are actually present in this file
                if (!foundIn.equals(inputLocale)) {
                    // inheritance marker came from somewhere else, ignore it
                    continue;
                }
            }
            if (value == null) {
                continue;
            }
            String fullPath = input.getFullXPath(xpath);
            String oldValue = output.getStringValue(xpath);
            String parentValue = outputParent.getStringValue(xpath);
            // allows us to change only new values
            if (oldValue != null && localeTransform.policy.keepIfExisting()) {
                value = oldValue;
                // retain full path of previous output (e.g. draft status)
                fullPath = output.getFullXPath(xpath);
            } else {
                value =
                        transformValue(
                                transliterator,
                                localeTransform,
                                xpath,
                                value,
                                oldValue,
                                parentValue);
                if (value != null) {
                    // check again
                    if (CldrUtility.INHERITANCE_MARKER.equals(value)) {
                        final String foundIn = input.getSourceLocaleID(xpath, null);
                        // Include these only when they are actually present in this file
                        if (!foundIn.equals(inputLocale)) {
                            // inheritance marker came from somewhere else (not from input locale),
                            // ignore it
                            continue;
                        }
                    }
                    // skip items that should not have DAIP
                    if (!xpath.contains("rbnfRules")) {
                        value = daip.processInput(xpath, value, null);
                    }
                }
            }
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
     *
     * @param parentValue
     */
    private String transformValue(
            Transliterator transliterator,
            LocaleTransform localeTransform,
            String path,
            String value,
            String oldValue,
            String parentValue) {
        UnicodeSet chars = localeTransform.getInputChars();
        String transliterated;

        // TODO: Don't transform dates/patterns.
        // For now, don't try to transliterate the exemplar characters - use the ones from the
        // original locale.
        // In the future, we can probably control this better with a config file - similar to
        // CLDRModify's config file.
        if (path.contains("exemplarCharacters")) {
            if (oldValue != null) {
                transliterated = oldValue;
            } else {
                transliterated = value;
            }
        } else {
            transliterated = transliterateByLines(transliterator, value);
            transliterated = Normalizer.compose(transliterated, false);
        }
        if (localeTransform.policy == PolicyIfExisting.MINIMIZE
                && !path.startsWith("//ldml/rbnf")) {
            if (transliterated.equals(value)) {
                return null;
            }
        }

        if (chars.containsSome(transliterated)) {
            unconverted.addAll(new UnicodeSet().addAll(chars).retainAll(transliterated));
        }
        return transliterated;
    }

    private String transliterateByLines(Transliterator transliterator, String value) {
        StringBuilder transliterated = new StringBuilder();
        for (final String valueLine : value.split("[\r\n]")) {
            if (transliterated.length() > 0) {
                transliterated.append("\n");
            }
            transliterated.append(transliterator.transliterate(valueLine));
        }
        return transliterated.toString();
    }

    enum MyOptions {
        verbose(new Params().setHelp("Show verbose status").setFlag('v')),
        dir(
                new Params()
                        .setHelp("Comma-separated list of 'common' subdirs to process")
                        .setFlag('d')
                        .setMatch("[a-z,]*")
                        .setDefault(getDefaultDirs())),
        filter(
                new Params()
                        .setHelp("Regex of output locales to match")
                        .setFlag('f')
                        .setMatch(".*")
                        .setDefault(".*"));

        // BOILERPLATE TO COPY
        final Option option;

        private MyOptions(Params params) {
            option = new Option(this, params);
        }

        private static Option.Options myOptions = new Option.Options();

        static {
            for (MyOptions option : MyOptions.values()) {
                myOptions.add(option, option.option);
            }
        }

        private static Set<String> parse(String[] args, boolean showArguments) {
            return myOptions.parse(MyOptions.values()[0], args, true);
        }
    }

    private static Predicate<String> skipThisDir =
            dir -> {
                if (dir.equals("casing") // skip, field contents are keywords, not localizable
                        // content
                        || dir.equals("collation") // skip, field contents are complex, and can't be
                        // simply
                        // remapped
                        || dir.equals("annotationsDerived") // skip, derived later
                ) {
                    return true;
                }
                return false;
            };

    /** return the default dirs as a string */
    private static String getDefaultDirs() {
        Set<String> s = new TreeSet<String>(DtdType.ldml.directories);
        s.removeIf(skipThisDir);
        return String.join(",", s);
    }

    static boolean VERBOSE = false;

    public static void main(String[] args) throws Exception {
        System.out.println(
                "# For help: https://cldr.unicode.org/development/generate-algorithmic-locales");
        MyOptions.parse(args, true);
        final Pattern matchPattern = Pattern.compile(MyOptions.filter.option.getValue());

        if (MyOptions.verbose.option.doesOccur()) {
            VERBOSE = true;
        }

        for (String dir : MyOptions.dir.option.getValue().split(",")) {
            if (!DtdType.ldml.directories.contains(dir) || skipThisDir.test(dir)) continue;
            System.out.println("\nDirectory: " + dir);
            final String sourceDirectory = CLDRPaths.COMMON_DIRECTORY + dir + "/";
            Factory factory = Factory.make(sourceDirectory, ".*");

            CLDRFileTransformer transformer =
                    new CLDRFileTransformer(
                            factory, CLDRPaths.COMMON_DIRECTORY + "transforms" + File.separator);
            for (LocaleTransform localeTransform : LocaleTransform.values()) {
                if (!matchPattern.matcher(localeTransform.getOutputLocale()).matches()) {
                    if (VERBOSE) System.out.println("Skipping: " + localeTransform);
                    continue;
                }
                CLDRFile output = transformer.transform(localeTransform);
                if (output == null) {
                    System.out.println(
                            "SKIPPING missing file: "
                                    + dir
                                    + "/"
                                    + localeTransform.inputLocale
                                    + ".xml");
                    continue;
                }
                String outputFile = output.getLocaleID() + ".xml";
                if (VERBOSE) System.out.println("Begin: " + localeTransform);

                try (TempPrintWriter out =
                        TempPrintWriter.openUTF8Writer(sourceDirectory, outputFile)
                                .skipCopyright(true)) {
                    if (!transformer.unconverted.isEmpty()) {
                        System.out.println("Untransformed characters: " + transformer.unconverted);
                        transformer.unconverted.clear();
                    }
                    output.write(out.asPrintWriter());
                }
            }
        }
    }
}
