package org.unicode.cldr.web.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.json.JSONObject;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.UserRegistry;

@Path("/userlevels")
@Tag(name = "userlevels", description = "Get the list of Survey Tool user levels")
public class UserLevels {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get User Level List",
        description = "Handle a request for the list of user levels and associated data partly depending on current user rights")
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "200",
                description = "Results of UserLevels request",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = String.class,
                        example = "{\n"
                            + "  \"levels\":{\n" +
                            "      \"0\":{\"string\":\"0: (ADMIN)\",\"isManagerFor\":true,\"name\":\"admin\",\"canCreateOrSetLevelTo\":true},\n"
                            + "    \"1\":{\"string\":\"1: (TC)\",\"isManagerFor\":true,\"name\":\"tc\",\"canCreateOrSetLevelTo\":true},\n"
                            + "    \"2\":{\"string\":\"2: (MANAGER)\",\"isManagerFor\":true,\"name\":\"manager\",\"canCreateOrSetLevelTo\":true},\n"
                            + "    \"5\":{\"string\":\"5: (VETTER)\",\"isManagerFor\":true,\"name\":\"vetter\",\"canCreateOrSetLevelTo\":true},\n"
                            + "    \"10\":{\"string\":\"10: (STREET)\",\"isManagerFor\":true,\"name\":\"street\",\"canCreateOrSetLevelTo\":true},\n"
                            + "    \"999\":{\"string\":\"999: (LOCKED)\",\"isManagerFor\":true,\"name\":\"locked\",\"canCreateOrSetLevelTo\":true}\n"
                            + "  }\n"
                            + "}"))),
        })
    public Response getLevels(
        @QueryParam("s") @Schema(required = true, description = "Session String") String sessionString) {
        CookieSession session = Auth.getSession(sessionString);
        if (session == null) {
            return Auth.noSessionResponse();
        }
        if (!UserRegistry.userCanCreateUsers(session.user)) {
            return Response.status(403, "Forbidden").build();
        }
        session.userDidAction();
        try {
            JSONObject jo = new JSONObject();
            jo.put("levels", UserRegistry.getLevelMenuJson(session.user));
            return Response.ok(jo.toString()).build();
        } catch (Exception e) {
            return Response.status(500, "An exception occurred").entity(e).build();
        }
    }
}
