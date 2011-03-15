package org.unicode.cldr.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCoverage;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.test.OutdatedPaths;
import org.unicode.cldr.tool.GenerateXMB;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.VettingViewer.HTMLType;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

/**
 * Provides a HTML tables showing the important issues for vetters to review for
 * a given locale. See the main for an example. Most elements have CSS styles,
 * allowing for customization of the display.
 * 
 * @author markdavis
 */
public class VettingViewer {

    private static final boolean TESTING = true;
    static RegexLookup<String> pathHandling = new RegexLookup<String>().loadFromFile(GenerateXMB.class, "xmbHandling.txt");

    enum Choice {
        /**
         * There is a console-check error
         */
        error('E', "Detected Error", "The Survey Tool detected an error in the winning value."),
        /**
         * The value changed from the last version of CLDR
         */
        changedOldValue('O', "Changed from CLDR 1.9", "The value in the last version of CLDR was changed."),
        /**
         * My choice is not the winning item
         */
        weLost('L', "My organization's choice didn’t win", "The value that your organization chose (overall) is not the winning value."),
        /**
         * The English value for the path changed AFTER the current value for
         * the locale.
         */
        missingCoverage('M', "Missing from Coverage", "Your current coverage level requires the item to be present, but it is missing."),
        /**
         * The English value for the path changed AFTER the current value for
         * the locale.
         */
        englishChanged('C', "English Changed", "The English value changed at some point in CLDR, but the corresponding value for your language didn’t."),
        /**
         * There is a console-check error
         */
        warning('W', "Detected Warning", "The Survey Tool detected a warning about the winning value."),
        ;

        public final char    abbreviation;
        public final String  buttonLabel;
        public final String  description;
        private final String display;

        Choice(char abbreviation, String buttonLabel, String description) {
            this.abbreviation = abbreviation;
            this.buttonLabel = TransliteratorUtilities.toHTML.transform(buttonLabel);
            this.description = TransliteratorUtilities.toHTML.transform(description);
            this.display = "<span title='" + description + "'>" + abbreviation + "</span>";
        }

        public static <T extends Appendable> T appendDisplay(EnumSet<Choice> choices, T target) {
            try {
                for (Choice item : choices) {
                    target.append(item.display);
                }
                return target;
            } catch (IOException e) {
                throw new IllegalArgumentException(e); // damn'd checked
                // exceptions
            }
        }
    }

    static private PrettyPath                      pathTransform         = new PrettyPath();
    static Pattern                                 breaks                = Pattern.compile("\\|");

    private static final UnicodeSet                NEEDS_PERCENT_ESCAPED = new UnicodeSet("[[\\u0000-\\u009F]-[a-zA-z0-9]]");
    private static final Transform<String, String> percentEscape         = new Transform<String, String>() {
        @Override
        public String transform(String source) {
            StringBuilder buffer = new StringBuilder();
            buffer.setLength(0);
            for (int cp : CharSequences.codePoints(source)) {
                if (NEEDS_PERCENT_ESCAPED.contains(cp)) {
                    buffer.append('%').append(Utility.hex(cp, 2));
                } else {
                    buffer.appendCodePoint(cp);
                }
            }
            return buffer.toString();
        }
    };

    public static interface UsersChoice {
        /**
         * Return the value that the user's organization (as a whole) voted for,
         * or null if none of the users in the organization voted for the path.
         * <br>NOTE: Would be easier if this were a method on CLDRFile.
         * 
         * @param locale
         */
        public String getWinningValueForUsersOrganization(CLDRFile cldrFile, String path, int user);
    }

    private final Factory              cldrFactory;
    private final Factory              cldrFactoryOld;
    private final CLDRFile             englishFile;
    private final UsersChoice          userVoteStatus;
    private final SupplementalDataInfo supplementalDataInfo;
    private final String lastVersionTitle;
    private final String currentWinningTitle;
    private final PathDescription pathDescription;

    /**
     * @param supplementalDataInfo
     * @param cldrFactory
     * @param cldrFactoryOld
     * @param lastVersionTitle 
     * @param currentWinningTitle 
     */
    public VettingViewer(SupplementalDataInfo supplementalDataInfo, Factory cldrFactory, Factory cldrFactoryOld, UsersChoice userVoteStatus, String lastVersionTitle, String currentWinningTitle) {
        super();
        this.cldrFactory = cldrFactory;
        this.cldrFactoryOld = cldrFactoryOld;
        englishFile = cldrFactory.make("en", true);
        this.userVoteStatus = userVoteStatus;
        this.supplementalDataInfo = supplementalDataInfo;
        this.lastVersionTitle = lastVersionTitle;
        this.currentWinningTitle = currentWinningTitle;
        Map<String, List<Set<String>>> starredPaths = new HashMap();
        Map<String, String> extras = new HashMap();
        reasonsToPaths = new Relation(new HashMap<String,Set<String>>(), HashSet.class);
        this.pathDescription = new PathDescription(supplementalDataInfo, englishFile, extras, starredPaths, PathDescription.ErrorHandling.CONTINUE);
    }

    static class WritingInfo {
        final String path;
        final EnumSet<Choice> problems;
        final String testMessage;

        public WritingInfo(String path, EnumSet<Choice> problems, CharSequence testMessage) {
            super();
            this.path = path;
            this.problems = problems.clone();
            this.testMessage = testMessage.toString();
        }
    }

    /**
     * Show a table of values, filtering according to the choices.
     * 
     * @param output
     * @param choices
     * @param moderate
     * @param i
     * @param string
     */
    void generateHtmlErrorTables(Appendable output, EnumSet<Choice> choices, String localeID, int user, Level usersLevel) {

        // first gather the relevant paths
        // each one will be marked with the choice that it triggered.

        CLDRFile sourceFile = cldrFactory.make(localeID, true);
        Matcher altProposed = Pattern.compile("\\[@alt=\"[^\"]*proposed").matcher("");
        EnumSet<Choice> problems = EnumSet.noneOf(Choice.class);

        // Initialize
        CoverageLevel2 coverage = null;
        Status status = null;
        OutdatedPaths outdatedPaths = null;
        CheckCLDR checkCldr = null;
        CLDRFile lastSourceFile = null;
        Map<String, String> options = null;
        List<CheckStatus> result = null;

        for (Choice choice : choices) {
            switch (choice) {
            case changedOldValue:
                lastSourceFile = cldrFactoryOld.make(localeID, true);
                break;
            case missingCoverage:
                coverage = CoverageLevel2.getInstance(supplementalDataInfo, localeID);
                status = new Status();
                break;
            case englishChanged:
                outdatedPaths = new OutdatedPaths();
                break;
            case error:
            case warning:
                checkCldr = CheckCLDR.getCheckAll(".*");
                options = new HashMap<String, String>();
                result = new ArrayList<CheckStatus>();
                checkCldr.setCldrFileToCheck(sourceFile, options, result);
                break;
            case weLost:
                break;
            default:
                System.out.println(choice + " not implemented yet");
            }
        }

        // now look through the paths

        TreeMap<String, WritingInfo> sorted = new TreeMap<String, WritingInfo>();

        Counter<Choice> problemCounter = new Counter<Choice>();
        StringBuilder testMessage = new StringBuilder();

        for (String path : sourceFile) {
            // note that the value might be missing!
            
            // make sure we only look at the real values
            if (altProposed.reset(path).find()) {
                continue;
            }
            
            String value = sourceFile.getWinningValue(path);
            Level level = coverage.getLevel(path);

            problems.clear();
            testMessage.setLength(0);
            
            for (Choice choice : choices) {
                switch (choice) {
                case changedOldValue:
                    String oldValue = lastSourceFile.getWinningValue(path);
                    if (oldValue != null && !oldValue.equals(value)) {
                        problems.add(choice);
                        problemCounter.increment(choice);
                    }
                    break;
                case missingCoverage:
                    // skip anything above the requested level
                    if (level.compareTo(usersLevel) > 0) {
                        continue;
                    }
                    if (!localeID.equals("root")) {
                        String localeFound = sourceFile.getSourceLocaleID(path, status);
                        // only count it as missing IF the (localeFound is root or codeFallback) AND the aliasing didn't change the path
                        if (!path.equals(status.pathWhereFound)) {
                            if (localeFound.equals("root")
                                    || localeFound.equals(XMLSource.CODE_FALLBACK_ID)) {
                                problems.add(choice);
                                problemCounter.increment(choice);
                            }
                        }
                    }
                    break;
                case englishChanged:
                    if (outdatedPaths.isOutdated(localeID, path)) {
                        // the outdated paths compares the base value, before
                        // data submission,
                        // so see if the value changed.
                        String lastValue = lastSourceFile.getWinningValue(path);
                        if (CharSequences.equals(value, lastValue)) {
                            problems.add(choice);
                            problemCounter.increment(choice);
                        }
                    }
                    break;
                case error:
                case warning:
                    String fullPath = sourceFile.getFullXPath(path);
                    checkCldr.check(path, fullPath, value, options, result);
                    boolean haveItem = false;
                    for (CheckStatus checkStatus : result) {
                        final CheckCLDR cause = checkStatus.getCause();
                        if (cause instanceof CheckCoverage) {
                            continue;
                        }
                        String statusType = checkStatus.getType();
                        if ((choice == Choice.error && statusType.equals(CheckStatus.errorType))
                                || (choice == Choice.warning && statusType.equals(CheckStatus.warningType))) {
                            problems.add(choice);
                            appendToMessage(checkStatus.getMessage(), testMessage);
                            haveItem = true;
                            break;
                        }
                    }
                    if (haveItem) {
                        problemCounter.increment(choice);
                    }
                    break;
                case weLost:
                    String usersValue = userVoteStatus.getWinningValueForUsersOrganization(sourceFile, path, user);
                    if (usersValue != null && !usersValue.equals(value)) {
                        problems.add(choice);
                        problemCounter.increment(choice);
                        appendToMessage(usersValue, testMessage);
                    }
                }
            }
            if (!problems.isEmpty()) {
                reasonsToPaths.clear();
                final String description = pathDescription.getDescription(path, value, level, null);
                if (!reasonsToPaths.isEmpty()) {
                    appendToMessage(level + " " + TransliteratorUtilities.toHTML.transform(reasonsToPaths.toString()), testMessage);
                }
                if (description != null) {
                    appendToMessage(TransliteratorUtilities.toHTML.transform(description), testMessage);
                }
                sorted.put(pathTransform.getPrettyPath(path), new WritingInfo(path, problems, testMessage));
            }
        }

        // now write the results out
        writeTables(output, sourceFile, lastSourceFile, sorted, problemCounter, choices, localeID);
    }

    private StringBuilder appendToMessage(String usersValue, StringBuilder testMessage) {
        if (testMessage.length() != 0) {
            testMessage.append("<br>");
        }
        return testMessage.append(usersValue);
    }

    static final NumberFormat nf = NumberFormat.getIntegerInstance(ULocale.ENGLISH);
    private Relation<String, String> reasonsToPaths;
    static {
        nf.setGroupingUsed(true);
    }

    private void writeTables(Appendable output, CLDRFile sourceFile, CLDRFile lastSourceFile, 
            TreeMap<String, WritingInfo> sorted,
            Counter<Choice> problemCounter, 
            EnumSet<Choice> choices,
            String localeID) {
        try {

            output.append("<h2>Summary</h2>\n");
            output.append("<table class='tvs-table'>\n");
            output.append("<tr class='tvs-tr'>" +
                    "<th class='tv-th'>Count</th>" +
                    "<th class='tv-th'>Abbr.</th>" +
                    "<th class='tv-th'>Description</th>" +
            "</tr>\n");
            for (Choice choice : choices) {
                long count = problemCounter.get(choice);
                output.append("<tr><td class='tvs-count'>")
                .append(nf.format(count))
                .append("</td><td class='tvs-abb'>")
                .append(choice.abbreviation)
                .append("</td><td class='tvs-desc'>")
                .append(choice.description)
                .append("</td></tr>");
            }
            output.append("</table>\n");

            int count = 0;
            String lastSection = "";
            String lastSubsection = "";
            for (Entry<String, WritingInfo> entry : sorted.entrySet()) {
                String pretty = pathTransform.getOutputForm(entry.getKey());
                String[] pathParts = breaks.split(pretty);
                String section = pathParts.length == 3 ? pathParts[0] : "Unknown";
                String subsection = pathParts.length == 3 ? pathParts[1] : "Unknown";
                String code = pathParts.length == 3 ? pathParts[2] : pretty;
                if (!lastSection.equals(section) || !lastSubsection.equals(subsection)) {
                    if (!lastSection.isEmpty()) {
                        output.append("</table>\n");
                    }
                    output.append("\n<h2 class='tv-s'>Section: ")
                    .append(section)
                    .append(" — <i>Subsection: ")
                    .append(subsection)
                    .append("</i></h3>\n");
                    startTable(output);
                    lastSection = section;
                    lastSubsection = subsection;
                }
                WritingInfo pathInfo = entry.getValue();
                String path = pathInfo.path;
                EnumSet<Choice> choicesForPath = pathInfo.problems;

                output.append("<tr class='tv-tr'>\n");
                addCell(output, nf.format(++count), "tv-num", HTMLType.plain);
                // path
                addCell(output, code, "tv-code", HTMLType.plain);
                // English value
                addCell(output, englishFile.getStringValue(path), "tv-eng", HTMLType.plain);
                // value for last version
                addCell(output, lastSourceFile.getStringValue(path), "tv-last", HTMLType.plain);
                // value for last version
                addCell(output, sourceFile.getWinningValue(path), choicesForPath.contains(Choice.missingCoverage) ? "tv-miss" : "tv-win", HTMLType.plain);
                // Fix?
                // http://unicode.org/cldr/apps/survey?_=az&xpath=%2F%2Fldml%2FlocaleDisplayNames%2Flanguages%2Flanguage%5B%40type%3D%22az%22%5D
                output.append("<td class='tv-fix'><a href='http://unicode.org/cldr/apps/survey?_=")
                .append(localeID)
                .append("&xpath=")
                .append(percentEscape.transform(path))
                .append("'>");
                Choice.appendDisplay(choicesForPath, output)
                .append("</a></td>");
                
                if (!pathInfo.testMessage.isEmpty()) {
                    addCell(output, pathInfo.testMessage, "tv-test", HTMLType.markup);
                }
                output.append("</tr>\n");
            }
            output.append("</table>\n");
        } catch (IOException e) {
            throw new IllegalArgumentException(e); // damn'ed checked exceptions
        }
    }

    private void startTable(Appendable output) throws IOException {
        output.append("<table class='tv-table'>\n");
        output.append("<tr>" +
                "<th class='tv-th'>No.</th>" +
                "<th class='tv-th'>Code</th>" +
                "<th class='tv-th'>English</th>" +
                "<th class='tv-th'>" + lastVersionTitle + "</th>" +
                "<th class='tv-th'>" + currentWinningTitle + "</th>" +
                "<th class='tv-th'>Fix?</th>" +
        "</tr>\n");
    }

    enum HTMLType {plain, markup}
    
    private void addCell(Appendable output, String value, String classValue, HTMLType htmlType) throws IOException {
        output.append("<td class='")
        .append(classValue);
        if (value == null) {
            output.append(" tv-null'><i>missing</i></td>");
        } else {
            output
            .append("'>")
            .append(htmlType == HTMLType.markup ? value : TransliteratorUtilities.toHTML.transform(value))
            .append("</td>");
        }
    }

    /**
     * Simple example of usage
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        Timer timer = new Timer();
        final String currentMain = "/Users/markdavis/Documents/workspace/cldr/common/main";
        final String lastMain = "/Users/markdavis/Documents/workspace/cldr-1.7.2/common/main";

        Factory cldrFactory = Factory.make(currentMain, ".*");
        Factory cldrFactoryOld = Factory.make(lastMain, ".*");
        SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo.getInstance(CldrUtility.SUPPLEMENTAL_DIRECTORY);
        CheckCLDR.setDisplayInformation(cldrFactory.make("en", true));

        // fake this, because we don't have access to ST data
        UsersChoice usersChoice = new UsersChoice() {
            public String getWinningValueForUsersOrganization(CLDRFile cldrFile, String path, int user) {
                if (path.contains("\"en")) {
                    return "dummy ‘losing’ value";
                }
                return null; // assume we didn't vote on anything else.
            }
        };

        // create the tableView and set the options desired.
        // The Options should come from a GUI; from each you can get a long
        // description and a button label.
        // Assuming user can be identified by an int
        VettingViewer tableView = new VettingViewer(supplementalDataInfo, cldrFactory, cldrFactoryOld, usersChoice, "CLDR 1.7.2", "Winning 1.9");

        // here are per-view parameters

        EnumSet<Choice> choiceSet = EnumSet.allOf(Choice.class);
        String localeStringID = "de";
        int userNumericID = 666;
        Level usersLevel = Level.MODERATE;

        // open up a file, and output some of the styles to control the table
        // appearance

        PrintWriter out = BagFormatter.openUTF8Writer(CldrUtility.GEN_DIRECTORY + "temp", "vettingView.html");
        out.println("<html>\n"
                + "<meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />\n"
                + "<base target='_new'>\n"
                + "<style type='text/css'>\n"
                + "table.tv-table, table.tvs-table {\n"
                + "    border-collapse:collapse;\n"
                + "}\n"
                + "table.tv-table, th.tv-th, tr.tv-tr, td.tv-num, td.tv-code, td.tv-eng, td.tv-last, td.tv-win, td.tv-fix, table.tvs-table, tr.tvs-tr, td.tvs-count, td.tvs-abb, td.tvs-desc {\n"
                + "    border:1px solid gray;\n"
                + "}\n"
                + "td.tv-num, td.tv-code, td.tv-eng, td.tv-last, td.tv-fix, td.tvs-count, td.tvs-abb, td.tvs-desc {\n"
                + "    background-color: #FFFFCC;\n"
                + "}\n"
                + "th.tv-th, th.tvs-th {\n"
                + "    background-color: #DDDDDD;\n"
                + "}\n"
                + "td.tv-win {\n"
                + "    background-color: #CCFFCC;\n"
                + "}\n"
                + "td.tv-num {\n"
                + "    text-align: right;\n"
                + "}\n"
                + "td.tv-miss, td.tv-null {\n"
                + "    background-color: #FFCCCC;\n"
                + "}\n"
                + "</style>\n"
                + "<body><p>Note: this is just a sample run. The user, locale, user's coverage level, and choices of tests will change the output. In a real ST page using these, the first three would "
                + "come from context, and the choices of tests would be set with radio buttons. Demo settings are: </p>\n<ol>"
                + "<li>choices: "
                + choiceSet
                + "</li><li>localeStringID: "
                + localeStringID
                + "</li><li>userNumericID: "
                + userNumericID
                + "</li><li>usersLevel: "
                + usersLevel
                + "</ol>"
                + "<p>Notes: The L value is just a hack, because that information is only available in the ST. " +
                        "I'm comparing 1.7.2 against trunk, for the same reason. Also, the white cell after the Fix column is just for testing.</p><hr>\n");

        // now generate the table with the desired options
        // The options should come from a GUI; from each you can get a long
        // description and a button label.
        // Assuming user can be identified by an int

        tableView.generateHtmlErrorTables(out, choiceSet, localeStringID, userNumericID, usersLevel);
        out.println("</body></html>");
        out.close();
        System.out.println(timer.getDuration() / 10000000000.0 + " secs");
        /**
         * function changeStyle(selectorText) { var theRules = new Array(); if
         * (document.styleSheets[0].cssRules) { theRules =
         * document.styleSheets[0].cssRules; } else if
         * (document.styleSheets[0].rules) { theRules =
         * document.styleSheets[0].rules; } for (n in theRules) { if
         * (theRules[n].selectorText == selectorText) { theRules[n].style.color
         * = 'blue'; } } }
         */
    }
}
