package org.unicode.cldr.web.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.unicode.cldr.web.UserRegistry;

@Path("/organizations")
@Tag(name = "organizations", description = "Get the list of Survey Tool organizations")
public class OrgList {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get Organization List",
        description = "This handles a request for the list of organizations")
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "200",
                description = "Results of OrgList request",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = OrgListResponse.class))),
        })
    public Response getOrgs() {
        try {
            OrgListResponse response = new OrgListResponse();
            return Response.ok(response, MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            return Response.status(500, "An exception occurred").entity(e).build();
        }
    }

    @Schema(description = "Response for OrgList query")
    public final class OrgListResponse {

        @Schema(description = "List of organization names")
        public String[] list;

        public OrgListResponse() {
            list = UserRegistry.getOrgList();
        }
    }
}
