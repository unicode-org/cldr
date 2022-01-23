package org.unicode.cldr.web.api;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.ibm.icu.dev.util.ElapsedTimer;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.test.SubmissionLocales;
import org.unicode.cldr.test.TestCache.TestResultBundle;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.SurveyToolStatus;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.STFactory;
import org.unicode.cldr.web.SurveyLog;
import org.unicode.cldr.web.SurveyMain;

/**
 * "A locale has complete coverage when there are no Missing values, no Provisional values, and no Errors
 * (aka no MEPs). The Missing / Provisional values are determined at the Locale.txt coverage levels,
 * while Errors need to be counted at the comprehensive level (because we have to resolve all of them in resolution).
 *
 *
 * "In order to show progress towards completion of the locale, we compare the current status to that of
 *  the corresponding baseline (blue star) values.
 * Those baseline values can be computed and cached the first time they are needed, so that there is
 *  only a 1 time cost rather than a constant cost. But if that is done, the values must be cleared at
 *  each push to production, so that they are recomputed afterwards. That is because sometimes we change/add
 *  the baseline values when we push to production.
 */
@Path("/completion")
@Tag(name = "completion", description = "APIs for voting completion statistics")
public class LocaleCompletion {
    private static final Logger logger = SurveyLog.forClass(LocaleCompletion.class);

    @GET
    @Path("/locale/{locale}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get locale completion statistics",
        description = "Get locale completion statistics for the given locale")
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "200",
                description = "Voting completion statistics for the requesting user and the given locale and coverage level",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = LocaleCompletionResponse.class))),
            @APIResponse(
                responseCode = "404",
                description = "Locale not found"),
            @APIResponse(
                responseCode = "503",
                description = "Not ready yet"),
            @APIResponse(
                responseCode = "500",
                description = "Internal Server Error",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = STError.class))),
        })
    public Response getLocaleCompletion(
        @PathParam("locale") @Schema(required = true, description = "Locale ID", example = "aa") String localeId) {
        CLDRLocale cldrLocale = CLDRLocale.getInstance(localeId);
        return getLocaleCompletion(cldrLocale);
    }

    /**
     * Fetch locale completion for the specified locale.
     * Note: Static and package-private for testability.
     * @param cldrLocale
     * @return Response
     */
    static Response getLocaleCompletion(CLDRLocale cldrLocale) {
        if (SurveyMain.isBusted() || !SurveyMain.wasInitCalled() || !SurveyMain.triedToStartUp()) {
            return STError.surveyNotQuiteReady();
        }

        final STFactory stFactory = CookieSession.sm.getSTFactory();
        final LocaleCompletionResponse lcr = getLocaleCompletion(cldrLocale, stFactory);
        return Response.ok(lcr).build();
    }

    static final class LocaleCompletionHelper {
        PathHeader.Factory phf = null;

        LocaleCompletionHelper() {
            phf = PathHeader.getFactory(CLDRConfig.getInstance().getEnglish());
        }

        static LocaleCompletionHelper INSTANCE = new LocaleCompletionHelper();
    }

    static LocaleCompletionResponse getLocaleCompletion(final CLDRLocale cldrLocale, final STFactory stFactory) {
        final String localeId = cldrLocale.toString(); // normalized
        final Level level = StandardCodes.make().getTargetCoverageLevel(localeId);
        final CheckCLDR.Options options = new CheckCLDR.Options(cldrLocale, SurveyMain.getTestPhase(),
            level.toString(),
            null);
        ElapsedTimer et = new ElapsedTimer("LocaleCompletion:" + options.toString());
        logger.info("Starting LocaleCompletion for " + options.toString());
        final TestResultBundle checkCldr = stFactory.getTestResult(cldrLocale, options);
        final CLDRFile file = stFactory.make(localeId, true);
        final CLDRFile baselineFile = stFactory.getDiskFile(cldrLocale);

        LocaleCompletionResponse lcr = new LocaleCompletionResponse(level);

        final SupplementalDataInfo sdi = SupplementalDataInfo.getInstance(stFactory.getSupplementalDirectory());
        final CoverageLevel2 covLeveller = CoverageLevel2.getInstance(sdi, localeId);

        final List<CheckStatus> results = new ArrayList<>();
        for (final String xpath : file) {
            lcr.allXpaths++;
            final Level pathLevel = covLeveller.getLevel(xpath);
            final String fullPath = file.getFullXPath(xpath);

            final PathHeader ph = LocaleCompletionHelper.INSTANCE.phf.fromPath(xpath);
            SurveyToolStatus surveyToolStatus = ph.getSurveyToolStatus();
            if (surveyToolStatus == SurveyToolStatus.DEPRECATED || surveyToolStatus == SurveyToolStatus.HIDE) {
                lcr.ignoredHidden++;
                continue; // not visible
            }

            checkCldr.check(xpath, results, file.getStringValue(xpath));

            final boolean hasError = CheckStatus.hasError(results);

            final boolean statusMissing = STFactory.calculateStatus(file, baselineFile, xpath) == VoteResolver.Status.missing;

            if (statusMissing) {
                lcr.statusMissing++;
            }

            // TODO: copy and paste from CheckCLDR.getShowRowAction - CLDR-15230
            if (CheckCLDR.LIMITED_SUBMISSION && !SubmissionLocales.allowEvenIfLimited(
                localeId,
                xpath,
                hasError,
                statusMissing)) {
                lcr.ignoredLimited++;
                continue; // not allowed through by SubmissionLocales.
            }

            if (hasError) {
                if (results.size() == 1 && results.get(0).getSubtype() == Subtype.coverageLevel) {
                    lcr.addMissing();
                } else {
                    lcr.addError();
                }
            } else {
                if (pathLevel.getLevel() < level.getLevel()) {
                    final CLDRFile.DraftStatus status = CLDRFile.DraftStatus.forXpath(fullPath);
                    if (status == DraftStatus.provisional || status == DraftStatus.unconfirmed) {
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
        logger.info(et.toString());
        return lcr;
    }

    public static class LocaleCompletionResponse {
        public int votes = 0;
        public int total = 0;
        public int error = 0;
        public int missing = 0;
        public int provisional = 0;
        final public String level;

        // The following are more or less debug items
        public int ignoredLimited = 0;
        public int ignoredHidden = 0;
        public int allXpaths = 0;
        public int statusMissing = 0;
        public int ignoredOutOfCov = 0;

        LocaleCompletionResponse(Level l) {
            level = l.name();
        }

        /**
         * Add a count for an Error path
         */
        void addError() {
            error++;
            total++;
        }

        /**
         * Add a count for a Missing path
         */
        void addMissing() {
            missing++;
            total++;
        }

        /**
         * Add a count for a Provisional path
         */
        void addProvisional() {
            provisional++;
            total++;
        }

        /**
         * Add a count for an OK path
         */
        void addOk() {
            votes++; // OK count
            total++;
        }
    }
}
