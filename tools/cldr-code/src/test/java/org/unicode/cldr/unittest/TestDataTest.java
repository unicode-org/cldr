package org.unicode.cldr.unittest;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.ibm.icu.util.ICUUncheckedIOException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.personname.PersonNameFormatter;
import org.unicode.cldr.util.personname.PersonNameFormatter.FormatParameters;
import org.unicode.cldr.util.personname.SimpleNameObject;

/**
 * Check that all of the test data files (in common/testData) are themselves tested in CLDR.
 *
 * <ul>
 *   <li>Call skipFile(name) when processing each file, to skip files that shouldn't be processed
 *   <li>Otherwise, add the files to FILES_CHECKED, so that the final test can check that all files
 *       are processed.
 *   <li>For debugging, you can set TestDataTest:SHOW_PROGRESS to see the progress.
 *   <li>You can set TestDataTest:FILE_FILTER to just focus on particular files.
 * </ul>
 */
public class TestDataTest extends TestFmwkPlus {
    static final boolean SHOW_PROGRESS = System.getProperty("TestDataTest:SHOW_PROGRESS") != null;
    static final Pattern FILE_FILTER;

    static {
        String prop = System.getProperty("TestDataTest:FILE_FILTER");
        FILE_FILTER = prop == null ? null : Pattern.compile(prop);
    }

    private static final String TEST_DATA_DIR = CLDRPaths.COMMON_DIRECTORY + "testData/";

    static CLDRConfig testInfo = CLDRConfig.getInstance();
    static Factory FACTORY = testInfo.getCldrFactory();
    private static final SupplementalDataInfo SUPPLEMENTAL_DATA_INFO =
            testInfo.getSupplementalDataInfo();

    static final Splitter SEMI_SPLIT = Splitter.on(';').trimResults();
    static final Splitter COMMA_SPLIT = Splitter.on(',').trimResults();

    public static void main(String[] args) {
        new TestDataTest().run(args);
    }

    /** Call this when processing files, and skip if null (which will be doc files, etc) */
    public String getFileName(Path filePath) {
        String name = filePath.toFile().getName();
        final boolean skip =
                name.startsWith("_")
                        || name.startsWith(".")
                        || name.endsWith(".md")
                        || (FILE_FILTER != null && !FILE_FILTER.matcher(name).matches());
        if (!skip && SHOW_PROGRESS) {
            System.out.println(filePath);
        }
        return skip ? null : name;
    }

    /** List of directories in testData/ that are tested. Each test adds the ones it handles. */
    static final Set<Path> FILES_CHECKED = new TreeSet<>();

    /** See GeneratePersonNameTestData */
    public void testPersonNameTests() {
        if (SHOW_PROGRESS) {
            System.out.println();
        }
        Path PERSON_NAMES_DIR = Paths.get(TEST_DATA_DIR, "personNameTest");
        try (DirectoryStream<Path> filepath = Files.newDirectoryStream(PERSON_NAMES_DIR)) {
            filepath.forEach(x -> checkPersonNameTests(x));
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
        FILES_CHECKED.add(PERSON_NAMES_DIR);
    }

    private void checkPersonNameTests(Path filePath) {
        String name = getFileName(filePath);
        if (name == null) {
            return;
        }
        String localeId = name.substring(0, name.length() - 4);
        if (localeId.equals("km")) {
            logKnownIssue(
                    "CLDR-18870",
                    "With ICU4J 2025-08-05, testData/personNameTest/km.txt needs updating");
            return;
        }
        final PersonNameData pnd = new PersonNameData();
        pnd.pnf = new PersonNameFormatter(FACTORY.make(localeId, true));
        pnd.localeId = localeId;
        if (SHOW_PROGRESS) {
            System.out.println(filePath);
        }
        try {
            Files.lines(filePath).forEach(line -> pnd.checkLine(line));
            FILES_CHECKED.add(filePath);
        } catch (Exception e) {
            errln(e.getMessage());
        }
    }

    enum PersonNameState {
        start,
        haveName,
        haveResult
    }

    boolean gavePersonNameFormattingGuidance = false;

    static final FormatParameters testParameters =
            FormatParameters.from(
                    "order=surnameFirst; length=long; usage=monogram; formality=informal");

    class PersonNameData {
        private static final char NAME_PATTERN_SEPERATOR = '∪';
        int count = 0;
        StringBuilder namePattern = new StringBuilder();
        SimpleNameObject sampleName = null;
        String expectedResult = null;
        PersonNameFormatter pnf = null;
        String localeId = null;
        String nameLocale = null;
        PersonNameState pnState = PersonNameState.start;
        Map<String, String> valueToKey = new TreeMap<>();

        //                #     name ; given; Iris
        //                #     name ; surname; Falke
        //                #     name ; locale; de
        //                #
        //                #     expectedResult; Falke, Iris
        //                #
        //                #     parameters; sorting; long; referring; formal
        //                #     parameters; sorting; medium; referring; informal
        //                #
        //                #     endName
        //                #
        //                #     name ; given; Max
        //                #     name ; given2; Ben
        //                #     name ; surname; Mustermann

        void checkLine(String line) {
            try {
                ++count;
                if (line.startsWith("#") || line.isBlank()) {
                    // skip
                } else {
                    List<String> parts = SEMI_SPLIT.splitToList(line);
                    switch (parts.get(0)) {
                        case "enum":
                            // enum ; length ; long, medium, short
                            String parameter = parts.get(1);
                            if (parameter.equals("order")) { // special additions
                                valueToKey.put("givenFirst", parameter);
                                valueToKey.put("surnameFirst", parameter);
                            }
                            for (String value : COMMA_SPLIT.split(parts.get(2))) {
                                valueToKey.put(value, parameter);
                            }
                            break;
                        case "name":
                            // name ; given; Iris
                            // name ; locale; de
                            if (pnState == PersonNameState.start) {
                                pnState = PersonNameState.haveName;
                                namePattern.setLength(0);
                            } else {
                                namePattern.append(NAME_PATTERN_SEPERATOR).append(' ');
                            }
                            // locale=fr, title=Dr., given=John, given2-initial=B.,...
                            namePattern.append(parts.get(1)).append('=').append(parts.get(2));
                            break;
                        case "expectedResult":
                            expectedResult = parts.get(1); // TODO unescape ";"
                            if (pnState == PersonNameState.haveName) {
                                sampleName =
                                        SimpleNameObject.from(
                                                NAME_PATTERN_SEPERATOR, namePattern.toString());
                                pnState = PersonNameState.haveResult;
                            }
                            break;
                        case "parameters":
                            // order=sorting; length=long; usage=referring; formality=formal
                            StringBuilder parameters = new StringBuilder();
                            for (int i = 1; i < parts.size(); ++i) {
                                String value = parts.get(i);
                                if (value.equals("none")) { // special order indicating use locale)
                                    value =
                                            pnf.getOrderFromLocale(sampleName.getNameLocale())
                                                    .toString();
                                }
                                String key = valueToKey.get(value);
                                if (key == null) {
                                    int debug = 0;
                                }
                                parameters.append(key).append('=').append(value).append("; ");
                            }
                            String actual;
                            try {
                                FormatParameters formatParameters =
                                        FormatParameters.from(parameters.toString());
                                if (formatParameters.equals(testParameters)) {
                                    int debug = 0;
                                }

                                actual =
                                        pnf.formatWithoutSuperscripts(sampleName, formatParameters);
                            } catch (Exception e) {
                                actual = e.getMessage();
                            }
                            if (!assertEquals(
                                    localeId + " (" + count + ") ", expectedResult, actual)) {
                                if (!gavePersonNameFormattingGuidance) {
                                    warnln(
                                            "This problem could be caused by a failure to regenerate the test data with GeneratePersonNameTestData.java"
                                                    + " after Person Name Formatting data changed");
                                    gavePersonNameFormattingGuidance = true;
                                }
                            }
                            break;
                        case "endName":
                            pnState = PersonNameState.start;
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown value");
                    }
                }
            } catch (Exception e) {
                throw new IllegalArgumentException(localeId + " (" + count + ") " + line, e);
            }
        }
    }

    /** Make sure this is the last test */
    public void testΩ() {
        Set<Path> missing = new TreeSet<>();
        try (DirectoryStream<Path> filepath = Files.newDirectoryStream(Paths.get(TEST_DATA_DIR))) {
            filepath.forEach(x -> checkDirectories(x, missing));
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
        // TODO once we get full tests, turn this on
        //        if (!assertEquals("Files all tested", 0, missing.size())) {
        //            warnln("\n\t" + Joiner.on("\n\t").join(missing));
        //        }
        if (!missing.isEmpty()) {
            if (logKnownIssue("CLDR-17910", "Missing tests for files in common/testData/")) {
                warnln("Files or Directories missing tests:\n\t" + Joiner.on("\n\t").join(missing));
            } else {
                errln("Files or Directories missing tests:\n\t" + Joiner.on("\n\t").join(missing));
            }
        } else if (FILE_FILTER != null) {
            warnln(
                    "Files or Directories missing tests are not checked unless they match the filter: "
                            + FILE_FILTER.pattern());
        }
    }

    /**
     * If we tested any file in a directory, then we put the directory in FILES_CHECKED. So to show
     * what is missing, we can stop at directories that are not in FILES_CHECKED.
     */
    private void checkDirectories(Path filepath, Set<Path> missing) {
        String name = getFileName(filepath);
        if (name == null) {
            return;
        }
        if (FILES_CHECKED.contains(filepath)) {
            // this file is ok, but if it is a directory we need to check below
            if (filepath.toFile().isDirectory()) {
                try (DirectoryStream<Path> filepath2 = Files.newDirectoryStream(filepath)) {
                    filepath2.forEach(x -> checkDirectories(x, missing));
                } catch (IOException e) {
                    throw new ICUUncheckedIOException(e);
                }
            }
        } else {
            missing.add(filepath);
        }
    }

    public void testLikelySubtags() {
        if (SHOW_PROGRESS) {
            System.out.println();
        }
        Path likelySubtagsPath = Paths.get(TEST_DATA_DIR, "localeIdentifiers", "likelySubtags.txt");
        LikelySubtags likelyFavorRegion =
                new LikelySubtags(SUPPLEMENTAL_DATA_INFO.getLikelySubtags()).setFavorRegion(true);
        LikelySubtags likelyFavorScript =
                new LikelySubtags(SUPPLEMENTAL_DATA_INFO.getLikelySubtags()).setFavorRegion(false);

        String name = getFileName(likelySubtagsPath);
        if (name == null) {
            return;
        }
        try {
            // # Source ;   AddLikely ; RemoveFavorScript ; RemoveFavorRegion
            Files.lines(likelySubtagsPath)
                    .forEach(line -> checkLikely(likelyFavorRegion, likelyFavorScript, line));
            FILES_CHECKED.add(likelySubtagsPath);
        } catch (Exception e) {
            errln(e.getMessage());
        }
    }

    private void checkLikely(
            LikelySubtags likelyFavorRegion, LikelySubtags likelyFavorScript, String line) {
        // # Source ;   AddLikely ; RemoveFavorScript ; RemoveFavorRegion
        if (line.isBlank() || line.startsWith("#")) {
            return;
        }
        List<String> parts = SEMI_SPLIT.splitToList(line);
        if (parts.size() != 4) {
            errln("Too few items on line: " + line);
        }
        /*
        #   AddLikely: the result of the Add Likely Subtags.
        #                      If Add Likely Subtags fails, then “FAIL”.
        #   RemoveFavorScript: Remove Likely Subtags, when the script is favored.
        #                      Only included when different than AddLikely.
        #   RemoveFavorRegion: Remove Likely Subtags, when the region is favored.
        #                      Only included when different than RemoveFavorScript.

                 */
        String source = parts.get(0);
        final String expectedMax = parts.get(1);

        // if the maxLang is empty, we have no data for the language
        String lang = CLDRLocale.getInstance(source.replace('-', '_')).getLanguage();
        String maxLang = likelyFavorScript.maximize(lang);
        final boolean fails = maxLang == null;

        final String maximized = fails ? null : likelyFavorScript.maximize(source);
        final String minimizedFavorRegion = fails ? null : likelyFavorRegion.minimize(source);
        final String minimizedFavorScript = fails ? null : likelyFavorScript.minimize(source);

        assertEquals("Maximizing " + source, expectedMax, checkNullAndFix(maximized));

        final String expectedMinFavorScript = CldrUtility.ifEqual(parts.get(2), "", expectedMax);
        assertEquals(
                "Minimizing (favor script) " + source,
                expectedMinFavorScript,
                checkNullAndFix(minimizedFavorScript));

        final String expectedMinFavorRegion =
                CldrUtility.ifEqual(parts.get(3), "", expectedMinFavorScript);
        assertEquals(
                "Minimizing (favor region) " + source,
                expectedMinFavorRegion,
                checkNullAndFix(minimizedFavorRegion));
    }

    public String checkNullAndFix(final String likelyResult) {
        return likelyResult == null ? "FAIL" : likelyResult.replace('_', '-');
    }
}
