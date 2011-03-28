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

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCoverage;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.test.OutdatedPaths;
import org.unicode.cldr.util.CLDRFile.Factory;
import org.unicode.cldr.util.CLDRFile.Status;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.dev.test.util.TransliteratorUtilities;
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
public class VettingViewer<T> {

    private static final boolean TESTING = CldrUtility.getProperty("TEST", false);
    private static final boolean SHOW_ALL = CldrUtility.getProperty("SHOW", false);

    public enum Choice {
        /**
         * There is a console-check error
         */
        error('E', "Error", "The Survey Tool detected an error in the winning value."),
        /**
         * My choice is not the winning item
         */
        weLost('L', "Losing", "The value that your organization chose (overall) is either not the winning value, or doesn't have enough votes to be approved."),
        /**
         * There is a dispute.
         */
        hasDispute('D', "Disputed", "There is a dispute between other organizations that needs your help in resolving to the best value."),
        /**
         * There is a console-check warning
         */
        warning('W', "Warning", "The Survey Tool detected a warning about the winning value."),
        /**
         * Given the users coverage, some items are missing.
         */
        missingCoverage('M', "Missing", "Your current coverage level requires the item to be present, but it is missing."),
        /**
         * The value changed from the last version of CLDR
         */
        changedOldValue('N', "New", "The winning value was altered from the CLDR 1.9 value."),
        /**
         * The English value for the path changed AFTER the current value for
         * the locale.
         */
        englishChanged('U', "Unsync’d", "The English value changed at some point in CLDR, but the corresponding value for your language didn’t."),
        /**
         * There is a console-check error
         */
        other('O', "Other", "Everything else."),
        ;

        public final char    abbreviation;
        public final String  buttonLabel;
        public final String  description;
        private final String display;

        Choice(char abbreviation, String buttonLabel, String description) {
            this.abbreviation = abbreviation;
            this.buttonLabel = TransliteratorUtilities.toHTML.transform(buttonLabel);
            this.description = TransliteratorUtilities.toHTML.transform(description);
            this.display = "<span title='" + description + "'>" + buttonLabel + "*</span>";
        }

        public static <T extends Appendable> T appendDisplay(EnumSet<Choice> choices, T target) {
            try {
                boolean first = true;
                for (Choice item : choices) {
                    if (first) {
                        first = false;
                    } else {
                        target.append(", ");
                    }
                    target.append(item.display);
                }
                return target;
            } catch (IOException e) {
                throw new IllegalArgumentException(e); // damn'd checked
                // exceptions
            }
        }

        public static Choice fromString(String i) {
            try {
                return valueOf(i);
            } catch (NullPointerException e) {
                throw e;
            } catch (RuntimeException e) {
                if (i.isEmpty()) {
                    throw e;
                }
                int cp = i.codePointAt(0);
                for (Choice choice : Choice.values()) {
                    if (cp == choice.abbreviation) {
                        return choice;
                    }
                }
                throw e;
            }
        }

        public static Appendable appendRowStyles(EnumSet<Choice> choices, Appendable target) {
            try {
                target.append("hide");
                for (Choice item : choices) {
                    target.append(' ').append("vv").append(Character.toLowerCase(item.abbreviation));
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

    /**
     * The status should be computed in the following way:
     * <ol>
     * <li>If we voted</li>
     * <ol>
     * <li>if we conflicted internally → *disputed*</li>
     * <li>if our choice lost → losing</li>
     * <li>if our choice ≤ provisional → provisionalOrWorse</li>
     * <li>otherwise → *ok*</li>
     * </ol>
     * <li>If we didn't vote*</li>
     * <ol>
     * <li>there's a tie among others → *disputed*</li>
     * <li>at least one other organization voted, and the winning value ≤ provisional → *disputed*</li>
     * <li>otherwise → *ok*</li>
     * </ol>
     * </ol>
     * 
     * @author markdavis
     * 
     */
    public enum VoteStatus {
        /**
         * The value for the path is either contributed or approved, and either
         * the user's organization chose the winning value or didn't vote.
         */
        ok,

        /**
         * The user's organization chose the winning value for the path, but
         * that value is neither contributed nor approved.
         */
        provisionalOrWorse,

        /**
         * The user's organization's choice is not winning. There may be
         * insufficient votes to overcome a previously approved value, or other
         * organizations may be voting against it.
         */
        losing,

        /**
         * There is a dispute, meaning one of the following:
         * <ol>
         * <li>the user's organization voted but its votes cancel, or</li>
         * <li>the user's organization didn't vote, and either
         * <ol>
         * <li>others are disputing the correct value, or</li>
         * <li>there are insufficient votes to do better than provisional.</li>
         * </ol>
         * </ol>
         */
        disputed
    }

    public static interface UsersChoice<T> {
        /**
         * Return the value that the user's organization (as a whole) voted for,
         * or null if none of the users in the organization voted for the path.
         * <br>NOTE: Would be easier if this were a method on CLDRFile.
         * 
         * @param locale
         */
        public String getWinningValueForUsersOrganization(CLDRFile cldrFile, String path, T user);

        /**

         * Return the vote status
         * 
         * @param locale
         */
        public VoteStatus getStatusForUsersOrganization(CLDRFile cldrFile, String path, T user);
    }

    public static interface ErrorChecker {
        enum Status {ok, error, warning}
        /**
         * Initialize an error checker with a cldrFile. MUST be called before any getErrorStatus.
         */
        public Status initErrorStatus(CLDRFile cldrFile);
        /**
         * Return the status, and append the error message to the status message.
         */
        public Status getErrorStatus(String path, String value, StringBuilder statusMessage);
    }

    public static class NoErrorStatus implements ErrorChecker {
        @Override
        public Status initErrorStatus(CLDRFile cldrFile) {
            return Status.ok;
        }

        @Override
        public Status getErrorStatus(String path, String value, StringBuilder statusMessage) {
            return Status.ok;
        }
    }

    public static class DefaultErrorStatus implements ErrorChecker {

        private CheckCLDR checkCldr;
        private HashMap<String, String> options = new HashMap<String, String>();
        private ArrayList<CheckStatus> result = new ArrayList<CheckStatus>();
        private CLDRFile cldrFile;

        @Override
        public Status initErrorStatus(CLDRFile cldrFile) {
            this.cldrFile = cldrFile;
            options = new HashMap<String, String>();
            result = new ArrayList<CheckStatus>();
            checkCldr = CheckCLDR.getCheckAll(".*");
            checkCldr.setCldrFileToCheck(cldrFile, options, result);
            return Status.ok;
        }

        @Override
        public Status getErrorStatus(String path, String value, StringBuilder statusMessage) {
            Status result0 = Status.ok;
            CharSequence errorMessage = null;
            String fullPath = cldrFile.getFullXPath(path);
            checkCldr.check(path, fullPath, value, options, result);
            for (CheckStatus checkStatus : result) {
                final CheckCLDR cause = checkStatus.getCause();
                if (cause instanceof CheckCoverage) {
                    continue;
                }
                String statusType = checkStatus.getType();
                if (statusType.equals(CheckStatus.errorType)) {
                    result0 = Status.error;
                    errorMessage = checkStatus.getMessage();
                    break;
                } else if (statusType.equals(CheckStatus.warningType)) {
                    result0 = Status.warning;
                    errorMessage = checkStatus.getMessage();
                }
            }
            statusMessage.append(errorMessage);
            return result0;
        }
    }

    private final Factory              cldrFactory;
    private final Factory              cldrFactoryOld;
    private final CLDRFile             englishFile;
    private final CLDRFile             oldEnglishFile;
    private final UsersChoice<T>          userVoteStatus;
    private final SupplementalDataInfo supplementalDataInfo;
    private final String lastVersionTitle;
    private final String currentWinningTitle;
    private final PathDescription pathDescription;
    private ErrorChecker errorChecker = new DefaultErrorStatus(); // new NoErrorStatus(); // for testing

    /**
     * Create the Vetting Viewer.
     * @param supplementalDataInfo
     * @param cldrFactory
     * @param cldrFactoryOld
     * @param lastVersionTitle The title of the last released version of CLDR.
     * @param currentWinningTitle The title of the next version of CLDR to be released.
     */
    public VettingViewer(SupplementalDataInfo supplementalDataInfo, Factory cldrFactory, Factory cldrFactoryOld, UsersChoice<T> userVoteStatus, String lastVersionTitle, String currentWinningTitle) {
        super();
        this.cldrFactory = cldrFactory;
        this.cldrFactoryOld = cldrFactoryOld;
        englishFile = cldrFactory.make("en", true);
        oldEnglishFile = cldrFactoryOld.make("en", true);
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
     * Show a table of values, filtering according to the choices here and in the constructor.
     * 
     * @param output
     * @param choices See the class description for more information.
     * @param localeId
     * @param user
     * @param usersLevel
     */
    public void generateHtmlErrorTables(Appendable output, EnumSet<Choice> choices, String localeID, T user, Level usersLevel) {
        generateHtmlErrorTables( output, choices, localeID, user, usersLevel, false);
    }

    private void generateHtmlErrorTables(Appendable output, EnumSet<Choice> choices, String localeID, T user, Level usersLevel, boolean showAll) {

        // first gather the relevant paths
        // each one will be marked with the choice that it triggered.

        CLDRFile sourceFile = cldrFactory.make(localeID, true);
        Matcher altProposed = Pattern.compile("\\[@alt=\"[^\"]*proposed").matcher("");
        EnumSet<Choice> problems = EnumSet.noneOf(Choice.class);

        // Initialize
        CoverageLevel2 coverage = CoverageLevel2.getInstance(supplementalDataInfo, localeID);
        CLDRFile lastSourceFile = null;
        try {
            lastSourceFile = cldrFactoryOld.make(localeID, true);
        } catch (Exception e) {}

        // set the following only where needed.
        Status status = null;
        OutdatedPaths outdatedPaths = null;

        Map<String, String> options = null;
        List<CheckStatus> result = null;

        for (Choice choice : choices) {
            switch (choice) {
            case changedOldValue:
                break;
            case missingCoverage:
                status = new Status();
                break;
            case englishChanged:
                outdatedPaths = new OutdatedPaths();
                break;
            case error:
            case warning:
                errorChecker.initErrorStatus(sourceFile);
                break;
            case weLost:
            case hasDispute:
            case other:
                break;
            default:
                System.out.println(choice + " not implemented yet");
            }
        }

        // now look through the paths

        TreeMap<String, WritingInfo> sorted = new TreeMap<String, WritingInfo>();

        Counter<Choice> problemCounter = new Counter<Choice>();
        StringBuilder testMessage = new StringBuilder();
        StringBuilder statusMessage = new StringBuilder();

        for (String path : sourceFile) {
            progressCallback.nudge(); // Let the user know we're moving along.

            // note that the value might be missing!

            // make sure we only look at the real values
            if (altProposed.reset(path).find()) {
                continue;
            }

            if (path.contains("/exemplarCharacters") || path.contains("/references")) {
                continue;
            }

            Level level = coverage.getLevel(path);

            // skip anything above the requested level
            if (level.compareTo(usersLevel) > 0) {
                continue;
            }

            String value = sourceFile.getWinningValue(path);

            problems.clear();
            testMessage.setLength(0);
            boolean haveError = false;
            VoteStatus voteStatus = null;

            for (Choice choice : choices) {
                switch (choice) {
                case changedOldValue:
                    String oldValue = lastSourceFile == null ? null : lastSourceFile.getWinningValue(path);
                    if (oldValue != null && !oldValue.equals(value)) {
                        problems.add(choice);
                        problemCounter.increment(choice);
                    }
                    break;
                case missingCoverage:
                    if (!localeID.equals("root")) {
                        if (isMissing(sourceFile, path, status)) {
                            problems.add(choice);
                            problemCounter.increment(choice);
                        }
                    }
                    break;
                case englishChanged:
                    if (outdatedPaths.isOutdated(localeID, path)
                            //                            || !CharSequences.equals(englishFile.getWinningValue(path), oldEnglishFile.getWinningValue(path))
                    ) {
                        // the outdated paths compares the base value, before
                        // data submission,
                        // so see if the value changed.
                        String lastValue = lastSourceFile == null ? null : lastSourceFile.getWinningValue(path);
                        if (CharSequences.equals(value, lastValue)) {
                            problems.add(choice);
                            problemCounter.increment(choice);
                        }
                    }
                    break;
                case error:
                case warning:
                    if (haveError) {
                        break;
                    }
                    ErrorChecker.Status errorStatus = errorChecker.getErrorStatus(path, value, statusMessage);
                    if ((choice == Choice.error && errorStatus == ErrorChecker.Status.error)
                            || (choice == Choice.warning && errorStatus == ErrorChecker.Status.warning)) {
                        problems.add(choice);
                        appendToMessage(statusMessage, testMessage);
                        problemCounter.increment(choice);
                        haveError = true;
                        break;
                    }
                    break;
                case weLost:
                    if (voteStatus == null) {
                        voteStatus = userVoteStatus.getStatusForUsersOrganization(sourceFile, path, user);
                    }
                    switch (voteStatus) {
                    case provisionalOrWorse:
                    case losing: 
                        if (choice == Choice.weLost) {
                            problems.add(choice);
                            problemCounter.increment(choice);
                            String usersValue = userVoteStatus.getWinningValueForUsersOrganization(sourceFile, path, user);
                            appendToMessage(usersValue, testMessage);
                        }
                        break;
                    }
                    break;
                case hasDispute:
                    if (voteStatus == null) {
                        voteStatus = userVoteStatus.getStatusForUsersOrganization(sourceFile, path, user);
                    }
                    if (voteStatus == VoteStatus.disputed) {
                        problems.add(choice);
                        problemCounter.increment(choice);
                        String usersValue = userVoteStatus.getWinningValueForUsersOrganization(sourceFile, path, user);
                        if (usersValue != null) {
                            appendToMessage(usersValue, testMessage);
                        }
                    }
                    break;
                }
            }
            if (showAll || !problems.isEmpty()) {
                if (showAll && problems.isEmpty()) {
                    problems.add(Choice.other);
                    problemCounter.increment(Choice.other);
                }
                reasonsToPaths.clear();
                appendToMessage("level:" + level.toString(), testMessage);
                final String description = pathDescription.getDescription(path, value, level, null);
                if (!reasonsToPaths.isEmpty()) {
                    appendToMessage(level + " " + TransliteratorUtilities.toHTML.transform(reasonsToPaths.toString()), testMessage);
                }
                if (description != null && !description.equals("SKIP")) {
                    appendToMessage(TransliteratorUtilities.toHTML.transform(description), testMessage);
                }
                sorted.put(pathTransform.getPrettyPath(path), new WritingInfo(path, problems, testMessage));
            }
        }

        // now write the results out
        writeTables(output, sourceFile, lastSourceFile, sorted, problemCounter, choices, localeID, showAll);
    }

    private boolean isMissing(CLDRFile sourceFile, String path, Status status) {
        if (sourceFile == null) {
            return true;
        }
        String localeFound = sourceFile.getSourceLocaleID(path, status);
        // only count it as missing IF the (localeFound is root or codeFallback) AND the aliasing didn't change the path
        boolean missing = false;
        if (!path.equals(status.pathWhereFound)) {
            if (localeFound.equals("root")
                    || localeFound.equals(XMLSource.CODE_FALLBACK_ID)) {
                missing = true;
            }
        }
        return missing;
    }

    private StringBuilder appendToMessage(CharSequence usersValue, StringBuilder testMessage) {
        if (testMessage.length() != 0) {
            testMessage.append("<br>");
        }
        return testMessage.append(usersValue);
    }

    static final NumberFormat nf = NumberFormat.getIntegerInstance(ULocale.ENGLISH);
    private Relation<String, String> reasonsToPaths;
    private String baseUrl = "http://unicode.org/cldr/apps/survey";
    static {
        nf.setGroupingUsed(true);
    }
    /**
     * Set the base URL, equivalent to 'http://unicode.org/cldr/apps/survey' for generated URLs.
     * @param url
     * @author srl
     */
    public void setBaseUrl(String url) {
        baseUrl = url;
    }
    /**
     * Class that allows the relaying of progress information
     * @author srl
     *
     */
    public static class ProgressCallback {
        /**
         * Note any progress. This will be called before any output is printed.
         * It will be called approximately once per xpath.
         */
        public void nudge() {}
        /**
         * Called when all operations are complete.
         */
        public void done() {}
    }
    
    private ProgressCallback progressCallback = new ProgressCallback(); // null instance by default

    /**
     * Select a new callback. Must be set before running.
     * @return 
     * 
     */
    public VettingViewer<T> setProgressCallback(ProgressCallback newCallback) {
        progressCallback = newCallback;
        return this;
    }

    public ErrorChecker getErrorChecker() {
        return errorChecker;
    }

    /**
     * Select a new error checker. Must be set before running.
     * @return 
     * 
     */
    public VettingViewer<T> setErrorChecker(ErrorChecker errorChecker) {
        this.errorChecker = errorChecker;
        return this;
    }
    
    /**
     * Provide the styles for inclusion into the ST &lt;head&gt; element.
     * @return
     */
    public static String getHeaderStyles() {
        return 
        "<style type='text/css'>\n"
        +".hide {display:none}\n"
        +".vve {}\n"
        +".vvn {}\n"
        +".vvl {}\n"
        +".vvm {}\n"
        +".vvu {}\n"
        +".vvw {}\n"
        +".vvd {}\n"
        +".vvo {}\n"
        +"</style>";
    }

    private void writeTables(Appendable output, CLDRFile sourceFile, CLDRFile lastSourceFile, 
            TreeMap<String, WritingInfo> sorted,
            Counter<Choice> problemCounter, 
            EnumSet<Choice> choices,
            String localeID,
            boolean showAll) {
        try {

            Status status = new Status();

            output.append("<h2>Summary</h2>\n")
            .append("<p>For instructions, see <a target='CLDR-ST-DOCS' href='http://cldr.unicode.org/translation/vetting-view'>Vetting View Instructions</a>.</p>")
            .append("<form name='checkboxes'>\n")
            .append("<table class='tvs-table'>\n")
            .append("<tr class='tvs-tr'>" +
                    "<th class='tv-th'>Count</th>" +
                    "<th class='tv-th'>Abbr.</th>" +
                    "<th class='tv-th'>Description</th>" +
            "</tr>\n");
            for (Choice choice : choices) {
                if (choice == Choice.other && !showAll) {
                    continue;
                }
                long count = problemCounter.get(choice);
                output.append("<tr><td class='tvs-count'>")
                .append(nf.format(count))
                .append("</td>\n\t<td class='tvs-abb'>")
                .append("<input type='checkbox' name='")
                .append(Character.toLowerCase(choice.abbreviation))
                .append("' onclick='setStyles()'/> ")
                .append(choice.display)
                .append("</td>\n\t<td class='tvs-desc'>")
                .append(choice.description)
                .append("</td></tr>\n");
            }
            output.append("</table>\n</form>\n");


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

                output.append("<tr class='");
                Choice.appendRowStyles(choicesForPath, output);
                output.append("'>\n");
                addCell(output, nf.format(++count), null, "tv-num", HTMLType.plain);
                // path
                addCell(output, code, null, "tv-code", HTMLType.plain);
                // English value
                addCell(output, englishFile.getWinningValue(path), null, "tv-eng", HTMLType.plain);
                // value for last version
                final String oldStringValue = lastSourceFile == null ? null : lastSourceFile.getWinningValue(path);
                boolean oldValueMissing = isMissing(lastSourceFile, path, status);

                addCell(output, oldStringValue, null, oldValueMissing ? "tv-miss" : "tv-last", HTMLType.plain);
                // value for last version
                String newWinningValue = sourceFile.getWinningValue(path);
                if (CharSequences.equals(newWinningValue, oldStringValue)) {
                    newWinningValue = "=";
                }
                addCell(output, newWinningValue, null, choicesForPath.contains(Choice.missingCoverage) ? "tv-miss" : "tv-win", HTMLType.plain);
                // Fix?
                // http://unicode.org/cldr/apps/survey?_=az&xpath=%2F%2Fldml%2FlocaleDisplayNames%2Flanguages%2Flanguage%5B%40type%3D%22az%22%5D
                output.append("<td class='tv-fix'><a target='CLDR-ST-ZOOMED' href='"+baseUrl +"?_=")
                .append(localeID)
                .append("&xpath=")
                .append(percentEscape.transform(path))
                .append("'>");
                Choice.appendDisplay(choicesForPath, output)
                .append("</a></td>");

                if (TESTING && !pathInfo.testMessage.isEmpty()) {
                    addCell(output, pathInfo.testMessage, null, "tv-test", HTMLType.markup);
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

    private void addCell(Appendable output, String value, String title, String classValue, HTMLType htmlType) throws IOException {
        output.append("<td class='")
        .append(classValue);
        if (value == null) {
            output.append(" tv-null'><i>missing</i></td>");
        } else {
            if (title != null && !title.equals(value)) {
                output.append("title='").append(TransliteratorUtilities.toHTML.transform(title)).append('\'');
            }
            output
            .append("'>")
            .append(htmlType == HTMLType.markup ? value : TransliteratorUtilities.toHTML.transform(value))
            .append("</td>\n");
        }
    }

    /**
     * Simple example of usage
     * 
     * @param args
     * @throws IOException
     */
    static final String myOutputDir = CldrUtility.GEN_DIRECTORY + "temp/";

    public static void main(String[] args) throws IOException {
        Timer timer = new Timer();
        timer.start();
        final String currentMain = "/Users/markdavis/Documents/workspace/cldr/common/main";
        final String lastMain = "/Users/markdavis/Documents/workspace/cldr-1.7.2/common/main";

        Factory cldrFactory = Factory.make(currentMain, ".*");
        Factory cldrFactoryOld = Factory.make(lastMain, ".*");
        SupplementalDataInfo supplementalDataInfo = SupplementalDataInfo.getInstance(CldrUtility.SUPPLEMENTAL_DIRECTORY);
        CheckCLDR.setDisplayInformation(cldrFactory.make("en", true));

        // FAKE this, because we don't have access to ST data

        UsersChoice<Integer> usersChoice = new UsersChoice<Integer>() {
            // Fake values for now
            public String getWinningValueForUsersOrganization(CLDRFile cldrFile, String path, Integer user) {
                if (path.contains("AFN")) {
                    return "dummy ‘losing’ value";
                }
                return null; // assume we didn't vote on anything else.
            }
            // Fake values for now
            public VoteStatus getStatusForUsersOrganization(CLDRFile cldrFile, String path, Integer user) {
                String usersValue = getWinningValueForUsersOrganization(cldrFile, path, user);
                String winningValue = cldrFile.getWinningValue(path);
                if (CharSequences.equals(usersValue, winningValue)) {
                    return VoteStatus.ok;
                }
                String fullPath = cldrFile.getFullXPath(path);
                if (fullPath.contains("AMD") || fullPath.contains("unconfirmed") || fullPath.contains("provisional")) {
                    return VoteStatus.provisionalOrWorse;
                }
                if (fullPath.contains("AED")) {
                    return VoteStatus.disputed;
                }
                return VoteStatus.ok;
            }
        };

        // create the tableView and set the options desired.
        // The Options should come from a GUI; from each you can get a long
        // description and a button label.
        // Assuming user can be identified by an int
        VettingViewer<Integer> tableView = new VettingViewer<Integer>(supplementalDataInfo, cldrFactory, cldrFactoryOld, usersChoice, "CLDR 1.7.2", "Winning 1.9");

        // here are per-view parameters

        final EnumSet<Choice> choiceSet = EnumSet.allOf(Choice.class);
        String localeStringID = "fr";
        int userNumericID = 666;
        Level usersLevel = Level.MODERN;
        System.out.println(timer.getDuration() / 10000000000.0 + " secs");

        timer.start();
        FileUtilities.copyFile(VettingViewer.class, "vettingView.css", myOutputDir);
        FileUtilities.copyFile(VettingViewer.class, "vettingView.js", myOutputDir);
        writeFile(tableView, choiceSet, "", localeStringID, userNumericID, usersLevel);
        System.out.println(timer.getDuration() / 10000000000.0 + " secs");

        // check that the choices work.
        for (Choice choice : choiceSet) {
            timer.start();
            writeFile(tableView, EnumSet.of(choice), "-" + choice.abbreviation, localeStringID, userNumericID, usersLevel);
            System.out.println(timer.getDuration() / 10000000000.0 + " secs");
        }
    }

    private static void writeFile(VettingViewer<Integer> tableView, final EnumSet<Choice> choiceSet, String name, String localeStringID, int userNumericID, Level usersLevel)
    throws IOException {
        // open up a file, and output some of the styles to control the table
        // appearance

        PrintWriter out = BagFormatter.openUTF8Writer(myOutputDir, "vettingView" + name + ".html");
        FileUtilities.appendFile(VettingViewer.class, "vettingViewerHead.txt", out);
        out.append(getHeaderStyles());
        out.append("</head><body>\n");
        

        //                + "<style type='text/css'>\n"
        //                + "table.tv-table, table.tvs-table {\n"
        //                + "    border-collapse:collapse;\n"
        //                + "}\n"
        //                + "table.tv-table, th.tv-th, tr.tv-tr, td.tv-num, td.tv-code, td.tv-eng, td.tv-last, td.tv-win, td.tv-fix, table.tvs-table, tr.tvs-tr, td.tvs-count, td.tvs-abb, td.tvs-desc {\n"
        //                + "    border:1px solid gray;\n"
        //                + "}\n"
        //                + "td.tv-num, td.tv-code, td.tv-eng, td.tv-last, td.tv-fix, td.tvs-count, td.tvs-abb, td.tvs-desc {\n"
        //                + "    background-color: #FFFFCC;\n"
        //                + "}\n"
        //                + "th.tv-th, th.tvs-th {\n"
        //                + "    background-color: #DDDDDD;\n"
        //                + "}\n"
        //                + "td.tv-win {\n"
        //                + "    background-color: #CCFFCC;\n"
        //                + "}\n"
        //                + "td.tv-num {\n"
        //                + "    text-align: right;\n"
        //                + "}\n"
        //                + "td.tv-miss, td.tv-null {\n"
        //                + "    background-color: #FFCCCC;\n"
        //                + "}\n"
        //                + "</style>\n"
        out.println("<p>Note: this is just a sample run. The user, locale, user's coverage level, and choices of tests will change the output. In a real ST page using these, the first three would "
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
                + "<p>Notes: This is a static version, using old values (1.7.2) and faked values (L) just for testing."
                + (TESTING ? "Also, the white cell after the Fix column is just for testing." : "")
                + "</p><hr>\n");

        // now generate the table with the desired options
        // The options should come from a GUI; from each you can get a long
        // description and a button label.
        // Assuming user can be identified by an int

        tableView.generateHtmlErrorTables(out, choiceSet, localeStringID, userNumericID, usersLevel, SHOW_ALL);
        out.println("</body>\n</html>\n");
        out.close();
    }

}
