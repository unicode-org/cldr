package org.unicode.cldr.web.api;

import java.io.StringWriter;
import java.io.Writer;
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
import org.unicode.cldr.web.*;

@ApplicationScoped
@Path("/vxml")
@Tag(name = "Generate VXML", description = "APIs for Survey Tool VXML (Vetted XML) generation")
public class GenerateVxml {
    private static final Logger logger = SurveyLog.forClass(GenerateVxml.class);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Generate VXML", description = "Generate VXML")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Generate VXML",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = VxmlResponse.class))),
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
    public Response generateVxml(@HeaderParam(Auth.SESSION_HEADER) String sessionString) {
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
        VxmlResponse response = new VxmlResponse();
        try (Writer out = new StringWriter()) {
            OutputFileManager.generateVxml(
                    out,
                    true /* outputFiles */,
                    true /* removeEmpty */,
                    true /* verifyConsistent */);
            response.output = out.toString();
        } catch (Exception e) {
            return Response.status(500, "Internal Server Error").build();
        }
        return Response.ok(response).build();
    }

    public enum Status {
        /** Waiting on other users/tasks */
        WAITING,
        /** Processing in progress */
        PROCESSING,
        /** Contents are available */
        READY,
        /** Stopped, due to error or successful completion */
        STOPPED,
    }

    @Schema(description = "VXML Response")
    public static final class VxmlResponse {
        public VxmlResponse() {}

        @Schema(description = "VXML Response status enum")
        public Status status;

        @Schema(description = "HTML current status message")
        public String message = "";

        @Schema(description = "Estimated percentage complete")
        public Number percent;

        @Schema(description = "HTML output on success")
        public String output;
    }
}
