package org.unicode.cldr.web.api;

import com.ibm.icu.dev.util.ElapsedTimer;
import java.util.EnumSet;
import java.util.logging.Logger;
import org.unicode.cldr.util.*;
import org.unicode.cldr.web.*;

public class LocaleCompletionCounter {

    private static final Logger logger = SurveyLog.forClass(LocaleCompletionCounter.class);

    private static final EnumSet<VettingViewer.Choice> choiceSet = EnumSet.of(
        VettingViewer.Choice.error,
        VettingViewer.Choice.hasDispute,
        VettingViewer.Choice.notApproved,
        VettingViewer.Choice.missingCoverage
    );

    private final String localeId;
    private final Level level;
    private final LocaleCompletion.LocaleCompletionResponse lcr;
    private final VettingViewer<Organization> vv;
    private final VettingViewer.DashboardArgs args;

    public LocaleCompletionCounter(CLDRLocale cldrLocale, STFactory stFactory) {
        localeId = cldrLocale.toString();
        level = StandardCodes.make().getTargetCoverageLevel(localeId);
        lcr = new LocaleCompletion.LocaleCompletionResponse(level);
        final SurveyMain sm = CookieSession.sm;
        vv = new VettingViewer<>(sm.getSupplementalDataInfo(), stFactory, new STUsersChoice(sm));
        args = new VettingViewer.DashboardArgs(choiceSet, cldrLocale, level);
        args.setUserAndOrganization(0, VettingViewer.getNeutralOrgForSummary());
        Dashboard.setFiles(args, cldrLocale, stFactory);
    }

    public LocaleCompletion.LocaleCompletionResponse getResponse() {
        logger.info("Starting LocaleCompletion for " + localeId + "/" + level);
        final ElapsedTimer et = new ElapsedTimer("Finishing LocaleCompletion: " + localeId + "/" + level);
        VettingViewer<Organization>.LocaleCompletionData lcd = vv.generateLocaleCompletion(args);
        lcr.votes = lcd.localeProgress.getVotedPathCount();
        lcr.total = lcd.localeProgress.getVotablePathCount();
        lcr.error = lcd.errorDebug;
        lcr.missing = lcd.missingDebug;
        lcr.provisional = lcd.provisionalDebug;
        logger.info(et.toString());
        return lcr;
    }
}
