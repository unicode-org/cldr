package org.unicode.cldr.web.api;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.XMLSource.Listener;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.STFactory;
import org.unicode.cldr.web.SurveyMain;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

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
@ApplicationScoped
@Path("/completion")
@Tag(name = "completion", description = "APIs for voting completion statistics")
public class LocaleCompletion {

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
    @Counted(name = "getLocaleCompletionCount", absolute = true, description = "Number of locale completions computed")
    @Timed(absolute = true, name = "getLocaleCompletionTime", description = "Time to fetch Locale Completion")
    public Response getLocaleCompletion(
        @PathParam("locale") @Schema(required = true, description = "Locale ID", example = "aa") String localeId) throws ExecutionException {
        if (SurveyMain.isBusted() || !SurveyMain.wasInitCalled() || !SurveyMain.triedToStartUp()) {
            return STError.surveyNotQuiteReady();
        }
        CLDRLocale cldrLocale = CLDRLocale.getInstance(localeId);
        return Response.ok(getLocaleCompletion(cldrLocale)).build();
    }

    static LocaleCompletionResponse getLocaleCompletion(CLDRLocale cldrLocale) throws ExecutionException {
        return LocaleCompletionHelper.INSTANCE.cache.get(cldrLocale);
    }

    /**
     * This function computes the actual locale completion given a Locale
     * @param cldrLocale the locale
     * @return the response
     */
    static LocaleCompletionResponse handleGetLocaleCompletion(CLDRLocale cldrLocale) {
        final STFactory stFactory = CookieSession.sm.getSTFactory();
        return handleGetLocaleCompletion(cldrLocale, stFactory);
    }

    static final class LocaleCompletionHelper implements Listener {
        PathHeader.Factory phf;
        LoadingCache<CLDRLocale, LocaleCompletionResponse> cache;
        LocaleCompletionHelper() {
            phf = PathHeader.getFactory(CLDRConfig.getInstance().getEnglish());
            cache = CacheBuilder.newBuilder().maximumSize(500)
            .concurrencyLevel(5) // allow 5 threads to compute completion, uncontested
            .expireAfterWrite(Duration.ofMinutes(20)) // expire 20 min after last change
            .build(new CacheLoader<>() {
                @Override
                public LocaleCompletionResponse load(CLDRLocale key) {
                    return handleGetLocaleCompletion(key);
                }
            });
        }
        static LocaleCompletionHelper INSTANCE = new LocaleCompletionHelper();

        @Override
        public void valueChanged(String xpath, XMLSource source) {
            // invalidate the named entry
            cache.invalidate(CLDRLocale.getInstance(source.getLocaleID()));
        }
    }

    /**
     * This function computes the actual locale completion given a Locale and STFactory
     *
     * @param cldrLocale the locale
     * @param stFactory the STFactory
     * @return the response
     */
    static LocaleCompletionResponse handleGetLocaleCompletion(final CLDRLocale cldrLocale, final STFactory stFactory) {
        // we need an XML Source to receive notification.
        // This causes LocaleCompletionHelper.INSTANCE.valueChanged(...) to be called
        // whenever a vote happens.
        final XMLSource mySource = stFactory.makeSource(cldrLocale.toString(), false);
        mySource.addListener(LocaleCompletion.LocaleCompletionHelper.INSTANCE);

        return new LocaleCompletionCounter(cldrLocale, stFactory).getResponse();
    }

    public static class LocaleCompletionResponse {
        /*
         * The front end only uses votes, total, and level
         * The rest is for debugging, testing, or convenience on the back end
         */
        public int votes = 0;
        public int total = 0;
        final public String level;

        public int error = 0;
        public int missing = 0;
        public int provisional = 0;

        LocaleCompletionResponse(Level l) {
            level = l.name();
        }
    }
}
