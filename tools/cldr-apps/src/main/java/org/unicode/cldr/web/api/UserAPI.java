package org.unicode.cldr.web.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.unicode.cldr.util.VoteResolver.Level;
import org.unicode.cldr.web.ClaSignature;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.UserRegistry;
import org.unicode.cldr.web.UserRegistry.User;

@Path("/users")
@Tag(name = "user", description = "APIs for user management")
public class UserAPI {

    public static final class UserInfo {
        public final String email;
        public final String emailHash;
        public final Level level;
        public final String org;
        public final String name;
        public final int userid;

        @Schema(description = "CLA is signed, if false no permission to contribute.")
        public final boolean claSigned;

        public UserInfo(int userid) {
            this.userid = userid;
            // Logged out user, no info
            this.email = null;
            this.level = null;
            this.org = null;
            this.emailHash = null;
            this.name = "User #" + userid;
            this.claSigned = false; // TODO
        }

        public UserInfo(User u) {
            this.email = u.email;
            this.emailHash = u.getEmailHash();
            this.org = u.getOrganization().name();
            this.level = u.getLevel();
            this.name = u.name;
            this.userid = u.id;
            this.claSigned = u.claSigned;
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get user info", description = "Returns information about a user")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "User info",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = UserInfo.class))),
                @APIResponse(responseCode = "404", description = "Session not found"),
            })
    @Path("/info/{uid}")
    public Response getUserInfo(
            @Parameter(required = true, example = "1234") @PathParam("uid") int uid,
            @HeaderParam(Auth.SESSION_HEADER) String session) {
        final CookieSession mySession = Auth.getSession(session);
        if (mySession == null) {
            return Auth.noSessionResponse();
        }

        if (mySession.user == null) {
            // Note: user isn't even logged in, don't even validity check
            return Response.ok(new UserInfo(uid)).build();
        }

        UserRegistry.User u = CookieSession.sm.reg.getInfo(uid);
        if (u == null) {
            return Response.status(404).build();
        }
        return Response.ok(new UserInfo(u)).build();
    }

    @PUT
    @Operation(summary = "Sign CLA", description = "mark user as signing the CLA")
    @Consumes(MediaType.APPLICATION_JSON)
    @APIResponses(
            value = {
                @APIResponse(responseCode = "202", description = "Accepted CLA"),
                @APIResponse(responseCode = "401", description = "Unauthorized"),
                @APIResponse(responseCode = "406", description = "Parameter error"),
                @APIResponse(responseCode = "423", description = "cannot modify org CLA")
            })
    @Path("/cla")
    public Response signCla(@HeaderParam(Auth.SESSION_HEADER) String session, ClaSignature cla) {
        final CookieSession mySession = Auth.getSession(session);
        if (mySession == null || mySession.user == null) {
            return Response.status(401).build();
        }
        if (ClaSignature.CLA_ORGS.contains(mySession.user.getOrganization())) {
            return Response.status(423, "Cannot modify org CLA").build();
        }
        if (!cla.valid()) {
            return Response.status(406, "invalid CLA parameters").build();
        }
        mySession.user.signCla(cla);
        return Response.status(202, "ok").build();
    }

    @DELETE
    @Operation(summary = "Revoke CLA", description = "mark user as not signing the CLA")
    @APIResponses(
            value = {
                @APIResponse(responseCode = "202", description = "removed"),
                @APIResponse(responseCode = "401", description = "unauthorized"),
                @APIResponse(responseCode = "404", description = "not found"),
                @APIResponse(responseCode = "423", description = "cannot modify org CLA")
            })
    @Path("/cla")
    public Response revokeCla(@HeaderParam(Auth.SESSION_HEADER) String session) {
        final CookieSession mySession = Auth.getSession(session);
        if (mySession == null || mySession.user == null) {
            return Response.status(401).build();
        }
        if (ClaSignature.CLA_ORGS.contains(mySession.user.getOrganization())) {
            return Response.status(423, "Cannot modify org CLA").build();
        }
        if (!mySession.user.revokeCla()) {
            return Response.status(404, "not found").build();
        }
        return Response.status(202, "ok").build();
    }

    @GET
    @Operation(summary = "Get CLA", description = "Get CLA info")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "CLA info",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ClaSignature.class))),
                @APIResponse(responseCode = "401", description = "unauthorized"),
                @APIResponse(responseCode = "404", description = "not found")
            })
    @Path("/cla")
    public Response getCla(@HeaderParam(Auth.SESSION_HEADER) String session) {
        final CookieSession mySession = Auth.getSession(session);
        if (mySession == null || mySession.user == null) {
            return Response.status(401).build();
        }
        final ClaSignature cla = mySession.user.getCla();
        if (cla == null) {
            return Response.status(404, "not found").build();
        }
        return Response.ok(cla).build();
    }
}
