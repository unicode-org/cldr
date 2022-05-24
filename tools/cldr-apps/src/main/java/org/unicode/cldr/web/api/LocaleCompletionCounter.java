package org.unicode.cldr.web.api;

import com.ibm.icu.dev.util.ElapsedTimer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.test.SubmissionLocales;
import org.unicode.cldr.test.TestCache;
import org.unicode.cldr.util.*;
import org.unicode.cldr.web.STFactory;
import org.unicode.cldr.web.SurveyLog;
import org.unicode.cldr.web.SurveyMain;

public class LocaleCompletionCounter {

    private static final Logger logger = SurveyLog.forClass(LocaleCompletionCounter.class);

    private final String localeId;
    private final Level level;
    private final CheckCLDR.Options options;
    private final TestCache.TestResultBundle checkCldr;
    private final CLDRFile file;
    private final CLDRFile baselineFile;
    private final LocaleCompletion.LocaleCompletionResponse lcr;
    private final CoverageLevel2 covLeveller;
    private final List<CheckCLDR.CheckStatus> results;

    public LocaleCompletionCounter(CLDRLocale cldrLocale, STFactory stFactory) {
        localeId = cldrLocale.toString(); // normalized
        level = StandardCodes.make().getTargetCoverageLevel(localeId);
        options = new CheckCLDR.Options(cldrLocale, SurveyMain.getTestPhase(), level.toString(), null);
        checkCldr = stFactory.getTestResult(cldrLocale, options);

        // we need an XML Source to receive notification.
        // This causes LocaleCompletionHelper.INSTANCE.valueChanged(...) to be called
        // whenever a vote happens.
        final XMLSource mySource = stFactory.makeSource(localeId, false);
        mySource.addListener(LocaleCompletion.LocaleCompletionHelper.INSTANCE);

        file = stFactory.make(localeId, true);
        baselineFile = stFactory.getDiskFile(cldrLocale);
        lcr = new LocaleCompletion.LocaleCompletionResponse(level);
        SupplementalDataInfo sdi = SupplementalDataInfo.getInstance(stFactory.getSupplementalDirectory());
        covLeveller = CoverageLevel2.getInstance(sdi, localeId);
        results = new ArrayList<>();
    }

    public LocaleCompletion.LocaleCompletionResponse getResponse() {
        final ElapsedTimer et = new ElapsedTimer("LocaleCompletion:" + options);
        logger.info("Starting LocaleCompletion for " + options);
        for (final String path : file) {
            handleOnePath(path);
        }
        logger.info(et.toString());
        return lcr;
    }

    private void handleOnePath(String xpath) {
        lcr.allXpaths++;
        final Level pathLevel = covLeveller.getLevel(xpath);
        final String fullPath = file.getFullXPath(xpath);

        final PathHeader ph = LocaleCompletion.LocaleCompletionHelper.INSTANCE.phf.fromPath(xpath);
        PathHeader.SurveyToolStatus surveyToolStatus = ph.getSurveyToolStatus();
        if (
            surveyToolStatus == PathHeader.SurveyToolStatus.DEPRECATED ||
            surveyToolStatus == PathHeader.SurveyToolStatus.HIDE
        ) {
            lcr.ignoredHidden++;
            return; // not visible
        }

        checkCldr.check(xpath, results, file.getStringValue(xpath));

        final boolean hasError = CheckCLDR.CheckStatus.hasError(results);

        final boolean statusMissing =
            STFactory.calculateStatus(file, baselineFile, xpath) == VoteResolver.Status.missing;

        if (statusMissing) {
            lcr.statusMissing++;
        }

        // TODO: copy and paste from CheckCLDR.getShowRowAction - CLDR-15230
        if (
            CheckCLDR.LIMITED_SUBMISSION &&
            !SubmissionLocales.allowEvenIfLimited(localeId, xpath, hasError, statusMissing)
        ) {
            lcr.ignoredLimited++;
            return; // not allowed through by SubmissionLocales.
        }

        // TODO: fix the logic for addMissing -- testing shows addMissing never being called,
        // since hasError is false even for missing items
        // -- consider sharing more code with VettingViewer.handleOnePath
        // Reference: https://unicode-org.atlassian.net/browse/CLDR-15662
        if (hasError) {
            if (results.size() == 1 && results.get(0).getSubtype() == CheckCLDR.CheckStatus.Subtype.coverageLevel) {
                lcr.addMissing();
            } else {
                lcr.addError();
            }
        } else {
            if (pathLevel.getLevel() < level.getLevel()) {
                final CLDRFile.DraftStatus status = CLDRFile.DraftStatus.forXpath(fullPath);
                if (status == CLDRFile.DraftStatus.provisional || status == CLDRFile.DraftStatus.unconfirmed) {
                    lcr.addProvisional();
                } else {
                    lcr.addOk();
                }
            } else {
                // out of coverage level, do not count the path.
                lcr.ignoredOutOfCov++;
            }
        }
    }
}
