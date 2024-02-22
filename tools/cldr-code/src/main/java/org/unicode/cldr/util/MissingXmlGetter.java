package org.unicode.cldr.util;

import com.ibm.icu.impl.Row;
import java.io.*;
import java.util.EnumSet;
import java.util.Map;
import java.util.TreeMap;

public class MissingXmlGetter {

    private static final String VALUE_PLACEHOLDER = "n/a";

    private final Factory factory;
    private final CLDRFile englishFile;
    private final Factory baselineFactory;

    private int userId = 0;
    private Organization usersOrg = null;
    private VettingViewer.UsersChoice<Organization> usersChoice = null;

    public MissingXmlGetter(Factory factory, Factory baselineFactory) {
        this.factory = factory;
        this.baselineFactory = baselineFactory;
        this.englishFile = factory.make("en", true);
    }

    public void setUserInfo(
            int userId,
            Organization usersOrg,
            VettingViewer.UsersChoice<Organization> usersChoice) {
        this.userId = userId;
        this.usersOrg = usersOrg;
        this.usersChoice = usersChoice;
    }

    /**
     * Construct XML for the paths that are error/missing/provisional in a locale, with value
     * placeholders, so that somebody can download and edit the .xml file to fill in the values and
     * then do a bulk submission of that file
     *
     * @param locale the locale
     * @param coverageLevel the coverage level for paths to be included
     * @return the XML as a string
     * @throws IOException for StringWriter
     */
    public String getXml(CLDRLocale locale, Level coverageLevel) throws IOException {
        if (usersOrg == null || usersChoice == null) {
            throw new IllegalArgumentException("usersOrg and usersChoice must be set");
        }
        final CLDRConfig config = CLDRConfig.getInstance();
        final SupplementalDataInfo SDI = config.getSupplementalDataInfo();
        final VettingViewer<Organization> vv = new VettingViewer<>(SDI, factory, usersChoice);
        final EnumSet<NotificationCategory> choiceSet =
                VettingViewer.getDashboardNotificationCategories(usersOrg);
        final VettingParameters args = new VettingParameters(choiceSet, locale, coverageLevel);
        args.setUserAndOrganization(userId, usersOrg);
        args.setFiles(locale, factory, baselineFactory);
        final VettingViewer<Organization>.DashboardData dd = vv.generateDashboard(args);
        return reallyGetXml(locale, dd, args.getSourceFile());
    }

    private String reallyGetXml(
            CLDRLocale locale, VettingViewer<Organization>.DashboardData dd, CLDRFile sourceFile)
            throws IOException {
        final XMLSource xmlSource = new SimpleXMLSource(locale.getBaseName());
        final CLDRFile cldrFile = new CLDRFile(xmlSource);
        populateMissingCldrFile(cldrFile, sourceFile, dd);
        try (StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw)) {
            Map<String, Object> options = new TreeMap<>();
            cldrFile.write(pw, options);
            return sw.toString();
        }
    }

    private void populateMissingCldrFile(
            CLDRFile cldrFile, CLDRFile sourceFile, VettingViewer<Organization>.DashboardData dd) {
        for (Map.Entry<
                        Row.R2<PathHeader.SectionId, PathHeader.PageId>,
                        VettingViewer<Organization>.WritingInfo>
                e : dd.sorted.entrySet()) {
            final VettingViewer<Organization>.WritingInfo wi = e.getValue();
            final String path = wi.codeOutput.getOriginalPath();
            if (wi.problems.contains(NotificationCategory.missingCoverage)) {
                addMissingPath(cldrFile, path);
            } else if (wi.problems.contains(NotificationCategory.error)
                    || wi.problems.contains(NotificationCategory.notApproved)) {
                addPresentPath(cldrFile, sourceFile, path, wi);
            }
        }
    }

    private void addMissingPath(CLDRFile cldrFile, String path) {
        final String comment = "English: " + englishFile.getStringValue(path);
        addPathCommentValue(cldrFile, path, comment, VALUE_PLACEHOLDER);
    }

    private void addPresentPath(
            CLDRFile cldrFile,
            CLDRFile sourceFile,
            String path,
            VettingViewer<Organization>.WritingInfo wi) {
        String value = sourceFile.getStringValue(path);
        if (value == null) {
            value = VALUE_PLACEHOLDER;
        }
        String comment = "";
        // put notApproved (provisional) before error, if both are present
        if (wi.problems.contains(NotificationCategory.notApproved)) {
            comment = NotificationCategory.notApproved.buttonLabel;
        }
        if (wi.problems.contains(NotificationCategory.error)) {
            if (!comment.isEmpty()) {
                comment += "; ";
            }
            comment += NotificationCategory.error.buttonLabel + ": " + wi.subtype + "; ";
            // TODO: convert htmlMessage into plain text for legibility/legality inside xml comment
            comment += wi.htmlMessage;
        }
        comment += "; English: " + englishFile.getStringValue(path);
        addPathCommentValue(cldrFile, path, comment, value);
    }

    private void addPathCommentValue(CLDRFile cldrFile, String path, String comment, String value) {
        cldrFile.add(path, value);
        cldrFile.addComment(path, comment, XPathParts.Comments.CommentType.PREBLOCK);
    }
}
