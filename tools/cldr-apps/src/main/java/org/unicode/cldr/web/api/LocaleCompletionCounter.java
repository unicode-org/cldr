package org.unicode.cldr.web.api;

import java.util.EnumSet;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import org.unicode.cldr.icu.dev.util.ElapsedTimer;
import org.unicode.cldr.util.*;
import org.unicode.cldr.web.*;

public class LocaleCompletionCounter {

    private static final Logger logger = SurveyLog.forClass(LocaleCompletionCounter.class);

    private final CLDRLocale cldrLocale;
    private final String localeId;
    private final Level level;
    private final VettingViewer<Organization> vv;
    private final VettingParameters args;
    private final boolean isBaseline;

    public LocaleCompletionCounter(CLDRLocale cldrLocale, Factory factory) {
        this(cldrLocale, factory, false);
    }

    public LocaleCompletionCounter(CLDRLocale cldrLocale, Factory factory, boolean isBaseline) {
        this.isBaseline = isBaseline;
        this.cldrLocale = cldrLocale;
        localeId = cldrLocale.toString();
        level = StandardCodes.make().getTargetCoverageLevel(localeId);
        final SurveyMain sm = CookieSession.sm;
        final VettingViewer.UsersChoice<Organization> userVoteStatus =
                isBaseline ? new VotelessUsersChoice() : new STUsersChoice(sm);
        vv = new VettingViewer<>(sm.getSupplementalDataInfo(), factory, userVoteStatus);
        final EnumSet<NotificationCategory> set = VettingViewer.getLocaleCompletionCategories();
        args = new VettingParameters(set, cldrLocale, level);
        args.setUserAndOrganization(0, VettingViewer.getNeutralOrgForSummary());
        if (isBaseline) {
            args.setFilesForBaseline(cldrLocale, factory);
        } else {
            args.setFiles(cldrLocale, factory, sm.getDiskFactory());
        }
    }

    public LocaleCompletion.LocaleCompletionResponse getResponse() throws ExecutionException {
        final String desc = description();
        logger.info("Starting " + desc);
        final ElapsedTimer et = new ElapsedTimer("Finishing " + desc);
        final LocaleCompletionData lcd = vv.generateLocaleCompletion(args);
        final LocaleCompletion.LocaleCompletionResponse lcr =
                new LocaleCompletion.LocaleCompletionResponse(level, lcd);
        if (!isBaseline) {
            lcr.setBaselineCount(LocaleCompletion.getBaselineCount(cldrLocale));
        }
        logger.info(et.toString());
        return lcr;
    }

    private String description() {
        return String.format(
                "LocaleCompletion for %s/%s %s", localeId, level, isBaseline ? "(Baseline)" : "");
    }
}
