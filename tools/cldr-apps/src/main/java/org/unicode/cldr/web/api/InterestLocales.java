package org.unicode.cldr.web.api;

import java.util.Map;
import java.util.TreeMap;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.UserRegistry;

@Path("/intlocs")
@Tag(name = "user", description = "APIs for user management")
public class InterestLocales {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Set interest locales",
            description = "Enables users to specify their locales of interest")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Interest locales were set",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        type = SchemaType.OBJECT,
                                                        example =
                                                                "{\n"
                                                                        + "  \"status\": \"OK\",\n"
                                                                        + "}\n"
                                                                        + "")))
            })
    public Response setIntLocs(IntLocsRequest request) {
        CookieSession.checkForExpiredSessions();
        if (request.sessionString == null || request.sessionString.isEmpty()) {
            return Response.status(401, "No session string").build();
        }
        CookieSession session = CookieSession.retrieve(request.sessionString);
        if (session == null) {
            return Response.status(401, "No session").build();
        }
        if (request.email == null || request.email.isEmpty()) {
            return Response.status(401, "No email").build();
        }
        /*
         * intlocs may be empty, for deletion; but it can't be null
         */
        if (request.intlocs == null) {
            return Response.status(401, "Null intlocs").build();
        }
        if (!UserRegistry.userCanSetInterestLocales(session.user)) {
            return Response.status(403, "Forbidden").build();
        }
        if (!request.email.equals(session.user.email)) {
            return Response.status(403, "Wrong email").build();
        }
        session.userDidAction();
        return reallySetIntLocs(session, request.intlocs);
    }

    private Response reallySetIntLocs(CookieSession session, String intlocs) {
        Map<String, String> r = new TreeMap<>();
        UserRegistry reg = CookieSession.sm.reg;
        /*
         * Last arg "true" for setLocales means set the interest locales (intlocs),
         * not the authorized locales (locales)
         *
         * Here the current user (session.user) is setting their own interest locs,
         * not setting them for a different user
         */
        String msg = reg.setLocales(session, session.user, intlocs, true);
        /*
         * session.user.intlocs does NOT get updated automatically by setLocales
         * so call userModified to clear the cache, and getInfo to get the updated intlocs
         */
        reg.userModified(session.user.id);
        UserRegistry.User updatedUser = reg.getInfo(session.user.id);
        r.put("status", msg);
        r.put("email", updatedUser.email);
        r.put("intlocs", updatedUser.intlocs);
        return Response.ok(r).build();
    }
}
