package org.unicode.cldr.web.api;

import java.io.IOException;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
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
import org.json.JSONException;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.web.*;
import org.unicode.cldr.web.Dashboard.ReviewOutput;
import org.unicode.cldr.web.VettingViewerQueue.LoadingPolicy;

@Path("/summary")
@Tag(name = "voting", description = "APIs for voting")
public class Summary {

    /**
     * jsonb enables converting an object to a json string,
     * used for creating snapshots
     */
    private static final Jsonb jsonb = JsonbBuilder.create();

    /**
     * For saving and retrieving "snapshots" of Summary responses
     *
     * Note: for debugging/testing without using db, new SurveySnapshotDb() can be
     * changed here to new SurveySnapshotMap()
     */
    private static final SurveySnapshot snap = new SurveySnapshotDb();

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get Priority Items Summary",
        description = "Also known as Vetting Summary, this like a Dashboard for multiple locales.")
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "200",
                description = "Results of Summary operation",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SummaryResponse.class)))
        })
    public Response doVettingSummary(
        SummaryRequest request,
        @HeaderParam(Auth.SESSION_HEADER) String sessionString) {
        try {
            CookieSession cs = Auth.getSession(sessionString);
            if (cs == null) {
                return Auth.noSessionResponse();
            }
            if (!UserRegistry.userCanUseVettingSummary(cs.user)) {
                return Response.status(Status.FORBIDDEN).build();
            }
            if (SurveySnapshot.SNAP_CREATE.equals(request.snapshotPolicy)
                && !UserRegistry.userCanCreateSummarySnapshot(cs.user)) {
                return Response.status(Status.FORBIDDEN).build();
            }
            return getPriorityItemsSummary(cs, request);
        } catch (JSONException | IOException ioe) {
            return Response.status(500, "An exception occurred").entity(ioe).build();
        }
    }

    /**
     * Get the response for Priority Items Summary (possibly a snapshot)
     *
     * Each request specifies a loading policy: START, NOSTART, or FORCESTOP.
     * Typically, the client makes a request with START, then makes repeated requests with NOSTART
     * while the responses have status PROCESSING (or WAITING), until there is a response with status READY.
     * The response with status READY contains the actual requested Priority Items Summary data.
     *
     * Each request specifies a snapshot policy: SNAP_NONE, SNAP_CREATE, or SNAP_SHOW.
     *
     * @param cs the CookieSession identifying the user
     * @param request the SummaryRequest
     * @return the Response
     *
     * @throws IOException
     * @throws JSONException
     */
    private Response getPriorityItemsSummary(CookieSession cs, SummaryRequest request) throws IOException, JSONException {
        cs.userDidAction();
        if (SurveySnapshot.SNAP_SHOW.equals(request.snapshotPolicy)) {
            return showSnapshot(request.snapshotId);
        }
        SummaryResponse sr = getSummaryResponse(cs, request.loadingPolicy);
        if (SurveySnapshot.SNAP_CREATE.equals(request.snapshotPolicy)
            && sr.status == VettingViewerQueue.Status.READY) {
            saveSnapshot(sr);
        }
        return Response.ok(sr, MediaType.APPLICATION_JSON).build();
    }

    /**
     * Get the response for Priority Items Summary (new, not an already existing snapshot)
     *
     * @param cs the CookieSession identifying the user
     * @param loadingPolicy the LoadingPolicy
     * @return the SummaryResponse
     *
     * @throws IOException
     * @throws JSONException
     */
    private SummaryResponse getSummaryResponse(CookieSession cs, LoadingPolicy loadingPolicy) throws IOException, JSONException {
        VettingViewerQueue.Status[] status = new VettingViewerQueue.Status[1];
        StringBuilder sb = new StringBuilder();
        VettingViewerQueue vvq = VettingViewerQueue.getInstance();
        String str = vvq.getPriorityItemsSummaryOutput(cs, status, loadingPolicy, sb);
        SummaryResponse sr = new SummaryResponse();
        sr.status = status[0];
        sr.ret = str;
        sr.output = sb.toString();
        return sr;
    }

    /**
     * Return a Response containing the snapshot with the given id
     *
     * @param snapshotId
     * @return the Response
     */
    private Response showSnapshot(String snapshotId) {
        final String json = snap.get(snapshotId);
        if (json == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        return Response.ok(json).build();
    }

    /**
     * Assign a new snapshot id to sr, then save sr as a json snapshot
     *
     * @param sr the SummaryResponse
     */
    private void saveSnapshot(SummaryResponse sr) {
        sr.snapshotId = SurveySnapshot.newId();
        final String json = jsonb.toJson(sr);
        if (json != null && !json.isEmpty()) {
            snap.put(sr.snapshotId, json);
        }
    }

    @GET
    @Path("/snapshots")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "List All Snapshots",
        description = "Get a list of all available snapshots of the Priority Items Summary")
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "200",
                description = "Snapshot List",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SnapshotListResponse.class)))
        })
    public Response listSnapshots(
        @HeaderParam(Auth.SESSION_HEADER) String sessionString) {
        CookieSession cs = Auth.getSession(sessionString);
        if (cs == null) {
            return Auth.noSessionResponse();
        }
        if (!UserRegistry.userCanUseVettingSummary(cs.user)) {
            return Response.status(Status.FORBIDDEN).build();
        }
        cs.userDidAction();
        SnapshotListResponse ret = new SnapshotListResponse(snap.list());
        return Response.ok().entity(ret).build();
    }

    @Schema(description = "Response for List Snapshots request")
    public static final class SnapshotListResponse {

        @Schema(description = "Array of available snapshots")
        public String[] array;

        public SnapshotListResponse(String[] array) {
            this.array = array;
        }
    }

    @GET
    @Path("/dashboard/{locale}/{level}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Fetch the Dashboard for a locale",
        description = "Given a locale, get the summary information, aka Dashboard")
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "200",
                description = "Dashboard results",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ReviewOutput.class))) // TODO: SummaryResults.class
        })
    public Response getDashboard(
        @PathParam("locale") @Schema(required = true, description = "Locale ID") String locale,
        @PathParam("level") @Schema(required = true, description = "Coverage Level") String level,
        @HeaderParam(Auth.SESSION_HEADER) String sessionString) {
        CLDRLocale loc = CLDRLocale.getInstance(locale);
        CookieSession cs = Auth.getSession(sessionString);
        if (cs == null) {
            return Auth.noSessionResponse();
        }
        if (!UserRegistry.userCanModifyLocale(cs.user, loc)) {
            return Response.status(403, "Forbidden").build();
        }
        cs.userDidAction();

        // *Beware*  org.unicode.cldr.util.Level (coverage) ≠ VoteResolver.Level (user)
        Level coverageLevel = org.unicode.cldr.util.Level.fromString(level);
        ReviewOutput ret = new Dashboard().get(loc, cs.user, coverageLevel);

        return Response.ok().entity(ret).build();
    }
}
