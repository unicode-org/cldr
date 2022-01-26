package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.test.DisplayAndInputProcessor;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.tool.Option.Params;
import org.unicode.cldr.util.Annotations;
import org.unicode.cldr.util.Annotations.AnnotationSet;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Emoji;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.XPathParts.Comments.CommentType;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSortedSet;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.impl.locale.XCldrStub.ImmutableMap;
import com.ibm.icu.text.UnicodeSet;

public class GenerateDerivedAnnotations {
    // Use EmojiData.getDerivableNames() to update this for each version of Unicode.

    private static final CLDRConfig CLDR_CONFIG = CLDRConfig.getInstance();

    static final UnicodeSet SKIP = new UnicodeSet()
        .add(Annotations.ENGLISH_MARKER)
        .add(Annotations.BAD_MARKER)
        .add(Annotations.MISSING_MARKER)
        .freeze();

    static Map<String,String> codepointToIsoCurrencyCode;
    static {
        final Splitter tabSplitter = Splitter.on('\t').trimResults();
        Map<String,String> _codepointToIsoCurrencyCode = new TreeMap<>();
        for (String line : FileUtilities.in(CldrUtility.class, "data/codepointToIsoCurrencyCode.tsv")) {
            if (line.startsWith("#")) {
                continue;
            }
            List<String> parts = tabSplitter.splitToList(line);
            _codepointToIsoCurrencyCode.put(parts.get(0), parts.get(1));
        }
        codepointToIsoCurrencyCode = ImmutableMap.copyOf(_codepointToIsoCurrencyCode);
    }

    private enum MyOptions {
        fileFilter(new Params().setHelp("filter files by dir/locale, eg: ^main/en$ or .*/en").setMatch(".*").setDefault(".*")),
        missing(new Params().setHelp("only missing").setMatch("")),
        ;

        // BOILERPLATE TO COPY
        final Option option;

        private MyOptions(Params params) {
            option = new Option(this, params);
        }

        private static Options myOptions = new Options();
        static {
            for (MyOptions option : MyOptions.values()) {
                myOptions.add(option, option.option);
            }
        }

        private static Set<String> parse(String[] args) {
            return myOptions.parse(MyOptions.values()[0], args, true);
        }
    }

    public static void main(String[] args) throws IOException {
        MyOptions.parse(args);

        boolean missingOnly = MyOptions.missing.option.doesOccur();
        if (missingOnly) {
            System.out.println("With the 'missing' argument files will not be written, only the missing items will be written to the console");
        }

        Matcher localeMatcher = Pattern.compile(MyOptions.fileFilter.option.getValue()).matcher("");
        Joiner BAR = Joiner.on(" | ");
        AnnotationSet enAnnotations = Annotations.getDataSet("en");
        CLDRFile english = CLDR_CONFIG.getEnglish();

        UnicodeSet derivables = new UnicodeSet(Emoji.getAllRgiNoES())
            .addAll(codepointToIsoCurrencyCode.keySet())
            .removeAll(enAnnotations.keySet())
            .freeze();

        for (String d : derivables) {
            if (d.contains("💏🏻")) {
                System.out.println(d + "\t" + Utility.hex(d));
            }
        }

        Map<String, UnicodeSet> localeToFailures = new LinkedHashMap<>();
        Set<String> locales = ImmutableSortedSet.copyOf(Annotations.getAvailable());
        final Factory cldrFactory = CLDRConfig.getInstance().getCldrFactory();
        final Map<String, Integer> failureMap = new TreeMap<>();
        int processCount = 0;

        for (String locale : locales) {
            if ("root".equals(locale)) {
                continue;
            }
            if (!localeMatcher.reset(locale).matches()) {
                continue;
            }
            processCount++;
            UnicodeSet failures = new UnicodeSet(Emoji.getAllRgiNoES());
            localeToFailures.put(locale, failures);

            AnnotationSet annotations;
            try {
                annotations = Annotations.getDataSet(locale);
                failures.removeAll(annotations.getExplicitValues());
            } catch (Exception e) {
                System.out.println("Can't create annotations for: " + locale + "\n\t" + e.getMessage());
                annotations = Annotations.getDataSet(locale);
                continue;
            }
            CLDRFile target = new CLDRFile(new SimpleXMLSource(locale));
            CLDRFile main = null;
            DisplayAndInputProcessor DAIP = new DisplayAndInputProcessor(target);
            Exception[] internalException = new Exception[1];

            target.addComment("//ldml", "Derived short names and annotations, using GenerateDerivedAnnotations.java. See warnings in /annotations/ file.",
                CommentType.PREBLOCK);
            for (String derivable : derivables) {
                String shortName = null;
                try {
                    shortName = annotations.getShortName(derivable);
                } catch (Exception e) {
                }

                if (shortName == null) {
                    String currencyCode = codepointToIsoCurrencyCode.get(derivable);
                    if (currencyCode != null) {
                        if (main == null) {
                            main = cldrFactory.make(locale, true);
                        }
                        shortName = main.getName(CLDRFile.CURRENCY_NAME, currencyCode);
                        if (shortName.contentEquals(currencyCode)) {
                            shortName = null; // don't want fallback raw code
                        }
                    }
                }

                if (shortName == null || SKIP.containsSome(shortName)) {
                    continue; // missing
                }
                Set<String> keywords = annotations.getKeywordsMinus(derivable);
                String path = "//ldml/annotations/annotation[@cp=\"" + derivable + "\"]";
                if (!keywords.isEmpty()) {
                    Set<String> keywordsFixed = new HashSet<>();
                    for (String keyword : keywords) {
                        if (!SKIP.containsSome(keyword)) {
                            keywordsFixed.add(keyword);
                        }
                    }
                    if (!keywordsFixed.isEmpty()) {
                        String value = BAR.join(keywordsFixed);
                        String newValue = DAIP.processInput(path, value, internalException);
                        target.add(path, newValue);
                    }
                }
                failures.remove(derivable);
                String ttsPath = path + "[@type=\"tts\"]";
                String shortName2 = DAIP.processInput(path, shortName, internalException);
                target.add(ttsPath, shortName2);
            }
            failures.freeze();
            if (!failures.isEmpty()) {
                Level level = StandardCodes.make().getLocaleCoverageLevel(Organization.cldr, locale);
                System.out.println("Failures\t" + locale
                    + "\t" + level
                    + "\t" + english.getName(locale)
                    + "\t" + failures.size()
                    + "\t" + failures.toPattern(false));
                failureMap.put(locale, failures.size());
            }
            if (missingOnly) {
                continue;
            }
            try (PrintWriter pw = FileUtilities.openUTF8Writer(CLDRPaths.COMMON_DIRECTORY + "annotationsDerived", locale + ".xml")) {
                target.write(pw);
            }
        }
        Factory factory = Factory.make(CLDRPaths.COMMON_DIRECTORY + "annotationsDerived", ".*");
        for (String locale : locales) {
            if ("root".equals(locale)) {
                continue;
            }
            if (!localeMatcher.reset(locale).matches()) {
                continue;
            }
            CLDRFile cldrFileUnresolved = factory.make(locale, false);
            CLDRFile cldrFileResolved = factory.make(locale, true);
            Set<String> toRemove = new TreeSet<>(); // TreeSet just makes debugging easier
            boolean gotOne = false;
            for (String xpath : cldrFileUnresolved) {
                if (xpath.startsWith("//ldml/identity")) {
                    continue;
                }

                String value = cldrFileUnresolved.getStringValue(xpath);

                // remove items that are the same as their bailey values. This also catches Inheritance Marker

                String bailey = cldrFileResolved.getConstructedBaileyValue(xpath, null, null);
                if (value.equals(bailey)) {
                    toRemove.add(xpath);
                    continue;
                }
                gotOne = true;
            }
            if (!gotOne) {
                if (locale.equals("sr_Cyrl")) {
                    System.err.println("TODO: keep from deleting files with non-empty children");
                } else {
                    System.out.println("Removing empty " + locale);
                    new File(CLDRPaths.COMMON_DIRECTORY + "annotationsDerived", locale + ".xml").deleteOnExit();
                }
            } else if (!toRemove.isEmpty()) {
                System.out.println("Removing " + toRemove.size() + " items from " + locale);
                CLDRFile fileToWrite = cldrFileUnresolved.cloneAsThawed();
                fileToWrite.removeAll(toRemove, false);
                File file = new File(CLDRPaths.COMMON_DIRECTORY + "annotationsDerived", locale + ".xml");
                try (PrintWriter pw = new PrintWriter(file)) {
                    fileToWrite.write(pw);
                }
            }
        }
        System.out.println("Be sure to run CLDRModify passes afterwards, and generate transformed locales (like de-CH).");
        if (!failureMap.isEmpty()) {
            failureMap.entrySet().forEach(e -> System.err.printf("ERROR: %s: %d errors\n", e.getKey(), e.getValue()));
            System.err.printf("ERROR: Errors in %d/%d locales.\n", failureMap.size(), processCount);
            System.exit(1);
        } else if(processCount == 0) {
            System.err.println("ERROR: No locales matched. Check the -f option.\n");
            System.exit(1);
        } else {
            System.out.printf("OK: %d locales processed without error\n", processCount);
            System.exit(0);
        }
    }
}
