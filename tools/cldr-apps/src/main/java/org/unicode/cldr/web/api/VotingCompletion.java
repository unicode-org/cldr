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
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.DBUtils;
import org.unicode.cldr.web.SurveyMain;
import org.unicode.cldr.web.api.VotingParticipation.VotingParticipationResults;

@Path("/completion")
@Tag(name = "completion", description = "APIs for voting completion statistics")
public class VotingCompletion {

    private static final boolean DEBUG = false;

    private final SurveyMain sm = CookieSession.sm;
    private final SupplementalDataInfo supplementalDataInfo = sm.getSupplementalDataInfo();

    @GET
    @Path("/voting/{locale}/{level}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get voting completion statistics",
        description = "Get voting completion statistics for the requesting user and the given locale and coverage level")
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "200",
                description = "Voting completion statistics for the requesting user and the given locale and coverage level",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = VotingParticipationResults.class))),
            @APIResponse(
                responseCode = "401",
                description = "Authorization required, send a valid session id"),
            @APIResponse(
                responseCode = "404",
                description = "Locale or Level not found"),
            @APIResponse(
                responseCode = "500",
                description = "Internal Server Error",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = STError.class))),
        })
    public Response getVotingCompletion(
        @PathParam("locale") @Schema(required = true, description = "Locale ID", example = "aa") String localeId,
        @PathParam("level") @Schema(required = true, description = "Coverage Level", example = "modern") String level,
        @HeaderParam(Auth.SESSION_HEADER) String session) {
        final CookieSession mySession = Auth.getSession(session);
        if (mySession == null || mySession.user == null) {
            return Response.status(Status.UNAUTHORIZED).build();
        }
        Level coverageLevel = Level.fromString(level);
        CLDRLocale cldrLocale = CLDRLocale.getInstance(localeId);
        if (coverageLevel == Level.UNDETERMINED || cldrLocale == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return get(mySession.user.id, cldrLocale, coverageLevel);
    }

    private Response get(int userId, CLDRLocale cldrLocale, Level coverageLevel) {
        final long firstTime, secondTime;
        if (DEBUG) {
            firstTime = System.currentTimeMillis();
        }
        final String localeId = cldrLocale.toString(); // normalized
        int voted = -1;
        try {
            voted = getVotedPathCount(userId, localeId, coverageLevel);
        } catch (SQLException se) {
            return new STError(se).build();
        }
        if (DEBUG) {
            System.out.println("voting completion: voted = " + voted);
            secondTime = System.currentTimeMillis();
            System.out.println("voting completion: time elapsed for voted (ms) = " + (secondTime - firstTime));
        }
        int total = getTotalPathCount(localeId, coverageLevel);
        if (DEBUG) {
            System.out.println("voting completion: total = " + total + " at coverage " + coverageLevel);
            System.out.println("voting completion: time elapsed for total (ms) = " + (System.currentTimeMillis() - secondTime));
        }
        return Response.ok(new VotingCompletionResponse(voted, total)).build();
    }

    public class VotingCompletionResponse {
        public int votes;
        public int total;

        public VotingCompletionResponse(int votes, int total) {
            this.votes = votes;
            this.total = total;
        }
    }

    /**
     * How many paths have current votes by the given user, in the given locale and coverage level?
     *
     * @param userId
     * @param localeId
     * @param coverageLevel
     * @return the number of paths satisfying the criteria
     * @throws SQLException
     *
     * Performance as measured on localhost: for fr/comprehensive, if the current user had 8909 votes
     * satisfying the criteria, this function took 443 ms when called the first time, and 38 ms when called
     * again shortly thereafter.
     *
     * The count could be cached (per user, etc.) and updated more quickly just when the user votes; that could
     * be done on either the front or the back end. Probably the best performance gain would be by caching on
     * the front end.
     */
    private synchronized int getVotedPathCount(int userId, String localeId, Level coverageLevel) throws SQLException {
        final String sql = "SELECT xpath FROM " + DBUtils.Table.VOTE_VALUE
            + " WHERE submitter=" + userId
            + " AND locale='" + localeId + "'"
            + " AND value IS NOT NULL";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int votedPathCount = 0;
        try {
            conn = sm.dbUtils.getAConnection();
            ps = DBUtils.prepareStatement(conn, "votingCompletion", sql);
            rs = ps.executeQuery();
            if (rs == null) {
                return 0;
            }
            while (rs.next()) {
                int xpathId = rs.getInt(1);
                String xpath = sm.xpt.getById(xpathId);
                Level pathCovLevel = supplementalDataInfo.getCoverageLevel(xpath, localeId);
                if (pathCovLevel.compareTo(coverageLevel) <= 0) {
                    ++votedPathCount;
                }
            }
        } finally {
            DBUtils.close(conn, ps, rs);
        }
        return votedPathCount;
    }

    /**
     * How many paths exist in the given locale and coverage level?
     *
     * @param localeId
     * @param coverageLevel
     * @return the number of paths satisfying the criteria
     *
     * Possibly worth caching by locale and level; the number never (?) changes between
     * Survey Tool initialization and shutdown.
     *
     * Possibly there are other functions that loop through a CLDRFile during initialization
     * and they could calculate the totals less expensively, as a side effect.
     *
     * Performance as measured on localhost: for fr/comprehensive, this function took
     * 5735 milliseconds (!) when called the first time, then 30 milliseconds if called
     * again shortly thereafter. However, that 5735 milliseconds evidently involves
     * server/locale initialization that only happens once per Survey Tool startup,
     * and would happen anyway if this function weren't the first thing called.
     * If the user has already loaded the Dashboard, then this function takes 358 ms
     * the first time called, then 25 ms if called again shortly thereafter.
     */
    private synchronized int getTotalPathCount(String localeId, Level coverageLevel) {
        final CLDRFile cldrFile = sm.getSTFactory().make(localeId, true, true);
        int total = 0;
        for (String xpath : cldrFile.fullIterable()) {
            // TODO: take into account Mark's note about "skip SUPPRESS (path header)"?
            Level pathCovLevel = supplementalDataInfo.getCoverageLevel(xpath, localeId);
            if (pathCovLevel.compareTo(coverageLevel) <= 0) {
                ++total;
            }
        }
        return total;
    }
}
