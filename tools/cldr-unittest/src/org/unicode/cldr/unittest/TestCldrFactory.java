package org.unicode.cldr.unittest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.ibm.icu.dev.util.CollectionUtilities;

public class TestCldrFactory extends TestFmwkPlus {
    private static final boolean DEBUG = false;

    static CLDRConfig testInfo = CLDRConfig.getInstance();
    static SupplementalDataInfo sdi = testInfo.getSupplementalDataInfo();

    public static void main(String[] args) {
        new TestCldrFactory().run(args);
    }

    public void testDirectories() {
        File[] paths = {
            new File(CLDRPaths.MAIN_DIRECTORY),
            new File(CLDRPaths.ANNOTATIONS_DIRECTORY),
            new File(CLDRPaths.SUPPLEMENTAL_DIRECTORY)
        };
        Factory factory = SimpleFactory.make(paths, ".*");
        List<File> enExpected = Arrays.asList(new File(CLDRPaths.MAIN_DIRECTORY), new File(CLDRPaths.ANNOTATIONS_DIRECTORY));

        File[] dirs = factory.getSourceDirectories();
        assertEquals("", Arrays.asList(paths), Arrays.asList(dirs));

        List<File> enDirs = factory.getSourceDirectoriesForLocale("en");
        assertEquals("", enExpected, enDirs);

        // Make sure old method works
        File enDir = factory.getSourceDirectoryForLocale("en");
        assertEquals("", new File(CLDRPaths.MAIN_DIRECTORY), enDir);
    }

    public void testMerge() {
        CLDRFile enMain = testInfo.getCldrFactory().make("en", false);
        assertEquals("no annotations", Status.noAnnotations, checkAnnotations(enMain));

        Factory factoryAnnotations = SimpleFactory.make(CLDRPaths.ANNOTATIONS_DIRECTORY, ".*");
        CLDRFile enAnnotations = factoryAnnotations.make("en", false);
        assertEquals("annotations only", Status.onlyAnnotations, checkAnnotations(enAnnotations));

        File[] paths = { new File(CLDRPaths.MAIN_DIRECTORY), new File(CLDRPaths.ANNOTATIONS_DIRECTORY) };
        Factory factoryDouble = SimpleFactory.make(paths, ".*");

        CLDRFile enDouble = factoryDouble.make("en", false);
        assertEquals("annotations only", Status.mixed, checkAnnotations(enDouble));

        assertEquals("no annotations", Status.noAnnotations, checkAnnotations(enMain));
        assertEquals("annotations only", Status.onlyAnnotations, checkAnnotations(enAnnotations));
        assertEquals("annotations only", Status.mixed, checkAnnotations(enDouble));

        assertEquals("en subset of enDouble", null, getUncontainedPath(enMain, enDouble));
        assertEquals("enAnnotations subset of enDouble", null, getUncontainedPath(enAnnotations, enDouble));
    }

    enum Status {
        none, onlyAnnotations, noAnnotations, mixed
    }

    private Status checkAnnotations(CLDRFile cldrFile) {
        Status status = Status.none;
        for (String xpath : cldrFile) {
            if (xpath.startsWith("//ldml/identity")) continue;
            boolean isAnnotation = xpath.startsWith("//ldml/annotation");
            if (isAnnotation) {
                switch (status) {
                case none:
                    status = Status.onlyAnnotations;
                    break;
                case noAnnotations:
                    return Status.mixed;
                }
            } else {
                switch (status) {
                case none:
                    status = Status.noAnnotations;
                    break;
                case onlyAnnotations:
                    return Status.mixed;
                }
            }
        }
        return status;
    }

    /**
     * Returns first string that has a different value in superset than in subset.
     * @param subset
     * @param superset
     * @return
     */
    private String getUncontainedPath(CLDRFile subset, CLDRFile superset) {
        int debugCount = 0;
        for (String xpath : subset) {
            if (++debugCount < 100) {
                logln(debugCount + "\t" + xpath);
            }
            String subValue = subset.getStringValue(xpath);
            String superValue = superset.getStringValue(xpath);
            if (!Objects.equal(subValue, superValue)) {
                return xpath;
            }
        }
        return null;
    }

    /**
     * Returns first string that has a different value in a than in b.
     * @param subset
     * @param superset
     * @return
     */
    private String differentPathValue(CLDRFile a, CLDRFile b) {
        int debugCount = 0;
        Set<String> paths = new TreeSet<>();
        CollectionUtilities.addAll(a.iterator(), paths);
        CollectionUtilities.addAll(b.iterator(), paths);
        for (String xpath : paths) {
            if (++debugCount < 100) {
                logln(debugCount + "\t" + xpath);
            }
            String aValue = a.getStringValue(xpath);
            String bValue = b.getStringValue(xpath);
            if (!Objects.equal(aValue, bValue)) {
                return xpath;
            }
        }
        return null;
    }

    public void testWrite() {
        Predicate<String> isAnnotations = x -> x.startsWith("//ldml/annotations");
        Map<String, ?> skipAnnotations = ImmutableMap.of("SKIP_PATH", isAnnotations);
        Map<String, ?> keepAnnotations = ImmutableMap.of("SKIP_PATH", isAnnotations.negate());

        CLDRFile enMain = testInfo.getCldrFactory().make("en", false);

        Factory factoryAnnotations = SimpleFactory.make(CLDRPaths.ANNOTATIONS_DIRECTORY, ".*");
        CLDRFile enAnnotations = factoryAnnotations.make("en", false);

        File[] paths = { new File(CLDRPaths.MAIN_DIRECTORY), new File(CLDRPaths.ANNOTATIONS_DIRECTORY) };
        Factory factoryDouble = SimpleFactory.make(paths, ".*");
        CLDRFile enDouble = factoryDouble.make("en", false);

        String temp = cldrFileToString(enDouble, skipAnnotations);
        if (DEBUG) {
            System.out.println("Without Annotations\t");
            System.out.println(temp);
        }
        CLDRFile enDoubleWithoutAnnotations = cldrFileFromString(temp);
        assertEquals("enMain == enDoubleWithoutAnnotations", null, differentPathValue(enMain, enDoubleWithoutAnnotations));

        temp = cldrFileToString(enDouble, keepAnnotations);
        if (DEBUG) {
            System.out.println("With Annotations\t");
            System.out.println(temp);
        }
        CLDRFile enDoubleWithAnnotations = cldrFileFromString(temp);
        assertEquals("enAnnotations == enDoubleWithAnnotations", null, differentPathValue(enAnnotations, enDoubleWithAnnotations));
    }

    private CLDRFile cldrFileFromString(String string) {
        byte[] b = string.getBytes(StandardCharsets.UTF_8);
        InputStream fis = new ByteArrayInputStream(b);
        CLDRFile filteredCldrFile = new CLDRFile(new SimpleXMLSource("enx"));
        filteredCldrFile.loadFromInputStream("enx", "enx", fis, DraftStatus.unconfirmed);
        return filteredCldrFile;
    }

    private String cldrFileToString(CLDRFile sourceCldrFile, Map<String, ?> options) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter pw = new PrintWriter(stringWriter);
        sourceCldrFile.write(pw, options);
        pw.flush();
        return stringWriter.toString();
    }
}
