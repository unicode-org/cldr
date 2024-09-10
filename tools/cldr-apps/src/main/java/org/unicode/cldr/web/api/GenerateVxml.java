package org.unicode.cldr.web.api;

import java.io.IOException;
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
    public Response generateVxml(
            VxmlRequest request, @HeaderParam(Auth.SESSION_HEADER) String sessionString) {
        try {
            CookieSession cs = Auth.getSession(sessionString);
            if (cs == null) {
                return Auth.noSessionResponse();
            }
            if (!UserRegistry.userIsAdmin(cs.user)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            if (SurveyMain.isBusted()
                    || !SurveyMain.wasInitCalled()
                    || !SurveyMain.triedToStartUp()) {
                return STError.surveyNotQuiteReady();
            }
            // Make sure setupDB has run before we start to generate VXML, to avoid the risk of
            // concurrency problems, since otherwise if the server just started, setupDB may run
            // concurrently with the vxml worker thread.
            CookieSession.sm.getSTFactory().setupDB();
            cs.userDidAction();
            VxmlResponse vr = getVxmlResponse(request.loadingPolicy, cs);
            return Response.ok(vr).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get the response for VXML
     *
     * @param loadingPolicy the LoadingPolicy
     * @param cs the CookieSession
     * @return the VxmlResponse
     * @throws IOException if thrown by VxmlQueue.getOutput
     */
    private VxmlResponse getVxmlResponse(VxmlQueue.LoadingPolicy loadingPolicy, CookieSession cs)
            throws IOException {
        VxmlQueue queue = VxmlQueue.getInstance();
        QueueMemberId qmi = new QueueMemberId(cs);
        VxmlResponse response = new VxmlResponse();
        VxmlQueue.Args args = new VxmlQueue.Args(qmi, loadingPolicy);
        VxmlQueue.Results results = new VxmlQueue.Results();
        response.message = queue.getOutput(args, results);
        response.percent = queue.getPercent();
        response.status = results.status;
        response.output = results.output.toString();
        return response;
    }

    @Schema(description = "VXML Request")
    public static final class VxmlRequest {
        @Schema(implementation = VxmlQueue.LoadingPolicy.class)
        public VxmlQueue.LoadingPolicy loadingPolicy;
    }

    @Schema(description = "VXML Response")
    public static final class VxmlResponse {
        @Schema(description = "VXML Response status enum")
        public VxmlQueue.Status status;

        @Schema(description = "Current status message")
        public String message = "";

        @Schema(description = "Estimated percentage complete")
        public Number percent;

        @Schema(description = "Output on success")
        public String output;
    }
}
