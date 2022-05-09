package org.unicode.cldr.tool;

import com.google.common.base.Joiner;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.VersionInfo;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.unicode.cldr.test.DisplayAndInputProcessor;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.SimpleFactory;

public class FindFlipFlops {

    private static int flipFlopCount = 0;

    private static CldrVersion[] VERSIONS;

    private static Factory[] factories;

    private static final boolean USE_RESOLVED = false;

    static final Options myOptions = new Options()
        .add(
            "file",
            ".*",
            ".*",
            "Filter the information based on file name, using a regex argument. The '.xml' is removed from the file before filtering"
        )
        .add("oldest", "\\d+(\\.\\d+)?(\\.\\d+)?", "1.1", "Oldest version to go back to, eg 36.1");

    public static void main(String[] args) throws IOException {
        myOptions.parse(args, true);
        confirmVersionArchiveIsPresent();
        VERSIONS = makeVersionArray();
        factories = setUpFactories();

        testFindFlipFlop();

        // load and process English
        System.out.println("en");
        findFlops("en");

        // Load and process all the locales
        LanguageTagParser ltp = new LanguageTagParser();
        for (String fileName : factories[0].getAvailable()) {
            if (fileName.equals("en")) {
                continue;
            }
            if (!ltp.set(fileName).getRegion().isEmpty()) {
                continue; // skip region locales
            }
            System.out.println(fileName);
            findFlops(fileName);
        }
        System.out.println("Total " + flipFlopCount + " flip-flops");
    }

    private static void confirmVersionArchiveIsPresent() {
        try {
            CldrVersion.checkVersions();
        } catch (Exception e) {
            throw new ICUException(
                "This tool can only be run if the archive of released versions matching CldrVersion is available.",
                e
            );
        }
    }

    private static CldrVersion[] makeVersionArray() {
        final VersionInfo oldest = VersionInfo.getInstance(myOptions.get("oldest").getValue());
        final List<CldrVersion> versions = new ArrayList<>();
        boolean foundStart = false;
        for (CldrVersion version : CldrVersion.CLDR_VERSIONS_DESCENDING) {
            versions.add(version);
            if (version.getVersionInfo() == oldest) {
                foundStart = true;
                break;
            }
        }
        if (!foundStart) {
            throw new IllegalArgumentException(
                "The last version is " +
                myOptions.get("oldest").getValue() +
                "; it must be in: " +
                Joiner.on(", ").join(CldrVersion.CLDR_VERSIONS_DESCENDING)
            );
        }
        return versions.toArray(new CldrVersion[0]);
    }

    private static Factory[] setUpFactories() {
        Factory[] factories = new Factory[VERSIONS.length];
        String filePattern = myOptions.get("file").getValue();
        ArrayList<Factory> list = new ArrayList<>();
        for (CldrVersion version : VERSIONS) {
            if (version == CldrVersion.unknown) {
                continue;
            }
            List<File> paths = version.getPathsForFactory();
            System.out.println(version + ", " + paths);
            Factory aFactory = SimpleFactory.make(paths.toArray(new File[0]), filePattern);
            list.add(aFactory);
        }
        list.toArray(factories);
        return factories;
    }

    private static void findFlops(String fileName) {
        CLDRFile[] files = new CLDRFile[factories.length];
        DisplayAndInputProcessor[] processors = new DisplayAndInputProcessor[factories.length];
        setUpFilesAndProcessors(fileName, files, processors);
        for (String xpath : files[0]) {
            xpath = xpath.intern();
            final String base = getProcessedStringValue(xpath, files[0], processors[0]);
            final ArrayList<String> history = new ArrayList<>();
            history.add(base);
            for (int i = 1; i < files.length && files[i] != null; ++i) {
                final String previous = getProcessedStringValue(xpath, files[i], processors[0]);
                if (previous == null) {
                    break;
                }
                history.add(previous);
            }
            if (findFlipFlop(history)) {
                ++flipFlopCount;
                System.out.println("🏓🏓 " + flipFlopCount + " " + xpath + " " + fileName);
                history.forEach(System.out::println);
            }
        }
    }

    private static void setUpFilesAndProcessors(
        String fileName,
        CLDRFile[] files,
        DisplayAndInputProcessor[] processors
    ) {
        for (int i = 0; i < factories.length; ++i) {
            try {
                files[i] = factories[i].make(fileName, USE_RESOLVED);
                processors[i] = new DisplayAndInputProcessor(files[i], false);
            } catch (Exception e) {
                // stop when we fail to find
                break;
            }
        }
    }

    private static class TestData {

        String[] values;
        boolean expectFlipFlop;

        public TestData(String[] values, boolean expectFlipFlop) {
            this.values = values;
            this.expectFlipFlop = expectFlipFlop;
        }
    }

    private static final TestData d0 = new TestData(new String[] { "a", "a" }, false);
    private static final TestData d1 = new TestData(new String[] { "a", "b", "c" }, false);
    private static final TestData d2 = new TestData(new String[] { "a", "b", "a" }, true);
    private static final TestData d3 = new TestData(new String[] { "a", "b", "c", "d" }, false);
    private static final TestData d4 = new TestData(new String[] { "a", "b", "c", "a" }, true);
    private static final TestData d5 = new TestData(new String[] { "a", "b", "c", "b" }, true);
    private static final TestData[] testData = { d0, d1, d2, d3, d4, d5 };

    private static void testFindFlipFlop() {
        for (TestData data : testData) {
            ArrayList<String> history = new ArrayList<>(List.of(data.values));
            if (findFlipFlop(history) != data.expectFlipFlop) {
                System.out.println("🏓🏓🏓 testFindFlipFlop FAILURE:" + String.join(",\t", history));
            }
        }
    }

    private static boolean findFlipFlop(final ArrayList<String> history) {
        if (history.size() < 3) {
            return false;
        }
        for (int i = 0; i < history.size() - 1; i++) {
            String hi = history.get(i);
            for (int j = i + 2; j < history.size(); j++) {
                String hj = history.get(j);
                if (hj.equals(hi) && !hj.equals(history.get(j - 1))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String getProcessedStringValue(String xpath, CLDRFile file, DisplayAndInputProcessor processor) {
        String base = file.getStringValue(xpath);
        if (base != null) {
            base = processor.processInput(xpath, base, null);
        }
        return base;
    }
}
