package org.unicode.cldr.util;

import com.ibm.icu.impl.Row;
import java.io.*;
import java.util.EnumSet;
import java.util.Map;
import java.util.TreeMap;

public class MissingXmlGetter {

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

    public void setUserInfo(int userId, Organization usersOrg, VettingViewer.UsersChoice<Organization> usersChoice) {
        this.userId = userId;
        this.usersOrg = usersOrg;
        this.usersChoice = usersChoice;
    }

    /**
     * Construct XML for the paths that are error/missing/provisional in a locale, with value placeholders,
     * so that somebody can download and edit the .xml file to fill in the values and then do a bulk
     * submission of that file
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
        final EnumSet<NotificationCategory> choiceSet = VettingViewer.getDashboardNotificationCategories(usersOrg);
        final VettingParameters args = new VettingParameters(choiceSet, locale, coverageLevel);
        args.setUserAndOrganization(userId, usersOrg);
        args.setFiles(locale, factory, baselineFactory);
        final VettingViewer<Organization>.DashboardData dd = vv.generateDashboard(args);
        return reallyGetXml(locale, dd);
    }

    private String reallyGetXml(CLDRLocale locale, VettingViewer<Organization>.DashboardData dd) throws IOException {
        final XMLSource source = new SimpleXMLSource(locale.getBaseName());
        final CLDRFile cldrFile = new CLDRFile(source);
        populateMissingCldrFile(cldrFile, dd);
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            Map<String, Object> options = new TreeMap<>();
            cldrFile.write(pw, options);
            return sw.toString();
        }
    }

    private void populateMissingCldrFile(CLDRFile cldrFile, VettingViewer<Organization>.DashboardData dd) {
        for (Map.Entry<Row.R2<PathHeader.SectionId, PathHeader.PageId>, VettingViewer<Organization>.WritingInfo> e : dd.sorted.entrySet()) {
            final VettingViewer<Organization>.WritingInfo wi = e.getValue();
            final String path = wi.codeOutput.getOriginalPath();
            for (NotificationCategory cat : wi.problems) {
                if (
                    cat == NotificationCategory.error ||
                    cat == NotificationCategory.missingCoverage ||
                    cat == NotificationCategory.notApproved
                ) {
                    addPath(cldrFile, path, wi, cat);
                }
            }
        }
    }

    private void addPath(
        CLDRFile cldrFile,
        String path,
        VettingViewer<Organization>.WritingInfo wi,
        NotificationCategory cat
    ) {
        cldrFile.add(path, "n/a");
        // most common category is missingCoverage; only show category label for the others
        String comment = (cat == NotificationCategory.missingCoverage) ? "" : cat.buttonLabel + "; ";
        if (cat == NotificationCategory.error) {
            comment += " " + wi.subtype + "; ";
            // TODO: convert htmlMessage into plain text for legibility/legality inside xml comment
            comment += " " + wi.htmlMessage + "; ";
        }
        comment += "English: " + englishFile.getStringValue(path);
        cldrFile.addComment(path, comment, XPathParts.Comments.CommentType.PREBLOCK);
    }
}
