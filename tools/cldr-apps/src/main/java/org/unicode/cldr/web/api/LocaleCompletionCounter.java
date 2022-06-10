package org.unicode.cldr.web.api;

import com.ibm.icu.dev.util.ElapsedTimer;
import java.util.EnumSet;
import java.util.logging.Logger;
import org.unicode.cldr.util.*;
import org.unicode.cldr.web.*;

public class LocaleCompletionCounter {

    private static final Logger logger = SurveyLog.forClass(LocaleCompletionCounter.class);

    private final String localeId;
    private final Level level;
    private final LocaleCompletion.LocaleCompletionResponse lcr;
    private final VettingViewer<Organization> vv;
    private final VettingViewer.DashboardArgs args;
    private final boolean isBaseline;

    public LocaleCompletionCounter(CLDRLocale cldrLocale, STFactory stFactory) {
        this.isBaseline = false;
        localeId = cldrLocale.toString();
        level = StandardCodes.make().getTargetCoverageLevel(localeId);
        lcr = new LocaleCompletion.LocaleCompletionResponse(level);
        final SurveyMain sm = CookieSession.sm;
        vv = new VettingViewer<>(sm.getSupplementalDataInfo(), stFactory, new STUsersChoice(sm));
        final EnumSet<VettingViewer.Choice> set = VettingViewer.getLocaleCompletionCategories();
        args = new VettingViewer.DashboardArgs(set, cldrLocale, level);
        args.setUserAndOrganization(0, VettingViewer.getNeutralOrgForSummary());
        Dashboard.setFiles(args, cldrLocale, stFactory);
    }

    /**
     * Get a baseline count
     */
    public LocaleCompletionCounter(CLDRLocale cldrLocale, Factory factory, boolean isBaseline) {
        this.isBaseline = true;
        localeId = cldrLocale.toString();
        level = StandardCodes.make().getTargetCoverageLevel(localeId);
        lcr = new LocaleCompletion.LocaleCompletionResponse(level);
        final SurveyMain sm = CookieSession.sm;
        vv = new VettingViewer<>(sm.getSupplementalDataInfo(), factory, new STUsersChoice(sm));
        final EnumSet<VettingViewer.Choice> set = VettingViewer.getLocaleCompletionCategories();
        args = new VettingViewer.DashboardArgs(set, cldrLocale, level);
        args.setUserAndOrganization(0, VettingViewer.getNeutralOrgForSummary());
        Dashboard.setFilesForBaseline(args, cldrLocale, factory);
    }

    public LocaleCompletion.LocaleCompletionResponse getResponse() {
        logger.info("Starting " + toString());
        final ElapsedTimer et = new ElapsedTimer("Finishing " + toString());
        VettingViewer<Organization>.LocaleCompletionData lcd = vv.generateLocaleCompletion(args);
        lcr.votes = lcd.localeProgress.getVotedPathCount();
        lcr.total = lcd.localeProgress.getVotablePathCount();
        lcr.error = lcd.errorDebug;
        lcr.missing = lcd.missingDebug;
        lcr.provisional = lcd.provisionalDebug;
        logger.info(et.toString());
        return lcr;
    }

    @Override
    public String toString() {
        return String.format("LocaleCompletion for %s/%s %s",
            localeId,
            level,
            isBaseline ? "(Baseline)":"");
    }
}
