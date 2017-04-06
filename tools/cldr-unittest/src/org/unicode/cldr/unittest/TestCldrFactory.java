package org.unicode.cldr.unittest;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.google.common.base.Objects;

public class TestCldrFactory extends TestFmwkPlus {
    private static final boolean DISABLE_TIL_WORKS = false;

    static CLDRConfig testInfo = CLDRConfig.getInstance();
    static SupplementalDataInfo sdi = testInfo.getSupplementalDataInfo();

    public static void main(String[] args) {
        new TestCldrFactory().run(args);
    }

    public void testDirectories() {
        File[] paths = { new File(CLDRPaths.MAIN_DIRECTORY), 
            new File(CLDRPaths.ANNOTATIONS_DIRECTORY), 
            new File(CLDRPaths.SUPPLEMENTAL_DIRECTORY) };
        List<File> enExpected = Arrays.asList(new File(CLDRPaths.MAIN_DIRECTORY), new File(CLDRPaths.ANNOTATIONS_DIRECTORY));
        Factory factory = SimpleFactory.make(paths, ".*");

        File[] dirs = factory.getSourceDirectories();
        assertEquals("", Arrays.asList(paths), Arrays.asList(dirs));

        List<File> enDirs = factory.getSourceDirectoriesForLocale("en");
        assertEquals("", enExpected, enDirs);

        // Make sure old method works
        File enDir = factory.getSourceDirectoryForLocale("en");
        assertEquals("", new File(CLDRPaths.MAIN_DIRECTORY), enDir);
    }

    public void testMerge() {
        boolean resolved = false;
        CLDRFile en = testInfo.getCldrFactory().make("en", resolved);
        assertEquals("no annotations", Status.noAnnotations, checkAnnotations(en));

        Factory factoryAnnotations = SimpleFactory.make(CLDRPaths.ANNOTATIONS_DIRECTORY, ".*");
        CLDRFile enAnnotations = factoryAnnotations.make("en", resolved);
        assertEquals("annotations only", Status.onlyAnnotations, checkAnnotations(enAnnotations));

        File[] paths = { new File(CLDRPaths.MAIN_DIRECTORY), new File(CLDRPaths.ANNOTATIONS_DIRECTORY)};
        Factory factoryDouble = SimpleFactory.make(paths, ".*");

        CLDRFile enDouble = factoryDouble.make("en", resolved);
        assertEquals("annotations only", Status.mixed, checkAnnotations(enDouble));

        assertEquals("no annotations", Status.noAnnotations, checkAnnotations(en));
        assertEquals("annotations only", Status.onlyAnnotations, checkAnnotations(enAnnotations));
        assertEquals("annotations only", Status.mixed, checkAnnotations(enDouble));

        assertEquals("en subset of enDouble", Collections.EMPTY_SET, 
            getUncontainedPaths(en, enDouble));
        assertEquals("enAnnotations subset of enDouble", Collections.EMPTY_SET, 
            getUncontainedPaths(enAnnotations, enDouble));
    }

    enum Status {none, onlyAnnotations, noAnnotations, mixed}

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

    private Set<String> getUncontainedPaths(CLDRFile subset, CLDRFile superset) {
        Set<String> results = new TreeSet<>();
        int debugCount = 0;
        for (String xpath : subset) {
            if (++debugCount < 100) {
                logln(debugCount + "\t" + xpath);
            }
            String subValue = subset.getStringValue(xpath);
            String superValue = superset.getStringValue(xpath);
            if (!Objects.equal(subValue, superValue)) {
                //System.out.println(xpath + "; " + value + "; " + value2);
                results.add(xpath);
                break;
            }
        }
        return results;
    }
}
