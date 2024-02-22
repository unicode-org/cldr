package org.unicode.cldr.web.api;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.unicode.cldr.util.DowngradePaths;
import org.unicode.cldr.util.VoteType;
import org.unicode.cldr.web.*;

@ApplicationScoped
@Path("/voting/downgraded")
@Tag(name = "Downgraded votes", description = "APIs for Survey Tool downgraded votes")
public class DowngradedVotes {
    private static final Logger logger = SurveyLog.forClass(DowngradedVotes.class);

    /**
     * The submitter column must be included even though it isn't used explicitly, since it is a
     * primary key, and the query must select all primary keys in order to call
     * ResultSet.deleteRow() successfully
     */
    private static final String SELECT_SQL =
            "SELECT locale,xpath,value,vote_type,submitter FROM " + DBUtils.Table.VOTE_VALUE;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get statistics",
            description = "Get statistics about votes for downgraded paths")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Downgraded Stats",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                DowngradedStatsResponse.class))),
                @APIResponse(responseCode = "403", description = "Forbidden"),
                @APIResponse(
                        responseCode = "500",
                        description = "Internal Server Error",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = STError.class))),
                @APIResponse(responseCode = "503", description = "Not ready yet"),
            })
    public Response getStats(@HeaderParam(Auth.SESSION_HEADER) String sessionString) {
        CookieSession session = Auth.getSession(sessionString);
        if (session == null) {
            return Auth.noSessionResponse();
        }
        if (!UserRegistry.userIsAdmin(session.user)) {
            return Response.status(403, "Forbidden").build();
        }
        session.userDidAction();
        if (SurveyMain.isBusted() || !SurveyMain.wasInitCalled() || !SurveyMain.triedToStartUp()) {
            return STError.surveyNotQuiteReady();
        }
        DowngradedStatsResponse response = new DowngradedStatsResponse();
        return Response.ok(response).build();
    }

    @Schema(description = "Stats of downgraded paths")
    public static final class DowngradedStatsResponse {
        @Schema(description = "Number of votes for downgraded paths, categorized by vote type")
        public DowngradeCategory[] cats;

        public DowngradedStatsResponse() {
            Map<VoteType, Integer> catMap = new HashMap<>();
            loopThroughAllVotes(catMap);
            cats = new DowngradeCategory[catMap.size()];
            int i = 0;
            for (Map.Entry<VoteType, Integer> entry : catMap.entrySet()) {
                cats[i++] = new DowngradeCategory(entry.getKey(), entry.getValue());
            }
        }
    }

    @Schema(description = "Single category of votes for downgraded paths")
    public static class DowngradeCategory {
        @Schema(description = "The vote type")
        public VoteType voteType;

        @Schema(description = "The number of downgraded votes with this type")
        public int count;

        @Schema(description = "Whether this type of vote is classified as being imported")
        public boolean isImported;

        public DowngradeCategory(VoteType voteType, int count) {
            this.voteType = voteType;
            this.count = count;
            this.isImported = this.voteType.isImported();
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Delete", description = "Delete imported votes for downgraded paths")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Successful deletion",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                DowngradedStatsResponse.class))),
                @APIResponse(responseCode = "403", description = "Forbidden"),
                @APIResponse(
                        responseCode = "500",
                        description = "Internal Server Error",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = STError.class))),
            })
    public Response deleteImported(@HeaderParam(Auth.SESSION_HEADER) String sessionString) {
        CookieSession session = Auth.getSession(sessionString);
        if (session == null) {
            return Auth.noSessionResponse();
        }
        if (!UserRegistry.userIsAdmin(session.user)) {
            return Response.status(403, "Forbidden").build();
        }
        session.userDidAction();
        DeletionResponse response;
        try {
            response = new DeletionResponse();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
        return Response.ok(response).build();
    }

    @Schema(description = "Report on status of deletion request")
    public static final class DeletionResponse {
        public DeletionResponse() {
            loopThroughAllVotes(null /* delete */);
        }
    }

    /**
     * Loop through all the votes, and fill in the given map with info about votes for downgraded
     * paths; or, if the map is null, delete all imported votes for downgraded paths.
     *
     * @param catMap the category map, or null for deletion
     */
    private static void loopThroughAllVotes(Map<VoteType, Integer> catMap) {
        Connection conn;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBUtils.getInstance().getAConnection();
            if (conn == null) {
                return;
            }
            ps = DBUtils.prepareForwardUpdateable(conn, SELECT_SQL);
            rs = ps.executeQuery();
            while (rs.next()) {
                handleVote(rs, catMap /* if catMap is null, it means DELETE */);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            DBUtils.close(rs, ps);
        }
    }

    private static void handleVote(ResultSet rs, Map<VoteType, Integer> catMap)
            throws SQLException {
        String localeId = rs.getString(1);
        int xp = rs.getInt(2);
        String xpath = CookieSession.sm.xpt.getById(xp);
        String value = DBUtils.getStringUTF8(rs, 3);
        VoteType voteType = VoteType.fromId(rs.getInt(4));
        if (voteType == null || voteType == VoteType.NONE) {
            logger.warning("DowngradedVotes got vote type " + voteType + "; changed to UNKNOWN");
            voteType = VoteType.UNKNOWN;
        }
        if (catMap != null) {
            if (DowngradePaths.lookingAt(localeId, xpath, value)) {
                Integer n = catMap.get(voteType);
                catMap.put(voteType, (n == null) ? 1 : n + 1);
            }
        } else {
            /* if catMap is null and the vote is imported and the path is downgraded, delete the vote */
            if (voteType.isImported() && DowngradePaths.lookingAt(localeId, xpath, value)) {
                rs.deleteRow();
            }
        }
    }
}
