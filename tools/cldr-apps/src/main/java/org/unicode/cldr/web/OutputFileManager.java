/*
 ******************************************************************************
 * Copyright (C) 2004-2012, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */

package org.unicode.cldr.web;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusClient;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.web.CLDRProgressIndicator.CLDRProgressTask;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.ibm.icu.dev.util.ElapsedTimer;

public class OutputFileManager {

    private static boolean DEBUG = false;
    private static final String XML_SUFFIX = ".xml";
    private SurveyMain sm;

    public OutputFileManager(SurveyMain surveyMain) {
        this.sm = surveyMain;

        File vxmlDir = null;
        SVNInfo i = null;
        try {
            vxmlDir = sm.makeDataDir(Kind.vxml.name());
            i = svnInfo(vxmlDir);
            if (i.getKind() != SVNNodeKind.DIR) {
                throw new IllegalArgumentException("Unknown node kind :  " + i.getKind());
            }
        } catch (Throwable t) {
            tryCommit = false;
            String whyNot = tryCommitWhyNot = "SVN disabled - because: " + t.toString() + " on "
                + ((vxmlDir == null) ? "(null vxml directory)" : vxmlDir.getAbsolutePath());

            System.err.println(whyNot);
        }
        if (tryCommit) {
            System.err.println("SVN commits active in " + vxmlDir.getAbsolutePath() + " - r"
                + i.getCommittedRevision().getNumber() + " " + i.getCommittedDate());
        }
    }

    public enum Kind {
        vxml("Vetted XML. This is the 'final' output from the SurveyTool."),

        xml("Input XML. This is the on-disk data as read by the SurveyTool."),

        rxml("Fully resolved, vetted, XML. This includes all parent data. Huge and expensive."),

        pxml("Proposed XML. This data contains all possible user proposals and can be used to reconstruct the voting situation.");

        Kind(String desc) {
            this.desc = desc;
        }

        public String getDesc() {
            return desc;
        }

        private final String desc;
    };

    /**
     * @param kind
     * @return true if this kind is cacheable
     */
    private static boolean isCacheableKind(String kind) {
        try {
            Kind.valueOf(kind);
            return true;
        } catch (IllegalArgumentException iae) {
            return false;
        }
    }

    public static final String XML_PREFIX = "/xml/main";
    public static final String ZXML_PREFIX = "/zxml/main";
    public static final String ZVXML_PREFIX = "/zvxml/main";
    public static final String VXML_PREFIX = "/vxml/main";
    public static final String PXML_PREFIX = "/pxml/main";
    public static final String TXML_PREFIX = "/txml/main";
    public static final String RXML_PREFIX = "/rxml/main";
    public static final String FEED_PREFIX = "/feed";

    public boolean tryCommit = true;
    public String tryCommitWhyNot = null;

    /**
     * Names of directories
     *
     * CLDRConfig.COMMON_DIR, etc., are private; TODO what's a better way to
     * get them here without hard-coding strings? Make this class public and
     * move it elsewhere?
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
        static final String[] commonAndSeed = { justCommon, justSeed };
        static final String[] mainAndAnnotations = { justMain, justAnnotations };
    }

    /**
     * Output all files (VXML, etc.) and verify their consistency
     *
     * @param request the HttpServletRequest, used for "vap"
     * @param out the Writer, to receive HTML output
     *
     * Invoked by pasting a url like this into a browser:
     *     http://localhost:8080/cldr-apps/admin-OutputAllFiles.jsp?vap=...
     *
     * This function was started using code moved here from admin-OutputAllFiles.jsp.
     * Reference: CLDR-12016 and CLDR-11877
     *
     * Compare output-status.jsp which maybe should be linked from here -- user may want to
     * view it at the same time as this. However, that gets complicated if we create a new
     * vetdata folder.
     *
     * TODO: link to gear menu and use JavaScript for a front-end.
     */
    public static void outputAndVerifyAllFiles(HttpServletRequest request, Writer out) {
        String vap = request.getParameter("vap");
        try {
            if (vap == null || !vap.equals(SurveyMain.vap)) {
                out.write("Not authorized.");
                return;
            }
            boolean outputFiles = "true".equals(request.getParameter("output"));
            boolean makeSeparateDir = "true".equals(request.getParameter("separate"));
            boolean removeEmpty = "true".equals(request.getParameter("remove"));
            boolean verifyConsistent = "true".equals(request.getParameter("verify"));
            if (!(outputFiles || makeSeparateDir || removeEmpty || verifyConsistent)) {
                out.write("<p>Usage: specify at least one of these parameters (all false by default):</p>\n");
                out.write("output=true/false<br>\n");
                out.write("separate=true/false<br>\n");
                out.write("remove=true/false<br>\n");
                out.write("verify=true/false<br>\n");
                return;
            }
            /*
             * Sync on OutputFileManager.class here prevents re-entrance if invoked repeatedly before completion.
             * Performance problem if run while Survey Tool has multiple users/requests?
             * Completion of http request/response may take over ten minutes! TODO: use ajax.
             */
            synchronized (OutputFileManager.class) {
                SurveyMain sm = CookieSession.sm;

                OutputFileManager ofm = sm.getOutputFileManager();
                // top line is like "Have OFM=org.unicode.cldr.web.OutputFileManager@4d150a19" -- is this still needed?
                out.write("Have OFM=" + ofm.toString() + "\n");

                File vetdataDir = sm.getVetdir();
                if (makeSeparateDir) {
                    vetdataDir = createNewManualVetdataDir(vetdataDir);
                    if (vetdataDir == null) {
                        out.write("Directory creation for vetting data failed.");
                        return;
                    }
                    out.write("<p>Created new directory: " + vetdataDir.toString() + "</p>");
                } else {
                    out.write("<p>Using auto directory: " + vetdataDir.toString() + "</p>");
                }

                if (outputFiles && !ofm.outputAllFiles(out, vetdataDir, makeSeparateDir)) {
                    out.write("File output failed.");
                    return;
                }
                if ((makeSeparateDir || removeEmpty) && !ofm.copyDtd(vetdataDir)) {
                    out.write("Copying DTD failed.");
                    return;
                }
                File vxmlDir = null;
                if (removeEmpty || verifyConsistent) {
                    vxmlDir = new File(vetdataDir.toString() + "/" + Kind.vxml.name());
                }
                if (removeEmpty) {
                    ofm.removeEmptyFiles(out, vxmlDir);
                }
                if (verifyConsistent) {
                    ofm.verifyAllFiles(out, vxmlDir);
                }
            }
        } catch (Exception e) {
            System.err.println("Exception in outputAndVerifyAllFiles: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Create a new directory like ".../vetdata-2019-05-28T12-34-56-789Z", as a sibling
     * of the "automatic vetdata" directory
     *
     * @param autoVetdataDir the File for the "automatic vetdata" directory
     * @return the File for the newly created directory, or null for failure
     */
    private static File createNewManualVetdataDir(File autoVetdataDir) {
        /*
         * Include in the directory name a timestamp like 2019-05-28T12-34-56-789Z,
         * which is almost standard like 2019-05-28T12:34:56.789Z,
         * but using hyphens to replace all colons and periods.
         */
        String timestamp = Instant.now().toString();
        timestamp = timestamp.replace(':', '-');
        timestamp = timestamp.replace('.', '-');
        String manualVetdataDirName = autoVetdataDir.getParent() + "/" + autoVetdataDir.getName() + "-" + timestamp;
        File manualVetdataDir = new File(manualVetdataDirName);
        if (manualVetdataDir.mkdirs() == false) {
            return null;
        }
        return manualVetdataDir;
    }

    /**
     * Copy the DTD file from trunk into subfolders of the given vetdata folder ("auto" or "manual")
     *
     * @param vetdataDir the File for the vetdata directory
     * @param common the name of the "common" folder
     * @return true for success, or false for failure
     *
     * The dtd is required for removeEmptyFiles when it calls XMLFileReader.loadPathValues.
     * The xml files all have something like: <!DOCTYPE ldml SYSTEM "../../common/dtd/ldml.dtd">,
     * which for generated vxml and pxml means we need ldml.dtd in locations like:
     *
     *     vetdata/vxml/common/dtd/ldml.dtd
     *     vetdata/pxml/common/dtd/ldml.dtd
     *     vetdata-2019-05-29T02-08-33-389Z/vxml/common/dtd/ldml.dtd
     *     vetdata-2019-05-29T02-08-33-389Z/pxml/common/dtd/ldml.dtd
     *
     * They should be copies of "trunk" like cldr/common/dtd/ldml.dtd
     */
    private boolean copyDtd(File vetdataDir) {
        String dtdDirName = "dtd";
        String dtdFileName = "ldml.dtd";
        File baseDir = CLDRConfig.getInstance().getCldrBaseDirectory();
        String dtdSourceName = baseDir + "/" + DirNames.justCommon + "/" + dtdDirName + "/" + dtdFileName;
        File dtdSource = new File(dtdSourceName);
        if (!dtdSource.exists()) {
            return false;
        }
        String vp[] = { Kind.vxml.toString(), Kind.pxml.toString() };
        for (String s: vp) {
            File destDir = new File(vetdataDir + "/" + s + "/" + DirNames.justCommon + "/" + dtdDirName);
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
     * @param out the Writer, to receive HTML output
     * @param vetDataDir the folder in which to write
     * @param makeSeparateDir true if vetDataDir is a newly created "manual" folder,
     *                        false if it's the regular auto folder
     * @return true for success, false for failure
     *
     * This function was first created using code moved here from admin-OutputAllFiles.jsp.
     * Reference: CLDR-12016 and CLDR-11877 and CLDR-11850
     */
    private boolean outputAllFiles(Writer out, File vetDataDir, boolean makeSeparateDir) {
        try {
            long start = System.currentTimeMillis();
            ElapsedTimer overallTimer = new ElapsedTimer("overall update started " + new java.util.Date());
            int numupd = 0;

            out.write("<ol>\n");

            Set<CLDRLocale> sortSet = new TreeSet<CLDRLocale>();
            sortSet.addAll(SurveyMain.getLocalesSet());
            /*
             * If makeSeparateDir is false, only replace files if they need to be updated; use
             * a database Connection to help determine whether files need to be updated.
             * If makeSeparateDir is true, all files are new so there's no need for Connection.
             */
            Connection conn = null;
            try {
                if (!makeSeparateDir) {
                    conn = sm.dbUtils.getDBConnection();
                }
                for (CLDRLocale loc : sortSet) {
                    Timestamp locTime = null;
                    if (conn != null) {
                        locTime = this.getLocaleTime(conn, loc);
                        out.write("<li>" + loc.getDisplayName() + " - " + locTime.toLocaleString() + "<br/>\n");
                    } else {
                        out.write("<li>" + loc.getDisplayName() + "<br/>\n");
                    }
                    for (OutputFileManager.Kind kind : OutputFileManager.Kind.values()) {
                        /*
                         * TODO: is there any point in outputting anything here for kind other than vxml and pxml?
                         */
                        boolean nu = makeSeparateDir || this.fileNeedsUpdate(locTime, loc, kind.name());
                        String background = nu ? "#ff9999" : "green";
                        String weight = nu ? "regular" : "bold";
                        String color = nu ? "silver" : "black";
                        out.write("\n\n\t<span style=' background-color: " + background + "; font-weight: " + weight + "; color: " + color + ";'>");
                        out.write(kind.toString());
                        if (nu && (kind == OutputFileManager.Kind.vxml || kind == OutputFileManager.Kind.pxml)) {
                            System.err.println("Writing " + loc.getDisplayName() + ":" + kind);
                            ElapsedTimer et = new ElapsedTimer("to write " + loc + ":" + kind);
                            if (makeSeparateDir) {
                                File f = writeManualOutputFile(vetDataDir, loc, kind);
                                if (f == null) {
                                    out.write("FILE CREATION FAILED: " + loc.toString() + kind.name());
                                    return false;
                                }
                            } else {
                                File f = this.getOutputFile(conn, loc, kind.name());
                                out.write(" x=" + (f != null && f.exists()));
                            }
                            numupd++;
                            System.err.println(et + " - upd " + numupd + "/" + (sortSet.size() + 2));
                        }
                        out.write("</span>  &nbsp;");
                    }
                    out.write("</li>\n");
                }
            } finally {
                if (conn != null) {
                    DBUtils.close(conn);
                }
            }
            out.write("</ol>\n");
            out.write("<hr>\n");
            out.write("Total upd: " + numupd + "/" + (sortSet.size() + 2) + "\n");
            out.write("Total time: " + overallTimer + " : " + ((System.currentTimeMillis() - start) / (1000.0 * 60)) + "min\n");

            System.err.println(overallTimer + " - updated " + numupd + "/" + (sortSet.size() + 2) +
                " in " + (System.currentTimeMillis() - start) / (1000.0 * 60) + " min");
            return true;
        } catch (Exception e) {
            System.err.println("Exception in outputAllFiles: " + e);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Write out the specified file(s).
     *
     * If kind is vxml (for example), we may write to both common/main and common/annotations,
     * or to both seed/main and seed/annotations.
     *
     * Note: this is only used for "manually" generated files.
     * Compare writeOutputFile which is for "automatic" scheduled generation of files.
     *
     * @param loc the CLDRLocale
     * @param kind the Kind, currently Kind.vxml and Kind.pxml are supported
     * @return the File, or null for failure
     */
    private File writeManualOutputFile(File vetDataDir, CLDRLocale loc, Kind kind) {
        long st = System.currentTimeMillis();
        CLDRFile cldrFile;
        if (kind == Kind.vxml) {
            cldrFile = sm.getSTFactory().makeVettedFile(loc);
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
            File baseDir = CLDRConfig.getInstance().getCldrBaseDirectory();
            String commonOrSeed = DirNames.justCommon;
            for (String c: DirNames.commonAndSeed) {
                String path = baseDir + "/" + c + "/" + DirNames.justMain + "/" + loc.toString() + XML_SUFFIX;
                if (new File(path).exists()) {
                    commonOrSeed = c;
                    break;
                }
            }
            /*
             * Only create the file in "main" here; doWriteFile will then create the file in "annotations"
             */
            String outDirName = vetDataDir + "/" + kind.toString() +  "/" + commonOrSeed + "/" + DirNames.justMain;
            File outDir = new File(outDirName);
            if (!outDir.exists() && !outDir.mkdirs()) {
                throw new InternalError("Unable to create directory: " + outDirName);
            }
            String outFileName = outDirName + "/" + loc.toString() + XML_SUFFIX;
            File outFile = new File(outFileName);
            doWriteFile(loc, cldrFile, kind, outFile);
            SurveyLog.debug("Updater: MANUALLY wrote: " + kind + "/" + loc + " - " + ElapsedTimer.elapsedTime(st));
            return outFile;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("IO Exception " + e.toString(), e);
        }
    }

    /**
     * Remove "empty" VXML files in a set of directories
     *
     * @param out the Writer, to receive HTML output
     *
     * Compare RemoveEmptyCLDR.main
     *
     * Reference: https://unicode-org.atlassian.net/browse/CLDR-12016
     */
    private void removeEmptyFiles(Writer out, File vxmlDir) throws IOException {
        for (String c: DirNames.commonAndSeed) {
            /*
             * Skip main. Only do common/annotations and seed/annotations.
             */
            File dirFile = new File(vxmlDir + "/" + c + "/" + DirNames.justAnnotations);
            if (dirFile.exists()) {
                removeEmptyFilesOneDir(out, dirFile);
            }
        }
    }

    /**
     * Remove "empty" VXML files in the given directory
     *
     * @param out the Writer, to receive HTML output
     * @param dirFile the given directory
     * @throws IOException
     */
    private void removeEmptyFilesOneDir(Writer out, File dirFile) throws IOException {
        Set<String> treatAsNonEmpty = new HashSet<>();
        BiMap<String, File> onlyHasIdentity = HashBiMap.create();
        int counter = 0;
        for (File f : dirFile.listFiles()) {
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
                    System.out.println(++counter + ") NOT-EMPTY: " + canonicalPath);
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
                System.out.println(++counter + ") Deleting: " + file.getCanonicalPath());
                file.delete();
            }
        }
    }

    /**
     * Add the given name, and the names of its ancestors (except root), to the given set
     *
     * @param treatAsNonEmpty
     * @param name the xml file name (without the .xml extension)
     */
    private static void addNameAndParents(Set<String> treatAsNonEmpty, String name) {
        treatAsNonEmpty.add(name);
        String parent = LocaleIDParser.getParent(name);
        if (!"root".equals(parent)) {
            addNameAndParents(treatAsNonEmpty, parent);
        }
    }

    /**
     * Verify all VXML files
     *
     * @param out the Writer, to receive HTML output
     *
     * The following need to be verified on the server when generating vxml:
     * • The same file must not occur in both the common/X and seed/X directories, for any X=main|annotations
     * • If a parent locale (except for root) must occur in the same directory as the child locale
     * • Every file in trunk (common|seed/X) must have a corresponding vxml file
     * • Every file in vxml (common|seed/X) must have a corresponding trunk file
     * This should fail with clear warning to the user that there is a major problem.
     * Reference: CLDR-12016
     * @throws IOException
     *
     * vetdata
     * └── vxml
     *     ├── common
     *     │   ├── annotations
     *     │   └── main
     *     └── seed
     *         ├── annotations
     *         └── main
     */
    private void verifyAllFiles(Writer out, File vxmlDir) throws IOException {
        int failureCount = 0;

        /*
         * The same file must not occur in both the common/X and seed/X directories, for any X=main|annotations
         */
        if (!verifyNoDuplicatesInCommonAndSeed(out, vxmlDir)) {
            ++failureCount;
        }
        /*
         * A parent locale (except for root) must occur in the same directory as the child locale
         */
        if (!verifyParentChildSameDirectory(out, vxmlDir)) {
            ++failureCount;
        }
        /*
         * Every file in trunk (common|seed/X) must have a corresponding vxml file
         * Every file in vxml (common|seed/X) must have a corresponding trunk file
         */
        if (!verifyVxmlAndBaselineFilesCorrespond(out, vxmlDir)) {
            ++failureCount;
        }

        if (failureCount == 0) {
            out.write("<h1>✅ VXML verification succeeded</h1>\nOK<br>");
        } else {
            out.write("<h1>❌ VXML verification failed!</h1>\nFailure count = " + failureCount + "<br>");
        }
    }

    /**
     * Verify that the same file does not occur in both the common/X and seed/X directories, for any X=main|annotations
     *
     * @param out the Writer, to receive HTML output
     * @return true if verification succeeded, false for failure
     * @throws IOException
     */
    private boolean verifyNoDuplicatesInCommonAndSeed(Writer out, File vxmlDir)
            throws IOException {

        for (String m: DirNames.mainAndAnnotations) {
            String commonDirName = vxmlDir + "/" + DirNames.justCommon + "/" + m;
            String seedDirName = vxmlDir + "/" + DirNames.justSeed + "/" + m;
            File dirFile = new File(commonDirName);
            if (dirFile.exists()) {
                for (String commonName : dirFile.list()) {
                    String commonPathName = commonDirName + "/" + commonName;
                    String seedPathName = seedDirName + "/" + commonName;
                    File fSeed = new File(seedPathName);
                    if (fSeed.exists()) {
                        out.write("<h2>Verification failure, found duplicates</h2>\n"
                            + commonPathName + "<br>\n"
                            + seedPathName + "<br>\n");
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Verify that a file for parent locale does occur in the same directory as the file for the child locale
     *
     * Examples:
     *   if we have "aa_NA.xml" we should have "aa.xml" in the same folder
     *   if we have "ff_Adlm_BF.xml" we should have "ff_Adlm.xml" in the same folder
     * Note handling of two underscores in "ff_Adlm_BF.xml"
     *
     * @param out the Writer, to receive HTML output
     * @return true if verification succeeded, false for failure
     * @throws IOException
     */
    private boolean verifyParentChildSameDirectory(Writer out, File vxmlDir)
            throws IOException {

        for (String c: DirNames.commonAndSeed) {
            for (String m: DirNames.mainAndAnnotations) {
                String dirName = vxmlDir + "/" + c + "/" + m;
                File dirFile = new File(dirName);
                if (!dirFile.exists()) {
                    continue;
                }
                for (String childName : dirFile.list()) {
                    String childPathName = dirName + "/" + childName;
                    /*
                     * Get the parent from the child. Change "aa_NA.xml" to "aa.xml";
                     * "ff_Adlm_BF.xml" to "ff_Adlm.xml"; "sr_Cyrl_BA.xml" to "sr_Cyrl.xml" (not "sr.xml")
                     */
                    String localeName = childName.replaceFirst("\\.xml$", "");
                    CLDRLocale childLoc = CLDRLocale.getInstance(localeName);
                    if (childLoc == null) {
                        out.write("<h2>Verification failure, locale not recognized from file name</h2>\n"
                            + childPathName + "<br>\n");
                        return false;
                    }
                    CLDRLocale parLoc = childLoc.getParent();
                    if (parLoc != null) {
                        // String parentName = fileName.replaceFirst("_[a-zA-Z]+\\.xml$", "\\.xml");
                        String parentName = parLoc.toString() + XML_SUFFIX;
                        if (!childName.equals(parentName) && !"root.xml".equals(parentName)) {
                            String parentPathName = dirName + "/" + parentName;
                            File fParent = new File(parentPathName);
                            if (!fParent.exists() && !otherParentExists(parentPathName, parentName, c)) {
                                out.write("<h2>Verification failure, child without parent</h2>\n"
                                    + "Child, present: " + childPathName + "<br>\n"
                                    + "Parent, absent: " + parentPathName + "<br>\n");
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Does a parent exist in the other possible location?
     *
     * Given, e.g., "el_POLYTON.xml", we're looking for its "parent", e.g., "el.xml".
     * We already know it's not in the same directory as the child. Try the related
     * directory which is obtained by changing "seed" to "common". E.g.:
     * Child:  .../seed/main/el_POLYTON.xml
     * Missing parent: .../seed/main/el.xml
     * Other parent: .../common/main/el.xml
     *
     * @param parentPathName like ".../seed/main"
     * @param parentName like "el_POLYTON.xml"
     * @param commonOrSeed where we already looked: "common" (give up) or "seed" (try "common")
     * @return true if the other parent exists
     */
    private boolean otherParentExists(String parentPathName, String parentName, String commonOrSeed) {
        /*
         * Allow "parent in common and child in seed" but not vice-versa
         */
        if (commonOrSeed.equals(DirNames.justCommon)) {
            return false; // give up
        }
        /*
         * Replace "/seed/" with "/common/" in the parent path
         */
        parentPathName = parentPathName.replace(
                "/" + DirNames.justSeed + "/",
                "/" + DirNames.justCommon + "/");
        return new File(parentPathName).exists();
    }

    /**
     * Verify that every file in baseline-cldr (common|seed/X) has a corresponding vxml file (required)
     * AND every file in vxml (common|seed/X) has a corresponding baseline-cldr file (optional).
     * The first condition is required for verification to succeed; the second is not required,
     * and only results in a notification (new files in vxml are to be expected from time to time).
     *
     * @param out the Writer, to receive HTML output
     * @return true if verification succeeded, false for failure
     * @throws IOException
     */
    private boolean verifyVxmlAndBaselineFilesCorrespond(Writer out, File vxmlDir)
            throws IOException {

        String bxmlDir = CLDRConfig.getInstance().getCldrBaseDirectory().toString();
        ArrayList<String> vxmlFiles = new ArrayList<String>();
        ArrayList<String> bxmlFiles = new ArrayList<String>(); /* bxml = baseline cldr xml */
        for (String c: DirNames.commonAndSeed) {
            for (String m: DirNames.mainAndAnnotations) {
                File vxmlDirFile = new File(vxmlDir + "/" + c + "/" + m);
                File bxmlDirFile = new File(bxmlDir + "/" + c + "/" + m);
                if (vxmlDirFile.exists()) {
                    for (String name : vxmlDirFile.list()) {
                        vxmlFiles.add(c + "/" + m  + "/" + name);
                    }
                }
                if (bxmlDirFile.exists()) {
                    for (String name : bxmlDirFile.list()) {
                        bxmlFiles.add(c + "/" + m  + "/" + name);
                    }
                }
            }
        }
        Set<String> diff = symmetricDifference(vxmlFiles, bxmlFiles);
        if (!diff.isEmpty()) {
            boolean someOnlyInVxml = false, someOnlyInBxml = false;
            for (String name: diff) {
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
                out.write("<h2>Verification notice, file(s) present in vxml but not in baseline</h2>\n");
                for (String name: diff) {
                    if (vxmlFiles.contains(name)) {
                        out.write(name + "<br>\n");
                    }
                }
            }
            if (someOnlyInBxml) {
                out.write("<h2>Verification failure, file(s) present in baseline but not in vxml</h2>\n");
                for (String name: diff) {
                    if (bxmlFiles.contains(name)) {
                        out.write(name + "<br>\n");
                    }
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Return the set of all strings that are in one list but not the other
     *
     * @param list1
     * @param list2
     * @return the Set
     */
    private static Set<String> symmetricDifference(final ArrayList<String> list1, final ArrayList<String> list2) {
        Set<String> diff = new HashSet<String>(list1);
        diff.addAll(list2);
        Set<String> tmp = new HashSet<String>(list1);
        tmp.retainAll(list2);
        diff.removeAll(tmp);
        return diff;
    }

    /**
     * Write out the specified file.
     *
     * Note: this is only used for "automatic" scheduled generation of files.
     * Compare writeManualOutputFile which is for "manually" generated files.
     *
     * @param loc
     * @param kind
     * @return
     */
    private File writeOutputFile(CLDRLocale loc, Kind kind) {
        long st = System.currentTimeMillis();
        CLDRFile file;
        if (kind == Kind.vxml) {
            file = sm.getSTFactory().makeVettedFile(loc);
        } else if (kind == Kind.pxml) {
            file = sm.getSTFactory().makeProposedFile(loc);
        } else {
            throw new InternalError("Don't know how to make kind " + kind + " for loc " + loc);
        }
        try {
            File outFile = sm.getDataFile(kind.toString(), loc);

            doWriteFile(loc, file, kind, outFile);
            SurveyLog.debug("Updater: Wrote: " + kind + "/" + loc + " - " + ElapsedTimer.elapsedTime(st));

            if (tryCommit && (kind.equals("vxml") || kind.equals("pxml"))) {
                try {
                    ElapsedTimer et = new ElapsedTimer();
                    svnAdd(outFile);
                    if (true)
                        System.err.println("SVN: added " + outFile.getAbsolutePath() + " t=" + et);
                } catch (SVNException e) {
                    if (e.getMessage().contains("E155007")) {
                        SurveyLog.logException(e, "Trying to add [and giving up on SVN commits!]" + outFile.getAbsolutePath());
                        tryCommitWhyNot = "Trying to add [and giving up on SVN commits!]" + outFile.getAbsolutePath() + " - "
                            + e.toString();
                        tryCommit = false;
                    } else if (e.getMessage().contains("E155015")) {
                        svnRemoveAndResolved(outFile);
                        doWriteFile(loc, file, kind, outFile);
                        SurveyLog
                            .debug("Updater: Resolved, Re-Wrote: " + kind + "/" + loc + " - " + ElapsedTimer.elapsedTime(st));
                    } else if (!e.getMessage().contains("E150002")) {
                        SurveyLog.logException(e, "Trying to add " + outFile.getAbsolutePath());
                    }
                }
            }
            return outFile;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("IO Exception " + e.toString(), e);
        }
    }

    static final Predicate<String> isAnnotations = x -> x.startsWith("//ldml/annotations");

    Map<String, Object> OPTS_SKIP_ANNOTATIONS = ImmutableMap.of(
        "SKIP_PATH", isAnnotations);
    /*
     * TODO: decide whether to keep SKIP_FILE_IF_SKIP_ALL_PATHS.
     * If we do a separate "remove empty files" step (better due to taking
     * inheritance into account, e.g., if sr_Cyrl_BA.xml is non-empty, then sr_Cyrl.xml
     * will also be treated as non-empty) then we won't use SKIP_FILE_IF_SKIP_ALL_PATHS.
     * Reference: https://unicode-org.atlassian.net/browse/CLDR-12016
     */
    Map<String, Object> OPTS_KEEP_ANNOTATIONS = ImmutableMap.of(
        "SKIP_PATH", isAnnotations.negate()
        /***,
        "SKIP_FILE_IF_SKIP_ALL_PATHS", true
        ***/
        );

    /**
     * Write one or more files. For vxml (at least), write one in "main" and one in "annotations".
     *
     * @param loc the CLDRLocale
     * @param file the CLDRFile for reading
     * @param outFile the File for "main"; another file will be created in "annotations"
     * @throws UnsupportedEncodingException
     * @throws FileNotFoundException
     */
    private void doWriteFile(CLDRLocale loc, CLDRFile file, Kind kind, File outFile) throws UnsupportedEncodingException,
        FileNotFoundException {
        try (PrintWriter u8out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF8"))) {
            if (kind == Kind.vxml || kind == Kind.rxml) {
                file.write(u8out, OPTS_SKIP_ANNOTATIONS);

                // output annotations, too
                File parentDir = outFile.getParentFile().getParentFile();
                File annotationsDir = new File(parentDir, "annotations"); // TODO: avoid hard-coding "annotations" here
                annotationsDir.mkdirs();
                File aFile = new File(annotationsDir, outFile.getName()); // same name, different subdir
                try (PrintWriter u8outa = new PrintWriter(new OutputStreamWriter(new FileOutputStream(aFile), "UTF8"))) {
                    if (!file.write(u8outa, OPTS_KEEP_ANNOTATIONS)) {
                        aFile.delete();
                    }
                }
            } else {
                file.write(u8out);
            }
        }
    }

    /**
     * TODO: delete dead code if there's no plan to resurrect it
     *
     * @param ctx
     */
    public void doRaw(WebContext ctx) {
        ctx.println("raw not supported currently. ");
    }

    /**
     * For a request like ".../cldr-apps/survey/vxml/main/aa.xml", respond with the xml
     *
     * @param request
     * @param response
     * @return true if request is for a kind of xml we can provide, else false.
     * @throws IOException
     * @throws ServletException
     *
     * Called by SurveyMain.doGet when get a request.
     */
    public boolean doRawXml(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        /*
         * request.getPathInfo returns what follows "survey" in the url.
         * If the url is ".../cldr-apps/survey/vxml/main/aa.xml", it returns "vxml/main/aa.xml".
         */
        String s = request.getPathInfo();
        if ((s == null)
            || !(s.startsWith(XML_PREFIX) || s.startsWith(ZXML_PREFIX) || s.startsWith(ZVXML_PREFIX)
                || s.startsWith(VXML_PREFIX) || s.startsWith(PXML_PREFIX)
                || s.startsWith(RXML_PREFIX) || s.startsWith(TXML_PREFIX) || s.startsWith(FEED_PREFIX))) {
            return false;
        }

        if (s.startsWith(FEED_PREFIX)) {
            return sm.fora.doFeed(request, response);
        }

        CLDRProgressTask p = sm.openProgress("Raw XML");
        try {

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
                s = s.substring(VXML_PREFIX.length() + 1, s.length()); // "foo.xml"
            } else if (s.startsWith(PXML_PREFIX)) {
                finalData = true;
                if (s.equals(PXML_PREFIX)) {
                    return true;
                }
                kind = "pxml";
                s = s.substring(PXML_PREFIX.length() + 1, s.length()); // "foo.xml"
            } else if (s.startsWith(RXML_PREFIX)) {
                finalData = true;
                if (s.equals(RXML_PREFIX)) {
                    WebContext ctx = new WebContext(request, response);
                    response.sendRedirect(ctx.schemeHostPort() + ctx.base() + RXML_PREFIX + "/");
                    return true;
                }
                kind = "rxml"; // cached
                s = s.substring(RXML_PREFIX.length() + 1, s.length()); // "foo.xml"
            } else if (s.startsWith(TXML_PREFIX)) {
                finalData = true;
                if (s.equals(TXML_PREFIX)) {
                    WebContext ctx = new WebContext(request, response);
                    response.sendRedirect(ctx.schemeHostPort() + ctx.base() + TXML_PREFIX + "/");
                    return true;
                }
                s = s.substring(TXML_PREFIX.length() + 1, s.length()); // "foo.xml"
            } else if (s.startsWith(ZXML_PREFIX)) {
                finalData = false;
                s = s.substring(ZXML_PREFIX.length() + 1, s.length()); // "foo.xml"
            } else if (s.startsWith(ZVXML_PREFIX)) {
                finalData = true;
                s = s.substring(ZVXML_PREFIX.length() + 1, s.length()); // "foo.xml"
            } else {
                if (s.equals(XML_PREFIX)) {
                    WebContext ctx = new WebContext(request, response);
                    response.sendRedirect(ctx.schemeHostPort() + ctx.base() + XML_PREFIX + "/");
                    return true;
                }
                kind = "xml";
                s = s.substring(XML_PREFIX.length() + 1, s.length()); // "foo.xml"
            }

            if (s.length() == 0) {
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
                CLDRLocale locales[] = SurveyMain.getLocales();
                int nrInFiles = locales.length;
                for (int i = 0; i < nrInFiles; i++) {
                    CLDRLocale locale = locales[i];
                    String localeName = locale.getBaseName();
                    String fileName = localeName + XML_SUFFIX;
                    ctx.println("<li><a href='" + fileName + "'>" + fileName + "</a> " + locale.getDisplayName(ctx.displayLocale)
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
                CLDRLocale locales[] = SurveyMain.getLocales();
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
                    boolean isKvp = (doKvp != null && doKvp.length() > 0);

                    if (isKvp) {
                        response.setContentType("text/plain; charset=utf-8");
                    } else {
                        response.setContentType("application/xml; charset=utf-8");
                    }

                    if (kind.equals("vxml")) {
                        sm.getSTFactory().make(foundLocale.getBaseName(), false).write(response.getWriter());
                        return true;
                    } else if (kind.equals("pxml")) {
                        sm.getSTFactory().makeProposedFile(foundLocale).write(response.getWriter());
                        return true;
                    }
                }
            }
            return true;
        } finally {
            if (p != null) {
                p.close();
            }
        }
    }

    /**
     * Get the output file, creating if needed. Uses a temp Connection
     *
     * @param surveyMain
     *            TODO
     * @param loc
     * @param kind
     * @return
     * @throws IOException
     * @throws SQLException
     */
    File getOutputFile(SurveyMain surveyMain, CLDRLocale loc, String kind) throws IOException, SQLException {
        /*
         * Get a java.sql.Connection to be used by fileNeedsUpdate, getLocaleTime
         */
        Connection conn = null;
        try {
            conn = surveyMain.dbUtils.getDBConnection();
            return getOutputFile(conn, loc, kind);
        } finally {
            DBUtils.close(conn);
        }
    }

    /**
     * Get and write the file
     *
     * @param conn
     * @param loc
     * @param kind
     * @return
     * @throws SQLException
     * @throws IOException
     */
    public File getOutputFile(Connection conn, CLDRLocale loc, String kind) throws SQLException, IOException {
        if (fileNeedsUpdate(conn, loc, kind)) {
            if (!isCacheableKind(kind)) {
                throw new InternalError("Can't (yet) cache kind " + kind + " for loc " + loc);
            }
            return writeOutputFile(loc, Kind.valueOf(kind));
        } else {
            return sm.getDataFile(kind, loc);
        }
    }

    public Timestamp getLocaleTime(CLDRLocale loc) throws SQLException {
        Connection conn = null;
        try {
            conn = DBUtils.getInstance().getDBConnection();
            return getLocaleTime(conn, loc);
        } finally {
            DBUtils.close(conn);
        }
    }

    boolean haveVbv = false;
    public boolean outputDisabled = false;

    /**
     * Get a timestamp associated with the given CLDRLocale and java.sql.Connection.
     *
     * The timestamp may depend on
     * (1) when the most recent vote was made for any item in the given locale
     * (2) the modification time of a file for that locale in trunk/baseline (version control)
     * (3) the timestamp for the parent locale, if more recent
     *
     * @param conn the java.sql.Connection used for querying the VOTE_VALUE table
     * @param loc the CLDRLocale
     * @return the Timestamp
     * @throws SQLException
     */
    public Timestamp getLocaleTime(Connection conn, CLDRLocale loc) throws SQLException {
        Timestamp theDate = null;
        if (haveVbv || DBUtils.hasTable(conn, DBUtils.Table.VOTE_VALUE.toString())) {
            if (haveVbv == false) {
                SurveyLog
                    .debug("OutputFileManager: have "
                        + DBUtils.Table.VOTE_VALUE
                        + ", commencing  output file updates ( use CLDR_NOOUTPUT=true in cldr.properties to suppress  -  CLDR_NOOUTPUT current value = "
                        + CldrUtility.getProperty("CLDR_NOOUTPUT", false));
            }
            haveVbv = true;
            Object[][] o = DBUtils.sqlQueryArrayArrayObj(conn, "select max(last_mod) from " + DBUtils.Table.VOTE_VALUE + " where locale=?", loc);
            if (o != null && o.length > 0 && o[0] != null && o[0].length > 0) {
                theDate = (Timestamp) o[0][0];
                // System.err.println("for " + loc + " = " + theDate +
                // " - len="+o.length + ":"+o[0].length);
            }
        }
        File svnFile = sm.getBaseFile(loc);
        if (svnFile.exists()) {
            Timestamp fileTimestamp = new Timestamp(svnFile.lastModified());
            if (theDate == null || fileTimestamp.after(theDate)) {
                theDate = fileTimestamp;
            }
        }

        CLDRLocale parLoc = loc.getParent();
        if (parLoc != null) {
            Timestamp parTimestamp = getLocaleTime(conn, parLoc);
            if (theDate == null || parTimestamp.after(theDate)) {
                theDate = parTimestamp;
            }
        }

        return theDate;
    }

    static final boolean debugWhyUpdate = false;

    /**
     * Is an update needed for a file for the given locale and kind?
     *
     * @param loc the locale
     * @param kind VXML, etc.
     * @return true if an update is needed, else false
     *
     * @throws IOException
     * @throws SQLException
     */
    private boolean fileNeedsUpdate(Connection conn, CLDRLocale loc, String kind) throws SQLException, IOException {
        return fileNeedsUpdate(getLocaleTime(conn, loc), loc, kind);
    }

    public boolean fileNeedsUpdate(Timestamp theDate, CLDRLocale loc, String kind) throws SQLException, IOException {
        File outFile = sm.getDataFile(kind, loc);
        if (!outFile.exists()) {
            if (debugWhyUpdate)
                SurveyLog.debug("Out of Date: MISSING! Must output " + loc + " / " + kind);
            return true;
        }
        Timestamp theFile = null;

        long lastMod = outFile.lastModified();
        if (outFile.exists()) {
            theFile = new Timestamp(lastMod);
        }
        if (theDate == null) {
            SurveyLog.logger.warning(" .. no data.");
            return false; // no data (?)
        }
        if (debugWhyUpdate)
            SurveyLog.debug(loc + " .. exists " + theFile + " vs " + theDate);
        if (theFile != null && !theFile.before(theDate)) {
            if (debugWhyUpdate)
                SurveyLog.debug(" .. OK, up to date.");
            return false;
        }
        if (debugWhyUpdate)
            SurveyLog.debug("Out of Date: Must output " + loc + " / " + kind + " - @" + theFile + " vs  SQL " + theDate);
        return true;
    }

    void addUpdateTasks() {
        if (CldrUtility.getProperty("CLDR_NOOUTPUT", false))
            return;

        System.err.println("addPeriodicTask... updater");
        SurveyMain.addPeriodicTask(new Runnable() {
            // Start on a different locale each time.
            int spinner = (int) Math.round(Math.random() * (double) SurveyMain.getLocales().length);

            @Override
            public void run() {
                if (outputDisabled || SurveyMain.isBusted() || !SurveyMain.isSetup) {
                    return;
                }

                final String CLDR_OUTPUT_ONLY = CldrUtility.getProperty("CLDR_OUTPUT_ONLY", null);

                if (CLDR_OUTPUT_ONLY != null) {
                    System.err.println("Only outputting -DCLDR_OUTPUT_ONLY=" + CLDR_OUTPUT_ONLY);
                }

                Connection conn = null;
                CLDRProgressTask progress = null;
                try {
                    conn = DBUtils.getInstance().getDBConnection();
                    CLDRLocale locs[] = SurveyMain.getLocales();
                    //File outFile = null;
                    // SurveyLog.logger.warning("Updater: locs to do: "
                    // +locs.length );
                    CLDRLocale loc = null;

                    for (int wrtl = 1; wrtl < locs.length; wrtl++) { // keep
                        // going
                        // while
                        // not busy

                        for (int j = 0; j < locs.length; j++) { // Try 16
                            // locales
                            // looking for
                            // one that
                            // doesn't
                            // exist. No
                            // more, due to
                            // load.
                            loc = CLDR_OUTPUT_ONLY != null ? CLDRLocale.getInstance(CLDR_OUTPUT_ONLY) // DEBUGGING
                                : locs[(spinner++) % locs.length]; // A new
                            // one
                            // each
                            // time.
                            // (normal
                            // case
                            // SurveyLog.debug("Updater: Considering: " +loc );

                            Timestamp localeTime = getLocaleTime(conn, loc);
                            SurveyLog.debug("Updater: Considering: " + loc + " - " + localeTime);
                            if (!fileNeedsUpdate(localeTime, loc, "vxml") /*
                                                                          * &&
                                                                          * !
                                                                          * fileNeedsUpdate
                                                                          * (
                                                                          * localeTime
                                                                          * ,
                                                                          * loc
                                                                          * ,
                                                                          * "xml"
                                                                          * )
                                                                          */) {
                                loc = null;
                                // progress.update(0, "Still looking.");
                            } else {
                                SurveyLog.debug("Updater: To update:: " + loc + " - " + localeTime);
                                break; // update it.
                            }
                            if (j % 16 == 0) {
                                // SurveyLog.debug("Updater: looked at " + j +
                                // " locales, sleeping..");
                                Thread.sleep(1000);
                                if (SurveyMain.hostBusy()) {
                                    SurveyLog.debug("CPU busy - exitting." + SurveyMain.osmxbean.getSystemLoadAverage());
                                    return;
                                } else {
                                    SurveyLog.debug("CPU not busy- continuing!" + SurveyMain.osmxbean.getSystemLoadAverage());
                                }
                            }
                        }

                        if (loc == null) {
                            SurveyLog.debug("None to update.");
                            // SurveyLog.logger.warning("All " + locs.length +
                            // " up to date.");
                            return; // nothing to do.
                        }

                        if (progress == null) {
                            progress = sm.openProgress("Updater", 3);
                        }
                        progress.update(1, "Update vxml:" + loc);
                        SurveyLog.debug("Updater update vxml: " + loc);
                        getOutputFile(sm, loc, "vxml");
                        getOutputFile(sm, loc, "pxml");
                        /*
                         * progress.update(2, "Writing xml:" +loc);
                         * getOutputFile(loc, "xml");
                         */
                        progress.update(3, "Done:" + loc);

                        if (SurveyMain.hostBusy()) {
                            SurveyLog.debug("Wrote " + wrtl + "locales  , but host is busy:  "
                                + SurveyMain.osmxbean.getSystemLoadAverage());
                            return;
                        } else {
                            Thread.sleep(5000);
                            if (SurveyMain.hostBusy()) {
                                SurveyLog.debug("Wrote " + wrtl + "locales, slept 5s , but host is now busy:  "
                                    + SurveyMain.osmxbean.getSystemLoadAverage());
                                return;
                            } else {
                                SurveyLog.debug("Wrote " + wrtl + "locales  , continuing! host is not busy:  "
                                    + SurveyMain.osmxbean.getSystemLoadAverage());
                            }
                        }
                    }

                    // SurveyLog.logger.warning("Finished writing " + loc);
                } catch (InterruptedException ie) {
                    SurveyLog.logger.warning("Interrupted while running Updater - goodbye: " + ie);
//                } catch (SQLException e) {
//                    SurveyLog.logException(e, "while running updater");
//                    outputDisabled = true; // SurveyMain.busted("while running updater", e);
//                } catch (IOException e) {
//                    SurveyLog.logException(e);
//                    e.printStackTrace();
//                    SurveyMain.busted("while running updater", e);
                } catch (Throwable e) {
                    SurveyLog.logException(e, "while running updater");
                    e.printStackTrace();
                    outputDisabled = true; // SurveyMain.busted("while running updater", e);
                } finally {
                    // SurveyLog.logger.warning("(exitting updater");
                    if (progress != null)
                        progress.close();
                    DBUtils.close(conn);
                }
            }
        });

        SurveyMain.addDailyTask(new Runnable() {

            @Override
            public void run() {
                SurveyMain sm = CookieSession.sm;
                //ElapsedTimer daily = new ElapsedTimer();
                // Date ourDate = new Date();
                try {
                    File usersa = sm.makeDataDir("usersa");
                    sm.reg.writeUserFile(sm, "sometime", true, new File(usersa, "usersa.xml"));
                    File users = sm.makeDataDir("users");
                    sm.reg.writeUserFile(sm, "sometime", false, new File(users, "users.xml"));
                    System.err.println("Writing users data: " + new Date());
                } catch (Throwable t) {
                    SurveyLog.logException(t, "writing user data");
                }

                addAndCommitData("vxml"); // vetted

                addAndCommitData("pxml"); // proposed

            }

            private void addAndCommitData(String type) {
                if (!tryCommit)
                    return;
                ElapsedTimer daily = new ElapsedTimer();
                try {
                    //boolean svnOk = true;
                    System.err.println("Beginning daily (or once at boot) update of SVN " + type + " data: " + new Date());
                    // quickAddAll
                    int added = 0;
                    File some = null;
                    int toupdate = 0;
                    CLDRLocale locs[] = SurveyMain.getLocales();
                    // System.err.println("Traversing..!!");
                    for (CLDRLocale l : locs) {
                        try {
                            File f = CookieSession.sm.getDataFile(type, l);
                            if (some == null) {
                                some = f.getParentFile().getParentFile().getParentFile();

                                svnCleanup(some);

                            }
                            if (!f.exists())
                                continue;
                            // SVNInfo i = svnInfo(f);
                            SVNStatus s = svnStatus(f);
                            // System.err.println(f.getAbsolutePath() + " - " +
                            // i.getKind() + " - " + s.getNodeStatus());
                            if (s == null) {
                                System.err.println("SVN: empty node status:  - " + f.getAbsolutePath());
                            } else if (s.getNodeStatus() == SVNStatusType.STATUS_UNVERSIONED) {
                                svnAdd(f);
                                added++;
                            } else if (s.getNodeStatus() != SVNStatusType.STATUS_NORMAL) {
                                // System.err.println(f.getAbsolutePath() +
                                // "  - " + s.getNodeStatus());
                                toupdate++;
                            }
                        } catch (Throwable e) {
                            SurveyLog.logException(e, "trying to get data file for " + l);
                            return;
                        }
                    }
                    if (added > 0) {
                        System.err.println("Added " + added + " unversioned files.");
                        toupdate += added;
                    }
                    if (toupdate > 0) {
                        System.err.println("Detected " + toupdate + "  files out of date. Committing:");
                        File f[] = { some };
                        try {
                            System.out.println("committed  " + some.getAbsolutePath() + " -> " + svnCommit(f));
                        } catch (SVNException e) {
                            SurveyLog.logException(e, "Trying to commit [and giving up on commits] " + some.getAbsolutePath());
                            tryCommit = false;
                        }
                    } else {
                        System.err.println("Nothing out of date.");
                    }
                } finally {
                    System.err.println("Exitting Daily " + daily.toString());
                }
            }
        });
    }

    /**
     * Client access to SVN.
     */
    private SVNClientManager ourClientManager = null;

    private synchronized SVNClientManager getClientManager() throws SVNException {
        if (ourClientManager == null) {
            if (tryCommit == false) {
                throw new SVNException(SVNErrorMessage.create(SVNErrorCode.ASSERTION_FAIL, "commits were disabled: "
                    + tryCommitWhyNot));
            }

            ourClientManager = SVNClientManager.newInstance();
        }
        return ourClientManager;
    }

    public long svnCheckout(File dir, String url) throws SVNException {
        return svnCheckout(dir, url, SVNRevision.UNDEFINED, SVNRevision.HEAD, SVNDepth.INFINITY, true);
    }

    public long svnCheckout(File dir, String url, SVNRevision r1, SVNRevision r2, SVNDepth d, boolean b) throws SVNException {
        synchronized (OutputFileManager.class) {
            SVNUpdateClient updateClient = getClientManager().getUpdateClient();
            updateClient.setIgnoreExternals(true);
            System.err.println("Checking out " + url + " into " + dir.getAbsolutePath());
            long rv = updateClient.doCheckout(SVNURL.parseURIEncoded(url), dir, r1, r2, d, b);
            System.err.println(".. Checked out  r" + rv);
            return rv;
        }
    }

    public void svnExport(File dir, String url) throws SVNException {
        synchronized (OutputFileManager.class) {
            SVNUpdateClient updateClient = getClientManager().getUpdateClient();
            updateClient.setIgnoreExternals(true);
            System.err.println("Exporting " + url + " into " + dir.getAbsolutePath());
            long rv = updateClient.doExport(SVNURL.parseURIEncoded(url), dir, SVNRevision.UNDEFINED, SVNRevision.HEAD, "native",
                false, SVNDepth.INFINITY);
            System.err.println(".. Exported r" + rv);
        }
    }

    public SVNCommitInfo svnCommit(File[] f) throws SVNException {
        synchronized (OutputFileManager.class) {
            SVNCommitClient commitClient = getClientManager().getCommitClient();
            return commitClient.doCommit(f, false, "Automated update", false, true);
        }
    }

    public long svnUpdate(File f) throws SVNException {
        synchronized (OutputFileManager.class) {
            SVNUpdateClient updateClient = getClientManager().getUpdateClient();
            return updateClient.doUpdate(f, SVNRevision.HEAD, true);
        }
    }

    public long[] svnUpdate(File f[], SVNRevision rev, SVNDepth depth, boolean allowUnversionedObstructions, boolean depthIsSticky)
        throws SVNException {
        synchronized (OutputFileManager.class) {
            SVNUpdateClient updateClient = getClientManager().getUpdateClient();
            return updateClient.doUpdate(f, rev, depth, allowUnversionedObstructions, depthIsSticky);
        }
    }

    private void svnRemoveAndResolved(File outFile) {
        try {
            synchronized (OutputFileManager.class) {
                getClientManager().getWCClient().doResolve(outFile, true);
            }
        } catch (SVNException e) {
            SurveyLog.logException(e, "While marking " + outFile.getAbsolutePath() + " resolved.");
        } finally {
            outFile.delete();
        }
    }

    public void svnAdd(File f) throws SVNException {
        synchronized (OutputFileManager.class) {
            getClientManager().getWCClient().doAdd(f, false, false, false, true);
        }
    }

    public SVNStatus svnStatus(File item) throws SVNException {
        synchronized (OutputFileManager.class) {
            SVNStatusClient updateClient = getClientManager().getStatusClient();
            return updateClient.doStatus(item, false);
        }
    }

    public SVNInfo svnInfo(File item) throws SVNException {
        synchronized (OutputFileManager.class) {
            SVNWCClient updateClient = getClientManager().getWCClient();
            return updateClient.doInfo(item, SVNRevision.WORKING);
        }
    }

    public void svnCleanup(File item) throws SVNException {
        synchronized (OutputFileManager.class) {
            SVNWCClient updateClient = getClientManager().getWCClient();
            updateClient.doCleanup(item);
            System.err.println("-- cleanup " + item.getAbsolutePath());
        }
    }

    public void svnShutdown() {
        if (ourClientManager != null) {
            ourClientManager.dispose();
            ourClientManager = null;
            Thread.yield();
            System.err.println("Shutdown SVN client.");
        }
    }

    public void svnAddOrWarn(File subDir) {
        if (tryCommit) {
            synchronized (OutputFileManager.class) {
                try {
                    getClientManager().getWCClient().doAdd(subDir, false, false, false, false);
                } catch (SVNException e) {
                    System.err.println("warning: could not add " + subDir.getAbsolutePath() + " - " + e.getMessage());
                }
            }
        }
    }

    // statistics helpers
    private static Map<CLDRLocale, Pair<String, String>> localeNameCache = new ConcurrentHashMap<CLDRLocale, Pair<String, String>>();

    // for the statistics page - wrap locale ids in an <old data> span to show they were from the previous revision
    private static final String OLD_DATA_BEGIN = "<span class='olddata'>";
    private static final String OLD_DATA_END = "</span>";

    public static Pair<String, String> statGetLocaleDisplayName(CLDRLocale loc) {
        Pair<String, String> ret = localeNameCache.get(loc), toAdd = null;
        if (ret == null) {
            toAdd = ret = new Pair<String, String>();
        }
        // note, may concurrently modify this object- that's OK.
        if (ret.getFirst() == null) {
            // use baseline data
            ret.setFirst(loc.getDisplayName(false, null));
        }
        if (ret.getSecond() == null) {
            // uses 'on disk' (old) data.
            ret.setSecond(OLD_DATA_BEGIN + CookieSession.sm.getDiskFactory().make(loc.getBaseName(), true).getName(loc.toLanguageTag()) + OLD_DATA_END);
        }
        // needed to add it
        if (toAdd != null) {
            localeNameCache.put(loc, toAdd);
        }
        return ret;
    }

    // update the cache
    public static void updateLocaleDisplayName(CLDRFile f, CLDRLocale l) {
        try {
            Pair<String, String> ret = statGetLocaleDisplayName(l);
            String newValue = (f.getName(l.getBaseName()));
            if (DEBUG) {
                if (!newValue.equals(ret.getSecond())) {
                    System.out.println("Setting: " + newValue + " insteadof " + ret.getSecond() + " for " + ret.getFirst());
                }
            }
            ret.setSecond(newValue);
        } catch (Throwable t) {
            SurveyLog.logException(t, "Updating the Locale Display Name for " + l.getBaseName() + " with language tag " + l.toLanguageTag());
        }
    }

}
