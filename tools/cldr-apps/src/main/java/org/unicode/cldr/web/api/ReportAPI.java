package org.unicode.cldr.web.api;

import java.sql.SQLException;

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
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.VoterReportStatus;
import org.unicode.cldr.util.VoterReportStatus.ReportId;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.ReportsDB;
import org.unicode.cldr.web.ReportsDB.UserReport;
import org.unicode.cldr.web.UserRegistry;

@Path("/voting/reports")
@Tag(name = "voting", description = "APIs for voting and retrieving vote and row data")
public class ReportAPI {
    @GET
    @Path("/users/{user}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get User Report Status", description = "Get one or all user’s report status")
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "200",
                description = "Results of an All Reports Status request",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(type = SchemaType.ARRAY, implementation = UserReport.class)))
        })
    public Response getAllReports(
        @HeaderParam(Auth.SESSION_HEADER) String session,
        @Parameter(required = true, example = "1",
            schema = @Schema(type = SchemaType.STRING, description = "user ID or '-' for all")) @PathParam("user") String user) throws SQLException {
        final CookieSession mySession = Auth.getSession(session);
        if (mySession == null) {
            return Auth.noSessionResponse();
        }
        Integer id;
        if (user.equals("-")) {
            id = null;
            if (!UserRegistry.userIsTC(mySession.user)) {
                // only TC can do this
                return Response.status(Status.FORBIDDEN).build();
            }
        } else {
            id = Integer.parseInt(user);
            UserRegistry.User u = mySession.sm.reg.getInfo(id);
            if (u == null) {
                return Response.status(Status.NOT_FOUND).build();
            } else if (!(mySession.user.id == id || !mySession.user.isAdminFor(u))) {
                return Response.status(Status.FORBIDDEN).build();
            }
        }
        return Response.ok(ReportsDB.getInstance().getAllReports(id)).build();
    }

    @GET
    @Path("/users/{user}/locales/{locale}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get Report Status",
        description = "This handles a request for a specific user and locale report status")
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "200",
                description = "Results of Report Status request",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = VoterReportStatus.ReportStatus.class))),
            @APIResponse(
                responseCode = "403",
                description = "Forbidden"),
        })
    public Response getReport(
        @Parameter(required = true, example = "1", schema = @Schema(type = SchemaType.INTEGER)) @PathParam("user") Integer user,
        @Parameter(required=true, example="mt", schema = @Schema(type = SchemaType.STRING)) @PathParam("locale") String locale,
        @HeaderParam(Auth.SESSION_HEADER) String session) {
        final CookieSession mySession = Auth.getSession(session);
        if (mySession == null || mySession.user == null) {
            return Auth.noSessionResponse();
        }
        UserRegistry.User u = mySession.sm.reg.getInfo(user);
        if (u == null) {
            return Response.status(Status.NOT_FOUND).build();
        } else if (!(mySession.user.id == user || !mySession.user.isAdminFor(u))) {
            return Response.status(Status.FORBIDDEN).build();
        }

        return Response.ok().entity(
            ReportsDB.getInstance()
                .getReportStatus(user, CLDRLocale.getInstance(locale))).build();
    }

    @POST
    @Path("/users/{user}/locales/{locale}/reports/{report}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Update Report Status",
        description = "This updates a specific user’s report status. "+
        "You can only update your own status.")
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "204",
                description = "Updated OK"
            ),
            @APIResponse(
                responseCode = "403",
                description = "Forbidden"
            ),
            @APIResponse(
                responseCode = "401",
                description = "Unauthorized"
            )
        })
    public Response updateReport(
        @Parameter(required=true, example="1", schema = @Schema(type = SchemaType.INTEGER)) @PathParam("user") Integer user,
        @Parameter(required=true, example="mt", schema = @Schema(type = SchemaType.STRING)) @PathParam("locale") String locale,
        // Note:  @Schema(implementation = ReportId.class) did not work here. The following works.
        @Parameter(required=true, example="compact", schema = @Schema(type = SchemaType.STRING)) @PathParam("report") ReportId report,
        @HeaderParam(Auth.SESSION_HEADER) String session,
        @Schema(description = "Two parameters ")
        ReportUpdate update
        ) {
        final CookieSession mySession = Auth.getSession(session);
        if (mySession == null) {
            return Auth.noSessionResponse();
        }
        if (mySession.user == null || mySession.user.id != user) {
            return Response.status(Status.FORBIDDEN).build();
        }
        ReportsDB.getInstance()
            .markReportComplete(user, CLDRLocale.getInstance(locale),
                report, update.completed, update.acceptable);

        return Response.status(Status.NO_CONTENT).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary="List all report types")
    @APIResponse(
        responseCode = "200",
        description = "list responses",
        content = @Content(mediaType = "application/json",
        schema = @Schema(type = SchemaType.ARRAY, implementation = String.class))
    )
    public Response listReports() {
        return Response.ok().entity(ReportId.values()).build();
    }
    @Schema(description = "update to user’s report status")
    public static final class ReportUpdate {
        public ReportUpdate() {
        }
        @Schema(description = "True if user has completed evaluating this report, False if not complete. May not be !complete&&acceptable.")
        public boolean completed = false;
        @Schema(description = "True if values were acceptable. False if values weren’t acceptable, but user has voted for correct ones.")
        public boolean acceptable = false;
    }
}
