package org.unicode.cldr.web.api;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.UserRegistry.LogoutException;
import org.unicode.cldr.web.WebContext;

@Path("/auth")
@Tag(name = "auth", description = "APIs for authentication")
public class Auth {
    /**
     * Header to be used for a ST Session id
     */
    public static final String SESSION_HEADER = "X-SurveyTool-Session";


    @Path("/login")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Login and get a CookieSession id",
        description = "This logs in with a username/password and returns a CookieSession")
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "200",
                description = "CookieSession",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = LoginResponse.class))),
            @APIResponse(
                responseCode = "401",
                description = "Authorization required"),
        })
    public Response login(
        @Context HttpServletRequest hreq,
        LoginRequest request) {
        if (request.isEmpty()) {
            return Response.status(401, "Authorization required").build();
        }
        try {
            LoginResponse resp = new LoginResponse();
            // we start with the user
            String userIP = WebContext.userIP(hreq);
            resp.user = CookieSession.sm.reg.get(request.password,
                request.email, userIP);
            if (resp.user == null) {
                return Response.status(403, "Login failed").build();
            }
            CookieSession session = CookieSession.retrieveUser(resp.user);
            if (session == null) {
                resp.newlyLoggedIn = true;
                session = CookieSession.newSession(resp.user, userIP);
            }
            resp.sessionId = session.id;
            return Response.ok().entity(resp)
                .header(SESSION_HEADER, session.id)
                .build();
        } catch (LogoutException ioe) {
            return Response.status(403, "Login Failed").build();
        }
    }


    @Path("/logout")
    @GET
    @Operation(
        summary = "Logout, clear cookies",
        description = "Clear auto-login cookies, and log out the specified session.")
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "204",
                description = "Cookies cleared, Logged Out"),
            @APIResponse(
                responseCode = "404",
                description = "Session not found (cookies still cleared)"),
            @APIResponse(
                responseCode = "417",
                description = "Invalid parameter (cookies still cleared"),
        })
    public Response logout(
        @Context HttpServletRequest hreq,
        @Context HttpServletResponse hresp,
        @QueryParam("session") @Schema(required = true, description = "Session ID to logout")
        final String session) {
        // Remove auto-login cookies
        WebContext.removeLoginCookies(hreq, hresp);

        // Validate input string
        if (session == null || session.isEmpty()) {
            return Response.status(Status.EXPECTATION_FAILED).build();
        }

        // Fetch the Cookie Session
        CookieSession s = CookieSession.retrieveWithoutTouch(session);
        if (s == null) {
            return Response.status(Status.NOT_FOUND).build();
        }

        // Remove the session
        s.remove();

        return Response.status(Status.NO_CONTENT)
            .header(SESSION_HEADER, null)
            .build();
    }


    @Path("/info")
    @GET
    @Operation(
        summary = "Validate session",
        description = "Validate session and return user info.")
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "200",
                description = "Session OK",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = LoginResponse.class))),
            @APIResponse(
                responseCode = "404",
                description = "Session not found"),
            @APIResponse(
                responseCode = "417",
                description = "Invalid parameter"),
        })
    public Response info(
        @QueryParam("session") @Schema(required = true, description = "Session ID to check")
        final String session,

        @QueryParam("touch") @Schema(required = false, defaultValue = "false", description = "Whether to mark the session as updated")
        final boolean touch) {

        // Fetch the Cookie Session
        final CookieSession s = getSession(session);
        if (s == null) {
            return Auth.noSessionResponse();
        }

        // Touch if requested
        if (touch) {
            s.userDidAction();
            s.touch();
        }

        // Response
        LoginResponse resp = new LoginResponse();
        resp.sessionId = session;
        resp.user = s.user;
        return Response.ok().entity(resp)
            .header(SESSION_HEADER, session)
            .build();
    }


    /**
     * Extract a CookieSession from a session string
     * @param session
     * @return session or null
     */
    public static CookieSession getSession(String session) {
        if (session == null || session.isEmpty()) return null;
        CookieSession.checkForExpiredSessions();
        return CookieSession.retrieveWithoutTouch(session);
    }

    /**
     * Convenience function for returning the response when there's no session
     * @return
     */
    public static Response noSessionResponse() {
        return Response.status(Status.UNAUTHORIZED).build();
    }
}
