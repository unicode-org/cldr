package org.unicode.cldr.icu;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.ant.CLDRConverterTool;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCoverage;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LDMLUtilities;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.StandardCodes;
import org.w3c.dom.Node;

import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.text.Transliterator;

public class CheckIBMCoverage extends CLDRConverterTool {
    /**
     * These must be kept in sync with getOptions().
     */
    private static final int HELP1 = 0;
    private static final int HELP2 = 1;
    private static final int SOURCEDIR = 2;
    private static final int DESTDIR = 3;
    private static final int VERBOSE = 4;

    private static final UOption[] options = new UOption[] {
        UOption.HELP_H(),
        UOption.HELP_QUESTION_MARK(),
        UOption.SOURCEDIR(),
        UOption.DESTDIR(),
        UOption.VERBOSE(),
    };

    private String sourceDir = null;
    private String destDir = null;
    private boolean verbose = false;

    private void usage() {
        System.out.println("\nUsage: CheckIBMCoverage [OPTIONS] [FILES]\nCheckIBMCoverage [OPTIONS] -w [DIRECTORY] \n" +
            "This program is used to convert LDML files to ICU ResourceBundle TXT files.\n" +
            "Please refer to the following options. Options are not case sensitive.\n" +
            "Options:\n" +
            "-s or --sourcedir          source directory for files followed by path, default is current directory.\n" +
            "-d or --destdir            destination directory, followed by the path, default is current directory.\n" +
            "-h or -? or --help         this usage text.\n" +
            "-v or --verbose            print out verbose output.\n" +
            "example: org.unicode.cldr.icu.CheckIBMCoverage -s xxx -d yyy en.xml");
        System.exit(-1);
    }

    private void printInfo(String message) {
        if (verbose) {
            System.out.println("INFO : " + message);
        }
    }

    private void printWarning(String fileName, String message) {
        System.err.println(fileName + ": WARNING : " + message);
    }

    private void printError(String fileName, String message) {
        System.err.println(fileName + ": ERROR : " + message);
    }

    public static void main(String[] args) {
        CheckIBMCoverage cov = new CheckIBMCoverage();
        cov.processArgs(args);
    }

    public void processArgs(String[] args) {
        int remainingArgc = 0;
        // for some reason when
        // Class classDefinition = Class.forName(className);
        // object = classDefinition.newInstance();
        // is done then the options are not reset!!
        for (int i = 0; i < options.length; i++) {
            options[i].doesOccur = false;
        }
        try {
            remainingArgc = UOption.parseArgs(args, options);
        } catch (Exception e) {
            printError("", "(parsing args): " + e.toString());
            e.printStackTrace();
            usage();
        }
        if (args.length == 0 || options[HELP1].doesOccur || options[HELP2].doesOccur) {
            usage();
        }

        if (options[SOURCEDIR].doesOccur) {
            sourceDir = options[SOURCEDIR].value;
        }
        if (options[DESTDIR].doesOccur) {
            destDir = options[DESTDIR].value;
        }
        if (options[VERBOSE].doesOccur) {
            verbose = true;
        }
        if (destDir == null) {
            destDir = ".";
        }

        try {
            if (getLocalesMap() != null && getLocalesMap().size() > 0) {
                processMap();
            } else if (remainingArgc > 0) {
                for (int i = 0; i < remainingArgc; i++) {
                    processFile(args[i], verbose);
                }
            } else if (sourceDir != null) {
                FilenameFilter filter = new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        if (name.matches(".*_.*\\.xml")) {
                            return true;
                        }
                        return false;
                    }
                };
                File myDir = new File(sourceDir);
                String[] files = myDir.list(filter);
                if (getLocalesMap() == null) {
                    setLocalesMap(new TreeMap<String, String>());
                }
                for (int i = 0; i < files.length; i++) {
                    getLocalesMap().put(files[i], "");
                }
                processMap();
            } else {
                printError("", "No files specified for processing. Please check the arguments and try again");
                usage();
            }
        } catch (IOException ex) {
            printError("", ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void processMap() throws IOException {
        FileWriter fw = new FileWriter(destDir + File.separator + "IBMCoverage.html");
        fw.write("<html>\n");
        fw.write("\t<body>\n");
        // fw.write("\t\t<br><b><font color=\"FF0000\">Note: Please ignore the POSIX column. The data is not accurate</font></b>\n");
        fw.write("\t\t<table border=\"1\">\n");
        fw.write("\t\t\t<tr>\n");
        fw.write("\t\t\t\t<th>Locale</td>\n");
        fw.write("\t\t\t\t<th>Group</td>\n");
        fw.write("\t\t\t\t<th>Minimum Coverage(POSIX)</td>\n");
        fw.write("\t\t\t\t<th>Minimum Coverage(BASIC)</td>\n");
        fw.write("\t\t\t\t<th>Minimum Coverage(ICU)</td>\n");
        fw.write("\t\t\t</tr>\n");
        int bp = 0, bf = 0, mp = 0, mf = 0;

        for (Iterator<String> iter = getLocalesMap().keySet().iterator(); iter.hasNext();) {
            String fileName = iter.next();
            int index = fileName.indexOf(".");
            String locale = fileName.substring(0, index);
            String glf = locale.concat("_group.log");
            String blf = locale.concat("_basic.log");
            String plf = locale.concat("_posix.log");
            StandardCodes sc = StandardCodes.make();
            String group = sc.getGroup(locale, "IBM");
            if (group == null) {
                printWarning(locale, " not required by IBM whitepaper");
                continue;
            }
            Level level = Level.get(group);

            fw.write("\t\t\t<tr>\n");
            fw.write("\t\t\t\t<td>" + locale + "</td>\n");
            fw.write("\t\t\t\t<td>" + group + "</td>\n");

            // check posix coverage
            int posix = processFile(locale, destDir + File.separator + plf, Level.POSIX, group, true);
            if (posix == 0) {
                fw.write("\t\t\t\t<td><img src=\"blue_check.gif\" border=\"0\" ALT=\"Pass\"></td>\n");
                mp++;
            } else {
                fw.write("\t\t\t\t<td><a href=\"" + plf
                    + "\"><img src=\"red_x.gif\" border=\"0\" ALT=\"Fail\"></a></td>\n");
                mf++;
            }

            int basic = processFile(locale, destDir + File.separator + blf, Level.BASIC, group, true);
            if (basic == 0) {
                fw.write("\t\t\t\t<td><img src=\"blue_check.gif\" border=\"0\" ALT=\"Pass\"></td>\n");
                bp++;
            } else {
                fw.write("\t\t\t\t<td><a href=\"" + blf
                    + "\"><img src=\"red_x.gif\" border=\"0\" ALT=\"Fail\"></a></td>\n");
                bf++;
            }

            if (level.equals(Level.POSIX)) {
                if (posix == 0) {
                    fw.write("\t\t\t\t<td><img src=\"blue_check.gif\" border=\"0\" ALT=\"Pass\"></td>\n");
                } else {
                    fw.write("\t\t\t\t<td><a href=\"" + glf
                        + "\"><img src=\"red_x.gif\" border=\"0\" ALT=\"Fail\"></a></td>\n");
                }
            } else if (level.equals(Level.BASIC)) {
                if (basic == 0) {
                    fw.write("\t\t\t\t<td><img src=\"blue_check.gif\" border=\"0\" ALT=\"Pass\"></td>\n");
                } else {
                    fw.write("\t\t\t\t<td><a href=\"" + glf
                        + "\"><img src=\"red_x.gif\" border=\"0\" ALT=\"Fail\"></a></td>\n");
                }
            } else {
                int out = processFile(locale, destDir + File.separator + glf, level, group, true);

                if (out == 0) {
                    fw.write("\t\t\t\t<td><img src=\"blue_check.gif\" border=\"0\" ALT=\"Pass\"></td>\n");
                } else {
                    fw.write("\t\t\t\t<td><a href=\"" + glf
                        + "\"><img src=\"red_x.gif\" border=\"0\" ALT=\"Fail\"></a></td>\n");
                }
            }
            fw.write("\t\t\t</tr>\n");
            fw.flush();
        }
        fw.write("\t\t</table>\n");

        fw.write("\t\t<br><b><i>Note: </i></b>\n");
        fw.write("\t\t<ol>\n");
        fw.write("\t\t\t<il>POSIX==G4</il>\n");
        fw.write("\t\t\t<il>Basic==G3</il>\n");
        fw.write("\t\t\t<il>Moderate==G2</il>\n");
        fw.write("\t\t\t<il>Modern==G1</il>\n");
        fw.write("\t\t\t<il>Comprehensive==G0</il>\n");
        fw.write("\t\t</ol>\n");
        fw.write("\t\t<br><b>Basic Requirement Passed: " + bp + "</b>\n");
        fw.write("\t\t<br><b>Basic Requirement Failed: " + bf + "</b>\n");
        fw.write("\t\t<br><b>Total: " + (bp + bf) + "</b>\n");
        fw.write("\t\t<br><b>Posix Requirement Passed: " + mp + "</b>\n");
        fw.write("\t\t<br><b>Posix Requirement Failed: " + mf + "</b>\n");
        fw.write("\t\t<br><b>Total: " + (mp + mf) + "</b>\n");
        fw.write("\t</body>\n");
        fw.write("</html>\n");
        fw.flush();
        fw.close();
    }

    static Transliterator prettyPath = CheckCLDR.getTransliteratorFromFile("ID", "prettyPath.txt");

    private int processFile(String fileName, boolean pretty) throws IOException {
        int index = fileName.indexOf(".");
        String locale = fileName.substring(0, index);
        StandardCodes sc = StandardCodes.make();
        String group = sc.getGroup(locale, "IBM");
        if (group == null) {
            printWarning(locale, " not required by IBM whitepaper");
            return -1;
        }
        Level level = Level.get(group);
        int out = processFile(locale, destDir + File.separator + locale + "_posix.log", Level.POSIX, group, pretty);
        out = processFile(locale, destDir + File.separator + locale + "_basic.log", Level.BASIC, group, pretty);
        out = processFile(locale, destDir + File.separator + locale + "_group.log", level, group, pretty);
        return out;
    }

    private static final String RULES = "//ldml/collations/collation[@type=\"standard\"]/rules";
    private static final String COLLATIONS = "//ldml/collations";

    private int processFile(String locale, String logFile, Level level, String group, boolean pretty)
        throws IOException {

        // Map m = new TreeMap();
        FileWriter fw = new FileWriter(logFile);
        int ret = check(locale, group, level, fw);
        if (level.compareTo(Level.POSIX) == 0) {
            Node node = LDMLUtilities.getFullyResolvedLDML(sourceDir + "/../collation", locale, false, true, false,
                false);
            Node collation = LDMLUtilities.getNode(node, COLLATIONS);
            String validSubLocales = LDMLUtilities.getAttributeValue(collation, LDMLConstants.VALID_SUBLOCALE);
            if (validSubLocales == null || !validSubLocales.matches(".*\\b" + locale + ".*\\b")) {
                fw.write(COLLATIONS + " : Found but not valid according to validSublocales attribute");
                fw.write("\n");
            }

            Node rules = LDMLUtilities.getNode(node, RULES);
            if (rules == null) {
                fw.write(RULES + " : Not found. Required for POSIX level coverage");
                fw.write("\n");
            }
        }
        fw.flush();
        fw.close();
        return ret;
    }

    private int check(String locale, String group, Level level, FileWriter fw) throws IOException {

        Factory cldrFactory = Factory.make(sourceDir, "xml");
        CheckCoverage coverage = new CheckCoverage(cldrFactory);
        CLDRFile file = cldrFactory.make(locale, true);
        List<CheckStatus> result = new ArrayList<CheckStatus>();
        Map<String, String> options = new HashMap<String, String>();
        options.put("CoverageLevel.localeType", group);
        options.put("CheckCoverage.requiredLevel", group);
        options.put("submission", "true");
        printInfo("Processing file " + locale);
        coverage.setCldrFileToCheck(file, options, result);
        CLDRFile resolved = coverage.getResolvedCldrFileToCheck();
        Set<String> paths = new TreeSet<String>(resolved.getComparator());
        com.ibm.icu.dev.util.CollectionUtilities.addAll(resolved.iterator(), paths);
        int ret = 0;
        if (level != null) {
            coverage.setRequiredLevel(level);
        }
        for (Iterator<String> it2 = paths.iterator(); it2.hasNext();) {

            String path = (String) it2.next();
            String value = file.getStringValue(path);
            String fullPath = file.getFullXPath(path);
            if (verbose) {
                System.out.println(fullPath);
            }
            result.clear();
            coverage.check(path, fullPath, value, options, result);
            for (Iterator<CheckStatus> it3 = result.iterator(); it3.hasNext();) {
                CheckStatus status = (CheckStatus) it3.next();
                // String statusString = status.toString(); // com.ibm.icu.impl.Utility.escape(
                CheckStatus.Type statusType = status.getType();

                if (statusType.equals(CheckStatus.errorType)) {
                    fw.write(fullPath + " : Untranslated. " + status.getMessage());
                    fw.write("\n");
                    ret = -1;
                    continue;
                }
            }
        }
        return ret;
    }
}