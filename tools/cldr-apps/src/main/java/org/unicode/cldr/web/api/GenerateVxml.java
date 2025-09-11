package org.unicode.cldr.web.api;

import java.io.File;
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
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Zipper;
import org.unicode.cldr.web.*;

@ApplicationScoped
@Path("/vxml")
@Tag(name = "Generate VXML", description = "APIs for Survey Tool VXML (Vetted XML) generation")
public class GenerateVxml {

    /**
     * Substitute for not-found MediaType.APPLICATION_ZIP. An alternative would be
     * MediaType.APPLICATION_OCTET_STREAM.
     */
    private final String APPLICATION_ZIP = "application/zip";

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
            if (!UserRegistry.userIsTCOrStronger(cs.user)) {
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
            VxmlResponse vr = getVxmlResponse(request.requestType, cs);
            return Response.ok(vr).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get the response for VXML
     *
     * @param requestType the RequestType
     * @param cs the CookieSession
     * @return the VxmlResponse
     * @throws IOException for getCanonicalPath failure
     */
    private VxmlResponse getVxmlResponse(VxmlQueue.RequestType requestType, CookieSession cs)
            throws IOException {
        VxmlQueue queue = VxmlQueue.getInstance();
        QueueMemberId qmi = new QueueMemberId(cs);
        VxmlResponse response = new VxmlResponse();
        VxmlQueue.Args args = new VxmlQueue.Args(qmi, requestType);
        VxmlQueue.Results results = queue.getResults(args);
        response.message = results.generationMessage;
        response.status = results.status;
        response.directory = results.directory == null ? "" : results.directory.getCanonicalPath();
        CLDRLocale loc = results.getLocale();
        response.localeId = loc == null ? "" : loc.getBaseName();
        response.localesDone = results.getLocalesDone();
        response.localesTotal = results.getLocalesTotal();
        response.percent = results.getPercent();
        response.verificationStatus = results.verificationStatus;
        response.verificationFailures = results.verificationFailures.toArray(new String[0]);
        response.verificationWarnings = results.verificationWarnings.toArray(new String[0]);
        return response;
    }

    @Schema(description = "VXML Request")
    public static final class VxmlRequest {
        @Schema(implementation = VxmlQueue.RequestType.class)
        public VxmlQueue.RequestType requestType;
    }

    @Schema(description = "VXML Response")
    public static final class VxmlResponse {
        @Schema(description = "VXML Response status enum")
        public VxmlQueue.Status status;

        @Schema(description = "Current status message")
        public String message = "";

        @Schema(description = "Directory newly created to contain generated files")
        public String directory;

        @Schema(description = "Latest locale written")
        public String localeId;

        @Schema(description = "Locales written")
        public int localesDone;

        @Schema(description = "Total number of locales")
        public int localesTotal;

        @Schema(description = "Estimated percentage complete")
        public Number percent;

        @Schema(description = "Verification status enum")
        public VxmlGenerator.VerificationStatus verificationStatus;

        @Schema(description = "Verification failure messages")
        public String[] verificationFailures;

        @Schema(description = "Verification warning messages")
        public String[] verificationWarnings;
    }

    @GET
    @Path("/download")
    @Produces(APPLICATION_ZIP)
    @Operation(summary = "Download VXML", description = "Download VXML as zip file")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Download VXML",
                        content =
                                @Content(
                                        mediaType = "application/zip",
                                        schema = @Schema(implementation = Response.class))),
                @APIResponse(responseCode = "403", description = "Forbidden"),
                @APIResponse(responseCode = "404", description = "Not Found"),
                @APIResponse(
                        responseCode = "500",
                        description = "Internal Server Error",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = STError.class))),
            })
    public Response downloadVxml(
            @QueryParam("dirName")
                    @Schema(
                            description =
                                    "The name (not full path) of the directory containing VXML")
                    String dirName,
            @HeaderParam(Auth.SESSION_HEADER) String sessionString) {
        try {
            CookieSession cs = Auth.getSession(sessionString);
            if (cs == null) {
                return Auth.noSessionResponse();
            }
            if (!UserRegistry.userIsTCOrStronger(cs.user)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            if (SurveyMain.isBusted()
                    || !SurveyMain.wasInitCalled()
                    || !SurveyMain.triedToStartUp()) {
                return STError.surveyNotQuiteReady();
            }
            cs.userDidAction();
            File dir = getDirectory(dirName);
            if (dir == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            File zipFile = Zipper.zipDirectory(dir);
            return Response.ok(zipFile).header("Content-Type", APPLICATION_ZIP).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }
    }

    /**
     * Sanitize the "user-provided" dirName, normally actually provided by the back end, but
     * possibly concocted by evildoers. It should contain no slashes or periods, and should match
     * one of the "vetdata-..." siblings of the automatic vetdata dir CookieSession.sm.getVetdir().
     * Some of this sanitizing is redundant and is really intended to satisfy automatic checkers and
     * prevent failures like "Uncontrolled data used in path expression".
     *
     * @param dirName the directory name (not full path)
     * @return the directory (sibling of the automatic vetdata directory)
     */
    private File getDirectory(String dirName) {
        if (dirName.contains("/") || dirName.contains(".")) {
            return null;
        }
        File autoVetDir = CookieSession.sm.getVetdir();
        if (!dirName.startsWith(autoVetDir.getName())) {
            return null;
        }
        File parent = autoVetDir.getParentFile();
        File[] children = parent.listFiles();
        if (children != null) {
            for (File child : children) {
                if (dirName.equals(child.getName())) {
                    return child;
                }
            }
        }
        return null;
    }
}
