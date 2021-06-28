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
import org.unicode.cldr.web.SurveyMain;
import org.unicode.cldr.web.UserRegistry;
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
        @Context HttpServletResponse hresp,
        @QueryParam("remember") @Schema( required = false, defaultValue = "false", description = "If true, remember login")
        boolean remember,
        LoginRequest request) {

        // If there's no user/pass, try to fill one in from cookies.
        if (request.isEmpty()) {
            // No option to ignore the cookies.
            // If you want to logout, use the /logout endpoint first.
            final String myEmail = WebContext.getCookieValue(hreq, SurveyMain.QUERY_EMAIL);
            final String myPassword = WebContext.getCookieValue(hreq, SurveyMain.QUERY_PASSWORD);
            if (myEmail != null && !myEmail.isEmpty() && myPassword != null && !myPassword.isEmpty()) {
                // use values from cookie
                request.password = myPassword;
                request.email = myEmail;
            }
        }
        try {
            // we start with the user
            String userIP = WebContext.userIP(hreq);
            CookieSession session = null;
            if (!request.isEmpty()) {
                UserRegistry.User user = CookieSession.sm.reg.get(request.password,
                    request.email, userIP);
                if (user == null) {
                    return Response.status(403, "Login failed").build();
                }
                session = CookieSession.retrieveUser(user);
                if (session == null) {
                    session = CookieSession.newSession(user, userIP);
                }
                if (remember == true && user != null) {
                    WebContext.loginRemember(hresp, user);
                }
            } else {
                // Is there a valid sessionId in the cookie?
                final String sessionFromCookie = WebContext.getSessionIdFromCookie(hreq);
                if (sessionFromCookie != null && !sessionFromCookie.isEmpty()) {
                    session = CookieSession.retrieve(sessionFromCookie);
                    // session will be null if not valid
                }

                if (session == null) {
                    // anonymous session
                    // code ported from WebContext

                    // Funny interface. Non-null means a banned IP.
                    // We aren't going to return the special session, but just throw.
                    if (CookieSession.checkForAbuseFrom(userIP,
                        SurveyMain.BAD_IPS,
                        hreq.getHeader("User-Agent")) != null) {
                            final String tooManyMessage = "Your IP, " + userIP
                                + " has been throttled for making too many connections." +
                                " Try turning on cookies, or obeying the 'META ROBOTS' tag.";
                            return Response.status(429, "Too many requests from this IP")
                            .entity(new STError(tooManyMessage)).build();
                    }

                    // Also check for too many guests.
                    if (CookieSession.tooManyGuests()) {
                        final String tooManyMessage = "We have too many people ("+
                            CookieSession.getUserCount() +
                            ") browsing the CLDR Data on the Survey Tool. Please try again later when the load has gone down.";
                            return Response.status(429, "Too many guests")
                            .entity(new STError(tooManyMessage)).build();
                    }

                    // All clear. Make an anonymous session.
                    session = CookieSession.newSession(true, userIP);
                }
            }
            LoginResponse resp = createLoginResponse(session);
            WebContext.setSessionCookie(hresp, resp.sessionId);
            return Response.ok().entity(resp)
                .header(SESSION_HEADER, session.id)
                .build();
        } catch (LogoutException ioe) {
            return Response.status(403, "Login Failed").build();
        }
    }


    /**
     * Create a LoginResponse, given a session.
     * Put this here and not in LoginResponse because of serialization
     * @param session
     * @return
     */
    private LoginResponse createLoginResponse(CookieSession session) {
        LoginResponse resp = new LoginResponse();
        resp.sessionId = session.id;
        resp.user = (session.user != null);
        return resp;
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

        // TODO: move Cookie management out of WebContext and into Auth.java
        WebContext.logout(hreq, hresp);

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
        LoginResponse resp = createLoginResponse(s);

        return Response.ok().entity(resp)
            .header(SESSION_HEADER, session)
            .build();
    }


    @Path("/lock")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Lock (disable) account",
        description = "Lock (disable, unsubscribe) the account of the user making the request.")
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "200",
                description = "Locked OK",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = LoginResponse.class))),
            @APIResponse(
                responseCode = "403",
                description = "Forbidden"),
            @APIResponse(
                responseCode = "404",
                description = "Session not found"),
            @APIResponse(
                responseCode = "417",
                description = "Invalid parameter"),
            @APIResponse(
                responseCode = "500",
                description = "Failure"),
        })
    public Response lock(
        @Context HttpServletRequest hreq,
        @Context HttpServletResponse hresp,
        LockRequest request) {

        if (request.isEmpty()) {
            return Response.status(417, "Missing parameter").build();
        }
        final CookieSession s = getSession(request.session);
        if (s == null) {
            return noSessionResponse();
        } else if (!s.user.email.equals(request.email)) {
            return Response.status(417, "Invalid parameter")
                .entity(new STError("Mismatched E-mail parameter")).build();
        } else if (request.email.equals(UserRegistry.ADMIN_EMAIL)) {
            return Response.status(403, "Forbidden")
                .entity(new STError("Cannot lock Admin")).build();
        }
        String lockResult = CookieSession.sm.reg.lockAccount(request.email, request.reason, WebContext.userIP(hreq));
        if ("OK".equals(lockResult)) {
            LoginResponse loginResponse = createLoginResponse(s);
            WebContext.logout(hreq, hresp);
            return Response.ok().entity(loginResponse)
                .header(SESSION_HEADER, request.session)
                .build();
        } else {
            return Response.status(500, "Failure")
                .entity(new STError(lockResult)).build();
        }
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

