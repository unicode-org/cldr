package org.unicode.cldr.web.api;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
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
import org.unicode.cldr.util.*;
import org.unicode.cldr.util.XMLSource.Listener;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.STFactory;
import org.unicode.cldr.web.SurveyMain;

/**
 * "A locale has complete coverage when there are no Missing values, no Provisional values, and no
 * Errors (aka no MEPs). The Missing / Provisional values are determined at the target coverage
 * levels (not specific to any organization), while Errors need to be counted at the comprehensive
 * level (because we have to resolve all of them in resolution).
 *
 * <p>"In order to show progress towards completion of the locale, we compare the current status to
 * that of the corresponding baseline (blue star) values. Those baseline values can be computed and
 * cached the first time they are needed, so that there is only a 1 time cost rather than a constant
 * cost. But if that is done, the values must be cleared at each push to production, so that they
 * are recomputed afterwards. That is because sometimes we change/add the baseline values when we
 * push to production.
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
                        description =
                                "Voting completion statistics for the given locale at the target coverage level (not specific to any organization)",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                LocaleCompletionResponse.class))),
                @APIResponse(responseCode = "404", description = "Locale not found"),
                @APIResponse(responseCode = "503", description = "Not ready yet"),
                @APIResponse(
                        responseCode = "500",
                        description = "Internal Server Error",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = STError.class))),
            })
    @Counted(
            name = "getLocaleCompletionCount",
            absolute = true,
            description = "Number of locale completions computed")
    @Timed(
            absolute = true,
            name = "getLocaleCompletionTime",
            description = "Time to fetch Locale Completion")
    public Response getLocaleCompletion(
            @PathParam("locale") @Schema(required = true, description = "Locale ID", example = "aa")
                    String localeId)
            throws ExecutionException {
        if (SurveyMain.isBusted() || !SurveyMain.wasInitCalled() || !SurveyMain.triedToStartUp()) {
            return STError.surveyNotQuiteReady();
        }
        if (localeId == null || localeId.isBlank() || localeId.equals("USER")) {
            return STError.badLocale(localeId); // 404
        }
        CLDRLocale cldrLocale = CLDRLocale.getInstance(localeId);
        return Response.ok(getLocaleCompletion(cldrLocale)).build();
    }

    /**
     * Getter for cached Locale Completion
     *
     * @param cldrLocale the locale
     * @return the response
     * @throws ExecutionException
     */
    public static LocaleCompletionResponse getLocaleCompletion(CLDRLocale cldrLocale)
            throws ExecutionException {
        return LocaleCompletionHelper.INSTANCE.cache.get(cldrLocale);
    }

    /**
     * This function computes the actual locale completion given a Locale
     *
     * @param cldrLocale the locale
     * @return the response
     */
    static LocaleCompletionResponse handleGetLocaleCompletion(CLDRLocale cldrLocale)
            throws ExecutionException {
        final STFactory stFactory = CookieSession.sm.getSTFactory();
        return handleGetLocaleCompletion(cldrLocale, stFactory);
    }

    static final class LocaleCompletionHelper implements Listener {

        PathHeader.Factory phf;
        LoadingCache<CLDRLocale, LocaleCompletionResponse> cache;
        LoadingCache<CLDRLocale, Integer> basecache;

        LocaleCompletionHelper() {
            phf = PathHeader.getFactory(CLDRConfig.getInstance().getEnglish());
            cache =
                    CacheBuilder.newBuilder()
                            .maximumSize(500)
                            .concurrencyLevel(
                                    5) // allow 5 threads to compute completion, uncontested
                            .expireAfterWrite(
                                    Duration.ofMinutes(20)) // expire 20 min after last change
                            .build(
                                    new CacheLoader<>() {
                                        @Override
                                        public LocaleCompletionResponse load(CLDRLocale key)
                                                throws ExecutionException {
                                            return handleGetLocaleCompletion(key);
                                        }
                                    });
            basecache =
                    CacheBuilder.newBuilder()
                            .maximumSize(500)
                            .concurrencyLevel(
                                    5) // allow 5 threads to compute completion, uncontested
                            // no expiry
                            .build(
                                    new CacheLoader<>() {
                                        @Override
                                        public Integer load(CLDRLocale key)
                                                throws ExecutionException {
                                            return handleGetBaseCount(key);
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
    static LocaleCompletionResponse handleGetLocaleCompletion(
            final CLDRLocale cldrLocale, final STFactory stFactory) throws ExecutionException {
        // we need an XML Source to receive notification.
        // This causes LocaleCompletionHelper.INSTANCE.valueChanged(...) to be called
        // whenever a vote happens.
        stFactory
                .get(cldrLocale)
                .getSource()
                .addListener(LocaleCompletion.LocaleCompletionHelper.INSTANCE);
        return new LocaleCompletionCounter(cldrLocale, stFactory).getResponse();
    }

    public static int getBaselineCount(CLDRLocale cldrLocale) throws ExecutionException {
        return LocaleCompletionHelper.INSTANCE.basecache.get(cldrLocale);
    }

    static int handleGetBaseCount(final CLDRLocale cldrLocale) throws ExecutionException {
        // no need to listen
        final Factory baselineFactory = CookieSession.sm.getDiskFactory();
        LocaleCompletionCounter lcc =
                new LocaleCompletionCounter(cldrLocale, baselineFactory, true);
        LocaleCompletionResponse response = lcc.getResponse();
        return response.problemCount();
    }

    public static class LocaleCompletionResponse {

        @Schema(description = "coverage level")
        public final String level;

        private final LocaleCompletionData lcd;

        LocaleCompletionResponse(Level l, LocaleCompletionData lcd) {
            this.level = l.name();
            this.lcd = lcd;
        }

        @Schema(description = "number of items with errors")
        public int getError() {
            return lcd.errorCount();
        }

        @Schema(description = "number of missing items")
        public int getMissing() {
            return lcd.missingCount();
        }

        @Schema(description = "number of provisional items")
        public int getProvisional() {
            return lcd.provisionalCount();
        }

        private int baselineCount = 0;

        @Schema(description = "error+missing+provisional from baseline (HEAD)")
        public int getBaselineCount() {
            return baselineCount;
        }

        public void setBaselineCount(int count) {
            this.baselineCount = count;
        }

        public int problemCount() {
            return lcd.problemCount();
        }
    }
}
