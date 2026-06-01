/*
 ******************************************************************************
 * Copyright (C) 2004-2012, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */

package org.unicode.cldr.web;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.unicode.cldr.util.*;

public class OutputFileManager {
    private static final Logger logger = SurveyLog.forClass(OutputFileManager.class);

    private static final String XML_SUFFIX = ".xml";
    private final SurveyMain sm;

    public OutputFileManager(SurveyMain surveyMain) {
        this.sm = surveyMain;
    }

    private enum Kind {
        vxml, // Vetted XML. This is the 'final' output from the SurveyTool.
        xml, // Input XML. This is the on-disk data as read by the SurveyTool.
        rxml, // Fully resolved, vetted, XML. This includes all parent data. Huge and expensive.
        pxml // Proposed XML. This data contains all possible user proposals and can be used to
        // reconstruct the voting situation.
    }

    private static final String XML_PREFIX = "/xml/main";
    private static final String ZXML_PREFIX = "/zxml/main";
    private static final String ZVXML_PREFIX = "/zvxml/main";
    private static final String VXML_PREFIX = "/vxml/main";
    private static final String PXML_PREFIX = "/pxml/main";
    private static final String TXML_PREFIX = "/txml/main";
    private static final String RXML_PREFIX = "/rxml/main";

    private static final FileFilter xmlFileFilter =
            file -> {
                String s = file.getName().toLowerCase();
                return s.endsWith(XML_SUFFIX) && !"en.xml".equals(s) && !"root.xml".equals(s);
            };

    /**
     * Names of directories
     *
     * <p>CLDRConfig.COMMON_DIR, etc., are private; TODO what's a better way to get them here
     * without hard-coding strings? Make this class public and move it elsewhere?
     */
    private static class DirNames {
        /*
         * "just" in these names means, for example, that justMain is really just
         * the string "main", not some path that ends in ".../main".
         */
        static final String justCommon = "common"; /* CLDRConfig.COMMON_DIR */
        static final String justSeed = "seed"; /* CLDRConfig.SEED_DIR */
        static final String justMain = "main"; /* CLDRConfig.MAIN_DIR */
        static final String justAnnotations = "annotations"; /* CLDRConfig.ANNOTATIONS_DIR */
        static final String[] commonAndSeed = {justCommon, justSeed};
        static final String[] mainAndAnnotations = {justMain, justAnnotations};
    }

    /**
     * Generate VXML
     *
     * @param vxmlGenerator the VxmlGenerator
     * @param results the VxmlQueue.Results
     */
    public static void generateVxml(VxmlGenerator vxmlGenerator, VxmlQueue.Results results) {
        try {
            /*
             * Sync on OutputFileManager.class here prevents re-entrance if invoked repeatedly before completion.
             */
            synchronized (OutputFileManager.class) {
                SurveyMain sm = CookieSession.sm;
                OutputFileManager ofm = sm.getOutputFileManager();
                results.directory = createNewManualVetdataDir(sm.getVetdir());
                if (results.directory == null) {
                    results.generationMessage = "Directory creation for vetting data failed.";
                    return;
                }
                if (!ofm.outputAllFiles(vxmlGenerator, results)) {
                    results.generationMessage = "File output failed.";
                    return;
                }
                if (!ofm.copyDtd(results.directory)) {
                    results.generationMessage = "Copying DTD failed.";
                    return;
                }
                File vxmlDir = new File(results.directory + "/" + Kind.vxml.name());
                ofm.removeEmptyFiles(vxmlDir);
                ofm.verifyAllFiles(results, vxmlDir);
            }
            logger.log(Level.WARNING, "generateVxml finished");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "generateVxml: " + e.getMessage(), e);
        }
    }

    /**
     * Create a new directory like ".../vetdata-2019-05-28T12-34-56-789Z"
     *
     * @param vetdataDir the File that would have been the "automatic vetdata" directory when that
     *     existed
     * @return the File for the newly created directory, or null for failure
     */
    private static File createNewManualVetdataDir(File vetdataDir) {
        /*
         * Include in the directory name a timestamp like 2019-05-28T12-34-56-789Z,
         * which is almost standard like 2019-05-28T12:34:56.789Z,
         * but using hyphens to replace all colons and periods.
         */
        String timestamp = Instant.now().toString();
        timestamp = timestamp.replace(':', '-');
        timestamp = timestamp.replace('.', '-');
        String manualVetdataDirName =
                vetdataDir.getParent() + "/" + vetdataDir.getName() + "-" + timestamp;
        File manualVetdataDir = new File(manualVetdataDirName);
        if (!manualVetdataDir.mkdirs()) {
            return null;
        }
        return manualVetdataDir;
    }

    /**
     * Copy the DTD file from trunk into subfolders of the given vetdata folder ("auto" or "manual")
     *
     * @param vetdataDir the File for the vetdata directory
     * @return true for success, or false for failure
     *     <p>The dtd is required for removeEmptyFiles when it calls XMLFileReader.loadPathValues.
     *     The xml files all have something like: <!DOCTYPE ldml SYSTEM
     *     "../../common/dtd/ldml.dtd">, which for generated vxml and pxml means we need ldml.dtd in
     *     locations like:
     *     <p>vetdata/vxml/common/dtd/ldml.dtd vetdata/pxml/common/dtd/ldml.dtd
     *     vetdata-2019-05-29T02-08-33-389Z/vxml/common/dtd/ldml.dtd
     *     vetdata-2019-05-29T02-08-33-389Z/pxml/common/dtd/ldml.dtd
     *     <p>They should be copies of "trunk" like cldr/common/dtd/ldml.dtd
     */
    private boolean copyDtd(File vetdataDir) {
        String dtdDirName = "dtd";
        String dtdFileName = "ldml.dtd";
        File baseDir = CLDRConfig.getInstance().getCldrBaseDirectory();
        String dtdSourceName =
                baseDir + "/" + DirNames.justCommon + "/" + dtdDirName + "/" + dtdFileName;
        File dtdSource = new File(dtdSourceName);
        if (!dtdSource.exists()) {
            return false;
        }
        String[] vp = {Kind.vxml.toString(), Kind.pxml.toString()};
        for (String s : vp) {
            File destDir =
                    new File(vetdataDir + "/" + s + "/" + DirNames.justCommon + "/" + dtdDirName);
            if (!destDir.exists() && !destDir.mkdirs()) {
                return false;
            }
            try {
                File dtdFile = new File(destDir + "/" + dtdFileName);
                if (!dtdFile.exists()) {
                    Files.copy(dtdSource.toPath(), dtdFile.toPath());
                }
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Output all files (VXML, etc.)
     *
     * @param vxmlGenerator the VxmlGenerator
     * @param results the VxmlQueue.Results
     * @return true for success, false for failure
     */
    private boolean outputAllFiles(VxmlGenerator vxmlGenerator, VxmlQueue.Results results) {
        try {
            for (CLDRLocale loc : vxmlGenerator.getLocales()) {
                for (OutputFileManager.Kind kind : OutputFileManager.Kind.values()) {
                    if (kind == OutputFileManager.Kind.vxml
                            || kind == OutputFileManager.Kind.pxml) {
                        logger.log(Level.WARNING, "Writing " + loc.getDisplayName() + ":" + kind);
                        writeManualOutputFile(results.directory, loc, kind);
                    }
                }
                vxmlGenerator.update(loc);
            }
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "outputAllFiles: " + e.getMessage(), e);
            return false;
        }
    }

    public static Set<CLDRLocale> createVxmlLocaleSet() {
        Set<CLDRLocale> set = new TreeSet<>(SurveyMain.getLocalesSet());
        // skip "en" and "root", since they should never be changed by the Survey Tool
        set.remove(CLDRLocale.getInstance("en"));
        set.remove(CLDRLocale.getInstance(LocaleNames.ROOT));
        // Remove "mul", "mul_ZZ", etc.; all "special" locales except algorithmic.
        set.removeIf(
                loc -> {
                    SpecialLocales.Type t = SpecialLocales.getType(loc);
                    return t != null && t != SpecialLocales.Type.algorithmic;
                });
        return set;
    }

    /**
     * Write out the specified file(s).
     *
     * <p>If kind is vxml (for example), we may write to both common/main and common/annotations, or
     * to both seed/main and seed/annotations.
     *
     * @param loc the CLDRLocale
     * @param kind the Kind, currently Kind.vxml and Kind.pxml are supported
     */
    private void writeManualOutputFile(File vetDataDir, CLDRLocale loc, Kind kind) {
        CLDRFile cldrFile;
        if (kind == Kind.vxml) {
            cldrFile = sm.getSTFactory().make(loc.getBaseName(), false);
        } else if (kind == Kind.pxml) {
            cldrFile = sm.getSTFactory().makeProposedFile(loc);
        } else {
            throw new InternalError("Don't know MANUALLY how to make kind " + kind);
        }
        try {
            /*
             * Does the file belong in common, or in seed? Currently we answer that by
             * looking for a corresponding baseline cldr xml file (not vxml or pxml)
             * in both common and seed, and follow that example. (This is somewhat circular.)
             * If a baseline file doesn't exist in common or seed, go with common.
             */
            String outDirName = getOutDirName(vetDataDir, loc, kind);
            File outDir = new File(outDirName);
            if (!outDir.exists() && !outDir.mkdirs()) {
                throw new InternalError("Unable to create directory: " + outDirName);
            }
            String outFileName = outDirName + "/" + loc + XML_SUFFIX;
            File outFile = new File(outFileName);
            doWriteFile(cldrFile, kind, outFile);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "writeManualOutputFile: " + e.getMessage(), e);
            throw new RuntimeException("IO Exception " + e, e);
        }
    }

    private static String getOutDirName(File vetDataDir, CLDRLocale loc, Kind kind) {
        File baseDir = CLDRConfig.getInstance().getCldrBaseDirectory();
        String commonOrSeed = DirNames.justCommon;
        for (String c : DirNames.commonAndSeed) {
            String path = baseDir + "/" + c + "/" + DirNames.justMain + "/" + loc + XML_SUFFIX;
            if (new File(path).exists()) {
                commonOrSeed = c;
                break;
            }
        }
        /*
         * Only create the file in "main" here; doWriteFile will then create the file in "annotations"
         */
        return vetDataDir + "/" + kind + "/" + commonOrSeed + "/" + DirNames.justMain;
    }

    /** Remove "empty" VXML files in a set of directories */
    private void removeEmptyFiles(File vxmlDir) throws IOException {
        for (String c : DirNames.commonAndSeed) {
            /*
             * Skip main. Only do common/annotations and seed/annotations.
             */
            File dirFile = new File(vxmlDir + "/" + c + "/" + DirNames.justAnnotations);
            if (dirFile.exists()) {
                removeEmptyFilesOneDir(dirFile);
            }
        }
    }

    /**
     * Remove "empty" VXML files in the given directory
     *
     * @param dirFile the given directory
     * @throws IOException failed IO
     */
    private void removeEmptyFilesOneDir(File dirFile) throws IOException {
        Set<String> treatAsNonEmpty = new HashSet<>();
        BiMap<String, File> onlyHasIdentity = HashBiMap.create();
        int counter = 0;
        for (File f : Objects.requireNonNull(dirFile.listFiles())) {
            List<Pair<String, String>> data = new ArrayList<>();
            String canonicalPath = f.getCanonicalPath();
            if (canonicalPath.endsWith("root.xml") || !canonicalPath.endsWith(XML_SUFFIX)) {
                continue;
            }
            String name = f.getName();
            name = name.substring(0, name.length() - 4); // remove .xml
            XMLFileReader.loadPathValues(canonicalPath, data, false);
            /*
             * Treat a file as "non-empty" if it, or any of its descendants,
             * contains items other than "identity"
             */
            boolean itemHasMoreThanIdentity = false;
            for (Pair<String, String> item : data) {
                if (!item.getFirst().contains("/identity")) {
                    logger.log(Level.WARNING, ++counter + ") NOT-EMPTY: " + canonicalPath);
                    /*
                     * keep this file, and its ancestors (needed for inheritance even if only identity)
                     */
                    addNameAndParents(treatAsNonEmpty, name);
                    itemHasMoreThanIdentity = true;
                    break;
                }
            }
            if (!itemHasMoreThanIdentity) {
                onlyHasIdentity.put(name, f);
            }
        }
        counter = 0;
        for (Entry<String, File> entry : onlyHasIdentity.entrySet()) {
            String name = entry.getKey();
            if (!treatAsNonEmpty.contains(name)) {
                File file = entry.getValue();
                logger.log(Level.WARNING, ++counter + ") Deleting: " + file.getCanonicalPath());
                if (file.exists() && !file.delete()) {
                    throw new InternalError("Unable to delete file: " + file);
                }
            }
        }
    }

    /**
     * Add the given name, and the names of its ancestors (except root), to the given set
     *
     * @param treatAsNonEmpty the set to be modified
     * @param name the xml file name (without the .xml extension)
     */
    private static void addNameAndParents(Set<String> treatAsNonEmpty, String name) {
        treatAsNonEmpty.add(name);
        String parent = LocaleIDParser.getParent(name);
        if (!LocaleNames.ROOT.equals(parent)) {
            addNameAndParents(treatAsNonEmpty, parent);
        }
    }

    /**
     * Verify all VXML files
     *
     * @param results the VxmlQueue.Results
     *     <p>The following need to be verified on the server when generating vxml: • The same file
     *     must not occur in both the common/X and seed/X directories, for any X=main|annotations •
     *     If a parent locale (except for root) must occur in the same directory as the child locale
     *     • Every file in trunk (common|seed/X) must have a corresponding vxml file • Every file in
     *     vxml (common|seed/X) must have a corresponding trunk file This should fail with clear
     *     warning to the user that there is a major problem. Reference: CLDR-12016
     */
    private void verifyAllFiles(VxmlQueue.Results results, File vxmlDir) {
        /*
         * The same file must not occur in both the common/X and seed/X directories, for any X=main|annotations
         */
        verifyNoDuplicatesInCommonAndSeed(results, vxmlDir);

        /*
         * A parent locale (except for root) must occur in the same directory as the child locale
         */
        verifyParentChildSameDirectory(results, vxmlDir);

        /*
         * Every file in trunk (common|seed/X) must have a corresponding vxml file
         * Every file in vxml (common|seed/X) must have a corresponding trunk file
         */
        verifyVxmlAndBaselineFilesCorrespond(results, vxmlDir);

        if (results.verificationFailures.isEmpty()) {
            results.setVerificationStatus(VxmlGenerator.VerificationStatus.SUCCESSFUL);
        } else {
            results.setVerificationStatus(VxmlGenerator.VerificationStatus.FAILED);
        }
    }

    /**
     * Verify that the same file does not occur in both the common/X and seed/X directories, for any
     * X=main|annotations
     *
     * @param results the VxmlQueue.Results
     */
    private void verifyNoDuplicatesInCommonAndSeed(VxmlQueue.Results results, File vxmlDir) {
        for (String m : DirNames.mainAndAnnotations) {
            String commonDirName = vxmlDir + "/" + DirNames.justCommon + "/" + m;
            String seedDirName = vxmlDir + "/" + DirNames.justSeed + "/" + m;
            File dirFile = new File(commonDirName);
            if (dirFile.exists()) {
                for (File file : Objects.requireNonNull(dirFile.listFiles(xmlFileFilter))) {
                    String commonName = file.getName();
                    String commonPathName = commonDirName + "/" + commonName;
                    String seedPathName = seedDirName + "/" + commonName;
                    File fSeed = new File(seedPathName);
                    if (fSeed.exists()) {
                        results.addVerificationFailure(
                                "Found duplicates: " + commonPathName + " - " + seedPathName);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Verify that a file for parent locale does occur in the same directory as the file for the
     * child locale
     *
     * <p>Examples: if we have "aa_NA.xml" we should have "aa.xml" in the same folder if we have
     * "ff_Adlm_BF.xml" we should have "ff_Adlm.xml" in the same folder Note handling of two
     * underscores in "ff_Adlm_BF.xml"
     *
     * @param results the VxmlQueue.Results
     */
    private void verifyParentChildSameDirectory(VxmlQueue.Results results, File vxmlDir) {
        for (String c : DirNames.commonAndSeed) {
            for (String m : DirNames.mainAndAnnotations) {
                String dirName = vxmlDir + "/" + c + "/" + m;
                File dirFile = new File(dirName);
                if (!dirFile.exists()) {
                    continue;
                }
                for (File file : Objects.requireNonNull(dirFile.listFiles(xmlFileFilter))) {
                    String childName = file.getName();
                    String childPathName = dirName + "/" + childName;
                    /*
                     * Get the parent from the child. Change "aa_NA.xml" to "aa.xml";
                     * "ff_Adlm_BF.xml" to "ff_Adlm.xml"; "sr_Cyrl_BA.xml" to "sr_Cyrl.xml" (not "sr.xml")
                     */
                    String localeName = childName.replaceFirst("\\.xml$", "");
                    CLDRLocale childLoc = CLDRLocale.getInstance(localeName);
                    if (childLoc == null) {
                        results.addVerificationFailure(
                                "Locale not recognized from file name: " + childPathName);
                        return;
                    }
                    CLDRLocale parLoc = childLoc.getParent();
                    if (parLoc != null) {
                        String parentName = parLoc + XML_SUFFIX;
                        if (!childName.equals(parentName)
                                && !"en.xml".equals(parentName)
                                && !"root.xml".equals(parentName)) {
                            String parentPathName = dirName + "/" + parentName;
                            File fParent = new File(parentPathName);
                            if (!fParent.exists() && !otherParentExists(parentPathName, c)) {
                                results.addVerificationFailure(
                                        "Child without parent. "
                                                + "Child, present: "
                                                + childPathName
                                                + " - "
                                                + "Parent, absent: "
                                                + parentPathName);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Does a parent exist in the other possible location?
     *
     * <p>Given, e.g., "el_POLYTON.xml", we're looking for its "parent", e.g., "el.xml". We already
     * know it's not in the same directory as the child. Try the related directory which is obtained
     * by changing "seed" to "common". E.g.: Child: .../seed/main/el_POLYTON.xml Missing parent:
     * .../seed/main/el.xml Other parent: .../common/main/el.xml
     *
     * @param parentPathName like ".../seed/main"
     * @param commonOrSeed where we already looked: "common" (give up) or "seed" (try "common")
     * @return true if the other parent exists
     */
    private boolean otherParentExists(String parentPathName, String commonOrSeed) {
        /*
         * Allow "parent in common and child in seed" but not vice-versa
         */
        if (commonOrSeed.equals(DirNames.justCommon)) {
            return false; // give up
        }
        /*
         * Replace "/seed/" with "/common/" in the parent path
         */
        parentPathName =
                parentPathName.replace(
                        "/" + DirNames.justSeed + "/", "/" + DirNames.justCommon + "/");
        return new File(parentPathName).exists();
    }

    /**
     * Verify that every file in baseline-cldr (common|seed/X) has a corresponding vxml file
     * (required) AND every file in vxml (common|seed/X) has a corresponding baseline-cldr file
     * (optional). The first condition is required for verification to succeed; the second is not
     * required, and only results in a notification (new files in vxml are to be expected from time
     * to time).
     *
     * @param results the VxmlQueue.Results
     */
    private void verifyVxmlAndBaselineFilesCorrespond(VxmlQueue.Results results, File vxmlDir) {
        String bxmlDir = CLDRConfig.getInstance().getCldrBaseDirectory().toString();
        ArrayList<String> vxmlFiles = new ArrayList<>();
        ArrayList<String> bxmlFiles = new ArrayList<>(); /* bxml = baseline cldr xml */
        for (String c : DirNames.commonAndSeed) {
            for (String m : DirNames.mainAndAnnotations) {
                File vxmlDirFile = new File(vxmlDir + "/" + c + "/" + m);
                File bxmlDirFile = new File(bxmlDir + "/" + c + "/" + m);
                if (vxmlDirFile.exists()) {
                    for (File file : Objects.requireNonNull(vxmlDirFile.listFiles(xmlFileFilter))) {
                        vxmlFiles.add(c + "/" + m + "/" + file.getName());
                    }
                }
                if (bxmlDirFile.exists()) {
                    for (File file : Objects.requireNonNull(bxmlDirFile.listFiles(xmlFileFilter))) {
                        bxmlFiles.add(c + "/" + m + "/" + file.getName());
                    }
                }
            }
        }
        Set<String> diff = symmetricDifference(vxmlFiles, bxmlFiles);
        if (!diff.isEmpty()) {
            boolean someOnlyInVxml = false, someOnlyInBxml = false;
            for (String name : diff) {
                if (vxmlFiles.contains(name)) {
                    someOnlyInVxml = true;
                } else {
                    someOnlyInBxml = true;
                }
                if (someOnlyInVxml && someOnlyInBxml) {
                    break;
                }
            }
            if (someOnlyInVxml) {
                /*
                 * Notification only, not a failure
                 */
                StringBuilder message =
                        new StringBuilder("File(s) present in VXML but not in baseline:");
                for (String name : diff) {
                    if (vxmlFiles.contains(name)) {
                        message.append(" ").append(name);
                    }
                }
                results.addVerificationWarning(message.toString());
            }
            if (someOnlyInBxml) {
                StringBuilder message =
                        new StringBuilder("File(s) present in baseline but not in VXML:");
                for (String name : diff) {
                    if (bxmlFiles.contains(name)) {
                        message.append(" ").append(name);
                    }
                }
                results.addVerificationFailure(message.toString());
            }
        }
    }

    /**
     * Return the set of all strings that are in one list but not the other
     *
     * @param list1 the first list
     * @param list2 the second list
     * @return the Set
     */
    private static Set<String> symmetricDifference(
            final ArrayList<String> list1, final ArrayList<String> list2) {
        Set<String> diff = new HashSet<>(list1);
        diff.addAll(list2);
        Set<String> tmp = new HashSet<>(list1);
        tmp.retainAll(list2);
        diff.removeAll(tmp);
        return diff;
    }

    private static final Predicate<String> isAnnotations = x -> x.startsWith("//ldml/annotations");

    private final Map<String, Object> OPTS_SKIP_ANNOTATIONS =
            ImmutableMap.of("SKIP_PATH", isAnnotations);
    private final Map<String, Object> OPTS_KEEP_ANNOTATIONS =
            ImmutableMap.of("SKIP_PATH", isAnnotations.negate());

    /**
     * Write one or more files. For vxml (at least), write one in "main" and one in "annotations".
     *
     * @param file the CLDRFile for reading
     * @param outFile the File for "main"; another file will be created in "annotations"
     * @throws UnsupportedEncodingException for bad encoding, unlikely for UTF-8
     * @throws FileNotFoundException for file error
     */
    private void doWriteFile(CLDRFile file, Kind kind, File outFile)
            throws UnsupportedEncodingException, FileNotFoundException {
        try (PrintWriter u8out =
                new PrintWriter(
                        new OutputStreamWriter(
                                new FileOutputStream(outFile), StandardCharsets.UTF_8))) {
            if (kind == Kind.vxml || kind == Kind.rxml) {
                file.write(u8out, OPTS_SKIP_ANNOTATIONS);

                // output annotations, too
                File aFile = makeAnnotationFile(outFile); // same name, different subdir
                try (PrintWriter u8outa =
                        new PrintWriter(
                                new OutputStreamWriter(
                                        new FileOutputStream(aFile), StandardCharsets.UTF_8))) {
                    if (!file.write(u8outa, OPTS_KEEP_ANNOTATIONS)) {
                        if (aFile.exists() && !aFile.delete()) {
                            throw new InternalError("Unable to delete aFile: " + aFile);
                        }
                    }
                }
            } else {
                file.write(u8out);
            }
        }
    }

    private static File makeAnnotationFile(File outFile) {
        File parentDir = outFile.getParentFile().getParentFile();
        File annotationsDir = new File(parentDir, DirNames.justAnnotations);
        if (!annotationsDir.exists() && !annotationsDir.mkdirs()) {
            throw new InternalError("Unable to create directory: " + annotationsDir);
        }
        return new File(annotationsDir, outFile.getName());
    }

    /**
     * For a request like ".../cldr-apps/survey/vxml/main/aa.xml", respond with the xml
     *
     * @param request the HttpServletRequest
     * @param response the HttpServletResponse
     * @return true if request is for a kind of xml we can provide, else false.
     * @throws IOException for WebContext failure
     */
    public boolean doRawXml(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        /*
         * request.getPathInfo returns what follows "survey" in the url.
         * If the url is ".../cldr-apps/survey/vxml/main/aa.xml", it returns "vxml/main/aa.xml".
         */
        String s = request.getPathInfo();
        if ((s == null)
                || !(s.startsWith(XML_PREFIX)
                        || s.startsWith(ZXML_PREFIX)
                        || s.startsWith(ZVXML_PREFIX)
                        || s.startsWith(VXML_PREFIX)
                        || s.startsWith(PXML_PREFIX)
                        || s.startsWith(RXML_PREFIX)
                        || s.startsWith(TXML_PREFIX))) {
            return false;
        }
        boolean finalData = false;
        String kind = null;

        if (s.startsWith(VXML_PREFIX)) {
            finalData = true;
            if (s.equals(VXML_PREFIX)) {
                WebContext ctx = new WebContext(request, response);
                response.sendRedirect(ctx.schemeHostPort() + ctx.base() + VXML_PREFIX + "/");
                return true;
            }
            kind = "vxml";
            s = s.substring(VXML_PREFIX.length() + 1); // "foo.xml"
        } else if (s.startsWith(PXML_PREFIX)) {
            finalData = true;
            if (s.equals(PXML_PREFIX)) {
                return true;
            }
            kind = "pxml";
            s = s.substring(PXML_PREFIX.length() + 1); // "foo.xml"
        } else if (s.startsWith(RXML_PREFIX)) {
            finalData = true;
            if (s.equals(RXML_PREFIX)) {
                WebContext ctx = new WebContext(request, response);
                response.sendRedirect(ctx.schemeHostPort() + ctx.base() + RXML_PREFIX + "/");
                return true;
            }
            kind = "rxml"; // cached
            s = s.substring(RXML_PREFIX.length() + 1); // "foo.xml"
        } else if (s.startsWith(TXML_PREFIX)) {
            finalData = true;
            if (s.equals(TXML_PREFIX)) {
                WebContext ctx = new WebContext(request, response);
                response.sendRedirect(ctx.schemeHostPort() + ctx.base() + TXML_PREFIX + "/");
                return true;
            }
            s = s.substring(TXML_PREFIX.length() + 1); // "foo.xml"
        } else if (s.startsWith(ZXML_PREFIX)) {
            s = s.substring(ZXML_PREFIX.length() + 1); // "foo.xml"
        } else if (s.startsWith(ZVXML_PREFIX)) {
            finalData = true;
            s = s.substring(ZVXML_PREFIX.length() + 1); // "foo.xml"
        } else {
            if (s.equals(XML_PREFIX)) {
                WebContext ctx = new WebContext(request, response);
                response.sendRedirect(ctx.schemeHostPort() + ctx.base() + XML_PREFIX + "/");
                return true;
            }
            kind = "xml";
            s = s.substring(XML_PREFIX.length() + 1); // "foo.xml"
        }

        if (s.isEmpty()) {
            WebContext ctx = new WebContext(request, response);
            response.setContentType("text/html; charset=utf-8");
            if (finalData) {
                ctx.println("<title>CLDR Data | All Locales - Vetted Data</title>");
            } else {
                ctx.println("<title>CLDR Data | All Locales</title>");
            }
            ctx.println("<a href='" + ctx.base() + "'>Return to SurveyTool</a><p>");
            ctx.println("<h4>Locales</h4>");
            ctx.println("<ul>");
            CLDRLocale[] locales = SurveyMain.getLocales();
            for (CLDRLocale locale : locales) {
                String localeName = locale.getBaseName();
                String fileName = localeName + XML_SUFFIX;
                ctx.println(
                        "<li><a href='"
                                + fileName
                                + "'>"
                                + fileName
                                + "</a> "
                                + locale.getDisplayName(ctx.displayLocale)
                                + "</li>");
            }
            ctx.println("</ul>");
            ctx.println("<hr>");
            ctx.println("<a href='" + ctx.base() + "'>Return to SurveyTool</a><p>");
            ctx.close();
        } else if (!s.endsWith(XML_SUFFIX)) {
            WebContext ctx = new WebContext(request, response);
            response.sendRedirect(ctx.schemeHostPort() + ctx.base() + XML_PREFIX + "/");
        } else {
            boolean found = false;
            CLDRLocale[] locales = SurveyMain.getLocales();
            CLDRLocale foundLocale = null;
            int nrInFiles = locales.length;
            for (int i = 0; (!found) && (i < nrInFiles); i++) {
                CLDRLocale locale = locales[i];
                String localeName = locale.getBaseName();
                String fileName = localeName + XML_SUFFIX;
                if (s.equals(fileName)) {
                    found = true;
                    foundLocale = locale;
                }
            }
            if (!found) {
                throw new InternalError("No such locale: " + s);
            } else {
                String doKvp = request.getParameter("kvp");
                boolean isKvp = (doKvp != null && !doKvp.isEmpty());

                if (isKvp) {
                    response.setContentType("text/plain; charset=utf-8");
                } else {
                    response.setContentType("application/xml; charset=utf-8");
                }

                if (kind.equals("vxml")) {
                    sm.getSTFactory()
                            .make(foundLocale.getBaseName(), false)
                            .write(response.getWriter());
                    return true;
                } else if (kind.equals("pxml")) {
                    sm.getSTFactory().makeProposedFile(foundLocale).write(response.getWriter());
                    return true;
                }
            }
        }
        return true;
    }
}
