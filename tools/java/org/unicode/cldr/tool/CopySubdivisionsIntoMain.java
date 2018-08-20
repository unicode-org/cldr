package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.tool.Option.Options;
import org.unicode.cldr.tool.Option.Params;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.SimpleXMLSource;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.util.ICUUncheckedIOException;

public class CopySubdivisionsIntoMain {

    // TODO add seed

    private static final String MAIN_TARGET_DIR = CLDRPaths.MAIN_DIRECTORY; // for testing, CLDRPaths.GEN_DIRECTORY + "sub-main/";

    private static final String SUBDIVISION_TARGET_DIR = CLDRPaths.SUBDIVISIONS_DIRECTORY; // CLDRPaths.SUBDIVISIONS_DIRECTORY;
    // for testing, CLDRPaths.GEN_DIRECTORY + "test_subdivisions/";

    enum MyOptions {
        beforeSubmission(new Params().setHelp("Before submission: copy from /subdivisions/ to /main/")),
        afterSubmission(new Params().setHelp("After submission: copy from /main/ to /subdivisions/")),
        verbose(new Params().setHelp("verbose debugging messages")),
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

        private static Set<String> parse(String[] args, boolean showArguments) {
            return myOptions.parse(MyOptions.values()[0], args, true);
        }
    }

    // TODO make target according to language/region in future

    static final Set<String> target = ImmutableSet.of("gbeng", "gbsct", "gbwls");
    static final Factory mainFactory = CLDRConfig.getInstance().getCldrFactory();
    static final Factory subdivisionFactory = CLDRConfig.getInstance().getSubdivisionFactory();
    static boolean verbose;

    public static void main(String[] args) {
        MyOptions.parse(args, true);
        verbose = MyOptions.verbose.option.doesOccur();
        boolean before = MyOptions.beforeSubmission.option.doesOccur();
        boolean after = MyOptions.afterSubmission.option.doesOccur();
        if (before == after) {
            throw new IllegalArgumentException("Must do exactly one of " + MyOptions.beforeSubmission + " and " + MyOptions.afterSubmission);
        }
        if (before) {
            doBefore();
        } else {
            doAfter();
        }

    }

    private static void doAfter() {
        PathStarrer pathStarrer = new PathStarrer();

        for (String localeId : mainFactory.getAvailable()) {
            
            boolean isRoot = "root".equals(localeId);
            CLDRFile mainFile = mainFactory.make(localeId, false);
            CLDRFile mainFileOut = null;
            CLDRFile subdivisionFile = null;
            CLDRFile subdivisionFileOut = null;

            for (Iterator<String> subdivisionIterator = mainFile.iterator(SubdivisionNames.SUBDIVISION_PATH_PREFIX); subdivisionIterator.hasNext(); ) {
                String path = subdivisionIterator.next();
                String value = mainFile.getStringValue(path);
                
                // remove from main file
                
                if (mainFileOut == null) {
                    mainFileOut = mainFile.cloneAsThawed(); // lazy create
                }
                mainFileOut.remove(path);
                
                // now copy to subdivision file
                
                if (isRoot) {
                    continue;
                }

                if (subdivisionFile == null) {
                    try {
                        subdivisionFile = subdivisionFactory.make(localeId, false); // lazy open
                    } catch (SimpleFactory.NoSourceDirectoryException e) {
                        System.out.println("No existing /subdivisions/ file, so creating one: " + localeId);
                        subdivisionFile = new CLDRFile(new SimpleXMLSource(localeId));
                    }
                }
                
                String oldValue = subdivisionFile.getStringValue(path);
                if (!Objects.equal(oldValue, value)) {
                    pathStarrer.set(path);
                    String firstAttributeValue = pathStarrer.getAttributes().iterator().next();
                    if (Objects.equal(firstAttributeValue, value)) {
                        System.out.println(localeId + " — ERROR: Value == ID! for " + value + ". See https://unicode.org/cldr/trac/ticket/11358");
                    } else {
                        if (subdivisionFileOut == null) {
                            subdivisionFileOut = subdivisionFile.cloneAsThawed();
                        }
                        subdivisionFileOut.add(path, value);
                    }
                }
            }
            if (mainFileOut != null) {
                writeFile(MAIN_TARGET_DIR, localeId, mainFileOut);
                if (verbose) {
                    System.out.println("Removing /main/ subdivisions: " + localeId);
                }
            }
            if (subdivisionFileOut != null) {
                writeFile(SUBDIVISION_TARGET_DIR, localeId, subdivisionFile);
                System.out.println("Adding changed/new /subdivision/ items: " + localeId);
            } else if (verbose) {
                System.out.println("No change in: " + localeId);
            }
        }
    }

    private static void doBefore() {
        for (String locale : SubdivisionNames.getAvailableLocales()) {
            SubdivisionNames sdn = new SubdivisionNames(locale);
            Set<String> keySet = sdn.keySet();
            if (Collections.disjoint(target, keySet)) {
                continue;
            }
            CLDRFile cldrFile;
            try {
                cldrFile = mainFactory.make(locale, false);
            } catch (Exception e) {
                System.out.println("Not in main, skipping for now: " + locale);
                continue;
            }
            boolean added = false;
            for (String key : target) {
                String path = SubdivisionNames.getPathFromCode(key);
                // skip if no new value
                String name = sdn.get(key);
                if (name == null) {
                    continue;
                }
                // don't copy if present already
                String oldValue = cldrFile.getStringValue(path);
                if (oldValue != null) {
                    continue;
                }
                if (!added) {
                    cldrFile = cldrFile.cloneAsThawed();
                    added = true;
                }
                cldrFile.add(path, name);
                System.out.println("Adding " + locale + ": " + path + "\t=«" + name + "»");
            }
            if (added) {
                writeFile(MAIN_TARGET_DIR, locale, cldrFile);
            }
        }
    }

    private static void writeFile(String directory, String locale, CLDRFile cldrFile) {
        try (PrintWriter pw = FileUtilities.openUTF8Writer(directory, locale + ".xml")) {
            cldrFile.write(pw);
        } catch (IOException e) {
            throw new ICUUncheckedIOException(e);
        }
    }
}
