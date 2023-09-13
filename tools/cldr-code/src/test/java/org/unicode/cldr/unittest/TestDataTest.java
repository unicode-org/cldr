package org.unicode.cldr.unittest;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.io.IOException;
import java.io.UncheckedIOException;
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
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.personname.PersonNameFormatter;
import org.unicode.cldr.util.personname.PersonNameFormatter.FormatParameters;
import org.unicode.cldr.util.personname.SimpleNameObject;

public class TestDataTest extends TestFmwkPlus {

    static final boolean SHOW_PROGRESS = System.getProperty("TestDataTest:SHOW_PROGRESS") != null;
    static final String FILE_FILTER = System.getProperty("TestDataTest:FILE_FILTER");

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

    /** List of directories in testData/ that are tested. Each test adds the ones it handles. */
    static final Set<Path> FILES_CHECKED = new TreeSet<>();

    /** See GeneratePersonNameTestData */
    public void testPersonNameTests() {
        if (SHOW_PROGRESS) {
            System.out.println();
        }
        Path PERSON_NAMES_DIR = Paths.get(TEST_DATA_DIR + "personNameTest");
        try (DirectoryStream<Path> filepath = Files.newDirectoryStream(PERSON_NAMES_DIR)) {
            filepath.forEach(x -> checkPersonNameTests(x));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        FILES_CHECKED.add(PERSON_NAMES_DIR);
    }

    private void checkPersonNameTests(Path filePath) {
        String name = filePath.toFile().getName();
        if (name.startsWith("_") || !name.endsWith(".txt")) {
            return;
        }
        if (FILE_FILTER != null && !Pattern.compile(FILE_FILTER).matcher(name).matches()) {
            return;
        }
        String localeId = name.substring(0, name.length() - 4);
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
            throw new UncheckedIOException(e);
        }
        // TODO once we get full tests, turn this on
        //        if (!assertEquals("Files all tested", 0, missing.size())) {
        //            warnln("\n\t" + Joiner.on("\n\t").join(missing));
        //        }
        warnln("Files or Directories missing tests:\n\t" + Joiner.on("\n\t").join(missing));
    }

    /**
     * If we tested any file in a directory, then we put the directory in FILES_CHECKED. So to show
     * what is missing, we can stop at directories that are not in FILES_CHECKED.
     */
    private void checkDirectories(Path filepath, Set<Path> missing) {
        final String name = filepath.getFileName().toString();
        if (name.startsWith(".") || name.startsWith("_")) {
            return;
        }
        if (FILES_CHECKED.contains(filepath)) {
            // this file is ok, but if it is a directory we need to check below
            if (filepath.toFile().isDirectory()) {
                try (DirectoryStream<Path> filepath2 = Files.newDirectoryStream(filepath)) {
                    filepath2.forEach(x -> checkDirectories(x, missing));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        } else {
            missing.add(filepath);
        }
    }
}
