package org.unicode.cldr.web.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.Factory;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.DBUtils;
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
    private final SurveyMain sm = CookieSession.sm;
    private final SupplementalDataInfo supplementalDataInfo = sm.getSupplementalDataInfo();
    private final Factory pathHeaderFactory = PathHeader.getFactory();

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
            // @APIResponse(
            //     responseCode = "401",
            //     description = "Authorization required, send a valid session id"),
            @APIResponse(
                responseCode = "404",
                description = "Locale not found"),
            @APIResponse(
                responseCode = "500",
                description = "Internal Server Error",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = STError.class))),
        })
    public Response getLocaleCompletion(
        @PathParam("locale") @Schema(required = true, description = "Locale ID", example = "aa") String localeId
        // Should not require session header.
        // @HeaderParam(Auth.SESSION_HEADER) String session
        ) {
        // final CookieSession mySession = Auth.getSession(session);
        // if (mySession == null || mySession.user == null) {
        //     return Response.status(Status.UNAUTHORIZED).build();
        // }
        // Level coverageLevel = Level.fromString(level);
        CLDRLocale cldrLocale = CLDRLocale.getInstance(localeId);
        // if (coverageLevel == Level.UNDETERMINED || cldrLocale == null) {
        //     return Response.status(Status.NOT_FOUND).build();
        // }
        return get(/* mySession.user.id,*/ cldrLocale /*, coverageLevel*/);
    }

    private Response get(/*int userId, */CLDRLocale cldrLocale/*, Level coverageLevel*/) {
        final boolean MEASURE_PERFORMANCE = false;
        final long firstTime, secondTime;
        if (MEASURE_PERFORMANCE) {
            firstTime = System.currentTimeMillis();
        }
        final String localeId = cldrLocale.toString(); // normalized
        final Level level = StandardCodes.make().getTargetCoverageLevel(localeId);
        int voted = 5;
        int total = 9;
        // try {
        //     voted = getVotedPathCount(userId, localeId, coverageLevel);
        // } catch (SQLException se) {
        //     return new STError(se).build();
        // }
        // if (MEASURE_PERFORMANCE) {
        //     System.out.println("voting completion: voted = " + voted);
        //     secondTime = System.currentTimeMillis();
        //     System.out.println("voting completion: time elapsed for voted (ms) = " + (secondTime - firstTime));
        // }
        // int total = getTotalPathCount(localeId, coverageLevel);
        // if (MEASURE_PERFORMANCE) {
        //     System.out.println("voting completion: total = " + total + " at coverage " + coverageLevel);
        //     System.out.println("voting completion: time elapsed for total (ms) = " + (System.currentTimeMillis() - secondTime));
        // }
        return Response.ok(new LocaleCompletionResponse(voted, total, level.name())).build();
    }

    public class LocaleCompletionResponse {
        public int votes;
        public int total;
        public String level;

        public LocaleCompletionResponse(int votes, int total, String level) {
            this.votes = votes;
            this.total = total;
            this.level = level;
        }
    }
}
