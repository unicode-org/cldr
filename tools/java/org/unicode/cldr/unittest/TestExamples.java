package org.unicode.cldr.unittest;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.test.ExampleGenerator.ExampleContext;
import org.unicode.cldr.test.ExampleGenerator.ExampleType;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.DeprecationChecker;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.StringId;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;

public class TestExamples extends TestFmwk {
    static String DEBUG = "ALK";
    static boolean resolved = false;

    public static void main(String[] args) {
        new TestExamples().run(args);
    }

    static TestInfo testInfo = TestInfo.getInstance();

    public void TestBasic() {
        final String localeId = "fr";
        checkLocale(localeId);
    }

    public void checkLocale(final String localeId) {
        final CLDRFile nativeFile = testInfo.getCldrFactory().make(localeId, true);
        PathHeader.Factory header = PathHeader.getFactory(testInfo.getEnglish());

        RegexLookup<String> expectPathsWithExamples = new RegexLookup<String>()
            .setPatternTransform(RegexLookup.RegexFinderTransformPath)
            .loadFromFile(TestExamples.class, "TestExamples.txt");

        PathStarrer pathStarrer = new PathStarrer();
        Relation<String, String> pathsToAttributes = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);
        Relation<String, String> examplePathToAttributes = Relation.of(new TreeMap<String, Set<String>>(),
            TreeSet.class);

        DeprecationChecker deprecationTester = new DeprecationChecker();
        Set<String> deprecated = new TreeSet<String>();

        ExampleGenerator exampleGenerator = new ExampleGenerator(nativeFile, testInfo.getEnglish(),
            CldrUtility.SUPPLEMENTAL_DIRECTORY);
        ExampleContext exampleContext = new ExampleContext();
        Map<PathHeader, String> sortedExamples = isVerbose() ? new TreeMap() : null;
        Status status = new Status();

        for (String path : nativeFile) {
            if (!resolved) {
                String sourceLocale = nativeFile.getSourceLocaleID(path, status);
                if (!localeId.equals(sourceLocale) || !path.equals(status.pathWhereFound)) {
                    continue;
                }
            }
            if (deprecationTester.isBoilerplate(path)) {
                continue;
            }
            if (deprecationTester.isDeprecated(path)) {
                deprecated.add(path);
                continue;
            }
            if (path.contains(DEBUG)) {
                int x = 0; // for debuggin
            }
            String value = nativeFile.getStringValue(path);
            String example = exampleGenerator.getExampleHtml(path, value, ExampleGenerator.Zoomed.OUT, exampleContext,
                ExampleType.NATIVE);
            if (example == null) {
                if (expectPathsWithExamples.get(path) != null) {
                    errln("Expected example for " + path);
                }
                addSample(pathStarrer, pathsToAttributes, path, value);
            } else if (sortedExamples != null) {
                sortedExamples.put(header.fromPath(path), "\t" + value + "\t" + example);
                addSample(pathStarrer, examplePathToAttributes, path, value);
            }
        }
        if (sortedExamples != null) {
            for (Entry<PathHeader, String> item : sortedExamples.entrySet()) {
                final PathHeader pathHeader = item.getKey();
                logln("examples:\t" + pathHeader + StringId.getId(pathHeader.getOriginalPath()) + "\t"
                    + item.getValue());
            }
        }
        for (String item : deprecated) {
            logln("deprecated:\t" + item);
        }

        for (Entry<String, Set<String>> item : examplePathToAttributes.keyValuesSet()) {
            logln("example:\t" + item.getKey() + "\t" + item.getValue().iterator().next() + "...");
        }

        // Later test expected missing, and just log them
        for (Entry<String, Set<String>> item : pathsToAttributes.keyValuesSet()) {
            logln("no example:\t" + item.getKey() + "\t" + item.getValue().iterator().next() + "...");
        }
    }

    public void addSample(PathStarrer pathStarrer, Relation<String, String> pathsToAttributes,
        String path, String value) {
        String starred = pathStarrer.set(path).replace("[@alt=\"([^\"]*+)\"]", "");
        List<String> attributes = pathStarrer.getAttributes();
        pathsToAttributes.put(starred, "<" + value + ">\t" + CollectionUtilities.join(attributes, "|"));
    }
}
